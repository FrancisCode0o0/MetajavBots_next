package castillafantasx.editor;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.f1.Herramienta;
import castillafantasx.f1.PanelF1;

/**
 * GRUPO DE F1 — Una herramienta del editor hecha con filas de F1: las de un
 * tema («el héroe», «Males»…) o una sola («Maniquíes»). Las filas hacen
 * lo mismo que en F1 (Partida.usarFilaDeF1); aquí solo se agrupan.
 */
public final class GrupoDeF1 extends HerramientaDeEditor {

    /** Los id de F1 que lleva (f1.CatalogoF1); uno acabado en «*» vale por todos los que empiezan así. */
    private final String[] ids;
    /** Una sola fila que abre otra pantalla: al elegirla se acciona, sin panel. */
    private final boolean directa;

    public GrupoDeF1(String id, String nombre, Categoria categoria, String icono, String ayuda, String palabras, boolean directa, String... ids) {
        super(id, nombre, categoria, icono, ayuda, palabras);
        this.ids = ids;
        this.directa = directa;
    }

    /** ¿Lleva este grupo la herramienta de F1 con ese id? */
    public boolean lleva(String idDeF1) {
        for (String mio : ids) {
            if (mio.endsWith("*") ? idDeF1.startsWith(mio.substring(0, mio.length() - 1)) : mio.equals(idDeF1)) return true;
        }
        return false;
    }

    /** Las herramientas de F1 de este grupo que existen ahora, en el orden en que se piden. */
    private List<Herramienta> mias(Editor editor) {
        List<Herramienta> mias = new ArrayList<>();
        List<Herramienta> todas = editor.partida.herramientasDeF1();
        if (this == CatalogoDelEditor.OTRAS_DE_F1) {
            // Las filas de F1 que ningún grupo lleva: así una fila nueva de otro encargo nunca se pierde.
            for (Herramienta h : todas) {
                if (h.tema != null && !h.id.equals("editor") && CatalogoDelEditor.grupoDe(h.id) == null) mias.add(h);
            }
            return mias;
        }
        for (String mio : ids) {
            for (Herramienta h : todas) {
                boolean casa = mio.endsWith("*") ? h.id.startsWith(mio.substring(0, mio.length() - 1)) : mio.equals(h.id);
                if (casa) mias.add(h);
            }
        }
        return mias;
    }

    @Override
    public List<Opcion> opciones(Editor editor) {
        List<String> filas = editor.partida.opcionesDeDepuracion();
        List<Opcion> opciones = new ArrayList<>();
        for (Herramienta h : mias(editor)) {
            String texto = filas.get(h.fila);
            String idDeF1 = h.id;
            boolean conValor = texto.contains("←") || texto.startsWith("Traer delante") || texto.startsWith("Bajar a");
            opciones.add(new Opcion(h.id, h.nombre, texto, h.icono, conValor, (e, paso) -> e.usarDeF1(idDeF1, paso)));
        }
        return opciones;
    }

    @Override
    public boolean alElegir(Editor editor) {
        if (!directa) return true;
        List<Opcion> opciones = opciones(editor);
        if (!opciones.isEmpty()) opciones.get(0).hacer(editor, 0);
        return false;
    }

    /** El valor corto de una fila de F1, para la ficha (como en el mosaico de F1). */
    public static String valorCorto(String texto) {
        return PanelF1.valorCorto(texto);
    }
}
