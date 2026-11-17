package castillafantasx.datos;

/**
 * EL JUEGO — Lo que el motor sabe del juego que corre encima: su nombre y su héroe (res/datos/juego.txt).
 * El motor no lleva escrito ningún nombre del juego: los textos que lo necesitan lo piden aquí.
 */
public final class ElJuego {

    private ElJuego() { }

    private static LectorSecciones archivo;

    private static String valor(String clave, String siNoEsta) {
        if (archivo == null) archivo = new LectorSecciones("/datos/juego.txt");
        return archivo.valor("juego", clave, siNoEsta);
    }

    /** Cómo se llama el juego (lo de «nombre» en res/datos/juego.txt). */
    public static String nombre() {
        return valor("nombre", "el juego");
    }

    /** El héroe, como se le nombra en los textos («el héroe»). */
    public static String heroe() {
        return valor("heroe", "el héroe");
    }

    /** Las mazmorras del juego, en orden: el nombre de cada archivo de res/mundo/mazmorras/. */
    public static String[] mazmorras() {
        String lista = valor("mazmorras", "");
        return lista.isEmpty() ? new String[0] : lista.trim().split("\\s*,\\s*");
    }

    /** El héroe, como se le llama en los datos («id_del_heroe»). */
    public static String idDelHeroe() {
        return valor("id_del_heroe", "heroe");
    }
}
