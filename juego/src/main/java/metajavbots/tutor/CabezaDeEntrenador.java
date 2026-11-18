package metajavbots.tutor;

import metajavbots.pantalla.Pincel;

/**
 * CABEZA DE ENTRENADOR — La cabecita flotante de un entrenador del modo
 * tutor: una cabeza de robot que sube y baja, parpadea y mueve la boca
 * mientras habla, con lo suyo dibujado encima (gorra, cinta, barba de cables,
 * cejas, gafas, bigote, capucha, estrellas, antena rota). Cada uno con su
 * dibujo y sus colores (Entrenador).
 */
public final class CabezaDeEntrenador {

    private CabezaDeEntrenador() {
    }

    /** La dibuja con el centro en (cx, cy) y ese radio; «habla» mueve la boca; t, el tiempo (para flotar y parpadear). */
    public static void dibujar(Pincel p, Entrenador e, float cx, float cy, float r, boolean habla, float t) {
        float y = cy + (float) Math.sin(t * 2) * r * 0.08f;
        p.elipse(cx, cy + r * 1.35f, r * 0.7f, r * 0.14f, 0, 0x000000, 0.25f);
        if (e.tiene("capucha")) p.circulo(cx, y + r * 0.1f, r * 1.12f, 0x1A1D24, 1);
        // La cabeza: una caja redondeada, con las orejas-tornillo.
        p.circulo(cx - r * 0.98f, y, r * 0.18f, Pincel.oscurecer(e.cara, 0.3f), 1);
        p.circulo(cx + r * 0.98f, y, r * 0.18f, Pincel.oscurecer(e.cara, 0.3f), 1);
        p.caja(cx - r * 0.9f, y - r * 0.85f, r * 1.8f, r * 1.7f, r * 0.45f, e.cara, 1);
        p.caja(cx - r * 0.75f, y - r * 0.55f, r * 1.5f, r * 0.55f, r * 0.2f, 0x14171C, 1);
        boolean parpadea = (t % 3.7f) < 0.13f;
        for (int lado = -1; lado <= 1; lado += 2) {
            float ox = cx + lado * r * 0.36f, oy = y - r * 0.28f;
            if (parpadea) p.rect(ox - r * 0.14f, oy, r * 0.28f, r * 0.05f, e.ojos, 1);
            else p.circulo(ox, oy, r * 0.13f, e.ojos, 1);
        }
        // La boca: una rejilla que se abre al hablar.
        float abre = habla ? (0.08f + 0.12f * Math.abs((float) Math.sin(t * 18))) * r : 0.05f * r;
        p.caja(cx - r * 0.4f, y + r * 0.28f - abre / 2, r * 0.8f, abre + r * 0.06f, r * 0.05f, 0x14171C, 1);
        for (int i = 1; i < 4; i++) p.rect(cx - r * 0.4f + i * r * 0.2f, y + r * 0.28f - abre / 2, r * 0.03f, abre + r * 0.06f, e.cara, 0.6f);
        rasgos(p, e, cx, y, r, t);
    }

    private static void rasgos(Pincel p, Entrenador e, float cx, float y, float r, float t) {
        if (e.tiene("cejas")) {
            p.linea(cx - r * 0.55f, y - r * 0.6f, cx - r * 0.15f, y - r * 0.5f, r * 0.12f, 0xF2F2F2, 1);
            p.linea(cx + r * 0.55f, y - r * 0.6f, cx + r * 0.15f, y - r * 0.5f, r * 0.12f, 0xF2F2F2, 1);
        }
        if (e.tiene("gafas")) {
            p.aro(cx - r * 0.36f, y - r * 0.28f, r * 0.24f, r * 0.06f, e.detalle, 1);
            p.aro(cx + r * 0.36f, y - r * 0.28f, r * 0.24f, r * 0.06f, e.detalle, 1);
            p.linea(cx - r * 0.12f, y - r * 0.28f, cx + r * 0.12f, y - r * 0.28f, r * 0.05f, e.detalle, 1);
        }
        if (e.tiene("bigote")) {
            p.elipse(cx - r * 0.22f, y + r * 0.14f, r * 0.26f, r * 0.08f, 0, 0x5A5F66, 1);
            p.elipse(cx + r * 0.22f, y + r * 0.14f, r * 0.26f, r * 0.08f, 0, 0x5A5F66, 1);
        }
        if (e.tiene("barba")) {
            // Barba de cables, que se mece.
            for (int i = -3; i <= 3; i++) {
                float largo = r * (0.9f - Math.abs(i) * 0.12f);
                float mece = (float) Math.sin(t * 2 + i) * r * 0.06f;
                p.linea(cx + i * r * 0.12f, y + r * 0.62f, cx + i * r * 0.14f + mece, y + r * 0.62f + largo, r * 0.07f, 0xF2F2F2, 1);
            }
        }
        if (e.tiene("cinta")) {
            p.rect(cx - r * 0.92f, y - r * 0.82f, r * 1.84f, r * 0.2f, e.detalle, 1);
            p.circulo(cx, y - r * 0.72f, r * 0.1f, 0xFFFFFF, 1);
            p.linea(cx + r * 0.9f, y - r * 0.74f, cx + r * 1.2f, y - r * 0.5f + (float) Math.sin(t * 4) * r * 0.08f, r * 0.08f, e.detalle, 1);
        }
        if (e.tiene("gorra")) {
            p.caja(cx - r * 0.85f, y - r * 1.2f, r * 1.7f, r * 0.5f, r * 0.25f, e.detalle, 1);
            p.rect(cx - r * 0.2f, y - r * 0.78f, r * 1.25f, r * 0.12f, Pincel.oscurecer(e.detalle, 0.2f), 1);
        }
        if (e.tiene("estrellas")) {
            for (int i = 0; i < 3; i++) p.texto("★", cx - r * 0.6f + i * r * 0.4f, y - r * 1.25f, Math.max(14, r * 0.3f), true, e.detalle, 1);
        }
        if (e.tiene("antena_rota")) {
            p.linea(cx + r * 0.3f, y - r * 0.85f, cx + r * 0.45f, y - r * 1.25f, r * 0.06f, 0xAAB0BA, 1);
            p.linea(cx + r * 0.45f, y - r * 1.25f, cx + r * 0.7f, y - r * 1.2f, r * 0.06f, 0xAAB0BA, 1);
        }
        if (e.tiene("capucha")) p.texto("~", cx - r * 0.12f, y - r * 1.05f, Math.max(14, r * 0.4f), true, e.ojos, 1);
    }
}
