package metajavbots.tutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * BIOGRAFÍAS — Las de los robots y las de los entrenadores (res/biografias/),
 * muy absurdas, en archivos de texto fáciles de retocar: [id] y una línea por
 * idioma. Soporta ediciones personalizadas persistidas en la partida del usuario
 * sin modificar nunca los recursos base de res/biografias/, con restauración al
 * texto original.
 */
public final class Biografias {

    private static final Map<String, Map<String, String>> LEIDAS = new LinkedHashMap<>();
    private static final Map<String, String> PERSONALIZADAS = new LinkedHashMap<>();
    private static Path archivoActivo;

    private Biografias() {
    }

    /** La biografía efectiva de un robot (personalizada si existe; si no, la original). */
    public static synchronized String deRobot(String modelo, Idioma i) {
        return obtener("robots", modelo, i);
    }

    /** La biografía efectiva de un entrenador (personalizada si existe; si no, la original). */
    public static synchronized String deEntrenador(String id, Idioma i) {
        return obtener("entrenadores", id, i);
    }

    /** La biografía original de fábrica de un robot (desde res/biografias/robots.txt). */
    public static synchronized String baseDeRobot(String modelo, Idioma i) {
        return deBase("robots", modelo, i);
    }

    /** La biografía original de fábrica de un entrenador (desde res/biografias/entrenadores.txt). */
    public static synchronized String baseDeEntrenador(String id, Idioma i) {
        return deBase("entrenadores", id, i);
    }

    /** Indica si la biografía de ese robot en ese idioma ha sido editada por el jugador. */
    public static synchronized boolean esPersonalizadaRobot(String modelo, Idioma i) {
        return PERSONALIZADAS.containsKey(clave("robots", modelo, i));
    }

    /** Indica si la biografía de ese entrenador en ese idioma ha sido editada por el jugador. */
    public static synchronized boolean esPersonalizadaEntrenador(String id, Idioma i) {
        return PERSONALIZADAS.containsKey(clave("entrenadores", id, i));
    }

    /** Guarda una biografía editada de un robot y la persiste en el archivo de usuario activo. */
    public static synchronized void guardarRobot(String modelo, Idioma i, String texto) {
        guardar("robots", modelo, i, texto);
    }

    /** Guarda una biografía editada de un entrenador y la persiste en el archivo de usuario activo. */
    public static synchronized void guardarEntrenador(String id, Idioma i, String texto) {
        guardar("entrenadores", id, i, texto);
    }

    /** Restaura la biografía original de un robot en ese idioma y persiste el cambio. */
    public static synchronized void restaurarRobot(String modelo, Idioma i) {
        restaurar("robots", modelo, i);
    }

    /** Restaura la biografía original de un entrenador en ese idioma y persiste el cambio. */
    public static synchronized void restaurarEntrenador(String id, Idioma i) {
        restaurar("entrenadores", id, i);
    }

    /** Activa las biografías personalizadas de la partida del jugador (partidas/<jugador>/biografias.txt). */
    public static synchronized void usarPartida(Jugador jugador) {
        if (jugador == null || jugador.nombre == null || jugador.nombre.isBlank()) {
            limpiarPersonalizadas();
            return;
        }
        Path archivo = Guardado.carpeta.resolve(Guardado.carpetaDe(jugador.nombre)).resolve("biografias.txt");
        cargarPersonalizadas(archivo);
    }

    /** Carga las biografías personalizadas desde un archivo de usuario y lo deja activo para futuros guardados. */
    public static synchronized void cargarPersonalizadas(Path archivo) {
        PERSONALIZADAS.clear();
        archivoActivo = archivo;
        if (archivo == null || !Files.isRegularFile(archivo)) return;
        try {
            String seccion = "";
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String l = linea.strip();
                if (l.isEmpty() || l.startsWith("#")) continue;
                if (l.startsWith("[") && l.endsWith("]")) {
                    seccion = l.substring(1, l.length() - 1).strip();
                } else if (l.length() > 3 && l.charAt(2) == ':' && !seccion.isEmpty()) {
                    String cod = l.substring(0, 2);
                    String val = l.substring(3).strip();
                    if (!val.isEmpty()) PERSONALIZADAS.put(seccion + "." + cod, val);
                }
            }
        } catch (IOException e) {
            // archivo ilegible: se mantienen las biografías de fábrica
        }
    }

    /** Limpia las biografías personalizadas en memoria y desactiva el archivo de usuario. */
    public static synchronized void limpiarPersonalizadas() {
        PERSONALIZADAS.clear();
        archivoActivo = null;
    }

    private static String obtener(String archivo, String id, Idioma i) {
        String custom = PERSONALIZADAS.get(clave(archivo, id, i));
        return custom != null ? custom : deBase(archivo, id, i);
    }

    private static void guardar(String archivo, String id, Idioma i, String texto) {
        if (id == null || id.isBlank() || i == null) return;
        String limpio = texto == null ? "" : texto.replace('\r', ' ').replace('\n', ' ').strip();
        String base = deBase(archivo, id, i);
        String k = clave(archivo, id, i);
        if (limpio.isEmpty() || limpio.equals(base)) PERSONALIZADAS.remove(k);
        else PERSONALIZADAS.put(k, limpio);
        persistir();
    }

    private static void restaurar(String archivo, String id, Idioma i) {
        if (id == null || id.isBlank() || i == null) return;
        if (PERSONALIZADAS.remove(clave(archivo, id, i)) != null) persistir();
    }

    private static void persistir() {
        if (archivoActivo == null) return;
        try {
            if (PERSONALIZADAS.isEmpty()) {
                Files.deleteIfExists(archivoActivo);
                return;
            }
            Path padre = archivoActivo.getParent();
            if (padre != null) Files.createDirectories(padre);
            Map<String, Map<String, String>> porSeccion = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : PERSONALIZADAS.entrySet()) {
                int punto = e.getKey().lastIndexOf('.');
                if (punto <= 0) continue;
                String sec = e.getKey().substring(0, punto);
                String cod = e.getKey().substring(punto + 1);
                porSeccion.computeIfAbsent(sec, k -> new LinkedHashMap<>()).put(cod, e.getValue());
            }
            StringBuilder sb = new StringBuilder("# Biografías personalizadas de MetajavBots (no modifica res/biografias/)\n");
            for (Map.Entry<String, Map<String, String>> sec : porSeccion.entrySet()) {
                sb.append('[').append(sec.getKey()).append("]\n");
                for (Map.Entry<String, String> kv : sec.getValue().entrySet()) {
                    sb.append(kv.getKey()).append(": ").append(kv.getValue()).append('\n');
                }
                sb.append('\n');
            }
            Files.writeString(archivoActivo, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // no interrumpir el juego si falla el guardado en disco
        }
    }

    private static String clave(String archivo, String id, Idioma i) {
        return archivo + ":" + id + "." + i.codigo();
    }

    private static String deBase(String archivo, String id, Idioma i) {
        Map<String, String> m = LEIDAS.computeIfAbsent(archivo, Biografias::leer);
        return m.getOrDefault(id + "." + i.codigo(), "");
    }

    private static Map<String, String> leer(String archivo) {
        Map<String, String> m = new LinkedHashMap<>();
        try (InputStream in = Biografias.class.getResourceAsStream("/biografias/" + archivo + ".txt")) {
            if (in == null) return m;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String id = "", linea;
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("#") || linea.isBlank()) continue;
                if (linea.startsWith("[")) id = linea.substring(1, linea.indexOf(']')).strip();
                else if (linea.length() > 3 && linea.charAt(2) == ':') m.put(id + "." + linea.substring(0, 2), linea.substring(3).strip());
            }
        } catch (Exception e) {
            // sin biografías: el juego sigue
        }
        return m;
    }
}
