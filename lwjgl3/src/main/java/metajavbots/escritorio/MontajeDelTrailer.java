package metajavbots.escritorio;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MONTAJE DEL TRÁILER — Con los clips que grabó GrabadorDelTrailer hace las versiones del tráiler (de 60 a 90 segundos), cada
 * una con su GIF. Los cortes caen en los pulsos de «Último asalto» (cada toma dura un número entero de pulsos) y, en las tomas
 * de «lo mejor», el golpe más fuerte de la toma cae justo en un pulso. Las embestidas entre robots puntúan en contra: salen
 * poco y breves. «gradlew lwjgl3:montarTrailer» (lwjgl3/build.gradle); salen en build/trailer/.
 */
final class MontajeDelTrailer {

    static final double FPS = GrabadorDelTrailer.FPS;
    /** Un sonido apuntado por el grabador: en qué segundo del clip, cuál, su fuerza y su tono. */
    static final class Evento {
        final double segundo;
        final String nombre;
        final float fuerza, tono;

        Evento(double segundo, String nombre, float fuerza, float tono) {
            this.segundo = segundo;
            this.nombre = nombre;
            this.fuerza = fuerza;
            this.tono = tono;
        }

        /** Choque entre dos robots (los del juego llevan el tono cambiado según la fuerza; el de un robot que cae, no). */
        boolean embestida() {
            return nombre.equals("choque") && Math.abs(tono - 1) > 0.001f;
        }

        /** Lo que vale para el tráiler: explosiones y piezas rotas mucho; los disparos, algo; las embestidas, en contra. */
        double peso() {
            if (embestida()) return -4;
            switch (nombre) {
                case "explosion": return 6;
                case "parte_rota": return 5;
                case "golpe_fuerte": return 4;
                case "choque": return 4;
                case "golpe_metal": case "misil": case "ovacion": return 2;
                case "chispa": return 1 + fuerza;
                case "laser": case "laser_pesado": case "disparo": case "ametralladora": case "llama": return 0.6;
                case "pared": return -0.2;
                default: return 0;
            }
        }
    }

    /** Un clip grabado: su largo, hasta dónde se puede usar (luego salen los carteles del final) y sus sonidos. */
    static final class Clip {
        final String nombre;
        final double largo, hasta;
        final List<Evento> eventos = new ArrayList<>();

        Clip(File carpeta, String nombre, double largo, double hastaFijo) throws IOException {
            this.nombre = nombre;
            this.largo = largo;
            double fin = largo;
            for (String l : Files.readAllLines(new File(carpeta, nombre + ".sonidos.txt").toPath(), StandardCharsets.UTF_8)) {
                String[] c = l.split("\t");
                if (c.length < 4) continue;
                Evento e = new Evento(Integer.parseInt(c[0]) / FPS, c[1], Float.parseFloat(c[2]), Float.parseFloat(c[3]));
                eventos.add(e);
                // La campana fuerte de la segunda mitad es el final: luego vienen los carteles.
                if (e.nombre.equals("campana") && e.fuerza > 0.7f && e.segundo > largo / 2) fin = Math.min(fin, e.segundo + 1.5);
            }
            hasta = Math.min(fin, hastaFijo);
        }

        File archivo(File carpeta) {
            return new File(carpeta, nombre + ".mp4");
        }
    }

    /** Una toma del montaje: de qué clip, cuántos pulsos dura y a qué velocidad; si es «lo mejor», el montaje elige desde dónde. */
    static final class Toma {
        final String nombreDelClip;
        final double pulsos, velocidad;
        final boolean mejor;
        double desde, inicio, segundos;
        String rotulo;
        boolean destello;
        Clip clip;

        Toma(String clip, double pulsos, double desde, double velocidad, boolean mejor) {
            this.nombreDelClip = clip;
            this.pulsos = pulsos;
            this.desde = desde;
            this.velocidad = velocidad;
            this.mejor = mejor;
        }

        double segundosDeOrigen() {
            return segundos * velocidad;
        }

        Toma rotulo(String r) {
            rotulo = r;
            return this;
        }

        Toma destello() {
            destello = true;
            return this;
        }
    }

    /** Una versión del tráiler: su nombre de archivo, una línea que la describe, sus tomas y de dónde sale su GIF. */
    static final class Version {
        final String nombre, descripcion;
        final List<Toma> tomas;
        final double gifDesde;

        Version(String nombre, String descripcion, double gifDesde, List<Toma> tomas) {
            this.nombre = nombre;
            this.descripcion = descripcion;
            this.gifDesde = gifDesde;
            this.tomas = tomas;
        }
    }

    static Toma fija(String clip, double pulsos, double desde) {
        return new Toma(clip, pulsos, desde, 1, false);
    }

    static Toma rapida(String clip, double pulsos, double desde, double velocidad) {
        return new Toma(clip, pulsos, desde, velocidad, false);
    }

    static Toma mejor(String clip, double pulsos) {
        return new Toma(clip, pulsos, 0, 1, true);
    }

    private final File clips, salida;
    private final String ffmpeg;
    private final MusicaDelTrailer musica;
    private final Map<String, Clip> grabados = new HashMap<>();
    /** El segundo de la música en que cada pulso: 0,5 s a 120 pulsos por minuto. */
    private final double segundosPorPulso;

    MontajeDelTrailer(File clips, File salida, String ffmpeg, MusicaDelTrailer musica) throws IOException {
        this.clips = clips;
        this.salida = salida;
        this.ffmpeg = ffmpeg;
        this.musica = musica;
        this.segundosPorPulso = musica.segundoDelPulso(1);
        for (EscenasDelTrailer.Escena e : EscenasDelTrailer.todas()) {
            if (new File(clips, e.nombre + ".mp4").isFile()) {
                grabados.put(e.nombre, new Clip(clips, e.nombre, e.segundos, VersionesDelTrailer.HASTA.getOrDefault(e.nombre, 1e9)));
            }
        }
    }

    /** Coloca las tomas: dónde empieza cada una en el vídeo (en un pulso) y, en las de «lo mejor», desde dónde del clip. */
    void colocar(Version v) {
        double pulso = 0;
        Map<String, List<double[]>> usados = new HashMap<>();
        int embestidas = 0;
        contactoUsado = 0;
        for (Toma t : v.tomas) {
            t.clip = grabados.get(t.nombreDelClip);
            if (t.clip == null) throw new IllegalStateException("falta el clip " + t.nombreDelClip + " (gradlew lwjgl3:grabarTrailer)");
            t.inicio = musica.segundoDelPulso(pulso);
            pulso += t.pulsos;
            t.segundos = musica.segundoDelPulso(pulso) - t.inicio;
            List<double[]> ya = usados.computeIfAbsent(t.nombreDelClip, k -> new ArrayList<>());
            if (t.mejor) t.desde = elegir(t, ya);
            ya.add(new double[] {t.desde, t.desde + t.segundosDeOrigen()});
            if (!t.mejor) contactoUsado += contacto(t.clip, t.desde, t.segundosDeOrigen());
            embestidas += contacto(t.clip, t.desde, t.segundosDeOrigen());
        }
        System.out.printf(Locale.ROOT, "%s: %d tomas, %.1f s, %.1f s con choques entre robots%n", v.nombre, v.tomas.size(), duracion(v),
                embestidas / 2.0);
    }

    /** Medios segundos con choques entre robots que el montaje se permite en toda una versión (destellos, no la pelea entera). */
    static final int CONTACTO_MAXIMO = 6;
    private int contactoUsado;

    /** Los medios segundos de una ventana del clip en que hay choques entre robots. */
    private static int contacto(Clip c, double desde, double largo) {
        java.util.Set<Long> medios = new java.util.HashSet<>();
        for (Evento e : c.eventos) if (e.embestida() && e.segundo >= desde && e.segundo < desde + largo) medios.add(Math.round(Math.floor(e.segundo * 2)));
        return medios.size();
    }

    /** Lo que vale una ventana: la acción (las chispas suman poco, y con tope), y cada medio segundo de choques, mucho en contra. */
    private static double nota(Clip c, double desde, double largo) {
        double nota = 0, chispas = 0;
        for (Evento e : c.eventos) {
            if (e.segundo < desde || e.segundo >= desde + largo || e.embestida()) continue;
            if (e.nombre.equals("chispa")) chispas += e.peso();
            else nota += e.peso();
        }
        return nota + Math.min(10, chispas) - 25 * contacto(c, desde, largo);
    }

    /**
     * Desde dónde del clip: la ventana con más acción que no repite lo ya usado, con su mejor golpe cayendo en un pulso, y sin
     * pasarse del tope de choques entre robots de la versión.
     */
    private double elegir(Toma t, List<double[]> ya) {
        double d = elegir(t, ya, true);
        // Si con el tope de choques no queda ningún trozo, el que menos tenga (los choques siguen puntuando en contra).
        return d >= 0 ? d : elegir(t, ya, false);
    }

    private double elegir(Toma t, List<double[]> ya, boolean conTope) {
        double largo = t.segundosDeOrigen(), paso = segundosPorPulso * t.velocidad;
        double mejorDesde = -1, mejorNota = -1e9;
        List<Double> candidatos = new ArrayList<>();
        for (Evento e : t.clip.eventos) {
            if (e.peso() < 2) continue;
            for (int k = 1; k < t.pulsos; k++) candidatos.add(e.segundo - k * paso);
        }
        for (double d = 0.5; d + largo <= t.clip.hasta; d += paso) candidatos.add(d);
        for (double d : candidatos) {
            if (d < 0.5 || d + largo > t.clip.hasta) continue;
            boolean pisa = false;
            for (double[] u : ya) if (d < u[1] && d + largo > u[0]) pisa = true;
            if (pisa || conTope && contactoUsado + contacto(t.clip, d, largo) > CONTACTO_MAXIMO) continue;
            double nota = nota(t.clip, d, largo);
            if (nota > mejorNota) {
                mejorNota = nota;
                mejorDesde = d;
            }
        }
        if (mejorDesde < 0 && conTope) return -1;
        if (mejorDesde < 0) throw new IllegalStateException("no queda trozo libre de " + t.nombreDelClip + " para " + t.pulsos + " pulsos");
        contactoUsado += contacto(t.clip, mejorDesde, largo);
        return mejorDesde;
    }

    static double duracion(Version v) {
        Toma u = v.tomas.get(v.tomas.size() - 1);
        return u.inicio + u.segundos;
    }

    /** Hace el MP4 (H.264 y AAC) y el GIF de una versión. */
    void hacer(Version v) throws IOException, InterruptedException {
        colocar(v);
        File tmp = new File(salida, "tmp_" + v.nombre);
        tmp.mkdirs();
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < v.tomas.size(); i++) {
            Toma t = v.tomas.get(i);
            File f = new File(tmp, String.format(Locale.ROOT, "%02d.mp4", i));
            int desdeF = (int) Math.round(t.inicio * FPS), hastaF = (int) Math.round((t.inicio + t.segundos) * FPS);
            StringBuilder filtro = new StringBuilder(String.format(Locale.ROOT, "setpts=(PTS-STARTPTS)/%.4f,fps=%d,trim=end_frame=%d,setpts=PTS-STARTPTS",
                    t.velocidad, (int) FPS, hastaF - desdeF));
            if (t.destello) filtro.append(",fade=in:st=0:d=0.18:color=white");
            if (t.rotulo != null) filtro.append(",").append(rotulo(t.rotulo, (hastaF - desdeF) / FPS));
            ejecutar(List.of(ffmpeg, "-y", "-loglevel", "error", "-ss", fmt(t.desde), "-t", fmt(t.segundosDeOrigen() + 0.5),
                    "-i", t.clip.archivo(clips).getAbsolutePath(), "-vf", filtro.toString(), "-an",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "12", "-pix_fmt", "yuv420p", f.getAbsolutePath()));
            lista.add("file '" + f.getName() + "'");
        }
        File listaF = new File(tmp, "lista.txt");
        Files.write(listaF.toPath(), lista, StandardCharsets.UTF_8);
        double dur = duracion(v);
        File audio = new File(tmp, "sonido.wav");
        MezclaDelTrailer.wav(MezclaDelTrailer.mezclar(musica, v.tomas, dur, 3.0), audio);
        File mp4 = new File(salida, v.nombre + ".mp4");
        ejecutar(List.of(ffmpeg, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", listaF.getAbsolutePath(), "-i", audio.getAbsolutePath(),
                "-vf", String.format(Locale.ROOT, "fade=in:st=0:d=0.5,fade=out:st=%.3f:d=2.5", dur - 2.5),
                "-c:v", "libx264", "-preset", "slow", "-crf", "21", "-maxrate", "2300k", "-bufsize", "4600k", "-pix_fmt", "yuv420p", "-r", "60",
                "-c:a", "aac", "-b:a", "160k", "-t", fmt(dur), "-movflags", "+faststart", mp4.getAbsolutePath()));
        gif(mp4, new File(salida, v.nombre + ".gif"), v.gifDesde);
        System.out.printf(Locale.ROOT, "%s: %.1f s, %.1f MB; GIF %.1f MB%n", v.nombre, dur, mp4.length() / 1e6,
                new File(salida, v.nombre + ".gif").length() / 1e6);
        // Los trozos de cada toma ya no hacen falta (si Windows aún tiene alguno abierto, se quedan en build/).
        for (File f : tmp.listFiles()) f.delete();
        tmp.delete();
        try (PrintWriter w = new PrintWriter(new File(salida, v.nombre + ".tomas.txt"), StandardCharsets.UTF_8)) {
            for (Toma t : v.tomas) {
                w.printf(Locale.ROOT, "%6.2f  %-12s desde %6.2f  %5.2f s  x%.2f%s%n", t.inicio, t.nombreDelClip, t.desde, t.segundos, t.velocidad,
                        t.rotulo != null ? "  «" + t.rotulo + "»" : "");
            }
        }
    }

    /** El GIF: 12 segundos, 640 de ancho a 15 fotogramas por segundo (menos si pasa de 8 MB). */
    private void gif(File mp4, File gif, double desde) throws IOException, InterruptedException {
        int[][] intentos = {{640, 15}, {560, 12}, {480, 12}, {420, 10}};
        for (int[] i : intentos) {
            ejecutar(List.of(ffmpeg, "-y", "-loglevel", "error", "-ss", fmt(desde), "-t", "12", "-i", mp4.getAbsolutePath(), "-vf",
                    "fps=" + i[1] + ",scale=" + i[0] + ":-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128:stats_mode=diff[p];"
                            + "[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle", "-loop", "0", gif.getAbsolutePath()));
            if (gif.length() < 8_000_000) return;
        }
    }

    /** Un rótulo abajo a la izquierda (con la letra del juego), que entra y sale con un fundido. */
    private static String rotulo(String texto, double dura) {
        String t = texto.replace("\\", "").replace("'", "").replace(":", "").replace("%", "");
        return String.format(Locale.ROOT, "drawbox=x=0:y=ih-170:w=iw:h=120:color=black@0.45:t=fill:enable='lt(t,%.2f)',"
                + "drawtext=fontfile=res/fuentes/SpaceGrotesk-Bold.ttf:text='%s':x=70:y=h-150:fontsize=58:fontcolor=white"
                + ":borderw=3:bordercolor=black@0.8:alpha='min(1,min(t/0.25,(%.2f-t)/0.25))':enable='lt(t,%.2f)'",
                Math.min(dura, 3.0), t, Math.min(dura, 3.0), Math.min(dura, 3.0));
    }

    private static String fmt(double s) {
        return String.format(Locale.ROOT, "%.3f", Math.max(0, s));
    }

    private static void ejecutar(List<String> orden) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(orden).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        if (p.waitFor() != 0) throw new IOException("ffmpeg falló: " + String.join(" ", orden));
    }

    /** El SoundFont: el que se pase, o el GeneralUser GS (u otro .sf2) de ~/.metajavbots/. */
    static File soundFont(String dado) {
        if (dado != null && !dado.isEmpty()) return new File(dado);
        File carpeta = new File(System.getProperty("user.home"), ".metajavbots");
        File gs = new File(carpeta, "GeneralUser-GS.sf2");
        if (gs.isFile()) return gs;
        File[] otros = carpeta.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".sf2"));
        return otros != null && otros.length > 0 ? otros[0] : null;
    }

    public static void main(String[] args) throws Exception {
        File clips = new File(args.length > 0 && !args[0].isEmpty() ? args[0] : "build/trailer/clips");
        File salida = new File(args.length > 1 && !args[1].isEmpty() ? args[1] : "build/trailer");
        String ffmpeg = args.length > 2 && !args[2].isEmpty() ? args[2] : "ffmpeg";
        File sf2 = soundFont(args.length > 3 ? args[3] : "");
        String solo = args.length > 4 ? args[4] : "";
        if (sf2 == null) System.out.println("trailer: sin SoundFont; la música suena con los instrumentos de Java");
        MusicaDelTrailer musica = new MusicaDelTrailer(new File("res/musica/ultimo_asalto.mid"), sf2, 8);
        MontajeDelTrailer m = new MontajeDelTrailer(clips, salida, ffmpeg, musica);
        try (PrintWriter lista = new PrintWriter(new File(salida, "versiones.txt"), StandardCharsets.UTF_8)) {
            for (Version v : VersionesDelTrailer.todas()) {
                if (!solo.isEmpty() && !List.of(solo.split(",")).contains(v.nombre)) continue;
                m.hacer(v);
                lista.println(v.nombre + ".mp4: " + v.descripcion);
            }
        }
    }
}
