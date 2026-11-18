package metajavbots.pantallas;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * GUÍA DE LOS CEREBROS — El entrenador enseña, paso a paso y con dibujos,
 * a quien no sabe nada: qué es un cerebro de IA, cómo poner uno local
 * (KoboldCpp y un modelo .gguf en una carpeta), cómo sacar la clave de uno
 * en línea (Google, Claude u OpenAI) y dónde ponerla, cómo llenar las tres
 * casillas, cómo despertar los cerebros cuando hace falta, y además cómo se
 * juega y dónde se ven las notas. Cada página: a la izquierda los pasos
 * (res/textos/ui.txt, «guia.N.*»), a la derecha su dibujo (ventanas del juego
 * y de Windows dibujadas a mano, con una flecha a lo que hay que pulsar).
 */
public final class GuiaDeCerebros implements Pantalla {

    /** Cuántas páginas tiene (guia.1 … guia.N en ui.txt). */
    public static final int PAGINAS = 10;

    private int pagina;
    private float t;
    private final Ui.Desplazable desp = new Ui.Desplazable();

    public GuiaDeCerebros() {
        this(0);
    }

    /** Abierta en esa página (desde 0). */
    public GuiaDeCerebros(int pagina) {
        this.pagina = Math.max(0, Math.min(PAGINAS - 1, pagina));
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        p.rect(0, 0, p.ancho, p.alto, ui.tema.fondo, 1);
        if (ui.boton("← " + T.t("comun.volver"), 20, 16, 180, 46, Ui.Estilo.PLANO) || ui.atras()) {
            j.volver();
            return;
        }
        p.texto(T.t("guia.titulo"), 220, 22, ui.letra * 1.4f, true, ui.tema.texto, 1);
        // Las páginas, como pestañas numeradas.
        float bx = 20, by = 76, bw = Math.min(150, (p.ancho - 40) / PAGINAS - 6);
        for (int i = 0; i < PAGINAS; i++) {
            String n = (i + 1) + ". " + T.t("guia." + (i + 1) + ".corto");
            if (ui.boton(ui.recortarTexto(n, bw - 12, ui.letra * 0.7f, false), bx + i * (bw + 6), by, bw, 40,
                    i == pagina ? Ui.Estilo.ELEGIDO : Ui.Estilo.PLANO, true, T.t("guia." + (i + 1) + ".titulo"))) {
                pagina = i;
                desp.alPrincipio();
            }
        }
        float y = by + 56, h = p.alto - y - 80;
        float iw = Math.min(620, p.ancho * 0.42f);
        // Los pasos, con el entrenador que los cuenta.
        Entrenador en = Entrenador.de(j.jugador == null ? "tuercas" : j.jugador.entrenador);
        CabezaDeEntrenador.dibujar(p, en, 56, y + 34, 30, false, t);
        p.parrafo(T.t("guia." + (pagina + 1) + ".titulo"), 100, y + 12, iw - 90, ui.letra * 1.2f, true, ui.tema.acento, 1);
        float d = ui.desplazable(desp, 20, y + 80, iw, h - 80);
        float yy = y + 80 - d;
        int k = 1;
        for (String paso : T.t("guia." + (pagina + 1) + ".pasos").split("\n")) {
            if (paso.isBlank()) continue;
            p.textoDerecha((k++) + ".", 52, yy, ui.letra * 0.95f, true, ui.tema.acento, 1);
            yy += p.parrafo(paso, 60, yy, iw - 50, ui.letra * 0.95f, ui.tema.texto, 1) + ui.letra * 0.7f;
        }
        ui.finDesplazable(desp, yy + d - (y + 80));
        // El dibujo de la página.
        float dx = iw + 50, dw = p.ancho - dx - 30;
        dibujo(j, pagina, dx, y, dw, h);
        if (pagina > 0 && ui.boton("◄ " + T.t("guia.anterior"), 20, p.alto - 66, 220, 50, Ui.Estilo.NORMAL)) {
            pagina--;
            desp.alPrincipio();
        }
        if (pagina < PAGINAS - 1 && ui.boton(T.t("guia.siguiente") + " ►", p.ancho - 240, p.alto - 66, 220, 50, Ui.Estilo.PRINCIPAL)) {
            pagina++;
            desp.alPrincipio();
        }
    }

    // ================================================================ los dibujos

    private void dibujo(Juego j, int n, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        switch (n) {
            case 0 -> tresCasillas(p, x, y, w, h, -1);
            case 1 -> navegador(p, x, y, w, h, "github.com/LostRuins/koboldcpp/releases", new String[]{"koboldcpp.exe", "koboldcpp_nocuda.exe",
                "koboldcpp-linux-x64"}, 0);
            case 2 -> navegador(p, x, y, w, h, "huggingface.co/bartowski/google_gemma-3-4b-it-GGUF", new String[]{
                "google_gemma-3-4b-it-Q4_K_M.gguf   2,5 GB", "google_gemma-3-4b-it-Q8_0.gguf   4,1 GB", "google_gemma-3-4b-it-f16.gguf   7,8 GB"}, 0);
            case 3 -> carpeta(p, x, y, w, h);
            case 4 -> panelDeCerebros(p, x, y, w, h, false);
            case 5 -> navegador(p, x, y, w, h, "aistudio.google.com/apikey", new String[]{"Create API key", "AIza••••••••••••••••••", "Copy"}, 0);
            case 6 -> panelDeCerebros(p, x, y, w, h, true);
            case 7 -> despertar(p, x, y, w, h);
            case 8 -> menu(p, x, y, w, h, new String[]{"menu.taller", "menu.pelear", "menu.ejercicios", "menu.tutoriales"}, 0);
            default -> menu(p, x, y, w, h, new String[]{"menu.historial", "menu.ejercicios", "menu.temario", "menu.ajustes"}, 0);
        }
    }

    /** Una ventana con su barra de título (con los tres botones de Windows); devuelve dónde empieza lo de dentro. */
    private float ventana(Pincel p, float x, float y, float w, float h, String titulo) {
        p.caja(x + 5, y + 7, w, h, 10, 0x000000, 0.35f);
        p.caja(x, y, w, h, 10, 0xF3F4F6, 1);
        p.caja(x, y, w, 34, 10, 0xD9DDE3, 1);
        p.rect(x, y + 24, w, 10, 0xD9DDE3, 1);
        p.texto(titulo, x + 12, y + 7, 15, true, 0x2B2F36, 1);
        p.texto("–  □  ✕", x + w - 86, y + 7, 15, false, 0x2B2F36, 1);
        return y + 44;
    }

    /** Una flecha amarilla que se mueve un poco, apuntando a (ax, ay) desde la izquierda. */
    private void flecha(Pincel p, float ax, float ay) {
        float o = (float) Math.sin(t * 5) * 6;
        p.linea(ax - 70 + o, ay + 30, ax - 12 + o, ay + 4, 6, 0xFFD23F, 1);
        p.triangulo(ax - 4 + o, ay, ax - 22 + o, ay - 4, ax - 12 + o, ay + 14, 0xFFD23F, 1);
    }

    /** Un botón dibujado (no se pulsa: es parte del dibujo). */
    private void botonDibujado(Pincel p, String texto, float x, float y, float w, float h, int color, int letra) {
        p.caja(x, y, w, h, 6, color, 1);
        p.textoCentrado(texto, x + w / 2, y + (h - 16) / 2, 15, true, letra, 1);
    }

    private void navegador(Pincel p, float x, float y, float w, float h, String direccion, String[] filas, int marcada) {
        float vh = Math.min(h, 420);
        float cy = ventana(p, x, y, w, vh, "Navegador");
        p.caja(x + 12, cy, w - 24, 32, 16, 0xFFFFFF, 1);
        p.texto("https://" + direccion, x + 26, cy + 7, 14, false, 0x3A3F46, 1);
        cy += 52;
        for (int i = 0; i < filas.length; i++) {
            boolean es = i == marcada;
            p.caja(x + 24, cy, w - 48, 44, 6, es ? 0xDDEBFF : 0xFFFFFF, 1);
            p.bordeRedondeado(x + 24, cy, w - 48, 44, 6, es ? 0x2F80ED : 0xC9CED6, 1, es ? 2 : 1);
            p.texto((i == 0 ? "↓ " : "   ") + filas[i], x + 40, cy + 13, 15, es, 0x2B2F36, 1);
            if (es) flecha(p, x + 24, cy + 22);
            cy += 56;
        }
    }

    private void carpeta(Pincel p, float x, float y, float w, float h) {
        float cy = ventana(p, x, y, w, Math.min(h, 400), "C:\\…\\000KI_Local");
        String[][] filas = {{"[ ]", "piper"}, {" ▶ ", "koboldcpp.exe"}, {" ◆ ", "google_gemma-3-4b-it-Q4_K_M.gguf"}, {" ◆ ", "Qwen_Qwen3.5-9B-Q4_K_M.gguf"}};
        for (String[] f : filas) {
            p.texto(f[0] + "  " + f[1], x + 30, cy + 8, 16, false, 0x2B2F36, 1);
            cy += 40;
        }
        p.caja(x + 20, cy + 10, w - 40, 54, 8, 0xFFF4C2, 1);
        p.parrafo(T.t("guia.dibujo.carpeta"), x + 32, cy + 18, w - 64, 14, 0x3A3F46, 1);
    }

    /** Ajustes › Cerebros de IA: las tres pestañas, los tipos, el modelo o la clave, y «Probar». */
    private void panelDeCerebros(Pincel p, float x, float y, float w, float h, boolean enLinea) {
        float cy = ventana(p, x, y, w, Math.min(h, 460), "MetajavBots — " + T.t("ajustes.cerebro"));
        float tw = (w - 48) / 3;
        for (int i = 0; i < 3; i++) {
            String tipo = T.t(i == 0 ? "cerebro.local" : enLinea && i == 1 ? "cerebro.en_linea" : i == 2 && enLinea ? "cerebro.local" : "cerebro.ninguno");
            botonDibujado(p, T.f("cerebros.pestana", i + 1, tipo), x + 16 + i * (tw + 8), cy, tw, 38, i == (enLinea ? 1 : 0) ? 0x7B61FF : 0xC9CED6,
                    i == (enLinea ? 1 : 0) ? 0xFFFFFF : 0x2B2F36);
        }
        cy += 54;
        String[] tipos = {"cerebro.ninguno", "cerebro.local", "cerebro.en_linea"};
        for (int k = 0; k < 3; k++) {
            boolean es = k == (enLinea ? 2 : 1);
            botonDibujado(p, T.t(tipos[k]), x + 16 + k * (tw + 8), cy, tw, 38, es ? 0x2F80ED : 0xE3E6EA, es ? 0xFFFFFF : 0x2B2F36);
        }
        cy += 56;
        if (enLinea) {
            String[] prov = {"anthropic", "openai", "gemini"};
            for (int k = 0; k < 3; k++) botonDibujado(p, prov[k], x + 16 + k * (tw + 8), cy, tw, 36, k == 2 ? 0x2F80ED : 0xE3E6EA, k == 2 ? 0xFFFFFF : 0x2B2F36);
            cy += 52;
            p.caja(x + 16, cy, w - 200, 40, 6, 0xFFFFFF, 1);
            p.texto("AIza••••••••••••", x + 28, cy + 11, 15, false, 0x3A3F46, 1);
            botonDibujado(p, T.t("ajustes.guardar_clave"), x + w - 176, cy, 160, 40, 0x5BD685, 0x10161C);
            flecha(p, x + w - 176, cy + 20);
        } else {
            botonDibujado(p, "◄", x + 16, cy, 44, 40, 0xE3E6EA, 0x2B2F36);
            p.caja(x + 66, cy, w - 148, 40, 6, 0xFFFFFF, 1);
            p.texto("google_gemma-3-4b-it-Q4_K_M", x + 78, cy + 11, 15, false, 0x2B2F36, 1);
            botonDibujado(p, "►", x + w - 76, cy, 44, 40, 0xE3E6EA, 0x2B2F36);
            flecha(p, x + 16, cy + 20);
        }
        cy += 60;
        botonDibujado(p, T.f("cerebros.probar", enLinea ? 2 : 1), x + 16, cy, Math.min(320, w - 32), 44, 0xFF7A45, 0xFFFFFF);
    }

    /** El cerebro dormido (Zzz), el que se despierta (barra de carga) y el despierto (contesta). */
    private void despertar(Pincel p, float x, float y, float w, float h) {
        float cw = (w - 40) / 3, cy = y + 40;
        String[] rotulos = {T.t("guia.dibujo.dormido"), T.t("guia.dibujo.despertando"), T.t("guia.dibujo.despierto")};
        for (int i = 0; i < 3; i++) {
            float cx = x + i * (cw + 20) + cw / 2;
            p.caja(cx - cw / 2, cy, cw, 230, 14, 0x1B2430, 1);
            // Un cerebro: dos lóbulos y unas arrugas.
            int color = i == 0 ? 0x8A7F9A : i == 1 ? 0xC79BD6 : 0xF29BD0;
            p.circulo(cx - 22, cy + 90, 34, color, 1);
            p.circulo(cx + 22, cy + 90, 34, color, 1);
            p.linea(cx, cy + 60, cx, cy + 122, 3, 0x5B4A6A, 1);
            if (i == 0) p.texto("Z z z", cx + 30, cy + 30 - (t * 8) % 20, 18, true, 0xBFD7E3, 1);
            if (i == 1) {
                float carga = (t * 0.3f) % 1f;
                p.caja(cx - cw / 2 + 20, cy + 150, cw - 40, 14, 7, 0x10161C, 1);
                p.caja(cx - cw / 2 + 20, cy + 150, (cw - 40) * carga, 14, 7, 0xFFD23F, 1);
            }
            if (i == 2) {
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + t;
                    p.linea(cx + (float) Math.cos(a) * 50, cy + 90 + (float) Math.sin(a) * 50, cx + (float) Math.cos(a) * 62,
                            cy + 90 + (float) Math.sin(a) * 62, 3, 0xFFD23F, 1);
                }
            }
            p.parrafo(rotulos[i], cx - cw / 2 + 12, cy + 176, cw - 24, 14, 0xE8E2C8, 1);
        }
        p.parrafo(T.t("guia.dibujo.memoria"), x, cy + 260, w, 15, 0xBFC5CE, 1);
    }

    /** El menú del juego, con la flecha en la opción que toca. */
    private void menu(Pincel p, float x, float y, float w, float h, String[] opciones, int marcada) {
        float cy = ventana(p, x, y, w, Math.min(h, 60 + opciones.length * 66.0f), "MetajavBots — Menú");
        for (int i = 0; i < opciones.length; i++) {
            boolean es = i == marcada;
            botonDibujado(p, T.t(opciones[i]), x + 30, cy, w - 60, 50, es ? 0x7B61FF : 0x3A3F46, 0xFFFFFF);
            p.texto(T.t(opciones[i] + "_ayuda"), x + 30, cy + 52, 12, false, 0x5A606A, 1);
            if (es) flecha(p, x + 30, cy + 25);
            cy += 66;
        }
    }

    /** La de las tres casillas: un portátil (local) y dos nubes (en línea), cualquier mezcla. */
    private void tresCasillas(Pincel p, float x, float y, float w, float h, int marcada) {
        float cw = (w - 40) / 3, cy = y + 30;
        String[] tipos = {T.t("cerebro.local"), T.t("cerebro.en_linea"), T.t("cerebro.en_linea")};
        for (int i = 0; i < 3; i++) {
            float cx = x + i * (cw + 20) + cw / 2;
            p.caja(cx - cw / 2, cy, cw, 220, 14, 0x1B2430, 1);
            p.textoCentrado(T.f("cerebros.pestana", i + 1, tipos[i]), cx, cy + 14, 16, true, 0xFFFFFF, 1);
            if (i == 0) {
                p.caja(cx - 50, cy + 70, 100, 64, 6, 0x3A3F46, 1);
                p.caja(cx - 44, cy + 76, 88, 52, 4, 0x3FE0D0, 0.8f);
                p.caja(cx - 64, cy + 134, 128, 10, 4, 0x8A9199, 1);
            } else {
                int c = 0xE8ECEF;
                p.circulo(cx - 28, cy + 110, 26, c, 1);
                p.circulo(cx + 6, cy + 96, 34, c, 1);
                p.circulo(cx + 38, cy + 112, 24, c, 1);
                p.rect(cx - 28, cy + 110, 66, 26, c, 1);
            }
            float pulso = (float) (0.5 + 0.5 * Math.sin(t * 3 + i));
            p.circulo(cx, cy + 186, 8, 0x5BD685, 0.4f + 0.6f * pulso);
        }
        p.parrafo(T.t("guia.dibujo.mezcla"), x, cy + 250, w, 16, 0xBFC5CE, 1);
    }
}
