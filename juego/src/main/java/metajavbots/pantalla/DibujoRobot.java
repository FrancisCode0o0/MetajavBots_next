package metajavbots.pantalla;

import java.util.List;

import metajavbots.combate.Combatiente;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;

/**
 * DIBUJO DEL ROBOT — Dibuja un metajavbot de pie, en vista de tres cuartos,
 * con formas: su torso y su cabeza según el modelo (Modelo.Torso, Modelo.Cabeza),
 * sus brazos según el arma de cada pieza, sus piernas según cómo se mueve,
 * sus colores y detalles; y encima, las abolladuras, las quemaduras, las
 * partes rotas y los efectos (escudo, turbo, camuflaje, aturdido…).
 * Diseños propios, inspirados en la robótica y la ciencia ficción.
 */
public final class DibujoRobot {

    /** Cómo está el robot en este momento. */
    public static final class Estado {
        /** Hacia dónde mira: 1 a la derecha, -1 a la izquierda. */
        public float lado = 1;
        /** La inclinación de los brazos al apuntar (radianes; positivo hacia arriba). */
        public float alzado;
        public float paso, tiempo;
        /** 0..1 lo que le queda a cada parte (cabeza, izquierda, derecha, piernas). */
        public final float[] partes = {1, 1, 1, 1};
        public List<Combatiente.Abolladura> abolladuras;
        public float retroceso, sacudida, alegria;
        public boolean escudo, escudoDoble, turbo, camuflaje, aturdido, sobrecarga, fuera, atacando, cortocircuito;
        public int colorEscudo = 0x59C3FF;
        public int brazoQueAtaca = -1;

        /** Configura el color y grosor del aura de escudo según el tipo de protección activo. */
        public void aplicarEscudo(boolean cabeza, metajavbots.combate.Armas.TipoProteccion tCab,
                                  boolean brazo, metajavbots.combate.Armas.TipoProteccion tBr) {
            escudo = cabeza || brazo;
            metajavbots.combate.Armas.TipoProteccion t = brazo && tBr != null ? tBr : tCab;
            escudoDoble = t == metajavbots.combate.Armas.TipoProteccion.IMPACTO || t == metajavbots.combate.Armas.TipoProteccion.INTEGRAL;
            colorEscudo = t == metajavbots.combate.Armas.TipoProteccion.PROYECTIL ? 0x66F0FF
                    : t == metajavbots.combate.Armas.TipoProteccion.IMPACTO ? 0xFFAA3B
                    : t == metajavbots.combate.Armas.TipoProteccion.INTEGRAL ? 0x5BE8B5 : 0x59C3FF;
        }
        /** La cara de la pantalla (PANTALLA): 0 normal, 1 contento, 2 enfadado, 3 dolorido. */
        public int cara;
        /** Cómo va el último golpe del brazo que ataca: 0 recién lanzado, 1 ya recogido (para la bola, la pinza, el volteador). */
        public float faseDelAtaque = 1;
        /** Lo que le queda en el aire tras un volteador (0..1). */
        public float volteo;
        /** Elevación normalizada (0..1) durante un salto desde una rampa. */
        public float salto;
    }

    private final Pincel p;
    // La transformación de este dibujo: el pie en (x0, y0), u píxeles por unidad, lado ±1.
    private float x0, y0, u, lado;
    private float alfa = 1;

    public DibujoRobot(Pincel p) {
        this.p = p;
    }

    /** De la x local del robot (en unidades de su dibujo) a la de la pantalla. */
    private float enX(float lx) {
        return x0 + lx * u * lado;
    }

    /** De la y local del robot (hacia arriba) a la de la pantalla (hacia abajo). */
    private float enY(float ly) {
        return y0 - ly * u;
    }

    private float[] pts(float... local) {
        float[] v = new float[local.length];
        for (int i = 0; i < local.length; i += 2) {
            v[i] = enX(local[i]);
            v[i + 1] = enY(local[i + 1]);
        }
        return v;
    }

    private void forma(int rgb, float... local) {
        p.poligono(pts(local), rgb, alfa);
    }

    private void borde(float grosor, int rgb, float... local) {
        float[] v = pts(local);
        float[] cerrado = new float[v.length + 2];
        System.arraycopy(v, 0, cerrado, 0, v.length);
        cerrado[v.length] = v[0];
        cerrado[v.length + 1] = v[1];
        p.contorno(cerrado, grosor, rgb, alfa * 0.85f);
    }

    private void bola(float lx, float ly, float r, int rgb) {
        p.circulo(enX(lx), enY(ly), r * u, rgb, alfa);
    }

    private void ovalo(float lx, float ly, float rx, float ry, int rgb) {
        p.elipse(enX(lx), enY(ly), rx * u, ry * u, 0, rgb, alfa);
    }

    private void caja(float lx, float ly, float w, float h, float radio, int rgb) {
        // (lx, ly) es la esquina de abajo a la izquierda en unidades; se dibuja según el lado.
        float sx = lado > 0 ? enX(lx) : enX(lx + w);
        p.caja(sx, enY(ly + h), w * u, h * u, radio * u, rgb, alfa);
    }

    private void raya(float ax, float ay, float bx, float by, float grosor, int rgb) {
        p.linea(enX(ax), enY(ay), enX(bx), enY(by), grosor * u, rgb, alfa);
    }

    /**
     * Dibuja el robot con los pies en (x, y) de la pantalla. «talla» es el
     * tamaño en píxeles de una unidad (un robot mediano mide unas 80 unidades).
     */
    public void dibujar(Modelo m, Montaje mo, float x, float y, float talla, Estado e) {
        u = talla * (float) (m.tamano / 30.0);
        lado = e.lado >= 0 ? 1 : -1;
        alfa = e.camuflaje ? 0.28f + 0.1f * (float) Math.sin(e.tiempo * 20) : 1;
        float bote = e.alegria > 0 ? Math.abs((float) Math.sin(e.tiempo * 9)) * 10 * e.alegria : 0;
        float temblor = e.sacudida > 0 ? (float) Math.sin(e.tiempo * 70) * e.sacudida * 0.4f : 0;
        x0 = x + temblor * u;
        y0 = y;
        Pieza.Locomocion loco = mo.pieza(Montaje.Hueco.PIERNAS) == null ? Pieza.Locomocion.BIPEDO : mo.pieza(Montaje.Hueco.PIERNAS).locomocion();
        float flota = loco == Pieza.Locomocion.VUELO ? 22 + 3 * (float) Math.sin(e.tiempo * 3)
                : loco == Pieza.Locomocion.FLOTANTE ? 7 + 2 * (float) Math.sin(e.tiempo * 4) : 0;
        // La sombra en el suelo (se encoge ligeramente al saltar o volar).
        float escalaSombra = flota > 10 ? 0.7f : e.salto > 0 ? Math.max(0.55f, 1f - e.salto * 0.35f) : 1f;
        p.elipse(x, y, 26 * u * escalaSombra, 7 * u * escalaSombra, 0, 0x000000, 0.35f * alfa);
        float aire = (e.volteo > 0 ? (float) Math.sin(Math.PI * Math.min(1, e.volteo)) * 34 : 0) + e.salto * 30;
        y0 = y - (flota + bote + aire) * u;
        if (e.fuera) {
            // Fuera de combate: se dibuja más abajo, torcido y apagado (se ve en Arena).
            y0 = y - (flota * 0.2f) * u + 6 * u;
        }
        int c1 = m.color1, c2 = m.color2, c3 = m.color3;
        if (e.fuera) {
            c1 = Pincel.oscurecer(c1, 0.55f);
            c2 = Pincel.oscurecer(c2, 0.55f);
            c3 = 0x333333;
        }
        float piernas = loco == Pieza.Locomocion.ORUGAS ? 16 : loco == Pieza.Locomocion.RUEDAS ? 18 : loco == Pieza.Locomocion.FLOTANTE ? 10
                : loco == Pieza.Locomocion.VUELO ? 8 : loco == Pieza.Locomocion.PATAS ? 16 : 24;
        float alto = m.torso == Modelo.Torso.ESBELTO ? 36 : m.torso == Modelo.Torso.CAPARAZON ? 24 : 28;
        float ancho = m.torso == Modelo.Torso.BARRIL ? 36 : m.torso == Modelo.Torso.CAPARAZON ? 44 : m.torso == Modelo.Torso.ESBELTO ? 22
                : m.torso == Modelo.Torso.ESFERA ? 32 : 32;
        float base = piernas;
        float cima = base + alto;
        float hombro = cima - 6;

        // Lo que va detrás: capa, mochila, panel, alerón, cola.
        detallesDetras(m, base, cima, ancho, c1, c2, c3, e);
        // El brazo de atrás (el izquierdo si mira a la derecha), más oscuro.
        brazo(m, mo, Montaje.Hueco.IZQUIERDA, -ancho / 2 + 4, hombro, e, true, c1, c2, c3);
        locomocion(loco, base, ancho, c1, c2, c3, e);
        torso(m, base, alto, ancho, c1, c2, c3, e);
        cabeza(m, cima, c1, c2, c3, e);
        emblemaDeLaCabeza(mo, cima, c2, e);
        brazo(m, mo, Montaje.Hueco.DERECHA, ancho / 2 - 4, hombro, e, false, c1, c2, c3);
        detallesDelante(m, base, cima, ancho, c1, c2, c3, e);
        danos(e, base, cima, ancho, piernas);
        efectos(e, base, cima, ancho, c3);
        alfa = 1;
    }

    // ================================================================ partes

    private void locomocion(Pieza.Locomocion loco, float base, float ancho, int c1, int c2, int c3, Estado e) {
        int oscuro = Pincel.oscurecer(c2, 0.45f), metal = Pincel.mezcla(c2, 0x777777, 0.5f);
        float fase = e.paso * 0.12f;
        boolean rotas = e.partes[Montaje.PIERNAS] <= 0;
        switch (loco) {
            case ORUGAS: {
                caja(-21, 0, 42, 15, 7, oscuro);
                caja(-19, 2, 38, 11, 5, Pincel.oscurecer(c2, 0.25f));
                for (int i = 0; i < 8; i++) {
                    float lx = -18 + ((i * 5 + e.paso * 0.4f) % 38);
                    raya(lx, 1, lx, 14, 1.4f, oscuro);
                }
                bola(-13, 7.5f, 4.5f, metal);
                bola(13, 7.5f, 4.5f, metal);
                bola(0, 7.5f, 3.5f, metal);
                caja(-10, 13, 20, 5, 2, c1);
                break;
            }
            case RUEDAS: {
                caja(-6, 8, 12, 12, 3, metal);
                for (float wx : new float[]{-12, 12}) {
                    bola(wx, 8, 9, oscuro);
                    bola(wx, 8, 6, metal);
                    for (int k = 0; k < 3; k++) {
                        double a = fase * 3 + k * Math.PI * 2 / 3;
                        raya(wx, 8, wx + (float) Math.cos(a) * 5.5f, 8 + (float) Math.sin(a) * 5.5f, 1.6f, oscuro);
                    }
                    bola(wx, 8, 2, c3);
                }
                break;
            }
            case FLOTANTE: {
                p.brillo(enX(0), enY(-2), 14 * u, c3, 0.6f * alfa);
                ovalo(0, 3, 20, 5.5f, oscuro);
                ovalo(0, 4.5f, 17, 3.5f, metal);
                ovalo(0, 1.5f, 12, 2, c3);
                caja(-6, 4, 12, 7, 3, c1);
                break;
            }
            case VUELO: {
                float aleteo = (float) Math.sin(e.tiempo * 18) * 6;
                forma(Pincel.aclarar(c2, 0.2f), -4, 30, -34, 42 + aleteo, -30, 30 + aleteo * 0.5f, -6, 22);
                forma(Pincel.aclarar(c2, 0.3f), 4, 30, 34, 42 - aleteo, 30, 30 - aleteo * 0.5f, 6, 22);
                p.brillo(enX(0), enY(0), 9 * u, c3, 0.5f * alfa);
                caja(-5, 0, 10, 8, 3, metal);
                bola(0, -1, 3, c3);
                break;
            }
            case PATAS: {
                for (int i = 0; i < 3; i++) {
                    float s = (float) Math.sin(fase * 2 + i * 2.1f) * 4;
                    float bx = -6 + i * 6;
                    raya(bx, 14, bx - 14 - i * 2, 9 + s, 2.6f, oscuro);
                    raya(bx - 14 - i * 2, 9 + s, bx - 18 - i * 3, 0, 2.2f, oscuro);
                    raya(bx, 14, bx + 14 + i * 2, 9 - s, 2.6f, metal);
                    raya(bx + 14 + i * 2, 9 - s, bx + 18 + i * 3, 0, 2.2f, metal);
                }
                ovalo(0, 13, 12, 5, c2);
                break;
            }
            case BIPEDO:
            default: {
                float s = rotas ? 0 : (float) Math.sin(fase) * 5;
                for (int k = 0; k < 2; k++) {
                    float lx = k == 0 ? -7 : 7, d = k == 0 ? s : -s;
                    int col = k == 0 ? oscuro : metal;
                    raya(lx, base - 2, lx + d * 0.5f, base * 0.5f, 6, col);
                    raya(lx + d * 0.5f, base * 0.5f, lx + d, 3, 5, col);
                    bola(lx + d * 0.5f, base * 0.5f, 3.6f, Pincel.mezcla(c1, col, 0.5f));
                    caja(lx + d - 5, 0, 11, 4, 2, k == 0 ? Pincel.oscurecer(c1, 0.3f) : c1);
                }
                caja(-11, base - 5, 22, 6, 3, oscuro);
                break;
            }
        }
    }

    private void torso(Modelo m, float base, float alto, float ancho, int c1, int c2, int c3, Estado e) {
        int luz = Pincel.aclarar(c1, 0.25f), sombra = Pincel.oscurecer(c1, 0.3f), linea = Pincel.oscurecer(c1, 0.6f);
        float cy = base + alto / 2, w = ancho / 2;
        switch (m.torso) {
            case ESFERA:
                bola(0, cy, w, sombra);
                bola(-1.5f, cy + 1.5f, w - 1.5f, c1);
                bola(-w * 0.35f, cy + w * 0.35f, w * 0.35f, luz);
                p.aro(enX(0), enY(cy), w * u, 1.5f * u, linea, alfa);
                ovalo(0, cy - w * 0.15f, w * 0.75f, w * 0.2f, c2);
                break;
            case CUNA:
                forma(c1, -w - 4, base + alto, w + 4, base + alto, w * 0.45f, base, -w * 0.45f, base);
                forma(luz, -w - 4, base + alto, -w * 0.2f, base + alto, -w * 0.15f, base + 3, -w * 0.45f, base);
                borde(1.5f, linea, -w - 4, base + alto, w + 4, base + alto, w * 0.45f, base, -w * 0.45f, base);
                forma(c2, -w * 0.4f, base + alto - 5, w * 0.4f, base + alto - 5, w * 0.15f, base + 6, -w * 0.15f, base + 6);
                break;
            case BARRIL:
                caja(-w, base, ancho, alto, 10, sombra);
                caja(-w + 1, base + 1, ancho - 3, alto - 2, 9, c1);
                caja(-w + 3, base + alto * 0.55f, ancho * 0.35f, alto * 0.35f, 6, luz);
                for (float f : new float[]{0.25f, 0.75f}) caja(-w, base + alto * f - 2, ancho, 4, 1, c2);
                break;
            case HEXAGONO:
                torsoHexagonal(cy, w, alto, c1, c2, c3, luz, linea);
                break;
            case ESBELTO:
                caja(-w, base, ancho, alto, 6, sombra);
                caja(-w + 1, base + 1, ancho - 2.5f, alto - 2, 5, c1);
                caja(-w + 2, base + alto * 0.5f, ancho * 0.3f, alto * 0.45f, 3, luz);
                caja(-w * 0.5f, base + alto * 0.3f, w, alto * 0.18f, 2, c2);
                for (int i = 0; i < 3; i++) bola(w * 0.15f, base + alto * 0.62f + i * 4, 1.2f, c3);
                break;
            case CAPARAZON: {
                ovalo(0, base + alto * 0.35f, w, alto * 0.85f, sombra);
                ovalo(-1, base + alto * 0.4f, w - 2, alto * 0.8f, c1);
                caja(-w, base - 2, ancho, alto * 0.4f, 4, c2);
                for (int i = -1; i <= 1; i++) ovalo(i * w * 0.5f, base + alto * 0.75f, w * 0.22f, alto * 0.22f, Pincel.mezcla(c1, luz, 0.6f));
                ovalo(-w * 0.35f, base + alto * 0.9f, w * 0.3f, alto * 0.18f, luz);
                break;
            }
            case CRISTAL:
                forma(c1, 0, base + alto + 4, w + 3, cy, 0, base - 2, -w - 3, cy);
                forma(luz, 0, base + alto + 4, -w - 3, cy, 0, cy);
                forma(sombra, 0, base - 2, w + 3, cy, 0, cy);
                borde(1.5f, linea, 0, base + alto + 4, w + 3, cy, 0, base - 2, -w - 3, cy);
                bola(0, cy, 3.5f, c3);
                break;
            case CAJA:
            default:
                caja(-w, base, ancho, alto, 5, sombra);
                caja(-w + 1, base + 1, ancho - 2.5f, alto - 2, 4, c1);
                caja(-w + 2, base + alto * 0.55f, ancho * 0.4f, alto * 0.38f, 3, luz);
                caja(-w * 0.55f, base + alto * 0.18f, w * 1.1f, alto * 0.25f, 2, c2);
                for (int i = 0; i < 3; i++) caja(-w * 0.4f + i * w * 0.32f, base + alto * 0.22f, w * 0.18f, alto * 0.16f, 1, c3);
                break;
        }
        if (m.tiene("rayas")) {
            for (int i = 0; i < 3; i++) raya(-w * 0.6f + i * w * 0.3f, base + alto * 0.95f, -w * 0.9f + i * w * 0.3f, base + alto * 0.55f, 2.2f, c3);
        }
        if (m.tiene("remaches")) {
            for (float rx : new float[]{-w + 3, w - 3}) for (float ry : new float[]{base + 3, base + alto - 3}) bola(rx, ry, 1.1f, Pincel.aclarar(c2, 0.4f));
        }
        if (m.tiene("parches")) {
            caja(w * 0.1f, base + alto * 0.6f, w * 0.5f, alto * 0.25f, 1, Pincel.mezcla(c1, 0x8C7A5B, 0.6f));
            raya(w * 0.1f, base + alto * 0.72f, w * 0.6f, base + alto * 0.72f, 0.8f, linea);
        }
        if (m.tiene("tubos")) {
            raya(-w * 0.7f, base + 3, -w * 0.9f, base + alto - 4, 2.4f, Pincel.mezcla(c2, 0xB0B0B0, 0.4f));
        }
        if (e.sobrecarga) p.brillo(enX(0), enY(base + alto / 2), ancho * 0.6f * u, 0xFF3B30, 0.5f * alfa);
    }

    /** El torso de seis lados, con su núcleo en medio. */
    private void torsoHexagonal(float cy, float w, float alto, int c1, int c2, int c3, int luz, int linea) {
        float[] v = new float[12];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            v[i * 2] = (float) Math.cos(a) * w;
            v[i * 2 + 1] = cy + (float) Math.sin(a) * alto / 2;
        }
        forma(c1, v);
        forma(luz, v[6], v[7], v[8], v[9], 0, cy, v[4], v[5]);
        borde(1.5f, linea, v);
        bola(0, cy, w * 0.32f, c2);
        bola(0, cy, w * 0.18f, c3);
    }

    private void cabeza(Modelo m, float cima, int c1, int c2, int c3, Estado e) {
        int luz = Pincel.aclarar(c1, 0.25f), linea = Pincel.oscurecer(c1, 0.6f), cuello = Pincel.oscurecer(c2, 0.3f);
        boolean rota = e.partes[Montaje.CABEZA] <= 0.0001f || e.fuera;
        int ojo = rota ? 0x2A2A2A : c3;
        caja(-3.5f, cima - 1, 7, 4, 1, cuello);
        float hy = cima + 3;
        switch (m.cabeza) {
            case CUPULA:
                ovalo(0, hy + 2, 12, 3, c2);
                p.elipse(enX(0), enY(hy + 7), 11 * u, 10 * u, 0, Pincel.mezcla(c3, 0x9FE7FF, 0.5f), 0.45f * alfa);
                p.aro(enX(0), enY(hy + 7), 11 * u, 1.2f * u, Pincel.aclarar(c2, 0.3f), alfa);
                bola(2, hy + 8, 3, ojo);
                bola(-3, hy + 11, 2, 0xFFFFFF);
                break;
            case MONOOJO:
                bola(0, hy + 8, 10, c1);
                bola(-2.5f, hy + 11, 4, luz);
                bola(3, hy + 8, 6, 0x111318);
                bola(3.5f, hy + 8, 4, ojo);
                bola(4.5f, hy + 9, 1.4f, 0xFFFFFF);
                p.aro(enX(3), enY(hy + 8), 6 * u, 1.2f * u, Pincel.aclarar(c2, 0.4f), alfa);
                break;
            case ANTENAS:
                cabezaConAntenas(hy, c1, c2, luz, ojo, e);
                break;
            case CUERNOS:
                caja(-10, hy, 20, 11, 4, c1);
                forma(Pincel.aclarar(c2, 0.4f), -9, hy + 9, -17, hy + 19, -13, hy + 8);
                forma(Pincel.aclarar(c2, 0.4f), 9, hy + 9, 17, hy + 19, 13, hy + 8);
                caja(-7, hy + 3, 14, 3.5f, 1.5f, 0x111318);
                caja(-6, hy + 3.6f, 12, 2.3f, 1, ojo);
                break;
            case CRESTA:
                forma(c3, -2, hy + 10, 6, hy + 24, 10, hy + 9);
                caja(-8, hy, 16, 11, 5, c1);
                caja(-6, hy + 5, 6, 5, 2, luz);
                forma(0x111318, -2, hy + 4, 8, hy + 6, 8, hy + 3, -2, hy + 2.5f);
                bola(5, hy + 4.5f, 1.5f, ojo);
                break;
            case PANTALLA:
                cabezaPantalla(hy, c1, c3, rota, e);
                break;
            case FAROL:
                caja(-7, hy, 14, 13, 2, c2);
                caja(-6, hy + 2, 12, 9, 2, Pincel.aclarar(c3, rota ? 0 : 0.3f));
                if (!rota) p.brillo(enX(3), enY(hy + 6.5f), 9 * u, c3, 0.5f * alfa);
                forma(c1, -9, hy + 13, 9, hy + 13, 0, hy + 20);
                bola(0, hy + 20.5f, 1.8f, c3);
                break;
            case CASCO:
                bola(0, hy + 7, 10, c1);
                caja(-10, hy, 20, 7, 2, c1);
                caja(-3, hy + 10, 14, 3, 1, 0x111318);
                caja(-2, hy + 10.6f, 12, 1.8f, 0.8f, ojo);
                caja(-1.5f, hy + 14, 3, 6, 1, c3);
                bola(-4, hy + 11, 3, luz);
                break;
            case OJOS:
                bola(0, hy + 7, 9, c1);
                bola(-3, hy + 10, 3, luz);
                for (int i = 0; i < 4; i++) bola(-2 + i * 2.6f + (i % 2) * 0.5f, hy + 6 + (i % 2) * 3, 1.6f, ojo);
                bola(6, hy + 4, 1.1f, ojo);
                bola(6.5f, hy + 9, 1.1f, ojo);
                break;
            case VISOR:
            default:
                caja(-9, hy, 18, 12, 4, c1);
                caja(-8, hy + 7, 8, 4, 2, luz);
                caja(-6, hy + 3.5f, 15, 4, 2, 0x111318);
                caja(-5, hy + 4.2f, 13, 2.6f, 1.3f, ojo);
                if (!rota) p.brillo(enX(5), enY(hy + 5.5f), 5 * u, ojo, 0.35f * alfa);
                break;
        }
        if (m.tiene("aro")) p.elipse(enX(0), enY(hy + 22), 9 * u, 2.4f * u, 0, c3, 0.8f * alfa);
        if (rota) {
            raya(-6, hy + 2, 3, hy + 10, 1, 0x111111);
        }
    }

    /**
     * La pieza de cabeza montada, a la vista: un emblema en el lado de la cabeza según lo que hace (radar, escudo, turbo,
     * cruz médica, sombra, pulso o furia), para que se vea qué cabeza lleva aunque el modelo sea el mismo.
     */
    private void emblemaDeLaCabeza(Montaje mo, float cima, int c2, Estado e) {
        Pieza pz = mo.pieza(Montaje.Hueco.CABEZA);
        if (pz == null || e.fuera) return;
        float bx = -10, by = cima + 9;
        bola(bx, by, 3.4f, Pincel.oscurecer(c2, 0.35f));
        switch (pz.especial()) {
            case ESCUDO:
                forma(0x6FD3FF, bx - 2.2f, by + 2, bx + 2.2f, by + 2, bx + 2.2f, by - 0.5f, bx, by - 2.6f, bx - 2.2f, by - 0.5f);
                break;
            case TURBO:
                forma(0xFFB000, bx - 2.5f, by - 1.5f, bx + 2.5f, by, bx - 2.5f, by + 1.5f);
                break;
            case REPARAR:
                caja(bx - 2.4f, by - 0.7f, 4.8f, 1.4f, 0.3f, 0x4CD964);
                caja(bx - 0.7f, by - 2.4f, 1.4f, 4.8f, 0.3f, 0x4CD964);
                break;
            case CAMUFLAJE:
                caja(bx - 2.6f, by - 0.8f, 5.2f, 1.6f, 0.6f, 0x111318);
                break;
            case PULSO:
                p.aro(enX(bx), enY(by), 2.4f * u, 0.7f * u, 0x9BE7FF, alfa);
                break;
            case SOBRECARGA:
                forma(0xFF3B30, bx - 1, by + 2.6f, bx + 1.6f, by + 0.2f, bx - 0.4f, by - 0.2f, bx + 1, by - 2.6f, bx - 1.6f, by - 0.2f, bx + 0.4f, by + 0.2f);
                break;
            default:
                // El radar: una antena parabólica pequeña que gira.
                float giro = (float) Math.sin(e.tiempo * 2) * 2;
                raya(bx, by, bx + giro, by + 4, 0.7f, 0xCCCCCC);
                ovalo(bx + giro, by + 4.5f, 2.6f, 1, 0xDDE3EA);
                break;
        }
    }

    /** La cabeza con dos antenas que se balancean. */
    private void cabezaConAntenas(float hy, int c1, int c2, int luz, int ojo, Estado e) {
        caja(-8, hy, 16, 12, 3, c1);
        caja(-7, hy + 6, 7, 5, 2, luz);
        caja(-6, hy + 3, 13, 4, 1.5f, 0x111318);
        bola(-2, hy + 5, 1.6f, ojo);
        bola(4, hy + 5, 1.6f, ojo);
        float vaiven = (float) Math.sin(e.tiempo * 5) * 2;
        raya(-4, hy + 12, -7 + vaiven, hy + 22, 1.2f, c2);
        raya(4, hy + 12, 8 + vaiven, hy + 21, 1.2f, c2);
        bola(-7 + vaiven, hy + 22, 2, ojo);
        bola(8 + vaiven, hy + 21, 2, ojo);
    }

    /** La cabeza de pantalla vieja: la cara en píxeles (contenta, enfadada, dolorida o normal), que parpadea, y sus rayas. */
    private void cabezaPantalla(float hy, int c1, int c3, boolean rota, Estado e) {
        caja(-10, hy, 20, 15, 2.5f, Pincel.oscurecer(c1, 0.15f));
        caja(-8.5f, hy + 1.5f, 17, 12, 1.5f, 0x0A1410);
        int pix = rota ? 0x333333 : c3;
        float t = e.tiempo;
        boolean parpadea = (t % 3.2f) < 0.12f;
        if (!parpadea) {
            caja(-5, hy + 8, 2.5f, 2.5f, 0, pix);
            caja(2.5f, hy + 8, 2.5f, 2.5f, 0, pix);
        }
        if (e.cara == 1) {
            caja(-5, hy + 4, 10, 1.5f, 0, pix);
            caja(-6, hy + 5, 1.5f, 1.5f, 0, pix);
            caja(4.5f, hy + 5, 1.5f, 1.5f, 0, pix);
        } else if (e.cara == 2) {
            raya(-6, hy + 11.5f, -2, hy + 10.5f, 1, pix);
            raya(6, hy + 11.5f, 2, hy + 10.5f, 1, pix);
            caja(-4, hy + 4, 8, 1.5f, 0, pix);
        } else if (e.cara == 3) {
            caja(-4, hy + 4, 8, 1.5f, 0, pix);
            caja(-5, hy + 3, 1.5f, 1.5f, 0, pix);
            caja(3.5f, hy + 3, 1.5f, 1.5f, 0, pix);
        } else {
            caja(-4, hy + 4.5f, 8, 1.5f, 0, pix);
        }
        // Las rayas de la pantalla vieja.
        for (int i = 0; i < 6; i++) caja(-8.5f, hy + 2 + i * 2, 17, 0.4f, 0, 0x000000);
    }

    private void brazo(Modelo m, Montaje mo, Montaje.Hueco hueco, float hx, float hy, Estado e, boolean detras, int c1, int c2, int c3) {
        Pieza pz = mo.pieza(hueco);
        int parte = hueco == Montaje.Hueco.IZQUIERDA ? Montaje.IZQUIERDA : Montaje.DERECHA;
        boolean rota = e.partes[parte] <= 0;
        float grosor = m.brazos == Modelo.Brazos.GRUESOS ? 7 : m.brazos == Modelo.Brazos.FINOS ? 3.8f : 5.2f;
        int col = detras ? Pincel.oscurecer(c2, 0.35f) : c2;
        int colArma = detras ? Pincel.oscurecer(c1, 0.3f) : c1;
        if (e.fuera) colArma = Pincel.oscurecer(colArma, 0.4f);
        float a = rota ? -1.2f : e.alzado + (e.alegria > 0 ? 1.2f : 0);
        int brazoQueAtaca = hueco == Montaje.Hueco.IZQUIERDA ? 0 : 1;
        float empuje = e.brazoQueAtaca == brazoQueAtaca ? -e.retroceso * 0.6f : 0;
        float largo = 15 + empuje;
        float codoX = hx + (float) Math.cos(a) * largo * 0.55f, codoY = hy + (float) Math.sin(a) * largo * 0.55f - 3;
        float manoX = hx + (float) Math.cos(a) * largo, manoY = hy + (float) Math.sin(a) * largo;
        if (m.tiene("hombreras")) {
            ovalo(hx, hy + 1, grosor * 1.25f, grosor * 0.9f, Pincel.aclarar(c1, detras ? 0 : 0.15f));
        }
        raya(hx, hy, codoX, codoY, grosor, col);
        raya(codoX, codoY, manoX, manoY, grosor * 0.9f, col);
        bola(codoX, codoY, grosor * 0.55f, Pincel.oscurecer(col, 0.2f));
        bola(hx, hy, grosor * 0.7f, colArma);
        if (pz == null) return;
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        float px = -sa, py = ca;   // perpendicular
        switch (pz.arma()) {
            case LASER: {
                float l = pz.valor("dano") > 10 ? 16 : 12;
                raya(manoX, manoY, manoX + ca * l, manoY + sa * l, grosor * 1.1f, colArma);
                raya(manoX + ca * 2, manoY + sa * 2, manoX + ca * (l - 2), manoY + sa * (l - 2), grosor * 0.4f, c3);
                bola(manoX + ca * l, manoY + sa * l, grosor * 0.55f, rota ? 0x333333 : 0x9FF7FF);
                if (!rota) p.brillo(enX(manoX + ca * l), enY(manoY + sa * l), grosor * 0.8f * u, 0x5FE8FF, 0.5f * alfa);
                break;
            }
            case BALA: {
                float l = pz.valor("dano") > 12 ? 20 : 13;
                raya(manoX, manoY, manoX + ca * l, manoY + sa * l, grosor * 1.25f, Pincel.oscurecer(colArma, 0.2f));
                raya(manoX, manoY, manoX + ca * l * 0.6f, manoY + sa * l * 0.6f, grosor * 1.6f, colArma);
                bola(manoX + ca * l, manoY + sa * l, grosor * 0.45f, 0x111111);
                break;
            }
            case MISIL: {
                float bx = manoX + ca * 4, by = manoY + sa * 4;
                forma(colArma, bx + px * 6, by + py * 6, bx + px * 6 + ca * 12, by + py * 6 + sa * 12,
                        bx - px * 6 + ca * 12, by - py * 6 + sa * 12, bx - px * 6, by - py * 6);
                for (int k = -1; k <= 1; k++) bola(bx + ca * 12 + px * k * 3.5f, by + sa * 12 + py * k * 3.5f, 1.8f, 0xE63946);
                break;
            }
            case CUERPO:
                armaDeMano(pz.id, manoX, manoY, ca, sa, grosor, colArma, rota, e, e.brazoQueAtaca == brazoQueAtaca ? e.faseDelAtaque : 1);
                break;
            case LLAMA: {
                raya(manoX, manoY, manoX + ca * 12, manoY + sa * 12, grosor * 1.2f, colArma);
                raya(manoX + ca * 12, manoY + sa * 12, manoX + ca * 15, manoY + sa * 15, grosor * 0.8f, 0x333333);
                if (!rota) bola(manoX + ca * 16, manoY + sa * 16, 1.5f + (float) Math.abs(Math.sin(e.tiempo * 15)), 0x3FA9FF);
                break;
            }
            case ESCUDO: {
                float sx = manoX + ca * 4, sy = manoY + sa * 4;
                float[] v = new float[12];
                for (int k = 0; k < 6; k++) {
                    double b = Math.PI / 6 + k * Math.PI / 3;
                    v[k * 2] = sx + (float) Math.cos(b) * 5;
                    v[k * 2 + 1] = sy + (float) Math.sin(b) * 11;
                }
                forma(Pincel.mezcla(colArma, 0x9AA0A6, 0.3f), v);
                borde(1.2f, c3, v);
                bola(sx, sy, 2, c3);
                break;
            }
            case REPARAR:
                reparadora(manoX, manoY, ca, sa, grosor, colArma);
                break;
            default:
                break;
        }
        if (rota && !detras) {
            // Cables sueltos.
            raya(manoX, manoY, manoX + 3, manoY - 5, 0.8f, 0xFFB000);
            raya(manoX, manoY, manoX - 2, manoY - 6, 0.8f, 0x3FA9FF);
        }
    }

    /**
     * Las armas de las luchas de robots de verdad: sierra (gira, y más deprisa al golpear), pinza (se cierra), volteador
     * (la pala sube), cuña, bola con cadena (sale disparada y se recoge) y sus mezclas. ¿Era una de ellas?
     */
    private boolean armaDeLucha(String id, float mx, float my, float ca, float sa, float grosor, int col, Estado e, float fase) {
        float px = -sa, py = ca;
        float golpe = 1 - fase;   // 1 justo al golpear, 0 en reposo
        boolean hecho = false;
        if (id.startsWith("sierra")) {
            float cx = mx + ca * 9, cy = my + sa * 9, r = 8;
            raya(mx, my, cx, cy, grosor * 0.7f, Pincel.oscurecer(col, 0.2f));
            bola(cx, cy, r, 0xB8BEC6);
            float giro = e.tiempo * (golpe > 0 ? 40 : 12);
            for (int k = 0; k < 8; k++) {
                double a = giro + k * Math.PI / 4;
                float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
                forma(0xE0E4EA, cx + dx * r, cy + dy * r, cx + dx * (r + 3) - dy * 2, cy + dy * (r + 3) + dx * 2, cx + dx * (r - 1) - dy * 3, cy
                        + dy * (r - 1) + dx * 3);
            }
            bola(cx, cy, 2.2f, Pincel.oscurecer(col, 0.3f));
            if (golpe > 0.5f) p.brillo(enX(cx + ca * r), enY(cy + sa * r), 6 * u, 0xFFD27A, 0.5f * alfa);
            hecho = true;
        }
        if (id.startsWith("pinza")) {
            float abre = 4 + 5 * fase;   // al golpear se cierra
            float bx = mx + ca * 4, by = my + sa * 4;
            raya(bx, by, bx + ca * 12 + px * abre, by + sa * 12 + py * abre, grosor * 0.6f, col);
            raya(bx, by, bx + ca * 12 - px * abre, by + sa * 12 - py * abre, grosor * 0.6f, col);
            raya(bx + ca * 12 + px * abre, by + sa * 12 + py * abre, bx + ca * 15 + px * (abre - 3), by + sa * 15 + py * (abre - 3), grosor * 0.5f,
                    Pincel.aclarar(col, 0.2f));
            raya(bx + ca * 12 - px * abre, by + sa * 12 - py * abre, bx + ca * 15 - px * (abre - 3), by + sa * 15 - py * (abre - 3), grosor * 0.5f,
                    Pincel.aclarar(col, 0.2f));
            if (id.contains("taladro")) forma(0xB8BEC6, bx + px * 2, by + py * 2, bx + ca * 16, by + sa * 16, bx - px * 2, by - py * 2);
            bola(bx, by, grosor * 0.6f, Pincel.oscurecer(col, 0.25f));
            return true;
        }
        if (id.startsWith("volteador") || id.equals("sierra_volteadora")) {
            // La pala: plana delante; al golpear sube de golpe.
            float sube = golpe * 18;
            float bx = mx + ca * 3, by = my + sa * 3 - 5;
            int pala = Pincel.mezcla(col, 0x9AA0A6, 0.5f);
            forma(pala, bx, by, bx + ca * 26, by + sa * 26 + sube, bx + ca * 26 + px * 4, by + sa * 26 + sube + 5, bx + px * 4, by + 5);
            raya(bx, by + 2, bx + ca * 26, by + sa * 26 + sube + 2, 0.9f, Pincel.aclarar(pala, 0.35f));
            raya(bx, by, bx + ca * 8, by + sa * 8 + sube * 0.4f, grosor, 0x5B6067);
            return true;
        }
        if (id.startsWith("cuna")) {
            float bx = mx + ca * 2, by = my + sa * 2;
            forma(Pincel.mezcla(col, 0xC0C6CE, 0.4f), bx + px * 9, by + py * 9, bx + ca * 18, by + sa * 18 - 6, bx - px * 9, by - py * 9);
            raya(bx + px * 9, by + py * 9, bx + ca * 18, by + sa * 18 - 6, 0.8f, 0xFFFFFF);
            return true;
        }
        if (id.startsWith("bola")) {
            // La cadena sale hasta 50 unidades al golpear y vuelve poco a poco.
            float lejos = 6 + 44 * (fase < 0.25f ? fase / 0.25f : 1 - (fase - 0.25f) / 0.75f) * (fase < 1 ? 1 : 0);
            float bx = mx + ca * lejos, by = my + sa * lejos;
            for (int k = 1; k <= 6; k++) {
                float f = k / 7f;
                bola(mx + (bx - mx) * f, my + (by - my) * f, 0.9f, 0x8A8F96);
            }
            bola(bx, by, 5.5f, 0x3A3F46);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + e.tiempo * 3;
                raya(bx, by, bx + (float) Math.cos(a) * 7.5f, by + (float) Math.sin(a) * 7.5f, 1.4f, 0x5B6067);
            }
            bola(bx - 1.5f, by + 1.5f, 1.6f, 0x9AA0A6);
            return true;
        }
        return hecho;
    }

    /** El brazo reparador: una pinza y una cruz verde. */
    private void reparadora(float manoX, float manoY, float ca, float sa, float grosor, int colArma) {
        float px = -sa, py = ca;   // perpendicular
        raya(manoX, manoY, manoX + ca * 9, manoY + sa * 9, grosor, colArma);
        raya(manoX + ca * 9, manoY + sa * 9, manoX + ca * 13 + px * 3, manoY + sa * 13 + py * 3, 1.5f, 0xCCCCCC);
        raya(manoX + ca * 9, manoY + sa * 9, manoX + ca * 13 - px * 3, manoY + sa * 13 - py * 3, 1.5f, 0xCCCCCC);
        caja(manoX - 2, manoY + 3, 4, 1.4f, 0.3f, 0x4CD964);
        caja(manoX - 0.7f, manoY + 1.7f, 1.4f, 4, 0.3f, 0x4CD964);
    }

    /** Las armas de cuerpo a cuerpo en la mano: espada, martillo, taladro, látigo o el puño (ca y sa: hacia dónde apunta el brazo). */
    private void armaDeMano(String id, float manoX, float manoY, float ca, float sa, float grosor, int colArma, boolean rota, Estado e, float fase) {
        float px = -sa, py = ca;   // perpendicular
        if (armaDeLucha(id, manoX, manoY, ca, sa, grosor, colArma, e, fase)) return;
        if (id.startsWith("espada") || id.startsWith("herencia") || id.startsWith("sable")) {
            int colHoja = id.startsWith("sable_fotonico") ? 0x4FE8FF : id.startsWith("sable_termico") ? 0xFF5A2C : 0xFF4FA3;
            float largoHoja = id.startsWith("sable_termico") ? 34 : 30;
            raya(manoX, manoY, manoX + ca * 4, manoY + sa * 4, grosor, Pincel.oscurecer(colArma, 0.3f));
            raya(manoX + ca * 4, manoY + sa * 4, manoX + ca * largoHoja, manoY + sa * largoHoja, grosor * 0.72f, colHoja);
            p.linea(enX(manoX + ca * 4), enY(manoY + sa * 4), enX(manoX + ca * largoHoja), enY(manoY + sa * largoHoja), grosor * 0.25f * u, 0xFFFFFF, alfa);
            if (!rota) p.brillo(enX(manoX + ca * 18), enY(manoY + sa * 18), 6 * u, colHoja, 0.35f * alfa);
        } else if (id.startsWith("martillo") || id.startsWith("maza") || id.startsWith("ariete")) {
            raya(manoX, manoY, manoX + ca * 10, manoY + sa * 10, grosor * 0.6f, 0x5A4632);
            float hx2 = manoX + ca * 12, hy2 = manoY + sa * 12;
            forma(Pincel.mezcla(colArma, 0x9AA0A6, 0.5f), hx2 + px * 8 - ca * 3, hy2 + py * 8 - sa * 3, hx2 + px * 8 + ca * 5, hy2 + py * 8 + sa * 5,
                    hx2 - px * 8 + ca * 5, hy2 - py * 8 + sa * 5, hx2 - px * 8 - ca * 3, hy2 - py * 8 - sa * 3);
        } else if (id.startsWith("taladro")) {
            float gx = manoX + ca * 4, gy = manoY + sa * 4;
            forma(0xB8BEC6, gx + px * 5, gy + py * 5, gx + ca * 18, gy + sa * 18, gx - px * 5, gy - py * 5);
            float giro = e.tiempo * 30;
            for (int k = 0; k < 3; k++) {
                float f = ((k / 3f + giro * 0.05f) % 1);
                raya(gx + ca * f * 16 + px * 4 * (1 - f), gy + sa * f * 16 + py * 4 * (1 - f), gx + ca * (f * 16 + 2) - px * 4 * (1 - f),
                        gy + sa * (f * 16 + 2) - py * 4 * (1 - f), 0.8f, 0x5B6067);
            }
        } else if (id.startsWith("latigo") || id.startsWith("lambda")) {
            float ox = manoX, oy = manoY;
            for (int k = 1; k <= 8; k++) {
                float f = k / 8f;
                float nx = manoX + ca * 30 * f + px * (float) Math.sin(e.tiempo * 12 + k) * 4 * f;
                float ny = manoY + sa * 30 * f + py * (float) Math.sin(e.tiempo * 12 + k) * 4 * f;
                raya(ox, oy, nx, ny, 1.4f, 0x9BE7FF);
                ox = nx;
                oy = ny;
            }
            if (!rota) p.brillo(enX(ox), enY(oy), 3 * u, 0x9BE7FF, 0.6f * alfa);
        } else {
            // Puños.
            bola(manoX + ca * 4, manoY + sa * 4, grosor * 1.2f, colArma);
            bola(manoX + ca * 5 + px * 2, manoY + sa * 5 + py * 2, grosor * 0.5f, Pincel.aclarar(colArma, 0.25f));
        }
    }

    private void detallesDetras(Modelo m, float base, float cima, float ancho, int c1, int c2, int c3, Estado e) {
        float w = ancho / 2;
        if (m.tiene("capa")) {
            float ondea = (float) Math.sin(e.tiempo * 3) * 3;
            forma(Pincel.oscurecer(c3, 0.55f), -w * 0.6f, cima - 2, w * 0.2f, cima - 2, -w * 0.4f - 8 + ondea, base - 6, -w - 10 + ondea, base);
        }
        if (m.tiene("mochila")) caja(-w - 6, base + 6, 10, (cima - base) * 0.7f, 3, Pincel.oscurecer(c2, 0.2f));
        if (m.tiene("panel")) {
            forma(0x1D3557, -w - 2, cima + 2, -w - 16, cima + 14, -w - 22, cima + 6, -w - 8, cima - 6);
            raya(-w - 9, cima + 4, -w - 15, cima + 9, 0.6f, 0x457B9D);
        }
        if (m.tiene("aleron")) forma(Pincel.oscurecer(c1, 0.2f), -w + 2, cima - 4, -w - 10, cima + 10, -w - 4, cima + 10, -w + 6, cima - 2);
        if (m.tiene("cola")) {
            raya(-w + 2, base + 4, -w - 12, base + 2, 2.5f, c2);
            raya(-w - 12, base + 2, -w - 18, base + 8, 2, c2);
            bola(-w - 18, base + 8, 2.2f, c3);
        }
        if (e.turbo) {
            float l = 14 + (float) Math.random() * 8;
            forma(0xFF9F1C, -w - 2, base + 8, -w - l, base + 5, -w - 2, base + 2);
            forma(0xFFF3B0, -w - 2, base + 7, -w - l * 0.6f, base + 5, -w - 2, base + 3);
        }
    }

    private void detallesDelante(Modelo m, float base, float cima, float ancho, int c1, int c2, int c3, Estado e) {
        if (m.tiene("pinchos")) {
            float w = ancho / 2;
            for (int i = 0; i < 3; i++) {
                float x = -w + 6 + i * w * 0.6f;
                forma(0xD9D9D9, x - 2, cima - 1, x, cima + 5, x + 2, cima - 1);
            }
        }
    }

    /** Las abolladuras y quemaduras de toda la pelea, en el sitio de cada parte. */
    private void danos(Estado e, float base, float cima, float ancho, float piernas) {
        if (e.abolladuras == null) return;
        float w = ancho / 2;
        int n = 0;
        for (Combatiente.Abolladura a : e.abolladuras) {
            if (++n > 60) break;
            float lx, ly, r = 1.2f + a.tamano * 2.6f;
            switch (a.parte) {
                case Montaje.CABEZA: lx = -6 + a.u * 12; ly = cima + 4 + a.v * 8; r *= 0.7f; break;
                case Montaje.IZQUIERDA: lx = -w + a.u * w * 0.6f; ly = cima - 10 + a.v * 6; break;
                case Montaje.DERECHA: lx = w * 0.4f + a.u * w * 0.6f; ly = cima - 10 + a.v * 6; break;
                default: lx = -w * 0.8f + a.u * w * 1.6f; ly = base + 2 + a.v * (cima - base - 4); break;
            }
            if (a.parte == Montaje.PIERNAS && a.v < 0.35f) ly = 2 + a.v * piernas * 2;
            if (a.quemadura) {
                p.elipse(enX(lx), enY(ly), r * 1.4f * u, r * u, 0, 0x1A1410, 0.55f * alfa);
                p.elipse(enX(lx), enY(ly), r * 0.7f * u, r * 0.5f * u, 0, 0x3B2A1A, 0.6f * alfa);
            } else {
                p.circulo(enX(lx), enY(ly), r * u, 0x000000, 0.32f * alfa);
                p.circulo(enX(lx - r * 0.3f), enY(ly + r * 0.3f), r * 0.55f * u, 0xFFFFFF, 0.12f * alfa);
                if (a.tamano > 0.7f) raya(lx - r, ly + r * 0.3f, lx + r * 0.8f, ly - r * 0.5f, 0.5f, 0x111111);
            }
        }
    }

    private void efectos(Estado e, float base, float cima, float ancho, int c3) {
        float cy = (base + cima) / 2 + 4;
        if (e.escudo) {
            float pulso = 1 + 0.04f * (float) Math.sin(e.tiempo * 8);
            float rx = (ancho * 0.75f + 10) * u * pulso, ry = (cima - base + 26) * 0.62f * u * pulso;
            p.elipse(enX(0), enY(cy), rx, ry, 0, e.colorEscudo, 0.18f);
            p.formas.setColor(p.color(Pincel.aclarar(e.colorEscudo, 0.25f), 0.75f));
            p.formas.ellipse(enX(0), enY(cy), rx, ry, 0, (e.escudoDoble ? 2.4f : 1.6f) * u);
            if (e.escudoDoble) p.formas.ellipse(enX(0), enY(cy), rx * 1.08f, ry * 1.08f, 0, 1.1f * u);
        }
        if (e.aturdido) {
            for (int i = 0; i < 3; i++) {
                double a = e.tiempo * 5 + i * Math.PI * 2 / 3;
                float sx = (float) Math.cos(a) * 12, sy = cima + 24 + (float) Math.sin(a) * 3;
                p.circulo(enX(sx), enY(sy), 2.2f * u, 0xFFE066, 1);
            }
            for (int i = 0; i < 2; i++) {
                float ax = (float) (Math.random() * 20 - 10), ay = (float) (base + Math.random() * (cima - base));
                p.linea(enX(ax), enY(ay), enX(ax + 5), enY(ay + 6), 1.2f * u, 0x9BE7FF, 0.9f);
            }
        }
        if (e.cortocircuito) {
            for (int i = 0; i < 4; i++) {
                float ax = (float) Math.sin(e.tiempo * 31 + i * 2.3) * (ancho * 0.55f);
                float ay = base + ((i + 0.5f) / 4f) * (cima - base);
                int col = i % 2 == 0 ? 0x3FE0FF : 0xFFE066;
                p.linea(enX(ax), enY(ay), enX(ax + 7), enY(ay + 5), 1.6f * u, col, 0.95f);
                p.linea(enX(ax + 7), enY(ay + 5), enX(ax + 2), enY(ay + 10), 1.6f * u, col, 0.95f);
            }
        }
    }
}
