package metajavbots.api;

/**
 * TOPE — Cuenta los pasos que da tu código. El juego mete, sin cambiar tus
 * líneas, una llamada a Tope.paso() en cada bucle y al empezar cada método:
 * así un bucle infinito no cuelga la pelea, y el código que hace mucho de más
 * cuesta turnos. Puedes mirar cuántos pasos llevas con Tope.pasos().
 *
 * Los hilos que crea tu código (solo en los ejercicios de hilos) comparten la
 * cuenta del que los creó.
 */
public final class Tope {

    /** La cuenta de un hilo: pasos dados, el máximo y la hora límite. */
    static final class Cuenta {
        volatile long pasos;
        long maximo = Long.MAX_VALUE;
        long limiteNanos = Long.MAX_VALUE;
        volatile boolean cancelada;
    }

    static final InheritableThreadLocal<Cuenta> CUENTA = new InheritableThreadLocal<>();

    private Tope() {
    }

    /** Un paso más. Devuelve siempre true (para meterlo en la condición de un bucle). */
    public static boolean paso() {
        Cuenta c = CUENTA.get();
        if (c == null) return true;
        long p = ++c.pasos;
        if (p > c.maximo) throw new TopeDeCalculo("pasos: " + p);
        if ((p & 1023) == 0 && (c.cancelada || System.nanoTime() > c.limiteNanos)) {
            c.cancelada = true;
            throw new TopeDeCalculo("tiempo");
        }
        if (c.cancelada) throw new TopeDeCalculo("cancelado");
        return true;
    }

    /** Los pasos dados en este turno (o en este ejercicio). */
    public static long pasos() {
        Cuenta c = CUENTA.get();
        return c == null ? 0 : c.pasos;
    }
}
