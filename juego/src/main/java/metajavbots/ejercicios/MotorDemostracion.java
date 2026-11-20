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
    private boolean ordenEmitida;
    private boolean enemigoDetectado;
    private int turnosTrasDeteccion;
    private boolean disparoRealizado;
    private int turnosTrasDisparo;
    private boolean impactoRealizado;
    private int turnosTrasImpacto;
    private double inicioX;
    private double inicioY;
    private double inicioRumbo;
    private double inicioMira;
    private double blindajeRivalInicial;
    private double distanciaRecorrida;
    private double giroAcumulado;
    private double giroMiraAcumulado;
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
        ordenEmitida = false;
        enemigoDetectado = false;
        turnosTrasDeteccion = 0;
        disparoRealizado = false;
        turnosTrasDisparo = 0;
        impactoRealizado = false;
        turnosTrasImpacto = 0;
        distanciaRecorrida = 0;
        giroAcumulado = 0;
        giroMiraAcumulado = 0;
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
        yo.x = p.accion == Demostracion.Accion.GIRAR ? 340 : 265;
        yo.y = 340;
        yo.rumbo = 0;
        switch (p.accion) {
            case RADAR:
                yo.mira = -130;
                break;
            case DETECTAR:
                yo.mira = -52;
                break;
            case APUNTAR_Y_DISPARAR:
                yo.mira = -26;
                break;
            default:
                yo.mira = 0;
                break;
        }
        rival.x = p.accion == Demostracion.Accion.APUNTAR_Y_DISPARAR ? 560 : 595;
        rival.y = 360;
        rival.rumbo = 180;
        rival.mira = 180;
        inicioX = yo.x;
        inicioY = yo.y;
        inicioRumbo = yo.rumbo;
        inicioMira = yo.mira;
        blindajeRivalInicial = rival.blindaje();
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
        Cuerpo rival = pelea.cuerpo(1);
        if (yo == null) return;
        distanciaRecorrida = Math.max(distanciaRecorrida, Math.hypot(yo.x - inicioX, yo.y - inicioY));
        giroAcumulado = Math.max(giroAcumulado, Math.abs(Pelea.normal(yo.rumbo - inicioRumbo)));
        giroMiraAcumulado = Math.max(giroMiraAcumulado, Math.abs(Pelea.normal(yo.mira - inicioMira)));
        if (enemigoDetectado) turnosTrasDeteccion++;
        if (disparoRealizado) turnosTrasDisparo++;
        if (rival != null && rival.blindaje() < blindajeRivalInicial - 0.01) impactoRealizado = true;
        if (impactoRealizado) turnosTrasImpacto++;
    }

    private void comprobarErrorEnPelea() {
        String err = pelea.combatientes[0].ultimoError;
        if (err != null && !err.isBlank()) ultimoError = err;
    }

    private boolean debePausar() {
        Demostracion.Paso p = pasoActual();
        if (turnosEnPaso >= p.turnosMaximos) return true;
        if (turnosEnPaso < p.turnosMinimos) return false;
        Cuerpo yo = pelea.cuerpo(0);
        switch (p.accion) {
            case AVANZAR:
                return yo != null && yo.restanteAvance() < 0.5 && distanciaRecorrida > 40;
            case GIRAR:
                return yo != null && Math.abs(yo.restanteGiro()) < 0.5 && giroAcumulado > 35;
            case RADAR:
                return giroMiraAcumulado >= 75;
            case DETECTAR:
                return enemigoDetectado && turnosTrasDeteccion >= 2;
            case APUNTAR_Y_DISPARAR:
                return enemigoDetectado && disparoRealizado && ((impactoRealizado && turnosTrasImpacto >= 4) || turnosTrasDisparo >= 20);
            default:
                return false;
        }
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
                case AVANZAR:
                    if (!ordenEmitida) {
                        avanzar(80);
                        ordenEmitida = true;
                    }
                    marcarLineaViva(6, 5);
                    break;
                case GIRAR:
                    if (!ordenEmitida) {
                        girar(45);
                        ordenEmitida = true;
                    }
                    marcarLineaViva(7, 5);
                    break;
                case RADAR:
                    apuntar(4);
                    marcarLineaViva(9, 8, 10);
                    break;
                case DETECTAR:
                    if (enemigosVistos().isEmpty()) {
                        apuntar(5);
                        marcarLineaViva(9, 8);
                    }
                    break;
                case APUNTAR_Y_DISPARAR:
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
            Demostracion.Accion a = pasoActual().accion;
            if (a == Demostracion.Accion.DETECTAR) {
                enemigoDetectado = true;
                marcarLineaViva(14, 8);
                return;
            }
            if (a != Demostracion.Accion.APUNTAR_Y_DISPARAR) return;
            enemigoDetectado = true;
            apuntarHacia(e.angulo());
            double error = Math.abs(normalizar(e.angulo() - mira()));
            if (!disparoRealizado && error < 5) {
                if (atacar()) disparoRealizado = true;
                marcarLineaViva(16, 15, 14);
            } else if (!disparoRealizado) {
                marcarLineaViva(15, 14, 16);
            } else {
                marcarLineaViva(16, 15, 14);
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

    public boolean impactoRealizado() {
        return impactoRealizado;
    }

    public double inicioX() {
        return inicioX;
    }

    public double inicioY() {
        return inicioY;
    }

    public double inicioRumbo() {
        return inicioRumbo;
    }

    public double distanciaRecorrida() {
        return distanciaRecorrida;
    }

    public double giroAcumulado() {
        return giroAcumulado;
    }

    public double giroMiraAcumulado() {
        return giroMiraAcumulado;
    }

    public String ultimoError() {
        return ultimoError;
    }
}
