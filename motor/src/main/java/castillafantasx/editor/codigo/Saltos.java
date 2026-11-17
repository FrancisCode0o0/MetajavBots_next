package castillafantasx.editor.codigo;

import java.util.ArrayList;
import java.util.List;

/**
 * SALTOS — Por dónde se ha ido pasando en el visor de código (archivo y
 * línea), para volver atrás y adelante como en un navegador: Alt+← y Alt+→.
 */
public final class Saltos {

    /** Un sitio por el que se pasó: su archivo (como se abrió) y su línea (desde 0). */
    public static final class Sitio {
        public final String ruta;
        public final int linea;

        public Sitio(String ruta, int linea) {
            this.ruta = ruta;
            this.linea = linea;
        }
    }

    /** Cuántos saltos se recuerdan hacia atrás. */
    public static final int CUANTOS = 50;

    private final List<Sitio> atras = new ArrayList<>(), adelante = new ArrayList<>();

    /** Se salta desde 'aqui' a otro sitio: 'aqui' queda para volver; lo de adelante se olvida. */
    public void saltar(Sitio aqui) {
        if (aqui == null) return;
        atras.add(aqui);
        if (atras.size() > CUANTOS) atras.remove(0);
        adelante.clear();
    }

    /** Vuelve atrás desde 'aqui': el sitio al que ir, o null si no hay. */
    public Sitio atras(Sitio aqui) {
        if (atras.isEmpty()) return null;
        if (aqui != null) adelante.add(aqui);
        return atras.remove(atras.size() - 1);
    }

    /** Vuelve adelante desde 'aqui': el sitio al que ir, o null si no hay. */
    public Sitio adelante(Sitio aqui) {
        if (adelante.isEmpty()) return null;
        if (aqui != null) atras.add(aqui);
        return adelante.remove(adelante.size() - 1);
    }

    public boolean hayAtras() {
        return !atras.isEmpty();
    }

    public boolean hayAdelante() {
        return !adelante.isEmpty();
    }
}
