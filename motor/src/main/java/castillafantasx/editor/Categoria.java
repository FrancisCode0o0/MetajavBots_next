package castillafantasx.editor;

/**
 * CATEGORÍA — Los grandes grupos del editor, en su orden fijo: los sectores
 * de la rueda. Cada una tiene su icono, su color propio y una línea de ayuda.
 * Una herramienta nueva dice su categoría y sale sola en la rueda.
 */
public enum Categoria {
    MUNDO      ("Mundo", "cat_mundo", "Ir a cualquier sitio, ver el mapa y construir y decorar el mundo: poner cosas, pintar, dibujar y pegar imágenes."),
    FIGURAS    ("Figuras", "cat_figuras", "Galerías de figuras, armas, maniquíes y retratos. Aquí irán las poses y los seres nuevos."),
    PERSONAJES ("Personajes", "cat_personajes", "El estado de " + castillafantasx.datos.ElJuego.heroe() + ", sus males y lo que lleva. Aquí irán el carácter y las variantes."),
    LUZ        ("Luz y tiempo", "cat_luz", "La hora del día. Aquí irán las luces y el paso del tiempo."),
    HISTORIA   ("Historia", "cat_historia", "Eventos, misiones, diálogos y fama. Aquí irán el cine y el guion."),
    COMBATE    ("Combate", "cat_combate", "La arena, los trucos y lo que se ve de un golpe. Aquí irán combos, choques y armas a distancia."),
    CODIGO     ("Código", "cat_codigo", "Del mundo al código: pincha una cosa y se abre donde se decide cómo se porta."),
    SONIDO     ("Sonido", "cat_sonido", "Abrir la música y los sonidos en su programa."),
    TALLER     ("Taller", "cat_taller", "El centro de mando, deshacer y rehacer, y lo demás del propio editor.");

    public final String nombre;
    /** El nombre de su icono ("f1/" + icono en el Almacen). */
    public final String icono;
    /** Una línea que dice para qué sirve (se ve al pasar por encima). */
    public final String ayuda;

    Categoria(String nombre, String icono, String ayuda) {
        this.nombre = nombre;
        this.icono = icono;
        this.ayuda = ayuda;
    }

    /** Su color propio (Estilo.COLORES_DE_CATEGORIA). */
    public int color() {
        return Estilo.COLORES_DE_CATEGORIA[ordinal()];
    }

    /** La categoría de cada tema de F1: así F1 y el editor se ordenan igual. */
    public static Categoria de(castillafantasx.f1.Tema tema) {
        switch (tema) {
            case SALTOS:     return MUNDO;
            case GALERIAS:   return FIGURAS;
            case HEROE: case MALES: case LLEVA: return PERSONAJES;
            case HORAS:      return LUZ;
            case EVENTOS: case DIALOGOS: case FAMA: return HISTORIA;
            case TRUCOS: case ARENA: case DEPURACION: return COMBATE;
            default:         return TALLER;
        }
    }
}
