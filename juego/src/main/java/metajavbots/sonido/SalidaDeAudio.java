package metajavbots.sonido;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * SALIDA DE AUDIO — Destino de audio PCM crudo (16 bits con signo, mono,
 * little endian) para reproducir la voz en streaming conforme llegan los
 * bloques, sin esperar a que termine la frase entera.
 */
public interface SalidaDeAudio {

    /** Prepara la salida para esa frecuencia de muestreo (Hz). */
    void abrir(int frecuencia) throws Exception;

    /** Escribe un bloque de audio PCM en la salida. */
    void escribir(byte[] pcm, int inicio, int longitud);

    /** Corta la reproducción al instante y descarta el audio pendiente en búfer. */
    void detenerInmediato();

    /** Espera a que termine de sonar lo que ya se envió al búfer. */
    void vaciarYEsperar();

    /** Libera los recursos de audio de forma definitiva. */
    void cerrar();

    /** Crea la salida PCM estándar del sistema (SourceDataLine de Java Sound). */
    static SalidaDeAudio crearPcm() {
        return new SalidaLineaPcm();
    }

    /** Implementación de salida en streaming sobre {@link SourceDataLine}. */
    final class SalidaLineaPcm implements SalidaDeAudio {

        private static final int TAMANO_BUFER = 4096;
        private volatile SourceDataLine linea;
        private int frecuenciaActual;

        @Override
        public synchronized void abrir(int frecuencia) throws Exception {
            if (linea != null && linea.isOpen() && frecuenciaActual == frecuencia) {
                if (!linea.isRunning()) linea.start();
                return;
            }
            cerrar();
            AudioFormat formato = new AudioFormat(frecuencia, 16, 1, true, false);
            SourceDataLine nueva = AudioSystem.getSourceDataLine(formato);
            nueva.open(formato, TAMANO_BUFER);
            nueva.start();
            linea = nueva;
            frecuenciaActual = frecuencia;
        }

        @Override
        public void escribir(byte[] pcm, int inicio, int longitud) {
            SourceDataLine l = linea;
            if (l == null || !l.isOpen() || pcm == null || longitud <= 0) return;
            try {
                l.write(pcm, inicio, longitud);
            } catch (RuntimeException e) {
                // la línea se cerró o falló el dispositivo en mitad de la frase
            }
        }

        @Override
        public void detenerInmediato() {
            SourceDataLine l = linea;
            if (l == null || !l.isOpen()) return;
            try {
                l.stop();
                l.flush();
            } catch (RuntimeException e) {
                // ya estaba parada o cerrada
            }
        }

        @Override
        public void vaciarYEsperar() {
            SourceDataLine l = linea;
            if (l == null || !l.isOpen()) return;
            try {
                l.drain();
            } catch (RuntimeException e) {
                // interrumpida o cerrada
            }
        }

        @Override
        public synchronized void cerrar() {
            SourceDataLine l = linea;
            linea = null;
            frecuenciaActual = 0;
            if (l == null) return;
            try {
                l.stop();
                l.flush();
                l.close();
            } catch (RuntimeException e) {
                // ya estaba cerrada
            }
        }
    }
}
