package castillafantasx.interfaz;

/**
 * RELOJ DE PANTALLA — La hora de verdad para lo que solo se ve: el cursor de escribir que parpadea y el temblor
 * de la vista tras un golpe. La red de pruebas lo para (docs/RED_DE_PRUEBAS.md) para que cada foto salga igual cada vez.
 */
public final class RelojDePantalla {

    private RelojDePantalla() {
    }

    /** Parado, siempre marca 0: el cursor se ve y el temblor va siempre al mismo lado. */
    public static boolean parado = false;

    /** Los milisegundos del reloj del PC (o 0, si está parado). */
    public static long milisegundos() {
        return parado ? 0 : System.currentTimeMillis();
    }
}
