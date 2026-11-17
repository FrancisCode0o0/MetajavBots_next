package castillafantasx.eventos;

/**
 * ESCENA — Cómo se ve ahora mismo una secuencia de cine (docs/EVENTOS.md):
 * si el jugador tiene el control, el fundido, las franjas negras, la viñeta,
 * el temblor, la cámara lenta, adónde mira la cámara y el aviso de tutorial.
 * El Ejecutor lo va cambiando paso a paso; el dibujo (DibujoInterfaz,
 * DibujoSuperficie) solo lo lee. Vale igual en la superficie y en la mazmorra.
 */
public final class Escena {

    // --- Cifras PROVISIONALES ---
    // TODO-FRAN: lo que tardan en entrar las franjas y la viñeta, y lo que cubren.
    /** Lo que tardan en entrar o salir las franjas negras y la viñeta (segundos). */
    public static final double SEGUNDOS_DE_FRANJAS = 0.5;
    /** La parte del alto de la pantalla que tapa cada franja negra, ya entrada. */
    public static final float ALTO_DE_FRANJA = 0.12f;
    /** Lo oscuro que llega a ser el borde de la viñeta (0 a 1). */
    public static final float FUERZA_DE_VINETA = 0.7f;
    /** Lo que dura un fundido si el paso no lo dice (segundos). */
    public static final double SEGUNDOS_DE_FUNDIDO = 0.6;
    /** Lo que se ve el aviso de «pulsa otra vez para saltar» (segundos). */
    public static final double SEGUNDOS_PARA_SALTAR = 2.5;

    /** El jugador no maneja al héroe (paso «sin_control»). */
    public boolean sinControl;
    /** El fundido: su color (RRGGBB) y lo que tapa, de 0 (nada) a 1 (todo). */
    public int colorDeFundido;
    public float fundido;
    /** Las franjas negras de cine, arriba y abajo: si están puestas y lo que han entrado (0 a 1). */
    public boolean conFranjas;
    public float franjas;
    /** La viñeta oscura: si está puesta y lo que ha entrado (0 a 1). */
    public boolean conVineta;
    public float vineta;
    /** La cámara lenta: 1, normal; 0,5, a la mitad. */
    public double lenta = 1;
    /** El temblor: los píxeles que se mueve la pantalla y los segundos que le quedan. */
    public double temblor, segundosDeTemblor;
    /** Adónde mira la cámara de la superficie {x, y} en píxeles del mapa, o null: sigue al héroe. */
    public float[] camara;
    /** El aviso de tutorial que se está enseñando, o null. */
    public Tutorial tutorial;
    /** Segundos que le quedan al aviso «pulsa otra vez para saltar la escena» (0: no se ve). */
    public double avisoDeSalto;
    /** Se está saltando la secuencia: lo que queda de ella pasa de golpe. */
    public boolean saltando;
    private double restoDeLenta;

    /** ¿Hay algo de cine que pintar encima del juego? */
    public boolean seVe() {
        return fundido > 0 || franjas > 0 || vineta > 0 || tutorial != null || avisoDeSalto > 0;
    }

    /** Pasa 'dt' segundos: las franjas y la viñeta entran o salen poco a poco, y el temblor se acaba. */
    public void avanzar(double dt) {
        float paso = (float) (dt / SEGUNDOS_DE_FRANJAS);
        franjas = conFranjas ? Math.min(1, franjas + paso) : Math.max(0, franjas - paso);
        vineta = conVineta ? Math.min(1, vineta + paso) : Math.max(0, vineta - paso);
        if (segundosDeTemblor > 0) {
            segundosDeTemblor -= dt;
            if (segundosDeTemblor <= 0) temblor = 0;
        }
        if (avisoDeSalto > 0) avisoDeSalto = Math.max(0, avisoDeSalto - dt);
    }

    /** Con la cámara lenta, ¿toca mover el mundo en este frame? (A la mitad, uno sí y uno no.) */
    public boolean tocaMoverElMundo() {
        if (lenta >= 1) return true;
        restoDeLenta += Math.max(0.05, lenta);
        if (restoDeLenta < 1) return false;
        restoDeLenta -= 1;
        return true;
    }

    /** Todo como cuando no hay secuencia: el control al jugador, sin franjas, sin viñeta, a su ritmo y la cámara con el héroe. */
    public void soltar() {
        sinControl = false;
        conFranjas = false;
        conVineta = false;
        lenta = 1;
        temblor = 0;
        segundosDeTemblor = 0;
        camara = null;
        saltando = false;
        avisoDeSalto = 0;
    }

    /** De golpe, sin nada de cine (al empezar una partida o cargarla). */
    public void limpiar() {
        soltar();
        fundido = 0;
        franjas = 0;
        vineta = 0;
        tutorial = null;
    }
}
