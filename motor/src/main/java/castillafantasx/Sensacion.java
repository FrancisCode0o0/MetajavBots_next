package castillafantasx;

/**
 * SENSACIÓN — Lo que hace que un golpe «se note» (superficie y mazmorra):
 * una breve congelación, una vibración de la pantalla según la fuerza y un
 * destello rojo de sangre, muy transparente y muy corto.
 */
public final class Sensacion {

    // --- Cifras PROVISIONALES (segundos y píxeles) ---
    private static final double CONGELACION_MINIMA = 0.025, CONGELACION_POR_FUERZA = 0.055;
    private static final double TEMBLOR_MINIMO = 1.2, TEMBLOR_POR_FUERZA = 3.8;
    private static final double SEGUNDOS_DE_TEMBLOR = 0.22, SEGUNDOS_DE_DESTELLO = 0.10;
    // Un golpe parado: congela un instante y, solo si el choque es fuerte, la pantalla vibra un poco.
    private static final double CONGELACION_DE_CHOQUE = 0.02, FUERZA_DE_CHOQUE_FUERTE = 0.5, TEMBLOR_DE_CHOQUE = 1.6;
    /** Lo más opaco que llega a ser el destello de sangre (0 a 1). */
    public static final float ALFA_DEL_DESTELLO = 0.16f;

    private double congelado;       // segundos que le quedan a la congelación
    public double temblor;          // píxeles que se mueve la pantalla ahora mismo
    public double destello;         // de 1 (recién golpeado) a 0

    /** Ha acertado un golpe. 'fuerza' va de 0 (un roce) a 1 (un mazazo). */
    public void golpe(double fuerza) {
        fuerza = Math.max(0, Math.min(1, fuerza));
        congelado = Math.max(congelado, CONGELACION_MINIMA + CONGELACION_POR_FUERZA * fuerza);
        temblor = Math.max(temblor, TEMBLOR_MINIMO + TEMBLOR_POR_FUERZA * fuerza);
        destello = 1;
    }

    /** Se ha parado un golpe (con el escudo, el arma o los antebrazos). 'fuerza' va de 0 a 1. Sin destello: no hay sangre. */
    public void choque(double fuerza) {
        fuerza = Math.max(0, Math.min(1, fuerza));
        congelado = Math.max(congelado, CONGELACION_DE_CHOQUE);
        if (fuerza >= FUERZA_DE_CHOQUE_FUERTE) temblor = Math.max(temblor, TEMBLOR_DE_CHOQUE * fuerza);
    }

    /** Pasa 'dt' segundos. Devuelve true mientras el juego debe seguir congelado. */
    public boolean avanzar(double dt) {
        if (congelado > 0) {
            congelado -= dt;
            return true;
        }
        temblor = Math.max(0, temblor - dt * (TEMBLOR_MINIMO + TEMBLOR_POR_FUERZA) / SEGUNDOS_DE_TEMBLOR);
        destello = Math.max(0, destello - dt / SEGUNDOS_DE_DESTELLO);
        return false;
    }
}
