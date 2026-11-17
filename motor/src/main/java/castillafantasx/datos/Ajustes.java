package castillafantasx.datos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * AJUSTES — Las cifras de cada ser cambiadas en el panel de la arena
 * (docs/encargos_viejos/ENCARGO_AJUSTES.md). Las de fábrica son las de res/datos/ y el código,
 * y nunca se tocan: aquí solo se apunta lo que cambia, ser por ser.
 *
 * Lo cambiado se guarda como un juego de cifras con nombre («jabato duro»),
 * cada uno en su archivo de la carpeta ajustes/. El juego elegido vale en todo
 * el juego, también en la versión publicada, hasta que se vuelva a las de fábrica.
 *
 * Una cifra de un archivo de datos se nombra "combate.txt [criaturas] jabato 2"
 * (archivo, sección, clave y columna); una del código, por su nombre ("velocidad").
 */
public final class Ajustes {

    private Ajustes() { }

    /** La carpeta de los juegos de cifras (junto a partida.txt). Las pruebas la cambian por una temporal. */
    public static Path carpeta = Path.of("ajustes");
    private static final String ELEGIDO = "elegido.txt";       // el nombre del juego que vale ahora
    private static final String EXTENSION = ".txt";

    // Lo cambiado: ser -> (cifra -> valor). Vacío = todo de fábrica.
    private static final Map<String, Map<String, Double>> CAMBIADAS = new TreeMap<>();
    // El ser cuyas cifras se están leyendo, cuando la línea de datos la comparten varios (el rango de una persona).
    private static String quien;
    /** El juego de cifras que vale ahora, por el nombre de su archivo ("jabato_duro"; null: las de fábrica). */
    private static String juego;
    // Se ha cambiado algo desde que se cargó o se guardó el juego.
    private static boolean sinGuardar;

    // ------------------------------------------------------------------
    // Las cifras, cambiadas o de fábrica
    // ------------------------------------------------------------------

    /** El nombre de una cifra de un archivo de datos: "combate.txt [criaturas] jabato 2" (la columna, desde 1). */
    public static String cifraDeArchivo(String archivo, String seccion, String clave, int columna) {
        return archivo + " [" + seccion + "] " + clave + " " + columna;
    }

    /** Lo que vale una cifra del código de ese ser: la cambiada o, si no, la de fábrica. */
    public static double numero(String ser, String cifra, double deFabrica) {
        if (CAMBIADAS.isEmpty() || ser == null) return deFabrica;
        Map<String, Double> suyas = CAMBIADAS.get(ser);
        Double valor = suyas == null ? null : suyas.get(cifra);
        return valor == null ? deFabrica : valor;
    }

    /** La cifra cambiada de ese ser, o null si está como de fábrica. */
    public static Double cambiada(String ser, String cifra) {
        Map<String, Double> suyas = CAMBIADAS.get(ser);
        return suyas == null ? null : suyas.get(cifra);
    }

    /** Cambia una cifra de un ser. Si se deja igual que la de fábrica, deja de estar cambiada. */
    public static void cambiar(String ser, String cifra, double valor, double deFabrica) {
        if (valor == deFabrica) {
            volverAFabrica(ser, cifra);
            return;
        }
        CAMBIADAS.computeIfAbsent(ser, s -> new TreeMap<>()).put(cifra, valor);
        sinGuardar = true;
    }

    /** Una cifra de un ser vuelve a la de fábrica. */
    public static void volverAFabrica(String ser, String cifra) {
        Map<String, Double> suyas = CAMBIADAS.get(ser);
        if (suyas == null || suyas.remove(cifra) == null) return;
        if (suyas.isEmpty()) CAMBIADAS.remove(ser);
        sinGuardar = true;
    }

    /** Todas las cifras de un ser vuelven a las de fábrica. */
    public static void volverAFabrica(String ser) {
        if (CAMBIADAS.remove(ser) != null) sinGuardar = true;
    }

    /** Todo vuelve a las de fábrica, y deja de haber juego elegido (también para la próxima vez que se juegue). */
    public static void todoDeFabrica() {
        CAMBIADAS.clear();
        juego = null;
        sinGuardar = false;
        elegir(null);
    }

    /** ¿Hay cambios desde que se cargó o se guardó el juego (o, sin juego, desde las de fábrica)? */
    public static boolean sinGuardar() {
        return sinGuardar;
    }

    /** ¿Tiene ese ser alguna cifra cambiada? */
    public static boolean tieneCambios(String ser) {
        return CAMBIADAS.containsKey(ser);
    }

    /** ¿Hay alguna cifra cambiada? */
    public static boolean hayCambios() {
        return !CAMBIADAS.isEmpty();
    }

    /** Lee algo con las cifras de ese ser (las de su rango, en una persona). */
    public static <T> T para(String ser, Supplier<T> que) {
        String antes = quien;
        quien = ser;
        try {
            return que.get();
        } finally {
            quien = antes;
        }
    }

    /**
     * El texto de una línea de datos con las cifras cambiadas puestas. Lo
     * llama LectorSecciones cada vez que lee. Vale lo cambiado del ser que se
     * esté leyendo o, si no se lee ninguno en concreto, el del dueño de la
     * línea: el de la clave ("jabato = …") o el de la sección ("[heroe]").
     */
    static String aplicar(String origen, String seccion, String clave, String texto) {
        if (CAMBIADAS.isEmpty() || texto == null) return texto;
        String archivo = origen.substring(origen.lastIndexOf('/') + 1);
        if (quien != null) return poner(CAMBIADAS.get(quien), archivo, seccion, clave, texto);
        String conLaClave = poner(CAMBIADAS.get(clave), archivo, seccion, clave, texto);
        return poner(CAMBIADAS.get(seccion), archivo, seccion, clave, conLaClave);
    }

    /** Pone en el texto, columna a columna, las cifras de esa línea que estén en 'suyas'. */
    private static String poner(Map<String, Double> suyas, String archivo, String seccion, String clave, String texto) {
        if (suyas == null) return texto;
        String[] partes = texto.trim().split("\\s+");
        boolean cambia = false;
        for (int i = 0; i < partes.length; i++) {
            Double valor = suyas.get(cifraDeArchivo(archivo, seccion, clave, i + 1));
            if (valor == null) continue;
            partes[i] = escrita(valor);
            cambia = true;
        }
        return cambia ? String.join(" ", partes) : texto;
    }

    /** Una cifra como se escribe en los archivos: "12" o "0.35". */
    public static String escrita(double valor) {
        if (valor == Math.rint(valor) && Math.abs(valor) < 1e9) return Long.toString((long) valor);
        String texto = String.format(java.util.Locale.ROOT, "%.4f", valor);
        return texto.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // ------------------------------------------------------------------
    // Los juegos de cifras con nombre
    // ------------------------------------------------------------------

    /** El juego de cifras que vale ahora, por el nombre de su archivo ("jabato_duro"), o null si son las de fábrica. */
    public static String juego() {
        return juego;
    }

    /** Los nombres de los juegos guardados, por orden alfabético. */
    public static List<String> juegos() {
        List<String> nombres = new ArrayList<>();
        if (!Files.isDirectory(carpeta)) return nombres;
        try (var archivos = Files.list(carpeta)) {
            archivos.forEach(a -> {
                String nombre = a.getFileName().toString();
                if (nombre.endsWith(EXTENSION) && !nombre.equals(ELEGIDO)) nombres.add(nombre.substring(0, nombre.length() - EXTENSION.length()));
            });
        } catch (IOException e) {
            System.err.println("ajustes: no se pudo mirar la carpeta: " + e);
        }
        Collections.sort(nombres);
        return nombres;
    }

    /** Guarda lo cambiado como un juego con ese nombre (si ya existe, lo reescribe) y lo deja elegido. */
    public static void guardar(String nombre) {
        List<String> lineas = new ArrayList<>();
        lineas.add("# Juego de cifras «" + nombre + "», hecho en el panel de la arena (docs/encargos_viejos/ENCARGO_AJUSTES.md).");
        lineas.add("# Solo lleva lo que cambia; lo demás es de fábrica (res/datos/ y el código). Se puede retocar a mano.");
        for (Map.Entry<String, Map<String, Double>> ser : CAMBIADAS.entrySet()) {
            lineas.add("");
            lineas.add("[" + ser.getKey() + "]");
            for (Map.Entry<String, Double> cifra : ser.getValue().entrySet()) {
                lineas.add(cifra.getKey() + " = " + escrita(cifra.getValue()));
            }
        }
        try {
            Files.createDirectories(carpeta);
            castillafantasx.datos.Candado.vigilar(carpeta.resolve(baseDe(nombre) + EXTENSION));
            Files.write(carpeta.resolve(baseDe(nombre) + EXTENSION), lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el juego de cifras «" + nombre + "»", e);
        }
        juego = baseDe(nombre);
        sinGuardar = false;
        elegir(juego);
    }

    /** Carga un juego guardado en lugar de lo cambiado hasta ahora, y lo deja elegido. Devuelve si existía. */
    public static boolean cargar(String nombre) {
        Path archivo = carpeta.resolve(baseDe(nombre) + EXTENSION);
        if (!Files.exists(archivo)) return false;
        CAMBIADAS.clear();
        try {
            String ser = null;
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                String limpia = linea.trim();
                if (limpia.isEmpty() || limpia.startsWith("#")) continue;
                if (limpia.startsWith("[") && limpia.endsWith("]") && limpia.indexOf(']') == limpia.length() - 1) {
                    ser = limpia.substring(1, limpia.length() - 1);
                    continue;
                }
                int igual = limpia.lastIndexOf('=');
                if (ser == null || igual < 0) continue;
                try {
                    double valor = Double.parseDouble(limpia.substring(igual + 1).trim());
                    CAMBIADAS.computeIfAbsent(ser, s -> new TreeMap<>()).put(limpia.substring(0, igual).trim(), valor);
                } catch (NumberFormatException e) {
                    System.err.println("ajustes: «" + limpia + "» no es una cifra (" + nombre + ")");
                }
            }
        } catch (IOException e) {
            System.err.println("ajustes: no se pudo leer «" + nombre + "»: " + e);
            return false;
        }
        juego = baseDe(nombre);
        sinGuardar = false;
        elegir(juego);
        return true;
    }

    /** Al arrancar el juego: carga el juego de cifras que se dejó elegido, si lo hay. */
    public static void cargarElElegido() {
        CAMBIADAS.clear();
        juego = null;
        sinGuardar = false;
        Path archivo = carpeta.resolve(ELEGIDO);
        if (!Files.exists(archivo)) return;
        try {
            String nombre = Files.readString(archivo, StandardCharsets.UTF_8).trim();
            if (!nombre.isEmpty() && !cargar(nombre)) elegir(null);
        } catch (IOException e) {
            System.err.println("ajustes: no se pudo leer " + ELEGIDO + ": " + e);
        }
    }

    /** Apunta qué juego vale para la próxima vez (null: las de fábrica, y se borra la nota). */
    private static void elegir(String nombre) {
        Path archivo = carpeta.resolve(ELEGIDO);
        try {
            if (nombre == null) {
                Files.deleteIfExists(archivo);
            } else {
                Files.createDirectories(carpeta);
                castillafantasx.datos.Candado.vigilar(archivo);
                Files.writeString(archivo, nombre + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("ajustes: no se pudo apuntar el juego elegido: " + e);
        }
    }

    /** El nombre del archivo de un juego, sin ".txt": «jabato duro» → "jabato_duro". Se queda con letras, números, guiones y espacios. */
    private static String baseDe(String nombre) {
        return nombre.trim().replaceAll("[^\\p{L}\\p{N} _-]", "").replace(' ', '_');
    }

    /** El nombre que se lee de un archivo: "jabato_duro" → "jabato duro". */
    public static String legible(String nombre) {
        return nombre == null ? null : nombre.replace('_', ' ');
    }
}
