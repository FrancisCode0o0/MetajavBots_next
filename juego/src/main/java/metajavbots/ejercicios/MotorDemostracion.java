package metajavbots.ejercicios;

import java.util.BitSet;
import java.util.List;

import metajavbots.api.Enemigo;
import metajavbots.api.Metajavbot;
import metajavbots.api.Obstaculo;
import metajavbots.api.RobotBase;
import metajavbots.combate.Arena;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
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
    private boolean obstaculoDestruido;
    private double inicioX;
    private double inicioY;
    private double inicioRumbo;
    private double inicioMira;
    private double blindajeRivalInicial;
    private double distanciaRecorrida;
    private double giroAcumulado;
    private double giroMiraAcumulado;
    private String ultimoError = "";
    private final SoporteDemo4 demo4 = new SoporteDemo4();

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
        ajustarAvanceExacto();
        turnosEnPaso++;
        actualizarMetricas();
        comprobarErrorEnPelea();
        if (debePausar()) pausarEnPaso();
    }

    private void ajustarAvanceExacto() {
        if (pasoActual().accion != Demostracion.Accion.AVANZAR) return;
        Cuerpo yo = pelea.cuerpo(0);
        if (yo == null) return;
        double dx = yo.x - inicioX;
        double dy = yo.y - inicioY;
        double dist = Math.hypot(dx, dy);
        if (dist >= 80.0) {
            double ux = dx / dist;
            double uy = dy / dist;
            yo.x = inicioX + ux * 80.0;
            yo.y = inicioY + uy * 80.0;
            yo.velocidad = 0;
            yo.detener();
        }
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
        obstaculoDestruido = false;
        distanciaRecorrida = 0;
        giroAcumulado = 0;
        giroMiraAcumulado = 0;
        demo4.reiniciar();
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
        boolean esDemo4 = Demostracion.ID_DEMO_4.equals(demo.id);
        Fabrica fYo = esDemo4 ? demo4.fabricaJugador(p.accion, enlaceDemo4()) : fabricaDemo();
        Fabrica fRival = esDemo4 ? demo4.fabricaRival(p.accion) : fabricaMuneco();
        Combatiente yo = new Combatiente(nombreDemo, montajeDemo.copia(), fYo);
        Combatiente muneco = new Combatiente(nombreMuneco, new Montaje(modMuneco), fRival);
        pelea = new Pelea(yo, muneco, 42L + indicePaso * 7L, 1000, 700, 1200);
        pelea.arena = Arena.de(demo.escenario.name(), pelea.ancho, pelea.alto);
        pelea.rondas = 1;
        pelea.empezarRonda();
        posicionarParaPaso(p);
    }

    private SoporteDemo4.Enlace enlaceDemo4() {
        return new SoporteDemo4.Enlace() {
            @Override
            public void marcarLinea(int principal, int... secundarias) {
                marcarLineaViva(principal, secundarias);
            }

            @Override
            public void anotarDeteccion() {
                enemigoDetectado = true;
            }

            @Override
            public void anotarDisparo() {
                disparoRealizado = true;
            }
        };
    }

    private void posicionarParaPaso(Demostracion.Paso p) {
        Cuerpo yo = pelea.cuerpo(0);
        Cuerpo rival = pelea.cuerpo(1);
        if (yo == null || rival == null) return;
        if (Demostracion.ID_DEMO_3.equals(demo.id)) {
            posicionarDemo3(p, yo, rival);
            return;
        }
        if (Demostracion.ID_DEMO_4.equals(demo.id)) {
            demo4.preparar(pelea, p.accion);
            inicioX = yo.x;
            inicioY = yo.y;
            inicioRumbo = yo.rumbo;
            inicioMira = yo.mira;
            blindajeRivalInicial = rival.blindaje();
            return;
        }
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
            case FALLO_ORDEN:
            case ACIERTO_ORDEN:
                yo.mira = -34;
                break;
            case ORDEN_TURNO:
                yo.mira = 22;
                break;
            case CORREGIR_LINEA:
                yo.mira = -28;
                break;
            default:
                yo.mira = 0;
                break;
        }
        boolean cerca = p.accion == Demostracion.Accion.APUNTAR_Y_DISPARAR
                || p.accion == Demostracion.Accion.FALLO_ORDEN
                || p.accion == Demostracion.Accion.ORDEN_TURNO
                || p.accion == Demostracion.Accion.CORREGIR_LINEA
                || p.accion == Demostracion.Accion.ACIERTO_ORDEN;
        rival.x = cerca ? 560 : 595;
        rival.y = 360;
        rival.rumbo = 180;
        rival.mira = 180;
        inicioX = yo.x;
        inicioY = yo.y;
        inicioRumbo = yo.rumbo;
        inicioMira = yo.mira;
        blindajeRivalInicial = rival.blindaje();
    }

    private void posicionarDemo3(Demostracion.Paso p, Cuerpo yo, Cuerpo rival) {
        yo.x = 110;
        yo.y = 490;
        yo.rumbo = 0;
        boolean vivo = p.accion == Demostracion.Accion.ENTORNO_INGENUO
                || p.accion == Demostracion.Accion.ENTORNO_AJUSTADO;
        yo.mira = vivo ? -22 : 0;
        rival.x = 350;
        rival.y = 490;
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
        for (Suceso s : pelea.sucesos()) {
            if (s.tipo == Suceso.Tipo.OBSTACULO_ROTO) obstaculoDestruido = true;
        }
        if (Demostracion.ID_DEMO_4.equals(demo.id)) {
            demo4.actualizarMetricas(pelea, pasoActual().accion);
        }
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
                return yo != null && (distanciaRecorrida >= 79.5 || (yo.restanteAvance() < 0.5 && distanciaRecorrida > 40));
            case GIRAR:
                return yo != null && Math.abs(yo.restanteGiro()) < 0.5 && giroAcumulado > 35;
            case RADAR:
                return giroMiraAcumulado >= 75;
            case DETECTAR:
                return enemigoDetectado && turnosTrasDeteccion >= 2;
            case APUNTAR_Y_DISPARAR:
            case ACIERTO_ORDEN:
                return enemigoDetectado && disparoRealizado && ((impactoRealizado && turnosTrasImpacto >= 4) || turnosTrasDisparo >= 20);
            case FALLO_ORDEN:
                return enemigoDetectado && disparoRealizado && turnosTrasDisparo >= 10;
            case ENTORNO_INGENUO:
                return enemigoDetectado && disparoRealizado && turnosTrasDisparo >= 14;
            case ENTORNO_AJUSTADO:
                return obstaculoDestruido && enemigoDetectado && disparoRealizado
                        && ((impactoRealizado && turnosTrasImpacto >= 4) || turnosTrasDisparo >= 60);
            case POO_EXPUESTO:
            case POO_ENCAPSULADO:
            case APARATO_ERROR:
            case APARATO_BIEN:
            case GESTION_ENERGIA:
            case TOPE_DEUDA:
            case TOPE_OPTIMO:
                return demo4.debePausar(p.accion, impactoRealizado);
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
                case FALLO_ORDEN:
                    if (enemigosVistos().isEmpty()) {
                        avanzar(40);
                    } else {
                        detener();
                    }
                    apuntar(25);
                    marcarLineaViva(15, 6, 7);
                    break;
                case ACIERTO_ORDEN:
                    if (enemigosVistos().isEmpty()) {
                        avanzar(40);
                        apuntar(25);
                        marcarLineaViva(14, 12, 13);
                    } else {
                        detener();
                    }
                    break;
                case ENTORNO_INGENUO:
                    ejecutarEntornoIngenuo();
                    break;
                case ENTORNO_AJUSTADO:
                    ejecutarEntornoAjustado();
                    break;
                case QUIETO:
                case ORDEN_TURNO:
                case CORREGIR_LINEA:
                case AJUSTAR_ENTORNO:
                case CIERRE:
                default:
                    detener();
                    break;
            }
        }

        private void ejecutarEntornoIngenuo() {
            avanzar(60);
            if (enemigosVistos().isEmpty()) {
                apuntar(20);
                marcarLineaViva(14, 12, 13);
            } else {
                marcarLineaViva(7, 6, 12);
            }
        }

        private void ejecutarEntornoAjustado() {
            for (Obstaculo o : obstaculos()) {
                if (o.peligroso() && o.distancia() < 150) {
                    girarHacia(o.angulo() + 90);
                    avanzar(60);
                    marcarLineaViva(9, 7, 8, 10);
                    return;
                }
                if (o.solido() && o.distancia() < 180) {
                    detener();
                    apuntarHacia(o.angulo());
                    if (Math.abs(normalizar(o.angulo() - mira())) < 8) {
                        if (atacar()) disparoRealizado = true;
                        marcarLineaViva(16, 13, 14, 15);
                    } else {
                        marcarLineaViva(14, 13, 15);
                    }
                    return;
                }
            }
            List<Enemigo> vistos = enemigosVistos();
            if (vistos.isEmpty()) {
                apuntar(20);
                marcarLineaViva(23, 21, 22);
                return;
            }
            Enemigo e = vistos.get(0);
            enemigoDetectado = true;
            double alcance = Math.max(alcanceIzquierda(), alcanceDerecha());
            apuntarHacia(e.angulo());
            double error = Math.abs(normalizar(e.angulo() - mira()));
            if (e.distancia() <= alcance && error < 8) {
                if (atacar()) disparoRealizado = true;
                marcarLineaViva(31, 21, 27, 29, 30);
            } else {
                marcarLineaViva(28, 21, 27, 29, 30);
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
            if (a == Demostracion.Accion.FALLO_ORDEN) {
                enemigoDetectado = true;
                apuntarHacia(e.angulo());
                if (!disparoRealizado && atacar()) {
                    disparoRealizado = true;
                }
                marcarLineaViva(15, 6, 7);
                return;
            }
            if (a == Demostracion.Accion.ENTORNO_INGENUO) {
                enemigoDetectado = true;
                apuntarHacia(e.angulo());
                if (atacar()) disparoRealizado = true;
                marcarLineaViva(7, 6, 12);
                return;
            }
            if (a != Demostracion.Accion.APUNTAR_Y_DISPARAR && a != Demostracion.Accion.ACIERTO_ORDEN) return;
            enemigoDetectado = true;
            apuntarHacia(e.angulo());
            double error = Math.abs(normalizar(e.angulo() - mira()));
            int lApuntar = a == Demostracion.Accion.ACIERTO_ORDEN ? 6 : 15;
            int lAtacar = a == Demostracion.Accion.ACIERTO_ORDEN ? 7 : 16;
            int lBloque = a == Demostracion.Accion.ACIERTO_ORDEN ? 5 : 14;
            if (!disparoRealizado && error < 1.0) {
                if (atacar()) disparoRealizado = true;
                marcarLineaViva(lAtacar, lApuntar, lBloque);
            } else if (!disparoRealizado) {
                marcarLineaViva(lApuntar, lBloque, lAtacar);
            } else {
                marcarLineaViva(lAtacar, lApuntar, lBloque);
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

    /** Turnos reales de simulación ejecutados dentro del paso actual. */
    public int turnosEnPaso() {
        return turnosEnPaso;
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

    /** Indica si en el paso actual se destruyó al menos un obstáculo destructible de la arena. */
    public boolean obstaculoDestruido() {
        return obstaculoDestruido;
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

    /** Código fuente que corresponde al paso actual en el idioma activo. */
    public String codigoActual() {
        return pasoActual().codigo(demo, idioma);
    }

    /** Color semántico del resaltado de la línea activa (▶) en el paso actual. */
    public Demostracion.Resaltado resaltadoActual() {
        return pasoActual().resaltado;
    }

    /** Salta directamente a un paso concreto (útil para capturas o pruebas). */
    public void irAPaso(int indice) {
        indicePaso = Math.max(0, Math.min(demo.pasos.size() - 1, indice));
        terminada = false;
        iniciarPaso();
    }

    /** Indica si las barras de blindaje y energía del HUD están listas para mostrarse en la demostración. */
    public boolean hudBarrasVisible() {
        return pelea != null && pelea.cuerpo(0) != null;
    }

    /** Indica si el paso actual llama explícitamente la atención sobre las barras de blindaje y energía. */
    public boolean resaltarHudBarras() {
        Demostracion.Accion a = pasoActual().accion;
        return a == Demostracion.Accion.APUNTAR_Y_DISPARAR
                || a == Demostracion.Accion.ACIERTO_ORDEN
                || a == Demostracion.Accion.ENTORNO_AJUSTADO
                || a == Demostracion.Accion.GESTION_ENERGIA;
    }

    /** Indica si el robot recogió un aparato en el paso actual de Demo 4. */
    public boolean aparatoRecogido() {
        return demo4.aparatoRecogido;
    }

    /** Indica si se registró un intento fallido de conexión en un hueco erróneo. */
    public boolean conexionFallida() {
        return demo4.conexionFallida;
    }

    /** Indica si el aparato quedó conectado en su hueco válido. */
    public boolean aparatoConectado() {
        return demo4.aparatoConectado;
    }

    /** Indica si el aparato conectado llegó a activarse en combate. */
    public boolean aparatoUsado() {
        return demo4.aparatoUsado;
    }

    /** Energía mínima registrada durante el paso actual de Demo 4. */
    public double energiaMinima() {
        return demo4.energiaMinima;
    }

    /** Indica si la energía volvió a subir tras gastar energía en el paso actual. */
    public boolean energiaRecargada() {
        return demo4.energiaRecargada;
    }

    /** Indica si se disparó un corte real por {@code TopeDeCalculo} en el paso actual. */
    public boolean topeAlcanzado() {
        return demo4.topeAlcanzado;
    }

    /** Indica si el robot entró en deuda de turnos tras superar el tope de cálculo. */
    public boolean deudaObservada() {
        return demo4.deudaObservada;
    }

    /** Daño total recibido por el robot protagonista durante el paso actual. */
    public double danoRecibidoDemo() {
        return demo4.danoRecibidoDemo;
    }
}
