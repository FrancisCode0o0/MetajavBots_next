package castillafantasx.paisaje;

import castillafantasx.arte.Colores;
import castillafantasx.superficie.MapaSuperficie;

/**
 * HORNO — Pinta un Trozo del paisaje. Va columna a columna de píxeles y, en
 * cada una, recorre el suelo de norte a sur: cada punto del suelo se pinta
 * subido lo que mida su altura, y lo que queda más al sur tapa a lo de
 * detrás. Donde el suelo baja de golpe se ve el corte (la peña o el ribazo).
 *
 * La luz: cada punto se aclara o se oscurece según hacia dónde mira su
 * pendiente, y recibe la sombra del terreno más alto que tenga a levante
 * (por la mañana) o a poniente (por la tarde).
 */
final class Horno {

    private static final int TAM = MapaSuperficie.TAM, LADO = Trozo.LADO;
    // Cifras de dibujo, PROVISIONALES.
    private static final int CORTE_MINIMO = 5;              // píxeles que tiene que bajar el suelo de golpe para que se vea un corte
    private static final float LOMAS = 7f, LOMITAS = 2.2f;  // el relieve fingido del llano (solo da luz y sombra)
    private static final float LUZ_DE_LADERA = 0.085f;      // cuánto aclara u oscurece cada píxel de pendiente
    private static final float PENDIENTE_MAXIMA = 3f;
    private static final float SOMBRA_AL_PIE = 0.30f;       // lo que oscurece tener un corte pegado
    private static final float LUZ_DEL_CANTO = 1.14f, SOMBRA_DEL_CANTO = 0.60f;
    private static final float LARGO_DE_SOMBRA = 70f, PASO_DE_SOMBRA = 10f, CAIDA_DEL_SOL = 0.5f;   // la sombra de lo alto: alcance y pendiente del sol
    private static final float FUERZA_DE_SOMBRA = 0.46f;

    private final Terreno terreno;
    private final Formas formas;
    private final Pintor pintor;
    private Punto punto = new Punto(), anterior = new Punto();
    private float[] relieveDeAlLado = new float[0];        // la altura (con sus lomas) de cada punto de la columna anterior

    Horno(Terreno terreno) {
        this.terreno = terreno;
        formas = new Formas(terreno);
        pintor = new Pintor(terreno);
    }

    /** Pinta el trozo entero. */
    void hornear(Trozo t) {
        int x0 = t.columna * LADO, y0 = t.fila * LADO;
        // Hasta dónde hay que mirar hacia el sur: lo más alto que haya por debajo se pinta aquí arriba.
        float masAlto = 0;
        boolean hoyos = false;
        for (int f = Math.floorDiv(y0, TAM) - 2; f <= Math.floorDiv(y0 + LADO, TAM) + 8; f++) {
            for (int c = Math.floorDiv(x0, TAM) - 1; c <= Math.floorDiv(x0 + LADO, TAM) + 1; c++) {
                masAlto = Math.max(masAlto, terreno.bulto(c, f) + (terreno.clase(c, f) == Terreno.ROCA ? 24 : 0));
                if (terreno.clase(c, f) == Terreno.GRIETA) hoyos = true;
            }
        }
        int desde = y0 - (hoyos ? (int) Formas.HONDO_DE_GRIETA + 4 : 6), hasta = y0 + LADO + (int) masAlto + 2;
        if (relieveDeAlLado.length < hasta - desde + 1) relieveDeAlLado = new float[hasta - desde + 1];
        // La columna de antes de la primera: solo hace falta su altura.
        for (int y = desde; y <= hasta; y++) {
            formas.leer(x0 - 1, y, punto);
            relieveDeAlLado[y - desde] = punto.altura + lomas(x0 - 1, y);
        }
        for (int x = x0; x < x0 + LADO; x++) {
            columna(t, x, y0, desde, hasta);
        }
    }

    /** El relieve fingido del llano: lomas suaves que no se pisan; solo sirven para que la luz no sea plana. */
    private static float lomas(int x, int y) {
        return Ruido.suave(x, y, 5, 601) * LOMAS + Ruido.suave(x, y, 3, 602) * LOMITAS;
    }

    /** El bulto del terreno en un punto, sin escalones: entre los centros de las cuatro casillas de alrededor, se pasa de uno a otro poco a poco. */
    private float bultoSuave(float x, float y) {
        float tx = x / TAM - 0.5f, ty = y / TAM - 0.5f;
        int c = (int) Math.floor(tx), f = (int) Math.floor(ty);
        float ax = tx - c, ay = ty - f;
        float arriba = terreno.bulto(c, f) + (terreno.bulto(c + 1, f) - terreno.bulto(c, f)) * ax;
        float abajo = terreno.bulto(c, f + 1) + (terreno.bulto(c + 1, f + 1) - terreno.bulto(c, f + 1)) * ax;
        return arriba + (abajo - arriba) * ay;
    }

    /** De 0 a 1, cuánta sombra recibe un punto del terreno más alto que tiene hacia 'lado' (1 = a levante, -1 = a poniente). */
    private float sombra(int x, int y, float altura, int lado) {
        float sombra = 0;
        for (float d = PASO_DE_SOMBRA; d <= LARGO_DE_SOMBRA; d += PASO_DE_SOMBRA) {
            float tapa = bultoSuave(x + lado * d, y) - altura - d * CAIDA_DEL_SOL;
            if (tapa > 0) sombra = Math.max(sombra, Math.min(1, tapa / 10f));
        }
        return sombra;
    }

    /** Un color con su luz ('luz': 1 = tal cual) y su sombra (de 0 a 1), que es algo azulada. */
    private static int iluminado(int color, float luz, float sombra) {
        float queda = luz * (1 - FUERZA_DE_SOMBRA * sombra);
        return Colores.mult(color, queda, queda * (1 + 0.04f * sombra), queda * (1 + 0.16f * sombra)) & 0xFFFFFF;
    }

    private void columna(Trozo t, int x, int y0, int desde, int hasta) {
        int px = x - t.columna * LADO;
        int pintadoHasta = Integer.MIN_VALUE;       // la fila de pantalla del punto anterior
        float relieveDeArriba = 0;
        float luzM = 1, luzT = 1, luzN = 1, sombraM = 0, sombraT = 0;
        int alfa = 0;
        for (int y = desde; y <= hasta; y++) {
            Punto cambio = anterior;
            anterior = punto;
            punto = cambio;
            int diaDeArriba = pintor.dia;
            formas.leer(x, y, punto);
            float relieve = punto.altura + lomas(x, y);
            int fila = (int) Math.floor(y - punto.altura + 0.5f) - y0;

            // Lo que queda entre el punto anterior y este, si el suelo ha bajado de golpe: el corte.
            if (pintadoHasta != Integer.MIN_VALUE && fila > pintadoHasta + 1) {
                int total = fila - pintadoHasta - 1;
                for (int k = 0; k < total; k++) {
                    int f = pintadoHasta + 1 + k;
                    if (f < 0 || f >= LADO) continue;
                    if (total >= CORTE_MINIMO) {
                        pintor.diaDeArriba = diaDeArriba;
                        pintor.corte(anterior, x, y - 1, k, total, punto.clase == Terreno.GRIETA);
                    } else {
                        pintor.dia = Colores.brillo(diaDeArriba, 0.86f) & 0xFFFFFF;
                        pintor.noche = Pintor.deNoche(pintor.dia);
                    }
                    poner(t, px, f, alfa, pintor.dia, pintor.noche, 0.96f, 0.96f, luzN, sombraM, sombraT);
                }
            }

            // La luz de este punto: según su pendiente (a los lados y de frente) y lo que tenga encima.
            float alLado = Math.max(-PENDIENTE_MAXIMA, Math.min(PENDIENTE_MAXIMA, relieve - relieveDeAlLado[y - desde]));
            float deFrente = y == desde ? 0 : Math.max(-PENDIENTE_MAXIMA, Math.min(PENDIENTE_MAXIMA, relieve - relieveDeArriba));
            float pie = 1 - SOMBRA_AL_PIE * Math.min(1, punto.masAlto * 1.6f);
            // El canto de un corte que mira a poniente brilla; y al pie de uno que mira a levante, una raya oscura.
            float escalon = relieve - relieveDeAlLado[y - desde];
            if (escalon > CORTE_MINIMO) pie *= LUZ_DEL_CANTO;
            else if (escalon < -CORTE_MINIMO) pie *= SOMBRA_DEL_CANTO;
            // Lo que mira al sur (baja hacia nosotros) queda algo más oscuro que lo de arriba, como en las figuras.
            float frente = 1 + (deFrente < 0 ? 0.05f : 0.07f) * deFrente;
            luzM = pie * frente * (1 - LUZ_DE_LADERA * alLado);
            luzT = pie * frente * (1 + LUZ_DE_LADERA * alLado);
            luzN = pie * frente * (1 - LUZ_DE_LADERA * 0.5f * alLado);
            // La sombra de lo alto: solo si hay algo más alto cerca; y cambia tan poco a poco que basta mirarla un punto sí y otro no.
            if (terreno.bultoCerca(punto.columna, punto.fila) - punto.altura < 4) {
                sombraM = sombraT = 0;
            } else if ((y & 1) == 0 || y == desde) {
                sombraM = sombra(x, y, punto.altura, 1);
                sombraT = sombra(x, y, punto.altura, -1);
            }
            // De qué fila de casillas es este punto, si está en alto: la suya o, si es el borde de un terreno
            // más alto que se mete en una casilla más baja, la de ese terreno (así tapa a quien pasa por detrás).
            alfa = 0;
            if (punto.altura > 1 || punto.clase == Terreno.ROCA) {
                int suya = Math.floorDiv(y, TAM);
                boolean seMete = punto.altura > terreno.altura(Math.floorDiv(x, TAM), suya) + 4;
                alfa = Paisaje.marcaDeFila(seMete ? punto.fila : suya) << 24;
            }
            relieveDeAlLado[y - desde] = relieve;
            relieveDeArriba = relieve;

            pintor.pintar(punto, x, y);
            if (fila >= 0 && fila < LADO) poner(t, px, fila, alfa, pintor.dia, pintor.noche, luzM, luzT, luzN, sombraM, sombraT);
            pintadoHasta = fila;
        }
    }

    private static void poner(Trozo t, int px, int fila, int alfa, int dia, int noche, float luzM, float luzT, float luzN, float sombraM, float sombraT) {
        int i = fila * LADO + px;
        t.manana.px[i] = alfa | iluminado(dia, luzM, sombraM);
        t.tarde.px[i] = alfa | iluminado(dia, luzT, sombraT);
        t.noche.px[i] = alfa | iluminado(noche, luzN, 0);
    }
}
