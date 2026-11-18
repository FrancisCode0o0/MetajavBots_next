package metajavbots.progreso;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;

/**
 * PREGUNTAS DE LA IA — Con cerebro, la prueba de nivel también lleva preguntas
 * que inventa la IA («¿Qué escribe este código?»), pero comprobadas antes,
 * como las plantillas: el juego compila y ejecuta cada código y la respuesta
 * buena es la que escribe de verdad (no la que diga la IA). Las tres malas de
 * la IA solo valen si son distintas entre sí y de la buena; si no, la
 * pregunta se tira. Tampoco valen las que no compilan, tardan o escriben más
 * de una línea.
 */
public final class PreguntasDeLaIa {

    private PreguntasDeLaIa() {
    }

    /** Lo que se le pide al cerebro (en inglés: los modelos lo siguen mejor). */
    public static String sistema() {
        return "You write multiple-choice Java questions «What does this code print?» for a level test, from beginner to senior. Output ONLY "
                + "blocks in this exact format, 6 of them, two for each level you are asked for:\n=== pregunta\nnivel: JUNIOR_2\narriba:\n"
                + "    static int f(int x) { return x * 2; }\ncuerpo:\n    System.out.println(f(3) + 1);\nmalas: 6 | 8 | 4\n"
                + "Rules: nivel is one of NOOB_2, NOOB_4, JUNIOR_2, JUNIOR_4, SENIOR_1, SENIOR_2. «arriba» holds static methods or static nested "
                + "classes (can be empty); «cuerpo» holds statements that print exactly ONE line with System.out.println. Lines of arriba and "
                + "cuerpo are indented with 4 spaces. Use only java.lang and java.util (fully qualified, e.g. java.util.List.of). «malas» are three "
                + "plausible WRONG answers (typical mistakes: off-by-one, integer division, == on strings, wrong order), separated by « | ». "
                + "Senior questions: generics, streams, records, switch expressions, overloading vs overriding, equals/hashCode, immutability.";
    }

    /** Lo que se le pregunta: los niveles que hacen falta. */
    public static String peticion(List<Etiqueta> niveles) {
        List<String> l = new ArrayList<>();
        for (Etiqueta e : niveles) l.add(e.name());
        return "Levels: " + String.join(", ", l);
    }

    /** Los bloques de la respuesta, como mapas campo → valor (los de «arriba» y «cuerpo», con sus líneas). */
    static List<Map<String, String>> bloques(String respuesta) {
        List<Map<String, String>> l = new ArrayList<>();
        Map<String, String> v = null;
        String campo = null;
        for (String linea : respuesta.replace("\r", "").split("\n")) {
            if (linea.startsWith("=== ")) {
                v = new java.util.LinkedHashMap<>();
                l.add(v);
                campo = null;
            } else if (v != null && linea.startsWith("    ") && campo != null) {
                v.merge(campo, linea.substring(4), (a, b) -> a + "\n" + b);
            } else if (v != null && linea.contains(":") && !linea.startsWith(" ")) {
                int i = linea.indexOf(':');
                campo = linea.substring(0, i).strip();
                String valor = linea.substring(i + 1).strip();
                if (!valor.isEmpty()) v.put(campo, valor);
                else v.putIfAbsent(campo, "");
            }
        }
        return l;
    }

    /**
     * Las preguntas buenas de la respuesta de la IA: se ejecuta cada código (todos a la vez, en un solo programa, cada uno en su
     * clase) y se queda la salida de verdad como respuesta buena.
     */
    public static List<GeneradorDePreguntas.Generada> comprobar(String respuesta) {
        List<GeneradorDePreguntas.Generada> buenas = new ArrayList<>();
        List<Map<String, String>> bs = bloques(respuesta);
        for (Map<String, String> v : bs) {
            Etiqueta nivel;
            try {
                nivel = Etiqueta.valueOf(v.getOrDefault("nivel", "").strip());
            } catch (IllegalArgumentException e) {
                continue;
            }
            String arriba = v.getOrDefault("arriba", "").strip(), cuerpo = v.getOrDefault("cuerpo", "").strip();
            if (cuerpo.isEmpty()) continue;
            String salida = ejecutar(arriba, cuerpo);
            if (salida == null || salida.isBlank() || salida.contains("\n")) continue;
            Set<String> malas = new LinkedHashSet<>();
            for (String m : v.getOrDefault("malas", "").split("\\|")) if (!m.isBlank() && !m.strip().equals(salida)) malas.add(m.strip());
            if (malas.size() < 3) continue;
            buenas.add(new GeneradorDePreguntas.Generada(nivel, arriba, cuerpo, salida, new ArrayList<>(malas).subList(0, 3)));
        }
        return buenas;
    }

    /** Lo que escribe ese código (una línea, sin el salto del final), o null si no compila o falla. */
    static String ejecutar(String arriba, String cuerpo) {
        String ej = "=== ia\ntema: 01\ndificultad: 1\njava: 21\ntipo: salida\ntitulo.es: x\ntitulo.de: x\ntitulo.en: x\nenunciado.es: x\n"
                + "enunciado.de: x\nenunciado.en: x\nfirma: static void q()\nplantilla:\n    return;\nsolucion:\n    return;\ncasos:\n    C.q(); => \"?\"\n";
        try {
            Ejercicio e = Ejercicio.leer(new ByteArrayInputStream(ej.getBytes(StandardCharsets.UTF_8)), "ia").get(0);
            String codigo = "static class C {\n" + arriba + "\nstatic void q() {\n" + cuerpo + "\n}\n}\n";
            Comprobador.Informe inf = Comprobador.comprobar(e, codigo, Locale.ROOT);
            if (!inf.compila() || inf.casos.isEmpty()) return null;
            String s = inf.casos.get(0).obtenido;
            // Lo que se obtuvo, como se enseña: entre comillas y escapado («\n», «\"»); si lanzó algo («!…»), no vale.
            if (s == null || s.length() < 2 || !s.startsWith("\"") || !s.endsWith("\"")) return null;
            s = s.substring(1, s.length() - 1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            return s.endsWith("\n") ? s.substring(0, s.length() - 1) : s;
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }
}
