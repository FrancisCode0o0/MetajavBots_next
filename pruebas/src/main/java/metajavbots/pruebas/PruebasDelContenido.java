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
