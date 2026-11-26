package metajavbots.pruebas;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.backends.headless.HeadlessFiles;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SharedLibraryLoader;

import metajavbots.pantalla.Fuentes;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.TextoEnriquecido;

/**
 * MEDIDOR DE TEXTOS — Mide con las mismas fuentes y parámetros que {@link Pincel}, sin ventana y sin OpenGL.
 * Usa {@code generateData} y {@link GlyphLayout}, sin crear texturas, así que el ancho medido es el que
 * vería el jugador. Sirve para comprobar recuadros, márgenes y solapes en los tres idiomas y dos letras.
 */
final class MedidorDeTextos implements AutoCloseable {

    /** Los caracteres que {@code Pincel} pide a las fuentes (letras de los tres idiomas y símbolos del juego). */
    private static final String LETRAS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóúüñÁÉÍÓÚÜÑ¿¡«»…—–·×÷±≤≥≠≈°äöüßÄÖÜàèìòùâêîôûçÇ€¢£¥©®™•▲▼►◄■□▣●○◆◇★☆✓✔✗✘⚠⏱→←↑↓↔⇒│─┌┐└┘├┤┬┴┼█▌▐░▒▓∞λπΣΩµ“”„‹›−⌨▤▶⚔⚙✎✕❚↺⚄";

    private final FreeTypeFontGenerator fuenteUi, fuenteTitulo, fuenteCodigo, fuenteCodigoNegrita, fuenteSimbolos, fuenteSimbolos2;
    private final Map<Integer, BitmapFont> fuentes = new HashMap<>();
    private final GlyphLayout medida = new GlyphLayout();

    MedidorDeTextos() {
        new SharedLibraryLoader().load("gdx");
        new SharedLibraryLoader().load("gdx-freetype");
        Gdx.files = new HeadlessFiles();
        Gdx.graphics = (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(), new Class<?>[]{Graphics.class},
                (proxy, metodo, argumentos) -> "getGL20".equals(metodo.getName()) ? Gdx.gl : valorPorDefecto(metodo.getReturnType()));
        // FreeType crea texturas temporales al generar los datos: un GL mudo basta para medir, nunca se dibuja.
        Gdx.gl = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class<?>[]{GL20.class},
                (proxy, metodo, argumentos) -> valorPorDefecto(metodo.getReturnType()));
        Gdx.gl20 = Gdx.gl;
        fuenteUi = new FreeTypeFontGenerator(Gdx.files.classpath(Fuentes.INTERFAZ));
        fuenteTitulo = new FreeTypeFontGenerator(Gdx.files.classpath(Fuentes.TITULO));
        fuenteCodigo = new FreeTypeFontGenerator(Gdx.files.classpath(Fuentes.CODIGO));
        fuenteCodigoNegrita = new FreeTypeFontGenerator(Gdx.files.classpath(Fuentes.CODIGO_NEGRITA));
        fuenteSimbolos = new FreeTypeFontGenerator(Gdx.files.classpath(Fuentes.SIMBOLOS));
        fuenteSimbolos2 = new FreeTypeFontGenerator(Gdx.files.classpath(Fuentes.SIMBOLOS_2));
    }

    /** Ancho en unidades lógicas de la pantalla (escala 1), con la letra mínima del juego. */
    float ancho(String s, float tamano, boolean negrita, boolean codigo) {
        if (s == null || s.isEmpty()) return 0f;
        if (!codigo && TextoEnriquecido.tieneMarcas(s)) {
            float total = 0f;
            for (TextoEnriquecido.Trozo t : TextoEnriquecido.aTrozos(s, negrita)) {
                total += anchoExacto(fuente(t.pequeno ? versalita(tamano) : tamano, t.negrita || t.pequeno, false), t.texto);
            }
            return total;
        }
        return anchoExacto(fuente(tamano, negrita, codigo), s);
    }

    /** Igual que {@code Pincel.altoDeLinea}. */
    float altoDeLinea(float tamano) {
        float t = Math.max(Pincel.LETRA_MINIMA, tamano);
        return t * (t <= 18f ? 1.42f : 1.36f);
    }

    /** Alto de un párrafo con saltos de línea y palabras que bajan de línea al no caber. */
    float altoDeParrafo(String s, float anchoMax, float tamano) {
        if (s == null || s.isEmpty()) return 0f;
        float alto = 0f;
        for (String linea : s.split("\\r?\\n", -1)) {
            float usado = 0f;
            alto += altoDeLinea(tamano);
            for (String palabra : linea.split(" +")) {
                float w = ancho(palabra + " ", tamano, false, false);
                if (usado + w > anchoMax && usado > 0f) {
                    alto += altoDeLinea(tamano);
                    usado = 0f;
                }
                usado += w;
            }
        }
        return alto;
    }

    private float versalita(float tamano) {
        return Math.max(10.5f, Math.max(Pincel.LETRA_MINIMA, tamano) * TextoEnriquecido.ESCALA_VERSALITA);
    }

    private float anchoExacto(BitmapFont f, String texto) {
        if (texto.isEmpty()) return 0f;
        medida.setText(f, texto + "|");
        float con = medida.width;
        medida.setText(f, "|");
        return Math.max(0f, con - medida.width);
    }

    private BitmapFont fuente(float tamano, boolean negrita, boolean codigo) {
        int px = Math.max(6, Math.round(Math.max(Pincel.LETRA_MINIMA, tamano)));
        int clave = (px << 2) | (codigo ? 2 : 0) | (negrita ? 1 : 0);
        BitmapFont f = fuentes.get(clave);
        if (f != null) return f;
        FreeTypeFontGenerator primario = codigo ? (negrita ? fuenteCodigoNegrita : fuenteCodigo) : (negrita ? fuenteTitulo : fuenteUi);
        StringBuilder enPrimario = new StringBuilder(), enCodigo = new StringBuilder();
        StringBuilder enSimbolos = new StringBuilder(), enSimbolos2 = new StringBuilder();
        for (int i = 0; i < LETRAS.length(); i++) {
            char c = LETRAS.charAt(i);
            if (primario.hasGlyph(c)) enPrimario.append(c);
            else if (!codigo && fuenteCodigo.hasGlyph(c)) enCodigo.append(c);
            else if (fuenteSimbolos.hasGlyph(c)) enSimbolos.append(c);
            else if (fuenteSimbolos2.hasGlyph(c)) enSimbolos2.append(c);
        }
        int espacioX = !codigo && px <= 18 ? 1 : 0;
        FreeTypeFontGenerator.FreeTypeBitmapFontData data = primario.generateData(parametros(px, enPrimario.toString(), espacioX));
        anadirRespaldo(data, fuenteCodigo, enCodigo.toString(), px, espacioX);
        anadirRespaldo(data, fuenteSimbolos, enSimbolos.toString(), px, espacioX);
        anadirRespaldo(data, fuenteSimbolos2, enSimbolos2.toString(), px, espacioX);
        // Una región vacía: GlyphLayout solo lee avances y kerning, nunca pide la textura.
        Array<TextureRegion> regiones = new Array<>();
        regiones.add(new TextureRegion(new Texture(1, 1, Pixmap.Format.RGBA8888)));
        f = new BitmapFont(data, regiones, true);
        f.setUseIntegerPositions(true);
        data.missingGlyph = data.getGlyph('?');
        fuentes.put(clave, f);
        return f;
    }

    private static FreeTypeFontGenerator.FreeTypeFontParameter parametros(int px, String chars, int espacioX) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = px;
        p.flip = true;
        p.characters = chars;
        p.kerning = true;
        p.spaceX = espacioX;
        p.hinting = FreeTypeFontGenerator.Hinting.AutoSlight;
        p.incremental = false;
        return p;
    }

    private static void anadirRespaldo(FreeTypeFontGenerator.FreeTypeBitmapFontData destino,
            FreeTypeFontGenerator respaldo, String chars, int px, int espacioX) {
        if (chars.isEmpty()) return;
        FreeTypeFontGenerator.FreeTypeBitmapFontData fb = respaldo.generateData(parametros(px, chars, espacioX));
        int ajuste = Math.round((destino.capHeight - fb.capHeight) * 0.5f);
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            BitmapFont.Glyph g = fb.getGlyph(c);
            if (g != null && g.id == c && destino.getGlyph(c) == null) {
                g.yoffset += ajuste;
                destino.setGlyph(c, g);
            }
        }
    }

    /** Valor neutro para los métodos del GL y del Graphics mudos: nunca se dibuja, solo se mide. */
    private static Object valorPorDefecto(Class<?> tipo) {
        if (!tipo.isPrimitive()) return null;
        if (tipo == int.class) return 1;
        if (tipo == boolean.class) return false;
        if (tipo == float.class) return 0f;
        if (tipo == long.class) return 1L;
        if (tipo == double.class) return 0d;
        if (tipo == short.class) return (short) 1;
        if (tipo == byte.class) return (byte) 1;
        if (tipo == char.class) return (char) 1;
        return null;
    }

    @Override
    public void close() {
        for (BitmapFont f : fuentes.values()) f.dispose();
        fuenteUi.dispose();
        fuenteTitulo.dispose();
        fuenteCodigo.dispose();
        fuenteCodigoNegrita.dispose();
        fuenteSimbolos.dispose();
        fuenteSimbolos2.dispose();
    }
}
