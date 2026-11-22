package metajavbots.pantallas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import metajavbots.progreso.Jugador;
import metajavbots.tutor.ConsejosDelJuego;
import metajavbots.tutor.ConsejosDelTaller;

/**
 * PLANIFICADOR DE CONSEJOS — Controla la cadencia moderada de consejos
 * espontáneos del entrenador flotante, el enfriamiento prolongado tras cerrar
 * con «×», la conservación del enfriamiento entre cambios de pantalla y la no
 * repetición inmediata de consejos.
 */
final class PlanificadorDeConsejos {

    private ConsejosDelTaller.Tip tip;
    private boolean tipSoloDeCodigo;
    private float tiempoDelTip;
    private float esperaDelTip = EntrenadorFlotante.ESPERA_INICIAL_TIP;
    private final Set<String> tipsDados = new HashSet<>();
    private String ultimoTipId;

    ConsejosDelTaller.Tip tipActual() {
        return tip;
    }

    float tiempoDelTip() {
        return tiempoDelTip;
    }

    float esperaDelTip() {
        return esperaDelTip;
    }

    void tipAhora() {
        esperaDelTip = 0f;
    }

    void cerrarManual() {
        cerrar(EntrenadorFlotante.ESPERA_TRAS_CERRAR_MANUAL);
    }

    void cerrarNormal() {
        cerrar(EntrenadorFlotante.ESPERA_ENTRE_TIPS);
    }

    private void cerrar(float enfriamiento) {
        tip = null;
        tipSoloDeCodigo = false;
        tiempoDelTip = 0f;
        esperaDelTip = Math.max(esperaDelTip, enfriamiento);
    }

    /** Limpia el consejo visible y el historial al cambiar de entrenador, sin reiniciar enfriamientos largos. */
    void alCambiarEntrenador() {
        tip = null;
        tipSoloDeCodigo = false;
        tiempoDelTip = 0f;
        tipsDados.clear();
        ultimoTipId = null;
    }

    /**
     * Avanza el temporizador de consejos espontáneos.
     * Si el jugador sale del taller con un consejo específico de código visible, lo oculta
     * conservando el enfriamiento (nunca lo reinicia a 0).
     */
    boolean avanzar(Jugador jugador, String codigo, float dt, boolean permitirFueraDeTaller) {
        if (codigo == null && !permitirFueraDeTaller) {
            if (tip != null) cerrarNormal();
            return false;
        }
        if (codigo == null && tip != null && tipSoloDeCodigo) {
            cerrarNormal();
            return false;
        }
        if (tip == null) {
            esperaDelTip -= dt;
            if (esperaDelTip > 0f) return false;
            tip = siguiente(jugador, codigo);
            tiempoDelTip = dt;
            return tip != null;
        }
        tiempoDelTip += dt;
        if (tiempoDelTip > EntrenadorFlotante.DURACION_VISIBLE_TIP) {
            cerrarNormal();
        }
        return false;
    }

    /** Selecciona el siguiente consejo sin repetir el último ni repetir mientras queden inéditos. */
    ConsejosDelTaller.Tip siguiente(Jugador jugador, String codigo) {
        List<ConsejosDelTaller.Tip> candidatos = ConsejosDelJuego.candidatos(jugador, codigo);
        if (candidatos.isEmpty()) return null;
        ConsejosDelTaller.Tip elegido = buscarNoRepetido(candidatos, true);
        if (elegido == null) {
            tipsDados.clear();
            elegido = buscarNoRepetido(candidatos, false);
        }
        if (elegido == null) elegido = candidatos.get(0);
        tipsDados.add(elegido.id());
        ultimoTipId = elegido.id();
        tip = elegido;
        tipSoloDeCodigo = codigo != null && !elegido.id().startsWith("general_") && !elegido.id().startsWith("juego_");
        tiempoDelTip = 0f;
        return elegido;
    }

    private ConsejosDelTaller.Tip buscarNoRepetido(List<ConsejosDelTaller.Tip> candidatos, boolean filtrarDados) {
        for (ConsejosDelTaller.Tip t : candidatos) {
            if (filtrarDados && tipsDados.contains(t.id())) continue;
            if (ultimoTipId != null && ultimoTipId.equals(t.id()) && candidatos.size() > 1) continue;
            return t;
        }
        return null;
    }
}
