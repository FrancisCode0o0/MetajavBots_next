package castillafantasx.combate;

/**
 * ESQUIVA — El quiebro del héroe: un movimiento corto y rápido,
 * rodando, que lo saca de un golpe si se hace a tiempo. Lleva hacia dónde va
 * y por dónde va; lo mueve cada mundo. Vale igual en la superficie y en la
 * mazmorra. Las cifras están en res/datos/salto.txt.
 */
public final class Esquiva {

    /** Hacia dónde rueda (una flecha de largo 1, en el suelo de cada mundo). */
    public double dirX, dirY;
    private double tiempo = -1;         // segundos que lleva esquivando; -1 = no esquiva
    private double espera;              // segundos que faltan para poder esquivar otra vez

    public boolean activa() {
        return tiempo >= 0;
    }

    /** ¿Está en la parte de la esquiva en que ningún golpe le alcanza? */
    public boolean intocable() {
        return activa() && tiempo < CifrasDeSalto.SEGUNDOS_INTOCABLE;
    }

    /** ¿Puede empezar una ahora? */
    public boolean lista() {
        return !activa() && espera <= 0;
    }

    /** Empieza a rodar hacia (dx, dy). */
    public void empezar(double dx, double dy) {
        double largo = Math.max(1e-6, Math.hypot(dx, dy));
        dirX = dx / largo;
        dirY = dy / largo;
        tiempo = 0;
    }

    /** Pasan 'dt' segundos. */
    public void avanzar(double dt) {
        if (espera > 0) espera -= dt;
        if (!activa()) return;
        tiempo += dt;
        if (tiempo >= CifrasDeSalto.SEGUNDOS_DE_ESQUIVA) {
            tiempo = -1;
            espera = CifrasDeSalto.ESPERA_DE_LA_ESQUIVA;
        }
    }

    /** De 0 a 1: por dónde va (0 si no esquiva). */
    public double avance() {
        return activa() ? Math.min(1, tiempo / CifrasDeSalto.SEGUNDOS_DE_ESQUIVA) : 0;
    }

    /** Por cuánto se multiplica ahora la velocidad de andar: sale deprisa y va frenando. */
    public double velocidad() {
        double frena = 1 - avance();
        return 1 + (CifrasDeSalto.VELOCIDAD_DE_ESQUIVA - 1) * frena * (2 - frena);
    }

    /** La corta (lo han tirado, cambia de mundo…). */
    public void reiniciar() {
        tiempo = -1;
        espera = 0;
    }
}
