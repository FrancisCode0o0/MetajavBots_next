package metajavbots.codigo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * VIGILANTE — Mira las clases ya compiladas del jugador y dice si usan algo
 * que no se puede usar: archivos del ordenador, la red, otros programas,
 * System.exit, la reflexión… Lo hace leyendo la «lista de nombres» (la
 * constant pool) de cada .class. El Cargador lo vuelve a comprobar al cargar.
 *
 * No es una caja fuerte contra alguien que quiera hacer daño a propósito:
 * es una barandilla para que un robot (o un robot ajeno que te pasen) no
 * toque el ordenador sin querer. Los robots de otros, aun así, se abren con
 * un aviso (docs/ESTADO.md).
 */
public final class Vigilante {

    /** Qué se permite: en un robot, menos (nada de hilos); en los ejercicios, también los hilos. */
    public enum Modo { ROBOT, EJERCICIO, CONFIANZA }

    private Vigilante() {
    }

    /** Lo que se puede usar, por paquete o clase (en formato interno: java/util/). */
    private static final String[] PERMITIDO = {
            "java/lang/Object", "java/lang/String", "java/lang/StringBuilder", "java/lang/StringBuffer",
            "java/lang/Math", "java/lang/StrictMath", "java/lang/Integer", "java/lang/Long", "java/lang/Double",
            "java/lang/Float", "java/lang/Short", "java/lang/Byte", "java/lang/Character", "java/lang/Boolean",
            "java/lang/Number", "java/lang/Comparable", "java/lang/CharSequence", "java/lang/Iterable", "java/lang/Enum",
            "java/lang/Record", "java/lang/System", "java/lang/Class", "java/lang/Void", "java/lang/Runnable",
            "java/lang/AutoCloseable", "java/lang/Cloneable", "java/lang/Override", "java/lang/FunctionalInterface",
            "java/lang/Deprecated", "java/lang/SuppressWarnings", "java/lang/SafeVarargs", "java/lang/Appendable",
            "java/lang/Throwable", "java/lang/Exception", "java/lang/RuntimeException", "java/lang/Error",
            "java/lang/AssertionError", "java/lang/ArithmeticException", "java/lang/ArrayIndexOutOfBoundsException",
            "java/lang/IndexOutOfBoundsException", "java/lang/StringIndexOutOfBoundsException",
            "java/lang/NullPointerException", "java/lang/IllegalArgumentException", "java/lang/IllegalStateException",
            "java/lang/NumberFormatException", "java/lang/UnsupportedOperationException", "java/lang/ClassCastException",
            "java/lang/CloneNotSupportedException", "java/lang/NegativeArraySizeException", "java/lang/ArrayStoreException",
            "java/lang/StackOverflowError", "java/lang/InterruptedException", "java/lang/MatchException",
            "java/lang/IncompatibleClassChangeError", "java/lang/NoSuchFieldError", "java/lang/Character$UnicodeBlock",
            "java/lang/invoke/", "java/lang/runtime/",
            "java/util/", "java/text/", "java/time/", "java/math/",
            "java/io/PrintStream", "java/io/Serializable", "java/io/IOException", "java/io/UncheckedIOException",
            "java/io/StringReader", "java/io/StringWriter", "java/io/BufferedReader", "java/io/Reader", "java/io/Writer",
            "java/io/Closeable", "java/io/Flushable", "java/io/FilterOutputStream", "java/io/OutputStream",
            "metajavbots/api/Metajavbot", "metajavbots/api/MetajavbotDe", "metajavbots/api/MetajavbotEn",
            "metajavbots/api/RobotBase", "metajavbots/api/Enemigo", "metajavbots/api/Gegner", "metajavbots/api/Enemy",
            "metajavbots/api/Golpe", "metajavbots/api/Treffer", "metajavbots/api/Hit", "metajavbots/api/Tope",
            "metajavbots/api/Obstaculo", "metajavbots/api/Hindernis", "metajavbots/api/Obstacle",
            "metajavbots/api/Aparato", "metajavbots/api/Geraet", "metajavbots/api/Gadget",
            "metajavbots/api/TopeDeCalculo", "metajavbots/junit/",
            "java/lang/Thread", "java/lang/ThreadLocal", "java/lang/InheritableThreadLocal", "java/lang/Thread$Builder",
            "java/lang/Thread$State", "java/lang/Thread$Builder$OfVirtual", "java/lang/Thread$Builder$OfPlatform",
    };

    /** Lo que no, aunque su paquete se pueda (va antes que lo permitido). */
    private static final String[] PROHIBIDO = {
            "java/util/ServiceLoader", "java/util/prefs/", "java/util/jar/", "java/util/zip/", "java/util/logging/",
            "java/util/spi/", "java/util/random/RandomGeneratorFactory",
    };

    /** En un robot, además, nada de hilos. */
    private static final String[] PROHIBIDO_EN_ROBOT = {
            "java/lang/Thread", "java/lang/ThreadLocal", "java/lang/InheritableThreadLocal",
            "java/util/concurrent/Executor", "java/util/concurrent/ExecutorService", "java/util/concurrent/Executors",
            "java/util/concurrent/ForkJoinPool", "java/util/concurrent/CompletableFuture", "java/util/concurrent/ThreadPoolExecutor",
            "java/util/concurrent/ScheduledExecutorService", "java/util/concurrent/ScheduledThreadPoolExecutor",
            "java/util/Timer", "java/util/concurrent/StructuredTaskScope",
    };

    /** Métodos y campos que no, de clases que sí: clase.miembro. */
    private static final String[] MIEMBROS_PROHIBIDOS = {
            "java/lang/System.exit", "java/lang/System.in", "java/lang/System.getenv", "java/lang/System.getProperty",
            "java/lang/System.getProperties", "java/lang/System.setProperty", "java/lang/System.setProperties",
            "java/lang/System.clearProperty", "java/lang/System.load", "java/lang/System.loadLibrary", "java/lang/System.setIn",
            "java/lang/System.setOut", "java/lang/System.setErr", "java/lang/System.console", "java/lang/System.inheritedChannel",
            "java/lang/System.getLogger", "java/lang/System.mapLibraryName", "java/lang/System.setSecurityManager",
            "java/lang/Class.forName", "java/lang/Class.getClassLoader", "java/lang/Class.getMethod", "java/lang/Class.getMethods",
            "java/lang/Class.getDeclaredMethod", "java/lang/Class.getDeclaredMethods", "java/lang/Class.getField",
            "java/lang/Class.getFields", "java/lang/Class.getDeclaredField", "java/lang/Class.getDeclaredFields",
            "java/lang/Class.getConstructor", "java/lang/Class.getConstructors", "java/lang/Class.getDeclaredConstructor",
            "java/lang/Class.getDeclaredConstructors", "java/lang/Class.newInstance", "java/lang/Class.getResource",
            "java/lang/Class.getResourceAsStream", "java/lang/Class.getModule", "java/lang/Class.getProtectionDomain",
            "java/lang/Class.getEnclosingMethod", "java/lang/Class.getRecordComponents", "java/lang/Class.getNestHost",
            "java/io/PrintStream.<init>", "java/lang/Object.wait", "java/lang/Thread.stop",
    };

    /** Dueños de métodos de java/lang/invoke y java/lang/runtime que sí puede llamar lo que genera el compilador. */
    private static final String[] INVOKE_PERMITIDO = {
            "java/lang/invoke/LambdaMetafactory", "java/lang/invoke/StringConcatFactory", "java/lang/runtime/ObjectMethods",
            "java/lang/runtime/SwitchBootstraps", "java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodType",
            "java/lang/invoke/CallSite", "java/lang/invoke/MethodHandle", "java/lang/invoke/ConstantBootstraps",
            "java/lang/invoke/TypeDescriptor", "java/lang/runtime/TemplateRuntime",
    };

    /** ¿Se puede usar esta clase (nombre interno, con barras)? Las del propio jugador se pasan aparte. */
    public static boolean clasePermitida(String nombre, Modo modo) {
        // Código del propio juego (el rival, para enseñarlo con su traza): se fía.
        if (modo == Modo.CONFIANZA) return true;
        if (nombre.startsWith("[")) {
            int i = nombre.lastIndexOf('[') + 1;
            if (i >= nombre.length() || nombre.charAt(i) != 'L') return true;   // array de primitivos
            nombre = nombre.substring(i + 1, nombre.length() - 1);
        }
        for (String p : PROHIBIDO) if (casa(nombre, p)) return false;
        if (modo == Modo.ROBOT) for (String p : PROHIBIDO_EN_ROBOT) if (casa(nombre, p)) return false;
        for (String p : PERMITIDO) if (casa(nombre, p)) return true;
        return false;
    }

    private static boolean casa(String nombre, String regla) {
        if (regla.endsWith("/")) return nombre.startsWith(regla);
        return nombre.equals(regla) || nombre.startsWith(regla + "$");
    }

    /** Lo que encuentra mal en una clase: una lista de textos («java.io.File», «System.exit»…), vacía si está bien. */
    public static List<String> revisar(byte[] clase, Set<String> propias, Modo modo) {
        List<String> mal = new ArrayList<>();
        try {
            Pool pool = Pool.leer(clase);
            Set<String> usadas = new LinkedHashSet<>();
            for (String c : pool.clases) usadas.add(c);
            for (String d : pool.descriptores) {
                int i = 0;
                while ((i = d.indexOf('L', i)) >= 0) {
                    int fin = d.indexOf(';', i);
                    if (fin < 0) break;
                    String nombre = d.substring(i + 1, fin);
                    if (nombre.contains("/") || propias.contains(nombre)) usadas.add(nombre);
                    i = fin + 1;
                }
            }
            for (String c : usadas) {
                if (c.startsWith("[")) {
                    int i = c.lastIndexOf('[') + 1;
                    if (i >= c.length() || c.charAt(i) != 'L') continue;   // array de primitivos
                    c = c.substring(i + 1, c.length() - 1);
                }
                if (propias.contains(c) || propias.contains(c.split("\\$")[0])) continue;
                if (!clasePermitida(c, modo)) mal.add(c.replace('/', '.'));
            }
            for (String[] m : pool.miembros) {
                String dueno = m[0], nombre = m[1];
                for (String p : MIEMBROS_PROHIBIDOS) {
                    if (p.equals(dueno + "." + nombre)) mal.add(dueno.substring(dueno.lastIndexOf('/') + 1) + "." + nombre);
                }
                if ((dueno.startsWith("java/lang/invoke/") || dueno.startsWith("java/lang/runtime/")) && !m[2].equals("handle")) {
                    boolean vale = false;
                    for (String p : INVOKE_PERMITIDO) if (dueno.equals(p) && !nombre.startsWith("find") && !nombre.equals("unreflect")) vale = true;
                    if (dueno.equals("java/lang/invoke/MethodHandles") || !vale) mal.add(dueno.replace('/', '.') + "." + nombre);
                }
            }
        } catch (IOException | RuntimeException e) {
            mal.add("(clase ilegible: " + e + ")");
        }
        return mal;
    }

    /** Lo justo de la constant pool de un .class. */
    static final class Pool {
        final List<String> clases = new ArrayList<>();
        final List<String> descriptores = new ArrayList<>();
        /** {dueño, nombre, "ref" o "handle"} */
        final List<String[]> miembros = new ArrayList<>();
        String esta, padre;

        static Pool leer(byte[] b) throws IOException {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
            if (in.readInt() != 0xCAFEBABE) throw new IOException("no es un .class");
            in.readUnsignedShort();
            in.readUnsignedShort();
            int n = in.readUnsignedShort();
            String[] utf = new String[n];
            int[] tipo = new int[n];
            int[] a = new int[n], c = new int[n];
            for (int i = 1; i < n; i++) {
                int tag = in.readUnsignedByte();
                tipo[i] = tag;
                switch (tag) {
                    case 1: utf[i] = in.readUTF(); break;
                    case 3: case 4: in.readInt(); break;
                    case 5: case 6: in.readLong(); i++; break;
                    case 7: case 8: case 16: case 19: case 20: a[i] = in.readUnsignedShort(); break;
                    case 9: case 10: case 11: case 12: case 17: case 18:
                        a[i] = in.readUnsignedShort();
                        c[i] = in.readUnsignedShort();
                        break;
                    case 15:
                        a[i] = in.readUnsignedByte();
                        c[i] = in.readUnsignedShort();
                        break;
                    default:
                        throw new IOException("constante desconocida " + tag);
                }
            }
            Pool p = new Pool();
            for (int i = 1; i < n; i++) {
                if (tipo[i] == 7 && utf[a[i]] != null) p.clases.add(utf[a[i]]);
                if (tipo[i] == 12 && utf[c[i]] != null) p.descriptores.add(utf[c[i]]);
                if (tipo[i] == 16 && utf[a[i]] != null) p.descriptores.add(utf[a[i]]);
                if (tipo[i] == 9 || tipo[i] == 10 || tipo[i] == 11) {
                    String dueno = utf[a[a[i]]];
                    String nombre = utf[a[c[i]]];
                    if (dueno != null && nombre != null) p.miembros.add(new String[]{dueno, nombre, "ref"});
                }
            }
            // Los MethodHandle (tag 15) apuntan a refs que solo usa el arranque de las lambdas: se marcan.
            for (int i = 1; i < n; i++) {
                if (tipo[i] == 15) {
                    int ref = c[i];
                    if (ref > 0 && ref < n && (tipo[ref] == 10 || tipo[ref] == 11)) {
                        String dueno = utf[a[a[ref]]];
                        String nombre = utf[a[c[ref]]];
                        for (String[] m : p.miembros) {
                            if (m[0].equals(dueno) && m[1].equals(nombre)) m[2] = "handle";
                        }
                    }
                }
            }
            for (String u : utf) {
                if (u != null && u.startsWith("(") && u.contains(")")) p.descriptores.add(u);
            }
            in.readUnsignedShort();
            p.esta = clase(utf, a, in.readUnsignedShort());
            p.padre = clase(utf, a, in.readUnsignedShort());
            return p;
        }

        private static String clase(String[] utf, int[] a, int i) {
            return i <= 0 || i >= a.length ? null : utf[a[i]];
        }
    }
}
