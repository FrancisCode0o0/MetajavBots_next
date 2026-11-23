package metajavbots.api;

/**
 * PIEZA PROPIA — Contrato en Java para diseñar tu propia pieza de robot desde
 * el Taller. Implementa esta interfaz en tu clase, elige su ranura ({@code CABEZA},
 * {@code BRAZO}, {@code PIERNAS} o {@code NUCLEO}) y define sus atributos dentro
 * del presupuesto de equilibrio (máximo 100 puntos).
 *
 * <p>Tema: interfaces (13), encapsulación y diseño equilibrado de componentes.</p>
 */
public interface PiezaPropia {

    /** Nombre visible de tu pieza (1 a 28 caracteres). */
    String nombre();

    /** Ranura donde se monta: {@code "CABEZA"}, {@code "BRAZO"}, {@code "PIERNAS"} o {@code "NUCLEO"}. */
    String ranura();

    /** Naturaleza de la pieza: {@code "ACERO"}, {@code "RAYO"}, {@code "FOCO"}, {@code "FURIA"}, {@code "INGENIO"} o {@code "ETER"}. */
    default String naturaleza() {
        return "ACERO";
    }

    /**
     * Estilo según la ranura: arma en {@code BRAZO} ({@code LASER}, {@code BALA}, {@code MISIL}, {@code CUERPO},
     * {@code LLAMA}, {@code ESCUDO}, {@code REPARAR}), especial en {@code CABEZA} ({@code ESCUDO}, {@code TURBO},
     * {@code REPARAR}, {@code CAMUFLAJE}, {@code PULSO}, {@code SOBRECARGA}) o locomoción en {@code PIERNAS}
     * ({@code BIPEDO}, {@code ORUGAS}, {@code RUEDAS}, {@code FLOTANTE}, {@code VUELO}, {@code PATAS}).
     */
    default String estilo() {
        return "LASER";
    }

    /** Blindaje extra que aporta la pieza. */
    default double blindaje() {
        return 10;
    }

    /** Peso de la pieza (más peso libera presupuesto, pero sobrecargar las piernas frena al robot). */
    default double peso() {
        return 6;
    }

    /** Daño por ataque (solo para {@code BRAZO}). */
    default double dano() {
        return 9;
    }

    /** Alcance del arma (solo para {@code BRAZO}). */
    default double alcance() {
        return 260;
    }

    /** Turnos de enfriamiento entre ataques (solo para {@code BRAZO}; menor valor = dispara antes). */
    default double enfriamiento() {
        return 18;
    }

    /** Energía consumida al atacar o activar el especial. */
    default double energia() {
        return 10;
    }

    /** Alcance del radar (solo para {@code CABEZA}). */
    default double radar() {
        return 360;
    }

    /** Ángulo de apertura del cono del radar en grados (solo para {@code CABEZA}). */
    default double cono() {
        return 50;
    }

    /** Velocidad base (solo para {@code PIERNAS}). */
    default double velocidad() {
        return 4.0;
    }

    /** Velocidad de giro en grados por turno (solo para {@code PIERNAS}). */
    default double giro() {
        return 6.0;
    }

    /** Aceleración por turno (solo para {@code PIERNAS}). */
    default double aceleracion() {
        return 0.5;
    }

    /** Carga que aguantan las piernas: el chasis y las demás piezas; pasarse frena y, por encima del 120 %, no deja pelear (solo {@code PIERNAS}). */
    default double carga() {
        return 32;
    }

    /** Pasos de cálculo por turno (solo para {@code NUCLEO}). */
    default double pasos() {
        return 3200;
    }

    /** Energía máxima del robot (solo para {@code NUCLEO}). */
    default double energiaMax() {
        return 105;
    }

    /** Recarga de energía por turno (solo para {@code NUCLEO}). */
    default double recarga() {
        return 0.42;
    }
}
