package castillafantasx.agua;

import castillafantasx.datos.LectorSecciones;

/**
 * CIFRAS DEL AGUA — Lo hondo que es el agua, lo que cuesta nadar, lo torpe
 * que se lucha en ella y cuánto dura estar mojado, leídas una vez de
 * res/datos/agua.txt (ahí se explica cada una). Todas PROVISIONALES.
 */
// TODO-FRAN: todo esto se ajusta en res/datos/agua.txt, sin tocar el código.
public final class CifrasDeAgua {

    private CifrasDeAgua() { }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/agua.txt");

    // El fondo: honduras en píxeles del dibujo de la superficie.
    public static final float PENDIENTE = (float) numero("fondo", "pendiente"), HONDURA_MAXIMA = (float) numero("fondo", "maxima");
    public static final float TOBILLOS = (float) numero("fondo", "tobillos"), CINTURA = (float) numero("fondo", "cintura");
    /** Desde esta hondura no se hace pie: se nada. */
    public static final float PARA_NADAR = (float) numero("fondo", "para_nadar");
    public static final double CASILLA_DE_MAZMORRA = numero("fondo", "casilla_de_mazmorra");

    /** Quien no entra al agua: no admite ninguna hondura. */
    public static final float NO_ENTRA = 0;
    /** Quien se baña en la orilla: entra mientras haga pie. */
    public static final float HACE_PIE = PARA_NADAR;
    /** Quien nada: entra en cualquier agua. */
    public static final float NADA = Float.MAX_VALUE;

    /**
     * Cómo trata el agua una persona de ese aspecto (sección [gente] de
     * agua.txt: evita, orilla o nada): hasta qué hondura se mete. Quien no
     * está en la lista, la evita, como casi todo el mundo en el siglo XV.
     */
    // TODO-FRAN: quién se baña y quién sabe nadar, en res/datos/agua.txt [gente] (y, más adelante, desde el editor).
    public static float tratoDeLaGente(String aspecto) {
        // Se pregunta cada frame por cada uno: leído una vez por aspecto (si no hay cifras cambiadas en Ajustes).
        if (aspecto == null || castillafantasx.datos.Ajustes.hayCambios()) return trato(ARCHIVO.valor("gente", aspecto, "evita"));
        Float ya = TRATO_DE_LA_GENTE.get(aspecto);
        if (ya == null) TRATO_DE_LA_GENTE.put(aspecto, ya = trato(ARCHIVO.valor("gente", aspecto, "evita")));
        return ya;
    }

    private static final java.util.Map<String, Float> TRATO_DE_LA_GENTE = new java.util.HashMap<>();
    private static final java.util.Map<String, Float> TRATO_DE_LA_CRIATURA = new java.util.HashMap<>();

    /** Lo mismo para una criatura (sección [criaturas]): las que no están, no entran. */
    // TODO-FRAN: qué criaturas nadan o se meten en la orilla, en res/datos/agua.txt [criaturas].
    public static float tratoDeLaCriatura(castillafantasx.seres.Ficha especie) {
        if (especie == null) return NO_ENTRA;
        if (castillafantasx.datos.Ajustes.hayCambios()) return trato(ARCHIVO.valor("criaturas", especie.id(), "evita"));
        Float ya = TRATO_DE_LA_CRIATURA.get(especie.id());
        if (ya == null) TRATO_DE_LA_CRIATURA.put(especie.id(), ya = trato(ARCHIVO.valor("criaturas", especie.id(), "evita")));
        return ya;
    }

    /** ¿Dice agua.txt cómo trata el agua esa especie? (Todas deben estar en su sección [criaturas].) */
    public static boolean tieneTrato(castillafantasx.seres.Ficha especie) {
        return ARCHIVO.valor("criaturas", especie.id(), null) != null;
    }

    /** De la palabra de los datos (evita, orilla o nada) a la hondura hasta la que se mete. */
    public static float trato(String palabra) {
        switch (palabra.trim()) {
            case "nada": return NADA;
            case "orilla": return HACE_PIE;
            default: return NO_ENTRA;
        }
    }

    // Nadar.
    public static final float VELOCIDAD = (float) numero("nadar", "velocidad"), VELOCIDAD_VADEANDO = (float) numero("nadar", "velocidad_vadeando");
    public static final float VELOCIDAD_AGOTADO = (float) numero("nadar", "velocidad_agotado");
    public static final double AGUANTE_POR_SEGUNDO = numero("nadar", "aguante_por_segundo"), AGUANTE_A_FLOTE = numero("nadar", "aguante_a_flote");
    public static final double CANSADO_DESDE = numero("nadar", "cansado_desde");
    public static final double VIDA_SIN_AGUANTE = numero("nadar", "vida_sin_aguante");
    public static final boolean AHOGARSE_MATA = texto("nadar", "ahogarse_mata").equals("si");
    public static final double BRAZADAS_POR_SEGUNDO = numero("nadar", "brazadas_por_segundo");
    public static final double SEGUNDOS_DE_TENDERSE = numero("nadar", "segundos_de_tenderse");

    // Brazadas fuertes (con la tecla de saltar) y el arma, que se guarda al meterse en lo hondo.
    public static final float FUERTE_VELOCIDAD = (float) numero("nadar", "fuerte_velocidad");
    public static final double FUERTE_AGUANTE = numero("nadar", "fuerte_aguante"), FUERTE_BRAZADAS = numero("nadar", "fuerte_brazadas");
    public static final boolean GUARDA_EL_ARMA = texto("nadar", "guarda_el_arma").equals("si");

    // Ondas, salpicaduras y estela.
    public static final double SEGUNDOS_DE_ONDA = numero("rizos", "segundos_de_onda"), RADIO_DE_ONDA = numero("rizos", "radio_de_onda");
    public static final double PASO_DE_ESTELA = numero("rizos", "paso_de_estela"), SEGUNDOS_DE_SALPICADURA = numero("rizos", "segundos_de_salpicadura");
    public static final int GOTAS_AL_ENTRAR = (int) numero("rizos", "gotas_al_entrar"), GOTAS_AL_ZAMBULLIRSE = (int) numero("rizos", "gotas_al_zambullirse");
    public static final int GOTAS_DE_BRAZADA = (int) numero("rizos", "gotas_de_brazada"), MAXIMO_DE_RIZOS = (int) numero("rizos", "maximo");

    // Luchar en el agua.
    public static final double LENTITUD = numero("lucha", "lentitud"), DANIO = numero("lucha", "danio");
    /** Un arma que no es daga, con el agua por la cintura o más: cuánto más dura su golpe y qué parte le queda de alcance y de daño. */
    public static final double ARMA_LENTA = numero("lucha", "arma_lenta"), ARMA_CORTA = numero("lucha", "arma_corta"), ARMA_FLOJA = numero("lucha", "arma_floja");
    public static final float AGUANTE_POR_GOLPE = (float) numero("lucha", "aguante_por_golpe");

    // Tirarse de cabeza.
    public static final double SEGUNDOS_DE_ZAMBULLIDA = numero("zambullida", "segundos");
    public static final float AGUANTE_DE_ZAMBULLIDA = (float) numero("zambullida", "aguante");

    // Mojado.
    public static final double SEGUNDOS_MOJADO = numero("mojado", "segundos"), FRENO_DEL_AGUANTE = numero("mojado", "freno");
    public static final boolean TIRITA_DE_NOCHE = texto("mojado", "tirita_de_noche").equals("si");
    public static final boolean TIRITA_EN_MAZMORRA = texto("mojado", "tirita_en_mazmorra").equals("si");
    public static final boolean SE_GUARDA = texto("mojado", "se_guarda").equals("si");

    // Cómo se le ve el cuerpo (tercera persona).
    public static final double FLOTA = numero("cuerpo", "flota"), SE_HUNDE = numero("cuerpo", "se_hunde"), BAJO_EL_AGUA = numero("cuerpo", "bajo_el_agua");
    public static final double OSCURECE = numero("cuerpo", "oscurece"), TIEMBLA = numero("cuerpo", "tiembla");
    public static final int GOTAS = (int) numero("cuerpo", "gotas");

    // Cómo se ve por sus ojos (primera persona).
    public static final double OJOS = numero("vista", "ojos"), LINEA = numero("vista", "linea"), SUBE_Y_BAJA = numero("vista", "sube_y_baja");
    public static final double AL_CANSARSE = numero("vista", "al_cansarse"), TURBIA = numero("vista", "turbia");
    public static final double GOTAS_EN_LA_VISTA = numero("vista", "gotas"), VISTA_TIEMBLA = numero("vista", "tiembla");

    /** Lo mojado que lo deja la fila «Mojar» de F1 (de 0 a 1). */
    public static final double MOJAR_DE_F1 = numero("depuracion", "mojar_de_f1");

    /**
     * La hondura del agua, en píxeles, a esa distancia de la orilla (también
     * en píxeles): crece con la pendiente hasta la máxima.
     */
    public static float hondura(float distanciaALaOrilla) {
        return Math.max(0, Math.min(HONDURA_MAXIMA, distanciaALaOrilla * PENDIENTE));
    }

    private static String texto(String seccion, String clave) {
        String texto = ARCHIVO.valor(seccion, clave, null);
        if (texto == null) throw new IllegalStateException("agua.txt: falta «" + clave + "» en [" + seccion + "]");
        return texto;
    }

    private static double numero(String seccion, String clave) {
        return Double.parseDouble(texto(seccion, clave));
    }
}
