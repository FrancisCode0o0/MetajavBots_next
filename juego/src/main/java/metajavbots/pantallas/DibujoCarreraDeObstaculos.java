package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.combate.Arena;
import metajavbots.combate.CarreraDeObstaculos;
import metajavbots.combate.Cuerpo;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * DIBUJO CARRERA DE OBSTÁCULOS — Mapa general previo con visibilidad parcial,
 * minimapa panorámico en directo (solo elementos ya visibles), líneas de salida
 * y meta en pista 6× y tarjeta de resultado.
 */
final class DibujoCarreraDeObstaculos {

    private DibujoCarreraDeObstaculos() {
    }

    /** Dibuja el mapa general previo con visibilidad parcial (sin ocultar ni revelar repentinos). */
    static void mapaGeneral(Pincel p, Ui ui, Arena vistaPrevia, double anchoPista, double metaX,
            float x, float y, float w, float h) {
        p.caja(x, y, w, h, 8, 0x1A202C, 1);
        p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 1, 1.5f);
        float sxSalida = (float) (x + (CarreraDeObstaculos.SALIDA_X / anchoPista) * w);
        float sxMeta = (float) (x + (metaX / anchoPista) * w);
        p.rect(sxSalida, y + 6, 3, h - 12, 0x5BD685, 0.85f);
        dibujarMetaCuadros(p, sxMeta, y + 6, h - 12, 7);
        p.rect(x + w * 0.14f, y + 8, w * 0.78f, h - 16, 0x3FE0FF, 0.05f);
        p.textoCentrado("? RADAR ?", x + w * 0.53f, y + 8, ui.letra * 0.62f, true, 0x3FE0FF, 0.45f);
        p.circulo(sxSalida - 5, y + h * 0.66f, 5, VistaDeArena.COLORES[0], 1);
        p.circulo(sxSalida - 5, y + h * 0.34f, 5, VistaDeArena.COLORES[1], 1);
        for (Arena.Trampa t : vistaPrevia.trampas()) {
            if (t.ocultoEnMapa || t.repentino) continue;
            float ox = (float) (x + (t.x / anchoPista) * w);
            float oy = (float) (y + (1.0 - t.y / CarreraDeObstaculos.ALTO) * h);
            dibujarMarcaElemento(p, t, ox, oy, 4.5f);
        }
        p.texto(T.t("carrera_obs.salida"), x + 6, y + h - 20, ui.letra * 0.58f, true, 0x5BD685, 1);
        p.texto(T.t("carrera_obs.meta"), sxMeta - 44, y + h - 20, ui.letra * 0.58f, true, 0xFFC857, 1);
    }

    /** Dibuja el minimapa panorámico compacto con salida, meta, competidores y solo elementos ya visibles. */
    static void minimapa(Pincel p, Ui ui, CarreraDeObstaculos carrera, VistaDeArena vista, float x, float y, float w, float h) {
        double anchoPista = carrera.anchoPista();
        p.caja(x, y, w, h, 8, 0x0E131D, 0.88f);
        p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 0.85f, 1.5f);
        float sxSalida = (float) (x + (CarreraDeObstaculos.SALIDA_X / anchoPista) * w);
        float sxMeta = (float) (x + (carrera.metaX() / anchoPista) * w);
        p.rect(sxSalida, y + 4, 2.5f, h - 8, 0x5BD685, 0.9f);
        dibujarMetaCuadros(p, sxMeta, y + 4, h - 8, 6);
        if (vista.anchoVentana > 0) {
            float vx = (float) (x + ((vista.camaraX - vista.anchoVentana / 2) / anchoPista) * w);
            float vw = (float) ((vista.anchoVentana / anchoPista) * w);
            p.rect(Math.max(x + 2, vx), y + 2, Math.min(vw, w - 4), h - 4, 0x3FE0FF, 0.12f);
            p.bordeRedondeado(Math.max(x + 2, vx), y + 2, Math.min(vw, w - 4), h - 4, 4, 0x3FE0FF, 0.65f, 1.2f);
        }
        for (Arena.Trampa t : carrera.pelea.arena.trampas()) {
            if (t.destruida() || (t.ocultoEnMapa && !t.revelado())) continue;
            float ox = (float) (x + (t.x / anchoPista) * w);
            float oy = (float) (y + (1.0 - t.y / CarreraDeObstaculos.ALTO) * h);
            dibujarMarcaElemento(p, t, ox, oy, 3.2f);
        }
        for (Cuerpo d : carrera.robotsDefectuosos()) {
            if (d.fuera) continue;
            float dx = (float) (x + (d.x / anchoPista) * w);
            float dy = (float) (y + (1.0 - d.y / CarreraDeObstaculos.ALTO) * h);
            p.circulo(dx, dy, 3.6f, 0xFF7A45, 0.9f);
        }
        dibujarCorredorEnMinimapa(p, carrera.pelea.cuerpo(1), anchoPista, x, y, w, h, VistaDeArena.COLORES[1]);
        dibujarCorredorEnMinimapa(p, carrera.pelea.cuerpo(0), anchoPista, x, y, w, h, VistaDeArena.COLORES[0]);
    }

    private static void dibujarCorredorEnMinimapa(Pincel p, Cuerpo c, double anchoPista, float x, float y, float w, float h, int color) {
        if (c == null || c.fuera) return;
        float cx = (float) (x + (c.x / anchoPista) * w);
        float cy = (float) (y + (1.0 - c.y / CarreraDeObstaculos.ALTO) * h);
        p.circulo(cx, cy, 5.2f, 0xFFFFFF, 0.95f);
        p.circulo(cx, cy, 4.0f, color, 1);
    }

    private static void dibujarMarcaElemento(Pincel p, Arena.Trampa t, float ox, float oy, float r) {
        if (t.tipo.equals("impulso")) {
            p.circulo(ox, oy, r * 0.95f, 0x3FE0FF, 0.95f);
            p.circulo(ox, oy, r * 0.45f, 0x9CFF57, 1);
        } else if (t.tipo.equals(Arena.TIPO_REPARACION)) {
            p.circulo(ox, oy, r * 0.95f, 0x2EE58D, 0.95f);
            p.rect(ox - r * 0.22f, oy - r * 0.65f, r * 0.44f, r * 1.3f, 0xFFFFFF, 1);
            p.rect(ox - r * 0.65f, oy - r * 0.22f, r * 1.3f, r * 0.44f, 0xFFFFFF, 1);
        } else if (t.tipo.equals(Arena.TIPO_MONEDA_EXTRA)) {
            p.circulo(ox, oy, r * 1.05f, 0xFFD23F, 1);
            p.circulo(ox, oy, r * 0.5f, 0xFFF7C2, 1);
        } else if (t.tipo.equals(Arena.TIPO_RAMPA)) {
            p.triangulo(ox - r, oy + r * 0.7f, ox + r, oy + r * 0.7f, ox + r, oy - r * 0.7f, 0xFF9F1C, 0.95f);
        } else if (t.tipo.equals(Arena.TIPO_AGUA)) {
            p.circulo(ox, oy, r * 1.05f, 0x33A8E8, 0.9f);
        } else if (t.tipo.equals(Arena.TIPO_TRAMPA_MORTAL)) {
            p.circulo(ox, oy, r * 1.15f, 0xFFC857, 0.98f);
            p.circulo(ox, oy, r * 0.85f, 0xFF1E42, 1);
        } else if (t.destructible) {
            p.circulo(ox, oy, r, 0xD97236, 0.92f);
        } else {
            p.rect(ox - r * 0.8f, oy - r * 0.8f, r * 1.6f, r * 1.6f, 0xE05A4F, 0.9f);
        }
    }

    /** Dibuja las líneas de salida y meta sobre el suelo 3D de la pista escalada. */
    static void lineasPista(Pincel p, Ui ui, VistaDeArena vista, double metaX) {
        float yArriba = vista.sy(CarreraDeObstaculos.ALTO);
        float yAbajo = vista.sy(0);
        float xSalidaArriba = vista.sx(CarreraDeObstaculos.SALIDA_X, CarreraDeObstaculos.ALTO);
        float xSalidaAbajo = vista.sx(CarreraDeObstaculos.SALIDA_X, 0);
        p.linea(xSalidaArriba, yArriba, xSalidaAbajo, yAbajo, 3f, 0x5BD685, 0.78f);
        int cuadros = 16;
        for (int i = 0; i < cuadros; i++) {
            double y0 = CarreraDeObstaculos.ALTO * i / cuadros;
            double y1 = CarreraDeObstaculos.ALTO * (i + 1) / cuadros;
            float sx0 = vista.sx(metaX, y0);
            float sx1 = vista.sx(metaX, y1);
            float sy0 = vista.sy(y0);
            float sy1 = vista.sy(y1);
            int c1 = i % 2 == 0 ? 0xFFFFFF : 0x111318;
            int c2 = i % 2 == 0 ? 0x111318 : 0xFFFFFF;
            p.linea(sx0, sy0, sx1, sy1, 8f, c1, 0.92f);
            p.linea(sx0 + 8, sy0, sx1 + 8, sy1, 8f, c2, 0.92f);
        }
        p.textoCentrado(T.t("carrera_obs.meta"), vista.sx(metaX, CarreraDeObstaculos.ALTO),
                yArriba - 24, ui.letra * 0.85f, true, 0xFFC857, 1);
    }

    /** Dibuja el panel de resultado final con desglose completo de botín; devuelve 1 repetir, 2 editar, 3 nueva pista o 0 nada. */
    static int resultado(Juego j, CarreraDeObstaculos carrera, boolean monedaGanada, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        CarreraDeObstaculos.Medalla medalla = carrera.medallaObtenida();
        float cw = Math.min(640, w - 40), ch = 372;
        float cx = x + (w - cw) / 2, cy = y + (h - ch) / 2;
        p.caja(cx, cy, cw, ch, 14, ui.tema.panel, 0.96f);
        p.bordeRedondeado(cx, cy, cw, ch, 14, carrera.ganador() == 0 ? ui.tema.bien : ui.tema.aviso, 1, 2.5f);
        String titulo = carrera.ganador() == 0 ? T.t("carrera_obs.ganas")
                : carrera.ganador() == 1 ? T.t("carrera_obs.pierdes") : T.t("carrera_obs.empate");
        p.textoCentrado(titulo, cx + cw / 2, cy + 16, ui.letra * 1.28f, true,
                carrera.ganador() == 0 ? ui.tema.bien : ui.tema.texto, 1);
        double totalCarrera = carrera.centimosGanadosTotal() / 100.0;
        String premio = monedaGanada
                ? T.f("carrera_obs.botin_total", Jugador.formatearMonedas(totalCarrera),
                        Jugador.formatearMonedas(j.jugador.monedas), j.jugador.centimosDeMoneda)
                : T.t("carrera_obs.sin_moneda");
        p.textoCentrado(premio, cx + cw / 2, cy + 56, ui.letra * 0.84f, true,
                monedaGanada ? 0xFFC857 : ui.tema.textoSuave, 1);
        long danoPct = Math.round(carrera.porcentajeDanoJugador());
        String lineaMedalla = T.t(medalla.claveTexto) + " · " + T.f("carrera_obs.dano_recibido", danoPct);
        p.textoCentrado(lineaMedalla, cx + cw / 2, cy + 90, ui.letra * 0.88f, true, medalla.color, 1);
        String desglose1 = T.f("carrera_obs.desglose_bajas", carrera.objetosDestruidos(),
                carrera.defectuososDestruidos(), carrera.rivalDestruido() ? 1 : 0);
        p.textoCentrado(desglose1, cx + cw / 2, cy + 126, ui.letra * 0.76f, false, ui.tema.texto, 1);
        String desglose2 = T.f("carrera_obs.desglose_extras", carrera.monedasExtraRecogidas(),
                carrera.cuartosGanados() > 0 ? "+0.25" : "0");
        p.textoCentrado(desglose2, cx + cw / 2, cy + 156, ui.letra * 0.76f, false, 0xFFD23F, 1);
        if (carrera.ganador() == 0 && medalla.esPerfecta) {
            p.textoCentrado(T.t("carrera_obs.reconocimiento_perfecta"), cx + cw / 2, cy + 190,
                    ui.letra * 0.78f, true, 0xFFD23F, 1);
            String cuartos = carrera.monedasExtraPorCuartos() > 0
                    ? T.f("carrera_obs.cuarto_completado", j.jugador.cuartosDeMoneda)
                    : T.f("carrera_obs.cuarto_ganado", j.jugador.cuartosDeMoneda);
            p.textoCentrado(cuartos, cx + cw / 2, cy + 220, ui.letra * 0.76f, false, 0x9CFF57, 1);
        }
        float bw = (cw - 60) / 3;
        if (ui.boton(T.t("carrera_obs.repetir"), cx + 20, cy + ch - 64, bw, 46, Ui.Estilo.PRINCIPAL)) return 1;
        if (ui.boton(T.t("carrera_obs.editar"), cx + 30 + bw, cy + ch - 64, bw, 46, Ui.Estilo.NORMAL)) return 2;
        if (ui.boton(T.t("carrera_obs.nueva_pista"), cx + 40 + bw * 2, cy + ch - 64, bw, 46, Ui.Estilo.PLANO)) return 3;
        return 0;
    }

    private static void dibujarMetaCuadros(Pincel p, float x, float y, float h, int paso) {
        int filas = Math.max(2, (int) (h / paso));
        for (int i = 0; i < filas; i++) {
            float fy = y + i * paso;
            float fh = Math.min(paso, y + h - fy);
            p.rect(x, fy, 4, fh, i % 2 == 0 ? 0xFFFFFF : 0x111318, 1);
            p.rect(x + 4, fy, 4, fh, i % 2 == 0 ? 0x111318 : 0xFFFFFF, 1);
        }
    }
}
