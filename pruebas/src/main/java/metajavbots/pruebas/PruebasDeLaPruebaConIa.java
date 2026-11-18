package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.List;
import java.util.Random;

import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.GeneradorDePreguntas;
import metajavbots.progreso.PreguntasDeLaIa;
import metajavbots.progreso.PruebaDeNivel;

/** La prueba de nivel con preguntas que inventa la IA, comprobadas antes: la buena es la que escribe el código de verdad. */
final class PruebasDeLaPruebaConIa {

    private PruebasDeLaPruebaConIa() {
    }

    static void apuntar() {
        caso("prueba de nivel con IA: el juego ejecuta cada pregunta y se queda la salida de verdad (y tira las malas)", () -> {
            if (!metajavbots.codigo.Compilador.hayCompilador()) return;
            String respuesta = "Claro, aquí van:\n"
                    + "=== pregunta\nnivel: JUNIOR_2\narriba:\n    static int f(int x) { return x * 2; }\ncuerpo:\n    System.out.println(f(3) + 1);\n"
                    + "malas: 6 | 8 | 4\n\n"
                    // La IA cree que 7 / 2 da 3.5: la buena la pone el juego (3), y 3 no puede estar entre las malas.
                    + "=== pregunta\nnivel: NOOB_2\narriba:\ncuerpo:\n    int a = 7;\n    System.out.println(a / 2);\nmalas: 3 | 4 | 2\n\n"
                    + "=== pregunta\nnivel: NOOB_2\ncuerpo:\n    int a = 7;\n    System.out.println(a / 2);\nmalas: 3.5 | 4 | 2\n\n"
                    + "=== pregunta\nnivel: SENIOR_1\ncuerpo:\n    System.out.println(noExiste());\nmalas: 1 | 2 | 3\n\n"
                    + "=== pregunta\nnivel: SENIOR_1\ncuerpo:\n    System.out.println(\"a\");\n    System.out.println(\"b\");\nmalas: x | y | z\n\n"
                    + "=== pregunta\nnivel: SENIOR_2\ncuerpo:\n    var l = java.util.List.of(\"Bo\", \"Ana\", \"Cris\");\n"
                    + "    System.out.println(l.stream().filter(s -> s.length() > 2).map(String::toUpperCase).toList());\n"
                    + "malas: [ANA] | [Bo, Ana, Cris] | [CRIS]\n";
            List<GeneradorDePreguntas.Generada> l = PreguntasDeLaIa.comprobar(respuesta);
            igual(3, l.size(), "se quedan tres (una con malas que no valen, una que no compila, una de dos líneas: fuera)");
            igual("7", l.get(0).respuesta, "f(3) + 1");
            igual("3", l.get(1).respuesta, "la buena la pone el juego, no la IA");
            cierto(!l.get(1).malas.contains("3"), "la buena no está entre las malas");
            igual("[ANA, CRIS]", l.get(2).respuesta, "streams");
            igual(Etiqueta.SENIOR_2, l.get(2).nivel, "su nivel");
            // Entran en el montón de su escalón.
            boolean sale = false;
            for (int s = 0; s < 40 && !sale; s++) {
                for (PruebaDeNivel.Pregunta q : PruebaDeNivel.nueva(new Random(s), l)) if (q.codigo.contains("toUpperCase")) sale = true;
            }
            cierto(sale, "las de la IA salen en la prueba");
        });
    }
}
