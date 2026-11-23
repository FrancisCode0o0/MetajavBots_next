package metajavbots.combate;

import java.util.List;

import metajavbots.api.Mando;
import metajavbots.api.Puente;
import metajavbots.piezas.Pieza;

/**
 * ATAQUE LEJOS — Un ataque que salió con la mira lejos del enemigo que el robot
 * estaba viendo: la mira gira despacio y el disparo sale hacia donde mira en ese
 * momento, no hacia donde se le acaba de pedir. Lo apunta Cuerpo en cada turno
 * del código del jugador (solo el primero del turno) para el depurador y el aviso
 * del entrenador (pantallas/AvisoDeOrdenes).
 */
public final class AtaqueLejos {

    /**
     * Cuántos grados de margen hay como mínimo entre la mira y el enemigo antes de decir que el disparo sale lejos.
     * De cerca el margen es mayor: el enemigo (unos 35 de radio) ocupa más ángulo.
     */
    public static final double UMBRAL = 8, RADIO_DEL_BLANCO = 35;

    /** El método desde el que se atacó, su línea, cuánto faltaba a la mira (grados) y la línea de la última orden de mira (0: ninguna). */
    public final String metodo;
    public final int linea;
    public final double faltaban;
    public final int lineaDeLaMira;

    public AtaqueLejos(String metodo, int linea, double faltaban, int lineaDeLaMira) {
        this.metodo = metodo == null ? "" : metodo;
        this.linea = linea;
        this.faltaban = faltaban;
        this.lineaDeLaMira = lineaDeLaMira;
    }

    /** Si ese ataque de ese brazo sale lejos de todos los enemigos que ve, lo que hay que contar; si no, null. */
    static AtaqueLejos mirar(Cuerpo c, int brazo, String metodo, List<OrdenDelTurno> ordenes) {
        if (brazo == Mando.CABEZA || c.vistos.isEmpty()) return null;
        Pieza p = c.piezaDe(brazo);
        Pieza.Arma a = p == null ? Pieza.Arma.NINGUNA : p.arma();
        if (a == Pieza.Arma.ESCUDO || a == Pieza.Arma.REPARAR || a == Pieza.Arma.NINGUNA) return null;
        double mejor = 180;
        for (Avistamiento v : c.vistos) {
            double falta = Math.abs(Pelea.normal(v.angulo() - c.mira));
            if (falta <= margen(v.distancia())) return null;
            mejor = Math.min(mejor, falta);
        }
        int lineaMira = 0;
        for (OrdenDelTurno o : ordenes) if ((o.canales & Mando.CANAL_MIRA) != 0) lineaMira = o.linea;
        return new AtaqueLejos(metodo, Puente.ultimaLinea(), mejor, lineaMira);
    }

    /** El margen (grados) con el que un disparo aún va hacia un enemigo a esa distancia. */
    public static double margen(double distancia) {
        return Math.max(UMBRAL, Math.toDegrees(Math.atan2(RADIO_DEL_BLANCO, Math.max(1, distancia))));
    }
}
