package metajavbots.codigo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * COMPILADOR — Compila en memoria el código del jugador (sus robots, sus
 * ejercicios, sus pruebas) con el javac que trae el JDK: sin escribir nada
 * en disco. Antes le mete el contador de pasos (Inyector); después pasa el
 * Vigilante por cada clase. Los errores salen con su línea y su columna, las
 * del jugador.
 *
 * Hace falta un JDK (con solo un JRE no hay javac): hayCompilador() lo dice,
 * y el juego avisa con calma y deja pelear con los robots de fábrica.
 */
public final class Compilador {

    /** Un error (o un aviso) de compilación, con la línea y la columna del jugador (desde 1). */
    public static final class Fallo {
        public final String archivo, codigo, mensaje;
        public final long linea, columna;
        public final boolean grave;

        public Fallo(String archivo, long linea, long columna, String codigo, String mensaje, boolean grave) {
            this.archivo = archivo;
            this.linea = linea;
            this.columna = columna;
            this.codigo = codigo == null ? "" : codigo;
            this.mensaje = mensaje == null ? "" : mensaje;
            this.grave = grave;
        }

        @Override
        public String toString() {
            return archivo + ":" + linea + ":" + columna + ": " + mensaje;
        }
    }

    /** Lo que sale de compilar. */
    public static final class Resultado {
        public final boolean bien;
        public final List<Fallo> errores;
        public final Map<String, byte[]> clases;
        public final long milisegundos;

        public Resultado(boolean bien, List<Fallo> errores, Map<String, byte[]> clases, long ms) {
            this.bien = bien;
            this.errores = Collections.unmodifiableList(errores);
            this.clases = Collections.unmodifiableMap(clases);
            this.milisegundos = ms;
        }

        public List<Fallo> graves() {
            List<Fallo> l = new ArrayList<>();
            for (Fallo e : errores) if (e.grave) l.add(e);
            return l;
        }
    }

    private static JavaCompiler javac;
    private static StandardJavaFileManager archivos;
    private static volatile boolean buscado;
    private static volatile String rutaClases;

    private Compilador() {
    }

    /** ¿Hay javac? (no lo hay si el juego corre con un JRE en vez de un JDK). */
    public static boolean hayCompilador() {
        if (!buscado) {
            synchronized (Compilador.class) {
                if (!buscado) {
                    try {
                        javac = ToolProvider.getSystemJavaCompiler();
                    } catch (Throwable e) {
                        javac = null;
                    }
                    buscado = true;
                }
            }
        }
        return javac != null;
    }

    /** La versión de Java con la que corre el juego (17, 21, 25…). */
    public static int versionDeJava() {
        return Runtime.version().feature();
    }

    /** El nombre de la clase pública de un código (o de la primera clase, si no hay pública). */
    public static String nombreDeClase(String fuente) {
        List<Lexico.Trozo> t = Lexico.codigo(fuente);
        String primera = null;
        for (int i = 0; i + 1 < t.size(); i++) {
            Lexico.Trozo x = t.get(i);
            if ((x.es("class") || x.es("interface") || x.es("enum") || x.es("record"))
                    && t.get(i + 1).clase == Lexico.Clase.NOMBRE && !(i > 0 && t.get(i - 1).es("."))) {
                String n = t.get(i + 1).texto;
                if (primera == null) primera = n;
                for (int k = i - 1; k >= 0 && k >= i - 4; k--) {
                    if (t.get(k).es("public")) return n;
                    if (t.get(k).es(";") || t.get(k).es("}") || t.get(k).es("{")) break;
                }
            }
        }
        return primera == null ? "SinNombre" : primera;
    }

    /**
     * Compila varios archivos (nombre de clase → código). Con contarPasos, les mete el Tope antes.
     * Con un modo, el Vigilante revisa cada clase y lo prohibido sale como error.
     */
    public static synchronized Resultado compilar(Map<String, String> fuentes, boolean contarPasos, Vigilante.Modo modo, Locale idioma) {
        return compilar(fuentes, contarPasos, false, modo, idioma);
    }

    /** Con traza, cada sentencia apunta su línea al ejecutarse (el modo máquina contra máquina). */
    public static synchronized Resultado compilar(Map<String, String> fuentes, boolean contarPasos, boolean traza, Vigilante.Modo modo, Locale idioma) {
        long t0 = System.currentTimeMillis();
        List<Fallo> errores = new ArrayList<>();
        if (!hayCompilador()) {
            errores.add(new Fallo("", 0, 0, "metajavbots.sin.jdk", "No hay compilador de Java (hace falta un JDK)", true));
            return new Resultado(false, errores, new LinkedHashMap<>(), 0);
        }
        Map<String, Inyector.Inyectado> inyectados = new LinkedHashMap<>();
        List<JavaFileObject> unidades = new ArrayList<>();
        for (Map.Entry<String, String> e : fuentes.entrySet()) {
            String fuente = Documento.normalizar(e.getValue());
            Inyector.Inyectado in = contarPasos ? Inyector.inyectar(fuente, traza) : null;
            inyectados.put(e.getKey(), in);
            unidades.add(new Fuente(e.getKey(), in != null ? in.fuente : fuente));
        }
        Map<String, ByteArrayOutputStream> salida = new LinkedHashMap<>();
        DiagnosticCollector<JavaFileObject> diagnosticos = new DiagnosticCollector<>();
        try {
            if (archivos == null) archivos = javac.getStandardFileManager(null, idioma, java.nio.charset.StandardCharsets.UTF_8);
            JavaFileManager enMemoria = new EnMemoria(archivos, salida);
            List<String> opciones = new ArrayList<>(List.of("-g", "-proc:none", "-implicit:none", "-Xlint:none", "-nowarn",
                    "-encoding", "UTF-8", "-XDuseUnsharedTable=true"));
            opciones.add("-classpath");
            opciones.add(classpath());
            JavaCompiler.CompilationTask tarea = javac.getTask(null, enMemoria, diagnosticos, opciones, null, unidades);
            tarea.setLocale(idioma);
            boolean bien = tarea.call();
            for (Diagnostic<? extends JavaFileObject> d : diagnosticos.getDiagnostics()) {
                if (d.getKind() != Diagnostic.Kind.ERROR && d.getKind() != Diagnostic.Kind.WARNING
                        && d.getKind() != Diagnostic.Kind.MANDATORY_WARNING) continue;
                String archivo = d.getSource() == null ? "" : nombreDe(d.getSource());
                Inyector.Inyectado in = inyectados.get(archivo);
                long linea = d.getLineNumber(), col = d.getColumnNumber();
                if (in != null && d.getPosition() >= 0 && linea > 0) {
                    long pos = in.original(d.getPosition());
                    col = columnaDe(fuentes.get(archivo), pos);
                }
                String mensaje = limpiar(d.getMessage(idioma));
                errores.add(new Fallo(archivo, linea, col, d.getCode(), mensaje, d.getKind() == Diagnostic.Kind.ERROR));
            }
            Map<String, byte[]> clases = new LinkedHashMap<>();
            for (Map.Entry<String, ByteArrayOutputStream> e : salida.entrySet()) clases.put(e.getKey(), e.getValue().toByteArray());
            if (bien && modo != null && modo != Vigilante.Modo.CONFIANZA) {
                Set<String> propias = new HashSet<>();
                for (String n : clases.keySet()) propias.add(n.replace('.', '/'));
                for (Map.Entry<String, byte[]> e : clases.entrySet()) {
                    for (String cosa : Vigilante.revisar(e.getValue(), propias, modo)) {
                        String archivo = archivoDeClase(e.getKey(), fuentes);
                        long linea = lineaDe(fuentes.get(archivo), cosa);
                        errores.add(new Fallo(archivo, linea, 1, "metajavbots.prohibido", cosa, true));
                        bien = false;
                    }
                }
            }
            return new Resultado(bien, errores, bien ? clases : new LinkedHashMap<>(), System.currentTimeMillis() - t0);
        } catch (RuntimeException | LinkageError e) {
            errores.add(new Fallo("", 0, 0, "metajavbots.fallo", "El compilador ha fallado: " + e, true));
            return new Resultado(false, errores, new LinkedHashMap<>(), System.currentTimeMillis() - t0);
        }
    }

    /** Lo de siempre: un solo archivo. */
    public static Resultado compilar(String fuente, boolean contarPasos, Vigilante.Modo modo, Locale idioma) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(nombreDeClase(fuente), fuente);
        return compilar(m, contarPasos, modo, idioma);
    }

    private static String classpath() {
        String cp = rutaClases;
        if (cp != null) return cp;
        cp = System.getProperty("java.class.path", "");
        // Si se juega desde un IDE o un lanzador que no pone el classpath, al menos la carpeta o el jar del juego.
        try {
            String propio = new File(Compilador.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            if (!cp.contains(propio)) cp = cp.isEmpty() ? propio : cp + File.pathSeparator + propio;
        } catch (Exception | Error e) {
            // sin más
        }
        rutaClases = cp;
        return cp;
    }

    private static String nombreDe(JavaFileObject f) {
        String n = f.getName();
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.endsWith(".java") ? n.substring(0, n.length() - 5) : n;
    }

    private static String limpiar(String m) {
        if (m == null) return "";
        // javac pone a veces el archivo delante: «/MiRobot.java:3: error: …»
        return m.replaceAll("^\\S+\\.java:\\d+: ", "").strip();
    }

    static long columnaDe(String fuente, long pos) {
        if (fuente == null) return 1;
        int p = (int) Math.min(pos, fuente.length());
        int ini = fuente.lastIndexOf('\n', p - 1) + 1;
        return p - ini + 1;
    }

    private static String archivoDeClase(String clase, Map<String, String> fuentes) {
        String base = clase.contains("$") ? clase.substring(0, clase.indexOf('$')) : clase;
        base = base.substring(base.lastIndexOf('.') + 1);
        if (fuentes.containsKey(base)) return base;
        for (Map.Entry<String, String> e : fuentes.entrySet()) {
            if (Pattern.compile("\\b(class|interface|enum|record)\\s+" + Pattern.quote(base) + "\\b").matcher(e.getValue()).find()) return e.getKey();
        }
        return fuentes.keySet().iterator().next();
    }

    /** La primera línea donde aparece lo prohibido (su nombre corto), para señalarla en el editor. */
    private static long lineaDe(String fuente, String cosa) {
        if (fuente == null) return 1;
        String corto = cosa.contains(".") ? cosa.substring(cosa.lastIndexOf('.') + 1) : cosa;
        String clase = cosa.contains(".") ? cosa.substring(0, cosa.lastIndexOf('.')) : cosa;
        clase = clase.substring(clase.lastIndexOf('.') + 1);
        String[] lineas = fuente.split("\n", -1);
        for (String buscar : new String[]{clase + "." + corto, corto, clase}) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(buscar) + "\\b").matcher("");
            for (int i = 0; i < lineas.length; i++) if (m.reset(lineas[i]).find()) return i + 1;
        }
        return 1;
    }

    // ---------------------------------------------------------------- archivos en memoria

    private static final class Fuente extends SimpleJavaFileObject {
        private final String codigo;

        Fuente(String nombre, String codigo) {
            super(URI.create("string:///" + nombre.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.codigo = codigo;
        }

        @Override
        public CharSequence getCharContent(boolean ignorarErrores) {
            return codigo;
        }
    }

    private static final class Clase extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes;

        Clase(String nombre, ByteArrayOutputStream bytes) {
            super(URI.create("bytes:///" + nombre.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.bytes = bytes;
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }

    private static final class EnMemoria extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, ByteArrayOutputStream> salida;

        EnMemoria(StandardJavaFileManager m, Map<String, ByteArrayOutputStream> salida) {
            super(m);
            this.salida = salida;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location donde, String nombre, JavaFileObject.Kind tipo, FileObject hermano) throws IOException {
            if (donde == StandardLocation.CLASS_OUTPUT && tipo == JavaFileObject.Kind.CLASS) {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                salida.put(nombre, b);
                return new Clase(nombre, b);
            }
            return super.getJavaFileForOutput(donde, nombre, tipo, hermano);
        }
    }
}
