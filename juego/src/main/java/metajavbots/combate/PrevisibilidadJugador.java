package metajavbots.combate;

/**
 * Seguimiento ligero de previsibilidad del movimiento del jugador en
 * «Destrucción y supervivencia». Detecta si en los últimos turnos el robot
 * permaneció casi quieto o mantuvo una trayectoria recta y velocidad constantes.
 */
final class PrevisibilidadJugador {

    private static final int VENTANA = 16;
    private static final int MIN_MUESTRAS = 12;
    private static final double UMBRAL_DESPLAZAMIENTO_QUIETO = 18.0;
    private static final double UMBRAL_PASO_QUIETO = 1.2;
    private static final double UMBRAL_DELTA_RUMBO_PASO = 2.5;
    private static final double UMBRAL_CAMBIO_RUMBO_TOTAL = 12.0;
    private static final double UMBRAL_RANGO_VELOCIDAD = 2.0;

    private final double[] xs = new double[VENTANA];
    private final double[] ys = new double[VENTANA];
    private final double[] rumbos = new double[VENTANA];
    private final double[] velocidades = new double[VENTANA];
    private int cabeza;
    private int muestras;

    /** Registra la posición, rumbo y velocidad actuales del jugador. */
    void registrar(Cuerpo c0) {
        if (c0 == null || c0.fuera) {
            muestras = 0;
            cabeza = 0;
            return;
        }
        xs[cabeza] = c0.x;
        ys[cabeza] = c0.y;
        rumbos[cabeza] = c0.rumbo;
        velocidades[cabeza] = c0.velocidad;
        cabeza = (cabeza + 1) % VENTANA;
        if (muestras < VENTANA) muestras++;
    }

    /** Indica si el patrón de movimiento reciente del jugador es previsible. */
    boolean esPrevisible() {
        if (muestras < MIN_MUESTRAS) return false;
        int inicio = (cabeza - muestras + VENTANA) % VENTANA;
        int ultimo = (cabeza - 1 + VENTANA) % VENTANA;
        double despNeto = Math.hypot(xs[ultimo] - xs[inicio], ys[ultimo] - ys[inicio]);
        double sumaPasos = 0.0;
        double sumaDeltaRumbo = 0.0;
        double maxDeltaRumbo = 0.0;
        double minVel = velocidades[inicio];
        double maxVel = velocidades[inicio];

        int prev = inicio;
        for (int k = 1; k < muestras; k++) {
            int idx = (inicio + k) % VENTANA;
            sumaPasos += Math.hypot(xs[idx] - xs[prev], ys[idx] - ys[prev]);
            double dRumbo = Math.abs(Pelea.normal(rumbos[idx] - rumbos[prev]));
            sumaDeltaRumbo += dRumbo;
            if (dRumbo > maxDeltaRumbo) maxDeltaRumbo = dRumbo;
            double v = velocidades[idx];
            if (v < minVel) minVel = v;
            if (v > maxVel) maxVel = v;
            prev = idx;
        }

        double pasoMedio = sumaPasos / (muestras - 1);
        if (despNeto <= UMBRAL_DESPLAZAMIENTO_QUIETO && pasoMedio <= UMBRAL_PASO_QUIETO) {
            return true;
        }
        return maxDeltaRumbo <= UMBRAL_DELTA_RUMBO_PASO
                && sumaDeltaRumbo <= UMBRAL_CAMBIO_RUMBO_TOTAL
                && (maxVel - minVel) <= UMBRAL_RANGO_VELOCIDAD;
    }
}
