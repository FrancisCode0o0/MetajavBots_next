package metajavbots.piezas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * MODELO — Un modelo de robot: su silueta y colores (cómo se dibuja), su
 * naturaleza, su personalidad (sus diálogos, que trae de fábrica) y sus
 * piezas de fábrica. Salen de res/datos/modelos.txt, un bloque por modelo.
 */
public final class Modelo {

    /** La forma del cuerpo. */
    public enum Torso { CAJA, ESFERA, CUNA, BARRIL, HEXAGONO, ESBELTO, CAPARAZON, CRISTAL }

    /** La forma de la cabeza. */
    public enum Cabeza { VISOR, CUPULA, MONOOJO, ANTENAS, CUERNOS, CRESTA, PANTALLA, FAROL, CASCO, OJOS }

    /** El grosor de los brazos. */
    public enum Brazos { FINOS, NORMALES, GRUESOS }

    public final String id;
    public final Trilingue nombre, lema;
    public final Naturaleza naturaleza;
    public final Arquetipo arquetipo;
    public final String personalidad;
    /** El radio del robot en la arena. */
    public final double tamano;
    public final Torso torso;
    public final Cabeza cabeza;
    public final Brazos brazos;
    public final List<String> detalles;
    /** Principal, secundario y acento (RGB). */
    public final int color1, color2, color3;
    /** Cuánto blindaje tiene respecto a lo normal (1 = normal). */
    public final double blindaje;
    /** Cuánto corre respecto a lo normal. */
    public final double velocidad;
    /** Sus piezas de fábrica: cabeza, brazo izquierdo, brazo derecho, piernas, núcleo. */
    public final List<String> piezas;
    public final String desbloqueo;

    Modelo(Map<String, String> v) {
        id = v.get("id");
        nombre = trilingue(v.getOrDefault("nombre", id));
        lema = trilingue(v.getOrDefault("lema", ""));
        naturaleza = Naturaleza.de(v.get("naturaleza"));
        arquetipo = Arquetipo.de(v.get("arquetipo"));
        personalidad = v.getOrDefault("personalidad", "estoico");
        tamano = numero(v.get("tamano"), 32);
        torso = Pieza.enumDe(Torso.class, v.get("torso"), Torso.CAJA);
        cabeza = Pieza.enumDe(Cabeza.class, v.get("cabeza"), Cabeza.VISOR);
        brazos = Pieza.enumDe(Brazos.class, v.get("brazos"), Brazos.NORMALES);
        detalles = Collections.unmodifiableList(Arrays.asList(v.getOrDefault("detalles", "").trim().split("\\s+")));
        String[] c = v.getOrDefault("colores", "888888 444444 FFFFFF").trim().split("\\s+");
        color1 = color(c, 0);
        color2 = color(c, 1);
        color3 = color(c, 2);
        blindaje = numero(v.get("blindaje"), 1);
        velocidad = numero(v.get("velocidad"), 1);
        piezas = Collections.unmodifiableList(Arrays.asList(v.getOrDefault("piezas", "").trim().split("\\s+")));
        desbloqueo = v.getOrDefault("desbloqueo", "inicio");
    }

    public String nombre(Idioma i) {
        return nombre.en(i);
    }

    public boolean tiene(String detalle) {
        return detalles.contains(detalle);
    }

    /** Devuelve el identificador base de una pieza sin sufijo de marca («martillo_3» → «martillo»). */
    public static String idBaseDePieza(String idPieza) {
        if (idPieza == null) return "";
        String s = idPieza.strip().toLowerCase(Locale.ROOT);
        int i = s.lastIndexOf('_');
        if (i > 0 && s.substring(i + 1).matches("[2-9]|10")) return s.substring(0, i);
        return s;
    }

    /** Identificadores base de las armas características (brazos de fábrica) de este modelo. */
    public List<String> armasCaracteristicas() {
        List<String> armas = new ArrayList<>(2);
        for (int idx = 1; idx <= 2 && idx < piezas.size(); idx++) {
            String base = idBaseDePieza(piezas.get(idx));
            if (!base.isEmpty() && !armas.contains(base)) armas.add(base);
        }
        return Collections.unmodifiableList(armas);
    }

    /** Indica si la pieza corresponde a una de las piezas de fábrica de este modelo (en cualquier marca Mk I–V). */
    public boolean esPiezaCaracteristica(Pieza pieza) {
        if (pieza == null) return false;
        String base = idBaseDePieza(pieza.id);
        for (String p : piezas) {
            if (idBaseDePieza(p).equals(base)) return true;
        }
        return false;
    }

    /** Indica si la pieza es un arma característica (brazo propio) de este modelo en cualquier marca Mk I–V. */
    public boolean esArmaCaracteristica(Pieza pieza) {
        if (pieza == null || pieza.ranura != Pieza.Ranura.BRAZO) return false;
        return armasCaracteristicas().contains(idBaseDePieza(pieza.id));
    }

    private static Trilingue trilingue(String s) {
        String[] p = s.split("\\|", -1);
        return new Trilingue(p[0].strip(), p.length > 1 ? p[1].strip() : "", p.length > 2 ? p[2].strip() : "");
    }

    private static double numero(String s, double siNo) {
        try {
            return s == null ? siNo : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }

    private static int color(String[] c, int i) {
        try {
            return i < c.length ? Integer.parseInt(c[i], 16) : 0x888888;
        } catch (NumberFormatException e) {
            return 0x888888;
        }
    }

    // ------------------------------------------------------------------ el catálogo

    private static List<Modelo> delJuego;

    public static synchronized List<Modelo> todos() {
        if (delJuego == null) {
            try (InputStream in = Modelo.class.getResourceAsStream("/datos/modelos.txt")) {
                if (in == null) throw new IllegalStateException("Falta res/datos/modelos.txt");
                delJuego = Collections.unmodifiableList(leer(in));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return delJuego;
    }

    public static Modelo de(String id) {
        for (Modelo m : todos()) if (m.id.equals(id)) return m;
        return todos().get(0);
    }

    static List<Modelo> leer(InputStream in) throws IOException {
        List<Modelo> lista = new ArrayList<>();
        Map<String, String> actual = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("[") && linea.endsWith("]")) {
                    if (actual != null) lista.add(new Modelo(actual));
                    actual = new LinkedHashMap<>();
                    actual.put("id", linea.substring(1, linea.length() - 1).strip().toLowerCase(Locale.ROOT));
                } else if (actual != null && linea.contains("=")) {
                    int i = linea.indexOf('=');
                    actual.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
                }
            }
        }
        if (actual != null) lista.add(new Modelo(actual));
        return lista;
    }

    @Override
    public String toString() {
        return id;
    }
}
