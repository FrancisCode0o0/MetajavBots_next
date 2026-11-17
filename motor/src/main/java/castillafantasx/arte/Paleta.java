package castillafantasx.arte;

/**
 * PALETA — Los 32 colores propios del juego, cada uno con su letra.
 * Los dibujos hechos a mano (MatricesPersonas) y los que fabrica el Taller
 * usan estas letras: así todo lo nuestro comparte la misma gama oscura.
 */
public final class Paleta {

    private Paleta() { }

    public static final String LETRAS = "abcdefghijklmnopqrstuvwxyzABCDEF";

    private static final int[] RGB = {
        0x0B090D, // a abismo        sombra máxima
        0x1A1418, // b noche         borde de sombra de telas oscuras
        0x2A2026, // c sombra        sombra de piedra y lana
        0x3B3236, // d piedra_osc    piedra base oscura, calzas
        0x554A4A, // e piedra        piedra, lana en sombra
        0x776A62, // f piedra_clara  arista de sillar, lana
        0x9E8E7C, // g sillar_luz    luz de piedra y lana
        0x2E1F17, // h cuero_osc     borde de cuero, madera y piel
        0x4A3121, // i cuero         cuero, tierra batida
        0x6B4A2C, // j madera        madera, luz del cuero
        0x8F6A3C, // k ocre          latón, cuerda, luz de madera
        0x4E2C22, // l piel_sombra   piel y carne en sombra
        0x7A4A36, // m piel          piel base
        0xA27052, // n piel_luz      piel con luz
        0x10170F, // o hoja_osc      sombra vegetal
        0x1C2819, // p hoja          base vegetal
        0x2E3A25, // q hierba        hierba base
        0x4A5230, // r hierba_luz    brotes con luz
        0x141C25, // s agua_honda    agua profunda
        0x2A3D44, // t agua          agua, brillo de baba
        0x2E1012, // u granate_osc   sombra de rojo
        0x561E1E, // v granate       perpunte, sangre seca
        0x7E3426, // w teja          teja, ladrillo, luz del rojo
        0x22273A, // x indigo        sombra del acero
        0x7C8288, // y acero         hierro
        0xC8B48C, // z hueso         brillo, dientes, pergamino
        0x0D0716, // A vacio         fondo extradimensional
        0x3A1448, // B morado        base extradimensional
        0x8E1E54, // C carne         biomasa, lo vivo por dentro
        0x86A834, // D bilis         pus, ácido, ojos de fuera
        0x3FD2BC, // E cian          luz de máquina
        0xE2F49C  // F fosforo       luz máxima extradimensional
    };

    /** El color de una letra, como 0xAARRGGBB opaco. */
    public static int argb(char letra) {
        int i = LETRAS.indexOf(letra);
        if (i < 0) {
            throw new IllegalArgumentException("La paleta no tiene la letra '" + letra + "'");
        }
        return 0xFF000000 | RGB[i];
    }

    // Colores de la interfaz, con nombre (0xRRGGBB).
    public static final int TINTA = 0x0B090D;
    public static final int HUESO = 0xC8B48C;
    public static final int PIEDRA_CLARA = 0x9E8E7C;
    public static final int PIEDRA = 0x554A4A;
    public static final int ORO = 0xC89A4A;
    public static final int SANGRE = 0x8E2222;
    public static final int SANGRE_LUZ = 0xC23A2A;
    public static final int CIAN = 0x3FD2BC;
    public static final int BILIS = 0x86A834;
}
