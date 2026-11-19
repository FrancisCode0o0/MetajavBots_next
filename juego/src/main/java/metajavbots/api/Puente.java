package metajavbots.api;

/**
 * PUENTE — Por donde la pelea maneja a los robots y al Tope. Es interno: el
 * cargador de robots no deja que el código del jugador lo use.
 */
public final class Puente {

    private Puente() {
    }

    public static void conectar(RobotBase r, Mando m) {
        r.mando = m;
    }

    public static void empezar(RobotBase r) {
        r.empezar();
    }

    public static void turno(RobotBase r) {
        r.turno();
    }

    public static void ver(RobotBase r, Avistado e) {
        r.ver(e);
    }

    public static void golpeado(RobotBase r, Impacto g) {
        r.golpeado(g);
    }

    public static void acierto(RobotBase r, Impacto g) {
        r.acierto(g);
    }

    public static void pared(RobotBase r) {
        r.pared();
    }

    public static void choque(RobotBase r, Avistado e) {
        r.choque(e);
    }

    public static void finDeRonda(RobotBase r) {
        r.finDeRonda();
    }

    public static void equipo(RobotBase r, String de, String mensaje) {
        r.equipo(de, mensaje);
    }

    // ---------------------------------------------------------------- el Tope

    /** Empieza a contar en este hilo: hasta «maximo» pasos y «nanos» de tiempo. */
    public static void contar(long maximo, long nanos) {
        Tope.Cuenta c = new Tope.Cuenta();
        c.maximo = maximo;
        c.limiteNanos = nanos == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + nanos;
        Tope.CUENTA.set(c);
    }

    /** Deja de contar y devuelve los pasos dados. */
    public static long dejarDeContar() {
        Tope.Cuenta c = Tope.CUENTA.get();
        if (c != null) c.cancelada = true;
        Tope.CUENTA.remove();
        return c == null ? 0 : c.pasos;
    }

    /** Las líneas que ha ejecutado el código que se está contando, y la última (la traza); null si no hay cuenta. */
    public static java.util.BitSet lineas() {
        Tope.Cuenta c = Tope.CUENTA.get();
        if (c == null) return null;
        synchronized (c) {
            return (java.util.BitSet) c.lineas.clone();
        }
    }

    /** Las líneas ejecutadas, en orden (la traza paso a paso); vacío si no hay cuenta. */
    public static int[] secuencia() {
        Tope.Cuenta c = Tope.CUENTA.get();
        if (c == null) return new int[0];
        synchronized (c) {
            return java.util.Arrays.copyOf(c.orden, c.enOrden);
        }
    }

    public static int ultimaLinea() {
        Tope.Cuenta c = Tope.CUENTA.get();
        return c == null ? 0 : c.ultima;
    }

    /** Para lo que esté contando ese hilo (y los suyos): el siguiente paso lanza TopeDeCalculo. */
    public static Object cuentaActual() {
        return Tope.CUENTA.get();
    }

    public static void cancelar(Object cuenta) {
        if (cuenta instanceof Tope.Cuenta) ((Tope.Cuenta) cuenta).cancelada = true;
    }
}
