package castillafantasx.arte;

import java.util.Map;

/**
 * TALLER DE CURSORES — Los dibujos del cursor del juego, con símbolos de la
 * Castilla del XV (docs/investigacion/INVESTIGACION_EDITOR.md, 4): la cruz de la Orden (la
 * hoja-cruz roja), la misma en oro sobre un objeto, la manícula
 * de los manuscritos sobre una persona y la cruz derecha, de blasón, sobre un
 * enemigo. Se fabrican una vez; los usa interfaz.Cursor y la mira de la mazmorra.
 */
public final class TallerDeCursores {

    private TallerDeCursores() { }

    // La hoja-cruz con la punta hacia arriba: la punta es el punto exacto que señala.
    // R: rojo (u oro, sobre un objeto) · H: hueso (el filo de luz, para que destaque sobre cualquier suelo) · O: oro.
    private static final String[] HOJA = {
        ".....R.....",
        "....RHR....",
        "....RHR....",
        "....RHR....",
        "....RHR....",
        "....RHR....",
        ".R..RHR..R.",
        "RRRRRHRRRRR",
        ".R..RHR..R.",
        "....RHR....",
        "...RRHRR...",
        "..RR.R.RR..",
        "..R.....R..",
    };

    // Sobre un objeto: además del oro, un aro junto a la punta (que no se distinga solo por el color).
    private static final String[] ARO = { "OOO", "O.O", "OOO" };

    // La manícula: la manita que señala en los márgenes de los manuscritos, con su puño rojo.
    private static final String[] MANICULA = {
        "....HH......",
        "....HH......",
        "....HH......",
        "....HH......",
        "....HHHH.H..",
        "..H.HHHHHHH.",
        ".HH.HHHHHHH.",
        ".HHHHHHHHHH.",
        ".HHHHHHHHHH.",
        "..HHHHHHHH..",
        "...HHHHHHH..",
        "...RRRRRR...",
        "...RRRRRR...",
    };

    /** El punto exacto que señala cada dibujo, en sus píxeles: {x, y}. */
    public static final int[] PUNTA_DE_LA_HOJA = { 6, 1 }, PUNTA_DE_LA_MANICULA = { 5, 1 }, CENTRO_DEL_BLASON = { 6, 6 };

    /** Fabrica los dibujos y los deja en el mapa del Almacen ("cursor/reposo", "cursor/objeto", "cursor/persona", "cursor/enemigo"). */
    public static void fabricar(Map<String, Imagen> imagenes) {
        imagenes.put("cursor/reposo", reposo());
        imagenes.put("cursor/objeto", objeto());
        imagenes.put("cursor/persona", persona());
        imagenes.put("cursor/enemigo", enemigo());
    }

    public static Imagen reposo() {
        return conBorde(HOJA, Paleta.SANGRE_LUZ, null);
    }

    public static Imagen objeto() {
        return conBorde(HOJA, Paleta.ORO, ARO);
    }

    public static Imagen persona() {
        return conBorde(MANICULA, Paleta.SANGRE_LUZ, null);
    }

    /** La cruz de la Orden como en el blasón, con la punta hacia abajo. */
    public static Imagen enemigo() {
        String[] derecha = new String[HOJA.length];
        for (int i = 0; i < HOJA.length; i++) derecha[i] = HOJA[HOJA.length - 1 - i];
        return conBorde(derecha, Paleta.SANGRE_LUZ, null);
    }

    /** El dibujo con su color y un borde oscuro de un píxel alrededor (y, si se pide, el aro arriba a la derecha). */
    private static Imagen conBorde(String[] filas, int rojo, String[] aro) {
        int ancho = filas[0].length() + 2 + (aro == null ? 0 : 2), alto = filas.length + 2;
        Imagen im = new Imagen(ancho, alto);
        for (int y = 0; y < filas.length; y++) {
            for (int x = 0; x < filas[y].length(); x++) {
                char letra = filas[y].charAt(x);
                if (letra != '.') im.set(x + 1, y + 1, 0xFF000000 | (letra == 'H' ? Paleta.HUESO : rojo));
            }
        }
        for (int y = 0; aro != null && y < aro.length; y++) {
            for (int x = 0; x < aro[y].length(); x++) {
                if (aro[y].charAt(x) != '.') im.set(ancho - 1 - aro[y].length() + x, y + 1, 0xFF000000 | Paleta.ORO);
            }
        }
        // El borde: cada hueco vacío que toca un píxel pintado.
        int oscuro = 0xFF000000 | Paleta.TINTA;
        boolean[] pintado = new boolean[ancho * alto];
        for (int i = 0; i < pintado.length; i++) pintado[i] = im.px[i] != 0;
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                if (pintado[y * ancho + x]) continue;
                boolean toca = false;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int bx = x + dx, by = y + dy;
                        if (bx >= 0 && by >= 0 && bx < ancho && by < alto && pintado[by * ancho + bx]) toca = true;
                    }
                }
                if (toca) im.set(x, y, oscuro);
            }
        }
        return im;
    }
}
