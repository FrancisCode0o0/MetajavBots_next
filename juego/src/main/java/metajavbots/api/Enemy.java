package metajavbots.api;

/**
 * ENEMY — What your radar knows about an enemy robot it can see right now.
 * You get it in onSeeEnemy(Enemy e) and in seenEnemies().
 *
 * Topic: interfaces (13). Enemy is an interface: it says WHAT you can ask,
 * not HOW it is computed.
 */
public interface Enemy {

    /** Where it is (in the arena). */
    double x();

    double y();

    /** How far away it is. */
    double distance();

    /** The absolute angle (degrees) at which you see it: aimTo(e.angle()). */
    double angle();

    /** Where it is heading (degrees). */
    double heading();

    /** How fast it moves (units per tick; negative when reversing). */
    double speed();

    /** Its remaining armor, from 0 to 100. */
    double armor();

    /** Its name. */
    String name();

    /** Its model (for example "cobalto"). */
    String model();

    /** Is it flying? (fliers dodge many melee hits; if it is only jumping or was flipped, none reach it). */
    boolean flies();

    /** Its legs: "BIPEDO", "ORUGAS", "RUEDAS", "FLOTANTE", "VUELO", or "PATAS" (to pick your weapon and tactics). */
    default String locomotion() {
        return "";
    }
}
