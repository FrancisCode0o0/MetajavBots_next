package metajavbots.ejercicios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * LECTOR DE TEMARIO — Saca el texto del temario que da el jugador, venga en el
 * formato que venga, sin bibliotecas de fuera: texto (.txt, .md, .java, .csv),
 * Word (.docx), OpenDocument (.odt, .odp), PowerPoint (.pptx), páginas web
 * (.html) y RTF, que son zips con XML o texto con marcas; y PDF, del que se
 * leen los textos de sus páginas (los trozos entre paréntesis de Tj y TJ,
 * descomprimiendo los que van con FlateDecode). Un PDF escaneado (solo
 * imágenes) no tiene texto: se avisa.
 */
public final class LectorDeTemario {

    /** Lo más largo que se lee (un temario de 300 páginas no hace falta entero para proponer ejercicios). */
    public static final int MAXIMO = 60_000;

    private LectorDeTemario() {
    }

    /** Las extensiones que entiende. */
    public static final List<String> FORMATOS = List.of("txt", "md", "java", "csv", "docx", "odt", "odp", "pptx", "html", "htm", "rtf", "pdf");

    /** El texto del archivo (recortado a MAXIMO); lanza IOException si no se puede leer o no es de un formato que entienda. */
    public static String leer(Path archivo) throws IOException {
        String n = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        String ext = n.contains(".") ? n.substring(n.lastIndexOf('.') + 1) : "txt";
        byte[] b = Files.readAllBytes(archivo);
        String t;
        switch (ext) {
            case "docx": t = docx(b); break;
            case "odt": case "odp": t = deXml(entradas(b, "content.xml"), "text:p|text:h"); break;
            case "pptx": t = deXml(entradas(b, "ppt/slides/slide"), "a:p"); break;
            case "html": case "htm": t = html(new String(b, StandardCharsets.UTF_8)); break;
            case "rtf": t = rtf(new String(b, StandardCharsets.ISO_8859_1)); break;
            case "pdf": t = pdf(b); break;
            default: t = new String(b, StandardCharsets.UTF_8);
        }
        t = t.replace("\r", "").replaceAll("[ \\t]+", " ").replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").strip();
        if (t.isEmpty()) throw new IOException("sin texto");
        return t.length() > MAXIMO ? t.substring(0, MAXIMO) : t;
    }

    /** Word: los párrafos de word/document.xml (cada w:p, una línea; el texto, en los w:t). */
    static String docx(byte[] b) throws IOException {
        return deXml(entradas(b, "word/document.xml"), "w:p");
    }

    /** Lo de dentro de las entradas del zip cuyo nombre empieza así (en orden de nombre: slide1, slide2… con su número). */
    static String entradas(byte[] zip, String prefijo) throws IOException {
        Map<String, String> l = new TreeMap<>((a, c) -> {
            String na = a.replaceAll("\\D", ""), nc = c.replaceAll("\\D", "");
            if (!na.isEmpty() && !nc.isEmpty() && na.length() != nc.length()) return na.length() - nc.length();
            return a.compareTo(c);
        });
        try (ZipInputStream z = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().startsWith(prefijo) && e.getName().endsWith(".xml")) l.put(e.getName(), new String(z.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        if (l.isEmpty()) throw new IOException("no es un documento: falta " + prefijo);
        // Cada XML ya acaba sus párrafos con un salto: se juntan sin más.
        return String.join("", l.values());
    }

    /** De un XML: un salto de línea al cerrar cada párrafo («etiquetas», separadas por |), sin marcas y con las entidades. */
    static String deXml(String xml, String etiquetas) {
        String s = xml.replaceAll("</(" + etiquetas + ")>", "\n").replaceAll("<(w:tab|text:tab)/>", " ").replaceAll("<[^>]+>", "");
        return entidades(s);
    }

    private static String entidades(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&nbsp;", " ").replace("&amp;", "&");
    }

    static String html(String h) {
        String s = h.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ").replaceAll("(?i)<br\\s*/?>|</(p|div|h\\d|li|tr)>", "\n");
        return entidades(s.replaceAll("<[^>]+>", ""));
    }

    /** RTF: sin grupos de cabecera, sin palabras de control; \par es un salto y \'e1 una letra. */
    static String rtf(String r) {
        String s = r;
        // Los grupos de cabecera (letras, colores, estilos, info) y los «{\*…}», con todo lo que llevan dentro.
        for (String g : new String[]{"{\\fonttbl", "{\\colortbl", "{\\stylesheet", "{\\info", "{\\*"}) {
            for (int i = s.indexOf(g); i >= 0; i = s.indexOf(g)) {
                int prof = 0, k = i;
                do {
                    char ch = s.charAt(k++);
                    if (ch == '{') prof++;
                    else if (ch == '}') prof--;
                } while (prof > 0 && k < s.length());
                s = s.substring(0, i) + s.substring(k);
            }
        }
        s = s.replaceAll("\\\\par[d]?\\b", "\n");
        Matcher m = Pattern.compile("\\\\'([0-9a-fA-F]{2})").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(m.group(1), 16))));
        m.appendTail(sb);
        return sb.toString().replaceAll("\\\\[a-zA-Z]+-?\\d* ?", "").replace("{", "").replace("}", "");
    }

    // ================================================================ PDF

    /** El texto de un PDF: de cada stream (descomprimido si va con FlateDecode), lo que pintan Tj, TJ, ' y ". */
    static String pdf(byte[] b) throws IOException {
        String todo = new String(b, StandardCharsets.ISO_8859_1);
        if (!todo.startsWith("%PDF")) throw new IOException("no es un PDF");
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("(<<(?:(?!>>).)*?>>)\\s*stream\\r?\\n", Pattern.DOTALL).matcher(todo);
        int desde = 0;
        while (m.find(desde)) {
            int ini = m.end(), fin = todo.indexOf("endstream", ini);
            if (fin < 0) break;
            desde = fin;
            byte[] datos = java.util.Arrays.copyOfRange(b, ini, fin);
            String dic = m.group(1);
            if (dic.contains("/Image") || dic.contains("/FontFile")) continue;
            if (dic.contains("/FlateDecode")) datos = inflar(datos);
            if (datos == null) continue;
            textoDeContenido(new String(datos, StandardCharsets.ISO_8859_1), sb);
        }
        if (sb.toString().isBlank()) throw new IOException("el PDF no tiene texto (¿escaneado?)");
        return sb.toString();
    }

    private static byte[] inflar(byte[] d) {
        Inflater inf = new Inflater();
        inf.setInput(d);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                o.write(buf, 0, n);
            }
            return o.toByteArray();
        } catch (java.util.zip.DataFormatException e) {
            return o.size() > 0 ? o.toByteArray() : null;
        } finally {
            inf.end();
        }
    }

    /** Los textos de un contenido de página: cada BT…ET con sus (cadenas); un salto con Td, TD, T* o «'», y al acabar el bloque. */
    static void textoDeContenido(String c, StringBuilder sb) {
        if (!c.contains("BT")) return;
        int i = 0;
        boolean algo = false;
        while (i < c.length()) {
            char ch = c.charAt(i);
            if (ch == '(') {
                StringBuilder t = new StringBuilder();
                i = cadena(c, i + 1, t);
                sb.append(t);
                algo = true;
                continue;
            }
            if (algo && (c.startsWith("Td", i) || c.startsWith("TD", i) || c.startsWith("T*", i) || c.startsWith("ET", i) || ch == '\'')) {
                sb.append('\n');
                algo = false;
            } else if (ch == ']' && algo) {
                sb.append(' ');
            }
            i++;
        }
        sb.append('\n');
    }

    /** Lee una cadena literal de PDF desde «i» (tras el paréntesis), con sus escapes y paréntesis anidados; devuelve dónde acaba. */
    private static int cadena(String c, int i, StringBuilder t) {
        int prof = 1;
        while (i < c.length()) {
            char ch = c.charAt(i++);
            if (ch == '\\' && i < c.length()) {
                char e = c.charAt(i++);
                switch (e) {
                    case 'n': t.append('\n'); break;
                    case 'r': case 't': t.append(' '); break;
                    case '(': case ')': case '\\': t.append(e); break;
                    default:
                        if (e >= '0' && e <= '7') {
                            int fin = i - 1;
                            while (fin < c.length() && fin < i + 2 && c.charAt(fin) >= '0' && c.charAt(fin) <= '7') fin++;
                            t.append((char) Integer.parseInt(c.substring(i - 1, fin), 8));
                            i = fin;
                        }
                }
            } else if (ch == '(') {
                prof++;
                t.append(ch);
            } else if (ch == ')') {
                if (--prof == 0) return i;
                t.append(ch);
            } else {
                t.append(ch);
            }
        }
        return i;
    }

    /** Las extensiones, para enseñarlas al jugador («txt, md, docx…»). */
    public static String formatos() {
        List<String> l = new ArrayList<>(FORMATOS);
        l.remove("htm");
        return String.join(", ", l);
    }
}
