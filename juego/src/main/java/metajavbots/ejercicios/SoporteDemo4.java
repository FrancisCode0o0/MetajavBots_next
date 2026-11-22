package metajavbots.ejercicios;

import java.util.List;

import metajavbots.api.Aparato;
import metajavbots.api.Enemigo;
import metajavbots.api.Metajavbot;
import metajavbots.api.Tope;
import metajavbots.combate.Aparatos;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;

/**
 * SOPORTE DEMO 4 — Ejecución real en arena para Demo 4 (modificadores POO
 * inspeccionados por reflexión, aparatos con hueco erróneo y válido, consumo
 * y recarga de energía por turno, y corte por {@code TopeDeCalculo} con deuda
 * de turnos frente a la versión optimizada).
 */
final class SoporteDemo4 {

    /** Callback para sincronizar las líneas resaltadas y banderas del motor. */
    interface Enlace {
        void marcarLinea(int principal, int... secundarias);

        void anotarDeteccion();

        void anotarDisparo();
    }

    boolean aparatoRecogido;
    boolean conexionFallida;
    boolean aparatoConectado;
    boolean aparatoUsado;
    double energiaInicial;
    double energiaMinima;
    boolean energiaRecargada;
    boolean topeAlcanzado;
    boolean deudaObservada;
    double danoRecibidoDemo;
    private boolean esperandoRecargaEnergia;
    private int turnosRecargaEnergia;
    private int turnosTrasAparato;
    private boolean disparoRivalHecho;
    private boolean preparandoAparato;
    private Enlace enlaceActual;

    void reiniciar() {
        aparatoRecogido = false;
        conexionFallida = false;
        aparatoConectado = false;
        aparatoUsado = false;
        energiaInicial = 0;
        energiaMinima = 0;
        energiaRecargada = false;
        topeAlcanzado = false;
        deudaObservada = false;
        danoRecibidoDemo = 0;
        esperandoRecargaEnergia = false;
        turnosRecargaEnergia = 0;
        turnosTrasAparato = 0;
        disparoRivalHecho = false;
        preparandoAparato = false;
    }

    /** Fábrica de robot con la clase Java real para que {@code ModificadoresPoo} analice sus modificadores. */
    Fabrica fabricaJugador(Demostracion.Accion accion, Enlace enlace) {
        this.enlaceActual = enlace;
        if (accion == Demostracion.Accion.POO_EXPUESTO) {
            return RobotPooExpuesto::new;
        }
        if (accion == Demostracion.Accion.POO_ENCAPSULADO) {
            return RobotPooEncapsulado::new;
        }
        return () -> new RobotGeneralDemo4(accion, enlace);
    }

    /** Fábrica del rival: en los pasos POO dispara un tiro real para comparar el daño recibido. */
    Fabrica fabricaRival(Demostracion.Accion accion) {
        boolean dispara = accion == Demostracion.Accion.POO_EXPUESTO
                || accion == Demostracion.Accion.POO_ENCAPSULADO;
        return () -> new RivalDemo4(dispara);
    }

    /** Configura posiciones, aparatos y condiciones iniciales en la Pelea real. */
    void preparar(Pelea pelea, Demostracion.Accion accion) {
        boolean conAparato = accion == Demostracion.Accion.APARATO_ERROR
                || accion == Demostracion.Accion.APARATO_BIEN;
        if (conAparato) {
            pelea.aparatos.activos = true;
            pelea.aparatos.fijarSiguiente(Aparatos.Tipo.CHORRO);
            preparandoAparato = true;
            for (int t = 0; t < Aparatos.SALE - 6; t++) {
                pelea.avanzar();
            }
            preparandoAparato = false;
        }
        Cuerpo yo = pelea.cuerpo(0);
        Cuerpo rival = pelea.cuerpo(1);
        if (yo == null || rival == null) return;
        yo.x = 280;
        yo.y = 350;
        yo.rumbo = 0;
        yo.mira = accion == Demostracion.Accion.TOPE_OPTIMO ? -18 : 0;
        boolean pasoPoo = accion == Demostracion.Accion.POO_EXPUESTO
                || accion == Demostracion.Accion.POO_ENCAPSULADO;
        rival.x = pasoPoo ? 460 : 560;
        rival.y = 350;
        rival.rumbo = 180;
        rival.mira = 180;
        energiaInicial = yo.energia;
        energiaMinima = yo.energia;
    }

    /** Actualiza métricas observables tras avanzar un turno real de la pelea. */
    void actualizarMetricas(Pelea pelea, Demostracion.Accion accion) {
        Cuerpo yo = pelea.cuerpo(0);
        if (yo == null) return;
        danoRecibidoDemo = pelea.combatientes[0].danoTotalRecibido;
        if (yo.energia < energiaMinima) {
            energiaMinima = yo.energia;
        }
        if (energiaMinima < energiaInicial - 0.5 && yo.energia > energiaMinima + 0.5) {
            energiaRecargada = true;
        }
        if (pelea.informe(0).topes > 0) {
            topeAlcanzado = true;
        }
        if (yo.deuda > 0 || yo.estadoPensamiento == Cuerpo.EstadoPensamiento.DEUDA_TOPE) {
            deudaObservada = true;
        }
        Aparatos.Uno a = pelea.aparatos.delCuerpo(0);
        if (a != null) {
            aparatoRecogido = true;
            if (a.conectado) aparatoConectado = true;
            if (a.activo > 0 || a.recarga > 0) aparatoUsado = true;
        }
        for (Suceso s : pelea.sucesos()) {
            if (s.tipo == Suceso.Tipo.APARATO && s.quien == 0 && "hueco".equals(s.texto)) {
                conexionFallida = true;
            }
        }
        if ((accion == Demostracion.Accion.APARATO_ERROR && conexionFallida)
                || (accion == Demostracion.Accion.APARATO_BIEN && aparatoUsado)) {
            turnosTrasAparato++;
        }
    }

    /** Condición de pausa automática determinista para cada paso de Demo 4. */
    boolean debePausar(Demostracion.Accion accion, boolean impacto) {
        switch (accion) {
            case POO_EXPUESTO:
            case POO_ENCAPSULADO:
                return danoRecibidoDemo > 0.1;
            case APARATO_ERROR:
                return aparatoRecogido && conexionFallida && turnosTrasAparato >= 6;
            case APARATO_BIEN:
                return aparatoRecogido && aparatoConectado && aparatoUsado && turnosTrasAparato >= 12;
            case GESTION_ENERGIA:
                return energiaMinima < energiaInicial - 1.0 && energiaRecargada && turnosRecargaEnergia >= 10;
            case TOPE_DEUDA:
                return topeAlcanzado && deudaObservada;
            case TOPE_OPTIMO:
                return impacto;
            default:
                return false;
        }
    }

    // ================================================================ Robots reales para Demo 4

    /** Robot con 2 campos {@code public} mutables (penalizado por {@code ModificadoresPoo}). */
    final class RobotPooExpuesto extends Metajavbot {
        public int fase = 0;
        public double distanciaObjetivo = 120;

        @Override
        public void cadaTurno() {
            fase++;
            if (distanciaObjetivo > 0) {
                detener();
            }
            if (enlaceActual != null) enlaceActual.marcarLinea(4, 5, 8);
        }
    }

    /** Robot con {@code private}, {@code final} y {@code static} (bonificado por {@code ModificadoresPoo}). */
    final class RobotPooEncapsulado extends Metajavbot {
        private int fase = 0;
        private double ultimoAngulo = 0;
        private final int umbralGiro = 15;
        private final double distanciaSegura = 120;
        private static int senalEquipo = 0;

        private void actualizarEstado() {
            fase++;
            senalEquipo = fase;
        }

        @Override
        public void cadaTurno() {
            actualizarEstado();
            ultimoAngulo = mira();
            if (distanciaSegura > umbralGiro && ultimoAngulo <= 360) {
                detener();
            }
            if (enlaceActual != null) enlaceActual.marcarLinea(4, 6, 8, 10);
        }
    }

    /** Robot de los pasos de aparatos, energía y tope de cálculo. */
    final class RobotGeneralDemo4 extends Metajavbot {
        private final Demostracion.Accion accion;
        private final Enlace enlace;

        RobotGeneralDemo4(Demostracion.Accion accion, Enlace enlace) {
            this.accion = accion;
            this.enlace = enlace;
        }

        @Override
        public void cadaTurno() {
            if (preparandoAparato) {
                detener();
                return;
            }
            switch (accion) {
                case APARATO_ERROR:
                    ejecutarAparato(false);
                    break;
                case APARATO_BIEN:
                    ejecutarAparato(true);
                    break;
                case GESTION_ENERGIA:
                    ejecutarEnergia();
                    break;
                case TOPE_DEUDA:
                    ejecutarTopeLento();
                    break;
                case TOPE_OPTIMO:
                    ejecutarTopeOptimo();
                    break;
                default:
                    detener();
                    break;
            }
        }

        private void ejecutarAparato(boolean correcto) {
            Aparato mio = miAparato();
            if (mio != null) {
                detener();
                if (!mio.conectado()) {
                    if (correcto && "chorro".equals(mio.tipo())) {
                        conectar("espalda");
                        enlace.marcarLinea(10, 7, 9);
                    } else if (!correcto) {
                        conectar("cabeza");
                        enlace.marcarLinea(10, 7, 9);
                    }
                } else if (correcto && mio.listo()) {
                    if (usarAparato()) {
                        avanzar(90);
                    }
                    enlace.marcarLinea(12, 11, 10);
                }
                return;
            }
            List<Aparato> suelo = aparatos();
            if (!suelo.isEmpty()) {
                Aparato a = suelo.get(0);
                girarHacia(a.angulo());
                avanzar(Math.max(40, a.distancia()));
                enlace.marcarLinea(correcto ? 19 : 17, correcto ? 16 : 14, correcto ? 20 : 18);
            } else {
                detener();
            }
        }

        private void ejecutarEnergia() {
            List<Enemigo> vistos = enemigosVistos();
            if (vistos.isEmpty()) {
                apuntar(15);
                enlace.marcarLinea(11, 9, 10);
                return;
            }
            Enemigo e = vistos.get(0);
            enlace.anotarDeteccion();
            apuntarHacia(e.angulo());
            if (!esperandoRecargaEnergia) {
                boolean disparo = atacar();
                if (disparo) enlace.anotarDisparo();
                if (listoEspecial()) especial();
                if (energia() < energiaInicial - 1.0) {
                    esperandoRecargaEnergia = true;
                }
                enlace.marcarLinea(17, 16, 19);
            } else {
                turnosRecargaEnergia++;
                enlace.marcarLinea(16, 5);
            }
        }

        private void ejecutarTopeLento() {
            enlace.marcarLinea(8, 7, 9);
            double suma = 0;
            for (int i = 0; i < 200; i++) {
                Tope.paso();
                for (int j = 0; j < 200; j++) {
                    Tope.paso();
                    suma += (i + j) * 0.01;
                }
            }
            if (suma > 0 && !enemigosVistos().isEmpty()) {
                atacar();
            }
        }

        private void ejecutarTopeOptimo() {
            Tope.paso();
            List<Enemigo> vistos = enemigosVistos();
            if (vistos.isEmpty()) {
                apuntar(15);
                enlace.marcarLinea(11, 9, 10);
                return;
            }
            Enemigo e = vistos.get(0);
            enlace.anotarDeteccion();
            apuntarHacia(e.angulo());
            if (Math.abs(normalizar(e.angulo() - mira())) < 8.0) {
                if (atacar()) enlace.anotarDisparo();
                enlace.marcarLinea(17, 5, 15, 16);
            } else {
                enlace.marcarLinea(15, 9, 16);
            }
        }
    }

    /** Rival determinista: dispara un solo tiro limpio en los pasos POO y espera quieto en el resto. */
    final class RivalDemo4 extends Metajavbot {
        private final boolean dispararUnaVez;

        RivalDemo4(boolean dispararUnaVez) {
            this.dispararUnaVez = dispararUnaVez;
        }

        @Override
        public void cadaTurno() {
            detener();
            if (!dispararUnaVez || disparoRivalHecho || preparandoAparato) return;
            List<Enemigo> vistos = enemigosVistos();
            if (!vistos.isEmpty()) {
                apuntarHacia(vistos.get(0).angulo());
                if (Math.abs(normalizar(vistos.get(0).angulo() - mira())) < 4.0) {
                    boolean ok = esDistancia(armaIzquierda()) ? atacarIzquierda()
                            : esDistancia(armaDerecha()) ? atacarDerecha() : atacar();
                    if (ok) disparoRivalHecho = true;
                }
            }
        }

        private boolean esDistancia(String arma) {
            return "LASER".equals(arma) || "BALA".equals(arma) || "MISIL".equals(arma);
        }
    }
}
