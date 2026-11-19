package metajavbots.escritorio;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * SELECTOR DE ARCHIVOS DE ESCRITORIO — Abre el explorador nativo del sistema operativo
 * en un hilo propio para no bloquear ni interferir con el bucle gráfico de LWJGL3.
 * En Windows y Linux usa {@link FileDialog} (diálogo nativo del sistema); en macOS usa
 * {@code osascript} para evitar conflictos entre AWT y el hilo principal de GLFW.
 */
public final class SelectorDeArchivosEscritorio {

    private SelectorDeArchivosEscritorio() {
    }

    /** Abre el diálogo nativo de selección de archivo sin bloquear el hilo gráfico. */
    public static void abrir(String titulo, List<String> extensiones, Path inicial, Consumer<Path> alElegir) {
        if (alElegir == null) return;
        Thread hilo = new Thread(() -> {
            Path elegido = null;
            try {
                if (esMac()) {
                    elegido = elegirEnMac(titulo, extensiones, inicial);
                } else if (!GraphicsEnvironment.isHeadless()) {
                    elegido = elegirConAwt(titulo, extensiones, inicial);
                }
            } catch (Throwable t) {
                elegido = null;
            } finally {
                alElegir.accept(elegido);
            }
        }, "selector-archivos");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static Path elegirConAwt(String titulo, List<String> extensiones, Path inicial) {
        Frame marco = new Frame();
        marco.setUndecorated(true);
        marco.setAlwaysOnTop(true);
        FileDialog dialogo = new FileDialog(marco, titulo == null ? "" : titulo, FileDialog.LOAD);
        try {
            Path dirInicial = carpetaDe(inicial);
            if (dirInicial != null) dialogo.setDirectory(dirInicial.toAbsolutePath().toString());
            if (extensiones != null && !extensiones.isEmpty()) {
                dialogo.setFilenameFilter((dir, nombre) -> coincideExtension(nombre, extensiones));
                if (esWindows()) dialogo.setFile(patronWindows(extensiones));
            }
            dialogo.setVisible(true);
            String dir = dialogo.getDirectory();
            String archivo = dialogo.getFile();
            if (dir != null && archivo != null && !archivo.isBlank() && !archivo.contains("*")) {
                return Path.of(dir, archivo);
            }
            return null;
        } finally {
            dialogo.dispose();
            marco.dispose();
        }
    }

    /** En macOS, osascript abre el Finder nativo en un proceso aparte sin tocar AppKit/AWT desde el proceso GLFW. */
    private static Path elegirEnMac(String titulo, List<String> extensiones, Path inicial) throws IOException, InterruptedException {
        StringBuilder script = new StringBuilder("POSIX path of (choose file");
        if (titulo != null && !titulo.isBlank()) {
            script.append(" with prompt \"").append(escaparAppleScript(titulo)).append('"');
        }
        List<String> limpias = extensionesLimpias(extensiones);
        if (!limpias.isEmpty()) {
            script.append(" of type {");
            for (int i = 0; i < limpias.size(); i++) {
                if (i > 0) script.append(", ");
                script.append('"').append(escaparAppleScript(limpias.get(i))).append('"');
            }
            script.append('}');
        }
        Path dirInicial = carpetaDe(inicial);
        if (dirInicial != null) {
            script.append(" default location POSIX file \"").append(escaparAppleScript(dirInicial.toAbsolutePath().toString())).append('"');
        }
        script.append(')');
        Process proceso = new ProcessBuilder("osascript", "-e", script.toString()).start();
        String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        if (proceso.waitFor() == 0 && !salida.isEmpty()) return Path.of(salida);
        return null;
    }

    private static Path carpetaDe(Path inicial) {
        if (inicial == null) return null;
        if (Files.isDirectory(inicial)) return inicial;
        Path padre = inicial.getParent();
        return padre != null && Files.isDirectory(padre) ? padre : null;
    }

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean esMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    static String patronWindows(List<String> extensiones) {
        List<String> limpias = extensionesLimpias(extensiones);
        if (limpias.isEmpty()) return "*.*";
        List<String> patrones = new ArrayList<>(limpias.size());
        for (String ext : limpias) patrones.add("*." + ext);
        return String.join(";", patrones);
    }

    static boolean coincideExtension(String nombre, List<String> extensiones) {
        if (nombre == null || extensiones == null || extensiones.isEmpty()) return true;
        int punto = nombre.lastIndexOf('.');
        if (punto <= 0 || punto >= nombre.length() - 1) return false;
        String ext = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        for (String valida : extensiones) {
            if (valida != null && ext.equals(valida.strip().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static List<String> extensionesLimpias(List<String> extensiones) {
        List<String> l = new ArrayList<>();
        if (extensiones == null) return l;
        for (String ext : extensiones) {
            if (ext != null && !ext.isBlank()) {
                String limpia = ext.strip().replaceAll("^\\.+", "").toLowerCase(Locale.ROOT);
                if (!limpia.isEmpty()) l.add(limpia);
            }
        }
        return l;
    }

    private static String escaparAppleScript(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
