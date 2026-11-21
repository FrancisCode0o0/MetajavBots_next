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
public final class Armas {

    /** Límites de equilibrio anti-abuso para armas y habilidades especiales. */
    public static final double MAX_DANO = 45.0;
    public static final double MAX_DANO_EFECTIVO = 75.0;
    public static final double MIN_ALCANCE = 30.0;
    public static final double MAX_ALCANCE = 1000.0;
    public static final int MIN_PROYECTILES = 1;
    public static final int MAX_PROYECTILES = 6;
    public static final double MIN_VELOCIDAD_PROYECTIL = 3.0;
    public static final double MAX_VELOCIDAD_PROYECTIL = 40.0;
    public static final double MIN_DISPERSION = 0.5;
    public static final double MAX_DISPERSION = 45.0;
    public static final int MIN_DURACION = 5;
    public static final int MAX_DURACION = 120;
    public static final double MIN_BLOQUEO = 0.1;
    public static final double MAX_BLOQUEO = 0.85;
    public static final double MIN_FACTOR = 1.0;
    public static final double MAX_FACTOR = 2.0;
    public static final double MIN_CURA = 1.0;
    public static final double MAX_CURA = 25.0;
    public static final int MAX_ATURDE = 45;
    public static final double MAX_EMPUJE = 80.0;
    public static final double MIN_CONO = 15.0;
    public static final double MAX_CONO = 180.0;

    private final Pelea pelea;

    Armas(Pelea pelea) {
        this.pelea = pelea;
    }

    /** Daño base de un arma acotado a [0, MAX_DANO] (rechaza NaN, infinitos y negativos). */
    public static double danoSeguro(double dano) {
        if (!Double.isFinite(dano) || dano <= 0) return 0;
        return Math.min(MAX_DANO, dano);
    }

    /** Daño efectivo tras multiplicadores (sobrecarga, naturaleza) acotado a [0, MAX_DANO_EFECTIVO]. */
    public static double danoEfectivoSeguro(double dano) {
        if (!Double.isFinite(dano) || dano <= 0) return 0;
        return Math.min(MAX_DANO_EFECTIVO, dano);
    }

    /** Alcance acotado a [MIN_ALCANCE, MAX_ALCANCE] (0 si el arma no tiene alcance). */
    public static double alcanceSeguro(double alcance) {
        if (!Double.isFinite(alcance) || alcance <= 0) return 0;
        return Math.max(MIN_ALCANCE, Math.min(MAX_ALCANCE, alcance));
    }

    /** Número de proyectiles/misiles por disparo acotado a [MIN_PROYECTILES, MAX_PROYECTILES]. */
    public static int proyectilesSeguros(double n) {
        if (!Double.isFinite(n) || n < MIN_PROYECTILES) return MIN_PROYECTILES;
        return Math.max(MIN_PROYECTILES, Math.min(MAX_PROYECTILES, (int) Math.round(n)));
    }

    /** Velocidad de proyectil acotada a [MIN_VELOCIDAD_PROYECTIL, MAX_VELOCIDAD_PROYECTIL]. */
    public static double velocidadProyectilSegura(double v) {
        if (!Double.isFinite(v) || v <= 0) return MIN_VELOCIDAD_PROYECTIL;
        return Math.max(MIN_VELOCIDAD_PROYECTIL, Math.min(MAX_VELOCIDAD_PROYECTIL, v));
    }

    /** Dispersión angular acotada a [MIN_DISPERSION, MAX_DISPERSION]. */
    public static double dispersionSegura(double d) {
        if (!Double.isFinite(d) || d <= 0) return MIN_DISPERSION;
        return Math.max(MIN_DISPERSION, Math.min(MAX_DISPERSION, d));
    }

    /** Duración de efecto acotada a [MIN_DURACION, MAX_DURACION] turnos. */
    public static int duracionSegura(double turnos) {
        if (!Double.isFinite(turnos) || turnos <= 0) return MIN_DURACION;
        return Math.max(MIN_DURACION, Math.min(MAX_DURACION, (int) Math.round(turnos)));
    }

    /** Bloqueo de escudo acotado a [MIN_BLOQUEO, MAX_BLOQUEO] (nunca 100 % invulnerable). */
    public static double bloqueoSeguro(double bloqueo) {
        if (!Double.isFinite(bloqueo) || bloqueo <= 0) return MIN_BLOQUEO;
        return Math.max(MIN_BLOQUEO, Math.min(MAX_BLOQUEO, bloqueo));
    }

    /** Multiplicador de turbo/sobrecarga acotado a [MIN_FACTOR, MAX_FACTOR]. */
    public static double factorSeguro(double factor) {
        if (!Double.isFinite(factor) || factor < MIN_FACTOR) return MIN_FACTOR;
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, factor));
    }

    /** Curación por uso acotada a [MIN_CURA, MAX_CURA]. */
    public static double curaSegura(double cura) {
        if (!Double.isFinite(cura) || cura <= 0) return MIN_CURA;
        return Math.max(MIN_CURA, Math.min(MAX_CURA, cura));
    }

    /** Turnos de aturdimiento/volteo acotados a [0, MAX_ATURDE]. */
    public static int aturdeSeguro(double turnos) {
        if (!Double.isFinite(turnos) || turnos <= 0) return 0;
        return Math.min(MAX_ATURDE, (int) Math.round(turnos));
    }

    /** Empuje de impacto acotado a [0, MAX_EMPUJE]. */
    public static double empujeSeguro(double empuje) {
        if (!Double.isFinite(empuje) || empuje <= 0) return 0;
        return Math.min(MAX_EMPUJE, empuje);
    }

    /** Ángulo de cono acotado a [MIN_CONO, MAX_CONO]. */
    public static double conoSeguro(double cono) {
        if (!Double.isFinite(cono) || cono <= 0) return MIN_CONO;
        return Math.max(MIN_CONO, Math.min(MAX_CONO, cono));
    }

    /** Ataca con el arma de un brazo (láser, balas, misiles, golpe, llamas, escudo o reparar). */
    void usar(Cuerpo c, Pieza p, int brazo) {
        Combatiente.Informe inf = pelea.informe(c.indice);
        double sobre = c.sobrecarga > 0 ? factorSeguro(c.factorSobrecarga) : 1;
        double[] boca = boca(c, brazo);
        switch (p.arma()) {
            case LASER: {
                inf.ataque("laser");
                double alcance = alcanceSeguro(p.valor("alcance", 500));
                double danoBase = danoSeguro(p.valor("dano"));
                double ang = c.mira;
                double dx = Math.cos(Math.toRadians(ang)), dy = Math.sin(Math.toRadians(ang));
                // El rayo se para en el primer robot que se cruza (justo en su chapa); a un compañero no le hace daño.
                double[] fin = {alcance};
                Cuerpo tocado = primeroEnLaLinea(c, boca[0], boca[1], dx, dy, alcance, fin);
                boolean da = tocado != null && tocado.equipo != c.equipo;
                double hx = boca[0] + dx * fin[0], hy = boca[1] + dy * fin[0];
                pelea.rayos.add(new Pelea.Rayo(c.indice, boca[0], boca[1], hx, hy, da));
                pelea.suceso(Suceso.Tipo.LASER, c.indice, boca[0], boca[1], ang, danoBase, "laser");
                if (da) pelea.danar(tocado, c, danoEfectivoSeguro(danoBase * sobre), "laser", ang + 180, false, hx, hy);
                c.retroceso = 4;
                break;
            }
            case BALA: {
                int n = proyectilesSeguros(p.valor("perdigones", 1));
                double disp = dispersionSegura(p.valor("dispersion", 3)) / Math.max(0.5, c.cifras.precision);
                double vel = velocidadProyectilSegura(p.valor("velocidad", 14));
                double dano = danoEfectivoSeguro(danoSeguro(p.valor("dano")) * sobre);
                double alcance = alcanceSeguro(p.valor("alcance", 600));
                for (int i = 0; i < n; i++) {
                    inf.ataque("bala");
                    double ang = c.mira + (pelea.azar.nextDouble() * 2 - 1) * disp;
                    pelea.proyectiles.add(new Proyectil(c.indice, false, boca[0], boca[1], ang, vel, dano, alcance, 0));
                }
                pelea.suceso(Suceso.Tipo.DISPARO, c.indice, boca[0], boca[1], c.mira, n, "bala");
                c.retroceso = 6;
                break;
            }
            case MISIL: {
                int n = proyectilesSeguros(p.valor("misiles", 1));
                double vel = velocidadProyectilSegura(p.valor("velocidad", 7));
                double dano = danoEfectivoSeguro(danoSeguro(p.valor("dano")) * sobre);
                double alcance = alcanceSeguro(p.valor("alcance", 800));
                double giro = Cuerpo.giroSeguro(p.valor("giro", 4));
                for (int i = 0; i < n; i++) {
                    inf.ataque("misil");
                    double ang = c.mira + (n == 1 ? 0 : (i - (n - 1) / 2.0) * 14);
                    pelea.proyectiles.add(new Proyectil(c.indice, true, boca[0], boca[1], ang, vel, dano, alcance, giro));
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
                c.llama = duracionSegura(p.valor("duracion", 14));
                c.brazoDeLaLlama = brazo;
                pelea.suceso(Suceso.Tipo.LLAMA, c.indice, boca[0], boca[1], c.mira, c.llama, "llama");
                break;
            case ESCUDO:
                c.escudoBrazo = duracionSegura(p.valor("duracion", 45));
                c.bloqueoBrazo = bloqueoSeguro(p.valor("bloqueo", 0.6));
                c.conoEscudo = conoSeguro(p.valor("cono", 120));
                c.brazoDelEscudo = brazo;
                pelea.suceso(Suceso.Tipo.ESPECIAL, c.indice, c.x, c.y, c.mira, c.escudoBrazo, "escudo_brazo");
                break;
            case REPARAR:
                reparar(c, curaSegura(p.valor("cura", 10)));
                break;
            default:
                break;
        }
    }

    /** Lo especial de la cabeza: escudo, turbo, reparar, camuflaje, pulso o sobrecarga. */
    void especial(Cuerpo c, Pieza p) {
        switch (p.especial()) {
            case ESCUDO:
                c.escudoCabeza = duracionSegura(p.valor("duracion", 90));
                c.bloqueoCabeza = bloqueoSeguro(p.valor("bloqueo", 0.6));
                break;
            case TURBO:
                c.turbo = duracionSegura(p.valor("duracion", 60));
                c.factorTurbo = factorSeguro(p.valor("factor", 1.8));
                break;
            case REPARAR:
                reparar(c, curaSegura(p.valor("cura", 15)));
                break;
            case CAMUFLAJE:
                c.camuflaje = duracionSegura(p.valor("duracion", 75));
                break;
            case PULSO: {
                double alcance = alcanceSeguro(p.valor("alcance", 240));
                int aturde = aturdeSeguro(p.valor("aturde", 30));
                // El pulso llega a todos los enemigos cercanos.
                for (Cuerpo otro : pelea.cuerpos) {
                    if (otro.equipo == c.equipo || otro.fuera) continue;
                    double d = Math.hypot(otro.x - c.x, otro.y - c.y);
                    if (d <= alcance) {
                        aturdir(otro, aturde);
                        pelea.danar(otro, c, 3, "pulso", Math.toDegrees(Math.atan2(c.y - otro.y, c.x - otro.x)), false);
                    }
                }
                // El pulso también borra los pelea.proyectiles enemigos cercanos.
                for (Proyectil pr : pelea.proyectiles) {
                    if (pelea.cuerpos[pr.dueno].equipo != c.equipo && Math.hypot(pr.x - c.x, pr.y - c.y) < alcance) pr.vivo = false;
                }
                break;
            }
            case SOBRECARGA:
                c.sobrecarga = duracionSegura(p.valor("duracion", 90));
                c.factorSobrecarga = factorSeguro(p.valor("factor", 1.5));
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
        Cuerpo e = alAlcance(c, alcanceSeguro(p.valor("alcance", 50)), conoSeguro(p.valor("cono", 70)));
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
        double danoBase = danoSeguro(p.valor("dano"));
        pelea.suceso(Suceso.Tipo.GOLPE_CUERPO, c.indice, hx, hy, c.mira, danoBase, p.id);
        if (pelea.danar(e, c, danoEfectivoSeguro(danoBase * sobre * c.cifras.cuerpo), "cuerpo", ang + 180, true, hx, hy)) {
            int aturde = aturdeSeguro(p.valor("aturde"));
            if (aturde > 0) aturdir(e, aturde);
            // Y lo echa para atrás (las cuñas y los volteadores, mucho más).
            double empuje = empujeSeguro(p.valor("empuje", 10));
            e.x = Math.max(e.radio, Math.min(pelea.ancho - e.radio, e.x + Math.cos(Math.toRadians(ang)) * empuje));
            e.y = Math.max(e.radio, Math.min(pelea.alto - e.radio, e.y + Math.sin(Math.toRadians(ang)) * empuje));
            int vuelo = aturdeSeguro(p.valor("volteo"));
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
