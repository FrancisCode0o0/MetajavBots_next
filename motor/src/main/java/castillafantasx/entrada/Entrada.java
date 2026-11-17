package castillafantasx.entrada;

/**
 * ENTRADA — Qué teclas están pulsadas ahora mismo (también los botones del
 * ratón, con los códigos de Teclas). La ventana (o una prueba) avisa con
 * pulsar() y soltar(); el juego pregunta con mantenida().
 */
public class Entrada {

    private final boolean[] abajo = new boolean[Teclas.CODIGOS];   // una casilla por código de tecla

    public void pulsar(int tecla) {
        poner(tecla, true);
    }

    public void soltar(int tecla) {
        poner(tecla, false);
    }

    /** Pulsa o suelta una tecla. */
    public void poner(int tecla, boolean pulsada) {
        if (tecla >= 0 && tecla < abajo.length) abajo[tecla] = pulsada;
    }

    public boolean mantenida(int tecla) {
        return tecla >= 0 && tecla < abajo.length && abajo[tecla];
    }

    /** Suelta todo: al cambiar de modo o al perder el foco, para que nada se quede «pegado». */
    public void soltarTodo() {
        java.util.Arrays.fill(abajo, false);
    }
}