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
 * PANTALLA CARRERA DE OBSTÁCULOS — Combina mapa general con visibilidad parcial,
 * editor trilingüe con compilación y carrera en tiempo real contra la máquina o
 * contra un robot de otro usuario (robots_ajenos/), otorgando 1 moneda por victoria.
 */
public final class PantallaCarreraDeObstaculos implements Pantalla {

    /** Resultado de cargar y validar un robot ajeno para la carrera bajo Vigilante y Cargador. */
    public record CargaAjeno(Combatiente combatiente, String error) {
        /** Indica si la carga y compilación salieron bien. */
        public boolean bien() {
            return combatiente != null;
        }
    }

    private final VentanaDeCodigo editor = new VentanaDeCodigo();
    private final RobotDelJugador compilado = new RobotDelJugador();
    private VistaDeArena vista;
    private CarreraDeObstaculos carrera;
    private Arena vistaPrevia;
    private long semilla = (System.nanoTime() & 0xFFFFFL) + 17L;
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

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        vista.escenario = VistaDeArena.Escenario.CHATARRERIA;
        ajenos = PrepararPelea.robotsAjenos();
        vistaPrevia = Arena.carreraDeObstaculos(CarreraDeObstaculos.ANCHO, CarreraDeObstaculos.ALTO, semilla);
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
                "The player is in Obstacle race (Special mode), programming a robot to race to the finish line "
                        + "using radar/obstacles() and if conditionals to shoot destructible barrels/chimneys and dodge indestructibles.",
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
        Ui ui = j.ui;
        float x = 20, y = arriba + 12, w = p.ancho - 40;
        float yTrasRival = barraRival(j, x, y, w);
        float altoCuerpo = p.alto - yTrasRival - 16;
        float anchoMapa = Math.max(360, w * 0.42f);
        float anchoEditor = w - anchoMapa - 16;
        dibujarPanelMapa(j, x, yTrasRival, anchoMapa, altoCuerpo);
        dibujarPanelEditor(j, x + anchoMapa + 16, yTrasRival, anchoEditor, altoCuerpo);
    }

    private float barraRival(Juego j, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = contraMaquina ? 64 : 116;
        p.caja(x, y, w, h, 10, ui.tema.panel, 1);
        float bx = x + 14, by = y + 10;
        p.texto(T.t("carrera_obs.rival"), bx, by + 11, ui.letra * 0.9f, true, ui.tema.textoSuave, 1);
        bx += Math.max(90, p.ancho(T.t("carrera_obs.rival"), ui.letra * 0.9f, true) + 16);
        if (ui.boton(T.t("carrera_obs.vs_maquina"), bx, by, 220, 42, contraMaquina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
            contraMaquina = true;
            aviso = "";
        }
        bx += 230;
        if (ui.boton(T.t("carrera_obs.vs_usuario"), bx, by, 260, 42, !contraMaquina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
            contraMaquina = false;
            ajenos = PrepararPelea.robotsAjenos();
            aviso = "";
        }
        if (ui.boton(T.t("carrera_obs.nueva_pista"), x + w - 210, by, 196, 42, Ui.Estilo.NORMAL)) {
            regenerarPista();
        }
        if (!contraMaquina) {
            dibujarSelectorAjeno(j, x + 14, y + 62, w - 28);
        }
        return y + h + 12;
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
        float mh = Math.min(h * 0.50f, mw * 0.56f);
        dibujarMapaGeneral(p, ui, mx, cy + 4, mw, mh);
        float ly = cy + mh + 18;
        p.circulo(mx + 10, ly + 8, 7, 0xD97236, 1);
        p.texto(T.t("carrera_obs.leyenda_destructible"), mx + 26, ly, ui.letra * 0.78f, false, ui.tema.texto, 1);
        ly += ui.letra * 1.45f;
        p.rect(mx + 3, ly + 1, 14, 14, 0xE05A4F, 1);
        p.texto(T.t("carrera_obs.leyenda_indestructible"), mx + 26, ly, ui.letra * 0.78f, false, ui.tema.texto, 1);
        ly += ui.letra * 1.45f;
        p.circulo(mx + 10, ly + 8, 7, 0x3FE0FF, 0.55f);
        p.texto(T.t("carrera_obs.leyenda_ocultos"), mx + 26, ly, ui.letra * 0.78f, true, ui.tema.aviso, 1);
        ly += ui.letra * 1.65f;
        p.parrafo(T.t("carrera_obs.mapa_ayuda"), mx, ly, mw, ui.letra * 0.78f, ui.tema.textoSuave, 1);
    }

    private void dibujarMapaGeneral(Pincel p, Ui ui, float x, float y, float w, float h) {
        p.caja(x, y, w, h, 8, 0x1A202C, 1);
        p.bordeRedondeado(x, y, w, h, 8, ui.tema.borde, 1, 1.5f);
        float sxSalida = (float) (x + (CarreraDeObstaculos.SALIDA_X / CarreraDeObstaculos.ANCHO) * w);
        float sxMeta = (float) (x + (CarreraDeObstaculos.META_X / CarreraDeObstaculos.ANCHO) * w);
        p.rect(sxSalida, y + 6, 3, h - 12, 0x5BD685, 0.85f);
        dibujarMetaCuadros(p, sxMeta, y + 6, h - 12, 8);
        // Zona de niebla de radar en el tramo central (obstáculos ocultos no revelados en el mapa general).
        p.rect(x + w * 0.30f, y + 8, w * 0.52f, h - 16, 0x3FE0FF, 0.06f);
        p.textoCentrado("? RADAR ?", x + w * 0.56f, y + 10, ui.letra * 0.65f, true, 0x3FE0FF, 0.45f);
        // Salida de los dos corredores.
        p.circulo(sxSalida - 6, y + h * 0.66f, 6, VistaDeArena.COLORES[0], 1);
        p.circulo(sxSalida - 6, y + h * 0.34f, 6, VistaDeArena.COLORES[1], 1);
        // Solo los obstáculos visibles en el mapa general (!ocultoEnMapa).
        for (Arena.Trampa t : vistaPrevia.trampas()) {
            if (t.ocultoEnMapa) continue;
            float ox = (float) (x + (t.x / CarreraDeObstaculos.ANCHO) * w);
            float oy = (float) (y + (1.0 - t.y / CarreraDeObstaculos.ALTO) * h);
            float r = Math.max(6f, (float) (t.radio / CarreraDeObstaculos.ANCHO) * w);
            if (t.destructible) {
                p.circulo(ox, oy, r, 0xD97236, 0.95f);
                p.linea(ox - r * 0.5f, oy - r * 0.3f, ox + r * 0.4f, oy + r * 0.4f, 1.8f, 0x1A1512, 0.9f);
            } else {
                p.rect(ox - r, oy - r, r * 2, r * 2, 0xE05A4F, 0.9f);
                p.bordeRedondeado(ox - r, oy - r, r * 2, r * 2, 2, 0xFFC857, 0.85f, 1.5f);
            }
        }
        p.texto(T.t("carrera_obs.salida"), x + 6, y + h - 22, ui.letra * 0.62f, true, 0x5BD685, 1);
        p.texto(T.t("carrera_obs.meta"), sxMeta - 46, y + h - 22, ui.letra * 0.62f, true, 0xFFC857, 1);
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
                    iniciarCarrera(j);
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
            iniciarCarrera(j);
        }
    }

    private void iniciarCarrera(Juego j) {
        Combatiente rival;
        if (contraMaquina) {
            rival = CarreraDeObstaculos.rivalMaquina(T.t("carrera_obs.maquina_nombre"));
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
        Jugador.Robot r = j.jugador.robot();
        Montaje montajeJug = r != null ? j.jugador.montajeDe(r) : new Montaje(Modelo.de("cobalto"));
        String nombreJug = r != null ? r.nombre : j.jugador.nombre;
        Combatiente cJug = new Combatiente(nombreJug, montajeJug, compilado);
        carrera = new CarreraDeObstaculos(cJug, rival, semilla, contraMaquina);
        preparando = false;
        pausado = false;
        acumulador = 0;
        monedaGanada = false;
        finProcesado = false;
    }

    private void regenerarPista() {
        semilla = semilla * 6364136223846793005L + 1442695040888963407L;
        vistaPrevia = Arena.carreraDeObstaculos(CarreraDeObstaculos.ANCHO, CarreraDeObstaculos.ALTO, semilla);
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
        float ay = y + hudH + 10;
        float ah = p.alto - ay - 16;
        vista.dibujar(p, carrera.pelea, x, ay, w, ah, dt);
        dibujarLineasPista(p, ui);
        if (carrera.terminada()) {
            dibujarResultado(j, x, ay, w, ah);
        }
    }

    private void dibujarHudCarrera(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.caja(x, y, w, h, 10, ui.tema.panel, 0.96f);
        float bw = Math.min(240, w * 0.20f);
        double p0 = carrera.progreso(0), p1 = carrera.progreso(1);
        p.texto(carrera.pelea.combatientes[0].nombre + " " + Math.round(p0 * 100) + "%", x + 14, y + 8, ui.letra * 0.75f, true, VistaDeArena.COLORES[0], 1);
        ui.barra(x + 14, y + 34, bw, 18, (float) p0, VistaDeArena.COLORES[0], null);
        float x1 = x + 28 + bw;
        p.texto(carrera.pelea.combatientes[1].nombre + " " + Math.round(p1 * 100) + "%", x1, y + 8, ui.letra * 0.75f, true, VistaDeArena.COLORES[1], 1);
        ui.barra(x1, y + 34, bw, 18, (float) p1, VistaDeArena.COLORES[1], null);
        float cx = x + w - 460;
        if (ui.boton(pausado ? T.t("carrera_obs.reanudar") : T.t("carrera_obs.pausar"), cx, y + 12, 120, 42, Ui.Estilo.NORMAL)) {
            pausado = !pausado;
        }
        if (ui.boton(velocidad + "x", cx + 128, y + 12, 64, 42, Ui.Estilo.PLANO)) {
            velocidad = velocidad == 1 ? 2 : velocidad == 2 ? 4 : 1;
        }
        if (ui.boton(T.t("carrera_obs.editar"), cx + 200, y + 12, 124, 42, Ui.Estilo.PLANO)) {
            preparando = true;
        }
        if (ui.boton(T.t("carrera_obs.nueva_pista"), cx + 332, y + 12, 118, 42, Ui.Estilo.PLANO)) {
            regenerarPista();
            iniciarCarrera(j);
        }
    }

    private void dibujarLineasPista(Pincel p, Ui ui) {
        float yArriba = vista.sy(CarreraDeObstaculos.ALTO);
        float yAbajo = vista.sy(0);
        float xSalidaArriba = vista.sx(CarreraDeObstaculos.SALIDA_X, CarreraDeObstaculos.ALTO);
        float xSalidaAbajo = vista.sx(CarreraDeObstaculos.SALIDA_X, 0);
        p.linea(xSalidaArriba, yArriba, xSalidaAbajo, yAbajo, 3f, 0x5BD685, 0.75f);
        int cuadros = 16;
        for (int i = 0; i < cuadros; i++) {
            double y0 = CarreraDeObstaculos.ALTO * i / cuadros;
            double y1 = CarreraDeObstaculos.ALTO * (i + 1) / cuadros;
            float sx0 = vista.sx(CarreraDeObstaculos.META_X, y0);
            float sx1 = vista.sx(CarreraDeObstaculos.META_X, y1);
            float sy0 = vista.sy(y0);
            float sy1 = vista.sy(y1);
            int c1 = i % 2 == 0 ? 0xFFFFFF : 0x111318;
            int c2 = i % 2 == 0 ? 0x111318 : 0xFFFFFF;
            p.linea(sx0, sy0, sx1, sy1, 8f, c1, 0.92f);
            p.linea(sx0 + 8, sy0, sx1 + 8, sy1, 8f, c2, 0.92f);
        }
        p.textoCentrado(T.t("carrera_obs.meta"), vista.sx(CarreraDeObstaculos.META_X, CarreraDeObstaculos.ALTO),
                yArriba - 24, ui.letra * 0.85f, true, 0xFFC857, 1);
    }

    private void dibujarResultado(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float cw = Math.min(540, w - 40), ch = 220;
        float cx = x + (w - cw) / 2, cy = y + (h - ch) / 2;
        p.caja(cx, cy, cw, ch, 14, ui.tema.panel, 0.96f);
        p.bordeRedondeado(cx, cy, cw, ch, 14, carrera.ganador() == 0 ? ui.tema.bien : ui.tema.aviso, 1, 2.5f);
        String titulo = carrera.ganador() == 0 ? T.t("carrera_obs.ganas")
                : carrera.ganador() == 1 ? T.t("carrera_obs.pierdes") : T.t("carrera_obs.empate");
        p.textoCentrado(titulo, cx + cw / 2, cy + 24, ui.letra * 1.4f, true,
                carrera.ganador() == 0 ? ui.tema.bien : ui.tema.texto, 1);
        String premio = monedaGanada
                ? T.f("carrera_obs.premio_moneda", Jugador.formatearMonedas(j.jugador.monedas))
                : T.t("carrera_obs.sin_moneda");
        p.textoCentrado(premio, cx + cw / 2, cy + 78, ui.letra * 0.95f, true,
                monedaGanada ? 0xFFC857 : ui.tema.textoSuave, 1);
        float bw = (cw - 60) / 3;
        if (ui.boton(T.t("carrera_obs.repetir"), cx + 20, cy + ch - 68, bw, 46, Ui.Estilo.PRINCIPAL)) {
            iniciarCarrera(j);
        }
        if (ui.boton(T.t("carrera_obs.editar"), cx + 30 + bw, cy + ch - 68, bw, 46, Ui.Estilo.NORMAL)) {
            preparando = true;
        }
        if (ui.boton(T.t("carrera_obs.nueva_pista"), cx + 40 + bw * 2, cy + ch - 68, bw, 46, Ui.Estilo.PLANO)) {
            regenerarPista();
            iniciarCarrera(j);
        }
    }

    private static void dibujarMetaCuadros(Pincel p, float x, float y, float h, int paso) {
        int filas = Math.max(2, (int) (h / paso));
        for (int i = 0; i < filas; i++) {
            float fy = y + i * paso;
            float fh = Math.min(paso, y + h - fy);
            p.rect(x, fy, 5, fh, i % 2 == 0 ? 0xFFFFFF : 0x111318, 1);
            p.rect(x + 5, fy, 5, fh, i % 2 == 0 ? 0x111318 : 0xFFFFFF, 1);
        }
    }
}
