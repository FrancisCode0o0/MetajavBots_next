package castillafantasx.lumbre;

import castillafantasx.figura.Instante;
import castillafantasx.tiempo.DiaYNoche;

/**
 * LUMBRE — Las dos clases de lumbre del héroe
 * (docs/encargos_viejos/ENCARGO_DIA_NOCHE.md), con lo que alumbra cada una. Las cifras están
 * en res/datos/dia_noche.txt, sección [lumbre], y son PROVISIONALES.
 *
 * El FAROL de mano (una linterna de hoja de lata con ventanas de asta) lo
 * tiene desde el principio, alumbra poco y nunca se gasta. La ANTORCHA
 * alumbra mucho más, pero se consume hasta apagarse.
 */
// TODO-FRAN: cuánto alumbra cada lumbre y cuánto dura una antorcha (res/datos/dia_noche.txt).
public enum Lumbre {

    FAROL("farol", "Farol de mano", Instante.Lumbre.FAROL),
    ANTORCHA("antorcha", "Antorcha", Instante.Lumbre.ANTORCHA);

    /** El color cálido de la llama (RRGGBB). */
    public static final int COLOR = 0xFFB060;
    // Sin ninguna lumbre encendida apenas se ve lo que se tiene encima.
    public static final float RADIO_SIN_LUMBRE = numero("sin_lumbre_radio");
    public static final double ALCANCE_SIN_LUMBRE = numero("sin_lumbre_alcance"), FUERZA_SIN_LUMBRE = numero("sin_lumbre_fuerza");
    /** Una lumbre dejada en el suelo alumbra esta parte de lo que alumbra en la mano. */
    public static final float EN_EL_SUELO = numero("en_el_suelo");
    public static final float TEMBLOR = numero("temblor");
    public static final double SEGUNDOS_DE_ANTORCHA = numero("antorcha_segundos");
    public static final int ANTORCHAS_AL_EMPEZAR = (int) numero("antorchas_al_empezar");
    public static final int RAMAS_POR_ANTORCHA = (int) numero("ramas_por_antorcha");

    public final String id;             // el que se apunta en la partida guardada
    public final String nombre;         // el que se ve en pantalla
    public final Instante.Lumbre forma;  // cómo se dibuja en la mano
    public final float radio;           // superficie: radio del círculo de luz, en píxeles
    public final double alcance;        // mazmorra: casillas a las que llega la mitad de su luz
    public final double fuerza;         // mazmorra: lo viva que es la luz junto a la llama

    Lumbre(String id, String nombre, Instante.Lumbre forma) {
        this.id = id;
        this.nombre = nombre;
        this.forma = forma;
        this.radio = (float) DiaYNoche.numero("lumbre", id + "_radio");
        this.alcance = DiaYNoche.numero("lumbre", id + "_alcance");
        this.fuerza = DiaYNoche.numero("lumbre", id + "_fuerza");
    }

    private static float numero(String clave) {
        return (float) DiaYNoche.numero("lumbre", clave);
    }

    /**
     * Cuánto tiembla una llama en ese instante (alrededor de 1). 'tiempo' va
     * en segundos; 'semilla' hace que cada llama tiemble a su aire.
     */
    public static float temblor(double tiempo, double semilla) {
        double t = Math.sin(tiempo * 13 + semilla) * 0.5 + Math.sin(tiempo * 29 + semilla * 1.7) * 0.3 + Math.sin(tiempo * 47 + semilla * 0.3) * 0.2;
        return (float) (1 + TEMBLOR * t);
    }

    /** "antorcha" → ANTORCHA; null si no existe. */
    public static Lumbre porId(String id) {
        for (Lumbre l : values()) {
            if (l.id.equals(id)) return l;
        }
        return null;
    }
}
