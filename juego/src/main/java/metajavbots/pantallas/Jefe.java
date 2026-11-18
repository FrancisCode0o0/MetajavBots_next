package metajavbots.pantallas;

import java.util.List;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * JEFE — Los jefes del modo especial: cada uno es inmune (casi) salvo que tu
 * código use de verdad su tema (lo mira el DetectorDeTemas). Vencerlo da una
 * pieza épica (desbloqueo «jefe:<id>» en res/datos/piezas.txt).
 */
public final class Jefe {

    public final String id, tema, modelo;
    public final Trilingue nombre, pista;
    public final double nivel;
    /** Si es un líder de gimnasio (los temas básicos, más fácil) y no un jefe. */
    public final boolean gimnasio;
    /** Lo que lleva grabado su medalla («if», «for», «<T>»…) y su color. */
    public final String simbolo;
    public final int color;

    Jefe(String id, String tema, String modelo, double nivel, Trilingue nombre, Trilingue pista) {
        this(id, tema, modelo, nivel, nombre, pista, false);
    }

    Jefe(String id, String tema, String modelo, double nivel, Trilingue nombre, Trilingue pista, boolean gimnasio) {
        this.id = id;
        this.tema = tema;
        this.modelo = modelo;
        this.nivel = nivel;
        this.nombre = nombre;
        this.pista = pista;
        this.gimnasio = gimnasio;
        String[] m = MEDALLAS.getOrDefault(tema, new String[]{"?", "8A92A3"});
        this.simbolo = m[0];
        this.color = Integer.parseInt(m[1], 16);
    }

    /** El símbolo y el color de la medalla de cada tema. */
    private static final java.util.Map<String, String[]> MEDALLAS = java.util.Map.ofEntries(
            java.util.Map.entry("condicionales", new String[]{"if", "5BD685"}), java.util.Map.entry("bucles", new String[]{"for", "3FA9F5"}),
            java.util.Map.entry("arrays", new String[]{"[ ]", "FFC857"}), java.util.Map.entry("metodos", new String[]{"( )", "FF8FB1"}),
            java.util.Map.entry("encapsulacion", new String[]{"get", "3FE0D0"}), java.util.Map.entry("polimorfismo", new String[]{"@", "B07CFF"}),
            java.util.Map.entry("moderno", new String[]{"rec", "E5C07B"}), java.util.Map.entry("herencia", new String[]{"ext", "FF6B6B"}),
            java.util.Map.entry("interfaces", new String[]{"imp", "7FB2FF"}), java.util.Map.entry("colecciones", new String[]{"{ }", "9BE15D"}),
            java.util.Map.entry("excepciones", new String[]{"!", "FF9F43"}), java.util.Map.entry("genericos", new String[]{"<T>", "C792EA"}),
            java.util.Map.entry("lambdas", new String[]{"->", "56CCF2"}), java.util.Map.entry("streams", new String[]{"≈", "2DD4BF"}),
            java.util.Map.entry("recursion", new String[]{"f(f)", "F472B6"}), java.util.Map.entry("hilos", new String[]{"||", "FACC15"}));

    public String nombre(Idioma i) {
        return nombre.en(i);
    }

    // TODO-FRAN: los nombres, los modelos y la dificultad de los jefes son tuyos.
    public static final List<Jefe> TODOS = List.of(
            new Jefe("gim_condicionales", "condicionales", "grulla", 0.22,
                    new Trilingue("Líder Bifurca", "Leiterin Gabelung", "Leader Fork"),
                    new Trilingue("Gimnasio de las condiciones: solo le haces daño si tu código decide con if o switch.",
                            "Arena der Bedingungen: Du triffst sie nur, wenn dein Code mit if oder switch entscheidet.",
                            "Gym of conditions: you only hurt her if your code decides with if or switch."), true),
            new Jefe("gim_bucles", "bucles", "remolino", 0.25,
                    new Trilingue("Líder Tiovivo", "Leiter Karussell", "Leader Carousel"),
                    new Trilingue("Gimnasio de los bucles: usa for o while en tu código.",
                            "Arena der Schleifen: Benutze for oder while in deinem Code.",
                            "Gym of loops: use for or while in your code."), true),
            new Jefe("gim_metodos", "metodos", "chispa", 0.28,
                    new Trilingue("Líder Recetario", "Leiterin Rezeptbuch", "Leader Cookbook"),
                    new Trilingue("Gimnasio de los métodos: escribe un método propio y llámalo.",
                            "Arena der Methoden: Schreibe eine eigene Methode und rufe sie auf.",
                            "Gym of methods: write your own method and call it."), true),
            new Jefe("gim_arrays", "arrays", "serrucho", 0.3,
                    new Trilingue("Líder Fila India", "Leiter Gänsemarsch", "Leader Single File"),
                    new Trilingue("Gimnasio de los arrays: guarda algo en un array (int[], double[]…).",
                            "Arena der Arrays: Speichere etwas in einem Array (int[], double[] …).",
                            "Gym of arrays: store something in an array (int[], double[]…)."), true),
            new Jefe("gim_encapsulacion", "encapsulacion", "galapago", 0.35,
                    new Trilingue("Líder Caja Fuerte", "Leiterin Tresor", "Leader Safe"),
                    new Trilingue("Gimnasio de la encapsulación: atributos privados y métodos para leerlos o cambiarlos.",
                            "Arena der Kapselung: private Attribute und Methoden, um sie zu lesen oder zu ändern.",
                            "Gym of encapsulation: private fields and methods to read or change them."), true),
            new Jefe("gim_polimorfismo", "polimorfismo", "volteo", 0.4,
                    new Trilingue("Líder Cambiaformas", "Leiter Gestaltwandler", "Leader Shapeshifter"),
                    new Trilingue("Gimnasio del polimorfismo: varias clases propias que sobrescriben el mismo método.",
                            "Arena der Polymorphie: mehrere eigene Klassen, die dieselbe Methode überschreiben.",
                            "Gym of polymorphism: several classes of your own overriding the same method."), true),
            new Jefe("gim_moderno", "moderno", "barrena", 0.55,
                    new Trilingue("Líder Último Grito", "Leiterin Letzter Schrei", "Leader Latest Craze"),
                    new Trilingue("Gimnasio del Java moderno: usa un record, una clase sealed o instanceof con patrón.",
                            "Arena des modernen Java: Benutze einen record, eine sealed-Klasse oder instanceof mit Muster.",
                            "Gym of modern Java: use a record, a sealed class or pattern-matching instanceof."), true),
            new Jefe("herencia", "herencia", "filo", 0.45,
                    new Trilingue("El Heredero", "Der Erbe", "The Heir"),
                    new Trilingue("Solo le hace daño un robot cuyo código tenga una clase propia que herede de otra propia (extends).",
                            "Nur ein Roboter, dessen Code eine eigene Klasse hat, die von einer anderen eigenen erbt (extends), kann ihm schaden.",
                            "Only a robot whose code has its own class extending another own class (extends) can hurt it.")),
            new Jefe("interfaces", "interfaces", "bastion", 0.5,
                    new Trilingue("La Muralla del Contrato", "Die Vertragsmauer", "The Contract Wall"),
                    new Trilingue("Declara una interfaz propia e impleméntala (implements) en tu código.",
                            "Deklariere ein eigenes Interface und implementiere es (implements).",
                            "Declare your own interface and implement it (implements).")),
            new Jefe("colecciones", "colecciones", "tejedora", 0.55,
                    new Trilingue("La Colmena", "Der Bienenstock", "The Hive"),
                    new Trilingue("Usa colecciones (List, Map, Set…) para recordar lo que ves.",
                            "Benutze Collections (List, Map, Set…), um dir zu merken, was du siehst.",
                            "Use collections (List, Map, Set…) to remember what you see.")),
            new Jefe("excepciones", "excepciones", "chatarra", 0.55,
                    new Trilingue("El Atrapa-errores", "Der Fehlerfänger", "The Error Catcher"),
                    new Trilingue("Lanza (throw) y atrapa (try/catch) excepciones en tu código; mejor si es una propia.",
                            "Wirf (throw) und fange (try/catch) Ausnahmen in deinem Code; am besten eine eigene.",
                            "Throw and catch (try/catch) exceptions in your code; better if it is your own.")),
            new Jefe("genericos", "genericos", "abaco", 0.65,
                    new Trilingue("El Núcleo Genérico", "Der Generische Kern", "The Generic Core"),
                    new Trilingue("Escribe una clase o un método genérico (<T>).",
                            "Schreibe eine generische Klasse oder Methode (<T>).",
                            "Write a generic class or method (<T>).")),
            new Jefe("lambdas", "lambdas", "voltio", 0.65,
                    new Trilingue("El Látigo Lambda", "Die Lambda-Peitsche", "The Lambda Whip"),
                    new Trilingue("Usa lambdas (->) o referencias a métodos (::).",
                            "Benutze Lambdas (->) oder Methodenreferenzen (::).",
                            "Use lambdas (->) or method references (::).")),
            new Jefe("streams", "streams", "pixel", 0.7,
                    new Trilingue("La Corriente", "Der Strom", "The Stream"),
                    new Trilingue("Usa streams (.stream(), filter, map…) para decidir.",
                            "Benutze Streams (.stream(), filter, map…), um zu entscheiden.",
                            "Use streams (.stream(), filter, map…) to decide.")),
            new Jefe("recursion", "recursion", "eclipse", 0.7,
                    new Trilingue("El Espejo Infinito", "Der Endlose Spiegel", "The Infinite Mirror"),
                    new Trilingue("Escribe un método que se llame a sí mismo (con su caso base, ¡ojo al tope de cálculo!).",
                            "Schreibe eine Methode, die sich selbst aufruft (mit Abbruchbedingung, Vorsicht beim Rechenlimit!).",
                            "Write a method that calls itself (with a base case; mind the step limit!).")),
            new Jefe("hilos", "hilos", "avispa", 0.75,
                    new Trilingue("El Enjambre Paralelo", "Der Parallelschwarm", "The Parallel Swarm"),
                    new Trilingue("Usa clases seguras para hilos (AtomicInteger, ConcurrentHashMap…) o synchronized.",
                            "Benutze threadsichere Klassen (AtomicInteger, ConcurrentHashMap…) oder synchronized.",
                            "Use thread-safe classes (AtomicInteger, ConcurrentHashMap…) or synchronized.")));

    public static Jefe de(String id) {
        for (Jefe j : TODOS) if (j.id.equals(id)) return j;
        return null;
    }
}
