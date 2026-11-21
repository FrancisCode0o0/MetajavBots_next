package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import metajavbots.api.Puente;
import metajavbots.api.Tope;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.Inyector;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.codigo.Vigilante;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.rivales.Rivales;

/** El compilador del jugador: compila, cuenta pasos, corta bucles infinitos y no deja tocar el ordenador. */
final class PruebasDelCompilador {

    private PruebasDelCompilador() {
    }

    static final String SENCILLO = String.join("\n",
            "import metajavbots.api.*;",
            "",
            "public class MiRobot extends Metajavbot {",
            "    @Override",
            "    public void cadaTurno() {",
            "        if (restanteAvance() == 0) {",
            "            avanzar(100);",
            "            girar(45);",
            "        }",
            "        apuntar(8);",
            "    }",
            "",
            "    @Override",
            "    public void alVerEnemigo(Enemigo e) {",
            "        apuntarHacia(e.angulo());",
            "        atacar();",
            "        System.out.println(\"te veo a \" + (int) e.distancia());",
            "    }",
            "}");

    static Combatiente jugador(String codigo) {
        RobotDelJugador r = new RobotDelJugador();
        Compilador.Resultado res = r.compilar(codigo, Locale.ROOT);
        if (!res.bien) throw new AssertionError("no compila: " + res.errores);
        return new Combatiente("jugador", new Montaje(Modelo.de("cobalto")), r);
    }

    static Pelea contra(Combatiente c, long semilla) {
        return new Pelea(c, Rivales.crear(Modelo.de("chispa"), 0.3, 0.5, new Random(semilla)), semilla);
    }

    static String robotCon(String cuerpoDeCadaTurno, String extra) {
        return "import metajavbots.api.*;\nimport java.util.*;\npublic class R extends Metajavbot {\n" + extra
                + "\n    @Override public void cadaTurno() {\n" + cuerpoDeCadaTurno + "\n    }\n}\n";
    }

    static void apuntar() {
        apuntarParte1();
        apuntarParte2();
        apuntarParte3();
        apuntarParte4();
        apuntarParte5();
    }

    private static void apuntarParte1() {
        caso("compilador: hay JDK (javac) para compilar los robots", () -> cierto(Compilador.hayCompilador(), "sin JDK"));

        caso("compilador: un robot compila, pelea y escribe en su consola", () -> {
            Combatiente c = jugador(SENCILLO);
            Pelea p = contra(c, 1);
            p.jugarEntera();
            cierto(c.danoTotalHecho > 0, "hace daño");
            boolean escribio = false;
            for (String l : c.consola) if (l.contains("te veo a")) escribio = true;
            cierto(escribio, "System.out va a la consola del robot: " + c.consola);
        });

        caso("compilador: los errores salen con la línea y la columna del jugador", () -> {
            String malo = SENCILLO.replace("avanzar(100);", "avanzar(100)");
            Compilador.Resultado r = Compilador.compilar(malo, true, Vigilante.Modo.ROBOT, Locale.ROOT);
            falso(r.bien, "no debe compilar");
            Compilador.Fallo f = r.graves().get(0);
            igual(7L, f.linea, "línea");
            igual(25L, f.columna, "columna (la del jugador, sin lo inyectado)");
            igual("compiler.err.expected", f.codigo);
        });

        caso("compilador: un bucle infinito no cuelga la pelea (tope de cálculo)", () -> {
            for (String bucle : new String[]{"while (true) { }", "for (;;) { }", "do { } while (1 < 2);", "int i = 0; while (i >= 0) i = (i + 1) % 10;"}) {
                Combatiente c = jugador(robotCon("        " + bucle, ""));
                Pelea p = contra(c, 2);
                long t0 = System.currentTimeMillis();
                p.jugarEntera();
                cierto(System.currentTimeMillis() - t0 < 20000, "tarda demasiado: " + bucle);
                cierto(c.informes.get(0).topes > 0, "cuenta los topes: " + bucle);
            }
        });

        caso("compilador: recursión infinita y recursión enorme se cortan", () -> {
            Combatiente c = jugador(robotCon("        f(1);", "    int f(int n) { return f(n + 1) + 1; }"));
            contra(c, 3).jugarEntera();
            // Cada llamada es un paso: o se acaba el tope o se acaba la pila; las dos cosas se cortan sin romper nada.
            cierto(c.informes.get(0).topes > 0 || c.ultimoError.contains("recursión"), "infinita: " + c.ultimoError);
            Combatiente d = jugador(robotCon("        fib(40);", "    long fib(int n) { return n < 2 ? n : fib(n - 1) + fib(n - 2); }"));
            long t0 = System.currentTimeMillis();
            contra(d, 3).jugarEntera();
            cierto(System.currentTimeMillis() - t0 < 20000, "fib(40) se corta");
            cierto(d.informes.get(0).topes > 0, "fib(40) gasta el tope");
        });

        caso("compilador: lo prohibido no compila (archivos, salir, procesos, reflexión, hilos en robots)", () -> {
            String[][] malos = {
                    {"System.exit(0);", ""},
                    {"new java.io.File(\"x\").delete();", ""},
                    {"try { Runtime.getRuntime().exec(\"calc\"); } catch (Exception e) { }", ""},
                    {"try { Class.forName(\"java.lang.Runtime\"); } catch (Exception e) { }", ""},
                    {"new Thread(() -> { }).start();", ""},
                    {"try { java.nio.file.Files.readAllLines(java.nio.file.Path.of(\"x\")); } catch (Exception e) { }", ""},
                    {"try { new java.net.URL(\"http://x\").openStream(); } catch (Exception e) { }", ""},
                    {"System.out.println(System.getenv(\"PATH\"));", ""},
                    {"try { java.lang.invoke.MethodHandles.lookup(); } catch (Exception e) { }", ""},
                    {"new Scanner(System.in);", ""},
            };
            for (String[] m : malos) {
                Compilador.Resultado r = Compilador.compilar(robotCon("        " + m[0], m[1]), true, Vigilante.Modo.ROBOT, Locale.ROOT);
                falso(r.bien, "no debería compilar: " + m[0]);
                boolean prohibido = false;
                for (Compilador.Fallo f : r.errores) if (f.codigo.equals("metajavbots.prohibido")) prohibido = true;
                cierto(prohibido, "dice que está prohibido: " + m[0] + " → " + r.errores);
            }
        });
    }

    private static void apuntarParte2() {
        caso("compilador: Java moderno compila sin falsos avisos (lambdas, records, switch, text blocks, var, streams)", () -> {
            String moderno = String.join("\n",
                    "import metajavbots.api.*;",
                    "import java.util.*;",
                    "import java.util.function.*;",
                    "import java.util.stream.*;",
                    "public class Moderno extends Metajavbot {",
                    "    record Punto(double x, double y) { double d() { return Math.hypot(x, y); } }",
                    "    enum Modo { ATAQUE, HUIDA; Modo otro() { return this == ATAQUE ? HUIDA : ATAQUE; } }",
                    "    sealed interface Orden permits Ir, Parar { }",
                    "    record Ir(double d) implements Orden { }",
                    "    record Parar() implements Orden { }",
                    "    private Modo modo = Modo.ATAQUE;",
                    "    private final List<Punto> vistos = new ArrayList<>();",
                    "    @Override public void cadaTurno() {",
                    "        var texto = \"\"\"",
                    "            hola",
                    "            \"\"\";",
                    "        Function<Integer, Integer> doble = n -> n * 2;",
                    "        int s = IntStream.range(0, 10).map(doble::apply).sum();",
                    "        String q = switch (modo) { case ATAQUE -> \"a\" + s; case HUIDA -> \"h\"; };",
                    "        Orden o = turnoActual() % 2 == 0 ? new Ir(50) : new Parar();",
                    "        if (o instanceof Ir ir) avanzar(ir.d());",
                    "        double m = vistos.stream().mapToDouble(Punto::d).max().orElse(0);",
                    "        Map<String, Integer> cuenta = new HashMap<>();",
                    "        cuenta.merge(q, 1, Integer::sum);",
                    "        Optional.of(texto).map(String::strip).ifPresent(t -> { });",
                    "        if (turnoActual() % 100 == 0) modo = modo.otro();",
                    "        Runnable r = () -> girar(5);",
                    "        r.run();",
                    "        String desc = o.toString() + m;",
                    "    }",
                    "    @Override public void alVerEnemigo(Enemigo e) { vistos.add(new Punto(e.x(), e.y())); if (vistos.size() > 20) vistos.remove(0); }",
                    "}");
            Compilador.Resultado r = Compilador.compilar(moderno, true, Vigilante.Modo.ROBOT, Locale.ROOT);
            cierto(r.bien, "compila: " + r.errores);
            Combatiente c = jugador(moderno);
            contra(c, 4).jugarEntera();
            igual("", c.ultimoError, "sin errores al pelear");
        });

        caso("compilador: el inyector no cambia lo que hace el código", () -> {
            String prog = "public class Cuenta { public static int f() { int s = 0; for (int i = 0; i < 10; i++) { int j = 0; "
                    + "while (j < i) { s += j; j++; } } do { s--; } while (s > 100); for (int k : new int[]{1,2,3}) s += k; return s; } }";
            Inyector.Inyectado in = Inyector.inyectar(prog);
            cierto(in.cuantos() >= 4, "mete pasos: " + in.cuantos());
            cierto(in.fuente.contains("i++) { metajavbots.api.Tope.paso();"), "en el for: " + in.fuente);
            Compilador.Resultado r = Compilador.compilar(prog, true, Vigilante.Modo.EJERCICIO, Locale.ROOT);
            cierto(r.bien, "compila: " + r.errores);
            metajavbots.codigo.Cargador cg = new metajavbots.codigo.Cargador(r.clases, Vigilante.Modo.EJERCICIO);
            Object v = cg.loadClass("Cuenta").getMethod("f").invoke(null);
            igual(106, v, "mismo resultado (120, baja a 100, +6)");
        });
    }

    private static void apuntarParte3() {
        caso("compilador: constructores, clases anónimas y enums con cuerpo no se rompen al inyectar", () -> {
            String prog = String.join("\n",
                    "import java.util.*;",
                    "public class Varios {",
                    "    private int x;",
                    "    public Varios(int x) { this.x = x; }",
                    "    public Varios() { this(3); }",
                    "    enum Op { SUMA(1) { int f(int a) { return a + 1; } }, RESTA(2) { int f(int a) { return a - 1; } }; "
                            + "final int c; Op(int c) { this.c = c; } abstract int f(int a); }",
                    "    static class Hijo extends Varios { Hijo() { super(5); } }",
                    "    public static int g() {",
                    "        Comparator<Integer> c = new Comparator<Integer>() { public int compare(Integer a, Integer b) { return b - a; } };",
                    "        List<Integer> l = new ArrayList<>(List.of(1, 3, 2));",
                    "        l.sort(c);",
                    "        return l.get(0) * 100 + Op.SUMA.f(1) * 10 + ((Varios) new Hijo()).x + new Varios().x;",
                    "    }",
                    "}");
            Compilador.Resultado r = Compilador.compilar(prog, true, Vigilante.Modo.EJERCICIO, Locale.ROOT);
            cierto(r.bien, "compila: " + r.errores + "\n" + Inyector.inyectar(prog).fuente);
            metajavbots.codigo.Cargador cg = new metajavbots.codigo.Cargador(r.clases, Vigilante.Modo.EJERCICIO);
            igual(328, cg.loadClass("Varios").getMethod("g").invoke(null));
        });

        caso("traza: el rival compilado con traza pelea y dice qué líneas ejecuta", () -> {
            Combatiente a = Rivales.crear(Modelo.de("vigia"), 0.6, 0.5, new Random(1), true);
            Combatiente b = Rivales.crear(Modelo.de("mazo"), 0.6, 0.5, new Random(2), true);
            Pelea p = new Pelea(a, b, 9);
            p.empezarRonda();
            for (int i = 0; i < 200; i++) p.avanzar();
            cierto(p.cuerpo(0).ultimaLinea > 0, "apunta la última línea");
            cierto(p.cuerpo(0).lineasDelTurno.cardinality() >= 3, "varias líneas en un turno: " + p.cuerpo(0).lineasDelTurno);
            String[] lineas = metajavbots.rivales.RivalConTraza.fuente().split("\n");
            String l = lineas[p.cuerpo(0).ultimaLinea - 1].strip();
            cierto(!l.isEmpty() && !l.startsWith("//") && !l.startsWith("*"), "la línea es código: «" + l + "»");
            p.jugarEntera();
            igual("", a.ultimoError, "sin errores");
        });

        caso("traza: todas las soluciones de los ejercicios siguen compilando con la traza metida", () -> {
            List<String> mal = new ArrayList<>();
            int n = 0;
            for (metajavbots.ejercicios.Ejercicio e : metajavbots.ejercicios.Banco.delJuego().todos()) {
                if (e.java > Compilador.versionDeJava()) continue;
                java.util.Map<String, String> f = new java.util.LinkedHashMap<>();
                if (e.tipo == metajavbots.ejercicios.Ejercicio.Tipo.CLASE) {
                    f.put(Compilador.nombreDeClase(e.solucion), e.solucion);
                } else {
                    f.put("Ejercicio", "import java.util.*; import java.util.function.*; import java.util.stream.*; public class Ejercicio {\n"
                            + e.codigoSolucion() + "\n}\n");
                }
                Compilador.Resultado r = Compilador.compilar(f, true, true, Vigilante.Modo.EJERCICIO, Locale.ROOT);
                n++;
                if (!r.bien) mal.add(e.id + ": " + r.graves());
            }
            System.out.println("            " + n + " soluciones compiladas con traza");
            cierto(mal.isEmpty(), String.join("\n  ", mal));
        });
    }

    private static void apuntarParte4() {
        caso("compilador: cambiar el código entre rondas estrena el robot y conserva la memoria", () -> {
            RobotDelJugador r = new RobotDelJugador();
            String v1 = robotCon("        memoria().merge(\"turnos\", 1, (a, b) -> (Integer) a + (Integer) b);", "");
            cierto(r.compilar(v1, Locale.ROOT).bien, "v1");
            Combatiente c = new Combatiente("j", new Montaje(Modelo.de("vigia")), r);
            Pelea p = contra(c, 5);
            p.empezarRonda();
            while (p.estado() == Pelea.Estado.LUCHANDO) p.avanzar();
            Object turnos = c.memoria.get("turnos");
            cierto(turnos instanceof Integer && (Integer) turnos > 100, "cuenta turnos: " + turnos);
            String v2 = robotCon("        memoria().put(\"version\", 2);", "");
            cierto(r.compilar(v2, Locale.ROOT).bien, "v2");
            p.empezarRonda();
            p.avanzar();
            igual(2, c.memoria.get("version"), "la ronda 2 usa el código nuevo");
            igual(turnos, c.memoria.get("turnos"), "la memoria se conserva");
        });
    }

    private static void apuntarParte5() {
        caso("compilador: la cuenta de pasos no pierde pasos entre hilos", () -> {
            Puente.contar(1_000_000L, 5_000_000_000L);
            long devueltos;
            try {
                CountDownLatch salida = new CountDownLatch(1);
                Thread[] hilos = new Thread[4];
                Throwable[] fallos = new Throwable[hilos.length];
                for (int i = 0; i < hilos.length; i++) {
                    int cual = i;
                    hilos[i] = new Thread(() -> {
                        try {
                            salida.await();
                            for (int j = 0; j < 25_000; j++) Tope.paso();
                        } catch (Throwable t) {
                            fallos[cual] = t;
                        }
                    });
                    hilos[i].start();
                }
                salida.countDown();
                for (Thread h : hilos) h.join();
                for (Throwable t : fallos) if (t != null) throw new AssertionError("un hilo falló", t);
                igual(100_000L, Tope.pasos(), "pasos contados");
            } finally {
                devueltos = Puente.dejarDeContar();
            }
            igual(100_000L, devueltos, "pasos devueltos");
        });
    }
}
