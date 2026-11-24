package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import metajavbots.api.Cosa;
import metajavbots.api.Metajavbot;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Arena;
import metajavbots.combate.CarreraDeObstaculos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.Fabrica;
import metajavbots.combate.MinijuegoDestruccion;
import metajavbots.combate.Pelea;
import metajavbots.combate.RobotAveriadoDestruccion;
import metajavbots.combate.Suceso;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.pantallas.MiniJuegos;
import metajavbots.pantallas.ModoEspecial;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.ConsejosDelJuego;

/** Pruebas automatizadas del minijuego especial de destrucción y supervivencia 3×. */
final class PruebasDeMinijuegoDestruccion {

    private PruebasDeMinijuegoDestruccion() {
    }

    /** Robot pasivo para pruebas controladas. */
    public static class Quieto extends Metajavbot {
        @Override
        public void cadaTurno() {
            // No hace nada.
        }
    }

    /** Robot que dispara con el brazo izquierdo cada turno. */
    public static class Tirador extends Metajavbot {
        @Override
        public void cadaTurno() {
            atacarIzquierda();
        }
    }

    /** Robot que avanza recto para probar sesgo espacial de obstáculos en su trayectoria. */
    public static class CorredorRecto extends Metajavbot {
        @Override
        public void cadaTurno() {
            avanzar(40);
        }
    }

    /** Robot que cambia de rumbo continuamente en zigzag para probar movimiento imprevisible. */
    public static class ZigzagEvasivo extends Metajavbot {
        private int paso;

        @Override
        public void cadaTurno() {
            paso++;
            if (x() < 260 || x() > anchoArena() - 260 || y() < 160 || y() > altoArena() - 160) {
                girarHacia(Math.toDegrees(Math.atan2(altoArena() / 2.0 - y(), anchoArena() / 2.0 - x())));
            } else if ((paso / 5) % 2 == 0) {
                girar(24);
            } else {
                girar(-24);
            }
            avanzar(45);
        }
    }

    static void apuntar() {
        apuntarArena3xYCuentaAtras();
        apuntarAparicionesCieloYSuelo();
        apuntarPrevisibilidadCaidasCielo();
        apuntarDanoDobleQueCarrera();
        apuntarLetalidadRealYRobotsAgresivos();
        apuntarRecompensasExactasEnCentesimas();
        apuntarDificultadModelosYPiezasEnMenu();
        apuntarPlantillasUiYEntrenador();
    }

    private static Combatiente combatienteConBala(String nombre, Fabrica fabrica) {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
            if (p.arma() == Pieza.Arma.BALA && p.valor("dano") > 0) {
                m.poner(Montaje.Hueco.IZQUIERDA, p);
                break;
            }
        }
        return new Combatiente(nombre, m, fabrica);
    }

    private static void apuntarArena3xYCuentaAtras() {
        caso("minijuego destrucción: arena exactamente 3x y cuenta atrás funcional hasta 0", () -> {
            igual(5, MiniJuegos.retos().length, "los 5 minijuegos básicos siguen intactos");
            igual(MinijuegoDestruccion.ANCHO_BASE_ESTANDAR * 3.0, MinijuegoDestruccion.ANCHO, "ancho exactamente 3x la arena estándar");
            igual(MinijuegoDestruccion.ALTO_BASE_ESTANDAR, MinijuegoDestruccion.ALTO, "alto igual a la arena estándar");
            igual(MinijuegoDestruccion.ANCHO_BASE_ESTANDAR * MinijuegoDestruccion.ALTO_BASE_ESTANDAR * 3.0,
                    MinijuegoDestruccion.ANCHO * MinijuegoDestruccion.ALTO, "superficie exactamente 3x la arena estándar");

            Combatiente jug = new Combatiente("Superviviente", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            MinijuegoDestruccion reto = new MinijuegoDestruccion(jug, 123L, null, DificultadCarrera.NOOB, 1, 90);
            reto.pelea.resistencia[0] = 0.0;
            igual(MinijuegoDestruccion.ANCHO, reto.pelea.ancho, "pelea usa ancho 3x");
            igual(MinijuegoDestruccion.ALTO, reto.pelea.alto, "pelea usa alto estándar");
            igual(90, reto.turnosRestantes(), "cuenta atrás empieza en 90 turnos");

            for (int t = 0; t < 90 && !reto.terminada(); t++) {
                // Mantener blindaje lleno para probar el fin limpio de la cuenta atrás.
                Cuerpo c0 = reto.pelea.cuerpo(0);
                for (int k = 0; k < 4; k++) c0.parte[k] = c0.parteMax[k];
                reto.avanzar();
            }
            cierto(reto.terminada(), "termina al llegar la cuenta atrás a 0");
            cierto(reto.sobrevivio(), "marca supervivencia al llegar a 0 con vida");
            igual(0, reto.turnosRestantes(), "cuenta atrás llega exactamente a 0");
            igual("supervivencia", reto.motivoFin(), "motivo de fin es supervivencia");
        });
    }

    private static void apuntarAparicionesCieloYSuelo() {
        caso("minijuego destrucción: apariciones en trayectoria, caídas con sombra previa (~0,8 s) y detección", () -> {
            // La sombra de caída desde el cielo dura ~0,6–0,9 s (18–27 turnos a 30 turnos/s).
            cierto(MinijuegoDestruccion.TURNOS_AVISO_CIELO >= 18 && MinijuegoDestruccion.TURNOS_AVISO_CIELO <= 27,
                    "aviso previo de caída en 0,6–0,9 s: " + MinijuegoDestruccion.TURNOS_AVISO_CIELO);

            // Sesgo espacial del suelo delante de la trayectoria de un robot que avanza en rumbo 0.
            Combatiente corredor = new Combatiente("Corredor", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(CorredorRecto::new));
            MinijuegoDestruccion retoTray = new MinijuegoDestruccion(corredor, 456L, null, DificultadCarrera.NOOB, 1, 600);
            Cuerpo cTray = retoTray.pelea.cuerpo(0);
            cTray.x = 600;
            int avisosSuelo = 0;
            int delanteTrayectoria = 0;
            for (int t = 0; t < 260 && !retoTray.terminada(); t++) {
                for (int k = 0; k < 4; k++) cTray.parte[k] = cTray.parteMax[k];
                int antes = retoTray.avisosActivos().size();
                retoTray.avanzar();
                if (retoTray.avisosActivos().size() > antes) {
                    for (MinijuegoDestruccion.AvisoAparicion a : retoTray.avisosActivos()) {
                        if (!a.desdeCielo && a.turnosRestantes() == a.turnosTotales - 1) {
                            avisosSuelo++;
                            if (a.x > cTray.x && Math.abs(a.y - cTray.y) <= 130) delanteTrayectoria++;
                        }
                    }
                }
            }
            cierto(avisosSuelo >= 4, "brotaron suficientes avisos de suelo: " + avisosSuelo);
            cierto(delanteTrayectoria >= avisosSuelo / 2,
                    "sesgo espacial delante de la trayectoria (" + delanteTrayectoria + "/" + avisosSuelo + ")");

            // Un jugador detecta obstáculos del suelo y robots averiados caídos del cielo.
            Combatiente jug = new Combatiente("Observador", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            MinijuegoDestruccion reto = new MinijuegoDestruccion(jug, 456L, null, DificultadCarrera.JUNIOR, 2, 480);
            Cuerpo c0 = reto.pelea.cuerpo(0);
            igual(0, reto.totalAparicionesSuelo(), "sin apariciones de suelo en turno 0");
            igual(0, reto.totalAparicionesCielo(), "sin robots caídos en turno 0");
            for (int t = 0; t < 260 && !reto.terminada(); t++) {
                for (int k = 0; k < 4; k++) c0.parte[k] = c0.parteMax[k];
                c0.aturdido = 0;
                reto.avanzar();
            }
            cierto(reto.totalAparicionesSuelo() >= 1, "brotaron objetos/trampas del suelo: " + reto.totalAparicionesSuelo());
            cierto(reto.totalAparicionesCielo() >= 1, "cayeron robots averiados del cielo: " + reto.totalAparicionesCielo());

            // Detectables en obstaculos() y enemigosVistos().
            List<? extends Cosa> obs = c0.obstaculos();
            cierto(!obs.isEmpty(), "los objetos del suelo aparecen al instante en obstaculos()");
            List<Cuerpo> robots = reto.robotsActivos();
            cierto(!robots.isEmpty(), "hay robots averiados activos tras caer del cielo");
            Cuerpo primero = robots.get(0);
            cierto(primero.combatiente.defectuoso, "el robot caído del cielo lleva perfil defectuoso");
            c0.x = primero.x - 90;
            c0.y = primero.y;
            c0.mira = 0;
            reto.avanzar();
            cierto(!c0.vistos().isEmpty(), "el robot caído del cielo es detectable en enemigosVistos()");
        });
    }

    private static void apuntarPrevisibilidadCaidasCielo() {
        caso("minijuego destrucción: caídas cerca por defecto; directas muy raras solo si es previsible", () -> {
            // Movimiento imprevisible (cambia de rumbo continuamente sin atascarse): 0 caídas directas, todas cerca.
            Combatiente evasivo = new Combatiente("Evasivo", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(ZigzagEvasivo::new));
            MinijuegoDestruccion retoImp = new MinijuegoDestruccion(evasivo, 707L, null, DificultadCarrera.JUNIOR, 2, 720);
            retoImp.pelea.resistencia[0] = 0.0;
            Cuerpo cImp = retoImp.pelea.cuerpo(0);
            int avisosCieloImp = 0;
            int avisosCercaImp = 0;
            for (int t = 0; t < 650 && !retoImp.terminada(); t++) {
                for (int k = 0; k < 4; k++) cImp.parte[k] = cImp.parteMax[k];
                cImp.aturdido = 0;
                double prevX = cImp.x;
                double prevY = cImp.y;
                retoImp.avanzar();
                if (Math.hypot(cImp.x - prevX, cImp.y - prevY) < 2.0) {
                    cImp.rumbo = Pelea.normal(cImp.rumbo + 75.0);
                    cImp.x = Math.max(220, Math.min(MinijuegoDestruccion.ANCHO - 220, cImp.x + Math.cos(Math.toRadians(cImp.rumbo)) * 18.0));
                    cImp.y = Math.max(150, Math.min(MinijuegoDestruccion.ALTO - 150, cImp.y + Math.sin(Math.toRadians(cImp.rumbo)) * 18.0));
                }
                for (MinijuegoDestruccion.AvisoAparicion a : retoImp.avisosActivos()) {
                    if (a.desdeCielo && a.turnosRestantes() == a.turnosTotales - 1) {
                        avisosCieloImp++;
                        double dist = Math.hypot(a.x - cImp.x, a.y - cImp.y);
                        double radioImpacto = cImp.radio + a.radio + MinijuegoDestruccion.MARGEN_IMPACTO_CAIDA;
                        cierto(!a.directa, "sin caídas directas cuando el movimiento es imprevisible");
                        if (dist >= radioImpacto && dist <= MinijuegoDestruccion.DISTANCIA_MAX_CAIDA_CERCA + 100.0) {
                            avisosCercaImp++;
                        }
                    }
                }
            }
            falso(retoImp.jugadorPrevisible(), "el jugador en zigzag no se clasifica como previsible");
            cierto(retoImp.totalAparicionesCielo() >= 6, "cayeron suficientes robots en modo imprevisible: " + retoImp.totalAparicionesCielo());
            igual(0, retoImp.totalCaidasDirectas(), "0 caídas directas si el jugador varía el rumbo");
            igual(retoImp.totalAparicionesCielo(), retoImp.totalCaidasCerca(), "todas las caídas aterrizan cerca si es imprevisible");
            igual(avisosCieloImp, avisosCercaImp, "todos los avisos de caída aterrizan cerca fuera del impacto directo instantáneo");

            // Movimiento previsible (quieto): mayoría cerca, solo muy pocas caídas directas y con aviso previo de sombra.
            Combatiente quieto = new Combatiente("QuietoPrev", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            MinijuegoDestruccion retoPrev = new MinijuegoDestruccion(quieto, 456L, null, DificultadCarrera.SENIOR, 2, 1400);
            retoPrev.pelea.resistencia[0] = 0.0;
            Cuerpo cPrev = retoPrev.pelea.cuerpo(0);
            int impactosQuieto = 0;
            for (int t = 0; t < 1300 && !retoPrev.terminada(); t++) {
                for (int k = 0; k < 4; k++) cPrev.parte[k] = cPrev.parteMax[k];
                cPrev.aturdido = 0;
                retoPrev.avanzar();
                for (Suceso s : retoPrev.pelea.sucesos()) {
                    if (s.tipo == Suceso.Tipo.EXPLOSION && "caida_robot".equals(s.texto)) impactosQuieto++;
                }
            }
            cierto(retoPrev.jugadorPrevisible(), "un jugador quieto se clasifica como previsible");
            cierto(retoPrev.totalAparicionesCielo() >= 10, "cayeron suficientes robots en modo previsible: " + retoPrev.totalAparicionesCielo());
            cierto(retoPrev.totalCaidasDirectas() >= 1, "ocurre al menos una caída directa si es previsible: " + retoPrev.totalCaidasDirectas());
            cierto(retoPrev.totalCaidasCerca() > retoPrev.totalCaidasDirectas(),
                    "la mayoría de caídas siguen cayendo cerca (" + retoPrev.totalCaidasCerca() + " vs " + retoPrev.totalCaidasDirectas() + ")");
            cierto(retoPrev.totalCaidasDirectas() * 3 <= retoPrev.totalAparicionesCielo(),
                    "las caídas directas son una minoría muy baja (" + retoPrev.totalCaidasDirectas() + "/" + retoPrev.totalAparicionesCielo() + ")");
            cierto(impactosQuieto >= 1, "un jugador quieto recibe impacto si no se aparta de la caída directa");

            // Apartarse durante la sombra evita el impacto incluso en una caída directa.
            Combatiente esquiva = new Combatiente("EsquivaPrev", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            MinijuegoDestruccion retoEsq = new MinijuegoDestruccion(esquiva, 456L, null, DificultadCarrera.SENIOR, 2, 1400);
            retoEsq.pelea.resistencia[0] = 0.0;
            Cuerpo cEsq = retoEsq.pelea.cuerpo(0);
            int impactosEsquivando = 0;
            for (int t = 0; t < 1300 && !retoEsq.terminada(); t++) {
                for (int k = 0; k < 4; k++) cEsq.parte[k] = cEsq.parteMax[k];
                for (MinijuegoDestruccion.AvisoAparicion a : retoEsq.avisosActivos()) {
                    if (a.desdeCielo && Math.hypot(a.x - cEsq.x, a.y - cEsq.y) < cEsq.radio + a.radio + 45) {
                        cEsq.x = Math.min(MinijuegoDestruccion.ANCHO - 120, a.x + 170);
                    }
                }
                retoEsq.avanzar();
                for (Suceso s : retoEsq.pelea.sucesos()) {
                    if (s.tipo == Suceso.Tipo.EXPLOSION && "caida_robot".equals(s.texto)) impactosEsquivando++;
                }
            }
            igual(0, impactosEsquivando, "apartarse durante la sombra evita el impacto de caída directa");
        });
    }

    private static void apuntarDanoDobleQueCarrera() {
        caso("destrucción y supervivencia: reducción exacta −20 % (×0,80) en daño y progresión Noob→Especial", () -> {
            cierto(ModoEspecial.CLAVES_PESTANAS.contains("especial.destruccion"),
                    "ModoEspecial incluye la pestaña especial.destruccion");
            falso(ModoEspecial.CLAVES_PESTANAS.contains("especial.carrera"),
                    "ModoEspecial ya NO incluye especial.carrera");

            // Pelea estándar sigue en 1.0.
            Combatiente aStd = new Combatiente("A", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Combatiente bStd = new Combatiente("B", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Pelea pStd = new Pelea(aStd, bStd, 90L);
            pStd.empezarRonda();
            igual(1.0, pStd.multiplicadorDano, "Pelea estándar conserva multiplicador 1.0");

            double prevMultDif = 0.0;
            for (DificultadCarrera dif : DificultadCarrera.values()) {
                double multSub1 = MinijuegoDestruccion.multiplicadorDanoPara(dif, 1);
                cierto(multSub1 > prevMultDif, "multiplicador crece con dificultad en " + dif + ": " + multSub1 + " > " + prevMultDif);
                prevMultDif = multSub1;
                for (int sub = DificultadCarrera.SUBNIVEL_MIN; sub <= DificultadCarrera.SUBNIVEL_MAX; sub++) {
                    Combatiente jCar = new Combatiente("Car", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                    Combatiente rCar = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                    CarreraDeObstaculos carrera = new CarreraDeObstaculos(jCar, rCar, 77L, false, null, dif, sub);
                    double multCarrera = carrera.pelea.multiplicadorDano;

                    Combatiente jMini = new Combatiente("Mini", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                    MinijuegoDestruccion mini = new MinijuegoDestruccion(jMini, 77L, null, dif, sub);
                    double multMini = mini.pelea.multiplicadorDano;
                    double multPrevioEndurecimiento = multCarrera * (2.60 + (sub - 1) * 0.12);
                    double esperadoTrasReduccion = multPrevioEndurecimiento * 0.80;

                    cierto(Math.abs(multMini - esperadoTrasReduccion) < 1e-9,
                            dif + " sub " + sub + ": mult destrucción (" + multMini + ") == 0.80× previo (" + esperadoTrasReduccion + ")");
                    cierto(multMini >= multCarrera * 2.0 - 1e-9,
                            dif + " sub " + sub + ": mult destrucción (" + multMini + ") >= 2.0x carrera (" + (multCarrera * 2.0) + ")");

                    // Mismo golpe equivalente sin saturar el blindaje de una pieza (trampa y disparo de robot defectuoso).
                    carrera.pelea.golpeEspecial(0, 0, 3.0, "trampa");
                    mini.pelea.golpeEspecial(0, 0, 3.0, "trampa");
                    double esperadoTrampa = jCar.danoTotalRecibido * (2.60 + (sub - 1) * 0.12) * 0.80;
                    cierto(Math.abs(jMini.danoTotalRecibido - esperadoTrampa) < 1e-4,
                            dif + " sub " + sub + ": daño trampa (" + jMini.danoTotalRecibido + ") == −20 % del previo (" + esperadoTrampa + ")");

                    double antesCar = jCar.danoTotalRecibido;
                    double antesMini = jMini.danoTotalRecibido;
                    carrera.pelea.golpeEspecial(2, 0, 3.0, "bala");
                    mini.pelea.golpeEspecial(1, 0, 3.0, "bala");
                    double dDefCar = jCar.danoTotalRecibido - antesCar;
                    double dDefMini = jMini.danoTotalRecibido - antesMini;
                    cierto(dDefMini >= dDefCar * 2.0 - 1e-6,
                            dif + " sub " + sub + ": daño defectuoso (" + dDefMini + ") >= 2.0x carrera (" + (dDefCar * 2.0) + ")");
                }
            }
        });
    }

    private static void apuntarLetalidadRealYRobotsAgresivos() {
        caso("destrucción y supervivencia: daño ajustado −20 % (3-4 golpes >= 40-50 % en Noob) y robots agresivos", () -> {
            // En Noob I sobre Cobalto (~391 blindaje), 4 golpes de bala defectuosa quitan ~47 % (tras −20 %).
            Combatiente jNoob = new Combatiente("CobaltoNoob", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            MinijuegoDestruccion miniNoob = new MinijuegoDestruccion(jNoob, 88L, null, DificultadCarrera.NOOB, 1);
            Cuerpo cNoob = miniNoob.pelea.cuerpo(0);
            double blindMax = cNoob.blindajeMaximoTotal();
            for (int k = 0; k < 4 && !cNoob.fuera; k++) {
                miniNoob.pelea.golpeEspecial(1, 0, 5.0, "bala");
            }
            double perdidoBalas = (1.0 - cNoob.blindajeQueQueda() / blindMax) * 100.0;
            cierto(cNoob.fuera || (perdidoBalas >= 40.0 && perdidoBalas < 55.0),
                    "4 balas en Noob I quitan 40-55 % de blindaje tras −20 % (perdido=" + perdidoBalas + "%)");

            // Impacto de caída de robot: 3 golpes de caída quitan ~45 % en Noob I (tras −20 %).
            Combatiente jCaida = new Combatiente("CobaltoCaida", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            MinijuegoDestruccion miniCaida = new MinijuegoDestruccion(jCaida, 89L, null, DificultadCarrera.NOOB, 1);
            Cuerpo cCaida = miniCaida.pelea.cuerpo(0);
            for (int k = 0; k < 3 && !cCaida.fuera; k++) {
                miniCaida.pelea.golpeEspecial(0, 0, MinijuegoDestruccion.DANO_BASE_CAIDA_NORMAL, "caida_robot");
            }
            double perdidoCaida = (1.0 - cCaida.blindajeQueQueda() / cCaida.blindajeMaximoTotal()) * 100.0;
            cierto(cCaida.fuera || (perdidoCaida >= 40.0 && perdidoCaida < 55.0),
                    "3 impactos de caída en Noob I quitan 40-55 % tras −20 % (perdido=" + perdidoCaida + "%)");

            // Escalado creciente de daño efectivo en Noob < Junior < Senior < Especial.
            double prevMult = 0;
            for (DificultadCarrera dif : DificultadCarrera.values()) {
                double m = MinijuegoDestruccion.multiplicadorDanoPara(dif, 1) * MinijuegoDestruccion.escalaDanoDefectuosoPara(dif, 1);
                cierto(m > prevMult, "daño crece en " + dif + ": " + m + " > " + prevMult);
                prevMult = m;
            }

            // Robot averiado agresivo: al detectar al jugador se acerca y le acierta disparos reales.
            Combatiente blanco = new Combatiente("Blanco", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Combatiente agresivo = combatienteConBala("AveriadoAgresivo",
                    PruebasDeLaPelea.de(() -> new RobotAveriadoDestruccion(false, 12, 10.0)));
            agresivo.defectuoso = true;
            agresivo.escalaDano = MinijuegoDestruccion.escalaDanoDefectuosoPara(DificultadCarrera.NOOB, 1);
            Pelea p = new Pelea(blanco, agresivo, 321L);
            p.multiplicadorDano = MinijuegoDestruccion.multiplicadorDanoPara(DificultadCarrera.NOOB, 1);
            p.empezarRonda();
            Cuerpo cb = p.cuerpo(0);
            Cuerpo ca = p.cuerpo(1);
            cb.x = 300; cb.y = 350;
            ca.x = 680; ca.y = 350; ca.rumbo = 180; ca.mira = 180;
            double distInicial = Math.hypot(ca.x - cb.x, ca.y - cb.y);
            double distMin = distInicial;
            for (int t = 0; t < 180 && !cb.fuera; t++) {
                p.avanzar();
                distMin = Math.min(distMin, Math.hypot(ca.x - cb.x, ca.y - cb.y));
            }
            cierto(distMin < distInicial - 80.0,
                    "robot averiado agresivo se acerca hacia el jugador (distInicial=" + distInicial + ", distMin=" + distMin + ")");
            cierto(blanco.danoTotalRecibido > 80.0,
                    "robot averiado agresivo dispara hacia el jugador y le inflige daño real: " + blanco.danoTotalRecibido);
        });
    }

    private static void apuntarRecompensasExactasEnCentesimas() {
        caso("minijuego destrucción: 0,1-0,6 por baja, +0,5 al sobrevivir, 1/4 al morir, 0 al rendirse", () -> {
            verificarRangoCentesimasObjetivos();
            Path anterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("mjb-destruccion");
            try {
                Guardado.carpeta = temp;
                verificarRecompensaSupervivencia();
                verificarRecompensaMuerteYRendicion();
            } finally {
                Guardado.carpeta = anterior;
            }
        });
    }

    private static void verificarRangoCentesimasObjetivos() {
        for (DificultadCarrera dif : DificultadCarrera.values()) {
            for (int sub = DificultadCarrera.SUBNIVEL_MIN; sub <= DificultadCarrera.SUBNIVEL_MAX; sub++) {
                for (String tipo : new String[]{"caja", "barril", "chimenea", "robot"}) {
                    for (boolean pesado : new boolean[]{false, true}) {
                        int cent = MinijuegoDestruccion.recompensaPorObjetivo(tipo, pesado, dif, sub);
                        cierto(cent >= 10 && cent <= 60,
                                "recompensa en [10, 60] para " + tipo + "/" + pesado + "/" + dif + "/" + sub + ": " + cent);
                    }
                }
            }
        }
    }

    private static void verificarRecompensaSupervivencia() throws Exception {
        Combatiente tirador = combatienteConBala("Tirador", PruebasDeLaPelea.de(Tirador::new));
        MinijuegoDestruccion ganado = new MinijuegoDestruccion(tirador, 501L, null, DificultadCarrera.NOOB, 1, 260);
        Cuerpo c0 = ganado.pelea.cuerpo(0);
        for (int t = 0; t < 260 && !ganado.terminada(); t++) {
            for (int k = 0; k < 4; k++) c0.parte[k] = c0.parteMax[k];
            Arena.Trampa destr = primerDestructible(ganado);
            if (destr != null) {
                c0.x = destr.x - 110;
                c0.y = destr.y;
                c0.mira = 0;
            }
            ganado.avanzar();
        }
        cierto(ganado.terminada() && ganado.sobrevivio(), "sobrevive al final de la cuenta atrás");
        cierto(ganado.objetosDestruidos() >= 1, "destruyó al menos un objeto: " + ganado.objetosDestruidos());
        int acumulado = ganado.botinAcumuladoCentimos();
        cierto(acumulado >= 10, "acumuló botín en centésimas: " + acumulado);

        Jugador j1 = new Jugador("Ganador");
        j1.monedas = 1.0;
        cierto(ganado.aplicarRecompensa(j1), "aplica recompensa de supervivencia");
        falso(ganado.aplicarRecompensa(j1), "recompensa es idempotente");
        igual(acumulado + 50, ganado.centimosGanadosTotal(), "supervivencia = 100 % acumulado + 50 centésimas");
        igual(50, ganado.bonoSupervivenciaCentimos(), "bono de supervivencia exacto de +50 centésimas");
        Guardado.guardar(j1);
        Jugador j1Rec = Guardado.leer(Guardado.carpetaDe(j1.nombre));
        igual(j1.monedas, j1Rec.monedas, "monedas enteras persisten en Guardado");
        igual(j1.centimosDeMoneda, j1Rec.centimosDeMoneda, "centésimas persisten en Guardado");
    }

    private static void verificarRecompensaMuerteYRendicion() {
        Combatiente tirador2 = combatienteConBala("Tirador2", PruebasDeLaPelea.de(Tirador::new));
        MinijuegoDestruccion muerto = new MinijuegoDestruccion(tirador2, 502L, null, DificultadCarrera.NOOB, 1, 600);
        Cuerpo m0 = muerto.pelea.cuerpo(0);
        for (int t = 0; t < 240 && muerto.botinAcumuladoCentimos() == 0 && !muerto.terminada(); t++) {
            for (int k = 0; k < 4; k++) m0.parte[k] = m0.parteMax[k];
            Arena.Trampa destr = primerDestructible(muerto);
            if (destr != null) {
                m0.x = destr.x - 110;
                m0.y = destr.y;
                m0.mira = 0;
            }
            muerto.avanzar();
        }
        cierto(muerto.botinAcumuladoCentimos() > 0, "acumuló botín antes de morir");
        int acumMuerto = muerto.botinAcumuladoCentimos();
        m0.fuera = true;
        muerto.avanzar();
        cierto(muerto.terminada() && !muerto.sobrevivio(), "termina por muerte antes de la cuenta atrás");
        Jugador j2 = new Jugador("Caido");
        muerto.aplicarRecompensa(j2);
        igual(acumMuerto / 4, muerto.centimosGanadosTotal(), "muerte anticipada cobra exactamente 1/4 del botín");
        cierto(muerto.bonosReducidosPorDerrota(), "marca botín reducido por derrota");

        Combatiente quieto = new Combatiente("Quieto", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
        MinijuegoDestruccion cero = new MinijuegoDestruccion(quieto, 503L, null, DificultadCarrera.NOOB, 1, 600);
        cero.pelea.cuerpo(0).fuera = true;
        cero.avanzar();
        Jugador j3 = new Jugador("SinBotin");
        falso(cero.aplicarRecompensa(j3), "muerte sin destrucciones no suma nada");
        igual(0, cero.centimosGanadosTotal(), "muerte sin destrucciones = 0");

        Combatiente rend = new Combatiente("Rend", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
        MinijuegoDestruccion rendido = new MinijuegoDestruccion(rend, 504L, null, DificultadCarrera.NOOB, 1, 600);
        rendido.rendirse();
        Jugador j4 = new Jugador("Rendido");
        falso(rendido.aplicarRecompensa(j4), "rendirse no otorga recompensas");
        igual(0, rendido.centimosGanadosTotal(), "rendirse = 0");
    }

    private static Arena.Trampa primerDestructible(MinijuegoDestruccion juego) {
        for (Arena.Trampa t : juego.pelea.arena.trampas()) {
            if (t.destructible && !t.destruida()) return t;
        }
        return null;
    }

    private static void apuntarDificultadModelosYPiezasEnMenu() {
        caso("minijuego destrucción: dificultad por rango, subniveles I-III, modelos desbloqueados y piezas en menú", () -> {
            Jugador noob = new Jugador("Noob");
            noob.robots.add(new Jugador.Robot("Alfa", "cobalto"));
            PantallaMinijuegoDestruccion pantalla = new PantallaMinijuegoDestruccion();
            cierto(PantallaMinijuegoDestruccion.dificultadDesbloqueada(noob, DificultadCarrera.NOOB), "NOOB desbloqueado");
            falso(PantallaMinijuegoDestruccion.dificultadDesbloqueada(noob, DificultadCarrera.JUNIOR), "JUNIOR bloqueado en Noob");
            falso(pantalla.seleccionarDificultad(noob, DificultadCarrera.JUNIOR), "no permite elegir JUNIOR en Noob");
            igual(DificultadCarrera.NOOB, pantalla.dificultadSeleccionada(), "mantiene NOOB");

            Jugador senior = new Jugador("Senior");
            senior.etiqueta = Etiqueta.SENIOR_1;
            senior.jefesVencidos.add("gim_condicionales");
            senior.robots.add(new Jugador.Robot("Beta", "cobalto"));
            for (Modelo m : Modelo.todos()) senior.modelosGanados.add(m.id);
            cierto(pantalla.seleccionarDificultad(senior, DificultadCarrera.ESPECIAL), "ESPECIAL desbloqueado con jefe");
            igual(DificultadCarrera.ESPECIAL, pantalla.dificultadSeleccionada(), "ESPECIAL seleccionado");
            pantalla.seleccionarSubnivel(3);
            igual(3, pantalla.subnivelSeleccionado(), "subnivel III seleccionado");

            // Cambio de modelo desbloqueado y piezas aplicando Montaje.Cifras reales.
            List<Modelo> modelos = PantallaMinijuegoDestruccion.modelosDesbloqueados(senior);
            cierto(modelos.size() >= 2, "hay varios modelos desbloqueados");
            String antes = pantalla.modeloSeleccionado(senior);
            cierto(pantalla.cambiarModelo(senior, 1), "cambia de modelo directamente en el menú");
            falso(antes.equals(pantalla.modeloSeleccionado(senior)), "el modelo equipado cambió");
            Montaje.Cifras cifras = pantalla.cifrasActuales(senior);
            igual(senior.montajeDe(senior.robots.get(0)).cifras().velocidad, cifras.velocidad, "cifras coinciden con Montaje.Cifras reales");

            for (Pieza pz : CatalogoDePiezas.delJuego().de(Pieza.Ranura.PIERNAS)) {
                senior.piezasGanadas.add(pz.id);
            }
            List<Pieza> piernas = PantallaMinijuegoDestruccion.piezasDisponiblesParaHueco(senior, senior.robots.get(0), Montaje.Hueco.PIERNAS);
            cierto(!piernas.isEmpty(), "lista piezas disponibles para PIERNAS");
            if (piernas.size() > 1) {
                String idAntes = senior.montajeDe(senior.robots.get(0)).pieza(Montaje.Hueco.PIERNAS).id;
                cierto(pantalla.cambiarPieza(senior, Montaje.Hueco.PIERNAS, 1), "cambia pieza de PIERNAS en el menú");
                falso(idAntes.equals(senior.montajeDe(senior.robots.get(0)).pieza(Montaje.Hueco.PIERNAS).id), "pieza actualizada");
            }
        });
    }

    private static void apuntarPlantillasUiYEntrenador() {
        caso("minijuego destrucción: plantillas ES/DE/EN sin fugas, ui.txt completo y conocimiento del entrenador", () -> {
            if (Compilador.hayCompilador()) {
                for (Idioma id : Idioma.values()) {
                    String codigo = MinijuegoDestruccion.plantillaInicial(id);
                    RobotDelJugador r = new RobotDelJugador();
                    Compilador.Resultado res = r.compilar(codigo, id.locale(), false);
                    cierto(res.bien, "plantilla compila en " + id + ": " + res.errores);
                    if (id == Idioma.DE) {
                        cierto(codigo.contains("MetajavbotDe") && codigo.contains("hindernisse()") && codigo.contains("gesehen()"), "DE usa API alemana");
                        falso(codigo.contains("cadaTurno") || codigo.contains("onTick") || codigo.contains("obstaculos()"), "DE sin fugas");
                    } else if (id == Idioma.EN) {
                        cierto(codigo.contains("MetajavbotEn") && codigo.contains("obstacles()") && codigo.contains("seenEnemies()"), "EN usa API inglesa");
                        falso(codigo.contains("cadaTurno") || codigo.contains("jedenZug") || codigo.contains("obstaculos()"), "EN sin fugas");
                    } else {
                        cierto(codigo.contains("Metajavbot") && codigo.contains("obstaculos()") && codigo.contains("enemigosVistos()"), "ES usa API española");
                        falso(codigo.contains("jedenZug") || codigo.contains("onTick") || codigo.contains("obstacles()"), "ES sin fugas");
                    }
                }
            }

            igual("Destrucción y supervivencia", T.en("especial.destruccion", Idioma.ES), "nombre ES pestaña ModoEspecial");
            igual("Zerstörung und Überleben", T.en("especial.destruccion", Idioma.DE), "nombre DE pestaña ModoEspecial");
            igual("Destruction and survival", T.en("especial.destruccion", Idioma.EN), "nombre EN pestaña ModoEspecial");

            String[] claves = {
                    "especial.destruccion", "especial.destruccion_ayuda", "especial.destruccion_entrar",
                    "destruccion.titulo", "destruccion.panel_titulo", "destruccion.arena_info",
                    "destruccion.nueva_arena", "destruccion.regla_arena", "destruccion.regla_apariciones",
                    "destruccion.regla_dano", "destruccion.regla_dano_corto", "destruccion.regla_recompensas",
                    "destruccion.regla_derrota", "destruccion.iniciar", "destruccion.hud_tiempo", "destruccion.hud_blindaje",
                    "destruccion.hud_botin", "destruccion.victoria_titulo", "destruccion.derrota_titulo",
                    "destruccion.bono_supervivencia", "destruccion.derrota_cuarto", "destruccion.saldo_final"
            };
            for (Idioma id : Idioma.values()) {
                for (String k : claves) {
                    String val = T.en(k, id);
                    cierto(val != null && !val.isBlank() && !val.equals(k), "clave ui.txt presente en " + id + ": " + k);
                    falso(val.contains("+25 %") || val.contains("+25%") || val.contains("2,6×") || val.contains("2.6×"),
                            "sin cifras desfasadas en " + k + " (" + id + ")");
                }
            }

            ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("minijuego_destruccion");
            cierto(entrada != null, "existe ficha minijuego_destruccion en ConocimientoDelEntrenador");
            cierto(!entrada.consejos().isEmpty(), "existe consejo espontáneo para minijuego_destruccion");
            String[][] consultas = {
                    {"destrucción y supervivencia", "ES", "modo especial", "2,08×", "40 %", "0,5", "1/4"},
                    {"Zerstörung und Überleben", "DE", "spezialmodus", "2,08×", "40", "0,5", "1/4"},
                    {"destruction and survival", "EN", "special mode", "2.08×", "40", "0.5", "1/4"}
            };
            for (String[] q : consultas) {
                Idioma id = Idioma.valueOf(q[1]);
                List<Concepto> res = Concepto.buscar(q[0], id, 3);
                cierto(!res.isEmpty() && res.get(0).id.equals("minijuego_destruccion"),
                        "búsqueda offline encuentra minijuego_destruccion en " + id);
                String texto = String.join(" ", res.get(0).parrafos(id)).toLowerCase(Locale.ROOT);
                cierto(texto.contains(q[2]) && texto.contains(q[3]) && texto.contains(q[4])
                        && texto.contains(q[5]) && texto.contains(q[6]),
                        "ficha del entrenador actualizada (Modo especial y daño >= 2.08x) en " + id);
                String tip = ConsejosDelJuego.textoDelTip(entrada.consejos().get(0).aTip(), id);
                cierto(tip != null && !tip.isBlank() && tip.toLowerCase(Locale.ROOT).contains(q[2]),
                        "consejo espontáneo apunta a Modo especial en " + id);
                String sistemaIa = ConsultaIaEntrenador.construirSistema(new Jugador("Test"), id, null).toLowerCase(Locale.ROOT);
                cierto(sistemaIa.contains("destruction & survival") && sistemaIa.contains("special mode")
                        && sistemaIa.contains("3x") && sistemaIa.contains("2.08x")
                        && sistemaIa.contains("40") && sistemaIa.contains("+0.5") && sistemaIa.contains("1/4"),
                        "sistema IA actualizado con Destrucción y supervivencia en Modo especial en " + id);
            }
        });
    }
}
