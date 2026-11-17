package castillafantasx.rostros;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import castillafantasx.datos.LectorSecciones;

/**
 * ROSTROS — Todos los rostros del juego y cuál lleva cada personaje. Se leen
 * de res/datos/rostros.txt: una sección por rostro y la sección [personajes],
 * que dice qué rostro lleva cada aspecto del Vestuario.
 */
public final class Rostros {

    private Rostros() { }

    private static final Map<String, Rostro> TODOS = new LinkedHashMap<>();
    private static final Map<String, String> DE_CADA_PERSONAJE = new LinkedHashMap<>();
    private static final String PERSONAJES = "personajes";

    static {
        LectorSecciones archivo = new LectorSecciones("/datos/rostros.txt");
        for (String seccion : archivo.secciones()) {
            Map<String, String> datos = new LinkedHashMap<>();
            for (String linea : archivo.lineas(seccion)) {
                int igual = linea.indexOf('=');
                if (igual > 0) datos.put(linea.substring(0, igual).trim(), linea.substring(igual + 1).trim());
            }
            if (seccion.equals(PERSONAJES)) {
                DE_CADA_PERSONAJE.putAll(datos);
            } else {
                Rostro r = new Rostro(seccion);
                r.leer(datos);
                TODOS.put(seccion, r);
            }
        }
        for (Map.Entry<String, String> p : DE_CADA_PERSONAJE.entrySet()) {
            if (!TODOS.containsKey(p.getValue())) {
                throw new IllegalStateException("rostros.txt: «" + p.getKey() + "» pide el rostro «" + p.getValue() + "», que no existe");
            }
        }
    }

    /** El rostro con ese nombre, o null. */
    public static Rostro rostro(String nombre) {
        return TODOS.get(nombre);
    }

    /**
     * El rostro de un personaje, por su aspecto del Vestuario ("heroe",
     * "vieja"…). Quien lleva otro equipo ("soldado+arma+…") tiene la cara de
     * siempre. Si nadie le ha puesto rostro, null: se le pinta uno corriente.
     */
    public static Rostro de(String aspecto) {
        int mas = aspecto.indexOf('+');
        String nombre = DE_CADA_PERSONAJE.get(mas < 0 ? aspecto : aspecto.substring(0, mas));
        return nombre == null ? null : TODOS.get(nombre);
    }

    /** Los nombres de todos los rostros, en el orden del archivo. */
    public static List<String> nombres() {
        return new ArrayList<>(TODOS.keySet());
    }

    /** Los rostros de la cantera: los que aún no lleva ningún personaje. */
    public static List<String> cantera() {
        List<String> libres = new ArrayList<>(TODOS.keySet());
        libres.removeAll(DE_CADA_PERSONAJE.values());
        return libres;
    }

    /** Qué personaje lleva ese rostro, o null si es de la cantera. */
    public static String personajeDe(String rostro) {
        for (Map.Entry<String, String> p : DE_CADA_PERSONAJE.entrySet()) {
            if (p.getValue().equals(rostro)) return p.getKey();
        }
        return null;
    }
}
