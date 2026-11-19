package metajavbots.combate;

/**
 * ORDEN DEL TURNO — Una acción pedida por el robot durante un turno (avanzar,
 * girar, apuntar, detener, atacar…), el método desde el que se pidió, la línea
 * de código donde se ejecutó y si otra orden posterior del mismo turno la pisó.
 */
public final class OrdenDelTurno {

    public final String metodo;
    public final String accion;
    public final int canales;
    public final int linea;
    public final int invocacion;
    public final String pisadaPor;
    public final int lineaPisada;

    public OrdenDelTurno(String metodo, String accion, int canales, int linea, int invocacion, String pisadaPor, int lineaPisada) {
        this.metodo = metodo == null ? "" : metodo;
        this.accion = accion == null ? "" : accion;
        this.canales = canales;
        this.linea = linea;
        this.invocacion = invocacion;
        this.pisadaPor = pisadaPor == null ? "" : pisadaPor;
        this.lineaPisada = lineaPisada;
    }

    /** Si otra orden posterior en el mismo turno sobrescribió o anuló esta. */
    public boolean pisada() {
        return !pisadaPor.isEmpty();
    }

    /** Devuelve una copia marcada como pisada por otra orden posterior. */
    OrdenDelTurno conPisada(String por, int lineaQuePisa) {
        return new OrdenDelTurno(metodo, accion, canales, linea, invocacion, por, lineaQuePisa);
    }
}
