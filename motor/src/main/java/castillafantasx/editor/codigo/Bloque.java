package castillafantasx.editor.codigo;

import java.util.List;

/**
 * BLOQUE — Hasta dónde llega el trozo de código que empieza en una línea: la
 * clase o el método hasta su llave de cierre, una constante de enum hasta su
 * coma, un «case» hasta el siguiente, un nudo de Ink hasta el siguiente nudo,
 * una sección de datos hasta la siguiente. Lo usan el visor (abrir solo esa
 * parte) y el camino del código al mundo (a quién toca una línea).
 */
public final class Bloque {

    private Bloque() { }

    /** La última línea (desde 0) del bloque que empieza en 'inicio', en un archivo con ese nombre. */
    public static int fin(List<String> lineas, int inicio, String archivo) {
        if (inicio < 0 || inicio >= lineas.size()) return inicio;
        if (archivo.endsWith(".ink")) return hastaAntesDe(lineas, inicio, "=");
        if (!archivo.endsWith(".java")) return hastaAntesDe(lineas, inicio, "[");
        String primera = lineas.get(inicio).trim();
        if (primera.startsWith("case ") || primera.startsWith("default:")) return finDelCase(lineas, inicio);
        return finDeJava(lineas, inicio);
    }

    /** Hasta antes de la siguiente línea que empieza por esa marca (en Ink, «=»; en los datos, «[»). */
    private static int hastaAntesDe(List<String> lineas, int inicio, String marca) {
        for (int i = inicio + 1; i < lineas.size(); i++) {
            if (lineas.get(i).trim().startsWith(marca)) return quitarBlancos(lineas, inicio, i - 1);
        }
        return quitarBlancos(lineas, inicio, lineas.size() - 1);
    }

    /** Sin las líneas en blanco ni los comentarios sueltos del final (los que van ya con lo siguiente). */
    private static int quitarBlancos(List<String> lineas, int inicio, int fin) {
        while (fin > inicio && (lineas.get(fin).trim().isEmpty() || lineas.get(fin).trim().startsWith("#"))) fin--;
        return fin;
    }

    /** Un «case»: hasta antes del siguiente «case» o «default» con su misma sangría, o del cierre de su switch. */
    private static int finDelCase(List<String> lineas, int inicio) {
        int sangria = sangria(lineas.get(inicio));
        for (int i = inicio + 1; i < lineas.size(); i++) {
            String l = lineas.get(i), t = l.trim();
            if (t.isEmpty()) continue;
            if (sangria(l) < sangria || sangria(l) == sangria && (t.startsWith("case ") || t.startsWith("default:"))) return quitarBlancos(lineas, inicio, i - 1);
        }
        return lineas.size() - 1;
    }

    /**
     * Java: se cuentan llaves y paréntesis (sin mirar dentro de textos ni
     * comentarios). Acaba al cerrar la última llave abierta, o en una línea
     * terminada en «,» o «;» sin nada abierto (una constante, un campo).
     */
    private static int finDeJava(List<String> lineas, int inicio) {
        int llaves = 0, parentesis = 0;
        boolean abrioLlave = false, enComentario = false;
        for (int i = inicio; i < lineas.size(); i++) {
            String l = lineas.get(i);
            boolean enTexto = false;
            char ultimo = 0;
            for (int c = 0; c < l.length(); c++) {
                char ch = l.charAt(c);
                if (enComentario) {
                    if (ch == '*' && c + 1 < l.length() && l.charAt(c + 1) == '/') {
                        enComentario = false;
                        c++;
                    }
                    continue;
                }
                if (enTexto) {
                    if (ch == '\\') c++;
                    else if (ch == '"') enTexto = false;
                    continue;
                }
                if (ch == '/' && c + 1 < l.length() && l.charAt(c + 1) == '/') break;
                if (ch == '/' && c + 1 < l.length() && l.charAt(c + 1) == '*') {
                    enComentario = true;
                    c++;
                    continue;
                }
                if (ch == '"') enTexto = true;
                else if (ch == '\'' && c + 2 < l.length()) c += l.charAt(c + 1) == '\\' ? 3 : 2;
                else if (ch == '{') {
                    llaves++;
                    abrioLlave = true;
                } else if (ch == '}') llaves--;
                else if (ch == '(') parentesis++;
                else if (ch == ')') parentesis--;
                if (!Character.isWhitespace(ch)) ultimo = ch;
            }
            if (enComentario) continue;
            if (abrioLlave && llaves <= 0) return i;
            if (!abrioLlave && llaves <= 0 && parentesis <= 0 && (ultimo == ',' || ultimo == ';')) return i;
            if (llaves < 0) return i;
        }
        return lineas.size() - 1;
    }

    /**
     * El bloque de Java que envuelve esa línea (el método o la clase en que
     * está): {primera, última}. Sirve para los ganchos, que son comentarios
     * sueltos dentro de un método. Si no hay ninguno, el archivo entero.
     */
    public static int[] envolvente(List<String> lineas, int linea) {
        int hondo = 0;
        for (int i = linea - 1; i >= 0; i--) {
            String l = sinTextos(lineas.get(i));
            for (int c = l.length() - 1; c >= 0; c--) {
                if (l.charAt(c) == '}') hondo++;
                else if (l.charAt(c) == '{' && --hondo < 0) return new int[] { i, finDeJava(lineas, i) };
            }
        }
        return new int[] { 0, lineas.size() - 1 };
    }

    /** La línea sin sus textos entre comillas ni su comentario (lo justo para contar llaves). */
    private static String sinTextos(String l) {
        String t = l.trim();
        if (t.startsWith("*") || t.startsWith("/*")) return "";
        StringBuilder limpia = new StringBuilder();
        boolean enTexto = false;
        for (int c = 0; c < l.length(); c++) {
            char ch = l.charAt(c);
            if (enTexto) {
                if (ch == '\\') c++;
                else if (ch == '"') enTexto = false;
            } else if (ch == '/' && c + 1 < l.length() && l.charAt(c + 1) == '/') {
                break;
            } else if (ch == '"') {
                enTexto = true;
            } else if (ch == '\'' && c + 2 < l.length()) {
                c += l.charAt(c + 1) == '\\' ? 3 : 2;
            } else {
                limpia.append(ch);
            }
        }
        return limpia.toString();
    }

    private static int sangria(String l) {
        int n = 0;
        while (n < l.length() && l.charAt(n) == ' ') n++;
        return n;
    }
}
