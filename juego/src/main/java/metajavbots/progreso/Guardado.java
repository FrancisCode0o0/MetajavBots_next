package metajavbots.progreso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import metajavbots.cerebro.Ranura;
import metajavbots.cerebro.Tarea;
import metajavbots.piezas.Montaje;
import metajavbots.textos.Idioma;

/**
 * GUARDADO — Lee y escribe las partidas en partidas/<nombre>/: partida.txt
 * (clave = valor), robot_N.java (el código de cada robot, tal cual, para que
 * se pueda abrir con cualquier editor), ejercicios/<id>.java e historial.txt
 * (una pelea por línea). Se escribe primero en un archivo aparte y luego se
 * cambia por el bueno: si el juego se cierra a medias, la partida no se rompe.
 */
public final class Guardado {

    /** Dónde se guardan (se cambia en las pruebas). */
    public static Path carpeta = Paths.get("partidas");

    private Guardado() {
    }

    public static List<String> partidas() {
        List<String> l = new ArrayList<>();
        if (!Files.isDirectory(carpeta)) return l;
        try (Stream<Path> s = Files.list(carpeta)) {
            s.filter(p -> Files.exists(p.resolve("partida.txt"))).forEach(p -> l.add(p.getFileName().toString()));
        } catch (IOException e) {
            // sin partidas
        }
        l.sort(String.CASE_INSENSITIVE_ORDER);
        return l;
    }

    /** Adonde van las partidas borradas (no se listan; si fue sin querer, se puede sacar de ahí a mano). */
    public static final String BORRADAS = "_borradas";

    /** Borra una partida: la saca de la lista y la deja en partidas/_borradas/, con la fecha. */
    public static void borrar(String carpetaDelJugador) throws IOException {
        Path origen = carpeta.resolve(carpetaDelJugador);
        if (!Files.isDirectory(origen) || carpetaDelJugador.equals(BORRADAS)) return;
        Path destino = carpeta.resolve(BORRADAS).resolve(carpetaDelJugador + "_" + System.currentTimeMillis());
        Files.createDirectories(destino.getParent());
        Files.move(origen, destino);
    }

    /**
     * Cambia el nombre de una partida (y el de su carpeta). Devuelve la carpeta nueva; si ya hay otra partida con ese
     * nombre, no toca nada y lanza un error.
     */
    public static String renombrar(String carpetaDelJugador, String nombreNuevo) throws IOException {
        String nueva = carpetaDe(nombreNuevo);
        Jugador j = leer(carpetaDelJugador);
        boolean mismaCarpeta = nueva.equalsIgnoreCase(carpetaDelJugador);
        if (!mismaCarpeta && Files.exists(carpeta.resolve(nueva))) throw new IOException("ya existe");
        if (!mismaCarpeta) Files.move(carpeta.resolve(carpetaDelJugador), carpeta.resolve(nueva));
        else if (!nueva.equals(carpetaDelJugador)) {
            // Solo cambian mayúsculas: en Windows hay que pasar por un nombre intermedio.
            Path paso = carpeta.resolve(nueva + "_" + System.nanoTime());
            Files.move(carpeta.resolve(carpetaDelJugador), paso);
            Files.move(paso, carpeta.resolve(nueva));
        }
        j.nombre = nombreNuevo.strip();
        guardar(j);
        return nueva;
    }

    /** El nombre de la carpeta de un jugador (sin letras que el sistema no acepte). */
    public static String carpetaDe(String nombre) {
        String s = nombre.strip().replaceAll("[^\\p{L}\\p{N} _-]", "").replace(' ', '_');
        return s.isEmpty() ? "jugador" : s.length() > 40 ? s.substring(0, 40) : s;
    }

    public static void guardar(Jugador j) throws IOException {
        Path dir = carpeta.resolve(carpetaDe(j.nombre));
        Files.createDirectories(dir.resolve("ejercicios"));
        Map<String, String> v = new LinkedHashMap<>();
        v.put("nombre", j.nombre);
        v.put("idioma", j.idioma.codigo());
        v.put("etiqueta", j.etiqueta.name());
        v.put("pruebaHecha", Boolean.toString(j.pruebaHecha));
        v.put("experiencia", Long.toString(j.experiencia));
        v.put("victoriasEnLaEtiqueta", Integer.toString(j.victoriasEnLaEtiqueta));
        v.put("derrotasEnLaEtiqueta", Integer.toString(j.derrotasEnLaEtiqueta));
        v.put("derrotasSeguidas", Integer.toString(j.derrotasSeguidas));
        v.put("victoriasSeguidas", Integer.toString(j.victoriasSeguidas));
        v.put("peleasDesdeElDesafio", Integer.toString(j.peleasDesdeElDesafio));
        v.put("victoriasExtrema", Integer.toString(j.victoriasExtrema));
        v.put("robotElegido", Integer.toString(j.robotElegido));
        v.put("robots", Integer.toString(j.robots.size()));
        for (int i = 0; i < j.robots.size(); i++) {
            Jugador.Robot r = j.robots.get(i);
            v.put("robot." + i + ".nombre", r.nombre);
            v.put("robot." + i + ".modelo", r.modelo);
            for (Map.Entry<Montaje.Hueco, String> e : r.piezas.entrySet()) {
                v.put("robot." + i + ".pieza." + e.getKey().name(), e.getValue());
            }
            escribir(dir.resolve("robot_" + (i + 1) + ".java"), r.codigo);
        }
        v.put("piezasGanadas", lista(j.piezasGanadas));
        v.put("modelosGanados", lista(j.modelosGanados));
        v.put("ejerciciosResueltos", lista(j.ejerciciosResueltos));
        v.put("jefesVencidos", lista(j.jefesVencidos));
        v.put("retosHechos", lista(j.retosHechos));
        v.put("laboratoriosHechos", lista(j.laboratoriosHechos));
        v.put("capitulosGanados", lista(j.capitulosGanados));
        v.put("tutorialesHechos", lista(j.tutorialesHechos));
        v.put("legendariasGanadas", lista(j.legendariasGanadas));
        v.put("modoIA", j.modoIA);
        v.put("agresividad", numero(j.agresividad));
        v.put("humor", numero(j.humor));
        v.put("cortesia", numero(j.cortesia));
        j.migrarCerebroAntiguo();
        for (int i = 0; i < j.cerebros.length; i++) {
            Ranura r = j.cerebros[i];
            String k = "cerebro." + (i + 1) + ".";
            v.put(k + "tipo", r.tipo.name());
            v.put(k + "modeloLocal", r.modeloLocal);
            v.put(k + "direccionLocal", r.direccionLocal);
            v.put(k + "proveedor", r.proveedor);
            v.put(k + "modeloEnLinea", r.modeloEnLinea);
            v.put(k + "direccionEnLinea", r.direccionEnLinea);
        }
        for (Tarea t : Tarea.values()) v.put("reparto." + t.name(), Integer.toString(j.ranuraDe(t) + 1));
        v.put("carpetaModelos", j.carpetaModelos);
        v.put("tamanoDeLetra", numero(j.tamanoDeLetra));
        v.put("volumen", numero(j.volumen));
        v.put("volumenVoz", numero(j.volumenVoz));
        v.put("musicaActiva", String.valueOf(j.musicaActiva));
        v.put("volumenMusica", numero(j.volumenMusica));
        v.put("vozDelLocutor", Boolean.toString(j.vozDelLocutor));
        v.put("vozClara", Boolean.toString(j.vozClara));
        v.put("entrenador", j.entrenador);
        v.put("ayudaDelTutor", numero(j.ayudaDelTutor));
        v.put("temaClaro", Boolean.toString(j.temaClaro));
        StringBuilder b = new StringBuilder("# Partida de MetajavBots (se puede leer; mejor no tocarla a mano)\n");
        for (Map.Entry<String, String> e : v.entrySet()) {
            b.append(e.getKey()).append(" = ").append(e.getValue().replace("\n", " ")).append('\n');
        }
        escribir(dir.resolve("partida.txt"), b.toString());
        for (Map.Entry<String, String> e : j.codigoDeEjercicios.entrySet()) {
            if (e.getKey().matches("[a-z0-9_]+")) escribir(dir.resolve("ejercicios").resolve(e.getKey() + ".java"), e.getValue());
        }
        StringBuilder h = new StringBuilder();
        for (Registro r : j.historial) h.append(r.aLinea()).append('\n');
        escribir(dir.resolve("historial.txt"), h.toString());
    }

    /** Los tres cerebros y su reparto (las partidas de antes traen uno solo: se pasa a las ranuras al usarlo). */
    private static void leerCerebros(Map<String, String> v, Jugador j) {
        for (int i = 0; i < j.cerebros.length; i++) {
            Ranura r = j.cerebros[i];
            String k = "cerebro." + (i + 1) + ".";
            try {
                r.tipo = Ranura.Tipo.valueOf(v.getOrDefault(k + "tipo", "NINGUNO"));
            } catch (IllegalArgumentException e) {
                r.tipo = Ranura.Tipo.NINGUNO;
            }
            r.modeloLocal = v.getOrDefault(k + "modeloLocal", "");
            r.direccionLocal = v.getOrDefault(k + "direccionLocal", "");
            r.proveedor = v.getOrDefault(k + "proveedor", "anthropic");
            r.modeloEnLinea = v.getOrDefault(k + "modeloEnLinea", "");
            r.direccionEnLinea = v.getOrDefault(k + "direccionEnLinea", "");
        }
        for (Tarea t : Tarea.values()) {
            int n = (int) largo(v.getOrDefault("reparto." + t.name(), Integer.toString(t.ranuraPorDefecto() + 1))) - 1;
            j.reparto[t.ordinal()] = Math.max(0, Math.min(j.cerebros.length - 1, n));
        }
    }

    public static Jugador leer(String carpetaDelJugador) throws IOException {
        Path dir = carpeta.resolve(carpetaDelJugador);
        Map<String, String> v = new LinkedHashMap<>();
        for (String linea : Files.readAllLines(dir.resolve("partida.txt"), StandardCharsets.UTF_8)) {
            if (linea.startsWith("#") || !linea.contains("=")) continue;
            int i = linea.indexOf('=');
            v.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
        }
        Jugador j = new Jugador(v.getOrDefault("nombre", carpetaDelJugador));
        j.idioma = Idioma.de(v.get("idioma"));
        j.etiqueta = Etiqueta.de(v.getOrDefault("etiqueta", "NOOB_1"));
        j.pruebaHecha = Boolean.parseBoolean(v.get("pruebaHecha"));
        j.experiencia = largo(v.get("experiencia"));
        j.victoriasEnLaEtiqueta = (int) largo(v.get("victoriasEnLaEtiqueta"));
        j.derrotasEnLaEtiqueta = (int) largo(v.get("derrotasEnLaEtiqueta"));
        j.derrotasSeguidas = (int) largo(v.get("derrotasSeguidas"));
        j.victoriasSeguidas = (int) largo(v.get("victoriasSeguidas"));
        j.peleasDesdeElDesafio = (int) largo(v.getOrDefault("peleasDesdeElDesafio", "99"));
        j.victoriasExtrema = (int) largo(v.get("victoriasExtrema"));
        j.robotElegido = (int) largo(v.get("robotElegido"));
        int n = (int) largo(v.get("robots"));
        for (int i = 0; i < n; i++) {
            Jugador.Robot r = new Jugador.Robot(v.getOrDefault("robot." + i + ".nombre", "Robot " + (i + 1)),
                    v.getOrDefault("robot." + i + ".modelo", "cobalto"));
            for (Montaje.Hueco h : Montaje.Hueco.values()) {
                String p = v.get("robot." + i + ".pieza." + h.name());
                if (p != null && !p.isEmpty()) r.piezas.put(h, p);
            }
            Path codigo = dir.resolve("robot_" + (i + 1) + ".java");
            if (Files.exists(codigo)) r.codigo = Files.readString(codigo, StandardCharsets.UTF_8);
            j.robots.add(r);
        }
        leerLista(v.get("piezasGanadas"), j.piezasGanadas);
        leerLista(v.get("modelosGanados"), j.modelosGanados);
        leerLista(v.get("ejerciciosResueltos"), j.ejerciciosResueltos);
        leerLista(v.get("jefesVencidos"), j.jefesVencidos);
        leerLista(v.get("retosHechos"), j.retosHechos);
        leerLista(v.get("laboratoriosHechos"), j.laboratoriosHechos);
        leerLista(v.get("capitulosGanados"), j.capitulosGanados);
        leerLista(v.get("tutorialesHechos"), j.tutorialesHechos);
        leerLista(v.get("legendariasGanadas"), j.legendariasGanadas);
        j.modoIA = v.getOrDefault("modoIA", "ADAPTADA");
        j.agresividad = decimal(v.get("agresividad"), 0.4);
        j.humor = decimal(v.get("humor"), 0.6);
        j.cortesia = decimal(v.get("cortesia"), 0.5);
        j.cerebro = v.getOrDefault("cerebro", "NINGUNO");
        j.proveedor = v.getOrDefault("proveedor", "anthropic");
        j.modeloEnLinea = v.getOrDefault("modeloEnLinea", "");
        j.direccionLocal = v.getOrDefault("direccionLocal", "http://localhost:5002");
        j.direccionEnLinea = v.getOrDefault("direccionEnLinea", "");
        j.modeloLocal = v.getOrDefault("modeloLocal", "");
        j.carpetaModelos = v.getOrDefault("carpetaModelos", "");
        leerCerebros(v, j);
        leerAjustes(v, j);
        Path ej = dir.resolve("ejercicios");
        if (Files.isDirectory(ej)) {
            try (Stream<Path> s = Files.list(ej)) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    String f = p.getFileName().toString();
                    if (f.endsWith(".java")) j.codigoDeEjercicios.put(f.substring(0, f.length() - 5), Files.readString(p, StandardCharsets.UTF_8));
                }
            }
        }
        Path h = dir.resolve("historial.txt");
        if (Files.exists(h)) {
            for (String linea : Files.readAllLines(h, StandardCharsets.UTF_8)) {
                Registro r = Registro.deLinea(linea);
                if (r != null) j.historial.add(r);
            }
        }
        return j;
    }

    /** La letra, el sonido, la música, la voz y el entrenador. */
    private static void leerAjustes(Map<String, String> v, Jugador j) {
        j.tamanoDeLetra = (float) decimal(v.get("tamanoDeLetra"), 18);
        j.volumen = (float) decimal(v.get("volumen"), 0.8);
        j.volumenVoz = (float) decimal(v.get("volumenVoz"), 0.9);
        j.musicaActiva = !"false".equals(v.get("musicaActiva"));
        j.volumenMusica = (float) decimal(v.get("volumenMusica"), 0.45);
        j.vozDelLocutor = !"false".equals(v.get("vozDelLocutor"));
        j.vozClara = !"false".equals(v.get("vozClara"));
        j.entrenador = v.getOrDefault("entrenador", "tuercas");
        j.ayudaDelTutor = (float) decimal(v.get("ayudaDelTutor"), 0.5);
        j.temaClaro = Boolean.parseBoolean(v.get("temaClaro"));
    }

    static void escribir(Path destino, String texto) throws IOException {
        Path nuevo = destino.resolveSibling(destino.getFileName() + ".nuevo");
        Files.writeString(nuevo, texto, StandardCharsets.UTF_8);
        try {
            Files.move(nuevo, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(nuevo, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String lista(Collection<String> c) {
        return String.join(",", c);
    }

    private static void leerLista(String s, Collection<String> destino) {
        if (s == null || s.isBlank()) return;
        for (String x : s.split(",")) if (!x.isBlank()) destino.add(x.strip());
    }

    private static long largo(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double decimal(String s, double siNo) {
        try {
            return s == null ? siNo : Double.parseDouble(s.strip());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }

    private static String numero(double d) {
        return String.format(Locale.ROOT, "%.3f", d);
    }
}
