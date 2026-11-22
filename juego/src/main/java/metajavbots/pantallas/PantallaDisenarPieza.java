package metajavbots.pantallas;

import java.util.List;
import java.util.Map;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.piezas.DisenadorDePieza;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * DISEÑAR PIEZA — Pantalla accesible desde el Taller donde el jugador escribe
 * en Java su propia pieza (implementando {@code PiezaPropia}, {@code EigenesTeil}
 * o {@code CustomPart}), la compila bajo las barandillas de seguridad de
 * {@code Vigilante} y {@code Cargador}, comprueba el presupuesto de equilibrio
 * (máx. 100 puntos), la guarda en su partida y la monta en su robot activo.
 */
public final class PantallaDisenarPieza implements Pantalla {

    private static final Pieza.Ranura[] RANURAS = {
        Pieza.Ranura.BRAZO, Pieza.Ranura.CABEZA, Pieza.Ranura.PIERNAS, Pieza.Ranura.NUCLEO
    };

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final Ui.Desplazable despl = new Ui.Desplazable();
    private int indiceRanura;
    private volatile DisenadorDePieza.Resultado resultado;
    private volatile boolean compilando;
    private String aviso = "";
    private float t;

    @Override
    public void alEntrar(Juego j) {
        if (editor.doc.largo() > 0) return;
        Jugador jug = j.jugador;
        editor.ponerTexto(DisenadorDePieza.plantilla(jug.idioma, RANURAS[indiceRanura]));
        editor.tamano = jug.tamanoDeLetra;
        editor.alCompilar = () -> compilar(j);
        editor.palabras = Palabras.api(jug.idioma);
        editor.documentacion = w -> Referencia.de(w, jug.idioma);
    }

    @Override
    public String contextoParaElEntrenador() {
        return EntrenadorFlotante.contextoDeCodigo(
                "The player is designing a custom Java robot part implementing PiezaPropia/EigenesTeil/CustomPart (max budget 100).", editor);
    }

    private void compilar(Juego j) {
        if (compilando) return;
        if (!Compilador.hayCompilador()) {
            aviso = T.t("taller.sin_jdk");
            return;
        }
        compilando = true;
        j.sonidos.tocar("compilar", 0.5f);
        String codigo = editor.doc.texto();
        Thread h = new Thread(() -> {
            try {
                resultado = DisenadorDePieza.compilarYValidar(codigo, j.jugador.idioma);
            } finally {
                compilando = false;
            }
        }, "disenar-pieza");
        h.setDaemon(true);
        h.start();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        DisenadorDePieza.Resultado res = resultado;
        if (res != null && !compilando && editor.marcas.isEmpty() && res.compilacion != null && !res.compilacion.bien) {
            for (Compilador.Fallo f : res.compilacion.errores) editor.marcas.add(Explicador.marca(f, j.jugador));
        }
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("pieza_propia.titulo"), true);
        float x = 20, y = arriba + 14, w = Math.min(500, p.ancho * 0.36f);
        panelIzquierdo(j, res, x, y, w);
        panelEditor(j, res, x + w + 16, y, p.ancho - (x + w + 36));
    }

    private void panelIzquierdo(Juego j, DisenadorDePieza.Resultado res, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cy = ui.panel(x, y, w, p.alto - y - 20, T.t("pieza_propia.panel"));
        float d = ui.desplazable(despl, x + 8, cy, w - 16, p.alto - cy - 32);
        float yy = cy - d;
        yy += p.parrafo(T.t("pieza_propia.ayuda"), x + 18, yy, w - 44, ui.letra * 0.82f, ui.tema.textoSuave, 1) + 12;
        yy = selectorRanura(j, x + 18, yy, w - 44);
        yy = barraPresupuesto(j, res, x + 18, yy, w - 44);
        yy = listaErroresOValidacion(j, res, x + 18, yy, w - 44);
        yy = listaGuardadas(j, x + 18, yy, w - 44);
        ui.finDesplazable(despl, yy + d - cy);
    }

    private float selectorRanura(Juego j, float x, float yy, float w) {
        Ui ui = j.ui;
        Pincel p = j.pincel;
        p.texto(T.t("pieza_propia.plantilla_ranura"), x, yy, ui.letra * 0.82f, true, ui.tema.texto, 1);
        yy += ui.letra * 1.35f;
        String[] nombres = {T.t("hueco.izquierda"), T.t("hueco.cabeza"), T.t("hueco.piernas"), T.t("hueco.nucleo")};
        Ui.Estilo[] est = new Ui.Estilo[RANURAS.length];
        boolean[] act = new boolean[RANURAS.length];
        for (int i = 0; i < RANURAS.length; i++) {
            est[i] = i == indiceRanura ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
            act[i] = true;
        }
        int pulsado = ui.botonera(nombres, est, act, new String[RANURAS.length], x, yy, w, 38);
        if (pulsado >= 0) {
            indiceRanura = pulsado;
            editor.doc.cambiarTodo(DisenadorDePieza.plantilla(j.jugador.idioma, RANURAS[indiceRanura]));
            editor.marcas.clear();
            resultado = null;
            aviso = "";
        }
        return yy + ui.altoDeBotonera + 12;
    }

    private float barraPresupuesto(Juego j, DisenadorDePieza.Resultado res, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        double pts = res == null ? 0 : res.presupuesto;
        int color = res == null ? ui.tema.textoApagado : pts <= DisenadorDePieza.MAX_PRESUPUESTO ? ui.tema.bien : ui.tema.mal;
        String txt = T.f("pieza_propia.presupuesto", Math.round(pts), Math.round(DisenadorDePieza.MAX_PRESUPUESTO));
        p.texto(txt, x, yy, ui.letra * 0.84f, true, color, 1);
        yy += ui.letra * 1.3f;
        p.caja(x, yy, w, 14, 6, ui.tema.panelAlto, 1);
        float frac = (float) Math.max(0, Math.min(1, pts / DisenadorDePieza.MAX_PRESUPUESTO));
        if (frac > 0) p.caja(x, yy, w * frac, 14, 6, color, 1);
        return yy + 26;
    }

    private float listaErroresOValidacion(Juego j, DisenadorDePieza.Resultado res, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (res == null) return yy;
        if (res.valida && res.pieza != null) {
            p.texto("✔ " + res.pieza.nombre(T.idioma), x, yy, ui.letra * 0.9f, true, ui.tema.bien, 1);
            yy += ui.letra * 1.3f;
            yy += p.parrafo(Taller.cifras(res.pieza), x, yy, w, ui.letra * 0.76f, ui.tema.texto, 1) + 12;
        } else {
            for (String err : res.errores) {
                yy += p.parrafo("✘ " + err, x, yy, w, ui.letra * 0.76f, ui.tema.mal, 1) + 6;
            }
            yy += 8;
        }
        return yy;
    }

    private float listaGuardadas(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        p.texto(T.t("pieza_propia.guardadas"), x, yy, ui.letra * 0.85f, true, ui.tema.acento, 1);
        yy += ui.letra * 1.4f;
        List<Pieza> propias = jug.piezasPropias();
        if (propias.isEmpty()) {
            return yy + p.parrafo(T.t("pieza_propia.ninguna"), x, yy, w, ui.letra * 0.76f, ui.tema.textoApagado, 1) + 10;
        }
        for (Pieza pz : propias) {
            float bw = w - 48;
            if (ui.boton(pz.nombre(T.idioma) + " (" + pz.ranura.name() + ")", x, yy, bw, 38, Ui.Estilo.PLANO)) {
                String cod = jug.piezasPropiasCodigo.get(pz.id);
                if (cod != null) {
                    editor.doc.cambiarTodo(cod);
                    editor.marcas.clear();
                    resultado = DisenadorDePieza.compilarYValidar(cod, jug.idioma);
                }
            }
            if (ui.boton("✕", x + bw + 6, yy, 40, 38, Ui.Estilo.PLANO)) {
                jug.eliminarPiezaPropia(pz.id);
                j.guardar();
                aviso = T.f("pieza_propia.borrada", pz.nombre(T.idioma));
                break;
            }
            yy += 44;
        }
        return yy + 8;
    }

    private void panelEditor(Juego j, DisenadorDePieza.Resultado res, float ex, float y, float ew) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean puedeGuardar = res != null && res.valida && res.pieza != null && !compilando;
        String[] barra = {
            compilando ? T.t("taller.compilando") : T.t("pieza_propia.validar") + " (Ctrl+S)",
            T.t("pieza_propia.guardar_montar"), T.t("taller.formatear")
        };
        Ui.Estilo[] estilos = {Ui.Estilo.COMPILAR, Ui.Estilo.PRINCIPAL, Ui.Estilo.PLANO};
        boolean[] activos = {!compilando && Compilador.hayCompilador(), puedeGuardar, true};
        String[] ayudas = {Compilador.hayCompilador() ? null : T.t("taller.sin_jdk"), null, null};
        switch (ui.botonera(barra, estilos, activos, ayudas, ex, y, ew, 46)) {
            case 0:
                editor.marcas.clear();
                compilar(j);
                break;
            case 1:
                if (puedeGuardar) guardarYMontar(j, res.pieza);
                break;
            case 2:
                editor.doc.formatear();
                break;
            default:
                break;
        }
        float ey = y + ui.altoDeBotonera + 12, eh = p.alto - ey - 66;
        editor.dibujar(ui, ex, ey, ew, eh);
        String estado = aviso.isEmpty()
                ? (res == null ? T.t("pieza_propia.estado_inicial")
                : res.valida ? T.f("pieza_propia.lista_para_montar", res.pieza.nombre(T.idioma))
                : T.f("pelea.errores", res.errores.size()))
                : aviso;
        int color = !aviso.isEmpty() ? ui.tema.bien : res == null ? ui.tema.textoApagado : res.valida ? ui.tema.bien : ui.tema.mal;
        p.texto(ui.recortarTexto(estado, ew, ui.letra * 0.9f, true), ex, p.alto - 48, ui.letra * 0.9f, true, color, 1);
    }

    private void guardarYMontar(Juego j, Pieza pieza) {
        Jugador jug = j.jugador;
        jug.guardarPiezaPropia(pieza, editor.doc.texto());
        Jugador.Robot r = jug.robot();
        if (r != null) {
            Montaje.Hueco hueco = huecoPara(pieza.ranura);
            if (hueco != null) r.piezas.put(hueco, pieza.id);
        }
        j.guardar();
        j.sonidos.tocar("subida", 0.7f);
        aviso = T.f("pieza_propia.guardada_ok", pieza.nombre(T.idioma));
    }

    private static Montaje.Hueco huecoPara(Pieza.Ranura ranura) {
        for (Map.Entry<Pieza.Ranura, Montaje.Hueco> e : Map.of(
                Pieza.Ranura.CABEZA, Montaje.Hueco.CABEZA,
                Pieza.Ranura.BRAZO, Montaje.Hueco.IZQUIERDA,
                Pieza.Ranura.PIERNAS, Montaje.Hueco.PIERNAS,
                Pieza.Ranura.NUCLEO, Montaje.Hueco.NUCLEO).entrySet()) {
            if (e.getKey() == ranura) return e.getValue();
        }
        return null;
    }
}
