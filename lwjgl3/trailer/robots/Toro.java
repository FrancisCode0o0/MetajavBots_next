import metajavbots.api.*;

/** Toro: va a por el rival con el martillo; solo embiste cuando lo tiene lejos. */
public class Toro extends Metajavbot {

    @Override
    public void cadaTurno() {
        if (enemigosVistos().isEmpty()) {
            girar(12);
            apuntar(-12);
        }
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        girarHacia(e.angulo());
        apuntarHacia(e.angulo());
        if (e.distancia() > 380 && listoEspecial()) {
            embestir();
        } else {
            avanzar(Math.max(0, e.distancia() - 50));
        }
        if (e.distancia() < alcanceDerecha() + 30) atacar();
    }

    @Override
    public void alChocarConEnemigo(Enemigo e) {
        // Tras el choque se aparta: lo suyo es el martillo, no empujar.
        retroceder(90);
    }

    @Override
    public void alRecibirGolpe(Golpe g) {
        girarHacia(g.angulo());
    }
}
