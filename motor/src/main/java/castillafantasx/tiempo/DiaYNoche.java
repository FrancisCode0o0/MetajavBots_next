package castillafantasx.tiempo;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.datos.LectorSecciones;
import castillafantasx.seres.Ficha;

/**
 * DÍA Y NOCHE — Las cifras del reloj, del cielo, de quién anda a cada hora y
 * de las mazmorras a oscuras, leídas una vez de res/datos/dia_noche.txt (ahí
 * se explica cada una). Todas PROVISIONALES.
 */
// TODO-FRAN: todo esto se ajusta en res/datos/dia_noche.txt, sin tocar el código.
public final class DiaYNoche {

    private DiaYNoche() { }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/dia_noche.txt");

    // --- El reloj ---
    public static final double SEGUNDOS_POR_DIA = numero("reloj", "segundos_por_dia");
    public static final double HORA_INICIAL = numero("reloj", "hora_inicial");
    /** A qué hora empieza cada parte del día, en el orden de ParteDelDia. */
    private static final double[] EMPIEZA = {
        numero("reloj", "alba"), numero("reloj", "dia"), numero("reloj", "crepusculo"), numero("reloj", "noche") };

    // --- El cielo ---
    public static final int COLOR_DEL_ALBA = color("cielo", "alba");
    public static final int COLOR_DEL_DIA = color("cielo", "dia");
    public static final int COLOR_DEL_CREPUSCULO = color("cielo", "crepusculo");
    public static final float LUCES_DE_DIA = (float) numero("cielo", "luces_de_dia");
    public static final float SOMBRA_DE_DIA = (float) numero("cielo", "sombra_de_dia");

    // --- La gente ---
    public static final float CASILLAS_HASTA_CASA = (float) numero("gente", "casillas_hasta_casa");
    public static final float GENTE_CON_ANTORCHA = (float) numero("gente", "con_antorcha");
    public static final float RADIO_DEL_FAROL_DE_LA_GENTE = (float) numero("gente", "farol_radio");
    public static final float RADIO_DE_LA_ANTORCHA_DE_LA_GENTE = (float) numero("gente", "antorcha_radio");

    // --- Las mazmorras a oscuras ---
    public static final float CLARIDAD_A_OSCURAS = (float) numero("a_oscuras", "claridad");
    public static final double SEGUNDOS_DE_ACECHO = numero("a_oscuras", "segundos_de_acecho");
    public static final double SEGUNDOS_DE_ACOMETIDA = numero("a_oscuras", "segundos_de_acometida");

    /** Unas criaturas que salen con una parte del día: cuántas, de qué especie y en qué lugar. */
    public static final class Salida {
        public final ParteDelDia parte;
        public final String lugar;
        public final Ficha especie;
        public final int cuantas;

        Salida(ParteDelDia parte, String lugar, Ficha especie, int cuantas) {
            this.parte = parte;
            this.lugar = lugar;
            this.especie = especie;
            this.cuantas = cuantas;
        }
    }

    /** La hora a la que empieza una parte del día. */
    public static double empieza(ParteDelDia parte) {
        return EMPIEZA[parte.ordinal()];
    }

    /** Qué parte de la gente anda por la calle en esa parte del día (0 a 1). */
    public static float genteEnLaCalle(ParteDelDia parte) {
        return (float) numero("gente", parte.clave());
    }

    /** ¿Es de los que no se recogen en ninguna casa aunque la tengan cerca? (Los aspectos de «no_se_recogen».) */
    public static boolean noSeRecoge(String aspecto) {
        for (String uno : ARCHIVO.valor("gente", "no_se_recogen", "").split(",")) {
            if (uno.trim().equals(aspecto)) return true;
        }
        return false;
    }

    /** Cuánta luz del día llega a un lugar (0 a 1); a los que no están en la lista, toda. */
    public static float luzDeDiaEn(String lugar) {
        String texto = ARCHIVO.valor("luz_de_dia", lugar, null);
        return texto == null ? 1 : Float.parseFloat(texto);
    }

    /** ¿Es esa mazmorra una de las abandonadas, sin antorchas en los muros? */
    public static boolean aOscuras(String mazmorra) {
        for (String id : ARCHIVO.valor("a_oscuras", "mazmorras", "").split(",")) {
            if (id.trim().equals(mazmorra)) return true;
        }
        return false;
    }

    /** Las criaturas que salen con cada parte del día (sección [criaturas]). */
    public static List<Salida> salidas() {
        List<Salida> lista = new ArrayList<>();
        for (String linea : ARCHIVO.lineas("criaturas")) {
            String[] p = linea.split("\\|");
            ParteDelDia parte = p.length == 4 ? ParteDelDia.porClave(p[0].trim()) : null;
            Ficha especie = p.length == 4 ? Ficha.porId(p[2].trim()) : null;
            if (parte == null || especie == null || !especie.enSuperficie()) {
                throw new IllegalStateException("dia_noche.txt [criaturas]: no se entiende «" + linea.trim() + "»");
            }
            lista.add(new Salida(parte, p[1].trim(), especie, Integer.parseInt(p[3].trim())));
        }
        return lista;
    }

    /** Un número de una sección; si falta, el juego se para y dice cuál. */
    public static double numero(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) throw new IllegalStateException("dia_noche.txt: falta «" + clave + "» en [" + seccion + "]");
        return Double.parseDouble(texto);
    }

    private static int color(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) throw new IllegalStateException("dia_noche.txt: falta «" + clave + "» en [" + seccion + "]");
        return Integer.parseInt(texto, 16);
    }
}
