package metajavbots.piezas;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MONTAJE — Un robot montado: su modelo y la pieza de cada hueco. Sabe sacar
 * las cifras con que pelea (Cifras): blindaje de cada parte, velocidad,
 * pasos de cálculo, energía y los refuerzos de su naturaleza.
 */
public final class Montaje {

    /** Los huecos del robot. */
    public enum Hueco {
        CABEZA(Pieza.Ranura.CABEZA), IZQUIERDA(Pieza.Ranura.BRAZO), DERECHA(Pieza.Ranura.BRAZO),
        PIERNAS(Pieza.Ranura.PIERNAS), NUCLEO(Pieza.Ranura.NUCLEO), NATURALEZA(Pieza.Ranura.NATURALEZA);

        public final Pieza.Ranura ranura;

        Hueco(Pieza.Ranura ranura) {
            this.ranura = ranura;
        }
    }

    /** Las partes que reciben golpes. */
    public static final int CABEZA = 0, IZQUIERDA = 1, DERECHA = 2, PIERNAS = 3;
    public static final String[] PARTES = {"cabeza", "izquierda", "derecha", "piernas"};

    public final Modelo modelo;
    private final EnumMap<Hueco, Pieza> piezas = new EnumMap<>(Hueco.class);

    public Montaje(Modelo modelo) {
        this.modelo = modelo;
        CatalogoDePiezas c = CatalogoDePiezas.delJuego();
        Hueco[] orden = {Hueco.CABEZA, Hueco.IZQUIERDA, Hueco.DERECHA, Hueco.PIERNAS, Hueco.NUCLEO};
        for (int i = 0; i < orden.length && i < modelo.piezas.size(); i++) {
            Pieza p = c.pieza(modelo.piezas.get(i));
            if (p != null && p.ranura == orden[i].ranura) piezas.put(orden[i], p);
        }
    }

    public Montaje copia() {
        Montaje m = new Montaje(modelo);
        m.piezas.clear();
        m.piezas.putAll(piezas);
        return m;
    }

    public Pieza pieza(Hueco h) {
        return piezas.get(h);
    }

    /** Pone una pieza en su hueco (si la ranura no casa, no hace nada y devuelve false). */
    public boolean poner(Hueco h, Pieza p) {
        if (p == null) {
            if (h == Hueco.NATURALEZA) piezas.remove(h);
            return h == Hueco.NATURALEZA;
        }
        if (p.ranura != h.ranura) return false;
        piezas.put(h, p);
        return true;
    }

    public Map<Hueco, Pieza> piezas() {
        return new LinkedHashMap<>(piezas);
    }

    /** Lo que pesa todo. */
    public double peso() {
        double p = 0;
        for (Pieza x : piezas.values()) p += x.valor("peso");
        return p;
    }

    /** Cuánto refuerza la naturaleza este aspecto ("blindaje", "velocidad", "precision", "cuerpo", "pasos", "esquiva"). */
    public double refuerzo(String aspecto) {
        double r = 0;
        // Lo propio de cada modelo: un 8 % en lo suyo, de fábrica.
        if (modelo.naturaleza.refuerza.equals(aspecto)) r += 0.08;
        Pieza n = piezas.get(Hueco.NATURALEZA);
        if (n != null && n.naturaleza.refuerza.equals(aspecto)) {
            r += n.valor("refuerzo") * (n.naturaleza == modelo.naturaleza ? 1.0 : 0.5);
        }
        // Cada pieza de la misma naturaleza que el modelo suma un poco.
        for (Map.Entry<Hueco, Pieza> e : piezas.entrySet()) {
            if (e.getKey() != Hueco.NATURALEZA && e.getValue().naturaleza == modelo.naturaleza
                    && modelo.naturaleza.refuerza.equals(aspecto)) r += 0.015;
        }
        return r;
    }

    /** Las cifras con que pelea. */
    public Cifras cifras() {
        return new Cifras(this);
    }

    /** Las cifras de pelea de un montaje (se calculan una vez al empezar cada ronda). */
    public static final class Cifras {
        public final double[] blindaje = new double[4];
        public final double velocidad, giro, aceleracion, pasos, energiaMax, recarga, precision, cuerpo, esquiva;
        public final boolean vuela;
        public final Pieza.Locomocion locomocion;

        Cifras(Montaje m) {
            double refBlindaje = 1 + m.refuerzo("blindaje");
            double base = m.modelo.blindaje;
            Pieza cab = m.pieza(Hueco.CABEZA), izq = m.pieza(Hueco.IZQUIERDA), der = m.pieza(Hueco.DERECHA);
            Pieza pie = m.pieza(Hueco.PIERNAS), nuc = m.pieza(Hueco.NUCLEO);
            double extra = nuc == null ? 0 : nuc.valor("blindaje") / 4;
            blindaje[CABEZA] = (40 * base + v(cab, "blindaje") + extra) * refBlindaje;
            blindaje[IZQUIERDA] = (35 * base + v(izq, "blindaje") + extra) * refBlindaje;
            blindaje[DERECHA] = (35 * base + v(der, "blindaje") + extra) * refBlindaje;
            blindaje[PIERNAS] = (45 * base + v(pie, "blindaje") + extra) * refBlindaje;
            double carga = pie == null ? 30 : pie.valor("carga", 30);
            double peso = m.peso();
            double porPeso = peso <= carga ? 1 : Math.max(0.5, 1 - (peso - carga) / carga * 0.6);
            locomocion = pie == null ? Pieza.Locomocion.BIPEDO : pie.locomocion();
            vuela = locomocion == Pieza.Locomocion.VUELO;
            velocidad = (pie == null ? 4 : pie.valor("velocidad", 4)) * m.modelo.velocidad * porPeso * (1 + m.refuerzo("velocidad"));
            giro = (pie == null ? 6 : pie.valor("giro", 6)) * (0.8 + 0.2 * porPeso);
            aceleracion = (pie == null ? 0.5 : pie.valor("aceleracion", 0.5)) * porPeso;
            pasos = (nuc == null ? 3000 : nuc.valor("pasos", 3000)) * (1 + 2 * m.refuerzo("pasos"));
            energiaMax = nuc == null ? 100 : nuc.valor("energiaMax", 100);
            recarga = (nuc == null ? 0.4 : nuc.valor("recarga", 0.4)) * (1 + m.refuerzo("velocidad") / 2);
            precision = 1 + m.refuerzo("precision");
            cuerpo = 1 + m.refuerzo("cuerpo");
            esquiva = Math.min(0.35, m.refuerzo("esquiva") * 1.2 + (vuela ? 0.06 : 0));
        }

        public double blindajeTotal() {
            return blindaje[0] + blindaje[1] + blindaje[2] + blindaje[3];
        }

        private static double v(Pieza p, String clave) {
            return p == null ? 0 : p.valor(clave);
        }
    }
}
