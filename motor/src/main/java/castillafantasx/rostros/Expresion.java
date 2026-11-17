package castillafantasx.rostros;

/**
 * EXPRESIÓN — Cómo pone la cara un rostro: cejas, ojos, boca y cabeza. El
 * pintor de retratos lee estas cifras; las mismas valen para todos los rostros.
 *
 * En un diálogo, una frase puede empezar por la expresión entre llaves:
 * "{miedo} No salgáis de noche." (ver deFrase).
 */
public enum Expresion {

    //       nombre     cejaDentro cejaFuera ojos  miraY  boca  comisuras dientes cabezaBaja ladea tension
    CALMA(  "calma",     0.0,      0.0,     1.00,  0.0,  0.00,  0.0,     false,   0.00,     0.00, 0.0),
    HABLA(  "habla",     0.1,      0.0,     1.00,  0.0,  0.45,  0.0,     false,   0.00,     0.00, 0.0),
    DOLOR(  "dolor",     0.8,     -0.4,     0.22,  0.0,  0.40, -0.7,     true,    0.10,     0.14, 1.0),
    MIEDO(  "miedo",     1.0,      0.5,     1.40,  0.0,  0.35, -0.4,     false,  -0.10,     0.00, 0.5),
    IRA(    "ira",      -1.0,      0.4,     0.80,  0.0,  0.28, -0.6,     true,    0.12,     0.00, 1.0),
    REZO(   "rezo",      0.3,      0.0,     0.00,  0.0,  0.00,  0.0,     false,   0.30,     0.00, 0.0),
    PENA(   "pena",      0.8,     -0.2,     0.70, -0.6,  0.00, -0.8,     false,   0.14,     0.06, 0.4),
    SONRISA("sonrisa",   0.2,      0.1,     0.80,  0.0,  0.12,  0.9,     false,   0.00,     0.04, 0.3),
    // Cansado y harto: ojos entornados, la mirada baja y la boca algo caída (la queja del punto débil, docs/encargos_viejos/ENCARGO_RATON.md).
    CANSANCIO("cansancio", 0.5,   -0.3,     0.45,  0.3,  0.08, -0.4,     false,   0.16,     0.08, 0.3);

    public final String nombre;
    /** Las cejas, por dentro y por fuera: de -1 (bajas) a 1 (levantadas). */
    public final double cejaDentro, cejaFuera;
    /** Cuánto se abren los ojos: 0 cerrados, 1 normal, más de 1 desorbitados. */
    public final double ojos;
    /** Hacia dónde mira: -1 abajo, 1 arriba. */
    public final double miraY;
    /** Cuánto se abre la boca (0 a 1) y hacia dónde van las comisuras (-1 abajo, 1 arriba). */
    public final double boca, comisuras;
    /** Enseña los dientes apretados. */
    public final boolean dientes;
    /** La cabeza: cuánto se agacha y cuánto se ladea (radianes). */
    public final double cabezaBaja, ladea;
    /** Cuánto se marcan el ceño y los surcos de la cara, de 0 a 1. */
    public final double tension;

    Expresion(String nombre, double cejaDentro, double cejaFuera, double ojos, double miraY, double boca, double comisuras,
              boolean dientes, double cabezaBaja, double ladea, double tension) {
        this.nombre = nombre;
        this.cejaDentro = cejaDentro;
        this.cejaFuera = cejaFuera;
        this.ojos = ojos;
        this.miraY = miraY;
        this.boca = boca;
        this.comisuras = comisuras;
        this.dientes = dientes;
        this.cabezaBaja = cabezaBaja;
        this.ladea = ladea;
        this.tension = tension;
    }

    /** La expresión con ese nombre ("miedo", "ira"…), o null si no existe. */
    public static Expresion de(String nombre) {
        for (Expresion e : values()) {
            if (e.nombre.equalsIgnoreCase(nombre.trim())) return e;
        }
        return null;
    }

    /**
     * La expresión que pide una frase de diálogo: la que lleve entre llaves al
     * principio ("{miedo} …", o "{heroe miedo} …" si la dice otro rostro: ver
     * rostroDeFrase); si no lleva, calma.
     */
    public static Expresion deFrase(String frase) {
        String marca = marcaDe(frase);
        if (marca == null) return CALMA;
        Expresion e = de(marca.substring(marca.lastIndexOf(' ') + 1));
        return e == null ? CALMA : e;
    }

    /** Quién dice la frase, si la marca lo pone delante de la expresión ("{heroe ira} …" → "heroe"), o null. */
    public static String rostroDeFrase(String frase) {
        String marca = marcaDe(frase);
        if (marca == null || marca.indexOf(' ') < 0 || de(marca.substring(marca.lastIndexOf(' ') + 1)) == null) return null;
        return marca.substring(0, marca.indexOf(' '));
    }

    /** La frase sin su marca de expresión, que es lo que se lee en pantalla. */
    public static String sinMarca(String frase) {
        String marca = marcaDe(frase);
        if (marca == null || de(marca.substring(marca.lastIndexOf(' ') + 1)) == null) return frase;
        return frase.substring(frase.indexOf('}') + 1).trim();
    }

    // Lo que va entre las llaves del principio, o null si no empieza por llave.
    private static String marcaDe(String frase) {
        int fin = frase.indexOf('}');
        if (!frase.startsWith("{") || fin < 0) return null;
        return frase.substring(1, fin).trim();
    }
}
