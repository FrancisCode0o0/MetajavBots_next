package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.combate.Aparatos;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.T;

/**
 * PANEL DEL APARATO — En la pelea, el botón del aparato de la ronda y, al
 * pulsarlo, su código (de solo lectura) en grande: a qué hueco se conecta y
 * cómo se usa.
 */
public final class PanelDelAparato {

    private boolean verAparato;
    private String codigoDelAparato = "";
    private final VentanaDeCodigo ventanaDelAparato = new VentanaDeCodigo();

    /** Uno con el código ya abierto (para las capturas). */
    public static PanelDelAparato abierto() {
        PanelDelAparato p = new PanelDelAparato();
        p.verAparato = true;
        return p;
    }

    /**
     * El aparato de esta ronda (o de la siguiente): un botón con su nombre que abre su código, para leer cómo se
     * conecta. Devuelve si hay que poner la pelea en pausa (al abrirlo).
     */
    public boolean dibujar(Juego j, Aparatos.Tipo tipo, float derecha, float y) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (tipo == null) return false;
        boolean pausar = false;
        String texto = "▣ " + T.f("aparato.boton", tipo.nombre);
        float bw = Math.min(360, p.ancho(texto, ui.letra, true) + 36);
        if (ui.boton(ui.recortarTexto(texto, bw - 24, ui.letra, true), derecha - bw, y, bw, 46, verAparato ? Ui.Estilo.ELEGIDO : Ui.Estilo.NORMAL, true,
                T.t("aparato.boton_ayuda"))) {
            verAparato = !verAparato;
            pausar = verAparato;
        }
        if (!verAparato) return pausar;
        if (!codigoDelAparato.equals(tipo.id())) {
            codigoDelAparato = tipo.id();
            ventanaDelAparato.ponerTexto(CodigoDeAparato.texto(tipo.id(), j.jugador.idioma));
            ventanaDelAparato.soloLectura = true;
        }
        float w = Math.min(1100, p.ancho - 120), h = p.alto - 250, x = (p.ancho - w) / 2, py = 110;
        float cy = ui.panel(x, py, w, h, T.f("aparato.titulo", tipo.nombre));
        ventanaDelAparato.tamano = j.jugador.tamanoDeLetra;
        ventanaDelAparato.dibujar(ui, x + 12, cy, w - 24, py + h - cy - 70);
        if (ui.boton(T.t("aparato.cerrar"), x + w - 220, py + h - 60, 200, 48, Ui.Estilo.PRINCIPAL) || ui.atras()) verAparato = false;
        return pausar;
    }
}
