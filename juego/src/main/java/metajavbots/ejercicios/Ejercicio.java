package metajavbots.ejercicios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * EJERCICIO — Un ejercicio corto que se comprueba solo (al estilo CodingBat
 * y CodeStepByStep). Se leen de res/ejercicios/ (el formato, en
 * docs/FORMATOS.md): su tema, su dificultad, el enunciado en tres idiomas,
 * la firma del método, la plantilla, la solución y los casos.
 */
public final class Ejercicio {

    /** Cómo se comprueba: un método que devuelve algo, lo que escribe, o una clase entera. */
    public enum Tipo { METODO, SALIDA, CLASE }

    /** Un caso: lo que se llama y lo que se espera. */
    public static final class Caso {
        public final String llamada, esperado;

        Caso(String llamada, String esperado) {
            this.llamada = llamada;
            this.esperado = esperado;
        }

        /** ¿Se espera una excepción («!ArithmeticException»)? */
        public boolean esperaExcepcion() {
            return esperado.startsWith("!");
        }

        @Override
        public String toString() {
            return llamada + " => " + esperado;
        }
    }

    public final String id, tema, firma, plantilla, solucion, archivo;
    public final int dificultad, java;
    public final Tipo tipo;
    public final Trilingue titulo, enunciado, pista;
    public final List<Caso> casos;
    private final Map<Idioma, String> plantillas = new EnumMap<>(Idioma.class);
    private final Map<Idioma, String> soluciones = new EnumMap<>(Idioma.class);
    private final Map<Idioma, List<Caso>> casosPorIdioma = new EnumMap<>(Idioma.class);
    /** Lo que desbloquea (reto:<id>) al resolverlo; "" si nada. */
    public final String desbloquea;
    /** La escena de su animación si no es la que toca por lo que devuelve: «giro» o «avance» (pantallas/EscenaDeArena), o "". */
    public final String escena;

    Ejercicio(Map<String, String> v, String archivo) {
        this.archivo = archivo;
        id = v.getOrDefault("id", "");
        tema = v.getOrDefault("tema", "01");
        dificultad = entero(v.get("dificultad"), 1);
        java = entero(v.get("java"), 8);
        tipo = tipoDe(v.get("tipo"));
        titulo = trilingue(v, "titulo");
        enunciado = trilingue(v, "enunciado");
        pista = trilingue(v, "pista");
        firma = v.getOrDefault("firma", "").strip();
        plantilla = v.getOrDefault("plantilla", "");
        solucion = v.getOrDefault("solucion", "");
        desbloquea = v.getOrDefault("desbloquea", "").strip();
        escena = v.getOrDefault("escena", "").strip();
        String casosRaw = v.getOrDefault("casos", "");
        casos = leerCasos(casosRaw);
        for (Idioma i : Idioma.values()) {
            String term = v.get("terminos." + i.codigo());
            String pl = v.containsKey("plantilla." + i.codigo()) ? v.get("plantilla." + i.codigo()) : aplicarTerminos(plantilla, term);
            String so = v.containsKey("solucion." + i.codigo()) ? v.get("solucion." + i.codigo()) : aplicarTerminos(solucion, term);
            String ca = v.containsKey("casos." + i.codigo()) ? v.get("casos." + i.codigo()) : aplicarTerminos(casosRaw, term);
            plantillas.put(i, pl);
            soluciones.put(i, so);
            casosPorIdioma.put(i, ca.equals(casosRaw) ? casos : leerCasos(ca));
        }
    }

    private static List<Caso> leerCasos(String texto) {
        List<Caso> l = new ArrayList<>();
        for (String linea : texto.split("\n")) {
            linea = linea.strip();
            if (linea.isEmpty()) continue;
            int i = linea.lastIndexOf("=>");
            if (i < 0) continue;
            l.add(new Caso(linea.substring(0, i).strip(), linea.substring(i + 2).strip()));
        }
        return Collections.unmodifiableList(l);
    }

    private static String aplicarTerminos(String s, String spec) {
        if (s == null || s.isEmpty() || spec == null || spec.isBlank()) return s;
        String res = s;
        for (String linea : spec.split("\n")) {
            String t = linea.strip();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int sep = t.indexOf("==>");
            if (sep <= 0) continue;
            String viejo = t.substring(0, sep).strip();
            String nuevo = t.substring(sep + 3).strip();
            res = res.replace(viejo, nuevo);
        }
        return res;
    }

    public String titulo(Idioma i) {
        return titulo.en(i);
    }

    public String plantilla(Idioma i) {
        return plantillas.getOrDefault(i == null ? Idioma.ES : i, plantilla);
    }

    public String solucion(Idioma i) {
        return soluciones.getOrDefault(i == null ? Idioma.ES : i, solucion);
    }

    public List<Caso> casos(Idioma i) {
        return casosPorIdioma.getOrDefault(i == null ? Idioma.ES : i, casos);
    }

    /** Lo que aparece en el editor al empezar: en un método, la firma con la plantilla dentro. */
    public String codigoInicial() {
        return codigoInicial(Idioma.ES);
    }

    public String codigoInicial(Idioma i) {
        String p = plantilla(i);
        if (tipo == Tipo.CLASE) return p;
        return firma + " {\n" + sangrar(p) + "}\n";
    }

    /** El código de la solución, como lo escribiría el jugador. */
    public String codigoSolucion() {
        return codigoSolucion(Idioma.ES);
    }

    public String codigoSolucion(Idioma i) {
        String s = solucion(i);
        if (tipo == Tipo.CLASE) return s;
        return firma + " {\n" + sangrar(s) + "}\n";
    }

    /** Los puntos de experiencia que da (más cuanto más difícil y más avanzado el tema). */
    public int experiencia() {
        Tema t = Tema.de(tema);
        int n = t == null ? 1 : t.numero();
        return 10 * dificultad + 2 * n;
    }

    private static String sangrar(String s) {
        StringBuilder b = new StringBuilder();
        for (String l : s.split("\n", -1)) {
            if (b.length() > 0 || !l.isEmpty()) b.append(l.isEmpty() ? "" : "    " + l).append('\n');
        }
        String r = b.toString();
        while (r.endsWith("\n\n")) r = r.substring(0, r.length() - 1);
        return r;
    }

    private static Trilingue trilingue(Map<String, String> v, String clave) {
        return new Trilingue(v.getOrDefault(clave + ".es", "").strip(), v.getOrDefault(clave + ".de", "").strip(),
                v.getOrDefault(clave + ".en", "").strip());
    }

    private static int entero(String s, int siNo) {
        try {
            return s == null ? siNo : Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }

    private static Tipo tipoDe(String s) {
        if (s == null) return Tipo.METODO;
        switch (s.strip().toLowerCase(Locale.ROOT)) {
            case "salida": return Tipo.SALIDA;
            case "clase": return Tipo.CLASE;
            default: return Tipo.METODO;
        }
    }

    // ------------------------------------------------------------------ leer

    /** Lee los ejercicios de un archivo (varios por archivo, cada uno tras «=== id»). */
    public static List<Ejercicio> leer(InputStream in, String archivo) throws IOException {
        List<Ejercicio> lista = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            Map<String, String> actual = null;
            String clave = null;
            StringBuilder bloque = null;
            String linea;
            while ((linea = r.readLine()) != null) {
                if (linea.startsWith("===")) {
                    if (actual != null) {
                        if (clave != null && bloque != null) actual.put(clave, bloque.toString());
                        lista.add(new Ejercicio(actual, archivo));
                    }
                    actual = new LinkedHashMap<>();
                    actual.put("id", linea.substring(3).strip());
                    clave = null;
                    bloque = null;
                    continue;
                }
                if (actual == null) continue;
                boolean sangrada = linea.startsWith(" ") || linea.startsWith("\t");
                if (bloque != null && (sangrada || linea.isBlank())) {
                    // Una línea de un bloque (plantilla, solución, casos): sin sus 4 espacios.
                    String l = linea.replace("\t", "    ");
                    bloque.append(l.length() >= 4 ? l.substring(4) : l.strip()).append('\n');
                    continue;
                }
                if (linea.isBlank() || linea.strip().startsWith("#")) continue;
                if (clave != null && bloque != null) {
                    actual.put(clave, bloque.toString());
                    clave = null;
                    bloque = null;
                }
                int dos = linea.indexOf(':');
                if (dos <= 0) continue;
                String k = linea.substring(0, dos).strip();
                String valor = linea.substring(dos + 1).strip();
                if (valor.isEmpty()) {
                    clave = k;
                    bloque = new StringBuilder();
                } else {
                    // Un comentario al final de una línea simple («dificultad: 2   # …»).
                    if (!k.startsWith("enunciado") && !k.startsWith("pista") && !k.startsWith("titulo") && !k.equals("firma")) {
                        int h = valor.indexOf(" #");
                        if (h >= 0) valor = valor.substring(0, h).strip();
                    }
                    actual.merge(k, valor, (a, b) -> a + " " + b);
                }
            }
            if (actual != null) {
                if (clave != null && bloque != null) actual.put(clave, bloque.toString());
                lista.add(new Ejercicio(actual, archivo));
            }
        }
        return lista;
    }

    @Override
    public String toString() {
        return id;
    }
}
