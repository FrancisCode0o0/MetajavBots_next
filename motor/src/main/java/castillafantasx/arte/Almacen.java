package castillafantasx.arte;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

import castillafantasx.Contenido;
import castillafantasx.mundo.ImagenesPropias;
import castillafantasx.mundo.Manchas;

/**
 * ALMACÉN — Todos los dibujos del juego, en memoria, cada uno con su nombre
 * ("muros/ladrillo_0", "heroe/abajo_0"…). Se llenan UNA vez, al arrancar:
 * primero los PNG de res/arte (lista en res/arte/indice.txt) y después los
 * que fabrica el Taller. No necesita tarjeta gráfica.
 */
public final class Almacen {

    private Almacen() { }

    private static final Map<String, Imagen> imagenes = new TreeMap<>();

    // Los tiles de terceros se apagan un poco al cargar, para que casen con el tono oscuro del juego.
    private static final float CUANTO_APAGAR = 0.30f;
    private static final float TINTE_R = 0.88f, TINTE_G = 0.92f, TINTE_B = 0.96f;

    /** Carga y fabrica todo. Llamarlo dos veces no repite el trabajo. */
    public static synchronized void cargar() {
        if (!imagenes.isEmpty()) return;
        try (InputStream in = Almacen.class.getResourceAsStream("/arte/indice.txt")) {
            if (in == null) throw new IllegalStateException("Falta res/arte/indice.txt");
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                // "arte/muros/ladrillo_0.png" → "muros/ladrillo_0"
                String nombre = linea.substring("arte/".length(), linea.length() - ".png".length());
                Imagen im = leerPng("/" + linea);
                if (!nombre.startsWith("efectos/")) apagar(im);
                imagenes.put(nombre, im);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el arte", e);
        }
        Contenido.delJuego().fabricarDibujos(imagenes);    // los del juego (castillafantasx.Contenido)
        TallerDeIconos.fabricar(imagenes);         // los iconos de F1
        TallerDeCursores.fabricar(imagenes);       // los dibujos del cursor (la cruz y sus variantes)
        Contenido.delJuego().retocarDibujos(imagenes);     // lo que el juego retoca después (la veta de la madera)
        Manchas.fabricar(imagenes);           // las pinceladas del editor (sangre, barro, hollín, musgo y humedad)
        ImagenesPropias.cargar(imagenes);        // las imágenes de Fran pegadas con el editor (res/mundo/imagenes/)
    }

    private static Imagen leerPng(String recurso) throws IOException {
        try (InputStream in = Almacen.class.getResourceAsStream(recurso)) {
            if (in == null) throw new IllegalStateException("Falta el dibujo " + recurso);
            BufferedImage bi = ImageIO.read(in);
            Imagen im = new Imagen(bi.getWidth(), bi.getHeight());
            bi.getRGB(0, 0, im.ancho, im.alto, im.px, 0, im.ancho);
            return im;
        }
    }

    private static void apagar(Imagen im) {
        for (int i = 0; i < im.px.length; i++) {
            im.px[i] = Colores.mult(Colores.apagar(im.px[i], CUANTO_APAGAR), TINTE_R, TINTE_G, TINTE_B);
        }
    }

    /** El dibujo con ese nombre. Si no existe, el juego se para aquí y dice cuál falta. */
    public static Imagen imagen(String nombre) {
        Imagen im = imagenes.get(nombre);
        if (im == null) throw new IllegalArgumentException("No existe el dibujo «" + nombre + "»");
        return im;
    }

    public static boolean existe(String nombre) {
        return imagenes.containsKey(nombre);
    }

    /** Cuántas variantes numeradas tiene una serie: "muros/ladrillo" → ladrillo_0, _1… */
    public static int variantes(String serie) {
        int n = 0;
        while (imagenes.containsKey(serie + "_" + n)) n++;
        return n;
    }

    /** Todos los dibujos, por nombre (para subirlos a la tarjeta gráfica). */
    public static Map<String, Imagen> todas() {
        return imagenes;
    }

    /** Guarda un dibujo fabricado después (por ejemplo, un muro con su antorcha). */
    public static void guardar(String nombre, Imagen im) {
        imagenes.put(nombre, im);
    }
}
