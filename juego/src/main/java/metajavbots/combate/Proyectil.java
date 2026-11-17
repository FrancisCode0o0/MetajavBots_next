package metajavbots.combate;

/** PROYECTIL — Una bala o un misil en vuelo. */
public final class Proyectil {

    public final int dueno;
    public final boolean misil;
    public double x, y, vx, vy;
    public final double dano, velocidad, giro;
    public double recorrido;
    public final double alcance;
    public boolean vivo = true;
    /** Para el dibujo: de dónde salió (la estela). */
    public final double x0, y0;

    Proyectil(int dueno, boolean misil, double x, double y, double angulo, double velocidad, double dano, double alcance, double giro) {
        this.dueno = dueno;
        this.misil = misil;
        this.x = x;
        this.y = y;
        this.x0 = x;
        this.y0 = y;
        this.velocidad = velocidad;
        this.vx = Math.cos(Math.toRadians(angulo)) * velocidad;
        this.vy = Math.sin(Math.toRadians(angulo)) * velocidad;
        this.dano = dano;
        this.alcance = alcance;
        this.giro = giro;
    }

    public double angulo() {
        return Math.toDegrees(Math.atan2(vy, vx));
    }
}
