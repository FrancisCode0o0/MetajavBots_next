package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/** PREMIO DEL DESAFÍO — Tras ganar a una IA unos escalones por encima: subir de etiqueta o llevarse una pieza legendaria. */
public final class PremioDelDesafio implements Pantalla {

    private boolean elegido;
    /** Lo que pasa al elegir (la pelea quita su botón del premio). */
    private final Runnable alElegir;

    public PremioDelDesafio(Runnable alElegir) {
        this.alElegir = alElegir;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, 0);
        // «Volver» lleva al final de la pelea, donde el botón del premio sigue esperando.
        Comun.cabecera(j, T.t("premio.titulo"), true);
        dibujar(j, p, ui);
    }

    private void dibujar(Juego j, Pincel p, Ui ui) {
        if (elegido) {
            p.textoCentrado(T.t("premio.hecho"), p.ancho / 2, 300, ui.letra * 1.5f, true, ui.tema.bien, 1);
            if (ui.boton(T.t("final.menu"), p.ancho / 2 - 120, 400, 240, 56, Ui.Estilo.PRINCIPAL)) j.irSolo(new Menu());
            return;
        }
        p.textoCentrado(T.t("premio.elige"), p.ancho / 2, 140, ui.letra * 1.4f, true, ui.tema.texto, 1);
        boolean puedeSubir = j.jugador.etiqueta != Etiqueta.SENIOR_4 && j.jugador.etiqueta != Etiqueta.MASTER;
        if (ui.boton(T.f("premio.subir", j.jugador.etiqueta.siguiente().nombre), p.ancho / 2 - 300, 210, 600, 70, Ui.Estilo.PRINCIPAL, puedeSubir, null)) {
            Progreso.premioDelDesafio(j.jugador, true, null);
            j.sonidos.tocar("subida", 1);
            j.sacudir(8, 0.8f);
            elegido = true;
            alElegir.run();
            j.guardar();
        }
        List<Pieza> leg = Progreso.legendariasPorGanar(j.jugador);
        float y = 320;
        for (Pieza pz : leg) {
            if (ui.boton(pz.nombre(T.idioma) + "  ·  " + Taller.cifras(pz), 160, y, p.ancho - 320, 58, Ui.Estilo.NORMAL)) {
                Progreso.premioDelDesafio(j.jugador, false, pz);
                j.sonidos.tocar("reparar", 1);
                elegido = true;
                alElegir.run();
                j.guardar();
            }
            y += 68;
        }
        if (leg.isEmpty() && !puedeSubir) elegido = true;
    }
}
