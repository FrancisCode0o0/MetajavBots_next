package metajavbots.ejercicios;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * TEMARIO GUARDADO — La propuesta que la IA hizo de un temario, ya comprobada
 * (cada ejercicio, con su solución que pasa los casos y su plantilla que no),
 * guardada como campaña propia en la carpeta de las partidas
 * («temarios/<nombre>.txt»): sus temas y sus ejercicios en el formato de
 * res/ejercicios. Se juega después sin IA: sus ejercicios y un combate de
 * sparring por tema.
 */
public final class TemarioGuardado {

    public final String nombre;
    public final Set<String> temas;
    public final List<Ejercicio> ejercicios;
    public final Path archivo;

    private TemarioGuardado(String nombre, Set<String> temas, List<Ejercicio> ejercicios, Path archivo) {
        this.nombre = nombre;
        this.temas = temas;
        this.ejercicios = ejercicios;
        this.archivo = archivo;
    }

    /** Los bloques «=== id …» de ese texto cuyo id está en «ids», tal cual. */
    static String bloques(String texto, Collection<String> ids) {
        StringBuilder sb = new StringBuilder();
        String[] partes = ("\n" + texto.replace("\r", "")).split("\n(?==== )");
        for (String b : partes) {
            if (!b.startsWith("=== ")) continue;
            String id = b.substring(4, b.indexOf('\n') < 0 ? b.length() : b.indexOf('\n')).strip();
            if (ids.contains(id)) sb.append(b.strip()).append("\n\n");
        }
        return sb.toString();
    }

    /** Un nombre de archivo sin cosas raras. */
    static String limpio(String nombre) {
        String s = java.text.Normalizer.normalize(nombre.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "temario" : s.length() > 40 ? s.substring(0, 40) : s;
    }

    /**
     * Guarda en «carpeta»/temarios los ejercicios validados («ids») de la respuesta de la IA, con sus temas. Si ya hay
     * uno con ese nombre, los ejercicios nuevos se añaden a los que tenía (un temario crece con cada propuesta).
     */
    public static Path guardar(Path carpeta, String nombre, Collection<String> temas, String respuesta, Collection<String> ids) throws IOException {
        Path dir = carpeta.resolve("temarios");
        Files.createDirectories(dir);
        Path f = dir.resolve(limpio(nombre) + ".txt");
        Set<String> todosLosTemas = new LinkedHashSet<>(temas);
        String antes = "";
        if (Files.exists(f)) {
            TemarioGuardado viejo = leer(f);
            todosLosTemas.addAll(viejo.temas);
            List<String> viejos = new ArrayList<>();
            for (Ejercicio e : viejo.ejercicios) if (!ids.contains(e.id)) viejos.add(e.id);
            antes = bloques(Files.readString(f, StandardCharsets.UTF_8), viejos);
        }
        String s = "# temario: " + nombre.replace("\n", " ").strip() + "\n# temas: " + String.join(",", todosLosTemas) + "\n\n" + antes
                + bloques(respuesta, ids);
        Files.writeString(f, s, StandardCharsets.UTF_8);
        return f;
    }

    public static TemarioGuardado leer(Path f) throws IOException {
        String s = Files.readString(f, StandardCharsets.UTF_8);
        String nombre = f.getFileName().toString().replace(".txt", "");
        Set<String> temas = new LinkedHashSet<>();
        for (String l : s.split("\n")) {
            if (l.startsWith("# temario:")) nombre = l.substring(10).strip();
            if (l.startsWith("# temas:")) for (String t : l.substring(8).split(",")) if (!t.isBlank()) temas.add(t.strip());
        }
        List<Ejercicio> l = Ejercicio.leer(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), "temario:" + nombre);
        return new TemarioGuardado(nombre, temas, l, f);
    }

    /** Los temarios guardados en «carpeta»/temarios (los que no se pueden leer, se saltan). */
    public static List<TemarioGuardado> todos(Path carpeta) {
        List<TemarioGuardado> l = new ArrayList<>();
        Path dir = carpeta.resolve("temarios");
        if (!Files.isDirectory(dir)) return l;
        try (Stream<Path> s = Files.list(dir)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".txt")).sorted().toList()) {
                try {
                    l.add(leer(f));
                } catch (IOException | RuntimeException e) {
                    // uno roto no estropea los demás
                }
            }
        } catch (IOException e) {
            // sin carpeta legible: ninguno
        }
        return l;
    }
}
