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
import metajavbots.combate.Fabrica;
import metajavbots.combate.PistaDeCarrera;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.pantallas.ModoEspecial;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
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
                if (t.tipo.equals("impulso")) continue;
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
                ganada.pelea.cuerpo(0).x = CarreraDeObstaculos.META_X + 5;
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
                perdida.pelea.cuerpo(1).x = CarreraDeObstaculos.META_X + 5;
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
                    "carrera_obs.rival", "carrera_obs.vs_maquina", "carrera_obs.vs_usuario",
                    "carrera_obs.nueva_pista", "carrera_obs.sin_ajenos", "carrera_obs.mapa_titulo",
                    "carrera_obs.leyenda_destructible", "carrera_obs.leyenda_indestructible",
                    "carrera_obs.leyenda_impulso", "carrera_obs.leyenda_rotos", "carrera_obs.leyenda_ocultos",
                    "carrera_obs.iniciar", "carrera_obs.ganas", "carrera_obs.pierdes", "carrera_obs.empate",
                    "carrera_obs.premio_moneda", "carrera_obs.sin_moneda"
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
                    {"carrera de obstáculos", "ES", "6×", "defectuosos", "impulso"},
                    {"Hindernislauf", "DE", "6×", "defekte", "impulso"},
                    {"obstacle race", "EN", "6×", "broken", "impulso"}
            };
            for (String[] q : consultas) {
                Idioma id = Idioma.valueOf(q[1]);
                List<Concepto> res = Concepto.buscar(q[0], id, 3);
                cierto(!res.isEmpty() && res.get(0).id.equals("carrera_de_obstaculos"),
                        "búsqueda offline encuentra carrera_de_obstaculos para «" + q[0] + "» (" + id + ")");
                String texto = String.join(" ", res.get(0).parrafos(id)).toLowerCase(Locale.ROOT);
                cierto(texto.contains(q[2].toLowerCase(Locale.ROOT)) && texto.contains(q[3]) && texto.contains(q[4]),
                        "ficha del entrenador actualizada en " + id);
                String tip = ConsejosDelJuego.textoDelTip(entrada.consejos().get(0).aTip(), id);
                cierto(tip != null && !tip.isBlank(), "consejo espontáneo trilingüe no vacío en " + id);
                String sistemaIa = ConsultaIaEntrenador.construirSistema(new Jugador("Test"), id, null).toLowerCase(Locale.ROOT);
                cierto(sistemaIa.contains("obstacle race") && sistemaIa.contains("6x") && sistemaIa.contains("impulso"),
                        "sistema IA actualizado con mecánicas 6x e impulso en " + id);
            }
        });
    }
}
