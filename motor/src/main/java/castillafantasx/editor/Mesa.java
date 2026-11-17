package castillafantasx.editor;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.Pantalla;
import castillafantasx.arte.Imagen;

/**
 * MESA — La mesa de trabajo de una herramienta del editor: un dibujo en
 * píxeles (una figura, un retrato) que se ve grande sobre el mundo, en el
 * hueco que deja libre el panel, y sobre el que se trabaja con el puntero.
 * La herramienta pinta su 'lienzo' y deja sus 'marcas' (tiradores, marcos,
 * rótulos); DibujoEditor lo lleva a la pantalla.
 */
public final class Mesa {

    /** Algo que se pinta encima del lienzo, en sus píxeles: un tirador, un marco o un rótulo. */
    public static final class Marca {
        public enum Tipo { PUNTO, MARCO, TEXTO }

        public final Tipo tipo;
        public final float x, y, ancho, alto;
        public final int color;
        public final String texto;

        Marca(Tipo tipo, float x, float y, float ancho, float alto, int color, String texto) {
            this.tipo = tipo;
            this.x = x;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
            this.color = color;
            this.texto = texto;
        }
    }

    /** Lo que se ve: la herramienta lo pinta entero cada vez. */
    public final Imagen lienzo;
    /** Cuántas veces se amplía en la pantalla y dónde cae su esquina (lo pone colocar()). */
    public int veces = 1;
    public float x, y;
    /** El aumento mayor que se le deja tomar. */
    public int vecesMaximas = 6;
    /** El color de su fondo (RRGGBB) y un renglón que se escribe encima de ella. */
    public int fondo = 0x2A2E2A;
    public String titulo = "";
    public final List<Marca> marcas = new ArrayList<>();

    public Mesa(int ancho, int alto) {
        lienzo = new Imagen(ancho, alto);
    }

    /** La coloca en el hueco libre más grande que deja el panel (a un lado o debajo), con el mayor aumento entero que quepa. */
    public void colocar(Panel panel) {
        float arriba = Editor.ALTO_DE_LA_BARRA + Estilo.RENGLON + 4, abajo = Pantalla.alto - Estilo.RENGLON - 6;
        float px0 = panel.x, px1 = panel.x + panel.ancho, py1 = panel.y + panel.altoAhora();
        // Los huecos: a la izquierda del panel, a su derecha y debajo de él.
        float[][] huecos = {
            { Estilo.MARGEN, arriba, px0 - Estilo.MARGEN, abajo },
            { px1 + Estilo.MARGEN, arriba, Pantalla.ancho - Estilo.MARGEN, abajo },
            { Estilo.MARGEN, Math.max(arriba, py1 + Estilo.RENGLON + 4), Pantalla.ancho - Estilo.MARGEN, abajo },
        };
        float[] mejor = huecos[1];
        int mejorVeces = 0;
        for (float[] h : huecos) {
            int caben = (int) Math.min((h[2] - h[0]) / lienzo.ancho, (h[3] - h[1]) / lienzo.alto);
            if (caben > mejorVeces) {
                mejorVeces = caben;
                mejor = h;
            }
        }
        veces = Math.max(1, Math.min(vecesMaximas, mejorVeces));
        x = Math.round(mejor[0] + Math.max(0, (mejor[2] - mejor[0] - lienzo.ancho * veces) / 2));
        y = Math.round(mejor[1] + Math.max(0, (mejor[3] - mejor[1] - lienzo.alto * veces) / 2));
    }

    /** ¿Cae ese punto de la imagen del juego dentro de la mesa? */
    public boolean dentro(float px, float py) {
        return px >= x && py >= y && px < x + lienzo.ancho * veces && py < y + lienzo.alto * veces;
    }

    /** Ese punto de la imagen del juego, en píxeles del lienzo. */
    public float[] aLienzo(float px, float py) {
        return new float[] { ox + (px - x) / escala(), oy + (py - y) / escala() };
    }

    /** Un punto del lienzo, en la imagen del juego (para las pruebas y para pinchar justo ahí). */
    public float[] aPantalla(float lx, float ly) {
        return new float[] { x + (lx - ox) * escala(), y + (ly - oy) * escala() };
    }

    // ------------------------------------------------------------------
    // Acercar y mover la vista de la mesa (encargo ORDEN_ERGONOMIA_2), como en los programas de dibujo:
    // Ctrl + la rueda (o Ctrl + y Ctrl −, o sus botones), y arrastrar con el botón central (o con la mano).
    // ------------------------------------------------------------------

    /** Las veces que se acerca, sobre lo que ya amplía (1 = la mesa entera a la vista). */
    public int lupa = 1;
    public static final int LUPA_MAXIMA = 4;
    /** El píxel del lienzo que queda en la esquina de arriba a la izquierda (con la lupa en 1, el 0, 0). */
    public float ox, oy;
    /** ¿Arrastrar con el botón izquierdo mueve la vista, en vez de trabajar? (La mano, para el dedo y el lápiz.) */
    public boolean conLaMano;

    /** Los píxeles de pantalla que mide un píxel del lienzo. */
    public int escala() {
        return veces * lupa;
    }

    /** Acerca (paso > 0) o aleja (paso < 0) un punto, dejando quieto lo que hay bajo ese punto de la pantalla. */
    public void acercar(int paso, float px, float py) {
        int nueva = Math.max(1, Math.min(LUPA_MAXIMA, lupa + paso));
        if (nueva == lupa) return;
        float[] fijo = aLienzo(px, py);
        lupa = nueva;
        ox = fijo[0] - (px - x) / escala();
        oy = fijo[1] - (py - y) / escala();
        acotar();
    }

    /** Mueve la vista esos píxeles de pantalla (lo de debajo va con el puntero). */
    public void desplazar(float dx, float dy) {
        ox -= dx / escala();
        oy -= dy / escala();
        acotar();
    }

    /** Vuelve a la mesa entera a la vista. */
    public void encajar() {
        lupa = 1;
        ox = oy = 0;
        conLaMano = false;
    }

    /** Lo que se ve no se sale del lienzo. */
    private void acotar() {
        ox = Math.max(0, Math.min(lienzo.ancho - lienzo.ancho / (float) lupa, ox));
        oy = Math.max(0, Math.min(lienzo.alto - lienzo.alto / (float) lupa, oy));
        if (lupa == 1) conLaMano = false;
    }

    /** Sus botones, arriba a la derecha y por dentro: acercar, alejar, la mesa entera y la mano (cuando está acercada). */
    public static final String[] BOTONES = { "+", "-", "[]", "mano" };
    public static final String[] AYUDA_DE_LOS_BOTONES = { "Acercar (Ctrl + la rueda, o Ctrl +)", "Alejar (Ctrl + la rueda, o Ctrl −)",
        "La mesa entera a la vista (Ctrl+0)", "La mano: arrastrar mueve la vista (o el botón central, o Ctrl + flechas)" };

    /** Cuántos botones se ven ahora (la mano, solo de cerca). */
    public int botones() {
        return lupa > 1 ? BOTONES.length : BOTONES.length - 1;
    }

    /** La x del botón 'cual' (sus zonas son de toque entero: Estilo.TOQUE_MINIMO). */
    public float xDelBoton(int cual) {
        return x + lienzo.ancho * veces - (cual + 1) * (Estilo.TOQUE_MINIMO + 1);
    }

    /** El botón que hay en ese punto, o -1. */
    public int botonEn(float px, float py) {
        if (py < y || py >= y + Estilo.TOQUE_MINIMO) return -1;
        for (int i = 0; i < botones(); i++) {
            float bx = xDelBoton(i);
            if (px >= bx && px < bx + Estilo.TOQUE_MINIMO) return i;
        }
        return -1;
    }

    /** Pulsa el botón 'cual' (sobre el centro de la mesa, para acercar y alejar). */
    public void pulsar(int cual) {
        float cx = x + lienzo.ancho * veces / 2f, cy = y + lienzo.alto * veces / 2f;
        if (cual == 0) acercar(1, cx, cy);
        else if (cual == 1) acercar(-1, cx, cy);
        else if (cual == 2) encajar();
        else if (cual == 3) conLaMano = !conLaMano;
    }

    // ------------------------------------------------------------------
    // Los colores de hace poco (como la paleta de un programa de dibujo): abajo a la izquierda, por dentro
    // ------------------------------------------------------------------

    /** Los colores (RRGGBB) que enseña abajo para elegirlos de un toque; la herramienta los pone. */
    public final List<Integer> muestras = new ArrayList<>();
    /** La muestra que es el color de ahora (con su marco de oro), o -1. */
    public int muestraElegida = -1;

    /** La x de la muestra 'cual'. */
    public float xDeLaMuestra(int cual) {
        return x + 2 + cual * (Estilo.TOQUE_MINIMO + 2);
    }

    /** La y de las muestras. */
    public float yDeLasMuestras() {
        return y + lienzo.alto * veces - Estilo.TOQUE_MINIMO - 2;
    }

    /** La muestra que hay en ese punto, o -1. */
    public int muestraEn(float px, float py) {
        float my = yDeLasMuestras();
        if (py < my || py >= my + Estilo.TOQUE_MINIMO) return -1;
        for (int i = 0; i < muestras.size(); i++) {
            float mx = xDeLaMuestra(i);
            if (px >= mx && px < mx + Estilo.TOQUE_MINIMO) return i;
        }
        return -1;
    }

    /** Un tirador: un cuadradito en ese píxel del lienzo, con su rótulo al lado (o null). */
    public void punto(float lx, float ly, int color, String rotulo) {
        marcas.add(new Marca(Marca.Tipo.PUNTO, lx, ly, 0, 0, color, rotulo));
    }

    /** El borde de un rectángulo del lienzo. */
    public void marco(float lx, float ly, float ancho, float alto, int color) {
        marcas.add(new Marca(Marca.Tipo.MARCO, lx, ly, ancho, alto, color, null));
    }

    /** Un rótulo que empieza en ese píxel del lienzo. */
    public void texto(String texto, float lx, float ly, int color) {
        marcas.add(new Marca(Marca.Tipo.TEXTO, lx, ly, 0, 0, color, texto));
    }
}
