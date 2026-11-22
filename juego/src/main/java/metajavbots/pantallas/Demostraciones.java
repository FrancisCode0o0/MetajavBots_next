package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.ejercicios.Demostracion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * DEMOSTRACIONES — Lista de demostraciones automatizadas en vivo, accesible
 * desde la parte superior de Tutoriales. Cada tarjeta abre PantallaDemostracion.
 */
public final class Demostraciones implements Pantalla {

    private final Ui.Desplazable despl = new Ui.Desplazable();
    private float t;

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("demo.titulo"), true);
        float x = 24;
        float w = p.ancho - 48;
        float yIntro = arriba + 14;
        float tIntro = Math.min(ui.letra, 24f);
        float hIntro = p.parrafo(T.t("demo.intro"), x, yIntro, w, tIntro, ui.tema.textoSuave, 1);
        float panelY = yIntro + hIntro + 14;
        float panelH = Math.max(180, p.alto - panelY - 20);
        float cy = ui.panel(x, panelY, w, panelH, null);
        float d = ui.desplazable(despl, x + 8, cy, w - 16, panelY + panelH - cy - 10);
        float yy = cy - d + 6;
        List<Demostracion> lista = Demostracion.todas();
        for (Demostracion demo : lista) {
            yy = dibujarTarjeta(j, demo, x + 20, yy, w - 52) + 14;
        }
        ui.finDesplazable(despl, yy + d - cy);
    }

    private float dibujarTarjeta(Juego j, Demostracion demo, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float tNivel = Math.min(ui.letra * 0.78f, 20f);
        float tTit = Math.min(ui.letra * 1.15f, 26f);
        float tRes = Math.min(ui.letra * 0.92f, 22f);
        String btnTxt = "▶ " + T.t("demo.ver");
        float bw = Math.max(210, Math.min(w * 0.32f, p.ancho(btnTxt, Math.min(ui.letra, 24f), true) + 42));
        float textoW = Math.max(220, w - bw - 48);
        String nivelTxt = T.t("demo.nivel." + demo.nivel) + " · " + T.f("demo.pasos", demo.pasos.size());
        String titTxt = demo.titulo(T.idioma);
        String resTxt = demo.resumen(T.idioma);
        float hNivel = p.altoDeLinea(tNivel);
        float hTit = p.altoDeParrafo(titTxt, textoW, tTit, true);
        float hRes = p.altoDeParrafo(resTxt, textoW, tRes);
        float h = Math.max(108, 28 + hNivel + 6 + hTit + 8 + hRes);
        p.caja(x, y, w, h, 12, ui.tema.panelAlto, 1);
        p.bordeRedondeado(x, y, w, h, 12, ui.tema.acento, 0.8f, 1.5f);
        float ty = y + 14;
        p.textoConBordeNegro(nivelTxt, x + 20, ty, tNivel, true, ui.tema.acento, 1);
        ty += hNivel + 6;
        ty += p.parrafo(titTxt, x + 20, ty, textoW, tTit, true, ui.tema.texto, 1) + 8;
        p.parrafo(resTxt, x + 20, ty, textoW, tRes, ui.tema.textoSuave, 1);
        float bh = Math.max(48, Math.min(60, ui.letra * 2.1f));
        if (ui.boton(btnTxt, x + w - bw - 18, y + (h - bh) / 2f, bw, bh, Ui.Estilo.PRINCIPAL)) {
            j.ir(new PantallaDemostracion(demo));
        }
        return y + h;
    }
}
