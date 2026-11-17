package castillafantasx.entrada;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;

/**
 * TECLAS — La tabla única de qué tecla hace qué (DECISIONES_CONTROLES §2).
 * Cada acción tiene las teclas que se quieran (también botones del ratón y la
 * rueda), y una tecla puede ser de varias acciones a la vez: entonces está
 * «compartida» y las hace todas. Se guarda en teclas.txt; lo que falte o
 * esté mal escrito se queda con la tecla de fábrica.
 */
public class Teclas {

    /** Dónde se guarda. Las pruebas lo cambian para no tocar el de Fran. */
    public static Path archivo = Path.of("teclas.txt");

    /** Sin tecla. */
    public static final int NINGUNA = -1;

    // El ratón y la rueda cuentan como teclas, con códigos por encima de los del teclado de libGDX (0–255).
    public static final int RATON = 1000;                           // RATON + el botón de libGDX
    public static final int RATON_IZQUIERDO = RATON + Buttons.LEFT;
    public static final int RATON_DERECHO = RATON + Buttons.RIGHT;
    public static final int RATON_CENTRAL = RATON + Buttons.MIDDLE;
    public static final int RATON_ATRAS = RATON + Buttons.BACK;
    public static final int RATON_ADELANTE = RATON + Buttons.FORWARD;
    public static final int RUEDA_ARRIBA = 1100, RUEDA_ABAJO = 1101;
    /** Un código más que el más alto: el tamaño de la tabla de Entrada. */
    public static final int CODIGOS = 1102;

    private final EnumMap<Accion, List<Integer>> tabla = new EnumMap<>(Accion.class);

    public Teclas() {
        restaurar();
    }

    /** Vuelve a las teclas de fábrica. */
    public void restaurar() {
        for (Accion a : Accion.values()) {
            tabla.put(a, deFabrica(a));
        }
    }

    /** ¿Están todas como de fábrica? */
    public boolean deFabrica() {
        for (Accion a : Accion.values()) {
            if (!deFabrica(a).equals(tabla.get(a))) return false;
        }
        return true;
    }

    private static List<Integer> deFabrica(Accion a) {
        List<Integer> lista = new ArrayList<>();
        for (int tecla : a.teclasPorDefecto()) lista.add(tecla);
        return lista;
    }

    /** ¿Esa tecla hace esa acción? */
    public boolean es(Accion accion, int tecla) {
        return tecla != NINGUNA && tabla.get(accion).contains(tecla);
    }

    /** Las teclas de una acción, en orden (una copia). */
    public List<Integer> teclasDe(Accion accion) {
        return new ArrayList<>(tabla.get(accion));
    }

    /** ¿Está mantenida alguna de las teclas de la acción? */
    public boolean mantenida(Accion accion, Entrada entrada) {
        for (int tecla : tabla.get(accion)) {
            if (entrada.mantenida(tecla)) return true;
        }
        return false;
    }

    /** ¿Se puede asignar esta tecla? Todas, menos Esc y las de depuración: F1 (el menú) y F2, F3 y F5 (la arena de pruebas). */
    public static boolean asignable(int tecla) {
        if (tecla == Keys.F1 || tecla == Keys.F2 || tecla == Keys.F3 || tecla == Keys.F5 || tecla == Keys.ESCAPE) return false;
        return tecla > 0 && tecla < CODIGOS;
    }

    /** Las otras acciones del mismo modo que ya usan esa tecla (vacía si ninguna). */
    public List<Accion> otrasQueLaUsan(Accion accion, int tecla) {
        List<Accion> otras = new ArrayList<>();
        for (Accion a : Accion.values()) {
            if (a != accion && a.modo == accion.modo && tabla.get(a).contains(tecla)) otras.add(a);
        }
        return otras;
    }

    /** ¿Esa tecla de esa acción la usa también otra acción del mismo modo? (El menú la marca.) */
    public boolean compartida(Accion accion, int tecla) {
        return !otrasQueLaUsan(accion, tecla).isEmpty();
    }

    /** Añade una tecla a una acción (si no la tenía ya). No toca las demás acciones. */
    public void anadir(Accion accion, int tecla) {
        if (!tabla.get(accion).contains(tecla)) tabla.get(accion).add(tecla);
    }

    /** Cambia la tecla número 'indice' de una acción por otra. No toca las demás acciones. */
    public void cambiar(Accion accion, int indice, int tecla) {
        List<Integer> lista = tabla.get(accion);
        if (lista.contains(tecla)) lista.remove(indice);       // ya la tenía en otro sitio: no se repite
        else lista.set(indice, tecla);
    }

    /** Quita la tecla número 'indice' de una acción. */
    public void quitar(Accion accion, int indice) {
        tabla.get(accion).remove(indice);
    }

    /** Quita esa tecla a todas las demás acciones del mismo modo (para que solo sea de 'accion'). */
    public void quitarDeLasDemas(Accion accion, int tecla) {
        for (Accion a : otrasQueLaUsan(accion, tecla)) {
            tabla.get(a).remove(Integer.valueOf(tecla));
        }
    }

    /** Las teclas de una acción, para enseñarlas: "W / Arriba" ("—" si no tiene). */
    public String nombres(Accion accion) {
        List<Integer> lista = tabla.get(accion);
        if (lista.isEmpty()) return nombreDe(NINGUNA);
        StringBuilder texto = new StringBuilder();
        for (int tecla : lista) {
            if (texto.length() > 0) texto.append(" / ");
            texto.append(nombreDe(tecla));
        }
        return texto.toString();
    }

    /** La primera tecla de una acción: la que nombran las pistas de la pantalla ("—" si no tiene). */
    public String primera(Accion accion) {
        List<Integer> lista = tabla.get(accion);
        return nombreDe(lista.isEmpty() ? NINGUNA : lista.get(0));
    }

    private static final int LARGO_MAXIMO_DE_PISTA = 18;

    /**
     * Las teclas que eligen cada arma (la primera de cada acción), resumidas
     * para una pista: "1-9" si van seguidas; si no, una a una ("1,2,Z,4"), o
     * la primera y la última si la lista sale muy larga. "—" si no hay ninguna.
     */
    public String resumen(Accion[] acciones) {
        List<String> nombres = new ArrayList<>();
        boolean seguidas = true;
        for (Accion a : acciones) {
            List<Integer> lista = tabla.get(a);
            if (lista.isEmpty()) {
                seguidas = false;
                continue;
            }
            String nombre = nombreDe(lista.get(0));
            // Seguidas: cada nombre es una sola letra o cifra, la siguiente a la anterior.
            if (nombre.length() != 1 || (!nombres.isEmpty() && nombre.charAt(0) != nombres.get(nombres.size() - 1).charAt(0) + 1)) seguidas = false;
            nombres.add(nombre);
        }
        if (nombres.isEmpty()) return nombreDe(NINGUNA);
        String primera = nombres.get(0), ultima = nombres.get(nombres.size() - 1);
        if (seguidas && nombres.size() > 1) return primera + "-" + ultima;
        String todas = String.join(",", nombres);
        return todas.length() <= LARGO_MAXIMO_DE_PISTA ? todas : primera + "…" + ultima;
    }

    /** La pista de las teclas de armas e inventario, la misma en los dos modos: "[Q/E] arma · [1-9] · [I] inventario". */
    public String pistaDeArmas(Accion anterior, Accion siguiente, Accion[] armas, Accion inventario) {
        return "[" + primera(anterior) + "/" + primera(siguiente) + "] arma · [" + resumen(armas) + "] · [" + primera(inventario) + "] inventario";
    }

    /** El nombre de una tecla en español. */
    public static String nombreDe(int tecla) {
        switch (tecla) {
            case NINGUNA:          return "—";
            case Keys.SPACE:       return "Espacio";
            case Keys.ENTER:       return "Intro";
            case Keys.UP:          return "Arriba";
            case Keys.DOWN:        return "Abajo";
            case Keys.LEFT:        return "Izquierda";
            case Keys.RIGHT:       return "Derecha";
            case Keys.SHIFT_LEFT:  return "Mayús";
            case Keys.SHIFT_RIGHT: return "Mayús der.";
            case Keys.CONTROL_LEFT:  return "Ctrl";
            case Keys.CONTROL_RIGHT: return "Ctrl der.";
            case Keys.TAB:         return "Tab";
            case Keys.BACKSPACE:   return "Retroceso";
            case Keys.FORWARD_DEL: return "Supr";
            case RATON_IZQUIERDO:  return "Clic izquierdo";
            case RATON_DERECHO:    return "Clic derecho";
            case RATON_CENTRAL:    return "Clic central";
            case RATON_ATRAS:      return "Ratón atrás";
            case RATON_ADELANTE:   return "Ratón adelante";
            case RUEDA_ARRIBA:     return "Rueda arriba";
            case RUEDA_ABAJO:      return "Rueda abajo";
            default:               return Keys.toString(tecla);
        }
    }

    // ------------------------------------------------------------------
    // teclas.txt: una línea por acción, "superficie.atacar = Space, MouseLeft"
    // (los nombres del teclado son los de libGDX, en inglés, para poder leerlos de vuelta).
    // ------------------------------------------------------------------

    // Cómo se escriben en teclas.txt el ratón y la rueda.
    private static final String VERSION = "version";        // la línea que dice de qué versión es el archivo
    // La línea que dice que el archivo ya conoce el clic derecho de «Acción» (encargo del ratón); sin ella, se le da.
    private static final String CON_CLIC_DERECHO = "accion_con_clic_derecho";
    private static final int[] RATON_Y_RUEDA = { RATON_IZQUIERDO, RATON_DERECHO, RATON_CENTRAL, RATON_ATRAS, RATON_ADELANTE, RUEDA_ARRIBA, RUEDA_ABAJO };
    private static final String[] NOMBRES_EN_EL_ARCHIVO = { "MouseLeft", "MouseRight", "MouseMiddle", "MouseBack", "MouseForward", "WheelUp", "WheelDown" };

    private static String enElArchivo(int tecla) {
        for (int i = 0; i < RATON_Y_RUEDA.length; i++) {
            if (RATON_Y_RUEDA[i] == tecla) return NOMBRES_EN_EL_ARCHIVO[i];
        }
        return Keys.toString(tecla);
    }

    private static int delArchivo(String nombre) {
        for (int i = 0; i < NOMBRES_EN_EL_ARCHIVO.length; i++) {
            if (NOMBRES_EN_EL_ARCHIVO[i].equals(nombre)) return RATON_Y_RUEDA[i];
        }
        return Keys.valueOf(nombre);
    }

    /** Escribe la tabla en teclas.txt. Si no se puede, el juego sigue. */
    public void guardar() {
        List<String> lineas = new ArrayList<>();
        lineas.add("# Teclas de " + castillafantasx.datos.ElJuego.nombre() + ". Se cambian desde el menú de teclas.");
        lineas.add("# Si borras este archivo, vuelven las de fábrica. Una acción sin nada detrás del '=' no tiene tecla.");
        lineas.add(VERSION + " = 2");
        lineas.add(CON_CLIC_DERECHO + " = si");
        for (Accion a : Accion.values()) {
            StringBuilder texto = new StringBuilder();
            for (int tecla : tabla.get(a)) {
                if (texto.length() > 0) texto.append(", ");
                texto.append(enElArchivo(tecla));
            }
            lineas.add(a.clave + " = " + texto);
        }
        try {
            castillafantasx.datos.Candado.vigilar(archivo);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar teclas.txt: " + e.getMessage());
        }
    }

    /**
     * Lee teclas.txt. Cada acción que esté en el archivo se queda con sus teclas
     * (aunque el archivo sea de otra versión del juego); la que no esté, con las
     * de fábrica. Las teclas mal escritas se saltan.
     */
    public void cargar() {
        restaurar();
        if (!Files.exists(archivo)) return;
        try {
            List<String> todas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            // Antes de la versión 2 el ratón y la rueda no se asignaban: hacían lo de fábrica, y así siguen.
            boolean antiguo = todas.stream().noneMatch(l -> l.trim().startsWith(VERSION));
            boolean sinClicDerecho = todas.stream().noneMatch(l -> l.trim().startsWith(CON_CLIC_DERECHO));
            for (String linea : todas) {
                linea = linea.trim();
                int igual = linea.indexOf('=');
                if (linea.startsWith("#") || igual < 0) continue;
                Accion accion = Accion.porClave(linea.substring(0, igual).trim());
                if (accion == null) continue;          // una acción que ya no existe
                List<Integer> lista = new ArrayList<>();
                for (String nombre : linea.substring(igual + 1).split(",")) {
                    if (nombre.trim().isEmpty()) continue;
                    int tecla = delArchivo(nombre.trim());
                    if (asignable(tecla) && !lista.contains(tecla)) lista.add(tecla);
                }
                if (antiguo) {
                    for (int tecla : accion.teclasPorDefecto()) {
                        if (tecla >= RATON && !lista.contains(tecla)) lista.add(tecla);
                    }
                }
                tabla.put(accion, lista);
            }
            // A un archivo de antes del encargo del ratón se le da el clic derecho en «Acción», si ninguna otra acción lo usa ya.
            if (sinClicDerecho) {
                for (Accion a : new Accion[] { Accion.SUP_ACCION, Accion.MAZ_ACCION }) {
                    if (otrasQueLaUsan(a, RATON_DERECHO).isEmpty()) anadir(a, RATON_DERECHO);
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("teclas.txt no se pudo leer; se usan las teclas de fábrica.");
            restaurar();
        }
    }
}
