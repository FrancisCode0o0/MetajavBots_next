package castillafantasx.figura;



/**
 * RETOQUE — Lo que se le cambia a cada pieza de una figura justo antes de
 * pintarla: dónde queda, lo gruesa que es y de qué material. Lo usa el editor
 * de figuras (docs/EDITOR.md): el Escultor pregunta aquí por cada punto.
 * Una 'pieza' es parte * 1000 + su orden dentro de la parte.
 */
public interface Retoque {

    /** Empieza una figura: se olvidan los ejes de la anterior. */
    void empezar();

    /** Dónde queda ese punto de la figura. */
    P3 punto(P3 p, int parte, int pieza);

    /** Por cuánto se multiplica el grueso de la pieza en ese punto (1 = igual). */
    double grosor(P3 p, int parte, int pieza);

    /** De qué material se pinta (null = no se pinta). */
    Material material(Material m, int parte, int pieza);
}
