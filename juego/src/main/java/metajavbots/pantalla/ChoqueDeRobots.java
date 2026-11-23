package metajavbots.pantalla;

import metajavbots.sonido.Sonidos;

/**
 * CHOQUE DE ROBOTS — Lo que se ve y se oye cuando dos robots chocan: una chispa
 * breve en el punto de contacto y un golpe metálico según la fuerza (más fuerte
 * y más grave cuanto más deprisa iban). Mientras se empujan no se repite (como
 * mucho uno cada cuarto de segundo, salvo que el nuevo sea más fuerte), para no
 * tapar la pelea ni llenar la pantalla de chispas.
 */
final class ChoqueDeRobots {

    /** La velocidad de los dos juntos (por turno) a partir de la cual el choque suena con toda su fuerza. */
    static final double VELOCIDAD_PLENA = 12;
    private static final float ESPERA = 0.25f;
    private float ultimo = -1, fuerzaDelUltimo;

    /** La fuerza del choque (0-1) según la velocidad de los dos juntos. */
    static float fuerza(double velocidad) {
        return (float) Math.max(0, Math.min(1, velocidad / VELOCIDAD_PLENA));
    }

    void chocar(Efectos efectos, Sonidos sonidos, float x, float y, double velocidad, float tiempo, float escala) {
        float f = fuerza(velocidad);
        if (tiempo - ultimo >= 0 && tiempo - ultimo < ESPERA && f <= fuerzaDelUltimo) return;
        ultimo = tiempo;
        fuerzaDelUltimo = f;
        efectos.chispas(x, y, 3 + Math.round(f * 9), 0xFFE9B0);
        if (f > 0.5f) efectos.anadir(Efectos.Tipo.DESTELLO, x, y, 0, 0, 0.12f, 34 * escala * f, 0xFFF6DC);
        if (sonidos != null) sonidos.tocar("choque", 0.25f + 0.75f * f, 1.25f - 0.4f * f);
    }
}
