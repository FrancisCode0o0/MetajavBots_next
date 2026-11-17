package metajavbots.codigo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * SALIDA — Lo que el código del jugador escribe con System.out.println va a
 * su consola, no a la del juego. Se pone una vez (instalar) un System.out que
 * mira quién escribe: si el hilo está «capturando», va a su búfer; si no, a
 * la salida de siempre. Los hilos que cree el código heredan la captura.
 */
public final class Salida {

    private static final InheritableThreadLocal<ByteArrayOutputStream> BUFER = new InheritableThreadLocal<>();
    private static PrintStream original, originalErr;
    private static final int MAXIMO = 200_000;

    private Salida() {
    }

    public static synchronized void instalar() {
        if (original != null) return;
        original = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(new Repartidor(original), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new Repartidor(originalErr), true, StandardCharsets.UTF_8));
    }

    /** Empieza a capturar lo que escribe este hilo (y los suyos). */
    public static void capturar() {
        instalar();
        BUFER.set(new ByteArrayOutputStream());
    }

    /** Deja de capturar y devuelve lo escrito. */
    public static String soltar() {
        System.out.flush();
        ByteArrayOutputStream b = BUFER.get();
        BUFER.remove();
        return b == null ? "" : b.toString(StandardCharsets.UTF_8);
    }

    private static final class Repartidor extends OutputStream {
        private final PrintStream destino;

        Repartidor(PrintStream destino) {
            this.destino = destino;
        }

        @Override
        public void write(int b) {
            ByteArrayOutputStream bufer = BUFER.get();
            if (bufer != null) {
                if (bufer.size() < MAXIMO) bufer.write(b);
            } else {
                destino.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            ByteArrayOutputStream bufer = BUFER.get();
            if (bufer != null) {
                if (bufer.size() < MAXIMO) bufer.write(b, off, Math.min(len, MAXIMO - bufer.size()));
            } else {
                destino.write(b, off, len);
            }
        }

        @Override
        public void flush() {
            destino.flush();
        }
    }
}
