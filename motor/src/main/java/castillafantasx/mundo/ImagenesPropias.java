package castillafantasx.mundo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import castillafantasx.arte.Almacen;
import castillafantasx.arte.Imagen;
import castillafantasx.arte.Paleta;
import castillafantasx.datos.EscritorSecciones;

/**
 * IMÁGENES PROPIAS — Las imágenes de Fran (PNG o GIF, quietas o animadas) que
 * el editor pega en el mundo como pinturas, carteles, retablos o pintadas.
 * Los originales se dejan en arte/imagenes/; al pegar una, se pasa al pixel
 * art del juego (se achica y se lleva a los colores de la Paleta) y se guarda
 * ya hecha en res/mundo/imagenes/, de donde el juego la lee al arrancar.
 */
public final class ImagenesPropias {

    private ImagenesPropias() { }

    /** Con qué empieza su nombre en el Almacen: "propias/retablo_0" es el primer fotograma de «retablo». */
    public static final String PREFIJO = "propias/";
    /** Donde Fran deja sus imágenes para pegarlas. */
    public static File originales = new File("arte/imagenes");
    /** Donde quedan, ya pasadas a pixel art, con su índice (las pruebas ponen otra carpeta). */
    public static Path carpeta = Path.of("res/mundo/imagenes");
    private static final String INDICE = "indice.txt", RECURSO = "/mundo/imagenes/";

    // TODO-FRAN: lo más que mide (en píxeles del juego) una imagen pegada, cuántos fotogramas se cogen de un GIF y lo más deprisa que se mueve.
    public static final int LADO_DE_FABRICA = 48, LADO_MINIMO = 8, LADO_MAXIMO = 128;
    public static final int FOTOGRAMAS_MAXIMOS = 24, MS_MINIMOS = 60, MS_SI_NO_SE_DICE = 120;
    /** Lo transparente que tiene que ser un punto (de 0 a 255) para quedarse sin pintar: el pixel art no tiene medias tintas. */
    private static final int OPACO_DESDE = 128;

    /** De cada imagen, cuántos fotogramas tiene y cuántos milisegundos dura cada uno. */
    private static final Map<String, int[]> LEIDAS = new LinkedHashMap<>();

    /** Lee las imágenes ya hechas y las mete en el Almacen (lo llama el Almacen al arrancar, una vez). */
    public static void cargar(Map<String, Imagen> imagenes) {
        LEIDAS.clear();
        for (String linea : lineasDelIndice()) {
            String[] p = linea.trim().split("\\s+");
            if (p.length < 3 || linea.trim().startsWith("#")) continue;
            try {
                int fotogramas = Integer.parseInt(p[1]), ms = Integer.parseInt(p[2]);
                List<Imagen> leidos = new ArrayList<>();
                for (int k = 0; k < fotogramas; k++) {
                    Imagen im = leerPng(p[0] + "_" + k + ".png");
                    if (im != null) leidos.add(im);
                }
                if (leidos.size() != fotogramas) {
                    System.err.println("Imágenes propias: faltan fotogramas de «" + p[0] + "»; no se usa.");
                    continue;
                }
                for (int k = 0; k < fotogramas; k++) imagenes.put(PREFIJO + p[0] + "_" + k, leidos.get(k));
                LEIDAS.put(p[0], new int[] { fotogramas, Math.max(MS_MINIMOS, ms) });
            } catch (NumberFormatException e) {
                System.err.println("Imágenes propias: no se entiende «" + linea.trim() + "» del índice.");
            }
        }
    }

    /** Las líneas del índice: las del disco si está (lleva lo recién pegado); si no, las que van dentro del juego. */
    private static List<String> lineasDelIndice() {
        Path enDisco = carpeta.resolve(INDICE);
        if (Files.exists(enDisco)) return EscritorSecciones.leer(enDisco);
        List<String> lineas = new ArrayList<>();
        try (InputStream in = ImagenesPropias.class.getResourceAsStream(RECURSO + INDICE)) {
            if (in == null) return lineas;
            BufferedReader lector = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) lineas.add(linea);
        } catch (IOException e) {
            System.err.println("No se ha podido leer el índice de las imágenes propias: " + e.getMessage());
        }
        return lineas;
    }

    private static Imagen leerPng(String nombre) {
        try {
            Path enDisco = carpeta.resolve(nombre);
            BufferedImage bi;
            if (Files.exists(enDisco)) {
                bi = ImageIO.read(enDisco.toFile());
            } else {
                try (InputStream in = ImagenesPropias.class.getResourceAsStream(RECURSO + nombre)) {
                    bi = in == null ? null : ImageIO.read(in);
                }
            }
            return bi == null ? null : deBuffered(bi);
        } catch (IOException e) {
            return null;
        }
    }

    private static Imagen deBuffered(BufferedImage bi) {
        Imagen im = new Imagen(bi.getWidth(), bi.getHeight());
        bi.getRGB(0, 0, im.ancho, im.alto, im.px, 0, im.ancho);
        return im;
    }

    /** Los nombres de las imágenes que hay listas para colocar. */
    public static List<String> nombres() {
        return new ArrayList<>(LEIDAS.keySet());
    }

    public static boolean existe(String nombre) {
        return LEIDAS.containsKey(nombre);
    }

    /** ¿Se mueve? (Tiene más de un fotograma.) */
    public static boolean animada(String nombre) {
        return LEIDAS.containsKey(nombre) && LEIDAS.get(nombre)[0] > 1;
    }

    /** Los milisegundos que dura cada fotograma de esa imagen. */
    public static int msDe(String nombre) {
        return LEIDAS.containsKey(nombre) ? LEIDAS.get(nombre)[1] : MS_SI_NO_SE_DICE;
    }

    /** El dibujo del Almacen que toca ahora de esa imagen (el juego va a 60 frames por segundo); null si no existe. */
    public static String fotograma(String nombre, long frame) {
        int[] datos = LEIDAS.get(nombre);
        if (datos == null) return null;
        long ms = frame * 1000 / 60;
        return PREFIJO + nombre + "_" + (int) ((ms / datos[1]) % datos[0]);
    }

    /** Los PNG y GIF que hay en arte/imagenes/ para pegar, por orden alfabético. */
    public static List<File> porPegar() {
        List<File> lista = new ArrayList<>();
        File[] archivos = originales.listFiles();
        if (archivos == null) return lista;
        Arrays.sort(archivos);
        for (File f : archivos) {
            String n = f.getName().toLowerCase();
            if (f.isFile() && (n.endsWith(".png") || n.endsWith(".gif"))) lista.add(f);
        }
        return lista;
    }

    /** El nombre que tendrá en el juego la imagen de ese archivo: el suyo, en minúsculas y sin nada raro. */
    public static String nombreDe(File original) {
        String n = original.getName();
        n = n.substring(0, n.lastIndexOf('.')).toLowerCase().replaceAll("[^a-z0-9]+", "_");
        return n.isEmpty() ? "imagen" : n;
    }

    /**
     * Pasa una imagen de Fran al pixel art del juego, con 'lado' píxeles en
     * su lado mayor, la guarda en res/mundo/imagenes/ y la deja lista para
     * colocar. Devuelve su nombre, o null si no se ha podido leer.
     */
    public static String importar(File original, int lado) {
        List<BufferedImage> cuadros = new ArrayList<>();
        int ms = leerCuadros(original, cuadros);
        if (cuadros.isEmpty()) return null;
        String nombre = nombreDe(original);
        try {
            Files.createDirectories(carpeta);
            for (int k = 0; k < cuadros.size(); k++) {
                Imagen hecha = aPixelArt(deBuffered(cuadros.get(k)), lado);
                BufferedImage salida = new BufferedImage(hecha.ancho, hecha.alto, BufferedImage.TYPE_INT_ARGB);
                salida.setRGB(0, 0, hecha.ancho, hecha.alto, hecha.px, 0, hecha.ancho);
                castillafantasx.datos.Candado.vigilar(carpeta.resolve(nombre + "_" + k + ".png"));
                ImageIO.write(salida, "png", carpeta.resolve(nombre + "_" + k + ".png").toFile());
                EscritorSecciones.apuntarEscrito(carpeta.resolve(nombre + "_" + k + ".png"));
                Almacen.guardar(PREFIJO + nombre + "_" + k, hecha);
            }
        } catch (IOException e) {
            System.err.println("No se ha podido guardar la imagen «" + nombre + "»: " + e.getMessage());
            return null;
        }
        LEIDAS.put(nombre, new int[] { cuadros.size(), Math.max(MS_MINIMOS, ms) });
        apuntarEnElIndice(nombre, cuadros.size(), Math.max(MS_MINIMOS, ms));
        return nombre;
    }

    /** Deja en el índice la línea de esa imagen: «nombre fotogramas milisegundos». */
    private static void apuntarEnElIndice(String nombre, int fotogramas, int ms) {
        Path indice = carpeta.resolve(INDICE);
        List<String> lineas = EscritorSecciones.leer(indice);
        if (lineas.isEmpty()) {
            lineas.add("# Las imágenes propias pegadas con el editor (docs/EDITOR.md), ya pasadas a pixel art:");
            lineas.add("# <nombre> <cuántos fotogramas> <milisegundos de cada uno>. Sus dibujos: <nombre>_0.png, <nombre>_1.png…");
        }
        lineas.removeIf(l -> l.trim().split("\\s+")[0].equals(nombre));
        lineas.add(nombre + " " + fotogramas + " " + ms);
        EscritorSecciones.escribir(indice, lineas);
    }

    /** Lee los cuadros de un PNG (uno) o de un GIF (todos, ya compuestos) y devuelve los milisegundos de cada uno. */
    private static int leerCuadros(File original, List<BufferedImage> cuadros) {
        int ms = MS_SI_NO_SE_DICE;
        try (ImageInputStream entrada = ImageIO.createImageInputStream(original)) {
            if (entrada == null) return ms;
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) return ms;
            ImageReader lector = lectores.next();
            lector.setInput(entrada);
            int cuantos = lector.getNumImages(true);
            BufferedImage lienzo = null;
            for (int k = 0; k < cuantos && cuadros.size() < FOTOGRAMAS_MAXIMOS; k++) {
                BufferedImage cuadro = lector.read(k);
                if (lienzo == null) lienzo = new BufferedImage(cuadro.getWidth(), cuadro.getHeight(), BufferedImage.TYPE_INT_ARGB);
                // En un GIF cada cuadro puede ser solo un trozo: se pinta en su sitio sobre lo anterior.
                int x = 0, y = 0;
                boolean borrarDespues = false;
                try {
                    IIOMetadataNode raiz = (IIOMetadataNode) lector.getImageMetadata(k).getAsTree("javax_imageio_gif_image_1.0");
                    for (Node n = raiz.getFirstChild(); n != null; n = n.getNextSibling()) {
                        NamedNodeMap a = n.getAttributes();
                        if (n.getNodeName().equals("ImageDescriptor")) {
                            x = Integer.parseInt(a.getNamedItem("imageLeftPosition").getNodeValue());
                            y = Integer.parseInt(a.getNamedItem("imageTopPosition").getNodeValue());
                        } else if (n.getNodeName().equals("GraphicControlExtension")) {
                            int centesimas = Integer.parseInt(a.getNamedItem("delayTime").getNodeValue());
                            if (centesimas > 0) ms = centesimas * 10;
                            borrarDespues = a.getNamedItem("disposalMethod").getNodeValue().equals("restoreToBackgroundColor");
                        }
                    }
                } catch (RuntimeException e) {
                    // No es un GIF (o no dice nada): el cuadro va entero.
                }
                Graphics2D g = lienzo.createGraphics();
                g.drawImage(cuadro, x, y, null);
                g.dispose();
                BufferedImage copia = new BufferedImage(lienzo.getWidth(), lienzo.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D gc = copia.createGraphics();
                gc.drawImage(lienzo, 0, 0, null);
                gc.dispose();
                cuadros.add(copia);
                if (borrarDespues) lienzo = new BufferedImage(lienzo.getWidth(), lienzo.getHeight(), BufferedImage.TYPE_INT_ARGB);
            }
            lector.dispose();
        } catch (IOException | RuntimeException e) {
            System.err.println("No se ha podido leer la imagen " + original + ": " + e.getMessage());
        }
        return ms;
    }

    /**
     * Una imagen cualquiera, al pixel art del juego: achicada hasta 'lado'
     * píxeles en su lado mayor (cada píxel nuevo, la media de los que tapa),
     * sin medias transparencias y con cada color llevado al más parecido de la Paleta.
     */
    public static Imagen aPixelArt(Imagen original, int lado) {
        int mayor = Math.max(original.ancho, original.alto);
        double escala = Math.min(1, (double) Math.max(LADO_MINIMO, Math.min(LADO_MAXIMO, lado)) / mayor);
        int ancho = Math.max(1, (int) Math.round(original.ancho * escala)), alto = Math.max(1, (int) Math.round(original.alto * escala));
        Imagen hecha = new Imagen(ancho, alto);
        double paso = 1 / escala;
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int color = media(original, x * paso, y * paso, paso);
                hecha.set(x, y, (color >>> 24) < OPACO_DESDE ? 0 : deLaPaleta(color));
            }
        }
        return hecha;
    }

    /** La media de color (y de transparencia) del cuadrado del original que empieza en (x, y) y mide 'lado'. */
    private static int media(Imagen im, double x, double y, double lado) {
        int xa = (int) x, ya = (int) y;
        int xb = Math.max(xa + 1, (int) Math.ceil(x + lado)), yb = Math.max(ya + 1, (int) Math.ceil(y + lado));
        double a = 0, r = 0, g = 0, b = 0;
        int n = 0;
        for (int j = ya; j < Math.min(yb, im.alto); j++) {
            for (int i = xa; i < Math.min(xb, im.ancho); i++) {
                int c = im.px[j * im.ancho + i];
                // El color pesa según lo opaco que sea, para que los bordes no se ensucien de negro.
                double alfa = (c >>> 24) / 255.0;
                a += alfa;
                r += ((c >> 16) & 0xFF) * alfa;
                g += ((c >> 8) & 0xFF) * alfa;
                b += (c & 0xFF) * alfa;
                n++;
            }
        }
        if (n == 0 || a == 0) return 0;
        return ((int) Math.round(a / n * 255) << 24) | ((int) Math.round(r / a) << 16) | ((int) Math.round(g / a) << 8) | (int) Math.round(b / a);
    }

    /** El color de la Paleta que más se parece a ese (opaco). */
    public static int deLaPaleta(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int mejor = 0;
        long cerca = Long.MAX_VALUE;
        for (char letra : Paleta.LETRAS.toCharArray()) {
            int c = Paleta.argb(letra);
            long dr = r - ((c >> 16) & 0xFF), dg = g - ((c >> 8) & 0xFF), db = b - (c & 0xFF);
            // El ojo nota más el verde que el rojo, y el rojo más que el azul.
            long d = 3 * dr * dr + 4 * dg * dg + 2 * db * db;
            if (d < cerca) {
                cerca = d;
                mejor = c;
            }
        }
        return mejor;
    }
}
