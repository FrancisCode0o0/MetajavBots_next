package castillafantasx.arte;

/**
 * IMAGEN — Un dibujo en memoria: ancho, alto y un color por píxel
 * (0xAARRGGBB). No necesita tarjeta gráfica: la usan el Taller para fabricar
 * dibujos, el trazador de la mazmorra para leer texturas y las pruebas.
 */
public final class Imagen {

    public final int ancho, alto;
    public final int[] px;
    /** Sus versiones reducidas (la mitad, el cuarto…): las hace una vez y las guarda aquí el trazador de la mazmorra. */
    public int[][] reducidas;

    public Imagen(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.px = new int[ancho * alto];
    }

    /** El color de un píxel; fuera del dibujo, transparente. */
    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= ancho || y >= alto) return 0;
        return px[y * ancho + x];
    }

    /** Pone un píxel; fuera del dibujo no hace nada. */
    public void set(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= ancho || y >= alto) return;
        px[y * ancho + x] = argb;
    }

    /** Rellena un rectángulo. */
    public void rect(int x, int y, int w, int h, int argb) {
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                set(i, j, argb);
            }
        }
    }

    /** Pinta otra imagen encima, en (dx, dy). Los píxeles transparentes no tapan. */
    public void pintar(Imagen otra, int dx, int dy) {
        for (int y = 0; y < otra.alto; y++) {
            for (int x = 0; x < otra.ancho; x++) {
                int c = otra.px[y * otra.ancho + x];
                if ((c >>> 24) >= 128) set(dx + x, dy + y, c | 0xFF000000);
            }
        }
    }

    /** Pinta otra imagen encima mezclando lo que se transparenta (los velos de las estelas). */
    public void mezclar(Imagen otra, int dx, int dy) {
        for (int y = 0; y < otra.alto; y++) {
            for (int x = 0; x < otra.ancho; x++) {
                int c = otra.px[y * otra.ancho + x];
                int alfa = c >>> 24;
                if (alfa == 0) continue;
                int debajo = get(dx + x, dy + y);
                set(dx + x, dy + y, alfa == 255 || (debajo >>> 24) == 0 ? c : Colores.mezcla(debajo, c | 0xFF000000, alfa / 255f));
            }
        }
    }

    public Imagen copia() {
        Imagen c = new Imagen(ancho, alto);
        System.arraycopy(px, 0, c.px, 0, px.length);
        return c;
    }

    /** La misma imagen mirando al otro lado. */
    public Imagen espejo() {
        Imagen e = new Imagen(ancho, alto);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                e.px[y * ancho + x] = px[y * ancho + (ancho - 1 - x)];
            }
        }
        return e;
    }

    /** La imagen ampliada un número entero de veces (cada píxel, un cuadrado). */
    public Imagen ampliada(int veces) {
        Imagen g = new Imagen(ancho * veces, alto * veces);
        for (int y = 0; y < g.alto; y++) {
            for (int x = 0; x < g.ancho; x++) {
                g.px[y * g.ancho + x] = px[(y / veces) * ancho + (x / veces)];
            }
        }
        return g;
    }

    /** Un trozo de la imagen. */
    public Imagen recorte(int x0, int y0, int w, int h) {
        Imagen r = new Imagen(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                r.px[y * w + x] = get(x0 + x, y0 + y);
            }
        }
        return r;
    }

    /** Multiplica el color de cada píxel por (r, g, b), de 0 a 1 o más. */
    public Imagen tenida(float r, float g, float b) {
        Imagen t = new Imagen(ancho, alto);
        for (int i = 0; i < px.length; i++) {
            t.px[i] = Colores.mult(px[i], r, g, b);
        }
        return t;
    }

    /** Un dibujo escrito con letras de la Paleta ('.' = transparente), una fila por texto. */
    public static Imagen deMatriz(String[] filas) {
        Imagen im = new Imagen(filas[0].length(), filas.length);
        for (int y = 0; y < filas.length; y++) {
            for (int x = 0; x < filas[y].length(); x++) {
                char letra = filas[y].charAt(x);
                if (letra != '.') im.px[y * im.ancho + x] = Paleta.argb(letra);
            }
        }
        return im;
    }

    /**
     * Un dibujo de letras con un parche encima. Cada cambio es "fila columna
     * letras": «en esa fila, desde esa columna, pon estas letras».
     */
    public static String[] conParche(String[] patron, String[] parche) {
        char[][] m = new char[patron.length][];
        for (int fila = 0; fila < patron.length; fila++) {
            m[fila] = patron[fila].toCharArray();
        }
        for (String cambio : parche) {
            String[] partes = cambio.split(" ");
            int fila = Integer.parseInt(partes[0]);
            int col = Integer.parseInt(partes[1]);
            for (int i = 0; i < partes[2].length(); i++) {
                m[fila][col + i] = partes[2].charAt(i);
            }
        }
        String[] copia = new String[patron.length];
        for (int fila = 0; fila < patron.length; fila++) {
            copia[fila] = new String(m[fila]);
        }
        return copia;
    }
}
