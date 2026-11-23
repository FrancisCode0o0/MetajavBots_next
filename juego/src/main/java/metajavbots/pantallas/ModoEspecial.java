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
    public static final List<String> CLAVES_PESTANAS = List.of("especial.jefes", "especial.tope", "especial.senior", "especial.retos",
            "especial.otros", "especial.destruccion");

    private int pestana;
    private boolean bajarAlFinal;

    public ModoEspecial() {
        this(0);
    }

    /** Abierto en esa pestaña (0 jefes, 1 tope de cálculo, 2 retos de programar, 3 robots de otros, 4 destrucción y supervivencia). */
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

    /** En la pestaña de las peleas Senior, su propio «¿Cómo se juega?»; en las demás, el del modo especial. */
    @Override
    public String comoSeJuega() {
        return pestana == 2 ? "senior" : "especial";
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.especial"), true);
        String[] pest = {T.t("especial.jefes"), T.t("especial.tope"), T.t("especial.senior"), T.t("especial.retos"),
                T.t("especial.otros"), T.t("especial.destruccion")};
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
                yy = PanelSenior.dibujar(j, x, yy, w);
                break;
            case 3:
                yy = retos(j, x, yy, w);
                break;
            case 4:
                yy += p.parrafo(T.t("especial.otros_ayuda"), x + 24, yy, w - 48, ui.letra * 0.95f, ui.tema.texto, 1) + 24;
                float bwOtros = (w - 48 - 32) / 3f;
                if (ui.boton(T.t("menu.taller"), x + 24, yy, bwOtros, 58, Ui.Estilo.NORMAL)) j.ir(new Taller());
                if (ui.boton(T.t("pelear.ajenos"), x + 24 + bwOtros + 16, yy, bwOtros, 58, Ui.Estilo.NORMAL)) j.ir(new PrepararPelea());
                if (ui.boton(T.t("mcm.titulo"), x + 24 + 2 * (bwOtros + 16), yy, bwOtros, 58, Ui.Estilo.NORMAL, true, T.t("mcm.ayuda"))) {
                    j.ir(new MaquinaContraMaquina());
                }
                yy += 82;
                break;
            default:
                yy += p.parrafo(T.t("especial.destruccion_ayuda"), x + 24, yy, w - 48, ui.letra * 0.95f, ui.tema.texto, 1) + 24;
                String txtDestruccion = T.t("especial.destruccion_entrar");
                float btDestruccion = Math.min(w - 48, Math.max(360, p.ancho(txtDestruccion, ui.letra, true) + 48));
                if (ui.boton(txtDestruccion, x + 24, yy, btDestruccion, 58, Ui.Estilo.PRINCIPAL)) {
                    j.ir(new PantallaMinijuegoDestruccion());
                }
                yy += 82;
                break;
        }
        ui.finDesplazable(despl, yy + d - cy);
    }

    /** Los retos de programar que desbloquean piezas especiales. */
    private float retos(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        yy += p.parrafo(T.t("especial.retos_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 18;
        for (Ejercicio e : Banco.delJuego().todos()) {
            if (e.desbloquea.isEmpty()) continue;
            boolean hecho = jug.ejerciciosResueltos.contains(e.id);
            int acento = hecho ? ui.tema.bien : ui.tema.acento;
            p.caja(x + 20, yy, w - 40, 72, 12, ui.tema.fondo, 0.92f);
            p.bordeRedondeado(x + 20, yy, w - 40, 72, 12, acento, hecho ? 0.65f : 0.45f, 1.3f);
            p.caja(x + 20, yy + 10, 5, 52, 2, acento, 0.9f);
            String titReto = (hecho ? "✔ " : "") + e.titulo(T.idioma);
            float tReto = ui.cabeEntero(titReto, w - 380, ui.letra * 1.05f, true);
            p.textoConBordeNegro(titReto, x + 38, yy + (72 - tReto * 1.2f) / 2, tReto, true,
                    hecho ? ui.tema.bien : ui.tema.texto, 1);
            if (ui.boton(T.t("especial.abrir"), x + w - 320, yy + 9, 280, 54, hecho ? Ui.Estilo.PLANO
                    : Ui.Estilo.PRINCIPAL)) j.ir(new PantallaEjercicio(e));
            yy += 84;
        }
        return yy;
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
                if (gimnasios || jefes) yy += 8;
                p.textoConBordeNegro(T.t(jefe.gimnasio ? "especial.gimnasios" : "especial.jefes_titulo"), x + 24, yy, ui.letra * 1.25f, true, 0xFFC857, 1);
                yy += ui.letra * 1.9f;
                if (jefe.gimnasio) gimnasios = true;
                else jefes = true;
            }
            boolean vencido = jug.jefesVencidos.contains(jefe.id);
            float hPista = p.altoDeParrafo(jefe.pista.en(T.idioma), w - 400, ui.letra * 0.82f);
            float altoTarjeta = Math.max(96, 44 + hPista + 18);
            int colorBorde = vencido ? ui.tema.bien : jefe.color;
            p.caja(x + 20, yy, w - 40, altoTarjeta, 12, ui.tema.fondo, 0.92f);
            p.bordeRedondeado(x + 20, yy, w - 40, altoTarjeta, 12, colorBorde, vencido ? 0.7f : 0.45f, 1.3f);
            p.caja(x + 20, yy + 10, 5, altoTarjeta - 20, 2, colorBorde, 0.92f);
            String titJefe = (vencido ? "✔ " : "") + jefe.nombre(T.idioma);
            float tJefe = ui.cabeEntero(titJefe, w - 400, ui.letra * 1.06f, true);
            p.textoConBordeNegro(titJefe, x + 38, yy + 12, tJefe, true, vencido ? ui.tema.bien : ui.tema.texto, 1);
            p.parrafo(jefe.pista.en(T.idioma), x + 38, yy + 18 + tJefe * 1.25f, w - 400, ui.letra * 0.82f, ui.tema.textoSuave, 1);
            if (ui.boton(T.t("especial.luchar"), x + w - 320, yy + (altoTarjeta - 54) / 2, 280, 54,
                    vencido ? Ui.Estilo.PLANO : Ui.Estilo.PRINCIPAL)) luchar(j, jefe);
            yy += altoTarjeta + 12;
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
        p.textoConBordeNegro(T.f("especial.medallas", ganadas, n), x + 24, yy, ui.letra * 1.1f, true, ui.tema.texto, 1);
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
        e.musica = "jefe";
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
