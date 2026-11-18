package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.List;

import metajavbots.codigo.DocumentacionPropia;
import metajavbots.pantallas.Referencia;
import metajavbots.textos.Idioma;

/** El editor como un IDE (bloque 3): la documentación al pasar el ratón y el autocompletado con las clases del propio jugador. */
final class PruebasDelIde {

    private PruebasDelIde() {
    }

    private static final String CODIGO = "public class MiRobot extends Metajavbot {\n"
            + "    private final Arma espada = new Arma(\"espada\", 12);\n"
            + "    int vueltas;\n\n"
            + "    /** Cuánto daño hace un golpe con la espada, contando el turbo. */\n"
            + "    @Override\n"
            + "    public void cadaTurno() {\n"
            + "        espada.\n"
            + "    }\n\n"
            + "    /**\n     * Lo que hace daño.\n     * Cada arma, con su nombre.\n     */\n"
            + "    static class Arma {\n"
            + "        private final String nombre;\n"
            + "        int dano;\n\n"
            + "        Arma(String nombre, int dano) {\n            this.nombre = nombre;\n            this.dano = dano;\n        }\n\n"
            + "        /** Dispara una vez y devuelve el daño. */\n"
            + "        int disparar(double angulo) {\n            int local = dano;\n            return local;\n        }\n\n"
            + "        String nombre() {\n            return nombre;\n        }\n"
            + "    }\n"
            + "}\n";

    static void apuntar() {
        caso("editor: como un IDE, documenta y completa las clases del jugador (y la API, al pasar el ratón)", () -> {
            String d = DocumentacionPropia.de(CODIGO, "disparar");
            cierto(d != null && d.startsWith("int disparar(double angulo)") && d.contains("Dispara una vez y devuelve el daño."), "método: " + d);
            String a = DocumentacionPropia.de(CODIGO, "Arma");
            cierto(a != null && a.startsWith("static class Arma") && a.contains("Lo que hace daño. Cada arma, con su nombre."), "clase: " + a);
            String c = DocumentacionPropia.de(CODIGO, "vueltas");
            igual("int vueltas;", c, "campo sin comentario");
            cierto(DocumentacionPropia.de(CODIGO, "noExiste") == null, "lo que no declara");
            List<String> m = DocumentacionPropia.miembros(CODIGO, "espada");
            cierto(m.containsAll(List.of("nombre", "dano", "disparar")) && !m.contains("local"), "tras «espada.»: " + m);
            cierto(DocumentacionPropia.miembros(CODIGO, "Arma").contains("disparar"), "tras «Arma.»");
            cierto(DocumentacionPropia.miembros(CODIGO, "Math").isEmpty(), "lo que no es suyo");
            String api = Referencia.de("avanzar", Idioma.ES);
            cierto(api != null && api.contains("avanzar("), "la API: " + api);
            cierto(Referencia.de("jedenZug", Idioma.DE) != null, "la API en alemán");
        });
    }
}
