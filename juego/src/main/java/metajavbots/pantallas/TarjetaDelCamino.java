package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * TARJETA DEL CAMINO — Arriba del menú, para quien empieza: el siguiente paso
 * del camino «Empezar» (pantallas/Camino) con una sola frase de qué se hace, un
 * botón grande para ir y otro para ver ya todo el menú. Debajo, solo las
 * casillas que el camino ya ha abierto.
 */
final class TarjetaDelCamino {

    private TarjetaDelCamino() {
    }

    /** Dibuja la tarjeta en lo alto de la zona derecha del menú; devuelve su alto. */
    static float dibujar(Juego j, float x, float y, float w, float t, boolean compacta) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        int sig = Camino.siguiente(jug), total = Camino.PASOS.size();
        Camino.Paso paso = Camino.PASOS.get(Math.min(sig, total - 1));
        float bw = Math.min(340, Math.max(220, w * 0.3f));
        float tw = w - bw - 64;
        String cab = T.t("camino.titulo").toUpperCase(java.util.Locale.ROOT) + "  ·  " + T.f("camino.paso", sig + 1, total);
        String titulo = Camino.titulo(paso), que = Camino.que(paso);
        float tCab = Math.min(18, ui.letra * 0.8f), tTit = Math.min(30, ui.letra * 1.3f), tQue = Math.min(21, ui.letra * 0.92f);
        float hTexto = p.altoDeLinea(tCab) + 8 + p.altoDeParrafo(titulo, tw, tTit, true) + (compacta ? 0 : 6 + p.altoDeParrafo(que, tw, tQue));
        float h = Math.max(compacta ? 108 : 136, hTexto + 26);
        int color = ui.tema.acento;
        p.caja(x + 2, y + 3, w, h, 14, 0x000000, 0.22f);
        p.caja(x, y, w, h, 14, Pincel.mezcla(ui.tema.panel, color, 0.12f), 1);
        float brillo = 0.55f + 0.45f * (float) Math.abs(Math.sin(t * 1.6));
        p.bordeRedondeado(x, y, w, h, 14, color, brillo, 2.4f);
        float cy = y + 16, tx = x + 24;
        p.texto(cab, tx, cy, tCab, true, color, 1);
        float puntosX = tx + p.ancho(cab, tCab, true) + 24, puntosY = cy + p.altoDeLinea(tCab) / 2f;
        cy += p.altoDeLinea(tCab) + 8;
        cy += p.parrafo(titulo, tx, cy, tw, tTit, true, ui.tema.texto, 1) + 6;
        if (!compacta) cy += p.parrafo(que, tx, cy, tw, tQue, ui.tema.textoSuave, 1) + 10;
        // Un punto por paso, en la línea de arriba: hechos llenos, el siguiente con aro.
        float r = 5, paso2 = Math.min(18, Math.max(8, (tx + tw - puntosX) / Math.max(1, total)));
        for (int i = 0; i < total; i++) {
            float px = puntosX + r + i * paso2, py = puntosY;
            boolean hecho = Camino.hecho(jug, Camino.PASOS.get(i));
            if (hecho) p.circulo(px, py, r, color, 1);
            else if (i == sig) p.aro(px, py, r, 2f, color, 1);
            else p.circulo(px, py, r * 0.6f, ui.tema.borde, 1);
        }
        float bx = x + w - bw - 24, bh = 46;
        String ir = "▶ " + T.t(sig == 0 ? "camino.empezar" : "camino.seguir");
        if (ui.boton(ir, bx, y + 14, bw, bh, Ui.Estilo.PRINCIPAL, true, que)) {
            j.sonidos.tocar("clic", 0.5f);
            j.ir(Camino.destino(j, paso));
        }
        if (ui.boton(T.t("camino.todo"), bx, y + 14 + bh + 6, bw, 38, Ui.Estilo.PLANO, true, T.t("camino.todo_ayuda"))) {
            jug.menuCompleto = true;
            j.guardar();
        }
        return h;
    }
}
