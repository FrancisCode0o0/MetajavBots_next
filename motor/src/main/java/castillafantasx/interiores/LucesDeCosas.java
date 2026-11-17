package castillafantasx.interiores;

import java.util.HashMap;
import java.util.Map;

import castillafantasx.datos.LectorSecciones;

/**
 * LUCES DE LAS COSAS — Qué cosas de las que se colocan en un mapa dan luz,
 * de qué color, cuánta y cuándo (res/datos/luces.txt): el hogar, las velas y
 * los candiles de una casa, la claridad que entra por una ventana, el fuego
 * verde de un caldero… Valen en cualquier mapa de la superficie y, con su
 * alcance en casillas, en las mazmorras.
 */
public final class LucesDeCosas {

    private LucesDeCosas() { }

    /** Cuándo alumbra una cosa. */
    public enum Cuando {
        SIEMPRE,        // un fuego que no se apaga: el de un caldero alquímico, una reliquia que brilla
        LUMBRE,         // lo que alguien enciende: dentro de una casa, a las horas en que esa casa se alumbra; al aire libre, con la oscuridad
        DIA             // la claridad del día que entra por un hueco: una ventana, una puerta
    }

    /** La luz de una cosa. */
    public static final class Luz {
        public final int color;             // RRGGBB
        public final float radio;           // en píxeles de la superficie (una casilla mide 32)
        public final Cuando cuando;
        public final boolean tiembla;
        public final float alto;            // a cuántos píxeles sobre su pie está la luz

        Luz(int color, float radio, Cuando cuando, boolean tiembla, float alto) {
            this.color = color;
            this.radio = radio;
            this.cuando = cuando;
            this.tiembla = tiembla;
            this.alto = alto;
        }
    }

    private static Map<String, Luz> luces;

    /** Lee (o relee) res/datos/luces.txt, sección [cosas]: «<dibujo> = <color> <radio> <cuándo> [tiembla] [alto=<píxeles>]». */
    public static synchronized void leer() {
        Map<String, Luz> nuevas = new HashMap<>();
        for (String linea : new LectorSecciones("/datos/luces.txt").lineas("cosas")) {
            String[] lados = linea.split("=", 2);
            String[] p = lados[1].trim().split("\\s+");
            if (p.length < 3) throw new IllegalStateException("luces.txt [cosas]: no se entiende «" + linea.trim() + "»");
            boolean tiembla = false;
            float alto = 8;
            for (int i = 3; i < p.length; i++) {
                if (p[i].equals("tiembla")) tiembla = true;
                else if (p[i].startsWith("alto=")) alto = Float.parseFloat(p[i].substring(5));
            }
            nuevas.put(lados[0].trim(), new Luz(Integer.parseInt(p[0], 16), Float.parseFloat(p[1]), Cuando.valueOf(p[2].toUpperCase()), tiembla, alto));
        }
        luces = nuevas;
        Map<String, float[]> danios = new HashMap<>();
        Map<String, Float> velos = new HashMap<>();
        LectorSecciones archivo = new LectorSecciones("/datos/luces.txt");
        // [peligros]: «<dibujo> = <parte de la vida por segundo> <radio>».
        for (String linea : archivo.lineas("peligros")) {
            String[] lados = linea.split("=", 2);
            String[] p = lados[1].trim().split("\\s+");
            danios.put(lados[0].trim(), new float[] { Float.parseFloat(p[0]), Float.parseFloat(p[1]) });
        }
        // [nieblas]: «<dibujo> = <cuánto se aparta>».
        for (String linea : archivo.lineas("nieblas")) {
            String[] lados = linea.split("=", 2);
            velos.put(lados[0].trim(), Float.parseFloat(lados[1].trim()));
        }
        peligros = danios;
        nieblas = velos;
    }

    private static Map<String, float[]> peligros;
    private static Map<String, Float> nieblas;

    /** Lo que daña la cosa con ese dibujo a quien la pisa: {parte de la vida que quita por segundo, radio en píxeles}; null si no daña. */
    public static float[] peligroDe(String dibujo) {
        if (luces == null) leer();
        return peligros.get(dibujo);
    }

    /** Cuánto se aparta del héroe (píxeles) la niebla o el humo con ese dibujo; 0 si no lo es. */
    public static float nieblaDe(String dibujo) {
        if (luces == null) leer();
        Float cuanto = nieblas.get(dibujo);
        return cuanto == null ? 0 : cuanto;
    }

    /** La luz que da la cosa con ese dibujo ("cocina/hogar"), o null si no da ninguna. */
    public static Luz de(String dibujo) {
        if (luces == null) leer();
        return luces.get(dibujo);
    }

    /** Todas las cosas que dan luz, por su dibujo. */
    public static Map<String, Luz> todas() {
        if (luces == null) leer();
        return luces;
    }
}
