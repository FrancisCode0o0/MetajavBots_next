package metajavbots.codigo;

import java.util.ArrayList;
import java.util.List;

/**
 * TEXTO DE CÓDIGO — Lo que el editor sabe hacer con un texto de Java sin
 * tocarlo (salió de Documento, que pasaba de 900 líneas): normalizar los
 * saltos, la sangría de una línea, qué cierra a qué, dónde empieza y acaba
 * una palabra, la pareja de una llave y buscar. Todo estático: entra texto,
 * sale una respuesta.
 */
final class TextoDeCodigo {

    private TextoDeCodigo() {
    }

    /** Saltos de Windows y Mac a «\n», tabuladores a espacios y nada de letras de control. */
    static String normalizar(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') continue;
                b.append('\n');
            } else if (c == '\t') {
                b.append("    ");
            } else if (c == '\n' || c >= ' ' && c != 0x7f || Character.isSurrogate(c)) {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** Los espacios con que empieza una línea. */
    static int sangriaDe(String linea) {
        int i = 0;
        while (i < linea.length() && linea.charAt(i) == ' ') i++;
        return i;
    }

    /** Lo que cierra a una «(», «[», «{» o unas comillas; null si no se cierra. */
    static String cierreDe(char c) {
        switch (c) {
            case '(':
                return ")";
            case '[':
                return "]";
            case '{':
                return "}";
            case '"':
                return "\"";
            case '\'':
                return "'";
            default:
                return null;
        }
    }

    /** ¿Es «case …:» o «default:»? (tras ella, Intro sangra un poco más). */
    static boolean esEtiquetaDeCaso(String s) {
        String t = s.trim();
        return (t.startsWith("case ") || t.startsWith("default")) && t.endsWith(":");
    }

    /** La línea sin su comentario de final («x = 1; // uno» → «x = 1; »). */
    static String sinComentario(String linea) {
        for (Lexico.Trozo t : Lexico.trozos(linea)) {
            if (t.clase == Lexico.Clase.COMENTARIO) return linea.substring(0, t.inicio);
        }
        return linea;
    }

    static boolean esDePalabra(char c) {
        return Character.isJavaIdentifierPart(c) && c != '\0';
    }

    /** Adónde salta Ctrl+← (sentido -1) o Ctrl+→ (1) desde pos: al principio o al final de la palabra. */
    static int saltoDePalabra(CharSequence texto, int pos, int sentido) {
        int n = texto.length();
        if (sentido < 0) {
            int i = pos;
            while (i > 0 && texto.charAt(i - 1) == ' ') i--;
            if (i > 0 && texto.charAt(i - 1) == '\n') return i == pos ? i - 1 : i;
            if (i > 0 && esDePalabra(texto.charAt(i - 1))) {
                while (i > 0 && esDePalabra(texto.charAt(i - 1))) i--;
            } else if (i > 0) {
                i--;
            }
            return i;
        }
        int i = pos;
        if (i < n && texto.charAt(i) == '\n') return i + 1;
        if (i < n && esDePalabra(texto.charAt(i))) {
            while (i < n && esDePalabra(texto.charAt(i))) i++;
        } else if (i < n && texto.charAt(i) != ' ') {
            i++;
        }
        while (i < n && texto.charAt(i) == ' ') i++;
        return i;
    }

    /** La pareja de la llave, paréntesis o corchete que hay en pos (sin contar los de textos y comentarios); -1 si no hay. */
    static int pareja(String texto, int pos) {
        char c = pos >= 0 && pos < texto.length() ? texto.charAt(pos) : 0;
        String abre = "([{", cierra = ")]}";
        int k = abre.indexOf(c), m = cierra.indexOf(c);
        if (k < 0 && m < 0) return -1;
        List<Lexico.Trozo> codigo = Lexico.codigo(texto);
        int indice = -1;
        for (int i = 0; i < codigo.size(); i++) {
            if (codigo.get(i).inicio == pos && codigo.get(i).fin == pos + 1) {
                indice = i;
                break;
            }
        }
        if (indice < 0) return -1;
        char a = k >= 0 ? c : abre.charAt(m), b = k >= 0 ? cierra.charAt(k) : c;
        int sentido = k >= 0 ? 1 : -1;
        int nivel = 0;
        for (int i = indice; i >= 0 && i < codigo.size(); i += sentido) {
            Lexico.Trozo t = codigo.get(i);
            if (t.texto.length() != 1) continue;
            char x = t.texto.charAt(0);
            if (x == a) nivel += sentido;
            else if (x == b) nivel -= sentido;
            if (nivel == 0) return t.inicio;
        }
        return -1;
    }

    /** Busca desde pos (hacia delante o atrás, dando la vuelta); -1 si no está. */
    static int buscar(String texto, String que, int desde, boolean adelante, boolean mayusculas) {
        if (que == null || que.isEmpty()) return -1;
        String t = mayusculas ? texto : texto.toLowerCase(java.util.Locale.ROOT);
        String q = mayusculas ? que : que.toLowerCase(java.util.Locale.ROOT);
        int d = Math.max(0, Math.min(desde, t.length()));
        if (adelante) {
            int i = t.indexOf(q, d);
            return i >= 0 ? i : t.indexOf(q);
        }
        int i = d - 1 < 0 ? -1 : t.lastIndexOf(q, d - 1);
        return i >= 0 ? i : t.lastIndexOf(q);
    }

    /** Todas las apariciones (para marcarlas). */
    static List<Integer> apariciones(String texto, String que, boolean mayusculas) {
        List<Integer> lista = new ArrayList<>();
        if (que == null || que.isEmpty()) return lista;
        String t = mayusculas ? texto : texto.toLowerCase(java.util.Locale.ROOT);
        String q = mayusculas ? que : que.toLowerCase(java.util.Locale.ROOT);
        for (int i = t.indexOf(q); i >= 0 && lista.size() < 10000; i = t.indexOf(q, i + Math.max(1, q.length()))) lista.add(i);
        return lista;
    }

    /** Una línea con un nivel más de sangría (sentido 1) o uno menos (-1); las vacías no se tocan. */
    static String sangrar(String linea, int sentido, int nivel) {
        if (sentido > 0) return linea.isBlank() ? linea : " ".repeat(nivel) + linea;
        int quitar = Math.min(nivel, sangriaDe(linea));
        if (quitar == 0 || sangriaDe(linea) % nivel != 0) quitar = Math.min(quitar, Math.max(1, sangriaDe(linea) % nivel));
        return linea.substring(Math.min(quitar, linea.length()));
    }

    /** Las líneas comentadas (o descomentadas, si ya lo estaban todas) y si se quitó el comentario. */
    record Comentado(String texto, boolean quitado) {
    }

    /**
     * Ctrl+/: pone «// » a las líneas (todas a la misma columna, la de la menos sangrada) o, si ya lo llevan todas, se lo
     * quita. Las líneas vacías se quedan como están. Null si todas están vacías.
     */
    static Comentado comentar(List<String> lineas) {
        boolean todas = true;
        int menor = Integer.MAX_VALUE;
        for (String f : lineas) {
            if (f.isBlank()) continue;
            menor = Math.min(menor, sangriaDe(f));
            if (!f.trim().startsWith("//")) todas = false;
        }
        if (menor == Integer.MAX_VALUE) return null;
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < lineas.size(); k++) {
            String f = lineas.get(k);
            if (k > 0) b.append('\n');
            if (f.isBlank()) {
                b.append(f);
            } else if (todas) {
                int i = f.indexOf("//");
                int j = i + 2;
                if (j < f.length() && f.charAt(j) == ' ') j++;
                b.append(f, 0, i).append(f.substring(j));
            } else {
                b.append(f, 0, menor).append("// ").append(f.substring(menor));
            }
        }
        return new Comentado(b.toString(), todas);
    }

    /** Lo pegado con la sangría de donde cae: todas las líneas se mueven igual, y las vacías se quedan vacías. */
    static String conSangria(String pegado, String sangria) {
        String[] filas = pegado.split("\n", -1);
        int menor = Integer.MAX_VALUE;
        for (String f : filas) if (!f.isBlank()) menor = Math.min(menor, sangriaDe(f));
        if (menor == Integer.MAX_VALUE) menor = 0;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < filas.length; i++) {
            String f = filas[i];
            String sin = f.isBlank() ? "" : f.substring(Math.min(menor, sangriaDe(f)));
            if (i == 0) b.append(sin);
            else b.append('\n').append(sin.isEmpty() ? "" : sangria + sin);
        }
        return b.toString();
    }

    /** ¿Se cierra solo el paréntesis o las comillas «c» tecleados en «cursor»? No si detrás hay una palabra pegada, ni dentro de un texto. */
    static boolean debeCerrar(CharSequence texto, int cursor, char c) {
        char despues = cursor < texto.length() ? texto.charAt(cursor) : 0;
        if (Character.isLetterOrDigit(despues) || despues == '_') return false;
        if (c == '"' || c == '\'') {
            char antes = cursor > 0 ? texto.charAt(cursor - 1) : 0;
            if (Character.isLetterOrDigit(antes) || antes == c) return false;
            // Dentro de un texto no se abren comillas nuevas.
            if (dentroDeTexto(texto.toString(), cursor)) return false;
        }
        return c != '{';   // la «{» se cierra al pulsar Intro (más cómodo)
    }

    /** ¿La posición cae dentro de un texto entre comillas o de una letra entre apóstrofos? */
    static boolean dentroDeTexto(String texto, int pos) {
        int ini = pos <= 0 ? 0 : texto.lastIndexOf('\n', pos - 1) + 1;
        int fin = texto.indexOf('\n', pos);
        if (fin < 0) fin = texto.length();
        for (Lexico.Trozo t : Lexico.trozos(texto.substring(ini, fin))) {
            if (t.inicio + ini < pos && t.fin + ini > pos && (t.clase == Lexico.Clase.TEXTO || t.clase == Lexico.Clase.LETRA)) return true;
        }
        return false;
    }

    /** La palabra (nombre) en la que está pos, o "". */
    static String palabraEn(CharSequence texto, int pos) {
        int a = pos, b = pos;
        while (a > 0 && esDePalabra(texto.charAt(a - 1))) a--;
        while (b < texto.length() && esDePalabra(texto.charAt(b))) b++;
        return texto.subSequence(a, b).toString();
    }

    /** El trozo de palabra que hay justo antes de pos. */
    static String prefijo(CharSequence texto, int pos) {
        int a = pos;
        while (a > 0 && esDePalabra(texto.charAt(a - 1))) a--;
        return texto.subSequence(a, pos).toString();
    }
}
