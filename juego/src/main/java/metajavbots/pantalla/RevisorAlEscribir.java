package metajavbots.pantalla;

import java.util.List;
import java.util.function.Function;

import com.badlogic.gdx.Gdx;

import metajavbots.codigo.Documento;

/**
 * REVISOR AL ESCRIBIR — Los errores mientras se escribe, como en los editores
 * de verdad: tras un segundo sin cambios en el documento, se revisa el código
 * en otro hilo (lo compila la pantalla, con VentanaDeCodigo.revisarAlEscribir)
 * y, cuando acaba y nadie ha vuelto a escribir, sus marcas sustituyen a las
 * que había.
 */
final class RevisorAlEscribir {

    private long versionVista = -1, versionRevisada = -1;
    private float quieto;
    private volatile List<VentanaDeCodigo.Marca> revisadas;
    private volatile boolean revisando;

    /** Cada fotograma del editor. */
    void fotograma(Documento doc, List<VentanaDeCodigo.Marca> marcas, Function<String, List<VentanaDeCodigo.Marca>> revisar) {
        List<VentanaDeCodigo.Marca> listas = revisadas;
        if (listas != null && !revisando) {
            revisadas = null;
            if (versionRevisada == doc.version()) {
                marcas.clear();
                marcas.addAll(listas);
            }
        }
        if (doc.version() != versionVista) {
            versionVista = doc.version();
            quieto = 0;
            return;
        }
        quieto += Gdx.graphics == null ? 0.016f : Gdx.graphics.getDeltaTime();
        if (quieto < 1 || revisando || versionRevisada == doc.version()) return;
        revisando = true;
        versionRevisada = doc.version();
        String texto = doc.texto();
        Thread h = new Thread(() -> {
            try {
                revisadas = revisar.apply(texto);
            } catch (RuntimeException | LinkageError e) {
                revisadas = null;
            } finally {
                revisando = false;
            }
        }, "revisar-al-escribir");
        h.setDaemon(true);
        h.start();
    }
}
