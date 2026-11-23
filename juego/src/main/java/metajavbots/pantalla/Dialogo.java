package metajavbots.pantalla;

import java.util.function.Consumer;

import com.badlogic.gdx.Input;

/**
 * DIÁLOGO — Una ventanita encima de la pantalla para confirmar algo («¿Borrar
 * la partida?»), elegir entre tres opciones («Seguir», «Editar el código y
 * reintentar», «Rendirme») o pedir un texto («Nombre nuevo»). Mientras está
 * abierta, lo de debajo no recibe ni el ratón ni el teclado. Intro acepta y
 * Esc cancela. Se usa en dos pasos: antes() al principio del fotograma y
 * dibujar() al final.
 */
public final class Dialogo {

    private boolean abierto, peligro, conCampo;
    private String titulo = "", texto = "", aceptar = "", cancelar = "", intermedia = "";
    private Consumer<String> alAceptar;
    private Runnable alCancelar, alIntermedio;
    private final CampoDeTexto campo = new CampoDeTexto();

    /** ¿Está abierta la ventana del diálogo? */
    public boolean abierto() {
        return abierto;
    }

    /** Pide confirmar algo. Con «peligro», el botón de aceptar sale en rojo (borrar o rendirse). */
    public void confirmar(String titulo, String texto, String aceptar, String cancelar, boolean peligro, Runnable siAcepta) {
        confirmar(titulo, texto, aceptar, cancelar, peligro, siAcepta, null);
    }

    /** Igual, avisando también si el usuario cancela o pulsa Esc. */
    public void confirmar(String titulo, String texto, String aceptar, String cancelar, boolean peligro, Runnable siAcepta, Runnable siCancela) {
        abrir(titulo, texto, aceptar, cancelar, peligro);
        conCampo = false;
        intermedia = "";
        alIntermedio = null;
        alAceptar = s -> siAcepta.run();
        alCancelar = siCancela;
    }

    /**
     * Presenta un cuadro con tres opciones (por ejemplo, al pulsar Escape en carrera:
     * «Seguir», «Editar el código y reintentar» y «Rendirme»). Esc equivale a la primera opción («Seguir»).
     */
    public void elegirTres(String titulo, String texto, String seguir, String opcionIntermedia, String rendirse,
            Runnable alSeguir, Runnable accionIntermedia, Runnable alRendirse) {
        abrir(titulo, texto, rendirse, seguir, true);
        conCampo = false;
        this.intermedia = opcionIntermedia == null ? "" : opcionIntermedia;
        this.alCancelar = alSeguir;
        this.alIntermedio = accionIntermedia;
        this.alAceptar = s -> {
            if (alRendirse != null) alRendirse.run();
        };
    }

    /** Pide un texto (un nombre nuevo), con lo que había escrito para cambiarlo. */
    public void pedirTexto(String titulo, String texto, String inicial, String aceptar, String cancelar, Consumer<String> siAcepta) {
        abrir(titulo, texto, aceptar, cancelar, false);
        conCampo = true;
        intermedia = "";
        alIntermedio = null;
        campo.ponerTexto(inicial);
        campo.doc.seleccionarTodo();
        campo.enfocado = true;
        campo.maximo = 30;
        alAceptar = siAcepta;
    }

    private void abrir(String titulo, String texto, String aceptar, String cancelar, boolean peligro) {
        this.titulo = titulo;
        this.texto = texto;
        this.aceptar = aceptar;
        this.cancelar = cancelar;
        this.peligro = peligro;
        this.alCancelar = null;
        abierto = true;
    }

    /** Cierra el diálogo y desenfoca el campo de texto. */
    public void cerrar() {
        abierto = false;
        campo.enfocado = false;
    }

    /** Al principio del fotograma: con el diálogo abierto, lo de debajo no se entera de nada. */
    public void antes(Ui ui) {
        if (abierto) ui.e.bloqueada = true;
    }

    /** Al final del fotograma: lo oscurece todo, dibuja la ventanita encima y la atiende. */
    public void dibujar(Ui ui) {
        if (!abierto) return;
        Entrada e = ui.e;
        e.bloqueada = false;
        Pincel p = ui.p;
        Tema t = ui.tema;
        p.rect(0, 0, p.ancho, p.alto, 0x000000, 0.58f);
        boolean tres = !intermedia.isEmpty();
        float wBase = tres ? 860f : 720f;
        if (tres) {
            float tBtn = Math.min(ui.letra, 28f);
            float req = p.ancho(cancelar, tBtn, true) + p.ancho(intermedia, tBtn, true) + p.ancho(aceptar, tBtn, true) + 190f;
            wBase = Math.max(wBase, req);
        }
        float w = Math.min(wBase, p.ancho - 64f);
        float letra = ui.letra;
        float altoTexto = p.altoDeParrafo(texto, w - 64, letra);
        float h = 40 + letra * 1.8f + 20 + altoTexto + (conCampo ? 76 : 0) + 104;
        float x = (p.ancho - w) / 2, y = (p.alto - h) / 2;
        p.caja(x + 4, y + 6, w, h, 14, 0x000000, 0.42f);
        p.caja(x, y, w, h, 14, t.panelAlto, 1);
        p.bordeRedondeado(x, y, w, h, 14, peligro ? t.mal : t.acento, 1, 2);
        float cy = y + 26;
        float tTit = ui.cabe(titulo, w - 64, letra * 1.35f, true);
        p.texto(titulo, x + 32, cy, tTit, true, t.texto, 1);
        cy += letra * 1.8f + 18;
        cy += p.parrafo(texto, x + 32, cy, w - 64, letra, t.texto, 1);
        boolean intro = false;
        if (conCampo) {
            cy += 16;
            intro = campo.dibujar(ui, x + 32, cy, w - 64, 54, "");
            campo.enfocado = true;
        }
        dibujarBotones(ui, e, p, x, y, w, h, tres, intro);
    }

    private void dibujarBotones(Ui ui, Entrada e, Pincel p, float x, float y, float w, float h, boolean tres, boolean intro) {
        float by = y + h - 82;
        boolean si, no, medio = false;
        if (tres) {
            float sep = 14f, disp = w - 64f - 2f * sep;
            float tBtn = Math.min(ui.letra, 28f);
            float w1 = Math.max(120f, p.ancho(cancelar, tBtn, true) + 36f);
            float w2 = Math.max(180f, p.ancho(intermedia, tBtn, true) + 36f);
            float w3 = Math.max(130f, p.ancho(aceptar, tBtn, true) + 36f);
            float suma = w1 + w2 + w3;
            float esc = disp / Math.max(1f, suma);
            w1 *= esc;
            w2 *= esc;
            w3 *= esc;
            no = ui.boton(cancelar, x + 32f, by, w1, 56, Ui.Estilo.PRINCIPAL, true, null);
            medio = ui.boton(intermedia, x + 32f + w1 + sep, by, w2, 56, Ui.Estilo.NORMAL, true, null);
            si = ui.boton(aceptar, x + 32f + w1 + sep + w2 + sep, by, w3, 56, Ui.Estilo.PELIGRO, true, null);
        } else {
            float bw = Math.min(280, (w - 84) / 2);
            si = ui.boton(aceptar, x + w - 32 - bw, by, bw, 56, peligro ? Ui.Estilo.PELIGRO : Ui.Estilo.PRINCIPAL, true, null);
            no = ui.boton(cancelar, x + w - 48 - 2 * bw, by, bw, 56, Ui.Estilo.NORMAL, true, null);
        }
        if (!conCampo && !tres && (e.tecla(Input.Keys.ENTER) || e.tecla(Input.Keys.NUMPAD_ENTER))) intro = true;
        if (e.tecla(Input.Keys.ESCAPE)) no = true;
        ui.reservarEscape();
        if (medio) {
            Runnable r = alIntermedio;
            cerrar();
            if (r != null) r.run();
        } else if (si || intro) {
            cerrar();
            alAceptar.accept(campo.texto().strip());
        } else if (no) {
            Runnable r = alCancelar;
            cerrar();
            if (r != null) r.run();
        }
    }
}
