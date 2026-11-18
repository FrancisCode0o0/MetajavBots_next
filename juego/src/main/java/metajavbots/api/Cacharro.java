package metajavbots.api;

/**
 * CACHARRO — Un aparato en los tres idiomas a la vez: Aparato (español),
 * Geraet (alemán) y Gadget (inglés). Lo crea la pelea; el jugador lo recibe
 * como Aparato, Geraet o Gadget según la fachada que use.
 */
public interface Cacharro extends Aparato, Geraet, Gadget {

    /** El nombre es de marca (Jetzazo, Flotón…): el mismo en los tres idiomas. */
    @Override
    default String name() {
        return nombre();
    }

    @Override
    default String typ() {
        return tipo();
    }

    @Override
    default String type() {
        return tipo();
    }

    @Override
    default double entfernung() {
        return distancia();
    }

    @Override
    default double distance() {
        return distancia();
    }

    @Override
    default double winkel() {
        return angulo();
    }

    @Override
    default double angle() {
        return angulo();
    }

    @Override
    default boolean angeschlossen() {
        return conectado();
    }

    @Override
    default boolean pluggedIn() {
        return conectado();
    }

    @Override
    default boolean bereit() {
        return listo();
    }

    @Override
    default boolean ready() {
        return listo();
    }
}
