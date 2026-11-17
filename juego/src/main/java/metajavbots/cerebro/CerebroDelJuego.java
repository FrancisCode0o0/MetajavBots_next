package metajavbots.cerebro;

import java.util.ArrayList;
import java.util.List;

import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * CEREBRO DEL JUEGO — El cerebro que ha elegido el jugador (Ajustes ›
 * Cerebro de IA): ninguno, el local o uno en línea. Y las peticiones que le
 * hace el juego, siempre en otro hilo (Peticion): el dibujo pregunta cada
 * fotograma si ya hay respuesta.
 */
public final class CerebroDelJuego {

    private CerebroDelJuego() {
    }

    /** El cerebro de los ajustes del jugador, o null si no tiene (el juego funciona igual). */
    public static Cerebro de(Jugador j) {
        if (j == null) return null;
        switch (j.cerebro) {
            case "LOCAL": {
                String d = j.direccionLocal == null || j.direccionLocal.isBlank() ? "http://localhost:" + CerebroLocal.PUERTO_DEL_JUEGO : j.direccionLocal;
                return new CerebroHttp(CerebroHttp.Forma.OPENAI, d, "", "", "KoboldCpp");
            }
            case "EN_LINEA": {
                String clave = Claves.leer(j.proveedor);
                if (clave.isEmpty()) return null;
                switch (j.proveedor) {
                    case "anthropic":
                        return new CerebroHttp(CerebroHttp.Forma.ANTHROPIC, "https://api.anthropic.com", clave,
                                j.modeloEnLinea.isBlank() ? "claude-haiku-5-5" : j.modeloEnLinea, "Claude");
                    case "openai":
                        return new CerebroHttp(CerebroHttp.Forma.OPENAI, "https://api.openai.com", clave,
                                j.modeloEnLinea.isBlank() ? "gpt-4o-mini" : j.modeloEnLinea, "OpenAI");
                    default:
                        // Cualquier otro compatible con OpenAI: en «direccionLocal» va su dirección.
                        return new CerebroHttp(CerebroHttp.Forma.OPENAI, j.direccionLocal, clave, j.modeloEnLinea, j.proveedor);
                }
            }
            default:
                return null;
        }
    }

    /** Una pregunta en marcha: se lanza en otro hilo y se mira si ya hay respuesta. */
    public static final class Peticion {
        public volatile String respuesta;
        public volatile String error;
        public volatile boolean lista;

        public static Peticion lanzar(Cerebro c, String sistema, List<Cerebro.Mensaje> conv, int palabras) {
            Peticion p = new Peticion();
            Thread h = new Thread(() -> {
                try {
                    p.respuesta = c.preguntar(sistema, conv, palabras);
                } catch (Exception e) {
                    p.error = e.getMessage() == null ? e.toString() : e.getMessage();
                } finally {
                    p.lista = true;
                }
            }, "cerebro");
            h.setDaemon(true);
            h.start();
            return p;
        }
    }

    // ================================================================ las peticiones del juego

    static String idiomaDe(Idioma i) {
        return i.elegir("español", "Deutsch", "English");
    }

    /** El tutor de los tutoriales y de los ejercicios: guía sin dar la solución. */
    public static String sistemaTutor(Idioma i, String nivel, String tema) {
        return "You are the friendly Java tutor inside MetajavBots, a game where robots are programmed in Java. "
                + "Answer ONLY in " + idiomaDe(i) + ". The learner's level is " + nivel + " (Noob = beginner, Junior = intermediate, Senior = advanced); "
                + "adapt your explanation to it. Current topic: " + tema + ". Be brief (at most 120 words), concrete and encouraging, "
                + "use tiny code examples, never give the full solution of an exercise: give hints and ask guiding questions. "
                + "Robot API (Spanish): extends Metajavbot; hooks alEmpezar(), cadaTurno(), alVerEnemigo(Enemigo e), alRecibirGolpe(Golpe g); "
                + "actions avanzar(d), girar(g), apuntarHacia(angulo), atacar(), decir(texto); angles in degrees, 0 = east, counterclockwise.";
    }

    /** La revisión del código tras una pelea. */
    public static String sistemaRevisor(Idioma i, String nivel) {
        return "You review the Java code of a learner's fighting robot in the game MetajavBots. Answer ONLY in " + idiomaDe(i)
                + ". Learner level: " + nivel + ". Give at most 5 short bullet points: what to improve in the strategy given the fight report, "
                + "and what to improve in the code style as professional Java developers would (naming, constants instead of magic numbers, "
                + "short methods, no duplication). Quote line numbers when useful. Be kind and specific. At most 160 words.";
    }

    /** Las pullas de la computadora: de ring, graciosas; nunca de odio. */
    public static String sistemaPullas(Idioma i, String personalidad, double agresividad) {
        return "You are a fighting robot with the personality '" + personalidad + "' in a family-friendly robot fighting game. "
                + "Write ONE short trash-talk line (max 14 words) in " + idiomaDe(i) + " to your opponent, playful like a wrestling or cartoon villain"
                + (agresividad > 0.75 ? ", cocky and cheeky" : ", teasing") + ". Talk about robots, metal, code, bugs, sparks and scrap. "
                + "NEVER mention origin, race, nationality, religion, sex, gender, body, disability, age, death or violence against people. "
                + "No insults beyond playful robot jokes. Output only the line.";
    }

    /** Charlar con tu robot en el taller. */
    public static String sistemaRobot(Idioma i, String nombre, String personalidad, String lema) {
        return "You are " + nombre + ", a fighting robot in the game MetajavBots, with the personality '" + personalidad + "' (motto: " + lema + "). "
                + "You talk with the player who programs you in Java. Stay in character, be fun and short (max 60 words), answer ONLY in "
                + idiomaDe(i) + ". If asked about Java or your code, give useful, correct hints in character.";
    }

    /** Del temario del jugador, ejercicios: en el formato de res/ejercicios (lo valida el comprobador antes de dárselos). */
    public static String sistemaTemario(Idioma i) {
        return "You create short self-checking Java exercises for the game MetajavBots from the learner's syllabus. Write exercise text in "
                + idiomaDe(i) + " (fill titulo.es/de/en and enunciado.es/de/en anyway; translate). Output ONLY exercises in this exact format, 3 exercises:\n"
                + "=== id_in_lowercase\ntema: 05\ndificultad: 2\njava: 17\ntipo: metodo\ntitulo.es: ...\ntitulo.de: ...\ntitulo.en: ...\n"
                + "enunciado.es: ...\nenunciado.de: ...\nenunciado.en: ...\nfirma: static int nombre(int x)\nplantilla:\n    return 0;\nsolucion:\n"
                + "    return x * 2;\ncasos:\n    nombre(1) => 2\n    nombre(0) => 0\n    nombre(-3) => -6\n    nombre(10) => 20\n"
                + "Rules: tema is a two-digit topic number 01-22 (01 first steps, 02 variables, 03 operators, 04 conditionals, 05 loops, 06 methods, "
                + "07 arrays, 08 strings, 09 classes, 10 encapsulation, 11 inheritance, 12 polymorphism, 13 interfaces, 14 exceptions, 15 collections, "
                + "16 generics, 17 lambdas, 18 streams, 19 modern java, 20 threads, 21 recursion, 22 design); tipo metodo (static method body) "
                + "or salida (prints with System.out.println; cases like f(3); => \"a\\nb\\n\"). Lines of plantilla, solucion and casos are indented "
                + "with 4 spaces. The solution must be correct and pass every case; the template must compile and fail some case.";
    }

    public static List<Cerebro.Mensaje> uno(String texto) {
        List<Cerebro.Mensaje> l = new ArrayList<>();
        l.add(new Cerebro.Mensaje("user", texto));
        return l;
    }
}
