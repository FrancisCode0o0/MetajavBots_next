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

        caso("cerebro: el local se arranca solo al preguntar y se apaga (solo con METAJAVBOTS_PROBAR_CEREBRO)", () -> {
            if (System.getenv("METAJAVBOTS_PROBAR_CEREBRO") == null) {
                System.out.println("            (se salta: arranca KoboldCpp, tarda)");
                return;
            }
            java.nio.file.Path carpeta = CerebroLocal.carpeta(null);
            Cerebro c = new metajavbots.cerebro.CerebroLocalPerezoso("http://localhost:" + CerebroLocal.PUERTO_DEL_JUEGO, carpeta,
                    "google_gemma-3-4b-it-Q4_K_M.gguf");
            long t0 = System.currentTimeMillis();
            String r = c.preguntar(CerebroDelJuego.sistemaPullas(Idioma.EN, "sarcastico", 0.7), "Opponent robot: Bolt.", 20);
            long arranque = System.currentTimeMillis() - t0;
            t0 = System.currentTimeMillis();
            String r2 = c.preguntar(CerebroDelJuego.sistemaLocutor(Idioma.ES), "Round 1. Player robot took 40 damage, dealt 70.", 25);
            long segunda = System.currentTimeMillis() - t0;
            System.out.println("            arranque + primera: " + arranque + " ms → " + r);
            System.out.println("            segunda: " + segunda + " ms → " + r2);
            CerebroLocal.parar();
            cierto(!r.isBlank() && !r2.isBlank(), "contesta");
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
