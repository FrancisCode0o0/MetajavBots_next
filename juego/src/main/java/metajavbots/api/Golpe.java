package metajavbots.api;

/**
 * GOLPE — Un golpe: el que recibes (alRecibirGolpe) o el que das (alAcertar).
 */
public interface Golpe {

    /** Cuánto blindaje quitó. */
    double dano();

    /** Con qué: "laser", "bala", "misil", "cuerpo", "choque" o "pared". */
    String arma();

    /** De dónde vino (grados, absoluto): si te dan, girarHacia(g.angulo() + 90) para esquivar de lado. */
    double angulo();

    /** Qué pieza recibió el golpe: "cabeza", "izquierda", "derecha" o "piernas". */
    String pieza();
}
