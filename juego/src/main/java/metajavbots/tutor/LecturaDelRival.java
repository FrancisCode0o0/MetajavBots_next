package metajavbots.tutor;

import java.util.ArrayList;
import java.util.List;

/**
 * LECTURA DEL RIVAL — Lo que el entrenador ve del enemigo que el jugador ha
 * marcado en la pelea: turno a turno apunta dónde está, cómo se mueve
 * alrededor del jugador y cuánto daño se hacen, y de las últimas ~3 s saca
 * qué hace («te rodea por la izquierda», «se te echa encima», «no se
 * mueve», «vuela»…). Cada lectura es un id; los consejos, con su pista y su
 * trocito de pseudocódigo, están en res/textos/ui.txt
 * («entrenador.rival.<id>» y «entrenador.rival.<id>.trozo»). Sin libGDX:
 * se prueba sola (PruebasDelEntrenador).
 */
public final class LecturaDelRival {

    /** Cuántos turnos mira hacia atrás (unos 3 s). */
    static final int VENTANA = 90;

    private final double[] angulo = new double[VENTANA], distancia = new double[VENTANA], velocidad = new double[VENTANA];
    private final double[] errorDeMira = new double[VENTANA], cercaDePared = new double[VENTANA];
    private final double[] danoRecibido = new double[VENTANA], danoHecho = new double[VENTANA];
    private int n;
    private boolean vuela;

    /**
     * Un turno: dónde estoy y adónde miro, dónde está él, lo rápido que va y si vuela, y el daño total que
     * llevo recibido y hecho hasta ahora en la pelea. La arena, para saber si se pega a las paredes.
     */
    public void observar(double yoX, double yoY, double miMira, double elX, double elY, double suVelocidad, boolean suVuela,
            double danoRecibidoTotal, double danoHechoTotal, double anchoArena, double altoArena) {
        int i = n % VENTANA;
        double dx = elX - yoX, dy = elY - yoY;
        angulo[i] = Math.toDegrees(Math.atan2(dy, dx));
        distancia[i] = Math.hypot(dx, dy);
        velocidad[i] = Math.abs(suVelocidad);
        errorDeMira[i] = Math.abs(normalizar(angulo[i] - miMira));
        double pared = Math.min(Math.min(elX, anchoArena - elX), Math.min(elY, altoArena - elY));
        cercaDePared[i] = pared < Math.min(anchoArena, altoArena) * 0.12 ? 1 : 0;
        danoRecibido[i] = danoRecibidoTotal;
        danoHecho[i] = danoHechoTotal;
        vuela = suVuela;
        n++;
    }

    /** Si ya ha visto bastante para opinar (1 s). */
    public boolean bastante() {
        return n >= 30;
    }

    /** Lo que se ha olvidado todo (otra ronda u otro enemigo marcado). */
    public void olvidar() {
        n = 0;
    }

    private int cuantos() {
        return Math.min(n, VENTANA);
    }

    private int indice(int haceTurnos) {
        return Math.floorMod(n - 1 - haceTurnos, VENTANA);
    }

    private static double normalizar(double g) {
        g = g % 360;
        if (g > 180) g -= 360;
        if (g < -180) g += 360;
        return g;
    }

    /** Cuánto ha girado a mi alrededor en la ventana (grados; positivo = en contra de las agujas del reloj = por mi izquierda). */
    double giroAlrededor() {
        double s = 0;
        for (int k = cuantos() - 1; k > 0; k--) s += normalizar(angulo[indice(k - 1)] - angulo[indice(k)]);
        return s;
    }

    private double media(double[] a) {
        double s = 0;
        int c = cuantos();
        for (int k = 0; k < c; k++) s += a[indice(k)];
        return c == 0 ? 0 : s / c;
    }

    /**
     * Lo que hace, de lo más importante a lo menos (ids): «rodea_izquierda», «rodea_derecha», «se_acerca»,
     * «se_aleja», «lejos», «quieto», «rapido», «vuela», «pared», «no_apuntas», «te_gana», «le_ganas». Vacío
     * si aún no ha visto bastante.
     */
    public List<String> lecturas() {
        List<String> l = new ArrayList<>();
        if (!bastante()) return l;
        int c = cuantos();
        double primera = distancia[indice(c - 1)], ultima = distancia[indice(0)];
        double cambio = ultima - primera, dist = media(distancia), vel = media(velocidad);
        double recibido = danoRecibido[indice(0)] - danoRecibido[indice(c - 1)];
        double hecho = danoHecho[indice(0)] - danoHecho[indice(c - 1)];
        // TODO-FRAN: los umbrales (en unidades de la arena y por ventana de ~3 s).
        double giro = giroAlrededor();
        if (giro > 40) l.add("rodea_izquierda");
        else if (giro < -40) l.add("rodea_derecha");
        if (cambio < -120) l.add("se_acerca");
        else if (cambio > 120) l.add("se_aleja");
        else if (dist > 420) l.add("lejos");
        if (recibido > hecho * 1.6 + 4) l.add("te_gana");
        if (vuela) l.add("vuela");
        if (vel < 0.4) l.add("quieto");
        else if (vel > 5.5) l.add("rapido");
        if (media(errorDeMira) > 40) l.add("no_apuntas");
        if (media(cercaDePared) > 0.6) l.add("pared");
        if (hecho > recibido * 1.6 + 4 && !l.contains("te_gana")) l.add("le_ganas");
        return l;
    }
}
