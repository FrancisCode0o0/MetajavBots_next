package metajavbots.sonido;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

/**
 * SONIDOS — Toca los sonidos del Sintetizador (y las frases del locutor).
 * Cada sonido se hace la primera vez que se pide, se guarda como WAV en una
 * carpeta temporal y libGDX lo carga. Sin audio (en las pruebas), no suena
 * nada y no pasa nada.
 */
public final class Sonidos {

    public float volumen = 0.8f;
    private final Map<String, Sound> cargados = new HashMap<>();
    private final Map<String, Integer> esteFotograma = new HashMap<>();
    /** Las frases dichas (texto → sonido), las últimas 40. */
    private final LinkedHashMap<String, Sound> frases = new LinkedHashMap<>(16, 0.75f, true);
    private Path carpeta;
    private boolean sinAudio;
    private long semilla = 7;

    private Path carpeta() throws IOException {
        if (carpeta == null) {
            carpeta = Files.createTempDirectory("metajavbots_sonidos");
            carpeta.toFile().deleteOnExit();
        }
        return carpeta;
    }

    private Sound cargar(String nombre, float[] muestras) {
        if (sinAudio || Gdx.audio == null) return null;
        try {
            Path f = carpeta().resolve(nombre.replaceAll("[^A-Za-z0-9_]", "_") + ".wav");
            Files.write(f, Sintetizador.wav(muestras));
            f.toFile().deleteOnExit();
            return Gdx.audio.newSound(Gdx.files.absolute(f.toString()));
        } catch (Throwable e) {
            sinAudio = true;
            return null;
        }
    }

    /** Toca un sonido del sintetizador; «tono» cambia el tono (1 = normal). */
    public void tocar(String nombre, float fuerza, float tono) {
        if (sinAudio || volumen <= 0) return;
        int ya = esteFotograma.getOrDefault(nombre, 0);
        if (ya >= 2) return;   // dos iguales a la vez como mucho: si no, satura
        esteFotograma.put(nombre, ya + 1);
        Sound s = cargados.get(nombre);
        if (s == null && !cargados.containsKey(nombre)) {
            s = cargar(nombre, Sintetizador.hacer(nombre, semilla++));
            cargados.put(nombre, s);
        }
        if (s != null) s.play(Math.max(0, Math.min(1, fuerza * volumen)), tono, 0);
    }

    public void tocar(String nombre, float fuerza) {
        tocar(nombre, fuerza, 1);
    }

    /** Toca unas muestras ya hechas (la voz del locutor), y las guarda por si se repite la frase. */
    public void tocarMuestras(String clave, float[] muestras, float fuerza) {
        if (sinAudio || volumen <= 0 || muestras == null) return;
        Sound s = frases.get(clave);
        if (s == null) {
            s = cargar("voz_" + Integer.toHexString(clave.hashCode()), muestras);
            if (s == null) return;
            frases.put(clave, s);
            while (frases.size() > 40) {
                String vieja = frases.keySet().iterator().next();
                Sound v = frases.remove(vieja);
                if (v != null) v.dispose();
            }
        }
        s.play(Math.max(0, Math.min(1, fuerza)));
    }

    public void fotograma(float dt) {
        esteFotograma.clear();
    }

    public void liberar() {
        for (Sound s : cargados.values()) if (s != null) s.dispose();
        for (Sound s : frases.values()) if (s != null) s.dispose();
        cargados.clear();
        frases.clear();
    }
}
