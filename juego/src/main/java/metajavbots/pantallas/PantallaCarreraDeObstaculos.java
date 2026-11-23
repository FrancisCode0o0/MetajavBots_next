package metajavbots.pantallas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Arena;
import metajavbots.combate.CarreraDeObstaculos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Dialogo;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.T;

/**
 * PANTALLA CARRERA DE OBSTÁCULOS — Selección de robot del jugador, elección de
 * rival (máquina o robots_ajenos/), rotación de arena 6×, mapa general parcial,
 * editor trilingüe, cámara de seguimiento suave, minimapa panorámico y premio
 * de 1 moneda por victoria.
 */
public final class PantallaCarreraDeObstaculos implements Pantalla {

    /** Resultado de cargar y validar un robot ajeno para la carrera bajo Vigilante y Cargador. */
    public record CargaAjeno(Combatiente combatiente, String error) {
        /** Indica si la carga y compilación salieron bien. */
        public boolean bien() {
            return combatiente != null;
        }
    }

    private static final double VENTANA_CAMARA = 1400.0;
    /** Huecos editables directamente desde el menú de la Carrera de obstáculos. */
    public static final Montaje.Hueco[] HUECOS_EDITABLES = {
            Montaje.Hueco.CABEZA, Montaje.Hueco.IZQUIERDA, Montaje.Hueco.DERECHA, Montaje.Hueco.PIERNAS
    };

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final VentanaDeCodigo visorEnVivo = new VentanaDeCodigo();
    private final RobotDelJugador compilado = new RobotDelJugador();
    private final SelectorDeMinijuego selector = new SelectorDeMinijuego();
    private final Dialogo dialogo = new Dialogo();
    private VistaDeArena vista;
    private CarreraDeObstaculos carrera;
    private Arena vistaPrevia;
    private long semilla = (System.nanoTime() & 0xFFFFFL) + 17L;
    private String escenarioAnterior;
    private String escenarioActual;
    private DificultadCarrera dificultad = DificultadCarrera.porDefecto();
    private int subnivel = DificultadCarrera.SUBNIVEL_MIN;
    private int robotElegido;
    private boolean preparando = true;
    private boolean contraMaquina = true;
    private List<Path> ajenos = new ArrayList<>();
    private int ajenoElegido;
    private boolean pausado;
    private boolean verCodigoEnVivo;
    private boolean reintentarAlGuardar;
    private boolean rotarEnSiguienteCarrera;
    private String fuenteVisorEnVivo = "";
    private int velocidad = 1;
    private float acumulador;
    private float tiempo;
    private String aviso = "";
    private boolean monedaGanada;
    private boolean finProcesado;
    private volatile boolean compilando;
    private volatile Compilador.Resultado resultadoCompilacion;
    private boolean arrancarTrasCompilar;

    @Override
    public String musica() {
        if (carrera != null && carrera.terminada()) {
            return carrera.ganador() == 0 ? "victoria" : "derrota";
        }
        if (!preparando && carrera != null) {
            double avance = Math.max(carrera.progreso(0), carrera.progreso(1));
            if (avance >= 0.72) return "carrera_recta";
        }
        return "carrera";
    }

    /** Carga un archivo .java de robots_ajenos/ reutilizando Exportar, Compilador, Vigilante y Cargador. */
    public static CargaAjeno cargarRivalAjeno(Path archivo) {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            return new CargaAjeno(null, T.t("carrera_obs.sin_ajenos"));
        }
        if (!Compilador.hayCompilador()) {
            return new CargaAjeno(null, T.t("taller.sin_jdk"));
        }
        try {
            String codigo = Files.readString(archivo, StandardCharsets.UTF_8);
            RobotDelJugador fabrica = new RobotDelJugador();
            Compilador.Resultado res = fabrica.compilar(codigo, Locale.ROOT);
            if (!res.bien) {
                String detalle = res.graves().isEmpty() ? "" : res.graves().get(0).mensaje;
                return new CargaAjeno(null, T.f("pelear.ajeno_no_compila", detalle));
            }
            Montaje montaje = Exportar.montajeDe(codigo);
            String nombre = archivo.getFileName().toString().replace(".java", "");
            Combatiente rival = new Combatiente(nombre, montaje, fabrica);
            rival.computadora = true;
            return new CargaAjeno(rival, "");
        } catch (Exception ex) {
            return new CargaAjeno(null, ex.toString());
        }
    }

    /** Comprueba si una dificultad de Carrera de obstáculos está desbloqueada para el jugador. */
    public static boolean dificultadDesbloqueada(Jugador jugador, DificultadCarrera dificultad) {
        return CarreraDeObstaculos.dificultadDesbloqueada(jugador, dificultad);
    }

    /** Intenta seleccionar una dificultad respetando el desbloqueo progresivo por rango; devuelve si se aplicó. */
    public boolean seleccionarDificultad(Jugador jugador, DificultadCarrera nueva) {
        if (!dificultadDesbloqueada(jugador, nueva)) return false;
        dificultad = nueva;
        actualizarVistaPrevia();
        return true;
    }

    /** Devuelve la dificultad actualmente seleccionada en el menú de la carrera. */
    public DificultadCarrera dificultadSeleccionada() {
        return dificultad;
    }

    /** Selecciona el subnivel de dificultad (1 = I, 2 = II, 3 = III). */
    public boolean seleccionarSubnivel(int nuevo) {
        int normalizado = DificultadCarrera.normalizarSubnivel(nuevo);
        subnivel = normalizado;
        actualizarVistaPrevia();
        return true;
    }

    /** Devuelve el subnivel actualmente seleccionado (1..3). */
    public int subnivelSeleccionado() {
        return subnivel;
    }

    /** Selecciona un robot disponible del inventario del jugador y actualiza el índice activo. */
    public boolean seleccionarRobot(Jugador jugador, int indice) {
        if (jugador == null || jugador.robots.isEmpty()) return false;
        int idx = Math.max(0, Math.min(indice, jugador.robots.size() - 1));
        robotElegido = idx;
        jugador.robotElegido = idx;
        return true;
    }

    /** Índice del robot actualmente seleccionado en el menú de la carrera. */
    public int robotSeleccionado() {
        return robotElegido;
    }

    /** Lista de todos los modelos de robot desbloqueados por el jugador (igual que la pestaña Modelo del Taller). */
    public static List<Modelo> modelosDesbloqueados(Jugador jugador) {
        if (jugador == null) return List.of();
        return Progreso.modelosDisponibles(jugador);
    }

    /** Cambia el modelo del robot seleccionado a un modelo desbloqueado concreto, actualizando su montaje de fábrica. */
    public boolean seleccionarModelo(Jugador jugador, String modeloId) {
        boolean ok = SelectorDeMinijuego.seleccionarModelo(jugador, robotActual(jugador), modeloId);
        if (ok) selector.invalidar();
        return ok;
    }

    /** Rota entre los modelos desbloqueados del jugador ({@code paso} = -1 o +1) y actualiza el montaje del robot. */
    public boolean cambiarModelo(Jugador jugador, int paso) {
        boolean ok = SelectorDeMinijuego.cambiarModelo(jugador, robotActual(jugador), paso);
        if (ok) selector.invalidar();
        return ok;
    }

    /** Identificador del modelo actualmente equipado en el robot seleccionado. */
    public String modeloSeleccionado(Jugador jugador) {
        Jugador.Robot r = robotActual(jugador);
        return r != null ? r.modelo : "cobalto";
    }

    private static int indiceDeModelo(List<Modelo> opciones, String modeloId) {
        if (modeloId == null) return -1;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).id.equals(modeloId)) return i;
        }
        return -1;
    }

    /** Lista de piezas disponibles/usables del inventario del jugador para un hueco concreto del robot. */
    public static List<Pieza> piezasDisponiblesParaHueco(Jugador jugador, Jugador.Robot robot, Montaje.Hueco hueco) {
        return SelectorDeMinijuego.piezasDisponiblesParaHueco(jugador, robot, hueco);
    }

    /** Rota la pieza montada en {@code hueco} ({@code paso} = -1 o +1) respetando el inventario y compatibilidad. */
    public boolean cambiarPieza(Jugador jugador, Montaje.Hueco hueco, int paso) {
        boolean ok = SelectorDeMinijuego.cambiarPieza(jugador, robotActual(jugador), hueco, paso);
        if (ok) selector.invalidar();
        return ok;
    }

    /** Equipa una pieza específica por identificador si está disponible para el robot actual. */
    public boolean equiparPieza(Jugador jugador, Montaje.Hueco hueco, String piezaId) {
        boolean ok = SelectorDeMinijuego.equiparPieza(jugador, robotActual(jugador), hueco, piezaId);
        if (ok) selector.invalidar();
        return ok;
    }

    /** Cifras reales ({@link Montaje.Cifras}) del robot actualmente seleccionado con su montaje actual. */
    public Montaje.Cifras cifrasActuales(Jugador jugador) {
        return selector.montaje(jugador, robotActual(jugador)).cifras();
    }

    private Jugador.Robot robotActual(Jugador jugador) {
        if (jugador == null || jugador.robots.isEmpty()) return null;
        robotElegido = Math.max(0, Math.min(robotElegido, jugador.robots.size() - 1));
        return jugador.robots.get(robotElegido);
    }

    private void actualizarVistaPrevia() {
        if (escenarioActual != null) {
            vistaPrevia = Arena.carreraDeObstaculos(CarreraDeObstaculos.anchoPara(dificultad),
                    CarreraDeObstaculos.ALTO, semilla, escenarioActual, dificultad, subnivel);
        }
    }

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        ajenos = PrepararPelea.robotsAjenos();
        robotElegido = j.jugador != null ? j.jugador.robotElegido : 0;
        dificultad = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        escenarioActual = CarreraDeObstaculos.siguienteArena(escenarioActual, semilla);
        vista.escenario = escenarioDe(escenarioActual);
        actualizarVistaPrevia();
        editor.palabras = Palabras.api(j.jugador.idioma);
        editor.documentacion = w -> Referencia.de(w, j.jugador.idioma);
        editor.tamano = j.jugador.tamanoDeLetra;
        editor.alCompilar = () -> lanzarCompilacion(j, reintentarAlGuardar);
        visorEnVivo.soloLectura = true;
        visorEnVivo.seguirActiva = true;
        visorEnVivo.palabras = Palabras.api(j.jugador.idioma);
        if (editor.doc.texto().isBlank()) {
            editor.ponerTexto(CarreraDeObstaculos.plantillaInicial(j.jugador.idioma));
        }
    }

    @Override
    public String contextoParaElEntrenador() {
        return EntrenadorFlotante.contextoDeCodigo(
                "The player is in Obstacle race (direct access from Main Menu -> Arena and Special mode), difficulty "
                        + dificultad.name() + " " + DificultadCarrera.etiquetaSubnivel(subnivel)
                        + " (Noob/Junior/Senior/Special unlock progressively by player rank + Special mode, each with sublevels I/II/III; "
                        + "rewards scale up with difficulty and sublevel; losing without breaking or collecting anything gives 0 coins, "
                        + "losing with bonuses gives 1/4 of their value, winning gives 100%, and pressing Surrender ends immediately with 0 rewards). "
                        + "The player can choose and live-preview unlocked robot models, switch robots, and swap available/purchased parts "
                        + "(head, arms, legs) directly in the race menu.",
                editor);
    }

    @Override
    public String codigoParaConsejos() {
        return editor.doc.texto();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        tiempo += dt;
        dialogo.antes(j.ui);
        atenderCompilacion(j);
        Comun.fondo(j, tiempo);
        float arriba = Comun.cabecera(j, T.t("especial.carrera"), () -> alPulsarVolverOEscape(j));
        if (preparando || carrera == null) {
            dibujarPreparacion(j, arriba);
        } else {
            dibujarCarrera(j, arriba, dt);
        }
        dialogo.dibujar(j.ui);
    }

    private void alPulsarVolverOEscape(Juego j) {
        if (!preparando && carrera != null && !carrera.terminada()) {
            preguntarSalidaEnCarrera();
        } else {
            j.volver();
        }
    }

    /** Abre el cuadro de confirmación de Escape durante la carrera («Seguir», «Editar el código y reintentar», «Rendirme»). */
    private void preguntarSalidaEnCarrera() {
        boolean estabaPausado = pausado;
        pausado = true;
        dialogo.elegirTres(
                T.t("carrera_obs.salir_titulo"),
                T.t("carrera_obs.salir_texto"),
                T.t("comun.seguir"),
                T.t("carrera_obs.editar_reintentar"),
                T.t("carrera_obs.rendirme"),
                () -> pausado = estabaPausado,
                () -> {
                    reintentarAlGuardar = true;
                    rotarEnSiguienteCarrera = false;
                    preparando = true;
                },
                () -> {
                    pausado = false;
                    rotarEnSiguienteCarrera = true;
                    if (carrera != null && !carrera.terminada()) carrera.rendirse();
                });
    }

    private void dibujarPreparacion(Juego j, float arriba) {
        Pincel p = j.pincel;
        float x = 20, y = arriba + 12, w = p.ancho - 40;
        float yTrasBarra = barraSeleccion(j, x, y, w);
        float altoCuerpo = p.alto - yTrasBarra - 16;
        float anchoMapa = Math.max(360, w * 0.42f);
        float anchoEditor = w - anchoMapa - 16;
        dibujarPanelMapa(j, x, yTrasBarra, anchoMapa, altoCuerpo);
        dibujarPanelEditor(j, x + anchoMapa + 16, yTrasBarra, anchoEditor, altoCuerpo);
    }

    private float barraSeleccion(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = contraMaquina ? 236 : 280;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        float pw = 132, ph = 138;
        Jugador.Robot rActual = robotActual(j.jugador);
        Montaje mActual = selector.montaje(j.jugador, rActual);
        DibujoCarreraDeObstaculos.vistaPreviaRobot(j, vista, mActual, tiempo, x + 14, y + 8, pw, ph);
        float rx = x + 14 + pw + 12;
        float rw = w - 28 - pw - 12;
        dibujarSelectorRobotYModelo(j, rx, y + 8, rw);
        selector.dibujarSelectorPiezas(j, rActual, rx, y + 50, rw);
        float bx = rx, by = y + 102;
        float tEt = Math.min(ui.letra * 0.84f, 19f);
        p.textoConBordeNegro(T.t("carrera_obs.rival"), bx, by + 9, tEt, true, ui.tema.textoSuave, 1);
        bx += Math.max(74, p.ancho(T.t("carrera_obs.rival"), tEt, true) + 12);
        float wMaq = Math.min(220, Math.max(155, rw * 0.22f));
        float wUsu = Math.min(248, Math.max(170, rw * 0.25f));
        if (ui.boton(T.t("carrera_obs.vs_maquina"), bx, by, wMaq, 38, contraMaquina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
            contraMaquina = true;
            aviso = "";
        }
        bx += wMaq + 10;
        if (ui.boton(T.t("carrera_obs.vs_usuario"), bx, by, wUsu, 38, !contraMaquina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
            contraMaquina = false;
            ajenos = PrepararPelea.robotsAjenos();
            aviso = "";
        }
        float wNueva = Math.min(204, Math.max(150, p.ancho(T.t("carrera_obs.nueva_pista"), Math.min(ui.letra, 19f), true) + 32));
        if (ui.boton(T.t("carrera_obs.nueva_pista"), rx + rw - wNueva, by, wNueva, 38, Ui.Estilo.NORMAL)) {
            regenerarPista();
        }
        dibujarSelectorDificultad(j, x + 14, y + 154, w - 28);
        if (!contraMaquina) {
            dibujarSelectorAjeno(j, x + 14, y + 234, w - 28);
        }
        return y + h + 10;
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
            boolean elegida = d == dificultad;
            String texto = d.nombre(T.idioma);
            String ayuda = desbloqueada ? d.resumen(T.idioma)
                    : d.requiereModoEspecial ? T.t("carrera_obs.dif_especial_bloqueada")
                    : T.f("carrera_obs.dif_bloqueada_rango", d.nombreRangoRequerido());
            Ui.Estilo estilo = elegida ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
            if (ui.boton(texto, dx, y, bw, 34, estilo, desbloqueada, ayuda)) {
                seleccionarDificultad(j.jugador, d);
            }
            if (!desbloqueada) {
                ui.candado(dx + 14, y + 17, 13, ui.tema.textoApagado, 0.95f);
            }
            dx += bw + sep;
        }
        float ySub = y + 40;
        float finSubX = dibujarSelectorSubnivel(j, x, ySub);
        float resX = finSubX + 14;
        float resW = Math.max(80, x + w - resX);
        float tRes = Math.min(ui.letra * 0.74f, 16.5f);
        String multTxt = T.f("carrera_obs.multiplicador",
                String.format(Locale.ROOT, "%.2f", dificultad.multiplicadorRecompensa(subnivel)));
        String resumen = dificultad.nombre(T.idioma) + " " + DificultadCarrera.etiquetaSubnivel(subnivel)
                + " (" + multTxt + "): " + dificultad.resumen(T.idioma);
        p.textoConBordeNegro(ui.recortarTexto(resumen, resW, tRes, false), resX, ySub + 8, tRes, false, ui.tema.textoSuave, 1);
    }

    private float dibujarSelectorSubnivel(Juego j, float x, float y) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float tEt = Math.min(ui.letra * 0.84f, 18.5f);
        String et = T.t("carrera_obs.subnivel");
        p.textoConBordeNegro(et, x, y + 7, tEt, true, ui.tema.textoSuave, 1);
        float sx = x + Math.max(98, p.ancho(et, tEt, true) + 12);
        float bw = 46;
        for (int s = DificultadCarrera.SUBNIVEL_MIN; s <= DificultadCarrera.SUBNIVEL_MAX; s++) {
            String rom = DificultadCarrera.etiquetaSubnivel(s);
            Ui.Estilo estilo = s == subnivel ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
            if (ui.boton(rom, sx, y, bw, 34, estilo)) {
                seleccionarSubnivel(s);
            }
            sx += bw + 6;
        }
        return sx;
    }

    private void dibujarSelectorRobotYModelo(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Jugador.Robot> lista = j.jugador.robots;
        float tEt = Math.min(ui.letra * 0.80f, 18f);
        String arenaTxt = T.f("carrera_obs.arena_actual", escenarioActual);
        float arenaW = Math.max(145, p.ancho(arenaTxt, tEt, true) + 8);
        p.textoConBordeNegro(arenaTxt, x + w - arenaW, y + 8, tEt, true, 0xFFC857, 1);
        float utilW = Math.max(320, w - arenaW - 12);
        float cx = x;
        if (!lista.isEmpty()) {
            robotElegido = Math.max(0, Math.min(robotElegido, lista.size() - 1));
            Jugador.Robot r = lista.get(robotElegido);
            List<Modelo> modelos = selector.modelos(j.jugador);
            int idxMod = Math.max(0, indiceDeModelo(modelos, r.modelo));
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
            String modTxt = "[" + (idxMod + 1) + "/" + Math.max(1, modelos.size()) + "] "
                    + Modelo.de(r.modelo).nombre(T.idioma);
            float modW = Math.min(185, Math.max(115, utilW * 0.24f));
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
                float robW = Math.min(140, Math.max(84, utilW * 0.16f));
                p.textoConBordeNegro(ui.recortarTexto(robTxt, robW, tEt, true), cx, y + 8, tEt, true, ui.tema.textoSuave, 1);
                cx += robW + 4;
                if (ui.boton("►", cx, y, 32, 36, Ui.Estilo.PLANO, true, null)) {
                    seleccionarRobot(j.jugador, (robotElegido + 1) % lista.size());
                }
                cx += 42;
            }
            Montaje m = j.jugador.montajeDe(r);
            Montaje.Cifras c = m != null ? m.cifras() : new Montaje(Modelo.de("cobalto")).cifras();
            String cifrasTxt = String.format(Locale.ROOT, "VEL %.1f · BLIND %.0f", c.velocidad, c.blindajeTotal());
            float restW = Math.max(60, x + utilW - cx);
            p.textoConBordeNegro(ui.recortarTexto(cifrasTxt, restW, tEt, true), cx, y + 8, tEt, true, 0x3FE0FF, 1);
        }
    }

    private void dibujarSelectorAjeno(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (ajenos.isEmpty()) {
            p.texto(ui.recortarTexto(T.t("carrera_obs.sin_ajenos"), w - 180, ui.letra * 0.8f, false),
                    x, y + 10, ui.letra * 0.8f, false, ui.tema.aviso, 1);
        } else {
            ajenoElegido = Math.max(0, Math.min(ajenoElegido, ajenos.size() - 1));
            if (ui.boton("◄", x, y, 44, 38, Ui.Estilo.PLANO, ajenos.size() > 1, null)) {
                ajenoElegido = (ajenoElegido - 1 + ajenos.size()) % ajenos.size();
            }
            String nombre = ajenos.get(ajenoElegido).getFileName().toString();
            p.texto(ui.recortarTexto(nombre, w - 290, ui.letra * 0.9f, true), x + 56, y + 9, ui.letra * 0.9f, true, ui.tema.texto, 1);
            if (ui.boton("►", x + w - 220, y, 44, 38, Ui.Estilo.PLANO, ajenos.size() > 1, null)) {
                ajenoElegido = (ajenoElegido + 1) % ajenos.size();
            }
        }
        if (ui.boton(T.t("carrera_obs.refrescar"), x + w - 166, y, 166, 38, Ui.Estilo.PLANO)) {
            ajenos = PrepararPelea.robotsAjenos();
        }
    }

    private void dibujarPanelMapa(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cy = ui.panel(x, y, w, h, T.t("carrera_obs.mapa_titulo"));
        float mx = x + 16, mw = w - 32;
        float mh = Math.min(h * 0.36f, mw * 0.42f);
        DibujoCarreraDeObstaculos.mapaGeneral(p, ui, vistaPrevia, CarreraDeObstaculos.anchoPara(dificultad),
                CarreraDeObstaculos.metaPara(dificultad), mx, cy + 4, mw, mh);
        float ly = cy + mh + 10;
        float paso = ui.letra * 1.06f;
        float tl = ui.letra * 0.66f;
        p.circulo(mx + 9, ly + 7, 5.5f, 0xD97236, 1);
        p.texto(T.t("carrera_obs.leyenda_destructible"), mx + 24, ly, tl, false, ui.tema.texto, 1);
        ly += paso;
        p.rect(mx + 3, ly + 1, 11, 11, 0xE05A4F, 1);
        p.texto(T.t("carrera_obs.leyenda_indestructible"), mx + 24, ly, tl, false, ui.tema.texto, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 6.5f, 0xFFC857, 1);
        p.circulo(mx + 9, ly + 7, 4.5f, 0xFF1E42, 1);
        p.texto(T.t("carrera_obs.leyenda_mortal"), mx + 24, ly, tl, true, 0xFF6B7A, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 5.5f, 0xFFD23F, 1);
        p.texto(T.t("carrera_obs.leyenda_moneda"), mx + 24, ly, tl, true, 0xFFD23F, 1);
        ly += paso;
        p.triangulo(mx + 3, ly + 12, mx + 15, ly + 12, mx + 15, ly + 2, 0xFF9F1C, 1);
        p.texto(T.t("carrera_obs.leyenda_rampa"), mx + 24, ly, tl, false, 0xFFC857, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 5.5f, 0x33A8E8, 0.95f);
        p.texto(T.t("carrera_obs.leyenda_agua"), mx + 24, ly, tl, false, 0x6FD3FF, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 5.5f, 0x3FE0FF, 0.95f);
        p.texto(T.t("carrera_obs.leyenda_impulso"), mx + 24, ly, tl, false, 0x9CFF57, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 5.5f, 0x2EE58D, 0.95f);
        p.texto(T.t("carrera_obs.leyenda_reparacion"), mx + 24, ly, tl, false, 0x2EE58D, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 5.5f, 0xFF7A45, 0.95f);
        p.texto(T.t("carrera_obs.leyenda_rotos"), mx + 24, ly, tl, false, ui.tema.aviso, 1);
        ly += paso;
        p.circulo(mx + 9, ly + 7, 5.5f, 0x3FE0FF, 0.45f);
        p.texto(T.t("carrera_obs.leyenda_ocultos"), mx + 24, ly, tl, true, ui.tema.textoSuave, 1);
        ly += paso + 4;
        p.parrafo(T.t("carrera_obs.mapa_ayuda"), mx, ly, mw, tl, ui.tema.textoSuave, 1);
    }

    private void dibujarPanelEditor(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String[] textos = {
                compilando ? T.t("taller.compilando") : T.t("taller.compilar"),
                T.t("taller.plantilla"),
                T.t("carrera_obs.iniciar")
        };
        Ui.Estilo[] estilos = {Ui.Estilo.COMPILAR, Ui.Estilo.PLANO, Ui.Estilo.DESTACADO};
        boolean[] activos = {!compilando, !compilando, !compilando};
        String[] ayudas = {T.t("taller.compilar_ayuda"), T.t("taller.plantilla_ayuda"), T.t("carrera_obs.iniciar_ayuda")};
        switch (ui.botonera(textos, estilos, activos, ayudas, x, y, w, 44)) {
            case 0:
                lanzarCompilacion(j, reintentarAlGuardar);
                break;
            case 1:
                editor.doc.cambiarTodo(CarreraDeObstaculos.plantillaInicial(j.jugador.idioma));
                aviso = "";
                break;
            case 2:
                reintentarAlGuardar = false;
                iniciarCarrera(j, rotarEnSiguienteCarrera);
                break;
            default:
                break;
        }
        String pie = aviso.isEmpty() ? (!compilado.listo() ? T.t("taller.aviso_nunca_compilado")
                : !editor.doc.texto().equals(compilado.fuente()) ? T.t("taller.aviso_cambios_sin_compilar")
                : T.t("carrera_obs.pista_editor")) : aviso;
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
            resultadoCompilacion = compilado.compilar(codigo, j.jugador.idioma.locale(), true);
            compilando = false;
        }, "compilar-carrera");
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
        if (r.bien && arrancar) {
            reintentarAlGuardar = false;
            iniciarCarrera(j, false);
        }
    }

    private void iniciarCarrera(Juego j, boolean rotarArena) {
        DificultadCarrera difEfectiva = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        dificultad = difEfectiva;
        Combatiente rival;
        if (contraMaquina) {
            rival = CarreraDeObstaculos.rivalMaquina(T.t("carrera_obs.maquina_nombre"), difEfectiva, subnivel);
        } else {
            if (ajenos.isEmpty()) {
                aviso = T.t("carrera_obs.sin_ajenos");
                return;
            }
            CargaAjeno carga = cargarRivalAjeno(ajenos.get(Math.max(0, Math.min(ajenoElegido, ajenos.size() - 1))));
            if (!carga.bien()) {
                aviso = carga.error();
                return;
            }
            rival = carga.combatiente();
        }
        if (rotarArena || rotarEnSiguienteCarrera) {
            regenerarPista();
            rotarEnSiguienteCarrera = false;
        }
        Combatiente cJug = CarreraDeObstaculos.combatienteDeJugador(j.jugador, robotElegido, compilado.fabricaSiListo());
        carrera = new CarreraDeObstaculos(cJug, rival, semilla, contraMaquina, escenarioAnterior, difEfectiva, subnivel);
        escenarioActual = carrera.escenario();
        vista.escenario = escenarioDe(escenarioActual);
        vista.enfocarCamara(VENTANA_CAMARA / 2, VENTANA_CAMARA);
        preparando = false;
        pausado = false;
        acumulador = 0;
        monedaGanada = false;
        finProcesado = false;
    }

    private void regenerarPista() {
        escenarioAnterior = escenarioActual;
        semilla = semilla * 6364136223846793005L + 1442695040888963407L;
        escenarioActual = CarreraDeObstaculos.siguienteArena(escenarioAnterior, semilla);
        if (vista != null) vista.escenario = escenarioDe(escenarioActual);
        actualizarVistaPrevia();
    }

    private void dibujarCarrera(Juego j, float arriba, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float x = 20, y = arriba + 10, w = p.ancho - 40;
        float hudH = 66;
        dibujarHudCarrera(j, x, y, w, hudH);
        if (!pausado && !carrera.terminada()) {
            acumulador += dt * Pelea.TURNOS_POR_SEGUNDO * velocidad;
            while (acumulador >= 1 && !carrera.terminada()) {
                carrera.avanzar();
                vista.sucesos(carrera.pelea, j.sonidos);
                acumulador -= 1;
            }
        }
        if (carrera.terminada() && !finProcesado) {
            finProcesado = true;
            monedaGanada = carrera.aplicarRecompensa(j.jugador);
            if (monedaGanada) {
                j.guardar();
                j.sonidos.tocar("subida", 0.6f);
            }
        }
        Cuerpo c0 = carrera.pelea.cuerpo(0);
        if (c0 != null) {
            vista.objetivoCamaraX = c0.x + 240;
        }
        float ay = y + hudH + 10;
        float ah = p.alto - ay - 16;
        float anchoCodigo = verCodigoEnVivo ? Math.min(500f, Math.max(360f, w * 0.36f)) : 0f;
        float aw = verCodigoEnVivo ? w - anchoCodigo - 12f : w;
        vista.dibujar(p, carrera.pelea, x, ay, aw, ah, dt);
        DibujoCarreraDeObstaculos.lineasPista(p, ui, vista, carrera.metaX());
        float mmW = Math.min(aw - 40, 680);
        float mmH = 44;
        DibujoCarreraDeObstaculos.minimapa(p, ui, carrera, vista, x + (aw - mmW) / 2, ay + ah - mmH - 12, mmW, mmH);
        if (verCodigoEnVivo) {
            dibujarPanelCodigoEnVivo(j, c0, x + aw + 12f, ay, anchoCodigo, ah);
        }
        if (carrera.terminada()) {
            int accion = DibujoCarreraDeObstaculos.resultado(j, carrera, monedaGanada, x, ay, aw, ah);
            if (accion == 1) {
                regenerarPista();
                iniciarCarrera(j, false);
            } else if (accion == 2) {
                regenerarPista();
                reintentarAlGuardar = false;
                preparando = true;
            } else if (accion == 3) {
                regenerarPista();
                iniciarCarrera(j, false);
            }
        }
    }

    private void dibujarPanelCodigoEnVivo(Juego j, Cuerpo c0, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String sufijo = c0 != null && c0.ultimaLinea > 0 ? " · " + T.t("mcm.linea") + " " + c0.ultimaLinea : "";
        String titulo = T.t("carrera_obs.codigo_vivo") + sufijo;
        float cy = ui.panel(x, y, w, h, titulo);
        String fuente = compilado.listo() ? compilado.fuente() : editor.doc.texto();
        if (!fuente.equals(fuenteVisorEnVivo)) {
            visorEnVivo.ponerTexto(fuente);
            fuenteVisorEnVivo = fuente;
        }
        visorEnVivo.tamano = Math.max(12f, Math.min(20f, ui.letra * 0.78f));
        if (c0 != null && compilado.listo()) {
            visorEnVivo.lineasActivas = c0.lineasDelTurno;
            visorEnVivo.lineaActiva = c0.ultimaLinea;
        } else {
            visorEnVivo.lineasActivas = new java.util.BitSet();
            visorEnVivo.lineaActiva = 0;
        }
        String estado = !compilado.listo() ? T.t("taller.aviso_nunca_compilado")
                : PanelDepurador.sinEjecucion(c0) ? PanelDepurador.motivoSinEjecucion(c0) : T.t("editor.solo_lectura");
        float tEst = Math.min(16f, ui.letra * 0.72f);
        float hEst = p.altoDeParrafo(estado, w - 24f, tEst) + 8f;
        visorEnVivo.dibujar(ui, x + 8f, cy, w - 16f, Math.max(80f, y + h - cy - hEst - 6f));
        p.parrafo(estado, x + 12f, y + h - hEst, w - 24f, tEst, !compilado.listo() ? ui.tema.aviso : ui.tema.textoApagado, 1);
    }

    private void dibujarHudCarrera(Juego j, float x, float y, float w, float h) {
        int accion = DibujoCarreraDeObstaculos.hudCarrera(j, carrera, pausado, velocidad, verCodigoEnVivo, x, y, w, h);
        if (accion == 1) {
            pausado = !pausado;
        } else if (accion == 2) {
            velocidad = velocidad == 1 ? 2 : velocidad == 2 ? 4 : 1;
        } else if (accion == 3) {
            verCodigoEnVivo = !verCodigoEnVivo;
        } else if (accion == 4) {
            regenerarPista();
            iniciarCarrera(j, false);
        } else if (accion == 5 && !carrera.terminada()) {
            preguntarSalidaEnCarrera();
        }
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
