package metajavbots.cerebro;

/**
 * RANURA — Uno de los tres cerebros que puede integrar el jugador (Ajustes ›
 * Cerebros de IA): vacío, un modelo local (un .gguf de la carpeta, con su
 * propio KoboldCpp) o uno en línea con su proveedor, su modelo y su
 * dirección. Qué tarea hace cada uno lo dice el reparto (Tarea).
 */
public final class Ranura {

    /** Lo que hay en la ranura. */
    public enum Tipo { NINGUNO, LOCAL, EN_LINEA }

    public Tipo tipo = Tipo.NINGUNO;
    /** El .gguf elegido (vacío: el que toca a esta ranura, ver CerebroLocal.modeloPorDefecto). */
    public String modeloLocal = "";
    /** Vacía: el KoboldCpp que arranca el juego; si no, otro que ya escuche (el de Jarvis, en el 5001). */
    public String direccionLocal = "";
    public String proveedor = "anthropic";
    public String modeloEnLinea = "";
    public String direccionEnLinea = "";

    public boolean vacia() {
        return tipo == Tipo.NINGUNO;
    }

    /** Copia lo de otra ranura (para pasar los ajustes de una a otra). */
    public void copiar(Ranura o) {
        tipo = o.tipo;
        modeloLocal = o.modeloLocal;
        direccionLocal = o.direccionLocal;
        proveedor = o.proveedor;
        modeloEnLinea = o.modeloEnLinea;
        direccionEnLinea = o.direccionEnLinea;
    }
}
