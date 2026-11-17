package castillafantasx.mundo;

import java.util.Map;

import castillafantasx.arte.Colores;
import castillafantasx.arte.Imagen;
import castillafantasx.arte.Paleta;

/**
 * MANCHAS — Las pinceladas del editor: sangre, barro, hollín, musgo y
 * humedad. Cada una tiene unas cuantas formas, hechas una vez al arrancar con
 * los colores de la Paleta ("manchas/<tipo>_<n>" en el Almacen); el pincel
 * las va dejando sobre el suelo, las paredes y los objetos.
 */
public final class Manchas {

    private Manchas() { }

    /** Un tipo de mancha: su id, su nombre, sus tres tonos (letras de la Paleta: sombra, base y luz) y lo que tapa (de 0 a 1). */
    public static final class Tipo {
        public final String id, nombre;
        final String tonos;
        public final float opacidad;
        /** Cuánto de la mancha es salpicadura suelta (0 = un charco entero; 1 = solo motas). */
        final float suelta;

        Tipo(String id, String nombre, String tonos, float opacidad, float suelta) {
            this.id = id;
            this.nombre = nombre;
            this.tonos = tonos;
            this.opacidad = opacidad;
            this.suelta = suelta;
        }
    }

    // TODO-FRAN: los colores y lo que tapa cada mancha (PROVISIONALES).
    public static final Tipo[] TIPOS = {
        new Tipo("sangre", "Sangre", "uvw", 0.85f, 0.35f),
        new Tipo("barro", "Barro", "hij", 0.80f, 0.20f),
        new Tipo("hollin", "Hollín", "abc", 0.70f, 0.55f),
        new Tipo("musgo", "Musgo", "pqr", 0.80f, 0.60f),
        new Tipo("humedad", "Humedad", "sxt", 0.45f, 0.30f),
    };

    /** Cuántas formas distintas tiene cada mancha y lo que mide su dibujo (el pincel de tamaño 1). */
    public static final int VARIANTES = 6, LADO = 20;

    public static Tipo tipo(String id) {
        for (Tipo t : TIPOS) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    /** El nombre del dibujo de esa mancha en esa variante, o null si no existe ese tipo. */
    public static String dibujo(String tipo, int variante) {
        return tipo(tipo) == null ? null : "manchas/" + tipo + "_" + Math.floorMod(variante, VARIANTES);
    }

    /** Fabrica los dibujos de todas las manchas (lo llama el Almacen al arrancar, una vez). */
    public static void fabricar(Map<String, Imagen> imagenes) {
        for (int t = 0; t < TIPOS.length; t++) {
            for (int v = 0; v < VARIANTES; v++) {
                imagenes.put("manchas/" + TIPOS[t].id + "_" + v, pintar(TIPOS[t], t * 100 + v));
            }
        }
    }

    /** Una mancha: un borrón de borde irregular, con el centro lleno y las orillas deshechas en motas, en tres tonos. */
    private static Imagen pintar(Tipo tipo, int semilla) {
        Imagen im = new Imagen(LADO, LADO);
        float centro = (LADO - 1) / 2f, radio = LADO * 0.42f;
        int sombra = Paleta.argb(tipo.tonos.charAt(0)), base = Paleta.argb(tipo.tonos.charAt(1)), luz = Paleta.argb(tipo.tonos.charAt(2));
        for (int y = 0; y < LADO; y++) {
            for (int x = 0; x < LADO; x++) {
                float dx = x - centro, dy = (y - centro) * 1.15f;
                // El borde entra y sale: el radio cambia con un ruido suave según hacia dónde se mire.
                float borde = radio * (0.62f + 0.5f * Colores.ruidoSuave(x, y, 5, semilla));
                float lejos = (float) Math.hypot(dx, dy) / borde;
                float mota = Colores.ruido(x, y, semilla + 7);
                // Dentro del todo, lleno; hacia la orilla, cada vez más motas sueltas.
                float lleno = 1 - tipo.suelta * 0.9f;
                boolean pinta = lejos < lleno * 0.75f ? mota > tipo.suelta * 0.35f : lejos < 1 && mota > 0.25f + 0.7f * (lejos - lleno * 0.75f) / (1 - lleno * 0.75f + 0.001f);
                if (!pinta) continue;
                float tono = Colores.ruidoSuave(x, y, 3, semilla + 13);
                im.set(x, y, tono < 0.35f ? sombra : tono > 0.78f ? luz : base);
            }
        }
        return im;
    }
}
