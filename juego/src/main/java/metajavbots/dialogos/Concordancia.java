package metajavbots.dialogos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metajavbots.textos.Idioma;

/**
 * CONCORDANCIA — Las frases con «{pieza}» se escribieron pensando en una
 * pieza cualquiera; aquí se ajustan a la que se ha roto, para que no salga
 * «brazo derecho rota» ni «piernas ha caído»: el género y el número del
 * adjetivo, el verbo en plural con las piernas y el artículo detrás de un
 * verbo («Has roto el brazo izquierdo»). Solo se toca lo que está pegado al
 * hueco; el resto de la frase queda como la escribió su autor.
 */
public final class Concordancia {

    private static final Pattern ADJETIVO = Pattern.compile("(\\{pieza\\}:?\\s+)(rota|perdida)\\b");
    private static final Pattern VERBO_DELANTE = Pattern.compile("\\b(roto|quitado|[Ee]limino|rompen?|eran?|quedado|fue|fueron)\\s+\\{pieza\\}");

    private Concordancia() {
    }

    /** La frase (aún con «{pieza}») ajustada a esa pieza, en ese idioma. */
    public static String ajustar(String frase, String pieza, Idioma idioma) {
        if (frase == null || pieza == null || pieza.isEmpty() || !frase.contains("{pieza}")) return frase;
        boolean plural = pieza.equals("piernas") || pieza.equals("Beine") || pieza.equals("legs");
        switch (idioma) {
            case ES: return espanol(frase, pieza, plural);
            case DE: return plural ? aleman(frase) : frase;
            default: return plural ? ingles(frase) : frase;
        }
    }

    private static String espanol(String frase, String pieza, boolean plural) {
        boolean masculino = pieza.startsWith("brazo");
        Matcher m = ADJETIVO.matcher(frase);
        StringBuilder b = new StringBuilder();
        while (m.find()) {
            String adj = m.group(2);
            if (masculino) adj = adj.substring(0, adj.length() - 1) + "o";
            if (plural) adj = adj + "s";
            m.appendReplacement(b, Matcher.quoteReplacement(m.group(1) + adj));
        }
        m.appendTail(b);
        String s = b.toString();
        if (plural) {
            s = s.replace("Mi {pieza}", "Mis {pieza}").replace("mi {pieza}", "mis {pieza}")
                    .replace("{pieza} cae", "{pieza} caen").replace("rompe {pieza}", "rompen {pieza}").replace("ha roto {pieza}", "han roto {pieza}")
                    .replace("ha quedado {pieza}", "han quedado {pieza}").replace("fue {pieza}", "fueron {pieza}")
                    .replace("era {pieza}", "eran {pieza}");
        }
        // Detrás de un verbo, con su artículo: «Has roto el brazo izquierdo».
        String articulo = plural ? "las " : masculino ? "el " : "la ";
        Matcher v = VERBO_DELANTE.matcher(s);
        StringBuilder c = new StringBuilder();
        while (v.find()) v.appendReplacement(c, Matcher.quoteReplacement(v.group().replace("{pieza}", articulo + "{pieza}")));
        v.appendTail(c);
        return c.toString();
    }

    private static String aleman(String frase) {
        return frase.replace("{pieza} ist", "{pieza} sind").replace("{pieza} hat", "{pieza} haben").replace("{pieza} fällt", "{pieza} fallen")
                .replace("{pieza} geht", "{pieza} gehen").replace("war {pieza}", "waren {pieza}").replace("Mein {pieza}", "Meine {pieza}")
                .replace("mein {pieza}", "meine {pieza}");
    }

    private static String ingles(String frase) {
        return frase.replace("{pieza} is", "{pieza} are").replace("{pieza} has", "{pieza} have").replace("{pieza} falls", "{pieza} fall")
                .replace("{pieza} leaves", "{pieza} leave").replace("was {pieza}", "were {pieza}").replace("was my {pieza}", "were my {pieza}");
    }
}
