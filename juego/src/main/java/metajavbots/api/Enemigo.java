package metajavbots.api;

/**
 * ENEMIGO — Lo que tu radar sabe de un robot enemigo que ve ahora mismo.
 * Te llega en alVerEnemigo(Enemigo e) y en enemigosVistos().
 *
 * Tema: interfaces (13). Enemigo es una interfaz: dice QUÉ puedes preguntar,
 * no CÓMO se calcula. La pelea tiene su propia clase que la implementa.
 */
public interface Enemigo {

    /** Dónde está (en la arena). */
    double x();

    double y();

    /** Lo lejos que está de ti. */
    double distancia();

    /** El ángulo (en grados, absoluto) en que lo ves desde donde estás: apuntarHacia(e.angulo()). */
    double angulo();

    /** Hacia dónde va él (grados). */
    double rumbo();

    /** Lo rápido que va (unidades por turno; negativo si va marcha atrás). */
    double velocidad();

    /** Lo que le queda de blindaje, de 0 a 100. */
    double blindaje();

    /** Su nombre. */
    String nombre();

    /** Su modelo (por ejemplo, "cobalto"). */
    String modelo();

    /** ¿Va volando? (los golpes cuerpo a cuerpo no le llegan). */
    boolean vuela();
}
