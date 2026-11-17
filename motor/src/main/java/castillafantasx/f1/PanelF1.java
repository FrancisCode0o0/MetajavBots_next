package castillafantasx.f1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PANEL DE F1 — Lo que F1 recuerda aparte de la fila elegida: lo que se está
 * buscando, las favoritas (a mano arriba, con Ctrl+1…9) y las usadas hace poco.
 * Las favoritas y los recientes se guardan en f1.txt, como teclas.txt.
 */
public final class PanelF1 {

    /** Dónde se guardan las favoritas y los recientes (las pruebas lo cambian por una carpeta temporal). */
    public static Path archivo = Path.of("f1.txt");

    // TODO-FRAN: cuántas favoritas y recientes caben arriba y cuántos resultados enseña el buscador.
    public static final int MAXIMO_DE_FAVORITAS = 9, MAXIMO_DE_RECIENTES = 6, MAXIMO_DE_RESULTADOS = 14;
    public static final int LARGO_MAXIMO_DE_BUSQUEDA = 24, LETRAS_PARA_BUSCAR_DENTRO = 4;
    /** Las favoritas de fábrica, si aún no hay f1.txt. */
    public static final String[] FAVORITAS_DE_FABRICA = { "invencible", "sanar", "arena", "hora", "pajaro", "minimapa" };

    /** Lo escrito en el buscador ("" si no se busca). */
    public String busqueda = "";
    /** Los id de las favoritas, en su orden. */
    public final List<String> favoritas = new ArrayList<>();
    /** Los id de las usadas hace poco, la última primero. */
    public final List<String> recientes = new ArrayList<>();

    public PanelF1() {
        cargar();
    }

    /** Al abrir F1: el buscador, vacío. */
    public void abrir() {
        busqueda = "";
    }

    public boolean buscando() {
        return !busqueda.isEmpty();
    }

    /** Una letra más en el buscador. */
    public void escribir(char letra) {
        if (busqueda.length() >= LARGO_MAXIMO_DE_BUSQUEDA) return;
        if (letra == ' ' && (busqueda.isEmpty() || busqueda.endsWith(" "))) return;     // sin espacios al principio ni dobles
        busqueda += letra;
    }

    /** Borra la última letra del buscador. */
    public void borrar() {
        if (buscando()) busqueda = busqueda.substring(0, busqueda.length() - 1);
    }

    // ------------------------------------------------------------------
    // El buscador
    // ------------------------------------------------------------------

    /**
     * Las herramientas que casan con lo escrito, las mejores primero: las que
     * empiezan así, luego las que tienen una palabra que empieza así, luego
     * las que tienen todas las palabras escritas (en el nombre, la fila, el
     * tema o sus palabras de más). A igualdad, las usadas hace poco y luego en su orden.
     */
    public List<Herramienta> resultados(List<Herramienta> todas, List<String> filas) {
        String q = normalizar(busqueda).trim();
        List<Herramienta> casan = new ArrayList<>();
        List<Integer> notas = new ArrayList<>();
        if (q.isEmpty()) return casan;
        for (Herramienta h : todas) {
            int nota = nota(h, q, filas);
            if (nota < 0) continue;
            // Las usadas hace poco suben dentro de su nota.
            int reciente = recientes.indexOf(h.id);
            int clave = nota * 100 + (reciente >= 0 ? reciente : 50);
            int donde = 0;
            while (donde < notas.size() && notas.get(donde) <= clave) donde++;
            casan.add(donde, h);
            notas.add(donde, clave);
        }
        return casan.size() > MAXIMO_DE_RESULTADOS ? new ArrayList<>(casan.subList(0, MAXIMO_DE_RESULTADOS)) : casan;
    }

    /** Lo bien que casa una herramienta con lo escrito: 0 (empieza así) a 3 (lo tiene dentro), o -1 si no casa. */
    private static int nota(Herramienta h, String q, List<String> filas) {
        String nombre = normalizar(h.nombre);
        return nota(nombre, nombre + " " + normalizar(h.fila < filas.size() ? filas.get(h.fila) : "") + " "
                + (h.tema != null ? normalizar(h.tema.nombre) : "") + " " + h.palabras, q);
    }

    /** Lo mismo, con el nombre y todo su texto ya sin tildes (lo usa también MenuComodo). */
    static int nota(String nombre, String todo, String q) {
        if (nombre.startsWith(q)) return 0;
        if (nombre.contains(" " + q)) return 1;
        boolean todas = true;
        for (String palabra : q.split(" +")) {
            if (!(" " + todo).contains(" " + palabra)) todas = false;
        }
        if (todas) return 2;
        // Dentro de otra palabra, solo con 4 letras o más («dar» no casa con «nadar»).
        return q.length() >= LETRAS_PARA_BUSCAR_DENTRO && todo.contains(q) ? 3 : -1;
    }

    /** Lo que falta por escribir del nombre de esa herramienta, si empieza por lo escrito (el autocompletado), o "". */
    public String autocompletado(Herramienta h) {
        if (h == null || !buscando()) return "";
        String nombre = normalizar(h.nombre);
        String q = normalizar(busqueda);
        return nombre.startsWith(q) && h.nombre.length() > busqueda.length() ? h.nombre.substring(busqueda.length()) : "";
    }

    /** Completa lo escrito con el autocompletado (Tab). Devuelve false si no había nada que completar. */
    public boolean completar(Herramienta h) {
        String resto = autocompletado(h);
        if (resto.isEmpty()) return false;
        busqueda = (busqueda + resto).substring(0, Math.min(LARGO_MAXIMO_DE_BUSQUEDA, busqueda.length() + resto.length()));
        return true;
    }

    /** En minúsculas y sin tildes (la ñ queda en n): "Galería" → "galeria". */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes;
    }

    // ------------------------------------------------------------------
    // Favoritas y recientes
    // ------------------------------------------------------------------

    public boolean esFavorita(Herramienta h) {
        return h != null && favoritas.contains(h.id);
    }

    /** La pone o la quita de las favoritas (Ctrl+D o la estrella). Devuelve true si ahora es favorita. */
    public boolean cambiarFavorita(Herramienta h) {
        if (h == null || h.tema == null) return false;
        boolean ahora;
        if (favoritas.remove(h.id)) {
            ahora = false;
        } else {
            if (favoritas.size() >= MAXIMO_DE_FAVORITAS) favoritas.remove(favoritas.size() - 1);
            favoritas.add(h.id);
            ahora = true;
        }
        guardar();
        return ahora;
    }

    /** Se ha usado esa herramienta: pasa la primera de los recientes. */
    public void usada(Herramienta h) {
        if (h == null || h.tema == null) return;        // «Cerrar F1» no cuenta
        recientes.remove(h.id);
        recientes.add(0, h.id);
        while (recientes.size() > MAXIMO_DE_RECIENTES) recientes.remove(recientes.size() - 1);
        guardar();
    }

    /** Las herramientas de una lista de id (favoritas o recientes) que existen ahora, en su orden. */
    public static List<Herramienta> deLista(List<String> ids, List<Herramienta> todas) {
        List<Herramienta> lista = new ArrayList<>();
        for (String id : ids) {
            Herramienta h = CatalogoF1.deId(todas, id);
            if (h != null) lista.add(h);
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // f1.txt
    // ------------------------------------------------------------------

    /** Las favoritas y los recientes, como de fábrica (y guardado). */
    public void aFabrica() {
        favoritas.clear();
        favoritas.addAll(java.util.Arrays.asList(FAVORITAS_DE_FABRICA));
        recientes.clear();
        guardar();
    }

    /** ¿Están las favoritas y los recientes como de fábrica? */
    public boolean deFabrica() {
        return recientes.isEmpty() && favoritas.equals(java.util.Arrays.asList(FAVORITAS_DE_FABRICA));
    }

    /** Vuelve a leer f1.txt (tras deshacer «lo de fábrica»). */
    public void releer() {
        cargar();
    }

    private void cargar() {
        favoritas.clear();
        recientes.clear();
        if (!Files.exists(archivo)) {
            favoritas.addAll(Arrays.asList(FAVORITAS_DE_FABRICA));
            return;
        }
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                linea = linea.trim();
                if (linea.startsWith("favorita=") && favoritas.size() < MAXIMO_DE_FAVORITAS) favoritas.add(linea.substring("favorita=".length()));
                if (linea.startsWith("reciente=") && recientes.size() < MAXIMO_DE_RECIENTES) recientes.add(linea.substring("reciente=".length()));
            }
        } catch (IOException e) {
            // Si no se puede leer, las de fábrica: F1 sirve igual.
            favoritas.addAll(Arrays.asList(FAVORITAS_DE_FABRICA));
        }
    }

    private void guardar() {
        List<String> lineas = new ArrayList<>();
        lineas.add("# F1: las favoritas, en su orden (Ctrl+1…9), y las usadas hace poco (la primera, la última).");
        for (String id : favoritas) lineas.add("favorita=" + id);
        for (String id : recientes) lineas.add("reciente=" + id);
        try {
            castillafantasx.datos.Candado.vigilar(archivo);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se ha podido guardar " + archivo + ": " + e.getMessage());
        }
    }

    /**
     * El valor corto de una fila de F1, para el mosaico: lo que va tras «: »
     * hasta el primer «  (» (la ayuda), si es corto. "Invencible: sí" → "sí".
     */
    public static String valorCorto(String fila) {
        int dos = fila.indexOf(": ");
        if (dos < 0) {
            // "Dar antorchas (llevas 3)" → "3"
            int llevas = fila.indexOf("(llevas ");
            return llevas >= 0 ? fila.substring(llevas + "(llevas ".length(), fila.indexOf(')', llevas)) : "";
        }
        if (fila.startsWith("Ir a: ")) return "";       // el nombre ya es el lugar
        String valor = fila.substring(dos + 2);
        int ayuda = valor.indexOf("  (");
        if (ayuda >= 0) valor = valor.substring(0, ayuda);
        if (fila.startsWith("Bajar a: ") && valor.contains(" · ")) valor = valor.substring(valor.indexOf(" · ") + 3);     // solo la zona
        return valor.length() <= LARGO_DEL_VALOR ? valor : "";
    }

    private static final int LARGO_DEL_VALOR = 16;      // más largo, no cabe en el mosaico: se lee abajo, entero
}
