package metajavbots.api;

/**
 * TOPE DE CÁLCULO — Tu código ha dado más pasos de los que caben en un turno
 * (un bucle que no acaba, o uno que hace demasiado). El robot pierde el resto
 * de ese turno y, si se pasa mucho, los siguientes: el código lento hace lento
 * al robot. Es un Error (no una Exception) para que un catch (Exception e) no
 * lo tape sin querer.
 */
public final class TopeDeCalculo extends Error {

    private static final long serialVersionUID = 1L;

    public TopeDeCalculo(String mensaje) {
        super(mensaje, null, false, false);
    }
}
