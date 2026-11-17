package metajavbots.progreso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.textos.Trilingue;

/**
 * PRUEBA DE NIVEL — Las preguntas del principio (res/datos/prueba_de_nivel.txt):
 * tres por escalón, de menos a más. Se empieza en el último escalón del que
 * se han acertado al menos dos de sus tres preguntas (sin saltarse ninguno).
 * Quien no ha programado nunca empieza en Noob I sin pasar por aquí.
 */
public final class PruebaDeNivel {

    public static final class Pregunta {
        public final Etiqueta nivel;
        public final Trilingue pregunta;
        public final String codigo;
        public final List<Trilingue> opciones;
        /** La buena, desde 0. */
        public final int bien;

        Pregunta(Etiqueta nivel, Trilingue pregunta, String codigo, List<Trilingue> opciones, int bien) {
            this.nivel = nivel;
            this.pregunta = pregunta;
            this.codigo = codigo;
            this.opciones = Collections.unmodifiableList(opciones);
            this.bien = bien;
        }
    }

    private static List<Pregunta> preguntas;

    private PruebaDeNivel() {
    }

    public static synchronized List<Pregunta> preguntas() {
        if (preguntas == null) {
            try (InputStream in = PruebaDeNivel.class.getResourceAsStream("/datos/prueba_de_nivel.txt")) {
                preguntas = Collections.unmodifiableList(leer(in));
            } catch (IOException | RuntimeException e) {
                throw new IllegalStateException("No se pudo leer res/datos/prueba_de_nivel.txt", e);
            }
        }
        return preguntas;
    }

    static List<Pregunta> leer(InputStream in) throws IOException {
        List<Pregunta> l = new ArrayList<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        Etiqueta nivel = null;
        String es = "", de = "", en = "", seccion = "";
        StringBuilder codigo = new StringBuilder();
        List<Trilingue> opciones = new ArrayList<>();
        String linea;
        while ((linea = r.readLine()) != null) {
            if (linea.startsWith("#")) continue;
            if (linea.startsWith("===")) {
                nivel = Etiqueta.de(linea.substring(3).strip());
                es = de = en = seccion = "";
                codigo.setLength(0);
                opciones = new ArrayList<>();
                continue;
            }
            if (linea.startsWith("    ") && seccion.equals("codigo")) {
                codigo.append(linea.substring(4)).append('\n');
            } else if (linea.startsWith("    ") && seccion.equals("opciones")) {
                String[] p = linea.strip().split("\\|");
                opciones.add(new Trilingue(p[0].strip(), p.length > 1 ? p[1].strip() : "", p.length > 2 ? p[2].strip() : ""));
            } else if (linea.startsWith("pregunta.es:")) {
                es = linea.substring(12).strip();
            } else if (linea.startsWith("pregunta.de:")) {
                de = linea.substring(12).strip();
            } else if (linea.startsWith("pregunta.en:")) {
                en = linea.substring(12).strip();
            } else if (linea.startsWith("codigo:")) {
                seccion = "codigo";
            } else if (linea.startsWith("opciones:")) {
                seccion = "opciones";
            } else if (linea.startsWith("bien:") && nivel != null) {
                int bien = Integer.parseInt(linea.substring(5).strip()) - 1;
                l.add(new Pregunta(nivel, new Trilingue(es, de, en), codigo.toString().stripTrailing(), opciones, bien));
                seccion = "";
            }
        }
        return l;
    }

    /** La etiqueta con que se empieza según los aciertos (uno por pregunta, en el orden de preguntas()). */
    public static Etiqueta resultado(List<Boolean> aciertos) {
        List<Pregunta> p = preguntas();
        Etiqueta resultado = Etiqueta.NOOB_1;
        int i = 0;
        while (i < p.size()) {
            Etiqueta nivel = p.get(i).nivel;
            int bien = 0, total = 0;
            while (i < p.size() && p.get(i).nivel == nivel) {
                if (i < aciertos.size() && Boolean.TRUE.equals(aciertos.get(i))) bien++;
                total++;
                i++;
            }
            if (bien * 3 >= total * 2) resultado = nivel;
            else break;
        }
        return resultado;
    }
}
