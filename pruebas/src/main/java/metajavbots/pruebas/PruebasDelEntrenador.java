package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.sonido.Timbre;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConsejosDelTaller;
import metajavbots.tutor.Entrenador;
import metajavbots.tutor.LecturaDelRival;

/**
 * El entrenador flotante: sus explicaciones de serie (res/conceptos) y cómo encuentra lo que se le pregunta,
 * los errores de javac que explica, los tips del taller, lo que lee del rival en la pelea, el hospital y las voces.
 */
final class PruebasDelEntrenador {

    private PruebasDelEntrenador() {
    }

    /** Lo que se importa al compilar un ejemplo (los ejemplos son trocitos: dan por hechos los import de siempre). */
    private static final String IMPORTS = "import java.util.*;\nimport java.util.function.*;\nimport java.util.stream.*;\nimport java.util.concurrent.*;\n"
            + "import java.util.concurrent.atomic.*;\nimport java.util.concurrent.locks.*;\nimport metajavbots.api.*;\n"
            + "import static metajavbots.junit.Assertions.*;\nimport metajavbots.junit.*;\n";

    /** Los que solo se enseñan, sin compilarse solos (necesitan otros archivos o módulos). */
    private static final Set<String> ILUSTRATIVOS = Set.of("modulos", "paquete_import");

    static void apuntar() {
        caso("entrenador: los conceptos, de cero a senior, en tres idiomas, con pseudocódigo, ejemplo y relacionados que existen", () -> {
            List<Concepto> todos = Concepto.todos();
            cierto(todos.size() >= 130, "al menos 130 conceptos: " + todos.size());
            Set<String> ids = new HashSet<>();
            int[] porNivel = new int[6];
            for (Concepto c : todos) {
                cierto(ids.add(c.id), "id repetido: " + c.id);
                cierto(c.nivel >= 1 && c.nivel <= 5, c.id + ": nivel 1-5");
                porNivel[c.nivel]++;
                for (Idioma i : Idioma.values()) {
                    cierto(!c.titulo.en(i).isBlank(), c.id + ": título en " + i);
                    cierto(c.parrafos(i).size() >= 2, c.id + ": explicación en " + i);
                    cierto(!c.claves(i).isEmpty(), c.id + ": claves en " + i);
                    cierto(!c.pseudo(i).isBlank(), c.id + ": pseudocódigo en " + i);
                    // El ejemplo, en el idioma del jugador: su comentario y no los tres.
                    for (String l : c.ejemplo(i).split("\n")) {
                        int com = l.indexOf("//");
                        cierto(com < 0 || !l.substring(com).contains(" || "), c.id + ": comentario sin elegir en " + i + ": " + l);
                    }
                }
                cierto(c.guia > 0 || !c.ejemplo.isBlank(), c.id + ": ejemplo");
                cierto(c.guia <= metajavbots.pantallas.GuiaDeCerebros.PAGINAS, c.id + ": página de la guía");
            }
            for (int n = 1; n <= 5; n++) cierto(porNivel[n] >= 10, "nivel " + n + " con conceptos: " + porNivel[n]);
            for (Concepto c : todos) for (String v : c.ver) cierto(Concepto.de(v) != null, c.id + " → «ver» que no existe: " + v);
            System.out.println("            " + todos.size() + " conceptos (por nivel: " + porNivel[1] + ", " + porNivel[2] + ", " + porNivel[3] + ", "
                    + porNivel[4] + ", " + porNivel[5] + ")");
        });

        caso("entrenador: el ejemplo de cada concepto se ve con la API y los comentarios del idioma del jugador", () -> {
            Concepto v = Concepto.de("variable");
            cierto(v.ejemplo(Idioma.ES).contains("alRecibirGolpe") && v.ejemplo(Idioma.ES).contains("// fuera: dura toda la ronda"), "español");
            cierto(v.ejemplo(Idioma.DE).contains("beiTreffer") && v.ejemplo(Idioma.DE).contains("// außen: hält die ganze Runde"), "alemán");
            cierto(v.ejemplo(Idioma.EN).contains("onHit") && v.ejemplo(Idioma.EN).contains("// outside: lasts the whole round"), "inglés");
        });

        caso("entrenador: los ejemplos de los conceptos compilan (con los import de siempre)", () -> {
            if (!Compilador.hayCompilador()) return;
            List<String> malos = new ArrayList<>();
            for (Concepto c : Concepto.todos()) {
                if (c.ejemplo.isBlank() || ILUSTRATIVOS.contains(c.id) || soloComentarios(c.ejemplo)) continue;
                String fallo = compilarEjemplo(c.ejemplo);
                if (fallo != null) malos.add(c.id + ": " + fallo);
            }
            for (String m : malos) System.out.println("            ✗ " + m);
            igual(0, malos.size(), "ejemplos que no compilan");
        });

        caso("entrenador: encuentra el concepto por lo que pregunta quien no sabe nada, en cualquier idioma", () -> {
            String[][] preguntas = {
                {"¿qué es una variable?", "variable", "ES"},
                {"cómo se ejecuta un programa", "programa", "ES"},
                {"what is a class", "clase", "EN"},
                {"was ist ein Objekt", "objeto", "DE"},
                {"Vererbung", "herencia", "DE"},
                {"para qué sirve un bucle for", "for", "ES"},
                {"hilos virtuales", "hilos_virtuales", "ES"},
                {"virtual threads", "hilos_virtuales", "EN"},
                {"condición de carrera", "condicion_de_carrera", "ES"},
                {"recolector de basura", "recolector_de_basura", "ES"},
                {"? extends", "comodines", "ES"},
                {"try catch", "try_catch", "ES"},
                {"bytecode", "bytecode", "EN"},
                {"¿por qué pierdo?", "por_que_pierdo", "ES"},
                {"cómo pongo la clave de gemini", "cerebro_en_linea", "ES"},
                {"koboldcpp", "cerebro_local", "DE"},
                {"where can I see my grades", "ver_notas", "EN"},
                {"qué es una clase abstracta", "clase_abstracta", "ES"},
                {"modelo de memoria", "modelo_de_memoria", "ES"},
            };
            for (String[] q : preguntas) {
                List<Concepto> l = Concepto.buscar(q[0], Idioma.valueOf(q[2]), 3);
                cierto(!l.isEmpty(), "algo para «" + q[0] + "»");
                igual(q[1], l.get(0).id, "«" + q[0] + "»");
            }
            cierto(Concepto.buscar("zzzz qqqq", Idioma.ES, 3).isEmpty(), "nada para lo que no se parece a nada");
        });

        caso("entrenador: cada error de javac explicado tiene un concepto detrás", () -> {
            igual("sentencia", Concepto.delError("compiler.err.expected").id, "falta «;»");
            igual("bloque", Concepto.delError("compiler.err.premature.eof").id, "falta «}»");
            igual("variable", Concepto.delError("compiler.err.cant.resolve.location").id, "símbolo no encontrado");
            igual("static", Concepto.delError("compiler.err.non-static.cant.be.ref").id, "static");
            igual("char", Concepto.delError("compiler.err.unclosed.char.lit").id, "char");
            igual("llamar_metodo", Concepto.delError("compiler.err.dot.class.expected").id, "no «expected» a secas");
            igual("error_de_compilacion", Concepto.delError("compiler.err.no.se.que").id, "lo desconocido");
            // Todos los de res/textos/errores.txt dan con un concepto que existe.
            try (var in = PruebasDelEntrenador.class.getResourceAsStream("/textos/errores.txt")) {
                for (String l : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    l = l.strip();
                    if (l.startsWith("[") && l.endsWith("]")) cierto(Concepto.delError(l.substring(1, l.length() - 1)) != null, l);
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });

        caso("entrenador: los tips del taller ven lo que le pasa al código (y siempre tiene alguno)", () -> {
            String malo = "public class MiRobot extends Metajavbot {\n    public void cadaTurno() {\n        int vueltas = 0;\n        vueltas++;\n"
                    + "        while (true) {\n            avanzar(10);\n        }\n    }\n}\n";
            List<String> ids = ids(ConsejosDelTaller.de(malo));
            for (String id : new String[]{"contador_dentro", "while_true", "sin_radar", "sin_apuntar", "override", "sin_golpes", "paredes"})
                cierto(ids.contains(id), "ve «" + id + "»: " + ids);
            String bueno = "public class MiRobot extends Metajavbot {\n    private int vueltas;\n\n    @Override\n    public void cadaTurno() {\n"
                    + "        vueltas++;\n        avanzar(5);\n    }\n\n    @Override\n    public void alVerEnemigo(Enemigo e) {\n"
                    + "        apuntarHacia(e.angulo());\n        atacar();\n    }\n\n    @Override\n    public void alRecibirGolpe(Golpe g) {\n        girar(90);\n    }\n\n"
                    + "    @Override\n    public void alChocarConPared() {\n        girar(180);\n    }\n}\n";
            for (String id : ids(ConsejosDelTaller.de(bueno))) cierto(id.startsWith("general_"), "nada que señalar al bueno: " + id);
            cierto(ids(ConsejosDelTaller.de("")).contains("vacio"), "el vacío");
            cierto(ids(ConsejosDelTaller.de("class A { void f() { if (e.nombre() == \"Eclipse\") {} } }")).contains("string_igual"), "== con textos");
            // En alemán y en inglés, con su API.
            String de = "public class MeinRoboter extends MetajavbotDe {\n    @Override\n    public void jedenZug() {\n        vorwaerts(5);\n    }\n"
                    + "    @Override\n    public void beiSicht(Gegner g) {\n        zielenNach(g.winkel());\n    }\n}\n";
            List<String> idsDe = ids(ConsejosDelTaller.de(de));
            cierto(!idsDe.contains("sin_radar") && !idsDe.contains("sin_apuntar") && !idsDe.contains("override"), "API alemana: " + idsDe);
            for (ConsejosDelTaller.Tip t : ConsejosDelTaller.de(malo)) {
                cierto(T.existe("entrenador.tip." + t.id()), "texto del tip " + t.id());
                cierto(Concepto.de(t.concepto()) != null, "concepto del tip " + t.id() + ": " + t.concepto());
            }
        });

        caso("entrenador: en la pelea lee al rival marcado (rodea, se acerca, quieto, vuela) y tiene consejo para cada cosa", () -> {
            // Un rival que da vueltas a mi alrededor en contra de las agujas del reloj: me rodea por la izquierda.
            LecturaDelRival l = new LecturaDelRival();
            for (int k = 0; k < 90; k++) {
                double a = Math.toRadians(k * 2.0);
                l.observar(500, 350, 0, 500 + 200 * Math.cos(a), 350 + 200 * Math.sin(a), 4, false, 0, 0, 1000, 700);
            }
            cierto(l.lecturas().contains("rodea_izquierda"), "izquierda: " + l.lecturas());
            l.olvidar();
            for (int k = 0; k < 90; k++) {
                double a = Math.toRadians(-k * 2.0);
                l.observar(500, 350, 0, 500 + 200 * Math.cos(a), 350 + 200 * Math.sin(a), 4, false, 0, 0, 1000, 700);
            }
            cierto(l.lecturas().contains("rodea_derecha"), "derecha: " + l.lecturas());
            // Uno que se me echa encima y me pega más que yo a él.
            l.olvidar();
            for (int k = 0; k < 90; k++) l.observar(200, 350, 0, 800 - k * 5, 350, 5, false, k * 0.6, k * 0.1, 1000, 700);
            cierto(l.lecturas().contains("se_acerca") && l.lecturas().contains("te_gana"), "se acerca y gana: " + l.lecturas());
            // Uno quieto que vuela, y al que no apunto (miro al lado contrario).
            l.olvidar();
            for (int k = 0; k < 90; k++) l.observar(200, 350, 180, 500, 350, 0, true, 0, 0, 1000, 700);
            cierto(l.lecturas().containsAll(List.of("quieto", "vuela", "no_apuntas")), "quieto, vuela y no apunto: " + l.lecturas());
            cierto(new LecturaDelRival().lecturas().isEmpty(), "sin ver bastante, nada");
            for (String id : new String[]{"nada", "rodea_izquierda", "rodea_derecha", "se_acerca", "se_aleja", "lejos", "quieto", "rapido", "vuela",
                "pared", "no_apuntas", "te_gana", "le_ganas"}) {
                cierto(T.existe("entrenador.rival." + id) && T.existe("entrenador.rival." + id + ".trozo"), "consejo y trozo de " + id);
            }
        });

        caso("entrenador: cada entrenador con su voz; contra los jefes, en el hospital por causas que cambian", () -> {
            Set<Timbre> timbres = new HashSet<>();
            for (Entrenador e : Entrenador.todos()) timbres.add(e.timbre);
            igual(Entrenador.todos().size(), timbres.size(), "una voz distinta por entrenador");
            cierto(!timbres.contains(Timbre.LOCUTOR), "ninguno con la del locutor");
            igual(new Timbre(0.85, 0.9, 0.7), Timbre.de("0.85 0.9 0.7"), "se lee");
            igual(Timbre.LOCUTOR, Timbre.de("tonterías"), "lo mal escrito, el del locutor");
            float[] voz = new float[4410];
            for (int i = 0; i < voz.length; i++) voz[i] = (float) Math.sin(i * 0.1);
            cierto(metajavbots.sonido.VozPiper.robotico(voz, 0).length == voz.length, "el efecto no cambia la duración");
            Set<String> causas = new HashSet<>();
            for (long s = 0; s < 40; s++) causas.add(metajavbots.pantallas.Hospital.causa(s));
            cierto(causas.size() >= 5, "causas que cambian: " + causas.size());
            for (Idioma i : Idioma.values()) {
                T.idioma = i;
                igual(T.t("entrenador.hospital.causas").split(";").length, 10, "diez causas en " + i);
                cierto(T.t("entrenador.milagro.frases").split(";").length >= 4, "milagros en " + i);
            }
            T.idioma = Idioma.ES;
        });

        caso("entrenador: la guía con dibujos tiene todas sus páginas en los tres idiomas", () -> {
            for (Idioma i : Idioma.values()) {
                T.idioma = i;
                for (int n = 1; n <= metajavbots.pantallas.GuiaDeCerebros.PAGINAS; n++) {
                    for (String k : new String[]{"corto", "titulo", "pasos"}) cierto(T.existe("guia." + n + "." + k), "guia." + n + "." + k);
                    cierto(T.t("guia." + n + ".pasos").split("\n").length >= 3, "pasos de la página " + n + " en " + i);
                }
            }
            T.idioma = Idioma.ES;
        });
    }

    private static List<String> ids(List<ConsejosDelTaller.Tip> l) {
        List<String> r = new ArrayList<>();
        for (ConsejosDelTaller.Tip t : l) r.add(t.id());
        return r;
    }

    private static boolean soloComentarios(String ejemplo) {
        for (String l : ejemplo.split("\n")) if (!l.isBlank() && !l.strip().startsWith("//")) return false;
        return true;
    }

    /**
     * Compila un ejemplo: si declara sus propias clases, tal cual (sin «public» delante, para que quepan en un
     * archivo); si es un trozo de robot, dentro de «class MiRobot extends Metajavbot»; y si son sentencias sueltas,
     * dentro de un método. Devuelve el primer error, o null si compila de alguna de esas maneras.
     */
    private static String compilarEjemplo(String ejemplo) {
        String sinPublic = ejemplo.replaceAll("(?m)^public\\s+(?=(final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*(class|interface|record|enum|@interface)\\b)", "");
        boolean conTipos = java.util.regex.Pattern.compile("(?m)^(@\\w+(\\([^)]*\\))?\\s+)*(final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*"
                + "(class|interface|record|enum|@interface)\\s").matcher(sinPublic).find();
        String[] intentos = conTipos
                ? new String[]{IMPORTS + sinPublic + "\nclass MiRobotDePrueba extends Metajavbot {\n}\n"}
                : new String[]{IMPORTS + "class MiRobot extends Metajavbot {\n" + ejemplo + "\n}\n",
                    IMPORTS + "class MiRobot extends Metajavbot {\n    void ejemplo() throws Exception {\n" + ejemplo + "\n    }\n}\n"};
        String primero = null;
        for (String fuente : intentos) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("Ejemplo", fuente);
            Compilador.Resultado r = Compilador.compilar(m, false, Vigilante.Modo.CONFIANZA, Locale.ROOT);
            if (r.bien) return null;
            if (primero == null && !r.graves().isEmpty()) {
                Compilador.Fallo f = r.graves().get(0);
                int linea = (int) f.linea - IMPORTS.split("\n").length;
                primero = "línea " + linea + ": " + f.mensaje;
            }
        }
        return primero == null ? "no compila" : primero;
    }
}
