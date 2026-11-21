package metajavbots.pantalla;

import java.util.Random;

import metajavbots.combate.Arena;

/**
 * DIBUJO DE ARENAS — Lo que se ve de las arenas con trampas: el fondo de cada
 * una (la chatarrería, la cocina gigante, la pista de hielo y el tejado con
 * lluvia), el color de su suelo y cada trampa u objeto, animado (los pinchos
 * suben y bajan, las sierras giran, el imán late, el carrito rueda…).
 */
final class DibujoDeArenas {

    private DibujoDeArenas() {
    }

    /** ¿Es una de las arenas de aquí? */
    static boolean esNueva(VistaDeArena.Escenario e) {
        switch (e) {
            case CHATARRERIA: case COCINA: case HIELO: case TEJADO: return true;
            default: return false;
        }
    }

    /** Los colores del suelo: las baldosas claras, las oscuras y las líneas. */
    static int[] suelo(VistaDeArena.Escenario e) {
        switch (e) {
            case CHATARRERIA: return new int[]{0x7A6A58, 0x5E5040, 0x8C7A62};
            case COCINA: return new int[]{0xF1EDE4, 0xC9C2B4, 0xB3AA98};
            case HIELO: return new int[]{0xDDF1FB, 0xB7DDF0, 0xFFFFFF};
            case TEJADO: return new int[]{0x4A3B3B, 0x352A2A, 0x5E4A48};
            default: return null;
        }
    }

    /** El fondo (lo de detrás de la arena), de (x, y) a (x + w, cielo). */
    static void fondo(Pincel p, VistaDeArena.Escenario e, float x, float y, float w, float h, float t) {
        float cielo = y + h * 0.32f;
        Random r = new Random(e.ordinal() * 31L);
        switch (e) {
            case CHATARRERIA: {
                p.degradado(x - 20, y - 20, w + 40, cielo - y + 30, 0x6B3A2A, 0xE38B4F, 1);
                p.circulo(x + w * 0.82f, y + h * 0.12f, h * 0.06f, 0xFFD9A0, 0.9f);
                // Montañas de chatarra, con piezas sueltas que asoman.
                for (int i = 0; i < 7; i++) {
                    float mx = x + i * w / 6f - w * 0.05f, mw = w * (0.16f + r.nextFloat() * 0.1f), mh = h * (0.08f + r.nextFloat() * 0.1f);
                    p.poligono(new float[]{mx - mw / 2, cielo, mx, cielo - mh, mx + mw / 2, cielo}, Pincel.mezcla(0x3A2A22, 0x5A4232, i / 7f), 1);
                    for (int k = 0; k < 3; k++) {
                        float px = mx - mw * 0.3f + r.nextFloat() * mw * 0.6f, py = cielo - mh * (0.2f + r.nextFloat() * 0.5f);
                        p.circulo(px, py, 4 + r.nextFloat() * 6, 0x7C6A5A, 1);
                    }
                }
                // Una grúa con su gancho, que se balancea.
                float gx = x + w * 0.15f;
                p.rect(gx, y + h * 0.04f, 8, cielo - y - h * 0.04f, 0x2A1E18, 1);
                p.rect(gx, y + h * 0.04f, w * 0.18f, 6, 0x2A1E18, 1);
                float gancho = (float) Math.sin(t * 0.8) * 12;
                p.linea(gx + w * 0.16f, y + h * 0.04f, gx + w * 0.16f + gancho, y + h * 0.16f, 2, 0x2A1E18, 1);
                p.aro(gx + w * 0.16f + gancho, y + h * 0.17f, 8, 3, 0x2A1E18, 1);
                break;
            }
            case COCINA: {
                p.degradado(x - 20, y - 20, w + 40, cielo - y + 30, 0xF7F3EA, 0xE4DCCB, 1);
                // Azulejos y, encima, una cuchara y un tenedor gigantes colgados.
                for (int i = 0; i < 24; i++) {
                    for (int k = 0; k < 6; k++) {
                        if ((i + k) % 2 == 0) p.rect(x + i * w / 24f, y + k * (cielo - y) / 6f, w / 24f - 2, (cielo - y) / 6f - 2, 0xD6EAF2, 1);
                    }
                }
                float cx = x + w * 0.2f;
                p.rect(cx - 4, y, 8, h * 0.16f, 0x9AA3AD, 1);
                p.elipse(cx, y + h * 0.2f, 22, 34, 0, 0xB7C0CA, 1);
                float fx = x + w * 0.8f;
                p.rect(fx - 4, y, 8, h * 0.17f, 0x9AA3AD, 1);
                for (int k = -1; k <= 1; k++) p.rect(fx + k * 10 - 3, y + h * 0.17f, 6, h * 0.08f, 0x9AA3AD, 1);
                // Un fogón con su llama azul al fondo.
                p.caja(x + w * 0.45f, cielo - h * 0.07f, w * 0.1f, h * 0.07f, 6, 0x3A3F46, 1);
                for (int k = 0; k < 5; k++) {
                    float a = 0.5f + 0.5f * (float) Math.sin(t * 9 + k);
                    p.circulo(x + w * 0.465f + k * w * 0.017f, cielo - h * 0.075f, 5 + a * 3, 0x4FA3FF, 0.7f);
                }
                break;
            }
            case HIELO: {
                p.degradado(x - 20, y - 20, w + 40, cielo - y + 30, 0x8EC5E8, 0xE6F4FB, 1);
                for (int i = 0; i < 5; i++) {
                    float mx = x + i * w / 4f, mh = h * (0.12f + r.nextFloat() * 0.08f), mw = w * 0.32f;
                    p.poligono(new float[]{mx - mw / 2, cielo, mx, cielo - mh, mx + mw / 2, cielo}, 0xB9D6E8, 1);
                    p.poligono(new float[]{mx - mw * 0.12f, cielo - mh * 0.76f, mx, cielo - mh, mx + mw * 0.12f, cielo - mh * 0.76f}, 0xFFFFFF, 1);
                }
                // Nieve que cae despacio.
                for (int i = 0; i < 60; i++) {
                    float sx = x + (r.nextFloat() * w + t * 12 * (0.5f + r.nextFloat())) % w;
                    float sy = y + (r.nextFloat() * h + t * 30 * (0.6f + r.nextFloat())) % (cielo - y);
                    p.circulo(sx, sy, 1.5f + r.nextFloat() * 1.5f, 0xFFFFFF, 0.8f);
                }
                break;
            }
            case TEJADO: {
                p.degradado(x - 20, y - 20, w + 40, cielo - y + 30, 0x0A0E1A, 0x1E2438, 1);
                // La ciudad de noche, con ventanas encendidas.
                for (int i = 0; i < 18; i++) {
                    float bw = 40 + r.nextFloat() * 60, bh = 40 + r.nextFloat() * h * 0.2f, bx = x + r.nextFloat() * w;
                    p.rect(bx, cielo - bh, bw, bh + 4, 0x141826, 1);
                    for (int k = 0; k < 5; k++) {
                        if (r.nextFloat() < 0.6f) p.rect(bx + 5 + r.nextFloat() * (bw - 12), cielo - bh + 6 + r.nextFloat() * (bh - 14), 5, 4, 0xFFD27A, 0.7f);
                    }
                }
                // Un relámpago de vez en cuando.
                if (t % 7 < 0.12f) p.rect(x - 20, y - 20, w + 40, cielo - y + 30, 0xFFFFFF, 0.25f);
                break;
            }
            default:
                break;
        }
    }

    /** La lluvia del tejado, por encima de todo (se dibuja al final). */
    static void lluvia(Pincel p, VistaDeArena.Escenario e, float x, float y, float w, float h, float t) {
        if (e != VistaDeArena.Escenario.TEJADO) return;
        Random r = new Random(77);
        for (int i = 0; i < 110; i++) {
            float lx = x + (r.nextFloat() * w + t * 60) % w, ly = y + (r.nextFloat() * h + t * (500 + r.nextFloat() * 200)) % h;
            p.linea(lx, ly, lx - 4, ly + 16, 1.2f, 0xA9C4E8, 0.45f);
        }
    }

    /** Las trampas planas (en el suelo): pinchos, sierras, imán, foso, rayo y restos breves. Antes que los robots. */
    static void delSuelo(Pincel p, VistaDeArena v, Arena arena, float t) {
        for (Arena.Trampa tr : arena.trampas()) {
            float cx = v.sx(tr.x, tr.y), cy = v.sy(tr.y), rx = (float) tr.radio * v.escala(tr.y), ry = (float) tr.radio * v.escalaY();
            if (tr.destruida()) {
                if (tr.restos() > 0) restos(p, cx, cy, rx, ry, tr);
                continue;
            }
            if (tr.solido) continue;
            switch (tr.tipo) {
                case "foso":
                    p.elipse(cx, cy, rx * 1.08f, ry * 1.08f, 0, 0x8C9AA6, 1);
                    p.elipse(cx, cy, rx, ry, 0, 0x05070A, 1);
                    p.elipse(cx, cy - ry * 0.15f, rx * 0.8f, ry * 0.6f, 0, 0x101722, 1);
                    break;
                case "iman":
                    for (int k = 0; k < 3; k++) {
                        float f = ((t * 0.6f + k / 3f) % 1f);
                        p.elipse(cx, cy, rx * (1 - f), ry * (1 - f), 0, 0x6FA8FF, 0.05f + 0.25f * f);
                    }
                    p.elipse(cx, cy, rx * 0.16f, ry * 0.16f, 0, 0xD94848, 1);
                    p.elipse(cx, cy, rx * 0.08f, ry * 0.08f, 0, 0x3A4CC0, 1);
                    break;
                case "pinchos":
                    pinchos(p, cx, cy, rx, ry, tr.salida(), tr.peligroso());
                    break;
                case "sierra":
                    sierra(p, cx, cy, rx, ry, t * 9);
                    break;
                case "rayo":
                    rayo(p, cx, cy, rx, ry, tr.peligroso(), t);
                    break;
                case "aceite": {
                    // Un charco negro con brillos de colores; se va secando.
                    float a = tr.vida() < 0 ? 1 : Math.min(1, tr.vida() / 60f);
                    p.elipse(cx, cy, rx, ry, 0, 0x14121C, 0.85f * a);
                    p.elipse(cx - rx * 0.2f, cy - ry * 0.2f, rx * 0.5f, ry * 0.3f, 0, 0x9B7BFF, 0.25f * a);
                    p.elipse(cx + rx * 0.25f, cy + ry * 0.1f, rx * 0.35f, ry * 0.2f, 0, 0x4FD8C0, 0.2f * a);
                    break;
                }
                case "pegamento": {
                    float a = tr.vida() < 0 ? 1 : Math.min(1, tr.vida() / 60f);
                    p.elipse(cx, cy, rx, ry, 0, 0x4FB548, 0.8f * a);
                    for (int k = 0; k < 5; k++) {
                        float f = (t * 0.7f + k / 5f) % 1f;
                        p.circulo(cx + (k - 2) * rx * 0.3f, cy - ry * 0.2f, rx * 0.08f * (0.5f + f), 0xB9F29A, (1 - f) * 0.8f * a);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private static void pinchos(Pincel p, float cx, float cy, float rx, float ry, float salida, boolean peligro) {
        p.elipse(cx, cy, rx, ry, 0, 0x2A2E36, 1);
        p.elipse(cx, cy, rx * 0.9f, ry * 0.9f, 0, peligro ? 0x5A2020 : 0x3A3F48, 1);
        // Una rejilla de agujeros; los pinchos salen de ellos.
        for (int i = -2; i <= 2; i++) {
            for (int k = -1; k <= 1; k++) {
                float px = cx + i * rx * 0.32f, py = cy + k * ry * 0.5f;
                if ((i * i) / 4.4f + (k * k) / 1.2f > 1) continue;
                p.elipse(px, py, rx * 0.06f, ry * 0.06f, 0, 0x101216, 1);
                if (salida > 0) {
                    float alto = rx * 0.35f * salida;
                    p.poligono(new float[]{px - rx * 0.05f, py, px, py - alto, px + rx * 0.05f, py}, 0xCFD6DE, 1);
                    p.linea(px, py - alto, px + rx * 0.02f, py - alto * 0.5f, 1, 0xFFFFFF, 0.6f);
                }
            }
        }
        if (!peligro && salida <= 0) p.elipse(cx, cy, rx, ry, 0, 0xFFC857, 0.08f);
    }

    private static void sierra(Pincel p, float cx, float cy, float rx, float ry, float giro) {
        p.elipse(cx, cy, rx * 1.05f, ry * 1.05f, 0, 0x1A1D22, 0.9f);
        int dientes = 16;
        float[] v = new float[dientes * 4];
        for (int k = 0; k < dientes * 2; k++) {
            double a = giro + k * Math.PI / dientes;
            float rr = k % 2 == 0 ? 1f : 0.82f;
            v[k * 2] = cx + (float) Math.cos(a) * rx * rr;
            v[k * 2 + 1] = cy + (float) Math.sin(a) * ry * rr;
        }
        p.poligono(v, 0xB8C0C8, 1);
        p.elipse(cx, cy, rx * 0.55f, ry * 0.55f, 0, 0x8A929C, 1);
        p.elipse(cx, cy, rx * 0.15f, ry * 0.15f, 0, 0x3A3F46, 1);
        p.linea(cx, cy, cx + (float) Math.cos(giro) * rx * 0.5f, cy + (float) Math.sin(giro) * ry * 0.5f, 2, 0xFFFFFF, 0.5f);
    }

    private static void rayo(Pincel p, float cx, float cy, float rx, float ry, boolean activo, float t) {
        p.elipse(cx, cy, rx, ry, 0, activo ? 0x8FD3FF : 0x4F6A8A, activo ? 0.35f : 0.15f);
        if (!activo) {
            p.elipse(cx, cy, rx * 0.2f, ry * 0.2f, 0, 0x9FB8D8, 0.6f);
            return;
        }
        Random r = new Random((long) (t * 20));
        for (int k = 0; k < 5; k++) {
            float x0 = cx, y0 = cy - rx * 2.2f;
            for (int s = 0; s < 5; s++) {
                float x1 = cx + (r.nextFloat() - 0.5f) * rx * 1.4f, y1 = y0 + rx * 0.45f;
                p.linea(x0, y0, x1, y1, 2.5f, 0xE8F6FF, 0.9f);
                x0 = x1;
                y0 = y1;
            }
        }
    }

    /** Restos breves en el suelo tras romperse un barril o una chimenea. */
    private static void restos(Pincel p, float cx, float cy, float rx, float ry, Arena.Trampa tr) {
        float a = Math.min(1f, tr.restos() / 20f);
        boolean barril = tr.tipo.equals("barril");
        int base = barril ? 0x6E3518 : 0x5A2620;
        int detalle = barril ? 0x3A3F46 : 0x8A3B2E;
        p.elipse(cx, cy, rx * 0.95f, ry * 0.7f, 0, 0x1A1816, 0.35f * a);
        for (int k = 0; k < 5; k++) {
            double ang = k * 1.25 + 0.4;
            float ox = cx + (float) Math.cos(ang) * rx * 0.55f;
            float oy = cy + (float) Math.sin(ang) * ry * 0.4f;
            p.rect(ox - rx * 0.16f, oy - ry * 0.1f, rx * 0.32f, ry * 0.22f, k % 2 == 0 ? base : detalle, 0.85f * a);
        }
    }

    /** Grietas visibles sobre un obstáculo destructible dañado. */
    private static void grietas(Pincel p, float cx, float cy, float radio, double proporcion) {
        int trazos = proporcion < 0.4 ? 3 : 2;
        for (int k = 0; k < trazos; k++) {
            float dx = (k - 1) * radio * 0.45f;
            p.linea(cx + dx, cy - radio * 0.4f, cx + dx * 0.4f, cy + radio * 0.1f, 2f, 0x1A1512, 0.85f);
            p.linea(cx + dx * 0.4f, cy + radio * 0.1f, cx + dx * 0.9f, cy + radio * 0.5f, 1.6f, 0x1A1512, 0.75f);
        }
    }

    /** Un objeto sólido (barril, chimenea, carrito), con los pies en su sitio. Entre los robots, por profundidad. */
    static void solido(Pincel p, VistaDeArena v, Arena.Trampa tr, float t) {
        if (tr.destruida()) return;
        float cx = v.sx(tr.x, tr.y), cy = v.sy(tr.y), rx = (float) tr.radio * v.escala(tr.y), ry = (float) tr.radio * v.escalaY();
        p.elipse(cx, cy + ry * 0.2f, rx * 1.1f, ry * 0.9f, 0, 0x000000, 0.3f);
        switch (tr.tipo) {
            case "barril": {
                float alto = rx * 1.8f;
                p.rect(cx - rx, cy - alto, rx * 2, alto, 0x9A4A22, 1);
                for (float k : new float[]{0.18f, 0.8f}) p.rect(cx - rx, cy - alto * k - 4, rx * 2, 6, 0x3A3F46, 1);
                p.elipse(cx, cy, rx, ry * 0.7f, 0, 0x7A3A1A, 1);
                p.elipse(cx, cy - alto, rx, ry * 0.7f, 0, 0xB85E30, 1);
                p.elipse(cx, cy - alto, rx * 0.25f, ry * 0.18f, 0, 0x3A2A22, 1);
                p.rect(cx - rx * 0.7f, cy - alto * 0.55f, rx * 0.25f, alto * 0.3f, 0xFFC857, 0.9f);
                if (tr.proporcionResistencia() < 0.75) grietas(p, cx, cy - alto * 0.5f, rx * 0.65f, tr.proporcionResistencia());
                break;
            }
            case "chimenea": {
                float alto = rx * 2.6f;
                p.rect(cx - rx, cy - alto, rx * 2, alto, 0x8A3B2E, 1);
                for (int k = 1; k < 7; k++) p.rect(cx - rx, cy - alto * k / 7f, rx * 2, 2, 0x5A2620, 1);
                p.rect(cx - rx * 1.15f, cy - alto - 8, rx * 2.3f, 12, 0x5A2620, 1);
                if (tr.proporcionResistencia() < 0.75) grietas(p, cx, cy - alto * 0.45f, rx * 0.7f, tr.proporcionResistencia());
                for (int k = 0; k < 3; k++) {
                    float f = (t * 0.4f + k / 3f) % 1f;
                    p.circulo(cx + (float) Math.sin(t + k) * 6, cy - alto - 14 - f * 50, 6 + f * 10, 0x9AA3AD, 0.4f * (1 - f));
                }
                break;
            }
            case "carrito": {
                // Un carrito de cocina con una sartén enorme encima; las ruedas giran.
                float alto = rx * 1.1f;
                p.caja(cx - rx * 1.2f, cy - alto, rx * 2.4f, alto * 0.7f, 6, 0xB8C0C8, 1);
                for (int k = -1; k <= 1; k += 2) {
                    float wx = cx + k * rx * 0.8f;
                    p.circulo(wx, cy - 4, rx * 0.28f, 0x2A2E36, 1);
                    float a = (float) (tr.x * 0.1);
                    p.linea(wx, cy - 4, wx + (float) Math.cos(a) * rx * 0.25f, cy - 4 + (float) Math.sin(a) * rx * 0.25f, 2, 0x8A929C, 1);
                }
                p.elipse(cx, cy - alto - 6, rx * 1.1f, ry * 0.5f, 0, 0x2A2E36, 1);
                p.rect(cx + rx * 1.0f, cy - alto - 10, rx * 0.9f, 7, 0x3A2A22, 1);
                break;
            }
            default:
                p.circulo(cx, cy - rx, rx, 0x8A929C, 1);
                break;
        }
    }
}
