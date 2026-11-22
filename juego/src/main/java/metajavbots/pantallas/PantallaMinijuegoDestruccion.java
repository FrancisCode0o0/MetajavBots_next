package metajavbots.pantallas;

import java.util.List;
import java.util.Locale;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.MinijuegoDestruccion;
import metajavbots.combate.Pelea;
import metajavbots.combate.PistaDeCarrera;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * PANTALLA MINIJUEGO DESTRUCCIÓN — Reto especial de destrucción y supervivencia
 * en arena 3× con cuenta atrás, cámara de seguimiento fluida, minimapa compacto,
 * selector directo de modelos/piezas/dificultad/subnivel idéntico al de la
 * Carrera de obstáculos y editor de código integrado.
 */
public final class PantallaMinijuegoDestruccion implements Pantalla {

    private static final double VENTANA_CAMARA = 1150.0;

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final RobotDelJugador compilado = new RobotDelJugador();
    private VistaDeArena vista;
    private MinijuegoDestruccion reto;
    private long semilla = (System.nanoTime() & 0xFFFFFL) + 41L;
    private String escenarioAnterior;
    private String escenarioActual;
    private DificultadCarrera dificultad = DificultadCarrera.porDefecto();
    private int subnivel = DificultadCarrera.SUBNIVEL_MIN;
    private int robotElegido;
    private boolean preparando = true;
    private boolean pausado;
    private int velocidad = 1;
    private float acumulador;
    private float tiempo;
    private String aviso = "";
    private boolean cobroMonedas;
    private boolean finProcesado;
    private volatile boolean compilando;
    private volatile Compilador.Resultado resultadoCompilacion;
    private boolean arrancarTrasCompilar;

    /** Comprueba si la dificultad está desbloqueada para el jugador (misma regla que Carrera de obstáculos). */
    public static boolean dificultadDesbloqueada(Jugador jugador, DificultadCarrera dificultad) {
        return PantallaCarreraDeObstaculos.dificultadDesbloqueada(jugador, dificultad);
    }

    /** Selecciona la dificultad si está desbloqueada para el jugador; devuelve si se aplicó. */
    public boolean seleccionarDificultad(Jugador jugador, DificultadCarrera nueva) {
        if (!dificultadDesbloqueada(jugador, nueva)) return false;
        dificultad = nueva;
        return true;
    }

    /** Dificultad actualmente seleccionada en el menú del reto especial. */
    public DificultadCarrera dificultadSeleccionada() {
        return dificultad;
    }

    /** Selecciona el subnivel (1 = I, 2 = II, 3 = III). */
    public boolean seleccionarSubnivel(int nuevo) {
        subnivel = DificultadCarrera.normalizarSubnivel(nuevo);
        return true;
    }

    /** Subnivel actualmente seleccionado (1..3). */
    public int subnivelSeleccionado() {
        return subnivel;
    }

    /** Selecciona uno de los robots del jugador. */
    public boolean seleccionarRobot(Jugador jugador, int indice) {
        if (jugador == null || jugador.robots.isEmpty()) return false;
        int idx = Math.max(0, Math.min(indice, jugador.robots.size() - 1));
        robotElegido = idx;
        jugador.robotElegido = idx;
        return true;
    }

    /** Índice del robot actualmente seleccionado. */
    public int robotSeleccionado() {
        return robotElegido;
    }

    /** Lista de modelos de robot desbloqueados por el jugador. */
    public static List<Modelo> modelosDesbloqueados(Jugador jugador) {
        return PantallaCarreraDeObstaculos.modelosDesbloqueados(jugador);
    }

    /** Cambia el modelo del robot seleccionado a un modelo desbloqueado concreto. */
    public boolean seleccionarModelo(Jugador jugador, String modeloId) {
        Jugador.Robot r = robotActual(jugador);
        if (r == null || modeloId == null || modeloId.isBlank()) return false;
        for (Modelo m : modelosDesbloqueados(jugador)) {
            if (m.id.equals(modeloId)) {
                if (!m.id.equals(r.modelo)) {
                    r.modelo = m.id;
                    r.piezas.clear();
                }
                return true;
            }
        }
        return false;
    }

    /** Rota entre los modelos desbloqueados del jugador ({@code paso} = -1 o +1). */
    public boolean cambiarModelo(Jugador jugador, int paso) {
        Jugador.Robot r = robotActual(jugador);
        if (r == null) return false;
        List<Modelo> opciones = modelosDesbloqueados(jugador);
        if (opciones.size() <= 1) return false;
        int idx = indiceDeModelo(opciones, r.modelo);
        int siguiente = Math.floorMod((idx < 0 ? 0 : idx) + paso, opciones.size());
        r.modelo = opciones.get(siguiente).id;
        r.piezas.clear();
        return true;
    }

    /** Identificador del modelo actualmente equipado en el robot seleccionado. */
    public String modeloSeleccionado(Jugador jugador) {
        Jugador.Robot r = robotActual(jugador);
        return r != null ? r.modelo : "cobalto";
    }

    /** Lista de piezas disponibles del inventario del jugador para un hueco concreto. */
    public static List<Pieza> piezasDisponiblesParaHueco(Jugador jugador, Jugador.Robot robot, Montaje.Hueco hueco) {
        return PantallaCarreraDeObstaculos.piezasDisponiblesParaHueco(jugador, robot, hueco);
    }

    /** Rota la pieza montada en {@code hueco} ({@code paso} = -1 o +1). */
    public boolean cambiarPieza(Jugador jugador, Montaje.Hueco hueco, int paso) {
        Jugador.Robot r = robotActual(jugador);
        if (r == null || hueco == null) return false;
        List<Pieza> opciones = piezasDisponiblesParaHueco(jugador, r, hueco);
        if (opciones.size() <= 1) return false;
        Montaje m = jugador.montajeDe(r);
        Pieza actual = m.pieza(hueco);
        int idx = indiceDePieza(opciones, actual);
        int siguiente = Math.floorMod((idx < 0 ? 0 : idx) + paso, opciones.size());
        m.poner(hueco, opciones.get(siguiente));
        r.ponerPiezas(m);
        return true;
    }

    /** Equipa una pieza específica por identificador si está disponible para el robot actual. */
    public boolean equiparPieza(Jugador jugador, Montaje.Hueco hueco, String piezaId) {
        Jugador.Robot r = robotActual(jugador);
        if (r == null || hueco == null || piezaId == null) return false;
        for (Pieza opcion : piezasDisponiblesParaHueco(jugador, r, hueco)) {
            if (opcion.id.equals(piezaId)) {
                Montaje m = jugador.montajeDe(r);
                m.poner(hueco, opcion);
                r.ponerPiezas(m);
                return true;
            }
        }
        return false;
    }

    /** Cifras reales ({@link Montaje.Cifras}) del robot actualmente seleccionado. */
    public Montaje.Cifras cifrasActuales(Jugador jugador) {
        Jugador.Robot r = robotActual(jugador);
        Montaje m = r != null ? jugador.montajeDe(r) : new Montaje(Modelo.de("cobalto"));
        return m.cifras();
    }

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        robotElegido = j.jugador != null ? j.jugador.robotElegido : 0;
        dificultad = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        escenarioActual = PistaDeCarrera.siguienteArena(escenarioActual, semilla);
        vista.escenario = escenarioDe(escenarioActual);
        editor.palabras = Palabras.api(j.jugador.idioma);
        editor.documentacion = w -> Referencia.de(w, j.jugador.idioma);
        editor.tamano = j.jugador.tamanoDeLetra;
        editor.alCompilar = () -> lanzarCompilacion(j, false);
        if (editor.doc.texto().isBlank()) {
            editor.ponerTexto(MinijuegoDestruccion.plantillaInicial(j.jugador.idioma));
        }
    }

    @Override
    public String contextoParaElEntrenador() {
        return EntrenadorFlotante.contextoDeCodigo(
                "The player is in Destruction & Survival (Special mode -> Destruction & Survival), "
                        + "difficulty " + dificultad.name() + " " + DificultadCarrera.etiquetaSubnivel(subnivel)
                        + " in a 3x arena with a 60s countdown, falling broken robots from the sky, sprouting ground objects/traps, "
                        + "very high incoming damage at least 2.0x Obstacle race (+60% more lethal than before), 0.1–0.6 coins per destroyed target, "
                        + "+0.5 bonus for surviving, and 1/4 loot on early death (0 on surrender).",
                editor);
    }

    @Override
    public String codigoParaConsejos() {
        return editor.doc.texto();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        tiempo += dt;
        atenderCompilacion(j);
        Comun.fondo(j, tiempo);
        float arriba = Comun.cabecera(j, T.t("destruccion.titulo"), true);
        if (preparando || reto == null) {
            dibujarPreparacion(j, arriba);
        } else {
            dibujarReto(j, arriba, dt);
        }
    }

    private void dibujarPreparacion(Juego j, float arriba) {
        Pincel p = j.pincel;
        float x = 20, y = arriba + 12, w = p.ancho - 40;
        float yTrasBarra = barraSeleccion(j, x, y, w);
        float altoCuerpo = p.alto - yTrasBarra - 16;
        float anchoReglas = Math.max(350, w * 0.40f);
        float anchoEditor = w - anchoReglas - 16;
        DibujoMinijuegoDestruccion.panelReglas(p, j.ui, dificultad, subnivel, x, yTrasBarra, anchoReglas, altoCuerpo);
        dibujarPanelEditor(j, x + anchoReglas + 16, yTrasBarra, anchoEditor, altoCuerpo);
    }

    private float barraSeleccion(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = 232;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        float pw = 132, ph = 136;
        Jugador.Robot rActual = robotActual(j.jugador);
        Montaje mActual = rActual != null ? j.jugador.montajeDe(rActual) : new Montaje(Modelo.de("cobalto"));
        DibujoCarreraDeObstaculos.vistaPreviaRobot(j, vista, mActual, tiempo, x + 14, y + 8, pw, ph);
        float rx = x + 14 + pw + 12;
        float rw = w - 28 - pw - 12;
        dibujarSelectorRobotYModelo(j, rx, y + 8, rw);
        dibujarSelectorPiezas(j, rx, y + 50, rw);
        float wNueva = Math.min(210, Math.max(150, p.ancho(T.t("destruccion.nueva_arena"), Math.min(ui.letra, 18.5f), true) + 30));
        String infoArena = T.f("destruccion.arena_info", escenarioActual, MinijuegoDestruccion.DURACION_SEGUNDOS);
        p.textoConBordeNegro(ui.recortarTexto(infoArena, Math.max(120, rw - wNueva - 14), Math.min(ui.letra * 0.80f, 18f), true),
                rx, y + 111, Math.min(ui.letra * 0.80f, 18f), true, 0xFFC857, 1);
        if (ui.boton(T.t("destruccion.nueva_arena"), rx + rw - wNueva, y + 102, wNueva, 36, Ui.Estilo.NORMAL)) {
            rotarArena();
        }
        dibujarSelectorDificultad(j, x + 14, y + 150, w - 28);
        return y + h + 10;
    }

    private void dibujarSelectorRobotYModelo(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Jugador.Robot> lista = j.jugador.robots;
        if (lista.isEmpty()) return;
        float tEt = Math.min(ui.letra * 0.80f, 18f);
        robotElegido = Math.max(0, Math.min(robotElegido, lista.size() - 1));
        Jugador.Robot r = lista.get(robotElegido);
        List<Modelo> modelos = modelosDesbloqueados(j.jugador);
        int idxMod = Math.max(0, indiceDeModelo(modelos, r.modelo));
        float cx = x;
        String etMod = T.t("carrera_obs.modelo");
        p.textoConBordeNegro(etMod, cx, y + 8, tEt, true, ui.tema.textoSuave, 1);
        cx += Math.max(66, p.ancho(etMod, tEt, true) + 8);
        boolean variosMod = modelos.size() > 1;
        if (ui.boton("◄", cx, y, 34, 36, Ui.Estilo.PLANO, variosMod, null)) {
            if (cambiarModelo(j.jugador, -1)) {
                j.guardar();
                j.sonidos.tocar("servo", 0.5f);
            }
        }
        cx += 38;
        String modTxt = "[" + (idxMod + 1) + "/" + Math.max(1, modelos.size()) + "] " + Modelo.de(r.modelo).nombre(T.idioma);
        float modW = Math.min(190, Math.max(115, w * 0.25f));
        p.textoConBordeNegro(ui.recortarTexto(modTxt, modW, tEt, true), cx, y + 8, tEt, true, ui.tema.texto, 1);
        cx += modW + 4;
        if (ui.boton("►", cx, y, 34, 36, Ui.Estilo.PLANO, variosMod, null)) {
            if (cambiarModelo(j.jugador, 1)) {
                j.guardar();
                j.sonidos.tocar("servo", 0.5f);
            }
        }
        cx += 46;
        if (lista.size() > 1) {
            if (ui.boton("◄", cx, y, 32, 36, Ui.Estilo.PLANO, true, null)) {
                seleccionarRobot(j.jugador, (robotElegido - 1 + lista.size()) % lista.size());
            }
            cx += 36;
            String robTxt = "[" + (robotElegido + 1) + "/" + lista.size() + "] " + r.nombre;
            float robW = Math.min(140, Math.max(84, w * 0.16f));
            p.textoConBordeNegro(ui.recortarTexto(robTxt, robW, tEt, true), cx, y + 8, tEt, true, ui.tema.textoSuave, 1);
            cx += robW + 4;
            if (ui.boton("►", cx, y, 32, 36, Ui.Estilo.PLANO, true, null)) {
                seleccionarRobot(j.jugador, (robotElegido + 1) % lista.size());
            }
            cx += 42;
        }
        Montaje.Cifras c = cifrasActuales(j.jugador);
        String cifrasTxt = String.format(Locale.ROOT, "VEL %.1f · BLIND %.0f", c.velocidad, c.blindajeTotal());
        p.textoConBordeNegro(ui.recortarTexto(cifrasTxt, Math.max(60, x + w - cx), tEt, true), cx, y + 8, tEt, true, 0x3FE0FF, 1);
    }

    private void dibujarSelectorPiezas(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador.Robot r = robotActual(j.jugador);
        if (r == null) return;
        Montaje m = j.jugador.montajeDe(r);
        Montaje.Hueco[] huecos = PantallaCarreraDeObstaculos.HUECOS_EDITABLES;
        float sep = 8;
        float cw = (w - sep * (huecos.length - 1)) / huecos.length;
        float ch = 46;
        float tHueco = Math.min(ui.letra * 0.62f, 13.5f);
        float tPieza = Math.min(ui.letra * 0.76f, 16.5f);
        for (int i = 0; i < huecos.length; i++) {
            Montaje.Hueco hueco = huecos[i];
            float cx = x + i * (cw + sep);
            p.caja(cx, y, cw, ch, 8, ui.tema.panelAlto, 0.92f);
            p.bordeRedondeado(cx, y, cw, ch, 8, ui.tema.borde, 0.7f, 1f);
            List<Pieza> opciones = piezasDisponiblesParaHueco(j.jugador, r, hueco);
            Pieza actual = m.pieza(hueco);
            boolean puede = opciones.size() > 1;
            if (ui.boton("◄", cx + 4, y + 6, 32, 34, Ui.Estilo.PLANO, puede, null)) {
                if (cambiarPieza(j.jugador, hueco, -1)) {
                    j.guardar();
                    j.sonidos.tocar("servo", 0.4f);
                }
            }
            if (ui.boton("►", cx + cw - 36, y + 6, 32, 34, Ui.Estilo.PLANO, puede, null)) {
                if (cambiarPieza(j.jugador, hueco, 1)) {
                    j.guardar();
                    j.sonidos.tocar("servo", 0.4f);
                }
            }
            float tx = cx + 40, tw = Math.max(40, cw - 80);
            String etHueco = T.t("hueco." + hueco.name().toLowerCase(Locale.ROOT));
            p.textoConBordeNegro(ui.recortarTexto(etHueco, tw, tHueco, false), tx, y + 4, tHueco, false, ui.tema.textoSuave, 1);
            String nomPieza = actual == null ? T.t("taller.sin_pieza") : actual.nombre(T.idioma);
            p.textoConBordeNegro(ui.recortarTexto(nomPieza, tw, tPieza, true), tx, y + 22, tPieza, true, ui.tema.texto, 1);
        }
    }

    private void dibujarSelectorDificultad(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        dificultad = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        float tEt = Math.min(ui.letra * 0.84f, 19f);
        String et = T.t("carrera_obs.dificultad");
        p.textoConBordeNegro(et, x, y + 7, tEt, true, ui.tema.textoSuave, 1);
        float dx = x + Math.max(98, p.ancho(et, tEt, true) + 12);
        DificultadCarrera[] niveles = DificultadCarrera.values();
        float sep = 8;
        float bw = Math.min(160, Math.max(96, (x + w - dx - sep * (niveles.length - 1)) / niveles.length));
        for (DificultadCarrera d : niveles) {
            boolean desbloqueada = dificultadDesbloqueada(j.jugador, d);
            String ayuda = desbloqueada ? d.resumen(T.idioma)
                    : d.requiereModoEspecial ? T.t("carrera_obs.dif_especial_bloqueada")
                    : T.f("carrera_obs.dif_bloqueada_rango", d.nombreRangoRequerido());
            if (ui.boton(d.nombre(T.idioma), dx, y, bw, 34, d == dificultad ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, desbloqueada, ayuda)) {
                seleccionarDificultad(j.jugador, d);
            }
            if (!desbloqueada) ui.candado(dx + 14, y + 17, 13, ui.tema.textoApagado, 0.95f);
            dx += bw + sep;
        }
        float ySub = y + 40;
        String etSub = T.t("carrera_obs.subnivel");
        p.textoConBordeNegro(etSub, x, ySub + 7, Math.min(ui.letra * 0.84f, 18.5f), true, ui.tema.textoSuave, 1);
        float sx = x + Math.max(98, p.ancho(etSub, Math.min(ui.letra * 0.84f, 18.5f), true) + 12);
        for (int s = DificultadCarrera.SUBNIVEL_MIN; s <= DificultadCarrera.SUBNIVEL_MAX; s++) {
            if (ui.boton(DificultadCarrera.etiquetaSubnivel(s), sx, ySub, 46, 34, s == subnivel ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                seleccionarSubnivel(s);
            }
            sx += 52;
        }
        double multDano = MinijuegoDestruccion.multiplicadorDanoPara(dificultad, subnivel);
        String res = dificultad.nombre(T.idioma) + " " + DificultadCarrera.etiquetaSubnivel(subnivel)
                + " · " + T.f("destruccion.regla_dano_corto", String.format(Locale.ROOT, "%.2f", multDano));
        p.textoConBordeNegro(ui.recortarTexto(res, Math.max(80, x + w - sx - 14), Math.min(ui.letra * 0.74f, 16.5f), false),
                sx + 14, ySub + 8, Math.min(ui.letra * 0.74f, 16.5f), false, 0xFF8A7A, 1);
    }

    private void dibujarPanelEditor(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String[] textos = {
                compilando ? T.t("taller.compilando") : T.t("taller.compilar"),
                T.t("taller.plantilla"),
                T.t("destruccion.iniciar")
        };
        Ui.Estilo[] estilos = {Ui.Estilo.COMPILAR, Ui.Estilo.PLANO, Ui.Estilo.DESTACADO};
        boolean[] activos = {!compilando, !compilando, !compilando};
        String[] ayudas = {T.t("taller.compilar_ayuda"), T.t("taller.plantilla_ayuda"), T.t("destruccion.iniciar_ayuda")};
        switch (ui.botonera(textos, estilos, activos, ayudas, x, y, w, 44)) {
            case 0:
                lanzarCompilacion(j, false);
                break;
            case 1:
                editor.doc.cambiarTodo(MinijuegoDestruccion.plantillaInicial(j.jugador.idioma));
                aviso = "";
                break;
            case 2:
                iniciarReto(j);
                break;
            default:
                break;
        }
        String pie = aviso.isEmpty() ? (!compilado.listo() ? T.t("taller.aviso_nunca_compilado")
                : !editor.doc.texto().equals(compilado.fuente()) ? T.t("taller.aviso_cambios_sin_compilar")
                : T.t("destruccion.pista_editor")) : aviso;
        float tPie = ui.letra * 0.78f;
        float altoPie = p.altoDeParrafo(pie, w, tPie) + 10;
        float ey = y + ui.altoDeBotonera + 10;
        float eh = y + h - ey - altoPie - 6;
        editor.dibujar(ui, x, ey, w, Math.max(140, eh));
        p.parrafo(pie, x, y + h - altoPie, w, tPie, aviso.isEmpty() ? ui.tema.textoApagado : ui.tema.textoSuave, 1);
    }

    private void lanzarCompilacion(Juego j, boolean arrancarDespues) {
        if (compilando) return;
        if (!Compilador.hayCompilador()) {
            aviso = T.t("taller.sin_jdk");
            return;
        }
        arrancarTrasCompilar = arrancarDespues;
        compilando = true;
        j.sonidos.tocar("compilar", 0.5f);
        String codigo = editor.doc.texto();
        Thread hilo = new Thread(() -> {
            resultadoCompilacion = compilado.compilar(codigo, j.jugador.idioma.locale(), false);
            compilando = false;
        }, "compilar-destruccion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void atenderCompilacion(Juego j) {
        if (resultadoCompilacion == null) return;
        Compilador.Resultado r = resultadoCompilacion;
        resultadoCompilacion = null;
        editor.marcas.clear();
        for (Compilador.Fallo f : r.errores) {
            editor.marcas.add(Explicador.marca(f, j.jugador));
        }
        aviso = r.bien ? T.f("taller.compilado_ok", r.milisegundos) : T.f("pelea.errores", r.graves().size());
        j.sonidos.tocar(r.bien ? "bien" : "mal", 0.6f);
        boolean arrancar = arrancarTrasCompilar;
        arrancarTrasCompilar = false;
        if (r.bien && arrancar) iniciarReto(j);
    }

    private void iniciarReto(Juego j) {
        dificultad = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        Combatiente cJug = MinijuegoDestruccion.combatienteDeJugador(j.jugador, robotElegido, compilado.fabricaSiListo());
        reto = new MinijuegoDestruccion(cJug, semilla, escenarioAnterior, dificultad, subnivel);
        escenarioActual = reto.escenario;
        vista.escenario = escenarioDe(escenarioActual);
        vista.enfocarCamara(MinijuegoDestruccion.ANCHO / 2.0, VENTANA_CAMARA);
        preparando = false;
        pausado = false;
        acumulador = 0;
        cobroMonedas = false;
        finProcesado = false;
    }

    private void rotarArena() {
        escenarioAnterior = escenarioActual;
        semilla = semilla * 6364136223846793005L + 1442695040888963407L;
        escenarioActual = PistaDeCarrera.siguienteArena(escenarioAnterior, semilla);
        if (vista != null) vista.escenario = escenarioDe(escenarioActual);
    }

    private void dibujarReto(Juego j, float arriba, float dt) {
        Pincel p = j.pincel;
        float x = 20, y = arriba + 10, w = p.ancho - 40, hudH = 66;
        int accionHud = DibujoMinijuegoDestruccion.hud(j, reto, pausado, velocidad, x, y, w, hudH);
        procesarAccionHud(j, accionHud);
        if (preparando) return;
        if (!pausado && !reto.terminada()) {
            acumulador += dt * Pelea.TURNOS_POR_SEGUNDO * velocidad;
            while (acumulador >= 1 && !reto.terminada()) {
                reto.avanzar();
                vista.sucesos(reto.pelea, j.sonidos);
                acumulador -= 1;
            }
        }
        if (reto.terminada() && !finProcesado) {
            finProcesado = true;
            cobroMonedas = reto.aplicarRecompensa(j.jugador);
            if (cobroMonedas) {
                j.guardar();
                j.sonidos.tocar("subida", 0.6f);
            }
        }
        Cuerpo c0 = reto.pelea.cuerpo(0);
        if (c0 != null) vista.objetivoCamaraX = c0.x;
        float ay = y + hudH + 10, ah = p.alto - ay - 16;
        vista.dibujar(p, reto.pelea, x, ay, w, ah, dt);
        DibujoMinijuegoDestruccion.avisosEnArena(p, vista, reto);
        float mmW = Math.min(w - 40, 520), mmH = 44;
        DibujoMinijuegoDestruccion.minimapa(p, j.ui, reto, vista, x + (w - mmW) / 2, ay + ah - mmH - 12, mmW, mmH);
        if (reto.terminada()) {
            int accionFin = DibujoMinijuegoDestruccion.resultado(j, reto, cobroMonedas, x, ay, w, ah);
            if (accionFin == 1) {
                rotarArena();
                iniciarReto(j);
            } else if (accionFin == 2) {
                rotarArena();
                preparando = true;
            } else if (accionFin == 3) {
                rotarArena();
                iniciarReto(j);
            }
        }
    }

    private void procesarAccionHud(Juego j, int accion) {
        if (accion == 1) pausado = !pausado;
        else if (accion == 2) velocidad = velocidad == 1 ? 2 : velocidad == 2 ? 4 : 1;
        else if (accion == 3) {
            rotarArena();
            preparando = true;
        } else if (accion == 4) {
            rotarArena();
            iniciarReto(j);
        } else if (accion == 5 && !reto.terminada()) {
            reto.rendirse();
        }
    }

    private Jugador.Robot robotActual(Jugador jugador) {
        if (jugador == null || jugador.robots.isEmpty()) return null;
        robotElegido = Math.max(0, Math.min(robotElegido, jugador.robots.size() - 1));
        return jugador.robots.get(robotElegido);
    }

    private static int indiceDeModelo(List<Modelo> opciones, String modeloId) {
        if (modeloId == null) return -1;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).id.equals(modeloId)) return i;
        }
        return -1;
    }

    private static int indiceDePieza(List<Pieza> opciones, Pieza actual) {
        if (actual == null) return -1;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).id.equals(actual.id)) return i;
        }
        return -1;
    }

    private static VistaDeArena.Escenario escenarioDe(String nombre) {
        if (nombre == null) return VistaDeArena.Escenario.CHATARRERIA;
        try {
            return VistaDeArena.Escenario.valueOf(nombre);
        } catch (IllegalArgumentException ex) {
            return VistaDeArena.Escenario.CHATARRERIA;
        }
    }
}
