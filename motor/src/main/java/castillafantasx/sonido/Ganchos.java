package castillafantasx.sonido;

import java.util.ArrayList;
import java.util.List;

/**
 * GANCHOS DE SONIDO — La lógica avisa aquí de cada sonido que debería oírse
 * (una respiración, un latido, un quejido) y con cuánta fuerza. Hoy nadie los
 * reproduce: quedan en una cola para el encargo de sonido, que la irá vaciando
 * con recoger(). Vale igual en la superficie y en la mazmorra.
 *
 * Para añadir un sonido nuevo: un valor más en Sonido y una llamada a
 * emitir(Sonido.ESE, fuerza) donde ocurra.
 */
public class Ganchos {

    /** Los sonidos que la lógica sabe avisar. */
    // TODO-FRAN: qué grabación suena con cada uno (encargo de sonido).
    public enum Sonido {
        RESPIRACION,    // una respiración tranquila (cada vez que toma aire)
        JADEO,          // una respiración de agotado
        LATIDO,         // un latido del corazón, herido
        QUEJIDO,        // se queja al recibir un golpe que le quita vida
        // --- El agua (docs/encargos_viejos/ENCARGO_INTERFAZ.md; los emiten el nado del juego y cada mundo) ---
        BRAZADA,        // una brazada, nadando
        CHAPOTEO,       // entra al agua andando, o da un paso vadeando
        ZAMBULLIDA,     // entra de cabeza en agua honda
        SALIR_CHORREANDO, // sale del agua, mojado
        TRAGAR_AGUA,    // nada sin aguante: traga agua
        TIRITAR,        // mojado y con frío, le castañetean los dientes
        // --- La fama (encargo de la reputación; lo emite superficie.Miradas) ---
        LADRIDO         // con la fama por los suelos, un perro guardián le ladra al pasar
    }

    /** Un aviso: qué sonido y con cuánta fuerza, de 0 (apenas) a 1 (del todo). */
    public static final class Aviso {
        public final Sonido sonido;
        public final float fuerza;

        Aviso(Sonido sonido, float fuerza) {
            this.sonido = sonido;
            this.fuerza = fuerza;
        }
    }

    /** Si nadie recoge los avisos, solo se guardan los últimos (así la cola no crece sin fin). */
    public static final int AVISOS_QUE_CABEN = 64;

    private final List<Aviso> cola = new ArrayList<>();
    /** Cuántas veces se ha avisado de cada sonido desde el principio (para las pruebas y la depuración). */
    private final int[] veces = new int[Sonido.values().length];

    /** La lógica avisa de un sonido. 'fuerza' va de 0 a 1. */
    public void emitir(Sonido sonido, double fuerza) {
        if (cola.size() >= AVISOS_QUE_CABEN) cola.remove(0);
        cola.add(new Aviso(sonido, (float) Math.max(0, Math.min(1, fuerza))));
        veces[sonido.ordinal()]++;
    }

    /** Devuelve los avisos pendientes, del más antiguo al más reciente, y vacía la cola (lo hará el encargo de sonido en cada paso). */
    public List<Aviso> recoger() {
        List<Aviso> pendientes = new ArrayList<>(cola);
        cola.clear();
        return pendientes;
    }

    /** Cuántos avisos hay sin recoger. */
    public int pendientes() {
        return cola.size();
    }

    /** Cuántas veces se ha avisado de ese sonido desde el principio. */
    public int veces(Sonido sonido) {
        return veces[sonido.ordinal()];
    }
}
