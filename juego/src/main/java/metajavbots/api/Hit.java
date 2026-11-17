package metajavbots.api;

/**
 * HIT — A hit: one you take (onHit) or one you land (onHitEnemy).
 */
public interface Hit {

    /** How much armor it took. */
    double damage();

    /** With what: "laser", "bullet", "missile", "melee", "ram" or "wall". */
    String weapon();

    /** Where it came from (absolute degrees). */
    double angle();

    /** Which part was hit: "head", "left", "right" or "legs". */
    String part();
}
