package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.Juego;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/**
 * SELECTOR DE MINIJUEGO — Lógica y caché compartidas de selección de robot,
 * modelo y piezas para los modos especiales ({@link PantallaCarreraDeObstaculos}
 * y {@link PantallaMinijuegoDestruccion}), evitando recalcular y asignar listas
 * del catálogo en cada fotograma.
 */
final class SelectorDeMinijuego {

    private Jugador jugadorCache;
    private Etiqueta etiquetaCache;
    private int desbloqueadosCache = -1;
    private String robotModeloCache;
    private int robotPiezasHash = -1;
    private List<Modelo> modelosCache;
    private Montaje montajeCache;
    private final Map<Montaje.Hueco, List<Pieza>> piezasCache = new EnumMap<>(Montaje.Hueco.class);

    void invalidar() {
        modelosCache = null;
        montajeCache = null;
        piezasCache.clear();
        robotModeloCache = null;
        robotPiezasHash = -1;
    }

    private void sincronizar(Jugador jug, Jugador.Robot r) {
        int desb = jug == null ? -1 : jug.piezasGanadas.size() * 31 + jug.modelosGanados.size()
                + jug.piezasPropiasCodigo.size() * 997 + jug.ajustesDePiezas.size() * 17;
        Etiqueta et = jug == null ? null : jug.etiqueta;
        String mod = r == null ? null : r.modelo;
        int pHash = r == null ? -1 : r.piezas.hashCode();
        if (jug != jugadorCache || et != etiquetaCache || desb != desbloqueadosCache) {
            jugadorCache = jug;
            etiquetaCache = et;
            desbloqueadosCache = desb;
            modelosCache = null;
            piezasCache.clear();
            montajeCache = null;
        }
        if (mod == null ? robotModeloCache != null : !mod.equals(robotModeloCache) || pHash != robotPiezasHash) {
            robotModeloCache = mod;
            robotPiezasHash = pHash;
            montajeCache = null;
            piezasCache.clear();
        }
    }

    List<Modelo> modelos(Jugador jug) {
        sincronizar(jug, null);
        if (modelosCache == null) modelosCache = jug == null ? List.of() : Progreso.modelosDisponibles(jug);
        return modelosCache;
    }

    Montaje montaje(Jugador jug, Jugador.Robot r) {
        sincronizar(jug, r);
        if (montajeCache == null) {
            montajeCache = r != null && jug != null ? jug.montajeDe(r) : new Montaje(Modelo.de("cobalto"));
        }
        return montajeCache;
    }

    List<Pieza> piezasParaHueco(Jugador jug, Jugador.Robot r, Montaje.Hueco hueco) {
        sincronizar(jug, r);
        return piezasCache.computeIfAbsent(hueco, h -> piezasDisponiblesParaHueco(jug, r, h));
    }

    static List<Pieza> piezasDisponiblesParaHueco(Jugador jugador, Jugador.Robot robot, Montaje.Hueco hueco) {
        List<Pieza> opciones = new ArrayList<>();
        if (jugador == null || robot == null || hueco == null) return opciones;
        Modelo modelo = Modelo.de(robot.modelo);
        for (Pieza pz : CatalogoDePiezas.delJuego().de(hueco.ranura)) {
            if (Progreso.piezaUsable(jugador, pz, modelo)) {
                opciones.add(metajavbots.ejercicios.RetoDePieza.aplicarAjustes(pz, jugador.ajustesDePiezas.get(pz.id)));
            }
        }
        return opciones;
    }

    static boolean seleccionarModelo(Jugador jugador, Jugador.Robot r, String modeloId) {
        if (r == null || modeloId == null || modeloId.isBlank()) return false;
        for (Modelo m : Progreso.modelosDisponibles(jugador)) {
            if (m.id.equals(modeloId)) {
                r.cambiarModelo(m, jugador.idioma);
                return true;
            }
        }
        return false;
    }

    static boolean cambiarModelo(Jugador jugador, Jugador.Robot r, int paso) {
        if (r == null) return false;
        List<Modelo> opciones = Progreso.modelosDisponibles(jugador);
        if (opciones.size() <= 1) return false;
        int idx = indiceDeModelo(opciones, r.modelo);
        int siguiente = Math.floorMod((idx < 0 ? 0 : idx) + paso, opciones.size());
        r.modelo = opciones.get(siguiente).id;
        r.piezas.clear();
        return true;
    }

    static boolean cambiarPieza(Jugador jugador, Jugador.Robot r, Montaje.Hueco hueco, int paso) {
        if (r == null || hueco == null) return false;
        List<Pieza> opciones = piezasDisponiblesParaHueco(jugador, r, hueco);
        if (opciones.size() <= 1) return false;
        Montaje m = jugador.montajeDe(r);
        int idx = indiceDePieza(opciones, m.pieza(hueco));
        int siguiente = Math.floorMod((idx < 0 ? 0 : idx) + paso, opciones.size());
        m.poner(hueco, opciones.get(siguiente));
        r.ponerPiezas(m);
        return true;
    }

    static boolean equiparPieza(Jugador jugador, Jugador.Robot r, Montaje.Hueco hueco, String piezaId) {
        if (r == null || hueco == null || piezaId == null) return false;
        for (Pieza opcion : piezasDisponiblesParaHueco(jugador, r, hueco)) {
            if (opcion.id.equals(piezaId)) {
                Montaje m = jugador.montajeDe(r);
                m.poner(hueco, opcion);
                r.ponerPiezas(m);
                return true;
            }
        }
        return false;
    }

    void dibujarSelectorPiezas(Juego j, Jugador.Robot r, float x, float y, float w) {
        if (r == null) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Montaje m = montaje(j.jugador, r);
        Montaje.Hueco[] huecos = PantallaCarreraDeObstaculos.HUECOS_EDITABLES;
        float sep = 8;
        float cw = (w - sep * (huecos.length - 1)) / huecos.length;
        float ch = 46;
        float tHueco = Math.min(ui.letra * 0.62f, 13.5f);
        float tPieza = Math.min(ui.letra * 0.76f, 16.5f);
        for (int i = 0; i < huecos.length; i++) {
            Montaje.Hueco hueco = huecos[i];
            float cx = x + i * (cw + sep);
            p.caja(cx, y, cw, ch, 8, ui.tema.panelAlto, 0.92f);
            p.bordeRedondeado(cx, y, cw, ch, 8, ui.tema.borde, 0.7f, 1f);
            List<Pieza> opciones = piezasParaHueco(j.jugador, r, hueco);
            Pieza actual = m.pieza(hueco);
            boolean puede = opciones.size() > 1;
            if (ui.boton("◄", cx + 4, y + 6, 32, 34, Ui.Estilo.PLANO, puede, null)) {
                if (cambiarPieza(j.jugador, r, hueco, -1)) {
                    invalidar();
                    j.guardar();
                    j.sonidos.tocar("servo", 0.4f);
                }
            }
            if (ui.boton("►", cx + cw - 36, y + 6, 32, 34, Ui.Estilo.PLANO, puede, null)) {
                if (cambiarPieza(j.jugador, r, hueco, 1)) {
                    invalidar();
                    j.guardar();
                    j.sonidos.tocar("servo", 0.4f);
                }
            }
            float tx = cx + 40;
            float tw = Math.max(40, cw - 80);
            String etHueco = T.t("hueco." + hueco.name().toLowerCase(Locale.ROOT));
            p.textoConBordeNegro(ui.recortarTexto(etHueco, tw, tHueco, false), tx, y + 4, tHueco, false, ui.tema.textoSuave, 1);
            String nomPieza = actual == null ? T.t("taller.sin_pieza") : actual.nombre(T.idioma);
            p.textoConBordeNegro(ui.recortarTexto(nomPieza, tw, tPieza, true), tx, y + 22, tPieza, true, ui.tema.texto, 1);
        }
    }

    private static int indiceDeModelo(List<Modelo> opciones, String modeloId) {
        if (modeloId == null) return -1;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).id.equals(modeloId)) return i;
        }
        return -1;
    }

    private static int indiceDePieza(List<Pieza> opciones, Pieza actual) {
        if (actual == null) return -1;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).id.equals(actual.id)) return i;
        }
        return -1;
    }
}
