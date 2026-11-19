package metajavbots.combate;

import metajavbots.api.RobotBase;

/**
 * FÁBRICA — Lo que crea el robot (su código) de un combatiente: el del
 * jugador sale de su código compilado (codigo.Compilador); los de la
 * computadora, de las clases de metajavbots.rivales.
 */
public interface Fabrica {

    RobotBase crear() throws Exception;

    /** Cambia cada vez que cambia el código (para crear el robot de nuevo en la siguiente ronda). */
    default long version() {
        return 0;
    }

    /** El nombre de la clase (para los mensajes de error y el depurador). */
    default String clase() {
        return "";
    }

    /** Las clases que son código del jugador (para encontrar sus líneas en los errores). */
    default boolean esSuya(String nombreDeClase) {
        return false;
    }

    /** El código fuente de este robot (vacío si no está disponible). */
    default String fuente() {
        return "";
    }
}
