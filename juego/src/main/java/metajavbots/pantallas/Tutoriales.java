package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.ejercicios.Leccion;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * TUTORIALES — Las lecciones por temas, de cero absoluto a avanzado: cada
 * una mejora el robot y enseña un poco de Java. Se marca la siguiente que
 * toca según la etiqueta del jugador.
 */
public final class Tutoriales implements Pantalla {

    private final Ui.Desplazable despl = new Ui.Desplazable();
    private float t;

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("menu.tutoriales"), true);
        float x = 20, y = arriba + 16, w = p.ancho - 40, h = p.alto - y - 20;
        float cy = ui.panel(x, y, w, h, null);
        float d = ui.desplazable(despl, x + 8, cy, w - 16, y + h - cy - 10);
        float yy = cy - d;
        boolean marcada = false;
        for (Tema tm : Tema.todos()) {
            List<Leccion> ls = Leccion.deTema(tm.id);
            if (ls.isEmpty()) continue;
            p.texto(tm.id + " · " + tm.nombre(T.idioma), x + 24, yy + 8, ui.letra * 1.05f, true, ui.tema.texto, 1);
            p.textoDerecha(Tema.bloque(tm.bloque, T.idioma), x + w - 40, yy + 10, ui.letra * 0.75f, false, ui.tema.textoApagado, 1);
            yy += ui.letra * 2;
            float cw = (w - 60) / 4;
            int i = 0;
            for (Leccion l : ls) {
                float cx = x + 24 + (i % 4) * cw;
                if (i > 0 && i % 4 == 0) yy += 76;
                boolean hecha = jug.tutorialesHechos.contains(l.id);
                boolean siguiente = !hecha && !marcada;
                if (siguiente) marcada = true;
                Ui.Estilo e = hecha ? Ui.Estilo.PLANO : siguiente ? Ui.Estilo.PRINCIPAL : Ui.Estilo.NORMAL;
                if (ui.boton((hecha ? "✔ " : "") + l.titulo(T.idioma), cx, yy, cw - 12, 64, e)) j.ir(new PantallaLeccion(l));
                i++;
            }
            yy += 84;
        }
        ui.finDesplazable(despl, yy + d - cy);
    }
}
