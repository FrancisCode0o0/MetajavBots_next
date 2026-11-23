package metajavbots.combate;

import metajavbots.piezas.Equilibrio;
import metajavbots.piezas.Pieza;

/**
 * REGLAS DE PESO — Lo que hacen en la pelea la masa, la locomoción y el peso de
 * cada arma (docs/EQUILIBRIO.md), con los números de res/datos/equilibrio.txt:
 * el empujón y el volteo según la masa de cada uno (como en las luchas de
 * robots de verdad), la esquiva de los voladores frente a los golpes, el daño
 * antiaéreo, el retroceso de las armas pesadas (salvo en orugas y patas, que lo
 * absorben) y la potencia regulable de Robocode. En los minijuegos con reglas
 * propias (Carrera de obstáculos, Destrucción y supervivencia) no se aplica:
 * allí solo cuenta el terreno sobre la locomoción (Movimiento).
 */
final class ReglasDePeso {

    private ReglasDePeso() {
    }

    private static Equilibrio eq() {
        return Equilibrio.delJuego();
    }

    /** ¿Rigen en esta pelea las reglas de masa y armas pesadas? */
    static boolean rigen(Pelea p) {
        return p != null && !p.reglasPropias;
    }

    /** El empujón de un golpe: masa del que pega frente a la del que lo recibe, la estabilidad de sus piernas y las cuñas. */
    static double empuje(Pelea p, Cuerpo de, Cuerpo a, double base) {
        if (!rigen(p) || base <= 0) return base;
        Equilibrio e = eq();
        double ratio = de.cifras.masa / Math.max(1, a.cifras.masa);
        double f = Math.max(e.valor("choques", "empuje_minimo", 0.5), Math.min(e.valor("choques", "empuje_maximo", 1.8), ratio));
        double cuna = base >= 40 ? a.cifras.rasgos.cuna : 1;
        return Armas.empujeSeguro(base * f * cuna / a.cifras.rasgos.estabilidad);
    }

    /** Los turnos de volteo: menos si el volteado pesa más o lleva orugas; los ligeros se recolocan antes. */
    static int volteo(Pelea p, Cuerpo de, Cuerpo a, int base) {
        if (!rigen(p) || base <= 0) return base;
        Equilibrio e = eq();
        double ratio = Math.max(0.6, Math.min(1.5, de.cifras.masa / Math.max(1, a.cifras.masa)));
        double t = base * ratio * (1 - a.cifras.rasgos.antivuelco) * a.cifras.rasgos.cuna;
        return t < e.valor("choques", "volteo_minimo", 4) ? 0 : Armas.aturdeSeguro(t);
    }

    /**
     * El daño de un golpe cuerpo a cuerpo en las peleas normales (×cuerpo_a_cuerpo de equilibrio.txt): compensa tener que
     * acercarse, así que no vale para las armas largas (látigos, bola con cadena), que pegan desde lejos.
     */
    static double golpe(Pelea p, Pieza arma, double dano) {
        if (!rigen(p) || arma != null && arma.valor("alcance") >= eq().valor("aereo", "alcance_cuerpo", 120)) return dano;
        return Armas.danoEfectivoSeguro(dano * eq().valor("armas", "cuerpo_a_cuerpo", 1));
    }

    /** ¿Esquiva el volador este golpe cuerpo a cuerpo? (Las armas largas, como látigos y bolas, llegan arriba.) */
    static boolean esquivaGolpe(Pelea p, Cuerpo de, Cuerpo a, Pieza arma) {
        if (!rigen(p) || !a.cifras.rasgos.aereo || de.vuela()) return false;
        if (arma != null && arma.valor("alcance") >= eq().valor("aereo", "alcance_cuerpo", 120)) return false;
        return p.azar.nextDouble() < a.cifras.rasgos.esquivaCuerpo;
    }

    /** Lo que multiplica el daño de un arma contra ese blanco (antiaéreo contra voladores). */
    static double antiaereo(Pelea p, Pieza arma, Cuerpo a) {
        if (!rigen(p) || arma == null || a == null || !a.cifras.rasgos.aereo) return 1;
        double f = arma.valor("antiaereo", 1);
        return Double.isFinite(f) && f > 1 ? Math.min(3, f) : 1;
    }

    /** El tirón de un imán según sus piernas (flotadores y voladores, más; orugas, menos) y su masa. */
    static double tiron(Pelea p, Cuerpo a, double base) {
        if (!rigen(p)) return base;
        double masa = Math.max(0.6, Math.min(1.5, eq().masaReferencia / Math.max(1, a.cifras.masa)));
        return base * a.cifras.rasgos.iman * masa;
    }

    /** El retroceso al disparar un arma pesada: lo echa atrás y retrasa el siguiente disparo (orugas y patas lo absorben). */
    static void retroceso(Pelea p, Cuerpo c, Pieza arma, int brazo) {
        if (!rigen(p) || arma == null || c.cifras.rasgos.absorbeRetroceso) return;
        Equilibrio e = eq();
        double exceso = arma.valor("peso") - e.valor("retroceso", "umbral", 10);
        if (exceso <= 0) return;
        double masa = Math.max(0.5, e.masaReferencia / Math.max(1, c.cifras.masa));
        double d = exceso * e.valor("retroceso", "desplazamiento", 1.1) * masa;
        double rad = Math.toRadians(c.mira);
        c.x = Math.max(c.radio, Math.min(p.ancho - c.radio, c.x - Math.cos(rad) * d));
        c.y = Math.max(c.radio, Math.min(p.alto - c.radio, c.y - Math.sin(rad) * d));
        if (brazo >= 0 && brazo < c.enfriamiento.length) {
            c.enfriamiento[brazo] += (int) Math.round(exceso * e.valor("retroceso", "espera", 1.5));
        }
    }

    // ---------------------------------------------------------------- potencia regulable (Robocode)

    /** La potencia con que dispara ese arma (0 si no es regulable): la pedida con regularPotencia o la suya. */
    static double potencia(Cuerpo c, Pieza arma) {
        if (arma == null || arma.valor("potencia") <= 0) return 0;
        double pedida = Double.isNaN(c.potenciaPedida) ? arma.valor("potencia") : c.potenciaPedida;
        return acotarPotencia(pedida);
    }

    /** La potencia dentro de sus límites (0,1 a 3). */
    static double acotarPotencia(double pw) {
        Equilibrio e = eq();
        double min = e.valor("potencia", "minima", 0.1), max = e.valor("potencia", "maxima", 3);
        return Double.isFinite(pw) ? Math.max(min, Math.min(max, pw)) : 1;
    }

    /** Daño relativo a potencia 1: (4p + 2·max(p − 1, 0)) / 4. */
    static double factorDano(double pw) {
        return pw <= 0 ? 1 : (4 * pw + 2 * Math.max(pw - 1, 0)) / 4;
    }

    /** Velocidad relativa a potencia 1: (20 − 3p) / 17 (las balas fuertes van más lentas). */
    static double factorVelocidad(double pw) {
        return pw <= 0 ? 1 : Math.max(0.3, (20 - 3 * pw) / 17);
    }

    /** Enfriamiento (calor) relativo a potencia 1: (10 + 2p) / 12. */
    static double factorEnfriamiento(double pw) {
        return pw <= 0 ? 1 : (10 + 2 * pw) / 12;
    }

    /** Energía extra por disparar a esa potencia (solo lo que pase de 1, la potencia de siempre). */
    static double costeExtra(double pw) {
        return pw <= 1 ? 0 : (pw - 1) * eq().valor("potencia", "energia", 9);
    }

    /** Energía que devuelve acertar con un disparo de esa potencia. */
    static double reembolso(double pw) {
        return pw <= 0 ? 0 : pw * eq().valor("potencia", "reembolso", 1.5);
    }
}
