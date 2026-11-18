package metajavbots.api;

/**
 * COSA — Un obstáculo de la arena en los tres idiomas a la vez: Obstaculo
 * (español), Hindernis (alemán) y Obstacle (inglés). Lo crea la pelea; el
 * jugador lo recibe como Obstaculo, Hindernis u Obstacle según la fachada.
 */
public interface Cosa extends Obstaculo, Hindernis, Obstacle {

    @Override
    default String typ() {
        return tipo();
    }

    @Override
    default String type() {
        return tipo();
    }

    @Override
    default double radius() {
        return radio();
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
    default boolean gefaehrlich() {
        return peligroso();
    }

    @Override
    default boolean dangerous() {
        return peligroso();
    }

    @Override
    default boolean fest() {
        return solido();
    }

    @Override
    default boolean solid() {
        return solido();
    }

    @Override
    default double richtung() {
        return rumbo();
    }

    @Override
    default double heading() {
        return rumbo();
    }

    @Override
    default double geschwindigkeit() {
        return velocidad();
    }

    @Override
    default double speed() {
        return velocidad();
    }
}
