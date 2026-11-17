package castillafantasx;

import java.util.List;

import castillafantasx.entrada.Entrada;
import castillafantasx.f1.Herramienta;
import castillafantasx.tiempo.Reloj;

/**
 * LA PARTIDA — Lo que el motor (el editor, F1) necesita de la partida en marcha, sea del juego que sea. El juego la
 * cumple con su propia partida  y el motor solo la ve por aquí.
 */
public interface LaPartida {

    /** El teclado y el ratón de esta imagen. */
    Entrada entrada();

    /** Dónde está el ratón, en píxeles de la imagen del juego (-1 si no se sabe). */
    float ratonX();

    float ratonY();

    /** El reloj del mundo (la hora del día). */
    Reloj reloj();

    /** Las filas de F1, tal como se leen («Invencible: sí»). */
    List<String> opcionesDeDepuracion();

    /** Las herramientas de F1 para las filas de ahora (castillafantasx.f1.CatalogoF1). */
    List<Herramienta> herramientasDeF1();

    /** Acciona una fila de F1 con esa tecla (Intro, ← o →), como en F1. */
    void usarFilaDeF1(int fila, int tecla);

    /** Cierra el editor y vuelve al juego. */
    void salirDelEditor();

    /** Cierra el editor y vuelve a F1, de donde se abrió. */
    void volverAF1DesdeElEditor();

    /** Dónde y cómo estaba la partida, para el registro de un cierre (castillafantasx.Cierre). */
    String estadoParaElRegistro();
}
