package castillafantasx.salud;

import castillafantasx.datos.LectorSecciones;

/**
 * CIFRAS DE LAS SEÑALES — Desde cuándo y cuánto avisa el cuerpo de Don
 * héroe (herido, agotado, a punto de caer), leídas una vez de
 * res/datos/senales.txt (ahí se explica cada una). Todas PROVISIONALES.
 */
// TODO-FRAN: todo esto se ajusta en res/datos/senales.txt, sin tocar el código.
public final class CifrasDeSenales {

    private CifrasDeSenales() { }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/senales.txt");

    // Los umbrales: la parte (0 a 1) de vida, aguante o firmeza desde la que empieza cada señal y con la que se nota del todo.
    public static final double HERIDA_DESDE = numero("herida", "desde"), HERIDA_DEL_TODO = numero("herida", "del_todo");
    public static final double AGONIA_DESDE = numero("agonia", "desde"), AGONIA_DEL_TODO = numero("agonia", "del_todo");
    public static final double FATIGA_DESDE = numero("fatiga", "desde"), FATIGA_DEL_TODO = numero("fatiga", "del_todo");
    public static final double TAMBALEO_DESDE = numero("tambaleo", "desde"), TAMBALEO_DEL_TODO = numero("tambaleo", "del_todo");
    /** Con esta fatiga o más, al pararse se apoya en las rodillas. */
    public static final double FATIGA_PARA_APOYARSE = numero("fatiga", "se_apoya");

    /** Segundos que tarda una señal en aparecer y segundos que tarda en calmarse. */
    public static final double SEGUNDOS_DE_SUBIR = numero("ritmo", "sube"), SEGUNDOS_DE_CALMARSE = numero("ritmo", "se_calma");
    /** Respiraciones y latidos por segundo: tranquilo y al límite. */
    public static final double RESPIRA = numero("ritmo", "respira"), JADEA = numero("ritmo", "jadea");
    public static final double LATE = numero("ritmo", "late"), LATE_AL_LIMITE = numero("ritmo", "late_al_limite");

    // Cuánto se nota en el cuerpo (tercera persona).
    public static final double ENCORVA = numero("cuerpo", "encorva"), COJEA = numero("cuerpo", "cojea"), JADEO = numero("cuerpo", "jadea");
    public static final double SE_TAMBALEA = numero("cuerpo", "se_tambalea"), PIES = numero("cuerpo", "pies");
    public static final boolean SANGRA = texto("cuerpo", "sangra").equals("si");

    // Cuánto se nota por sus ojos (primera persona).
    public static final double BORDE = numero("vista", "borde"), PULSO = numero("vista", "pulso"), TIEMBLA = numero("vista", "tiembla");
    public static final double SE_APAGA = numero("vista", "se_apaga"), SE_NUBLA = numero("vista", "se_nubla");
    public static final double VISTA_RESPIRA = numero("vista", "respira"), BRAZOS = numero("vista", "brazos"), SE_LADEA = numero("vista", "se_ladea");

    // Los avisos de sonido.
    public static final double JADEO_DESDE = numero("sonido", "jadeo_desde"), LATIDO_DESDE = numero("sonido", "latido_desde");

    private static String texto(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) throw new IllegalStateException("senales.txt: falta «" + clave + "» en [" + seccion + "]");
        return texto;
    }

    private static double numero(String seccion, String clave) {
        return Double.parseDouble(texto(seccion, clave));
    }
}
