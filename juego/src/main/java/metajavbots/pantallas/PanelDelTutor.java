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
 * metas («que vaya más rápido»…) y, de menos a más, una pregunta para pensar,
 * una pista, el pseudocódigo y un trozo pequeño de código; nunca la solución
 * completa ni el programa entero, y nunca se queda callado: al acabar las
 * ayudas de una meta, anima y propone la siguiente (Fran, 18-nov). Con IA,
 * además, se le puede preguntar, con la misma regla.
 */
final class PanelDelTutor {

    private final CampoDeTexto pregunta = new CampoDeTexto();
    private int meta, nivel = -1, frase;
    /** Las veces seguidas que no ha compilado (si se atasca, el entrenador ayuda más). */
    private int fallosSeguidos;
    private String dice = "";
    private float hablando, t;
    private CerebroDelJuego.Peticion peticion;

    /** Dice algo: la viñeta, la boca que se mueve y la voz. */
    private void decir(Juego j, String texto) {
        dice = texto;
        hablando = Math.min(6, 0.6f + texto.length() * 0.045f);
        j.voz.decir(texto.replace("\n", " "), j.jugador.idioma, Entrenador.de(j.jugador.entrenador).timbre);
    }

    /** Lo que dijo el compilador del taller (para notar si el jugador se atasca). */
    void compilado(boolean bien) {
        fallosSeguidos = bien ? 0 : fallosSeguidos + 1;
    }

    /**
     * Cuánto ayuda ahora, de 0 a 1, como un tutor de verdad: lo que pide el jugador (Ajustes, «dame más / dame menos»),
     * menos si ya sabe más (su etiqueta) y más si se atasca (compilaciones fallidas seguidas).
     */
    float ayuda(Jugador jug) {
        return ayuda(jug, fallosSeguidos);
    }

    /** Lo mismo, para quien no lleva la cuenta de fallos (el entrenador flotante usa la misma barra «Dame menos / Dame más»). */
    static float ayuda(Jugador jug, int fallosSeguidos) {
        float sabe = jug.etiqueta.ordinal() / (float) (metajavbots.progreso.Etiqueta.values().length - 1);
        // Con el ajuste a la mitad: un Noob, 0,8 (trozos enseguida); un Junior, ~0,55; un Senior, ~0,35; un Master, 0,2 (una pregunta).
        return Math.max(0, Math.min(1, jug.ayudaDelTutor + 0.3f - sabe * 0.6f + Math.min(3, fallosSeguidos) * 0.12f));
    }

    /** El primer nivel de ayuda que da: con mucha ayuda se salta la pregunta y va a la pista. */
    private int primerNivel(Jugador jug) {
        return ayuda(jug) > 0.75f ? 1 : 0;
    }

    /** Hasta dónde llega: con muy poca ayuda, solo la pregunta; luego la pista; luego el pseudocódigo; con bastante, el trocito de código. */
    private int ultimoNivel(Jugador jug) {
        float a = ayuda(jug);
        return a < 0.25f ? 0 : a < 0.45f ? 1 : a < 0.6f ? 2 : Consejo.NIVELES - 1;
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
        // Su viñeta, a la derecha de la cabeza, con la punta hacia la boca (sin pisar los controles ni el robot de abajo).
        float bx = x + 2 * r + 22, bw = w - (bx - x), tl = ui.letra * 0.82f;
        String texto = peticion != null ? T.t("cerebro.pensando") : dice;
        float bhMax = Math.max(92, Math.min(h * 0.45f, h - 256));
        while (tl > Pincel.LETRA_MINIMA && p.altoDeParrafo(texto, bw - 20, tl) + 16 > bhMax) tl = Math.max(Pincel.LETRA_MINIMA, tl - 1);
        float bh = Math.min(bhMax, p.altoDeParrafo(texto, bw - 20, tl) + 16);
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

    /** La meta (◄ ►), el botón de la siguiente ayuda y «Dame menos / Dame más». Devuelve dónde acaba. */
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
        // Ayudas de menos a más; nunca el programa entero. Al acabarlas, anima y pasa a otra meta (nunca deja tirado).
        String[] botones = {T.t("tutor.ayudame"), T.t("tutor.otra_pista"), T.t("tutor.pseudo"), T.t("tutor.trozo"), T.t("tutor.otra_meta")};
        Jugador jug = j.jugador;
        int siguiente = nivel < 0 ? primerNivel(jug) : nivel + 1;
        // El botón dice lo que dará: la primera vez «Ayúdame»; luego la pista, el pseudocódigo, el trocito… y al final, otra meta.
        String boton = nivel < 0 ? botones[0] : siguiente > ultimoNivel(jug) ? botones[botones.length - 1] : botones[siguiente];
        if (ui.boton(boton, x, y, w, 44, Ui.Estilo.NORMAL, true, T.t("tutor.ayuda"))) {
            Entrenador e = Entrenador.de(j.jugador.entrenador);
            if (siguiente > ultimoNivel(jug)) {
                elegirMeta(j, meta + 1);
                // Si se quedó corto por el nivel, recuerda que puede pedir más: nunca deja al jugador tirado.
                String mas = siguiente < Consejo.NIVELES ? " " + T.t("tutor.si_atascas") : "";
                decir(j, T.t("tutor.animo") + mas + " " + e.frase(j.jugador.idioma, frase++) + " " + dice);
            } else {
                nivel = siguiente;
                String texto = c.nivel(nivel, j.jugador.idioma);
                String antes = nivel == 2 ? T.t("tutor.en_pseudo") + "\n" : nivel == 3 ? T.t("tutor.un_trozo") + "\n" : "";
                decir(j, antes + texto + (nivel == 1 ? " " + e.frase(j.jugador.idioma, frase++) : ""));
            }
        }
        y += 54;
        // «Dame menos» y «Dame más»: el jugador regula cuánto le ayuda (se guarda, como en Ajustes).
        float mitad = (w - 8) / 2;
        if (ui.boton(T.t("tutor.menos"), x, y, mitad, 40, Ui.Estilo.PLANO, jug.ayudaDelTutor > 0, T.t("tutor.menos_ayuda"))) {
            jug.ayudaDelTutor = Math.max(0, jug.ayudaDelTutor - 0.2f);
            decir(j, T.t("tutor.vale_menos"));
        }
        if (ui.boton(T.t("tutor.mas"), x + mitad + 8, y, mitad, 40, Ui.Estilo.PLANO, jug.ayudaDelTutor < 1, T.t("tutor.mas_ayuda"))) {
            jug.ayudaDelTutor = Math.min(1, jug.ayudaDelTutor + 0.2f);
            decir(j, T.t("tutor.vale_mas"));
        }
        return y + 50;
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
                + " Speak as " + e.nombre.en + ", " + e.estilo + ". Coach for real: hints, advice, guiding questions, pseudocode and SMALL code "
                + "snippets (a few lines); never the complete solution nor the whole program, even if asked. Never refuse to help and never "
                + "leave the learner stuck: always end with a concrete next step. Never be rude. Help level " + Math.round(ayuda(j.jugador) * 10)
                + "/10: at low levels only questions and small hints; at high levels pseudocode and a short snippet.";
        peticion = CerebroDelJuego.Peticion.lanzar(c, sistema, CerebroDelJuego.uno(q), 110);
    }

    private void atenderPeticion(Juego j) {
        CerebroDelJuego.Peticion pe = peticion;
        if (pe == null || !pe.lista) return;
        peticion = null;
        decir(j, pe.respuesta != null && !pe.respuesta.isBlank() ? pe.respuesta : T.f("cerebro.error", pe.error));
    }
}
