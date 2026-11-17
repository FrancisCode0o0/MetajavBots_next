package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;

/** Temario — provisional (se rellena en el siguiente paso). */
public final class Temario implements Pantalla {

    @Override
    public void fotograma(Juego j, float dt) {
        Comun.fondo(j, 0);
        Comun.cabecera(j, "Temario", true);
    }
}
