package castillafantasx.paisaje;

import java.util.ArrayDeque;

import castillafantasx.superficie.MapaSuperficie;

/**
 * PUEBLO — Lo que hace que una aldea no parezca una cuadrícula: cada casa
 * se pinta unos píxeles movida de su sitio (ninguna se alinea del todo con
 * la de al lado), con su pared y su tejado de un tono algo distinto (cal
 * más o menos limpia, adobe, tapial; teja nueva o vieja), y las cercas y
 * las tapias hacen un poco de eses en vez de ir a cordel.
 *
 * Solo cambia el dibujo: cada casa y cada cerca estorba donde siempre.
 */
public final class Pueblo {

    private static final int TAM = MapaSuperficie.TAM;
    private static final String CASAS = "hdnkq";            // las letras de casa y de cobertizo del mapa
    // Cifras de dibujo, PROVISIONALES.
    private static final int VAIVEN_DE_CASA = 3;            // píxeles que se mueve una casa, a lo más, hacia cada lado
    private static final float VAIVEN_DE_CERCA = 7f;        // píxeles que se tuerce una cerca, entre un lado y otro
    /** Los tonos de pared y de tejado (cada uno multiplica el color del dibujo: rojo, verde y azul). */
    private static final float[][] PAREDES = { { 1f, 1f, 1f }, { 1f, 0.93f, 0.82f }, { 0.92f, 0.90f, 0.86f }, { 1f, 0.88f, 0.74f }, { 0.96f, 0.96f, 0.90f } };
    private static final float[][] TEJADOS = { { 1f, 1f, 1f }, { 0.86f, 0.80f, 0.74f }, { 0.95f, 0.86f, 0.70f }, { 0.80f, 0.84f, 0.80f } };

    private final MapaSuperficie mapa;
    private final int[][] casa;         // un número distinto para cada casa (0 = no hay)

    Pueblo(MapaSuperficie mapa) {
        this.mapa = mapa;
        casa = new int[mapa.filas][mapa.columnas];
        int siguiente = 1;
        // Cada casa: las casillas de casa que se tocan (las de un cobertizo no se juntan con las de una casa).
        ArrayDeque<int[]> cola = new ArrayDeque<>();
        for (int f = 0; f < mapa.filas; f++) {
            for (int c = 0; c < mapa.columnas; c++) {
                if (!esCasa(c, f) || casa[f][c] != 0) continue;
                boolean cobertizo = esCobertizo(c, f);
                casa[f][c] = siguiente;
                cola.add(new int[] { c, f });
                while (!cola.isEmpty()) {
                    int[] aqui = cola.poll();
                    int[][] lados = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
                    for (int[] lado : lados) {
                        int cc = aqui[0] + lado[0], ff = aqui[1] + lado[1];
                        if (!esCasa(cc, ff) || casa[ff][cc] != 0 || esCobertizo(cc, ff) != cobertizo) continue;
                        casa[ff][cc] = siguiente;
                        cola.add(new int[] { cc, ff });
                    }
                }
                siguiente++;
            }
        }
    }

    private boolean esCasa(int c, int f) {
        return c >= 0 && f >= 0 && c < mapa.columnas && f < mapa.filas && CASAS.indexOf(mapa.suelo(c, f)) >= 0;
    }

    private boolean esCobertizo(int c, int f) {
        return mapa.suelo(c, f) == 'k' || mapa.suelo(c, f) == 'q';
    }

    /** El número de la casa de esa casilla, o 0 si no es de ninguna. */
    private int casaDe(int c, int f) {
        return esCasa(c, f) ? casa[f][c] : 0;
    }

    /** Un número de 0 a 1, fijo para cada casa. */
    private static float suerte(int casa, int semilla) {
        return (Ruido.entero(casa, 0, semilla) & 0xFFFF) / 65535f;
    }

    /** Los píxeles que se pinta movida a la derecha la casa de esa casilla (0 si no hay casa; los cobertizos no se mueven). */
    public int dx(int c, int f) {
        int n = casaDe(c, f);
        return n == 0 || esCobertizo(c, f) ? 0 : Math.round((suerte(n, 801) - 0.5f) * 2 * VAIVEN_DE_CASA);
    }

    /** Y hacia abajo. */
    public int dy(int c, int f) {
        int n = casaDe(c, f);
        return n == 0 || esCobertizo(c, f) ? 0 : Math.round((suerte(n, 802) - 0.5f) * 2 * VAIVEN_DE_CASA);
    }

    /** El tono de la pared de la casa de esa casilla: {rojo, verde, azul}, de 0 a 1. */
    public float[] pared(int c, int f) {
        return PAREDES[(int) (suerte(casaDe(c, f), 803) * PAREDES.length * 0.999f)];
    }

    /** El tono de su tejado. */
    public float[] tejado(int c, int f) {
        return TEJADOS[(int) (suerte(casaDe(c, f), 804) * TEJADOS.length * 0.999f)];
    }

    /** Los píxeles que se tuerce hacia un lado el tramo de cerca de esa casilla: cambia poco a poco a lo largo de la cerca. */
    public int vaivenDeCerca(int c, int f) {
        return Math.round((Ruido.suave(c * TAM, f * TAM, 6, 805) - 0.5f) * VAIVEN_DE_CERCA);
    }
}
