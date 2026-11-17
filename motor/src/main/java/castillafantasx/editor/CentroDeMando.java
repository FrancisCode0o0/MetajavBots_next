package castillafantasx.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CENTRO DE MANDO — Desde el editor, cada cosa se abre en su programa (mapas,
 * diálogos, código, pixel art, modelos, sonido) con el archivo justo. Al
 * guardar allí, el vigía avisa al juego y se recarga lo que se pueda. Los
 * programas están en res/editor/programas.txt (Programas).
 */
public final class CentroDeMando extends HerramientaDeEditor {

    /** El archivo elegido de cada programa (los que abren varios). */
    private final Map<String, Integer> elegido = new HashMap<>();

    public CentroDeMando(String id, String nombre, Categoria categoria, String icono, String ayuda, String palabras) {
        super(id, nombre, categoria, icono, ayuda, palabras);
    }

    /** Los programas de esta herramienta: en «Sonido», los de sonido; en el Taller, todos. */
    private List<Programas.Programa> programas() {
        List<Programas.Programa> lista = new ArrayList<>();
        for (Programas.Programa p : Programas.todos()) {
            if (categoria != Categoria.SONIDO || p.grupo.equals("sonido")) lista.add(p);
        }
        return lista;
    }

    @Override
    public List<Opcion> opciones(Editor editor) {
        List<Opcion> opciones = new ArrayList<>();
        for (Programas.Programa p : programas()) {
            List<String> archivos = Programas.archivosDe(p);
            int cual = archivos.isEmpty() ? 0 : Math.floorMod(elegido.getOrDefault(p.id, 0), archivos.size());
            String archivo = archivos.isEmpty() ? null : archivos.get(cual);
            boolean instalado = Programas.ejecutableDe(p) != null;
            String texto = p.para + " · " + p.nombre + ": "
                    + (!instalado ? "no está instalado (o pon su ruta en res/editor/programas.txt)"
                            : archivos.size() > 1 ? "< " + corto(archivo) + " >" : archivo == null ? "abrir" : corto(archivo));
            opciones.add(new Opcion(p.id, p.para, texto, icono, instalado && archivos.size() > 1, (e, paso) -> {
                if (paso != 0) elegido.put(p.id, cual + paso);
                else e.avisar(Programas.abrir(p.id, archivo, 0), Programas.ejecutableDe(p) == null);
            }));
        }
        return opciones;
    }

    /** Solo el nombre del archivo, sin su carpeta. */
    private static String corto(String ruta) {
        return ruta.equals(".") ? "la carpeta del juego" : ruta.substring(ruta.lastIndexOf('/') + 1);
    }

    @Override
    public String pie(Editor editor) {
        return "Intro: abrirlo en su programa · ←→: otro archivo · al guardar allí, el juego lo recarga";
    }
}
