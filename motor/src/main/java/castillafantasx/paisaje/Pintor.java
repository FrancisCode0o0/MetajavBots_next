package castillafantasx.paisaje;

import castillafantasx.arte.Colores;
import castillafantasx.arte.Trama;

/**
 * PINTOR — El color de cada punto del suelo, de día y de noche, según lo que
 * Formas dice que hay en él. Los colores son los del campo de Castilla:
 * pajizos y dorados de la hierba agostada, pardos y rojizos de la tierra,
 * verde vivo solo junto al agua, grises cálidos y fríos de la roca.
 *
 * Como el Escultor de las figuras: pocos tonos por material (una «rampa» de
 * cinco, de oscuro a claro) y, entre un tono y el siguiente, tramado de puntos.
 * Nada se repite de casilla en casilla: todo se pinta con las coordenadas del mapa.
 */
final class Pintor {

    // --- Las rampas de día (0xRRGGBB, de oscuro a claro). PROVISIONALES. ---
    private static final int[] PAJIZA = { 0x857B50, 0x968C5D, 0xA69C6A, 0xB4AB78, 0xC1B988 };       // hierba agostada
    private static final int[] DORADA = { 0x877443, 0x9A864D, 0xAB9758, 0xBAA765, 0xC8B775 };       // pasto y rastrojo
    private static final int[] VERDE_SECA = { 0x666C40, 0x767D49, 0x868E54, 0x959D60, 0xA4AC6F };   // hierba que aún verdea
    private static final int[] VERDE_VIVA = { 0x435F31, 0x517139, 0x618342, 0x72954D, 0x85A65C };   // riberas y huertas
    private static final int[] PARDA = { 0x6F573A, 0x806745, 0x907650, 0x9F865D, 0xAD966B };        // tierra de camino
    private static final int[] ROJIZA = { 0x6E4C33, 0x805A3D, 0x916948, 0xA07855, 0xAE8864 };       // tierra de barbecho
    private static final int[] POLVO = { 0x7C6A4B, 0x8E7B58, 0x9E8B66, 0xAD9B75, 0xBBAA85 };        // tierra seca y pisada
    private static final int[] BARRO = { 0x3F3224, 0x4D3E2C, 0x5B4A35, 0x69573F, 0x77644A };
    private static final int[] GRAVA = { 0x6F695C, 0x837D6E, 0x979080, 0xA9A292, 0xBAB4A4 };
    private static final int[] GRANITO = { 0x5E5850, 0x736C62, 0x878075, 0x9B9488, 0xAFA89C };      // gris cálido
    private static final int[] CALIZA = { 0x766E5A, 0x8C846D, 0xA09880, 0xB3AB94, 0xC4BDA8 };       // ocre claro
    private static final int[] PIZARRA = { 0x3F434A, 0x4E535C, 0x5E646E, 0x6F7680, 0x828993 };      // gris frío
    private static final int[] RIO = { 0x8E8C66, 0x6F8566, 0x4F705B, 0x37584E, 0x25413F };          // de somero a hondo
    private static final int[] LAGO = { 0x8A9684, 0x71908A, 0x5A7F82, 0x496E74, 0x3C5E66 };
    private static final int LIQUEN_OCRE = 0xA89A4A, LIQUEN_GRIS = 0x9AA58C, MUSGO = 0x55702F, ESPUMA = 0xDDE6DC;
    private static final int[] HIERBA_DE_NOCHE = { 0x1C2818, 0x24331D, 0x2C3E22, 0x354828, 0x3F532F };
    private static final int FONDO_DE_GRIETA = 0x070608;

    // Cifras de dibujo, PROVISIONALES.
    private static final float SOL_EN_LA_PIEDRA = 2.1f;     // cuánto aclara el sol el empedrado y las losas
    static final int GRANITO_ES = 0, CALIZA_ES = 1, PIZARRA_ES = 2;
    private static final int COMARCA = 11;                  // lo que mide cada comarca de roca: 2 elevado a esto, en píxeles
    // Las piedras de cada roca (granito, caliza, pizarra): lo que miden, lo alargadas que son y hacia dónde se tumban (coseno y seno).
    private static final float[] PIEDRA_DE = { 27, 21, 13 }, ESTIRADA = { 1.15f, 1.6f, 3.4f };
    private static final float[] COSENO = { 0.88f, 1f, 0.91f }, SENO = { 0.48f, 0.06f, 0.41f };

    private final Grano hierba = new Grano("suelos/hierba"), tierra = new Grano("suelos/tierra"), empedrado = new Grano("suelos/empedrado"),
            losa = new Grano("suelos/losa"), paja = new Grano("suelos/paja"), nervios = new Grano("suelos/nervios");
    private final Celdas celdas = new Celdas();
    private final Terreno terreno;

    /** El color de día y el de noche del último punto pintado (0xRRGGBB). */
    int dia, noche;
    /** El color de día del suelo de arriba del corte que se está pintando (lo pone el Horno antes de llamar a corte). */
    int diaDeArriba;

    // El suelo del monte (res/mundo/bosque.txt): a la sombra no hay pasto agostado, sino hojarasca, sotobosque y musgo.
    private static final int[] HOJARASCA = { 0x3A3122, 0x473C29, 0x544831, 0x61543A, 0x6E6044 };
    private static final int[] SOTOBOSQUE = { 0x2C3D25, 0x36492B, 0x415632, 0x4D633A, 0x5A7044 };
    private static final int[] MUSGO_DE_UMBRIA = { 0x24391F, 0x2C4625, 0x35532B, 0x3F6032, 0x4A6E3A };
    private final boolean esBosque;       // este mapa es el bosque

    Pintor(Terreno terreno) {
        this.terreno = terreno;
        this.esBosque = castillafantasx.superficie.MapaSuperficie.BOSQUE.equals(terreno.mapa.id);
    }

    /** El tono de una rampa que toca para un valor de 0 (oscuro) a 1 (claro), tramado en el píxel (x, y). */
    static int tono(int[] rampa, float valor, int x, int y) {
        return Trama.tono(rampa, valor, x, y);
    }

    /** ¿Toca aquí lo segundo? 'cuanto' va de 0 (nunca) a 1 (siempre); entre medias, a puntitos: así dos colores se funden poco a poco. */
    private static boolean tramado(float cuanto, int x, int y) {
        return cuanto > (Trama.BAYER[(y >> 1) & 3][(x >> 1) & 3] + 0.5f) / 16f;
    }

    /** Un color de día, como se ve de noche con una lumbre cerca: más oscuro y algo frío. */
    static int deNoche(int dia) {
        return Colores.mult(Colores.apagar(dia, 0.25f), 0.50f, 0.53f, 0.58f) & 0xFFFFFF;
    }

    // Hasta dónde llega cada roca, de poniente a levante, en píxeles del mapa (PROVISIONAL).
    private static final int PIZARRA_HASTA = 62 * 32, CALIZA_HASTA = 184 * 32, VAIVEN_DE_COMARCA = 6 * 32;

    /**
     * Qué roca hay en cada comarca, como en las sierras del centro: pizarra
     * a poniente, caliza en medio (la peña del castillo y los cerros del
     * valle) y granito a levante (la sierra). El límite entre una y otra no es recto.
     */
    // TODO-FRAN: qué roca tiene cada lugar del mapa.
    static int rocaDe(int x, int y) {
        float donde = x + (Ruido.suave(x, y, COMARCA, 401) - 0.5f) * 2 * VAIVEN_DE_COMARCA;
        return donde < PIZARRA_HASTA ? PIZARRA_ES : donde < CALIZA_HASTA ? CALIZA_ES : GRANITO_ES;
    }

    private static int[] rampaDeRoca(int tipo) {
        return tipo == GRANITO_ES ? GRANITO : tipo == CALIZA_ES ? CALIZA : PIZARRA;
    }

    /** Pinta el punto: deja su color en 'dia' y 'noche'. */
    void pintar(Punto p, int x, int y) {
        int clase = p.clase;
        // Cerca del borde entre dos suelos, uno se deshace en el otro a mechones.
        if (p.margen < 0.5f && p.otra != clase && blando(clase) && blando(p.otra)) {
            float mechones = Ruido.suave(x, y, 2, 411) * 0.6f + Colores.ruido(x, y, 412) * 0.4f;
            if (mechones < 0.5f - p.margen) clase = p.otra;
        }
        // Una cuesta es de tierra pisada.
        if (p.cuesta && clase != Terreno.LANCHA) clase = Terreno.TIERRA;
        // Junto a las casas, la hierba está pisada: calvas de tierra.
        if (clase == Terreno.HIERBA && p.pisado > 0.1f
                && Ruido.suave(x, y, 4, 413) * 0.7f + Ruido.suave(x, y, 2, 414) * 0.3f < p.pisado * 0.5f) clase = Terreno.TIERRA;
        // La orilla: grava en los ríos y barro en las aguas quietas, más cuanto más cerca del agua.
        if (clase != Terreno.AGUA && clase != Terreno.GRIETA && p.orilla > 0.12f
                && p.orilla + (Ruido.suave(x, y, 2, 415) - 0.5f) * 0.3f > 0.36f) {
            orilla(p, x, y);
            return;
        }
        switch (clase) {
            case Terreno.HIERBA:    hierba(p, x, y); break;
            case Terreno.TIERRA:    tierra(p, x, y); break;
            case Terreno.BARRO:     barro(x, y); break;
            case Terreno.EMPEDRADO: deArchivo(empedrado, x, y, SOL_EN_LA_PIEDRA); break;
            case Terreno.LOSA:      deArchivo(losa, x, y, SOL_EN_LA_PIEDRA); break;
            case Terreno.PAJA:      deArchivo(paja, x, y, 1); break;
            case Terreno.SEMBRADO:  labor(p, x, y, true); break;
            case Terreno.BARBECHO:  labor(p, x, y, false); break;
            case Terreno.NERVIOS:   deArchivo(nervios, x, y, 1); break;
            case Terreno.AGUA:
                if (p.piedra) roca(p, x, y, 1f, 0.1f);
                else agua(p, x, y);
                break;
            case Terreno.GRIETA:    dia = noche = FONDO_DE_GRIETA; break;
            case Terreno.TARIMA:    tarima(x, y); break;
            case Terreno.BALDOSA:   baldosa(x, y); break;
            case Terreno.ESTERA:    estera(x, y); break;
            case Terreno.DAMERO:    damero(x, y); break;
            case Terreno.OLAMBRILLA: olambrilla(x, y); break;
            case Terreno.TIERRA_PISADA: tierraPisada(x, y); break;
            case Terreno.ENLOSADO:  enlosado(x, y); break;
            case Terreno.LANCHA:    roca(p, x, y, 0.35f, 0); break;
            default:                roca(p, x, y, 1f, 0); break;
        }
        // Los peldaños de una cuesta: una raya oscura cada pocos píxeles de subida, con su filo de luz debajo.
        if (p.cuesta) {
            int sube = Math.floorMod((int) p.altura, 4);
            if (sube == 0) dia = Colores.brillo(dia, 0.74f) & 0xFFFFFF;
            else if (sube == 3) dia = Colores.brillo(dia, 1.10f) & 0xFFFFFF;
            noche = deNoche(dia);
            return;
        }
        // El matorral bajo que crece solo, y las piedras: las caídas al pie de una peña (el canchal) y alguna suelta por el campo.
        if (clase == Terreno.HIERBA) matas(p, x, y);
        if (clase == Terreno.HIERBA || clase == Terreno.TIERRA) {
            piedras(x, y, Math.max(0, p.roca - 0.12f) * 1.9f + p.pedregal * 0.05f + 0.004f);
        }
    }

    /** ¿Es un suelo blando, de los que se deshacen en el de al lado a mechones? (El agua y lo empedrado tienen el borde limpio.) */
    private static boolean blando(int clase) {
        return clase == Terreno.HIERBA || clase == Terreno.TIERRA || clase == Terreno.BARRO || clase == Terreno.PAJA || clase == Terreno.LANCHA
                || clase == Terreno.SEMBRADO || clase == Terreno.BARBECHO;
    }

    // ------------------------------------------------------------------
    // Cada suelo
    // ------------------------------------------------------------------

    private void hierba(Punto p, int x, int y) {
        // Lo claro: el grano fino y, por encima, manchas grandes y medianas.
        float manchas = Ruido.suave(x, y, 7, 421) * 0.6f + Ruido.suave(x, y, 5, 422) * 0.4f;
        float valor = 0.5f + (hierba.claro(x, y) - 0.5f) * 0.7f + (manchas - 0.5f) * 0.45f;
        // El verdor: junto al agua y a la sombra de los árboles; en lo demás, agostada.
        float verdor = p.frescor * 1.2f + p.arbolado * 0.4f + (Ruido.suave(x, y, 7, 423) - 0.5f) * 0.4f;
        // Entre lo dorado y lo pajizo, y entre lo seco y lo verde, se pasa poco a poco, a puntitos.
        float dorado = (Ruido.suave(x, y, 8, 425) - 0.35f) * 3.3f;
        int[] rampa = tramado(dorado, x, y) ? DORADA : PAJIZA;
        if (tramado((verdor - 0.22f) * 3.4f, x, y)) rampa = VERDE_SECA;
        if (tramado((verdor - 0.80f) * 4f, x, y)) rampa = VERDE_VIVA;
        if (esBosque) {
            // En el monte: sotobosque verde oscuro, con rodales de hojarasca y de musgo; solo en los claros al sol verdea la hierba.
            float hojas = Ruido.suave(x, y, 6, 441) * 0.6f + Ruido.suave(x, y, 4, 442) * 0.4f;
            float musgo = Ruido.suave(x, y, 6, 443) + p.frescor * 0.5f;
            rampa = SOTOBOSQUE;
            if (tramado((hojas - 0.47f) * 5f, x, y)) rampa = HOJARASCA;
            if (tramado((musgo - 0.62f) * 5f, x, y)) rampa = MUSGO_DE_UMBRIA;
            if (tramado((0.18f - p.arbolado) * 4f + (dorado - 0.5f) * 0.3f, x, y)) rampa = VERDE_SECA;
            if (tramado((p.frescor - 0.55f) * 4f, x, y)) rampa = VERDE_VIVA;
            valor -= 0.06f;
        }
        dia = tono(rampa, valor, x, y);
        // De noche, a la luz de una lumbre, la hierba se ve verde oscura (como hasta ahora).
        noche = Colores.mezcla(tono(HIERBA_DE_NOCHE, valor, x, y), deNoche(dia), 0.35f) & 0xFFFFFF;
    }

    private void tierra(Punto p, int x, int y) {
        float manchas = Ruido.suave(x, y, 7, 431) * 0.55f + Ruido.suave(x, y, 5, 432) * 0.45f;
        float valor = 0.5f + (tierra.claro(x, y) - 0.5f) * 0.65f + (manchas - 0.5f) * 0.45f - p.frescor * 0.25f;
        float clase = Ruido.suave(x, y, 7, 433);
        int[] rampa = tramado((clase - 0.42f) * 5f, x, y) ? POLVO : PARDA;
        if (tramado((0.30f - clase) * 6f, x, y)) rampa = ROJIZA;
        // Las sendas del monte: tierra oscura y húmeda, con hojas.
        if (esBosque) rampa = tramado((clase - 0.5f) * 4f, x, y) ? PARDA : HOJARASCA;
        dia = tono(rampa, valor, x, y);
        noche = deNoche(dia);
    }

    // Los surcos de la tierra de labor (cifras PROVISIONALES): cada cuántos píxeles va uno y cuánto se tuercen.
    private static final int PASO_DE_SURCO = 6;
    private static final float VAIVEN_DE_SURCO = 14f;

    /**
     * La tierra de labor, con sus surcos: el sembrado es mies o rastrojo
     * (dorado, con algo de verde junto al agua) y el barbecho, tierra rojiza
     * levantada por el arado. Los surcos del sembrado van de levante a
     * poniente y los del barbecho, de norte a sur; ninguno va a cordel.
     */
    private void labor(Punto p, int x, int y, boolean sembrado) {
        float tuerce = (Ruido.suave(x, y, 7, 471) - 0.5f) * VAIVEN_DE_SURCO + (Ruido.suave(x, y, 4, 472) - 0.5f) * 3;
        int surco = Math.floorMod((int) ((sembrado ? y : x) + tuerce), PASO_DE_SURCO);
        float manchas = Ruido.suave(x, y, 7, 473) * 0.6f + Ruido.suave(x, y, 4, 474) * 0.4f;
        float valor = (sembrado ? 0.62f : 0.5f) + (tierra.claro(x, y) - 0.5f) * 0.5f + (manchas - 0.5f) * 0.4f;
        // El lomo del surco, claro; su fondo, en sombra.
        valor += surco == 0 ? -0.42f : surco == 1 ? -0.16f : surco == 3 ? 0.12f : 0;
        int[] rampa = sembrado ? DORADA : ROJIZA;
        if (sembrado && tramado((p.frescor - 0.35f) * 3f, x, y)) rampa = VERDE_SECA;
        if (!sembrado && tramado((Ruido.suave(x, y, 6, 475) - 0.55f) * 4f, x, y)) rampa = PARDA;
        dia = tono(rampa, valor, x, y);
        noche = deNoche(dia);
    }

    private void barro(int x, int y) {
        float valor = Ruido.suave(x, y, 5, 441) * 0.45f + Ruido.suave(x, y, 3, 442) * 0.35f + Colores.ruido(x, y, 443) * 0.2f;
        dia = tono(BARRO, valor, x, y);
        // Algún charco pequeño, que brilla.
        if (Ruido.suave(x, y, 3, 444) > 0.86f) dia = Colores.mezcla(dia, 0x8A8F86, 0.35f) & 0xFFFFFF;
        noche = deNoche(dia);
    }

    // ------------------------------------------------------------------
    // Los suelos de dentro de las casas (encargo de los interiores). Medidas y colores PROVISIONALES.
    // ------------------------------------------------------------------

    private static final int[] TABLA = { 0x3A2A1C, 0x4A3624, 0x5A432D, 0x6A5037, 0x7A5E42 };          // roble viejo, oscurecido
    private static final int[] BARRO_COCIDO = { 0x6A3A28, 0x7E4630, 0x92543A, 0xA46446, 0xB47654 };   // baldosa de alfar
    private static final int[] ESPARTO = { 0x6E5E34, 0x82703E, 0x96824A, 0xA89458, 0xBAA668 };
    private static final int[] LOSA_CLARA = { 0x8A8474, 0x9C9684, 0xAEA894, 0xBEB8A4, 0xCEC8B6 };     // caliza
    private static final int[] LOSA_NEGRA = { 0x24242A, 0x2E2E36, 0x393A42, 0x45464E, 0x52535C };     // pizarra
    private static final int[] TIERRA_DE_CASA = { 0x3E3226, 0x4A3C2D, 0x564735, 0x62523D, 0x6E5D46 };
    private static final int ALTO_DE_TABLA = 8, LARGO_DE_TABLA = 56, LADO_DE_BALDOSA = 16, LADO_DE_LOSA = 16;

    /** De noche, un suelo de interior se ve como de día, algo más apagado: su luz es la de las velas, no la del cielo. */
    private void deInterior(int color) {
        dia = color & 0xFFFFFF;
        noche = Colores.brillo(color, 0.82f) & 0xFFFFFF;
    }

    /** Tarima: tablas largas a lo ancho, a rompejuntas, cada una de su tono, con su veta y sus clavos. */
    private void tarima(int x, int y) {
        int fila = Math.floorDiv(y, ALTO_DE_TABLA), dy = Math.floorMod(y, ALTO_DE_TABLA);
        int corrida = x + (int) (Colores.ruido(fila, 0, 601) * LARGO_DE_TABLA);
        int tabla = Math.floorDiv(corrida, LARGO_DE_TABLA), dx = Math.floorMod(corrida, LARGO_DE_TABLA);
        float veta = Colores.ruidoSuave(x * 0.25f, y * 2.2f + tabla * 31, 3, 602);
        float valor = 0.30f + Colores.ruido(tabla, fila, 603) * 0.42f + (veta - 0.5f) * 0.22f;
        if (dy == 0 || dx == 0) valor = 0.02f;                    // la junta
        else if (dy == 1) valor += 0.10f;                         // el canto de la tabla, con algo de luz
        boolean clavo = (dx == 3 || dx == LARGO_DE_TABLA - 3) && (dy == 2 || dy == ALTO_DE_TABLA - 2);
        deInterior(clavo ? 0x1E1A18 : tono(TABLA, valor, x, y));
    }

    /** Baldosas de barro cocido, cuadradas, con la junta de tierra y alguna más gastada o más quemada. */
    private void baldosa(int x, int y) {
        int c = Math.floorDiv(x, LADO_DE_BALDOSA), f = Math.floorDiv(y, LADO_DE_BALDOSA);
        int dx = Math.floorMod(x, LADO_DE_BALDOSA), dy = Math.floorMod(y, LADO_DE_BALDOSA);
        float valor = 0.32f + Colores.ruido(c, f, 611) * 0.40f + (Ruido.suave(x, y, 2, 612) - 0.5f) * 0.20f;
        if (dx == 0 || dy == 0) valor = 0.0f;
        else if (dx == 1 || dy == 1) valor += 0.12f;
        else if (dx == LADO_DE_BALDOSA - 1 || dy == LADO_DE_BALDOSA - 1) valor -= 0.14f;
        deInterior(tono(BARRO_COCIDO, valor, x, y));
    }

    /** Estera de esparto: pleita cosida en tiras, con la trenza en espiga. */
    private void estera(int x, int y) {
        int tira = Math.floorDiv(y, 6), dy = Math.floorMod(y, 6);
        int espiga = Math.floorMod(x + (tira % 2 == 0 ? dy : -dy), 4);
        float valor = 0.45f + (espiga == 0 ? -0.30f : espiga == 2 ? 0.14f : 0) + (Colores.ruido(x, y, 621) - 0.5f) * 0.16f;
        if (dy == 0) valor = 0.08f;       // la costura entre dos tiras
        deInterior(tono(ESPARTO, valor, x, y));
    }

    /** Damero: losas de caliza y de pizarra, alternas (iglesias y casas ricas). */
    private void damero(int x, int y) {
        int c = Math.floorDiv(x, LADO_DE_LOSA), f = Math.floorDiv(y, LADO_DE_LOSA);
        int dx = Math.floorMod(x, LADO_DE_LOSA), dy = Math.floorMod(y, LADO_DE_LOSA);
        boolean negra = Math.floorMod(c + f, 2) == 0;
        float valor = 0.40f + Colores.ruido(c, f, 631) * 0.24f + (Ruido.suave(x, y, 3, 632) - 0.5f) * 0.26f;
        if (dx == 0 || dy == 0) valor = 0.0f;
        deInterior(tono(negra ? LOSA_NEGRA : LOSA_CLARA, valor, x, y));
    }

    private static final int[] VIDRIADOS = { 0x2E5A6E, 0xC8C0A0, 0x3E6A46, 0x8A6A2A };     // azul, blanco, verde y melado

    /** Ladrillo a la morisca con olambrillas: ladrillos en cestillo y, en los cruces, un azulejo pequeño vidriado. */
    private void olambrilla(int x, int y) {
        int c = Math.floorDiv(x, 16), f = Math.floorDiv(y, 16), dx = Math.floorMod(x, 16), dy = Math.floorMod(y, 16);
        // El azulejito: un cuadrado de 6 en la esquina de cada cuatro ladrillos.
        int ex = Math.floorMod(x + 3, 16), ey = Math.floorMod(y + 3, 16);
        if (ex < 6 && ey < 6) {
            int cual = (int) (Colores.ruido(Math.floorDiv(x + 3, 16), Math.floorDiv(y + 3, 16), 641) * VIDRIADOS.length);
            boolean borde = ex == 0 || ey == 0 || ex == 5 || ey == 5;
            boolean dibujo = (ex == 2 || ex == 3) && (ey == 2 || ey == 3);
            deInterior(borde ? 0x2A2420 : dibujo ? 0xD8D0B0 : VIDRIADOS[Math.min(cual, VIDRIADOS.length - 1)]);
            return;
        }
        // Los ladrillos: en una casilla, dos tumbados; en la de al lado, dos de pie.
        boolean tumbados = Math.floorMod(c + f, 2) == 0;
        int a = tumbados ? dy : dx;
        float valor = 0.36f + Colores.ruido(c * 2 + (a < 8 ? 0 : 1), f * 2 + (tumbados ? 0 : 1), 642) * 0.36f + (Colores.ruido(x, y, 643) - 0.5f) * 0.14f;
        if (a == 0 || a == 8 || (tumbados ? dx : dy) == 0) valor = 0.0f;
        deInterior(tono(BARRO_COCIDO, valor, x, y));
    }

    private static final int[] LOSA_DE_IGLESIA = { 0x3E3A34, 0x524C44, 0x666056, 0x7A7468, 0x8E887A };

    /** Enlosado: losas largas de piedra, a rompejuntas, gastadas por el paso (iglesias, claustros, torres). */
    private void enlosado(int x, int y) {
        int fila = Math.floorDiv(y, 16), corrida = x + (Math.floorMod(fila, 2) == 0 ? 0 : 16);
        int losa = Math.floorDiv(corrida, 32), dx = Math.floorMod(corrida, 32), dy = Math.floorMod(y, 16);
        float valor = 0.40f + Colores.ruido(losa, fila, 661) * 0.30f + (Ruido.suave(x, y, 4, 662) - 0.5f) * 0.24f + (Colores.ruido(x, y, 663) - 0.5f) * 0.08f;
        if (dx == 0 || dy == 0) valor = 0.0f;
        else if (dx == 1 || dy == 1) valor += 0.10f;
        deInterior(tono(LOSA_DE_IGLESIA, valor, x, y));
    }

    /** Tierra apisonada: el suelo de las casas pobres, oscuro y liso, con rodales más pisados. */
    private void tierraPisada(int x, int y) {
        float valor = 0.42f + (Ruido.suave(x, y, 6, 651) - 0.5f) * 0.5f + (Ruido.suave(x, y, 3, 652) - 0.5f) * 0.25f + (Colores.ruido(x, y, 653) - 0.5f) * 0.10f;
        deInterior(tono(TIERRA_DE_CASA, valor, x, y));
    }

    /** Un suelo que conserva su dibujo de siempre (empedrado, losas, paja…): de noche tal cual y de día, aclarado por el sol. */
    private void deArchivo(Grano grano, int x, int y, float sol) {
        float manchas = 0.92f + Ruido.suave(x, y, 6, 451) * 0.16f;
        noche = Colores.brillo(grano.color(x, y), manchas) & 0xFFFFFF;
        dia = sol == 1 ? Colores.brillo(noche, 1.25f) & 0xFFFFFF : Colores.mult(noche, sol, sol * 0.96f, sol * 0.86f) & 0xFFFFFF;
    }

    /** La orilla: playa de grava (río) o de barro (agua quieta), con el filo mojado más oscuro. */
    private void orilla(Punto p, int x, int y) {
        float valor = Ruido.suave(x, y, 4, 461) * 0.5f + Colores.ruido(x >> 1, y >> 1, 462) * 0.5f - Math.max(0, p.orilla - 0.42f) * 2.2f;
        boolean deRio = terreno.esRio(p.columna, p.fila) || terreno.esRio(p.columna + 1, p.fila) || terreno.esRio(p.columna - 1, p.fila)
                || terreno.esRio(p.columna, p.fila + 1) || terreno.esRio(p.columna, p.fila - 1);
        dia = tono(deRio ? GRAVA : BARRO, valor + (deRio ? 0 : 0.2f), x, y);
        noche = deNoche(dia);
    }

    /**
     * La roca. Está partida en piedras (bolos redondos el granito, bloques la
     * caliza, lajas tumbadas la pizarra): cada una con su tono, con luz
     * arriba a la izquierda y sombra abajo a la derecha, y las juntas
     * oscuras. Encima, alguna fisura fina, líquenes y, donde hay frescor, musgo.
     * 'bulto': de 0 (una lancha lisa) a 1 (peñas con volumen). 'masClara': lo que se aclara, de 0 a 1.
     */
    void roca(Punto p, int x, int y, float bulto, float masClara) {
        int tipo = rocaDe(x, y);
        float tamano = PIEDRA_DE[tipo] * (bulto < 1 ? 1.7f : 1);       // las lanchas son piedras grandes y llanas
        // Las juntas no son rectas: el punto se mueve un poco antes de buscar su piedra.
        float mx = x + (Ruido.suave(x, y, 3, 471) - 0.5f) * 5, my = y + (Ruido.suave(x, y, 3, 472) - 0.5f) * 5;
        celdas.buscar(mx, my, tamano, ESTIRADA[tipo], COSENO[tipo], SENO[tipo], 473 + tipo * 10);
        float grano = Ruido.suave(x, y, 2, 474) * 0.6f + Colores.ruido(x, y, 475) * 0.4f;
        float valor = 0.46f + masClara + (celdas.suerte - 0.5f) * 0.30f + (grano - 0.5f) * 0.22f
                - (celdas.dx + celdas.dy * 1.3f) * (0.10f + 0.16f * bulto);
        boolean junta = celdas.borde < (bulto < 1 ? 0.75f : 1.1f);
        // Una fisura fina cruza algunas piedras.
        boolean fisura = !junta && celdas.otraSuerte > 0.55f && Math.abs(celdas.dx * 0.6f + celdas.dy - (celdas.suerte - 0.5f)) < 0.045f;
        if (junta) valor = valor * 0.35f - 0.12f;
        else if (fisura) valor -= 0.3f;
        dia = tono(rampaDeRoca(tipo), valor, x, y);
        if (junta) dia = Colores.brillo(dia, bulto < 1 ? 0.80f : 0.66f) & 0xFFFFFF;
        // Líquenes: unas piedras los tienen y otras no. Musgo: donde hay agua cerca o sombra de árboles, en las juntas.
        float mancha = Ruido.suave(x, y, 2, 481);
        if (!junta && celdas.otraSuerte < 0.30f && mancha > 0.66f) {
            dia = Colores.mezcla(dia, celdas.suerte > 0.5f ? LIQUEN_OCRE : LIQUEN_GRIS, 0.5f) & 0xFFFFFF;
        } else if (celdas.borde < 3.5f && mancha < (p.frescor + p.arbolado) * 0.5f + 0.12f && Ruido.suave(x, y, 5, 484) > 0.45f) {
            dia = Colores.mezcla(dia, MUSGO, 0.6f) & 0xFFFFFF;
        }
        noche = deNoche(dia);
    }

    /**
     * El agua. La del río: verdosa, con la grava a la vista donde es somera y
     * oscura donde es honda, y rayada por la corriente. La quieta: del color
     * del cielo, lisa, con alguna onda.
     */
    private void agua(Punto p, int x, int y) {
        float hondo = Math.min(1, p.orilla * 0.55f + p.hondura * 0.8f);
        if (p.paso) hondo *= 0.4f;         // donde cruza el camino, el agua es somera: por ahí se salta
        if (p.rio) {
            // Tramos: unos someros y rápidos, otros hondos y lentos.
            float tramo = Ruido.suave(x, y, 7, 491);
            hondo = Math.min(1, hondo * (0.55f + tramo * 0.9f));
        }
        float valor = hondo + (Ruido.suave(x, y, 3, 492) - 0.5f) * 0.14f;
        dia = tono(p.rio ? RIO : LAGO, valor, x, y);
        if (hondo < 0.45f) {
            // El fondo se ve: guijarros claros y oscuros bajo el agua.
            float guijarro = Colores.ruido(x / 3, y / 2, 493);
            if (guijarro > 0.62f) dia = Colores.mezcla(dia, guijarro > 0.85f ? 0xB9B4A2 : 0x5E5A4C, (0.45f - hondo) * 1.1f) & 0xFFFFFF;
        }
        if (p.rio) {
            // La corriente: rayas claras a lo largo de por donde corre; más, y más blancas, en los rápidos.
            float aLoLargo = x * p.corrienteX + y * p.corrienteY, aLoAncho = -x * p.corrienteY + y * p.corrienteX;
            float raya = Colores.ruidoSuave(aLoAncho * 3f, aLoLargo * 0.45f, 5, 494);
            float rapido = 1 - hondo;
            if (raya > 0.80f - rapido * 0.16f) dia = Colores.mezcla(dia, ESPUMA, 0.12f + rapido * 0.3f) & 0xFFFFFF;
        } else if (Colores.ruidoSuave(x * 0.35f, y, 3, 495) > 0.83f && hondo > 0.3f) {
            dia = Colores.mezcla(dia, 0xB8CCD0, 0.16f) & 0xFFFFFF;     // una onda, a lo ancho
        }
        // El filo de la orilla: una línea clara donde el agua lame la tierra.
        if (p.orilla < 0.07f) dia = Colores.mezcla(dia, ESPUMA, p.rio ? 0.45f : 0.25f) & 0xFFFFFF;
        noche = Colores.mult(Colores.apagar(dia, 0.35f), 0.36f, 0.42f, 0.52f) & 0xFFFFFF;
    }

    /** Piedras sueltas: cada una ocupa un hueco de una rejilla torcida; 'cuantas', de 0 (ninguna) a 1 (todas). */
    private void piedras(int x, int y, float cuantas) {
        if (cuantas <= 0) return;
        int fila = Math.floorDiv(y, 5), desplazada = x + (int) (Colores.ruido(fila, 0, 501) * 7);
        int hueco = Math.floorDiv(desplazada, 7);
        if (Colores.ruido(hueco, fila, 502) >= cuantas) return;
        // La piedra ocupa parte de su hueco: su tamaño también va a suertes.
        int dx = Math.floorMod(desplazada, 7), dy = Math.floorMod(y, 5);
        int ancho = 3 + (int) (Colores.ruido(hueco, fila, 503) * 4), alto = 2 + (int) (Colores.ruido(hueco, fila, 504) * 3);
        if (dx >= ancho || dy >= alto) return;
        boolean esquina = (dx == 0 || dx == ancho - 1) && (dy == 0 || dy == alto - 1);
        if (esquina && ancho > 3) return;
        // Luz arriba a la izquierda, sombra abajo.
        float valor = dy == alto - 1 ? 0.05f : dy == 0 && dx < ancho - 1 ? 0.95f : 0.55f;
        dia = tono(rampaDeRoca(rocaDe(x, y)), valor, x, y);
        noche = deNoche(dia);
    }

    // Las matas: tres tonos (sombra, cuerpo y luz) y el color de su flor.
    private static final int[] TOMILLO = { 0x5C6648, 0x76805A, 0x8F9A70, 0xB58BB0 }, ROMERO = { 0x475A40, 0x5E7354, 0x788C6A, 0xA9A6D2 },
            JARA = { 0x3D4D2B, 0x536639, 0x6A7F47, 0xECE8D6 }, RETAMA = { 0x626C2C, 0x818C38, 0x9FA94A, 0xE2C230 },
            HELECHO = { 0x3A5829, 0x4C6E35, 0x618645, 0x618645 }, JUNCO = { 0x3F5A2E, 0x557538, 0x6C8E46, 0x8A6E3C };
    private static final int HUECO_DE_MATA = 15, FILA_DE_MATA = 11;     // la rejilla torcida donde crecen (píxeles)

    /**
     * El matorral bajo, que crece solo entre la hierba: tomillo y retama en
     * el secano, jara y romero en el monte, helechos a la sombra y juncos
     * junto al agua. Cada mata es un cojín redondo visto desde arriba, con su
     * luz, su sombra debajo y, de día, sus flores.
     */
    private void matas(Punto p, int x, int y) {
        float cuantas = 0.04f + p.arbolado * 0.26f + p.pedregal * 0.10f + p.frescor * 0.10f - p.pisado * 0.5f;
        if (cuantas <= 0) return;
        int fila = Math.floorDiv(y, FILA_DE_MATA), desplazada = x + (Ruido.entero(fila, 0, 521) & 15);
        int hueco = Math.floorDiv(desplazada, HUECO_DE_MATA), azar = Ruido.entero(hueco, fila, 522);
        if ((azar & 255) / 255f >= cuantas) return;
        // Qué mata es, según dónde crece.
        float suerte = ((azar >> 8) & 255) / 255f;
        int[] mata = p.frescor > 0.62f ? JUNCO : p.arbolado > 0.45f ? (suerte < 0.5f ? HELECHO : JARA)
                : p.arbolado > 0.15f || p.pedregal > 0.3f ? (suerte < 0.45f ? JARA : suerte < 0.8f ? ROMERO : RETAMA)
                : (suerte < 0.5f ? TOMILLO : suerte < 0.75f ? RETAMA : ROMERO);
        // Su tamaño y dónde cae dentro de su hueco.
        float radioX = 3.4f + ((azar >> 16) & 3), radioY = radioX * 0.72f;
        float dx = Math.floorMod(desplazada, HUECO_DE_MATA) - 4 - ((azar >> 18) & 7), dy = Math.floorMod(y, FILA_DE_MATA) - 4 - ((azar >> 21) & 3);
        float redonda = dx * dx / (radioX * radioX) + dy * dy / (radioY * radioY) + (Colores.ruido(x, y, 523) - 0.5f) * 0.5f;
        if (redonda > 1) {
            // Justo debajo, su sombra en el suelo.
            if (dy > 0 && dx * dx / (radioX * radioX) + (dy - 1.5f) * (dy - 1.5f) / (radioY * radioY) < 1) {
                dia = Colores.brillo(dia, 0.78f) & 0xFFFFFF;
                noche = Colores.brillo(noche, 0.78f) & 0xFFFFFF;
            }
            return;
        }
        float luz = -(dx / radioX + dy / radioY * 1.4f);
        dia = luz > 0.45f ? mata[2] : luz < -0.55f ? mata[0] : mata[1];
        noche = deNoche(dia);
        if (luz > -0.2f && Colores.ruido(x, y, 524) > 0.86f) dia = mata[3];       // una flor
    }

    // ------------------------------------------------------------------
    // Los cortes del terreno (lo que se ve de frente al bajar un desnivel)
    // ------------------------------------------------------------------

    /**
     * El color de un punto del corte que baja desde el punto 'p' (el de
     * arriba): 'hondo' son los píxeles que lleva bajados y 'total', los que
     * baja el corte entero. De roca, si lo de arriba es roca; si no, un
     * ribazo de tierra con la hierba colgando y, a trechos, su pared de
     * piedra seca. 'aGrieta': el corte baja a una grieta, y se pierde en lo oscuro.
     */
    void corte(Punto p, int x, int y, int hondo, int total, boolean aGrieta) {
        int yDelCorte = y + hondo;
        if (p.clase == Terreno.GRIETA) {
            dia = noche = FONDO_DE_GRIETA;
            return;
        }
        if (p.clase == Terreno.AGUA && !p.piedra) {
            // Un salto de agua: blanco arriba, con hilos que caen.
            float hilo = Colores.ruido(x, hondo / 3, 511);
            dia = Colores.mezcla(0x6F9590, ESPUMA, hondo < 2 ? 0.9f : hilo * 0.7f) & 0xFFFFFF;
            noche = deNoche(dia);
            return;
        }
        boolean deRoca = p.clase == Terreno.ROCA || p.clase == Terreno.LANCHA || p.clase == Terreno.EMPEDRADO || p.clase == Terreno.LOSA || p.piedra;
        // La pared de piedra seca de un bancal: a trechos, donde el corte es de un nivel.
        boolean pared = !deRoca && !aGrieta && total <= 18 && Colores.ruidoSuave(x, y * 0.2f, 67, 512) > 0.52f;
        if (deRoca || aGrieta) {
            // La misma roca de arriba, vista de frente: más oscura, y más hacia el pie.
            roca(p, x, y + hondo * 2, 1f, 0);
            float sombra = 0.80f - 0.24f * hondo / Math.max(1, total);
            dia = Colores.brillo(dia, hondo == 0 && deRoca ? 1.12f : sombra) & 0xFFFFFF;
        } else if (pared) {
            // Cantos sin labrar, unos sobre otros, cada uno de su tono.
            celdas.buscar(x, yDelCorte * 1.4f, 6.5f, 1.5f, 1f, 0.05f, 513);
            float valor = celdas.borde < 0.7f ? 0.05f : 0.42f + (celdas.suerte - 0.5f) * 0.5f - (celdas.dx + celdas.dy) * 0.18f;
            dia = Colores.brillo(tono(rampaDeRoca(rocaDe(x, y)), valor, x, yDelCorte), celdas.borde < 0.7f ? 0.55f : 0.82f) & 0xFFFFFF;
            dia = Colores.mezcla(dia, 0x8C7650, 0.30f) & 0xFFFFFF;     // la pared coge el color de la tierra que sujeta
        } else {
            // Tierra apretada, más oscura hacia abajo, con alguna piedra.
            float valor = Colores.ruidoSuave(x, yDelCorte * 2.5f, 5, 515) * 0.5f + Colores.ruido(x, yDelCorte, 516) * 0.3f - 0.3f * hondo / Math.max(1, total);
            dia = Colores.brillo(tono(ROJIZA, valor, x, yDelCorte), 0.70f) & 0xFFFFFF;
            if (Colores.ruido(x / 3, yDelCorte / 2, 517) > 0.93f) dia = tono(GRANITO, 0.6f, x, yDelCorte);
        }
        if (!deRoca) {
            // La hierba (o lo que haya arriba) cuelga del canto a mechones desiguales.
            int cuelga = 1 + (int) (Colores.ruido(x / 2, y / 16, 518) * 3.4f);
            if (hondo < cuelga) dia = Colores.brillo(diaDeArriba, hondo == 0 ? 0.95f : 0.68f) & 0xFFFFFF;
            else if (hondo == cuelga) dia = Colores.brillo(dia, 0.6f) & 0xFFFFFF;
        }
        // El pie del corte, más oscuro: ahí se junta con el suelo de abajo. Y una grieta se traga la luz.
        if (hondo >= total - 1 && !aGrieta) dia = Colores.brillo(dia, 0.72f) & 0xFFFFFF;
        if (aGrieta) dia = Colores.mezcla(dia, FONDO_DE_GRIETA, Math.min(1, (hondo + 4) / (float) total)) & 0xFFFFFF;
        noche = deNoche(dia);
    }
}
