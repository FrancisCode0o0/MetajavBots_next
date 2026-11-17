package castillafantasx.figura;



/**
 * REPTANTE — Lo que se arrastra: un limaco, una sierpe (alada o de
 * agua) o un ser de cuerpo de sierpe y cabeza de gallo. El cuerpo es
 * una ristra de tramos que ondula; la parte de delante puede ir alzada.
 */
public final class Reptante implements Figura {

    public enum Cabeza { CON_ANTENAS, DE_SIERPE, DE_GALLO }

    // --- Medidas y detalles ---
    public double largo = 18, grueso = 4, alzado = 0, onda = 1.5;
    public int tramos = 8;
    public Cabeza cabeza = Cabeza.CON_ANTENAS;
    public double alas = 0;                   // envergadura de cada ala (0 = sin alas)
    public boolean patas, cresta;
    public Material piel = Material.BABA, vientre = Material.ESCAMA_VIENTRE, ala = Material.QUITINA;
    public Material ojoCalma = Material.NEGRO, ojoAlerta = Material.OJO_BILIS;

    private static final double DOS_PI = Math.PI * 2;
    private final Instante deBestia = new Instante();     // para traducir los gestos que una bestia no tiene

    @Override
    public double talla() {
        return 1;
    }

    @Override
    public double alto() {
        return Math.max(grueso * 2 + alzado + 8, alas * 0.8 + alzado);
    }

    @Override
    public P3 centroTendido(Instante instante) {
        Instante m = instante;
        return P3.CERO;
    }

    @Override
    public void esculpir(Escultor e, Instante instante) {
        Instante m = instante;
        e.figura(this, m);       // los retoques del editor de figuras, si los tiene
        e.bajoElAgua(m.agua, m.mojado);        // la que nada (la sierpe de agua) asoma del agua lo que esta deja
        m = m.paraBestia(deBestia);
        double embiste = 0, boca = 0, aplasta = 0;
        if (m.gesto == Gesto.ATACAR) {
            // Recoge el cuello, lo dispara y lo retrae.
            if (m.avance < 0.35) embiste = -0.4 * Pose.suave(m.avance / 0.35);
            else if (m.avance < 0.6) embiste = -0.4 + 1.4 * Pose.latigazo((m.avance - 0.35) / 0.25);
            else embiste = 1 - Pose.suave((m.avance - 0.6) / 0.4);
            boca = Math.max(0, embiste);
        } else if (m.gesto == Gesto.GOLPE) {
            embiste = -0.5 * Math.sin(Math.PI * Math.min(1, m.avance));
        } else if (m.gesto == Gesto.MORIR) {
            aplasta = m.caidaAlMorir(0.6);
        }
        double sube = (alzado * (m.alerta ? 1.25 : 0.8)) * (1 - aplasta);
        double ritmo = m.fase * 2 + m.tiempo * (m.alerta ? 1.2 : 0.35);
        double estira = cabeza == Cabeza.CON_ANTENAS ? 1 + 0.12 * Math.sin(DOS_PI * m.fase * 2) : 1;

        P3 antes = null, cuello = null, testa = null;
        double radioAntes = 0;
        for (int i = 0; i <= tramos; i++) {
            double s = i / (double) tramos;                 // 0 = cabeza, 1 = punta de la cola
            double delante = Math.max(0, 1 - s / 0.35);     // cuánto pertenece al cuello
            double x = Math.sin(DOS_PI * (s * onda - ritmo)) * grueso * 0.9 * (0.25 + s) * (1 - aplasta * 0.5);
            double y = largo * (0.5 - s) * estira + embiste * largo * 0.45 * delante;
            double z = grueso * (1 - aplasta * 0.5) + sube * delante * delante;
            double r = grueso * Math.min(1, 0.72 + 2.2 * s) * (1 - 0.82 * s) * (1 - aplasta * 0.35);
            P3 ahora = new P3(x, y, z);
            if (i > 0) e.hueso(antes, ahora, radioAntes, r, piel);
            if (cresta && i > 0 && i < tramos - 1) e.bola(ahora.mas(0, 0, r), r * 0.42, ala);
            if (i == 0) testa = ahora;
            if (i == 1) cuello = ahora;
            antes = ahora;
            radioAntes = r;
        }
        P3 frente = testa.menos(cuello).unidad();
        P3 derecha = frente.cruz(new P3(0, 0, 1)).unidad();
        P3 arriba = derecha.cruz(frente);
        Material ojo = m.alerta ? ojoAlerta : ojoCalma;

        switch (cabeza) {
            case CON_ANTENAS:
                // Dos cuernecillos con los ojos en la punta, que se encogen al golpearla.
                for (int lado = -1; lado <= 1; lado += 2) {
                    double largoCuerno = grueso * (m.gesto == Gesto.GOLPE ? 0.4 : 0.95 + 0.1 * Math.sin(m.tiempo * 3 + lado));
                    P3 punta = testa.mas(derecha.por(lado * grueso * 0.45)).mas(arriba.por(largoCuerno)).mas(frente.por(grueso * 0.5));
                    e.hueso(testa.mas(derecha.por(lado * grueso * 0.3)), punta, 0.6, 0.45, piel);
                    e.bola(punta, 0.75, ojo);
                }
                if (boca > 0.3) e.bola(testa.mas(frente.por(grueso * 0.6)), grueso * 0.45, Material.FAUCES);
                break;
            case DE_SIERPE: {
                double r = grueso * 0.95;
                e.bola(testa.mas(frente.por(r * 0.4)), r, piel);
                P3 morro = testa.mas(frente.por(r * 2.3)).mas(arriba.por(boca * r * 0.5));
                e.hueso(testa.mas(frente.por(r * 0.6)), morro, r * 0.8, r * 0.45, piel);
                P3 quijada = testa.mas(frente.por(r * 2.1)).mas(arriba.por(-r * (0.45 + boca * 1.1)));
                e.hueso(testa.mas(arriba.por(-r * 0.4)), quijada, r * 0.6, r * 0.35, vientre);
                if (boca > 0.2) {
                    e.hueso(testa.mas(frente.por(r)), morro.hacia(quijada, 0.5), r * 0.5, Material.FAUCES);
                    for (int lado = -1; lado <= 1; lado += 2) {
                        P3 raiz = morro.mas(derecha.por(lado * r * 0.3));
                        e.hueso(raiz, raiz.mas(arriba.por(-r * 0.8)), 0.6, 0.15, Material.DIENTE);
                    }
                }
                for (int lado = -1; lado <= 1; lado += 2) {
                    e.bola(testa.mas(derecha.por(lado * r * 0.7)).mas(frente.por(r * 0.9)).mas(arriba.por(r * 0.5)), 0.7, ojo);
                    P3 raiz = testa.mas(derecha.por(lado * r * 0.5)).mas(arriba.por(r * 0.7));
                    e.hueso(raiz, raiz.mas(frente.por(-r * 1.6)).mas(arriba.por(r * 0.6)).mas(derecha.por(lado * r * 0.4)), 0.8, 0.2, Material.HUESO);
                }
                break;
            }
            default: {
                // Cabeza de gallo: cresta y barbas rojas, pico y ojo que mata.
                double r = grueso * 0.9;
                e.bola(testa.mas(frente.por(r * 0.3)), r, piel);
                e.hueso(testa.mas(frente.por(r)), testa.mas(frente.por(r * 2.1)).mas(arriba.por(-r * 0.2 + boca * r * 0.3)), r * 0.45, 0.15, Material.AZAFRAN);
                if (boca > 0.3) e.hueso(testa.mas(frente.por(r)), testa.mas(frente.por(r * 1.8)).mas(arriba.por(-r * 0.8)), r * 0.35, 0.15, Material.AZAFRAN);
                for (int i = 0; i < 3; i++) {
                    e.bola(testa.mas(arriba.por(r * 1.05)).mas(frente.por(r * (0.6 - i * 0.6))), r * 0.5, Material.ROJO);
                }
                e.bola(testa.mas(frente.por(r * 0.9)).mas(arriba.por(-r * 0.9)), r * 0.45, Material.ROJO);
                for (int lado = -1; lado <= 1; lado += 2) {
                    e.bola(testa.mas(derecha.por(lado * r * 0.75)).mas(frente.por(r * 0.6)).mas(arriba.por(r * 0.3)), 0.7, ojo);
                }
                break;
            }
        }

        if (alas > 0) {
            // Alas de membrana: plegadas en calma, abiertas y batiendo en peligro.
            double abre = (m.alerta ? 0.25 + 0.55 * Math.sin(m.tiempo * 9) : 1.05 + 0.08 * Math.sin(m.tiempo * 2)) * (1 - aplasta) - 0.2 * aplasta;
            P3 raizDelAla = cuello.hacia(antes, 0.12).mas(0, 0, grueso * 0.6);
            for (int lado = -1; lado <= 1; lado += 2) {
                P3 codo = raizDelAla.mas(lado * Math.cos(abre) * alas * 0.5, -alas * 0.1, Math.sin(abre) * alas * 0.5);
                P3 punta = codo.mas(lado * Math.cos(abre * 0.5) * alas * 0.55, -alas * 0.25, Math.sin(abre * 0.5) * alas * 0.3);
                P3 zaga = raizDelAla.mas(0, -alas * 0.55, -grueso * 0.3);
                P3 pliegue = codo.hacia(zaga, 0.55).mas(lado * alas * 0.1, 0, 0);
                e.hueso(raizDelAla, codo, 0.9, 0.6, piel);
                e.hueso(codo, punta, 0.6, 0.25, piel);
                e.lamina(raizDelAla, codo, pliegue, zaga, ala);
                e.lamina(codo, punta, punta.hacia(zaga, 0.45), pliegue, ala);
            }
        }
        if (patas) {
            // Dos patas de gallo bajo el pecho.
            P3 pechuga = cuello.hacia(antes, 0.2);
            boolean anda = m.gesto == Gesto.ANDAR || m.gesto == Gesto.CORRER;
            for (int lado = -1; lado <= 1; lado += 2) {
                double u = m.fase + (lado > 0 ? 0.5 : 0);
                double paso = anda ? Math.sin(DOS_PI * u) * grueso * 0.8 : 0;
                P3 pie = new P3(pechuga.x + lado * grueso * 0.7, pechuga.y + paso, 0.6 + (anda ? Math.max(0, Math.cos(DOS_PI * u)) * 1.5 : 0));
                P3 rodilla = pechuga.hacia(pie, 0.5).mas(0, -grueso * 0.5, 0);
                e.hueso(pechuga, rodilla, 1.1, 0.7, piel);
                e.hueso(rodilla, pie, 0.7, 0.5, Material.AZAFRAN);
                e.hueso(pie, pie.mas(0, grueso * 0.6, -0.3), 0.5, 0.2, Material.AZAFRAN);
            }
        }
    }
}
