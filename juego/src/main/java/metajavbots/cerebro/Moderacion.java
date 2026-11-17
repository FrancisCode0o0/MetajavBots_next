package metajavbots.cerebro;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * MODERACIÓN — Lo que dice la IA en las pullas pasa por aquí: si toca
 * cualquier tema prohibido (origen, cuerpo, sexo, religión, discapacidad…)
 * o suena a insulto de odio, se tira y se usa una frase de las escritas a
 * mano. Las pullas de ring sí: «¡te voy a dejar más plano que un disquete!».
 * Además, la petición ya le dice al modelo que no lo haga; esto es la
 * segunda red.
 */
public final class Moderacion {

    // Raíces (sin tildes, en minúscula) de lo que no se dice nunca, en los tres idiomas. Lista corta a propósito:
    // se tira la frase entera si aparece cualquiera, aunque sea en un contexto inocente.
    private static final List<String> PROHIBIDO = List.of(
            "raza", "racis", "negr", "moro", "sudaca", "gitan", "judi", "jud", "musulm", "islam", "cristian", "religi", "dios", "allah",
            "gay", "marica", "maric", "maricon", "lesbi", "trans", "puta", "puto", "zorra", "violar", "viola", "nazi", "hitler",
            "retrasad", "subnormal", "mongol", "autista", "minusval", "discapac", "tullid", "gord", "fe@", "feo", "fea", "enano",
            "maldit", "muere", "muerte", "matar", "suicid", "cancer",
            "rasse", "neger", "jude", "muslim", "schwul", "schwuchtel", "nutte", "hure", "behindert", "spast", "krüppel", "kruppel",
            "fett", "tod", "toten", "umbring", "selbstmord", "vergewalt",
            "race", "nigg", "jew", "muslim", "faggot", "fag", "dyke", "tranny", "whore", "slut", "retard", "cripple", "fatso", "kill",
            "suicide", "rape", "die ", "god", "jesus");

    private Moderacion() {
    }

    static String limpio(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** ¿Se puede decir esta pulla en el juego? */
    public static boolean aceptable(String frase) {
        if (frase == null || frase.isBlank() || frase.length() > 160) return false;
        String s = " " + limpio(frase) + " ";
        for (String p : PROHIBIDO) if (s.contains(p)) return false;
        return true;
    }
}
