package metajavbots.sonido;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Gdx;

import metajavbots.textos.Idioma;

/**
 * VOZ — La voz del locutor robot loco (y la de cada entrenador, con su
 * {@link Timbre}): el texto lo lee Piper en streaming si está ({@link VozPiper}:
 * voz clara con efecto robótico suave y proceso persistente) y, si no,
 * {@link VozRobot} (un sintetizador de formantes hecho aquí, sin internet ni
 * bibliotecas de fuera). Todo corre en un hilo aparte para no parar el juego.
 */
public final class Voz {

    private ExecutorService hilo;
    private volatile boolean cerrada;
    private static final int MAX_CACHE = 24;
    private final Map<String, float[]> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, float[]>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > MAX_CACHE;
                }
            });
    private volatile float[] lista;
    private volatile String claveLista;
    private volatile long pedida;
    private volatile boolean detenerSonidosPendiente;
    private volatile float volumenMaestro = 1f;
    public boolean activa = true;
    public float volumen = 0.9f;
    /** Con Piper, si está (voz humana con efecto robótico suave); si no, la voz propia. */
    public boolean clara = true;
    private volatile VozPiper piper;
    private volatile SalidaDeAudio salidaStreaming;
    private volatile boolean salidaExterna;
    private volatile boolean hablandoStreaming;
    private volatile long finVozRespaldoNanos;

    /** Indica si la voz del locutor o del entrenador está hablando en este instante (para atenuar la música). */
    public boolean estaHablando() {
        if (!activa || volumen <= 0f || volumenMaestro <= 0f) return false;
        return hablandoStreaming || System.nanoTime() < finVozRespaldoNanos;
    }

    /** Dónde está Piper (000KI_Local/piper, junto a los modelos de IA). */
    public void usarPiper(Path carpeta) {
        VozPiper actual = piper;
        if (actual != null && Objects.equals(actual.carpeta(), carpeta)) return;
        usarPiper(carpeta == null ? null : new VozPiper(carpeta));
    }

    /** Asigna directamente una instancia de {@link VozPiper} (cerrando la anterior si cambia). */
    public void usarPiper(VozPiper nuevo) {
        VozPiper anterior = piper;
        if (anterior == nuevo) return;
        piper = nuevo;
        if (anterior != null) anterior.cerrar();
    }

    /** Inyecta una salida de audio personalizada para streaming (pruebas sin hardware de audio). */
    public void usarSalida(SalidaDeAudio salida) {
        SalidaDeAudio anterior = salidaStreaming;
        boolean eraExterna = salidaExterna;
        salidaStreaming = salida;
        salidaExterna = salida != null;
        if (anterior != null && !eraExterna) anterior.cerrar();
    }

    /** ¿Hay voz clara para ese idioma? */
    public boolean hayVozClara(Idioma idioma) {
        VozPiper p = piper;
        return p != null && p.disponible(idioma);
    }

    private synchronized ExecutorService hilo() {
        if (cerrada) return null;
        if (hilo == null) {
            hilo = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "voz-del-locutor");
                t.setDaemon(true);
                return t;
            });
        }
        return hilo.isShutdown() ? null : hilo;
    }

    /** Precarga en segundo plano el proceso de Piper del idioma y timbre activos sin bloquear la UI. */
    public void precargar(Idioma idioma, Timbre timbre) {
        if (!activa || !clara || idioma == null) return;
        VozPiper p = piper;
        if (p == null || !p.disponible(idioma)) return;
        ExecutorService exec = hilo();
        if (exec == null) return;
        Timbre t = timbre == null ? Timbre.LOCUTOR : timbre;
        exec.submit(() -> {
            if (!activa || !clara || piper != p) return;
            p.precargar(idioma, t);
        });
    }

    /** Detiene inmediatamente cualquier voz en reproducción o síntesis pendiente. */
    public void detener() {
        pedida++;
        lista = null;
        hablandoStreaming = false;
        finVozRespaldoNanos = 0L;
        detenerSonidosPendiente = true;
        detenerStreamingActual();
    }

    /** Pide decir un texto (si ya está diciendo otro, este lo sustituye). */
    public void decir(String texto, Idioma idioma) {
        decir(texto, idioma, Timbre.LOCUTOR);
    }

    /** Lo mismo, con otra voz (la de cada entrenador: tutor/Entrenador.timbre). */
    public void decir(String texto, Idioma idioma, Timbre timbre) {
        if (!activa || volumen <= 0f || texto == null || texto.isBlank()) return;
        long yo = ++pedida;
        lista = null;
        detenerSonidosPendiente = true;
        detenerStreamingActual();
        String limpio = texto.replace("[[", "").replace("]]", "").strip();
        if (limpio.isEmpty()) return;
        Timbre t = timbre == null ? Timbre.LOCUTOR : timbre;
        VozPiper p = clara ? piper : null;
        boolean puedeStreaming = p != null && p.disponible(idioma) && (salidaExterna || Gdx.audio != null);
        if (!puedeStreaming) {
            reproducirRespaldo(limpio, idioma, t, yo);
            return;
        }
        ExecutorService exec = hilo();
        if (exec == null) return;
        exec.submit(() -> {
            if (yo != pedida || !activa || volumenEfectivo() <= 0f) return;
            SalidaDeAudio salida = obtenerSalidaStreaming();
            boolean ok = false;
            hablandoStreaming = true;
            try {
                ok = salida != null && p.hablar(limpio, idioma, t, salida,
                        () -> activa ? volumenEfectivo() : 0.0,
                        () -> yo != pedida || !activa || volumenEfectivo() <= 0f);
            } finally {
                hablandoStreaming = false;
            }
            if (!ok && yo == pedida && activa) {
                sintetizarVozRobot(limpio, idioma, t, yo);
            }
        });
    }

    /** Cada fotograma: vigila el silencio y reproduce cualquier frase de respaldo lista. */
    public void fotograma(Sonidos sonidos) {
        volumenMaestro = sonidos != null ? sonidos.volumen : 1f;
        if (detenerSonidosPendiente) {
            detenerSonidosPendiente = false;
            if (sonidos != null) sonidos.detenerVoz();
        }
        if (!activa || volumen <= 0f || volumenMaestro <= 0f) {
            if (lista != null || salidaStreaming != null) {
                lista = null;
                hablandoStreaming = false;
                finVozRespaldoNanos = 0L;
                detenerStreamingActual();
                if (sonidos != null) sonidos.detenerVoz();
            }
            return;
        }
        float[] m = lista;
        String clave = claveLista;
        if (m == null) return;
        lista = null;
        long durNanos = (long) (m.length * 1_000_000_000.0 / Sintetizador.FRECUENCIA);
        finVozRespaldoNanos = System.nanoTime() + durNanos;
        if (sonidos != null) sonidos.tocarMuestras("voz:" + clave, m, volumen);
    }

    /** Cancela cualquier síntesis en marcha, cierra Piper y libera el hilo de voz al salir. */
    public void liberar() {
        cerrada = true;
        activa = false;
        pedida++;
        lista = null;
        detenerStreamingActual();
        VozPiper p = piper;
        piper = null;
        if (p != null) p.cerrar();
        ExecutorService exec;
        synchronized (this) {
            exec = hilo;
            hilo = null;
        }
        if (exec != null) {
            exec.shutdownNow();
            try {
                exec.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        SalidaDeAudio s = salidaStreaming;
        salidaStreaming = null;
        if (s != null && !salidaExterna) s.cerrar();
    }

    private void reproducirRespaldo(String limpio, Idioma idioma, Timbre timbre, long yo) {
        String clave = claveRobot(idioma, timbre, limpio);
        float[] cacheada = cache.get(clave);
        if (cacheada != null) {
            claveLista = clave;
            lista = cacheada;
            return;
        }
        ExecutorService exec = hilo();
        if (exec == null) return;
        exec.submit(() -> sintetizarVozRobot(limpio, idioma, timbre, yo));
    }

    private void sintetizarVozRobot(String limpio, Idioma idioma, Timbre timbre, long yo) {
        if (yo != pedida || !activa) return;
        String clave = claveRobot(idioma, timbre, limpio);
        float[] m = cache.get(clave);
        if (m == null) {
            m = Timbre.cambiarTono(VozRobot.decir(limpio, idioma, yo), timbre.tono());
            if (m != null) cache.put(clave, m);
        }
        if (yo == pedida && activa) {
            claveLista = clave;
            lista = m;
        }
    }

    private static String claveRobot(Idioma idioma, Timbre timbre, String limpio) {
        return idioma.name() + "|R|" + timbre + "|" + limpio;
    }

    private float volumenEfectivo() {
        return Math.max(0f, Math.min(1f, volumen * volumenMaestro));
    }

    private SalidaDeAudio obtenerSalidaStreaming() {
        SalidaDeAudio s = salidaStreaming;
        if (s != null) return s;
        if (Gdx.audio == null) return null;
        synchronized (this) {
            if (salidaStreaming == null) salidaStreaming = SalidaDeAudio.crearPcm();
            return salidaStreaming;
        }
    }

    private void detenerStreamingActual() {
        VozPiper p = piper;
        if (p != null) p.cancelar();
        SalidaDeAudio s = salidaStreaming;
        if (s != null) s.detenerInmediato();
    }
}
