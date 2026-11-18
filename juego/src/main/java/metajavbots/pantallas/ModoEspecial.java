package metajavbots.pantallas;

import java.util.Random;

import metajavbots.Juego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Laboratorio;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
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
                    if (ui.boton(T.f("especial.tope_boton", tope), x + 24, yy, 420, 56, Ui.Estilo.NORMAL)) {
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
