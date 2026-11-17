package castillafantasx.figura;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MATERIALES — Los materiales por su nombre, para escribirlos en los archivos
 * del editor de figuras: los de Material (PIEL, CARNE_VIVA, ACERO…) y los
 * hechos a medida, «acabado:RRGGBB» (lana:561E1E, metal:5E5650…).
 */
public final class Materiales {

    private Materiales() { }

    /** Los acabados con los que se hace un material a medida. */
    public static final String[] ACABADOS = { "mate", "lana", "lino", "seda", "cuero", "aspero", "escama", "rugoso", "acolchado", "malla", "metal", "placa", "luz" };

    private static final Map<String, Material> POR_NOMBRE = new LinkedHashMap<>();
    private static final Map<String, Material> A_MEDIDA = new HashMap<>();

    static {
        // Los materiales con nombre de Material: sus constantes públicas.
        for (Field f : Material.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Material.class) continue;
            try {
                POR_NOMBRE.put(f.getName(), (Material) f.get(null));
            } catch (IllegalAccessException e) {
                // No pasa: son públicas.
            }
        }
    }

    /** Los nombres de los materiales con nombre, en el orden de Material. */
    public static List<String> nombres() {
        return new ArrayList<>(POR_NOMBRE.keySet());
    }

    /** El material con ese nombre o hecho a medida («lana:561E1E»); null si no se entiende. */
    public static synchronized Material de(String nombre) {
        if (nombre == null) return null;
        Material m = POR_NOMBRE.get(nombre);
        if (m != null) return m;
        m = A_MEDIDA.get(nombre);
        if (m != null) return m;
        int dos = nombre.indexOf(':');
        if (dos < 0) return null;
        int rgb;
        try {
            rgb = Integer.parseInt(nombre.substring(dos + 1).trim(), 16);
        } catch (NumberFormatException e) {
            return null;
        }
        m = hacer(nombre.substring(0, dos).trim(), rgb);
        if (m != null) A_MEDIDA.put(nombre, m);
        return m;
    }

    /** El nombre con el que se escribe un material a medida. */
    public static String aMedida(String acabado, int rgb) {
        return acabado + ":" + String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    /** El nombre de ese material si es uno de los que tienen nombre; si no, null. */
    public static String nombreDe(Material m) {
        for (Map.Entry<String, Material> e : POR_NOMBRE.entrySet()) {
            if (e.getValue() == m) return e.getKey();
        }
        return null;
    }

    private static Material hacer(String acabado, int rgb) {
        switch (acabado) {
            case "mate":      return Material.mate(rgb);
            case "lana":      return Material.lana(rgb);
            case "lino":      return Material.lino(rgb);
            case "seda":      return Material.seda(rgb);
            case "cuero":     return Material.cuero(rgb);
            case "aspero":    return Material.aspero(rgb);
            case "escama":    return Material.escama(rgb);
            case "rugoso":    return Material.rugoso(rgb);
            case "acolchado": return Material.acolchado(rgb);
            case "malla":     return Material.malla(rgb);
            case "metal":     return Material.metal(rgb);
            case "placa":     return Material.placa(rgb);
            case "luz":       return Material.luz(rgb);
            default:          return null;
        }
    }
}
