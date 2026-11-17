package metajavbots.api;

/**
 * TREFFER — Ein Treffer: den du einsteckst (beiTreffer) oder austeilst (beiEigenemTreffer).
 */
public interface Treffer {

    /** Wie viel Panzerung er gekostet hat. */
    double schaden();

    /** Womit: "laser", "kugel", "rakete", "nahkampf", "zusammenstoss" oder "wand". */
    String waffe();

    /** Woher er kam (Grad, absolut). */
    double winkel();

    /** Welches Teil getroffen wurde: "kopf", "links", "rechts" oder "beine". */
    String teil();
}
