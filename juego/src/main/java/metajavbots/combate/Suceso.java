package metajavbots.combate;

/**
 * SUCESO — Algo que pasa en la pelea y que alguien tiene que ver u oír: un
 * disparo, un golpe, una explosión, lo que dice un robot… La pelea los deja
 * en una lista cada turno; los leen el dibujo (chispas, humo, destellos), el
 * sonido y el locutor.
 */
public final class Suceso {

    public enum Tipo {
        RONDA_EMPIEZA, RONDA_ACABA, PELEA_ACABA,
        LASER, DISPARO, MISIL, LLAMA, GOLPE_CUERPO, FALLO_CUERPO,
        IMPACTO, ESQUIVA, BLOQUEO, EXPLOSION, PARED, CHOQUE,
        PARTE_ROTA, FUERA_DE_COMBATE, ESPECIAL, REPARACION, ATURDIDO,
        DECIR, ERROR_CODIGO, TOPE
    }

    public final Tipo tipo;
    /** Quién lo hace (0 o 1); -1 si es de la pelea. */
    public final int quien;
    public final double x, y, angulo, valor;
    /** Un texto (lo que dice, el arma, la pieza…). */
    public final String texto;
    public final int turno;

    public Suceso(Tipo tipo, int quien, double x, double y, double angulo, double valor, String texto, int turno) {
        this.tipo = tipo;
        this.quien = quien;
        this.x = x;
        this.y = y;
        this.angulo = angulo;
        this.valor = valor;
        this.texto = texto == null ? "" : texto;
        this.turno = turno;
    }

    @Override
    public String toString() {
        return tipo + "(" + quien + ", " + texto + ", " + Math.round(valor) + ")";
    }
}
