package metajavbots.ejercicios;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import metajavbots.textos.T;

/**
 * LECTOR DE TEMARIO — Saca el texto del temario que da el jugador, venga en el
 * formato que venga: texto (.txt, .md, .java, .csv), Word (.docx),
 * OpenDocument (.odt, .odp), PowerPoint (.pptx), páginas web (.html), RTF y
 * PDF (con Apache PDFBox para streams comprimidos, fuentes CID/Type0, mapas
 * ToUnicode y codificaciones reales). Los formatos no soportados, archivos
 * binarios o PDFs escaneados sin texto extraíble se rechazan con un aviso
 * claro en el idioma activo.
 */
public final class LectorDeTemario {

    /** Lo más largo que se lee (un temario de 300 páginas no hace falta entero para proponer ejercicios). */
    public static final int MAXIMO = 60_000;

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private LectorDeTemario() {
    }

    /** Las extensiones que entiende. */
    public static final List<String> FORMATOS = List.of("txt", "md", "java", "csv", "docx", "odt", "odp", "pptx", "html", "htm", "rtf", "pdf");

    /** El texto del archivo (recortado a MAXIMO); lanza IOException con aviso en el idioma activo si no se puede leer o no es válido. */
    public static String leer(Path archivo) throws IOException {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new IOException(T.t("temario.error_no_existe"));
        }
        String n = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        int punto = n.lastIndexOf('.');
        String ext = punto > 0 && punto < n.length() - 1 ? n.substring(punto + 1) : "";
        byte[] b = Files.readAllBytes(archivo);
        if (b.length == 0) {
            throw new IOException(T.t("temario.error_sin_texto"));
        }
        String t = extraer(b, ext);
        t = t.replace("\r", "").replace('\f', '\n').replace('\u00A0', ' ').replace("\uFEFF", "").replace("\u00AD", "");
        t = t.replaceAll("[ \\t]+", " ").replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").strip();
        if (t.isEmpty()) {
            throw new IOException(T.t("temario.error_sin_texto"));
        }
        if (!esTextoLegible(t)) {
            throw new IOException("pdf".equals(ext) ? T.t("temario.error_pdf_ilegible") : T.t("temario.error_binario"));
        }
        return t.length() > MAXIMO ? t.substring(0, MAXIMO) : t;
    }

    private static String extraer(byte[] b, String ext) throws IOException {
        switch (ext) {
            case "docx": return docx(b);
            case "odt": case "odp": return deXml(entradas(b, "content.xml"), "text:p|text:h");
            case "pptx": return deXml(entradas(b, "ppt/slides/slide"), "a:p");
            case "html": case "htm": return html(decodificarTexto(b));
            case "rtf": return rtfDe(b);
            case "pdf": return pdf(b);
            case "": case "txt": case "md": case "java": case "csv": return decodificarTexto(b);
            default: throw new IOException(T.f("temario.error_formato", "." + ext, formatos()));
        }
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
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().startsWith(prefijo) && e.getName().endsWith(".xml")) {
                    l.put(e.getName(), new String(z.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }
        if (l.isEmpty()) throw new IOException(T.f("temario.error_documento", prefijo));
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

    private static String rtfDe(byte[] b) throws IOException {
        if (esBinario(b)) throw new IOException(T.t("temario.error_binario"));
        String crudo = new String(b, StandardCharsets.ISO_8859_1);
        if (!crudo.stripLeading().startsWith("{\\rtf")) {
            throw new IOException(T.f("temario.error_documento", "{\\rtf"));
        }
        return rtf(crudo);
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

    /** El texto de un PDF real (streams comprimidos, fuentes CID/Type0, mapas ToUnicode y orden de lectura) con Apache PDFBox. */
    static String pdf(byte[] b) throws IOException {
        if (!esPdf(b)) throw new IOException(T.t("temario.error_pdf_invalido"));
        String extraido;
        try (PDDocument doc = Loader.loadPDF(b)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            StringBuilder sb = new StringBuilder();
            int paginas = doc.getNumberOfPages();
            for (int p = 1; p <= paginas && sb.length() <= MAXIMO + 2000; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String trozo = stripper.getText(doc);
                if (trozo != null && !trozo.isBlank()) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(trozo);
                }
            }
            extraido = sb.toString();
        } catch (InvalidPasswordException e) {
            throw new IOException(T.t("temario.error_pdf_protegido"), e);
        } catch (IOException e) {
            throw new IOException(T.t("temario.error_pdf_invalido"), e);
        }
        if (extraido.isBlank()) throw new IOException(T.t("temario.error_pdf_sin_texto"));
        if (!esTextoLegible(extraido)) throw new IOException(T.t("temario.error_pdf_ilegible"));
        return extraido;
    }

    private static boolean esPdf(byte[] b) {
        int limite = Math.min(b.length, 1024);
        String cabecera = new String(b, 0, limite, StandardCharsets.ISO_8859_1);
        return cabecera.contains("%PDF-");
    }

    // ================================================================ TEXTO Y LEGIBILIDAD

    /** Decodifica texto plano (UTF-8 con o sin BOM, UTF-16 con BOM o Windows-1252) y rechaza archivos binarios. */
    static String decodificarTexto(byte[] b) throws IOException {
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        }
        int inicio = (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) ? 3 : 0;
        if (esBinario(b, inicio)) throw new IOException(T.t("temario.error_binario"));
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b, inicio, b.length - inicio))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(b, inicio, b.length - inicio, WINDOWS_1252);
        }
    }

    static boolean esBinario(byte[] b) {
        return esBinario(b, 0);
    }

    private static boolean esBinario(byte[] b, int inicio) {
        int limite = Math.min(b.length, inicio + 8192);
        int muestra = limite - inicio;
        if (muestra <= 0) return false;
        int controles = 0;
        for (int i = inicio; i < limite; i++) {
            int v = b[i] & 0xFF;
            if (v == 0) return true;
            if ((v < 0x20 && v != '\t' && v != '\n' && v != '\r' && v != '\f') || v == 0x7F) controles++;
        }
        return controles > 0 && (double) controles / muestra > 0.05;
    }

    /** Comprueba que el texto extraído sea legible (no caracteres de control, reemplazo U+FFFD ni basura de fuentes sin Unicode). */
    static boolean esTextoLegible(String t) {
        int total = 0, alfanum = 0, legibles = 0, corruptos = 0;
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isWhitespace(cp)) continue;
            total++;
            int tipo = Character.getType(cp);
            if (cp == 0xFFFD || Character.isISOControl(cp) || tipo == Character.PRIVATE_USE
                    || tipo == Character.UNASSIGNED || tipo == Character.SURROGATE) {
                corruptos++;
            } else if (Character.isLetterOrDigit(cp)) {
                alfanum++;
                legibles++;
            } else if (esPuntuacionOSimbolo(cp, tipo)) {
                legibles++;
            }
        }
        if (total == 0 || alfanum == 0) return false;
        if (corruptos > 0 && (double) corruptos / total > 0.15) return false;
        return total < 20 || (double) legibles / total >= 0.50;
    }

    private static boolean esPuntuacionOSimbolo(int cp, int tipo) {
        if (cp >= 0x21 && cp <= 0x7E) return true;
        return tipo == Character.DASH_PUNCTUATION || tipo == Character.START_PUNCTUATION || tipo == Character.END_PUNCTUATION
                || tipo == Character.CONNECTOR_PUNCTUATION || tipo == Character.OTHER_PUNCTUATION
                || tipo == Character.INITIAL_QUOTE_PUNCTUATION || tipo == Character.FINAL_QUOTE_PUNCTUATION
                || tipo == Character.CURRENCY_SYMBOL || tipo == Character.MATH_SYMBOL;
    }

    /** Las extensiones, para enseñarlas al jugador («txt, md, docx…»). */
    public static String formatos() {
        List<String> l = new ArrayList<>(FORMATOS);
        l.remove("htm");
        return String.join(", ", l);
    }
}
