package castillafantasx.interfaz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;

import castillafantasx.arte.Almacen;
import castillafantasx.arte.Imagen;

/**
 * LÁPIZ — Con lo que se dibuja en pantalla: los dibujos del Almacén subidos a
 * la tarjeta gráfica (una sola vez, todos en una textura grande) y las letras.
 *
 * Las coordenadas son las de la imagen del juego (Pantalla), con la y hacia
 * ABAJO: (0, 0) es la esquina de arriba a la izquierda.
 */
public class Lapiz {

    public final SpriteBatch lote = new SpriteBatch();
    private final Map<String, TextureRegion> dibujos = new HashMap<>();
    private final TextureAtlas atlas;
    private final BitmapFont letra, letraMediana, letraGrande;
    private final GlyphLayout medida = new GlyphLayout();

    private static final String LETRAS = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúüñÁÉÍÓÚÜÑ¿¡«»…—·×▲▼■←→↑↓◄►‹›●";

    public Lapiz() {
        // Todos los dibujos, empaquetados en una textura: así la tarjeta gráfica no cambia de textura a cada rato.
        PixmapPacker empaquetador = new PixmapPacker(2048, 2048, Pixmap.Format.RGBA8888, 2, true);
        for (Map.Entry<String, Imagen> e : Almacen.todas().entrySet()) {
            Pixmap p = aPixmap(e.getValue());
            empaquetador.pack(e.getKey(), p);
            p.dispose();
        }
        atlas = empaquetador.generateTextureAtlas(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, false);
        empaquetador.dispose();
        for (TextureAtlas.AtlasRegion r : atlas.getRegions()) {
            r.flip(false, true);        // la y de la pantalla va hacia abajo
            // El empaquetador separa el número final del nombre ("hierba_3" → "hierba", 3): se vuelve a juntar.
            dibujos.put(r.index >= 0 ? r.name + "_" + r.index : r.name, r);
        }

        FreeTypeFontGenerator generador = new FreeTypeFontGenerator(Gdx.files.internal("fuentes/DejaVuSansMono-Bold.ttf"));
        letra = generador.generateFont(parametros(11));
        letraMediana = generador.generateFont(parametros(LETRA_MEDIANA));
        letraGrande = generador.generateFont(parametros(26));
        generador.dispose();
        generadorNitido = new FreeTypeFontGenerator(Gdx.files.internal("fuentes/DejaVuSansMono.ttf"));
    }

    private static FreeTypeFontGenerator.FreeTypeFontParameter parametros(int tamano) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = tamano;
        p.flip = true;
        p.mono = true;                  // sin suavizado: letras de píxel, como el resto
        p.characters = LETRAS;
        p.minFilter = Texture.TextureFilter.Nearest;
        p.magFilter = Texture.TextureFilter.Nearest;
        return p;
    }

    /** Una Imagen (0xAARRGGBB) pasada a Pixmap de libGDX (0xRRGGBBAA). */
    public static Pixmap aPixmap(Imagen im) {
        Pixmap p = new Pixmap(im.ancho, im.alto, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);       // copiar tal cual, sin mezclar con lo que hubiera
        for (int y = 0; y < im.alto; y++) {
            for (int x = 0; x < im.ancho; x++) {
                int c = im.px[y * im.ancho + x];
                p.drawPixel(x, y, (c << 8) | (c >>> 24));
            }
        }
        return p;
    }

    public TextureRegion region(String nombre) {
        TextureRegion r = dibujos.get(nombre);
        // Un dibujo hecho después de arrancar (una imagen propia recién pegada con el editor): se sube una vez y se guarda.
        if (r == null && Almacen.existe(nombre)) r = subir(nombre, Almacen.imagen(nombre));
        // Una imagen propia que el editor ha vuelto a hacer (con otro tamaño): se sube la nueva, una vez.
        if (nombre.startsWith(castillafantasx.mundo.ImagenesPropias.PREFIJO) && Almacen.existe(nombre) && propias.get(nombre) != Almacen.imagen(nombre)) {
            r = subir(nombre, Almacen.imagen(nombre));
        }
        if (r == null) throw new IllegalArgumentException("No existe el dibujo «" + nombre + "»");
        return r;
    }

    /** Las texturas de los dibujos hechos después de arrancar: cada una se crea una vez. */
    private final java.util.List<Texture> sueltas = new java.util.ArrayList<>();

    /** De cada dibujo subido suelto, la imagen con la que se hizo: si el Almacen tiene ya otra, hay que subirla de nuevo. */
    private final Map<String, Imagen> propias = new HashMap<>();

    private TextureRegion subir(String nombre, Imagen im) {
        Pixmap p = aPixmap(im);
        Texture textura = new Texture(p);
        p.dispose();
        textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sueltas.add(textura);
        propias.put(nombre, im);
        TextureRegion r = new TextureRegion(textura);
        r.flip(false, true);        // la y de la pantalla va hacia abajo
        dibujos.put(nombre, r);
        return r;
    }

    /** Un texto a otro tamaño (1 = el de siempre) y girado esos grados alrededor de su centro, que queda en (x, y). */
    public void textoGirado(String texto, float x, float y, int rgb, float alfa, float escala, float grados) {
        letra.getData().setScale(escala);
        medida.setText(letra, texto);
        float ancho = medida.width, alto = medida.height;
        lote.flush();
        giro.idt().translate(x, y, 0).rotate(0, 0, 1, grados);
        lote.setTransformMatrix(giro);
        letra.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alfa);
        letra.draw(lote, texto, -ancho / 2, -alto / 2);
        lote.flush();
        lote.setTransformMatrix(giro.idt());
        letra.getData().setScale(1);
    }

    private final com.badlogic.gdx.math.Matrix4 giro = new com.badlogic.gdx.math.Matrix4();

    // ------------------------------------------------------------------
    // Dibujos
    // ------------------------------------------------------------------

    public void imagen(String nombre, float x, float y) {
        TextureRegion r = region(nombre);
        lote.draw(r, x, y, r.getRegionWidth(), r.getRegionHeight());
    }

    /** Un dibujo estirado a ancho × alto. */
    public void imagen(String nombre, float x, float y, float ancho, float alto) {
        lote.draw(region(nombre), x, y, ancho, alto);
    }

    /** Un dibujo, mirando al otro lado si 'espejo' es true. */
    public void imagenEspejo(String nombre, float x, float y, boolean espejo) {
        TextureRegion r = region(nombre);
        int w = r.getRegionWidth(), h = r.getRegionHeight();
        if (espejo) lote.draw(r, x + w, y, -w, h);
        else lote.draw(r, x, y, w, h);
    }

    /** Un dibujo ampliado y girado alrededor del punto (ejeX, ejeY) del propio dibujo. */
    public void imagenGirada(String nombre, float x, float y, float ejeX, float ejeY, float escala, float grados) {
        TextureRegion r = region(nombre);
        lote.draw(r, x, y, ejeX, ejeY, r.getRegionWidth(), r.getRegionHeight(), escala, escala, grados);
    }

    /** El color con el que se tiñe lo que se dibuje a partir de ahora (blanco = tal cual). */
    public void tinte(float r, float g, float b, float alfa) {
        lote.setColor(r, g, b, alfa);
    }

    public void tinte(int rgb, float alfa) {
        lote.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alfa);
    }

    public void sinTinte() {
        lote.setColor(1, 1, 1, 1);
    }

    /** Un rectángulo de un color. */
    public void rect(float x, float y, float ancho, float alto, int rgb, float alfa) {
        tinte(rgb, alfa);
        lote.draw(region("ui/blanco"), x, y, ancho, alto);
        sinTinte();
    }

    /** El borde de un rectángulo, de un píxel de grueso. */
    public void marco(float x, float y, float ancho, float alto, int rgb, float alfa) {
        rect(x, y, ancho, 1, rgb, alfa);
        rect(x, y + alto - 1, ancho, 1, rgb, alfa);
        rect(x, y + 1, 1, alto - 2, rgb, alfa);
        rect(x + ancho - 1, y + 1, 1, alto - 2, rgb, alfa);
    }

    /** Una caja oscura con marco, para diálogos y menús. */
    public void caja(float x, float y, float ancho, float alto) {
        rect(x, y, ancho, alto, 0x0B090D, 0.90f);
        rect(x, y, ancho, 1, 0x776A62, 1);
        rect(x, y + alto - 1, ancho, 1, 0x3B3236, 1);
        rect(x, y, 1, alto, 0x776A62, 1);
        rect(x + ancho - 1, y, 1, alto, 0x3B3236, 1);
    }

    // ------------------------------------------------------------------
    // Letras
    // ------------------------------------------------------------------

    public void texto(String texto, float x, float y, int rgb) {
        texto(texto, x, y, rgb, 1);
    }

    public void texto(String texto, float x, float y, int rgb, float alfa) {
        letra.setColor(0, 0, 0, alfa);              // sombra de un píxel: se lee sobre cualquier fondo
        letra.draw(lote, texto, x + 1, y + 1);
        ponerColor(letra, rgb, alfa);
        letra.draw(lote, texto, x, y);
    }

    /** Una pista de teclas, pegada al borde derecho, sobre un fondo oscuro para que se lea encima de cualquier cosa. */
    public void pista(String texto, float y) {
        float ancho = anchoDe(texto);
        rect(castillafantasx.Pantalla.ancho - ancho - 12, y - 2, ancho + 8, 14, 0x0B090D, 0.55f);
        texto(texto, castillafantasx.Pantalla.ancho - ancho - 8, y, castillafantasx.arte.Paleta.PIEDRA_CLARA, 0.9f);
    }

    public void textoCentrado(String texto, float centroX, float y, int rgb, float alfa) {
        medida.setText(letra, texto);
        texto(texto, Math.round(centroX - medida.width / 2), y, rgb, alfa);
    }

    /** Texto que salta de línea al llegar al ancho dado. */
    public void textoEnCaja(String texto, float x, float y, float ancho, int rgb) {
        ponerColor(letra, rgb, 1);
        letra.draw(lote, texto, x, y, ancho, Align.left, true);
    }

    /** Lo que mide de alto un texto que salta de línea al llegar al ancho dado. */
    public float altoDeTextoEnCaja(String texto, float ancho) {
        medida.setText(letra, texto, letra.getColor(), ancho, Align.left, true);
        return medida.height;
    }

    public void titulo(String texto, float centroX, float y, int rgb) {
        medida.setText(letraGrande, texto);
        float x = Math.round(centroX - medida.width / 2);
        letraGrande.setColor(0, 0, 0, 1);
        letraGrande.draw(lote, texto, x + 2, y + 2);
        ponerColor(letraGrande, rgb, 1);
        letraGrande.draw(lote, texto, x, y);
    }

    /** El tamaño de la letra mediana: lo elegido que tiene que sobresalir (el nombre de la casilla elegida de la rueda). */
    public static final int LETRA_MEDIANA = 15;

    /** Un texto con la letra mediana, centrado en 'centroX', con su sombra. */
    public void textoMediano(String texto, float centroX, float y, int rgb, float alfa) {
        medida.setText(letraMediana, texto);
        float x = Math.round(centroX - medida.width / 2);
        letraMediana.setColor(0, 0, 0, alfa);
        letraMediana.draw(lote, texto, x + 1, y + 1);
        ponerColor(letraMediana, rgb, alfa);
        letraMediana.draw(lote, texto, x, y);
    }

    public float anchoDeMediano(String texto) {
        medida.setText(letraMediana, texto);
        return medida.width;
    }

    public float anchoDe(String texto) {
        medida.setText(letra, texto);
        return medida.width;
    }

    // ------------------------------------------------------------------
    // Letras nítidas: a la resolución de la ventana (el visor de código)
    // ------------------------------------------------------------------

    /** La letra lisa (DejaVu Sans Mono normal, con suavizado) de cada tamaño en píxeles de la ventana: cada una se crea una vez. */
    private final Map<Integer, BitmapFont> letrasNitidas = new HashMap<>();
    private final FreeTypeFontGenerator generadorNitido;

    /** Un texto apuntado para pintarlo nítido encima de la imagen ya ampliada. */
    private static final class TextoNitido {
        final String texto;
        final float x, y, tamano, altoDeFila;
        final int rgb;
        /** El recorte que había al apuntarlo (cada panel, el suyo), o null. */
        float[] recorte;

        TextoNitido(String texto, float x, float y, int rgb, float tamano, float altoDeFila) {
            this.texto = texto;
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            this.tamano = tamano;
            this.altoDeFila = altoDeFila;
        }
    }

    private final List<TextoNitido> nitidos = new ArrayList<>();
    /** Dónde caben los textos nítidos (en píxeles de la imagen): lo que se salga, no se pinta. */
    private float[] recorteNitido;

    private BitmapFont letraNitida(int pixeles) {
        return letrasNitidas.computeIfAbsent(pixeles, px -> {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = px;
            p.flip = true;
            p.hinting = FreeTypeFontGenerator.Hinting.Full;     // los trazos, encajados en los píxeles: más limpios
            p.characters = LETRAS;
            p.incremental = true;           // una letra que no esté en LETRAS se hace al pedirla
            p.minFilter = Texture.TextureFilter.Nearest;
            p.magFilter = Texture.TextureFilter.Nearest;
            return generadorNitido.generateFont(p);
        });
    }

    /** Los píxeles de la ventana de una letra de 'tamano' píxeles de la imagen, con el aumento de ahora. */
    private static int pixelesNitidos(float tamano) {
        return Math.max(6, Math.round(tamano * castillafantasx.Pantalla.aumento));
    }

    /** Lo que avanza cada letra nítida (todas miden lo mismo), en píxeles de la imagen. */
    public float anchoDeLetraNitida(float tamano) {
        return letraNitida(pixelesNitidos(tamano)).getData().getGlyph('M').xadvance / (float) castillafantasx.Pantalla.aumento;
    }

    /** Apunta un texto para pintarlo nítido, centrado en una fila de ese alto que empieza en (x, y), en píxeles de la imagen. */
    public void textoNitido(String texto, float x, float y, int rgb, float tamano, float altoDeFila) {
        TextoNitido t = new TextoNitido(texto, x, y, rgb, tamano, altoDeFila);
        t.recorte = recorteNitido;
        nitidos.add(t);
    }

    /** Lo que se salga de este rectángulo (en píxeles de la imagen) no se pinta. */
    public void recorteNitido(float x, float y, float ancho, float alto) {
        recorteNitido = new float[] { x, y, ancho, alto };
    }

    /** Los textos nítidos que se apunten desde ahora no se recortan (hasta otro recorteNitido). */
    public void sinRecorteNitido() {
        recorteNitido = null;
    }

    /** Olvida los textos nítidos apuntados: se llama al empezar a pintar cada imagen. */
    public void olvidarTextosNitidos() {
        nitidos.clear();
        recorteNitido = null;
    }

    /**
     * Pinta los textos nítidos sobre la imagen ya ampliada: (x0, y0) es dónde
     * cae su esquina en la ventana, que mide 'altoDeLaVentana' de alto. Hay
     * que llamarlo con el lote abierto y la proyección de la ventana.
     */
    public void pintarTextosNitidos(float x0, float y0, int altoDeLaVentana) {
        if (nitidos.isEmpty()) return;
        int aumento = castillafantasx.Pantalla.aumento;
        float[] puesto = null;
        for (TextoNitido t : nitidos) {
            // Cada texto, con el recorte que había al apuntarlo: al cambiar de panel, cambia la tijera.
            if (t.recorte != puesto) {
                lote.flush();
                tijera(t.recorte, x0, y0, altoDeLaVentana, aumento);
                puesto = t.recorte;
            }
            BitmapFont f = letraNitida(pixelesNitidos(t.tamano));
            ponerColor(f, t.rgb, 1);
            float hueco = (t.altoDeFila * aumento - f.getLineHeight()) / 2;     // la letra, centrada en su fila
            f.draw(lote, t.texto, Math.round(x0 + t.x * aumento), Math.round(y0 + t.y * aumento + hueco));
        }
        if (puesto != null) {
            lote.flush();
            com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
        }
    }

    /** Pone la tijera en ese recorte (en píxeles de la imagen), o la quita si es null. */
    private static void tijera(float[] recorte, float x0, float y0, int altoDeLaVentana, int aumento) {
        if (recorte == null) {
            com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
            return;
        }
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_SCISSOR_TEST);
        // La tijera cuenta la y desde abajo de la ventana.
        float rx = x0 + recorte[0] * aumento, ry = y0 + recorte[1] * aumento;
        com.badlogic.gdx.Gdx.gl.glScissor(Math.round(rx), Math.round(altoDeLaVentana - ry - recorte[3] * aumento),
                Math.round(recorte[2] * aumento), Math.round(recorte[3] * aumento));
    }

    private static void ponerColor(BitmapFont f, int rgb, float alfa) {
        f.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, alfa);
    }

    public void liberar() {
        lote.dispose();
        atlas.dispose();
        for (Texture suelta : sueltas) suelta.dispose();
        letra.dispose();
        letraMediana.dispose();
        letraGrande.dispose();
        for (BitmapFont f : letrasNitidas.values()) f.dispose();
        generadorNitido.dispose();
    }
}
