package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;

/**
 * MECÁNICAS — El índice de las fichas de las mecánicas del juego (res/conceptos/
 * 08_mecanicas.txt, id «mec_…»): a la izquierda la lista; a la derecha la ficha
 * elegida, con sus cuatro párrafos (qué es, las cifras, cómo se ve en la pelea y
 * cómo se usa desde el código), la idea en pseudocódigo (nunca Java para copiar)
 * y las fichas relacionadas. Se llega desde el menú, desde «¿Cómo se juega?» de
 * cada modo y desde cada cifra del informe de la ronda.
 */
public final class Mecanicas implements Pantalla {

    private String elegida;
    private final Ui.Desplazable desplLista = new Ui.Desplazable(), desplFicha = new Ui.Desplazable();
    private float t;

    public Mecanicas() {
        this(null);
    }

    /** El índice con esa ficha abierta (o la primera, si no existe). */
    public Mecanicas(String ficha) {
        elegida = ficha;
    }

    /** Todas las fichas de mecánicas, en el orden del archivo. */
    public static List<Concepto> fichas() {
        List<Concepto> l = new ArrayList<>();
        for (Concepto c : Concepto.todos()) if (c.id.startsWith("mec_")) l.add(c);
        return l;
    }

    /** La ficha que se está enseñando. */
    public String elegida() {
        return elegida;
    }

    @Override
    public void alEntrar(Juego j) {
        if (elegida == null || Concepto.de(elegida) == null) elegida = fichas().isEmpty() ? null : fichas().get(0).id;
    }

    @Override
    public String comoSeJuega() {
        return "mecanicas";
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("mecanicas.titulo"), true);
        float x = 24, y = arriba + 16, h = p.alto - y - 20;
        float anchoLista = Math.min(420, Math.max(260, p.ancho * 0.28f));
        lista(j, x, y, anchoLista, h);
        Concepto c = Concepto.de(elegida);
        if (c != null) ficha(j, c, x + anchoLista + 20, y, p.ancho - x - anchoLista - 20 - 24, h);
    }

    private void lista(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        ui.panel(x, y, w, h, null);
        String intro = T.t("mecanicas.intro");
        float tIntro = Math.min(18, ui.letra * 0.8f);
        float altoIntro = p.altoDeParrafo(intro, w - 28, tIntro);
        p.parrafo(intro, x + 14, y + 12, w - 28, tIntro, ui.tema.textoSuave, 1);
        float ly = y + 24 + altoIntro, lh = h - (ly - y) - 10;
        float alto = Math.max(44, ui.letra * 2.1f);
        float d = ui.desplazable(desplLista, x + 6, ly, w - 12, lh);
        float yy = ly - d;
        for (Concepto c : fichas()) {
            boolean es = c.id.equals(elegida);
            String tit = c.titulo.en(T.idioma);
            if (ui.boton(tit, x + 12, yy, w - 36, alto, es ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true, null) && !es) {
                elegida = c.id;
                desplFicha.alPrincipio();
            }
            yy += alto + 6;
        }
        ui.finDesplazable(desplLista, yy + d - ly);
    }

    private void ficha(Juego j, Concepto c, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        ui.panel(x, y, w, h, null);
        float d = ui.desplazable(desplFicha, x + 4, y + 6, w - 8, h - 12);
        float inicio = y + 18 - d, yy = inicio, tx = x + 24, tw = w - 64;
        String titulo = c.titulo.en(T.idioma);
        float tTit = Math.min(34, ui.letra * 1.45f);
        yy += p.parrafo(titulo, tx, yy, tw, tTit, true, ui.tema.acento, 1) + 14;
        float tPar = Math.min(24, ui.letra * 1.0f);
        for (String par : c.parrafos(T.idioma)) {
            int dos = par.indexOf(':');
            if (dos > 0 && dos < 24) {
                String etiqueta = par.substring(0, dos + 1);
                yy += p.parrafo(etiqueta, tx, yy, tw, tPar * 0.82f, true, ui.tema.textoSuave, 1) + 2;
                yy += p.parrafo(par.substring(dos + 1).strip(), tx, yy, tw, tPar, ui.tema.texto, 1) + 14;
            } else {
                yy += p.parrafo(par, tx, yy, tw, tPar, ui.tema.texto, 1) + 14;
            }
        }
        String pseudo = c.pseudo(T.idioma);
        if (!pseudo.isBlank()) {
            yy += p.parrafo(T.t("mecanicas.idea"), tx, yy, tw, tPar * 0.82f, true, ui.tema.textoSuave, 1) + 6;
            float tCod = Math.min(22, ui.letra * 0.92f);
            float altoCod = ComoSeJuega.pseudocodigo(p, pseudo, tx + 14, yy + 12, tw - 28, tCod, 0xE8E2C8, false);
            p.caja(tx, yy, tw, altoCod + 24, 10, 0x1B2430, 1);
            ComoSeJuega.pseudocodigo(p, pseudo, tx + 14, yy + 12, tw - 28, tCod, 0xE8E2C8, true);
            yy += altoCod + 40;
        }
        List<Concepto> rel = new ArrayList<>();
        for (String v : c.ver) {
            Concepto r = Concepto.de(v);
            if (r != null) rel.add(r);
        }
        if (!rel.isEmpty()) {
            yy += p.parrafo(T.t("mecanicas.ver"), tx, yy, tw, tPar * 0.82f, true, ui.tema.textoSuave, 1) + 8;
            float bx = tx, bh = Math.max(40, ui.letra * 1.9f);
            for (Concepto r : rel) {
                String tt = r.titulo.en(T.idioma);
                float bw = Math.min(tw, p.ancho(tt, Math.min(ui.letra, 22), true) + 36);
                if (bx + bw > tx + tw) {
                    bx = tx;
                    yy += bh + 8;
                }
                if (ui.boton(tt, bx, yy, bw, bh, Ui.Estilo.NORMAL, true, null)) abrir(j, r.id);
                bx += bw + 10;
            }
            yy += bh + 12;
        }
        ui.finDesplazable(desplFicha, yy - inicio + 24);
    }

    /** Una ficha de mecánica se abre aquí; un concepto del entrenador, en el entrenador. */
    private void abrir(Juego j, String id) {
        if (id.startsWith("mec_")) {
            elegida = id;
            desplFicha.alPrincipio();
        } else {
            j.entrenadorFlotante.abrir(j, id);
        }
    }
}
