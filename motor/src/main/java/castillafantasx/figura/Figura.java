package castillafantasx.figura;



/** FIGURA — Algo que el Escultor sabe dibujar en cualquier momento de su movimiento. */
public interface Figura {

    /** Pone sus piezas en el Escultor (la cámara ya está colocada). No llama a terminar(). */
    void esculpir(Escultor e, Instante m);

    /** Su tamaño respecto a un adulto (1 = adulto): multiplica la escala de la cámara. */
    double talla();

    /** Lo que mide de alto, en píxeles de adulto, para encajarla en un recuadro. */
    double alto();

    /**
     * Dónde queda el centro de su cuerpo cuando está tendida en el suelo,
     * muerta, medido desde sus pies (x a su derecha, y hacia delante). Ahí
     * debajo va su mancha, y ahí está su área de choque.
     */
    P3 centroTendido(Instante m);


    /**
     * La clave de sus retoques a mano (editor › Figuras; Retoques), si no se le dio otra al crearla: null si no tiene.
     * Las personas la sacan de su aspecto.
     */
    default String claveDeRetoque() {
        return null;
    }
}
