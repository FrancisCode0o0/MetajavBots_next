import metajavbots.api.*;

/** Faro: juega en equipo. Avisa de dónde está el rival y apunta adonde le avisan. */
public class Faro extends Metajavbot {

    @Override
    public void cadaTurno() {
        apuntar(14);
        if (!huirDeLaPared() && restanteAvance() == 0) {
            girar(60);
            avanzar(100);
        }
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        avisarAlEquipo(Math.round(e.x()) + "," + Math.round(e.y()));
        apuntarHacia(e.angulo());
        atacar();
    }

    @Override
    public void alOirAlEquipo(String de, String mensaje) {
        String[] xy = mensaje.split(",");
        if (xy.length == 2 && enemigosVistos().isEmpty()) {
            apuntarA(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
            atacar();
        }
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
