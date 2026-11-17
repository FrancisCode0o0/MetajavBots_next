package castillafantasx.mazmorra;

import castillafantasx.salud.CifrasDeSenales;
import castillafantasx.salud.Senales;

/**
 * VELO DE LA VISTA — El cuerpo avisa, por los ojos del héroe
 * (docs/encargos_viejos/ENCARGO_INTERFAZ.md). Retoca la vista ya pintada del Trazador:
 *   - a punto de caer, la vista se ladea (ladear);
 *   - herido, los bordes se oscurecen y enrojecen, y laten con su corazón;
 *   - casi muerto, todo pierde el color y se nubla (velar).
 * Cuánto se nota cada cosa está en res/datos/senales.txt. La tabla de los
 * bordes se calcula una sola vez, al crear el velo.
 */
public final class VeloDeLaVista {

    // El color hacia el que se van los bordes (rojo muy oscuro) y el de la niebla de casi muerto (gris).
    private static final int BORDE_R = 0x34, BORDE_G = 0x04, BORDE_B = 0x06;
    private static final int NIEBLA = 0x4A;
    /** A qué distancia del centro empieza el borde (0 = el centro, 1 = la esquina). */
    private static final double DONDE_EMPIEZA_EL_BORDE = 0.38;

    private final int ancho, alto;
    /** Cuánto es «borde» cada píxel: 0 en el centro de la vista y 256 en las esquinas. */
    private final int[] borde;

    public VeloDeLaVista(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        borde = new int[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                double dx = (x - ancho / 2.0) / (ancho / 2.0), dy = (y - alto / 2.0) / (alto / 2.0);
                double lejos = Math.sqrt((dx * dx + dy * dy) / 2);
                double parte = Math.max(0, Math.min(1, (lejos - DONDE_EMPIEZA_EL_BORDE) / (1 - DONDE_EMPIEZA_EL_BORDE)));
                borde[y * ancho + x] = (int) Math.round(parte * parte * (3 - 2 * parte) * 256);
            }
        }
    }

    /**
     * Ladea la vista: cada columna sube o baja según lo lejos que esté del
     * centro. 'pendiente' es cuánto (0.05 = 5 píxeles cada 100); con signo.
     */
    public void ladear(int[] lienzo, double pendiente) {
        for (int x = 0; x < ancho; x++) {
            int baja = (int) Math.round((x - ancho / 2.0) * pendiente);
            if (baja > 0) {
                // La columna baja: se copia de abajo arriba, para no pisar lo que falta por copiar.
                // (El hueco de arriba se rellena con la fila 1: la fila 0 del Trazador puede quedar sin pintar.)
                for (int y = alto - 1; y >= 0; y--) {
                    lienzo[y * ancho + x] = lienzo[Math.max(1, y - baja) * ancho + x];
                }
            } else if (baja < 0) {
                for (int y = 0; y < alto; y++) {
                    lienzo[y * ancho + x] = lienzo[Math.min(alto - 1, y - baja) * ancho + x];
                }
            }
        }
    }

    // El agua (docs/encargos_viejos/ENCARGO_INTERFAZ.md): el color del agua turbia que tapa lo de debajo de la línea, el de la espuma de la
    // línea y cuánto ondea la línea (píxeles) y a qué ritmo.
    private static final int AGUA_R = 0x2A, AGUA_G = 0x4E, AGUA_B = 0x56, ESPUMA = 0xFFB8D0CC;
    private static final double ONDEA = 2.5, VECES_QUE_ONDEA = 3.0;

    /**
     * Nadando: lo que queda por debajo de la fila 'linea' está bajo el agua:
     * se va hacia el color del agua ('turbia', de 0 a 1), más cuanto más abajo,
     * y la línea ondea con una espuma clara.
     */
    public void agua(int[] lienzo, int linea, double turbia, double tiempo) {
        if (linea >= alto || turbia <= 0) return;
        int fuerza = (int) (256 * Math.min(1, turbia));
        for (int x = 0; x < ancho; x++) {
            int desde = linea + (int) Math.round(Math.sin(x * 0.11 + tiempo * VECES_QUE_ONDEA) * ONDEA);
            desde = Math.max(0, Math.min(alto - 1, desde));
            if (desde < alto) lienzo[desde * ancho + x] = ESPUMA;
            for (int y = desde + 1; y < alto; y++) {
                int i = y * ancho + x, c = lienzo[i];
                int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
                // Más turbio cuanto más hondo: de la mitad de 'fuerza' en la línea a 'fuerza' entera abajo del todo.
                int k = fuerza * (128 + 128 * (y - desde) / Math.max(1, alto - desde)) >> 8;
                r += ((AGUA_R - r) * k) >> 8;
                g += ((AGUA_G - g) * k) >> 8;
                b += ((AGUA_B - b) * k) >> 8;
                lienzo[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
            }
        }
    }

    /** Oscurece y enrojece los bordes (herido) y apaga y nubla toda la vista (casi muerto). */
    public void velar(int[] lienzo, Senales s) {
        if (s.herida <= 0 && s.agonia <= 0) return;
        // Todo en partes de 256, para hacer las cuentas con enteros.
        int fuerza = (int) (256 * Math.min(1, s.herida * (CifrasDeSenales.BORDE + CifrasDeSenales.PULSO * s.pulso())));
        int apaga = (int) (256 * CifrasDeSenales.SE_APAGA * s.agonia);
        int nubla = (int) (256 * CifrasDeSenales.SE_NUBLA * s.agonia);
        for (int i = 0; i < lienzo.length; i++) {
            int c = lienzo[i];
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            if (nubla > 0) {
                // Se nubla: cada píxel se emborrona con el de al lado y se va hacia un gris de niebla (más en los bordes).
                int vecino = lienzo[i + 1 < lienzo.length ? i + 1 : i];
                int mitad = nubla / 2;
                r += ((((vecino >> 16) & 0xFF) - r) * mitad) >> 8;
                g += ((((vecino >> 8) & 0xFF) - g) * mitad) >> 8;
                b += (((vecino & 0xFF) - b) * mitad) >> 8;
                int niebla = (nubla * (96 + borde[i] * 160 / 256)) >> 8;
                r += ((NIEBLA - r) * niebla) >> 8;
                g += ((NIEBLA - g) * niebla) >> 8;
                b += ((NIEBLA - b) * niebla) >> 8;
            }
            if (apaga > 0) {
                // Se apaga: el color se va hacia su gris.
                int gris = (r * 77 + g * 150 + b * 29) >> 8;
                r += ((gris - r) * apaga) >> 8;
                g += ((gris - g) * apaga) >> 8;
                b += ((gris - b) * apaga) >> 8;
            }
            int k = (borde[i] * fuerza) >> 8;
            if (k > 0) {
                r += ((BORDE_R - r) * k) >> 8;
                g += ((BORDE_G - g) * k) >> 8;
                b += ((BORDE_B - b) * k) >> 8;
            }
            lienzo[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
    }
}
