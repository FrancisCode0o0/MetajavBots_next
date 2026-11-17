package metajavbots.cerebro;

import java.util.List;

/**
 * CEREBRO — Un modelo de lenguaje que el juego puede usar (docs/CEREBRO.md):
 * el local (KoboldCpp, como el de Jarvis) o uno en línea con la clave del
 * jugador. El juego funciona entero sin él; con él, los tutoriales se
 * vuelven conversaciones, los robots charlan, la computadora suelta pullas
 * nuevas, el código se revisa tras cada pelea y el temario se convierte en
 * ejercicios. Las llamadas tardan: se hacen siempre en un hilo aparte.
 */
public interface Cerebro {

    /** Un mensaje de la conversación: «user» (el jugador o el juego) o «assistant» (el cerebro). */
    final class Mensaje {
        public final String rol, texto;

        public Mensaje(String rol, String texto) {
            this.rol = rol;
            this.texto = texto;
        }
    }

    String nombre();

    /** Pregunta y espera la respuesta (en un hilo aparte, nunca en el del dibujo). */
    String preguntar(String sistema, List<Mensaje> conversacion, int maximoDePalabras) throws Exception;

    default String preguntar(String sistema, String pregunta, int maximoDePalabras) throws Exception {
        return preguntar(sistema, List.of(new Mensaje("user", pregunta)), maximoDePalabras);
    }
}
