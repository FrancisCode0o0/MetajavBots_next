package metajavbots.api;

/**
 * ROBOT BASE — Lo común a los robots de los tres idiomas. No se hereda de ella
 * directamente: tu robot hereda de Metajavbot (español), MetajavbotDe (alemán)
 * o MetajavbotEn (inglés), que la usan por dentro. La pelea habla con ella
 * por el Puente.
 */
public abstract class RobotBase {

    Mando mando;

    // Solo las tres fachadas de este paquete pueden heredar de aquí.
    RobotBase() {
    }

    abstract void empezar();

    abstract void turno();

    abstract void ver(Avistado e);

    abstract void golpeado(Impacto g);

    abstract void acierto(Impacto g);

    abstract void pared();

    abstract void choque(Avistado e);

    abstract void finDeRonda();

    final Mando m() {
        if (mando == null) throw new IllegalStateException("El robot aún no está en la arena (no muevas nada en el constructor: usa alEmpezar).");
        return mando;
    }

    /** Lleva un ángulo a (-180, 180]. */
    static double normalizado(double grados) {
        if (Double.isNaN(grados) || Double.isInfinite(grados)) return 0;
        double g = grados % 360;
        if (g > 180) g -= 360;
        if (g <= -180) g += 360;
        return g;
    }

    final double anguloHasta(double x, double y) {
        return Math.toDegrees(Math.atan2(y - m().y(), x - m().x()));
    }

    final double distanciaHasta(double x, double y) {
        return Math.hypot(x - m().x(), y - m().y());
    }

    static String aTexto(Object o) {
        return String.valueOf(o);
    }
}
