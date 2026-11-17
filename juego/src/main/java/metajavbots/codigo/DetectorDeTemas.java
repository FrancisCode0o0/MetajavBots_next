package metajavbots.codigo;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DETECTOR DE TEMAS — Mira un código y dice qué temas de Java usa de verdad
 * (no basta con nombrarlos en un comentario): herencia propia, interfaces
 * propias, colecciones, genéricos, excepciones propias, lambdas, streams,
 * recursión, concurrencia, Java moderno… Lo usan los jefes del modo
 * especial (solo se les vence usando su tema), el temario y el revisor.
 */
public final class DetectorDeTemas {

    public static final String HERENCIA = "herencia", INTERFACES = "interfaces", COLECCIONES = "colecciones",
            GENERICOS = "genericos", EXCEPCIONES = "excepciones", LAMBDAS = "lambdas", STREAMS = "streams",
            RECURSION = "recursion", HILOS = "hilos", MODERNO = "moderno", POLIMORFISMO = "polimorfismo",
            ENCAPSULACION = "encapsulacion", BUCLES = "bucles", ARRAYS = "arrays", METODOS = "metodos", CONDICIONALES = "condicionales";

    private static final Set<String> COLECCION = Set.of("List", "ArrayList", "LinkedList", "Map", "HashMap", "TreeMap", "LinkedHashMap",
            "Set", "HashSet", "TreeSet", "LinkedHashSet", "Deque", "ArrayDeque", "Queue", "PriorityQueue", "Collection", "Iterator");
    private static final Set<String> CONCURRENTE = Set.of("AtomicInteger", "AtomicLong", "AtomicBoolean", "AtomicReference",
            "ConcurrentHashMap", "CopyOnWriteArrayList", "ConcurrentLinkedQueue", "ReentrantLock", "Lock", "Semaphore", "Thread",
            "ExecutorService", "Executors", "CompletableFuture", "synchronized", "volatile");
    private static final Set<String> BASE = Set.of("Metajavbot", "MetajavbotDe", "MetajavbotEn", "Object");

    private DetectorDeTemas() {
    }

    public static Set<String> temas(String fuente) {
        Set<String> t = new LinkedHashSet<>();
        List<Lexico.Trozo> c = Lexico.codigo(fuente);
        Set<String> clasesPropias = new HashSet<>(), interfacesPropias = new HashSet<>(), abstractas = new HashSet<>();
        for (int i = 0; i + 1 < c.size(); i++) {
            if (c.get(i).es("class") || c.get(i).es("record") || c.get(i).es("enum")) clasesPropias.add(c.get(i + 1).texto);
            if (c.get(i).es("interface")) interfacesPropias.add(c.get(i + 1).texto);
            if (c.get(i).es("abstract") && i + 2 < c.size() && c.get(i + 1).es("class")) abstractas.add(c.get(i + 2).texto);
        }
        boolean campoPrivado = false, getter = false;
        for (int i = 0; i < c.size(); i++) {
            Lexico.Trozo x = c.get(i);
            String s = x.texto;
            Lexico.Trozo sig = i + 1 < c.size() ? c.get(i + 1) : null;
            if (s.equals("extends") && sig != null) {
                if (clasesPropias.contains(sig.texto) && !BASE.contains(sig.texto)) t.add(HERENCIA);
                if (abstractas.contains(sig.texto)) t.add(POLIMORFISMO);
                if (sig.texto.endsWith("Exception") || sig.texto.equals("Error") || sig.texto.equals("Throwable")) t.add(EXCEPCIONES);
            }
            if (s.equals("implements") && sig != null) {
                for (int k = i + 1; k < c.size() && !c.get(k).es("{"); k++) {
                    String n = c.get(k).texto;
                    if (interfacesPropias.contains(n) || n.equals("Comparable") || n.equals("Comparator") || n.equals("Iterable")) t.add(INTERFACES);
                }
            }
            if (x.clase == Lexico.Clase.NOMBRE && COLECCION.contains(s)) t.add(COLECCIONES);
            if (CONCURRENTE.contains(s)) t.add(HILOS);
            if ((s.equals("class") || s.equals("interface") || s.equals("record")) && i + 2 < c.size() && c.get(i + 2).es("<")) t.add(GENERICOS);
            if (s.equals("<") && i > 0 && (c.get(i - 1).es("static") || c.get(i - 1).es("public") || c.get(i - 1).es("private"))) t.add(GENERICOS);
            if (s.equals("->") || s.equals("::")) t.add(LAMBDAS);
            if ((s.equals("stream") && i > 0 && c.get(i - 1).es(".")) || s.equals("IntStream") || s.equals("Stream") || s.equals("Collectors")) t.add(STREAMS);
            if (s.equals("record") || s.equals("sealed") || s.equals("permits") || s.equals("yield")) t.add(MODERNO);
            if (s.equals("instanceof") && i + 2 < c.size() && c.get(i + 2).clase == Lexico.Clase.NOMBRE && !c.get(i + 2).es(")")) t.add(MODERNO);
            if (s.equals("for") || s.equals("while")) t.add(BUCLES);
            if (s.equals("if") || s.equals("switch")) t.add(CONDICIONALES);
            if (s.equals("[") && sig != null && sig.es("]")) t.add(ARRAYS);
            if (s.equals("private") && i + 3 < c.size() && !c.get(i + 3).es("(") && !c.get(i + 2).es("(")) campoPrivado = true;
            if (x.clase == Lexico.Clase.NOMBRE && (s.startsWith("get") || s.startsWith("es") && s.length() > 2 && Character.isUpperCase(s.charAt(2)))
                    && sig != null && sig.es("(") && i + 2 < c.size() && c.get(i + 2).es(")") && i + 3 < c.size() && c.get(i + 3).es("{")) getter = true;
        }
        if (campoPrivado && getter) t.add(ENCAPSULACION);
        boolean throwPropio = false, hayTry = false;
        for (int i = 0; i + 2 < c.size(); i++) {
            if (c.get(i).es("throw") && c.get(i + 1).es("new")) throwPropio = true;
            if (c.get(i).es("try") || c.get(i).es("catch")) hayTry = true;
        }
        if (throwPropio || hayTry) {
            if (t.contains(EXCEPCIONES) || throwPropio && hayTry) t.add(EXCEPCIONES);
        }
        if (recursion(c)) t.add(RECURSION);
        if (metodosPropios(c) >= 2) t.add(METODOS);
        return t;
    }

    /** ¿Algún método se llama a sí mismo? Se buscan los cuerpos de los métodos y su nombre dentro. */
    static boolean recursion(List<Lexico.Trozo> c) {
        for (int i = 0; i + 1 < c.size(); i++) {
            Lexico.Trozo n = c.get(i);
            if (n.clase != Lexico.Clase.NOMBRE || !c.get(i + 1).es("(") || i == 0) continue;
            Lexico.Trozo antes = c.get(i - 1);
            boolean declaracion = antes.clase == Lexico.Clase.TIPO_PRIMITIVO || antes.clase == Lexico.Clase.NOMBRE || antes.es(">") || antes.es("]");
            if (!declaracion || antes.es("new") || antes.es("return")) continue;
            // Saltar los parámetros hasta la «{».
            int k = i + 1, nivel = 0;
            for (; k < c.size(); k++) {
                if (c.get(k).es("(")) nivel++;
                else if (c.get(k).es(")") && --nivel == 0) break;
            }
            int j = k + 1;
            while (j < c.size() && !c.get(j).es("{") && !c.get(j).es(";")) j++;
            if (j >= c.size() || !c.get(j).es("{")) continue;
            int llaves = 0;
            for (int m = j; m < c.size(); m++) {
                if (c.get(m).es("{")) llaves++;
                else if (c.get(m).es("}") && --llaves == 0) break;
                else if (c.get(m).texto.equals(n.texto) && m + 1 < c.size() && c.get(m + 1).es("(")
                        && !(m > 0 && c.get(m - 1).es("."))) return true;
            }
        }
        return false;
    }

    private static int metodosPropios(List<Lexico.Trozo> c) {
        int n = 0;
        for (int i = 1; i + 1 < c.size(); i++) {
            if (c.get(i).clase == Lexico.Clase.NOMBRE && c.get(i + 1).es("(")
                    && (c.get(i - 1).clase == Lexico.Clase.TIPO_PRIMITIVO || c.get(i - 1).es("void"))) n++;
        }
        return n;
    }
}
