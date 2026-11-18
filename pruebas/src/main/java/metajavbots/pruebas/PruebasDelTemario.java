package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import metajavbots.ejercicios.LectorDeTemario;
import metajavbots.ejercicios.TemarioGuardado;

/** El temario del jugador en cualquier formato (PDF, Word, OpenDocument, PowerPoint, HTML, RTF) y guardado como campaña propia. */
final class PruebasDelTemario {

    private PruebasDelTemario() {
    }

    private static byte[] zip(String... nombreYContenido) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(o)) {
            for (int i = 0; i < nombreYContenido.length; i += 2) {
                z.putNextEntry(new ZipEntry(nombreYContenido[i]));
                z.write(nombreYContenido[i + 1].getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return o.toByteArray();
    }

    /** Un PDF mínimo con una página: su contenido, comprimido o no. */
    private static byte[] pdf(String contenido, boolean comprimido) {
        byte[] datos = contenido.getBytes(StandardCharsets.ISO_8859_1);
        String filtro = "";
        if (comprimido) {
            Deflater d = new Deflater();
            d.setInput(datos);
            d.finish();
            byte[] buf = new byte[4096];
            int n = d.deflate(buf);
            datos = java.util.Arrays.copyOf(buf, n);
            filtro = " /Filter /FlateDecode";
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        String cab = "%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /Contents 4 0 R >> endobj\n4 0 obj << /Length " + datos.length + filtro + " >>\nstream\n";
        o.writeBytes(cab.getBytes(StandardCharsets.ISO_8859_1));
        o.writeBytes(datos);
        o.writeBytes("\nendstream\nendobj\ntrailer << /Root 1 0 R >>\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1));
        return o.toByteArray();
    }

    private static String leer(Path dir, String nombre, byte[] b) throws IOException {
        Path f = dir.resolve(nombre);
        Files.write(f, b);
        return LectorDeTemario.leer(f);
    }

    static void apuntar() {
        caso("temario: se lee de PDF, Word, OpenDocument, PowerPoint, HTML, RTF y texto", () -> {
            Path dir = Files.createTempDirectory("temario");
            String docx = leer(dir, "apuntes.docx", zip("[Content_Types].xml", "<x/>", "word/document.xml",
                    "<w:document><w:body><w:p><w:r><w:t>Tema 1: bucles for</w:t></w:r></w:p><w:p><w:r><w:t>Tema 2: </w:t></w:r>"
                    + "<w:r><w:t>herencia &amp; interfaces</w:t></w:r></w:p></w:body></w:document>"));
            igual("Tema 1: bucles for\nTema 2: herencia & interfaces", docx, "Word");
            String odt = leer(dir, "apuntes.odt", zip("mimetype", "application/vnd.oasis.opendocument.text", "content.xml",
                    "<office:document-content><text:h>Streams</text:h><text:p>map y filter</text:p></office:document-content>"));
            igual("Streams\nmap y filter", odt, "OpenDocument");
            String pptx = leer(dir, "clase.pptx", zip("ppt/slides/slide2.xml", "<p:sld><a:p><a:r><a:t>Genéricos</a:t></a:r></a:p></p:sld>",
                    "ppt/slides/slide10.xml", "<p:sld><a:p><a:r><a:t>Hilos</a:t></a:r></a:p></p:sld>",
                    "ppt/slides/slide1.xml", "<p:sld><a:p><a:r><a:t>Clases</a:t></a:r></a:p></p:sld>"));
            igual("Clases\nGenéricos\nHilos", pptx, "PowerPoint, en el orden de las diapositivas");
            String html = leer(dir, "web.html", "<html><style>p{}</style><body><h1>Excepciones</h1><p>try &lt;catch&gt;</p></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            igual("Excepciones\ntry <catch>", html, "HTML");
            String rtf = leer(dir, "notas.rtf", "{\\rtf1\\ansi{\\fonttbl{\\f0 Arial;}}\\f0 Recursi\\'f3n\\par Arrays\\par}"
                    .getBytes(StandardCharsets.ISO_8859_1));
            igual("Recursión\nArrays", rtf, "RTF");
            String contenido = "BT /F1 12 Tf 72 700 Td (Tema 3: colecciones) Tj 0 -20 Td [(List, ) -250 (Set y Map)] TJ ET";
            igual("Tema 3: colecciones\nList, Set y Map", leer(dir, "plano.pdf", pdf(contenido, false)), "PDF sin comprimir");
            igual("Tema 3: colecciones\nList, Set y Map", leer(dir, "comprimido.pdf", pdf(contenido, true)), "PDF comprimido");
            igual("for (int i = 0; ...)", leer(dir, "escapes.pdf", pdf("BT (for \\(int i = 0; ...\\)) Tj ET", false)), "PDF con paréntesis escapados");
            boolean escaneado = false;
            try {
                leer(dir, "escaneado.pdf", pdf("q 100 0 0 100 0 0 cm /Im1 Do Q", false));
            } catch (IOException e) {
                escaneado = true;
            }
            cierto(escaneado, "un PDF sin texto avisa");
            igual("Variables y tipos", leer(dir, "temario.txt", "  Variables   y tipos \r\n".getBytes(StandardCharsets.UTF_8)), "texto");
        });

        caso("temario: la propuesta comprobada se guarda como campaña propia y se juega sin IA", () -> {
            Path dir = Files.createTempDirectory("partida");
            String respuesta = "Aquí tienes:\n=== tm_doble\ntema: 03\ndificultad: 1\njava: 8\ntipo: metodo\ntitulo.es: Doble\ntitulo.de: Doppelt\n"
                    + "titulo.en: Double\nenunciado.es: El doble.\nenunciado.de: Das Doppelte.\nenunciado.en: Twice.\nfirma: static int doble(int x)\n"
                    + "plantilla:\n    return 0;\nsolucion:\n    return 2 * x;\ncasos:\n    doble(1) => 2\n    doble(0) => 0\n    doble(-2) => -4\n\n"
                    + "=== tm_malo\ntema: 05\ntipo: metodo\nfirma: static int f()\nsolucion:\n    return 1;\n";
            TemarioGuardado.guardar(dir, "Programación I (2026)", List.of("03", "05"), respuesta, List.of("tm_doble"));
            List<TemarioGuardado> l = TemarioGuardado.todos(dir);
            igual(1, l.size(), "uno guardado");
            TemarioGuardado g = l.get(0);
            igual("Programación I (2026)", g.nombre, "su nombre");
            igual(List.of("03", "05"), List.copyOf(g.temas), "sus temas");
            igual(1, g.ejercicios.size(), "solo el que se comprobó bien");
            igual("tm_doble", g.ejercicios.get(0).id, "el bueno");
            cierto(g.archivo.getFileName().toString().equals("programacion_i_2026.txt"), g.archivo.toString());
            // Otra propuesta del mismo temario se añade a lo que había.
            TemarioGuardado.guardar(dir, "Programación I (2026)", List.of("07"), respuesta.replace("tm_doble", "tm_triple"), List.of("tm_triple"));
            TemarioGuardado h = TemarioGuardado.todos(dir).get(0);
            igual(2, h.ejercicios.size(), "crece: " + h.ejercicios.size());
            cierto(h.temas.containsAll(List.of("03", "05", "07")), "con los temas de las dos");
        });
    }
}
