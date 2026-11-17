package metajavbots.progreso;

import java.util.Locale;

/**
 * REGISTRO — Una pelea del historial: cuándo, en qué modo, contra quién,
 * cómo acabó y con cuánto daño. Se guarda como una línea de historial.txt.
 */
public final class Registro {

    public enum Resultado { VICTORIA, DERROTA, EMPATE }

    public final long fecha;
    /** ADAPTADA, FACIL…EXTREMA, AZAR, DESAFIO, JEFE, TEMARIO, PRACTICA, AJENO. */
    public final String modo;
    public final String rival, modeloRival, miModelo;
    public final double nivelRival;
    public final Resultado resultado;
    public final double danoRecibido, danoHecho;
    public final int rondas;
    public final String etiqueta;

    public Registro(long fecha, String modo, String rival, String modeloRival, String miModelo, double nivelRival,
                    Resultado resultado, double danoRecibido, double danoHecho, int rondas, String etiqueta) {
        this.fecha = fecha;
        this.modo = limpio(modo);
        this.rival = limpio(rival);
        this.modeloRival = limpio(modeloRival);
        this.miModelo = limpio(miModelo);
        this.nivelRival = nivelRival;
        this.resultado = resultado;
        this.danoRecibido = danoRecibido;
        this.danoHecho = danoHecho;
        this.rondas = rondas;
        this.etiqueta = limpio(etiqueta);
    }

    private static String limpio(String s) {
        return s == null ? "" : s.replace('|', '/').replace('\n', ' ');
    }

    public String aLinea() {
        return String.join("|", Long.toString(fecha), modo, rival, modeloRival, miModelo,
                String.format(Locale.ROOT, "%.3f", nivelRival), resultado.name(),
                String.format(Locale.ROOT, "%.1f", danoRecibido), String.format(Locale.ROOT, "%.1f", danoHecho),
                Integer.toString(rondas), etiqueta);
    }

    public static Registro deLinea(String linea) {
        String[] p = linea.split("\\|", -1);
        if (p.length < 11) return null;
        try {
            return new Registro(Long.parseLong(p[0]), p[1], p[2], p[3], p[4], Double.parseDouble(p[5]),
                    Resultado.valueOf(p[6]), Double.parseDouble(p[7]), Double.parseDouble(p[8]), Integer.parseInt(p[9]), p[10]);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
