package castillafantasx.editor.codigo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * ÍNDICE DE NOMBRES — Todos los nombres que se declaran en el código y en los
 * diálogos: clases, constantes de los enum, métodos, campos, nudos de Ink y
 * secciones de los datos, cada uno con su archivo y su línea. Con él, el
 * buscador del editor encuentra cada cosa por su nombre y lo autocompleta.
 */
public final class IndiceDeNombres {

    /** Un nombre declarado: cómo se llama, qué es («clase», «método», «nudo»…), su archivo y su línea (desde 0). */
    public static final class Nombre {
        public final String nombre, tipo, ruta;
        public final int linea;
        /** El nombre sin tildes y en minúsculas, hecho una vez (el buscador lo mira en cada fotograma). */
        final String llano;

        Nombre(String nombre, String tipo, String ruta, int linea) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.ruta = ruta;
            this.linea = linea;
            this.llano = IndiceDeNombres.llano(nombre);
        }

        @Override
        public String toString() {
            return nombre + " (" + tipo + ", " + ruta + ":" + (linea + 1) + ")";
        }
    }

    // Lo que declara cada línea de Java: un tipo, una constante de enum, un campo o un método.
    private static final Pattern TIPO = Pattern.compile("\\b(class|enum|interface|record)\\s+([A-Z]\\w*)");
    private static final Pattern CONSTANTE_DE_ENUM = Pattern.compile("^\\s+([A-Z][A-Z0-9_]+)\\s*(\\(|,|;|$)");
    private static final Pattern CAMPO = Pattern.compile(
            "^\\s+(?:(?:public|private|protected|static|final|volatile|transient)\\s+)+[\\w<>\\[\\],.? ]+?\\s+([A-Za-z_]\\w*)\\s*(=|;)");
    private static final Pattern METODO = Pattern.compile(
            "^\\s+(?:(?:public|private|protected|static|final|abstract|synchronized|default)\\s+)*[A-Za-z][\\w<>\\[\\],.? ]*\\s+([a-z]\\w*)\\s*\\([^;]*$");
    // En Ink: los nudos (=== nombre ===), los puntos (= nombre) y las variables (VAR nombre).
    private static final Pattern NUDO = Pattern.compile("^\\s*(===?)\\s*(\\w+)");
    private static final Pattern VARIABLE_DE_INK = Pattern.compile("^\\s*(?:VAR|CONST)\\s+(\\w+)");
    // En los datos: las secciones ([nombre] o [evento nombre]).
    private static final Pattern SECCION = Pattern.compile("^\\[(?:\\w+\\s+)?(\\w+)\\]");
    private static final Set<String> NO_SON_METODOS = new HashSet<>(Arrays.asList("return", "new", "else", "throw", "case", "if", "for", "while", "switch", "catch"));

    private final List<String> carpetas;
    private List<Nombre> nombres;

    /** Un índice de esas carpetas (con «/» al final, relativas a la raíz del repo): las de Java, las de Ink y las de datos. */
    public IndiceDeNombres(List<String> carpetas) {
        this.carpetas = new ArrayList<>(carpetas);
    }

    /** Todos los nombres; la primera vez se leen los archivos. */
    public List<Nombre> todos() {
        if (nombres == null) leer();
        return nombres;
    }

    /** Olvida lo leído: la próxima búsqueda lee de nuevo (tras guardar un archivo). */
    public void olvidar() {
        nombres = null;
        ultimaBusqueda = null;
    }

    /** Cuántos nombres da una búsqueda si no se dice otra cosa (lo que se ve en el buscador del editor). */
    public static final int CUANTOS_SE_BUSCAN = 30;

    // La última búsqueda y lo que dio: el panel pregunta lo mismo en cada fotograma.
    private String ultimaBusqueda;
    private int ultimasCuantas;
    private List<Nombre> ultimoResultado;

    /** Lee de nuevo todos los archivos (tras guardar o cambiar un nombre). */
    public void leer() {
        List<Nombre> lista = new ArrayList<>();
        for (String carpeta : carpetas) {
            Path dir = Path.of(carpeta);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> archivos = Files.walk(dir)) {
                for (Path a : (Iterable<Path>) archivos.filter(Files::isRegularFile).sorted()::iterator) {
                    String ruta = carpeta + dir.relativize(a).toString().replace('\\', '/');
                    leerArchivo(a, ruta, lista);
                }
            } catch (IOException e) {
                System.err.println("No se ha podido leer " + carpeta + ": " + e.getMessage());
            }
        }
        nombres = lista;
        ultimaBusqueda = null;
    }

    private static void leerArchivo(Path archivo, String ruta, List<Nombre> lista) throws IOException {
        boolean java = ruta.endsWith(".java"), ink = ruta.endsWith(".ink"), datos = ruta.endsWith(".txt");
        if (!java && !ink && !datos) return;
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        boolean enEnum = false;
        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i);
            if (java) {
                String sinComentario = l.contains("//") ? l.substring(0, l.indexOf("//")) : l;
                if (sinComentario.trim().startsWith("*") || sinComentario.trim().startsWith("/*")) continue;
                Matcher m = TIPO.matcher(sinComentario);
                if (m.find()) {
                    lista.add(new Nombre(m.group(2), "clase", ruta, i));
                    enEnum = m.group(1).equals("enum");
                    continue;
                }
                if (enEnum && (m = CONSTANTE_DE_ENUM.matcher(sinComentario)).find()) {
                    lista.add(new Nombre(m.group(1), "constante", ruta, i));
                    if (m.group(2).equals(";")) enEnum = false;
                    continue;
                }
                if ((m = CAMPO.matcher(sinComentario)).find()) {
                    lista.add(new Nombre(m.group(1), "campo", ruta, i));
                } else if ((m = METODO.matcher(sinComentario)).find() && !NO_SON_METODOS.contains(sinComentario.trim().split("\\s+")[0])) {
                    lista.add(new Nombre(m.group(1), "método", ruta, i));
                }
            } else if (ink) {
                Matcher m = NUDO.matcher(l);
                if (m.find()) lista.add(new Nombre(m.group(2), m.group(1).length() == 3 ? "nudo" : "punto", ruta, i));
                else if ((m = VARIABLE_DE_INK.matcher(l)).find()) lista.add(new Nombre(m.group(1), "variable", ruta, i));
            } else {
                Matcher m = SECCION.matcher(l);
                if (m.find()) lista.add(new Nombre(m.group(1), "sección", ruta, i));
            }
        }
    }

    /** Sin tildes y en minúsculas: «Jabalí» casa con «jabali». */
    public static String llano(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    /**
     * Los nombres que casan con lo escrito, los mejores primero: los que
     * empiezan así antes que los que lo llevan dentro, y los cortos antes.
     * Como mucho 'cuantos'.
     */
    public List<Nombre> buscar(String texto, int cuantos) {
        String q = llano(texto.trim());
        List<Nombre> empiezan = new ArrayList<>(), llevan = new ArrayList<>();
        if (q.isEmpty()) return empiezan;
        if (q.equals(ultimaBusqueda) && cuantos == ultimasCuantas && nombres != null) return ultimoResultado;
        for (Nombre n : todos()) {
            if (n.llano.startsWith(q)) empiezan.add(n);
            else if (n.llano.contains(q)) llevan.add(n);
        }
        empiezan.sort((a, b) -> a.nombre.length() - b.nombre.length());
        llevan.sort((a, b) -> a.nombre.length() - b.nombre.length());
        empiezan.addAll(llevan);
        ultimaBusqueda = q;
        ultimasCuantas = cuantos;
        ultimoResultado = new ArrayList<>(empiezan.subList(0, Math.min(cuantos, empiezan.size())));
        return ultimoResultado;
    }

    /** Lo que falta para completar lo escrito con el mejor nombre que empieza así ("" si ninguno). */
    public String autocompletado(String texto) {
        for (Nombre n : buscar(texto, CUANTOS_SE_BUSCAN)) {
            if (llano(n.nombre).startsWith(llano(texto)) && n.nombre.length() > texto.length()) return n.nombre.substring(texto.length());
        }
        return "";
    }

    /**
     * Dónde se declara ese nombre (tal cual, con sus mayúsculas); null si en
     * ningún sitio. Si hay varios, la clase que da nombre a su archivo
     * (Ser.java antes que una clase Ser de dentro de otra); si no, el primero.
     */
    public Nombre declaracion(String nombre) {
        Nombre primero = null;
        for (Nombre n : todos()) {
            if (!n.nombre.equals(nombre)) continue;
            if (n.tipo.equals("clase") && n.ruta.endsWith("/" + nombre + ".java")) return n;
            if (primero == null) primero = n;
        }
        return primero;
    }
}
