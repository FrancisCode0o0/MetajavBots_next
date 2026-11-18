package metajavbots.pantallas;

import com.badlogic.gdx.Gdx;

import java.util.Random;

import metajavbots.Juego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Laboratorio;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * MODO ESPECIAL — Todas las ideas de Fran: jefes que solo se vencen usando un
 * tema de Java; peleas con tope de cálculo estricto (el código ineficiente
 * hace lento al robot); el depurador en vivo; el laboratorio de pruebas
 * (piezas por escribir pruebas JUnit); los retos de programar que dan piezas;
 * y los robots de otros jugadores. Los errores explicados según el nivel y la
 * revisión del código por la IA están en todas partes (docs/MODO_ESPECIAL.md).
 */
public final class ModoEspecial implements Pantalla {

    private int pestana;
    private final java.util.Set<String> pistaAbierta = new java.util.HashSet<>();
    private final java.util.Map<String, RobotEnReparacion> reparaciones = new java.util.HashMap<>();
    private final java.util.Map<String, metajavbots.piezas.Montaje> montajes = new java.util.HashMap<>();
    private VistaDeArena vista;
    private boolean bajarAlFinal;

    public ModoEspecial() {
        this(0);
    }

    /** Abierto en esa pestaña (0 jefes, 1 tope, depurador y robots rotos…). */
    public ModoEspecial(int pestana) {
        this.pestana = pestana;
    }

    /** Abierto en esa pestaña y bajado del todo (para las capturas). */
    public static ModoEspecial alFinal(int pestana) {
        ModoEspecial m = new ModoEspecial(pestana);
        m.bajarAlFinal = true;
        return m;
    }
    private final Ui.Desplazable despl = new Ui.Desplazable();

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.especial"), true);
        String[] pest = {T.t("especial.jefes"), T.t("especial.tope"), T.t("especial.laboratorio"), T.t("especial.retos"), T.t("especial.otros")};
        pestana = ui.pestanas(pest, pestana, 20, arriba + 16, p.ancho - 40, 50);
        float x = 20, y = arriba + 80, w = p.ancho - 40, h = p.alto - y - 20;
        float cy = ui.panel(x, y, w, h, null);
        if (bajarAlFinal) despl.desplazamiento = despl.contenido;
        float d = ui.desplazable(despl, x + 8, cy, w - 16, y + h - cy - 10);
        float yy = cy - d;
        switch (pestana) {
            case 0:
                yy = jefes(j, x, yy, w);
                break;
            case 1:
                yy += p.parrafo(T.t("especial.tope_ayuda"), x + 24, yy, w - 48, ui.letra * 0.95f, ui.tema.texto, 1) + 24;
                long[] topes = {150, 400, 1000};
                for (long tope : topes) {
                    String texto = T.f("especial.tope_boton", tope);
                    float bt = Math.min(w - 48, Math.max(420, p.ancho(texto, ui.letra, true) + 48));
                    if (ui.boton(texto, x + 24, yy, bt, 56, Ui.Estilo.NORMAL)) {
                        Encuentro e = PrepararPelea.encuentro(j, "ADAPTADA");
                        e.topeDePasos = tope;
                        e.modo = "TOPE";
                        j.ir(new PantallaPelea(e));
                    }
                    yy += 70;
                }
                yy += 20;
                yy += p.parrafo(T.t("especial.depurador_ayuda"), x + 24, yy, w - 48, ui.letra * 0.95f, ui.tema.texto, 1) + 16;
                if (ui.boton(T.t("especial.practicar_depurador"), x + 24, yy, 420, 56, Ui.Estilo.NORMAL)) j.ir(new PantallaPelea(PrepararPelea.encuentro(j,
                        "PRACTICA")));
                yy += 80;
                yy += p.parrafo(T.t("especial.errores_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
                yy = robotsRotos(j, x, yy + 10, w);
                break;
            case 2:
                yy += p.parrafo(T.t("especial.laboratorio_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
                for (Laboratorio l : Laboratorio.todos()) {
                    boolean hecho = jug.laboratoriosHechos.contains(l.id);
                    p.texto((hecho ? "✔ " : "") + l.titulo(T.idioma), x + 24, yy + 10, ui.letra * 1.1f, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
                    if (ui.boton(T.t("especial.abrir"), x + w - 320, yy, 280, 54, hecho ? Ui.Estilo.PLANO
                            : Ui.Estilo.PRINCIPAL)) j.ir(new PantallaLaboratorio(l));
                    yy += 74;
                }
                break;
            case 3:
                yy += p.parrafo(T.t("especial.retos_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
                for (Ejercicio e : Banco.delJuego().todos()) {
                    if (e.desbloquea.isEmpty()) continue;
                    boolean hecho = jug.ejerciciosResueltos.contains(e.id);
                    p.texto((hecho ? "✔ " : "") + e.titulo(T.idioma), x + 24, yy + 10, ui.letra * 1.1f, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
                    if (ui.boton(T.t("especial.abrir"), x + w - 320, yy, 280, 54, hecho ? Ui.Estilo.PLANO
                            : Ui.Estilo.PRINCIPAL)) j.ir(new PantallaEjercicio(e));
                    yy += 74;
                }
                break;
            default:
                yy += p.parrafo(T.t("especial.otros_ayuda"), x + 24, yy, w - 48, ui.letra * 0.95f, ui.tema.texto, 1) + 24;
                if (ui.boton(T.t("menu.taller"), x + 24, yy, 300, 56, Ui.Estilo.NORMAL)) j.ir(new Taller());
                if (ui.boton(T.t("pelear.ajenos"), x + 340, yy, 360, 56, Ui.Estilo.NORMAL)) j.ir(new PrepararPelea());
                if (ui.boton(T.t("mcm.titulo"), x + 720, yy, 360, 56, Ui.Estilo.NORMAL, true, T.t("mcm.ayuda"))) j.ir(new MaquinaContraMaquina());
                yy += 80;
                break;
        }
        ui.finDesplazable(despl, yy + d - cy);
    }

    /** Los jefes de tema: su nombre, qué hay que usar para hacerles daño y «Luchar». Devuelve dónde acaba. */
    private float jefes(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        yy += p.parrafo(T.t("especial.jefes_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
        for (Jefe jefe : Jefe.TODOS) {
            boolean vencido = jug.jefesVencidos.contains(jefe.id);
            p.texto((vencido ? "✔ " : "") + jefe.nombre(T.idioma), x + 24, yy + 4, ui.letra * 1.1f, true, vencido ? ui.tema.bien : ui.tema.texto, 1);
            float pista = p.parrafo(jefe.pista.en(T.idioma), x + 24, yy + 36, w - 380, ui.letra * 0.8f, ui.tema.textoSuave, 1);
            if (ui.boton(T.t("especial.luchar"), x + w - 320, yy + 10, 280, 54, vencido ? Ui.Estilo.PLANO : Ui.Estilo.PRINCIPAL)) luchar(j, jefe);
            yy += Math.max(110, 36 + pista + 24);
        }
        return yy;
    }

    /** Los robots rotos: su síntoma, una pista que se abre si se pide y el botón para arreglarlo en la arena. */
    private float robotsRotos(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        p.texto(T.t("especial.rotos"), x + 24, yy, ui.letra * 1.3f, true, 0xFFC857, 1);
        yy += ui.letra * 1.8f;
        yy += p.parrafo(T.t("especial.rotos_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 14;
        float bw = 240, fx = x + 24 + 130;
        if (vista == null) vista = new VistaDeArena(p);
        for (metajavbots.ejercicios.RobotRoto r : metajavbots.ejercicios.RobotRoto.todos()) {
            boolean hecho = jug.robotsArreglados.contains(r.id);
            float tx = w - 48 - bw - 20 - 130;
            RobotEnReparacion dibujo = reparaciones.computeIfAbsent(r.id, k -> new RobotEnReparacion());
            dibujo.dibujar(p, vista, montajes.computeIfAbsent(r.modelo, k -> new metajavbots.piezas.Montaje(Modelo.de(k))), x + 24 + 60, yy + 162, 1.15f,
                    hecho ? 1 : 0.1f, Gdx.graphics == null ? 0 : Gdx.graphics.getDeltaTime());
            p.texto((hecho ? "✔ " : "") + r.titulo.en(jug.idioma), fx, yy, ui.letra * 1.1f, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
            float alto = ui.letra * 1.5f;
            alto += p.parrafo(r.sintoma.en(jug.idioma), fx, yy + alto, tx, ui.letra * 0.9f, ui.tema.texto, 1);
            if (pistaAbierta.contains(r.id)) {
                alto += 6 + p.parrafo("► " + r.pista.en(jug.idioma), fx, yy + alto + 6, tx, ui.letra * 0.85f, ui.tema.textoSuave, 1);
            }
            if (ui.boton(T.t("especial.arreglar"), x + w - 24 - bw, yy, bw, 48, hecho ? Ui.Estilo.NORMAL : Ui.Estilo.PRINCIPAL)) arreglar(j, r);
            if (!pistaAbierta.contains(r.id) && ui.boton(T.t("especial.pista"), x + w - 24 - bw, yy + 56, bw, 40, Ui.Estilo.PLANO)) {
                pistaAbierta.add(r.id);
            }
            yy += Math.max(alto, 172) + 14;
        }
        return yy;
    }

    /** Pelea de práctica con el código roto en el editor: el objetivo cuenta si lo arregla. */
    private static void arreglar(Juego j, metajavbots.ejercicios.RobotRoto r) {
        j.ir(new PantallaPelea(encuentroRoto(j, r)));
    }

    /** La pelea de práctica de un robot roto (con su código y su cuerpo). */
    public static Encuentro encuentroRoto(Juego j, metajavbots.ejercicios.RobotRoto r) {
        Jugador jug = j.jugador;
        Encuentro e = new Encuentro();
        e.modo = "ROTO";
        e.cuenta = false;
        e.rondas = 1;
        e.nivelRival = r.nivel;
        e.rival = Rivales.crear(Modelo.de(r.rival), r.nivel, 0.5, new Random());
        e.escenario = PrepararPelea.escenarioAlAzar();
        e.titulo = r.titulo.en(jug.idioma);
        e.codigo = r.codigo(jug.idioma);
        e.montaje = new metajavbots.piezas.Montaje(Modelo.de(r.modelo));
        e.objetivo = r.objetivo;
        e.alAcabar = bien -> {
            if (bien) jug.robotsArreglados.add(r.id);
        };
        e.otraVez = () -> encuentroRoto(j, r);
        return e;
    }

    /** La pelea contra un jefe: su modelo, su nivel y su nombre; si se le gana, queda vencido. */
    private static void luchar(Juego j, Jefe jefe) {
        Jugador jug = j.jugador;
        Encuentro e = new Encuentro();
        e.jefe = jefe;
        e.modo = "JEFE";
        e.nivelRival = jefe.nivel;
        e.rival = Rivales.crear(Modelo.de(jefe.modelo), jefe.nivel, Math.max(0.6, jug.agresividad), new Random());
        e.rival = new metajavbots.combate.Combatiente(jefe.nombre(T.idioma), e.rival.montaje, e.rival.fabrica);
        e.rival.computadora = true;
        e.escenario = PrepararPelea.escenarioAlAzar();
        e.alAcabar = gano -> {
            if (gano) jug.jefesVencidos.add(jefe.id);
        };
        j.ir(new PantallaPelea(e));
    }
}
