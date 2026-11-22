package metajavbots.combate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import metajavbots.api.Puente;

/**
 * EJECUTOR PARALELO — Ejecuta la fase de pensamiento de varios robots a la vez
 * en un grupo acotado de hilos demonio para las peleas Senior en paralelo.
 *
 * Garantías de seguridad:
 * - Nunca bloquea indefinidamente el hilo de renderizado (límite estricto por turno).
 * - Cancela tanto el {@code Future} como la cuenta de {@code Tope} si un robot se atasca.
 * - {@link #cerrar()} detiene el grupo inmediatamente para no dejar hilos colgados.
 */
final class EjecutorParalelo {

    private static final AtomicInteger CONTADOR_POOL = new AtomicInteger();

    private final ExecutorService pool;
    private volatile boolean cerrado;

    EjecutorParalelo(int combatientes) {
        int hilos = Math.max(2, Math.min(4, combatientes));
        int idPool = CONTADOR_POOL.incrementAndGet();
        AtomicInteger idHilo = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(hilos, r -> {
            Thread t = new Thread(r, "metajavbots-paralelo-" + idPool + "-" + idHilo.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /** Ejecuta {@code pensarUno} para cada cuerpo en paralelo con un tiempo máximo estricto por turno. */
    void ejecutarTurno(Pelea pelea, Cuerpo[] cuerpos, Consumer<Cuerpo> pensarUno, long nanosPorTurno) {
        if (cerrado || cuerpos == null || cuerpos.length == 0) return;
        int n = cuerpos.length;
        List<Future<?>> tareas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final Cuerpo c = cuerpos[i];
            tareas.add(pool.submit(() -> pensarUno.accept(c)));
        }
        long limite = System.nanoTime() + Math.max(20_000_000L, Math.min(120_000_000L, nanosPorTurno * 3L));
        for (int i = 0; i < n; i++) {
            Future<?> f = tareas.get(i);
            long queda = Math.max(1L, limite - System.nanoTime());
            try {
                f.get(queda, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                Cuerpo c = cuerpos[i];
                Puente.cancelar(c.cuentaActiva);
                f.cancel(true);
                c.deuda = Math.min(20, c.deuda + 3);
                pelea.informe(c.indice).excesosTiempo++;
                c.combatiente.escribir(pelea.turno(), "⏱ Tiempo agotado en ejecución paralela: hilo interrumpido.");
                pelea.suceso(Suceso.Tipo.TOPE, c.indice, c.x, c.y, 0, c.deuda, "tiempo");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                f.cancel(true);
            } catch (ExecutionException e) {
                // las excepciones del robot ya se capturan dentro de Pelea.ejecutar
            }
        }
    }

    /** Detiene inmediatamente los hilos del grupo para no dejar hilos vivos tras la pelea. */
    void cerrar() {
        if (cerrado) return;
        cerrado = true;
        pool.shutdownNow();
        try {
            pool.awaitTermination(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    boolean estaCerrado() {
        return cerrado && pool.isShutdown();
    }
}
