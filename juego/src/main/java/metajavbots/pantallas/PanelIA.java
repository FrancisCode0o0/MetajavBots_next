package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.dialogos.Dialogos;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Dificultad;
import metajavbots.textos.T;

/**
 * PANEL DE LA IA — Cómo es la computadora: su dificultad (adaptada a la
 * etiqueta, que es lo recomendado, o fija de fácil a extrema), su
 * agresividad (con la alta se ríe del jugador y le suelta pullas de ring),
 * su humor y su cortesía. Un ejemplo de lo que diría, para probar.
 */
public final class PanelIA implements Pantalla {

    private String ejemplo = "";

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.panel_ia"), true);
        float x = 120, y = arriba + 30, w = p.ancho - 240;
        p.texto(T.t("panel.dificultad"), x, y, ui.letra * 1.2f, true, ui.tema.texto, 1);
        y += 44;
        String[] modos = {"ADAPTADA", "FACIL", "NORMAL", "DIFICIL", "MUY_DIFICIL", "EXTREMA"};
        float bw = w / modos.length;
        for (int i = 0; i < modos.length; i++) {
            String nombre = i == 0 ? T.t("panel.adaptada") : Dificultad.valueOf(modos[i]).nombre(T.idioma);
            if (ui.boton(nombre, x + i * bw, y, bw - 10, 56, jug.modoIA.equals(modos[i]) ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) jug.modoIA = modos[i];
        }
        y += 70;
        p.parrafo(jug.modoIA.equals("ADAPTADA") ? T.t("panel.adaptada_ayuda") : T.t("panel.fija_ayuda"), x, y, w, ui.letra * 0.85f, ui.tema.textoSuave, 1);
        y += 80;
        jug.agresividad = ui.deslizador(T.t("panel.agresividad"), (float) jug.agresividad, x, y, w, 50, T.t("panel.agresividad_ayuda"));
        y += 80;
        jug.humor = ui.deslizador(T.t("panel.humor"), (float) jug.humor, x, y, w, 50, T.t("panel.humor_ayuda"));
        y += 80;
        jug.cortesia = ui.deslizador(T.t("panel.cortesia"), (float) jug.cortesia, x, y, w, 50, T.t("panel.cortesia_ayuda"));
        y += 90;
        float pw = Math.min(w * 0.45f, p.ancho(T.t("panel.probar"), ui.letra, true) + 40);
        if (ui.boton(T.t("panel.probar"), x, y, pw, 54, Ui.Estilo.NORMAL)) {
            String evento = jug.agresividad >= 0.6 && Math.random() < jug.agresividad ? "pulla" : jug.cortesia > 0.6 ? "saludo" : "burla";
            String f = Dialogos.de("bravucon").frase(evento, T.idioma, java.util.Map.of("yo", "Mazo", "rival", jug.robot() == null ? "?" : jug.robot().nombre));
            ejemplo = f == null ? "" : "«" + f + "»";
        }
        p.parrafo(ejemplo, x + pw + 20, y + 12, w - pw - 20, ui.letra, ui.tema.texto, 1);
        Comun.pie(j, T.t("panel.pie"));
    }

    @Override
    public void alSalir(Juego j) {
        j.guardar();
    }
}
