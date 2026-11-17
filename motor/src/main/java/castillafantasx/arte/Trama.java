package castillafantasx.arte;

/**
 * TRAMA — La trama de Bayer 4×4: reparte en puntitos el paso de un tono al
 * siguiente (como en los juegos de PS1), para los dibujos hechos píxel a píxel.
 */
public final class Trama {

    private Trama() { }

    /** El orden en que se encienden los 16 puntitos de cada cuadro de 4×4 píxeles. */
    public static final int[][] BAYER = { { 0, 8, 2, 10 }, { 12, 4, 14, 6 }, { 3, 11, 1, 9 }, { 15, 7, 13, 5 } };

    /** El tono de una rampa (0xRRGGBB, de oscuro a claro) que toca para un valor de 0 a 1, tramado en el píxel (x, y). */
    public static int tono(int[] rampa, float valor, int x, int y) {
        float v = Math.max(0, Math.min(0.999f, valor)) * (rampa.length - 1);
        int escalon = (int) v;
        if (v - escalon > (BAYER[y & 3][x & 3] + 0.5f) / 16f) escalon++;
        return rampa[escalon];
    }

    /** Lo mismo, ya opaco (0xFFRRGGBB). */
    public static int tonoOpaco(int[] rampa, float valor, int x, int y) {
        return 0xFF000000 | tono(rampa, valor, x, y);
    }
}
