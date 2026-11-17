package castillafantasx.editor;

import castillafantasx.arte.Paleta;

/**
 * ESTILO — Los colores, los tamaños de letra y las medidas del editor, en un
 * solo sitio. Todo lo que pinta el editor los toma de aquí: así los temas
 * (nocturno, cálido, blanco) y la lupa de ENCARGO_EDITOR_TEMAS se hacen
 * cambiando solo esta clase.
 */
public final class Estilo {

    private Estilo() { }

    // --- Colores (0xRRGGBB). El tema de ahora es el del juego: oscuro. ---
    // TODO-FRAN: los colores del editor (ENCARGO_EDITOR_TEMAS pondrá aquí los tres temas).
    public static int fondo = Paleta.TINTA;             // el fondo de los paneles
    public static int borde = Paleta.PIEDRA;            // el marco de un panel en reposo
    public static int texto = Paleta.HUESO;             // el texto normal
    public static int textoSuave = Paleta.PIEDRA_CLARA; // valores, ayudas
    public static int textoApagado = Paleta.PIEDRA;     // pies, lo que no se puede usar
    public static int acento = Paleta.ORO;              // lo elegido
    public static int aviso = Paleta.SANGRE_LUZ;        // errores y globitos de tareas
    public static int bien = 0x86A834;                  // «guardado», «recargado»
    public static float alfaDelFondo = 0.88f;           // lo que tapa un panel el mundo

    // Los colores del visor de código (docs/VISOR_DE_CODIGO.md). Contraste con su fondo, medido como WCAG:
    // el texto, 12,6:1; palabras, textos, números y ganchos, más de 8:1; comentarios, 7,8:1; números de línea, 4,5:1.
    public static int codigoFondo = 0x1A1714;          // oscuro y cálido, sin llegar al negro (el blanco «vibra» sobre negro puro)
    public static int codigoTexto = 0xE4D8BE, codigoPalabra = 0xE8B45A, codigoComentario = 0x9FB0A0, codigoGancho = 0x5CE0CA,
            codigoCadena = 0xAACC66, codigoNumero = 0xE0A478, codigoLinea = 0x8C7E70, codigoHallado = 0x5A2A22;
    /** La fila del cursor, el canal de los números y las guías de sangría: apenas se distinguen del fondo. */
    public static int codigoFilaDelCursor = 0x2C2620, codigoCanal = 0x14110F, codigoGuia = 0x3A332C;

    /** El tamaño de la letra del visor de código, en píxeles de la imagen (Ctrl + y Ctrl −). PROVISIONAL. */
    public static float letraDeCodigo = 10;
    public static final float LETRA_DE_CODIGO_MINIMA = 7, LETRA_DE_CODIGO_MAXIMA = 16;
    /** El alto de cada fila de código, por el tamaño de la letra: los libros piden de 1,2 a 1,45. */
    public static final float INTERLINEADO_DE_CODIGO = 1.4f;

    // La guía de una parte, al lado del visor (EDITOR_CODIGO): letra más grande y renglones más anchos que el código,
    // fondo de papel viejo en oscuro y texto cálido, para leer sin cansarse (docs/VISOR_DE_CODIGO.md).
    public static float letraDeGuia = 11;
    public static final float INTERLINEADO_DE_GUIA = 1.6f;
    public static int guiaFondo = 0x221D17, guiaTexto = 0xEDE3CC, guiaTitulo = 0xE8B45A, guiaEjemplo = 0x9FD8C8;
    public static final int ANCHO_DE_LA_GUIA = 230;

    // El color propio de cada categoría (Categoria los toma de aquí, por su orden).
    static final int[] COLORES_DE_CATEGORIA = {
        0x7E9A3E,   // Mundo: verde de hierba
        0xB08A4A,   // Figuras: ocre de madera
        0xC08A66,   // Personajes: piel
        0x5E96B4,   // Luz y tiempo: azul de cielo
        0xA8508A,   // Historia: morado de tinta
        0xC23A2A,   // Combate: sangre
        0xD8A848,   // Código: el oro de la taza
        0x8C949C,   // Sonido: acero
        0xC8B48C,   // Taller: hueso
    };

    /** La lupa: por cuánto se multiplican las medidas del editor (1 = como el juego). */
    // TODO-FRAN: la lupa de ENCARGO_EDITOR_TEMAS cambia este número; las medidas pasan por medida().
    public static float lupa = 1;

    // --- Medidas, en píxeles de la imagen del juego (antes de la lupa) ---
    public static final int RENGLON = 13, ICONO = 12, MARGEN = 6, RELLENO = 8;
    /** El lado mínimo de algo que se toca con el dedo o el lápiz (para la tablet). */
    public static final int TOQUE_MINIMO = 20;

    /** Una medida, pasada por la lupa. */
    public static int medida(int pixeles) {
        return Math.round(pixeles * lupa);
    }
}
