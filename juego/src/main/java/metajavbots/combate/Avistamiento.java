package metajavbots.combate;

import metajavbots.api.Avistado;

/** AVISTAMIENTO — Lo que se ve de un enemigo en un turno (una foto: no cambia aunque él se mueva). */
final class Avistamiento implements Avistado {

    private final double x, y, distancia, angulo, rumbo, velocidad, blindaje;
    private final String nombre, modelo;
    private final boolean vuela;

    Avistamiento(Cuerpo yo, Cuerpo el) {
        x = el.x;
        y = el.y;
        distancia = Math.hypot(el.x - yo.x, el.y - yo.y);
        angulo = Math.toDegrees(Math.atan2(el.y - yo.y, el.x - yo.x));
        rumbo = Pelea.normal(el.rumbo);
        velocidad = el.velocidad;
        blindaje = el.blindaje();
        nombre = el.combatiente.nombre;
        modelo = el.combatiente.montaje.modelo.id;
        vuela = el.vuela();
    }

    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double distancia() { return distancia; }
    @Override public double angulo() { return angulo; }
    @Override public double rumbo() { return rumbo; }
    @Override public double velocidad() { return velocidad; }
    @Override public double blindaje() { return blindaje; }
    @Override public String nombre() { return nombre; }
    @Override public String modelo() { return modelo; }
    @Override public boolean vuela() { return vuela; }
    // Gegner
    @Override public double entfernung() { return distancia; }
    @Override public double winkel() { return angulo; }
    @Override public double richtung() { return rumbo; }
    @Override public double geschwindigkeit() { return velocidad; }
    @Override public double panzerung() { return blindaje; }
    @Override public String name() { return nombre; }
    @Override public String modell() { return modelo; }
    @Override public boolean fliegt() { return vuela; }
    // Enemy
    @Override public double distance() { return distancia; }
    @Override public double angle() { return angulo; }
    @Override public double heading() { return rumbo; }
    @Override public double speed() { return velocidad; }
    @Override public double armor() { return blindaje; }
    @Override public String model() { return modelo; }
    @Override public boolean flies() { return vuela; }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s a %.0f (ángulo %.0f°)", nombre, distancia, angulo);
    }
}
