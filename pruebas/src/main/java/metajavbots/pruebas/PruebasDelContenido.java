package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;

/** El contenido: ejercicios (cada solución pasa sus casos), temas, diálogos y tutoriales, en los tres idiomas. */
final class PruebasDelContenido {

    private PruebasDelContenido() {
    }

    static List<String> archivosDe(String carpeta) throws IOException {
        List<String> l = new ArrayList<>();
        try (Stream<Path> s = Files.list(Path.of("res", carpeta))) {
            s.filter(p -> p.toString().endsWith(".txt")).forEach(p -> l.add(p.getFileName().toString()));
        }
        return l;
    }

    static void apuntar() {
        caso("contenido: los temas están en los tres idiomas y en orden", () -> {
            List<Tema> t = Tema.todos();
            cierto(t.size() >= 20, "hay " + t.size() + " temas");
            for (int i = 0; i < t.size(); i++) {
                igual(String.format("%02d", i + 1), t.get(i).id, "en orden");
                cierto(!t.get(i).nombre.de.isEmpty() && !t.get(i).nombre.en.isEmpty(), "tres idiomas: " + t.get(i).id);
            }
        });

        caso("ejercicios: todos los archivos están en el índice y se leen sin avisos", () -> {
            List<String> indice = Banco.indice("/ejercicios/indice.txt");
            for (String a : archivosDe("ejercicios")) {
                if (a.equals("indice.txt")) continue;
                cierto(indice.contains(a), "falta en res/ejercicios/indice.txt: " + a);
            }
            igual("[]", Banco.delJuego().avisos().toString(), "avisos");
        });

        caso("ejercicios: cada uno está bien escrito (tres idiomas, tema, casos)", () -> {
            for (Ejercicio e : Banco.delJuego().todos()) {
                String q = e.id + " (" + e.archivo + ")";
                cierto(e.id.matches("[a-z0-9_]+"), "id en minúsculas: " + q);
                cierto(Tema.de(e.tema) != null, "tema que existe: " + q);
                for (String s : new String[]{e.titulo.es, e.titulo.de, e.titulo.en, e.enunciado.es, e.enunciado.de, e.enunciado.en}) {
                    cierto(!s.isEmpty(), "título y enunciado en los tres idiomas: " + q);
                }
                cierto(e.casos.size() >= 3, "al menos tres casos: " + q);
                cierto(e.dificultad >= 1 && e.dificultad <= 5, "dificultad de 1 a 5: " + q);
                cierto(e.tipo == Ejercicio.Tipo.CLASE || !e.firma.isEmpty(), "tiene firma: " + q);
                cierto(!e.solucion.isBlank(), "tiene solución: " + q);
            }
        });

        caso("ejercicios: la solución de cada uno pasa todos sus casos y la plantilla no", () -> {
            List<String> mal = new ArrayList<>();
            int n = 0;
            for (Ejercicio e : Banco.delJuego().todos()) {
                if (e.java > Compilador.versionDeJava()) continue;
                n++;
                Comprobador.Informe bien = Comprobador.comprobar(e, e.codigoSolucion(), Locale.ROOT);
                if (!bien.todoBien()) {
                    StringBuilder b = new StringBuilder(e.id + ": la solución no pasa:");
                    for (Compilador.Fallo f : bien.errores) b.append(" [").append(f).append("]");
                    for (Comprobador.Resultado r : bien.casos) if (!r.bien) b.append(" [").append(r.caso).append(" → ").append(r.obtenido).append("]");
                    mal.add(b.toString());
                }
                Comprobador.Informe plantilla = Comprobador.comprobar(e, e.codigoInicial(), Locale.ROOT);
                if (plantilla.todoBien()) mal.add(e.id + ": la plantilla ya pasa todos los casos");
                if (e.tipo != Ejercicio.Tipo.CLASE && !plantilla.compila()) mal.add(e.id + ": la plantilla no compila " + plantilla.errores);
            }
            System.out.println("            " + n + " ejercicios comprobados");
            cierto(mal.isEmpty(), String.join("\n  ", mal));
        });

        caso("ejercicios: el comprobador corta un bucle infinito y entiende las excepciones", () -> {
            Ejercicio e = Banco.delJuego().ejercicio("muestra_sumar");
            long t0 = System.currentTimeMillis();
            Comprobador.Informe i = Comprobador.comprobar(e, "static int danoTotal(int a, int b) {\n    while (true) { }\n}", Locale.ROOT);
            cierto(System.currentTimeMillis() - t0 < 15000, "se corta a tiempo");
            falso(i.todoBien(), "no pasa");
            cierto(i.casos.get(0).obtenido.contains("pasos") || i.casos.get(0).obtenido.contains("tarda"), "dice por qué: " + i.casos.get(0).obtenido);
            Comprobador.Informe j = Comprobador.comprobar(e, "static int danoTotal(int a, int b) {\n    return a / 0;\n}", Locale.ROOT);
            cierto(j.casos.get(0).obtenido.startsWith("!ArithmeticException"), "la excepción: " + j.casos.get(0).obtenido);
            Comprobador.Informe k = Comprobador.comprobar(e, "static int danoTotal(int a, int b) {\n    return a + b\n}", Locale.ROOT);
            falso(k.compila(), "no compila");
            igual(2L, k.errores.get(0).linea, "el error, en la línea del jugador");
        });

        caso("textos: cada texto de la interfaz que usa el código está en los tres idiomas", () -> {
            List<String> faltan = new ArrayList<>();
            java.util.regex.Pattern pat = java.util.regex.Pattern.compile("T\\.[tf]\\(\"([a-z_.0-9]+)\"");
            try (Stream<Path> s = Files.walk(Path.of("juego", "src", "main", "java"))) {
                for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
                    java.util.regex.Matcher m = pat.matcher(Files.readString(f));
                    while (m.find()) {
                        String k = m.group(1);
                        if (k.endsWith(".")) continue;
                        metajavbots.textos.Trilingue t = metajavbots.textos.T.todos().get(k);
                        if (t == null || t.es.isEmpty() || t.de.isEmpty() || t.en.isEmpty()) faltan.add(k);
                    }
                }
            }
            for (String k : new String[]{"menu.pelear_ayuda", "tipo.metodo", "hueco.nucleo", "valor.dano", "cerebro.local", "resultado.victoria"}) {
                if (!metajavbots.textos.T.existe(k)) faltan.add(k);
            }
            cierto(faltan.isEmpty(), "faltan: " + new java.util.TreeSet<>(faltan));
        });

        caso("diálogos: cada personalidad y el locutor tienen todos sus eventos, en tres idiomas", () -> {
            String[] eventos = {"saludo", "inicio_ronda", "ataque", "acierto", "fallo", "golpe", "golpe_fuerte", "poco_blindaje", "parte_rota",
                    "esquiva", "victoria_ronda", "derrota_ronda", "victoria", "derrota", "burla", "pulla", "subida", "error_codigo", "espera",
                    "elogio", "reparar", "especial"};
            for (metajavbots.piezas.Modelo m : metajavbots.piezas.Modelo.todos()) {
                metajavbots.dialogos.Dialogos d = metajavbots.dialogos.Dialogos.de(m.personalidad);
                for (String e : eventos) {
                    cierto(d.frases(e).size() >= 3, m.personalidad + " sin bastantes frases de " + e);
                    for (metajavbots.textos.Trilingue t : d.frases(e)) {
                        cierto(!t.de.isEmpty() && !t.en.isEmpty(), m.personalidad + "/" + e + ": tres idiomas");
                    }
                }
            }
            metajavbots.dialogos.Dialogos l = metajavbots.dialogos.Dialogos.de("locutor");
            for (String e : new String[]{"inicio_pelea", "inicio_ronda", "primer_golpe", "golpe_fuerte", "parte_rota", "fuera_de_combate",
                    "error_codigo", "tope", "fin_ronda", "explosion_final", "victoria_jugador", "derrota_jugador", "subida_nivel"}) {
                cierto(l.frases(e).size() >= 4, "locutor sin frases de " + e);
            }
        });

        caso("tutoriales: se leen, cada lección tiene páginas y tareas, y sus ejercicios existen", () -> {
            List<metajavbots.ejercicios.Leccion> ls = metajavbots.ejercicios.Leccion.todas();
            cierto(ls.size() >= 60, "lecciones: " + ls.size());
            java.util.Set<String> temas = new java.util.HashSet<>();
            for (metajavbots.ejercicios.Leccion l : ls) {
                temas.add(l.tema);
                cierto(!l.paginas.isEmpty() && !l.tareas.isEmpty(), "páginas y tareas: " + l.id);
                cierto(!l.titulo.de.isEmpty() && !l.titulo.en.isEmpty(), "título en tres idiomas: " + l.id);
                for (metajavbots.ejercicios.Leccion.Pagina p : l.paginas) {
                    cierto(!p.texto.es.isEmpty() && !p.texto.de.isEmpty() && !p.texto.en.isEmpty(), "página: " + l.id);
                }
                for (metajavbots.ejercicios.Leccion.Tarea t : l.tareas) {
                    cierto(t.ejercicio != null || t.robot != null, "tarea sin nada: " + l.id);
                    if (t.ejercicio != null) cierto(Banco.delJuego().ejercicio(t.ejercicio) != null, "ejercicio que no existe: " + t.ejercicio);
                }
            }
            igual(22, temas.size(), "todos los temas tienen lecciones");
        });

        caso("laboratorio: la solución de cada reto pasa y caza a todos los mutantes; la plantilla no", () -> {
            List<metajavbots.ejercicios.Laboratorio> ls = metajavbots.ejercicios.Laboratorio.todos();
            igual(3, ls.size());
            for (metajavbots.ejercicios.Laboratorio l : ls) {
                metajavbots.ejercicios.Laboratorio.Resultado r = l.comprobar(l.solucion, Locale.ROOT);
                cierto(r.todoBien(), l.id + ": la solución " + r.pruebas + " cazados " + r.cazados + " " + r.errores);
                cierto(!l.comprobar(l.plantilla, Locale.ROOT).todoBien(), l.id + ": la plantilla no basta");
                boolean pieza = false;
                for (metajavbots.piezas.Pieza p : metajavbots.piezas.CatalogoDePiezas.delJuego().todas()) if (p.desbloqueo.equals("pruebas:" + l.id)) pieza = true;
                cierto(pieza, l.id + " da una pieza");
            }
        });

        caso("errores y referencia: cada error tiene sus 9 textos y la referencia cubre los 22 temas", () -> {
            List<String> lineas = Files.readAllLines(Path.of("res", "textos", "errores.txt"));
            String codigo = null;
            java.util.Map<String, Integer> cuenta = new java.util.HashMap<>();
            for (String l : lineas) {
                l = l.strip();
                if (l.startsWith("#") || l.isEmpty()) continue;
                if (l.startsWith("[")) codigo = l;
                else if (codigo != null && l.contains("=")) cuenta.merge(codigo, 1, Integer::sum);
            }
            cierto(cuenta.size() >= 60, "errores explicados: " + cuenta.size());
            for (java.util.Map.Entry<String, Integer> e : cuenta.entrySet()) igual(9, e.getValue(), "textos de " + e.getKey());
            java.util.Set<String> temas = new java.util.HashSet<>();
            for (metajavbots.pantallas.Referencia.Entrada e : metajavbots.pantallas.Referencia.todas()) temas.add(e.tema);
            cierto(temas.size() >= 22, "temas de la referencia: " + temas);
            cierto(!metajavbots.pantallas.Referencia.buscar("girar", null, metajavbots.textos.Idioma.ES).isEmpty(), "busca «girar»");
            cierto(!metajavbots.pantallas.Referencia.buscar("drehen", null, metajavbots.textos.Idioma.DE).isEmpty(), "busca «drehen»");
            cierto(!metajavbots.pantallas.Referencia.buscar("arraylist", null, metajavbots.textos.Idioma.EN).isEmpty(), "busca «arraylist»");
        });

        caso("voz: el locutor dice algo en los tres idiomas (audio no vacío y sin saturar)", () -> {
            for (metajavbots.textos.Idioma i : metajavbots.textos.Idioma.values()) {
                float[] a = metajavbots.sonido.VozRobot.decir(i.elegir("¡Bienvenidos a la arena, 42 voltios de pelea!",
                        "Willkommen in der Arena, schöne Grüße!", "Welcome to the arena, robots!"), i, 1);
                cierto(a.length > 22050 / 2, "dura algo: " + a.length);
                double energia = 0;
                for (float x : a) {
                    cierto(!Float.isNaN(x) && Math.abs(x) <= 1, "sin saturar");
                    energia += x * x;
                }
                cierto(energia / a.length > 0.005, "se oye: " + energia / a.length);
            }
            igual("cuarenta y dos", metajavbots.sonido.VozRobot.numeroEnLetras(42, metajavbots.textos.Idioma.ES));
            igual("zweiundvierzig", metajavbots.sonido.VozRobot.numeroEnLetras(42, metajavbots.textos.Idioma.DE));
        });

        caso("detector de temas y temario: reconocen herencia, interfaces, colecciones, lambdas y recursión", () -> {
            String c = "import java.util.*;\npublic class R extends Metajavbot {\n interface Plan { void hacer(); }\n"
                    + " class A implements Plan { public void hacer() { } }\n class B { }\n class C extends B { }\n"
                    + " List<Integer> l = new ArrayList<>();\n int f(int n) { return n < 1 ? 0 : f(n - 1); }\n Runnable r = () -> { };\n}\n";
            java.util.Set<String> t = metajavbots.codigo.DetectorDeTemas.temas(c);
            for (String q : new String[]{"herencia", "interfaces", "colecciones", "recursion", "lambdas"}) cierto(t.contains(q), "detecta " + q + ": " + t);
            cierto(!metajavbots.codigo.DetectorDeTemas.temas("public class R extends Metajavbot { }").contains("herencia"), "heredar de Metajavbot no cuenta");
            java.util.Set<String> tm = metajavbots.pantallas.Temario.temasDe("Kapitel 3: Schleifen. Unit 4: Arrays. Tema 6: Excepciones y herencia.");
            for (String q : new String[]{"05", "07", "14", "11"}) cierto(tm.contains(q), "temario: " + q + " en " + tm);
        });

        caso("moderación: las pullas de ring pasan; lo de odio, no", () -> {
            cierto(metajavbots.cerebro.Moderacion.aceptable("¡Te voy a dejar más plano que un disquete!"), "pulla de ring");
            falso(metajavbots.cerebro.Moderacion.aceptable("Eres un retrasado"), "nada de insultos de odio");
            falso(metajavbots.cerebro.Moderacion.aceptable("Go back home, nazi"), "nada de eso");
        });

        caso("ejercicios: los iguales comparan decimales, arrays y listas", () -> {
            cierto(Comprobador.iguales(0.1 + 0.2, 0.3), "decimales con tolerancia");
            cierto(Comprobador.iguales(new int[]{1, 2}, new int[]{1, 2}), "arrays");
            cierto(Comprobador.iguales(new int[][]{{1}, {2, 3}}, new int[][]{{1}, {2, 3}}), "arrays de arrays");
            cierto(Comprobador.iguales(List.of(1, 2), new ArrayList<>(List.of(1, 2))), "listas");
            cierto(Comprobador.iguales(3, 3L), "int y long");
            falso(Comprobador.iguales("a", "b"), "textos distintos");
        });
    }
}
