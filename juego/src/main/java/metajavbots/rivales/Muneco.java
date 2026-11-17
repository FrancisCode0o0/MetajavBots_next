package metajavbots.rivales;

import metajavbots.api.Enemigo;
import metajavbots.api.Metajavbot;

/**
 * MUÑECO — El robot de entrenamiento de los tutoriales: da vueltas despacio
 * por la arena y casi nunca ataca. Ideal para probar el código sin agobios.
 */
public class Muneco extends Metajavbot {

    @Override
    public void alEmpezar() {
        decir("Bip. Soy el muñeco.");
    }

    @Override
    public void cadaTurno() {
        if (restanteAvance() == 0) {
            avanzar(160);
            girar(25);
        }
        if (x() < 80 || y() < 80 || x() > anchoArena() - 80 || y() > altoArena() - 80) {
            girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2));
        }
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        // De vez en cuando, un golpecito para que haya que esquivar algo.
        if (turnoActual() % 150 == 0) {
            apuntarHacia(e.angulo());
            atacar();
        }
    }
}
