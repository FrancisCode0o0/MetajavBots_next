package metajavbots.api;

/**
 * APARATO — Un cacharro que aparece en la arena de ronda en ronda: motor a
 * chorro, propulsor antigravedad, descarga eléctrica, lanzallamas, aceite,
 * pegamento… Los que hay en el suelo te llegan en aparatos(); el que llevas,
 * en miAparato(). Para ponértelo, lee su código (el botón del aparato en la
 * pelea): dice a qué hueco se conecta (conectar("espalda")…) y cómo se usa.
 *
 * Tema: interfaces (13) y composición (un robot hecho de piezas).
 */
public interface Aparato {

    /** Qué es: "chorro", "antigravedad", "descarga", "lanzallamas", "aceite", "pegamento", "iman" o "martillo". */
    String tipo();

    /** Su nombre de marca (Jetzazo, Flotón, Chispazo…), para decirlo. */
    String nombre();

    /** Dónde está (si lo llevas, dónde estás tú). */
    double x();

    double y();

    /** Lo lejos que está de ti y en qué ángulo lo ves (grados, absoluto). */
    double distancia();

    double angulo();

    /** ¿Ya está conectado a tu robot? (Solo los que llevas.) */
    boolean conectado();

    /** ¿Se puede usar ya? (Conectado y recargado.) */
    boolean listo();
}
