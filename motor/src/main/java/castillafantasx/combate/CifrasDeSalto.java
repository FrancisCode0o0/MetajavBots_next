package castillafantasx.combate;

import castillafantasx.datos.LectorSecciones;

/**
 * CIFRAS DE SALTO — Las cifras del salto, de la esquiva, del terreno con
 * desniveles y de la altura en el combate, leídas una vez de
 * res/datos/salto.txt (ahí se explica cada una). Todas PROVISIONALES.
 */
// TODO-FRAN: todo esto se ajusta en res/datos/salto.txt, sin tocar el código.
public final class CifrasDeSalto {

    private CifrasDeSalto() { }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/salto.txt");

    // --- El salto ---
    public static final double GRAVEDAD = numero("salto", "gravedad");
    public static final double ALTURA_PARADO = numero("salto", "altura_parado");
    public static final double ALTURA_CON_IMPULSO = numero("salto", "altura_con_impulso");
    public static final float AGUANTE_DEL_SALTO = (float) numero("salto", "aguante");
    public static final float CONTROL_EN_EL_AIRE = (float) numero("salto", "control_en_el_aire");
    public static final double SEGUNDOS_RECIBIENDO = numero("salto", "segundos_recibiendo");

    // --- La esquiva ---
    public static final double SEGUNDOS_DE_ESQUIVA = numero("esquiva", "segundos");
    public static final double VELOCIDAD_DE_ESQUIVA = numero("esquiva", "velocidad");
    public static final double SEGUNDOS_INTOCABLE = numero("esquiva", "segundos_intocable");
    public static final float AGUANTE_DE_LA_ESQUIVA = (float) numero("esquiva", "aguante");
    public static final double ESPERA_DE_LA_ESQUIVA = numero("esquiva", "espera");

    // --- El terreno ---
    public static final float ESCALON = (float) numero("terreno", "escalon");
    public static final float ALCANCE_AL_ENCARAMARSE = (float) numero("terreno", "alcance_al_encaramarse");
    public static final double SEGUNDOS_ENCARAMANDOSE = numero("terreno", "segundos_encaramandose");
    public static final float BAJADA_MAXIMA = (float) numero("terreno", "bajada_maxima");
    public static final int DANIO_AL_VACIO = (int) numero("terreno", "danio_al_vacio");
    public static final int DANIO_AL_AGUA = (int) numero("terreno", "danio_al_agua");
    public static final float AJUSTE_AL_CAER = (float) numero("terreno", "ajuste_al_caer");

    // --- La altura en el combate ---
    public static final double ALCANCE_ALTO = numero("combate", "alcance_alto");
    public static final double ALCANCE_BAJO = numero("combate", "alcance_bajo");
    public static final double ALTO_DEL_HEROE = numero("combate", "alto_del_heroe");
    public static final double DANIO_EN_SALTO = numero("combate", "danio_en_salto");
    public static final double ALCANCE_ALTO_EN_SALTO = numero("combate", "alcance_alto_en_salto");

    private static double numero(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) throw new IllegalStateException("salto.txt: falta «" + clave + "» en [" + seccion + "]");
        return Double.parseDouble(texto);
    }

    /** La velocidad hacia arriba con la que hay que despegar para subir esa altura. */
    public static double impulsoPara(double altura) {
        return Math.sqrt(2 * GRAVEDAD * altura);
    }

    /** Lo que dura en el aire un salto de esa altura que cae al mismo nivel del que salió. */
    public static double segundosEnElAire(double altura) {
        return 2 * impulsoPara(altura) / GRAVEDAD;
    }
}
