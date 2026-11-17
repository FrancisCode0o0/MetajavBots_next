package castillafantasx;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CUADRILLA — Reparte un trabajo entre los núcleos del procesador (encargo de la
 * optimización): unos cuantos obreros, hilos que esperan dormidos, y el hilo del
 * juego, que trabaja con ellos. Cada tarea se hace entera en un solo hilo y
 * hacer() no vuelve hasta que están todas: lo que sale es lo mismo que si se
 * hicieran una tras otra, siempre que dos tareas no toquen lo mismo.
 */
public final class Cuadrilla {

    /** Los obreros, además del hilo del juego: uno menos que los hilos del procesador, y como mucho siete. */
    public static final int OBREROS = Math.max(0, Math.min(7, Runtime.getRuntime().availableProcessors() - 1));
    /** false: todo en el hilo del juego, una tarea tras otra (para comparar). */
    public static boolean repartir = true;

    /** Una tarea; 'obrero' dice quién la hace (0 = el hilo del juego; 1, 2… los obreros), por si necesita sus propias herramientas. */
    public interface Tarea {
        void hacer(int obrero);
    }

    /** Un reparto: sus tareas, la siguiente que queda por coger y cuántas faltan por acabar. */
    private static final class Tanda {
        final List<? extends Tarea> tareas;
        final AtomicInteger siguiente = new AtomicInteger();
        int pendientes;
        Throwable fallo;

        Tanda(List<? extends Tarea> tareas) {
            this.tareas = tareas;
            this.pendientes = tareas.size();
        }
    }

    private static final Object CERROJO = new Object();
    private static Thread[] hilos;
    private static Tanda enCurso;          // la tanda que se está haciendo (null = ninguna)

    private Cuadrilla() {
    }

    /** Hace todas las tareas, repartidas, y vuelve cuando han acabado. Si alguna falla, el fallo sale aquí. */
    public static void hacer(List<? extends Tarea> lista) {
        if (lista.isEmpty()) return;
        Tanda tanda = new Tanda(lista);
        boolean solo;
        synchronized (CERROJO) {
            // Sin obreros, con una sola tarea o si ya hay una tanda en marcha (una tarea que reparte a su vez): una tras otra, aquí.
            solo = !repartir || OBREROS == 0 || lista.size() == 1 || enCurso != null;
            if (!solo) {
                if (hilos == null) contratar();
                enCurso = tanda;
                CERROJO.notifyAll();
            }
        }
        if (solo) {
            for (Tarea t : lista) t.hacer(0);
            return;
        }
        trabajar(0, tanda);
        synchronized (CERROJO) {
            while (tanda.pendientes > 0) {
                try {
                    CERROJO.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            enCurso = null;
        }
        if (tanda.fallo instanceof RuntimeException) throw (RuntimeException) tanda.fallo;
        if (tanda.fallo instanceof Error) throw (Error) tanda.fallo;
        if (tanda.fallo != null) throw new IllegalStateException(tanda.fallo);
    }

    /** Coge tareas de la tanda hasta que no quedan. */
    private static void trabajar(int obrero, Tanda tanda) {
        for (int i = tanda.siguiente.getAndIncrement(); i < tanda.tareas.size(); i = tanda.siguiente.getAndIncrement()) {
            Throwable fallo = null;
            try {
                tanda.tareas.get(i).hacer(obrero);
            } catch (Throwable t) {
                fallo = t;
            }
            synchronized (CERROJO) {
                if (fallo != null && tanda.fallo == null) tanda.fallo = fallo;
                if (--tanda.pendientes == 0) CERROJO.notifyAll();
            }
        }
    }

    /** Crea los obreros (hilos que no impiden cerrar el juego) la primera vez que hacen falta. */
    private static void contratar() {
        hilos = new Thread[OBREROS];
        for (int k = 0; k < OBREROS; k++) {
            final int obrero = k + 1;
            hilos[k] = new Thread(() -> {
                Tanda hecha = null;
                while (true) {
                    Tanda tanda;
                    synchronized (CERROJO) {
                        while (enCurso == null || enCurso == hecha) {
                            try {
                                CERROJO.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        tanda = enCurso;
                    }
                    trabajar(obrero, tanda);
                    hecha = tanda;
                }
            }, "obrero " + obrero);
            hilos[k].setDaemon(true);
            hilos[k].start();
        }
    }
}
