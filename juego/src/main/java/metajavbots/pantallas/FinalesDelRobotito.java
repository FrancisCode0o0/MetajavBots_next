package metajavbots.pantallas;

import metajavbots.pantalla.Pincel;
import metajavbots.textos.T;

/**
 * FINALES DEL ROBOTITO — Lo que hace el robotito de los ejercicios al acabar,
 * con variedad (uno al azar cada vez). Si acierta: baila con confeti, se
 * alza de un salto y aterriza triunfal entre rayos de luz, o pone ojos rojos
 * de máquina malvada («MUAJAJA…») antes de volver a sonreír. Si falla: se
 * marea, se le saltan los engranajes y los muelles y se desmorona hasta
 * apagarse, o hace cortocircuito y le salta la cabeza con un muelle.
 */
public final class FinalesDelRobotito {

    private FinalesDelRobotito() {
    }

    static final int VARIANTES = 3;

    /** Los seis finales a la vez, para las capturas: arriba los de acertar, abajo los de fallar. */
    public static void muestra(Pincel p, float x, float y, float w, float t) {
        for (int v = 0; v < VARIANTES; v++) {
            float cx = x + w * (v + 0.5f) / VARIANTES;
            bien(p, cx, y + 200, t, v);
            mal(p, cx, y + 430, t, v);
        }
    }
    private static final float K = 1.6f;
    private static final int AZUL = 0x3FA9F5, APAGADO = 0x6E8FAF;

    /** Ha acertado: «t», los segundos desde que acabó; «v», la variante. */
    static void bien(Pincel p, float x, float suelo, float t, int v) {
        switch (v % VARIANTES) {
            case 1: alzarse(p, x, suelo, t); break;
            case 2: maquinaMalvada(p, x, suelo, t); break;
            default: bailar(p, x, suelo, t); break;
        }
    }

    /** Se ha equivocado. */
    static void mal(Pincel p, float x, float suelo, float t, int v) {
        switch (v % VARIANTES) {
            case 1: desmontarse(p, x, suelo, t); break;
            case 2: cortocircuito(p, x, suelo, t); break;
            default: marearse(p, x, suelo, t); break;
        }
    }

    // ================================================================ al acertar

    private static void bailar(Pincel p, float x, float suelo, float t) {
        float salto = Math.abs((float) Math.sin(t * 8)) * 16;
        float baile = (float) Math.sin(t * 6) * 8;
        AnimacionDelCaso.robotito(p, x + baile, suelo - salto, (float) (Math.sin(t * 12) + 1) / 2, AnimacionDelCaso.VERDE, AZUL);
        int[] colores = {0xFFD23F, 0x5BD685, 0xFF5E5B, 0x7B61FF};
        for (int i = 0; i < 6; i++) {
            double a = t * 3 + i * Math.PI / 3;
            p.circulo(x + baile + (float) Math.cos(a) * 46, suelo - 60 - salto + (float) Math.sin(a) * 22, 3.5f, colores[i % 4], 1);
        }
    }

    /** Se agacha, salta muy alto y aterriza con el brazo en alto; detrás, rayos de luz y estrellas. */
    private static void alzarse(Pincel p, float x, float suelo, float t) {
        float alto;
        if (t < 0.3f) alto = -6 * (t / 0.3f);
        else if (t < 1.1f) {
            float f = (t - 0.3f) / 0.8f;
            alto = (float) Math.sin(f * Math.PI) * 70;
        } else alto = 0;
        if (t > 1.1f) {
            for (int i = 0; i < 10; i++) {
                double a = t * 0.6 + i * Math.PI / 5;
                p.linea(x, suelo - 50, x + (float) Math.cos(a) * 90, suelo - 50 + (float) Math.sin(a) * 60, 6, 0xFFE27A, 0.3f);
            }
            for (int i = 0; i < 5; i++) {
                float f = ((t * 0.7f + i / 5f) % 1f);
                p.textoCentrado("★", x - 50 + i * 25, suelo - 90 - f * 50, 16, true, 0xFFD23F, 1 - f);
            }
        }
        AnimacionDelCaso.robotito(p, x, suelo - Math.max(0, alto), t < 0.3f ? 0 : 1, AnimacionDelCaso.VERDE, AZUL);
    }

    /** Los ojos se le ponen rojos y brillantes, escanea con un rayo y se ríe; luego vuelve a ser bueno y guiña. */
    private static void maquinaMalvada(Pincel p, float x, float suelo, float t) {
        boolean malvado = t > 0.4f && t < 2.6f;
        AnimacionDelCaso.robotito(p, x, suelo, malvado ? 0.6f : 1, malvado ? 0xFF1A1A : AnimacionDelCaso.VERDE, malvado ? 0x4A5566 : AZUL);
        if (!malvado) return;
        float ojoY = suelo - 44 * K;
        p.brillo(x, ojoY, 26, 0xFF2020, 0.5f + 0.3f * (float) Math.sin(t * 10));
        float barrido = (float) Math.sin(t * 2.5) * 70;
        p.linea(x + 2.5f * K, ojoY, x + 90 + barrido * 0.2f, ojoY + barrido, 2, 0xFF3030, 0.6f);
        p.texto(T.idioma.elegir("MUAJAJA…", "MUAHAHA…", "MWAHAHA…"), x + 30, suelo - 110, 18, true, 0xFF4040, Math.min(1, (t - 0.4f) * 3));
    }

    // ================================================================ al fallar

    private static void marearse(Pincel p, float x, float suelo, float t) {
        float temblor = (float) Math.sin(t * 40) * 2;
        AnimacionDelCaso.robotito(p, x + temblor, suelo + 6, 0, AnimacionDelCaso.ROJO, APAGADO);
        for (int i = 0; i < 3; i++) {
            double a = t * 5 + i * 2 * Math.PI / 3;
            p.texto("★", x + (float) Math.cos(a) * 26 - 6, suelo - 100 + (float) Math.sin(a) * 8, 16, true, 0xFFD23F, 1);
        }
        float humo = (t * 20) % 30;
        p.circulo(x + 14, suelo - 90 - humo, 6 + humo * 0.2f, 0x8A919C, Math.max(0, 0.5f - humo / 60));
    }

    /**
     * Tiembla, le saltan engranajes y muelles, el cuerpo se le hunde y la cabeza se le cae al suelo; los ojos se le
     * apagan poco a poco.
     */
    private static void desmontarse(Pincel p, float x, float suelo, float t) {
        float caida = Math.max(0, Math.min(1, (t - 0.6f) / 0.6f));
        float temblor = t < 0.6f ? (float) Math.sin(t * 50) * 2.5f : 0;
        // Las ruedas se quedan; el cuerpo baja; la cabeza rueda al lado.
        p.elipse(x, suelo + 2, 16 * K, 4 * K, 0, 0x000000, 0.3f);
        p.circulo(x - 9 * K - caida * 6, suelo - 5 * K, 5 * K, 0x2A2F38, 1);
        p.circulo(x + 9 * K + caida * 6, suelo - 5 * K, 5 * K, 0x2A2F38, 1);
        float cuerpoY = suelo - 34 * K + caida * 18 * K;
        p.caja(x - 13 * K + temblor, cuerpoY, 26 * K, 26 * K - caida * 8 * K, 5 * K, APAGADO, 1);
        float cx = x + temblor + caida * 26 * K, cy = suelo - 52 * K + caida * 36 * K;
        p.caja(cx - 9 * K, cy, 18 * K, 16 * K, 4 * K, Pincel.aclarar(APAGADO, 0.15f), 1);
        p.caja(cx - 6 * K, cy + 5 * K, 12 * K, 6 * K, 2 * K, 0x111318, 1);
        float luz = Math.max(0, 1 - Math.max(0, t - 1.2f) * 1.2f);
        int ojo = luz > 0.05f && (t < 1.2f || (t * 9) % 1f < 0.7f) ? AnimacionDelCaso.ROJO : 0x3A3F46;
        p.circulo(cx - 2.5f * K, cy + 8 * K, 1.6f * K, ojo, Math.max(0.3f, luz));
        p.circulo(cx + 2.5f * K, cy + 8 * K, 1.6f * K, ojo, Math.max(0.3f, luz));
        // Engranajes y muelles que saltan y caen.
        for (int i = 0; i < 6; i++) {
            float vx = (i - 2.5f) * 26, vy = -140 - (i % 3) * 30;
            float tt = Math.min(t, 1.4f);
            float px = x + vx * tt, py = suelo - 40 * K + vy * tt + 300 * tt * tt;
            py = Math.min(py, suelo - 4);
            if (i % 2 == 0) engranaje(p, px, py, 6 + i % 3 * 2, t * 6 + i);
            else muelle(p, px, py);
        }
        if (t > 1.6f) p.texto("…", x - 6, suelo - 70, 20, true, 0x8A919C, Math.min(1, (t - 1.6f)));
    }

    /** Chispas y humo; la cabeza le salta hacia arriba con un muelle y rebota. */
    private static void cortocircuito(Pincel p, float x, float suelo, float t) {
        float rebote = (float) (Math.abs(Math.sin(t * 7)) * Math.exp(-t * 1.2) * 40);
        // El cuerpo sin cabeza (la cabeza va encima del muelle).
        p.elipse(x, suelo + 2, 16 * K, 4 * K, 0, 0x000000, 0.3f);
        p.circulo(x - 9 * K, suelo - 5 * K, 5 * K, 0x2A2F38, 1);
        p.circulo(x + 9 * K, suelo - 5 * K, 5 * K, 0x2A2F38, 1);
        p.caja(x - 13 * K, suelo - 34 * K, 26 * K, 26 * K, 5 * K, APAGADO, 1);
        p.linea(x + 13 * K, suelo - 26 * K, x + 25 * K, suelo - 20 * K, 3 * K, Pincel.oscurecer(APAGADO, 0.2f), 1);
        float baseY = suelo - 34 * K;
        for (int k = 0; k < 6; k++) {
            float y0 = baseY - k * rebote / 6, y1 = baseY - (k + 1) * rebote / 6;
            p.linea(x + (k % 2 == 0 ? -5 : 5), y0, x + (k % 2 == 0 ? 5 : -5), y1, 2, 0xC9CED6, 1);
        }
        p.caja(x - 9 * K, baseY - rebote - 16 * K, 18 * K, 16 * K, 4 * K, Pincel.aclarar(APAGADO, 0.15f), 1);
        p.caja(x - 6 * K, baseY - rebote - 11 * K, 12 * K, 6 * K, 2 * K, 0x111318, 1);
        p.texto("x", x - 5 * K, baseY - rebote - 13 * K, 10 * K, true, AnimacionDelCaso.ROJO, 1);
        p.texto("x", x + 1 * K, baseY - rebote - 13 * K, 10 * K, true, AnimacionDelCaso.ROJO, 1);
        if ((t * 8) % 1f < 0.5f) {
            for (int i = 0; i < 4; i++) {
                double a = t * 17 + i * 1.6;
                p.linea(x + 14 * K, suelo - 26 * K, x + 14 * K + (float) Math.cos(a) * 16, suelo - 26 * K + (float) Math.sin(a) * 16, 2, 0xFFE27A, 0.9f);
            }
        }
        float humo = (t * 18) % 34;
        p.circulo(x - 8, suelo - 70 - humo, 7 + humo * 0.25f, 0x5A5F66, Math.max(0, 0.55f - humo / 60));
    }

    private static void engranaje(Pincel p, float x, float y, float r, float giro) {
        for (int k = 0; k < 8; k++) {
            double a = giro + k * Math.PI / 4;
            p.circulo(x + (float) Math.cos(a) * r, y + (float) Math.sin(a) * r, r * 0.3f, 0xB8C0C8, 1);
        }
        p.circulo(x, y, r * 0.9f, 0xB8C0C8, 1);
        p.circulo(x, y, r * 0.35f, 0x3A3F46, 1);
    }

    private static void muelle(Pincel p, float x, float y) {
        for (int k = 0; k < 5; k++) p.linea(x - 5, y - k * 3, x + 5, y - k * 3 - 1.5f, 1.6f, 0xC9CED6, 1);
    }
}
