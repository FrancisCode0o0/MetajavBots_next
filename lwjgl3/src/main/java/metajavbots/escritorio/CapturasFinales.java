package metajavbots.escritorio;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import metajavbots.Juego;
import metajavbots.campanas.Campana;
import metajavbots.campanas.CatalogoDeEscenas;
import metajavbots.combate.Armas;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Demostracion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Campanas;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.PanelSenior;
import metajavbots.pantallas.PantallaBiografias;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaDemostracion;
import metajavbots.pantallas.PantallaDisenarPieza;
import metajavbots.pantallas.PantallaEscenaCampana;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;
import metajavbots.pantallas.PantallaPelea;
import metajavbots.pantallas.PrepararPelea;
import metajavbots.piezas.Modelo;
import metajavbots.rivales.Rivales;
import metajavbots.rivales.RivalSenior;

/**
 * CAPTURAS FINALES — Las pantallas que faltaban en el repaso final de
 * METAJAVBOTS_3: demostraciones 3 y 4, peleas Senior (presupuesto y robots en
 * paralelo), el aura de los escudos en la arena, el HUD con depurador y
 * entrenador a la vez, la pieza propia, las biografías, las escenas de las
 * campañas (Creed e Isla) con el hospital, y la Carrera y Destrucción tal como
 * se abren. Aparte, para no hacer crecer Capturas (en el límite de líneas).
 */
final class CapturasFinales {

    private CapturasFinales() {
    }

    static void registrar(Map<String, Supplier<Pantalla>> pantallas, Juego juego) {
        pantallas.put("27b_senior_paralelo", () -> new PantallaPelea(PanelSenior.encuentroParalelo(juego)));
        pantallas.put("27c_senior_rival", () -> new PantallaPelea(PanelSenior.encuentroRivalSenior(juego, RivalSenior.Tipo.values()[0])));
        pantallas.put("28a_escudo_arena", ArenaConEscudos::new);
        pantallas.put("28b_hud_depurador_entrenador", () -> new PeleaConTodo(PrepararPelea.encuentro(juego, "ADAPTADA"), true));
        pantallas.put("29a_campana_creed_hospital", () -> new PeleaConTodo(jefeDe(juego, "creed1"), false));
        pantallas.put("29b_campana_isla_hospital", () -> new PeleaConTodo(jefeDe(juego, "isla_torneo"), false));
        pantallas.put("32a_demo3", () -> new PantallaDemostracion(Demostracion.de(Demostracion.ID_DEMO_3)));
        pantallas.put("32b_demo4", () -> new PantallaDemostracion(Demostracion.de(Demostracion.ID_DEMO_4)));
        pantallas.put("32c_demo4_paso", () -> new PantallaDemostracion(Demostracion.de(Demostracion.ID_DEMO_4), 3));
        pantallas.put("33_disenar_pieza", PantallaDisenarPieza::new);
        pantallas.put("34a_biografias_robots", () -> new PantallaBiografias(false, null));
        pantallas.put("34b_biografias_entrenadores", () -> new PantallaBiografias(true, null));
        escenas(pantallas);
        pantallas.put("36a_carrera", PantallaCarreraDeObstaculos::new);
        pantallas.put("36b_destruccion", PantallaMinijuegoDestruccion::new);
    }

    private static void escenas(Map<String, Supplier<Pantalla>> pantallas) {
        String[][] cuales = {{"35a_escena_creed", "creed1", "0"}, {"35b_escena_creed_jefe", "creed1", "jefe"},
            {"35c_escena_isla", "isla_torneo", "0"}, {"35d_escena_isla_jefe", "isla_torneo", "jefe"}};
        for (String[] e : cuales) {
            pantallas.put(e[0], () -> {
                Campana c = Campana.de(e[1]);
                Campana.Capitulo k = "jefe".equals(e[2]) ? primerJefe(c) : c.capitulos.get(0);
                return new PantallaEscenaCampana(c, k, CatalogoDeEscenas.paraCapitulo(c, k), false, null);
            });
        }
    }

    private static Campana.Capitulo primerJefe(Campana c) {
        for (Campana.Capitulo k : c.capitulos) {
            if (k.esJefe) return k;
        }
        return c.capitulos.get(c.capitulos.size() - 1);
    }

    private static Encuentro jefeDe(Juego j, String campana) {
        Campana c = Campana.de(campana);
        return Campanas.encuentro(j, c, primerJefe(c));
    }

    /** Una pelea con el depurador abierto y el entrenador flotante (o en el hospital, contra un jefe). */
    static final class PeleaConTodo implements Pantalla {
        private final PantallaPelea pelea;
        private final boolean depurador;
        private int n;

        PeleaConTodo(Encuentro e, boolean depurador) {
            this.pelea = new PantallaPelea(e);
            this.depurador = depurador;
        }

        @Override
        public void alEntrar(Juego j) {
            pelea.alEntrar(j);
            if (depurador) pelea.abrirDepurador(1);
        }

        @Override
        public void alSalir(Juego j) {
            pelea.alSalir(j);
        }

        @Override
        public boolean entrenadorEnElHospital() {
            return pelea.entrenadorEnElHospital();
        }

        @Override
        public void fotograma(Juego j, float dt) {
            if (++n == 60) j.entrenadorFlotante.abrir(j, null);
            pelea.fotograma(j, dt);
        }
    }

    /** Dos robots con escudo puesto: de proyectiles (cian) en el brazo y de impacto (naranja, doble) en la cabeza. */
    static final class ArenaConEscudos implements Pantalla {
        private VistaDeArena vista;
        private Pelea pelea;

        @Override
        public void fotograma(Juego j, float dt) {
            if (vista == null) {
                vista = new VistaDeArena(j.pincel);
                vista.escenario = VistaDeArena.Escenario.NEON;
                Random r = new Random(7);
                List<Modelo> ms = Modelo.todos();
                pelea = new Pelea(Rivales.crear(ms.get(2), 0.6, 0.5, r), Rivales.crear(ms.get(5), 0.6, 0.5, r), 7);
                pelea.empezarRonda();
                for (int i = 0; i < 120; i++) pelea.avanzar();
            }
            pelea.cuerpo(0).escudoBrazo = 99;
            pelea.cuerpo(0).tipoEscudoBrazo = Armas.TipoProteccion.PROYECTIL;
            pelea.cuerpo(1).escudoCabeza = 99;
            pelea.cuerpo(1).tipoEscudoCabeza = Armas.TipoProteccion.IMPACTO;
            pelea.avanzar();
            vista.sucesos(pelea, null);
            vista.dibujar(j.pincel, pelea, 0, 0, j.pincel.ancho, j.pincel.alto, dt);
        }
    }
}
