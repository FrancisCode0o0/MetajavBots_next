package castillafantasx.editor;



/**
 * OPCIÓN — Una fila del panel de una herramienta del editor: su texto y lo
 * que hace con Intro (paso 0) y, si tiene valor, con ← (paso -1) y → (paso 1).
 */
public final class Opcion {

    /** Lo que hace una opción: paso 0 es Intro (o un clic); -1 y 1, izquierda y derecha. */
    public interface Accion {
        void hacer(Editor editor, int paso);
    }

    /** No cambia aunque cambie el texto (lo usan las favoritas y el buscador). */
    public final String id;
    /** El nombre corto (el del buscador y las favoritas). */
    public final String nombre;
    /** La fila entera, con su valor de ahora. */
    public final String texto;
    /** El nombre de su icono ("f1/" + icono), o null. */
    public final String icono;
    /** ¿Tiene un valor que cambian ← y →? Entonces se pintan sus dos flechas. */
    public final boolean conValor;
    private final Accion accion;
    /** Si es una fila que se despliega (el buscador del código): «►» cerrada, «▼» abierta; si no, null. → la abre y ← la cierra. */
    public String flecha;

    public Opcion(String id, String nombre, String texto, String icono, boolean conValor, Accion accion) {
        this.id = id;
        this.nombre = nombre;
        this.texto = texto;
        this.icono = icono;
        this.conValor = conValor;
        this.accion = accion;
    }

    /** Una opción sencilla: su texto es su nombre y solo se acciona con Intro. */
    public Opcion(String id, String texto, String icono, Accion accion) {
        this(id, texto, texto, icono, false, accion);
    }

    /**
     * El título de un grupo de filas (encargo ORDEN_ERGONOMIA_2: «La figura»,
     * «El pincel», «El color»): no se elige ni hace nada; ↑ ↓ lo saltan.
     */
    public static Opcion seccion(String titulo) {
        Opcion o = new Opcion("seccion_" + titulo, titulo, titulo, null, false, null);
        o.esSeccion = true;
        return o;
    }

    /** ¿Es el título de un grupo de filas? */
    public boolean esSeccion;

    /** Una fila que se despliega con su flechita (→ o un toque en la flecha la abre; ← la cierra; Intro hace lo suyo). */
    public static Opcion desplegable(String id, String texto, String icono, boolean abierta, Accion accion) {
        Opcion o = new Opcion(id, texto, texto, icono, true, accion);
        o.flecha = abierta ? "▼" : "►";
        return o;
    }

    /**
     * Lo que la devuelve a su valor de fábrica (encargo ORDEN_ERGONOMIA), o
     * null si no tiene. Sale su ↺ cuando está cambiada; un clic en él (o
     * Ctrl+Retroceso) la devuelve, y se puede deshacer.
     */
    public Accion deFabrica;
    /** ¿Está distinta de la de fábrica? */
    public boolean cambiada;

    /** Le da su vuelta a lo de fábrica: 'cambiada' si ahora no lo está; 'volver', lo que la devuelve. */
    public Opcion conFabrica(boolean cambiada, Accion volver) {
        this.cambiada = cambiada;
        this.deFabrica = volver;
        return this;
    }

    /** La devuelve a lo de fábrica, si tiene y está cambiada. Devuelve true si lo ha hecho. */
    public boolean volverAFabrica(Editor editor) {
        if (deFabrica == null || !cambiada) return false;
        deFabrica.hacer(editor, 0);
        return true;
    }

    /** ¿Está abierta su flechita? */
    public boolean abierta() {
        return "▼".equals(flecha);
    }

    public void hacer(Editor editor, int paso) {
        if (accion != null && (paso == 0 || conValor)) accion.hacer(editor, paso);
    }
}
