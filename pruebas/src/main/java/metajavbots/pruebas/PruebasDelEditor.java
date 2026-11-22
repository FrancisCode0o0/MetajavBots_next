package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.Random;

import metajavbots.codigo.Documento;
import metajavbots.codigo.Formateo;
import metajavbots.codigo.Lexico;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeCodigo;

/** El editor de código: que no se rompa nunca y que ayude como los editores profesionales. */
final class PruebasDelEditor {

    private PruebasDelEditor() {
    }

    /** Teclea un texto letra a letra, como el jugador. */
    static void teclear(Documento d, String s) {
        for (char c : s.toCharArray()) d.teclear(c);
    }

    static void apuntar() {
        apuntarParte1();
        apuntarParte2();
        apuntarParte3();
        apuntarParte4();
        apuntarParte5();
    }

    private static void apuntarParte1() {
        caso("editor: escribir y leer líneas", () -> {
            Documento d = new Documento("uno\ndos\r\ntres\tcuatro");
            igual(3, d.lineas());
            igual("dos", d.linea(1));
            igual("tres    cuatro", d.linea(2));
            igual(2, d.lineaDe(d.largo()));
            igual(4, d.inicioDeLinea(1));
        });

        caso("editor: paréntesis y comillas se cierran solos y se pisan", () -> {
            Documento d = new Documento();
            teclear(d, "f(");
            igual("f()", d.texto());
            teclear(d, "x)");
            igual("f(x)", d.texto());
            igual(4, d.cursor());
            teclear(d, ";\"");
            igual("f(x);\"\"", d.texto());
            teclear(d, "hola\"");
            igual("f(x);\"hola\"", d.texto());
        });

        caso("editor: retroceso entre () puestos juntos borra los dos", () -> {
            Documento d = new Documento();
            teclear(d, "g(");
            d.borrarAtras();
            igual("g", d.texto());
        });

        caso("editor: Intro tras { pone la } debajo con la sangría buena", () -> {
            Documento d = new Documento();
            teclear(d, "class A {\n");
            igual("class A {\n    \n}", d.texto());
            igual(1, d.lineaDelCursor());
            igual(4, d.columnaDelCursor());
            teclear(d, "void f() {\nx();");
            igual("class A {\n    void f() {\n        x();\n    }\n}", d.texto());
        });

        caso("editor: la } tecleada vuelve a la sangría de su {", () -> {
            Documento d = new Documento("if (a) {\n        x();\n        ");
            d.finalDelTexto(false);
            d.teclear('}');
            igual("if (a) {\n        x();\n}", d.texto());
        });

        caso("editor: retroceso en la sangría quita un nivel entero", () -> {
            Documento d = new Documento("        x");
            d.moverA(8, false);
            d.borrarAtras();
            igual("    x", d.texto());
        });
    }

    private static void apuntarParte2() {
        caso("editor: deshacer junta lo escrito seguido y rehacer lo devuelve", () -> {
            Documento d = new Documento();
            long[] hora = {1000};
            d.reloj = () -> hora[0];
            teclear(d, "int");
            hora[0] += 100;
            teclear(d, " x");
            hora[0] += 5000;
            teclear(d, " = 1;");
            igual("int x = 1;", d.texto());
            // Se deshace palabra a palabra, como en los editores profesionales.
            d.deshacer();
            igual("int x =", d.texto());
            d.deshacer();
            igual("int x", d.texto());
            d.deshacer();
            igual("int", d.texto());
            while (d.deshacer()) { }
            igual("", d.texto());
            while (d.rehacer()) { }
            igual("int x = 1;", d.texto());
        });

        caso("editor: Tab con varias líneas las mete y Mayús+Tab las saca", () -> {
            Documento d = new Documento("a\nb\nc");
            d.seleccionar(0, d.largo());
            d.tabulador(false);
            igual("    a\n    b\n    c", d.texto());
            d.tabulador(true);
            igual("a\nb\nc", d.texto());
        });

        caso("editor: Ctrl+/ comenta y descomenta", () -> {
            Documento d = new Documento("    x();\n    y();");
            d.seleccionar(0, d.largo());
            d.comentar();
            igual("    // x();\n    // y();", d.texto());
            d.seleccionar(0, d.largo());
            d.comentar();
            igual("    x();\n    y();", d.texto());
        });

        caso("editor: duplicar, borrar y mover líneas", () -> {
            Documento d = new Documento("a\nb\nc");
            d.moverA(0, false);
            d.duplicarLinea();
            igual("a\na\nb\nc", d.texto());
            d.borrarLinea();
            igual("a\nb\nc", d.texto());
            d.moverA(0, false);
            d.moverLineas(1);
            igual("b\na\nc", d.texto());
            igual(1, d.lineaDelCursor());
            d.moverLineas(-1);
            igual("a\nb\nc", d.texto());
        });

        caso("editor: pegar ajusta la sangría al sitio", () -> {
            Documento d = new Documento("void f() {\n    \n}");
            d.moverA(d.posicion(1, 4), false);
            d.pegar("if (x) {\n    y();\n}");
            igual("void f() {\n    if (x) {\n        y();\n    }\n}", d.texto());
        });
    }

    private static void apuntarParte3() {
        caso("editor: Inicio va tras la sangría y luego al principio", () -> {
            Documento d = new Documento("    abc");
            d.finalDelTexto(false);
            d.inicio(false);
            igual(4, d.cursor());
            d.inicio(false);
            igual(0, d.cursor());
        });

        caso("editor: palabras con Ctrl+flechas", () -> {
            Documento d = new Documento("int cuenta = 0;");
            d.moverA(0, false);
            d.derecha(false, true);
            igual(4, d.cursor());
            d.derecha(true, true);
            igual("cuenta ", d.seleccion());
            d.borrarPalabraAtras();
            igual("int = 0;", d.texto());
        });

        caso("editor: pareja de llaves sin contar las de los textos", () -> {
            Documento d = new Documento("f(\"(\", g(1)) { }");
            igual(11, d.parejaDe(1));
            igual(1, d.parejaDe(11));
            igual(15, d.parejaDe(13));
        });

        caso("editor: buscar y reemplazar", () -> {
            Documento d = new Documento("uno dos UNO tres");
            igual(8, d.buscar("uno", 1, true, false));
            igual(-1, d.buscar("cuatro", 0, true, false));
            igual(2, d.reemplazarTodo("uno", "1", false));
            igual("1 dos 1 tres", d.texto());
            d.deshacer();
            igual("uno dos UNO tres", d.texto());
        });

        caso("editor: formatear pone la sangría profesional", () -> {
            String feo = "public class A {\nvoid f(int x) {\nif (x > 0)\nx--;\nswitch (x) {\ncase 1:\ny();\nbreak;\ndefault:\nz();\n}\nint s = 1\n+ 2;\n}\n}";
            String bien = "public class A {\n    void f(int x) {\n        if (x > 0)\n            x--;\n        switch (x) {\n"
                    + "            case 1:\n                y();\n                break;\n            default:\n                z();\n"
                    + "        }\n        int s = 1\n                + 2;\n    }\n}";
            igual(bien, Formateo.formatear(feo));
            igual(bien, Formateo.formatear(bien));
        });

        caso("editor: formatear respeta los bloques de texto y los comentarios", () -> {
            String s = "class A {\nString t = \"\"\"\n   hola\n     mundo\n\"\"\";\n/**\n* doc\n*/\nvoid f() {}\n}";
            String f = Formateo.formatear(s);
            cierto(f.contains("\n   hola\n     mundo\n"), "el bloque de texto no se toca:\n" + f);
            cierto(f.contains("    /**\n     * doc\n     */"), "el javadoc se alinea:\n" + f);
        });

        caso("editor: el léxico nunca falla (textos sin cerrar)", () -> {
            String[] raros = {"\"sin cerrar", "/* sin acabar", "'", "\"\"\"", "@", "0x", "1e", "a->b::c", "non-sealed x", "\\u0041"};
            for (String r : raros) Lexico.trozos(r);
            Random azar = new Random(7);
            for (int i = 0; i < 300; i++) Lexico.trozos(textoAlAzar(azar, 80));
        });
    }

    private static void apuntarParte4() {
        caso("editor: miles de pulsaciones al azar, sin romperse y con deshacer exacto", () -> {
            Random azar = new Random(20261117);
            for (int vuelta = 0; vuelta < 40; vuelta++) {
                Documento d = new Documento(textoAlAzar(azar, 200));
                long[] hora = {0};
                d.reloj = () -> hora[0];
                String inicial = d.texto();
                for (int paso = 0; paso < 400; paso++) {
                    hora[0] += azar.nextInt(2000);
                    pulsacionAlAzar(d, azar);
                    cierto(d.cursor() >= 0 && d.cursor() <= d.largo(), "cursor dentro");
                    cierto(d.inicioDeSeleccion() <= d.finDeSeleccion(), "selección ordenada");
                    cierto(!d.texto().contains("\t") && !d.texto().contains("\r"), "sin tabuladores ni \\r");
                    int suma = 0;
                    for (int l = 0; l < d.lineas(); l++) suma += d.linea(l).length() + 1;
                    igual(d.largo() + 1, suma, "las líneas suman el texto");
                }
                String finalDelTexto = d.texto();
                while (d.deshacer()) { }
                // Se deshace como mucho lo que guarda (1000 cambios): con 400 pulsaciones, todo.
                igual(inicial, d.texto(), "deshacer todo vuelve al principio");
                while (d.rehacer()) { }
                igual(finalDelTexto, d.texto(), "rehacer todo vuelve al final");
            }
        });

        caso("editor: formatear no cambia ninguna letra que no sea espacio", () -> {
            Random azar = new Random(3);
            for (int i = 0; i < 300; i++) {
                String s = codigoAlAzar(azar);
                String f = Formateo.formatear(s);
                igual(sinEspacios(s), sinEspacios(f), "las letras siguen iguales");
                igual(f, Formateo.formatear(f), "formatear dos veces da lo mismo");
            }
        });
    }

    private static void apuntarParte5() {
        caso("editor: Backspace uniendo con línea larga mantiene scrollX acotado y vuelve a 0", () -> {
            VentanaDeCodigo ed = new VentanaDeCodigo();
            String larga = "int valorMuyLargoParaDesbordarElAnchoVisibleDelEditor = 1234567890 + 987654321;";
            ed.ponerTexto(larga + "\nfin();");
            float ex = 80, ey = 60, ew = 420, eh = 300, alto = 24, letra = 10;
            VistaDeCodigo.Geometria g = ed.geometria(ex, ey, ew, eh, alto, letra);
            ed.doc.moverA(ed.doc.posicion(1, 0), false);
            ed.ajustarDesplazamiento(g.zw(), g.zh(), alto, letra, true);
            igual(0f, ed.desplazamientoX(), "antes de unir líneas scrollX es 0");

            ed.doc.borrarAtras();
            ed.ajustarDesplazamiento(g.zw(), g.zh(), alto, letra, true);
            float maxX = ed.maximoDesplazamientoX(g.zw(), letra);
            cierto(maxX > 0, "la línea unida supera el ancho visible");
            cierto(ed.desplazamientoX() > 0 && ed.desplazamientoX() <= maxX, "scrollX acotado en (0, maxX]");
            float cursorX = g.zx() + ed.doc.columnaDelCursor() * letra - ed.desplazamientoX();
            cierto(cursorX >= g.clipX() && cursorX <= g.clipX() + g.clipW(), "cursor visible dentro del área de texto");
            cierto(cursorX > ex + g.anchoGutter(), "cursor estrictamente a la derecha del gutter");

            ed.doc.inicio(false);
            ed.ajustarDesplazamiento(g.zw(), g.zh(), alto, letra, true);
            igual(0f, ed.desplazamientoX(), "al volver al inicio de la línea scrollX vuelve a 0");

            ed.doc.fin(false);
            ed.ajustarDesplazamiento(g.zw(), g.zh(), alto, letra, true);
            cierto(ed.desplazamientoX() > 0, "al ir al final de la línea larga vuelve a desplazarse");
            while (ed.doc.linea(0).length() > 12) ed.doc.borrarAtras();
            ed.ajustarDesplazamiento(g.zw(), g.zh(), alto, letra, true);
            igual(0f, ed.desplazamientoX(), "al borrar el exceso hasta caber scrollX vuelve a 0");
        });

        caso("editor: recorte del área de texto nunca invade el gutter de números", () -> {
            VentanaDeCodigo ed = new VentanaDeCodigo();
            for (int n : new int[]{1, 15, 250, 1200}) {
                ed.ponerTexto("a\n".repeat(n));
                float ex = 120, ey = 40, ew = 600, eh = 400, alto = 25, letra = 11;
                VistaDeCodigo.Geometria g = ed.geometria(ex, ey, ew, eh, alto, letra);
                cierto(g.anchoGutter() > 0, "gutter con ancho positivo");
                cierto(g.clipX() >= ex + g.anchoGutter(), "clipX a la derecha del gutter con " + n + " líneas");
                cierto(g.clipX() + g.clipW() <= ex + ew - 12 + 0.01f, "clipW no invade la barra vertical");
                cierto(g.clipY() + g.clipH() <= ey + eh - 12 + 0.01f, "clipH no invade la barra horizontal");
            }
        });

        caso("editor: sustituir por texto más corto restablece scrollX a 0 en el acto", () -> {
            VentanaDeCodigo ed = new VentanaDeCodigo();
            ed.ponerTexto("a".repeat(140));
            float zw = 320, zh = 240, alto = 24, letra = 10;
            ed.doc.fin(false);
            ed.ajustarDesplazamiento(zw, zh, alto, letra, true);
            cierto(ed.desplazamientoX() > 0, "desplazado en línea larga");

            ed.doc.cambiarTodo("int x = 1;");
            ed.ajustarDesplazamiento(zw, zh, alto, letra, false);
            igual(0f, ed.desplazamientoX(), "cambiarTodo por texto corto devuelve scrollX a 0 sin recrear editor");

            ed.doc.cambiarTodo("b".repeat(140));
            ed.doc.fin(false);
            ed.ajustarDesplazamiento(zw, zh, alto, letra, true);
            cierto(ed.desplazamientoX() > 0, "desplazado de nuevo");

            ed.ponerTexto("corto();");
            ed.ajustarDesplazamiento(zw, zh, alto, letra, false);
            igual(0f, ed.desplazamientoX(), "ponerTexto corto mantiene scrollX en 0");
        });
    }

    static String sinEspacios(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static final String LETRAS = "abcxyz019 (){}[];.,=+-*/<>!&|\"'\n\n    \t@:?\\";

    static String textoAlAzar(Random azar, int largo) {
        StringBuilder b = new StringBuilder();
        int n = azar.nextInt(largo + 1);
        for (int i = 0; i < n; i++) b.append(LETRAS.charAt(azar.nextInt(LETRAS.length())));
        return b.toString();
    }

    private static final String[] PIEZAS_DE_CODIGO = {
            "class A {", "}", "void f() {", "int x = 1;", "if (x > 0) {", "} else {", "for (int i = 0; i < 3; i++)",
            "x++;", "switch (x) {", "case 1:", "default:", "break;", "// nota", "/* bloque", "*/", "String s = \"{\";",
            "return x", "+ 1;", "while (true)", "@Override", "var l = List.of(1,", "2);", "\"\"\"", "texto {", "\"\"\";",
            "case 2 -> {", "};", "char c = '}';", "/** doc", "* linea", "    ", ""
    };

    static String codigoAlAzar(Random azar) {
        StringBuilder b = new StringBuilder();
        int n = 3 + azar.nextInt(25);
        for (int i = 0; i < n; i++) {
            b.append(" ".repeat(azar.nextInt(9))).append(PIEZAS_DE_CODIGO[azar.nextInt(PIEZAS_DE_CODIGO.length)]);
            b.append(azar.nextInt(5) == 0 ? "   " : "").append('\n');
        }
        return b.toString();
    }

    static void pulsacionAlAzar(Documento d, Random azar) {
        boolean mayus = azar.nextBoolean();
        switch (azar.nextInt(30)) {
            case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                d.teclear(LETRAS.charAt(azar.nextInt(LETRAS.length())));
                break;
            case 8: d.borrarAtras(); break;
            case 9: d.borrarDelante(); break;
            case 10: d.izquierda(mayus, azar.nextBoolean()); break;
            case 11: d.derecha(mayus, azar.nextBoolean()); break;
            case 12: d.arriba(mayus); break;
            case 13: d.abajo(mayus); break;
            case 14: d.inicio(mayus); break;
            case 15: d.fin(mayus); break;
            case 16: d.pegar(textoAlAzar(azar, 30)); break;
            case 17: d.cortar(); break;
            case 18: d.duplicarLinea(); break;
            case 19: d.borrarLinea(); break;
            case 20: d.moverLineas(azar.nextBoolean() ? 1 : -1); break;
            case 21: d.comentar(); break;
            case 22: d.tabulador(mayus); break;
            case 23: d.nuevaLinea(); break;
            case 24: d.seleccionar(azar.nextInt(d.largo() + 3) - 1, azar.nextInt(d.largo() + 3) - 1); break;
            case 25: d.seleccionarPalabra(azar.nextInt(d.largo() + 1)); break;
            case 26: d.borrarPalabraAtras(); break;
            case 27: d.moverVertical(azar.nextInt(21) - 10, mayus); break;
            case 28: d.formatear(); break;
            default: d.reemplazarTodo("a", "bb", false); break;
        }
    }
}
