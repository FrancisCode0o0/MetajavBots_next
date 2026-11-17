package castillafantasx.superficie;

/** Hacia dónde mira alguien en la superficie. El nombre en minúsculas es el del dibujo ("heroe/abajo_0"). */
public enum Direccion {
    ABAJO(0, 1, "abajo"),
    ARRIBA(0, -1, "arriba"),
    DERECHA(1, 0, "derecha"),
    IZQUIERDA(-1, 0, "izquierda");

    public final int dx, dy;
    public final String nombre;

    Direccion(int dx, int dy, String nombre) {
        this.dx = dx;
        this.dy = dy;
        this.nombre = nombre;
    }
}
