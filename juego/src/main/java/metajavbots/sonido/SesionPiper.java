package metajavbots.sonido;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * SESIÓN PIPER — Mantiene vivo un proceso persistente de Piper para un modelo
 * y velocidad concretos, consumiendo {@code stdout} en bloques pequeños y
 * {@code stderr} en un hilo dedicado para detectar el fin de cada frase sin
 * cerrar el proceso ni bloquear los pipes en Windows.
 */
public final class SesionPiper {

    /** Abstracción de un proceso hijo de Piper (permite pruebas deterministas sin binario externo). */
    public interface ProcesoPiper {
        /** Flujo de entrada estándar (texto UTF-8 terminado en salto de línea). */
        OutputStream entrada();

        /** Flujo de salida estándar (audio PCM crudo de 16 bits LE mono). */
        InputStream salida();

        /** Flujo de error estándar (registros de Piper). */
        InputStream error();

        /** Indica si el proceso sigue en ejecución. */
        boolean vivo();

        /** Espera hasta {@code ms} milisegundos a que el proceso termine. */
        boolean esperarFin(long ms) throws InterruptedException;

        /** Solicita un cierre ordenado del proceso. */
        void destruir();

        /** Fuerza la terminación inmediata del proceso. */
        void destruirForzosamente();
    }

    /** Fábrica que lanza un proceso de Piper con los parámetros dados. */
    public interface FabricaDeProcesos {
        /** Arranca el proceso persistente de Piper para ese modelo y escala de duración. */
        ProcesoPiper arrancar(Path ejecutable, Path modelo, String largo, Path carpeta) throws IOException;
    }

    /** Tamaño de bloque de lectura de PCM crudo (~46 ms a 22 050 Hz, 16 bits mono). */
    static final int TAMANO_BLOQUE = 2048;
    private static final long PAUSA_DRENAJE_FIN_MS = 35;
    private static final long ESPERA_INACTIVIDAD_SIN_MARCA_MS = 600;
    private static final long ESPERA_PRIMER_BYTE_MS = 15_000;
    private static final long ESPERA_DRENAJE_CANCELADO_MS = 450;
    private static final byte[] FIN_DE_FLUJO = new byte[0];

    private final ProcesoPiper proceso;
    private final BlockingQueue<byte[]> cola = new LinkedBlockingQueue<>();
    private final AtomicLong frasesCompletadas = new AtomicLong();
    private final Thread hiloSalida;
    private final Thread hiloError;
    private volatile boolean usaMarcadorStderr;
    private volatile boolean eofSalida;
    private volatile boolean cerrada;

    /** Crea una sesión sobre el proceso ya arrancado y levanta los hilos lectores de stdout y stderr. */
    SesionPiper(ProcesoPiper proceso) {
        this.proceso = proceso;
        this.hiloSalida = new Thread(this::leerSalida, "piper-stdout");
        this.hiloSalida.setDaemon(true);
        this.hiloError = new Thread(this::leerError, "piper-stderr");
        this.hiloError.setDaemon(true);
        this.hiloSalida.start();
        this.hiloError.start();
    }

    /** Fábrica real que ejecuta el binario local de Piper con {@code --output_raw}. */
    static FabricaDeProcesos fabricaReal() {
        return (exe, modelo, largo, carpeta) -> {
            Process p = new ProcessBuilder(exe.toString(), "--model", modelo.toString(), "--output_raw",
                    "--length_scale", largo, "--sentence_silence", "0.15")
                    .directory(carpeta.toFile())
                    .start();
            return new ProcesoReal(p);
        };
    }

    /** Indica si la sesión sigue utilizable para nuevas frases. */
    boolean viva() {
        return !cerrada && !eofSalida && proceso.vivo();
    }

    /**
     * Envía una frase al proceso persistente y entrega cada bloque PCM al receptor en cuanto llega.
     * Devuelve {@code true} si la frase se sintetizó o se canceló limpiamente, y {@code false} si el proceso falló.
     */
    synchronized boolean sintetizar(String texto, Consumer<byte[]> receptor, BooleanSupplier cancelada) {
        if (!viva()) return false;
        cola.clear();
        long objetivo = frasesCompletadas.get() + 1;
        String linea = texto.replace('\r', ' ').replace('\n', ' ').strip();
        if (linea.isEmpty()) return true;
        try {
            OutputStream in = proceso.entrada();
            in.write((linea + "\n").getBytes(StandardCharsets.UTF_8));
            in.flush();
        } catch (IOException e) {
            cerrar();
            return false;
        }
        return recibirBloques(objetivo, receptor, cancelada);
    }

    private boolean recibirBloques(long objetivo, Consumer<byte[]> receptor, BooleanSupplier cancelada) {
        boolean recibidoAlgo = false;
        long inicio = System.currentTimeMillis();
        long ultimoBloque = inicio;
        while (true) {
            if (cancelada.getAsBoolean()) {
                drenarTrasCancelacion(objetivo);
                return true;
            }
            boolean marcada = frasesCompletadas.get() >= objetivo;
            long espera = marcada ? PAUSA_DRENAJE_FIN_MS : 40;
            byte[] trozo;
            try {
                trozo = cola.poll(espera, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cerrar();
                return false;
            }
            if (trozo == FIN_DE_FLUJO) {
                cerrar();
                return recibidoAlgo;
            }
            if (trozo != null) {
                recibidoAlgo = true;
                ultimoBloque = System.currentTimeMillis();
                if (!cancelada.getAsBoolean()) receptor.accept(trozo);
                continue;
            }
            if (marcada) return recibidoAlgo;
            long ahora = System.currentTimeMillis();
            if (!recibidoAlgo && (ahora - inicio > ESPERA_PRIMER_BYTE_MS || !proceso.vivo())) {
                cerrar();
                return false;
            }
            if (recibidoAlgo && !usaMarcadorStderr && ahora - ultimoBloque >= ESPERA_INACTIVIDAD_SIN_MARCA_MS) {
                return true;
            }
        }
    }

    private void drenarTrasCancelacion(long objetivo) {
        long limite = System.currentTimeMillis() + ESPERA_DRENAJE_CANCELADO_MS;
        while (System.currentTimeMillis() < limite) {
            boolean marcada = frasesCompletadas.get() >= objetivo;
            try {
                byte[] trozo = cola.poll(marcada ? PAUSA_DRENAJE_FIN_MS : 25, TimeUnit.MILLISECONDS);
                if (trozo == FIN_DE_FLUJO) {
                    cerrar();
                    return;
                }
                if (trozo == null && marcada) return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cerrar();
                return;
            }
        }
        // Si Piper sigue generando una frase muy larga tras cancelar, reiniciamos la sesión limpiamente.
        cerrar();
    }

    private void leerSalida() {
        byte[] bufer = new byte[TAMANO_BLOQUE];
        try (InputStream out = proceso.salida()) {
            int n;
            while (!cerrada && (n = out.read(bufer)) != -1) {
                if (n > 0) cola.offer(Arrays.copyOf(bufer, n));
            }
        } catch (IOException e) {
            // el proceso se cerró o se interrumpió la tubería
        } finally {
            eofSalida = true;
            cola.offer(FIN_DE_FLUJO);
        }
    }

    private void leerError() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proceso.error(), StandardCharsets.UTF_8))) {
            String linea;
            while (!cerrada && (linea = r.readLine()) != null) {
                if (linea.contains("Initialized piper") || linea.contains("Loaded voice")) {
                    usaMarcadorStderr = true;
                }
                if (linea.contains("Real-time factor:") || linea.contains("Waiting for audio to finish")) {
                    usaMarcadorStderr = true;
                }
                if (linea.contains("Real-time factor:")) {
                    frasesCompletadas.incrementAndGet();
                }
            }
        } catch (IOException e) {
            // fin del flujo de error al cerrarse el proceso
        }
    }

    /** Cierra limpiamente el proceso hijo (destroy y destroyForcibly si no responde). */
    void cerrar() {
        if (cerrada) return;
        cerrada = true;
        try {
            proceso.entrada().close();
        } catch (IOException e) {
            // ya cerrada
        }
        try {
            if (proceso.vivo() && !proceso.esperarFin(150)) {
                proceso.destruir();
                if (proceso.vivo() && !proceso.esperarFin(200)) {
                    proceso.destruirForzosamente();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proceso.destruirForzosamente();
        } finally {
            if (proceso.vivo()) proceso.destruirForzosamente();
        }
        eofSalida = true;
        cola.offer(FIN_DE_FLUJO);
        hiloSalida.interrupt();
        hiloError.interrupt();
    }

    /** Adaptador sobre {@link Process} del sistema operativo. */
    private static final class ProcesoReal implements ProcesoPiper {
        private final Process proceso;

        ProcesoReal(Process proceso) {
            this.proceso = proceso;
        }

        @Override
        public OutputStream entrada() {
            return proceso.getOutputStream();
        }

        @Override
        public InputStream salida() {
            return proceso.getInputStream();
        }

        @Override
        public InputStream error() {
            return proceso.getErrorStream();
        }

        @Override
        public boolean vivo() {
            return proceso.isAlive();
        }

        @Override
        public boolean esperarFin(long ms) throws InterruptedException {
            return proceso.waitFor(ms, TimeUnit.MILLISECONDS);
        }

        @Override
        public void destruir() {
            proceso.destroy();
        }

        @Override
        public void destruirForzosamente() {
            proceso.destroyForcibly();
        }
    }
}
