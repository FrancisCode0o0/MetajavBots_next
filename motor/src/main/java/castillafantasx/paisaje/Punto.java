package castillafantasx.paisaje;

/**
 * PUNTO — Lo que hay en un punto del suelo del mapa, ya con forma natural:
 * lo rellena Formas y lo lee el Pintor. Se usa siempre el mismo objeto (no
 * se crea uno por píxel).
 */
public final class Punto {
    public int clase;              // la clase de suelo (Terreno.HIERBA…)
    public int otra;               // la clase que tiene más cerca al otro lado del borde (la misma, si no hay borde cerca)
    public float margen;           // de 0 (justo en el borde entre las dos) a 1 (lejos de cualquier borde)
    public float altura;           // píxeles sobre el llano
    public boolean piedra;         // es una piedra que asoma del agua (una pasadera)
    public boolean cuesta;         // es una cuesta: tierra pisada, con sus peldaños
    public int columna, fila;      // la casilla que más pesa aquí
    public float roca;             // cuánto se mete en una peña: 0.5 es su borde
    public float orilla;           // en tierra, cuánta agua hay al lado; en el agua, lo lejos que queda la orilla (de 0 a 1)
    public float hondura;          // en el agua, de 0 (somera) a 1 (honda)
    public float frescor, pedregal, arbolado, pisado;      // de 0 a 1 (ver Terreno)
    public float masAlto;          // cuánto terreno más alto tiene pegado, en niveles: para la sombra del pie de un corte
    public boolean rio;            // agua que corre
    public boolean paso;           // agua por donde cruza un camino
    public float corrienteX, corrienteY;
}
