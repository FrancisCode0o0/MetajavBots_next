package metajavbots.escritorio;

import java.util.Map;
import java.util.function.Supplier;

import metajavbots.pantalla.Pantalla;

/**
 * CAPTURAS DE PESO — Las pantallas donde se ve el peso frente a la carga
 * (docs/EQUILIBRIO.md): el Taller en la pestaña de piezas y la Tienda. Se
 * apuntan aparte para no hacer crecer Capturas (que llega al límite de líneas).
 */
final class CapturasDePeso {

    private CapturasDePeso() {
    }

    static void registrar(Map<String, Supplier<Pantalla>> pantallas) {
        pantallas.put("05d_taller_piezas", metajavbots.pantallas.Taller::enPiezas);
        pantallas.put("05e_tienda", metajavbots.pantallas.Tienda::new);
    }
}
