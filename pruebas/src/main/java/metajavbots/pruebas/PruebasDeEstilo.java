package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import metajavbots.piezas.Modelo;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * Pruebas de ESTILO (ENCARGO_REVISION_ESTILO): que los textos que ve el jugador no tengan signos duplicados ni
 * sobrantes, que cada idioma use sus comillas («» en español, „“ en alemán, “” en inglés) y que no quede nada sin
 * traducir. Leen todo res/ (menos el código Java de los ejemplos, las claves de búsqueda y las tablas de términos).
 */
final class PruebasDeEstilo {

    /** Un texto para el jugador: dónde está, en qué idioma y si es una línea de un bloque con sangría (pseudocódigo, tablas). */
    record Texto(String archivo, int linea, String idioma, String texto, boolean bloque) {
        @Override
        public String toString() {
            return archivo + ":" + linea + " [" + idioma + "] " + (texto.length() > 140 ? texto.substring(0, 140) + "…" : texto);
        }
    }

    /** Las carpetas y archivos de res/ con textos para el jugador. */
    private static final List<String> CARPETAS = List.of("textos", "dialogos", "biografias", "conceptos", "ejercicios", "especiales",
            "laboratorio", "tutoriales", "historia");
    private static final List<String> DATOS = List.of("api.txt", "campanas.txt", "como_se_juega.txt", "consejos.txt", "entrenadores.txt",
            "modelos.txt", "paquetes.txt", "piezas.txt", "prueba_de_nivel.txt", "referencia.txt", "robots_rotos.txt", "temas.txt");
    /** Claves que no son prosa: código, firmas, palabras de búsqueda y tablas de términos para traducir el código. */
    private static final Pattern NO_PROSA = Pattern.compile("^(claves|terminos|firma|codigo|plantilla|solucion|casos|clase|import)\\b.*");
    private static final Pattern CON_IDIOMA = Pattern.compile("^((?:[\\w\\-]+\\.)*)(es|de|en)\\s*[:=]\\s?(.*)$");
    private static final Pattern CLAVE = Pattern.compile("^([\\w.\\-\\[\\]]+)\\s*[=:]\\s?(.*)$");
    private static final Pattern HABLA = Pattern.compile("^[A-ZÑ]+:\\s*");
    private static final String[] IDIOMAS = {"es", "de", "en"};

    private PruebasDeEstilo() {
    }

    static void apuntar() {
        caso("estilo: ni signos duplicados ni sobrantes (??, ,,, ;;, .., «? ¿», ¿¿, ¡¡ sin !!) en es, de y en", () -> {
            List<String> mal = new ArrayList<>();
            // Los «¡¡…!!» de los gritos de los robots son énfasis (la RAE los admite); un «??», «,,» o un «?» suelto delante de «¿», no.
            Pattern dobles = Pattern.compile("\\?\\?|!!!!|,,|;;|¿¿|(^|\\s)\\?\\s*¿|(^|\\s)!\\s*¡|(?<![.\\w\\s])\\.\\.(?![.\\w])");
            for (Texto t : textos()) {
                String s = sinCodigo(t.texto());
                if (dobles.matcher(s).find() || cuentaDe(s, "¡¡") > cuentaDe(s, "!!")) mal.add(t.toString());
            }
            vacia(mal, "signos duplicados o sobrantes");
        });
        caso("estilo: cada idioma con sus comillas («» en español, „“ en alemán, “” en inglés)", () -> {
            List<String> mal = new ArrayList<>();
            Pattern inglesas = Pattern.compile("“[^”]*\\p{L}[^”]*”");
            for (Texto t : textos()) {
                String s = sinCodigo(t.texto());
                boolean malo = switch (t.idioma()) {
                    case "es" -> s.contains("„") || (inglesas.matcher(s).find() && !s.contains("«"));
                    case "de" -> s.contains("«") || s.contains("»") || (inglesas.matcher(s).find() && !s.contains("„"));
                    default -> s.contains("«") || s.contains("»") || s.contains("„");
                };
                if (malo) mal.add(t.toString());
            }
            vacia(mal, "comillas de otro idioma");
        });
        caso("estilo: en español, cada pregunta y exclamación con su signo de apertura", () -> {
            List<String> mal = new ArrayList<>();
            for (Texto t : textos()) {
                if (!t.idioma().equals("es") || t.bloque()) continue;
                // Fuera los operadores de Java: !x, x != y, n! (factorial), a ? b : c, <? extends T>, «!» citado.
                String s = sinCodigo(t.texto()).replaceAll("«[^»\\s]{1,3}»", "").replaceAll("(^|\\s)[!?](\\s|$)", " ").replaceAll("[(<,]\\s*\\?", "(").replaceAll("![\\w(]|[\\w)]!|!=|<\\?|\\?>", "");
                if (cuenta(s, '?') > cuenta(s, '¿') || cuenta(s, '!') > cuenta(s, '¡')) mal.add(t.toString());
            }
            vacia(mal, "preguntas o exclamaciones sin ¿ o ¡");
        });
        caso("estilo: nada sin traducir (ni alemán ni inglés iguales al español, ni ¿ ¡ fuera del español)", () -> {
            List<String> mal = new ArrayList<>();
            List<Texto> todos = textos();
            for (int i = 0; i < todos.size(); i++) {
                Texto t = todos.get(i);
                String s = sinCodigo(t.texto());
                if (!t.idioma().equals("es") && (s.contains("¿") || s.contains("¡"))) mal.add(t.toString());
                if (!t.idioma().equals("es") || !pareceFrase(t.texto())) continue;
                for (int k = i + 1; k < Math.min(todos.size(), i + 6); k++) {
                    Texto o = todos.get(k);
                    if (!o.archivo().equals(t.archivo()) || o.idioma().equals("es")) continue;
                    if (o.texto().strip().equals(t.texto().strip())) mal.add(o + "  (igual que el español)");
                }
            }
            vacia(mal, "textos sin traducir");
        });
        caso("estilo: sin dos espacios seguidos dentro de una frase", () -> {
            List<String> mal = new ArrayList<>();
            for (Texto t : textos()) {
                if (t.bloque()) continue;
                String s = sinCodigo(t.texto()).replaceAll("\\\\n +", "\\\\n").replaceAll(" +//", " //");
                // Los dibujos de texto de los ejercicios (|  ARENA  |, «  / \\») llevan sus espacios a propósito.
                if (s.contains("|") || s.contains("\"") || s.contains("#")) continue;
                if (s.strip().matches(".*\\S {2,}\\S.*")) mal.add(t.toString());
            }
            vacia(mal, "dos espacios seguidos");
        });
        caso("estilo: el botón «¿Cómo se juega?» no lleva un «?» de más delante", () -> {
            for (Idioma i : Idioma.values()) cierto(!T.en("como.boton", i).startsWith("?"), "como.boton en " + i);
            String comun = Files.readString(Path.of("juego", "src", "main", "java", "metajavbots", "pantallas", "Comun.java"), StandardCharsets.UTF_8);
            cierto(!comun.contains("\"? \" + T.t(\"como.boton\")"), "Comun.botonComoSeJuega no pone «? » delante");
        });
        caso("estilo: un robot que cambia de modelo deja de llamarse como el de antes (nada de «Cobalto · Modelo: Chispa»)", () -> {
            Modelo cobalto = Modelo.de("cobalto"), chispa = Modelo.de("chispa");
            Jugador.Robot r = new Jugador.Robot(cobalto.nombre(Idioma.ES), "cobalto");
            r.cambiarModelo(chispa, Idioma.ES);
            igual(chispa.nombre(Idioma.ES), r.nombre, "toma el nombre del modelo nuevo");
            igual("chispa", r.modelo, "y su modelo");
            Jugador.Robot propio = new Jugador.Robot("Tostadora", "cobalto");
            propio.cambiarModelo(chispa, Idioma.ES);
            igual("Tostadora", propio.nombre, "un nombre puesto por el jugador se respeta");
            Jugador.Robot viejo = new Jugador.Robot(cobalto.nombre(Idioma.DE), "chispa");
            cierto(viejo.seLlamaComoOtroModelo(), "se nota que «Kobalt» es el nombre de otro modelo");
        });
    }

    private static void vacia(List<String> mal, String que) {
        if (!mal.isEmpty()) {
            throw new AssertionError(mal.size() + " " + que + ":\n  " + String.join("\n  ", mal.subList(0, Math.min(40, mal.size()))));
        }
    }

    private static int cuentaDe(String s, String trozo) {
        int n = 0;
        for (int i = s.indexOf(trozo); i >= 0; i = s.indexOf(trozo, i + trozo.length())) n++;
        return n;
    }

    private static int cuenta(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    /** Quita lo que es código dentro de una frase: los literales "…" de Java, los huecos {…} y las marcas [[…]]. */
    static String sinCodigo(String s) {
        return s.replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"", "\"\"").replaceAll("'[^'\\s]'", "''").replaceAll("\\{[^{}]*\\}", "{}")
                .replaceAll("\\[\\[|\\]\\]", "");
    }

    /** Si es una frase de verdad (varias palabras en minúscula), no un nombre, una cifra o un trozo de código. */
    private static boolean pareceFrase(String s) {
        String sin = sinCodigo(s);
        return sin.length() > 25 && Pattern.compile("\\p{Ll}{3,} \\p{Ll}{2,} \\p{Ll}{2,} \\p{Ll}{2,}").matcher(sin).find() && !sin.contains("(") && !sin.contains(";");
    }

    /** Todos los textos de res/ para el jugador, en el orden de los archivos. */
    static List<Texto> textos() throws Exception {
        List<Path> archivos = new ArrayList<>();
        for (String c : CARPETAS) {
            try (Stream<Path> s = Files.walk(Path.of("res", c))) {
                s.filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".ink")).filter(p -> !p.toString().contains("arte"))
                        .filter(p -> !p.getFileName().toString().equals("indice.txt")).sorted().forEach(archivos::add);
            }
        }
        for (String d : DATOS) archivos.add(Path.of("res", "datos", d));
        List<Texto> out = new ArrayList<>();
        for (Path p : archivos) leer(p, out);
        return out;
    }

    private static void leer(Path p, List<Texto> out) throws Exception {
        String nombre = p.toString().replace('\\', '/');
        List<String> lineas = Files.readAllLines(p, StandardCharsets.UTF_8);
        boolean ink = nombre.endsWith(".ink");
        String idiomaInk = ink ? nombre.substring(nombre.length() - 6, nombre.length() - 4) : null;
        boolean tabla = nombre.endsWith("datos/piezas.txt") || nombre.endsWith("datos/temas.txt");
        String bloque = null;
        for (int i = 0; i < lineas.size(); i++) {
            String l = lineas.get(i);
            int n = i + 1;
            if (ink) {
                String s = l.strip();
                if (s.isEmpty() || s.startsWith("//") || s.startsWith("===") || s.startsWith("->") || s.startsWith("~") || s.startsWith("VAR")) continue;
                s = s.replaceAll("\\s#.*$", "").replaceAll("^[*+\\-]+\\s*", "");
                s = HABLA.matcher(s).replaceFirst("");
                if (!s.isBlank() && !s.startsWith("#")) out.add(new Texto(nombre, n, idiomaInk, s, false));
                continue;
            }
            if (l.isBlank() || l.stripLeading().startsWith("#") || l.stripLeading().startsWith("//")) continue;
            if (Character.isWhitespace(l.charAt(0))) {
                if (bloque != null) out.add(new Texto(nombre, n, bloque, l.strip(), true));
                continue;
            }
            bloque = null;
            if (tabla) {
                String[] c = l.split("\\|");
                int k = nombre.endsWith("piezas.txt") ? 2 : 4;
                if (c.length > k + 2) for (int j = 0; j < 3; j++) out.add(new Texto(nombre, n, IDIOMAS[j], c[k + j].strip(), false));
                continue;
            }
            Matcher m = CON_IDIOMA.matcher(l);
            if (m.matches()) {
                String clave = m.group(1);
                if (NO_PROSA.matcher(clave).matches()) continue;
                String texto = m.group(3);
                if (texto.isBlank()) {
                    bloque = m.group(2);
                    continue;
                }
                if (clave.startsWith("frases.")) {
                    for (String f : texto.split(" ; ")) out.add(new Texto(nombre, n, m.group(2), f, false));
                } else {
                    out.add(new Texto(nombre, n, m.group(2), texto, false));
                }
                continue;
            }
            Matcher c = CLAVE.matcher(l);
            if (c.matches() && !NO_PROSA.matcher(c.group(1)).matches()) {
                String[] partes = c.group(2).split(" \\| ", -1);
                if (partes.length == 3) for (int j = 0; j < 3; j++) out.add(new Texto(nombre, n, IDIOMAS[j], partes[j], false));
            }
        }
    }
}
