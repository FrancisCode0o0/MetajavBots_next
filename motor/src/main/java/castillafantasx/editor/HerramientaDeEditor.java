package castillafantasx.editor;

import java.util.Collections;
import java.util.List;

/**
 * HERRAMIENTA DEL EDITOR — Una tarea de la rueda. Dice su categoría, su icono,
 * su nombre y su línea de ayuda, y da las opciones que salen al entrar en
 * ella. Para añadir una: una clase que herede de esta y una línea en
 * CatalogoDelEditor (docs/EDITOR.md, «Cómo se añade una herramienta»).
 */
public abstract class HerramientaDeEditor {

    /** No cambia aunque cambie el nombre (lo usan las favoritas y los recientes). */
    public final String id;
    public final String nombre;
    public final Categoria categoria;
    /** El nombre de su icono ("f1/" + icono en el Almacen; se dibuja en arte/TallerDeIconos). */
    public final String icono;
    /** Una línea que dice para qué sirve (se ve al pasar por encima y al elegirla). */
    public final String ayuda;
    /** Más palabras por las que se encuentra en el buscador, sin tildes. */
    public final String palabras;

    protected HerramientaDeEditor(String id, String nombre, Categoria categoria, String icono, String ayuda, String palabras) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.icono = icono;
        this.ayuda = ayuda;
        this.palabras = palabras;
    }

    /** Las opciones de su panel, una por fila, con su valor de ahora. */
    public List<Opcion> opciones(Editor editor) {
        return Collections.emptyList();
    }

    /**
     * Se elige en la rueda. Devuelve true si se queda flotando arriba con su
     * panel (lo normal); false si ya ha hecho lo suyo (abrir otra pantalla).
     */
    public boolean alElegir(Editor editor) {
        return true;
    }

    /** Se quita de la pantalla (Esc, llevar la ficha hacia arriba o la ×). */
    public void alQuitar(Editor editor) { }

    /** Con ella puesta, se pincha el mundo en ese punto de la imagen. Devuelve true si era cosa suya. */
    public boolean alPincharElMundo(Editor editor, float x, float y) {
        return false;
    }

    /** Tras pinchar el mundo, con el botón aún apretado, el puntero pasa a ese punto de la imagen (una pincelada, mover lo elegido). */
    public void alArrastrarElMundo(Editor editor, float x, float y) { }

    /** Se suelta el botón con el que se pinchó el mundo, en ese punto de la imagen. */
    public void alSoltarElMundo(Editor editor, float x, float y) { }

    /** Se pincha el mundo con el botón derecho (coger lo que hay, cancelar). Devuelve true si era cosa suya; si no, el editor vuelve atrás. */
    public boolean alPincharConElDerecho(Editor editor, float x, float y) {
        return false;
    }

    /**
     * El menú del clic derecho (o de la pulsación larga) sobre lo que hay en
     * ese punto de la imagen (encargo ORDEN_ERGONOMIA_2), o null si ahí no
     * tiene menú: entonces el clic derecho es alPincharConElDerecho.
     */
    public MenuContextual menuContextual(Editor editor, float x, float y) {
        return null;
    }

    /** El mismo menú, para lo que ya está elegido (Mayús+F10, o la fila «Qué hacer con lo elegido»); null si no hay nada elegido. */
    public MenuContextual menuDeLoElegido(Editor editor) {
        return null;
    }

    /** Se toca una de las muestras de color de su mesa (Mesa.muestras). */
    public void alElegirMuestra(Editor editor, int cual) { }

    /** Lo que pinta sobre el mundo mientras está puesta (el marco de lo elegido, el fantasma de lo que va a poner); vacío si nada. */
    public List<MarcaDeEditor> marcas(Editor editor) {
        return Collections.emptyList();
    }

    /** Un clic en el título de su panel, antes que el editor (un botón propio, como el «?» de la guía del visor). Devuelve true si era suyo. */
    public boolean alPincharElTitulo(Editor editor, float x, float y) {
        return false;
    }

    /** Si pinta su propio lienzo, un clic dentro de su panel, en ese punto de la imagen. */
    public void alPincharElPanel(Editor editor, float x, float y) { }

    /** Una tecla, antes que el editor. Devuelve true si era suya (el visor de código, por ejemplo, se queda las flechas). */
    public boolean tecla(Editor editor, int tecla) {
        return false;
    }

    /** ¿Se queda las letras que se escriben? Si no, van al buscador del editor. */
    public boolean escribe(Editor editor) {
        return false;
    }

    /** Una letra escrita, si escribe(). */
    public void letra(Editor editor, char letra) { }

    /** La rueda del ratón sobre su panel (pasos > 0: hacia abajo). Devuelve true si era suya. */
    public boolean rueda(Editor editor, int pasos) {
        return false;
    }

    /** Cada frame, mientras está puesta. */
    public void actualizar(Editor editor) { }

    /** Las teclas propias, para el pie de su panel ("" si no tiene). */
    public String pie(Editor editor) {
        return "";
    }

    /** Si pinta algo propio en su panel en lugar de las opciones (el visor de código), lo que pinta; si no, null. */
    public Object lienzo(Editor editor) {
        return null;
    }

    /** Si trabaja sobre una mesa (una figura o un retrato que se ve grande sobre el mundo), esa mesa ya pintada; si no, null. */
    public Mesa mesa(Editor editor) {
        return null;
    }

    /** Cuántas tareas pendientes tiene (el globito rojo de ENCARGO_EDITOR_TABLET). */
    // TODO-FRAN: las tareas con globito llegan con el encargo de la tablet; aquí queda su sitio.
    public int pendientes() {
        return 0;
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ", " + categoria.nombre + ")";
    }
}
