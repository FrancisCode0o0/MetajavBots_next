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
        float cwMitad = gw / 2 - 8;
        float my = ui.panel(x, y + 246, cwMitad, h2 - 246, T.t("historial.por_modelo"));
        float tFila = Math.min(17f, ui.letra * 0.82f);
        for (Map.Entry<String, int[]> en : porModelo.entrySet()) {
            if (my + tFila > p.alto - 32) break;
            String nom = Modelo.de(en.getKey()).nombre(T.idioma);
            String cta = en.getValue()[0] + " / " + en.getValue()[1];
            float wCta = p.ancho(cta, tFila, false) + 16;
            float tNom = ui.cabeEntero(nom, cwMitad - 36 - wCta, tFila, false);
            p.texto(nom, x + 18, my, tNom, false, ui.tema.texto, 1);
            p.textoDerecha(cta, x + cwMitad - 18, my, tFila, false, ui.tema.textoSuave, 1);
            my += tFila * 1.42f;
        }
        float mx = x + gw / 2 + 8;
        float my2 = ui.panel(mx, y + 246, cwMitad, h2 - 246, T.t("historial.por_modo"));
        for (Map.Entry<String, int[]> en : porModo.entrySet()) {
            if (my2 + tFila > p.alto - 32) break;
            String cta = en.getValue()[0] + " / " + en.getValue()[1];
            float wCta = p.ancho(cta, tFila, false) + 16;
            float tNom = ui.cabeEntero(en.getKey(), cwMitad - 36 - wCta, tFila, false);
            p.texto(en.getKey(), mx + 18, my2, tNom, false, ui.tema.texto, 1);
            p.textoDerecha(cta, mx + cwMitad - 18, my2, tFila, false, ui.tema.textoSuave, 1);
            my2 += tFila * 1.42f;
        }
        // A la derecha: los ejercicios por tema y la lista de peleas.
        columnaDerecha(j, h, x + gw + 20, y, p.ancho - (x + gw + 20) - 20, h2);
    }

    private void columnaDerecha(Juego j, List<Registro> h, float rx, float y, float rw, float h2) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        float ey = ui.panel(rx, y, rw, 330, T.t("historial.ejercicios"));
        int cols = 2, i = 0;
        List<Tema> temas = Tema.todos();
        int filasTemas = (temas.size() + cols - 1) / cols;
        float pasoY = Math.min(24f, (y + 320 - ey) / Math.max(1, filasTemas));
        float tEj = Math.min(14.5f, ui.letra * 0.68f);
        for (Tema tm : temas) {
            List<Ejercicio> es = Banco.delJuego().deTema(tm.id);
            int hechos = 0;
            for (Ejercicio ej : es) if (jug.ejerciciosResueltos.contains(ej.id)) hechos++;
            float col = (rw - 32) / cols, tx = rx + 16 + (i % cols) * col, ty = ey + (i / cols) * pasoY;
            float cifra = p.ancho("00/00", tEj, false);
            p.texto(tm.id, tx, ty, tEj, false, ui.tema.textoSuave, 1);
            float bx = tx + p.ancho("00", tEj, false) + 10;
            ui.barra(bx, ty + 4, col - (bx - tx) - cifra - 40, 10, es.isEmpty() ? 0 : (float) hechos / es.size(), ui.tema.bien, null);
            p.textoDerecha(hechos + "/" + es.size(), tx + col - 30, ty, tEj, false, ui.tema.textoSuave, 1);
            i++;
        }
        float ly = ui.panel(rx, y + 346, rw, h2 - 346, T.t("historial.ultimas"));
        float dd = ui.desplazable(lista, rx + 8, ly, rw - 16, y + h2 - ly - 10);
        float yy = ly - dd;
        SimpleDateFormat f = new SimpleDateFormat("dd/MM HH:mm");
        for (int k = h.size() - 1; k >= 0; k--) {
            Registro r = h.get(k);
            int color = r.resultado == Registro.Resultado.VICTORIA ? ui.tema.bien : r.resultado == Registro.Resultado.DERROTA ? ui.tema.mal : ui.tema.aviso;
            float tl = Math.min(16.5f, ui.letra * 0.75f);
            String fecha = f.format(new Date(r.fecha)), resultado = T.t("resultado." + r.resultado.name().toLowerCase());
            float c2 = rx + 16 + p.ancho("00/00 00:00", tl, false) + 14, c3 = c2 + p.ancho("Unentschieden", tl, true) + 14;
            p.texto(fecha, rx + 16, yy, tl, false, ui.tema.textoSuave, 1);
            p.texto(resultado, c2, yy, tl, true, color, 1);
            String danoTxt = Math.round(r.danoHecho) + " / " + Math.round(r.danoRecibido);
            float cifras = Math.max(p.ancho("000 / 000", tl, false), p.ancho(danoTxt, tl, false)) + 36;
            String rivalModo = r.rival + " (" + r.modo + ")";
            float tRiv = ui.cabeEntero(rivalModo, Math.max(60, rx + rw - cifras - c3), tl, false);
            p.texto(rivalModo, c3, yy, tRiv, false, ui.tema.texto, 1);
            p.textoDerecha(danoTxt, rx + rw - 28, yy, tl, false, ui.tema.textoSuave, 1);
            yy += tl * 1.5f;
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
        int[] acentos = {ui.tema.acento, ui.tema.bien, ui.tema.mal, ui.tema.aviso, 0xFFC857, 0x3FE0FF, 0xFF8FB1, 0x9B7EDE};
        float cw = (p.ancho - 40) / cifras.length;
        for (int i = 0; i < cifras.length; i++) {
            float cx = x + i * cw;
            int acento = acentos[i % acentos.length];
            p.caja(cx + 4, y + 3, cw - 8, 92, 10, 0x000000, 0.28f);
            p.caja(cx + 4, y, cw - 8, 92, 10, ui.tema.panel, 1);
            p.bordeRedondeado(cx + 4, y, cw - 8, 92, 10, acento, 0.55f, 1.3f);
            p.caja(cx + 12, y, cw - 24, 3, 1.5f, acento, 0.9f);
            float tv = ui.cabeEntero(cifras[i][1], cw - 20, Math.min(34f, ui.letra * 1.75f), true);
            p.textoCentrado(cifras[i][1], cx + cw / 2, y + 12, tv, true, ui.tema.texto, 1);
            float tl = ui.cabeEntero(cifras[i][0], cw - 20, Math.min(16f, ui.letra * 0.72f), false);
            p.textoCentrado(cifras[i][0], cx + cw / 2, y + 60, tl, false, ui.tema.textoSuave, 1);
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
