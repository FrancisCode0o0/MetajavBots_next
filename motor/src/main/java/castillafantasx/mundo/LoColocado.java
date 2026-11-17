package castillafantasx.mundo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import castillafantasx.datos.EscritorSecciones;
import castillafantasx.datos.LectorSecciones;

/**
 * LO COLOCADO — Todas las piezas puestas a mano en un mapa (su sección
 * [colocado]). Las lee del mapa al cargarlo y, cuando el editor añade, cambia
 * o quita una, lo escribe en el mismo archivo.
 */
public final class LoColocado {

    public static final String SECCION = "colocado";

    private static final String NOTA = "Lo puesto a mano con el editor (docs/EDITOR.md, «Construir y decorar»): una pieza por línea.\n"
            + "<clase> <x> <y> <qué> [giro=grados] [tam=1.5] [espejo] [capa=suelo|pie|encima] [solido] [variante=n] [cara=n|s|e|o altura=0.5] [| texto]\n"
            + "clase: cosa (un dibujo del juego), imagen (una propia, de res/mundo/imagenes/), mancha (sangre, barro, hollin, musgo, humedad),\n"
            + "trazo (color RRGGBB | sus puntos) o letrero (color RRGGBB | lo escrito). x, y: píxeles del mapa (en una mazmorra, casillas).";

    /** Las piezas, en el orden del archivo (las de después se pintan encima de las de antes de su misma capa). */
    public final List<Colocado> piezas = new ArrayList<>();
    /** El archivo del mapa en el que se escriben. */
    private final Path archivo;
    /** Cuenta los cambios: quien guarda algo calculado a partir de las piezas (la mazmorra, sus calcos) sabe así cuándo rehacerlo. */
    public int cambios;

    public LoColocado(LectorSecciones mapa, Path archivo) {
        this.archivo = archivo;
        for (String linea : mapa.lineas(SECCION)) {
            Colocado c = Colocado.deLinea(linea);
            if (c != null) piezas.add(c);
            else System.err.println(mapa.origen() + " [colocado]: no se entiende «" + linea.trim() + "»");
        }
    }

    /** Añade una pieza al mapa y la escribe en su archivo. */
    public void anadir(Colocado c) {
        piezas.add(c);
        cambios++;
        EscritorSecciones.anadirLinea(archivo, SECCION, c.aLinea(), NOTA);
    }

    /** Quita una pieza del mapa y de su archivo. */
    public void quitar(Colocado c) {
        piezas.remove(c);
        cambios++;
        EscritorSecciones.quitarLinea(archivo, SECCION, c.aLinea());
    }

    /** Añade varias piezas de una vez (una pincelada entera): el archivo se escribe una sola vez. */
    public void anadirVarias(List<Colocado> nuevas) {
        List<String> lineas = new ArrayList<>();
        for (Colocado c : nuevas) {
            if (!piezas.contains(c)) piezas.add(c);
            lineas.add(c.aLinea());
        }
        cambios++;
        EscritorSecciones.anadirLineas(archivo, SECCION, lineas, NOTA);
    }

    /** Quita varias piezas de una vez. */
    public void quitarVarias(List<Colocado> viejas) {
        List<String> lineas = new ArrayList<>();
        for (Colocado c : viejas) {
            piezas.remove(c);
            lineas.add(c.aLinea());
        }
        cambios++;
        EscritorSecciones.quitarLineas(archivo, SECCION, lineas);
    }

    /** Cambia una pieza (lo que haga 'cambio' con ella) y deja su línea al día en el archivo. */
    public void cambiar(Colocado c, Runnable cambio) {
        String vieja = c.aLinea();
        cambio.run();
        cambios++;
        if (!EscritorSecciones.cambiarLinea(archivo, SECCION, vieja, c.aLinea())) EscritorSecciones.anadirLinea(archivo, SECCION, c.aLinea(), NOTA);
    }

    /** Las piezas sólidas (las cosas de pie que estorban). */
    public List<Colocado> solidas() {
        List<Colocado> lista = new ArrayList<>();
        for (Colocado c : piezas) {
            if (c.clase == Colocado.Clase.COSA && castillafantasx.Contenido.delJuego().estorba(c)) lista.add(c);
        }
        return lista;
    }
}
