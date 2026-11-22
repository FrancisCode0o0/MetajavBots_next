package metajavbots.pantalla;

import java.util.Random;

/**
 * EFECTOS — Las partículas: chispas, humo, fuego, trozos de metal, confeti,
 * destellos y los fuegos artificiales. Viven en arrays fijos (nada se crea
 * en cada fotograma) y se dibujan en coordenadas de pantalla.
 */
public final class Efectos {

    /** Las clases de partícula, cada una con su forma de moverse y de dibujarse. */
    public enum Tipo { CHISPA, HUMO, FUEGO, TROZO, CONFETI, DESTELLO, ONDA, COHETE, BRASA }

    private static final int MAXIMO = 3000;
    private static final int[] COLORES_CONFETI = {0xFF5E5B, 0xFFD23F, 0x3FE0D0, 0x7B61FF, 0x5BD685, 0xFF8FB1};
    private final Tipo[] tipo = new Tipo[MAXIMO];
    private final float[] x = new float[MAXIMO], y = new float[MAXIMO], vx = new float[MAXIMO], vy = new float[MAXIMO];
    private final float[] vida = new float[MAXIMO], duracion = new float[MAXIMO], tam = new float[MAXIMO], giro = new float[MAXIMO];
    private final int[] color = new int[MAXIMO];
    private final float[] polyTrozo = new float[6];
    private int vivas;
    private final Random azar = new Random(3);
    /** Lo que estalla cuando un cohete llega arriba (lo oye el juego). */
    public Runnable alEstallar;

    public void anadir(Tipo t, float px, float py, float pvx, float pvy, float dur, float tamano, int rgb) {
        int i;
        if (vivas < MAXIMO) i = vivas++;
        else i = azar.nextInt(MAXIMO);   // lleno: se reemplaza una al azar
        tipo[i] = t;
        x[i] = px;
        y[i] = py;
        vx[i] = pvx;
        vy[i] = pvy;
        vida[i] = 0;
        duracion[i] = dur;
        tam[i] = tamano;
        color[i] = rgb;
        giro[i] = azar.nextFloat() * 6.28f;
    }

    public void chispas(float px, float py, int n, int rgb) {
        for (int k = 0; k < n; k++) {
            double a = azar.nextDouble() * Math.PI * 2;
            float v = 80 + azar.nextFloat() * 260;
            anadir(Tipo.CHISPA, px, py, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 80, 0.25f + azar.nextFloat() * 0.35f, 2 + azar.nextFloat() * 2, rgb);
        }
    }

    public void humo(float px, float py, float fuerza) {
        anadir(Tipo.HUMO, px + azar.nextFloat() * 10 - 5, py, azar.nextFloat() * 20 - 10, -30 - azar.nextFloat() * 30,
                1.2f + azar.nextFloat(), 6 + fuerza * 8, azar.nextBoolean() ? 0x555555 : 0x3A3A3A);
    }

    public void explosion(float px, float py, float fuerza) {
        anadir(Tipo.DESTELLO, px, py, 0, 0, 0.25f, 40 * fuerza, 0xFFF1C1);
        anadir(Tipo.ONDA, px, py, 0, 0, 0.5f, 80 * fuerza, 0xFFB347);
        for (int k = 0; k < 30 * fuerza; k++) {
            double a = azar.nextDouble() * Math.PI * 2;
            float v = 60 + azar.nextFloat() * 220 * fuerza;
            anadir(Tipo.FUEGO, px, py, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 40, 0.4f + azar.nextFloat() * 0.5f, 8
                    + azar.nextFloat() * 10 * fuerza,
                    azar.nextBoolean() ? 0xFF6B1A : 0xFFC145);
        }
        for (int k = 0; k < 12 * fuerza; k++) {
            double a = -Math.PI * azar.nextDouble();
            float v = 150 + azar.nextFloat() * 260 * fuerza;
            anadir(Tipo.TROZO, px, py, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 1 + azar.nextFloat(), 3 + azar.nextFloat() * 5, 0x6B6F76);
        }
        for (int k = 0; k < 10 * fuerza; k++) humo(px + azar.nextFloat() * 40 - 20, py + azar.nextFloat() * 20 - 10, fuerza * 1.5f);
    }

    /** Escombros, chispas y polvo al romperse un obstáculo destructible (barril o chimenea). */
    public void escombros(float px, float py, int colorPrincipal, int colorSecundario) {
        anadir(Tipo.ONDA, px, py, 0, 0, 0.35f, 55, colorSecundario);
        chispas(px, py, 10, 0xFFE38A);
        for (int k = 0; k < 14; k++) {
            double a = -Math.PI * azar.nextDouble();
            float v = 90 + azar.nextFloat() * 220;
            int c = k % 3 == 0 ? colorSecundario : colorPrincipal;
            anadir(Tipo.TROZO, px, py, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 0.7f + azar.nextFloat() * 0.7f, 4 + azar.nextFloat() * 5, c);
        }
        for (int k = 0; k < 6; k++) humo(px + azar.nextFloat() * 24 - 12, py + azar.nextFloat() * 14 - 7, 0.9f);
    }

    /** Un cohete de fuegos artificiales que sube y estalla en colores. */
    public void cohete(float px, float py, float alto, int rgb) {
        anadir(Tipo.COHETE, px, py, azar.nextFloat() * 60 - 30, -alto * 1.6f, 0.62f + azar.nextFloat() * 0.25f, 3, rgb);
    }

    public void confeti(float px, float py, int n) {
        for (int k = 0; k < n; k++) {
            anadir(Tipo.CONFETI, px + azar.nextFloat() * 40 - 20, py, azar.nextFloat() * 300 - 150, -200 - azar.nextFloat() * 300,
                    2 + azar.nextFloat() * 2, 5 + azar.nextFloat() * 4, COLORES_CONFETI[azar.nextInt(COLORES_CONFETI.length)]);
        }
    }

    public void avanzar(float dt) {
        for (int i = 0; i < vivas; i++) {
            vida[i] += dt;
            if (vida[i] >= duracion[i]) {
                if (tipo[i] == Tipo.COHETE) estallar(x[i], y[i], color[i]);
                vivas--;
                copiar(vivas, i);
                i--;
                continue;
            }
            switch (tipo[i]) {
                case HUMO:
                    vx[i] *= 0.98f;
                    vy[i] *= 0.99f;
                    tam[i] += dt * 14;
                    break;
                case CHISPA: case TROZO: case CONFETI: case BRASA:
                    vy[i] += (tipo[i] == Tipo.CONFETI ? 260 : 520) * dt;
                    if (tipo[i] == Tipo.CONFETI) vx[i] *= 0.985f;
                    break;
                case FUEGO:
                    vx[i] *= 0.92f;
                    vy[i] = vy[i] * 0.92f - 30 * dt;
                    break;
                case COHETE:
                    vy[i] += 300 * dt;
                    if (azar.nextFloat() < 0.6f) anadir(Tipo.BRASA, x[i], y[i], azar.nextFloat() * 20 - 10, 20, 0.4f, 1.6f, 0xFFE0A0);
                    break;
                default:
                    break;
            }
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            giro[i] += dt * 8;
        }
    }

    private void estallar(float px, float py, int rgb) {
        int n = 70;
        for (int k = 0; k < n; k++) {
            double a = Math.PI * 2 * k / n + azar.nextDouble() * 0.1;
            float v = 120 + azar.nextFloat() * 160;
            anadir(Tipo.BRASA, px, py, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 1.1f + azar.nextFloat() * 0.6f, 2.6f,
                    azar.nextInt(4) == 0 ? 0xFFFFFF : rgb);
        }
        anadir(Tipo.DESTELLO, px, py, 0, 0, 0.18f, 50, rgb);
        if (alEstallar != null) alEstallar.run();
    }

    private void copiar(int de, int a) {
        tipo[a] = tipo[de];
        x[a] = x[de];
        y[a] = y[de];
        vx[a] = vx[de];
        vy[a] = vy[de];
        vida[a] = vida[de];
        duracion[a] = duracion[de];
        tam[a] = tam[de];
        color[a] = color[de];
        giro[a] = giro[de];
    }

    public void dibujar(Pincel p) {
        for (int i = 0; i < vivas; i++) {
            float t = vida[i] / duracion[i], a = 1 - t;
            switch (tipo[i]) {
                case HUMO: p.circulo(x[i], y[i], tam[i], color[i], 0.35f * a); break;
                case FUEGO:
                    p.circulo(x[i], y[i], tam[i] * (1 - t * 0.5f), Pincel.mezcla(color[i], 0x551100, t), 0.85f * a);
                    break;
                case CHISPA:
                    p.linea(x[i], y[i], x[i] - vx[i] * 0.03f, y[i] - vy[i] * 0.03f, tam[i] * 0.7f, color[i], a);
                    break;
                case TROZO: {
                    float s = tam[i];
                    float c = (float) Math.cos(giro[i]) * s, d = (float) Math.sin(giro[i]) * s;
                    polyTrozo[0] = x[i] - c; polyTrozo[1] = y[i] - d;
                    polyTrozo[2] = x[i] + d; polyTrozo[3] = y[i] - c;
                    polyTrozo[4] = x[i] + c; polyTrozo[5] = y[i] + d;
                    p.poligono(polyTrozo, color[i], Math.min(1, a * 2));
                    break;
                }
                case CONFETI: {
                    float w = tam[i] * Math.abs((float) Math.cos(giro[i]));
                    p.rect(x[i] - w / 2, y[i] - tam[i] / 3, Math.max(1, w), tam[i] * 0.66f, color[i], Math.min(1, a * 3));
                    break;
                }
                case DESTELLO: p.brillo(x[i], y[i], tam[i] * (0.6f + t), color[i], a); break;
                case ONDA: p.aro(x[i], y[i], tam[i] * t + 4, 4 * a + 1, color[i], a * 0.8f); break;
                case COHETE: p.circulo(x[i], y[i], 3, color[i], 1); break;
                case BRASA: p.circulo(x[i], y[i], tam[i] * a + 0.5f, color[i], a); break;
                default: break;
            }
        }
    }

    public int vivas() {
        return vivas;
    }

    public void vaciar() {
        vivas = 0;
    }
}
