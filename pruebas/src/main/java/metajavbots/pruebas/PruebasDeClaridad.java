package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.Leccion;
import metajavbots.ejercicios.MotorDemostracion;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantallas.Camino;
import metajavbots.pantallas.ComoSeJuega;
import metajavbots.pantallas.InformeDeRonda;
import metajavbots.pantallas.Mecanicas;
import metajavbots.pantallas.PanelDepurador;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Registro;
import metajavbots.progreso.RitmoDeAvisos;
import metajavbots.rivales.Muneco;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;
import metajavbots.tutor.Concepto;

/**
 * Pruebas de CLARIDAD: que se entienda cómo se juega cada parte. Cada modo tiene su «¿Cómo se juega?» y cada
 * mecánica su ficha, con su explicación y su pseudocódigo en es, de y en (y sin Java para copiar); el informe
 * de la ronda explica cada cifra; el camino «Empezar» abre el menú poco a poco; el entrenador avisa de las
 * órdenes al ritmo de cada etiqueta; y las líneas de la mira ya no están.
 */
final class PruebasDeClaridad {

    /** Los modos que explican en su pantalla cómo se juegan (ENCARGO_CLARIDAD, punto 2). */
    static final List<String> MODOS = List.of("juego", "taller", "historia", "pelea", "desafios", "equipos", "carrera", "destruccion",
            "minijuegos", "especial", "senior", "ejercicios", "tutoriales", "demostraciones", "tienda", "pieza", "depuracion", "mecanicas");

    /** Las mecánicas que tienen que tener ficha (ENCARGO_CLARIDAD, punto 3b). */
    static final List<String> MECANICAS = List.of("mec_turno", "mec_como_se_gana", "mec_partes", "mec_energia", "mec_armas", "mec_potencia",
            "mec_escudos", "mec_habilidades", "mec_peso", "mec_terrenos", "mec_choques", "mec_trampas", "mec_aparatos", "mec_desgaste",
            "mec_tope", "mec_senior", "mec_radar", "mec_tienda", "mec_dinero_rangos", "mec_naturaleza", "mec_poo", "mec_equipo", "mec_jefes",
            "mec_arma_especial");

    private static final String[][] ETIQUETAS = {{"Qué es:", "Las cifras:", "En la pelea:", "Desde tu código:"},
        {"Was es ist:", "Die Zahlen:", "Im Kampf:", "In deinem Code:"}, {"What it is:", "The numbers:", "In the fight:", "From your code:"}};

    private PruebasDeClaridad() {
    }

    static void apuntar() {
        apuntarModos();
        apuntarMecanicas();
        apuntarInforme();
        apuntarCamino();
        apuntarAvisos();
        apuntarTutorialesYDemo();
        apuntarTextos();
    }

    /** Pseudocódigo, no Java: sin punto y coma, sin llaves y sin «()» vacíos. */
    private static boolean sinJava(String s) {
        return !s.contains(";") && !s.contains("{") && !s.contains("}") && !s.contains("()") && !s.contains("public ");
    }

    private static void apuntarModos() {
        caso("claridad: cada modo cuenta cómo se juega (objetivo, cómo se gana, nota y pseudocódigo) en es, de y en", () -> {
            Map<String, ComoSeJuega.Modo> todos = ComoSeJuega.todos();
            for (String id : MODOS) {
                ComoSeJuega.Modo m = todos.get(id);
                cierto(m != null, "existe «¿Cómo se juega?» de " + id);
                for (Idioma i : Idioma.values()) {
                    for (Trilingue t : new Trilingue[]{m.titulo(), m.objetivo(), m.gana(), m.nota()}) cierto(!t.en(i).isBlank(), id + ": texto en " + i);
                    String frases = m.objetivo().en(i) + " " + m.gana().en(i) + " " + m.nota().en(i);
                    cierto(frases.split("[.!?](\\s|$)").length <= 7, id + ": pocas frases en " + i + " (" + frases + ")");
                    cierto(!m.pseudo(i).isBlank(), id + ": pseudocódigo en " + i);
                    cierto(sinJava(m.pseudo(i)), id + ": el pseudocódigo no es Java en " + i);
                    cierto(m.metodos(i).size() == m.metodos().size(), id + ": métodos en " + i);
                }
                for (String f : m.mecanicas()) cierto(Concepto.de(f) != null, id + " → ficha que no existe: " + f);
            }
            cierto(ComoSeJuega.de("pelea").metodos(Idioma.EN).contains("onTick"), "los métodos salen en la API del idioma (onTick)");
            cierto(ComoSeJuega.de("pelea").metodos(Idioma.DE).contains("jedenZug"), "los métodos salen en la API del idioma (jedenZug)");
        });

        caso("claridad: cada pantalla de modo tiene su botón «¿Cómo se juega?» y su id existe", () -> {
            Pantalla[] pantallas = {new metajavbots.pantallas.Menu(), new metajavbots.pantallas.Taller(), new metajavbots.pantallas.Campanas(),
                new metajavbots.pantallas.PrepararPelea(), new metajavbots.pantallas.PantallaCarreraDeObstaculos(),
                new metajavbots.pantallas.PantallaMinijuegoDestruccion(), new metajavbots.pantallas.MiniJuegos(), new metajavbots.pantallas.ModoEspecial(0),
                new metajavbots.pantallas.ModoEspecial(2), new metajavbots.pantallas.Ejercicios(), new metajavbots.pantallas.Tutoriales(),
                new metajavbots.pantallas.Demostraciones(), new metajavbots.pantallas.Tienda(), new metajavbots.pantallas.PantallaDisenarPieza(),
                new metajavbots.pantallas.DepuracionYTesting(), new Mecanicas()};
            List<String> vistos = new ArrayList<>();
            for (Pantalla p : pantallas) {
                String id = ComoSeJuega.idDe(p);
                cierto(id != null, p.getClass().getSimpleName() + ": tiene «¿Cómo se juega?»");
                vistos.add(id);
            }
            igual("senior", vistos.get(8), "la pestaña Senior del modo especial tiene el suyo");
            for (String id : MODOS) {
                if (!id.equals("pelea") && !id.equals("equipos")) cierto(vistos.contains(id), "alguna pantalla enseña " + id);
            }
        });
    }

    private static void apuntarMecanicas() {
        caso("claridad: cada mecánica tiene su ficha (qué es, cifras, en la pelea, desde tu código) y su pseudocódigo en tres idiomas", () -> {
            List<String> fichas = new ArrayList<>();
            for (Concepto c : Mecanicas.fichas()) fichas.add(c.id);
            for (String id : MECANICAS) {
                Concepto c = Concepto.de(id);
                cierto(c != null && fichas.contains(id), "ficha de " + id);
                igual(Concepto.NIVEL_JUEGO, c.nivel, id + ": en la pestaña «El juego» del entrenador");
                cierto(c.ejemplo.isBlank(), id + ": sin Java para copiar");
                for (Idioma i : Idioma.values()) {
                    List<String> ps = c.parrafos(i);
                    igual(4, ps.size(), id + ": cuatro párrafos en " + i);
                    for (int k = 0; k < 4; k++) cierto(ps.get(k).startsWith(ETIQUETAS[i.ordinal()][k]), id + ": párrafo " + (k + 1) + " en " + i);
                    cierto(!c.pseudo(i).isBlank() && sinJava(c.pseudo(i)), id + ": pseudocódigo (no Java) en " + i);
                    cierto(!c.claves(i).isEmpty(), id + ": se puede buscar en " + i);
                }
            }
            igual(MECANICAS.size(), fichas.size(), "ni fichas de más ni de menos");
        });
    }

    private static Pelea peleaDeMunecos(int turnos) {
        List<Modelo> ms = Modelo.todos();
        Combatiente a = new Combatiente("A", new Montaje(ms.get(0)), Muneco::new);
        Combatiente b = new Combatiente("B", new Montaje(ms.get(1)), Muneco::new);
        Pelea p = new Pelea(a, b, 7);
        p.rondas = 1;
        p.empezarRonda();
        for (int i = 0; i < turnos && p.estado() == Pelea.Estado.LUCHANDO; i++) p.avanzar();
        return p;
    }

    private static void apuntarInforme() {
        caso("claridad: el informe de la ronda explica cada cifra, da pistas si va mal y lleva a su ficha", () -> {
            Idioma antes = T.idioma;
            try {
                for (Idioma i : Idioma.values()) {
                    T.idioma = i;
                    Pelea p = peleaDeMunecos(200);
                    List<InformeDeRonda.Cifra> cs = InformeDeRonda.cifras(p, 0);
                    cierto(cs.size() >= 7, "siete cifras en " + i);
                    boolean sinAtaques = false;
                    for (InformeDeRonda.Cifra c : cs) {
                        cierto(!c.que().isBlank(), "qué significa " + c.texto());
                        cierto(Concepto.de(c.ficha()) != null, "ficha de " + c.texto());
                        if (c.pista() != null && c.pista().equals(T.t("informe.pista.sin_ataques"))) sinAtaques = true;
                    }
                    cierto(sinAtaques, "un robot que no ataca recibe la pista de atacar en " + i);
                    for (InformeDeRonda.Cifra c : InformeDeRonda.cifras(p, 1)) cierto(c.pista() == null, "al rival no se le dan pistas");
                    cierto(PanelDepurador.mandos(p.cuerpo(0)).size() >= 3, "paso a paso: los tres mandos en llano en " + i);
                }
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static void apuntarCamino() {
        caso("claridad: el camino «Empezar» abre el menú poco a poco y se puede abrir todo", () -> {
            for (Camino.Paso paso : Camino.PASOS) {
                if (paso.tipo().equals("leccion")) {
                    boolean hay = false;
                    for (Leccion l : Leccion.todas()) hay |= l.id.equals(paso.id());
                    cierto(hay, "existe la lección " + paso.id());
                } else if (paso.tipo().equals("ejercicio")) {
                    cierto(Banco.delJuego().ejercicio(paso.id()) != null, "existe el ejercicio " + paso.id());
                }
            }
            Jugador j = new Jugador("Camino");
            falso(j.menuCompleto, "quien empieza ve el camino");
            igual(0, Camino.siguiente(j), "empieza por el primer paso");
            falso(Camino.abierta(j, "menu.pelear_ya"), "¡A pelear! aún cerrado");
            falso(Camino.abierta(j, "menu.especial"), "el modo especial aún cerrado");
            j.tutorialesHechos.add("l00_como_obedece");
            cierto(Camino.abierta(j, "menu.tutoriales"), "tras el primer paso se abren los tutoriales");
            for (Camino.Paso paso : Camino.PASOS) {
                if (paso.tipo().equals("leccion")) j.tutorialesHechos.add(paso.id());
                if (paso.tipo().equals("ejercicio")) j.ejerciciosResueltos.add(paso.id());
            }
            j.historial.add(Registro.deLinea(lineaDeRegistro()));
            igual(Camino.PASOS.size(), Camino.siguiente(j), "camino acabado");
            cierto(Camino.abierta(j, "menu.especial"), "al acabar se ve todo");
            Jugador otro = new Jugador("Sabe");
            otro.menuCompleto = true;
            cierto(Camino.abierta(otro, "menu.especial"), "con «Ver todo el menú» se ve todo");
        });

        caso("claridad: el menú completo, los avisos y lo visto se guardan; las partidas de antes con progreso ven el menú entero", () -> {
            Path anterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("metajavbots_claridad_");
            try {
                Guardado.carpeta = temp;
                Jugador j = new Jugador("ClaridadTest");
                j.robots.add(new Jugador.Robot("Bot", "cobalto"));
                j.comoSeJuegaVisto.add("taller");
                j.fallosDeOrdenes[0] = 3;
                j.avisoDeOrdenesSalio[1] = true;
                Guardado.guardar(j);
                Jugador l = Guardado.leer(Guardado.carpetaDe(j.nombre));
                falso(l.menuCompleto, "sigue en el camino");
                cierto(l.comoSeJuegaVisto.contains("taller"), "lo visto se guarda");
                igual(3, l.fallosDeOrdenes[0], "las veces del fallo se guardan");
                cierto(l.avisoDeOrdenesSalio[1], "si salió el aviso se guarda");
                Path partida = Guardado.carpeta.resolve(Guardado.carpetaDe(j.nombre)).resolve("partida.txt");
                Files.writeString(partida, Files.readString(partida).replaceAll("(?m)^menuCompleto = .*\\n", ""));
                Files.writeString(Guardado.carpeta.resolve(Guardado.carpetaDe(j.nombre)).resolve("historial.txt"), lineaDeRegistro() + "\n");
                cierto(Guardado.leer(Guardado.carpetaDe(j.nombre)).menuCompleto, "una partida de antes con peleas ve el menú entero");
            } finally {
                Guardado.carpeta = anterior;
            }
        });
    }

    /** Una línea de historial cualquiera (una victoria), como las que escribe el juego. */
    private static String lineaDeRegistro() {
        return new Registro(1_795_000_000_000L, "ADAPTADA", "Rival", "tejedora", "cobalto", 0.2, Registro.Resultado.VICTORIA, 10, 20, 4,
                "NOOB_1").aLinea();
    }

    private static void apuntarAvisos() {
        caso("claridad: el consejo de las órdenes sale al ritmo de la etiqueta (Noob, Junior, Senior y Master)", () -> {
            Etiqueta.Categoria noob = Etiqueta.Categoria.NOOB, junior = Etiqueta.Categoria.JUNIOR;
            cierto(RitmoDeAvisos.toca(noob, 1, false, 0.9), "Noob: la primera pelea con el fallo, sí");
            falso(RitmoDeAvisos.toca(noob, 2, true, 0.0), "Noob: la segunda, no");
            cierto(RitmoDeAvisos.toca(noob, 3, false, 0.9), "Noob: la tercera, sí");
            falso(RitmoDeAvisos.toca(noob, 4, true, 0.0), "Noob: la cuarta, no");
            falso(RitmoDeAvisos.toca(junior, 2, true, 0.0), "Junior: nunca dos veces seguidas");
            cierto(RitmoDeAvisos.toca(junior, 3, false, 0.0), "Junior: al azar, sí");
            falso(RitmoDeAvisos.toca(junior, 3, false, 0.99), "Junior: al azar, no");
            int senior = 0, master = 0;
            for (int vez = 1; vez <= 24; vez++) {
                if (RitmoDeAvisos.toca(Etiqueta.Categoria.SENIOR, vez, false, 0)) senior++;
                if (RitmoDeAvisos.toca(Etiqueta.Categoria.MASTER, vez, false, 0)) master++;
            }
            igual(24 / RitmoDeAvisos.CADA_SENIOR, senior, "Senior: una de cada " + RitmoDeAvisos.CADA_SENIOR);
            igual(24 / RitmoDeAvisos.CADA_MASTER, master, "Master: una de cada " + RitmoDeAvisos.CADA_MASTER);
        });

        caso("claridad: las dos líneas de la mira ya no están en la pelea ni en el depurador", () -> {
            falso(T.existe("pelea.depurador_mira"), "sin el botón «Ver la mira»");
            for (java.lang.reflect.Method m : PanelDepurador.class.getDeclaredMethods()) falso(m.getName().equals("mira"), "sin dibujo de la mira");
        });
    }

    private static void apuntarTutorialesYDemo() {
        caso("claridad: los tutoriales nuevos (cómo obedece, primera pelea, carrera, destrucción, equipo) en tres idiomas y sin Java", () -> {
            List<String> orden = new ArrayList<>();
            for (Leccion l : Leccion.todas()) orden.add(l.id);
            cierto(orden.indexOf("l00_como_obedece") >= 0 && orden.indexOf("l00_como_obedece") < orden.indexOf("l01_hola_arena"),
                    "«Así obedece tu robot» va antes de «Hola, arena»");
            cierto(orden.indexOf("l01_primera_pelea") == orden.indexOf("l01_hola_arena") + 1, "la primera pelea completa va justo después");
            for (String id : List.of("l00_como_obedece", "l01_primera_pelea", "l05_carrera", "l05_destruccion", "l15_equipo")) {
                Leccion l = null;
                for (Leccion x : Leccion.todas()) if (x.id.equals(id)) l = x;
                cierto(l != null, "existe " + id);
                cierto(!l.tareas.isEmpty(), id + ": tiene tarea");
                for (Leccion.Pagina pg : l.paginas) {
                    cierto(pg.codigo.isBlank(), id + ": sin código para copiar");
                    for (Idioma i : Idioma.values()) {
                        cierto(!pg.texto.en(i).isBlank(), id + ": página en " + i);
                        String tx = pg.texto.en(i);
                        falso(java.util.regex.Pattern.compile("\\w\\([^)]*\\)\\s*;").matcher(tx).find() || tx.contains("public ") || tx.contains("{"),
                                id + ": sin Java en " + i);
                    }
                }
            }
        });

        caso("claridad: la demostración «Tu primera pelea, en palabras» corre entera en la arena en es, de y en", () -> {
            Demostracion d = Demostracion.de(Demostracion.ID_DEMO_5);
            cierto(d != null, "existe la demo 5");
            for (Idioma i : Idioma.values()) {
                cierto(sinJava(d.codigo(i)), "la hoja de la demo es pseudocódigo en " + i);
                MotorDemostracion m = new MotorDemostracion(d, i);
                m.empezar();
                for (int paso = 0; paso < d.pasos.size(); paso++) {
                    for (int k = 0; k < 400 && !m.pausado(); k++) m.avanzarTurno();
                    cierto(m.ultimoError().isEmpty(), i + ": sin error en el paso " + paso + ": " + m.ultimoError());
                    m.continuar();
                }
                cierto(m.terminada(), i + ": la demo acaba");
            }
        });
    }

    private static void apuntarTextos() {
        caso("claridad: los textos nuevos de la interfaz, en es, de y en", () -> {
            String[] claves = {"como.boton", "como.boton_ayuda", "como.titulo", "como.objetivo", "como.gana", "como.metodos", "como.idea", "como.ojo",
                "como.mecanicas", "como.seguir", "como.todas", "como.todas_ayuda", "mecanicas.titulo", "mecanicas.intro", "mecanicas.idea",
                "mecanicas.ver", "camino.titulo", "camino.paso", "camino.empezar", "camino.seguir", "camino.todo", "camino.todo_ayuda",
                "camino.que.leccion", "camino.que.ejercicio", "camino.que.pelea", "camino.pelea", "menu.mecanicas", "menu.mecanicas_ayuda",
                "ajustes.camino", "ajustes.camino_ayuda", "peso.ficha", "camino.se_abre",
                "pelea.mandos", "pelea.mando_ruedas",
                "pelea.mando_ruedas_quietas", "pelea.mando_cuerpo", "pelea.mando_cuerpo_quieto", "pelea.mando_cabeza", "pelea.mando_cabeza_llego",
                "pelea.mando_disparo", "aviso.ordenes.titulo", "informe.pista_titulo", "informe.ficha", "informe.que.arma", "informe.que.poo"};
            List<String> todas = new ArrayList<>(List.of(claves));
            for (String s : new String[]{"dano_recibido", "dano_hecho", "punteria", "visto", "errores", "topes"}) {
                todas.add("informe.que." + s);
                todas.add("informe.pista." + s);
            }
            todas.add("informe.que.total");
            todas.add("informe.pista.sin_ataques");
            for (String k : todas) {
                Trilingue t = T.todos().get(k);
                cierto(t != null, "existe " + k);
                for (Idioma i : Idioma.values()) cierto(!t.en(i).isBlank(), k + " en " + i);
            }
        });
    }
}
