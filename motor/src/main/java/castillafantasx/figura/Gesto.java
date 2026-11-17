package castillafantasx.figura;

/** Lo que puede estar haciendo una figura. Las criaturas usan solo algunos. */
public enum Gesto {
    QUIETO, ANDAR, CORRER,
    /** Postura firme: se cubre con el arma, el escudo o los antebrazos. */
    GUARDIA,
    /** Un ataque entero: preparación, golpe y recuperación ('avance' de 0 a 1). */
    ATACAR,
    /** Acaba de recibir un golpe. */
    GOLPE,
    MORIR, DESENVAINAR, ENVAINAR, HABLAR,
    /** El aviso antes de embestir de testa: baja la cabeza y escarba. Los demás lo hacen como «quieto». */
    AVISAR,

    // --- Reacciones de la gente (encargo de reacciones) ---
    /** Plantado, voceando y agitando el puño. */
    VOCEAR,
    /** Corre despavorido, con los brazos en alto y mirando atrás. */
    HUIR,
    /** Se agacha y coge una piedra del suelo ('avance' de 0 a 1). */
    COGER_PIEDRA,
    /** Lanza la piedra que lleva en la mano. */
    LANZAR_PIEDRA,
    /** Se agacha y coge un palo del suelo. */
    COGER_PALO,
    /** Un golpe de arriba abajo con el palo. */
    GARROTAZO,

    // --- Desarme y derribo ---
    /** Le arrancan el arma de la mano: sale despedida. */
    PERDER_ARMA,
    /** Se agacha y recoge un arma del suelo. */
    RECOGER_ARMA,
    /** Cae sentado (el golpe le llega de frente); con 'avance' 1 se queda en el suelo. */
    CAER_DE_CULO,
    /** Cae boca abajo (el golpe le llega por la espalda o de lado). */
    CAER_DE_BRUCES,
    /** Se levanta del suelo (Momento.deBruces dice de cuál de las dos caídas). */
    LEVANTARSE,
    /** Se levanta lanzando un golpe corto de abajo arriba. */
    LEVANTARSE_GOLPEANDO,

    // --- Salto y esquiva (docs/encargos_viejos/ENCARGO_SALTO.md) ---
    /** Un salto entero: se impulsa, vuela con las piernas recogidas, cae y se recompone ('avance' de 0 a 1). */
    SALTAR,
    /** El quiebro: se encoge y rueda fuera del golpe. */
    ESQUIVAR,
    /** Se agarra al borde de un desnivel y sube a él. */
    ENCARAMARSE,
    /** El golpe en el aire, propio de cada estilo: cae con un tajo, con una estocada desde arriba o con un mazazo. */
    ATACAR_EN_SALTO,

    // --- Curarse (docs/encargos_viejos/ENCARGO_INTERFAZ.md): 'avance' de 0 a 1; lo que tiene en la mano lo dice Momento.toma ---
    /** Come: lleva a la boca lo que tiene en la mano y le da varios bocados. */
    COMER,
    /** Bebe: alza la bota o la escudilla y echa la cabeza atrás. */
    BEBER,
    /** Se unta: coge ungüento del tarro y se lo extiende por el otro brazo. */
    UNTARSE,

    // --- El agua (docs/encargos_viejos/ENCARGO_INTERFAZ.md): cuánta agua tiene debajo lo dice Momento.agua ---
    /** Nada: tendido, con la cabeza fuera, a brazadas ('Momento.brazada' lleva el ritmo). */
    NADAR,
    /** Se tira de cabeza: hasta la mitad de 'avance' va por el aire; después, bajo el agua, hasta salir a flote. */
    ZAMBULLIRSE,
    /** Un golpe dado nadando: se incorpora en el agua y barre con el arma (o con el puño) por encima de ella. */
    ATACAR_NADANDO,

    // --- Las faenas (encargo de la supervivencia): agachado, con las manos; 'avance' de 0 a 1 ---
    /** Saca la carne de un cuerpo: agachado sobre él, lo sujeta con la izquierda y corta con la daga. */
    DESPIEZAR,
    /** Arranca una planta: se agacha, la coge con las dos manos, tira y se incorpora. */
    ARRANCAR,
    /** Enciende una hoguera: agachado, golpea el eslabón contra el pedernal sobre la yesca. */
    ENCENDER,
    /** Guisa en un fuego: agachado junto a la lumbre, con la mano sobre ella, removiendo o dando vueltas al asador. */
    GUISAR;

    /** ¿Es una de las faenas (despiezar, arrancar, encender o guisar)? Las criaturas no las tienen. */
    public boolean esDeFaena() {
        return this == DESPIEZAR || this == ARRANCAR || this == ENCENDER || this == GUISAR;
    }

    /** ¿Es uno de los gestos del agua (nadar, tirarse de cabeza o golpear nadando)? Las criaturas no los tienen. */
    public boolean esDeAgua() {
        return this == NADAR || this == ZAMBULLIRSE || this == ATACAR_NADANDO;
    }

    /** ¿Es uno de los gestos de curarse (comer, beber o untarse)? Las criaturas no los tienen. */
    public boolean esDeCura() {
        return this == COMER || this == BEBER || this == UNTARSE;
    }

    /** ¿Es uno de los gestos de la gente que reacciona sin arma (vocear, huir, piedra o palo)? */
    public boolean esDeReaccion() {
        return this == VOCEAR || this == HUIR || this == COGER_PIEDRA || this == LANZAR_PIEDRA || this == COGER_PALO || this == GARROTAZO;
    }

    /** ¿Es uno de los gestos del salto y la esquiva? Las criaturas no los tienen. */
    public boolean esDeSalto() {
        return this == SALTAR || this == ESQUIVAR || this == ENCARAMARSE || this == ATACAR_EN_SALTO;
    }

    /** ¿Es caer al suelo o levantarse de él? */
    public boolean esDeDerribo() {
        return this == CAER_DE_CULO || this == CAER_DE_BRUCES || this == LEVANTARSE || this == LEVANTARSE_GOLPEANDO;
    }
}
