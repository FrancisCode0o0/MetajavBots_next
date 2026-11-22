package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import metajavbots.api.Cosa;
import metajavbots.api.Metajavbot;
import metajavbots.api.MetajavbotDe;
import metajavbots.api.MetajavbotEn;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Arena;
import metajavbots.combate.CarreraDeObstaculos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.combate.PistaDeCarrera;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.pantallas.ModoEspecial;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.ConsejosDelJuego;

/** Pruebas automatizadas de la Carrera de obstáculos 6×, mecánicas en pista y conocimiento del entrenador. */
final class PruebasDeCarreraDeObstaculos {

    private PruebasDeCarreraDeObstaculos() {
    }

    /** Robot inmóvil para pruebas controladas de derrota y sensores. */
    public static class Quieto extends Metajavbot {
        @Override
        public void cadaTurno() {
            // No avanza.
        }
    }

    /** Robot que dispara hacia su mira cada turno. */
    public static class Tirador extends Metajavbot {
        @Override
        public void cadaTurno() {
            atacarIzquierda();
        }
    }

    /** Robot que avanza recto hacia la derecha para probar impulsos y avance. */
    public static class Recto extends Metajavbot {
        @Override
        public void cadaTurno() {
            girarHacia(0);
            avanzar(120);
        }
    }

    /** Robot en alemán que cuenta los obstáculos visibles en hindernisse(). */
    public static class ObservadorDe extends MetajavbotDe {
        int vistos = -1;

        @Override
        public void jedenZug() {
            vistos = hindernisse().size();
        }
    }

    /** Robot en inglés que cuenta los obstáculos visibles en obstacles(). */
    public static class ObservadorEn extends MetajavbotEn {
        int vistos = -1;

        @Override
        public void onTick() {
            vistos = obstacles().size();
        }
    }

    static void apuntar() {
        apuntarPista6xRotacionYSeleccionRobot();
        apuntarGeneracionYVisibilidad();
        apuntarRepentinosDefectuososEImpulsos();
        apuntarDestructiblesIndestructiblesYCombate();
        apuntarRecompensaMonedasYRivalAjeno();
        apuntarPlantillaTorpeTextosYEntrenador();
        apuntarNivelesDeDificultadYDesbloqueoEspecial();
        apuntarDanoEscaladoYRobotIngenuoDestruido();
        apuntarTrampaMortalYBonosDeReparacion();
        apuntarMedallasCuartosDeMonedaYPersistencia();
    }

    private static Combatiente combatienteConArma(String nombre, Pieza.Arma arma, Fabrica fabrica) {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
            if (p.arma() == arma && p.valor("dano") > 0) {
                m.poner(Montaje.Hueco.IZQUIERDA, p);
                break;
            }
        }
        return new Combatiente(nombre, m, fabrica);
    }

    private static boolean contiene(List<? extends Cosa> lista, Arena.Trampa trampa) {
        for (Cosa c : lista) {
            if (c.tipo().equals(trampa.tipo) && Math.hypot(c.x() - trampa.x, c.y() - trampa.y) < 1e-3) return true;
        }
        return false;
    }

    private static void apuntarPista6xRotacionYSeleccionRobot() {
        caso("carrera de obstáculos: pista >= 6x, rotación sin repetir arena y selección de robot del jugador", () -> {
            cierto(ModoEspecial.CLAVES_PESTANAS.contains("especial.carrera"), "ModoEspecial incluye pestaña especial.carrera");
            cierto(CarreraDeObstaculos.ANCHO >= CarreraDeObstaculos.ANCHO_BASE_REFERENCIA * 6.0,
                    "pista al menos 6x más grande que la base: " + CarreraDeObstaculos.ANCHO);

            // Rotación de arena: dos carreras consecutivas nunca repiten la misma arena.
            Combatiente cJug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            CarreraDeObstaculos primera = new CarreraDeObstaculos(cJug, CarreraDeObstaculos.rivalMaquina("IA"), 100L, true, null);
            CarreraDeObstaculos segunda = new CarreraDeObstaculos(cJug, CarreraDeObstaculos.rivalMaquina("IA"), 100L, true, primera.escenario());
            falso(primera.escenario().equals(segunda.escenario()),
                    "dos carreras consecutivas no repiten arena (" + primera.escenario() + " vs " + segunda.escenario() + ")");

            // Selección de robot del jugador desde Jugador.robots usando su montaje/cifras reales.
            Jugador jug = new Jugador("Piloto");
            Jugador.Robot r0 = new Jugador.Robot("Basico", "cobalto");
            Jugador.Robot r1 = new Jugador.Robot("Veloz", Modelo.todos().size() > 1 ? Modelo.todos().get(1).id : "cobalto");
            Pieza piernas = CatalogoDePiezas.delJuego().de(Pieza.Ranura.PIERNAS).get(0);
            r1.piezas.put(Montaje.Hueco.PIERNAS, piernas.id);
            jug.robots.add(r0);
            jug.robots.add(r1);

            Combatiente elegido = CarreraDeObstaculos.combatienteDeJugador(jug, 1, PruebasDeLaPelea.de(Quieto::new));
            igual("Veloz", elegido.nombre, "usa el robot seleccionado de Jugador.robots");
            igual(r1.modelo, elegido.montaje.modelo.id, "usa el modelo real del robot elegido");
            igual(piernas.id, elegido.montaje.pieza(Montaje.Hueco.PIERNAS).id, "usa las piezas montadas reales");
            CarreraDeObstaculos conElegido = new CarreraDeObstaculos(elegido, CarreraDeObstaculos.rivalMaquina("IA"), 200L, true);
            igual(jug.montajeDe(r1).cifras().velocidad, conElegido.pelea.cuerpo(0).cifras.velocidad, "aplica cifras reales del montaje");
        });
    }

    private static void apuntarGeneracionYVisibilidad() {
        caso("carrera de obstáculos: pista con visibles, ocultos detectables por radar, destructibles e indestructibles", () -> {
            Combatiente jug = new Combatiente("jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Combatiente riv = CarreraDeObstaculos.rivalMaquina("ia");
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(jug, riv, 42L, true);

            List<Arena.Trampa> visibles = carrera.obstaculosVisiblesEnMapa();
            List<Arena.Trampa> ocultos = carrera.obstaculosOcultosEnMapa();
            cierto(visibles.size() >= 8, "hay obstáculos visibles en el mapa general: " + visibles.size());
            cierto(ocultos.size() >= 4, "hay obstáculos ocultos en el mapa general: " + ocultos.size());

            int destructibles = 0, indestructibles = 0;
            for (Arena.Trampa t : carrera.pelea.arena.trampas()) {
                if (t.tipo.equals("impulso") || t.tipo.equals(Arena.TIPO_REPARACION)
                        || t.tipo.equals(Arena.TIPO_MONEDA_EXTRA) || t.tipo.equals(Arena.TIPO_RAMPA)) continue;
                if (t.destructible) destructibles++;
                else indestructibles++;
            }
            cierto(destructibles >= 6, "hay barriles/chimeneas destructibles");
            cierto(indestructibles >= 6, "hay obstáculos indestructibles");

            Cuerpo c0 = carrera.pelea.cuerpo(0);
            Arena.Trampa ocultoLejano = null;
            for (Arena.Trampa t : ocultos) {
                if (Math.hypot(t.x - c0.x, t.y - c0.y) > Arena.ALCANCE_SENSOR_OCULTOS + 120) {
                    ocultoLejano = t;
                    break;
                }
            }
            cierto(ocultoLejano != null, "existe al menos un obstáculo oculto lejos de la salida");
            falso(ocultoLejano.revelado(), "el obstáculo oculto empieza sin revelar");
            falso(contiene(c0.obstaculos(), ocultoLejano), "el obstáculo oculto lejano no aparece aún en obstaculos()");

            c0.x = ocultoLejano.x - 140;
            c0.y = ocultoLejano.y;
            cierto(contiene(c0.obstaculos(), ocultoLejano), "al acercarse entra en alcance de sensores y aparece en obstaculos()");
            cierto(ocultoLejano.revelado(), "al detectarse queda revelado en carrera");
        });
    }

    private static void apuntarRepentinosDefectuososEImpulsos() {
        caso("carrera de obstáculos: obstáculos repentinos, robots defectuosos destruibles e impulsos de velocidad", () -> {
            Combatiente tirador = combatienteConArma("tirador", Pieza.Arma.BALA, PruebasDeLaPelea.de(Tirador::new));
            Combatiente rivalQuieto = new Combatiente("quieto", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(tirador, rivalQuieto, 88L, false);

            // 1) Obstáculos repentinos: no activos en turno 0, aparecen al avanzar, nunca encima del robot.
            cierto(!carrera.obstaculosRepentinos().isEmpty(), "hay obstáculos repentinos planificados");
            for (PistaDeCarrera.ObstaculoRepentino rep : carrera.obstaculosRepentinos()) {
                falso(rep.aparecido(), "ningún obstáculo repentino está activo en turno 0");
            }
            PistaDeCarrera.ObstaculoRepentino primero = carrera.obstaculosRepentinos().get(0);
            Cuerpo c0 = carrera.pelea.cuerpo(0);
            c0.x = primero.activacionX + 10;
            c0.y = primero.yBase;
            carrera.avanzar();
            cierto(primero.aparecido() && primero.trampa() != null, "el obstáculo repentino aparece al llegar a su tramo");
            double distAlRobot = Math.hypot(primero.trampa().x - c0.x, primero.trampa().y - c0.y);
            cierto(distAlRobot >= c0.radio + primero.trampa().radio + 80,
                    "el obstáculo repentino nunca aparece encima del robot (dist=" + distAlRobot + ")");
            cierto(contiene(c0.obstaculos(), primero.trampa()), "el obstáculo repentino aparece al instante en obstaculos()");

            // 2) Robots defectuosos: existen, disparan/se mueven erráticamente y se destruyen a disparos.
            List<Cuerpo> defectuosos = carrera.robotsDefectuosos();
            cierto(defectuosos.size() >= 5, "hay robots defectuosos repartidos en la pista: " + defectuosos.size());
            Cuerpo roto = defectuosos.get(0);
            cierto(roto.combatiente.defectuoso, "marcado como defectuoso");
            double angAntes = roto.rumbo;
            boolean disparoDefectuoso = false;
            for (int t = 0; t < 45; t++) {
                carrera.avanzar();
                if (roto.ultimoAtaque >= 0) disparoDefectuoso = true;
            }
            cierto(disparoDefectuoso || Math.abs(roto.rumbo - angAntes) > 0.1, "el robot defectuoso se mueve o dispara erráticamente");

            for (int t = 0; t < 600 && !roto.fuera && !carrera.terminada(); t++) {
                c0.x = roto.x - 75;
                c0.y = roto.y;
                c0.mira = 0;
                carrera.avanzar();
            }
            cierto(roto.fuera, "el jugador puede destruir a disparos un robot defectuoso despejando el paso");

            // 3) Objetos recogibles de impulso: al recogerlo desaparece y aumenta temporalmente la velocidad.
            Combatiente corredor = new Combatiente("corredor", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Recto::new));
            CarreraDeObstaculos cImp = new CarreraDeObstaculos(corredor, rivalQuieto, 91L, false);
            List<Arena.Trampa> impulsos = cImp.impulsosActivos();
            cierto(impulsos.size() >= 4, "hay objetos de impulso repartidos por la pista");
            Arena.Trampa imp = impulsos.get(0);
            Cuerpo cj = cImp.pelea.cuerpo(0);
            double velNormal = cj.cifras.velocidad;
            cj.x = imp.x;
            cj.y = imp.y;
            cj.rumbo = 0;
            cImp.avanzar();
            cierto(imp.destruida(), "el impulso desaparece de la pista al recogerlo");
            cierto(cj.turbo > 0, "el robot recibe efecto temporal de turbo");
            for (int t = 0; t < 25; t++) cImp.avanzar();
            cierto(cj.velocidad > velNormal * 1.2, "la velocidad aumenta claramente por encima de la normal: " + cj.velocidad + " vs " + velNormal);
        });
    }

    private static void apuntarDestructiblesIndestructiblesYCombate() {
        caso("carrera de obstáculos: destructibles, indestructibles y combate contra el adversario", () -> {
            ObservadorDe[] obsDe = new ObservadorDe[1];
            Combatiente tirador = combatienteConArma("tirador", Pieza.Arma.BALA, PruebasDeLaPelea.de(Tirador::new));
            Combatiente rivalDe = new Combatiente("de", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(() -> {
                obsDe[0] = new ObservadorDe();
                return obsDe[0];
            }));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(tirador, rivalDe, 77L, false);

            Arena.Trampa barril = null, bloque = null;
            for (Arena.Trampa t : carrera.pelea.arena.trampas()) {
                if (barril == null && t.tipo.equals("barril") && !t.ocultoEnMapa) barril = t;
                if (bloque == null && t.tipo.equals("bloque") && !t.ocultoEnMapa) bloque = t;
            }
            cierto(barril != null && barril.destructible && barril.resistenciaMax == 30.0, "barril destructible con resistencia 30");
            cierto(bloque != null && !bloque.destructible && bloque.solido, "bloque sólido indestructible");

            Cuerpo c0 = carrera.pelea.cuerpo(0), c1 = carrera.pelea.cuerpo(1);
            c1.x = 120;
            c1.y = 660;
            cierto(contiene(c0.obstaculos(), barril), "el barril aparece inicialmente en obstaculos()");
            for (int t = 0; t < 200 && !barril.destruida(); t++) {
                c0.x = barril.x - 130;
                c0.y = barril.y;
                c0.mira = 0;
                carrera.avanzar();
            }
            cierto(barril.destruida(), "el barril se destruye con proyectiles");
            falso(contiene(c0.obstaculos(), barril), "el barril roto desaparece de obstaculos()");

            for (int t = 0; t < 80; t++) {
                c0.x = bloque.x - 130;
                c0.y = bloque.y;
                c0.mira = 0;
                carrera.avanzar();
            }
            falso(bloque.destruida(), "el bloque indestructible nunca se destruye");
            cierto(contiene(c0.obstaculos(), bloque), "el bloque indestructible sigue en obstaculos()");
            cierto(obsDe[0].vistos >= 1, "hindernisse() funciona en la pista");

            // Combate contra el adversario: el jugador puede dañar y derribar al adversario en carrera.
            double blindajeAntes = c1.blindaje();
            for (int t = 0; t < 160 && !carrera.terminada(); t++) {
                c0.x = c1.x - 130;
                c0.y = c1.y;
                c0.mira = 0;
                carrera.avanzar();
            }
            cierto(c1.blindaje() < blindajeAntes || c1.fuera, "el jugador puede dañar o derribar al adversario en carrera");

            ObservadorEn[] obsEn = new ObservadorEn[1];
            Combatiente rivalEn = new Combatiente("en", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(() -> {
                obsEn[0] = new ObservadorEn();
                return obsEn[0];
            }));
            CarreraDeObstaculos cEn = new CarreraDeObstaculos(tirador, rivalEn, 78L, false);
            cEn.avanzar();
            cierto(obsEn[0].vistos >= 1, "obstacles() funciona en la pista");
        });
    }

    private static void apuntarRecompensaMonedasYRivalAjeno() {
        caso("carrera de obstáculos: victoria suma exactamente 1 moneda, derrota 0 y rival ajeno bajo Vigilante", () -> {
            Path carpetaAnterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("mjb-carrera-monedas");
            try {
                Guardado.carpeta = temp;
                Jugador jug = new Jugador("PilotoPrueba");
                jug.monedas = 2.0;

                Combatiente cJug = new Combatiente("Piloto", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                CarreraDeObstaculos ganada = new CarreraDeObstaculos(cJug, CarreraDeObstaculos.rivalMaquina("IA"), 101L, true);
                ganada.pelea.cuerpo(0).x = ganada.metaX() + 5;
                ganada.avanzar();
                cierto(ganada.terminada(), "la carrera termina al cruzar la meta");
                igual(0, ganada.ganador(), "gana el jugador");
                cierto(ganada.aplicarRecompensa(jug), "aplica recompensa al ganar");
                falso(ganada.aplicarRecompensa(jug), "la recompensa es idempotente y no duplica monedas");
                igual(3.0, jug.monedas, "suma exactamente 1 moneda (2 -> 3)");
                Guardado.guardar(jug);
                Jugador recargado = Guardado.leer(Guardado.carpetaDe(jug.nombre));
                igual(3.0, recargado.monedas, "el saldo con la moneda ganada persiste tras guardar");

                CarreraDeObstaculos perdida = new CarreraDeObstaculos(cJug, CarreraDeObstaculos.rivalMaquina("IA"), 102L, true);
                perdida.pelea.cuerpo(1).x = perdida.metaX() + 5;
                perdida.avanzar();
                cierto(perdida.terminada(), "la carrera termina cuando gana la máquina");
                igual(1, perdida.ganador(), "gana la máquina");
                falso(perdida.aplicarRecompensa(jug), "no aplica recompensa al perder");
                igual(3.0, jug.monedas, "derrota no suma monedas");
            } finally {
                Guardado.carpeta = carpetaAnterior;
            }

            if (!Compilador.hayCompilador()) return;
            Path tempAjeno = Files.createTempDirectory("mjb-carrera-ajeno");
            Path valido = tempAjeno.resolve("RivalVeloz.java");
            String codigoValido = "// @metajavbot modelo=cobalto\n"
                    + "import metajavbots.api.*;\n"
                    + "public class RivalVeloz extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        girarHacia(0);\n"
                    + "        avanzar(90);\n"
                    + "    }\n"
                    + "}\n";
            Files.writeString(valido, codigoValido, StandardCharsets.UTF_8);

            PantallaCarreraDeObstaculos.CargaAjeno carga = PantallaCarreraDeObstaculos.cargarRivalAjeno(valido);
            cierto(carga.bien(), "el robot ajeno válido compila y carga bajo Vigilante/Cargador: " + carga.error());
            Combatiente cJug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(cJug, carga.combatiente(), 55L, false);
            for (int t = 0; t < 90 && !carrera.terminada(); t++) carrera.avanzar();
            cierto(carrera.progreso(1) > 0.02, "el robot ajeno avanza realmente en la pista 6x: " + carrera.progreso(1));

            Path prohibido = tempAjeno.resolve("RobotProhibido.java");
            String codigoProhibido = "import metajavbots.api.*;\n"
                    + "public class RobotProhibido extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        new java.io.File(\"x\").exists();\n"
                    + "    }\n"
                    + "}\n";
            Files.writeString(prohibido, codigoProhibido, StandardCharsets.UTF_8);
            falso(PantallaCarreraDeObstaculos.cargarRivalAjeno(prohibido).bien(), "Vigilante bloquea código ajeno con clases prohibidas");
        });
    }

    private static void apuntarPlantillaTorpeTextosYEntrenador() {
        caso("carrera de obstáculos: código base torpe ES/DE/EN no gana solo, ui.txt completo y entrenador actualizado", () -> {
            if (Compilador.hayCompilador()) {
                for (Idioma id : Idioma.values()) {
                    String codigo = CarreraDeObstaculos.plantillaInicial(id);
                    RobotDelJugador r = new RobotDelJugador();
                    Compilador.Resultado res = r.compilar(codigo, id.locale(), false);
                    cierto(res.bien, "plantilla torpe compila en " + id + ": " + res.errores);
                    if (id == Idioma.DE) {
                        cierto(codigo.contains("MetajavbotDe") && codigo.contains("hindernisse()"), "DE usa API alemana");
                        falso(codigo.contains("cadaTurno") || codigo.contains("onTick"), "DE sin fugas de ES/EN");
                    } else if (id == Idioma.EN) {
                        cierto(codigo.contains("MetajavbotEn") && codigo.contains("obstacles()"), "EN usa API inglesa");
                        falso(codigo.contains("cadaTurno") || codigo.contains("jedenZug"), "EN sin fugas de ES/DE");
                    } else {
                        cierto(codigo.contains("Metajavbot") && codigo.contains("obstaculos()"), "ES usa API española");
                        falso(codigo.contains("jedenZug") || codigo.contains("onTick"), "ES sin fugas de DE/EN");
                    }
                    // El código base torpe NO gana la carrera solo (se queda dando vueltas).
                    Combatiente torpe = new Combatiente("Torpe-" + id, new Montaje(Modelo.de("cobalto")), r);
                    CarreraDeObstaculos cTorpe = new CarreraDeObstaculos(torpe, CarreraDeObstaculos.rivalMaquina("IA"), 303L, true);
                    for (int t = 0; t < 450 && !cTorpe.terminada(); t++) cTorpe.avanzar();
                    falso(cTorpe.ganador() == 0, "código base torpe en " + id + " nunca gana la carrera solo");
                    cierto(cTorpe.progreso(0) < 0.05, "código base torpe en " + id + " no resuelve la pista: " + cTorpe.progreso(0));
                }
            }

            String[] claves = {
                    "especial.carrera", "especial.carrera_ayuda", "especial.carrera_entrar",
                    "carrera_obs.tu_robot", "carrera_obs.arena_actual",
                    "carrera_obs.dificultad", "carrera_obs.dif_noob_resumen", "carrera_obs.dif_junior_resumen",
                    "carrera_obs.dif_senior_resumen", "carrera_obs.dif_especial_resumen", "carrera_obs.dif_especial_bloqueada",
                    "carrera_obs.rival", "carrera_obs.vs_maquina", "carrera_obs.vs_usuario",
                    "carrera_obs.nueva_pista", "carrera_obs.sin_ajenos", "carrera_obs.mapa_titulo",
                    "carrera_obs.leyenda_destructible", "carrera_obs.leyenda_indestructible",
                    "carrera_obs.leyenda_mortal", "carrera_obs.leyenda_impulso",
                    "carrera_obs.leyenda_reparacion", "carrera_obs.leyenda_rotos", "carrera_obs.leyenda_ocultos",
                    "carrera_obs.iniciar", "carrera_obs.ganas", "carrera_obs.pierdes", "carrera_obs.empate",
                    "carrera_obs.premio_moneda", "carrera_obs.sin_moneda",
                    "carrera_obs.medalla_oro", "carrera_obs.medalla_plata",
                    "carrera_obs.medalla_bronce", "carrera_obs.medalla_ninguna",
                    "carrera_obs.dano_recibido", "carrera_obs.reconocimiento_perfecta",
                    "carrera_obs.cuarto_ganado", "carrera_obs.cuarto_completado"
            };
            for (Idioma id : Idioma.values()) {
                for (String k : claves) {
                    String val = T.en(k, id);
                    cierto(val != null && !val.isBlank() && !val.equals(k), "clave ui.txt presente en " + id + ": " + k);
                }
            }

            ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("carrera_de_obstaculos");
            cierto(entrada != null, "existe ficha carrera_de_obstaculos en ConocimientoDelEntrenador");
            cierto(!entrada.consejos().isEmpty(), "existe consejo espontáneo para carrera_de_obstaculos");
            String[][] consultas = {
                    {"carrera de obstáculos", "ES", "6×", "defectuosos", "impulso", "especial", "trampa_mortal", "reparacion", "1/4"},
                    {"Hindernislauf", "DE", "6×", "defekte", "impulso", "spezial", "trampa_mortal", "reparacion", "1/4"},
                    {"obstacle race", "EN", "6×", "broken", "impulso", "special", "trampa_mortal", "reparacion", "1/4"}
            };
            for (String[] q : consultas) {
                Idioma id = Idioma.valueOf(q[1]);
                List<Concepto> res = Concepto.buscar(q[0], id, 3);
                cierto(!res.isEmpty() && res.get(0).id.equals("carrera_de_obstaculos"),
                        "búsqueda offline encuentra carrera_de_obstaculos para «" + q[0] + "» (" + id + ")");
                String texto = String.join(" ", res.get(0).parrafos(id)).toLowerCase(Locale.ROOT);
                cierto(texto.contains(q[2].toLowerCase(Locale.ROOT)) && texto.contains(q[3])
                        && texto.contains(q[4]) && texto.contains(q[5])
                        && texto.contains(q[6]) && texto.contains(q[7]) && texto.contains(q[8])
                        && texto.contains("noob") && texto.contains("junior") && texto.contains("senior"),
                        "ficha del entrenador actualizada con daño, trampas mortales, reparación y 1/4 en " + id);
                String tip = ConsejosDelJuego.textoDelTip(entrada.consejos().get(0).aTip(), id);
                cierto(tip != null && !tip.isBlank() && tip.contains("trampa_mortal") && tip.contains("reparacion"),
                        "consejo espontáneo trilingüe actualizado en " + id);
                String sistemaIa = ConsultaIaEntrenador.construirSistema(new Jugador("Test"), id, null).toLowerCase(Locale.ROOT);
                cierto(sistemaIa.contains("obstacle race") && sistemaIa.contains("6x")
                        && sistemaIa.contains("impulso") && sistemaIa.contains("trampa_mortal")
                        && sistemaIa.contains("reparacion") && sistemaIa.contains("1/4")
                        && sistemaIa.contains("noob") && sistemaIa.contains("junior")
                        && sistemaIa.contains("senior") && sistemaIa.contains("special"),
                        "sistema IA actualizado con daño, trampas mortales, reparación y 1/4 en " + id);
            }
        });
    }

    private static int contarObstaculos(CarreraDeObstaculos carrera) {
        int n = 0;
        for (Arena.Trampa t : carrera.pelea.arena.trampas()) {
            if (!t.tipo.equals("impulso") && !t.tipo.equals(Arena.TIPO_REPARACION)) n++;
        }
        return n;
    }

    private static void apuntarNivelesDeDificultadYDesbloqueoEspecial() {
        caso("carrera de obstáculos: progresión estricta Noob < Junior < Senior < Especial y bloqueo/desbloqueo de Especial", () -> {
            igual("Noob", DificultadCarrera.NOOB.nombre(Idioma.ES), "nombre ES Noob");
            igual("Noob", DificultadCarrera.NOOB.nombre(Idioma.DE), "nombre DE Noob");
            igual("Noob", DificultadCarrera.NOOB.nombre(Idioma.EN), "nombre EN Noob");
            igual("Especial", DificultadCarrera.ESPECIAL.nombre(Idioma.ES), "nombre ES Especial");
            igual("Spezial", DificultadCarrera.ESPECIAL.nombre(Idioma.DE), "nombre DE Spezial");
            igual("Special", DificultadCarrera.ESPECIAL.nombre(Idioma.EN), "nombre EN Special");

            Combatiente jug = new Combatiente("Corredor", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Recto::new));
            DificultadCarrera[] niveles = DificultadCarrera.values();
            int[] totalObstaculos = new int[niveles.length];
            int[] totalRepentinos = new int[niveles.length];
            int[] totalDefectuosos = new int[niveles.length];
            int[] totalImpulsos = new int[niveles.length];

            for (int i = 0; i < niveles.length; i++) {
                DificultadCarrera d = niveles[i];
                CarreraDeObstaculos c = new CarreraDeObstaculos(jug, CarreraDeObstaculos.rivalMaquina("IA", d), 700L, true, null, d);
                totalObstaculos[i] = contarObstaculos(c);
                totalRepentinos[i] = c.obstaculosRepentinos().size();
                totalDefectuosos[i] = c.robotsDefectuosos().size();
                totalImpulsos[i] = c.impulsosActivos().size();
                for (int t = 0; t < 180 && !c.terminada(); t++) c.avanzar();
                cierto(c.progreso(0) > 0 || c.terminada(), "la carrera ejecuta sin bloqueos en " + d);
            }

            for (int i = 0; i < niveles.length - 1; i++) {
                DificultadCarrera a = niveles[i], b = niveles[i + 1];
                cierto(totalObstaculos[i] < totalObstaculos[i + 1],
                        "más obstáculos en " + b + " (" + totalObstaculos[i + 1] + ") que en " + a + " (" + totalObstaculos[i] + ")");
                cierto(totalRepentinos[i] < totalRepentinos[i + 1],
                        "más repentinos en " + b + " (" + totalRepentinos[i + 1] + ") que en " + a + " (" + totalRepentinos[i] + ")");
                cierto(totalDefectuosos[i] < totalDefectuosos[i + 1],
                        "más defectuosos en " + b + " (" + totalDefectuosos[i + 1] + ") que en " + a + " (" + totalDefectuosos[i] + ")");
                cierto(totalImpulsos[i] > totalImpulsos[i + 1],
                        "menos impulsos en " + b + " (" + totalImpulsos[i + 1] + ") que en " + a + " (" + totalImpulsos[i] + ")");
                cierto(a.cadenciaDisparoDefectuosos > b.cadenciaDisparoDefectuosos, "mayor cadencia de defectuosos en " + b);
                cierto(a.nivelRival < b.nivelRival && a.errorPunteriaRival > b.errorPunteriaRival,
                        "rival más táctico y preciso en " + b);
            }

            // Desbloqueo progresivo por rango (Noob -> Junior -> Senior -> Especial).
            Jugador nuevo = new Jugador("Novato");
            falso(Progreso.modoEspecialDesbloqueado(nuevo), "jugador nuevo no tiene Modo especial desbloqueado");
            cierto(PantallaCarreraDeObstaculos.dificultadDesbloqueada(nuevo, DificultadCarrera.NOOB), "NOOB disponible desde el inicio");
            falso(PantallaCarreraDeObstaculos.dificultadDesbloqueada(nuevo, DificultadCarrera.JUNIOR), "JUNIOR bloqueado en rango Noob");
            falso(PantallaCarreraDeObstaculos.dificultadDesbloqueada(nuevo, DificultadCarrera.SENIOR), "SENIOR bloqueado en rango Noob");
            falso(PantallaCarreraDeObstaculos.dificultadDesbloqueada(nuevo, DificultadCarrera.ESPECIAL), "ESPECIAL bloqueado inicialmente");

            PantallaCarreraDeObstaculos pantalla = new PantallaCarreraDeObstaculos();
            falso(pantalla.seleccionarDificultad(nuevo, DificultadCarrera.JUNIOR), "no permite forzar JUNIOR en rango Noob");
            falso(pantalla.seleccionarDificultad(nuevo, DificultadCarrera.ESPECIAL), "no permite forzar ESPECIAL si está bloqueado");
            igual(DificultadCarrera.NOOB, pantalla.dificultadSeleccionada(), "mantiene NOOB tras intentos bloqueados");

            Jugador junior = new Jugador("Intermedio");
            junior.etiqueta = Etiqueta.JUNIOR_1;
            cierto(PantallaCarreraDeObstaculos.dificultadDesbloqueada(junior, DificultadCarrera.NOOB), "NOOB disponible en Junior");
            cierto(PantallaCarreraDeObstaculos.dificultadDesbloqueada(junior, DificultadCarrera.JUNIOR), "JUNIOR desbloqueado en rango Junior");
            falso(PantallaCarreraDeObstaculos.dificultadDesbloqueada(junior, DificultadCarrera.SENIOR), "SENIOR bloqueado en rango Junior");
            falso(PantallaCarreraDeObstaculos.dificultadDesbloqueada(junior, DificultadCarrera.ESPECIAL), "ESPECIAL bloqueado sin Modo especial");
            cierto(pantalla.seleccionarDificultad(junior, DificultadCarrera.JUNIOR), "permite elegir JUNIOR en rango Junior");
            igual(DificultadCarrera.JUNIOR, pantalla.dificultadSeleccionada(), "JUNIOR queda seleccionado");

            Jugador senior = new Jugador("Veterano");
            senior.etiqueta = Etiqueta.SENIOR_1;
            cierto(PantallaCarreraDeObstaculos.dificultadDesbloqueada(senior, DificultadCarrera.SENIOR), "SENIOR desbloqueado en rango Senior");
            falso(PantallaCarreraDeObstaculos.dificultadDesbloqueada(senior, DificultadCarrera.ESPECIAL), "ESPECIAL sigue bloqueado sin jefe/Master");
            cierto(pantalla.seleccionarDificultad(senior, DificultadCarrera.SENIOR), "permite elegir SENIOR en rango Senior");
            igual(DificultadCarrera.SENIOR, pantalla.dificultadSeleccionada(), "SENIOR queda seleccionado");

            // Al desbloquear Modo especial (por jefe vencido o rango Master), ESPECIAL queda seleccionable.
            Jugador conJefe = new Jugador("Cazajefes");
            conJefe.jefesVencidos.add("gim_condicionales");
            cierto(Progreso.modoEspecialDesbloqueado(conJefe), "vencer jefe desbloquea Modo especial");
            cierto(PantallaCarreraDeObstaculos.dificultadDesbloqueada(conJefe, DificultadCarrera.ESPECIAL), "ESPECIAL desbloqueado por jefe");
            cierto(pantalla.seleccionarDificultad(conJefe, DificultadCarrera.ESPECIAL), "permite seleccionar ESPECIAL al estar desbloqueado");
            igual(DificultadCarrera.ESPECIAL, pantalla.dificultadSeleccionada(), "ESPECIAL seleccionado");

            Jugador master = new Jugador("Maestro");
            master.etiqueta = Etiqueta.MASTER;
            cierto(Progreso.modoEspecialDesbloqueado(master), "rango Master desbloquea Modo especial");
            cierto(PantallaCarreraDeObstaculos.dificultadDesbloqueada(master, DificultadCarrera.ESPECIAL), "ESPECIAL desbloqueado en Master");
        });
    }

    private static void apuntarDanoEscaladoYRobotIngenuoDestruido() {
        caso("carrera de obstáculos: daño multiplicado por dificultad y robot ingenuo en línea recta destruido", () -> {
            DificultadCarrera[] niveles = DificultadCarrera.values();
            cierto(DificultadCarrera.NOOB.multiplicadorDano > 1.0, "NOOB multiplica el daño sobre combate estándar");
            for (int i = 0; i < niveles.length - 1; i++) {
                cierto(niveles[i].multiplicadorDano < niveles[i + 1].multiplicadorDano,
                        "multiplicador crece: " + niveles[i] + " < " + niveles[i + 1]);
            }

            // Mismo golpe en Pelea estándar vs Carrera en cada dificultad.
            Combatiente aStd = new Combatiente("A", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Combatiente bStd = new Combatiente("B", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Pelea peleaStd = new Pelea(aStd, bStd, 900L);
            peleaStd.empezarRonda();
            igual(1.0, peleaStd.multiplicadorDano, "Pelea estándar conserva multiplicador 1.0");
            peleaStd.golpeEspecial(1, 0, 10.0, "prueba");
            double danoStd = aStd.danoTotalRecibido;

            double danoAnterior = danoStd;
            for (DificultadCarrera d : niveles) {
                Combatiente cj = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                Combatiente cr = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                CarreraDeObstaculos c = new CarreraDeObstaculos(cj, cr, 900L, false, null, d);
                igual(d.multiplicadorDano, c.pelea.multiplicadorDano, "carrera aplica multiplicador de " + d);
                c.pelea.golpeEspecial(1, 0, 10.0, "prueba");
                double danoActual = cj.danoTotalRecibido;
                cierto(danoActual > danoAnterior + 1e-6,
                        "daño en " + d + " (" + danoActual + ") supera al tramo anterior (" + danoAnterior + ")");
                danoAnterior = danoActual;
            }

            // Robot ingenuo que solo avanza en línea recta queda destruido antes de la meta en todos los niveles.
            for (DificultadCarrera d : niveles) {
                Combatiente ingenuo = new Combatiente("Recto-" + d, new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Recto::new));
                Combatiente quieto = new Combatiente("Quieto", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                CarreraDeObstaculos c = new CarreraDeObstaculos(ingenuo, quieto, 1234L, false, null, d);
                for (int t = 0; t < c.maxTurnos() && !c.terminada(); t++) {
                    c.avanzar();
                }
                cierto(c.terminada(), "carrera con robot ingenuo termina en " + d);
                cierto(c.pelea.cuerpo(0).fuera, "robot ingenuo queda destruido antes de cruzar la meta en " + d);
                falso(c.ganador() == 0, "robot ingenuo NO gana la carrera en " + d);
                cierto(c.pelea.cuerpo(0).x < c.metaX(), "destruido antes de la línea de meta en " + d);
            }
        });
    }

    private static void apuntarTrampaMortalYBonosDeReparacion() {
        caso("carrera de obstáculos: trampas mortales instantáneas y bonos de reparación detectables ES/DE/EN", () -> {
            Combatiente jug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(jug, riv, 515L, false, null, DificultadCarrera.JUNIOR);

            List<Arena.Trampa> mortales = carrera.trampasMortalesActivas();
            List<Arena.Trampa> reparaciones = carrera.reparacionesActivas();
            cierto(mortales.size() >= 2, "existen trampas mortales en pista: " + mortales.size());
            cierto(reparaciones.size() >= 2, "existen bonos de reparación en pista: " + reparaciones.size());

            Cuerpo c0 = carrera.pelea.cuerpo(0);
            Arena.Trampa mortal = mortales.get(0);
            Arena.Trampa rep = reparaciones.get(0);
            cierto(contiene(c0.obstaculos(), mortal), "trampa_mortal visible en obstaculos()");
            cierto(contiene(c0.obstaculos(), rep), "reparacion visible en obstaculos()");
            boolean vioMortalDe = false, vioRepEn = false;
            for (Cosa cosa : c0.obstaculos()) {
                if (cosa.typ().equals(Arena.TIPO_TRAMPA_MORTAL) && cosa.gefaehrlich()) vioMortalDe = true;
                if (cosa.type().equals(Arena.TIPO_REPARACION) && !cosa.dangerous()) vioRepEn = true;
            }
            cierto(vioMortalDe, "fachada DE (typ/gefaehrlich) detecta trampa_mortal");
            cierto(vioRepEn, "fachada EN (type/dangerous) detecta reparacion");

            // 1) Recoger bono de reparación desaparece el objeto y recupera blindaje sin pasar del máximo.
            for (int k = 0; k < 4; k++) c0.parte[k] = c0.parteMax[k] * 0.5;
            double blindajeAntes = c0.blindaje();
            c0.x = rep.x;
            c0.y = rep.y;
            carrera.avanzar();
            cierto(rep.destruida(), "el bono de reparación desaparece al recogerlo");
            cierto(c0.blindaje() > blindajeAntes + 15.0, "recupera blindaje del robot dañado: " + blindajeAntes + " -> " + c0.blindaje());
            cierto(c0.blindaje() <= 100.0 + 1e-6, "la reparación nunca supera el 100 % de blindaje");

            // 2) Pisar trampa mortal destruye inmediatamente al robot incluso con 100 % de blindaje.
            for (int k = 0; k < 4; k++) c0.parte[k] = c0.parteMax[k];
            igual(100.0, Math.round(c0.blindaje()) * 1.0, "blindaje restaurado al 100 % antes de pisar trampa mortal");
            c0.x = mortal.x;
            c0.y = mortal.y;
            carrera.avanzar();
            cierto(c0.fuera, "pisar trampa_mortal destruye instantáneamente al robot al 100 %");
            igual(0.0, c0.blindaje(), "blindaje a 0 tras trampa mortal");
            cierto(carrera.terminada() && carrera.ganador() == 1, "la carrera termina por KO al pisar trampa mortal");
        });
    }

    private static void apuntarMedallasCuartosDeMonedaYPersistencia() {
        caso("carrera de obstáculos: medallas por daño, +1/4 de moneda por perfección y persistencia de 4/4", () -> {
            Path carpetaAnterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("mjb-carrera-cuartos");
            try {
                Guardado.carpeta = temp;
                Jugador jug = new Jugador("PilotoPerfecto");
                jug.monedas = 5.0;
                jug.cuartosDeMoneda = 0;

                // Tres carreras perfectas: cada una da +1 moneda y +1 cuarto (3/4).
                for (int i = 1; i <= 3; i++) {
                    Combatiente cJug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                    CarreraDeObstaculos c = new CarreraDeObstaculos(cJug, CarreraDeObstaculos.rivalMaquina("IA"), 200L + i, true);
                    c.pelea.cuerpo(0).x = c.metaX() + 5;
                    c.avanzar();
                    igual(CarreraDeObstaculos.Medalla.ORO, c.medallaObtenida(), "victoria sin daño obtiene medalla de oro");
                    cierto(c.aplicarRecompensa(jug), "aplica recompensa perfecta #" + i);
                    igual(1, c.cuartosGanados(), "otorga +1 cuarto por carrera perfecta");
                    igual(0, c.monedasExtraPorCuartos(), "aún no completa 4 cuartos en #" + i);
                    igual(i, jug.cuartosDeMoneda, "cuartos acumulados = " + i);
                }
                igual(8.0, jug.monedas, "3 victorias perfectas suman 3 monedas base (5 -> 8)");

                Guardado.guardar(jug);
                Jugador recargado = Guardado.leer(Guardado.carpetaDe(jug.nombre));
                igual(3, recargado.cuartosDeMoneda, "cuartosDeMoneda=3 persiste en Guardado");

                // Cuarta carrera perfecta: 3/4 + 1/4 = 4/4 -> +1 moneda base + 1 moneda extra, cuartos reinicia a 0.
                Combatiente cJug4 = new Combatiente("Jug4", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                CarreraDeObstaculos c4 = new CarreraDeObstaculos(cJug4, CarreraDeObstaculos.rivalMaquina("IA"), 204L, true);
                c4.pelea.cuerpo(0).x = c4.metaX() + 5;
                c4.avanzar();
                cierto(c4.aplicarRecompensa(recargado), "aplica cuarta recompensa perfecta");
                igual(1, c4.monedasExtraPorCuartos(), "al llegar a 4/4 otorga +1 moneda extra");
                igual(0, recargado.cuartosDeMoneda, "cuartosDeMoneda vuelve a 0 tras completar 4/4");
                igual(10.0, recargado.monedas, "8 + 1 victoria + 1 extra por 4/4 = 10 monedas");

                Guardado.guardar(recargado);
                Jugador trasCuatro = Guardado.leer(Guardado.carpetaDe(recargado.nombre));
                igual(10.0, trasCuatro.monedas, "moneda extra por 4/4 persiste tras guardar");
                igual(0, trasCuatro.cuartosDeMoneda, "cuartos reiniciados persisten tras guardar");

                // Tramos intermedios: plata y bronce no suman cuarto de moneda.
                Combatiente cPlata = new Combatiente("Plata", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                CarreraDeObstaculos cp = new CarreraDeObstaculos(cPlata, CarreraDeObstaculos.rivalMaquina("IA"), 205L, true);
                cPlata.danoTotalRecibido = cp.pelea.cuerpo(0).blindajeMaximoTotal() * 0.12;
                cp.pelea.cuerpo(0).x = cp.metaX() + 5;
                cp.avanzar();
                igual(CarreraDeObstaculos.Medalla.PLATA, cp.medallaObtenida(), "12 % daño -> medalla de plata");
                cp.aplicarRecompensa(trasCuatro);
                igual(0, cp.cuartosGanados(), "plata no suma cuarto de moneda");
                igual(0, trasCuatro.cuartosDeMoneda, "cuartos sin cambios tras plata");

                Combatiente cBronce = new Combatiente("Bronce", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
                CarreraDeObstaculos cb = new CarreraDeObstaculos(cBronce, CarreraDeObstaculos.rivalMaquina("IA"), 206L, true);
                cBronce.danoTotalRecibido = cb.pelea.cuerpo(0).blindajeMaximoTotal() * 0.35;
                cb.pelea.cuerpo(0).x = cb.metaX() + 5;
                cb.avanzar();
                igual(CarreraDeObstaculos.Medalla.BRONCE, cb.medallaObtenida(), "35 % daño -> medalla de bronce");
            } finally {
                Guardado.carpeta = carpetaAnterior;
            }
        });
    }
}
