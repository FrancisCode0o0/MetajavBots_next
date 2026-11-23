package metajavbots.escritorio;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.sun.management.ThreadMXBean;

import metajavbots.Juego;
import metajavbots.campanas.Campana;
import metajavbots.campanas.CatalogoDeEscenas;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Demostracion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Comun;
import metajavbots.pantallas.Ejercicios;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.Menu;
import metajavbots.pantallas.MiniJuegos;
import metajavbots.pantallas.PanelSenior;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaDemostracion;
import metajavbots.pantallas.PantallaEjercicio;
import metajavbots.pantallas.PantallaEscenaCampana;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;
import metajavbots.pantallas.PantallaPelea;
import metajavbots.pantallas.PrepararPelea;
import metajavbots.pantallas.Repeticion;
import metajavbots.pantallas.Taller;
import metajavbots.pantallas.Tienda;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.Idioma;

/**
 * MEDIDOR DE RENDIMIENTO — Perfilador headless (ventana invisible 1600×900) que
 * mide tiempo de arranque hasta el menú, tiempo medio y peor fotograma (FPS),
 * memoria usada y bytes asignados por fotograma en las 14 pantallas clave.
 */
public final class MedidorRendimiento extends Juego {

    private static final int CALENTAMIENTO = 30;
    private static final int MEDIDOS = 120;

    private static long inicioMainNanos;
    private final Map<String, Supplier<Pantalla>> pantallas = new LinkedHashMap<>();
    private final List<String> nombres = new ArrayList<>();
    private final ThreadMXBean hiloMx;
    private int actual = -1;
    private int fotogramas;
    private long sumaNanos;
    private long maxNanos;
    private long sumaBytes;
    private boolean arranqueReportado;

    MedidorRendimiento() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        this.hiloMx = base instanceof ThreadMXBean ? (ThreadMXBean) base : null;
        if (this.hiloMx != null && this.hiloMx.isThreadAllocatedMemorySupported()) {
            this.hiloMx.setThreadAllocatedMemoryEnabled(true);
        }
        registrarPantallas();
        nombres.addAll(pantallas.keySet());
    }

    private void registrarPantallas() {
        pantallas.put("01_Menu", Menu::new);
        pantallas.put("02_Taller", Taller::new);
        pantallas.put("03_Tienda", Tienda::new);
        pantallas.put("04_Pelea_HUD_Depurador", this::pantallaPeleaDepurador);
        pantallas.put("05_Pelea_Senior_Paralelo", this::pantallaPeleaSenior);
        pantallas.put("06_Arena_Destructible", this::pantallaArenaDestructible);
        pantallas.put("07_Carrera_Obstaculos", PantallaCarreraDeObstaculos::new);
        pantallas.put("08_Destruccion_Supervivencia", PantallaMinijuegoDestruccion::new);
        pantallas.put("09_Ejercicios_Escena", this::pantallaEjercicioEscena);
        pantallas.put("10_Campanas_Escena", this::pantallaCampanaEscena);
        pantallas.put("11_Repeticion_Codigo", this::pantallaRepeticion);
        pantallas.put("12_Demostracion", () -> new PantallaDemostracion(Demostracion.de(Demostracion.ID_DEMO_3)));
        pantallas.put("13_Minijuegos", MiniJuegos::new);
        pantallas.put("14_Ejercicios_Lista", Ejercicios::new);
    }

    private Pantalla pantallaPeleaDepurador() {
        Encuentro e = PrepararPelea.encuentro(this, "ADAPTADA");
        e.escenario = VistaDeArena.Escenario.LABORATORIO;
        PantallaPelea pp = new PantallaPelea(e);
        return new Pantalla() {
            @Override
            public void alEntrar(Juego j) {
                pp.alEntrar(j);
                pp.abrirDepurador(1);
            }

            @Override
            public void alSalir(Juego j) {
                pp.alSalir(j);
            }

            @Override
            public void fotograma(Juego j, float dt) {
                pp.fotograma(j, dt);
            }
        };
    }

    private Pantalla pantallaPeleaSenior() {
        Encuentro e = PanelSenior.encuentroParalelo(this);
        e.escenario = VistaDeArena.Escenario.NEON;
        return new PantallaPelea(e);
    }

    private Pantalla pantallaArenaDestructible() {
        Encuentro e = PrepararPelea.encuentro(this, "ADAPTADA");
        e.escenario = VistaDeArena.Escenario.CHATARRERIA;
        return new PantallaPelea(e);
    }

    private Pantalla pantallaEjercicioEscena() {
        PantallaEjercicio pe = new PantallaEjercicio(Banco.delJuego().deTema("05").get(0));
        return new Pantalla() {
            private boolean probado;

            @Override
            public void alEntrar(Juego j) {
                pe.alEntrar(j);
            }

            @Override
            public void alSalir(Juego j) {
                pe.alSalir(j);
            }

            @Override
            public void fotograma(Juego j, float dt) {
                if (!probado) {
                    probado = true;
                    pe.probarCon(j, pe.firmaCon("int s = 0; for (int i = 1; i <= n; i++) s += i; return s;"));
                }
                pe.fotograma(j, dt);
            }
        };
    }

    private Pantalla pantallaCampanaEscena() {
        Campana c = Campana.deSerie().get(0);
        Campana.Capitulo cap = c.capitulos.get(0);
        return new PantallaEscenaCampana(c, cap, CatalogoDeEscenas.paraCapitulo(c, cap), false, null);
    }

    private Pantalla pantallaRepeticion() {
        return new Pantalla() {
            private VistaDeArena vista;
            private Pelea pelea;
            private final Repeticion rep = new Repeticion();

            @Override
            public void fotograma(Juego j, float dt) {
                if (vista == null) {
                    vista = new VistaDeArena(j.pincel);
                    vista.escenario = VistaDeArena.Escenario.NEON;
                    vista.conPublico = true;
                    Random r = new Random(12);
                    List<Modelo> ms = Modelo.todos();
                    pelea = new Pelea(Rivales.crear(ms.get(3), 0.8, 0.9, r),
                            Rivales.crear(ms.get(10), 0.8, 0.9, r), 13);
                    pelea.empezarRonda();
                    for (int i = 0; i < 600 && pelea.estado() == Pelea.Estado.LUCHANDO; i++) {
                        pelea.avanzar();
                        rep.grabar(pelea);
                    }
                    rep.empezar(pelea, j.jugador.robot().codigo);
                }
                if (rep.activa()) rep.avanzar(j, pelea, vista, 1 / 60f);
                float aw = Repeticion.anchoArena(j.pincel.ancho);
                vista.dibujar(j.pincel, pelea, 0, 88, aw, j.pincel.alto - 88, 1 / 60f);
                if (rep.activa()) {
                    rep.rotulo(j, 0, 88, aw, j.pincel.alto - 88);
                    rep.panelDeCodigo(j, pelea, aw, 88, j.pincel.ancho - aw, j.pincel.alto - 88);
                }
            }
        };
    }

    @Override
    public void create() {
        primera = new Menu();
        super.create();
        Guardado.carpeta = java.nio.file.Paths.get("build", "partidas_de_medicion");
        Jugador j = new Jugador("Medidor");
        j.etiqueta = Etiqueta.SENIOR_2;
        j.idioma = Idioma.ES;
        j.tamanoDeLetra = 18;
        j.vozClara = false;
        j.vozDelLocutor = false;
        j.musicaActiva = false;
        j.monedas = 50.0;
        Jugador.Robot r = new Jugador.Robot("Cobaltito", "cobalto");
        r.codigo = Comun.plantilla(Idioma.ES);
        r.codigoCompilado = r.codigo;
        j.robots.add(r);
        ponerJugador(j);
        siguiente();
    }

    private void siguiente() {
        entrenadorFlotante.cerrar();
        actual++;
        fotogramas = 0;
        sumaNanos = 0;
        maxNanos = 0;
        sumaBytes = 0;
        if (actual >= nombres.size()) {
            Gdx.app.exit();
            return;
        }
        irSolo(pantallas.get(nombres.get(actual)).get());
    }

    @Override
    public void render() {
        long b0 = hiloMx != null ? hiloMx.getCurrentThreadAllocatedBytes() : 0;
        long t0 = System.nanoTime();
        super.render();
        long dtNanos = System.nanoTime() - t0;
        long b1 = hiloMx != null ? hiloMx.getCurrentThreadAllocatedBytes() : 0;
        if (!arranqueReportado) {
            arranqueReportado = true;
            double arranqueMs = (System.nanoTime() - inicioMainNanos) / 1e6;
            System.out.printf(Locale.ROOT, "ARRANQUE_HASTA_MENU_MS=%.1f%n", arranqueMs);
        }
        if (actual < 0 || actual >= nombres.size()) return;
        fotogramas++;
        if (fotogramas > CALENTAMIENTO) {
            sumaNanos += dtNanos;
            if (dtNanos > maxNanos) maxNanos = dtNanos;
            sumaBytes += Math.max(0L, b1 - b0);
        }
        if (fotogramas >= CALENTAMIENTO + MEDIDOS) {
            double avgMs = sumaNanos / 1e6 / MEDIDOS;
            double maxMs = maxNanos / 1e6;
            double fpsAvg = avgMs > 0 ? 1000.0 / avgMs : 0;
            double fpsMin = maxMs > 0 ? 1000.0 / maxMs : 0;
            long allocPorFrame = sumaBytes / MEDIDOS;
            Runtime rt = Runtime.getRuntime();
            double heapMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);
            System.out.printf(Locale.ROOT,
                    "PANTALLA=%-28s avgMs=%5.2f maxMs=%6.2f fpsAvg=%6.1f fpsMin=%5.1f heapMB=%5.1f allocB=%8d%n",
                    nombres.get(actual), avgMs, maxMs, fpsAvg, fpsMin, heapMb, allocPorFrame);
            siguiente();
        }
    }

    /** Ejecuta la medición headless de arranque y de las 14 pantallas clave. */
    public static void main(String[] args) {
        inicioMainNanos = System.nanoTime();
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setWindowedMode(1600, 900);
        c.setInitialVisible(false);
        c.useVsync(false);
        c.setForegroundFPS(0);
        c.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        c.disableAudio(true);
        new Lwjgl3Application(new MedidorRendimiento(), c);
    }
}
