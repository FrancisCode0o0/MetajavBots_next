package castillafantasx.superficie;

import castillafantasx.datos.Ajustes;

/**
 * AGRESIVIDAD — Lo dispuesto a pelear que está cada tipo de rival, graduado en
 * el panel de la arena (docs/encargos_viejos/ENCARGO_AJUSTES.md): de huir al primer gesto de
 * El héroe a atacar sin que nadie lo provoque. Se guarda en el juego de
 * cifras como una cifra más ("agresividad" = 0 … 4); sin ella, cada uno se
 * porta como siempre (o como diga la fila «Se portan»).
 */
// TODO-FRAN: si se les pasa el miedo o el enfado, y cuánto tardan.
public enum Agresividad {
    HUYE_AL_VERLE("huye al primer gesto de " + castillafantasx.datos.ElJuego.heroe()),
    HUYE_SI_LE_PEGAN("huye si le pegan"),
    SE_DEFIENDE("se defiende: replica si le pegan"),
    ATACA_SI_SE_ACERCAN("ataca si se le acercan"),
    ATACA("ataca sin que nadie le provoque");

    /** Su nombre de cifra en el juego de cifras. */
    public static final String CIFRA = "agresividad";
    // Cifras PROVISIONALES, en píxeles.
    public static final float DISTANCIA_DEL_GESTO = 96;      // tan cerca, el héroe ya asusta al que huye al verle
    public static final float DISTANCIA_DE_ACOSO = 56;       // tan cerca, ataca el que ataca si se le acercan

    public final String nombre;

    Agresividad(String nombre) {
        this.nombre = nombre;
    }

    /** La de ese tipo de ser cambiada en el panel, o null si se porta como siempre. */
    public static Agresividad de(String ser) {
        double n = Ajustes.numero(ser, CIFRA, -1);
        if (n < 0) return null;
        return values()[Math.min(values().length - 1, (int) Math.round(n))];
    }

    /** Gradúa la de ese tipo de ser (null: vuelve a portarse como siempre). */
    public static void poner(String ser, Agresividad a) {
        if (a == null) Ajustes.volverAFabrica(ser, CIFRA);
        else Ajustes.cambiar(ser, CIFRA, a.ordinal(), -1);
    }
}
