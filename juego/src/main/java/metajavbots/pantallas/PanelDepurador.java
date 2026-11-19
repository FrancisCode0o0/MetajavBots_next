package metajavbots.pantallas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.api.RobotBase;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.OrdenDelTurno;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.T;

/**
 * PANEL DEPURADOR (F9) — El depurador en vivo durante una pelea del jugador:
 * enseña los pasos por turno, telemetría y dos pestañas compactas:
 * 1) Variables y consola del robot.
 * 2) Código / Turno: el código del jugador con las líneas ejecutadas iluminadas
 *    (y ▶ en la última), las órdenes pedidas en el turno y avisos en ámbar si
 *    una llamada posterior pisa la orden de un evento anterior.
 */
final class PanelDepurador {

    private final Ui.Desplazable desplConsola = new Ui.Desplazable();
    private final Ui.Desplazable desplVariables = new Ui.Desplazable();
    private final Ui.Desplazable desplOrdenes = new Ui.Desplazable();
    private final VentanaDeCodigo visorCodigo = new VentanaDeCodigo();
    private String fuenteCargada = "";
    private int pestana;

    PanelDepurador() {
        visorCodigo.soloLectura = true;
        visorCodigo.seguirActiva = true;
    }

    /** Selecciona la pestaña activa (0: variables y consola; 1: código y órdenes del turno). */
    void pestana(int p) {
        this.pestana = p == 1 ? 1 : 0;
    }

    void dibujar(Juego j, Pelea pelea, Combatiente yo, String fuente, boolean conTraza, float x) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float w = p.ancho - x, y = 88, h = p.alto - 88;
        p.rect(x, y, w, h, ui.tema.panel, 0.98f);
        p.rect(x, y, 1, h, ui.tema.borde, 1);
        float ty = y + 10;
        float tamTitulo = ui.cabe(T.t("pelea.depurador"), w - 32, ui.letra * 1.05f, true);
        p.texto(T.t("pelea.depurador"), x + 16, ty, tamTitulo, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamTitulo) + 4;
        Cuerpo c = pelea.cuerpo(0);
        if (c != null) {
            long tope = pelea.topeDePasos > 0 ? pelea.topeDePasos : (long) c.cifras.pasos;
            float parte = Math.min(1, c.pasosDelTurno / (float) tope);
            ui.barra(x + 16, ty, w - 32, 18, parte, parte > 0.8f ? ui.tema.mal : ui.tema.acento, T.f("pelea.pasos", c.pasosDelTurno, tope));
            ty += 24;
            String pos = String.format(java.util.Locale.ROOT, "x=%.0f  y=%.0f  rumbo=%.0f°  mira=%.0f°", c.x, c.y, c.rumbo(), c.mira());
            float tamPos = ui.cabe(pos, w - 32, ui.letra * 0.78f, false);
            p.texto(pos, x + 16, ty, tamPos, false, ui.tema.textoSuave, 1);
            ty += p.altoDeLinea(tamPos) + 2;
            String est = String.format(java.util.Locale.ROOT, "%s=%.0f  %s=%.0f  %s=%d", T.t("pelea.blindaje"), c.blindaje(),
                    T.t("pelea.energia"), c.energia, T.t("pelea.turno"), pelea.turno());
            float tamEst = ui.cabe(est, w - 32, ui.letra * 0.78f, false);
            p.texto(est, x + 16, ty, tamEst, false, ui.tema.textoSuave, 1);
            ty += p.altoDeLinea(tamEst) + 6;
        }
        String tabCodigo = T.t("pelea.depurador_codigo") + (c != null && c.tieneConflictoDeOrdenes ? " ⚠" : "");
        String[] pestanas = {T.t("pelea.depurador_vars"), tabCodigo};
        pestana = ui.pestanas(pestanas, pestana, x + 16, ty, w - 32, 34);
        ty += 42;
        if (pestana == 1) codigoYTurno(j, c, fuente, conTraza, x, ty, w, y + h - 14);
        else variablesYConsola(j, yo, x, ty, w, y + h - 18);
    }

    private void variablesYConsola(Juego j, Combatiente yo, float x, float ty, float w, float abajo) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float tamCab = ui.cabe(T.t("pelea.variables"), w - 32, ui.letra * 0.88f, true);
        p.texto(T.t("pelea.variables"), x + 16, ty, tamCab, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 4;
        List<String[]> vars = variables(yo.robot());
        float restante = Math.max(120, abajo - ty);
        float zona = restante * 0.45f;
        float tamVar = Math.min(20, ui.letra * 0.78f);
        float altoVar = p.altoDeLinea(tamVar) + 2;
        float d = ui.desplazable(desplVariables, x + 8, ty, w - 16, zona);
        float yy = ty - d;
        for (String[] v : vars) {
            p.texto(ui.recortarTexto(v[0], 158, tamVar, false), x + 16, yy, tamVar, false, ui.tema.tipo, 1);
            p.texto(ui.recortarTexto(v[1], w - 196, tamVar, false), x + 180, yy, tamVar, false, ui.tema.texto, 1);
            yy += altoVar;
        }
        if (vars.isEmpty()) p.parrafo(T.t("pelea.sin_variables"), x + 16, yy, w - 32, tamVar, ui.tema.textoApagado, 1);
        ui.finDesplazable(desplVariables, Math.max(1, vars.size()) * altoVar + 8);
        ty += zona + 8;
        p.texto(T.t("pelea.consola"), x + 16, ty, tamCab, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 4;
        consola(j, yo, x + 8, ty, w - 16, Math.max(60, abajo - ty));
    }

    private void consola(Juego j, Combatiente yo, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 6, ui.tema.fondo, 1);
        float tam = Math.min(18, ui.letra * 0.74f);
        float alto = p.altoDeLinea(tam) + 2;
        desplConsola.desplazamiento = Math.max(0, yo.consola.size() * alto - h + 10);
        float d = ui.desplazable(desplConsola, x, y + 4, w, h - 8);
        float yy = y + 4 - d;
        for (String l : yo.consola) {
            int color = l.contains("⚠") ? ui.tema.mal : l.contains("⏱") ? ui.tema.aviso : ui.tema.texto;
            p.texto(l, x + 8, yy, tam, false, color, 1);
            yy += alto;
        }
        ui.finDesplazable(desplConsola, yo.consola.size() * alto + 10);
    }

    private void codigoYTurno(Juego j, Cuerpo c, String fuente, boolean conTraza, float x, float ty, float w, float abajo) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String cab = T.t("pelea.depurador_traza") + (c != null && c.ultimaLinea > 0 ? " · " + T.t("mcm.linea") + " " + c.ultimaLinea : "");
        float tamCab = ui.cabe(cab, w - 32, ui.letra * 0.85f, true);
        p.texto(cab, x + 16, ty, tamCab, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 4;
        float restante = Math.max(160, abajo - ty);
        float altoCodigo = restante * 0.56f;
        if (!conTraza || fuente == null || fuente.isEmpty()) {
            p.caja(x + 8, ty, w - 16, altoCodigo, 6, ui.tema.fondo, 1);
            p.parrafo(T.t("pelea.depurador_sin_traza"), x + 18, ty + 12, w - 36, Math.min(19, ui.letra * 0.78f), ui.tema.textoApagado, 1);
        } else {
            if (!fuente.equals(fuenteCargada)) {
                visorCodigo.ponerTexto(fuente);
                fuenteCargada = fuente;
            }
            visorCodigo.tamano = Math.max(12, Math.min(20, ui.letra * 0.76f));
            if (c != null) {
                visorCodigo.lineasActivas = c.lineasDelTurno;
                visorCodigo.lineaActiva = c.ultimaLinea;
            }
            visorCodigo.dibujar(ui, x + 8, ty, w - 16, altoCodigo);
        }
        ordenesDelTurno(j, c, x, ty + altoCodigo + 8, w, abajo);
    }

    private void ordenesDelTurno(Juego j, Cuerpo c, float x, float ty, float w, float abajo) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean hayConflicto = c != null && c.tieneConflictoDeOrdenes;
        float tamCab = ui.cabe(T.t("pelea.depurador_ordenes"), w - 32, ui.letra * 0.85f, true);
        p.texto(T.t("pelea.depurador_ordenes"), x + 16, ty, tamCab, true, hayConflicto ? ui.tema.aviso : ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 4;
        float bh = Math.max(70, abajo - ty);
        p.caja(x + 8, ty, w - 16, bh, 6, ui.tema.fondo, 1);
        float d = ui.desplazable(desplOrdenes, x + 8, ty + 4, w - 16, bh - 8);
        float inicioY = ty + 6 - d, yy = inicioY, tw = w - 36;
        float tamOrden = Math.min(18, ui.letra * 0.76f);
        float tamAviso = Math.min(17, ui.letra * 0.72f);
        List<OrdenDelTurno> ordenes = c == null ? List.of() : c.ordenesDelTurno;
        if (ordenes.isEmpty()) {
            yy += p.parrafo(T.t("pelea.depurador_sin_ordenes"), x + 18, yy, tw, tamOrden, ui.tema.textoApagado, 1);
        } else {
            for (OrdenDelTurno o : ordenes) {
                String texto = (o.metodo.isEmpty() ? "" : o.metodo + " -> ") + o.accion + (o.linea > 0 ? "  (L" + o.linea + ")" : "");
                if (o.pisada()) {
                    String aviso = "⚠ " + (o.lineaPisada > 0 ? T.f("pelea.depurador_pisado_linea", o.pisadaPor, o.lineaPisada)
                            : T.f("pelea.depurador_pisado", o.pisadaPor));
                    float hOrd = p.altoDeParrafo(texto, tw - 6, tamOrden, true);
                    float hAvi = p.altoDeParrafo(aviso, tw - 6, tamAviso, true);
                    float hCaja = hOrd + hAvi + 8;
                    p.rect(x + 12, yy - 2, w - 24, hCaja, ui.tema.aviso, 0.16f);
                    p.rect(x + 12, yy - 2, 3, hCaja, ui.tema.aviso, 1f);
                    p.parrafo(texto, x + 20, yy + 1, tw - 6, tamOrden, true, ui.tema.texto, 1);
                    p.parrafo(aviso, x + 20, yy + 3 + hOrd, tw - 6, tamAviso, true, ui.tema.aviso, 1);
                    yy += hCaja + 4;
                } else {
                    yy += p.parrafo(texto, x + 18, yy, tw, tamOrden, ui.tema.texto, 1) + 4;
                }
            }
        }
        ui.finDesplazable(desplOrdenes, Math.max(1, yy - inicioY) + 8);
    }

    /** Los campos del robot del jugador y lo que valen ahora (con reflexión: es el juego quien mira, no su código). */
    static List<String[]> variables(RobotBase r) {
        List<String[]> l = new ArrayList<>();
        if (r == null) return l;
        Class<?> c = r.getClass();
        while (c != null && !c.getName().startsWith("metajavbots.api.")) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(r);
                    String s = v == null ? "null" : v.getClass().isArray() ? java.util.Arrays.deepToString(new Object[]{v}) : String.valueOf(v);
                    if (v instanceof Double || v instanceof Float) s = String.format(java.util.Locale.ROOT, "%.2f", ((Number) v).doubleValue());
                    l.add(new String[]{f.getType().getSimpleName() + " " + f.getName(), s.length() > 200 ? s.substring(0, 200) + "…" : s});
                } catch (RuntimeException | IllegalAccessException e) {
                    l.add(new String[]{f.getName(), "?"});
                }
            }
            c = c.getSuperclass();
        }
        return l;
    }
}
