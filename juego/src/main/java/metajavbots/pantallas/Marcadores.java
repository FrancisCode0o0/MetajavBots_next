package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.T;

/**
 * MARCADORES — La barra superior de la pelea (88 px de alto), tanto en duelos
 * de uno contra uno como en peleas de varios robots (battle royale, equipos).
 * Calcula geometrías sin solapamientos para letras grandes y textos largos.
 */
public final class Marcadores {

    /** Altura fija de la barra superior del marcador. */
    public static final float ALTO = 88f;

    /** Geometría horizontal del marcador 1 contra 1 (bloques laterales, partes y columna central). */
    public record GeometriaDos(float xIzq, float xDer, float anchoBloque, float pasoParte, float anchoParte,
            float centroX, float centroAncho) {
    }

    /** Geometría horizontal del marcador de varios robots (tarjetas y bloque derecho de ronda/reloj). */
    public record GeometriaVarios(float anchoPaso, float anchoTarjeta, float xReloj, float anchoReloj) {
    }

    /** Geometría de la banda superior del locutor para coordinar rótulos de la arena y evitar solapamientos. */
    public record GeometriaBanda(boolean visible, float x, float y, float w, float h, float techo) {
    }

    /** Geometría calculada de la barra inferior de controles para evitar solapamientos con cualquier idioma o letra. */
    public record BarraInferior(float y, float finIzq, float wPaso, float xVisor, float wVisor, float xDepurador,
            float wDepurador, float xRendirse, float wRendirse, float xAparatoIzq, float xAparatoDer, float yAparato,
            boolean aparatoArriba) {
    }

    private Marcadores() {
    }

    /** Si en la barra inferior cabe el botón «?» (¿Cómo se juega?) sin apretar los demás. */
    public static boolean cabeAyuda(float der) {
        return der >= 760f;
    }

    /** Calcula la geometría sin solapamientos de la barra inferior de controles y del botón del aparato. */
    public static BarraInferior calcularBarraInferior(float pantallaAlto, float der, boolean enPausa, float wPasoDeseado,
            float wVisorDeseado, float wDepDeseado, float wRendirseDeseado, boolean hayAparato, float wAparatoDeseado) {
        float y = pantallaAlto - 64f;
        float margen = 20f;
        float gap = 10f;
        // Pausa, ×1, ×2, ×4 y, si cabe, «?» (¿Cómo se juega?).
        float baseIzq = margen + 64f + (cabeAyuda(der) ? 4f : 3f) * 62f;
        float wPaso = enPausa ? Math.max(84f, Math.min(wPasoDeseado, Math.max(84f, der - margen - baseIzq - 240f))) : 0f;
        float finIzq = enPausa ? baseIzq + wPaso : baseIzq - 6f;
        float wRend = Math.max(116f, wRendirseDeseado);
        float wDep = Math.max(132f, wDepDeseado);
        float wVis = Math.max(96f, wVisorDeseado);
        float maxDer = Math.max(160f, der - margen - finIzq - gap);
        float dispBotones = Math.max(120f, maxDer - 2f * gap);
        float sumaDer = wVis + wDep + wRend;
        if (sumaDer > dispBotones) {
            float factor = dispBotones / sumaDer;
            wVis *= factor;
            wDep *= factor;
            wRend *= factor;
        }
        float xRend = der - margen - wRend;
        float xDep = xRend - gap - wDep;
        float xVis = xDep - gap - wVis;
        float xApIzq = finIzq + gap;
        float xApDer = xVis - gap;
        boolean arriba = hayAparato && (xApDer - xApIzq) < PanelDelAparato.ANCHO_BOTON_MIN;
        float yAp = arriba ? y - (PanelDelAparato.ALTO_BOTON + 8f) : y;
        if (arriba) {
            xApIzq = margen;
            xApDer = Math.min(der - margen, margen + Math.max(PanelDelAparato.ANCHO_BOTON_MIN, wAparatoDeseado));
        }
        return new BarraInferior(y, finIzq, wPaso, xVis, wVis, xDep, wDep, xRend, wRend, xApIzq, xApDer, yAp, arriba);
    }

    /** Calcula la caja de la banda del locutor y su techo inferior evitando el marcador y el panel de reparación. */
    public static GeometriaBanda geometriaBanda(float anchoZona, boolean visible, boolean conReparacion, float altoParrafo) {
        if (!visible) return new GeometriaBanda(false, 0f, 0f, 0f, 0f, 0f);
        float minX = conReparacion ? 282f : 30f;
        float maxAncho = Math.max(220f, anchoZona - minX - 30f);
        float w = Math.min(1000f, maxAncho);
        float x = minX + (maxAncho - w) / 2f;
        float y = 100f;
        float alto = Math.max(64f, altoParrafo + 26f);
        return new GeometriaBanda(true, x, y, w, alto, y + alto + 8f);
    }

    /** Calcula la distribución horizontal del marcador 1v1 sin solapamiento entre laterales y centro. */
    public static GeometriaDos geometriaDos(float pantallaAncho) {
        float margen = 20f;
        float centroMin = Math.min(300f, Math.max(200f, pantallaAncho * 0.2f));
        float anchoBloque = Math.min(588f, Math.max(240f, (pantallaAncho - 2f * margen - centroMin - 16f) / 2f));
        float xIzq = margen;
        float xDer = pantallaAncho - margen - anchoBloque;
        float centroX = xIzq + anchoBloque + 8f;
        float centroAncho = Math.max(120f, xDer - centroX - 8f);
        float gapParte = 8f;
        float anchoParte = Math.max(40f, (anchoBloque - 3f * gapParte) / 4f);
        float pasoParte = anchoParte + gapParte;
        return new GeometriaDos(xIzq, xDer, anchoBloque, pasoParte, anchoParte, centroX, centroAncho);
    }

    /** Calcula la distribución horizontal del marcador de varios robots sin pisar el reloj derecho. */
    public static GeometriaVarios geometriaVarios(float pantallaAncho, int numCombatientes) {
        int n = Math.max(1, numCombatientes);
        float anchoReloj = Math.min(240f, Math.max(170f, pantallaAncho * 0.15f));
        float margenIzq = 16f;
        float disp = Math.max(80f, pantallaAncho - anchoReloj - margenIzq - 8f);
        float anchoPaso = Math.min(260f, disp / n);
        float anchoTarjeta = Math.max(36f, anchoPaso - 12f);
        float xReloj = pantallaAncho - anchoReloj;
        return new GeometriaVarios(anchoPaso, anchoTarjeta, xReloj, anchoReloj);
    }

    /** Dibuja el marcador superior en un duelo de dos combatientes. */
    static void deDos(Juego j, Pelea pelea, Encuentro encuentro) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 0, p.ancho, ALTO, ui.tema.panel, 0.97f);
        p.rect(0, ALTO - 1, p.ancho, 1, ui.tema.borde, 1);
        GeometriaDos g = geometriaDos(p.ancho);
        String[] nombres = {T.t("parte.cabeza"), T.t("parte.izquierda"), T.t("parte.derecha"), T.t("parte.piernas")};
        for (int i = 0; i < 2; i++) {
            dibujarCombatienteDos(j, pelea, g, nombres, i);
        }
        dibujarCentroDos(j, pelea, encuentro, g);
    }

    private static void dibujarCombatienteDos(Juego j, Pelea pelea, GeometriaDos g, String[] nombres, int i) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Cuerpo c = pelea.cuerpo(i);
        Combatiente co = pelea.combatientes[i];
        float x = i == 1 ? g.xDer() : g.xIzq();
        int color = i == 0 ? 0x3FE0FF : 0xFF4FD8;
        String dano = T.f("pelea.dano", Math.round(co.danoTotalRecibido));
        float tamDanoMax = Math.min(18f, ui.letra * 0.78f);
        float maxAnchoDano = g.anchoBloque() * 0.44f;
        float tamDano = ui.cabe(dano, maxAnchoDano, tamDanoMax, false);
        String danoVis = ui.recortarTexto(dano, maxAnchoDano, tamDano, false);
        float danoW = c == null ? 0f : p.ancho(danoVis, tamDano, false);
        String nombre = PantallaPelea.etiquetaMarcador(co, T.idioma);
        float anchoNom = Math.max(80f, g.anchoBloque() - danoW - 14f);
        float tamNom = ui.cabe(nombre, anchoNom, Math.min(20f, ui.letra * 0.92f), true);
        String nomVis = ui.recortarTexto(nombre, anchoNom, tamNom, true);
        p.texto(nomVis, x, 6, tamNom, true, color, 1);
        if (!nomVis.equals(nombre) && ui.e.dentro(x, 4, anchoNom, 26)) ui.avisar(nombre);
        if (c == null) return;
        p.textoDerecha(danoVis, x + g.anchoBloque(), 8, tamDano, false, ui.tema.texto, 1);
        if (!danoVis.equals(dano) && ui.e.dentro(x + g.anchoBloque() - danoW, 4, danoW, 26)) ui.avisar(dano);
        float by = 34f;
        float tamParteMax = Math.min(13.5f, ui.letra * 0.6f);
        for (int k = 0; k < 4; k++) {
            float px = x + k * g.pasoParte();
            float parte = (float) (c.parte[k] / Math.max(1, c.parteMax[k]));
            int col = parte > 0.5f ? ui.tema.bien : parte > 0.2f ? ui.tema.aviso : ui.tema.mal;
            ui.barra(px, by, g.anchoParte(), 14, parte, col, null);
            float tp = ui.cabe(nombres[k], g.anchoParte() - 4f, tamParteMax, false);
            p.texto(ui.recortarTexto(nombres[k], g.anchoParte() - 4f, tp, false), px + 2, by + 16, tp, false, ui.tema.textoSuave, 1);
            if (ui.e.dentro(px, by - 1, g.anchoParte(), 34)) {
                ui.avisar(nombres[k] + ": " + Math.round(c.parte[k]) + " / " + Math.round(c.parteMax[k]));
            }
        }
        ui.barra(x, 72, g.anchoBloque(), 7, (float) (c.energia / Math.max(1, c.cifras.energiaMax)), 0xFFD23F, null);
        if (ui.e.dentro(x, 69, g.anchoBloque(), 16)) ui.avisar(T.t("pelea.energia") + ": " + Math.round(c.energia));
    }

    private static void dibujarCentroDos(Juego j, Pelea pelea, Encuentro encuentro, GeometriaDos g) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cx = g.centroX() + g.centroAncho() / 2f;
        String ronda = T.f("pelea.ronda", pelea.ronda(), pelea.rondas);
        float tamRonda = ui.cabe(ronda, g.centroAncho(), Math.min(19f, ui.letra * 0.95f), true);
        p.textoCentrado(ui.recortarTexto(ronda, g.centroAncho(), tamRonda, true), cx, 6, tamRonda, true, ui.tema.texto, 1);
        int segundos = (int) Math.ceil((pelea.turnosPorRonda - pelea.turno()) / (double) Pelea.TURNOS_POR_SEGUNDO);
        String reloj = String.format("%d:%02d", Math.max(0, segundos) / 60, Math.max(0, segundos) % 60);
        boolean esSenior = pelea.presupuestoNanosPorTurno > 0 || pelea.presupuestoBytesPorTurno > 0 || pelea.pensarEnParalelo;
        boolean tieneSub = encuentro.objetivo != null || encuentro.jefe != null || esSenior;
        float tamReloj = tieneSub ? Math.min(28f, ui.letra * 1.35f) : Math.min(34f, ui.letra * 1.55f);
        float yReloj = tieneSub ? 30f : 36f;
        p.textoCentrado(reloj, cx, yReloj, tamReloj, true, segundos <= 10 ? ui.tema.mal : ui.tema.texto, 1);
        if (!tieneSub) return;
        String sub;
        int colSub;
        if (encuentro.jefe != null) {
            boolean vulnerable = pelea.resistencia[1] >= 1;
            sub = vulnerable ? T.t("pelea.jefe_vulnerable") : T.t("pelea.jefe_inmune");
            colSub = vulnerable ? ui.tema.bien : ui.tema.mal;
        } else if (encuentro.objetivo != null) {
            sub = Objetivo.texto(encuentro.objetivo);
            colSub = ui.tema.aviso;
        } else {
            Cuerpo c0 = pelea.cuerpo(0);
            boolean mal = c0 != null && ((pelea.presupuestoNanosPorTurno > 0 && c0.nanosDelTurno > pelea.presupuestoNanosPorTurno)
                    || (pelea.presupuestoBytesPorTurno > 0 && c0.bytesDelTurno > pelea.presupuestoBytesPorTurno));
            sub = resumenSeniorHud(pelea, c0);
            colSub = mal ? ui.tema.mal : 0x3FE0FF;
        }
        float tamSub = ui.cabe(sub, g.centroAncho(), Math.min(13.5f, ui.letra * 0.68f), false);
        String subVis = ui.recortarTexto(sub, g.centroAncho(), tamSub, false);
        p.textoCentrado(subVis, cx, 67, tamSub, false, colSub, 1);
        if (!subVis.equals(sub) && ui.e.dentro(g.centroX(), 64, g.centroAncho(), 22)) ui.avisar(sub);
    }

    /** Texto compacto para el HUD central en peleas Senior (tiempo, memoria o paralelo). */
    public static String resumenSeniorHud(Pelea pelea, Cuerpo c) {
        StringBuilder sb = new StringBuilder();
        if (pelea.presupuestoNanosPorTurno > 0) {
            double us = c == null ? 0 : c.nanosDelTurno / 1_000_000.0;
            double max = pelea.presupuestoNanosPorTurno / 1_000_000.0;
            sb.append(String.format(java.util.Locale.ROOT, "⏱ %.1f/%.1fms", us, max));
        }
        if (pelea.presupuestoBytesPorTurno > 0) {
            if (sb.length() > 0) sb.append(" · ");
            double kb = c == null ? 0 : c.bytesDelTurno / 1024.0;
            double maxKb = pelea.presupuestoBytesPorTurno / 1024.0;
            sb.append(String.format(java.util.Locale.ROOT, "MEM %.1f/%.0fKB", kb, maxKb));
        }
        if (pelea.pensarEnParalelo) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("|| ").append(T.t("pelea.paralelo"));
        }
        return sb.toString();
    }

    /** Dibuja el marcador superior cuando pelean más de dos robots. */
    static void deVarios(Juego j, Pelea pelea) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 0, p.ancho, ALTO, ui.tema.panel, 0.97f);
        p.rect(0, ALTO - 1, p.ancho, 1, ui.tema.borde, 1);
        int n = pelea.combatientes.length;
        GeometriaVarios g = geometriaVarios(p.ancho, n);
        for (int i = 0; i < n; i++) {
            Cuerpo c = pelea.cuerpo(i);
            Combatiente co = pelea.combatientes[i];
            float x = 16f + i * g.anchoPaso();
            int color = VistaDeArena.COLORES[pelea.equipo[i] % VistaDeArena.COLORES.length];
            float tamNom = ui.cabe(co.nombre, g.anchoTarjeta(), Math.min(17f, ui.letra * 0.78f), true);
            p.texto(ui.recortarTexto(co.nombre, g.anchoTarjeta(), tamNom, true), x, 5, tamNom, true, color, 1);
            if (c != null) {
                float parte = (float) (c.blindaje() / 100);
                ui.barra(x, 28, g.anchoTarjeta(), 12, parte, parte > 0.5f ? ui.tema.bien : parte > 0.2f ? ui.tema.aviso : ui.tema.mal, null);
            }
            String dano = T.f("pelea.dano", Math.round(co.danoTotalRecibido));
            float tamDano = ui.cabe(dano, g.anchoTarjeta(), Math.min(13.5f, ui.letra * 0.62f), false);
            p.texto(ui.recortarTexto(dano, g.anchoTarjeta(), tamDano, false), x, 44, tamDano, false, ui.tema.textoSuave, 1);
            if (co.montaje != null && co.montaje.modelo != null) {
                String modelo = T.f("pelea.modelo", co.montaje.modelo.nombre(T.idioma));
                float tamMod = ui.cabe(modelo, g.anchoTarjeta(), Math.min(13f, ui.letra * 0.58f), false);
                p.texto(ui.recortarTexto(modelo, g.anchoTarjeta(), tamMod, false), x, 64, tamMod, false, ui.tema.textoSuave, 1);
            }
            if (ui.e.dentro(x, 4, g.anchoTarjeta(), 80)) {
                ui.avisar(PantallaPelea.etiquetaMarcador(co, T.idioma) + "\n" + dano);
            }
        }
        float cx = g.xReloj() + g.anchoReloj() / 2f;
        String ronda = T.f("pelea.ronda", pelea.ronda(), pelea.rondas);
        float tamRonda = ui.cabe(ronda, g.anchoReloj() - 16f, Math.min(18f, ui.letra * 0.88f), true);
        p.textoCentrado(ui.recortarTexto(ronda, g.anchoReloj() - 16f, tamRonda, true), cx, 8, tamRonda, true, ui.tema.texto, 1);
        int segundos = (int) Math.ceil((pelea.turnosPorRonda - pelea.turno()) / (double) Pelea.TURNOS_POR_SEGUNDO);
        float tamReloj = Math.min(32f, ui.letra * 1.45f);
        p.textoCentrado(String.format("%d:%02d", Math.max(0, segundos) / 60, Math.max(0, segundos) % 60), cx, 36, tamReloj, true,
                segundos <= 10 ? ui.tema.mal : ui.tema.texto, 1);
    }
}
