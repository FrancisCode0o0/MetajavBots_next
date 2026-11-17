package castillafantasx.figura;

import castillafantasx.datos.LectorSecciones;

/**
 * CIFRAS DE LA VISTA — El tamaño y el sitio de los brazos de primera persona,
 * leídos una vez de res/datos/vista.txt (ahí se explica cada cifra). Todas PROVISIONALES.
 */
// TODO-FRAN: todo esto se ajusta en res/datos/vista.txt, sin tocar el código.
public final class CifrasDeVista {

    private CifrasDeVista() { }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/vista.txt");

    /** true: la vista despejada; false: la de antes del encargo de la vista. */
    public static final boolean DESPEJADA = !"no".equals(ARCHIVO.valor("brazos", "despejada", "si"));
    /** true: brazos, manos y armas a la misma escala y con las posturas sacadas del maniquí (remate de la vista). */
    public static final boolean NATURAL = !"no".equals(ARCHIVO.valor("brazos", "natural", "si"));
    public static final double TAMANO = numero("brazos", "tamano");
    public static final double SEPARA = numero("brazos", "separa");
    public static final double ALTO = numero("brazos", "alto");
    public static final double FONDO = numero("brazos", "fondo");

    private static double numero(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) throw new IllegalStateException("vista.txt: falta «" + clave + "» en [" + seccion + "]");
        return Double.parseDouble(texto);
    }
}
