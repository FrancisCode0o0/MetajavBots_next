package metajavbots.api;

/**
 * OBSTACLE — A trap or an object in the arena: spikes that pop out of the
 * floor, saws in the corners, a magnet, a pit, barrels, a cart going back and
 * forth… You get them all in obstacles() (empty in arenas with nothing).
 * Dodge them, or push your rival into them.
 *
 * Topic: interfaces (13) and collections (15).
 */
public interface Obstacle {

    /** What it is: "pinchos" (spikes), "sierra" (saw), "iman" (magnet), "foso" (pit), "barril" (barrel), "carrito" (cart)… */
    String type();

    /** Where its centre is (in the arena). */
    double x();

    double y();

    /** What it takes up: a circle of this radius around its centre. */
    double radius();

    /** How far its centre is from you. */
    double distance();

    /** The angle (in degrees, absolute) at which you see it from where you are. */
    double angle();

    /** Does it hurt right now? (The spikes only when they are out.) */
    boolean dangerous();

    /** Is it solid? (You can't go through it: you have to go round.) */
    boolean solid();

    /** Where it is moving (degrees) and how fast (0 if it stands still). */
    double heading();

    double speed();
}
