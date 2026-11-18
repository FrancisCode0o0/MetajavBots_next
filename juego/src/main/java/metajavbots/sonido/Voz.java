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
    private volatile float[] lista;
    private volatile String textoListo;
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
        VozPiper p = clara ? piper : null;
        hilo.submit(() -> {
            float[] m = p != null ? p.decir(texto, idioma, timbre) : null;
            if (m == null) m = Timbre.cambiarTono(VozRobot.decir(texto, idioma, yo), timbre.tono());
            if (yo == pedida) {
                textoListo = texto;
                lista = m;
            }
        });
    }

    /** Cada fotograma: si hay una frase lista, suena. */
    public void fotograma(Sonidos sonidos) {
        float[] m = lista;
        if (m == null) return;
        lista = null;
        sonidos.tocarMuestras("voz:" + textoListo, m, volumen);
    }
}
