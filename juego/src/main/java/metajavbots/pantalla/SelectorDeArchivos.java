package metajavbots.pantalla;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * SELECTOR DE ARCHIVOS — Abre el diálogo nativo del sistema para elegir un archivo
 * sin bloquear el bucle gráfico. El lanzador de escritorio (lwjgl3) instala la
 * implementación real; en pruebas o sin ventana puede quedar en null o sustituirse.
 */
@FunctionalInterface
public interface SelectorDeArchivos {

    /**
     * Abre el explorador de archivos del sistema de forma asíncrona, filtrando por las
     * extensiones indicadas (sin punto, por ejemplo {@code "pdf"}, {@code "docx"}), y
     * avisa a {@code alElegir} con la ruta seleccionada o {@code null} si se cancela.
     */
    void abrir(String titulo, List<String> extensiones, Path inicial, Consumer<Path> alElegir);
}
