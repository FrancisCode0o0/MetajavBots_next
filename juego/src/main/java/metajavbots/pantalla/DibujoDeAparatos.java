package metajavbots.pantalla;

import metajavbots.combate.Aparatos;

/**
 * DIBUJO DE APARATOS — Los cacharros de la arena: flotando en el suelo
 * (con su anillo de luz y su nombre), pasando de la mano a su hueco mientras
 * el robot se los pone, y ya montados: la mochila a chorro en la espalda, el
 * anillo antigravedad bajo los pies, la bobina en la cabeza, el lanzallamas o
 * la pistola de pegamento en el brazo, la lata de aceite a la espalda. Cuando
 * se usan, se nota (llamas, rayos, luz azul).
 */
final class DibujoDeAparatos {

    private DibujoDeAparatos() {
    }

    /** Uno en el suelo, esperando: un anillo de luz, el aparato que sube y baja y su nombre. */
    static void enElSuelo(Pincel p, VistaDeArena v, Aparatos.Uno a, float t, float letra) {
        float cx = v.sx(a.x, a.y), cy = v.sy(a.y), r = 30 * v.escala(a.y);
        int color = color(a.tipo);
        float pulso = 0.5f + 0.5f * (float) Math.sin(t * 4);
        p.elipse(cx, cy, r * (1.2f + pulso * 0.3f), r * 0.5f, 0, color, 0.18f + pulso * 0.15f);
        p.aro(cx, cy, r, 2, color, 0.8f);
        float alto = cy - r * 1.6f - (float) Math.sin(t * 3) * r * 0.25f;
        p.brillo(cx, alto, r * 1.4f, color, 0.35f);
        icono(p, a.tipo, cx, alto, r * 0.9f, t, false);
        float tl = Math.max(14, letra * 0.7f);
        p.textoCentrado(a.tipo.nombre, cx, alto - r * 1.4f - tl, tl, true, color, 1);
    }

    /**
     * Uno montado en el robot (con los pies en (px, py), «alto» de robot y «lado» ±1); mientras se monta, va de la
     * mano a su hueco con chispas.
     */
    static void montado(Pincel p, Aparatos.Uno a, float px, float py, float alto, float lado, float t) {
        float[] hueco = hueco(a.tipo, px, py, alto, lado);
        float s = alto * 0.16f;
        if (!a.conectado && a.montando == 0) {
            // Cogido pero sin conectar: lo lleva en la mano, delante.
            icono(p, a.tipo, px + lado * alto * 0.35f, py - alto * 0.45f, s, t, false);
            return;
        }
        if (a.montando > 0) {
            float f = 1 - a.montando / (float) Aparatos.MONTAR;
            float hx = px + lado * alto * 0.35f, hy = py - alto * 0.45f;
            float x = hx + (hueco[0] - hx) * f, y = hy + (hueco[1] - hy) * f - (float) Math.sin(f * Math.PI) * alto * 0.25f;
            icono(p, a.tipo, x, y, s, t, false);
            for (int k = 0; k < 4; k++) {
                double ang = t * 11 + k * 1.57;
                p.linea(x, y, x + (float) Math.cos(ang) * s * 1.2f, y + (float) Math.sin(ang) * s * 1.2f, 2, 0xFFE38A, 0.8f);
            }
            return;
        }
        if (a.tipo == Aparatos.Tipo.ANTIGRAVEDAD) {
            // El anillo bajo los pies; al usarlo, luz azul hacia abajo.
            p.elipse(px, py - 2, alto * 0.3f, alto * 0.08f, 0, 0x2A3644, 1);
            p.elipse(px, py - 2, alto * 0.24f, alto * 0.05f, 0, color(a.tipo), a.activo > 0 ? 1 : 0.5f);
            if (a.activo > 0) {
                for (int k = 0; k < 5; k++) p.linea(px - alto * 0.2f + k * alto * 0.1f, py, px - alto * 0.2f + k * alto * 0.1f, py + alto * 0.18f, 3,
                        0x7FD8FF, 0.35f);
            }
            return;
        }
        icono(p, a.tipo, hueco[0], hueco[1], s, t, a.activo > 0);
        if (a.tipo == Aparatos.Tipo.CHORRO && a.activo > 0) {
            // Llamas del motor hacia atrás.
            for (int k = 0; k < 6; k++) {
                float l = s * (1.5f + (float) Math.random() * 1.5f);
                p.linea(hueco[0], hueco[1] + s * 0.6f, hueco[0] - lado * l, hueco[1] + s * 0.6f + l * 0.5f, s * 0.35f, k % 2 == 0 ? 0xFF8A1A : 0xFFD24A, 0.8f);
            }
        }
    }

    /** Dónde va cada aparato en el robot. */
    private static float[] hueco(Aparatos.Tipo tipo, float px, float py, float alto, float lado) {
        switch (tipo.hueco) {
            case "espalda": return new float[]{px - lado * alto * 0.28f, py - alto * 0.6f};
            case "cabeza": return new float[]{px, py - alto * 1.08f};
            case "piernas": return new float[]{px, py - 2};
            default: return new float[]{px + lado * alto * 0.38f, py - alto * 0.52f};
        }
    }

    static int color(Aparatos.Tipo tipo) {
        switch (tipo) {
            case CHORRO: return 0xFF8A1A;
            case ANTIGRAVEDAD: return 0x5BC8FF;
            case DESCARGA: return 0xFFE14A;
            case LANZALLAMAS: return 0xFF4A3A;
            case ACEITE: return 0x9B7BFF;
            default: return 0x7BE36B;
        }
    }

    /** El dibujo del aparato, centrado en (cx, cy), de tamaño s. */
    static void icono(Pincel p, Aparatos.Tipo tipo, float cx, float cy, float s, float t, boolean activo) {
        switch (tipo) {
            case CHORRO:
                // Dos bombonas plateadas con sus toberas.
                for (int k = -1; k <= 1; k += 2) {
                    p.caja(cx + k * s * 0.32f - s * 0.25f, cy - s * 0.7f, s * 0.5f, s * 1.3f, s * 0.2f, 0xC7CED6, 1);
                    p.rect(cx + k * s * 0.32f - s * 0.18f, cy + s * 0.6f, s * 0.36f, s * 0.2f, 0x3A3F46, 1);
                }
                p.rect(cx - s * 0.6f, cy - s * 0.2f, s * 1.2f, s * 0.15f, 0xFF8A1A, 1);
                break;
            case ANTIGRAVEDAD:
                p.elipse(cx, cy, s, s * 0.35f, 0, 0x2A3644, 1);
                p.elipse(cx, cy, s * 0.75f, s * 0.22f, 0, 0x5BC8FF, 0.6f + 0.4f * (float) Math.sin(t * 6));
                p.elipse(cx, cy - s * 0.1f, s * 0.3f, s * 0.1f, 0, 0xFFFFFF, 0.7f);
                break;
            case DESCARGA:
                // Una bobina de cobre con su bola; al usarla, rayos.
                for (int k = 0; k < 4; k++) p.elipse(cx, cy + s * 0.5f - k * s * 0.28f, s * 0.42f, s * 0.12f, 0, 0xC87533, 1);
                p.circulo(cx, cy - s * 0.65f, s * 0.3f, 0xD9DEE4, 1);
                if (activo || (t * 3) % 1f < 0.2f) {
                    for (int k = 0; k < 3; k++) {
                        double ang = t * 17 + k * 2.1;
                        p.linea(cx, cy - s * 0.65f, cx + (float) Math.cos(ang) * s * 1.1f, cy - s * 0.65f + (float) Math.sin(ang) * s * 1.1f, 2, 0xFFF4A0,
                                0.9f);
                    }
                }
                break;
            case LANZALLAMAS:
                p.caja(cx - s * 0.5f, cy - s * 0.35f, s * 0.7f, s * 0.7f, s * 0.3f, 0xD9403A, 1);
                p.rect(cx + s * 0.15f, cy - s * 0.1f, s * 0.7f, s * 0.2f, 0x3A3F46, 1);
                p.circulo(cx + s * 0.88f, cy, s * 0.12f, activo ? 0xFFD24A : 0xFF8A1A, 1);
                break;
            case ACEITE:
                // Una lata de aceite con su gota.
                p.caja(cx - s * 0.4f, cy - s * 0.55f, s * 0.8f, s * 1.1f, s * 0.12f, 0x2A2E36, 1);
                p.rect(cx - s * 0.4f, cy - s * 0.15f, s * 0.8f, s * 0.25f, 0x9B7BFF, 1);
                p.circulo(cx + s * 0.55f, cy + s * 0.35f, s * 0.13f, 0x1A1A22, 1);
                break;
            default:
                // Pegamento: un tubo verde con su boquilla y una gota colgando.
                p.caja(cx - s * 0.55f, cy - s * 0.25f, s * 0.9f, s * 0.5f, s * 0.2f, 0x4FB548, 1);
                p.poligono(new float[]{cx + s * 0.35f, cy - s * 0.15f, cx + s * 0.8f, cy, cx + s * 0.35f, cy + s * 0.15f}, 0xE8E8E8, 1);
                p.circulo(cx + s * 0.8f, cy + s * 0.18f, s * 0.1f, 0x7BE36B, 0.9f);
                break;
        }
    }
}
