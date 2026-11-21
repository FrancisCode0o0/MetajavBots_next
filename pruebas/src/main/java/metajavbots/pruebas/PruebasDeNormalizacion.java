package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.igual;

import metajavbots.combate.Pelea;

/** Prueba la normalización angular compartida. */
final class PruebasDeNormalizacion {

    private PruebasDeNormalizacion() {
    }

    static void apuntar() {
        caso("normalización angular única", () -> {
            igual(-10.0, Pelea.normal(350), "350 grados");
            igual(180.0, Pelea.normal(180), "180 grados");
            igual(180.0, Pelea.normal(-180), "-180 grados");
            igual(180.0, Pelea.normal(540), "540 grados");
            igual(0.0, Pelea.normal(Double.NaN), "NaN");
            igual(0.0, Pelea.normal(Double.POSITIVE_INFINITY), "infinito positivo");
            igual(0.0, Pelea.normal(Double.NEGATIVE_INFINITY), "infinito negativo");
        });
    }
}
