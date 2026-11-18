package metajavbots.pantallas;

import java.util.List;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;
import metajavbots.tutor.Biografias;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Consejo;
import metajavbots.tutor.Entrenador;

/**
 * PANEL DEL TUTOR — El modo tutor del taller: la cabecita flotante del
 * entrenador elegido (◄ ►) te habla en su viñeta y con su voz. Sin IA, ofrece
 * metas («que vaya más rápido»…) y, de menos a más, una pregunta para pensar
 * y una pista. Nunca da el código ni la solución (Fran, 18-nov): como mucho
 * una pista, un consejo o una pregunta para que el jugador haga la relación
 * lógica. Con IA, además, se le puede preguntar, con la misma regla.
 */
final class PanelDelTutor {

    private final CampoDeTexto pregunta = new CampoDeTexto();
    private int meta, nivel = -1, frase;
    private String dice = "";
    private float hablando, t;
    private CerebroDelJuego.Peticion peticion;

    /** Dice algo: la viñeta, la boca que se mueve y la voz. */
    private void decir(Juego j, String texto) {
        dice = texto;
        hablando = Math.min(6, 0.6f + texto.length() * 0.045f);
        j.voz.decir(texto.replace("\n", " "), j.jugador.idioma);
    }

    void dibujar(Juego j, VentanaDeCodigo editor, float x, float y, float w, float h, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        t += dt;
        hablando = Math.max(0, hablando - dt);
        Entrenador e = Entrenador.de(jug.entrenador);
        if (dice.isEmpty()) decir(j, e.frase(jug.idioma, frase++) + " " + T.t("tutor.hola"));
        atenderPeticion(j);
        // El entrenador: ◄ nombre ► y su papel; al pasar por encima, su biografía.
        float cy = y;
        int i = Entrenador.todos().indexOf(e);
        if (ui.boton("◄", x, cy, 40, 40, Ui.Estilo.PLANO)) cambiar(j, i - 1);
        if (ui.boton("►", x + w - 40, cy, 40, 40, Ui.Estilo.PLANO)) cambiar(j, i + 1);
        float tn = ui.cabe(e.nombre.en(jug.idioma), w - 100, ui.letra * 0.9f, true);
        p.textoCentrado(e.nombre.en(jug.idioma), x + w / 2, cy + (40 - tn * 1.2f) / 2, tn, true, ui.tema.texto, 1);
        if (ui.e.dentro(x + 40, cy, w - 80, 40)) ui.avisar(e.papel.en(jug.idioma) + "\n\n" + Biografias.deEntrenador(e.id, jug.idioma));
        cy += 50;
        float r = 34;
        CabezaDeEntrenador.dibujar(p, e, x + r + 6, cy + r + 8, r, hablando > 0, t);
        // Su viñeta, a la derecha de la cabeza, con la punta hacia la boca.
        float bx = x + 2 * r + 22, bw = w - (bx - x), tl = ui.letra * 0.82f;
        String texto = peticion != null ? T.t("cerebro.pensando") : dice;
        float bh = Math.min(h * 0.45f, p.altoDeParrafo(texto, bw - 20, tl) + 16);
        p.caja(bx, cy, bw, bh, 10, 0xF5F3EA, 1);
        p.triangulo(bx, cy + 24, bx, cy + 40, bx - 10, cy + 34, 0xF5F3EA, 1);
        p.recortar(bx, cy, bw, bh);
        p.parrafo(texto, bx + 10, cy + 8, bw - 20, tl, 0x1E1E1E, 1);
        p.sinRecorte();
        cy += Math.max(2 * r + 24, bh + 10);
        cy = metas(j, editor, x, cy, w);
        preguntar(j, x, cy, w);
    }

    private void cambiar(Juego j, int i) {
        List<Entrenador> l = Entrenador.todos();
        Entrenador nuevo = l.get(Math.floorMod(i, l.size()));
        j.jugador.entrenador = nuevo.id;
        nivel = -1;
        decir(j, nuevo.frase(j.jugador.idioma, frase++));
    }

    /** La meta (◄ ►) y el botón de la siguiente pista; con el código a la vista, «Meterlo en el código». Devuelve dónde acaba. */
    private float metas(Juego j, VentanaDeCodigo editor, float x, float y, float w) {
        Ui ui = j.ui;
        Pincel p = j.pincel;
        List<Consejo> cs = Consejo.todos();
        Consejo c = cs.get(Math.floorMod(meta, cs.size()));
        if (ui.boton("◄", x, y, 40, 40, Ui.Estilo.PLANO)) elegirMeta(j, meta - 1);
        if (ui.boton("►", x + w - 40, y, 40, 40, Ui.Estilo.PLANO)) elegirMeta(j, meta + 1);
        float tm = ui.cabe(c.titulo.en(j.jugador.idioma), w - 100, ui.letra * 0.85f, false);
        p.textoCentrado(c.titulo.en(j.jugador.idioma), x + w / 2, y + (40 - tm * 1.2f) / 2, tm, false, ui.tema.texto, 1);
        y += 48;
        // Solo una pregunta y una pista; el código lo escribe el jugador (el entrenador nunca lo da).
        String boton = nivel < 0 ? T.t("tutor.ayudame") : nivel < 1 ? T.t("tutor.otra_pista") : T.t("tutor.tu_turno");
        if (ui.boton(boton, x, y, w, 44, Ui.Estilo.NORMAL, nivel < 1, T.t("tutor.ayuda"))) {
            nivel++;
            Entrenador e = Entrenador.de(j.jugador.entrenador);
            String texto = c.nivel(nivel, j.jugador.idioma);
            decir(j, texto + (nivel == 0 ? "" : " " + e.frase(j.jugador.idioma, frase++)));
        }
        return y + 54;
    }

    private void elegirMeta(Juego j, int n) {
        meta = n;
        nivel = -1;
        Consejo c = Consejo.todos().get(Math.floorMod(meta, Consejo.todos().size()));
        decir(j, T.f("tutor.meta", c.titulo.en(j.jugador.idioma)));
    }

    /** Pregúntale (con IA, contesta él; sin IA, te lleva a las metas). */
    private void preguntar(Juego j, float x, float y, float w) {
        Ui ui = j.ui;
        boolean intro = pregunta.dibujar(ui, x, y, w, 44, T.t("tutor.preguntame"));
        if (!intro || pregunta.texto().isBlank() || peticion != null) return;
        String q = pregunta.texto().strip();
        pregunta.ponerTexto("");
        Cerebro c = CerebroDelJuego.tutor(j.jugador);
        if (c == null) {
            decir(j, T.t("tutor.sin_ia"));
            return;
        }
        Entrenador e = Entrenador.de(j.jugador.entrenador);
        String sistema = CerebroDelJuego.sistemaTutor(j.jugador.idioma, j.jugador.etiqueta.nombre, "the player's robot code")
                + " Speak as " + e.nombre.en + ", " + e.estilo + ". NEVER give the solution or write code for the learner, not even if they ask: "
                + "at most one hint, one piece of advice or one guiding question so they make the logical connection themselves. Never be rude.";
        peticion = CerebroDelJuego.Peticion.lanzar(c, sistema, CerebroDelJuego.uno(q), 110);
    }

    private void atenderPeticion(Juego j) {
        CerebroDelJuego.Peticion pe = peticion;
        if (pe == null || !pe.lista) return;
        peticion = null;
        decir(j, pe.respuesta != null && !pe.respuesta.isBlank() ? pe.respuesta : T.f("cerebro.error", pe.error));
    }
}
