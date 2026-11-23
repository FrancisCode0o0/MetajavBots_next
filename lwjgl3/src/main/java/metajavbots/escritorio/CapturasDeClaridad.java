package metajavbots.escritorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import metajavbots.Juego;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.Leccion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.pantallas.Camino;
import metajavbots.pantallas.ComoSeJuega;
import metajavbots.pantallas.Comun;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.Mecanicas;
import metajavbots.pantallas.Menu;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaDemostracion;
import metajavbots.pantallas.PantallaLeccion;
import metajavbots.pantallas.PantallaPelea;
import metajavbots.pantallas.PrepararPelea;
import metajavbots.pantallas.Taller;
import metajavbots.progreso.Jugador;

/**
 * CAPTURAS DE CLARIDAD — Lo nuevo de ENCARGO_CLARIDAD («-Psolo=38»): el menú con
 * el camino «Empezar» (al principio y a medias), «¿Cómo se juega?» del juego, del
 * Taller y de la carrera, el índice de mecánicas con dos fichas, el informe de la
 * ronda con sus pistas, el aviso del entrenador al borde de la arena, el paso a
 * paso con los mandos, dos tutoriales nuevos y la demostración en palabras.
 * Aparte, porque Capturas está en el límite de líneas.
 */
final class CapturasDeClaridad {

    private CapturasDeClaridad() {
    }

    /** El jugador de las capturas ve el menú entero y ya ha visto las tarjetas (las de abajo las abren a propósito). */
    static void prepararJugador(Jugador j) {
        j.menuCompleto = true;
        j.comoSeJuegaVisto.addAll(ComoSeJuega.todos().keySet());
    }

    static void registrar(Map<String, Supplier<Pantalla>> pantallas) {
        pantallas.put("38a_menu_camino_inicio", () -> new MenuDelCamino(0));
        pantallas.put("38b_menu_camino_mitad", () -> new MenuDelCamino(7));
        pantallas.put("38c_como_juego", () -> new ConTarjeta(new Menu(), "juego"));
        pantallas.put("38d_como_taller", () -> new ConTarjeta(new Taller(), "taller"));
        pantallas.put("38e_como_carrera", () -> new ConTarjeta(new PantallaCarreraDeObstaculos(), "carrera"));
        pantallas.put("38f_mecanicas_turno", () -> new Mecanicas("mec_turno"));
        pantallas.put("38g_mecanicas_peso", () -> new Mecanicas("mec_peso"));
        pantallas.put("38h_informe_ronda", () -> new Pelea(Pelea.Hasta.INFORME));
        pantallas.put("38i_aviso_al_borde", () -> new Pelea(Pelea.Hasta.AVISO));
        pantallas.put("38j_paso_a_paso", () -> new Pelea(Pelea.Hasta.PASO_A_PASO));
        pantallas.put("38k_leccion_obedece", () -> new PantallaLeccion(leccion("l00_como_obedece"), 3));
        pantallas.put("38l_leccion_primera_pelea", () -> new PantallaLeccion(leccion("l01_primera_pelea"), 4));
        pantallas.put("38m_demo_en_palabras", () -> new PantallaDemostracion(Demostracion.de(Demostracion.ID_DEMO_5), 5));
    }

    private static Leccion leccion(String id) {
        for (Leccion l : Leccion.todas()) if (l.id.equals(id)) return l;
        return Leccion.todas().get(0);
    }

    /** El menú de quien empieza: el camino con esos pasos hechos (y al salir, el jugador como estaba). */
    private static final class MenuDelCamino implements Pantalla {
        private final int hechos;
        private final Menu menu = new Menu();
        private final List<String> lecciones = new ArrayList<>(), ejercicios = new ArrayList<>();
        private List<metajavbots.progreso.Registro> historial;

        MenuDelCamino(int hechos) {
            this.hechos = hechos;
        }

        @Override
        public void alEntrar(Juego j) {
            Jugador jug = j.jugador;
            jug.menuCompleto = false;
            lecciones.addAll(jug.tutorialesHechos);
            ejercicios.addAll(jug.ejerciciosResueltos);
            historial = new ArrayList<>(jug.historial);
            jug.tutorialesHechos.clear();
            jug.ejerciciosResueltos.clear();
            jug.historial.clear();
            for (int i = 0; i < hechos && i < Camino.PASOS.size(); i++) {
                Camino.Paso p = Camino.PASOS.get(i);
                if (p.tipo().equals("leccion")) jug.tutorialesHechos.add(p.id());
                else if (p.tipo().equals("ejercicio")) jug.ejerciciosResueltos.add(p.id());
                else jug.historial.add(historial.isEmpty() ? null : historial.get(0));
            }
            menu.alEntrar(j);
        }

        @Override
        public void alSalir(Juego j) {
            Jugador jug = j.jugador;
            jug.menuCompleto = true;
            jug.tutorialesHechos.clear();
            jug.tutorialesHechos.addAll(lecciones);
            jug.ejerciciosResueltos.clear();
            jug.ejerciciosResueltos.addAll(ejercicios);
            jug.historial.clear();
            jug.historial.addAll(historial);
        }

        @Override
        public String comoSeJuega() {
            return menu.comoSeJuega();
        }

        @Override
        public void fotograma(Juego j, float dt) {
            menu.fotograma(j, dt);
        }
    }

    /** Una pantalla con su «¿Cómo se juega?» abierto encima. */
    private static final class ConTarjeta implements Pantalla {
        private final Pantalla pantalla;
        private final String id;

        ConTarjeta(Pantalla pantalla, String id) {
            this.pantalla = pantalla;
            this.id = id;
        }

        @Override
        public void alEntrar(Juego j) {
            pantalla.alEntrar(j);
            j.comoSeJuega.abrir(j, id);
        }

        @Override
        public String comoSeJuega() {
            return pantalla.comoSeJuega();
        }

        @Override
        public void fotograma(Juego j, float dt) {
            pantalla.fotograma(j, dt);
        }
    }

    /** Una pelea con la plantilla, deprisa hasta el momento que se quiere enseñar. */
    private static final class Pelea implements Pantalla {
        enum Hasta { INFORME, AVISO, PASO_A_PASO }

        private final Hasta hasta;
        private PantallaPelea pp;
        private String codigoAntes;
        private boolean parada;
        private int vueltas;

        Pelea(Hasta hasta) {
            this.hasta = hasta;
        }

        @Override
        public void alEntrar(Juego j) {
            codigoAntes = j.jugador.robot().codigo;
            j.jugador.robot().codigo = Comun.plantilla(j.jugador.idioma);
            // Como si fuera la primera vez que comete el fallo (si no, el ritmo de su etiqueta puede callarlo).
            j.jugador.fallosDeOrdenes[0] = 0;
            j.jugador.fallosDeOrdenes[1] = 0;
            j.jugador.avisoDeOrdenesSalio[0] = false;
            j.jugador.avisoDeOrdenesSalio[1] = false;
            Encuentro e = PrepararPelea.encuentro(j, "ADAPTADA");
            e.escenario = VistaDeArena.Escenario.LABORATORIO;
            e.rondas = 2;
            pp = new PantallaPelea(e);
            pp.alEntrar(j);
        }

        @Override
        public void alSalir(Juego j) {
            if (codigoAntes != null) j.jugador.robot().codigo = codigoAntes;
        }

        @Override
        public void fotograma(Juego j, float dt) {
            for (int i = 0; i < 60 && !parada; i++) {
                pp.fotograma(j, 1 / 20f);
                vueltas++;
                switch (hasta) {
                    case INFORME:
                        parada = pp.enLaPausaEntreRondas();
                        break;
                    case AVISO:
                        if (pp.tieneConflictoDeOrdenes()) {
                            // Unos fotogramas más para que el panel termine de entrar, y quieta.
                            for (int k = 0; k < 12; k++) pp.fotograma(j, 1 / 20f);
                            pp.pausar(true);
                            parada = true;
                        }
                        break;
                    default:
                        // Cuando ya pelea (tras la presentación): la plantilla pisa una orden en los primeros turnos.
                        if (pp.tieneConflictoDeOrdenes() && vueltas > 20) {
                            pp.empezarPasoAPaso();
                            parada = true;
                        }
                        break;
                }
            }
            pp.fotograma(j, dt);
        }
    }
}
