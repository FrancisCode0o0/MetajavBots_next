package metajavbots.tutor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * CONSEJO — Una meta del modo tutor sin IA (res/datos/consejos.txt): «que
 * vaya más rápido», «que apunte mejor»… con ayudas de menos a más: una
 * pregunta para que el jugador piense, una pista, el pseudocódigo y un trozo
 * pequeño de código en la API de su idioma (nunca el programa entero).
 */
public final class Consejo {

    public final String id;
    public final Trilingue titulo, pregunta, pista, pseudo;
    private final Map<Idioma, String> codigo;

    private Consejo(String id, Map<String, String> v) {
        this.id = id;
        this.titulo = trilingue(v.getOrDefault("titulo", id));
        this.pregunta = trilingue(v.getOrDefault("pregunta", ""));
        this.pista = trilingue(v.getOrDefault("pista", ""));
        this.pseudo = trilingue(v.getOrDefault("pseudo", ""));
        this.codigo = new HashMap<>();
        for (Idioma i : Idioma.values()) codigo.put(i, v.getOrDefault("codigo." + i.codigo(), "").replace("\\n", "\n"));
    }

    private static Trilingue trilingue(String s) {
        return metajavbots.textos.LectorRes.trilingue(s, false);
    }

    /** El trozo de código, en la API de ese idioma. */
    public String codigo(Idioma i) {
        return codigo.getOrDefault(i, "");
    }

    /** Cuántos niveles de ayuda hay: la pregunta, la pista, el pseudocódigo y un trozo pequeño de código. */
    public static final int NIVELES = 4;

    /** La ayuda de ese nivel (0 la pregunta, 1 la pista, 2 el pseudocódigo, 3 un trozo pequeño de código). */
    public String nivel(int n, Idioma i) {
        switch (n) {
            case 0: return pregunta.en(i);
            case 1: return pista.en(i);
            case 2: return pseudo.en(i);
            default: return codigo(i);
        }
    }

    private static List<Consejo> todos;

    /** Todas las metas, en el orden del archivo. */
    public static synchronized List<Consejo> todos() {
        if (todos == null) {
            List<Consejo> l = new ArrayList<>();
            try (InputStream in = Consejo.class.getResourceAsStream("/datos/consejos.txt")) {
                for (metajavbots.textos.LectorRes.Seccion s : metajavbots.textos.LectorRes.secciones(in, ':')) {
                    l.add(new Consejo(s.id(), s.valores()));
                }
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer res/datos/consejos.txt", e);
            }
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }
}
