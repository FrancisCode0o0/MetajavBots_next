package castillafantasx.paisaje;

import castillafantasx.arte.Almacen;
import castillafantasx.arte.Colores;
import castillafantasx.arte.Imagen;

/**
 * GRANO — El dibujo fino de un suelo, sacado de sus casillas de siempre
 * (res/arte/suelos): de cada píxel, su color y lo claro que es (de 0 a 1,
 * con la media en 0.5). El Pintor pone el color del paisaje y el grano le da
 * el punteado hecho a mano.
 */
public final class Grano {

    private static final int TAM = 32;
    private final Imagen[] variantes;
    private final float[][] claro;

    public Grano(String serie) {
        int n = Almacen.variantes(serie);
        variantes = new Imagen[n];
        claro = new float[n][TAM * TAM];
        for (int v = 0; v < n; v++) {
            variantes[v] = Almacen.imagen(serie + "_" + v);
            float media = 0;
            for (int i = 0; i < TAM * TAM; i++) {
                claro[v][i] = luz(variantes[v].px[i]);
                media += claro[v][i] / (TAM * TAM);
            }
            for (int i = 0; i < TAM * TAM; i++) {
                claro[v][i] = Math.max(0, Math.min(1, 0.5f + (claro[v][i] - media) * 2.2f));
            }
        }
    }

    /** Lo claro que es un color, de 0 a 1. */
    static float luz(int c) {
        return (((c >> 16) & 255) * 0.30f + ((c >> 8) & 255) * 0.59f + (c & 255) * 0.11f) / 255f;
    }

    /** Qué variante toca en la casilla de ese punto (siempre la misma para la misma casilla). */
    private int variante(int x, int y) {
        return (int) (Colores.ruido(x >> 5, y >> 5, 17) * variantes.length * 0.999f);
    }

    /** Lo claro que es el grano en ese punto del mapa, de 0 a 1. */
    public float claro(int x, int y) {
        return claro[variante(x, y)][(y & 31) * TAM + (x & 31)];
    }

    /** El color del dibujo de siempre en ese punto del mapa (0xRRGGBB). */
    public int color(int x, int y) {
        return variantes[variante(x, y)].px[(y & 31) * TAM + (x & 31)] & 0xFFFFFF;
    }
}
