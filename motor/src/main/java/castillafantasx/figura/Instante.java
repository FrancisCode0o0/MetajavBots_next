package castillafantasx.figura;

/**
 * INSTANTE — Lo que cualquier figura necesita saber para dibujarse en este momento: qué gesto hace y por dónde va,
 * cómo está su cuerpo (alerta, herido, cansado, mojado) y el agua que lo cubre. Lo rellena la lógica; lo leen las
 * figuras y el Escultor. Cada juego le añade lo suyo heredando de él (el arma, el combo…).
 */
public class Instante {

    /** Lo que lleva en la mano derecha quien no lleva arma: nada, una piedra o un palo cogidos del suelo. */
    public enum Lleva { NADA, PIEDRA, PALO }

    /** La lumbre que lleva en la mano izquierda: ninguna, el farol de mano o una antorcha (docs/encargos_viejos/ENCARGO_DIA_NOCHE.md). */
    public enum Lumbre { NINGUNA, FAROL, ANTORCHA }

    /**
     * Lo que tiene en la mano quien se está curando: el pan, la cecina, el queso, la bota de vino, la escudilla, el
     * tarro, la orza; y (encargo de la supervivencia) la carne cruda, la pieza asada, el pescado, la redoma de una
     * poción y las plantas: la ramita, las hojas, las flores y la raíz.
     */
    public enum Toma { NADA, PAN, CECINA, QUESO, BOTA, CUENCO, TARRO, ORZA, CARNE_CRUDA, ASADO, PESCADO, REDOMA, RAMITA, HOJAS, FLORES, RAIZ }

    public Gesto gesto = Gesto.QUIETO;
    /** Ciclos de paso: avanza con la distancia recorrida (1 = dos pasos). */
    public double fase;
    /** De 0 a 1: por dónde va un gesto que empieza y acaba (atacar, morir, desenvainar…). */
    public double avance;
    /** Segundos de reloj: mueve lo que se mueve solo (la respiración, la capa, los tentáculos). */
    public double tiempo;
    /** El puñetazo de ahora sale del puño izquierdo. Al morir: cae hacia el otro lado (así no caen todos igual). */
    public boolean zurdo;
    /** En guardia de pelea (una persona) o en peligro (una criatura): tenso y rápido. */
    public boolean alerta;
    /** Se desplaza mientras hace el gesto (ataca o se cubre sin dejar de andar). */
    public boolean andando;
    /** Qué golpe del combo es el ataque en curso (0 = el primero). */
    public int paso;
    /** Hacia qué costado se desplaza en un golpe «de lado»: 1 a su derecha, -1 a su izquierda. */
    public int lado = 1;
    /** De 0 a 1: cuánto está en guardia de combate con el arma (0 = la lleva sin más). */
    public double tension;
    /** Acaba de parar un golpe: 1 en el instante del choque y baja hasta 0 mientras se recompone. */
    public double impacto;
    /** En primera persona, cuánto ha subido ya la guardia: de 0 (manos abajo) a 1 (cubierto); -1 = lo que diga el gesto. */
    public double alza = -1;
    /** En primera persona, lo que queda de los ojos al techo, en casillas: bajo un techo bajo, el arma larga se inclina para no atravesarlo. */
    public double techoSobreLosOjos = SIN_TECHO;
    public static final double SIN_TECHO = 99;
    /** Acaba de recibir un golpe que le ha hecho daño: 1 en el instante del golpe y baja hasta 0 (en primera persona, los brazos se sacuden). */
    public double sacudida;
    /** Sus colores (RRGGBB): lo de dentro, la carne, la hoja…, y el segundo (la grasa, la flor…). -1 = los de su forma. */
    public int tinte = -1, tinte2 = -1;
    /** Al levantarse: estaba caído de bruces (si no, de culo). Al morir: cae de bruces (si no, de espaldas). */
    public boolean deBruces;
    /** Al morir: ya estaba en el suelo cuando lo mataron (no vuelve a caer: se queda tendido donde estaba). */
    public boolean yaCaido;
    /** Al morir: por dónde va la caída de su arma al suelo, de 0 a 1 (0 = no se le cae ninguna). */
    public double armaCae;
    /**
     * El cuerpo avisa (lo rellena salud.Senales; solo el héroe): de 0 a 1, cuánto se le nota
     * la herida, la agonía (casi muerto), la fatiga y el tambaleo (a punto de caer).
     */
    public double herida, agonia, fatiga, tambaleo;
    /** Por dónde van su respiración y su latido, en vueltas (cada unidad, una respiración o un latido entero). */
    public double respiracion, latido;
    /**
     * El agua (lo rellena agua.Nado; solo el héroe): 'agua' son los píxeles de figura que le cubre por encima
     * de los pies (0 = en seco); 'nado', de 0 (de pie) a 1 (tendido nadando); 'brazada', por dónde va, en vueltas;
     * 'hundido', de 0 a 1, cuánto se hunde de cansado.
     */
    public double agua, nado, brazada, hundido;
    /** Nada a brazadas fuertes; y lleva el arma guardada a la espalda (se la echó al meterse en lo hondo). */
    public boolean brazadaFuerte, aLaEspalda;
    /** De 0 a 1: lo mojado que está (ropa oscura, gotea) y cuánto tirita de frío. */
    public double mojado, tirita;
    // Memoria del dibujo: cuánto vuelan ahora sus telas (capa, falda, ala del sombrero) y cuándo se miró por última vez.
    private double vueloDeLasTelas, tiempoDeLasTelas = Double.NaN;
    // TODO-FRAN: cuánto tardan las telas en levantarse y en volver a caer (cifras PROVISIONALES).
    private static final double TELA_SE_LEVANTA = 0.22, TELA_VUELVE_A_CAER = 0.50;   // segundos que tarda en seguir al cuerpo
    private static final double SALTO_DE_TIEMPO = 0.3;      // si pasa más que esto sin dibujarlo, la tela no arrastra retraso

    /**
     * Las telas siguen al cuerpo con retraso: dado lo que deberían volar
     * ahora ('objetivo', de 0 a 1), devuelve lo que vuelan de verdad, que se
     * va acercando poco a poco (más despacio al volver a caer).
     */
    public double mecer(double objetivo) {
        double pasa = tiempo - tiempoDeLasTelas;
        if (Double.isNaN(pasa) || pasa < 0 || pasa > SALTO_DE_TIEMPO) {
            vueloDeLasTelas = objetivo;
        } else if (pasa > 0) {
            double tarda = objetivo > vueloDeLasTelas ? TELA_SE_LEVANTA : TELA_VUELVE_A_CAER;
            vueloDeLasTelas += (objetivo - vueloDeLasTelas) * (1 - Math.exp(-pasa / tarda));
        }
        tiempoDeLasTelas = tiempo;
        return vueloDeLasTelas;
    }
    /** Sin señales en el cuerpo: ni herido, ni agotado, ni a punto de caer. */
    public void sinSenales() {
        herida = 0;
        agonia = 0;
        fatiga = 0;
        tambaleo = 0;
        sinAgua();
    }

    /** En seco y seco: ni agua debajo, ni mojado, ni tiritando. */
    public void sinAgua() {
        agua = 0;
        brazadaFuerte = false;
        aLaEspalda = false;
        nado = 0;
        hundido = 0;
        mojado = 0;
        tirita = 0;
    }

    /** Parte de la caída de morir que usan las criaturas para caer derribadas y levantarse. */
    private static final double CAIDA_DE_BESTIA = 0.6;

    /** Un instante nuevo de la misma clase que este (el juego, que lo hereda, da uno suyo). */
    public Instante nuevoIgual() {
        return new Instante();
    }

    /** Copia en este instante todo lo de otro. */
    public void copiar(Instante de) {
        gesto = de.gesto;
        tinte2 = de.tinte2;
        fase = de.fase;
        avance = de.avance;
        tiempo = de.tiempo;
        zurdo = de.zurdo;
        alerta = de.alerta;
        andando = de.andando;
        paso = de.paso;
        lado = de.lado;
        tension = de.tension;
        impacto = de.impacto;
        alza = de.alza;
        techoSobreLosOjos = de.techoSobreLosOjos;
        sacudida = de.sacudida;
        tinte = de.tinte;
        deBruces = de.deBruces;
        yaCaido = de.yaCaido;
        armaCae = de.armaCae;
        herida = de.herida;
        agonia = de.agonia;
        fatiga = de.fatiga;
        tambaleo = de.tambaleo;
        respiracion = de.respiracion;
        latido = de.latido;
        agua = de.agua;
        brazadaFuerte = de.brazadaFuerte;
        aLaEspalda = de.aLaEspalda;
        nado = de.nado;
        brazada = de.brazada;
        hundido = de.hundido;
        mojado = de.mojado;
        tirita = de.tirita;
    }

    /** Cuánto ha caído ya quien muere, de 0 (de pie) a 1 (en el suelo). 'parte': qué parte del gesto de morir dura la caída. */
    public double caidaAlMorir(double parte) {
        if (gesto != Gesto.MORIR) return 0;
        return yaCaido ? 1 : Pose.suave(avance / parte);
    }

    /**
     * Las criaturas no tienen los gestos de la gente: caen derribadas y se
     * levantan con su caída de morir, y lo demás lo hacen con el gesto suyo
     * que más se parece. 'destino' es un momento de trabajo de la figura.
     */
    public Instante paraBestia(Instante destino) {
        if (!gesto.esDeReaccion() && !gesto.esDeDerribo() && !gesto.esDeSalto() && !gesto.esDeCura() && !gesto.esDeAgua() && !gesto.esDeFaena()
                && gesto != Gesto.PERDER_ARMA && gesto != Gesto.RECOGER_ARMA) return this;
        destino.copiar(this);
        if (gesto == Gesto.CAER_DE_CULO || gesto == Gesto.CAER_DE_BRUCES) {
            destino.gesto = Gesto.MORIR;
            destino.avance = avance * CAIDA_DE_BESTIA;
        } else if (gesto.esDeDerribo()) {
            destino.gesto = Gesto.MORIR;
            destino.avance = (1 - avance) * CAIDA_DE_BESTIA;
        } else {
            destino.gesto = gesto == Gesto.HUIR || gesto == Gesto.SALTAR || gesto == Gesto.ESQUIVAR || gesto == Gesto.ZAMBULLIRSE ? Gesto.CORRER
                    : gesto == Gesto.ATACAR_EN_SALTO || gesto == Gesto.ATACAR_NADANDO ? Gesto.ATACAR
                    : gesto == Gesto.NADAR ? Gesto.ANDAR : Gesto.QUIETO;
        }
        return destino;
    }
}
