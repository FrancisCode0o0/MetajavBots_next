package metajavbots.api;

/**
 * GEGNER — Was dein Radar über einen feindlichen Roboter weiß, den es gerade
 * sieht. Kommt in beiSicht(Gegner g) und in gesehen().
 *
 * Thema: Interfaces (13). Gegner ist ein Interface: Es sagt, WAS du fragen
 * kannst, nicht WIE es berechnet wird.
 */
public interface Gegner {

    /** Wo er steht (in der Arena). */
    double x();

    double y();

    /** Wie weit er von dir entfernt ist. */
    double entfernung();

    /** Der Winkel (in Grad, absolut), unter dem du ihn siehst: zielenNach(g.winkel()). */
    double winkel();

    /** Wohin er fährt (Grad). */
    double richtung();

    /** Wie schnell er ist (Einheiten pro Zug; negativ rückwärts). */
    double geschwindigkeit();

    /** Seine restliche Panzerung, von 0 bis 100. */
    double panzerung();

    /** Sein Name. */
    String name();

    /** Sein Modell (zum Beispiel "cobalto"). */
    String modell();

    /** Fliegt er? (Nahkampfschläge erreichen ihn nicht). */
    boolean fliegt();
}
