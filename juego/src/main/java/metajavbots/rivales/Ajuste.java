package metajavbots.rivales;

/**
 * AJUSTE — Lo que hace fácil o difícil a un rival. Sale de un nivel entre 0
 * (lo más fácil) y 1 (extrema) y de la agresividad del panel de la IA. No
 * cambia en toda la pelea.
 */
public final class Ajuste {

    /** Cuántos grados puede fallar al apuntar (hacia un lado o hacia el otro). */
    public final double punteria;
    /** De 0 a 1: cuánto adivina adónde irás (apunta por delante de ti). */
    public final double prediccion;
    /** Cada cuántos turnos se entera de dónde estás (1 = siempre). */
    public final int reaccion;
    /** De 0 a 1: lo que cambia de sentido y esquiva. */
    public final double esquiva;
    /** De 0 a 1: lo bien que usa lo especial y el escudo. */
    public final double usoEspecial;
    /** ¿Cambia de táctica entre rondas según lo que le hizo daño? */
    public final boolean adapta;
    /** ¿Se burla (con la agresividad alta)? */
    public final boolean burlas;
    /** Lo que dice al empezar (o null). */
    public final String saludo;
    /** El nivel de 0 a 1 del que sale (para las piezas que lleva). */
    public final double nivel;

    public Ajuste(double nivel, double agresividad, String saludo) {
        nivel = Math.max(0, Math.min(1, nivel));
        this.nivel = nivel;
        punteria = 22 - 21 * nivel;
        prediccion = nivel < 0.25 ? 0 : Math.min(1, (nivel - 0.2) * 1.25);
        reaccion = (int) Math.round(12 - 11 * nivel);
        esquiva = 0.1 + 0.8 * nivel;
        usoEspecial = 0.15 + 0.85 * nivel;
        adapta = nivel >= 0.5;
        burlas = agresividad >= 0.6;
        this.saludo = saludo;
    }
}
