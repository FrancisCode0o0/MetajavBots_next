package castillafantasx.figura;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import castillafantasx.datos.EscritorSecciones;
import castillafantasx.datos.LectorSecciones;

/**
 * RETOQUES — Lo que el editor de figuras le ha cambiado a cada figura del
 * juego, guardado en res/figuras/retoques.txt (la forma) y
 * res/figuras/poses.txt (las poses de cada gesto). Cada figura tiene su
 * clave: «especie:<id>», «persona:aldeano», «primera_persona».
 * Sin retoques (los dos archivos vacíos), todo se dibuja como siempre.
 */
public final class Retoques {

    private Retoques() { }

    /** Los archivos donde se guardan (las pruebas ponen otros). */
    public static Path archivo = Path.of("res/figuras/retoques.txt"), archivoDePoses = Path.of("res/figuras/poses.txt");

    /** La clave de los brazos que se ven en la mazmorra. */
    public static final String PRIMERA_PERSONA = "primera_persona";

    private static final Map<String, RetoqueDeFigura> FORMAS = new TreeMap<>(), POSES = new TreeMap<>();
    private static final Map<Figura, String> CLAVES = Collections.synchronizedMap(new IdentityHashMap<>());

    static {
        leerDelDisco();
    }

    /** Dice cuál es la clave de una figura (lo hacen Bestiario y Vestuario al crearla). */
    public static void nombrar(Figura f, String clave) {
        CLAVES.put(f, clave);
    }

    /** La clave de una figura: la que se le dio o la suya (Figura.claveDeRetoque: la de su aspecto, si es una persona); null si no tiene. */
    public static String claveDe(Figura f) {
        String clave = CLAVES.get(f);
        if (clave == null) {
            clave = f.claveDeRetoque();
            if (clave != null) CLAVES.put(f, clave);
        }
        return clave;
    }

    /** ¿Hay algo retocado? (Si no, el Escultor ni pregunta.) */
    public static boolean hay() {
        return !FORMAS.isEmpty() || !POSES.isEmpty();
    }

    /** El retoque de la forma de esa figura, o null si no tiene. */
    public static synchronized RetoqueDeFigura de(String clave) {
        return clave == null ? null : FORMAS.get(clave);
    }

    /** El retoque de la pose de esa figura en ese gesto, o null si no tiene. */
    public static synchronized RetoqueDeFigura dePose(String clave, Gesto gesto) {
        return clave == null || gesto == null || POSES.isEmpty() ? null : POSES.get(clave + "@" + gesto.name());
    }

    /** El retoque de la forma de esa figura para cambiarlo: si no tenía, uno vacío que ya vale (se ve al momento). Después, guardar(). */
    public static synchronized RetoqueDeFigura editar(String clave) {
        return FORMAS.computeIfAbsent(clave, k -> new RetoqueDeFigura());
    }

    /** Lo mismo con la pose de esa figura en ese gesto. Después, guardarPoses(). */
    public static synchronized RetoqueDeFigura editarPose(String clave, Gesto gesto) {
        return POSES.computeIfAbsent(clave + "@" + gesto.name(), k -> new RetoqueDeFigura());
    }

    /** Guarda las formas retocadas (las que no cambian nada se olvidan). */
    public static synchronized void guardar() {
        FORMAS.values().removeIf(RetoqueDeFigura::vacio);
        guardar(archivo, FORMAS, "Retoques de las figuras (editor: Figuras › Esculpir figuras). Una sección por figura.");
    }

    /** Guarda las poses retocadas: desde ese momento valen en el juego. */
    public static synchronized void guardarPoses() {
        POSES.values().removeIf(RetoqueDeFigura::vacio);
        guardar(archivoDePoses, POSES, "Poses retocadas (editor: Figuras › Poses). Una sección por figura y gesto: «clave@GESTO».");
    }

    /** Las claves que tienen la forma retocada. */
    public static synchronized List<String> claves() {
        return new ArrayList<>(FORMAS.keySet());
    }

    /** Las secciones («clave@GESTO») que tienen la pose retocada. */
    public static synchronized List<String> poses() {
        return new ArrayList<>(POSES.keySet());
    }

    /** La pose de una persona en ese momento, con lo que se le haya retocado a ese gesto sumado. */
    public static Pose posar(Figura h, Instante m, Pose p) {
        if (POSES.isEmpty()) return p;
        RetoqueDeFigura r = dePose(claveDe(h), m.gesto);
        if (r == null || r.pose.isEmpty()) return p;
        // Sobre una copia: la pose del Animador puede ser una de las suyas de siempre.
        Pose copia = Pose.mezcla(p, p, 0);
        copia.sumar(r.pose);
        return copia;
    }

    /** Lee de nuevo los dos archivos (al arrancar, y cuando cambian con el juego abierto). */
    public static synchronized void leerDelDisco() {
        leer(archivo, FORMAS);
        leer(archivoDePoses, POSES);
    }

    private static void leer(Path de, Map<String, RetoqueDeFigura> en) {
        en.clear();
        if (!Files.exists(de)) return;
        try {
            LectorSecciones lector = new LectorSecciones(de);
            for (String seccion : lector.secciones()) {
                RetoqueDeFigura r = new RetoqueDeFigura();
                for (String linea : lector.lineas(seccion)) r.leerLinea(linea);
                if (!r.vacio()) en.put(seccion, r);
            }
        } catch (RuntimeException e) {
            // Un archivo estropeado no tumba el juego: las figuras salen como siempre.
            System.err.println("No se han podido leer los retoques de " + de + ": " + e.getMessage());
        }
    }

    private static void guardar(Path en, Map<String, RetoqueDeFigura> que, String cabecera) {
        List<String> lineas = new ArrayList<>();
        // En los comentarios no van corchetes ni almohadillas sueltas: el lector los tomaría por secciones.
        lineas.add("# " + cabecera);
        lineas.add("# todo, «parte N» y «pieza N»: grosor y largo (veces), mover x y z (píxeles), gira, alza y ladea (grados), material NOMBRE, oculta.");
        lineas.add("# «tinte RRGGBB = MATERIAL»: todo lo de ese color pasa a ser de ese material. Materiales: los de figura/Material o «acabado:RRGGBB».");
        lineas.add("# «pose manoD = x y z» (solo personas): lo que se suma a su pose en ese gesto.");
        for (Map.Entry<String, RetoqueDeFigura> e : que.entrySet()) {
            lineas.add("");
            lineas.add("[" + e.getKey() + "]");
            List<String> suyas = e.getValue().aLineas();
            lineas.addAll(suyas);
            // Lo que queda en la memoria es justo lo escrito (con sus decimales): al volver a arrancar sale idéntico.
            RetoqueDeFigura escrito = new RetoqueDeFigura();
            for (String linea : suyas) escrito.leerLinea(linea);
            e.getValue().tomar(escrito);
        }
        EscritorSecciones.escribir(en, lineas);
    }
}
