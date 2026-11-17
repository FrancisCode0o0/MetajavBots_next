package castillafantasx.editor.codigo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import castillafantasx.datos.Candado;

/**
 * CAMBIO DE NOMBRE — Cambiar un nombre en todo el código y en los datos que
 * lo usan: primero se planea (dónde sale, palabra entera y con sus
 * mayúsculas), se enseña cuántas veces, y luego se aplica. Si el nombre es el
 * de una clase, su archivo también cambia de nombre. Todo se deshace.
 */
public final class CambioDeNombre {

    /** Una línea que cambia: su archivo, su número (desde 0), cómo estaba y cómo queda. */
    public static final class Linea {
        public final String ruta;
        public final int linea;
        public final String antes, despues;

        Linea(String ruta, int linea, String antes, String despues) {
            this.ruta = ruta;
            this.linea = linea;
            this.antes = antes;
            this.despues = despues;
        }
    }

    private static final Set<String> PALABRAS_DE_JAVA = new HashSet<>(Arrays.asList(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "if", "implements", "import", "instanceof", "int", "interface", "long", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "this", "throw", "throws", "try",
            "void", "while", "true", "false", "var", "record"));
    // Los archivos en que se busca el nombre: el código, los diálogos y los datos.
    private static final String[] EXTENSIONES = { ".java", ".ink", ".txt" };

    public final String viejo, nuevo;
    /** Las líneas que cambian, archivo a archivo. */
    public final List<Linea> lineas = new ArrayList<>();
    /** Lo que no deja hacer el cambio ("" si se puede). */
    public final String problema;
    /** Si es una clase con su archivo: el archivo de ahora y el nuevo; si no, null. */
    public final String archivoViejo, archivoNuevo;

    private final Map<String, byte[]> comoEstaban = new LinkedHashMap<>();
    private boolean aplicado;

    /**
     * Planea cambiar 'viejo' por 'nuevo' en los archivos de esas carpetas.
     * 'indice' dice qué nombres ya existen (para no pisar otro); puede ser null.
     */
    public CambioDeNombre(String viejo, String nuevo, List<String> carpetas, IndiceDeNombres indice) {
        this.viejo = viejo;
        this.nuevo = nuevo;
        Pattern palabra = Pattern.compile("(?<![\\w$])" + Pattern.quote(viejo) + "(?![\\w$])");
        String archivoDeLaClase = null;
        for (String carpeta : carpetas) {
            Path dir = Path.of(carpeta);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> archivos = Files.walk(dir)) {
                for (Path a : (Iterable<Path>) archivos.filter(Files::isRegularFile).sorted()::iterator) {
                    String ruta = carpeta + dir.relativize(a).toString().replace('\\', '/');
                    if (!deTexto(ruta)) continue;
                    if (a.getFileName().toString().equals(viejo + ".java")) archivoDeLaClase = ruta;
                    List<String> suyas = Files.readAllLines(a, StandardCharsets.UTF_8);
                    for (int i = 0; i < suyas.size(); i++) {
                        Matcher m = palabra.matcher(suyas.get(i));
                        if (m.find()) lineas.add(new Linea(ruta, i, suyas.get(i), m.replaceAll(Matcher.quoteReplacement(nuevo))));
                    }
                }
            } catch (IOException e) {
                System.err.println("No se ha podido leer " + carpeta + ": " + e.getMessage());
            }
        }
        archivoViejo = archivoDeLaClase;
        archivoNuevo = archivoDeLaClase == null ? null : archivoDeLaClase.substring(0, archivoDeLaClase.length() - (viejo + ".java").length()) + nuevo + ".java";
        problema = problemaDe(viejo, nuevo, indice);
    }

    private static boolean deTexto(String ruta) {
        for (String e : EXTENSIONES) {
            if (ruta.endsWith(e)) return true;
        }
        return false;
    }

    /** Por qué no se puede: nombre vacío, con letras raras, una palabra de Java, uno que ya existe o sin nada que cambiar. */
    private String problemaDe(String viejo, String nuevo, IndiceDeNombres indice) {
        if (nuevo.isEmpty()) return "Escribe el nombre nuevo.";
        if (nuevo.equals(viejo)) return "Es el mismo nombre.";
        if (!nuevo.matches("[A-Za-z_][A-Za-z0-9_]*")) return "Un nombre lleva solo letras sin tilde, cifras y «_», y no empieza por cifra.";
        if (PALABRAS_DE_JAVA.contains(nuevo)) return "«" + nuevo + "» es una palabra de Java.";
        if (indice != null && indice.declaracion(nuevo) != null) return "Ya hay algo que se llama «" + nuevo + "» (" + indice.declaracion(nuevo).ruta + ").";
        if (lineas.isEmpty()) return "«" + viejo + "» no sale en ningún archivo.";
        if (archivoNuevo != null && Files.exists(Path.of(archivoNuevo))) return "Ya existe " + archivoNuevo + ".";
        return "";
    }

    /** Cuántos archivos distintos cambian. */
    public int archivos() {
        Set<String> distintos = new HashSet<>();
        for (Linea l : lineas) distintos.add(l.ruta);
        return distintos.size();
    }

    /** Escribe el cambio en los archivos (con los mismos finales de línea que tenían). Devuelve false si había un problema. */
    public boolean aplicar() throws IOException {
        if (!problema.isEmpty() || aplicado) return false;
        Map<String, List<Linea>> porArchivo = new LinkedHashMap<>();
        for (Linea l : lineas) porArchivo.computeIfAbsent(l.ruta, k -> new ArrayList<>()).add(l);
        for (Map.Entry<String, List<Linea>> e : porArchivo.entrySet()) {
            Path archivo = Path.of(e.getKey());
            byte[] bytes = Files.readAllBytes(archivo);
            comoEstaban.put(e.getKey(), bytes);
            String texto = new String(bytes, StandardCharsets.UTF_8);
            String salto = texto.contains("\r\n") ? "\r\n" : "\n";
            List<String> suyas = new ArrayList<>(Arrays.asList(texto.replace("\r\n", "\n").split("\n", -1)));
            for (Linea l : e.getValue()) suyas.set(l.linea, l.despues);
            Candado.vigilar(archivo);
            Files.write(archivo, String.join(salto, suyas).getBytes(StandardCharsets.UTF_8));
        }
        if (archivoViejo != null) {
            Candado.vigilar(Path.of(archivoNuevo));
            Files.move(Path.of(archivoViejo), Path.of(archivoNuevo));
        }
        aplicado = true;
        return true;
    }

    /** Deja los archivos como estaban antes de aplicar. */
    public void deshacer() throws IOException {
        if (!aplicado) return;
        if (archivoViejo != null) Files.move(Path.of(archivoNuevo), Path.of(archivoViejo));
        for (Map.Entry<String, byte[]> e : comoEstaban.entrySet()) {
            Candado.vigilar(Path.of(e.getKey()));
            Files.write(Path.of(e.getKey()), e.getValue());
        }
        aplicado = false;
    }

    /** Lo vuelve a aplicar tras deshacerlo (rehacer). */
    public void rehacer() throws IOException {
        if (aplicado) return;
        comoEstaban.clear();
        aplicar();
    }

    /** Dónde cae una ruta tras el cambio (si era el archivo de la clase, el nuevo). */
    public String rutaTrasElCambio(String ruta) {
        return aplicado && ruta.equals(archivoViejo) ? archivoNuevo : !aplicado && ruta.equals(archivoNuevo) ? archivoViejo : ruta;
    }
}
