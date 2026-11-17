package castillafantasx.interfaz;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import castillafantasx.arte.Imagen;
import castillafantasx.paisaje.Paisaje;
import castillafantasx.paisaje.Trozo;
import castillafantasx.superficie.MapaSuperficie;

/**
 * DIBUJO DEL PAISAJE — Lleva a la pantalla el suelo que pinta
 * castillafantasx.paisaje. El paisaje se pinta a trozos de 256 píxeles; aquí cada
 * trozo se sube a una textura (sus tres dibujos, uno al lado de otro: noche,
 * mañana y tarde) y se guarda mientras ande cerca de la cámara.
 *
 * Para que andar no dé tirones, los trozos de alrededor de la pantalla los
 * pinta un «ayudante» en otro hilo; el juego solo pinta él mismo los que
 * hacen falta ya y aún no están (al empezar, o tras un «Ir a»).
 *
 * Las texturas se crean según hacen falta y se reutilizan: cuando un trozo
 * queda lejos, su textura sirve para otro.
 */
public class DibujoPaisaje {

    private static final int LADO = Trozo.LADO;
    private static final int MARGEN = 200;              // píxeles alrededor de la pantalla que se tienen pintados de antemano
    private static final int TEXTURAS_DE_SOBRA = 12;    // las que se guardan aunque ya no se vean, por si se vuelve

    /** Un trozo con su textura. */
    private static final class Hueco {
        final Trozo trozo = new Trozo();
        Texture textura;
        boolean subido;         // la textura ya tiene el dibujo de este trozo
        boolean enUso;          // guarda un trozo (si no, está libre)
        long visto;             // el último frame en que hizo falta
    }

    /**
     * El «pincel» con que se pinta el paisaje. En cada píxel de un trozo, la
     * opacidad no es opacidad: dice de qué fila de casillas es ese trozo de
     * suelo en alto (0 = llano). Con el rojo del tinte a 0 se pinta todo; con
     * otro valor, solo los píxeles de esa fila: así cada fila en alto se
     * vuelve a pintar en su turno y tapa a quien pasa por detrás. La
     * opacidad del tinte es con la que se pinta (para fundir noche y día).
     */
    private static final String VERTICES =
            "attribute vec4 a_position;\n"
            + "attribute vec4 a_color;\n"
            + "attribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\n"
            + "varying vec4 v_tinte;\n"
            + "varying vec2 v_punto;\n"
            + "void main() {\n"
            + "    v_tinte = a_color;\n"
            + "    v_tinte.a = v_tinte.a * (255.0 / 254.0);\n"
            + "    v_punto = a_texCoord0;\n"
            + "    gl_Position = u_projTrans * a_position;\n"
            + "}\n";
    private static final String PIXELES =
            "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec4 v_tinte;\n"
            + "varying vec2 v_punto;\n"
            + "uniform sampler2D u_texture;\n"
            + "void main() {\n"
            + "    vec4 suelo = texture2D(u_texture, v_punto);\n"
            + "    if (v_tinte.r > 0.0 && abs(suelo.a - v_tinte.r) > 0.002) discard;\n"
            + "    gl_FragColor = vec4(suelo.rgb, v_tinte.a);\n"
            + "}\n";

    private final ShaderProgram pincel = new ShaderProgram(VERTICES, PIXELES);
    private final Pixmap paso = new Pixmap(LADO * 3, LADO, Pixmap.Format.RGBA8888);     // por donde pasa cada trozo camino de su textura
    private final IntBuffer pixelesDePaso = paso.getPixels().order(ByteOrder.BIG_ENDIAN).asIntBuffer();
    private final int[] fila = new int[LADO * 3];
    private final List<Hueco> huecos = new ArrayList<>();
    private final Map<Long, Hueco> porSitio = new HashMap<>();
    private final ExecutorService ayudante = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "paisaje");
        hilo.setDaemon(true);       // si el juego se cierra, el ayudante no lo retiene
        return hilo;
    });
    private volatile Paisaje paisaje;      // «volatile»: también lo lee el ayudante, desde su hilo
    private int huella;
    private long frame;

    public DibujoPaisaje() {
        if (!pincel.isCompiled()) throw new IllegalStateException("El pincel del paisaje no compila: " + pincel.getLog());
    }

    /** El paisaje del mapa que se está viendo (si el mapa es otro, se empieza de nuevo). */
    public Paisaje paisajeDe(MapaSuperficie mapa) {
        if (paisaje == null || huella != mapa.huella) {
            paisaje = new Paisaje(mapa);
            huella = mapa.huella;
            // Los trozos del mapa anterior ya no valen (si el ayudante tiene uno a medias, se le espera).
            porSitio.clear();
            encargados.clear();
            for (Hueco h : huecos) {
                while (h.trozo.estado.get() == Trozo.PINTANDOSE) Thread.onSpinWait();
                h.enUso = false;
            }
        }
        return paisaje;
    }

    private static long clave(int columna, int fila) {
        return ((long) columna << 32) ^ (fila & 0xFFFFFFFFL);
    }

    /** El hueco del trozo (columna, fila): el que ya lo guarda o, si no, uno libre. */
    private Hueco hueco(int columna, int fila) {
        Hueco h = porSitio.get(clave(columna, fila));
        if (h == null) {
            h = huecoLibre();
            h.trozo.empezar(columna, fila);
            h.subido = false;
            h.enUso = true;
            porSitio.put(clave(columna, fila), h);
        }
        h.visto = frame;
        return h;
    }

    /** Un hueco que no guarde nada; si no hay, el del trozo que hace más que no se ve (si ya sobran bastantes); y si no, uno nuevo. */
    private Hueco huecoLibre() {
        Hueco viejo = null;
        int sobran = 0;
        for (Hueco otro : huecos) {
            if (!otro.enUso) return otro;
            // Los que se ven ahora, y los que tiene a medias el ayudante, no se tocan.
            if (otro.visto >= frame - 1 || otro.trozo.estado.get() != Trozo.LISTO) continue;
            sobran++;
            if (viejo == null || otro.visto < viejo.visto) viejo = otro;
        }
        if (viejo != null && sobran > TEXTURAS_DE_SOBRA) {
            porSitio.remove(clave(viejo.trozo.columna, viejo.trozo.fila));
            return viejo;
        }
        Hueco nuevo = new Hueco();
        nuevo.textura = new Texture(LADO * 3, LADO, Pixmap.Format.RGBA8888);
        nuevo.textura.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        huecos.add(nuevo);
        return nuevo;
    }

    /** Sube a su textura el dibujo de un trozo ya pintado: noche, mañana y tarde, uno al lado de otro. */
    private void subir(Hueco h) {
        Imagen[] dibujos = { h.trozo.noche, h.trozo.manana, h.trozo.tarde };
        pixelesDePaso.clear();
        for (int y = 0; y < LADO; y++) {
            for (int d = 0; d < 3; d++) {
                int[] px = dibujos[d].px;
                for (int x = 0; x < LADO; x++) {
                    int c = px[y * LADO + x];
                    fila[d * LADO + x] = (c << 8) | (c >>> 24);         // de 0xAARRGGBB a 0xRRGGBBAA, que es como lo quiere la textura
                }
            }
            pixelesDePaso.put(fila);
        }
        h.textura.draw(paso, 0, 0);
        h.subido = true;
    }

    /**
     * Antes de pintar: deja listos los trozos que se ven desde (camX, camY)
     * y encarga al ayudante los de alrededor.
     */
    public void preparar(float camX, float camY, int ancho, int alto) {
        frame++;
        int c0 = Math.floorDiv((int) camX, LADO), c1 = Math.floorDiv((int) camX + ancho, LADO);
        int f0 = Math.floorDiv((int) camY, LADO), f1 = Math.floorDiv((int) camY + alto, LADO);
        // Los de alrededor: al ayudante.
        int mc0 = Math.floorDiv((int) camX - MARGEN, LADO), mc1 = Math.floorDiv((int) camX + ancho + MARGEN, LADO);
        int mf0 = Math.floorDiv((int) camY - MARGEN, LADO), mf1 = Math.floorDiv((int) camY + alto + MARGEN, LADO);
        for (int f = mf0; f <= mf1; f++) {
            for (int c = mc0; c <= mc1; c++) {
                Hueco h = hueco(c, f);
                boolean seVe = c >= c0 && c <= c1 && f >= f0 && f <= f1;
                if (!seVe && h.trozo.estado.get() == Trozo.PENDIENTE && h.visto == frame && !h.subido && !encargado(h)) {
                    Trozo trozo = h.trozo;
                    Paisaje suyo = paisaje;
                    encargados.add(h);
                    ayudante.execute(() -> {
                        if (suyo == paisaje) suyo.hornear(trozo, true);
                    });
                }
            }
        }
        // Los que se ven: los que falten se pintan ahora mismo (si uno lo tiene a medias el ayudante, se le espera).
        for (int f = f0; f <= f1; f++) {
            for (int c = c0; c <= c1; c++) {
                Hueco h = hueco(c, f);
                paisaje.hornear(h.trozo, false);
                while (h.trozo.estado.get() != Trozo.LISTO) Thread.onSpinWait();
            }
        }
        // Los que ya estén pintados y sin subir, a su textura.
        for (Hueco h : huecos) {
            if (h.enUso && !h.subido && h.trozo.estado.get() == Trozo.LISTO) {
                subir(h);
                encargados.remove(h);
            }
        }
        soltarLasQueSobran();
    }

    // Cada cuántos frames se mira si sobran texturas, y cuánto tiene que llevar una sin verse para soltarla (60 frames = un segundo).
    private static final int FRAMES_ENTRE_LIMPIEZAS = 120, FRAMES_SIN_VERSE = 600;

    /**
     * Las texturas que llevan mucho sin verse y pasan de las de sobra se devuelven a la tarjeta gráfica. Tras
     * alejarse con el modo pájaro (o con una ventana enorme) se quedaban cientos pedidas para siempre.
     */
    private void soltarLasQueSobran() {
        if (frame % FRAMES_ENTRE_LIMPIEZAS != 0) return;
        int sobran = 0;
        for (Hueco h : huecos) {
            if (h.visto < frame - FRAMES_SIN_VERSE && h.trozo.estado.get() != Trozo.PINTANDOSE) sobran++;
        }
        for (int i = huecos.size() - 1; i >= 0 && sobran > TEXTURAS_DE_SOBRA; i--) {
            Hueco h = huecos.get(i);
            if (h.visto >= frame - FRAMES_SIN_VERSE || h.trozo.estado.get() == Trozo.PINTANDOSE) continue;
            if (h.enUso && porSitio.get(clave(h.trozo.columna, h.trozo.fila)) == h) porSitio.remove(clave(h.trozo.columna, h.trozo.fila));
            encargados.remove(h);
            h.textura.dispose();
            huecos.remove(i);
            sobran--;
        }
    }

    /** Cuántas texturas de paisaje tiene pedidas ahora (para las pruebas). */
    public int texturas() {
        return huecos.size();
    }

    private final List<Hueco> encargados = new ArrayList<>();

    private boolean encargado(Hueco h) {
        return encargados.contains(h);
    }

    /** Pinta con el pincel el rectángulo (x0, y0)-(x1, y1) del paisaje, de uno de los tres dibujos (0 = noche, 1 = mañana, 2 = tarde). */
    private void rect(Lapiz lapiz, int dibujo, float x0, float y0, float x1, float y1) {
        for (int f = Math.floorDiv((int) y0, LADO); f <= Math.floorDiv((int) y1 - 1, LADO); f++) {
            for (int c = Math.floorDiv((int) x0, LADO); c <= Math.floorDiv((int) x1 - 1, LADO); c++) {
                Hueco h = porSitio.get(clave(c, f));
                if (h == null || !h.subido) continue;
                // La parte de este trozo que cae dentro del rectángulo.
                int px0 = Math.max((int) x0, c * LADO), py0 = Math.max((int) y0, f * LADO);
                int px1 = Math.min((int) x1, (c + 1) * LADO), py1 = Math.min((int) y1, (f + 1) * LADO);
                lapiz.lote.draw(h.textura, px0, py0, px1 - px0, py1 - py0,
                        dibujo * LADO + px0 - c * LADO, py0 - f * LADO, px1 - px0, py1 - py0, false, true);
            }
        }
    }

    /**
     * Pinta un rectángulo del paisaje a la hora que sea: primero la noche y,
     * encima, la mañana y la tarde con la opacidad que les toque. 'filaEnAlto':
     * 0 = todo el suelo; si no, solo el terreno en alto de esa fila de casillas (con Paisaje.marcaDeFila).
     */
    public void dibujar(Lapiz lapiz, float x0, float y0, float x1, float y1, float manana, float tarde, int filaEnAlto) {
        lapiz.lote.setShader(pincel);
        float marca = filaEnAlto == 0 ? 0 : (filaEnAlto + 0.5f) / 255f;
        lapiz.tinte(marca, 0, 0, 1);
        rect(lapiz, 0, x0, y0, x1, y1);
        if (manana > 0) {
            lapiz.tinte(marca, 0, 0, manana);
            rect(lapiz, 1, x0, y0, x1, y1);
        }
        if (tarde > 0) {
            lapiz.tinte(marca, 0, 0, tarde);
            rect(lapiz, 2, x0, y0, x1, y1);
        }
        lapiz.sinTinte();
        lapiz.lote.setShader(null);
    }

    public void liberar() {
        ayudante.shutdownNow();
        for (Hueco h : huecos) {
            h.textura.dispose();
        }
        paso.dispose();
        pincel.dispose();
    }
}
