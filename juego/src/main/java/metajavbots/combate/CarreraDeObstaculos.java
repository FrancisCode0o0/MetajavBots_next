package metajavbots.combate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.api.RobotBase;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * CARRERA DE OBSTÁCULOS — Lógica headless de una carrera en pista larga (6×)
 * desde la salida (izquierda) hasta la meta (derecha), con rotación de arenas,
 * obstáculos visibles/ocultos/repentinos, robots defectuosos destructibles,
 * objetos recogibles de impulso de velocidad y combate permitido contra el rival.
 */
public final class CarreraDeObstaculos {

    /** Ancho de referencia de la pista base original (antes de la ampliación 6×). */
    public static final double ANCHO_BASE_REFERENCIA = 1400;
    /** Ancho de la pista de carrera en unidades de arena (exactamente 6× la pista base). */
    public static final double ANCHO = 8400;
    /** Alto de la pista de carrera en unidades de arena. */
    public static final double ALTO = 800;
    /** Coordenada X de la línea de salida. */
    public static final double SALIDA_X = 110;
    /** Coordenada X de la línea de meta. */
    public static final double META_X = 8240;
    /** Duración máxima de una carrera en turnos (proporcional 6× para pista larga). */
    public static final int MAX_TURNOS = 7200;
    /** Recompensa exacta en monedas por cada carrera ganada por el jugador. */
    public static final double RECOMPENSA_MONEDAS = 1.0;

    /** La simulación física subyacente (reutiliza toda la física de combate y obstáculos). */
    public final Pelea pelea;
    /** Semilla determinista de la pista. */
    public final long semilla;
    /** Escenario/arena seleccionado para esta carrera (nunca repite el inmediato anterior). */
    public final String escenario;
    /** Si el rival es la computadora (true) o un robot ajeno (false). */
    public final boolean contraMaquina;
    /** Nivel de dificultad aplicado a esta carrera. */
    public final DificultadCarrera dificultad;

    private final List<PistaDeCarrera.ObstaculoRepentino> repentinos;
    private final List<PistaDeCarrera.DefectuosoEnPista> defectuosos;
    private boolean terminada;
    private int ganador = -2;
    private String motivoFin = "";
    private boolean recompensaAplicada;

    /** Crea una nueva carrera de obstáculos lista para ejecutarse turno a turno (dificultad por defecto). */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina) {
        this(jugador, rival, semilla, contraMaquina, null, DificultadCarrera.porDefecto());
    }

    /** Crea una nueva carrera evitando repetir {@code arenaAnterior} (dificultad por defecto). */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina, String arenaAnterior) {
        this(jugador, rival, semilla, contraMaquina, arenaAnterior, DificultadCarrera.porDefecto());
    }

    /** Crea una nueva carrera evitando repetir {@code arenaAnterior} con el nivel de dificultad indicado. */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina,
            String arenaAnterior, DificultadCarrera dificultad) {
        this.semilla = semilla;
        this.contraMaquina = contraMaquina;
        this.dificultad = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        this.escenario = PistaDeCarrera.siguienteArena(arenaAnterior, semilla);
        this.defectuosos = PistaDeCarrera.crearRobotsDefectuosos(ANCHO, ALTO, semilla, this.dificultad);
        this.repentinos = PistaDeCarrera.planearRepentinos(ANCHO, ALTO, semilla, this.dificultad);

        int total = 2 + defectuosos.size();
        Combatiente[] cs = new Combatiente[total];
        int[] equipos = new int[total];
        cs[0] = jugador;
        equipos[0] = 0;
        cs[1] = rival;
        equipos[1] = 1;
        for (int i = 0; i < defectuosos.size(); i++) {
            cs[2 + i] = defectuosos.get(i).combatiente();
            equipos[2 + i] = 2;
        }
        this.pelea = new Pelea(cs, equipos, semilla, ANCHO, ALTO, MAX_TURNOS);
        this.pelea.rondas = 1;
        this.pelea.arena = PistaDeCarrera.generarArena(ANCHO, ALTO, semilla, escenario, this.dificultad);
        this.pelea.empezarRonda();
        colocarEnSalida();
    }

    private void colocarEnSalida() {
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        if (c0 != null) {
            c0.x = SALIDA_X;
            c0.y = ALTO * 0.34;
            c0.rumbo = 0;
            c0.mira = 0;
            c0.velocidad = 0;
            c0.detener();
        }
        if (c1 != null) {
            c1.x = SALIDA_X;
            c1.y = ALTO * 0.66;
            c1.rumbo = 0;
            c1.mira = 0;
            c1.velocidad = 0;
            c1.detener();
        }
        for (int i = 0; i < defectuosos.size(); i++) {
            Cuerpo d = pelea.cuerpo(2 + i);
            PistaDeCarrera.DefectuosoEnPista info = defectuosos.get(i);
            if (d != null) {
                d.x = info.x();
                d.y = info.y();
                d.rumbo = 180;
                d.mira = 180;
                d.velocidad = 0;
                d.detener();
            }
        }
    }

    /** Avanza un turno de la carrera, activa obstáculos repentinos y evalúa las condiciones de victoria. */
    public void avanzar() {
        if (terminada) return;
        pelea.avanzar();
        PistaDeCarrera.actualizarRepentinos(repentinos, pelea, dificultad);
        evaluarFin();
    }

    private void evaluarFin() {
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        boolean fuera0 = c0 == null || c0.fuera;
        boolean fuera1 = c1 == null || c1.fuera;
        boolean meta0 = !fuera0 && c0.x >= META_X;
        boolean meta1 = !fuera1 && c1.x >= META_X;
        if (meta0 || meta1) {
            terminada = true;
            motivoFin = "meta";
            if (meta0 && meta1) {
                double diff = c0.x - c1.x;
                ganador = Math.abs(diff) < 1e-3 ? -1 : diff > 0 ? 0 : 1;
            } else {
                ganador = meta0 ? 0 : 1;
            }
            return;
        }
        if (fuera0 || fuera1) {
            terminada = true;
            motivoFin = "ko";
            ganador = fuera0 && fuera1 ? -1 : fuera0 ? 1 : 0;
            return;
        }
        if (pelea.estado() == Pelea.Estado.TERMINADA) {
            terminada = true;
            motivoFin = "tiempo";
            double diff = c0.x - c1.x;
            ganador = Math.abs(diff) < 5.0 ? -1 : diff > 0 ? 0 : 1;
        }
    }

    /** Indica si la carrera ya ha concluido. */
    public boolean terminada() {
        return terminada;
    }

    /** Ganador de la carrera: 0 jugador, 1 rival, -1 empate, -2 en curso. */
    public int ganador() {
        return ganador;
    }

    /** Motivo de finalización: "meta", "ko", "tiempo" o "" si sigue en curso. */
    public String motivoFin() {
        return motivoFin;
    }

    /** Escenario/arena asignado a esta carrera. */
    public String escenario() {
        return escenario;
    }

    /** Nivel de dificultad configurado para esta carrera. */
    public DificultadCarrera dificultad() {
        return dificultad;
    }

    /** Comprueba si una dificultad de carrera está desbloqueada para el jugador dado. */
    public static boolean dificultadDesbloqueada(Jugador jugador, DificultadCarrera dificultad) {
        return dificultad != null && dificultad.disponiblePara(jugador);
    }

    /** Progreso normalizado [0, 1] del corredor indicado hacia la línea de meta. */
    public double progreso(int indice) {
        Cuerpo c = pelea.cuerpo(indice);
        if (c == null) return 0;
        return Math.max(0, Math.min(1, (c.x - SALIDA_X) / (META_X - SALIDA_X)));
    }

    /** Obstáculos visibles en el mapa general previo (excluye ocultos e impulsos). */
    public List<Arena.Trampa> obstaculosVisiblesEnMapa() {
        List<Arena.Trampa> visibles = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.ocultoEnMapa && !t.repentino && !t.tipo.equals("impulso")) visibles.add(t);
        }
        return visibles;
    }

    /** Obstáculos que permanecen ocultos en el mapa general inicial. */
    public List<Arena.Trampa> obstaculosOcultosEnMapa() {
        List<Arena.Trampa> ocultos = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (t.ocultoEnMapa) ocultos.add(t);
        }
        return ocultos;
    }

    /** Objetos recogibles de impulso de velocidad actualmente activos en la pista. */
    public List<Arena.Trampa> impulsosActivos() {
        List<Arena.Trampa> impulsos = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.destruida() && t.tipo.equals("impulso")) impulsos.add(t);
        }
        return impulsos;
    }

    /** Lista completa de obstáculos repentinos planificados para esta carrera. */
    public List<PistaDeCarrera.ObstaculoRepentino> obstaculosRepentinos() {
        return Collections.unmodifiableList(repentinos);
    }

    /** Cuerpos de los robots defectuosos colocados en la pista. */
    public List<Cuerpo> robotsDefectuosos() {
        List<Cuerpo> lista = new ArrayList<>();
        for (int i = 2; i < pelea.combatientes.length; i++) {
            Cuerpo c = pelea.cuerpo(i);
            if (c != null) lista.add(c);
        }
        return lista;
    }

    /** Aplica la recompensa de 1 moneda si el jugador ganó la carrera (idempotente). */
    public boolean aplicarRecompensa(Jugador jugador) {
        if (!terminada || ganador != 0 || recompensaAplicada || jugador == null) return false;
        jugador.sumarMonedas(RECOMPENSA_MONEDAS);
        recompensaAplicada = true;
        return true;
    }

    /** Indica si ya se otorgó la recompensa de moneda de esta carrera. */
    public boolean recompensaAplicada() {
        return recompensaAplicada;
    }

    /** Selecciona la arena siguiente distinta de {@code anterior}. */
    public static String siguienteArena(String anterior, long semilla) {
        return PistaDeCarrera.siguienteArena(anterior, semilla);
    }

    /** Construye el combatiente del jugador a partir del robot elegido de {@code Jugador.robots} con su montaje real. */
    public static Combatiente combatienteDeJugador(Jugador jugador, int indiceRobot, Fabrica fabrica) {
        if (jugador != null && !jugador.robots.isEmpty()) {
            int idx = Math.max(0, Math.min(indiceRobot, jugador.robots.size() - 1));
            Jugador.Robot r = jugador.robots.get(idx);
            Montaje m = jugador.montajeDe(r);
            return new Combatiente(r.nombre, m != null ? m : new Montaje(Modelo.de("cobalto")), fabrica);
        }
        String nombre = jugador != null && jugador.nombre != null && !jugador.nombre.isBlank() ? jugador.nombre : "Piloto";
        return new Combatiente(nombre, new Montaje(Modelo.de("cobalto")), fabrica);
    }

    /** Crea el combatiente rival controlado por la computadora para la carrera de obstáculos (dificultad por defecto). */
    public static Combatiente rivalMaquina(String nombre) {
        return rivalMaquina(nombre, DificultadCarrera.porDefecto());
    }

    /** Crea el combatiente rival controlado por la computadora adaptado a la dificultad indicada. */
    public static Combatiente rivalMaquina(String nombre, DificultadCarrera dificultad) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        Montaje montaje = new Montaje(Modelo.de("cobalto"));
        Fabrica fabrica = new Fabrica() {
            @Override
            public RobotBase crear() {
                return new PistaDeCarrera.CorredorMaquina(dif);
            }

            @Override
            public long version() {
                return 1;
            }

            @Override
            public String clase() {
                return PistaDeCarrera.CorredorMaquina.class.getName();
            }

            @Override
            public boolean esSuya(String nombreDeClase) {
                return PistaDeCarrera.CorredorMaquina.class.getName().equals(nombreDeClase);
            }
        };
        Combatiente c = new Combatiente(nombre, montaje, fabrica);
        c.computadora = true;
        c.escalaDano = dif.escalaDanoRival;
        return c;
    }

    /**
     * Devuelve el código base deliberadamente torpe de Carrera de obstáculos en ES, DE o EN.
     * Da giros sin rumbo y tropieza con todo; el jugador debe programar su estrategia y pulsar Compilar.
     */
    public static String plantillaInicial(Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        switch (id) {
            case DE:
                return "import metajavbots.api.*;\n\n"
                        + "// Werkscode (absichtlich ungeschickt): dreht sich nur planlos im Kreis.\n"
                        + "// Schreib deine eigene Logik mit hindernisse(), geseheneGegner(),\n"
                        + "// Tempo-Boosts (\"impulso\"), Schüssen und if-Bedingungen, und drück Kompilieren.\n"
                        + "public class HindernisLaeufer extends MetajavbotDe {\n"
                        + "    @Override\n"
                        + "    public void jedenZug() {\n"
                        + "        // TODO: hindernisse() prüfen, Fässer/defekte Roboter abschießen, Boosts einsammeln.\n"
                        + "        drehen(28);\n"
                        + "        vorwaerts(14);\n"
                        + "    }\n"
                        + "}\n";
            case EN:
                return "import metajavbots.api.*;\n\n"
                        + "// Factory code (deliberately clumsy): just spins around aimlessly.\n"
                        + "// Write your own logic with obstacles(), seenEnemies(), speed boosts\n"
                        + "// (\"impulso\"), shots, and if conditionals, then press Compile.\n"
                        + "public class ObstacleRacer extends MetajavbotEn {\n"
                        + "    @Override\n"
                        + "    public void onTick() {\n"
                        + "        // TODO: check obstacles(), shoot barrels/broken robots, grab speed boosts.\n"
                        + "        turn(28);\n"
                        + "        ahead(14);\n"
                        + "    }\n"
                        + "}\n";
            default:
                return "import metajavbots.api.*;\n\n"
                        + "// Código de fábrica (torpe a propósito): solo da giros sin rumbo.\n"
                        + "// Escribe tu propia lógica con obstaculos(), enemigosVistos(), impulsos\n"
                        + "// (\"impulso\"), disparos y condicionales if, y pulsa Compilar.\n"
                        + "public class CorredorObstaculos extends Metajavbot {\n"
                        + "    @Override\n"
                        + "    public void cadaTurno() {\n"
                        + "        // TODO: leer obstaculos(), disparar a barriles/robots rotos y recoger impulsos.\n"
                        + "        girar(28);\n"
                        + "        avanzar(14);\n"
                        + "    }\n"
                        + "}\n";
        }
    }
}
