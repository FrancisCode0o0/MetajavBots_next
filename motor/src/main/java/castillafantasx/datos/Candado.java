package castillafantasx.datos;

import java.nio.file.Path;

/**
 * CANDADO — Para las pruebas que pulsan teclas al azar: mientras está echado, el juego solo puede
 * escribir dentro de una carpeta (la temporal). Si algo intenta escribir fuera (el res/ de Fran, su
 * partida, sus teclas), salta un error que dice qué archivo era. En el juego de verdad no hace nada.
 */
public final class Candado {

    private Candado() { }

    /** La única carpeta donde se puede escribir, o null (lo normal): se escribe donde toque. */
    public static Path soloEn;

    /** Se llama justo antes de escribir un archivo. */
    public static void vigilar(Path archivo) {
        if (soloEn == null) return;
        Path destino = archivo.toAbsolutePath().normalize();
        if (!destino.startsWith(soloEn.toAbsolutePath().normalize())) {
            throw new IllegalStateException("Con el candado echado se ha intentado escribir fuera de la carpeta de pruebas: " + destino);
        }
    }
}
