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
    private static final float DURACION_FUNDIDO = 0.25f;
    private static final float FACTOR_VOZ = 0.38f;
    private static final float VEL_BAJADA_VOZ = 6.0f;
    private static final float VEL_SUBIDA_VOZ = 2.5f;
    private static final java.util.Set<String> SIN_BUCLE = java.util.Set.of(
            "victoria", "derrota", "escena_milagro", "escena_victoria", "escena_derrota");

    public boolean activa = true;
    public float volumen = 0.5f;

    private String pedido = "";
    private volatile String sonando = "";
    private String pendiente = "";
    private boolean fundiendoSalida;
    private float factorFundido = 1f;
    private float factorVoz = 1f;
    private float volumenPuesto = -1f;
    private Map<String, String> deSerie;
    private Music archivo;
    private Sequencer secuenciador;
    private Synthesizer sintetizador;
    private ExecutorService hilo;

    /** Indica si un tema se repite en bucle o es una fanfarria/escena breve de una sola pasada. */
    public static boolean esBucle(String tema) {
        return tema != null && !SIN_BUCLE.contains(tema);
    }

    /** Pide un tema (se llama en cada fotograma; solo cambia algo si es otro). */
    public void tocar(String tema) {
        pedido = tema == null ? "" : tema;
    }

    /** Una vez por fotograma (compatibilidad): sin voz activa y paso estándar de 1/60 s. */
    public void fotograma() {
        fotograma(1f / 60f, false);
    }

    /**
     * Una vez por fotograma: aplica fundido corto al cambiar de tema, atenúa la música
     * suavemente mientras habla el locutor o el entrenador y actualiza el volumen.
     */
    public void fotograma(float dt, boolean vozActiva) {
        float paso = Math.max(0.001f, Math.min(0.1f, dt));
        float metaVoz = vozActiva ? FACTOR_VOZ : 1f;
        float velVoz = vozActiva ? VEL_BAJADA_VOZ : VEL_SUBIDA_VOZ;
        if (factorVoz < metaVoz) factorVoz = Math.min(metaVoz, factorVoz + paso * velVoz);
        else if (factorVoz > metaVoz) factorVoz = Math.max(metaVoz, factorVoz - paso * velVoz);

        String quiere = activa && volumen > 0 ? pedido : "";
        if (!quiere.equals(sonando)) {
            if (sonando.isEmpty()) {
                factorFundido = 0f;
                fundiendoSalida = false;
                pendiente = "";
                cambiar(quiere);
            } else {
                pendiente = quiere;
                fundiendoSalida = true;
            }
        } else if (fundiendoSalida) {
            fundiendoSalida = false;
            pendiente = "";
        }

        if (fundiendoSalida) {
            factorFundido = Math.max(0f, factorFundido - paso / DURACION_FUNDIDO);
            if (factorFundido <= 0f) {
                fundiendoSalida = false;
                String siguiente = pendiente;
                pendiente = "";
                cambiar(siguiente);
            }
        } else if (!sonando.isEmpty() && factorFundido < 1f) {
            factorFundido = Math.min(1f, factorFundido + paso / DURACION_FUNDIDO);
        }

        float ef = volumenEfectivo();
        if (Gdx.audio != null && (Math.abs(ef - volumenPuesto) > 0.005f || (ef == 0f && volumenPuesto != 0f))) {
            ponerVolumen(ef);
        }
    }

    /** Volumen efectivo actual tras aplicar fundido de transición y atenuación por voz. */
    public float volumenEfectivo() {
        if (!activa || sonando.isEmpty() || volumen <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, volumen * factorFundido * factorVoz));
    }

    /** Tema actualmente activo en el reproductor (vacío si está en silencio). */
    public String temaActual() {
        return sonando;
    }

    /** Indica si hay un fundido de salida en curso hacia otro tema. */
    public boolean cambiandoTema() {
        return fundiendoSalida;
    }

    /** Número de pistas activas en reproducción simultánea (nunca más de 1). */
    public int pistasActivas() {
        int n = archivo != null ? 1 : 0;
        if (secuenciador != null && secuenciador.isRunning()) n++;
        return n;
    }

    /** Lo apaga todo y cierra el sintetizador (al salir del juego). */
    public void liberar() {
        activa = false;
        pedido = "";
        pendiente = "";
        fundiendoSalida = false;
        sonando = "";
        parar();
        if (hilo != null) {
            if (!hilo.isShutdown()) {
                hilo.submit(this::cerrarMidi);
                hilo.shutdown();
            }
            try {
                hilo.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        cerrarMidi();
    }

    private void cambiar(String tema) {
        parar();
        sonando = tema;
        volumenPuesto = -1f;
        if (tema.isEmpty()) {
            // Sin música, ni el sintetizador queda abierto.
            if (hilo != null && !hilo.isShutdown()) hilo.submit(this::cerrarMidi);
            return;
        }
        if (Gdx.audio == null) return;
        Path propio = personal(tema);
        if (propio != null && !propio.toString().toLowerCase(Locale.ROOT).endsWith(".mid")) {
            try {
                archivo = Gdx.audio.newMusic(Gdx.files.absolute(propio.toAbsolutePath().toString()));
                archivo.setLooping(esBucle(tema));
                archivo.setVolume(volumenEfectivo());
                archivo.play();
            } catch (RuntimeException e) {
                if (archivo != null) {
                    try {
                        archivo.dispose();
                    } catch (RuntimeException ignorado) {
                        // ya falló al abrir o reproducir
                    }
                }
                archivo = null;
            }
            return;
        }
        String serie = elegir(deSerie().get(tema));
        if (propio == null && serie == null) return;
        if (hilo == null) {
            hilo = Executors.newSingleThreadExecutor(r -> {
                Thread h = new Thread(r, "musica");
                h.setDaemon(true);
                return h;
            });
        }
        if (!hilo.isShutdown()) hilo.submit(() -> empezarMidi(tema, propio, serie));
    }

    /** En el hilo de la música: abre el sintetizador (la primera vez) y pone el MIDI en bucle o pasada única. */
    private void empezarMidi(String tema, Path propio, String serie) {
        if (!tema.equals(sonando)) return;   // ya se pidió otro mientras tanto
        try (InputStream in = propio != null ? Files.newInputStream(propio) : Musica.class.getResourceAsStream("/musica/" + serie)) {
            if (in == null) return;
            Sequence s = MidiSystem.getSequence(new BufferedInputStream(in));
            if (secuenciador == null) {
                sintetizador = MidiSystem.getSynthesizer();
                sintetizador.open();
                Path sf = soundfont();
                if (sf != null) {
                    try {
                        javax.sound.midi.Soundbank banco = MidiSystem.getSoundbank(sf.toFile());
                        if (sintetizador.isSoundbankSupported(banco)) {
                            if (sintetizador.getDefaultSoundbank() != null) sintetizador.unloadAllInstruments(sintetizador.getDefaultSoundbank());
                            sintetizador.loadAllInstruments(banco);
                        }
                    } catch (Exception e) {
                        // un .sf2 roto: los instrumentos de Java
                    }
                }
                secuenciador = MidiSystem.getSequencer(false);
                secuenciador.open();
                secuenciador.getTransmitter().setReceiver(sintetizador.getReceiver());
            }
            secuenciador.stop();
            silenciar();
            secuenciador.setSequence(s);
            secuenciador.setLoopCount(esBucle(tema) ? Sequencer.LOOP_CONTINUOUSLY : 0);
            secuenciador.setTickPosition(0);
            volumenMidi(volumenEfectivo());
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
        if (hilo != null && !hilo.isShutdown()) {
            hilo.submit(() -> {
                if (secuenciador != null && secuenciador.isRunning()) secuenciador.stop();
                silenciar();
            });
        }
    }

    private void ponerVolumen(float v) {
        volumenPuesto = v;
        if (archivo != null) archivo.setVolume(v);
        if (hilo != null && !hilo.isShutdown()) hilo.submit(() -> volumenMidi(v));
    }

    private void volumenMidi(float v) {
        if (sintetizador == null) return;
        for (MidiChannel c : sintetizador.getChannels()) if (c != null) c.controlChange(7, Math.round(v * 110));
    }

    private void silenciar() {
        if (sintetizador == null) return;
        for (MidiChannel c : sintetizador.getChannels()) if (c != null) c.allNotesOff();
    }

    private synchronized void cerrarMidi() {
        try {
            if (secuenciador != null) {
                if (secuenciador.isRunning()) secuenciador.stop();
                secuenciador.close();
            }
        } catch (RuntimeException e) {
            // ya estaba cerrado
        }
        try {
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

    /**
     * De «a.mid, b.mid, c.mid» (varias piezas para un mismo tema, para que no se repita), una al azar; de una sola,
     * esa; null si no hay.
     */
    static String elegir(String lista) {
        if (lista == null) return null;
        String[] l = lista.split(",");
        return l[(int) (Math.random() * l.length)].strip();
    }

    /**
     * Un SoundFont (.sf2) libre para que el MIDI suene mejor: el primero que haya en musica_personal/ o en
     * ~/.metajavbots/ (no va en el repositorio: pesa). Sin él, los instrumentos de Java.
     */
    static Path soundfont() {
        for (Path dir : new Path[]{carpetaPersonal, Paths.get(System.getProperty("user.home"), ".metajavbots")}) {
            if (!Files.isDirectory(dir)) continue;
            try (java.util.stream.Stream<Path> s = Files.list(dir)) {
                java.util.Optional<Path> sf = s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".sf2")).sorted().findFirst();
                if (sf.isPresent()) return sf.get();
            } catch (java.io.IOException e) {
                // se mira la siguiente
            }
        }
        return null;
    }

    /** El archivo personal de ese tema (apuntado en musica_personal/temas.txt o con el nombre del tema), o null. */
    public static Path personal(String tema) {
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
