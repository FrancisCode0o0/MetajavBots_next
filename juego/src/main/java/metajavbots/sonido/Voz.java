package metajavbots.sonido;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import metajavbots.textos.Idioma;

/**
 * VOZ — La voz del locutor robot loco (y la de cada entrenador, con su
 * Timbre): el texto lo lee Piper si está
 * (VozPiper: voz clara con un efecto robótico suave) y, si no, VozRobot (un
 * sintetizador de formantes hecho aquí, sin internet y sin bibliotecas de
 * fuera, con efecto metálico). Se sintetiza en un hilo aparte para
 * no parar el juego; cuando está lista, suena.
 */
public final class Voz {

    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "voz-del-locutor");
        t.setDaemon(true);
        return t;
    });
    private static final int MAX_CACHE = 24;
    private final java.util.Map<String, float[]> cache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, float[]>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, float[]> eldest) {
                    return size() > MAX_CACHE;
                }
            });
    private volatile float[] lista;
    private volatile String claveLista;
    private volatile long pedida;
    public boolean activa = true;
    public float volumen = 0.9f;
    /** Con Piper, si está (voz humana con efecto robótico suave); si no, la voz propia. */
    public boolean clara = true;
    private volatile VozPiper piper;

    /** Dónde está Piper (000KI_Local/piper, junto a los modelos de IA). */
    public void usarPiper(java.nio.file.Path carpeta) {
        piper = carpeta == null ? null : new VozPiper(carpeta);
    }

    /** ¿Hay voz clara para ese idioma? */
    public boolean hayVozClara(Idioma idioma) {
        VozPiper p = piper;
        return p != null && p.disponible(idioma);
    }

    /** Pide decir un texto (si ya está diciendo otro, este lo sustituye). */
    public void decir(String texto, Idioma idioma) {
        decir(texto, idioma, Timbre.LOCUTOR);
    }

    /** Lo mismo, con otra voz (la de cada entrenador: tutor/Entrenador.timbre). */
    public void decir(String texto, Idioma idioma, Timbre timbre) {
        if (!activa || texto == null || texto.isBlank()) return;
        long yo = ++pedida;
        String limpio = texto.strip();
        String clave = idioma.name() + "|" + (clara ? "C" : "R") + "|" + timbre + "|" + limpio;
        float[] cacheada = cache.get(clave);
        if (cacheada != null) {
            claveLista = clave;
            lista = cacheada;
            return;
        }
        VozPiper p = clara ? piper : null;
        VozPiper actual = piper;
        if (actual != null) actual.cancelar();
        if (hilo.isShutdown()) return;
        hilo.submit(() -> {
            if (yo != pedida || !activa) return;
            float[] m = p != null ? p.decir(limpio, idioma, timbre) : null;
            if (m == null) m = Timbre.cambiarTono(VozRobot.decir(limpio, idioma, yo), timbre.tono());
            if (m != null) cache.put(clave, m);
            if (yo == pedida && activa) {
                claveLista = clave;
                lista = m;
            }
        });
    }

    /** Cada fotograma: si hay una frase lista, suena. */
    public void fotograma(Sonidos sonidos) {
        float[] m = lista;
        String clave = claveLista;
        if (m == null) return;
        lista = null;
        sonidos.tocarMuestras("voz:" + clave, m, volumen);
    }

    /** Cancela cualquier síntesis en marcha y cierra el hilo de voz al salir. */
    public void liberar() {
        activa = false;
        pedida++;
        lista = null;
        VozPiper p = piper;
        if (p != null) p.cancelar();
        hilo.shutdownNow();
    }
}
