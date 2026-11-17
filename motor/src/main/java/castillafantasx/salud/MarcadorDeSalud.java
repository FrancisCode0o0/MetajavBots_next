package castillafantasx.salud;

import castillafantasx.Pantalla;

/**
 * MARCADOR DE SALUD — El único sitio que decide si ahora se ven en pantalla
 * la vida y el aguante del héroe, según la opción «Interfaz» de la pausa
 * (Pantalla.interfaz). Vale igual en la superficie y en la mazmorra.
 *
 * Con «mínima», la lógica avisa (alGolpe, alCurarse, alAgotarse) y el marcador
 * asoma unos segundos y se desvanece.
 */
public class MarcadorDeSalud {

    // TODO-FRAN: cuánto rato asoman la vida y el aguante con la interfaz «mínima».
    /** Segundos que se ven enteros tras un aviso y segundos que tardan después en desvanecerse. PROVISIONAL. */
    public static final double SEGUNDOS_A_LA_VISTA = 3.0, SEGUNDOS_DE_DESVANECERSE = 1.5;

    /** Lo que le queda a la vista con «mínima» (cuenta atrás). */
    private double segundos;

    /** El héroe ha recibido un golpe que le quita vida. */
    public void alGolpe() {
        asomar();
    }

    /** El héroe se ha curado (ha tomado una cura). */
    public void alCurarse() {
        asomar();
    }

    /** El héroe se ha quedado sin aguante. */
    public void alAgotarse() {
        asomar();
    }

    private void asomar() {
        segundos = SEGUNDOS_A_LA_VISTA + SEGUNDOS_DE_DESVANECERSE;
    }

    /** Pasa el tiempo (en segundos): lo llaman la superficie y la mazmorra en cada paso. */
    public void avanzar(double dt) {
        segundos = Math.max(0, segundos - dt);
    }

    /** Deja de asomar ya (al limpiar la arena de pruebas). */
    public void ocultar() {
        segundos = 0;
    }

    /**
     * Cuánto se ven ahora la vida y el aguante: 0 = nada, 1 = del todo.
     * «Ninguna» siempre da 0; «completa», siempre 1; «mínima», 1 tras un aviso y luego baja hasta 0.
     */
    public float alfa() {
        if (Pantalla.interfaz == Pantalla.Interfaz.COMPLETA) return 1;
        if (Pantalla.interfaz == Pantalla.Interfaz.NINGUNA) return 0;
        return (float) Math.min(1, segundos / SEGUNDOS_DE_DESVANECERSE);
    }

    /** ¿Se ve ahora algo de la vida y el aguante? */
    public boolean seVe() {
        return alfa() > 0;
    }
}
