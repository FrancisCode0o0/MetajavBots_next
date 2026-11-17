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
    }

    public Ejercicio ejercicio(String id) {
        return porId.get(id);
    }

    public List<Ejercicio> todos() {
        return Collections.unmodifiableList(new ArrayList<>(porId.values()));
    }

    /** Los de un tema, de más fácil a más difícil. */
    public List<Ejercicio> deTema(String tema) {
        List<Ejercicio> l = new ArrayList<>();
        for (Ejercicio e : porId.values()) if (e.tema.equals(tema)) l.add(e);
        l.sort((a, b) -> Integer.compare(a.dificultad, b.dificultad));
        return l;
    }

    public List<String> avisos() {
        return Collections.unmodifiableList(avisos);
    }
}
