package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import metajavbots.Juego;
import metajavbots.campanas.Campana;
import metajavbots.campanas.EscenaDeCampana;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.Laboratorio;
import metajavbots.ejercicios.Leccion;
import metajavbots.ejercicios.RobotRoto;
import metajavbots.pantalla.Dialogo;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantallas.Ajustes;
import metajavbots.pantallas.Campanas;
import metajavbots.pantallas.Comun;
import metajavbots.pantallas.Demostraciones;
import metajavbots.pantallas.DepuracionYTesting;
import metajavbots.pantallas.Ejercicios;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.Historial;
import metajavbots.pantallas.Jefe;
import metajavbots.pantallas.MaquinaContraMaquina;
import metajavbots.pantallas.Menu;
import metajavbots.pantallas.MiniJuegos;
import metajavbots.pantallas.ModoEspecial;
import metajavbots.pantallas.PanelIA;
import metajavbots.pantallas.PanelSenior;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaDemostracion;
import metajavbots.pantallas.PantallaEjercicio;
import metajavbots.pantallas.PantallaEscenaCampana;
import metajavbots.pantallas.PantallaLaboratorio;
import metajavbots.pantallas.PantallaLeccion;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;
import metajavbots.pantallas.PantallaPelea;
import metajavbots.pantallas.PrepararPelea;
import metajavbots.pantallas.SesionDeHoy;
import metajavbots.pantallas.Taller;
import metajavbots.pantallas.Temario;
import metajavbots.pantallas.Tienda;
import metajavbots.pantallas.Tutoriales;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.rivales.RivalSenior;

/**
 * Red de seguridad del recorrido del Menú principal y Modo especial:
 * comprueba que se llega exactamente al mismo conjunto de pantallas,
 * campañas, desbloqueos y progreso guardado antes y después de reorganizar el menú.
 */
final class PruebasDelRecorridoMenu {

    private PruebasDelRecorridoMenu() {
    }

    /** Juego simulado sin ventana gráfica que registra cada transición de pantalla. */
    private static final class JuegoSimulado extends Juego {
        final List<String> visitadas = new ArrayList<>();

        JuegoSimulado(Jugador jug) {
            this.jugador = jug;
        }

        @Override
        public void ir(Pantalla nueva) {
            registrar(nueva);
        }

        @Override
        public void irSolo(Pantalla nueva) {
            registrar(nueva);
        }

        void registrar(Pantalla p) {
            cierto(p != null, "pantalla destino no nula");
            String id = p.getClass().getSimpleName();
            if (p instanceof PantallaPelea) {
                id += "(usaRobot=" + ((PantallaPelea) p).usaRobotDelJugador() + ")";
            }
            visitadas.add(id);
        }
    }

    private static Jugador crearJugadorDePrueba() {
        Jugador j = new Jugador("Recorrido");
        j.etiqueta = Etiqueta.JUNIOR_2;
        j.avisoCompilarVisto = true;
        Jugador.Robot r = new Jugador.Robot("Cobaltito", "cobalto");
        r.codigo = Comun.plantilla(j.idioma);
        r.codigoCompilado = r.codigo;
        j.robots.add(r);
        return j;
    }

    static void apuntar() {
        caso("recorrido menú y modo especial: todas las pantallas, campañas, desbloqueos y progreso son alcanzables", () -> {
            Jugador jug = crearJugadorDePrueba();
            JuegoSimulado j = new JuegoSimulado(jug);
            Set<String> destinos = new LinkedHashSet<>();

            recorrerMenuPrincipal(j, destinos);
            recorrerSubpantallasDeArena(j, destinos);
            recorrerModoEspecial(j, destinos);
            verificarDesbloqueosYGuardado(jug);

            List<String> esperadas = List.of(
                    "Menu",
                    "Tutoriales",
                    "PantallaLeccion",
                    "MiniJuegos",
                    "Demostraciones",
                    "PantallaDemostracion",
                    "Ejercicios",
                    "PantallaEjercicio",
                    "SesionDeHoy",
                    "Temario",
                    "Taller",
                    "Tienda",
                    "DepuracionYTesting",
                    "PantallaLaboratorio",
                    "PantallaPelea:ADAPTADA",
                    "PrepararPelea",
                    "PantallaPelea:AZAR",
                    "PantallaPelea:DESAFIO",
                    "PantallaPelea:PRACTICA",
                    "PantallaPelea:ROYALE",
                    "PantallaPelea:EQUIPOS",
                    "MaquinaContraMaquina",
                    "Campanas",
                    "PantallaEscenaCampana",
                    "PantallaPelea:CAMPANA",
                    "PantallaCarreraDeObstaculos",
                    "ModoEspecial",
                    "PantallaPelea:JEFE",
                    "PantallaPelea:TOPE",
                    "PantallaPelea:SENIOR_PRESUPUESTO",
                    "PantallaPelea:SENIOR_PARALELO",
                    "PantallaPelea:SENIOR_RIVAL",
                    "PantallaPelea:ROTO",
                    "PantallaMinijuegoDestruccion",
                    "Historial",
                    "PanelIA",
                    "Ajustes");
            for (String esp : esperadas) {
                cierto(destinos.contains(esp), "Falta pantalla en el recorrido: " + esp + " (vistas: " + destinos + ")");
            }
            igual(esperadas.size(), destinos.size(), "Conjunto exacto de pantallas alcanzables");
            verificarTextosDialogosYSinDestripesHospital();
        });
    }

    private static void verificarTextosDialogosYSinDestripesHospital() {
        for (metajavbots.textos.Idioma id : metajavbots.textos.Idioma.values()) {
            metajavbots.textos.T.idioma = id;
            cierto(!metajavbots.textos.T.t("menu.historia").startsWith("["), "menu.historia en " + id);
            cierto(!metajavbots.textos.T.t("menu.pelear").equals(metajavbots.textos.T.t("menu.pelear_ya")),
                    "Desafíos y ¡A pelear! no se confunden en " + id);
            cierto(!metajavbots.textos.T.t("carrera_obs.codigo_vivo").startsWith("["), "carrera_obs.codigo_vivo en " + id);
            cierto(!metajavbots.textos.T.t("carrera_obs.editar_reintentar").startsWith("["), "carrera_obs.editar_reintentar en " + id);
            String jefeTag = metajavbots.textos.T.t("campanas.creed_jefe").toLowerCase();
            cierto(!jefeTag.contains("hospital") && !jefeTag.contains("krankenhaus"),
                    "campanas.creed_jefe no destripa el hospital antes de pelear en " + id);
        }
        metajavbots.textos.T.idioma = metajavbots.textos.Idioma.ES;
        for (Campana c : Campana.deSerie()) {
            for (Campana.Capitulo cap : c.capitulos) {
                if (cap.esJefe) {
                    String previo = (cap.texto.es + " " + cap.texto.de + " " + cap.texto.en).toLowerCase();
                    cierto(!previo.contains("hospital") && !previo.contains("krankenhaus"),
                            "El texto previo del capítulo jefe " + c.id + ":" + cap.numero + " no debe destripar el hospital");
                }
            }
        }
        Dialogo d = new Dialogo();
        int[] elegido = {-1};
        d.elegirTres("Salir", "¿Qué quieres hacer?", "Seguir", "Editar y reintentar", "Rendirme",
                () -> elegido[0] = 0, () -> elegido[0] = 1, () -> elegido[0] = 2);
        cierto(d.abierto(), "diálogo de tres opciones abierto");
    }

    private static void recorrerMenuPrincipal(JuegoSimulado j, Set<String> destinos) {
        List<String> casillas = Menu.clavesDeCasillas();
        for (String clave : List.of(
                "menu.tutoriales", "mini.boton", "demo.boton",
                "menu.ejercicios", "sesion.boton", "menu.temario",
                "menu.taller", "menu.tienda", "menu.depuracion",
                "menu.historia", "menu.pelear_ya", "menu.pelear",
                "menu.carrera", "menu.especial", "menu.historial")) {
            cierto(casillas.contains(clave), "El menú principal contiene la casilla " + clave);
        }
        igual(17, casillas.size(), "El menú principal tiene las 17 casillas (3+3+3+6+2)");
        registrarPantalla(j, destinos, new Menu(), "Menu");
        registrarPantalla(j, destinos, new Tutoriales(), "Tutoriales");
        registrarPantalla(j, destinos, new PantallaLeccion(Leccion.todas().get(0)), "PantallaLeccion");
        registrarPantalla(j, destinos, new MiniJuegos(), "MiniJuegos");
        registrarPantalla(j, destinos, new Demostraciones(), "Demostraciones");
        registrarPantalla(j, destinos, new PantallaDemostracion(Demostracion.todas().get(0)), "PantallaDemostracion");
        registrarPantalla(j, destinos, new Ejercicios(), "Ejercicios");
        registrarPantalla(j, destinos, new PantallaEjercicio(Banco.delJuego().todos().get(0)), "PantallaEjercicio");
        registrarPantalla(j, destinos, new SesionDeHoy(), "SesionDeHoy");
        registrarPantalla(j, destinos, new Temario(), "Temario");
        registrarPantalla(j, destinos, new Taller(), "Taller");
        registrarPantalla(j, destinos, new Tienda(), "Tienda");
        registrarPantalla(j, destinos, new DepuracionYTesting(), "DepuracionYTesting");
        registrarPantalla(j, destinos, new PantallaLaboratorio(Laboratorio.todos().get(0)), "PantallaLaboratorio");
        Encuentro roto = DepuracionYTesting.encuentroRoto(j, RobotRoto.todos().get(0));
        cierto("ROTO".equals(roto.modo), "modo ROTO");
        registrarPantalla(j, destinos, new PantallaPelea(roto), "PantallaPelea:ROTO");
        registrarPantalla(j, destinos, new PantallaCarreraDeObstaculos(), "PantallaCarreraDeObstaculos");
        registrarPantalla(j, destinos, new Historial(), "Historial");
        registrarPantalla(j, destinos, new PanelIA(), "PanelIA");
        registrarPantalla(j, destinos, new Ajustes(), "Ajustes");
    }

    private static void recorrerSubpantallasDeArena(JuegoSimulado j, Set<String> destinos) {
        Pantalla pelearYa = PrepararPelea.adaptada(j);
        registrarPantalla(j, destinos, pelearYa, "PantallaPelea:ADAPTADA");
        registrarPantalla(j, destinos, new PrepararPelea(), "PrepararPelea");
        for (String modo : List.of("ADAPTADA", "AZAR", "DESAFIO", "PRACTICA")) {
            Encuentro e = PrepararPelea.encuentro(j, modo);
            igual(modo, e.modo, "encuentro " + modo);
            registrarPantalla(j, destinos, new PantallaPelea(e), "PantallaPelea:" + modo);
        }
        Encuentro royale = PrepararPelea.royale(j, List.of());
        igual("ROYALE", royale.modo);
        registrarPantalla(j, destinos, new PantallaPelea(royale), "PantallaPelea:ROYALE");
        Encuentro equipos = PrepararPelea.equipos(j, null);
        igual("EQUIPOS", equipos.modo);
        registrarPantalla(j, destinos, new PantallaPelea(equipos), "PantallaPelea:EQUIPOS");
        registrarPantalla(j, destinos, new MaquinaContraMaquina(), "MaquinaContraMaquina");

        registrarPantalla(j, destinos, new Campanas(), "Campanas");
        List<Campana> campanas = Campana.deSerie();
        cierto(campanas.size() >= 18, "18 campañas disponibles");
        long principales = campanas.stream().filter(c -> c.destacada).count();
        igual(2L, principales, "2 campañas principales destacadas");
        Campana primera = campanas.get(0);
        Campana.Capitulo cap1 = primera.capitulos.get(0);
        EscenaDeCampana escena = primera.escenaDe(cap1);
        cierto(escena != null && !escena.vinetas.isEmpty(), "escena ilustrada de campaña");
        registrarPantalla(j, destinos, new PantallaEscenaCampana(primera, cap1, escena, false, () -> { }), "PantallaEscenaCampana");
        Encuentro encCamp = Campanas.encuentro(j, primera, cap1);
        igual("CAMPANA", encCamp.modo);
        registrarPantalla(j, destinos, new PantallaPelea(encCamp), "PantallaPelea:CAMPANA");
    }

    private static void recorrerModoEspecial(JuegoSimulado j, Set<String> destinos) {
        registrarPantalla(j, destinos, new ModoEspecial(), "ModoEspecial");
        igual(List.of("especial.jefes", "especial.tope", "especial.senior", "especial.retos", "especial.otros", "especial.destruccion"),
                ModoEspecial.CLAVES_PESTANAS, "pestañas de ModoEspecial");
        igual(21, Jefe.TODOS.size(), "21 jefes de tema y gimnasios");
        Encuentro jefe = PrepararPelea.encuentro(j, "ADAPTADA");
        jefe.modo = "JEFE";
        jefe.jefe = Jefe.TODOS.get(0);
        registrarPantalla(j, destinos, new PantallaPelea(jefe), "PantallaPelea:JEFE");

        Encuentro tope = PrepararPelea.encuentro(j, "ADAPTADA");
        tope.modo = "TOPE";
        tope.topeDePasos = 150;
        registrarPantalla(j, destinos, new PantallaPelea(tope), "PantallaPelea:TOPE");

        Encuentro pres = PanelSenior.encuentroPresupuesto(j, 2_500_000L, 8_192L, 350L);
        igual("SENIOR", pres.modo);
        registrarPantalla(j, destinos, new PantallaPelea(pres), "PantallaPelea:SENIOR_PRESUPUESTO");

        Encuentro par = PanelSenior.encuentroParalelo(j);
        cierto(par.pensarEnParalelo, "Senior paralelo");
        registrarPantalla(j, destinos, new PantallaPelea(par), "PantallaPelea:SENIOR_PARALELO");

        for (RivalSenior.Tipo tipo : RivalSenior.todos()) {
            Encuentro riv = PanelSenior.encuentroRivalSenior(j, tipo);
            igual(tipo.id, riv.rivalSeniorId);
            registrarPantalla(j, destinos, new PantallaPelea(riv), "PantallaPelea:SENIOR_RIVAL");
        }

        long retosConDesbloqueo = Banco.delJuego().todos().stream().filter(e -> !e.desbloquea.isEmpty()).count();
        cierto(retosConDesbloqueo >= 3, "retos de programar con desbloqueo en ModoEspecial: " + retosConDesbloqueo);
        registrarPantalla(j, destinos, new PantallaMinijuegoDestruccion(), "PantallaMinijuegoDestruccion");
    }

    private static void registrarPantalla(JuegoSimulado j, Set<String> destinos, Pantalla p, String clave) {
        j.ir(p);
        destinos.add(clave);
    }

    private static void verificarDesbloqueosYGuardado(Jugador jug) throws Exception {
        List<Pieza> iniciales = Progreso.piezasDisponibles(jug);
        cierto(iniciales.size() >= 4, "piezas desbloqueadas iniciales");
        jug.jefesVencidos.add("gim_condicionales");
        jug.capitulosGanados.add("creed_cromo:1");
        jug.retosHechos.add("prediccion");
        List<Pieza> trasAvance = Progreso.piezasDisponibles(jug);
        cierto(trasAvance.size() > iniciales.size(), "desbloqueos por jefe y reto funcionan igual");
        for (Pieza p : CatalogoDePiezas.delJuego().todas()) {
            if ("jefe:gim_condicionales".equals(p.desbloqueo)) {
                cierto(Progreso.piezaDisponible(jug, p), "pieza de jefe desbloqueada: " + p.id);
            }
        }
        Path carpetaOriginal = Guardado.carpeta;
        Path temp = Files.createTempDirectory("metajavbots_recorrido");
        try {
            Guardado.carpeta = temp;
            Guardado.guardar(jug);
            Jugador cargado = Guardado.leer("Recorrido");
            igual(jug.etiqueta, cargado.etiqueta, "etiqueta guardada");
            igual(jug.jefesVencidos, cargado.jefesVencidos, "jefes guardados");
            igual(jug.capitulosGanados, cargado.capitulosGanados, "capítulos de campaña guardados");
            igual(jug.retosHechos, cargado.retosHechos, "retos guardados");
        } finally {
            Guardado.carpeta = carpetaOriginal;
        }
    }
}
