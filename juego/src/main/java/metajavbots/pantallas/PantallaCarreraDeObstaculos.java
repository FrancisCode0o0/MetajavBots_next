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
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.CatalogoDePiezas;
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
    private final RobotDelJugador compilado = new RobotDelJugador();
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
    private int velocidad = 1;
    private float acumulador;
    private float tiempo;
    private String aviso = "";
    private boolean monedaGanada;
    private boolean finProcesado;
    private volatile boolean compilando;
    private volatile Compilador.Resultado resultadoCompilacion;
    private boolean arrancarTrasCompilar;

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

    /** Lista de piezas disponibles/usables del inventario del jugador para un hueco concreto del robot. */
    public static List<Pieza> piezasDisponiblesParaHueco(Jugador jugador, Jugador.Robot robot, Montaje.Hueco hueco) {
        List<Pieza> opciones = new ArrayList<>();
        if (jugador == null || robot == null || hueco == null) return opciones;
        Modelo modelo = Modelo.de(robot.modelo);
        for (Pieza pz : CatalogoDePiezas.delJuego().de(hueco.ranura)) {
            if (Progreso.piezaUsable(jugador, pz, modelo)) {
                opciones.add(metajavbots.ejercicios.RetoDePieza.aplicarAjustes(pz, jugador.ajustesDePiezas.get(pz.id)));
            }
        }
        return opciones;
    }

    /** Rota la pieza montada en {@code hueco} ({@code paso} = -1 o +1) respetando el inventario y compatibilidad. */
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

    /** Cifras reales ({@link Montaje.Cifras}) del robot actualmente seleccionado con su montaje actual. */
    public Montaje.Cifras cifrasActuales(Jugador jugador) {
        Jugador.Robot r = robotActual(jugador);
        Montaje m = r != null ? jugador.montajeDe(r) : new Montaje(Modelo.de("cobalto"));
        return m.cifras();
    }

    private Jugador.Robot robotActual(Jugador jugador) {
        if (jugador == null || jugador.robots.isEmpty()) return null;
        robotElegido = Math.max(0, Math.min(robotElegido, jugador.robots.size() - 1));
        return jugador.robots.get(robotElegido);
    }

    private static int indiceDePieza(List<Pieza> opciones, Pieza actual) {
        if (actual == null) return -1;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).id.equals(actual.id)) return i;
        }
        return -1;
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
        editor.alCompilar = () -> lanzarCompilacion(j, false);
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
                        + "losing with bonuses gives 1/4 of their value, winning gives 100%). "
                        + "The player can switch both robots and available/purchased parts (head, arms, legs) directly in the race menu.",
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
        float arriba = Comun.cabecera(j, T.t("especial.carrera"), true);
        if (preparando || carrera == null) {
            dibujarPreparacion(j, arriba);
        } else {
            dibujarCarrera(j, arriba, dt);
        }
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
        float h = contraMaquina ? 228 : 272;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        dibujarSelectorRobot(j, x + 14, y + 8, w - 28);
        dibujarSelectorPiezas(j, x + 14, y + 52, w - 28);
        float bx = x + 14, by = y + 106;
        float tEt = Math.min(ui.letra * 0.84f, 19f);
        p.textoConBordeNegro(T.t("carrera_obs.rival"), bx, by + 9, tEt, true, ui.tema.textoSuave, 1);
        bx += Math.max(80, p.ancho(T.t("carrera_obs.rival"), tEt, true) + 12);
        float wMaq = Math.min(220, Math.max(165, w * 0.18f));
        float wUsu = Math.min(250, Math.max(180, w * 0.21f));
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
        float wNueva = Math.min(204, Math.max(150, w * 0.17f));
        if (ui.boton(T.t("carrera_obs.nueva_pista"), x + w - wNueva - 14, by, wNueva, 38, Ui.Estilo.NORMAL)) {
            regenerarPista();
        }
        dibujarSelectorDificultad(j, x + 14, y + 152, w - 28);
        if (!contraMaquina) {
            dibujarSelectorAjeno(j, x + 14, y + 224, w - 28);
        }
        return y + h + 10;
    }

    private void dibujarSelectorPiezas(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador.Robot r = robotActual(j.jugador);
        if (r == null) return;
        Montaje m = j.jugador.montajeDe(r);
        float sep = 8;
        float cw = (w - sep * (HUECOS_EDITABLES.length - 1)) / HUECOS_EDITABLES.length;
        float ch = 46;
        float tHueco = Math.min(ui.letra * 0.62f, 13.5f);
        float tPieza = Math.min(ui.letra * 0.76f, 16.5f);
        for (int i = 0; i < HUECOS_EDITABLES.length; i++) {
            Montaje.Hueco hueco = HUECOS_EDITABLES[i];
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
            float tx = cx + 40;
            float tw = Math.max(40, cw - 80);
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
        p.textoConBordeNegro(et, x, y + 8, tEt, true, ui.tema.textoSuave, 1);
        float dx = x + Math.max(98, p.ancho(et, tEt, true) + 12);
        DificultadCarrera[] niveles = DificultadCarrera.values();
        float sep = 6;
        float zonaSub = 276;
        float bw = Math.min(148, Math.max(92, (x + w - zonaSub - dx - sep * (niveles.length - 1)) / niveles.length));
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
        dibujarSelectorSubnivel(j, dx + 8, y, x + w - (dx + 8));
        float tRes = Math.min(ui.letra * 0.74f, 16.5f);
        String multTxt = T.f("carrera_obs.multiplicador",
                String.format(Locale.ROOT, "%.2f", dificultad.multiplicadorRecompensa(subnivel)));
        String resumen = dificultad.nombre(T.idioma) + " " + DificultadCarrera.etiquetaSubnivel(subnivel)
                + " (" + multTxt + "): " + dificultad.resumen(T.idioma);
        p.textoConBordeNegro(ui.recortarTexto(resumen, w, tRes, false), x, y + 40, tRes, false, ui.tema.textoSuave, 1);
    }

    private void dibujarSelectorSubnivel(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float tEt = Math.min(ui.letra * 0.8f, 17.5f);
        String et = T.t("carrera_obs.subnivel");
        p.textoConBordeNegro(et, x, y + 8, tEt, true, ui.tema.textoSuave, 1);
        float sx = x + Math.max(76, p.ancho(et, tEt, true) + 8);
        float bw = Math.min(46, Math.max(34, (x + w - sx - 12) / 3f));
        for (int s = DificultadCarrera.SUBNIVEL_MIN; s <= DificultadCarrera.SUBNIVEL_MAX; s++) {
            String rom = DificultadCarrera.etiquetaSubnivel(s);
            Ui.Estilo estilo = s == subnivel ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
            if (ui.boton(rom, sx, y, bw, 34, estilo)) {
                seleccionarSubnivel(s);
            }
            sx += bw + 6;
        }
    }

    private void dibujarSelectorRobot(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Jugador.Robot> lista = j.jugador.robots;
        float tEt = Math.min(ui.letra * 0.84f, 19f);
        p.textoConBordeNegro(T.t("carrera_obs.tu_robot"), x, y + 9, tEt, true, ui.tema.textoSuave, 1);
        float rx = x + Math.max(98, p.ancho(T.t("carrera_obs.tu_robot"), tEt, true) + 12);
        if (!lista.isEmpty()) {
            robotElegido = Math.max(0, Math.min(robotElegido, lista.size() - 1));
            if (ui.boton("◄", rx, y, 38, 36, Ui.Estilo.PLANO, lista.size() > 1, null)) {
                seleccionarRobot(j.jugador, (robotElegido - 1 + lista.size()) % lista.size());
            }
            Jugador.Robot r = lista.get(robotElegido);
            Montaje m = j.jugador.montajeDe(r);
            Montaje.Cifras c = m != null ? m.cifras() : new Montaje(Modelo.de("cobalto")).cifras();
            String cont = lista.size() > 1 ? "[" + (robotElegido + 1) + "/" + lista.size() + "] "
                    : "(" + T.t("carrera_obs.solo_un_robot") + ") ";
            String desc = cont + r.nombre + " (" + Modelo.de(r.modelo).nombre(T.idioma) + ") · "
                    + String.format(Locale.ROOT, "VEL %.1f · BLIND %.0f", c.velocidad, c.blindajeTotal());
            float tDesc = Math.min(ui.letra * 0.84f, 19f);
            p.textoConBordeNegro(ui.recortarTexto(desc, w - 410, tDesc, true), rx + 46, y + 8, tDesc, true, ui.tema.texto, 1);
            if (ui.boton("►", x + w - 256, y, 38, 36, Ui.Estilo.PLANO, lista.size() > 1, null)) {
                seleccionarRobot(j.jugador, (robotElegido + 1) % lista.size());
            }
        }
        String arenaTxt = T.f("carrera_obs.arena_actual", escenarioActual);
        float tArena = Math.min(ui.letra * 0.8f, 18f);
        p.textoConBordeNegro(arenaTxt, x + w - Math.max(196, p.ancho(arenaTxt, tArena, true)), y + 9, tArena, true, 0xFFC857, 1);
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
        Ui.Estilo[] estilos = {Ui.Estilo.PRINCIPAL, Ui.Estilo.PLANO, Ui.Estilo.DESTACADO};
        boolean[] activos = {!compilando, !compilando, !compilando};
        String[] ayudas = {T.t("taller.compilar_ayuda"), T.t("taller.plantilla_ayuda"), T.t("carrera_obs.iniciar_ayuda")};
        switch (ui.botonera(textos, estilos, activos, ayudas, x, y, w, 44)) {
            case 0:
                lanzarCompilacion(j, false);
                break;
            case 1:
                editor.doc.cambiarTodo(CarreraDeObstaculos.plantillaInicial(j.jugador.idioma));
                aviso = "";
                break;
            case 2:
                if (compilado.listo() && editor.doc.texto().equals(compilado.fuente())) {
                    iniciarCarrera(j, false);
                } else {
                    lanzarCompilacion(j, true);
                }
                break;
            default:
                break;
        }
        String pie = aviso.isEmpty() ? T.t("carrera_obs.pista_editor") : aviso;
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
        if (rotarArena) {
            regenerarPista();
        }
        Combatiente cJug = CarreraDeObstaculos.combatienteDeJugador(j.jugador, robotElegido, compilado);
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
        vista.dibujar(p, carrera.pelea, x, ay, w, ah, dt);
        DibujoCarreraDeObstaculos.lineasPista(p, ui, vista, carrera.metaX());
        float mmW = Math.min(w - 40, 680);
        float mmH = 44;
        DibujoCarreraDeObstaculos.minimapa(p, ui, carrera, vista, x + (w - mmW) / 2, ay + ah - mmH - 12, mmW, mmH);
        if (carrera.terminada()) {
            int accion = DibujoCarreraDeObstaculos.resultado(j, carrera, monedaGanada, x, ay, w, ah);
            if (accion == 1) {
                regenerarPista();
                iniciarCarrera(j, false);
            } else if (accion == 2) {
                regenerarPista();
                preparando = true;
            } else if (accion == 3) {
                regenerarPista();
                iniciarCarrera(j, false);
            }
        }
    }

    private void dibujarHudCarrera(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 10, ui.tema.panel, 0.96f);
        float bw = Math.min(220, w * 0.18f);
        double p0 = carrera.progreso(0), p1 = carrera.progreso(1);
        p.textoConBordeNegro(carrera.pelea.combatientes[0].nombre + " " + Math.round(p0 * 100) + "%", x + 14, y + 8,
                ui.letra * 0.74f, true, VistaDeArena.COLORES[0], 1);
        ui.barra(x + 14, y + 34, bw, 18, (float) p0, VistaDeArena.COLORES[0], null);
        float x1 = x + 28 + bw;
        p.textoConBordeNegro(carrera.pelea.combatientes[1].nombre + " " + Math.round(p1 * 100) + "%", x1, y + 8,
                ui.letra * 0.74f, true, VistaDeArena.COLORES[1], 1);
        ui.barra(x1, y + 34, bw, 18, (float) p1, VistaDeArena.COLORES[1], null);
        float cx = x + w - 460;
        if (ui.boton(pausado ? T.t("carrera_obs.reanudar") : T.t("carrera_obs.pausar"), cx, y + 12, 120, 42, Ui.Estilo.NORMAL)) {
            pausado = !pausado;
        }
        if (ui.boton(velocidad + "x", cx + 128, y + 12, 64, 42, Ui.Estilo.PLANO)) {
            velocidad = velocidad == 1 ? 2 : velocidad == 2 ? 4 : 1;
        }
        if (ui.boton(T.t("carrera_obs.editar"), cx + 200, y + 12, 124, 42, Ui.Estilo.PLANO)) {
            regenerarPista();
            preparando = true;
        }
        if (ui.boton(T.t("carrera_obs.nueva_pista"), cx + 332, y + 12, 118, 42, Ui.Estilo.PLANO)) {
            regenerarPista();
            iniciarCarrera(j, false);
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
