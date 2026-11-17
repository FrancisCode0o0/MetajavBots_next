package castillafantasx.paisaje;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import castillafantasx.superficie.MapaSuperficie;

/**
 * FLORA — Qué árbol y qué mata crece en cada casilla del mapa. En el mapa
 * un árbol es una «t» y una mata, unas comillas: aquí se decide cuál, según
 * el sitio, como crecen de verdad: chopos y sauces junto al agua, olivos
 * junto a las casas, encinas sueltas en el campo abierto, y robles y pinos
 * en rodales donde el monte es espeso. Y de qué tamaño, y un poco movido de
 * su casilla, para que no se vean en fila.
 *
 * Solo cambia el dibujo: cada árbol estorba donde siempre (el centro de su casilla).
 */
public final class Flora {

    private static final int TAM = MapaSuperficie.TAM;
    private static final int FORMAS = 3;                    // las que fabrica arte.TallerDeFlora de cada especie
    // Cifras de dibujo, PROVISIONALES.
    private static final int VAIVEN_X = 6, VAIVEN_Y = 4;    // píxeles que se mueve un árbol del centro de su casilla
    private static final float JUNCOS_EN_AGUA_QUIETA = 0.55f, JUNCOS_EN_RIO = 0.38f;   // la parte de las orillas que tiene juncos

    /** Una planta: su dibujo, lo que se mueve del centro de su casilla y si se pinta en espejo. */
    public static final class Planta {
        public String dibujo;
        public int dx, dy;
        public boolean espejo;
        public String copa;         // solo los árboles gigantes: el dibujo de su copa, que va aparte
    }

    private final Terreno terreno;
    // Las reglas de [flora] del mapa: de cada lugar, sus árboles y sus gigantes, cada uno repetido tantas veces como pesa.
    private final Map<String, String[]> arbolesDe = new HashMap<>(), gigantesDe = new HashMap<>();

    Flora(Terreno terreno) {
        this.terreno = terreno;
        for (Map.Entry<String, String> regla : terreno.mapa.flora.entrySet()) {
            String[] partes = regla.getValue().split("\\|");
            arbolesDe.put(regla.getKey(), lista(partes[0]));
            if (partes.length > 1) gigantesDe.put(regla.getKey(), lista(partes[1]));
        }
    }

    /** De "roble 6, encina 1" saca { roble, roble, roble, roble, roble, roble, encina }. */
    private static String[] lista(String texto) {
        List<String> nombres = new ArrayList<>();
        for (String trozo : texto.split(",")) {
            String[] p = trozo.trim().split("\\s+");
            if (p[0].isEmpty()) continue;
            int peso = p.length > 1 ? Integer.parseInt(p[1]) : 1;
            for (int i = 0; i < peso; i++) nombres.add(p[0]);
        }
        return nombres.toArray(new String[0]);
    }

    private static final int BORDE_MOVIDO = 5;      // casillas que se corre el borde entre dos lugares, para que no se vea la raya

    /** Lo que dice [flora] para el lugar de esa casilla (o null). El borde entre lugares va algo movido. */
    private String[] regla(Map<String, String[]> reglas, int c, int f) {
        if (reglas.isEmpty()) return null;
        int dc = Math.round((Ruido.suave(c * TAM, f * TAM, 7, 731) - 0.5f) * 2 * BORDE_MOVIDO);
        int df = Math.round((Ruido.suave(c * TAM, f * TAM, 7, 732) - 0.5f) * 2 * BORDE_MOVIDO);
        MapaSuperficie.Lugar lugar = terreno.mapa.lugarEn(c + dc, f + df);
        String[] lista = lugar == null ? null : reglas.get(lugar.nombre);
        return lista == null || lista.length == 0 ? null : lista;
    }

    // Cómo se pintan los gigantes (lo usan el juego y las fotos sin ventana). Cifras de dibujo, PROVISIONALES.
    public static final float GIGANTE_A_LOS_LADOS = 340f, GIGANTE_HACIA_ABAJO = 490f;   // lo lejos de la pantalla que aún asoma su copa
    public static final float COPA_BAJO_EL_TRONCO = 0.42f;      // la parte de la copa que cuelga por debajo de lo alto del tronco
    public static final float COPA_ENCIMA = 100000f;            // las copas se pintan por encima de todo lo demás, en su orden
    public static final float COPA_CLARA = 0.13f, COPA_OPACA = 0.95f;       // la copa se aclara sobre el héroe, para que se le vea debajo
    public static final float COPA_CLARA_HASTA = 0.8f, COPA_SE_CIERRA_EN = 0.8f;    // en radios de copa: hasta dónde es clara y lo que tarda en cerrarse
    public static final float COPA_SE_CORRE = 0.06f;            // la copa se corre un poco con la cámara: da hondura entre el suelo y el techo del bosque

    /** ¿Asoma en una vista (cámara y medidas) el gigante que tiene el centro del pie en (cx, base)? */
    public static boolean asomaElGigante(float cx, float base, float camX, float camY, float ancho, float alto) {
        return cx > camX - GIGANTE_A_LOS_LADOS && cx < camX + ancho + GIGANTE_A_LOS_LADOS && base > camY - 40 && base < camY + alto + GIGANTE_HACIA_ABAJO;
    }

    /** Lo opaca que se pinta una copa: (dx, dy), de su centro al héroe; 'radio', la mitad de su ancho. Debajo, clara; lejos, opaca. */
    public static float alfaDeCopa(float dx, float dy, float radio) {
        float lejos = (float) Math.sqrt(dx * dx + dy * dy) / radio;
        // Toda copa que tenga al héroe debajo (lejos < 1) queda clara; las de más allá se van cerrando.
        return Math.max(COPA_CLARA, Math.min(COPA_OPACA, COPA_CLARA + (lejos - COPA_CLARA_HASTA) / COPA_SE_CIERRA_EN * (COPA_OPACA - COPA_CLARA)));
    }

    /** Las especies de árbol gigante que sabe dibujar arte.TallerDeBosque. */
    public static final String[] GIGANTES = { "roble", "encina", "pino", "haya", "tejo" };
    /** Cuántas formas hay de cada tronco y de cada copa gigantes. */
    public static final int FORMAS_DE_GIGANTE = 2;

    /**
     * El árbol gigante cuyo pie empieza en (c, f) y mide 'ancho' casillas:
     * deja en 'p' el dibujo de su tronco y, en p.copa, el de su copa.
     */
    public void gigante(int c, int f, int ancho, Planta p) {
        String[] lista = regla(gigantesDe, c, f);
        String especie = lista == null ? GIGANTES[0] : lista[(int) (suerte(c, f, 741) * lista.length * 0.999f)];
        int forma = (int) (suerte(c, f, 742) * FORMAS_DE_GIGANTE * 0.999f), talla = ancho >= 3 ? 3 : 2;
        p.dibujo = "gigante/" + especie + "_tronco_" + talla + "_" + forma;
        p.copa = "gigante/" + especie + "_copa_" + talla + "_" + forma;
        p.dx = Math.round((suerte(c, f, 743) - 0.5f) * 8);
        p.dy = 0;
        p.espejo = suerte(c, f, 744) > 0.5f;
    }

    /** Un número de 0 a 1, fijo para cada casilla. */
    private static float suerte(int c, int f, int semilla) {
        return (Ruido.entero(c, f, semilla) & 0xFFFF) / 65535f;
    }

    /** El árbol de la casilla (c, f). 'muerto': en el mapa es un árbol muerto (solo se mueve un poco). */
    public void arbol(int c, int f, boolean muerto, Planta p) {
        float cual = suerte(c, f, 701), talla = suerte(c, f, 702);
        int forma = (int) (suerte(c, f, 703) * FORMAS * 0.999f);
        p.dx = Math.round((suerte(c, f, 704) - 0.5f) * 2 * VAIVEN_X);
        p.dy = Math.round((suerte(c, f, 705) - 0.6f) * 2 * VAIVEN_Y);
        p.espejo = suerte(c, f, 706) > 0.5f;
        if (muerto) {
            p.dibujo = "arbol/muerto_" + forma;
            return;
        }
        String especie;
        String[] delLugar = regla(arbolesDe, c, f);
        // TODO-FRAN: qué árboles crecen en cada lugar y en qué proporción.
        // Los rodales: manchas grandes donde manda una especie.
        float rodal = Ruido.suave(c * TAM, f * TAM, 8, 707);
        if (delLugar != null) especie = delLugar[(int) (cual * delLugar.length * 0.999f)];      // lo que diga [flora] del mapa
        else if (terreno.frescor(c, f) > 0.55f) especie = cual < 0.45f ? "chopo" : cual < 0.8f ? "sauce" : "roble";
        else if (terreno.pisado(c, f) > 0.12f) especie = cual < 0.5f ? "olivo" : cual < 0.8f ? "encina" : "roble";
        else if (terreno.arbolado(c, f) > 0.5f) especie = rodal < 0.36f ? (cual < 0.85f ? "pino" : "encina") : (cual < 0.72f ? "roble" : cual < 0.9f ? "encina" : "pino");
        else especie = cual < 0.62f ? "encina" : cual < 0.82f ? "roble" : "pino";
        boolean joven = talla < 0.2f && !especie.equals("roble") && !especie.equals("sauce") && !especie.equals("olivo");
        p.dibujo = "arbol/" + especie + (joven ? "_joven" : "_" + forma);
    }

    /** La mata de la casilla (c, f): la que crece en un sitio así. */
    public void mata(int c, int f, Planta p) {
        float cual = suerte(c, f, 711);
        int forma = (int) (suerte(c, f, 712) * FORMAS * 0.999f);
        p.dx = Math.round((suerte(c, f, 713) - 0.5f) * 10);
        p.dy = Math.round((suerte(c, f, 714) - 0.5f) * 6);
        p.espejo = suerte(c, f, 715) > 0.5f;
        String especie;
        if (terreno.clase(c - 1, f) == Terreno.TIERRA && terreno.clase(c + 1, f) == Terreno.TIERRA && enHuerto(c, f)) especie = "mata/hortaliza";
        else if (terreno.frescor(c, f) > 0.6f) especie = cual < 0.6f ? "mata/zarza" : "mata/helecho";
        else if (terreno.arbolado(c, f) > 0.4f) especie = cual < 0.45f ? "mata/helecho" : cual < 0.8f ? "mata/jara" : "mata/zarza";
        else especie = cual < 0.4f ? "mata/jara" : cual < 0.75f ? "mata/retama" : null;
        p.dibujo = especie == null ? "decor/matorral" : especie + "_" + forma;
        if (especie != null && especie.equals("mata/hortaliza")) p.dx = p.dy = 0;      // las hortalizas, en su fila
    }

    private static final int LADO_DE_HUERTO = 5;            // casillas a las que se busca la cerca de un huerto

    /** ¿Está la casilla en un huerto? (Junto a las casas, o con una cerca a pocas casillas en su fila y en su columna.) */
    private boolean enHuerto(int c, int f) {
        if (terreno.pisado(c, f) > 0) return true;
        boolean enFila = false, enColumna = false;
        for (int d = -LADO_DE_HUERTO; d <= LADO_DE_HUERTO; d++) {
            enFila |= terreno.mapa.esCerca(c + d, f);
            enColumna |= terreno.mapa.esCerca(c, f + d);
        }
        return enFila && enColumna;
    }

    private static final int[][] LADOS = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

    /**
     * ¿Hay juncos o cañas en la orilla de la casilla de agua (c, f) que da
     * hacia 'lado' (0 = norte, 1 = este, 2 = sur, 3 = oeste)? Si los hay,
     * deja en 'p' su dibujo y dónde tienen el pie (dx, dy: píxeles desde la
     * esquina de arriba a la izquierda de la casilla). Donde cruza un camino
     * o hay piedra o suelo hecho, la orilla queda limpia.
     */
    public boolean ribera(int c, int f, int lado, Planta p) {
        if (terreno.clase(c, f) != Terreno.AGUA || terreno.esPasadera(c, f) || terreno.esPaso(c, f)) return false;
        int vecina = terreno.clase(c + LADOS[lado][0], f + LADOS[lado][1]);
        if (vecina != Terreno.HIERBA && vecina != Terreno.BARRO) return false;
        if (terreno.mapa.macizo(c + LADOS[lado][0], f + LADOS[lado][1])) return false;
        float cuantos = terreno.esRio(c, f) ? JUNCOS_EN_RIO : JUNCOS_EN_AGUA_QUIETA;
        if (suerte(c * 4 + lado, f, 721) >= cuantos) return false;
        int aLoLargo = 5 + (int) (suerte(c * 4 + lado, f, 722) * 22), haciaDentro = 3 + (int) (suerte(c * 4 + lado, f, 723) * 4);
        p.dx = lado == 1 ? TAM - haciaDentro : lado == 3 ? haciaDentro : aLoLargo;
        p.dy = lado == 0 ? haciaDentro + 2 : lado == 2 ? TAM - haciaDentro : aLoLargo;
        p.espejo = suerte(c * 4 + lado, f, 724) > 0.5f;
        boolean canas = suerte(c * 4 + lado, f, 725) < 0.3f;
        p.dibujo = (canas ? "decor/canas_" : "decor/juncos_") + (int) (suerte(c * 4 + lado, f, 726) * FORMAS * 0.999f);
        return true;
    }
}
