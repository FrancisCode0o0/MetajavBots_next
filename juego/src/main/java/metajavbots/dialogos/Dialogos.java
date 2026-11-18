package metajavbots.dialogos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * DIÁLOGOS — Las frases de cada personalidad y del locutor (res/dialogos/),
 * por evento y en los tres idiomas. frase(evento, …) elige una al azar sin
 * repetir la última y rellena los huecos ({yo}, {rival}, {jugador}, {ronda},
 * {pieza}, {arma}). Cada robot trae así su personalidad de fábrica.
 */
public final class Dialogos {

    private static final Map<String, Dialogos> CARGADOS = new HashMap<>();

    private final Map<String, List<Trilingue>> porEvento = new LinkedHashMap<>();
    private final Map<String, Integer> ultima = new HashMap<>();
    private final Random azar = new Random();

    /** Los de una personalidad («estoico»…) o del «locutor». */
    public static synchronized Dialogos de(String personalidad) {
        return CARGADOS.computeIfAbsent(personalidad, Dialogos::leer);
    }

    private static Dialogos leer(String nombre) {
        Dialogos d = new Dialogos();
        try (InputStream in = Dialogos.class.getResourceAsStream("/dialogos/" + nombre + ".txt")) {
            if (in == null) return d;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea, evento = null, es = null, de = null;
            while ((linea = r.readLine()) != null) {
                linea = linea.strip();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("[") && linea.endsWith("]")) {
                    evento = linea.substring(1, linea.length() - 1).strip();
                    es = de = null;
                } else if (linea.startsWith("es:")) {
                    es = linea.substring(3).strip();
                } else if (linea.startsWith("de:")) {
                    de = linea.substring(3).strip();
                } else if (linea.startsWith("en:") && evento != null && es != null) {
                    d.porEvento.computeIfAbsent(evento, k -> new ArrayList<>()).add(new Trilingue(es, de, linea.substring(3).strip()));
                    es = de = null;
                }
            }
        } catch (IOException e) {
            // sin frases
        }
        return d;
    }

    public List<String> eventos() {
        return new ArrayList<>(porEvento.keySet());
    }

    public List<Trilingue> frases(String evento) {
        List<Trilingue> l = porEvento.get(evento);
        return l == null ? Collections.emptyList() : l;
    }

    /** Una frase del evento (o null si no hay), con los huecos rellenos. */
    public String frase(String evento, Idioma idioma, Map<String, String> huecos) {
        List<Trilingue> l = porEvento.get(evento);
        if (l == null || l.isEmpty()) return null;
        int i = azar.nextInt(l.size());
        Integer antes = ultima.get(evento);
        if (antes != null && l.size() > 1 && i == antes) i = (i + 1) % l.size();
        ultima.put(evento, i);
        String s = l.get(i).en(idioma);
        if (huecos != null) s = Concordancia.ajustar(s, huecos.get("pieza"), idioma);
        if (huecos != null) for (Map.Entry<String, String> h : huecos.entrySet()) s = s.replace("{" + h.getKey() + "}", h.getValue());
        return s;
    }
}
