package metajavbots.escritorio;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metajavbots.sonido.Sintetizador;

/**
 * MEZCLA DEL TRÁILER — El sonido de cada versión: la música («Último asalto») y, debajo, los sonidos que tocó el juego en
 * cada toma (los que apuntó el grabador), cada uno donde cae en el montaje, a su fuerza y su tono. Los choques entre robots
 * suenan como en el juego (golpe metálico según la fuerza). Al final, fundido; y un limitador suave para que no sature.
 */
final class MezclaDelTrailer {

    /** Lo fuerte que van los sonidos del juego frente a la música. */
    static final float SONIDOS = 0.32f, MUSICA = 0.95f;
    private final Map<String, float[]> sintetizados = new HashMap<>();

    private MezclaDelTrailer() {
    }

    /** La mezcla de una versión de «segundos» de largo, con un fundido final de «fundido» segundos. */
    static float[] mezclar(MusicaDelTrailer musica, List<MontajeDelTrailer.Toma> tomas, double segundos, double fundido) {
        MezclaDelTrailer m = new MezclaDelTrailer();
        int marcos = (int) Math.round(segundos * MusicaDelTrailer.FRECUENCIA);
        float[] mezcla = new float[marcos * 2];
        for (int i = 0; i < mezcla.length && i < musica.muestras.length; i++) mezcla[i] = musica.muestras[i] * MUSICA;
        for (MontajeDelTrailer.Toma t : tomas) {
            Map<String, Double> ultimo = new HashMap<>();
            for (MontajeDelTrailer.Evento e : t.clip.eventos) {
                if (e.segundo < t.desde || e.segundo >= t.desde + t.segundosDeOrigen()) continue;
                // El mismo sonido muy seguido satura (sobre todo «pared»): como mucho uno cada poco.
                double espera = e.nombre.equals("pared") ? 0.45 : 0.08;
                Double antes = ultimo.get(e.nombre);
                if (antes != null && e.segundo - antes < espera) continue;
                ultimo.put(e.nombre, e.segundo);
                double cae = t.inicio + (e.segundo - t.desde) / t.velocidad;
                float fuerza = e.fuerza * SONIDOS * (e.nombre.equals("pared") ? 0.4f : 1);
                m.poner(mezcla, e.nombre, cae, fuerza, e.tono);
            }
        }
        for (int i = 0; i < marcos; i++) {
            double s = (double) i / MusicaDelTrailer.FRECUENCIA;
            double g = Math.min(1, Math.min(s / 0.4, (segundos - s) / fundido));
            for (int c = 0; c < 2; c++) mezcla[i * 2 + c] = (float) (Math.tanh(mezcla[i * 2 + c] * 1.1) * Math.max(0, g));
        }
        return mezcla;
    }

    /** Un sonido del sintetizador del juego (22050 Hz, mono) puesto en la mezcla (44100 Hz, estéreo), con su tono. */
    private void poner(float[] mezcla, String nombre, double segundo, float fuerza, float tono) {
        float[] s = sintetizados.computeIfAbsent(nombre, n -> Sintetizador.hacer(n, 7));
        if (s == null || s.length == 0) return;
        double paso = (double) Sintetizador.FRECUENCIA * tono / MusicaDelTrailer.FRECUENCIA;
        int inicio = (int) Math.round(segundo * MusicaDelTrailer.FRECUENCIA);
        double p = 0;
        for (int i = inicio; i * 2 + 1 < mezcla.length && p < s.length - 1; i++, p += paso) {
            if (i < 0) continue;
            int k = (int) p;
            float v = (float) (s[k] + (s[k + 1] - s[k]) * (p - k)) * fuerza;
            mezcla[i * 2] += v;
            mezcla[i * 2 + 1] += v;
        }
    }

    /** Escribe la mezcla como WAV de 16 bits, estéreo, 44100 Hz. */
    static void wav(float[] mezcla, File archivo) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(44 + mezcla.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        int datos = mezcla.length * 2;
        b.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(36 + datos)
                .put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 2)
                .putInt(MusicaDelTrailer.FRECUENCIA).putInt(MusicaDelTrailer.FRECUENCIA * 4).putShort((short) 4).putShort((short) 16)
                .put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(datos);
        for (float v : mezcla) b.putShort((short) Math.round(Math.max(-1, Math.min(1, v)) * 32767));
        Files.write(archivo.toPath(), b.array());
    }
}
