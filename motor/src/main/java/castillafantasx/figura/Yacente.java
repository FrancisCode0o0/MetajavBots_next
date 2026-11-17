package castillafantasx.figura;

import java.util.HashMap;
import java.util.Map;

/**
 * YACENTE — Esculpe a quien ha muerto: su mancha en el suelo y, encima, su
 * cuerpo tendido. La mancha son unas pocas rodelas pegadas al suelo, de un
 * material que se transparenta: oscura en el centro y más clara en el borde.
 * Lo usan igual la superficie, la mazmorra, la galería y las láminas.
 */
public final class Yacente {

    private Yacente() { }

    // --- Cifras PROVISIONALES de la mancha ---
    private static final int ALFA_DEL_BORDE = 135, ALFA_DEL_CENTRO = 205;     // de 0 a 255: cuánto tapa el suelo
    private static final double PARTE_DEL_CENTRO = 0.62;      // el centro oscuro, respecto al radio de cada charco
    private static final double ACHATADA = 0.85;              // algo más ancha que larga
    private static final int CHARCOS = 4;                     // el grande y los pequeños que le salen alrededor
    private static final double RADIO_MINIMO = 0.9;           // más pequeño que esto, un charco no se pinta
    private static final double CORRIDA = 0.4;                // lo que se aparta del centro del cuerpo el charco grande, en partes de su radio

    // Los dos materiales de cada color de mancha {borde, centro}: se crean una vez.
    private static final Map<Integer, Material[]> MATERIALES = new HashMap<>();
    private static final int COLOR_DEL_BRILLO = 0xF2F0DC;

    private static synchronized Material[] materialesDe(int color) {
        Material[] m = MATERIALES.get(color);
        if (m == null) {
            m = new Material[] { Material.velo(color, ALFA_DEL_BORDE), Material.velo(color, ALFA_DEL_CENTRO) };
            MATERIALES.put(color, m);
        }
        return m;
    }

    /**
     * Pone en el Escultor la mancha y el cuerpo (la cámara ya está colocada; no llama a terminar()).
     * 'm' es el momento del cuerpo (ver Instante.tendido). 'color': el RRGGBB de
     * la mancha, o negativo si no deja. 'radio': lo que mide la mancha ya
     * extendida, en píxeles del mapa. 'extension': de 0 a 1, cuánto se ha
     * extendido. 'forma': un número propio de cada cuerpo, para que no haya
     * dos manchas iguales. 'brillo': de 0 a 1, el resplandor que lo envuelve (quien estaba hechizado).
     */
    public static void esculpir(Escultor e, Figura f, Instante m, int color, double radio, double extension, long forma, double brillo) {
        P3 centro = f.centroTendido(m);
        if (color >= 0 && extension > 0) mancha(e, centro, materialesDe(color), radio / f.talla(), extension, forma);
        f.esculpir(e, m);
        // El resplandor pálido que envuelve al hechizado mientras vuelve a ser quien era.
        if (brillo > 0) e.resplandecer(COLOR_DEL_BRILLO, brillo);
    }

    /**
     * La huella de sangre seca que queda donde cayó un cuerpo: una manchita
     * más oscura que la fresca, con una o dos gotas al lado. 'color' y 'radio'
     * como en esculpir(); 'variante' cambia un poco su forma.
     */
    public static void huella(Escultor e, int color, double radio, double oscurece, int variante) {
        int seco = castillafantasx.arte.Colores.mezcla(0xFF000000 | color, 0xFF0B090D, (float) oscurece) & 0xFFFFFF;
        Material[] de = materialesDe(seco);
        double angulo = variante * 2.1 + 0.4;
        e.mancha(new P3(0, 0, 0.05), new P3(radio, 0, 0), new P3(0, radio * ACHATADA, 0), de[0]);
        e.mancha(new P3(Math.cos(angulo) * radio * 1.3, Math.sin(angulo) * radio, 0.05), new P3(radio * 0.4, 0, 0), new P3(0, radio * 0.35, 0), de[0]);
        if (variante % 2 == 0) e.mancha(new P3(-Math.cos(angulo) * radio * 1.1, Math.sin(angulo + 1) * radio, 0.05), new P3(radio * 0.3, 0, 0), new P3(0, radio * 0.3, 0), de[0]);
        e.mancha(new P3(0, 0, 0.05), new P3(radio * PARTE_DEL_CENTRO, 0, 0), new P3(0, radio * PARTE_DEL_CENTRO * ACHATADA, 0), de[1]);
    }

    /** La mancha: un charco grande bajo el cuerpo y otros pequeños alrededor, que van saliendo mientras se extiende. */
    private static void mancha(Escultor e, P3 centro, Material[] de, double radio, double extension, long forma) {
        // Primero todos los bordes y luego todos los centros: así ningún borde aclara el centro de otro charco.
        for (int capa = 0; capa < 2; capa++) {
            for (int k = 0; k < CHARCOS; k++) {
                // Cada charco pequeño sale hacia un lado distinto, siempre el mismo para el mismo cuerpo.
                double angulo = forma * 1.9 + k * 2.3;
                // El grande no queda justo debajo del cuerpo, sino corrido a un lado: así asoma.
                double lejos = radio * (k == 0 ? CORRIDA : 0.75 + 0.15 * ((forma >> k) & 1));
                double grande = k == 0 ? 1 : 0.38 + 0.10 * ((forma >> (k + 3)) & 1);
                // Los pequeños empiezan a salir más tarde que el grande.
                double crece = Pose.suave((extension - k * 0.15) / (1 - k * 0.15));
                double r = radio * grande * crece * (capa == 1 ? PARTE_DEL_CENTRO : 1);
                if (r < RADIO_MINIMO) continue;
                P3 donde = centro.mas(Math.cos(angulo) * lejos, Math.sin(angulo) * lejos * ACHATADA, 0.05);
                e.mancha(donde, new P3(r, 0, 0), new P3(0, r * ACHATADA, 0), de[capa]);
            }
        }
    }
}
