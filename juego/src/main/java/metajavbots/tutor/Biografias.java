package metajavbots.tutor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import metajavbots.textos.Idioma;

/**
 * BIOGRAFÍAS — Las de los robots y las de los entrenadores (res/biografias/),
 * muy absurdas, en archivos de texto fáciles de retocar: [id] y una línea por
 * idioma. Lo que no esté, sale vacío.
 */
public final class Biografias {

    private static final Map<String, Map<String, String>> LEIDAS = new HashMap<>();

    private Biografias() {
    }

    /** La biografía de un robot (por el id de su modelo). */
    public static String deRobot(String modelo, Idioma i) {
        return de("robots", modelo, i);
    }

    /** La biografía de un entrenador. */
    public static String deEntrenador(String id, Idioma i) {
        return de("entrenadores", id, i);
    }

    private static synchronized String de(String archivo, String id, Idioma i) {
        Map<String, String> m = LEIDAS.computeIfAbsent(archivo, Biografias::leer);
        return m.getOrDefault(id + "." + i.codigo(), "");
    }

    private static Map<String, String> leer(String archivo) {
        Map<String, String> m = new HashMap<>();
        try (InputStream in = Biografias.class.getResourceAsStream("/biografias/" + archivo + ".txt")) {
            if (in == null) return m;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String id = "", linea;
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("#") || linea.isBlank()) continue;
                if (linea.startsWith("[")) id = linea.substring(1, linea.indexOf(']')).strip();
                else if (linea.length() > 3 && linea.charAt(2) == ':') m.put(id + "." + linea.substring(0, 2), linea.substring(3).strip());
            }
        } catch (Exception e) {
            // sin biografías: el juego sigue
        }
        return m;
    }
}
