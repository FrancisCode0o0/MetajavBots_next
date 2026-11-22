package metajavbots.pruebas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import metajavbots.sonido.ProcesadorDeVoz;
import metajavbots.sonido.SalidaDeAudio;
import metajavbots.sonido.SesionPiper;
import metajavbots.sonido.Sintetizador;
import metajavbots.sonido.Timbre;
import metajavbots.sonido.Voz;
import metajavbots.sonido.VozPiper;
import metajavbots.textos.Idioma;

import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

/**
 * PRUEBAS DE VOZ PIPER — Verifica de forma determinista y sin hardware de audio
 * la persistencia del proceso de Piper, el streaming incremental por bloques,
 * la interrupción inmediata, la recuperación tras caída y el cierre sin procesos huérfanos.
 */
final class PruebasDeVozPiper {

    private PruebasDeVozPiper() {
    }

    static void apuntar() {
        casoReutilizacionYLru();
        casoStreamingAntesDelFin();
        casoInterrupcionYDetencion();
        casoRecuperacionYCierreSinHuerfanos();
        casoProcesadorIncrementalYRespaldo();
    }

    private static void casoReutilizacionYLru() {
        Pruebas.caso("voz piper: reutiliza la sesión persistente en frases seguidas y acota sesiones LRU", () -> {
            List<ProcesoSimulado> creados = new CopyOnWriteArrayList<>();
            VozPiper piper = VozPiper.paraPruebas(Path.of("piper.exe"), Path.of("es_test.onnx"), 22050, (exe, mod, largo, dir) -> {
                ProcesoSimulado p = new ProcesoSimulado(2, 0, null, -1);
                creados.add(p);
                return p;
            });
            SalidaGrabadora salida = new SalidaGrabadora();
            try {
                for (int i = 1; i <= 3; i++) {
                    boolean ok = piper.hablar("Frase " + i, Idioma.ES, Timbre.LOCUTOR, salida, () -> 0.8, () -> false);
                    cierto(ok, "frase " + i + " reproducida");
                }
                igual(1, creados.size(), "un solo proceso Piper para 3 frases consecutivas");
                igual(1, piper.sesionesActivas(), "una sesión activa");
                cierto(salida.bytesTotales.get() > 0, "se escribió audio PCM");

                // 3 timbres con distinta escala de duración: el límite MAX_SESIONES (2) debe cerrar la más antigua.
                Timbre t1 = new Timbre(0.85, 1.0, 0.5);
                Timbre t2 = new Timbre(1.15, 0.9, 0.3);
                cierto(piper.hablar("Entrenador uno", Idioma.ES, t1, salida, () -> 0.8, () -> false), "timbre 1");
                cierto(piper.hablar("Entrenador dos", Idioma.ES, t2, salida, () -> 0.8, () -> false), "timbre 2");
                cierto(piper.sesionesActivas() <= VozPiper.MAX_SESIONES, "sesiones acotadas por MAX_SESIONES");
                falso(creados.get(0).vivo(), "la sesión sustituida por LRU se cerró limpiamente");
            } finally {
                piper.cerrar();
            }
            igual(0, piper.sesionesActivas(), "cero sesiones tras cerrar");
            for (ProcesoSimulado p : creados) falso(p.vivo(), "ningún proceso queda vivo");
        });
    }

    private static void casoStreamingAntesDelFin() {
        Pruebas.caso("voz piper: entrega el primer bloque de audio en streaming antes de acabar la frase", () -> {
            CountDownLatch liberarSegundoBloque = new CountDownLatch(1);
            CountDownLatch primerBloqueRecibido = new CountDownLatch(1);
            CountDownLatch fraseTerminada = new CountDownLatch(1);
            VozPiper piper = VozPiper.paraPruebas(Path.of("piper.exe"), Path.of("es_test.onnx"), 22050,
                    (exe, mod, largo, dir) -> new ProcesoSimulado(2, 0, liberarSegundoBloque, -1));
            SalidaGrabadora salida = new SalidaGrabadora();
            salida.alPrimerBloque = primerBloqueRecibido::countDown;
            AtomicBoolean resultado = new AtomicBoolean();
            Thread reproductor = new Thread(() -> {
                resultado.set(piper.hablar("Frase larga en dos bloques", Idioma.ES, Timbre.LOCUTOR, salida, () -> 0.9, () -> false));
                fraseTerminada.countDown();
            }, "prueba-streaming");
            try {
                reproductor.start();
                cierto(primerBloqueRecibido.await(3, TimeUnit.SECONDS), "el primer bloque llegó antes de liberar el segundo");
                falso(fraseTerminada.getCount() == 0, "la síntesis completa aún no había terminado al llegar el primer bloque");
                liberarSegundoBloque.countDown();
                cierto(fraseTerminada.await(3, TimeUnit.SECONDS), "la frase terminó limpiamente tras liberar el segundo bloque");
                cierto(resultado.get(), "hablar devolvió true");
                cierto(salida.bloques.get() >= 2, "llegaron ambos bloques");
            } finally {
                liberarSegundoBloque.countDown();
                piper.cerrar();
                reproductor.join(2000);
            }
        });
    }

    private static void casoInterrupcionYDetencion() {
        Pruebas.caso("voz piper: interrupción inmediata al detener o al entrar una frase nueva", () -> {
            CountDownLatch primerBloque = new CountDownLatch(1);
            CountDownLatch puertaSegundaFrase = new CountDownLatch(1);
            AtomicInteger frasesVistas = new AtomicInteger();
            VozPiper piper = VozPiper.paraPruebas(Path.of("piper.exe"), Path.of("es_test.onnx"), 22050,
                    (exe, mod, largo, dir) -> new ProcesoSimulado(3, 60, null, -1, frasesVistas));
            SalidaGrabadora salida = new SalidaGrabadora();
            salida.alPrimerBloque = primerBloque::countDown;
            Voz voz = new Voz();
            voz.usarPiper(piper);
            voz.usarSalida(salida);
            try {
                voz.decir("Primera frase que será interrumpida", Idioma.ES, Timbre.LOCUTOR);
                cierto(primerBloque.await(3, TimeUnit.SECONDS), "empezó a sonar la primera frase");
                voz.detener();
                cierto(salida.detenciones.get() >= 1, "detener cortó la salida inmediatamente");

                int bloquesAntes = salida.bloques.get();
                salida.alVaciar = puertaSegundaFrase::countDown;
                voz.decir("Segunda frase completa", Idioma.ES, Timbre.LOCUTOR);
                cierto(puertaSegundaFrase.await(3, TimeUnit.SECONDS), "la segunda frase terminó sin bloqueo");
                cierto(salida.bloques.get() > bloquesAntes, "la segunda frase produjo audio");
                cierto(frasesVistas.get() >= 2, "ambas frases pasaron por el proceso");
            } finally {
                voz.liberar();
            }
            igual(0, piper.sesionesActivas(), "liberar cerró las sesiones");
        });
    }

    private static void casoRecuperacionYCierreSinHuerfanos() {
        Pruebas.caso("voz piper: se recupera tras caída inesperada del proceso y cierra sin huérfanos", () -> {
            List<ProcesoSimulado> creados = new CopyOnWriteArrayList<>();
            AtomicInteger intentos = new AtomicInteger();
            VozPiper piper = VozPiper.paraPruebas(Path.of("piper.exe"), Path.of("es_test.onnx"), 22050, (exe, mod, largo, dir) -> {
                int num = intentos.incrementAndGet();
                // El primer proceso muere inesperadamente en la 2.ª frase; el segundo funciona bien.
                ProcesoSimulado p = new ProcesoSimulado(2, 0, null, num == 1 ? 2 : -1);
                creados.add(p);
                return p;
            });
            SalidaGrabadora salida = new SalidaGrabadora();
            try {
                cierto(piper.hablar("Frase uno", Idioma.ES, Timbre.LOCUTOR, salida, () -> 0.8, () -> false), "frase 1 ok");
                cierto(piper.hablar("Frase dos tras caída", Idioma.ES, Timbre.LOCUTOR, salida, () -> 0.8, () -> false), "frase 2 recuperada");
                igual(2, creados.size(), "se relanzó Piper automáticamente tras la caída");
                falso(creados.get(0).vivo(), "el proceso caído quedó cerrado");
                cierto(creados.get(1).vivo(), "el proceso nuevo quedó vivo y listo");
            } finally {
                piper.cerrar();
            }
            igual(0, piper.sesionesActivas(), "sin sesiones activas al cerrar");
            for (ProcesoSimulado p : creados) falso(p.vivo(), "sin procesos huérfanos");
        });
    }

    private static void casoProcesadorIncrementalYRespaldo() {
        Pruebas.caso("voz piper: procesador incremental mantiene tamaño/rango y degrada limpio sin dispositivo de audio", () -> {
            ProcesadorDeVoz proc = new ProcesadorDeVoz(22050, Sintetizador.FRECUENCIA, new Timbre(1.1, 1.0, 0.7));
            byte[] bloqueImpar = new byte[1025];
            for (int i = 0; i < 512; i++) {
                short v = (short) (Math.sin(i * 0.15) * 16000);
                bloqueImpar[2 * i] = (byte) (v & 0xFF);
                bloqueImpar[2 * i + 1] = (byte) ((v >> 8) & 0xFF);
            }
            byte[] out1 = proc.procesar(bloqueImpar, 0, bloqueImpar.length, 0.8f);
            byte[] out2 = proc.procesar(bloqueImpar, 0, bloqueImpar.length, 0.8f);
            cierto(out1.length > 0 && out1.length % 2 == 0, "primer bloque PCM alineado a 16 bits");
            cierto(out2.length > 0 && out2.length % 2 == 0, "segundo bloque PCM alineado tras byte pendiente");

            // Si el dispositivo de audio falla al abrir, hablar devuelve false sin lanzar excepciones.
            VozPiper piper = VozPiper.paraPruebas(Path.of("piper.exe"), Path.of("es_test.onnx"), 22050,
                    (exe, mod, largo, dir) -> new ProcesoSimulado(1, 0, null, -1));
            SalidaDeAudio rota = new SalidaDeAudio() {
                @Override
                public void abrir(int frecuencia) throws Exception {
                    throw new IOException("Sin dispositivo de audio");
                }

                @Override
                public void escribir(byte[] pcm, int inicio, int longitud) {
                    // no usada
                }

                @Override
                public void detenerInmediato() {
                    // no usada
                }

                @Override
                public void vaciarYEsperar() {
                    // no usada
                }

                @Override
                public void cerrar() {
                    // no usada
                }
            };
            try {
                falso(piper.hablar("Sin audio", Idioma.ES, Timbre.LOCUTOR, rota, () -> 0.8, () -> false), "degrada limpiamente");
            } finally {
                piper.cerrar();
            }
        });
    }

    /** Salida de audio en memoria que registra bloques, detenciones y cierres sin hardware. */
    private static final class SalidaGrabadora implements SalidaDeAudio {
        final AtomicInteger bloques = new AtomicInteger();
        final AtomicInteger bytesTotales = new AtomicInteger();
        final AtomicInteger detenciones = new AtomicInteger();
        volatile Runnable alPrimerBloque;
        volatile Runnable alVaciar;

        @Override
        public void abrir(int frecuencia) {
            // nada que abrir en memoria
        }

        @Override
        public void escribir(byte[] pcm, int inicio, int longitud) {
            bytesTotales.addAndGet(longitud);
            if (bloques.incrementAndGet() == 1 && alPrimerBloque != null) {
                alPrimerBloque.run();
            }
        }

        @Override
        public void detenerInmediato() {
            detenciones.incrementAndGet();
        }

        @Override
        public void vaciarYEsperar() {
            if (alVaciar != null) alVaciar.run();
        }

        @Override
        public void cerrar() {
            // nada que cerrar
        }
    }

    /** Proceso Piper simulado mediante tuberías en memoria para pruebas deterministas. */
    private static final class ProcesoSimulado implements SesionPiper.ProcesoPiper {
        private final PipedOutputStream stdinEscritura = new PipedOutputStream();
        private final PipedInputStream stdinLectura;
        private final PipedOutputStream stdoutEscritura = new PipedOutputStream();
        private final PipedInputStream stdoutLectura;
        private final PipedOutputStream stderrEscritura = new PipedOutputStream();
        private final PipedInputStream stderrLectura;
        private final AtomicBoolean vivo = new AtomicBoolean(true);
        private final Thread hilo;

        ProcesoSimulado(int bloquesPorFrase, long pausaEntreBloquesMs, CountDownLatch puertaTrasPrimero, int morirEnFrase) throws IOException {
            this(bloquesPorFrase, pausaEntreBloquesMs, puertaTrasPrimero, morirEnFrase, new AtomicInteger());
        }

        ProcesoSimulado(int bloquesPorFrase, long pausaEntreBloquesMs, CountDownLatch puertaTrasPrimero, int morirEnFrase,
                        AtomicInteger contadorFrases) throws IOException {
            this.stdinLectura = new PipedInputStream(stdinEscritura, 8192);
            this.stdoutLectura = new PipedInputStream(stdoutEscritura, 32768);
            this.stderrLectura = new PipedInputStream(stderrEscritura, 8192);
            this.hilo = new Thread(() -> ejecutar(bloquesPorFrase, pausaEntreBloquesMs, puertaTrasPrimero, morirEnFrase, contadorFrases),
                    "piper-simulado");
            this.hilo.setDaemon(true);
            this.hilo.start();
        }

        private void ejecutar(int bloquesPorFrase, long pausaMs, CountDownLatch puertaTrasPrimero, int morirEnFrase, AtomicInteger contadorFrases) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(stdinLectura, StandardCharsets.UTF_8))) {
                escribirErr("[piper] [info] Initialized piper\n");
                int nFrase = 0;
                String linea;
                while (vivo.get() && (linea = r.readLine()) != null) {
                    if (linea.isBlank()) continue;
                    nFrase++;
                    contadorFrases.incrementAndGet();
                    if (morirEnFrase > 0 && nFrase == morirEnFrase) break;
                    byte[] pcm = generarBloquePcm(2048);
                    for (int b = 0; b < bloquesPorFrase && vivo.get(); b++) {
                        stdoutEscritura.write(pcm);
                        stdoutEscritura.flush();
                        if (b == 0 && puertaTrasPrimero != null) {
                            puertaTrasPrimero.await(5, TimeUnit.SECONDS);
                        } else if (pausaMs > 0) {
                            Thread.sleep(pausaMs);
                        }
                    }
                    escribirErr("[piper] [info] Real-time factor: 0.09 (infer=0.05 sec, audio=0.50 sec)\n");
                }
            } catch (Exception e) {
                // cierre o interrupción esperada
            } finally {
                vivo.set(false);
                cerrarSilencioso(stdoutEscritura);
                cerrarSilencioso(stderrEscritura);
                cerrarSilencioso(stdinLectura);
            }
        }

        private void escribirErr(String texto) throws IOException {
            stderrEscritura.write(texto.getBytes(StandardCharsets.UTF_8));
            stderrEscritura.flush();
        }

        private static byte[] generarBloquePcm(int bytes) {
            byte[] b = new byte[bytes];
            for (int i = 0; i < bytes / 2; i++) {
                short v = (short) (Math.sin(i * 0.12) * 14000);
                b[2 * i] = (byte) (v & 0xFF);
                b[2 * i + 1] = (byte) ((v >> 8) & 0xFF);
            }
            return b;
        }

        private static void cerrarSilencioso(java.io.Closeable c) {
            try {
                c.close();
            } catch (IOException e) {
                // ya cerrado
            }
        }

        @Override
        public OutputStream entrada() {
            return stdinEscritura;
        }

        @Override
        public InputStream salida() {
            return stdoutLectura;
        }

        @Override
        public InputStream error() {
            return stderrLectura;
        }

        @Override
        public boolean vivo() {
            return vivo.get();
        }

        @Override
        public boolean esperarFin(long ms) throws InterruptedException {
            hilo.join(ms);
            return !vivo.get();
        }

        @Override
        public void destruir() {
            vivo.set(false);
            cerrarSilencioso(stdinEscritura);
            hilo.interrupt();
        }

        @Override
        public void destruirForzosamente() {
            destruir();
        }
    }
}
