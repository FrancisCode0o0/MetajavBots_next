package metajavbots.pantallas;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.MotorDemostracion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Montaje;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * PANTALLA DE DEMOSTRACIÓN — Reproduce una Demostracion automatizada en vivo:
 * arena a la izquierda (con VisorTactico activable), código de solo lectura a
 * la derecha con resaltado muy visible (▶ y marco) en la línea ejecutada, y
 * viñeta del entrenador abajo con pausas automáticas y botones Continuar,
 * Repetir paso y Salir.
 */
public final class PantallaDemostracion implements Pantalla {

    private final Demostracion demo;
    private MotorDemostracion motor;
    private VistaDeArena vista;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private Idioma idioma = Idioma.ES;
    private float acumulado;
    private float tiempo;
    private float hablando;
    private int pasoAnunciado = -1;
    private boolean eraPausado;

    public PantallaDemostracion(Demostracion demo) {
        this.demo = demo == null ? Demostracion.todas().get(0) : demo;
    }

    @Override
    public void alEntrar(Juego j) {
        idioma = j.jugador != null && j.jugador.idioma != null ? j.jugador.idioma : T.idioma;
        Montaje montaje = j.jugador == null ? null : j.jugador.montajeDe(j.jugador.robot());
        motor = new MotorDemostracion(demo, idioma, montaje);
        vista = new VistaDeArena(j.pincel);
        vista.escenario = demo.escenario;
        vista.conPublico = false;
        editor.ponerTexto(demo.codigo(idioma));
        editor.soloLectura = true;
        editor.seguirActiva = true;
        editor.palabras = Palabras.api(idioma);
        editor.documentacion = w -> Referencia.de(w, idioma);
        editor.tamano = tamanoCodigo(j.ui.letra, j.jugador == null ? 18 : j.jugador.tamanoDeLetra);
        acumulado = 0;
        pasoAnunciado = -1;
        eraPausado = false;
        sincronizarEditor();
    }

    private static float tamanoCodigo(float letraUi, float letraJugador) {
        float base = Math.max(letraJugador, letraUi * 0.78f);
        return Math.max(15f, Math.min(22f, base));
    }

    @Override
    public void fotograma(Juego j, float dt) {
        if (motor == null) alEntrar(j);
        tiempo += dt;
        hablando = Math.max(0, hablando - dt);
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, tiempo);
        float arriba = Comun.cabecera(j, demo.titulo(idioma), true);
        manejarTeclas(j);
        avanzarSimulacion(j, dt);
        anunciarSiPausa(j);
        sincronizarEditor();

        float margen = 20;
        float wTotal = p.ancho - margen * 2;
        float altoVigneta = calcularAltoVigneta(p, ui, wTotal);
        float yVigneta = p.alto - altoVigneta - 14;
        float ySuperior = arriba + 10;
        float hSuperior = Math.max(220, yVigneta - ySuperior - 12);

        float gap = 14;
        float wIzq = (wTotal - gap) * 0.52f;
        float wDer = wTotal - gap - wIzq;
        dibujarArena(j, margen, ySuperior, wIzq, hSuperior, dt);
        dibujarCodigo(ui, margen + wIzq + gap, ySuperior, wDer, hSuperior);
        dibujarVignetaEntrenador(j, margen, yVigneta, wTotal, altoVigneta);
    }

    private void manejarTeclas(Juego j) {
        if (j.entrada.tecla(Keys.F8)) motor.alternarVisor();
        if (j.entrada.tecla(Keys.R)) {
            motor.repetirPaso();
            acumulado = 0;
            eraPausado = false;
        }
        if (motor.pausado() && (j.entrada.tecla(Keys.SPACE) || j.entrada.tecla(Keys.ENTER))) {
            accionContinuar(j);
        }
    }

    private void avanzarSimulacion(Juego j, float dt) {
        if (motor.pausado() || motor.terminada()) return;
        acumulado += dt * Pelea.TURNOS_POR_SEGUNDO;
        int max = 0;
        while (acumulado >= 1 && max < 4 && !motor.pausado()) {
            acumulado -= 1;
            motor.avanzarTurno();
            vista.sucesos(motor.pelea(), j.sonidos);
            max++;
        }
    }

    private void anunciarSiPausa(Juego j) {
        boolean pausadoAhora = motor.pausado();
        if (pausadoAhora && (!eraPausado || pasoAnunciado != motor.indicePaso())) {
            pasoAnunciado = motor.indicePaso();
            hablando = 2.6f;
            if (j.jugador != null && j.voz != null) {
                String texto = motor.pasoActual().texto(idioma).replace("\n", " ");
                Entrenador en = Entrenador.de(j.jugador.entrenador);
                j.voz.decir(texto, idioma, en.timbre);
            }
        }
        eraPausado = pausadoAhora;
    }

    private void sincronizarEditor() {
        editor.lineaActiva = motor.lineaActiva();
        editor.lineasActivas = motor.lineasActivas();
    }

    private void dibujarArena(Juego j, float x, float y, float w, float h, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float barraH = 44;
        String pasoTxt = T.f("demo.paso_de", motor.indicePaso() + 1, motor.totalPasos())
                + " · " + motor.pasoActual().titulo(idioma);
        String visorTxt = T.t("pelea.visor") + " (F8)";
        float visorW = Math.max(130, p.ancho(visorTxt, Math.min(ui.letra * 0.85f, 20f), true) + 28);
        p.caja(x, y, w, barraH, 8, ui.tema.panel, 1);
        float tPaso = ui.cabe(pasoTxt, w - visorW - 28, Math.min(ui.letra * 0.9f, 22f), true);
        p.texto(pasoTxt, x + 12, y + (barraH - p.altoDeLinea(tPaso)) / 2f, tPaso, true, ui.tema.texto, 1);
        if (ui.boton(visorTxt, x + w - visorW - 4, y + 3, visorW, barraH - 6,
                motor.visorTactico() ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true, T.t("pelea.visor_ayuda"))) {
            motor.alternarVisor();
        }
        float arenaY = y + barraH + 6;
        float arenaH = Math.max(140, h - barraH - 6);
        vista.letra = Math.min(ui.letra, 22f);
        vista.techo = arenaY + 6;
        vista.visor(motor.visorTactico(), 1);
        vista.dibujar(p, motor.pelea(), x, arenaY, w, arenaH, motor.pausado() ? 0 : dt);
        p.bordeRedondeado(x, arenaY, w, arenaH, 8, ui.tema.borde, 0.9f, 1.5f);
        String estado = motor.pausado() ? T.t("demo.pausa_paso") : T.t("demo.ejecutando");
        float tEst = Math.min(ui.letra * 0.72f, 17f);
        float ew = p.ancho(estado, tEst, true) + 20;
        float eh = p.altoDeLinea(tEst) + 8;
        p.caja(x + 10, arenaY + 10, ew, eh, 6, 0x0B0F18, 0.84f);
        p.texto(estado, x + 20, arenaY + 14, tEst, true, motor.pausado() ? ui.tema.aviso : ui.tema.bien, 1);
    }

    private void dibujarCodigo(Ui ui, float x, float y, float w, float h) {
        editor.tamano = tamanoCodigo(ui.letra, editor.tamano);
        editor.dibujar(ui, x, y, w, h);
        ui.p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 0.9f, 1.5f);
    }

    private float calcularAltoVigneta(Pincel p, Ui ui, float wTotal) {
        float tTit = Math.min(ui.letra * 0.96f, 23f);
        float tTxt = Math.min(ui.letra * 0.90f, 22f);
        float anchoTexto = Math.max(260, wTotal - 122);
        String texto = motor.pasoActual().texto(idioma);
        float hTit = p.altoDeLinea(tTit);
        float hTxt = p.altoDeParrafo(texto, anchoTexto, tTxt);
        return Math.max(158, Math.min(248, 20 + hTit + 6 + hTxt + 14 + 46 + 14));
    }

    private void dibujarVignetaEntrenador(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Entrenador en = Entrenador.de(j.jugador == null ? null : j.jugador.entrenador);
        p.caja(x, y, w, h, 14, ui.tema.panelAlto, 0.98f);
        p.bordeRedondeado(x, y, w, h, 14, motor.pausado() ? ui.tema.acento : ui.tema.borde, 0.95f, 2f);

        float radio = 30;
        float cx = x + 50;
        float cy = y + Math.min(h * 0.44f, 64f);
        CabezaDeEntrenador.dibujar(p, en, cx, cy, radio, hablando > 0, tiempo);

        float tx = x + 98;
        float tw = Math.max(240, w - 118);
        float tTit = Math.min(ui.letra * 0.96f, 23f);
        float tTxt = Math.min(ui.letra * 0.90f, 22f);
        Demostracion.Paso paso = motor.pasoActual();
        String cab = en.nombre.en(idioma) + " · " + T.f("demo.paso_de", motor.indicePaso() + 1, motor.totalPasos())
                + " · " + paso.titulo(idioma);
        p.texto(cab, tx, y + 12, ui.cabe(cab, tw, tTit, true), true, ui.tema.acento, 1);
        float ty = y + 12 + p.altoDeLinea(tTit) + 6;
        float maxTextoH = Math.max(40, h - (ty - y) - 64);
        p.recortar(tx, ty, tw, maxTextoH);
        p.parrafo(paso.texto(idioma), tx, ty, tw, tTxt, ui.tema.texto, 1);
        p.sinRecorte();

        dibujarBotones(j, tx, y + h - 56, tw);
    }

    private void dibujarBotones(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float tBtn = Math.min(ui.letra, 22f);
        String txtContinuar = T.t("demo.continuar");
        String txtRepetir = T.t("demo.repetir");
        String txtSalir = T.t("demo.salir");
        float wCont = Math.max(150, p.ancho(txtContinuar, tBtn, true) + 36);
        float wRep = Math.max(170, p.ancho(txtRepetir, tBtn, true) + 36);
        float wSal = Math.max(120, p.ancho(txtSalir, tBtn, true) + 36);
        float total = wCont + wRep + wSal + 24;
        if (total > w) {
            float esc = (w - 24) / (wCont + wRep + wSal);
            wCont *= esc;
            wRep *= esc;
            wSal *= esc;
        }
        Ui.Estilo estCont = motor.pausado() ? Ui.Estilo.PRINCIPAL : Ui.Estilo.NORMAL;
        if (ui.boton(txtContinuar, x, y, wCont, 44, estCont)) accionContinuar(j);
        if (ui.boton(txtRepetir, x + wCont + 12, y, wRep, 44, Ui.Estilo.NORMAL)) {
            motor.repetirPaso();
            acumulado = 0;
            eraPausado = false;
        }
        if (ui.boton(txtSalir, x + wCont + wRep + 24, y, wSal, 44, Ui.Estilo.PLANO)) {
            j.volver();
        }
    }

    private void accionContinuar(Juego j) {
        if (motor.esUltimoPaso() && motor.pausado()) {
            j.volver();
            return;
        }
        motor.continuar();
        acumulado = 0;
        eraPausado = false;
    }
}
