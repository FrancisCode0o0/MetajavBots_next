package castillafantasx.paisaje;

import castillafantasx.superficie.MapaSuperficie;

/**
 * FORMAS — Convierte las casillas del mapa en formas naturales. A cada punto
 * del suelo le dice qué hay: para eso mira las cuatro casillas que lo rodean
 * (cada una pesa más cuanto más cerca tiene su centro: así las esquinas salen
 * redondas y las escaleras de casillas, en diagonal) y antes mueve el punto
 * un poco con un ruido suave (así ningún borde sale recto).
 *
 * Es el método de la «rejilla doble» de los juegos de casillas, hecho con
 * cuentas en vez de con dibujos de transición (fuentes en docs/ESTADO.md).
 */
final class Formas {

    private static final int TAM = MapaSuperficie.TAM;
    // Cifras de dibujo, PROVISIONALES.
    private static final float DESVIO_GRANDE = 13f, DESVIO_PEQUENO = 6f;    // píxeles que se mueve cada punto: en ondas largas y en cortas
    private static final int ONDA_GRANDE = 6, ONDA_PEQUENA = 4;             // lo que miden esas ondas (potencias de dos: 64 y 16 píxeles)
    private static final float GANA_LO_ALTO = 0.12f;        // el suelo más alto se come un poco del de al lado (así quien lo pisa nunca queda fuera)
    private static final float VAIVEN_DEL_AGUA = 0.30f;     // cuánto se ensancha y se estrecha un río
    private static final float VAIVEN_DEL_CAMINO = 0.75f;   // y un camino
    static final float AGUA_BAJO_LA_ORILLA = 4f;            // píxeles que queda el agua por debajo de su orilla
    static final float HONDO_DE_GRIETA = 48f;               // píxeles que baja una grieta
    private static final float SUBE_EL_BORDE = 17f, SUBE_POR_CASILLA = 12f, RISCOS = 12f;   // lo que levanta una peña
    private static final float ALTO_DE_PASADERA = 6f;

    private final Terreno terreno;
    // Las cuatro casillas de alrededor del último punto leído (se guardan: el punto siguiente suele caer entre las mismas).
    private int columna0 = Integer.MIN_VALUE, fila0;
    private final float[] peso = new float[4], alto = new float[4];
    private final int[] col = new int[4], fila = new int[4], clase = new int[4], dentro = new int[4];
    private final float[] frescor = new float[4], pedregal = new float[4], arbolado = new float[4], pisado = new float[4];
    private final boolean[] cuesta = new boolean[4];
    private float masAlta;
    private final float[] porClase = new float[Terreno.CLASES];

    Formas(Terreno terreno) {
        this.terreno = terreno;
    }

    /** Lo más que levanta una peña sobre su pie, según lo que se mete en ella la casilla (para saber cuánto ocupa su dibujo). */
    static float alzadoMaximoDeRoca(int dentro) {
        return SUBE_EL_BORDE + SUBE_POR_CASILLA * dentro + RISCOS;
    }

    /** Lo que levanta la peña en un punto: sube deprisa en el borde y luego poco a poco hacia dentro, con riscos. */
    private static float alzadoDeRoca(float roca, int x, int y) {
        float borde = Math.min(1, (roca - 0.5f) / 0.24f);
        borde = borde * borde * (3 - 2 * borde);
        float dentro = Math.max(0, roca - 0.9f);
        float riscos = (Ruido.suave(x, y, 5, 301) - 0.5f) * 2 * RISCOS * Math.min(1, dentro);
        return borde * SUBE_EL_BORDE + dentro * SUBE_POR_CASILLA + riscos;
    }

    /** Apunta lo que hace falta de las cuatro casillas cuya esquina común queda entre (c0, f0) y (c0 + 1, f0 + 1). */
    private void mirarCasillas(int c0, int f0) {
        columna0 = c0;
        fila0 = f0;
        masAlta = -1000;
        for (int i = 0; i < 4; i++) {
            int c = c0 + (i & 1), f = f0 + (i >> 1);
            col[i] = c;
            fila[i] = f;
            clase[i] = terreno.clase(c, f);
            alto[i] = terreno.altura(c, f);
            if (clase[i] == Terreno.AGUA) alto[i] -= AGUA_BAJO_LA_ORILLA;
            if (clase[i] == Terreno.GRIETA) alto[i] -= HONDO_DE_GRIETA;
            if (clase[i] != Terreno.ROCA) masAlta = Math.max(masAlta, alto[i]);
            dentro[i] = terreno.dentroDeRoca(c, f);
            frescor[i] = terreno.frescor(c, f);
            pedregal[i] = terreno.pedregal(c, f);
            arbolado[i] = terreno.arbolado(c, f);
            pisado[i] = terreno.pisado(c, f);
            cuesta[i] = terreno.mapa.esCuesta(c, f);
        }
    }

    /** Rellena 'p' con lo que hay en el punto (x, y) del suelo, en píxeles del mapa. */
    void leer(int x, int y, Punto p) {
        // 1. El punto se mueve un poco: ondas largas (los caminos serpentean) y cortas (los bordes se deshacen).
        float mx = x + (Ruido.suave(x, y, ONDA_GRANDE, 201) - 0.5f) * DESVIO_GRANDE + (Ruido.suave(x, y, ONDA_PEQUENA, 202) - 0.5f) * DESVIO_PEQUENO;
        float my = y + (Ruido.suave(x, y, ONDA_GRANDE, 203) - 0.5f) * DESVIO_GRANDE + (Ruido.suave(x, y, ONDA_PEQUENA, 204) - 0.5f) * DESVIO_PEQUENO;

        // 2. Las cuatro casillas de alrededor y lo que pesa cada una.
        float tx = mx / TAM - 0.5f, ty = my / TAM - 0.5f;
        int c0 = (int) Math.floor(tx), f0 = (int) Math.floor(ty);
        if (c0 != columna0 || f0 != fila0) mirarCasillas(c0, f0);
        float ax = tx - c0, ay = ty - f0;
        peso[0] = (1 - ax) * (1 - ay);
        peso[1] = ax * (1 - ay);
        peso[2] = (1 - ax) * ay;
        peso[3] = ax * ay;
        int mayor = 0;
        float pesoDeRoca = 0;
        p.roca = p.frescor = p.pedregal = p.arbolado = p.pisado = 0;
        for (int i = 0; i < 4; i++) {
            if (clase[i] == Terreno.ROCA) pesoDeRoca += peso[i];
            p.roca += peso[i] * dentro[i];
            p.frescor += peso[i] * frescor[i];
            p.pedregal += peso[i] * pedregal[i];
            p.arbolado += peso[i] * arbolado[i];
            p.pisado += peso[i] * pisado[i];
            if (peso[i] > peso[mayor]) mayor = i;
        }
        p.piedra = p.cuesta = false;
        p.rio = p.paso = false;
        p.orilla = p.hondura = p.masAlto = 0;
        p.margen = 1;

        // 3. ¿Es peña? Entonces sube desde su pie.
        if (p.roca > 0.5f) {
            float pie = 0;
            for (int i = 0; i < 4; i++) {
                if (clase[i] != Terreno.ROCA) continue;
                pie += peso[i] * alto[i];
                if (clase[mayor] != Terreno.ROCA || peso[i] > peso[mayor]) mayor = i;
            }
            p.clase = p.otra = Terreno.ROCA;
            p.altura = pie / pesoDeRoca + alzadoDeRoca(p.roca, x, y);
            p.columna = col[mayor];
            p.fila = fila[mayor];
            return;
        }

        // 4. Si no, gana la altura que más pesa entre las casillas que no son peña…
        int gana = -1;
        float mejor = -1, aguaAlLado = 0;
        for (int i = 0; i < 4; i++) {
            if (clase[i] == Terreno.ROCA) continue;
            if (clase[i] == Terreno.AGUA) aguaAlLado += peso[i];
            float suma = 0;
            for (int j = 0; j < 4; j++) {
                if (clase[j] != Terreno.ROCA && alto[j] == alto[i]) suma += peso[j];
            }
            suma /= 1 - Math.min(0.99f, pesoDeRoca);
            if (suma < 0.999f) {
                // Hay más de una altura: el agua se ensancha y se estrecha, y lo más alto gana un poco.
                if (clase[i] == Terreno.AGUA) suma += (Ruido.suave(x, y, 6, 205) - 0.5f) * VAIVEN_DEL_AGUA;
                else if (alto[i] == masAlta) suma += GANA_LO_ALTO;
            }
            if (suma > mejor) {
                mejor = suma;
                gana = i;
            }
        }
        // …y, de las casillas que están a esa altura, la que más pesa dice de qué casilla es el punto.
        for (int i = 0; i < 4; i++) {
            if (clase[i] != Terreno.ROCA && alto[i] == alto[gana] && peso[i] > peso[gana]) gana = i;
        }
        float altura = alto[gana];
        p.columna = col[gana];
        p.fila = fila[gana];
        p.cuesta = cuesta[gana];
        p.altura = p.cuesta ? terreno.alturaDeCuesta(col[gana], fila[gana], mx, my) : altura;
        for (int i = 0; i < 4; i++) {
            if (alto[i] > altura) p.masAlto += peso[i] * Math.min(3, (alto[i] - altura) / 16f);
        }
        p.masAlto += Math.max(0, p.roca - 0.15f) * 1.5f;

        // 5. La clase de suelo: la que más pesa entre las casillas de esa altura (el camino se ensancha y se estrecha).
        int primera = clase[gana], segunda = -1;
        boolean variasClases = false;
        for (int i = 0; i < 4; i++) {
            if (clase[i] != Terreno.ROCA && alto[i] == altura && clase[i] != primera) variasClases = true;
        }
        if (variasClases) {
            java.util.Arrays.fill(porClase, 0);
            float total = 0;
            for (int i = 0; i < 4; i++) {
                if (clase[i] == Terreno.ROCA || alto[i] != altura) continue;
                porClase[clase[i]] += peso[i];
                total += peso[i];
            }
            porClase[Terreno.TIERRA] *= 1 + (Ruido.suave(x, y, 7, 206) - 0.5f) * 2 * VAIVEN_DEL_CAMINO;
            for (int k = 0; k < Terreno.CLASES; k++) {
                if (porClase[k] > porClase[primera]) primera = k;
            }
            for (int k = 0; k < Terreno.CLASES; k++) {
                if (k != primera && porClase[k] > 0 && (segunda < 0 || porClase[k] > porClase[segunda])) segunda = k;
            }
            if (segunda >= 0) p.margen = Math.min(1, (porClase[primera] - porClase[segunda]) / total);
        }
        p.clase = primera;
        p.otra = segunda < 0 ? primera : segunda;

        if (p.clase == Terreno.AGUA) {
            // En el agua: lo lejos que queda la orilla, lo honda que es y hacia dónde corre.
            p.orilla = Math.min(1, Math.max(0, (mejor - 0.5f) / 0.5f));
            float cx = 0, cy = 0, suma = 0;
            for (int i = 0; i < 4; i++) {
                if (clase[i] != Terreno.AGUA) continue;
                p.hondura += peso[i] * terreno.hondura(col[i], fila[i]);
                cx += peso[i] * terreno.corrienteX(col[i], fila[i]);
                cy += peso[i] * terreno.corrienteY(col[i], fila[i]);
                suma += peso[i];
            }
            p.hondura /= suma;
            p.corrienteX = cx / suma;
            p.corrienteY = cy / suma;
            p.rio = terreno.esRio(col[gana], fila[gana]);
            p.paso = terreno.esPaso(col[gana], fila[gana]);
            // Una pasadera: una piedra redonda, algo torcida, que asoma en el centro de su casilla.
            if (terreno.esPasadera(col[gana], fila[gana])) {
                float dx = (x - (col[gana] + 0.5f) * TAM) / 11.5f, dy = (y - (fila[gana] + 0.5f) * TAM) / 8.5f;
                float redonda = dx * dx + dy * dy + (Ruido.suave(x, y, 3, 207) - 0.5f) * 0.5f;
                if (redonda < 1) {
                    p.piedra = true;
                    p.altura += AGUA_BAJO_LA_ORILLA + ALTO_DE_PASADERA * Math.min(1, (1 - redonda) * 3);
                }
            }
        } else {
            p.orilla = aguaAlLado;
        }
    }
}
