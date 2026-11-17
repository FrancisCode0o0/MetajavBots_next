package castillafantasx.datos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ESCRITOR DE SECCIONES — Cambia una línea de un archivo de res/ (los que lee
 * LectorSecciones) sin tocar lo demás: los comentarios y el orden se quedan
 * como estaban. Lo usa el editor para guardar lo que se cambia en él.
 */
public final class EscritorSecciones {

    private EscritorSecciones() { }

    /** Las líneas del archivo (lista vacía si no existe). */
    public static List<String> leer(Path archivo) {
        try {
            if (!Files.exists(archivo)) return new ArrayList<>();
            return new ArrayList<>(Files.readAllLines(archivo, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("No se ha podido leer " + archivo + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Escribe el archivo entero. Devuelve false si no se ha podido. */
    public static boolean escribir(Path archivo, List<String> lineas) {
        try {
            if (archivo.getParent() != null) Files.createDirectories(archivo.getParent());
            castillafantasx.datos.Candado.vigilar(archivo);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
            apuntarEscrito(archivo);
            return true;
        } catch (IOException e) {
            System.err.println("No se ha podido escribir " + archivo + ": " + e.getMessage());
            return false;
        }
    }

    /** Los archivos que acaba de escribir el editor: el vigía de res/ no avisa de ellos (ya están al día en el juego). */
    private static final java.util.Set<Path> ESCRITOS = new java.util.HashSet<>();

    /** Apunta que ese archivo lo ha escrito el propio editor. */
    public static void apuntarEscrito(Path archivo) {
        ESCRITOS.add(archivo.toAbsolutePath().normalize());
    }

    /** ¿Ha escrito el editor ese archivo desde la última vez que se preguntó? (Deja de estar apuntado.) */
    public static boolean loEscribioElEditor(Path archivo) {
        return ESCRITOS.remove(archivo.toAbsolutePath().normalize());
    }

    /** El número de la línea «[seccion]», o -1. */
    private static int cabecera(List<String> lineas, String seccion) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).trim().equals("[" + seccion + "]")) return i;
        }
        return -1;
    }

    /** El número de la línea en la que acaba la sección que empieza en 'cabecera' (la de la cabecera siguiente, o el final). */
    private static int finDe(List<String> lineas, int cabecera) {
        for (int i = cabecera + 1; i < lineas.size(); i++) {
            if (lineas.get(i).trim().startsWith("[")) return i;
        }
        return lineas.size();
    }

    private static boolean esDato(String linea) {
        String t = linea.trim();
        return !t.isEmpty() && !t.startsWith("#");
    }

    /** Lo que dice una línea, sin su comentario del final («  # …») ni espacios de más. */
    private static String sinComentario(String linea) {
        int i = linea.indexOf("  #");
        return (i >= 0 ? linea.substring(0, i) : linea).trim();
    }

    /** Dónde se añade una línea nueva en esa sección: detrás de su último dato (los comentarios de después son de la sección siguiente). */
    private static int sitioParaAnadir(List<String> lineas, int cabecera) {
        int sitio = cabecera + 1;
        for (int i = cabecera + 1; i < finDe(lineas, cabecera); i++) {
            if (esDato(lineas.get(i))) sitio = i + 1;
        }
        // En una sección aún sin datos, detrás de los comentarios que la explican.
        if (sitio == cabecera + 1) {
            while (sitio < lineas.size() && lineas.get(sitio).trim().startsWith("#")) sitio++;
        }
        return sitio;
    }

    /** Añade la sección al final del archivo, con ese comentario encima de sus datos (puede ser null). Devuelve su cabecera. */
    private static int crearSeccion(List<String> lineas, String seccion, String comentario) {
        if (!lineas.isEmpty() && !lineas.get(lineas.size() - 1).trim().isEmpty()) lineas.add("");
        lineas.add("[" + seccion + "]");
        if (comentario != null) {
            for (String c : comentario.split("\n")) lineas.add("# " + c);
        }
        return cabecera(lineas, seccion);
    }

    /**
     * Pone «clave = valor» en esa sección: cambia la línea si ya estaba (y
     * conserva su comentario del final) o la añade. Si falta la sección, la
     * crea al final, con ese comentario (puede ser null).
     */
    public static boolean ponerValor(Path archivo, String seccion, String clave, String valor, String comentario) {
        List<String> lineas = leer(archivo);
        int cabecera = cabecera(lineas, seccion);
        if (cabecera < 0) cabecera = crearSeccion(lineas, seccion, comentario);
        for (int i = cabecera + 1; i < finDe(lineas, cabecera); i++) {
            String linea = lineas.get(i);
            int igual = linea.indexOf('=');
            if (!esDato(linea) || igual < 0 || !linea.substring(0, igual).trim().equals(clave)) continue;
            int cola = linea.indexOf("  #");
            lineas.set(i, clave + " = " + valor + (cola >= 0 ? "   " + linea.substring(cola).trim() : ""));
            return escribir(archivo, lineas);
        }
        lineas.add(sitioParaAnadir(lineas, cabecera), clave + " = " + valor);
        return escribir(archivo, lineas);
    }

    /** Quita la línea «clave = …» de esa sección. Devuelve true si estaba. */
    public static boolean quitarValor(Path archivo, String seccion, String clave) {
        List<String> lineas = leer(archivo);
        int cabecera = cabecera(lineas, seccion);
        if (cabecera < 0) return false;
        for (int i = cabecera + 1; i < finDe(lineas, cabecera); i++) {
            String linea = lineas.get(i);
            int igual = linea.indexOf('=');
            if (esDato(linea) && igual > 0 && linea.substring(0, igual).trim().equals(clave)) {
                lineas.remove(i);
                return escribir(archivo, lineas);
            }
        }
        return false;
    }

    /** Añade una línea al final de los datos de esa sección (la crea si falta, con ese comentario). */
    public static boolean anadirLinea(Path archivo, String seccion, String linea, String comentario) {
        List<String> lineas = leer(archivo);
        int cabecera = cabecera(lineas, seccion);
        if (cabecera < 0) cabecera = crearSeccion(lineas, seccion, comentario);
        lineas.add(sitioParaAnadir(lineas, cabecera), linea);
        return escribir(archivo, lineas);
    }

    /** Añade varias líneas de una vez (una pincelada entera del editor): el archivo se escribe una sola vez. */
    public static boolean anadirLineas(Path archivo, String seccion, List<String> nuevas, String comentario) {
        if (nuevas.isEmpty()) return true;
        List<String> lineas = leer(archivo);
        int cabecera = cabecera(lineas, seccion);
        if (cabecera < 0) cabecera = crearSeccion(lineas, seccion, comentario);
        lineas.addAll(sitioParaAnadir(lineas, cabecera), nuevas);
        return escribir(archivo, lineas);
    }

    /** Quita varias líneas de una vez; las que no estén, se saltan. */
    public static boolean quitarLineas(Path archivo, String seccion, List<String> viejas) {
        if (viejas.isEmpty()) return true;
        List<String> lineas = leer(archivo);
        for (String vieja : viejas) {
            int i = lineaQueDice(lineas, seccion, vieja);
            if (i >= 0) lineas.remove(i);
        }
        return escribir(archivo, lineas);
    }

    /** El número de la línea de esa sección que dice eso mismo (sin contar su comentario ni los espacios de más), o -1. */
    private static int lineaQueDice(List<String> lineas, String seccion, String texto) {
        int cabecera = cabecera(lineas, seccion);
        if (cabecera < 0) return -1;
        String buscada = texto.trim().replaceAll("\\s+", " ");
        for (int i = cabecera + 1; i < finDe(lineas, cabecera); i++) {
            if (esDato(lineas.get(i)) && sinComentario(lineas.get(i)).replaceAll("\\s+", " ").equals(buscada)) return i;
        }
        return -1;
    }

    /** Quita de esa sección la línea que dice eso. Devuelve true si estaba. */
    public static boolean quitarLinea(Path archivo, String seccion, String linea) {
        List<String> lineas = leer(archivo);
        int i = lineaQueDice(lineas, seccion, linea);
        if (i < 0) return false;
        lineas.remove(i);
        return escribir(archivo, lineas);
    }

    /** Cambia en esa sección la línea que dice 'vieja' por 'nueva'. Devuelve true si estaba. */
    public static boolean cambiarLinea(Path archivo, String seccion, String vieja, String nueva) {
        List<String> lineas = leer(archivo);
        int i = lineaQueDice(lineas, seccion, vieja);
        if (i < 0) return false;
        lineas.set(i, nueva);
        return escribir(archivo, lineas);
    }

    /** ¿Tiene esa sección una línea que dice eso? */
    public static boolean tieneLinea(Path archivo, String seccion, String linea) {
        return lineaQueDice(leer(archivo), seccion, linea) >= 0;
    }

    /**
     * Cambia una letra de una rejilla (la sección [mapa] de un mapa): la de
     * esa fila y esa columna, contando solo las filas con datos. Devuelve la
     * letra que había, o 0 si no se ha podido.
     */
    public static char ponerLetra(Path archivo, String seccion, int fila, int columna, char letra) {
        List<String> lineas = leer(archivo);
        int cabecera = cabecera(lineas, seccion);
        if (cabecera < 0) return 0;
        int vista = -1;
        for (int i = cabecera + 1; i < finDe(lineas, cabecera); i++) {
            String linea = lineas.get(i);
            if (!esDato(linea) || ++vista != fila) continue;
            if (columna < 0 || columna >= linea.length()) return 0;
            char antes = linea.charAt(columna);
            lineas.set(i, linea.substring(0, columna) + letra + linea.substring(columna + 1));
            return escribir(archivo, lineas) ? antes : 0;
        }
        return 0;
    }
}
