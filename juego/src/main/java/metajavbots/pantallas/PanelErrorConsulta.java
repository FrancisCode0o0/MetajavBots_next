package metajavbots.pantallas;

import java.util.List;
import java.util.function.Consumer;

import metajavbots.Juego;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;

/**
 * Dibuja los bloques de código/pseudocódigo y la explicación paso a paso de un
 * error de compilación dentro de la ventana de consulta del entrenador flotante.
 */
final class PanelErrorConsulta {

    private static final String[] PASOS = {"entrenador.error.pelea", "entrenador.error.pseudo",
        "entrenador.error.ejemplo", "entrenador.error.arreglo"};

    private PanelErrorConsulta() {
    }

    /** Dibuja un recuadro de código Java (con comentarios en gris) o pseudocódigo. Devuelve dónde acaba. */
    static float bloqueDeCodigo(Juego j, String rotulo, String texto, boolean java, float x, float yy, float w) {
        if (texto == null || texto.isBlank()) return yy;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.texto(rotulo, x, yy, ui.letra * 0.85f, true, ui.tema.textoSuave, 1);
        yy += ui.letra * 1.4f;
        String[] lineas = texto.split("\n");
        float tl = ui.letra * 0.85f, alto = p.altoDeLinea(tl);
        p.caja(x, yy, w - 20, lineas.length * alto + 16, 8, java ? 0x0E1014 : 0x1B2430, 1);
        for (int i = 0; i < lineas.length; i++) {
            String s = lineas[i];
            int com = java ? s.indexOf("//") : -1;
            float lx = x + 12, ly = yy + 8 + i * alto;
            if (com >= 0) {
                String antes = s.substring(0, com);
                p.textoDeCodigo(antes, lx, ly, tl, false, ui.tema.cadena, 1);
                lx += p.anchoDeCodigo(antes, tl, false);
                s = s.substring(com);
            }
            p.textoDeCodigo(ui.recortarTextoDeCodigo(s, x + w - 32 - lx, tl, false), lx, ly, tl, false,
                    com >= 0 ? 0x7F8C8D : java ? ui.tema.cadena : 0xE8E2C8, 1);
        }
        return yy + lineas.length * alto + 28;
    }

    /** Dibuja el error marcado del editor y los pasos pedidos por el jugador. */
    static float explicarError(Juego j, VentanaDeCodigo.Senalado error, List<Integer> pasosDelError,
            Concepto c, float x, float yy, float w, Idioma idioma, Consumer<String> hablar) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        VentanaDeCodigo.Marca m = error.marca();
        p.texto(T.f("entrenador.error.titulo", m.linea), x, yy, ui.letra, true, ui.tema.mal, 1);
        yy += ui.letra * 1.6f;
        float tl = ui.letra * 0.9f, letra = p.anchoDeLetra(tl), alto = p.altoDeLinea(tl);
        String linea = error.linea() == null ? "" : error.linea().replace("\t", "    ");
        int sangria = linea.length() - linea.stripLeading().length();
        String visible = linea.strip();
        p.caja(x, yy, w - 20, alto + 22, 8, 0x0E1014, 1);
        p.textoDeCodigo(ui.recortarTextoDeCodigo(m.linea + "  " + visible, w - 44, tl, false), x + 12, yy + 8, tl, false, ui.tema.cadena, 1);
        int col = Math.max(0, m.columna - 1 - sangria) + (m.linea + "  ").length();
        float ox = x + 12 + col * letra;
        for (int i = 0; i < 5 && ox + i * letra * 0.5f < x + w - 30; i++) {
            float a = ox + i * letra * 0.5f;
            p.linea(a, yy + alto + 12, a + letra * 0.25f, yy + alto + 8 + (i % 2) * 6, 2, ui.tema.mal, 1);
        }
        yy += alto + 34;
        String dice = m.texto.split("\n\njavac:")[0];
        yy += p.parrafo(dice, x, yy, w - 20, ui.letra, ui.tema.texto, 1) + ui.letra * 0.5f;
        if (m.mensaje != null && !m.mensaje.isBlank()) {
            yy += p.parrafo("javac: " + m.mensaje, x, yy, w - 20, ui.letra * 0.8f, ui.tema.textoApagado, 1) + ui.letra * 0.6f;
        }
        yy += p.parrafo(T.f("entrenador.error.concepto", c.titulo.en(idioma)), x, yy, w - 20, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 8;
        float bw = (w - 20 - 24) / 4;
        for (int i = 0; i < PASOS.length; i++) {
            boolean visto = pasosDelError.contains(i);
            if (ui.boton(T.t(PASOS[i]), x + i * (bw + 8), yy, bw, 44, visto ? Ui.Estilo.ELEGIDO : Ui.Estilo.NORMAL) && !visto) {
                pasosDelError.add(i);
                hablar.accept(textoDelPaso(j, error, i, c, idioma));
            }
        }
        yy += 58;
        for (int i : pasosDelError) {
            p.texto(T.t(PASOS[i]), x, yy, ui.letra * 0.85f, true, ui.tema.acento, 1);
            yy += ui.letra * 1.4f;
            if (i == 1) yy = bloqueDeCodigo(j, "", c.pseudo(idioma), false, x, yy - ui.letra * 1.4f, w);
            else if (i == 2) yy = bloqueDeCodigo(j, "", c.ejemplo(idioma), true, x, yy - ui.letra * 1.4f, w);
            else yy += p.parrafo(textoDelPaso(j, error, i, c, idioma), x, yy, w - 20, ui.letra, ui.tema.texto, 1) + ui.letra * 0.8f;
        }
        p.linea(x, yy, x + w - 20, yy, 1, ui.tema.textoApagado, 0.5f);
        return yy + 12;
    }

    private static String textoDelPaso(Juego j, VentanaDeCodigo.Senalado error, int paso, Concepto c, Idioma idioma) {
        VentanaDeCodigo.Marca m = error.marca();
        if (paso == 0) {
            List<String> ps = c.parrafos(idioma);
            return ps.size() > 1 ? ps.get(1) : ps.isEmpty() ? "" : ps.get(0);
        }
        if (paso == 1) return c.pseudo(idioma);
        if (paso == 2) return T.t("entrenador.ejemplo");
        boolean poca = PanelDelTutor.ayuda(j.jugador, 0) < 0.45f;
        String s = Explicador.enNivel(m.codigo, m.mensaje, m.linea, poca ? "junior" : "noob", idioma);
        return s == null ? T.t("entrenador.error.sin_pista") : poca ? T.t("entrenador.error.piensa") + " " + s : s;
    }
}
