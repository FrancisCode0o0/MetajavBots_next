package metajavbots.codigo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metajavbots.textos.Idioma;

/**
 * TRADUCTOR DE API — Pasa un robot escrito con la API en español a la API de
 * otro idioma (alVerEnemigo → beiSicht → onSeeEnemy, Enemigo → Gegner →
 * Enemy…), con los nombres de res/datos/api.txt. Solo cambia los nombres de
 * la API; lo que va entre comillas y los comentarios se quedan igual.
 */
public final class TraductorDeApi {

    private static Map<Idioma, Map<String, String>> nombres;
    private static final Pattern PALABRA = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|//[^\\n]*|/\\*.*?\\*/|[A-Za-z_][A-Za-z0-9_]*",
            Pattern.DOTALL);

    private TraductorDeApi() {
    }

    /** El código con los nombres de la API de ese idioma (en español, tal cual). */
    public static String traducir(String codigo, Idioma idioma) {
        Map<String, String> m = nombres().get(idioma);
        if (m == null || m.isEmpty()) return codigo;
        Matcher x = PALABRA.matcher(codigo);
        StringBuilder sb = new StringBuilder();
        while (x.find()) {
            String s = x.group();
            x.appendReplacement(sb, Matcher.quoteReplacement(m.getOrDefault(s, s)));
        }
        x.appendTail(sb);
        return sb.toString();
    }

    private static synchronized Map<Idioma, Map<String, String>> nombres() {
        if (nombres == null) {
            nombres = new EnumMap<>(Idioma.class);
            Map<String, String> de = new HashMap<>(), en = new HashMap<>();
            de.put("Metajavbot", "MetajavbotDe");
            en.put("Metajavbot", "MetajavbotEn");
            try (InputStream in = TraductorDeApi.class.getResourceAsStream("/datos/api.txt")) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String linea, es = null;
                while ((linea = r.readLine()) != null) {
                    if (linea.startsWith("firma: ")) es = linea.substring(7);
                    else if (linea.startsWith("firma.de: ") && es != null) emparejar(es, linea.substring(10), de);
                    else if (linea.startsWith("firma.en: ") && es != null) emparejar(es, linea.substring(10), en);
                }
            } catch (Exception e) {
                // Sin la tabla, el código se queda en español (también compila: la API española siempre está).
            }
            nombres.put(Idioma.DE, de);
            nombres.put(Idioma.EN, en);
        }
        return nombres;
    }

    /**
     * «Enemigo: double angulo()» con «Gegner: double winkel()»: empareja, en orden, los nombres de los métodos (los
     * que van antes de un paréntesis) y de los tipos (con mayúscula). Los de los parámetros no: «distancia» es un
     * método de Enemigo y también el parámetro de avanzar.
     */
    private static void emparejar(String es, String otra, Map<String, String> m) {
        Matcher a = NOMBRE.matcher(es), b = NOMBRE.matcher(otra);
        while (a.find() && b.find()) {
            String x = a.group(1), y = b.group(1);
            boolean metodo = a.group(2) != null, tipo = Character.isUpperCase(x.charAt(0));
            if ((metodo || tipo) && !x.equals(y)) m.putIfAbsent(x, y);
        }
    }

    private static final Pattern NOMBRE = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*(\\()?");
}
