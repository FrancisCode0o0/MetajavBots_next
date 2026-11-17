package metajavbots.rivales;

import metajavbots.textos.Idioma;

/** DIFICULTAD — Cinco escalones, de fácil a extrema (el panel de la IA los cambia; el modo adaptado los elige solo). */
public enum Dificultad {
    FACIL("Fácil", "Leicht", "Easy", 0.0),
    NORMAL("Normal", "Normal", "Normal", 0.3),
    DIFICIL("Difícil", "Schwer", "Hard", 0.55),
    MUY_DIFICIL("Muy difícil", "Sehr schwer", "Very hard", 0.8),
    EXTREMA("Extrema", "Extrem", "Extreme", 1.0);

    private final String es, de, en;
    /** Dónde cae entre 0 (lo más fácil) y 1 (lo más difícil). */
    public final double nivel;

    Dificultad(String es, String de, String en, double nivel) {
        this.es = es;
        this.de = de;
        this.en = en;
        this.nivel = nivel;
    }

    public String nombre(Idioma i) {
        return i.elegir(es, de, en);
    }

    /** La dificultad más cercana a un nivel entre 0 y 1. */
    public static Dificultad cercana(double nivel) {
        Dificultad mejor = FACIL;
        for (Dificultad d : values()) if (Math.abs(d.nivel - nivel) < Math.abs(mejor.nivel - nivel)) mejor = d;
        return mejor;
    }
}
