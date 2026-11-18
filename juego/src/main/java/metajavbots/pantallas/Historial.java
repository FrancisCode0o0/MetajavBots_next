package metajavbots.pantallas;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import metajavbots.Juego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Registro;
import metajavbots.textos.T;

/**
 * HISTORIAL — Las victorias y derrotas, con sus estadísticas: el marcador,
 * la racha, el daño medio, cómo va contra cada modelo y en cada modo, la
 * evolución de las últimas peleas, y los ejercicios resueltos por tema.
 */
public final class Historial implements Pantalla {

    private final Ui.Desplazable lista = new Ui.Desplazable();

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.historial"), true);
        List<Registro> h = jug.historial;
        float x = 20, y = arriba + 16;
        cifras(j, x, y);
        y += 106;
        float h2 = p.alto - y - 20;
        evolucion(j, x, y);
        float gw = (p.ancho - 60) * 0.5f;
        // Contra cada modelo y en cada modo.
        Map<String, int[]> porModelo = new LinkedHashMap<>(), porModo = new LinkedHashMap<>();
        for (Registro r : h) {
            int[] a = porModelo.computeIfAbsent(r.modeloRival, k -> new int[2]);
            int[] b = porModo.computeIfAbsent(r.modo, k -> new int[2]);
            if (r.resultado == Registro.Resultado.VICTORIA) {
                a[0]++;
                b[0]++;
            }
            a[1]++;
            b[1]++;
        }
        float my = ui.panel(x, y + 246, gw / 2 - 8, h2 - 246, T.t("historial.por_modelo"));
        for (Map.Entry<String, int[]> en : porModelo.entrySet()) {
            if (my > p.alto - 50) break;
            p.texto(Modelo.de(en.getKey()).nombre(T.idioma), x + 18, my, ui.letra * 0.85f, false, ui.tema.texto, 1);
            p.textoDerecha(en.getValue()[0] + " / " + en.getValue()[1], x + gw / 2 - 26, my, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
            my += ui.letra * 1.4f;
        }
        float mx = x + gw / 2 + 8;
        float my2 = ui.panel(mx, y + 246, gw / 2 - 8, h2 - 246, T.t("historial.por_modo"));
        for (Map.Entry<String, int[]> en : porModo.entrySet()) {
            if (my2 > p.alto - 50) break;
            p.texto(en.getKey(), mx + 18, my2, ui.letra * 0.85f, false, ui.tema.texto, 1);
            p.textoDerecha(en.getValue()[0] + " / " + en.getValue()[1], mx + gw / 2 - 34, my2, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
            my2 += ui.letra * 1.4f;
        }
        // A la derecha: los ejercicios por tema y la lista de peleas.
        float rx = x + gw + 20, rw = p.ancho - rx - 20;
        float ey = ui.panel(rx, y, rw, 330, T.t("historial.ejercicios"));
        int cols = 2;
        int i = 0;
        for (Tema tm : Tema.todos()) {
            List<Ejercicio> es = Banco.delJuego().deTema(tm.id);
            int hechos = 0;
            for (Ejercicio ej : es) if (jug.ejerciciosResueltos.contains(ej.id)) hechos++;
            // Cada columna con su hueco a la derecha: la cifra de una no se pega al número de la siguiente.
            float col = (rw - 32) / cols, tx = rx + 16 + (i % cols) * col, ty = ey + (i / cols) * 24;
            float t = ui.letra * 0.7f, cifra = p.ancho("00/00", t, false);
            p.texto(tm.id, tx, ty, t, false, ui.tema.textoSuave, 1);
            float bx = tx + p.ancho("00", t, false) + 10;
            ui.barra(bx, ty + 4, col - (bx - tx) - cifra - 40, 10, es.isEmpty() ? 0 : (float) hechos / es.size(), ui.tema.bien, null);
            p.textoDerecha(hechos + "/" + es.size(), tx + col - 30, ty, t, false, ui.tema.textoSuave, 1);
            i++;
        }
        float ly = ui.panel(rx, y + 346, rw, h2 - 346, T.t("historial.ultimas"));
        float dd = ui.desplazable(lista, rx + 8, ly, rw - 16, y + h2 - ly - 10);
        float yy = ly - dd;
        SimpleDateFormat f = new SimpleDateFormat("dd/MM HH:mm");
        for (int k = h.size() - 1; k >= 0; k--) {
            Registro r = h.get(k);
            int color = r.resultado == Registro.Resultado.VICTORIA ? ui.tema.bien : r.resultado == Registro.Resultado.DERROTA ? ui.tema.mal : ui.tema.aviso;
            // Las columnas, del ancho de lo que llevan (con letra grande no se pisan).
            float tl = ui.letra * 0.75f;
            String fecha = f.format(new Date(r.fecha)), resultado = T.t("resultado." + r.resultado.name().toLowerCase());
            float c2 = rx + 16 + p.ancho("00/00 00:00", tl, false) + 14, c3 = c2 + p.ancho("Unentschieden", tl, true) + 14;
            p.texto(fecha, rx + 16, yy, tl, false, ui.tema.textoSuave, 1);
            p.texto(resultado, c2, yy, tl, true, color, 1);
            float cifras = p.ancho("000 / 000", tl, false) + 40;
            p.texto(ui.recortarTexto(r.rival + " (" + r.modo + ")", rx + rw - cifras - c3, tl, false), c3, yy, tl, false, ui.tema.texto, 1);
            p.textoDerecha(Math.round(r.danoHecho) + " / " + Math.round(r.danoRecibido), rx + rw - 30, yy, ui.letra * 0.75f, false, ui.tema.textoSuave, 1);
            yy += ui.letra * 1.3f;
        }
        ui.finDesplazable(lista, yy + dd - ly);
    }

    static int mejorRacha(List<Registro> h) {
        int mejor = 0, actual = 0;
        for (Registro r : h) {
            actual = r.resultado == Registro.Resultado.VICTORIA ? actual + 1 : 0;
            mejor = Math.max(mejor, actual);
        }
        return mejor;
    }

    /** Arriba, las cifras grandes: peleas, victorias, derrotas, empates, porcentaje, mejor racha, daño medio y experiencia. */
    private void cifras(Juego j, float x, float y) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        List<Registro> h = jug.historial;
        int v = jug.victorias(), d = jug.derrotas(), e = h.size() - v - d;
        // Las cifras grandes.
        String[][] cifras = {
                {T.t("historial.peleas"), Integer.toString(h.size())},
                {T.t("historial.victorias"), Integer.toString(v)},
                {T.t("historial.derrotas"), Integer.toString(d)},
                {T.t("historial.empates"), Integer.toString(e)},
                {T.t("historial.porcentaje"), h.isEmpty() ? "—" : Math.round(100.0 * v / h.size()) + " %"},
                {T.t("historial.mejor_racha"), Integer.toString(mejorRacha(h))},
                {T.t("historial.dano_medio"), h.isEmpty() ? "—" : Long.toString(Math.round(h.stream().mapToDouble(r -> r.danoRecibido).average().orElse(0)))},
                {T.t("historial.experiencia"), Long.toString(jug.experiencia)}};
        float cw = (p.ancho - 40) / cifras.length;
        for (int i = 0; i < cifras.length; i++) {
            float cx = x + i * cw;
            p.caja(cx + 4, y, cw - 8, 90, 10, ui.tema.panel, 1);
            p.textoCentrado(cifras[i][1], cx + cw / 2, y + 12, ui.letra * 1.8f, true, ui.tema.texto, 1);
            p.textoCentrado(cifras[i][0], cx + cw / 2, y + 60, ui.letra * 0.75f, false, ui.tema.textoSuave, 1);
        }
    }

    /** Las últimas 30 peleas, en barras: verde gana, rojo pierde; el alto es el daño hecho menos el recibido. */
    private void evolucion(Juego j, float x, float y) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Registro> h = j.jugador.historial;
        // Las últimas 30 peleas (barras: verde gana, rojo pierde; alto = daño hecho − recibido).
        float gw = (p.ancho - 60) * 0.5f;
        float gy = ui.panel(x, y, gw, 230, T.t("historial.evolucion"));
        int n = Math.min(30, h.size());
        float bw = (gw - 40) / 30;
        float base = gy + (230 - (gy - y)) / 2;
        p.rect(x + 20, base, gw - 40, 1, ui.tema.borde, 1);
        for (int i = 0; i < n; i++) {
            Registro r = h.get(h.size() - n + i);
            float alto = (float) Math.max(-80, Math.min(80, (r.danoHecho - r.danoRecibido) / 3));
            int color = r.resultado == Registro.Resultado.VICTORIA ? ui.tema.bien : r.resultado == Registro.Resultado.DERROTA ? ui.tema.mal : ui.tema.aviso;
            p.rect(x + 20 + i * bw, alto >= 0 ? base - alto : base, bw - 3, Math.max(2, Math.abs(alto)), color, 1);
        }
        if (n == 0) p.textoCentrado(T.t("historial.vacio"), x + gw / 2, base - 10, ui.letra * 0.9f, false, ui.tema.textoApagado, 1);
    }
}
