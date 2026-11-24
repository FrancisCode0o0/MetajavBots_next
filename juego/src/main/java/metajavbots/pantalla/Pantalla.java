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

    /** El tema de Java del que va (res/datos/temas.txt), si va de uno: el entrenador flotante empieza por él. */
    default String temaDeJava() {
        return null;
    }

    /**
     * Lo que el jugador tiene delante, para que el entrenador conteste con IA mirándolo («¿qué hace mi código
     * en esta línea?», «¿por qué pierdo?»): el código con números de línea, los errores, cómo va la pelea…
     */
    default String contextoParaElEntrenador() {
        return null;
    }

    /** El código que el jugador está escribiendo, si es un robot (el entrenador da tips mirándolo); null si no. */
    default String codigoParaConsejos() {
        return null;
    }

    /** Si el entrenador está en el hospital (contra los jefes no ayuda: tutor/Hospital). */
    default boolean entrenadorEnElHospital() {
        return false;
    }

    /**
     * El id de su «¿Cómo se juega?» (res/datos/como_se_juega.txt; pantallas/ComoSeJuega), si es un modo que se juega:
     * sale solo la primera vez y con el botón de la cabecera. null si no tiene.
     */
    default String comoSeJuega() {
        return null;
    }

    /** El tema de música que suena en ella (res/musica/temas.txt). */
    default String musica() {
        return "menu";
    }

    /**
     * Cuánto mide la barra de botones de abajo de esta pantalla (0 si no tiene): el cartel de los consejos del ayudante
     * de la esquina sube por encima de ella para no tapar sus botones.
     */
    default float reservaAbajo() {
        return 0;
    }
}
