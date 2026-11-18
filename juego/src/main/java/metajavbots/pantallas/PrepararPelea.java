package metajavbots.pantallas;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

import metajavbots.Juego;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
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
            e.rival = anterior.modo.equals("AJENO") ? anterior.rival : e.rival;
            e.modo = anterior.modo;
            e.nivelRival = anterior.nivelRival;
            e.jefe = anterior.jefe;
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
        Jugador jug = j.jugador;
        Random azar = new Random();
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
        return e;
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

    /** Lo que mide de alto la tarjeta de un modo, con la letra de ahora. */
    private float altoDeTarjeta(Juego j, String[] modo, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        float h = 18 + p.altoDeParrafo(T.t(modo[1]), w - 40, ui.letra * 1.3f, true) + 6
                + p.altoDeParrafo(T.t(modo[1] + "_ayuda"), w - 40, ui.letra * 0.85f) + 14;
        if (modo[0].equals("EQUIPOS") || modo[0].equals("ROYALE")) h += 54;
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
            if (ui.boton(T.f("pelear.companero", nombre), x + 20, cy, w - 40, 44, Ui.Estilo.PLANO, true, T.t("pelear.companero_ayuda"))) {
                companero = companero + 1 >= companeros.size() ? -1 : companero + 1;
            }
            cy += 54;
        }
        if (modo[0].equals("ROYALE")) {
            if (ui.boton(T.f("pelear.cerebros", EstiloPorIA.nombre(cerebrosRoyale)), x + 20, cy, w - 40, 44, Ui.Estilo.PLANO, true,
                    T.t("pelear.cerebros_ayuda"))) {
                cerebrosRoyale = EstiloPorIA.siguiente(cerebrosRoyale);
            }
            cy += 54;
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
                j.ir(new PantallaPelea(e));
            }
            else if (m.equals("EQUIPOS")) j.ir(new PantallaPelea(equipos(j, companero < 0 ? null : companeros.get(companero))));
            else j.ir(new PantallaPelea(encuentro(j, m)));
        }
    }

    /** Battle royale: todos contra todos. Tú, robots de la computadora con piezas al azar y los robots ajenos que haya (hasta 6 en total). */
    public static Encuentro royale(Juego j, List<Path> ajenos) {
        Random azar = new Random();
        Encuentro e = new Encuentro();
        e.modo = "ROYALE";
        e.nivelRival = Progreso.nivelParaLaPelea(j.jugador, azar);
        e.escenario = escenarioAlAzar();
        e.rival = Rivales.alAzar(e.nivelRival, j.jugador.agresividad, azar);
        int equipo = 2;
        for (Path a : ajenos) {
            if (e.demas.size() >= 2) break;
            Combatiente c = cargarAjeno(a);
            if (c == null) continue;
            e.demas.add(c);
            e.equiposDeLosDemas.add(equipo++);
        }
        while (e.demas.size() < 3) {
            e.demas.add(Rivales.alAzar(e.nivelRival, j.jugador.agresividad, azar));
            e.equiposDeLosDemas.add(equipo++);
        }
        return e;
    }

    /** Por equipos: tú y tu compañero (la computadora, un robot ajeno o el robot de otra partida) contra dos de la computadora. */
    public static Encuentro equipos(Juego j, String[] companero) {
        Random azar = new Random();
        Encuentro e = new Encuentro();
        e.modo = "EQUIPOS";
        e.nivelRival = Progreso.nivelParaLaPelea(j.jugador, azar);
        e.escenario = escenarioAlAzar();
        List<Modelo> ms = Modelo.todos();
        e.rival = Rivales.crear(ms.get(azar.nextInt(ms.size())), e.nivelRival, j.jugador.agresividad, azar);
        Combatiente amigo = null;
        if (companero != null && companero[1].equals("ajeno")) amigo = cargarAjeno(Paths.get(companero[2]));
        if (companero != null && companero[1].equals("partida")) amigo = robotDeOtraPartida(companero[2]);
        if (amigo == null) {
            amigo = Rivales.crear(ms.get(azar.nextInt(ms.size())), Math.max(0.3, e.nivelRival), 0.2, azar);
            amigo = new Combatiente(T.t("pelear.companero_ia") + " (" + amigo.montaje.modelo.nombre(T.idioma) + ")", amigo.montaje, amigo.fabrica);
        }
        amigo.computadora = false;
        e.demas.add(amigo);
        e.equiposDeLosDemas.add(0);
        e.demas.add(Rivales.crear(ms.get(azar.nextInt(ms.size())), e.nivelRival, j.jugador.agresividad, azar));
        e.equiposDeLosDemas.add(1);
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
            if (!f.compilar(r.codigo, Locale.ROOT).bien) return null;
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
