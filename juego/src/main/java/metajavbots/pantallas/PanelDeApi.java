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

    public void buscar(String palabra) {
        busqueda.ponerTexto(palabra == null ? "" : palabra);
        busqueda.enfocado = true;
        tema = null;
        elegida = 0;
    }

    public void dibujar(Ui ui, Idioma idioma, VentanaDeCodigo editor, float x, float y, float w, float h) {
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
        }
        float ly = y + 148, lh = (h - 160) * 0.45f;
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
                }
            }
            yy += alto;
        }
        ui.finDesplazable(lista, resultados.size() * alto);
        if (resultados.isEmpty()) p.texto(T.t("api.nada"), x + 16, ly + 8, ui.letra * 0.85f, false, ui.tema.textoApagado, 1);
        // La entrada elegida.
        float dy = ly + lh + 12;
        p.rect(x + 12, dy - 6, w - 24, 1, ui.tema.borde, 1);
        if (elegida < resultados.size()) {
            // La ficha de la entrada, que se desplaza si es larga (nada se sale del panel).
            float zona = y + h - 8 - dy;
            float desp = ui.desplazable(ficha, x + 4, dy, w - 8, zona);
            float inicio = dy;
            dy -= desp;
            Referencia.Entrada e = resultados.get(elegida);
            dy += p.parrafo(e.firma(idioma), x + 16, dy, w - 44, ui.letra * 0.9f, ui.tema.metodo, 1) + 6;
            Tema t = Tema.de(e.tema);
            if (t != null) dy += p.parrafo(T.f("api.tema", t.id, t.nombre(idioma)), x + 16, dy, w - 44, ui.letra * 0.78f, ui.tema.textoSuave, 1) + 6;
            dy += p.parrafo(e.texto(idioma), x + 16, dy, w - 44, ui.letra * 0.85f, ui.tema.texto, 1) + 8;
            if (!e.ejemplo.isEmpty()) {
                float eh = p.altoDeParrafo(e.ejemplo, w - 60, ui.letra * 0.8f) + 12;
                p.caja(x + 14, dy, w - 40, eh, 6, ui.tema.fondo, 1);
                p.parrafo(e.ejemplo, x + 24, dy + 6, w - 60, ui.letra * 0.8f, ui.tema.cadena, 1);
                dy += eh + 8;
            }
            String ins = e.insertable(idioma);
            if (editor != null && !editor.soloLectura && !ins.isEmpty()) {
                if (ui.boton(T.t("api.insertar"), x + 16, dy, Math.min(w - 44, p.ancho(T.t("api.insertar"), ui.letra, true) + 40), 44, Ui.Estilo.NORMAL)) {
                    editor.doc.insertar(ins);
                    editor.enfocada = true;
                }
                dy += 52;
            }
            ui.finDesplazable(ficha, dy + desp - inicio);
        }
    }
}
