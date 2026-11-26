package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** Clave y trozo que debe decir con 1 y con 2, en español, alemán e inglés. */
    private static final String[][] PLURALES = {
            {"menu.marcador", "1 victoria", "2 victorias", "1 Sieg", "2 Siege", "1 win", "2 wins"},
            {"menu.experiencia", "1 punto de experiencia", "2 puntos de experiencia", "1 Erfahrungspunkt", "2 Erfahrungspunkte",
                    "1 experience point", "2 experience points"},
            {"menu.monedas", "1 moneda", "2 monedas", "1 Münze", "2 Münzen", "1 coin", "2 coins"},
            {"tienda.sin_saldo", "Te falta 1 moneda", "Te faltan 2 monedas", "Dir fehlt 1 Münze", "Dir fehlen 2 Münzen",
                    "You need 1 more coin", "You need 2 more coins"},
            {"tienda.ok", "(-1 moneda)", "(-2 monedas)", "(-1 Münze)", "(-2 Münzen)", "(-1 coin)", "(-2 coins)"},
            {"pelear.desafio_espera", "falta 1 pelea", "faltan 2 peleas", "Noch 1 Kampf", "Noch 2 Kämpfe",
                    "1 more fight", "2 more fights"},
            {"pelea.mando_ruedas", "falta 1 paso", "faltan 2 pasos", "noch 1 Schritt", "noch 2 Schritte",
                    "1 step still", "2 steps still"},
            {"pelea.errores", "1 error:", "2 errores:", "1 Fehler:", "2 Fehler:", "1 error:", "2 errors:"},
            {"pelea.pasos", "1 de 1 paso", "2 de 2 pasos", "1 von 1 Rechenschritt", "2 von 2 Rechenschritten",
                    "1 of 1 computation step", "2 of 2 computation steps"},
            {"informe.topes", "media de 1 paso", "media de 2 pasos", "im Schnitt 1 Rechenschritt",
                    "im Schnitt 2 Rechenschritte", "average 1 computation step", "average 2 computation steps"},
            {"poo.consejo_public", "Tienes 1 campo public", "Tienes 2 campos public", "Du hast 1 veränderbares public-Feld",
                    "Du hast 2 veränderbare public-Felder", "You have 1 unencapsulated mutable public field",
                    "You have 2 unencapsulated mutable public fields"},
            {"final.experiencia", "+1 punto de experiencia", "+2 puntos de experiencia", "+1 Erfahrungspunkt",
                    "+2 Erfahrungspunkte", "+1 experience point", "+2 experience points"},
            {"taller.mas_robots", "con 1 victoria", "con 2 victorias", "nach 1 Sieg", "nach 2 Siegen",
                    "with 1 win", "with 2 wins"},
            {"desbloqueo.victorias", "1 victoria", "2 victorias", "1 Sieg", "2 Siege", "1 win", "2 wins"},
            {"editor.encontrados", "1 encontrado", "2 encontrados", "1 gefunden", "2 gefunden", "1 found", "2 found"},
            {"paquetes.elementos", "1 elemento", "2 elementos", "1 Element", "2 Elemente", "1 element", "2 elements"},
            {"ejercicio.no_compila", "(1 error)", "(2 errores)", "(1 Fehler)", "(2 Fehler)", "(1 error)", "(2 errors)"},
            {"ejercicio.casi", "1 de 1 caso", "2 de 2 casos", "1 von 1 Fall", "2 von 2 Fällen",
                    "1 of 1 case", "2 of 2 cases"},
            {"objetivo.aciertos", "acierta 1 vez", "acierta 2 veces", "Triff 1-mal", "Triff 2-mal",
                    "land 1 hit", "land 2 hits"},
            {"revisor.errores", "ha fallado 1 vez", "ha fallado 2 veces", "1-mal abgestürzt", "2-mal abgestürzt",
                    "failed 1 time", "failed 2 times"},
            {"revisor.magicos", "1 número «mágico» suelto", "2 números «mágicos» sueltos", "1 „magische“ Zahl",
                    "2 „magische“ Zahlen", "1 “magic” number", "2 “magic” numbers"},
            {"revisor.metodo_largo", "mide 1 línea", "mide 2 líneas", "hat 1 Zeile", "hat 2 Zeilen",
                    "is 1 line long", "is 2 lines long"},
            {"temario.validados", "propuso 1 ejercicio", "propuso 2 ejercicios", "schlug 1 Übung vor",
                    "schlug 2 Übungen vor", "proposed 1 exercise", "proposed 2 exercises"},
            {"temario.cargado", "(1 línea)", "(2 líneas)", "(1 Zeile)", "(2 Zeilen)", "(1 line)", "(2 lines)"},
            {"temario.guardado", "1 · 1 ejercicio", "2 · 2 ejercicios", "1 · 1 Übung", "2 · 2 Übungen",
                    "1 · 1 exercise", "2 · 2 exercises"},
            {"carrera.cuenta", "1 paso, 1 valla", "2 pasos, 2 vallas", "1 Schritt, 1 Hürde", "2 Schritte, 2 Hürden",
                    "1 step, 1 hurdle", "2 steps, 2 hurdles"},
            {"carrera.pierdes", "llega 1 paso antes", "llega 2 pasos antes", "ist 1 Schritt schneller",
                    "ist 2 Schritte schneller", "is 1 step faster", "is 2 steps faster"},
            {"campanas.detalle", "1 · 1 ronda", "2 · 2 rondas", "1 · 1 Runde", "2 · 2 Runden", "1 · 1 round", "2 · 2 rounds"},
            {"demo.pasos", "1 paso", "2 pasos", "1 Schritt", "2 Schritte", "1 step", "2 steps"},
            {"carrera_obs.botin_total", "+1 moneda", "+2 monedas", "+1 Münze", "+2 Münzen", "+1 coin", "+2 coins"},
            {"carrera_obs.derrota_cuarto", "+1 moneda", "+2 monedas", "+1 Münze", "+2 Münzen", "+1 coin", "+2 coins"},
            {"destruccion.hud_botin", "Botín: 1 moneda", "Botín: 2 monedas", "Beute: 1 Münze", "Beute: 2 Münzen",
                    "Loot: 1 coin", "Loot: 2 coins"},
            {"destruccion.desglose_bajas", "Acumulado: 1 moneda", "Acumulado: 2 monedas", "Gesammelt: 1 Münze",
                    "Gesammelt: 2 Münzen", "Accumulated: 1 coin", "Accumulated: 2 coins"},
            {"destruccion.bono_supervivencia", "1 bono = +1 moneda", "2 bonos = +2 monedas", "1 Bonus = +1 Münze",
                    "2 Bonus = +2 Münzen", "1 bonus = +1 coin", "2 bonuses = +2 coins"},
            {"destruccion.derrota_cuarto", "+1 moneda", "+2 monedas", "+1 Münze", "+2 Münzen", "+1 coin", "+2 coins"},
            {"destruccion.saldo_final", "+1 moneda", "+2 monedas", "+1 Münze", "+2 Münzen", "+1 coin", "+2 coins"},
            {"revisor.senior_nulos", "Se produjo 1 NullPointerException", "Se produjeron 2 NullPointerException",
                    "Es gab 1 NullPointerException", "Es gab 2 NullPointerException", "Hit 1 NullPointerException",
                    "Hit 2 NullPointerExceptions"}
    };

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
                String s = sinCodigo(t.texto()).replaceAll("«[^»\\s]{1,3}»", "")
                        .replaceAll("(^|\\s)[!?](\\s|$)", " ").replaceAll("[(<,]\\s*\\?", "(")
                        .replaceAll("![\\w(]|[\\w)]!|!=|<\\?|\\?>", "");
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
        caso("estilo: los botones con icono llevan un solo signo (el icono o el texto), en es, de y en", () -> botonesConIcono());
        caso("estilo: claves del código y plurales en los tres idiomas", () -> pendienteDelCodigo());
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

    /** Ningún botón con «?» de icono puede llevar un texto (o una ayuda) que vuelva a poner el signo, en ningún idioma. */
    private static void botonesConIcono() throws Exception {
        for (Idioma i : Idioma.values()) cierto(!T.en("como.boton", i).startsWith("?"), "como.boton en " + i);
        List<String> mal = new ArrayList<>();
        Pattern boton = Pattern.compile("boton\\(\\s*\"([^\"]*)\"([\\s\\S]{0,400}?)\\)");
        Pattern clave = Pattern.compile("T\\.[tf]\\(\"([\\w.]+)\"");
        try (Stream<Path> archivos = Files.walk(Path.of("juego", "src", "main", "java"))) {
            for (Path p : archivos.filter(q -> q.toString().endsWith(".java")).sorted().toList()) {
                String fuente = Files.readString(p, StandardCharsets.UTF_8);
                Matcher b = boton.matcher(fuente);
                while (b.find()) {
                    String icono = b.group(1).strip();
                    if (!icono.equals("?") && !icono.startsWith("? ")) continue;
                    if (icono.length() > 1 && (icono.substring(1).contains("?") || icono.substring(1).contains("¿"))) {
                        mal.add(p + ": icono y texto con signos en «" + icono + "»");
                    }
                    Matcher c = clave.matcher(b.group(2));
                    while (c.find()) {
                        for (Idioma id : Idioma.values()) {
                            String texto = T.en(c.group(1), id);
                            if (texto.contains("?") || texto.contains("¿")) {
                                mal.add(p + ": «?» con " + c.group(1) + " en " + id + ": " + texto);
                            }
                        }
                    }
                }
            }
        }
        vacia(mal, "botones con el signo duplicado");
    }

    /** Las claves que el historial y el informe buscaban en ui.txt y no estaban, y los plurales que T.f ya resuelve. */
    private static void pendienteDelCodigo() {
        nombresDelCodigo();
        plurales();
    }

    /** modos y causas de daño del historial y del informe, traducidos en los tres idiomas. */
    private static void nombresDelCodigo() {
        igual("campaña", T.en("modo.campana", Idioma.ES), "modo.campana en es");
        igual("Kampagne", T.en("modo.campana", Idioma.DE), "modo.campana en de");
        igual("campaign", T.en("modo.campana", Idioma.EN), "modo.campana en en");
        igual("jefe", T.en("modo.jefe", Idioma.ES), "modo.jefe en es");
        igual("Boss", T.en("modo.jefe", Idioma.DE), "modo.jefe en de");
        igual("boss", T.en("modo.jefe", Idioma.EN), "modo.jefe en en");
        for (Idioma id : Idioma.values()) {
            for (String arma : ARMAS) {
                String clave = "arma_de." + arma;
                cierto(T.existe(clave), "falta " + clave);
                cierto(!T.en(clave, id).equals(clave), clave + " sin traducir en " + id);
            }
        }
    }

    private static final List<String> ARMAS = List.of("laser", "bala", "misil", "cuerpo", "llama", "choque", "pared",
            "pulso", "caida_robot", "descarga", "martillo", "sierra", "foso", "carrito");

    /** Con 1 dice el singular y con 2 el plural, en cada idioma. */
    private static void plurales() {
        for (String[] fila : PLURALES) {
            for (int k = 0; k < 3; k++) {
                Idioma id = Idioma.values()[k];
                cierto(forma(fila[0], id, 1).contains(fila[1 + k * 2]),
                        fila[0] + " en " + id + " con 1: " + forma(fila[0], id, 1));
                cierto(forma(fila[0], id, 2).contains(fila[2 + k * 2]),
                        fila[0] + " en " + id + " con 2: " + forma(fila[0], id, 2));
            }
        }
    }

    /** Rellena todos los huecos con el mismo número para poder comprobar cada marcador de plural. */
    private static String forma(String clave, Idioma id, int n) {
        Object[] huecos = new Object[10];
        Arrays.fill(huecos, n);
        String s = T.plurales(T.en(clave, id), huecos);
        for (int i = 0; i < huecos.length; i++) s = s.replace("{" + i + "}", String.valueOf(n));
        return s;
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
        return sin.length() > 25 && Pattern.compile("\\p{Ll}{3,} \\p{Ll}{2,} \\p{Ll}{2,} \\p{Ll}{2,}").matcher(sin).find()
                && !sin.contains("(") && !sin.contains(";");
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
