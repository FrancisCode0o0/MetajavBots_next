package castillafantasx.paisaje;

import castillafantasx.arte.Colores;

/**
 * RUIDO — El ruido suave del paisaje, hecho para ir deprisa (el Horno lo
 * pide muchas veces por píxel): manchas al azar que cambian poco a poco,
 * siempre las mismas para el mismo punto del mapa.
 */
public final class Ruido {

    private Ruido() { }

    /** Un número entero «al azar», siempre el mismo para la misma casilla (x, y): de él se sacan varios números pequeños. */
    public static int entero(int x, int y, int semilla) {
        int n = x * 374761393 + y * 668265263 + semilla * 1274126177;
        n = (n ^ (n >>> 13)) * 1274126177;
        return n ^ (n >>> 16);
    }

    /**
     * Ruido suave en el punto (x, y), de 0 a 1. Las manchas miden
     * 2 elevado a 'potencia' píxeles (3 → 8 píxeles, 6 → 64…): con potencias
     * de dos las cuentas son sumas y desplazamientos, sin divisiones.
     */
    public static float suave(int x, int y, int potencia, int semilla) {
        // Cada ruido empieza en un sitio distinto: así su rejilla no coincide con la de las casillas ni con la de otro ruido.
        x += semilla * 37;
        y += semilla * 101;
        int x0 = x >> potencia, y0 = y >> potencia, resto = (1 << potencia) - 1;
        float fx = (x & resto) / (float) (resto + 1), fy = (y & resto) / (float) (resto + 1);
        // De una esquina a otra se pasa con una curva en S (no en línea recta): así no se notan las esquinas de la rejilla.
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float a = Colores.ruido(x0, y0, semilla), b = Colores.ruido(x0 + 1, y0, semilla);
        float c = Colores.ruido(x0, y0 + 1, semilla), d = Colores.ruido(x0 + 1, y0 + 1, semilla);
        float arriba = a + (b - a) * fx, abajo = c + (d - c) * fx;
        return arriba + (abajo - arriba) * fy;
    }
}
