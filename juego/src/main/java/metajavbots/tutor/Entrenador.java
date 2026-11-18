package metajavbots.tutor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * ENTRENADOR — Uno de los entrenadores del modo tutor (res/datos/entrenadores.txt):
 * parodias, con nombres y diseños propios, de los de las películas de combate.
 * Cada uno con su papel, sus colores, lo que se le dibuja en la cabeza (gorra,
 * barba, gafas…), sus muletillas y cómo habla (para el cerebro de IA). Su
 * biografía, en res/biografias/entrenadores.txt.
 */
public final class Entrenador {

    public final String id;
    public final Trilingue nombre, papel;
    public final int cara, ojos, detalle;
    public final List<String> rasgos;
    public final String estilo;
    /** Su voz: el tono, la velocidad y cuánto suena a robot (cada uno distinto, aunque Piper tenga una voz por idioma). */
    public final metajavbots.sonido.Timbre timbre;
    private final Map<Idioma, List<String>> frases;

    private Entrenador(String id, Map<String, String> v) {
        this.id = id;
        this.nombre = trilingue(v.getOrDefault("nombre", id));
        this.papel = trilingue(v.getOrDefault("papel", ""));
        String[] c = v.getOrDefault("colores", "888888 FFFFFF 333333").split("\\s+");
        this.cara = Integer.parseInt(c[0], 16);
        this.ojos = Integer.parseInt(c.length > 1 ? c[1] : "FFFFFF", 16);
        this.detalle = Integer.parseInt(c.length > 2 ? c[2] : "333333", 16);
        this.rasgos = List.of(v.getOrDefault("rasgos", "").trim().split("\\s+"));
        this.estilo = v.getOrDefault("estilo", "a friendly coach");
        this.timbre = metajavbots.sonido.Timbre.de(v.get("voz"));
        this.frases = new LinkedHashMap<>();
        for (Idioma i : Idioma.values()) {
            List<String> l = new ArrayList<>();
            for (String f : v.getOrDefault("frases." + i.codigo(), "").split(";")) if (!f.isBlank()) l.add(f.strip());
            frases.put(i, l);
        }
    }

    private static Trilingue trilingue(String s) {
        String[] p = s.split("\\|");
        return new Trilingue(p[0].strip(), p.length > 1 ? p[1].strip() : p[0].strip(), p.length > 2 ? p[2].strip() : p[0].strip());
    }

    /** ¿Se le dibuja ese rasgo (gorra, barba…)? */
    public boolean tiene(String rasgo) {
        return rasgos.contains(rasgo);
    }

    /** Una de sus muletillas (n: cuál; da la vuelta). */
    public String frase(Idioma i, int n) {
        List<String> l = frases.get(i);
        return l == null || l.isEmpty() ? "" : l.get(Math.floorMod(n, l.size()));
    }

    private static List<Entrenador> todos;

    /** Todos, en el orden del archivo. */
    public static synchronized List<Entrenador> todos() {
        if (todos == null) {
            List<Entrenador> l = new ArrayList<>();
            try (InputStream in = Entrenador.class.getResourceAsStream("/datos/entrenadores.txt")) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String id = null, linea;
                Map<String, String> v = new LinkedHashMap<>();
                while ((linea = r.readLine()) != null) {
                    if (linea.startsWith("#") || linea.isBlank()) continue;
                    if (linea.startsWith("[")) {
                        if (id != null) l.add(new Entrenador(id, v));
                        id = linea.substring(1, linea.indexOf(']')).strip();
                        v = new LinkedHashMap<>();
                    } else if (linea.contains("=")) {
                        int i = linea.indexOf('=');
                        v.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
                    }
                }
                if (id != null) l.add(new Entrenador(id, v));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer res/datos/entrenadores.txt", e);
            }
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }

    /** El de ese id (o el primero). */
    public static Entrenador de(String id) {
        for (Entrenador e : todos()) if (e.id.equals(id)) return e;
        return todos().get(0);
    }
}
