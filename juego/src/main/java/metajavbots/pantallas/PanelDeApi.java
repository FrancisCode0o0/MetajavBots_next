package metajavbots.pantallas;

import java.util.List;

import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * PANEL DE LA API — La ventana paralela al editor: un buscador (sin tildes,
 * por palabras) sobre la API de los robots y la referencia de Java,
 * ordenadas por los temas de Java; al elegir una entrada se ve su firma en
 * el idioma del jugador, su explicación y un ejemplo, y «Insertar» la
 * escribe en el código. Se abre con F1 (o Ctrl+clic) sobre una palabra.
 */
public final class PanelDeApi {

    private final CampoDeTexto busqueda = new CampoDeTexto();
    private String tema;
    private int elegida;
    private final Ui.Desplazable ficha = new Ui.Desplazable();
    private final Ui.Desplazable lista = new Ui.Desplazable();
    private String ultimaBusqueda = null, ultimoTema = "-";
    private List<Referencia.Entrada> resultados = List.of();
    private VentanaDeCodigo ultimoEditor;

    public void buscar(String palabra) {
        busqueda.ponerTexto(palabra == null ? "" : palabra);
        busqueda.enfocado = true;
        if (ultimoEditor != null) ultimoEditor.enfocada = false;
        tema = null;
        elegida = 0;
        ficha.alPrincipio();
        ficha.contenido = 0;
    }

    public void dibujar(Ui ui, Idioma idioma, VentanaDeCodigo editor, float x, float y, float w, float h) {
        ultimoEditor = editor;
        Pincel p = ui.p;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        p.bordeRedondeado(x, y, w, h, 10, ui.tema.borde, 1, 1);
        p.texto(T.t("api.titulo"), x + 14, y + 10, ui.letra * 1.05f, true, ui.tema.texto, 1);
        busqueda.dibujar(ui, x + 12, y + 44, w - 24, 46, T.t("api.buscar"));
        // El tema: se recorre con ◄ ►.
        List<Tema> temas = Tema.todos();
        int i = tema == null ? -1 : temas.indexOf(Tema.de(tema));
        if (ui.boton("◄", x + 12, y + 98, 40, 40, Ui.Estilo.PLANO)) i = Math.max(-1, i - 1);
        if (ui.boton("►", x + w - 52, y + 98, 40, 40, Ui.Estilo.PLANO)) i = Math.min(temas.size() - 1, i + 1);
        tema = i < 0 ? null : temas.get(i).id;
        String nombreTema = tema == null ? T.t("api.todos_los_temas") : tema + " · " + Tema.de(tema).nombre(idioma);
        p.textoCentrado(ui.recortarTexto(nombreTema, w - 130, ui.letra * 0.8f, false), x + w / 2, y + 108, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
        String q = busqueda.texto();
        if (!q.equals(ultimaBusqueda) || !String.valueOf(tema).equals(ultimoTema)) {
            resultados = Referencia.buscar(q, tema, idioma);
            ultimaBusqueda = q;
            ultimoTema = String.valueOf(tema);
            elegida = 0;
            lista.alPrincipio();
            ficha.alPrincipio();
            ficha.contenido = 0;
        }
        float ly = y + 148, lh = (h - 160) * 0.38f;
        float d = ui.desplazable(lista, x + 8, ly, w - 16, lh);
        float alto = ui.letra * 1.5f;
        float yy = ly - d;
        for (int k = 0; k < resultados.size(); k++) {
            if (yy > ly - alto && yy < ly + lh) {
                Referencia.Entrada e = resultados.get(k);
                if (k == elegida) p.rect(x + 10, yy, w - 32, alto, ui.tema.seleccion, 1);
                p.texto(ui.recortarTexto(e.firma(idioma), w - 50, ui.letra * 0.8f, false), x + 16, yy + alto * 0.18f, ui.letra * 0.8f, false,
                        e.esDeLaApi() ? ui.tema.metodo : ui.tema.texto, 1);
                if (ui.e.clic(x + 10, yy, w - 32, alto)) {
                    elegida = k;
                    ficha.alPrincipio();
                    ficha.contenido = 0;
                }
            }
            yy += alto;
        }
        ui.finDesplazable(lista, resultados.size() * alto);
        if (resultados.isEmpty()) p.texto(T.t("api.nada"), x + 16, ly + 8, ui.letra * 0.85f, false, ui.tema.textoApagado, 1);
        float dy = ly + lh + 12;
        p.rect(x + 12, dy - 6, w - 24, 1, ui.tema.borde, 1);
        if (elegida < resultados.size()) dibujarFicha(ui, idioma, editor, x, y, w, h, dy);
    }

    private void dibujarFicha(Ui ui, Idioma idioma, VentanaDeCodigo editor, float x, float y, float w, float h, float dy) {
        Pincel p = ui.p;
        Referencia.Entrada e = resultados.get(elegida);
        String ins = e.insertable(idioma);
        boolean mostrarInsertar = editor != null && !editor.soloLectura && !ins.isEmpty();
        float zona = Math.max(48, y + h - (mostrarInsertar ? 58 : 10) - dy);
        float desp = ui.desplazable(ficha, x + 4, dy, w - 8, zona);
        float inicio = dy;
        dy -= desp;
        float anchoTexto = w - 44;
        dy += p.parrafo(e.firma(idioma), x + 16, dy, anchoTexto, ui.letra * 0.9f, true, ui.tema.metodo, 1) + 6;
        Tema t = Tema.de(e.tema);
        if (t != null) dy += p.parrafo(T.f("api.tema", t.id, t.nombre(idioma)), x + 16, dy, anchoTexto, ui.letra * 0.78f, ui.tema.textoSuave, 1) + 6;
        dy += p.parrafo(e.texto(idioma), x + 16, dy, anchoTexto, ui.letra * 0.84f, ui.tema.texto, 1) + 8;
        String donde = e.donde(idioma);
        if (!donde.isEmpty()) {
            dy += p.parrafo(T.t("api.donde"), x + 16, dy, anchoTexto, ui.letra * 0.76f, true, ui.tema.aviso, 1) + 4;
            float anchoDonde = Math.max(40, w - 60);
            float dh = p.altoDeParrafo(donde, anchoDonde, ui.letra * 0.8f) + 12;
            p.caja(x + 14, dy, w - 38, dh, 6, ui.tema.panelAlto, 1);
            p.rect(x + 14, dy + 4, 3, Math.max(0, dh - 8), ui.tema.aviso, 0.9f);
            p.bordeRedondeado(x + 14, dy, w - 38, dh, 6, ui.tema.borde, 0.7f, 1);
            p.parrafo(donde, x + 23, dy + 6, anchoDonde, ui.letra * 0.8f, ui.tema.texto, 1);
            dy += dh + 10;
        }
        String ejemplo = e.ejemplo(idioma);
        if (!ejemplo.isEmpty()) {
            String etiqueta = e.esDeLaApi() ? T.t("entrenador.pseudo") : T.t("entrenador.ejemplo");
            dy += p.parrafo(etiqueta, x + 16, dy, anchoTexto, ui.letra * 0.76f, true, ui.tema.textoSuave, 1) + 4;
            float anchoEj = w - 60;
            float eh = p.altoDeParrafo(ejemplo, anchoEj, ui.letra * 0.78f) + 14;
            p.caja(x + 14, dy, w - 38, eh, 6, ui.tema.fondo, 1);
            p.bordeRedondeado(x + 14, dy, w - 38, eh, 6, ui.tema.borde, 0.7f, 1);
            p.parrafo(ejemplo, x + 23, dy + 7, anchoEj, ui.letra * 0.78f, ui.tema.cadena, 1);
            dy += eh + 10;
        }
        ui.finDesplazable(ficha, dy + desp - inicio);
        if (mostrarInsertar) {
            float by = y + h - 50;
            float bw = Math.min(w - 32, Math.max(120, p.ancho(T.t("api.insertar"), ui.letra, true) + 40));
            if (ui.boton(T.t("api.insertar"), x + 16, by, bw, 42, Ui.Estilo.NORMAL)) {
                editor.doc.insertar(ins);
                busqueda.enfocado = false;
                editor.enfocada = true;
                ui.registrarFoco(editor, () -> editor.enfocada = false);
            }
        }
    }
}
