package castillafantasx.paisaje;

import castillafantasx.arte.Imagen;

/**
 * TROZO — Un cuadrado del paisaje ya pintado, tal como se ve en pantalla (lo
 * que está en alto, subido). Son tres dibujos del mismo suelo: de noche, con
 * el sol de la mañana (las sombras caen a poniente) y con el de la tarde (a
 * levante). El dibujo de la superficie los funde según la hora.
 *
 * En cada píxel, la opacidad dice si es terreno en alto (255) o llano (0):
 * lo que está en alto se vuelve a pintar en su turno, para tapar a quien
 * pasa por detrás.
 */
public final class Trozo {

    /** Lado de un trozo, en píxeles. */
    public static final int LADO = 256;

    /** Qué trozo es: el que empieza en (columna × LADO, fila × LADO). */
    public int columna, fila;
    public final Imagen noche = new Imagen(LADO, LADO), manana = new Imagen(LADO, LADO), tarde = new Imagen(LADO, LADO);
    /** Por dónde va: lo cambia quien lo pinta (el juego o su ayudante, que trabaja en otro hilo). */
    public final java.util.concurrent.atomic.AtomicInteger estado = new java.util.concurrent.atomic.AtomicInteger(PENDIENTE);
    public static final int PENDIENTE = 0, PINTANDOSE = 1, LISTO = 2;

    /** Lo deja pendiente de pintar otro cuadrado del mapa. */
    public void empezar(int columna, int fila) {
        this.columna = columna;
        this.fila = fila;
        estado.set(PENDIENTE);
    }
}
