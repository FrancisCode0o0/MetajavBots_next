package metajavbots.sonido;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import metajavbots.textos.Idioma;

/**
 * VOZ PIPER — La voz clara del locutor: Piper (un programa libre de voz, MIT,
 * que va aparte como KoboldCpp, en 000KI_Local/piper) lee el texto con una
 * voz humana en cada idioma (voces CC0: davefx, thorsten y joe; docs/CEREBRO.md)
 * y encima va un efecto robótico suave, que no tapa las palabras. Si no está
 * o falla, el locutor usa la voz propia del juego (VozRobot).
 */
public final class VozPiper {

    /** Cuánto puede tardar Piper en leer una frase. */
    private static final long ESPERA_MS = 20_000;
    private static final Pattern FRECUENCIA_DEL_MODELO = Pattern.compile("\"sample_rate\"\\s*:\\s*(\\d+)");

    private final Path carpeta;

    /** La carpeta de Piper (con piper.exe y voces/). */
    public VozPiper(Path carpeta) {
        this.carpeta = carpeta;
    }

    /** El programa de Piper, o null si no está. */
    Path programa() {
        for (String n : new String[]{"piper.exe", "piper"}) {
            Path p = carpeta.resolve(n);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    /** La voz de un idioma (la primera voces/<es|de|en>_*.onnx), o null. */
    Path voz(Idioma idioma) {
        Path voces = carpeta.resolve("voces");
        if (!Files.isDirectory(voces)) return null;
        String prefijo = idioma.codigo() + "_";
        try (Stream<Path> s = Files.list(voces)) {
            List<Path> l = s.filter(p -> p.getFileName().toString().startsWith(prefijo) && p.toString().endsWith(".onnx")).sorted().toList();
            return l.isEmpty() ? null : l.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private final java.util.Map<Idioma, Boolean> disponibles = new java.util.concurrent.ConcurrentHashMap<>();

    /** ¿Puede leer en ese idioma? (se mira el disco una vez: los Ajustes lo preguntan en cada fotograma). */
    public boolean disponible(Idioma idioma) {
        return disponibles.computeIfAbsent(idioma, i -> programa() != null && voz(i) != null);
    }

    /** Lee el texto: el audio (mono, a la frecuencia del juego) con el efecto robótico, o null si no ha podido. */
    public float[] decir(String texto, Idioma idioma) {
        return decir(texto, idioma, Timbre.LOCUTOR);
    }

    /** Lee el texto con ese timbre (el tono se cambia después; Piper lee más despacio o más deprisa para compensarlo). */
    public float[] decir(String texto, Idioma idioma, Timbre timbre) {
        Path exe = programa(), voz = voz(idioma);
        if (exe == null || voz == null) return null;
        try {
            String largo = String.format(java.util.Locale.ROOT, "%.3f", 0.95 * timbre.tono() / timbre.velocidad());
            Process p = new ProcessBuilder(exe.toString(), "--model", voz.toString(), "--output_raw", "--length_scale", largo,
                    "--sentence_silence", "0.15").directory(carpeta.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try (OutputStream entrada = p.getOutputStream()) {
                entrada.write(texto.replace('\n', ' ').getBytes(StandardCharsets.UTF_8));
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream salida = p.getInputStream()) {
                salida.transferTo(bytes);
            }
            if (!p.waitFor(ESPERA_MS, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return null;
            }
            byte[] b = bytes.toByteArray();
            if (b.length < 2000) return null;
            return robotico(Timbre.cambiarTono(aFrecuenciaDelJuego(muestras(b), frecuencia(voz)), timbre.tono()), timbre.robot());
        } catch (Exception e) {
            return null;
        }
    }

    /** La frecuencia de la voz (la dice su .onnx.json; si no, la de casi todas las de Piper). */
    public static int frecuencia(Path voz) {
        try {
            Matcher m = FRECUENCIA_DEL_MODELO.matcher(Files.readString(Path.of(voz + ".json"), StandardCharsets.UTF_8));
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception e) {
            // la de siempre
        }
        return 22050;
    }

    /** De bytes (16 bits, little endian) a muestras entre -1 y 1. */
    public static float[] muestras(byte[] b) {
        float[] s = new float[b.length / 2];
        for (int i = 0; i < s.length; i++) s[i] = (short) ((b[2 * i] & 0xFF) | (b[2 * i + 1] << 8)) / 32768f;
        return s;
    }

    /** Cambia la frecuencia (interpolando) a la del juego. */
    public static float[] aFrecuenciaDelJuego(float[] a, int desde) {
        if (desde == Sintetizador.FRECUENCIA) return a;
        double paso = (double) desde / Sintetizador.FRECUENCIA;
        float[] s = new float[(int) (a.length / paso)];
        for (int i = 0; i < s.length; i++) {
            double x = i * paso;
            int k = (int) x;
            double f = x - k;
            s[i] = (float) (a[k] * (1 - f) + a[Math.min(a.length - 1, k + 1)] * f);
        }
        return s;
    }

    /**
     * El aire robótico, suave para que se entienda: un poco de modulación en anillo (el zumbido de máquina) y un eco
     * metálico muy corto. Mucho más ligero que el de la voz propia, que tapaba las consonantes.
     */
    public static float[] robotico(float[] a) {
        return robotico(a, 1);
    }

    /** El aire robótico, con más o menos efecto (0: la voz limpia, solo igualada de volumen; 1: el del locutor). */
    public static float[] robotico(float[] a, double cuanto) {
        int f = Sintetizador.FRECUENCIA;
        float maximo = 1e-6f;
        for (float x : a) maximo = Math.max(maximo, Math.abs(x));
        float[] s = new float[a.length];
        int d1 = (int) (f * 0.006), d2 = (int) (f * 0.013);
        for (int i = 0; i < a.length; i++) {
            double x = a[i] / maximo;
            double y = x * (1 - 0.16 * cuanto + 0.16 * cuanto * Math.sin(2 * Math.PI * 55 * i / f));
            if (i >= d1) y += s[i - d1] * 0.22 * cuanto;
            if (i >= d2) y += s[i - d2] * 0.1 * cuanto;
            s[i] = (float) Math.tanh(y * 1.1) * 0.85f;
        }
        return s;
    }
}
