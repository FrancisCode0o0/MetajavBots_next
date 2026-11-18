package metajavbots.tutor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CONSEJOS DEL TALLER — Los tips que el entrenador flotante da mientras se
 * programa el robot: mira el código (sin compilarlo, por patrones sencillos)
 * y dice lo que le llama la atención, como un entrenador asomado por encima
 * del hombro: «tu robot nunca mira al enemigo», «ese contador vuelve a 0 en
 * cada turno», «cadaTurno ya se repite solo: ese while(true) sobra»… Cada tip
 * es un id con su texto en res/textos/ui.txt («entrenador.tip.<id>») y el
 * concepto que lo explica a fondo (para «Explícamelo»). Nunca la solución:
 * una observación y, como mucho, por dónde mirar.
 */
public final class ConsejosDelTaller {

    /** Un tip: su id (el texto está en ui.txt) y el concepto que lo explica. */
    public record Tip(String id, String concepto) {
    }

    /** Los de siempre, cuando el código no tiene nada que señalar (atajos del editor y del juego). */
    static final String[] GENERALES = {"general_autocompletar", "general_f1", "general_f2", "general_comentar", "general_probar",
        "general_pseudo", "general_nombres"};

    private static final Pattern CONTADOR_EN_TURNO = Pattern.compile(
            "void\\s+(cadaTurno|jedenZug|onTick)\\s*\\(\\s*\\)\\s*\\{[^}]*?\\b(int|double)\\s+(\\w+)\\s*=\\s*0\\s*;[^}]*?\\b\\3\\s*(\\+\\+|\\+=|=\\s*\\3\\s*\\+)",
            Pattern.DOTALL);
    private static final Pattern NUMERO = Pattern.compile("(?<![\\w.])(\\d{2,}(?:\\.\\d+)?)(?![\\w.])");
    private static final Pattern CAMPO_SIN_PRIVATE = Pattern.compile("(?m)^\\s{4}(?!private|public|protected|static|final|@|//|\\*|/)"
            + "(int|double|boolean|String|long|float)\\s+\\w+\\s*(=|;)");

    private ConsejosDelTaller() {
    }

    /** Quita comentarios y textos entre comillas (para no ver lo que no es código). */
    static String limpio(String codigo) {
        return codigo.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ").replaceAll("\"(?:[^\"\\\\\\n]|\\\\.)*\"", "\"\"");
    }

    private static boolean tiene(String c, String... palabras) {
        for (String p : palabras) if (Pattern.compile("\\b" + Pattern.quote(p) + "\\b").matcher(c).find()) return true;
        return false;
    }

    /** Lo que le llama la atención de ese código, de lo más importante a lo menos (al final, los generales). */
    public static List<Tip> de(String codigo) {
        List<Tip> l = new ArrayList<>();
        String c = limpio(codigo == null ? "" : codigo);
        if (c.isBlank()) {
            l.add(new Tip("vacio", "programa"));
            return conGenerales(l);
        }
        boolean turno = tiene(c, "cadaTurno", "jedenZug", "onTick");
        if (tiene(c, "Thread", "ExecutorService", "synchronized")) l.add(new Tip("hilos", "hilo"));
        if (turno && Pattern.compile("while\\s*\\(\\s*true\\s*\\)").matcher(c).find()) l.add(new Tip("while_true", "turno"));
        if (CONTADOR_EN_TURNO.matcher(c).find()) l.add(new Tip("contador_dentro", "ambito"));
        if (Pattern.compile("(\\.nombre\\(\\)|\\.modelo\\(\\)|\\.modell\\(\\)|\\.name\\(\\)|\\.model\\(\\))\\s*==\\s*\"").matcher(c).find()
                || Pattern.compile("\"\"\\s*==").matcher(c).find()) l.add(new Tip("string_igual", "string"));
        if (!tiene(c, "alVerEnemigo", "beiSicht", "onSeeEnemy", "enemigosVistos", "gesehen", "seenEnemies"))
            l.add(new Tip("sin_radar", "turno"));
        if (!tiene(c, "apuntar", "apuntarHacia", "apuntarA", "zielen", "zielenAuf", "zielenNach", "aim", "aimAt", "aimTo"))
            l.add(new Tip("sin_apuntar", "llamar_metodo"));
        if (turno && !Pattern.compile("@Override\\s+public\\s+void\\s+(cadaTurno|jedenZug|onTick)").matcher(c).find())
            l.add(new Tip("override", "override"));
        if (!tiene(c, "alRecibirGolpe", "beiTreffer", "onHit")) l.add(new Tip("sin_golpes", "turno"));
        if (!tiene(c, "alChocarConPared", "beiWand", "onHitWall") && tiene(c, "avanzar", "vorwaerts", "ahead"))
            l.add(new Tip("paredes", "turno"));
        if (largoDeCadaTurno(c) > 35) l.add(new Tip("metodo_largo", "metodo"));
        if (numeroRepetido(c)) l.add(new Tip("numero_magico", "constante"));
        Matcher campos = CAMPO_SIN_PRIVATE.matcher(c);
        int sinPrivate = 0;
        while (campos.find()) sinPrivate++;
        if (sinPrivate >= 2) l.add(new Tip("privado", "encapsulacion"));
        if (!tiene(c, "memoria", "gedaechtnis", "memory") && c.length() > 600) l.add(new Tip("memoria", "memoria_entre_rondas"));
        return conGenerales(l);
    }

    private static List<Tip> conGenerales(List<Tip> l) {
        String[] conceptos = {"llamar_metodo", "llamar_metodo", "pseudocodigo", "comentario", "depurar", "pseudocodigo", "variable"};
        for (int i = 0; i < GENERALES.length; i++) l.add(new Tip(GENERALES[i], conceptos[i]));
        return l;
    }

    /** Las líneas que ocupa cadaTurno (contando llaves desde su «{»). */
    static int largoDeCadaTurno(String c) {
        Matcher m = Pattern.compile("void\\s+(cadaTurno|jedenZug|onTick)\\s*\\([^)]*\\)\\s*\\{").matcher(c);
        if (!m.find()) return 0;
        int prof = 1, lineas = 0;
        for (int i = m.end(); i < c.length() && prof > 0; i++) {
            char ch = c.charAt(i);
            if (ch == '{') prof++;
            else if (ch == '}') prof--;
            else if (ch == '\n') lineas++;
        }
        return lineas;
    }

    /** ¿Hay un número de dos cifras o más repetido tres veces o más (que pide una constante con nombre)? */
    static boolean numeroRepetido(String c) {
        java.util.Map<String, Integer> veces = new java.util.HashMap<>();
        Matcher m = NUMERO.matcher(c);
        while (m.find()) if (veces.merge(m.group(1), 1, Integer::sum) >= 3) return true;
        return false;
    }
}
