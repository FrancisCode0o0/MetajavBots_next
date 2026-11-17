package castillafantasx.f1;

/**
 * TEMA — Los grupos de herramientas de F1, en su orden fijo (docs/F1.md).
 * Cada uno lleva su icono y la columna del mosaico donde se pinta. La rueda
 * del EDITOR puede poner un sector por tema, en este mismo orden.
 */
public enum Tema {
    TRUCOS     ("Trucos", "tema_trucos", 0),
    GALERIAS   ("Galerías", "tema_galerias", 0),
    ARENA      ("La arena", "tema_arena", 0),
    DEPURACION ("Depuración", "tema_depuracion", 0),
    HORAS      ("Las horas", "tema_horas", 0),
    HEROE      (castillafantasx.datos.ElJuego.heroe(), "tema_heroe", 1),
    MALES      ("Males", "tema_males", 1),
    LLEVA      ("Lo que lleva", "tema_lleva", 1),
    EVENTOS    ("Eventos", "tema_eventos", 2),
    DIALOGOS   ("Diálogos", "tema_dialogos", 2),
    FAMA       ("Fama", "tema_fama", 2),
    EDITOR     ("El editor", "tema_editor", 2),
    SALTOS     ("Saltos de sitio", "tema_saltos", 2),
    OTRAS      ("Otras", "tema_otras", 2);      // las filas nuevas de F1 que aún no tienen tema

    /** Cuántas columnas tiene el mosaico de F1. */
    public static final int COLUMNAS = 3;

    public final String nombre;
    /** El nombre de su icono, sin la carpeta ("f1/" + icono en el Almacen). */
    public final String icono;
    /** La columna del mosaico (0, 1 o 2). */
    public final int columna;

    Tema(String nombre, String icono, int columna) {
        this.nombre = nombre;
        this.icono = icono;
        this.columna = columna;
    }
}
