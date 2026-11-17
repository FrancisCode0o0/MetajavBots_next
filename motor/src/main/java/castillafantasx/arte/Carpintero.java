package castillafantasx.arte;

import castillafantasx.figura.Escultor;
import castillafantasx.figura.Material;
import castillafantasx.figura.P3;

/**
 * CARPINTERO — Hace muebles y objetos con el Escultor, para verlos desde
 * arriba como todo lo de la superficie (encargo de los interiores).
 *
 * Trabaja con tres medidas llanas: 'x' (a la derecha), 'f' (el fondo: 0 es el
 * canto de delante del mueble, y hacia atrás es negativo) y 'z' (la altura
 * sobre el suelo). Una unidad es un píxel de ancho; una persona mide unas 24
 * de alto y una casilla, 32 de ancho. El pie del dibujo (por donde se apoya en
 * el mapa) es el centro de su canto de delante.
 */
public final class Carpintero {

    /** El Escultor con el que trabaja: quien necesite una pieza suya (un paño, una rodela) la pide aquí. */
    public final Escultor e;
    private final int pieX, pieY;
    private boolean deFrente;
    private static final int MARGEN = 3;        // píxeles que se dejan alrededor de lo pintado (el contorno y el resplandor)
    private static final double VUELTA = 2 * Math.PI;   // una vuelta entera, en radianes

    public Carpintero(int ancho, int alto) {
        e = new Escultor(ancho, alto);
        pieX = ancho / 2;
        pieY = alto - 8;
    }

    /** Empieza un dibujo nuevo, con el lienzo limpio. */
    public void empezar() {
        deFrente = false;
        e.mirarDesdeArriba(0, 1, pieX, pieY);
    }

    /** Empieza un dibujo de los que se ven de frente, en una mazmorra: 'escala' píxeles por unidad. */
    public void empezarDeFrente(double escala) {
        deFrente = true;
        e.mirarInclinado(0, Escultor.INCLINACION_DE_FRENTE, escala, pieX, pieY);
    }

    /** ¿Se está dibujando de frente (para una mazmorra)? Por si algo debe hacerse distinto. */
    public boolean deFrente() {
        return deFrente;
    }

    /** El punto (x a la derecha, f al fondo —negativo, hacia atrás—, z arriba) en las medidas del Escultor. */
    public static P3 p(double x, double f, double z) {
        return new P3(f, x, z);
    }

    /** Una caja maciza, de (x0, f0, z0) a (x1, f1, z1): se le ven la tapa y el frente. */
    public void caja(double x0, double f0, double z0, double x1, double f1, double z1, Material m) {
        double atras = Math.min(f0, f1), delante = Math.max(f0, f1);
        e.lamina(p(x0, atras, z1), p(x1, atras, z1), p(x1, delante, z1), p(x0, delante, z1), m);
        e.lamina(p(x0, delante, z1), p(x1, delante, z1), p(x1, delante, z0), p(x0, delante, z0), m);
    }

    /** Una tabla tumbada (un tablero, una balda): una caja de poco grueso con su cara de arriba a la altura 'z'. */
    public void tablero(double x0, double f0, double x1, double f1, double z, double grueso, Material m) {
        caja(x0, f0, z - grueso, x1, f1, z, m);
    }

    /** Una tabla de pie, de cara al que mira (una puerta de armario, un respaldo, un cuadro): en el fondo 'f'. */
    public void panel(double x0, double z0, double x1, double z1, double f, Material m) {
        e.lamina(p(x0, f, z1), p(x1, f, z1), p(x1, f, z0), p(x0, f, z0), m);
    }

    /** Una tabla tendida en el suelo o sobre algo (una estera, un mantel, una losa): solo su cara de arriba. */
    public void tendido(double x0, double f0, double x1, double f1, double z, Material m) {
        e.lamina(p(x0, f0, z), p(x1, f0, z), p(x1, f1, z), p(x0, f1, z), m);
    }

    /** Un palo entre dos puntos (una pata, un travesaño, una viga, una cadena). */
    public void palo(double x0, double f0, double z0, double x1, double f1, double z1, double radio, Material m) {
        e.hueso(p(x0, f0, z0), p(x1, f1, z1), radio, m);
    }

    /** Un palo que se estrecha o se ensancha de un extremo al otro (un cirio, un cuerno, una pata torneada). */
    public void palo(double x0, double f0, double z0, double x1, double f1, double z1, double radio0, double radio1, Material m) {
        e.hueso(p(x0, f0, z0), p(x1, f1, z1), radio0, radio1, m);
    }

    /** Las cuatro patas de una mesa o de un banco, en las esquinas de ese rectángulo, de 'z0' a 'z1'. */
    public void patas(double x0, double f0, double x1, double f1, double z0, double z1, double radio, Material m) {
        palo(x0, f0, z0, x0, f0, z1, radio, m);
        palo(x1, f0, z0, x1, f0, z1, radio, m);
        palo(x0, f1, z0, x0, f1, z1, radio, m);
        palo(x1, f1, z0, x1, f1, z1, radio, m);
    }

    public void bola(double x, double f, double z, double radio, Material m) {
        e.bola(p(x, f, z), radio, m);
    }

    /**
     * Una pieza de torno, de pie (un cántaro, una olla, una redoma, una columna):
     * a cada altura de 'alturas' (desde 'z'), el radio que dice 'radios'.
     */
    public void torno(double x, double f, double z, double[] alturas, double[] radios, Material m) {
        for (int i = 0; i + 1 < alturas.length; i++) {
            e.hueso(p(x, f, z + alturas[i]), p(x, f, z + alturas[i + 1]), radios[i], radios[i + 1], m);
        }
    }

    /** Un disco tumbado (un plato, la boca de un caldero, la tapa de un tonel, un círculo en el suelo). */
    public void disco(double x, double f, double z, double radio, Material m) {
        e.rodela(p(x, f, z), new P3(radio, 0, 0), new P3(0, radio, 0), m);
    }

    /** Un disco de pie, de cara al que mira (un espejo, un rosetón, la boca de un horno). */
    public void discoDePie(double x, double f, double z, double radioX, double radioZ, Material m) {
        e.rodela(p(x, f, z), new P3(0, radioX, 0), new P3(0, 0, radioZ), m);
    }

    /** Un aro tumbado (el cerco de un tonel, el borde de un brocal), hecho de bolitas. */
    public void aro(double x, double f, double z, double radio, double grueso, Material m) {
        int trozos = Math.max(10, (int) (radio * 5));
        for (int i = 0; i < trozos; i++) {
            double a = i * 2 * Math.PI / trozos;
            bola(x + Math.cos(a) * radio, f + Math.sin(a) * radio, z, grueso, m);
        }
    }

    /** Una cara plana de cuatro esquinas cualesquiera (x, f, z de cada una): para lo inclinado. */
    public void cara(double x0, double f0, double z0, double x1, double f1, double z1,
                     double x2, double f2, double z2, double x3, double f3, double z3, Material m) {
        e.lamina(p(x0, f0, z0), p(x1, f1, z1), p(x2, f2, z2), p(x3, f3, z3), m);
    }

    /** Una tabla en cuesta, de cara al que mira (un atril, un tejadillo, una tapa alzada): de ancho x0..x1, sube de (f0, z0) a (f1, z1). */
    public void cuesta(double x0, double x1, double f0, double z0, double f1, double z1, Material m) {
        cara(x0, f1, z1, x1, f1, z1, x1, f0, z0, x0, f0, z0, m);
    }

    /** Un óvalo tumbado (una piel, un charco, un lecho de ceniza): 'radioX' a lo ancho y 'radioF' hacia el fondo. */
    public void ovalo(double x, double f, double z, double radioX, double radioF, Material m) {
        e.rodela(p(x, f, z), new P3(0, radioX, 0), new P3(radioF, 0, 0), m);
    }

    /**
     * Un tronco de cono de pie, hecho de 14 caras (un brocal, una taza, un tambor): del radio r0, abajo,
     * al r1, arriba. A diferencia del torno, acaba plano: se le puede poner encima su boca o su tapa.
     */
    public void tronco(double x, double f, double z0, double r0, double z1, double r1, Material m) {
        tronco(x, f, z0, r0, z1, r1, 14, m);
    }

    /** Lo mismo, con las caras que se pidan (más, más redondo). */
    public void tronco(double x, double f, double z0, double r0, double z1, double r1, int caras, Material m) {
        for (int i = 0; i < caras; i++) {
            double a = i * 2 * Math.PI / caras, b = (i + 1) * 2 * Math.PI / caras;
            e.lamina(p(x + r1 * Math.cos(a), f + r1 * Math.sin(a), z1), p(x + r1 * Math.cos(b), f + r1 * Math.sin(b), z1),
                    p(x + r0 * Math.cos(b), f + r0 * Math.sin(b), z0), p(x + r0 * Math.cos(a), f + r0 * Math.sin(a), z0), m);
        }
    }

    /** Un tambor: un cilindro (o un cono cortado) de pie, con su tapa; solo se le hace la media vuelta que se ve. */
    public void tambor(double x, double f, double z0, double r0, double z1, double r1, Material m) {
        int caras = 8;
        for (int i = 0; i < caras; i++) {
            double a = -Math.PI / 2 + i * Math.PI / caras, b = a + Math.PI / caras;
            cara(x + r1 * Math.sin(a), f + r1 * Math.cos(a), z1, x + r1 * Math.sin(b), f + r1 * Math.cos(b), z1,
                    x + r0 * Math.sin(b), f + r0 * Math.cos(b), z0, x + r0 * Math.sin(a), f + r0 * Math.cos(a), z0, m);
        }
        disco(x, f, z1, r1, m);
    }

    /**
     * Una pieza redonda de pie, de paredes lisas (una tinaja, un barril, una tina): a cada altura
     * de 'alturas', su radio. Si 'tapa' no es null, se cierra por arriba con un disco de ese material.
     */
    public void vaso(double x, double f, double z, double[] alturas, double[] radios, Material m, Material tapa) {
        int ultimo = alturas.length - 1;
        for (int i = 0; i < ultimo; i++) tronco(x, f, z + alturas[i], radios[i], z + alturas[i + 1], radios[i + 1], 12, m);
        if (tapa != null) disco(x, f, z + alturas[ultimo], radios[ultimo], tapa);
    }

    /** Un vaso abierto: con su borde y, dentro, lo que se ve por la boca (lo hueco, agua, grano). */
    public void vasoAbierto(double x, double f, double z, double[] alturas, double[] radios, Material m, Material dentro) {
        int ultimo = alturas.length - 1;
        vaso(x, f, z, alturas, radios, m, m);
        disco(x, f, z + alturas[ultimo] + 0.2, Math.max(0.8, radios[ultimo] - 0.9), dentro);
    }

    /**
     * Una vasija de torno hueca, de paño fino y abierta por arriba (un caldero, un mortero,
     * un cuenco): a cada altura (desde 'z'), su radio. Se le ve el interior.
     */
    public void vasijaHueca(double x, double f, double z, double[] alturas, double[] radios, Material m) {
        int gajos = 14;
        P3[][] rejilla = new P3[alturas.length][gajos + 1];
        for (int i = 0; i < alturas.length; i++) {
            int fila = alturas.length - 1 - i;                 // las filas del paño van de arriba abajo
            for (int g = 0; g <= gajos; g++) {
                double a = g * VUELTA / gajos;
                rejilla[i][g] = p(x + Math.cos(a) * radios[fila], f + Math.sin(a) * radios[fila], z + alturas[fila]);
            }
        }
        e.pano(rejilla, m, true);
    }

    /**
     * Un trozo de aro de pie, de cara al que mira (un asa, un arco, la hoja de una hoz), hecho de palos:
     * ángulos en grados, 0 a la derecha y 90 arriba. Los trozos, según lo largo del arco (uno cada 12°, 6 al menos).
     */
    public void arco(double x, double f, double z, double radioX, double radioZ, double desde, double hasta, double grueso, Material m) {
        arco(x, f, z, radioX, radioZ, desde, hasta, grueso, Math.max(6, (int) (Math.abs(hasta - desde) / 12)), m);
    }

    /** Lo mismo, en tantos trozos como se pidan. */
    public void arco(double x, double f, double z, double radioX, double radioZ, double desde, double hasta, double grueso, int trozos, Material m) {
        for (int i = 0; i < trozos; i++) {
            double a = Math.toRadians(desde + (hasta - desde) * i / trozos), b = Math.toRadians(desde + (hasta - desde) * (i + 1) / trozos);
            palo(x + Math.cos(a) * radioX, f, z + Math.sin(a) * radioZ, x + Math.cos(b) * radioX, f, z + Math.sin(b) * radioZ, grueso, m);
        }
    }

    /** Un aro de pie, de cara al que mira, hecho de bolitas: 'vuelta' = 1, entero; 0.5, solo el arco de arriba. */
    public void aroDePie(double x, double f, double z, double radioX, double radioZ, double vuelta, double grueso, Material m) {
        int trozos = (int) (Math.max(radioX, radioZ) * 8);
        for (int i = 0; i <= trozos; i++) {
            double a = i * 2 * Math.PI * vuelta / trozos;
            bola(x + Math.cos(a) * radioX, f, z + Math.sin(a) * radioZ, grueso, m);
        }
    }

    /** Una raya oscura sobre lo ya pintado (una junta entre tablas, una grieta) y una raya de luz (un canto, un herraje fino). */
    public void surco(double x0, double f0, double z0, double x1, double f1, double z1) {
        e.surco(p(x0, f0, z0), p(x1, f1, z1));
    }

    public void realce(double x0, double f0, double z0, double x1, double f1, double z1) {
        e.realce(p(x0, f0, z0), p(x1, f1, z1));
    }

    /** Una llama corriente (la de una vela es g = 0.35; la de un hogar, 1.6), en el instante 'tiempo'. */
    public void llama(double x, double f, double z, double g, double tiempo) {
        if (tiempo < 0) return;         // apagado
        castillafantasx.figura.FormasDeLumbre.llama(e, p(x, f, z), tiempo, g, P3.CERO);
    }

    /**
     * Una llama de otro color (el fuego verde de un caldero, el morado de un
     * portal): lenguas que suben y se retuercen con el 'tiempo'. 'colores':
     * del borde al corazón de la llama (RRGGBB).
     */
    public void llamaDeColor(double x, double f, double z, double g, double tiempo, int... colores) {
        if (tiempo < 0) return;         // apagado
        for (int capa = 0; capa < colores.length; capa++) {
            Material m = Material.luz(colores[capa]);
            double parte = 1 - capa / (double) colores.length;       // las capas de dentro, más cortas y finas
            for (int lengua = 0; lengua < 3; lengua++) {
                double fase = tiempo * 7 + lengua * 2.1 + capa * 0.7;
                double lado = (lengua - 1) * 1.1 * g * parte + Math.sin(fase) * 0.35 * g;
                double alto = (3.2 + 1.3 * Math.sin(fase * 1.3 + 1)) * g * parte * (lengua == 1 ? 1.25 : 0.85);
                e.hueso(p(x + lado, f + 0.05 * capa, z), p(x + lado * 0.4 + Math.sin(fase * 0.8) * 0.5 * g, f + 0.05 * capa, z + alto), 0.9 * g * parte, 0.12 * g, m);
            }
        }
    }

    /** Un velo de color que se transparenta (humo, vapor, niebla, un resplandor): una bola blanda. 'alfa', de 0 a 255. */
    public void velo(double x, double f, double z, double radio, int color, int alfa) {
        e.bola(p(x, f, z), radio, Material.velo(color, alfa));
    }

    /** El dibujo entero resplandece de ese color (una reliquia, algo que no es de este mundo). */
    public void resplandor(int color, double fuerza) {
        e.resplandecer(color, fuerza);
    }

    /** Acaba un dibujo de los que van tendidos en el suelo o colgados encima (una alfombra, un círculo, un cuadro): recortado a lo justo, centrado. */
    public Imagen terminarCentrado() {
        e.terminar();
        Imagen lienzo = e.lienzo;
        int izq = lienzo.ancho, der = -1, arriba = lienzo.alto, abajo = -1;
        for (int y = 0; y < lienzo.alto; y++) {
            for (int x = 0; x < lienzo.ancho; x++) {
                if ((lienzo.px[y * lienzo.ancho + x] >>> 24) == 0) continue;
                izq = Math.min(izq, x);
                der = Math.max(der, x);
                arriba = Math.min(arriba, y);
                abajo = Math.max(abajo, y);
            }
        }
        if (der < 0) return new Imagen(2, 2);
        return lienzo.recorte(izq, arriba, der - izq + 1, abajo - arriba + 1);
    }

    /**
     * Acaba el dibujo y lo devuelve recortado: el pie queda en el centro de
     * su borde de abajo, como espera el mapa de todo lo que está de pie.
     */
    public Imagen terminar() {
        e.terminar();
        Imagen lienzo = e.lienzo;
        int izq = lienzo.ancho, der = -1, arriba = lienzo.alto, abajo = -1;
        for (int y = 0; y < lienzo.alto; y++) {
            for (int x = 0; x < lienzo.ancho; x++) {
                if ((lienzo.px[y * lienzo.ancho + x] >>> 24) == 0) continue;
                izq = Math.min(izq, x);
                der = Math.max(der, x);
                arriba = Math.min(arriba, y);
                abajo = Math.max(abajo, y);
            }
        }
        if (der < 0) return new Imagen(2, 2);
        // A los lados, lo mismo a cada lado del pie; abajo, hasta el pie.
        int medio = Math.max(pieX - izq, der - pieX) + MARGEN;
        int fin = pieY + 2;
        int x0 = Math.max(0, pieX - medio), y0 = Math.max(0, arriba - MARGEN);
        return lienzo.recorte(x0, y0, Math.min(lienzo.ancho - x0, 2 * medio), fin - y0);
    }
}
