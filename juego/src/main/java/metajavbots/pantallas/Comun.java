package metajavbots.pantallas;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import metajavbots.Juego;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** COMÚN — Lo que comparten las pantallas: el fondo, la cabecera con la etiqueta, el botón de volver, las plantillas. */
public final class Comun {

    private Comun() {
    }

    /** El fondo de todas las pantallas: oscuro, con una rejilla tenue y un brillo que respira. */
    public static void fondo(Juego j, float t) {
        Pincel p = j.pincel;
        Tema tema = j.ui.tema;
        p.degradado(0, 0, p.ancho, p.alto, tema.fondo, Pincel.oscurecer(tema.fondo, tema == Tema.OSCURO ? 0.35f : 0.06f), 1);
        int rejilla = Pincel.mezcla(tema.fondo, tema.acento, 0.12f);
        for (float x = (t * 8) % 60; x < p.ancho; x += 60) p.rect(x, 0, 1, p.alto, rejilla, 0.5f);
        for (float y = (t * 5) % 60; y < p.alto; y += 60) p.rect(0, y, p.ancho, 1, rejilla, 0.5f);
        p.brillo(p.ancho * 0.85f, p.alto * 0.1f, 220, tema.acento, 0.05f + 0.03f * (float) Math.sin(t));
    }

    /** La cabecera: el título de la pantalla, la etiqueta del jugador y su barra hacia la siguiente. Devuelve el alto. */
    public static float cabecera(Juego j, String titulo, boolean conVolver) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = 78;
        p.rect(0, 0, p.ancho, h, ui.tema.panel, 0.96f);
        p.rect(0, h - 1, p.ancho, 1, ui.tema.borde, 1);
        float x = 24;
        if (conVolver) {
            if (botonAtras(j)) j.volver();
            x = 40 + anchoDeAtras(j);
        }
        Jugador jug = j.jugador;
        // El título no se mete debajo de la insignia de la etiqueta (con letra grande, se encoge).
        p.texto(titulo, x, 20, ui.cabe(titulo, (jug != null ? p.ancho - 460 : p.ancho) - x - 20, ui.letra * 1.6f, true), true, ui.tema.texto, 1);
        if (jug != null) {
            float ex = p.ancho - 460;
            insignia(p, ui, jug.etiqueta, ex, 14, 1f);
            Progreso.Requisitos r = Progreso.requisitos(jug);
            float parte = jug.etiqueta == Etiqueta.MASTER ? 1 : (float) (0.5 * Math.min(1, (double) r.victoriasQueTiene / Math.max(1, r.victorias))
                    + 0.5 * (r.ejercicios == 0 ? 1 : Math.min(1, (double) r.ejerciciosQueTiene / r.ejercicios)));
            String texto = jug.etiqueta == Etiqueta.MASTER ? "MASTER" : T.f("comun.progreso", r.victoriasQueTiene, r.victorias, r.ejerciciosQueTiene,
                    r.ejercicios);
            ui.barra(ex + 150, 22, 290, 20, parte, ui.tema.acento, null);
            p.texto(texto, ex + 150, 46, ui.cabe(texto, 296, ui.letra * 0.72f, false), false, ui.tema.textoSuave, 1);
            if (ui.e.dentro(ex, 10, 450, 60)) ui.avisar(T.f("comun.progreso_ayuda", jug.etiqueta.nombre, jug.etiqueta.siguiente().nombre));
        }
        return h;
    }

    /** El ancho del botón «← Volver» (el alemán es más largo). */
    static float anchoDeAtras(Juego j) {
        return Math.max(150, j.pincel.ancho("← " + T.t("comun.volver"), Math.min(j.ui.letra, 23), true) + 32);
    }

    /** El botón «← Volver» arriba a la izquierda, siempre a la vista; también vale Esc. ¿Se ha pulsado? */
    public static boolean botonAtras(Juego j) {
        Ui ui = j.ui;
        boolean b = ui.boton("← " + T.t("comun.volver"), 20, 16, anchoDeAtras(j), 46, Ui.Estilo.NORMAL, true, T.t("comun.volver_ayuda"));
        return b || ui.atras();
    }

    /** La insignia de una etiqueta: un escudo de color con su nombre. */
    public static void insignia(Pincel p, Ui ui, Etiqueta e, float x, float y, float escala) {
        int color;
        switch (e.categoria()) {
            case JUNIOR: color = 0x3FA9F5; break;
            case SENIOR: color = 0xB07CFF; break;
            case MASTER: color = 0xFFC857; break;
            default: color = 0x5BD685; break;
        }
        float w = 136 * escala, h = 50 * escala;
        p.caja(x, y, w, h, 10 * escala, Pincel.oscurecer(color, 0.55f), 1);
        p.bordeRedondeado(x, y, w, h, 10 * escala, color, 1, 2 * escala);
        for (int i = 0; i < e.escalon() && e != Etiqueta.MASTER; i++) p.circulo(x + w - 14 * escala - i * 12 * escala, y + h - 11 * escala, 4 * escala, color,
                1);
        p.textoCentrado(e.nombre, x + w / 2, y + 8 * escala, 20 * escala, true, 0xFFFFFF, 1);
    }

    /** El código con que empieza un robot nuevo, en el idioma del jugador. */
    public static String plantilla(Idioma i) {
        String nombre = "/plantillas/robot_" + i.codigo() + ".java";
        try (InputStream in = Comun.class.getResourceAsStream(nombre)) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // sigue abajo
        }
        return "import metajavbots.api.*;\n\npublic class MiRobot extends Metajavbot {\n}\n";
    }

    /** Una pantalla de «aún no hecho» no debería verse nunca; por si acaso, se vuelve. */
    public static void pie(Juego j, String texto) {
        Pincel p = j.pincel;
        p.textoCentrado(texto, p.ancho / 2, p.alto - 34, j.ui.letra * 0.8f, false, j.ui.tema.textoApagado, 1);
    }
}
