package metajavbots.pantallas;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import metajavbots.Juego;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
import metajavbots.pantalla.GrabadorGif;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.T;

/**
 * REPETICIÓN — El mejor golpe de la pelea, a cámara lenta, con vista dividida:
 * la arena a la izquierda y el código ejecutado del robot a la derecha (con las
 * líneas activas resaltadas cuando hay traza disponible), además de exportación
 * a archivo GIF animado sin bloquear el hilo gráfico principal.
 */
public final class Repeticion {

    /** Un turno: posición de cada robot, sucesos y traza de líneas ejecutadas del jugador. */
    private static final class Turno {
        final double[][] cuerpos;
        final List<Suceso> sucesos;
        final BitSet lineas;
        final int lineaActiva;
        final int numero;

        Turno(Pelea p) {
            cuerpos = new double[p.combatientes.length][];
            for (int i = 0; i < cuerpos.length; i++) {
                Cuerpo c = p.cuerpo(i);
                if (c == null) continue;
                cuerpos[i] = new double[]{
                    c.x, c.y, c.rumbo, c.mira, c.velocidad,
                    c.parte[0], c.parte[1], c.parte[2], c.parte[3], c.fuera ? 1 : 0
                };
            }
            sucesos = new ArrayList<>(p.sucesos());
            Cuerpo c0 = p.cuerpo(0);
            lineas = c0 != null && c0.lineasDelTurno != null ? (BitSet) c0.lineasDelTurno.clone() : new BitSet();
            lineaActiva = c0 != null ? c0.ultimaLinea : 0;
            numero = p.turno();
        }

        void poner(Pelea p) {
            for (int i = 0; i < cuerpos.length; i++) {
                Cuerpo c = p.cuerpo(i);
                if (c == null || cuerpos[i] == null) continue;
                double[] v = cuerpos[i];
                c.x = v[0];
                c.y = v[1];
                c.rumbo = v[2];
                c.mira = v[3];
                c.velocidad = v[4];
                for (int k = 0; k < 4; k++) c.parte[k] = v[5 + k];
                c.fuera = v[9] > 0;
            }
        }
    }

    private static final int ANTES = 90, DESPUES = 30;
    private static final DateTimeFormatter MARCA_HORA = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ArrayDeque<Turno> ultimos = new ArrayDeque<>();
    private final VentanaDeCodigo visor = new VentanaDeCodigo();
    private List<Turno> mejor, grabando;
    private double mejorGolpe;
    private int faltan;
    private float reproduciendo = -1;
    private int aplicado = -1;
    private Turno comoAcabo;
    private String codigo = "";
    private volatile boolean exportandoGif;
    private volatile String estadoGif = "";
    private volatile boolean errorGif;

    /** Ancho reservado para la arena en la vista dividida de la repetición. */
    public static float anchoArena(float anchoPantalla) {
        return Math.max(440f, Math.min(anchoPantalla * 0.58f, anchoPantalla - 360f));
    }

    /** Un turno de la pelea (después de avanzar). */
    public void grabar(Pelea p) {
        Turno t = new Turno(p);
        ultimos.addLast(t);
        if (ultimos.size() > ANTES) ultimos.removeFirst();
        if (grabando != null) {
            grabando.add(t);
            if (--faltan <= 0) {
                mejor = grabando;
                grabando = null;
            }
        }
        for (Suceso s : t.sucesos) {
            if (s.tipo == Suceso.Tipo.IMPACTO && s.valor > mejorGolpe && s.valor >= 5) {
                mejorGolpe = s.valor;
                grabando = new ArrayList<>(ultimos);
                faltan = DESPUES;
            }
        }
    }

    public boolean hay() {
        return mejor != null || grabando != null || !ultimos.isEmpty();
    }

    public boolean activa() {
        return reproduciendo >= 0;
    }

    /** Número de turnos del clip listo para reproducir o exportar. */
    public int turnosGrabados() {
        List<Turno> clip = clipDisponible();
        return clip == null ? 0 : clip.size();
    }

    /** Código fuente asociado a la repetición actual. */
    public String codigo() {
        return codigo;
    }

    /** Línea activa del turno aplicado actualmente en la repetición (0 si no hay traza). */
    public int lineaActual() {
        List<Turno> clip = clipDisponible();
        if (clip == null || aplicado < 0 || aplicado >= clip.size()) return 0;
        return clip.get(aplicado).lineaActiva;
    }

    public void empezar(Pelea p) {
        empezar(p, codigo);
    }

    /** Inicia la reproducción en vista dividida asociando el código ejecutado por el robot. */
    public void empezar(Pelea p, String codigoRobot) {
        if (mejor == null && grabando != null) mejor = grabando;
        if (mejor == null && !ultimos.isEmpty()) mejor = new ArrayList<>(ultimos);
        if (mejor == null || mejor.isEmpty()) return;
        codigo = codigoRobot == null ? "" : codigoRobot;
        visor.ponerTexto(codigo.isBlank() ? "// " + T.t("repeticion.sin_codigo") : codigo);
        visor.soloLectura = true;
        comoAcabo = new Turno(p);
        reproduciendo = 0;
        aplicado = -1;
        estadoGif = "";
        errorGif = false;
    }

    /** Detiene la repetición y devuelve los robots al estado final real de la pelea. */
    public void cerrar(Pelea p) {
        if (comoAcabo != null) comoAcabo.poner(p);
        reproduciendo = -1;
    }

    /** Un fotograma: pone a los robots donde estaban (a un tercio de la velocidad) y sincroniza la traza de código. */
    public void avanzar(Juego j, Pelea p, VistaDeArena v, float dt) {
        reproduciendo += dt;
        int ahora = (int) (reproduciendo * Pelea.TURNOS_POR_SEGUNDO / 3);
        if (ahora >= mejor.size() || j.entrada.tecla(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            cerrar(p);
            return;
        }
        for (int k = aplicado + 1; k <= ahora && k < mejor.size(); k++) {
            Turno tr = mejor.get(k);
            tr.poner(p);
            v.sucesos(tr.sucesos, j.sonidos);
            visor.lineasActivas = tr.lineas;
            visor.lineaActiva = tr.lineaActiva;
            aplicado = k;
        }
    }

    /** Avanza la repetición y dibuja el rótulo de la arena junto al panel derecho del código sincronizado. */
    public void dibujarEnPantalla(Juego j, Pelea p, VistaDeArena v, float dt) {
        avanzar(j, p, v, dt);
        if (!activa()) return;
        float anchoArena = anchoArena(j.pincel.ancho);
        rotulo(j, 0, 88, anchoArena, j.pincel.alto - 88);
        panelDeCodigo(j, p, anchoArena, 88, j.pincel.ancho - anchoArena, j.pincel.alto - 88);
    }

    /** El rótulo y el marco de la repetición sobre la mitad izquierda (la arena). */
    public void rotulo(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        p.rect(x, y, w, 6, 0xFF4040, 0.85f);
        p.rect(x, y + h - 6, w, 6, 0xFF4040, 0.85f);
        boolean luz = (reproduciendo * 2) % 1f < 0.6f;
        if (luz) p.circulo(x + 28, y + 36, 9, 0xFF3030, 1);
        float tamTit = j.ui.cabe(T.t("repeticion.titulo"), w * 0.45f, j.ui.letra * 1.15f, true);
        p.texto(T.t("repeticion.titulo"), x + 46, y + 22, tamTit, true, 0xFFFFFF, 1);
        String lenta = T.t("repeticion.lenta");
        float tamLen = j.ui.cabe(lenta, w * 0.48f, j.ui.letra * 0.82f, false);
        p.textoDerecha(j.ui.recortarTexto(lenta, w * 0.48f, tamLen, false), x + w - 18, y + 26, tamLen, false, 0xFFE27A, 1);
    }

    /** Panel derecho de la vista dividida: código del robot sincronizado, botón de exportar GIF y botón de cerrar. */
    public void panelDeCodigo(Juego j, Pelea p, float x, float y, float w, float h) {
        Pincel pin = j.pincel;
        Ui ui = j.ui;
        pin.rect(x, y, w, h, ui.tema.panel, 0.98f);
        pin.rect(x, y, 2, h, ui.tema.borde, 1);
        int total = mejor == null ? 0 : mejor.size();
        int turnoVis = Math.max(1, Math.min(total, aplicado + 1));
        String cab = T.f("repeticion.codigo_titulo", turnoVis, total);
        float cy = ui.panel(x + 8, y + 8, w - 16, h - 16, ui.recortarTexto(cab, w - 40, ui.letra * 0.95f, true));
        float pieAlto = estadoGif.isEmpty() ? 66f : 96f;
        float altoEditor = Math.max(100f, y + h - cy - pieAlto - 12f);
        visor.tamano = Math.max(14f, j.jugador.tamanoDeLetra - 2f);
        visor.dibujar(ui, x + 16, cy, w - 32, altoEditor);
        float by = y + h - 60f;
        if (!estadoGif.isEmpty()) {
            float tamEst = ui.cabe(estadoGif, w - 36, ui.letra * 0.74f, false);
            pin.texto(ui.recortarTexto(estadoGif, w - 36, tamEst, false), x + 18, by - 26f, tamEst, false,
                    errorGif ? ui.tema.mal : ui.tema.bien, 1);
        }
        String[] botones = {exportandoGif ? T.t("repeticion.guardando_gif") : T.t("repeticion.guardar_gif"), T.t("repeticion.cerrar")};
        Ui.Estilo[] estilos = {Ui.Estilo.PRINCIPAL, Ui.Estilo.PLANO};
        boolean[] activos = {!exportandoGif, true};
        String[] ayudas = {null, null};
        switch (ui.botonera(botones, estilos, activos, ayudas, x + 16, by, w - 32, 44)) {
            case 0: exportarGifAsync(p); break;
            case 1: cerrar(p); break;
            default: break;
        }
        ui.reservarEscape();
    }

    /** Exporta la repetición actual a GIF en un hilo demonio para no bloquear el renderizado. */
    public void exportarGifAsync(Pelea p) {
        if (exportandoGif || clipDisponible() == null) return;
        exportandoGif = true;
        errorGif = false;
        estadoGif = T.t("repeticion.guardando_gif");
        File destino = new File(GrabadorGif.carpetaDeExportacion(),
                "repeticion_" + MARCA_HORA.format(LocalDateTime.now()) + ".gif");
        Thread h = new Thread(() -> {
            try {
                File guardado = exportarGif(p, destino);
                estadoGif = T.f("repeticion.gif_guardado", guardado.getName());
                errorGif = false;
            } catch (IOException e) {
                estadoGif = T.f("repeticion.gif_error", e.getMessage());
                errorGif = true;
            } finally {
                exportandoGif = false;
            }
        }, "exportar-gif-repeticion");
        h.setDaemon(true);
        h.start();
    }

    /**
     * Genera y guarda un GIF animado válido de la repetición (arena + robots + código con línea activa) sin
     * depender de pantalla ni de OpenGL, acotando memoria y fotogramas.
     */
    public File exportarGif(Pelea p, File destino) throws IOException {
        List<Turno> clip = clipDisponible();
        if (clip == null || clip.isEmpty()) throw new IOException("no hay repetición grabada");
        List<Turno> copia = new ArrayList<>(clip);
        String[] lineasCodigo = (codigo == null || codigo.isBlank() ? "// MetajavBots" : codigo).split("\n", -1);
        int paso = Math.max(1, (int) Math.ceil(copia.size() / 60.0));
        GrabadorGif gif = new GrabadorGif(640, 8);
        for (int i = 0; i < copia.size(); i += paso) {
            gif.agregarFotograma(renderizarFotograma(p, copia.get(i), i + 1, copia.size(), lineasCodigo));
        }
        return gif.guardar(destino);
    }

    private List<Turno> clipDisponible() {
        if (mejor != null && !mejor.isEmpty()) return mejor;
        if (grabando != null && !grabando.isEmpty()) return grabando;
        return ultimos.isEmpty() ? null : new ArrayList<>(ultimos);
    }

    private static BufferedImage renderizarFotograma(Pelea p, Turno tr, int idx, int total, String[] lineasCodigo) {
        int w = 640, h = 360, aw = 370;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x161B24));
        g.fillRect(0, 0, aw, h);
        g.setColor(new Color(0x283244));
        g.drawRect(8, 28, aw - 16, h - 36);
        g.setColor(new Color(0xFF4040));
        g.fillOval(12, 8, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString("REPLAY " + idx + "/" + total, 28, 17);
        dibujarRobotsEnImagen(g, p, tr, 8, 28, aw - 16, h - 36);
        dibujarCodigoEnImagen(g, tr, lineasCodigo, aw, 0, w - aw, h);
        g.dispose();
        return img;
    }

    private static void dibujarRobotsEnImagen(Graphics2D g, Pelea p, Turno tr, int ox, int oy, int aw, int ah) {
        double sx = aw / Math.max(1.0, p.ancho), sy = ah / Math.max(1.0, p.alto);
        Color[] colores = {new Color(0x3FA9F5), new Color(0xE84855), new Color(0x5BD685), new Color(0xFFD23F)};
        for (int i = 0; i < tr.cuerpos.length; i++) {
            double[] v = tr.cuerpos[i];
            if (v == null) continue;
            int cx = ox + (int) Math.round(v[0] * sx), cy = oy + (int) Math.round(v[1] * sy);
            int r = 11;
            g.setColor(v[9] > 0 ? new Color(0x666C75) : colores[i % colores.length]);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            double rad = Math.toRadians(v[3]);
            g.setColor(Color.WHITE);
            g.drawLine(cx, cy, cx + (int) (Math.cos(rad) * 16), cy + (int) (Math.sin(rad) * 16));
        }
        g.setColor(new Color(0xFFD24A));
        for (Suceso s : tr.sucesos) {
            if (s.tipo == Suceso.Tipo.IMPACTO) {
                int sxp = ox + (int) Math.round(s.x * sx), syp = oy + (int) Math.round(s.y * sy);
                g.drawOval(sxp - 10, syp - 10, 20, 20);
            }
        }
    }

    private static void dibujarCodigoEnImagen(Graphics2D g, Turno tr, String[] lineas, int x, int y, int w, int h) {
        g.setColor(new Color(0x11151C));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(0x303B4D));
        g.drawLine(x, y, x, y + h);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        int filaH = 15, maxFilas = (h - 16) / filaH;
        int inicio = tr.lineaActiva > 0 ? Math.max(0, tr.lineaActiva - maxFilas / 2 - 1) : 0;
        inicio = Math.min(inicio, Math.max(0, lineas.length - maxFilas));
        for (int i = 0; i < maxFilas && inicio + i < lineas.length; i++) {
            int num = inicio + i + 1;
            int ry = y + 10 + i * filaH;
            if (num == tr.lineaActiva) {
                g.setColor(new Color(0x3B5998));
                g.fillRect(x + 2, ry - 11, w - 4, filaH);
            } else if (tr.lineas.get(num)) {
                g.setColor(new Color(0x223047));
                g.fillRect(x + 2, ry - 11, w - 4, filaH);
            }
            g.setColor(new Color(0x7F8C9B));
            g.drawString(String.format("%2d", num), x + 6, ry);
            g.setColor(num == tr.lineaActiva ? new Color(0xFFE38A) : new Color(0xD8E0EA));
            String txt = lineas[inicio + i];
            if (txt.length() > 34) txt = txt.substring(0, 34);
            g.drawString(txt, x + 28, ry);
        }
    }
}
