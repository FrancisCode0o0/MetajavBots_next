package castillafantasx.entrada;

import com.badlogic.gdx.Input.Keys;

import castillafantasx.datos.ElJuego;

/**
 * ACCION — Todo lo que el jugador puede pedir con una tecla, un botón del
 * ratón o la rueda (DECISIONES_CONTROLES §2). Cada acción vale en un modo
 * (superficie o mazmorra) y admite las teclas que se quieran; se asignan en
 * el menú de teclas y se guardan en teclas.txt.
 *
 * Lo que no está aquí no se puede asignar: F1 (depuración), Esc (pausa y
 * volver) y mover el ratón para mirar en la mazmorra.
 *
 * Cada acción que existe en los dos modos sale de fábrica con las mismas
 * teclas en la superficie y en la mazmorra (regla 8 de CLAUDE.md).
 */
public enum Accion {

    // --- Superficie ---
    SUP_ARRIBA    (Modo.SUPERFICIE, "superficie.arriba",     "Moverse arriba",          Keys.UP, Keys.W),
    SUP_ABAJO     (Modo.SUPERFICIE, "superficie.abajo",      "Moverse abajo",           Keys.DOWN, Keys.S),
    SUP_IZQUIERDA (Modo.SUPERFICIE, "superficie.izquierda",  "Moverse a la izquierda",  Keys.LEFT, Keys.A),
    SUP_DERECHA   (Modo.SUPERFICIE, "superficie.derecha",    "Moverse a la derecha",    Keys.RIGHT, Keys.D),
    SUP_ATACAR    (Modo.SUPERFICIE, "superficie.atacar",     "Atacar",                  Keys.SPACE, Teclas.RATON_IZQUIERDO),
    // Con un botón del ratón (de fábrica, el derecho), la acción va a lo señalado con el cursor; con una tecla, a lo de delante.
    SUP_ACCION    (Modo.SUPERFICIE, "superficie.accion",     "Acción (hablar, coger, guisar…)", Keys.ENTER, Teclas.RATON_DERECHO),
    SUP_POSTURA   (Modo.SUPERFICIE, "superficie.postura",    "Cubrirse (postura firme)", Keys.X),
    SUP_CORRER    (Modo.SUPERFICIE, "superficie.correr",     "Correr",                  Keys.SHIFT_LEFT),
    // Cambiar de arma, igual que en la mazmorra: Q y la rueda hacia arriba, la anterior; E y la rueda hacia abajo, la siguiente.
    SUP_ARMA_ANT  (Modo.SUPERFICIE, "superficie.arma_anterior",  "Arma anterior",       Keys.Q, Teclas.RUEDA_ARRIBA),
    SUP_ARMA_SIG  (Modo.SUPERFICIE, "superficie.arma_siguiente", "Arma siguiente",      Keys.E, Teclas.RUEDA_ABAJO),
    SUP_ARMA_1    (Modo.SUPERFICIE, "superficie.arma_1", "Arma 1", Keys.NUM_1),
    SUP_ARMA_2    (Modo.SUPERFICIE, "superficie.arma_2", "Arma 2", Keys.NUM_2),
    SUP_ARMA_3    (Modo.SUPERFICIE, "superficie.arma_3", "Arma 3", Keys.NUM_3),
    SUP_ARMA_4    (Modo.SUPERFICIE, "superficie.arma_4", "Arma 4", Keys.NUM_4),
    SUP_ARMA_5    (Modo.SUPERFICIE, "superficie.arma_5", "Arma 5", Keys.NUM_5),
    SUP_ARMA_6    (Modo.SUPERFICIE, "superficie.arma_6", "Arma 6", Keys.NUM_6),
    SUP_ARMA_7    (Modo.SUPERFICIE, "superficie.arma_7", "Arma 7", Keys.NUM_7),
    SUP_ARMA_8    (Modo.SUPERFICIE, "superficie.arma_8", "Arma 8", Keys.NUM_8),
    SUP_ARMA_9    (Modo.SUPERFICIE, "superficie.arma_9", "Arma 9", Keys.NUM_9),
    SUP_INVENTARIO(Modo.SUPERFICIE, "superficie.inventario", "Inventario",              Keys.I),
    SUP_GUARDAR   (Modo.SUPERFICIE, "superficie.guardar",    "Guardar partida",         Keys.G),
    // TODO-FRAN: la tecla definitiva para envainar y desenvainar (DECISIONES_ARMAS §10).
    SUP_ENVAINAR  (Modo.SUPERFICIE, "superficie.envainar",   "Envainar / desenvainar",  Keys.F),
    // La lumbre (docs/encargos_viejos/ENCARGO_DIA_NOCHE.md), igual que en la mazmorra. TODO-FRAN: las teclas definitivas de la lumbre.
    SUP_LUMBRE    (Modo.SUPERFICIE, "superficie.lumbre",     "Encender / apagar la lumbre", Keys.L),
    SUP_DEJAR_LUMBRE(Modo.SUPERFICIE, "superficie.dejar_lumbre", "Dejar la lumbre en el suelo", Keys.T),
    // Saltar y esquivar (docs/encargos_viejos/ENCARGO_SALTO.md), igual que en la mazmorra. TODO-FRAN: las teclas definitivas del salto y de la esquiva.
    SUP_SALTAR    (Modo.SUPERFICIE, "superficie.saltar",     "Saltar",                  Keys.C),
    SUP_ESQUIVAR  (Modo.SUPERFICIE, "superficie.esquivar",   "Esquivar",                Keys.V),
    // Curarse (docs/encargos_viejos/ENCARGO_INTERFAZ.md), igual que en la mazmorra: toma la cura elegida. TODO-FRAN: la tecla definitiva de curarse.
    SUP_CURAR     (Modo.SUPERFICIE, "superficie.curar",      "Curarse (comer, beber o untarse)", Keys.R),
    // El diario del héroe (docs/EVENTOS.md), igual que en la mazmorra. TODO-FRAN: la tecla definitiva del diario.
    SUP_DIARIO    (Modo.SUPERFICIE, "superficie.diario",     "Diario de " + ElJuego.heroe(),  Keys.J),
    // Pantalla completa o ventana: vale también en el título, en los diálogos y en los menús abiertos desde la superficie.
    SUP_PANTALLA  (Modo.SUPERFICIE, "superficie.pantalla_completa", "Pantalla completa / ventana", Keys.F11),

    // --- Mazmorra (primera persona) ---
    MAZ_AVANZAR    (Modo.MAZMORRA, "mazmorra.avanzar",        "Avanzar",              Keys.W, Keys.UP),
    MAZ_RETROCEDER (Modo.MAZMORRA, "mazmorra.retroceder",     "Retroceder",           Keys.S, Keys.DOWN),
    MAZ_LATERAL_IZQ(Modo.MAZMORRA, "mazmorra.lateral_izq",    "Paso a la izquierda",  Keys.A),
    MAZ_LATERAL_DER(Modo.MAZMORRA, "mazmorra.lateral_der",    "Paso a la derecha",    Keys.D),
    MAZ_GIRAR_IZQ  (Modo.MAZMORRA, "mazmorra.girar_izq",      "Girar a la izquierda", Keys.LEFT),
    MAZ_GIRAR_DER  (Modo.MAZMORRA, "mazmorra.girar_der",      "Girar a la derecha",   Keys.RIGHT),
    // Mirar arriba y abajo: de fábrica, solo con el ratón (estas dos salen sin tecla).
    MAZ_MIRAR_ARRIBA(Modo.MAZMORRA, "mazmorra.mirar_arriba",  "Mirar arriba"),
    MAZ_MIRAR_ABAJO(Modo.MAZMORRA, "mazmorra.mirar_abajo",    "Mirar abajo"),
    MAZ_ATACAR     (Modo.MAZMORRA, "mazmorra.atacar",         "Atacar",               Keys.SPACE, Teclas.RATON_IZQUIERDO),
    MAZ_ARMA_ANT   (Modo.MAZMORRA, "mazmorra.arma_anterior",  "Arma anterior",        Keys.Q, Teclas.RUEDA_ARRIBA),
    MAZ_ARMA_SIG   (Modo.MAZMORRA, "mazmorra.arma_siguiente", "Arma siguiente",       Keys.E, Teclas.RUEDA_ABAJO),
    MAZ_ARMA_1     (Modo.MAZMORRA, "mazmorra.arma_1", "Arma 1", Keys.NUM_1),
    MAZ_ARMA_2     (Modo.MAZMORRA, "mazmorra.arma_2", "Arma 2", Keys.NUM_2),
    MAZ_ARMA_3     (Modo.MAZMORRA, "mazmorra.arma_3", "Arma 3", Keys.NUM_3),
    MAZ_ARMA_4     (Modo.MAZMORRA, "mazmorra.arma_4", "Arma 4", Keys.NUM_4),
    MAZ_ARMA_5     (Modo.MAZMORRA, "mazmorra.arma_5", "Arma 5", Keys.NUM_5),
    MAZ_ARMA_6     (Modo.MAZMORRA, "mazmorra.arma_6", "Arma 6", Keys.NUM_6),
    MAZ_ARMA_7     (Modo.MAZMORRA, "mazmorra.arma_7", "Arma 7", Keys.NUM_7),
    MAZ_ARMA_8     (Modo.MAZMORRA, "mazmorra.arma_8", "Arma 8", Keys.NUM_8),
    MAZ_ARMA_9     (Modo.MAZMORRA, "mazmorra.arma_9", "Arma 9", Keys.NUM_9),
    MAZ_CORRER     (Modo.MAZMORRA, "mazmorra.correr",         "Correr",               Keys.SHIFT_LEFT),
    MAZ_POSTURA    (Modo.MAZMORRA, "mazmorra.postura",        "Cubrirse (postura firme)", Keys.X),
    MAZ_ENVAINAR   (Modo.MAZMORRA, "mazmorra.envainar",       "Envainar / desenvainar", Keys.F),
    MAZ_LUMBRE     (Modo.MAZMORRA, "mazmorra.lumbre",         "Encender / apagar la lumbre", Keys.L),
    MAZ_DEJAR_LUMBRE(Modo.MAZMORRA, "mazmorra.dejar_lumbre",  "Dejar la lumbre en el suelo", Keys.T),
    MAZ_SALTAR     (Modo.MAZMORRA, "mazmorra.saltar",         "Saltar",               Keys.C),
    MAZ_ESQUIVAR   (Modo.MAZMORRA, "mazmorra.esquivar",       "Esquivar",             Keys.V),
    MAZ_CURAR      (Modo.MAZMORRA, "mazmorra.curar",          "Curarse (comer, beber o untarse)", Keys.R),
    MAZ_ACCION     (Modo.MAZMORRA, "mazmorra.accion",         "Acción (abrir, coger, guisar…)", Keys.ENTER, Teclas.RATON_DERECHO),
    MAZ_INVENTARIO (Modo.MAZMORRA, "mazmorra.inventario",     "Inventario",           Keys.I),
    MAZ_DIARIO     (Modo.MAZMORRA, "mazmorra.diario",         "Diario de " + ElJuego.heroe(), Keys.J),
    MAZ_PANTALLA   (Modo.MAZMORRA, "mazmorra.pantalla_completa", "Pantalla completa / ventana", Keys.F11);

    /** Los dos modos del juego. Dentro de uno, una tecla compartida hace todas sus acciones. */
    public enum Modo { SUPERFICIE, MAZMORRA }

    /** Las acciones de elegir el arma 1, 2… 9 de cada modo, en orden. */
    public static final Accion[] ARMAS_DE_SUPERFICIE = { SUP_ARMA_1, SUP_ARMA_2, SUP_ARMA_3, SUP_ARMA_4, SUP_ARMA_5, SUP_ARMA_6, SUP_ARMA_7, SUP_ARMA_8, SUP_ARMA_9 };
    public static final Accion[] ARMAS_DE_MAZMORRA = { MAZ_ARMA_1, MAZ_ARMA_2, MAZ_ARMA_3, MAZ_ARMA_4, MAZ_ARMA_5, MAZ_ARMA_6, MAZ_ARMA_7, MAZ_ARMA_8, MAZ_ARMA_9 };

    public final Modo modo;
    public final String clave;    // cómo se escribe en teclas.txt
    public final String nombre;   // cómo se ve en el menú de teclas
    private final int[] porDefecto;

    Accion(Modo modo, String clave, String nombre, int... porDefecto) {
        this.modo = modo;
        this.clave = clave;
        this.nombre = nombre;
        this.porDefecto = porDefecto;
    }

    /** Las teclas de fábrica (una copia, para que nadie las cambie por error). */
    public int[] teclasPorDefecto() {
        return porDefecto.clone();
    }

    /** La acción que se llama así en teclas.txt, o null. */
    public static Accion porClave(String clave) {
        for (Accion a : values()) {
            if (a.clave.equals(clave)) return a;
        }
        return null;
    }
}
