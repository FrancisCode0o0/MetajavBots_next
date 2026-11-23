package metajavbots.combate;

/**
 * CAÍDAS DEL CIELO EN LA CARRERA — Cuántos robots averiados caen del cielo en
 * cada Carrera de obstáculos, por dificultad y subnivel, y en qué tramo de la
 * pista. Mucho menos a menudo que en Destrucción y supervivencia: unos pocos
 * por carrera, repartidos entre la salida y la meta, siempre por delante de
 * quien va en cabeza y con la misma sombra de aviso, para que la carrera siga
 * siendo justa.
 */
public final class CaidasDelCieloEnCarrera {

    // TODO-FRAN: cuántos robots caen por dificultad y subnivel
    /** Filas: Noob, Junior, Senior, Especial. Columnas: subnivel I, II, III. */
    private static final int[][] CAIDAS = {
        {1, 2, 2},
        {2, 3, 4},
        {3, 4, 5},
        {5, 6, 7},
    };

    /** Avance del que va en cabeza (0 = salida, 1 = meta) al que puede caer el primero. */
    public static final double PRIMER_TRAMO = 0.12;
    /** Avance del que va en cabeza a partir del cual ya no se programan más caídas. */
    public static final double ULTIMO_TRAMO = 0.86;
    /** Distancia mínima por delante del que va en cabeza a la que cae un robot. */
    public static final double DELANTE_MIN = 380.0;
    /** Distancia máxima por delante del que va en cabeza a la que cae un robot. */
    public static final double DELANTE_MAX = 900.0;

    private CaidasDelCieloEnCarrera() {
    }

    /** Robots que caen del cielo en una carrera de esa dificultad y subnivel (1..3). */
    public static int cuantas(DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int fila = Math.min(CAIDAS.length - 1, dif.ordinal());
        int columna = DificultadCarrera.normalizarSubnivel(subnivel) - DificultadCarrera.SUBNIVEL_MIN;
        return CAIDAS[fila][Math.min(CAIDAS[fila].length - 1, columna)];
    }

    /** El máximo de la tabla: cuántos robots de reserva hacen falta como mucho. */
    public static int maximo() {
        int m = 0;
        for (int[] fila : CAIDAS) for (int n : fila) m = Math.max(m, n);
        return m;
    }
}
