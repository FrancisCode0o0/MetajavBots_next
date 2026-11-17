package castillafantasx.editor;

import castillafantasx.Pantalla;

/**
 * PANEL — Un panel flotante del editor: se mueve arrastrando su título, se
 * pliega (queda solo el título) y se agranda. Aquí solo está dónde está y
 * cómo está; lo que lleva dentro lo pone cada herramienta.
 */
public final class Panel {

    public static final int ALTO_DEL_TITULO = 15, BOTON = 14;

    /** El nombre con el que se recuerda su sitio entre sesiones. */
    public final String id;
    public float x, y, ancho, alto;
    private final float anchoNormal, altoNormal;
    private float xNormal, yNormal;         // dónde estaba antes de agrandarlo
    public boolean plegado, grande;
    private boolean arrastrando;
    private float agarreX, agarreY;

    public Panel(String id, float x, float y, float ancho, float alto) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.ancho = anchoNormal = ancho;
        this.alto = altoNormal = alto;
    }

    /** El alto que ocupa ahora (plegado, solo el título). */
    public float altoAhora() {
        return plegado ? ALTO_DEL_TITULO : alto;
    }

    public boolean dentro(float px, float py) {
        return px >= x && px < x + ancho && py >= y && py < y + altoAhora();
    }

    public boolean enElTitulo(float px, float py) {
        return px >= x && px < x + ancho && py >= y && py < y + ALTO_DEL_TITULO;
    }

    /** El botón número 'cual' del título, contando desde la derecha: 0 plegar, 1 agrandar. */
    public boolean enElBoton(int cual, float px, float py) {
        float bx = x + ancho - (cual + 1) * BOTON - 1;
        return px >= bx && px < bx + BOTON && py >= y && py < y + ALTO_DEL_TITULO;
    }

    /** Se pincha en ese punto. Devuelve true si el panel se lo queda (un botón o el título). */
    public boolean pinchar(float px, float py) {
        if (enElBoton(0, px, py)) {
            plegado = !plegado;
            return true;
        }
        if (enElBoton(1, px, py)) {
            agrandar(!grande);
            return true;
        }
        if (!enElTitulo(px, py)) return false;
        arrastrando = true;
        agarreX = px - x;
        agarreY = py - y;
        return true;
    }

    /** El puntero se mueve: si se lleva agarrado, el panel va con él, sin salirse de la pantalla. */
    public void arrastrar(float px, float py) {
        if (!arrastrando) return;
        x = px - agarreX;
        y = py - agarreY;
        encajar();
    }

    /** Se suelta. Devuelve true si se estaba moviendo. */
    public boolean soltar() {
        boolean movido = arrastrando;
        arrastrando = false;
        return movido;
    }

    /** Grande, ocupa casi toda la pantalla; normal, su tamaño de siempre. */
    public void agrandar(boolean si) {
        grande = si;
        plegado = false;
        ancho = si ? Pantalla.ancho - 2 * Estilo.MARGEN : anchoNormal;
        alto = si ? Pantalla.alto - 2 * Estilo.MARGEN - 24 : altoNormal;
        if (si) {
            xNormal = x;
            yNormal = y;
            x = Estilo.MARGEN;
            y = 24;
        } else {
            x = xNormal;
            y = yNormal;
        }
        encajar();
    }

    /** Lo deja dentro de la pantalla (también si la ventana ha cambiado de tamaño). */
    public void encajar() {
        // Grande, sigue a la ventana: si esta cambia de tamaño, el panel vuelve a llenarla.
        if (grande) {
            ancho = Pantalla.ancho - 2 * Estilo.MARGEN;
            alto = Pantalla.alto - 2 * Estilo.MARGEN - 24;
        }
        ancho = Math.min(ancho, Pantalla.ancho - 2);
        alto = Math.min(alto, Pantalla.alto - 2);
        x = Math.max(1, Math.min(Pantalla.ancho - ancho - 1, x));
        y = Math.max(1, Math.min(Pantalla.alto - ALTO_DEL_TITULO - 1, y));
    }

    /** Cómo se guarda: "x y plegado grande". */
    public String nota() {
        return Math.round(x) + " " + Math.round(y) + " " + (plegado ? 1 : 0) + " " + (grande ? 1 : 0);
    }

    /** Vuelve a como se guardó. Si la nota no vale, se queda como está. */
    public void deNota(String nota) {
        if (nota == null) return;
        String[] p = nota.trim().split(" +");
        if (p.length < 4) return;
        try {
            float nx = Float.parseFloat(p[0]), ny = Float.parseFloat(p[1]);
            if (p[3].equals("1")) agrandar(true);
            x = nx;
            y = ny;
            plegado = p[2].equals("1");
            encajar();
        } catch (NumberFormatException e) {
            // Una nota estropeada: el panel sale en su sitio de fábrica.
        }
    }
}
