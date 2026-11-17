package castillafantasx.arte;

import java.util.Map;

/**
 * VETA — La textura de la madera labrada (docs/encargos_viejos/ENCARGO_BOSQUE.md): vetas a lo
 * largo de cada tabla o viga, nudos, desgaste, humedad en lo bajo y algún
 * clavo de hierro. Se pasa una vez, al cargar, por los dibujos de madera que
 * ya hay (cercas, tablas, carreta, puertas, cofres, toneles, vigas y
 * entramados de los edificios…), sin cambiarles la forma ni el contorno.
 */
public final class Veta {

    private Veta() { }

    // --- Cifras de dibujo, PROVISIONALES ---
    private static final float FUERZA_DE_LA_VETA = 0.36f;    // cuánto aclara y oscurece la veta
    private static final int NUDO_CADA_X = 15, NUDO_CADA_Y = 11;     // la rejilla donde puede caer un nudo o un clavo (píxeles)
    private static final float NUDOS = 0.30f, CLAVOS = 0.16f;        // en qué parte de esos sitios hay un nudo, y un clavo
    private static final float HUMEDAD_DESDE = 0.70f;        // desde qué parte del alto del dibujo oscurece la humedad
    private static final int VERDIN = 0xFF3A4A2A, HIERRO = 0xFF26242A, BRILLO_DEL_CLAVO = 0xFF8A8A90;

    // Los dibujos de madera que fabrican los talleres o vienen de res/arte (los que empiecen así).
    private static final String[] DE_MADERA = { "decor/cartel", "decor/carreta", "decor/cerca_", "decor/tablas_", "decor/comedero",
        "decor/candil", "decor/tronco", "decor/barril", "decor/cofre", "decor/puerta_", "casa/puerta", "establo/frente_", "establo/alero_" };

    /** Pasa la veta por todos los dibujos de madera del almacén y por la madera de los edificios. */
    public static void labrarTodo(Map<String, Imagen> imagenes, int[] maderaDeEdificios) {
        for (Map.Entry<String, Imagen> e : imagenes.entrySet()) {
            String nombre = e.getKey();
            if (nombre.startsWith("edificio/") && !nombre.equals("edificio/luz_de_ventana")) {
                enRampa(e.getValue(), maderaDeEdificios, nombre.hashCode());
                continue;
            }
            for (String prefijo : DE_MADERA) {
                if (nombre.startsWith(prefijo)) {
                    labrar(e.getValue(), nombre.hashCode());
                    break;
                }
            }
        }
    }

    /** ¿Es de color de madera? Pardos: más rojo que verde y más verde que azul, ni muy oscuros ni muy claros. */
    private static boolean esMadera(int c) {
        if ((c >>> 24) < 200) return false;
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return r > g && g >= b && r - b >= 18 && r - b <= 120 && r >= 52 && r <= 200;
    }

    /** Un dibujo suelto: todo lo que tenga de color de madera recibe su veta. */
    static void labrar(Imagen im, int semilla) {
        boolean[][] madera = new boolean[im.alto][im.ancho];
        for (int y = 0; y < im.alto; y++) {
            for (int x = 0; x < im.ancho; x++) madera[y][x] = esMadera(im.get(x, y));
        }
        for (int y = 0; y < im.alto; y++) {
            for (int x = 0; x < im.ancho; x++) {
                if (!madera[y][x]) continue;
                boolean tumbada = aLoAncho(madera, x, y);
                int c = im.get(x, y);
                float f = factor(x, y, tumbada, semilla);
                // La humedad: lo bajo del dibujo, más oscuro y con algo de verdín.
                float bajo = (y / (float) im.alto - HUMEDAD_DESDE) / (1 - HUMEDAD_DESDE);
                if (bajo > 0) {
                    float moja = bajo * Colores.ruidoSuave(x, y, 3, semilla + 3);
                    f *= 1 - 0.30f * moja;
                    if (moja > 0.55f) c = Colores.mezcla(c, VERDIN, 0.22f);
                }
                c = Colores.brillo(c, f);
                int clavo = clavo(x, y, semilla);
                if (clavo != 0 && dentro(madera, x, y)) c = clavo;
                im.set(x, y, c);
            }
        }
    }

    /** La madera de un edificio: sus píxeles son los de una rampa de tonos; la veta los sube o los baja de tono. */
    static void enRampa(Imagen im, int[] rampa, int semilla) {
        boolean[][] madera = new boolean[im.alto][im.ancho];
        int[][] tono = new int[im.alto][im.ancho];
        for (int y = 0; y < im.alto; y++) {
            for (int x = 0; x < im.ancho; x++) {
                int c = im.get(x, y) & 0xFFFFFF;
                // El tono más oscuro de la rampa es el de los contornos: se deja.
                for (int k = 1; k < rampa.length; k++) {
                    if ((im.get(x, y) >>> 24) == 255 && c == (rampa[k] & 0xFFFFFF)) {
                        madera[y][x] = true;
                        tono[y][x] = k;
                    }
                }
            }
        }
        for (int y = 0; y < im.alto; y++) {
            for (int x = 0; x < im.ancho; x++) {
                if (!madera[y][x]) continue;
                float f = factor(x, y, aLoAncho(madera, x, y), semilla);
                int k = Math.max(1, Math.min(rampa.length - 1, tono[y][x] + Math.round((f - 1) * 4.2f)));
                int clavo = clavo(x, y, semilla);
                im.set(x, y, clavo != 0 && dentro(madera, x, y) ? clavo : 0xFF000000 | rampa[k]);
            }
        }
    }

    /** ¿Corre la madera a lo ancho en ese punto? Se mira hacia dónde sigue habiendo madera más lejos. */
    private static boolean aLoAncho(boolean[][] madera, int x, int y) {
        int ancho = 0, alto = 0;
        for (int d = 1; d <= 6; d++) {
            if (x - d >= 0 && madera[y][x - d]) ancho++;
            if (x + d < madera[0].length && madera[y][x + d]) ancho++;
            if (y - d >= 0 && madera[y - d][x]) alto++;
            if (y + d < madera.length && madera[y + d][x]) alto++;
        }
        return ancho > alto;
    }

    /** ¿Está rodeado de madera? (Los clavos no van en el canto.) */
    private static boolean dentro(boolean[][] madera, int x, int y) {
        return x > 0 && y > 0 && x < madera[0].length - 1 && y < madera.length - 1
                && madera[y][x - 1] && madera[y][x + 1] && madera[y - 1][x] && madera[y + 1][x];
    }

    /** Por cuánto se multiplica la luz de la madera en un punto: la veta, los nudos y el desgaste. */
    private static float factor(int x, int y, boolean tumbada, int semilla) {
        // A lo largo de la tabla la veta cambia despacio; de través, deprisa.
        float largo = tumbada ? x : y, traves = tumbada ? y : x;
        float veta = Colores.ruidoSuave(largo * 0.30f + (float) Math.sin(traves * 0.9f) * 0.8f, traves * 2.4f, 2, semilla);
        float f = 1 + (veta - 0.5f) * FUERZA_DE_LA_VETA + (Colores.ruido(x, y, semilla + 1) - 0.5f) * 0.08f;
        // Los nudos: un cerco oscuro con el corazón más oscuro todavía; la veta lo rodea.
        int cx = Math.floorDiv(x, NUDO_CADA_X), cy = Math.floorDiv(y, NUDO_CADA_Y);
        if (Colores.ruido(cx, cy, semilla + 2) < NUDOS) {
            float nx = (cx + 0.2f + Colores.ruido(cx, cy, semilla + 4) * 0.6f) * NUDO_CADA_X, ny = (cy + 0.2f + Colores.ruido(cx, cy, semilla + 5) * 0.6f) * NUDO_CADA_Y;
            float dx = x - nx, dy = y - ny, d = dx * dx + dy * dy;
            if (d < 1.2f) f *= 0.58f;
            else if (d < 5.5f) f *= 0.80f;
        }
        // El desgaste: alguna astilla clara donde la madera está rozada.
        if (Colores.ruido(x, y, semilla + 6) > 0.972f) f *= 1.22f;
        return f;
    }

    /** El color del clavo que cae en ese punto (su cabeza de hierro o su brillo), o 0 si no hay. */
    private static int clavo(int x, int y, int semilla) {
        int cx = Math.floorDiv(x, NUDO_CADA_X), cy = Math.floorDiv(y, NUDO_CADA_Y);
        if (Colores.ruido(cx, cy, semilla + 8) > CLAVOS) return 0;
        int nx = cx * NUDO_CADA_X + 2 + (int) (Colores.ruido(cx, cy, semilla + 9) * (NUDO_CADA_X - 5));
        int ny = cy * NUDO_CADA_Y + 2 + (int) (Colores.ruido(cx, cy, semilla + 10) * (NUDO_CADA_Y - 5));
        if (x == nx && y == ny) return HIERRO;
        return x == nx - 1 && y == ny - 1 ? BRILLO_DEL_CLAVO : 0;
    }
}
