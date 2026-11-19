package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * MINIJUEGOS — Cinco retos cortos contra el muñeco de prácticas para
 * aprender lo básico del robot, uno por cosa: moverse (recorrer la arena),
 * girar el radar (tener al muñeco a la vista), apuntar (darle tres veces),
 * disparar con cabeza (buena puntería, no a lo loco) y esquivar obstáculos
 * (sobrevivir en una arena con trampas activas usando obstaculos()). Cada uno
 * con su pista en pseudocódigo, una ronda y su objetivo (pantallas/Objetivo);
 * el que se cumple queda marcado (Jugador.retosHechos, «mini:…»).
 */
public final class MiniJuegos implements Pantalla {

    /** id, objetivo. TODO-FRAN: los números (recorrido, segundos a la vista, puntería, esquiva). */
    static final String[][] RETOS = {
        {"moverse", "recorre>=2500"},
        {"girar", "vistos>=300"},
        {"apuntar", "aciertos>=3"},
        {"disparar", "punteria>=60"},
        {"esquivar", "sinDanoTrampa>=600"},
    };

    private float t;

    /** Copia de los retos configurados ({id, objetivo}). */
    public static String[][] retos() {
        String[][] copia = new String[RETOS.length][];
        for (int i = 0; i < RETOS.length; i++) copia[i] = RETOS[i].clone();
        return copia;
    }

    /** Escenario con trampas y obstáculos activos usado en el minijuego de esquivar. */
    public static VistaDeArena.Escenario escenarioDeEsquiva() {
        return VistaDeArena.Escenario.COCINA;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("mini.titulo"), true);
        float x0 = 40, y0 = arriba + 20, gap = 20;
        int cols = 3;
        int filas = (RETOS.length + cols - 1) / cols;
        float w = (p.ancho - 2 * x0 - (cols - 1) * gap) / cols;
        float h = (p.alto - y0 - 30 - (filas - 1) * gap) / filas;
        for (int i = 0; i < RETOS.length; i++) {
            String id = RETOS[i][0], objetivo = RETOS[i][1];
            int fila = i / cols;
            int col = i % cols;
            int enFila = Math.min(cols, RETOS.length - fila * cols);
            float inicioX = (p.ancho - (enFila * w + (enFila - 1) * gap)) / 2;
            float x = inicioX + col * (w + gap), y = y0 + fila * (h + gap);
            boolean hecho = j.jugador.retosHechos.contains("mini:" + id);
            p.caja(x, y, w, h, 14, ui.tema.panel, 1);
            p.bordeRedondeado(x, y, w, h, 14, hecho ? ui.tema.bien : ui.tema.borde, 1, hecho ? 2.5f : 1);
            String titulo = (hecho ? "✔ " : "") + T.t("mini." + id);
            float tTit = ui.cabe(titulo, w - 40, ui.letra * 1.15f, true);
            p.texto(titulo, x + 20, y + 14, tTit, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
            float yy = y + 14 + p.altoDeLinea(tTit) + 4;
            yy += p.parrafo(T.t("mini." + id + ".texto"), x + 20, yy, w - 40, ui.letra * 0.82f, ui.tema.textoSuave, 1) + 6;
            String obj = Objetivo.texto(objetivo);
            float tObj = ui.cabe(obj, w - 40, ui.letra * 0.82f, true);
            p.texto(obj, x + 20, yy, tObj, true, ui.tema.aviso, 1);
            yy += p.altoDeLinea(tObj) + 6;
            // La pista, en pseudocódigo (si con letra grande no caben todas las líneas, se ajusta el tamaño).
            String[] lineas = T.t("mini." + id + ".pseudo").split("\n");
            float disponible = y + h - 68 - yy;
            float tPseudo = ui.letra * 0.78f;
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

    /** Prepara el encuentro de un minijuego contra el muñeco con ese objetivo. */
    public static Encuentro prepararReto(String id, String objetivo) {
        Encuentro en = new Encuentro();
        en.rival = Rivales.muneco();
        en.modo = "TUTORIAL";
        en.cuenta = false;
        en.rondas = 1;
        en.objetivo = objetivo;
        en.escenario = "esquivar".equals(id) ? escenarioDeEsquiva() : PrepararPelea.escenarioAlAzar();
        return en;
    }

    /** Una ronda contra el muñeco con ese objetivo; si lo cumple, queda hecho. */
    static Encuentro reto(Juego j, String id, String objetivo) {
        Encuentro en = prepararReto(id, objetivo);
        en.otraVez = () -> reto(j, id, objetivo);
        en.alAcabar = bien -> {
            if (bien) j.jugador.retosHechos.add("mini:" + id);
        };
        return en;
    }
}
