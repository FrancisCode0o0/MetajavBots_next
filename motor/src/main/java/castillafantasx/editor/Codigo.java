package castillafantasx.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CÓDIGO — Un archivo de texto abierto en el visor de código: sus líneas, el
 * cursor de escribir, lo que se busca y los ganchos «TODO-FRAN». Aquí está lo
 * que se puede hacer con él (moverse, buscar, escribir, guardar) y cómo se
 * colorea cada línea; lo pinta interfaz.DibujoEditor.
 */
public final class Codigo {

    /** La marca de los ganchos que Fran rellena. */
    public static final String GANCHO = "TODO-FRAN";

    public final Path archivo;
    public final List<String> lineas = new ArrayList<>();
    /** El cursor de escribir: su línea y su columna (desde 0). */
    public int linea, columna;
    /** La primera línea que se ve. */
    public int primera;
    /** Hay cambios sin guardar. */
    public boolean cambiado;
    /** Lo que se busca ("" si nada) y si se está escribiendo la búsqueda. */
    public String busqueda = "";
    public boolean escribiendoBusqueda;
    private boolean conRetorno;         // el archivo usa finales de línea de Windows (\r\n)

    public Codigo(Path archivo) throws IOException {
        this.archivo = archivo;
        String texto = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        conRetorno = texto.contains("\r\n");
        lineas.addAll(Arrays.asList(texto.replace("\r\n", "\n").split("\n", -1)));
        if (lineas.size() > 1 && lineas.get(lineas.size() - 1).isEmpty()) lineas.remove(lineas.size() - 1);
    }

    /** El nombre corto del archivo ("Ser.java"). */
    public String nombre() {
        return archivo.getFileName().toString();
    }

    // ------------------------------------------------------------------
    // Moverse
    // ------------------------------------------------------------------

    /** Lleva el cursor a esa línea (desde 0) y la deja a la vista, hacia el tercio de arriba. */
    public void irA(int cual, int filasQueCaben) {
        linea = Math.max(0, Math.min(lineas.size() - 1, cual));
        columna = Math.min(columna, lineas.get(linea).length());
        if (linea < primera || linea >= primera + filasQueCaben) primera = linea - filasQueCaben / 3;
        primera = Math.max(0, Math.min(Math.max(0, lineas.size() - filasQueCaben), primera));
    }

    /** Mueve el cursor: 'dl' líneas y 'dc' columnas. */
    public void mover(int dl, int dc, int filasQueCaben) {
        if (dc != 0) {
            columna += dc;
            if (columna < 0 && linea > 0) {
                linea--;
                columna = lineas.get(linea).length();
            } else if (columna > lineas.get(linea).length() && linea < lineas.size() - 1) {
                linea++;
                columna = 0;
            }
            columna = Math.max(0, Math.min(lineas.get(linea).length(), columna));
        }
        irA(linea + dl, filasQueCaben);
    }

    /** Sube o baja lo que se ve, sin mover el cursor más que lo justo para que siga a la vista. */
    public void desplazar(int cuantas, int filasQueCaben) {
        primera = Math.max(0, Math.min(Math.max(0, lineas.size() - filasQueCaben), primera + cuantas));
        linea = Math.max(primera, Math.min(primera + filasQueCaben - 1, linea));
        linea = Math.min(lineas.size() - 1, linea);
        columna = Math.min(columna, lineas.get(linea).length());
    }

    /** La primera línea (desde 'desde', dando la vuelta) que contiene ese texto, sin mirar mayúsculas; -1 si ninguna. */
    public int buscar(String texto, int desde, int paso) {
        if (texto.isEmpty()) return -1;
        String q = texto.toLowerCase();
        int n = lineas.size();
        for (int i = 0; i < n; i++) {
            int cual = (((desde + i * paso) % n) + n) % n;
            if (lineas.get(cual).toLowerCase().contains(q)) return cual;
        }
        return -1;
    }

    /** Salta a la siguiente línea (paso 1) o a la anterior (-1) que contiene lo buscado. Devuelve false si no hay. */
    public boolean siguiente(String texto, int paso, int filasQueCaben) {
        int cual = buscar(texto, linea + paso, paso);
        if (cual < 0) return false;
        irA(cual, filasQueCaben);
        columna = Math.max(0, lineas.get(cual).toLowerCase().indexOf(texto.toLowerCase()));
        return true;
    }

    /** Las líneas (desde 0) que llevan un gancho «TODO-FRAN». */
    public List<Integer> ganchos() {
        List<Integer> ganchos = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).contains(GANCHO)) ganchos.add(i);
        }
        return ganchos;
    }

    // ------------------------------------------------------------------
    // Escribir (cada cambio se puede deshacer: el visor lo apunta en el historial)
    // ------------------------------------------------------------------

    /** Una foto de cómo está el texto y el cursor, para deshacer. */
    public Object[] foto() {
        return new Object[] { new ArrayList<>(lineas), linea, columna, cambiado };
    }

    /** Vuelve a como estaba en esa foto. */
    @SuppressWarnings("unchecked")
    public void volverA(Object[] foto) {
        lineas.clear();
        lineas.addAll((List<String>) foto[0]);
        linea = Math.min((Integer) foto[1], lineas.size() - 1);
        columna = Math.min((Integer) foto[2], lineas.get(linea).length());
        cambiado = (Boolean) foto[3];
    }

    public void escribir(char letra) {
        String l = lineas.get(linea);
        lineas.set(linea, l.substring(0, columna) + letra + l.substring(columna));
        columna++;
        cambiado = true;
    }

    /** Intro: parte la línea; la nueva empieza con la misma sangría. */
    public void partir() {
        String l = lineas.get(linea);
        int sangria = 0;
        while (sangria < l.length() && sangria < columna && l.charAt(sangria) == ' ') sangria++;
        lineas.set(linea, l.substring(0, columna));
        lineas.add(linea + 1, l.substring(0, sangria) + l.substring(columna));
        linea++;
        columna = sangria;
        cambiado = true;
    }

    /** Retroceso: borra la letra de antes del cursor (al principio de la línea, la une con la de arriba). */
    public void borrar() {
        String l = lineas.get(linea);
        if (columna > 0) {
            lineas.set(linea, l.substring(0, columna - 1) + l.substring(columna));
            columna--;
        } else if (linea > 0) {
            String arriba = lineas.get(linea - 1);
            lineas.set(linea - 1, arriba + l);
            lineas.remove(linea);
            linea--;
            columna = arriba.length();
        } else {
            return;
        }
        cambiado = true;
    }

    /** Supr: borra la letra de bajo el cursor (al final de la línea, sube la de abajo). */
    public void suprimir() {
        String l = lineas.get(linea);
        if (columna < l.length()) {
            lineas.set(linea, l.substring(0, columna) + l.substring(columna + 1));
        } else if (linea < lineas.size() - 1) {
            lineas.set(linea, l + lineas.remove(linea + 1));
        } else {
            return;
        }
        cambiado = true;
    }

    /** Guarda el archivo, con los mismos finales de línea que tenía. */
    public void guardar() throws IOException {
        String salto = conRetorno ? "\r\n" : "\n";
        castillafantasx.datos.Candado.vigilar(archivo);
        Files.write(archivo, (String.join(salto, lineas) + salto).getBytes(StandardCharsets.UTF_8));
        cambiado = false;
    }

    // ------------------------------------------------------------------
    // Los colores
    // ------------------------------------------------------------------

    private static final Set<String> PALABRAS_DE_JAVA = new HashSet<>(Arrays.asList(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "if", "implements", "import", "instanceof", "int", "interface", "long", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static", "super", "switch", "this", "throw", "throws", "try",
            "void", "while", "true", "false", "var"));

    /**
     * Los trozos de una línea con su color: cada uno es {dónde empieza, dónde
     * acaba, color}. Sabe de Java (y sirve para los .txt y .ink: «#» y «//»
     * son comentarios): palabras del lenguaje, comentarios, ganchos, textos y números.
     */
    public static List<int[]> trozos(String l) {
        List<int[]> trozos = new ArrayList<>();
        String sinSangria = l.trim();
        boolean deBloque = sinSangria.startsWith("*") || sinSangria.startsWith("/*") || sinSangria.startsWith("#");
        int i = 0;
        while (i < l.length()) {
            char c = l.charAt(i);
            int fin;
            int color;
            if (deBloque || l.startsWith("//", i)) {
                fin = l.length();
                color = l.indexOf(GANCHO, i) >= 0 ? Estilo.codigoGancho : Estilo.codigoComentario;
            } else if (c == '"') {
                fin = i + 1;
                while (fin < l.length() && l.charAt(fin) != '"') fin += l.charAt(fin) == '\\' ? 2 : 1;
                fin = Math.min(l.length(), fin + 1);
                color = Estilo.codigoCadena;
            } else if (Character.isDigit(c)) {
                fin = i;
                while (fin < l.length() && (Character.isLetterOrDigit(l.charAt(fin)) || l.charAt(fin) == '.')) fin++;
                color = Estilo.codigoNumero;
            } else if (Character.isJavaIdentifierStart(c)) {
                fin = i;
                while (fin < l.length() && Character.isJavaIdentifierPart(l.charAt(fin))) fin++;
                color = PALABRAS_DE_JAVA.contains(l.substring(i, fin)) ? Estilo.codigoPalabra : Estilo.codigoTexto;
            } else {
                fin = i + 1;
                color = Estilo.codigoTexto;
            }
            // Los trozos seguidos del mismo color se juntan: menos que pintar.
            int[] ultimo = trozos.isEmpty() ? null : trozos.get(trozos.size() - 1);
            if (ultimo != null && ultimo[2] == color) ultimo[1] = fin;
            else trozos.add(new int[] { i, fin, color });
            i = fin;
        }
        return trozos;
    }
}
