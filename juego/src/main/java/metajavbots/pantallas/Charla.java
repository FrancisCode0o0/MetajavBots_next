package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.dialogos.Dialogos;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * CHARLA — Hablar con tu robot en el taller. Contesta con su personalidad
 * de fábrica: con cerebro de IA, conversa de verdad (y si le preguntas por
 * Java o por su código, da pistas); sin IA, suelta frases de las suyas.
 */
final class Charla {

    private final CampoDeTexto pregunta = new CampoDeTexto();
    private final List<String[]> mensajes = new ArrayList<>();
    private CerebroDelJuego.Peticion peticion;
    private final Ui.Desplazable despl = new Ui.Desplazable();

    void dibujar(Juego j, Jugador.Robot r, String codigo, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Modelo m = Modelo.de(r.modelo);
        float cy = ui.panel(x, y, w, h, T.f("charla.titulo", r.nombre));
        if (mensajes.isEmpty()) {
            String saludo = Dialogos.de(m.personalidad).frase("espera", T.idioma, java.util.Map.of("yo", r.nombre, "jugador", jug.nombre));
            if (saludo != null) mensajes.add(new String[]{"robot", saludo});
        }
        if (peticion != null && peticion.lista) {
            mensajes.add(new String[]{"robot", peticion.respuesta != null ? peticion.respuesta : T.f("cerebro.error", peticion.error)});
            peticion = null;
            despl.desplazamiento = 1e6f;
        }
        float d = ui.desplazable(despl, x + 10, cy, w - 20, h - (cy - y) - 80);
        float yy = cy - d;
        for (String[] msg : mensajes) {
            boolean mio = msg[0].equals("yo");
            float bw = Math.min(w * 0.7f, p.ancho(msg[1], ui.letra * 0.9f, false) + 30);
            float bh = p.altoDeParrafo(msg[1], bw - 30, ui.letra * 0.9f) + 16;
            float bx = mio ? x + w - bw - 30 : x + 20;
            p.caja(bx, yy, bw, bh, 12, mio ? Pincel.mezcla(ui.tema.panelAlto, ui.tema.acento, 0.3f) : ui.tema.panelAlto, 1);
            p.parrafo(msg[1], bx + 15, yy + 8, bw - 30, ui.letra * 0.9f, ui.tema.texto, 1);
            yy += bh + 10;
        }
        if (peticion != null) yy += p.parrafo(T.t("cerebro.pensando"), x + 20, yy, w - 40, ui.letra * 0.8f, ui.tema.textoApagado, 1);
        ui.finDesplazable(despl, yy + d - cy);
        boolean intro = pregunta.dibujar(ui, x + 16, y + h - 64, w - 32, 48, T.t("charla.escribe"));
        if (intro && !pregunta.texto().isBlank() && peticion == null) {
            String q = pregunta.texto().strip();
            mensajes.add(new String[]{"yo", q});
            pregunta.ponerTexto("");
            Cerebro c = CerebroDelJuego.rapido(jug);
            if (c == null) {
                String[] eventos = {"burla", "elogio", "espera", "saludo"};
                String f = Dialogos.de(m.personalidad).frase(eventos[(int) (Math.random() * eventos.length)], T.idioma,
                        java.util.Map.of("yo", r.nombre, "rival", "?", "jugador", jug.nombre));
                mensajes.add(new String[]{"robot", f == null ? "…" : f});
            } else {
                List<Cerebro.Mensaje> conv = new ArrayList<>();
                conv.add(new Cerebro.Mensaje("user", "My robot code:\n" + codigo));
                conv.add(new Cerebro.Mensaje("assistant", "OK."));
                for (String[] msg : mensajes) conv.add(new Cerebro.Mensaje(msg[0].equals("yo") ? "user" : "assistant", msg[1]));
                peticion = CerebroDelJuego.Peticion.lanzar(c, CerebroDelJuego.sistemaRobot(jug.idioma, r.nombre, m.personalidad, m.lema.en), conv, 70);
            }
            despl.desplazamiento = 1e6f;
        }
    }
}
