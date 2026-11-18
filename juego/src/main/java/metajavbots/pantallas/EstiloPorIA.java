package metajavbots.pantallas;

import java.util.Locale;

import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Estilo;
import metajavbots.textos.T;

/**
 * ESTILO POR IA — Entre rondas, un cerebro de IA elige la táctica de un robot
 * de la computadora (francotirador, acosador, tanque…) a la vista de su
 * informe; no lleva cada movimiento. En el battle royale y en máquina contra
 * máquina, cada robot puede tener su cerebro (Encuentro.cerebros): uno de los
 * tres del jugador, ninguno o «mezclados» (a cada robot el siguiente).
 */
final class EstiloPorIA {

    /** Qué cerebros llevan los robots de la computadora: ninguno, el de lo rápido, uno de los tres o mezclados. */
    static final int NINGUNO = -1, EL_RAPIDO = -2, MEZCLADOS = -3;
    /** Las opciones, en el orden en que se recorren con el botón. */
    static final int[] OPCIONES = {EL_RAPIDO, 0, 1, 2, MEZCLADOS, NINGUNO};

    private EstiloPorIA() {
    }

    /** El nombre de una opción (para el botón). */
    static String nombre(int opcion) {
        switch (opcion) {
            case NINGUNO: return T.t("cerebro.ninguno");
            case EL_RAPIDO: return T.t("estilo_ia.rapido");
            case MEZCLADOS: return T.t("estilo_ia.mezclados");
            default: return T.f("estilo_ia.cerebro", opcion + 1);
        }
    }

    /** La opción siguiente a esta. */
    static int siguiente(int opcion) {
        for (int i = 0; i < OPCIONES.length; i++) if (OPCIONES[i] == opcion) return OPCIONES[(i + 1) % OPCIONES.length];
        return OPCIONES[0];
    }

    /** El cerebro que toca al robot número «cual» (desde 0) de la computadora con esa opción, o null. */
    static Cerebro cerebroDe(Jugador j, int opcion, int cual) {
        if (opcion == NINGUNO) return null;
        if (opcion == EL_RAPIDO) return CerebroDelJuego.rapido(j);
        if (opcion >= 0) return CerebroDelJuego.conRespaldo(j, opcion);
        // Mezclados: a cada robot uno de los cerebros puestos, por turno.
        java.util.List<Integer> puestas = new java.util.ArrayList<>();
        for (int i = 0; i < j.cerebros.length; i++) if (!j.cerebros[i].vacia()) puestas.add(i);
        return puestas.isEmpty() ? null : CerebroDelJuego.conRespaldo(j, puestas.get(cual % puestas.size()));
    }

    /** El informe de la ronda de un robot, para el cerebro (en inglés: los modelos lo entienden mejor). */
    static String informe(Pelea pelea, int i) {
        Combatiente.Informe yo = pelea.informe(i);
        return String.format(Locale.ROOT,
                "Round %d. This computer robot took %.0f damage, dealt %.0f, accuracy %.0f%%, close-range damage taken %.0f, long-range damage taken %.0f.",
                pelea.ronda(), yo.danoRecibido, yo.danoHecho, yo.punteria() * 100,
                yo.danoPorArmaRecibido.getOrDefault("cuerpo", 0.0) + yo.danoPorArmaRecibido.getOrDefault("choque", 0.0),
                yo.danoPorArmaRecibido.getOrDefault("laser", 0.0) + yo.danoPorArmaRecibido.getOrDefault("bala", 0.0)
                        + yo.danoPorArmaRecibido.getOrDefault("misil", 0.0));
    }

    /** Pide al cerebro, en otro hilo, la táctica de ese robot para la ronda siguiente (si no contesta, el robot se adapta solo). */
    static void pedir(Cerebro c, Combatiente robot, String informe) {
        if (c == null || robot == null) return;
        Thread h = new Thread(() -> {
            try {
                String r = c.preguntar(CerebroDelJuego.sistemaEstilo(), informe, 6).toUpperCase(Locale.ROOT);
                for (Estilo e : Estilo.values()) {
                    if (r.contains(e.name())) {
                        robot.memoria.put("estilo", e);
                        break;
                    }
                }
            } catch (Exception e) {
                // sin respuesta, el robot se adapta solo (Rival.alAcabarRonda)
            }
        }, "estilo-por-ia");
        h.setDaemon(true);
        h.start();
    }
}
