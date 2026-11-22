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
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;

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
    private final FreeTypeFontGenerator generadorUi, generadorTitulo, generadorSimbolos, generadorSimbolos2;
    private FreeTypeFontGenerator generadorCodigo, generadorCodigoNegrita;
    private final Map<Integer, BitmapFont> letras = new HashMap<>();
    private final GlyphLayout medida = new GlyphLayout();
    private final Color color = new Color();
    private final Color colorDegradadoA = new Color();
    private final Color colorDegradadoB = new Color();
    private final StringBuilder bufferColores = new StringBuilder(160);
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    /** El tamaño lógico de la pantalla. */
    public float ancho = 1600, alto = 900;
    /** Píxeles de verdad por unidad lógica. */
    public float escala = 1;
    private int pixelesAncho = 1600, pixelesAlto = 900;

    /** Las letras que se usan (los acentos de los tres idiomas y los símbolos del juego). */
    static final String LETRAS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóúüñÁÉÍÓÚÜÑ¿¡«»…—–·×÷±≤≥≠≈°äöüßÄÖÜàèìòùâêîôûçÇ€¢£¥©®™•▲▼►◄■□▣●○◆◇★☆✓✔✗✘⚠⏱→←↑↓↔⇒│─┌┐└┘├┤┬┴┼█▌▐░▒▓∞λπΣΩµ“”„‹›−⌨▤▶⚔⚙✎✕❚↺⚄";

    public Pincel() {
        lote = new SpriteBatch(4000);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        blanco = new Texture(p);
        p.dispose();
        formas = new ShapeDrawer(lote, new TextureRegion(blanco, 0, 0, 1, 1));
        generadorUi = new FreeTypeFontGenerator(Gdx.files.internal(Fuentes.INTERFAZ));
        generadorTitulo = new FreeTypeFontGenerator(Gdx.files.internal(Fuentes.TITULO));
        generadorSimbolos = new FreeTypeFontGenerator(Gdx.files.internal(Fuentes.SIMBOLOS));
        generadorSimbolos2 = new FreeTypeFontGenerator(Gdx.files.internal(Fuentes.SIMBOLOS_2));
        for (int i = 0; i < pilaRecortes.length; i++) pilaRecortes[i] = new Rectangle();
    }

    private FreeTypeFontGenerator generadorCodigo(boolean negrita) {
        if (negrita) {
            if (generadorCodigoNegrita == null) {
                generadorCodigoNegrita = new FreeTypeFontGenerator(Gdx.files.internal(Fuentes.CODIGO_NEGRITA));
            }
            return generadorCodigoNegrita;
        }
        if (generadorCodigo == null) {
            generadorCodigo = new FreeTypeFontGenerator(Gdx.files.internal(Fuentes.CODIGO));
        }
        return generadorCodigo;
    }

    /** Cuando cambia el tamaño de la ventana: la pantalla lógica mide al menos 1600 × 900 de ancho y alto proporcionales. */
    public void redimensionar(int px, int py) {
        if (px <= 0 || py <= 0) return;
        pixelesAncho = px;
        pixelesAlto = py;
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
        while (recortes > 0) sinRecorte();
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
        colorDegradadoA.set(color(arriba, alfa));
        colorDegradadoB.set(color(abajo, alfa));
        // La y va hacia abajo: abajo-izquierda en la pantalla es (x, y + h).
        formas.filledRectangle(x, y, w, h, colorDegradadoA, colorDegradadoA, colorDegradadoB, colorDegradadoB);
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

    /** La letra más pequeña que se dibuja (Fran tiene la vista cansada: nada de letra diminuta, ni en los rincones). */
    public static final float LETRA_MINIMA = 14;

    /** La letra de interfaz/título de ese tamaño lógico (normal o negrita), creada a los píxeles de verdad. */
    public BitmapFont letra(float tamano, boolean negrita) {
        return letra(tamano, negrita, false);
    }

    /** La letra monoespaciada de código/depurador de ese tamaño lógico (normal o negrita). */
    public BitmapFont letraDeCodigo(float tamano, boolean negrita) {
        return letra(tamano, negrita, true);
    }

    private BitmapFont letra(float tamano, boolean negrita, boolean codigo) {
        int px = Math.max(6, Math.round(Math.max(LETRA_MINIMA, tamano) * escala));
        int clave = (px << 2) | (codigo ? 2 : 0) | (negrita ? 1 : 0);
        BitmapFont f = letras.get(clave);
        if (f == null) {
            f = generarFuente(px, negrita, codigo);
            letras.put(clave, f);
        }
        return f;
    }

    private BitmapFont generarFuente(int px, boolean negrita, boolean codigo) {
        FreeTypeFontGenerator respaldoCodigo = generadorCodigo(negrita);
        FreeTypeFontGenerator primario = codigo ? respaldoCodigo : (negrita ? generadorTitulo : generadorUi);
        StringBuilder enPrimario = new StringBuilder();
        StringBuilder enCodigo = new StringBuilder();
        StringBuilder enSimbolos = new StringBuilder();
        StringBuilder enSimbolos2 = new StringBuilder();
        for (int i = 0; i < LETRAS.length(); i++) {
            char c = LETRAS.charAt(i);
            if (primario.hasGlyph(c)) enPrimario.append(c);
            else if (!codigo && respaldoCodigo.hasGlyph(c)) enCodigo.append(c);
            else if (generadorSimbolos.hasGlyph(c)) enSimbolos.append(c);
            else if (generadorSimbolos2.hasGlyph(c)) enSimbolos2.append(c);
        }
        PixmapPacker packer = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 2, false);
        int espacioX = (!codigo && px <= 18) ? 1 : 0;
        FreeTypeFontGenerator.FreeTypeFontParameter p = parametrosFuente(px, enPrimario.toString(), packer, espacioX);
        FreeTypeFontGenerator.FreeTypeBitmapFontData data = primario.generateData(p);
        anadirGlifosDeRespaldo(data, respaldoCodigo, enCodigo.toString(), px, packer, espacioX);
        anadirGlifosDeRespaldo(data, generadorSimbolos, enSimbolos.toString(), px, packer, espacioX);
        anadirGlifosDeRespaldo(data, generadorSimbolos2, enSimbolos2.toString(), px, packer, espacioX);
        Array<TextureRegion> regiones = new Array<>();
        packer.updateTextureRegions(regiones, p.minFilter, p.magFilter, false);
        packer.dispose();
        BitmapFont f = new BitmapFont(data, regiones, true);
        f.setOwnsTexture(true);
        if (Math.abs(escala - 1f) > 1e-4f) f.getData().setScale(1 / escala);
        f.setUseIntegerPositions(true);
        f.getData().missingGlyph = f.getData().getGlyph('?');
        return f;
    }

    private static FreeTypeFontGenerator.FreeTypeFontParameter parametrosFuente(int px, String chars, PixmapPacker packer, int espacioX) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = px;
        p.flip = true;
        p.characters = chars;
        p.kerning = true;
        p.spaceX = espacioX;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.hinting = FreeTypeFontGenerator.Hinting.AutoSlight;
        p.incremental = false;
        p.packer = packer;
        return p;
    }

    private static void anadirGlifosDeRespaldo(FreeTypeFontGenerator.FreeTypeBitmapFontData destino, FreeTypeFontGenerator respaldo, String chars,
            int px, PixmapPacker packer, int espacioX) {
        if (chars.isEmpty()) return;
        FreeTypeFontGenerator.FreeTypeFontParameter fp = parametrosFuente(px, chars, packer, espacioX);
        FreeTypeFontGenerator.FreeTypeBitmapFontData fb = respaldo.generateData(fp);
        int ajusteY = Math.round((destino.capHeight - fb.capHeight) * 0.5f);
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            BitmapFont.Glyph g = fb.getGlyph(c);
            if (g != null && g.id == c && destino.getGlyph(c) == null) {
                g.yoffset += ajusteY;
                destino.setGlyph(c, g);
            }
        }
    }

    /** Escribe un texto con la esquina de arriba a la izquierda en (x, y). Devuelve su ancho. */
    public float texto(String s, float x, float y, float tamano, int rgb, float alfa) {
        return texto(s, x, y, tamano, false, rgb, alfa);
    }

    public float texto(String s, float x, float y, float tamano, boolean negrita, int rgb, float alfa) {
        return textoConFuente(s, x, y, tamano, negrita, false, rgb, alfa);
    }

    /** Escribe un texto con la fuente monoespaciada de código/depurador. Devuelve su ancho. */
    public float textoDeCodigo(String s, float x, float y, float tamano, boolean negrita, int rgb, float alfa) {
        return textoConFuente(s, x, y, tamano, negrita, true, rgb, alfa);
    }

    private float textoConFuente(String s, float x, float y, float tamano, boolean negrita, boolean codigo, int rgb, float alfa) {
        if (s == null || s.isEmpty()) return 0;
        if (!codigo && TextoEnriquecido.tieneMarcas(s)) {
            return dibujarEnriquecido(s, x, y, tamano, negrita, rgb, alfa);
        }
        BitmapFont f = letra(tamano, negrita, codigo);
        f.setColor(color(rgb, alfa));
        return f.draw(lote, s, x, y).width;
    }

    private BitmapFont letraVersalita(float tamano) {
        float t = Math.max(10.5f, Math.max(LETRA_MINIMA, tamano) * TextoEnriquecido.ESCALA_VERSALITA);
        int px = Math.max(6, Math.round(t * escala));
        int clave = (px << 2) | 1;
        BitmapFont f = letras.get(clave);
        if (f == null) {
            f = generarFuente(px, true, false);
            letras.put(clave, f);
        }
        return f;
    }

    private float dibujarEnriquecido(String s, float x, float y, float tamano, boolean negrita, int rgb, float alfa) {
        BitmapFont fGrande = letra(tamano, true, false);
        BitmapFont fPeq = letraVersalita(tamano);
        float desfaseY = Math.max(0f, fGrande.getCapHeight() - fPeq.getCapHeight());
        float cx = x;
        for (TextoEnriquecido.Trozo t : TextoEnriquecido.aTrozos(s, negrita)) {
            BitmapFont f = t.pequeno ? fPeq : letra(tamano, t.negrita, false);
            float ty = t.pequeno ? y + desfaseY : y;
            f.setColor(color(rgb, alfa));
            f.draw(lote, t.texto, cx, ty);
            cx += anchoExacto(f, t.texto);
        }
        return cx - x;
    }

    private float anchoExacto(BitmapFont f, String texto) {
        if (texto.isEmpty()) return 0;
        if (texto.charAt(texto.length() - 1) != ' ') {
            medida.setText(f, texto);
            return medida.width;
        }
        medida.setText(f, texto + "|");
        float con = medida.width;
        medida.setText(f, "|");
        return Math.max(0, con - medida.width);
    }

    /** Escribe un texto con un contorno sólido alrededor para garantizar alto contraste sobre cualquier fondo. */
    public float textoConBorde(String s, float x, float y, float tamano, boolean negrita, int rgb, int bordeRgb, float alfa) {
        if (s == null || s.isEmpty()) return 0;
        float g = Math.max(1.4f, tamano * 0.09f);
        float d = g * 0.75f;
        texto(s, x - g, y, tamano, negrita, bordeRgb, alfa);
        texto(s, x + g, y, tamano, negrita, bordeRgb, alfa);
        texto(s, x, y - g, tamano, negrita, bordeRgb, alfa);
        texto(s, x, y + g, tamano, negrita, bordeRgb, alfa);
        texto(s, x - d, y - d, tamano, negrita, bordeRgb, alfa);
        texto(s, x + d, y - d, tamano, negrita, bordeRgb, alfa);
        texto(s, x - d, y + d, tamano, negrita, bordeRgb, alfa);
        texto(s, x + d, y + d, tamano, negrita, bordeRgb, alfa);
        return texto(s, x, y, tamano, negrita, rgb, alfa);
    }

    /**
     * Escribe un texto con un contorno negro fino de 1 px detrás para que sobresalga sobre el fondo
     * de los menús sin asignar objetos nuevos por fotograma.
     */
    public float textoConBordeNegro(String s, float x, float y, float tamano, boolean negrita, int rgb, float alfa) {
        if (s == null || s.isEmpty()) return 0;
        float alfaBorde = esColorMuyOscuro(rgb) ? alfa * 0.22f : alfa * 0.85f;
        texto(s, x - 1f, y, tamano, negrita, 0x000000, alfaBorde);
        texto(s, x + 1f, y, tamano, negrita, 0x000000, alfaBorde);
        texto(s, x, y - 1f, tamano, negrita, 0x000000, alfaBorde);
        texto(s, x, y + 1f, tamano, negrita, 0x000000, alfaBorde);
        return texto(s, x, y, tamano, negrita, rgb, alfa);
    }

    /** Escribe un texto centrado con contorno negro fino de 1 px detrás. */
    public float textoCentradoConBordeNegro(String s, float cx, float y, float tamano, boolean negrita, int rgb, float alfa) {
        float w = ancho(s, tamano, negrita);
        textoConBordeNegro(s, cx - w / 2, y, tamano, negrita, rgb, alfa);
        return w;
    }

    /** Escribe un texto alineado a la derecha con contorno negro fino de 1 px detrás. */
    public float textoDerechaConBordeNegro(String s, float xDerecha, float y, float tamano, boolean negrita, int rgb, float alfa) {
        float w = ancho(s, tamano, negrita);
        textoConBordeNegro(s, xDerecha - w, y, tamano, negrita, rgb, alfa);
        return w;
    }

    private static boolean esColorMuyOscuro(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r * 299 + g * 587 + b * 114 < 70000;
    }

    /**
     * Una línea con colores por trozos, de una vez (la fuente coloca cada letra: en el editor de código,
     * así ninguna letra se monta en otra). Cada trozo: {texto, color RGB}.
     */
    public void textoDeColores(java.util.List<Object[]> trozos, float x, float y, float tamano) {
        if (trozos.isEmpty()) return;
        bufferColores.setLength(0);
        for (Object[] t : trozos) {
            String s = (String) t[0];
            if (s.isEmpty()) continue;
            anadirTrozoMarcado(bufferColores, s, ((Integer) t[1]) & 0xFFFFFF);
        }
        textoDeCodigoMarcado(bufferColores, x, y, tamano);
    }

    /** Escribe una línea de código ya formateada con marcas de color de BitmapFont, sin asignaciones por fotograma. */
    public void textoDeCodigoMarcado(CharSequence marcado, float x, float y, float tamano) {
        if (marcado == null || marcado.length() == 0) return;
        BitmapFont f = letraDeCodigo(tamano, false);
        boolean antes = f.getData().markupEnabled;
        f.getData().markupEnabled = true;
        f.setColor(1, 1, 1, 1);
        f.draw(lote, marcado, x, y);
        f.getData().markupEnabled = antes;
    }

    /** Añade un trozo con color RGB en formato de marcas de BitmapFont escapando '[' sin crear cadenas temporales. */
    public static void anadirTrozoMarcado(StringBuilder b, String s, int rgb) {
        b.append("[#");
        b.append(HEX[(rgb >> 20) & 0xF]).append(HEX[(rgb >> 16) & 0xF]);
        b.append(HEX[(rgb >> 12) & 0xF]).append(HEX[(rgb >> 8) & 0xF]);
        b.append(HEX[(rgb >> 4) & 0xF]).append(HEX[rgb & 0xF]).append(']');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c);
            if (c == '[') b.append('[');
        }
        b.append("[]");
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

    /** Escribe un texto alineado a la derecha con la fuente monoespaciada de código. */
    public float textoDeCodigoDerecha(String s, float xDerecha, float y, float tamano, boolean negrita, int rgb, float alfa) {
        float w = anchoDeCodigo(s, tamano, negrita);
        textoDeCodigo(s, xDerecha - w, y, tamano, negrita, rgb, alfa);
        return w;
    }

    /** Un texto que se parte en líneas para caber en «ancho». Devuelve el alto que ocupa. */
    public float parrafo(String s, float x, float y, float anchoMax, float tamano, int rgb, float alfa) {
        return parrafo(s, x, y, anchoMax, tamano, false, rgb, alfa);
    }

    /** Lo mismo, en negrita o no (los títulos largos también se parten: nada se sale de su marco). */
    public float parrafo(String s, float x, float y, float anchoMax, float tamano, boolean negrita, int rgb, float alfa) {
        return parrafoConFuente(s, x, y, anchoMax, tamano, negrita, false, rgb, alfa);
    }

    /** Párrafo en fuente monoespaciada de código/depurador. Devuelve el alto ocupado. */
    public float parrafoDeCodigo(String s, float x, float y, float anchoMax, float tamano, boolean negrita, int rgb, float alfa) {
        return parrafoConFuente(s, x, y, anchoMax, tamano, negrita, true, rgb, alfa);
    }

    private float parrafoConFuente(String s, float x, float y, float anchoMax, float tamano, boolean negrita, boolean codigo, int rgb, float alfa) {
        if (s == null || s.isEmpty()) return 0;
        float alto = altoDeLinea(tamano);
        float yy = y;
        for (String linea : partir(s, anchoMax, tamano, negrita, codigo)) {
            textoConFuente(linea, x, yy, tamano, negrita, codigo, rgb, alfa);
            yy += alto;
        }
        return yy - y;
    }

    public float altoDeParrafo(String s, float anchoMax, float tamano) {
        return altoDeParrafo(s, anchoMax, tamano, false);
    }

    public float altoDeParrafo(String s, float anchoMax, float tamano, boolean negrita) {
        if (s == null || s.isEmpty()) return 0;
        return partir(s, anchoMax, tamano, negrita, false).size() * altoDeLinea(tamano);
    }

    /** Alto de un párrafo en fuente monoespaciada de código/depurador. */
    public float altoDeParrafoDeCodigo(String s, float anchoMax, float tamano, boolean negrita) {
        if (s == null || s.isEmpty()) return 0;
        return partir(s, anchoMax, tamano, negrita, true).size() * altoDeLinea(tamano);
    }

    /** Clave sin asignaciones en consulta para el caché de párrafos partidos. */
    private static final class ClavePartir {
        String s;
        int anchoQ, tamQ, escQ;
        boolean negrita, codigo;
        int hash;

        ClavePartir() {
        }

        ClavePartir(String s, int anchoQ, int tamQ, int escQ, boolean negrita, boolean codigo) {
            fijar(s, anchoQ, tamQ, escQ, negrita, codigo);
        }

        void fijar(String s, int anchoQ, int tamQ, int escQ, boolean negrita, boolean codigo) {
            this.s = s;
            this.anchoQ = anchoQ;
            this.tamQ = tamQ;
            this.escQ = escQ;
            this.negrita = negrita;
            this.codigo = codigo;
            int h = s.hashCode();
            h = 31 * h + anchoQ;
            h = 31 * h + tamQ;
            h = 31 * h + escQ;
            h = 31 * h + (negrita ? 1 : 0) + (codigo ? 2 : 0);
            this.hash = h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClavePartir c)) return false;
            return anchoQ == c.anchoQ && tamQ == c.tamQ && escQ == c.escQ
                    && negrita == c.negrita && codigo == c.codigo && s.equals(c.s);
        }
    }

    /** Los textos ya partidos (se parten los mismos en cada fotograma: medir cada palabra cuesta). */
    private final java.util.Map<ClavePartir, java.util.List<String>> partidos = new java.util.HashMap<>();
    private final ClavePartir sondaPartir = new ClavePartir();

    /** Parte un texto en líneas que caben en anchoMax (respeta los saltos de línea; una palabra que no cabe sola, se corta). */
    public java.util.List<String> partir(String s, float anchoMax, float tamano, boolean negrita) {
        return partir(s, anchoMax, tamano, negrita, false);
    }

    private java.util.List<String> partir(String s, float anchoMax, float tamano, boolean negrita, boolean codigo) {
        int anchoQ = Math.round(anchoMax);
        int tamQ = Math.round(tamano * 10);
        int escQ = Math.round(escala * 100);
        sondaPartir.fijar(s, anchoQ, tamQ, escQ, negrita, codigo);
        java.util.List<String> hecho = partidos.get(sondaPartir);
        if (hecho != null) return hecho;
        boolean enriquecido = !codigo && TextoEnriquecido.tieneMarcas(s);
        java.util.List<String> l = new java.util.ArrayList<>();
        for (String parrafo : s.split("\n", -1)) {
            StringBuilder linea = new StringBuilder();
            for (String palabra : parrafo.split(" ", -1)) {
                String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
                String pruebaMedida = enriquecido && TextoEnriquecido.quedaAbierta(prueba) ? prueba + "]]" : prueba;
                if (anchoConFuente(pruebaMedida, tamano, negrita, codigo) > anchoMax && linea.length() > 0) {
                    String hecha = linea.toString();
                    boolean abierta = enriquecido && TextoEnriquecido.quedaAbierta(hecha);
                    l.add(abierta ? hecha + "]]" : hecha);
                    linea.setLength(0);
                    linea.append(abierta ? "[[" + palabra : palabra);
                } else {
                    linea.setLength(0);
                    linea.append(prueba);
                }
                // Una palabra más larga que la línea entera (una ruta, una palabra alemana): se parte por letras.
                while (linea.length() > 1 && anchoConFuente(
                        enriquecido && TextoEnriquecido.quedaAbierta(linea.toString()) ? linea + "]]" : linea.toString(),
                        tamano, negrita, codigo) > anchoMax) {
                    int n = linea.length() - 1;
                    while (n > 1 && anchoConFuente(
                            enriquecido && TextoEnriquecido.quedaAbierta(linea.substring(0, n)) ? linea.substring(0, n) + "]]" : linea.substring(0, n),
                            tamano, negrita, codigo) > anchoMax) n--;
                    String trozo = linea.substring(0, n);
                    boolean abierta = enriquecido && TextoEnriquecido.quedaAbierta(trozo);
                    l.add(abierta ? trozo + "]]" : trozo);
                    linea.delete(0, n);
                    if (abierta) linea.insert(0, "[[");
                }
            }
            l.add(linea.toString());
        }
        if (partidos.size() > 600) partidos.clear();
        java.util.List<String> fijo = java.util.Collections.unmodifiableList(l);
        partidos.put(new ClavePartir(s, anchoQ, tamQ, escQ, negrita, codigo), fijo);
        return fijo;
    }

    public float ancho(String s, float tamano, boolean negrita) {
        return anchoConFuente(s, tamano, negrita, false);
    }

    /** Ancho de un texto medido con la fuente monoespaciada de código. */
    public float anchoDeCodigo(String s, float tamano, boolean negrita) {
        return anchoConFuente(s, tamano, negrita, true);
    }

    private float anchoConFuente(String s, float tamano, boolean negrita, boolean codigo) {
        if (s == null || s.isEmpty()) return 0;
        if (!codigo && TextoEnriquecido.tieneMarcas(s)) {
            BitmapFont fPeq = letraVersalita(tamano);
            float w = 0;
            for (TextoEnriquecido.Trozo t : TextoEnriquecido.aTrozos(s, negrita)) {
                BitmapFont f = t.pequeno ? fPeq : letra(tamano, t.negrita, false);
                w += anchoExacto(f, t.texto);
            }
            return w;
        }
        medida.setText(letra(tamano, negrita, codigo), s);
        return medida.width;
    }

    /** El alto de una línea de texto (con su espacio; algo más de aire en tamaños pequeños). */
    public float altoDeLinea(float tamano) {
        float t = Math.max(LETRA_MINIMA, tamano);
        return t * (t <= 18f ? 1.42f : 1.36f);
    }

    /** El ancho de una letra de la fuente de código (todas iguales: es monoespaciada). */
    public float anchoDeLetra(float tamano) {
        // Lo que avanza cada letra (no lo que mide su dibujo): la diferencia entre dos letras y una.
        BitmapFont f = letraDeCodigo(tamano, false);
        medida.setText(f, "MMMMMMMMMMM");
        float once = medida.width;
        medida.setText(f, "M");
        return (once - medida.width) / 10;
    }

    // ================================================================ recortes

    private final Rectangle recorte = new Rectangle(), escena = new Rectangle();
    private final Rectangle[] pilaRecortes = new Rectangle[32];
    private final boolean[] apilados = new boolean[32];
    private int recortes;

    /** Solo se dibuja dentro de este rectángulo hasta sinRecorte(). Se pueden anidar. */
    public void recortar(float x, float y, float w, float h) {
        lote.flush();
        // ScissorStack trabaja en píxeles de la ventana, con la y hacia arriba.
        escena.set(x * escala, (alto - y - h) * escala, Math.max(0, w) * escala, Math.max(0, h) * escala);
        recorte.set(escena);
        if (recortes < apilados.length) {
            Rectangle slot = pilaRecortes[recortes];
            slot.set(recorte);
            apilados[recortes] = ScissorStack.pushScissors(slot);
            recortes++;
        }
    }

    public void sinRecorte() {
        if (recortes <= 0) return;
        lote.flush();
        recortes--;
        if (apilados[recortes]) {
            ScissorStack.popScissors();
            apilados[recortes] = false;
        }
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
        partidos.clear();
        generadorUi.dispose();
        generadorTitulo.dispose();
        if (generadorCodigo != null) {
            generadorCodigo.dispose();
            generadorCodigo = null;
        }
        if (generadorCodigoNegrita != null) {
            generadorCodigoNegrita.dispose();
            generadorCodigoNegrita = null;
        }
        generadorSimbolos.dispose();
        generadorSimbolos2.dispose();
        blanco.dispose();
        lote.dispose();
    }
}
