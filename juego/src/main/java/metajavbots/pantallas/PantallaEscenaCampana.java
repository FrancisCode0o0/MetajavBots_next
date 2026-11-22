package metajavbots.pantallas;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.campanas.Campana;
import metajavbots.campanas.EscenaDeCampana;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;
import metajavbots.tutor.Entrenador;

/**
 * PANTALLA ESCENA CAMPAÑA — Muestra la secuencia de viñetas ilustradas de un
 * capítulo de campaña. Se avanza con clic, Espacio o Enter y se salta al
 * instante con Esc o el botón «Saltar escena», sin bloquear el hilo gráfico
 * ni dejar hilos o recursos colgados al salir.
 */
public final class PantallaEscenaCampana implements Pantalla {

    private final Campana campana;
    private final Campana.Capitulo capitulo;
    private final EscenaDeCampana escena;
    private final EscenaDeCampana.Controlador controlador;
    private final Runnable alTerminar;
    private final boolean irAPelea;
    private VistaDeArena vista;
    private boolean finalEjecutado;
    private float t;

    /** Crea la pantalla de escena ilustrada para un capítulo. */
    public PantallaEscenaCampana(Campana campana, Campana.Capitulo capitulo, EscenaDeCampana escena,
                                 boolean irAPelea, Runnable alTerminar) {
        this.campana = campana;
        this.capitulo = capitulo;
        this.escena = escena;
        this.controlador = escena == null ? new EscenaDeCampana.Controlador(null) : escena.controlador();
        this.irAPelea = irAPelea;
        this.alTerminar = alTerminar;
    }

    @Override
    public String musica() {
        return campana == null ? "menu" : campana.musica;
    }

    @Override
    public void alEntrar(Juego j) {
        vista = j != null && j.pincel != null ? new VistaDeArena(j.pincel) : null;
        finalEjecutado = false;
        t = 0f;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        t += dt;
        controlador.avanzarTiempo(dt);
        if (controlador.terminada()) {
            cerrar(j);
            return;
        }
        Comun.fondo(j, t);
        String tituloCab = campana.titulo.en(jug.idioma) + "  ·  " + T.f("campanas.capitulo", capitulo.numero);
        float arriba = Comun.cabecera(j, tituloCab, true) + 12;
        float barraH = Math.max(66, ui.letra * 2.8f);
        float abajo = p.alto - barraH - 16;

        String sub = escena.titulo.en(jug.idioma) + "  [" + capitulo.rango + "]  ·  "
                + T.f("campanas.escena_contador", controlador.indice() + 1, controlador.total());
        float tamSub = ui.cabe(sub, p.ancho - 60, ui.letra * 1.05f, true);
        p.textoCentrado(sub, p.ancho / 2, arriba, tamSub, true, 0xFFC857, 1);
        float vx = 36, vy = arriba + tamSub * 1.45f;
        float vw = p.ancho - 72, vh = Math.max(180, abajo - vy);

        Entrenador en = campana.entrenadorPara(jug);
        DibujoEscenaCampana.dibujar(j, vista, controlador.actual(), en,
                capitulo.rival.en(jug.idioma), vx, vy, vw, vh, controlador.tiempoVineta());

        boolean avanzarTecla = j.entrada.tecla(Keys.SPACE) || j.entrada.tecla(Keys.ENTER) || j.entrada.tecla(Keys.RIGHT);
        boolean saltarTecla = j.entrada.tecla(Keys.ESCAPE);
        if (saltarTecla) {
            controlador.saltar();
            cerrar(j);
            return;
        }
        if (avanzarTecla || ui.e.clic(vx, vy, vw, vh)) {
            j.sonidos.tocar("clic", 0.5f);
            controlador.siguiente();
            if (controlador.terminada()) {
                cerrar(j);
                return;
            }
        }
        dibujarBarra(j, abajo + 10, barraH - 10);
    }

    private void dibujarBarra(Juego j, float y, float h) {
        Ui ui = j.ui;
        Pincel p = j.pincel;
        boolean ultima = controlador.indice() + 1 >= controlador.total();
        String txtSig = ultima
                ? (irAPelea ? T.t("campanas.a_pelear_escena") : T.t("campanas.terminar_escena"))
                : T.t("campanas.siguiente_vineta");
        String txtSaltar = T.t("campanas.saltar_escena");
        float bwSaltar = Math.min(p.ancho * 0.40f, Math.max(200, p.ancho(txtSaltar, ui.letra, true) + 48));
        float bwSig = Math.min(p.ancho * 0.48f, Math.max(220, p.ancho(txtSig, ui.letra, true) + 56));
        if (ui.boton(txtSaltar, 36, y, bwSaltar, h, Ui.Estilo.NORMAL, true, null)) {
            controlador.saltar();
            cerrar(j);
            return;
        }
        if (ui.boton(txtSig, p.ancho - 36 - bwSig, y, bwSig, h, Ui.Estilo.PRINCIPAL, true, null)) {
            controlador.siguiente();
            if (controlador.terminada()) cerrar(j);
        }
    }

    private void cerrar(Juego j) {
        if (finalEjecutado) return;
        finalEjecutado = true;
        if (alTerminar != null) alTerminar.run();
        else if (j != null) j.volver();
    }

    @Override
    public void alSalir(Juego j) {
        vista = null;
    }

    /** Acceso al controlador determinista para pruebas sin ventana. */
    public EscenaDeCampana.Controlador controlador() {
        return controlador;
    }
}
