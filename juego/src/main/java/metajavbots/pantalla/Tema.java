package metajavbots.pantalla;

/**
 * TEMA — Los colores de la interfaz. Oscuro por defecto (como los editores
 * profesionales, y descansa la vista), con uno claro para quien lo prefiera.
 * El contraste del texto con el fondo pasa de 7:1 (lo que pide WCAG AAA).
 */
public final class Tema {

    public final int fondo, panel, panelAlto, borde, texto, textoSuave, textoApagado, acento, acentoTexto, bien, mal, aviso,
            seleccion, lineaActual, numeros;
    /** El coloreado del código: palabras clave, tipos, nombres, números, textos, comentarios, anotaciones, llamadas. */
    public final int clave, tipo, nombre, numero, cadena, comentario, anotacion, metodo, operador;

    private Tema(int[] c) {
        fondo = c[0];
        panel = c[1];
        panelAlto = c[2];
        borde = c[3];
        texto = c[4];
        textoSuave = c[5];
        textoApagado = c[6];
        acento = c[7];
        acentoTexto = c[8];
        bien = c[9];
        mal = c[10];
        aviso = c[11];
        seleccion = c[12];
        lineaActual = c[13];
        numeros = c[14];
        clave = c[15];
        tipo = c[16];
        nombre = c[17];
        numero = c[18];
        cadena = c[19];
        comentario = c[20];
        anotacion = c[21];
        metodo = c[22];
        operador = c[23];
    }

    public static final Tema OSCURO = new Tema(new int[]{
            0x16181D, 0x1F2229, 0x2A2E37, 0x3A3F4B, 0xECEFF4, 0xD5DCE8, 0x9EA7B8, 0x3FA9F5, 0x0B1320, 0x5BD685, 0xFF6B6B, 0xFFC857,
            0x264F78, 0x232731, 0x6E7686,
            0xFF8FB1, 0x7FD3FF, 0xECEFF4, 0xFFB86C, 0xA8E890, 0x8B95A7, 0xE5C07B, 0x82AAFF, 0xC3C9D4});

    public static final Tema CLARO = new Tema(new int[]{
            0xF4F5F7, 0xFFFFFF, 0xE8EAEE, 0xC9CED6, 0x14161A, 0x2B313C, 0x4D5564, 0x0B66C3, 0xFFFFFF, 0x1E8A4C, 0xC62828, 0x9A6400,
            0xBBD6F7, 0xEEF2F8, 0x8A92A3,
            0xA626A4, 0x0B6E99, 0x14161A, 0xB35A00, 0x2E7D32, 0x6A737D, 0x8A6D00, 0x1E50B3, 0x3A404B});

    public static Tema de(boolean claro) {
        return claro ? CLARO : OSCURO;
    }
}
