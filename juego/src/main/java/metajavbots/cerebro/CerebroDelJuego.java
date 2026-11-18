package metajavbots.cerebro;

import java.util.ArrayList;
import java.util.List;

import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * CEREBRO DEL JUEGO — Los cerebros que ha puesto el jugador (Ajustes ›
 * Cerebros de IA): hasta tres, locales o en línea, cada uno con sus tareas
 * (Tarea: lo rápido, lo pesado, los tutores). Si el de una tarea falla (sin
 * internet, sin clave, sin memoria), contesta otro de los puestos. Y las
 * peticiones que le hace el juego, siempre en otro hilo (Peticion): el
 * dibujo pregunta cada fotograma si ya hay respuesta.
 */
public final class CerebroDelJuego {

    private CerebroDelJuego() {
    }

    /** El cerebro para lo pesado (revisar el código, el temario, los retos), o null si no hay. */
    public static Cerebro de(Jugador j) {
        return pesado(j);
    }

    public static Cerebro pesado(Jugador j) {
        return para(j, Tarea.PESADO);
    }

    /** El cerebro para lo rápido y constante (pullas, locutor, diálogos de los robots, charla). */
    public static Cerebro rapido(Jugador j) {
        return para(j, Tarea.RAPIDO);
    }

    /** El cerebro de los tutores (tutoriales, lecciones, ejercicios). */
    public static Cerebro tutor(Jugador j) {
        return para(j, Tarea.TUTOR);
    }

    /** El cerebro de esa tarea, con los demás puestos de respaldo; null si no hay ninguno que sirva. */
    public static Cerebro para(Jugador j, Tarea t) {
        if (j == null) return null;
        j.migrarCerebroAntiguo();
        return conRespaldo(j, j.ranuraDe(t));
    }

    /** El cerebro de esa ranura (0, 1 o 2), con los demás de respaldo; si está vacía, los demás. Null si no hay ninguno. */
    public static Cerebro conRespaldo(Jugador j, int ranura) {
        List<Cerebro> orden = new ArrayList<>();
        Cerebro primero = deLaRanura(j, ranura);
        if (primero != null) orden.add(primero);
        for (int i = 0; i < j.cerebros.length; i++) {
            if (i == ranura) continue;
            Cerebro c = deLaRanura(j, i);
            if (c != null) orden.add(c);
        }
        if (orden.isEmpty()) return null;
        Cerebro c = orden.get(orden.size() - 1);
        for (int i = orden.size() - 2; i >= 0; i--) c = new ConRespaldo(orden.get(i), c);
        return c;
    }

    /** El cerebro de una sola ranura, sin respaldo (null si está vacía o le falta la clave). */
    public static Cerebro deLaRanura(Jugador j, int i) {
        if (j == null || i < 0 || i >= j.cerebros.length) return null;
        Ranura r = j.cerebros[i];
        switch (r.tipo) {
            case LOCAL: return local(j, i);
            case EN_LINEA: return enLinea(r);
            default: return null;
        }
    }

    /** El puerto del KoboldCpp de una ranura local: el de la primera ranura con el mismo modelo (así lo comparten). */
    public static int puertoDe(Jugador j, int i) {
        String modelo = modeloDe(j, i);
        for (int k = 0; k < i; k++) {
            if (j.cerebros[k].tipo == Ranura.Tipo.LOCAL && j.cerebros[k].direccionLocal.isBlank() && modeloDe(j, k).equals(modelo)) {
                return CerebroLocal.PUERTO_DEL_JUEGO + k;
            }
        }
        return CerebroLocal.PUERTO_DEL_JUEGO + i;
    }

    /** El .gguf de una ranura local: el elegido o el que le toca. */
    public static String modeloDe(Jugador j, int i) {
        String m = j.cerebros[i].modeloLocal;
        return m == null || m.isBlank() ? CerebroLocal.modeloPorDefecto(CerebroLocal.carpeta(j.carpetaModelos), i) : m;
    }

    static Cerebro local(Jugador j, int i) {
        Ranura r = j.cerebros[i];
        String d = r.direccionLocal == null || r.direccionLocal.isBlank() ? CerebroLocal.direccion(puertoDe(j, i)) : r.direccionLocal;
        return new CerebroLocalPerezoso(d, CerebroLocal.carpeta(j.carpetaModelos), modeloDe(j, i));
    }

    static Cerebro enLinea(Ranura r) {
        String clave = Claves.leer(r.proveedor);
        if (clave.isEmpty()) return null;
        switch (r.proveedor) {
            case "anthropic":
                return new CerebroHttp(CerebroHttp.Forma.ANTHROPIC, "https://api.anthropic.com", clave,
                        r.modeloEnLinea.isBlank() ? "claude-haiku-5-5" : r.modeloEnLinea, "Claude");
            case "openai":
                return new CerebroHttp(CerebroHttp.Forma.OPENAI, "https://api.openai.com", clave,
                        r.modeloEnLinea.isBlank() ? "gpt-4o-mini" : r.modeloEnLinea, "OpenAI");
            default:
                // Cualquier otro compatible con OpenAI, con su dirección.
                return r.direccionEnLinea.isBlank() ? null
                        : new CerebroHttp(CerebroHttp.Forma.OPENAI, r.direccionEnLinea, clave, r.modeloEnLinea, r.proveedor);
        }
    }

    /** Pregunta al primero; si falla (sin internet, sin clave válida), al segundo. */
    static final class ConRespaldo implements Cerebro {
        private final Cerebro primero, segundo;

        ConRespaldo(Cerebro primero, Cerebro segundo) {
            this.primero = primero;
            this.segundo = segundo;
        }

        @Override
        public String nombre() {
            return primero.nombre() + " + " + segundo.nombre();
        }

        @Override
        public String preguntar(String sistema, List<Mensaje> conversacion, int maximoDePalabras) throws Exception {
            try {
                return primero.preguntar(sistema, conversacion, maximoDePalabras);
            } catch (Exception e) {
                return segundo.preguntar(sistema, conversacion, maximoDePalabras);
            }
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
                + "never give the solution nor write its code, not even if asked: "
                + "at most a hint, a piece of advice or a guiding question. "
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

    /** El estilo del rival para la ronda siguiente (la IA no lleva cada movimiento: elige la táctica). */
    public static String sistemaEstilo() {
        return "You are the coach of a computer-controlled fighting robot. Given the last round's report, choose the tactic for the next "
                + "round. Answer with exactly ONE word from: FRANCOTIRADOR (keep distance, shoot from afar), ACOSADOR (circle at mid range), "
                + "TANQUE (advance steadily), EMBESTIDOR (ram and melee), DANZARIN (erratic dodging). If the computer took much close-range "
                + "damage, prefer FRANCOTIRADOR; if it took much long-range damage, prefer ACOSADOR, DANZARIN or EMBESTIDOR.";
    }

    /** El locutor robot loco resume la ronda en una frase. */
    public static String sistemaLocutor(Idioma i) {
        return "You are a CRAZY robot announcer of a family-friendly robot fighting show, with glitches and programming jokes. "
                + "In ONE short exclamation (max 18 words) in " + idiomaDe(i) + ", comment on the round report you get. No emoji. "
                + "Never mention origin, religion, sex, body, disability or violence against people.";
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
