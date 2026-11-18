package metajavbots.tutor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * CONCEPTO — Lo que el entrenador flotante sabe explicar sin IA: un concepto
 * de Java o de la orientación a objetos (de «qué es un programa» a «el
 * modelo de memoria»), con su explicación para quien no sabe nada, en tres
 * idiomas, un ejemplo del propio juego y los conceptos relacionados.
 * Están en res/conceptos/ (formato en LEEME_FORMATO.txt). Se buscan por
 * palabras, sin tildes, en cualquiera de los tres idiomas.
 */
public final class Concepto {

    /** Los archivos de conceptos, en orden (de lo básico a lo avanzado). */
    static final String[] ARCHIVOS = {"01_basico.txt", "02_control.txt", "03_objetos.txt", "04_intermedio.txt", "05_moderno.txt", "06_senior.txt", "07_juego.txt"};

    public final String id, tema, ejemplo;
    public final int nivel;
    /** La página de la guía con dibujos (pantallas/GuiaDeCerebros, desde 1) que lo enseña, o 0 si no tiene. */
    public final int guia;
    public final Trilingue titulo;
    /** La explicación: un párrafo por elemento, en cada idioma. */
    private final Map<Idioma, List<String>> parrafos = new LinkedHashMap<>();
    private final Map<Idioma, List<String>> claves = new LinkedHashMap<>();
    /** El pseudocódigo, en cada idioma (con su sangría). */
    private final Map<Idioma, String> pseudo = new LinkedHashMap<>();
    public final List<String> ver;

    private Concepto(String id, Map<String, String> v, Map<String, List<String>> bloques) {
        this.id = id;
        nivel = entero(v.getOrDefault("nivel", "3"));
        guia = v.containsKey("guia") ? entero(v.get("guia")) : 0;
        tema = v.getOrDefault("tema", "--");
        titulo = new Trilingue(v.get("titulo.es"), v.get("titulo.de"), v.get("titulo.en"));
        List<String> vv = new ArrayList<>();
        for (String s : v.getOrDefault("ver", "").split(",")) if (!s.isBlank()) vv.add(s.strip());
        ver = Collections.unmodifiableList(vv);
        for (Idioma i : Idioma.values()) {
            String c = i.name().toLowerCase(java.util.Locale.ROOT);
            parrafos.put(i, bloques.getOrDefault(c, List.of()));
            pseudo.put(i, String.join("\n", bloques.getOrDefault("pseudo." + c, List.of())).stripTrailing());
            List<String> k = new ArrayList<>();
            for (String s : v.getOrDefault("claves." + c, "").split(",")) if (!s.isBlank()) k.add(normalizar(s));
            claves.put(i, k);
        }
        ejemplo = String.join("\n", bloques.getOrDefault("ejemplo", List.of())).stripTrailing();
    }

    private static int entero(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    /** Los párrafos de la explicación en ese idioma (en español si falta). */
    public List<String> parrafos(Idioma i) {
        List<String> l = parrafos.get(i);
        return l == null || l.isEmpty() ? parrafos.getOrDefault(Idioma.ES, List.of()) : l;
    }

    /** El pseudocódigo en ese idioma (en español si falta; vacío si no tiene). */
    public String pseudo(Idioma i) {
        String s = pseudo.getOrDefault(i, "");
        return s.isEmpty() ? pseudo.getOrDefault(Idioma.ES, "") : s;
    }

    /**
     * El ejemplo para quien juega en ese idioma: la API del robot en la suya
     * (TraductorDeApi) y, de cada comentario «// es || de || en», su parte.
     */
    public String ejemplo(Idioma i) {
        String t = metajavbots.codigo.TraductorDeApi.traducir(ejemplo, i);
        StringBuilder sb = new StringBuilder();
        for (String linea : t.split("\n", -1)) {
            int c = linea.indexOf("//");
            if (c >= 0 && linea.indexOf(" || ", c) > 0) {
                String[] partes = linea.substring(c + 2).split("\\|\\|");
                int n = Math.min(i.ordinal(), partes.length - 1);
                linea = linea.substring(0, c) + "// " + partes[n].strip();
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(linea);
        }
        return sb.toString();
    }

    /** Las palabras con las que se busca, en ese idioma (sin tildes, en minúsculas). */
    public List<String> claves(Idioma i) {
        return claves.getOrDefault(i, List.of());
    }

    // ================================================================ buscar

    /** En minúsculas, sin tildes ni diéresis (la ß, ss) y con un solo espacio entre palabras. */
    public static String normalizar(String s) {
        String n = Normalizer.normalize(s.toLowerCase(java.util.Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD);
        // El «?» suelto (para «? extends»), y sin el punto del final de una frase: «¿qué es una variable?» encuentra «variable».
        return n.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9<>?@_.\\- ]", " ").replace("?", " ? ").replaceAll("\\.(?=\\s|$)", " ")
                .replaceAll("\\s+", " ").strip();
    }

    /** Lo que se parece la pregunta a este concepto: las claves y el título que aparecen en ella, y sus palabras sueltas. */
    int puntos(String pregunta, Idioma idioma) {
        String q = " " + normalizar(pregunta) + " ";
        if (q.isBlank()) return 0;
        int p = 0;
        String idPalabras = " " + id.replace('_', ' ') + " ";
        if (q.contains(idPalabras)) p += 8 + idPalabras.length();
        for (Idioma i : Idioma.values()) {
            int peso = i == idioma ? 3 : 1;
            String tit = " " + normalizar(titulo.en(i)) + " ";
            if (tit.length() > 3 && q.contains(tit)) p += (6 + tit.length()) * peso;
            for (String k : claves(i)) {
                if (k.length() >= 2 && q.contains(" " + k + " ")) p += (4 + k.length()) * peso;
            }
        }
        // Palabras sueltas de la pregunta que salen en las claves (con 4 letras o más: «bucle» encuentra «bucle for»).
        for (String w : q.strip().split(" ")) {
            if (w.length() < 4) continue;
            for (String k : claves(idioma)) if ((" " + k + " ").contains(" " + w + " ")) p += 2;
        }
        return p;
    }

    /** Los conceptos que mejor responden a la pregunta (como mucho «cuantos»; ninguno si nada se parece). */
    public static List<Concepto> buscar(String pregunta, Idioma idioma, int cuantos) {
        List<Concepto> l = new ArrayList<>(todos());
        Map<Concepto, Integer> pts = new LinkedHashMap<>();
        for (Concepto c : l) pts.put(c, c.puntos(pregunta, idioma));
        l.removeIf(c -> pts.get(c) <= 0);
        // Los que más puntos tienen; a igualdad, el más básico (quien pregunta «qué es…» suele empezar).
        l.sort((a, b) -> {
            int d = pts.get(b) - pts.get(a);
            return d != 0 ? d : a.nivel - b.nivel;
        });
        return l.subList(0, Math.min(cuantos, l.size()));
    }

    /**
     * De qué concepto va un error de javac (por el principio de su código, «compiler.err.…»): para que el
     * entrenador explique la idea que hay detrás del subrayado rojo. El primero que encaja; si ninguno, el
     * error de compilación en general.
     */
    private static final String[][] ERRORES = {
        {"premature.eof", "bloque"}, {"class.method.or.field.expected", "bloque"}, {"orphaned", "switch"},
        {"expected", "sentencia"}, {"not.stmt", "sentencia"}, {"illegal.start", "sentencia"}, {"statement.not.expected", "sentencia"},
        {"unclosed.str.lit", "string"}, {"unclosed.text.block", "text_block"}, {"illegal.text.block", "text_block"},
        {"char.lit", "char"}, {"illegal.esc.char", "string"}, {"unclosed.comment", "comentario"}, {"int.number.too.large", "int_y_double"},
        {"else.without.if", "if_else"}, {"duplicate.case", "switch"}, {"duplicate.default", "switch"}, {"not.exhaustive", "sealed"},
        {"switch.mixing", "switch_expresion"}, {"rule.completes", "switch_expresion"}, {"pattern.dominated", "pattern_matching"},
        {"break.outside", "break_continue"}, {"cont.outside", "break_continue"}, {"not.loop.label", "break_continue"}, {"undef.label", "break_continue"}, {"unreachable.stmt", "return"},
        {"foreach.not.applicable", "recorrer_array"}, {"cant.resolve.location.args", "llamar_metodo"}, {"cant.resolve", "variable"},
        {"doesnt.exist", "paquete_import"}, {"already.defined", "ambito"}, {"var.might.not.have.been.initialized", "variable"},
        {"var.might.already.be.assigned", "constante"}, {"cant.assign.val", "constante"}, {"final.parameter", "constante"},
        {"illegal.forward.ref", "atributo"}, {"prob.found.req", "casting"}, {"type.found.req", "tipo"}, {"cant.infer.local.var", "var"},
        {"restricted.type", "var"}, {"operator.cant.be.applied", "operadores"}, {"incomparable.types", "comparacion"},
        {"unexpected.type", "asignacion"}, {"cant.deref", "tipo"}, {"array", "array"}, {"dot.class.expected", "llamar_metodo"},
        {"void.not.allowed", "return"}, {"cant.apply.diamond", "genericos"}, {"cant.apply.symbol", "parametro"}, {"ref.ambiguous", "sobrecarga"},
        {"missing.ret.stmt", "return"}, {"missing.meth.body", "clase_abstracta"}, {"invalid.meth.decl", "metodo"}, {"ret.outside.meth", "return"},
        {"non-static.cant.be.ref", "static"}, {"unreported.exception", "checked_unchecked"}, {"except.", "try_catch"},
        {"try.without.catch", "try_catch"}, {"catch.without.try", "try_catch"}, {"class.public.should.be.in.file", "clase"},
        {"duplicate.class", "clase"}, {"method.does.not.override", "override"}, {"does.not.override.abstract", "interfaz"},
        {"override", "override"}, {"abstract.cant.be.instantiated", "clase_abstracta"}, {"enum.cant.be.instantiated", "enum"},
        {"abstract.meth.cant.have.body", "clase_abstracta"}, {"intf.meth.cant.have.body", "metodo_default"},
        {"illegal.combination.of.modifiers", "modificadores_de_acceso"}, {"mod.not.allowed", "modificadores_de_acceso"},
        {"report.access", "modificadores_de_acceso"}, {"not.def.public", "modificadores_de_acceso"}, {"cant.inherit.from.final", "herencia"},
        {"cant.inherit.from.sealed", "sealed"}, {"cyclic.inheritance", "herencia"}, {"intf.expected", "interfaz"}, {"no.intf.expected", "herencia"},
        {"ctor", "constructor"}, {"call.must", "super"}, {"cant.ref.before.ctor", "constructor"}, {"default.constructor", "constructor"},
        {"record", "record"}, {"not.within.bounds", "genericos"}, {"wrong.number.type.args", "genericos"}, {"generic.array", "borrado_de_tipos"},
        {"effectively.final", "lambda"}, {"accessed.from.icls", "clase_interna"}, {"invalid.mref", "referencia_a_metodo"},
        {"metajavbots.prohibido", "reflexion"}, {"metajavbots.casos", "junit"},
    };

    /** El concepto que hay detrás de ese error de javac (nunca null si hay conceptos). */
    public static Concepto delError(String codigo) {
        if (codigo != null) {
            String c = codigo.startsWith("compiler.err.") ? codigo.substring("compiler.err.".length()) : codigo;
            // Primero los que empiezan así; luego, los que lo llevan dentro («unclosed.char.lit» → «char.lit»).
            for (int vuelta = 0; vuelta < 2; vuelta++) {
                for (String[] e : ERRORES) {
                    if (vuelta == 0 ? c.startsWith(e[0]) : e[0].length() > 6 && c.contains(e[0])) {
                        Concepto k = de(e[1]);
                        if (k != null) return k;
                    }
                }
            }
        }
        Concepto k = de("error_de_compilacion");
        return k != null ? k : todos().isEmpty() ? null : todos().get(0);
    }

    /** El de ese id (o null). */
    public static Concepto de(String id) {
        for (Concepto c : todos()) if (c.id.equals(id)) return c;
        return null;
    }

    private static List<Concepto> todos;

    /** Todos, de lo más básico a lo avanzado (en el orden de los archivos). */
    public static synchronized List<Concepto> todos() {
        if (todos == null) {
            List<Concepto> l = new ArrayList<>();
            for (String a : ARCHIVOS) leer("/conceptos/" + a, l);
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }

    /** Lee un archivo: «=== id», claves «x: valor» y bloques «x:» con sus líneas debajo, con cuatro espacios delante. */
    private static void leer(String recurso, List<Concepto> l) {
        try (InputStream in = Concepto.class.getResourceAsStream(recurso)) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String id = null, bloque = null, linea;
            Map<String, String> v = new LinkedHashMap<>();
            Map<String, List<String>> bloques = new LinkedHashMap<>();
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("#")) continue;
                if (linea.startsWith("=== ")) {
                    if (id != null) l.add(new Concepto(id, v, bloques));
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
            if (id != null) l.add(new Concepto(id, v, bloques));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo leer res" + recurso, e);
        }
    }
}
