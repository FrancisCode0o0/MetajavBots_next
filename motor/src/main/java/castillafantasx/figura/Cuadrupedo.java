package castillafantasx.figura;



/**
 * CUADRÚPEDO — Cualquier animal de cuatro patas, salvaje o de granja,
 * de monte o de tiro. Todos comparten esqueleto y manera de moverse;
 * lo que cambia son las medidas y los detalles, que se dan al crearlo.
 *
 * En calma anda despacio, con la cabeza baja. En peligro (Instante.alerta)
 * corre a saltos con la cabeza alta; al atacar se encoge, se lanza con la
 * boca abierta y se retrae. El que embiste de testa avisa bajando
 * la cabeza y escarbando, y ataca con un derrote: un golpe de cabeza hacia arriba.
 */
public final class Cuadrupedo implements Figura {

    public enum Orejas { NINGUNA, DE_PUNTA, CAIDAS, REDONDAS, LARGAS }

    public enum Arreos { NINGUNO, SILLA, ALBARDA, YUGO }

    // --- Medidas (píxeles de adulto) y detalles: se rellenan después de crearlo ---
    public double largo = 22, altoCruz = 15, altoGrupa = 14;      // del anca a la cruz; alto de hombros y de ancas
    public double pecho = 5, grupa = 4.2, pata = 1.5;             // radios
    public double cuello = 6, cuelloCalma = 0.1, cuelloAlerta = 0.7;   // largo y ángulo (0 = tendido, π/2 = vertical)
    public double cabeza = 3, hocico = 4, hocicoGrueso = 1.6;
    public double cola = 9, colaGruesa = 1.3, colaCalma = -0.9, colaAlerta = 0.2;
    public Orejas orejas = Orejas.DE_PUNTA;
    public Arreos arreos = Arreos.NINGUNO;
    public double cuernos = 0, joroba = 0;
    public boolean colmillos, crin, lomoErizado, dientes = true;
    public double ubre = 0;               // radio de la ubre (0 = no tiene)
    public boolean embisteDeTesta;        // ataca con los cuernos: avisa escarbando y da un derrote en vez de saltar a morder
    public Material puntaDeCola;          // si no es null: el último tramo de la cola es de este color
    public Material pelaje = Material.PELAJE_GRIS, morro = Material.NEGRO, pezuna = Material.NEGRO;
    public Material vientre, lomo;        // si no son null: la panza más clara y una lista más oscura por el lomo
    public Material ojoCalma = Material.NEGRO, ojoAlerta = Material.NEGRO, crinDe = Material.PELO_NEGRO;

    private static final double DOS_PI = Math.PI * 2;
    private static final double ESCARBADAS_POR_SEGUNDO = 1.8;     // PROVISIONAL: el ritmo con que escarba el que embiste al avisar
    private double tumbado;       // 0 = de pie; 1 = caído de costado
    private double ladoDeCaida = 1;   // 1 = cae sobre su costado derecho; -1, sobre el izquierdo

    @Override
    public double talla() {
        return 1;
    }

    @Override
    public double alto() {
        return altoCruz + cuello + cabeza + 6;
    }

    private final Instante deBestia = new Instante();     // para traducir los gestos que una bestia no tiene
    // Las partes, para el Escultor: donde una tapa a otra, las separa con una línea.
    private static final int TRONCO = 1, PRIMERA_PATA = 2, CABEZA = 6, COLA = 7, ARREOS = 8;

    @Override
    public P3 centroTendido(Instante instante) {
        Instante m = instante;
        // Cae de costado: el tronco queda a un lado de donde tenía las patas.
        return new P3((m.zurdo ? -1 : 1) * altoCruz * 0.7, 0, 0);
    }

    /** Al morir cae de costado: todo gira alrededor de la línea de las patas. */
    private P3 t(P3 p) {
        if (tumbado <= 0) return p;
        P3 v = p.ladeado(ladoDeCaida * tumbado * Math.PI / 2);
        return new P3(v.x, v.y, v.z + pecho * tumbado);
    }

    private P3 t(double x, double y, double z) {
        return t(new P3(x, y, z));
    }

    @Override
    public void esculpir(Escultor e, Instante instante) {
        Instante m = instante;
        e.figura(this, m);       // los retoques del editor de figuras, si los tiene
        e.bajoElAgua(m.agua, m.mojado);        // el que se baña en la orilla asoma del agua lo que esta deja
        m = m.paraBestia(deBestia);
        boolean corre = m.gesto == Gesto.CORRER || (m.alerta && m.gesto == Gesto.ANDAR);
        boolean semueve = m.gesto == Gesto.ANDAR || m.gesto == Gesto.CORRER;
        double adelante = 0, agacha = 0, boca = 0, alza = m.alerta ? cuelloAlerta : cuelloCalma;
        boolean escarba = false;
        tumbado = 0;

        switch (m.gesto) {
            case AVISAR:
                if (!embisteDeTesta) break;
                // El aviso del que embiste: la cabeza baja, los cuernos por delante, y una mano que escarba.
                alza = cuelloAlerta - 0.15;
                agacha = 0.05 * altoCruz;
                escarba = true;
                break;
            case ATACAR:
                if (embisteDeTesta) {
                    // El derrote: humilla, empuja y levanta la cabeza de golpe.
                    if (m.avance < 0.3) {
                        double s = Pose.suave(m.avance / 0.3);
                        adelante = -0.06 * largo * s;
                        alza = cuelloAlerta - 0.25 * s;
                    } else if (m.avance < 0.55) {
                        double s = Pose.latigazo((m.avance - 0.3) / 0.25);
                        adelante = largo * (-0.06 + 0.3 * s);
                        alza = cuelloAlerta - 0.25 + 1.35 * s;
                    } else {
                        double s = Pose.suave((m.avance - 0.55) / 0.45);
                        adelante = largo * 0.24 * (1 - s);
                        alza = cuelloAlerta + 1.1 * (1 - s);
                    }
                    break;
                }
                // Se encoge, salta con la boca abierta y se retrae.
                if (m.avance < 0.35) {
                    double s = Pose.suave(m.avance / 0.35);
                    adelante = -0.18 * largo * s;
                    agacha = 0.2 * altoCruz * s;
                } else if (m.avance < 0.6) {
                    double s = Pose.latigazo((m.avance - 0.35) / 0.25);
                    adelante = largo * (-0.18 + 0.55 * s);
                    agacha = 0.2 * altoCruz * (1 - s) - 0.08 * altoCruz * s;
                    boca = s;
                } else {
                    double s = Pose.suave((m.avance - 0.6) / 0.4);
                    adelante = largo * 0.37 * (1 - s);
                    boca = 1 - s;
                }
                alza = cuelloAlerta * 0.6;
                break;
            case GOLPE:
                adelante = -0.12 * largo * Math.sin(Math.PI * Math.min(1, m.avance));
                alza = cuelloAlerta + 0.3;
                boca = 0.6;
                break;
            case MORIR:
                tumbado = m.caidaAlMorir(0.6);
                ladoDeCaida = m.zurdo ? -1 : 1;
                alza = cuelloCalma - 0.3 * tumbado;
                boca = 0.4;
                break;
            default:
                // En calma, de vez en cuando baja la cabeza a husmear o a pastar.
                if (!m.alerta && !semueve) alza = cuelloCalma - 0.35 * Math.max(0, Math.sin(m.tiempo * 0.8));
                break;
        }

        // El lomo sube y baja con el paso; al correr, hombros y ancas se alternan.
        double vaiven = semueve ? Math.sin(DOS_PI * m.fase * (corre ? 1 : 2)) : Math.sin(m.tiempo * 2) * 0.15;
        double salto = corre ? 1.4 : 0.4;
        P3 anca = t(0, -largo / 2 + adelante, altoGrupa - agacha * 0.4 - vaiven * salto);
        P3 cruz = t(0, largo / 2 + adelante * 1.15, altoCruz - agacha + vaiven * salto);

        // Tronco.
        e.parte(TRONCO);
        e.hueso(anca, cruz, grupa, pecho, pelaje);
        // Las masas de la espalda y de la grupa: el tronco deja de ser un tubo.
        e.bola(cruz, pecho * 1.07, pelaje);
        e.bola(anca, grupa * 1.07, pelaje);
        e.bola(anca.hacia(cruz, 0.5).mas(t(0, 0, -pecho * 0.25).menos(t(0, 0, 0))), (grupa + pecho) / 2, pelaje);
        P3 bajoElTronco = t(0, 0, -1).menos(t(0, 0, 0));
        if (vientre != null) {
            e.hueso(anca.hacia(cruz, 0.15).mas(bajoElTronco.por(grupa * 0.45)), anca.hacia(cruz, 0.9).mas(bajoElTronco.por(pecho * 0.5)), grupa * 0.75, pecho * 0.75, vientre);
        }
        if (lomo != null) {
            e.hueso(anca.mas(bajoElTronco.por(-grupa * 0.5)), cruz.mas(bajoElTronco.por(-pecho * 0.5)), grupa * 0.62, pecho * 0.62, lomo);
        }
        if (joroba > 0) e.bola(cruz.mas(t(0, -pecho * 0.4, pecho * 0.5).menos(t(0, 0, 0))), joroba, pelaje);
        if (ubre > 0) e.bola(anca.hacia(cruz, 0.2).mas(bajoElTronco.por(grupa * 0.95)), ubre, Material.PIEL);
        if (lomoErizado && m.alerta) {
            for (int i = 0; i <= 4; i++) {
                e.bola(anca.hacia(cruz, 0.35 + i * 0.16).mas(0, 0, pecho * 0.95), 1.1, crinDe);
            }
        }

        // Patas: cada una con su momento del paso.
        double[] turno = corre ? new double[] { 0.50, 0.60, 0.00, 0.10 } : new double[] { 0.00, 0.50, 0.25, 0.75 };
        double zancada = !semueve ? 0 : (corre ? 0.50 : 0.28) * altoCruz;
        for (int i = 0; i < 4; i++) {
            boolean delantera = i >= 2;
            double lado = (i % 2 == 0) ? -1 : 1;
            double ancho = (delantera ? pecho : grupa) * 0.62;
            double y0 = (delantera ? largo / 2 - pecho * 0.3 : -largo / 2 + grupa * 0.2);
            double alto = delantera ? altoCruz : altoGrupa;
            double u = m.fase + turno[i];
            u -= Math.floor(u);
            double py = 0, pz = 0;
            if (zancada > 0) {
                if (u < 0.5) {
                    py = zancada * (1 - 4 * u);
                } else {
                    double s = (u - 0.5) * 2;
                    py = -zancada + 2 * zancada * Pose.suave(s);
                    pz = alto * (corre ? 0.30 : 0.16) * Math.sin(Math.PI * s);
                }
            }
            // En el salto del ataque las manos van por delante.
            if (delantera) py += adelante * 1.1 + boca * largo * 0.12;
            else py += adelante * 0.5;
            if (tumbado > 0) py += (delantera ? 1 : -1) * alto * 0.25 * tumbado;
            if (escarba && i == 2) {
                // La mano que escarba: sale adelante por el aire y vuelve arrastrando por el suelo.
                double vuelta = DOS_PI * m.tiempo * ESCARBADAS_POR_SEGUNDO;
                py += alto * 0.3 * Math.cos(vuelta);
                pz = alto * 0.14 * Math.max(0, Math.sin(vuelta));
            }
            P3 arranque = t(lado * ancho, y0 + adelante, alto - (delantera ? pecho : grupa) * 0.5 - agacha * (delantera ? 1 : 0.4));
            P3 pie = t(lado * ancho, y0 + py, pata + pz);
            double tramo = Math.max(alto * 0.53, arranque.menos(pie).largo() * 0.52);
            P3 rodilla = P3.doblar(arranque, pie, tramo, tramo, t(0, delantera ? 1 : -1, 0.3).menos(t(0, 0, 0)));
            // Cada pata es una parte: se separa del tronco y de las otras con una línea.
            // Arriba, el músculo del brazuelo o del muslo; después la articulación, la caña y la pezuña.
            e.parte(PRIMERA_PATA + i);
            e.musculo(arranque, rodilla, pata * 1.55, pata * 0.95, pata * 0.55, 0.22, pelaje);
            e.bola(rodilla, pata * 1.05, pelaje);
            e.hueso(rodilla, pie, pata * 0.95, pata * 0.7, pelaje);
            e.bola(pie, pata, pezuna);
            e.bola(pie.mas(t(0, pata * 0.55, -pata * 0.2).menos(t(0, 0, 0))), pata * 0.85, pezuna);
        }

        // Cuello y cabeza.
        e.parte(CABEZA);
        P3 arriba = t(0, 0, 1).menos(t(0, 0, 0)), frente = t(0, 1, 0).menos(t(0, 0, 0)), derecha = t(1, 0, 0).menos(t(0, 0, 0));
        P3 nuca = cruz.mas(frente.por(pecho * 0.45)).mas(arriba.por(pecho * 0.3));
        P3 crisma = nuca.mas(frente.por(Math.cos(alza) * cuello)).mas(arriba.por(Math.sin(alza) * cuello));
        e.hueso(nuca, crisma, pecho * 0.72, cabeza * 0.95, pelaje);
        e.bola(crisma, cabeza, pelaje);
        if (crin) {
            for (int i = 0; i <= 5; i++) {
                e.bola(nuca.hacia(crisma, i / 5.0).mas(arriba.por(pecho * 0.55)).mas(frente.por(-1)), 1.3, crinDe);
            }
        }
        double mira = alza * 0.4 - 0.35;                   // el hocico apunta algo hacia abajo
        P3 haciaElMorro = frente.por(Math.cos(mira)).mas(arriba.por(Math.sin(mira)));
        P3 punta = crisma.mas(haciaElMorro.por(cabeza * 0.6 + hocico));
        e.hueso(crisma, punta, cabeza * 0.8, hocicoGrueso, vientre != null ? vientre : pelaje);
        e.hueso(crisma.mas(arriba.por(cabeza * 0.3)), punta.mas(arriba.por(hocicoGrueso * 0.35)), cabeza * 0.7, hocicoGrueso * 0.8, pelaje);
        e.bola(punta, hocicoGrueso * 0.8, morro);
        // La mandíbula se abre girando hacia abajo.
        P3 haciaAbajo = haciaElMorro.por(Math.cos(boca * 0.7)).mas(arriba.por(-Math.sin(boca * 0.7) - 0.25));
        P3 menton = crisma.mas(arriba.por(-cabeza * 0.45));
        e.hueso(menton, menton.mas(haciaAbajo.por(cabeza * 0.4 + hocico * 0.9)), cabeza * 0.5, hocicoGrueso * 0.7, pelaje);
        if (boca > 0.2) {
            e.hueso(crisma.mas(haciaElMorro.por(cabeza * 0.6)), punta.mas(arriba.por(-hocicoGrueso)), hocicoGrueso * 0.7, Material.FAUCES);
            if (dientes) {
                e.bola(punta.mas(arriba.por(-hocicoGrueso * 1.1)).mas(derecha.por(hocicoGrueso * 0.6)), 0.55, Material.DIENTE);
                e.bola(punta.mas(arriba.por(-hocicoGrueso * 1.1)).mas(derecha.por(-hocicoGrueso * 0.6)), 0.55, Material.DIENTE);
            }
        }
        if (colmillos) {
            for (int lado = -1; lado <= 1; lado += 2) {
                P3 raiz = punta.mas(derecha.por(lado * hocicoGrueso * 0.9)).mas(haciaElMorro.por(-hocico * 0.3));
                e.hueso(raiz, raiz.mas(arriba.por(2.4)).mas(derecha.por(lado * 0.8)), 0.6, 0.2, Material.DIENTE);
            }
        }
        Material ojo = m.alerta ? ojoAlerta : ojoCalma;
        for (int lado = -1; lado <= 1; lado += 2) {
            e.bola(crisma.mas(derecha.por(lado * cabeza * 0.62)).mas(haciaElMorro.por(cabeza * 0.55)).mas(arriba.por(cabeza * 0.3)), m.alerta ? 0.75 : 0.55, ojo);
            P3 sien = crisma.mas(derecha.por(lado * cabeza * 0.7)).mas(arriba.por(cabeza * 0.6)).mas(frente.por(-cabeza * 0.2));
            switch (orejas) {
                case DE_PUNTA: e.hueso(sien, sien.mas(arriba.por(2.6)).mas(derecha.por(lado * 0.6)), 1.0, 0.25, pelaje); break;
                case LARGAS:   e.hueso(sien, sien.mas(arriba.por(5.0)).mas(derecha.por(lado * 1.2)), 1.0, 0.4, pelaje); break;
                case CAIDAS:   e.hueso(sien, sien.mas(arriba.por(-2.6)).mas(derecha.por(lado * 1.2)), 1.1, 0.7, pelaje); break;
                case REDONDAS: e.bola(sien.mas(arriba.por(0.6)), 1.3, pelaje); break;
                default: break;
            }
            if (cuernos > 0) {
                P3 codo = sien.mas(derecha.por(lado * cuernos * 0.6)).mas(arriba.por(cuernos * 0.2));
                e.hueso(sien, codo, 1.1, 0.8, Material.HUESO);
                e.hueso(codo, codo.mas(arriba.por(cuernos * 0.6)).mas(frente.por(cuernos * 0.3)), 0.8, 0.2, Material.HUESO);
            }
        }

        // Cola: cuelga en calma y se alza o se tensa en peligro; se menea sola.
        if (cola > 0) {
            e.parte(COLA);
            double sube = m.alerta ? colaAlerta : colaCalma;
            double menea = Math.sin(m.tiempo * (m.alerta ? 9 : 3)) * (m.alerta ? 0.5 : 0.25);
            P3 antes = anca.mas(frente.por(-grupa * 0.8)).mas(arriba.por(grupa * 0.3));
            for (int i = 1; i <= 3; i++) {
                double a = sube - i * 0.18;
                P3 ahora = antes.mas(frente.por(-Math.cos(a) * cola / 3)).mas(arriba.por(Math.sin(a) * cola / 3))
                        .mas(derecha.por(menea * i * cola / 9));
                Material deLaCola = i == 3 && puntaDeCola != null ? puntaDeCola : crin ? crinDe : pelaje;
                e.hueso(antes, ahora, colaGruesa * (1.15 - i * 0.2), colaGruesa * (0.95 - i * 0.2), deLaCola);
                antes = ahora;
            }
        }

        e.parte(ARREOS);
        arreos(e, anca, cruz, crisma, punta, arriba, frente, derecha);
        e.parte(0);
    }

    /** Silla y brida, albarda o yugo: los arreos de los de tiro y carga. */
    private void arreos(Escultor e, P3 anca, P3 cruz, P3 crisma, P3 punta, P3 arriba, P3 frente, P3 derecha) {
        P3 lomo = anca.hacia(cruz, 0.55).mas(arriba.por(pecho * 0.85));
        switch (arreos) {
            case SILLA:
                // Manta, silla de arzones altos, cincha, estribos y brida.
                for (int lado = -1; lado <= 1; lado += 2) {
                    P3 borde = lomo.mas(derecha.por(lado * pecho * 0.95)).mas(arriba.por(-pecho * 0.9));
                    e.lamina(lomo.mas(frente.por(5)), lomo.mas(frente.por(-6)), borde.mas(frente.por(-6)), borde.mas(frente.por(5)), Material.GRANATE);
                    e.hueso(borde, borde.mas(arriba.por(-pecho * 0.7)), 0.4, Material.CUERO_OSCURO);
                    e.bola(borde.mas(arriba.por(-pecho * 0.7)), 0.9, Material.HIERRO_VIEJO);
                }
                e.hueso(lomo.mas(frente.por(-3)), lomo.mas(frente.por(3)), 2.4, Material.CUERO);
                e.bola(lomo.mas(frente.por(4)).mas(arriba.por(1.5)), 1.5, Material.CUERO_OSCURO);
                e.bola(lomo.mas(frente.por(-4)).mas(arriba.por(1.8)), 1.8, Material.CUERO_OSCURO);
                e.hueso(punta.hacia(crisma, 0.3), cruz.mas(arriba.por(pecho)), 0.35, Material.CUERO_OSCURO);
                e.hueso(punta.hacia(crisma, 0.45).mas(arriba.por(hocicoGrueso)), punta.hacia(crisma, 0.45).mas(arriba.por(-hocicoGrueso)), 0.5, Material.CUERO_OSCURO);
                break;
            case ALBARDA:
                e.hueso(lomo.mas(frente.por(-4)), lomo.mas(frente.por(4)), 2.6, Material.PAJA);
                for (int lado = -1; lado <= 1; lado += 2) {
                    e.bola(lomo.mas(derecha.por(lado * pecho)).mas(arriba.por(-pecho * 0.5)), pecho * 0.62, Material.LINO);
                    e.hueso(lomo, lomo.mas(derecha.por(lado * pecho)).mas(arriba.por(-pecho * 0.2)), 0.5, Material.CUERO_OSCURO);
                }
                e.hueso(punta.hacia(crisma, 0.3), punta.hacia(crisma, 0.3).mas(arriba.por(-6)).mas(frente.por(2)), 0.35, Material.PAJA);
                break;
            case YUGO: {
                P3 testuz = crisma.mas(frente.por(-cabeza * 0.9)).mas(arriba.por(cabeza * 0.7));
                e.hueso(testuz.mas(derecha.por(-pecho * 1.1)), testuz.mas(derecha.por(pecho * 1.1)), 1.5, Material.MADERA);
                e.hueso(testuz, testuz.mas(arriba.por(-cabeza)), 0.5, Material.PAJA);
                break;
            }
            default:
                break;
        }
    }
}
