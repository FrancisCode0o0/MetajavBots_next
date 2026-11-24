package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.progreso.Etiqueta;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * ALARMA DE BAJADA — Evento visual luminoso en rojo (borde/baliza pulsante breve,
 * sin bloquear los controles) y aviso explícito con transición de etiqueta cuando
 * el jugador baja de nivel tras varias derrotas seguidas.
 */
public final class AlarmaBajada {

    /** Duración del destello/banner en segundos al volver al menú. */
    public static final float DURACION_MENU = 6.5f;
    private static final int ROJO_VIVO = 0xFF3333;
    private static final int ROJO_CLARO = 0xFF5C5C;

    private Etiqueta antes, despues;
    private float tiempo;
    private boolean activa, pendienteEnMenu;

    /** Activa la alarma visual al detectarse una bajada de nivel. */
    public void activar(Etiqueta etiquetaAntes, Etiqueta etiquetaDespues) {
        if (etiquetaAntes == null || etiquetaDespues == null || etiquetaAntes == etiquetaDespues) return;
        antes = etiquetaAntes;
        despues = etiquetaDespues;
        tiempo = 0;
        activa = true;
        pendienteEnMenu = true;
    }

    /** Al entrar al menú tras una bajada, relanza el destello breve en el menú una sola vez. */
    public void alEntrarAlMenu() {
        if (!pendienteEnMenu) return;
        pendienteEnMenu = false;
        tiempo = 0;
        activa = true;
    }

    /** Desactiva la alarma manualmente (al cerrarla el jugador o al empezar otra partida). */
    public void cancelar() {
        activa = false;
        pendienteEnMenu = false;
    }

    public boolean activa() {
        return activa;
    }

    public boolean pendienteEnMenu() {
        return pendienteEnMenu;
    }

    public Etiqueta antes() {
        return antes;
    }

    public Etiqueta despues() {
        return despues;
    }

    /** Transición de la etiqueta anterior a la nueva (p. ej. «Junior II ↓ Junior I»). */
    public String transicion() {
        if (antes == null || despues == null) return "";
        return antes.nombre + " ↓ " + despues.nombre;
    }

    /** Mensaje explícito de bajada de nivel en el idioma indicado. */
    public static String mensaje(Idioma idioma) {
        return T.en("final.bajas_aviso", idioma);
    }

    /**
     * Dibuja la alarma luminosa roja pulsante en los bordes de la pantalla.
     * Nunca bloquea los controles y se atenúa suavemente para no resultar molesta.
     */
    public void dibujarBordeLuminoso(Pincel p, float dt, boolean mantenerSuave) {
        if (!activa) return;
        tiempo += dt;
        float envolvente;
        if (mantenerSuave) {
            envolvente = tiempo < 4.5f ? 1f : Math.max(0.35f, 1f - (tiempo - 4.5f) * 0.35f);
        } else {
            if (tiempo >= DURACION_MENU) {
                activa = false;
                return;
            }
            envolvente = tiempo > DURACION_MENU - 1.5f ? Math.max(0, (DURACION_MENU - tiempo) / 1.5f) : 1f;
        }
        float pulso = 0.45f + 0.55f * (float) Math.abs(Math.sin(tiempo * 4.2f));
        float fuerza = envolvente * pulso;
        // Tres capas de resplandor rojo perimetral (baliza luminosa no invasiva).
        dibujarMarcoBorde(p, 18, ROJO_VIVO, 0.14f * fuerza);
        dibujarMarcoBorde(p, 9, ROJO_VIVO, 0.28f * fuerza);
        dibujarMarcoBorde(p, 3, ROJO_CLARO, 0.72f * fuerza);
        // Balizas en las cuatro esquinas.
        float r = 7 + 4 * pulso;
        p.brillo(22, 22, r * 2.2f, ROJO_VIVO, 0.55f * fuerza);
        p.brillo(p.ancho - 22, 22, r * 2.2f, ROJO_VIVO, 0.55f * fuerza);
        p.brillo(22, p.alto - 22, r * 2.2f, ROJO_VIVO, 0.55f * fuerza);
        p.brillo(p.ancho - 22, p.alto - 22, r * 2.2f, ROJO_VIVO, 0.55f * fuerza);
    }

    private static void dibujarMarcoBorde(Pincel p, float grosor, int color, float alfa) {
        if (alfa <= 0.005f) return;
        p.rect(0, 0, p.ancho, grosor, color, alfa);
        p.rect(0, p.alto - grosor, p.ancho, grosor, color, alfa);
        p.rect(0, grosor, grosor, p.alto - grosor * 2, color, alfa);
        p.rect(p.ancho - grosor, grosor, grosor, p.alto - grosor * 2, color, alfa);
    }

    /**
     * Aviso completo dentro de la tarjeta de resultado de PantallaPelea:
     * baliza roja, transición «Anterior ↓ Nueva» en rojo y mensaje explícito.
     * Devuelve el alto ocupado.
     */
    public float dibujarAvisoEnTarjeta(Pincel p, Ui ui, float x, float y, float w) {
        boolean claro = ui.tema == Tema.CLARO;
        int fondo = claro ? 0xFDECEC : Pincel.oscurecer(ROJO_VIVO, 0.76f);
        int borde = claro ? 0xC62828 : ROJO_CLARO;
        int colorTransicion = claro ? 0xB71C1C : 0xFF7B7B;
        int colorTexto = claro ? 0x5C1212 : 0xFFD9D9;
        float pulso = 0.5f + 0.5f * (float) Math.abs(Math.sin(tiempo * 4.5f));
        String titulo = "⚠  " + T.t("final.bajas_titulo");
        String trans = transicion();
        String aviso = T.t("final.bajas_aviso");
        float tamTit = ui.cabe(titulo, w - 64, ui.letra * 0.92f, true);
        float tamTrans = ui.cabe(trans, w - 48, ui.letra * 1.25f, true);
        float tamAviso = Math.min(22f, ui.letra * 0.88f);
        float hTit = p.altoDeLinea(tamTit);
        float hTrans = p.altoDeLinea(tamTrans);
        float hAviso = p.altoDeParrafo(aviso, w - 40, tamAviso, false);
        float h = 14 + hTit + 4 + hTrans + 6 + hAviso + 14;
        p.caja(x, y, w, h, 12, fondo, 1f);
        p.bordeRedondeado(x, y, w, h, 12, borde, 0.65f + 0.35f * pulso, 2.5f);
        // Baliza luminosa izquierda.
        p.brillo(x + 22, y + 14 + hTit * 0.5f, 10 + 4 * pulso, ROJO_VIVO, 0.6f * pulso);
        p.circulo(x + 22, y + 14 + hTit * 0.5f, 5.5f, ROJO_CLARO, 0.7f + 0.3f * pulso);
        float yy = y + 12;
        p.textoCentrado(titulo, x + w / 2, yy, tamTit, true, borde, 1);
        yy += hTit + 4;
        p.textoCentrado(trans, x + w / 2, yy, tamTrans, true, colorTransicion, 1);
        yy += hTrans + 6;
        p.parrafo(aviso, x + 20, yy, w - 40, tamAviso, false, colorTexto, 1);
        return h;
    }

    /**
     * Al volver al menú tras una bajada: borde rojo pulsante breve, halo rojo sobre
     * la insignia de rango en la cabecera y aviso superior no bloqueante descartable.
     */
    public void dibujarBannerEnMenu(Juego j, float arriba, float dt) {
        if (!activa) return;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        dibujarBordeLuminoso(p, dt, false);
        if (!activa) return;
        float alfa = tiempo > DURACION_MENU - 1.2f ? Math.max(0, (DURACION_MENU - tiempo) / 1.2f) : 1f;
        float pulso = 0.5f + 0.5f * (float) Math.abs(Math.sin(tiempo * 4.5f));
        // Resalta en rojo la nueva insignia del jugador en la cabecera.
        p.bordeRedondeado(p.ancho - 464, 10, 144, 58, 12, ROJO_CLARO, alfa * (0.5f + 0.5f * pulso), 3.5f);
        boolean claro = ui.tema == Tema.CLARO;
        int fondo = claro ? 0xFDECEC : Pincel.oscurecer(ROJO_VIVO, 0.78f);
        int borde = claro ? 0xC62828 : ROJO_CLARO;
        int colorTrans = claro ? 0xB71C1C : 0xFF7B7B;
        int colorTexto = claro ? 0x5C1212 : 0xFFE2E2;
        float bw = Math.min(720, p.ancho - 480);
        String cabecera = "⚠  " + T.t("final.bajas_titulo") + "  ·  " + transicion();
        String aviso = T.t("final.bajas_aviso");
        float tamCab = ui.cabe(cabecera, bw - 88, Math.min(22f, ui.letra * 0.98f), true);
        float tamAvi = Math.min(19f, ui.letra * 0.82f);
        float hCab = p.altoDeLinea(tamCab);
        float hAvi = p.altoDeParrafo(aviso, bw - 84, tamAvi, false);
        float bh = 12 + hCab + 4 + hAvi + 12;
        float bx = p.ancho - bw - 40;
        float by = Math.max(arriba + 12, p.alto - bh - 36);
        p.caja(bx + 3, by + 4, bw, bh, 12, 0x000000, 0.35f * alfa);
        p.caja(bx, by, bw, bh, 12, fondo, alfa);
        p.bordeRedondeado(bx, by, bw, bh, 12, borde, alfa * (0.65f + 0.35f * pulso), 2.5f);
        p.brillo(bx + 24, by + bh / 2, 12 + 4 * pulso, ROJO_VIVO, 0.65f * alfa * pulso);
        p.circulo(bx + 24, by + bh / 2, 6, ROJO_CLARO, alfa);
        p.texto(cabecera, bx + 44, by + 10, tamCab, true, colorTrans, alfa);
        p.parrafo(aviso, bx + 44, by + 10 + hCab + 4, bw - 84, tamAvi, false, colorTexto, alfa);
        if (ui.boton("✕", bx + bw - 38, by + 8, 30, 30, Ui.Estilo.PLANO)) cancelar();
    }
}
