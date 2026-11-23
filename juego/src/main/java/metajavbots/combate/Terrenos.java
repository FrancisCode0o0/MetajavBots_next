package metajavbots.combate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.piezas.Equilibrio;
import metajavbots.piezas.Pieza;
import metajavbots.textos.LectorRes;

/**
 * TERRENOS — La mezcla de terrenos de cada arena (res/datos/terrenos.txt):
 * el suelo de base y las zonas de aceite, agua, rampas y escombros que se le
 * ponen encima, además de las trampas y obstáculos que ya trae la arena. Con
 * el reparto (qué parte de la arena es cada terreno) se calcula cómo rinde
 * cada locomoción en cada arena: así el jugador lo ve antes de pelear y la
 * prueba de equilibrio comprueba que ninguna locomoción sale favorecida en el
 * conjunto (docs/EQUILIBRIO.md).
 */
public final class Terrenos {

    /** Una zona de terreno: su tipo, su centro (en tanto por uno) y su radio (en unidades de una arena de 1000 de ancho). */
    private record Zona(String tipo, double x, double y, double radio) {
    }

    /** El suelo de base y las zonas de una arena. */
    private record Plano(Equilibrio.Terreno suelo, List<Zona> zonas) {
    }

    private static Map<String, Plano> planos;

    private Terrenos() {
    }

    private static synchronized Map<String, Plano> planos() {
        if (planos == null) {
            Map<String, Plano> m = new LinkedHashMap<>();
            try (InputStream in = Terrenos.class.getResourceAsStream("/datos/terrenos.txt")) {
                for (LectorRes.Seccion s : LectorRes.secciones(in, '=')) {
                    m.put(s.id().toUpperCase(Locale.ROOT), new Plano(Equilibrio.Terreno.de(s.valores().get("suelo")), zonas(s.valores().get("zonas"))));
                }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer res/datos/terrenos.txt", e);
            }
            planos = m;
        }
        return planos;
    }

    private static List<Zona> zonas(String texto) {
        List<Zona> l = new ArrayList<>();
        if (texto == null || texto.isBlank()) return l;
        for (String trozo : texto.split(";")) {
            String[] p = trozo.strip().split("\\s+");
            if (p.length < 4) continue;
            l.add(new Zona(p[0].toLowerCase(Locale.ROOT), LectorRes.decimal(p[1], 0.5), LectorRes.decimal(p[2], 0.5), LectorRes.decimal(p[3], 60)));
        }
        return l;
    }

    /** Las arenas que tienen su mezcla de terrenos (en el orden del archivo). */
    public static List<String> escenarios() {
        return Collections.unmodifiableList(new ArrayList<>(planos().keySet()));
    }

    /**
     * La arena de una pelea en ese escenario: la de siempre (Arena.de, con sus trampas) con su suelo de base y sus zonas
     * de terreno. Las pruebas, las demostraciones y los minijuegos siguen usando Arena.de, que no cambia.
     */
    public static Arena arenaDePelea(String escenario, double ancho, double alto) {
        Plano plano = planos().get(escenario == null ? "" : escenario.toUpperCase(Locale.ROOT));
        if (plano == null) return Arena.de(escenario == null ? "" : escenario, ancho, alto);
        Arena a = Arena.de(escenario, ancho, alto);
        if (a == Arena.NINGUNA) a = Arena.vacia();
        a.suelo = plano.suelo;
        double escala = ancho / 1000.0;
        for (Zona z : plano.zonas) a.zona(z.tipo, z.x * ancho, z.y * alto, z.radio * escala);
        return a;
    }

    /** Qué parte de la arena (de 0 a 1) es cada terreno, con sus zonas, sus trampas y sus obstáculos. */
    public static Map<Equilibrio.Terreno, Double> reparto(String escenario) {
        double ancho = 1000, alto = 700, total = ancho * alto;
        EnumMap<Equilibrio.Terreno, Double> r = new EnumMap<>(Equilibrio.Terreno.class);
        for (Equilibrio.Terreno t : Equilibrio.Terreno.values()) r.put(t, 0.0);
        Arena a = arenaDePelea(escenario, ancho, alto);
        double ocupado = 0;
        for (Arena.Trampa t : a.trampas()) {
            Equilibrio.Terreno de = Arena.terrenoDe(t.tipo);
            if (de == null && t.destructible) de = Equilibrio.Terreno.ESCOMBROS;
            if (de == null) continue;
            double area = Math.PI * t.radio * t.radio / total;
            r.merge(de, area, Double::sum);
            ocupado += area;
        }
        r.merge(a.suelo, Math.max(0, 1 - ocupado), Double::sum);
        return r;
    }

    /** Cómo rinde esa locomoción en esa arena: la media de su rendimiento en cada terreno, pesada por lo que ocupa. */
    public static double rendimiento(String escenario, Pieza.Locomocion l) {
        Equilibrio.Rasgos ra = Equilibrio.delJuego().rasgos(l);
        double s = 0, peso = 0;
        for (Map.Entry<Equilibrio.Terreno, Double> e : reparto(escenario).entrySet()) {
            s += ra.rendimiento(e.getKey()) * e.getValue();
            peso += e.getValue();
        }
        return peso <= 0 ? 1 : s / peso;
    }

    /** La media de rendimiento de todas las locomociones en esa arena (para decir quién brilla y quién sufre). */
    public static double rendimientoMedio(String escenario) {
        double s = 0;
        for (Pieza.Locomocion l : Pieza.Locomocion.values()) s += rendimiento(escenario, l);
        return s / Pieza.Locomocion.values().length;
    }

    /** La locomoción que mejor rinde en esa arena. */
    public static Pieza.Locomocion mejorEn(String escenario) {
        Pieza.Locomocion m = Pieza.Locomocion.BIPEDO;
        for (Pieza.Locomocion l : Pieza.Locomocion.values()) if (rendimiento(escenario, l) > rendimiento(escenario, m) + 1e-9) m = l;
        return m;
    }

    /** La locomoción que peor rinde en esa arena. */
    public static Pieza.Locomocion peorEn(String escenario) {
        Pieza.Locomocion m = Pieza.Locomocion.BIPEDO;
        for (Pieza.Locomocion l : Pieza.Locomocion.values()) if (rendimiento(escenario, l) < rendimiento(escenario, m) - 1e-9) m = l;
        return m;
    }
}
