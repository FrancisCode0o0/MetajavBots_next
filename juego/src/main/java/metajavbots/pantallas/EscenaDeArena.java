package metajavbots.pantallas;

import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * ESCENA DE ARENA — Los ejercicios que enseñan el juego (res/ejercicios/24_juego.txt) se ven en una arena de
 * verdad. «giro»: vista desde arriba; el robot gira la torreta los grados que devuelve el código y dispara: si
 * eran los buenos, revienta el muñeco de prácticas; si no, el láser va a dar a un pato de goma que pasaba por ahí
 * (o al árbitro). «avance»: de lado; el robot avanza lo que devuelve el código hacia la bandera: si llega justo,
 * clava la bandera; si se pasa, cae a la piscina de aceite; si se queda corto, se le acaba la pila a dos palmos.
 * «f» va de 0 a 1.
 */
final class EscenaDeArena {

    private static final int ROJO = AnimacionDelCaso.ROJO, VERDE = AnimacionDelCaso.VERDE;

    private EscenaDeArena() {
    }

    private static float suave(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Desde arriba: la torreta gira de 0° a «obtenido» y dispara; el muñeco está en «esperado». */
    static void giro(Ui ui, float x, float y, float w, float h, long esperado, long obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float r = Math.min(w * 0.42f, h * 0.36f), cx = x + w / 2, cy = y + h * 0.5f;
        // La arena: un círculo con su raya y las marcas de 0°, 90°, 180° y -90°.
        p.circulo(cx, cy, r + 6, 0x3A3F46, 1);
        p.circulo(cx, cy, r, 0x5B6470, 1);
        for (int g = 0; g < 360; g += 90) {
            double a = Math.toRadians(g);
            p.linea(cx + (float) Math.cos(a) * (r - 14), cy - (float) Math.sin(a) * (r - 14), cx + (float) Math.cos(a) * r,
                    cy - (float) Math.sin(a) * r, 2, 0x8A9199, 1);
            p.texto(g > 180 ? (g - 360) + "°" : g + "°", cx + (float) Math.cos(a) * (r + 12) - 14, cy - (float) Math.sin(a) * (r + 12) - 8, 13, false,
                    0xBFC5CE, 1);
        }
        // El muñeco de prácticas, en el ángulo bueno.
        double ae = Math.toRadians(esperado);
        float mx = cx + (float) Math.cos(ae) * r * 0.78f, my = cy - (float) Math.sin(ae) * r * 0.78f;
        boolean revienta = bien && f > 0.72f;
        if (!revienta) {
            p.circulo(mx, my, 18, 0xFFFFFF, 1);
            p.circulo(mx, my, 12, ROJO, 1);
            p.circulo(mx, my, 6, 0xFFFFFF, 1);
        } else {
            float u = (f - 0.72f) / 0.28f;
            for (int k = 0; k < 10; k++) {
                double a = k * Math.PI / 5;
                p.circulo(mx + (float) Math.cos(a) * 40 * u, my + (float) Math.sin(a) * 40 * u, 6 * (1 - u) + 2, k % 2 == 0 ? ROJO : 0xFFFFFF, 1 - u);
            }
            p.textoCentrado(T.t("animacion.diana"), mx, my - 50, 22, true, 0xFFD23F, 1);
        }
        // La torreta gira hasta lo que dijo el código.
        float giro = (float) (obtenido * suave(f / 0.5f));
        double ag = Math.toRadians(giro);
        p.circulo(cx, cy, 26, 0x2F80ED, 1);
        p.linea(cx, cy, cx + (float) Math.cos(ag) * 44, cy - (float) Math.sin(ag) * 44, 9, 0xBFD7E3, 1);
        p.circulo(cx, cy, 12, 0x10161C, 1);
        if (f > 0.5f && f < 0.75f) {
            float largo = (r * 0.78f) * ((f - 0.5f) / 0.25f) * (bien ? 1 : 1.25f);
            p.linea(cx, cy, cx + (float) Math.cos(ag) * largo, cy - (float) Math.sin(ag) * largo, 4, bien ? 0x5BD6FF : 0xFF7A45, 1);
        }
        // Si falla: el láser se lleva por delante un pato de goma (que sale volando dando vueltas) donde apuntó.
        if (!bien && f > 0.72f) {
            float u = (f - 0.72f) / 0.28f;
            float px = cx + (float) Math.cos(ag) * r * (0.95f + u), py = cy - (float) Math.sin(ag) * r * (0.95f + u) - u * 60;
            p.circulo(px, py, 14, 0xFFD23F, 1);
            p.circulo(px + 10, py - 12, 9, 0xFFD23F, 1);
            p.triangulo(px + 18, py - 13, px + 18, py - 7, px + 26, py - 10, 0xFF7A45, 1);
            p.textoCentrado(T.t("animacion.cuac"), px, py - 40, 20, true, ROJO, 1);
        }
        p.texto(T.f("animacion.giro", obtenido, esperado), x + 14, y + h - 56, ui.letra * 0.8f, true, bien ? VERDE : ROJO, 1);
    }

    /** De lado: el robot avanza «obtenido» hacia la bandera, que está a «esperado» (negativo: hacia atrás). */
    static void avance(Ui ui, float x, float y, float suelo, float w, long esperado, long obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float escala = (w * 0.38f) / Math.max(1, Math.max(Math.abs(esperado), Math.abs(obtenido)));
        float x0 = x + w / 2;
        // La pista, la bandera y (al otro lado de la bandera) la piscina de aceite.
        p.rect(x + 10, suelo, w - 20, 4, 0x8A9199, 1);
        float bx = x0 + esperado * escala;
        int lado = esperado >= 0 ? 1 : -1;
        p.caja(bx + lado * 46 - 40, suelo + 2, 80, 16, 6, 0x3B2F1E, 1);
        p.texto("~ ~ ~", bx + lado * 46 - 26, suelo + 2, 13, true, 0x8A6A3A, 1);
        boolean clavada = bien && f > 0.8f;
        p.rect(bx - 1, suelo - 60, 3, 60, 0xE8ECEF, 1);
        float ondea = (float) Math.sin(f * 30) * (clavada ? 6 : 2);
        p.triangulo(bx + 2, suelo - 60, bx + 2, suelo - 40, bx + 32 + ondea, suelo - 50, clavada ? VERDE : ROJO, 1);
        // El robot avanza (hasta f = 0,8); luego, lo que le pase.
        float avance = (float) (obtenido * suave(f / 0.8f)) * escala;
        float rx = x0 + avance, rs = suelo;
        long pasado = (obtenido - esperado) * lado;
        if (!bien && pasado > 0 && f > 0.8f) {
            // Se pasa: resbala en la piscina de aceite y se hunde.
            float u = (f - 0.8f) / 0.2f;
            rs = suelo + u * 30;
            p.textoCentrado(T.t("animacion.chof"), rx, suelo - 110, 22, true, ROJO, 1);
        } else if (!bien && f > 0.8f) {
            // Se queda corto: sin pila, echando humo.
            for (int k = 0; k < 3; k++) p.circulo(rx + k * 6, suelo - 80 - k * 14 - (f - 0.8f) * 60, 8 + k * 3, 0x8A9199, 0.6f);
            p.textoCentrado(T.t("animacion.corto"), rx, suelo - 130, 20, true, ROJO, 1);
        }
        AnimacionDelCaso.robotito(p, rx, rs, clavada ? 1 : 0.2f, clavada, 0x2F80ED);
        p.texto(T.f("animacion.avanza", obtenido, esperado), x + 14, suelo - 40, ui.letra * 0.8f, true, bien ? VERDE : ROJO, 1);
    }
}
