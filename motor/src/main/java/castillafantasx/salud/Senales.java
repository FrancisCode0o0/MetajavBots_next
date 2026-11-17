package castillafantasx.salud;

import castillafantasx.figura.Instante;
import castillafantasx.sonido.Ganchos.Sonido;
import castillafantasx.sonido.Ganchos;

/**
 * SEÑALES — El cuerpo avisa (docs/encargos_viejos/ENCARGO_INTERFAZ.md). A partir de la vida,
 * el aguante y la firmeza que le quedan al héroe, calcula en cada paso
 * cuánto se le nota cada cosa, de 0 (nada) a 1 (del todo), y por dónde van su
 * respiración y su latido. Es el único sitio que lo decide: su figura en la
 * superficie y la vista de la mazmorra solo leen estas cifras, así que valen
 * igual en los dos modos, con cualquier opción de «Interfaz».
 *
 * Las señales no saltan: suben deprisa y se calman despacio (por eso, tras
 * una cura, la herida se le va pasando poco a poco).
 * Los umbrales están en res/datos/senales.txt.
 */
public class Senales {

    /** Herido: sangra, cojea y se encorva; por sus ojos, los bordes de la vista se oscurecen y enrojecen. */
    public double herida;
    /** Casi muerto: todo pierde el color y se nubla. */
    public double agonia;
    /** Sin aguante: jadea, corre torpe y, parado, se apoya en las rodillas. */
    public double fatiga;
    /** A punto de caer: se tambalea y se le van los pies; por sus ojos, la vista se ladea. */
    public double tambaleo;
    /** Por dónde va la respiración y por dónde el latido, en vueltas (cada unidad es una respiración o un latido entero). */
    public double respiracion, latido;

    /**
     * Cuánto se nota una señal con esa parte (0 a 1) de vida, aguante o
     * firmeza: 0 por encima de 'desde', 1 por debajo de 'delTodo' y, entre
     * los dos, lo que toque.
     */
    public static double cuanto(double parte, double desde, double delTodo) {
        if (parte >= desde) return 0;
        if (parte <= delTodo) return 1;
        return (desde - parte) / (desde - delTodo);
    }

    /**
     * Pasan 'dt' segundos. 'vida', 'aguante' y 'firmeza' son la parte (0 a 1)
     * que le queda de cada una. Avisa en 'sonidos' de cada respiración y de
     * cada latido (si es null, no avisa de nada).
     */
    public void avanzar(double dt, double vida, double aguante, double firmeza, Ganchos sonidos) {
        herida = acercar(herida, cuanto(vida, CifrasDeSenales.HERIDA_DESDE, CifrasDeSenales.HERIDA_DEL_TODO), dt);
        agonia = acercar(agonia, cuanto(vida, CifrasDeSenales.AGONIA_DESDE, CifrasDeSenales.AGONIA_DEL_TODO), dt);
        fatiga = acercar(fatiga, cuanto(aguante, CifrasDeSenales.FATIGA_DESDE, CifrasDeSenales.FATIGA_DEL_TODO), dt);
        tambaleo = acercar(tambaleo, cuanto(firmeza, CifrasDeSenales.TAMBALEO_DESDE, CifrasDeSenales.TAMBALEO_DEL_TODO), dt);

        // La respiración y el latido van más deprisa cuanto peor está.
        double antesDeRespirar = respiracion, antesDeLatir = latido;
        respiracion += dt * respiracionesPorSegundo();
        latido += dt * latidosPorSegundo();
        if (sonidos == null) return;
        // Al empezar cada vuelta, el aviso para el encargo de sonido.
        if (Math.floor(respiracion) > Math.floor(antesDeRespirar)) {
            sonidos.emitir(fatiga >= CifrasDeSenales.JADEO_DESDE ? Sonido.JADEO : Sonido.RESPIRACION, fatiga);
        }
        if (Math.floor(latido) > Math.floor(antesDeLatir) && herida >= CifrasDeSenales.LATIDO_DESDE) {
            sonidos.emitir(Sonido.LATIDO, Math.max(herida, agonia));
        }
    }

    /** Un golpe le ha quitado vida: se queja, más fuerte cuanto peor está. */
    public void alGolpe(Ganchos sonidos) {
        sonidos.emitir(Sonido.QUEJIDO, 0.4 + 0.6 * herida);
    }

    /** Deja las señales ya en su sitio, sin esperar a que suban o se calmen (al cargar una partida o preparar una foto). */
    public void asentar(double vida, double aguante, double firmeza) {
        herida = cuanto(vida, CifrasDeSenales.HERIDA_DESDE, CifrasDeSenales.HERIDA_DEL_TODO);
        agonia = cuanto(vida, CifrasDeSenales.AGONIA_DESDE, CifrasDeSenales.AGONIA_DEL_TODO);
        fatiga = cuanto(aguante, CifrasDeSenales.FATIGA_DESDE, CifrasDeSenales.FATIGA_DEL_TODO);
        tambaleo = cuanto(firmeza, CifrasDeSenales.TAMBALEO_DESDE, CifrasDeSenales.TAMBALEO_DEL_TODO);
    }

    /** Acerca una señal a lo que debería valer: deprisa si sube, despacio si se calma. */
    private static double acercar(double senal, double objetivo, double dt) {
        double tarda = objetivo > senal ? CifrasDeSenales.SEGUNDOS_DE_SUBIR : CifrasDeSenales.SEGUNDOS_DE_CALMARSE;
        double paso = dt / tarda;
        if (Math.abs(objetivo - senal) <= paso) return objetivo;
        return senal + (objetivo > senal ? paso : -paso);
    }

    /** Respiraciones por segundo ahora: más cuanto más fatigado. */
    public double respiracionesPorSegundo() {
        return CifrasDeSenales.RESPIRA + (CifrasDeSenales.JADEA - CifrasDeSenales.RESPIRA) * fatiga;
    }

    /** Latidos por segundo ahora: más cuanto más herido (y, agotado, la mitad de eso). */
    public double latidosPorSegundo() {
        double apuro = Math.max(Math.max(herida, agonia), fatiga * 0.5);
        return CifrasDeSenales.LATE + (CifrasDeSenales.LATE_AL_LIMITE - CifrasDeSenales.LATE) * apuro;
    }

    /** El golpe del latido: 1 en el instante en que late y baja enseguida a 0 hasta el siguiente. */
    public double pulso() {
        return pulsoDe(latido);
    }

    /** Lo mismo, para una fase de latido cualquiera (en vueltas). */
    public static double pulsoDe(double latido) {
        double u = latido - Math.floor(latido);
        return Math.max(0, 1 - u * 3);
    }

    /** ¿Se le nota algo ahora? */
    public boolean algo() {
        return herida > 0 || agonia > 0 || fatiga > 0 || tambaleo > 0;
    }

    /** Apunta las señales en el momento con que se le dibuja (en tercera o en primera persona). */
    public void aMomento(Instante m) {
        m.herida = herida;
        m.agonia = agonia;
        m.fatiga = fatiga;
        m.tambaleo = tambaleo;
        m.respiracion = respiracion;
        m.latido = latido;
    }
}
