package castillafantasx.editor;

import castillafantasx.Pantalla;

/**
 * HERRAMIENTA DE MESA — Lo común de las herramientas que trabajan sobre una
 * mesa (Mesa): esculpir figuras, poses, retratos y seres nuevos. Deja sitio
 * a la mesa llevando el panel a un lado y da las filas «Nombre: < valor >».
 */
public abstract class HerramientaDeMesa extends HerramientaDeEditor {

    protected HerramientaDeMesa(String id, String nombre, Categoria categoria, String icono, String ayuda, String palabras) {
        super(id, nombre, categoria, icono, ayuda, palabras);
    }

    /** Al elegirla, si el panel está en su sitio de siempre (arriba, en el centro), se va a la izquierda: la mesa queda a su derecha. */
    @Override
    public boolean alElegir(Editor editor) {
        Panel panel = editor.panel;
        boolean sinMover = (panel.x == 0 && panel.y == 0) || Math.abs(panel.x - Math.round((Pantalla.ancho - panel.ancho) / 2)) <= 1;
        if (sinMover) {
            panel.x = Estilo.MARGEN;
            panel.y = Editor.ALTO_DE_LA_BARRA + 4;
        }
        return true;
    }

    /** Una fila con valor: «Nombre: < valor >». ← y → (paso -1 y 1) lo cambian; Intro (paso 0) cuenta como →. */
    protected static Opcion fila(String id, String nombre, String valor, String icono, Opcion.Accion alCambiar) {
        return new Opcion(id, nombre, nombre + ": < " + valor + " >", icono, true, (e, paso) -> alCambiar.hacer(e, paso == 0 ? 1 : paso));
    }

    /** El siguiente (o el anterior) de 'cuantos', dando la vuelta. */
    protected static int ciclo(int ahora, int paso, int cuantos) {
        return cuantos <= 0 ? 0 : Math.floorMod(ahora + paso, cuantos);
    }

    protected static double entre(double minimo, double valor, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    /** ¿Está apretado Ctrl o Alt? Entonces un clic es el cuentagotas (como en Krita, Photoshop o Clip Studio), sin cambiar de pincel. */
    protected static boolean cuentagotas(Editor editor) {
        return editor.partida.entrada().mantenida(com.badlogic.gdx.Input.Keys.CONTROL_LEFT) || editor.partida.entrada().mantenida(com.badlogic.gdx.Input.Keys.CONTROL_RIGHT)
                || editor.partida.entrada().mantenida(com.badlogic.gdx.Input.Keys.ALT_LEFT) || editor.partida.entrada().mantenida(com.badlogic.gdx.Input.Keys.ALT_RIGHT);
    }

    /** ¿Está apretado Mayús? */
    protected static boolean mayus(Editor editor) {
        return editor.partida.entrada().mantenida(com.badlogic.gdx.Input.Keys.SHIFT_LEFT)
                || editor.partida.entrada().mantenida(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT);
    }
}
