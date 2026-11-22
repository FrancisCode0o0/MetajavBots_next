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
    /** Duración máxima base de una carrera en turnos (nivel Noob; escala con la longitud de pista). */
    public static final int MAX_TURNOS = 7200;
    /** Recompensa exacta en monedas por cada carrera ganada por el jugador. */
    public static final double RECOMPENSA_MONEDAS = 1.0;
    /** Centésimas de moneda otorgadas por cada obstáculo destructible (barril o chimenea) destruido (+0,2). */
    public static final int CENTIMOS_OBJETO_DESTRUIDO = 20;
    /** Centésimas de moneda otorgadas por cada robot defectuoso destruido (+0,4). */
    public static final int CENTIMOS_DEFECTUOSO_DESTRUIDO = 40;
    /** Centésimas de moneda otorgadas por destruir al rival principal antes de terminar (+1,0). */
    public static final int CENTIMOS_RIVAL_DESTRUIDO = 100;
    /** Centésimas de moneda otorgadas por cada moneda extra recogida en zona peligrosa (+1,0). */
    public static final int CENTIMOS_MONEDA_EXTRA = 100;
    /** Centésimas de moneda otorgadas por carrera perfecta con medalla de oro (+1/4 = +0,25). */
    public static final int CENTIMOS_PERFECCION = 25;
    /** Umbral máximo de daño recibido (%) para considerar la victoria como carrera perfecta (medalla de oro y +1/4 de moneda). */
    public static final double UMBRAL_PERFECTO = 1.0;
    /** Umbral máximo de daño recibido (%) para obtener medalla de plata. */
    public static final double UMBRAL_PLATA = 20.0;
    /** Umbral máximo de daño recibido (%) para obtener medalla de bronce. */
    public static final double UMBRAL_BRONCE = 50.0;

    /** Medalla otorgada al ganar la carrera según lo poco que haya sido dañado el robot. */
    public enum Medalla {
        NINGUNA("carrera_obs.medalla_ninguna", 0x9AA3AD, false),
        BRONCE("carrera_obs.medalla_bronce", 0xCD7F32, false),
        PLATA("carrera_obs.medalla_plata", 0xD0D8E2, false),
        ORO("carrera_obs.medalla_oro", 0xFFD23F, true);

        /** Clave de localización en ui.txt. */
        public final String claveTexto;
        /** Color RGB representativo de la medalla. */
        public final int color;
        /** Indica si otorga +1/4 de moneda adicional por carrera perfecta. */
        public final boolean esPerfecta;

        Medalla(String claveTexto, int color, boolean esPerfecta) {
            this.claveTexto = claveTexto;
            this.color = color;
            this.esPerfecta = esPerfecta;
        }
    }

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
    /** Subnivel de dificultad (1 = I, 2 = II, 3 = III). */
    public final int subnivel;
    /** Longitud total de esta pista en unidades de arena (escala ×4/3 por nivel desde Noob). */
    public final double anchoPista;
    /** Coordenada X de la línea de meta para esta dificultad. */
    public final double metaX;
    /** Límite máximo de turnos escalado proporcionalmente a la longitud de la pista. */
    public final int maxTurnos;

    private final List<PistaDeCarrera.ObstaculoRepentino> repentinos;
    private final List<PistaDeCarrera.DefectuosoEnPista> defectuosos;
    private boolean terminada;
    private boolean rendido;
    private int ganador = -2;
    private String motivoFin = "";
    private boolean recompensaAplicada;
    private int cuartosGanados;
    private int monedasExtraPorCuartos;
    private int monedasExtraRecogidas;
    private int objetosDestruidos;
    private int defectuososDestruidos;
    private boolean rivalDestruido;
    private int centimosGanadosTotal;
    private int monedasEnterasGanadas;
    private int bonosBrutosCentimos;
    private boolean bonosReducidosPorDerrota;

    /** Longitud de la pista para la dificultad indicada (Noob = 8400, ×4/3 cada nivel). */
    public static double anchoPara(DificultadCarrera dificultad) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        return Math.round(ANCHO * dif.escalaPista);
    }

    /** Coordenada X de la línea de meta para la dificultad indicada. */
    public static double metaPara(DificultadCarrera dificultad) {
        return anchoPara(dificultad) - 160.0;
    }

    /** Límite máximo de turnos para la dificultad indicada, proporcional a la longitud de pista. */
    public static int maxTurnosPara(DificultadCarrera dificultad) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        return (int) Math.round(MAX_TURNOS * dif.escalaPista);
    }

    /** Crea una nueva carrera de obstáculos lista para ejecutarse turno a turno (dificultad por defecto). */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina) {
        this(jugador, rival, semilla, contraMaquina, null, DificultadCarrera.porDefecto());
    }

    /** Crea una nueva carrera evitando repetir {@code arenaAnterior} (dificultad por defecto). */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina, String arenaAnterior) {
        this(jugador, rival, semilla, contraMaquina, arenaAnterior, DificultadCarrera.porDefecto());
    }

    /** Crea una nueva carrera evitando repetir {@code arenaAnterior} con el nivel de dificultad indicado (subnivel I). */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina,
            String arenaAnterior, DificultadCarrera dificultad) {
        this(jugador, rival, semilla, contraMaquina, arenaAnterior, dificultad, DificultadCarrera.SUBNIVEL_MIN);
    }

    /** Crea una nueva carrera evitando repetir {@code arenaAnterior} con dificultad y subnivel (1..3) indicados. */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina,
            String arenaAnterior, DificultadCarrera dificultad, int subnivel) {
        this.semilla = semilla;
        this.contraMaquina = contraMaquina;
        this.dificultad = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        this.subnivel = DificultadCarrera.normalizarSubnivel(subnivel);
        this.anchoPista = anchoPara(this.dificultad);
        this.metaX = metaPara(this.dificultad);
        this.maxTurnos = maxTurnosPara(this.dificultad);
        this.escenario = PistaDeCarrera.siguienteArena(arenaAnterior, semilla);
        this.defectuosos = PistaDeCarrera.crearRobotsDefectuosos(anchoPista, ALTO, semilla, this.dificultad, this.subnivel);
        this.repentinos = PistaDeCarrera.planearRepentinos(anchoPista, ALTO, semilla, this.dificultad, this.subnivel);

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
        this.pelea = new Pelea(cs, equipos, semilla, anchoPista, ALTO, maxTurnos);
        this.pelea.rondas = 1;
        this.pelea.multiplicadorDano = this.dificultad.multiplicadorDano;
        this.pelea.arena = PistaDeCarrera.generarArena(anchoPista, ALTO, semilla, escenario, this.dificultad, this.subnivel);
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

    /** Avanza un turno de la carrera, activa obstáculos repentinos, registra botín y evalúa las condiciones de victoria. */
    public void avanzar() {
        if (terminada) return;
        pelea.avanzar();
        procesarSucesosDelTurno();
        PistaDeCarrera.actualizarRepentinos(repentinos, pelea, dificultad);
        evaluarFin();
    }

    private void procesarSucesosDelTurno() {
        for (Suceso s : pelea.sucesos()) {
            if (s.tipo == Suceso.Tipo.OBSTACULO_ROTO && s.quien == 0
                    && (s.texto.equals("barril") || s.texto.equals("chimenea"))) {
                objetosDestruidos++;
            } else if (s.tipo == Suceso.Tipo.ESPECIAL && s.quien == 0
                    && s.texto.equals(Arena.TIPO_MONEDA_EXTRA)) {
                monedasExtraRecogidas++;
            } else if (s.tipo == Suceso.Tipo.FUERA_DE_COMBATE) {
                Cuerpo caido = pelea.cuerpo(s.quien);
                if (caido != null && caido.ultimoAtacante == 0) {
                    if (s.quien == 1) rivalDestruido = true;
                    else if (s.quien >= 2 && caido.combatiente.defectuoso) defectuososDestruidos++;
                }
            }
        }
    }

    private void evaluarFin() {
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        boolean fuera0 = c0 == null || c0.fuera;
        boolean fuera1 = c1 == null || c1.fuera;
        boolean meta0 = !fuera0 && c0.x >= metaX;
        boolean meta1 = !fuera1 && c1.x >= metaX;
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

    /** Finaliza la carrera inmediatamente por rendición del jugador, sin otorgar ninguna recompensa. */
    public void rendirse() {
        if (terminada) return;
        rendido = true;
        terminada = true;
        ganador = 1;
        motivoFin = "rendicion";
    }

    /** Indica si la carrera terminó porque el jugador pulsó Rendirse. */
    public boolean rendido() {
        return rendido;
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

    /** Subnivel de dificultad configurado para esta carrera (1 = I, 2 = II, 3 = III). */
    public int subnivel() {
        return subnivel;
    }

    /** Multiplicador total de recompensas aplicado por la combinación de dificultad y subnivel. */
    public double multiplicadorRecompensa() {
        return dificultad.multiplicadorRecompensa(subnivel);
    }

    /** Escala una cantidad base en centésimas por el multiplicador de recompensa indicado. */
    public static int escalarCentimos(int baseCentimos, double multiplicador) {
        return (int) Math.round(baseCentimos * multiplicador);
    }

    /** Bonos brutos (ya escalados por dificultad/subnivel) antes de aplicar la reducción a 1/4 por derrota. */
    public int bonosBrutosCentimos() {
        return bonosBrutosCentimos;
    }

    /** Indica si los bonos conseguidos fueron reducidos a 1/4 por no ganar la carrera. */
    public boolean bonosReducidosPorDerrota() {
        return bonosReducidosPorDerrota;
    }

    /** Comprueba si una dificultad de carrera está desbloqueada para el jugador dado. */
    public static boolean dificultadDesbloqueada(Jugador jugador, DificultadCarrera dificultad) {
        return dificultad != null && dificultad.disponiblePara(jugador);
    }

    /** Longitud total de esta pista en unidades de arena. */
    public double anchoPista() {
        return anchoPista;
    }

    /** Coordenada X de la línea de meta de esta pista. */
    public double metaX() {
        return metaX;
    }

    /** Límite máximo de turnos de esta carrera. */
    public int maxTurnos() {
        return maxTurnos;
    }

    /** Progreso normalizado [0, 1] del corredor indicado hacia la línea de meta. */
    public double progreso(int indice) {
        Cuerpo c = pelea.cuerpo(indice);
        if (c == null) return 0;
        return Math.max(0, Math.min(1, (c.x - SALIDA_X) / (metaX - SALIDA_X)));
    }

    /** Obstáculos visibles en el mapa general previo (excluye ocultos, impulsos, reparaciones, monedas y rampas). */
    public List<Arena.Trampa> obstaculosVisiblesEnMapa() {
        List<Arena.Trampa> visibles = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.ocultoEnMapa && !t.repentino
                    && !t.tipo.equals("impulso") && !t.tipo.equals(Arena.TIPO_REPARACION)
                    && !t.tipo.equals(Arena.TIPO_MONEDA_EXTRA) && !t.tipo.equals(Arena.TIPO_RAMPA)) {
                visibles.add(t);
            }
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

    /** Trampas mortales de destrucción instantánea actualmente activas en la pista. */
    public List<Arena.Trampa> trampasMortalesActivas() {
        List<Arena.Trampa> mortales = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.destruida() && t.tipo.equals(Arena.TIPO_TRAMPA_MORTAL)) mortales.add(t);
        }
        return mortales;
    }

    /** Bonos de reparación actualmente activos en la pista. */
    public List<Arena.Trampa> reparacionesActivas() {
        List<Arena.Trampa> reparaciones = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.destruida() && t.tipo.equals(Arena.TIPO_REPARACION)) reparaciones.add(t);
        }
        return reparaciones;
    }

    /** Monedas extra en zonas peligrosas actualmente activas en la pista. */
    public List<Arena.Trampa> monedasExtraActivas() {
        List<Arena.Trampa> monedas = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.destruida() && t.tipo.equals(Arena.TIPO_MONEDA_EXTRA)) monedas.add(t);
        }
        return monedas;
    }

    /** Rampas de salto actualmente activas en la pista. */
    public List<Arena.Trampa> rampasActivas() {
        List<Arena.Trampa> rampas = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.destruida() && t.tipo.equals(Arena.TIPO_RAMPA)) rampas.add(t);
        }
        return rampas;
    }

    /** Zonas de caída de agua actualmente activas en la pista. */
    public List<Arena.Trampa> zonasAguaActivas() {
        List<Arena.Trampa> aguas = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.destruida() && t.tipo.equals(Arena.TIPO_AGUA)) aguas.add(t);
        }
        return aguas;
    }

    /** Cantidad de monedas extra en zonas peligrosas recogidas por el jugador durante la carrera. */
    public int monedasExtraRecogidas() {
        return monedasExtraRecogidas;
    }

    /** Cantidad de obstáculos destructibles (barriles o chimeneas) destruidos por el jugador. */
    public int objetosDestruidos() {
        return objetosDestruidos;
    }

    /** Cantidad de robots defectuosos destruidos por el jugador. */
    public int defectuososDestruidos() {
        return defectuososDestruidos;
    }

    /** Indica si el jugador destruyó al rival principal antes de terminar la carrera. */
    public boolean rivalDestruido() {
        return rivalDestruido;
    }

    /** Total de centésimas de moneda ganadas en esta carrera (victoria, perfección, monedas extra y bajas). */
    public int centimosGanadosTotal() {
        return centimosGanadosTotal;
    }

    /** Total de monedas enteras sumadas al saldo del jugador al aplicar la recompensa. */
    public int monedasEnterasGanadas() {
        return monedasEnterasGanadas;
    }

    /** Porcentaje de daño recibido por el robot del jugador durante toda la carrera respecto a su blindaje máximo. */
    public double porcentajeDanoJugador() {
        Cuerpo c0 = pelea.cuerpo(0);
        if (c0 == null) return 0;
        double max = c0.blindajeMaximoTotal();
        if (max <= 0) return 0;
        return Math.max(0, 100.0 * pelea.combatientes[0].danoTotalRecibido / max);
    }

    /** Medalla de reconocimiento obtenida al ganar la carrera según el daño recibido (NINGUNA si no ganó). */
    public Medalla medallaObtenida() {
        if (!terminada || ganador != 0) return Medalla.NINGUNA;
        double danoPct = porcentajeDanoJugador();
        if (danoPct <= UMBRAL_PERFECTO) return Medalla.ORO;
        if (danoPct <= UMBRAL_PLATA) return Medalla.PLATA;
        if (danoPct <= UMBRAL_BRONCE) return Medalla.BRONCE;
        return Medalla.NINGUNA;
    }

    /** Cuartos de moneda ganados por perfección al aplicar la recompensa (1 si carrera perfecta, 0 si no). */
    public int cuartosGanados() {
        return cuartosGanados;
    }

    /** Monedas enteras extra completadas al alcanzar 4/4 cuartos en esta carrera (0 o 1). */
    public int monedasExtraPorCuartos() {
        return monedasExtraPorCuartos;
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

    /**
     * Aplica las recompensas exactas en centésimas escaladas por dificultad y subnivel:
     * victoria = 100 % (recompensa de victoria + bonos + perfección);
     * derrota/empate = 0 sin bonos, o exactamente 1/4 de los bonos conseguidos en pista. Idempotente.
     */
    public boolean aplicarRecompensa(Jugador jugador) {
        if (!terminada || recompensaAplicada || jugador == null) return false;
        if (rendido) {
            recompensaAplicada = true;
            bonosBrutosCentimos = 0;
            bonosReducidosPorDerrota = false;
            cuartosGanados = 0;
            monedasExtraPorCuartos = 0;
            centimosGanadosTotal = 0;
            monedasEnterasGanadas = 0;
            return false;
        }
        double mult = multiplicadorRecompensa();
        int bonosBase = monedasExtraRecogidas * CENTIMOS_MONEDA_EXTRA
                + objetosDestruidos * CENTIMOS_OBJETO_DESTRUIDO
                + defectuososDestruidos * CENTIMOS_DEFECTUOSO_DESTRUIDO
                + (rivalDestruido ? CENTIMOS_RIVAL_DESTRUIDO : 0);
        int bonosEscalados = escalarCentimos(bonosBase, mult);
        bonosBrutosCentimos = bonosEscalados;
        int totalCentimos;
        int centPerfeccion = 0;
        if (ganador == 0) {
            int centVictoria = escalarCentimos(100, mult);
            centPerfeccion = medallaObtenida().esPerfecta ? CENTIMOS_PERFECCION : 0;
            bonosReducidosPorDerrota = false;
            totalCentimos = centVictoria + centPerfeccion + bonosEscalados;
        } else {
            int bonosReducidos = bonosEscalados / 4;
            bonosReducidosPorDerrota = bonosEscalados > 0;
            totalCentimos = bonosReducidos;
        }
        recompensaAplicada = true;
        if (totalCentimos <= 0) {
            centimosGanadosTotal = 0;
            monedasEnterasGanadas = 0;
            return false;
        }
        if (centPerfeccion > 0) cuartosGanados = 1;
        int centAntes = jugador.centimosDeMoneda;
        monedasExtraPorCuartos = centPerfeccion > 0 && (centAntes + centPerfeccion) >= 100 ? 1 : 0;
        monedasEnterasGanadas = jugador.sumarCentimosDeMoneda(totalCentimos);
        centimosGanadosTotal = totalCentimos;
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

    /** Crea el combatiente rival controlado por la computadora adaptado a la dificultad indicada (subnivel I). */
    public static Combatiente rivalMaquina(String nombre, DificultadCarrera dificultad) {
        return rivalMaquina(nombre, dificultad, DificultadCarrera.SUBNIVEL_MIN);
    }

    /** Crea el combatiente rival controlado por la computadora adaptado a la dificultad y subnivel indicados. */
    public static Combatiente rivalMaquina(String nombre, DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int sub = DificultadCarrera.normalizarSubnivel(subnivel);
        Montaje montaje = new Montaje(Modelo.de("cobalto"));
        Fabrica fabrica = new Fabrica() {
            @Override
            public RobotBase crear() {
                return new PistaDeCarrera.CorredorMaquina(dif, sub);
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
