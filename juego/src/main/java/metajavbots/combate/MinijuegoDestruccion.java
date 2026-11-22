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
 * MINIJUEGO DE DESTRUCCIÓN Y SUPERVIVENCIA — Reto especial en una arena 3× más
 * grande que la arena estándar, con cuenta atrás, apariciones imprevisibles de
 * robots averiados desde el cielo y objetos destructibles/trampas desde el suelo,
 * daño recibido al menos un 25 % mayor que en la Carrera de obstáculos y
 * recompensas exactas en centésimas (0,1 a 0,6 por objetivo, +0,5 por sobrevivir
 * y 1/4 al morir antes de acabar la cuenta atrás).
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
    /** Multiplicador mínimo de daño recibido sobre la Carrera de obstáculos (+25 %). */
    public static final double FACTOR_DANO_SOBRE_CARRERA = 1.25;
    /** Incremento adicional del factor de daño por cada subnivel por encima de I. */
    public static final double INCREMENTO_DANO_SUBNIVEL = 0.05;
    /** Recompensa mínima en centésimas por objetivo destruido (0,10 monedas). */
    public static final int MIN_CENTIMOS_OBJETIVO = 10;
    /** Recompensa máxima en centésimas por objetivo destruido (0,60 monedas). */
    public static final int MAX_CENTIMOS_OBJETIVO = 60;
    /** Bono adicional en centésimas por sobrevivir hasta el final de la cuenta atrás (+0,50 monedas). */
    public static final int BONO_SUPERVIVENCIA_CENTIMOS = 50;
    /** Margen mínimo de seguridad respecto al robot del jugador al aparecer un peligro o caída. */
    public static final double MARGEN_SEGURIDAD_APARICION = 115.0;
    /** Turnos de sombra/aviso visual en el suelo antes de que impacte un robot del cielo. */
    public static final int TURNOS_AVISO_CIELO = 24;
    /** Turnos de aviso visual en el suelo antes de que brote un objeto o trampa. */
    public static final int TURNOS_AVISO_SUELO = 18;

    private static final int TAMANO_RESERVA_ROBOTS = 14;
    private static final int MAX_OBJETOS_SUELO = 24;
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
        /** Duración total del aviso en turnos. */
        public final int turnosTotales;
        /** Turnos restantes hasta la aparición efectiva. */
        private int turnosRestantes;

        AvisoAparicion(double x, double y, double radio, boolean desdeCielo, String tipo, int turnosTotales) {
            this.x = x;
            this.y = y;
            this.radio = radio;
            this.desdeCielo = desdeCielo;
            this.tipo = tipo;
            this.turnosTotales = Math.max(1, turnosTotales);
            this.turnosRestantes = this.turnosTotales;
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
    private final List<AvisoAparicion> avisos = new ArrayList<>();
    private final boolean[] robotPesado;
    private final int[] turnosCaido;
    private int proximoAvisoSuelo;
    private int proximoAvisoCielo;
    private int totalAparicionesSuelo;
    private int totalAparicionesCielo;
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
        this.pelea.multiplicadorDano = multiplicadorDanoPara(this.dificultad, this.subnivel);
        this.pelea.arena = Arena.crearParaCarrera(PistaDeCarrera.agarreDe(this.escenario));
        this.pelea.empezarRonda();
        inicializarPosiciones();
        this.proximoAvisoSuelo = 6;
        this.proximoAvisoCielo = 16;
    }

    private void poblarReservaRobots(Combatiente[] cs, int[] equipos) {
        List<Modelo> modelos = Modelo.todos();
        int extra = subnivel - 1;
        int cadencia = Math.max(5, dificultad.cadenciaDisparoDefectuosos - extra * 2);
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
            boolean inmovil = i % 5 == 4;
            Fabrica fab = crearFabricaDefectuoso(inmovil, cadencia);
            Combatiente c = new Combatiente("Caido-" + (i + 1), m, fab);
            c.computadora = true;
            c.defectuoso = true;
            c.escalaBlindaje = dificultad.escalaBlindajeDefectuosos * (pesado ? 1.25 : 0.95);
            c.escalaDano = dificultad.escalaDanoDefectuosos;
            for (int k = 0; k < 6; k++) {
                c.abollar(k % 4, azar, 14 + azar.nextDouble() * 14, k % 2 == 0);
            }
            cs[1 + i] = c;
            equipos[1 + i] = 1;
        }
    }

    private static Fabrica crearFabricaDefectuoso(boolean inmovil, int cadencia) {
        return new Fabrica() {
            @Override
            public RobotBase crear() {
                return new PistaDeCarrera.RobotDefectuoso(inmovil, cadencia);
            }

            @Override
            public long version() {
                return 1;
            }

            @Override
            public String clase() {
                return PistaDeCarrera.RobotDefectuoso.class.getName();
            }

            @Override
            public boolean esSuya(String nombreDeClase) {
                return PistaDeCarrera.RobotDefectuoso.class.getName().equals(nombreDeClase);
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
     * Multiplicador de daño recibido en este minijuego para la dificultad y subnivel indicados.
     * Siempre es estrictamente al menos un 25 % mayor ({@code >= 1.25×}) que en la Carrera de obstáculos.
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

    /** Avanza un turno del minijuego: actualiza apariciones, simula física, cuenta botín y evalúa el fin. */
    public void avanzar() {
        if (terminada) return;
        actualizarCaidasYReciclado();
        actualizarApariciones();
        pelea.avanzar();
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
        int base = Math.max(24, 62 - dificultad.ordinal() * 10 - (subnivel - 1) * 5);
        return base + azar.nextInt(Math.max(10, base / 2));
    }

    private int intervaloCielo() {
        int base = Math.max(38, 92 - dificultad.ordinal() * 14 - (subnivel - 1) * 7);
        return base + azar.nextInt(Math.max(14, base / 2));
    }

    private void programarAvisoSuelo() {
        if (contarTrampasActivas() >= MAX_OBJETOS_SUELO) return;
        double probTrampa = 0.24 + dificultad.ordinal() * 0.05 + (subnivel - 1) * 0.02;
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
        double[] pos = posicionSegura(radio);
        avisos.add(new AvisoAparicion(pos[0], pos[1], radio, false, tipo, TURNOS_AVISO_SUELO));
    }

    private void programarAvisoCielo() {
        if (buscarSlotLibre() < 0) return;
        double[] pos = posicionSegura(34);
        avisos.add(new AvisoAparicion(pos[0], pos[1], 34, true, "robot", TURNOS_AVISO_CIELO));
    }

    private void brotarDelSuelo(AvisoAparicion a) {
        double[] pos = asegurarLejosDelJugador(a.x, a.y, a.radio);
        boolean destructible = "caja".equals(a.tipo) || "barril".equals(a.tipo) || "chimenea".equals(a.tipo);
        if (destructible) {
            double res = "caja".equals(a.tipo) ? Arena.RESISTENCIA_CAJA
                    : "barril".equals(a.tipo) ? Arena.RESISTENCIA_BARRIL : Arena.RESISTENCIA_CHIMENEA;
            pelea.arena.agregarRepentina(a.tipo, pos[0], pos[1], a.radio, true, 0, 0, res);
        } else {
            boolean solido = "bloque".equals(a.tipo);
            int ciclo = "pinchos".equals(a.tipo) ? 115 : 0;
            int activo = "pinchos".equals(a.tipo) ? 55 : 0;
            pelea.arena.agregarRepentina(a.tipo, pos[0], pos[1], a.radio, solido, ciclo, activo, 0);
        }
        totalAparicionesSuelo++;
        pelea.suceso(Suceso.Tipo.TRAMPA, -1, pos[0], pos[1], 0, a.radio, "repentino_" + a.tipo);
    }

    private void aterrizarRobot(AvisoAparicion a) {
        int slot = buscarSlotLibre();
        if (slot < 0) return;
        Cuerpo c = pelea.cuerpo(1 + slot);
        if (c == null) return;
        double[] pos = asegurarLejosDelJugador(a.x, a.y, c.radio);
        c.enReserva = false;
        c.fuera = false;
        c.cayendo = 14;
        c.x = pos[0];
        c.y = pos[1];
        c.rumbo = azar.nextDouble() * 360 - 180;
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
        pelea.suceso(Suceso.Tipo.TRAMPA, -1, pos[0], pos[1], 0, c.radio, "repentino_robot");
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

    private double[] posicionSegura(double radio) {
        Cuerpo c0 = pelea.cuerpo(0);
        double margenBorde = Math.max(85.0, radio + 45.0);
        double baseX = c0 != null ? c0.x + (azar.nextDouble() * 2 - 1) * 950.0 : ANCHO / 2.0;
        double x = Math.max(margenBorde, Math.min(ANCHO - margenBorde, baseX));
        double y = margenBorde + azar.nextDouble() * (ALTO - 2 * margenBorde);
        return asegurarLejosDelJugador(x, y, radio);
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
        Idioma id = idioma == null ? Idioma.ES : idioma;
        switch (id) {
            case DE:
                return "import metajavbots.api.*;\n\n"
                        + "// Spezial-Herausforderung: 3x-Arena mit Countdown.\n"
                        + "// Nutze hindernisse() und gesehen(), um herabfallende defekte Roboter\n"
                        + "// sowie Kisten, Fässer und Schornsteine zu zerstören und zu überleben.\n"
                        + "public class UeberlebensBot extends MetajavbotDe {\n"
                        + "    @Override\n"
                        + "    public void jedenZug() {\n"
                        + "        if (!gesehen().isEmpty()) {\n"
                        + "            Gegner ziel = gesehen().get(0);\n"
                        + "            zielenNach(ziel.winkel());\n"
                        + "            angreifen();\n"
                        + "        } else {\n"
                        + "            for (Hindernis h : hindernisse()) {\n"
                        + "                if (h.typ().equals(\"caja\") || h.typ().equals(\"barril\") || h.typ().equals(\"chimenea\")) {\n"
                        + "                    zielenNach(h.winkel());\n"
                        + "                    angreifen();\n"
                        + "                    break;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        drehen(8);\n"
                        + "        vorwaerts(30);\n"
                        + "    }\n"
                        + "}\n";
            case EN:
                return "import metajavbots.api.*;\n\n"
                        + "// Special challenge: 3x arena with countdown.\n"
                        + "// Use obstacles() and seenEnemies() to destroy falling broken robots\n"
                        + "// plus crates, barrels, and chimneys while surviving until zero.\n"
                        + "public class SurvivalBot extends MetajavbotEn {\n"
                        + "    @Override\n"
                        + "    public void onTick() {\n"
                        + "        if (!seenEnemies().isEmpty()) {\n"
                        + "            Enemy target = seenEnemies().get(0);\n"
                        + "            aimTo(target.angle());\n"
                        + "            attack();\n"
                        + "        } else {\n"
                        + "            for (Obstacle o : obstacles()) {\n"
                        + "                if (o.type().equals(\"caja\") || o.type().equals(\"barril\") || o.type().equals(\"chimenea\")) {\n"
                        + "                    aimTo(o.angle());\n"
                        + "                    attack();\n"
                        + "                    break;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        turn(8);\n"
                        + "        ahead(30);\n"
                        + "    }\n"
                        + "}\n";
            default:
                return "import metajavbots.api.*;\n\n"
                        + "// Reto especial: arena 3x con cuenta atrás.\n"
                        + "// Usa obstaculos() y enemigosVistos() para destruir robots averiados\n"
                        + "// caídos del cielo y cajas, barriles o chimeneas mientras sobrevives.\n"
                        + "public class BotSupervivencia extends Metajavbot {\n"
                        + "    @Override\n"
                        + "    public void cadaTurno() {\n"
                        + "        if (!enemigosVistos().isEmpty()) {\n"
                        + "            Enemigo blanco = enemigosVistos().get(0);\n"
                        + "            apuntarHacia(blanco.angulo());\n"
                        + "            atacar();\n"
                        + "        } else {\n"
                        + "            for (Obstaculo o : obstaculos()) {\n"
                        + "                if (o.tipo().equals(\"caja\") || o.tipo().equals(\"barril\") || o.tipo().equals(\"chimenea\")) {\n"
                        + "                    apuntarHacia(o.angulo());\n"
                        + "                    atacar();\n"
                        + "                    break;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        girar(8);\n"
                        + "        avanzar(30);\n"
                        + "    }\n"
                        + "}\n";
        }
    }
}
