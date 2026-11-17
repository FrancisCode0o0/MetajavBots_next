package castillafantasx.lumbre;



/**
 * LUMBRE SUELTA — Un farol o una antorcha que el héroe ha dejado en el
 * suelo, encendidos: alumbran un círculo más pequeño que en la mano, y la
 * antorcha sigue gastándose. Se recoge con la tecla de acción, como un arma
 * caída. Vale en la superficie (x, y en píxeles del mapa) y en la mazmorra
 * (x, y en casillas).
 */
public class LumbreSuelta {

    public final Lumbre clase;
    public final double x, y;
    /** Segundos que le quedan a la antorcha (el farol no se gasta: no cuenta). */
    public double resto;

    public LumbreSuelta(Lumbre clase, double x, double y, double resto) {
        this.clase = clase;
        this.x = x;
        this.y = y;
        this.resto = resto;
    }

    /** Pasan esos segundos: la antorcha se gasta. Devuelve false cuando se ha consumido del todo. */
    public boolean arder(double segundos) {
        if (clase != Lumbre.ANTORCHA) return true;
        resto -= segundos;
        return resto > 0;
    }

    /** Lo que alumbra ahí en el suelo, respecto a su radio o su alcance en la mano; la antorcha mengua cuando se acaba. */
    public float parte() {
        float queda = clase == Lumbre.ANTORCHA ? (float) Math.min(1, resto / SEGUNDOS_APAGANDOSE) : 1;
        return Lumbre.EN_EL_SUELO * (0.4f + 0.6f * queda);
    }

    /** En sus últimos segundos, la antorcha alumbra cada vez menos. */
    public static final double SEGUNDOS_APAGANDOSE = 12;
}
