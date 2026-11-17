package castillafantasx.f1;

/**
 * HERRAMIENTA — Una fila de F1 vista como herramienta: su nombre corto, su
 * tema, su icono y la fila de Partida.opcionesDeDepuracion() que acciona.
 * El id no cambia aunque cambie el texto (lo usan las favoritas y los recientes).
 */
public final class Herramienta {

    public final String id;
    /** El nombre corto, el del mosaico, las favoritas y la rueda. */
    public final String nombre;
    /** Su tema; null solo en «Cerrar F1». */
    public final Tema tema;
    /** El nombre de su icono ("f1/" + icono en el Almacen). */
    public final String icono;
    /** La fila de F1 que acciona (Partida.opcion). */
    public final int fila;
    /** Más palabras por las que se encuentra en el buscador. */
    public final String palabras;

    public Herramienta(String id, String nombre, Tema tema, String icono, int fila, String palabras) {
        this.id = id;
        this.nombre = nombre;
        this.tema = tema;
        this.icono = icono;
        this.fila = fila;
        this.palabras = palabras;
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ", fila " + fila + ")";
    }
}
