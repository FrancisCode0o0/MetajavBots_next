package castillafantasx.figura;

import castillafantasx.arte.Colores;

/**
 * MATERIAL — De qué está hecha una pieza: sus cinco tonos, del más claro al
 * más oscuro, y cómo se comporta con la luz. El Escultor elige el tono de cada
 * píxel según la luz que le da. Todos los materiales se sacan de la misma
 * receta: por eso todo lo dibujado comparte gama (sombras frías y moradas,
 * luces cálidas). Cada clase de material se reconoce por su acabado:
 *
 *   placa   acero pulido: mucho contraste, un destello y el canto iluminado.
 *   malla   anillas: puntos claros y oscuros al tresbolillo.
 *   cuero   liso, con un brillo corto.
 *   lana    mate y con grano; nunca brilla.
 *   seda    brillos largos y sombras hondas (también el terciopelo).
 *   lino    claro y limpio, casi sin grano.
 */
public final class Material {

    /** El dibujo menudo que lleva la superficie, además del sombreado. */
    public enum Trama { LISA, MALLA, PELO, ACOLCHADO, ESCAMA }

    private static final int SOMBRA_COMUN = 0x140E1C;     // hacia aquí se oscurece todo
    private static final int LUZ_COMUN = 0xE8D8B0;        // hacia aquí se aclara todo
    private static final int LUZ_DEL_METAL = 0xDCE2E8;    // el metal brilla en frío

    public final int[] tonos = new int[5];    // 0xAARRGGBB, de claro a oscuro
    public final double grano;                // cuánto se mezclan dos tonos vecinos con puntitos (0 = nada)
    public final boolean brilla;              // luz propia: no le afecta la sombra
    /** El color del contorno de lo que es de este material: su tono más oscuro, casi negro. */
    public final int contorno;

    // El acabado (lo fijan los métodos de más abajo al crear el material).
    public Trama trama = Trama.LISA;
    /** Cuánto se adelantan las luces (positivo: más zona clara; negativo: más mate). */
    public double brillo;
    /** El tono más claro que puede tomar (0 = todos; 1 = sin luces vivas). */
    public int tonoMinimo;
    /** Los bordes que miran a la luz se aclaran un tono: el canto de una placa. */
    public boolean canto;
    /** Donde la luz da de lleno salta un destello de este color (0 = no destella). */
    public int destello;

    private static final int NEGRO_DEL_CONTORNO = 0xFF0B090D;
    /** El color del que salen los cinco tonos (0xRRGGBB): lo usa el pintor de retratos para vestir al mismo personaje. */
    public final int rgb;

    /** ¿Es de anillas (cota de malla)? */
    public boolean deMalla() {
        return trama == Trama.MALLA;
    }

    /** ¿Es metal que brilla (placa, latón, hierro)? */
    public boolean deMetal() {
        return canto;
    }

    /** ¿Es pelo o pelaje? */
    public boolean dePelo() {
        return trama == Trama.PELO;
    }

    private Material(int rgb, double grano, boolean brilla, double contraste, int alfa) {
        this(rgb, grano, brilla, contraste, alfa, LUZ_COMUN);
    }

    private Material(int rgb, double grano, boolean brilla, double contraste, int alfa, int rgbDeLaLuz) {
        this.rgb = rgb & 0xFFFFFF;
        int base = Colores.opaco(rgb);
        int sombra = Colores.opaco(SOMBRA_COMUN), luz = Colores.opaco(rgbDeLaLuz);
        tonos[0] = Colores.mezcla(base, luz, (float) Math.min(0.9, 0.30 * contraste));
        tonos[1] = Colores.mezcla(base, luz, (float) (0.08 * contraste));
        tonos[2] = Colores.mezcla(base, sombra, (float) (0.24 * contraste));
        tonos[3] = Colores.mezcla(base, sombra, (float) Math.min(0.9, 0.52 * contraste));
        tonos[4] = Colores.mezcla(base, sombra, (float) Math.min(0.94, 0.76 * contraste));
        contorno = Colores.mezcla(tonos[4], NEGRO_DEL_CONTORNO, 0.72f);
        for (int i = 0; i < tonos.length; i++) {
            tonos[i] = (tonos[i] & 0xFFFFFF) | (alfa << 24);
        }
        this.grano = grano;
        this.brilla = brilla;
    }

    /** Tela corriente, piel: mate, con algo de trama. */
    public static Material mate(int rgb) {
        return new Material(rgb, 0.32, false, 1.0, 255);
    }

    /** Metal sin pulir (hierro viejo, bronce, latón): contraste y un brillo corto. */
    public static Material metal(int rgb) {
        Material m = new Material(rgb, 0.30, false, 1.45, 255, LUZ_DEL_METAL);
        m.canto = true;
        return m;
    }

    /** Placa de acero pulido: bandas limpias, destello y canto iluminado. */
    public static Material placa(int rgb) {
        Material m = new Material(rgb, 0.18, false, 1.6, 255, LUZ_DEL_METAL);
        m.canto = true;
        m.brillo = 0.10;
        m.destello = Colores.mezcla(m.tonos[0], 0xFFFFFFFF, 0.6f);
        return m;
    }

    /** Cota de malla: anillas al tresbolillo. */
    public static Material malla(int rgb) {
        Material m = new Material(rgb, 0.5, false, 1.35, 255, LUZ_DEL_METAL);
        m.trama = Trama.MALLA;
        return m;
    }

    /** Pelo, pelaje, cerdas: mechones. */
    public static Material aspero(int rgb) {
        Material m = new Material(rgb, 0.9, false, 1.2, 255);
        m.trama = Trama.PELO;
        return m;
    }

    /** Escamas. */
    public static Material escama(int rgb) {
        Material m = new Material(rgb, 0.5, false, 1.25, 255);
        m.trama = Trama.ESCAMA;
        return m;
    }

    /** Cuero: liso, con un brillo corto. */
    public static Material cuero(int rgb) {
        Material m = new Material(rgb, 0.25, false, 1.15, 255);
        m.brillo = -0.08;
        return m;
    }

    /** Lana y paño basto: mate, con grano, sin luces vivas. */
    public static Material lana(int rgb) {
        Material m = new Material(rgb, 0.5, false, 0.95, 255);
        m.tonoMinimo = 1;
        return m;
    }

    /** Seda y terciopelo: brillos largos y sombras hondas. */
    public static Material seda(int rgb) {
        Material m = new Material(rgb, 0.20, false, 1.35, 255);
        m.brillo = 0.12;
        return m;
    }

    /** Lino: claro, limpio, casi sin grano. */
    public static Material lino(int rgb) {
        return new Material(rgb, 0.22, false, 0.9, 255);
    }

    /** Piedra, roca: mate y con mucho grano. */
    public static Material rugoso(int rgb) {
        return new Material(rgb, 0.9, false, 1.1, 255);
    }

    /** Jubón estofado: pespuntes en rombo. */
    public static Material acolchado(int rgb) {
        Material m = new Material(rgb, 0.35, false, 1.0, 255);
        m.trama = Trama.ACOLCHADO;
        m.tonoMinimo = 1;
        return m;
    }

    /** Algo que da luz: llama, ojo que brilla, filo de fotones. */
    public static Material luz(int rgb) {
        return new Material(rgb, 0, true, 0.6, 255);
    }

    /** Algo que se transparenta: la estela de un tajo. 'alfa' de 0 a 255. */
    public static Material velo(int rgb, int alfa) {
        return new Material(rgb, 0, true, 0.2, alfa);
    }

    // ------------------------------------------------------------------
    // Los materiales que comparten personas, criaturas y armas
    // ------------------------------------------------------------------

    public static final Material PIEL = mate(0xA27052), PIEL_MORENA = mate(0x7A4A36), PIEL_PALIDA = mate(0xB89878);
    public static final Material PIEL_MUERTA = mate(0x6E7A68), CARNE_VIVA = mate(0x8E2A30), HUESO = mate(0xC8B48C);
    public static final Material NEGRO = lana(0x2A2026), PARDO = lana(0x4A3121), CUERO = cuero(0x6B4A2C), CUERO_OSCURO = cuero(0x3A2A20);
    public static final Material LANA_GRIS = lana(0x776A62), LANA_CRUDA = lana(0x9E8E7C), LINO = lino(0xB8A888);
    public static final Material GRANATE = lana(0x561E1E), ROJO = mate(0x8E2A22), VERDE = lana(0x2E3A25), VERDE_CLARO = seda(0x4A5230);
    public static final Material AZUL = seda(0x22273A), AZUL_CLARO = mate(0x2A3D44), OCRE = mate(0x8F6A3C), AZAFRAN = seda(0xB07A2A);
    public static final Material BLANCO_SUCIO = lino(0xC0B49A), MORADO = seda(0x3A1448), HABITO = lana(0x3E2C22);
    public static final Material ACERO = placa(0x7C8288), ACERO_OSCURO = placa(0x4A4E58), HIERRO_VIEJO = metal(0x5E5650);
    public static final Material LATON = metal(0xA8843C), BRONCE = metal(0x8F6A3C), ORO = placa(0xC89A4A);
    public static final Material MALLA = malla(0x6C7078), MALLA_OSCURA = malla(0x464852);
    public static final Material MADERA = mate(0x5A3E26), PAJA = mate(0xA8904E);
    public static final Material PELO_NEGRO = aspero(0x1E181C), PELO_CASTANO = aspero(0x3E2A1E), PELO_CANO = aspero(0x8E8A84), PELO_ROJIZO = aspero(0x6A3420);
    /** Pelo negro con canas: la barba de un hombre que entra en la madurez. */
    public static final Material PELO_ENTRECANO = aspero(0x4C4648);
    public static final Material PELAJE_GRIS = aspero(0x4E4A50), PELAJE_PARDO = aspero(0x4A3424), PELAJE_NEGRO = aspero(0x241E24);
    public static final Material BABA = cuero(0x5E7A3A), BILIS = mate(0x86A834), ICOR = cuero(0x3A5A3E), CARNE_AJENA = mate(0x8E1E54);
    public static final Material QUITINA = metal(0x2E2438), ESCAMA = escama(0x3A5A4A), ESCAMA_VIENTRE = mate(0x9A9A6A);
    public static final Material SOMBRA_VIVA = mate(0x1A1420), SUDARIO = lino(0x9A9A90);
    public static final Material LLAMA = luz(0xF4A62A), OJO_ROJO = luz(0xE03A20), OJO_BILIS = luz(0xC8F040), CIAN = luz(0x3FD2BC), FOTON = luz(0xE2F49C);
    public static final Material CHISPA = luz(0xFFE08A);
    public static final Material SANGRE = mate(0x7E1E1E), DIENTE = mate(0xD8CCA8), FAUCES = mate(0x3A0E14);
    /** El agua que chorrea de quien sale mojado (docs/encargos_viejos/ENCARGO_INTERFAZ.md). */
    public static final Material AGUA = mate(0x8FB4BC);
}
