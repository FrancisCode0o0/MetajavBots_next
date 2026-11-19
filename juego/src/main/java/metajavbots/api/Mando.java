package metajavbots.api;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MANDO — Lo que el robot puede pedir a la pelea y saber de ella. Lo pone la
 * pelea (metajavbots.combate.Piloto) al empezar cada ronda. Es interno: el
 * código del jugador no lo ve (el cargador de robots lo bloquea); el jugador
 * usa los métodos de su Metajavbot, que llaman a esto.
 *
 * Los ángulos van en grados: 0 es el este (la derecha), 90 el norte (arriba);
 * los giros positivos van a la izquierda, como en Matemáticas. La arena va de
 * (0, 0), abajo a la izquierda, a (anchoArena, altoArena).
 */
public interface Mando {

    /** Los brazos: el izquierdo, el derecho y la cabeza (lo especial). */
    int IZQUIERDA = 0, DERECHA = 1, CABEZA = 2;

    /** Canales de órdenes que pueden pisarse en el mismo turno (solo observación para el depurador). */
    int CANAL_AVANCE = 1, CANAL_GIRO = 2, CANAL_MIRA = 4;

    double x();

    double y();

    double rumbo();

    double mira();

    double velocidad();

    double energia();

    double blindaje();

    int turno();

    int ronda();

    double anchoArena();

    double altoArena();

    double restanteAvance();

    double restanteGiro();

    double restanteMira();

    void avanzar(double distancia);

    void girar(double grados);

    void apuntar(double grados);

    void detener();

    boolean atacar(int brazo);

    boolean listo(int brazo);

    /** El tipo de arma del brazo (Arma.name(): LASER, BALA, MISIL, CUERPO, ESCUDO, REPARAR…); "" si no hay. */
    String arma(int brazo);

    double alcance(int brazo);

    /** Los enemigos que se ven ahora mismo. */
    List<? extends Avistado> vistos();

    /** Los compañeros de equipo (vivos), estén donde estén: el equipo comparte su posición. */
    List<? extends Avistado> companeros();

    /** Las trampas y los objetos de la arena (vacía en las arenas sin nada). */
    List<? extends Cosa> obstaculos();

    /** Los aparatos que hay en el suelo; el que lleva (o null); conectarlo en un hueco; usarlo. */
    List<? extends Cacharro> aparatos();

    Cacharro miAparato();

    boolean conectar(String hueco);

    boolean usarAparato(boolean conAngulo, double angulo);

    /** Manda un mensaje a los compañeros (les llega el turno siguiente, en alOirAlEquipo). */
    void avisarAlEquipo(String mensaje);

    void decir(String texto);

    void escribir(String texto);

    Map<String, Object> memoria();

    Random azar();

    /** Observación para el depurador: método del robot que se está ejecutando ahora. */
    default void enMetodo(String metodo) {
    }

    /** Observación para el depurador: acción pedida y qué canales (avance/giro/mira) toca. */
    default void anotarOrden(String accion, int canales) {
    }
}
