package castillafantasx;

import java.util.Arrays;

/**
 * RITMO — Cuántos pasos de lógica tocan en cada imagen del monitor.
 *
 * La lógica va a 60 pasos por segundo. Un monitor de «60 Hz» casi nunca va a 60 justos (el de Fran,
 * a 59,94): sumando el tiempo tal cual, cada pocos segundos una imagen se quedaba sin paso o se
 * llevaba dos, y la imagen daba un tironcito. Aquí, si el monitor va casi al ritmo de la lógica
 * (o a la mitad, o al doble…), cada imagen lleva siempre los mismos pasos: nada de tirones.
 * Con otro monitor (75 Hz, 144 Hz) o si el ordenador se atasca, se suma el tiempo como antes.
 */
public final class Ritmo {

    // Si el ordenador se atasca, no se recupera más de un cuarto de segundo de golpe.
    public static final float ATASCO_MAXIMO = 0.25f;
    private static final int RECIENTES = 64, DESCARTADAS = 8; // de cuántas imágenes se mira lo que tardan, y cuántas se quitan por cada extremo (los atascos)
    private static final int PARA_DECIDIR = 48;               // con menos imágenes vistas aún no se sabe a qué ritmo va el monitor
    // El monitor «va al ritmo» si su imagen se desvía menos de la primera parte de un paso, y deja de ir si pasa de la segunda.
    private static final double HOLGURA_PARA_ENTRAR = 0.03, HOLGURA_PARA_SALIR = 0.06;
    private static final double ARRASTRE = 0.5;              // qué parte del desfase de una imagen pasa a la siguiente

    private final double paso;
    private final float[] recientes = new float[RECIENTES], orden = new float[RECIENTES];
    private int vistas, siguiente;
    private double acumulado;       // tiempo sin gastar, cuando el monitor no va al ritmo de la lógica
    private double desfase;         // cuando sí va: lo que esta imagen se adelantó o se retrasó
    private boolean alRitmo;        // el monitor va al ritmo de la lógica (o a la mitad, o a un tercio…)

    /**
     * Cuánto se ha andado del paso siguiente, de 0 a 1, para pintar entre el último paso y el anterior (Suavidad).
     * Con el monitor al ritmo de la lógica, 1: se pinta donde está cada uno.
     */
    public double avance() {
        return alRitmo ? 1 : Math.min(1, acumulado / paso);
    }

    public Ritmo(double paso) {
        this.paso = paso;
    }

    /** Ha pasado 'delta' segundos desde la imagen anterior: devuelve cuántos pasos de lógica hay que dar ahora. */
    public int pasos(float delta) {
        if (!(delta > 0)) return 0;
        delta = Math.min(delta, ATASCO_MAXIMO);
        recientes[siguiente] = delta;
        siguiente = (siguiente + 1) % RECIENTES;
        if (vistas < RECIENTES) vistas++;
        // Lo que tarda una imagen de este monitor: la media de las últimas, sin las más largas ni las más cortas
        // (un atasco suelto no la mueve).
        int cuantas = vistas;
        System.arraycopy(recientes, 0, orden, 0, cuantas);
        Arrays.sort(orden, 0, cuantas);
        int quitadas = cuantas > 4 * DESCARTADAS ? DESCARTADAS : 0;
        double normal = 0;
        for (int i = quitadas; i < cuantas - quitadas; i++) normal += orden[i];
        normal /= cuantas - 2 * quitadas;
        long porImagen = Math.round(normal / paso);
        double desvio = porImagen >= 1 ? Math.abs(normal - porImagen * paso) / paso : 1;
        if (cuantas < PARA_DECIDIR) alRitmo = false;
        else if (desvio < HOLGURA_PARA_ENTRAR) alRitmo = true;
        else if (desvio > HOLGURA_PARA_SALIR) alRitmo = false;
        if (alRitmo) {
            normal = porImagen * paso;      // la imagen del monitor, redondeada a pasos enteros
            // Cuántas imágenes del monitor han pasado de verdad: una, salvo que se haya perdido alguna.
            // El piquito que sobra o falta en cada imagen no se va guardando (era lo que daba el tirón):
            // de lo que quedó de la anterior solo se arrastra una parte.
            desfase = desfase * ARRASTRE + delta;
            long imagenes = Math.max(1, Math.round(desfase / normal));
            desfase -= imagenes * normal;
            acumulado = 0;
            return (int) (imagenes * porImagen);
        }
        desfase = 0;
        acumulado += delta;
        int pasos = (int) (acumulado / paso);
        acumulado -= pasos * paso;
        return pasos;
    }
}
