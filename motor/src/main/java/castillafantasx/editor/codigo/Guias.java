package castillafantasx.editor.codigo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUÍAS — La guía corta de cómo se edita cada parte (el diálogo, el
 * comportamiento, el combate…), con un ejemplo modelo: res/editor/guias/<parte>.txt.
 * La primera línea es el título; las que empiezan por cuatro espacios, el
 * ejemplo (se pintan con la letra del código). La abre el iconito de ayuda
 * del visor, al lado del código.
 */
public final class Guias {

    /** La carpeta, en disco (la que se cambia a mano) y dentro del juego. */
    public static Path carpeta = Path.of("res/editor/guias");
    private static final String RECURSO = "/editor/guias/";
    private static final Map<String, List<String>> leidas = new LinkedHashMap<>();

    private Guias() { }

    /** El nombre del archivo de la guía de esa parte: «lo que hace» → «lo_que_hace». */
    public static String archivoDe(String parte) {
        return IndiceDeNombres.llano(parte).trim().replace(' ', '_');
    }

    /** ¿Tiene guía esa parte? */
    public static boolean hay(String parte) {
        return !de(parte).isEmpty();
    }

    /** Las líneas de la guía de esa parte (vacía si no tiene). */
    public static List<String> de(String parte) {
        if (parte == null) return new ArrayList<>();
        return leidas.computeIfAbsent(archivoDe(parte), Guias::leer);
    }

    /** Olvida lo leído (al guardar una guía, se lee de nuevo). */
    public static void olvidar() {
        leidas.clear();
    }

    private static List<String> leer(String nombre) {
        List<String> lineas = new ArrayList<>();
        Path archivo = carpeta.resolve(nombre + ".txt");
        try (BufferedReader lector = Files.exists(archivo) ? Files.newBufferedReader(archivo, StandardCharsets.UTF_8) : delJuego(nombre)) {
            String linea;
            while (lector != null && (linea = lector.readLine()) != null) {
                if (!linea.startsWith("#")) lineas.add(linea);
            }
        } catch (IOException e) {
            System.err.println("No se ha podido leer la guía " + archivo + ": " + e.getMessage());
        }
        while (!lineas.isEmpty() && lineas.get(lineas.size() - 1).trim().isEmpty()) lineas.remove(lineas.size() - 1);
        return lineas;
    }

    private static BufferedReader delJuego(String nombre) {
        InputStream in = Guias.class.getResourceAsStream(RECURSO + nombre + ".txt");
        return in == null ? null : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** ¿Es una línea del ejemplo (cuatro espacios delante)? */
    public static boolean deEjemplo(String linea) {
        return linea.startsWith("    ");
    }
}
