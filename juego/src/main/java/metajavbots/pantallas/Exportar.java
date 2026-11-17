package metajavbots.pantallas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;

/**
 * EXPORTAR — Un robot como archivo .java para jugar contra los de otros: el
 * código tal cual, con un comentario al principio que dice su modelo y sus
 * piezas («// @metajavbot modelo=cobalto cabeza=… »). Se guarda en
 * robots_exportados/; los de otros se ponen en robots_ajenos/.
 */
public final class Exportar {

    private static final Pattern CABECERA = Pattern.compile("//\\s*@metajavbot\\s+(.*)");

    private Exportar() {
    }

    public static String conCabecera(Jugador.Robot r, String autor) {
        StringBuilder b = new StringBuilder("// @metajavbot modelo=").append(r.modelo);
        for (Map.Entry<Montaje.Hueco, String> e : r.piezas.entrySet()) b.append(' ').append(e.getKey().name().toLowerCase()).append('=').append(e.getValue());
        b.append("\n// Robot «").append(r.nombre.replace('\n', ' ')).append("» de ").append(autor.replace('\n', ' '))
                .append(", hecho en MetajavBots. Ponlo en robots_ajenos/ para pelear contra él.\n");
        String codigo = r.codigo;
        // Si ya llevaba una cabecera (lo importó de otro), se cambia.
        codigo = codigo.replaceAll("(?m)^//\\s*@metajavbot.*\\R(//.*hecho en MetajavBots.*\\R)?", "");
        return b + codigo;
    }

    public static Path exportar(Jugador.Robot r, String autor) throws IOException {
        Path d = Paths.get("robots_exportados");
        Files.createDirectories(d);
        String nombre = r.nombre.replaceAll("[^\\p{L}\\p{N}_-]", "_");
        Path f = d.resolve(nombre.isEmpty() ? "robot.java" : nombre + ".java");
        Files.writeString(f, conCabecera(r, autor), StandardCharsets.UTF_8);
        return f.toAbsolutePath();
    }

    /** El montaje que dice la cabecera (o el de fábrica de su modelo, o Cobalto si no dice nada). */
    public static Montaje montajeDe(String codigo) {
        Matcher m = CABECERA.matcher(codigo);
        if (!m.find()) return new Montaje(Modelo.de("cobalto"));
        String modelo = "cobalto";
        String[] partes = m.group(1).strip().split("\\s+");
        for (String p : partes) if (p.startsWith("modelo=")) modelo = p.substring(7);
        Montaje mo = new Montaje(Modelo.de(modelo));
        for (String p : partes) {
            int i = p.indexOf('=');
            if (i <= 0) continue;
            String k = p.substring(0, i).toUpperCase(), v = p.substring(i + 1);
            for (Montaje.Hueco h : Montaje.Hueco.values()) {
                if (h.name().equals(k)) {
                    Pieza pz = CatalogoDePiezas.delJuego().pieza(v);
                    if (pz != null) mo.poner(h, pz);
                }
            }
        }
        return mo;
    }
}
