package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.pantalla.CampoDeTexto;
import metajavbots.pantalla.Efectos;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.progreso.PruebaDeNivel;
import metajavbots.textos.T;

/**
 * BIENVENIDA — Lo primero de una partida nueva: ¿ha programado antes? Si no,
 * empieza en Noob I; si sí, la prueba de nivel le pone su etiqueta (con
 * fuegos artificiales). Luego elige su primer robot y le pone nombre.
 */
public final class Bienvenida implements Pantalla {

    private int paso;
    private int pregunta;
    private final List<Boolean> aciertos = new ArrayList<>();
    private int elegida = -1;
    private float t, tiempoRespuesta;
    private final Efectos fuegos = new Efectos();
    private VistaDeArena vista;
    private int modelo;
    private final CampoDeTexto nombre = new CampoDeTexto();

    @Override
    public void alEntrar(Juego j) {
        vista = new VistaDeArena(j.pincel);
        fuegos.alEstallar = () -> j.sonidos.tocar("estallido", 0.4f);
        if (j.jugador.pruebaHecha) paso = 3;
        nombre.maximo = 24;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float cx = p.ancho / 2;
        switch (paso) {
            case 0: {
                p.textoCentrado(T.f("bienvenida.hola", j.jugador.nombre), cx, 150, ui.letra * 2.2f, true, ui.tema.texto, 1);
                p.textoCentrado(T.t("bienvenida.pregunta"), cx, 240, ui.letra * 1.3f, false, ui.tema.textoSuave, 1);
                if (ui.boton(T.t("bienvenida.nunca"), cx - 420, 330, 400, 90, Ui.Estilo.PRINCIPAL, true, T.t("bienvenida.nunca_ayuda"))) {
                    j.jugador.etiqueta = Etiqueta.NOOB_1;
                    j.jugador.pruebaHecha = true;
                    paso = 3;
                }
                if (ui.boton(T.t("bienvenida.algo"), cx + 20, 330, 400, 90, Ui.Estilo.NORMAL, true, T.t("bienvenida.algo_ayuda"))) paso = 1;
                break;
            }
            case 1:
                prueba(j, dt);
                break;
            case 2: {
                fuegos.avanzar(dt);
                if (t % 0.5f < dt) {
                    fuegos.cohete((float) (p.ancho * (0.2 + Math.random() * 0.6)), p.alto, p.alto * 0.6f,
                            new int[]{0xFF5E5B, 0xFFD23F, 0x3FE0D0, 0x7B61FF}[(int) (Math.random() * 4)]);
                    j.sonidos.tocar("fuegos", 0.3f);
                }
                fuegos.dibujar(p);
                p.textoCentrado(T.t("bienvenida.tu_etiqueta"), cx, 180, ui.letra * 1.4f, false, ui.tema.textoSuave, 1);
                Comun.insignia(p, ui, j.jugador.etiqueta, cx - 136 * 1.5f, 240, 3f);
                p.textoCentrado(j.jugador.etiqueta.descripcion(T.idioma), cx, 420, ui.letra * 1.1f, false, ui.tema.texto, 1);
                if (ui.boton(T.t("comun.seguir"), cx - 150, 520, 300, 64, Ui.Estilo.PRINCIPAL)) paso = 3;
                break;
            }
            default:
                elegirRobot(j);
                break;
        }
    }

    private void prueba(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        List<PruebaDeNivel.Pregunta> ps = PruebaDeNivel.preguntas();
        if (pregunta >= ps.size() || debeParar(ps)) {
            j.jugador.etiqueta = PruebaDeNivel.resultado(aciertos);
            j.jugador.pruebaHecha = true;
            j.sonidos.tocar("subida", 0.8f);
            j.sacudir(6, 0.6f);
            paso = 2;
            return;
        }
        PruebaDeNivel.Pregunta q = ps.get(pregunta);
        float x = 160, w = p.ancho - 320;
        p.texto(T.f("bienvenida.pregunta_n", pregunta + 1, ps.size()), x, 60, ui.letra, false, ui.tema.textoSuave, 1);
        ui.barra(x, 92, w, 10, (float) pregunta / ps.size(), ui.tema.acento, null);
        p.texto(q.pregunta.en(T.idioma), x, 130, ui.letra * 1.4f, true, ui.tema.texto, 1);
        String[] lineas = q.codigo.split("\n");
        float ch = lineas.length * ui.letra * 1.35f + 30;
        p.caja(x, 190, w, ch, 10, ui.tema.panel, 1);
        float ly = 205;
        for (String l : lineas) {
            p.texto(l, x + 24, ly, ui.letra * 1.05f, false, ui.tema.texto, 1);
            ly += ui.letra * 1.35f;
        }
        float oy = 210 + ch;
        for (int i = 0; i < q.opciones.size(); i++) {
            float bx = x + (i % 2) * (w / 2 + 10), by = oy + (i / 2) * 84;
            Ui.Estilo estilo = elegida < 0 ? Ui.Estilo.NORMAL : i == q.bien ? Ui.Estilo.PRINCIPAL : i == elegida ? Ui.Estilo.PELIGRO : Ui.Estilo.PLANO;
            if (ui.boton(q.opciones.get(i).en(T.idioma), bx, by, w / 2 - 10, 70, estilo) && elegida < 0) {
                elegida = i;
                tiempoRespuesta = 0;
                aciertos.add(i == q.bien);
                j.sonidos.tocar(i == q.bien ? "bien" : "mal", 0.7f);
            }
        }
        if (elegida >= 0) {
            tiempoRespuesta += dt;
            if (tiempoRespuesta > 1.1f) {
                elegida = -1;
                pregunta++;
            }
        }
        if (ui.boton(T.t("bienvenida.no_se"), x + w - 260, oy + 180, 260, 50, Ui.Estilo.PLANO) && elegida < 0) {
            aciertos.add(false);
            pregunta++;
        }
    }

    /** Si ya ha fallado dos del escalón en que está, no hace falta seguir. */
    private boolean debeParar(List<PruebaDeNivel.Pregunta> ps) {
        if (pregunta == 0) return false;
        Etiqueta nivel = ps.get(pregunta - 1).nivel;
        int fallos = 0;
        for (int i = 0; i < pregunta; i++) if (ps.get(i).nivel == nivel && !aciertos.get(i)) fallos++;
        return fallos >= 2;
    }

    private void elegirRobot(Juego j) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        List<Modelo> disponibles = Progreso.modelosDisponibles(jug);
        if (modelo >= disponibles.size()) modelo = 0;
        p.textoCentrado(T.t("bienvenida.elige"), p.ancho / 2, 50, ui.letra * 1.8f, true, ui.tema.texto, 1);
        float w = Math.min(380, (p.ancho - 120) / disponibles.size());
        float x0 = p.ancho / 2 - w * disponibles.size() / 2;
        for (int i = 0; i < disponibles.size(); i++) {
            Modelo m = disponibles.get(i);
            float x = x0 + i * w;
            boolean es = i == modelo;
            p.caja(x + 8, 120, w - 16, 470, 14, es ? ui.tema.panelAlto : ui.tema.panel, 1);
            if (es) p.bordeRedondeado(x + 8, 120, w - 16, 470, 14, ui.tema.acento, 1, 3);
            vista.robotSuelto(p, new Montaje(m), x + w / 2, 440, 3.4f, t + i, es ? 0.25f : 0, true);
            p.textoCentrado(m.nombre(T.idioma), x + w / 2, 470, ui.letra * 1.4f, true, ui.tema.texto, 1);
            p.textoCentrado(m.naturaleza.nombre(T.idioma), x + w / 2, 505, ui.letra * 0.9f, false, m.naturaleza.color(), 1);
            p.parrafo(m.lema.en(T.idioma), x + 30, 540, w - 60, ui.letra * 0.8f, ui.tema.textoSuave, 1);
            if (ui.e.clic(x + 8, 120, w - 16, 470)) {
                modelo = i;
                j.sonidos.tocar("servo", 0.6f);
            }
        }
        float y = 620;
        p.texto(T.t("bienvenida.nombre_robot"), p.ancho / 2 - 300, y, ui.letra, false, ui.tema.textoSuave, 1);
        boolean intro = nombre.dibujar(ui, p.ancho / 2 - 300, y + 30, 380, 54, disponibles.get(modelo).nombre(T.idioma));
        if (ui.boton(T.t("bienvenida.empezar"), p.ancho / 2 + 100, y + 30, 200, 54, Ui.Estilo.PRINCIPAL) || intro) {
            Modelo m = disponibles.get(modelo);
            String n = nombre.texto().isBlank() ? m.nombre(T.idioma) : nombre.texto().strip();
            Jugador.Robot r = new Jugador.Robot(n, m.id);
            r.codigo = Comun.plantilla(T.idioma);
            jug.robots.add(r);
            jug.robotElegido = jug.robots.size() - 1;
            jug.idioma = T.idioma;
            j.guardar();
            j.sonidos.tocar("subida", 0.6f);
            j.irSolo(new Menu());
        }
    }
}
