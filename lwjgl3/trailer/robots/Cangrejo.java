import metajavbots.api.*;

/** Cangrejo: nunca va de frente. Rodea al rival de lado y dispara sin parar. */
public class Cangrejo extends Metajavbot {

    private int sentido = 1;

    @Override
    public void cadaTurno() {
        apuntar(15);
        if (!huirDeLaPared() && restanteAvance() == 0) avanzar(140);
        if (turnoActual() % 90 == 0) sentido = -sentido;
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        // De lado: 90 grados respecto al rival.
        if (!huirDeLaPared()) girarHacia(e.angulo() + 90 * sentido);
        apuntarHacia(e.angulo());
        atacar();
    }

    @Override
    public void alChocarConPared() {
        sentido = -sentido;
        girar(120);
    }
    /** Cerca de una pared: vuelve hacia el centro (y devuelve true). */
    private boolean huirDeLaPared() {
        double m = 90;
        if (x() > m && y() > m && x() < anchoArena() - m && y() < altoArena() - m) return false;
        girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2));
        avanzar(160);
        return true;
    }
}
