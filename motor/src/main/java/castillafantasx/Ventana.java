package castillafantasx;

import com.badlogic.gdx.Gdx;

/**
 * VENTANA — Lo que el juego le pide a la ventana del ordenador: ponerse a pantalla completa o
 * volver a ventana, y esconder el cursor. En el PC lo hace lwjgl3.VentanaDePC; en las pruebas
 * y en las fotos, la de libGDX de siempre.
 */
public interface Ventana {

    /** ¿Está ahora a pantalla completa? */
    boolean completa();

    /** Pasa a pantalla completa o vuelve a una ventana de ese tamaño. */
    void ponerCompleta(boolean completa, int anchoDeVentana, int altoDeVentana);

    /** Esconde el cursor del todo (no se pinta nada) o lo vuelve a enseñar. */
    void esconderCursor(boolean si);

    /** La de libGDX tal cual: la pantalla completa de siempre; el cursor escondido es su dibujo transparente (interfaz.Cursor). */
    class DeLibGdx implements Ventana {
        @Override
        public boolean completa() {
            return Gdx.graphics.isFullscreen();
        }

        @Override
        public void ponerCompleta(boolean completa, int anchoDeVentana, int altoDeVentana) {
            if (completa) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            else Gdx.graphics.setWindowedMode(anchoDeVentana, altoDeVentana);
        }

        @Override
        public void esconderCursor(boolean si) {
        }
    }
}
