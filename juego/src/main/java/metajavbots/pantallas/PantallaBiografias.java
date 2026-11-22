package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Biografias;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * PANTALLA BIOGRAFÍAS — Permite consultar, editar y restaurar al texto original
 * las biografías de los 22 robots y los 7 entrenadores desde dentro del juego.
 * Los cambios se persisten en el archivo de partida sin modificar res/biografias/.
 */
public final class PantallaBiografias implements Pantalla {

    private boolean entrenadores;
    private int elegido;
    private String idInicial;
    private Idioma idiomaEditado;
    private final CampoDeTexto editor = new CampoDeTexto();
    private final Ui.Desplazable despLista = new Ui.Desplazable();
    private VistaDeArena vista;
    private String aviso = "";
    private float t;

    /** Abre el editor de biografías en la pestaña y elemento indicados (null para el primero). */
    public PantallaBiografias(boolean entrenadores, String idInicial) {
        this.entrenadores = entrenadores;
        this.idInicial = idInicial;
        this.editor.maximo = 360;
    }

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        idiomaEditado = j.jugador == null ? Idioma.ES : j.jugador.idioma;
        elegido = indiceInicial();
        cargarActual();
    }

    @Override
    public String musica() {
        return "menu";
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("biografias.titulo"), true) + 16;
        float abajo = p.alto - 20;
        float lw = Math.min(400, p.ancho * 0.30f);

        String[] tabs = {T.t("biografias.robots"), T.t("biografias.entrenadores")};
        int tab = ui.pestanas(tabs, entrenadores ? 1 : 0, 20, arriba, lw, 44);
        if ((tab == 1) != entrenadores) {
            entrenadores = tab == 1;
            elegido = 0;
            aviso = "";
            cargarActual();
        }
        dibujarLista(j, 20, arriba + 52, lw, abajo - arriba - 52);
        dibujarEditor(j, 20 + lw + 20, arriba, p.ancho - lw - 60, abajo - arriba);
    }

    private void dibujarLista(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Idioma idi = j.jugador.idioma;
        float cy = ui.panel(x, y, w, h, null);
        float zona = y + h - cy - 10;
        float d = ui.desplazable(despLista, x + 8, cy, w - 16, zona);
        float yy = cy - d;
        float fila = Math.max(48, ui.letra * 2.5f);
        int total = totalElementos();
        for (int i = 0; i < total; i++) {
            String id = idEn(i);
            String nom = nombreEn(i, idi);
            boolean custom = entrenadores ? Biografias.esPersonalizadaEntrenador(id, idiomaEditado)
                    : Biografias.esPersonalizadaRobot(id, idiomaEditado);
            String etiqueta = (custom ? "✎ " : "") + nom;
            if (yy + fila > cy && yy < cy + zona
                    && ui.boton(etiqueta, x + 12, yy, w - 36, fila - 6, i == elegido ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                elegido = i;
                aviso = "";
                cargarActual();
            }
            yy += fila;
        }
        ui.finDesplazable(despLista, yy + d - cy);
    }

    private void dibujarEditor(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Idioma idi = j.jugador.idioma;
        float cy = ui.panel(x, y, w, h, null);
        String id = idEn(elegido);
        String nombre = nombreEn(elegido, idi);

        if (entrenadores) {
            Entrenador en = Entrenador.de(id);
            CabezaDeEntrenador.dibujar(p, en, x + 64, cy + 52, 36, false, t);
            p.texto(nombre, x + 120, cy + 16, ui.letra * 1.4f, true, 0xFFC857, 1);
            p.texto(en.papel.en(idi), x + 120, cy + 16 + ui.letra * 1.8f, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
        } else {
            Modelo m = Modelo.de(id);
            vista.robotSuelto(p, new Montaje(m), x + 66, cy + 92, 1.0f, t, 0, true);
            p.texto(nombre, x + 124, cy + 16, ui.letra * 1.4f, true, 0xFFC857, 1);
            p.texto(m.lema.en(idi), x + 124, cy + 16 + ui.letra * 1.8f, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
        }
        cy += 116;

        String[] langs = {"ES", "DE", "EN"};
        int selLang = idiomaEditado.ordinal();
        int nuevoLang = ui.pestanas(langs, selLang, x + 20, cy, Math.min(300, w - 40), 38);
        if (nuevoLang != selLang) {
            idiomaEditado = Idioma.values()[nuevoLang];
            aviso = "";
            cargarActual();
        }
        boolean custom = entrenadores ? Biografias.esPersonalizadaEntrenador(id, idiomaEditado)
                : Biografias.esPersonalizadaRobot(id, idiomaEditado);
        String estado = custom ? T.t("biografias.personalizada") : T.t("biografias.original");
        p.texto(estado, x + Math.min(340, w - 220), cy + 8, ui.letra * 0.85f, true, custom ? ui.tema.aviso : ui.tema.textoSuave, 1);
        cy += 52;

        p.texto(T.t("biografias.vista_previa"), x + 20, cy, ui.letra * 0.85f, true, ui.tema.textoSuave, 1);
        cy += ui.letra * 1.3f;
        String preview = editor.texto().isBlank() ? textoBase(id) : editor.texto().strip();
        float ph = Math.max(70, p.altoDeParrafo(preview, w - 68, ui.letra * 0.95f) + 24);
        p.caja(x + 20, cy, w - 40, ph, 10, ui.tema.fondo, 0.95f);
        p.parrafo(preview, x + 34, cy + 12, w - 68, ui.letra * 0.95f, ui.tema.texto, 1);
        cy += ph + 16;

        p.texto(T.t("biografias.campo"), x + 20, cy, ui.letra * 0.85f, true, ui.tema.textoSuave, 1);
        cy += ui.letra * 1.3f;
        boolean intro = editor.dibujar(ui, x + 20, cy, w - 40, 52, textoBase(id));
        cy += 68;

        String[] btns = {T.t("biografias.guardar"), T.t("biografias.restaurar")};
        Ui.Estilo[] est = {Ui.Estilo.PRINCIPAL, Ui.Estilo.NORMAL};
        boolean[] act = {!editor.texto().isBlank(), custom};
        String[] ayu = {T.t("biografias.guardar_ayuda"), T.t("biografias.restaurar_ayuda")};
        int pulsado = ui.botonera(btns, est, act, ayu, x + 20, cy, w - 40, 48);
        if (intro || pulsado == 0) {
            guardarActual(id);
            j.sonidos.tocar("bien", 0.5f);
        } else if (pulsado == 1) {
            restaurarActual(id);
            j.sonidos.tocar("clic", 0.5f);
        }
        cy += ui.altoDeBotonera + 14;
        if (!aviso.isEmpty()) p.parrafo(aviso, x + 20, cy, w - 40, ui.letra * 0.9f, ui.tema.bien, 1);
    }

    private void guardarActual(String id) {
        if (entrenadores) Biografias.guardarEntrenador(id, idiomaEditado, editor.texto());
        else Biografias.guardarRobot(id, idiomaEditado, editor.texto());
        aviso = T.t("biografias.guardada");
    }

    private void restaurarActual(String id) {
        if (entrenadores) Biografias.restaurarEntrenador(id, idiomaEditado);
        else Biografias.restaurarRobot(id, idiomaEditado);
        cargarActual();
        aviso = T.t("biografias.restaurada");
    }

    private void cargarActual() {
        String id = idEn(elegido);
        String actual = entrenadores ? Biografias.deEntrenador(id, idiomaEditado) : Biografias.deRobot(id, idiomaEditado);
        editor.ponerTexto(actual);
    }

    private String textoBase(String id) {
        return entrenadores ? Biografias.baseDeEntrenador(id, idiomaEditado) : Biografias.baseDeRobot(id, idiomaEditado);
    }

    private int totalElementos() {
        return entrenadores ? Entrenador.todos().size() : Modelo.todos().size();
    }

    private String idEn(int i) {
        if (entrenadores) {
            List<Entrenador> l = Entrenador.todos();
            return l.get(Math.max(0, Math.min(l.size() - 1, i))).id;
        }
        List<Modelo> l = Modelo.todos();
        return l.get(Math.max(0, Math.min(l.size() - 1, i))).id;
    }

    private String nombreEn(int i, Idioma idi) {
        if (entrenadores) {
            List<Entrenador> l = Entrenador.todos();
            return l.get(Math.max(0, Math.min(l.size() - 1, i))).nombre.en(idi);
        }
        List<Modelo> l = Modelo.todos();
        return l.get(Math.max(0, Math.min(l.size() - 1, i))).nombre(idi);
    }

    private int indiceInicial() {
        if (idInicial == null || idInicial.isBlank()) return 0;
        int total = totalElementos();
        for (int i = 0; i < total; i++) if (idEn(i).equals(idInicial)) return i;
        return 0;
    }
}
