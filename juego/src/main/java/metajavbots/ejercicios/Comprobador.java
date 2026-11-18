package metajavbots.ejercicios;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import metajavbots.api.Puente;
import metajavbots.api.TopeDeCalculo;
import metajavbots.codigo.Cargador;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.Salida;
import metajavbots.codigo.Vigilante;

/**
 * COMPROBADOR — Comprueba un ejercicio: compila el código del jugador junto a
 * sus casos, los ejecuta uno a uno (con tope de pasos y de tiempo, y lo que
 * imprime, capturado) y dice cuáles pasan. Nada de lo que escriba el jugador
 * cuelga el juego: cada comprobación va en su propio hilo y se corta si tarda.
 */
public final class Comprobador {

    /** Cabecera de los ejercicios de método: en una sola línea, para que las líneas del jugador empiecen en la 2. */
    static final String CABECERA = "import java.util.*; import java.util.function.*; import java.util.stream.*; public class Ejercicio {";
    static final long PASOS = 50_000_000L;
    static final long NANOS_POR_CASO = 2_000_000_000L;
    static final long ESPERA_TOTAL_MS = 8000;

    /** Lo que pasó con un caso. */
    public static final class Resultado {
        public final Ejercicio.Caso caso;
        public final boolean bien;
        public final String obtenido;

        Resultado(Ejercicio.Caso caso, boolean bien, String obtenido) {
            this.caso = caso;
            this.bien = bien;
            this.obtenido = obtenido;
        }
    }

    /** Lo que pasó con todo el ejercicio. */
    public static final class Informe {
        public final List<Compilador.Fallo> errores;
        public final List<Resultado> casos;
        public final String salida;

        Informe(List<Compilador.Fallo> errores, List<Resultado> casos, String salida) {
            this.errores = errores;
            this.casos = casos;
            this.salida = salida;
        }

        public boolean compila() {
            for (Compilador.Fallo f : errores) if (f.grave) return false;
            return true;
        }

        public boolean todoBien() {
            if (!compila() || casos.isEmpty()) return false;
            for (Resultado r : casos) if (!r.bien) return false;
            return true;
        }

        public int bien() {
            int n = 0;
            for (Resultado r : casos) if (r.bien) n++;
            return n;
        }
    }

    private Comprobador() {
    }

    public static Informe comprobar(Ejercicio e, String codigo, Locale idioma) {
        Map<String, String> fuentes = new LinkedHashMap<>();
        int desplazamiento;
        StringBuilder casos = new StringBuilder();
        for (int i = 0; i < e.casos.size(); i++) {
            Ejercicio.Caso c = e.casos.get(i);
            if (e.tipo == Ejercicio.Tipo.SALIDA) {
                casos.append("\nstatic Object __caso").append(i).append("() throws Throwable { ").append(c.llamada)
                        .append(c.llamada.endsWith(";") ? "" : ";").append(" return null; }");
            } else {
                casos.append("\nstatic Object __caso").append(i).append("() throws Throwable { return (Object) (").append(c.llamada).append("); }");
                if (!c.esperaExcepcion()) {
                    casos.append("\nstatic Object __esperado").append(i).append("() throws Throwable { return (Object) (").append(c.esperado).append("); }");
                }
            }
        }
        String claseDeCasos;
        if (e.tipo == Ejercicio.Tipo.CLASE) {
            desplazamiento = 0;
            fuentes.put(Compilador.nombreDeClase(codigo), codigo);
            claseDeCasos = "Casos__";
            fuentes.put(claseDeCasos, "import java.util.*; import java.util.function.*; import java.util.stream.*; public class Casos__ {" + casos + "\n}\n");
        } else {
            desplazamiento = 1;
            claseDeCasos = "Ejercicio";
            fuentes.put(claseDeCasos, CABECERA + "\n" + codigo + "\n// ---- casos (no los escribe el jugador)" + casos + "\n}\n");
        }
        Compilador.Resultado r = Compilador.compilar(fuentes, true, Vigilante.Modo.EJERCICIO, idioma);
        List<Compilador.Fallo> errores = new ArrayList<>();
        int lineasDelJugador = codigo.split("\n", -1).length;
        for (Compilador.Fallo f : r.errores) {
            long linea = f.linea;
            boolean enLoSuyo = true;
            if (e.tipo != Ejercicio.Tipo.CLASE) {
                linea -= desplazamiento;
                if (linea < 1 || linea > lineasDelJugador) enLoSuyo = false;
            } else if (f.archivo.equals("Casos__")) {
                enLoSuyo = false;
            }
            if (!enLoSuyo) {
                // Un error en los casos: casi siempre, la firma del método o el nombre de la clase no son los del enunciado.
                errores.add(new Compilador.Fallo("", 0, 0, "metajavbots.casos", f.mensaje, f.grave));
            } else {
                errores.add(new Compilador.Fallo(f.archivo, linea, f.columna, f.codigo, f.mensaje, f.grave));
            }
        }
        if (!r.bien) return new Informe(errores, Collections.emptyList(), "");
        return ejecutar(e, r, claseDeCasos, errores);
    }

    private static Resultado ejecutarCaso(Class<?> clase, int i, Ejercicio.Caso c, Ejercicio.Tipo tipo, StringBuilder toda,
                                          AtomicReference<Object> cuenta) {
        Object obtenido;
        Throwable lanzado = null;
        String impreso;
        Puente.contar(PASOS, NANOS_POR_CASO);
        cuenta.set(Puente.cuentaActual());
        Salida.capturar();
        try {
            Method m = clase.getDeclaredMethod("__caso" + i);
            m.setAccessible(true);
            obtenido = m.invoke(null);
        } catch (InvocationTargetException ex) {
            lanzado = ex.getCause();
            obtenido = null;
        } catch (Throwable ex) {
            lanzado = ex;
            obtenido = null;
        } finally {
            impreso = Salida.soltar();
            Puente.dejarDeContar();
        }
        if (!impreso.isEmpty()) toda.append(impreso);
        if (lanzado instanceof TopeDeCalculo) {
            return new Resultado(c, false, "⏱ demasiados pasos (¿un bucle que no acaba?)");
        }
        if (lanzado instanceof StackOverflowError) {
            return new Resultado(c, false, "⚠ StackOverflowError (¿una recursión sin caso base?)");
        }
        if (c.esperaExcepcion()) {
            String quiero = c.esperado.substring(1).strip();
            if (lanzado != null && (lanzado.getClass().getSimpleName().equals(quiero) || lanzado.getClass().getName().equals(quiero))) {
                return new Resultado(c, true, "!" + lanzado.getClass().getSimpleName());
            }
            return new Resultado(c, false, lanzado != null ? "!" + lanzado.getClass().getSimpleName() : mostrar(obtenido));
        }
        if (lanzado != null) {
            return new Resultado(c, false, "!" + lanzado.getClass().getSimpleName() + (lanzado.getMessage() != null ? ": " + lanzado.getMessage() : ""));
        }
        if (tipo == Ejercicio.Tipo.SALIDA) {
            String quiero = desescapar(c.esperado);
            String limpio = impreso.replace("\r\n", "\n");
            boolean bien = limpio.equals(quiero) || limpio.stripTrailing().equals(quiero.stripTrailing());
            return new Resultado(c, bien, "\"" + escapar(limpio) + "\"");
        }
        Object esperado;
        try {
            Method m = clase.getDeclaredMethod("__esperado" + i);
            m.setAccessible(true);
            esperado = m.invoke(null);
        } catch (Throwable ex) {
            return new Resultado(c, false, "⚠ el caso no se pudo evaluar: " + ex);
        }
        return new Resultado(c, iguales(esperado, obtenido), mostrar(obtenido));
    }

    /** ¿Son lo mismo? Con tolerancia para los decimales y comparando arrays y listas por su contenido. */
    public static boolean iguales(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number && b instanceof Number) {
            boolean decimal = a instanceof Double || a instanceof Float || b instanceof Double || b instanceof Float;
            if (decimal) {
                double x = ((Number) a).doubleValue(), y = ((Number) b).doubleValue();
                if (Double.isNaN(x) || Double.isNaN(y)) return Double.isNaN(x) && Double.isNaN(y);
                return Math.abs(x - y) <= 1e-9 * Math.max(1, Math.max(Math.abs(x), Math.abs(y)));
            }
            return ((Number) a).longValue() == ((Number) b).longValue();
        }
        if (a.getClass().isArray() || b.getClass().isArray()) {
            if (!a.getClass().isArray() || !b.getClass().isArray()) return false;
            int n = Array.getLength(a);
            if (n != Array.getLength(b)) return false;
            for (int i = 0; i < n; i++) if (!iguales(Array.get(a, i), Array.get(b, i))) return false;
            return true;
        }
        if (a instanceof List && b instanceof List) {
            List<?> x = (List<?>) a, y = (List<?>) b;
            if (x.size() != y.size()) return false;
            for (int i = 0; i < x.size(); i++) if (!iguales(x.get(i), y.get(i))) return false;
            return true;
        }
        return a.equals(b);
    }

    /** Cómo se enseña un valor: los textos con comillas, los arrays con su contenido. */
    public static String mostrar(Object o) {
        if (o == null) return "null";
        if (o instanceof String) return "\"" + escapar((String) o) + "\"";
        if (o instanceof Character) return "'" + o + "'";
        if (o.getClass().isArray()) {
            StringBuilder b = new StringBuilder("{");
            int n = Array.getLength(o);
            for (int i = 0; i < n; i++) {
                if (i > 0) b.append(", ");
                if (i >= 50) {
                    b.append("…");
                    break;
                }
                b.append(mostrar(Array.get(o, i)));
            }
            return b.append('}').toString();
        }
        if (o instanceof Collection && !(o instanceof List)) return o.toString();
        String s = String.valueOf(o);
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }

    static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"");
    }

    static String desescapar(String s) {
        s = s.strip();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) s = s.substring(1, s.length() - 1);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char d = s.charAt(++i);
                b.append(d == 'n' ? '\n' : d == 't' ? '\t' : d);
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** Ejecuta los casos ya compilados en un hilo aparte (si se cuelga, se abandona) y apunta lo que sale de cada uno. */
    private static Informe ejecutar(Ejercicio e, Compilador.Resultado r, String claseDeCasos, List<Compilador.Fallo> errores) {
        Cargador cargador = new Cargador(r.clases, Vigilante.Modo.EJERCICIO);
        List<Resultado> resultados = new ArrayList<>();
        StringBuilder toda = new StringBuilder();
        // Todo en un hilo aparte: si se cuelga, se abandona (el Tope lo para en cuanto dé otro paso).
        AtomicReference<Object> cuenta = new AtomicReference<>();
        Thread hilo = new Thread(() -> {
            try {
                Class<?> clase = cargador.loadClass(claseDeCasos);
                for (int i = 0; i < e.casos.size(); i++) {
                    Ejercicio.Caso c = e.casos.get(i);
                    Resultado res = ejecutarCaso(clase, i, c, e.tipo, toda, cuenta);
                    synchronized (resultados) {
                        resultados.add(res);
                    }
                }
            } catch (Throwable t) {
                synchronized (resultados) {
                    while (resultados.size() < e.casos.size()) {
                        resultados.add(new Resultado(e.casos.get(resultados.size()), false, "⚠ " + t));
                    }
                }
            }
        }, "ejercicio-" + e.id);
        hilo.setDaemon(true);
        hilo.start();
        try {
            hilo.join(ESPERA_TOTAL_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (hilo.isAlive()) {
            Puente.cancelar(cuenta.get());
            hilo.interrupt();
        }
        List<Resultado> copia;
        synchronized (resultados) {
            copia = new ArrayList<>(resultados);
        }
        while (copia.size() < e.casos.size()) copia.add(new Resultado(e.casos.get(copia.size()), false, "⏱ tarda demasiado"));
        return new Informe(errores, copia, toda.toString());
    }
}
