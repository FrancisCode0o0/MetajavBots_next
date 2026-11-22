package metajavbots.tutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * CONOCIMIENTO DEL ENTRENADOR — Base de conocimiento centralizada y extensible
 * del entrenador flotante sobre todas las pantallas, modos y mecánicas de
 * MetajavBots (res/conceptos/07_juego.txt).
 *
 * Cada funcionalidad del juego se define una sola vez y alimenta
 * automáticamente:
 * 1) El buscador offline y la pestaña «El juego» de «Pregúntale a …» ({@link Concepto}).
 * 2) El contexto de sistema de la consulta con IA ({@code ConsultaIaEntrenador}).
 * 3) Los consejos espontáneos condicionales y aleatorios de jugabilidad ({@link ConsejosDelJuego}).
 *
 * Cómo registrar una nueva funcionalidad en futuros encargos:
 * añade un bloque {@code === id_nuevo} en {@code res/conceptos/07_juego.txt} con:
 * {@code titulo.es/de/en}, {@code claves.es/de/en}, {@code resumen.ia}, los
 * párrafos {@code es:/de:/en:}, {@code pseudo.es/de/en:}, {@code ejemplo:} y uno
 * o más consejos {@code tip.<tip_id>.es/de/en} (y opcionalmente
 * {@code tip.<tip_id>.condicion: principiante|oxidado|monedas|aleatorio}).
 */
public final class ConocimientoDelEntrenador {

    /** Condición de consejo espontáneo dirigido a principiantes. */
    public static final String CONDICION_PRINCIPIANTE = "principiante";
    /** Condición de consejo espontáneo dirigido a jugadores con derrotas o poca práctica. */
    public static final String CONDICION_OXIDADO = "oxidado";
    /** Condición de consejo espontáneo cuando el jugador tiene monedas ahorradas. */
    public static final String CONDICION_MONEDAS = "monedas";
    /** Condición por defecto para consejos aleatorios de jugabilidad. */
    public static final String CONDICION_ALEATORIO = "aleatorio";

    private static final String RECURSO = "/conceptos/07_juego.txt";

    /** Consejo espontáneo trilingüe ligado a una funcionalidad del juego. */
    public record ConsejoJuego(String id, String conceptoId, String condicion, Trilingue texto) {
        /** Convierte este consejo en un {@link ConsejosDelTaller.Tip} para el planificador. */
        public ConsejosDelTaller.Tip aTip() {
            return new ConsejosDelTaller.Tip(id, conceptoId);
        }

        /** Indica si es un consejo aleatorio de jugabilidad (no restringido a una condición de progreso). */
        public boolean esAleatorio() {
            return CONDICION_ALEATORIO.equals(condicion);
        }
    }

    /** Entrada completa de una funcionalidad del juego en la base de conocimiento. */
    public record Entrada(String id, int guia, Trilingue titulo, Map<Idioma, List<String>> claves,
            Map<Idioma, List<String>> parrafos, Map<Idioma, String> pseudo, String ejemplo,
            List<String> ver, String resumenIa, List<ConsejoJuego> consejos) {

        /** Convierte la entrada en un {@link Concepto} de nivel {@link Concepto#NIVEL_JUEGO}. */
        public Concepto aConcepto() {
            return new Concepto(id, Concepto.NIVEL_JUEGO, guia, "--", titulo, parrafos, claves, pseudo, ejemplo, ver);
        }
    }

    private static List<Entrada> entradas;
    private static List<ConsejoJuego> todosLosConsejos;
    private static Map<String, ConsejoJuego> consejosPorId;
    private static String resumenIaCache;

    private ConocimientoDelEntrenador() {
    }

    /** Devuelve todas las entradas registradas en la base de conocimiento del juego. */
    public static synchronized List<Entrada> entradas() {
        cargarSiHaceFalta();
        return entradas;
    }

    /** Busca una entrada por su identificador (o {@code null} si no existe). */
    public static Entrada de(String id) {
        if (id == null) return null;
        for (Entrada e : entradas()) if (e.id.equals(id)) return e;
        return null;
    }

    /** Construye los conceptos de nivel {@link Concepto#NIVEL_JUEGO} para el buscador offline. */
    public static List<Concepto> comoConceptos() {
        List<Concepto> lista = new ArrayList<>();
        for (Entrada e : entradas()) lista.add(e.aConcepto());
        return Collections.unmodifiableList(lista);
    }

    /** Construye el resumen conciso de todas las funcionalidades para el contexto del sistema de IA. */
    public static synchronized String resumenParaIa() {
        cargarSiHaceFalta();
        return resumenIaCache;
    }

    /** Devuelve todos los consejos espontáneos de jugabilidad y progreso definidos en el conocimiento. */
    public static synchronized List<ConsejoJuego> todosLosConsejos() {
        cargarSiHaceFalta();
        return todosLosConsejos;
    }

    /** Devuelve los consejos asociados a una condición concreta (p. ej. principiante, oxidado o aleatorio). */
    public static List<ConsejoJuego> consejosPorCondicion(String condicion) {
        List<ConsejoJuego> resultado = new ArrayList<>();
        for (ConsejoJuego c : todosLosConsejos()) {
            if (c.condicion.equals(condicion)) resultado.add(c);
        }
        return Collections.unmodifiableList(resultado);
    }

    /** Devuelve el texto de un consejo registrado por su id en el idioma dado, o {@code null} si no existe. */
    public static synchronized String textoDeConsejo(String tipId, Idioma idioma) {
        cargarSiHaceFalta();
        ConsejoJuego c = consejosPorId.get(tipId);
        return c == null ? null : c.texto.en(idioma == null ? Idioma.ES : idioma);
    }

    private static void cargarSiHaceFalta() {
        if (entradas != null) return;
        List<Entrada> leidas = leer(RECURSO);
        List<ConsejoJuego> consejos = new ArrayList<>();
        Map<String, ConsejoJuego> mapa = new LinkedHashMap<>();
        StringBuilder ia = new StringBuilder("Game screens, modes and mechanics you can guide the player through: ");
        boolean primeroIa = true;
        for (Entrada e : leidas) {
            if (!e.resumenIa.isBlank()) {
                if (!primeroIa) ia.append(' ');
                ia.append(e.resumenIa.strip());
                if (!e.resumenIa.strip().endsWith(".")) ia.append('.');
                primeroIa = false;
            }
            for (ConsejoJuego c : e.consejos) {
                consejos.add(c);
                mapa.put(c.id, c);
            }
        }
        entradas = Collections.unmodifiableList(leidas);
        todosLosConsejos = Collections.unmodifiableList(consejos);
        consejosPorId = Collections.unmodifiableMap(mapa);
        resumenIaCache = ia.toString();
    }

    private static List<Entrada> leer(String recurso) {
        List<Entrada> lista = new ArrayList<>();
        try (InputStream in = ConocimientoDelEntrenador.class.getResourceAsStream(recurso)) {
            if (in == null) return lista;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String id = null, bloque = null, linea;
            Map<String, String> v = new LinkedHashMap<>();
            Map<String, List<String>> bloques = new LinkedHashMap<>();
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("#")) continue;
                if (linea.startsWith("=== ")) {
                    if (id != null) lista.add(construirEntrada(id, v, bloques));
                    id = linea.substring(4).strip();
                    v = new LinkedHashMap<>();
                    bloques = new LinkedHashMap<>();
                    bloque = null;
                } else if (linea.isBlank() && bloque != null && (bloque.equals("ejemplo") || bloque.startsWith("pseudo"))) {
                    bloques.get(bloque).add("");
                } else if (linea.startsWith("    ") && bloque != null) {
                    String s = linea.substring(4);
                    boolean tal = bloque.equals("ejemplo") || bloque.startsWith("pseudo");
                    if (tal || !s.isBlank()) bloques.get(bloque).add(tal ? s.stripTrailing() : s.strip());
                } else if (!linea.isBlank() && linea.contains(":") && !linea.startsWith(" ")) {
                    int i = linea.indexOf(':');
                    String clave = linea.substring(0, i).strip(), valor = linea.substring(i + 1).strip();
                    if (valor.isEmpty()) {
                        bloque = clave;
                        bloques.put(clave, new ArrayList<>());
                    } else {
                        bloque = null;
                        v.put(clave, valor);
                    }
                }
            }
            if (id != null) lista.add(construirEntrada(id, v, bloques));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer res" + recurso, e);
        }
        return lista;
    }

    private static Entrada construirEntrada(String id, Map<String, String> v, Map<String, List<String>> bloques) {
        int guia = v.containsKey("guia") ? entero(v.get("guia")) : 0;
        Trilingue titulo = new Trilingue(v.getOrDefault("titulo.es", id), v.getOrDefault("titulo.de", id), v.getOrDefault("titulo.en", id));
        Map<Idioma, List<String>> parrafos = new EnumMap<>(Idioma.class);
        Map<Idioma, List<String>> claves = new EnumMap<>(Idioma.class);
        Map<Idioma, String> pseudo = new EnumMap<>(Idioma.class);
        for (Idioma i : Idioma.values()) {
            String c = i.name().toLowerCase(Locale.ROOT);
            parrafos.put(i, Collections.unmodifiableList(new ArrayList<>(bloques.getOrDefault(c, List.of()))));
            pseudo.put(i, String.join("\n", bloques.getOrDefault("pseudo." + c, List.of())).stripTrailing());
            List<String> k = new ArrayList<>();
            for (String s : v.getOrDefault("claves." + c, "").split(",")) {
                if (!s.isBlank()) k.add(Concepto.normalizar(s));
            }
            claves.put(i, Collections.unmodifiableList(k));
        }
        List<String> ver = new ArrayList<>();
        for (String s : v.getOrDefault("ver", "").split(",")) if (!s.isBlank()) ver.add(s.strip());
        String ejemplo = String.join("\n", bloques.getOrDefault("ejemplo", List.of())).stripTrailing();
        String resumenIa = v.getOrDefault("resumen.ia", "");
        List<ConsejoJuego> consejos = extraerConsejos(id, v);
        return new Entrada(id, guia, titulo, Collections.unmodifiableMap(claves), Collections.unmodifiableMap(parrafos),
                Collections.unmodifiableMap(pseudo), ejemplo, Collections.unmodifiableList(ver), resumenIa, consejos);
    }

    private static List<ConsejoJuego> extraerConsejos(String conceptoId, Map<String, String> v) {
        Map<String, Map<String, String>> porTip = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : v.entrySet()) {
            String k = entry.getKey();
            if (!k.startsWith("tip.")) continue;
            int ultimoPunto = k.lastIndexOf('.');
            if (ultimoPunto <= 4) continue;
            String tipId = k.substring(4, ultimoPunto);
            String campo = k.substring(ultimoPunto + 1);
            porTip.computeIfAbsent(tipId, unused -> new LinkedHashMap<>()).put(campo, entry.getValue());
        }
        List<ConsejoJuego> resultado = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : porTip.entrySet()) {
            Map<String, String> campos = entry.getValue();
            String es = campos.getOrDefault("es", "");
            String de = campos.getOrDefault("de", "");
            String en = campos.getOrDefault("en", "");
            if (es.isBlank() && de.isBlank() && en.isBlank()) continue;
            String cond = campos.getOrDefault("condicion", CONDICION_ALEATORIO).toLowerCase(Locale.ROOT);
            resultado.add(new ConsejoJuego(entry.getKey(), conceptoId, cond, new Trilingue(es, de, en)));
        }
        return Collections.unmodifiableList(resultado);
    }

    private static int entero(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
