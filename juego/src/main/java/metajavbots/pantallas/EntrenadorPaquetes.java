package metajavbots.pantallas;

import java.util.List;
import java.util.function.Consumer;

import metajavbots.Juego;
import metajavbots.codigo.Paquetes;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * ENTRENADOR PAQUETES — Renderiza en el panel derecho del EntrenadorFlotante
 * la explicación sin IA de un paquete o de un componente del catálogo
 * (res/datos/paquetes.txt) y genera su resumen para voz e IA.
 */
final class EntrenadorPaquetes {

    private EntrenadorPaquetes() {
    }

    /** Dibuja los paquetes/componentes encontrados en la lista izquierda del entrenador. Devuelve la nueva y. */
    static float dibujarEnLista(Ui ui, List<Paquetes.Sugerencia> lista, Paquetes.Sugerencia elegida,
            float x, float y, float yy, float w, float h, float alto, Idioma idioma, Consumer<Paquetes.Sugerencia> alElegir) {
        for (Paquetes.Sugerencia s : lista) {
            boolean este = elegida != null && elegida.paquete == s.paquete && elegida.componente == s.componente;
            String rot = "§ " + (s.esPaquete() ? s.paquete.nombre : s.componente.nombre(idioma) + " (" + s.paquete.nombre + ")");
            if (yy + alto > y - 4 && yy < y + h + 4 && ui.boton(rot, x, yy, w - 14, alto - 4, este ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                alElegir.accept(s);
            }
            yy += alto;
        }
        return yy;
    }

    /** Dibuja el paquete o componente elegido dentro del área desplazable del entrenador. Devuelve la altura final. */
    static float dibujar(Juego j, Paquetes.Sugerencia s, float x, float yy, float w, Idioma idioma,
            Consumer<Paquetes.Sugerencia> alElegir) {
        if (s.esPaquete()) return dibujarPaquete(j, s.paquete, x, yy, w, idioma, alElegir);
        return dibujarComponente(j, s.paquete, s.componente, x, yy, w, idioma, alElegir);
    }

    private static float dibujarPaquete(Juego j, Paquetes.Paquete pq, float x, float yy, float w, Idioma idioma,
            Consumer<Paquetes.Sugerencia> alElegir) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float ancho = w - 20;
        p.texto(ui.recortarTexto(pq.nombre, ancho, ui.letra * 1.4f, true), x, yy, ui.letra * 1.4f, true, ui.tema.acento, 1);
        yy += p.altoDeLinea(ui.letra * 1.4f) + 6;
        String impTxt = pq.automatico ? T.t("paquetes.auto") : pq.sentenciaImport;
        yy = bloqueSimple(p, ui, T.t("paquetes.import_etiqueta"), impTxt, x, yy, ancho) + 10;
        for (String par : pq.parrafos(idioma)) {
            yy += p.parrafo(par, x, yy, ancho, ui.letra * 0.95f, ui.tema.texto, 1) + ui.letra * 0.5f;
        }
        String bloq = pq.bloqueado(idioma);
        if (!bloq.isEmpty()) {
            float bh = p.altoDeParrafo(bloq, ancho - 24, ui.letra * 0.82f) + p.altoDeLinea(ui.letra * 0.8f) + 16;
            p.caja(x, yy, ancho, bh, 6, ui.tema.panel, 1);
            p.rect(x, yy + 4, 4, Math.max(0, bh - 8), ui.tema.aviso, 0.95f);
            p.texto(T.t("paquetes.bloqueado_titulo"), x + 14, yy + 6, ui.letra * 0.8f, true, ui.tema.aviso, 1);
            p.parrafo(bloq, x + 14, yy + 10 + p.altoDeLinea(ui.letra * 0.8f), ancho - 24, ui.letra * 0.82f, ui.tema.texto, 1);
            yy += bh + 12;
        }
        for (Paquetes.Tipo tipo : Paquetes.Tipo.values()) {
            List<Paquetes.Componente> grupo = pq.porTipo(tipo);
            if (grupo.isEmpty()) continue;
            p.texto(T.t("paquetes.grupo." + tipo.clave), x, yy, ui.letra * 0.9f, true, ui.tema.textoSuave, 1);
            yy += p.altoDeLinea(ui.letra * 0.9f) + 6;
            for (Paquetes.Componente c : grupo) {
                float bh = Math.max(38, ui.letra * 1.75f);
                String rotulo = c.nombre(idioma) + " — " + c.descripcion(idioma);
                if (ui.boton(rotulo, x, yy, ancho, bh, Ui.Estilo.PLANO)) {
                    alElegir.accept(Paquetes.buscar(pq.nombre + "." + c.nombre(idioma), idioma, 1).get(0));
                }
                yy += bh + 6;
            }
            yy += 6;
        }
        return yy;
    }

    private static float dibujarComponente(Juego j, Paquetes.Paquete pq, Paquetes.Componente c, float x, float yy, float w, Idioma idioma,
            Consumer<Paquetes.Sugerencia> alElegir) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float ancho = w - 20;
        String tipoTxt = T.t("paquetes.tipo." + c.tipo.clave).toUpperCase(idioma.locale());
        yy += p.parrafo(tipoTxt + " · " + pq.nombre, x, yy, ancho, ui.letra * 0.82f, true, ui.tema.textoSuave, 1) + 4;
        p.texto(ui.recortarTexto(c.nombre(idioma), ancho, ui.letra * 1.4f, true), x, yy, ui.letra * 1.4f, true, ui.tema.acento, 1);
        yy += p.altoDeLinea(ui.letra * 1.4f) + 8;
        String imp = pq.automatico ? T.t("paquetes.auto") : "import " + c.nombreCompleto(idioma) + ";";
        yy = bloqueSimple(p, ui, T.t("paquetes.import_etiqueta"), imp, x, yy, ancho) + 10;
        yy += p.parrafo(c.descripcion(idioma), x, yy, ancho, ui.letra * 0.95f, ui.tema.texto, 1) + 12;
        List<String> miembros = c.miembros(idioma);
        if (!miembros.isEmpty()) {
            p.texto(T.t("paquetes.miembros"), x, yy, ui.letra * 0.9f, true, ui.tema.textoSuave, 1);
            yy += p.altoDeLinea(ui.letra * 0.9f) + 6;
            for (String m : miembros) {
                yy += p.parrafo("• " + m, x + 6, yy, ancho - 12, ui.letra * 0.84f, ui.tema.texto, 1) + 6;
            }
            yy += 6;
        }
        String ej = c.ejemplo(idioma);
        if (!ej.isEmpty()) {
            yy = bloqueSimple(p, ui, T.t("paquetes.ejemplo"), ej, x, yy, ancho) + 10;
        }
        float bh = Math.max(38, ui.letra * 1.75f);
        String volver = T.f("paquetes.ver_paquete", pq.nombre);
        float bw = Math.min(ancho, p.ancho(volver, ui.letra, true) + 40);
        if (ui.boton(volver, x, yy, bw, bh, Ui.Estilo.NORMAL)) {
            List<Paquetes.Sugerencia> l = Paquetes.buscar(pq.nombre, idioma, 1);
            if (!l.isEmpty()) alElegir.accept(l.get(0));
        }
        return yy + bh + 12;
    }

    private static float bloqueSimple(Pincel p, Ui ui, String rotulo, String texto, float x, float yy, float ancho) {
        p.texto(rotulo, x, yy, ui.letra * 0.82f, true, ui.tema.textoSuave, 1);
        yy += p.altoDeLinea(ui.letra * 0.82f) + 4;
        float tl = ui.letra * 0.82f;
        float h = p.altoDeParrafo(texto, ancho - 24, tl) + 14;
        p.caja(x, yy, ancho, h, 8, 0x0E1014, 1);
        p.parrafo(texto, x + 12, yy + 7, ancho - 24, tl, ui.tema.cadena, 1);
        return yy + h;
    }

    /** Texto plano para leer en voz alta la ficha del paquete o componente. */
    static String textoParaVoz(Paquetes.Sugerencia s, Idioma idioma) {
        if (s.esPaquete()) return s.paquete.nombre + ". " + s.paquete.descripcion(idioma);
        return s.componente.nombre(idioma) + ". " + s.componente.descripcion(idioma);
    }

    /** Contexto en inglés para la IA cuando el jugador consulta un paquete o componente. */
    static String contextoParaIa(Paquetes.Sugerencia s) {
        if (s.esPaquete()) {
            return "Built-in package documentation for «" + s.paquete.nombre + "» (" + s.paquete.sentenciaImport + "): "
                    + s.paquete.descripcion(Idioma.EN);
        }
        return "Built-in class documentation for «" + s.componente.nombreCompleto(Idioma.EN) + "» (" + s.componente.tipo.clave + "): "
                + s.componente.descripcion(Idioma.EN);
    }
}
