package metajavbots.pantallas;

import java.util.List;
import java.util.Random;

import metajavbots.Juego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * MODO ESPECIAL — Jefes que solo se vencen usando un tema de Java; peleas con
 * tope de cálculo estricto (el código ineficiente hace lento al robot); los
 * retos de programar que dan piezas; y los robots de otros jugadores. La
 * depuración (robots rotos y depurador) y el laboratorio de pruebas tienen su
 * propia sección en {@link DepuracionYTesting} (docs/MODO_ESPECIAL.md).
 */
public final class ModoEspecial implements Pantalla {

    /** Claves de las pestañas de esta pantalla (en orden). */
    public static final List<String> CLAVES_PESTANAS = List.of("especial.jefes", "especial.tope", "especial.retos", "especial.otros");

    private int pestana;
    private boolean bajarAlFinal;

    public ModoEspecial() {
        this(0);
    }

    /** Abierto en esa pestaña (0 jefes, 1 tope de cálculo, 2 retos de programar, 3 robots de otros). */
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
        String[] pest = {T.t("especial.jefes"), T.t("especial.tope"), T.t("especial.retos"), T.t("especial.otros")};
        int anterior = pestana;
        pestana = ui.pestanas(pest, pestana, 20, arriba + 16, p.ancho - 40, 50);
        if (pestana != anterior) despl.alPrincipio();
        float x = 20, y = arriba + 80, w = p.ancho - 40, h = p.alto - y - 20;
        float cy = ui.panel(x, y, w, h, null);
        if (bajarAlFinal && despl.contenido > 0) {
            despl.desplazamiento = despl.contenido;
            bajarAlFinal = false;
        }
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
                break;
            case 2:
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
        yy = vitrina(j, x, yy, w);
        yy += p.parrafo(T.t("especial.jefes_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
        boolean gimnasios = false, jefes = false;
        for (Jefe jefe : Jefe.TODOS) {
            if (jefe.gimnasio ? !gimnasios : !jefes) {
                p.texto(T.t(jefe.gimnasio ? "especial.gimnasios" : "especial.jefes_titulo"), x + 24, yy, ui.letra * 1.3f, true, 0xFFC857, 1);
                yy += ui.letra * 2;
                if (jefe.gimnasio) gimnasios = true;
                else jefes = true;
            }
            boolean vencido = jug.jefesVencidos.contains(jefe.id);
            p.texto((vencido ? "✔ " : "") + jefe.nombre(T.idioma), x + 24, yy + 4, ui.letra * 1.1f, true, vencido ? ui.tema.bien : ui.tema.texto, 1);
            float pista = p.parrafo(jefe.pista.en(T.idioma), x + 24, yy + 36, w - 380, ui.letra * 0.8f, ui.tema.textoSuave, 1);
            if (ui.boton(T.t("especial.luchar"), x + w - 320, yy + 10, 280, 54, vencido ? Ui.Estilo.PLANO : Ui.Estilo.PRINCIPAL)) luchar(j, jefe);
            yy += Math.max(110, 36 + pista + 24);
        }
        return yy;
    }

    /** La vitrina: una medalla por cada gimnasio y cada jefe; brillan las ganadas. Encima de una, su nombre. */
    private float vitrina(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        int n = Jefe.TODOS.size(), ganadas = 0;
        for (Jefe jefe : Jefe.TODOS) if (jug.jefesVencidos.contains(jefe.id)) ganadas++;
        p.texto(T.f("especial.medallas", ganadas, n), x + 24, yy, ui.letra * 1.1f, true, ui.tema.texto, 1);
        yy += ui.letra * 1.8f;
        float paso = Math.min(110, (w - 48) / n), r = Math.min(34, paso * 0.4f);
        for (int i = 0; i < n; i++) {
            Jefe jefe = Jefe.TODOS.get(i);
            float cx = x + 24 + paso * (i + 0.5f), cy = yy + 22 + r;
            medalla(p, ui, cx, cy, r, jefe, jug.jefesVencidos.contains(jefe.id));
            if (ui.e.dentro(cx - r, cy - r - 22, 2 * r, 2 * r + 22)) ui.avisar(jefe.nombre(T.idioma));
        }
        return yy + 2 * r + 46;
    }

    /** Una medalla: su cinta, el disco con su borde y el símbolo del tema; sin ganar, apagada. */
    static void medalla(Pincel p, Ui ui, float cx, float cy, float r, Jefe jefe, boolean ganada) {
        float a = ganada ? 1 : 0.28f;
        int c = ganada ? jefe.color : 0x8A92A3;
        p.linea(cx - r * 0.5f, cy - r * 1.6f, cx - r * 0.15f, cy - r * 0.6f, r * 0.38f, Pincel.oscurecer(c, 0.35f), a);
        p.linea(cx + r * 0.5f, cy - r * 1.6f, cx + r * 0.15f, cy - r * 0.6f, r * 0.38f, Pincel.oscurecer(c, 0.2f), a);
        p.circulo(cx, cy, r, Pincel.oscurecer(c, 0.45f), a);
        p.circulo(cx, cy, r * 0.84f, c, a);
        p.aro(cx, cy, r * 0.68f, Math.max(1.5f, r * 0.06f), Pincel.oscurecer(c, 0.4f), a);
        float tl = ui.cabe(jefe.simbolo, r * 1.2f, r * 0.62f, true);
        p.textoCentrado(jefe.simbolo, cx, cy - tl * 0.62f, tl, true, ganada ? 0x1C1F26 : 0x5A5F66, a);
        if (ganada) p.circulo(cx - r * 0.6f, cy - r * 0.5f, r * 0.1f, 0xFFFFFF, 0.55f);
    }

    /** La pelea contra un jefe: su modelo, su nivel y su nombre; si se le gana, queda vencido. */
    private static void luchar(Juego j, Jefe jefe) {
        Jugador jug = j.jugador;
        Encuentro e = new Encuentro();
        e.jefe = jefe;
        e.modo = "JEFE";
        e.rondas = jug.rondas;
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
