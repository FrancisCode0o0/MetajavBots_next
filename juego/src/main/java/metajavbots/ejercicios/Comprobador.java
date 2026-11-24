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
import metajavbots.textos.Idioma;

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
        /** Las líneas del código del jugador que se ejecutaron en este caso, en orden (solo comprobando con traza). */
        public int[] traza = new int[0];

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
        return comprobar(e, codigo, idioma, false);
    }

    /** Solo compila el ejercicio con sus casos (para revisar al escribir, sin ejecutar los casos). */
    public static Informe soloCompilar(Ejercicio e, String codigo, Locale idioma) {
        return compilar(e, codigo, idioma, false, false);
    }

    /** Lo mismo; con traza, cada caso apunta además las líneas que ejecuta, en orden (para el robotito: AnimacionDelCaso). */
    public static Informe comprobar(Ejercicio e, String codigo, Locale idioma, boolean traza) {
        return compilar(e, codigo, idioma, traza, true);
    }

    private static Idioma idiomaDe(Locale locale) {
        if (locale == null) return Idioma.ES;
        String l = locale.getLanguage();
        if ("de".equals(l)) return Idioma.DE;
        if ("en".equals(l)) return Idioma.EN;
        return Idioma.ES;
    }

    private static Informe compilar(Ejercicio e, String codigo, Locale idioma, boolean traza, boolean ejecutarCasos) {
        List<Ejercicio.Caso> listaCasos = e.casos(idiomaDe(idioma));
        Map<String, String> fuentes = new LinkedHashMap<>();
        int desplazamiento;
        StringBuilder casos = new StringBuilder();
        for (int i = 0; i < listaCasos.size(); i++) {
            Ejercicio.Caso c = listaCasos.get(i);
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
        Compilador.Resultado r = Compilador.compilar(fuentes, ejecutarCasos, traza && ejecutarCasos, Vigilante.Modo.EJERCICIO, idioma);
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
        if (!r.bien || !ejecutarCasos) return new Informe(errores, Collections.emptyList(), "");
        return ejecutar(e, listaCasos, r, claseDeCasos, errores, desplazamiento, lineasDelJugador);
    }

    /** La traza del último caso ejecutado en este hilo. */
    private static final ThreadLocal<int[]> ULTIMA_TRAZA = ThreadLocal.withInitial(() -> new int[0]);

    /** Solo las líneas del código del jugador, con sus números (sin la cabecera ni los casos que añade el comprobador). */
    static int[] suyas(int[] traza, int desplazamiento, int lineasDelJugador) {
        int[] l = new int[traza.length];
        int n = 0;
        for (int x : traza) {
            int y = x - desplazamiento;
            if (y >= 1 && y <= lineasDelJugador) l[n++] = y;
        }
        return java.util.Arrays.copyOf(l, n);
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
            ULTIMA_TRAZA.set(Puente.secuencia());
            Puente.dejarDeContar();
        }
        if (!impreso.isEmpty()) toda.append(impreso);
        if (lanzado instanceof TopeDeCalculo) {
            return new Resultado(c, false, "⏱ demasiados pasos de cálculo (¿un bucle que no acaba?)");
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
        Puente.contar(PASOS, NANOS_POR_CASO);
        cuenta.set(Puente.cuentaActual());
        try {
            Method m = clase.getDeclaredMethod("__esperado" + i);
            m.setAccessible(true);
            esperado = m.invoke(null);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof TopeDeCalculo) {
                return new Resultado(c, false, "⏱ demasiados pasos de cálculo (¿un bucle que no acaba?)");
            }
            return new Resultado(c, false, "⚠ el caso no se pudo evaluar: " + ex.getCause());
        } catch (Throwable ex) {
            return new Resultado(c, false, "⚠ el caso no se pudo evaluar: " + ex);
        } finally {
            Puente.dejarDeContar();
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

    public static String desescapar(String s) {
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
    private static Informe ejecutar(Ejercicio e, List<Ejercicio.Caso> listaCasos, Compilador.Resultado r, String claseDeCasos,
            List<Compilador.Fallo> errores, int desplazamiento, int lineasDelJugador) {
        Cargador cargador = new Cargador(r.clases, Vigilante.Modo.EJERCICIO);
        List<Resultado> resultados = new ArrayList<>();
        StringBuilder toda = new StringBuilder();
        // Todo en un hilo aparte: si se cuelga, se abandona (el Tope lo para en cuanto dé otro paso).
        AtomicReference<Object> cuenta = new AtomicReference<>();
        Thread hilo = new Thread(() -> {
            try {
                Class<?> clase = cargador.loadClass(claseDeCasos);
                for (int i = 0; i < listaCasos.size(); i++) {
                    Ejercicio.Caso c = listaCasos.get(i);
                    Resultado res = ejecutarCaso(clase, i, c, e.tipo, toda, cuenta);
                    res.traza = suyas(ULTIMA_TRAZA.get(), desplazamiento, lineasDelJugador);
                    synchronized (resultados) {
                        resultados.add(res);
                    }
                }
            } catch (Throwable t) {
                synchronized (resultados) {
                    while (resultados.size() < listaCasos.size()) {
                        resultados.add(new Resultado(listaCasos.get(resultados.size()), false, "⚠ " + t));
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
        while (copia.size() < listaCasos.size()) copia.add(new Resultado(listaCasos.get(copia.size()), false, "⏱ tarda demasiado"));
        return new Informe(errores, copia, toda.toString());
    }
}
