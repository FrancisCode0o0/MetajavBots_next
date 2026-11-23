package metajavbots.progreso;

import java.nio.file.Path;
import java.nio.file.Paths;

import castillafantasx.Cierre;
import metajavbots.sonido.Musica;

/**
 * CARPETAS — Dónde deja el juego lo del jugador: partidas, registros de
 * errores, música personal y robots exportados o ajenos. Con jugar.bat (desde
 * el código) es la carpeta desde la que se lanza; desde el paquete listo para
 * jugar (con su propio Java dentro) es la carpeta del usuario, ~/MetajavBots,
 * porque la del paquete puede ser de solo lectura y se borra al actualizar.
 */
public final class Carpetas {

    /** Adonde van los robots exportados (Exportar). */
    public static Path exportados = Paths.get("robots_exportados");
    /** De donde se leen los robots de otros jugadores (Preparar pelea). */
    public static Path ajenos = Paths.get("robots_ajenos");

    private Carpetas() {
    }

    /** ¿Se juega desde el paquete hecho con jpackage? (su lanzador pone jpackage.app-path). */
    public static boolean desdeElPaquete() {
        return System.getProperty("jpackage.app-path") != null || "si".equals(System.getProperty("metajavbots.paquete"));
    }

    /** La carpeta del jugador: MetajavBots dentro de su carpeta personal (user.home). */
    public static Path delUsuario() {
        return Paths.get(System.getProperty("user.home", "."), "MetajavBots");
    }

    /** Pone todo lo del jugador dentro de esa carpeta. */
    public static void usar(Path base) {
        Guardado.carpeta = base.resolve("partidas");
        Musica.carpetaPersonal = base.resolve("musica_personal");
        Cierre.carpeta = base.resolve("registro");
        exportados = base.resolve("robots_exportados");
        ajenos = base.resolve("robots_ajenos");
    }
}
