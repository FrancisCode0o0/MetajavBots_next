package castillafantasx.editor.codigo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * PARTES — Las partes de cada cosa del mundo que se pueden tocar por separado
 * (su diálogo, su comportamiento, su combate, su aspecto…), y dónde está cada
 * una en el código. Se leen de res/editor/partes.txt: una sección por grupo
 * y una línea por parte, «parte = archivo # texto de la línea», con huecos
 * como {id} o {nudo} que llena cada cosa. Así cada cosa nueva queda enlazada
 * a su trozo de código sola, sin escribir nada a mano.
 */
public final class Partes {

    /** Una parte ya encontrada: su nombre, su archivo, el texto buscado, la línea (desde 0) a la que se salta y hasta dónde llega. */
    public static final class Parte {
        public final String nombre, ruta, ancla;
        public final int linea, fin;
        /** Dónde empieza el trozo que abarca (en un gancho, el principio de su método; si no, la misma línea). */
        public final int desde;
        /** ¿Es lo de todas las de su grupo (no lo propio de esta cosa)? */
        public final boolean general;

        Parte(String nombre, String ruta, String ancla, int linea, int desde, int fin, boolean general) {
            this.nombre = nombre;
            this.ruta = ruta;
            this.ancla = ancla;
            this.linea = linea;
            this.desde = desde;
            this.fin = fin;
            this.general = general;
        }

        /** ¿Cae esa línea (desde 0) de ese archivo dentro de esta parte? */
        public boolean contiene(Path archivo, int otraLinea) {
            Path suyo = resolver.apply(ruta);
            return suyo != null && archivo != null && otraLinea >= desde && otraLinea <= fin
                    && suyo.toAbsolutePath().normalize().equals(archivo.toAbsolutePath().normalize());
        }

        @Override
        public String toString() {
            return nombre + " (" + ruta + ":" + (linea + 1) + "-" + (fin + 1) + ")";
        }
    }

    /** Una línea del archivo: la parte, dónde buscarla y si es lo general del grupo («*» delante). */
    private static final class Plantilla {
        final String parte, ruta, ancla;
        final boolean general;

        Plantilla(String parte, String ruta, String ancla, boolean general) {
            this.parte = parte;
            this.ruta = ruta;
            this.ancla = ancla;
            this.general = general;
        }
    }

    private static final Pattern HUECO = Pattern.compile("\\{(\\w+)\\}");

    /** El archivo, en disco (el que se cambia a mano) y dentro del juego. */
    public static Path archivo = Path.of("res/editor/partes.txt");
    private static final String RECURSO = "/editor/partes.txt";
    /** Cómo se encuentra un archivo de código por su ruta (el juego pone el suyo: los fuentes viajan en otra carpeta en la versión portátil). */
    public static Function<String, Path> resolver = ruta -> Files.exists(Path.of(ruta)) ? Path.of(ruta) : null;

    private static Map<String, List<Plantilla>> porGrupo;
    private static Map<String, String> nombresDeGrupo;
    /** Lo ya leído de cada archivo, para no leerlo cada vez (se olvida al leer de nuevo). */
    private static final Map<Path, List<String>> leidos = new LinkedHashMap<>();
    /** Las partes ya buscadas de cada cosa (por su clave y sus datos), por lo mismo. */
    private static final Map<String, List<Parte>> buscadas = new LinkedHashMap<>();

    private Partes() { }

    /** Lee de nuevo res/editor/partes.txt (y olvida los archivos de código ya leídos). */
    public static void leer() {
        Map<String, List<Plantilla>> grupos = new LinkedHashMap<>();
        Map<String, String> nombres = new LinkedHashMap<>();
        String seccion = null;
        try (BufferedReader lector = abrir()) {
            String linea;
            while (lector != null && (linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                if (linea.startsWith("[") && linea.endsWith("]")) {
                    seccion = linea.substring(1, linea.length() - 1).trim();
                    continue;
                }
                int igual = linea.indexOf('=');
                if (seccion == null || igual < 0) continue;
                String clave = linea.substring(0, igual).trim(), resto = linea.substring(igual + 1).trim();
                if (seccion.equals("grupos")) {
                    nombres.put(clave, resto);
                    continue;
                }
                int almohadilla = resto.indexOf(" # ");
                String ruta = (almohadilla < 0 ? resto : resto.substring(0, almohadilla)).trim();
                String ancla = almohadilla < 0 ? "" : resto.substring(almohadilla + 3).trim();
                boolean general = clave.startsWith("*");
                grupos.computeIfAbsent(seccion, k -> new ArrayList<>()).add(new Plantilla(general ? clave.substring(1).trim() : clave, ruta, ancla, general));
            }
        } catch (IOException e) {
            System.err.println("No se ha podido leer " + archivo + ": " + e.getMessage());
        }
        porGrupo = grupos;
        nombresDeGrupo = nombres;
        olvidarLoLeido();
    }

    private static BufferedReader abrir() throws IOException {
        if (Files.exists(archivo)) return Files.newBufferedReader(archivo, StandardCharsets.UTF_8);
        InputStream in = Partes.class.getResourceAsStream(RECURSO);
        return in == null ? null : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Los grupos, en el orden del archivo: «persona» → «Personas». */
    public static Map<String, String> grupos() {
        if (porGrupo == null) leer();
        return nombresDeGrupo;
    }

    /** Los nombres de las partes que puede tener una cosa de ese grupo, sin repetir. */
    public static List<String> nombresDe(String grupo) {
        if (porGrupo == null) leer();
        List<String> nombres = new ArrayList<>();
        for (Plantilla p : porGrupo.getOrDefault(grupo, new ArrayList<>())) {
            if (!nombres.contains(p.parte)) nombres.add(p.parte);
        }
        return nombres;
    }

    /**
     * Las partes de esa cosa que están de verdad en el código, en el orden del
     * archivo. Si una parte tiene varias líneas, vale la primera que se
     * encuentra; la que no se encuentra (o le falta un dato), no sale.
     */
    public static List<Parte> de(Cosa cosa) {
        if (porGrupo == null) leer();
        String clave = cosa.clave() + cosa.datos;
        List<Parte> yaBuscadas = buscadas.get(clave);
        if (yaBuscadas != null) return yaBuscadas;
        List<Parte> partes = new ArrayList<>();
        List<String> hechas = new ArrayList<>();
        for (Plantilla p : porGrupo.getOrDefault(cosa.grupo, new ArrayList<>())) {
            if (hechas.contains(p.parte)) continue;
            String ruta = llenar(p.ruta, cosa), ancla = llenar(p.ancla, cosa);
            if (ruta == null || ancla == null) continue;
            Parte encontrada = buscar(p.parte, ruta, ancla, p.general);
            if (encontrada != null) {
                partes.add(encontrada);
                hechas.add(p.parte);
            }
        }
        buscadas.put(clave, partes);
        return partes;
    }

    /** La parte de ese nombre de esa cosa, o null. */
    public static Parte de(Cosa cosa, String parte) {
        for (Parte p : de(cosa)) {
            if (p.nombre.equals(parte)) return p;
        }
        return null;
    }

    /** Pone los datos de la cosa en los huecos; null si le falta alguno. */
    static String llenar(String plantilla, Cosa cosa) {
        Matcher m = HUECO.matcher(plantilla);
        StringBuilder lleno = new StringBuilder();
        while (m.find()) {
            String valor = cosa.datos.get(m.group(1));
            if (valor == null) return null;
            m.appendReplacement(lleno, Matcher.quoteReplacement(valor));
        }
        m.appendTail(lleno);
        return lleno.toString();
    }

    /** Busca esa línea en ese archivo (o, si la ruta acaba en «/», en el primer archivo de esa carpeta que la lleve). */
    private static Parte buscar(String nombre, String ruta, String ancla, boolean general) {
        List<String> candidatos = new ArrayList<>();
        if (ruta.endsWith("/")) {
            Path dir = resolver.apply(ruta);
            if (dir == null || !Files.isDirectory(dir)) return null;
            try (Stream<Path> archivos = Files.list(dir)) {
                archivos.filter(Files::isRegularFile).sorted().forEach(a -> candidatos.add(ruta + a.getFileName()));
            } catch (IOException e) {
                return null;
            }
        } else {
            candidatos.add(ruta);
        }
        for (String candidato : candidatos) {
            List<String> lineas = lineasDe(candidato);
            if (lineas == null) continue;
            int donde = ancla.isEmpty() ? 0 : lineaCon(lineas, ancla);
            if (donde < 0) continue;
            int desde = donde, fin = ancla.isEmpty() ? lineas.size() - 1 : Bloque.fin(lineas, donde, candidato);
            if (candidato.endsWith(".java") && lineas.get(donde).trim().startsWith("//")) {
                // Un gancho (un comentario suelto): la parte es el método entero en que está.
                int[] envuelve = Bloque.envolvente(lineas, donde);
                desde = envuelve[0];
                fin = envuelve[1];
            }
            return new Parte(nombre, candidato, ancla, donde, desde, fin, general);
        }
        return null;
    }

    /** La primera línea que lleva ese texto, o -1. */
    public static int lineaCon(List<String> lineas, String texto) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).contains(texto)) return i;
        }
        return -1;
    }

    /** Las líneas de un archivo de código (leídas una vez); null si no está. */
    public static List<String> lineasDe(String ruta) {
        Path archivo = resolver.apply(ruta);
        if (archivo == null || !Files.isRegularFile(archivo)) return null;
        return leidos.computeIfAbsent(archivo, a -> {
            try {
                return Files.readAllLines(a, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        });
    }

    /** Olvida lo leído de los archivos de código (tras guardar uno o cambiar un nombre). */
    public static void olvidarLoLeido() {
        leidos.clear();
        buscadas.clear();
    }

    /**
     * Del código al mundo: las cosas que usan esa línea de ese archivo, las
     * que la tienen en una parte propia primero; detrás, las que la tienen
     * solo en lo general de su grupo.
     */
    public static List<Cosa> quienesUsan(List<Cosa> cosas, Path ruta, int linea) {
        List<Cosa> propias = new ArrayList<>(), generales = new ArrayList<>();
        for (Cosa c : cosas) {
            boolean propia = false, general = false;
            for (Parte p : de(c)) {
                if (!p.contiene(ruta, linea)) continue;
                if (p.general) general = true;
                else propia = true;
            }
            if (propia) propias.add(c);
            else if (general) generales.add(c);
        }
        propias.addAll(generales);
        return propias;
    }

    /** ¿Toca esa línea a esa cosa en una parte propia (no solo en lo general)? */
    public static boolean propia(Cosa cosa, Path ruta, int linea) {
        for (Parte p : de(cosa)) {
            if (!p.general && p.contiene(ruta, linea)) return true;
        }
        return false;
    }
}
