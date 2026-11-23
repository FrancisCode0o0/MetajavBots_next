package metajavbots.pantallas;

import com.badlogic.gdx.Gdx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import metajavbots.Juego;
import metajavbots.ejercicios.Laboratorio;
import metajavbots.ejercicios.RobotRoto;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * DEPURACIÓN Y TESTING — Sección dedicada a encontrar y corregir fallos
 * (depurador en vivo y robots rotos) y a escribir pruebas unitarias en el
 * laboratorio (caja negra, caja blanca y mutantes).
 */
public final class DepuracionYTesting implements Pantalla {

    /** Claves de las pestañas de esta pantalla (en orden). */
    public static final List<String> CLAVES_PESTANAS = List.of("especial.rotos", "especial.laboratorio");

    private int pestana;
    private final Set<String> pistaAbierta = new HashSet<>();
    private final Map<String, RobotEnReparacion> reparaciones = new HashMap<>();
    private final Map<String, Montaje> montajes = new HashMap<>();
    private VistaDeArena vista;
    private boolean bajarAlFinal;
    private final Ui.Desplazable despl = new Ui.Desplazable();

    public DepuracionYTesting() {
        this(0);
    }

    /** Abierto en esa pestaña (0 robots rotos y depurador, 1 laboratorio de pruebas). */
    public DepuracionYTesting(int pestana) {
        this.pestana = pestana;
    }

    /** Abierto en esa pestaña y bajado del todo (para las capturas). */
    public static DepuracionYTesting alFinal(int pestana) {
        DepuracionYTesting d = new DepuracionYTesting(pestana);
        d.bajarAlFinal = true;
        return d;
    }

    @Override public String comoSeJuega() { return "depuracion"; }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.depuracion"), true);
        String[] pest = {T.t("especial.rotos"), T.t("especial.laboratorio")};
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
                yy = depuradorYRotos(j, x, yy, w);
                break;
            default:
                yy = laboratorio(j, jug, x, yy, w);
                break;
        }
        ui.finDesplazable(despl, yy + d - cy);
    }

    /** Depurador en vivo, errores explicados y lista de robots rotos para reparar. */
    private float depuradorYRotos(Juego j, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        yy += p.parrafo(T.t("especial.depurador_ayuda"), x + 24, yy, w - 48, ui.letra * 0.95f, ui.tema.texto, 1) + 16;
        if (ui.boton(T.t("especial.practicar_depurador"), x + 24, yy, 420, 56, Ui.Estilo.NORMAL)) {
            j.ir(new PantallaPelea(PrepararPelea.encuentro(j, "PRACTICA")));
        }
        yy += 80;
        yy += p.parrafo(T.t("especial.errores_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
        return robotsRotos(j, x, yy + 10, w);
    }

    /** Los retos del laboratorio de pruebas unitarias (caja negra, caja blanca y mutantes). */
    private float laboratorio(Juego j, Jugador jug, float x, float yy, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        yy += p.parrafo(T.t("especial.laboratorio_ayuda"), x + 24, yy, w - 48, ui.letra * 0.9f, ui.tema.textoSuave, 1) + 16;
        for (Laboratorio l : Laboratorio.todos()) {
            boolean hecho = jug.laboratoriosHechos.contains(l.id);
            p.texto((hecho ? "✔ " : "") + l.titulo(T.idioma), x + 24, yy + 10, ui.letra * 1.1f, true, hecho ? ui.tema.bien : ui.tema.texto, 1);
            if (ui.boton(T.t("especial.abrir"), x + w - 320, yy, 280, 54, hecho ? Ui.Estilo.PLANO
                    : Ui.Estilo.PRINCIPAL)) j.ir(new PantallaLaboratorio(l));
            yy += 74;
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
        for (RobotRoto r : RobotRoto.todos()) {
            boolean hecho = jug.robotsArreglados.contains(r.id);
            float tx = w - 48 - bw - 20 - 130;
            RobotEnReparacion dibujo = reparaciones.computeIfAbsent(r.id, k -> new RobotEnReparacion());
            dibujo.dibujar(p, vista, montajes.computeIfAbsent(r.modelo, k -> new Montaje(Modelo.de(k))), x + 24 + 60, yy + 162, 1.15f,
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
    private static void arreglar(Juego j, RobotRoto r) {
        j.ir(new PantallaPelea(encuentroRoto(j, r)));
    }

    /** La pelea de práctica de un robot roto (con su código y su cuerpo). */
    public static Encuentro encuentroRoto(Juego j, RobotRoto r) {
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
        e.montaje = new Montaje(Modelo.de(r.modelo));
        e.objetivo = r.objetivo;
        e.alAcabar = bien -> {
            if (bien) jug.robotsArreglados.add(r.id);
        };
        e.otraVez = () -> encuentroRoto(j, r);
        return e;
    }
}
