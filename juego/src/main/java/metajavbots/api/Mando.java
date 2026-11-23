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

    /** Avisa a tus compañeros (les llega el turno siguiente, en alOirAlEquipo). */
    void avisarAlEquipo(String mensaje);

    /** Lo que pesa lo que cargan tus piernas (el chasis y las piezas menos las piernas). */
    default double pesoTotal() {
        return 0;
    }

    /** Lo que aguantan tus piernas. */
    default double cargaMaxima() {
        return 1;
    }

    /** pesoTotal / cargaMaxima: hasta 0,70 ligero, hasta 1 normal, hasta 1,20 sobrecargado. */
    default double proporcionDeCarga() {
        return 0;
    }

    /** ¿Vas sobrecargado (más del 100 % de la carga)? */
    default boolean sobrecargado() {
        return false;
    }

    /** Tu velocidad máxima ahora mismo (con el peso y el terreno que pisas). */
    default double velocidadMaxima() {
        return 0;
    }

    /** Lo que puedes girar este turno (con el peso y la velocidad que llevas). */
    default double giroMaximo() {
        return 0;
    }

    /** Tu aceleración ahora mismo (con el peso y el terreno). */
    default double aceleracionMaxima() {
        return 0;
    }

    /** Tu locomoción: "BIPEDO", "ORUGAS", "RUEDAS", "FLOTANTE", "VUELO" o "PATAS". */
    default String locomocion() {
        return "";
    }

    /** El terreno que pisas: "LLANO", "ACEITE", "AGUA", "RAMPA", "ESCOMBROS" o "TRAMPA". */
    default String terreno() {
        return "";
    }

    /** La potencia de las armas de potencia regulable (0,1 a 3; Robocode). */
    default void regularPotencia(double potencia) {
    }

    /** El arquetipo del modelo de robot: "ASALTO", "TACTICO" o "DEFENSOR". */
    default String arquetipo() {
        return "";
    }

    /** Habilidad propia de Asalto: embestida con impulso frontal de velocidad. */
    default boolean embestir() {
        return false;
    }

    /** Habilidad propia de Táctico: barrido de radar en 360° con alcance ampliado. */
    default boolean pulsoRadar() {
        return false;
    }

    /** Habilidad propia de Defensor: postura fortificada que reduce el daño recibido. */
    default boolean fortificar() {
        return false;
    }

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
