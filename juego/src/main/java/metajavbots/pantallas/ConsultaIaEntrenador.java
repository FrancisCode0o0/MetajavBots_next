package metajavbots.pantallas;

import java.util.Locale;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.ConsejosDelJuego;
import metajavbots.tutor.Entrenador;

/**
 * Construye el prompt y lanza la petición de IA para la consulta del entrenador flotante,
 * cubriendo tanto dudas de programación (Java, POO, errores, estrategia) como cualquier
 * pantalla, modo y mecánica del juego adaptándose al progreso del jugador.
 */
public final class ConsultaIaEntrenador {

    private ConsultaIaEntrenador() {
    }

    /** Lanza una petición asíncrona con la personalidad del entrenador activo y el contexto actual. */
    static CerebroDelJuego.Peticion lanzar(Cerebro cerebro, Juego j, Concepto c, VentanaDeCodigo.Senalado error,
            String ultimaPregunta, Idioma idioma, Entrenador en) {
        String sistema = construirSistema(j == null ? null : j.jugador, idioma, en);
        String contexto = construirContexto(j == null ? null : j.jugador, j == null ? null : j.pantalla(),
                c, error, ultimaPregunta, idioma);
        return CerebroDelJuego.Peticion.lanzar(cerebro, sistema, CerebroDelJuego.uno(contexto), 280);
    }

    /** Construye las instrucciones de sistema con la personalidad del entrenador, programación y mapa del juego. */
    public static String construirSistema(Jugador jugador, Idioma idioma, Entrenador en) {
        Entrenador activo = en != null ? en : Entrenador.de(jugador == null ? null : jugador.entrenador);
        Idioma id = idioma != null ? idioma : Idioma.ES;
        int ayuda = jugador == null ? 5 : Math.round(PanelDelTutor.ayuda(jugador, 0) * 10);
        return "You are " + activo.nombre.en + ", " + activo.estilo
                + ", the coach in MetajavBots, a game where you program fighting robots in Java "
                + "(class MiRobot extends Metajavbot; API: cadaTurno, alVerEnemigo, avanzar, girar, apuntarHacia, atacar, energia, "
                + "enemigosVistos, obstaculos, memoria). "
                + "Answer any question about Java, object orientation, compiler errors OR any screen, mode and mechanic of the game, "
                + "to someone who may know nothing about programming: plain words, a fight metaphor with robots, pseudocode and small Java snippets "
                + "(Java standard library and the robot API) when relevant. "
                + ConocimientoDelEntrenador.resumenParaIa() + " "
                + "Adapt your guidance to the player's progress: if they are a beginner, suggest Tutorials and Demonstrations; "
                + "if they are rusty or on a losing streak, suggest Minigames, Exercises or Today's hour. "
                + "If the player's code or fight is given, look at it and talk about THEIR code and situation (line numbers, what happens, why they lose). "
                + "Coach for real: hints, guiding questions and small pieces; NEVER the complete solution nor the whole program, even if asked. "
                + "Help level " + ayuda + "/10: at low levels only a guiding question and a small hint; at high levels pseudocode and a short snippet. "
                + "Answer in " + id.name() + " (" + id.locale().getDisplayLanguage(Locale.ENGLISH) + "), at most 200 words, no markdown.";
    }

    /** Construye el contexto de usuario incluyendo progreso del jugador, pantalla actual, concepto, error y pregunta. */
    public static String construirContexto(Jugador jugador, Pantalla pantalla, Concepto c, VentanaDeCodigo.Senalado error,
            String ultimaPregunta, Idioma idioma) {
        Idioma id = idioma != null ? idioma : Idioma.ES;
        String q = ultimaPregunta == null || ultimaPregunta.isBlank() ? (c == null ? "" : c.titulo.en(id)) : ultimaPregunta;
        StringBuilder contexto = new StringBuilder();
        if (jugador != null) {
            contexto.append("Player progress: rank=").append(jugador.etiqueta)
                    .append(", wins=").append(jugador.victorias())
                    .append(", losses=").append(jugador.derrotas())
                    .append(", lossStreak=").append(jugador.derrotasSeguidas)
                    .append(", exercisesSolved=").append(jugador.ejerciciosResueltos.size())
                    .append(", tutorialsDone=").append(jugador.tutorialesHechos.size())
                    .append(", robotsRepaired=").append(jugador.robotsArreglados.size())
                    .append(", coins=").append(Jugador.formatearMonedas(jugador.monedas));
            if (ConsejosDelJuego.esPrincipiante(jugador)) contexto.append(" [BEGINNER]");
            else if (ConsejosDelJuego.estaOxidado(jugador)) contexto.append(" [RUSTY]");
            contexto.append(".\n");
        }
        if (pantalla != null) {
            contexto.append("Current screen: ").append(pantalla.getClass().getSimpleName()).append(".\n");
        }
        contexto.append('\n');
        if (c != null) {
            contexto.append("Built-in explanation of «").append(c.titulo.en).append("»: ")
                    .append(String.join(" ", c.parrafos(Idioma.EN))).append("\n\n");
        }
        if (error != null) {
            contexto.append("The editor marks line ").append(error.marca().linea).append(": ").append(error.linea())
                    .append("\njavac: ").append(error.marca().mensaje).append("\n\n");
        }
        String ctxPantalla = pantalla == null ? null : pantalla.contextoParaElEntrenador();
        if (ctxPantalla != null) {
            contexto.append(ctxPantalla).append("\n\n");
        } else if (error != null) {
            contexto.append("The player's code:\n").append(EntrenadorFlotante.conNumeros(error.codigo(), 160)).append("\n\n");
        }
        contexto.append("The player asks: ").append(q.isBlank() ? "explain this error" : q);
        return contexto.toString();
    }

    /** El código con su número delante de cada línea (como mucho «max» líneas). */
    static String conNumeros(String codigo, int max) {
        if (codigo == null) return "";
        String[] l = codigo.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.length && i < max; i++) sb.append(i + 1).append(": ").append(l[i]).append('\n');
        if (l.length > max) sb.append("…\n");
        return sb.toString();
    }

    /** El contexto de una pantalla con editor: qué es, el código numerado y los errores marcados. */
    static String contextoDeCodigo(String que, VentanaDeCodigo editor) {
        StringBuilder sb = new StringBuilder(que).append("\nThe player's code:\n").append(conNumeros(editor.doc.texto(), 160));
        if (!editor.marcas.isEmpty()) {
            sb.append("Errors marked by the editor:\n");
            for (VentanaDeCodigo.Marca m : editor.marcas) {
                sb.append("line ").append(m.linea).append(": ").append(m.mensaje != null ? m.mensaje : m.texto).append('\n');
            }
        }
        return sb.toString();
    }
}
