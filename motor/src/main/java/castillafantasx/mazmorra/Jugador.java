package castillafantasx.mazmorra;

/** JUGADOR — El héroe dentro de una mazmorra: dónde está, hacia dónde mira y su vida (de 0 a 100). */
public class Jugador {

    // --- Cifras PROVISIONALES ---
    public static final double RADIO = 0.22;              // casillas
    public static final double VELOCIDAD = 3.35;          // casillas por segundo (un 15 % más que antes)
    public static final double FACTOR_CORRER = 1.6;
    public static final double GIRO = 2.6;                // radianes por segundo, con el teclado
    // Ratón, como en el prototipo anterior (Heretic, Hexen). Sensibilidad PROVISIONAL.
    public static final double GIRO_RATON = 0.003;        // radianes de giro por píxel de ratón
    public static final double MIRADA_RATON = 0.0015;     // parte de la pantalla que sube la mirada por píxel de ratón
    // Tope de mirar arriba o abajo (parte de la pantalla): un arco amplio, para apuntar al suelo y a lo alto (docs/encargos_viejos/ENCARGO_RATON.md).
    public static final double MIRADA_MAXIMA = 1.0;
    public static final double MIRADA_TECLADO = 0.6;      // parte de la pantalla por segundo, con las teclas de mirar
    public static final double VIDA_MAXIMA = 100;

    public double x, y;
    public double angulo;             // 0 = este; crece hacia el sur (la y del mapa va hacia abajo)
    /** Mirar arriba (positivo) o abajo: parte de la pantalla que se desplaza el horizonte. No cambia la puntería. */
    public double mirada;
    public double vida = VIDA_MAXIMA;
    public double balanceo;           // avanza al andar: mueve la vista y el arma
    /** Lo que tiene los pies por encima del suelo (un salto), en casillas: sube la vista. */
    public double altura;
    /** Lo que se agacha (rodando en una esquiva), en casillas: baja la vista. */
    public double agachado;
    public double dolor;              // segundos de pantalla roja que quedan tras un mordisco

    /** El ratón: 'dx' píxeles a la derecha giran la vista; 'dy' píxeles hacia abajo bajan la mirada. */
    public void mirarConRaton(int dx, int dy) {
        angulo += dx * GIRO_RATON;
        mirada = Math.max(-MIRADA_MAXIMA, Math.min(MIRADA_MAXIMA, mirada - dy * MIRADA_RATON));
    }

    /** Sube (segundos positivos) o baja la mirada con las teclas de mirar arriba y abajo. */
    public void mirarConTeclas(double segundos) {
        mirada = Math.max(-MIRADA_MAXIMA, Math.min(MIRADA_MAXIMA, mirada + segundos * MIRADA_TECLADO));
    }

    public double dirX() {
        return Math.cos(angulo);
    }

    public double dirY() {
        return Math.sin(angulo);
    }
}
