package metajavbots.cerebro;

/**
 * TAREA — Lo que el juego pide a un cerebro, para repartirlo entre los tres
 * (Ajustes › Cerebros de IA › Reparto): lo rápido y constante, lo pesado y
 * los tutores.
 */
public enum Tarea {

    /** Diálogos de los robots, pullas, locutor, charla del taller y estilo del rival entre rondas. */
    RAPIDO,
    /** Revisión del código tras la pelea, modo temario y ejercicios nuevos. */
    PESADO,
    /** El tutor de los tutoriales, de las lecciones y de los ejercicios. */
    TUTOR;

    /**
     * La ranura (0, 1 o 2) que hace esta tarea si el jugador no dice otra cosa: la 2 lo pesado; la 1 lo demás (el tutor
     * conversa: con Gemma 3 4B contesta en ~11 s; con Qwen3.5-9B, en ~35 s en el portátil de Fran).
     */
    public int ranuraPorDefecto() {
        return this == PESADO ? 1 : 0;
    }
}
