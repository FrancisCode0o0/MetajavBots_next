package metajavbots.combate;

import metajavbots.piezas.Equilibrio;
import metajavbots.piezas.Montaje;

/**
 * MOVIMIENTO — Cómo se mueve cada robot en un turno y qué pasa cuando chocan
 * (salió de Pelea). Las reglas de docs/EQUILIBRIO.md: el terreno que pisa
 * frena o ayuda según sus piernas; cuanto más rápido va, peor gira (Robocode);
 * frenar es el doble de rápido que acelerar; moverse gasta un poco de energía
 * (más si va sobrecargado); y en un choque se empuja según la masa de cada uno
 * y hace daño la masa por la velocidad del que embiste. En los minijuegos con
 * reglas propias solo cuenta el terreno sobre la locomoción.
 */
final class Movimiento {

    private Movimiento() {
    }

    /** Un turno de movimiento de c: energía, recargas, estados, giro, avance y paredes. */
    static void mover(Pelea pelea, Cuerpo c) {
        if (c.fuera) {
            c.velocidad *= 0.8;
            return;
        }
        Equilibrio eq = Equilibrio.delJuego();
        boolean reglas = ReglasDePeso.rigen(pelea);
        c.energia = Math.min(Cuerpo.energiaMaxSegura(c.cifras.energiaMax),
                c.energia + Cuerpo.recargaSegura(c.cifras.recarga * c.efectosPoo().factorRecarga));
        for (int i = 0; i < 3; i++) {
            if (c.enfriamiento[i] > 0 && --c.enfriamiento[i] == 0) c.recargarCicloSiListo(i);
        }
        c.pasoEstados(pelea.turno());
        if (c.sacudida > 0) c.sacudida *= 0.8;
        c.retroceso *= 0.75;
        boolean piernasRotas = c.parteRota(Montaje.PIERNAS);
        Equilibrio.Terreno terreno = c.vuela() && !c.cifras.vuela ? Equilibrio.Terreno.LLANO : pelea.arena.terrenoEn(c.x, c.y, c);
        double rinde = c.cifras.rasgos.rendimiento(terreno);
        double vBase = Cuerpo.velocidadSegura(c.cifras.velocidad);
        double vmax = vBase * rinde * (c.turbo > 0 ? Armas.factorSeguro(c.factorTurbo) : 1) * (piernasRotas ? 0.4 : 1);
        double gmax = Cuerpo.giroSeguro(c.cifras.giro) * (piernasRotas ? 0.5 : 1);
        if (reglas) gmax *= 1 - eq.giroPorVelocidad * Math.min(1, Math.abs(c.velocidad) / vBase);
        if (c.aturdido > 0) {
            vmax = 0;
            gmax = 0;
        }
        girar(c, gmax, reglas ? c.cifras.giroMira : 14);
        avanzar(pelea, c, vmax, rinde, eq);
        if (reglas) {
            double coste = Math.abs(c.velocidad) * eq.costeMovimiento * eq.factorEnergia(c.cifras.proporcion);
            c.energia = Math.max(0, c.energia - coste);
        }
        paredes(pelea, c);
    }

    /** Gira el cuerpo (hasta gmax) y la mira (hasta giroMira, que baja con el peso del arma). */
    private static void girar(Cuerpo c, double gmax, double giroMira) {
        double g = Math.max(-gmax, Math.min(gmax, c.giro));
        c.rumbo = Pelea.normal(c.rumbo + g);
        c.giro -= g;
        double tope = c.aturdido > 0 ? 0 : giroMira;
        double gm = Math.max(-tope, Math.min(tope, c.giroMira));
        c.mira = Pelea.normal(c.mira + gm);
        c.giroMira -= gm;
    }

    /** Acelera hacia lo que falta (frenar es el doble de rápido) y frena para llegar justo. */
    private static void avanzar(Pelea pelea, Cuerpo c, double vmax, double rinde, Equilibrio eq) {
        double deseada = Math.max(-vmax, Math.min(vmax, c.avance));
        boolean frena = Math.signum(deseada) != Math.signum(c.velocidad) || Math.abs(deseada) < Math.abs(c.velocidad);
        double acel = c.cifras.aceleracion * rinde * (frena ? eq.frenada : 1) * agarre(pelea, c);
        if (c.velocidad < deseada) c.velocidad = Math.min(deseada, c.velocidad + acel);
        else if (c.velocidad > deseada) c.velocidad = Math.max(deseada, c.velocidad - acel);
        double rad = Math.toRadians(c.rumbo);
        c.x += Math.cos(rad) * c.velocidad;
        c.y += Math.sin(rad) * c.velocidad;
        double avanceAntes = c.avance;
        c.avance -= c.velocidad;
        if ((avanceAntes > 0 && c.avance < 0) || (avanceAntes < 0 && c.avance > 0)) c.avance = 0;
        if (Math.abs(c.avance) < 0.01) c.avance = 0;
        c.paso += Math.abs(c.velocidad);
    }

    /** Lo que agarra el suelo: los que vuelan o flotan no lo notan; las orugas y las patas, menos. */
    private static double agarre(Pelea pelea, Cuerpo c) {
        if (c.vuela() || !c.cifras.rasgos.pisaSuelo) return 1;
        return Math.max(c.cifras.rasgos.traccion, pelea.arena.agarreEn(c.x, c.y));
    }

    private static void paredes(Pelea pelea, Cuerpo c) {
        boolean choca = false;
        if (c.x < c.radio) { c.x = c.radio; choca = true; }
        if (c.x > pelea.ancho - c.radio) { c.x = pelea.ancho - c.radio; choca = true; }
        if (c.y < c.radio) { c.y = c.radio; choca = true; }
        if (c.y > pelea.alto - c.radio) { c.y = pelea.alto - c.radio; choca = true; }
        if (choca && Math.abs(c.velocidad) > 0.5) {
            double dano = 0.3 + Math.abs(c.velocidad) * 0.25;
            c.chocoConPared = true;
            pelea.suceso(Suceso.Tipo.PARED, c.indice, c.x, c.y, c.rumbo, dano, "");
            pelea.danar(c, null, dano, "pared", c.rumbo, false);
            c.velocidad = 0;
            c.avance = 0;
        }
    }

    /** Dos robots que se tocan: se separan (según su masa y sus piernas) y, si van deprisa, se hacen daño. */
    static void chocar(Pelea pelea, Cuerpo a, Cuerpo b) {
        if (a.fuera || b.fuera) return;
        if (a.vuela() != b.vuela()) return;   // uno vuela por encima del otro
        double dx = b.x - a.x, dy = b.y - a.y;
        double d = Math.hypot(dx, dy), min = a.radio + b.radio;
        if (d >= min) return;
        if (d < 0.001) { dx = 1; dy = 0; d = 1; }
        double solape = min - d + 1;
        double ux = dx / d, uy = dy / d;
        double ma = 1, mb = 1;
        if (ReglasDePeso.rigen(pelea)) {
            ma = a.cifras.masa * a.cifras.rasgos.estabilidad;
            mb = b.cifras.masa * b.cifras.rasgos.estabilidad;
        }
        double parteA = mb / (ma + mb), parteB = ma / (ma + mb);
        a.x -= ux * solape * parteA;
        a.y -= uy * solape * parteA;
        b.x += ux * solape * parteB;
        b.y += uy * solape * parteB;
        double va = Math.abs(a.velocidad), vb = Math.abs(b.velocidad);
        double angA = Math.toDegrees(Math.atan2(dy, dx));
        if (va + vb > 1 && a.equipo != b.equipo) {
            pelea.suceso(Suceso.Tipo.CHOQUE, a.indice, (a.x + b.x) / 2, (a.y + b.y) / 2, angA, va + vb, "");
            pelea.danar(b, a, danoDeEmbestida(pelea, a, b, va), "choque", angA + 180, true);
            pelea.danar(a, b, danoDeEmbestida(pelea, b, a, vb), "choque", angA, true);
        }
        if (a.equipo != b.equipo) {
            a.chocoCon = new Avistamiento(a, b);
            b.chocoCon = new Avistamiento(b, a);
        }
        a.velocidad *= 0.3;
        b.velocidad *= 0.3;
    }

    /** Daño de una embestida: masa por velocidad del que embiste (las ruedas, más); en los minijuegos, la regla de siempre. */
    private static double danoDeEmbestida(Pelea pelea, Cuerpo de, Cuerpo a, double v) {
        if (!ReglasDePeso.rigen(pelea)) {
            double pd = de.combatiente.montaje.peso(), pa = a.combatiente.montaje.peso();
            return 0.3 + v * 0.5 * (pd / Math.max(10, pa));
        }
        Equilibrio eq = Equilibrio.delJuego();
        return 0.3 + eq.valor("choques", "dano_embestida", 0.5) * v * (de.cifras.masa / eq.masaReferencia) * de.cifras.rasgos.embestida;
    }
}
