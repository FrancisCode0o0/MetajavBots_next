package metajavbots.pantalla;

import java.io.InputStream;

/**
 * FUENTES — Las tipografías libres (SIL OFL 1.1) diferenciadas por contexto:
 * interfaz y lectura (IBM Plex Sans Regular), títulos y cabeceras técnicas
 * (Space Grotesk Bold), editor de código, visores y pseudocódigo (JetBrains Mono
 * Regular y Bold) y respaldo de símbolos técnicos y del juego (Noto Sans Symbols 1 y 2).
 */
public final class Fuentes {

    /** Fuente sans-serif de alta claridad y regularidad para interfaz, descripciones y tutoriales. */
    public static final String INTERFAZ = "fuentes/IBMPlexSans-Regular.ttf";
    /** Fuente geométrica técnica en negrita para títulos, cabeceras, botones y marcadores. */
    public static final String TITULO = "fuentes/SpaceGrotesk-Bold.ttf";
    /** Fuente monoespaciada moderna y clara para el editor de código, visores y pseudocódigo. */
    public static final String CODIGO = "fuentes/JetBrainsMono-Regular.ttf";
    /** Variante en negrita de la fuente de código (números de línea activos, avisos del depurador). */
    public static final String CODIGO_NEGRITA = "fuentes/JetBrainsMono-Bold.ttf";
    /** Fuente de respaldo para símbolos generales y del juego (⚔ ⚙ ⚠ …). */
    public static final String SIMBOLOS = "fuentes/NotoSansSymbols-Regular.ttf";
    /** Fuente de respaldo complementaria para símbolos técnicos e iconos (▶ ✎ ✕ ▤ ▣ ❚…). */
    public static final String SIMBOLOS_2 = "fuentes/NotoSansSymbols2-Regular.ttf";

    /** Todas las fuentes tipográficas empaquetadas en el proyecto. */
    public static final String[] TODAS = {INTERFAZ, TITULO, CODIGO, CODIGO_NEGRITA, SIMBOLOS, SIMBOLOS_2};

    /** Archivos de licencia SIL OFL 1.1 junto a las fuentes. */
    public static final String[] LICENCIAS = {
            "fuentes/LICENCIA_IBMPlexSans_OFL.txt",
            "fuentes/LICENCIA_SpaceGrotesk_OFL.txt",
            "fuentes/LICENCIA_JetBrainsMono_OFL.txt",
            "fuentes/LICENCIA_NotoSansSymbols2_OFL.txt"};

    private Fuentes() {
    }

    /** Abre un recurso de fuente o licencia desde el classpath (null si no existe). */
    public static InputStream abrir(String ruta) {
        return Fuentes.class.getResourceAsStream("/" + ruta);
    }
}
