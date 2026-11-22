package metajavbots.api;

/**
 * OBSTÁCULO — Una trampa o un objeto de la arena: pinchos que salen del
 * suelo, sierras en las esquinas, un imán, un foso, barriles, un carrito que
 * va y viene… Te llegan todos en obstaculos() (las arenas sin nada, vacía).
 * Esquívalos, o empuja al rival hacia ellos.
 *
 * Tema: interfaces (13) y colecciones (15).
 */
public interface Obstaculo {

    /**
     * Qué es: "pinchos", "sierra", "iman", "foso", "barril", "carrito", "chimenea",
     * "rayo", "bloque", "trampa_mortal" (destrucción instantánea), "impulso" o "reparacion".
     */
    String tipo();

    /** Dónde está su centro (en la arena). */
    double x();

    double y();

    /** Lo que ocupa: un círculo de este radio alrededor del centro. */
    double radio();

    /** Lo lejos que está su centro de ti. */
    double distancia();

    /** El ángulo (en grados, absoluto) en que lo ves desde donde estás. */
    double angulo();

    /** ¿Hace daño ahora mismo? (los pinchos, solo cuando han salido). */
    boolean peligroso();

    /** ¿Es sólido? (no se puede atravesar: hay que rodearlo). */
    boolean solido();

    /** Hacia dónde se mueve (grados) y a qué velocidad (0 si está quieto). */
    double rumbo();

    double velocidad();
}
