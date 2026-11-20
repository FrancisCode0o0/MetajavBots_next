package metajavbots.ejercicios;

import java.util.Collections;
import java.util.List;

import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * DEMOSTRACIÓN — Modelo reutilizable de una demostración automatizada en vivo:
 * código de ejemplo en los tres idiomas (API activa de ES, DE y EN) y una
 * secuencia de pasos con acciones reales en la arena, líneas resaltadas y
 * explicaciones cortas del entrenador.
 */
public final class Demostracion {

    /** Acción que ejecuta el robot de demostración en la arena durante un paso. */
    public enum Accion {
        QUIETO,
        MOVER_Y_GIRAR,
        RADAR_Y_DISPARO,
        CIERRE
    }

    /** Un paso de la demostración: título, explicación del entrenador, líneas a resaltar y acción en la arena. */
    public static final class Paso {
        public final String id;
        public final Trilingue titulo;
        public final Trilingue texto;
        public final int lineaPrincipal;
        public final int[] lineasSecundarias;
        public final boolean activarVisor;
        public final int turnosMinimos;
        public final int turnosMaximos;
        public final Accion accion;

        public Paso(String id, Trilingue titulo, Trilingue texto, int lineaPrincipal, int[] lineasSecundarias,
                boolean activarVisor, int turnosMinimos, int turnosMaximos, Accion accion) {
            this.id = id;
            this.titulo = titulo;
            this.texto = texto;
            this.lineaPrincipal = lineaPrincipal;
            this.lineasSecundarias = lineasSecundarias == null ? new int[0] : lineasSecundarias.clone();
            this.activarVisor = activarVisor;
            this.turnosMinimos = Math.max(0, turnosMinimos);
            this.turnosMaximos = Math.max(this.turnosMinimos, turnosMaximos);
            this.accion = accion == null ? Accion.QUIETO : accion;
        }

        public String titulo(Idioma idioma) {
            return titulo.en(idioma);
        }

        public String texto(Idioma idioma) {
            return texto.en(idioma);
        }
    }

    public static final String ID_DEMO_1 = "demo_01_basica";

    private static final List<Demostracion> TODAS = Collections.unmodifiableList(List.of(crearDemo1()));

    public final String id;
    public final int nivel;
    public final Trilingue titulo;
    public final Trilingue resumen;
    public final VistaDeArena.Escenario escenario;
    public final Trilingue codigo;
    public final List<Paso> pasos;

    public Demostracion(String id, int nivel, Trilingue titulo, Trilingue resumen,
            VistaDeArena.Escenario escenario, Trilingue codigo, List<Paso> pasos) {
        this.id = id;
        this.nivel = nivel;
        this.titulo = titulo;
        this.resumen = resumen;
        this.escenario = escenario;
        this.codigo = codigo;
        this.pasos = Collections.unmodifiableList(pasos);
    }

    /** Todas las demostraciones disponibles en orden de aprendizaje. */
    public static List<Demostracion> todas() {
        return TODAS;
    }

    /** Busca una demostración por su identificador (o null si no existe). */
    public static Demostracion de(String id) {
        if (id == null) return null;
        for (Demostracion d : TODAS) {
            if (d.id.equals(id)) return d;
        }
        return null;
    }

    public String titulo(Idioma idioma) {
        return titulo.en(idioma);
    }

    public String resumen(Idioma idioma) {
        return resumen.en(idioma);
    }

    public String codigo(Idioma idioma) {
        return codigo.en(idioma);
    }

    private static Demostracion crearDemo1() {
        Trilingue titulo = new Trilingue(
                "Demo 1: Cómo funciona el juego y tu robot",
                "Demo 1: So funktioniert das Spiel und dein Roboter",
                "Demo 1: How the game and your robot work");
        Trilingue resumen = new Trilingue(
                "Flujo del Taller, mover y girar, radar y disparo, y pausa entre rondas.",
                "Ablauf in der Werkstatt, Fahren und Drehen, Radar und Schuss, Pause zwischen Runden.",
                "Workshop flow, move and turn, radar and shot, and the pause between rounds.");
        Trilingue codigo = new Trilingue(codigoDemo1Es(), codigoDemo1De(), codigoDemo1En());
        return new Demostracion(ID_DEMO_1, 1, titulo, resumen, VistaDeArena.Escenario.HANGAR, codigo, pasosDemo1());
    }

    private static List<Paso> pasosDemo1() {
        return List.of(
                new Paso(
                        "taller",
                        new Trilingue("El Taller", "Die Werkstatt", "The Workshop"),
                        new Trilingue(
                                "Empiezas en el Taller. Escribes tu código a la derecha y lo pruebas.\n"
                                        + "Si tienes dudas, pulsa F2 para preguntarme.\n"
                                        + "Cuando acabes, pulsa Ctrl+S para compilar.",
                                "Du startest in der Werkstatt. Rechts schreibst du deinen Code und testest ihn.\n"
                                        + "Bei Fragen drückst du F2 und fragst mich.\n"
                                        + "Wenn du fertig bist, drückst du Strg+S zum Kompilieren.",
                                "You start in the Workshop. Write your code on the right and test it.\n"
                                        + "If you get stuck, press F2 to ask me.\n"
                                        + "When you're ready, press Ctrl+S to compile."),
                        3, new int[]{5}, false, 0, 0, Accion.QUIETO),
                new Paso(
                        "mover_girar",
                        new Trilingue("Mover y girar", "Fahren und drehen", "Move and turn"),
                        new Trilingue(
                                "cadaTurno() se ejecuta todo el rato.\n"
                                        + "avanzar(120) mueve tu robot hacia delante y girar(12) gira su cuerpo.\n"
                                        + "Mira cómo cambia de sitio y gira en la arena.",
                                "jedenZug() läuft die ganze Zeit.\n"
                                        + "vorwaerts(120) fährt deinen Roboter nach vorn und drehen(12) dreht ihn.\n"
                                        + "Schau, wie er sich in der Arena bewegt und dreht.",
                                "onTick() runs over and over.\n"
                                        + "ahead(120) moves your robot forward and turn(12) turns its body.\n"
                                        + "Watch it move and turn in the arena."),
                        6, new int[]{7}, false, 45, 55, Accion.MOVER_Y_GIRAR),
                new Paso(
                        "radar_disparo",
                        new Trilingue("Radar y disparo", "Radar und Schuss", "Radar and shot"),
                        new Trilingue(
                                "El cono azul es tu radar. enemigosVistos() mira si hay alguien.\n"
                                        + "Al detectar al muñeco salta alVerEnemigo(e).\n"
                                        + "Luego apuntarHacia(e.angulo()) apunta y atacar() dispara.",
                                "Der blaue Kegel ist dein Radar. gesehen() prüft, ob jemand da ist.\n"
                                        + "Erkennt es die Puppe, startet beiSicht(e).\n"
                                        + "Dann zielt zielenNach(e.winkel()) und angreifen() schießt.",
                                "The blue cone is your radar. seenEnemies() checks if anyone is there.\n"
                                        + "When it spots the dummy, onSeeEnemy(e) runs.\n"
                                        + "Then aimTo(e.angle()) aims and attack() shoots."),
                        15, new int[]{8, 14, 16}, true, 25, 110, Accion.RADAR_Y_DISPARO),
                new Paso(
                        "pelear_rondas",
                        new Trilingue("¡A pelear! y rondas", "Auf in den Kampf! und Runden", "Let's fight! and rounds"),
                        new Trilingue(
                                "Con el código compilado, pulsa ¡A pelear!.\n"
                                        + "Entre cada ronda el juego se pausa solo.\n"
                                        + "Así ves qué falló, cambias tu código o equipas otras piezas antes de seguir.",
                                "Ist dein Code kompiliert, drückst du Auf in den Kampf!.\n"
                                        + "Nach jeder Runde pausiert das Spiel von selbst.\n"
                                        + "So siehst du, was schiefging, änderst den Code oder tauschst Teile.",
                                "Once your code compiles, press Let's fight!.\n"
                                        + "Between every round, the game pauses automatically.\n"
                                        + "That way you can see what went wrong, edit your code, or swap parts before the next round."),
                        3, new int[]{5, 14}, true, 0, 0, Accion.CIERRE));
    }

    private static String codigoDemo1Es() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        avanzar(120);\n"
                + "        girar(12);\n"
                + "        if (enemigosVistos().isEmpty()) {\n"
                + "            apuntar(15);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void alVerEnemigo(Enemigo e) {\n"
                + "        apuntarHacia(e.angulo());\n"
                + "        atacar();\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoDemo1De() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        vorwaerts(120);\n"
                + "        drehen(12);\n"
                + "        if (gesehen().isEmpty()) {\n"
                + "            zielen(15);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void beiSicht(Gegner e) {\n"
                + "        zielenNach(e.winkel());\n"
                + "        angreifen();\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoDemo1En() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        ahead(120);\n"
                + "        turn(12);\n"
                + "        if (seenEnemies().isEmpty()) {\n"
                + "            aim(15);\n"
                + "        }\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void onSeeEnemy(Enemy e) {\n"
                + "        aimTo(e.angle());\n"
                + "        attack();\n"
                + "    }\n"
                + "}\n";
    }
}
