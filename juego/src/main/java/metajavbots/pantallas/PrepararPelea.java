package metajavbots.pantallas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.rivales.Dificultad;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * PREPARAR LA PELEA — Elegir contra quién: la computadora adaptada a la
 * etiqueta (o con la dificultad fija del panel de la IA), el modo azar, el
 * desafío (una IA unos escalones por encima), una práctica sin consecuencias
 * o los robots de otros jugadores (archivos .java en robots_ajenos/).
 */
public final class PrepararPelea implements Pantalla {

    private float t;
    private int robotElegido = -1;
    private String aviso = "";
    private List<Path> ajenos = new ArrayList<>();
    private final Ui.Desplazable pagina = new Ui.Desplazable();
    /** Qué cerebro de IA llevan los robots de la computadora en el battle royale. */
    private int cerebrosRoyale = EstiloPorIA.EL_RAPIDO;

    /** La pelea normal, directa (el botón grande del menú). */
    public static Pantalla adaptada(Juego j) {
        return new PantallaPelea(encuentro(j, j.jugador.modoIA.equals("ADAPTADA") ? "ADAPTADA" : j.jugador.modoIA));
    }

    /** Otra igual que la anterior (revancha), con un rival nuevo del mismo modo. */
    public static Pantalla repetir(Juego j, Encuentro anterior) {
        if (anterior.otraVez != null) return new PantallaPelea(anterior.otraVez.get());
        if (anterior.modo.equals("ROYALE")) return new PantallaPelea(royale(j, robotsAjenos()));
        if (anterior.modo.equals("EQUIPOS")) return new PantallaPelea(equipos(j, null));
        if (anterior.modo.equals("JEFE") || anterior.modo.equals("TUTORIAL") || anterior.modo.equals("TEMARIO") || anterior.modo.equals("AJENO")) {
            Encuentro e = new Encuentro();
            e.rival = Rivales.crear(anterior.rival.montaje.modelo, anterior.nivelRival, j.jugador.agresividad, new Random());
            if (anterior.modo.equals("AJENO")) {
                e.rival = anterior.rival;
            } else if (anterior.modo.equals("JEFE") && anterior.jefe != null) {
                e.rival = new Combatiente(anterior.jefe.nombre(T.idioma), e.rival.montaje, e.rival.fabrica);
                e.rival.computadora = true;
            }
            e.modo = anterior.modo;
            e.nivelRival = anterior.nivelRival;
            e.jefe = anterior.jefe;
            if (anterior.musica != null && !anterior.musica.isEmpty()) e.musica = anterior.musica;
            e.rondas = anterior.rondas;
            e.objetivo = anterior.objetivo;
            e.alAcabar = anterior.alAcabar;
            e.cuenta = anterior.cuenta;
            e.topeDePasos = anterior.topeDePasos;
            e.escenario = escenarioAlAzar();
            return new PantallaPelea(e);
        }
        return new PantallaPelea(encuentro(j, anterior.modo));
    }

    /** Las rondas que se pueden elegir, como en el boxeo. */
    static final int[] RONDAS_A_ELEGIR = {3, 4, 6, 10, 12};

    /** Si las peleas al azar pueden salir en arenas con trampas (Ajustes; de fábrica, sí). */
    public static boolean conTrampas = true;

    static VistaDeArena.Escenario escenarioAlAzar() {
        VistaDeArena.Escenario[] e = VistaDeArena.Escenario.values();
        // Sin trampas, solo los cinco de siempre (los primeros).
        int n = conTrampas ? e.length : VistaDeArena.Escenario.CHATARRERIA.ordinal();
        return e[new Random().nextInt(n)];
    }

    /** Monta el encuentro de un modo: ADAPTADA, una Dificultad, AZAR, DESAFIO o PRACTICA. */
    public static Encuentro encuentro(Juego j, String modo) {
        return encuentro(j.jugador, modo, new Random());
    }

    /** Monta el encuentro para un jugador con un azar dado (usable también en pruebas sin ventana). */
    public static Encuentro encuentro(Jugador jug, String modo, Random azar) {
        Encuentro e = new Encuentro();
        e.modo = modo;
        e.escenario = escenarioAlAzar();
        List<Modelo> modelos = Modelo.todos();
        Modelo m = modelos.get(azar.nextInt(modelos.size()));
        switch (modo) {
            case "AZAR":
                e.nivelRival = Progreso.nivelParaLaPelea(jug, azar);
                e.rival = Rivales.alAzar(e.nivelRival, jug.agresividad, azar);
                break;
            case "DESAFIO":
                e.nivelRival = Progreso.nivelDelDesafio(jug);
                e.rival = Rivales.crear(m, e.nivelRival, Math.max(0.6, jug.agresividad), azar);
                break;
            case "PRACTICA":
                e.nivelRival = 0.15;
                e.cuenta = false;
                e.rondas = 1;
                e.rival = Rivales.crear(m, e.nivelRival, 0.2, azar);
                break;
            case "ADAPTADA":
                e.nivelRival = Progreso.nivelParaLaPelea(jug, azar);
                e.rival = Rivales.crear(m, e.nivelRival, jug.agresividad, azar);
                break;
            default: {
                Dificultad d;
                try {
                    d = Dificultad.valueOf(modo);
                } catch (IllegalArgumentException ex) {
                    d = Dificultad.NORMAL;
                }
                e.nivelRival = d.nivel;
                e.rival = Rivales.crear(m, e.nivelRival, jug.agresividad, azar);
                break;
            }
        }
        if (!"PRACTICA".equals(modo) && !"DESAFIO".equals(modo) && Progreso.tocaMultiplesObjetivos(jug, azar)) {
            poblarMultiplesObjetivos(e, jug, modelos, azar);
        }
        if (e.rondas == Pelea.RONDAS) e.rondas = jug.rondas;
        return e;
    }

    /** Convierte una pelea normal en un encuentro de 1 contra varios: rival + drones o 2–3 robots ligeros. */
    private static void poblarMultiplesObjetivos(Encuentro e, Jugador jug, List<Modelo> modelos, Random azar) {
        boolean seniorOMas = jug.etiqueta.categoria().ordinal() >= metajavbots.progreso.Etiqueta.Categoria.SENIOR.ordinal();
        boolean noob4 = jug.etiqueta == metajavbots.progreso.Etiqueta.NOOB_4;
        if (azar.nextBoolean()) {
            // Variante 1: rival principal + 1 o 2 drones ligeros de escolta.
            int drones = noob4 ? 1 : (seniorOMas || azar.nextBoolean() ? 2 : 1);
            e.rival.escalaBlindaje = drones == 1 ? 0.80 : 0.72;
            e.rival.escalaDano = drones == 1 ? 0.75 : 0.68;
            for (int i = 1; i <= drones; i++) {
                e.demas.add(Rivales.dron(e.nivelRival, jug.agresividad, azar, i));
                e.equiposDeLosDemas.add(1);
            }
        } else {
            // Variante 2: escuadrón de 2 o 3 enemigos ligeros simultáneos.
            int total = noob4 ? 2 : (seniorOMas || azar.nextBoolean() ? 3 : 2);
            Set<String> usados = new HashSet<>();
            e.rival = Rivales.ligero(modelos.get(azar.nextInt(modelos.size())), e.nivelRival, jug.agresividad, azar, total, usados);
            for (int i = 1; i < total; i++) {
                e.demas.add(Rivales.ligero(modelos.get(azar.nextInt(modelos.size())), e.nivelRival, jug.agresividad, azar, total, usados));
                e.equiposDeLosDemas.add(1);
            }
        }
    }

    /** Los posibles compañeros de equipo: {nombre, tipo («ajeno» o «partida»), ruta o carpeta}. -1: la computadora. */
    private final List<String[]> companeros = new ArrayList<>();
    private int companero = -1;

    @Override
    public void alEntrar(Juego j) {
        ajenos = robotsAjenos();
        companeros.clear();
        for (Path a : ajenos) companeros.add(new String[]{a.getFileName().toString().replace(".java", ""), "ajeno", a.toString()});
        for (String carpeta : metajavbots.progreso.Guardado.partidas()) {
            if (carpeta.equals(metajavbots.progreso.Guardado.carpetaDe(j.jugador.nombre))) continue;
            companeros.add(new String[]{carpeta.replace('_', ' '), "partida", carpeta});
        }
        if (robotElegido < 0) robotElegido = j.jugador.robotElegido;
    }

    static List<Path> robotsAjenos() {
        List<Path> l = new ArrayList<>();
        Path d = Paths.get("robots_ajenos");
        if (!Files.isDirectory(d)) return l;
        try (Stream<Path> s = Files.list(d)) {
            s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(l::add);
        } catch (Exception e) {
            // nada
        }
        return l;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, T.t("menu.pelear"), true);
        Jugador jug = j.jugador;
        // Qué robot pelea (si tiene varios).
        float y = arriba + 20;
        if (jug.robots.size() > 1) {
            p.texto(T.t("pelear.tu_robot"), 40, y + 12, ui.letra, false, ui.tema.textoSuave, 1);
            for (int i = 0; i < jug.robots.size(); i++) {
                if (ui.boton(jug.robots.get(i).nombre, 240 + i * 230, y, 220, 48, i == jug.robotElegido ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO)) {
                    jug.robotElegido = i;
                }
            }
            y += 70;
        }
        // Cuántas rondas, como en el boxeo: a más rondas, más desgaste.
        p.texto(T.t("pelear.rondas"), 40, y + 12, ui.letra, false, ui.tema.textoSuave, 1);
        float rx = 40 + p.ancho(T.t("pelear.rondas"), ui.letra, false) + 20;
        for (int r : RONDAS_A_ELEGIR) {
            if (ui.boton(Integer.toString(r), rx, y, 64, 48, jug.rondas == r ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true, T.t("pelear.rondas_ayuda"))) {
                jug.rondas = r;
            }
            rx += 72;
        }
        y += 70;
        y = vistaPreviaDeArena(j, y);
        String[][] modos = {
                {"ADAPTADA", "pelear.adaptada"}, {"AZAR", "pelear.azar"}, {"DESAFIO", "pelear.desafio"}, {"PRACTICA", "pelear.practica"},
                {"ROYALE", "pelear.royale"}, {"EQUIPOS", "pelear.equipos"}, {"MCM", "pelear.mcm"}, {"CAMPANAS", "pelear.campanas"}};
        int columnas = 4;
        float w = (p.ancho - 80 - (columnas - 1) * 20) / columnas;
        // Todo se desplaza si no cabe (letra grande): nada se sale de su tarjeta ni se monta.
        float zonaY = y, zonaH = p.alto - zonaY - 10;
        float desp = ui.desplazable(pagina, 0, zonaY, p.ancho, zonaH);
        y -= desp;
        float y0 = y;
        for (int fila = 0; fila * columnas < modos.length; fila++) {
            float h = 0;
            for (int i = fila * columnas; i < Math.min(modos.length, (fila + 1) * columnas); i++) h = Math.max(h, altoDeTarjeta(j, modos[i], w));
            for (int i = fila * columnas; i < Math.min(modos.length, (fila + 1) * columnas); i++) {
                tarjeta(j, modos[i], i == 0, 40 + (i % columnas) * (w + 20), y, w, h);
            }
            y += h + 16;
        }
        y += 10;
        // Los robots de otros jugadores.
        float pw = p.ancho - 80;
        float ayuda = p.altoDeParrafo(T.t("pelear.ajenos_ayuda"), pw - 40, ui.letra * 0.85f);
        float lista = Math.max(1, ajenos.size()) * 56;
        float alto = 26 + ui.letra * 1.4f + ayuda + 20 + lista + 20;
        float cy = ui.panel(40, y, pw, alto, T.t("pelear.ajenos"));
        cy += p.parrafo(T.t("pelear.ajenos_ayuda"), 60, cy, pw - 40, ui.letra * 0.85f, ui.tema.textoSuave, 1) + 20;
        for (Path a : ajenos) {
            String nombre = a.getFileName().toString();
            p.texto(ui.recortarTexto(nombre, pw - 360, ui.letra, false), 70, cy + 12, ui.letra, false, ui.tema.texto, 1);
            if (ui.boton(T.t("pelear.contra_este"), 50 + pw - 300, cy, 250, 46, Ui.Estilo.NORMAL)) pelearContraAjeno(j, a);
            cy += 56;
        }
        if (ajenos.isEmpty()) p.texto(T.t("pelear.sin_ajenos"), 70, cy + 10, ui.letra * 0.9f, false, ui.tema.textoApagado, 1);
        y += alto + 20;
        if (!aviso.isEmpty()) y += p.parrafo(aviso, 60, y, pw - 40, ui.letra * 0.85f, ui.tema.mal, 1) + 20;
        ui.finDesplazable(pagina, y + desp - zonaY);
    }

    /** La arena que se ve en la vista previa (la siguiente pelea sale en ella). */
    private VistaDeArena.Escenario proxima;

    /** Pone el encuentro en la arena de la vista previa y elige otra para la próxima vez. */
    private Encuentro enLaProxima(Encuentro e) {
        if (proxima != null) e.escenario = proxima;
        proxima = null;
        return e;
    }

    /** La próxima arena y sus terrenos (para pensar qué piernas y armas montar) y el tramo de peso del robot que pelea. */
    private float vistaPreviaDeArena(Juego j, float y) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (proxima == null) proxima = escenarioAlAzar();
        float x = 40, w = p.ancho - 80, bw = Math.min(260, Math.max(170, p.ancho(T.t("pelear.otra_arena"), ui.letra * 0.85f, false) + 30));
        float tw = w - bw - 48, tam = ui.letra * 0.82f;
        String arena = T.f("pelear.proxima_arena", T.t("arena." + proxima.name().toLowerCase(java.util.Locale.ROOT)), PanelDePeso.arena(proxima.name()));
        Montaje m = j.jugador.robot() == null ? null : j.jugador.montajeDe(j.jugador.robot());
        String peso = m == null ? "" : T.f("pelear.tu_peso", PanelDePeso.tramo(m.tramo()), Math.round(m.proporcionDeCarga() * 100),
                PanelDePeso.terrenos(m.cifras().rasgos)) + (m.puedePelear() ? "" : " " + T.t("peso.no_puede_pelear"));
        float h = Math.max(56, p.altoDeParrafo(arena, tw, tam) + (peso.isEmpty() ? 0 : p.altoDeParrafo(peso, tw, tam) + 4) + 20);
        p.caja(x, y, w, h, 12, ui.tema.panel, 1);
        float cy = y + 10;
        cy += p.parrafo(arena, x + 16, cy, tw, tam, ui.tema.texto, 1) + 4;
        if (!peso.isEmpty()) p.parrafo(peso, x + 16, cy, tw, tam, PanelDePeso.color(m.tramo()), 1);
        if (ui.boton(T.t("pelear.otra_arena"), x + w - bw - 14, y + (h - 44) / 2, bw, 44, Ui.Estilo.PLANO, true, T.t("pelear.otra_arena_ayuda"))) {
            VistaDeArena.Escenario antes = proxima;
            for (int k = 0; k < 8 && proxima == antes; k++) proxima = escenarioAlAzar();
        }
        return y + h + 14;
    }

    /** Lo que mide de alto la tarjeta de un modo, con la letra de ahora. */
    private float altoDeTarjeta(Juego j, String[] modo, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = 18 + p.altoDeParrafo(T.t(modo[1]), w - 40, ui.letra * 1.3f, true) + 6
                + p.altoDeParrafo(T.t(modo[1] + "_ayuda"), w - 40, ui.letra * 0.85f) + 14;
        if (modo[0].equals("EQUIPOS") || modo[0].equals("ROYALE")) h += 80;
        if (modo[0].equals("ADAPTADA") && !j.jugador.modoIA.equals("ADAPTADA")) h += ui.letra * 1.4f;
        if (modo[0].equals("DESAFIO") && !Progreso.puedeDesafiar(j.jugador)) {
            h += p.altoDeParrafo(T.f("pelear.desafio_espera", Progreso.PELEAS_ENTRE_DESAFIOS - j.jugador.peleasDesdeElDesafio), w - 40,
                    ui.letra * 0.8f) + 6;
        }
        return Math.max(200, h + 50 + 16);
    }

    /** La tarjeta de un modo: su nombre, qué es, lo suyo (el compañero, la espera del desafío) y «¡Vamos!». */
    private void tarjeta(Juego j, String[] modo, boolean principal, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        boolean puede = !modo[0].equals("DESAFIO") || Progreso.puedeDesafiar(jug);
        p.caja(x, y, w, h, 14, ui.tema.panel, 1);
        float cy = y + 18;
        cy += p.parrafo(T.t(modo[1]), x + 20, cy, w - 40, ui.letra * 1.3f, true, puede ? ui.tema.texto : ui.tema.textoApagado, 1) + 6;
        cy += p.parrafo(T.t(modo[1] + "_ayuda"), x + 20, cy, w - 40, ui.letra * 0.85f, ui.tema.textoSuave, 1) + 14;
        if (modo[0].equals("EQUIPOS")) {
            String nombre = companero < 0 ? T.t("pelear.companero_ia") : companeros.get(companero)[0];
            if (ui.boton(T.f("pelear.companero", nombre), x + 20, cy, w - 40, 70, Ui.Estilo.PLANO, true, T.t("pelear.companero_ayuda"))) {
                companero = companero + 1 >= companeros.size() ? -1 : companero + 1;
            }
            cy += 80;
        }
        if (modo[0].equals("ROYALE")) {
            if (ui.boton(T.f("pelear.cerebros", EstiloPorIA.nombre(cerebrosRoyale)), x + 20, cy, w - 40, 70, Ui.Estilo.PLANO, true,
                    T.t("pelear.cerebros_ayuda"))) {
                cerebrosRoyale = EstiloPorIA.siguiente(cerebrosRoyale);
            }
            cy += 80;
        }
        if (modo[0].equals("ADAPTADA") && !jug.modoIA.equals("ADAPTADA")) {
            p.texto(T.f("pelear.dificultad_fija", jug.modoIA), x + 20, cy, ui.letra * 0.8f, false, ui.tema.aviso, 1);
        }
        if (!puede) {
            p.parrafo(T.f("pelear.desafio_espera", Progreso.PELEAS_ENTRE_DESAFIOS - jug.peleasDesdeElDesafio), x + 20, cy, w - 40,
                    ui.letra * 0.8f, ui.tema.aviso, 1);
        }
        if (ui.boton(T.t("pelear.vamos"), x + 20, y + h - 66, w - 40, 50, principal ? Ui.Estilo.PRINCIPAL : Ui.Estilo.NORMAL, puede, null)) {
            String m = modo[0].equals("ADAPTADA") ? jug.modoIA : modo[0];
            if (m.equals("MCM")) j.ir(new MaquinaContraMaquina());
            else if (m.equals("CAMPANAS")) j.ir(new Campanas());
            else if (m.equals("ROYALE")) {
                Encuentro e = royale(j, ajenos);
                e.cerebros = cerebrosRoyale;
                j.ir(new PantallaPelea(enLaProxima(e)));
            }
            else if (m.equals("EQUIPOS")) j.ir(new PantallaPelea(enLaProxima(equipos(j, companero < 0 ? null : companeros.get(companero)))));
            else j.ir(new PantallaPelea(enLaProxima(encuentro(j, m))));
        }
    }

    /** Battle royale: todos contra todos. Tú, robots de la computadora con piezas al azar y los robots ajenos que haya (hasta 6 en total). */
    public static Encuentro royale(Juego j, List<Path> ajenos) {
        return royale(j.jugador, ajenos, new Random());
    }

    /** Battle royale para un jugador con un azar dado (usable también en pruebas sin ventana). */
    public static Encuentro royale(Jugador jug, List<Path> ajenos, Random azar) {
        Random r = azar == null ? new Random() : azar;
        Encuentro e = new Encuentro();
        e.modo = "ROYALE";
        e.nivelRival = Progreso.nivelParaLaPelea(jug, r);
        e.escenario = escenarioAlAzar();
        Set<String> usados = new HashSet<>();
        if (jug != null && jug.nombre != null) usados.add(jug.nombre);
        e.rival = Rivales.alAzar(e.nivelRival, jug.agresividad, r, usados);
        int equipo = 2;
        if (ajenos != null) {
            for (Path a : ajenos) {
                if (e.demas.size() >= 2) break;
                Combatiente c = cargarAjeno(a);
                if (c == null) continue;
                usados.add(c.nombre);
                e.demas.add(c);
                e.equiposDeLosDemas.add(equipo++);
            }
        }
        while (e.demas.size() < 3) {
            e.demas.add(Rivales.alAzar(e.nivelRival, jug.agresividad, r, usados));
            e.equiposDeLosDemas.add(equipo++);
        }
        if (e.rondas == Pelea.RONDAS) e.rondas = jug.rondas;
        return e;
    }

    /** Por equipos: tú y tu compañero (la computadora, un robot ajeno o el robot de otra partida) contra dos de la computadora. */
    public static Encuentro equipos(Juego j, String[] companero) {
        return equipos(j.jugador, companero, new Random());
    }

    /** Pelea por equipos para un jugador con un azar dado (usable también en pruebas sin ventana). */
    public static Encuentro equipos(Jugador jug, String[] companero, Random azar) {
        Random r = azar == null ? new Random() : azar;
        Encuentro e = new Encuentro();
        e.modo = "EQUIPOS";
        e.nivelRival = Progreso.nivelParaLaPelea(jug, r);
        e.escenario = escenarioAlAzar();
        List<Modelo> ms = Modelo.todos();
        Set<String> usados = new HashSet<>();
        if (jug != null && jug.nombre != null) usados.add(jug.nombre);
        e.rival = Rivales.crear(ms.get(r.nextInt(ms.size())), e.nivelRival, jug.agresividad, r, false, usados);
        Combatiente amigo = null;
        if (companero != null && companero[1].equals("ajeno")) amigo = cargarAjeno(Paths.get(companero[2]));
        if (companero != null && companero[1].equals("partida")) amigo = robotDeOtraPartida(companero[2]);
        if (amigo == null) {
            amigo = Rivales.crear(ms.get(r.nextInt(ms.size())), Math.max(0.3, e.nivelRival), 0.2, r);
            amigo = new Combatiente(T.t("pelear.companero_ia") + " (" + amigo.montaje.modelo.nombre(T.idioma) + ")", amigo.montaje, amigo.fabrica);
        }
        amigo.computadora = false;
        e.demas.add(amigo);
        e.equiposDeLosDemas.add(0);
        e.demas.add(Rivales.crear(ms.get(r.nextInt(ms.size())), e.nivelRival, jug.agresividad, r, false, usados));
        e.equiposDeLosDemas.add(1);
        if (e.rondas == Pelea.RONDAS) e.rondas = jug.rondas;
        return e;
    }

    static Combatiente cargarAjeno(Path archivo) {
        try {
            if (!Compilador.hayCompilador()) return null;
            String codigo = Files.readString(archivo, StandardCharsets.UTF_8);
            RobotDelJugador r = new RobotDelJugador();
            if (!r.compilar(codigo, Locale.ROOT).bien) return null;
            Combatiente c = new Combatiente(archivo.getFileName().toString().replace(".java", ""), Exportar.montajeDe(codigo), r);
            c.computadora = true;
            return c;
        } catch (Exception ex) {
            return null;
        }
    }

    /** El robot elegido de otra partida de este ordenador (otro jugador): su código y sus piezas. */
    static Combatiente robotDeOtraPartida(String carpeta) {
        try {
            metajavbots.progreso.Jugador otro = metajavbots.progreso.Guardado.leer(carpeta);
            metajavbots.progreso.Jugador.Robot r = otro.robot();
            if (r == null || !Compilador.hayCompilador()) return null;
            RobotDelJugador f = new RobotDelJugador();
            f.cargarDesdeJugador(r, Locale.ROOT, false);
            if (!f.listo()) return null;
            return new Combatiente(r.nombre + " (" + otro.nombre + ")", r.montaje(), f);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Un robot ajeno: su archivo .java lleva en un comentario el modelo y las piezas (lo pone «Exportar» del taller). */
    private void pelearContraAjeno(Juego j, Path archivo) {
        try {
            String codigo = Files.readString(archivo, StandardCharsets.UTF_8);
            if (!Compilador.hayCompilador()) {
                aviso = T.t("taller.sin_jdk");
                return;
            }
            RobotDelJugador r = new RobotDelJugador();
            Compilador.Resultado res = r.compilar(codigo, Locale.ROOT);
            if (!res.bien) {
                aviso = T.f("pelear.ajeno_no_compila", res.graves().isEmpty() ? "" : res.graves().get(0).mensaje);
                return;
            }
            Montaje m = Exportar.montajeDe(codigo);
            Combatiente c = new Combatiente(archivo.getFileName().toString().replace(".java", ""), m, r);
            c.computadora = true;
            Encuentro e = new Encuentro();
            e.rival = c;
            e.modo = "AJENO";
            e.nivelRival = 0.5;
            e.escenario = escenarioAlAzar();
            j.ir(new PantallaPelea(e));
        } catch (Exception ex) {
            aviso = ex.toString();
        }
    }
}
