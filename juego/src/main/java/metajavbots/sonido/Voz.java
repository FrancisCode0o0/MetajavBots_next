package metajavbots.sonido;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import metajavbots.textos.Idioma;

/**
 * VOZ — La voz del locutor robot loco: el texto pasa por VozRobot (un
 * sintetizador de formantes hecho aquí, sin internet y sin bibliotecas de
 * fuera) y suena con efecto metálico. Se sintetiza en un hilo aparte para
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

    /** Pide decir un texto (si ya está diciendo otro, este lo sustituye). */
    public void decir(String texto, Idioma idioma) {
        if (!activa || texto == null || texto.isBlank()) return;
        long yo = ++pedida;
        hilo.submit(() -> {
            float[] m = VozRobot.decir(texto, idioma, yo);
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
