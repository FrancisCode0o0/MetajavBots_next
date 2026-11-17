package metajavbots.codigo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LÉXICO — Parte el código Java en trozos (tokens): palabras clave, nombres,
 * números, textos, comentarios… Lo usan el coloreado del editor, la sangría
 * automática, el emparejado de llaves, el contador de pasos (Inyector), el
 * detector de temas y el revisor. Nunca falla: un texto sin cerrar o un
 * comentario sin acabar se cortan donde toca y se sigue.
 */
public final class Lexico {

    /** Las clases de trozo. */
    public enum Clase {
        CLAVE, TIPO_PRIMITIVO, NOMBRE, NUMERO, TEXTO, LETRA, BLOQUE_DE_TEXTO,
        COMENTARIO, COMENTARIO_BLOQUE, JAVADOC, ANOTACION, OPERADOR, SEPARADOR, OTRO
    }

    /** Un trozo: su clase y dónde empieza y acaba (fin, sin incluir) en el texto. */
    public static final class Trozo {
        public final Clase clase;
        public final int inicio, fin;
        public final String texto;

        Trozo(Clase clase, int inicio, int fin, String texto) {
            this.clase = clase;
            this.inicio = inicio;
            this.fin = fin;
            this.texto = texto;
        }

        public boolean es(String s) {
            return texto.equals(s);
        }

        /** ¿Es código (no comentario)? */
        public boolean esCodigo() {
            return clase != Clase.COMENTARIO && clase != Clase.COMENTARIO_BLOQUE && clase != Clase.JAVADOC;
        }

        @Override
        public String toString() {
            return clase + "«" + texto + "»";
        }
    }

    public static final Set<String> CLAVES = Collections.unmodifiableSet(new HashSet<>(List.of(
            "abstract", "assert", "break", "case", "catch", "class", "const", "continue", "default", "do",
            "else", "enum", "extends", "final", "finally", "for", "goto", "if", "implements", "import",
            "instanceof", "interface", "native", "new", "package", "private", "protected", "public", "return",
            "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "volatile", "while", "true", "false", "null",
            // Palabras con sentido solo en su sitio (Java 10+): se colorean igual.
            "var", "record", "sealed", "permits", "non-sealed", "yield")));

    public static final Set<String> PRIMITIVOS = Collections.unmodifiableSet(new HashSet<>(List.of(
            "int", "long", "short", "byte", "double", "float", "char", "boolean", "void")));

    private Lexico() {
    }

    /** Todos los trozos del texto, en orden (sin los espacios). */
    public static List<Trozo> trozos(CharSequence s) {
        List<Trozo> lista = new ArrayList<>();
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int inicio = i;
            Clase clase;
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') i++;
                clase = Clase.COMENTARIO;
            } else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                boolean javadoc = i + 2 < n && s.charAt(i + 2) == '*' && !(i + 3 < n && s.charAt(i + 3) == '/');
                i += 2;
                while (i < n && !(s.charAt(i) == '*' && i + 1 < n && s.charAt(i + 1) == '/')) i++;
                i = Math.min(n, i + 2);
                clase = javadoc ? Clase.JAVADOC : Clase.COMENTARIO_BLOQUE;
            } else if (c == '"' && i + 2 < n && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"') {
                // Bloque de texto (Java 15): de """ a """.
                i += 3;
                while (i < n) {
                    if (s.charAt(i) == '\\') {
                        i += 2;
                        continue;
                    }
                    if (s.charAt(i) == '"' && i + 2 < n && s.charAt(i + 1) == '"' && s.charAt(i + 2) == '"') {
                        i += 3;
                        break;
                    }
                    i++;
                }
                i = Math.min(i, n);
                clase = Clase.BLOQUE_DE_TEXTO;
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < n && s.charAt(i) != c && s.charAt(i) != '\n') {
                    if (s.charAt(i) == '\\') i++;
                    i++;
                }
                if (i < n && s.charAt(i) == c) i++;
                i = Math.min(i, n);
                clase = c == '"' ? Clase.TEXTO : Clase.LETRA;
            } else if (c == '@' && i + 1 < n && Character.isJavaIdentifierStart(s.charAt(i + 1))) {
                i++;
                while (i < n && (Character.isJavaIdentifierPart(s.charAt(i)) || s.charAt(i) == '.')) i++;
                clase = s.subSequence(inicio, i).toString().equals("@interface") ? Clase.CLAVE : Clase.ANOTACION;
            } else if (Character.isJavaIdentifierStart(c)) {
                while (i < n && Character.isJavaIdentifierPart(s.charAt(i))) i++;
                String palabra = s.subSequence(inicio, i).toString();
                if (palabra.equals("non") && i + 7 <= n && s.subSequence(i, i + 7).toString().equals("-sealed")) {
                    i += 7;
                    palabra = "non-sealed";
                }
                clase = PRIMITIVOS.contains(palabra) ? Clase.TIPO_PRIMITIVO
                        : CLAVES.contains(palabra) ? Clase.CLAVE : Clase.NOMBRE;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                i = finDeNumero(s, i);
                clase = Clase.NUMERO;
            } else if ("(){}[];,.".indexOf(c) >= 0) {
                i++;
                // "..." (varargs) y "::" van juntos
                if (c == '.' && i + 1 < n && s.charAt(i) == '.' && s.charAt(i + 1) == '.') i += 2;
                clase = Clase.SEPARADOR;
            } else if ("=+-*/%<>!&|^~?:".indexOf(c) >= 0) {
                i = finDeOperador(s, i);
                clase = Clase.OPERADOR;
            } else {
                i++;
                clase = Clase.OTRO;
            }
            if (i <= inicio) i = inicio + 1;     // nunca se queda quieto
            lista.add(new Trozo(clase, inicio, i, s.subSequence(inicio, i).toString()));
        }
        return lista;
    }

    private static int finDeNumero(CharSequence s, int i) {
        int n = s.length();
        if (s.charAt(i) == '0' && i + 1 < n && "xXbB".indexOf(s.charAt(i + 1)) >= 0) {
            i += 2;
            while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
            return i;
        }
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '_' || c == '.') {
                i++;
            } else if ((c == 'e' || c == 'E') && i + 1 < n) {
                i++;
                if (s.charAt(i) == '+' || s.charAt(i) == '-') i++;
            } else if ("fFdDlL".indexOf(c) >= 0) {
                i++;
                break;
            } else {
                break;
            }
        }
        return i;
    }

    private static final String[] OPERADORES_LARGOS = {
            ">>>=", "<<=", ">>=", ">>>", "->", "::", "++", "--", "&&", "||", "==", "!=", "<=", ">=",
            "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<"
    };

    private static int finDeOperador(CharSequence s, int i) {
        // ">>" no se junta: en List<List<String>> son dos cierres de genérico.
        for (String op : OPERADORES_LARGOS) {
            if (i + op.length() <= s.length() && s.subSequence(i, i + op.length()).toString().equals(op)) {
                return i + op.length();
            }
        }
        return i + 1;
    }

    /** Solo los trozos de código (sin comentarios). */
    public static List<Trozo> codigo(CharSequence s) {
        List<Trozo> todos = trozos(s);
        List<Trozo> lista = new ArrayList<>(todos.size());
        for (Trozo t : todos) if (t.esCodigo()) lista.add(t);
        return lista;
    }
}
