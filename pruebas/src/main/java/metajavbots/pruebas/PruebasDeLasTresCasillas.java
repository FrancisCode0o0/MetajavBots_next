package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;

import java.util.ArrayList;
import java.util.List;

import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.cerebro.CerebroLocal;
import metajavbots.cerebro.Ranura;
import metajavbots.progreso.Jugador;

/**
 * Las tres casillas de Fran a la vez, de verdad (bloques 2 y 7): Gemma y Qwen3.5 en su ordenador y Gemini en línea,
 * preguntados a la vez. Arranca dos KoboldCpp y llama a Google: solo con METAJAVBOTS_PROBAR_CEREBRO.
 */
final class PruebasDeLasTresCasillas {

    private PruebasDeLasTresCasillas() {
    }

    /** La configuración que se deja en la partida de Fran (bloque 7). */
    static Jugador conLasTres() {
        Jugador j = new Jugador("x");
        j.cerebros[0].tipo = Ranura.Tipo.LOCAL;
        j.cerebros[0].modeloLocal = "google_gemma-3-4b-it-Q4_K_M.gguf";
        j.cerebros[1].tipo = Ranura.Tipo.LOCAL;
        j.cerebros[1].modeloLocal = "Qwen_Qwen3.5-9B-Q4_K_M.gguf";
        j.cerebros[2].tipo = Ranura.Tipo.EN_LINEA;
        j.cerebros[2].proveedor = "gemini";
        return j;
    }

    static void apuntar() {
        caso("cerebros: las tres casillas de Fran a la vez de verdad: Gemma y Qwen3.5 locales y Gemini (solo con METAJAVBOTS_PROBAR_CEREBRO)", () -> {
            if (System.getenv("METAJAVBOTS_PROBAR_CEREBRO") == null) {
                System.out.println("            (se salta: arranca dos KoboldCpp y llama a Google)");
                return;
            }
            Jugador j = conLasTres();
            try {
                long t0 = System.currentTimeMillis();
                List<CerebroDelJuego.Peticion> ps = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    cierto(CerebroDelJuego.deLaRanura(j, i) != null, "casilla " + (i + 1) + " llena");
                    ps.add(CerebroDelJuego.Peticion.lanzar(CerebroDelJuego.deLaRanura(j, i), "Answer in Spanish, one short sentence.",
                            CerebroDelJuego.uno("¿Qué es una variable en Java?"), 60));
                }
                while (ps.stream().anyMatch(p -> !p.lista) && System.currentTimeMillis() - t0 < 540_000) Thread.sleep(500);
                for (int i = 0; i < 3; i++) {
                    CerebroDelJuego.Peticion p = ps.get(i);
                    System.out.println("            casilla " + (i + 1) + ": " + (p.respuesta != null ? p.respuesta.replace('\n', ' ') : "ERROR " + p.error));
                }
                System.out.println("            en " + (System.currentTimeMillis() - t0) / 1000 + " s; en marcha: " + CerebroLocal.enMarchaDelJuego() + ", "
                        + CerebroLocal.memoriaEnUsoMb() + " MB de " + CerebroLocal.presupuestoMb());
                for (int i = 0; i < 2; i++) cierto(ps.get(i).respuesta != null && !ps.get(i).respuesta.isBlank(), "la casilla local " + (i + 1) + " contesta");
                // Google a veces dice «mucha demanda» (503): eso no es un fallo del juego; cualquier otro error, sí.
                CerebroDelJuego.Peticion g = ps.get(2);
                boolean contesta = g.respuesta != null && !g.respuesta.isBlank();
                cierto(contesta || String.valueOf(g.error).contains("503"), "Gemini contesta (o Google está ocupado): " + g.error);
            } finally {
                CerebroLocal.cerrar();
            }
        });
    }
}
