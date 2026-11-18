package metajavbots.codigo;

/**
 * CAMBIO — Lo que guarda el deshacer del Documento por cada cambio del texto:
 * en «pos» se quitó «quitado» y se puso «puesto», y dónde estaban el cursor y
 * la selección antes y después. Escribir seguido se junta en un solo cambio.
 */
final class Cambio {
    final int pos;
    String quitado, puesto;
    final int cursorAntes, anclaAntes;
    int cursorDespues, anclaDespues;
    final String tipo;
    long hora;

    Cambio(int pos, String quitado, String puesto, int cursorAntes, int anclaAntes, String tipo, long hora) {
        this.pos = pos;
        this.quitado = quitado;
        this.puesto = puesto;
        this.cursorAntes = cursorAntes;
        this.anclaAntes = anclaAntes;
        this.tipo = tipo;
        this.hora = hora;
    }
}
