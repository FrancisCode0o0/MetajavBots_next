package metajavbots.codigo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import metajavbots.textos.Idioma;

/**
 * PAQUETES — Catálogo estilo Javadoc de los paquetes y clases permitidos en
 * MetajavBots (res/datos/paquetes.txt), alineado con Vigilante y Cargador.
 * Ofrece navegación por paquete y componente, buscador con autocompletado
 * sin tildes ni mayúsculas, traducción de metajavbots.api por idioma y
 * resumen para el entrenador (sin IA y con IA).
 */
public final class Paquetes {

    /** Tipo de componente Java dentro de un paquete. */
    public enum Tipo {
        INTERFAZ("interfaz"),
        CLASE("clase"),
        ENUM("enum"),
        ANOTACION("anotacion"),
        EXCEPCION("excepcion"),
        ERROR("error");

        public final String clave;

        Tipo(String clave) {
            this.clave = clave;
        }

        public static Tipo de(String texto) {
            if (texto == null) return CLASE;
            String n = normalizar(texto);
            for (Tipo t : values()) if (t.clave.equals(n)) return t;
            return CLASE;
        }
    }

    /** Un componente (clase, interfaz, enum, anotación, excepción o error) dentro de un paquete. */
    public static final class Componente {
        public final String id;
        public final String paquete;
        public final Tipo tipo;
        public final boolean soloEjercicio;
        private final Map<Idioma, String> nombres = new EnumMap<>(Idioma.class);
        private final Map<Idioma, String> descripciones = new EnumMap<>(Idioma.class);
        private final Map<Idioma, List<String>> miembros = new EnumMap<>(Idioma.class);
        private final String ejemplo;

        Componente(String paquete, String id, Map<String, String> v, Map<String, List<String>> bloques) {
            this.paquete = paquete;
            this.id = id;
            this.tipo = Tipo.de(v.get("tipo"));
            this.soloEjercicio = "true".equalsIgnoreCase(v.getOrDefault("solo_ejercicio", "false"));
            for (Idioma i : Idioma.values()) {
                String c = i.codigo();
                String nom = v.get("nombre." + c);
                nombres.put(i, nom == null || nom.isBlank() ? id : nom);
                descripciones.put(i, v.getOrDefault(c, ""));
                miembros.put(i, Collections.unmodifiableList(new ArrayList<>(bloques.getOrDefault("miembros." + c, List.of()))));
            }
            this.ejemplo = String.join("\n", bloques.getOrDefault("ejemplo", List.of())).stripTrailing();
        }

        /** Nombre visible del componente en el idioma activo (p. ej. Enemigo/Gegner/Enemy). */
        public String nombre(Idioma idioma) {
            String n = nombres.get(idioma);
            return n == null || n.isBlank() ? id : n;
        }

        /** Nombre completo con paquete (p. ej. java.util.ArrayList o metajavbots.api.Enemy). */
        public String nombreCompleto(Idioma idioma) {
            return paquete + "." + nombre(idioma);
        }

        /** Nombre interno JVM con barras (p. ej. java/util/ArrayList) para contrastar con Vigilante. */
        public String nombreInterno(Idioma idioma) {
            return paquete.replace('.', '/') + "/" + nombre(idioma);
        }

        public String descripcion(Idioma idioma) {
            String s = descripciones.get(idioma);
            return s == null || s.isBlank() ? descripciones.getOrDefault(Idioma.ES, "") : s;
        }

        public List<String> miembros(Idioma idioma) {
            List<String> l = miembros.get(idioma);
            return l == null || l.isEmpty() ? miembros.getOrDefault(Idioma.ES, List.of()) : l;
        }

        /** Ejemplo Java adaptado al idioma del jugador (traduce metajavbots.api si corresponde). */
        public String ejemplo(Idioma idioma) {
            if (ejemplo.isEmpty()) return "";
            String texto = "metajavbots.api".equals(paquete) ? TraductorDeApi.traducir(ejemplo, idioma) : ejemplo;
            StringBuilder sb = new StringBuilder();
            for (String linea : texto.split("\n", -1)) {
                int c = linea.indexOf("//");
                if (c >= 0 && linea.indexOf(" || ", c) > 0) {
                    String[] partes = linea.substring(c + 2).split("\\|\\|");
                    int idx = Math.min(idioma.ordinal(), partes.length - 1);
                    linea = linea.substring(0, c) + "// " + partes[idx].strip();
                }
                if (sb.length() > 0) sb.append('\n');
                sb.append(linea);
            }
            return sb.toString();
        }
    }

    /** Un paquete Java o de MetajavBots con su explicación y lista de componentes permitidos. */
    public static final class Paquete {
        public final String nombre;
        public final String sentenciaImport;
        public final boolean automatico;
        public final boolean enPlantilla;
        public final boolean soloEjercicio;
        public final List<Componente> componentes;
        private final Map<Idioma, String> resumenes = new EnumMap<>(Idioma.class);
        private final Map<Idioma, List<String>> descripciones = new EnumMap<>(Idioma.class);
        private final Map<Idioma, String> bloqueados = new EnumMap<>(Idioma.class);

        Paquete(String nombre, Map<String, String> v, Map<String, List<String>> bloques, List<Componente> componentes) {
            this.nombre = nombre;
            this.sentenciaImport = v.getOrDefault("import", "import " + nombre + ".*;");
            this.automatico = "true".equalsIgnoreCase(v.getOrDefault("auto", "false"));
            this.enPlantilla = "true".equalsIgnoreCase(v.getOrDefault("plantilla", "false"));
            this.soloEjercicio = "true".equalsIgnoreCase(v.getOrDefault("solo_ejercicio", "false"));
            this.componentes = Collections.unmodifiableList(new ArrayList<>(componentes));
            for (Idioma i : Idioma.values()) {
                String c = i.codigo();
                resumenes.put(i, v.getOrDefault("resumen." + c, ""));
                descripciones.put(i, Collections.unmodifiableList(new ArrayList<>(bloques.getOrDefault(c, List.of()))));
                bloqueados.put(i, v.getOrDefault("bloqueado." + c, ""));
            }
        }

        public String resumen(Idioma idioma) {
            String s = resumenes.get(idioma);
            return s == null || s.isBlank() ? resumenes.getOrDefault(Idioma.ES, "") : s;
        }

        public List<String> parrafos(Idioma idioma) {
            List<String> l = descripciones.get(idioma);
            return l == null || l.isEmpty() ? descripciones.getOrDefault(Idioma.ES, List.of()) : l;
        }

        public String descripcion(Idioma idioma) {
            return String.join("\n\n", parrafos(idioma));
        }

        public String bloqueado(Idioma idioma) {
            String s = bloqueados.get(idioma);
            return s == null || s.isBlank() ? bloqueados.getOrDefault(Idioma.ES, "") : s;
        }

        public List<Componente> porTipo(Tipo tipo) {
            List<Componente> l = new ArrayList<>();
            for (Componente c : componentes) if (c.tipo == tipo) l.add(c);
            return l;
        }

        public Componente componente(String idONombre) {
            if (idONombre == null) return null;
            for (Componente c : componentes) {
                if (c.id.equalsIgnoreCase(idONombre)) return c;
                for (Idioma i : Idioma.values()) if (c.nombre(i).equalsIgnoreCase(idONombre)) return c;
            }
            return null;
        }
    }

    /** Resultado de búsqueda / autocompletado: puede apuntar a un paquete entero o a un componente concreto. */
    public static final class Sugerencia {
        public final Paquete paquete;
        public final Componente componente;
        public final int puntuacion;

        Sugerencia(Paquete paquete, Componente componente, int puntuacion) {
            this.paquete = paquete;
            this.componente = componente;
            this.puntuacion = puntuacion;
        }

        public boolean esPaquete() {
            return componente == null;
        }

        public String titulo(Idioma idioma) {
            return componente == null ? paquete.nombre : componente.nombre(idioma);
        }

        public String subtitulo(Idioma idioma) {
            return componente == null ? paquete.resumen(idioma) : paquete.nombre;
        }
    }

    private static List<Paquete> todos;

    private Paquetes() {
    }

    /** Todos los paquetes documentados, en orden didáctico. */
    public static synchronized List<Paquete> todos() {
        if (todos == null) {
            todos = Collections.unmodifiableList(leer("/datos/paquetes.txt"));
        }
        return todos;
    }

    /** Busca un paquete por nombre exacto (sin distinguir mayúsculas), o null. */
    public static Paquete de(String nombre) {
        if (nombre == null) return null;
        for (Paquete p : todos()) if (p.nombre.equalsIgnoreCase(nombre.strip())) return p;
        return null;
    }

    /** Normaliza texto para búsquedas: minúsculas, sin tildes ni diéresis (ß -> ss). */
    public static String normalizar(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9._ ]", " ").replaceAll("\\s+", " ").strip();
    }

    /**
     * Busca paquetes y componentes por nombre, palabras clave o miembros (sin distinguir mayúsculas ni tildes).
     * Devuelve hasta «maximo» sugerencias ordenadas por relevancia.
     */
    public static List<Sugerencia> buscar(String consulta, Idioma idioma, int maximo) {
        String q = normalizar(consulta);
        if (q.isEmpty() || maximo <= 0) return List.of();
        String[] palabras = q.split(" ");
        List<Sugerencia> l = new ArrayList<>();
        for (Paquete p : todos()) {
            int ptsP = puntuarPaquete(p, q, palabras, idioma);
            if (ptsP > 0) l.add(new Sugerencia(p, null, ptsP));
            for (Componente c : p.componentes) {
                int ptsC = puntuarComponente(p, c, q, palabras, idioma);
                if (ptsC > 0) l.add(new Sugerencia(p, c, ptsC));
            }
        }
        l.sort((a, b) -> {
            int d = Integer.compare(b.puntuacion, a.puntuacion);
            if (d != 0) return d;
            if (a.esPaquete() != b.esPaquete()) return a.esPaquete() ? -1 : 1;
            return a.titulo(idioma).compareToIgnoreCase(b.titulo(idioma));
        });
        return l.size() > maximo ? l.subList(0, maximo) : l;
    }

    private static int puntuarPaquete(Paquete p, String q, String[] palabras, Idioma idioma) {
        String nom = normalizar(p.nombre);
        int pts = 0;
        if (nom.equals(q)) pts += 300;
        else if (nom.startsWith(q)) pts += 200;
        else if (nom.contains(q)) pts += 130;
        else if (q.contains(" " + nom + " ") || q.endsWith(" " + nom) || q.startsWith(nom + " ")) pts += 220;
        String todo = nom + " " + normalizar(p.resumen(idioma)) + " " + normalizar(p.descripcion(idioma)) + " " + normalizar(p.bloqueado(idioma));
        if (todasLasPalabras(todo, palabras)) pts += 40;
        else if (pts == 0) return 0;
        return pts;
    }

    private static int puntuarComponente(Paquete p, Componente c, String q, String[] palabras, Idioma idioma) {
        String nom = normalizar(c.nombre(idioma));
        String completo = normalizar(p.nombre + "." + c.nombre(idioma));
        String idNorm = normalizar(c.id);
        int pts = 0;
        if (nom.equals(q) || completo.equals(q) || idNorm.equals(q)) pts += 320;
        else if (nom.startsWith(q) || completo.startsWith(q) || idNorm.startsWith(q)) pts += 190;
        else if (nom.contains(q) || completo.contains(q)) pts += 120;
        else if (nom.length() >= 4 && (q.contains(" " + nom + " ") || q.endsWith(" " + nom) || q.startsWith(nom + " "))) pts += 170;
        StringBuilder miembros = new StringBuilder();
        for (String m : c.miembros(idioma)) miembros.append(' ').append(m);
        String todo = completo + " " + idNorm + " " + normalizar(c.descripcion(idioma)) + " " + normalizar(miembros.toString());
        if (todasLasPalabras(todo, palabras)) pts += 35;
        else if (pts == 0) return 0;
        return pts;
    }

    private static boolean todasLasPalabras(String texto, String[] palabras) {
        boolean alguna = false;
        for (String w : palabras) {
            if (w.isEmpty()) continue;
            alguna = true;
            if (!texto.contains(w)) return false;
        }
        return alguna;
    }

    /** Resumen en inglés de los paquetes permitidos y bloqueados por la caja de arena, para el contexto de la IA. */
    public static String resumenParaIa() {
        StringBuilder sb = new StringBuilder("Allowed packages in MetajavBots sandbox (Vigilante/Cargador): ");
        List<Paquete> ps = todos();
        for (int i = 0; i < ps.size(); i++) {
            Paquete p = ps.get(i);
            if (i > 0) sb.append(", ");
            sb.append(p.nombre);
            if (p.automatico) sb.append(" (auto-imported)");
            else if (p.enPlantilla) sb.append(" (in robot template)");
            else if (p.soloEjercicio) sb.append(" (exercises/lab only)");
        }
        sb.append(". Blocked by sandbox (NEVER recommend): java.io.File*, java.nio.file, java.net, java.lang.reflect, java.sql, javax.*, "
                + "Runtime, ProcessBuilder, System.exit, System.in, System.getenv, java.util.prefs/zip/jar/logging, "
                + "and threads/executors/CompletableFuture in robots (allowed only in exercises).");
        return sb.toString();
    }

    // ---------------------------------------------------------------- lectura de res/datos/paquetes.txt

    private static List<Paquete> leer(String recurso) {
        List<Paquete> resultado = new ArrayList<>();
        try (InputStream in = Paquetes.class.getResourceAsStream(recurso)) {
            if (in == null) return resultado;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            EstadoLectura st = new EstadoLectura();
            String linea;
            while ((linea = r.readLine()) != null) {
                procesarLinea(linea, st, resultado);
            }
            st.cerrarPaquete(resultado);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer res" + recurso, e);
        }
        return resultado;
    }

    private static void procesarLinea(String linea, EstadoLectura st, List<Paquete> salida) {
        if (linea.startsWith("#")) return;
        if (linea.startsWith("=== paquete ")) {
            st.cerrarPaquete(salida);
            st.paqueteNombre = linea.substring("=== paquete ".length()).strip();
            st.paqValores = new LinkedHashMap<>();
            st.paqBloques = new LinkedHashMap<>();
            st.componentes = new ArrayList<>();
            st.bloque = null;
            return;
        }
        if (linea.startsWith("--- ")) {
            st.cerrarComponente();
            st.compId = linea.substring(4).strip();
            st.compValores = new LinkedHashMap<>();
            st.compBloques = new LinkedHashMap<>();
            st.bloque = null;
            return;
        }
        if (st.paqueteNombre == null) return;
        if (linea.isBlank()) {
            if ("ejemplo".equals(st.bloque) && st.compId != null) st.compBloques.get(st.bloque).add("");
            return;
        }
        if (linea.startsWith("    ") && st.bloque != null) {
            String s = linea.substring(4);
            boolean codigo = "ejemplo".equals(st.bloque);
            Map<String, List<String>> destino = st.compId != null ? st.compBloques : st.paqBloques;
            if (codigo || !s.isBlank()) destino.get(st.bloque).add(codigo ? s.stripTrailing() : s.strip());
            return;
        }
        if (!linea.startsWith(" ") && linea.contains(":")) {
            int i = linea.indexOf(':');
            String clave = linea.substring(0, i).strip();
            String valor = linea.substring(i + 1).strip();
            Map<String, String> vals = st.compId != null ? st.compValores : st.paqValores;
            Map<String, List<String>> blqs = st.compId != null ? st.compBloques : st.paqBloques;
            if (valor.isEmpty()) {
                st.bloque = clave;
                blqs.put(clave, new ArrayList<>());
            } else {
                st.bloque = null;
                vals.put(clave, valor);
            }
        }
    }

    private static final class EstadoLectura {
        String paqueteNombre;
        Map<String, String> paqValores;
        Map<String, List<String>> paqBloques;
        List<Componente> componentes;
        String compId;
        Map<String, String> compValores;
        Map<String, List<String>> compBloques;
        String bloque;

        void cerrarComponente() {
            if (compId != null && paqueteNombre != null) {
                componentes.add(new Componente(paqueteNombre, compId, compValores, compBloques));
            }
            compId = null;
            compValores = null;
            compBloques = null;
            bloque = null;
        }

        void cerrarPaquete(List<Paquete> salida) {
            cerrarComponente();
            if (paqueteNombre != null) {
                salida.add(new Paquete(paqueteNombre, paqValores, paqBloques, componentes));
            }
            paqueteNombre = null;
        }
    }
}
