package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.campanas.EscenaDeCampana;
import metajavbots.campanas.EscenaDeCampana.Animacion;
import metajavbots.campanas.EscenaDeCampana.Decorado;
import metajavbots.campanas.EscenaDeCampana.Vineta;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.textos.Idioma;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * DIBUJO ESCENA CAMPAÑA — Dibuja por código una viñeta ilustrada de campaña
 * (decorados, público, robots, entrenador normal o en camilla de hospital,
 * animaciones de entrada/zoom/sacudida/destello/lluvia/confeti, bocadillos y
 * narración), sin imágenes externas y sin recortes ni solapamientos en letra
 * 18 y 26.
 */
public final class DibujoEscenaCampana {

    private static final int[] PALETA_CONFETI = {0xFFC857, 0x3FA9F5, 0xFF6B6B, 0x5BD685, 0xB07CFF, 0xFFF3B0};

    private DibujoEscenaCampana() {
    }

    /** Dibuja una viñeta completa dentro del rectángulo (x, y, w, h) en el instante t. */
    public static void dibujar(Juego j, VistaDeArena vista, Vineta v, Entrenador entrenador,
                               String nombreRival, float x, float y, float w, float h, float t) {
        if (j == null || j.pincel == null || v == null || w <= 40 || h <= 40) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Idioma idioma = j.jugador.idioma;
        String modeloJugador = j.jugador.robot() != null ? j.jugador.robot().modelo : "cobalto";
        float sacX = v.animacion == Animacion.SACUDIDA && t < 0.7f ? (float) Math.sin(t * 58) * (1 - t / 0.7f) * 9 : 0;
        float sacY = v.animacion == Animacion.SACUDIDA && t < 0.7f ? (float) Math.cos(t * 47) * (1 - t / 0.7f) * 6 : 0;

        String narracion = v.textoNarracion(idioma, entrenador.nombre.en(idioma), nombreRival, null);
        float tamNarr = Math.min(ui.letra * 0.96f, 24f);
        float altoNarr = Math.min(h * 0.30f, p.altoDeParrafo(narracion, w - 56, tamNarr) + 26);
        if (p.altoDeParrafo(narracion, w - 56, tamNarr) + 22 > altoNarr) {
            tamNarr = Math.max(14f, tamNarr * 0.84f);
            altoNarr = Math.min(h * 0.32f, p.altoDeParrafo(narracion, w - 56, tamNarr) + 24);
        }
        float escenaH = Math.max(120, h - altoNarr - 12);

        p.recortar(x, y, w, escenaH);
        dibujarDecorado(p, v.decorado, x + sacX, y + sacY, w, escenaH, t);
        if (v.mostrarPublico) dibujarPublico(p, x + sacX, y + sacY + escenaH * 0.60f, w, escenaH * 0.16f, t);
        dibujarPersonajes(p, vista, v, entrenador, modeloJugador, x + sacX, y + sacY, w, escenaH, t);
        dibujarEfectoAnimacion(p, v.animacion, x, y, w, escenaH, t);
        dibujarBocadillos(p, ui, v, entrenador.nombre.en(idioma), nombreRival, idioma, x, y, w, escenaH, t);
        p.sinRecorte();

        p.bordeRedondeado(x, y, w, escenaH, 14, 0x262C38, 0.95f, 3f);
        dibujarNarracion(p, narracion, x, y + escenaH + 8, w, altoNarr, tamNarr);
    }

    private static void dibujarDecorado(Pincel p, Decorado d, float x, float y, float w, float h, float t) {
        float sueloY = y + h * 0.68f;
        switch (d) {
            case GIMNASIO:
                p.degradado(x, y, w, h, 0x3A2E25, 0x1D1814, 1);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x5B422C, 1);
                p.linea(x, sueloY, x + w, sueloY, 4, 0x8F6A47, 1);
                float sacoX = x + w * 0.50f + (float) Math.sin(t * 2.4f) * 10;
                p.linea(x + w * 0.50f, y, sacoX, y + h * 0.22f, 3, 0xAAB0BA, 1);
                p.caja(sacoX - 18, y + h * 0.22f, 36, h * 0.26f, 12, 0xB83B2A, 1);
                break;
            case PLAYA:
                p.degradado(x, y, w, sueloY - y, 0xFF9F5A, 0xFFD27A, 1);
                p.circulo(x + w * 0.78f, y + h * 0.24f, h * 0.11f, 0xFFF2A6, 0.95f);
                p.rect(x, y + h * 0.50f, w, h * 0.18f, 0x2A82B8, 1);
                for (int i = 0; i < 6; i++) {
                    float ox = x + ((i * w * 0.2f + t * 35) % (w + 80)) - 40;
                    p.linea(ox, y + h * 0.58f, ox + 54, y + h * 0.58f, 3, 0xE6F7FF, 0.7f);
                }
                p.rect(x, sueloY, w, h - (sueloY - y), 0xE5C07B, 1);
                break;
            case HOSPITAL:
                p.degradado(x, y, w, h, 0xD7E6EE, 0xB4CDD9, 1);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x9BB2BE, 1);
                p.caja(x + w * 0.08f, y + h * 0.10f, w * 0.14f, h * 0.18f, 8, 0xFFFFFF, 0.9f);
                p.rect(x + w * 0.14f, y + h * 0.13f, w * 0.02f, h * 0.12f, 0xE03E3E, 1);
                p.rect(x + w * 0.11f, y + h * 0.18f, w * 0.08f, h * 0.03f, 0xE03E3E, 1);
                break;
            case MILAGRO:
                p.degradado(x, y, w, h, 0x1D2847, 0x3D2E1E, 1);
                p.brillo(x + w * 0.50f, y + h * 0.32f, w * 0.34f, 0xFFE680, 0.45f);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x283042, 1);
                break;
            case ISLA_PUERTO:
                p.degradado(x, y, w, sueloY - y, 0x1A2942, 0x3E5A78, 1);
                p.poligono(new float[]{x + w * 0.30f, sueloY, x + w * 0.52f, y + h * 0.18f, x + w * 0.76f, sueloY}, 0x243447, 1);
                p.rect(x, sueloY - h * 0.06f, w, h * 0.12f, 0x1D5C8A, 1);
                p.rect(x, sueloY + h * 0.02f, w, h - (sueloY - y), 0x6C4B32, 1);
                break;
            case TEMPLO_ISLA:
                p.degradado(x, y, w, h, 0x5A2E2E, 0xC46B3B, 1);
                p.rect(x + w * 0.28f, y + h * 0.14f, w * 0.44f, 12, 0x9E2B25, 1);
                p.rect(x + w * 0.34f, y + h * 0.14f, 12, sueloY - (y + h * 0.14f), 0x9E2B25, 1);
                p.rect(x + w * 0.66f - 12, y + h * 0.14f, 12, sueloY - (y + h * 0.14f), 0x9E2B25, 1);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x7A5A3E, 1);
                break;
            case COCINA_TORNEO:
                p.degradado(x, y, w, h, 0x2B333E, 0x1D222A, 1);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x454D59, 1);
                p.aro(x + w * 0.14f, y + h * 0.24f, 24, 4, 0xCFD8E3, 0.85f);
                p.aro(x + w * 0.86f, y + h * 0.24f, 24, 4, 0xCFD8E3, 0.85f);
                break;
            case VOLCAN_HIELO:
                p.degradado(x, y, w, h, 0x1B3A5C, 0x6CA8D6, 1);
                p.poligono(new float[]{x + w * 0.18f, sueloY, x + w * 0.50f, y + h * 0.12f, x + w * 0.82f, sueloY}, 0xB8E0F6, 0.85f);
                p.rect(x, sueloY, w, h - (sueloY - y), 0xD7F0FF, 1);
                break;
            case TORMENTA_AZOTEA:
                p.degradado(x, y, w, h, 0x0E1526, 0x22304A, 1);
                p.rect(x + w * 0.46f, y + h * 0.10f, w * 0.08f, sueloY - (y + h * 0.10f), 0x3E495C, 1);
                p.brillo(x + w * 0.50f, y + h * 0.14f, 65, 0xFFE680, 0.5f);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x28303D, 1);
                break;
            case DESIERTO_SOL:
                p.degradado(x, y, w, sueloY - y, 0xD95D24, 0xF5B553, 1);
                p.circulo(x + w * 0.50f, y + h * 0.22f, h * 0.12f, 0xFFF1A8, 0.95f);
                p.rect(x, sueloY, w, h - (sueloY - y), 0xC98B44, 1);
                break;
            default:
                p.degradado(x, y, w, h, 0x12192B, 0x241B3E, 1);
                p.poligono(new float[]{x + w * 0.18f, y, x + w * 0.24f, y, x + w * 0.42f, sueloY, x + w * 0.26f, sueloY}, 0xFFF1C1, 0.12f);
                p.poligono(new float[]{x + w * 0.76f, y, x + w * 0.82f, y, x + w * 0.74f, sueloY, x + w * 0.58f, sueloY}, 0x3FE0FF, 0.12f);
                p.rect(x, sueloY, w, h - (sueloY - y), 0x202738, 1);
                p.linea(x + 20, sueloY - 18, x + w - 20, sueloY - 18, 3, 0xFF5E5B, 0.85f);
                p.linea(x + 20, sueloY - 36, x + w - 20, sueloY - 36, 3, 0x3FA9F5, 0.85f);
                break;
        }
    }

    private static void dibujarPublico(Pincel p, float x, float baseY, float w, float alto, float t) {
        int n = 18;
        float paso = w / n;
        for (int i = 0; i < n; i++) {
            float cx = x + paso * (i + 0.5f);
            float salto = Math.abs((float) Math.sin(t * 5 + i * 0.7f)) * alto * 0.28f;
            int col = PALETA_CONFETI[i % PALETA_CONFETI.length];
            p.caja(cx - 10, baseY - 18 - salto, 20, 18, 5, Pincel.oscurecer(col, 0.25f), 0.9f);
            p.circulo(cx, baseY - 24 - salto, 8, col, 0.95f);
        }
    }

    private static void dibujarPersonajes(Pincel p, VistaDeArena vista, Vineta v, Entrenador entrenador,
                                          String modeloJugador, float x, float y, float w, float h, float t) {
        float suelo = y + h * 0.86f;
        float entra = v.animacion == Animacion.ENTRADA ? Math.min(1f, t / 0.55f) : 1f;
        float zoom = v.animacion == Animacion.ZOOM ? 1f + Math.min(0.14f, t * 0.08f) : 1f;
        float talla = Math.min(1.15f, Math.max(0.75f, h / 320f)) * zoom;

        if (v.entrenadorEnCamilla) {
            Hospital.camilla(p, entrenador, x + w * 0.16f, y + h * 0.18f, w * 0.68f, h * 0.72f, t);
            return;
        }
        String idIzq = modeloJugador != null && !modeloJugador.isBlank() ? modeloJugador : v.modeloIzq;
        float xIzq = x + w * 0.25f - (1 - entra) * w * 0.30f;
        float xDer = x + w * 0.75f + (1 - entra) * w * 0.30f;
        if (vista != null) {
            vista.robotSuelto(p, new Montaje(Modelo.de(idIzq)), xIzq, suelo, talla, t, v.decorado == Decorado.MILAGRO ? 1f : 0f, true);
            vista.robotSuelto(p, new Montaje(Modelo.de(v.modeloDer)), xDer, suelo, talla, t + 1.2f, 0f, false);
        }
        if (v.mostrarEntrenador) {
            float r = Math.min(38f, h * 0.11f);
            float ex = v.decorado == Decorado.MILAGRO ? x + w * 0.50f : x + w * 0.11f;
            float ey = v.decorado == Decorado.MILAGRO ? y + h * 0.28f + (float) Math.sin(t * 3) * 6 : y + h * 0.26f;
            CabezaDeEntrenador.dibujar(p, entrenador, ex, ey, r, true, t);
        }
    }

    private static void dibujarEfectoAnimacion(Pincel p, Animacion anim, float x, float y, float w, float h, float t) {
        if (anim == Animacion.DESTELLO && t < 0.65f) {
            p.rect(x, y, w, h, 0xFFF8D6, (1 - t / 0.65f) * 0.55f);
        } else if (anim == Animacion.LLUVIA) {
            for (int i = 0; i < 36; i++) {
                float gx = x + ((i * 59 + t * 180) % w);
                float gy = y + ((i * 43 + t * 420) % h);
                p.linea(gx, gy, gx - 8, gy + 20, 2, 0xB8E4FF, 0.55f);
            }
            if ((t % 2.4f) < 0.12f) p.rect(x, y, w, h, 0xFFFFFF, 0.30f);
        } else if (anim == Animacion.CONFETI) {
            for (int i = 0; i < 42; i++) {
                float cx = x + ((i * 73 + (float) Math.sin(t * 2 + i) * 28 + w) % w);
                float cy = y + ((i * 37 + t * (90 + (i % 5) * 24)) % h);
                p.rect(cx, cy, 8, 5, PALETA_CONFETI[i % PALETA_CONFETI.length], 0.92f);
            }
        }
    }

    private static void dibujarBocadillos(Pincel p, Ui ui, Vineta v, String entrenador, String rival,
                                          Idioma idioma, float x, float y, float w, float h, float t) {
        float alfa = Math.min(1f, Math.max(0f, (t - 0.15f) / 0.30f));
        if (alfa <= 0.01f) return;
        String izq = v.textoBocadilloIzq(idioma, entrenador, rival, null);
        String der = v.textoBocadilloDer(idioma, entrenador, rival, null);
        float tam = Math.min(ui.letra * 0.88f, 22f);
        float maxW = Math.min(w * 0.42f, 460f);
        if (!izq.isBlank()) {
            dibujarBocadillo(p, izq, x + 22, y + 18, maxW, h * 0.34f, tam, true, alfa);
        }
        if (!der.isBlank()) {
            dibujarBocadillo(p, der, x + w - maxW - 22, y + 18, maxW, h * 0.34f, tam, false, alfa);
        }
    }

    private static void dibujarBocadillo(Pincel p, String texto, float x, float y, float w, float maxH, float tam,
                                         boolean izquierda, float alfa) {
        float t = tam;
        float bh = p.altoDeParrafo(texto, w - 24, t) + 18;
        if (bh > maxH) {
            t = Math.max(14f, t * 0.82f);
            bh = Math.min(maxH, p.altoDeParrafo(texto, w - 24, t) + 16);
        }
        p.caja(x, y, w, bh, 12, 0xFFFDF7, 0.96f * alfa);
        p.bordeRedondeado(x, y, w, bh, 12, 0x1F2430, 0.85f * alfa, 2f);
        float px = izquierda ? x + w * 0.30f : x + w * 0.70f;
        p.triangulo(px - 8, y + bh - 1, px + 8, y + bh - 1, px, y + bh + 12, 0xFFFDF7, 0.96f * alfa);
        p.parrafo(texto, x + 12, y + 9, w - 24, t, 0x141821, alfa);
    }

    private static void dibujarNarracion(Pincel p, String texto, float x, float y, float w, float h, float tam) {
        p.caja(x, y, w, h, 12, 0xF4EFE2, 0.98f);
        p.bordeRedondeado(x, y, w, h, 12, 0x3A3429, 0.9f, 2f);
        p.parrafo(texto, x + 20, y + 12, w - 40, tam, 0x1C1F26, 1);
    }

    /** Devuelve un texto de estado útil para depuración o pruebas sin ventana. */
    public static String resumenDeVineta(EscenaDeCampana escena, int indice, Idioma idioma) {
        if (escena == null || indice < 0 || indice >= escena.vinetas.size()) return "";
        Vineta v = escena.vinetas.get(indice);
        return v.decorado + ":" + v.animacion + ":" + v.narracion.en(idioma);
    }
}
