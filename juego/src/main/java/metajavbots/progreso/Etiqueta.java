package metajavbots.progreso;

import metajavbots.textos.Idioma;

/**
 * ETIQUETA — El nivel del jugador. Tres categorías (Noob, Junior, Senior) con
 * cuatro escalones cada una, y una secreta, Master, para quien llega de
 * verdad al límite. Por qué doce escalones, y cuánto cuesta cada uno: en
 * docs/NIVELES.md (sale de comparar los niveles de las certificaciones de
 * Oracle, el modelo de Dreyfus y las horas de los cursos universitarios).
 *
 * Cada escalón tiene sus temas (res/datos/temas.txt): para subir hay que
 * ganar peleas y resolver los ejercicios de esos temas.
 */
public enum Etiqueta {
    NOOB_1("Noob I"), NOOB_2("Noob II"), NOOB_3("Noob III"), NOOB_4("Noob IV"),
    JUNIOR_1("Junior I"), JUNIOR_2("Junior II"), JUNIOR_3("Junior III"), JUNIOR_4("Junior IV"),
    SENIOR_1("Senior I"), SENIOR_2("Senior II"), SENIOR_3("Senior III"), SENIOR_4("Senior IV"),
    MASTER("Master");

    public final String nombre;

    Etiqueta(String nombre) {
        this.nombre = nombre;
    }

    /** Las cuatro categorías de etiquetas (cada una con sus escalones). */
    public enum Categoria { NOOB, JUNIOR, SENIOR, MASTER }

    public Categoria categoria() {
        if (this == MASTER) return Categoria.MASTER;
        return Categoria.values()[ordinal() / 4];
    }

    /** El escalón dentro de su categoría (1 a 4). */
    public int escalon() {
        return this == MASTER ? 1 : ordinal() % 4 + 1;
    }

    public Etiqueta siguiente() {
        return this == MASTER ? MASTER : values()[ordinal() + 1];
    }

    public Etiqueta anterior() {
        return ordinal() == 0 ? this : values()[ordinal() - 1];
    }

    /** Cuántos robots se pueden tener por partida en esta categoría (además hay que ganárselos: Progreso). */
    public int robotsPermitidos() {
        switch (categoria()) {
            case JUNIOR: return 2;
            case SENIOR: return 3;
            case MASTER: return 4;
            default: return 1;
        }
    }

    /** La fuerza de la computadora para esta etiqueta, de 0 (lo más fácil) a 1 (extrema). */
    public double nivelDeLaIA() {
        if (this == MASTER) return 1.0;
        return 0.05 + ordinal() * 0.075;
    }

    /** ¿Está al menos en la etiqueta e? */
    public boolean alMenos(Etiqueta e) {
        return ordinal() >= e.ordinal();
    }

    public String descripcion(Idioma i) {
        switch (categoria()) {
            case NOOB:
                return i.elegir("Aprendiendo lo básico: variables, condiciones, bucles, métodos.",
                        "Die Grundlagen: Variablen, Bedingungen, Schleifen, Methoden.",
                        "Learning the basics: variables, conditions, loops, methods.");
            case JUNIOR:
                return i.elegir("Objetos de verdad: clases, herencia, interfaces, excepciones, colecciones.",
                        "Echte Objekte: Klassen, Vererbung, Interfaces, Ausnahmen, Collections.",
                        "Real objects: classes, inheritance, interfaces, exceptions, collections.");
            case SENIOR:
                return i.elegir("Java avanzado: genéricos, lambdas, streams, hilos, diseño y pruebas.",
                        "Fortgeschrittenes Java: Generics, Lambdas, Streams, Threads, Entwurf und Tests.",
                        "Advanced Java: generics, lambdas, streams, threads, design and testing.");
            default:
                return i.elegir("El límite. Pocos llegan.", "Die Grenze. Nur wenige kommen an.", "The limit. Few get here.");
        }
    }

    public static Etiqueta de(String s) {
        try {
            return valueOf(s.strip());
        } catch (RuntimeException e) {
            return NOOB_1;
        }
    }
}
