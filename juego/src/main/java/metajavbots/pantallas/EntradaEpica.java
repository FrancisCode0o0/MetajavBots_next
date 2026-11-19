package metajavbots.pantallas;

import com.badlogic.gdx.Input.Keys;

import metajavbots.Juego;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.dialogos.Narrador;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.T;
import metajavbots.tutor.Biografias;

/**
 * ENTRADA ÉPICA — Antes de la primera ronda, el locutor loco presenta a los
 * robots uno a uno, como en el boxeo: la arena a oscuras, dos focos que
 * barren hasta él, humo a sus pies, su nombre en grande y su biografía
 * absurda en una frase (res/biografias/robots.txt). Se salta con un clic,
 * Espacio o Esc.
 */
public final class EntradaEpica {

    private static final float POR_ROBOT = 3.2f, FINAL = 1.0f;
    private final int cuantos;
    private float t = -0.3f;
    private int anunciado = -1;

    public EntradaEpica(Pelea p) {
        cuantos = Math.min(4, p.combatientes.length);
    }

    /** Para las capturas: la presentación parada en ese momento. */
    public void en(float segundos) {
        t = segundos;
    }

    boolean activa() {
        return t < cuantos * POR_ROBOT + FINAL;
    }

    /** Un fotograma: avanza, anuncia al que toca (humo y voz) o se salta. */
    void avanzar(Juego j, Pelea p, VistaDeArena v, Narrador narrador, float dt) {
        t += dt;
        Ui ui = j.ui;
        if (ui.e.clic(0, 0, j.pincel.ancho, j.pincel.alto) || j.entrada.tecla(Keys.SPACE) || j.entrada.tecla(Keys.ENTER) || j.entrada.tecla(Keys.ESCAPE)) {
            t = cuantos * POR_ROBOT + FINAL;
            return;
        }
        int quien = (int) Math.floor(t / POR_ROBOT);
        if (t >= 0 && quien < cuantos && quien != anunciado) {
            anunciado = quien;
            Cuerpo c = p.cuerpo(quien);
            if (c == null) return;
            for (int k = 0; k < 8; k++) v.efectos.humo(v.sx(c.x, c.y) + (k - 4) * 14, v.sy(c.y) - 6, 1.2f);
            j.sonidos.tocar("campana", 0.6f);
            narrador.anunciar(T.f(quien == 0 ? "entrada.esquina_1" : quien == 1 ? "entrada.esquina_2" : "entrada.esquina_n", p.combatientes[quien].nombre)
                    + " " + biografia(p, quien));
        } else if (quien >= cuantos && anunciado < cuantos) {
            anunciado = cuantos;
            narrador.anunciar(T.t("entrada.a_pelear"));
        }
    }

    /** La primera frase de su biografía absurda. */
    private static String biografia(Pelea p, int quien) {
        String b = Biografias.deRobot(p.combatientes[quien].montaje.modelo.id, T.idioma);
        int punto = b.indexOf(". ");
        return punto > 0 ? b.substring(0, punto + 1) : b;
    }

    /** Encima de la arena: la penumbra, los focos, el nombre y la frase. */
    public void dibujar(Juego j, Pelea p, VistaDeArena v, float x, float y, float w, float h) {
        Pincel pi = j.pincel;
        Ui ui = j.ui;
        int quien = (int) Math.floor(Math.max(0, t) / POR_ROBOT);
        pi.rect(x, y, w, h, 0x000000, 0.55f);
        if (quien >= cuantos) {
            float f = (t - cuantos * POR_ROBOT) / FINAL;
            pi.textoCentrado(T.t("entrada.a_pelear"), x + w / 2, y + h * 0.35f, ui.letra * (3 + f), true, 0xFFE27A, 1 - f * 0.5f);
            return;
        }
        Cuerpo c = p.cuerpo(quien);
        if (c == null) return;
        float local = t - quien * POR_ROBOT;
        float cx = v.sx(c.x, c.y), cy = v.sy(c.y);
        // Dos focos desde arriba que barren hasta el robot.
        float barrido = Math.min(1, local / 0.6f);
        for (int lado = -1; lado <= 1; lado += 2) {
            float ox = lado < 0 ? x + 40 : x + w - 40, oy = y;
            float tx = ox + (cx - ox) * barrido;
            pi.poligono(new float[]{ox - 12, oy, ox + 12, oy, tx + 70, cy, tx - 70, cy}, 0xFFF1C1, 0.16f);
        }
        pi.brillo(cx, cy - 50, 110, 0xFFF1C1, 0.35f);
        v.robotDe(pi, p, quien);
        // El nombre, que entra deslizándose; debajo, su modelo y su biografía en una frase.
        String nombre = p.combatientes[quien].nombre.toUpperCase();
        // El texto, en la mitad contraria a la del robot: no lo tapa.
        float mitad = cx > x + w / 2 ? x + w * 0.3f : x + w * 0.7f, ancho = Math.min(w * 0.55f, 760);
        float entra = Math.min(1, local / 0.45f), tn = ui.cabe(nombre, ancho, ui.letra * 3.2f, true);
        float nx = mitad + (1 - entra) * (quien % 2 == 0 ? -w : w);
        int color = quien == 0 ? 0x3FA9F5 : quien == 1 ? 0xFF6B6B : 0xFFC857;
        pi.textoCentrado(nombre, nx + 3, y + h * 0.04f + 3, tn, true, 0x000000, 0.6f * entra);
        pi.textoCentrado(nombre, nx, y + h * 0.04f, tn, true, color, entra);
        String linea = p.combatientes[quien].montaje.modelo.nombre(T.idioma) + " · " + biografia(p, quien);
        float bw = ancho, tb = ui.letra * 1.05f;
        float bh = pi.altoDeParrafo(linea, bw - 40, tb) + 24, by = y + h * 0.04f + tn * 1.4f;
        float a = Math.min(1, Math.max(0, (local - 0.5f) / 0.4f));
        pi.caja(mitad - bw / 2, by, bw, bh, 12, 0x0B0F18, 0.85f * a);
        pi.parrafo(linea, mitad - bw / 2 + 20, by + 12, bw - 40, tb, 0xF4F1E8, a);
        pi.textoCentrado(T.t("entrada.saltar"), x + w / 2, y + h - 76 - ui.letra * 0.9f, ui.letra * 0.75f, false, 0xAAB0BA, 0.85f);
    }
}
