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
    /** Bonificación medida (+8 %) al equipar un arma característica del propio modelo. */
    public static final double BONO_ARMA_CARACTERISTICA = 0.08;

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

    /** Lo que pesan todas las piezas juntas, piernas incluidas (sin el chasis; lo usan los choques de los minijuegos). */
    public double peso() {
        double p = 0;
        for (Pieza x : piezas.values()) p += x.valor("peso");
        return p;
    }

    /**
     * El peso que cargan las piernas: el chasis del modelo y todas las piezas menos las piernas (cabeza, brazos con su arma,
     * escudos, espadas, núcleo, naturaleza y la pieza propia), como en Armored Core.
     */
    public double pesoTotal() {
        double p = modelo.chasis;
        for (Map.Entry<Hueco, Pieza> e : piezas.entrySet()) if (e.getKey() != Hueco.PIERNAS) p += e.getValue().valor("peso");
        return p;
    }

    /** La masa del robot entero (lo que carga más las piernas): la que cuenta en choques, empujones y retroceso. */
    public double masa() {
        Pieza pie = piezas.get(Hueco.PIERNAS);
        return pesoTotal() + (pie == null ? 0 : pie.valor("peso"));
    }

    /** Lo que aguantan las piernas (30 si no hay). */
    public double carga() {
        Pieza pie = piezas.get(Hueco.PIERNAS);
        return Math.max(1, pie == null ? 30 : pie.valor("carga", 30));
    }

    /** r = peso que cargan las piernas / su carga. */
    public double proporcionDeCarga() {
        return pesoTotal() / carga();
    }

    /** El tramo de peso del montaje (ligero, normal, sobrecarga o imposible). */
    public Equilibrio.Tramo tramo() {
        return Equilibrio.delJuego().tramo(proporcionDeCarga());
    }

    /** ¿Puede salir a pelear? (No, si pasa del 120 % de la carga de sus piernas.) */
    public boolean puedePelear() {
        return tramo() != Equilibrio.Tramo.IMPOSIBLE;
    }

    /** Lo que pesa el arma más pesada de los dos brazos (la que frena la mira). */
    public double pesoArmaMaximo() {
        Pieza i = piezas.get(Hueco.IZQUIERDA), d = piezas.get(Hueco.DERECHA);
        return Math.max(i == null ? 0 : i.valor("peso"), d == null ? 0 : d.valor("peso"));
    }

    /** Los huecos que más pesan, de más a menos (sin las piernas): lo que conviene aligerar si no puede salir. */
    public java.util.List<Hueco> huecosQueMasPesan() {
        java.util.List<Hueco> l = new java.util.ArrayList<>();
        for (Hueco h : Hueco.values()) if (h != Hueco.PIERNAS && piezas.get(h) != null) l.add(h);
        l.sort((a, b) -> Double.compare(piezas.get(b).valor("peso"), piezas.get(a).valor("peso")));
        return l;
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

    /** Indica si la pieza es un arma característica del modelo de este montaje. */
    public boolean esArmaCaracteristica(Pieza p) {
        return modelo.esArmaCaracteristica(p);
    }

    /** Bonificación de sinergia controlada de un arma al usarla en este montaje (0 si no es característica). */
    public double sinergiaArma(Pieza p) {
        return esArmaCaracteristica(p) ? BONO_ARMA_CARACTERISTICA : 0.0;
    }

    /** Cuántos brazos actualmente montados llevan un arma característica de este modelo (0, 1 o 2). */
    public int armasCaracteristicasMontadas() {
        int n = 0;
        if (esArmaCaracteristica(piezas.get(Hueco.IZQUIERDA))) n++;
        if (esArmaCaracteristica(piezas.get(Hueco.DERECHA))) n++;
        return n;
    }

    /** Las cifras con que pelea. */
    public Cifras cifras() {
        return new Cifras(this);
    }

    /** Las cifras de pelea de un montaje (se calculan una vez al empezar cada ronda). */
    public static final class Cifras {
        public final double[] blindaje = new double[4];
        public final double velocidad, giro, aceleracion, pasos, energiaMax, recarga, precision, distancia, cuerpo, esquiva;
        public final boolean vuela;
        public final Pieza.Locomocion locomocion;
        /** Peso que cargan las piernas, su carga, la proporción r, la masa total y el giro de la mira (docs/EQUILIBRIO.md). */
        public final double pesoTotal, carga, proporcion, masa, giroMira;
        /** El tramo de peso (ligero, normal, sobrecarga o imposible). */
        public final Equilibrio.Tramo tramo;
        /** Los rasgos de su locomoción (estabilidad, terrenos…). */
        public final Equilibrio.Rasgos rasgos;

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
            Equilibrio eq = Equilibrio.delJuego();
            pesoTotal = m.pesoTotal();
            carga = m.carga();
            proporcion = pesoTotal / carga;
            masa = m.masa();
            tramo = eq.tramo(proporcion);
            locomocion = pie == null ? Pieza.Locomocion.BIPEDO : pie.locomocion();
            rasgos = eq.rasgos(locomocion);
            vuela = locomocion == Pieza.Locomocion.VUELO;
            giroMira = eq.giroMira(m.pesoArmaMaximo());
            velocidad = (pie == null ? 4 : pie.valor("velocidad", 4)) * m.modelo.velocidad * eq.factorVelocidad(proporcion)
                    * (1 + m.refuerzo("velocidad"));
            giro = (pie == null ? 6 : pie.valor("giro", 6)) * eq.factorGiro(proporcion) * (0.85 + 0.15 * m.modelo.velocidad);
            aceleracion = (pie == null ? 0.5 : pie.valor("aceleracion", 0.5)) * eq.factorAceleracion(proporcion);
            pasos = (nuc == null ? 3000 : nuc.valor("pasos", 3000)) * (1 + 2 * m.refuerzo("pasos"));
            energiaMax = nuc == null ? 100 : nuc.valor("energiaMax", 100);
            recarga = (nuc == null ? 0.4 : nuc.valor("recarga", 0.4)) * (1 + m.refuerzo("velocidad") / 2);
            precision = 1 + m.refuerzo("precision");
            distancia = Math.max(0.85, baseDistancia(m.modelo.naturaleza) + m.refuerzo("precision"));
            cuerpo = Math.max(0.85, baseCuerpo(m.modelo.naturaleza) + m.refuerzo("cuerpo"));
            boolean pesado = tramo == Equilibrio.Tramo.SOBRECARGA || tramo == Equilibrio.Tramo.IMPOSIBLE;
            double ligera = tramo == Equilibrio.Tramo.LIGERO ? eq.esquivaLigero : 0;
            esquiva = pesado ? 0 : Math.min(0.35, m.refuerzo("esquiva") * 1.2 + rasgos.esquiva + v(pie, "esquiva") + ligera);
        }

        private static double baseDistancia(Naturaleza nat) {
            switch (nat) {
                case FOCO: return 1.02;
                case FURIA: return 0.90;
                case INGENIO: return 0.95;
                case RAYO: return 0.96;
                case ACERO:
                case ETER: return 0.98;
                default: return 1.0;
            }
        }

        private static double baseCuerpo(Naturaleza nat) {
            switch (nat) {
                case FURIA: return 1.02;
                case FOCO: return 0.90;
                case ETER: return 0.94;
                case INGENIO: return 0.95;
                case RAYO: return 0.96;
                case ACERO:
                default: return 1.0;
            }
        }

        /** ¿Va sobrecargado (o peor)? Entonces ni salta ni esquiva rápido. */
        public boolean sobrecargado() {
            return tramo == Equilibrio.Tramo.SOBRECARGA || tramo == Equilibrio.Tramo.IMPOSIBLE;
        }

        public double blindajeTotal() {
            return blindaje[0] + blindaje[1] + blindaje[2] + blindaje[3];
        }

        private static double v(Pieza p, String clave) {
            return p == null ? 0 : p.valor(clave);
        }
    }
}
