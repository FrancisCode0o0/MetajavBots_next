package metajavbots.tutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * CONSEJOS DEL JUEGO — Combina consejos condicionales (según el progreso real
 * del jugador: principiante, racha de derrotas u oxidado, monedas ahorradas, o
 * código en el Taller) con consejos aleatorios de jugabilidad y funcionalidades
 * del juego alimentados desde la base centralizada {@link ConocimientoDelEntrenador}.
 */
public final class ConsejosDelJuego {

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

    /** Devuelve todos los consejos espontáneos sobre funcionalidades del juego registrados. */
    public static List<ConsejosDelTaller.Tip> todosLosConsejosDeJuego() {
        List<ConsejosDelTaller.Tip> l = new ArrayList<>();
        for (ConocimientoDelEntrenador.ConsejoJuego c : ConocimientoDelEntrenador.todosLosConsejos()) {
            l.add(c.aTip());
        }
        return Collections.unmodifiableList(l);
    }

    /** Devuelve los consejos aleatorios de jugabilidad y funcionalidades del juego. */
    public static List<ConsejosDelTaller.Tip> consejosAleatoriosDeJuego() {
        List<ConsejosDelTaller.Tip> l = new ArrayList<>();
        for (ConocimientoDelEntrenador.ConsejoJuego c : ConocimientoDelEntrenador.consejosPorCondicion(
                ConocimientoDelEntrenador.CONDICION_ALEATORIO)) {
            l.add(c.aTip());
        }
        return Collections.unmodifiableList(l);
    }

    /** Devuelve los consejos condicionales aplicables al estado actual del jugador. */
    public static List<ConsejosDelTaller.Tip> consejosCondicionales(Jugador jugador) {
        Set<ConsejosDelTaller.Tip> ordenados = new LinkedHashSet<>();
        if (esPrincipiante(jugador)) {
            agregarPorCondicion(ordenados, ConocimientoDelEntrenador.CONDICION_PRINCIPIANTE);
        } else if (estaOxidado(jugador)) {
            agregarPorCondicion(ordenados, ConocimientoDelEntrenador.CONDICION_OXIDADO);
        }
        if (jugador != null && jugador.monedas >= 2.0) {
            agregarPorCondicion(ordenados, ConocimientoDelEntrenador.CONDICION_MONEDAS);
        }
        return Collections.unmodifiableList(new ArrayList<>(ordenados));
    }

    /** Devuelve el texto de cualquier consejo (de juego o de taller) en el idioma dado. */
    public static String textoDelTip(ConsejosDelTaller.Tip tip, Idioma idioma) {
        if (tip == null) return "";
        Idioma id = idioma == null ? Idioma.ES : idioma;
        String delConocimiento = ConocimientoDelEntrenador.textoDeConsejo(tip.id(), id);
        if (delConocimiento != null && !delConocimiento.isBlank()) return delConocimiento;
        return T.en("entrenador.tip." + tip.id(), id);
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

    /** Devuelve los consejos de funcionalidades del juego ordenados priorizando condicionales del jugador. */
    public static List<ConsejosDelTaller.Tip> consejosDeJuego(Jugador jugador) {
        Set<ConsejosDelTaller.Tip> ordenados = new LinkedHashSet<>(consejosCondicionales(jugador));
        ordenados.addAll(todosLosConsejosDeJuego());
        return Collections.unmodifiableList(new ArrayList<>(ordenados));
    }

    /** Devuelve los consejos generales de programación y buenas prácticas. */
    public static List<ConsejosDelTaller.Tip> consejosGeneralesDeProgramacion() {
        return PROGRAMACION_GENERAL;
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

    private static void agregarPorCondicion(Set<ConsejosDelTaller.Tip> destino, String condicion) {
        for (ConocimientoDelEntrenador.ConsejoJuego c : ConocimientoDelEntrenador.consejosPorCondicion(condicion)) {
            destino.add(c.aTip());
        }
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
