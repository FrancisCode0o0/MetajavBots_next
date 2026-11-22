package metajavbots.sonido;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import metajavbots.textos.Idioma;

/**
 * VOZ PIPER — La voz clara del locutor y de los entrenadores: Piper (MIT, en
 * 000KI_Local/piper) lee el texto con una voz humana por idioma (voces CC0:
 * davefx, thorsten y joe; docs/CEREBRO.md). Mantiene un límite acotado de
 * procesos de Piper cargados en memoria entre frases y reproduce el audio en
 * streaming conforme llegan los bloques PCM, aplicando el {@link Timbre} de
 * forma incremental. Si no está o falla, el juego degrada a {@link VozRobot}.
 */
public final class VozPiper {

    /** Máximo de procesos persistentes simultáneos (p. ej. entrenador activo + locutor) para acotar la RAM. */
    public static final int MAX_SESIONES = 2;
    private static final Pattern FRECUENCIA_DEL_MODELO = Pattern.compile("\"sample_rate\"\\s*:\\s*(\\d+)");
    private static final Map<Path, Integer> FRECUENCIAS = new ConcurrentHashMap<>();
    private static final Set<VozPiper> INSTANCIAS = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(VozPiper::cerrarTodasLasInstancias, "cerrar-piper"));
    }

    /** Clave de reutilización de una sesión persistente de Piper (modelo de voz + velocidad). */
    private record ClaveSesion(Path voz, String largo) {
    }

    private final Path carpeta;
    private final SesionPiper.FabricaDeProcesos fabrica;
    private final Path ejecutableFijo;
    private final Path modeloFijo;
    private final int frecuenciaFija;
    private final Map<Idioma, Path> vocesPorIdioma = new ConcurrentHashMap<>();
    private final Map<Idioma, Boolean> disponibles = new ConcurrentHashMap<>();
    private final Map<ClaveSesion, SesionPiper> sesiones = new LinkedHashMap<>(4, 0.75f, true);
    private final AtomicLong generacion = new AtomicLong();
    private volatile boolean cerrada;

    /** La carpeta de Piper (con piper.exe y voces/). */
    public VozPiper(Path carpeta) {
        this(carpeta, SesionPiper.fabricaReal(), null, null, 0);
    }

    /** Constructor con fábrica de procesos inyectable (útil para pruebas deterministas). */
    public VozPiper(Path carpeta, SesionPiper.FabricaDeProcesos fabrica) {
        this(carpeta, fabrica == null ? SesionPiper.fabricaReal() : fabrica, null, null, 0);
    }

    private VozPiper(Path carpeta, SesionPiper.FabricaDeProcesos fabrica, Path ejecutableFijo, Path modeloFijo, int frecuenciaFija) {
        this.carpeta = carpeta;
        this.fabrica = fabrica;
        this.ejecutableFijo = ejecutableFijo;
        this.modeloFijo = modeloFijo;
        this.frecuenciaFija = frecuenciaFija;
        INSTANCIAS.add(this);
    }

    /** Crea una instancia de pruebas sin depender de archivos ni binarios reales de Piper en disco. */
    public static VozPiper paraPruebas(Path ejecutableFicticio, Path modeloFicticio, int frecuencia, SesionPiper.FabricaDeProcesos fabrica) {
        Path base = ejecutableFicticio.getParent() != null ? ejecutableFicticio.getParent() : Path.of(".");
        return new VozPiper(base, fabrica, ejecutableFicticio, modeloFicticio, frecuencia);
    }

    /** Cierra todas las instancias activas de Piper (invocado por el shutdown hook de seguridad). */
    public static void cerrarTodasLasInstancias() {
        for (VozPiper instancia : List.copyOf(INSTANCIAS)) {
            instancia.cerrar();
        }
    }

    /** La carpeta configurada de Piper. */
    public Path carpeta() {
        return carpeta;
    }

    /** Número de procesos persistentes activos actualmente en esta instancia. */
    public synchronized int sesionesActivas() {
        int vivas = 0;
        for (SesionPiper s : sesiones.values()) {
            if (s.viva()) vivas++;
        }
        return vivas;
    }

    /** El programa de Piper, o null si no está. */
    Path programa() {
        if (ejecutableFijo != null) return ejecutableFijo;
        if (carpeta == null) return null;
        for (String n : new String[]{"piper.exe", "piper"}) {
            Path p = carpeta.resolve(n);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    /** La voz de un idioma (la primera voces/<es|de|en>_*.onnx), o null. */
    Path voz(Idioma idioma) {
        if (modeloFijo != null) return modeloFijo;
        if (carpeta == null || idioma == null) return null;
        Path cacheada = vocesPorIdioma.get(idioma);
        if (cacheada != null && Files.isRegularFile(cacheada)) return cacheada;
        Path voces = carpeta.resolve("voces");
        if (!Files.isDirectory(voces)) return null;
        String prefijo = idioma.codigo() + "_";
        try (Stream<Path> s = Files.list(voces)) {
            List<Path> l = s.filter(p -> p.getFileName().toString().startsWith(prefijo) && p.toString().endsWith(".onnx")).sorted().toList();
            if (l.isEmpty()) return null;
            vocesPorIdioma.put(idioma, l.get(0));
            return l.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /** ¿Puede leer en ese idioma? (se mira el disco una vez: los Ajustes lo preguntan en cada fotograma). */
    public boolean disponible(Idioma idioma) {
        if (cerrada || idioma == null) return false;
        if (ejecutableFijo != null && modeloFijo != null) return true;
        return disponibles.computeIfAbsent(idioma, i -> programa() != null && voz(i) != null);
    }

    /** Interrumpe la reproducción o síntesis en curso sin destruir las sesiones persistentes sanas. */
    public void cancelar() {
        generacion.incrementAndGet();
    }

    /** Precarga en memoria el proceso de Piper para ese idioma y timbre (bloquea solo el hilo que lo llama). */
    public void precargar(Idioma idioma, Timbre timbre) {
        if (cerrada || !disponible(idioma)) return;
        Path exe = programa();
        Path voz = voz(idioma);
        if (exe == null || voz == null) return;
        Timbre t = timbre == null ? Timbre.LOCUTOR : timbre;
        obtenerSesion(exe, voz, escalaDuracion(t));
    }

    /**
     * Sintetiza y reproduce la frase en streaming incremental conforme llegan los bloques PCM de Piper.
     * Devuelve {@code true} si se reprodujo o canceló limpiamente, y {@code false} si Piper o el audio fallaron.
     */
    public boolean hablar(String texto, Idioma idioma, Timbre timbre, SalidaDeAudio salida, DoubleSupplier volumen, BooleanSupplier cancelada) {
        if (cerrada || texto == null || texto.isBlank() || salida == null) return false;
        Path exe = programa();
        Path voz = voz(idioma);
        if (exe == null || voz == null) return false;
        Timbre t = timbre == null ? Timbre.LOCUTOR : timbre;
        long gen = generacion.get();
        BooleanSupplier anulada = () -> cerrada || generacion.get() != gen || (cancelada != null && cancelada.getAsBoolean());
        if (anulada.getAsBoolean()) return true;
        try {
            salida.abrir(Sintetizador.FRECUENCIA);
        } catch (Exception e) {
            return false;
        }
        String largo = escalaDuracion(t);
        int freq = frecuenciaDe(voz);
        for (int intento = 0; intento < 2; intento++) {
            if (anulada.getAsBoolean()) {
                salida.detenerInmediato();
                return true;
            }
            SesionPiper sesion = obtenerSesion(exe, voz, largo);
            if (sesion == null) return false;
            ProcesadorDeVoz procesador = new ProcesadorDeVoz(freq, t);
            AtomicBoolean envioAlgo = new AtomicBoolean();
            boolean ok = sesion.sintetizar(texto, trozo -> {
                if (anulada.getAsBoolean()) return;
                float vol = volumen == null ? 1f : (float) volumen.getAsDouble();
                if (vol <= 0f) return;
                byte[] procesado = procesador.procesar(trozo, 0, trozo.length, vol);
                if (procesado.length > 0 && !anulada.getAsBoolean()) {
                    envioAlgo.set(true);
                    salida.escribir(procesado, 0, procesado.length);
                }
            }, anulada);
            if (anulada.getAsBoolean()) {
                salida.detenerInmediato();
                return true;
            }
            if (ok && envioAlgo.get()) {
                salida.vaciarYEsperar();
                return true;
            }
            descartarSesion(new ClaveSesion(voz, largo), sesion);
            if (envioAlgo.get()) return true;
        }
        return false;
    }

    /** Lee el texto: el audio (mono, a la frecuencia del juego) con el efecto robótico, o null si no ha podido. */
    public float[] decir(String texto, Idioma idioma) {
        return decir(texto, idioma, Timbre.LOCUTOR);
    }

    /** Lee el texto con ese timbre reutilizando la sesión persistente, o null si no ha podido. */
    public float[] decir(String texto, Idioma idioma, Timbre timbre) {
        if (cerrada || texto == null || texto.isBlank()) return null;
        Path exe = programa();
        Path voz = voz(idioma);
        if (exe == null || voz == null) return null;
        Timbre t = timbre == null ? Timbre.LOCUTOR : timbre;
        long gen = generacion.get();
        BooleanSupplier anulada = () -> cerrada || generacion.get() != gen;
        String largo = escalaDuracion(t);
        for (int intento = 0; intento < 2; intento++) {
            if (anulada.getAsBoolean()) return null;
            SesionPiper sesion = obtenerSesion(exe, voz, largo);
            if (sesion == null) return null;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            boolean ok = sesion.sintetizar(texto, trozo -> bytes.write(trozo, 0, trozo.length), anulada);
            if (anulada.getAsBoolean()) return null;
            if (ok && bytes.size() >= 2000) {
                return robotico(Timbre.cambiarTono(aFrecuenciaDelJuego(muestras(bytes.toByteArray()), frecuenciaDe(voz)), t.tono()), t.robot());
            }
            descartarSesion(new ClaveSesion(voz, largo), sesion);
        }
        return null;
    }

    /** Cierra de forma determinista todos los procesos hijos de Piper de esta instancia. */
    public void cerrar() {
        cerrada = true;
        generacion.incrementAndGet();
        List<SesionPiper> porCerrar;
        synchronized (this) {
            porCerrar = List.copyOf(sesiones.values());
            sesiones.clear();
        }
        for (SesionPiper s : porCerrar) {
            s.cerrar();
        }
        INSTANCIAS.remove(this);
    }

    private SesionPiper obtenerSesion(Path exe, Path voz, String largo) {
        ClaveSesion clave = new ClaveSesion(voz, largo);
        SesionPiper viejaEvictada = null;
        SesionPiper existente;
        synchronized (this) {
            if (cerrada) return null;
            existente = sesiones.get(clave);
            if (existente != null && existente.viva()) return existente;
            if (existente != null) {
                sesiones.remove(clave);
                viejaEvictada = existente;
            }
        }
        if (viejaEvictada != null) viejaEvictada.cerrar();
        SesionPiper nueva = arrancarSesion(exe, voz, largo);
        if (nueva == null) return null;
        SesionPiper sobrante = null;
        synchronized (this) {
            if (cerrada) {
                sobrante = nueva;
            } else {
                SesionPiper yaPuesta = sesiones.get(clave);
                if (yaPuesta != null && yaPuesta.viva()) {
                    sobrante = nueva;
                    nueva = yaPuesta;
                } else {
                    while (sesiones.size() >= MAX_SESIONES) {
                        Iterator<Map.Entry<ClaveSesion, SesionPiper>> it = sesiones.entrySet().iterator();
                        if (!it.hasNext()) break;
                        Map.Entry<ClaveSesion, SesionPiper> masVieja = it.next();
                        it.remove();
                        sobrante = masVieja.getValue();
                    }
                    sesiones.put(clave, nueva);
                }
            }
        }
        if (sobrante != null) sobrante.cerrar();
        return cerrada ? null : nueva;
    }

    private SesionPiper arrancarSesion(Path exe, Path voz, String largo) {
        try {
            Path dir = carpeta != null ? carpeta : Path.of(".");
            return new SesionPiper(fabrica.arrancar(exe, voz, largo, dir));
        } catch (Exception e) {
            return null;
        }
    }

    private void descartarSesion(ClaveSesion clave, SesionPiper sesion) {
        synchronized (this) {
            if (sesiones.get(clave) == sesion) sesiones.remove(clave);
        }
        sesion.cerrar();
    }

    private int frecuenciaDe(Path voz) {
        return frecuenciaFija > 0 ? frecuenciaFija : frecuencia(voz);
    }

    private static String escalaDuracion(Timbre timbre) {
        return String.format(Locale.ROOT, "%.3f", 0.95 * timbre.tono() / timbre.velocidad());
    }

    /** La frecuencia de la voz (la dice su .onnx.json; si no, la de casi todas las de Piper). */
    public static int frecuencia(Path voz) {
        return FRECUENCIAS.computeIfAbsent(voz, v -> {
            try {
                Matcher m = FRECUENCIA_DEL_MODELO.matcher(Files.readString(Path.of(v + ".json"), StandardCharsets.UTF_8));
                if (m.find()) return Integer.parseInt(m.group(1));
            } catch (Exception e) {
                // la de siempre
            }
            return 22050;
        });
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
