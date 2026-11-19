package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * MINIJUEGOS — Cuatro retos cortos contra el muñeco de prácticas para
 * aprender lo básico del robot, uno por cosa: moverse (recorrer la arena),
 * girar el radar (tener al muñeco a la vista), apuntar (darle tres veces) y
 * disparar con cabeza (buena puntería, no a lo loco). Cada uno con su pista
 * en pseudocódigo, una ronda y su objetivo (pantallas/Objetivo); el que se
 * cumple queda marcado (Jugador.retosHechos, «mini:…»).
 */
public final class MiniJuegos implements Pantalla {

    /** id, objetivo. TODO-FRAN: los números (recorrido, segundos a la vista, puntería). */
    static final String[][] RETOS = {
        {"moverse", "recorre>=2500"},
        {"girar", "vistos>=300"},
        {"apuntar", "aciertos>=3"},
        {"disparar", "punteria>=60"},
    };

    private float t;

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("mini.titulo"), true);
        float x0 = 40, y0 = arriba + 20, gap = 20;
        float w = (p.ancho - 2 * x0 - gap) / 2, h = (p.alto - y0 - 30 - gap) / 2;
        for (int i = 0; i < RETOS.length; i++) {
            String id = RETOS[i][0], objetivo = RETOS[i][1];
            float x = x0 + (i % 2) * (w + gap), y = y0 + (i / 2) * (h + gap);
            boolean hecho = j.jugador.retosHechos.contains("mini:" + id);
            p.caja(x, y, w, h, 14, ui.tema.panel, 1);
            p.bordeRedondeado(x, y, w, h, 14, hecho ? ui.tema.bien : ui.tema.borde, 1, hecho ? 2.5f : 1);
            p.texto((hecho ? "✔ " : "") + T.t("mini." + id), x + 20, y + 14, ui.letra * 1.2f, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
            float yy = y + 14 + p.altoDeLinea(ui.letra * 1.2f) + 4;
            yy += p.parrafo(T.t("mini." + id + ".texto"), x + 20, yy, w - 40, ui.letra * 0.85f, ui.tema.textoSuave, 1) + 6;
            String obj = Objetivo.texto(objetivo);
            float tObj = ui.cabe(obj, w - 40, ui.letra * 0.85f, true);
            p.texto(obj, x + 20, yy, tObj, true, ui.tema.aviso, 1);
            yy += p.altoDeLinea(tObj) + 6;
            // La pista, en pseudocódigo (si con letra grande no caben todas las líneas, se ajusta el tamaño).
            String[] lineas = T.t("mini." + id + ".pseudo").split("\n");
            float disponible = y + h - 68 - yy;
            float tPseudo = ui.letra * 0.8f;
            if (lineas.length * p.altoDeLinea(tPseudo) + 12 > disponible && disponible > 24) {
                tPseudo = Math.max(Pincel.LETRA_MINIMA, Math.min(tPseudo, (disponible - 12) / (lineas.length * 1.35f)));
            }
            for (String l : lineas) tPseudo = Math.min(tPseudo, ui.cabe(l, w - 60, tPseudo, false));
            float alto = p.altoDeLinea(tPseudo);
            float cajaAlta = Math.min(lineas.length * alto + 12, disponible);
            if (cajaAlta > 8) {
                p.caja(x + 20, yy, w - 40, cajaAlta, 8, 0x1B2430, 1);
                p.recortar(x + 20, yy, w - 40, cajaAlta);
                for (int k = 0; k < lineas.length; k++) p.texto(lineas[k], x + 30, yy + 6 + k * alto, tPseudo, false, 0xE8E2C8, 1);
                p.sinRecorte();
            }
            float bw = (w - 52) / 2;
            if (ui.boton(T.t("mini.jugar"), x + 20, y + h - 60, bw, 46, hecho ? Ui.Estilo.NORMAL : Ui.Estilo.PRINCIPAL)) {
                j.ir(new PantallaPelea(reto(j, id, objetivo)));
            }
            if (ui.boton(T.t("menu.taller"), x + 32 + bw, y + h - 60, bw, 46, Ui.Estilo.PLANO)) j.ir(new Taller());
        }
    }

    /** Una ronda contra el muñeco con ese objetivo; si lo cumple, queda hecho. */
    static Encuentro reto(Juego j, String id, String objetivo) {
        Encuentro en = new Encuentro();
        en.rival = Rivales.muneco();
        en.modo = "TUTORIAL";
        en.cuenta = false;
        en.rondas = 1;
        en.objetivo = objetivo;
        en.escenario = PrepararPelea.escenarioAlAzar();
        en.alAcabar = bien -> {
            if (bien) j.jugador.retosHechos.add("mini:" + id);
        };
        return en;
    }
}
