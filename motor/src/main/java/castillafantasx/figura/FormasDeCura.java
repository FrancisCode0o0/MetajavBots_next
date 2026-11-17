package castillafantasx.figura;



/**
 * FORMAS DE CURA — El modelo de lo que el héroe come, bebe o se unta
 * para curarse (docs/encargos_viejos/ENCARGO_INTERFAZ.md), para el Escultor: la hogaza de
 * pan, la tira de cecina, el queso, la bota de vino, la escudilla del
 * remedio, el tarro del ungüento y la orza de miel; y las del encargo de la
 * supervivencia: la carne cruda, la pieza asada, el pescado, la redoma de una
 * poción y las plantas (ramita, hojas, flores y raíz), cada una con sus colores
 * (los de su archivo de datos). Son las mismas en la mano (cuerpo entero y
 * primera persona), en el suelo, creciendo y en los iconos del inventario.
 */
public final class FormasDeCura {

    private FormasDeCura() { }

    private static final Material CORTEZA = Material.rugoso(0x9A6A34), MIGA = Material.mate(0xD8C08A);
    private static final Material CECINA = Material.rugoso(0x6A2A22), TOCINO = Material.mate(0xB89878);
    private static final Material QUESO = Material.mate(0xD0B060), CORTEZA_DEL_QUESO = Material.rugoso(0x8A6A30);
    private static final Material PELLEJO = Material.cuero(0x4A3020), BROCAL = Material.mate(0xC8B48C);
    private static final Material BARRO = Material.rugoso(0x8A5A3A), BARRO_VIDRIADO = Material.metal(0x5E6E36);
    private static final Material COCIMIENTO = Material.mate(0x46582A), UNTO = Material.mate(0xC8C088), MIEL = Material.mate(0xD0921E);
    private static final Material HUESO = Material.mate(0xD8CCA8), VIDRIO = Material.metal(0x9AAAA8), CORCHO = Material.rugoso(0x8A6A40);
    private static final Material TALLO = Material.mate(0x4A5A30), BOTON = Material.mate(0xC8A030), OJO = Material.mate(0x1A1418);
    private static final int GAJOS = 12;      // las caras de una pieza torneada

    // Los materiales de cada color se hacen una vez y se reutilizan (cada clase de material, por su color).
    private static final java.util.Map<Long, Material> MATERIALES = new java.util.HashMap<>();
    private static final int MATE = 0, RUGOSO = 1, CUERO = 2;

    private static synchronized Material de(int clase, int rgb, Material siNoHay) {
        if (rgb < 0) return siNoHay;
        Long clave = ((long) clase << 32) | rgb;
        Material m = MATERIALES.get(clave);
        if (m == null) {
            m = clase == MATE ? Material.mate(rgb) : clase == RUGOSO ? Material.rugoso(rgb) : Material.cuero(rgb);
            MATERIALES.put(clave, m);
        }
        return m;
    }

    /** Como el otro, con los colores de lo que se toma ('tinte' y 'tinte2', RRGGBB; -1 = los de su forma). */
    public static void esculpir(Escultor e, Instante.Toma toma, int tinte, int tinte2, P3 mano, P3 eje, double g, double gastado) {
        P3 d = eje.unidad();
        P3 lado = perpendicular(d);
        switch (toma) {
            case CARNE_CRUDA: {
                // Un tajo de carne cruda, grueso y desigual, con su veta de grasa y un poco de hueso.
                Material carne = de(CUERO, tinte, Material.CARNE_VIVA), grasa = de(MATE, tinte2, TOCINO);
                double queda = 1 - 0.5 * gastado;
                e.bola(mano.menos(d.por(0.6 * g)), 1.25 * g, carne);
                e.hueso(mano.menos(d.por(0.6 * g)), mano.mas(d.por(1.4 * queda * g)).mas(lado.por(0.3 * g)), 1.1 * g, 0.8 * g, carne);
                e.hueso(mano.menos(d.por(1.4 * g)).mas(lado.por(0.6 * g)), mano.mas(d.por(0.9 * queda * g)).mas(lado.por(0.8 * g)), 0.35 * g, 0.3 * g, grasa);
                e.bola(mano.menos(d.por(1.8 * g)), 0.4 * g, HUESO);
                break;
            }
            case ASADO: {
                // Una pieza asada en su hueso: el hueso asoma por donde se coge y la carne, tostada, abulta en la punta.
                Material carne = de(RUGOSO, tinte, Material.rugoso(0x7A3A1E)), hueso = de(MATE, tinte2, HUESO);
                double queda = 1 - 0.5 * gastado;
                e.hueso(mano.menos(d.por(0.8 * g)), mano.mas(d.por(0.6 * g)), 0.35 * g, hueso);
                e.bola(mano.menos(d.por(0.9 * g)), 0.45 * g, hueso);
                e.hueso(mano.mas(d.por(0.6 * g)), mano.mas(d.por((0.6 + 2.0 * queda) * g)), 0.9 * g, 1.3 * g, carne);
                e.bola(mano.mas(d.por((0.6 + 2.0 * queda) * g)), 1.2 * g, carne);
                break;
            }
            case PESCADO: {
                // Una trucha cogida por la cola: el cuerpo ahusado, el vientre claro, la aleta de la cola y el ojo.
                Material piel = de(RUGOSO, tinte, Material.rugoso(0x7A8A7A)), vientre = de(MATE, tinte2, MIGA);
                e.hueso(mano, mano.mas(d.por(1.6 * g)), 0.35 * g, 0.9 * g, piel);
                e.hueso(mano.mas(d.por(1.6 * g)), mano.mas(d.por(3.4 * g)), 0.9 * g, 0.45 * g, piel);
                e.hueso(mano.mas(d.por(1.4 * g)).mas(lado.por(0.4 * g)), mano.mas(d.por(2.8 * g)).mas(lado.por(0.35 * g)), 0.4 * g, 0.3 * g, vientre);
                e.lamina(mano.mas(lado.por(1.0 * g)).menos(d.por(0.7 * g)), mano.menos(lado.por(1.0 * g)).menos(d.por(0.7 * g)),
                        mano.menos(lado.por(0.2 * g)), mano.mas(lado.por(0.2 * g)), piel);
                e.bola(mano.mas(d.por(3.0 * g)).mas(lado.por(0.4 * g)), 0.15 * g, OJO);
                break;
            }
            case REDOMA:
                // La redoma de vidrio, panzuda y de cuello largo, con la poción dentro y su tapón de corcho.
                torno(e, mano.menos(d.por(1.0 * g)), d, g, new double[] { 0, 0.3, 0.9, 1.4, 1.7, 2.6 }, new double[] { 0.7, 1.2, 1.3, 0.9, 0.4, 0.4 },
                        de(MATE, tinte, VIDRIO), CORCHO);
                e.bola(mano.mas(d.por(1.7 * g)), 0.42 * g, CORCHO);
                e.realce(mano.menos(d.por(0.5 * g)).mas(lado.por(0.8 * g)), mano.mas(d.por(0.2 * g)).mas(lado.por(0.9 * g)));
                break;
            case RAMITA: {
                // Un manojo de ramitas de hoja fina (romero, tomillo, espliego), con la espiga de flor en la punta.
                Material hoja = de(MATE, tinte, TALLO), flor = de(MATE, tinte2, MIGA);
                for (int i = -1; i <= 1; i++) {
                    P3 punta = mano.mas(d.por((2.6 - 0.4 * Math.abs(i)) * g)).mas(lado.por(0.7 * i * g));
                    e.hueso(mano.menos(d.por(0.6 * g)), punta, 0.18 * g, 0.12 * g, TALLO);
                    for (int k = 1; k <= 4; k++) {
                        P3 en = mano.mas(punta.menos(mano).por(k / 5.0));
                        e.hueso(en.mas(lado.por(0.45 * g)), en.menos(lado.por(0.45 * g)), 0.14 * g, 0.1 * g, hoja);
                    }
                    e.bola(punta, 0.32 * g, flor);
                }
                break;
            }
            case HOJAS: {
                // Un puñado de hojas anchas (salvia, llantén, ruda…), abiertas en abanico, con su nervio.
                Material hoja = de(MATE, tinte, COCIMIENTO);
                for (int i = -1; i <= 1; i++) {
                    P3 dir = d.mas(lado.por(0.5 * i)).unidad(), ancho = perpendicular(dir).por(0.7 * g);
                    P3 medio = mano.mas(dir.por(1.6 * g)), punta = mano.mas(dir.por(3.0 * g));
                    e.lamina(mano.mas(dir.por(0.3 * g)), medio.mas(ancho), punta, medio.menos(ancho), hoja);
                    e.surco(mano.mas(dir.por(0.4 * g)), punta);
                }
                if (tinte2 >= 0) e.bola(mano.mas(d.por(0.4 * g)), 0.3 * g, de(MATE, tinte2, hoja));
                break;
            }
            case FLORES: {
                // Unos tallos con sus flores (manzanilla, hipérico, beleño, adormidera): la corola y el botón de en medio.
                Material hoja = de(MATE, tinte, TALLO), flor = de(MATE, tinte2, MIGA);
                for (int i = -1; i <= 1; i++) {
                    P3 punta = mano.mas(d.por((2.8 - 0.5 * Math.abs(i)) * g)).mas(lado.por(0.8 * i * g));
                    P3 nudo = mano.mas(punta.menos(mano).por(0.4));
                    e.hueso(mano.menos(d.por(0.5 * g)), punta, 0.16 * g, 0.12 * g, hoja);
                    e.hueso(nudo, nudo.mas(lado.por(0.6 * g)), 0.2 * g, 0.1 * g, hoja);
                    e.bola(punta, 0.6 * g, flor);
                    e.bola(punta.mas(d.por(0.25 * g)), 0.22 * g, BOTON);
                }
                break;
            }
            case RAIZ: {
                // La raíz partida en dos, como unas piernas (la mandrágora), con su penacho de hojas arriba.
                Material raiz = de(RUGOSO, tinte2, CORTEZA), hoja = de(MATE, tinte, TALLO);
                P3 cuello = mano.mas(d.por(0.6 * g)), horcadura = mano.menos(d.por(0.8 * g));
                e.hueso(cuello, horcadura, 0.7 * g, 0.6 * g, raiz);
                e.hueso(horcadura, mano.menos(d.por(2.4 * g)).mas(lado.por(0.6 * g)), 0.45 * g, 0.12 * g, raiz);
                e.hueso(horcadura, mano.menos(d.por(2.3 * g)).menos(lado.por(0.6 * g)), 0.45 * g, 0.12 * g, raiz);
                for (int i = -1; i <= 1; i++) {
                    P3 dir = d.mas(lado.por(0.7 * i)).unidad(), ancho = perpendicular(dir).por(0.5 * g);
                    P3 medio = cuello.mas(dir.por(1.0 * g)), punta = cuello.mas(dir.por(2.0 * g));
                    e.lamina(cuello, medio.mas(ancho), punta, medio.menos(ancho), hoja);
                }
                break;
            }
            case CUENCO:
                // La escudilla de barro, con lo que lleve dentro (el remedio, la olla, las gachas…).
                torno(e, mano.menos(d.por(0.5 * g)), d, g, new double[] { 0, 0.15, 0.6, 1.0 }, new double[] { 0.7, 0.9, 1.5, 1.7 }, BARRO, de(MATE, tinte, COCIMIENTO));
                break;
            case TARRO:
            case ORZA:
                torno(e, mano.menos(d.por(0.9 * g)), d, g, new double[] { 0, 0.3, 1.0, 1.6, 1.9, 2.1 }, new double[] { 0.8, 1.15, 1.3, 1.1, 0.95, 1.05 },
                        toma == Instante.Toma.ORZA ? BARRO_VIDRIADO : BARRO, de(MATE, tinte, toma == Instante.Toma.ORZA ? MIEL : UNTO));
                break;
            default:
                esculpir(e, toma, mano, eje, g, gastado);
                break;
        }
    }

    /**
     * Esculpe una cura. 'mano': donde la coge la mano. 'eje': hacia dónde
     * apunta su boca (o su parte de arriba). 'g': su tamaño (1 = el de una
     * persona de cuerpo entero). 'gastado': de 0 a 1, lo que lleva comido.
     */
    public static void esculpir(Escultor e, Instante.Toma toma, P3 mano, P3 eje, double g, double gastado) {
        P3 d = eje.unidad();
        switch (toma) {
            case PAN:
                // Una hogaza pequeña, redonda y algo aplastada, con la greña (el corte de arriba) más clara.
                torno(e, mano.menos(d.por(0.7 * g)), d, g, new double[] { 0, 0.3, 0.8, 1.2, 1.45 }, new double[] { 1.0, 1.6, 1.7, 1.3, 0.7 }, CORTEZA, MIGA);
                break;
            case CECINA: {
                // Una tira de carne seca, larga y algo retorcida, con su veta de grasa.
                P3 lado = perpendicular(d);
                e.hueso(mano.menos(d.por(1.4 * g)), mano.mas(d.por(1.0 * g)), 0.75 * g, 0.7 * g, CECINA);
                e.hueso(mano.mas(d.por(1.0 * g)), mano.mas(d.por((3.2 - 1.4 * gastado) * g)).mas(lado.por(0.4 * g)), 0.7 * g, 0.5 * g, CECINA);
                e.realce(mano.menos(d.por(1.0 * g)).mas(lado.por(0.4 * g)), mano.mas(d.por(1.4 * g)).mas(lado.por(0.5 * g)));
                e.bola(mano.menos(d.por(1.4 * g)), 0.5 * g, TOCINO);
                break;
            }
            case QUESO:
                // Un queso pequeño: la corteza oscura alrededor y la cara de arriba, clara.
                torno(e, mano.menos(d.por(0.5 * g)), d, g, new double[] { 0, 0.15, 0.95, 1.1 }, new double[] { 1.3, 1.5, 1.5, 1.3 }, CORTEZA_DEL_QUESO, QUESO);
                break;
            case BOTA: {
                // La bota de vino: un pellejo en forma de pera, cosido, con su brocal de cuerno.
                P3 lado = perpendicular(d);
                e.bola(mano.menos(d.por(1.6 * g)), 1.45 * g, PELLEJO);
                e.hueso(mano.menos(d.por(1.6 * g)), mano.mas(d.por(0.9 * g)), 1.35 * g, 0.55 * g, PELLEJO);
                e.hueso(mano.mas(d.por(0.9 * g)), mano.mas(d.por(1.7 * g)), 0.45 * g, 0.32 * g, BROCAL);
                e.surco(mano.menos(d.por(2.8 * g)).mas(lado.por(0.3 * g)), mano.mas(d.por(0.5 * g)).mas(lado.por(0.3 * g)));
                break;
            }
            case CUENCO:
                // La escudilla de barro, baja y ancha, con el cocimiento de hierbas dentro.
                torno(e, mano.menos(d.por(0.5 * g)), d, g, new double[] { 0, 0.15, 0.6, 1.0 }, new double[] { 0.7, 0.9, 1.5, 1.7 }, BARRO, COCIMIENTO);
                break;
            case TARRO:
            case ORZA:
            default:
                // Un tarro de barro panzudo y de boca ancha (el del ungüento, sin vidriar; la orza de miel, vidriada), lleno hasta arriba.
                torno(e, mano.menos(d.por(0.9 * g)), d, g, new double[] { 0, 0.3, 1.0, 1.6, 1.9, 2.1 }, new double[] { 0.8, 1.15, 1.3, 1.1, 0.95, 1.05 },
                        toma == Instante.Toma.ORZA ? BARRO_VIDRIADO : BARRO, toma == Instante.Toma.ORZA ? MIEL : UNTO);
                break;
        }
    }

    /** Una flecha de largo 1 perpendicular a 'd'. */
    private static P3 perpendicular(P3 d) {
        P3 lado = d.cruz(new P3(0, 1, 0.3));
        return lado.largo() < 0.1 ? new P3(1, 0, 0) : lado.unidad();
    }

    /**
     * Una pieza torneada (un cacharro, una hogaza, un queso): sale de 'base'
     * y sube por 'eje'; a cada altura tiene su radio. La pared es del
     * material 'pared' y la cara de arriba, plana, de 'tapa'.
     */
    private static void torno(Escultor e, P3 base, P3 eje, double g, double[] alturas, double[] radios, Material pared, Material tapa) {
        P3 u = perpendicular(eje), v = eje.cruz(u).unidad();
        for (int k = 0; k + 1 < alturas.length; k++) {
            P3 abajo = base.mas(eje.por(alturas[k] * g)), arriba = base.mas(eje.por(alturas[k + 1] * g));
            for (int i = 0; i < GAJOS; i++) {
                double a0 = i * 2 * Math.PI / GAJOS, a1 = (i + 1) * 2 * Math.PI / GAJOS;
                P3 r0 = u.por(Math.cos(a0)).mas(v.por(Math.sin(a0))), r1 = u.por(Math.cos(a1)).mas(v.por(Math.sin(a1)));
                e.lamina(arriba.mas(r0.por(radios[k + 1] * g)), arriba.mas(r1.por(radios[k + 1] * g)),
                        abajo.mas(r1.por(radios[k] * g)), abajo.mas(r0.por(radios[k] * g)), pared);
            }
        }
        // El fondo y la cara de arriba.
        int ultima = alturas.length - 1;
        e.rodela(base, u.por(radios[0] * g), v.por(radios[0] * g), pared);
        e.rodela(base.mas(eje.por(alturas[ultima] * g)), u.por(radios[ultima] * g), v.por(radios[ultima] * g), tapa);
    }
}
