package castillafantasx.datos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LECTOR DE SECCIONES — Lee los archivos de res/mundo: texto con secciones
 * "[nombre]", comentarios con '#' y líneas "clave = valor".
 */
public final class LectorSecciones {

    private final Map<String, List<String>> secciones = new LinkedHashMap<>();
    private final String origen;

    /** Lee un recurso del juego, por ejemplo "/mundo/superficie.txt". */
    public LectorSecciones(String recurso) {
        this.origen = recurso;
        try (InputStream in = LectorSecciones.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Falta el archivo " + recurso);
            }
            leer(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + recurso, e);
        }
    }

    /** Lee un archivo del disco (el editor relee así lo que se cambia fuera con el juego abierto). */
    public LectorSecciones(java.nio.file.Path archivo) {
        this.origen = archivo.toString();
        try (BufferedReader lector = java.nio.file.Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            leer(lector);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
    }

    private void leer(BufferedReader lector) throws IOException {
        List<String> actual = null;
        String linea;
        while ((linea = lector.readLine()) != null) {
            String limpia = quitarComentario(linea);
            if (limpia.trim().isEmpty()) continue;
            if (limpia.trim().startsWith("[")) {
                String nombre = limpia.trim().replace("[", "").replace("]", "");
                actual = new ArrayList<>();
                secciones.put(nombre, actual);
            } else if (actual != null) {
                actual.add(limpia);
            }
        }
    }

    /** Un '#' al principio de la línea, o precedido de dos espacios, empieza un comentario. */
    private static String quitarComentario(String linea) {
        if (linea.trim().startsWith("#")) return "";
        int i = linea.indexOf("  #");
        String sin = (i >= 0) ? linea.substring(0, i) : linea;
        // Se quitan los espacios del final, pero no los del principio (un mapa puede empezar por espacio).
        return sin.replaceAll("\\s+$", "");
    }

    /** Las líneas de una sección, tal cual (lista vacía si no existe). */
    public List<String> lineas(String seccion) {
        List<String> l = secciones.get(seccion);
        return l == null ? new ArrayList<>() : l;
    }

    /** El valor de "clave = valor" dentro de una sección, o 'siNoEsta'. Lleva puestas las cifras cambiadas en el panel de la arena (Ajustes). */
    public String valor(String seccion, String clave, String siNoEsta) {
        for (String linea : lineas(seccion)) {
            int igual = linea.indexOf('=');
            if (igual > 0 && linea.substring(0, igual).trim().equals(clave)) {
                return Ajustes.aplicar(origen, seccion, clave, linea.substring(igual + 1).trim());
            }
        }
        return siNoEsta;
    }

    /** Los nombres de todas las secciones, en el orden del archivo. */
    public List<String> secciones() {
        return new ArrayList<>(secciones.keySet());
    }

    /** Para los mensajes de error: de qué archivo hablamos. */
    public String origen() {
        return origen;
    }
}
