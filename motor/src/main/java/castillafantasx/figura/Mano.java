package castillafantasx.figura;



/**
 * MANO — Una mano con su palma, sus cuatro dedos y su pulgar. Es la misma en
 * el cuerpo entero y en los brazos de primera persona: abierta, a medio
 * cerrar, en puño o agarrando un mango (los dedos lo rodean).
 *
 * De lejos (la superficie) se dibuja resumida: palma, dedos juntos y pulgar.
 * De cerca, dedo a dedo, con nudillos y la raya entre cada dos.
 */
public final class Mano {

    private Mano() { }

    // Medidas de una mano de tamaño 1 (la del cuerpo entero), en píxeles de figura.
    public static final double LARGO_DE_LA_MUNECA = 1.5;         // de la muñeca al centro de la palma
    private static final double MEDIA_PALMA = 0.6, GRUESO_DE_PALMA = 0.95;
    private static final double SEPARA_DEDOS = 0.62, GRUESO_DE_DEDO = 0.37;
    private static final double FALANGE_1 = 1.15, FALANGE_2 = 1.0;
    private static final double[] LARGO_DE_CADA_DEDO = { 0.95, 1.08, 1.0, 0.82 };     // índice, corazón, anular y meñique
    private static final double DOBLA_NUDILLO = 1.45, DOBLA_DEDO = 3.0;               // radianes, con la mano cerrada del todo
    /** Desde este tamaño (los brazos de primera persona) se marcan también los nudillos y los tendones del dorso. */
    private static final double DE_MUY_CERCA = 1.5;

    /**
     * Pone la mano. 'centro': el centro de la palma (si agarra algo, por ahí
     * pasa el mango). 'antebrazo': flecha de largo 1 del codo a la mano.
     * 'dorso': hacia dónde mira el dorso. 'lado': 1 la derecha, -1 la izquierda.
     * 'tamano': 1 = la del cuerpo entero. 'cierre': de 0 (abierta) a 1 (puño).
     * 'mango': hacia dónde apunta lo que agarra (null = nada). 'fina': dedo a dedo.
     */
    public static void esculpir(Escultor e, P3 centro, P3 antebrazo, P3 dorso, int lado, double tamano, double cierre,
                         P3 mango, Material de, boolean fina) {
        double t = tamano;
        P3 d = antebrazo.unidad(), ancho;
        int pulgar;
        if (mango != null) {
            // Agarrando: los nudillos se alinean con el mango y el pulgar queda hacia la hoja.
            cierre = 1;
            ancho = mango.unidad();
            P3 recto = d.menos(ancho.por(d.punto(ancho)));
            // Si el antebrazo va en línea con el mango (una estocada), la mano se quiebra por la muñeca.
            if (recto.largo() < 0.35) recto = new P3(0, 0, 1).menos(ancho.por(ancho.z));
            if (recto.largo() < 0.35) recto = new P3(1, 0, 0).menos(ancho.por(ancho.x));
            d = recto.unidad();
            dorso = d.cruz(ancho);
            if (dorso.z < 0) dorso = dorso.por(-1);
            pulgar = 1;
        } else {
            P3 n = dorso.menos(d.por(dorso.punto(d)));
            dorso = n.largo() < 1e-6 ? d.cruz(new P3(0, 0, 1)).unidad() : n.unidad();
            ancho = d.cruz(dorso);
            pulgar = -lado;
        }
        P3 muneca = centro.menos(antebrazo.unidad().por(LARGO_DE_LA_MUNECA * t));

        if (!fina) {
            // Resumida: de lejos solo caben la palma, el bulto de los dedos y el pulgar.
            if (cierre > 0.8) {
                e.bola(centro, 1.45 * t, de);
                e.bola(centro.mas(ancho.por(pulgar * 0.9 * t)).mas(d.por(0.5 * t)), 0.7 * t, de);
                return;
            }
            double dobla = cierre * 1.2;
            P3 dedos = d.por(Math.cos(dobla)).menos(dorso.por(Math.sin(dobla)));
            e.hueso(muneca, centro, 1.1 * t, 1.2 * t, de);
            e.hueso(centro.mas(d.por(0.5 * t)), centro.mas(d.por(0.5 * t)).mas(dedos.por(1.6 * t)), 1.0 * t, 0.7 * t, de);
            e.bola(centro.mas(ancho.por(pulgar * 1.0 * t)).mas(d.por(0.2 * t)), 0.6 * t, de);
            return;
        }

        // La palma: de la muñeca a la línea de los nudillos, más ancha que gruesa.
        e.hueso(muneca, centro, 1.15 * t, GRUESO_DE_PALMA * t, de);
        e.hueso(centro.menos(ancho.por(MEDIA_PALMA * t)), centro.mas(ancho.por(MEDIA_PALMA * t)), GRUESO_DE_PALMA * t, de);

        // Los cuatro dedos: dos falanges que se doblan hacia la palma.
        double a1 = cierre * DOBLA_NUDILLO, a2 = cierre * DOBLA_DEDO;
        P3 dir1 = d.por(Math.cos(a1)).menos(dorso.por(Math.sin(a1)));
        P3 dir2 = d.por(Math.cos(a2)).menos(dorso.por(Math.sin(a2)));
        P3 nudilloAnterior = null, puntaAnterior = null;
        for (int i = 0; i < 4; i++) {
            // El índice es el más cercano al pulgar.
            double sitio = (1.5 - i) * pulgar * SEPARA_DEDOS * t;
            P3 nudillo = centro.mas(d.por(0.95 * t)).mas(ancho.por(sitio)).mas(dorso.por(0.25 * t));
            P3 medio = nudillo.mas(dir1.por(FALANGE_1 * LARGO_DE_CADA_DEDO[i] * t));
            P3 punta = medio.mas(dir2.por(FALANGE_2 * LARGO_DE_CADA_DEDO[i] * t));
            e.bola(nudillo, (GRUESO_DE_DEDO + (t > DE_MUY_CERCA ? 0.12 : 0.06)) * t, de);
            e.hueso(nudillo, medio, GRUESO_DE_DEDO * t, de);
            if (t > DE_MUY_CERCA) {
                // El tendón de cada dedo, del nudillo hacia la muñeca, y la luz del nudillo.
                P3 fueraDelDorso = dorso.por((GRUESO_DE_PALMA + 0.05) * t);
                e.realce(nudillo.mas(dorso.por(GRUESO_DE_DEDO * t)), nudillo.hacia(muneca, 0.75).mas(fueraDelDorso));
                e.surco(nudillo.mas(d.por(0.25 * t)).mas(dorso.por(GRUESO_DE_DEDO * 0.9 * t)).mas(ancho.por(-0.2 * t)),
                        nudillo.mas(d.por(0.25 * t)).mas(dorso.por(GRUESO_DE_DEDO * 0.9 * t)).mas(ancho.por(0.2 * t)));
            }
            e.hueso(medio, punta, GRUESO_DE_DEDO * t, (GRUESO_DE_DEDO - 0.06) * t, de);
            // La raya entre este dedo y el anterior, y el brillo del nudillo.
            if (nudilloAnterior != null) {
                P3 fuera = dorso.por(GRUESO_DE_DEDO * 0.6 * t);
                e.surco(nudillo.hacia(nudilloAnterior, 0.5).mas(fuera), medio.hacia(puntaAnterior, 0.5).mas(fuera));
            }
            nudilloAnterior = nudillo;
            puntaAnterior = medio;
        }

        // El pulgar: sale del lado de la palma; abierto se separa, cerrado cruza por encima de los dedos.
        P3 raiz = centro.menos(d.por(0.45 * t)).mas(ancho.por(pulgar * 0.95 * t));
        P3 abierto = d.por(0.55).mas(ancho.por(pulgar * 0.8)).unidad();
        P3 cerrado = d.por(0.85).menos(dorso.por(0.45)).mas(ancho.por(pulgar * 0.15)).unidad();
        P3 dirPulgar = abierto.hacia(cerrado, cierre).unidad();
        P3 medio = raiz.mas(dirPulgar.por(1.0 * t));
        P3 punta = medio.mas(dirPulgar.hacia(ancho.por(-pulgar), 0.45 * cierre).unidad().por(0.85 * t));
        e.hueso(raiz, medio, 0.5 * t, 0.42 * t, de);
        e.hueso(medio, punta, 0.42 * t, 0.36 * t, de);
    }
}
