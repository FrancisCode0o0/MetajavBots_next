package castillafantasx.editor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PROGRAMAS — Los programas de fuera que abre el centro de mando (Tiled,
 * Inky, VS Code, Blender…): cuál es cada uno, dónde está instalado y qué
 * archivos abre. Se leen de res/editor/programas.txt, que Fran cambia a mano.
 */
public final class Programas {

    private Programas() { }

    /** Un programa de fuera. */
    public static final class Programa {
        public final String id;
        public String nombre = "", para = "", grupo = "";
        /** Dónde puede estar instalado; vale el primero que exista. */
        public final List<String> ejecutables = new ArrayList<>();
        /** Los archivos o carpetas del juego que abre. */
        public final List<String> archivos = new ArrayList<>();
        /** Cómo se le llama: {programa}, {archivo} y {linea} se cambian por lo suyo. */
        public String orden = "{programa} {archivo}", ordenConLinea = "";

        Programa(String id) {
            this.id = id;
        }
    }

    /** El archivo, en disco (el que se cambia a mano) y dentro del juego. */
    public static Path archivo = Path.of("res/editor/programas.txt");
    private static final String RECURSO = "/editor/programas.txt";

    private static List<Programa> leidos;

    /** Todos los programas, en el orden del archivo. */
    public static List<Programa> todos() {
        if (leidos == null) leer();
        return leidos;
    }

    public static Programa de(String id) {
        for (Programa p : todos()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    /** Lee el archivo de nuevo (el vigía lo llama cuando cambia). */
    public static void leer() {
        List<Programa> lista = new ArrayList<>();
        try (BufferedReader lector = abrir()) {
            Programa actual = null;
            String linea;
            while (lector != null && (linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("[") && linea.endsWith("]")) {
                    actual = new Programa(linea.substring(1, linea.length() - 1).trim());
                    lista.add(actual);
                    continue;
                }
                int igual = linea.indexOf('=');
                if (actual == null || igual < 0) continue;
                String clave = linea.substring(0, igual).trim(), valor = linea.substring(igual + 1).trim();
                switch (clave) {
                    case "nombre": actual.nombre = valor; break;
                    case "para": actual.para = valor; break;
                    case "grupo": actual.grupo = valor; break;
                    case "orden": actual.orden = valor; break;
                    case "orden_con_linea": actual.ordenConLinea = valor; break;
                    case "ejecutable": actual.ejecutables.add(valor); break;
                    case "archivo": actual.archivos.add(valor); break;
                    default: break;
                }
            }
        } catch (IOException e) {
            System.err.println("No se ha podido leer " + archivo + ": " + e.getMessage());
        }
        leidos = lista;
    }

    private static BufferedReader abrir() throws IOException {
        if (Files.exists(archivo)) return Files.newBufferedReader(archivo, StandardCharsets.UTF_8);
        InputStream in = Programas.class.getResourceAsStream(RECURSO);
        return in == null ? null : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Dónde está instalado: el primero de sus ejecutables que existe (con %VARIABLES% del sistema), o null. */
    public static String ejecutableDe(Programa p) {
        for (String ruta : p.ejecutables) {
            String entera = conVariables(ruta);
            if (Files.exists(Path.of(entera))) return entera;
        }
        return null;
    }

    /** "%LOCALAPPDATA%\Programs" → "C:\Users\…\AppData\Local\Programs". */
    static String conVariables(String ruta) {
        StringBuilder s = new StringBuilder();
        int i = 0;
        while (i < ruta.length()) {
            int abre = ruta.indexOf('%', i), cierra = abre < 0 ? -1 : ruta.indexOf('%', abre + 1);
            if (cierra < 0) break;
            String valor = System.getenv(ruta.substring(abre + 1, cierra));
            s.append(ruta, i, abre).append(valor == null ? "" : valor);
            i = cierra + 1;
        }
        return s.append(ruta.substring(i)).toString();
    }

    /** Los archivos que abre, uno a uno: los de una carpeta salen todos, por orden. */
    public static List<String> archivosDe(Programa p) {
        List<String> lista = new ArrayList<>();
        for (String ruta : p.archivos) {
            Path sitio = Path.of(ruta);
            if (!Files.isDirectory(sitio)) {
                lista.add(ruta);
                continue;
            }
            try (Stream<Path> dentro = Files.list(sitio)) {
                dentro.filter(Files::isRegularFile).map(a -> a.toString().replace('\\', '/')).sorted().forEach(lista::add);
            } catch (IOException e) {
                lista.add(ruta);
            }
        }
        return lista;
    }

    /** La orden para abrir ese archivo (y esa línea, si es mayor que 0 y el programa sabe) con el programa: una lista lista para lanzar. */
    public static List<String> orden(Programa p, String ejecutable, String archivoQueAbre, int linea) {
        String plantilla = linea > 0 && !p.ordenConLinea.isEmpty() ? p.ordenConLinea : p.orden;
        List<String> orden = new ArrayList<>();
        for (String trozo : plantilla.split(" +")) {
            String entero = trozo.replace("{programa}", ejecutable).replace("{linea}", String.valueOf(Math.max(1, linea)))
                    .replace("{archivo}", archivoQueAbre == null ? "" : Path.of(archivoQueAbre).toAbsolutePath().toString());
            if (!entero.isEmpty()) orden.add(entero);
        }
        return orden;
    }

    /** Abre ese archivo en el programa. Devuelve lo que ha pasado, para decírselo a Fran; empieza por «No» si no se ha podido. */
    public static String abrir(String idDePrograma, String archivoQueAbre, int linea) {
        Programa p = de(idDePrograma);
        if (p == null) return "No hay ningún programa «" + idDePrograma + "» en res/editor/programas.txt.";
        String ejecutable = ejecutableDe(p);
        if (ejecutable == null) return "No encuentro " + p.nombre + ": pon dónde está en res/editor/programas.txt.";
        boolean lanzado = Editor.sistema.lanzar(orden(p, ejecutable, archivoQueAbre, linea));
        return lanzado ? p.nombre + " abre " + (archivoQueAbre == null ? "" : archivoQueAbre) : "No se ha podido lanzar " + p.nombre + ".";
    }
}
