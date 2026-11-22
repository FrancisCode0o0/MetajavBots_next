package metajavbots.textos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * T — Los textos de la interfaz en los tres idiomas (res/textos/ui.txt, una
 * línea por texto: «clave = español | Deutsch | English»). T.t("menu.pelear")
 * da el del idioma elegido; T.f(...) rellena los huecos {0}, {1}…
 * Las pruebas comprueban que cada clave que usa el código esté en los tres.
 */
public final class T {

    private static final Map<String, Trilingue> TEXTOS = new LinkedHashMap<>();
    public static Idioma idioma = Idioma.ES;

    static {
        try (InputStream in = T.class.getResourceAsStream("/textos/ui.txt")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String linea;
                while ((linea = r.readLine()) != null) {
                    if (linea.isBlank() || linea.strip().startsWith("#")) continue;
                    int i = linea.indexOf('=');
                    if (i <= 0) continue;
                    String[] p = linea.substring(i + 1).split("\\|", -1);
                    TEXTOS.put(linea.substring(0, i).strip(), new Trilingue(
                            p[0].strip().replace("\\n", "\n"),
                            p.length > 1 ? p[1].strip().replace("\\n", "\n") : "",
                            p.length > 2 ? p[2].strip().replace("\\n", "\n") : ""));
                }
            }
        } catch (IOException e) {
            // sin textos: se ven las claves
        }
    }

    private T() {
    }

    public static String t(String clave) {
        return en(clave, idioma);
    }

    /** El texto de esa clave en un idioma concreto (sin depender de T.idioma). */
    public static String en(String clave, Idioma id) {
        Trilingue t = TEXTOS.get(clave);
        return t == null ? clave : t.en(id == null ? Idioma.ES : id);
    }

    public static String f(String clave, Object... huecos) {
        String s = t(clave);
        for (int i = 0; i < huecos.length; i++) s = s.replace("{" + i + "}", String.valueOf(huecos[i]));
        return s;
    }

    public static boolean existe(String clave) {
        return TEXTOS.containsKey(clave);
    }

    public static Map<String, Trilingue> todos() {
        return TEXTOS;
    }
}
