package castillafantasx.editor;

import java.util.List;

/**
 * SISTEMA — Lo que el editor le pide al aparato en que corre y no es igual en
 * todos: lanzar otro programa y recompilar el juego. En el PC lo pone el
 * lanzador de escritorio (lwjgl3.SistemaDePC); la tablet pondrá el suyo.
 */
public interface Sistema {

    /** Lanza un programa con sus argumentos, sin esperarlo. Devuelve false si no se ha podido. */
    boolean lanzar(List<String> orden);

    /** ¿Se puede recompilar y volver a arrancar desde aquí? */
    boolean puedeRecompilar();

    /** Arranca de nuevo el juego, recompilado. Quien lo llama cierra después este. Devuelve false si no se ha podido. */
    boolean recompilarYVolver();

    /** Se va a escribir (se ha tocado un buscador): en la tablet, que saque su teclado; 'si' false lo guarda. En el PC no hace nada. */
    default void pedirTeclado(boolean si) { }

    /** Por qué no se pudo lo último que se pidió, para decírselo a Fran; null si no hay nada que decir. */
    default String porQueNo() {
        return null;
    }

    /** Sin sistema (las pruebas, o un aparato que aún no tiene el suyo): no lanza nada, solo apunta lo pedido. */
    final class Ninguno implements Sistema {
        /** Lo último que se pidió lanzar. */
        public List<String> ultimaOrden;
        public boolean pidioRecompilar;

        @Override
        public boolean lanzar(List<String> orden) {
            ultimaOrden = orden;
            return false;
        }

        @Override
        public boolean puedeRecompilar() {
            return false;
        }

        @Override
        public boolean recompilarYVolver() {
            pidioRecompilar = true;
            return false;
        }
    }
}
