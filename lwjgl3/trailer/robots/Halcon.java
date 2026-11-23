import metajavbots.api.*;

/** Halcón: vuela lejos de todos y lanza misiles desde la distancia. */
public class Halcon extends Metajavbot {

    @Override
    public void cadaTurno() {
        apuntar(18);
        if (restanteAvance() == 0) {
            // Hacia el centro, pero sin quedarse quieto.
            girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2) + azar().nextInt(160) - 80);
            avanzar(160);
        }
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        apuntarHacia(e.angulo());
        if (e.distancia() < 300) {
            girarHacia(e.angulo() + 180);
            avanzar(120);
        }
        if (listoIzquierda()) atacarIzquierda();
        else atacarDerecha();
    }
}
