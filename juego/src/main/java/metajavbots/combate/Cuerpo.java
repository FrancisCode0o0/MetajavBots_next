package metajavbots.combate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metajavbots.api.Avistado;
import metajavbots.api.Impacto;
import metajavbots.api.Mando;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;

/**
 * CUERPO — Un robot en la arena durante una ronda: dónde está, hacia dónde
 * va, lo que le queda de cada parte, su energía y sus órdenes pendientes. Es
 * también el Mando que su código usa (avanzar, girar, atacar…): cada orden
 * se recorta a lo que es posible y nunca rompe la pelea, venga lo que venga.
 */
public final class Cuerpo implements Mando {

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
    /** Los turnos que le quedan en el aire tras un volteador (se ve levantado; no se mueve). */
    public int volteado;
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
        this.radio = c.montaje.modelo.tamano;
        this.x = x;
        this.y = y;
        this.rumbo = rumbo;
        this.mira = rumbo;
        this.energia = cifras.energiaMax;
        for (int i = 0; i < 4; i++) {
            parte[i] = cifras.blindaje[i];
            parteMax[i] = cifras.blindaje[i];
        }
        this.azar = new Random(semilla);
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
        return pelea.turno();
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
        // Se marca ya, para que dos llamadas en el mismo turno no ataquen dos veces.
        enfriamiento[brazo] = Math.max(1, (int) Math.round(p.valor("enfriamiento", 30) / (1 + pelea.refuerzoDeEnfriamiento(this))));
        energia -= costeDe(brazo, p);
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
        if (p.arma() == Pieza.Arma.CUERPO) return p.valor("alcance") + radio * 2;
        return p.valor("alcance");
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
    public void avisarAlEquipo(String mensaje) {
        if (mensaje == null) return;
        String m = mensaje.length() > 200 ? mensaje.substring(0, 200) : mensaje;
        pelea.avisar(this, m);
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
        return brazo == CABEZA ? p.valor("coste") : p.valor("energia");
    }

    private static double limpio(double v, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0;
        return Math.max(-max, Math.min(max, v));
    }

    public boolean vuela() {
        return cifras.vuela;
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
}
