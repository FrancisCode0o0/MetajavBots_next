package metajavbots.sonido;

/**
 * TIMBRE — Cómo suena una voz, para que cada entrenador tenga la suya aunque
 * Piper solo traiga una por idioma: el tono (1 = el de la voz; 0,8 más grave,
 * 1,2 más agudo), la velocidad (1 = la normal) y cuánto efecto robótico lleva
 * (0 = la voz limpia, 1 = el del locutor).
 */
public record Timbre(double tono, double velocidad, double robot) {

    /** El del locutor robot loco. */
    public static final Timbre LOCUTOR = new Timbre(1, 1, 1);

    public Timbre {
        tono = Math.max(0.6, Math.min(1.5, tono));
        velocidad = Math.max(0.6, Math.min(1.5, velocidad));
        robot = Math.max(0, Math.min(1, robot));
    }

    /** De «tono velocidad robot» (p. ej. «0.85 0.9 0.7»); si falta o está mal, el del locutor. */
    public static Timbre de(String s) {
        if (s == null || s.isBlank()) return LOCUTOR;
        try {
            String[] p = s.strip().split("\\s+");
            return new Timbre(Double.parseDouble(p[0]), p.length > 1 ? Double.parseDouble(p[1]) : 1, p.length > 2 ? Double.parseDouble(p[2]) : 1);
        } catch (NumberFormatException e) {
            return LOCUTOR;
        }
    }

    /**
     * Sube o baja el tono remuestreando (lo que también cambia la duración:
     * Piper lee más despacio para compensarlo, ver VozPiper).
     */
    static float[] cambiarTono(float[] a, double tono) {
        if (Math.abs(tono - 1) < 1e-3) return a;
        return VozPiper.aFrecuenciaDelJuego(a, (int) Math.round(Sintetizador.FRECUENCIA * tono));
    }
}
