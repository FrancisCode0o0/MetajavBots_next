package metajavbots.api;

import java.util.concurrent.atomic.AtomicLong;

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
        final AtomicLong pasos = new AtomicLong();
        long maximo = Long.MAX_VALUE;
        long limiteNanos = Long.MAX_VALUE;
        volatile boolean cancelada;
        /** Las líneas que se han ejecutado (solo con la traza: el modo máquina contra máquina). */
        final java.util.BitSet lineas = new java.util.BitSet();
        int ultima;
        /** Las líneas en el orden en que se ejecutan (hasta 5000). */
        int[] orden = new int[64];
        int enOrden;
    }

    static final InheritableThreadLocal<Cuenta> CUENTA = new InheritableThreadLocal<>();

    private Tope() {
    }

    /** Un paso más. Devuelve siempre true (para meterlo en la condición de un bucle). */
    public static boolean paso() {
        Cuenta c = CUENTA.get();
        if (c == null) return true;
        long p = c.pasos.incrementAndGet();
        if (p > c.maximo) throw new TopeDeCalculo("pasos: " + p);
        if ((p & 1023) == 0 && (c.cancelada || System.nanoTime() > c.limiteNanos)) {
            c.cancelada = true;
            throw new TopeDeCalculo("tiempo");
        }
        if (c.cancelada) throw new TopeDeCalculo("cancelado");
        return true;
    }

    /** Una línea que se ejecuta (lo mete el Inyector con la traza puesta; no cuenta como paso). */
    public static void linea(int n) {
        Cuenta c = CUENTA.get();
        if (c == null) return;
        synchronized (c) {
            c.lineas.set(n);
            c.ultima = n;
            if (c.enOrden < 5000) {
                if (c.enOrden == c.orden.length) c.orden = java.util.Arrays.copyOf(c.orden, c.orden.length * 2);
                c.orden[c.enOrden++] = n;
            }
        }
    }

    /** Los pasos dados en este turno (o en este ejercicio). */
    public static long pasos() {
        Cuenta c = CUENTA.get();
        return c == null ? 0 : c.pasos.get();
    }
}
