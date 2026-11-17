package metajavbots.pantallas;

import java.util.function.Supplier;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * MENÚ — El centro del juego: pelear (contra la computadora, al azar, el
 * desafío), el taller del robot, los ejercicios, los tutoriales, el modo
 * especial, el temario, el historial, el panel de la IA y los ajustes.
 */
public final class Menu implements Pantalla {

    private float t;
    private VistaDeArena vista;

    private static final class Casilla {
        final String clave, icono;
        final int color;
        final Supplier<Pantalla> destino;

        Casilla(String clave, String icono, int color, Supplier<Pantalla> destino) {
            this.clave = clave;
            this.icono = icono;
            this.color = color;
            this.destino = destino;
        }
    }

    private static final Casilla[] CASILLAS = {
            new Casilla("menu.pelear", "⚔", 0xFF6B6B, PrepararPelea::new),
            new Casilla("menu.taller", "⚙", 0x3FA9F5, Taller::new),
            new Casilla("menu.ejercicios", "✓", 0x5BD685, Ejercicios::new),
            new Casilla("menu.tutoriales", "★", 0xFFC857, Tutoriales::new),
            new Casilla("menu.especial", "◆", 0xB07CFF, ModoEspecial::new),
            new Casilla("menu.temario", "▤", 0x3FE0D0, Temario::new),
            new Casilla("menu.historial", "▲", 0xFF8FB1, Historial::new),
            new Casilla("menu.panel_ia", "●", 0xE5C07B, PanelIA::new),
            new Casilla("menu.ajustes", "■", 0x8A92A3, Ajustes::new),
    };

    @Override
    public void alEntrar(Juego j) {
        if (vista == null) vista = new VistaDeArena(j.pincel);
        j.guardar();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, "MetajavBots", false);
        Jugador jug = j.jugador;
        // A la izquierda, el robot elegido.
        float iz = 40, ancho = 380;
        ui.panel(iz, arriba + 24, ancho, p.alto - arriba - 60, null);
        Jugador.Robot r = jug.robot();
        if (r != null) {
            vista.robotSuelto(p, r.montaje(), iz + ancho / 2, arriba + 420, 3.6f, t, 0, true);
            p.textoCentrado(r.nombre, iz + ancho / 2, arriba + 450, ui.letra * 1.4f, true, ui.tema.texto, 1);
            p.textoCentrado(r.montaje().modelo.nombre(T.idioma) + " · " + r.montaje().modelo.naturaleza.nombre(T.idioma),
                    iz + ancho / 2, arriba + 490, ui.letra * 0.9f, false, ui.tema.textoSuave, 1);
        }
        p.textoCentrado(T.f("menu.marcador", jug.victorias(), jug.derrotas()), iz + ancho / 2, arriba + 540, ui.letra, false, ui.tema.texto, 1);
        p.textoCentrado(T.f("menu.experiencia", jug.experiencia), iz + ancho / 2, arriba + 575, ui.letra * 0.9f, false, ui.tema.textoSuave, 1);
        if (ui.boton(T.t("menu.pelear_ya"), iz + 30, p.alto - 150, ancho - 60, 64, Ui.Estilo.PRINCIPAL, true, T.t("menu.pelear_ya_ayuda"))) {
            j.ir(PrepararPelea.adaptada(j));
        }
        // A la derecha, las casillas.
        float x0 = iz + ancho + 30, y0 = arriba + 24;
        float w = (p.ancho - x0 - 40 - 2 * 20) / 3, h = (p.alto - y0 - 36 - 2 * 20) / 3;
        for (int i = 0; i < CASILLAS.length; i++) {
            Casilla c = CASILLAS[i];
            float x = x0 + (i % 3) * (w + 20), y = y0 + (i / 3) * (h + 20);
            boolean encima = ui.e.dentro(x, y, w, h);
            p.caja(x, y + 3, w, h, 16, 0x000000, 0.25f);
            p.caja(x, y, w, h, 16, encima ? ui.tema.panelAlto : ui.tema.panel, 1);
            p.bordeRedondeado(x, y, w, h, 16, encima ? c.color : ui.tema.borde, 1, encima ? 2.5f : 1);
            p.circulo(x + 56, y + h / 2, 34, Pincel.oscurecer(c.color, 0.5f), 1);
            p.textoCentrado(c.icono, x + 56, y + h / 2 - 20, 34, true, c.color, 1);
            p.texto(T.t(c.clave), x + 110, y + h / 2 - ui.letra * 1.25f, ui.letra * 1.25f, true, ui.tema.texto, 1);
            p.parrafo(T.t(c.clave + "_ayuda"), x + 110, y + h / 2 + 8, w - 130, ui.letra * 0.78f, ui.tema.textoSuave, 1);
            if (ui.e.clic(x, y, w, h)) {
                j.sonidos.tocar("clic", 0.5f);
                j.ir(c.destino.get());
            }
        }
        if (ui.atras()) {
            j.guardar();
            j.irSolo(new Inicio());
        }
    }
}
