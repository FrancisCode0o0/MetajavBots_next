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

    Jefe(String id, String tema, String modelo, double nivel, Trilingue nombre, Trilingue pista) {
        this.id = id;
        this.tema = tema;
        this.modelo = modelo;
        this.nivel = nivel;
        this.nombre = nombre;
        this.pista = pista;
    }

    public String nombre(Idioma i) {
        return nombre.en(i);
    }

    // TODO-FRAN: los nombres, los modelos y la dificultad de los jefes son tuyos.
    public static final List<Jefe> TODOS = List.of(
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
