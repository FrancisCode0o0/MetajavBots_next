package metajavbots.pantallas;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import metajavbots.textos.Idioma;

/**
 * CÓDIGO DE APARATO — El código de cada aparato de la arena
 * (res/aparatos/<tipo>.txt), en la API de cada idioma: su clase con su
 * documentación, que el jugador lee para saber a qué hueco se conecta y cómo
 * se usa. Así se aprende a leer código ajeno.
 */
final class CodigoDeAparato {

    private CodigoDeAparato() {
    }

    /** El código de ese aparato en la API de ese idioma («== es», «== de», «== en» en el archivo). */
    static String texto(String tipo, Idioma idioma) {
        try (InputStream in = CodigoDeAparato.class.getResourceAsStream("/aparatos/" + tipo + ".txt")) {
            if (in == null) return "";
            String todo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String marca = "== " + idioma.codigo() + "\n";
            int i = todo.indexOf(marca);
            if (i < 0) return todo;
            int fin = todo.indexOf("\n== ", i + marca.length());
            return todo.substring(i + marca.length(), fin < 0 ? todo.length() : fin + 1);
        } catch (Exception e) {
            return "";
        }
    }
}
