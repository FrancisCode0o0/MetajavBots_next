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

    Capturas(File salida, String solo) {
        this.salida = salida;
        pantallas.put("01_inicio", metajavbots.pantallas.Inicio::new);
        pantallas.put("02_menu", metajavbots.pantallas.Menu::new);
        pantallas.put("03_robots", Escaparate::new);
        for (VistaDeArena.Escenario e : VistaDeArena.Escenario.values()) {
            pantallas.put("04_arena_" + e.name().toLowerCase(), () -> new Arena(e));
        }
        pantallas.put("05_taller", metajavbots.pantallas.Taller::new);
        pantallas.put("06_ejercicios", metajavbots.pantallas.Ejercicios::new);
        pantallas.put("07_pelear", metajavbots.pantallas.PrepararPelea::new);
        for (String n : pantallas.keySet()) if (solo.isEmpty() || n.contains(solo)) nombres.add(n);
    }

    @Override
    public void create() {
        primera = new Escaparate();
        super.create();
        Jugador j = new Jugador("Capturas");
        j.etiqueta = Etiqueta.JUNIOR_2;
        Jugador.Robot r = new Jugador.Robot("Cobaltito", "cobalto");
        r.codigo = Comun.plantilla(j.idioma);
        j.robots.add(r);
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

    @Override
    public void render() {
        super.render();
        if (actual < 0 || actual >= nombres.size()) return;
        fotogramas++;
        if (fotogramas == 45) {
            Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            // La imagen sale del revés (OpenGL empieza abajo): se da la vuelta.
            Pixmap derecha = new Pixmap(pm.getWidth(), pm.getHeight(), Pixmap.Format.RGBA8888);
            for (int y = 0; y < pm.getHeight(); y++) derecha.drawPixmap(pm, 0, y, pm.getWidth(), 1, 0, pm.getHeight() - 1 - y, pm.getWidth(), 1);
            File f = new File(salida, nombres.get(actual) + ".png");
            PixmapIO.writePNG(Gdx.files.absolute(f.getAbsolutePath()), derecha);
            pm.dispose();
            derecha.dispose();
            System.out.println("foto: " + f);
            siguiente();
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
            int porFila = 8;
            for (int i = 0; i < ms.size(); i++) {
                float x = 100 + (i % porFila) * ((p.ancho - 200) / (porFila - 1));
                float y = 370 + (i / porFila) * 410;
                vista.robotSuelto(p, new Montaje(ms.get(i)), x, y, 2.4f, t + i, 0, i % 2 == 0);
                p.textoCentrado(ms.get(i).nombre.es, x, y + 20, 18, true, 0xFFFFFF, 1);
            }
        }
    }

    /** Una pelea a medias en un escenario (dos rivales de la computadora). */
    static final class Arena implements Pantalla {
        private final VistaDeArena.Escenario escenario;
        private VistaDeArena vista;
        private Pelea pelea;

        Arena(VistaDeArena.Escenario e) {
            escenario = e;
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) {
                vista = new VistaDeArena(j.pincel);
                vista.escenario = escenario;
                Random r = new Random(escenario.ordinal());
                List<Modelo> ms = Modelo.todos();
                pelea = new Pelea(Rivales.crear(ms.get(r.nextInt(ms.size())), 0.6, 0.5, r), Rivales.crear(ms.get(r.nextInt(ms.size())), 0.6, 0.5, r), r.nextLong());
                pelea.empezarRonda();
                for (int i = 0; i < 500; i++) pelea.avanzar();
            }
            pelea.avanzar();
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
        c.disableAudio(true);
        new Lwjgl3Application(new Capturas(salida, args.length > 1 ? args[1] : ""), c);
    }
}
