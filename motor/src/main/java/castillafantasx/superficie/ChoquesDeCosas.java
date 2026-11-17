package castillafantasx.superficie;

import castillafantasx.datos.Ajustes;
import castillafantasx.datos.LectorSecciones;

/**
 * CHOQUES DE COSAS — Con qué choca lo fijo del mapa (res/datos/choques.txt, secciones [cosas], [saltables] y
 * [muros_altos]): la base de cada cosa por su letra, lo que levanta del suelo la que se salta y lo alto que no
 * se salta nunca. Lo de los seres, en el juego.
 */
public final class ChoquesDeCosas {

    private ChoquesDeCosas() { }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/choques.txt");

    /** El alto de lo que no se puede saltar. */
    public static final float NO_SE_SALTA = 100000f;

    /** Los números de "clave = 1 2" de una sección, o null si la clave no está. */
    static float[] numeros(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) return null;
        String[] partes = texto.trim().split("\\s+");
        float[] n = new float[partes.length];
        for (int i = 0; i < partes.length; i++) n[i] = Float.parseFloat(partes[i]);
        return n;
    }

    /** Lo que levanta del suelo una cosa del mapa que se puede saltar; si no se salta, un alto que nadie alcanza. */
    public static float altoDeCosa(char letra) {
        Float cambiado = CAMBIADOS.get(letra);
        if (cambiado != null) return cambiado;
        float[] n = numeros("saltables", String.valueOf(letra));
        return n != null ? n[0] : NO_SE_SALTA;
    }

    /** Lo que el editor ha cambiado de [saltables] desde que arrancó (lo escribe también en el archivo). */
    private static final java.util.Map<Character, Float> CAMBIADOS = new java.util.HashMap<>();

    /** Cambia, en caliente, lo que levanta una cosa (NO_SE_SALTA: deja de saltarse). */
    public static void cambiarAlto(char letra, float alto) {
        CAMBIADOS.put(letra, alto);
    }

    /** Las letras de lo alto que no se salta ni se pasa volando (sección [muros_altos]): tapias, murallas, paredes, roca… */
    public static String murosAltos() {
        StringBuilder letras = new StringBuilder();
        for (String linea : ARCHIVO.lineas("muros_altos")) {
            String clave = linea.contains("=") ? linea.substring(0, linea.indexOf('=')).trim() : "";
            if (clave.length() == 1) letras.append(clave);
        }
        return letras.toString();
    }

    /** La base {ancho, fondo} de una cosa fija del mapa, por su letra; null si no estorba. */
    public static float[] baseDeCosa(char letra) {
        // Se pide para cada casilla del mapa al poblarlo: leída una vez por letra (si no hay cifras cambiadas en Ajustes).
        if (letra >= BASES.length || Ajustes.hayCambios()) return numeros("cosas", String.valueOf(letra));
        if (!BASE_LEIDA[letra]) {
            BASES[letra] = numeros("cosas", String.valueOf(letra));
            BASE_LEIDA[letra] = true;
        }
        return BASES[letra];
    }

    // La base de cada letra ya leída (solo se lee: nadie la cambia).
    private static final float[][] BASES = new float[256][];
    private static final boolean[] BASE_LEIDA = new boolean[256];

    /** ¿Se pisan dos cajas? Cada una es {x0, y0, x1, y1}. */
    public static boolean sePisan(float[] a, float x0, float y0, float x1, float y1) {
        return a[0] < x1 && a[2] > x0 && a[1] < y1 && a[3] > y0;
    }
}
