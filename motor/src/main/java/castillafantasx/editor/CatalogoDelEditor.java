package castillafantasx.editor;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.Contenido;

/**
 * CATÁLOGO DEL EDITOR — Todas las herramientas del editor, categoría a
 * categoría y en su orden fijo: lo que enseña la rueda y lo que encuentra el
 * buscador. Una herramienta nueva es una línea aquí (o una llamada a
 * registrar): sale sola en su categoría, sin tocar las demás (docs/EDITOR.md).
 */
public final class CatalogoDelEditor {

    private CatalogoDelEditor() { }

    private static final List<HerramientaDeEditor> TODAS = new ArrayList<>();

    /** Las filas de F1 que ningún grupo lleva (las nuevas de otros encargos): nunca se pierde ninguna. */
    public static final GrupoDeF1 OTRAS_DE_F1 = new GrupoDeF1("otras_de_f1", "Otras de F1", Categoria.TALLER, "tema_otras",
            "Las filas nuevas de F1 que aún no tienen su herramienta en el editor.", "resto nuevas", false);

    static {
        // Las del juego (castillafantasx.Contenido), categoría a categoría; al final, las filas de F1 que ningún grupo lleva.
        Contenido.delJuego().registrarHerramientasDelEditor();
        registrar(OTRAS_DE_F1);
    }

    /** Apunta un grupo de herramientas de F1 (las que lleva, por su id de F1). */
    public static void f1(String id, String nombre, Categoria categoria, String icono, String ayuda, String palabras, boolean directa, String... ids) {
        registrar(new GrupoDeF1(id, nombre, categoria, icono, ayuda, palabras, directa, ids));
    }

    /** Añade una herramienta al final de su categoría. Si ya había una con ese id, la sustituye. */
    public static void registrar(HerramientaDeEditor herramienta) {
        for (int i = 0; i < TODAS.size(); i++) {
            if (TODAS.get(i).id.equals(herramienta.id)) {
                TODAS.set(i, herramienta);
                return;
            }
        }
        TODAS.add(herramienta);
    }

    /** Todas, categoría a categoría (el orden de Categoria) y, dentro, en el orden en que se registraron. */
    public static List<HerramientaDeEditor> todas() {
        List<HerramientaDeEditor> lista = new ArrayList<>();
        for (Categoria c : Categoria.values()) lista.addAll(de(c));
        return lista;
    }

    /** Las de una categoría, en su orden. */
    public static List<HerramientaDeEditor> de(Categoria categoria) {
        List<HerramientaDeEditor> lista = new ArrayList<>();
        for (HerramientaDeEditor h : TODAS) {
            if (h.categoria == categoria) lista.add(h);
        }
        return lista;
    }

    /** La herramienta con ese id, o null. */
    public static HerramientaDeEditor deId(String id) {
        for (HerramientaDeEditor h : TODAS) {
            if (h.id.equals(id)) return h;
        }
        return null;
    }

    /** El grupo que lleva esa herramienta de F1, o null si ninguno (va a «Otras de F1»). */
    public static GrupoDeF1 grupoDe(String idDeF1) {
        for (HerramientaDeEditor h : TODAS) {
            if (h instanceof GrupoDeF1 && h != OTRAS_DE_F1 && ((GrupoDeF1) h).lleva(idDeF1)) return (GrupoDeF1) h;
        }
        return null;
    }

    /** Los iconos que usa el editor (los de las categorías, las herramientas y sus botones): el Taller de iconos los fabrica. */
    public static List<String> iconos() {
        List<String> nombres = new ArrayList<>();
        for (Categoria c : Categoria.values()) nombres.add(c.icono);
        for (HerramientaDeEditor h : TODAS) nombres.add(h.icono);
        for (String boton : new String[] { "rueda", "deshacer", "rehacer", "plegar", "agrandar", "cerrar", "favorita", "recientes", "lupa", "tema_editor", "editor", "fabrica", "ayuda" }) {
            nombres.add(boton);
        }
        return nombres;
    }
}
