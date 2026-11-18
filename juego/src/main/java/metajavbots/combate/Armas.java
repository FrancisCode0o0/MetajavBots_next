package metajavbots.combate;

import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;

/**
 * ARMAS — Lo que hace cada arma al usarla (salió de Pelea, que pasaba de
 * 900 líneas): el láser se para en la chapa del primer robot que cruza, las
 * balas y los misiles salen de la punta del brazo, el golpe va al enemigo que
 * está delante y a su alcance, las llamas, el escudo de brazo y el brazo
 * reparador; y lo especial de la cabeza. El daño lo calcula la pelea
 * (Pelea.danar).
 */
final class Armas {

    private final Pelea pelea;

    Armas(Pelea pelea) {
        this.pelea = pelea;
    }

    /** Ataca con el arma de un brazo (láser, balas, misiles, golpe, llamas, escudo o reparar). */
    void usar(Cuerpo c, Cuerpo e, Pieza p, int brazo) {
        Combatiente.Informe inf = pelea.informe(c.indice);
        double sobre = c.sobrecarga > 0 ? c.factorSobrecarga : 1;
        double[] boca = boca(c, brazo);
        switch (p.arma()) {
            case LASER: {
                inf.ataque("laser");
                double alcance = p.valor("alcance", 500);
                double ang = c.mira;
                double dx = Math.cos(Math.toRadians(ang)), dy = Math.sin(Math.toRadians(ang));
                // El rayo se para en el primer robot que se cruza (justo en su chapa); a un compañero no le hace daño.
                double[] fin = {alcance};
                Cuerpo tocado = primeroEnLaLinea(c, boca[0], boca[1], dx, dy, alcance, fin);
                boolean da = tocado != null && tocado.equipo != c.equipo;
                double hx = boca[0] + dx * fin[0], hy = boca[1] + dy * fin[0];
                pelea.rayos.add(new Pelea.Rayo(c.indice, boca[0], boca[1], hx, hy, da));
                pelea.suceso(Suceso.Tipo.LASER, c.indice, boca[0], boca[1], ang, p.valor("dano"), "laser");
                if (da) pelea.danar(tocado, c, p.valor("dano") * sobre, "laser", ang + 180, false, hx, hy);
                c.retroceso = 4;
                break;
            }
            case BALA: {
                int n = Math.max(1, (int) p.valor("perdigones", 1));
                double disp = p.valor("dispersion", 3) / c.cifras.precision;
                for (int i = 0; i < n; i++) {
                    inf.ataque("bala");
                    double ang = c.mira + (pelea.azar.nextDouble() * 2 - 1) * disp;
                    pelea.proyectiles.add(new Proyectil(c.indice, false, boca[0], boca[1], ang, p.valor("velocidad", 14),
                            p.valor("dano") * sobre, p.valor("alcance", 600), 0));
                }
                pelea.suceso(Suceso.Tipo.DISPARO, c.indice, boca[0], boca[1], c.mira, n, "bala");
                c.retroceso = 6;
                break;
            }
            case MISIL: {
                int n = Math.max(1, (int) p.valor("misiles", 1));
                for (int i = 0; i < n; i++) {
                    inf.ataque("misil");
                    double ang = c.mira + (n == 1 ? 0 : (i - (n - 1) / 2.0) * 14);
                    pelea.proyectiles.add(new Proyectil(c.indice, true, boca[0], boca[1], ang, p.valor("velocidad", 7),
                            p.valor("dano") * sobre, p.valor("alcance", 800), p.valor("giro", 4)));
                }
                pelea.suceso(Suceso.Tipo.MISIL, c.indice, boca[0], boca[1], c.mira, n, "misil");
                c.retroceso = 5;
                break;
            }
            case CUERPO:
                inf.ataque("cuerpo");
                golpear(c, p, sobre);
                break;
            case LLAMA:
                inf.ataque("llama");
                c.llama = (int) p.valor("duracion", 14);
                c.brazoDeLaLlama = brazo;
                pelea.suceso(Suceso.Tipo.LLAMA, c.indice, boca[0], boca[1], c.mira, c.llama, "llama");
                break;
            case ESCUDO:
                c.escudoBrazo = (int) p.valor("duracion", 45);
                c.bloqueoBrazo = p.valor("bloqueo", 0.6);
                c.conoEscudo = p.valor("cono", 120);
                c.brazoDelEscudo = brazo;
                pelea.suceso(Suceso.Tipo.ESPECIAL, c.indice, c.x, c.y, c.mira, c.escudoBrazo, "escudo_brazo");
                break;
            case REPARAR:
                reparar(c, p.valor("cura", 10));
                break;
            default:
                break;
        }
    }

    /** Lo especial de la cabeza: escudo, turbo, reparar, camuflaje, pulso o sobrecarga. */
    void especial(Cuerpo c, Pieza p) {
        switch (p.especial()) {
            case ESCUDO:
                c.escudoCabeza = (int) p.valor("duracion", 90);
                c.bloqueoCabeza = p.valor("bloqueo", 0.6);
                break;
            case TURBO:
                c.turbo = (int) p.valor("duracion", 60);
                c.factorTurbo = p.valor("factor", 1.8);
                break;
            case REPARAR:
                reparar(c, p.valor("cura", 15));
                break;
            case CAMUFLAJE:
                c.camuflaje = (int) p.valor("duracion", 75);
                break;
            case PULSO: {
                // El pulso llega a todos los enemigos cercanos.
                for (Cuerpo otro : pelea.cuerpos) {
                    if (otro.equipo == c.equipo || otro.fuera) continue;
                    double d = Math.hypot(otro.x - c.x, otro.y - c.y);
                    if (d <= p.valor("alcance", 240)) {
                        aturdir(otro, (int) p.valor("aturde", 30));
                        pelea.danar(otro, c, 3, "pulso", Math.toDegrees(Math.atan2(c.y - otro.y, c.x - otro.x)), false);
                    }
                }
                // El pulso también borra los pelea.proyectiles enemigos cercanos.
                for (Proyectil pr : pelea.proyectiles) {
                    if (pelea.cuerpos[pr.dueno].equipo != c.equipo && Math.hypot(pr.x - c.x, pr.y - c.y) < p.valor("alcance", 240)) pr.vivo = false;
                }
                break;
            }
            case SOBRECARGA:
                c.sobrecarga = (int) p.valor("duracion", 90);
                c.factorSobrecarga = p.valor("factor", 1.5);
                break;
            default:
                return;
        }
        pelea.suceso(Suceso.Tipo.ESPECIAL, c.indice, c.x, c.y, c.mira, 0, p.especial().name().toLowerCase(java.util.Locale.ROOT));
    }

    private void reparar(Cuerpo c, double cura) {
        double total = 0;
        for (int i = 0; i < 4; i++) {
            if (c.parte[i] <= 0 && i == Montaje.CABEZA) continue;
            double falta = c.parteMax[i] - c.parte[i];
            double r = Math.min(falta, cura / 4 + (i == Montaje.PIERNAS ? 0 : 0));
            if (r > 0) {
                c.parte[i] += r;
                total += r;
            }
        }
        pelea.suceso(Suceso.Tipo.REPARACION, c.indice, c.x, c.y, 0, total, "");
    }

    private void aturdir(Cuerpo e, int turnos) {
        if (turnos <= 0 || e.fuera) return;
        e.aturdido = Math.max(e.aturdido, turnos);
        e.avance = 0;
        pelea.suceso(Suceso.Tipo.ATURDIDO, e.indice, e.x, e.y, 0, turnos, "");
    }

    /** Un golpe cuerpo a cuerpo: al enemigo que tenga delante y a su alcance (el más cercano), sea o no al que mira el código. */
    private void golpear(Cuerpo c, Pieza p, double sobre) {
        Cuerpo e = alAlcance(c, p.valor("alcance", 50), p.valor("cono", 70));
        // Un golpe cuerpo a cuerpo empuja un poco hacia delante.
        c.x += Math.cos(Math.toRadians(c.mira)) * 6;
        c.y += Math.sin(Math.toRadians(c.mira)) * 6;
        c.retroceso = -8;
        if (e == null) {
            pelea.suceso(Suceso.Tipo.FALLO_CUERPO, c.indice, c.x, c.y, c.mira, 0, p.id);
            return;
        }
        double ang = Math.toDegrees(Math.atan2(e.y - c.y, e.x - c.x));
        // Donde toca: en la chapa del otro, del lado del que pega.
        double hx = e.x - Math.cos(Math.toRadians(ang)) * e.radio * 0.85, hy = e.y - Math.sin(Math.toRadians(ang)) * e.radio * 0.85;
        pelea.suceso(Suceso.Tipo.GOLPE_CUERPO, c.indice, hx, hy, c.mira, p.valor("dano"), p.id);
        if (pelea.danar(e, c, p.valor("dano") * sobre * c.cifras.cuerpo, "cuerpo", ang + 180, true, hx, hy)) {
            int aturde = (int) p.valor("aturde");
            if (aturde > 0) aturdir(e, aturde);
            // Y lo echa para atrás (las cuñas y los volteadores, mucho más).
            double empuje = p.valor("empuje", 10);
            e.x = Math.max(e.radio, Math.min(pelea.ancho - e.radio, e.x + Math.cos(Math.toRadians(ang)) * empuje));
            e.y = Math.max(e.radio, Math.min(pelea.alto - e.radio, e.y + Math.sin(Math.toRadians(ang)) * empuje));
            int vuelo = (int) p.valor("volteo");
            if (vuelo > 0 && !e.vuela()) {
                // El volteador lo levanta por los aires: se ve volar y, mientras, no hace nada.
                e.volteado = Math.max(e.volteado, vuelo);
                aturdir(e, vuelo);
            }
        }
    }

    /**
     * El primer robot (que no sea c ni esté fuera) que cruza la línea que sale de (x, y) en la dirección (dx, dy), hasta
     * «alcance»; en fin[0] queda dónde toca su chapa (o el alcance si no toca a nadie).
     */
    Cuerpo primeroEnLaLinea(Cuerpo c, double x, double y, double dx, double dy, double alcance, double[] fin) {
        Cuerpo primero = null;
        fin[0] = alcance;
        for (Cuerpo o : pelea.cuerpos) {
            if (o == c || o.fuera) continue;
            double ox = o.x - x, oy = o.y - y;
            double t = ox * dx + oy * dy;
            double perp = Math.abs(ox * dy - oy * dx);
            if (t <= 0 || perp > o.radio) continue;
            double entrada = Math.max(0, t - Math.sqrt(o.radio * o.radio - perp * perp));
            if (entrada <= alcance && entrada < fin[0]) {
                fin[0] = entrada;
                primero = o;
            }
        }
        return primero;
    }

    /** El enemigo más cercano que c tiene delante (en su cono) y al alcance de un golpe; null si no hay. */
    Cuerpo alAlcance(Cuerpo c, double alcance, double cono) {
        Cuerpo mejor = null;
        double mejorD = Double.MAX_VALUE;
        for (Cuerpo o : pelea.cuerpos) {
            if (o == c || o.fuera || o.equipo == c.equipo || o.vuela() && !c.vuela()) continue;
            double d = Math.hypot(o.x - c.x, o.y - c.y);
            double ang = Math.toDegrees(Math.atan2(o.y - c.y, o.x - c.x));
            if (d <= c.radio + o.radio + alcance && Math.abs(Pelea.normal(ang - c.mira)) <= cono / 2 && d < mejorD) {
                mejor = o;
                mejorD = d;
            }
        }
        return mejor;
    }

    /** Por dónde sale el ataque: la punta del brazo. */
    static double[] boca(Cuerpo c, int brazo) {
        double lado = brazo == Cuerpo.IZQUIERDA ? 1 : brazo == Cuerpo.DERECHA ? -1 : 0;
        double a = Math.toRadians(c.mira);
        double px = Math.cos(a) * c.radio * 0.9 - Math.sin(a) * lado * c.radio * 0.55;
        double py = Math.sin(a) * c.radio * 0.9 + Math.cos(a) * lado * c.radio * 0.55;
        return new double[]{c.x + px, c.y + py};
    }
}
