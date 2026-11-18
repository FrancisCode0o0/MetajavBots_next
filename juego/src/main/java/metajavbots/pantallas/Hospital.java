package metajavbots.pantallas;

import metajavbots.pantalla.Pincel;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * HOSPITAL — Contra los jefes el entrenador no ayuda: está en el hospital,
 * agonizando en la camilla por una causa absurda que cambia cada vez («tomó
 * aceite en mal estado», «fue a la playa y se oxidó»…), con su gota a gota,
 * su monitor que pita y su termómetro. Y cuando el jugador gana, aparece
 * milagrosamente a felicitarlo: al final sí fue al combate.
 */
public final class Hospital {

    private Hospital() {
    }

    /** La causa de esta vez (cambia con la semilla: cada pelea contra un jefe, una). */
    public static String causa(long semilla) {
        String[] c = T.t("entrenador.hospital.causas").split(";");
        return c[Math.floorMod(semilla * 2654435761L, c.length)].strip();
    }

    /** Lo que dice el milagro (también cambia). */
    public static String milagro(long semilla) {
        String[] c = T.t("entrenador.milagro.frases").split(";");
        return c[Math.floorMod(semilla * 40503L + 7, c.length)].strip();
    }

    /** La camilla con el entrenador vendado, el gota a gota, el termómetro y el monitor que pita, en ese rectángulo. */
    public static void camilla(Pincel p, Entrenador e, float x, float y, float w, float h, float t) {
        float suelo = y + h * 0.82f, cx = x + w * 0.46f;
        // La camilla: colchón, sábana, patas con ruedas.
        p.caja(x + w * 0.12f, suelo - h * 0.22f, w * 0.68f, h * 0.1f, 8, 0xE8ECEF, 1);
        p.caja(x + w * 0.3f, suelo - h * 0.25f, w * 0.5f, h * 0.12f, 10, 0x9EC9E8, 1);
        p.rect(x + w * 0.16f, suelo - h * 0.12f, 6, h * 0.12f, 0x8A9199, 1);
        p.rect(x + w * 0.74f, suelo - h * 0.12f, 6, h * 0.12f, 0x8A9199, 1);
        p.circulo(x + w * 0.16f + 3, suelo, 7, 0x3A3F46, 1);
        p.circulo(x + w * 0.74f + 3, suelo, 7, 0x3A3F46, 1);
        // El entrenador, tumbado de lado, con la venda y una tirita cruzada; respira con dificultad.
        // La almohada, y la cabeza hundida en ella.
        p.caja(x + w * 0.14f, suelo - h * 0.3f, w * 0.17f, h * 0.1f, 14, 0xFFFFFF, 1);
        float r = Math.min(w, h) * 0.13f, cy = suelo - h * 0.25f - r * 0.45f + (float) Math.sin(t * 1.3f) * 2;
        CabezaDeEntrenador.dibujar(p, e, x + w * 0.24f, cy, r, Math.sin(t * 0.7f) > 0.6f, t);
        p.rect(x + w * 0.24f - r, cy - r * 0.55f, r * 2, r * 0.28f, 0xFFFFFF, 0.92f);
        p.linea(x + w * 0.24f + r * 0.2f, cy + r * 0.1f, x + w * 0.24f + r * 0.75f, cy + r * 0.5f, 5, 0xF2C29B, 1);
        p.linea(x + w * 0.24f + r * 0.75f, cy + r * 0.1f, x + w * 0.24f + r * 0.2f, cy + r * 0.5f, 5, 0xF2C29B, 1);
        // El termómetro en la boca, que sube y baja.
        float tx = x + w * 0.24f + r * 0.9f, ty = cy + r * 0.3f;
        p.linea(tx, ty, tx + r * 0.9f, ty - r * 0.35f, 4, 0xDDDDDD, 1);
        p.circulo(tx + r * 0.9f, ty - r * 0.35f, 4, 0xFF4D4D, 1);
        // El gota a gota: un soporte, una bolsa de aceite (¡de oliva!) y la gota que cae.
        float sx = x + w * 0.88f;
        p.rect(sx, y + h * 0.12f, 4, suelo - y - h * 0.12f, 0x8A9199, 1);
        p.caja(sx - 22, y + h * 0.14f, 40, h * 0.16f, 8, 0xD9C66B, 0.85f);
        float gota = (t * 0.9f) % 1f;
        p.circulo(sx + 2, y + h * 0.32f + gota * h * 0.18f, 3, 0xD9C66B, 1 - gota);
        p.linea(sx + 2, y + h * 0.3f, x + w * 0.62f, suelo - h * 0.22f, 2, 0xBFD7E3, 0.8f);
        // El monitor: la línea del pulso, con un pico cada segundo y el «bip».
        float mx = x + w * 0.55f, my = y + h * 0.08f, mw = w * 0.28f, mh = h * 0.2f;
        p.caja(mx, my, mw, mh, 8, 0x10161C, 1);
        p.bordeRedondeado(mx, my, mw, mh, 8, 0x5BD685, 0.7f, 1.5f);
        float antes = 0;
        for (int i = 1; i <= 40; i++) {
            float u = i / 40f, fase = (u * 2 + t) % 1f;
            float v = fase > 0.45f && fase < 0.5f ? -0.8f : fase > 0.5f && fase < 0.55f ? 0.5f : 0;
            p.linea(mx + 6 + (u - 1f / 40) * (mw - 12), my + mh / 2 + antes * mh * 0.4f, mx + 6 + u * (mw - 12), my + mh / 2 + v * mh * 0.4f, 2,
                    0x5BD685, 1);
            antes = v;
        }
        if ((t % 1f) < 0.25f) p.texto(T.t("entrenador.hospital.pitido"), mx + 8, my + mh + 4, 14, false, 0x5BD685, 1);
    }

    /**
     * El milagro: el entrenador baja del cielo entre destellos, con una tirita de recuerdo, y dice su frase.
     * «t» cuenta desde que apareció.
     */
    public static void milagro(Pincel p, Entrenador e, String frase, float x, float y, float w, float t) {
        float llega = Math.min(1, t / 1.2f);
        float cx = x + w / 2, cy = y - 120 + llega * 180 + (float) Math.sin(t * 3) * 4;
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5 + t * 1.5;
            float d = 60 + 12 * (float) Math.sin(t * 4 + i);
            p.circulo(cx + (float) Math.cos(a) * d, cy + (float) Math.sin(a) * d, 4, 0xFFD23F, 0.8f * llega);
        }
        p.circulo(cx, cy - 52, 26, 0xFFE680, 0.25f * llega);
        p.linea(cx - 22, cy - 50, cx + 22, cy - 50, 4, 0xFFE680, llega);
        CabezaDeEntrenador.dibujar(p, e, cx, cy, 40, t > 1.2f, t);
        p.linea(cx + 14, cy - 30, cx + 34, cy - 18, 5, 0xF2C29B, 1);
        if (llega < 1) return;
        float tt = 20, bw = Math.min(w - 20, 640);
        p.textoCentrado(T.t("entrenador.milagro.titulo"), cx, cy + 50, 30, true, 0xFFD23F, 1);
        float bh = p.altoDeParrafo(frase, bw - 24, tt) + 18;
        p.caja(cx - bw / 2, cy + 92, bw, bh, 10, 0xF5F3EA, 0.97f);
        p.parrafo(frase, cx - bw / 2 + 12, cy + 100, bw - 24, tt, 0x1E1E1E, 1);
    }
}
