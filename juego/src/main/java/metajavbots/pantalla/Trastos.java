package metajavbots.pantalla;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TRASTOS — Lo que sale despedido cuando un robot pierde: tornillos, tuercas
 * y engranajes que rebotan en el suelo hasta pararse, y su cabeza, que sale
 * volando y rueda. Cada trasto cae hasta «su» suelo (la línea del robot en
 * la arena, con la perspectiva) y rebota cada vez menos.
 */
final class Trastos {

    private enum Forma { TORNILLO, TUERCA, ENGRANAJE, CABEZA }

    private static final class Trasto {
        Forma forma;
        float x, y, vx, vy, suelo, giro, vgiro, vida, tam;
        int color;
    }

    private final List<Trasto> todos = new ArrayList<>();
    private final Random azar = new Random();

    /** Una lluvia de piezas desde (x, y), que caen hasta «suelo». */
    void piezas(float x, float y, float suelo, int n, float escala) {
        for (int i = 0; i < n; i++) {
            Trasto t = nuevo(x, y, suelo, escala);
            t.forma = i % 3 == 0 ? Forma.ENGRANAJE : i % 3 == 1 ? Forma.TUERCA : Forma.TORNILLO;
            t.color = i % 4 == 0 ? 0xC87533 : 0xB8C0C8;
        }
    }

    /** La cabeza del robot, que sale volando hacia un lado y rueda. */
    void cabeza(float x, float y, float suelo, float escala, int color) {
        Trasto t = nuevo(x, y, suelo, escala);
        t.forma = Forma.CABEZA;
        t.tam = 16 * escala;
        t.vx = (azar.nextBoolean() ? 1 : -1) * (140 + azar.nextFloat() * 60) * escala;
        t.vy = -320 * escala;
        t.color = color;
        t.vida = 6;
    }

    private Trasto nuevo(float x, float y, float suelo, float escala) {
        Trasto t = new Trasto();
        t.x = x;
        t.y = y;
        t.suelo = suelo + (azar.nextFloat() - 0.5f) * 30 * escala;
        t.vx = (azar.nextFloat() - 0.5f) * 420 * escala;
        t.vy = (-200 - azar.nextFloat() * 260) * escala;
        t.vgiro = (azar.nextFloat() - 0.5f) * 20;
        t.tam = (4 + azar.nextFloat() * 4) * escala;
        t.vida = 4 + azar.nextFloat() * 2;
        todos.add(t);
        return t;
    }

    void avanzar(float dt) {
        for (int i = todos.size() - 1; i >= 0; i--) {
            Trasto t = todos.get(i);
            t.vida -= dt;
            if (t.vida <= 0) {
                todos.remove(i);
                continue;
            }
            t.vy += 900 * dt;
            t.x += t.vx * dt;
            t.y += t.vy * dt;
            t.giro += t.vgiro * dt;
            if (t.y > t.suelo) {
                // Rebota cada vez menos; la cabeza, además, rueda (gira al ritmo de lo que avanza).
                t.y = t.suelo;
                t.vy = -t.vy * (t.forma == Forma.CABEZA ? 0.35f : 0.5f);
                t.vx *= 0.75f;
                if (Math.abs(t.vy) < 30) t.vy = 0;
                t.vgiro = t.forma == Forma.CABEZA ? t.vx / Math.max(1, t.tam) : t.vgiro * 0.6f;
            }
        }
    }

    void dibujar(Pincel p) {
        for (Trasto t : todos) {
            float a = Math.min(1, t.vida);
            switch (t.forma) {
                case CABEZA: {
                    p.elipse(t.x, t.suelo + 2, t.tam, t.tam * 0.3f, 0, 0x000000, 0.3f * a);
                    p.circulo(t.x, t.y - t.tam, t.tam, t.color, a);
                    // La cara, que da vueltas al rodar: dos ojos apagados (X) y la boca.
                    float ox = (float) Math.cos(t.giro) * t.tam * 0.45f, oy = (float) Math.sin(t.giro) * t.tam * 0.45f;
                    p.texto("x", t.x + ox - t.tam * 0.2f, t.y - t.tam + oy - t.tam * 0.3f, t.tam * 0.6f, true, 0x111318, a);
                    p.texto("x", t.x - oy - t.tam * 0.2f, t.y - t.tam + ox - t.tam * 0.3f, t.tam * 0.6f, true, 0x111318, a);
                    break;
                }
                case ENGRANAJE:
                    for (int k = 0; k < 6; k++) {
                        double g = t.giro + k * Math.PI / 3;
                        p.circulo(t.x + (float) Math.cos(g) * t.tam, t.y + (float) Math.sin(g) * t.tam, t.tam * 0.35f, t.color, a);
                    }
                    p.circulo(t.x, t.y, t.tam * 0.85f, t.color, a);
                    p.circulo(t.x, t.y, t.tam * 0.3f, 0x2A2E36, a);
                    break;
                case TUERCA:
                    p.circulo(t.x, t.y, t.tam, t.color, a);
                    p.circulo(t.x, t.y, t.tam * 0.45f, 0x2A2E36, a);
                    break;
                default:
                    p.linea(t.x - (float) Math.cos(t.giro) * t.tam, t.y - (float) Math.sin(t.giro) * t.tam, t.x + (float) Math.cos(t.giro) * t.tam,
                            t.y + (float) Math.sin(t.giro) * t.tam, t.tam * 0.45f, t.color, a);
                    p.circulo(t.x - (float) Math.cos(t.giro) * t.tam, t.y - (float) Math.sin(t.giro) * t.tam, t.tam * 0.45f, 0x8A929C, a);
                    break;
            }
        }
    }

    void vaciar() {
        todos.clear();
    }
}
