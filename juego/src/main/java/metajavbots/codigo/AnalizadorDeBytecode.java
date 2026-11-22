package metajavbots.codigo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * ANALIZADOR DE BYTECODE — Lee un archivo {@code .class} compilado en memoria
 * sin bibliotecas externas (cabecera {@code 0xCAFEBABE}, constant pool, campos,
 * métodos y atributos {@code Code}) para medir métricas reales de bytecode:
 * instrucciones, asignaciones ({@code new}/{@code newarray}), {@code invokedynamic},
 * monitores ({@code synchronized}), escrituras estáticas ({@code putstatic}) y
 * autoboxing ({@code Integer.valueOf}, {@code Double.valueOf}…).
 */
public final class AnalizadorDeBytecode {

    /** Métricas reales extraídas del bytecode de una o varias clases compiladas. */
    public static final class Informe {
        public final int versionMayor;
        public final int clases;
        public final int metodos;
        public final int instrucciones;
        public final int asignaciones;
        public final int invokedynamic;
        public final int monitores;
        public final int escriturasEstaticas;
        public final int camposEstaticosMutables;
        public final int autoboxing;
        public final int bytesDeCodigo;

        public Informe(int versionMayor, int clases, int metodos, int instrucciones, int asignaciones, int invokedynamic,
                int monitores, int escriturasEstaticas, int camposEstaticosMutables, int autoboxing, int bytesDeCodigo) {
            this.versionMayor = versionMayor;
            this.clases = clases;
            this.metodos = metodos;
            this.instrucciones = instrucciones;
            this.asignaciones = asignaciones;
            this.invokedynamic = invokedynamic;
            this.monitores = monitores;
            this.escriturasEstaticas = escriturasEstaticas;
            this.camposEstaticosMutables = camposEstaticosMutables;
            this.autoboxing = autoboxing;
            this.bytesDeCodigo = bytesDeCodigo;
        }

        /** ¿Tiene bytecode limpio y eficiente (válido, sin autoboxing y con pocas asignaciones)? */
        public boolean esEficiente() {
            return clases > 0 && instrucciones > 0 && autoboxing == 0 && asignaciones <= 6;
        }

        /** ¿Declara o escribe estado estático mutable (riesgo en ejecución paralela)? */
        public boolean tieneEstadoEstaticoMutable() {
            return camposEstaticosMutables > 0 || escriturasEstaticas > 0;
        }

        /** Resumen técnico breve para el depurador y el revisor. */
        public String resumen() {
            return "BC v" + versionMayor + ": " + instrucciones + " ops · " + asignaciones + " new · "
                    + autoboxing + " box · " + invokedynamic + " indy · " + monitores + " sync";
        }
    }

    /** Información mínima de la constant pool necesaria para resolver opcodes. */
    private static final class Pool {
        final String[] utf;
        final int[] tipo;
        final int[] a;
        final int[] b;

        Pool(int n) {
            utf = new String[n];
            tipo = new int[n];
            a = new int[n];
            b = new int[n];
        }

        String utf(int i) {
            return i > 0 && i < utf.length ? utf[i] : null;
        }

        boolean esAutoboxing(int idx) {
            if (idx <= 0 || idx >= tipo.length || (tipo[idx] != 10 && tipo[idx] != 11)) return false;
            String dueno = utf(a[a[idx]]);
            String nombre = utf(a[b[idx]]);
            if (!"valueOf".equals(nombre) || dueno == null) return false;
            return dueno.equals("java/lang/Integer") || dueno.equals("java/lang/Long") || dueno.equals("java/lang/Double")
                    || dueno.equals("java/lang/Float") || dueno.equals("java/lang/Boolean") || dueno.equals("java/lang/Byte")
                    || dueno.equals("java/lang/Short") || dueno.equals("java/lang/Character");
        }
    }

    /** Contadores mutables acumulados al recorrer una clase. */
    private static final class Acumulador {
        int versionMayor, clases, metodos, instrucciones, asignaciones, invokedynamic, monitores;
        int escriturasEstaticas, camposEstaticosMutables, autoboxing, bytesDeCodigo;

        Informe construir() {
            return new Informe(versionMayor, clases, metodos, instrucciones, asignaciones, invokedynamic,
                    monitores, escriturasEstaticas, camposEstaticosMutables, autoboxing, bytesDeCodigo);
        }
    }

    private AnalizadorDeBytecode() {
    }

    /** Analiza un único archivo {@code .class} en memoria. */
    public static Informe analizar(byte[] bytes) {
        Acumulador acc = new Acumulador();
        if (bytes != null && bytes.length > 10) leerClase(bytes, acc);
        return acc.construir();
    }

    /** Analiza todas las clases compiladas de un robot y agrega sus métricas de bytecode. */
    public static Informe analizarTodas(Map<String, byte[]> clases) {
        Acumulador acc = new Acumulador();
        if (clases != null) {
            for (byte[] b : clases.values()) if (b != null && b.length > 10) leerClase(b, acc);
        }
        return acc.construir();
    }

    private static void leerClase(byte[] bytes, Acumulador acc) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 0xCAFEBABE) return;
            in.readUnsignedShort();
            acc.versionMayor = Math.max(acc.versionMayor, in.readUnsignedShort());
            Pool pool = leerPool(in);
            in.readUnsignedShort(); // access_flags
            in.readUnsignedShort(); // this_class
            in.readUnsignedShort(); // super_class
            int ifaces = in.readUnsignedShort();
            saltar(in, ifaces * 2);
            leerCampos(in, acc);
            leerMetodos(in, pool, acc);
            acc.clases++;
        } catch (IOException | RuntimeException e) {
            // clase incompleta o no estándar: se omite sin romper el juego
        }
    }

    private static Pool leerPool(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();
        Pool p = new Pool(n);
        for (int i = 1; i < n; i++) {
            int tag = in.readUnsignedByte();
            p.tipo[i] = tag;
            switch (tag) {
                case 1: p.utf[i] = in.readUTF(); break;
                case 3: case 4: in.readInt(); break;
                case 5: case 6: in.readLong(); i++; break;
                case 7: case 8: case 16: case 19: case 20: p.a[i] = in.readUnsignedShort(); break;
                case 9: case 10: case 11: case 12: case 17: case 18:
                    p.a[i] = in.readUnsignedShort();
                    p.b[i] = in.readUnsignedShort();
                    break;
                case 15:
                    p.a[i] = in.readUnsignedByte();
                    p.b[i] = in.readUnsignedShort();
                    break;
                default:
                    throw new IOException("tag " + tag);
            }
        }
        return p;
    }

    private static void leerCampos(DataInputStream in, Acumulador acc) throws IOException {
        int n = in.readUnsignedShort();
        for (int i = 0; i < n; i++) {
            int flags = in.readUnsignedShort();
            in.readUnsignedShort(); // name
            in.readUnsignedShort(); // descriptor
            boolean esStatic = (flags & 0x0008) != 0;
            boolean esFinal = (flags & 0x0010) != 0;
            boolean esSintetico = (flags & 0x1000) != 0;
            if (esStatic && !esFinal && !esSintetico) acc.camposEstaticosMutables++;
            saltarAtributos(in);
        }
    }

    private static void leerMetodos(DataInputStream in, Pool pool, Acumulador acc) throws IOException {
        int n = in.readUnsignedShort();
        for (int i = 0; i < n; i++) {
            in.readUnsignedShort(); // flags
            in.readUnsignedShort(); // name
            in.readUnsignedShort(); // descriptor
            acc.metodos++;
            int attrs = in.readUnsignedShort();
            for (int a = 0; a < attrs; a++) {
                String nombre = pool.utf(in.readUnsignedShort());
                int len = in.readInt();
                if ("Code".equals(nombre) && len >= 12) {
                    byte[] cuerpo = in.readNBytes(len);
                    analizarCode(cuerpo, pool, acc);
                } else {
                    saltar(in, len);
                }
            }
        }
    }

    private static void analizarCode(byte[] cuerpo, Pool pool, Acumulador acc) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(cuerpo));
        in.readUnsignedShort(); // max_stack
        in.readUnsignedShort(); // max_locals
        int codeLen = in.readInt();
        if (codeLen <= 0 || codeLen > cuerpo.length - 8) return;
        byte[] code = in.readNBytes(codeLen);
        acc.bytesDeCodigo += codeLen;
        int pc = 0;
        while (pc < code.length) {
            int op = code[pc] & 0xFF;
            acc.instrucciones++;
            if (op == 0xBB || op == 0xBC || op == 0xBD || op == 0xC5) acc.asignaciones++;
            else if (op == 0xBA) acc.invokedynamic++;
            else if (op == 0xC2) acc.monitores++;
            else if (op == 0xB3) acc.escriturasEstaticas++;
            else if (op == 0xB8 && pc + 2 < code.length) {
                int idx = ((code[pc + 1] & 0xFF) << 8) | (code[pc + 2] & 0xFF);
                if (pool.esAutoboxing(idx)) acc.autoboxing++;
            }
            pc += tamanoInstruccion(code, pc, op);
        }
    }

    private static int tamanoInstruccion(byte[] code, int pc, int op) {
        if (op == 0xAA) return tamanoTableswitch(code, pc);
        if (op == 0xAB) return tamanoLookupswitch(code, pc);
        if (op == 0xC4) return (pc + 1 < code.length && (code[pc + 1] & 0xFF) == 0x84) ? 6 : 4;
        if (op == 0xB9 || op == 0xBA || op == 0xC8 || op == 0xC9) return 5;
        if (op == 0xC5) return 4;
        if (op == 0x10 || op == 0x12 || op == 0xBC || (op >= 0x15 && op <= 0x19) || (op >= 0x36 && op <= 0x3A) || op == 0xA9) return 2;
        if (op == 0x11 || op == 0x13 || op == 0x14 || op == 0x84 || (op >= 0x99 && op <= 0xA8)
                || (op >= 0xB2 && op <= 0xB8) || op == 0xBB || op == 0xBD || op == 0xC0 || op == 0xC1 || op == 0xC6 || op == 0xC7) return 3;
        return 1;
    }

    private static int tamanoTableswitch(byte[] code, int pc) {
        int pad = (4 - ((pc + 1) & 3)) & 3;
        int base = pc + 1 + pad;
        if (base + 12 > code.length) return code.length - pc;
        int low = entero(code, base + 4);
        int high = entero(code, base + 8);
        int count = high >= low ? (high - low + 1) : 0;
        return Math.max(1, (base - pc) + 12 + count * 4);
    }

    private static int tamanoLookupswitch(byte[] code, int pc) {
        int pad = (4 - ((pc + 1) & 3)) & 3;
        int base = pc + 1 + pad;
        if (base + 8 > code.length) return code.length - pc;
        int npairs = Math.max(0, entero(code, base + 4));
        return Math.max(1, (base - pc) + 8 + npairs * 8);
    }

    private static int entero(byte[] b, int o) {
        return ((b[o] & 0xFF) << 24) | ((b[o + 1] & 0xFF) << 16) | ((b[o + 2] & 0xFF) << 8) | (b[o + 3] & 0xFF);
    }

    private static void saltarAtributos(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();
        for (int i = 0; i < n; i++) {
            in.readUnsignedShort();
            saltar(in, in.readInt());
        }
    }

    private static void saltar(DataInputStream in, int bytes) throws IOException {
        int faltan = bytes;
        while (faltan > 0) {
            int s = in.skipBytes(faltan);
            if (s <= 0) break;
            faltan -= s;
        }
    }
}
