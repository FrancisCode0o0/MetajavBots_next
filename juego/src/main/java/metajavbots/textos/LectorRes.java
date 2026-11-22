package metajavbots.textos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * LECTOR RES — Utilidades comunes para leer archivos de texto en {@code res/}
 * con secciones {@code [id]} y pares {@code clave=valor} o {@code clave:valor},
 * textos trilingües separados por {@code |}, números con valor por defecto y
 * normalización de cadenas para búsquedas sin tildes.
 */
public final class LectorRes {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}");

    /** Una sección {@code [id]} con sus pares clave-valor en orden de lectura. */
    public record Seccion(String id, Map<String, String> valores) {
    }

    private LectorRes() {
    }

    /** Lee un flujo UTF-8 con bloques {@code [id]} y líneas {@code clave<sep>valor}, ignorando vacías y comentarios {@code #}. */
    public static List<Seccion> secciones(InputStream in, char separador) throws IOException {
        List<Seccion> lista = new ArrayList<>();
        if (in == null) return lista;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String id = null;
            Map<String, String> valores = null;
            String linea;
            while ((linea = r.readLine()) != null) {
                String s = linea.strip();
                if (s.isEmpty() || s.startsWith("#")) continue;
                int cierre = s.startsWith("[") ? s.indexOf(']') : -1;
                if (cierre > 0) {
                    if (id != null) lista.add(new Seccion(id, valores));
                    id = s.substring(1, cierre).strip();
                    valores = new LinkedHashMap<>();
                } else if (valores != null) {
                    int idx = s.indexOf(separador);
                    if (idx > 0) valores.put(s.substring(0, idx).strip(), s.substring(idx + 1).strip());
                }
            }
            if (id != null) lista.add(new Seccion(id, valores));
        }
        return lista;
    }

    /** Parte un texto {@code es | de | en} en un {@link Trilingue}; si {@code heredarEs} es true, rellena huecos con ES. */
    public static Trilingue trilingue(String s, boolean heredarEs) {
        if (s == null) return new Trilingue("", "", "");
        String[] p = s.split("\\|", -1);
        String es = p[0].strip();
        String defecto = heredarEs ? es : "";
        String de = p.length > 1 ? p[1].strip() : defecto;
        String en = p.length > 2 ? p[2].strip() : defecto;
        return new Trilingue(es, de, en);
    }

    /** Convierte una cadena a entero con valor por defecto si es nula o no numérica. */
    public static int entero(String s, int siNo) {
        if (s == null) return siNo;
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }

    /** Convierte una cadena a decimal con valor por defecto si es nula o no numérica. */
    public static double decimal(String s, double siNo) {
        if (s == null) return siNo;
        try {
            return Double.parseDouble(s.strip());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }

    /** Pasa a minúsculas (ß → ss) y elimina tildes/diéresis usando un patrón precompilado. */
    public static String sinTildes(String s) {
        if (s == null || s.isEmpty()) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD);
        return MARCAS_DIACRITICAS.matcher(n).replaceAll("");
    }
}
