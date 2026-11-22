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

/** Pruebas automatizadas de la Carrera de obstáculos en Modo especial y su registro en el entrenador. */
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
        apuntarGeneracionYVisibilidad();
        apuntarDestructiblesEIndestructibles();
        apuntarRecompensaMonedas();
        apuntarRivalAjeno();
        apuntarPlantillasTextosYEntrenador();
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

    private static void apuntarGeneracionYVisibilidad() {
        caso("carrera de obstáculos: pista con visibles, ocultos detectables por radar, destructibles e indestructibles", () -> {
            cierto(ModoEspecial.CLAVES_PESTANAS.contains("especial.carrera"), "ModoEspecial incluye pestaña especial.carrera");
            Combatiente jug = new Combatiente("jug", new Montaje(Modelo.de("cobalto")), PruebasDeLaPelea.de(Quieto::new));
            Combatiente riv = CarreraDeObstaculos.rivalMaquina("ia");
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(jug, riv, 42L, true);

            List<Arena.Trampa> visibles = carrera.obstaculosVisiblesEnMapa();
            List<Arena.Trampa> ocultos = carrera.obstaculosOcultosEnMapa();
            cierto(visibles.size() >= 4, "hay obstáculos visibles en el mapa general: " + visibles.size());
            cierto(ocultos.size() >= 3, "hay obstáculos ocultos en el mapa general: " + ocultos.size());

            int destructibles = 0, indestructibles = 0;
            for (Arena.Trampa t : carrera.pelea.arena.trampas()) {
                if (t.destructible) destructibles++;
                else indestructibles++;
            }
            cierto(destructibles >= 4, "hay barriles/chimeneas destructibles");
            cierto(indestructibles >= 4, "hay obstáculos indestructibles");

            // Un obstáculo oculto lejano NO aparece en obstaculos() al inicio, pero sí al acercarse al alcance de radar/sensores.
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

    private static void apuntarDestructiblesEIndestructibles() {
        caso("carrera de obstáculos: destructibles se rompen y desaparecen de la API; indestructibles resisten impactos", () -> {
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
            c1.y = 620;
            cierto(contiene(c0.obstaculos(), barril), "el barril aparece inicialmente en obstaculos()");
            for (int t = 0; t < 200 && !barril.destruida(); t++) {
                c0.x = barril.x - 130;
                c0.y = barril.y;
                c0.mira = 0;
                carrera.avanzar();
            }
            cierto(barril.destruida(), "el barril se destruye con proyectiles");
            falso(contiene(c0.obstaculos(), barril), "el barril roto desaparece de obstaculos()");

            // Disparamos ahora contra el bloque indestructible: nunca se destruye y sigue activo.
            for (int t = 0; t < 120; t++) {
                c0.x = bloque.x - 130;
                c0.y = bloque.y;
                c0.mira = 0;
                carrera.avanzar();
            }
            falso(bloque.destruida(), "el bloque indestructible nunca se destruye");
            cierto(bloque.activa(), "el bloque indestructible sigue activo");
            cierto(contiene(c0.obstaculos(), bloque), "el bloque indestructible sigue en obstaculos()");
            cierto(obsDe[0].vistos >= 1, "hindernisse() funciona en la pista");

            // Comprobación en inglés (obstacles()).
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

    private static void apuntarRecompensaMonedas() {
        caso("carrera de obstáculos: victoria suma exactamente 1 moneda y guarda; derrota no suma", () -> {
            Path carpetaAnterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("mjb-carrera-monedas");
            try {
                Guardado.carpeta = temp;
                Jugador jug = new Jugador("PilotoPrueba");
                jug.monedas = 2.0;

                // Victoria contra la máquina (colocamos al jugador cruzando la meta).
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

                // Derrota contra la máquina: no suma monedas.
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
        });
    }

    private static void apuntarRivalAjeno() {
        caso("carrera de obstáculos: carga y carrera contra robot ajeno bajo Vigilante y Cargador", () -> {
            if (!Compilador.hayCompilador()) return;
            Path temp = Files.createTempDirectory("mjb-carrera-ajeno");
            Path valido = temp.resolve("RivalVeloz.java");
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
            for (int t = 0; t < 60 && !carrera.terminada(); t++) {
                carrera.avanzar();
            }
            cierto(carrera.progreso(1) > 0.05, "el robot ajeno avanza realmente en la pista: " + carrera.progreso(1));

            // Un robot ajeno que intenta usar API prohibida (java.io.File) es bloqueado por Vigilante.
            Path prohibido = temp.resolve("RobotProhibido.java");
            String codigoProhibido = "import metajavbots.api.*;\n"
                    + "public class RobotProhibido extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        new java.io.File(\"x\").exists();\n"
                    + "    }\n"
                    + "}\n";
            Files.writeString(prohibido, codigoProhibido, StandardCharsets.UTF_8);
            PantallaCarreraDeObstaculos.CargaAjeno bloqueado = PantallaCarreraDeObstaculos.cargarRivalAjeno(prohibido);
            falso(bloqueado.bien(), "Vigilante bloquea código ajeno con clases prohibidas");
        });
    }

    private static void apuntarPlantillasTextosYEntrenador() {
        caso("carrera de obstáculos: plantillas ES/DE/EN compilan sin fugas, ui.txt completo y conocimiento del entrenador", () -> {
            if (Compilador.hayCompilador()) {
                for (Idioma id : Idioma.values()) {
                    String codigo = CarreraDeObstaculos.plantillaInicial(id);
                    RobotDelJugador r = new RobotDelJugador();
                    Compilador.Resultado res = r.compilar(codigo, id.locale(), false);
                    cierto(res.bien, "plantilla de carrera compila en " + id + ": " + res.errores);
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
                }
            }

            // Claves de ui.txt completas en ES, DE y EN.
            String[] claves = {
                    "especial.carrera", "especial.carrera_ayuda", "especial.carrera_entrar",
                    "carrera_obs.rival", "carrera_obs.vs_maquina", "carrera_obs.vs_usuario",
                    "carrera_obs.nueva_pista", "carrera_obs.sin_ajenos", "carrera_obs.mapa_titulo",
                    "carrera_obs.leyenda_destructible", "carrera_obs.leyenda_indestructible", "carrera_obs.leyenda_ocultos",
                    "carrera_obs.iniciar", "carrera_obs.ganas", "carrera_obs.pierdes", "carrera_obs.empate",
                    "carrera_obs.premio_moneda", "carrera_obs.sin_moneda"
            };
            for (Idioma id : Idioma.values()) {
                for (String k : claves) {
                    String val = T.en(k, id);
                    cierto(val != null && !val.isBlank() && !val.equals(k), "clave ui.txt presente en " + id + ": " + k);
                }
            }

            // Conocimiento del entrenador offline, consejo espontáneo e IA en ES, DE y EN.
            ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("carrera_de_obstaculos");
            cierto(entrada != null, "existe ficha carrera_de_obstaculos en ConocimientoDelEntrenador");
            cierto(!entrada.consejos().isEmpty(), "existe consejo espontáneo para carrera_de_obstaculos");
            String[][] consultas = {
                    {"carrera de obstáculos", "ES"},
                    {"Hindernislauf", "DE"},
                    {"obstacle race", "EN"}
            };
            for (String[] q : consultas) {
                Idioma id = Idioma.valueOf(q[1]);
                List<Concepto> res = Concepto.buscar(q[0], id, 3);
                cierto(!res.isEmpty() && res.get(0).id.equals("carrera_de_obstaculos"),
                        "búsqueda offline encuentra carrera_de_obstaculos para «" + q[0] + "» (" + id + ")");
                String tip = ConsejosDelJuego.textoDelTip(entrada.consejos().get(0).aTip(), id);
                cierto(tip != null && !tip.isBlank(), "consejo espontáneo trilingüe no vacío en " + id);
                String sistemaIa = ConsultaIaEntrenador.construirSistema(new Jugador("Test"), id, null);
                cierto(sistemaIa.toLowerCase(Locale.ROOT).contains("obstacle race"),
                        "sistema IA incluye explícitamente Obstacle race en " + id);
            }
        });
    }
}
