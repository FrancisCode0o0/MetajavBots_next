package castillafantasx.interfaz;

import java.util.ArrayList;
import java.util.List;

/**
 * LISTA QUIETA — Lo que se ve de una lista larga (encargo ORDEN_ERGONOMIA,
 * docs/ORDEN_ERGONOMIA.md). Solo se mueve cuando uno quiere: con el teclado
 * (lo justo para que se vea la elegida), con la rueda del ratón o con su
 * barra, que se pincha y se arrastra (con el ratón, el dedo o el lápiz).
 * Señalar una fila con el puntero nunca la mueve: antes, al pasar el ratón
 * por encima, la lista se iba sola arriba y abajo.
 */
public final class ListaQuieta {

    // TODO-FRAN: lo ancha que se ve la barra, lo ancha que se puede tocar y lo corto que puede quedar su tirador.
    public static final int ANCHO_DE_LA_BARRA = 5, ANCHO_PARA_TOCAR = 14, TIRADOR_MINIMO = 20;
    private static final int CARRIL = 0x2A2522, TIRADOR = 0x9C8F80, TIRADOR_COGIDO = 0xE8B45A;

    /** La primera fila que se ve. */
    public int primera;
    private int elegidaAntes = Integer.MIN_VALUE, total, visibles = 1;
    // Dónde se pintó la barra la última vez (x de su borde izquierdo, y de arriba y alto).
    private float xBarra, yBarra, altoBarra;
    private float yAlPinchar;
    private int primeraAlPinchar;

    /** Las barras pintadas en el último dibujo (para el puntero) y la que se está arrastrando. */
    private static final List<ListaQuieta> PINTADAS = new ArrayList<>();
    private static ListaQuieta cogida;

    /**
     * La primera fila que se ve. Si la elegida ha cambiado con el teclado (o
     * la rueda) y se ha salido de la vista, la lista se mueve lo justo para
     * enseñarla. Si la ha puesto el puntero ('porElPuntero'), no se mueve.
     */
    public int primera(int elegida, int total, int visibles) {
        return primera(elegida, total, visibles, false);
    }

    public int primera(int elegida, int total, int visibles, boolean porElPuntero) {
        this.total = total;
        this.visibles = Math.max(1, visibles);
        if (elegida != elegidaAntes && !porElPuntero && cogida != this && elegida >= 0) {
            if (elegida < primera) primera = elegida;
            else if (elegida >= primera + this.visibles) primera = elegida - this.visibles + 1;
        }
        elegidaAntes = elegida;
        primera = Math.max(0, Math.min(primera, total - this.visibles));
        return primera;
    }

    /** ¿Hay más filas de las que caben? */
    public boolean hayMas() {
        return total > visibles;
    }

    /** Mueve la vista esas filas (positivas: hacia abajo), sin cambiar la elegida. */
    public void desplazar(int filas) {
        primera = Math.max(0, Math.min(primera + filas, total - visibles));
    }

    /** La lista vuelve arriba del todo (al abrir otro menú, por ejemplo). */
    public void alPrincipio() {
        primera = 0;
        elegidaAntes = Integer.MIN_VALUE;
    }

    /**
     * Pinta la barra pegada por dentro al borde derecho 'xDerecho' de la
     * lista, de 'y' a 'y + alto', y la apunta para el puntero. Si caben todas
     * las filas, no pinta nada.
     */
    public void dibujar(Lapiz lapiz, float xDerecho, float y, float alto) {
        if (!hayMas() || alto <= 0) return;
        xBarra = xDerecho - ANCHO_DE_LA_BARRA - 2;
        yBarra = y;
        altoBarra = alto;
        float[] t = tirador();
        lapiz.rect(xBarra, y, ANCHO_DE_LA_BARRA, alto, CARRIL, 0.9f);
        lapiz.rect(xBarra, t[0], ANCHO_DE_LA_BARRA, t[1], cogida == this ? TIRADOR_COGIDO : TIRADOR, 1);
        PINTADAS.add(this);
    }

    private final float[] medidasDelTirador = new float[2];

    /** El tirador: {y, alto} (el mismo arreglo cada vez: no crea nada en cada fotograma). */
    private float[] tirador() {
        float alto = Math.max(TIRADOR_MINIMO, altoBarra * visibles / Math.max(1, total));
        alto = Math.min(alto, altoBarra);
        int recorrido = Math.max(1, total - visibles);
        medidasDelTirador[0] = yBarra + (altoBarra - alto) * primera / recorrido;
        medidasDelTirador[1] = alto;
        return medidasDelTirador;
    }

    /** ¿Está ese punto sobre la barra (con su margen para el dedo)? */
    private boolean dentro(float x, float y) {
        float centro = xBarra + ANCHO_DE_LA_BARRA / 2f;
        return Math.abs(x - centro) <= ANCHO_PARA_TOCAR / 2f && y >= yBarra && y < yBarra + altoBarra;
    }

    // ------------------------------------------------------------------
    // El puntero (todas las barras a la vez: solo hay una bajo el puntero)
    // ------------------------------------------------------------------

    /** Olvida las barras del dibujo anterior (al empezar a pintar). La que se arrastra sigue cogida. */
    public static void olvidarPintadas() {
        PINTADAS.clear();
    }

    /** ¿Hay una barra en ese punto? */
    public static boolean hayBarraEn(float x, float y) {
        for (ListaQuieta l : PINTADAS) {
            if (l.dentro(x, y)) return true;
        }
        return false;
    }

    /**
     * Se pincha en ese punto. Si es una barra, devuelve true (el clic no hace
     * nada más): sobre el tirador, se coge para arrastrarlo; sobre el carril,
     * la lista pasa una página hacia ese lado.
     */
    public static boolean pinchar(float x, float y) {
        for (ListaQuieta l : PINTADAS) {
            if (!l.dentro(x, y)) continue;
            float[] t = l.tirador();
            if (y < t[0] || y >= t[0] + t[1]) {
                l.desplazar(y < t[0] ? -l.visibles : l.visibles);
                return true;
            }
            cogida = l;
            l.yAlPinchar = y;
            l.primeraAlPinchar = l.primera;
            return true;
        }
        return false;
    }

    /** El puntero se mueve con la barra cogida: la lista le sigue. */
    public static void arrastrar(float y) {
        if (cogida == null) return;
        ListaQuieta l = cogida;
        float alto = l.tirador()[1];
        float porFila = (l.altoBarra - alto) / Math.max(1, l.total - l.visibles);
        if (porFila <= 0) return;
        l.primera = Math.max(0, Math.min(l.primeraAlPinchar + Math.round((y - l.yAlPinchar) / porFila), l.total - l.visibles));
    }

    /** Se suelta el botón (o el dedo). */
    public static void soltar() {
        cogida = null;
    }

    /** ¿Se está arrastrando una barra? */
    public static boolean arrastrandoAlguna() {
        return cogida != null;
    }

    /** El centro del tirador de la primera barra pintada en el último dibujo, o null si no hay (para las pruebas y las fotos). */
    public static float[] centroDelTirador() {
        if (PINTADAS.isEmpty()) return null;
        ListaQuieta l = PINTADAS.get(0);
        float[] t = l.tirador();
        return new float[] { l.xBarra + ANCHO_DE_LA_BARRA / 2f, t[0] + t[1] / 2 };
    }

    /** Lo que mide de alto el carril de la primera barra pintada (para las pruebas), o 0. */
    public static float altoDelCarril() {
        return PINTADAS.isEmpty() ? 0 : PINTADAS.get(0).altoBarra;
    }
}
