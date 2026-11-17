package castillafantasx;

/**
 * PÁJARO — El modo pájaro (herramienta de desarrollo, desde F1): una cámara
 * libre que vuela sobre el mapa sin mover al héroe, para ir de zona en
 * zona y ver qué hay en cada una. Vale en la superficie (comarca y bosque) y
 * en las mazmorras (allí se ve su plano desde arriba).
 *
 * Aquí solo se guarda dónde mira la cámara; la partida la mueve con las
 * teclas y el dibujo la usa.
 */
public final class Pajaro {

    // --- Cifras PROVISIONALES ---
    /** Lo que avanza en cada frame, en píxeles de la imagen (más alejada, más mapa recorre). */
    public static final float PASO = 9f;
    /** Con la tecla de correr, tantas veces más deprisa. */
    public static final float DEPRISA = 3f;
    /** Lo más que se aleja: la imagen abarca tantas veces más mapa a lo ancho. */
    public static final int ALEJADO_MAXIMO = 8;

    /** El centro de la vista: en la superficie, píxeles del mapa; en la mazmorra, casillas. */
    public float x, y;
    /** Cuánto se ha alejado: 1 (como el juego), 2, 4 u 8. */
    public int alejado = 1;
    /** Está sobre una mazmorra (si no, sobre la superficie). */
    public boolean enMazmorra;
    /** El último lugar con nombre al que saltó (-1: ninguno todavía). */
    public int lugar = -1;
    /** El nombre de ese lugar, para enseñarlo un momento. */
    public String rotulo = "";

    public void acercar() {
        if (alejado > 1) alejado /= 2;
    }

    public void alejar() {
        if (alejado < ALEJADO_MAXIMO) alejado *= 2;
    }
}
