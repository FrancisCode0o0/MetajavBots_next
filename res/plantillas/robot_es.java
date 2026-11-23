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
        // Ojo: cadaTurno va después de alVerEnemigo y la mira solo recuerda la última orden: esta borra la de allí.
        apuntar(12);
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        // La mira gira despacio (unos 14° por turno): no llega al enemigo en este mismo turno.
        apuntarHacia(e.angulo());
        // atacar() dispara ya, hacia donde mira AHORA, no hacia donde le acabas de pedir.
        atacar();
    }

    @Override
    public void alRecibirGolpe(Golpe g) {
        // Esquiva hacia un lado.
        girarHacia(g.angulo() + 90);
        avanzar(80);
    }
}
