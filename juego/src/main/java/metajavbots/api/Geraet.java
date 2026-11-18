package metajavbots.api;

/**
 * GERAET — Ein Gerät, das von Runde zu Runde in der Arena auftaucht:
 * Düsentriebwerk, Antigravitationsantrieb, Elektroschock, Flammenwerfer, Öl,
 * Kleber … Die auf dem Boden bekommst du in geraete(), deins in meinGeraet().
 * Um es anzubringen, lies seinen Code (der Geräte-Knopf im Kampf): Er sagt,
 * an welchen Platz es kommt (anschliessen("ruecken") …) und wie man es benutzt.
 *
 * Thema: Interfaces (13) und Komposition (ein Roboter aus Teilen).
 */
public interface Geraet {

    /** Was es ist: "chorro" (Düse), "antigravedad", "descarga" (Schock), "lanzallamas" (Flammenwerfer), "aceite" (Öl), "pegamento" (Kleber). */
    String typ();

    /** Sein Markenname (Jetzazo, Flotón, Chispazo …), zum Sagen. */
    String name();

    /** Wo es ist (wenn du es trägst, wo du bist). */
    double x();

    double y();

    /** Wie weit es von dir entfernt ist und in welchem Winkel du es siehst (Grad, absolut). */
    double entfernung();

    double winkel();

    /** Ist es schon an deinem Roboter angeschlossen? (Nur deins.) */
    boolean angeschlossen();

    /** Kann man es schon benutzen? (Angeschlossen und aufgeladen.) */
    boolean bereit();
}
