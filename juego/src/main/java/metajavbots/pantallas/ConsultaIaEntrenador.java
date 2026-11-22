package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.Idioma;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.Entrenador;

/**
 * Construye el prompt y lanza la petición de IA para la consulta del entrenador flotante.
 */
final class ConsultaIaEntrenador {

    private ConsultaIaEntrenador() {
    }

    /** Lanza una petición asíncrona con la personalidad del entrenador activo y el contexto actual de la pantalla. */
    static CerebroDelJuego.Peticion lanzar(Cerebro cerebro, Juego j, Concepto c, VentanaDeCodigo.Senalado error,
            String ultimaPregunta, Idioma idioma, Entrenador en) {
        String q = ultimaPregunta.isBlank() ? c == null ? "" : c.titulo.en(idioma) : ultimaPregunta;
        int ayuda = Math.round(PanelDelTutor.ayuda(j.jugador, 0) * 10);
        String sistema = "You are " + en.nombre.en + ", " + en.estilo + ", the coach in MetajavBots, a game where you program fighting robots in Java "
                + "(class MiRobot extends Metajavbot; API: cadaTurno, alVerEnemigo, avanzar, girar, apuntarHacia, atacar, energia, enemigosVistos, memoria). "
                + "Answer any question about Java, object orientation or the game, to someone who may know nothing about programming: plain words, "
                + "a fight metaphor with robots, LOTS of pseudocode and small Java snippets (Java standard library and the robot API). "
                + "If the player's code or fight is given, look at it and talk about THEIR code and situation (line numbers, what happens, why they lose). "
                + "Coach for real: hints, guiding questions and small pieces; NEVER the complete solution nor the whole program, even if asked. "
                + "Help level " + ayuda + "/10: at low levels only a guiding question and a small hint; at high levels pseudocode and a short snippet. "
                + "Answer in " + idioma.name() + " (" + idioma.locale().getDisplayLanguage(java.util.Locale.ENGLISH) + "), at most 200 words, no markdown.";
        StringBuilder contexto = new StringBuilder();
        if (c != null) contexto.append("Built-in explanation of «").append(c.titulo.en).append("»: ")
                .append(String.join(" ", c.parrafos(Idioma.EN))).append("\n\n");
        if (error != null) contexto.append("The editor marks line ").append(error.marca().linea).append(": ").append(error.linea()).append("\njavac: ")
                .append(error.marca().mensaje).append("\n\n");
        String pantalla = j.pantalla() == null ? null : j.pantalla().contextoParaElEntrenador();
        if (pantalla != null) contexto.append(pantalla).append("\n\n");
        else if (error != null) contexto.append("The player's code:\n").append(EntrenadorFlotante.conNumeros(error.codigo(), 160)).append("\n\n");
        contexto.append("The player asks: ").append(q.isBlank() ? "explain this error" : q);
        return CerebroDelJuego.Peticion.lanzar(cerebro, sistema, CerebroDelJuego.uno(contexto.toString()), 280);
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
