package castillafantasx.editor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VIGÍA — Mira de vez en cuando los archivos de res/ y dice cuáles han
 * cambiado desde la última vez: así, al guardar en otro programa (Tiled,
 * Inky, un editor de texto), el juego se entera y recarga lo que pueda.
 */
public final class Vigia {

    /** La carpeta que vigila (las pruebas la cambian por una temporal). */
    public static Path carpeta = Path.of("res");
    // TODO-FRAN: cada cuánto mira el vigía si ha cambiado algo (en frames; 60 = un segundo).
    public static final int FRAMES_ENTRE_MIRADAS = 45;

    private final Map<String, Long> vistos = new HashMap<>();
    private boolean primeraVez = true;
    private int frames;
    private static final List<String> NADA = java.util.Collections.emptyList();

    // El aviso de Windows (encargo de la optimización): dice cuándo cambia algo en una carpeta. Así solo se recorre
    // res/ cuando ha cambiado algo, en vez de cada 45 frames (eran unos 10 MB de basura por segundo). Hay uno solo
    // para todos los vigías (cada partida tiene el suyo): cada vez que trae algo, sube 'avisos', y cada vigía mira
    // si ha subido desde su última vez. Si no se puede usar, se mira como antes.
    private static java.nio.file.WatchService aviso;
    private static Path carpetaDelAviso;
    private static boolean sinAviso;
    private static long avisos;
    private long avisosVistos = -1;

    /** Cada frame: cuando toca, mira (si el aviso dice que algo ha cambiado). Devuelve los archivos cambiados ("ink/aldea.ink"), o una lista vacía. */
    public List<String> mirar() {
        if (++frames < FRAMES_ENTRE_MIRADAS) return NADA;
        frames = 0;
        if (!hayAviso()) return mirarYa();
        if (avisosVistos == avisos) return NADA;
        avisosVistos = avisos;
        return mirarYa();
    }

    /** Pone el aviso (la primera vez o si las pruebas cambian la carpeta) y recoge lo que haya traído. false: no hay aviso. */
    private static boolean hayAviso() {
        if (sinAviso) return false;
        try {
            if (aviso == null || !carpeta.equals(carpetaDelAviso)) {
                if (aviso != null) aviso.close();
                aviso = carpeta.getFileSystem().newWatchService();
                carpetaDelAviso = carpeta;
                vigilarCarpetas();
                avisos++;           // cada vigía toma nota de todo al empezar
            }
            boolean algo = false;
            for (java.nio.file.WatchKey k = aviso.poll(); k != null; k = aviso.poll()) {
                k.pollEvents();
                k.reset();
                algo = true;
            }
            if (algo) {
                vigilarCarpetas();     // por si hay carpetas nuevas
                avisos++;
            }
            return true;
        } catch (IOException | RuntimeException e) {
            sinAviso = true;           // sin aviso de Windows: se mira como antes
            return false;
        }
    }

    /** Pone el aviso en res/ y en cada carpeta de dentro (las que ya lo tienen no cambian). */
    private static void vigilarCarpetas() throws IOException {
        if (!Files.isDirectory(carpeta)) return;
        Files.walkFileTree(carpeta, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes atributos) throws IOException {
                dir.register(aviso, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE, java.nio.file.StandardWatchEventKinds.ENTRY_DELETE,
                        java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Mira ahora. La primera vez solo toma nota de cómo está todo. */
    public List<String> mirarYa() {
        List<String> cambiados = new ArrayList<>();
        if (!Files.isDirectory(carpeta)) return cambiados;
        Map<String, Long> ahora = new HashMap<>();
        try {
            // Con la fecha y el tamaño que ya trae la lista de cada carpeta, sin preguntar a Windows por cada archivo
            // (eso costaba unos 50 ms en el hilo del juego cada vez que miraba: un tirón cada 45 frames).
            Files.walkFileTree(carpeta, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path a, BasicFileAttributes atributos) {
                    // La fecha y el tamaño juntos: un número que cambia si el archivo cambia.
                    if (atributos.isRegularFile()) ahora.put(carpeta.relativize(a).toString().replace('\\', '/'), atributos.lastModifiedTime().toMillis() * 31 + atributos.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path a, IOException e) {
                    return FileVisitResult.CONTINUE;      // un archivo a medio guardar: se mira la próxima vez
                }
            });
        } catch (IOException | java.io.UncheckedIOException e) {
            return cambiados;
        }
        if (!primeraVez) {
            for (Map.Entry<String, Long> a : ahora.entrySet()) {
                if (!a.getValue().equals(vistos.get(a.getKey()))) cambiados.add(a.getKey());
            }
        }
        primeraVez = false;
        vistos.clear();
        vistos.putAll(ahora);
        java.util.Collections.sort(cambiados);
        return cambiados;
    }
}
