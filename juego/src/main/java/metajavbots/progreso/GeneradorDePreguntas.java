package metajavbots.progreso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import metajavbots.textos.Trilingue;

/**
 * GENERADOR DE PREGUNTAS — Preguntas de «¿Qué escribe este código?» con
 * números, nombres y límites al azar, para que la prueba de nivel no salga
 * nunca igual. El juego calcula la respuesta buena (y tres malas creíbles: los
 * despistes típicos, como contar una vuelta de más). Las pruebas compilan y
 * ejecutan cada plantilla con muchos azares y comprueban que el código
 * escribe de verdad la respuesta que da por buena.
 */
public final class GeneradorDePreguntas {

    /** Una pregunta generada: el código (métodos y clases arriba, sentencias abajo) y lo que escribe. */
    public static final class Generada {
        public final Etiqueta nivel;
        /** Lo que va fuera del método que se ejecuta (métodos static); vacío si no hace falta. */
        public final String arriba;
        /** Las sentencias que se ejecutan. */
        public final String cuerpo;
        public final String respuesta;
        public final List<String> malas;

        Generada(Etiqueta nivel, String arriba, String cuerpo, String respuesta, List<String> malas) {
            this.nivel = nivel;
            this.arriba = arriba;
            this.cuerpo = cuerpo;
            this.respuesta = respuesta;
            this.malas = malas;
        }

        /** El código tal como lo ve el jugador. */
        public String codigo() {
            return arriba.isEmpty() ? cuerpo : arriba + "\n\n" + cuerpo;
        }

        /** Como pregunta de la prueba de nivel, con las cuatro opciones en un orden al azar. */
        public PruebaDeNivel.Pregunta comoPregunta(Random r) {
            List<String> opciones = new ArrayList<>(malas);
            opciones.add(respuesta);
            Collections.shuffle(opciones, r);
            List<Trilingue> t = new ArrayList<>();
            for (String o : opciones) t.add(new Trilingue(o, o, o));
            return new PruebaDeNivel.Pregunta(nivel, QUE_ESCRIBE, codigo(), t, opciones.indexOf(respuesta));
        }
    }

    static final Trilingue QUE_ESCRIBE = new Trilingue("¿Qué escribe este código?", "Was gibt dieser Code aus?", "What does this code print?");
    private static final String[] NOMBRES = {"Chispa", "Tuerca", "Voltio", "Perno", "Rayo", "Bobina", "Cobalto", "Pistón"};
    private static final String[] PALABRAS = {"ROBOT", "LASER", "TURBO", "RADAR", "MISIL", "CHASIS", "MOTOR"};

    /** Las plantillas de cada escalón. */
    private static final List<Function<Random, Generada>> NOOB_2 = List.of(
            GeneradorDePreguntas::suma, GeneradorDePreguntas::division, GeneradorDePreguntas::resto, GeneradorDePreguntas::concatenar);
    private static final List<Function<Random, Generada>> NOOB_4 = List.of(
            GeneradorDePreguntas::siNo, GeneradorDePreguntas::vueltas, GeneradorDePreguntas::sumaHasta, GeneradorDePreguntas::mitades);
    private static final List<Function<Random, Generada>> JUNIOR_2 = List.of(
            GeneradorDePreguntas::posicion, GeneradorDePreguntas::maximo, GeneradorDePreguntas::trozo, GeneradorDePreguntas::metodo);
    private static final List<Function<Random, Generada>> JUNIOR_4 = List.of(
            GeneradorDePreguntas::lista, GeneradorDePreguntas::objeto, GeneradorDePreguntas::mapa);
    private static final List<Function<Random, Generada>> SENIOR_1 = List.of(
            GeneradorDePreguntas::herencia, GeneradorDePreguntas::excepcion, GeneradorDePreguntas::interfaz);
    private static final List<Function<Random, Generada>> SENIOR_2 = List.of(
            GeneradorDePreguntas::filtro, GeneradorDePreguntas::recursion, GeneradorDePreguntas::contarPares);

    private GeneradorDePreguntas() {
    }

    /** Las plantillas de un escalón (vacío si no tiene). */
    public static List<Function<Random, Generada>> plantillas(Etiqueta nivel) {
        switch (nivel) {
            case NOOB_2: return NOOB_2;
            case NOOB_4: return NOOB_4;
            case JUNIOR_2: return JUNIOR_2;
            case JUNIOR_4: return JUNIOR_4;
            case SENIOR_1: return SENIOR_1;
            case SENIOR_2: return SENIOR_2;
            default: return List.of();
        }
    }

    /** «n» preguntas del escalón, cada una de una plantilla distinta mientras haya. */
    public static List<Generada> generar(Etiqueta nivel, Random r, int n) {
        List<Function<Random, Generada>> ps = new ArrayList<>(plantillas(nivel));
        Collections.shuffle(ps, r);
        List<Generada> l = new ArrayList<>();
        for (int i = 0; i < n && !ps.isEmpty(); i++) l.add(ps.get(i % ps.size()).apply(r));
        return l;
    }

    // ================================================================ ayudas

    private static int entre(Random r, int desde, int hasta) {
        return desde + r.nextInt(hasta - desde + 1);
    }

    private static String de(Random r, String[] lista) {
        return lista[r.nextInt(lista.length)];
    }

    /** Tres malas distintas de la buena (y entre sí); si faltan, números cercanos. */
    private static List<String> malas(String buena, Object... candidatas) {
        Set<String> s = new LinkedHashSet<>();
        for (Object c : candidatas) {
            String t = String.valueOf(c);
            if (!t.equals(buena)) s.add(t);
        }
        int k = 1;
        while (s.size() < 3) {
            String t = buena.matches("-?\\d+") ? Long.toString(Long.parseLong(buena) + (k % 2 == 0 ? -k : k)) : buena + "!".repeat(k);
            if (!t.equals(buena)) s.add(t);
            k++;
        }
        return new ArrayList<>(s).subList(0, 3);
    }

    private static Generada g(Etiqueta n, String cuerpo, Object respuesta, Object... malas) {
        return conArriba(n, "", cuerpo, respuesta, malas);
    }

    /** Con métodos fuera (arriba) y las sentencias que los usan (cuerpo). */
    private static Generada conArriba(Etiqueta n, String arriba, String cuerpo, Object respuesta, Object... malas) {
        String b = String.valueOf(respuesta);
        return new Generada(n, arriba, cuerpo, b, malas(b, malas));
    }

    // ================================================================ Noob II: variables y operadores

    static Generada suma(Random r) {
        int a = entre(r, 2, 19), b = entre(r, 2, 9);
        return g(Etiqueta.NOOB_2, "int x = " + a + ";\nx = x + " + b + ";\nSystem.out.println(x);", a + b, a, "x + " + b, a * b, "" + a + b);
    }

    static Generada division(Random r) {
        int b = entre(r, 2, 5), a = b * entre(r, 2, 9) + entre(r, 1, b - 1);
        return g(Etiqueta.NOOB_2, "int energia = " + a + ";\nint robots = " + b + ";\nSystem.out.println(energia / robots);",
                a / b, a / b + 1, String.format(java.util.Locale.ROOT, "%.1f", (double) a / b), a % b);
    }

    static Generada resto(Random r) {
        int b = entre(r, 3, 7), a = entre(r, 10, 40);
        if (a % b == 0) a++;
        return g(Etiqueta.NOOB_2, "System.out.println(" + a + " % " + b + ");", a % b, a / b, b - a % b, 0);
    }

    static Generada concatenar(Random r) {
        String n = de(r, NOMBRES);
        int a = entre(r, 1, 9), b = entre(r, 1, 9);
        return g(Etiqueta.NOOB_2, "String nombre = \"" + n + "\";\nSystem.out.println(nombre + " + a + " + " + b + ");",
                n + a + b, n + (a + b), "nombre" + a + b, n + " " + a + " " + b);
    }

    // ================================================================ Noob IV: condiciones y bucles

    static Generada siNo(Random r) {
        int e = entre(r, 10, 90), limite = entre(r, 20, 80);
        if (e == limite) e++;
        String mayor = r.nextBoolean() ? ">" : ">=";
        boolean si = mayor.equals(">") ? e > limite : e >= limite;
        return g(Etiqueta.NOOB_4, "int energia = " + e + ";\nif (energia " + mayor + " " + limite + ") {\n    System.out.println(\"ATACAR\");\n} else {\n"
                + "    System.out.println(\"HUIR\");\n}", si ? "ATACAR" : "HUIR", si ? "HUIR" : "ATACAR", "ATACAR HUIR", "energia");
    }

    static Generada vueltas(Random r) {
        int desde = entre(r, 0, 3), hasta = entre(r, 6, 15), paso = entre(r, 1, 3);
        boolean incluido = r.nextBoolean();
        int n = 0;
        for (int i = desde; incluido ? i <= hasta : i < hasta; i += paso) n++;
        String cond = incluido ? "i <= " + hasta : "i < " + hasta;
        return g(Etiqueta.NOOB_4, "int vueltas = 0;\nfor (int i = " + desde + "; " + cond + "; i += " + paso
                + ") {\n    vueltas++;\n}\nSystem.out.println(vueltas);",
                n, n + 1, n - 1, hasta - desde);
    }

    static Generada sumaHasta(Random r) {
        int n = entre(r, 3, 9), s = n * (n + 1) / 2;
        return g(Etiqueta.NOOB_4, "int suma = 0;\nfor (int i = 1; i <= " + n + "; i++) {\n    suma += i;\n}\nSystem.out.println(suma);",
                s, s - n, s + n + 1, n);
    }

    static Generada mitades(Random r) {
        int v = entre(r, 9, 120), pasos = 0;
        for (int x = v; x > 1; x /= 2) pasos++;
        return g(Etiqueta.NOOB_4, "int vida = " + v
                + ";\nint pasos = 0;\nwhile (vida > 1) {\n    vida = vida / 2;\n    pasos++;\n}\nSystem.out.println(pasos);",
                pasos, pasos + 1, pasos - 1, v / 2);
    }

    // ================================================================ Junior II: arrays, textos y métodos

    static Generada posicion(Random r) {
        int[] a = new int[5];
        for (int k = 0; k < a.length; k++) a[k] = entre(r, 1, 50);
        int i = entre(r, 1, 3);
        return g(Etiqueta.JUNIOR_2, "int[] danos = {" + lista(a) + "};\nSystem.out.println(danos[" + i + "]);", a[i], a[i - 1], a[i + 1], i);
    }

    static Generada maximo(Random r) {
        int[] a = new int[5];
        for (int k = 0; k < a.length; k++) a[k] = entre(r, 1, 60);
        int max = a[0], min = a[0], suma = 0;
        for (int x : a) {
            max = Math.max(max, x);
            min = Math.min(min, x);
            suma += x;
        }
        return g(Etiqueta.JUNIOR_2, "int[] a = {" + lista(a) + "};\nint m = a[0];\nfor (int x : a) {\n    if (x > m) m = x;\n}\nSystem.out.println(m);",
                max, min, suma, a[a.length - 1]);
    }

    static Generada trozo(Random r) {
        String p = de(r, PALABRAS);
        int desde = entre(r, 0, 2), hasta = entre(r, desde + 2, p.length());
        return g(Etiqueta.JUNIOR_2, "String s = \"" + p + "\";\nSystem.out.println(s.substring(" + desde + ", " + hasta + "));",
                p.substring(desde, hasta), p.substring(desde, Math.min(p.length(), hasta + 1)), p.substring(Math.min(desde + 1, hasta), hasta),
                p.substring(0, hasta));
    }

    static Generada metodo(Random r) {
        int k = entre(r, 2, 5), c = entre(r, 1, 9), a = entre(r, 1, 6), b = entre(r, 1, 6);
        String arriba = "static int potencia(int nivel) {\n    return nivel * " + k + " + " + c + ";\n}";
        int bien = a * k + c + b * k + c;
        return conArriba(Etiqueta.JUNIOR_2, arriba, "System.out.println(potencia(" + a + ") + potencia(" + b + "));", bien,
                (a + b) * k + c, a * k + b * k, "" + (a * k + c) + (b * k + c));
    }

    private static String lista(int[] a) {
        StringBuilder b = new StringBuilder();
        for (int k = 0; k < a.length; k++) b.append(k == 0 ? "" : ", ").append(a[k]);
        return b.toString();
    }

    // ================================================================ Junior IV: clases y colecciones

    static Generada lista(Random r) {
        int a = entre(r, 1, 9), b = entre(r, 10, 19), c = entre(r, 20, 29);
        int quita = entre(r, 0, 2);
        List<Integer> l = new ArrayList<>(List.of(a, b, c));
        l.remove(quita);
        List<Integer> mal = new ArrayList<>(List.of(a, b, c));
        mal.remove(Integer.valueOf(quita));
        return g(Etiqueta.JUNIOR_4, "List<Integer> l = new ArrayList<>();\nl.add(" + a + ");\nl.add(" + b + ");\nl.add(" + c + ");\nl.remove(" + quita + ");\n"
                + "System.out.println(l);", l, mal, List.of(a, b, c), l.size());
    }

    static Generada objeto(Random r) {
        int vida = entre(r, 50, 100), d1 = entre(r, 5, 20), d2 = entre(r, 5, 20);
        String cuerpo = "class Robot {\n    int vida = " + vida + ";\n    void golpe(int dano) {\n        vida -= dano;\n    }\n}\n"
                + "Robot r = new Robot();\nr.golpe(" + d1 + ");\nr.golpe(" + d2 + ");\nSystem.out.println(r.vida);";
        return g(Etiqueta.JUNIOR_4, cuerpo, vida - d1 - d2, vida, vida - d2, d1 + d2);
    }

    static Generada mapa(Random r) {
        String n = de(r, NOMBRES);
        int a = entre(r, 1, 9), b = entre(r, 10, 30);
        String cuerpo = "Map<String, Integer> puntos = new HashMap<>();\npuntos.put(\"" + n + "\", " + a + ");\npuntos.put(\"" + n + "\", " + b + ");\n"
                + "System.out.println(puntos.get(\"" + n + "\") + puntos.size());";
        return g(Etiqueta.JUNIOR_4, cuerpo, b + 1, a + 1, a + b, b + 2);
    }

    // ================================================================ Senior I: herencia, interfaces, excepciones

    static Generada herencia(Random r) {
        String a = de(r, PALABRAS), b = de(r, NOMBRES).toUpperCase(java.util.Locale.ROOT);
        boolean conSuper = r.nextBoolean();
        String cuerpo = "class Pieza {\n    String nombre() { return \"" + a + "\"; }\n}\nclass Brazo extends Pieza {\n    String nombre() { return "
                + (conSuper ? "super.nombre() + \"-" + b + "\"" : "\"" + b + "\"") + "; }\n}\nPieza p = new Brazo();\nSystem.out.println(p.nombre());";
        String bien = conSuper ? a + "-" + b : b;
        return g(Etiqueta.SENIOR_1, cuerpo, bien, a, conSuper ? b : a + "-" + b, b + "-" + a);
    }

    static Generada excepcion(Random r) {
        int n = entre(r, 2, 5), i = entre(r, 0, 6);
        boolean falla = i >= n;
        String cuerpo = "int[] a = new int[" + n + "];\ntry {\n    a[" + i
                + "] = 1;\n    System.out.print(\"A\");\n} catch (ArrayIndexOutOfBoundsException e) {\n"
                + "    System.out.print(\"B\");\n} finally {\n    System.out.print(\"C\");\n}\nSystem.out.println();";
        return g(Etiqueta.SENIOR_1, cuerpo, falla ? "BC" : "AC", falla ? "AC" : "BC", falla ? "B" : "A", "ABC");
    }

    static Generada interfaz(Random r) {
        int x = entre(r, 2, 6), y = entre(r, 2, 6), z = entre(r, 1, 4);
        String cuerpo = "interface Arma { int dano(); }\nArma laser = () -> " + x + " * " + y + ";\nArma puno = () -> " + z + ";\n"
                + "System.out.println(laser.dano() + puno.dano());";
        return g(Etiqueta.SENIOR_1, cuerpo, x * y + z, "" + x * y + z, x + y + z, x * y);
    }

    // ================================================================ Senior II: lambdas, streams, recursión

    static Generada filtro(Random r) {
        int[] a = new int[6];
        for (int k = 0; k < a.length; k++) a[k] = entre(r, 1, 20);
        int limite = entre(r, 5, 15), suma = 0, cuantos = 0, todo = 0;
        for (int v : a) {
            todo += v;
            if (v > limite) {
                suma += v;
                cuantos++;
            }
        }
        return g(Etiqueta.SENIOR_2, "int s = List.of(" + lista(a) + ").stream()\n        .filter(x -> x > " + limite
                + ")\n        .mapToInt(x -> x)\n        .sum();\n"
                + "System.out.println(s);", suma, cuantos, todo, todo - suma);
    }

    static Generada recursion(Random r) {
        int n = entre(r, 3, 6), f = 1;
        for (int k = 2; k <= n; k++) f *= k;
        String arriba = "static int f(int n) {\n    if (n <= 1) return 1;\n    return n * f(n - 1);\n}";
        return conArriba(Etiqueta.SENIOR_2, arriba, "System.out.println(f(" + n + "));", f, f / n, f * (n + 1), n * (n + 1) / 2);
    }

    static Generada contarPares(Random r) {
        int n = entre(r, 5, 14), m = entre(r, 2, 3), cuenta = 0;
        for (int k = 1; k <= n; k++) if (k * m % 4 == 0) cuenta++;
        return g(Etiqueta.SENIOR_2, "long c = IntStream.rangeClosed(1, " + n + ")\n        .map(x -> x * " + m
                + ")\n        .filter(x -> x % 4 == 0)\n        .count();\n"
                + "System.out.println(c);", cuenta, n / 4, n / 2, n);
    }
}
