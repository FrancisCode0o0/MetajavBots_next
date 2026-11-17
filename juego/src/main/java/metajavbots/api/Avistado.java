package metajavbots.api;

/**
 * AVISTADO — Un enemigo visto por el radar, en los tres idiomas a la vez:
 * Enemigo (español), Gegner (alemán) y Enemy (inglés). Lo crea la pelea; el
 * jugador lo recibe como Enemigo, Gegner o Enemy según la fachada que use.
 */
public interface Avistado extends Enemigo, Gegner, Enemy {
}
