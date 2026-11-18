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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * CEREBRO LOCAL — Los modelos en el propio ordenador, con KoboldCpp (como el
 * cerebro de Jarvis): sin internet y sin claves. Cada cerebro local del
 * jugador tiene su KoboldCpp en su puerto (5002, 5003, 5004); si dos usan el
 * mismo modelo, comparten uno. Si ya hay uno escuchando (el de Jarvis en el
 * 5001), se usa sin arrancar nada. Antes de arrancar otro se mira la memoria:
 * los del juego, juntos, nunca pasan del presupuesto (lo que tiene el
 * ordenador menos lo que se deja a Windows y al juego); si no cabe, se apaga
 * antes el que lleve más rato sin usarse. Los modelos no van en el
 * repositorio (pesan gigas): docs/CEREBRO.md dice cuáles bajar.
 */
public final class CerebroLocal {

    /**
     * Los modelos del juego, por orden: Gemma 3 4B para lo rápido (contesta en ~11 s en el portátil de Fran), Qwen3.5-9B
     * para lo pesado (carga con KoboldCpp 1.124; ~30 s la primera respuesta) y el de Jarvis, de respaldo.
     */
    public static final String[] MODELOS = {"google_gemma-3-4b-it-Q4_K_M.gguf", "Qwen_Qwen3.5-9B-Q4_K_M.gguf", "qwen2.5-coder-7b-instruct-q6_k.gguf"};
    public static final int PUERTO_DEL_JUEGO = 5002, PUERTO_DE_JARVIS = 5001;
    /** Lo que se deja siempre a Windows, al juego y a lo demás que esté abierto (MB). */
    public static final long MEMORIA_RESERVADA_MB = 6000;
    /** Lo que gasta KoboldCpp además del archivo del modelo (contexto, cálculo; MB). */
    static final long MEMORIA_EXTRA_MB = 900;
    /** Cuánto se espera a que un modelo cargue antes de probar el siguiente. */
    private static final long ESPERA_DE_CARGA_MS = 180_000;

    /** Un KoboldCpp que arrancó el juego. */
    private static final class Servidor {
        final Process proceso;
        final Path modelo;
        final long memoriaMb;
        volatile long ultimoUso = System.currentTimeMillis();

        Servidor(Process proceso, Path modelo, long memoriaMb) {
            this.proceso = proceso;
            this.modelo = modelo;
            this.memoriaMb = memoriaMb;
        }
    }

    private static final Map<Integer, Servidor> SERVIDORES = new LinkedHashMap<>();
    /** El juego se cierra: no se arranca ninguno más (uno que estaba cargando se quedaba abierto sin juego). */
    private static volatile boolean cerrando;

    static {
        // Aunque el juego se cierre de golpe, los KoboldCpp que arrancó se cierran con él.
        Runtime.getRuntime().addShutdownHook(new Thread(CerebroLocal::cerrar, "cerrar-cerebros-locales"));
    }

    private CerebroLocal() {
    }

    /** La carpeta donde buscar koboldcpp y los modelos: la de los ajustes, o la de Jarvis si existe. */
    public static Path carpeta(String elegida) {
        if (elegida != null && !elegida.isBlank()) return Paths.get(elegida);
        return Paths.get(System.getProperty("user.home"), "OneDrive", "Documentos", "000KI_Local");
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
            String d = direccion(puerto);
            if (escucha(d)) return new CerebroHttp(CerebroHttp.Forma.OPENAI, d, "", "", puerto == PUERTO_DE_JARVIS ? "KoboldCpp (Jarvis)" : "KoboldCpp");
        }
        return null;
    }

    public static String direccion(int puerto) {
        return "http://localhost:" + puerto;
    }

    public static Path koboldcpp(Path carpeta) {
        for (String n : new String[]{"koboldcpp.exe", "koboldcpp", "koboldcpp-linux-x64", "koboldcpp-mac-arm64"}) {
            Path p = carpeta.resolve(n);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    private static Path carpetaMirada;
    private static List<Path> modelosMirados = List.of();
    private static long cuandoSeMiro;

    /** Los modelos .gguf de la carpeta, los del juego primero (se mira el disco como mucho cada dos segundos: los Ajustes lo piden en cada fotograma). */
    public static synchronized List<Path> modelos(Path carpeta) {
        long ahora = System.currentTimeMillis();
        if (carpeta.equals(carpetaMirada) && ahora - cuandoSeMiro < 2000) return modelosMirados;
        carpetaMirada = carpeta;
        cuandoSeMiro = ahora;
        modelosMirados = List.copyOf(buscarModelos(carpeta));
        return modelosMirados;
    }

    private static List<Path> buscarModelos(Path carpeta) {
        List<Path> l = new ArrayList<>();
        for (String n : MODELOS) if (Files.isRegularFile(carpeta.resolve(n))) l.add(carpeta.resolve(n));
        if (Files.isDirectory(carpeta)) {
            try (Stream<Path> s = Files.list(carpeta)) {
                s.filter(p -> p.toString().endsWith(".gguf") && !l.contains(p)).sorted().forEach(l::add);
            } catch (Exception e) {
                // nada más
            }
        }
        return l;
    }

    /** El modelo que toca a una ranura si el jugador no elige: la 1, Gemma (rápido); la 2, Qwen3.5 (pesado); la 3, el siguiente. */
    public static String modeloPorDefecto(Path carpeta, int ranura) {
        List<Path> ms = modelos(carpeta);
        if (ms.isEmpty()) return "";
        if (ranura < MODELOS.length && Files.isRegularFile(carpeta.resolve(MODELOS[ranura]))) return MODELOS[ranura];
        return ms.get(Math.min(ranura, ms.size() - 1)).getFileName().toString();
    }

    // ================================================================ memoria

    /** Lo que gastará en memoria un modelo cargado (MB): su archivo y un poco más. */
    public static long memoriaDe(Path modelo) {
        try {
            return Files.size(modelo) / (1024 * 1024) + MEMORIA_EXTRA_MB;
        } catch (Exception e) {
            return 4000;
        }
    }

    /** La memoria física del ordenador (MB), o 0 si no se sabe. */
    public static long memoriaTotalMb() {
        java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) os).getTotalMemorySize() / (1024 * 1024);
        }
        return 0;
    }

    /** Lo que pueden gastar juntos los modelos que arranca el juego (MB). */
    public static long presupuestoMb() {
        long total = memoriaTotalMb();
        return total == 0 ? 10_000 : Math.max(3000, total - MEMORIA_RESERVADA_MB);
    }

    /** Lo que gastan ahora los modelos que arrancó el juego (MB). */
    public static synchronized long memoriaEnUsoMb() {
        long n = 0;
        for (Servidor s : SERVIDORES.values()) if (s.proceso.isAlive()) n += s.memoriaMb;
        return n;
    }

    /** Apaga los que lleven más rato sin usarse hasta que quepa uno de «hace falta» MB. ¿Cabe? */
    private static synchronized boolean hacerSitio(long haceFalta, int puertoNuevo) {
        while (memoriaEnUsoMb() + haceFalta > presupuestoMb()) {
            Integer masViejo = null;
            for (Map.Entry<Integer, Servidor> e : SERVIDORES.entrySet()) {
                if (e.getKey() == puertoNuevo) continue;
                if (masViejo == null || e.getValue().ultimoUso < SERVIDORES.get(masViejo).ultimoUso) masViejo = e.getKey();
            }
            // Ninguno más que apagar: en un ordenador pequeño se prueba igual (Windows usará el archivo de paginación).
            if (masViejo == null) return true;
            parar(masViejo);
        }
        return true;
    }

    // ================================================================ arrancar y parar

    /** Arranca KoboldCpp con el modelo en ese puerto, en la gráfica (Vulkan). Devuelve un texto de lo que pasó. */
    public static String arrancar(Path carpeta, Path modelo, int puerto) {
        return arrancar(carpeta, modelo, puerto, true);
    }

    /**
     * Arranca KoboldCpp con el modelo en ese puerto (minimizado, sin ventana de inicio), en la gráfica o solo con el
     * procesador. Devuelve un texto de lo que pasó.
     */
    public static synchronized String arrancar(Path carpeta, Path modelo, int puerto, boolean enLaGrafica) {
        if (cerrando) return "cerrando";
        if (escucha(direccion(puerto))) return "ya";
        Servidor cargando = SERVIDORES.get(puerto);
        if (cargando != null && cargando.proceso.isAlive()) return "arrancando";
        SERVIDORES.remove(puerto);
        Path exe = koboldcpp(carpeta);
        if (exe == null) return "sin_koboldcpp";
        if (modelo == null || !Files.isRegularFile(modelo)) return "sin_modelo";
        long memoria = memoriaDe(modelo);
        if (!hacerSitio(memoria, puerto)) return "sin_memoria";
        try {
            List<String> orden = new ArrayList<>(List.of(exe.toString(), "--model", modelo.toString(), "--contextsize", "4096",
                    "--port", Integer.toString(puerto), "--skiplauncher", "--quiet"));
            orden.addAll(enLaGrafica ? List.of("--gpulayers", "-1", "--usevulkan") : List.of("--gpulayers", "0"));
            ProcessBuilder pb = new ProcessBuilder(orden).directory(carpeta.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(new File(System.getProperty("java.io.tmpdir"), "metajavbots_koboldcpp_" + puerto + ".txt"));
            SERVIDORES.put(puerto, new Servidor(pb.start(), modelo, memoria));
            return "arrancando";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /** Arranca KoboldCpp en el puerto del juego (5002): lo de antes de los tres cerebros. */
    public static String arrancar(Path carpeta, Path modelo) {
        return arrancar(carpeta, modelo, PUERTO_DEL_JUEGO);
    }

    /**
     * Arranca el primer modelo de la lista que de verdad cargue (espera hasta que escucha; si KoboldCpp se cierra antes,
     * prueba el siguiente). Se llama en un hilo aparte: tarda.
     */
    public static String arrancarElQueFuncione(Path carpeta, Consumer<String> estado) {
        return arrancarElQueFuncione(carpeta, modelos(carpeta), PUERTO_DEL_JUEGO, estado);
    }

    public static String arrancarElQueFuncione(Path carpeta, List<Path> modelos, Consumer<String> estado) {
        return arrancarElQueFuncione(carpeta, modelos, PUERTO_DEL_JUEGO, estado);
    }

    public static String arrancarElQueFuncione(Path carpeta, List<Path> modelos, int puerto, Consumer<String> estado) {
        if (escucha(direccion(puerto))) return "ya";
        String ultimo = "sin_modelo";
        for (Path m : modelos) {
            if (estado != null) estado.accept(m.getFileName().toString());
            // Primero en la gráfica; si no cabe (la integrada comparte poca memoria con los otros modelos), con el procesador,
            // que en el portátil de Fran contesta casi igual de rápido (Qwen3.5-9B: 25 s frente a 30 s).
            for (boolean enLaGrafica : new boolean[]{true, false}) {
                ultimo = arrancar(carpeta, m, puerto, enLaGrafica);
                if (ultimo.equals("ya")) return ultimo;
                if (!ultimo.equals("arrancando")) break;
                if (esperarACargar(puerto)) return "ya";
                parar(puerto);
                ultimo = "sin_modelo";
            }
        }
        return ultimo;
    }

    /** Espera a que el KoboldCpp de ese puerto escuche; false si se cierra antes o tarda demasiado. */
    private static boolean esperarACargar(int puerto) {
        long limite = System.currentTimeMillis() + ESPERA_DE_CARGA_MS;
        while (System.currentTimeMillis() < limite) {
            if (escucha(direccion(puerto))) return true;
            synchronized (CerebroLocal.class) {
                Servidor s = SERVIDORES.get(puerto);
                if (s == null || !s.proceso.isAlive()) return false;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** El modelo que tiene cargado el KoboldCpp del juego en ese puerto, o null. */
    public static synchronized Path modeloEn(int puerto) {
        Servidor s = SERVIDORES.get(puerto);
        return s == null || !s.proceso.isAlive() ? null : s.modelo;
    }

    /** Apunta que se acaba de usar el de ese puerto (para no apagarlo por viejo). */
    public static synchronized void usado(int puerto) {
        Servidor s = SERVIDORES.get(puerto);
        if (s != null) s.ultimoUso = System.currentTimeMillis();
    }

    /** Los puertos con un KoboldCpp del juego en marcha. */
    public static synchronized List<Integer> enMarchaDelJuego() {
        List<Integer> l = new ArrayList<>();
        for (Map.Entry<Integer, Servidor> e : SERVIDORES.entrySet()) if (e.getValue().proceso.isAlive()) l.add(e.getKey());
        return l;
    }

    /** Apaga los que lleven más de «minutos» sin usarse. ¿Queda alguno? */
    static synchronized boolean apagarLosQuietos(long minutos) {
        long ahora = System.currentTimeMillis();
        for (Integer puerto : new ArrayList<>(SERVIDORES.keySet())) {
            if (ahora - SERVIDORES.get(puerto).ultimoUso > minutos * 60_000) parar(puerto);
        }
        return !SERVIDORES.isEmpty();
    }

    /** Cierra el KoboldCpp que arrancó el juego en ese puerto. */
    public static synchronized void parar(int puerto) {
        Servidor s = SERVIDORES.remove(puerto);
        if (s != null) {
            s.proceso.descendants().forEach(ProcessHandle::destroy);
            s.proceso.destroy();
        }
    }

    /** Al cerrar el juego: los cierra todos y no deja arrancar más. */
    public static void cerrar() {
        cerrando = true;
        parar();
    }

    /** Cierra todos los KoboldCpp que arrancó el juego (el de Jarvis, no). */
    public static synchronized void parar() {
        for (Integer puerto : new ArrayList<>(SERVIDORES.keySet())) parar(puerto);
    }
}
