package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.cerebro.CerebroHttp;
import metajavbots.cerebro.CerebroLocal;
import metajavbots.cerebro.Claves;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/** El cerebro de IA: sin él todo funciona; con uno local en marcha (KoboldCpp), contesta de verdad. */
final class PruebasDelCerebro {

    private PruebasDelCerebro() {
    }

    static void apuntar() {
        caso("cerebro: sin cerebro elegido no hay cerebro (y el juego sigue)", () -> {
            Jugador j = new Jugador("x");
            igual(null, CerebroDelJuego.de(j));
            j.cerebro = "EN_LINEA";
            j.proveedor = "proveedor_de_prueba_sin_clave";
            igual(null, CerebroDelJuego.de(j), "en línea sin clave: ninguno");
        });

        caso("cerebro: la clave se enseña tapada", () -> {
            igual("sk-…cdef", Claves.tapada("sk-1234567890abcdef"));
            igual("", Claves.tapada(""));
        });

        caso("cerebro: si hay un KoboldCpp escuchando, contesta (pulla moderada y tutor)", () -> {
            String d = "http://localhost:" + CerebroLocal.PUERTO_DEL_JUEGO;
            if (!CerebroLocal.escucha(d)) {
                System.out.println("            (no hay cerebro local en marcha: se prueba solo lo demás)");
                return;
            }
            Cerebro c = new CerebroHttp(CerebroHttp.Forma.OPENAI, d, "", "", "KoboldCpp");
            String pulla = c.preguntar(CerebroDelJuego.sistemaPullas(Idioma.ES, "bravucon", 0.8), "Opponent robot: Cobaltito.", 20);
            System.out.println("            pulla: " + pulla);
            cierto(!pulla.isBlank(), "contesta algo");
            String tutor = c.preguntar(CerebroDelJuego.sistemaTutor(Idioma.DE, "Noob I", "Loops"), "Was ist eine for-Schleife? Ein Satz.", 30);
            System.out.println("            tutor: " + tutor.replace('\n', ' '));
            cierto(tutor.length() > 20, "el tutor explica");
        });
    }
}
