package metajavbots.api;

/**
 * HINDERNIS — Eine Falle oder ein Gegenstand in der Arena: Stacheln, die aus
 * dem Boden kommen, Sägen in den Ecken, ein Magnet, eine Grube, Fässer, ein
 * Wagen, der hin und her fährt … Du bekommst alle in hindernisse() (in Arenen
 * ohne etwas: leer). Weiche ihnen aus oder schieb den Gegner hinein.
 *
 * Thema: Interfaces (13) und Collections (15).
 */
public interface Hindernis {

    /**
     * Was es ist: "pinchos" (Stacheln), "sierra" (Säge), "iman" (Magnet), "foso" (Grube), "barril" (Fass),
     * "carrito" (Wagen), "trampa_mortal" (tödliche Falle), "impulso" (Boost), "reparacion" (Reparaturbonus),
     * "moneda" (Extramünze), "rampa" (Sprungrampe), "agua" (Wasserzone mit Kurzschluss) …
     */
    String typ();

    /** Wo seine Mitte ist (in der Arena). */
    double x();

    double y();

    /** Was es einnimmt: ein Kreis mit diesem Radius um die Mitte. */
    double radius();

    /** Wie weit seine Mitte von dir entfernt ist. */
    double entfernung();

    /** Der Winkel (in Grad, absolut), in dem du es von deinem Platz aus siehst. */
    double winkel();

    /** Schadet es gerade? (Die Stacheln nur, wenn sie draußen sind.) */
    boolean gefaehrlich();

    /** Ist es fest? (Man kommt nicht hindurch: Man muss außen herum.) */
    boolean fest();

    /** Wohin es sich bewegt (Grad) und wie schnell (0, wenn es stillsteht). */
    double richtung();

    double geschwindigkeit();
}
