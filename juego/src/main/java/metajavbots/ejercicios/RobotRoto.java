package metajavbots.ejercicios;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metajavbots.codigo.TraductorDeApi;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * ROBOT ROTO — Un robot con fallos escondidos (res/datos/robots_rotos.txt):
 * gira sin parar, apunta al revés, no ataca nunca, no compila… El jugador lo
 * ve fallar en la arena, lo arregla en el editor de la pelea y cumple el
 * objetivo. Así se practica a depurar código ajeno.
 */
public final class RobotRoto {

    public final String id, objetivo, rival, modelo;
    public final Trilingue titulo, sintoma, pista;
    public final double nivel;
    /** El código, con la API en español (codigo(Idioma) lo da en la del jugador). */
    private final String codigo;

    private RobotRoto(String id, Map<String, String> v, String codigo) {
        this.id = id;
        this.titulo = trilingue(v.getOrDefault("titulo", id));
        this.sintoma = trilingue(v.getOrDefault("sintoma", ""));
        this.pista = trilingue(v.getOrDefault("pista", ""));
        this.objetivo = v.getOrDefault("objetivo", "aciertos>=3");
        this.rival = v.getOrDefault("rival", "chatarra");
        this.modelo = v.getOrDefault("modelo", "cobalto");
        double n;
        try {
            n = Double.parseDouble(v.getOrDefault("nivel", "0.15"));
        } catch (NumberFormatException e) {
            n = 0.15;
        }
        this.nivel = n;
        this.codigo = codigo;
    }

    /** El código roto, con la API del idioma del jugador. */
    public String codigo(Idioma i) {
        return TraductorDeApi.traducir(codigo, i);
    }

    private static Trilingue trilingue(String s) {
        String[] p = s.split("\\|");
        String es = p[0].strip();
        return new Trilingue(es, p.length > 1 ? p[1].strip() : es, p.length > 2 ? p[2].strip() : es);
    }

    private static List<RobotRoto> todos;

    /** Todos, en el orden del archivo. */
    public static synchronized List<RobotRoto> todos() {
        if (todos == null) {
            List<RobotRoto> l = new ArrayList<>();
            try (InputStream in = RobotRoto.class.getResourceAsStream("/datos/robots_rotos.txt")) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String id = null, linea;
                Map<String, String> v = new HashMap<>();
                StringBuilder cod = null;
                while ((linea = r.readLine()) != null) {
                    if (cod != null && (linea.startsWith("    ") || linea.isBlank()) && !linea.startsWith("[")) {
                        cod.append(linea.length() >= 4 ? linea.substring(4) : "").append('\n');
                        continue;
                    }
                    if (linea.startsWith("#") || linea.isBlank()) continue;
                    if (linea.startsWith("[")) {
                        if (id != null) l.add(new RobotRoto(id, v, cod == null ? "" : cod.toString().strip() + "\n"));
                        id = linea.substring(1, linea.indexOf(']')).strip();
                        v = new HashMap<>();
                        cod = null;
                    } else if (linea.strip().equals("codigo:")) {
                        cod = new StringBuilder();
                    } else if (linea.contains(":")) {
                        int i = linea.indexOf(':');
                        v.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
                    }
                }
                if (id != null) l.add(new RobotRoto(id, v, cod == null ? "" : cod.toString().strip() + "\n"));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer res/datos/robots_rotos.txt", e);
            }
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }
}
