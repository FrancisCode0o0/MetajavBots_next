package castillafantasx.editor;

/**
 * RUEDA — La rueda de herramientas: media rueda colgada del borde de arriba,
 * con una casilla en el punto más bajo (la elegida) y las demás a los lados.
 * Aquí solo están sus cuentas: cuánto ha girado, dónde cae cada casilla y
 * cuál hay bajo un punto. La pinta interfaz.DibujoEditor.
 *
 * Se gira como un dial: se pincha y se arrastra a un lado, y mientras se
 * mantiene sigue girando sola, poco a poco, más deprisa cuanto más lejos.
 */
public final class Rueda {

    // TODO-FRAN: el tamaño de la rueda y lo deprisa que gira.
    // Un arco suave y ancho, con las casillas lo bastante separadas para que el nombre de cada una quepa
    // entero encima o debajo sin pisar a las vecinas (encargo ORDEN_ERGONOMIA: antes se sobreponían).
    public static final float RADIO = 1000, GRADOS_POR_CASILLA = 5f, CASILLAS_A_CADA_LADO = 3.6f;
    public static final float CASILLA = 30;                 // el lado de cada casilla (más que Estilo.TOQUE_MINIMO)
    private static final float ARRASTRE_PARA_GIRAR = 8;     // píxeles: menos que esto es un clic, no un giro
    private static final float VUELTAS_POR_PIXEL = 0.0009f; // casillas por frame y por píxel arrastrado
    private static final float VELOCIDAD_MAXIMA = 0.16f;    // casillas por frame
    private static final float MUELLE = 0.22f;              // lo deprisa que se asienta en una casilla al soltar

    /** Cuántas casillas tiene ahora. */
    public int casillas;
    /** Cuánto ha girado: el número (con decimales) de la casilla que está abajo del todo. */
    public float giro;
    /** La casilla hacia la que se asienta. */
    public int elegida;
    private boolean arrastrando, haGirado;
    private float xAlPinchar, xAhora;

    /** Pone otras casillas (al cambiar de nivel) con esa elegida, ya asentada. */
    public void poner(int cuantas, int cual) {
        casillas = cuantas;
        elegida = Math.max(0, Math.min(cuantas - 1, cual));
        giro = elegida;
        arrastrando = false;
    }

    /** Pasa a la casilla siguiente (paso 1) o a la anterior (-1), dando la vuelta. */
    public void girar(int paso) {
        if (casillas == 0) return;
        elegir(((elegida + paso) % casillas + casillas) % casillas);
    }

    /** Elige esa casilla: la rueda gira hasta ella por el camino corto. */
    public void elegir(int cual) {
        float d = distancia(cual);
        elegida = cual;
        giro = cual - d;        // el mismo sitio, contado desde la casilla nueva: el muelle recorre solo 'd'
    }

    /** Se pincha la rueda en ese punto. */
    public void pinchar(float x) {
        arrastrando = true;
        haGirado = false;
        xAlPinchar = x;
        xAhora = x;
    }

    /** El puntero se mueve con la rueda pinchada. */
    public void arrastrar(float x) {
        if (arrastrando) xAhora = x;
    }

    /** Se suelta. Devuelve true si se ha girado (no era un clic). */
    public boolean soltar() {
        boolean girado = arrastrando && haGirado;
        arrastrando = false;
        return girado;
    }

    public boolean arrastrando() {
        return arrastrando;
    }

    /** Un frame: pinchada y arrastrada a un lado, gira sola hacia ese lado; suelta, se asienta en su casilla. */
    public void actualizar() {
        if (casillas == 0) return;
        float dx = xAhora - xAlPinchar;
        if (arrastrando && Math.abs(dx) > ARRASTRE_PARA_GIRAR) {
            haGirado = true;
            // Arrastrar a la derecha trae las casillas de la izquierda (como empujar la rueda con el dedo).
            float empuje = dx - Math.signum(dx) * ARRASTRE_PARA_GIRAR;
            giro += Math.max(-VELOCIDAD_MAXIMA, Math.min(VELOCIDAD_MAXIMA, -empuje * VUELTAS_POR_PIXEL));
            if (giro < -0.5f) giro += casillas;
            if (giro >= casillas - 0.5f) giro -= casillas;
            elegida = ((Math.round(giro) % casillas) + casillas) % casillas;
        } else if (!arrastrando) {
            giro += (elegida - giro) * MUELLE;
            if (Math.abs(elegida - giro) < 0.01f) giro = elegida;
        }
    }

    /** ¿Está ya quieta en su casilla? */
    public boolean asentada() {
        return !arrastrando && giro == elegida;
    }

    /** A cuántas casillas de la de abajo queda la casilla 'i' (negativo: a la izquierda), por el camino corto de la rueda. */
    public float distancia(int i) {
        float d = i - giro;
        while (d > casillas / 2f) d -= casillas;
        while (d < -casillas / 2f) d += casillas;
        return d;
    }

    /** ¿Se ve la casilla 'i'? Solo las cercanas a la de abajo. */
    public boolean seVe(int i) {
        return Math.abs(distancia(i)) <= CASILLAS_A_CADA_LADO;
    }

    /**
     * El centro de la casilla 'i' en la imagen del juego: {x, y}. El eje de la
     * rueda está en (ejeX, ejeY), por encima del borde de arriba.
     */
    public float[] centroDe(int i, float ejeX, float ejeY) {
        double angulo = Math.toRadians(distancia(i) * GRADOS_POR_CASILLA);
        return new float[] { ejeX + (float) Math.sin(angulo) * RADIO, ejeY + (float) Math.cos(angulo) * RADIO };
    }

    /** La casilla que hay en ese punto, o -1. */
    public int en(float x, float y, float ejeX, float ejeY) {
        int mejor = -1;
        float cerca = CASILLA * 0.6f;
        for (int i = 0; i < casillas; i++) {
            if (!seVe(i)) continue;
            float[] c = centroDe(i, ejeX, ejeY);
            float d = (float) Math.hypot(x - c[0], y - c[1]);
            if (d < cerca) {
                cerca = d;
                mejor = i;
            }
        }
        return mejor;
    }
}
