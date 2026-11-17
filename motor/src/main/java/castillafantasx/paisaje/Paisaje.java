package castillafantasx.paisaje;

import java.util.HashMap;
import java.util.Map;

import castillafantasx.arte.Colores;
import castillafantasx.arte.Imagen;
import castillafantasx.combate.CifrasDeSalto;
import castillafantasx.superficie.MapaSuperficie;

/**
 * PAISAJE — El suelo de la superficie con formas naturales
 * (docs/encargos_viejos/ENCARGO_PAISAJE.md). El mapa sigue en casillas; aquí se pinta, a
 * trozos de 256 píxeles y solo cuando hacen falta, como un cuadro continuo:
 *
 *   Terreno  lo que se sabe de cada casilla (clase, altura, agua cerca…);
 *   Formas   qué hay en cada punto, con bordes redondos y torcidos;
 *   Pintor   de qué color es, de día y de noche;
 *   Horno    lo pinta en un Trozo, con su relieve, su luz y su sombra.
 *
 * No usa la tarjeta gráfica: el juego sube cada Trozo a una textura
 * (interfaz.DibujoPaisaje) y las pruebas sin ventana lo usan tal cual.
 */
public final class Paisaje {

    private static final int TAM = MapaSuperficie.TAM, LADO = Trozo.LADO;
    /** Píxeles que puede salirse un terreno en alto de sus casillas, por lo torcido de su borde. */
    private static final float MARGEN = 12f;
    private static final int TROZOS_GUARDADOS = 48;        // los que guardan las pruebas sin ventana

    public final Terreno terreno;
    /** Qué árbol y qué mata crece en cada casilla. */
    public final Flora flora;
    /** Lo que se mueve y se tiñe cada casa, y lo que se tuerce cada cerca. */
    public final Pueblo pueblo;
    /** Qué edificio es cada mancha de casillas de casa, y qué muros llevan almenas. */
    public final Edificios edificios;
    private final Horno horno, hornoDelAyudante;
    private final Map<Long, Trozo> guardados = new HashMap<>();
    private final Formas formas;            // para preguntar qué hay en un punto (las pruebas y la depuración)
    private final Punto punto = new Punto();

    public Paisaje(MapaSuperficie mapa) {
        terreno = new Terreno(mapa);
        flora = new Flora(terreno);
        pueblo = new Pueblo(mapa);
        edificios = new Edificios(mapa);
        horno = new Horno(terreno);
        hornoDelAyudante = new Horno(terreno);
        formas = new Formas(terreno);
    }

    /**
     * Pinta un trozo, si nadie lo ha pintado ni lo está pintando. Lo llama
     * el juego; 'ayudante' es true cuando lo llama su ayudante, que trabaja
     * en otro hilo mientras se juega: cada uno tiene su horno, para no estorbarse.
     */
    public void hornear(Trozo trozo, boolean ayudante) {
        if (!trozo.estado.compareAndSet(Trozo.PENDIENTE, Trozo.PINTANDOSE)) return;
        (ayudante ? hornoDelAyudante : horno).hornear(trozo);
        trozo.estado.set(Trozo.LISTO);
    }

    // ------------------------------------------------------------------
    // Lo que está en alto: se pinta dos veces, la segunda en su turno (fila a fila), para tapar a quien pasa por detrás
    // ------------------------------------------------------------------

    /** ¿Levanta del llano esa casilla? (Está en alto, o es una peña.) */
    public boolean enAlto(int c, int f) {
        return terreno.mapa.alturaDe(c, f) > 0 || terreno.clase(c, f) == Terreno.ROCA;
    }

    /**
     * La marca que llevan en un Trozo los píxeles del terreno en alto de una
     * fila de casillas (de 1 a 255; el llano lleva 0). Se repite cada 255
     * filas: de sobra, porque nunca se ven tantas a la vez.
     */
    public static int marcaDeFila(int fila) {
        return Math.floorMod(fila, 255) + 1;
    }

    /**
     * La franja de pantalla {x0, y0, x1, y1} (en píxeles del mapa) donde
     * cae el terreno en alto de la fila 'f' entre las columnas c0 y c1.
     * Devuelve false si en ese tramo de la fila no hay nada en alto.
     */
    public boolean franja(int f, int c0, int c1, float[] r) {
        float masAlto = -1;
        int primera = 0, ultima = 0;
        for (int c = c0; c <= c1; c++) {
            if (!enAlto(c, f)) continue;
            if (masAlto < 0) primera = c;
            ultima = c;
            float alto = terreno.altura(c, f) + CifrasDeSalto.ESCALON;
            if (terreno.clase(c, f) == Terreno.ROCA) alto += Formas.alzadoMaximoDeRoca(terreno.dentroDeRoca(c, f));
            masAlto = Math.max(masAlto, alto);
        }
        if (masAlto < 0) return false;
        r[0] = primera * TAM - MARGEN;
        r[1] = f * TAM - masAlto - MARGEN;
        r[2] = (ultima + 1) * TAM + MARGEN;
        r[3] = (f + 1) * TAM + MARGEN + Formas.HONDO_DE_GRIETA;
        return true;
    }

    /** Lo que hay que subir o bajar el dibujo de quien pisa (x, y) para que vaya por la pendiente de una cuesta. */
    public float ajusteDeCuesta(float x, float y) {
        return terreno.ajusteDeCuesta(x, y);
    }

    // ------------------------------------------------------------------
    // Sin tarjeta gráfica (las pruebas y las láminas)
    // ------------------------------------------------------------------

    /** El trozo (columna, fila), ya pintado. Se guardan unos cuantos para no repetirlos. */
    public Trozo trozo(int columna, int fila) {
        long clave = ((long) columna << 32) ^ (fila & 0xFFFFFFFFL);
        Trozo t = guardados.get(clave);
        if (t == null) {
            if (guardados.size() >= TROZOS_GUARDADOS) guardados.clear();
            t = new Trozo();
            t.empezar(columna, fila);
            hornear(t, false);
            guardados.put(clave, t);
        }
        return t;
    }

    /** La clase de suelo (Terreno.HIERBA…) que se pinta en un punto del suelo del mapa, ya con su forma natural. */
    public int claseEn(int x, int y) {
        formas.leer(x, y, punto);
        return punto.clase;
    }

    /** La altura a la que se pinta el suelo en un punto del mapa, en píxeles. */
    public float alturaEn(int x, int y) {
        formas.leer(x, y, punto);
        return punto.altura;
    }

    /** El color (0xRRGGBB) de un píxel del paisaje, a esa hora (ver 'pintar'). */
    public int color(int x, int y, float sol, float claridad) {
        Trozo t = trozo(Math.floorDiv(x, LADO), Math.floorDiv(y, LADO));
        int i = Math.floorMod(y, LADO) * LADO + Math.floorMod(x, LADO);
        return Colores.mezcla(t.noche.px[i], Colores.mezcla(t.manana.px[i], t.tarde.px[i], (sol + 1) / 2), claridad) & 0xFFFFFF;
    }

    /** La marca de un píxel del paisaje: 0 si es llano; si es terreno en alto, la de su fila de casillas (marcaDeFila). */
    public int marcaEn(int x, int y) {
        Trozo t = trozo(Math.floorDiv(x, LADO), Math.floorDiv(y, LADO));
        return t.noche.px[Math.floorMod(y, LADO) * LADO + Math.floorMod(x, LADO)] >>> 24;
    }

    /**
     * Pinta en 'im' (cuya esquina está en (camX, camY) del mapa) el
     * rectángulo {x0, y0, x1, y1} del paisaje, a una hora: 'sol' va de -1
     * (acaba de salir) a 1 (se pone) y 'claridad', de 0 (noche) a 1 (pleno
     * día). 'filaEnAlto': -1 = todo el suelo; si no, solo el terreno en alto de esa fila de casillas.
     */
    public void pintar(Imagen im, int camX, int camY, float[] rect, float sol, float claridad, int filaEnAlto) {
        int marca = filaEnAlto < 0 ? 0 : marcaDeFila(filaEnAlto);
        int x0 = Math.max(camX, (int) rect[0]), y0 = Math.max(camY, (int) rect[1]);
        int x1 = Math.min(camX + im.ancho, (int) rect[2]), y1 = Math.min(camY + im.alto, (int) rect[3]);
        // Trozo a trozo: de cada uno, la parte que cae dentro del rectángulo.
        for (int fila = Math.floorDiv(y0, LADO); fila <= Math.floorDiv(y1 - 1, LADO); fila++) {
            for (int columna = Math.floorDiv(x0, LADO); columna <= Math.floorDiv(x1 - 1, LADO); columna++) {
                Trozo t = trozo(columna, fila);
                for (int y = Math.max(y0, fila * LADO); y < Math.min(y1, (fila + 1) * LADO); y++) {
                    for (int x = Math.max(x0, columna * LADO); x < Math.min(x1, (columna + 1) * LADO); x++) {
                        int i = (y - fila * LADO) * LADO + (x - columna * LADO);
                        if (marca != 0 && (t.noche.px[i] >>> 24) != marca) continue;
                        int deDia = Colores.mezcla(t.manana.px[i], t.tarde.px[i], (sol + 1) / 2);
                        im.px[(y - camY) * im.ancho + (x - camX)] = 0xFF000000 | Colores.mezcla(t.noche.px[i], deDia, claridad);
                    }
                }
            }
        }
    }
}
