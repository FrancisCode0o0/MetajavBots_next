package metajavbots.pantalla;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import metajavbots.combate.Suceso;
import metajavbots.sonido.Sonidos;
import metajavbots.textos.T;

/**
 * PÚBLICO — Los robots espectadores de la grada, al fondo de la arena. Se
 * mecen; con un buen golpe, una pieza rota o un fuera de combate, saltan con
 * los brazos en alto y gritan («¡BIEN!», «¡OOOH!»); si alguien se estampa
 * contra la pared, el código falla o pasa mucho rato sin golpes, abuchean
 * («BUUU»). Con su ovación o su abucheo.
 */
public final class Publico {

    private static final int FILAS = 2, POR_FILA = 34;
    private final int[] colores = new int[FILAS * POR_FILA];
    private final float[] fase = new float[FILAS * POR_FILA];
    /** De -1 (abuchea) a 1 (vitorea); vuelve sola a 0. */
    private float emocion, tiempo, sinGolpes;
    private final List<float[]> gritos = new ArrayList<>();
    private final List<String> textos = new ArrayList<>();
    private final Random azar = new Random(11);

    public Publico() {
        int[] paleta = {0x3FA9F5, 0xFF6B6B, 0x5BD685, 0xFFC857, 0xB07CFF, 0xFF8FB1, 0x3FE0D0, 0xE5C07B, 0x8A92A3};
        for (int i = 0; i < colores.length; i++) {
            colores[i] = paleta[azar.nextInt(paleta.length)];
            fase[i] = azar.nextFloat() * 6.28f;
        }
    }

    /** Cómo reacciona a lo que pasa en la pelea. */
    void reaccionar(Suceso s, Sonidos sonidos) {
        switch (s.tipo) {
            case IMPACTO:
                sinGolpes = 0;
                if (s.valor >= 8) animar(0.6f, "publico.bien", sonidos, "ovacion");
                break;
            case PARTE_ROTA: animar(1, "publico.oooh", sonidos, "ovacion"); break;
            case FUERA_DE_COMBATE: animar(1, "publico.si", sonidos, "ovacion"); break;
            case PARED: animar(-0.5f, "publico.buu", sonidos, null); break;
            case ERROR_CODIGO: animar(-0.4f, "publico.buu", sonidos, null); break;
            default: break;
        }
    }

    private void animar(float cuanto, String clave, Sonidos sonidos, String sonido) {
        boolean nuevo = Math.signum(cuanto) != Math.signum(emocion) || Math.abs(emocion) < 0.3f;
        emocion = Math.max(-1, Math.min(1, emocion + cuanto));
        if (!nuevo || gritos.size() > 4) return;
        gritos.add(new float[]{0.1f + azar.nextFloat() * 0.8f, 0});
        textos.add(T.t(clave));
        if (sonidos != null) sonidos.tocar(sonido != null ? sonido : "abucheo", 0.35f);
    }

    /** La grada, de (x, abajo - alto) a (x + w, abajo): debajo del horizonte, detrás del suelo de la arena. */
    void dibujar(Pincel p, float x, float abajo, float w, float alto, float dt, Sonidos sonidos) {
        tiempo += dt;
        sinGolpes += dt;
        emocion *= (float) Math.pow(0.45, dt);
        // Si pasa mucho rato sin golpes, se aburren.
        if (sinGolpes > 7) {
            sinGolpes = 0;
            animar(-0.5f, "publico.buu", sonidos, null);
        }
        p.rect(x - 20, abajo - alto, w + 40, alto, 0x1A1D24, 0.92f);
        p.rect(x - 20, abajo - alto, w + 40, 4, 0x3A3F48, 1);
        float paso = (w + 40) / POR_FILA;
        for (int f = 0; f < FILAS; f++) {
            float base = abajo - alto * (f == 0 ? 0.55f : 0.08f);
            for (int i = 0; i < POR_FILA; i++) {
                int k = f * POR_FILA + i;
                float cx = x - 20 + paso * (i + (f == 0 ? 0.5f : 0)), r = paso * 0.28f;
                float salto = emocion > 0.2f ? Math.abs((float) Math.sin(tiempo * 9 + fase[k])) * emocion * alto * 0.25f : 0;
                float tiembla = emocion < -0.2f ? (float) Math.sin(tiempo * 25 + fase[k]) * 2 : (float) Math.sin(tiempo * 1.5 + fase[k]);
                float cy = base - salto;
                int c = Pincel.oscurecer(colores[k], f == 0 ? 0.35f : 0.15f);
                p.caja(cx - r + tiembla, cy - r * 1.6f, r * 2, r * 1.6f, r * 0.4f, c, 1);
                p.circulo(cx + tiembla, cy - r * 2.2f, r * 0.75f, Pincel.aclarar(c, 0.1f), 1);
                p.circulo(cx - r * 0.28f + tiembla, cy - r * 2.25f, r * 0.14f, emocion < -0.2f ? 0xFF6B6B : 0xFFE27A, 1);
                p.circulo(cx + r * 0.28f + tiembla, cy - r * 2.25f, r * 0.14f, emocion < -0.2f ? 0xFF6B6B : 0xFFE27A, 1);
                if (emocion > 0.35f) {
                    // Los brazos en alto.
                    p.linea(cx - r, cy - r * 1.4f, cx - r * 1.4f, cy - r * 2.8f, r * 0.25f, c, 1);
                    p.linea(cx + r, cy - r * 1.4f, cx + r * 1.4f, cy - r * 2.8f, r * 0.25f, c, 1);
                }
            }
        }
        // Lo que gritan, flotando hacia arriba.
        for (int i = gritos.size() - 1; i >= 0; i--) {
            float[] g = gritos.get(i);
            g[1] += dt;
            if (g[1] > 1.6f) {
                gritos.remove(i);
                textos.remove(i);
                continue;
            }
            float a = Math.min(1, (1.6f - g[1]) * 2);
            int color = textos.get(i).startsWith("B") ? 0xFF6B6B : 0xFFE27A;
            p.textoCentrado(textos.get(i), x + w * g[0], abajo - alto - 10 - g[1] * 30, Math.max(16, alto * 0.35f), true, color, a);
        }
    }
}
