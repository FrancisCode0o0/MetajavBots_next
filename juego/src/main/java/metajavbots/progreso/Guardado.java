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
import metajavbots.piezas.Modelo;
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
        // En Windows «ana» y «Ana» son la misma carpeta; en Linux y Mac no: se compara sin mayúsculas en todos.
        boolean ocupada = Files.exists(carpeta.resolve(nueva)) || partidas().stream().anyMatch(nueva::equalsIgnoreCase);
        if (!mismaCarpeta && ocupada) throw new IOException("ya existe");
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
        v.put("avisoCompilarVisto", Boolean.toString(j.avisoCompilarVisto));
        v.put("experiencia", Long.toString(j.experiencia));
        guardarMonedas(v, j);
        v.put("victoriasEnLaEtiqueta", Integer.toString(j.victoriasEnLaEtiqueta));
        v.put("derrotasEnLaEtiqueta", Integer.toString(j.derrotasEnLaEtiqueta));
        v.put("derrotasSeguidas", Integer.toString(j.derrotasSeguidas));
        v.put("victoriasSeguidas", Integer.toString(j.victoriasSeguidas));
        v.put("peleasDesdeElDesafio", Integer.toString(j.peleasDesdeElDesafio));
        v.put("victoriasExtrema", Integer.toString(j.victoriasExtrema));
        v.put("robotElegido", Integer.toString(j.robotElegido));
        guardarRobots(dir, v, j);
        v.put("piezasGanadas", lista(j.piezasGanadas));
        v.put("piezasPendientes", lista(j.piezasPendientes));
        guardarAjustesDePiezas(v, j);
        v.put("modelosGanados", lista(j.modelosGanados));
        v.put("ejerciciosResueltos", lista(j.ejerciciosResueltos));
        v.put("jefesVencidos", lista(j.jefesVencidos));
        v.put("retosHechos", lista(j.retosHechos));
        v.put("laboratoriosHechos", lista(j.laboratoriosHechos));
        v.put("capitulosGanados", lista(j.capitulosGanados));
        v.put("robotsArreglados", lista(j.robotsArreglados));
        v.put("retoDiario", j.retoDiarioUltimo + "," + j.retoDiarioId + "," + j.retoDiarioRacha + "," + j.retoDiarioMejor);
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
        v.put("arenasConTrampas", String.valueOf(j.arenasConTrampas));
        v.put("aparatosEnLaArena", String.valueOf(j.aparatosEnLaArena));
        v.put("rondas", Integer.toString(j.rondas));
        v.put("armasEspeciales", String.valueOf(j.armasEspeciales));
        v.put("vozDelLocutor", Boolean.toString(j.vozDelLocutor));
        v.put("vozClara", Boolean.toString(j.vozClara));
        v.put("entrenador", j.entrenador);
        v.put("ayudaDelTutor", numero(j.ayudaDelTutor) + (j.entrenadorActivo ? "" : ";sin_entrenador"));
        v.put("temaClaro", Boolean.toString(j.temaClaro));
        v.put("avisosDeOrdenes", Boolean.toString(j.avisosDeOrdenes));
        v.put("fallosDeOrdenes", j.fallosDeOrdenes[0] + "," + j.fallosDeOrdenes[1]);
        v.put("avisoDeOrdenesSalio", j.avisoDeOrdenesSalio[0] + "," + j.avisoDeOrdenesSalio[1]);
        v.put("comoSeJuegaVisto", lista(j.comoSeJuegaVisto));
        v.put("menuCompleto", Boolean.toString(j.menuCompleto));
        v.put("ultimoRivalNombre", j.ultimoRivalNombre == null ? "" : j.ultimoRivalNombre);
        StringBuilder b = new StringBuilder("# Partida de MetajavBots (se puede leer; mejor no tocarla a mano)\n");
        for (Map.Entry<String, String> e : v.entrySet()) {
            b.append(e.getKey()).append(" = ").append(e.getValue().replace("\n", " ")).append('\n');
        }
        escribir(dir.resolve("partida.txt"), b.toString());
        guardarArchivosAparte(dir, j);
    }

    /** Guarda los metadatos, el borrador y la última versión válida compilada de cada robot. */
    private static void guardarRobots(Path dir, Map<String, String> v, Jugador j) throws IOException {
        v.put("robots", Integer.toString(j.robots.size()));
        for (int i = 0; i < j.robots.size(); i++) {
            Jugador.Robot r = j.robots.get(i);
            v.put("robot." + i + ".nombre", r.nombre);
            v.put("robot." + i + ".modelo", r.modelo);
            v.put("robot." + i + ".compilado", Boolean.toString(r.tieneCodigoCompilado()));
            for (Map.Entry<Montaje.Hueco, String> e : r.piezas.entrySet()) {
                v.put("robot." + i + ".pieza." + e.getKey().name(), e.getValue());
            }
            escribir(dir.resolve("robot_" + (i + 1) + ".java"), r.codigo);
            Path compilado = dir.resolve("robot_" + (i + 1) + "_compilado.java");
            if (r.tieneCodigoCompilado()) escribir(compilado, r.codigoCompilado);
            else Files.deleteIfExists(compilado);
        }
    }

    /** Los ejercicios, el historial y el código del último rival (cada uno en su archivo). */
    private static void guardarArchivosAparte(Path dir, Jugador j) throws IOException {
        for (Map.Entry<String, String> e : j.codigoDeEjercicios.entrySet()) {
            if (e.getKey().matches("[a-z0-9_]+")) escribir(dir.resolve("ejercicios").resolve(e.getKey() + ".java"), e.getValue());
        }
        StringBuilder h = new StringBuilder();
        for (Registro r : j.historial) h.append(r.aLinea()).append('\n');
        escribir(dir.resolve("historial.txt"), h.toString());
        Path ultimoRival = dir.resolve("ultimo_rival.java");
        if (j.ultimoRivalCodigo != null && !j.ultimoRivalCodigo.isBlank()) escribir(ultimoRival, j.ultimoRivalCodigo);
        else Files.deleteIfExists(ultimoRival);
        guardarPiezasPropias(dir, j);
    }

    private static void guardarPiezasPropias(Path dir, Jugador j) throws IOException {
        Path carpetaPropias = dir.resolve("piezas_propias");
        if (j.piezasPropiasCodigo.isEmpty()) {
            if (Files.isDirectory(carpetaPropias)) {
                try (Stream<Path> s = Files.list(carpetaPropias)) {
                    for (Path p : (Iterable<Path>) s::iterator) {
                        if (p.getFileName().toString().endsWith(".java")) Files.deleteIfExists(p);
                    }
                }
            }
            return;
        }
        Files.createDirectories(carpetaPropias);
        for (Map.Entry<String, String> e : j.piezasPropiasCodigo.entrySet()) {
            if (e.getKey().matches("[a-z0-9_]+")) escribir(carpetaPropias.resolve(e.getKey() + ".java"), e.getValue());
        }
        try (Stream<Path> s = Files.list(carpetaPropias)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String f = p.getFileName().toString();
                if (f.endsWith(".java") && !j.piezasPropiasCodigo.containsKey(f.substring(0, f.length() - 5))) {
                    Files.deleteIfExists(p);
                }
            }
        }
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
        j.avisoCompilarVisto = Boolean.parseBoolean(v.get("avisoCompilarVisto"));
        j.experiencia = largo(v.get("experiencia"));
        leerMonedas(v, j);
        leerEstadisticas(v, j);
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
            // Un robot que se llama como otro modelo (se le cambió el modelo y se quedó con el nombre de antes) toma el del suyo.
            Modelo suyo = Modelo.de(r.modelo);
            if (suyo != null && r.seLlamaComoOtroModelo()) r.nombre = suyo.nombre(j.idioma);
            j.robots.add(r);
        }
        leerLista(v.get("piezasGanadas"), j.piezasGanadas);
        leerLista(v.get("piezasPendientes"), j.piezasPendientes);
        leerAjustesDePiezas(v, j);
        leerLista(v.get("modelosGanados"), j.modelosGanados);
        leerLista(v.get("ejerciciosResueltos"), j.ejerciciosResueltos);
        leerLista(v.get("jefesVencidos"), j.jefesVencidos);
        leerLista(v.get("retosHechos"), j.retosHechos);
        leerLista(v.get("laboratoriosHechos"), j.laboratoriosHechos);
        leerLista(v.get("capitulosGanados"), j.capitulosGanados);
        leerLista(v.get("robotsArreglados"), j.robotsArreglados);
        leerRetoDiario(v.get("retoDiario"), j);
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
        j.ultimoRivalNombre = v.getOrDefault("ultimoRivalNombre", "");
        Path ultimoRival = dir.resolve("ultimo_rival.java");
        if (Files.exists(ultimoRival)) j.ultimoRivalCodigo = Files.readString(ultimoRival, StandardCharsets.UTF_8);
        leerPiezasPropias(dir, j);
        leerEstadoCompilacionRobots(dir, v, j);
        // Las partidas de antes del camino «Empezar»: quien ya había jugado ve el menú entero, como siempre.
        if (!v.containsKey("menuCompleto")) {
            j.menuCompleto = !j.historial.isEmpty() || !j.tutorialesHechos.isEmpty() || !j.ejerciciosResueltos.isEmpty();
        }
        return j;
    }

    private static void leerPiezasPropias(Path dir, Jugador j) throws IOException {
        Path carpetaPropias = dir.resolve("piezas_propias");
        if (!Files.isDirectory(carpetaPropias)) return;
        try (Stream<Path> s = Files.list(carpetaPropias)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String f = p.getFileName().toString();
                if (!f.endsWith(".java")) continue;
                String id = f.substring(0, f.length() - 5);
                String codigo = Files.readString(p, StandardCharsets.UTF_8);
                j.piezasPropiasCodigo.put(id, codigo);
            }
        }
    }

    /** Restaura el estado de compilación verificada y el último código válido de cada robot (con migración compatible). */
    private static void leerEstadoCompilacionRobots(Path dir, Map<String, String> v, Jugador j) throws IOException {
        boolean partidaAntiguaConProgreso = !j.historial.isEmpty() || j.experiencia > 0;
        for (int i = 0; i < j.robots.size(); i++) {
            Jugador.Robot r = j.robots.get(i);
            String clave = "robot." + i + ".compilado";
            Path archivoCompilado = dir.resolve("robot_" + (i + 1) + "_compilado.java");
            if (v.containsKey(clave)) {
                boolean compilado = Boolean.parseBoolean(v.get(clave));
                if (compilado && Files.exists(archivoCompilado)) {
                    r.registrarCompilacionValida(Files.readString(archivoCompilado, StandardCharsets.UTF_8));
                } else if (compilado && r.codigo != null && !r.codigo.isBlank()) {
                    r.registrarCompilacionValida(r.codigo);
                } else {
                    r.compiladoAlgunaVez = false;
                    r.codigoCompilado = "";
                }
            } else if (Files.exists(archivoCompilado)) {
                r.registrarCompilacionValida(Files.readString(archivoCompilado, StandardCharsets.UTF_8));
            } else if (partidaAntiguaConProgreso && r.codigo != null && !r.codigo.isBlank()) {
                r.registrarCompilacionValida(r.codigo);
            }
        }
        if (!v.containsKey("avisoCompilarVisto") && j.haCompiladoAlgunRobot()) {
            j.avisoCompilarVisto = true;
        }
    }

    /** Persiste monedas y centésimas exactas (manteniendo cuartosDeMoneda sincronizado). */
    private static void guardarMonedas(Map<String, String> v, Jugador j) {
        v.put("monedas", String.format(Locale.ROOT, "%.2f", Jugador.redondearMonedas(j.monedas)));
        int centimos = Math.max(0, Math.min(99, j.centimosDeMoneda));
        v.put("centimosDeMoneda", Integer.toString(centimos));
        v.put("cuartosDeMoneda", Integer.toString(centimos / 25));
    }

    /** Lee monedas y centésimas exactas migrando partidas antiguas que solo tenían cuartosDeMoneda. */
    private static void leerMonedas(Map<String, String> v, Jugador j) {
        j.monedas = Math.max(0, Jugador.redondearMonedas(decimal(v.get("monedas"), 0)));
        if (v.containsKey("centimosDeMoneda")) {
            j.centimosDeMoneda = Math.max(0, Math.min(99, (int) largo(v.get("centimosDeMoneda"))));
        } else {
            int cuartosPrevios = Math.max(0, Math.min(3, (int) largo(v.get("cuartosDeMoneda"))));
            j.centimosDeMoneda = cuartosPrevios * 25;
        }
        j.cuartosDeMoneda = j.centimosDeMoneda / 25;
    }

    /** Contadores de victorias, derrotas, rachas y robot seleccionado. */
    private static void leerEstadisticas(Map<String, String> v, Jugador j) {
        j.victoriasEnLaEtiqueta = (int) largo(v.get("victoriasEnLaEtiqueta"));
        j.derrotasEnLaEtiqueta = (int) largo(v.get("derrotasEnLaEtiqueta"));
        j.derrotasSeguidas = (int) largo(v.get("derrotasSeguidas"));
        j.victoriasSeguidas = (int) largo(v.get("victoriasSeguidas"));
        j.peleasDesdeElDesafio = (int) largo(v.getOrDefault("peleasDesdeElDesafio", "99"));
        j.victoriasExtrema = (int) largo(v.get("victoriasExtrema"));
        j.robotElegido = (int) largo(v.get("robotElegido"));
    }

    /** «día,ejercicio,racha,mejor» (sin él, como nuevo). */
    private static void leerRetoDiario(String s, Jugador j) {
        if (s == null) return;
        String[] p = s.split(",", -1);
        try {
            j.retoDiarioUltimo = Long.parseLong(p[0]);
            j.retoDiarioId = p[1];
            j.retoDiarioRacha = Integer.parseInt(p[2]);
            j.retoDiarioMejor = Integer.parseInt(p[3]);
        } catch (RuntimeException e) {
            // mal escrito: se queda sin racha
        }
    }

    /** La letra, el sonido, la música, la voz y el entrenador. */
    private static void leerAjustes(Map<String, String> v, Jugador j) {
        j.tamanoDeLetra = (float) decimal(v.get("tamanoDeLetra"), 18);
        j.volumen = (float) decimal(v.get("volumen"), 0.8);
        j.volumenVoz = (float) decimal(v.get("volumenVoz"), 0.9);
        j.musicaActiva = !"false".equals(v.get("musicaActiva"));
        j.volumenMusica = (float) decimal(v.get("volumenMusica"), 0.45);
        j.arenasConTrampas = !"false".equals(v.get("arenasConTrampas"));
        j.aparatosEnLaArena = !"false".equals(v.get("aparatosEnLaArena"));
        j.rondas = (int) decimal(v.get("rondas"), 6);
        j.armasEspeciales = !"false".equals(v.get("armasEspeciales"));
        j.vozDelLocutor = !"false".equals(v.get("vozDelLocutor"));
        j.vozClara = !"false".equals(v.get("vozClara"));
        j.entrenador = v.getOrDefault("entrenador", "tuercas");
        // «0.5;sin_entrenador»: la ayuda y, si lo ha desactivado, el entrenador flotante (en una sola línea de la partida).
        String ayuda = v.getOrDefault("ayudaDelTutor", "0.5");
        j.entrenadorActivo = !ayuda.endsWith(";sin_entrenador");
        j.ayudaDelTutor = (float) decimal(ayuda.replace(";sin_entrenador", ""), 0.5);
        j.temaClaro = Boolean.parseBoolean(v.get("temaClaro"));
        j.avisosDeOrdenes = !"false".equals(v.get("avisosDeOrdenes"));
        String[] fallos = v.getOrDefault("fallosDeOrdenes", "0,0").split(",");
        String[] salio = v.getOrDefault("avisoDeOrdenesSalio", "false,false").split(",");
        for (int k = 0; k < 2; k++) {
            j.fallosDeOrdenes[k] = k < fallos.length ? (int) decimal(fallos[k], 0) : 0;
            j.avisoDeOrdenesSalio[k] = k < salio.length && Boolean.parseBoolean(salio[k].strip());
        }
        leerLista(v.get("comoSeJuegaVisto"), j.comoSeJuegaVisto);
        j.menuCompleto = Boolean.parseBoolean(v.get("menuCompleto"));
    }

    static void escribir(Path destino, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (Files.isRegularFile(destino) && Files.size(destino) == bytes.length
                && java.util.Arrays.equals(Files.readAllBytes(destino), bytes)) {
            return;
        }
        Path padre = destino.getParent();
        if (padre != null) Files.createDirectories(padre);
        Path nuevo = destino.resolveSibling(destino.getFileName() + ".nuevo");
        Files.write(nuevo, bytes);
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

    private static void guardarAjustesDePiezas(Map<String, String> v, Jugador j) {
        for (Map.Entry<String, Map<String, Double>> e : j.ajustesDePiezas.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            StringBuilder aj = new StringBuilder();
            for (Map.Entry<String, Double> kv : e.getValue().entrySet()) {
                if (aj.length() > 0) aj.append(',');
                aj.append(kv.getKey()).append('=').append(numero(kv.getValue()));
            }
            v.put("piezaAjuste." + e.getKey(), aj.toString());
        }
    }

    private static void leerAjustesDePiezas(Map<String, String> v, Jugador j) {
        for (Map.Entry<String, String> e : v.entrySet()) {
            if (!e.getKey().startsWith("piezaAjuste.")) continue;
            String idPieza = e.getKey().substring("piezaAjuste.".length()).strip();
            if (idPieza.isEmpty() || e.getValue().isBlank()) continue;
            Map<String, Double> mapa = new LinkedHashMap<>();
            for (String par : e.getValue().split(",")) {
                int eq = par.indexOf('=');
                if (eq <= 0) continue;
                String clave = par.substring(0, eq).strip();
                double val = decimal(par.substring(eq + 1), Double.NaN);
                if (!clave.isEmpty() && Double.isFinite(val)) mapa.put(clave, val);
            }
            if (!mapa.isEmpty()) j.ajustesDePiezas.put(idPieza, mapa);
        }
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
