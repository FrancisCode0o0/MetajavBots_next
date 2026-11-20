package metajavbots.pantalla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * TEXTO ENRIQUECIDO — Analiza la marca ligera {@code [[texto]]} para renderizar
 * expresiones enfáticas y nombres de botones o secciones en negrita y versalita
 * (mayúscula inicial al 100 % y el resto de letras en mayúscula al 78 % de escala).
 */
public final class TextoEnriquecido {

    /** Escala tipográfica de las letras en versalita respecto al tamaño base. */
    public static final float ESCALA_VERSALITA = 0.78f;

    /** Un tramo de texto plano o marcado con {@code [[...]]}. */
    public static final class Segmento {
        public final String texto;
        public final boolean versalita;

        public Segmento(String texto, boolean versalita) {
            this.texto = texto;
            this.versalita = versalita;
        }
    }

    /** Un trozo listo para dibujar con estilo uniforme (normal, negrita o versalita). */
    public static final class Trozo {
        public final String texto;
        public final boolean negrita;
        public final boolean pequeno;

        public Trozo(String texto, boolean negrita, boolean pequeno) {
            this.texto = texto;
            this.negrita = negrita;
            this.pequeno = pequeno;
        }
    }

    private TextoEnriquecido() {
    }

    /** Indica si el texto contiene al menos una marca {@code [[...]]} cerrada. */
    public static boolean tieneMarcas(String s) {
        if (s == null) return false;
        int inicio = s.indexOf("[[");
        return inicio >= 0 && s.indexOf("]]", inicio + 2) >= 0;
    }

    /** Elimina las marcas {@code [[...]]} devolviendo el texto limpio para voz o métricas planas. */
    public static String textoPlano(String s) {
        if (s == null || s.isEmpty() || !s.contains("[[")) return s == null ? "" : s;
        StringBuilder b = new StringBuilder(s.length());
        for (Segmento seg : parsear(s)) b.append(seg.texto);
        return b.toString();
    }

    /** Indica si una cadena deja una marca {@code [[} abierta sin su cierre {@code ]]}. */
    public static boolean quedaAbierta(String s) {
        if (s == null || s.isEmpty()) return false;
        boolean abierta = false;
        int i = 0;
        int n = s.length();
        while (i < n) {
            if (!abierta && i + 1 < n && s.charAt(i) == '[' && s.charAt(i + 1) == '[') {
                abierta = true;
                i += 2;
            } else if (abierta && i + 1 < n && s.charAt(i) == ']' && s.charAt(i + 1) == ']') {
                abierta = false;
                i += 2;
            } else {
                i++;
            }
        }
        return abierta;
    }

    /** Divide un texto en segmentos normales y segmentos marcados con {@code [[...]]}. */
    public static List<Segmento> parsear(String s) {
        if (s == null || s.isEmpty()) return Collections.emptyList();
        if (!s.contains("[[")) return Collections.singletonList(new Segmento(s, false));
        List<Segmento> res = new ArrayList<>();
        int pos = 0;
        int n = s.length();
        while (pos < n) {
            int ini = s.indexOf("[[", pos);
            if (ini < 0) {
                res.add(new Segmento(s.substring(pos), false));
                break;
            }
            int fin = s.indexOf("]]", ini + 2);
            if (fin < 0) {
                res.add(new Segmento(s.substring(pos), false));
                break;
            }
            if (ini > pos) res.add(new Segmento(s.substring(pos, ini), false));
            if (fin > ini + 2) res.add(new Segmento(s.substring(ini + 2, fin), true));
            pos = fin + 2;
        }
        return res;
    }

    /** Convierte un texto (con o sin marcas {@code [[...]]}) en trozos listos para renderizar. */
    public static List<Trozo> aTrozos(String s, boolean negritaBase) {
        List<Segmento> segs = parsear(s);
        if (segs.isEmpty()) return Collections.emptyList();
        List<Trozo> res = new ArrayList<>();
        for (Segmento seg : segs) {
            if (!seg.versalita) {
                res.add(new Trozo(seg.texto, negritaBase, false));
            } else {
                anadirVersalita(seg.texto, res);
            }
        }
        return res;
    }

    private static void anadirVersalita(String texto, List<Trozo> destino) {
        StringBuilder actual = new StringBuilder();
        boolean pequenoActual = false;
        boolean iniciado = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            boolean peq = Character.isLowerCase(c);
            String repr = peq ? String.valueOf(c).toUpperCase(Locale.ROOT) : String.valueOf(c);
            if (iniciado && peq != pequenoActual) {
                destino.add(new Trozo(actual.toString(), true, pequenoActual));
                actual.setLength(0);
            }
            actual.append(repr);
            pequenoActual = peq;
            iniciado = true;
        }
        if (actual.length() > 0) destino.add(new Trozo(actual.toString(), true, pequenoActual));
    }
}
