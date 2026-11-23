package metajavbots.pantallas;

import java.util.Locale;

import metajavbots.Juego;
import metajavbots.combate.Arena;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.MinijuegoDestruccion;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * DIBUJO MINIJUEGO DESTRUCCIÓN — Panel informativo del menú previo, señales de
 * aterrizaje/aparición en la arena 3×, HUD con cuenta atrás y botín en vivo,
 * minimapa compacto y tarjeta de resultado final con desglose en centésimas.
 */
final class DibujoMinijuegoDestruccion {

    private DibujoMinijuegoDestruccion() {
    }

    /** Dibuja el panel izquierdo del menú previo con reglas, daño ≥ 2,0×, recompensas y esquema 3×. */
    static void panelReglas(Pincel p, Ui ui, DificultadCarrera dif, int subnivel, float x, float y, float w, float h) {
        float cy = ui.panel(x, y, w, h, T.t("destruccion.panel_titulo"));
        float mx = x + 16, mw = w - 32;
        float esquemH = Math.min(h * 0.28f, mw * 0.34f);
        dibujarEsquemaArena(p, ui, mx, cy + 4, mw, esquemH);
        float ly = cy + esquemH + 14;
        float tl = Math.min(ui.letra * 0.72f, 16f);
        double mult = MinijuegoDestruccion.multiplicadorDanoPara(dif, subnivel);
        String[] lineas = {
                T.f("destruccion.regla_arena", (int) MinijuegoDestruccion.ANCHO, (int) MinijuegoDestruccion.ALTO,
                        MinijuegoDestruccion.DURACION_SEGUNDOS),
                T.t("destruccion.regla_apariciones"),
                T.f("destruccion.regla_dano", String.format(Locale.ROOT, "%.2f", mult)),
                T.t("destruccion.regla_recompensas"),
                T.t("destruccion.regla_derrota")
        };
        int[] colores = {0x3FE0FF, 0xFF9F1C, 0xFF6B7A, 0xFFD23F, ui.tema.textoSuave};
        for (int i = 0; i < lineas.length && ly < y + h - 18; i++) {
            ly += p.parrafo(lineas[i], mx, ly, mw, tl, colores[i], 1) + 8;
        }
    }

    private static void dibujarEsquemaArena(Pincel p, Ui ui, float x, float y, float w, float h) {
        p.caja(x, y, w, h, 8, 0x161E2B, 1);
        p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 1, 1.4f);
        float tercio = w / 3f;
        p.linea(x + tercio, y + 6, x + tercio, y + h - 6, 1.2f, 0x3FE0FF, 0.35f);
        p.linea(x + tercio * 2, y + 6, x + tercio * 2, y + h - 6, 1.2f, 0x3FE0FF, 0.35f);
        p.circulo(x + w * 0.5f, y + h * 0.5f, 6.5f, VistaDeArena.COLORES[0], 1);
        p.circulo(x + w * 0.22f, y + h * 0.32f, 5f, 0xFF7A45, 0.95f);
        p.circulo(x + w * 0.78f, y + h * 0.66f, 5f, 0xFF7A45, 0.95f);
        p.rect(x + w * 0.36f, y + h * 0.62f, 9, 9, 0xD97236, 0.95f);
        p.rect(x + w * 0.64f, y + h * 0.26f, 9, 9, 0xE05A4F, 0.95f);
        p.textoCentrado("3× ARENA (3000 × 700)", x + w / 2f, y + h - 22, Math.min(ui.letra * 0.64f, 14f), true, 0xFFC857, 0.95f);
    }

    /** Dibuja en la arena las sombras de caída desde el cielo y señales de aparición desde el suelo. */
    static void avisosEnArena(Pincel p, VistaDeArena vista, MinijuegoDestruccion juego) {
        for (MinijuegoDestruccion.AvisoAparicion a : juego.avisosActivos()) {
            float cx = vista.sx(a.x, a.y);
            float cy = vista.sy(a.y);
            float rx = (float) a.radio * vista.escala(a.y);
            float ry = (float) a.radio * vista.escalaY();
            float prog = a.progreso();
            if (a.desdeCielo) {
                p.elipse(cx, cy, rx * (0.45f + 0.75f * prog), ry * (0.45f + 0.75f * prog), 0, 0x05070A, 0.32f + 0.48f * prog);
                p.aro(cx, cy, rx * (1.55f - 0.55f * prog), 3.0f, 0xFF4536, 0.50f + 0.50f * prog);
                float cruz = rx * 0.45f;
                p.linea(cx - cruz, cy, cx + cruz, cy, 2.2f, 0xFF4536, 0.45f + 0.50f * prog);
                p.linea(cx, cy - cruz * 0.65f, cx, cy + cruz * 0.65f, 2.2f, 0xFF4536, 0.45f + 0.50f * prog);
                float altura = (1f - prog) * rx * 5.2f;
                p.linea(cx, cy - altura - 28, cx, cy - altura, 3f, 0xFFC857, 0.80f);
            } else {
                p.elipse(cx, cy, rx * (0.6f + 0.4f * prog), ry * (0.6f + 0.4f * prog), 0, 0xD97236, 0.28f + 0.35f * prog);
                p.aro(cx, cy, rx * (0.5f + 0.65f * prog), 2.4f, 0xFFC857, 0.5f + 0.45f * prog);
            }
        }
    }

    /** Dibuja el minimapa compacto de la arena 3× con cámara, jugador, objetos, robots y avisos. */
    static void minimapa(Pincel p, Ui ui, MinijuegoDestruccion juego, VistaDeArena vista, float x, float y, float w, float h) {
        p.caja(x, y, w, h, 8, 0x0E131D, 0.88f);
        p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 0.85f, 1.5f);
        if (vista.anchoVentana > 0) {
            float vx = (float) (x + ((vista.camaraX - vista.anchoVentana / 2) / MinijuegoDestruccion.ANCHO) * w);
            float vw = (float) ((vista.anchoVentana / MinijuegoDestruccion.ANCHO) * w);
            p.rect(Math.max(x + 2, vx), y + 2, Math.min(vw, w - 4), h - 4, 0x3FE0FF, 0.14f);
            p.bordeRedondeado(Math.max(x + 2, vx), y + 2, Math.min(vw, w - 4), h - 4, 4, 0x3FE0FF, 0.7f, 1.2f);
        }
        for (MinijuegoDestruccion.AvisoAparicion a : juego.avisosActivos()) {
            float ax = (float) (x + (a.x / MinijuegoDestruccion.ANCHO) * w);
            float ay = (float) (y + (1.0 - a.y / MinijuegoDestruccion.ALTO) * h);
            p.circulo(ax, ay, 3.2f, a.desdeCielo ? 0xFF5A36 : 0xFFC857, 0.75f);
        }
        for (Arena.Trampa t : juego.pelea.arena.trampas()) {
            if (t.destruida()) continue;
            float ox = (float) (x + (t.x / MinijuegoDestruccion.ANCHO) * w);
            float oy = (float) (y + (1.0 - t.y / MinijuegoDestruccion.ALTO) * h);
            if (t.destructible) p.circulo(ox, oy, 3.0f, 0xD97236, 0.92f);
            else p.rect(ox - 2.4f, oy - 2.4f, 4.8f, 4.8f, 0xE05A4F, 0.92f);
        }
        for (Cuerpo r : juego.robotsActivos()) {
            float rx = (float) (x + (r.x / MinijuegoDestruccion.ANCHO) * w);
            float ry = (float) (y + (1.0 - r.y / MinijuegoDestruccion.ALTO) * h);
            p.circulo(rx, ry, 3.8f, 0xFF7A45, 0.95f);
        }
        Cuerpo c0 = juego.pelea.cuerpo(0);
        if (c0 != null && !c0.fuera) {
            float px = (float) (x + (c0.x / MinijuegoDestruccion.ANCHO) * w);
            float py = (float) (y + (1.0 - c0.y / MinijuegoDestruccion.ALTO) * h);
            p.circulo(px, py, 5.2f, 0xFFFFFF, 0.95f);
            p.circulo(px, py, 3.9f, VistaDeArena.COLORES[0], 1);
        }
    }

    /**
     * Dibuja el HUD en vivo con cuenta atrás, blindaje del jugador, botín acumulado y botones.
     * Devuelve 1 pausa, 2 velocidad, 3 código en ejecución, 4 nueva arena, 5 rendirse o 0 nada.
     */
    static int hud(Juego j, MinijuegoDestruccion juego, boolean pausado, int velocidad, boolean viendoCodigo,
            float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 10, ui.tema.panel, 0.96f);
        float bh = 42f, sep = 8f, tBtn = Math.min(ui.letra, bh * 0.5f);
        String[] textos = {
                pausado ? T.t("carrera_obs.reanudar") : T.t("carrera_obs.pausar"),
                velocidad + "x",
                T.t("carrera_obs.codigo_vivo"),
                T.t("destruccion.nueva_arena"),
                T.t("carrera_obs.rendirse")
        };
        Ui.Estilo[] estilos = {
                Ui.Estilo.NORMAL, Ui.Estilo.PLANO, viendoCodigo ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, Ui.Estilo.PLANO, Ui.Estilo.PELIGRO
        };
        boolean[] activos = {true, true, true, true, !juego.terminada()};
        float[] anchos = new float[textos.length];
        float totalBtns = sep * (textos.length - 1);
        for (int i = 0; i < textos.length; i++) {
            anchos[i] = Math.max(i == 1 ? 56f : 92f, p.ancho(textos[i], tBtn, true) + 26f);
            totalBtns += anchos[i];
        }
        float maxBtns = Math.max(260f, w - 340f);
        if (totalBtns > maxBtns) {
            float esc = (maxBtns - sep * (textos.length - 1)) / Math.max(1f, totalBtns - sep * (textos.length - 1));
            totalBtns = sep * (textos.length - 1);
            for (int i = 0; i < anchos.length; i++) {
                anchos[i] = Math.max(46f, anchos[i] * esc);
                totalBtns += anchos[i];
            }
        }
        dibujarIndicadoresHud(p, ui, juego, x + 14f, y, Math.max(260f, w - totalBtns - 36f));
        float bx = x + w - 14f - totalBtns;
        int accion = 0;
        for (int i = 0; i < textos.length; i++) {
            if (ui.boton(textos[i], bx, y + 12f, anchos[i], bh, estilos[i], activos[i], null)) accion = i + 1;
            bx += anchos[i] + sep;
        }
        return accion;
    }

    private static void dibujarIndicadoresHud(Pincel p, Ui ui, MinijuegoDestruccion juego, float x, float y, float ancho) {
        float tHud = Math.min(ui.letra * 0.74f, 16.5f);
        float wTiempo = Math.min(185f, ancho * 0.34f);
        float wBlind = Math.min(185f, ancho * 0.34f);
        int seg = (int) Math.ceil(juego.segundosRestantes());
        String txtTiempo = T.f("destruccion.hud_tiempo", seg);
        int colTiempo = seg <= 10 ? 0xFF6B6B : 0xFFC857;
        p.textoConBordeNegro(ui.recortarTexto(txtTiempo, wTiempo, tHud, true), x, y + 8f, tHud, true, colTiempo, 1);
        ui.barra(x, y + 34f, wTiempo, 18f, juego.proporcionTiempoRestante(), colTiempo, null);

        float xBlind = x + wTiempo + 14f;
        Cuerpo c0 = juego.pelea.cuerpo(0);
        float blind = c0 == null ? 0f : (float) (c0.blindaje() / 100.0);
        String txtBlind = T.f("destruccion.hud_blindaje", Math.round(blind * 100));
        int colBlind = blind > 0.5f ? 0x5BD685 : blind > 0.25f ? 0xFFC857 : 0xFF6B6B;
        p.textoConBordeNegro(ui.recortarTexto(txtBlind, wBlind, tHud, true), xBlind, y + 8f, tHud, true, colBlind, 1);
        ui.barra(xBlind, y + 34f, wBlind, 18f, blind, colBlind, null);

        float xBotin = xBlind + wBlind + 14f;
        float wBotin = Math.max(80f, x + ancho - xBotin);
        String txtBotin = T.f("destruccion.hud_botin", Jugador.formatearMonedas(juego.botinAcumuladoCentimos() / 100.0));
        String txtBajas = T.f("destruccion.hud_bajas", juego.objetosDestruidos(), juego.robotsDestruidos());
        p.textoConBordeNegro(ui.recortarTexto(txtBotin, wBotin, tHud, true), xBotin, y + 8f, tHud, true, 0xFFD23F, 1);
        p.textoConBordeNegro(ui.recortarTexto(txtBajas, wBotin, tHud * 0.9f, false), xBotin, y + 34f, tHud * 0.9f, false, ui.tema.textoSuave, 1);
    }

    /** Dibuja el panel de resultado final con desglose completo; devuelve 1 repetir, 2 editar, 3 nueva arena o 0 nada. */
    static int resultado(Juego j, MinijuegoDestruccion juego, boolean cobroMonedas, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cw = Math.min(680, w - 40), ch = 368;
        float cx = x + (w - cw) / 2, cy = y + (h - ch) / 2;
        p.caja(cx, cy, cw, ch, 14, ui.tema.panel, 0.96f);
        int colorBorde = juego.rendido() ? ui.tema.mal : juego.sobrevivio() ? ui.tema.bien : ui.tema.aviso;
        p.bordeRedondeado(cx, cy, cw, ch, 14, colorBorde, 1, 2.5f);
        String titulo = juego.rendido() ? T.t("destruccion.rendido_titulo")
                : juego.sobrevivio() ? T.t("destruccion.victoria_titulo") : T.t("destruccion.derrota_titulo");
        int colorTit = juego.rendido() ? ui.tema.mal : juego.sobrevivio() ? ui.tema.bien : ui.tema.aviso;
        p.textoCentradoConBordeNegro(titulo, cx + cw / 2, cy + 18, ui.letra * 1.22f, true, colorTit, 1);
        String nivel = juego.dificultad.nombre(T.idioma) + " " + DificultadCarrera.etiquetaSubnivel(juego.subnivel)
                + " · " + T.f("destruccion.regla_dano_corto",
                String.format(Locale.ROOT, "%.2f", MinijuegoDestruccion.multiplicadorDanoPara(juego.dificultad, juego.subnivel)));
        p.textoCentradoConBordeNegro(nivel, cx + cw / 2, cy + 54, ui.letra * 0.76f, true, ui.tema.textoSuave, 1);
        String bajas = T.f("destruccion.desglose_bajas", juego.objetosDestruidos(), juego.robotsDestruidos(),
                Jugador.formatearMonedas(juego.botinAcumuladoCentimos() / 100.0));
        p.textoCentrado(bajas, cx + cw / 2, cy + 92, ui.letra * 0.80f, false, ui.tema.texto, 1);
        String detalle;
        int colDetalle;
        if (juego.rendido()) {
            detalle = T.t("carrera_obs.rendido_sin_premio");
            colDetalle = ui.tema.mal;
        } else if (juego.sobrevivio()) {
            detalle = T.f("destruccion.bono_supervivencia",
                    Jugador.formatearMonedas(juego.botinAcumuladoCentimos() / 100.0),
                    Jugador.formatearMonedas(juego.bonoSupervivenciaCentimos() / 100.0),
                    Jugador.formatearMonedas(juego.centimosGanadosTotal() / 100.0));
            colDetalle = 0x5BD685;
        } else if (juego.bonosReducidosPorDerrota()) {
            detalle = T.f("destruccion.derrota_cuarto",
                    Jugador.formatearMonedas(juego.botinAcumuladoCentimos() / 100.0),
                    Jugador.formatearMonedas(juego.centimosGanadosTotal() / 100.0));
            colDetalle = ui.tema.aviso;
        } else {
            detalle = T.t("destruccion.derrota_cero");
            colDetalle = ui.tema.textoSuave;
        }
        p.textoCentradoConBordeNegro(detalle, cx + cw / 2, cy + 132, ui.letra * 0.82f, true, colDetalle, 1);
        String saldo = T.f("destruccion.saldo_final",
                Jugador.formatearMonedas(juego.centimosGanadosTotal() / 100.0),
                Jugador.formatearMonedas(j.jugador.monedas), j.jugador.centimosDeMoneda);
        p.textoCentradoConBordeNegro(saldo, cx + cw / 2, cy + 174, ui.letra * 0.84f, true,
                cobroMonedas ? 0xFFD23F : ui.tema.textoSuave, 1);
        float bw = (cw - 60) / 3;
        if (ui.boton(T.t("destruccion.repetir"), cx + 20, cy + ch - 64, bw, 46, Ui.Estilo.PRINCIPAL)) return 1;
        if (ui.boton(T.t("carrera_obs.editar"), cx + 30 + bw, cy + ch - 64, bw, 46, Ui.Estilo.NORMAL)) return 2;
        if (ui.boton(T.t("destruccion.nueva_arena"), cx + 40 + bw * 2, cy + ch - 64, bw, 46, Ui.Estilo.PLANO)) return 3;
        return 0;
    }
}
