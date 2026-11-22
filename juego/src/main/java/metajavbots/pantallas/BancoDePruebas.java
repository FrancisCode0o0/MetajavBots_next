package metajavbots.pantallas;

import java.util.List;
import java.util.Random;

import metajavbots.Juego;
import metajavbots.api.Metajavbot;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Aparatos;
import metajavbots.combate.Arena;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * BANCO DE PRUEBAS — Simulación en vivo en miniatura dentro del taller, al lado
 * del editor de código. Usa el mismo motor de combate con un muñeco configurable
 * (quieto, en movimiento, con obstáculo o con aparato), sin tocar estadísticas,
 * historial, progreso, desgaste ni guardar partida, y sin entrada épica ni locutor.
 */
public final class BancoDePruebas {

    /** Comportamiento del muñeco y entorno del banco de pruebas rápido. */
    public enum Modo {
        QUIETO("banco.quieto"),
        MOVIMIENTO("banco.movimiento"),
        OBSTACULO("banco.obstaculo"),
        APARATO("banco.aparato");

        public final String clave;

        Modo(String clave) {
            this.clave = clave;
        }
    }

    public static final int COLOR_AZUL = 0x3FA9F5;
    public static final int COLOR_ROJO = 0xE84855;
    private static final float[] VELOCIDADES = {0.1f, 0.25f, 0.5f, 1f, 2f};
    private static final String[] ETIQUETAS_VELOCIDAD = {"0.1x", "0.25x", "0.5x", "1x", "2x"};

    private Modo modo = Modo.QUIETO;
    private Pelea pelea;
    private Combatiente yo;
    private Combatiente muneco;
    private VistaDeArena vista;
    private boolean pausado;
    private float acumulado;
    private int indiceVelocidad = 3;
    private long semilla = 1001L;
    private String claveRobot = "";
    private final Random azar = new Random();
    private int colorJugador = COLOR_AZUL;
    private int colorMuneco = COLOR_ROJO;

    public int colorJugador() {
        return colorJugador;
    }

    public int colorMuneco() {
        return colorMuneco;
    }

    public Modo modo() {
        return modo;
    }

    public void modo(Modo nuevo) {
        if (nuevo != null) modo = nuevo;
    }

    public boolean pausado() {
        return pausado;
    }

    public void alternarPausa() {
        pausado = !pausado;
    }

    public float velocidad() {
        return VELOCIDADES[indiceVelocidad];
    }

    public void masLento() {
        if (indiceVelocidad > 0) indiceVelocidad--;
    }

    public void masRapido() {
        if (indiceVelocidad < VELOCIDADES.length - 1) indiceVelocidad++;
    }

    public Pelea pelea() {
        return pelea;
    }

    public boolean activo() {
        return pelea != null;
    }

    /** Resalta en el editor las líneas ejecutadas en el último turno del banco de pruebas. */
    public void marcarLineas(VentanaDeCodigo editor) {
        if (editor == null) return;
        Cuerpo c = pelea == null ? null : pelea.cuerpo(0);
        if (c != null && c.lineasDelTurno != null && !c.lineasDelTurno.isEmpty()) {
            editor.lineasActivas = c.lineasDelTurno;
            editor.lineaActiva = c.ultimaLinea;
        } else {
            editor.lineasActivas.clear();
            editor.lineaActiva = 0;
        }
    }

    /** Limpia por completo la simulación y sus recursos visuales. */
    public void limpiar() {
        pelea = null;
        yo = null;
        muneco = null;
        vista = null;
        pausado = false;
        acumulado = 0;
        claveRobot = "";
    }

    /** Inicia o reinicia la simulación desde el turno 0 con el robot actual y el modo elegido. */
    public void reiniciar(Jugador.Robot r, RobotDelJugador probador, Pincel pincel) {
        reiniciar(null, r, probador, pincel);
    }

    /** Inicia o reinicia la simulación aplicando los ajustes reales del jugador sobre el montaje del robot. */
    public void reiniciar(Jugador jug, Jugador.Robot r, RobotDelJugador probador, Pincel pincel) {
        if (r == null) {
            limpiar();
            return;
        }
        Montaje montajeJugador = jug != null ? jug.montajeDe(r) : r.montaje();
        Fabrica fabJugador = probador != null && probador.listo() ? probador : null;
        yo = new Combatiente(r.nombre, montajeJugador, fabJugador);
        Modelo modMuneco = Modelo.de("cobalto");
        if (modMuneco == null) {
            List<Modelo> todos = Modelo.todos();
            modMuneco = todos.get(0);
        }
        Modo modoActual = modo;
        muneco = new Combatiente(T.t("banco.muneco"), new Montaje(modMuneco), () -> new MunecoDeBanco(modoActual));
        muneco.computadora = true;
        pelea = new Pelea(yo, muneco, semilla++);
        pelea.rondas = 1;
        if (modoActual == Modo.OBSTACULO) {
            pelea.arena = Arena.de("CHATARRERIA", pelea.ancho, pelea.alto);
        }
        if (modoActual == Modo.APARATO) {
            pelea.aparatos.activos = true;
        }
        pelea.empezarRonda();
        pausado = false;
        acumulado = 0;
        claveRobot = firmaDeRobot(r);
        boolean jugadorAzul = azar.nextBoolean();
        colorJugador = jugadorAzul ? COLOR_AZUL : COLOR_ROJO;
        colorMuneco = jugadorAzul ? COLOR_ROJO : COLOR_AZUL;
        if (pincel != null) {
            vista = new VistaDeArena(pincel);
            vista.conPublico = false;
            vista.coloresRobotitos = new int[]{colorJugador, colorMuneco};
            vista.escenario = modoActual == Modo.OBSTACULO ? VistaDeArena.Escenario.CHATARRERIA : VistaDeArena.Escenario.LABORATORIO;
        } else {
            vista = null;
        }
    }

    /** Pausa la simulación y avanza exactamente un turno. */
    public void paso() {
        pausado = true;
        if (pelea != null && pelea.estado() == Pelea.Estado.LUCHANDO) {
            pelea.avanzar();
            if (vista != null) vista.sucesos(pelea, null);
        }
    }

    /** Avanza la simulación en tiempo real (sin sonidos ni locutor). */
    public void avanzarTiempo(float dt) {
        if (pelea == null || pausado || pelea.estado() != Pelea.Estado.LUCHANDO) return;
        acumulado += Math.min(Math.max(dt, 0), 0.1f) * velocidad();
        float pasoTiempo = 1f / Pelea.TURNOS_POR_SEGUNDO;
        while (acumulado >= pasoTiempo && pelea.estado() == Pelea.Estado.LUCHANDO) {
            acumulado -= pasoTiempo;
            pelea.avanzar();
            if (vista != null) vista.sucesos(pelea, null);
        }
    }

    /**
     * Dibuja el panel completo del banco de pruebas rápido. Devuelve true si el jugador
     * pulsó «Cerrar» para volver al panel lateral anterior.
     */
    public boolean dibujar(Juego j, Jugador.Robot r, RobotDelJugador probador, boolean compilando,
            float x, float y, float w, float h, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (r != null && (pelea == null || !claveRobot.equals(firmaDeRobot(r)))) {
            reiniciar(j.jugador, r, probador, p);
        }
        if (!compilando) avanzarTiempo(dt);
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        p.bordeRedondeado(x, y, w, h, 10, ui.tema.borde, 1, 1);
        boolean cerrar = dibujarCabecera(ui, x, y, w);
        float altoBtn = Math.max(36, ui.letra * 1.55f);
        float cy = y + Math.max(46, ui.letra * 1.9f);
        cy = dibujarModos(ui, r, probador, p, x + 10, cy, w - 20, altoBtn);
        cy = dibujarAcciones(ui, r, probador, p, x + 10, cy, w - 20, altoBtn);
        cy = dibujarHud(ui, x + 10, cy, w - 20);
        String pie = textoPie(probador, compilando);
        int colorPie = colorPie(ui, probador, compilando);
        float tPie = Math.min(ui.letra * 0.78f, 19);
        float altoPie = Math.min(tPie * 2.8f, p.altoDeParrafo(pie, w - 24, tPie)) + 10;
        float arenaH = Math.max(120, y + h - 8 - altoPie - cy);
        if (pelea != null && vista != null) {
            vista.letra = Math.max(12, Math.min(16, ui.letra * 0.68f));
            vista.dibujar(p, pelea, x + 10, cy, w - 20, arenaH, pausado ? 0 : dt * velocidad());
            p.bordeRedondeado(x + 10, cy, w - 20, arenaH, 6, ui.tema.borde, 0.9f, 1);
        }
        p.recortar(x + 10, cy + arenaH + 4, w - 20, altoPie);
        p.parrafo(pie, x + 12, cy + arenaH + 6, w - 24, tPie, colorPie, 1);
        p.sinRecorte();
        return cerrar;
    }

    private boolean dibujarCabecera(Ui ui, float x, float y, float w) {
        Pincel p = ui.p;
        float altoCerrar = Math.max(34, ui.letra * 1.45f);
        float tBtn = Math.min(ui.letra, altoCerrar * 0.5f);
        String txtCerrar = "✕ " + T.t("banco.cerrar");
        float cw = Math.min(w * 0.45f, p.ancho(txtCerrar, tBtn, true) + 28);
        boolean cerrar = ui.boton(txtCerrar, x + w - 10 - cw, y + 6, cw, altoCerrar, Ui.Estilo.PLANO);
        String titulo = T.t("banco.titulo");
        float espacioTitulo = Math.max(40, w - 28 - cw);
        float tTit = ui.cabe(titulo, espacioTitulo, ui.letra * 1.0f, true);
        p.texto(ui.recortarTexto(titulo, espacioTitulo, tTit, true), x + 12, y + 6 + (altoCerrar - tTit * 1.15f) / 2,
                tTit, true, ui.tema.texto, 1);
        return cerrar;
    }

    private float dibujarModos(Ui ui, Jugador.Robot r, RobotDelJugador probador, Pincel p,
            float x, float y, float w, float h) {
        Modo[] modos = Modo.values();
        String[] textos = new String[modos.length];
        Ui.Estilo[] estilos = new Ui.Estilo[modos.length];
        for (int i = 0; i < modos.length; i++) {
            textos[i] = T.t(modos[i].clave);
            estilos[i] = modo == modos[i] ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
        }
        int pulsado = ui.botonera(textos, estilos, null, null, x, y, w, h);
        if (pulsado >= 0 && pulsado < modos.length) {
            modo = modos[pulsado];
            reiniciar(r, probador, p);
        }
        return y + ui.altoDeBotonera + 8;
    }

    private float dibujarAcciones(Ui ui, Jugador.Robot r, RobotDelJugador probador, Pincel p,
            float x, float y, float w, float h) {
        boolean luchando = pelea != null && pelea.estado() == Pelea.Estado.LUCHANDO;
        String[] textos = {T.t("banco.reiniciar"), pausado || !luchando ? T.t("banco.seguir") : T.t("banco.pausa"), T.t("banco.paso")};
        Ui.Estilo[] estilos = {Ui.Estilo.PLANO, pausado ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, Ui.Estilo.PLANO};
        boolean[] activos = {r != null, r != null, luchando};
        float velW = 136;
        float anchoIzq = Math.max(120, w - velW - 8);
        switch (ui.botonera(textos, estilos, activos, null, x, y, anchoIzq, h)) {
            case 0: reiniciar(r, probador, p); break;
            case 1:
                if (!luchando) reiniciar(r, probador, p);
                else alternarPausa();
                break;
            case 2: paso(); break;
            default: break;
        }
        float vx = x + w - velW;
        if (ui.boton("−", vx, y, 34, h, Ui.Estilo.PLANO, indiceVelocidad > 0, T.t("banco.mas_lento"))) masLento();
        if (ui.boton(ETIQUETAS_VELOCIDAD[indiceVelocidad], vx + 38, y, 60, h, Ui.Estilo.NORMAL, true, T.t("banco.velocidad_ayuda"))) {
            indiceVelocidad = 3;
        }
        if (ui.boton("+", vx + 102, y, 34, h, Ui.Estilo.PLANO, indiceVelocidad < VELOCIDADES.length - 1, T.t("banco.mas_rapido"))) masRapido();
        return y + Math.max(ui.altoDeBotonera, h) + 8;
    }

    private float dibujarHud(Ui ui, float x, float y, float w) {
        Pincel p = ui.p;
        float alto = Math.max(24, ui.letra * 1.15f);
        if (pelea == null) return y + alto + 4;
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        float pct0 = c0 == null ? 0 : (float) (c0.blindaje() / 100.0);
        float pct1 = c1 == null ? 0 : (float) (c1.blindaje() / 100.0);
        float barraW = Math.max(50, w * 0.28f);
        float barraH = 8;
        float by = y + (alto - barraH) / 2;
        dibujarBarra(p, x, by, barraW, barraH, pct0, colorJugador);
        dibujarBarra(p, x + w - barraW, by, barraW, barraH, pct1, colorMuneco);
        String centro = pelea.estado() == Pelea.Estado.TERMINADA ? T.t("banco.terminada")
                : pausado ? T.t("banco.en_pausa") : T.f("banco.turno", pelea.turno());
        float espacioCentro = Math.max(40, w - barraW * 2 - 16);
        float tCen = ui.cabe(centro, espacioCentro, Math.min(ui.letra * 0.78f, 18), false);
        p.textoCentrado(ui.recortarTexto(centro, espacioCentro, tCen, false), x + w / 2, y + (alto - tCen * 1.15f) / 2,
                tCen, false, ui.tema.textoSuave, 1);
        return y + alto + 4;
    }

    private static void dibujarBarra(Pincel p, float x, float y, float w, float h, float pct, int color) {
        float v = Math.max(0, Math.min(1, pct));
        p.caja(x, y, w, h, 4, 0x1A202C, 0.9f);
        if (v > 0) p.caja(x, y, w * v, h, 4, color, 1);
    }

    private String textoPie(RobotDelJugador probador, boolean compilando) {
        if (compilando) return T.t("banco.compilando");
        if (probador == null || !probador.listo()) return T.t("banco.sin_compilar");
        if (yo != null && !yo.ultimoError.isEmpty()) return "⚠ " + yo.ultimoError;
        if (pelea != null && pelea.estado() == Pelea.Estado.TERMINADA) return T.t("banco.terminada");
        if (modo == Modo.APARATO && pelea != null) {
            Aparatos.Tipo sig = pelea.aparatos.siguiente();
            if (sig != null) return T.f("banco.aparato_prox", sig.nombre);
        }
        if (yo != null && !yo.consola.isEmpty()) return yo.consola.get(yo.consola.size() - 1);
        return T.t("banco.pista");
    }

    private int colorPie(Ui ui, RobotDelJugador probador, boolean compilando) {
        if (compilando) return ui.tema.acento;
        if (probador == null || !probador.listo()) return ui.tema.aviso;
        if (yo != null && !yo.ultimoError.isEmpty()) return ui.tema.mal;
        if (pelea != null && pelea.estado() == Pelea.Estado.TERMINADA) return ui.tema.acento;
        return ui.tema.textoSuave;
    }

    private static String firmaDeRobot(Jugador.Robot r) {
        return r.nombre + "|" + r.modelo + "|" + r.piezas;
    }

    /** Muñeco configurable para el banco de pruebas: quieto o en patrulla suave, sin dañar estadísticas. */
    private static final class MunecoDeBanco extends Metajavbot {

        private final Modo modo;

        MunecoDeBanco(Modo modo) {
            this.modo = modo;
        }

        @Override
        public void cadaTurno() {
            if (modo != Modo.MOVIMIENTO) return;
            if (restanteAvance() == 0) {
                avanzar(150);
                girar(28);
            }
            if (x() < 90 || y() < 90 || x() > anchoArena() - 90 || y() > altoArena() - 90) {
                girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2));
            }
        }
    }
}
