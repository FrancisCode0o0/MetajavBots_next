package castillafantasx.rostros;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RETOQUE DE RETRATO — Lo que se le ha cambiado a mano al retrato de un
 * rostro en el editor (Figuras › Retocar retratos): el encuadre y el busto
 * (que se le vean los hombros y los brazos), sus rasgos, cada expresión, la
 * luz, el marco y el PNG elegido para cada expresión. Lo pintado encima con
 * el pincel va aparte, en un PNG por rostro (RetoquesDeRetrato).
 * Todo en su valor de fábrica = el retrato sale como siempre.
 */
public final class RetoqueDeRetrato {

    /** Los marcos que puede llevar un retrato. */
    public enum Marco {
        EPOCA("de época (madera y filete dorado)"), DORADO("dorado"), HIERRO("de hierro"), SOBRIO("sobrio (madera oscura)"), NINGUNO("sin marco");

        public final String nombre;

        Marco(String nombre) {
            this.nombre = nombre;
        }
    }

    /** Los rasgos del rostro que se pueden retocar (las claves de res/datos/rostros.txt que son una cifra). */
    public static final String[] RASGOS = { "ancho", "mandibula", "menton", "pomulos", "nariz_grande", "ojos_grandes", "boca", "labios", "cejas",
        "carnes", "giro", "cuello", "edad", "canas", "entradas", "curtida" };
    /** Lo que se puede retocar de una expresión, en el orden de sus cifras. */
    public static final String[] DE_EXPRESION = { "cejaDentro", "cejaFuera", "ojos", "miraY", "boca", "comisuras", "cabezaBaja", "ladea", "tension" };

    /** El encuadre: cuánto se acerca (1 = lo de siempre; menos, se ve más cuerpo) y cuánto sube o se va a un lado el busto (en caras). */
    public double zoom = 1, sube, lado;
    /** El busto: por cuánto se ensanchan los hombros y si se le ven los brazos. */
    public double hombros = 1;
    public boolean brazos;
    /** La luz: cuánto gira hacia la derecha y cuánto se alza (grados), por cuánto se multiplica su fuerza y cuánta claridad se suma a las sombras. */
    public double luzGira, luzAlza, luzFuerza = 1, ambiente;
    public Marco marco = Marco.EPOCA;
    /** Los rasgos cambiados: su clave (una de RASGOS) y su valor nuevo. */
    public final Map<String, Double> rasgos = new LinkedHashMap<>();
    /** Lo que se suma a cada cifra de cada expresión en este rostro: «ira» -> las de DE_EXPRESION. */
    public final Map<String, double[]> expresiones = new LinkedHashMap<>();
    /** El PNG de arte/retratos/ elegido para cada expresión (por su nombre): «ira» -> «mi_dibujo.png». */
    public final Map<String, String> png = new LinkedHashMap<>();

    public boolean vacio() {
        return zoom == 1 && sube == 0 && lado == 0 && hombros == 1 && !brazos && luzGira == 0 && luzAlza == 0 && luzFuerza == 1 && ambiente == 0
                && marco == Marco.EPOCA && rasgos.isEmpty() && expresiones.isEmpty() && png.isEmpty();
    }

    public RetoqueDeRetrato copia() {
        RetoqueDeRetrato c = new RetoqueDeRetrato();
        c.tomar(this);
        return c;
    }

    public void tomar(RetoqueDeRetrato o) {
        zoom = o.zoom;
        sube = o.sube;
        lado = o.lado;
        hombros = o.hombros;
        brazos = o.brazos;
        luzGira = o.luzGira;
        luzAlza = o.luzAlza;
        luzFuerza = o.luzFuerza;
        ambiente = o.ambiente;
        marco = o.marco;
        rasgos.clear();
        rasgos.putAll(o.rasgos);
        expresiones.clear();
        for (Map.Entry<String, double[]> e : o.expresiones.entrySet()) expresiones.put(e.getKey(), e.getValue().clone());
        png.clear();
        png.putAll(o.png);
    }

    /** Lo que se suma a las cifras de esa expresión (ceros si no se ha retocado). */
    public double[] deExpresion(Expresion e) {
        double[] d = expresiones.get(e.nombre);
        return d != null ? d : new double[DE_EXPRESION.length];
    }

    /** Suma 'cuanto' a esa cifra (una de DE_EXPRESION) de esa expresión. */
    public void sumarAExpresion(Expresion e, String cifra, double cuanto) {
        double[] d = expresiones.computeIfAbsent(e.nombre, k -> new double[DE_EXPRESION.length]);
        d[java.util.Arrays.asList(DE_EXPRESION).indexOf(cifra)] += cuanto;
        boolean nada = true;
        for (double v : d) nada &= Math.abs(v) < 1e-9;
        if (nada) expresiones.remove(e.nombre);
    }

    /** El rostro con los rasgos retocados puestos (una copia: el de rostros.txt no se toca). */
    public Rostro aplicarA(Rostro original) {
        if (rasgos.isEmpty()) return original;
        Rostro r = original.copia();
        Map<String, String> datos = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : rasgos.entrySet()) {
            datos.put(e.getKey(), e.getKey().equals("edad") ? String.valueOf((int) Math.round(e.getValue())) : String.valueOf(e.getValue()));
        }
        r.leer(datos);
        return r;
    }

    private static String num(double v) {
        String s = String.format(Locale.ROOT, "%.3f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Los renglones de su sección del archivo. */
    public List<String> aLineas() {
        List<String> l = new ArrayList<>();
        if (zoom != 1) l.add("zoom = " + num(zoom));
        if (sube != 0) l.add("sube = " + num(sube));
        if (lado != 0) l.add("lado = " + num(lado));
        if (hombros != 1) l.add("hombros = " + num(hombros));
        if (brazos) l.add("brazos = si");
        if (luzGira != 0) l.add("luz_gira = " + num(luzGira));
        if (luzAlza != 0) l.add("luz_alza = " + num(luzAlza));
        if (luzFuerza != 1) l.add("luz_fuerza = " + num(luzFuerza));
        if (ambiente != 0) l.add("ambiente = " + num(ambiente));
        if (marco != Marco.EPOCA) l.add("marco = " + marco.name().toLowerCase(Locale.ROOT));
        for (Map.Entry<String, Double> e : rasgos.entrySet()) l.add("rasgo." + e.getKey() + " = " + num(e.getValue()));
        for (Map.Entry<String, double[]> e : expresiones.entrySet()) {
            for (int i = 0; i < DE_EXPRESION.length; i++) {
                if (Math.abs(e.getValue()[i]) > 1e-9) l.add("gesto." + e.getKey() + "." + DE_EXPRESION[i] + " = " + num(e.getValue()[i]));
            }
        }
        for (Map.Entry<String, String> e : png.entrySet()) l.add("png." + e.getKey() + " = " + e.getValue());
        return l;
    }

    /** Lee un renglón de su sección. */
    public void leerLinea(String linea) {
        int igual = linea.indexOf('=');
        if (igual < 0) return;
        String clave = linea.substring(0, igual).trim(), valor = linea.substring(igual + 1).trim();
        try {
            if (clave.startsWith("rasgo.")) {
                rasgos.put(clave.substring(6), Double.parseDouble(valor));
            } else if (clave.startsWith("gesto.")) {
                String[] t = clave.split("\\.");
                Expresion e = Expresion.de(t[1]);
                if (e != null && java.util.Arrays.asList(DE_EXPRESION).contains(t[2])) sumarAExpresion(e, t[2], Double.parseDouble(valor));
            } else if (clave.startsWith("png.")) {
                png.put(clave.substring(4), valor);
            } else {
                switch (clave) {
                    case "zoom":       zoom = Double.parseDouble(valor); break;
                    case "sube":       sube = Double.parseDouble(valor); break;
                    case "lado":       lado = Double.parseDouble(valor); break;
                    case "hombros":    hombros = Double.parseDouble(valor); break;
                    case "brazos":     brazos = valor.equals("si"); break;
                    case "luz_gira":   luzGira = Double.parseDouble(valor); break;
                    case "luz_alza":   luzAlza = Double.parseDouble(valor); break;
                    case "luz_fuerza": luzFuerza = Double.parseDouble(valor); break;
                    case "ambiente":   ambiente = Double.parseDouble(valor); break;
                    case "marco":      marco = Marco.valueOf(valor.toUpperCase(Locale.ROOT)); break;
                    default: System.err.println("Retoque de retrato: no se entiende «" + linea + "»"); break;
                }
            }
        } catch (RuntimeException e) {
            // Un renglón estropeado no tumba el juego: ese retoque se queda sin poner.
            System.err.println("Retoque de retrato: no se entiende «" + linea + "»");
        }
    }
}
