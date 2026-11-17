package castillafantasx.maniqui;

import com.badlogic.gdx.math.Vector3;

/**
 * ALCANCE — Lleva la mano de un maniquí a un sitio moviendo solo las articulaciones
 * de ese brazo (hombro, codo, antebrazo y muñeca), cada una dentro de sus topes
 * (res/datos/articulaciones.txt). Va tanteando: prueba a girar un poco cada
 * articulación y se queda con lo que acerca la mano; luego con giros más pequeños.
 * Así salen las posturas del juego y la izquierda va sola al asta (encargos/ENCARGO_BRAZOS.md, remate).
 */
public final class Alcance {

    private Alcance() { }

    private static final String[] LADO = { "_d", "_i" };
    private static final String[] DEL_BRAZO = { "hombro%s.flexion", "hombro%s.abduccion", "hombro%s.rotacion", "codo%s.flexion",
            "antebrazo%s.pronacion", "muneca%s.flexion", "muneca%s.desviacion" };
    // De dónde se parte: los brazos caídos y tres posturas corrientes (ángulos en el orden de DEL_BRAZO).
    private static final double[][] SALIDAS = { { 0, 0, 0, 0, 0, 0, 0 }, { 30, 10, 0, 90, 0, 0, 0 }, { 80, 20, 20, 60, 40, 0, 0 }, { -10, 15, 0, 110, -40, 0, 0 } };
    private static final double[] PASOS = { 24, 12, 6, 3, 1.5, 0.7, 0.3 };
    private static final int VUELTAS_POR_PASO = 6;
    // Lo que pesa cada cosa al comparar dos posturas: que el mango apunte bien cuenta como unos centímetros de mano,
    // y entre dos que llegan igual gana la más cómoda (el codo bajo y la muñeca poco torcida).
    private static final double PESO_DEL_MANGO = 0.006, PESO_DE_LA_COMODIDAD = 0.00012;

    /**
     * Lleva el puño de ese lado a 'punio' (metros, en las medidas del cuerpo) y, si se da, con el mango
     * apuntando hacia 'mango'. Deja puestos los ángulos y los huesos (falta posar() para la piel).
     * Devuelve lo lejos que se queda la mano (metros): cerca de 0 si llega.
     */
    public static double llevar(Maniqui m, int lado, Vector3 punio, Vector3 mango) {
        Maniqui.Giro[] giros = new Maniqui.Giro[DEL_BRAZO.length];
        for (int i = 0; i < giros.length; i++) {
            giros[i] = m.giro(String.format(DEL_BRAZO[i], LADO[lado]));
            if (giros[i] == null) return Double.MAX_VALUE;      // este cuerpo no tiene ese brazo
        }
        Vector3 haciaElMango = mango == null ? null : new Vector3(mango).nor();
        double[] mejor = new double[giros.length];
        double mejorCoste = Double.MAX_VALUE;
        for (double[] salida : SALIDAS) {
            for (int i = 0; i < giros.length; i++) {
                giros[i].poner(salida[i]);
            }
            double coste = tantear(m, lado, giros, punio, haciaElMango);
            if (coste < mejorCoste) {
                mejorCoste = coste;
                for (int i = 0; i < giros.length; i++) {
                    mejor[i] = giros[i].grados();
                }
            }
        }
        for (int i = 0; i < giros.length; i++) {
            giros[i].poner(mejor[i]);
        }
        m.posarHuesos();
        return m.agarre(lado).centro.dst(punio);
    }

    private static double tantear(Maniqui m, int lado, Maniqui.Giro[] giros, Vector3 punio, Vector3 mango) {
        double coste = coste(m, lado, giros, punio, mango);
        for (double paso : PASOS) {
            for (int vuelta = 0; vuelta < VUELTAS_POR_PASO; vuelta++) {
                boolean mejora = false;
                for (Maniqui.Giro g : giros) {
                    double antes = g.grados();
                    for (int sentido = -1; sentido <= 1; sentido += 2) {
                        g.poner(antes + sentido * paso);
                        if (g.grados() == antes) continue;          // está en el tope
                        double nuevo = coste(m, lado, giros, punio, mango);
                        if (nuevo < coste) {
                            coste = nuevo;
                            antes = g.grados();
                            mejora = true;
                        } else {
                            g.poner(antes);
                        }
                    }
                }
                if (!mejora) break;
            }
        }
        return coste;
    }

    private static double coste(Maniqui m, int lado, Maniqui.Giro[] giros, Vector3 punio, Vector3 mango) {
        m.posarHuesos();
        Maniqui.Agarre a = m.agarre(lado);
        double lejos = a.centro.dst(punio), coste = lejos * lejos;
        if (mango != null) coste += PESO_DEL_MANGO * (1 - a.mango.dot(mango));
        // La comodidad: el codo, poco abierto; la muñeca, poco torcida.
        double abre = giros[1].grados() / 90, gira = giros[2].grados() / 90, dobla = giros[5].grados() / 70, desvia = giros[6].grados() / 25;
        return coste + PESO_DE_LA_COMODIDAD * (abre * abre + 0.3 * gira * gira + dobla * dobla + desvia * desvia);
    }
}
