package castillafantasx.figura;



/**
 * AVE — Lo que tiene alas y dos patas: la que vuela siempre (alas de piel) y la
 * que anda (picotea y aletea cuando se asusta).
 */
public final class Ave implements Figura {

    public boolean vuela = true;              // la de alas de piel: siempre en el aire
    public double cuerpo = 2.4, envergadura = 9, alturaDeVuelo = 15;
    public Material pluma = Material.PELAJE_NEGRO, ala = Material.QUITINA;

    private static final double DOS_PI = Math.PI * 2;
    private final Instante deBestia = new Instante();     // para traducir los gestos que una bestia no tiene

    @Override
    public double talla() {
        return 1;
    }

    @Override
    public double alto() {
        return vuela ? alturaDeVuelo + envergadura : cuerpo * 5;
    }

    @Override
    public P3 centroTendido(Instante instante) {
        Instante m = instante;
        return vuela ? P3.CERO : new P3(cuerpo, 0, 0);      // la que anda cae sobre un costado
    }

    @Override
    public void esculpir(Escultor e, Instante instante) {
        Instante m = instante;
        e.figura(this, m);       // los retoques del editor de figuras, si los tiene
        m = m.paraBestia(deBestia);
        if (vuela) volador(e, m);
        else andador(e, m);
    }

    private void volador(Escultor e, Instante m) {
        double cae = m.caidaAlMorir(0.5);
        double picado = 0;
        if (m.gesto == Gesto.ATACAR) {
            // Se alza un poco, cae en picado hacia delante y remonta.
            if (m.avance < 0.3) picado = -0.3 * Pose.suave(m.avance / 0.3);
            else if (m.avance < 0.55) picado = -0.3 + 1.3 * Pose.latigazo((m.avance - 0.3) / 0.25);
            else picado = 1 - Pose.suave((m.avance - 0.55) / 0.45);
        }
        double ritmo = m.alerta ? 15 : 8;
        double bate = cae > 0 ? -0.6 * cae : Math.sin(m.tiempo * ritmo) * (m.alerta ? 0.9 : 0.6);
        double z = (alturaDeVuelo + Math.sin(m.tiempo * 3) * 1.2 - bate * 1.2 - picado * alturaDeVuelo * 0.5) * (1 - cae) + cuerpo * cae;
        P3 c = new P3(0, picado * 7, z);
        e.bola(c, cuerpo, pluma);
        P3 testa = c.mas(0, cuerpo * 1.1, cuerpo * 0.3);
        e.bola(testa, cuerpo * 0.75, pluma);
        Material ojo = m.alerta ? Material.OJO_ROJO : Material.NEGRO;
        for (int lado = -1; lado <= 1; lado += 2) {
            e.hueso(testa.mas(lado * cuerpo * 0.4, 0, cuerpo * 0.5), testa.mas(lado * cuerpo * 0.7, -0.3, cuerpo * 1.5), 0.8, 0.2, pluma);
            e.bola(testa.mas(lado * cuerpo * 0.35, cuerpo * 0.6, cuerpo * 0.1), 0.5, ojo);
            // El ala: brazo, antebrazo y la membrana hasta el cuerpo.
            P3 hombro = c.mas(lado * cuerpo * 0.6, 0, cuerpo * 0.4);
            P3 codo = hombro.mas(lado * Math.cos(bate) * envergadura * 0.45, 0.5, Math.sin(bate) * envergadura * 0.45);
            P3 punta = codo.mas(lado * Math.cos(bate * 1.5) * envergadura * 0.55, -1, Math.sin(bate * 1.5) * envergadura * 0.55);
            P3 zaga = c.mas(0, -cuerpo * 1.6, -cuerpo * 0.3);
            P3 pliegue = codo.mas(0, -envergadura * 0.35, -0.5);
            e.hueso(hombro, codo, 0.7, 0.5, pluma);
            e.hueso(codo, punta, 0.5, 0.2, pluma);
            e.lamina(hombro, codo, pliegue, zaga, ala);
            e.lamina(codo, punta, punta.hacia(zaga, 0.3), pliegue, ala);
        }
        if (m.gesto == Gesto.ATACAR && picado > 0.3) {
            e.bola(testa.mas(0, cuerpo * 0.7, -cuerpo * 0.3), cuerpo * 0.4, Material.FAUCES);
            e.bola(testa.mas(cuerpo * 0.2, cuerpo * 0.8, -cuerpo * 0.5), 0.4, Material.DIENTE);
            e.bola(testa.mas(-cuerpo * 0.2, cuerpo * 0.8, -cuerpo * 0.5), 0.4, Material.DIENTE);
        }
    }

    private void andador(Escultor e, Instante m) {
        boolean anda = m.gesto == Gesto.ANDAR || m.gesto == Gesto.CORRER;
        double cae = m.caidaAlMorir(0.5);
        double r = cuerpo;
        double bote = anda ? Math.abs(Math.sin(DOS_PI * m.fase)) * 0.6 : 0;
        P3 c = new P3(cae * r, 0, (r * 1.9 + bote) * (1 - cae) + r * cae);
        e.bola(c, r, pluma);
        e.bola(c.mas(0, -r * 0.7, r * 0.2), r * 0.85, pluma);
        // La cola, en abanico hacia arriba.
        e.lamina(c.mas(-r * 0.5, -r, r * 0.3), c.mas(r * 0.5, -r, r * 0.3), c.mas(r * 0.7, -r * 2.0, r * 1.7), c.mas(-r * 0.7, -r * 2.0, r * 1.7), ala);
        // En calma picotea el suelo de vez en cuando; asustada estira el cuello.
        double pico = (!m.alerta && !anda) ? Math.max(0, Math.sin(m.tiempo * 2.5)) : 0;
        double cabecea = anda ? Math.sin(DOS_PI * m.fase * 2) * 0.5 : 0;
        P3 testa = c.mas(0, r * 1.3 + cabecea + pico * r * 0.8, r * 1.5 - pico * r * 2.4).mas(cae * r, 0, -cae * r * 1.2);
        e.hueso(c.mas(0, r * 0.6, r * 0.4), testa, r * 0.55, r * 0.4, pluma);
        e.bola(testa, r * 0.5, pluma);
        e.hueso(testa.mas(0, r * 0.4, 0), testa.mas(0, r * 1.0, -r * 0.15), 0.5, 0.15, Material.AZAFRAN);
        e.bola(testa.mas(0, 0, r * 0.55), r * 0.3, Material.ROJO);
        e.bola(testa.mas(0, r * 0.35, -r * 0.45), r * 0.22, Material.ROJO);
        e.bola(testa.mas(r * 0.35, r * 0.25, r * 0.1), 0.4, Material.NEGRO);
        e.bola(testa.mas(-r * 0.35, r * 0.25, r * 0.1), 0.4, Material.NEGRO);
        // Alas pegadas al cuerpo; asustada, aletea.
        double aletea = m.alerta ? Math.abs(Math.sin(m.tiempo * 18)) : 0;
        for (int lado = -1; lado <= 1; lado += 2) {
            e.hueso(c.mas(lado * r * 0.8, r * 0.2, r * 0.3), c.mas(lado * r * (0.9 + aletea), -r * 0.9, r * (0.1 + aletea * 1.2)), r * 0.5, r * 0.25, ala);
            double u = m.fase + (lado > 0 ? 0.5 : 0);
            double paso = anda ? Math.sin(DOS_PI * u) * r * 0.9 : 0;
            P3 pie = new P3(lado * r * 0.45 + cae * r * 2, paso, 0.4 + (anda ? Math.max(0, Math.cos(DOS_PI * u)) * 1.2 : 0));
            e.hueso(c.mas(lado * r * 0.4, 0, -r * 0.7), pie, 0.5, 0.4, Material.AZAFRAN);
            e.hueso(pie, pie.mas(0, r * 0.5, -0.2), 0.45, 0.2, Material.AZAFRAN);
        }
    }
}
