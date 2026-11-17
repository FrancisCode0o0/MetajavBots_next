package castillafantasx.figura;



/**
 * FORMAS DE LUMBRE — El modelo de las dos lumbres del juego
 * (docs/encargos_viejos/ENCARGO_DIA_NOCHE.md y docs/encargos_viejos/ENCARGO_BOSQUE.md), para el Escultor: el
 * farol de mano y la antorcha. Son las mismas en la mano del héroe, en
 * la de la gente que anda de noche, en primera persona, en el suelo y en los
 * iconos. Las fuentes de los dos modelos están en docs/CREDITOS.md.
 *
 * El FAROL es una linterna redonda de hoja de hierro, como las del siglo XV
 * que se conservan: cuerpo de tambor, capacete con respiraderos, anilla para
 * llevarla, ventanas de asta (cuerno raspado hasta dejarlo traslúcido) y una
 * vela dentro. La llama va guardada del viento y por eso alumbra poco.
 *
 * La ANTORCHA es un hacha de viento: un palo con una cabeza de sogas de
 * esparto retorcidas, atadas y empapadas en pez. Alumbra mucho, aguanta el
 * viento, humea y se consume.
 */
public final class FormasDeLumbre {

    private FormasDeLumbre() { }

    // El farol: la hoja de hierro, el asta apagada y encendida, y la vela que se adivina detrás.
    private static final Material LATA = Material.metal(0x6E6A60), LATA_OSCURA = Material.metal(0x3E3A36), LATON_VIEJO = Material.metal(0x8A7038);
    private static final Material ASTA_APAGADA = Material.mate(0x7A6A48), ASTA_ENCENDIDA = Material.luz(0xC97A22);
    private static final Material CLARIDAD_TRAS_EL_ASTA = Material.luz(0xF0A83A), LLAMA_TRAS_EL_ASTA = Material.luz(0xFFE9A0), VELA_TRAS_EL_ASTA = Material.luz(0xE2B46A);
    private static final Material RESPIRADERO_ENCENDIDO = Material.luz(0xE08A2A);
    private static final Material LUZ_DEL_FAROL = Material.velo(0xF4A62A, 30), LUZ_JUNTO_AL_FAROL = Material.velo(0xF8B848, 34);

    // La antorcha: el esparto, la pez que lo empapa, la cuerda que lo ata y la brasa.
    private static final Material ESPARTO = Material.mate(0x8E7A44), PEZ = Material.cuero(0x241C18), CUERDA = Material.mate(0xA8946A), BRASA = Material.luz(0xB83A14);

    // La llama, de fuera adentro: roja, naranja, amarilla y casi blanca en el corazón.
    private static final Material[] TONOS_DE_LLAMA = { Material.luz(0xC8421A), Material.LLAMA, Material.luz(0xFFD04E), Material.luz(0xFFF3C4) };
    // Las chispas (recién saltada y ya enfriándose) y el humo, cada vez más tenue. Son velos: no llevan contorno.
    private static final Material CHISPA_VIVA = Material.velo(0xFFE08A, 254), CHISPA_ROJA = Material.velo(0xE0621E, 254);
    private static final Material[] HUMO = { Material.velo(0x6A4A38, 86), Material.velo(0x4E443E, 60), Material.velo(0x443E3C, 34) };
    private static final Material HALO = Material.velo(0xF4A62A, 30), HALO_JUNTO_A_LA_LLAMA = Material.velo(0xF8B848, 38);

    private static final P3 ABAJO = new P3(0, 0, -1), ARRIBA = new P3(0, 0, 1);
    /** Lo que mide la antorcha de la mano a la punta de la llama, y el farol del asa a la base (en unidades de la figura). */
    public static final double LARGO_DE_LA_ANTORCHA = 12.5, ALTO_DEL_FAROL = 5.4;
    /** Con menos píxeles que estos por unidad, el modelo se resume: sin lo menudo, que solo ensuciaría. */
    private static final double DE_CERCA = 1.8;

    // Las medidas del farol, del asa hacia abajo (unidades de la figura, a tamaño 1).
    private static final double ANILLA = 0.9, ALERO = 2.0, FIN_DE_LA_CAJA = 4.7;


    // Las medidas de la antorcha, de la mano hacia la punta.
    private static final double FIN_DEL_PALO = 6.0, CABEZA = 5.6, EMPAPADA = 6.7, PUNTA = 8.3;
    // TODO-FRAN: cuántas chispas y cuánto humo echa la antorcha (PROVISIONAL).
    private static final int CHISPAS = 6, VOLUTAS_DE_HUMO = 4, LLAMITAS_SUELTAS = 2;
    private static final double ALTO_DE_LA_LLAMA = 4.4;

    // ------------------------------------------------------------------
    // El farol
    // ------------------------------------------------------------------

    // El farol de mano del XV (docs/CREDITOS.md, «Fuentes de la lumbre»): una caja de chapa de hierro de cuatro caras,
    // más alta que ancha (la de Múnich mide 18 × 8 × 10 cm), con ventanas de asta raspada, tejadillo y aro para llevarlo.
    private static final double MEDIO_FAROL = 1.15;         // la mitad del ancho de la caja
    private static final double GIRO_DEL_FAROL = 0.5;       // radianes que va vuelta la caja: así se le ven dos caras

    /**
     * El farol, colgando de su aro. 'asa' es donde lo coge la mano y 'g' su
     * tamaño (1 = el de una persona de cuerpo entero).
     */
    public static void farol(Escultor e, P3 asa, boolean encendido, double tiempo, double g) {
        boolean fino = e.escala() * g >= DE_CERCA;
        P3 delante = e.haciaElQueMira(), derecha = e.aLaDerecha();
        double tiembla = encendido ? parpadeo(tiempo, 0.7) : 0;
        P3 eje = asa;

        // El aro de llevarlo y el tejadillo en punta, con sus respiraderos.
        if (fino) aro(e, bajar(eje, 0.45 * g), derecha, ARRIBA, 0.45 * g, 0.12 * g, LATA_OSCURA);
        else e.hueso(eje, bajar(eje, ANILLA * g), 0.2 * g, LATA_OSCURA);
        P3 arriba = bajar(eje, ALERO * g), abajo = bajar(eje, FIN_DE_LA_CAJA * g);
        P3[] esquina = new P3[4];
        for (int k = 0; k < 4; k++) {
            esquina[k] = alrededor(GIRO_DEL_FAROL + Math.PI / 4 + k * Math.PI / 2).por(MEDIO_FAROL * Math.sqrt(2) * g);
        }
        // El tejadillo: cuatro aguas de chapa que vuelan un poco sobre la caja, con un respiradero en cada una.
        P3 punta = bajar(eje, ANILLA * g);
        e.hueso(eje, punta, 0.16 * g, LATA_OSCURA);
        for (int k = 0; k < 4; k++) {
            P3 a = arriba.mas(esquina[k].por(1.3)), b = arriba.mas(esquina[(k + 1) % 4].por(1.3));
            e.lamina(punta.mas(esquina[k].por(0.12)), punta.mas(esquina[(k + 1) % 4].por(0.12)), b, a, k % 2 == 0 ? LATA : LATA_OSCURA);
            if (fino) e.bola(punta.hacia(a.hacia(b, 0.5), 0.6), 0.14 * g, encendido ? RESPIRADERO_ENCENDIDO : LATA_OSCURA);
        }

        // La caja: cuatro caras de asta entre cuatro montantes de hierro. Encendida, el asta se ve de color de miel.
        for (int k = 0; k < 4; k++) {
            P3 a = esquina[k], b = esquina[(k + 1) % 4];
            e.lamina(arriba.mas(a), arriba.mas(b), abajo.mas(b), abajo.mas(a), encendido ? ASTA_ENCENDIDA : ASTA_APAGADA);
            if (encendido) {
                // El corazón de cada ventana, más claro: ahí está la vela.
                P3 a2 = a.hacia(b, 0.25).por(1.02), b2 = a.hacia(b, 0.75).por(1.02);
                e.lamina(bajar(arriba, 0.5 * g).mas(a2), bajar(arriba, 0.5 * g).mas(b2), bajar(abajo, -0.35 * g).mas(b2), bajar(abajo, -0.35 * g).mas(a2), CLARIDAD_TRAS_EL_ASTA);
            }
        }
        if (encendido) {
            // La llama de la vela, que se adivina tras el asta y sube y baja.
            double sube = (0.25 + 0.2 * tiembla) * g, cerca = MEDIO_FAROL * 1.5 * g;
            e.hueso(bajar(eje, 3.0 * g - sube).mas(delante.por(cerca)), bajar(eje, 4.0 * g).mas(delante.por(cerca)), 0.12 * g, 0.42 * g, LLAMA_TRAS_EL_ASTA);
            if (fino) e.hueso(bajar(eje, 4.0 * g).mas(delante.por(cerca)), bajar(eje, FIN_DE_LA_CAJA * g).mas(delante.por(cerca)), 0.26 * g, VELA_TRAS_EL_ASTA);
        }
        // Los montantes de las esquinas y, de cerca, el travesaño que parte cada ventana y el pestillo de la puerta.
        for (int k = 0; k < 4; k++) {
            P3 a = esquina[k].por(1.03), b = esquina[(k + 1) % 4].por(1.03);
            e.hueso(arriba.mas(a), abajo.mas(a), 0.17 * g, LATA_OSCURA);
            if (fino) e.hueso(bajar(eje, 3.3 * g).mas(a), bajar(eje, 3.3 * g).mas(b), 0.08 * g, LATA_OSCURA);
        }
        if (fino) e.bola(bajar(eje, 3.3 * g).mas(esquina[0].hacia(esquina[1], 0.85).por(1.08)), 0.18 * g, LATON_VIEJO);
        // La base, con su reborde y sus patas.
        P3 suelo = bajar(eje, 5.05 * g);
        for (int k = 0; k < 4; k++) {
            P3 a = esquina[k].por(1.15), b = esquina[(k + 1) % 4].por(1.15);
            e.lamina(abajo.mas(a), abajo.mas(b), suelo.mas(b), suelo.mas(a), LATA_OSCURA);
            if (fino) e.bola(suelo.mas(a).mas(0, 0, -0.15 * g), 0.2 * g, LATA_OSCURA);
        }
        // La claridad que deja pasar el asta: poca, y tiembla.
        if (encendido) {
            e.bola(bajar(eje, 3.5 * g), (2.9 + 0.2 * tiembla) * g, LUZ_DEL_FAROL);
            e.bola(bajar(eje, 3.5 * g), (2.0 + 0.15 * tiembla) * g, LUZ_JUNTO_AL_FAROL);
        }
    }

    private static P3 bajar(P3 p, double cuanto) {
        return p.mas(ABAJO.por(cuanto));
    }

    /** Un punto de la circunferencia de radio 1 tumbada en el suelo, a ese ángulo. */
    private static P3 alrededor(double angulo) {
        return new P3(Math.cos(angulo), Math.sin(angulo), 0);
    }

    /** Un aro de alambre: centro, sus dos ejes, su radio y su grueso. */
    private static void aro(Escultor e, P3 centro, P3 ejeU, P3 ejeV, double radio, double grueso, Material m) {
        int tramos = 8;
        P3 antes = centro.mas(ejeU.por(radio));
        for (int k = 1; k <= tramos; k++) {
            double a = k * 2 * Math.PI / tramos;
            P3 ahora = centro.mas(ejeU.por(radio * Math.cos(a))).mas(ejeV.por(radio * Math.sin(a)));
            e.hueso(antes, ahora, grueso, m);
            antes = ahora;
        }
    }

    // ------------------------------------------------------------------
    // La antorcha
    // ------------------------------------------------------------------

    /**
     * La antorcha, empuñada en 'mano' y apuntando hacia 'dir'. Encendida,
     * lleva su llama, sus chispas, su humo y su halo; apagada, la cabeza negra de pez.
     */
    public static void antorcha(Escultor e, P3 mano, P3 dir, boolean encendida, double tiempo, double g) {
        antorcha(e, mano, dir, encendida, tiempo, g, P3.CERO);
    }

    /**
     * Como la anterior, con el aire que le da a la llama: 'aire' es hacia
     * dónde la empuja y con cuánta fuerza (quien corre la lleva tendida hacia atrás).
     */
    public static void antorcha(Escultor e, P3 mano, P3 dir, boolean encendida, double tiempo, double g, P3 aire) {
        boolean fino = e.escala() * g >= DE_CERCA;
        P3 d = dir.unidad();
        // El palo: una rama descortezada, más gruesa hacia la cabeza, con sus nudos.
        e.hueso(mano.menos(d.por(2.2 * g)), mano.mas(d.por(FIN_DEL_PALO * g)), 0.42 * g, 0.52 * g, Material.MADERA);
        if (fino) {
            e.bola(mano.mas(d.por(2.6 * g)), 0.55 * g, Material.MADERA);
            e.bola(mano.mas(d.por(4.7 * g)), 0.6 * g, Material.MADERA);
        }
        // La cabeza: sogas de esparto retorcidas; abajo se ve el esparto y arriba, negro de pez.
        P3 cabeza = mano.mas(d.por(CABEZA * g)), empapada = mano.mas(d.por(EMPAPADA * g)), punta = mano.mas(d.por(PUNTA * g));
        e.hueso(cabeza, empapada, 0.72 * g, 0.95 * g, ESPARTO);
        e.hueso(empapada, punta, 0.98 * g, 1.08 * g, PEZ);
        if (fino) {
            retorcido(e, cabeza, punta, d, 0.72 * g, 1.08 * g);
            // Las ataduras de cuerda, y la pez que ha chorreado palo abajo.
            atadura(e, mano.mas(d.por(5.85 * g)), d, 0.86 * g, g);
            atadura(e, mano.mas(d.por(6.75 * g)), d, 1.1 * g, g);
            e.hueso(cabeza, mano.mas(d.por(4.4 * g)).mas(e.aLaDerecha().por(0.3 * g)), 0.3 * g, 0.12 * g, PEZ);
        }
        if (!encendida) return;
        e.bola(punta, 0.95 * g, BRASA);
        llama(e, punta, tiempo, g, aire);
    }

    /** Las vueltas de las sogas: surcos en espiral por la cabeza de la antorcha. */
    private static void retorcido(Escultor e, P3 a, P3 b, P3 eje, double ra, double rb) {
        P3 u = eje.cruz(Math.abs(eje.z) < 0.9 ? ARRIBA : new P3(1, 0, 0)).unidad(), w = eje.cruz(u);
        int tramos = 14, vueltas = 2, sogas = 3;
        for (int s = 0; s < sogas; s++) {
            P3 antes = null;
            for (int k = 0; k <= tramos; k++) {
                double t = k / (double) tramos, a0 = t * vueltas * 2 * Math.PI + s * 2 * Math.PI / sogas;
                double radio = (ra + (rb - ra) * t) * 1.02;
                P3 ahora = a.hacia(b, t).mas(u.por(Math.cos(a0) * radio)).mas(w.por(Math.sin(a0) * radio));
                if (antes != null) e.surco(antes, ahora);
                antes = ahora;
            }
        }
    }

    private static void atadura(Escultor e, P3 centro, P3 eje, double radio, double g) {
        e.hueso(centro.menos(eje.por(0.16 * g)), centro.mas(eje.por(0.16 * g)), radio, CUERDA);
    }

    // ------------------------------------------------------------------
    // La llama
    // ------------------------------------------------------------------

    /**
     * Cuánto se mueve una llama en ese instante, de -1 a 1: varias ondas de
     * ritmos que no casan, para que nunca se repita a la vista. 'semilla'
     * hace que cada lengua vaya a su aire.
     */
    static double parpadeo(double tiempo, double semilla) {
        return Math.sin(tiempo * 11 + semilla * 5) * 0.45 + Math.sin(tiempo * 17.3 + semilla * 9) * 0.3 + Math.sin(tiempo * 29 + semilla * 3) * 0.25;
    }

    /**
     * La llama viva que sale de 'base': lenguas rojas por fuera, naranjas,
     * amarillas y el corazón casi blanco; llamitas que se sueltan por la
     * punta, chispas que saltan y humo que sube. Sube derecha, haga lo que
     * haga la mano, y se tiende hacia donde la empuja el 'aire'.
     */
    // La hoguera (encargo de la supervivencia): el cerco de piedras, los leños cruzados y una llama grande.
    private static final Material PIEDRA_DEL_CERCO = Material.rugoso(0x6E665E), LENIO = Material.rugoso(0x4A3424), CENIZA = Material.mate(0x5A524C);
    private static final int PIEDRAS_DEL_CERCO = 9, LENIOS = 4;

    /**
     * Una hoguera en el suelo, con 'base' en su centro: un cerco de piedras,
     * cenizas, cuatro leños cruzados con sus brasas y, encima, la llama (dos
     * veces la de una antorcha), que baila con el 'tiempo'.
     */
    public static void hoguera(Escultor e, P3 base, double tiempo, double g) {
        for (int i = 0; i < PIEDRAS_DEL_CERCO; i++) {
            double a = i * 2 * Math.PI / PIEDRAS_DEL_CERCO;
            e.bola(base.mas(new P3(Math.cos(a) * 3.4 * g, Math.sin(a) * 3.4 * g, 0.5 * g)), (0.85 + 0.15 * Math.sin(i * 2.7)) * g, PIEDRA_DEL_CERCO);
        }
        e.bola(base.mas(new P3(0, 0, 0.1 * g)), 2.6 * g, CENIZA);
        for (int i = 0; i < LENIOS; i++) {
            double a = i * Math.PI / LENIOS + 0.3;
            P3 lado = new P3(Math.cos(a) * 2.6 * g, Math.sin(a) * 2.6 * g, 0);
            e.hueso(base.mas(lado).mas(new P3(0, 0, 0.4 * g)), base.menos(lado).mas(new P3(0, 0, 1.4 * g)), 0.42 * g, 0.36 * g, LENIO);
        }
        e.bola(base.mas(new P3(0, 0, 0.9 * g)), 1.1 * g, BRASA);
        llama(e, base.mas(new P3(0, 0, 1.2 * g)), tiempo, 2 * g, new P3(0, 0, 0));
    }

    public static void llama(Escultor e, P3 base, double tiempo, double g, P3 aire) {
        boolean fino = e.escala() * g >= DE_CERCA;
        P3 delante = e.haciaElQueMira(), derecha = e.aLaDerecha();
        // El halo, lo primero: lo demás se pinta encima de él.
        double late = parpadeo(tiempo, 0.3);
        e.bola(base.mas(ARRIBA.por(1.6 * g)), (3.4 + 0.3 * late) * g, HALO);
        e.bola(base.mas(ARRIBA.por(1.4 * g)), (2.1 + 0.25 * late) * g, HALO_JUNTO_A_LA_LLAMA);

        // El humo: volutas que suben, se ensanchan, se apartan y se deshacen.
        for (int k = 0; k < VOLUTAS_DE_HUMO; k++) {
            double vida = fraccion(tiempo * 0.5 + k / (double) VOLUTAS_DE_HUMO);
            P3 donde = tendida(base, (ALTO_DE_LA_LLAMA * 0.8 + vida * 5.5) * g, aire, g)
                    .mas(derecha.por((Math.sin(vida * 5 + k * 2.3) * 0.9 * vida + 0.5 * vida) * g));
            e.bola(donde, (0.55 + 0.95 * vida) * g, HUMO[Math.min(HUMO.length - 1, (int) (vida * HUMO.length))]);
        }

        // Las lenguas, de fuera adentro; cada capa va un poco más cerca del que mira, para que se vea dentro de la anterior.
        //                 capa, a qué lado, alto, grueso
        lengua(e, base, 0, 0.00, 1.00, 1.15, tiempo, g, aire, delante, derecha);
        lengua(e, base, 0, -0.60, 0.64, 0.75, tiempo, g, aire, delante, derecha);
        lengua(e, base, 0, 0.58, 0.55, 0.70, tiempo, g, aire, delante, derecha);
        lengua(e, base, 1, 0.05, 0.78, 0.85, tiempo, g, aire, delante, derecha);
        lengua(e, base, 1, -0.42, 0.46, 0.50, tiempo, g, aire, delante, derecha);
        lengua(e, base, 1, 0.45, 0.40, 0.46, tiempo, g, aire, delante, derecha);
        lengua(e, base, 2, -0.05, 0.55, 0.58, tiempo, g, aire, delante, derecha);
        lengua(e, base, 3, 0.00, 0.30, 0.34, tiempo, g, aire, delante, derecha);

        // Las llamitas que se sueltan por la punta y se apagan en el aire.
        for (int k = 0; k < LLAMITAS_SUELTAS; k++) {
            double vida = fraccion(tiempo * 1.9 + k * 0.5 + 0.2);
            P3 donde = tendida(base, (ALTO_DE_LA_LLAMA * 0.85 + vida * 2.0) * g, aire, g).mas(derecha.por(parpadeo(tiempo, k + 4) * 0.5 * g));
            double radio = 0.38 * (1 - vida) * g;
            e.hueso(donde, donde.mas(ARRIBA.por(radio * 2)), radio, radio * 0.2, TONOS_DE_LLAMA[vida < 0.4 ? 1 : 0]);
        }

        // Las chispas: saltan de la cabeza, suben haciendo eses y se enfrían.
        for (int k = 0; k < CHISPAS; k++) {
            double vida = fraccion(tiempo * (0.7 + 0.13 * k) + k * 0.37);
            if (vida > 0.9) continue;
            double aUnLado = (Math.sin(k * 12.9898) * 1.6 * vida + Math.sin(vida * 9 + k) * 0.35) * g;
            P3 donde = tendida(base, (1.2 + vida * 7.0) * g, aire, g).mas(derecha.por(aUnLado)).mas(delante.por(1.3 * g));
            e.bola(donde, (fino ? 0.13 : 0.05) * g, vida < 0.55 ? CHISPA_VIVA : CHISPA_ROJA);
        }
    }

    /**
     * Una lengua de la llama: nace ancha en la base, engorda un poco más
     * arriba y acaba en punta. 'capa' es su tono (0 = el de fuera), 'lado' a
     * qué lado de la base nace, 'alto' su altura (1 = la llama entera) y 'grueso' su radio.
     */
    private static void lengua(Escultor e, P3 base, int capa, double lado, double alto, double grueso, double tiempo, double g,
            P3 aire, P3 delante, P3 derecha) {
        double semilla = capa * 1.7 + lado * 3.1;
        // Cada lengua se estira y se encoge a su ritmo, y su punta se ladea.
        double largo = ALTO_DE_LA_LLAMA * alto * (1 + 0.20 * parpadeo(tiempo, semilla)) * g;
        double ladeo = (lado * 0.5 + 0.45 * parpadeo(tiempo * 0.8, semilla + 2)) * alto * g;
        P3 pie = base.mas(derecha.por(lado * g)).mas(delante.por(capa * 0.33 * g));
        P3 panza = tendida(pie, largo * 0.28, aire, g).mas(derecha.por(ladeo * 0.25));
        P3 punta = tendida(pie, largo, aire, g).mas(derecha.por(ladeo));
        Material tono = TONOS_DE_LLAMA[capa];
        e.hueso(pie, panza, grueso * 0.8 * g, grueso * g, tono);
        e.hueso(panza, punta, grueso * g, 0.08 * g, tono);
    }

    /** Un punto de la llama a esa altura sobre 'pie': cuanto más arriba, más se lo lleva el aire. */
    private static P3 tendida(P3 pie, double altura, P3 aire, double g) {
        double parte = altura / (ALTO_DE_LA_LLAMA * g);
        return pie.mas(ARRIBA.por(altura)).mas(aire.por(parte * parte * g));
    }

    private static double fraccion(double v) {
        return v - Math.floor(v);
    }
}
