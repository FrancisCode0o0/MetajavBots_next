package metajavbots.codigo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCUMENTACIÓN PROPIA — Lo que el editor sabe de las clases que escribe el
 * jugador, como un IDE de verdad: al pasar el ratón por un nombre suyo
 * (un método, una clase, un campo), su declaración y el comentario /** … *&#47;
 * que tenga encima; y al escribir «algo.», los métodos y campos de la clase
 * de «algo» si es una de las suyas. Por patrones sobre el texto (sin
 * compilar), así funciona aunque el código aún tenga errores.
 */
public final class DocumentacionPropia {

    private static final String MODIFICADORES = "(?:(?:public|private|protected|static|final|abstract|default|synchronized|sealed|non-sealed)\\s+)*";
    private static final String TIPO = "[A-Za-z_][\\w.]*(?:<[^;{}()=]*?>)?(?:\\[\\])*";

    private DocumentacionPropia() {
    }

    /** El comentario /** … *&#47; que acaba justo antes de esa posición (solo con espacios y anotaciones en medio), limpio; o "". */
    static String comentarioAntes(String codigo, int pos) {
        String antes = codigo.substring(0, pos).replaceAll("(?s)(\\s*@\\w+(\\([^)]*\\))?)*\\s*$", "");
        if (!antes.endsWith("*/")) return "";
        int ini = antes.lastIndexOf("/**");
        if (ini < 0) return "";
        String c = antes.substring(ini + 3, antes.length() - 2);
        StringBuilder sb = new StringBuilder();
        for (String l : c.split("\n")) {
            String s = l.strip();
            if (s.startsWith("*")) s = s.substring(1).strip();
            if (!s.isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(s);
        }
        return sb.toString();
    }

    /**
     * Lo que se sabe de ese nombre en el código del jugador: su declaración (la primera: clase, método o campo) y
     * su comentario; null si no lo declara él.
     */
    public static String de(String codigo, String nombre) {
        if (codigo == null || nombre == null || nombre.isBlank() || !nombre.matches("[A-Za-z_]\\w*")) return null;
        String n = Pattern.quote(nombre);
        Pattern[] formas = {
            Pattern.compile("(?m)^[ \\t]*" + MODIFICADORES + "(class|interface|record|enum)\\s+" + n + "\\b[^{]*"),
            Pattern.compile("(?m)^[ \\t]*" + MODIFICADORES + "(?:<[^>]+>\\s+)?" + TIPO + "\\s+" + n + "\\s*\\([^)]*\\)"),
            Pattern.compile("(?m)^[ \\t]*" + MODIFICADORES + TIPO + "\\s+" + n + "\\s*(=[^;]*)?;"),
        };
        for (Pattern f : formas) {
            Matcher m = f.matcher(codigo);
            while (m.find()) {
                String decl = m.group().strip().replaceAll("\\s+", " ");
                if (decl.startsWith("return ") || decl.startsWith("new ") || decl.startsWith("else ")) continue;
                String com = comentarioAntes(codigo, m.start());
                return com.isEmpty() ? decl : decl + "\n\n" + com;
            }
        }
        return null;
    }

    /** El cuerpo de la clase con ese nombre en el código (lo que va entre sus llaves), o null. */
    static String cuerpoDeClase(String codigo, String clase) {
        Matcher m = Pattern.compile("\\b(class|interface|record|enum)\\s+" + Pattern.quote(clase) + "\\b[^{]*\\{").matcher(codigo);
        if (!m.find()) return null;
        int prof = 1, i = m.end();
        for (; i < codigo.length() && prof > 0; i++) {
            if (codigo.charAt(i) == '{') prof++;
            else if (codigo.charAt(i) == '}') prof--;
        }
        return codigo.substring(m.end(), Math.max(m.end(), i - 1));
    }

    /** El tipo con el que se declaró esa variable («Arma espada = …», «Arma espada;», un parámetro), o null. */
    static String tipoDe(String codigo, String variable) {
        Matcher m = Pattern.compile("(" + TIPO + ")\\s+" + Pattern.quote(variable) + "\\s*[=;,)]").matcher(codigo);
        while (m.find()) {
            String t = m.group(1);
            if (!t.equals("return") && !t.equals("new")) return t.replaceAll("<.*", "");
        }
        return null;
    }

    /**
     * Lo que se puede escribir tras «antes.»: si «antes» es una clase del jugador (o una variable de una de sus
     * clases), sus métodos y campos, en el orden en que los declara. Vacío si no es suya.
     */
    public static List<String> miembros(String codigo, String antes) {
        List<String> l = new ArrayList<>();
        if (codigo == null || antes == null || !antes.matches("[A-Za-z_]\\w*")) return l;
        String cuerpo = cuerpoDeClase(codigo, antes);
        if (cuerpo == null) {
            String tipo = tipoDe(codigo, antes);
            if (tipo != null) cuerpo = cuerpoDeClase(codigo, tipo);
        }
        if (cuerpo == null) return l;
        // Solo lo del primer nivel de la clase (no lo de dentro de sus métodos).
        StringBuilder arriba = new StringBuilder();
        int prof = 0;
        for (char ch : cuerpo.toCharArray()) {
            if (ch == '{') prof++;
            if (prof == 0) arriba.append(ch);
            if (ch == '}') prof--;
        }
        Set<String> s = new LinkedHashSet<>();
        Matcher m = Pattern.compile(MODIFICADORES + "(?:<[^>]+>\\s+)?" + TIPO + "\\s+([a-zA-Z_]\\w*)\\s*(\\(|=|;)").matcher(arriba);
        while (m.find()) if (!m.group(1).equals("class")) s.add(m.group(1));
        l.addAll(s);
        return l;
    }
}
