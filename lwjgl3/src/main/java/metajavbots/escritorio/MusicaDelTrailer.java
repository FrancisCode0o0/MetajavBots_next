package metajavbots.escritorio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * MÚSICA DEL TRÁILER — «Último asalto» (res/musica/ultimo_asalto.mid) pasada a sonido con un SoundFont (el GeneralUser GS
 * de ~/.metajavbots/), sin tiempo real: el sintetizador de Java (Gervill) escribe las muestras tan deprisa como puede.
 * Y dónde cae cada pulso (el tempo sale del propio MIDI), para cortar el montaje a tiempo con la música.
 * Gervill da su salida «sin tiempo real» con un método que no está en la API pública (SoftSynthesizer.openStream): por eso
 * la tarea de Gradle abre ese paquete (--add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED).
 */
final class MusicaDelTrailer {

    static final int FRECUENCIA = 44100;
    /** Las muestras, estéreo intercaladas (izquierda, derecha…), entre -1 y 1. */
    final float[] muestras;
    /** Microsegundos por pulso desde cada tick (el mapa de tempo) y la resolución del MIDI. */
    private final TreeMap<Long, Integer> tempos = new TreeMap<>();
    private final int resolucion;
    final double segundosDeMusica;

    MusicaDelTrailer(File midi, File soundFont, double colaEnSegundos) throws Exception {
        Sequence secuencia = MidiSystem.getSequence(midi);
        resolucion = secuencia.getResolution();
        List<MidiEvent> eventos = new ArrayList<>();
        for (Track t : secuencia.getTracks()) for (int i = 0; i < t.size(); i++) eventos.add(t.get(i));
        eventos.sort(Comparator.comparingLong(MidiEvent::getTick));
        tempos.put(0L, 500_000);
        for (MidiEvent e : eventos) {
            if (e.getMessage() instanceof MetaMessage m && m.getType() == 0x51) {
                byte[] d = m.getData();
                tempos.put(e.getTick(), ((d[0] & 0xFF) << 16) | ((d[1] & 0xFF) << 8) | (d[2] & 0xFF));
            }
        }
        segundosDeMusica = segundoDelTick(secuencia.getTickLength());
        Synthesizer sinte = MidiSystem.getSynthesizer();
        AudioFormat formato = new AudioFormat(FRECUENCIA, 16, 2, true, false);
        Method abrir = sinte.getClass().getMethod("openStream", AudioFormat.class, Map.class);
        try (AudioInputStream flujo = (AudioInputStream) abrir.invoke(sinte, formato, null)) {
            if (soundFont != null && soundFont.isFile()) {
                Soundbank banco = MidiSystem.getSoundbank(soundFont);
                if (sinte.getDefaultSoundbank() != null) sinte.unloadAllInstruments(sinte.getDefaultSoundbank());
                sinte.loadAllInstruments(banco);
            }
            Receiver rx = sinte.getReceiver();
            for (MidiEvent e : eventos) {
                if (e.getMessage() instanceof MetaMessage) continue;
                rx.send(e.getMessage(), Math.round(segundoDelTick(e.getTick()) * 1e6));
            }
            int n = (int) Math.round((segundosDeMusica + colaEnSegundos) * FRECUENCIA);
            muestras = leer(flujo, n);
        } finally {
            sinte.close();
        }
    }

    private static float[] leer(InputStream flujo, int marcos) throws IOException {
        byte[] b = new byte[marcos * 4];
        int leidos = 0;
        while (leidos < b.length) {
            int r = flujo.read(b, leidos, b.length - leidos);
            if (r < 0) break;
            leidos += r;
        }
        float[] m = new float[marcos * 2];
        for (int i = 0; i < m.length && i * 2 + 1 < leidos; i++) m[i] = (short) ((b[i * 2] & 0xFF) | (b[i * 2 + 1] << 8)) / 32768f;
        return m;
    }

    /** El segundo en que suena un tick del MIDI (siguiendo los cambios de tempo). */
    double segundoDelTick(long tick) {
        double s = 0;
        long desde = 0;
        int us = tempos.firstEntry().getValue();
        for (Map.Entry<Long, Integer> t : tempos.entrySet()) {
            if (t.getKey() >= tick) break;
            s += (t.getKey() - desde) * (double) us / resolucion / 1e6;
            desde = t.getKey();
            us = t.getValue();
        }
        return s + (tick - desde) * (double) us / resolucion / 1e6;
    }

    /** El segundo en que cae el pulso número «pulso» (puede ser fraccionario). */
    double segundoDelPulso(double pulso) {
        return segundoDelTick(Math.round(pulso * resolucion));
    }
}
