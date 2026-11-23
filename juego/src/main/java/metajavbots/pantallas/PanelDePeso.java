package metajavbots.pantallas;

import java.util.Locale;

import metajavbots.Juego;
import metajavbots.combate.Terrenos;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.piezas.Equilibrio;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.textos.T;

/**
 * PANEL DE PESO — Lo que el jugador ve del peso frente a la carga (docs/EQUILIBRIO.md),
 * sobrio y claro: una barra con zona verde (ligero), amarilla (normal) y roja
 * (sobrecarga), con la raya del 120 % a partir de la cual no se puede pelear;
 * la velocidad, la aceleración y el giro que resultan; y en qué terreno rinden
 * mejor y peor sus piernas. Si se le pasa un montaje «previa» (lo que quedaría
 * al montar o quitar una pieza, en el Taller o en la Tienda), marca también
 * dónde quedaría y cómo cambian las cifras. Lo usan el Taller, la pausa entre
 * rondas, la Tienda y la preparación de la pelea.
 */
public final class PanelDePeso {

    /** Hasta dónde llega la barra (en r): un poco más allá del 120 %. */
    private static final float TOPE = 1.35f;
    private static final int VERDE = 0x4CC26B, AMARILLO = 0xE8C547, ROJO = 0xE0573F, ROJO_OSCURO = 0x7A1F18;

    private PanelDePeso() {
    }

    /** El color del tramo (verde, amarillo, rojo o rojo oscuro). */
    public static int color(Equilibrio.Tramo t) {
        switch (t) {
            case LIGERO: return VERDE;
            case NORMAL: return AMARILLO;
            case SOBRECARGA: return ROJO;
            default: return ROJO_OSCURO;
        }
    }

    /** El nombre del tramo en el idioma del jugador. */
    public static String tramo(Equilibrio.Tramo t) {
        return T.t("peso.tramo." + t.name().toLowerCase(Locale.ROOT));
    }

    /** Las cifras de peso y movilidad, con la barra; devuelve lo que ocupa de alto. Con previa, cómo quedaría. */
    public static float dibujar(Juego j, Montaje m, Montaje previa, String quePrevia, float x, float y, float w) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Montaje.Cifras cf = m.cifras();
        float tam = ui.letra * 0.82f, cy = y;
        String titulo = T.f("peso.titulo", Math.round(cf.pesoTotal), Math.round(cf.carga), Math.round(cf.proporcion * 100), tramo(cf.tramo));
        cy += p.parrafo(titulo, x, cy, w, tam, true, color(cf.tramo), 1) + 6;
        Montaje.Cifras cp = previa == null ? null : previa.cifras();
        cy += barra(p, cf, cp, x, cy, w) + 8;
        cy += p.parrafo(T.f("peso.movilidad", dec(cf.velocidad), dec2(cf.aceleracion), dec(cf.giro)), x, cy, w, tam, ui.tema.texto, 1) + 4;
        if (cp != null) {
            String cambio = T.f("peso.cambio", quePrevia == null ? "" : quePrevia, Math.round(cp.pesoTotal), Math.round(cp.proporcion * 100),
                    dec(cp.velocidad), dec(cp.giro));
            cy += p.parrafo(cambio, x, cy, w, tam, color(cp.tramo), 1) + 4;
        }
        cy += p.parrafo(terrenos(cf.rasgos), x, cy, w, tam * 0.95f, ui.tema.textoSuave, 1) + 4;
        if (cf.tramo == Equilibrio.Tramo.IMPOSIBLE) {
            cy += p.parrafo(T.f("peso.aviso_imposible", queQuitar(m)), x, cy, w, tam, true, ROJO, 1) + 4;
        } else if (cf.tramo == Equilibrio.Tramo.SOBRECARGA) {
            cy += p.parrafo(T.t("peso.aviso_sobrecarga"), x, cy, w, tam, ROJO, 1) + 4;
        }
        return cy - y;
    }

    /** La barra de peso frente a carga: zonas por tramo, la raya del 120 %, la marca de ahora y la de la previa. */
    private static float barra(Pincel p, Montaje.Cifras cf, Montaje.Cifras cp, float x, float y, float w) {
        Equilibrio eq = Equilibrio.delJuego();
        float h = 14;
        float xl = x + w * (float) (eq.ligero / TOPE), xn = x + w * (float) (eq.limite / TOPE), xm = x + w * (float) (eq.maximo / TOPE);
        p.caja(x, y, w, h, 5, 0x15171C, 1);
        p.rect(x + 2, y + 2, xl - x - 2, h - 4, VERDE, 0.55f);
        p.rect(xl, y + 2, xn - xl, h - 4, AMARILLO, 0.55f);
        p.rect(xn, y + 2, xm - xn, h - 4, ROJO, 0.6f);
        p.rect(xm, y + 2, x + w - 2 - xm, h - 4, ROJO_OSCURO, 0.7f);
        p.linea(xm, y - 3, xm, y + h + 3, 2, 0xFFFFFF, 0.9f);
        marca(p, cf.proporcion, x, y, w, h, 0xFFFFFF, 1);
        if (cp != null) marca(p, cp.proporcion, x, y, w, h, color(cp.tramo), 0.95f);
        return h;
    }

    private static void marca(Pincel p, double r, float x, float y, float w, float h, int color, float alfa) {
        float mx = x + w * (float) Math.max(0, Math.min(1, r / TOPE));
        p.rect(mx - 2, y - 4, 4, h + 8, 0x000000, 0.6f * alfa);
        p.rect(mx - 1.5f, y - 3, 3, h + 6, color, alfa);
    }

    /** «Orugas: mejor en …, peor en …» (o «igual en todos los terrenos»). */
    public static String terrenos(Equilibrio.Rasgos ra) {
        String loco = ra.nombre.en(T.idioma);
        if (ra.igualEnTodos()) return T.f("peso.terreno_igual", loco);
        return T.f("peso.terreno", loco, ra.mejor().nombre(T.idioma), ra.peor().nombre(T.idioma));
    }

    /** Lo que más pesa, para proponer qué quitar o cambiar («Brazo derecho, Cabeza»). */
    static String queQuitar(Montaje m) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (Montaje.Hueco h : m.huecosQueMasPesan()) {
            if (n++ >= 2) break;
            Pieza pz = m.pieza(h);
            if (b.length() > 0) b.append(", ");
            b.append(T.t("hueco." + h.name().toLowerCase(Locale.ROOT))).append(" (").append(pz.nombre(T.idioma)).append(", ")
                    .append(Math.round(pz.valor("peso"))).append(')');
        }
        return b.toString();
    }

    /** Los terrenos de una arena para la preparación de la pelea: «Escombros 44 % · Llano 48 % …» y quién brilla y quién sufre. */
    public static String arena(String escenario) {
        StringBuilder b = new StringBuilder();
        java.util.List<java.util.Map.Entry<Equilibrio.Terreno, Double>> l = new java.util.ArrayList<>(Terrenos.reparto(escenario).entrySet());
        l.sort((a, c) -> Double.compare(c.getValue(), a.getValue()));
        for (java.util.Map.Entry<Equilibrio.Terreno, Double> e : l) {
            long pct = Math.round(e.getValue() * 100);
            if (pct < 1) continue;
            if (b.length() > 0) b.append(" · ");
            b.append(e.getKey().nombre(T.idioma)).append(' ').append(pct).append(" %");
        }
        Equilibrio eq = Equilibrio.delJuego();
        return T.f("peso.arena", b, eq.rasgos(Terrenos.mejorEn(escenario)).nombre.en(T.idioma),
                eq.rasgos(Terrenos.peorEn(escenario)).nombre.en(T.idioma));
    }

    private static String dec(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String dec2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
