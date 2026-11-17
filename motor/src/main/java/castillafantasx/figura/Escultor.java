package castillafantasx.figura;

import castillafantasx.arte.Colores;
import castillafantasx.arte.Imagen;

/**
 * ESCULTOR — Convierte una figura hecha de piezas 3D en un dibujo de píxeles.
 *
 * Solo hay tres piezas: BOLA, HUESO (un tubo entre dos puntos, que puede
 * estrecharse) y LÁMINA (una superficie plana: capas, hojas de arma, alas).
 * Con ellas se hace todo: personas, bestias y armas. Por eso todo comparte el
 * mismo acabado.
 *
 * Cómo trabaja:
 *   1. Cada pieza se pinta píxel a píxel. De cada píxel se apunta el color y
 *      lo cerca que está del que mira ('hondo'): si luego llega otra pieza
 *      que queda detrás, no lo tapa. Así no importa en qué orden se pinten.
 *   2. El tono de cada píxel sale de la luz que le da (viene de arriba a la
 *      izquierda) y se elige entre los cinco del Material; en los saltos de
 *      tono se mezclan puntitos (trama), como en los grabados de las referencias.
 *   3. terminar() le da el acabado de dibujo hecho a mano: limpia los píxeles
 *      sueltos, rodea la figura con un contorno oscuro del color de cada
 *      material, separa con una línea las partes que se montan (un brazo por
 *      delante del pecho), echa la sombra de lo que tapa la luz y aclara el
 *      canto de las placas.
 *
 * Además de las piezas hay TRAZOS (surco y realce): líneas finas que oscurecen
 * o aclaran un tono lo ya pintado. Con ellos se dibujan pliegues, costuras,
 * dedos y rasgos, como se haría a mano.
 */
public final class Escultor {

    // De dónde viene la luz, en la pantalla: de la izquierda, de arriba y de delante.
    private static final double LUZ_X = -0.50, LUZ_Y = -0.62, LUZ_Z = 0.60;
    /** Cuánto se mira desde arriba en la vista cenital ¾ (radianes). */
    private static final double INCLINACION_CENITAL = 0.50;
    /** En la vista cenital, lo que mide en la pantalla un píxel de suelo hacia el fondo (de lado mide 1). */
    public static final double FONDO_CENITAL = Math.sin(INCLINACION_CENITAL);
    private static final int[][] TRAMA = { { 0, 8, 2, 10 }, { 12, 4, 14, 6 }, { 3, 11, 1, 9 }, { 15, 7, 13, 5 } };
    private static final int CONTORNO = 0xFF0B090D;
    private static final float ESCALON = 2.4f;        // diferencia de 'hondo' que cuenta como borde interior
    private static final float ENTRE_PARTES = 0.9f;   // lo mismo entre dos partes distintas del cuerpo: basta menos
    private static final float SOMBRA_ARROJADA = 3.2f; // cuánto tiene que sobresalir algo para dar sombra a lo de debajo
    private static final double LUZ_DEL_DESTELLO = 0.93;
    private static final float NADA = -1e9f;

    public final Imagen lienzo;
    private final float[] hondo;
    private final byte[] marca;
    // De cada píxel pintado: su material, su tono (0 a 4; -1 = destello) y la parte del cuerpo a la que pertenece.
    private final Material[] deQue;
    private final byte[] tono;
    private final short[] parteDe;
    private final boolean[] trazado;          // ya lo ha tocado un surco o un realce
    private final boolean[] esContorno;
    private short parte;
    // Los píxeles de una mancha del suelo: no llevan contorno, y el de la figura pasa por encima de ellos.
    private final boolean[] deSuelo;
    private boolean pintandoSuelo;
    // El resplandor pedido para esta figura (0 = ninguno): lo pone terminar().
    private double resplandor;
    private int colorDelResplandor;
    private static final int RADIO_DEL_HALO = 2;       // píxeles que asoma el resplandor alrededor de la figura
    // El resplandor de la maldición del alba (encargo de la supervivencia): solo las armas y la armadura.
    private double resplandorDelArmamento;
    private int colorDelArmamento;
    private final boolean[] partesDelArmamento = new boolean[64];

    // La cámara: cómo se pasa de un punto de la figura a un píxel.
    private double ax, ay, bx, by, sinI, cosI, escala, origenX, origenY, focal;
    private double finura = 1;                   // parte del paso normal con que se rellena un músculo (menos = más tupido)
    private P3 centro = P3.CERO;                 // desde dónde se mira, en las medidas de la figura (solo mirarAbajoPorSusOjos lo mueve)
    // La caja que encierra lo pintado (para no repasar el lienzo entero).
    private int minX, minY, maxX, maxY;
    // Resultado de proyectar(): píxel, hondo y cuánto encoge la perspectiva.
    private double px, py, pz, pk;
    // La lupa (ampliar): desde dónde crece lo que se esculpe y cuántas veces.
    private P3 lupaEn;
    private double lupa = 1;
    // Las láminas son lisas: un solo tono por cara, sin puntitos.
    private boolean conTrama = true;
    // El agua (docs/encargos_viejos/ENCARGO_INTERFAZ.md): por debajo de esta altura de la figura no se pinta nada (está bajo el agua),
    // y lo mojada que va la ropa (0 a 1) la oscurece al terminar. Se olvidan al colocar la cámara.
    private double nivelDelAgua = SIN_AGUA, mojado;
    private static final double SIN_AGUA = -1e9;
    /** Cuánto se oscurece lo pintado cuando está mojado del todo (res/datos/agua.txt, [cuerpo] oscurece). */
    private static final float OSCURECE_MOJADO = (float) castillafantasx.agua.CifrasDeAgua.OSCURECE;

    // El editor de figuras (Retoque): lo que cambia cada pieza antes de pintarla. Sin retoques, nada de esto se usa.
    private Retoque retoquePropio, retoqueDePose, retoqueDeFuera;
    private boolean hayRetoque;
    private double grosorAhora = 1;
    // Cada pieza (bola, hueso, lámina…) lleva su número: parte * 1000 + su orden dentro de la parte.
    private final short[] piezasDeParte = new short[64];
    private int piezaAhora, injerto;
    private boolean enRodela, piezaOculta;
    // De cada píxel, qué pieza lo pintó (más 1; 0 = ninguna): solo si se pide con apuntarPiezas().
    private int[] piezaDe;

    public Escultor(int ancho, int alto) {
        lienzo = new Imagen(ancho, alto);
        hondo = new float[ancho * alto];
        marca = new byte[ancho * alto];
        deQue = new Material[ancho * alto];
        tono = new byte[ancho * alto];
        parteDe = new short[ancho * alto];
        trazado = new boolean[ancho * alto];
        esContorno = new boolean[ancho * alto];
        deSuelo = new boolean[ancho * alto];
        java.util.Arrays.fill(hondo, NADA);
        minX = ancho;
        minY = alto;
        maxX = -1;
        maxY = -1;
    }

    // ------------------------------------------------------------------
    // Cámaras
    // ------------------------------------------------------------------

    /**
     * Vista cenital ¾ (la superficie). 'rumbo' es hacia dónde mira la figura en
     * el mapa: 0 = este, y crece hacia el sur. Sus pies quedan en (origenX, origenY).
     */
    public void mirarDesdeArriba(double rumbo, double escala, double origenX, double origenY) {
        camara(rumbo, INCLINACION_CENITAL, escala, origenX, origenY, 0);
    }

    /** Cuánto se mira desde arriba en la vista de frente (radianes): casi nada. */
    public static final double INCLINACION_DE_FRENTE = 0.10;

    /** Vista de frente (la mazmorra). 'rumbo' = π/2: la figura mira al que la ve. */
    public void mirarDeFrente(double rumbo, double escala, double origenX, double origenY) {
        camara(rumbo, INCLINACION_DE_FRENTE, escala, origenX, origenY, 0);
    }

    /** Como de frente, pero mirando desde arriba lo que se diga (radianes): así se ve en la mazmorra lo que está tendido en el suelo. */
    public void mirarInclinado(double rumbo, double inclinacion, double escala, double origenX, double origenY) {
        camara(rumbo, inclinacion, escala, origenX, origenY, 0);
    }

    /**
     * Vista por los ojos de la figura (primera persona): x a la derecha, z
     * arriba, y hacia el fondo; lo lejano se ve más pequeño.
     */
    public void mirarPorSusOjos(double escala, double origenX, double origenY, double focal) {
        camara(-Math.PI / 2, 0, escala, origenX, origenY, focal);
    }

    /**
     * Vista por sus ojos mirando hacia abajo (sus piernas y sus pies): los ojos están en
     * 'ojos', en las medidas de la figura, y la vista baja 'inclinacion' radianes.
     */
    public void mirarAbajoPorSusOjos(P3 ojos, double inclinacion, double escala, double origenX, double origenY, double focal) {
        camara(-Math.PI / 2, inclinacion, escala, origenX, origenY, focal);
        centro = ojos;
        finura = 0.3;       // las piernas apuntan casi al que mira: con el paso normal se les verían los anillos
    }

    private void camara(double rumbo, double inclinacion, double escala, double origenX, double origenY, double focal) {
        limpiar();
        ampliar(null, 1);
        centro = P3.CERO;
        finura = 1;
        // La derecha de la figura y su frente, en el mapa.
        ax = -Math.sin(rumbo);
        ay = Math.cos(rumbo);
        bx = Math.cos(rumbo);
        by = Math.sin(rumbo);
        sinI = Math.sin(inclinacion);
        cosI = Math.cos(inclinacion);
        this.escala = escala;
        this.origenX = origenX;
        this.origenY = origenY;
        this.focal = focal;
        nivelDelAgua = SIN_AGUA;
        mojado = 0;
    }

    /**
     * El agua: lo que quede por debajo de 'nivel' (píxeles de figura, medidos
     * desde los pies hacia arriba) no se pinta, como si lo tapara el agua; y
     * 'mojado' (0 a 1) oscurece toda la figura al terminar. Se dice después de
     * colocar la cámara y antes de esculpir. Solo recorta en las vistas sin
     * perspectiva (desde arriba y de frente): por sus propios ojos no hace falta.
     */
    public void bajoElAgua(double nivel, double mojado) {
        nivelDelAgua = nivel > 0 ? nivel : SIN_AGUA;
        this.mojado = Math.max(0, Math.min(1, mojado));
    }

    private void limpiar() {
        for (int y = Math.max(0, minY - 1); y <= Math.min(lienzo.alto - 1, maxY + 1); y++) {
            for (int x = Math.max(0, minX - 1); x <= Math.min(lienzo.ancho - 1, maxX + 1); x++) {
                lienzo.px[y * lienzo.ancho + x] = 0;
                hondo[y * lienzo.ancho + x] = NADA;
                deSuelo[y * lienzo.ancho + x] = false;
                deQue[y * lienzo.ancho + x] = null;
                trazado[y * lienzo.ancho + x] = false;
                esContorno[y * lienzo.ancho + x] = false;
            }
        }
        minX = lienzo.ancho;
        minY = lienzo.alto;
        maxX = -1;
        maxY = -1;
        resplandor = 0;
        resplandorDelArmamento = 0;
        parte = 0;
        retoquePropio = null;
        retoqueDePose = null;
        retoqueDeFuera = null;
        hayRetoque = false;
        piezaOculta = false;
        grosorAhora = 1;
        injerto = 0;
        java.util.Arrays.fill(piezasDeParte, (short) 0);
    }

    // ------------------------------------------------------------------
    // El editor de figuras: retoques y piezas
    // ------------------------------------------------------------------

    /**
     * Lo llama cada figura al empezar a esculpirse: desde aquí valen los
     * retoques que el editor le haya hecho a su forma y a su pose en ese
     * gesto (figura.Retoques). figura((Figura) null, null) los quita.
     */
    public void figura(Figura f, Instante m) {
        java.util.Arrays.fill(piezasDeParte, (short) 0);
        retoquePropio = null;
        retoqueDePose = null;
        if (f != null && Retoques.hay()) {
            String clave = Retoques.claveDe(f);
            retoquePropio = Retoques.de(clave);
            retoqueDePose = m == null ? null : Retoques.dePose(clave, m.gesto);
        }
        alCambiarDeRetoque();
    }

    /** Lo mismo para lo que no es una Figura (los brazos de la primera persona): por su clave. */
    public void figura(String clave, Instante m) {
        java.util.Arrays.fill(piezasDeParte, (short) 0);
        retoquePropio = Retoques.hay() ? Retoques.de(clave) : null;
        retoqueDePose = Retoques.hay() && m != null ? Retoques.dePose(clave, m.gesto) : null;
        alCambiarDeRetoque();
    }

    /**
     * Un retoque puesto desde fuera, además de los de la figura: así un ser
     * hecho de trozos (FiguraCompuesta) coloca cada injerto. 'cual' es el
     * número del injerto (para saber luego de quién es cada píxel). null lo quita.
     */
    public void injertar(Retoque r, int cual) {
        retoqueDeFuera = r;
        injerto = cual;
        piezaOculta = false;
        if (r != null) r.empezar();
        hayRetoque = retoquePropio != null || retoqueDePose != null || retoqueDeFuera != null;
        if (!hayRetoque) grosorAhora = 1;
    }

    private void alCambiarDeRetoque() {
        if (retoquePropio != null) retoquePropio.empezar();
        if (retoqueDePose != null) retoqueDePose.empezar();
        hayRetoque = retoquePropio != null || retoqueDePose != null || retoqueDeFuera != null;
        if (!hayRetoque) grosorAhora = 1;
    }

    /** Desde ahora se apunta qué pieza pinta cada píxel (para elegirlas con el puntero en el editor). */
    public void apuntarPiezas() {
        if (piezaDe == null) piezaDe = new int[lienzo.px.length];
    }

    /** La pieza que se ve en ese píxel: injerto * 100000 + parte * 1000 + orden; -1 si ahí no hay figura. Pide apuntarPiezas(). */
    public int piezaEn(int x, int y) {
        if (piezaDe == null || x < 0 || y < 0 || x >= lienzo.ancho || y >= lienzo.alto) return -1;
        int i = y * lienzo.ancho + x;
        return hondo[i] == NADA ? -1 : piezaDe[i] - 1;
    }

    /** El color (RRGGBB) del que salen los tonos del material pintado en ese píxel; -1 si ahí no hay figura. */
    public int colorEn(int x, int y) {
        if (x < 0 || y < 0 || x >= lienzo.ancho || y >= lienzo.alto) return -1;
        Material m = deQue[y * lienzo.ancho + x];
        return m == null ? -1 : m.rgb;
    }

    /** Empieza una pieza: le da su número y dice de qué material se pinta al final (null = no se pinta). */
    private Material pieza(Material m) {
        if (!enRodela) piezaAhora = (parte & 63) * 1000 + piezasDeParte[parte & 63]++;
        if (!hayRetoque) return m;
        if (retoquePropio != null && m != null) m = retoquePropio.material(m, parte, piezaAhora);
        if (retoqueDePose != null && m != null) m = retoqueDePose.material(m, parte, piezaAhora);
        if (retoqueDeFuera != null && m != null) m = retoqueDeFuera.material(m, parte, piezaAhora);
        // Los surcos y realces que vengan detrás son de esta pieza: si no se pinta, ellos tampoco.
        piezaOculta = m == null;
        return m;
    }

    /** Un punto de la figura, pasado por los retoques; deja en grosorAhora por cuánto engorda ahí la pieza. */
    private P3 retocado(P3 p) {
        grosorAhora = 1;
        if (retoquePropio != null) {
            grosorAhora *= retoquePropio.grosor(p, parte, piezaAhora);
            p = retoquePropio.punto(p, parte, piezaAhora);
        }
        if (retoqueDePose != null) {
            grosorAhora *= retoqueDePose.grosor(p, parte, piezaAhora);
            p = retoqueDePose.punto(p, parte, piezaAhora);
        }
        if (retoqueDeFuera != null) {
            grosorAhora *= retoqueDeFuera.grosor(p, parte, piezaAhora);
            p = retoqueDeFuera.punto(p, parte, piezaAhora);
        }
        return p;
    }

    /**
     * Desde ahora, lo que se pinte es de esta parte del cuerpo (un número
     * cualquiera: 1 el tronco, 2 un brazo…). Donde una parte tapa a otra,
     * terminar() las separa con una línea, aunque sean del mismo color.
     */
    public void parte(int cual) {
        parte = (short) cual;
    }

    /**
     * Pide que la figura salga envuelta en un resplandor de ese color: toda
     * ella más clara y con un halo fino alrededor. 'fuerza' de 0 a 1. Lo pinta terminar().
     */
    public void resplandecer(int rgb, double fuerza) {
        colorDelResplandor = rgb | 0xFF000000;
        resplandor = Math.max(0, Math.min(1, fuerza));
    }

    /**
     * Pide que resplandezcan solo las armas y la armadura de la figura (la
     * maldición del alba): lo de esas partes del cuerpo y todo lo que sea de
     * metal o de malla, más claro y con su halo. 'fuerza' de 0 a 1. Lo pinta terminar().
     */
    public void resplandecerElArmamento(int rgb, double fuerza, int... partes) {
        colorDelArmamento = rgb | 0xFF000000;
        resplandorDelArmamento = Math.max(0, Math.min(1, fuerza));
        java.util.Arrays.fill(partesDelArmamento, false);
        for (int cual : partes) partesDelArmamento[cual] = true;
    }

    private boolean esDelArmamento(int i) {
        if (hondo[i] == NADA || deQue[i] == null) return false;
        return partesDelArmamento[parteDe[i] & 63] || deQue[i].deMetal() || deQue[i].deMalla();
    }

    /** El resplandor del armamento: aclara sus píxeles y deja un halo fino alrededor de ellos. */
    private void pintarResplandorDelArmamento() {
        int w = lienzo.ancho, r = RADIO_DEL_HALO;
        int x0 = Math.max(r, minX - r), x1 = Math.min(w - 1 - r, maxX + r);
        int y0 = Math.max(r, minY - r), y1 = Math.min(lienzo.alto - 1 - r, maxY + r);
        double f = resplandorDelArmamento;
        int halo = (colorDelArmamento & 0xFFFFFF) | ((int) (130 * f) << 24);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                marca[i] = 0;
                if (esDelArmamento(i)) {
                    marca[i] = 1;
                } else if (hondo[i] == NADA) {
                    for (int dy = -r; dy <= r && marca[i] == 0; dy++) {
                        for (int dx = -r; dx <= r; dx++) {
                            if (dx * dx + dy * dy <= r * r && esDelArmamento(i + dy * w + dx)) {
                                marca[i] = 3;
                                break;
                            }
                        }
                    }
                }
            }
        }
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                if (marca[i] == 1) lienzo.px[i] = Colores.mezcla(lienzo.px[i], colorDelArmamento, (float) (0.65 * f));
                else if (marca[i] == 3 && (lienzo.px[i] >>> 24) < (halo >>> 24)) lienzo.px[i] = halo;
            }
        }
        minX = Math.max(0, minX - r);
        minY = Math.max(0, minY - r);
        maxX = Math.min(w - 1, maxX + r);
        maxY = Math.min(lienzo.alto - 1, maxY + r);
    }

    /**
     * Desde ahora, todo lo que se esculpa sale 'veces' más grande, creciendo desde 'pivote'
     * (por ejemplo, el arma desde la mano que la empuña). ampliar(null, 1) lo deja como estaba.
     */
    public void ampliar(P3 pivote, double veces) {
        lupaEn = pivote;
        lupa = pivote == null ? 1 : veces;
    }

    private void proyectar(P3 p) {
        if (hayRetoque) p = retocado(p);
        if (lupaEn != null) p = lupaEn.mas(p.menos(lupaEn).por(lupa));
        double x = p.x - centro.x, y = p.y - centro.y, z = p.z - centro.z;
        double mx = x * ax + y * ay;              // a la derecha de la pantalla
        double my = x * bx + y * by;              // hacia el que mira
        double cerca = my * cosI + z * sinI;      // lo cerca que queda del que mira
        pk = focal > 0 ? focal / Math.max(8, focal - cerca) : 1;
        px = origenX + mx * escala * pk;
        py = origenY + (my * sinI - z * cosI) * escala * pk;
        pz = cerca * escala;
        pk *= lupa;          // de aquí en adelante 'pk' solo se usa para los gruesos, que crecen con la lupa
        if (hayRetoque) pk *= grosorAhora;
    }

    /** Cuántos píxeles mide un píxel de figura (para saber si se dibuja de cerca). */
    public double escala() {
        return escala;
    }

    /** Hacia dónde queda el que mira, en las medidas de la figura: sirve para poner una pieza delante de otra en cualquier vista. */
    public P3 haciaElQueMira() {
        return new P3(bx * cosI, by * cosI, sinI);
    }

    /** Hacia dónde queda la derecha de la pantalla, en las medidas de la figura. */
    public P3 aLaDerecha() {
        return new P3(ax, ay, 0);
    }

    /** Lo que hay que mover un punto de la figura para que se mueva (dx, dy) píxeles en el lienzo, sin acercarse ni alejarse del que mira. */
    public P3 enLaFigura(double dx, double dy) {
        double k = 1 / escala;
        return new P3((ax * dx + bx * sinI * dy) * k, (ay * dx + by * sinI * dy) * k, -cosI * dy * k);
    }

    /** En qué píxel del lienzo cae un punto de la figura tal cual, sin pasarlo por los retoques (para los tiradores del editor): {x, y}. */
    public double[] pixelLimpio(P3 p) {
        boolean habia = hayRetoque;
        hayRetoque = false;
        proyectar(p);
        hayRetoque = habia;
        return new double[] { px, py };
    }

    /** En qué píxel del lienzo cae un punto de la figura: {x, y}. */
    public double[] pixelDe(P3 p) {
        proyectar(p);
        return new double[] { px, py };
    }

    /**
     * Cuántos píxeles de esa parte se ven ahora mismo (antes de terminar()).
     * Para las pruebas: así se comprueba que un brazo no ha quedado escondido.
     */
    public int pixelesDeLaParte(int cual) {
        int n = 0;
        for (int y = Math.max(0, minY); y <= maxY; y++) {
            for (int x = Math.max(0, minX); x <= maxX; x++) {
                int i = y * lienzo.ancho + x;
                if (hondo[i] != NADA && parteDe[i] == cual) n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------
    // Las tres piezas
    // ------------------------------------------------------------------

    public void bola(P3 centro, double radio, Material m) {
        m = pieza(m);
        if (m == null) return;
        proyectar(centro);
        disco(px, py, pz, radio * escala * pk, m);
    }

    /** Un tubo de 'a' a 'b' con radio 'ra' en un extremo y 'rb' en el otro. */
    public void hueso(P3 a, P3 b, double ra, double rb, Material m) {
        m = pieza(m);
        if (m == null) return;
        proyectar(a);
        double x0 = px, y0 = py, z0 = pz, r0 = ra * escala * pk;
        proyectar(b);
        double x1 = px, y1 = py, z1 = pz, r1 = rb * escala * pk;
        // Bolas bien juntas, también cuando el tubo se aleja hacia el fondo: si no, se le notarían anillos.
        double largo = Math.max(Math.hypot(x1 - x0, y1 - y0), Math.abs(z1 - z0));
        double paso = Math.max(0.5, Math.min(r0, r1) * 0.4);
        int pasos = (int) Math.ceil(largo / paso) + 1;
        for (int i = 0; i <= pasos; i++) {
            double t = i / (double) pasos;
            disco(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t, r0 + (r1 - r0) * t, m);
        }
    }

    public void hueso(P3 a, P3 b, double radio, Material m) {
        hueso(a, b, radio, radio, m);
    }

    /**
     * Un hueso con carne: como hueso(), pero engorda 'panza' píxeles a la
     * altura 'donde' (0 = en 'a', 1 = en 'b'). Un antebrazo, un gemelo, un muslo.
     */
    public void musculo(P3 a, P3 b, double ra, double rb, double panza, double donde, Material m) {
        m = pieza(m);
        if (m == null) return;
        proyectar(a);
        double x0 = px, y0 = py, z0 = pz, k0 = escala * pk;
        proyectar(b);
        double x1 = px, y1 = py, z1 = pz, k1 = escala * pk;
        double largo = Math.max(Math.hypot(x1 - x0, y1 - y0), Math.abs(z1 - z0));
        double paso = Math.max(0.5, Math.min(ra * k0, rb * k1) * 0.4) * finura;
        int pasos = (int) Math.ceil(largo / paso) + 1;
        for (int i = 0; i <= pasos; i++) {
            double t = i / (double) pasos;
            double lejos = (t - donde) / 0.3;
            double r = ra + (rb - ra) * t + panza * Math.exp(-lejos * lejos);
            disco(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, z0 + (z1 - z0) * t, r * (k0 + (k1 - k0) * t), m);
        }
    }

    /**
     * Un paño: una tela que pasa por una rejilla de puntos (filas de arriba
     * abajo). A diferencia de la lámina, la luz cambia suavemente de un punto
     * a otro: los pliegues de la rejilla se ven como pliegues, no como facetas.
     */
    private double[] panoX = new double[0], panoY = new double[0], panoZ = new double[0], panoLuz = new double[0];

    public void pano(P3[][] rejilla, Material m) {
        pano(rejilla, m, false);
    }

    /** Un paño; si es 'cerrado', da la vuelta entera (una falda): su última columna es otra vez la primera. */
    public void pano(P3[][] rejilla, Material m, boolean cerrado) {
        m = pieza(m);
        if (m == null) return;
        int filas = rejilla.length, columnas = rejilla[0].length;
        // Las cuentas de cada punto de la rejilla, en las mesas del Escultor (se reutilizan: nada de basura por cada paño).
        int puntos = filas * columnas;
        if (panoX.length < puntos) {
            panoX = new double[puntos];
            panoY = new double[puntos];
            panoZ = new double[puntos];
            panoLuz = new double[puntos];
        }
        double[] x = panoX, y = panoY, z = panoZ, luz = panoLuz;
        int k = columnas;      // de [f][c] a [f * k + c]
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                proyectar(rejilla[f][c]);
                x[f * k + c] = px;
                y[f * k + c] = py;
                z[f * k + c] = pz;
            }
        }
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                // Hacia dónde mira la tela en este punto: de sus vecinos de la rejilla.
                int c0 = Math.max(0, c - 1), c1 = Math.min(columnas - 1, c + 1), f0 = Math.max(0, f - 1), f1 = Math.min(filas - 1, f + 1);
                if (cerrado && c == 0) c0 = columnas - 2;
                if (cerrado && c == columnas - 1) c1 = 1;
                double ux = x[f * k + c1] - x[f * k + c0], uy = y[f * k + c1] - y[f * k + c0], uz = z[f * k + c1] - z[f * k + c0];
                double vx = x[f1 * k + c] - x[f0 * k + c], vy = y[f1 * k + c] - y[f0 * k + c], vz = z[f1 * k + c] - z[f0 * k + c];
                double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
                double n = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (n < 1e-9) {
                    luz[f * k + c] = 0.3;
                    continue;
                }
                if (nz < 0) n = -n;
                luz[f * k + c] = (nx * LUZ_X + ny * LUZ_Y + nz * LUZ_Z) / n;
            }
        }
        for (int f = 0; f + 1 < filas; f++) {
            for (int c = 0; c + 1 < columnas; c++) {
                int pasosU = (int) Math.ceil(Math.max(Math.hypot(x[f * k + c + 1] - x[f * k + c], y[f * k + c + 1] - y[f * k + c]),
                        Math.hypot(x[(f + 1) * k + c + 1] - x[(f + 1) * k + c], y[(f + 1) * k + c + 1] - y[(f + 1) * k + c])) * 1.5) + 1;
                int pasosV = (int) Math.ceil(Math.max(Math.hypot(x[(f + 1) * k + c] - x[f * k + c], y[(f + 1) * k + c] - y[f * k + c]),
                        Math.hypot(x[(f + 1) * k + c + 1] - x[f * k + c + 1], y[(f + 1) * k + c + 1] - y[f * k + c + 1])) * 1.5) + 1;
                for (int j = 0; j <= pasosV; j++) {
                    double v = j / (double) pasosV;
                    for (int i = 0; i <= pasosU; i++) {
                        double u = i / (double) pasosU;
                        double a = (1 - u) * (1 - v), b = u * (1 - v), d = (1 - u) * v, e = u * v;
                        pixel((int) Math.floor(a * x[f * k + c] + b * x[f * k + c + 1] + d * x[(f + 1) * k + c] + e * x[(f + 1) * k + c + 1]),
                                (int) Math.floor(a * y[f * k + c] + b * y[f * k + c + 1] + d * y[(f + 1) * k + c] + e * y[(f + 1) * k + c + 1]),
                                (float) (a * z[f * k + c] + b * z[f * k + c + 1] + d * z[(f + 1) * k + c] + e * z[(f + 1) * k + c + 1]),
                                a * luz[f * k + c] + b * luz[f * k + c + 1] + d * luz[(f + 1) * k + c] + e * luz[(f + 1) * k + c + 1], m);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Los trazos: líneas finas sobre lo ya pintado
    // ------------------------------------------------------------------

    /** Una línea que oscurece un tono lo que hay pintado justo ahí: un pliegue, una costura, la raya entre dos dedos. */
    public void surco(P3 a, P3 b) {
        trazo(a, b, 1);
    }

    /** Una línea que aclara un tono lo que hay pintado justo ahí: una arista, el lomo de un pliegue, un nudillo. */
    public void realce(P3 a, P3 b) {
        trazo(a, b, -1);
    }

    private void trazo(P3 a, P3 b, int cuanto) {
        if (piezaOculta) return;
        proyectar(a);
        double x0 = px, y0 = py, z0 = pz;
        proyectar(b);
        double x1 = px, y1 = py, z1 = pz;
        // Solo toca lo que está a ras de la línea: ni lo que la tapa por delante ni lo que queda lejos por detrás.
        double holgura = 1.1 * escala * pk + 0.9;
        int pasos = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))) + 1;
        for (int k = 0; k <= pasos; k++) {
            double t = k / (double) pasos;
            int x = (int) Math.floor(x0 + (x1 - x0) * t), y = (int) Math.floor(y0 + (y1 - y0) * t);
            if (x < 0 || y < 0 || x >= lienzo.ancho || y >= lienzo.alto) continue;
            int i = y * lienzo.ancho + x;
            Material m = deQue[i];
            if (m == null || m.brilla || trazado[i] || Math.abs(z0 + (z1 - z0) * t - hondo[i]) > holgura) continue;
            trazado[i] = true;
            tono[i] = (byte) Math.max(m.tonoMinimo, Math.min(4, Math.max(0, tono[i]) + cuanto));
            lienzo.px[i] = m.tonos[tono[i]];
        }
    }

    /** Una superficie plana de cuatro esquinas, dadas en orden (a-b arriba, d-c abajo). */
    public void lamina(P3 a, P3 b, P3 c, P3 d, Material m) {
        m = pieza(m);
        if (m == null) return;
        proyectar(a);
        double xa = px, ya = py, za = pz;
        proyectar(b);
        double xb = px, yb = py, zb = pz;
        proyectar(c);
        double xc = px, yc = py, zc = pz;
        proyectar(d);
        double xd = px, yd = py, zd = pz;
        // La luz que recibe: una sola para toda la lámina, según hacia dónde mira su cara.
        double ux = xb - xa, uy = yb - ya, uz = zb - za, vx = xd - xa, vy = yd - ya, vz = zd - za;
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double n = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (n < 1e-9) return;
        if (nz < 0) n = -n;                       // la cara que se ve es la que mira al que mira
        double luz = (nx * LUZ_X + ny * LUZ_Y + nz * LUZ_Z) / n;
        conTrama = false;
        int pasosU = (int) Math.ceil(Math.max(Math.hypot(ux, uy), Math.hypot(xc - xd, yc - yd)) * 1.5) + 1;
        int pasosV = (int) Math.ceil(Math.max(Math.hypot(vx, vy), Math.hypot(xc - xb, yc - yb)) * 1.5) + 1;
        for (int j = 0; j <= pasosV; j++) {
            double v = j / (double) pasosV;
            double x0 = xa + (xd - xa) * v, y0 = ya + (yd - ya) * v, z0 = za + (zd - za) * v;
            double x1 = xb + (xc - xb) * v, y1 = yb + (yc - yb) * v, z1 = zb + (zc - zb) * v;
            for (int i = 0; i <= pasosU; i++) {
                double u = i / (double) pasosU;
                pixel((int) Math.floor(x0 + (x1 - x0) * u), (int) Math.floor(y0 + (y1 - y0) * u),
                        (float) (z0 + (z1 - z0) * u), luz, m);
            }
        }
        conTrama = true;
    }

    /** Una lámina redonda (un ala de sombrero, un escudo): centro y sus dos medios ejes. */
    public void rodela(P3 centro, P3 ejeU, P3 ejeV, Material m) {
        int gajos = 10;
        P3 antes = centro.mas(ejeU);
        // Para los retoques, toda la rodela es una sola pieza: sus gajos no cuentan aparte.
        boolean yaDentro = enRodela;
        if (!yaDentro) pieza(m);
        enRodela = true;
        for (int i = 1; i <= gajos; i++) {
            double a = i * 2 * Math.PI / gajos;
            P3 ahora = centro.mas(ejeU.por(Math.cos(a))).mas(ejeV.por(Math.sin(a)));
            lamina(centro, antes, ahora, ahora, m);
            antes = ahora;
        }
        enRodela = yaDentro;
    }

    /**
     * Una mancha en el suelo (la sangre bajo un cuerpo): una rodela de un
     * material que se transparenta. No lleva contorno, y el contorno de la
     * figura se traza por encima de ella.
     */
    public void mancha(P3 centro, P3 ejeU, P3 ejeV, Material m) {
        pintandoSuelo = true;
        rodela(centro, ejeU, ejeV, m);
        pintandoSuelo = false;
    }

    private void disco(double cx, double cy, double cz, double r, Material m) {
        if (r < 0.75) {                            // más fino que un píxel: un punto
            pixel((int) Math.floor(cx), (int) Math.floor(cy), (float) cz, 0.45, m);
            return;
        }
        int x0 = (int) Math.floor(cx - r), x1 = (int) Math.ceil(cx + r);
        int y0 = (int) Math.floor(cy - r), y1 = (int) Math.ceil(cy + r);
        double r2 = r * r;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d2 = dx * dx + dy * dy;
                if (d2 > r2) continue;
                // La bola sobresale hacia el que mira: de ahí salen su 'hondo' y la luz que le da.
                double dz = Math.sqrt(r2 - d2);
                double luz = (dx * LUZ_X + dy * LUZ_Y + dz * LUZ_Z) / r;
                pixel(x, y, (float) (cz + dz), luz, m);
            }
        }
    }

    private void pixel(int x, int y, float z, double luz, Material m) {
        if (x < 0 || y < 0 || x >= lienzo.ancho || y >= lienzo.alto) return;
        int i = y * lienzo.ancho + x;
        if (z <= hondo[i]) return;
        if (nivelDelAgua != SIN_AGUA && focal == 0 && !pintandoSuelo) {
            // De la fila del píxel y de su 'hondo' se deshace la cámara y sale la altura del punto en la figura.
            double alturaEnLaFigura = (z / escala) * sinI - ((y + 0.5 - origenY) / escala) * cosI;
            if (alturaEnLaFigura < nivelDelAgua) return;
        }
        boolean velo = (m.tonos[0] >>> 24) < 255;
        if (velo && (lienzo.px[i] >>> 24) == 255) {
            // Un velo delante de algo macizo: se mezcla con lo que tapa.
            float parte = (m.tonos[0] >>> 24) / 255f;
            lienzo.px[i] = Colores.mezcla(lienzo.px[i], m.tonos[0] | 0xFF000000, parte);
            return;
        }
        int t;
        if (m.brilla) {
            t = luz > 0.25 ? 0 : 1;
        } else {
            double v = (0.80 - luz - m.brillo) * 2.8 + dibujoDe(m.trama, x, y);
            if (conTrama) v += (TRAMA[y & 3][x & 3] / 16.0 - 0.47) * m.grano;
            t = (int) Math.floor(v + 0.5);
            t = t < m.tonoMinimo ? m.tonoMinimo : (t > 4 ? 4 : t);
            if (m.destello != 0 && luz > LUZ_DEL_DESTELLO) t = -1;
        }
        lienzo.px[i] = t < 0 ? m.destello : m.tonos[t];
        deSuelo[i] = pintandoSuelo;
        trazado[i] = false;
        if (!velo) {
            hondo[i] = z;
            deQue[i] = m;
            tono[i] = (byte) t;
            parteDe[i] = parte;
            if (piezaDe != null) piezaDe[i] = injerto * 100000 + piezaAhora + 1;
        }
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
    }

    /** El dibujo menudo de cada trama: cuánto aclara (negativo) u oscurece (positivo) este píxel. */
    private static double dibujoDe(Material.Trama trama, int x, int y) {
        switch (trama) {
            case MALLA: {
                // Anillas al tresbolillo: una clara, una oscura.
                int k = (x + ((y & 1) << 1)) & 3;
                return k == 0 ? -1.0 : (k == 2 ? 0.7 : 0);
            }
            case PELO: {
                // Mechones: rayitas cortas hacia abajo, cada columna a su altura.
                int n = (x * 7 + (y >> 2) * 13 + (x >> 1) * 5) & 7;
                return n == 0 ? 0.8 : (n == 3 ? -0.6 : 0);
            }
            case ACOLCHADO:
                // Pespuntes en rombo.
                return ((x + y) % 5 == 0 || ((x - y) % 5 + 5) % 5 == 0) ? 0.9 : 0;
            case ESCAMA:
                return (y & 1) == 0 && ((x + (y & 2)) & 3) == 0 ? 1.0 : (((y & 1) == 1 && ((x + (y & 2)) & 3) == 2) ? -0.7 : 0);
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------------
    // El trazo final
    // ------------------------------------------------------------------

    /** El acabado: limpieza, contorno, líneas entre partes, sombras y cantos. Se llama una vez, al acabar la figura. */
    public void terminar() {
        int w = lienzo.ancho;
        int x0 = Math.max(2, minX - 1), x1 = Math.min(w - 3, maxX + 1);
        int y0 = Math.max(2, minY - 1), y1 = Math.min(lienzo.alto - 3, maxY + 1);
        limpiarSueltos(x0, x1, y0, y1);
        // Cuántos píxeles se mira hacia la luz para saber si algo da sombra (más de cerca, más lejos).
        int alcance = Math.max(1, (int) Math.round(escala * 1.2));
        float umbralDeSombra = (float) (SOMBRA_ARROJADA * Math.max(1, escala));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                marca[i] = 0;
                float aqui = hondo[i];
                if (aqui == NADA) {
                    // Hueco (o mancha del suelo) pegado a la figura: contorno.
                    boolean pegado = hondo[i - 1] != NADA || hondo[i + 1] != NADA || hondo[i - w] != NADA || hondo[i + w] != NADA;
                    if (pegado && (lienzo.px[i] == 0 || deSuelo[i])) marca[i] = 1;
                    continue;
                }
                if (deQue[i].brilla) continue;
                // ¿Queda detrás de algo? Entre partes distintas basta un escalón pequeño.
                if (tapa(i, i - 1) || tapa(i, i + 1) || tapa(i, i - w) || tapa(i, i + w)) {
                    marca[i] = 2;
                    continue;
                }
                // ¿Le da sombra algo que sobresale por el lado de la luz (arriba a la izquierda)?
                boolean enSombra = false;
                for (int k = 1; k <= alcance && !enSombra; k++) {
                    int j = i - k * (w + 1);
                    if (j < 0) break;
                    enSombra = hondo[j] != NADA && hondo[j] - aqui > umbralDeSombra + k;
                }
                if (enSombra) marca[i] = 4;
                else if (deQue[i].canto && (borde(i, i - 1) || borde(i, i - w))) marca[i] = 3;
            }
        }
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                Material m = deQue[i];
                switch (marca[i]) {
                    case 1:
                        lienzo.px[i] = colorDelContorno(i, w);
                        esContorno[i] = true;
                        break;
                    case 2:
                        tono[i] = (byte) Math.min(4, Math.max(0, tono[i]) + 2);
                        lienzo.px[i] = Colores.mezcla(m.tonos[tono[i]], m.contorno, 0.45f);
                        break;
                    case 3:
                        if (tono[i] > m.tonoMinimo) tono[i]--;
                        lienzo.px[i] = m.tonos[Math.max(0, tono[i])];
                        break;
                    case 4:
                        tono[i] = (byte) Math.min(4, Math.max(0, tono[i]) + 1);
                        lienzo.px[i] = m.tonos[tono[i]];
                        break;
                    default:
                        break;
                }
            }
        }
        minX = Math.max(0, minX - 1);
        minY = Math.max(0, minY - 1);
        maxX = Math.min(w - 1, maxX + 1);
        maxY = Math.min(lienzo.alto - 1, maxY + 1);
        if (mojado > 0) oscurecerMojado();
        if (nivelDelAgua != SIN_AGUA && focal == 0) pintarLineaDelAgua();
        if (resplandor > 0) pintarResplandor();
        if (resplandorDelArmamento > 0) pintarResplandorDelArmamento();
    }

    // La línea del agua sobre el cuerpo: lo gruesa que es (píxeles de figura), su color de espuma y cuánto tapa (PROVISIONAL).
    private static final double GRUESO_DE_LA_LINEA = 2.2;
    private static final int ESPUMA = 0xFFD8E6E0;
    private static final float FUERZA_DE_LA_ESPUMA = 0.7f;

    /**
     * Donde el agua corta el cuerpo queda una línea clara de espuma, de un
     * píxel: el borde de abajo de lo pintado, si está justo por encima del nivel.
     */
    private void pintarLineaDelAgua() {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int i = y * lienzo.ancho + x;
                if (hondo[i] == NADA || deSuelo[i]) continue;
                if (y + 1 < lienzo.alto && hondo[i + lienzo.ancho] != NADA) continue;       // tiene cuerpo debajo: no es el borde
                double alturaEnLaFigura = (hondo[i] / escala) * sinI - ((y + 0.5 - origenY) / escala) * cosI;
                if (alturaEnLaFigura < nivelDelAgua + GRUESO_DE_LA_LINEA) lienzo.px[i] = Colores.mezcla(lienzo.px[i], ESPUMA, FUERZA_DE_LA_ESPUMA);
            }
        }
    }

    /** La ropa mojada se oscurece: todo lo pintado (menos lo que brilla con luz propia) se apaga un poco. */
    private void oscurecerMojado() {
        float f = 1 - OSCURECE_MOJADO * (float) mojado;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int i = y * lienzo.ancho + x;
                if ((lienzo.px[i] >>> 24) == 0 || (hondo[i] != NADA && deQue[i] != null && deQue[i].brilla)) continue;
                lienzo.px[i] = Colores.brillo(lienzo.px[i], f);
            }
        }
    }

    /** ¿El píxel 'otro' tapa al píxel 'i'? (Está delante por un escalón, o es de otra parte y está algo delante.) */
    private boolean tapa(int i, int otro) {
        if (hondo[otro] == NADA) return false;
        float salto = hondo[otro] - hondo[i];
        return salto > ESCALON || (salto > ENTRE_PARTES && parteDe[otro] != parteDe[i]);
    }

    /** ¿El píxel 'i' acaba ahí, mirando hacia 'otro'? (Fuera no hay nada, o lo que hay queda detrás.) */
    private boolean borde(int i, int otro) {
        return hondo[otro] == NADA || hondo[i] - hondo[otro] > ENTRE_PARTES;
    }

    /**
     * El contorno toma el color oscuro del material que rodea, no negro puro:
     * así la línea pertenece a la figura. Por el lado de la luz es algo más claro.
     */
    private int colorDelContorno(int i, int w) {
        // Lo que tiene debajo o a la derecha le da la luz de cara: contorno más claro.
        Material deLuz = deQue[i + 1] != null ? deQue[i + 1] : deQue[i + w];
        if (deLuz != null && hondo[i - 1] == NADA && hondo[i - w] == NADA) {
            return deLuz.brilla ? CONTORNO : Colores.mezcla(deLuz.tonos[4] | 0xFF000000, CONTORNO, 0.45f);
        }
        Material m = deQue[i - 1] != null ? deQue[i - 1] : deQue[i - w] != null ? deQue[i - w] : deLuz;
        return m == null || m.brilla ? CONTORNO : m.contorno;
    }

    /**
     * Quita los píxeles sueltos: el que no comparte tono con ninguno de sus
     * cuatro vecinos del mismo material toma el de ellos. Los materiales con
     * mucho grano o con dibujo (malla, pelo) se dejan como están.
     */
    private void limpiarSueltos(int x0, int x1, int y0, int y1) {
        int w = lienzo.ancho;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                Material m = deQue[i];
                if (m == null || m.brilla || trazado[i] || m.grano > 0.6 || m.trama != Material.Trama.LISA) continue;
                int a = i - 1, b = i + 1, c = i - w, d = i + w;
                if (deQue[a] != m || deQue[b] != m || deQue[c] != m || deQue[d] != m) continue;
                byte t = tono[i];
                if (tono[a] == t || tono[b] == t || tono[c] == t || tono[d] == t) continue;
                // Toma el tono que más se repite a su alrededor.
                byte nuevo = tono[a] == tono[b] || tono[a] == tono[c] || tono[a] == tono[d] ? tono[a] : tono[b] == tono[c] || tono[b] == tono[d] ? tono[b] : tono[c];
                tono[i] = nuevo;
                lienzo.px[i] = nuevo < 0 ? m.destello : m.tonos[nuevo];
            }
        }
    }

    /** El resplandor: aclara la figura (su contorno, menos) y deja un halo que se transparenta a su alrededor. */
    private void pintarResplandor() {
        int w = lienzo.ancho, r = RADIO_DEL_HALO;
        int x0 = Math.max(r, minX - r), x1 = Math.min(w - 1 - r, maxX + r);
        int y0 = Math.max(r, minY - r), y1 = Math.min(lienzo.alto - 1 - r, maxY + r);
        int halo = (colorDelResplandor & 0xFFFFFF) | ((int) (150 * resplandor) << 24);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                marca[i] = 0;
                if (hondo[i] != NADA) {
                    marca[i] = 1;                                   // figura
                } else if (esContorno[i]) {
                    marca[i] = 2;                                   // su contorno
                } else {
                    // Suelo o hueco: es halo si tiene figura a menos de dos píxeles.
                    for (int dy = -r; dy <= r && marca[i] == 0; dy++) {
                        for (int dx = -r; dx <= r; dx++) {
                            if (dx * dx + dy * dy <= r * r && hondo[i + dy * w + dx] != NADA) {
                                marca[i] = 3;
                                break;
                            }
                        }
                    }
                }
            }
        }
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * w + x;
                if (marca[i] == 1) lienzo.px[i] = Colores.mezcla(lienzo.px[i], colorDelResplandor, (float) (0.75 * resplandor));
                else if (marca[i] == 2) lienzo.px[i] = Colores.mezcla(lienzo.px[i], colorDelResplandor, (float) (0.45 * resplandor));
                else if (marca[i] == 3) lienzo.px[i] = halo;
            }
        }
        minX = Math.max(0, minX - r);
        minY = Math.max(0, minY - r);
        maxX = Math.min(w - 1, maxX + r);
        maxY = Math.min(lienzo.alto - 1, maxY + r);
    }
}
