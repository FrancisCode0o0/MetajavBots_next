package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.T;

/** MARCADORES — El marcador de arriba de la pelea cuando hay más de dos robots (battle royale, por equipos): uno por robot. */
final class Marcadores {

    private Marcadores() {
    }

    static void deVarios(Juego j, Pelea pelea) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 0, p.ancho, 88, ui.tema.panel, 0.97f);
        p.rect(0, 87, p.ancho, 1, ui.tema.borde, 1);
        int n = pelea.combatientes.length;
        float w = Math.min(260, (p.ancho - 260) / n);
        for (int i = 0; i < n; i++) {
            Cuerpo c = pelea.cuerpo(i);
            Combatiente co = pelea.combatientes[i];
            float x = 16 + i * w;
            int color = VistaDeArena.COLORES[pelea.equipo[i] % VistaDeArena.COLORES.length];
            p.texto(ui.recortarTexto(co.nombre, w - 20, ui.letra * 0.8f, true), x, 8, ui.letra * 0.8f, true, color, 1);
            if (c != null) {
                float parte = (float) (c.blindaje() / 100);
                ui.barra(x, 34, w - 20, 14, parte, parte > 0.5f ? ui.tema.bien : parte > 0.2f ? ui.tema.aviso : ui.tema.mal, null);
            }
            p.texto(T.f("pelea.dano", Math.round(co.danoTotalRecibido)), x, 54, ui.letra * 0.65f, false, ui.tema.textoSuave, 1);
            if (co.montaje != null && co.montaje.modelo != null) {
                String modelo = T.f("pelea.modelo", co.montaje.modelo.nombre(T.idioma));
                p.texto(ui.recortarTexto(modelo, w - 20, ui.letra * 0.6f, false), x, 70, ui.letra * 0.6f, false, ui.tema.textoSuave, 1);
            }
        }
        float cx = p.ancho - 130;
        p.textoCentrado(T.f("pelea.ronda", pelea.ronda(), pelea.rondas), cx, 8, ui.letra * 0.9f, true, ui.tema.texto, 1);
        int segundos = (int) Math.ceil((pelea.turnosPorRonda - pelea.turno()) / (double) Pelea.TURNOS_POR_SEGUNDO);
        p.textoCentrado(String.format("%d:%02d", Math.max(0, segundos) / 60, Math.max(0, segundos) % 60), cx, 36, ui.letra * 1.5f, true,
                segundos <= 10 ? ui.tema.mal : ui.tema.texto, 1);
    }
}
