package castillafantasx;

import java.util.Map;
import java.util.ServiceLoader;

import castillafantasx.arte.Imagen;

/**
 * CONTENIDO — Lo que el juego le da al motor: sus dibujos, sus seres, sus herramientas… El motor no conoce al
 * juego: le pregunta a esta interfaz. El juego la cumple con una clase suya y la apunta en
 * META-INF/services/castillafantasx.Contenido (así lo encuentra Java, con ServiceLoader, sin que el motor la nombre).
 * Cada método trae lo que hace el motor si el juego no dice nada.
 */
public interface Contenido {

    /** Fabrica los dibujos propios del juego y los deja en el mapa del Almacen (antes que los del motor). */
    default void fabricarDibujos(Map<String, Imagen> imagenes) { }

    /** Retoca los dibujos ya hechos, tras los del motor (la veta de la madera, por ejemplo). */
    default void retocarDibujos(Map<String, Imagen> imagenes) { }

    /** La ficha del tipo de ser con ese id, o null si el juego no tiene ninguno así. */
    default castillafantasx.seres.Ficha ficha(String id) {
        return null;
    }

    /** Las herramientas fijas de F1, en su orden (castillafantasx.f1.CatalogoF1): cada una con la fila de F1 que acciona. */
    default castillafantasx.f1.Herramienta[] herramientasDeF1() {
        return new castillafantasx.f1.Herramienta[0];
    }

    /** Apunta en el editor sus herramientas, categoría a categoría (castillafantasx.editor.CatalogoDelEditor.registrar). */
    default void registrarHerramientasDelEditor() { }

    /** Las herramientas del editor que salen de fábrica entre las favoritas («grupo/herramienta» o «grupo»). */
    default String[] favoritasDelEditor() {
        return new String[0];
    }

    /** La cura que hay en una casilla «v» del mapa si el mapa no dice otra (su id). */
    default String curaDeFabrica() {
        return "";
    }

    /** ¿Hay una cura con ese id? (Los mapas la nombran en [curas] y [agua].) */
    default boolean existeCura(String id) {
        return true;
    }

    /** ¿Estorba el paso esa pieza colocada a mano? De fábrica, las de pie marcadas «sólido». */
    default boolean estorba(castillafantasx.mundo.Colocado c) {
        return c.solido && c.capa == castillafantasx.mundo.Colocado.Capa.PIE;
    }

    /** La ficha del ser que va con esa letra en los mapas de las mazmorras, o null. */
    default castillafantasx.seres.Ficha fichaPorLetra(char letra) {
        return null;
    }

    /** El arma con ese id, o null si el juego no tiene ninguna así. */
    default castillafantasx.combate.Arma arma(String id) {
        return null;
    }

    /** La figura con ese nombre (un ser del juego o una pieza), para hacer seres de piezas (castillafantasx.figura.FiguraCompuesta); null si no hay. */
    default castillafantasx.figura.Figura figuraDe(String nombre) {
        return null;
    }

    /** El contenido del juego que corre encima (o uno vacío, si no hay juego). Se busca una vez. */
    static Contenido delJuego() {
        return Buscado.CONTENIDO;
    }

    /** Lo buscado una sola vez (las interfaces no pueden tener campos que cambien). */
    final class Buscado {
        private Buscado() { }

        static final Contenido CONTENIDO = ServiceLoader.load(Contenido.class).findFirst().orElse(new Contenido() { });
    }
}
