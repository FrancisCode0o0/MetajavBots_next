package metajavbots.pantallas;

import java.util.Locale;

import metajavbots.api.Mando;
import metajavbots.combate.AtaqueLejos;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.OrdenDelTurno;
import metajavbots.textos.T;

/**
 * AVISO DE ÓRDENES — Cuenta en lenguaje llano los dos tropiezos de quien
 * empieza: una orden que borra a otra del mismo mando en el mismo turno («la
 * línea 27 borra la orden de la línea 41: la cabeza nunca llega al enemigo») y
 * un ataque con la mira aún lejos del enemigo. Mira el último turno del robot del
 * jugador (Cuerpo.ordenesDelTurno y Cuerpo.ataqueLejos); cada aviso sale una sola
 * vez por pelea (una instancia por pelea) y lo enseña el entrenador si el
 * jugador no lo ha apagado en Ajustes. El depurador usa los mismos textos.
 */
public final class AvisoDeOrdenes {

    /** Cuántos segundos se queda el aviso en el bocadillo del entrenador. */
    public static final float DURACION = 14f;

    private boolean avisadaPisada, avisadoLejos, recien;
    private String texto = "";
    private float tiempo = DURACION;

    /** Mira el último turno de ese robot; true si acaba de salir un aviso nuevo. */
    public boolean observar(Cuerpo c) {
        if (c == null) return false;
        if (!avisadaPisada && c.tieneConflictoDeOrdenes) {
            OrdenDelTurno o = primeraPisada(c);
            if (o != null) {
                avisadaPisada = true;
                return salir(textoPisada(o));
            }
        }
        if (!avisadoLejos && c.ataqueLejos != null) {
            avisadoLejos = true;
            return salir(textoLejos(c.ataqueLejos));
        }
        return false;
    }

    private boolean salir(String t) {
        texto = t;
        tiempo = 0;
        recien = true;
        return true;
    }

    /** Si hay un aviso que enseñar ahora. */
    public boolean activo() {
        return tiempo < DURACION && !texto.isEmpty();
    }

    /** El aviso que se está enseñando («» si ninguno). */
    public String texto() {
        return activo() ? texto : "";
    }

    /** True solo la primera vez que se pregunta tras salir un aviso (para decirlo en voz alta una vez). */
    boolean recienSalido() {
        boolean r = recien && activo();
        recien = false;
        return r;
    }

    /** Pasa el tiempo (solo mientras la pelea va). */
    void pasar(float dt) {
        if (tiempo < DURACION) tiempo += dt;
    }

    /** La primera orden pisada del turno, o null. */
    public static OrdenDelTurno primeraPisada(Cuerpo c) {
        if (c == null) return null;
        for (OrdenDelTurno o : c.ordenesDelTurno) if (o.pisada()) return o;
        return null;
    }

    /** «La línea 27 borra la orden de la línea 41: la cabeza nunca llega al enemigo. Cada mando recuerda…». */
    public static String textoPisada(OrdenDelTurno o) {
        String despues = linea(o.lineaPisada), antes = linea(o.linea);
        boolean mira = (o.canales & Mando.CANAL_MIRA) != 0;
        boolean alVer = o.metodo.equals("alVerEnemigo") || o.metodo.equals("beiSicht") || o.metodo.equals("onSeeEnemy");
        String primera = mira && alVer ? T.f("aviso.ordenes.pisada_mira", despues, antes)
                : T.f("aviso.ordenes.pisada", despues, antes, T.t("aviso.mando." + mando(o.canales)));
        String porQue = o.metodo.equals(o.pisadaPor) || o.metodo.isEmpty() || o.pisadaPor.isEmpty() ? T.t("aviso.ordenes.por_que_mismo")
                : T.f("aviso.ordenes.por_que", o.pisadaPor, o.metodo);
        return primera + " " + porQue;
    }

    /** «La línea 41 pide girar la cabeza, pero la línea 42 dispara antes de que llegue…». */
    public static String textoLejos(AtaqueLejos a) {
        String grados = String.format(Locale.ROOT, "%.0f", a.faltaban);
        if (a.lineaDeLaMira > 0 && a.lineaDeLaMira != a.linea) {
            return T.f("aviso.ordenes.lejos_mira", linea(a.lineaDeLaMira), linea(a.linea), grados);
        }
        return T.f("aviso.ordenes.lejos", linea(a.linea), grados);
    }

    /** El mando que más importa de esos canales: la mira, luego el giro, luego el avance. */
    static int mando(int canales) {
        if ((canales & Mando.CANAL_MIRA) != 0) return Mando.CANAL_MIRA;
        if ((canales & Mando.CANAL_GIRO) != 0) return Mando.CANAL_GIRO;
        return Mando.CANAL_AVANCE;
    }

    private static String linea(int n) {
        return n > 0 ? Integer.toString(n) : "?";
    }
}
