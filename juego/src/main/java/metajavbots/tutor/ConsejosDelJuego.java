package metajavbots.tutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import metajavbots.progreso.Jugador;

/**
 * CONSEJOS DEL JUEGO — Selecciona consejos espontáneos del entrenador flotante
 * cubriendo tanto la programación (taller y buenas prácticas) como todas las
 * funcionalidades del juego (tutoriales, demostraciones, minijuegos, ejercicios,
 * la hora de hoy, mi temario, taller, tienda, depuración y testing, arena,
 * panel de la IA y ajustes), adaptando el orden al progreso del jugador.
 */
public final class ConsejosDelJuego {

    private static final ConsejosDelTaller.Tip TIP_EMPEZAR = new ConsejosDelTaller.Tip("juego_empezar_tutoriales", "tutoriales_y_demos");
    private static final ConsejosDelTaller.Tip TIP_DEMOS = new ConsejosDelTaller.Tip("juego_demostraciones", "tutoriales_y_demos");
    private static final ConsejosDelTaller.Tip TIP_OXIDADO = new ConsejosDelTaller.Tip("juego_oxidado_minijuegos", "minijuegos_y_ejercicios");
    private static final ConsejosDelTaller.Tip TIP_HORA_DE_HOY = new ConsejosDelTaller.Tip("juego_hora_de_hoy", "hora_de_hoy_y_temario");
    private static final ConsejosDelTaller.Tip TIP_TALLER = new ConsejosDelTaller.Tip("juego_taller", "taller_y_compilar");
    private static final ConsejosDelTaller.Tip TIP_TIENDA = new ConsejosDelTaller.Tip("juego_tienda", "tienda_y_monedas");
    private static final ConsejosDelTaller.Tip TIP_DEPURACION = new ConsejosDelTaller.Tip("juego_depuracion", "depuracion_y_testing");
    private static final ConsejosDelTaller.Tip TIP_TEMARIO = new ConsejosDelTaller.Tip("juego_temario", "hora_de_hoy_y_temario");
    private static final ConsejosDelTaller.Tip TIP_ARENA = new ConsejosDelTaller.Tip("juego_arena", "arena_y_modos");
    private static final ConsejosDelTaller.Tip TIP_PANEL_IA = new ConsejosDelTaller.Tip("juego_panel_ia", "panel_ia_y_ajustes");
    private static final ConsejosDelTaller.Tip TIP_AJUSTES = new ConsejosDelTaller.Tip("juego_ajustes", "panel_ia_y_ajustes");

    private static final List<ConsejosDelTaller.Tip> TODOS_JUEGO = List.of(
            TIP_EMPEZAR, TIP_DEMOS, TIP_OXIDADO, TIP_HORA_DE_HOY, TIP_TALLER,
            TIP_TIENDA, TIP_DEPURACION, TIP_TEMARIO, TIP_ARENA, TIP_PANEL_IA, TIP_AJUSTES);

    private static final List<ConsejosDelTaller.Tip> PROGRAMACION_GENERAL = List.of(
            new ConsejosDelTaller.Tip("general_f2", "pseudocodigo"),
            new ConsejosDelTaller.Tip("general_pseudo", "pseudocodigo"),
            new ConsejosDelTaller.Tip("general_nombres", "variable"),
            new ConsejosDelTaller.Tip("general_probar", "depurar"),
            new ConsejosDelTaller.Tip("general_f1", "llamar_metodo"),
            new ConsejosDelTaller.Tip("general_autocompletar", "llamar_metodo"),
            new ConsejosDelTaller.Tip("general_comentar", "comentario"),
            new ConsejosDelTaller.Tip("general_banco", "depurar"));

    private ConsejosDelJuego() {
    }

    /** Devuelve todos los consejos espontáneos sobre funcionalidades del juego. */
    public static List<ConsejosDelTaller.Tip> todosLosConsejosDeJuego() {
        return TODOS_JUEGO;
    }

    /** Indica si el jugador está empezando (sin victorias y con muy pocos tutoriales y ejercicios hechos). */
    public static boolean esPrincipiante(Jugador jugador) {
        if (jugador == null) return true;
        return jugador.victorias() == 0 && jugador.tutorialesHechos.size() < 2 && jugador.ejerciciosResueltos.size() < 3;
    }

    /** Indica si el jugador lleva derrotas recientes o poca práctica de ejercicios y le vendría bien entrenar. */
    public static boolean estaOxidado(Jugador jugador) {
        if (jugador == null) return false;
        return jugador.derrotasSeguidas >= 2 || (jugador.derrotas() > jugador.victorias() && jugador.ejerciciosResueltos.size() < 5);
    }

    /** Devuelve los consejos de funcionalidades del juego ordenados según el progreso del jugador. */
    public static List<ConsejosDelTaller.Tip> consejosDeJuego(Jugador jugador) {
        Set<ConsejosDelTaller.Tip> ordenados = new LinkedHashSet<>();
        if (esPrincipiante(jugador)) {
            ordenados.add(TIP_EMPEZAR);
            ordenados.add(TIP_DEMOS);
            ordenados.add(TIP_OXIDADO);
            ordenados.add(TIP_TALLER);
        } else if (estaOxidado(jugador)) {
            ordenados.add(TIP_OXIDADO);
            ordenados.add(TIP_HORA_DE_HOY);
            ordenados.add(TIP_DEPURACION);
            ordenados.add(TIP_DEMOS);
        }
        if (jugador != null && jugador.monedas >= 2.0) {
            ordenados.add(TIP_TIENDA);
        }
        ordenados.addAll(TODOS_JUEGO);
        return Collections.unmodifiableList(new ArrayList<>(ordenados));
    }

    /**
     * Devuelve la lista combinada de consejos candidatos para el estado actual:
     * si hay código de taller, prioriza observaciones concretas del código y luego
     * intercala consejos de juego y de programación; fuera del taller, intercala
     * consejos del juego adaptados al progreso con consejos generales de programación.
     */
    public static List<ConsejosDelTaller.Tip> candidatos(Jugador jugador, String codigoEnTaller) {
        List<ConsejosDelTaller.Tip> resultado = new ArrayList<>();
        List<ConsejosDelTaller.Tip> deJuego = consejosDeJuego(jugador);
        if (codigoEnTaller != null) {
            List<ConsejosDelTaller.Tip> delTaller = ConsejosDelTaller.de(codigoEnTaller);
            List<ConsejosDelTaller.Tip> generales = new ArrayList<>();
            for (ConsejosDelTaller.Tip t : delTaller) {
                if (t.id().startsWith("general_")) generales.add(t);
                else resultado.add(t);
            }
            intercalar(resultado, generales, deJuego);
            return resultado;
        }
        intercalar(resultado, deJuego, PROGRAMACION_GENERAL);
        return resultado;
    }

    private static void intercalar(List<ConsejosDelTaller.Tip> destino, List<ConsejosDelTaller.Tip> primera,
            List<ConsejosDelTaller.Tip> segunda) {
        int max = Math.max(primera.size(), segunda.size());
        for (int i = 0; i < max; i++) {
            if (i < primera.size()) destino.add(primera.get(i));
            if (i < segunda.size()) destino.add(segunda.get(i));
        }
    }
}
