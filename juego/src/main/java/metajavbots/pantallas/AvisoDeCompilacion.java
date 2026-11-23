package metajavbots.pantallas;

import com.badlogic.gdx.Input;

import metajavbots.Juego;
import metajavbots.pantalla.Entrada;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * AVISO DE COMPILACIÓN — Cartel modal con el entrenador activo (respetando el
 * modelo elegido en {@link Entrenador}) que advierte de forma inequívoca al
 * jugador antes de entrar a pelear por primera vez si su robot nunca se ha
 * compilado, y ofrece ir directamente al Taller.
 */
public final class AvisoDeCompilacion {

    private boolean abierto;
    private PantallaPelea peleaPendiente;
    private boolean pasePelea;
    private float tiempo;

    /** Indica si corresponde mostrar el aviso inicial del entrenador al llegar al Menú. */
    public static boolean debeAvisarEnMenu(Jugador jug) {
        return jug != null && !jug.robotActualCompilado() && !jug.avisoCompilarVisto;
    }

    /** Indica si corresponde advertir al jugador antes de lanzar un combate con su robot. */
    public static boolean debeAvisarAlPelear(Jugador jug) {
        return jug != null && !jug.robotActualCompilado();
    }

    /** Texto de bienvenida del entrenador activo cuando la partida arranca sin ningún robot compilado. */
    public static String mensajeBienvenida(Jugador jug, Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        Entrenador en = Entrenador.de(jug == null ? null : jug.entrenador);
        return en.nombre.en(id) + ": " + T.en("entrenador.aviso_sin_compilar_menu", id);
    }

    /** Texto de advertencia del entrenador activo al intentar entrar en combate con un robot nunca compilado. */
    public static String mensajeAntesDePelear(Jugador jug, Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        Entrenador en = Entrenador.de(jug == null ? null : jug.entrenador);
        return en.nombre.en(id) + ": " + T.en("entrenador.aviso_sin_compilar_pelea", id);
    }

    /** Recordatorio visible en el Taller si el robot nunca se ha compilado o tiene cambios sin compilar. */
    public static String recordatorioEnTaller(Jugador.Robot r, String codigoActual, Idioma idioma) {
        if (r == null) return "";
        Idioma id = idioma == null ? Idioma.ES : idioma;
        if (!r.tieneCodigoCompilado()) return T.en("taller.aviso_nunca_compilado", id);
        String actual = codigoActual == null ? "" : codigoActual;
        if (!actual.equals(r.codigoCompilado)) return T.en("taller.aviso_cambios_sin_compilar", id);
        return "";
    }

    /** Muestra el aviso inicial del entrenador en el menú principal. */
    public void mostrarBienvenida(Juego j) {
        if (j == null || j.jugador == null) return;
        abierto = true;
        peleaPendiente = null;
        tiempo = 0f;
        Entrenador en = Entrenador.de(j.jugador.entrenador);
        if (j.voz != null) j.voz.decir(T.t("entrenador.aviso_sin_compilar_menu"), j.jugador.idioma, en.timbre);
    }

    /** Muestra el aviso obligatorio del entrenador antes de entrar a una pelea con robot sin compilar. */
    public void mostrarAntesDePelear(Juego j, PantallaPelea destino) {
        if (j == null || j.jugador == null || destino == null) return;
        abierto = true;
        peleaPendiente = destino;
        tiempo = 0f;
        Entrenador en = Entrenador.de(j.jugador.entrenador);
        if (j.voz != null) j.voz.decir(T.t("entrenador.aviso_sin_compilar_pelea"), j.jugador.idioma, en.timbre);
    }

    /** Consume el pase de una sola vez tras confirmar que se entra en combate sin compilar. */
    public boolean consumirPasePelea() {
        if (!pasePelea) return false;
        pasePelea = false;
        return true;
    }

    /** Indica si el cartel de aviso está abierto en pantalla. */
    public boolean abierto() {
        return abierto;
    }

    /** Cierra el aviso sin lanzar acciones pendientes. */
    public void cerrar() {
        abierto = false;
        peleaPendiente = null;
    }

    /** Bloquea la entrada de la pantalla inferior mientras el aviso está abierto. */
    public void antes(Juego j) {
        if (abierto && j != null && j.entrada != null) j.entrada.bloqueada = true;
    }

    /** Dibuja el cartel con la cabeza del entrenador activo y sus botones de acción. */
    public void dibujar(Juego j, float dt) {
        if (!abierto || j == null || j.jugador == null) return;
        // Si a la vez sale «¿Cómo se juega?», primero ese: una sola cosa en pantalla cada vez.
        if (j.comoSeJuega.abierto()) return;
        tiempo += dt;
        Entrada e = j.entrada;
        e.bloqueada = false;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Entrenador en = Entrenador.de(jug.entrenador);
        Idioma id = jug.idioma;
        boolean esPelea = peleaPendiente != null;
        String titulo = T.en("entrenador.aviso_sin_compilar_titulo", id);
        String cuerpo = T.en(esPelea ? "entrenador.aviso_sin_compilar_pelea" : "entrenador.aviso_sin_compilar_menu", id);
        p.rect(0, 0, p.ancho, p.alto, 0x000000, 0.62f);
        float w = Math.min(820f, p.ancho - 64f);
        float radio = 46f;
        float tx = 28f + radio * 2f + 22f;
        float tw = Math.max(240f, w - tx - 28f);
        float tTit = ui.cabe(titulo, tw, Math.min(28f, ui.letra * 1.3f), true);
        float tNom = ui.cabe(en.nombre.en(id), tw, Math.min(20f, ui.letra * 0.9f), true);
        float tCue = Math.min(21f, ui.letra * 0.96f);
        float altoTexto = p.altoDeLinea(tTit) + 4f + p.altoDeLinea(tNom) + 12f + p.altoDeParrafo(cuerpo, tw, tCue);
        float h = Math.max(250f, 28f + Math.max(radio * 2f + 24f, altoTexto) + 92f);
        float x = (p.ancho - w) / 2f;
        float y = (p.alto - h) / 2f;
        dibujarTarjeta(p, ui, en, id, x, y, w, h, titulo, cuerpo);
        dibujarBotones(j, x, y, w, h, esPelea, id);
    }

    private void dibujarTarjeta(Pincel p, Ui ui, Entrenador en, Idioma id, float x, float y, float w, float h,
            String titulo, String cuerpo) {
        Tema tema = ui.tema;
        float radio = 46f;
        float tx = 28f + radio * 2f + 22f;
        float tw = Math.max(240f, w - tx - 28f);
        float tTit = ui.cabe(titulo, tw, Math.min(28f, ui.letra * 1.3f), true);
        float tNom = ui.cabe(en.nombre.en(id), tw, Math.min(20f, ui.letra * 0.9f), true);
        float tCue = Math.min(21f, ui.letra * 0.96f);
        p.caja(x + 4, y + 6, w, h, 16, 0x000000, 0.42f);
        p.caja(x, y, w, h, 16, tema.panelAlto, 1f);
        p.bordeRedondeado(x, y, w, h, 16, Ui.COMPILAR_FONDO, 1f, 2.5f);
        CabezaDeEntrenador.dibujar(p, en, x + 28f + radio, y + 32f + radio, radio, tiempo < 4.5f, tiempo);
        float cy = y + 24f;
        p.texto(titulo, x + tx, cy, tTit, true, Ui.COMPILAR_TEXTO, 1f);
        cy += p.altoDeLinea(tTit) + 4f;
        p.texto(en.nombre.en(id) + " · " + en.papel.en(id), x + tx, cy, tNom, false, tema.textoSuave, 1f);
        cy += p.altoDeLinea(tNom) + 12f;
        p.parrafo(cuerpo, x + tx, cy, tw, tCue, tema.texto, 1f);
    }

    private void dibujarBotones(Juego j, float x, float y, float w, float h, boolean esPelea, Idioma id) {
        Ui ui = j.ui;
        Entrada e = j.entrada;
        float by = y + h - 74f;
        float bw = Math.min(330f, (w - 84f) / 2f);
        String txtTaller = T.en("entrenador.ir_al_taller", id);
        String txtSec = T.en(esPelea ? "entrenador.pelear_sin_compilar" : "comun.entendido", id);
        boolean irTaller = ui.boton(txtTaller, x + w - 28f - bw, by, bw, 52f, Ui.Estilo.COMPILAR, true, null);
        boolean secundario = ui.boton(txtSec, x + 28f, by, bw, 52f, Ui.Estilo.NORMAL, true, null);
        ui.reservarEscape();
        boolean esc = e.tecla(Input.Keys.ESCAPE);
        if (irTaller) {
            j.jugador.avisoCompilarVisto = true;
            cerrar();
            j.guardar();
            j.ir(new Taller());
        } else if (secundario || esc) {
            j.jugador.avisoCompilarVisto = true;
            PantallaPelea destino = peleaPendiente;
            cerrar();
            j.guardar();
            if (esPelea && secundario && destino != null) {
                pasePelea = true;
                j.ir(destino);
            }
        }
    }
}
