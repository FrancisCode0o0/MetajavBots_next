import metajavbots.api.*;

/** Fortín: se va a una esquina, se atrinchera y dispara desde allí. */
public class Fortin extends Metajavbot {

    @Override
    public void alEmpezar() {
        double ex = x() < anchoArena() / 2 ? 80 : anchoArena() - 80;
        double ey = y() < altoArena() / 2 ? 80 : altoArena() - 80;
        memoria().put("esquinaX", ex);
        memoria().put("esquinaY", ey);
    }

    @Override
    public void cadaTurno() {
        double ex = (double) memoria().get("esquinaX");
        double ey = (double) memoria().get("esquinaY");
        if (distanciaA(ex, ey) > 40) {
            girarHacia(anguloHacia(ex, ey));
            avanzar(distanciaA(ex, ey));
        }
        apuntar(10);
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        apuntarHacia(e.angulo());
        atacar();
    }

    @Override
    public void alRecibirGolpe(Golpe g) {
        if (listoEspecial()) fortificar();
        apuntarHacia(g.angulo());
    }
}
