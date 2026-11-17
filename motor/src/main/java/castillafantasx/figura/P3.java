package castillafantasx.figura;

/**
 * P3 — Un punto (o una flecha) en el espacio de una figura, en píxeles:
 * x = hacia su derecha, y = hacia delante (adonde mira), z = hacia arriba.
 * No cambia nunca: cada cuenta devuelve un punto nuevo.
 */
public final class P3 {

    public static final P3 CERO = new P3(0, 0, 0);

    public final double x, y, z;

    public P3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public P3 mas(P3 o) {
        return new P3(x + o.x, y + o.y, z + o.z);
    }

    public P3 mas(double dx, double dy, double dz) {
        return new P3(x + dx, y + dy, z + dz);
    }

    public P3 menos(P3 o) {
        return new P3(x - o.x, y - o.y, z - o.z);
    }

    public P3 por(double k) {
        return new P3(x * k, y * k, z * k);
    }

    /** El punto que está entre este (parte 0) y 'o' (parte 1). */
    public P3 hacia(P3 o, double parte) {
        return new P3(x + (o.x - x) * parte, y + (o.y - y) * parte, z + (o.z - z) * parte);
    }

    public double largo() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** La misma flecha con largo 1. */
    public P3 unidad() {
        double l = largo();
        return l < 1e-9 ? new P3(0, 1, 0) : por(1 / l);
    }

    /** Producto escalar: cuánto apuntan dos flechas al mismo sitio. */
    public double punto(P3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    /** Producto vectorial: una flecha perpendicular a las dos. */
    public P3 cruz(P3 o) {
        return new P3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    /** Girado alrededor del eje vertical (positivo = hacia su izquierda). */
    public P3 girado(double angulo) {
        double c = Math.cos(angulo), s = Math.sin(angulo);
        return new P3(x * c - y * s, x * s + y * c, z);
    }

    /** Inclinado hacia delante alrededor del eje que va de lado a lado (positivo = la cabeza va hacia delante). */
    public P3 inclinado(double angulo) {
        double c = Math.cos(angulo), s = Math.sin(angulo);
        return new P3(x, y * c + z * s, -y * s + z * c);
    }

    /** Ladeado alrededor del eje que va de atrás adelante (positivo = la cabeza va hacia su derecha). */
    public P3 ladeado(double angulo) {
        double c = Math.cos(angulo), s = Math.sin(angulo);
        return new P3(x * c + z * s, y, -x * s + z * c);
    }

    /**
     * Dobla una extremidad de dos tramos: dados el arranque (hombro o cadera) y
     * el final (mano o pie), devuelve dónde queda la articulación del medio
     * (codo o rodilla), que se aparta hacia 'hacia'.
     */
    public static P3 doblar(P3 raiz, P3 fin, double largo1, double largo2, P3 hacia) {
        P3 eje = fin.menos(raiz);
        double d = Math.max(0.01, Math.min(eje.largo(), largo1 + largo2 - 0.01));
        P3 u = eje.unidad();
        double a = (largo1 * largo1 - largo2 * largo2 + d * d) / (2 * d);
        double h = Math.sqrt(Math.max(0, largo1 * largo1 - a * a));
        P3 lado = hacia.menos(u.por(hacia.punto(u))).unidad();
        return raiz.mas(u.por(a)).mas(lado.por(h));
    }

    /** Si 'fin' queda más lejos de lo que da la extremidad, lo acerca hasta donde llega. */
    public static P3 alcanzar(P3 raiz, P3 fin, double largo) {
        P3 eje = fin.menos(raiz);
        return eje.largo() <= largo ? fin : raiz.mas(eje.unidad().por(largo));
    }
}
