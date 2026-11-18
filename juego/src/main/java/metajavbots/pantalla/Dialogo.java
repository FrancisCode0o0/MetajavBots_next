package metajavbots.pantalla;

import java.util.function.Consumer;

import com.badlogic.gdx.Input;

/**
 * DIÁLOGO — Una ventanita encima de la pantalla para confirmar algo («¿Borrar
 * la partida?») o pedir un texto («Nombre nuevo»). Mientras está abierta, lo
 * de debajo no recibe ni el ratón ni el teclado. Intro acepta y Esc cancela.
 * Se usa en dos pasos: antes() al principio del fotograma y dibujar() al final.
 */
public final class Dialogo {

    private boolean abierto, peligro;
    private String titulo = "", texto = "", aceptar = "", cancelar = "";
    private Consumer<String> alAceptar;
    private final CampoDeTexto campo = new CampoDeTexto();
    private boolean conCampo;

    public boolean abierto() {
        return abierto;
    }

    /** Pide confirmar algo. Con «peligro», el botón de aceptar sale en rojo (borrar). */
    public void confirmar(String titulo, String texto, String aceptar, String cancelar, boolean peligro, Runnable siAcepta) {
        abrir(titulo, texto, aceptar, cancelar, peligro);
        conCampo = false;
        alAceptar = s -> siAcepta.run();
    }

    /** Pide un texto (un nombre nuevo), con lo que había escrito para cambiarlo. */
    public void pedirTexto(String titulo, String texto, String inicial, String aceptar, String cancelar, Consumer<String> siAcepta) {
        abrir(titulo, texto, aceptar, cancelar, false);
        conCampo = true;
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
        abierto = true;
    }

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
        p.rect(0, 0, p.ancho, p.alto, 0x000000, 0.55f);
        float w = Math.min(720, p.ancho - 80);
        float letra = ui.letra;
        float altoTexto = p.altoDeParrafo(texto, w - 64, letra);
        float h = 40 + letra * 1.8f + 20 + altoTexto + (conCampo ? 76 : 0) + 100;
        float x = (p.ancho - w) / 2, y = (p.alto - h) / 2;
        p.caja(x + 4, y + 6, w, h, 14, 0x000000, 0.4f);
        p.caja(x, y, w, h, 14, t.panelAlto, 1);
        p.bordeRedondeado(x, y, w, h, 14, peligro ? t.mal : t.acento, 1, 2);
        float cy = y + 28;
        p.texto(titulo, x + 32, cy, letra * 1.35f, true, t.texto, 1);
        cy += letra * 1.8f + 20;
        cy += p.parrafo(texto, x + 32, cy, w - 64, letra, t.texto, 1);
        boolean intro = false;
        if (conCampo) {
            cy += 16;
            intro = campo.dibujar(ui, x + 32, cy, w - 64, 54, "");
            campo.enfocado = true;
            cy += 60;
        }
        float bw = Math.min(260, (w - 96) / 2), by = y + h - 84;
        boolean si = ui.boton(aceptar, x + w - 32 - bw, by, bw, 56, peligro ? Ui.Estilo.PELIGRO : Ui.Estilo.PRINCIPAL, true, null);
        boolean no = ui.boton(cancelar, x + w - 48 - 2 * bw, by, bw, 56, Ui.Estilo.NORMAL, true, null);
        if (!conCampo && (e.tecla(Input.Keys.ENTER) || e.tecla(Input.Keys.NUMPAD_ENTER))) intro = true;
        if (e.tecla(Input.Keys.ESCAPE)) no = true;
        // Lo de debajo no debe ver ni el Esc ni el Intro del diálogo en el fotograma siguiente.
        ui.reservarEscape();
        if (si || intro) {
            cerrar();
            alAceptar.accept(campo.texto().strip());
        } else if (no) {
            cerrar();
        }
    }
}
