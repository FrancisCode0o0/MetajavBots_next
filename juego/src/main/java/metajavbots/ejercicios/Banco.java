package metajavbots.ejercicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BANCO — Todos los ejercicios del juego. Los archivos están en
 * res/ejercicios/ y se nombran en res/ejercicios/indice.txt (uno por línea;
 * las pruebas avisan si falta alguno en el índice).
 */
public final class Banco {

    private static Banco delJuego;

    private final Map<String, Ejercicio> porId = new LinkedHashMap<>();
    private final List<String> avisos = new ArrayList<>();

    public static synchronized Banco delJuego() {
        if (delJuego == null) delJuego = leer();
        return delJuego;
    }

    private static Banco leer() {
        Banco b = new Banco();
        for (String archivo : indice("/ejercicios/indice.txt")) {
            try (InputStream in = Banco.class.getResourceAsStream("/ejercicios/" + archivo)) {
                if (in == null) {
                    b.avisos.add("falta " + archivo);
                    continue;
                }
                for (Ejercicio e : Ejercicio.leer(in, archivo)) b.anadir(e);
            } catch (IOException e) {
                b.avisos.add(archivo + ": " + e);
            }
        }
        return b;
    }

    /** Las líneas de un índice (sin comentarios ni vacías). */
    public static List<String> indice(String recurso) {
        List<String> l = new ArrayList<>();
        try (InputStream in = Banco.class.getResourceAsStream(recurso)) {
            if (in == null) return l;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = r.readLine()) != null) {
                linea = linea.strip();
                if (!linea.isEmpty() && !linea.startsWith("#")) l.add(linea);
            }
        } catch (IOException e) {
            // sin índice, sin ejercicios
        }
        return l;
    }

    void anadir(Ejercicio e) {
        if (porId.containsKey(e.id)) avisos.add("id repetido: " + e.id + " (" + e.archivo + ")");
        porId.put(e.id, e);
        porTema.clear();
        todosCache = null;
    }

    /** Los de cada tema, ya ordenados (las pantallas los piden en cada fotograma). */
    private final java.util.Map<String, List<Ejercicio>> porTema = new java.util.HashMap<>();
    private List<Ejercicio> todosCache;

    public Ejercicio ejercicio(String id) {
        return porId.get(id);
    }

    public synchronized List<Ejercicio> todos() {
        if (todosCache == null) todosCache = Collections.unmodifiableList(new ArrayList<>(porId.values()));
        return todosCache;
    }

    /** Los de un tema, de más fácil a más difícil. */
    public synchronized List<Ejercicio> deTema(String tema) {
        return porTema.computeIfAbsent(tema, t -> {
            List<Ejercicio> n = new ArrayList<>();
            for (Ejercicio e : porId.values()) if (e.tema.equals(t)) n.add(e);
            n.sort((a, b) -> Integer.compare(a.dificultad, b.dificultad));
            return Collections.unmodifiableList(n);
        });
    }

    public List<String> avisos() {
        return Collections.unmodifiableList(avisos);
    }
}
