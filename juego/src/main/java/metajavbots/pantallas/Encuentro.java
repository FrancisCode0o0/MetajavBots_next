package metajavbots.pantallas;

import java.util.function.Consumer;

import metajavbots.combate.Combatiente;
import metajavbots.pantalla.VistaDeArena;

/**
 * ENCUENTRO — Cómo es una pelea antes de empezar: contra quién, en qué modo
 * (el que se apunta en el historial), en qué escenario, con qué fuerza sale
 * la computadora y lo especial de cada modo (el objetivo de un tutorial, el
 * tema que exige un jefe, el tope de cálculo estricto).
 */
public final class Encuentro {

    public Combatiente rival;
    /** ADAPTADA, FACIL…EXTREMA, AZAR, DESAFIO, JEFE, TEMARIO, PRACTICA, AJENO, TUTORIAL. */
    public String modo = "ADAPTADA";
    public double nivelRival = 0.3;
    public VistaDeArena.Escenario escenario = VistaDeArena.Escenario.HANGAR;
    /** Las rondas (4 en una pelea; 1 en una práctica o un tutorial). */
    public int rondas = 4;
    /** Si no es 0, el tope de pasos por turno para los dos (el modo «tope de cálculo»). */
    public long topeDePasos;
    /** El jefe de un tema (o null): solo se le hace daño si el código usa su tema. */
    public Jefe jefe;
    /** El objetivo de un tutorial («aciertos>=3»…), o null. */
    public String objetivo;
    /** Lo que se hace al acabar (con si se ganó); puede ser null. */
    public Consumer<Boolean> alAcabar;
    /** Si cuenta para el progreso (el historial y la etiqueta). */
    public boolean cuenta = true;
    public String titulo = "";
    /**
     * Los demás de una pelea de varios (battle royale o por equipos), además del jugador (equipo 0) y del rival
     * (equipo 1): cada uno con su equipo en «equiposDeLosDemas». Vacío en una pelea normal.
     */
    public final java.util.List<Combatiente> demas = new java.util.ArrayList<>();
    public final java.util.List<Integer> equiposDeLosDemas = new java.util.ArrayList<>();
    /** Qué cerebro de IA elige la táctica de los robots de la computadora entre rondas (EstiloPorIA: el rápido, uno, mezclados…). */
    public int cerebros = EstiloPorIA.EL_RAPIDO;
    /** Lo que se cuenta al ganar (el final de un capítulo de campaña), o null. */
    public String alGanar;
    /** Cómo se prepara la misma pelea otra vez («Revancha»), o null para hacerlo según el modo. */
    public java.util.function.Supplier<Encuentro> otraVez;
    /** Los temas de pelea, con energía: el himno ochentero, el rock y el de artes marciales. */
    public static final String[] TEMAS_DE_PELEA = {"combate", "rock", "artes_marciales"};
    /** El código con que pelea el jugador en vez del de su robot (un robot roto que arreglar), o null. */
    public String codigo;
    /** El cuerpo con que pelea el jugador en vez del de su robot (el del robot roto), o null. */
    public metajavbots.piezas.Montaje montaje;
    /** El tema de música de la pelea (uno de pelea al azar, o el de la campaña); en el último asalto suena «final». */
    public String musica = TEMAS_DE_PELEA[(int) (Math.random() * TEMAS_DE_PELEA.length)];
}
