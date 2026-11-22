package metajavbots.pantallas;

import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;

/**
 * GEOMETRÍA DEL ENTRENADOR — Calcula cajas seguras en la esquina inferior
 * derecha para el entrenador recogido (flechita), el entrenador desplegado
 * (cabecita completa con botón de recoger) y el cartelito de consejos pegado a él,
 * garantizando ausencia de solapamientos y límites seguros en cualquier tamaño de
 * pantalla y de letra (incluyendo -Pletra=26).
 */
public final class GeometriaEntrenador {

    /** Márgenes y tamaño del botón «×» en la esquina superior derecha del bocadillo de tips. */
    public static final float MARGEN_TEXTO_IZQ = 12f;
    public static final float BOTON_CERRAR_TAM = 24f;
    /** Margen vertical mínimo entre el bocadillo de tips y los bordes superior e inferior de la pantalla. */
    public static final float MARGEN_VERTICAL_TIP = 16f;
    /** Margen seguro respecto al borde derecho y al borde inferior de la pantalla. */
    public static final float MARGEN_ESQUINA_DER = 8f;
    public static final float MARGEN_ESQUINA_INF = 16f;
    /** Separación horizontal entre el cartelito de consejo y la cabecita desplegada (espacio del piquito). */
    public static final float SEPARACION_TIP_CABEZA = 14f;
    /** Ancho de la franja derecha de la pestaña desplegada reservada a la flecha inversa para recoger. */
    public static final float ANCHO_BOTON_RECOGER = 24f;

    static final float BOTON_CERRAR_MARGEN = 6f;
    static final float MARGEN_TEXTO_DER = BOTON_CERRAR_TAM + BOTON_CERRAR_MARGEN + 12f;
    static final float RESERVA_VERTICAL_TIP = 70f;

    private GeometriaEntrenador() {
    }

    /** Calcula la pestaña recogida en la esquina inferior derecha: flechita «◀» y cabecita medio asomada. */
    public static EntrenadorFlotante.CajaRecogida cajaRecogida(float pantallaAncho, float pantallaAlto, float tamanoLetra) {
        float r = radioCabeza(tamanoLetra);
        float anchoFlecha = 18f;
        float anchoCabeza = Math.round(r * 2.15f);
        float w = anchoFlecha + anchoCabeza;
        float h = Math.round(r * 1.65f);
        float x = Math.max(0f, pantallaAncho - w - MARGEN_ESQUINA_DER);
        float y = Math.max(0f, pantallaAlto - h);
        float flechaX = x + anchoFlecha * 0.52f;
        float flechaY = y + h * 0.52f;
        float cabezaX = x + anchoFlecha + anchoCabeza * 0.48f;
        float cabezaY = y + h - r * 0.05f;
        return new EntrenadorFlotante.CajaRecogida(x, y, w, h, flechaX, flechaY, cabezaX, cabezaY, r);
    }

    /** Calcula la pestaña desplegada con la cabecita completa en la esquina inferior derecha y la flecha para recoger. */
    public static EntrenadorFlotante.CajaPestana cajaPestana(float pantallaAncho, float pantallaAlto, float asomada, float tamanoLetra) {
        float factor = Math.max(0f, Math.min(1f, asomada));
        float r = radioCabeza(tamanoLetra);
        float anchoCabeza = Math.round(r * 3.1f);
        float w = anchoCabeza + ANCHO_BOTON_RECOGER;
        float h = Math.round(r * 3.4f);
        float margenDer = MARGEN_ESQUINA_DER + factor * 4f;
        float x = Math.max(0f, Math.min(pantallaAncho - w, pantallaAncho - w - margenDer));
        float margenInf = margenInferior(pantallaAlto);
        float y = Math.max(margenInf, pantallaAlto - h - margenInf);
        float cabezaX = x + anchoCabeza * 0.50f;
        float cabezaY = y + h * 0.48f;
        float recogerX = x + anchoCabeza;
        float insigniaX = recogerX - 7f;
        float insigniaY = y + 6f;
        return new EntrenadorFlotante.CajaPestana(x, y, w, h, cabezaX, cabezaY, r, insigniaX, insigniaY,
                recogerX, y, ANCHO_BOTON_RECOGER, h);
    }

    private static float radioCabeza(float tamanoLetra) {
        return Math.max(20f, Math.min(24f, tamanoLetra * 0.9f));
    }

    /** Calcula la caja del cartelito pegado a la izquierda de la cabecita desplegada, siempre dentro de pantalla. */
    public static EntrenadorFlotante.CajaTip cajaTip(float pantallaAncho, float pantallaAlto, float altoTexto, float tamanoLetra) {
        EntrenadorFlotante.CajaPestana pestana = cajaPestana(pantallaAncho, pantallaAlto, 1f, tamanoLetra);
        float maxAnchoDisp = Math.max(160f, pestana.x() - SEPARACION_TIP_CABEZA - 12f);
        float w = Math.min(420f, Math.min(maxAnchoDisp, Math.max(160f, pantallaAncho * 0.38f)));
        float anchoTexto = Math.max(80f, w - MARGEN_TEXTO_IZQ - MARGEN_TEXTO_DER);
        float margenY = margenInferior(pantallaAlto);
        float altoMax = Math.max(0f, pantallaAlto - 2f * margenY);
        float hDeseado = Math.max(Math.max(0f, altoTexto), BOTON_CERRAR_TAM) + RESERVA_VERTICAL_TIP;
        float h = Math.min(hDeseado, altoMax);
        float x = Math.max(12f, pestana.x() - SEPARACION_TIP_CABEZA - w);
        float yIdeal = pestana.y() + pestana.h() - h;
        float y = Math.max(margenY, Math.min(pantallaAlto - margenY - h, yIdeal));
        float cerrarX = x + w - BOTON_CERRAR_MARGEN - BOTON_CERRAR_TAM;
        float cerrarY = y + BOTON_CERRAR_MARGEN;
        return new EntrenadorFlotante.CajaTip(x, y, w, h, anchoTexto, cerrarX, cerrarY, BOTON_CERRAR_TAM);
    }

    static float tamanoLetraTip(Pincel p, Ui ui, String texto, float anchoTexto) {
        float margenY = margenInferior(p.alto);
        float altoMaxTexto = Math.max(BOTON_CERRAR_TAM, p.alto - 2f * margenY - RESERVA_VERTICAL_TIP);
        float tl = Math.max(Pincel.LETRA_MINIMA, ui.letra * 0.8f);
        while (tl > Pincel.LETRA_MINIMA && p.altoDeParrafo(texto, anchoTexto, tl) > altoMaxTexto) {
            tl = Math.max(Pincel.LETRA_MINIMA, tl - 1f);
        }
        return tl;
    }

    static EntrenadorFlotante.CajaTip cajaTip(Pincel p, Ui ui, String texto) {
        EntrenadorFlotante.CajaPestana pestana = cajaPestana(p.ancho, p.alto, 1f, ui.letra);
        float maxAnchoDisp = Math.max(160f, pestana.x() - SEPARACION_TIP_CABEZA - 12f);
        float w = Math.min(420f, Math.min(maxAnchoDisp, Math.max(160f, p.ancho * 0.38f)));
        float anchoTexto = Math.max(80f, w - MARGEN_TEXTO_IZQ - MARGEN_TEXTO_DER);
        float tl = tamanoLetraTip(p, ui, texto, anchoTexto);
        return cajaTip(p.ancho, p.alto, p.altoDeParrafo(texto, anchoTexto, tl), ui.letra);
    }

    private static float margenInferior(float pantallaAlto) {
        return Math.min(MARGEN_VERTICAL_TIP, Math.max(0f, pantallaAlto * 0.05f));
    }
}
