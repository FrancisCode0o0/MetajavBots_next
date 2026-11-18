package metajavbots.campanas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import metajavbots.textos.Trilingue;

/**
 * CAMPAÑA — Una historia de varias peleas seguidas, parodia de una película
 * de combate (res/datos/campanas.txt; las que inventa la IA, en la carpeta de
 * la partida, con el mismo formato). Cada capítulo cuenta un trozo de la
 * historia, presenta al rival y, al ganarle, abre el siguiente.
 */
public final class Campana {

    /** Un capítulo: el rival (su nombre y su modelo), lo fuerte que sale, dónde se pelea y lo que se cuenta antes y después. */
    public static final class Capitulo {
        public final int numero;
        public final String modelo, escenario;
        /** El nombre del rival (con uno solo, el mismo en los tres idiomas). */
        public final Trilingue rival;
        public final double nivel;
        public final int rondas;
        public final Trilingue texto, victoria;

        Capitulo(int numero, Map<String, String> v) {
            this.numero = numero;
            this.rival = trilingue(v.getOrDefault("rival", "?"));
            this.modelo = v.getOrDefault("modelo", "cobalto");
            this.escenario = v.getOrDefault("escenario", "HANGAR").toUpperCase();
            this.nivel = decimal(v.get("nivel"), 0.3);
            this.rondas = (int) Math.max(1, Math.min(6, decimal(v.get("rondas"), 3)));
            this.texto = trilingue(v.getOrDefault("texto", ""));
            this.victoria = trilingue(v.getOrDefault("victoria", ""));
        }
    }

    public final String id;
    public final Trilingue titulo, lema;
    /** La película a la que rinde homenaje (solo para leerlo; el juego lo enseña en pequeño). */
    public final String homenaje;
    /** El entrenador que la cuenta (Entrenador) y el tema de música (Musica). */
    public final String entrenador, musica;
    /** La campaña que hay que acabar antes (las secuelas, en orden), o vacío. */
    public final String requiere;
    public final List<Capitulo> capitulos;
    /** Si la inventó la IA (se guarda en la partida, no en el juego). */
    public final boolean inventada;

    private Campana(String id, Map<String, String> v, boolean inventada) {
        this.id = id;
        this.inventada = inventada;
        this.titulo = trilingue(v.getOrDefault("titulo", id));
        this.lema = trilingue(v.getOrDefault("lema", ""));
        this.homenaje = v.getOrDefault("homenaje", "");
        this.entrenador = v.getOrDefault("entrenador", "tuercas");
        this.musica = v.getOrDefault("musica", "combate");
        this.requiere = v.getOrDefault("requiere", "");
        // Los capítulos: «1.rival», «1.texto»… agrupados por su número.
        Map<Integer, Map<String, String>> porNumero = new TreeMap<>();
        for (Map.Entry<String, String> e : v.entrySet()) {
            int punto = e.getKey().indexOf('.');
            if (punto <= 0) continue;
            try {
                int n = Integer.parseInt(e.getKey().substring(0, punto));
                porNumero.computeIfAbsent(n, k -> new LinkedHashMap<>()).put(e.getKey().substring(punto + 1), e.getValue());
            } catch (NumberFormatException ex) {
                // una clave con punto que no es de un capítulo: se ignora
            }
        }
        List<Capitulo> cs = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, String>> e : porNumero.entrySet()) cs.add(new Capitulo(e.getKey(), e.getValue()));
        this.capitulos = Collections.unmodifiableList(cs);
    }

    /** La clave con que se apunta en la partida que se ganó ese capítulo. */
    public String clave(int capitulo) {
        return id + ":" + capitulo;
    }

    /** Cuántos capítulos seguidos, desde el primero, se han ganado (con los ganados de la partida). */
    public int ganados(Set<String> capitulosGanados) {
        int n = 0;
        while (n < capitulos.size() && capitulosGanados.contains(clave(capitulos.get(n).numero))) n++;
        return n;
    }

    public boolean terminada(Set<String> capitulosGanados) {
        return !capitulos.isEmpty() && ganados(capitulosGanados) == capitulos.size();
    }

    /** Si se puede jugar: no pide ninguna o la que pide ya está terminada. */
    public boolean abierta(Set<String> capitulosGanados) {
        if (requiere.isEmpty()) return true;
        Campana antes = de(requiere);
        return antes == null || antes.terminada(capitulosGanados);
    }

    // ================================================================ leer

    private static List<Campana> deSerie;

    /** Las campañas escritas de serie, en el orden del archivo. */
    public static synchronized List<Campana> deSerie() {
        if (deSerie == null) {
            try (InputStream in = Campana.class.getResourceAsStream("/datos/campanas.txt")) {
                deSerie = Collections.unmodifiableList(leer(new InputStreamReader(in, StandardCharsets.UTF_8), false));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer res/datos/campanas.txt", e);
            }
        }
        return deSerie;
    }

    /** La campaña de serie con ese id, o null. */
    public static Campana de(String id) {
        for (Campana c : deSerie()) if (c.id.equals(id)) return c;
        return null;
    }

    /** Lee campañas en el formato del archivo («[id]», «clave: valor», «#» comentario). */
    public static List<Campana> leer(Reader fuente, boolean inventadas) throws IOException {
        List<Campana> l = new ArrayList<>();
        BufferedReader r = new BufferedReader(fuente);
        String id = null, linea;
        Map<String, String> v = new LinkedHashMap<>();
        while ((linea = r.readLine()) != null) {
            linea = linea.strip();
            if (linea.startsWith("#") || linea.isEmpty()) continue;
            if (linea.startsWith("[") && linea.contains("]")) {
                if (id != null) l.add(new Campana(id, v, inventadas));
                id = linea.substring(1, linea.indexOf(']')).strip();
                v = new LinkedHashMap<>();
            } else if (linea.contains(":")) {
                int i = linea.indexOf(':');
                v.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
            }
        }
        if (id != null) l.add(new Campana(id, v, inventadas));
        return l;
    }

    /** Lee campañas de un texto (lo que contesta la IA); las que no traen ningún capítulo se descartan. */
    public static List<Campana> leer(String texto, boolean inventadas) {
        try {
            List<Campana> l = new ArrayList<>();
            for (Campana c : leer(new StringReader(texto), inventadas)) if (!c.capitulos.isEmpty()) l.add(c);
            return l;
        } catch (IOException e) {
            return List.of();
        }
    }

    static Trilingue trilingue(String s) {
        String[] p = s.split("\\|");
        String es = p[0].strip();
        return new Trilingue(es, p.length > 1 ? p[1].strip() : es, p.length > 2 ? p[2].strip() : es);
    }

    private static double decimal(String s, double siNo) {
        try {
            return s == null ? siNo : Double.parseDouble(s.strip());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }
}
