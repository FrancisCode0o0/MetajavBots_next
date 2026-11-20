package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.codigo.Lexico;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.ConsejosDelTaller;

/**
 * REVISOR — Consejos sobre el código y la ronda, sin IA (con el cerebro de
 * IA, la Revisión los amplía): lo que dicen las cifras de la ronda (puntería,
 * lo que ve el radar, de dónde le llegan los golpes, los errores, los topes)
 * y el estilo de los profesionales (números mágicos, métodos largos,
 * demasiada sangría, catch vacíos, textos comparados con ==, nombres).
 */
public final class Revisor {

    /** Lo que dice el Revisor: los consejos sobre la ronda y sobre el estilo del código. */
    public static final class Informe {
        public final List<String> consejos = new ArrayList<>();
    }

    private Revisor() {
    }

    public static Informe revisar(String codigo, Pelea pelea, Idioma idioma) {
        Informe r = new Informe();
        if (pelea != null && pelea.ronda() > 0) {
            Combatiente.Informe inf = pelea.informe(0);
            if (inf.ataques == 0) r.consejos.add(T.t("revisor.no_ataca"));
            else if (inf.punteria() < 0.3) r.consejos.add(T.f("revisor.punteria", Math.round(inf.punteria() * 100)));
            if (inf.turnos > 0 && inf.turnosVistos < inf.turnos * 0.3) r.consejos.add(T.t("revisor.radar"));
            double cerca = inf.danoPorArmaRecibido.getOrDefault("cuerpo", 0.0) + inf.danoPorArmaRecibido.getOrDefault("llama", 0.0)
                    + inf.danoPorArmaRecibido.getOrDefault("choque", 0.0);
            if (inf.danoRecibido > 20 && cerca > inf.danoRecibido * 0.5) r.consejos.add(T.t("revisor.cerca"));
            double lejos = inf.danoPorArmaRecibido.getOrDefault("laser", 0.0) + inf.danoPorArmaRecibido.getOrDefault("bala", 0.0)
                    + inf.danoPorArmaRecibido.getOrDefault("misil", 0.0);
            if (inf.danoRecibido > 20 && lejos > inf.danoRecibido * 0.6) r.consejos.add(T.t("revisor.lejos"));
            if (inf.danoPorArmaRecibido.getOrDefault("pared", 0.0) > 5) r.consejos.add(T.t("revisor.paredes"));
            if (inf.errores > 0) r.consejos.add(T.f("revisor.errores", inf.errores, pelea.combatientes[0].ultimoError));
            if (inf.topes > 0) r.consejos.add(T.f("revisor.topes", inf.topes));
            if (pelea.tieneMultiplesObjetivos() && !ConsejosDelTaller.recorreEnemigosVistosConBucle(codigo)) {
                r.consejos.add(T.t("revisor.multiples_objetivos"));
            }
            if (pelea.combatientes[0].efectosPoo != null && pelea.combatientes[0].efectosPoo.camposPublicosMutables > 0) {
                r.consejos.add(T.f("poo.consejo_public", pelea.combatientes[0].efectosPoo.camposPublicosMutables));
            }
        }
        estilo(codigo, r);
        if (r.consejos.isEmpty()) r.consejos.add(T.t("revisor.bien"));
        return r;
    }

    static void estilo(String codigo, Informe r) {
        List<Lexico.Trozo> t = Lexico.codigo(codigo);
        int magicos = 0;
        for (int i = 0; i < t.size(); i++) {
            Lexico.Trozo x = t.get(i);
            if (x.clase == Lexico.Clase.NUMERO && !x.texto.matches("0|1|2|0\\.0|1\\.0|90|180|360")) magicos++;
            if (x.es("==") && i > 0 && i + 1 < t.size() && (t.get(i + 1).clase == Lexico.Clase.TEXTO || t.get(i - 1).clase == Lexico.Clase.TEXTO)) {
                r.consejos.add(T.t("revisor.texto_igual"));
            }
            if (x.es("catch")) {
                int k = i + 1, nivel = 0;
                while (k < t.size() && !(t.get(k).es(")") && --nivel < 0)) {
                    if (t.get(k).es("(")) nivel++;
                    k++;
                }
                if (k + 2 < t.size() && t.get(k + 1).es("{") && t.get(k + 2).es("}")) r.consejos.add(T.t("revisor.catch_vacio"));
            }
            if (x.es("class") && i + 1 < t.size() && Character.isLowerCase(t.get(i + 1).texto.charAt(0))) r.consejos.add(T.t("revisor.nombre_clase"));
        }
        if (magicos >= 6) r.consejos.add(T.f("revisor.magicos", magicos));
        // Métodos largos y sangría profunda (por líneas).
        String[] lineas = codigo.split("\n");
        int profundidad = 0, maxima = 0;
        for (String l : lineas) {
            int s = 0;
            while (s < l.length() && l.charAt(s) == ' ') s++;
            if (!l.isBlank()) maxima = Math.max(maxima, s / 4);
        }
        if (maxima >= 6) r.consejos.add(T.t("revisor.sangria"));
        int inicio = -1, nivel = 0;
        for (int i = 0; i < lineas.length; i++) {
            String l = lineas[i].strip();
            if (nivel == 1 && l.endsWith("{") && l.contains("(")) inicio = i;
            for (char c : l.toCharArray()) {
                if (c == '{') nivel++;
                if (c == '}') nivel--;
            }
            if (nivel == 1 && inicio >= 0) {
                if (i - inicio > 40) r.consejos.add(T.f("revisor.metodo_largo", inicio + 1, i - inicio));
                inicio = -1;
            }
            profundidad = Math.max(profundidad, nivel);
        }
    }
}
