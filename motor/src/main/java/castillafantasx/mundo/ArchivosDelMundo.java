package castillafantasx.mundo;

import java.nio.file.Path;

/** ARCHIVOS DEL MUNDO — Dónde se escriben los mapas que cambia el editor (res/mundo/; las pruebas ponen otra carpeta). */
public final class ArchivosDelMundo {

    private ArchivosDelMundo() { }

    /** La carpeta de los mapas que se escriben (las pruebas ponen otra). */
    public static Path carpeta = Path.of("res/mundo");

    /** El archivo de un mapa de la superficie (o de un interior). */
    public static Path archivoDe(String mapa) {
        return carpeta.resolve(mapa + ".txt");
    }

    /** El archivo de una mazmorra. */
    public static Path archivoDeMazmorra(String id) {
        return carpeta.resolve("mazmorras").resolve(id + ".txt");
    }
}
