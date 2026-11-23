package metajavbots.pantallas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.api.RobotBase;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.ModificadoresPoo;
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
public final class PanelDepurador {

    /** Longitud máxima visible de un valor de variable antes de truncarlo explícitamente con «…». */
    public static final int MAX_LARGO_VALOR = 200;
    private static final float SEPARACION_COL_VAR = 10f;
    private static final float SANGRIA_VALOR_APILADO = 10f;

    /** Disposición calculada para una fila de variable (dos columnas o apilada). */
    public record DisposicionVariable(boolean apilada, float anchoNombre, float xValor, float anchoValor) {
    }

    private final Ui.Desplazable desplConsola = new Ui.Desplazable();
    private final Ui.Desplazable desplVariables = new Ui.Desplazable();
    private final Ui.Desplazable desplOrdenes = new Ui.Desplazable();
    private final VentanaDeCodigo visorCodigo = new VentanaDeCodigo();
    private String fuenteCargada = "";
    private int cantidadConsola;
    private String ultimaLineaConsola;
    private int pestana;

    PanelDepurador() {
        visorCodigo.soloLectura = true;
        visorCodigo.seguirActiva = true;
    }

    /** Selecciona la pestaña activa (0: variables y consola; 1: código y órdenes del turno). */
    void pestana(int p) {
        this.pestana = p >= 1 ? 1 : 0;
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
            long tope = c.topeDePasos();
            float parte = Math.min(1, c.pasosDelTurno / (float) tope);
            ui.barra(x + 16, ty, w - 32, 18, parte, parte > 0.8f ? ui.tema.mal : ui.tema.acento, T.f("pelea.pasos", c.pasosDelTurno, tope));
            ty += 24;
            ty = barrasSenior(j, pelea, c, x, ty, w);
            String pos = String.format(java.util.Locale.ROOT, "x=%.0f  y=%.0f  rumbo=%.0f°  mira=%.0f°", c.x, c.y, c.rumbo(), c.mira());
            float tamPos = ui.cabeDeCodigo(pos, w - 32, ui.letra * 0.78f, false);
            p.textoDeCodigo(pos, x + 16, ty, tamPos, false, ui.tema.textoSuave, 1);
            ty += p.altoDeLinea(tamPos) + 2;
            String est = String.format(java.util.Locale.ROOT, "%s=%.0f  %s=%.0f  %s=%d", T.t("pelea.blindaje"), c.blindaje(),
                    T.t("pelea.energia"), c.energia, T.t("pelea.turno"), c.turnoPensado);
            float tamEst = ui.cabeDeCodigo(est, w - 32, ui.letra * 0.78f, false);
            p.textoDeCodigo(est, x + 16, ty, tamEst, false, ui.tema.textoSuave, 1);
            ty += p.altoDeLinea(tamEst) + 6;
            ty = indicadorSinEjecucion(j, c, x, ty, w);
        }
        String tabCodigo = T.t("pelea.depurador_codigo") + (c != null && c.tieneConflictoDeOrdenes ? " ⚠" : "");
        String[] pestanas = {T.t("pelea.depurador_vars"), tabCodigo};
        pestana = ui.pestanas(pestanas, pestana, x + 16, ty, w - 32, 34);
        ty += 42;
        if (pestana == 1) codigoYTurno(j, c, fuente, conTraza, x, ty, w, y + h - 14);
        else variablesYConsola(j, yo, x, ty, w, y + h - 18);
    }

    private float barrasSenior(Juego j, Pelea pelea, Cuerpo c, float x, float ty, float w) {
        Ui ui = j.ui;
        Pincel p = j.pincel;
        if (pelea.presupuestoNanosPorTurno > 0) {
            float f = Math.min(1f, c.nanosDelTurno / (float) pelea.presupuestoNanosPorTurno);
            String txt = T.f("pelea.tiempo_turno", String.format(java.util.Locale.ROOT, "%.2f", c.nanosDelTurno / 1_000_000.0),
                    String.format(java.util.Locale.ROOT, "%.2f", pelea.presupuestoNanosPorTurno / 1_000_000.0));
            ui.barra(x + 16, ty, w - 32, 18, f, f >= 1f ? ui.tema.mal : ui.tema.bien, txt);
            ty += 22;
        }
        if (pelea.presupuestoBytesPorTurno > 0) {
            float f = Math.min(1f, c.bytesDelTurno / (float) pelea.presupuestoBytesPorTurno);
            String txt = T.f("pelea.memoria_turno", String.format(java.util.Locale.ROOT, "%.1f", c.bytesDelTurno / 1024.0),
                    String.format(java.util.Locale.ROOT, "%.1f", pelea.presupuestoBytesPorTurno / 1024.0));
            ui.barra(x + 16, ty, w - 32, 18, f, f >= 1f ? ui.tema.mal : 0x3FE0FF, txt);
            ty += 22;
        }
        if (pelea.pensarEnParalelo) {
            String par = "|| " + T.t("pelea.paralelo");
            float tam = ui.cabeDeCodigo(par, w - 32, ui.letra * 0.75f, true);
            p.textoDeCodigo(par, x + 16, ty, tam, true, 0x3FE0FF, 1);
            ty += p.altoDeLinea(tam) + 4;
        }
        return ty;
    }

    private float indicadorSinEjecucion(Juego j, Cuerpo c, float x, float ty, float w) {
        if (!sinEjecucion(c)) return ty;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String icono = c.estadoPensamiento == Cuerpo.EstadoPensamiento.DEUDA_TOPE ? "⏱ " : "⚠ ";
        String titulo = icono + tituloSinEjecucion(c);
        String motivo = motivoSinEjecucion(c);
        int color = c.estadoPensamiento == Cuerpo.EstadoPensamiento.FUERA_DE_COMBATE ? ui.tema.mal : ui.tema.aviso;
        float tamTitulo = Math.min(18, ui.letra * 0.76f);
        float tamMotivo = Math.min(17, ui.letra * 0.72f);
        float tw = Math.max(80f, w - 40f);
        float hTit = p.altoDeParrafo(titulo, tw, tamTitulo, true);
        float hMot = motivo.isEmpty() ? 0f : p.altoDeParrafo(motivo, tw, tamMotivo, false);
        float hCaja = hTit + (hMot > 0f ? hMot + 2f : 0f) + 10f;
        p.rect(x + 12, ty, w - 24, hCaja, color, 0.16f);
        p.rect(x + 12, ty, 3, hCaja, color, 1f);
        p.parrafo(titulo, x + 20, ty + 4, tw, tamTitulo, true, color, 1);
        if (!motivo.isEmpty()) {
            p.parrafo(motivo, x + 20, ty + 6 + hTit, tw, tamMotivo, false, ui.tema.texto, 1);
        }
        return ty + hCaja + 6f;
    }

    /** Indica si el cuerpo del robot no ejecutó su código en el turno actual. */
    public static boolean sinEjecucion(Cuerpo c) {
        return c != null && c.sinEjecucion();
    }

    /** Encabezado corto del indicador de turno sin ejecución (incluye turnos restantes si aplica). */
    public static String tituloSinEjecucion(Cuerpo c) {
        if (!sinEjecucion(c)) return "";
        int restantes = c.turnosSinEjecucionRestantes();
        return restantes > 0 ? T.f("pelea.depurador_sin_ejecucion_turnos", restantes) : T.t("pelea.depurador_sin_ejecucion");
    }

    /** Motivo legible por el que el robot no ejecutó su código en el turno actual. */
    public static String motivoSinEjecucion(Cuerpo c) {
        return !sinEjecucion(c) ? "" : mensajeEstado(c.estadoPensamiento);
    }

    private static String mensajeEstado(Cuerpo.EstadoPensamiento estado) {
        if (estado == null) return "";
        switch (estado) {
            case ATURDIDO:
                return T.t("pelea.estado_aturdido");
            case DEUDA_TOPE:
                return T.t("pelea.estado_deuda_tope");
            case FUERA_DE_COMBATE:
                return T.t("pelea.estado_fuera");
            case SIN_CODIGO:
                return T.t("pelea.estado_sin_codigo");
            default:
                return "";
        }
    }

    private void variablesYConsola(Juego j, Combatiente yo, float x, float ty, float w, float abajo) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        ty = resumenPoo(j, yo, x, ty, w);
        float tamCab = ui.cabe(T.t("pelea.variables"), w - 32, ui.letra * 0.88f, true);
        p.texto(T.t("pelea.variables"), x + 16, ty, tamCab, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 4;
        List<String[]> vars = variables(yo.robot());
        float restante = Math.max(120, abajo - ty);
        float zona = restante * 0.45f;
        float tamVar = Math.min(20, ui.letra * 0.78f);
        float altoLinea = p.altoDeLinea(tamVar);
        float anchoInterior = anchoTextoVariables(w - 16f);
        float d = ui.desplazable(desplVariables, x + 8, ty, w - 16, zona);
        float inicioY = ty - d, yy = inicioY;
        for (int i = 0; i < vars.size(); i++) {
            String[] v = vars.get(i);
            String nombre = normalizarTexto(v[0]);
            String valor = normalizarTexto(v[1]);
            DisposicionVariable disp = disposicionVariable(anchoInterior, p.anchoDeCodigo(nombre, tamVar, false));
            float altoFila;
            if (disp.apilada()) {
                float hNom = Math.max(altoLinea, p.altoDeParrafoDeCodigo(nombre, disp.anchoNombre(), tamVar, false));
                float hVal = valor.isEmpty() ? altoLinea : p.altoDeParrafoDeCodigo(valor, disp.anchoValor(), tamVar, false);
                altoFila = hNom + hVal + 4f;
                if (yy + altoFila >= ty && yy <= ty + zona) {
                    p.parrafoDeCodigo(nombre, x + 16, yy, disp.anchoNombre(), tamVar, false, ui.tema.tipo, 1);
                    if (!valor.isEmpty()) {
                        p.parrafoDeCodigo(valor, x + 16 + disp.xValor(), yy + hNom, disp.anchoValor(), tamVar, false, ui.tema.texto, 1);
                    }
                }
            } else {
                float hVal = valor.isEmpty() ? altoLinea : p.altoDeParrafoDeCodigo(valor, disp.anchoValor(), tamVar, false);
                altoFila = Math.max(altoLinea, hVal) + 4f;
                if (yy + altoFila >= ty && yy <= ty + zona) {
                    p.textoDeCodigo(nombre, x + 16, yy, tamVar, false, ui.tema.tipo, 1);
                    if (!valor.isEmpty()) {
                        p.parrafoDeCodigo(valor, x + 16 + disp.xValor(), yy, disp.anchoValor(), tamVar, false, ui.tema.texto, 1);
                    }
                }
            }
            if (i + 1 < vars.size() && yy + altoFila >= ty && yy + altoFila <= ty + zona) {
                p.rect(x + 16, yy + altoFila - 2f, anchoInterior, 1f, ui.tema.borde, 0.25f);
            }
            yy += altoFila;
        }
        if (vars.isEmpty()) {
            yy += p.parrafo(T.t("pelea.sin_variables"), x + 16, yy, anchoInterior, tamVar, ui.tema.textoApagado, 1);
        }
        ui.finDesplazable(desplVariables, Math.max(altoLinea, yy - inicioY) + 8f);
        ty += zona + 8;
        p.texto(T.t("pelea.consola"), x + 16, ty, tamCab, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 4;
        consola(j, yo, x + 8, ty, w - 16, Math.max(60, abajo - ty));
    }

    private float resumenPoo(Juego j, Combatiente yo, float x, float ty, float w) {
        List<ModificadoresPoo.Entrada> entradas = yo.efectosPoo == null ? List.of() : yo.efectosPoo.resumen();
        if (entradas.isEmpty()) return ty;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float tamCab = ui.cabe(T.t("poo.titulo"), w - 32, ui.letra * 0.84f, true);
        p.texto(T.t("poo.titulo"), x + 16, ty, tamCab, true, ui.tema.texto, 1);
        ty += p.altoDeLinea(tamCab) + 2;
        float tam = Math.min(17, ui.letra * 0.72f);
        for (ModificadoresPoo.Entrada e : entradas) {
            int col = e.nivel == ModificadoresPoo.Nivel.PENALIZACION ? ui.tema.mal
                    : e.nivel == ModificadoresPoo.Nivel.BONO ? ui.tema.bien : ui.tema.aviso;
            ty += p.parrafo("• " + e.texto, x + 16, ty, w - 32, tam, col, 1) + 2;
        }
        return ty + 6;
    }

    private void consola(Juego j, Combatiente yo, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 6, ui.tema.fondo, 1);
        float tam = Math.min(18, ui.letra * 0.74f);
        float altoLinea = p.altoDeLinea(tam);
        float espacio = 2f;
        float anchoTexto = anchoTextoConsola(w);
        float altoTotal = 8f;
        for (String l : yo.consola) {
            String texto = normalizarTexto(l);
            float hl = texto.isEmpty() ? altoLinea : p.altoDeParrafoDeCodigo(texto, anchoTexto, tam, false);
            altoTotal += hl + espacio;
        }
        float altoVisible = Math.max(1f, h - 8f);
        float maximoAntes = Math.max(0f, desplConsola.contenido - altoVisible);
        boolean estabaAbajo = desplConsola.desplazamiento >= maximoAntes - altoLinea;
        String ultima = yo.consola.isEmpty() ? null : yo.consola.get(yo.consola.size() - 1);
        boolean hayNuevas = yo.consola.size() != cantidadConsola || (ultima != null && !ultima.equals(ultimaLineaConsola));
        desplConsola.contenido = altoTotal;
        if (hayNuevas && (estabaAbajo || cantidadConsola == 0)) {
            desplConsola.desplazamiento = Math.max(0f, altoTotal - altoVisible);
        }
        cantidadConsola = yo.consola.size();
        ultimaLineaConsola = ultima;
        float d = ui.desplazable(desplConsola, x, y + 4, w, altoVisible);
        float yy = y + 4 - d;
        for (String l : yo.consola) {
            String texto = normalizarTexto(l);
            int color = texto.contains("⚠") ? ui.tema.mal : texto.contains("⏱") ? ui.tema.aviso : ui.tema.texto;
            float hl = texto.isEmpty() ? altoLinea : p.altoDeParrafoDeCodigo(texto, anchoTexto, tam, false);
            if (yy + hl >= y && yy <= y + h && !texto.isEmpty()) {
                p.parrafoDeCodigo(texto, x + 8, yy, anchoTexto, tam, false, color, 1);
            }
            yy += hl + espacio;
        }
        ui.finDesplazable(desplConsola, altoTotal);
    }

    private void codigoYTurno(Juego j, Cuerpo c, String fuente, boolean conTraza, float x, float ty, float w, float abajo) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String sufijo = sinEjecucion(c) ? " · " + T.t("pelea.depurador_sin_ejecucion")
                : (c != null && c.ultimaLinea > 0 ? " · " + T.t("mcm.linea") + " " + c.ultimaLinea : "");
        String cab = T.t("pelea.depurador_traza") + sufijo;
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
        // Paso a paso: cómo queda cada mando tras este turno (lo que le falta y por qué el disparo sale hacia donde sale).
        List<String> mandos = mandos(c);
        if (!mandos.isEmpty()) {
            yy += p.parrafo(T.t("pelea.mandos"), x + 18, yy, tw, tamOrden, true, ui.tema.acento, 1) + 2;
            for (String m : mandos) yy += p.parrafo("· " + m, x + 22, yy, tw - 4, tamAviso, ui.tema.texto, 1) + 2;
            yy += 8;
        }
        if (ordenes.isEmpty()) {
            String vacio = sinEjecucion(c) ? motivoSinEjecucion(c) : T.t("pelea.depurador_sin_ordenes");
            int colorVacio = sinEjecucion(c) ? ui.tema.aviso : ui.tema.textoApagado;
            yy += p.parrafo(vacio, x + 18, yy, tw, tamOrden, colorVacio, 1);
        } else {
            for (OrdenDelTurno o : ordenes) {
                String texto = (o.metodo.isEmpty() ? "" : o.metodo + " -> ") + o.accion + (o.linea > 0 ? "  (L" + o.linea + ")" : "");
                if (o.pisada()) {
                    String aviso = "⚠ " + (o.lineaPisada > 0 ? T.f("pelea.depurador_pisado_linea", o.pisadaPor, o.lineaPisada)
                            : T.f("pelea.depurador_pisado", o.pisadaPor)) + "\n" + AvisoDeOrdenes.textoPisada(o);
                    float hOrd = p.altoDeParrafoDeCodigo(texto, tw - 6, tamOrden, true);
                    float hAvi = p.altoDeParrafo(aviso, tw - 6, tamAviso, true);
                    float hCaja = hOrd + hAvi + 8;
                    p.rect(x + 12, yy - 2, w - 24, hCaja, ui.tema.aviso, 0.16f);
                    p.rect(x + 12, yy - 2, 3, hCaja, ui.tema.aviso, 1f);
                    p.parrafoDeCodigo(texto, x + 20, yy + 1, tw - 6, tamOrden, true, ui.tema.texto, 1);
                    p.parrafo(aviso, x + 20, yy + 3 + hOrd, tw - 6, tamAviso, true, ui.tema.aviso, 1);
                    yy += hCaja + 4;
                } else {
                    yy += p.parrafoDeCodigo(texto, x + 18, yy, tw, tamOrden, false, ui.tema.texto, 1) + 4;
                }
            }
            if (c.ataqueLejos != null) {
                // El disparo de este turno salió con la mira lejos del enemigo: se cuenta en llano, como las órdenes pisadas.
                String lejos = "⚠ " + AvisoDeOrdenes.textoLejos(c.ataqueLejos);
                float hL = p.altoDeParrafo(lejos, tw - 6, tamAviso, true) + 8;
                p.rect(x + 12, yy - 2, w - 24, hL, ui.tema.aviso, 0.16f);
                p.rect(x + 12, yy - 2, 3, hL, ui.tema.aviso, 1f);
                p.parrafo(lejos, x + 20, yy + 2, tw - 6, tamAviso, true, ui.tema.aviso, 1);
                yy += hL + 4;
            }
        }
        ui.finDesplazable(desplOrdenes, Math.max(1, yy - inicioY) + 8);
    }

    /**
     * Cómo quedan los tres mandos del robot tras el último turno, en llano: lo que les falta a las ruedas, al
     * cuerpo y a la mira (y en cuántos turnos llega), y, si atacó, hacia dónde sale el disparo. Vacío sin robot.
     */
    public static List<String> mandos(Cuerpo c) {
        List<String> l = new ArrayList<>();
        if (c == null || c.fuera) return l;
        double av = c.restanteAvance(), gi = c.restanteGiro(), mi = c.restanteMira();
        l.add(Math.abs(av) >= 1 ? T.f("pelea.mando_ruedas", Math.round(Math.abs(av))) : T.t("pelea.mando_ruedas_quietas"));
        l.add(Math.abs(gi) >= 1 ? T.f("pelea.mando_cuerpo", Math.round(Math.abs(gi))) : T.t("pelea.mando_cuerpo_quieto"));
        double porTurno = Math.max(1, c.cifras.giroMira);
        if (Math.abs(mi) >= 1) {
            l.add(T.f("pelea.mando_cabeza", Math.round(Math.abs(mi)), (int) Math.ceil(Math.abs(mi) / porTurno)));
        } else {
            l.add(T.f("pelea.mando_cabeza_llego", Math.round(c.mira())));
        }
        for (OrdenDelTurno o : c.ordenesDelTurno) {
            if (o.accion.startsWith("atacar") || o.accion.startsWith("angriff") || o.accion.startsWith("attack")) {
                l.add(T.f("pelea.mando_disparo", Math.round(c.mira())));
                break;
            }
        }
        return l;
    }

    /** Ancho útil para el texto de la consola descontando márgenes y la barra de desplazamiento. */
    public static float anchoTextoConsola(float anchoCaja) {
        return Math.max(60f, anchoCaja - 28f);
    }

    /** Ancho útil para la lista de variables descontando márgenes y la barra de desplazamiento. */
    public static float anchoTextoVariables(float anchoZona) {
        return Math.max(80f, anchoZona - 28f);
    }

    /** Calcula si una variable se dibuja en dos columnas o apilada para evitar recortes opacos y solapamientos. */
    public static DisposicionVariable disposicionVariable(float anchoInterior, float anchoTextoNombre) {
        float anchoSeguro = Math.max(80f, anchoInterior);
        float colNombre = Math.min(180f, anchoSeguro * 0.42f);
        float colValor = anchoSeguro - colNombre - SEPARACION_COL_VAR;
        if (anchoTextoNombre <= colNombre && colValor >= 110f) {
            return new DisposicionVariable(false, colNombre, colNombre + SEPARACION_COL_VAR, colValor);
        }
        float sangria = Math.min(SANGRIA_VALOR_APILADO, anchoSeguro * 0.12f);
        return new DisposicionVariable(true, anchoSeguro, sangria, Math.max(40f, anchoSeguro - sangria));
    }

    /** Normaliza saltos de línea y tabuladores antes de medir o envolver texto en el depurador. */
    public static String normalizarTexto(String texto) {
        if (texto == null) return "";
        return texto.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ");
    }

    /** Formatea un valor de variable de forma legible, con truncado explícito «…» si supera el límite. */
    public static String formatearValor(Object v) {
        String s;
        if (v == null) s = "null";
        else if (v.getClass().isArray()) s = java.util.Arrays.deepToString(new Object[]{v});
        else if (v instanceof Double || v instanceof Float) s = String.format(java.util.Locale.ROOT, "%.2f", ((Number) v).doubleValue());
        else s = String.valueOf(v);
        s = normalizarTexto(s);
        return s.length() > MAX_LARGO_VALOR ? s.substring(0, MAX_LARGO_VALOR) + "…" : s;
    }

    /** Los campos del robot del jugador y lo que valen ahora (con reflexión: es el juego quien mira, no su código). */
    public static List<String[]> variables(RobotBase r) {
        List<String[]> l = new ArrayList<>();
        if (r == null) return l;
        Class<?> c = r.getClass();
        while (c != null && !c.getName().startsWith("metajavbots.api.")) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    l.add(new String[]{f.getType().getSimpleName() + " " + f.getName(), formatearValor(f.get(r))});
                } catch (RuntimeException | IllegalAccessException e) {
                    l.add(new String[]{f.getName(), "?"});
                }
            }
            c = c.getSuperclass();
        }
        return l;
    }
}
