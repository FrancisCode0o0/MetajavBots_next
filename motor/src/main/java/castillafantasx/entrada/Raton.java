package castillafantasx.entrada;

import java.util.ArrayList;
import java.util.List;

/**
 * RATON — Dónde se pintó, en el último dibujo, cada cosa de los menús que se
 * puede señalar con el ratón: las filas, las casillas de las teclas y los
 * botones de un aviso. El dibujo las apunta al pintarlas; la Partida pregunta
 * cuál hay bajo el cursor. Así el ratón y el dibujo nunca se desacuerdan.
 */
public final class Raton {

    private Raton() { }

    /** Qué es lo que se señala: una fila de una lista, una casilla de una fila (el menú de teclas) o un botón. */
    public enum Zona { FILA, COLUMNA, BOTON, ATAJO, ESTRELLA, LUPA, AYUDA, FABRICA, CERRAR }      // ATAJO y ESTRELLA: los de F1 (una favorita o un reciente, y la estrella de cada fila); LUPA, AYUDA («?»), FABRICA (volver a lo de fábrica) y CERRAR (la × de cada menú): los de ORDEN_ERGONOMIA

    // Cada zona: {qué es, su número, x0, y0, x1, y1}, en píxeles de la imagen del juego.
    private static final List<float[]> ZONAS = new ArrayList<>();

    /** El menú para el que se apuntaron las zonas (el estado de la partida al pintarlas), o null si no se sabe. */
    public static Object deQuien;

    /** Olvida las zonas del dibujo anterior (al empezar a pintar la interfaz). */
    public static void olvidar() {
        olvidar(null);
    }

    /** Olvida las zonas y apunta para qué menú van a ser las nuevas: las de un menú no valen en otro. */
    public static void olvidar(Object menu) {
        ZONAS.clear();
        castillafantasx.interfaz.ListaQuieta.olvidarPintadas();
        deQuien = menu;
    }

    /** ¿Son de otro menú las zonas apuntadas? (El menú ha cambiado y aún no se ha vuelto a pintar.) */
    public static boolean sonDeOtro(Object menu) {
        return deQuien != null && deQuien != menu;
    }

    /** Apunta que en ese rectángulo se pintó la fila (o casilla, o botón) número 'indice'. */
    public static void apuntar(Zona zona, int indice, float x, float y, float ancho, float alto) {
        ZONAS.add(new float[] { zona.ordinal(), indice, x, y, x + ancho, y + alto });
    }

    /** El número de la fila (o casilla, o botón) que hay en ese punto, o -1 si no hay ninguna. */
    public static int en(Zona zona, float x, float y) {
        for (float[] z : ZONAS) {
            if (z[0] == zona.ordinal() && x >= z[2] && x < z[4] && y >= z[3] && y < z[5]) return (int) z[1];
        }
        return -1;
    }

    /** El centro de la fila (o casilla, o botón) número 'indice', o null si no se pintó: para las pruebas. */
    public static float[] centroDe(Zona zona, int indice) {
        return centroDe(zona, indice, Float.NaN);
    }

    /** Lo mismo, de la que cruza la altura 'y' (una casilla de esa fila); con NaN, la primera que haya. */
    public static float[] centroDe(Zona zona, int indice, float y) {
        for (float[] z : ZONAS) {
            boolean aEsaAltura = Float.isNaN(y) || (y >= z[3] && y < z[5]);
            if (z[0] == zona.ordinal() && z[1] == indice && aEsaAltura) return new float[] { (z[2] + z[4]) / 2, (z[3] + z[5]) / 2 };
        }
        return null;
    }
}
