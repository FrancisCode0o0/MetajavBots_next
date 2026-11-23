import metajavbots.api.*;

/** Profeta: no apunta adonde está el rival, sino adonde va a estar. */
public class Profeta extends Metajavbot {

    @Override
    public void alEmpezar() {
        decir("Ya sé dónde vas a estar.");
    }

    @Override
    public void cadaTurno() {
        if (enemigosVistos().isEmpty()) {
            apuntar(20);
            if (listoEspecial()) pulsoRadar();
        }
        if (!huirDeLaPared() && restanteAvance() == 0) {
            girar(azar().nextInt(90) - 45);
            avanzar(90);
        }
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        // Lo que tarda el disparo en llegar, y cuánto se habrá movido en ese tiempo.
        double turnos = e.distancia() / 20;
        double rumbo = Math.toRadians(e.rumbo());
        double x = e.x() + Math.cos(rumbo) * e.velocidad() * turnos;
        double y = e.y() + Math.sin(rumbo) * e.velocidad() * turnos;
        apuntarA(x, y);
        if (e.distancia() < 220 && !huirDeLaPared()) retroceder(60);
        atacar();
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
