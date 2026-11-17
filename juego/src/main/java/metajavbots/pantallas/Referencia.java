package metajavbots.pantallas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.textos.Idioma;

/**
 * REFERENCIA — Lo que sale en el buscador de la API: la API de los robots
 * (res/datos/api.txt, generada de las fachadas) y la referencia de Java
 * (res/datos/referencia.txt), cada entrada con su tema, su firma, su
 * explicación en tres idiomas y un ejemplo. Busca sin tildes, por palabras.
 */
public final class Referencia {

    public static final class Entrada {
        public final String id, tema, ejemplo;
        final Map<String, String> v;

        Entrada(Map<String, String> v) {
            this.v = v;
            id = v.getOrDefault("id", "");
            tema = v.getOrDefault("tema", "01");
            ejemplo = v.getOrDefault("ejemplo", "");
        }

        public String firma(Idioma i) {
            String f = v.get(i == Idioma.ES ? "firma" : "firma." + i.codigo());
            return f == null ? v.getOrDefault("firma", id) : f;
        }

        public String texto(Idioma i) {
            String t = v.get(i.codigo());
            return t == null ? v.getOrDefault("es", "") : t;
        }

        public boolean esDeLaApi() {
            return id.startsWith("api.") || id.startsWith("Enemigo.") || id.startsWith("Golpe.");
        }

        /** Lo que se escribe en el editor al pulsar «Insertar» (el nombre del método o de la palabra). */
        public String insertable(Idioma i) {
            String f = firma(i);
            int p = f.indexOf('(');
            if (p < 0) return "";
            String antes = f.substring(0, p).strip();
            String nombre = antes.substring(antes.lastIndexOf(' ') + 1);
            if (nombre.contains(".")) return "";
            return nombre + "(";
        }
    }

    private static List<Entrada> todas;

    private Referencia() {
    }

    public static synchronized List<Entrada> todas() {
        if (todas == null) {
            List<Entrada> l = new ArrayList<>();
            leer("/datos/api.txt", l);
            leer("/datos/referencia.txt", l);
            todas = Collections.unmodifiableList(l);
        }
        return todas;
    }

    private static void leer(String recurso, List<Entrada> l) {
        try (InputStream in = Referencia.class.getResourceAsStream(recurso)) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            Map<String, String> actual = null;
            String linea;
            String ultima = null;
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("#")) continue;
                if (linea.startsWith("===")) {
                    if (actual != null) l.add(new Entrada(actual));
                    actual = new LinkedHashMap<>();
                    actual.put("id", linea.substring(3).strip());
                    continue;
                }
                if (actual == null || linea.isBlank()) continue;
                int i = linea.indexOf(':');
                if (i > 0 && !linea.startsWith(" ")) {
                    ultima = linea.substring(0, i).strip();
                    actual.merge(ultima, linea.substring(i + 1).strip(), (a, b) -> a + "\n" + b);
                } else if (ultima != null) {
                    actual.merge(ultima, linea.strip(), (a, b) -> a + "\n" + b);
                }
            }
            if (actual != null) l.add(new Entrada(actual));
        } catch (IOException e) {
            // sin referencia
        }
    }

    static String sinTildes(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Las que casan con lo buscado (todas las palabras), primero las que lo tienen en el nombre; y de un tema, si se da. */
    public static List<Entrada> buscar(String que, String tema, Idioma idioma) {
        String[] palabras = sinTildes(que.strip()).split("\\s+");
        List<Entrada> l = new ArrayList<>();
        List<Entrada> segundas = new ArrayList<>();
        for (Entrada e : todas()) {
            if (tema != null && !e.tema.equals(tema)) continue;
            String nombre = sinTildes(e.id + " " + e.firma(idioma));
            String todo = nombre + " " + sinTildes(e.texto(idioma) + " " + e.ejemplo);
            boolean enNombre = true, enTodo = true;
            for (String p : palabras) {
                if (p.isEmpty()) continue;
                if (!nombre.contains(p)) enNombre = false;
                if (!todo.contains(p)) enTodo = false;
            }
            if (enNombre) l.add(e);
            else if (enTodo) segundas.add(e);
        }
        l.addAll(segundas);
        return l;
    }
}
