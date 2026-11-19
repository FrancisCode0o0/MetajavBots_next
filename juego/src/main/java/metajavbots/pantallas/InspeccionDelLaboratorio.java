package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.ejercicios.Laboratorio;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * INSPECCIÓN DEL LABORATORIO — Lo que hacen las pruebas del jugador, a la
 * vista: un inspector con lupa en su escáner y una fila de robots que
 * pasan de uno en uno. Primero la clase buena (con su corona); detrás, los
 * mutantes, disfrazados con bigote postizo y gafas de sol. Cada prueba es
 * un rayo del escáner (verde si pasa, rojo si falla). La clase buena, si
 * pasan todas, cruza la puerta; si alguna falla, el inspector la achicharra
 * por error («¡ERA DE LOS BUENOS!»). Un mutante cazado cae por la trampilla
 * y pierde el bigote; uno que se escapa cruza de puntillas, riéndose.
 */
final class InspeccionDelLaboratorio {

    private InspeccionDelLaboratorio() {
    }

    /** Lo que tarda cada robot en pasar por el escáner (s). */
    static final float TURNO = 2.0f;
    private static final int VERDE = AnimacionDelCaso.VERDE, ROJO = AnimacionDelCaso.ROJO;

    /** Dibuja la inspección: t, los segundos desde que llegó el resultado (da la vuelta al acabar la fila). */
    static void dibujar(Ui ui, float x, float y, float w, float h, Laboratorio.Resultado r, int mutantes, float t) {
        Pincel p = ui.p;
        p.caja(x, y, w, h, 10, 0x14171C, 1);
        p.recortar(x, y, w, h);
        float suelo = y + h - 18;
        p.rect(x, suelo, w, 18, 0x3A3F46, 1);
        float escaner = x + w * 0.5f;
        // El escáner: un arco con su luz y la trampilla en el suelo.
        p.rect(escaner - 46, suelo - 120, 8, 120, 0x6E7681, 1);
        p.rect(escaner + 38, suelo - 120, 8, 120, 0x6E7681, 1);
        p.rect(escaner - 46, suelo - 124, 92, 10, 0x6E7681, 1);
        List<Boolean> pruebas = new ArrayList<>();
        for (String v : r.pruebas.values()) pruebas.add(v.isEmpty());
        boolean sinPruebas = pruebas.isEmpty();
        inspector(p, escaner + 92, suelo, t, sinPruebas);
        if (sinPruebas) {
            p.texto("Zzz", escaner + 70, suelo - 140 - (t * 10) % 20, ui.letra, true, 0x8A919C, 1);
            p.sinRecorte();
            return;
        }
        int total = 1 + mutantes;
        float ciclo = (t / TURNO) % (total + 0.5f);
        int quien = (int) ciclo;
        float u = ciclo - quien;
        // Los que esperan su turno, en fila a la izquierda.
        for (int k = quien + 1; k < total && k < quien + 4; k++) robot(p, escaner - 120 - (k - quien) * 52, suelo, k, 0, t + k);
        if (quien < total) turno(ui, quien, escaner, suelo, u, pruebas, r, t, x + w);
        p.sinRecorte();
    }

    /** El turno de un robot: entra, lo escanean (un rayo por prueba) y su final. */
    private static void turno(Ui ui, int quien, float escaner, float suelo, float u, List<Boolean> pruebas, Laboratorio.Resultado r, float t,
            float derecha) {
        Pincel p = ui.p;
        boolean bueno = quien == 0;
        boolean cazado = !bueno && quien - 1 < r.cazados.size() && r.cazados.get(quien - 1);
        boolean pasan = r.pasanTodas();
        float rx;
        if (u < 0.3f) rx = escaner - 120 + 120 * (u / 0.3f);
        else rx = escaner;
        float caida = 0;
        if (u >= 0.3f && u < 0.65f) {
            // Un rayo por prueba, uno tras otro.
            int n = pruebas.size();
            int k = Math.min(n - 1, (int) ((u - 0.3f) / 0.35f * n));
            boolean ok = bueno ? pruebas.get(k) : !cazado || k != n - 1;
            float by = suelo - 110 + ((u * 40) % 1) * 90;
            p.rect(escaner - 38, by, 76, 4, ok ? VERDE : ROJO, 0.85f);
            p.brillo(escaner, by, 40, ok ? VERDE : ROJO, 0.3f);
        }
        if (u >= 0.65f) {
            float v = (u - 0.65f) / 0.35f;
            if (bueno && pasan) {
                rx = escaner + v * (derecha - escaner);
                bocadillo(ui, "✔", escaner, suelo - 150, VERDE);
            } else if (bueno) {
                // Achicharrado por error: tiembla, negro de hollín.
                rx = escaner + (float) Math.sin(t * 60) * 3;
                bocadillo(ui, T.t("laboratorio.era_bueno"), escaner, suelo - 150, ROJO);
            } else if (cazado) {
                // Se abre la trampilla y cae; el bigote se queda flotando.
                p.rect(escaner - 30, suelo, 60, 18, 0x050608, 1);
                caida = v * v * 120;
                p.texto("~~", escaner - 12, suelo - 70 - v * 30, 22, true, 0x111111, 1 - v);
                bocadillo(ui, T.t("laboratorio.pillado"), escaner, suelo - 150, VERDE);
            } else {
                rx = escaner + v * (derecha - escaner);
                p.texto(T.idioma.elegir("je je", "hehe", "hehe"), rx - 18, suelo - 120 - (float) Math.abs(Math.sin(t * 8)) * 6, 16, true, 0xFFD23F, 1);
                bocadillo(ui, T.t("laboratorio.se_escapa"), escaner, suelo - 150, ROJO);
            }
        }
        if (caida < 110) robot(p, rx, suelo + caida, quien, bueno && !pasan && u >= 0.65f ? 1 : 0, t);
    }

    /** El inspector, con gorra y lupa; si no hay pruebas, dormido. */
    private static void inspector(Pincel p, float x, float suelo, float t, boolean dormido) {
        AnimacionDelCaso.robotito(p, x, suelo, dormido ? 0 : 0.5f + (float) Math.sin(t * 3) * 0.3f, dormido ? 0x3A3F46 : 0xFFD23F, 0xE8A33D);
        p.rect(x - 16, suelo - 98, 32, 6, 0x2A2F38, 1);
        p.rect(x - 10, suelo - 106, 20, 9, 0x2A2F38, 1);
        if (!dormido) {
            float lx = x - 34, ly = suelo - 70 + (float) Math.sin(t * 3) * 6;
            p.aro(lx, ly, 11, 3, 0xC9CED6, 1);
            p.circulo(lx, ly, 9, 0xBFE3FF, 0.35f);
        }
    }

    /** Un robot de la fila: el 0, la clase buena (con corona); los demás, mutantes con bigote y gafas de sol. */
    private static void robot(Pincel p, float x, float suelo, int quien, int hollin, float t) {
        int color = hollin > 0 ? 0x2A2A2A : quien == 0 ? 0x3FA9F5 : 0x9B6BD6;
        AnimacionDelCaso.robotito(p, x, suelo, (float) Math.abs(Math.sin(t * 4)) * 0.3f, quien == 0 ? VERDE : 0xFF9F43, color);
        float k = 1.6f;
        if (quien == 0) {
            float cy = suelo - 52 * k;
            p.poligono(new float[] {x - 8, cy, x - 10, cy - 10, x - 4, cy - 5, x, cy - 12, x + 4, cy - 5, x + 10, cy - 10, x + 8, cy}, 0xFFD23F, 1);
        } else {
            // Gafas de sol y bigote postizo.
            p.rect(x - 7 * k, suelo - 45.5f * k, 14 * k, 3.4f * k, 0x050608, 1);
            p.elipse(x - 3.5f * k, suelo - 39 * k, 3.5f * k, 1.4f * k, 0.3f, 0x111111, 1);
            p.elipse(x + 3.5f * k, suelo - 39 * k, 3.5f * k, 1.4f * k, -0.3f, 0x111111, 1);
        }
    }

    private static void bocadillo(Ui ui, String s, float x, float y, int color) {
        Pincel p = ui.p;
        float t = ui.letra * 0.8f, w = p.ancho(s, t, true) + 16;
        p.caja(x - w / 2, y - t * 1.5f, w, t * 1.5f, 6, 0xFFFFFF, 0.92f);
        p.textoCentrado(s, x, y - t * 1.3f, t, true, color, 1);
    }
}
