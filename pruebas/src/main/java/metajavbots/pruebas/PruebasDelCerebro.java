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
        apuntarParte1();
        apuntarParte2();
        apuntarParte3();
    }

    private static void apuntarParte1() {
        caso("cerebro: sin cerebro elegido no hay cerebro (y el juego sigue)", () -> {
            Jugador j = new Jugador("x");
            igual(null, CerebroDelJuego.de(j));
            j.cerebro = "EN_LINEA";
            j.proveedor = "proveedor_de_prueba_sin_clave";
            igual(null, CerebroDelJuego.de(j), "en línea sin clave: ninguno");
        });

        caso("cerebro: tres ranuras con su reparto; la partida de antes pasa a las ranuras", () -> {
            Jugador j = new Jugador("x");
            j.cerebro = "AMBOS";
            j.proveedor = "proveedor_de_prueba_sin_clave";
            cierto(j.conCerebro(), "el cerebro de antes cuenta");
            igual(metajavbots.cerebro.Ranura.Tipo.LOCAL, j.cerebros[0].tipo, "el local, a la 1");
            igual(metajavbots.cerebro.Ranura.Tipo.EN_LINEA, j.cerebros[1].tipo, "el de en línea, a la 2");
            igual(0, j.ranuraDe(metajavbots.cerebro.Tarea.RAPIDO));
            igual(1, j.ranuraDe(metajavbots.cerebro.Tarea.PESADO));
            // El de en línea no tiene clave: lo pesado lo hace el local (de respaldo).
            Cerebro pesado = CerebroDelJuego.pesado(j);
            cierto(pesado != null && pesado.nombre().startsWith("KoboldCpp"), "sin clave, el local hace lo pesado");
            // Dos ranuras con el mismo modelo comparten KoboldCpp; con otro modelo, cada una el suyo.
            j.cerebros[1].tipo = metajavbots.cerebro.Ranura.Tipo.LOCAL;
            j.cerebros[0].modeloLocal = "a.gguf";
            j.cerebros[1].modeloLocal = "a.gguf";
            j.cerebros[2].tipo = metajavbots.cerebro.Ranura.Tipo.LOCAL;
            j.cerebros[2].modeloLocal = "b.gguf";
            igual(5002, CerebroDelJuego.puertoDe(j, 0));
            igual(5002, CerebroDelJuego.puertoDe(j, 1), "mismo modelo, mismo KoboldCpp");
            igual(5004, CerebroDelJuego.puertoDe(j, 2));
            // Se guarda y se lee igual.
            java.nio.file.Path antes = metajavbots.progreso.Guardado.carpeta;
            java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("metajavbots");
            metajavbots.progreso.Guardado.carpeta = tmp;
            try {
                j.reparto[metajavbots.cerebro.Tarea.TUTOR.ordinal()] = 2;
                metajavbots.progreso.Guardado.guardar(j);
                Jugador k = metajavbots.progreso.Guardado.leer(metajavbots.progreso.Guardado.carpetaDe("x"));
                igual("b.gguf", k.cerebros[2].modeloLocal);
                igual(metajavbots.cerebro.Ranura.Tipo.LOCAL, k.cerebros[1].tipo);
                igual(2, k.ranuraDe(metajavbots.cerebro.Tarea.TUTOR));
            } finally {
                metajavbots.progreso.Guardado.carpeta = antes;
            }
        });

        caso("cerebro: los modelos locales caben en la memoria (se deja sitio a Windows y al juego)", () -> {
            long presupuesto = CerebroLocal.presupuestoMb(), total = CerebroLocal.memoriaTotalMb();
            System.out.println("            memoria " + total + " MB, para modelos " + presupuesto + " MB");
            cierto(total == 0 || presupuesto <= total - CerebroLocal.MEMORIA_RESERVADA_MB || presupuesto == 3000, "queda memoria de sobra");
        });

        caso("cerebro: la clave se enseña tapada", () -> {
            igual("sk-…cdef", Claves.tapada("sk-1234567890abcdef"));
            igual("", Claves.tapada(""));
        });
    }

    private static void apuntarParte2() {
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

        caso("cerebro: varios cerebros locales a la vez, Gemma (rápido) y Qwen3.5 (pesado), sin pasar de la memoria "
                + "(solo con METAJAVBOTS_PROBAR_CEREBRO)", () -> {
            if (System.getenv("METAJAVBOTS_PROBAR_CEREBRO") == null) {
                System.out.println("            (se salta: arranca dos KoboldCpp, tarda)");
                return;
            }
            Jugador j = new Jugador("x");
            for (int i = 0; i < 2; i++) j.cerebros[i].tipo = metajavbots.cerebro.Ranura.Tipo.LOCAL;
            try {
                long t0 = System.currentTimeMillis();
                String r = CerebroDelJuego.rapido(j).preguntar(CerebroDelJuego.sistemaLocutor(Idioma.ES), "Round 1. Player robot took 40 damage, dealt 70.",
                        25);
                System.out.println("            rápido (" + CerebroDelJuego.modeloDe(j, 0) + "): " + (System.currentTimeMillis() - t0) + " ms → " + r);
                t0 = System.currentTimeMillis();
                String r2 = CerebroDelJuego.pesado(j).preguntar(CerebroDelJuego.sistemaTutor(Idioma.ES, "Noob I", "Loops"),
                        "¿Qué es un bucle for? Una frase.", 40);
                System.out.println("            pesado (" + CerebroDelJuego.modeloDe(j, 1) + "): " + (System.currentTimeMillis() - t0) + " ms → "
                        + r2.replace('\n', ' '));
                t0 = System.currentTimeMillis();
                String r3 = CerebroDelJuego.pesado(j).preguntar(CerebroDelJuego.sistemaTutor(Idioma.ES, "Noob I", "Loops"), "¿Y un while? Una frase.", 40);
                System.out.println("            pesado, segunda: " + (System.currentTimeMillis() - t0) + " ms → " + r3.replace('\n', ' '));
                System.out.println("            en marcha: " + CerebroLocal.enMarchaDelJuego() + ", " + CerebroLocal.memoriaEnUsoMb() + " MB de "
                        + CerebroLocal.presupuestoMb());
                igual(2, CerebroLocal.enMarchaDelJuego().size(), "los dos a la vez");
                cierto(!r.isBlank() && !r2.isBlank(), "contestan");
                // El tercero (el de Jarvis, 6,3 GB) no cabe con los otros dos: se apaga el que lleva más rato sin usarse.
                j.cerebros[2].tipo = metajavbots.cerebro.Ranura.Tipo.LOCAL;
                t0 = System.currentTimeMillis();
                String r4 = CerebroDelJuego.deLaRanura(j, 2).preguntar("Answer with one word.", "Say OK.", 4);
                System.out.println("            tercero (" + CerebroDelJuego.modeloDe(j, 2) + "): " + (System.currentTimeMillis() - t0) + " ms → " + r4);
                System.out.println("            en marcha: " + CerebroLocal.enMarchaDelJuego() + ", " + CerebroLocal.memoriaEnUsoMb() + " MB de "
                        + CerebroLocal.presupuestoMb());
                cierto(CerebroLocal.memoriaEnUsoMb() <= CerebroLocal.presupuestoMb(), "nunca pasan del presupuesto");
            } finally {
                CerebroLocal.parar();
            }
        });
    }

    private static void apuntarParte3() {
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
