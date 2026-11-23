package metajavbots.escritorio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;

/**
 * GRABADOR DEL TRÁILER — Graba las escenas del tráiler (EscenasDelTrailer) jugadas de verdad y renderizadas por el propio
 * juego, fotograma a fotograma: con una ventana invisible de 1920×1080 y un paso de tiempo fijo de 1/60 s (no depende de lo
 * deprisa que vaya el ordenador, ni de la ventana ni del ratón). Cada fotograma se lee de la pantalla y pasa por una tubería
 * a ffmpeg, que hace un clip por escena en build/trailer/clips/; al lado, los sonidos que tocó el juego en cada fotograma
 * (nombre.sonidos.txt), con los que MontajeDelTrailer mezcla luego el sonido y busca los mejores momentos.
 * «gradlew lwjgl3:trailer» lo graba y lo monta todo (lwjgl3/build.gradle).
 */
public final class GrabadorDelTrailer extends Juego {

    static final int ANCHO = 1920, ALTO = 1080, FPS = 60;
    private final File carpeta;
    private final String ffmpeg;
    private final List<EscenasDelTrailer.Escena> escenas;
    private int actual = -1, fotograma;
    private EscenasDelTrailer.Escena escena;
    private Process proceso;
    private Thread escritor;
    private final BlockingQueue<byte[]> cola = new ArrayBlockingQueue<>(4);
    private static final byte[] FIN = new byte[0];
    private PrintWriter registro;
    private ByteBuffer pixeles;
    private int w, h;
    private volatile IOException fallo;
    /** Se dibuja aquí, siempre de 1920×1080 (la ventana puede salir más pequeña si no cabe en la pantalla). */
    private FrameBuffer lienzo;

    GrabadorDelTrailer(File carpeta, String ffmpeg, List<EscenasDelTrailer.Escena> escenas) {
        this.carpeta = carpeta;
        this.ffmpeg = ffmpeg;
        this.escenas = escenas;
    }

    @Override
    protected float pasoDelFotograma() {
        return 1f / FPS;
    }

    @Override
    public void create() {
        primera = new Pantalla() {
            @Override
            public void fotograma(Juego j, float dt) {
                // negro: la primera escena empieza en el primer fotograma de verdad
            }
        };
        super.create();
        lienzo = new FrameBuffer(Pixmap.Format.RGBA8888, ANCHO, ALTO, false);
        pincel.redimensionar(ANCHO, ALTO);
        metajavbots.progreso.Guardado.carpeta = java.nio.file.Paths.get("build", "partidas_del_trailer");
        Jugador j = new Jugador("Tráiler");
        // CLARIDAD: menú entero y «¿Cómo se juega?» ya visto (si no, la tarjeta saldría encima de cada pantalla).
        j.menuCompleto = true;
        j.comoSeJuegaVisto.addAll(metajavbots.pantallas.ComoSeJuega.todos().keySet());
        j.etiqueta = Etiqueta.JUNIOR_2;
        j.idioma = metajavbots.textos.Idioma.de(System.getProperty("trailer.idioma", "es"));
        j.vozClara = false;
        j.musicaActiva = false;
        j.armasEspeciales = false;
        // Sin los cartelitos de consejos del entrenador flotante: taparían la pelea.
        j.entrenadorActivo = false;
        // Un cerebro que no escucha: el tráiler no arranca ninguna IA.
        j.cerebros[0].tipo = metajavbots.cerebro.Ranura.Tipo.LOCAL;
        j.cerebros[0].direccionLocal = "http://localhost:5999";
        j.robots.add(new Jugador.Robot("Cangrejo", "chispa"));
        ponerJugador(j);
        siguiente();
    }

    @Override
    public void resize(int ancho, int alto) {
        super.resize(ANCHO, ALTO);
    }

    private void siguiente() {
        cerrarClip();
        actual++;
        fotograma = 0;
        if (actual >= escenas.size()) {
            Gdx.app.exit();
            return;
        }
        escena = escenas.get(actual);
        entrenadorFlotante.cerrar();
        try {
            abrirClip(escena.nombre);
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo arrancar ffmpeg (" + ffmpeg + "): " + e.getMessage(), e);
        }
        // Primero se sale de la pantalla anterior (el Taller, al salir, guarda su código en el robot elegido) y luego se
        // prepara la escena: así cada una lleva su robot con su código.
        irSolo(primera);
        irSolo(escena.crear.apply(this));
    }

    private void abrirClip(String nombre) throws IOException {
        w = ANCHO;
        h = ALTO;
        pixeles = ByteBuffer.allocateDirect(w * h * 4);
        File clip = new File(carpeta, nombre + ".mp4");
        List<String> orden = new ArrayList<>(List.of(ffmpeg, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgba",
                "-s", w + "x" + h, "-r", String.valueOf(FPS), "-i", "-",
                // OpenGL lee de abajo arriba: se le da la vuelta.
                "-vf", "vflip",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "12", "-pix_fmt", "yuv420p", clip.getAbsolutePath()));
        proceso = new ProcessBuilder(orden).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        OutputStream salida = new BufferedOutputStream(proceso.getOutputStream(), 1 << 22);
        escritor = new Thread(() -> {
            try {
                for (byte[] b = cola.take(); b != FIN; b = cola.take()) salida.write(b);
                salida.close();
            } catch (IOException e) {
                fallo = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "trailer-ffmpeg");
        escritor.start();
        registro = new PrintWriter(new File(carpeta, nombre + ".sonidos.txt"), StandardCharsets.UTF_8);
        PrintWriter s = registro;
        sonidos.oyente = (n, f, t) -> s.printf(Locale.ROOT, "%d\t%s\t%.3f\t%.3f%n", fotograma, n, f, t);
        System.out.println("trailer: grabando " + nombre + " (" + escena.segundos + " s, " + w + "x" + h + ")");
    }

    private void cerrarClip() {
        if (proceso == null) return;
        try {
            cola.put(FIN);
            escritor.join();
            int fin = proceso.waitFor();
            if (fin != 0 || fallo != null) throw new IllegalStateException("ffmpeg acabó mal (" + fin + "): " + fallo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sonidos.oyente = null;
        registro.close();
        proceso = null;
    }

    @Override
    public void render() {
        if (escena != null && actual < escenas.size()) escena.antes.accept(this, fotograma);
        lienzo.begin();
        super.render();
        if (escena == null || actual >= escenas.size()) {
            lienzo.end();
            return;
        }
        pixeles.clear();
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        Gdx.gl.glReadPixels(0, 0, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixeles);
        byte[] copia = new byte[w * h * 4];
        lienzo.end();
        pixeles.get(copia);
        try {
            cola.put(copia);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (++fotograma >= Math.round(escena.segundos * FPS)) siguiente();
    }

    public static void main(String[] args) {
        File carpeta = new File(args.length > 0 && !args[0].isEmpty() ? args[0] : "build/trailer/clips");
        carpeta.mkdirs();
        String solo = args.length > 1 ? args[1] : "";
        String ffmpeg = args.length > 2 && !args[2].isEmpty() ? args[2] : "ffmpeg";
        List<EscenasDelTrailer.Escena> escenas = new ArrayList<>();
        for (EscenasDelTrailer.Escena e : EscenasDelTrailer.todas()) {
            if (solo.isEmpty() || List.of(solo.split(",")).contains(e.nombre)) escenas.add(e);
        }
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setWindowedMode(ANCHO, ALTO);
        c.setInitialVisible(false);
        c.setResizable(false);
        c.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        c.setForegroundFPS(0);
        c.useVsync(false);
        c.disableAudio(true);
        new Lwjgl3Application(new GrabadorDelTrailer(carpeta, ffmpeg, escenas), c);
    }
}
