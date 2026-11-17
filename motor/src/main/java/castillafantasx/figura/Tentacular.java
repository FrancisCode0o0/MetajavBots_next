package castillafantasx.figura;



/**
 * TENTACULAR — Los horrores de la otra dimensión: un bulbo de carne con
 * tentáculos. El que se arrastra los apoya en el suelo y abre una boca; el que
 * flota los lleva colgando bajo un gran ojo.
 *
 * En calma los tentáculos se mecen despacio; en peligro se alzan y se
 * retuercen; al atacar, los de delante dan un latigazo.
 */
public final class Tentacular implements Figura {

    public boolean flota;
    public double bulbo = 6, tentaculo = 16;
    public int tentaculos = 7;
    public Material carne = Material.ICOR, punta = Material.BILIS, ojo = Material.OJO_BILIS;

    private static final double DOS_PI = Math.PI * 2;
    private final Instante deBestia = new Instante();     // para traducir los gestos que una bestia no tiene

    @Override
    public double talla() {
        return 1;
    }

    @Override
    public double alto() {
        return flota ? 22 + bulbo * 2 : bulbo * 2.4 + 8;
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
        m = m.paraBestia(deBestia);
        e.bajoElAgua(m.agua, m.mojado);        // si se mete en el agua, asoma de ella lo que esta deja
        double muere = m.caidaAlMorir(0.6);
        double chasquido = 0, encoge = 0;
        if (m.gesto == Gesto.ATACAR) {
            if (m.avance < 0.35) chasquido = -0.5 * Pose.suave(m.avance / 0.35);
            else if (m.avance < 0.6) chasquido = -0.5 + 1.5 * Pose.latigazo((m.avance - 0.35) / 0.25);
            else chasquido = 1 - Pose.suave((m.avance - 0.6) / 0.4);
        } else if (m.gesto == Gesto.GOLPE) {
            encoge = Math.sin(Math.PI * Math.min(1, m.avance));
        }
        double golpea = Math.max(0, chasquido);
        double vivo = m.alerta ? 1 : 0.35;
        double late = Math.sin(m.tiempo * (m.alerta ? 7 : 2.5));
        double alturaBase = flota ? 19 + Math.sin(m.tiempo * 2.2) * 1.5 : bulbo * 0.95;
        double altura = alturaBase * (1 - muere) + bulbo * 0.6 * muere;
        double r = bulbo * (1 + 0.05 * late - 0.12 * encoge) * (1 - 0.25 * muere);
        P3 c = new P3(0, golpea * (flota ? 5 : 3), altura);
        e.bola(c, r, carne);
        // Bultos de carne sobre el bulbo, para que no sea una bola lisa.
        for (int i = 0; i < 5; i++) {
            double a = i * 2.4 + 0.5;
            e.bola(c.mas(Math.cos(a) * r * 0.6, -Math.abs(Math.sin(a)) * r * 0.55, r * (0.45 + 0.15 * Math.sin(a * 3))), r * 0.42, carne);
        }

        Material mirada = m.alerta ? Material.OJO_ROJO : ojo;
        if (flota) {
            // Un gran ojo al frente, que se entrecierra en calma.
            e.bola(c.mas(0, r * 0.62, 0), r * 0.62, Material.HUESO);
            e.bola(c.mas(0, r * 1.08, 0), r * 0.33, mirada);
            e.bola(c.mas(0, r * 1.32, 0), r * 0.14, Material.NEGRO);
            if (!m.alerta) e.hueso(c.mas(-r * 0.6, r * 0.75, r * 0.45), c.mas(r * 0.6, r * 0.75, r * 0.45), r * 0.3, carne);
        } else {
            // Boca redonda con dientes, que se abre al atacar; ojos repartidos.
            double abre = 0.45 + 0.55 * golpea;
            e.bola(c.mas(0, r * 0.75, -r * 0.1), r * 0.5 * abre + 0.8, Material.FAUCES);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                e.bola(c.mas(Math.cos(a) * r * 0.5 * abre, r * 1.05, -r * 0.1 + Math.sin(a) * r * 0.5 * abre), 0.6, Material.DIENTE);
            }
            e.bola(c.mas(r * 0.55, r * 0.6, r * 0.5), 1.1, mirada);
            e.bola(c.mas(-r * 0.5, r * 0.55, r * 0.6), 0.9, mirada);
            e.bola(c.mas(0.5, r * 0.3, r * 0.92), 0.8, ojo);
        }

        // Tentáculos: cada uno, una ristra de tramos que se curva.
        int tramos = 6;
        for (int i = 0; i < tentaculos; i++) {
            double a = flota ? i * DOS_PI / tentaculos : -Math.PI * 0.15 + i * (Math.PI * 1.3) / (tentaculos - 1);
            double haciaX = Math.cos(a), haciaY = flota ? Math.sin(a) : Math.sin(a) * 0.9 + 0.2;
            boolean delantero = haciaY > 0.5;
            P3 antes = c.mas(haciaX * r * 0.7, haciaY * r * 0.7, -r * (flota ? 0.7 : 0.35));
            for (int k = 1; k <= tramos; k++) {
                double s = k / (double) tramos;
                double onda = Math.sin(m.tiempo * (m.alerta ? 6 : 1.8) + i * 1.7 - s * 3 + m.fase * DOS_PI) * vivo;
                P3 ahora;
                if (flota) {
                    // Cuelgan; en peligro se abren; al atacar los de delante azotan hacia el frente.
                    double abre = 0.25 + 0.5 * vivo * s;
                    double azote = delantero ? golpea : 0;
                    ahora = c.mas(haciaX * (r * 0.6 + tentaculo * abre * s * 0.5) + onda * 1.5 * s,
                            haciaY * (r * 0.6 + tentaculo * abre * s * 0.5) + azote * tentaculo * 0.8 * s,
                            -r * 0.6 - tentaculo * s * (1 - 0.6 * azote) * (1 - muere * 0.6));
                } else {
                    // Se apoyan en el suelo y la punta se alza; al atacar los de delante se lanzan.
                    double lejos = r * 0.6 + tentaculo * s * (1 - 0.3 * encoge);
                    double alza = (m.alerta ? 5 : 1.5) * s * s * (1 + onda * 0.6) * (1 - muere);
                    double golpe = delantero ? chasquido * tentaculo * 0.55 * s : 0;
                    ahora = new P3(haciaX * lejos + onda * 2.2 * s, c.y + haciaY * lejos + golpe,
                            Math.max(0.8, altura * 0.5 * (1 - s) * (1 - s) + alza + (delantero ? golpea * 6 * s : 0)));
                }
                double grosor = (flota ? 1.3 : 1.9) * (1.15 - s * 0.85);
                e.hueso(antes, ahora, grosor + 0.25, grosor, k == tramos ? punta : carne);
                antes = ahora;
            }
        }
    }
}
