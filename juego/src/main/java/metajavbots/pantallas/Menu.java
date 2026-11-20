package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;
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
 * especial, depuración y testing, el temario, el historial, el panel de la IA
 * y los ajustes.
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
            new Casilla("menu.depuracion", "!", 0xFF9F43, DepuracionYTesting::new),
            new Casilla("menu.temario", "▤", 0x3FE0D0, Temario::new),
            new Casilla("menu.historial", "▲", 0xFF8FB1, Historial::new),
            new Casilla("menu.panel_ia", "●", 0xE5C07B, PanelIA::new),
            new Casilla("menu.ajustes", "■", 0x8A92A3, Ajustes::new),
    };

    /** Claves de las casillas visibles en el menú principal. */
    public static List<String> clavesDeCasillas() {
        List<String> claves = new ArrayList<>(CASILLAS.length);
        for (Casilla c : CASILLAS) claves.add(c.clave);
        return claves;
    }

    @Override
    public void alEntrar(Juego j) {
        if (vista == null) vista = new VistaDeArena(j.pincel);
        j.alarmaBajada.alEntrarAlMenu();
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
        dibujarMonedasCabecera(j, jug);
        // A la izquierda, el robot elegido.
        float iz = 40, ancho = 380;
        ui.panel(iz, arriba + 24, ancho, p.alto - arriba - 60, null);
        // De abajo arriba, para que nada se monte con ninguna letra ni altura de ventana: «¡A pelear!», la fila de los
        // minijuegos y la hora de hoy, el marcador, la experiencia, el robot (lo que quede de hueco) con su nombre.
        float abajo = p.alto - 150;
        if (ui.boton(T.t("menu.pelear_ya"), iz + 30, abajo, ancho - 60, 64, Ui.Estilo.PRINCIPAL, true, T.t("menu.pelear_ya_ayuda"))) {
            j.ir(PrepararPelea.adaptada(j));
        }
        // La hora de hoy (repaso, tema nuevo y pelea), los minijuegos y la tienda, uno encima del otro.
        float fila = abajo - 60;
        if (ui.boton(T.t("sesion.boton"), iz + 30, fila, ancho - 60, 50, Ui.Estilo.NORMAL, true, T.t("sesion.boton_ayuda"))) j.ir(new SesionDeHoy());
        fila -= 58;
        if (ui.boton(T.t("mini.boton"), iz + 30, fila, ancho - 60, 50, Ui.Estilo.NORMAL, true, T.t("mini.boton_ayuda"))) j.ir(new MiniJuegos());
        fila -= 58;
        if (ui.boton("◆ " + T.t("menu.tienda"), iz + 30, fila, ancho - 60, 50, Ui.Estilo.DESTACADO, true, T.t("menu.tienda_ayuda"))) j.ir(new Tienda());
        float alto = p.altoDeLinea(ui.letra);
        float yExp = fila - 12 - alto, yMarcador = yExp - alto * 1.2f;
        String exp = T.f("menu.experiencia", jug.experiencia), marcador = T.f("menu.marcador", jug.victorias(), jug.derrotas());
        p.textoCentrado(exp, iz + ancho / 2, yExp, ui.cabe(exp, ancho - 24, ui.letra * 0.9f, false), false, ui.tema.textoSuave, 1);
        p.textoCentrado(marcador, iz + ancho / 2, yMarcador, ui.cabe(marcador, ancho - 24, ui.letra, false), false, ui.tema.texto, 1);
        Jugador.Robot r = jug.robot();
        if (r != null) {
            float yModelo = yMarcador - alto * 1.6f, yNombre = yModelo - p.altoDeLinea(ui.letra * 1.4f) - 4;
            p.textoCentrado(r.montaje().modelo.nombre(T.idioma) + " · " + r.montaje().modelo.naturaleza.nombre(T.idioma),
                    iz + ancho / 2, yModelo, ui.letra * 0.9f, false, ui.tema.textoSuave, 1);
            p.textoCentrado(r.nombre, iz + ancho / 2, yNombre, ui.cabe(r.nombre, ancho - 24, ui.letra * 1.4f, true), true, ui.tema.texto, 1);
            // El robot, de pie sobre su nombre, tan grande como quepa (a 3,6 mide unos 300 px).
            float pies = yNombre - 24, talla = Math.max(1.2f, Math.min(3.6f, (pies - arriba - 50) / 85f));
            vista.robotSuelto(p, r.montaje(), iz + ancho / 2, pies, talla, t, 0, true);
        }
        // A la derecha, las casillas.
        int cols = 3;
        int filas = (CASILLAS.length + cols - 1) / cols;
        float sep = 16;
        float x0 = iz + ancho + 30, y0 = arriba + 24;
        float w = (p.ancho - x0 - 40 - (cols - 1) * sep) / cols;
        float h = (p.alto - y0 - 36 - (filas - 1) * sep) / filas;
        float radio = Math.min(34, h * 0.32f);
        float tx = x0;
        for (int i = 0; i < CASILLAS.length; i++) {
            Casilla c = CASILLAS[i];
            float x = tx + (i % cols) * (w + sep), y = y0 + (i / cols) * (h + sep);
            boolean encima = ui.e.dentro(x, y, w, h);
            p.caja(x, y + 3, w, h, 16, 0x000000, 0.25f);
            p.caja(x, y, w, h, 16, encima ? ui.tema.panelAlto : ui.tema.panel, 1);
            p.bordeRedondeado(x, y, w, h, 16, encima ? c.color : ui.tema.borde, 1, encima ? 2.5f : 1);
            p.circulo(x + 18 + radio, y + h / 2, radio, Pincel.oscurecer(c.color, 0.5f), 1);
            p.textoCentrado(c.icono, x + 18 + radio, y + h / 2 - radio * 0.58f, radio, true, c.color, 1);
            float margenTexto = 32 + radio * 2;
            // El título y su explicación, a la medida de la casilla (con letra grande no se salen: se encogen).
            float tt = ui.cabe(T.t(c.clave), w - margenTexto - 16, ui.letra * 1.2f, true);
            p.texto(T.t(c.clave), x + margenTexto, y + h / 2 - tt - 4, tt, true, ui.tema.texto, 1);
            float ta = ui.letra * 0.78f;
            while (ta > 10 && p.altoDeParrafo(T.t(c.clave + "_ayuda"), w - margenTexto - 16, ta) > h / 2 - 10) ta -= 1;
            p.parrafo(T.t(c.clave + "_ayuda"), x + margenTexto, y + h / 2 + 4, w - margenTexto - 16, ta, ui.tema.textoSuave, 1);
            if (ui.e.clic(x, y, w, h)) {
                j.sonidos.tocar("clic", 0.5f);
                j.ir(c.destino.get());
            }
        }
        if (ui.atras()) {
            j.guardar();
            j.irSolo(new Inicio());
        }
        j.alarmaBajada.dibujarBannerEnMenu(j, arriba, dt);
    }

    /** Muestra el saldo actual de monedas en la cabecera del menú, respetando letra escalable e idioma. */
    private void dibujarMonedasCabecera(Juego j, Jugador jug) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String texto = "◆ " + T.f("menu.monedas", Jugador.formatearMonedas(jug.monedas));
        float anchoMax = Math.max(120, p.ancho - 460 - 360);
        float tam = ui.cabe(texto, anchoMax - 32, Math.min(ui.letra, 22f), true);
        float mw = Math.min(anchoMax, Math.max(130, p.ancho(texto, tam, true) + 32));
        float mx = p.ancho - 460 - mw - 16, my = 14, mh = 50;
        int dorado = 0xFFC857;
        boolean encima = ui.e.dentro(mx, my, mw, mh);
        p.caja(mx, my, mw, mh, 10, Pincel.oscurecer(dorado, encima ? 0.55f : 0.65f), 1);
        p.bordeRedondeado(mx, my, mw, mh, 10, dorado, 1, encima ? 2.5f : 2);
        p.textoCentrado(texto, mx + mw / 2, my + (mh - p.altoDeLinea(tam)) / 2, tam, true, 0xFFFFFF, 1);
        if (encima) ui.avisar(T.t("menu.monedas_ayuda"));
        if (ui.e.clic(mx, my, mw, mh)) {
            j.sonidos.tocar("clic", 0.5f);
            j.ir(new Tienda());
        }
    }
}
