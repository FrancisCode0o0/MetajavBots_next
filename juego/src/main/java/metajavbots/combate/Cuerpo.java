package metajavbots.combate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metajavbots.api.Avistado;
import metajavbots.api.Impacto;
import metajavbots.api.Mando;
import metajavbots.api.Puente;
import metajavbots.piezas.Arquetipo;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * CUERPO — Un robot en la arena durante una ronda: dónde está, hacia dónde
 * va, lo que le queda de cada parte, su energía y sus órdenes pendientes. Es
 * también el Mando que su código usa (avanzar, girar, atacar…): cada orden
 * se recorta a lo que es posible y nunca rompe la pelea, venga lo que venga.
 */
public final class Cuerpo implements Mando {

    /** Motivo por el que el robot no pudo usar su código en el turno actual. */
    public enum EstadoPensamiento {
        EJECUTADO,
        ATURDIDO,
        DEUDA_TOPE,
        FUERA_DE_COMBATE,
        SIN_CODIGO
    }

    /** Límites de equilibrio anti-abuso para cadencia, recarga, energía y movilidad en combate. */
    public static final int MIN_ENFRIAMIENTO = 3;
    public static final int MAX_ENFRIAMIENTO = 600;
    public static final double MIN_RECARGA = 0.1;
    public static final double MAX_RECARGA = 1.2;
    public static final double MIN_ENERGIA_MAX = 40.0;
    public static final double MAX_ENERGIA_MAX = 220.0;
    public static final double MAX_COSTE = 100.0;
    public static final double MIN_VELOCIDAD = 1.0;
    public static final double MAX_VELOCIDAD = 12.0;
    public static final double MIN_GIRO = 1.5;
    public static final double MAX_GIRO = 18.0;
    public static final double MIN_BLINDAJE_PARTE = 10.0;
    public static final double MAX_BLINDAJE_PARTE = 250.0;

    final Pelea pelea;
    public final int indice;
    /** Su equipo (en una pelea de dos, 0 y 1; en un battle royale, cada uno el suyo). */
    public final int equipo;
    /** Los avisos de los compañeros que le llegan el turno siguiente: {de, mensaje}. */
    final List<String[]> avisos = new ArrayList<>();
    List<Avistamiento> companeros = new ArrayList<>();
    public final Combatiente combatiente;
    public final Montaje.Cifras cifras;
    public final double radio;

    public double x, y, rumbo, mira, velocidad;
    double avance, giro, giroMira;
    public double energia;
    public final double[] parte = new double[4];
    public final double[] parteMax = new double[4];
    final int[] enfriamiento = new int[3];
    final boolean[] pedido = new boolean[3];
    /** Lo que dura cada efecto (en turnos). */
    public int escudoBrazo, escudoCabeza, turbo, camuflaje, sobrecarga, aturdido, llama, deuda;
    /** Los turnos que le quedan flotando (el propulsor antigravedad) y echando fuego (el lanzallamas de un aparato). */
    public int flotando, llamaAparato;
    public double miraDeLaLlama;
    /** Los turnos que le quedan en el aire tras un volteador (se ve levantado; no se mueve). */
    public int volteado;
    /** Turnos de barrido táctico en 360°, enfriamiento de la habilidad de arquetipo y cadencia del aviso. */
    public int pulsoTactico;
    int enfriamientoArquetipo, avisoArquetipo;
    int brazoDelEscudo = -1, brazoDeLaLlama = -1;
    double bloqueoBrazo, conoEscudo, bloqueoCabeza, factorTurbo = 1, factorSobrecarga = 1;
    public boolean fuera;
    /** Lo que dice (el bocadillo) y cuánto le queda. */
    public String bocadillo = "";
    public int tiempoDeBocadillo;
    /** Lo que ha pasado desde el último turno (se le cuenta a su código). */
    final List<Impacto> golpesRecibidos = new ArrayList<>();
    final List<Impacto> aciertos = new ArrayList<>();
    boolean chocoConPared;
    Avistado chocoCon;
    List<Avistamiento> vistos = new ArrayList<>();
    final Random azar;
    /** Las líneas de su código que se ejecutaron en el último turno que pensó, y la última (con la traza). */
    public java.util.BitSet lineasDelTurno = new java.util.BitSet();
    public int ultimaLinea;
    /** Las órdenes pedidas en el último turno que pensó y si alguna fue pisada (para el depurador F9). */
    public List<OrdenDelTurno> ordenesDelTurno = List.of();
    public boolean tieneConflictoDeOrdenes;
    public EstadoPensamiento estadoPensamiento = EstadoPensamiento.EJECUTADO;
    /** El turno que acaba de recibir el código del robot (desde 0). */
    public int turnoPensado;
    private final List<OrdenDelTurno> ordenesEnCurso = new ArrayList<>();
    private String metodoActual = "";
    private int invocacionActual;
    private boolean registrandoOrdenes;
    /** Pasos de cálculo del último turno (para el medidor y el depurador). */
    public long pasosDelTurno;
    /** Para el dibujo: cuánto se ha movido (las piernas), el último golpe recibido y el retroceso del arma. */
    public double paso, sacudida, retroceso;
    public int ultimoAtaque = -100, ultimoGolpe = -100;
    public int brazoDelUltimoAtaque;

    Cuerpo(Pelea pelea, int indice, Combatiente c, double x, double y, double rumbo, long semilla) {
        this.pelea = pelea;
        this.indice = indice;
        this.equipo = pelea.equipo[indice];
        this.combatiente = c;
        this.cifras = c.montaje.cifras();
        this.radio = c.escalaBlindaje < 0.7 ? c.montaje.modelo.tamano * 0.82 : c.montaje.modelo.tamano;
        this.x = x;
        this.y = y;
        this.rumbo = rumbo;
        this.mira = rumbo;
        this.energia = energiaMaxSegura(cifras.energiaMax);
        for (int i = 0; i < 4; i++) {
            double bl = blindajeSeguro(cifras.blindaje[i] * c.escalaBlindaje);
            parte[i] = bl;
            parteMax[i] = bl;
        }
        limpiarTelemetriaDeTurno(EstadoPensamiento.EJECUTADO);
        this.azar = new Random(semilla);
    }

    /** Restablece la telemetría visible del turno y registra por qué no hubo ejecución normal. */
    public void limpiarTelemetriaDeTurno(EstadoPensamiento estado) {
        pasosDelTurno = 0;
        lineasDelTurno.clear();
        ultimaLinea = -1;
        ordenesDelTurno = new ArrayList<>();
        tieneConflictoDeOrdenes = false;
        estadoPensamiento = estado == null ? EstadoPensamiento.EJECUTADO : estado;
    }

    /** Indica si el robot no pudo ejecutar su código en el turno actual por bloqueo, aturdimiento, deuda o falta de código. */
    public boolean sinEjecucion() {
        return estadoPensamiento != null && estadoPensamiento != EstadoPensamiento.EJECUTADO;
    }

    /** Turnos restantes tras este turno de aturdimiento o deuda de pasos (0 si no aplica). */
    public int turnosSinEjecucionRestantes() {
        if (estadoPensamiento == EstadoPensamiento.ATURDIDO) return Math.max(0, aturdido);
        if (estadoPensamiento == EstadoPensamiento.DEUDA_TOPE) return Math.max(0, deuda);
        return 0;
    }

    /** Descarta los eventos pendientes que el código bloqueado no debe recibir. */
    public void limpiarEventosPendientes() {
        golpesRecibidos.clear();
        aciertos.clear();
        chocoConPared = false;
        chocoCon = null;
        avisos.clear();
    }

    /** Coloca los cuerpos al empezar una ronda: 1v1, 1 contra varios en formación o corro por equipos. */
    static Cuerpo[] posicionar(Pelea pelea, int ronda, long semilla, Random azar) {
        double margen = 120;
        Combatiente[] cs = pelea.combatientes;
        int n = cs.length;
        if (n == 2) {
            double ya = margen + azar.nextDouble() * (pelea.alto - 2 * margen);
            double yb = margen + azar.nextDouble() * (pelea.alto - 2 * margen);
            boolean cambia = ronda % 2 == 0;
            double xa = cambia ? pelea.ancho - margen : margen;
            double xb = cambia ? margen : pelea.ancho - margen;
            return new Cuerpo[]{
                    new Cuerpo(pelea, 0, cs[0], xa, ya, cambia ? 180 : 0, semilla * 31 + ronda * 7),
                    new Cuerpo(pelea, 1, cs[1], xb, yb, cambia ? 0 : 180, semilla * 31 + ronda * 7 + 3)};
        }
        if (pelea.esUnoContraVarios()) {
            boolean cambia = ronda % 2 == 0;
            double xJug = cambia ? pelea.ancho - margen : margen;
            double xRiv = cambia ? margen : pelea.ancho - margen;
            double rumboJug = cambia ? 180 : 0;
            double rumboRiv = cambia ? 0 : 180;
            Cuerpo[] res = new Cuerpo[n];
            double yCentro = pelea.alto / 2 + (azar.nextDouble() - 0.5) * 120;
            res[0] = new Cuerpo(pelea, 0, cs[0], xJug, yCentro, rumboJug, semilla * 31 + ronda * 7);
            int enemigos = n - 1;
            double paso = Math.min(110, (pelea.alto - 2 * margen) / Math.max(1, enemigos));
            double yInicio = yCentro - paso * (enemigos - 1) / 2.0;
            for (int i = 1; i < n; i++) {
                double y = Math.max(margen, Math.min(pelea.alto - margen, yInicio + (i - 1) * paso));
                double desfaseX = ((i - 1) % 2) * (cambia ? 45 : -45);
                res[i] = new Cuerpo(pelea, i, cs[i], xRiv + desfaseX, y, rumboRiv, semilla * 31 + ronda * 7 + i * 3);
            }
            return res;
        }
        // Varios: en corro alrededor del centro, los del mismo equipo juntos, mirando al centro.
        Integer[] orden = new Integer[n];
        for (int i = 0; i < n; i++) orden[i] = i;
        java.util.Arrays.sort(orden, (p, q) -> Integer.compare(pelea.equipo[p], pelea.equipo[q]));
        double giro = azar.nextDouble() * 360;
        Cuerpo[] res = new Cuerpo[n];
        for (int k = 0; k < n; k++) {
            int i = orden[k];
            double a = Math.toRadians(giro + 360.0 * k / n);
            double x = pelea.ancho / 2 + Math.cos(a) * (pelea.ancho / 2 - margen);
            double y = pelea.alto / 2 + Math.sin(a) * (pelea.alto / 2 - margen);
            res[i] = new Cuerpo(pelea, i, cs[i], x, y, Pelea.normal(Math.toDegrees(a) + 180), semilla * 31 + ronda * 7 + i * 3);
        }
        return res;
    }

    // ================================================================ el Mando (lo que usa su código)

    @Override
    public double x() {
        return x;
    }

    @Override
    public double y() {
        return y;
    }

    @Override
    public double rumbo() {
        return Pelea.normal(rumbo);
    }

    @Override
    public double mira() {
        return Pelea.normal(mira);
    }

    @Override
    public double velocidad() {
        return velocidad;
    }

    @Override
    public double energia() {
        return energia;
    }

    @Override
    public double blindaje() {
        double t = 0, m = 0;
        for (int i = 0; i < 4; i++) {
            t += Math.max(0, parte[i]);
            m += parteMax[i];
        }
        return m <= 0 ? 0 : 100 * t / m;
    }

    @Override
    public int turno() {
        return turnoPensado;
    }

    @Override
    public int ronda() {
        return pelea.ronda();
    }

    @Override
    public double anchoArena() {
        return pelea.ancho;
    }

    @Override
    public double altoArena() {
        return pelea.alto;
    }

    @Override
    public double restanteAvance() {
        return avance;
    }

    @Override
    public double restanteGiro() {
        return giro;
    }

    @Override
    public double restanteMira() {
        return giroMira;
    }

    @Override
    public void avanzar(double distancia) {
        avance = limpio(distancia, 5000);
    }

    @Override
    public void girar(double grados) {
        giro = limpio(grados, 3600);
    }

    @Override
    public void apuntar(double grados) {
        giroMira = limpio(grados, 3600);
    }

    @Override
    public void detener() {
        avance = 0;
        giro = 0;
    }

    @Override
    public boolean atacar(int brazo) {
        if (brazo < 0 || brazo > 2 || !listo(brazo)) return false;
        pedido[brazo] = true;
        Pieza p = piezaDe(brazo);
        // Se marca ya, para que dos llamadas en el mismo turno no ataquen dos veces (nunca cadencia infinita ni recarga <= 0).
        enfriamiento[brazo] = enfriamientoSeguro(p.valor("enfriamiento", 30), pelea.refuerzoDeEnfriamiento(this));
        energia = Math.max(0, energia - costeDe(brazo, p));
        return true;
    }

    @Override
    public boolean listo(int brazo) {
        if (brazo < 0 || brazo > 2 || fuera) return false;
        Pieza p = piezaDe(brazo);
        if (p == null) return false;
        if (brazo == CABEZA ? p.especial() == Pieza.Especial.NINGUNO : p.arma() == Pieza.Arma.NINGUNA) return false;
        if (brazo != CABEZA && parte[brazo == IZQUIERDA ? Montaje.IZQUIERDA : Montaje.DERECHA] <= 0) return false;
        return enfriamiento[brazo] <= 0 && !pedido[brazo] && energia >= costeDe(brazo, p);
    }

    @Override
    public String arma(int brazo) {
        if (brazo == CABEZA) {
            Pieza p = piezaDe(CABEZA);
            return p == null ? "" : p.especial() == Pieza.Especial.NINGUNO ? "" : p.especial().name();
        }
        Pieza p = piezaDe(brazo);
        return p == null ? "" : p.arma() == Pieza.Arma.NINGUNA ? "" : p.arma().name();
    }

    @Override
    public double alcance(int brazo) {
        Pieza p = piezaDe(brazo);
        if (p == null) return 0;
        double alc = Armas.alcanceSeguro(p.valor("alcance"));
        if (p.arma() == Pieza.Arma.CUERPO) return alc + radio * 2;
        return alc;
    }

    @Override
    public List<? extends Avistado> vistos() {
        return vistos;
    }

    @Override
    public List<? extends Avistado> companeros() {
        return companeros;
    }

    @Override
    public List<? extends metajavbots.api.Cosa> obstaculos() {
        return pelea.arena.vistas(this);
    }

    @Override
    public List<? extends metajavbots.api.Cacharro> aparatos() {
        return pelea.aparatos.vistas(this, false);
    }

    @Override
    public metajavbots.api.Cacharro miAparato() {
        List<metajavbots.api.Cacharro> l = pelea.aparatos.vistas(this, true);
        return l.isEmpty() ? null : l.get(0);
    }

    @Override
    public boolean conectar(String hueco) {
        return pelea.aparatos.conectar(this, hueco);
    }

    @Override
    public boolean usarAparato(boolean conAngulo, double angulo) {
        return pelea.aparatos.usar(this, conAngulo ? angulo : mira);
    }

    @Override
    public void avisarAlEquipo(String mensaje) {
        if (mensaje == null) return;
        String m = mensaje.length() > 200 ? mensaje.substring(0, 200) : mensaje;
        pelea.avisar(this, m);
    }

    @Override
    public String arquetipo() {
        return combatiente.montaje.modelo.arquetipo.name();
    }

    @Override
    public boolean embestir() {
        if (!exigirArquetipo(Arquetipo.ASALTO)) return false;
        if (fuera || enfriamientoArquetipo > 0 || energia < 12) return false;
        energia = Math.max(0, energia - 12);
        enfriamientoArquetipo = 90;
        turbo = Math.max(turbo, 18);
        factorTurbo = Math.max(factorTurbo, 1.55);
        avance = Math.max(avance, 140);
        pelea.suceso(Suceso.Tipo.ESPECIAL, indice, x, y, rumbo, 18, "embestir");
        return true;
    }

    @Override
    public boolean pulsoRadar() {
        if (!exigirArquetipo(Arquetipo.TACTICO)) return false;
        if (fuera || enfriamientoArquetipo > 0 || energia < 10) return false;
        energia = Math.max(0, energia - 10);
        enfriamientoArquetipo = 90;
        pulsoTactico = Math.max(pulsoTactico, 25);
        pelea.actualizarVistos(this);
        pelea.suceso(Suceso.Tipo.ESPECIAL, indice, x, y, mira, 25, "pulso_radar");
        return true;
    }

    @Override
    public boolean fortificar() {
        if (!exigirArquetipo(Arquetipo.DEFENSOR)) return false;
        if (fuera || enfriamientoArquetipo > 0 || energia < 12) return false;
        energia = Math.max(0, energia - 12);
        enfriamientoArquetipo = 105;
        escudoCabeza = Math.max(escudoCabeza, 45);
        bloqueoCabeza = Math.max(bloqueoCabeza, 0.55);
        avance = 0;
        pelea.suceso(Suceso.Tipo.ESPECIAL, indice, x, y, rumbo, 45, "fortificar");
        return true;
    }

    private boolean exigirArquetipo(Arquetipo requerido) {
        Modelo mod = combatiente.montaje.modelo;
        if (mod.arquetipo == requerido) return true;
        if (avisoArquetipo <= 0) {
            Idioma id = T.idioma == null ? Idioma.ES : T.idioma;
            String msg = id.elegir(
                    "⚠ " + requerido.habilidad(Idioma.ES) + " solo funciona en robots de arquetipo "
                            + requerido.nombre(Idioma.ES) + " (tu modelo " + mod.nombre(Idioma.ES) + " es " + mod.arquetipo.nombre(Idioma.ES) + ").",
                    "⚠ " + requerido.habilidad(Idioma.DE) + " funktioniert nur bei Robotern des Archetyps "
                            + requerido.nombre(Idioma.DE) + " (dein Modell " + mod.nombre(Idioma.DE) + " ist " + mod.arquetipo.nombre(Idioma.DE) + ").",
                    "⚠ " + requerido.habilidad(Idioma.EN) + " only works on " + requerido.nombre(Idioma.EN)
                            + " archetype robots (your model " + mod.nombre(Idioma.EN) + " is " + mod.arquetipo.nombre(Idioma.EN) + ").");
            escribir(msg);
            avisoArquetipo = 30;
        }
        return false;
    }

    void pasoArquetipo() {
        if (pulsoTactico > 0) pulsoTactico--;
        if (enfriamientoArquetipo > 0) enfriamientoArquetipo--;
        if (avisoArquetipo > 0) avisoArquetipo--;
    }

    @Override
    public void decir(String texto) {
        if (texto == null) return;
        String t = texto.length() > 120 ? texto.substring(0, 120) + "…" : texto;
        if (!t.equals(bocadillo) || tiempoDeBocadillo <= 0) {
            pelea.suceso(Suceso.Tipo.DECIR, indice, x, y, 0, 0, t);
        }
        bocadillo = t;
        tiempoDeBocadillo = 90;
    }

    @Override
    public void escribir(String texto) {
        combatiente.escribir(pelea.turno(), texto);
    }

    @Override
    public Map<String, Object> memoria() {
        return combatiente.memoria;
    }

    @Override
    public Random azar() {
        return azar;
    }

    @Override
    public void enMetodo(String metodo) {
        if (!registrandoOrdenes) return;
        if (metodo != null && !metodo.isEmpty()) {
            invocacionActual++;
            metodoActual = metodo;
        } else {
            metodoActual = "";
        }
    }

    @Override
    public void anotarOrden(String accion, int canales) {
        if (!registrandoOrdenes || ordenesEnCurso.size() >= 64) return;
        ordenesEnCurso.add(new OrdenDelTurno(metodoActual, accion, canales, Puente.ultimaLinea(), invocacionActual, "", 0));
    }

    void empezarRegistroDeTurno() {
        ordenesEnCurso.clear();
        metodoActual = "";
        invocacionActual = 0;
        registrandoOrdenes = true;
    }

    void cerrarRegistroDeTurno() {
        registrandoOrdenes = false;
        metodoActual = "";
        int n = ordenesEnCurso.size();
        if (n == 0) {
            ordenesDelTurno = List.of();
            tieneConflictoDeOrdenes = false;
            return;
        }
        List<OrdenDelTurno> resultado = new ArrayList<>(n);
        boolean conflicto = false;
        for (int i = 0; i < n; i++) {
            OrdenDelTurno o = ordenesEnCurso.get(i);
            if (o.canales != 0) {
                for (int j = n - 1; j > i; j--) {
                    OrdenDelTurno posterior = ordenesEnCurso.get(j);
                    if (posterior.invocacion > o.invocacion && (o.canales & posterior.canales) != 0) {
                        o = o.conPisada(posterior.metodo, posterior.linea);
                        conflicto = true;
                        break;
                    }
                }
            }
            resultado.add(o);
        }
        ordenesDelTurno = Collections.unmodifiableList(resultado);
        tieneConflictoDeOrdenes = conflicto;
    }

    // ================================================================ por dentro

    Pieza piezaDe(int brazo) {
        Montaje m = combatiente.montaje;
        switch (brazo) {
            case IZQUIERDA:
                return m.pieza(Montaje.Hueco.IZQUIERDA);
            case DERECHA:
                return m.pieza(Montaje.Hueco.DERECHA);
            default:
                return m.pieza(Montaje.Hueco.CABEZA);
        }
    }

    static double costeDe(int brazo, Pieza p) {
        return costeSeguro(brazo == CABEZA ? p.valor("coste") : p.valor("energia"));
    }

    /** Enfriamiento acotado en turnos: rechaza valores <= 0, NaN o infinitos (evita cadencia infinita). */
    public static int enfriamientoSeguro(double base, double refuerzo) {
        double b = Double.isFinite(base) && base > 0 ? base : MIN_ENFRIAMIENTO;
        double r = Double.isFinite(refuerzo) ? Math.max(0, Math.min(1.5, refuerzo)) : 0;
        int turnos = (int) Math.round(b / (1.0 + r));
        return Math.max(MIN_ENFRIAMIENTO, Math.min(MAX_ENFRIAMIENTO, turnos));
    }

    /** Coste de energía acotado (nunca negativo ni infinito). */
    public static double costeSeguro(double coste) {
        if (!Double.isFinite(coste) || coste <= 0) return 0;
        return Math.min(MAX_COSTE, coste);
    }

    /** Recarga de energía por turno acotada (siempre > 0 y dentro de límites). */
    public static double recargaSegura(double recarga) {
        if (!Double.isFinite(recarga) || recarga <= 0) return MIN_RECARGA;
        return Math.max(MIN_RECARGA, Math.min(MAX_RECARGA, recarga));
    }

    /** Energía máxima acotada dentro de límites de equilibrio. */
    public static double energiaMaxSegura(double energiaMax) {
        if (!Double.isFinite(energiaMax) || energiaMax <= 0) return MIN_ENERGIA_MAX;
        return Math.max(MIN_ENERGIA_MAX, Math.min(MAX_ENERGIA_MAX, energiaMax));
    }

    /** Velocidad máxima acotada dentro de límites de equilibrio. */
    public static double velocidadSegura(double velocidad) {
        if (!Double.isFinite(velocidad) || velocidad <= 0) return MIN_VELOCIDAD;
        return Math.max(MIN_VELOCIDAD, Math.min(MAX_VELOCIDAD, velocidad));
    }

    /** Giro máximo acotado dentro de límites de equilibrio. */
    public static double giroSeguro(double giro) {
        if (!Double.isFinite(giro) || giro <= 0) return MIN_GIRO;
        return Math.max(MIN_GIRO, Math.min(MAX_GIRO, giro));
    }

    /** Blindaje por parte acotado dentro de límites de equilibrio. */
    public static double blindajeSeguro(double blindaje) {
        if (!Double.isFinite(blindaje) || blindaje <= 0) return MIN_BLINDAJE_PARTE;
        return Math.max(MIN_BLINDAJE_PARTE, Math.min(MAX_BLINDAJE_PARTE, blindaje));
    }

    private static double limpio(double v, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return Math.max(-max, Math.min(max, v));
    }

    public boolean vuela() {
        return cifras.vuela || flotando > 0 || volteado > 0;
    }

    /** Cuánto le queda de blindaje en total (sin porcentaje). */
    public double blindajeQueQueda() {
        double t = 0;
        for (double p : parte) t += Math.max(0, p);
        return t;
    }

    public boolean parteRota(int p) {
        return parte[p] <= 0;
    }

    /** Turnos que le quedan al arma de ese brazo para volver a estar lista (0 si ya puede disparar). */
    public int enfriamiento(int brazo) {
        return brazo >= 0 && brazo < enfriamiento.length ? enfriamiento[brazo] : 0;
    }

    /** Efectos POO activos de este robot (calculados por reflexión sobre su clase compilada). */
    public ModificadoresPoo efectosPoo() {
        return combatiente.efectosPoo == null ? ModificadoresPoo.NINGUNO : combatiente.efectosPoo;
    }

    /** Presupuesto efectivo de pasos de cálculo por turno, aplicando el bono de modificadores {@code final}. */
    public long topeDePasos() {
        double base = pelea.topeDePasos > 0 ? pelea.topeDePasos : cifras.pasos;
        return Math.max(1L, Math.round(base * efectosPoo().factorPasos));
    }

    /** Analiza los modificadores POO de la clase del robot al iniciar la ronda. */
    void actualizarEfectosPoo() {
        boolean enEquipo = false;
        for (int i = 0; i < pelea.equipo.length; i++) {
            if (i != indice && pelea.equipo[i] == equipo) {
                enEquipo = true;
                break;
            }
        }
        combatiente.efectosPoo = combatiente.robot == null
                ? ModificadoresPoo.NINGUNO
                : ModificadoresPoo.analizar(combatiente.robot.getClass(), enEquipo);
    }
}
