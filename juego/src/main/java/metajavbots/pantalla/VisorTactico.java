package metajavbots.pantalla;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import metajavbots.api.Avistado;
import metajavbots.api.Cosa;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.textos.T;

/**
 * VISOR TÁCTICO — Capa semitransparente sobre la arena (activable con F8 o con el botón de la barra) que muestra en
 * tiempo real lo que «ve» la API del robot del jugador: cono del radar (cambia de tono al detectar), círculos punteados
 * con el alcance real de cada brazo, línea discontinua con distancia y arco del ángulo relativo hacia el enemigo
 * visible o marcado, y aros/zonas de peligro de los obstáculos de la arena. Solo lee el estado; nunca altera la pelea.
 */
public final class VisorTactico {

    private static final int COLOR_RADAR = 0x3FE0FF;
    private static final int COLOR_OBJETIVO = 0xFFE27A;
    private static final int COLOR_IZQ = 0x5BD685;
    private static final int COLOR_DER = 0x3FA9F5;
    private static final int COLOR_PELIGRO = 0xFF5252;
    private static final int COLOR_SOLIDO = 0x8FD8FF;
    private static final int COLOR_OBSTACULO = 0xB8C4D4;
    private static final int FONDO_ETIQUETA = 0x0B0F18;

    private float deteccion;

    /** Enemigo apuntado por el visor (el marcado o el más cercano visible en el radar), con medidas de la API. */
    public static final class Objetivo {
        public final double x, y, distancia, anguloAbs, anguloRel;
        public final boolean visible;

        Objetivo(double x, double y, double distancia, double anguloAbs, double anguloRel, boolean visible) {
            this.x = x;
            this.y = y;
            this.distancia = distancia;
            this.anguloAbs = anguloAbs;
            this.anguloRel = anguloRel;
            this.visible = visible;
        }
    }

    /** Foto de solo lectura de los sensores y medidas de la API del robot del jugador en este fotograma. */
    public static final class Datos {
        public final double x, y, mira, radioRadar, conoRadar, alcanceIzq, alcanceDer;
        public final boolean detecta;
        public final Objetivo objetivo;
        public final List<? extends Cosa> obstaculos;

        Datos(double x, double y, double mira, double radioRadar, double conoRadar, double alcanceIzq, double alcanceDer, boolean detecta,
                Objetivo objetivo, List<? extends Cosa> obstaculos) {
            this.x = x;
            this.y = y;
            this.mira = mira;
            this.radioRadar = radioRadar;
            this.conoRadar = conoRadar;
            this.alcanceIzq = alcanceIzq;
            this.alcanceDer = alcanceDer;
            this.detecta = detecta;
            this.objetivo = objetivo;
            this.obstaculos = obstaculos;
        }
    }

    /** Lee el estado actual del robot del jugador sin modificar nada de la pelea. Devuelve null si el jugador está fuera. */
    public static Datos leer(Pelea pelea, int marcado) {
        if (pelea == null || pelea.combatientes.length == 0) return null;
        Cuerpo yo = pelea.cuerpo(0);
        if (yo == null || yo.fuera) return null;
        Pieza cabeza = yo.combatiente.montaje.pieza(Montaje.Hueco.CABEZA);
        double radioRadar = (cabeza == null ? 400 : cabeza.valor("radar", 400)) * yo.efectosPoo().factorRadarPropio;
        double conoRadar = cabeza == null ? 50 : cabeza.valor("cono", 50);
        if (yo.parteRota(Montaje.CABEZA)) radioRadar *= 0.4;
        List<? extends Avistado> vistos = yo.vistos();
        boolean detecta = !vistos.isEmpty();
        Objetivo objetivo = elegirObjetivo(pelea, yo, vistos, marcado);
        return new Datos(yo.x, yo.y, yo.mira(), radioRadar, conoRadar, yo.alcance(Cuerpo.IZQUIERDA), yo.alcance(Cuerpo.DERECHA), detecta, objetivo,
                yo.obstaculos());
    }

    private static Objetivo elegirObjetivo(Pelea pelea, Cuerpo yo, List<? extends Avistado> vistos, int marcado) {
        Cuerpo m = marcado >= 0 && marcado < pelea.combatientes.length ? pelea.cuerpo(marcado) : null;
        if (m != null && (m.fuera || m.equipo == yo.equipo)) m = null;
        if (m != null) {
            for (Avistado a : vistos) {
                if (a.nombre().equals(m.combatiente.nombre)) {
                    return new Objetivo(a.x(), a.y(), a.distancia(), a.angulo(), Pelea.normal(a.angulo() - yo.mira()), true);
                }
            }
        }
        if (!vistos.isEmpty()) {
            Avistado a = vistos.get(0);
            return new Objetivo(a.x(), a.y(), a.distancia(), a.angulo(), Pelea.normal(a.angulo() - yo.mira()), true);
        }
        if (m != null) {
            double dist = Math.hypot(m.x - yo.x, m.y - yo.y);
            double ang = Math.toDegrees(Math.atan2(m.y - yo.y, m.x - yo.x));
            return new Objetivo(m.x, m.y, dist, ang, Pelea.normal(ang - yo.mira()), false);
        }
        return null;
    }

    /** Capa de suelo: obstáculos, cono del radar, círculos de alcance y línea/arco al objetivo (debajo de los robots). */
    void dibujarSuelo(Pincel p, VistaDeArena v, Pelea pelea, int marcado, float dt) {
        Datos d = leer(pelea, marcado);
        if (d == null) return;
        float destino = d.detecta ? 1f : 0f;
        deteccion = dt <= 0 ? destino : deteccion + (destino - deteccion) * Math.min(1f, dt * 8f);
        dibujarObstaculos(p, v, d.obstaculos);
        dibujarRadar(p, v, d, pelea.ancho, pelea.alto);
        dibujarAlcances(p, v, d);
        if (d.objetivo != null) dibujarObjetivo(p, v, d, d.objetivo);
    }

    /** Capa de etiquetas mínimas: alcances y distancia/ángulo al objetivo (debajo de bocadillos y HUD). */
    void dibujarEtiquetas(Pincel p, VistaDeArena v, Pelea pelea, int marcado) {
        Datos d = leer(pelea, marcado);
        if (d == null) return;
        float tl = tamanoLetra(v.letra);
        List<float[]> puestas = new ArrayList<>();
        if (d.objetivo != null) {
            Objetivo o = d.objetivo;
            String txt = String.format(Locale.ROOT, "%.0f · %+.0f°", o.distancia, o.anguloRel);
            float x1 = v.sx(d.x, d.y), y1 = v.sy(d.y), x2 = v.sx(o.x, o.y), y2 = v.sy(o.y);
            float dx = x2 - x1, dy = y2 - y1, len = (float) Math.hypot(dx, dy);
            float nx = len > 1 ? -dy / len * 18 : 0, ny = len > 1 ? dx / len * 18 : -18;
            if (ny > 0) {
                nx = -nx;
                ny = -ny;
            }
            etiqueta(p, v, txt, (x1 + x2) / 2 + nx, (y1 + y2) / 2 + ny, tl, COLOR_OBJETIVO, puestas);
        }
        etiquetasDeAlcance(p, v, d, pelea.ancho, pelea.alto, tl, puestas);
    }

    /** Tamaño de letra acotado para las etiquetas del visor (legible y sin tapar textos ni con -Pletra=26). */
    public static float tamanoLetra(float letraBase) {
        return Math.max(Pincel.LETRA_MINIMA, Math.min(18.5f, letraBase * 0.74f));
    }

    /** Encaja una etiqueta de w × h dentro del rectángulo útil de la arena (sin pisar marcador, locutor ni barra). */
    public static float[] encajarEtiqueta(float x, float y, float w, float h, float ax, float ay, float aw, float ah, float techo) {
        float minX = ax + 10, maxX = Math.max(minX, ax + aw - w - 10);
        float minY = Math.max(ay + 10, techo + 4);
        float maxY = Math.max(minY, ay + ah - 84 - h);
        return new float[]{Math.max(minX, Math.min(maxX, x)), Math.max(minY, Math.min(maxY, y))};
    }

    private void dibujarObstaculos(Pincel p, VistaDeArena v, List<? extends Cosa> obstaculos) {
        for (Cosa o : obstaculos) {
            if (o.radio() <= 0) continue;
            if (o.peligroso()) {
                circuloRelleno(p, v, o.x(), o.y(), o.radio(), COLOR_PELIGRO, 0.14f);
                aroEnSuelo(p, v, o.x(), o.y(), o.radio(), 2.1f, COLOR_PELIGRO, 0.72f);
                aroPunteado(p, v, o.x(), o.y(), o.radio() * 0.72, 1.4f, 0xFF9B9B, 0.42f, 16, 0);
            } else {
                aroEnSuelo(p, v, o.x(), o.y(), o.radio(), 1.6f, o.solido() ? COLOR_SOLIDO : COLOR_OBSTACULO, 0.42f);
            }
        }
    }

    private void dibujarRadar(Pincel p, VistaDeArena v, Datos d, double ancho, double alto) {
        int col = Pincel.mezcla(COLOR_RADAR, COLOR_OBJETIVO, deteccion);
        float alfaRelleno = 0.09f + 0.08f * deteccion;
        float alfaBorde = 0.38f + 0.26f * deteccion;
        int pasos = 22;
        double inicio = d.mira - d.conoRadar / 2;
        float ox = v.sx(d.x, d.y), oy = v.sy(d.y);
        float prevX = 0, prevY = 0;
        for (int i = 0; i <= pasos; i++) {
            double ang = Math.toRadians(inicio + d.conoRadar * i / pasos);
            double[] fin = recortarRayo(d.x, d.y, Math.cos(ang), Math.sin(ang), d.radioRadar, ancho, alto);
            float sx = v.sx(fin[0], fin[1]), sy = v.sy(fin[1]);
            if (i > 0) {
                p.triangulo(ox, oy, prevX, prevY, sx, sy, col, alfaRelleno);
                p.linea(prevX, prevY, sx, sy, 1.7f, col, alfaBorde);
            } else {
                p.linea(ox, oy, sx, sy, 1.7f, col, alfaBorde);
            }
            if (i == pasos) p.linea(ox, oy, sx, sy, 1.7f, col, alfaBorde);
            prevX = sx;
            prevY = sy;
        }
        double radMira = Math.toRadians(d.mira);
        double[] eje = recortarRayo(d.x, d.y, Math.cos(radMira), Math.sin(radMira), Math.min(d.radioRadar, 150), ancho, alto);
        p.linea(ox, oy, v.sx(eje[0], eje[1]), v.sy(eje[1]), 1.4f, col, 0.28f);
    }

    private void dibujarAlcances(Pincel p, VistaDeArena v, Datos d) {
        if (d.alcanceIzq > 0 && d.alcanceDer > 0 && Math.abs(d.alcanceIzq - d.alcanceDer) < 1) {
            aroPunteado(p, v, d.x, d.y, d.alcanceIzq, 1.8f, COLOR_IZQ, 0.56f, 44, 0);
            return;
        }
        if (d.alcanceIzq > 0) aroPunteado(p, v, d.x, d.y, d.alcanceIzq, 1.8f, COLOR_IZQ, 0.56f, 44, 0);
        if (d.alcanceDer > 0) aroPunteado(p, v, d.x, d.y, d.alcanceDer, 1.8f, COLOR_DER, 0.56f, 44, 0.5);
    }

    private void dibujarObjetivo(Pincel p, VistaDeArena v, Datos d, Objetivo o) {
        float alfa = o.visible ? 0.85f : 0.58f;
        float x1 = v.sx(d.x, d.y), y1 = v.sy(d.y), x2 = v.sx(o.x, o.y), y2 = v.sy(o.y);
        lineaDiscontinua(p, x1, y1, x2, y2, 2.3f, COLOR_OBJETIVO, alfa, 12, 8);
        aroEnSuelo(p, v, o.x, o.y, 28, 1.8f, COLOR_OBJETIVO, alfa * 0.7f);
        double radioArco = Math.min(82, Math.max(44, o.distancia * 0.32));
        double radMira = Math.toRadians(d.mira);
        p.linea(x1, y1, v.sx(d.x + Math.cos(radMira) * radioArco, d.y + Math.sin(radMira) * radioArco),
                v.sy(d.y + Math.sin(radMira) * radioArco), 1.6f, COLOR_OBJETIVO, alfa * 0.6f);
        int pasos = Math.max(4, (int) Math.ceil(Math.abs(o.anguloRel) / 10));
        for (int k = 0; k < pasos; k++) {
            double a = Math.toRadians(d.mira + o.anguloRel * k / pasos);
            double b = Math.toRadians(d.mira + o.anguloRel * (k + 1) / pasos);
            double ax = d.x + Math.cos(a) * radioArco, ay = d.y + Math.sin(a) * radioArco;
            double bx = d.x + Math.cos(b) * radioArco, by = d.y + Math.sin(b) * radioArco;
            p.linea(v.sx(ax, ay), v.sy(ay), v.sx(bx, by), v.sy(by), 2.2f, COLOR_OBJETIVO, alfa);
        }
    }

    private void etiquetasDeAlcance(Pincel p, VistaDeArena v, Datos d, double ancho, double alto, float tl, List<float[]> puestas) {
        if (d.alcanceIzq > 0 && d.alcanceDer > 0 && Math.abs(d.alcanceIzq - d.alcanceDer) < 1) {
            String txt = T.t("visor.izq") + "/" + T.t("visor.der") + " " + Math.round(d.alcanceIzq);
            double[] pt = puntoEnCirculo(d.x, d.y, d.alcanceIzq, ancho, alto, 155, 200, 120, 40, 25);
            etiqueta(p, v, txt, v.sx(pt[0], pt[1]), v.sy(pt[1]), tl, COLOR_IZQ, puestas);
            return;
        }
        if (d.alcanceIzq > 0) {
            String txt = T.t("visor.izq") + " " + Math.round(d.alcanceIzq);
            double[] pt = puntoEnCirculo(d.x, d.y, d.alcanceIzq, ancho, alto, 155, 200, 115, 25, 240);
            etiqueta(p, v, txt, v.sx(pt[0], pt[1]), v.sy(pt[1]), tl, COLOR_IZQ, puestas);
        }
        if (d.alcanceDer > 0) {
            String txt = T.t("visor.der") + " " + Math.round(d.alcanceDer);
            double[] pt = puntoEnCirculo(d.x, d.y, d.alcanceDer, ancho, alto, 30, 340, 65, 145, 225);
            etiqueta(p, v, txt, v.sx(pt[0], pt[1]), v.sy(pt[1]), tl, COLOR_DER, puestas);
        }
    }

    private static double[] puntoEnCirculo(double cx, double cy, double r, double ancho, double alto, double... angulos) {
        double margen = 28;
        for (double g : angulos) {
            double rad = Math.toRadians(g);
            double px = cx + Math.cos(rad) * r, py = cy + Math.sin(rad) * r;
            boolean enEsquinaEntrenador = px > ancho * 0.62 && py < alto * 0.34;
            if (!enEsquinaEntrenador && px >= margen && px <= ancho - margen && py >= margen && py <= alto - margen) return new double[]{px, py};
        }
        double rad = Math.toRadians(angulos[0]);
        return recortarRayo(cx, cy, Math.cos(rad), Math.sin(rad), r, ancho - margen, alto - margen);
    }

    /** Recorta un rayo que sale de (ox, oy) en dirección (dx, dy) y longitud r al rectángulo [0, ancho] × [0, alto]. */
    private static double[] recortarRayo(double ox, double oy, double dx, double dy, double r, double ancho, double alto) {
        double t = r;
        if (dx > 1e-9) t = Math.min(t, (ancho - ox) / dx);
        else if (dx < -1e-9) t = Math.min(t, -ox / dx);
        if (dy > 1e-9) t = Math.min(t, (alto - oy) / dy);
        else if (dy < -1e-9) t = Math.min(t, -oy / dy);
        t = Math.max(0, t);
        return new double[]{ox + dx * t, oy + dy * t};
    }

    private static void etiqueta(Pincel p, VistaDeArena v, String texto, float cx, float cy, float tl, int color, List<float[]> puestas) {
        float tw = p.ancho(texto, tl, true), w = tw + 12, h = tl * 1.38f;
        float[] pos = encajarEtiqueta(cx - w / 2, cy - h / 2, w, h, v.ax(), v.ay(), v.aw(), v.ah(), v.techo);
        for (int intento = 0; intento < 6 && choca(pos[0], pos[1], w, h, puestas); intento++) {
            pos = encajarEtiqueta(pos[0], pos[1] - h - 4, w, h, v.ax(), v.ay(), v.aw(), v.ah(), v.techo);
        }
        puestas.add(new float[]{pos[0], pos[1], w, h});
        p.caja(pos[0], pos[1], w, h, 5, FONDO_ETIQUETA, 0.8f);
        p.texto(texto, pos[0] + 6, pos[1] + 2, tl, true, color, 0.95f);
    }

    private static boolean choca(float x, float y, float w, float h, List<float[]> puestas) {
        for (float[] r : puestas) {
            if (x < r[0] + r[2] + 4 && x + w + 4 > r[0] && y < r[1] + r[3] + 4 && y + h + 4 > r[1]) return true;
        }
        return false;
    }

    private static void circuloRelleno(Pincel p, VistaDeArena v, double cx, double cy, double r, int color, float alfa) {
        int pasos = 24;
        float ox = v.sx(cx, cy), oy = v.sy(cy);
        float px = v.sx(cx + r, cy), py = v.sy(cy);
        for (int i = 1; i <= pasos; i++) {
            double a = 2 * Math.PI * i / pasos;
            double wx = cx + Math.cos(a) * r, wy = cy + Math.sin(a) * r;
            float nx = v.sx(wx, wy), ny = v.sy(wy);
            p.triangulo(ox, oy, px, py, nx, ny, color, alfa);
            px = nx;
            py = ny;
        }
    }

    private static void aroEnSuelo(Pincel p, VistaDeArena v, double cx, double cy, double r, float grosor, int color, float alfa) {
        int pasos = 28;
        float px = v.sx(cx + r, cy), py = v.sy(cy);
        for (int i = 1; i <= pasos; i++) {
            double a = 2 * Math.PI * i / pasos;
            double wx = cx + Math.cos(a) * r, wy = cy + Math.sin(a) * r;
            float nx = v.sx(wx, wy), ny = v.sy(wy);
            p.linea(px, py, nx, ny, grosor, color, alfa);
            px = nx;
            py = ny;
        }
    }

    private static void aroPunteado(Pincel p, VistaDeArena v, double cx, double cy, double r, float grosor, int color, float alfa, int trazos, double fase) {
        double paso = 2 * Math.PI / trazos, ancho = v.anchoArena(), alto = v.altoArena();
        for (int i = 0; i < trazos; i++) {
            double a = (i + fase) * paso, b = a + paso * 0.52;
            double x1 = cx + Math.cos(a) * r, y1 = cy + Math.sin(a) * r;
            double x2 = cx + Math.cos(b) * r, y2 = cy + Math.sin(b) * r;
            if (x1 < 0 || x1 > ancho || y1 < 0 || y1 > alto || x2 < 0 || x2 > ancho || y2 < 0 || y2 > alto) continue;
            p.linea(v.sx(x1, y1), v.sy(y1), v.sx(x2, y2), v.sy(y2), grosor, color, alfa);
        }
    }

    private static void lineaDiscontinua(Pincel p, float x1, float y1, float x2, float y2, float grosor, int color, float alfa, float trazo, float hueco) {
        float dx = x2 - x1, dy = y2 - y1, largo = (float) Math.hypot(dx, dy);
        if (largo < 1) return;
        float ux = dx / largo, uy = dy / largo, paso = trazo + hueco;
        for (float d = 0; d < largo; d += paso) {
            float fin = Math.min(largo, d + trazo);
            p.linea(x1 + ux * d, y1 + uy * d, x1 + ux * fin, y1 + uy * fin, grosor, color, alfa);
        }
    }
}
