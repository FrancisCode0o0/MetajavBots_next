package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.combate.Combatiente;
import metajavbots.pantalla.DibujoRobot;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Montaje;
import metajavbots.textos.T;

/**
 * ROBOT EN REPARACIÓN — Un robot roto que se va arreglando a la vista: con
 * poco arreglo le faltan piezas, echa humo y chispas y pone cara de dolor; a
 * cada paso bien hecho (compila, se ha cambiado el código, acierta) recupera
 * una pieza con una lluvia de tornillos, y al final queda reluciente y
 * contento.
 */
final class RobotEnReparacion {

    /** Lo arreglado que se ve (va subiendo poco a poco hasta lo que toca). */
    private float visto = -1;
    private float t;
    private final List<float[]> tornillos = new ArrayList<>();
    /** Lo que suena al recuperar una pieza (puede ser null). */
    Runnable alRecuperar;

    RobotEnReparacion() {
    }

    RobotEnReparacion(Runnable alRecuperar) {
        this.alRecuperar = alRecuperar;
    }

    /**
     * En la pelea de un robot roto: arriba a la izquierda, cómo se va reparando según lo que el jugador hace bien
     * (compila, ha cambiado el código y va cumpliendo el objetivo); en la arena, el humo de lo que le falta.
     */
    void panel(Juego j, VistaDeArena vista, Combatiente yo, boolean compila, String fuente, String original, String objetivo, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean cambiado = compila && !fuente.equals(original);
        Combatiente.Informe inf = yo.informes.isEmpty() ? new Combatiente.Informe() : yo.informes.get(yo.informes.size() - 1);
        int quiere = Objetivo.aciertosQuePide(objetivo);
        float parte = quiere <= 0 ? (Objetivo.cumple(objetivo, yo, fuente) ? 1 : 0) : Math.min(1, inf.aciertos / (float) quiere);
        float arreglo = 0.1f + (compila ? 0.2f : 0) + (cambiado ? 0.2f : 0) + (cambiado ? 0.5f * parte : 0);
        vista.averiaDelJugador = 1 - arreglo;
        float tl = ui.letra * 0.72f, w = 250, h = 240 + 3 * tl * 1.4f, x = 16, y = 100;
        p.caja(x, y, w, h, 12, ui.tema.panel, 0.88f);
        p.texto(T.f("roto.reparado", Math.round(arreglo * 100)), x + 12, y + 8, ui.letra * 0.85f, true, arreglo >= 1 ? ui.tema.bien : 0xFFC857, 1);
        dibujar(p, vista, yo.montaje, x + w / 2, y + 222, 2.1f, arreglo, dt);
        float ly = y + 234;
        String[][] pasos = {{compila ? "✔" : "□", T.t("roto.compila")}, {cambiado ? "✔" : "□", T.t("roto.cambiado")},
            {parte >= 1 ? "✔" : "□", quiere > 0 ? T.f("roto.aciertos", inf.aciertos, quiere) : Objetivo.texto(objetivo)}};
        for (String[] paso : pasos) {
            boolean hecho = paso[0].equals("✔");
            p.texto(paso[0] + " " + ui.recortarTexto(paso[1], w - 40, tl, false), x + 12, ly, tl, false, hecho ? ui.tema.bien : ui.tema.texto, 1);
            ly += tl * 1.4f;
        }
    }

    /** Las piezas vuelven en este orden: piernas, brazo izquierdo, brazo derecho y cabeza. */
    private static final float[] UMBRAL = {0.85f, 0.45f, 0.65f, 0.25f};

    /**
     * Lo dibuja con los pies en (x, y). «arreglo», de 0 (destrozado) a 1 (como nuevo); «dt» anima (0 lo deja
     * quieto, para las listas).
     */
    void dibujar(Pincel p, VistaDeArena vista, Montaje m, float x, float y, float talla, float arreglo, float dt) {
        t += dt;
        if (visto < 0 || dt == 0) visto = arreglo;
        float antes = visto;
        visto += Math.signum(arreglo - visto) * Math.min(Math.abs(arreglo - visto), dt * 0.6f);
        for (float u : UMBRAL) {
            if (antes < u && visto >= u) recuperar(x, y - 40 * talla);
        }
        DibujoRobot.Estado e = new DibujoRobot.Estado();
        e.tiempo = t;
        for (int i = 0; i < 4; i++) e.partes[i] = visto >= UMBRAL[i] ? 1 : 0;
        e.cara = visto >= 1 ? 1 : visto >= UMBRAL[Montaje.CABEZA] ? 0 : 3;
        e.aturdido = visto < 0.2f;
        e.alegria = visto >= 1 ? 0.6f : 0;
        e.sacudida = visto < 0.5f ? (1 - visto) * 0.6f * (float) Math.sin(t * 23) : 0;
        e.alzado = visto >= 1 ? 0.1f * (float) Math.sin(t * 3) : -0.25f * (1 - visto);
        vista.robotConEstado(p, m, x, y, talla, e);
        humoYChispas(p, x, y - 50 * talla, talla);
        if (visto >= 1) brillo(p, x, y - 45 * talla, talla);
        moverTornillos(p, dt);
    }

    /** El humo (cuanto más roto, más) y las chispas sueltas. */
    private void humoYChispas(Pincel p, float x, float y, float talla) {
        float roto = 1 - visto;
        if (roto <= 0.02f) return;
        int bocanadas = 2 + Math.round(roto * 5);
        for (int i = 0; i < bocanadas; i++) {
            float f = ((t * 0.5f + i / (float) bocanadas) % 1f);
            float r = (6 + f * 18) * talla;
            p.circulo(x + (float) Math.sin(i * 2.3 + t) * 10 * talla, y - f * 70 * talla, r, 0x8A8F99, (1 - f) * 0.35f * roto);
        }
        if (visto < 0.65f && (t * 7) % 1f < 0.35f) {
            float a = t * 13;
            for (int k = 0; k < 3; k++) {
                float ang = a + k * 2.1f;
                p.linea(x + 14 * talla, y + 18 * talla, x + 14 * talla + (float) Math.cos(ang) * 14 * talla,
                        y + 18 * talla + (float) Math.sin(ang) * 14 * talla, 2, 0xFFD25A, 0.9f);
            }
        }
    }

    private void brillo(Pincel p, float x, float y, float talla) {
        for (int k = 0; k < 4; k++) {
            float ang = t * 1.5f + k * 1.57f, d = 38 * talla;
            float bx = x + (float) Math.cos(ang) * d, by = y + (float) Math.sin(ang) * d * 0.7f;
            p.textoCentrado("★", bx, by - 8, Math.max(14, 16 * talla), true, 0xFFE27A, 0.5f + 0.5f * (float) Math.sin(t * 4 + k));
        }
    }

    /** Una pieza que vuelve: salen tornillos dando vueltas y suena. */
    private void recuperar(float x, float y) {
        for (int k = 0; k < 10; k++) {
            double a = Math.PI * 2 * k / 10;
            tornillos.add(new float[]{x, y, (float) Math.cos(a) * 140, (float) Math.sin(a) * 140 - 120, 0.9f});
        }
        if (alRecuperar != null) alRecuperar.run();
    }

    private void moverTornillos(Pincel p, float dt) {
        for (int i = tornillos.size() - 1; i >= 0; i--) {
            float[] b = tornillos.get(i);
            b[0] += b[2] * dt;
            b[1] += b[3] * dt;
            b[3] += 420 * dt;
            b[4] -= dt;
            if (b[4] <= 0) {
                tornillos.remove(i);
                continue;
            }
            p.circulo(b[0], b[1], 4, 0xC9CED6, Math.min(1, b[4] * 2));
            p.rect(b[0] - 3, b[1] - 0.8f, 6, 1.6f, 0x5A5F66, Math.min(1, b[4] * 2));
        }
    }
}
