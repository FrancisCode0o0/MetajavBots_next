package metajavbots.pantallas;

import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * DIBUJO DEL TURNO — Un turno del robot, paso a paso y en bucle, para el
 * tutorial 1 («dibujo: turno» en res/tutoriales/) y el concepto «Así obedece tu
 * robot» del entrenador. A la izquierda, la arena vista desde arriba: el robot,
 * su cabeza (la mira) y el enemigo. A la derecha, el paso en palabras y los tres
 * mandos (ruedas, cuerpo y cabeza) con la orden que recuerda cada uno.
 * 1) Avisos: alVerEnemigo pide «mira al enemigo» y dispara: el disparo sale ya,
 * hacia donde mira la cabeza. 2) cadaTurno pide «gira 12°»: la orden de antes se
 * borra. 3) El robot obedece: la cabeza gira despacio hacia la última orden.
 */
final class DibujoDelTurno {

    /** Lo que dura cada paso (s). */
    static final float PASO = 3.4f;
    private static final float MIRA0 = -30, ENEMIGO = 40, GIRO = 12;
    private static final String[] PASOS = {"dibujo.turno.paso1", "dibujo.turno.paso2", "dibujo.turno.paso3"};
    private static final int AZUL = 0x2F80ED, ROJO = 0xE5484D, CIAN = 0x3FE0FF, NARANJA = 0xFF7A3D, AMARILLO = 0xFFD23F;

    private DibujoDelTurno() {
    }

    /** El paso (0, 1 o 2) que toca en el instante t. */
    static int paso(float t) {
        return (int) (Math.max(0, t) / PASO) % 3;
    }

    /** Dibuja el turno en ese recuadro; devuelve el alto usado. */
    static float dibujar(Ui ui, float x, float y, float w, float h, float t) {
        Pincel p = ui.p;
        int paso = paso(t);
        float f = (Math.max(0, t) % PASO) / PASO;
        p.caja(x, y, w, h, 12, ui.tema.panel, 1);
        float titulo = Math.min(ui.letra * 0.9f, ui.cabe(T.t("dibujo.turno.titulo"), w - 32, ui.letra * 0.9f, true));
        p.texto(T.t("dibujo.turno.titulo"), x + 16, y + 10, titulo, true, ui.tema.acento, 1);
        float arriba = y + 16 + titulo * 1.4f, alto = h - (arriba - y) - 12;
        float lado = Math.min(alto, w * 0.36f);
        arena(p, ui, x + 12, arriba, lado, paso, f);
        float dx = x + lado + 28, dw = w - lado - 44;
        float yy = arriba;
        // Los tres pasos, como puntos: el de ahora, encendido.
        for (int i = 0; i < 3; i++) {
            float cx = dx + 14 + i * 36;
            float tn = Math.min(20, ui.letra * 0.8f);
            p.circulo(cx, yy + 12, 13, i == paso ? ui.tema.acento : ui.tema.panelAlto, 1);
            p.textoCentrado(Integer.toString(i + 1), cx, yy + 12 - tn * 0.62f, tn, true, i == paso ? ui.tema.acentoTexto : ui.tema.textoSuave, 1);
        }
        yy += 34;
        float mandosAlto = Math.min(alto * 0.45f, ui.letra * 4.4f);
        String texto = T.t(PASOS[paso]);
        float tam = ui.letra * 0.95f, sitio = alto - 34 - mandosAlto - 10;
        while (tam > 11 && p.altoDeParrafo(texto, dw, tam) > sitio) tam -= 0.5f;
        p.parrafo(texto, dx, yy, dw, tam, ui.tema.texto, 1);
        mandos(p, ui, dx, arriba + alto - mandosAlto, dw, mandosAlto, paso, f);
        return h;
    }

    /** La arena desde arriba: el robot con su cabeza, el enemigo, lo que quiere la cabeza (discontinuo) y el disparo. */
    private static void arena(Pincel p, Ui ui, float x, float y, float lado, int paso, float f) {
        p.caja(x, y, lado, lado, 10, 0x2A3038, 1);
        float cx = x + lado * 0.32f, cy = y + lado * 0.62f, r = lado * 0.08f, largo = lado * 0.6f;
        float ex = cx + (float) Math.cos(Math.toRadians(ENEMIGO)) * largo, ey = cy - (float) Math.sin(Math.toRadians(ENEMIGO)) * largo;
        p.circulo(ex, ey, r * 0.9f, ROJO, 1);
        p.circulo(ex, ey, r * 0.4f, 0xFFFFFF, 1);
        // Adónde mira la cabeza ahora: quieta en los pasos 1 y 2; en el 3 gira despacio (como mucho 14° por turno).
        float mira = paso < 2 ? MIRA0 : MIRA0 + GIRO * suave(f / 0.8f);
        float quiere = paso == 0 ? ENEMIGO : MIRA0 + GIRO;
        if (paso == 1 && f < 0.45f) discontinua(p, cx, cy, ENEMIGO, largo, CIAN, 1 - f / 0.45f);
        discontinua(p, cx, cy, quiere, largo, CIAN, paso == 1 ? Math.min(1, f / 0.45f) : 1);
        if (paso == 1 && f < 0.6f) {
            // La orden «mira al enemigo», tachada: el mando de la cabeza ya no la recuerda.
            float mx = (cx + ex) / 2, my = (cy + ey) / 2;
            p.linea(mx - 12, my - 12, mx + 12, my + 12, 4, ROJO, 1);
            p.linea(mx - 12, my + 12, mx + 12, my - 12, 4, ROJO, 1);
        }
        leyenda(p, ui, x, y, lado);
        p.circulo(cx, cy, r, AZUL, 1);
        double am = Math.toRadians(mira);
        p.linea(cx, cy, cx + (float) Math.cos(am) * r * 2.2f, cy - (float) Math.sin(am) * r * 2.2f, 5, NARANJA, 1);
        if (paso == 0 && f > 0.35f) {
            // El disparo sale hacia donde mira la cabeza AHORA (y no da al enemigo).
            float d = r * 2.4f + (f - 0.35f) / 0.65f * largo * 1.1f;
            p.circulo(cx + (float) Math.cos(am) * d, cy - (float) Math.sin(am) * d, r * 0.35f, AMARILLO, 1);
        }
    }

    /** Abajo, qué es cada línea: la discontinua, adónde quiere ir la cabeza; la continua, adónde mira ahora. */
    private static void leyenda(Pincel p, Ui ui, float x, float y, float lado) {
        float t = Math.min(20, ui.letra * 0.75f), ly = y + lado - t * 2.9f;
        String[] textos = {T.t("pelea.mira_quiere"), T.t("pelea.mira_ahora")};
        for (int i = 0; i < 2; i++) {
            float fy = ly + i * t * 1.4f + t * 0.6f;
            if (i == 0) for (int k = 0; k < 3; k++) p.linea(x + 10 + k * 9, fy, x + 15 + k * 9, fy, 2.5f, CIAN, 1);
            else p.linea(x + 10, fy, x + 33, fy, 4, NARANJA, 1);
            float tt = Math.min(t, ui.cabe(textos[i], lado - 52, t, false));
            p.texto(textos[i], x + 40, ly + i * t * 1.4f, tt, false, 0xC8CDD4, 1);
        }
    }

    private static void discontinua(Pincel p, float cx, float cy, float grados, float largo, int color, float alfa) {
        double a = Math.toRadians(grados);
        float ux = (float) Math.cos(a), uy = (float) -Math.sin(a);
        for (int i = 0; i < 9; i++) {
            float d0 = largo * i / 9f, d1 = largo * (i + 0.55f) / 9f;
            p.linea(cx + ux * d0, cy + uy * d0, cx + ux * d1, cy + uy * d1, 2.5f, color, alfa);
        }
    }

    /** Los tres mandos y la orden que recuerda cada uno en este paso. */
    private static void mandos(Pincel p, Ui ui, float x, float y, float w, float h, int paso, float f) {
        String[] nombres = {T.t("dibujo.mando.ruedas"), T.t("dibujo.mando.cuerpo"), T.t("dibujo.mando.cabeza")};
        String cabeza = paso == 0 || (paso == 1 && f < 0.45f) ? T.t("dibujo.orden.mirar_enemigo") : T.t("dibujo.orden.girar12");
        String[] ordenes = {T.t("dibujo.orden.avanzar"), T.t("dibujo.orden.ninguna"), cabeza};
        float bw = (w - 16) / 3;
        for (int i = 0; i < 3; i++) {
            float bx = x + i * (bw + 8);
            boolean destaca = i == 2;
            p.caja(bx, y, bw, h, 8, ui.tema.panelAlto, 1);
            p.bordeRedondeado(bx, y, bw, h, 8, destaca ? CIAN : ui.tema.borde, 1, destaca ? 2.5f : 1.2f);
            float tn = ui.cabe(nombres[i], bw - 16, ui.letra * 0.8f, true);
            p.texto(nombres[i], bx + 8, y + 6, tn, true, ui.tema.textoSuave, 1);
            float to = ui.cabe(ordenes[i], bw - 16, ui.letra * 0.78f, false);
            float oy = y + 10 + tn * 1.35f;
            boolean borrando = destaca && paso == 1 && f < 0.45f;
            p.texto(ordenes[i], bx + 8, oy, to, false, borrando ? ROJO : destaca ? CIAN : ui.tema.texto, 1);
            if (borrando) {
                // La orden vieja, tachada, y debajo la nueva: el mando solo se queda con la última.
                float ancho = p.ancho(ordenes[i], to, false);
                p.linea(bx + 8, oy + to * 0.62f, bx + 8 + ancho, oy + to * 0.62f, 2.5f, ROJO, 1);
                String nueva = "→ " + T.t("dibujo.orden.girar12");
                p.texto(nueva, bx + 8, oy + to * 1.3f, ui.cabe(nueva, bw - 16, to, false), false, CIAN, 1);
            }
        }
    }

    private static float suave(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
