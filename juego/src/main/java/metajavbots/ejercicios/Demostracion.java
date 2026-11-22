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
 * explicaciones claras del entrenador.
 */
public final class Demostracion {

    /** Acción que ejecuta el robot de demostración en la arena durante un paso. */
    public enum Accion {
        QUIETO,
        AVANZAR,
        GIRAR,
        RADAR,
        DETECTAR,
        APUNTAR_Y_DISPARAR,
        FALLO_ORDEN,
        ORDEN_TURNO,
        CORREGIR_LINEA,
        ACIERTO_ORDEN,
        ENTORNO_INGENUO,
        AJUSTAR_ENTORNO,
        ENTORNO_AJUSTADO,
        POO_EXPUESTO,
        POO_ENCAPSULADO,
        APARATO_ERROR,
        APARATO_BIEN,
        GESTION_ENERGIA,
        TOPE_DEUDA,
        TOPE_OPTIMO,
        CIERRE
    }

    /** Color del resaltado de la línea activa (▶) en el editor de código. */
    public enum Resaltado {
        BIEN,
        AVISO,
        ERROR
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
        public final Resaltado resaltado;
        public final Trilingue codigoPaso;

        public Paso(String id, Trilingue titulo, Trilingue texto, int lineaPrincipal, int[] lineasSecundarias,
                boolean activarVisor, int turnosMinimos, int turnosMaximos, Accion accion) {
            this(id, titulo, texto, lineaPrincipal, lineasSecundarias, activarVisor, turnosMinimos, turnosMaximos,
                    accion, Resaltado.BIEN, null);
        }

        public Paso(String id, Trilingue titulo, Trilingue texto, int lineaPrincipal, int[] lineasSecundarias,
                boolean activarVisor, int turnosMinimos, int turnosMaximos, Accion accion,
                Resaltado resaltado, Trilingue codigoPaso) {
            this.id = id;
            this.titulo = titulo;
            this.texto = texto;
            this.lineaPrincipal = lineaPrincipal;
            this.lineasSecundarias = lineasSecundarias == null ? new int[0] : lineasSecundarias.clone();
            this.activarVisor = activarVisor;
            this.turnosMinimos = Math.max(0, turnosMinimos);
            this.turnosMaximos = Math.max(this.turnosMinimos, turnosMaximos);
            this.accion = accion == null ? Accion.QUIETO : accion;
            this.resaltado = resaltado == null ? Resaltado.BIEN : resaltado;
            this.codigoPaso = codigoPaso;
        }

        public String titulo(Idioma idioma) {
            return titulo.en(idioma);
        }

        public String texto(Idioma idioma) {
            return texto.en(idioma);
        }

        /** Código que debe mostrar el editor en este paso (el propio del paso si lo cambia, o el general de la demo). */
        public String codigo(Demostracion demo, Idioma idioma) {
            return codigoPaso != null ? codigoPaso.en(idioma) : demo.codigo(idioma);
        }
    }

    public static final String ID_DEMO_1 = "demo_01_basica";
    public static final String ID_DEMO_2 = "demo_02_orden";
    public static final String ID_DEMO_3 = "demo_03_entorno";
    public static final String ID_DEMO_4 = "demo_04_arquitectura";

    private static final List<Demostracion> TODAS = Collections.unmodifiableList(
            List.of(crearDemo1(), Demo2Orden.crear(), Demo3Entorno.crear(), Demo4Arquitectura.crear()));

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
                "Guía paso a paso desde cero: qué es cada línea del código, qué significa cada número, el radar, los disparos y las rondas.",
                "Schritt-für-Schritt-Anleitung von Anfang an: was jede Codezeile tut, was jede Zahl bedeutet, Radar, Schüsse und Runden.",
                "Step-by-step guide from scratch: what each line of code does, what every number means, radar, shooting, and rounds.");
        Trilingue codigo = new Trilingue(codigoDemo1Es(), codigoDemo1De(), codigoDemo1En());
        return new Demostracion(ID_DEMO_1, 1, titulo, resumen, VistaDeArena.Escenario.HANGAR, codigo, pasosDemo1());
    }

    private static List<Paso> pasosDemo1() {
        return List.of(
                paso1IntroJuego(),
                paso2CadaTurno(),
                paso3Avanzar(),
                paso4Girar(),
                paso5Radar(),
                paso6VerEnemigo(),
                paso7ApuntarDisparar(),
                paso8PelearRondas());
    }

    private static Paso paso1IntroJuego() {
        return new Paso(
                "intro_juego",
                new Trilingue(
                        "¿De qué va este juego?",
                        "Worum geht es in diesem Spiel?",
                        "What is this game about?"),
                new Trilingue(
                        "Aquí no mueves al robot con flechas como un coche teledirigido: tu robot pelea solo.\n"
                                + "El código de la derecha es su hoja de instrucciones, como una receta de cocina que obedece al pie de la letra.\n"
                                + "En el [[Taller]] escribes tu plan, pulsas Ctrl+S para guardarlo y comprobar que no tiene faltas (compilar), y lo pruebas.\n"
                                + "Si alguna vez te atascas, pulsa F2 para abrir al entrenador.",
                        "Hier steuerst du den Roboter nicht mit Pfeiltasten wie ein Fernsteuerauto: Dein Roboter kämpft ganz allein.\n"
                                + "Der Code rechts ist seine Anleitung, wie ein Kochrezept, das er Schritt für Schritt genau befolgt.\n"
                                + "In der [[Werkstatt]] schreibst du deinen Plan, drückst Strg+S zum Speichern und Prüfen "
                                + "auf Fehler (Kompilieren) und testest ihn.\n"
                                + "Wenn du einmal nicht weiterweißt, drücke F2 für deinen Trainer.",
                        "Here you do not drive your robot with arrow keys like a remote-control car: your robot fights on its own.\n"
                                + "The code on the right is its instruction sheet, like a recipe that it follows word for word.\n"
                                + "In the [[Workshop]] you write your plan, press Ctrl+S to save and check it for mistakes (compile), and test it.\n"
                                + "If you ever get stuck, press F2 to open the coach."),
                3, new int[]{5, 14}, false, 0, 0, Accion.QUIETO);
    }

    private static Paso paso2CadaTurno() {
        return new Paso(
                "cada_turno",
                new Trilingue(
                        "El latido del robot: cadaTurno()",
                        "Der Herzschlag des Roboters: jedenZug()",
                        "The robot's heartbeat: onTick()"),
                new Trilingue(
                        "cadaTurno() es el latido del corazón del robot: mientras dura la pelea, se repite unas 30 veces por segundo.\n"
                                + "Todo lo que pongas entre sus llaves ({}) se lee de arriba abajo una y otra vez, sin parar.\n"
                                + "Cada línea que ves ahí dentro es una orden pequeña que le dice al robot qué hacer en ese instante.",
                        "jedenZug() ist der Herzschlag deines Roboters: Während des Kampfes wiederholt er sich etwa 30 Mal pro Sekunde.\n"
                                + "Alles, was zwischen den geschweiften Klammern ({}) steht, liest der Roboter immer wieder von oben nach unten.\n"
                                + "Jede Zeile darin ist ein kurzer Befehl, der dem Roboter sagt, was er gerade tun soll.",
                        "onTick() is the robot's heartbeat: while the fight lasts, it repeats about 30 times every second.\n"
                                + "Everything you put between its curly braces ({}) is read from top to bottom over and over without stopping.\n"
                                + "Each line inside is a short order telling the robot what to do right now."),
                5, new int[]{6, 7, 8, 9, 10, 11}, false, 0, 0, Accion.QUIETO);
    }

    private static Paso paso3Avanzar() {
        return new Paso(
                "avanzar",
                new Trilingue(
                        "Avanzar y qué significa el número",
                        "Vorwärts fahren und was die Zahl bedeutet",
                        "Moving forward and what the number means"),
                new Trilingue(
                        "avanzar(80); no mueve al robot de golpe: fija una orden solicitada de 80 unidades hacia adelante.\n"
                                + "El robot cumple esa orden poco a poco durante varios turnos según su velocidad y aceleración: "
                                + "la distancia efectiva recorrida es menor en cada turno y puede detenerse antes si choca o recibe otra orden.\n"
                                + "El número entre paréntesis () marca ese objetivo (20 es corto, 150 largo y -50 marcha atrás); "
                                + "el punto y coma (;) cierra la orden.",
                        "vorwaerts(80); bewegt den Roboter nicht sofort: Es setzt einen angeforderten Befehl über 80 Einheiten nach vorn.\n"
                                + "Der Roboter führt diesen Befehl über mehrere Züge hinweg je nach Tempo und Beschleunigung aus: "
                                + "Die tatsächlich zurückgelegte Strecke ist pro Zug kleiner und kann bei einer Wand oder einem neuen Befehl früher enden.\n"
                                + "Die Zahl in Klammern () legt dieses Ziel fest (20 kurz, 150 lang, -50 rückwärts); "
                                + "das Semikolon (;) schließt den Befehl ab.",
                        "ahead(80); does not teleport the robot: it sets a requested order of 80 units forward.\n"
                                + "The robot carries it out gradually over several ticks according to its speed and acceleration: "
                                + "the actual distance traveled each tick is smaller and may stop early on a wall or a new order.\n"
                                + "The number in parentheses () sets that target (20 is short, 150 is long, -50 goes backward); "
                                + "the semicolon (;) ends the order."),
                6, new int[]{5}, false, 18, 45, Accion.AVANZAR);
    }

    private static Paso paso4Girar() {
        return new Paso(
                "girar",
                new Trilingue(
                        "Girar y el truco del reloj",
                        "Drehen und der Uhren-Trick",
                        "Turning and the clock trick"),
                new Trilingue(
                        "girar(45); hace que el cuerpo del robot gire sin moverse del sitio. En la arena, 0° apunta a la derecha (como las 3 en un reloj) "
                                + "y 90° apunta hacia arriba (como las 12).\n"
                                + "Los números positivos giran en sentido contrario a las agujas del reloj (hacia la izquierda del robot): "
                                + "90 es un cuarto de vuelta (de las 3 a las 12), 180 es media vuelta completa y 45 es un giro suave.\n"
                                + "Con signo menos, como girar(-45);, gira en el sentido de las agujas del reloj (hacia la derecha del robot): "
                                + "-90 gira como de las 12 a las 3.",
                        "drehen(45); dreht den Körper des Roboters auf der Stelle. In der Arena zeigt 0° nach rechts (wie 3 Uhr auf einer Uhr) "
                                + "und 90° nach oben (wie 12 Uhr).\n"
                                + "Positive Zahlen drehen gegen den Uhrzeigersinn (aus Sicht des Roboters nach links): "
                                + "90 ist eine Vierteldrehung (von 3 auf 12 Uhr), 180 eine halbe Kehrtwende und 45 eine sanfte Drehung.\n"
                                + "Mit einem Minuszeichen wie drehen(-45); dreht er im Uhrzeigersinn (aus Sicht des Roboters nach rechts): "
                                + "-90 dreht wie von 12 auf 3 Uhr.",
                        "turn(45); rotates the robot's body on the spot. In the arena, 0° points right (like 3 o'clock on a clock) "
                                + "and 90° points up (like 12 o'clock).\n"
                                + "Positive numbers turn counter-clockwise (to the robot's left): "
                                + "90 is a quarter turn (from 3 to 12), 180 is a half-turn around, and 45 is a gentle turn.\n"
                                + "With a minus sign, like turn(-45);, it turns clockwise (to the robot's right): "
                                + "-90 turns like from 12 to 3."),
                7, new int[]{5}, false, 10, 35, Accion.GIRAR);
    }

    private static Paso paso5Radar() {
        return new Paso(
                "radar",
                new Trilingue(
                        "Los ojos del robot: el radar",
                        "Die Augen des Roboters: das Radar",
                        "The robot's eyes: the radar"),
                new Trilingue(
                        "Ese abanico azul en la arena es el radar: es como una linterna invisible. "
                                + "Fuera de esa luz, el robot está ciego y no sabe dónde está el rival.\n"
                                + "La línea if (enemigosVistos().isEmpty()) pregunta «¿no veo a nadie todavía?». "
                                + "Si no ve a nadie, ejecuta apuntar(15);.\n"
                                + "apuntar(15); gira esa linterna y los brazos 15 grados hacia la izquierda (en sentido contrario a las agujas del reloj; "
                                + "con números negativos, hacia la derecha) sin girar todo el cuerpo.",
                        "Dieser blaue Kegel in der Arena ist das Radar: wie eine unsichtbare Taschenlampe. "
                                + "Außerhalb dieses Lichts ist der Roboter blind und weiß nicht, wo der Gegner ist.\n"
                                + "Die Zeile if (gesehen().isEmpty()) fragt «Sehe ich noch niemanden?». "
                                + "Wenn niemand da ist, läuft zielen(15);.\n"
                                + "zielen(15); dreht diese Taschenlampe samt Armen um 15 Grad nach links (gegen den Uhrzeigersinn; "
                                + "mit negativen Zahlen nach rechts), ohne den ganzen Körper zu drehen.",
                        "That blue cone in the arena is the radar: think of it as an invisible flashlight. "
                                + "Outside that beam, your robot is blind and cannot tell where the rival is.\n"
                                + "The line if (seenEnemies().isEmpty()) asks \"do I see nobody yet?\". "
                                + "When the answer is yes, it runs aim(15);.\n"
                                + "aim(15); turns that flashlight and the arms 15 degrees to the left (counter-clockwise; "
                                + "negative numbers turn right) without turning the whole body."),
                9, new int[]{8, 10}, true, 22, 34, Accion.RADAR);
    }

    private static Paso paso6VerEnemigo() {
        return new Paso(
                "ver_enemigo",
                new Trilingue(
                        "¡Enemigo a la vista!",
                        "Gegner in Sicht!",
                        "Enemy spotted!"),
                new Trilingue(
                        "¡Justo en este instante la luz del radar tocó al muñeco y saltó alVerEnemigo(Enemigo e)!\n"
                                + "Piensa en alVerEnemigo como una alarma que suena solo cuando tu linterna ilumina a un rival.\n"
                                + "La letra e es una ficha con los datos del enemigo que acabas de ver: "
                                + "hacia dónde está (e.angulo()) y a qué distancia (e.distancia()).",
                        "Genau in diesem Moment traf das Radarlicht die Puppe und löste beiSicht(Gegner e) aus!\n"
                                + "Stell dir beiSicht wie einen Alarm vor, der genau dann klingelt, wenn deine Taschenlampe einen Gegner anleuchtet.\n"
                                + "Der Buchstabe e ist wie eine Karteikarte über diesen Gegner: "
                                + "wohin du zielen musst (e.winkel()) und wie weit er entfernt ist (e.abstand()).",
                        "Right at this moment the radar beam touched the dummy and triggered onSeeEnemy(Enemy e)!\n"
                                + "Think of onSeeEnemy as an alarm bell that rings the instant your flashlight shines on a rival.\n"
                                + "The letter e is a little card with facts about that rival: "
                                + "which way they are (e.angle()) and how far away (e.distance())."),
                14, new int[]{8, 9}, true, 6, 90, Accion.DETECTAR);
    }

    private static Paso paso7ApuntarDisparar() {
        return new Paso(
                "apuntar_disparar",
                new Trilingue(
                        "Apuntar y disparar",
                        "Zielen und schießen",
                        "Aim and shoot"),
                new Trilingue(
                        "Primero apuntarHacia(e.angulo()); orienta el arma hacia el muñeco, como apuntar una manguera antes de abrir el grifo.\n"
                                + "Después atacar(); pide el disparo desde tu posición actual "
                                + "(en cada turno el motor resuelve los ataques antes del movimiento). "
                                + "¡Mira cómo impacta el tiro en el muñeco!\n"
                                + "Fíjate también en las barras de blindaje y energía encima de la arena: disparar gasta energía y el arma necesita recargar.",
                        "Zuerst richtet zielenNach(e.winkel()); die Waffe auf die Puppe aus, wie einen Gartenschlauch, bevor du den Hahn aufdrehst.\n"
                                + "Danach fordert angreifen(); den Schuss von deiner aktuellen Position an "
                                + "(pro Zug löst die Engine Angriffe vor der Bewegung aus). "
                                + "Schau, wie der Schuss die Puppe trifft!\n"
                                + "Achte auch auf die Panzerungs- und Energiebalken über der Arena: Schießen kostet Energie, und die Waffe muss nachladen.",
                        "First aimTo(e.angle()); aligns the weapon with the dummy, like aiming a garden hose before turning on the tap.\n"
                                + "Then attack(); queues the shot from your current position (each tick the engine resolves attacks before movement). "
                                + "Watch the shot hit the dummy!\n"
                                + "Also notice the armor and energy bars above the arena: shooting costs energy, and the weapon needs time to reload."),
                15, new int[]{14, 16}, true, 12, 110, Accion.APUNTAR_Y_DISPARAR);
    }

    private static Paso paso8PelearRondas() {
        return new Paso(
                "pelear_rondas",
                new Trilingue(
                        "Cómo se juega una pelea real",
                        "So läuft ein echter Kampf",
                        "How a real fight works"),
                new Trilingue(
                        "Ya conoces el ciclo completo del juego: en el [[Taller]] escribes tu hoja de instrucciones y la pruebas contra el muñeco.\n"
                                + "Pulsas Ctrl+S para guardarla y comprobar que no hay errores, y luego vas al botón [[¡A pelear!]] contra rivales de verdad.\n"
                                + "Entre cada ronda la pelea se pausa sola: así ves qué salió mal, "
                                + "cambias líneas de tu código o pones piezas nuevas y vuelves a intentarlo.",
                        "Jetzt kennst du den ganzen Ablauf: In der [[Werkstatt]] schreibst du deine Anleitung und testest sie an der Puppe.\n"
                                + "Du drückst Strg+S zum Speichern und Prüfen und klickst dann auf die Schaltfläche [[Auf in den Kampf!]] gegen echte Gegner.\n"
                                + "Zwischen den Runden pausiert der Kampf von selbst: So siehst du, was schiefging, "
                                + "änderst Codezeilen oder baust neue Teile ein und probierst es erneut.",
                        "Now you know the full game loop: in the [[Workshop]] you write your instruction sheet and test it against the dummy.\n"
                                + "Press Ctrl+S to save and check it, then click the [[Let's fight!]] button against real rivals.\n"
                                + "Between every round the fight pauses on its own: you can see what went wrong, "
                                + "tweak lines of code or swap parts, and try again."),
                3, new int[]{5, 14, 15, 16}, true, 0, 0, Accion.CIERRE);
    }

    private static String codigoDemo1Es() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        avanzar(80);\n"
                + "        girar(45);\n"
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
                + "        vorwaerts(80);\n"
                + "        drehen(45);\n"
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
                + "        ahead(80);\n"
                + "        turn(45);\n"
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
