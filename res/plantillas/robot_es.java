import metajavbots.api.*;

/**
 * Mi primer metajavbot. Cambia lo que quieras y pulsa Compilar (Ctrl+S).
 * Los métodos «al…» se llaman solos cuando pasa algo; cadaTurno, unas 30 veces por segundo.
 */
public class MiRobot extends Metajavbot {

    @Override
    public void alEmpezar() {
        decir("¡A pelear!");
    }

    @Override
    public void cadaTurno() {
        // Si ya llegó adonde iba, avanza otro poco y gira.
        if (restanteAvance() == 0) {
            avanzar(120);
            girar(35);
        }
        // Mueve la mira (los brazos y el radar) para buscar al enemigo.
        apuntar(12);
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        apuntarHacia(e.angulo());
        atacar();
    }

    @Override
    public void alRecibirGolpe(Golpe g) {
        // Esquiva hacia un lado.
        girarHacia(g.angulo() + 90);
        avanzar(80);
    }
}
