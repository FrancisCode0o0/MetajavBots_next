package metajavbots.textos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    // El separador de idiomas es « | » (con espacios): dentro de {0:uno|varios} la barra es parte del texto.
                    String[] p = linea.substring(i + 1).split(" \\| ", -1);
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

    /** «{0:paso|pasos}»: la palabra en singular si el hueco 0 vale 1 (o -1), y si no, en plural. */
    private static final Pattern PLURAL = Pattern.compile("\\{(\\d+):([^|{}]*)\\|([^{}]*)\\}");

    public static String f(String clave, Object... huecos) {
        String s = plurales(t(clave), huecos);
        for (int i = 0; i < huecos.length; i++) s = s.replace("{" + i + "}", String.valueOf(huecos[i]));
        return s;
    }

    /** Elige singular o plural en cada «{n:singular|plural}» según el valor del hueco n. */
    public static String plurales(String s, Object... huecos) {
        if (s.indexOf(':') < 0) return s;
        Matcher m = PLURAL.matcher(s);
        StringBuilder b = new StringBuilder();
        while (m.find()) {
            int i = Integer.parseInt(m.group(1));
            boolean uno = false;
            if (i < huecos.length && huecos[i] != null) {
                try {
                    uno = Math.abs(Double.parseDouble(String.valueOf(huecos[i]).replace(',', '.').strip())) == 1;
                } catch (NumberFormatException e) {
                    uno = false;
                }
            }
            m.appendReplacement(b, Matcher.quoteReplacement(uno ? m.group(2) : m.group(3)));
        }
        m.appendTail(b);
        return b.toString();
    }

    public static boolean existe(String clave) {
        return TEXTOS.containsKey(clave);
    }

    public static Map<String, Trilingue> todos() {
        return TEXTOS;
    }
}
