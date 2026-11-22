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
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
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

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final RobotDelJugador compilado = new RobotDelJugador();
    private VistaDeArena vista;
    private CarreraDeObstaculos carrera;
    private Arena vistaPrevia;
    private long semilla = (System.nanoTime() & 0xFFFFFL) + 17L;
    private String escenarioAnterior;
    private String escenarioActual;
    private DificultadCarrera dificultad = DificultadCarrera.porDefecto();
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

    /** Intenta seleccionar una dificultad respetando el bloqueo de Especial; devuelve si se aplicó. */
    public boolean seleccionarDificultad(Jugador jugador, DificultadCarrera nueva) {
        if (!dificultadDesbloqueada(jugador, nueva)) return false;
        dificultad = nueva;
        if (escenarioActual != null) {
            vistaPrevia = Arena.carreraDeObstaculos(CarreraDeObstaculos.ANCHO, CarreraDeObstaculos.ALTO, semilla, escenarioActual, dificultad);
        }
        return true;
    }

    /** Devuelve la dificultad actualmente seleccionada en el menú de la carrera. */
    public DificultadCarrera dificultadSeleccionada() {
        return dificultad;
    }

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        ajenos = PrepararPelea.robotsAjenos();
        robotElegido = j.jugador != null ? j.jugador.robotElegido : 0;
        dificultad = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        escenarioActual = CarreraDeObstaculos.siguienteArena(escenarioActual, semilla);
        vista.escenario = escenarioDe(escenarioActual);
        vistaPrevia = Arena.carreraDeObstaculos(CarreraDeObstaculos.ANCHO, CarreraDeObstaculos.ALTO, semilla, escenarioActual, dificultad);
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
                "The player is in Obstacle race (Special mode), difficulty " + dificultad.name()
                        + " (Noob/Junior/Senior/Special; Special unlocks with Special mode), on a 6x track with rotating arenas, "
                        + "multiplied damage scaling with difficulty (driving straight into hazards destroys the robot), "
                        + "instant-kill lethal traps (\"trampa_mortal\"), spaced repair pickups (\"reparacion\"), "
                        + "sudden obstacles (destructible barrels/chimneys and indestructible traps), broken malfunctioning robots "
                        + "shooting randomly, speed boost pickups (\"impulso\"), live combat against the rival, and perfection medals "
                        + "(Gold/Silver/Bronze; perfect Gold wins award +1/4 extra coin, 4 quarters = +1 full coin).",
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
        float h = contraMaquina ? 172 : 218;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        dibujarSelectorRobot(j, x + 14, y + 8, w - 28);
        float bx = x + 14, by = y + 54;
        float tEt = Math.min(ui.letra * 0.86f, 20f);
        p.texto(T.t("carrera_obs.rival"), bx, by + 10, tEt, true, ui.tema.textoSuave, 1);
        bx += Math.max(82, p.ancho(T.t("carrera_obs.rival"), tEt, true) + 14);
        float wMaq = Math.min(220, Math.max(170, w * 0.19f));
        float wUsu = Math.min(250, Math.max(185, w * 0.22f));
        if (ui.boton(T.t("carrera_obs.vs_maquina"), bx, by, wMaq, 40, contraMaquina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
            contraMaquina = true;
            aviso = "";
        }
        bx += wMaq + 10;
        if (ui.boton(T.t("carrera_obs.vs_usuario"), bx, by, wUsu, 40, !contraMaquina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
            contraMaquina = false;
            ajenos = PrepararPelea.robotsAjenos();
            aviso = "";
        }
        float wNueva = Math.min(204, Math.max(150, w * 0.17f));
        if (ui.boton(T.t("carrera_obs.nueva_pista"), x + w - wNueva - 14, by, wNueva, 40, Ui.Estilo.NORMAL)) {
            regenerarPista();
        }
        dibujarSelectorDificultad(j, x + 14, y + 102, w - 28);
        if (!contraMaquina) {
            dibujarSelectorAjeno(j, x + 14, y + 174, w - 28);
        }
        return y + h + 10;
    }

    private void dibujarSelectorDificultad(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        dificultad = DificultadCarrera.seleccionarPara(j.jugador, dificultad);
        float tEt = Math.min(ui.letra * 0.86f, 20f);
        String et = T.t("carrera_obs.dificultad");
        p.texto(et, x, y + 9, tEt, true, ui.tema.textoSuave, 1);
        float dx = x + Math.max(106, p.ancho(et, tEt, true) + 14);
        DificultadCarrera[] niveles = DificultadCarrera.values();
        float sep = 8;
        float bw = Math.min(168, Math.max(96, (x + w - dx - sep * (niveles.length - 1)) / niveles.length));
        for (DificultadCarrera d : niveles) {
            boolean desbloqueada = dificultadDesbloqueada(j.jugador, d);
            boolean elegida = d == dificultad;
            String texto = desbloqueada ? d.nombre(T.idioma) : "🔒 " + d.nombre(T.idioma);
            String ayuda = desbloqueada ? d.resumen(T.idioma) : T.t("carrera_obs.dif_especial_bloqueada");
            Ui.Estilo estilo = elegida ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO;
            if (ui.boton(texto, dx, y, bw, 36, estilo, desbloqueada, ayuda)) {
                seleccionarDificultad(j.jugador, d);
            }
            dx += bw + sep;
        }
        float tRes = Math.min(ui.letra * 0.76f, 17f);
        String resumen = dificultad.nombre(T.idioma) + ": " + dificultad.resumen(T.idioma);
        p.texto(ui.recortarTexto(resumen, w, tRes, false), x, y + 42, tRes, false, ui.tema.textoSuave, 1);
    }

    private void dibujarSelectorRobot(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<Jugador.Robot> lista = j.jugador.robots;
        p.texto(T.t("carrera_obs.tu_robot"), x, y + 10, ui.letra * 0.88f, true, ui.tema.textoSuave, 1);
        float rx = x + Math.max(106, p.ancho(T.t("carrera_obs.tu_robot"), ui.letra * 0.88f, true) + 14);
        if (!lista.isEmpty()) {
            robotElegido = Math.max(0, Math.min(robotElegido, lista.size() - 1));
            if (ui.boton("◄", rx, y, 40, 38, Ui.Estilo.PLANO, lista.size() > 1, null)) {
                robotElegido = (robotElegido - 1 + lista.size()) % lista.size();
            }
            Jugador.Robot r = lista.get(robotElegido);
            Montaje m = j.jugador.montajeDe(r);
            Montaje.Cifras c = m != null ? m.cifras() : new Montaje(Modelo.de("cobalto")).cifras();
            String desc = r.nombre + " (" + Modelo.de(r.modelo).nombre(T.idioma) + ") · "
                    + String.format(Locale.ROOT, "VEL %.1f · BLIND %.0f", c.velocidad, c.blindajeTotal());
            p.texto(ui.recortarTexto(desc, w - 420, ui.letra * 0.86f, true), rx + 50, y + 9, ui.letra * 0.86f, true, ui.tema.texto, 1);
            if (ui.boton("►", x + w - 260, y, 40, 38, Ui.Estilo.PLANO, lista.size() > 1, null)) {
                robotElegido = (robotElegido + 1) % lista.size();
            }
        }
        String arenaTxt = T.f("carrera_obs.arena_actual", escenarioActual);
        p.texto(arenaTxt, x + w - Math.max(196, p.ancho(arenaTxt, ui.letra * 0.82f, true)), y + 10, ui.letra * 0.82f, true, 0xFFC857, 1);
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
        DibujoCarreraDeObstaculos.mapaGeneral(p, ui, vistaPrevia, mx, cy + 4, mw, mh);
        float ly = cy + mh + 10;
        float paso = ui.letra * 1.18f;
        float tl = ui.letra * 0.70f;
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
            rival = CarreraDeObstaculos.rivalMaquina(T.t("carrera_obs.maquina_nombre"), difEfectiva);
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
        carrera = new CarreraDeObstaculos(cJug, rival, semilla, contraMaquina, escenarioAnterior, difEfectiva);
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
        vistaPrevia = Arena.carreraDeObstaculos(CarreraDeObstaculos.ANCHO, CarreraDeObstaculos.ALTO, semilla, escenarioActual, dificultad);
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
        DibujoCarreraDeObstaculos.lineasPista(p, ui, vista);
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
        p.texto(carrera.pelea.combatientes[0].nombre + " " + Math.round(p0 * 100) + "%", x + 14, y + 8, ui.letra * 0.74f, true, VistaDeArena.COLORES[0], 1);
        ui.barra(x + 14, y + 34, bw, 18, (float) p0, VistaDeArena.COLORES[0], null);
        float x1 = x + 28 + bw;
        p.texto(carrera.pelea.combatientes[1].nombre + " " + Math.round(p1 * 100) + "%", x1, y + 8, ui.letra * 0.74f, true, VistaDeArena.COLORES[1], 1);
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
