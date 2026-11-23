package metajavbots.escritorio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;
import metajavbots.pantallas.PantallaPelea;
import metajavbots.pantallas.Taller;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;

/**
 * ESCENAS DEL TRÁILER — Lo que se graba, en orden: el código de un robot escribiéndose en el Taller, un duelo, un battle
 * royale con cinco robots, un combate por equipos (dos contra dos), la Carrera de obstáculos con robots que caen del
 * cielo, Destrucción y supervivencia, y el cierre con el título. Los robots son de lwjgl3/trailer/robots/: código de
 * jugador de verdad, con estrategias distintas, que el juego compila como compila el de cualquiera. Las peleas llevan
 * semilla fija (se graban siempre igual), elegida simulando muchas sin pantalla: que duren la ronda entera (o acaben en un
 * K.O.), con muchos golpes fuertes y pocos choques entre robots.
 */
final class EscenasDelTrailer {

    /** Una escena: su nombre (el del clip), cuánto se graba, la pantalla y lo que se hace antes de cada fotograma. */
    static final class Escena {
        final String nombre;
        final float segundos;
        final Function<Juego, Pantalla> crear;
        final BiConsumer<Juego, Integer> antes;

        Escena(String nombre, float segundos, Function<Juego, Pantalla> crear, BiConsumer<Juego, Integer> antes) {
            this.nombre = nombre;
            this.segundos = segundos;
            this.crear = crear;
            this.antes = antes;
        }
    }

    static final Path ROBOTS = Paths.get("lwjgl3", "trailer", "robots");
    /** Letras que se escriben por fotograma en el Taller (a 60 fotogramas por segundo). */
    static final int LETRAS_POR_FOTOGRAMA = 2;

    private EscenasDelTrailer() {
    }

    static List<Escena> todas() {
        return List.of(
                taller(),
                new Escena("duelo", 52, j -> pelea(j, "Profeta", "vigia", VistaDeArena.Escenario.NEON, 10,
                        new String[][] {{"Cangrejo", "chispa", "1"}}), (j, f) -> { }),
                new Escena("royale", 58, j -> pelea(j, "Halcon", "grulla", VistaDeArena.Escenario.CHATARRERIA, 17,
                        new String[][] {{"Toro", "mazo", "1"}, {"Fortin", "bastion", "2"}, {"Cangrejo", "chispa", "3"},
                            {"Profeta", "vigia", "4"}}), (j, f) -> { }),
                new Escena("equipos", 56, j -> pelea(j, "Faro", "faro", VistaDeArena.Escenario.LUNA, 20,
                        new String[][] {{"Cangrejo", "chispa", "1"}, {"Faro", "pixel", "0"}, {"Halcon", "grulla", "1"}}), (j, f) -> { }),
                carrera(),
                destruccion(),
                new Escena("titulo", 10, j -> new TituloDelTrailer(), (j, f) -> { }));
    }

    /** El código del robot tal como lo deja el editor del juego (así no cuenta como «cambios sin compilar»). */
    static String codigo(String robot) {
        try {
            VentanaDeCodigo editor = new VentanaDeCodigo();
            editor.ponerTexto(Files.readString(ROBOTS.resolve(robot + ".java"), StandardCharsets.UTF_8));
            return editor.doc.texto();
        } catch (IOException e) {
            throw new IllegalStateException("falta el robot " + robot + " en " + ROBOTS.toAbsolutePath(), e);
        }
    }

    /** Pone el robot como el del jugador (su código y su modelo), como si lo hubiera escrito y compilado en el Taller. */
    private static void robotDelJugador(Juego j, String robot, String modelo, String codigo) {
        Jugador.Robot r = new Jugador.Robot(robot, modelo);
        r.codigo = codigo;
        if (!codigo.isEmpty()) r.registrarCompilacionValida(codigo);
        j.jugador.robots.clear();
        j.jugador.robots.add(r);
        j.jugador.robotElegido = 0;
    }

    /** Un robot de lwjgl3/trailer/robots/ compilado por el compilador del juego, como el de un jugador. */
    static Combatiente compilado(String robot, String modelo) {
        RobotDelJugador r = new RobotDelJugador();
        Compilador.Resultado res = r.compilar(codigo(robot), Locale.forLanguageTag("es"));
        if (!res.bien) throw new IllegalStateException(robot + " no compila: " + res.errores);
        Combatiente c = new Combatiente(robot, new Montaje(Modelo.de(modelo)), r);
        c.computadora = true;
        return c;
    }

    /**
     * Una pelea de verdad (PantallaPelea): el jugador lleva el primer robot; los demás, {nombre, modelo, equipo}. El primero
     * de ellos es el rival (equipo 1); el resto va en «demás» con su equipo (0 es el del jugador).
     */
    private static Pantalla pelea(Juego j, String robot, String modelo, VistaDeArena.Escenario escenario, long semilla, String[][] otros) {
        robotDelJugador(j, robot, modelo, codigo(robot));
        Encuentro e = new Encuentro();
        e.modo = otros.length == 1 ? "NORMAL" : otros.length == 3 && "0".equals(otros[1][2]) ? "EQUIPOS" : "ROYALE";
        e.escenario = escenario;
        e.rondas = 1;
        e.semilla = semilla;
        e.cuenta = false;
        e.nivelRival = 0.6;
        e.musica = "combate";
        e.rival = compilado(otros[0][0], otros[0][1]);
        for (int i = 1; i < otros.length; i++) {
            Combatiente c = compilado(otros[i][0], otros[i][1]);
            if ("0".equals(otros[i][2])) {
                c = new Combatiente(otros[i][0] + " II", c.montaje, c.fabrica);
                c.computadora = false;
            }
            e.demas.add(c);
            e.equiposDeLosDemas.add(Integer.parseInt(otros[i][2]));
        }
        return new PantallaPelea(e);
    }

    /** El Cangrejo escribiéndose en el Taller, letra a letra, y al acabar, compilado (Ctrl+S). */
    private static Escena taller() {
        String codigo = codigo("Cangrejo");
        int empieza = 20, acaba = empieza + (codigo.length() + LETRAS_POR_FOTOGRAMA - 1) / LETRAS_POR_FOTOGRAMA;
        Taller[] taller = new Taller[1];
        return new Escena("taller", (acaba + 150) / 60f, j -> {
            robotDelJugador(j, "Cangrejo", "chispa", "");
            taller[0] = new Taller();
            return taller[0];
        }, (j, f) -> {
            VentanaDeCodigo ed = taller[0].editor();
            if (f == empieza - 1) ed.ponerTexto("");
            if (f >= empieza && f < acaba) {
                int desde = (f - empieza) * LETRAS_POR_FOTOGRAMA;
                ed.doc.insertar(codigo.substring(desde, Math.min(codigo.length(), desde + LETRAS_POR_FOTOGRAMA)));
            }
            if (f == acaba + 30 && ed.alCompilar != null) ed.alCompilar.run();
        });
    }

    /**
     * La Carrera de obstáculos contra la máquina, en el subnivel más alto (el que más robots tira del cielo), con el código de
     * partida del editor: se compila y sale, como al pulsar «¡A correr!». La pantalla no tiene sitio para un método más (está
     * en el límite de líneas), así que el tráiler elige la dificultad y compila por reflexión.
     */
    private static Escena carrera() {
        PantallaCarreraDeObstaculos[] carrera = new PantallaCarreraDeObstaculos[1];
        return new Escena("carrera", 40, j -> {
            robotDelJugador(j, "Halcon", "chispa", codigo("Cangrejo"));
            carrera[0] = new PantallaCarreraDeObstaculos();
            return carrera[0];
        }, (j, f) -> {
            if (f != 2) return;
            try {
                Class<PantallaCarreraDeObstaculos> c = PantallaCarreraDeObstaculos.class;
                java.lang.reflect.Field subnivel = c.getDeclaredField("subnivel");
                subnivel.setAccessible(true);
                subnivel.setInt(carrera[0], metajavbots.combate.DificultadCarrera.SUBNIVEL_MAX);
                java.lang.reflect.Method compilar = c.getDeclaredMethod("lanzarCompilacion", Juego.class, boolean.class);
                compilar.setAccessible(true);
                compilar.invoke(carrera[0], j, true);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("la Carrera ha cambiado: " + e, e);
            }
        });
    }

    /** Destrucción y supervivencia con el código de partida: se compila y empieza. */
    private static Escena destruccion() {
        PantallaMinijuegoDestruccion[] reto = new PantallaMinijuegoDestruccion[1];
        return new Escena("destruccion", 30, j -> {
            robotDelJugador(j, "Fortin", "galapago", codigo("Fortin"));
            reto[0] = new PantallaMinijuegoDestruccion();
            return reto[0];
        }, (j, f) -> {
            if (f == 2) reto[0].empezarParaElTrailer(j);
        });
    }
}
