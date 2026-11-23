package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.combate.Aparatos;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.textos.T;

/**
 * PANEL DEL APARATO — En la pelea, el botón del aparato de la ronda y, al
 * pulsarlo, su código (de solo lectura) en grande: a qué hueco se conecta y
 * cómo se usa.
 */
public final class PanelDelAparato {

    /** Altura estándar del botón del aparato en la barra inferior. */
    public static final float ALTO_BOTON = 46f;
    /** Ancho máximo del botón del aparato. */
    public static final float ANCHO_BOTON_MAX = 360f;
    /** Ancho mínimo razonable del botón del aparato antes de saltar de fila. */
    public static final float ANCHO_BOTON_MIN = 150f;

    /** Geometría del cuadro modal de código del aparato, libre del marcador superior y la barra inferior. */
    public record CajaModal(float x, float y, float w, float h) {
    }

    private boolean verAparato;
    private String codigoDelAparato = "";
    private final VentanaDeCodigo ventanaDelAparato = new VentanaDeCodigo();

    /** ¿Está abierta la ventana del aparato? */
    public boolean estaAbierto() {
        return verAparato;
    }

    /** Uno con el código ya abierto (para las capturas). */
    public static PanelDelAparato abierto() {
        PanelDelAparato p = new PanelDelAparato();
        p.verAparato = true;
        return p;
    }

    /** Calcula la caja del panel del aparato acoplada a la derecha, respetando el marcador superior (88 px) y la barra inferior. */
    public static CajaModal cajaModal(float pantallaAncho, float pantallaAlto) {
        float w = Math.min(460f, Math.max(320f, pantallaAncho * 0.36f));
        float x = Math.max(12f, pantallaAncho - w - 16f);
        float y = Marcadores.ALTO + 12f;
        float abajoLibre = Math.max(y + 180f, pantallaAlto - 78f);
        float h = Math.max(180f, Math.min(pantallaAlto - 178f, abajoLibre - y));
        return new CajaModal(x, y, w, h);
    }

    /** Texto visible del botón del aparato para el idioma activo. */
    public static String textoBoton(Aparatos.Tipo tipo) {
        return tipo == null ? "" : "▣ " + T.f("aparato.boton", tipo.nombre);
    }

    /** Ancho deseado del botón del aparato según la medición del texto con la letra real de botón. */
    public static float anchoBotonDeseado(Pincel p, Ui ui, Aparatos.Tipo tipo) {
        if (tipo == null) return 0f;
        float tBtn = Math.min(ui.letra, ALTO_BOTON * 0.5f);
        return Math.min(ANCHO_BOTON_MAX, Math.max(ANCHO_BOTON_MIN, p.ancho(textoBoton(tipo), tBtn, true) + 32f));
    }

    /**
     * El aparato de esta ronda (o de la siguiente): un botón con su nombre que abre su código, para leer cómo se
     * conecta. Devuelve si hay que poner la pelea en pausa (al abrirlo).
     */
    public boolean dibujar(Juego j, Aparatos.Tipo tipo, float derecha, float y) {
        return dibujar(j, tipo, 20f, derecha, y);
    }

    /**
     * Dibuja el botón del aparato dentro del rango horizontal [izquierda, derecha] sin invadir otros botones,
     * y si está abierto, su ventana modal de código de solo lectura.
     */
    public boolean dibujar(Juego j, Aparatos.Tipo tipo, float izquierda, float derecha, float y) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        if (tipo == null) return false;
        boolean pausar = false;
        String texto = textoBoton(tipo);
        float maxDisp = Math.max(80f, derecha - izquierda);
        float bw = Math.min(maxDisp, anchoBotonDeseado(p, ui, tipo));
        float bx = Math.max(izquierda, derecha - bw);
        if (ui.boton(texto, bx, y, bw, ALTO_BOTON, verAparato ? Ui.Estilo.ELEGIDO : Ui.Estilo.NORMAL, true,
                T.t("aparato.boton_ayuda"))) {
            verAparato = !verAparato;
            pausar = verAparato;
        }
        if (!verAparato) return pausar;
        if (!codigoDelAparato.equals(tipo.id())) {
            codigoDelAparato = tipo.id();
            ventanaDelAparato.ponerTexto(CodigoDeAparato.texto(tipo.id(), j.jugador.idioma));
            ventanaDelAparato.soloLectura = true;
        }
        CajaModal c = cajaModal(p.ancho, p.alto);
        String titulo = T.f("aparato.titulo", tipo.nombre);
        float tamTit = ui.cabe(titulo, c.w() - 32f, ui.letra * 1.05f, true);
        String tituloVis = ui.recortarTexto(titulo, c.w() - 32f, tamTit, true);
        float cy = ui.panel(c.x(), c.y(), c.w(), c.h(), tituloVis);
        ventanaDelAparato.tamano = j.jugador.tamanoDeLetra;
        float altoCodigo = Math.max(80f, c.y() + c.h() - cy - 70f);
        ventanaDelAparato.dibujar(ui, c.x() + 12f, cy, c.w() - 24f, altoCodigo);
        String cerrar = T.t("aparato.cerrar");
        float tCerrar = Math.min(ui.letra, 48f * 0.5f);
        float wCerrar = Math.min(c.w() - 32f, Math.max(160f, p.ancho(cerrar, tCerrar, true) + 36f));
        if (ui.boton(cerrar, c.x() + c.w() - wCerrar - 16f, c.y() + c.h() - 58f, wCerrar, 48f, Ui.Estilo.PRINCIPAL) || ui.atras()) {
            verAparato = false;
        }
        ui.reservarEscape();
        return pausar;
    }
}
