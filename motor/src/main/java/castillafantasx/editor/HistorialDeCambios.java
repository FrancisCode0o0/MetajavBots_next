package castillafantasx.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * HISTORIAL DE CAMBIOS — La herramienta del Taller que enseña lo que se
 * puede deshacer y rehacer, con el nombre de cada cambio (Historial).
 */
public final class HistorialDeCambios extends HerramientaDeEditor {

    private static final int CAMBIOS_A_LA_VISTA = 6;

    public HistorialDeCambios() {
        super("historial", "Deshacer y rehacer", Categoria.TALLER, "deshacer",
                "Ver los últimos cambios hechos en el editor y deshacerlos o rehacerlos.", "ctrl z y cambios atras");
    }

    @Override
    public List<Opcion> opciones(Editor editor) {
        List<Opcion> opciones = new ArrayList<>();
        List<String> nombres = editor.historial.nombres();
        opciones.add(new Opcion("deshacer", "Deshacer (Ctrl+Z)" + (nombres.isEmpty() ? ": no hay nada" : ": " + nombres.get(0)), "deshacer", (e, paso) -> e.deshacer()));
        opciones.add(new Opcion("rehacer", "Rehacer (Ctrl+Y)" + (editor.historial.hayQueRehacer() ? "" : ": no hay nada"), "rehacer", (e, paso) -> e.rehacer()));
        for (int i = 1; i < nombres.size() && i <= CAMBIOS_A_LA_VISTA; i++) {
            opciones.add(new Opcion("cambio_" + i, "   antes: " + nombres.get(i), null, null));
        }
        return opciones;
    }
}
