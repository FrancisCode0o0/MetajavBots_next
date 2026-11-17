package castillafantasx.editor.codigo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * COSA — Una cosa del mundo tal como la ve el buscador del código: su grupo
 * (persona, criatura, animal, objeto, casa, suelo, ambiente, evento, misión),
 * su nombre, los datos con que se buscan sus partes en el código ({id},
 * {ID}, {nudo}…) y dónde está en el mapa. La llena el juego; el motor no
 * sabe qué cosas hay.
 */
public final class Cosa {

    public final String grupo, id, nombre;
    /** Lo que se pone en las plantillas de res/editor/partes.txt: «id» → «lobo», «ID» → «LOBO». */
    public final Map<String, String> datos = new LinkedHashMap<>();
    /** Dónde está en el mapa ({x, y} en píxeles del mundo), una entrada por cada una que haya. */
    public final List<float[]> lugares = new ArrayList<>();
    /** Lo del juego que es esta cosa (un ser, un objeto…), para llevar a ella; puede quedar vacío. */
    public final List<Object> enElMundo = new ArrayList<>();

    public Cosa(String grupo, String id, String nombre) {
        this.grupo = grupo;
        this.id = id;
        this.nombre = nombre;
        datos.put("id", id);
        datos.put("ID", id.toUpperCase());
        datos.put("nombre", nombre);
    }

    /** Añade un dato para las plantillas (si es null, no se añade). Devuelve la cosa, para encadenar. */
    public Cosa con(String clave, String valor) {
        if (valor != null) datos.put(clave, valor);
        return this;
    }

    /** Añade un sitio del mapa donde está (y lo del juego que hay ahí). */
    public Cosa en(float x, float y, Object delJuego) {
        lugares.add(new float[] { x, y });
        if (delJuego != null) enElMundo.add(delJuego);
        return this;
    }

    /** La clave con que se la nombra en una sola palabra: «criatura.lobo». */
    public String clave() {
        return grupo + "." + id;
    }

    @Override
    public String toString() {
        return nombre + " (" + grupo + ")";
    }
}
