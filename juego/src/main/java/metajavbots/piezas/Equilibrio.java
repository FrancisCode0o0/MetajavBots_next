package metajavbots.piezas;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.LectorRes;
import metajavbots.textos.Trilingue;

/**
 * EQUILIBRIO — Las reglas del peso, la carga y la locomoción, leídas de
 * res/datos/equilibrio.txt (docs/EQUILIBRIO.md). Como en Armored Core, las
 * piernas fijan la carga: r = peso que llevan encima / su carga. Por debajo de
 * 0,70 el robot es ligero; hasta 1 baja un poco su movilidad; hasta 1,2 va
 * sobrecargado (lento, torpe y sin saltos ni esquivas rápidas); por encima, no
 * puede salir a pelear. Cada locomoción tiene sus rasgos y su rendimiento en
 * cada terreno, como los tipos de piernas de Medabots. Todos los números están
 * en el archivo, no aquí: así se retocan sin tocar el código.
 */
public final class Equilibrio {

    /** El tramo de peso frente a carga. */
    public enum Tramo { LIGERO, NORMAL, SOBRECARGA, IMPOSIBLE }

    /** Los terrenos de las arenas. */
    public enum Terreno {
        LLANO("Llano", "Ebene", "Flat"), ACEITE("Aceite", "Öl", "Oil"), AGUA("Agua", "Wasser", "Water"),
        RAMPA("Rampas", "Rampen", "Ramps"), ESCOMBROS("Escombros", "Trümmer", "Rubble"),
        TRAMPA("Trampas de suelo", "Bodenfallen", "Floor traps");

        private final String es, de, en;

        Terreno(String es, String de, String en) {
            this.es = es;
            this.de = de;
            this.en = en;
        }

        /** Su nombre en ese idioma. */
        public String nombre(Idioma i) {
            return i.elegir(es, de, en);
        }

        /** El terreno de ese nombre (sin distinguir mayúsculas); LLANO si no se conoce. */
        public static Terreno de(String s) {
            return Pieza.enumDe(Terreno.class, s, LLANO);
        }
    }

    /** Los rasgos de una locomoción (una sección de equilibrio.txt). */
    public static final class Rasgos {
        public final Pieza.Locomocion tipo;
        public final Trilingue nombre;
        /** Divide los empujones; parte del volteo que se anula; agarre mínimo en lo que resbala. */
        public final double estabilidad, antivuelco, traccion;
        /** Esquiva contra balas y misiles, y contra golpes (solo los voladores). */
        public final double esquiva, esquivaCuerpo;
        /** Cuánto le afecta un imán; daño de sus embestidas; cuánto le afectan cuñas y volteadores. */
        public final double iman, embestida, cuna;
        /** Absorbe el retroceso; es blanco antiaéreo; pisa el suelo (aceite y trampas de suelo); el agua lo desorienta. */
        public final boolean absorbeRetroceso, aereo, pisaSuelo, cortocircuito;
        private final EnumMap<Terreno, Double> rendimiento = new EnumMap<>(Terreno.class);

        Rasgos(Pieza.Locomocion tipo, Map<String, String> v) {
            this.tipo = tipo;
            nombre = LectorRes.trilingue(v.getOrDefault("nombre", tipo.name()), true);
            estabilidad = Math.max(0.2, num(v, "estabilidad", 1));
            antivuelco = Math.max(0, Math.min(1, num(v, "antivuelco", 0)));
            traccion = Math.max(0, Math.min(1, num(v, "traccion", 0)));
            esquiva = Math.max(0, Math.min(0.3, num(v, "esquiva", 0)));
            esquivaCuerpo = Math.max(0, Math.min(0.9, num(v, "esquiva_cuerpo", 0)));
            iman = Math.max(0, num(v, "iman", 1));
            embestida = Math.max(0, num(v, "embestida", 1));
            cuna = Math.max(0, num(v, "cuna", 1));
            absorbeRetroceso = num(v, "absorbe_retroceso", 0) > 0;
            aereo = num(v, "aereo", 0) > 0;
            pisaSuelo = num(v, "pisa_suelo", 1) > 0;
            cortocircuito = num(v, "cortocircuito", 1) > 0;
            for (Terreno t : Terreno.values()) {
                rendimiento.put(t, Math.max(0.2, Math.min(1.5, num(v, t.name().toLowerCase(Locale.ROOT), 1))));
            }
        }

        /** Lo que rinde (velocidad y aceleración) en ese terreno: 1 normal, menos peor, más mejor. */
        public double rendimiento(Terreno t) {
            Double r = t == null ? null : rendimiento.get(t);
            return r == null ? 1 : r;
        }

        /** El terreno donde mejor rinde (el primero si empatan). */
        public Terreno mejor() {
            Terreno m = Terreno.LLANO;
            for (Terreno t : Terreno.values()) if (rendimiento(t) > rendimiento(m) + 1e-9) m = t;
            return m;
        }

        /** El terreno donde peor rinde (el primero si empatan). */
        public Terreno peor() {
            Terreno m = Terreno.LLANO;
            for (Terreno t : Terreno.values()) if (rendimiento(t) < rendimiento(m) - 1e-9) m = t;
            return m;
        }

        /** ¿Rinde igual en todos los terrenos (ni gana ni pierde)? */
        public boolean igualEnTodos() {
            return Math.abs(rendimiento(mejor()) - rendimiento(peor())) < 1e-9;
        }

        private static double num(Map<String, String> v, String k, double siNo) {
            return LectorRes.decimal(v.get(k), siNo);
        }
    }

    private static Equilibrio delJuego;

    private final Map<String, Map<String, String>> secciones = new HashMap<>();
    private final EnumMap<Pieza.Locomocion, Rasgos> rasgos = new EnumMap<>(Pieza.Locomocion.class);
    /** Los límites de los tramos (en r) y lo que se pierde en cada uno. */
    public final double ligero, limite, maximo, penalizacionNormal, sobreVelocidad, sobreAceleracion, sobreGiro, sobreEnergia;
    /** Esquiva extra del ligero y masa de referencia de los choques. */
    public final double esquivaLigero, masaReferencia;
    /** Giro según la velocidad, energía por unidad recorrida y lo que frena de más. */
    public final double giroPorVelocidad, costeMovimiento, frenada;

    private Equilibrio(Iterable<LectorRes.Seccion> leidas) {
        for (LectorRes.Seccion s : leidas) secciones.put(s.id().toLowerCase(Locale.ROOT), s.valores());
        for (Pieza.Locomocion l : Pieza.Locomocion.values()) {
            rasgos.put(l, new Rasgos(l, secciones.getOrDefault(l.name().toLowerCase(Locale.ROOT), Map.of())));
        }
        ligero = valor("peso", "ligero", 0.70);
        limite = Math.max(ligero + 0.01, valor("peso", "limite", 1.00));
        maximo = Math.max(limite + 0.01, valor("peso", "maximo", 1.20));
        penalizacionNormal = acotar(valor("peso", "penalizacion_normal", 0.15), 0, 0.9);
        sobreVelocidad = acotar(valor("peso", "sobrecarga_velocidad", 0.40), 0, 0.9);
        sobreAceleracion = acotar(valor("peso", "sobrecarga_aceleracion", 0.40), 0, 0.9);
        sobreGiro = acotar(valor("peso", "sobrecarga_giro", 0.30), 0, 0.9);
        sobreEnergia = acotar(valor("peso", "sobrecarga_energia", 1.40), 1, 5);
        esquivaLigero = acotar(valor("peso", "esquiva_ligero", 0.04), 0, 0.3);
        masaReferencia = acotar(valor("peso", "masa_referencia", 45), 5, 500);
        giroPorVelocidad = acotar(valor("movimiento", "giro_por_velocidad", 0.6), 0, 0.95);
        costeMovimiento = acotar(valor("movimiento", "coste_movimiento", 0.012), 0, 1);
        frenada = acotar(valor("movimiento", "frenada", 2.0), 1, 5);
    }

    /** Las del juego (res/datos/equilibrio.txt), leídas una vez. */
    public static synchronized Equilibrio delJuego() {
        if (delJuego == null) {
            try (InputStream in = Equilibrio.class.getResourceAsStream("/datos/equilibrio.txt")) {
                delJuego = new Equilibrio(LectorRes.secciones(in, '='));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer res/datos/equilibrio.txt", e);
            }
        }
        return delJuego;
    }

    /** Un número de una sección del archivo (o siNo si no está). */
    public double valor(String seccion, String clave, double siNo) {
        Map<String, String> s = secciones.get(seccion.toLowerCase(Locale.ROOT));
        return s == null ? siNo : LectorRes.decimal(s.get(clave), siNo);
    }

    /** Los rasgos de esa locomoción. */
    public Rasgos rasgos(Pieza.Locomocion l) {
        return rasgos.get(l == null ? Pieza.Locomocion.BIPEDO : l);
    }

    /** El tramo de una proporción peso/carga. */
    public Tramo tramo(double r) {
        if (!Double.isFinite(r) || r > maximo + 1e-9) return Tramo.IMPOSIBLE;
        if (r > limite + 1e-9) return Tramo.SOBRECARGA;
        return r > ligero + 1e-9 ? Tramo.NORMAL : Tramo.LIGERO;
    }

    /** Lo que queda de velocidad máxima con esa proporción (1 sin castigo). */
    public double factorVelocidad(double r) {
        return factor(r, sobreVelocidad);
    }

    /** Lo que queda de aceleración con esa proporción. */
    public double factorAceleracion(double r) {
        return factor(r, sobreAceleracion);
    }

    /** Lo que queda de giro con esa proporción. */
    public double factorGiro(double r) {
        return factor(r, sobreGiro);
    }

    /** Cuánto más cuesta moverse con esa proporción (1 normal; más en sobrecarga). */
    public double factorEnergia(double r) {
        Tramo t = tramo(r);
        return t == Tramo.SOBRECARGA || t == Tramo.IMPOSIBLE ? sobreEnergia : 1;
    }

    private double factor(double r, double sobre) {
        switch (tramo(r)) {
            case LIGERO:
                return 1;
            case NORMAL:
                return 1 - penalizacionNormal * (r - ligero) / (limite - ligero);
            default:
                return 1 - sobre;
        }
    }

    /** Giro de la mira (grados por turno) con el arma más pesada que lleva (más pesada, más lenta). */
    public double giroMira(double pesoArma) {
        double base = valor("mira", "giro", 14);
        double f = 1 - valor("mira", "por_peso", 0.035) * (pesoArma - valor("mira", "peso_referencia", 8));
        return base * acotar(f, valor("mira", "minimo", 0.55), valor("mira", "maximo", 1.2));
    }

    static double acotar(double v, double min, double max) {
        if (!Double.isFinite(v)) return min;
        return Math.max(min, Math.min(max, v));
    }
}
