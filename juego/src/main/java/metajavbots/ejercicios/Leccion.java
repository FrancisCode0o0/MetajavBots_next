package metajavbots.ejercicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * LECCIÓN — Un tutorial corto (res/tutoriales/, formato en docs/FORMATOS.md):
 * unas páginas con explicación y ejemplo, y una o dos tareas (un ejercicio
 * del banco o una prueba con el robot en la arena).
 */
public final class Leccion {

    /** Una página de la lección: su texto y, si hay, un trozo de código. */
    public static final class Pagina {
        public final Trilingue texto;
        public final String codigo;

        Pagina(Trilingue texto, String codigo) {
            this.texto = texto;
            this.codigo = codigo;
        }
    }

    /** Una tarea de la lección: un ejercicio que resolver o una prueba con el robot en la arena. */
    public static final class Tarea {
        /** El id de un ejercicio, o null. */
        public final String ejercicio;
        /** El objetivo en la arena («aciertos>=3», «dano<=40», «ronda_sin_errores», «usa:for»), o null. */
        public final String robot;
        public final Trilingue texto;

        Tarea(String ejercicio, String robot, Trilingue texto) {
            this.ejercicio = ejercicio;
            this.robot = robot;
            this.texto = texto;
        }
    }

    public final String id, tema;
    public final int orden;
    public final Trilingue titulo;
    public final List<Pagina> paginas;
    public final List<Tarea> tareas;

    Leccion(String id, String tema, int orden, Trilingue titulo, List<Pagina> paginas, List<Tarea> tareas) {
        this.id = id;
        this.tema = tema;
        this.orden = orden;
        this.titulo = titulo;
        this.paginas = Collections.unmodifiableList(paginas);
        this.tareas = Collections.unmodifiableList(tareas);
    }

    public String titulo(Idioma i) {
        return titulo.en(i);
    }

    private static List<Leccion> todas;

    public static synchronized List<Leccion> todas() {
        if (todas == null) {
            List<Leccion> l = new ArrayList<>();
            List<String> indice = Banco.indice("/ejercicios/indice.txt");
            for (Tema t : Tema.todos()) {
                String nombre = archivoDe(t, indice);
                if (nombre == null) continue;
                try (InputStream in = Leccion.class.getResourceAsStream("/tutoriales/" + nombre)) {
                    if (in != null) l.addAll(leer(in));
                } catch (IOException e) {
                    // sin lecciones de ese tema
                }
            }
            l.sort((a, b) -> a.tema.equals(b.tema) ? Integer.compare(a.orden, b.orden) : a.tema.compareTo(b.tema));
            todas = Collections.unmodifiableList(l);
        }
        return todas;
    }

    /** Los tutoriales se llaman como los ejercicios de su tema (01_primeros_pasos.txt…). */
    static String archivoDe(Tema t) {
        return archivoDe(t, Banco.indice("/ejercicios/indice.txt"));
    }

    static String archivoDe(Tema t, List<String> indice) {
        for (String a : indice) if (a.startsWith(t.id + "_")) return a;
        return null;
    }

    public static List<Leccion> deTema(String tema) {
        List<Leccion> l = new ArrayList<>();
        for (Leccion x : todas()) if (x.tema.equals(tema)) l.add(x);
        return l;
    }

    static List<Leccion> leer(InputStream in) throws IOException {
        List<Leccion> l = new ArrayList<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String id = null, tema = "01";
        int orden = 0;
        String[] titulo = new String[3];
        List<Pagina> paginas = new ArrayList<>();
        List<Tarea> tareas = new ArrayList<>();
        // Lo que se va leyendo de la página o tarea actual.
        String bloque = null;
        StringBuilder[] texto = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
        StringBuilder codigo = new StringBuilder();
        String ejercicio = null, robot = null;
        boolean enCodigo = false;
        String linea;
        List<String> lineas = new ArrayList<>();
        while ((linea = r.readLine()) != null) lineas.add(linea);
        lineas.add("=== fin");
        for (String ln : lineas) {
            if (ln.startsWith("#")) continue;
            boolean nuevaLeccion = ln.startsWith("===");
            boolean nuevoBloque = ln.startsWith("---");
            if ((nuevaLeccion || nuevoBloque) && bloque != null) {
                Trilingue t = new Trilingue(texto[0].toString().strip(), texto[1].toString().strip(), texto[2].toString().strip());
                if (bloque.equals("pagina")) paginas.add(new Pagina(t, codigo.toString().stripTrailing()));
                else if (bloque.equals("tarea")) tareas.add(new Tarea(ejercicio, robot, t));
                bloque = null;
            }
            if (nuevaLeccion) {
                if (id != null) l.add(new Leccion(id, tema, orden, new Trilingue(titulo[0], titulo[1], titulo[2]), paginas, tareas));
                id = ln.substring(3).strip();
                tema = "01";
                orden = 0;
                titulo = new String[3];
                paginas = new ArrayList<>();
                tareas = new ArrayList<>();
                continue;
            }
            if (nuevoBloque) {
                bloque = ln.substring(3).strip();
                for (StringBuilder b : texto) b.setLength(0);
                codigo.setLength(0);
                ejercicio = robot = null;
                enCodigo = false;
                continue;
            }
            if (enCodigo && (ln.startsWith("    ") || ln.isBlank())) {
                codigo.append(ln.length() >= 4 ? ln.substring(4) : "").append('\n');
                continue;
            }
            enCodigo = false;
            String s = ln.strip();
            if (s.isEmpty()) continue;
            if (bloque == null) {
                if (s.startsWith("tema:")) tema = s.substring(5).strip();
                else if (s.startsWith("orden:")) orden = entero(s.substring(6));
                else if (s.startsWith("titulo.es:")) titulo[0] = s.substring(10).strip();
                else if (s.startsWith("titulo.de:")) titulo[1] = s.substring(10).strip();
                else if (s.startsWith("titulo.en:")) titulo[2] = s.substring(10).strip();
                continue;
            }
            if (s.startsWith("es:")) unir(texto[0], s.substring(3));
            else if (s.startsWith("de:")) unir(texto[1], s.substring(3));
            else if (s.startsWith("en:")) unir(texto[2], s.substring(3));
            else if (s.startsWith("codigo:")) enCodigo = true;
            else if (s.startsWith("ejercicio:")) ejercicio = s.substring(10).strip();
            else if (s.startsWith("robot:")) robot = s.substring(6).strip();
        }
        return l;
    }

    private static void unir(StringBuilder b, String s) {
        s = s.strip();
        if (b.length() > 0) b.append(s.isEmpty() ? "\n" : " ");
        b.append(s);
    }

    private static int entero(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
