package metajavbots.pantallas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import metajavbots.Juego;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.T;

/**
 * REPETICIÓN — El mejor golpe de la pelea, a cámara lenta. Mientras se
 * pelea, guarda los últimos 4 s (dónde está cada robot y lo que pasa); cada
 * vez que hay un golpe más fuerte que el mejor, se queda con los 3 s de
 * antes y 1 s de después. Al acabar, se reproduce a un tercio de la
 * velocidad, con el rótulo «REPETICIÓN» (la pelea no se puede volver a
 * simular: el jugador puede cambiar el código a mitad).
 */
public final class Repeticion {

    /** Un turno: la posición de cada robot y lo que pasó. */
    private static final class Turno {
        final double[][] cuerpos;
        final List<Suceso> sucesos;

        Turno(Pelea p) {
            cuerpos = new double[p.combatientes.length][];
            for (int i = 0; i < cuerpos.length; i++) {
                Cuerpo c = p.cuerpo(i);
                if (c == null) continue;
                cuerpos[i] = new double[]{c.x, c.y, c.rumbo, c.mira, c.velocidad, c.parte[0], c.parte[1], c.parte[2], c.parte[3], c.fuera ? 1 : 0};
            }
            sucesos = new ArrayList<>(p.sucesos());
        }

        void poner(Pelea p) {
            for (int i = 0; i < cuerpos.length; i++) {
                Cuerpo c = p.cuerpo(i);
                if (c == null || cuerpos[i] == null) continue;
                double[] v = cuerpos[i];
                c.x = v[0];
                c.y = v[1];
                c.rumbo = v[2];
                c.mira = v[3];
                c.velocidad = v[4];
                for (int k = 0; k < 4; k++) c.parte[k] = v[5 + k];
                c.fuera = v[9] > 0;
            }
        }
    }

    private static final int ANTES = 90, DESPUES = 30;
    private final ArrayDeque<Turno> ultimos = new ArrayDeque<>();
    private List<Turno> mejor, grabando;
    private double mejorGolpe;
    private int faltan;
    private float reproduciendo = -1;
    private int aplicado = -1;
    private Turno comoAcabo;

    /** Un turno de la pelea (después de avanzar). */
    public void grabar(Pelea p) {
        Turno t = new Turno(p);
        ultimos.addLast(t);
        if (ultimos.size() > ANTES) ultimos.removeFirst();
        if (grabando != null) {
            grabando.add(t);
            if (--faltan <= 0) {
                mejor = grabando;
                grabando = null;
            }
        }
        for (Suceso s : t.sucesos) {
            if (s.tipo == Suceso.Tipo.IMPACTO && s.valor > mejorGolpe && s.valor >= 5) {
                mejorGolpe = s.valor;
                grabando = new ArrayList<>(ultimos);
                faltan = DESPUES;
            }
        }
    }

    public boolean hay() {
        return mejor != null || grabando != null;
    }

    public boolean activa() {
        return reproduciendo >= 0;
    }

    public void empezar(Pelea p) {
        if (mejor == null && grabando != null) mejor = grabando;
        if (mejor == null) return;
        comoAcabo = new Turno(p);
        reproduciendo = 0;
        aplicado = -1;
    }

    /** Un fotograma: pone a los robots donde estaban (a un tercio de la velocidad) y se ve lo que pasó. */
    public void avanzar(Juego j, Pelea p, VistaDeArena v, float dt) {
        reproduciendo += dt;
        int ahora = (int) (reproduciendo * Pelea.TURNOS_POR_SEGUNDO / 3);
        if (ahora >= mejor.size() || j.entrada.tecla(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            comoAcabo.poner(p);
            reproduciendo = -1;
            return;
        }
        // Cada turno, una sola vez (a cámara lenta, varios fotogramas seguidos son el mismo turno).
        for (int k = aplicado + 1; k <= ahora && k < mejor.size(); k++) {
            mejor.get(k).poner(p);
            v.sucesos(mejor.get(k).sucesos, j.sonidos);
            aplicado = k;
        }
    }

    /** El rótulo y el marco de la repetición. */
    public void rotulo(Juego j, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        p.rect(x, y, w, 8, 0xFF4040, 0.8f);
        p.rect(x, y + h - 8, w, 8, 0xFF4040, 0.8f);
        boolean luz = (reproduciendo * 2) % 1f < 0.6f;
        if (luz) p.circulo(x + 40, y + 46, 10, 0xFF3030, 1);
        p.texto(T.t("repeticion.titulo"), x + 60, y + 30, j.ui.letra * 1.3f, true, 0xFFFFFF, 1);
        p.textoDerecha(T.t("repeticion.lenta"), x + w - 30, y + 34, j.ui.letra * 0.9f, false, 0xFFE27A, 1);
    }
}
