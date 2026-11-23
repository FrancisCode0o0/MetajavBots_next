package metajavbots.progreso;

/**
 * RITMO DE AVISOS — Cuándo vuelve a decir el entrenador, en la pelea, el aviso
 * de órdenes (una orden que borra otra, o atacar con la mira lejos) a quien ya
 * lo ha oído. Lo que pidió Fran (23-nov): en Noob, una pelea sí y otra no (la
 * primera pelea en que comete ese fallo, sale); en Junior, menos, al azar y
 * nunca dos veces seguidas; en Senior y en Master, una de cada tantas. Se
 * cuentan solo las peleas en las que el jugador comete ese fallo y el
 * entrenador está (no contra los jefes, ni con el entrenador apagado).
 */
public final class RitmoDeAvisos {

    // TODO-FRAN: los números de «aparece menos» (Junior) y de «una cada tantas» (Senior y Master) son una propuesta
    // prudente (docs/CLARIDAD.md); cámbialos aquí.
    /** En Junior: la probabilidad de que salga en una pelea con el fallo (nunca dos veces seguidas). */
    public static final double PROBABILIDAD_JUNIOR = 0.4;
    /** En Senior: sale en la primera pelea con el fallo y luego una de cada tantas. */
    public static final int CADA_SENIOR = 4;
    /** En Master: igual, más espaciado. */
    public static final int CADA_MASTER = 6;

    private RitmoDeAvisos() {
    }

    /**
     * Si toca decir el aviso en esta pelea.
     *
     * @param categoria la de la etiqueta del jugador (Noob, Junior, Senior, Master)
     * @param vez       en cuántas peleas ha tenido el jugador este fallo, contando esta (1 = la primera)
     * @param anterior  si el aviso salió la vez anterior que tuvo el fallo
     * @param azar      un número al azar entre 0 y 1 (solo cuenta en Junior)
     */
    public static boolean toca(Etiqueta.Categoria categoria, int vez, boolean anterior, double azar) {
        if (vez <= 1) return true;
        switch (categoria) {
            case NOOB:
                return vez % 2 == 1;
            case JUNIOR:
                return !anterior && azar < PROBABILIDAD_JUNIOR;
            case SENIOR:
                return (vez - 1) % CADA_SENIOR == 0;
            default:
                return (vez - 1) % CADA_MASTER == 0;
        }
    }
}
