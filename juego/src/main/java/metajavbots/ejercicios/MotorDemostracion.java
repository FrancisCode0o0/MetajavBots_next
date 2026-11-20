package metajavbots.ejercicios;

import java.util.BitSet;

import metajavbots.api.Enemigo;
import metajavbots.api.Metajavbot;
import metajavbots.api.RobotBase;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * MOTOR DE DEMOSTRACIÓN — Ejecuta una Demostracion en una Pelea real sin
 * depender de gráficos ni de javac: controla las pausas automáticas entre
 * pasos, la línea activa del código, el visor táctico y la repetición de
 * cualquier paso.
 */
public final class MotorDemostracion {

    private final Demostracion demo;
    private final Idioma idioma;
    private final Montaje montajeDemo;
    private Pelea pelea;
    private int indicePaso;
    private boolean pausado;
    private boolean terminada;
    private boolean visorTactico;
    private int turnosEnPaso;
    private int lineaActiva;
    private final BitSet lineasActivas = new BitSet();
    private boolean enemigoDetectado;
    private boolean disparoRealizado;
    private int turnosTrasDisparo;
    private double inicioX;
    private double inicioY;
    private double inicioRumbo;
    private double distanciaRecorrida;
    private double giroAcumulado;
    private String ultimoError = "";

    public MotorDemostracion(Demostracion demo, Idioma idioma) {
        this(demo, idioma, null);
    }

    public MotorDemostracion(Demostracion demo, Idioma idioma, Montaje montaje) {
        if (demo == null || demo.pasos.isEmpty()) {
            throw new IllegalArgumentException("La demostración necesita al menos un paso");
        }
        this.demo = demo;
        this.idioma = idioma == null ? Idioma.ES : idioma;
        this.montajeDemo = elegirMontaje(montaje);
        empezar();
    }

    private static Montaje elegirMontaje(Montaje preferido) {
        if (preferido != null && tieneArmaADistancia(preferido)) return preferido.copia();
        Modelo chispa = Modelo.de("chispa");
        if (chispa != null) return new Montaje(chispa);
        return new Montaje(Modelo.todos().get(0));
    }

    private static boolean tieneArmaADistancia(Montaje m) {
        return esDistancia(m.pieza(Montaje.Hueco.IZQUIERDA)) || esDistancia(m.pieza(Montaje.Hueco.DERECHA));
    }

    private static boolean esDistancia(metajavbots.piezas.Pieza p) {
        if (p == null) return false;
        metajavbots.piezas.Pieza.Arma a = p.arma();
        return a == metajavbots.piezas.Pieza.Arma.LASER || a == metajavbots.piezas.Pieza.Arma.BALA
                || a == metajavbots.piezas.Pieza.Arma.MISIL;
    }

    /** Arranca la demostración desde el primer paso. */
    public void empezar() {
        indicePaso = 0;
        terminada = false;
        ultimoError = "";
        iniciarPaso();
    }

    /** Repite desde cero el paso actual. */
    public void repetirPaso() {
        terminada = false;
        iniciarPaso();
    }

    /** Avanza al siguiente paso (o finaliza la demostración si ya era el último). */
    public void continuar() {
        if (indicePaso + 1 < demo.pasos.size()) {
            indicePaso++;
            iniciarPaso();
        } else {
            pausado = true;
            terminada = true;
        }
    }

    /** Activa o desactiva el visor táctico manualmente. */
    public void alternarVisor() {
        visorTactico = !visorTactico;
    }

    public void ponerVisor(boolean activo) {
        visorTactico = activo;
    }

    /** Avanza un turno real de la pelea si el paso está ejecutándose. */
    public void avanzarTurno() {
        if (pausado || terminada || pelea == null) return;
        if (pelea.estado() != Pelea.Estado.LUCHANDO) {
            pausarEnPaso();
            return;
        }
        pelea.avanzar();
        turnosEnPaso++;
        actualizarMetricas();
        comprobarErrorEnPelea();
        if (debePausar()) pausarEnPaso();
    }

    private void iniciarPaso() {
        Demostracion.Paso p = pasoActual();
        turnosEnPaso = 0;
        enemigoDetectado = false;
        disparoRealizado = false;
        turnosTrasDisparo = 0;
        distanciaRecorrida = 0;
        giroAcumulado = 0;
        visorTactico = p.activarVisor;
        prepararPelea(p);
        aplicarLineasDePaso(p);
        pausado = p.turnosMaximos <= 0;
    }

    private void prepararPelea(Demostracion.Paso p) {
        String nombreDemo = idioma.elegir("MiRobot", "MeinRoboter", "MyRobot");
        String nombreMuneco = T.en("pelea.muneco", idioma);
        if (nombreMuneco.equals("pelea.muneco")) nombreMuneco = idioma.elegir("Muñeco", "Puppe", "Dummy");
        String idMuneco = "cobalto".equals(montajeDemo.modelo.id) ? "mazo" : "cobalto";
        Modelo modMuneco = Modelo.de(idMuneco) != null ? Modelo.de(idMuneco) : Modelo.todos().get(0);
        Combatiente yo = new Combatiente(nombreDemo, montajeDemo.copia(), fabricaDemo());
        Combatiente muneco = new Combatiente(nombreMuneco, new Montaje(modMuneco), fabricaMuneco());
        pelea = new Pelea(yo, muneco, 42L + indicePaso * 7L, 1000, 700, 1200);
        pelea.rondas = 1;
        pelea.empezarRonda();
        posicionarParaPaso(p);
    }

    private void posicionarParaPaso(Demostracion.Paso p) {
        Cuerpo yo = pelea.cuerpo(0);
        Cuerpo rival = pelea.cuerpo(1);
        if (yo == null || rival == null) return;
        yo.x = 270;
        yo.y = 330;
        yo.rumbo = 0;
        yo.mira = p.accion == Demostracion.Accion.RADAR_Y_DISPARO ? -52 : 0;
        rival.x = 590;
        rival.y = 365;
        rival.rumbo = 180;
        rival.mira = 180;
        inicioX = yo.x;
        inicioY = yo.y;
        inicioRumbo = yo.rumbo;
    }

    private Fabrica fabricaDemo() {
        return new Fabrica() {
            @Override
            public RobotBase crear() {
                return new RobotDeDemo();
            }
        };
    }

    private Fabrica fabricaMuneco() {
        return new Fabrica() {
            @Override
            public RobotBase crear() {
                return new MunecoQuieto();
            }
        };
    }

    private void actualizarMetricas() {
        Cuerpo yo = pelea.cuerpo(0);
        if (yo == null) return;
        distanciaRecorrida = Math.max(distanciaRecorrida, Math.hypot(yo.x - inicioX, yo.y - inicioY));
        giroAcumulado = Math.max(giroAcumulado, Math.abs(Pelea.normal(yo.rumbo - inicioRumbo)));
        if (disparoRealizado) turnosTrasDisparo++;
    }

    private void comprobarErrorEnPelea() {
        String err = pelea.combatientes[0].ultimoError;
        if (err != null && !err.isBlank()) ultimoError = err;
    }

    private boolean debePausar() {
        Demostracion.Paso p = pasoActual();
        if (turnosEnPaso >= p.turnosMaximos) return true;
        if (turnosEnPaso < p.turnosMinimos) return false;
        if (p.accion == Demostracion.Accion.RADAR_Y_DISPARO) {
            return enemigoDetectado && disparoRealizado && turnosTrasDisparo >= 10;
        }
        return false;
    }

    private void pausarEnPaso() {
        pausado = true;
        aplicarLineasDePaso(pasoActual());
    }

    private void aplicarLineasDePaso(Demostracion.Paso p) {
        lineasActivas.clear();
        lineaActiva = p.lineaPrincipal;
        if (p.lineaPrincipal > 0) lineasActivas.set(p.lineaPrincipal);
        for (int l : p.lineasSecundarias) {
            if (l > 0) lineasActivas.set(l);
        }
    }

    private void marcarLineaViva(int principal, int... secundarias) {
        lineasActivas.clear();
        lineaActiva = principal;
        if (principal > 0) lineasActivas.set(principal);
        for (int l : secundarias) {
            if (l > 0) lineasActivas.set(l);
        }
    }

    /** Robot determinista que ejecuta en la arena la acción del paso actual sincronizada con las líneas del código. */
    private final class RobotDeDemo extends Metajavbot {

        @Override
        public void cadaTurno() {
            Demostracion.Paso p = pasoActual();
            switch (p.accion) {
                case MOVER_Y_GIRAR:
                    avanzar(120);
                    girar(12);
                    if (turnosEnPaso < 24) marcarLineaViva(6, 7);
                    else marcarLineaViva(7, 6);
                    break;
                case RADAR_Y_DISPARO:
                    if (enemigosVistos().isEmpty()) {
                        apuntar(5);
                        marcarLineaViva(9, 8);
                    }
                    break;
                case QUIETO:
                case CIERRE:
                default:
                    detener();
                    break;
            }
        }

        @Override
        public void alVerEnemigo(Enemigo e) {
            if (pasoActual().accion != Demostracion.Accion.RADAR_Y_DISPARO) return;
            enemigoDetectado = true;
            apuntarHacia(e.angulo());
            double error = Math.abs(normalizar(e.angulo() - mira()));
            if (error < 6) {
                if (atacar()) disparoRealizado = true;
                marcarLineaViva(16, 14, 15);
            } else {
                marcarLineaViva(15, 14);
            }
        }
    }

    /** Muñeco tranquilo que espera en la arena para que la demostración sea clara y reproducible. */
    private static final class MunecoQuieto extends Metajavbot {
        @Override
        public void cadaTurno() {
            detener();
        }
    }

    // ================================================================ estado observable

    public Demostracion demostracion() {
        return demo;
    }

    public Idioma idioma() {
        return idioma;
    }

    public Pelea pelea() {
        return pelea;
    }

    public Demostracion.Paso pasoActual() {
        return demo.pasos.get(indicePaso);
    }

    public int indicePaso() {
        return indicePaso;
    }

    public int totalPasos() {
        return demo.pasos.size();
    }

    public boolean pausado() {
        return pausado;
    }

    public boolean terminada() {
        return terminada;
    }

    public boolean esUltimoPaso() {
        return indicePaso == demo.pasos.size() - 1;
    }

    public boolean visorTactico() {
        return visorTactico;
    }

    public int lineaActiva() {
        return lineaActiva;
    }

    public BitSet lineasActivas() {
        return (BitSet) lineasActivas.clone();
    }

    public boolean enemigoDetectado() {
        return enemigoDetectado;
    }

    public boolean disparoRealizado() {
        return disparoRealizado;
    }

    public double distanciaRecorrida() {
        return distanciaRecorrida;
    }

    public double giroAcumulado() {
        return giroAcumulado;
    }

    public String ultimoError() {
        return ultimoError;
    }
}
