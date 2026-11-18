package metajavbots.pantallas;

import java.nio.file.Path;
import java.util.List;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.cerebro.CerebroLocal;
import metajavbots.cerebro.Claves;
import metajavbots.cerebro.Ranura;
import metajavbots.cerebro.Tarea;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * PANEL DE CEREBROS — La parte de los Ajustes con los tres cerebros de IA:
 * una pestaña por cerebro (vacío, local o en línea, con su modelo, su clave
 * o su dirección) y el reparto de las tareas (qué cerebro hace lo rápido, lo
 * pesado y los tutores). Abajo, la memoria que gastan los modelos locales.
 */
final class PanelDeCerebros {

    private static final int RANURAS = 3;
    private static final String[] PROVEEDORES = {"anthropic", "openai", "otro"};

    private final CampoDeTexto[] clave = campos(true), modelo = campos(false), direccion = campos(false);
    private final CampoDeTexto carpeta = new CampoDeTexto();
    private final Ui.Desplazable despl = new Ui.Desplazable();
    private int elegida;
    private volatile String estado = "";

    private static CampoDeTexto[] campos(boolean ocultos) {
        CampoDeTexto[] c = new CampoDeTexto[RANURAS];
        for (int i = 0; i < RANURAS; i++) {
            c[i] = new CampoDeTexto();
            c[i].oculto = ocultos;
        }
        return c;
    }

    /** Pone en los campos lo que tiene el jugador. */
    void cargar(Jugador jug) {
        jug.migrarCerebroAntiguo();
        for (int i = 0; i < RANURAS; i++) {
            Ranura r = jug.cerebros[i];
            clave[i].ponerTexto(Claves.leer(r.proveedor));
            modelo[i].ponerTexto(r.modeloEnLinea);
            direccion[i].ponerTexto(r.direccionEnLinea);
        }
        carpeta.ponerTexto(CerebroLocal.carpeta(jug.carpetaModelos).toString());
    }

    /** Pasa al jugador lo escrito en los campos. */
    void guardar(Jugador jug) {
        for (int i = 0; i < RANURAS; i++) {
            jug.cerebros[i].modeloEnLinea = modelo[i].texto().strip();
            jug.cerebros[i].direccionEnLinea = direccion[i].texto().strip();
        }
        String c = carpeta.texto().strip();
        jug.carpetaModelos = c.equals(CerebroLocal.carpeta(null).toString()) ? "" : c;
    }

    void dibujar(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        float cy = ui.panel(x, y, w, h, T.t("ajustes.cerebro"));
        float zona = y + h - cy - 8;
        float d = ui.desplazable(despl, x + 4, cy, w - 8, zona);
        float inicio = cy;
        cy -= d;
        float ix = x + 20, iw = w - 52;
        float t = ui.letra * 0.85f;
        cy += p.parrafo(T.t("cerebros.ayuda"), ix, cy, iw, t, ui.tema.textoSuave, 1) + 14;
        if (ui.boton(T.t("guia.abrir"), ix, cy, iw, 46, Ui.Estilo.PLANO, true, T.t("guia.abrir_ayuda"))) j.ir(new GuiaDeCerebros());
        cy += 56;
        if (ui.boton(T.t("cerebros.recomendado"), ix, cy, iw, 46, Ui.Estilo.NORMAL, true, T.t("cerebros.recomendado_ayuda"))) {
            recomendado(jug);
        }
        cy += 60;
        String[] nombres = new String[RANURAS];
        for (int i = 0; i < RANURAS; i++) nombres[i] = T.f("cerebros.pestana", i + 1, T.t("cerebro." + jug.cerebros[i].tipo.name().toLowerCase()));
        elegida = ui.pestanas(nombres, elegida, ix, cy, iw, 48);
        cy += 62;
        cy = ranura(j, jug, elegida, ix, cy, iw);
        cy = reparto(j, jug, ix, cy + 10, iw);
        cy += memoria(p, ui, ix, cy, iw);
        ui.finDesplazable(despl, cy + d - inicio + 20);
    }

    /** Lo de un cerebro: qué es, su modelo o su clave, y probarlo. Devuelve dónde acaba. */
    private float ranura(Juego j, Jugador jug, int i, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Ranura r = jug.cerebros[i];
        float t = ui.letra * 0.85f;
        Ranura.Tipo[] tipos = Ranura.Tipo.values();
        float bw = w / tipos.length;
        for (int k = 0; k < tipos.length; k++) {
            String n = tipos[k].name().toLowerCase();
            if (ui.boton(T.t("cerebro." + n), x + k * bw, y, bw - 10, 48, r.tipo == tipos[k] ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true,
                    T.t("cerebro." + n + "_ayuda"))) {
                r.tipo = tipos[k];
            }
        }
        y += 62;
        if (r.tipo == Ranura.Tipo.LOCAL) y = local(j, jug, i, x, y, w);
        if (r.tipo == Ranura.Tipo.EN_LINEA) y = enLinea(j, r, i, x, y, w);
        if (!r.vacia()) {
            if (ui.boton(T.f("cerebros.probar", i + 1), x, y, Math.min(w, 320), 48, Ui.Estilo.PRINCIPAL)) probar(jug, i);
            y += 58;
        }
        if (!estado.isEmpty()) y += p.parrafo(estado, x, y, w, t, ui.tema.texto, 1) + 12;
        return y;
    }

    private float local(Juego j, Jugador jug, int i, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Ranura r = jug.cerebros[i];
        float t = ui.letra * 0.85f;
        p.texto(T.t("ajustes.carpeta_modelos"), x, y, t, false, ui.tema.textoSuave, 1);
        y += t * 1.5f;
        carpeta.dibujar(ui, x, y, w, 44, "");
        y += 56;
        String c = carpeta.texto().strip();
        jug.carpetaModelos = c.equals(CerebroLocal.carpeta(null).toString()) ? "" : c;
        Path dir = CerebroLocal.carpeta(jug.carpetaModelos);
        List<Path> ms = CerebroLocal.modelos(dir);
        String actual = CerebroDelJuego.modeloDe(jug, i);
        int k = -1;
        for (int n = 0; n < ms.size(); n++) if (ms.get(n).getFileName().toString().equals(actual)) k = n;
        if (ui.boton("◄", x, y, 50, 46, Ui.Estilo.PLANO, !ms.isEmpty(), null)) k = (k - 1 + ms.size()) % Math.max(1, ms.size());
        if (ui.boton("►", x + w - 50, y, 50, 46, Ui.Estilo.PLANO, !ms.isEmpty(), null)) k = (k + 1) % Math.max(1, ms.size());
        if (k >= 0 && k < ms.size() && !ms.get(k).getFileName().toString().equals(actual)) r.modeloLocal = ms.get(k).getFileName().toString();
        String nombre = ms.isEmpty() ? T.t("ajustes.sin_modelos")
                : r.modeloLocal.isEmpty() ? T.f("ajustes.modelo_auto", actual) : r.modeloLocal;
        p.textoCentrado(ui.recortarTexto(nombre, w - 130, t, false), x + w / 2, y + (46 - t * 1.2f) / 2, t, false, ui.tema.texto, 1);
        y += 56;
        if (k >= 0 && k < ms.size()) {
            String gb = String.format(java.util.Locale.ROOT, "%.1f", CerebroLocal.memoriaDe(ms.get(k)) / 1024.0);
            p.texto(T.f("cerebros.gasta", gb), x, y, t, false, ui.tema.textoSuave, 1);
            y += t * 1.6f;
        }
        boolean jarvis = r.direccionLocal.contains(":" + CerebroLocal.PUERTO_DE_JARVIS);
        if (ui.boton(T.t("ajustes.usar_jarvis"), x, y, w, 46, jarvis ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true, T.t("ajustes.usar_jarvis_ayuda"))) {
            r.direccionLocal = jarvis ? "" : CerebroLocal.direccion(CerebroLocal.PUERTO_DE_JARVIS);
        }
        return y + 58;
    }

    private float enLinea(Juego j, Ranura r, int i, float x, float y, float w) {
        Ui ui = j.ui;
        float bw = w / PROVEEDORES.length;
        for (int k = 0; k < PROVEEDORES.length; k++) {
            if (ui.boton(PROVEEDORES[k], x + k * bw, y, bw - 10, 46, r.proveedor.equals(PROVEEDORES[k]) ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                r.proveedor = PROVEEDORES[k];
                clave[i].ponerTexto(Claves.leer(r.proveedor));
            }
        }
        y += 58;
        clave[i].dibujar(ui, x, y, w - 170, 46, T.t("ajustes.clave"));
        if (ui.boton(T.t("ajustes.guardar_clave"), x + w - 160, y, 160, 46, Ui.Estilo.NORMAL, true, T.t("ajustes.clave_ayuda"))) {
            try {
                Claves.guardar(r.proveedor, clave[i].texto());
                estado = T.t("ajustes.clave_guardada");
            } catch (Exception e) {
                estado = e.toString();
            }
        }
        y += 58;
        String porDefecto = r.proveedor.equals("anthropic") ? "claude-haiku-5-5" : r.proveedor.equals("openai") ? "gpt-4o-mini" : "…";
        modelo[i].dibujar(ui, x, y, w, 46, T.t("ajustes.modelo") + ": " + porDefecto);
        r.modeloEnLinea = modelo[i].texto().strip();
        y += 58;
        if (r.proveedor.equals("otro")) {
            direccion[i].dibujar(ui, x, y, w, 46, T.t("ajustes.direccion") + ": https://…");
            r.direccionEnLinea = direccion[i].texto().strip();
            y += 58;
        }
        return y;
    }

    /** Qué cerebro hace cada tarea: una fila por tarea con los botones 1, 2 y 3. */
    private float reparto(Juego j, Jugador jug, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(x, y, w, 1, ui.tema.borde, 1);
        y += 14;
        p.texto(T.t("cerebros.reparto"), x, y, ui.letra, true, ui.tema.texto, 1);
        y += ui.letra * 1.8f;
        float bw = 64, botones = RANURAS * (bw + 8);
        for (Tarea tarea : Tarea.values()) {
            String n = tarea.name().toLowerCase();
            float t = ui.letra * 0.9f;
            float alto = Math.max(48, p.altoDeParrafo(T.t("tarea." + n), w - botones - 16, t));
            p.parrafo(T.t("tarea." + n), x, y + Math.max(0, (48 - t * 1.2f) / 2), w - botones - 16, t, ui.tema.texto, 1);
            if (ui.e.dentro(x, y, w - botones, alto)) ui.avisar(T.t("tarea." + n + "_ayuda"));
            for (int k = 0; k < RANURAS; k++) {
                boolean es = jug.ranuraDe(tarea) == k;
                String ayuda = jug.cerebros[k].vacia() ? T.t("cerebros.vacio_ayuda") : null;
                if (ui.boton(Integer.toString(k + 1), x + w - botones + k * (bw + 8), y, bw, 48, es ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true, ayuda)) {
                    jug.reparto[tarea.ordinal()] = k;
                }
            }
            y += alto + 10;
        }
        return y + 6;
    }

    /** La memoria de los modelos locales que arrancó el juego, y lo que pueden gastar. Devuelve el alto. */
    private float memoria(Pincel p, Ui ui, float x, float y, float w) {
        float t = ui.letra * 0.85f;
        String uso = String.format(java.util.Locale.ROOT, "%.1f", CerebroLocal.memoriaEnUsoMb() / 1024.0);
        String tope = String.format(java.util.Locale.ROOT, "%.1f", CerebroLocal.presupuestoMb() / 1024.0);
        return p.parrafo(T.f("cerebros.memoria", uso, tope, CerebroLocal.enMarchaDelJuego().size()), x, y, w, t, ui.tema.textoSuave, 1) + 8;
    }

    /** Lo recomendado en el portátil de Fran: Gemma 3 4B para lo rápido, Qwen3.5-9B para lo pesado y los tutores. */
    private void recomendado(Jugador jug) {
        for (int i = 0; i < 2; i++) {
            jug.cerebros[i].tipo = Ranura.Tipo.LOCAL;
            jug.cerebros[i].modeloLocal = "";
            jug.cerebros[i].direccionLocal = "";
        }
        for (Tarea t : Tarea.values()) jug.reparto[t.ordinal()] = t.ranuraPorDefecto();
        estado = T.t("cerebros.recomendado_hecho");
    }

    private void probar(Jugador jug, int i) {
        Cerebro c = CerebroDelJuego.deLaRanura(jug, i);
        if (c == null) {
            estado = T.t("ajustes.falta_clave");
            return;
        }
        estado = jug.cerebros[i].tipo == Ranura.Tipo.LOCAL ? T.t("ajustes.arrancando") : T.t("cerebro.pensando");
        Thread h = new Thread(() -> {
            long t0 = System.currentTimeMillis();
            try {
                String r = c.preguntar("Answer with one word.", "Say OK.", 4);
                long s = (System.currentTimeMillis() - t0 + 500) / 1000;
                estado = r == null || r.isBlank() ? T.t("ajustes.no_responde") : T.f("cerebros.responde", c.nombre(), s);
            } catch (Exception e) {
                estado = T.t("ajustes.no_responde") + " (" + e.getMessage() + ")";
            }
        }, "probar-cerebro");
        h.setDaemon(true);
        h.start();
    }
}
