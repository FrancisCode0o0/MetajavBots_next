package metajavbots.api;

/**
 * CUSTOM PART — Java contract in English for designing your own robot part
 * from the Workshop. Implement this interface, choose its slot ({@code HEAD},
 * {@code ARM}, {@code LEGS}, or {@code CORE}), and keep your stats within the
 * balance budget (maximum 100 points).
 */
public interface CustomPart extends PiezaPropia {

    /** Visible name of your part (1 to 28 characters). */
    String name();

    /** Mounting slot: {@code "HEAD"}, {@code "ARM"}, {@code "LEGS"}, or {@code "CORE"}. */
    String slot();

    /** Part nature: {@code "ACERO"}, {@code "RAYO"}, {@code "FOCO"}, {@code "FURIA"}, {@code "INGENIO"}, or {@code "ETER"}. */
    default String nature() {
        return "ACERO";
    }

    /** Style according to slot (weapon, head special, or locomotion). */
    default String style() {
        return "LASER";
    }

    /** Bonus armor provided by this part. */
    default double armor() {
        return 10;
    }

    /** Weight of this part. */
    default double weight() {
        return 6;
    }

    /** Damage per attack (for {@code ARM} only). */
    default double damage() {
        return 9;
    }

    /** Weapon range (for {@code ARM} only). */
    default double range() {
        return 260;
    }

    /** Cooldown in ticks (for {@code ARM} only). */
    default double cooldown() {
        return 18;
    }

    /** Energy cost per use. */
    default double energy() {
        return 10;
    }

    /** Radar cone angle in degrees (for {@code HEAD} only). */
    default double cone() {
        return 50;
    }

    /** Base speed (for {@code LEGS} only). */
    default double speed() {
        return 4.0;
    }

    /** Turn speed in degrees per tick (for {@code LEGS} only). */
    default double turn() {
        return 6.0;
    }

    /** Acceleration per tick (for {@code LEGS} only). */
    default double acceleration() {
        return 0.5;
    }

    /** Maximum carry capacity (for {@code LEGS} only). */
    default double capacity() {
        return 32;
    }

    /** Computation steps per tick (for {@code CORE} only). */
    default double steps() {
        return 3200;
    }

    /** Maximum energy pool (for {@code CORE} only). */
    default double maxEnergy() {
        return 105;
    }

    /** Energy recharge per tick (for {@code CORE} only). */
    default double recharge() {
        return 0.42;
    }

    @Override
    default String nombre() {
        return name();
    }

    @Override
    default String ranura() {
        return slot();
    }

    @Override
    default String naturaleza() {
        return nature();
    }

    @Override
    default String estilo() {
        return style();
    }

    @Override
    default double blindaje() {
        return armor();
    }

    @Override
    default double peso() {
        return weight();
    }

    @Override
    default double dano() {
        return damage();
    }

    @Override
    default double alcance() {
        return range();
    }

    @Override
    default double enfriamiento() {
        return cooldown();
    }

    @Override
    default double energia() {
        return energy();
    }

    @Override
    default double cono() {
        return cone();
    }

    @Override
    default double velocidad() {
        return speed();
    }

    @Override
    default double giro() {
        return turn();
    }

    @Override
    default double aceleracion() {
        return acceleration();
    }

    @Override
    default double carga() {
        return capacity();
    }

    @Override
    default double pasos() {
        return steps();
    }

    @Override
    default double energiaMax() {
        return maxEnergy();
    }

    @Override
    default double recarga() {
        return recharge();
    }
}
