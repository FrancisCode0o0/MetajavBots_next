package metajavbots.pantalla;

import metajavbots.Juego;

/** PANTALLA — Una de las pantallas del juego (el menú, el taller, la pelea…). */
public interface Pantalla {

    /** Se dibuja y atiende al jugador, una vez por fotograma. */
    void fotograma(Juego j, float dt);

    /** Al llegar a esta pantalla. */
    default void alEntrar(Juego j) {
    }

    /** Al irse de ella (para guardar lo que haga falta). */
    default void alSalir(Juego j) {
    }

    /** El tema de música que suena en ella (res/musica/temas.txt). */
    default String musica() {
        return "menu";
    }
}
