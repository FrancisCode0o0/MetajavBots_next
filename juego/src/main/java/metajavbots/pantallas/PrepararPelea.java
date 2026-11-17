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
    private final Ui.Desplazable despl = new Ui.Desplazable();

    /** La pelea normal, directa (el botón grande del menú). */
    public static Pantalla adaptada(Juego j) {
        return new PantallaPelea(encuentro(j, j.jugador.modoIA.equals("ADAPTADA") ? "ADAPTADA" : j.jugador.modoIA));
    }

    /** Otra igual que la anterior (revancha), con un rival nuevo del mismo modo. */
    public static Pantalla repetir(Juego j, Encuentro anterior) {
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

    static VistaDeArena.Escenario escenarioAlAzar() {
        VistaDeArena.Escenario[] e = VistaDeArena.Escenario.values();
        return e[new Random().nextInt(e.length)];
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

    @Override
    public void alEntrar(Juego j) {
        ajenos = robotsAjenos();
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
                {"MCM", "pelear.mcm"}};
        float w = (p.ancho - 80 - (modos.length - 1) * 20) / modos.length, h = 230;
        for (int i = 0; i < modos.length; i++) {
            float x = 40 + i * (w + 20);
            boolean puede = !modos[i][0].equals("DESAFIO") || Progreso.puedeDesafiar(jug);
            p.caja(x, y, w, h, 14, ui.tema.panel, 1);
            p.texto(T.t(modos[i][1]), x + 20, y + 18, ui.letra * 1.3f, true, puede ? ui.tema.texto : ui.tema.textoApagado, 1);
            p.parrafo(T.t(modos[i][1] + "_ayuda"), x + 20, y + 60, w - 40, ui.letra * 0.85f, ui.tema.textoSuave, 1);
            if (modos[i][0].equals("ADAPTADA") && !jug.modoIA.equals("ADAPTADA")) {
                p.texto(T.f("pelear.dificultad_fija", jug.modoIA), x + 20, y + h - 100, ui.letra * 0.8f, false, ui.tema.aviso, 1);
            }
            if (!puede) {
                p.parrafo(T.f("pelear.desafio_espera", Progreso.PELEAS_ENTRE_DESAFIOS - jug.peleasDesdeElDesafio), x + 20, y + h - 100, w - 40,
                        ui.letra * 0.8f, ui.tema.aviso, 1);
            }
            if (ui.boton(T.t("pelear.vamos"), x + 20, y + h - 66, w - 40, 50, i == 0 ? Ui.Estilo.PRINCIPAL : Ui.Estilo.NORMAL, puede, null)) {
                String modo = modos[i][0].equals("ADAPTADA") ? jug.modoIA : modos[i][0];
                if (modo.equals("MCM")) j.ir(new MaquinaContraMaquina());
                else j.ir(new PantallaPelea(encuentro(j, modo)));
            }
        }
        y += h + 30;
        // Los robots de otros jugadores.
        float pw = p.ancho - 80;
        float cy = ui.panel(40, y, pw, p.alto - y - 30, T.t("pelear.ajenos"));
        p.parrafo(T.t("pelear.ajenos_ayuda"), 60, cy, pw - 40, ui.letra * 0.85f, ui.tema.textoSuave, 1);
        cy += 70;
        float zona = p.alto - 30 - cy - 20;
        float d = ui.desplazable(despl, 50, cy, pw - 20, zona);
        float yy = cy - d;
        for (Path a : ajenos) {
            String nombre = a.getFileName().toString();
            p.texto(nombre, 70, yy + 12, ui.letra, false, ui.tema.texto, 1);
            if (ui.boton(T.t("pelear.contra_este"), 50 + pw - 300, yy, 250, 46, Ui.Estilo.NORMAL)) pelearContraAjeno(j, a);
            yy += 56;
        }
        if (ajenos.isEmpty()) p.texto(T.t("pelear.sin_ajenos"), 70, yy + 10, ui.letra * 0.9f, false, ui.tema.textoApagado, 1);
        ui.finDesplazable(despl, Math.max(56, ajenos.size() * 56));
        if (!aviso.isEmpty()) p.texto(aviso, 60, p.alto - 60, ui.letra * 0.85f, false, ui.tema.mal, 1);
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
