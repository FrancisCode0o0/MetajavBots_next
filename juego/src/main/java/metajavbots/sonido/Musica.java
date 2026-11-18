package metajavbots.sonido;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * MÚSICA — El tema que suena de fondo: el del menú, el del combate o el de
 * cada campaña (res/musica/temas.txt). Los temas de serie son MIDI originales
 * y suenan con el sintetizador de Java; lo que Fran ponga en musica_personal/
 * (.mid, .ogg, .mp3 o .wav) suena en su lugar. El MIDI se prepara en otro
 * hilo (el juego no se para nunca) y, con la música quitada, no queda nada
 * abierto gastando.
 */
public final class Musica {

    /** La carpeta de la música personal (junto al juego; no se sube al repositorio). */
    public static Path carpetaPersonal = Paths.get("musica_personal");
    private static final String[] EXTENSIONES = {".mid", ".ogg", ".mp3", ".wav"};

    public boolean activa = true;
    public float volumen = 0.5f;

    private String pedido = "";
    private volatile String sonando = "";
    private float volumenPuesto = -1;
    private Map<String, String> deSerie;
    private Music archivo;
    private Sequencer secuenciador;
    private Synthesizer sintetizador;
    private ExecutorService hilo;

    /** Pide un tema (se llama en cada fotograma; solo cambia algo si es otro). */
    public void tocar(String tema) {
        pedido = tema == null ? "" : tema;
    }

    /** Una vez por fotograma: cambia de tema si hace falta y aplica el volumen. Barato si no cambia nada. */
    public void fotograma() {
        if (Gdx.audio == null) return;
        String quiere = activa && volumen > 0 ? pedido : "";
        if (!quiere.equals(sonando)) cambiar(quiere);
        if (volumen != volumenPuesto) ponerVolumen();
    }

    /** Lo apaga todo y cierra el sintetizador (al salir del juego). */
    public void liberar() {
        parar();
        if (hilo != null) {
            hilo.submit(this::cerrarMidi);
            hilo.shutdown();
        }
    }

    private void cambiar(String tema) {
        parar();
        sonando = tema;
        volumenPuesto = -1;
        if (tema.isEmpty()) {
            // Sin música, ni el sintetizador queda abierto.
            if (hilo != null) hilo.submit(this::cerrarMidi);
            return;
        }
        Path propio = personal(tema);
        if (propio != null && !propio.toString().toLowerCase(Locale.ROOT).endsWith(".mid")) {
            try {
                archivo = Gdx.audio.newMusic(Gdx.files.absolute(propio.toAbsolutePath().toString()));
                archivo.setLooping(true);
                archivo.play();
            } catch (RuntimeException e) {
                archivo = null;
            }
            return;
        }
        String serie = deSerie().get(tema);
        if (propio == null && serie == null) return;
        if (hilo == null) {
            hilo = Executors.newSingleThreadExecutor(r -> {
                Thread h = new Thread(r, "musica");
                h.setDaemon(true);
                return h;
            });
        }
        hilo.submit(() -> empezarMidi(tema, propio, serie));
    }

    /** En el hilo de la música: abre el sintetizador (la primera vez) y pone el MIDI en bucle. */
    private void empezarMidi(String tema, Path propio, String serie) {
        if (!tema.equals(sonando)) return;   // ya se pidió otro mientras tanto
        try (InputStream in = propio != null ? Files.newInputStream(propio) : Musica.class.getResourceAsStream("/musica/" + serie)) {
            if (in == null) return;
            Sequence s = MidiSystem.getSequence(new BufferedInputStream(in));
            if (secuenciador == null) {
                sintetizador = MidiSystem.getSynthesizer();
                sintetizador.open();
                secuenciador = MidiSystem.getSequencer(false);
                secuenciador.open();
                secuenciador.getTransmitter().setReceiver(sintetizador.getReceiver());
            }
            secuenciador.stop();
            silenciar();
            secuenciador.setSequence(s);
            secuenciador.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            secuenciador.setTickPosition(0);
            volumenMidi(volumen);
            if (tema.equals(sonando)) secuenciador.start();
        } catch (Exception | LinkageError e) {
            // Sin sintetizador MIDI en este equipo: el juego sigue sin música.
            cerrarMidi();
        }
    }

    private void parar() {
        if (archivo != null) {
            archivo.stop();
            archivo.dispose();
            archivo = null;
        }
        if (hilo != null) {
            hilo.submit(() -> {
                if (secuenciador != null && secuenciador.isRunning()) secuenciador.stop();
                silenciar();
            });
        }
    }

    private void ponerVolumen() {
        volumenPuesto = volumen;
        float v = volumen;
        if (archivo != null) archivo.setVolume(v);
        if (hilo != null) hilo.submit(() -> volumenMidi(v));
    }

    private void volumenMidi(float v) {
        if (sintetizador == null) return;
        for (MidiChannel c : sintetizador.getChannels()) if (c != null) c.controlChange(7, Math.round(v * 110));
    }

    private void silenciar() {
        if (sintetizador == null) return;
        for (MidiChannel c : sintetizador.getChannels()) if (c != null) c.allNotesOff();
    }

    private void cerrarMidi() {
        try {
            if (secuenciador != null) secuenciador.close();
            if (sintetizador != null) sintetizador.close();
        } catch (RuntimeException e) {
            // ya estaba cerrado
        }
        secuenciador = null;
        sintetizador = null;
    }

    // ================================================================ qué archivo es cada tema

    /** Los temas de serie: tema → archivo de res/musica. */
    public synchronized Map<String, String> deSerie() {
        if (deSerie == null) {
            try (InputStream in = Musica.class.getResourceAsStream("/musica/temas.txt")) {
                deSerie = in == null ? Map.of() : leer(in);
            } catch (IOException e) {
                deSerie = Map.of();
            }
        }
        return deSerie;
    }

    /** El archivo personal de ese tema (apuntado en musica_personal/temas.txt o con el nombre del tema), o null. */
    static Path personal(String tema) {
        if (!Files.isDirectory(carpetaPersonal)) return null;
        Path lista = carpetaPersonal.resolve("temas.txt");
        if (Files.exists(lista)) {
            try (InputStream in = Files.newInputStream(lista)) {
                String nombre = leer(in).get(tema);
                if (nombre != null && Files.isRegularFile(carpetaPersonal.resolve(nombre))) return carpetaPersonal.resolve(nombre);
            } catch (IOException e) {
                // la lista no se puede leer: se prueba por el nombre
            }
        }
        for (String ext : EXTENSIONES) {
            Path p = carpetaPersonal.resolve(tema + ext);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    /** Lee «tema: archivo», con «#» de comentario. */
    static Map<String, String> leer(InputStream in) throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String linea;
        while ((linea = r.readLine()) != null) {
            linea = linea.strip();
            if (linea.isEmpty() || linea.startsWith("#") || !linea.contains(":")) continue;
            int i = linea.indexOf(':');
            m.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
        }
        return m;
    }
}
