package castillafantasx.seres;

/**
 * FICHA — Lo que el motor sabe de un tipo de ser del juego (cada juego tiene las suyas):
 * su nombre en los datos y dónde vive. Con el nombre se buscan sus cifras en res/datos/.
 */
public interface Ficha {

    /** Su nombre en los datos y en los mapas, en minúsculas. */
    String id();

    /** ¿Vive en la superficie? */
    boolean enSuperficie();

    /** ¿Vive en las mazmorras? */
    boolean enMazmorra();

    /** La ficha del juego que va con esa letra en los mapas de las mazmorras, o null (castillafantasx.Contenido.fichaPorLetra). */
    static Ficha porLetra(char letra) {
        return castillafantasx.Contenido.delJuego().fichaPorLetra(letra);
    }

    /** La ficha del juego con ese id, o null si no hay ninguna (castillafantasx.Contenido.ficha). */
    static Ficha porId(String id) {
        return castillafantasx.Contenido.delJuego().ficha(id);
    }
}
