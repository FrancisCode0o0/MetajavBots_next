package metajavbots.pantallas;

import metajavbots.codigo.Lexico;
import metajavbots.combate.Combatiente;
import metajavbots.textos.T;

/**
 * OBJETIVO — Las pruebas de robot de los tutoriales: «aciertos>=3» (dar 3
 * golpes en la ronda), «dano<=40» (recibir como mucho 40), «ronda_sin_errores»
 * y «usa:for» (el código tiene esa palabra, de verdad: no en un comentario).
 */
public final class Objetivo {

    private Objetivo() {
    }

    /** Los aciertos que pide un objetivo «aciertos>=N» (0 si es de otro tipo). */
    public static int aciertosQuePide(String objetivo) {
        if (objetivo == null || !objetivo.startsWith("aciertos>=")) return 0;
        try {
            return Integer.parseInt(objetivo.substring(10).strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean cumple(String objetivo, Combatiente yo, String codigo) {
        if (objetivo == null) return true;
        Combatiente.Informe inf = yo.informes.isEmpty() ? new Combatiente.Informe() : yo.informes.get(yo.informes.size() - 1);
        try {
            if (objetivo.startsWith("aciertos>=")) return inf.aciertos >= Integer.parseInt(objetivo.substring(10).strip());
            if (objetivo.startsWith("dano<=")) return inf.danoRecibido <= Double.parseDouble(objetivo.substring(6).strip());
            if (objetivo.equals("ronda_sin_errores")) return inf.errores == 0 && inf.turnos > 0;
            if (objetivo.startsWith("usa:")) return usa(codigo, objetivo.substring(4).strip());
            if (objetivo.startsWith("recorre>=")) return inf.recorrido >= Double.parseDouble(objetivo.substring(9).strip());
            if (objetivo.startsWith("vistos>=")) return inf.turnosVistos >= Integer.parseInt(objetivo.substring(8).strip());
            if (objetivo.startsWith("punteria>=")) return inf.ataques >= 5 && inf.punteria() * 100 >= Double.parseDouble(objetivo.substring(10).strip());
        } catch (NumberFormatException e) {
            return false;
        }
        return false;
    }

    public static boolean usa(String codigo, String palabra) {
        for (Lexico.Trozo t : Lexico.codigo(codigo == null ? "" : codigo)) if (t.texto.equals(palabra)) return true;
        return false;
    }

    /** El objetivo dicho para el jugador. */
    public static String texto(String objetivo) {
        if (objetivo == null) return "";
        if (objetivo.startsWith("aciertos>=")) return T.f("objetivo.aciertos", objetivo.substring(10));
        if (objetivo.startsWith("dano<=")) return T.f("objetivo.dano", objetivo.substring(6));
        if (objetivo.equals("ronda_sin_errores")) return T.t("objetivo.sin_errores");
        if (objetivo.startsWith("usa:")) return T.f("objetivo.usa", objetivo.substring(4));
        if (objetivo.startsWith("recorre>=")) return T.f("objetivo.recorre", objetivo.substring(9));
        if (objetivo.startsWith("vistos>=")) return T.f("objetivo.vistos", Integer.parseInt(objetivo.substring(8)) / 30);
        if (objetivo.startsWith("punteria>=")) return T.f("objetivo.punteria", objetivo.substring(10));
        return objetivo;
    }
}
