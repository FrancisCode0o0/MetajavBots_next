package castillafantasx.combate;

/**
 * GOLPE — Lo que quita un golpe de un arma, de los puños o de una criatura:
 * vida y firmeza, cada una con su cifra (res/datos/combate.txt, sección [armas] o [ataques]).
 * La vida va en dos escalas: la de la superficie (2 puntos = 1 corazón) y la de la mazmorra.
 * La firmeza es la misma en las dos.
 */
public final class Golpe {

    public final double vidaEnSuperficie;   // 0 = no se usa en la superficie
    public final double vidaEnMazmorra;     // 0 = no se usa en la mazmorra
    public final double firmeza;

    public Golpe(double vidaEnSuperficie, double vidaEnMazmorra, double firmeza) {
        this.vidaEnSuperficie = vidaEnSuperficie;
        this.vidaEnMazmorra = vidaEnMazmorra;
        this.firmeza = firmeza;
    }
}
