package metajavbots.tutor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * CONSEJO — Una meta del modo tutor sin IA (res/datos/consejos.txt): «que
 * vaya más rápido», «que apunte mejor»… con tres pistas de menos a más: una
 * pregunta para que el jugador piense, una ayuda concreta y, solo si la pide,
 * un trozo de código en la API de su idioma.
 */
public final class Consejo {

    public final String id;
    public final Trilingue titulo, pregunta, pista;
    private final Map<Idioma, String> codigo;

    private Consejo(String id, Map<String, String> v) {
        this.id = id;
        this.titulo = trilingue(v.getOrDefault("titulo", id));
        this.pregunta = trilingue(v.getOrDefault("pregunta", ""));
        this.pista = trilingue(v.getOrDefault("pista", ""));
        this.codigo = new HashMap<>();
        for (Idioma i : Idioma.values()) codigo.put(i, v.getOrDefault("codigo." + i.codigo(), "").replace("\\n", "\n"));
    }

    private static Trilingue trilingue(String s) {
        String[] p = s.split("\\|");
        return new Trilingue(p[0].strip(), p.length > 1 ? p[1].strip() : "", p.length > 2 ? p[2].strip() : "");
    }

    /** El trozo de código, en la API de ese idioma. */
    public String codigo(Idioma i) {
        return codigo.getOrDefault(i, "");
    }

    /** La pista de ese nivel (0 la pregunta, 1 la ayuda, 2 el código). */
    public String nivel(int n, Idioma i) {
        switch (n) {
            case 0: return pregunta.en(i);
            case 1: return pista.en(i);
            default: return codigo(i);
        }
    }

    private static List<Consejo> todos;

    /** Todas las metas, en el orden del archivo. */
    public static synchronized List<Consejo> todos() {
        if (todos == null) {
            List<Consejo> l = new ArrayList<>();
            try (InputStream in = Consejo.class.getResourceAsStream("/datos/consejos.txt")) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String id = null, linea;
                Map<String, String> v = new HashMap<>();
                while ((linea = r.readLine()) != null) {
                    if (linea.startsWith("#") || linea.isBlank()) continue;
                    if (linea.startsWith("[")) {
                        if (id != null) l.add(new Consejo(id, v));
                        id = linea.substring(1, linea.indexOf(']')).strip();
                        v = new HashMap<>();
                    } else if (linea.contains(":")) {
                        int i = linea.indexOf(':');
                        v.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
                    }
                }
                if (id != null) l.add(new Consejo(id, v));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer res/datos/consejos.txt", e);
            }
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }
}
