package metajavbots.combate;

import java.util.List;

import metajavbots.api.Enemigo;
import metajavbots.api.Metajavbot;

/**
 * ROBOT AVERIADO DE DESTRUCCIÓN — IA específica de los robots averiados que caen
 * del cielo en «Destrucción y supervivencia»: conserva aspecto roto y conducción
 * torpe/errática (tirones y choques contra obstáculos), pero con sesgo agresivo
 * claro hacia el jugador para acercarse a su zona y dispararle con ligera dispersión.
 */
public final class RobotAveriadoDestruccion extends Metajavbot {

    private final boolean inmovil;
    private final int cadenciaDisparo;
    private final double dispersionDisparo;
    private double rumboErratico;
    private double bandazoErratico;
    private double ultimoAnguloJugador;
    private int turnosMemoria;
    private int paso;

    /** Crea un robot averiado agresivo para el reto de Destrucción y supervivencia. */
    public RobotAveriadoDestruccion(boolean inmovil, int cadenciaDisparo, double dispersionDisparo) {
        this.inmovil = inmovil;
        this.cadenciaDisparo = Math.max(5, cadenciaDisparo);
        this.dispersionDisparo = Math.max(3.0, dispersionDisparo);
    }

    @Override
    public void cadaTurno() {
        int t = paso++;
        if (t % 9 == 0) {
            bandazoErratico = (azar().nextDouble() * 2.0 - 1.0) * 22.0;
        }
        List<Enemigo> vistos = enemigosVistos();
        if (!vistos.isEmpty()) {
            perseguirYDisparar(vistos.get(0), t);
            return;
        }
        buscarJugador(t);
    }

    private void perseguirYDisparar(Enemigo blanco, int t) {
        ultimoAnguloJugador = blanco.angulo();
        turnosMemoria = 65;
        if (inmovil) {
            if (t % 6 == 0) {
                girarHacia(blanco.angulo() + bandazoErratico * 0.8);
            }
            if (t % 23 == 0) {
                avanzar(azar().nextBoolean() ? 14 : -10);
            }
        } else {
            // Movimiento torpe pero con tendencia clara a ir contra el jugador (choca con obstáculos).
            double angMovimiento = blanco.angulo() + bandazoErratico;
            girarHacia(angMovimiento);
            double pasoAvance = (t % 17 == 0) ? 16 : (blanco.distancia() > 140 ? 58 : 34);
            avanzar(pasoAvance);
        }
        double desvio = (azar().nextDouble() * 2.0 - 1.0) * dispersionDisparo;
        apuntarHacia(blanco.angulo() + desvio);
        if (t % cadenciaDisparo == 0) {
            atacar();
        }
    }

    private void buscarJugador(int t) {
        if (turnosMemoria > 0) {
            turnosMemoria--;
            if (!inmovil) {
                girarHacia(ultimoAnguloJugador + bandazoErratico);
                avanzar(t % 13 == 0 ? 18 : 48);
            }
            double barrido = Math.sin(t * 0.45) * 42.0;
            apuntarHacia(ultimoAnguloJugador + barrido);
            return;
        }
        if (inmovil) {
            if (t % 8 == 0) girar(azar().nextDouble() * 70 - 35);
            apuntar(32);
            return;
        }
        if (t % 18 == 0) {
            rumboErratico = azar().nextDouble() * 360.0 - 180.0;
        }
        if (y() < 105) rumboErratico = 70;
        else if (y() > altoArena() - 105) rumboErratico = -70;
        if (x() < 120) rumboErratico = 0;
        else if (x() > anchoArena() - 120) rumboErratico = 180;
        girarHacia(rumboErratico + bandazoErratico * 0.5);
        avanzar(t % 15 == 0 ? 15 : 46);
        apuntar(34);
    }
}
