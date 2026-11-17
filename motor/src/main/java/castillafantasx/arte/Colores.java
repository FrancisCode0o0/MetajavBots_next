package castillafantasx.arte;

/** Cuentas con colores 0xAARRGGBB: mezclar, oscurecer, apagar. */
public final class Colores {

    private Colores() { }

    public static int argb(int a, int r, int g, int b) {
        return (limite(a) << 24) | (limite(r) << 16) | (limite(g) << 8) | limite(b);
    }

    /** Un color opaco a partir de 0xRRGGBB. */
    public static int opaco(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static int limite(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** Multiplica cada canal por su factor (el alfa no cambia). */
    public static int mult(int c, float fr, float fg, float fb) {
        int r = (int) (((c >> 16) & 255) * fr);
        int g = (int) (((c >> 8) & 255) * fg);
        int b = (int) ((c & 255) * fb);
        return (c & 0xFF000000) | (limite(r) << 16) | (limite(g) << 8) | limite(b);
    }

    /** Más claro (f > 1) o más oscuro (f < 1). */
    public static int brillo(int c, float f) {
        return mult(c, f, f, f);
    }

    /** Mezcla dos colores: parte 0 = todo 'a', parte 1 = todo 'b'. El alfa es el de 'a'. */
    public static int mezcla(int a, int b, float parte) {
        float q = 1 - parte;
        int r = (int) (((a >> 16) & 255) * q + ((b >> 16) & 255) * parte);
        int g = (int) (((a >> 8) & 255) * q + ((b >> 8) & 255) * parte);
        int bl = (int) ((a & 255) * q + (b & 255) * parte);
        return (a & 0xFF000000) | (limite(r) << 16) | (limite(g) << 8) | limite(bl);
    }

    /** Quita color: cuanto = 0 lo deja igual, cuanto = 1 lo deja gris. */
    public static int apagar(int c, float cuanto) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        int gris = (r * 30 + g * 59 + b * 11) / 100;
        return mezcla(c, (c & 0xFF000000) | (gris << 16) | (gris << 8) | gris, cuanto);
    }

    /**
     * Un número «al azar» fijo para cada casilla (x, y): siempre el mismo para
     * la misma casilla, de 0 a 1. Sirve para variar dibujos sin que cambien de un frame a otro.
     */
    public static float ruido(int x, int y, int semilla) {
        int n = x * 374761393 + y * 668265263 + semilla * 1274126177;
        n = (n ^ (n >>> 13)) * 1274126177;
        n = n ^ (n >>> 16);
        return (n & 0xFFFF) / 65535f;
    }

    /** Ruido suave: mezcla el ruido de las cuatro esquinas de una rejilla de 'paso' píxeles. */
    public static float ruidoSuave(float x, float y, int paso, int semilla) {
        int x0 = (int) Math.floor(x / paso), y0 = (int) Math.floor(y / paso);
        float fx = x / paso - x0, fy = y / paso - y0;
        float a = ruido(x0, y0, semilla), b = ruido(x0 + 1, y0, semilla);
        float c = ruido(x0, y0 + 1, semilla), d = ruido(x0 + 1, y0 + 1, semilla);
        float arriba = a + (b - a) * fx, abajo = c + (d - c) * fx;
        return arriba + (abajo - arriba) * fy;
    }
}
