package metajavbots.escritorio;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import metajavbots.Juego;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Comun;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;

/**
 * CAPTURAS — Saca fotos de las pantallas con una ventana invisible
 * («gradlew lwjgl3:capturas», en build/capturas/). Sirve para ver cómo
 * quedan sin jugar, y para comparar antes y después de un cambio.
 */
public final class Capturas extends Juego {

    private final Map<String, Supplier<Pantalla>> pantallas = new LinkedHashMap<>();
    private final List<String> nombres = new ArrayList<>();
    private final File salida;
    private int actual = -1, fotogramas;

    /** Las pantallas de lo último: los aparatos de la arena. */
    private void nuevas() {
        pantallas.put("08e_arma_especial", () -> new Pantalla() {
            private VistaDeArena vista;
            private Pelea pelea;
            private metajavbots.pantallas.ArmaEspecial especial;

            @Override
            public void fotograma(Juego j, float dt) {
                if (vista == null) {
                    vista = new VistaDeArena(j.pincel);
                    vista.escenario = VistaDeArena.Escenario.NEON;
                    Random r = new Random(8);
                    List<Modelo> ms = Modelo.todos();
                    pelea = new Pelea(Rivales.crear(ms.get(0), 0.6, 0.5, r), Rivales.crear(ms.get(4), 0.6, 0.5, r), 9);
                    pelea.empezarRonda();
                    for (int i = 0; i < 90; i++) pelea.avanzar();
                    especial = metajavbots.pantallas.ArmaEspecial.para(j.jugador);
                    especial.empezar(j, pelea);
                }
                vista.dibujar(j.pincel, pelea, 0, 88, j.pincel.ancho * 0.48f, j.pincel.alto - 88, dt);
                especial.avanzar(j, pelea, vista, dt);
                especial.dibujar(j, vista, pelea);
            }
        });
        pantallas.put("08f_derrotas", () -> new Pantalla() {
            private VistaDeArena vista;
            private Pelea pelea;
            private int fotogramas;

            @Override
            public void fotograma(Juego j, float dt) {
                if (vista == null) {
                    vista = new VistaDeArena(j.pincel);
                    vista.escenario = VistaDeArena.Escenario.DESIERTO;
                    Random r = new Random(3);
                    List<Modelo> ms = Modelo.todos();
                    pelea = new Pelea(Rivales.crear(ms.get(2), 0.6, 0.5, r), Rivales.crear(ms.get(7), 0.6, 0.5, r), 4);
                    pelea.empezarRonda();
                    for (int i = 0; i < 120; i++) pelea.avanzar();
                }
                // Con un paso fijo (las capturas van muy deprisa): se ve cómo van cayendo las piezas.
                vista.dibujar(j.pincel, pelea, 0, 0, j.pincel.ancho, j.pincel.alto, 1 / 40f);
                if (++fotogramas == 365) {
                    vista.explotar(pelea.cuerpo(0), 1);
                    vista.explotar(pelea.cuerpo(1), 3);
                }
            }
        });
        pantallas.put("09f_finales", () -> new Pantalla() {
            private float t = 1.0f;

            @Override
            public void fotograma(Juego j, float dt) {
                t += dt;
                metajavbots.pantallas.Comun.fondo(j, 0);
                metajavbots.pantallas.FinalesDelRobotito.muestra(j.pincel, 100, 100, j.pincel.ancho - 200, Math.min(t, 1.9f));
            }
        });
        pantallas.put("08b_aparato_suelo", () -> new Arena(VistaDeArena.Escenario.HANGAR, 1));
        pantallas.put("08c_aparato_montado", () -> new Arena(VistaDeArena.Escenario.LABORATORIO, 2));
        pantallas.put("08d_aparato_codigo", () -> new Pantalla() {
            private final metajavbots.pantallas.PanelDelAparato panel = metajavbots.pantallas.PanelDelAparato.abierto();

            @Override
            public void fotograma(Juego j, float dt) {
                metajavbots.pantallas.Comun.fondo(j, 0);
                panel.dibujar(j, metajavbots.combate.Aparatos.Tipo.PEGAMENTO, j.pincel.ancho - 400, j.pincel.alto - 64);
            }
        });
    }

    Capturas(File salida, String solo) {
        this.salida = salida;
        pantallas.put("01_inicio", metajavbots.pantallas.Inicio::new);
        pantallas.put("02_menu", metajavbots.pantallas.Menu::new);
        pantallas.put("03_robots", Escaparate::new);
        pantallas.put("03b_armas", Armeria::new);
        pantallas.put("03d_entrenadores", () -> new Pantalla() {
            private float t;

            @Override
            public void fotograma(Juego j, float dt) {
                t += dt;
                Comun.fondo(j, t);
                java.util.List<metajavbots.tutor.Entrenador> es = metajavbots.tutor.Entrenador.todos();
                for (int i = 0; i < es.size(); i++) {
                    float x = 130 + i * (j.pincel.ancho - 260) / (es.size() - 1);
                    metajavbots.tutor.CabezaDeEntrenador.dibujar(j.pincel, es.get(i), x, 330, 80, i % 2 == 0, t + i);
                    j.pincel.textoCentrado(es.get(i).nombre.es, x, 520, 18, true, 0xFFFFFF, 1);
                    j.pincel.parrafo(es.get(i).papel.es, x - 95, 560, 190, 14, 0xBBBBBB, 1);
                }
            }
        });
        pantallas.put("03c_escenas", () -> new Pantalla() {
            private float t;

            @Override
            public void fotograma(Juego j, float dt) {
                t += dt;
                Comun.fondo(j, t);
                int n = metajavbots.pantallas.EscenasDelRobotito.cuantas();
                for (int i = 0; i < n; i++) {
                    float w = 390, h = 140;
                    metajavbots.pantallas.EscenasDelRobotito.muestra(j.pincel, i, 8 + (i % 4) * (w + 8), 8 + (i / 4) * (h + 8), w, h, t);
                }
            }
        });
        for (VistaDeArena.Escenario e : VistaDeArena.Escenario.values()) {
            pantallas.put("04_arena_" + e.name().toLowerCase(), () -> new Arena(e));
        }
        pantallas.put("05_taller", metajavbots.pantallas.Taller::new);
        pantallas.put("06_ejercicios", metajavbots.pantallas.Ejercicios::new);
        pantallas.put("07_pelear", metajavbots.pantallas.PrepararPelea::new);
        pantallas.put("08_pelea", () -> new metajavbots.pantallas.PantallaPelea(metajavbots.pantallas.PrepararPelea.encuentro(this, "ADAPTADA")));
        pantallas.put("09_ejercicio", () -> new metajavbots.pantallas.PantallaEjercicio(metajavbots.ejercicios.Banco.delJuego().deTema("05").get(2)));
        // El robotito que hace lo que hace el código: con un fallo en cada forma (cajas, puertas, pizarra, estantería, papel).
        String[][] robotitos = {{"09b_robot_cajas", "05", "return n * 2;"}, {"09c_robot_puertas", "04", "return \"HUIR\";"}};
        for (String[] rb : robotitos) {
            pantallas.put(rb[0], () -> new ProbarEjercicio(metajavbots.ejercicios.PlantillasDeEjercicios.generar(rb[1], new Random(3)), rb[2]));
        }
        for (metajavbots.ejercicios.Ejercicio ej : metajavbots.ejercicios.Banco.delJuego().todos()) {
            if (ej.tipo == metajavbots.ejercicios.Ejercicio.Tipo.METODO && ej.casos.get(0).esperado.startsWith("List.of(")
                    && ej.firma.startsWith("static List")) {
                pantallas.put("09d_robot_estanteria", () -> new ProbarEjercicio(ej, "return List.of();"));
                break;
            }
        }
        pantallas.put("09e_robot_papel", () -> new ProbarEjercicio(metajavbots.ejercicios.Banco.delJuego().deTema("01").get(0),
                "System.out.println(\"MetajavBot OFFLINE\");"));
        pantallas.put("10_tutoriales", metajavbots.pantallas.Tutoriales::new);
        pantallas.put("11_leccion", () -> new metajavbots.pantallas.PantallaLeccion(metajavbots.ejercicios.Leccion.todas().get(0)));
        pantallas.put("12_especial", metajavbots.pantallas.ModoEspecial::new);
        pantallas.put("12b_rotos", () -> metajavbots.pantallas.ModoEspecial.alFinal(1));
        nuevas();
        pantallas.put("12c_roto_pelea", () -> new metajavbots.pantallas.PantallaPelea(metajavbots.pantallas.ModoEspecial.encuentroRoto(this,
                metajavbots.ejercicios.RobotRoto.todos().get(1))));
        pantallas.put("13_laboratorio", () -> new metajavbots.pantallas.PantallaLaboratorio(metajavbots.ejercicios.Laboratorio.todos().get(0)));
        pantallas.put("14_temario", metajavbots.pantallas.Temario::new);
        pantallas.put("15_historial", metajavbots.pantallas.Historial::new);
        pantallas.put("16_panel_ia", metajavbots.pantallas.PanelIA::new);
        pantallas.put("17_ajustes", metajavbots.pantallas.Ajustes::new);
        pantallas.put("18_bienvenida", metajavbots.pantallas.Bienvenida::new);
        pantallas.put("19_rival", metajavbots.pantallas.CodigoDelRival::new);
        pantallas.put("20_maquinas", MaquinasEnMarcha::new);
        pantallas.put("21_royale", () -> new metajavbots.pantallas.PantallaPelea(metajavbots.pantallas.PrepararPelea.royale(this, java.util.List.of())));
        pantallas.put("22_equipos", () -> new metajavbots.pantallas.PantallaPelea(metajavbots.pantallas.PrepararPelea.equipos(this, null)));
        pantallas.put("23_campanas", metajavbots.pantallas.Campanas::new);
        for (String n : pantallas.keySet()) if (solo.isEmpty() || n.contains(solo)) nombres.add(n);
    }

    @Override
    public void create() {
        primera = new Escaparate();
        super.create();
        // Las partidas de las capturas, aparte: no se mezclan con las del jugador.
        metajavbots.progreso.Guardado.carpeta = java.nio.file.Paths.get("build", "partidas_de_capturas");
        Jugador j = new Jugador("Capturas");
        j.etiqueta = Etiqueta.JUNIOR_2;
        // "-Pidioma=de" y "-Pletra=24": para mirar la legibilidad en cada idioma y con la letra grande.
        j.idioma = metajavbots.textos.Idioma.de(System.getProperty("capturas.idioma", "es"));
        j.tamanoDeLetra = Float.parseFloat(System.getProperty("capturas.letra", "18"));
        // Sin sonido no hace falta Piper (la voz clara es un programa aparte).
        j.vozClara = System.getProperty("capturas.sonido") != null;
        j.musicaActiva = System.getProperty("capturas.sonido") != null;
        j.cerebros[0].tipo = metajavbots.cerebro.Ranura.Tipo.LOCAL;
        // Un cerebro que no escucha (las capturas no deben arrancar KoboldCpp de verdad).
        j.cerebros[0].direccionLocal = "http://localhost:5999";
        j.cerebros[1].tipo = metajavbots.cerebro.Ranura.Tipo.EN_LINEA;
        Jugador.Robot r = new Jugador.Robot("Cobaltito", "cobalto");
        r.codigo = Comun.plantilla(j.idioma);
        j.robots.add(r);
        Random azar = new Random(4);
        for (int i = 0; i < 24; i++) {
            j.historial.add(new metajavbots.progreso.Registro(System.currentTimeMillis() - (24 - i) * 3600_000L, i % 5 == 0 ? "AZAR" : "ADAPTADA",
                    "Rival", Modelo.todos().get(azar.nextInt(16)).id, "cobalto", 0.4, azar.nextInt(3) == 0
                    ? metajavbots.progreso.Registro.Resultado.DERROTA : metajavbots.progreso.Registro.Resultado.VICTORIA,
                    40 + azar.nextInt(120), 60 + azar.nextInt(140), 4, "JUNIOR_2"));
        }
        j.ejerciciosResueltos.addAll(java.util.List.of("t01_hola_arena", "muestra_sumar"));
        j.capitulosGanados.addAll(java.util.List.of("rocko:1", "rocko:2", "rocko:3", "rocko2:1"));
        j.robotsArreglados.add("peonza");
        j.jefesVencidos.addAll(java.util.List.of("gim_condicionales", "gim_bucles", "herencia"));
        ponerJugador(j);
        siguiente();
    }

    private void siguiente() {
        actual++;
        fotogramas = 0;
        if (actual >= nombres.size()) {
            Gdx.app.exit();
            return;
        }
        irSolo(pantallas.get(nombres.get(actual)).get());
    }

    private long nanos;
    private int medidos;

    @Override
    public void render() {
        long t0 = System.nanoTime();
        super.render();
        if (actual < 0 || actual >= nombres.size()) return;
        fotogramas++;
        // Lo que tarda cada fotograma (sin contar la espera de la pantalla): para vigilar la fluidez.
        if (fotogramas > 20) {
            nanos += System.nanoTime() - t0;
            medidos++;
        }
        if (fotogramas == 10 && System.getProperty("capturas.sonido") != null) {
            sonidos.tocar("laser", 0.1f);
            sonidos.tocar("explosion", 0.1f);
            voz.decir("Prueba", metajavbots.textos.Idioma.ES);
        }
        if (fotogramas == (nombres.get(actual).startsWith("08") || nombres.get(actual).startsWith("09c")
                || nombres.get(actual).startsWith("09d") || nombres.get(actual).startsWith("09e") || nombres.get(actual).startsWith("20")
                        || nombres.get(actual).startsWith("2")
                && nombres.get(actual).compareTo("21") >= 0 ? 400 : 45)) {
            Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            // La imagen sale del revés (OpenGL empieza abajo): se da la vuelta.
            Pixmap derecha = new Pixmap(pm.getWidth(), pm.getHeight(), Pixmap.Format.RGBA8888);
            for (int y = 0; y < pm.getHeight(); y++) derecha.drawPixmap(pm, 0, y, pm.getWidth(), 1, 0, pm.getHeight() - 1 - y, pm.getWidth(), 1);
            File f = new File(salida, nombres.get(actual) + ".png");
            PixmapIO.writePNG(Gdx.files.absolute(f.getAbsolutePath()), derecha);
            pm.dispose();
            derecha.dispose();
            System.out.println("foto: " + f + String.format(java.util.Locale.ROOT, "  (%.1f ms por fotograma)", medidos == 0 ? 0 : nanos / 1e6 / medidos));
            nanos = 0;
            medidos = 0;
            siguiente();
        }
    }

    /** Un ejercicio ya comprobado con un código dado (el robotito lo anima debajo del editor). */
    static final class ProbarEjercicio implements Pantalla {
        private final metajavbots.pantallas.PantallaEjercicio pantalla;
        private final String codigo;
        private boolean probado;

        ProbarEjercicio(metajavbots.ejercicios.Ejercicio e, String codigo) {
            pantalla = new metajavbots.pantallas.PantallaEjercicio(e);
            this.codigo = codigo;
        }

        @Override
        public void alEntrar(Juego j) {
            pantalla.alEntrar(j);
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (!probado) {
                probado = true;
                String firma = codigo.startsWith("System") ? codigo : codigo;
                pantalla.probarCon(j, pantalla.firmaCon(firma));
            }
            pantalla.fotograma(j, dt);
        }
    }

    /** Máquina contra máquina ya empezada (la pantalla de verdad, tras pulsar «¡Que empiece!»). */
    static final class MaquinasEnMarcha implements Pantalla {
        private final metajavbots.pantallas.MaquinaContraMaquina m = new metajavbots.pantallas.MaquinaContraMaquina();
        private int n;

        @Override
        public void alEntrar(Juego j) {
            m.alEntrar(j);
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (n++ == 2) m.empezarParaCapturas(j);
            m.fotograma(j, dt);
        }
    }

    /** Los dieciséis modelos de pie, en fila. */
    static final class Escaparate implements Pantalla {
        private VistaDeArena vista;
        private float t;

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) vista = new VistaDeArena(j.pincel);
            t += dt;
            Pincel p = j.pincel;
            Comun.fondo(j, t);
            List<Modelo> ms = Modelo.todos();
            int porFila = 8, filas = (ms.size() + porFila - 1) / porFila;
            float alto = (p.alto - 40) / filas;
            for (int i = 0; i < ms.size(); i++) {
                float x = 100 + (i % porFila) * ((p.ancho - 200) / (porFila - 1));
                float y = 20 + alto * (i / porFila) + alto - 40;
                vista.robotSuelto(p, new Montaje(ms.get(i)), x, y, 2.4f * Math.min(1, alto / 410) * 1.4f, t + i, 0, i % 2 == 0);
                p.textoCentrado(ms.get(i).nombre.es, x, y + 10, 18, true, 0xFFFFFF, 1);
            }
        }
    }

    /** Las armas de las luchas de robots de verdad, montadas: arriba en reposo, abajo a medio golpe. */
    static final class Armeria implements Pantalla {
        private static final String[] ARMAS = {"sierra", "pinza", "volteador", "cuna", "bola_cadena", "sierra_volteadora", "pinza_taladro", "martillo"};
        private VistaDeArena vista;
        private float t;

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) vista = new VistaDeArena(j.pincel);
            t += dt;
            Pincel p = j.pincel;
            Comun.fondo(j, t);
            metajavbots.piezas.CatalogoDePiezas cat = metajavbots.piezas.CatalogoDePiezas.delJuego();
            for (int i = 0; i < ARMAS.length; i++) {
                float x = 110 + i * ((p.ancho - 220) / (ARMAS.length - 1));
                for (int fila = 0; fila < 2; fila++) {
                    Montaje m = new Montaje(Modelo.todos().get(i * 2 % Modelo.todos().size()));
                    metajavbots.piezas.Pieza pz = cat.pieza(ARMAS[i]);
                    if (pz == null) pz = cat.pieza(ARMAS[i] + "_1");
                    if (pz != null) m.poner(Montaje.Hueco.DERECHA, pz);
                    metajavbots.pantalla.DibujoRobot.Estado e = new metajavbots.pantalla.DibujoRobot.Estado();
                    e.tiempo = t;
                    if (fila == 1) {
                        e.brazoQueAtaca = 1;
                        e.faseDelAtaque = 0.2f;
                    }
                    vista.robotConEstado(p, m, x, 380 + fila * 380, 2.4f, e);
                    if (fila == 0) p.textoCentrado(pz == null ? "?" + ARMAS[i] : pz.nombre(metajavbots.textos.Idioma.ES), x, 400, 18, true, 0xFFFFFF, 1);
                }
            }
        }
    }

    /** Una pelea a medias en un escenario (dos rivales de la computadora). */
    static final class Arena implements Pantalla {
        private final VistaDeArena.Escenario escenario;
        private VistaDeArena vista;
        private Pelea pelea;
        /** Con aparatos: 0 sin, 1 recién salido en el suelo, 2 montado y en marcha. */
        private int aparatos;

        Arena(VistaDeArena.Escenario e) {
            escenario = e;
        }

        Arena(VistaDeArena.Escenario e, int aparatos) {
            escenario = e;
            this.aparatos = aparatos;
        }

        /** Hasta que sale el aparato; con 2, el primero se lo pone y lo enciende. */
        private void prepararAparatos() {
            pelea.aparatos.activos = true;
            while (pelea.turno() <= metajavbots.combate.Aparatos.SALE) pelea.avanzar();
            if (aparatos < 2 || pelea.aparatos.todos().isEmpty()) return;
            metajavbots.combate.Aparatos.Uno a = pelea.aparatos.todos().get(0);
            metajavbots.combate.Cuerpo c = pelea.cuerpo(0);
            c.x = a.x;
            c.y = a.y;
            pelea.avanzar();
            c.conectar(a.tipo.hueco);
            for (int i = 0; i <= metajavbots.combate.Aparatos.MONTAR; i++) pelea.avanzar();
            c.usarAparato(false, 0);
            for (int i = 0; i < 6; i++) pelea.avanzar();
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) {
                vista = new VistaDeArena(j.pincel);
                vista.escenario = escenario;
                Random r = new Random(escenario.ordinal());
                List<Modelo> ms = Modelo.todos();
                pelea = new Pelea(Rivales.crear(ms.get(r.nextInt(ms.size())), 0.6, 0.5, r), Rivales.crear(ms.get(r.nextInt(ms.size())), 0.6, 0.5, r),
                        r.nextLong());
                pelea.arena = metajavbots.combate.Arena.de(escenario.name(), pelea.ancho, pelea.alto);
                pelea.empezarRonda();
                if (aparatos > 0) prepararAparatos();
                else for (int i = 0; i < 160; i++) pelea.avanzar();
            }
            if (aparatos != 1) pelea.avanzar();
            vista.sucesos(pelea, null);
            vista.dibujar(j.pincel, pelea, 0, 0, j.pincel.ancho, j.pincel.alto, dt);
        }
    }

    public static void main(String[] args) {
        File salida = new File(args.length > 0 && !args[0].isEmpty() ? args[0] : "build/capturas");
        salida.mkdirs();
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        String tam = System.getProperty("tam", "1600x900");
        String[] wh = tam.split("x");
        c.setWindowedMode(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
        c.setInitialVisible(false);
        c.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        c.disableAudio(System.getProperty("capturas.sonido") == null);
        new Lwjgl3Application(new Capturas(salida, args.length > 1 ? args[1] : ""), c);
    }
}
