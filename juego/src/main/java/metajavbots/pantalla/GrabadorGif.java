package metajavbots.pantalla;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * GRABADOR DE GIF — Junta fotogramas de la pantalla y los guarda como un GIF
 * animado que se repite (con lo que trae Java: ImageIO, sin bibliotecas).
 * Lo usan la repetición del mejor golpe («Guardar GIF») y las capturas
 * («gradlew lwjgl3:capturas -Pgif=…», para enseñar cada novedad).
 * Cada fotograma se encoge a la anchura pedida al cogerlo, así no ocupa
 * mucha memoria.
 */
public final class GrabadorGif {

    /** Límite máximo de fotogramas en memoria para evitar consumos descontrolados. */
    public static final int MAX_FOTOGRAMAS = 120;

    private final List<BufferedImage> fotos = new ArrayList<>();
    private final int ancho;
    private final int centesimas;
    /** Lo que se recorta de la pantalla (en píxeles de la ventana; todo si el ancho es 0). */
    private int rx, ry, rw, rh;

    /** Un GIF de esa anchura (el alto, en proporción) y esa pausa entre fotogramas (en centésimas de segundo). */
    public GrabadorGif(int ancho, int centesimas) {
        this.ancho = Math.max(64, ancho);
        this.centesimas = Math.max(2, centesimas);
    }

    /** Carpeta clara de salida dentro del directorio de usuario/exportaciones para repeticiones GIF. */
    public static File carpetaDeExportacion() {
        return new File(new File(new File(System.getProperty("user.home", "."), "MetajavBots"), "exportaciones"), "repeticiones");
    }

    /** Añade un fotograma ya renderizado (p. ej. sin pantalla), escalándolo si supera el ancho y respetando el tope. */
    public void agregarFotograma(BufferedImage imagen) {
        if (imagen == null || fotos.size() >= MAX_FOTOGRAMAS) return;
        int w = imagen.getWidth(), h = imagen.getHeight();
        if (w <= ancho && imagen.getType() == BufferedImage.TYPE_INT_RGB) {
            fotos.add(imagen);
            return;
        }
        int aw = Math.min(ancho, w), ah = Math.max(1, Math.round(h * aw / (float) w));
        BufferedImage escala = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = escala.createGraphics();
        g.drawImage(imagen, 0, 0, aw, ah, null);
        g.dispose();
        fotos.add(escala);
    }

    /** Solo un trozo de la pantalla (en las coordenadas de dibujo, con el origen arriba a la izquierda). */
    public GrabadorGif recorte(int x, int y, int w, int h) {
        rx = x;
        ry = y;
        rw = w;
        rh = h;
        return this;
    }

    /** Cuántos fotogramas lleva. */
    public int fotogramas() {
        return fotos.size();
    }

    /** Coge lo que hay ahora en la pantalla (llamar después de dibujar el fotograma). */
    public void coger() {
        if (fotos.size() >= MAX_FOTOGRAMAS) return;
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        float escala = bw / (float) Gdx.graphics.getWidth();
        int x = (int) (rx * escala), w = rw == 0 ? bw : (int) (rw * escala), h = rh == 0 ? bh : (int) (rh * escala);
        // OpenGL cuenta desde abajo.
        int y = rw == 0 ? 0 : bh - (int) (ry * escala) - h;
        x = Math.max(0, Math.min(bw - 1, x));
        y = Math.max(0, Math.min(bh - 1, y));
        w = Math.max(1, Math.min(bw - x, w));
        h = Math.max(1, Math.min(bh - y, h));
        Pixmap pm = Pixmap.createFromFrameBuffer(x, y, w, h);
        fotos.add(encoger(pm, ancho));
        pm.dispose();
    }

    /** Del Pixmap (al revés, como lo da OpenGL) a una imagen derecha de esa anchura (media de los píxeles que caen en cada uno). */
    static BufferedImage encoger(Pixmap pm, int ancho) {
        int w = pm.getWidth(), h = pm.getHeight();
        int aw = Math.min(ancho, w), ah = Math.max(1, Math.round(h * aw / (float) w));
        BufferedImage img = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_RGB);
        // Los bytes tal cual (RGBA, 4 por píxel): leerlos uno a uno con getPixel sería muy lento.
        java.nio.ByteBuffer bytes = pm.getPixels();
        int por = pm.getFormat() == Pixmap.Format.RGB888 ? 3 : 4;
        for (int y = 0; y < ah; y++) {
            int y0 = y * h / ah, y1 = Math.max(y0 + 1, (y + 1) * h / ah);
            for (int x = 0; x < aw; x++) {
                int x0 = x * w / aw, x1 = Math.max(x0 + 1, (x + 1) * w / aw);
                long r = 0, g = 0, b = 0, n = 0;
                for (int yy = y0; yy < y1; yy += 1 + (y1 - y0) / 3) {
                    for (int xx = x0; xx < x1; xx += 1 + (x1 - x0) / 3) {
                        int i = ((h - 1 - yy) * w + xx) * por;
                        r += bytes.get(i) & 0xFF;
                        g += bytes.get(i + 1) & 0xFF;
                        b += bytes.get(i + 2) & 0xFF;
                        n++;
                    }
                }
                img.setRGB(x, y, (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n));
            }
        }
        return img;
    }

    /** Guarda el GIF (que se repite sin fin). Devuelve el archivo. */
    public File guardar(File archivo) throws IOException {
        if (fotos.isEmpty()) throw new IOException("sin fotogramas");
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (carpeta != null) carpeta.mkdirs();
        Iterator<ImageWriter> ws = ImageIO.getImageWritersByFormatName("gif");
        if (!ws.hasNext()) throw new IOException("Java no trae escritor de GIF");
        ImageWriter w = ws.next();
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(archivo)) {
            w.setOutput(salida);
            w.prepareWriteSequence(null);
            for (BufferedImage f : fotos) {
                IIOMetadata meta = w.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(f), null);
                ponerTiempo(meta, centesimas);
                w.writeToSequence(new IIOImage(f, null, meta), null);
            }
            w.endWriteSequence();
        } finally {
            w.dispose();
        }
        return archivo;
    }

    /** La pausa de cada fotograma y que el GIF se repita (la extensión NETSCAPE2.0). */
    private static void ponerTiempo(IIOMetadata meta, int centesimas) throws IOException {
        String formato = meta.getNativeMetadataFormatName();
        IIOMetadataNode raiz = (IIOMetadataNode) meta.getAsTree(formato);
        IIOMetadataNode control = hijo(raiz, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        control.setAttribute("delayTime", Integer.toString(centesimas));
        control.setAttribute("transparentColorIndex", "0");
        IIOMetadataNode extensiones = hijo(raiz, "ApplicationExtensions");
        IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
        app.setAttribute("applicationID", "NETSCAPE");
        app.setAttribute("authenticationCode", "2.0");
        app.setUserObject(new byte[] {1, 0, 0});
        extensiones.appendChild(app);
        meta.setFromTree(formato, raiz);
    }

    private static IIOMetadataNode hijo(IIOMetadataNode raiz, String nombre) {
        for (int i = 0; i < raiz.getLength(); i++) {
            if (raiz.item(i).getNodeName().equalsIgnoreCase(nombre)) return (IIOMetadataNode) raiz.item(i);
        }
        IIOMetadataNode n = new IIOMetadataNode(nombre);
        raiz.appendChild(n);
        return n;
    }
}
