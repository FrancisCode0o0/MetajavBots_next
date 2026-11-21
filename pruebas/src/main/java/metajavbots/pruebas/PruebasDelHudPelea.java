package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;

import metajavbots.combate.Aparatos;
import metajavbots.pantallas.Marcadores;
import metajavbots.pantallas.PanelDelAparato;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas de geometría del HUD de la pelea (marcadores, barra inferior, panel del aparato y banda del locutor). */
final class PruebasDelHudPelea {

    private PruebasDelHudPelea() {
    }

    static void apuntar() {
        barraInferiorSinSolapamientos();
        panelDelAparatoYMarcadores();
        bandaDelLocutorYTecho();
    }

    private static void barraInferiorSinSolapamientos() {
        caso("hud pelea: barra inferior y botón del aparato sin solapamientos con depurador, pausa y letras grandes", () -> {
            float[] anchos = {1600f, 1280f, 1024f};
            for (float ancho : anchos) {
                for (boolean dep : new boolean[]{false, true}) {
                    float der = ancho - (dep ? 460f : 0f);
                    if (der < 560f) continue;
                    for (boolean pausa : new boolean[]{false, true}) {
                        Marcadores.BarraInferior b = Marcadores.calcularBarraInferior(900f, der, pausa, 138f, 136f, 196f, 168f, true, 340f);
                        cierto(b.finIzq() <= b.xVisor(), "izquierda no pisa Visor en der=" + der + " pausa=" + pausa);
                        cierto(b.xVisor() + b.wVisor() <= b.xDepurador(), "Visor no pisa Depurador en der=" + der);
                        cierto(b.xDepurador() + b.wDepurador() <= b.xRendirse(), "Depurador no pisa Rendirse en der=" + der);
                        cierto(b.xRendirse() + b.wRendirse() <= der - 19f, "Rendirse dentro del borde derecho en der=" + der);
                        if (b.aparatoArriba()) {
                            cierto(b.yAparato() + PanelDelAparato.ALTO_BOTON < b.y(), "aparato en fila superior no pisa botones");
                            cierto(b.xAparatoIzq() >= 20f && b.xAparatoDer() <= der - 20f, "aparato arriba dentro de la zona");
                        } else {
                            cierto(b.xAparatoIzq() >= b.finIzq(), "aparato en fila no pisa controles izquierdos en der=" + der);
                            cierto(b.xAparatoDer() <= b.xVisor(), "aparato en fila no pisa Visor en der=" + der);
                            cierto(b.xAparatoDer() - b.xAparatoIzq() >= PanelDelAparato.ANCHO_BOTON_MIN, "ancho mínimo del botón del aparato");
                        }
                    }
                }
            }
        });
    }

    private static void panelDelAparatoYMarcadores() {
        caso("hud pelea: marcadores 1v1, varios y modal del aparato sin solapamientos en ES/DE/EN", () -> {
            for (float ancho : new float[]{1600f, 1280f, 1024f}) {
                Marcadores.GeometriaDos g = Marcadores.geometriaDos(ancho);
                cierto(g.xIzq() + g.anchoBloque() < g.centroX(), "bloque izquierdo no pisa el centro en " + ancho);
                cierto(g.centroX() + g.centroAncho() < g.xDer(), "centro no pisa el bloque derecho en " + ancho);
                cierto(g.xDer() + g.anchoBloque() <= ancho - 19f, "bloque derecho dentro de pantalla en " + ancho);
                cierto(g.pasoParte() * 3f + g.anchoParte() <= g.anchoBloque() + 0.5f, "4 barras de partes caben en el bloque");
                for (int n : new int[]{3, 4, 6, 8}) {
                    Marcadores.GeometriaVarios gv = Marcadores.geometriaVarios(ancho, n);
                    float finTarjetas = 16f + (n - 1) * gv.anchoPaso() + gv.anchoTarjeta();
                    cierto(finTarjetas <= gv.xReloj(), "tarjetas de " + n + " robots no pisan reloj en " + ancho);
                    cierto(gv.anchoTarjeta() < gv.anchoPaso(), "tarjetas no se pisan entre sí");
                }
                PanelDelAparato.CajaModal m = PanelDelAparato.cajaModal(ancho, 900f);
                cierto(m.y() >= Marcadores.ALTO + 8f, "modal del aparato debajo del marcador");
                cierto(m.y() + m.h() <= 900f - 70f, "modal del aparato encima de la barra inferior");
                cierto(m.x() >= 12f && m.x() + m.w() <= ancho - 12f, "modal dentro del ancho");
            }
            Idioma antes = T.idioma;
            try {
                for (Idioma id : Idioma.values()) {
                    T.idioma = id;
                    for (Aparatos.Tipo t : Aparatos.Tipo.values()) {
                        String txt = PanelDelAparato.textoBoton(t);
                        cierto(txt.contains(t.nombre), "texto de botón incluye nombre del aparato en " + id);
                    }
                }
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static void bandaDelLocutorYTecho() {
        caso("hud pelea: banda del locutor calcula techo exacto y respeta el panel de reparación", () -> {
            Marcadores.GeometriaBanda oculta = Marcadores.geometriaBanda(1600f, false, false, 40f);
            falso(oculta.visible(), "banda oculta");
            cierto(oculta.techo() == 0f, "sin banda el techo es 0");
            Marcadores.GeometriaBanda normal = Marcadores.geometriaBanda(1140f, true, false, 90f);
            cierto(normal.visible(), "banda visible");
            cierto(normal.y() >= Marcadores.ALTO + 8f, "banda por debajo del marcador superior");
            cierto(normal.techo() >= normal.y() + normal.h() + 8f, "techo deja margen debajo de la banda");
            cierto(normal.x() >= 30f && normal.x() + normal.w() <= 1140f - 29f, "banda dentro de la zona con depurador");
            Marcadores.GeometriaBanda conRep = Marcadores.geometriaBanda(1140f, true, true, 70f);
            cierto(conRep.x() >= 282f, "banda no tapa el panel de reparación a la izquierda");
            cierto(conRep.x() + conRep.w() <= 1140f - 29f, "banda con reparación dentro de la zona");
        });
    }
}
