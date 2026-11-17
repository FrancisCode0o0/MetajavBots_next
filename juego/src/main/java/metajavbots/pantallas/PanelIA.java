package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;

/** PanelIA — provisional (se rellena en el siguiente paso). */
public final class PanelIA implements Pantalla {

    @Override
    public void fotograma(Juego j, float dt) {
        Comun.fondo(j, 0);
        Comun.cabecera(j, "PanelIA", true);
    }
}
