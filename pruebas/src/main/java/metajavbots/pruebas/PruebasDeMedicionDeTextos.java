package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;

import java.util.ArrayList;
import java.util.List;

import metajavbots.pantalla.Pincel;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * PRUEBAS DE MEDICIÓN DE TEXTOS — Miden los textos del juego con los avances reales de sus tipografías
 * (sin ventana, sin OpenGL) contra el recuadro que les da cada pantalla. Comprueban los tres idiomas
 * con la letra normal (18) y la grande (26): que nada se corte, se salga del margen o se solape con el
 * texto de al lado. Las reglas y los anchos salen del código de las pantallas; si cambia la pantalla,
 * cambia aquí la regla.
 */
final class PruebasDeMedicionDeTextos {

    /** Ancho lógico de {@link metajavbots.pantalla.Pincel}: 1600 × 900, escala 1. */
    private static final float ANCHO = 1600f;
    /** {@code Pincel.letra} no baja de aquí: si a este tamaño no cabe, {@code cabeEntero} ya no puede arreglarlo. */
    private static final float LEGIBLE = Pincel.LETRA_MINIMA;

    private PruebasDeMedicionDeTextos() {
    }

    static void apuntar() {
        caso("medicion: los textos caben en su recuadro en ES, DE y EN con letra 18 y 26", () -> {
            List<String> fallos = new ArrayList<>();
            try (MedidorDeTextos m = new MedidorDeTextos()) {
                for (Idioma id : Idioma.values()) {
                    for (float letra : new float[]{18f, 26f}) {
                        historial(m, id, letra, fallos);
                        sesion(m, id, letra, fallos);
                        guia(m, id, letra, fallos);
                    }
                }
            }
            cierto(fallos.isEmpty(), "medición de textos:\n  " + String.join("\n  ", fallos));
        });
    }

    /** Historial.cifras: 8 tarjetas de (ancho−40)/8; la etiqueta dispone de cw−20 (Historial.java:157-168). */
    private static void historial(MedidorDeTextos m, Idioma id, float letra, List<String> fallos) {
        float cw = (ANCHO - 40f) / 8f;
        float hueco = cw - 12f;
        String[] claves = {"historial.peleas", "historial.victorias", "historial.derrotas", "historial.empates",
                "historial.porcentaje", "historial.mejor_racha", "historial.dano_medio", "historial.experiencia"};
        for (String clave : claves) {
            comprobar(fallos, m, "historial", id, letra, clave, T.en(clave, id), hueco, false);
        }
    }

    /** SesionDeHoy.parada: tarjeta de (ancho−120)/3; título y tiempo comparten la primera fila (SesionDeHoy.java:44,91-94). */
    private static void sesion(MedidorDeTextos m, Idioma id, float letra, List<String> fallos) {
        float w = (ANCHO - 80f - 40f) / 3f;
        String[][] paradas = {{"sesion.repaso", "20 min"}, {"sesion.nuevo", "25 min"}, {"sesion.pelea", "15 min"}};
        for (String[] parada : paradas) {
            float tiempoW = m.ancho(parada[1], letra * 0.85f, false, false);
            float hueco = w - 64f - tiempoW - 26f;
            comprobar(fallos, m, "sesión", id, letra, parada[0] + " + " + parada[1],
                    T.en(parada[0], id), hueco, true);
        }
    }

    /** GuiaDeCerebros: título desde x=220 con (ancho−240) de hueco, y pasos en iw−50 (GuiaDeCerebros.java:49-74). */
    private static void guia(MedidorDeTextos m, Idioma id, float letra, List<String> fallos) {
        comprobar(fallos, m, "guía", id, letra, "guia.titulo", T.en("guia.titulo", id),
                ANCHO - 240f, true);

        float iw = Math.min(620f, ANCHO * 0.42f);
        float huecoPaso = iw - 50f;
        float tPaso = letra * 0.95f;
        for (int pagina = 1; pagina <= 10; pagina++) {
            String pasos = T.en("guia." + pagina + ".pasos", id);
            for (String paso : pasos.split("\\r?\\n")) {
                for (String palabra : paso.split(" +")) {
                    if (palabra.isEmpty()) continue;
                    float ancho = m.ancho(palabra, tPaso, false, false);
                    if (ancho > huecoPaso) {
                        fallos.add("guía " + id + " letra " + red(letra) + ": «" + palabra + "» (página " + pagina
                                + ") mide " + red(ancho) + " y el hueco es " + red(huecoPaso));
                    }
                }
            }
        }
    }

    /** Si aun a la letra mínima el texto no cabe en el hueco, {@code cabeEntero} ya no puede salvarlo. */
    private static void comprobar(List<String> fallos, MedidorDeTextos m, String lugar, Idioma id, float letra,
            String nombre, String texto, float hueco, boolean negrita) {
        float ancho = m.ancho(texto, LEGIBLE, negrita, false);
        if (ancho > hueco) {
            fallos.add(lugar + " " + id + " letra " + red(letra) + ": «" + nombre + "» mide "
                    + red(ancho) + " a la letra mínima y solo caben " + red(hueco));
        }
    }

    private static String red(float n) {
        return n == Math.rint(n) ? Math.round(n) + "" : String.format("%.1f", n);
    }
}
