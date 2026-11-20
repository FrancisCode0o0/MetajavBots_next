package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * MINIJUEGOS — Cinco retos cortos contra el muñeco de prácticas ordenados de
 * menor a mayor dificultad: 1. moverse (recorrer la arena), 2. girar el radar
 * (tener al muñeco a la vista), 3. apuntar (darle tres veces), 4. disparar con
 * cabeza (buena puntería, no a lo loco) y 5. esquivar obstáculos (sobrevivir
 * en una arena con trampas activas usando obstaculos()). Cada uno muestra su
 * pista en pseudocódigo, su objetivo y si ya está completado.
 */
public final class MiniJuegos implements Pantalla {

    /** id, objetivo. Ordenados de menor a mayor dificultad. */
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
        float x0 = 40, gap = 18;
        int hechos = contarHechos(j);
        float y0 = dibujarBarraProgreso(j, x0, arriba + 12, p.ancho - 2 * x0, hechos) + 14;
        int cols = 3;
        int filas = (RETOS.length + cols - 1) / cols;
        float w = (p.ancho - 2 * x0 - (cols - 1) * gap) / cols;
        float h = (p.alto - y0 - 24 - (filas - 1) * gap) / filas;
        for (int i = 0; i < RETOS.length; i++) {
            int fila = i / cols;
            int col = i % cols;
            int enFila = Math.min(cols, RETOS.length - fila * cols);
            float inicioX = (p.ancho - (enFila * w + (enFila - 1) * gap)) / 2;
            float x = inicioX + col * (w + gap), y = y0 + fila * (h + gap);
            dibujarReto(j, i, x, y, w, h);
        }
    }

    private static int contarHechos(Juego j) {
        int n = 0;
        for (String[] reto : RETOS) {
            if (j.jugador.retosHechos.contains("mini:" + reto[0])) n++;
        }
        return n;
    }

    private float dibujarBarraProgreso(Juego j, float x, float y, float w, int hechos) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = 42;
        p.caja(x, y, w, h, 10, ui.tema.panel, 0.95f);
        p.bordeRedondeado(x, y, w, h, 10, Pincel.mezcla(ui.tema.borde, ui.tema.acento, 0.4f), 1, 1.2f);
        float bw = 220;
        String resumen = T.f("mini.orden", hechos, RETOS.length);
        float tRes = ajustarAncho(p, resumen, w - bw - 48, Math.min(ui.letra * 0.9f, 20f), 12f, true);
        p.texto(resumen, x + 16, y + (h - p.altoDeLinea(tRes)) / 2f, tRes, true, ui.tema.texto, 1);
        int colorBarra = hechos == RETOS.length ? ui.tema.bien : ui.tema.acento;
        ui.barra(x + w - bw - 16, y + (h - 20) / 2f, bw, 20, hechos / (float) RETOS.length, colorBarra, hechos + " / " + RETOS.length);
        return y + h;
    }

    private void dibujarReto(Juego j, int indice, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String id = RETOS[indice][0], objetivo = RETOS[indice][1];
        boolean hecho = j.jugador.retosHechos.contains("mini:" + id);
        int acento = hecho ? ui.tema.bien : ui.tema.acento;
        boolean claro = ui.tema == Tema.CLARO;
        p.caja(x, y, w, h, 14, Pincel.mezcla(ui.tema.panel, acento, hecho ? (claro ? 0.06f : 0.09f) : 0.03f), 1);
        p.bordeRedondeado(x, y, w, h, 14, hecho ? ui.tema.bien : Pincel.mezcla(ui.tema.borde, acento, 0.35f), 1, hecho ? 2.4f : 1.2f);
        p.caja(x + 4, y + 10, 4, h - 20, 2, acento, 0.9f);
        // Insignia de estado (completado / pendiente) arriba a la derecha.
        String estado = (hecho ? "✔ " : "○ ") + T.t(hecho ? "mini.hecho" : "mini.pendiente");
        float tEst = ajustarAncho(p, estado, w * 0.36f, Math.min(ui.letra * 0.68f, 15f), 10.5f, true);
        float pillW = p.ancho(estado, tEst, true) + 16;
        float pillH = p.altoDeLinea(tEst) + 8;
        float px = x + w - 16 - pillW, py = y + 12;
        p.caja(px, py, pillW, pillH, pillH / 2f, Pincel.mezcla(ui.tema.panelAlto, acento, hecho ? 0.30f : 0.10f), 1);
        p.bordeRedondeado(px, py, pillW, pillH, pillH / 2f, hecho ? ui.tema.bien : ui.tema.borde, 1, 1.1f);
        p.textoCentrado(estado, px + pillW / 2f, py + 4, tEst, true, hecho ? ui.tema.bien : ui.tema.textoSuave, 1);
        // Título numerado por dificultad.
        String titulo = (indice + 1) + ". " + T.t("mini." + id);
        float maxTitW = Math.max(80, px - (x + 18) - 10);
        float tTit = ajustarAncho(p, titulo, maxTitW, Math.min(ui.letra * 1.08f, 23f), 13f, true);
        p.texto(titulo, x + 18, y + 12, tTit, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
        float yy = Math.max(y + 12 + p.altoDeLinea(tTit), py + pillH) + 3;
        // Indicador visual de nivel de dificultad (1 a 5 puntos).
        String nivel = "●".repeat(indice + 1) + "○".repeat(RETOS.length - 1 - indice);
        float tNiv = Math.min(ui.letra * 0.72f, 15f);
        p.texto(nivel, x + 18, yy, tNiv, false, acento, 1);
        yy += p.altoDeLinea(tNiv) + 4;
        // Descripción y objetivo ajustados para que siempre quede sitio al pseudocódigo.
        String desc = T.t("mini." + id + ".texto");
        float maxDescH = Math.max(44, h * 0.25f);
        float tDesc = Math.min(ui.letra * 0.80f, 18f);
        while (tDesc > 11f && p.altoDeParrafo(desc, w - 36, tDesc) > maxDescH) tDesc -= 0.5f;
        yy += p.parrafo(desc, x + 18, yy, w - 36, tDesc, ui.tema.textoSuave, 1) + 5;
        String obj = Objetivo.texto(objetivo);
        float tObj = ajustarAncho(p, obj, w - 36, Math.min(ui.letra * 0.80f, 18f), 11f, true);
        p.texto(obj, x + 18, yy, tObj, true, ui.tema.aviso, 1);
        yy += p.altoDeLinea(tObj) + 6;
        dibujarPseudocodigo(p, ui, T.t("mini." + id + ".pseudo"), x + 18, yy, w - 36, y + h - 62 - yy);
        float bw = (w - 48) / 2;
        if (ui.boton(T.t("mini.jugar"), x + 18, y + h - 54, bw, 42, hecho ? Ui.Estilo.NORMAL : Ui.Estilo.PRINCIPAL)) {
            j.ir(new PantallaPelea(reto(j, id, objetivo)));
        }
        if (ui.boton(T.t("menu.taller"), x + 30 + bw, y + h - 54, bw, 42, Ui.Estilo.PLANO)) j.ir(new Taller());
    }

    private static void dibujarPseudocodigo(Pincel p, Ui ui, String texto, float x, float y, float w, float disponible) {
        if (disponible <= 12) return;
        String[] lineas = texto.split("\n");
        float tPseudo = Math.min(ui.letra * 0.76f, 17f);
        if (lineas.length * p.altoDeLinea(tPseudo) + 12 > disponible && disponible > 24) {
            tPseudo = Math.max(Pincel.LETRA_MINIMA, Math.min(tPseudo, (disponible - 12) / (lineas.length * 1.32f)));
        }
        for (String l : lineas) tPseudo = Math.min(tPseudo, ajustarAncho(p, l, w - 20, tPseudo, Pincel.LETRA_MINIMA, false));
        float alto = p.altoDeLinea(tPseudo);
        float cajaAlta = Math.min(lineas.length * alto + 12, disponible);
        p.caja(x, y, w, cajaAlta, 8, 0x1B2430, 1);
        p.recortar(x, y, w, cajaAlta);
        for (int k = 0; k < lineas.length; k++) p.texto(lineas[k], x + 10, y + 6 + k * alto, tPseudo, false, 0xE8E2C8, 1);
        p.sinRecorte();
    }

    private static float ajustarAncho(Pincel p, String texto, float anchoMax, float tamMax, float tamMin, boolean negrita) {
        if (texto == null || texto.isEmpty()) return tamMax;
        float ancho = p.ancho(texto, tamMax, negrita);
        if (ancho <= anchoMax || ancho <= 0) return tamMax;
        return Math.max(tamMin, tamMax * anchoMax / ancho);
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
