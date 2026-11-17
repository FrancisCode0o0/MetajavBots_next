package castillafantasx;

import castillafantasx.rostros.Expresion;

/**
 * VENTANA DE PISTAS — Una ventanita pequeña, arriba a la derecha, con el
 * retrato de alguien, la cara que pone y una frase corta: una pista. Se va
 * sola a los pocos segundos y no para el juego. Cualquier parte del juego (un
 * evento, una misión, un sitio) puede abrirla; la primera que lo hace es la
 * queja del héroe cuando no acierta el punto débil (combate.Queja).
 * Vale igual en la superficie y en la mazmorra.
 */
public final class VentanaDePistas {

    /** Lo que dura abierta. TODO-FRAN: cuánto. */
    public static final double SEGUNDOS = 4.5;

    /** De quién es el retrato: su aspecto (el id de su rostro). */
    public String quien;
    /** La cara que pone. */
    public Expresion cara = Expresion.CALMA;
    /** Lo que dice (sin la marca de la expresión). */
    public String frase;
    private double quedan;          // segundos que le quedan abierta

    /** Abre la ventana con ese retrato, esa cara y esa frase (si ya había otra, la cambia). */
    public void abrir(String quien, Expresion cara, String frase) {
        this.quien = quien;
        this.cara = cara;
        this.frase = frase;
        quedan = SEGUNDOS;
    }

    /** Pasa el tiempo: al acabarse el suyo, se cierra sola. */
    public void avanzar(double segundos) {
        quedan = Math.max(0, quedan - segundos);
    }

    public boolean abierta() {
        return quedan > 0 && frase != null;
    }

    /** Lo que le queda abierta, de 1 (recién abierta) a 0: para que se desvanezca al final. */
    public double parteQueQueda() {
        return quedan / SEGUNDOS;
    }

    public void cerrar() {
        quedan = 0;
    }
}
