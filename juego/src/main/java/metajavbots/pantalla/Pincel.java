package metajavbots.pantalla;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import space.earlygrey.shapedrawer.ShapeDrawer;

/**
 * PINCEL — Con lo que se dibuja todo: formas (con shapedrawer, la misma
 * biblioteca que usa el motor) y letras nítidas a cualquier tamaño. Las
 * coordenadas son las de la pantalla lógica (Pincel.ancho × Pincel.alto,
 * unos 1600 × 900), con la y hacia ABAJO. Las letras se crean al tamaño de
 * los píxeles de verdad (por la escala de la ventana) y se guardan: así se
 * ven nítidas en cualquier pantalla, también para la vista cansada.
 */
public final class Pincel {

    public final SpriteBatch lote;
    public final ShapeDrawer formas;
    private final Texture blanco;
    private final OrthographicCamera camara = new OrthographicCamera();
    private final FreeTypeFontGenerator generadorMono, generadorNegrita;
    private final Map<Integer, BitmapFont> letras = new HashMap<>();
    private final GlyphLayout medida = new GlyphLayout();
    private final Color color = new Color();
    /** El tamaño lógico de la pantalla. */
    public float ancho = 1600, alto = 900;
    /** Píxeles de verdad por unidad lógica. */
    public float escala = 1;
    private int pixelesAncho = 1600, pixelesAlto = 900;

    /** Las letras que se usan (los acentos de los tres idiomas y los símbolos del juego). */
    static final String LETRAS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóúüñÁÉÍÓÚÜÑ¿¡«»…—–·×÷±≤≥≠≈°äöüßÄÖÜàèìòùâêîôûçÇ€¢£¥©®™•▲▼►◄■□●○◆◇★☆✓✔✗✘⚠⏱→←↑↓↔⇒│─┌┐└┘├┤┬┴┼█▌▐░▒▓∞λπΣΩµ“”„‹›−⌨▤▶⚔⚙✎✕❚↺";

    public Pincel() {
        lote = new SpriteBatch(4000);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        blanco = new Texture(p);
        p.dispose();
        formas = new ShapeDrawer(lote, new TextureRegion(blanco, 0, 0, 1, 1));
        generadorMono = new FreeTypeFontGenerator(Gdx.files.internal("fuentes/DejaVuSansMono.ttf"));
        generadorNegrita = new FreeTypeFontGenerator(Gdx.files.internal("fuentes/DejaVuSansMono-Bold.ttf"));
    }

    /** Cuando cambia el tamaño de la ventana: la pantalla lógica mide al menos 1600 × 900 de ancho y alto proporcionales. */
    public void redimensionar(int px, int py) {
        pixelesAncho = Math.max(1, px);
        pixelesAlto = Math.max(1, py);
        escala = Math.max(0.5f, Math.min(pixelesAncho / 1600f, pixelesAlto / 900f));
        ancho = pixelesAncho / escala;
        alto = pixelesAlto / escala;
        camara.setToOrtho(true, ancho, alto);
        camara.update();
        lote.setProjectionMatrix(camara.combined);
        // Las letras de otra escala ya no valen.
        for (BitmapFont f : letras.values()) f.dispose();
        letras.clear();
    }

    public void empezar() {
        lote.setProjectionMatrix(camara.combined);
        lote.begin();
    }

    public void acabar() {
        lote.end();
    }

    // ================================================================ colores

    public Color color(int rgb, float alfa) {
        return color.set(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, Math.max(0, Math.min(1, alfa)));
    }

    public void pintar(int rgb, float alfa) {
        formas.setColor(color(rgb, alfa));
    }

    /** Mezcla dos colores (t = 0, el primero; t = 1, el segundo). */
    public static int mezcla(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int aclarar(int rgb, float t) {
        return mezcla(rgb, 0xFFFFFF, t);
    }

    public static int oscurecer(int rgb, float t) {
        return mezcla(rgb, 0x000000, t);
    }

    // ================================================================ formas

    public void rect(float x, float y, float w, float h, int rgb, float alfa) {
        if (w <= 0 || h <= 0) return;
        pintar(rgb, alfa);
        formas.filledRectangle(x, y, w, h);
    }

    /** Un rectángulo con un degradado de arriba (a) a abajo (b). */
    public void degradado(float x, float y, float w, float h, int arriba, int abajo, float alfa) {
        if (w <= 0 || h <= 0) return;
        Color a = new Color(color(arriba, alfa)), b = new Color(color(abajo, alfa));
        // La y va hacia abajo: abajo-izquierda en la pantalla es (x, y + h).
        formas.filledRectangle(x, y, w, h, a, a, b, b);
    }

    public void marco(float x, float y, float w, float h, int rgb, float alfa, float grosor) {
        if (w <= 0 || h <= 0) return;
        pintar(rgb, alfa);
        formas.rectangle(x, y, w, h, grosor);
    }

    /** Un rectángulo con las esquinas redondeadas. */
    public void caja(float x, float y, float w, float h, float radio, int rgb, float alfa) {
        if (w <= 0 || h <= 0) return;
        radio = Math.min(radio, Math.min(w, h) / 2);
        pintar(rgb, alfa);
        if (radio < 1) {
            formas.filledRectangle(x, y, w, h);
            return;
        }
        formas.filledRectangle(x + radio, y, w - 2 * radio, h);
        formas.filledRectangle(x, y + radio, radio, h - 2 * radio);
        formas.filledRectangle(x + w - radio, y + radio, radio, h - 2 * radio);
        formas.filledCircle(x + radio, y + radio, radio);
        formas.filledCircle(x + w - radio, y + radio, radio);
        formas.filledCircle(x + radio, y + h - radio, radio);
        formas.filledCircle(x + w - radio, y + h - radio, radio);
    }

    public void bordeRedondeado(float x, float y, float w, float h, float radio, int rgb, float alfa, float grosor) {
        if (w <= 0 || h <= 0) return;
        radio = Math.min(radio, Math.min(w, h) / 2);
        pintar(rgb, alfa);
        formas.line(x + radio, y, x + w - radio, y, grosor);
        formas.line(x + radio, y + h, x + w - radio, y + h, grosor);
        formas.line(x, y + radio, x, y + h - radio, grosor);
        formas.line(x + w, y + radio, x + w, y + h - radio, grosor);
        float pi = (float) Math.PI;
        formas.arc(x + radio, y + radio, radio, pi, pi / 2, grosor);
        formas.arc(x + w - radio, y + radio, radio, 1.5f * pi, pi / 2, grosor);
        formas.arc(x + w - radio, y + h - radio, radio, 0, pi / 2, grosor);
        formas.arc(x + radio, y + h - radio, radio, pi / 2, pi / 2, grosor);
    }

    public void linea(float x1, float y1, float x2, float y2, float grosor, int rgb, float alfa) {
        pintar(rgb, alfa);
        formas.line(x1, y1, x2, y2, grosor);
    }

    public void circulo(float x, float y, float r, int rgb, float alfa) {
        if (r <= 0) return;
        pintar(rgb, alfa);
        formas.filledCircle(x, y, r);
    }

    public void aro(float x, float y, float r, float grosor, int rgb, float alfa) {
        if (r <= 0) return;
        pintar(rgb, alfa);
        formas.circle(x, y, r, grosor);
    }

    public void elipse(float x, float y, float rx, float ry, float giro, int rgb, float alfa) {
        if (rx <= 0 || ry <= 0) return;
        pintar(rgb, alfa);
        formas.filledEllipse(x, y, rx, ry, giro);
    }

    public void poligono(float[] v, int rgb, float alfa) {
        if (v.length < 6) return;
        pintar(rgb, alfa);
        formas.filledPolygon(v);
    }

    public void contorno(float[] v, float grosor, int rgb, float alfa) {
        if (v.length < 4) return;
        pintar(rgb, alfa);
        formas.path(v, grosor, space.earlygrey.shapedrawer.JoinType.NONE, false);
    }

    public void triangulo(float x1, float y1, float x2, float y2, float x3, float y3, int rgb, float alfa) {
        pintar(rgb, alfa);
        formas.filledTriangle(x1, y1, x2, y2, x3, y3);
    }

    /** Un resplandor: varios círculos cada vez más grandes y transparentes. */
    public void brillo(float x, float y, float r, int rgb, float alfa) {
        for (int i = 4; i >= 1; i--) circulo(x, y, r * i / 2.5f, rgb, alfa * 0.18f);
    }

    // ================================================================ letras

    /** La letra de ese tamaño lógico (normal o negrita), creada a los píxeles de verdad. */
    public BitmapFont letra(float tamano, boolean negrita) {
        int px = Math.max(6, Math.round(tamano * escala));
        int clave = px * 2 + (negrita ? 1 : 0);
        BitmapFont f = letras.get(clave);
        if (f == null) {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = px;
            p.flip = true;
            p.characters = LETRAS;
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;
            p.hinting = FreeTypeFontGenerator.Hinting.Full;
            p.incremental = true;
            f = (negrita ? generadorNegrita : generadorMono).generateFont(p);
            f.getData().setScale(1 / escala);
            f.setUseIntegerPositions(false);
            f.getData().missingGlyph = f.getData().getGlyph('?');
            letras.put(clave, f);
        }
        return f;
    }

    /** Escribe un texto con la esquina de arriba a la izquierda en (x, y). Devuelve su ancho. */
    public float texto(String s, float x, float y, float tamano, int rgb, float alfa) {
        return texto(s, x, y, tamano, false, rgb, alfa);
    }

    public float texto(String s, float x, float y, float tamano, boolean negrita, int rgb, float alfa) {
        if (s == null || s.isEmpty()) return 0;
        BitmapFont f = letra(tamano, negrita);
        f.setColor(color(rgb, alfa));
        medida.setText(f, s);
        f.draw(lote, s, x, y);
        return medida.width;
    }

    /**
     * Una línea con colores por trozos, de una vez (la fuente coloca cada letra: en el editor de código,
     * así ninguna letra se monta en otra). Cada trozo: {texto, color RGB}.
     */
    public void textoDeColores(java.util.List<Object[]> trozos, float x, float y, float tamano) {
        if (trozos.isEmpty()) return;
        BitmapFont f = letra(tamano, false);
        StringBuilder b = new StringBuilder();
        for (Object[] t : trozos) {
            String s = (String) t[0];
            if (s.isEmpty()) continue;
            b.append("[#").append(String.format("%06X", ((Integer) t[1]) & 0xFFFFFF)).append(']').append(s.replace("[", "[[")).append("[]");
        }
        boolean antes = f.getData().markupEnabled;
        f.getData().markupEnabled = true;
        f.setColor(1, 1, 1, 1);
        f.draw(lote, b, x, y);
        f.getData().markupEnabled = antes;
    }

    public float textoCentrado(String s, float cx, float y, float tamano, boolean negrita, int rgb, float alfa) {
        float w = ancho(s, tamano, negrita);
        texto(s, cx - w / 2, y, tamano, negrita, rgb, alfa);
        return w;
    }

    public float textoDerecha(String s, float xDerecha, float y, float tamano, boolean negrita, int rgb, float alfa) {
        float w = ancho(s, tamano, negrita);
        texto(s, xDerecha - w, y, tamano, negrita, rgb, alfa);
        return w;
    }

    /** Un texto que se parte en líneas para caber en «ancho». Devuelve el alto que ocupa. */
    public float parrafo(String s, float x, float y, float anchoMax, float tamano, int rgb, float alfa) {
        if (s == null || s.isEmpty()) return 0;
        float alto = altoDeLinea(tamano);
        float yy = y;
        for (String linea : partir(s, anchoMax, tamano, false)) {
            texto(linea, x, yy, tamano, false, rgb, alfa);
            yy += alto;
        }
        return yy - y;
    }

    public float altoDeParrafo(String s, float anchoMax, float tamano) {
        if (s == null || s.isEmpty()) return 0;
        return partir(s, anchoMax, tamano, false).size() * altoDeLinea(tamano);
    }

    /** Parte un texto en líneas que caben en anchoMax (respeta los saltos de línea). */
    public java.util.List<String> partir(String s, float anchoMax, float tamano, boolean negrita) {
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String parrafo : s.split("\n", -1)) {
            StringBuilder linea = new StringBuilder();
            for (String palabra : parrafo.split(" ", -1)) {
                String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
                if (ancho(prueba, tamano, negrita) > anchoMax && linea.length() > 0) {
                    l.add(linea.toString());
                    linea.setLength(0);
                    linea.append(palabra);
                } else {
                    linea.setLength(0);
                    linea.append(prueba);
                }
            }
            l.add(linea.toString());
        }
        return l;
    }

    public float ancho(String s, float tamano, boolean negrita) {
        if (s == null || s.isEmpty()) return 0;
        medida.setText(letra(tamano, negrita), s);
        return medida.width;
    }

    /** El alto de una línea de texto (con su espacio). */
    public float altoDeLinea(float tamano) {
        return tamano * 1.35f;
    }

    /** El ancho de una letra de la fuente de código (todas iguales: es monoespaciada). */
    public float anchoDeLetra(float tamano) {
        // Lo que avanza cada letra (no lo que mide su dibujo): la diferencia entre dos letras y una.
        BitmapFont f = letra(tamano, false);
        medida.setText(f, "MMMMMMMMMMM");
        float once = medida.width;
        medida.setText(f, "M");
        return (once - medida.width) / 10;
    }

    // ================================================================ recortes

    private final Rectangle recorte = new Rectangle(), escena = new Rectangle();
    private int recortes;

    /** Solo se dibuja dentro de este rectángulo hasta sinRecorte(). Se pueden anidar. */
    public void recortar(float x, float y, float w, float h) {
        lote.flush();
        // ScissorStack trabaja en píxeles de la ventana, con la y hacia arriba.
        escena.set(x * escala, (alto - y - h) * escala, Math.max(0, w) * escala, Math.max(0, h) * escala);
        recorte.set(escena);
        if (ScissorStack.pushScissors(new Rectangle(recorte))) recortes++;
        else {
            ScissorStack.pushScissors(new Rectangle(0, 0, 0, 0));
            recortes++;
        }
    }

    public void sinRecorte() {
        if (recortes <= 0) return;
        lote.flush();
        ScissorStack.popScissors();
        recortes--;
    }

    public int pixelesAncho() {
        return pixelesAncho;
    }

    public int pixelesAlto() {
        return pixelesAlto;
    }

    public void liberar() {
        for (BitmapFont f : letras.values()) f.dispose();
        letras.clear();
        generadorMono.dispose();
        generadorNegrita.dispose();
        blanco.dispose();
        lote.dispose();
    }
}
