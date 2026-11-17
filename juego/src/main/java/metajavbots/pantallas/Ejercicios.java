package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * EJERCICIOS — El modo de ejercicios, aparte, al estilo CodingBat y
 * CodeStepByStep: los temas de Java por bloques (a la izquierda, con lo que
 * lleva resuelto de cada uno) y los ejercicios del tema elegido, de más
 * fácil a más difícil. Cada uno se abre en PantallaEjercicio.
 */
public final class Ejercicios implements Pantalla {

    private String tema = "01";
    private final Ui.Desplazable temas = new Ui.Desplazable(), lista = new Ui.Desplazable();
    private float t;

    public Ejercicios() {
    }

    public Ejercicios(String tema) {
        this.tema = tema;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("menu.ejercicios"), true);
        float y = arriba + 16, h = p.alto - y - 20;
        // Los temas, por bloques.
        float tw = 470;
        float cy = ui.panel(20, y, tw, h, T.t("ejercicios.temas"));
        float d = ui.desplazable(temas, 28, cy, tw - 16, y + h - cy - 10);
        float yy = cy - d;
        String bloque = "";
        Banco banco = Banco.delJuego();
        for (Tema tm : Tema.todos()) {
            if (!tm.bloque.equals(bloque)) {
                bloque = tm.bloque;
                p.texto(Tema.bloque(bloque, T.idioma).toUpperCase(), 36, yy + 6, ui.letra * 0.7f, true, ui.tema.textoApagado, 1);
                yy += ui.letra * 1.4f;
            }
            List<Ejercicio> es = banco.deTema(tm.id);
            int hechos = 0;
            for (Ejercicio e : es) if (jug.ejerciciosResueltos.contains(e.id)) hechos++;
            boolean elegido = tm.id.equals(tema);
            float fh = 54;
            if (elegido) p.caja(32, yy, tw - 40, fh - 4, 8, ui.tema.seleccion, 1);
            else if (ui.e.dentro(32, yy, tw - 40, fh - 4)) p.caja(32, yy, tw - 40, fh - 4, 8, ui.tema.panelAlto, 1);
            p.texto(tm.id + "  " + ui.recortarTexto(tm.nombre(T.idioma), tw - 160, ui.letra * 0.85f, false), 44, yy + 6, ui.letra * 0.85f, false,
                    ui.tema.texto, 1);
            ui.barra(44, yy + 32, tw - 170, 8, es.isEmpty() ? 0 : (float) hechos / es.size(), ui.tema.bien, null);
            p.textoDerecha(hechos + "/" + es.size(), tw - 30, yy + 24, ui.letra * 0.75f, false, ui.tema.textoSuave, 1);
            if (ui.e.clic(32, yy, tw - 40, fh - 4)) {
                tema = tm.id;
                lista.alPrincipio();
            }
            yy += fh;
        }
        ui.finDesplazable(temas, yy + d - cy);
        // Los ejercicios del tema.
        float lx = 20 + tw + 20, lw = p.ancho - lx - 20;
        Tema elegido = Tema.de(tema);
        float ly = ui.panel(lx, y, lw, h, elegido == null ? "" : elegido.id + " · " + elegido.nombre(T.idioma));
        if (!Compilador.hayCompilador()) {
            p.parrafo(T.t("taller.sin_jdk"), lx + 20, ly, lw - 40, ui.letra * 0.9f, ui.tema.aviso, 1);
            ly += 60;
        }
        float d2 = ui.desplazable(lista, lx + 8, ly, lw - 16, y + h - ly - 10);
        float y2 = ly - d2;
        for (Ejercicio e : banco.deTema(tema)) {
            boolean hecho = jug.ejerciciosResueltos.contains(e.id);
            boolean puede = e.java <= Compilador.versionDeJava();
            float fh = 70;
            boolean encima = ui.e.dentro(lx + 16, y2, lw - 44, fh - 6);
            p.caja(lx + 16, y2, lw - 44, fh - 6, 10, encima ? ui.tema.panelAlto : ui.tema.fondo, 1);
            p.texto(hecho ? "✔" : "○", lx + 32, y2 + 18, ui.letra * 1.2f, true, hecho ? ui.tema.bien : ui.tema.textoApagado, 1);
            p.texto(e.titulo(T.idioma), lx + 72, y2 + 10, ui.letra, true, ui.tema.texto, 1);
            p.texto(ui.recortarTexto(e.enunciado.en(T.idioma), lw - 330, ui.letra * 0.75f, false), lx + 72, y2 + 38, ui.letra * 0.75f, false,
                    ui.tema.textoSuave, 1);
            p.textoDerecha("★".repeat(e.dificultad) + "☆".repeat(5 - e.dificultad), lx + lw - 50, y2 + 10, ui.letra * 0.9f, false, 0xFFC857, 1);
            p.textoDerecha((e.java > 8 ? "Java " + e.java + " · " : "") + T.t("tipo." + e.tipo.name().toLowerCase()), lx + lw - 50, y2 + 38,
                    ui.letra * 0.7f, false, puede ? ui.tema.textoApagado : ui.tema.mal, 1);
            if (puede && ui.e.clic(lx + 16, y2, lw - 44, fh - 6)) j.ir(new PantallaEjercicio(e));
            y2 += fh;
        }
        ui.finDesplazable(lista, y2 + d2 - ly);
    }
}
