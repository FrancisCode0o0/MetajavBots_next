package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import metajavbots.Juego;
import metajavbots.ejercicios.LectorDeTemario;
import metajavbots.ejercicios.TemarioGuardado;
import metajavbots.pantallas.Temario;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

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

    /** Un PDF mínimo con una página y fuente estándar: su contenido, comprimido o no. */
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
                + "3 0 obj << /Type /Page /Parent 2 0 R /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >>"
                + " /Contents 4 0 R >> endobj\n4 0 obj << /Length " + datos.length + filtro + " >>\nstream\n";
        o.writeBytes(cab.getBytes(StandardCharsets.ISO_8859_1));
        o.writeBytes(datos);
        o.writeBytes("\nendstream\nendobj\ntrailer << /Root 1 0 R >>\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1));
        return o.toByteArray();
    }

    /** Un PDF real con fuente TrueType embebida (Type0/Identity-H + mapa ToUnicode + FlateDecode). */
    private static byte[] pdfRealUnicode(String[] lineas, String clave) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            doc.addPage(pagina);
            PDType0Font fuente = PDType0Font.load(doc, Path.of("res", "fuentes", "DejaVuSansMono.ttf").toFile());
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                cs.beginText();
                cs.setFont(fuente, 12);
                cs.setLeading(16f);
                cs.newLineAtOffset(50, 720);
                for (String l : lineas) {
                    cs.showText(l);
                    cs.newLine();
                }
                cs.endText();
            }
            if (clave != null) {
                doc.protect(new StandardProtectionPolicy(clave, clave, new AccessPermission()));
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** Un PDF real con fuente Type1 estándar (varias páginas). */
    private static byte[] pdfRealMultipagina(String... paginas) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String texto : paginas) {
                PDPage pagina = new PDPage();
                doc.addPage(pagina);
                try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                    cs.beginText();
                    cs.setFont(fuente, 12);
                    cs.newLineAtOffset(50, 720);
                    cs.showText(texto);
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static String leer(Path dir, String nombre, byte[] b) throws IOException {
        Path f = dir.resolve(nombre);
        Files.write(f, b);
        return LectorDeTemario.leer(f);
    }

    private static String debeFallar(Path dir, String nombre, byte[] b, String motivo) throws IOException {
        Path f = dir.resolve(nombre);
        Files.write(f, b);
        try {
            String obtenido = LectorDeTemario.leer(f);
            throw new AssertionError(motivo + ": debía fallar pero devolvió «" + obtenido + "»");
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    static void apuntar() {
        apuntarFormatosValidos();
        apuntarPdfReal();
        apuntarRechazoLimpio();
        apuntarSelectorYLimpieza();
        apuntarGuardado();
    }

    private static void apuntarFormatosValidos() {
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
            igual("for (int i = 0; ...)", leer(dir, "escapes.pdf", pdf("BT /F1 12 Tf (for \\(int i = 0; ...\\)) Tj ET", false)),
                    "PDF con paréntesis escapados");
            igual("Variables y tipos", leer(dir, "temario.txt", "  Variables   y tipos \r\n".getBytes(StandardCharsets.UTF_8)), "texto UTF-8");
            igual("Übung: Schleifen y recursión", leer(dir, "ansi.txt", "Übung: Schleifen y recursión".getBytes(Charset.forName("windows-1252"))),
                    "texto Windows-1252");
            igual("Sin extensión: arrays", leer(dir, "TEMARIO", "Sin extensión: arrays".getBytes(StandardCharsets.UTF_8)), "texto sin extensión");
        });
    }

    private static void apuntarPdfReal() {
        caso("temario: PDF real con fuentes CID/Type0, mapa ToUnicode, compresión y varias páginas", () -> {
            Path dir = Files.createTempDirectory("temario_pdf");
            byte[] unicodePdf = pdfRealUnicode(new String[]{
                "PR1 Übungsbuch nach Folien",
                "Kapitel 2: Datentypen, Variablen und Schleifen (for, while)",
                "Tema 5: Herencia, colecciones y recursión — äöüß áéíóúñ"
            }, null);
            String texto = leer(dir, "PR1_Uebungsbuch_nach_Folien.pdf", unicodePdf);
            cierto(texto.contains("PR1 Übungsbuch nach Folien"), "título con diéresis: " + texto);
            cierto(texto.contains("Datentypen, Variablen und Schleifen"), "alemán: " + texto);
            cierto(texto.contains("Herencia, colecciones y recursión"), "español: " + texto);
            Set<String> temas = Temario.temasDe(texto);
            for (String id : new String[]{"02", "05", "11", "15", "21"}) {
                cierto(temas.contains(id), "reconoce tema " + id + " en " + temas);
            }
            String multi = leer(dir, "varias.pdf", pdfRealMultipagina("Unit 1: Variables", "Unit 2: Interfaces"));
            cierto(multi.contains("Unit 1: Variables") && multi.contains("Unit 2: Interfaces"), "varias páginas: " + multi);
        });
    }

    private static void apuntarRechazoLimpio() {
        caso("temario: rechaza con aviso en el idioma activo los formatos no soportados, binarios y PDFs ilegibles", () -> {
            Path dir = Files.createTempDirectory("temario_mal");
            Idioma antes = T.idioma;
            try {
                T.idioma = Idioma.ES;
                String msgEscaneado = debeFallar(dir, "escaneado.pdf", pdf("q 100 0 0 100 0 0 cm /Im1 Do Q", false), "PDF escaneado");
                cierto(msgEscaneado.contains("escaneado") || msgEscaneado.contains("extraíble"), "aviso PDF escaneado ES: " + msgEscaneado);

                String msgNoPdf = debeFallar(dir, "falso.pdf", "esto no es un pdf".getBytes(StandardCharsets.UTF_8), "falso PDF");
                cierto(msgNoPdf.contains("PDF"), "aviso falso PDF ES: " + msgNoPdf);

                String msgProtegido = debeFallar(dir, "secreto.pdf", pdfRealUnicode(new String[]{"Privado"}, "clave123"), "PDF protegido");
                cierto(msgProtegido.contains("contraseña"), "aviso PDF protegido ES: " + msgProtegido);

                byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
                String msgPng = debeFallar(dir, "foto.png", png, "formato no soportado .png");
                cierto(msgPng.contains(".png") && msgPng.contains("soportado"), "aviso .png ES: " + msgPng);

                String msgDoc = debeFallar(dir, "antiguo.doc", png, "formato no soportado .doc");
                cierto(msgDoc.contains(".doc"), "aviso .doc ES: " + msgDoc);

                String msgBinario = debeFallar(dir, "disfrazado.txt", png, "binario disfrazado de .txt");
                cierto(msgBinario.contains("binario"), "aviso binario ES: " + msgBinario);

                String msgBasura = debeFallar(dir, "basura.txt", "\uE001\uE002\uE003\uFFFD\uFFFD\uFFFD".getBytes(StandardCharsets.UTF_8), "basura Unicode");
                cierto(msgBasura.contains("binario") || msgBasura.contains("legible"), "aviso basura ES: " + msgBasura);

                T.idioma = Idioma.DE;
                String msgPngDe = debeFallar(dir, "bild.png", png, "formato no soportado DE");
                cierto(msgPngDe.contains("unterstützt"), "aviso .png DE: " + msgPngDe);
                String msgScanDe = debeFallar(dir, "scan_de.pdf", pdf("q 100 0 0 100 0 0 cm /Im1 Do Q", false), "PDF escaneado DE");
                cierto(msgScanDe.contains("gescannt") || msgScanDe.contains("Text"), "aviso escaneado DE: " + msgScanDe);

                T.idioma = Idioma.EN;
                String msgPngEn = debeFallar(dir, "image.png", png, "formato no soportado EN");
                cierto(msgPngEn.contains("unsupported"), "aviso .png EN: " + msgPngEn);
                String msgScanEn = debeFallar(dir, "scan_en.pdf", pdf("q 100 0 0 100 0 0 cm /Im1 Do Q", false), "PDF escaneado EN");
                cierto(msgScanEn.contains("scanned") || msgScanEn.contains("extractable"), "aviso escaneado EN: " + msgScanEn);
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static void apuntarSelectorYLimpieza() {
        caso("temario: pulsar Cargar abre el explorador con los formatos soportados y la ruta manual sigue funcionando", () -> {
            Path dir = Files.createTempDirectory("temario_ui");
            Path primero = dir.resolve("unidad1.txt");
            Path segundo = dir.resolve("unidad2.txt");
            Files.writeString(primero, "Tema 1: bucles for y arrays", StandardCharsets.UTF_8);
            Files.writeString(segundo, "Tema 2: herencia e interfaces", StandardCharsets.UTF_8);

            Juego juego = new Juego();
            AtomicInteger aperturas = new AtomicInteger();
            AtomicReference<List<String>> filtroRecibido = new AtomicReference<>();
            AtomicReference<Path> inicialRecibida = new AtomicReference<>();
            AtomicReference<Path> siguienteResultado = new AtomicReference<>(primero);
            juego.selectorDeArchivos = (titulo, extensiones, inicial, alElegir) -> {
                aperturas.incrementAndGet();
                filtroRecibido.set(extensiones);
                inicialRecibida.set(inicial);
                alElegir.accept(siguienteResultado.get());
            };

            Temario pantalla = new Temario();
            // 1. Campo vacío + «Cargar»: abre el selector y carga automáticamente al confirmar.
            pantalla.pulsarCargar(juego);
            pantalla.procesarArchivoPendiente(juego);
            igual(1, aperturas.get(), "se abre el explorador la primera vez");
            igual(LectorDeTemario.FORMATOS, filtroRecibido.get(), "filtra por los formatos de LectorDeTemario");
            igual(primero.toAbsolutePath().toString(), pantalla.rutaActual(), "ruta rellenada tras elegir");
            cierto(pantalla.textoActual().contains("bucles for"), "editor cargado: " + pantalla.textoActual());
            cierto(pantalla.estadoActual().contains("unidad1.txt"), "aviso de archivo cargado: " + pantalla.estadoActual());

            // 2. Pulsar «Cargar» otra vez con el mismo archivo cargado vuelve a abrir el explorador (en su carpeta).
            siguienteResultado.set(null);
            pantalla.pulsarCargar(juego);
            pantalla.procesarArchivoPendiente(juego);
            igual(2, aperturas.get(), "vuelve a abrir el explorador para elegir otro archivo");
            igual(dir.toAbsolutePath(), inicialRecibida.get().toAbsolutePath(), "abre en la carpeta del archivo actual");
            cierto(pantalla.textoActual().contains("bucles for"), "cancelar conserva el temario actual");

            // 3. Escribir una ruta nueva manualmente y pulsar «Cargar» carga ese archivo sin abrir el explorador.
            pantalla.ponerRuta(segundo.toString());
            pantalla.pulsarCargar(juego);
            igual(2, aperturas.get(), "ruta manual nueva carga directamente");
            cierto(pantalla.textoActual().contains("herencia"), "editor actualizado por ruta manual");
            cierto(pantalla.estadoActual().contains("unidad2.txt"), "aviso actualizado por ruta manual");
        });

        caso("temario: Limpiar vacía ruta, editor, estado y plan para cargar otro temario al instante", () -> {
            Path dir = Files.createTempDirectory("temario_limpiar");
            Path f1 = dir.resolve("uno.txt");
            Path f2 = dir.resolve("dos.txt");
            Files.writeString(f1, "Tema 4: bucles while y excepciones", StandardCharsets.UTF_8);
            Files.writeString(f2, "Tema 5: streams y lambdas", StandardCharsets.UTF_8);

            Temario pantalla = new Temario();
            pantalla.ponerRuta(f1.toString());
            pantalla.cargar(f1.toString());
            pantalla.prepararSinIa();
            cierto(pantalla.hayContenidoParaLimpiar(), "antes de limpiar hay contenido");
            cierto(!pantalla.temasActuales().isEmpty(), "hay temas en el plan");

            pantalla.limpiar();
            igual("", pantalla.rutaActual(), "ruta vacía tras limpiar");
            igual("", pantalla.textoActual(), "editor vacío tras limpiar");
            igual("", pantalla.estadoActual(), "estado vacío tras limpiar");
            igual(0, pantalla.temasActuales().size(), "plan de temas vacío tras limpiar");
            igual(0, pantalla.ejerciciosNuevos().size(), "lista de ejercicios vacía tras limpiar");
            cierto(!pantalla.hayContenidoParaLimpiar(), "tras limpiar no queda contenido pendiente");

            // Queda lista para cargar otro temario al instante.
            Juego juego = new Juego();
            juego.selectorDeArchivos = (titulo, extensiones, inicial, alElegir) -> alElegir.accept(f2);
            pantalla.pulsarCargar(juego);
            pantalla.procesarArchivoPendiente(juego);
            cierto(pantalla.textoActual().contains("streams"), "carga inmediata tras limpiar");
            cierto(pantalla.estadoActual().contains("dos.txt"), "estado del nuevo archivo");
        });
    }

    private static void apuntarGuardado() {
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
