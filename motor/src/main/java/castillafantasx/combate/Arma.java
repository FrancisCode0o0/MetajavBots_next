package castillafantasx.combate;

/**
 * ARMA — Lo que el motor sabe de un arma del juego (cada juego tiene las suyas): de momento,
 * su nombre en los datos (el que se escribe en los mapas de las mazmorras). Lo demás lo sabe el juego.
 */
public interface Arma {

    /** Su nombre en los datos y en los mapas, en minúsculas. */
    String id();

    /** El arma del juego con ese id, o null si no hay ninguna (castillafantasx.Contenido.arma). */
    static Arma porId(String id) {
        return castillafantasx.Contenido.delJuego().arma(id);
    }
}
