package castillafantasx.editor;

/**
 * MARCA DE EDITOR — Algo que una herramienta pinta sobre el mundo mientras
 * está puesta: el marco de lo elegido, el fantasma de lo que se va a colocar,
 * el borde de una zona de un evento, un rótulo. Sus medidas van en píxeles
 * del mapa; DibujoEditor las lleva a la pantalla.
 */
public final class MarcaDeEditor {

    public enum Tipo { MARCO, RELLENO, DIBUJO, TEXTO }

    public final Tipo tipo;
    /** MARCO y RELLENO: de (x0, y0) a (x1, y1). DIBUJO: centrado (o apoyado, si 'dePie') en (x0, y0). TEXTO: empieza en (x0, y0). */
    public float x0, y0, x1, y1;
    public int color;
    public float alfa = 1;
    /** El nombre del dibujo, o lo que dice el texto. */
    public String que;
    public float tam = 1;
    public int giro;
    public boolean espejo, dePie;

    private MarcaDeEditor(Tipo tipo) {
        this.tipo = tipo;
    }

    /** El borde de un rectángulo. */
    public static MarcaDeEditor marco(float x0, float y0, float x1, float y1, int color) {
        MarcaDeEditor m = new MarcaDeEditor(Tipo.MARCO);
        m.x0 = x0;
        m.y0 = y0;
        m.x1 = x1;
        m.y1 = y1;
        m.color = color;
        return m;
    }

    /** Un rectángulo lleno, medio transparente. */
    public static MarcaDeEditor relleno(float x0, float y0, float x1, float y1, int color, float alfa) {
        MarcaDeEditor m = new MarcaDeEditor(Tipo.RELLENO);
        m.x0 = x0;
        m.y0 = y0;
        m.x1 = x1;
        m.y1 = y1;
        m.color = color;
        m.alfa = alfa;
        return m;
    }

    /** Un dibujo del juego, medio transparente: centrado en (x, y) o, si 'dePie', apoyado en ese punto. */
    public static MarcaDeEditor dibujo(String nombre, float x, float y, float tam, int giro, boolean espejo, boolean dePie, float alfa) {
        MarcaDeEditor m = new MarcaDeEditor(Tipo.DIBUJO);
        m.que = nombre;
        m.x0 = x;
        m.y0 = y;
        m.tam = tam;
        m.giro = giro;
        m.espejo = espejo;
        m.dePie = dePie;
        m.alfa = alfa;
        return m;
    }

    /** Un rótulo. */
    public static MarcaDeEditor texto(String texto, float x, float y, int color) {
        MarcaDeEditor m = new MarcaDeEditor(Tipo.TEXTO);
        m.que = texto;
        m.x0 = x;
        m.y0 = y;
        m.color = color;
        return m;
    }
}
