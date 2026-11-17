package metajavbots.cerebro;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * CEREBRO LOCAL — El modelo en el propio ordenador, con KoboldCpp (como el
 * cerebro de Jarvis): sin internet y sin claves. Si ya hay uno escuchando
 * (el de Jarvis en el puerto 5001, o el del juego en el 5002), se usa; si
 * no, el juego puede arrancarlo él mismo con el modelo que haya en la
 * carpeta de modelos (por defecto, la de Jarvis). Los modelos no van en el
 * repositorio (pesan gigas): docs/CEREBRO.md dice cuáles bajar.
 */
public final class CerebroLocal {

    /** El modelo del juego (Qwen3.5-9B) y el de Jarvis, por orden de preferencia. */
    // Gemma 3 4B primero: en el portátil de Fran (i7-1065G7, 20 GB, gráfica integrada) es el que contesta rápido y deja
    // memoria de sobra; Qwen3.5-9B, cuando KoboldCpp lo cargue (docs/CEREBRO.md); el de Jarvis, de respaldo.
    public static final String[] MODELOS = {"google_gemma-3-4b-it-Q4_K_M.gguf", "Qwen_Qwen3.5-9B-Q4_K_M.gguf", "qwen2.5-coder-7b-instruct-q6_k.gguf"};
    public static final int PUERTO_DEL_JUEGO = 5002, PUERTO_DE_JARVIS = 5001;
    private static Process proceso;

    private CerebroLocal() {
    }

    /** La carpeta donde buscar koboldcpp y los modelos: la de los ajustes, o la de Jarvis si existe. */
    public static Path carpeta(String elegida) {
        if (elegida != null && !elegida.isBlank()) return Paths.get(elegida);
        Path jarvis = Paths.get(System.getProperty("user.home"), "OneDrive", "Documentos", "000KI_Local");
        return jarvis;
    }

    /** ¿Hay alguien escuchando en esa dirección? (pregunta por el modelo cargado). */
    public static boolean escucha(String direccion) {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create(direccion + "/api/v1/model")).timeout(Duration.ofSeconds(3)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return r.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** El cerebro local que ya esté en marcha (el del juego primero, luego el de Jarvis), o null. */
    public static Cerebro enMarcha() {
        for (int puerto : new int[]{PUERTO_DEL_JUEGO, PUERTO_DE_JARVIS}) {
            String d = "http://localhost:" + puerto;
            if (escucha(d)) return new CerebroHttp(CerebroHttp.Forma.OPENAI, d, "", "", puerto == PUERTO_DE_JARVIS ? "KoboldCpp (Jarvis)" : "KoboldCpp");
        }
        return null;
    }

    public static Path koboldcpp(Path carpeta) {
        for (String n : new String[]{"koboldcpp.exe", "koboldcpp", "koboldcpp-linux-x64", "koboldcpp-mac-arm64"}) {
            Path p = carpeta.resolve(n);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    /** Los modelos .gguf de la carpeta, el del juego primero. */
    public static List<Path> modelos(Path carpeta) {
        List<Path> l = new ArrayList<>();
        for (String n : MODELOS) if (Files.isRegularFile(carpeta.resolve(n))) l.add(carpeta.resolve(n));
        if (Files.isDirectory(carpeta)) {
            try (Stream<Path> s = Files.list(carpeta)) {
                s.filter(p -> p.toString().endsWith(".gguf") && !l.contains(p)).forEach(l::add);
            } catch (Exception e) {
                // nada más
            }
        }
        return l;
    }

    /** Arranca KoboldCpp con el modelo en el puerto del juego (minimizado, sin ventana de inicio). Devuelve un texto de lo que pasó. */
    public static synchronized String arrancar(Path carpeta, Path modelo) {
        if (escucha("http://localhost:" + PUERTO_DEL_JUEGO)) return "ya";
        Path exe = koboldcpp(carpeta);
        if (exe == null) return "sin_koboldcpp";
        if (modelo == null || !Files.isRegularFile(modelo)) return "sin_modelo";
        try {
            List<String> orden = new ArrayList<>(List.of(exe.toString(), "--model", modelo.toString(), "--contextsize", "4096",
                    "--gpulayers", "-1", "--usevulkan", "--port", Integer.toString(PUERTO_DEL_JUEGO), "--skiplauncher", "--quiet"));
            ProcessBuilder pb = new ProcessBuilder(orden).directory(carpeta.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(new File(System.getProperty("java.io.tmpdir"), "metajavbots_koboldcpp.txt"));
            proceso = pb.start();
            return "arrancando";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /**
     * Arranca el primer modelo de la carpeta que de verdad cargue (espera hasta que escucha; si KoboldCpp se cierra
     * antes, prueba el siguiente). Con KoboldCpp 1.112, Qwen3.5 aún no carga y se queda el de Jarvis (docs/ESTADO.md).
     * Se llama en un hilo aparte: tarda.
     */
    public static String arrancarElQueFuncione(Path carpeta, java.util.function.Consumer<String> estado) {
        return arrancarElQueFuncione(carpeta, modelos(carpeta), estado);
    }

    public static synchronized String arrancarElQueFuncione(Path carpeta, List<Path> modelos, java.util.function.Consumer<String> estado) {
        if (escucha("http://localhost:" + PUERTO_DEL_JUEGO)) return "ya";
        for (Path m : modelos) {
            if (estado != null) estado.accept(m.getFileName().toString());
            String r = arrancar(carpeta, m);
            if (!r.equals("arrancando")) return r;
            long limite = System.currentTimeMillis() + 180_000;
            while (System.currentTimeMillis() < limite) {
                if (escucha("http://localhost:" + PUERTO_DEL_JUEGO)) return "ya";
                synchronized (CerebroLocal.class) {
                    if (proceso == null || !proceso.isAlive()) break;
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "interrumpido";
                }
            }
            parar();
        }
        return "sin_modelo";
    }

    /** Cierra el KoboldCpp que arrancó el juego (el de Jarvis, no). */
    public static synchronized void parar() {
        if (proceso != null) {
            proceso.descendants().forEach(ProcessHandle::destroy);
            proceso.destroy();
            proceso = null;
        }
    }
}
