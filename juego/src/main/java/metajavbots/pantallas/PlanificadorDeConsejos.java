package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import metajavbots.progreso.Jugador;
import metajavbots.tutor.ConsejosDelJuego;
import metajavbots.tutor.ConsejosDelTaller;

/**
 * PLANIFICADOR DE CONSEJOS — Controla la cadencia prudencial de consejos
 * espontáneos del entrenador flotante, el enfriamiento ampliado tras cerrar
 * con «×», la conservación del enfriamiento entre cambios de pantalla y la
 * mezcla natural de consejos condicionales (estado del jugador o código del
 * taller) con consejos aleatorios de jugabilidad sin repeticiones seguidas.
 */
final class PlanificadorDeConsejos {

    private final Random azar;
    private ConsejosDelTaller.Tip tip;
    private boolean tipSoloDeCodigo;
    private float tiempoDelTip;
    private float esperaDelTip = EntrenadorFlotante.ESPERA_INICIAL_TIP;
    private final Set<String> tipsDados = new HashSet<>();
    private String ultimoTipId;
    private int consejosMostrados;

    PlanificadorDeConsejos() {
        this(new Random());
    }

    PlanificadorDeConsejos(Random azar) {
        this.azar = azar == null ? new Random() : azar;
    }

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
        consejosMostrados = 0;
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

    /**
     * Selecciona el siguiente consejo mezclando condicionales (observaciones del
     * taller o progreso del jugador) con consejos aleatorios de jugabilidad, sin
     * repetir el último ni repetir mientras queden inéditos en la sesión.
     */
    ConsejosDelTaller.Tip siguiente(Jugador jugador, String codigo) {
        ConsejosDelTaller.Tip elegido = elegirObservacionDeCodigo(codigo);
        if (elegido == null && tocaCondicional()) {
            elegido = elegirPrimeroDisponible(ConsejosDelJuego.consejosCondicionales(jugador), true);
        }
        if (elegido == null) {
            List<ConsejosDelTaller.Tip> poolAleatorio = construirPoolAleatorio(jugador);
            elegido = elegirAlAzar(poolAleatorio, true);
            if (elegido == null) {
                tipsDados.clear();
                elegido = elegirAlAzar(poolAleatorio, false);
            }
        }
        if (elegido == null) {
            List<ConsejosDelTaller.Tip> todos = ConsejosDelJuego.candidatos(jugador, codigo);
            if (todos.isEmpty()) return null;
            elegido = elegirPrimeroDisponible(todos, false);
            if (elegido == null) elegido = todos.get(0);
        }
        tipsDados.add(elegido.id());
        ultimoTipId = elegido.id();
        consejosMostrados++;
        tip = elegido;
        tipSoloDeCodigo = codigo != null && !elegido.id().startsWith("general_") && !elegido.id().startsWith("juego_");
        tiempoDelTip = 0f;
        return elegido;
    }

    private boolean tocaCondicional() {
        return consejosMostrados % 3 == 0;
    }

    private ConsejosDelTaller.Tip elegirObservacionDeCodigo(String codigo) {
        if (codigo == null) return null;
        List<ConsejosDelTaller.Tip> especificos = new ArrayList<>();
        for (ConsejosDelTaller.Tip t : ConsejosDelTaller.de(codigo)) {
            if (!t.id().startsWith("general_")) especificos.add(t);
        }
        return elegirPrimeroDisponible(especificos, true);
    }

    private List<ConsejosDelTaller.Tip> construirPoolAleatorio(Jugador jugador) {
        List<ConsejosDelTaller.Tip> pool = new ArrayList<>(ConsejosDelJuego.consejosAleatoriosDeJuego());
        pool.addAll(ConsejosDelJuego.consejosGeneralesDeProgramacion());
        if (pool.isEmpty()) pool.addAll(ConsejosDelJuego.consejosDeJuego(jugador));
        return pool;
    }

    private ConsejosDelTaller.Tip elegirPrimeroDisponible(List<ConsejosDelTaller.Tip> candidatos, boolean filtrarDados) {
        for (ConsejosDelTaller.Tip t : candidatos) {
            if (filtrarDados && tipsDados.contains(t.id())) continue;
            if (ultimoTipId != null && ultimoTipId.equals(t.id()) && candidatos.size() > 1) continue;
            return t;
        }
        return null;
    }

    private ConsejosDelTaller.Tip elegirAlAzar(List<ConsejosDelTaller.Tip> candidatos, boolean filtrarDados) {
        List<ConsejosDelTaller.Tip> disponibles = new ArrayList<>();
        for (ConsejosDelTaller.Tip t : candidatos) {
            if (filtrarDados && tipsDados.contains(t.id())) continue;
            if (ultimoTipId != null && ultimoTipId.equals(t.id()) && candidatos.size() > 1) continue;
            disponibles.add(t);
        }
        if (disponibles.isEmpty()) return null;
        return disponibles.get(azar.nextInt(disponibles.size()));
    }
}
