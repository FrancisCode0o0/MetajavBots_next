package metajavbots.combate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import metajavbots.api.RobotBase;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * MINIJUEGO DE DESTRUCCIÓN Y SUPERVIVENCIA — Reto de Modo especial en una arena
 * 3× más grande que la arena estándar, con cuenta atrás, apariciones
 * imprevisibles de robots averiados desde el cielo y objetos destructibles/trampas
 * desde el suelo, daño recibido al menos el doble (≥ 2,0×) que en la Carrera de
 * obstáculos y recompensas exactas en centésimas (0,1 a 0,6 por objetivo, +0,5
 * por sobrevivir y 1/4 al morir antes de acabar la cuenta atrás).
 */
public final class MinijuegoDestruccion {

    /** Ancho de la arena estándar de combate. */
    public static final double ANCHO_BASE_ESTANDAR = 1000.0;
    /** Alto de la arena estándar de combate. */
    public static final double ALTO_BASE_ESTANDAR = 700.0;
    /** Factor de ampliación de la arena respecto al tamaño estándar (exactamente 3×). */
    public static final double FACTOR_ARENA = 3.0;
    /** Ancho de la arena especial de destrucción (3× la arena estándar). */
    public static final double ANCHO = ANCHO_BASE_ESTANDAR * FACTOR_ARENA;
    /** Alto de la arena especial de destrucción (misma profundidad vertical estándar; área 3×). */
    public static final double ALTO = ALTO_BASE_ESTANDAR;
    /** Duración estándar de la cuenta atrás en segundos. */
    public static final int DURACION_SEGUNDOS = 60;
    /** Turnos totales de la cuenta atrás (60 s × 30 turnos/s = 1800 turnos). */
    public static final int TURNOS_CUENTA_ATRAS = DURACION_SEGUNDOS * Pelea.TURNOS_POR_SEGUNDO;
    /** Multiplicador mínimo de daño recibido sobre la Carrera de obstáculos (≥ 2,08× tras ajuste −20 %). */
    public static final double FACTOR_DANO_SOBRE_CARRERA = 2.08;
    /** Incremento adicional del factor de daño por cada subnivel por encima de I (0,12 × 0,80). */
    public static final double INCREMENTO_DANO_SUBNIVEL = 0.096;
    /** Probabilidad base de caída directa sobre el jugador cuando su movimiento es previsible. */
    public static final double PROB_CAIDA_DIRECTA_BASE = 0.12;
    /** Incremento de probabilidad de caída directa por nivel de dificultad cuando el jugador es previsible. */
    public static final double INCREMENTO_PROB_CAIDA_DIRECTA = 0.02;
    /** Distancia mínima al jugador para caídas cercanas por defecto (fuera del impacto directo instantáneo). */
    public static final double DISTANCIA_MIN_CAIDA_CERCA = 135.0;
    /** Distancia máxima al jugador para caídas cercanas por defecto (anillo de presión media-corta). */
    public static final double DISTANCIA_MAX_CAIDA_CERCA = 295.0;
    /** Daño base (antes del multiplicador del modo) al caer un robot normal del cielo encima del jugador. */
    public static final double DANO_BASE_CAIDA_NORMAL = 10.5;
    /** Daño base (antes del multiplicador del modo) al caer un robot pesado del cielo encima del jugador. */
    public static final double DANO_BASE_CAIDA_PESADO = 13.0;
    /** Margen adicional sobre la suma de radios dentro del cual un robot al caer golpea al jugador. */
    public static final double MARGEN_IMPACTO_CAIDA = 26.0;
    /** Recompensa mínima en centésimas por objetivo destruido (0,10 monedas). */
    public static final int MIN_CENTIMOS_OBJETIVO = 10;
    /** Recompensa máxima en centésimas por objetivo destruido (0,60 monedas). */
    public static final int MAX_CENTIMOS_OBJETIVO = 60;
    /** Bono adicional en centésimas por sobrevivir hasta el final de la cuenta atrás (+0,50 monedas). */
    public static final int BONO_SUPERVIVENCIA_CENTIMOS = 50;
    /** Margen mínimo respecto al jugador al programar un obstáculo o trampa del suelo. */
    public static final double MARGEN_SEGURIDAD_APARICION = 65.0;
    /** Turnos de sombra/aviso visual en el suelo antes de que impacte un robot del cielo (24 turnos = 0,80 s). */
    public static final int TURNOS_AVISO_CIELO = 24;
    /** Turnos de aviso visual en el suelo antes de que brote un objeto o trampa (18 turnos = 0,60 s). */
    public static final int TURNOS_AVISO_SUELO = 18;

    private static final int TAMANO_RESERVA_ROBOTS = 14;
    private static final int MAX_OBJETOS_SUELO = 32;
    private static final int TURNOS_RECICLADO_ROBOT = 45;

    /** Aviso visual previo a la aparición de un robot que cae del cielo o un objeto/trampa que brota del suelo. */
    public static final class AvisoAparicion {
        /** Coordenada X del punto de aparición en la arena. */
        public double x;
        /** Coordenada Y del punto de aparición en la arena. */
        public double y;
        /** Radio aproximado del elemento que va a aparecer. */
        public final double radio;
        /** True si cae desde el cielo (robot averiado); false si brota del suelo. */
        public final boolean desdeCielo;
        /** Tipo del elemento ("robot", "caja", "barril", "chimenea", "pinchos", "sierra", "bloque"). */
        public final String tipo;
        /** True si es una caída directa dirigida a la posición actual/proyectada del jugador. */
        public final boolean directa;
        /** Duración total del aviso en turnos. */
        public final int turnosTotales;
        /** Turnos restantes hasta la aparición efectiva. */
        private int turnosRestantes;

        AvisoAparicion(double x, double y, double radio, boolean desdeCielo, String tipo, int turnosTotales, boolean directa) {
            this.x = x;
            this.y = y;
            this.radio = radio;
            this.desdeCielo = desdeCielo;
            this.tipo = tipo;
            this.directa = directa;
            this.turnosTotales = Math.max(1, turnosTotales);
            this.turnosRestantes = this.turnosTotales;
        }

        /** Gasta un turno del aviso; devuelve true cuando el elemento ya aterriza o brota. */
        boolean pasarTurno() {
            turnosRestantes--;
            return turnosRestantes <= 0;
        }

        /** Turnos restantes antes de que el elemento aterrice o brote. */
        public int turnosRestantes() {
            return turnosRestantes;
        }

        /** Progreso normalizado [0, 1] de la señal previa (1 = impacto inminente). */
        public float progreso() {
            return 1f - Math.max(0f, Math.min(1f, turnosRestantes / (float) turnosTotales));
        }
    }

    /** Simulación física subyacente (reutiliza física de combate, armas y obstáculos). */
    public final Pelea pelea;
    /** Semilla determinista de la partida. */
    public final long semilla;
    /** Escenario visual seleccionado para esta partida. */
    public final String escenario;
    /** Nivel de dificultad configurado (mismo rango/desbloqueo que la Carrera de obstáculos). */
    public final DificultadCarrera dificultad;
    /** Subnivel de dificultad (1 = I, 2 = II, 3 = III). */
    public final int subnivel;
    /** Duración total de esta cuenta atrás en turnos. */
    public final int turnosTotales;

    private final Random azar;
    private final PrevisibilidadJugador previsibilidad = new PrevisibilidadJugador();
    private final List<AvisoAparicion> avisos = new ArrayList<>();
    private final boolean[] robotPesado;
    private final int[] turnosCaido;
    private int proximoAvisoSuelo;
    private int proximoAvisoCielo;
    private int totalAparicionesSuelo;
    private int totalAparicionesCielo;
    private int totalCaidasDirectas;
    private int totalCaidasCerca;
    private int objetosDestruidos;
    private int robotsDestruidos;
    private int botinAcumuladoCentimos;
    private int bonoSupervivenciaCentimos;
    private int centimosGanadosTotal;
    private int monedasEnterasGanadas;
    private boolean bonosReducidosPorDerrota;
    private boolean terminada;
    private boolean sobrevivio;
    private boolean rendido;
    private boolean recompensaAplicada;
    private String motivoFin = "";

    /** Crea una nueva partida del minijuego especial con la duración estándar de cuenta atrás. */
    public MinijuegoDestruccion(Combatiente jugador, long semilla, String arenaAnterior,
            DificultadCarrera dificultad, int subnivel) {
        this(jugador, semilla, arenaAnterior, dificultad, subnivel, TURNOS_CUENTA_ATRAS);
    }

    /** Crea una nueva partida del minijuego especial con duración personalizable en turnos (para pruebas). */
    public MinijuegoDestruccion(Combatiente jugador, long semilla, String arenaAnterior,
            DificultadCarrera dificultad, int subnivel, int turnosCuentaAtras) {
        this.semilla = semilla;
        this.dificultad = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        this.subnivel = DificultadCarrera.normalizarSubnivel(subnivel);
        this.turnosTotales = Math.max(30, turnosCuentaAtras);
        this.escenario = PistaDeCarrera.siguienteArena(arenaAnterior, semilla);
        this.azar = new Random(semilla ^ (this.dificultad.ordinal() * 131L) ^ (this.subnivel * 29L) ^ 0x5A3CL);
        this.robotPesado = new boolean[TAMANO_RESERVA_ROBOTS];
        this.turnosCaido = new int[TAMANO_RESERVA_ROBOTS];

        Combatiente[] cs = new Combatiente[1 + TAMANO_RESERVA_ROBOTS];
        int[] equipos = new int[cs.length];
        cs[0] = jugador;
        equipos[0] = 0;
        poblarReservaRobots(cs, equipos);

        this.pelea = new Pelea(cs, equipos, semilla, ANCHO, ALTO, this.turnosTotales);
        this.pelea.rondas = 1;
        this.pelea.sinFinPorEquipos = true;
        // Sus reglas y su daño ya ajustados: del equilibrio de peso solo cuenta el terreno sobre la locomoción.
        this.pelea.reglasPropias = true;
        this.pelea.multiplicadorDano = multiplicadorDanoPara(this.dificultad, this.subnivel);
        this.pelea.arena = Arena.crearParaDestruccion(PistaDeCarrera.agarreDe(this.escenario));
        this.pelea.empezarRonda();
        inicializarPosiciones();
        this.proximoAvisoSuelo = 4;
        this.proximoAvisoCielo = 12;
    }

    private void poblarReservaRobots(Combatiente[] cs, int[] equipos) {
        List<Modelo> modelos = Modelo.todos();
        int cadencia = cadenciaDisparoPara(dificultad, subnivel);
        double dispersion = dispersionDisparoPara(dificultad, subnivel);
        double escalaDano = escalaDanoDefectuosoPara(dificultad, subnivel);
        for (int i = 0; i < TAMANO_RESERVA_ROBOTS; i++) {
            Modelo modelo = modelos.isEmpty() ? Modelo.de("cobalto")
                    : modelos.get(Math.floorMod(i * 3 + dificultad.ordinal() + azar.nextInt(modelos.size()), modelos.size()));
            boolean pesado = modelo.tamano >= 32 || (i % 3 == 2 && dificultad.ordinal() >= DificultadCarrera.JUNIOR.ordinal());
            robotPesado[i] = pesado;
            Montaje m = new Montaje(modelo);
            for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
                if (p.arma() == Pieza.Arma.BALA && p.valor("dano") > 0) {
                    m.poner(Montaje.Hueco.IZQUIERDA, p);
                    break;
                }
            }
            boolean inmovil = i % 7 == 6;
            Fabrica fab = crearFabricaDefectuoso(inmovil, cadencia, dispersion);
            Combatiente c = new Combatiente("Caido-" + (i + 1), m, fab);
            c.computadora = true;
            c.defectuoso = true;
            c.escalaBlindaje = dificultad.escalaBlindajeDefectuosos * (pesado ? 1.25 : 0.95);
            c.escalaDano = escalaDano;
            for (int k = 0; k < 6; k++) {
                c.abollar(k % 4, azar, 14 + azar.nextDouble() * 14, k % 2 == 0);
            }
            cs[1 + i] = c;
            equipos[1 + i] = 1;
        }
    }

    /** Escala de daño de disparos de los robots averiados en este reto (independiente de Carrera de obstáculos). */
    public static double escalaDanoDefectuosoPara(DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        return 1.65 + dif.ordinal() * 0.20 + extra * 0.08;
    }

    private static int cadenciaDisparoPara(DificultadCarrera dif, int subnivel) {
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        int base = dif == DificultadCarrera.NOOB ? 22 : dif == DificultadCarrera.JUNIOR ? 15
                : dif == DificultadCarrera.SENIOR ? 10 : 7;
        return Math.max(5, base - extra * 2);
    }

    private static double dispersionDisparoPara(DificultadCarrera dif, int subnivel) {
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        double base = dif == DificultadCarrera.NOOB ? 14.0 : dif == DificultadCarrera.JUNIOR ? 11.0
                : dif == DificultadCarrera.SENIOR ? 8.0 : 6.0;
        return Math.max(4.0, base - extra * 0.8);
    }

    private static Fabrica crearFabricaDefectuoso(boolean inmovil, int cadencia, double dispersion) {
        return new Fabrica() {
            @Override
            public RobotBase crear() {
                return new RobotAveriadoDestruccion(inmovil, cadencia, dispersion);
            }

            @Override
            public long version() {
                return 1;
            }

            @Override
            public String clase() {
                return RobotAveriadoDestruccion.class.getName();
            }

            @Override
            public boolean esSuya(String nombreDeClase) {
                return RobotAveriadoDestruccion.class.getName().equals(nombreDeClase);
            }
        };
    }

    private void inicializarPosiciones() {
        Cuerpo c0 = pelea.cuerpo(0);
        if (c0 != null) {
            c0.x = ANCHO / 2.0;
            c0.y = ALTO / 2.0;
            c0.rumbo = 0;
            c0.mira = 0;
            c0.velocidad = 0;
            c0.detener();
        }
        for (int i = 1; i < pelea.combatientes.length; i++) {
            Cuerpo c = pelea.cuerpo(i);
            if (c != null) {
                c.enReserva = true;
                c.fuera = true;
                c.cayendo = 0;
                c.x = -500;
                c.y = -500;
            }
        }
    }

    /**
     * Multiplicador de daño recibido en este reto para la dificultad y subnivel indicados.
     * Siempre es estrictamente al menos el doble ({@code >= 2.0×}) que en la Carrera de obstáculos.
     */
    public static double multiplicadorDanoPara(DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        return dif.multiplicadorDano * (FACTOR_DANO_SOBRE_CARRERA + extra * INCREMENTO_DANO_SUBNIVEL);
    }

    /**
     * Recompensa exacta en centésimas (siempre entre 10 y 60, es decir, 0,1 a 0,6 monedas)
     * según tipo/resistencia del objetivo, dificultad y subnivel.
     */
    public static int recompensaPorObjetivo(String tipo, boolean pesado, DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        int base;
        if ("caja".equals(tipo)) base = 10;
        else if ("barril".equals(tipo)) base = 20;
        else if ("chimenea".equals(tipo)) base = 30;
        else base = pesado ? 48 : 40;
        double escala = 1.0 + dif.ordinal() * 0.06 + extra * 0.03;
        int valor = (int) Math.round(base * escala);
        return Math.max(MIN_CENTIMOS_OBJETIVO, Math.min(MAX_CENTIMOS_OBJETIVO, valor));
    }

    /** Probabilidad de caída directa sobre el jugador (solo aplicable cuando su movimiento es previsible). */
    public static double probabilidadCaidaDirectaPara(DificultadCarrera dificultad) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        return PROB_CAIDA_DIRECTA_BASE + dif.ordinal() * INCREMENTO_PROB_CAIDA_DIRECTA;
    }

    /** Avanza un turno del minijuego: simula física, actualiza apariciones, cuenta botín y evalúa el fin. */
    public void avanzar() {
        if (terminada) return;
        actualizarCaidasYReciclado();
        pelea.avanzar();
        previsibilidad.registrar(pelea.cuerpo(0));
        actualizarApariciones();
        procesarSucesosDelTurno();
        evaluarFin();
    }

    private void actualizarCaidasYReciclado() {
        for (int i = 0; i < TAMANO_RESERVA_ROBOTS; i++) {
            Cuerpo c = pelea.cuerpo(1 + i);
            if (c == null) continue;
            if (c.cayendo > 0) c.cayendo--;
            if (!c.enReserva && c.fuera) {
                turnosCaido[i]++;
                if (turnosCaido[i] >= TURNOS_RECICLADO_ROBOT) {
                    c.enReserva = true;
                    c.x = -500;
                    c.y = -500;
                    turnosCaido[i] = 0;
                }
            }
        }
    }

    private void actualizarApariciones() {
        int t = pelea.turno();
        if (t >= proximoAvisoSuelo) {
            programarAvisoSuelo();
            proximoAvisoSuelo = t + intervaloSuelo();
        }
        if (t >= proximoAvisoCielo) {
            programarAvisoCielo();
            proximoAvisoCielo = t + intervaloCielo();
        }
        for (int i = avisos.size() - 1; i >= 0; i--) {
            AvisoAparicion a = avisos.get(i);
            a.turnosRestantes--;
            if (a.turnosRestantes <= 0) {
                avisos.remove(i);
                if (a.desdeCielo) aterrizarRobot(a);
                else brotarDelSuelo(a);
            }
        }
    }

    private int intervaloSuelo() {
        int base = Math.max(16, 38 - dificultad.ordinal() * 6 - (subnivel - 1) * 3);
        return base + azar.nextInt(Math.max(8, base / 2));
    }

    private int intervaloCielo() {
        int base = Math.max(28, 62 - dificultad.ordinal() * 10 - (subnivel - 1) * 5);
        return base + azar.nextInt(Math.max(10, base / 2));
    }

    private void programarAvisoSuelo() {
        if (contarTrampasActivas() >= MAX_OBJETOS_SUELO) return;
        double probTrampa = 0.28 + dificultad.ordinal() * 0.05 + (subnivel - 1) * 0.02;
        String tipo;
        double radio;
        if (azar.nextDouble() < probTrampa) {
            int k = azar.nextInt(3);
            tipo = k == 0 ? "pinchos" : k == 1 ? "sierra" : "bloque";
            radio = k == 0 ? 44 : k == 1 ? 48 : 34;
        } else {
            int k = azar.nextInt(3);
            tipo = k == 0 ? "caja" : k == 1 ? "barril" : "chimenea";
            radio = k == 0 ? 25 : k == 1 ? 28 : 34;
        }
        double[] pos = posicionSueloEnTrayectoria(radio);
        avisos.add(new AvisoAparicion(pos[0], pos[1], radio, false, tipo, TURNOS_AVISO_SUELO, false));
    }

    private void programarAvisoCielo() {
        if (buscarSlotLibre() < 0) return;
        boolean directa = previsibilidad.esPrevisible()
                && azar.nextDouble() < probabilidadCaidaDirectaPara(dificultad);
        double[] pos = posicionCaidaCielo(34, directa);
        avisos.add(new AvisoAparicion(pos[0], pos[1], 34, true, "robot", TURNOS_AVISO_CIELO, directa));
    }

    private void brotarDelSuelo(AvisoAparicion a) {
        double margenBorde = Math.max(85.0, a.radio + 45.0);
        double px = Math.max(margenBorde, Math.min(ANCHO - margenBorde, a.x));
        double py = Math.max(margenBorde, Math.min(ALTO - margenBorde, a.y));
        boolean destructible = "caja".equals(a.tipo) || "barril".equals(a.tipo) || "chimenea".equals(a.tipo);
        if (destructible) {
            double res = "caja".equals(a.tipo) ? Arena.RESISTENCIA_CAJA
                    : "barril".equals(a.tipo) ? Arena.RESISTENCIA_BARRIL : Arena.RESISTENCIA_CHIMENEA;
            pelea.arena.agregarRepentina(a.tipo, px, py, a.radio, true, 0, 0, res);
        } else {
            boolean solido = "bloque".equals(a.tipo);
            int ciclo = "pinchos".equals(a.tipo) ? 105 : 0;
            int activo = "pinchos".equals(a.tipo) ? 60 : 0;
            pelea.arena.agregarRepentina(a.tipo, px, py, a.radio, solido, ciclo, activo, 0);
        }
        totalAparicionesSuelo++;
        pelea.suceso(Suceso.Tipo.TRAMPA, -1, px, py, 0, a.radio, "repentino_" + a.tipo);
    }

    private void aterrizarRobot(AvisoAparicion a) {
        int slot = buscarSlotLibre();
        if (slot < 0) return;
        Cuerpo c = pelea.cuerpo(1 + slot);
        if (c == null) return;
        double margenBorde = Math.max(85.0, c.radio + 45.0);
        double px = Math.max(margenBorde, Math.min(ANCHO - margenBorde, a.x));
        double py = Math.max(margenBorde, Math.min(ALTO - margenBorde, a.y));
        c.enReserva = false;
        c.fuera = false;
        c.cayendo = 14;
        c.x = px;
        c.y = py;
        Cuerpo c0 = pelea.cuerpo(0);
        if (c0 != null && !c0.fuera) {
            double angJug = Math.toDegrees(Math.atan2(c0.y - py, c0.x - px));
            c.rumbo = Pelea.normal(angJug + (azar.nextDouble() * 2.0 - 1.0) * 35.0);
        } else {
            c.rumbo = azar.nextDouble() * 360 - 180;
        }
        c.mira = c.rumbo;
        c.velocidad = 0;
        c.detener();
        c.energia = Cuerpo.energiaMaxSegura(c.cifras.energiaMax);
        for (int k = 0; k < 4; k++) c.parte[k] = c.parteMax[k];
        c.aturdido = 0;
        c.volteado = 0;
        c.deuda = 0;
        c.ultimoAtacante = -1;
        turnosCaido[slot] = 0;
        totalAparicionesCielo++;
        if (a.directa) totalCaidasDirectas++;
        else totalCaidasCerca++;
        pelea.suceso(Suceso.Tipo.TRAMPA, -1, px, py, 0, c.radio, "repentino_robot");
        aplicarImpactoCaidaSiAcierta(c, slot, c0);
    }

    private void aplicarImpactoCaidaSiAcierta(Cuerpo caido, int slot, Cuerpo c0) {
        if (c0 == null || c0.fuera) return;
        double dx = c0.x - caido.x;
        double dy = c0.y - caido.y;
        double dist = Math.hypot(dx, dy);
        double radioImpacto = c0.radio + caido.radio + MARGEN_IMPACTO_CAIDA;
        if (dist > radioImpacto) return;
        boolean pesado = slot >= 0 && slot < robotPesado.length && robotPesado[slot];
        double danoBase = pesado ? DANO_BASE_CAIDA_PESADO : DANO_BASE_CAIDA_NORMAL;
        double ang = dist < 0.01 ? 0 : Math.toDegrees(Math.atan2(-dy, -dx));
        pelea.danar(c0, null, danoBase, "caida_robot", ang, true, c0.x, c0.y);
        pelea.suceso(Suceso.Tipo.EXPLOSION, 0, caido.x, caido.y, ang, danoBase * pelea.multiplicadorDano, "caida_robot");
        double minSep = c0.radio + caido.radio + 8.0;
        if (dist < minSep) {
            double ux = dist < 0.01 ? 1.0 : dx / dist;
            double uy = dist < 0.01 ? 0.0 : dy / dist;
            c0.x = Math.max(c0.radio, Math.min(ANCHO - c0.radio, caido.x + ux * minSep));
            c0.y = Math.max(c0.radio, Math.min(ALTO - c0.radio, caido.y + uy * minSep));
        }
        c0.velocidad *= 0.25;
        c0.aturdido = Math.max(c0.aturdido, 8);
    }

    private int buscarSlotLibre() {
        for (int i = 0; i < TAMANO_RESERVA_ROBOTS; i++) {
            Cuerpo c = pelea.cuerpo(1 + i);
            if (c != null && c.enReserva) return i;
        }
        return -1;
    }

    private int contarTrampasActivas() {
        int n = 0;
        for (Arena.Trampa t : pelea.arena.trampas()) if (!t.destruida()) n++;
        return n;
    }

    private double[] posicionSueloEnTrayectoria(double radio) {
        Cuerpo c0 = pelea.cuerpo(0);
        double margenBorde = Math.max(85.0, radio + 45.0);
        double probTrayectoria = 0.65 + dificultad.ordinal() * 0.05;
        if (c0 != null && !c0.fuera && azar.nextDouble() < probTrayectoria) {
            double rad = Math.toRadians(c0.rumbo);
            double distAdelante = 135.0 + Math.abs(c0.velocidad) * 24.0 + azar.nextDouble() * 175.0;
            double desvioLat = (azar.nextDouble() * 2.0 - 1.0) * 65.0;
            double x = c0.x + Math.cos(rad) * distAdelante - Math.sin(rad) * desvioLat;
            double y = c0.y + Math.sin(rad) * distAdelante + Math.cos(rad) * desvioLat;
            return asegurarLejosDelJugador(x, y, radio);
        }
        double baseX = c0 != null ? c0.x + (azar.nextDouble() * 2 - 1) * 540.0 : ANCHO / 2.0;
        double x = Math.max(margenBorde, Math.min(ANCHO - margenBorde, baseX));
        double y = margenBorde + azar.nextDouble() * (ALTO - 2 * margenBorde);
        return asegurarLejosDelJugador(x, y, radio);
    }

    private double[] posicionCaidaCielo(double radio, boolean directa) {
        Cuerpo c0 = pelea.cuerpo(0);
        double margenBorde = Math.max(85.0, radio + 45.0);
        if (c0 == null || c0.fuera) {
            return new double[]{ANCHO / 2.0, ALTO / 2.0};
        }
        if (directa) {
            double rad = Math.toRadians(c0.rumbo);
            double pred = Math.abs(c0.velocidad) > 0.3 ? Math.min(90.0, Math.abs(c0.velocidad) * 14.0) : 0.0;
            double x = c0.x + Math.cos(rad) * pred + (azar.nextDouble() * 2.0 - 1.0) * 12.0;
            double y = c0.y + Math.sin(rad) * pred + (azar.nextDouble() * 2.0 - 1.0) * 12.0;
            return new double[]{
                    Math.max(margenBorde, Math.min(ANCHO - margenBorde, x)),
                    Math.max(margenBorde, Math.min(ALTO - margenBorde, y))
            };
        }
        double distMin = Math.max(DISTANCIA_MIN_CAIDA_CERCA, c0.radio + radio + MARGEN_IMPACTO_CAIDA + 35.0);
        double distMax = Math.max(distMin + 80.0, DISTANCIA_MAX_CAIDA_CERCA);
        for (int intento = 0; intento < 12; intento++) {
            double ang = azar.nextDouble() * Math.PI * 2.0;
            double dist = distMin + azar.nextDouble() * (distMax - distMin);
            double rx = Math.max(margenBorde, Math.min(ANCHO - margenBorde, c0.x + Math.cos(ang) * dist));
            double ry = Math.max(margenBorde, Math.min(ALTO - margenBorde, c0.y + Math.sin(ang) * dist));
            if (Math.hypot(rx - c0.x, ry - c0.y) >= distMin) {
                return new double[]{rx, ry};
            }
        }
        double rx = c0.x > ANCHO / 2.0 ? Math.max(margenBorde, c0.x - distMin - 30.0)
                : Math.min(ANCHO - margenBorde, c0.x + distMin + 30.0);
        double ry = c0.y > ALTO / 2.0 ? Math.max(margenBorde, c0.y - 60.0)
                : Math.min(ALTO - margenBorde, c0.y + 60.0);
        return new double[]{rx, ry};
    }

    private double[] asegurarLejosDelJugador(double x, double y, double radio) {
        Cuerpo c0 = pelea.cuerpo(0);
        double margenBorde = Math.max(85.0, radio + 45.0);
        double rx = Math.max(margenBorde, Math.min(ANCHO - margenBorde, x));
        double ry = Math.max(margenBorde, Math.min(ALTO - margenBorde, y));
        if (c0 == null || c0.fuera) return new double[]{rx, ry};
        double minDist = c0.radio + radio + MARGEN_SEGURIDAD_APARICION;
        for (int intento = 0; intento < 10; intento++) {
            if (Math.hypot(rx - c0.x, ry - c0.y) >= minDist) return new double[]{rx, ry};
            double ang = azar.nextDouble() * Math.PI * 2;
            rx = Math.max(margenBorde, Math.min(ANCHO - margenBorde, c0.x + Math.cos(ang) * (minDist + 70)));
            ry = Math.max(margenBorde, Math.min(ALTO - margenBorde, c0.y + Math.sin(ang) * (minDist + 50)));
        }
        rx = c0.x > ANCHO / 2.0 ? Math.max(margenBorde, c0.x - minDist - 80) : Math.min(ANCHO - margenBorde, c0.x + minDist + 80);
        ry = c0.y > ALTO / 2.0 ? margenBorde + 20 : ALTO - margenBorde - 20;
        return new double[]{rx, ry};
    }

    private void procesarSucesosDelTurno() {
        for (Suceso s : pelea.sucesos()) {
            if (s.tipo == Suceso.Tipo.OBSTACULO_ROTO && s.quien == 0) {
                objetosDestruidos++;
                botinAcumuladoCentimos += recompensaPorObjetivo(s.texto, false, dificultad, subnivel);
            } else if (s.tipo == Suceso.Tipo.FUERA_DE_COMBATE && s.quien >= 1) {
                Cuerpo caido = pelea.cuerpo(s.quien);
                if (caido != null && caido.ultimoAtacante == 0) {
                    int slot = s.quien - 1;
                    boolean pesado = slot >= 0 && slot < robotPesado.length && robotPesado[slot];
                    robotsDestruidos++;
                    botinAcumuladoCentimos += recompensaPorObjetivo("robot", pesado, dificultad, subnivel);
                }
            }
        }
    }

    private void evaluarFin() {
        Cuerpo c0 = pelea.cuerpo(0);
        if (c0 == null || c0.fuera) {
            terminada = true;
            sobrevivio = false;
            motivoFin = "ko";
            return;
        }
        if (pelea.turno() >= turnosTotales || pelea.estado() == Pelea.Estado.TERMINADA) {
            terminada = true;
            sobrevivio = true;
            motivoFin = "supervivencia";
        }
    }

    /** Finaliza el minijuego inmediatamente por rendición, otorgando 0 monedas. */
    public void rendirse() {
        if (terminada) return;
        rendido = true;
        terminada = true;
        sobrevivio = false;
        motivoFin = "rendicion";
    }

    /**
     * Aplica la recompensa exacta en centésimas (idempotente):
     * supervivencia = 100 % del botín acumulado + 50 centésimas (+0,5 monedas);
     * muerte antes de acabar = 1/4 del botín acumulado (0 si no destruyó nada);
     * rendición = 0.
     */
    public boolean aplicarRecompensa(Jugador jugador) {
        if (!terminada || recompensaAplicada || jugador == null) return false;
        recompensaAplicada = true;
        if (rendido) {
            bonoSupervivenciaCentimos = 0;
            bonosReducidosPorDerrota = false;
            centimosGanadosTotal = 0;
            monedasEnterasGanadas = 0;
            return false;
        }
        int totalCentimos;
        if (sobrevivio) {
            bonoSupervivenciaCentimos = BONO_SUPERVIVENCIA_CENTIMOS;
            bonosReducidosPorDerrota = false;
            totalCentimos = botinAcumuladoCentimos + bonoSupervivenciaCentimos;
        } else {
            bonoSupervivenciaCentimos = 0;
            bonosReducidosPorDerrota = botinAcumuladoCentimos > 0;
            totalCentimos = botinAcumuladoCentimos / 4;
        }
        centimosGanadosTotal = totalCentimos;
        if (totalCentimos <= 0) {
            monedasEnterasGanadas = 0;
            return false;
        }
        monedasEnterasGanadas = jugador.sumarCentimosDeMoneda(totalCentimos);
        return true;
    }

    /** Indica si la partida ha concluido. */
    public boolean terminada() {
        return terminada;
    }

    /** Indica si el jugador sobrevivió hasta el final de la cuenta atrás. */
    public boolean sobrevivio() {
        return sobrevivio;
    }

    /** Indica si el jugador se rindió. */
    public boolean rendido() {
        return rendido;
    }

    /** Motivo de finalización ("supervivencia", "ko", "rendicion" o ""). */
    public String motivoFin() {
        return motivoFin;
    }

    /** Turnos restantes de la cuenta atrás (llega hasta 0). */
    public int turnosRestantes() {
        return Math.max(0, turnosTotales - pelea.turno());
    }

    /** Segundos restantes de la cuenta atrás. */
    public double segundosRestantes() {
        return turnosRestantes() / (double) Pelea.TURNOS_POR_SEGUNDO;
    }

    /** Fracción [0, 1] del tiempo restante de la cuenta atrás. */
    public float proporcionTiempoRestante() {
        return Math.max(0f, Math.min(1f, turnosRestantes() / (float) turnosTotales));
    }

    /** Avisos visuales activos de próximas apariciones (cielo y suelo). */
    public List<AvisoAparicion> avisosActivos() {
        return Collections.unmodifiableList(avisos);
    }

    /** Robots averiados actualmente activos (ya aterrizados y aún en pie) en la arena. */
    public List<Cuerpo> robotsActivos() {
        List<Cuerpo> lista = new ArrayList<>();
        for (int i = 1; i < pelea.combatientes.length; i++) {
            Cuerpo c = pelea.cuerpo(i);
            if (c != null && !c.enReserva && !c.fuera) lista.add(c);
        }
        return lista;
    }

    /** Cantidad total de objetos/trampas que brotaron del suelo durante la partida. */
    public int totalAparicionesSuelo() {
        return totalAparicionesSuelo;
    }

    /** Cantidad total de robots averiados que cayeron del cielo durante la partida. */
    public int totalAparicionesCielo() {
        return totalAparicionesCielo;
    }

    /** Cantidad de robots caídos directamente sobre la posición actual/proyectada del jugador. */
    public int totalCaidasDirectas() {
        return totalCaidasDirectas;
    }

    /** Cantidad de robots caídos cerca del jugador (anillo de presión sin impacto directo instantáneo). */
    public int totalCaidasCerca() {
        return totalCaidasCerca;
    }

    /** Indica si el seguimiento reciente clasifica el movimiento del jugador como previsible. */
    public boolean jugadorPrevisible() {
        return previsibilidad.esPrevisible();
    }

    /** Objetos destructibles del suelo destruidos por el jugador. */
    public int objetosDestruidos() {
        return objetosDestruidos;
    }

    /** Robots averiados caídos del cielo destruidos por el jugador. */
    public int robotsDestruidos() {
        return robotsDestruidos;
    }

    /** Botín bruto acumulado en centésimas por las destrucciones conseguidas. */
    public int botinAcumuladoCentimos() {
        return botinAcumuladoCentimos;
    }

    /** Bono de supervivencia en centésimas (+50 si sobrevivió, 0 si murió o se rindió). */
    public int bonoSupervivenciaCentimos() {
        return bonoSupervivenciaCentimos;
    }

    /** Centésimas totales abonadas al jugador al terminar. */
    public int centimosGanadosTotal() {
        return centimosGanadosTotal;
    }

    /** Monedas enteras completadas al aplicar la recompensa. */
    public int monedasEnterasGanadas() {
        return monedasEnterasGanadas;
    }

    /** Indica si el botín acumulado se redujo a 1/4 por morir antes de acabar la cuenta atrás. */
    public boolean bonosReducidosPorDerrota() {
        return bonosReducidosPorDerrota;
    }

    /** Indica si la recompensa ya fue aplicada al jugador. */
    public boolean recompensaAplicada() {
        return recompensaAplicada;
    }

    /** Construye el combatiente del jugador reutilizando exactamente el montaje y cifras de {@link CarreraDeObstaculos}. */
    public static Combatiente combatienteDeJugador(Jugador jugador, int indiceRobot, Fabrica fabrica) {
        return CarreraDeObstaculos.combatienteDeJugador(jugador, indiceRobot, fabrica);
    }

    /** Plantilla inicial compilable en ES, DE y EN sin fugas de API entre idiomas. */
    public static String plantillaInicial(Idioma idioma) {
        return PlantillasDestruccion.plantillaInicial(idioma);
    }
}
