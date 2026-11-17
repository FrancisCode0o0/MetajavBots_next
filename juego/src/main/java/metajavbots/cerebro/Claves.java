package metajavbots.cerebro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * CLAVES — Las claves de los modelos en línea (API keys). Se guardan fuera
 * del juego y de su repositorio: en la carpeta del usuario,
 * ~/.metajavbots/clave_<proveedor>.txt, solo legible por él cuando el
 * sistema lo permite. Nunca van a la partida ni a ningún registro.
 */
public final class Claves {

    private Claves() {
    }

    static Path carpeta() {
        return Paths.get(System.getProperty("user.home"), ".metajavbots");
    }

    private static Path archivo(String proveedor) {
        return carpeta().resolve("clave_" + proveedor.replaceAll("[^a-z0-9_-]", "") + ".txt");
    }

    public static String leer(String proveedor) {
        try {
            Path f = archivo(proveedor);
            return Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8).strip() : "";
        } catch (IOException e) {
            return "";
        }
    }

    public static void guardar(String proveedor, String clave) throws IOException {
        Files.createDirectories(carpeta());
        Path f = archivo(proveedor);
        Files.writeString(f, clave == null ? "" : clave.strip(), StandardCharsets.UTF_8);
        try {
            Files.setPosixFilePermissions(f, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException e) {
            // Windows: la carpeta del usuario ya es solo suya
        }
    }

    /** Para enseñarla sin enseñarla: «sk-…a1b2». */
    public static String tapada(String clave) {
        if (clave == null || clave.length() < 8) return clave == null || clave.isEmpty() ? "" : "••••";
        return clave.substring(0, 3) + "…" + clave.substring(clave.length() - 4);
    }
}
