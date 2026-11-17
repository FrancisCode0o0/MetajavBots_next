package castillafantasx.f1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MENÚ CÓMODO — Lo cómodo de F1 (docs/F1.md) para cualquier menú largo: el
 * buscador con autocompletado, las favoritas y las usadas hace poco. Lo usan
 * el editor y el panel de la arena; cada menú guarda lo suyo en su archivo.
 * Cómo se le pone a otro menú: docs/EDITOR.md, «Lo cómodo de F1 en otro menú».
 */
public final class MenuComodo {

    /** Una cosa que se puede elegir en el menú: una fila, una herramienta… */
    public static final class Entrada {
        /** No cambia aunque cambie el texto (lo usan las favoritas y los recientes). */
        public final String id;
        public final String nombre;
        /** El grupo (tema o categoría) al que pertenece, o "". */
        public final String grupo;
        /** El nombre de su icono ("f1/" + icono en el Almacen). */
        public final String icono;
        /** Su número en el menú (la fila que acciona). */
        public final int fila;
        /** Más palabras por las que se encuentra, sin tildes. */
        public final String palabras;

        public Entrada(String id, String nombre, String grupo, String icono, int fila, String palabras) {
            this.id = id;
            this.nombre = nombre;
            this.grupo = grupo;
            this.icono = icono;
            this.fila = fila;
            this.palabras = palabras;
        }

        @Override
        public String toString() {
            return nombre + " (" + id + ")";
        }
    }

    /** La carpeta donde cada menú guarda su archivo (las pruebas la cambian por una temporal). */
    public static Path carpeta = Path.of(".");

    // TODO-FRAN: cuántas favoritas y recientes guarda cada menú y cuántos resultados enseña su buscador.
    public static final int MAXIMO_DE_FAVORITAS = 9, MAXIMO_DE_RECIENTES = 6, MAXIMO_DE_RESULTADOS = 12;

    private final String nombreDelArchivo;
    private final String[] deFabrica;
    /** Lo escrito en el buscador ("" si no se busca). */
    public String busqueda = "";
    /** Los id de las favoritas, en su orden. */
    public final List<String> favoritas = new ArrayList<>();
    /** Los id de las usadas hace poco, la última primero. */
    public final List<String> recientes = new ArrayList<>();
    /** Más cosas que el menú quiere recordar en su archivo ("clave=valor"): el sitio de un panel, por ejemplo. */
    public final java.util.Map<String, String> notas = new java.util.LinkedHashMap<>();

    /** Un menú que guarda lo suyo en 'nombreDelArchivo' (por ejemplo "arena.txt"), con esas favoritas de fábrica. */
    public MenuComodo(String nombreDelArchivo, String... favoritasDeFabrica) {
        this.nombreDelArchivo = nombreDelArchivo;
        this.deFabrica = favoritasDeFabrica;
        cargar();
    }

    /** Al abrir el menú: el buscador, vacío. */
    public void abrir() {
        busqueda = "";
    }

    public boolean buscando() {
        return !busqueda.isEmpty();
    }

    /** Una letra más en el buscador. */
    public void escribir(char letra) {
        if (busqueda.length() >= PanelF1.LARGO_MAXIMO_DE_BUSQUEDA) return;
        if (letra == ' ' && (busqueda.isEmpty() || busqueda.endsWith(" "))) return;     // sin espacios al principio ni dobles
        busqueda += letra;
    }

    /** Borra la última letra del buscador. */
    public void borrar() {
        if (buscando()) busqueda = busqueda.substring(0, busqueda.length() - 1);
    }

    /**
     * Las entradas que casan con lo escrito, las mejores primero (como en F1:
     * PanelF1.nota). 'textos' son los textos enteros de las filas, por su
     * número, o null si las entradas no tienen más texto que su nombre.
     */
    public List<Entrada> resultados(List<Entrada> todas, List<String> textos) {
        String q = PanelF1.normalizar(busqueda).trim();
        List<Entrada> casan = new ArrayList<>();
        List<Integer> notas = new ArrayList<>();
        if (q.isEmpty()) return casan;
        for (Entrada e : todas) {
            String texto = textos != null && e.fila >= 0 && e.fila < textos.size() ? textos.get(e.fila) : "";
            String nombre = PanelF1.normalizar(e.nombre);
            int nota = PanelF1.nota(nombre, nombre + " " + PanelF1.normalizar(texto) + " " + PanelF1.normalizar(e.grupo) + " " + e.palabras, q);
            if (nota < 0) continue;
            int reciente = recientes.indexOf(e.id);
            int clave = nota * 100 + (reciente >= 0 ? reciente : 50);
            int donde = 0;
            while (donde < notas.size() && notas.get(donde) <= clave) donde++;
            casan.add(donde, e);
            notas.add(donde, clave);
        }
        return casan.size() > MAXIMO_DE_RESULTADOS ? new ArrayList<>(casan.subList(0, MAXIMO_DE_RESULTADOS)) : casan;
    }

    /** Lo que falta por escribir del nombre de esa entrada, si empieza por lo escrito (el autocompletado), o "". */
    public String autocompletado(Entrada e) {
        if (e == null || !buscando()) return "";
        boolean empieza = PanelF1.normalizar(e.nombre).startsWith(PanelF1.normalizar(busqueda));
        return empieza && e.nombre.length() > busqueda.length() ? e.nombre.substring(busqueda.length()) : "";
    }

    /** Completa lo escrito con el autocompletado (Tab). Devuelve false si no había nada que completar. */
    public boolean completar(Entrada e) {
        String resto = autocompletado(e);
        if (resto.isEmpty()) return false;
        String entero = busqueda + resto;
        busqueda = entero.substring(0, Math.min(PanelF1.LARGO_MAXIMO_DE_BUSQUEDA, entero.length()));
        return true;
    }

    public boolean esFavorita(Entrada e) {
        return e != null && favoritas.contains(e.id);
    }

    /** La pone o la quita de las favoritas (Ctrl+D o la estrella). Devuelve true si ahora es favorita. */
    public boolean cambiarFavorita(Entrada e) {
        if (e == null) return false;
        boolean ahora = !favoritas.remove(e.id);
        if (ahora) {
            if (favoritas.size() >= MAXIMO_DE_FAVORITAS) favoritas.remove(favoritas.size() - 1);
            favoritas.add(e.id);
        }
        guardar();
        return ahora;
    }

    /** Se ha usado esa entrada: pasa la primera de los recientes. */
    public void usada(Entrada e) {
        if (e == null) return;
        recientes.remove(e.id);
        recientes.add(0, e.id);
        while (recientes.size() > MAXIMO_DE_RECIENTES) recientes.remove(recientes.size() - 1);
        guardar();
    }

    /** Las entradas de una lista de id (favoritas o recientes) que existen ahora, en su orden. */
    public static List<Entrada> deLista(List<String> ids, List<Entrada> todas) {
        List<Entrada> lista = new ArrayList<>();
        for (String id : ids) {
            Entrada e = deId(todas, id);
            if (e != null) lista.add(e);
        }
        return lista;
    }

    /** La entrada con ese id, o null. */
    public static Entrada deId(List<Entrada> todas, String id) {
        for (Entrada e : todas) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    /** La entrada de esa fila, o null. */
    public static Entrada deFila(List<Entrada> todas, int fila) {
        for (Entrada e : todas) {
            if (e.fila == fila) return e;
        }
        return null;
    }

    /** Las favoritas y los recientes como de fábrica (lo demás que recuerda, como el sitio del panel, se queda). */
    public void aFabrica() {
        favoritas.clear();
        favoritas.addAll(Arrays.asList(deFabrica));
        recientes.clear();
        guardar();
    }

    /** ¿Están las favoritas y los recientes como de fábrica? */
    public boolean deFabrica() {
        return recientes.isEmpty() && favoritas.equals(Arrays.asList(deFabrica));
    }

    // ------------------------------------------------------------------
    // Su archivo
    // ------------------------------------------------------------------

    private Path archivo() {
        return carpeta.resolve(nombreDelArchivo);
    }

    /** Vuelve a leer su archivo (tras deshacer «lo de fábrica»). */
    public void releer() {
        cargar();
    }

    /** Su archivo (para copiarlo antes de volver a lo de fábrica). */
    public Path archivoDelMenu() {
        return archivo();
    }

    private void cargar() {
        favoritas.clear();
        recientes.clear();
        notas.clear();
        if (!Files.exists(archivo())) {
            favoritas.addAll(Arrays.asList(deFabrica));
            return;
        }
        try {
            for (String linea : Files.readAllLines(archivo(), StandardCharsets.UTF_8)) {
                linea = linea.trim();
                int igual = linea.indexOf('=');
                if (linea.startsWith("#") || igual < 0) continue;
                String clave = linea.substring(0, igual), valor = linea.substring(igual + 1);
                if (clave.equals("favorita")) {
                    if (favoritas.size() < MAXIMO_DE_FAVORITAS) favoritas.add(valor);
                } else if (clave.equals("reciente")) {
                    if (recientes.size() < MAXIMO_DE_RECIENTES) recientes.add(valor);
                } else {
                    notas.put(clave, valor);
                }
            }
        } catch (IOException e) {
            // Si no se puede leer, las de fábrica: el menú sirve igual.
            favoritas.addAll(Arrays.asList(deFabrica));
        }
    }

    /** Guarda las favoritas, los recientes y las notas. */
    public void guardar() {
        List<String> lineas = new ArrayList<>();
        lineas.add("# Las favoritas de este menú, en su orden (Ctrl+1…9), las usadas hace poco y lo que recuerda.");
        for (String id : favoritas) lineas.add("favorita=" + id);
        for (String id : recientes) lineas.add("reciente=" + id);
        for (java.util.Map.Entry<String, String> nota : notas.entrySet()) lineas.add(nota.getKey() + "=" + nota.getValue());
        try {
            castillafantasx.datos.Candado.vigilar(archivo());
            Files.write(archivo(), lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se ha podido guardar " + archivo() + ": " + e.getMessage());
        }
    }
}
