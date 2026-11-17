package castillafantasx.eventos;

import castillafantasx.entrada.Accion;

/**
 * TUTORIAL — Un aviso que le enseña al jugador, en su momento, qué tecla hace
 * qué, y espera a que lo pruebe (paso «tutorial» de una secuencia,
 * docs/EVENTOS.md). La frase sale de Ink; «<tecla>» se cambia por la tecla
 * que el jugador tenga puesta. Vale igual en la superficie y en la mazmorra.
 */
public final class Tutorial {

    /** Cómo se sabe que el jugador lo ha probado. */
    public enum Prueba { PULSAR, MANTENER, ANDAR, NADAR, COMPRAR }

    /** Lo que se puede enseñar: su nombre en los archivos, la tecla de la superficie y la de la mazmorra, y cómo se prueba. */
    public enum Leccion {
        ANDAR("andar", Accion.SUP_ARRIBA, Accion.MAZ_AVANZAR, Prueba.ANDAR),
        CORRER("correr", Accion.SUP_CORRER, Accion.MAZ_CORRER, Prueba.MANTENER),
        ATACAR("atacar", Accion.SUP_ATACAR, Accion.MAZ_ATACAR, Prueba.PULSAR),
        CUBRIRSE("cubrirse", Accion.SUP_POSTURA, Accion.MAZ_POSTURA, Prueba.MANTENER),
        SALTAR("saltar", Accion.SUP_SALTAR, Accion.MAZ_SALTAR, Prueba.PULSAR),
        ESQUIVAR("esquivar", Accion.SUP_ESQUIVAR, Accion.MAZ_ESQUIVAR, Prueba.PULSAR),
        LUMBRE("lumbre", Accion.SUP_LUMBRE, Accion.MAZ_LUMBRE, Prueba.PULSAR),
        CURARSE("curarse", Accion.SUP_CURAR, Accion.MAZ_CURAR, Prueba.PULSAR),
        ENVAINAR("envainar", Accion.SUP_ENVAINAR, Accion.MAZ_ENVAINAR, Prueba.PULSAR),
        ACCION("accion", Accion.SUP_ACCION, Accion.MAZ_ACCION, Prueba.PULSAR),
        INVENTARIO("inventario", Accion.SUP_INVENTARIO, Accion.MAZ_INVENTARIO, Prueba.PULSAR),
        DIARIO("diario", Accion.SUP_DIARIO, Accion.MAZ_DIARIO, Prueba.PULSAR),
        NADAR("nadar", Accion.SUP_ARRIBA, Accion.MAZ_AVANZAR, Prueba.NADAR),
        COMPRAR("comprar", Accion.SUP_ACCION, Accion.MAZ_ACCION, Prueba.COMPRAR);

        public final String id;
        public final Accion enSuperficie, enMazmorra;
        public final Prueba prueba;

        Leccion(String id, Accion enSuperficie, Accion enMazmorra, Prueba prueba) {
            this.id = id;
            this.enSuperficie = enSuperficie;
            this.enMazmorra = enMazmorra;
            this.prueba = prueba;
        }

        public static Leccion porId(String id) {
            for (Leccion l : values()) {
                if (l.id.equals(id)) return l;
            }
            return null;
        }
    }

    // --- Cifras PROVISIONALES ---
    // TODO-FRAN: cuánto hay que mantener, andar o esperar en cada aviso de tutorial.
    /** Los frames que hay que mantener la tecla en las de mantener (cubrirse, correr). */
    public static final int FRAMES_DE_MANTENER = 30;
    /** Lo que hay que andar para dar por probado «andar» (píxeles de la superficie; en la mazmorra, casillas × 32). */
    public static final float PASOS_DE_ANDAR = 48f;
    /** Lo que sigue a la vista el aviso ya cumplido, con su marca, antes de pasar al siguiente (segundos). */
    public static final double SEGUNDOS_DE_HECHO = 0.9;
    /** Con la interfaz «ninguna», cada cuánto se repite el aviso si el jugador no lo prueba (segundos). */
    public static final double SEGUNDOS_ENTRE_RECUERDOS = 9;

    public final Leccion leccion;
    /** La frase, como sale de Ink (con «<tecla>» sin cambiar todavía). */
    public final String frase;
    /** Quién lo dice (su rostro) y con qué cara, por si se enseña en la ventana de pistas. */
    public final String rostro;
    public final castillafantasx.rostros.Expresion cara;
    /** Lo que lleva hecho de la prueba, de 0 a 1. */
    public float avance;
    /** Ya lo ha probado: el aviso se despide. */
    public boolean hecho;
    /** Segundos desde que se enseñó (o desde que se cumplió, si 'hecho'). */
    public double segundos;
    // Para las pruebas de mantener y de andar.
    public int framesMantenida;
    public float andadoDesdeX, andadoDesdeY;
    public boolean conSitio;
    public double proximoRecuerdo;

    public Tutorial(Leccion leccion, String frase, String rostro, castillafantasx.rostros.Expresion cara) {
        this.leccion = leccion;
        this.frase = frase;
        this.rostro = rostro;
        this.cara = cara;
    }

    /** La frase con la tecla puesta: «Pulsa [Espacio] para atacar». */
    public String texto(castillafantasx.entrada.Teclas teclas, boolean enMazmorra) {
        return frase.replace("<tecla>", "[" + teclas.primera(enMazmorra ? leccion.enMazmorra : leccion.enSuperficie) + "]");
    }
}
