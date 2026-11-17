package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;

/** Ajustes — provisional (se rellena en el siguiente paso). */
public final class Ajustes implements Pantalla {

    @Override
    public void fotograma(Juego j, float dt) {
        Comun.fondo(j, 0);
        Comun.cabecera(j, "Ajustes", true);
    }
}
