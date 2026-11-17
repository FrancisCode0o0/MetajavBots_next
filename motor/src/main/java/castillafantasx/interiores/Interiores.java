package castillafantasx.interiores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import castillafantasx.datos.LectorSecciones;

/**
 * INTERIORES — El registro de los interiores del juego (res/datos/interiores.txt,
 * docs/INTERIORES.md): qué interior hay detrás de cada puerta, cuáles se
 * alumbran de noche y a qué horas, quién vive en cada casa y cómo es la boca
 * de cada mazmorra. Cada interior es un mapa más de la superficie,
 * res/mundo/interiores/<nombre>.txt, que se carga la primera vez que se entra.
 */
public final class Interiores {

    private Interiores() { }

    /** Con qué empieza el id del mapa de un interior: «interiores/iglesia» es res/mundo/interiores/iglesia.txt. */
    public static final String CARPETA = "interiores/";

    /** Una puerta de un mapa al aire libre: el mapa y la casilla de su «d». */
    public static final class Puerta {
        public final String mapa;
        public final int columna, fila;

        Puerta(String mapa, int columna, int fila) {
            this.mapa = mapa;
            this.columna = columna;
            this.fila = fila;
        }
    }

    private static final Map<String, Puerta> puertas = new LinkedHashMap<>();       // por el id del mapa del interior
    private static final List<String> todos = new ArrayList<>();
    private static final Map<String, double[]> lumbre = new LinkedHashMap<>();      // {desde, hasta} en horas; null = nunca
    private static final Map<String, String> textoDeLumbre = new LinkedHashMap<>();   // lo mismo, como está escrito ("19.5-22", "siempre")
    /** El archivo del registro, para escribir en él lo que cambia el editor. Las pruebas lo cambian por una copia. */
    public static java.nio.file.Path archivo = java.nio.file.Path.of("res/datos/interiores.txt");
    private static final Map<String, String> vecinos = new LinkedHashMap<>(), bocas = new LinkedHashMap<>();
    // Si no se dice nada de un interior, de noche está a oscuras. TODO-FRAN: qué casas se alumbran y cuándo (res/datos/interiores.txt, [lumbre]).
    private static final double[] SIEMPRE = { 0, 24 };
    private static boolean leido;
    /**
     * Opción: los comerciantes con tienda («dentro = …» en su archivo de res/comercio/) despachan dentro de ella.
     * Apagada, siguen en su sitio de la calle, como antes de este encargo (así pasan las pruebas antiguas).
     */
    public static boolean comerciantesDentro = true;

    /** Lee (o relee) res/datos/interiores.txt. */
    public static synchronized void leer() {
        puertas.clear();
        todos.clear();
        lumbre.clear();
        textoDeLumbre.clear();
        vecinos.clear();
        bocas.clear();
        leido = true;
        LectorSecciones archivo = new LectorSecciones("/datos/interiores.txt");
        // [puertas]: «<mapa> <columna> <fila> = <interior>».
        for (String linea : archivo.lineas("puertas")) {
            String[] lados = linea.split("=");
            String[] p = lados[0].trim().split("\\s+");
            if (lados.length != 2 || p.length != 3) throw new IllegalStateException("interiores.txt [puertas]: no se entiende «" + linea.trim() + "»");
            String id = CARPETA + lados[1].trim();
            puertas.put(id, new Puerta(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2])));
            todos.add(id);
        }
        // [sueltos]: un interior por línea, de los que no tienen puerta en un mapa (se llega desde otro interior o desde el editor).
        for (String linea : archivo.lineas("sueltos")) {
            todos.add(CARPETA + linea.trim());
        }
        // [lumbre]: «<interior> = siempre | nunca | <desde>-<hasta>» (horas; de 19.5 a 23, o de 21 a 2 si pasa de medianoche).
        for (String linea : archivo.lineas("lumbre")) {
            String[] lados = linea.split("=");
            lumbre.put(CARPETA + lados[0].trim(), horasDe(lados[1].trim()));
            textoDeLumbre.put(CARPETA + lados[0].trim(), lados[1].trim());
        }
        // [vecinos]: «<persona> = <interior>»: la persona, por su nudo de Ink o por su id del mapa.
        for (String linea : archivo.lineas("vecinos")) {
            String[] lados = linea.split("=");
            vecinos.put(lados[0].trim(), CARPETA + lados[1].trim());
        }
        // [bocas]: «<mazmorra> = <clase de umbral>»: cómo se ve la boca de cada mazmorra al entrar.
        for (String linea : archivo.lineas("bocas")) {
            String[] lados = linea.split("=");
            bocas.put(lados[0].trim(), lados[1].trim());
        }
    }

    /** Las horas de «siempre», «nunca» o «<desde>-<hasta>»: {desde, hasta}, o null si nunca. */
    private static double[] horasDe(String cuando) {
        if (cuando.equals("siempre")) return SIEMPRE;
        if (cuando.equals("nunca")) return null;
        String[] h = cuando.split("-");
        return new double[] { Double.parseDouble(h[0].trim()), Double.parseDouble(h[1].trim()) };
    }

    /** Cuándo se alumbra ese interior, como está escrito en el registro: «siempre», «nunca» o «19.5-22». */
    public static String lumbreDe(String interior) {
        asegurar();
        return textoDeLumbre.getOrDefault(interior, "nunca");
    }

    /** Cambia cuándo se alumbra un interior («siempre», «nunca» o «<desde>-<hasta>») y lo escribe en el registro (lo usa el editor). */
    public static void ponerLumbre(String interior, String cuando) {
        asegurar();
        lumbre.put(interior, horasDe(cuando));
        textoDeLumbre.put(interior, cuando);
        castillafantasx.datos.EscritorSecciones.ponerValor(archivo, "lumbre", nombreDe(interior), cuando, null);
    }

    private static void asegurar() {
        if (!leido) leer();
    }

    /** ¿Es el id del mapa de un interior? */
    public static boolean esInterior(String mapa) {
        return mapa != null && mapa.startsWith(CARPETA);
    }

    /** El nombre corto de un interior: «interiores/iglesia» → «iglesia». */
    public static String nombreDe(String mapa) {
        return esInterior(mapa) ? mapa.substring(CARPETA.length()) : mapa;
    }

    /** El id del mapa del interior que hay detrás de esa puerta, o null si esa puerta no lleva a ninguno. */
    public static String deLaPuerta(String mapa, int columna, int fila) {
        asegurar();
        for (Map.Entry<String, Puerta> e : puertas.entrySet()) {
            Puerta p = e.getValue();
            if (p.mapa.equals(mapa) && p.columna == columna && p.fila == fila) return e.getKey();
        }
        return null;
    }

    /** La puerta de la calle de un interior, o null si no la tiene (uno de la cantera, o uno al que se llega desde otro). */
    public static Puerta puertaDe(String interior) {
        asegurar();
        return puertas.get(interior);
    }

    /** Los ids de los mapas de todos los interiores, en el orden del archivo. */
    public static List<String> todos() {
        asegurar();
        return todos;
    }

    /** ¿Tiene ese interior sus velas, sus candiles y su hogar encendidos a esa hora (0 a 24)? */
    public static boolean alumbrado(String interior, double hora) {
        asegurar();
        double[] horas = lumbre.get(interior);
        if (horas == null) return false;
        if (horas[0] <= horas[1]) return hora >= horas[0] && hora < horas[1];
        return hora >= horas[0] || hora < horas[1];        // pasa de medianoche
    }

    /** El interior donde vive esa persona (por su nudo de Ink o su id), o null si no tiene casa propia. */
    public static String casaDe(String persona) {
        asegurar();
        return persona == null ? null : vecinos.get(persona);
    }

    /** Quién vive en ese interior: los nudos o ids de [vecinos]. */
    public static List<String> vecinosDe(String interior) {
        asegurar();
        List<String> suyos = new ArrayList<>();
        for (Map.Entry<String, String> e : vecinos.entrySet()) {
            if (e.getValue().equals(interior)) suyos.add(e.getKey());
        }
        return suyos;
    }

    /** La clase de umbral (la que dibuja el juego) de la boca de esa mazmorra; si no se dice, la de una cueva. */
    public static String bocaDe(String mazmorra) {
        asegurar();
        return bocas.getOrDefault(mazmorra, "cueva");
    }
}
