package metajavbots.ejercicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.api.Puente;
import metajavbots.codigo.Cargador;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * LABORATORIO DE PRUEBAS — El modo especial «piezas por escribir pruebas»
 * (res/laboratorio/): el jugador escribe pruebas al estilo JUnit 5 para una
 * clase del juego. Valen si pasan con la clase buena y cazan a los mutantes
 * (copias con un fallo escondido): cada mutante tiene que hacer fallar al
 * menos una prueba. Si las cazan todas, se gana la pieza «pruebas:<id>».
 */
public final class Laboratorio {

    public final String id, clase, plantilla, solucion;
    public final Trilingue titulo, enunciado;
    public final List<String> mutantes;

    Laboratorio(Map<String, String> v, List<String> mutantes) {
        id = v.getOrDefault("id", "");
        clase = v.getOrDefault("clase", "");
        plantilla = v.getOrDefault("plantilla", "");
        solucion = v.getOrDefault("solucion", "");
        titulo = new Trilingue(v.get("titulo.es"), v.get("titulo.de"), v.get("titulo.en"));
        enunciado = new Trilingue(v.get("enunciado.es"), v.get("enunciado.de"), v.get("enunciado.en"));
        this.mutantes = Collections.unmodifiableList(mutantes);
    }

    public String titulo(Idioma i) {
        return titulo.en(i);
    }

    /** Lo que salió: las pruebas (nombre → bien o el fallo) con la clase buena, y qué mutantes cazó. */
    public static final class Resultado {
        public final List<Compilador.Fallo> errores = new ArrayList<>();
        public final Map<String, String> pruebas = new LinkedHashMap<>();
        public final List<Boolean> cazados = new ArrayList<>();

        public boolean pasanTodas() {
            if (pruebas.isEmpty()) return false;
            for (String s : pruebas.values()) if (!s.isEmpty()) return false;
            return true;
        }

        public boolean todoBien() {
            if (!pasanTodas() || pruebas.size() < 3) return false;
            for (boolean c : cazados) if (!c) return false;
            return !cazados.isEmpty();
        }
    }

    /** Compila las pruebas con la clase buena y con cada mutante, y las ejecuta (en otro hilo, con tope). */
    public Resultado comprobar(String pruebas, Locale idioma) {
        Resultado r = new Resultado();
        Map<String, String> pruebasConClase = ejecutar(clase, pruebas, idioma, r.errores);
        if (pruebasConClase == null) return r;
        r.pruebas.putAll(pruebasConClase);
        if (!r.pasanTodas()) return r;
        for (String m : mutantes) {
            Map<String, String> conMutante = ejecutar(m, pruebas, idioma, new ArrayList<>());
            boolean cazado = conMutante == null;
            if (conMutante != null) for (String s : conMutante.values()) if (!s.isEmpty()) cazado = true;
            r.cazados.add(cazado);
        }
        return r;
    }

    /** Ejecuta las pruebas contra una versión de la clase: nombre → "" si pasa, o por qué falla. null si no compila. */
    private static Map<String, String> ejecutar(String clase, String pruebas, Locale idioma, List<Compilador.Fallo> errores) {
        Map<String, String> fuentes = new LinkedHashMap<>();
        fuentes.put(Compilador.nombreDeClase(clase), clase);
        String nombrePruebas = Compilador.nombreDeClase(pruebas);
        fuentes.put(nombrePruebas, pruebas);
        Compilador.Resultado c = Compilador.compilar(fuentes, true, Vigilante.Modo.EJERCICIO, idioma);
        if (!c.bien) {
            errores.addAll(c.errores);
            return null;
        }
        Map<String, String> res = new LinkedHashMap<>();
        Thread h = new Thread(() -> {
            try {
                Cargador cg = new Cargador(c.clases, Vigilante.Modo.EJERCICIO);
                Class<?> k = cg.loadClass(nombrePruebas);
                Method antes = null;
                List<Method> tests = new ArrayList<>();
                for (Method m : k.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(metajavbots.junit.Test.class)) tests.add(m);
                    if (m.isAnnotationPresent(metajavbots.junit.BeforeEach.class)) antes = m;
                }
                tests.sort((a, b) -> a.getName().compareTo(b.getName()));
                for (Method m : tests) {
                    String nombre = m.getName();
                    metajavbots.junit.DisplayName dn = m.getAnnotation(metajavbots.junit.DisplayName.class);
                    if (dn != null) nombre = dn.value();
                    Puente.contar(20_000_000L, 2_000_000_000L);
                    try {
                        java.lang.reflect.Constructor<?> ctor = k.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        Object o = ctor.newInstance();
                        if (antes != null) {
                            antes.setAccessible(true);
                            antes.invoke(o);
                        }
                        m.setAccessible(true);
                        m.invoke(o);
                        synchronized (res) {
                            res.put(nombre, "");
                        }
                    } catch (InvocationTargetException e) {
                        Throwable t = e.getCause();
                        synchronized (res) {
                            res.put(nombre, t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : ""));
                        }
                    } catch (Throwable t) {
                        synchronized (res) {
                            res.put(nombre, t.toString());
                        }
                    } finally {
                        Puente.dejarDeContar();
                    }
                }
            } catch (Throwable t) {
                synchronized (res) {
                    res.put("?", t.toString());
                }
            }
        }, "laboratorio");
        h.setDaemon(true);
        h.start();
        try {
            h.join(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (res) {
            return new LinkedHashMap<>(res);
        }
    }

    // ------------------------------------------------------------------ leer

    private static List<Laboratorio> todos;

    public static synchronized List<Laboratorio> todos() {
        if (todos == null) {
            List<Laboratorio> l = new ArrayList<>();
            for (String a : Banco.indice("/laboratorio/indice.txt")) {
                try (InputStream in = Laboratorio.class.getResourceAsStream("/laboratorio/" + a)) {
                    if (in != null) l.add(leer(in));
                } catch (IOException e) {
                    // sin ese laboratorio
                }
            }
            todos = Collections.unmodifiableList(l);
        }
        return todos;
    }

    static Laboratorio leer(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        Map<String, String> v = new LinkedHashMap<>();
        List<String> mutantes = new ArrayList<>();
        String clave = null;
        StringBuilder bloque = null;
        String linea;
        while ((linea = r.readLine()) != null) {
            if (linea.startsWith("#")) continue;
            if (linea.startsWith("===")) {
                v.put("id", linea.substring(3).strip());
                continue;
            }
            if (bloque != null && (linea.startsWith("    ") || linea.isBlank())) {
                bloque.append(linea.length() >= 4 ? linea.substring(4) : "").append('\n');
                continue;
            }
            if (bloque != null) {
                guardar(v, mutantes, clave, bloque.toString());
                bloque = null;
            }
            int i = linea.indexOf(':');
            if (i <= 0) continue;
            String k = linea.substring(0, i).strip(), valor = linea.substring(i + 1).strip();
            if (valor.isEmpty()) {
                clave = k;
                bloque = new StringBuilder();
            } else {
                v.put(k, valor);
            }
        }
        if (bloque != null) guardar(v, mutantes, clave, bloque.toString());
        return new Laboratorio(v, mutantes);
    }

    private static void guardar(Map<String, String> v, List<String> mutantes, String clave, String texto) {
        if ("mutante".equals(clave)) mutantes.add(texto.stripTrailing() + "\n");
        else v.put(clave, texto.stripTrailing() + "\n");
    }
}
