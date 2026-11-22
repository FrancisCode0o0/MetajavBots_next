package metajavbots.pantallas;

import java.util.Locale;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.combate.Cuerpo;
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
 * arena a la izquierda (con VisorTactico y señales visuales didácticas), código
 * de solo lectura a la derecha con resaltado muy visible (▶ y marco) en la línea
 * explicada, y viñeta del entrenador abajo con scroll suave, pausas automáticas
 * y botones Continuar, Repetir paso y Salir.
 */
public final class PantallaDemostracion implements Pantalla {

    private final Demostracion demo;
    private final int pasoInicial;
    private MotorDemostracion motor;
    private VistaDeArena vista;
    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final Ui.Desplazable desplTexto = new Ui.Desplazable();
    private Idioma idioma = Idioma.ES;
    private String codigoMostrado = "";
    private float acumulado;
    private float tiempo;
    private float hablando;
    private int pasoAnunciado = -1;
    private boolean eraPausado;

    public PantallaDemostracion(Demostracion demo) {
        this(demo, 0);
    }

    public PantallaDemostracion(Demostracion demo, int pasoInicial) {
        this.demo = demo == null ? Demostracion.todas().get(0) : demo;
        this.pasoInicial = Math.max(0, pasoInicial);
    }

    @Override
    public void alEntrar(Juego j) {
        idioma = j.jugador != null && j.jugador.idioma != null ? j.jugador.idioma : T.idioma;
        Montaje montaje = j.jugador == null ? null : j.jugador.montajeDe(j.jugador.robot());
        motor = new MotorDemostracion(demo, idioma, montaje);
        if (pasoInicial > 0) motor.irAPaso(pasoInicial);
        vista = new VistaDeArena(j.pincel);
        vista.escenario = demo.escenario;
        vista.conPublico = false;
        codigoMostrado = motor.codigoActual();
        editor.ponerTexto(codigoMostrado);
        editor.soloLectura = true;
        editor.seguirActiva = true;
        editor.palabras = Palabras.api(idioma);
        editor.documentacion = w -> Referencia.de(w, idioma);
        editor.tamano = tamanoCodigo(j.ui.letra, j.jugador == null ? 18 : j.jugador.tamanoDeLetra);
        acumulado = 0;
        pasoAnunciado = -1;
        eraPausado = false;
        desplTexto.alPrincipio();
        sincronizarEditor(j.ui);
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
        sincronizarEditor(ui);

        float margen = 20;
        float wTotal = p.ancho - margen * 2;
        float altoVigneta = calcularAltoVigneta(p, ui, wTotal);
        float yVigneta = p.alto - altoVigneta - 14;
        float ySuperior = arriba + 10;
        float hSuperior = Math.max(240, yVigneta - ySuperior - 12);

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
            desplTexto.alPrincipio();
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

    private void sincronizarEditor(Ui ui) {
        String codigoActual = motor.codigoActual();
        if (!codigoActual.equals(codigoMostrado)) {
            codigoMostrado = codigoActual;
            editor.ponerTexto(codigoMostrado);
        }
        editor.lineaActiva = motor.lineaActiva();
        editor.lineasActivas = motor.lineasActivas();
        Demostracion.Resaltado r = motor.resaltadoActual();
        if (r == Demostracion.Resaltado.ERROR) editor.colorLineaActiva = ui.tema.mal;
        else if (r == Demostracion.Resaltado.AVISO) editor.colorLineaActiva = ui.tema.aviso;
        else editor.colorLineaActiva = 0;
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
        float hudY = y + barraH + 6;
        float hudH = 68;
        dibujarBarrasHud(j, x, hudY, w, hudH);
        float arenaY = hudY + hudH + 6;
        float arenaH = Math.max(140, h - barraH - hudH - 12);
        vista.letra = Math.min(ui.letra, 22f);
        vista.techo = arenaY + 6;
        vista.visor(motor.visorTactico(), 1);
        vista.dibujar(p, motor.pelea(), x, arenaY, w, arenaH, motor.pausado() ? 0 : dt);
        dibujarIndicadoresArena(p, ui, x, arenaY, w, arenaH);
        p.bordeRedondeado(x, arenaY, w, arenaH, 8, ui.tema.borde, 0.9f, 1.5f);
        String estado = motor.pausado() ? T.t("demo.pausa_paso") : T.t("demo.ejecutando");
        float tEst = Math.min(ui.letra * 0.72f, 17f);
        float ew = p.ancho(estado, tEst, true) + 20;
        float eh = p.altoDeLinea(tEst) + 8;
        p.caja(x + 10, arenaY + 10, ew, eh, 6, 0x0B0F18, 0.84f);
        p.texto(estado, x + 20, arenaY + 14, tEst, true, motor.pausado() ? ui.tema.aviso : ui.tema.bien, 1);
    }

    /** Dibuja las barras de blindaje por partes, energía y recarga con el mismo estilo visual que PantallaPelea. */
    private void dibujarBarrasHud(Juego j, float x, float y, float w, float h) {
        Pelea pelea = motor.pelea();
        if (pelea == null) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean resaltar = motor.resaltarHudBarras();
        float pulso = resaltar ? 0.65f + 0.35f * (float) Math.sin(tiempo * 5f) : 0.9f;
        p.caja(x, y, w, h, 8, ui.tema.panel, 0.97f);
        p.bordeRedondeado(x, y, w, h, 8, resaltar ? ui.tema.acento : ui.tema.borde, pulso, resaltar ? 2.2f : 1.2f);
        float gap = 14;
        float colW = (w - 20 - gap) / 2f;
        dibujarBloqueRobotHud(j, pelea, 0, x + 10, y + 6, colW, 0x3FE0FF);
        dibujarBloqueRobotHud(j, pelea, 1, x + 10 + colW + gap, y + 6, colW, 0xFF4FD8);
    }

    private void dibujarBloqueRobotHud(Juego j, Pelea pelea, int indice, float x, float y, float w, int color) {
        Cuerpo c = pelea.cuerpo(indice);
        if (c == null) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String nombre = pelea.combatientes[indice].nombre;
        String extra = indice == 0
                ? String.format(Locale.ROOT, "%s %.0f", T.t("pelea.energia"), c.energia)
                : String.format(Locale.ROOT, "%.0f%%", c.blindaje());
        float tExtra = Math.min(ui.letra * 0.68f, 15.5f);
        float wExtra = p.ancho(extra, tExtra, false);
        float tNom = ui.cabe(nombre, Math.max(60, w - wExtra - 10), Math.min(ui.letra * 0.78f, 17f), true);
        p.texto(nombre, x, y, tNom, true, color, 1);
        p.textoDerecha(extra, x + w, y + 1, tExtra, false, indice == 0 ? 0xFFD23F : ui.tema.textoSuave, 1);

        float by = y + 22;
        float barGap = 5;
        float bw = (w - barGap * 3) / 4f;
        String[] nombres = {T.t("parte.cabeza"), T.t("parte.izquierda"), T.t("parte.derecha"), T.t("parte.piernas")};
        float tParte = Math.min(ui.letra * 0.56f, 14f);
        for (int k = 0; k < 4; k++) {
            float parte = (float) (c.parte[k] / Math.max(1, c.parteMax[k]));
            int col = parte > 0.5f ? ui.tema.bien : parte > 0.2f ? ui.tema.aviso : ui.tema.mal;
            float bx = x + k * (bw + barGap);
            ui.barra(bx, by, bw, 11, parte, col, null);
            String et = nombres[k];
            if (p.ancho(et, tParte, false) > bw) et = et.substring(0, Math.min(4, et.length())) + ".";
            p.texto(et, bx + 2, by + 13, ui.cabe(et, bw - 2, tParte, false), false, ui.tema.textoSuave, 1);
        }
        float ey = by + 31;
        float ratioEnergia = (float) (c.energia / Math.max(1, c.cifras.energiaMax));
        ui.barra(x, ey, w, 7, ratioEnergia, 0xFFD23F, null);
    }

    private void dibujarIndicadoresArena(Pincel p, Ui ui, float ax, float ay, float aw, float ah) {
        Pelea pelea = motor.pelea();
        if (pelea == null) return;
        Cuerpo yo = pelea.cuerpo(0);
        Cuerpo rival = pelea.cuerpo(1);
        if (yo == null) return;
        float pulso = 0.65f + 0.35f * (float) Math.sin(tiempo * 5f);
        float tl = Math.min(ui.letra * 0.72f, 17f);
        p.recortar(ax, ay, aw, ah);
        switch (motor.pasoActual().accion) {
            case AVANZAR:
                dibujarIndicadorAvance(p, yo, tl);
                break;
            case GIRAR:
                dibujarIndicadorGiro(p, yo, tl);
                break;
            case RADAR:
                dibujarIndicadorRadar(p, yo, pulso, tl);
                break;
            case DETECTAR:
                if (motor.enemigoDetectado() && rival != null) {
                    dibujarIndicadorDeteccion(p, rival, pulso, tl);
                }
                break;
            case APUNTAR_Y_DISPARAR:
            case ACIERTO_ORDEN:
                dibujarIndicadorDisparo(p, yo, rival, pulso, tl);
                break;
            case FALLO_ORDEN:
                dibujarIndicadorFallo(p, yo, pulso, tl);
                break;
            case ORDEN_TURNO:
                dibujarIndicadorOrdenTurno(p, ax, ay, aw, tl);
                break;
            case CORREGIR_LINEA:
                dibujarIndicadorCorregido(p, ax, ay, aw, tl);
                break;
            case ENTORNO_INGENUO:
                dibujarIndicadorEntornoIngenuo(p, yo, pulso, tl);
                break;
            case AJUSTAR_ENTORNO:
                dibujarIndicadorAjustarEntorno(p, ax, ay, aw, tl);
                break;
            case ENTORNO_AJUSTADO:
                dibujarIndicadorDisparo(p, yo, rival, pulso, tl);
                break;
            default:
                dibujarIndicadoresDemo4(p, yo, rival, ax, ay, aw, pulso, tl);
                break;
        }
        p.sinRecorte();
    }

    private void dibujarIndicadoresDemo4(Pincel p, Cuerpo yo, Cuerpo rival,
            float ax, float ay, float aw, float pulso, float tl) {
        switch (motor.pasoActual().accion) {
            case POO_EXPUESTO:
                dibujarIndicadorDemo4Texto(p, ax, ay, aw, tl, 0xFF6B6B,
                        idioma.elegir("public mutable: +8 % daño · +16 % radar rival",
                                "public veränderbar: +8 % Schaden · +16 % Gegner-Radar",
                                "mutable public: +8% damage · +16% enemy radar"));
                break;
            case POO_ENCAPSULADO:
                dibujarIndicadorDemo4Texto(p, ax, ay, aw, tl, 0x5BD685,
                        idioma.elegir("private/final/static: -6 % daño · +10 % pasos",
                                "private/final/static: -6 % Schaden · +10 % Schritte",
                                "private/final/static: -6% damage · +10% steps"));
                break;
            case APARATO_ERROR:
                dibujarIndicadorDemo4Texto(p, ax, ay, aw, tl, 0xFF6B6B,
                        idioma.elegir("✗ conectar(\"cabeza\") rechazado (espalda)",
                                "✗ anschliessen(\"kopf\") abgelehnt (ruecken)",
                                "✗ plugIn(\"head\") rejected (back)"));
                break;
            case APARATO_BIEN:
                dibujarIndicadorDemo4Texto(p, ax, ay, aw, tl, 0x5BD685,
                        idioma.elegir("✓ conectar(\"espalda\") → usarAparato()",
                                "✓ anschliessen(\"ruecken\") → geraetBenutzen()",
                                "✓ plugIn(\"back\") → useGadget()"));
                break;
            case GESTION_ENERGIA:
            case TOPE_OPTIMO:
                dibujarIndicadorDisparo(p, yo, rival, pulso, tl);
                break;
            case TOPE_DEUDA:
                dibujarIndicadorDemo4Texto(p, ax, ay, aw, tl, 0xFF6B6B,
                        idioma.elegir("⏱ TopeDeCalculo · deuda de turnos",
                                "⏱ TopeDeCalculo · Zugschuld",
                                "⏱ TopeDeCalculo · turn debt"));
                break;
            default:
                break;
        }
    }

    private void dibujarIndicadorAvance(Pincel p, Cuerpo yo, float tl) {
        float x1 = vista.sx(motor.inicioX(), motor.inicioY());
        float y1 = vista.sy(motor.inicioY());
        float x2 = vista.sx(yo.x, yo.y);
        float y2 = vista.sy(yo.y);
        if (Math.hypot(x2 - x1, y2 - y1) < 6) return;
        p.circulo(x1, y1, 5f, 0x5BD685, 0.9f);
        p.linea(x1, y1, x2, y2, 3.2f, 0x5BD685, 0.95f);
        dibujarPuntaFlecha(p, x1, y1, x2, y2, 11f, 0x5BD685);
        String txt = String.format(Locale.ROOT, "+%.0f", motor.distanciaRecorrida());
        dibujarEtiquetaArena(p, txt, (x1 + x2) / 2f, Math.min(y1, y2) - 26f, tl, 0x5BD685);
    }

    private void dibujarIndicadorGiro(Pincel p, Cuerpo yo, float tl) {
        double r = 74;
        float ox = vista.sx(yo.x, yo.y);
        float oy = vista.sy(yo.y);
        double a0 = Math.toRadians(motor.inicioRumbo());
        double a1 = Math.toRadians(yo.rumbo);
        float ex0 = vista.sx(yo.x + Math.cos(a0) * r, yo.y + Math.sin(a0) * r);
        float ey0 = vista.sy(yo.y + Math.sin(a0) * r);
        float ex1 = vista.sx(yo.x + Math.cos(a1) * r, yo.y + Math.sin(a1) * r);
        float ey1 = vista.sy(yo.y + Math.sin(a1) * r);
        p.linea(ox, oy, ex0, ey0, 2f, 0x9BB0C8, 0.7f);
        p.linea(ox, oy, ex1, ey1, 3f, 0xFFE27A, 0.95f);
        dibujarPuntaFlecha(p, ox, oy, ex1, ey1, 10f, 0xFFE27A);
        double delta = Pelea.normal(yo.rumbo - motor.inicioRumbo());
        int segs = Math.max(4, (int) Math.ceil(Math.abs(delta) / 6));
        double rArco = 48;
        for (int i = 0; i < segs; i++) {
            double u = Math.toRadians(motor.inicioRumbo() + delta * i / segs);
            double v = Math.toRadians(motor.inicioRumbo() + delta * (i + 1) / segs);
            p.linea(vista.sx(yo.x + Math.cos(u) * rArco, yo.y + Math.sin(u) * rArco),
                    vista.sy(yo.y + Math.sin(u) * rArco),
                    vista.sx(yo.x + Math.cos(v) * rArco, yo.y + Math.sin(v) * rArco),
                    vista.sy(yo.y + Math.sin(v) * rArco), 2.6f, 0xFFE27A, 0.95f);
        }
        double aMid = Math.toRadians(motor.inicioRumbo() + delta * 0.5);
        String txt = String.format(Locale.ROOT, "%+.0f°", delta);
        dibujarEtiquetaArena(p, txt, vista.sx(yo.x + Math.cos(aMid) * 90, yo.y + Math.sin(aMid) * 90),
                vista.sy(yo.y + Math.sin(aMid) * 90) - 10f, tl, 0xFFE27A);
    }

    private void dibujarIndicadorRadar(Pincel p, Cuerpo yo, float pulso, float tl) {
        float ox = vista.sx(yo.x, yo.y);
        float oy = vista.sy(yo.y);
        p.aro(ox, oy, 22f + pulso * 7f, 2.2f, 0x3FE0FF, 0.85f);
        String txt = idioma.elegir("RADAR", "RADAR", "RADAR");
        dibujarEtiquetaArena(p, txt, ox, oy + 26f, tl, 0x3FE0FF);
    }

    private void dibujarIndicadorDeteccion(Pincel p, Cuerpo rival, float pulso, float tl) {
        float rx = vista.sx(rival.x, rival.y);
        float ry = vista.sy(rival.y);
        p.aro(rx, ry, 28f + pulso * 10f, 3f, 0xFFE27A, 0.95f);
        String txt = idioma.elegir("¡alVerEnemigo!", "beiSicht!", "onSeeEnemy!");
        dibujarEtiquetaArena(p, txt, rx, ry - 68f, tl, 0xFFE27A);
    }

    private void dibujarIndicadorDisparo(Pincel p, Cuerpo yo, Cuerpo rival, float pulso, float tl) {
        if (motor.impactoRealizado() && rival != null) {
            float rx = vista.sx(rival.x, rival.y);
            float ry = vista.sy(rival.y);
            p.aro(rx, ry, 26f + pulso * 9f, 3f, 0xFF7A45, 0.95f);
        }
        float ox = vista.sx(yo.x, yo.y);
        float oy = vista.sy(yo.y);
        String txt = String.format(Locale.ROOT, "%s %.0f",
                idioma.elegir("Energía", "Energie", "Energy"), yo.energia);
        dibujarEtiquetaArena(p, txt, ox, oy + 26f, tl, 0x3FE0FF);
    }

    private void dibujarIndicadorFallo(Pincel p, Cuerpo yo, float pulso, float tl) {
        if (!motor.disparoRealizado()) return;
        float ox = vista.sx(yo.x, yo.y);
        float oy = vista.sy(yo.y);
        p.aro(ox, oy, 26f + pulso * 8f, 2.8f, 0xFF6B6B, 0.95f);
        String txt = idioma.elegir("¡Fallo! +25°", "Daneben! +25°", "Miss! +25°");
        dibujarEtiquetaArena(p, txt, ox, oy + 34f, tl, 0xFF6B6B);
    }

    private void dibujarIndicadorOrdenTurno(Pincel p, float ax, float ay, float aw, float tl) {
        String txt = idioma.elegir(
                "1. avisos → 2. alVerEnemigo() → 3. cadaTurno()",
                "1. Ereignisse → 2. beiSicht() → 3. jedenZug()",
                "1. events → 2. onSeeEnemy() → 3. onTick()");
        dibujarEtiquetaArena(p, txt, ax + aw / 2f, ay + 64f, tl, 0xFFC857);
    }

    private void dibujarIndicadorCorregido(Pincel p, float ax, float ay, float aw, float tl) {
        String txt = idioma.elegir(
                "✓ apuntar(25) dentro del if",
                "✓ zielen(25) im if-Block",
                "✓ aim(25) inside the if");
        dibujarEtiquetaArena(p, txt, ax + aw / 2f, ay + 64f, tl, 0x5BD685);
    }

    private void dibujarIndicadorEntornoIngenuo(Pincel p, Cuerpo yo, float pulso, float tl) {
        if (!motor.disparoRealizado()) return;
        float ox = vista.sx(yo.x, yo.y);
        float oy = vista.sy(yo.y);
        p.aro(ox, oy, 26f + pulso * 8f, 2.8f, 0xFF6B6B, 0.95f);
        String txt = idioma.elegir("¡Bloqueado por barril!", "Vom Fass blockiert!", "Blocked by barrel!");
        dibujarEtiquetaArena(p, txt, ox, oy + 34f, tl, 0xFF6B6B);
    }

    private void dibujarIndicadorAjustarEntorno(Pincel p, float ax, float ay, float aw, float tl) {
        String txt = idioma.elegir(
                "✓ obstaculos() + enemigosVistos() + alcance",
                "✓ hindernisse() + gesehen() + Reichweite",
                "✓ obstacles() + seenEnemies() + range");
        dibujarEtiquetaArena(p, txt, ax + aw / 2f, ay + 64f, tl, 0x5BD685);
    }

    private void dibujarIndicadorDemo4Texto(Pincel p, float ax, float ay, float aw, float tl, int color, String txt) {
        dibujarEtiquetaArena(p, txt, ax + aw / 2f, ay + 64f, tl, color);
    }

    private static void dibujarPuntaFlecha(Pincel p, float x1, float y1, float x2, float y2, float largo, int color) {
        double ang = Math.atan2(y2 - y1, x2 - x1);
        float ax = (float) (x2 - Math.cos(ang - 0.5) * largo);
        float ay = (float) (y2 - Math.sin(ang - 0.5) * largo);
        float bx = (float) (x2 - Math.cos(ang + 0.5) * largo);
        float by = (float) (y2 - Math.sin(ang + 0.5) * largo);
        p.triangulo(x2, y2, ax, ay, bx, by, color, 0.95f);
    }

    private static void dibujarEtiquetaArena(Pincel p, String texto, float cx, float cy, float tl, int color) {
        float w = p.ancho(texto, tl, true) + 14;
        float h = p.altoDeLinea(tl) + 6;
        p.caja(cx - w / 2f, cy - h / 2f, w, h, 6, 0x0B0F18, 0.88f);
        p.bordeRedondeado(cx - w / 2f, cy - h / 2f, w, h, 6, color, 0.85f, 1.4f);
        p.textoCentrado(texto, cx, cy - h / 2f + 3f, tl, true, color, 1);
    }

    private void dibujarCodigo(Ui ui, float x, float y, float w, float h) {
        editor.tamano = tamanoCodigo(ui.letra, editor.tamano);
        editor.dibujar(ui, x, y, w, h);
        ui.p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 0.9f, 1.5f);
    }

    private float calcularAltoVigneta(Pincel p, Ui ui, float wTotal) {
        float tTit = Math.min(ui.letra * 0.92f, 23f);
        float tTxt = Math.min(ui.letra * 0.85f, 21f);
        float anchoTexto = Math.max(260, wTotal - 134);
        String texto = motor.pasoActual().texto(idioma);
        float hTit = p.altoDeLinea(tTit);
        float hTxt = p.altoDeParrafo(texto, anchoTexto, tTxt);
        float deseado = 20 + hTit + 8 + hTxt + 14 + 46 + 14;
        float tope = Math.min(290f, Math.max(195f, p.alto * 0.34f));
        return Math.max(176f, Math.min(tope, deseado));
    }

    private void dibujarVignetaEntrenador(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Entrenador en = Entrenador.de(j.jugador == null ? null : j.jugador.entrenador);
        p.caja(x, y, w, h, 14, ui.tema.panelAlto, 0.98f);
        p.bordeRedondeado(x, y, w, h, 14, motor.pausado() ? ui.tema.acento : ui.tema.borde, 0.95f, 2f);

        float radio = 30;
        float cx = x + 50;
        float cy = y + Math.min(h * 0.42f, 64f);
        CabezaDeEntrenador.dibujar(p, en, cx, cy, radio, hablando > 0, tiempo);

        float tx = x + 98;
        float tw = Math.max(240, w - 118);
        float tTit = Math.min(ui.letra * 0.92f, 23f);
        float tTxt = Math.min(ui.letra * 0.85f, 21f);
        Demostracion.Paso paso = motor.pasoActual();
        String cab = en.nombre.en(idioma) + " · " + T.f("demo.paso_de", motor.indicePaso() + 1, motor.totalPasos())
                + " · " + paso.titulo(idioma);
        p.texto(cab, tx, y + 12, ui.cabe(cab, tw, tTit, true), true, ui.tema.acento, 1);
        float ty = y + 12 + p.altoDeLinea(tTit) + 6;
        float maxTextoH = Math.max(48, h - (ty - y) - 62);
        float d = ui.desplazable(desplTexto, tx, ty, tw, maxTextoH);
        float hTexto = p.parrafo(paso.texto(idioma), tx, ty - d, tw - 14, tTxt, ui.tema.texto, 1);
        ui.finDesplazable(desplTexto, hTexto);

        dibujarBotones(j, tx, y + h - 54, tw);
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
            desplTexto.alPrincipio();
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
        desplTexto.alPrincipio();
        acumulado = 0;
        eraPausado = false;
    }
}
