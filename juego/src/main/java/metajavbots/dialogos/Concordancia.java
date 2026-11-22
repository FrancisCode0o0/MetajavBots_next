package metajavbots.dialogos;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metajavbots.textos.Idioma;

/**
 * CONCORDANCIA — Las frases con «{pieza}» se escribieron pensando en una
 * pieza cualquiera; aquí se ajustan a la que se ha roto, para que no salga
 * «brazo derecho rota» ni «piernas ha caído», ni errores de caso o género en
 * alemán («Ohne linker Arm», «Mit meinem Beine», «Du hast linker Arm zerstört»).
 */
public final class Concordancia {

    private enum CasoDe { NOM, AKK, DAT }

    private enum PiezaDe {
        KOPF(false, "Kopf", "Kopf", "Kopf", "Kopf", "Kopf", "Kopf", "Kopf"),
        LINKER_ARM(false, "linker Arm", "linke Arm", "linker Arm", "linken Arm", "linken Arm", "linkem Arm", "linken Arm"),
        RECHTER_ARM(false, "rechter Arm", "rechte Arm", "rechter Arm", "rechten Arm", "rechten Arm", "rechtem Arm", "rechten Arm"),
        BEINE(true, "Beine", "Beine", "Beine", "Beine", "Beine", "Beinen", "Beinen");

        final boolean plural;
        final String nomBare, nomDef, nomIndef, akkBare, akkDet, datBare, datDet;

        PiezaDe(boolean plural, String nomBare, String nomDef, String nomIndef,
                String akkBare, String akkDet, String datBare, String datDet) {
            this.plural = plural;
            this.nomBare = nomBare;
            this.nomDef = nomDef;
            this.nomIndef = nomIndef;
            this.akkBare = akkBare;
            this.akkDet = akkDet;
            this.datBare = datBare;
            this.datDet = datDet;
        }

        static PiezaDe de(String pieza) {
            String s = pieza.strip().toLowerCase(Locale.ROOT);
            if (s.equals("beine") || s.equals("die beine")) return BEINE;
            if (s.contains("link")) return LINKER_ARM;
            if (s.contains("recht")) return RECHTER_ARM;
            if (s.contains("kopf")) return KOPF;
            return null;
        }

        String artDef(CasoDe caso) {
            if (plural) return caso == CasoDe.DAT ? "den" : "die";
            if (caso == CasoDe.AKK) return "den";
            return caso == CasoDe.DAT ? "dem" : "der";
        }

        String sustantivoConDef(CasoDe caso) {
            if (caso == CasoDe.AKK) return akkDet;
            return caso == CasoDe.DAT ? datDet : nomDef;
        }

        String sustantivoConIndef(CasoDe caso) {
            if (caso == CasoDe.AKK) return akkDet;
            return caso == CasoDe.DAT ? datDet : nomIndef;
        }

        String sustantivoSolo(CasoDe caso) {
            if (caso == CasoDe.AKK) return akkBare;
            return caso == CasoDe.DAT ? datBare : nomBare;
        }

        String sufijoPosesivo(CasoDe caso) {
            if (plural) return caso == CasoDe.DAT ? "en" : "e";
            if (caso == CasoDe.AKK) return "en";
            return caso == CasoDe.DAT ? "em" : "";
        }
    }

    private static final Pattern ADJETIVO = Pattern.compile("(\\{pieza\\}:?\\s+)(rota|perdida)\\b");
    private static final Pattern VERBO_DELANTE = Pattern.compile("\\b(roto|quitado|[Ee]limino|rompen?|eran?|quedado|fue|fueron)\\s+\\{pieza\\}");

    private static final Pattern DE_DET = Pattern.compile(
            "(?i)\\b(der|den|dem|die|mein(?:en|em|e)?|dein(?:en|em|e)?|sein(?:en|em|e)?|ihr(?:en|em|e)?|kein(?:en|em|e)?|ein(?:en|em|e)?)\\s+\\{pieza\\}");
    private static final Pattern DE_PREP_DAT = Pattern.compile("(?i)\\b(mit|von|aus|bei|nach|seit|zu|an|auf|in|unter|über|vor)\\s+$");
    private static final Pattern DE_PREP_AKK = Pattern.compile("(?i)\\b(ohne|für|gegen|durch|um)\\s+$");
    private static final Pattern DE_TRANSITIVO = Pattern.compile(
            "(?i)\\b(hast(?:\\s+mir)?|hat(?:\\s+mir)?|habe(?:\\s+mir)?|entferne|verliere|brauche|sehe|nehme|zerstöre|triffst|trifft|jemand)\\s+$");
    private static final Pattern DE_PARTICIPIO_AKK = Pattern.compile(
            "(?i)^\\s+(zerstört|genommen|zerbrochen|gesehen|verloren|abgesägt|abgerissen|wegkatapultiert|zerschlagen|durchbohrt|weggerissen)\\b");
    private static final Pattern DE_BARE_PREP_AKK = Pattern.compile("(?i)\\b(ohne|für|gegen|durch|um)\\s+\\{pieza\\}");
    private static final Pattern DE_BARE_PREP_DAT = Pattern.compile("(?i)\\b(mit|von|aus|bei|nach|seit|zu)\\s+\\{pieza\\}");
    private static final Pattern DE_BARE_TRANS = Pattern.compile(
            "(?i)\\b(hast(?:\\s+mir)?|hat(?:\\s+mir)?|habe(?:\\s+mir)?|entferne|verliere|brauche|jemand)\\s+\\{pieza\\}");
    private static final Pattern DE_VERBO_PLURAL = Pattern.compile("\\{pieza\\}\\s+(ist|hat|fällt|geht|bleibt|fliegt)\\b(?!\\s+es\\b)");
    private static final Pattern DE_PREP_ANTES = Pattern.compile(
            "(?i)\\b(?:mit|ohne|von|aus|bei|nach|seit|zu|an|auf|in|unter|über|vor|für|gegen|durch|um)\\s+(?:\\S+\\s+)?$");

    private Concordancia() {
    }

    /** La frase (aún con «{pieza}») ajustada a esa pieza, en ese idioma. */
    public static String ajustar(String frase, String pieza, Idioma idioma) {
        if (frase == null || pieza == null || pieza.isEmpty() || !frase.contains("{pieza}")) return frase;
        boolean plural = pieza.equals("piernas") || pieza.equalsIgnoreCase("Beine") || pieza.equalsIgnoreCase("die Beine") || pieza.equals("legs");
        switch (idioma) {
            case ES: return espanol(frase, pieza, plural);
            case DE: return aleman(frase, pieza, plural);
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
        String articulo = plural ? "las " : masculino ? "el " : "la ";
        Matcher v = VERBO_DELANTE.matcher(s);
        StringBuilder c = new StringBuilder();
        while (v.find()) v.appendReplacement(c, Matcher.quoteReplacement(v.group().replace("{pieza}", articulo + "{pieza}")));
        v.appendTail(c);
        return c.toString();
    }

    private static String aleman(String frase, String pieza, boolean plural) {
        PiezaDe pd = PiezaDe.de(pieza);
        String s = plural ? pluralizarVerbosDe(frase) : frase;
        if (pd == null) {
            return plural ? s.replace("Mein {pieza}", "Meine {pieza}").replace("mein {pieza}", "meine {pieza}") : s;
        }
        s = reemplazarConDeterminanteDe(s, pd, pieza);
        s = reemplazarSinDeterminanteDe(s, pd, pieza);
        return s;
    }

    private static String pluralizarVerbosDe(String frase) {
        String s = frase.replace("war {pieza}", "waren {pieza}");
        Matcher m = DE_VERBO_PLURAL.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String antes = s.substring(0, m.start());
            if (DE_PREP_ANTES.matcher(antes).find()) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
                continue;
            }
            String verbo = m.group(1);
            String pl = verbo.equals("ist") ? "sind" : verbo.equals("hat") ? "haben" : verbo.equals("fällt") ? "fallen"
                    : verbo.equals("geht") ? "gehen" : verbo.equals("bleibt") ? "bleiben" : "fliegen";
            m.appendReplacement(out, Matcher.quoteReplacement("{pieza} " + pl));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String reemplazarConDeterminanteDe(String frase, PiezaDe pd, String piezaOrig) {
        Matcher m = DE_DET.matcher(frase);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String det = m.group(1);
            String antes = frase.substring(0, m.start());
            String despues = frase.substring(m.end());
            CasoDe caso = detectarCasoDe(det, antes, despues);
            String reemplazo = declinarDeterminanteDe(det, caso, pd, piezaOrig);
            m.appendReplacement(out, Matcher.quoteReplacement(reemplazo));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String reemplazarSinDeterminanteDe(String frase, PiezaDe pd, String piezaOrig) {
        String s = aplicarPatronBare(frase, DE_BARE_PREP_AKK, CasoDe.AKK, false, pd, piezaOrig);
        s = aplicarPatronBare(s, DE_BARE_PREP_DAT, CasoDe.DAT, true, pd, piezaOrig);
        s = aplicarPatronBare(s, DE_BARE_TRANS, CasoDe.AKK, true, pd, piezaOrig);
        return s;
    }

    private static String aplicarPatronBare(String frase, Pattern patron, CasoDe caso, boolean conArtDef, PiezaDe pd, String piezaOrig) {
        Matcher m = patron.matcher(frase);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String prefijo = m.group(1);
            String sust = conArtDef ? pd.artDef(caso) + " " + formaSustantivo(pd.sustantivoConDef(caso), piezaOrig)
                    : formaSustantivo(pd.sustantivoSolo(caso), piezaOrig);
            m.appendReplacement(out, Matcher.quoteReplacement(prefijo + " " + sust));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static CasoDe detectarCasoDe(String det, String antes, String despues) {
        if (DE_PREP_DAT.matcher(antes).find()) return CasoDe.DAT;
        if (DE_PREP_AKK.matcher(antes).find()) return CasoDe.AKK;
        if (DE_TRANSITIVO.matcher(antes).find() || DE_PARTICIPIO_AKK.matcher(despues).find()) return CasoDe.AKK;
        String d = det.toLowerCase(Locale.ROOT);
        if (d.equals("dem") || d.endsWith("em")) return CasoDe.DAT;
        if (d.equals("den") || d.endsWith("en")) return CasoDe.AKK;
        return CasoDe.NOM;
    }

    private static String declinarDeterminanteDe(String det, CasoDe caso, PiezaDe pd, String piezaOrig) {
        boolean mayus = Character.isUpperCase(det.charAt(0));
        String d = det.toLowerCase(Locale.ROOT);
        if (d.equals("der") || d.equals("den") || d.equals("dem") || d.equals("die")) {
            String art = capitalizar(pd.artDef(caso), mayus);
            return art + " " + formaSustantivo(pd.sustantivoConDef(caso), piezaOrig);
        }
        String raiz = d.startsWith("mein") ? "mein" : d.startsWith("dein") ? "dein"
                : d.startsWith("sein") ? "sein" : d.startsWith("ihr") ? "ihr"
                : d.startsWith("kein") ? "kein" : "ein";
        if (raiz.equals("ein") && pd.plural) {
            return capitalizar(pd.sustantivoSolo(caso), mayus);
        }
        String formaDet = capitalizar(raiz + pd.sufijoPosesivo(caso), mayus);
        return formaDet + " " + formaSustantivo(pd.sustantivoConIndef(caso), piezaOrig);
    }

    private static String formaSustantivo(String forma, String piezaOrig) {
        return forma.equals(piezaOrig) ? "{pieza}" : forma;
    }

    private static String capitalizar(String s, boolean mayus) {
        if (!mayus || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String ingles(String frase) {
        return frase.replace("{pieza} is", "{pieza} are").replace("{pieza} has", "{pieza} have").replace("{pieza} falls", "{pieza} fall")
                .replace("{pieza} leaves", "{pieza} leave").replace("was {pieza}", "were {pieza}").replace("was my {pieza}", "were my {pieza}");
    }
}
