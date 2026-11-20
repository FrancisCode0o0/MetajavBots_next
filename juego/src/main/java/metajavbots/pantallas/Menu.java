package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import metajavbots.Juego;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Tema;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * MENÚ — El centro del juego organizado en tres bloques con código de colores:
 * Aprender (azul/cian), Tu Robot (ámbar/dorado) y Arena (rojo/coral), más el
 * escaparate del robot actual y los accesos de configuración.
 */
public final class Menu implements Pantalla {

    private static final int COLOR_APRENDER_OSCURO = 0x38BDF8;
    private static final int COLOR_APRENDER_CLARO = 0x0B66C3;
    private static final int COLOR_ROBOT_OSCURO = 0xF5B742;
    private static final int COLOR_ROBOT_CLARO = 0x9A6400;
    private static final int COLOR_ARENA_OSCURO = 0xFF7070;
    private static final int COLOR_ARENA_CLARO = 0xC62828;

    private float t;
    private VistaDeArena vista;

    private static final class Casilla {
        final String clave, ayuda, icono, etiqueta;
        final boolean destacada;
        final Function<Juego, Pantalla> destino;

        Casilla(String clave, String ayuda, String icono, String etiqueta, boolean destacada, Function<Juego, Pantalla> destino) {
            this.clave = clave;
            this.ayuda = ayuda;
            this.icono = icono;
            this.etiqueta = etiqueta;
            this.destacada = destacada;
            this.destino = destino;
        }
    }

    private static final class Bloque {
        final String titulo;
        final int colorOscuro, colorClaro, columnas;
        final Casilla[] casillas;

        Bloque(String titulo, int colorOscuro, int colorClaro, int columnas, Casilla... casillas) {
            this.titulo = titulo;
            this.colorOscuro = colorOscuro;
            this.colorClaro = colorClaro;
            this.columnas = columnas;
            this.casillas = casillas;
        }

        int color(Tema tema) {
            return tema == Tema.CLARO ? colorClaro : colorOscuro;
        }

        int filas() {
            return (casillas.length + columnas - 1) / columnas;
        }
    }

    private static final Bloque[] BLOQUES = {
            new Bloque("menu.bloque_aprender", COLOR_APRENDER_OSCURO, COLOR_APRENDER_CLARO, 3,
                    new Casilla("menu.tutoriales", "menu.tutoriales_ayuda", "★", null, false, j -> new Tutoriales()),
                    new Casilla("mini.boton", "mini.boton_ayuda", "⚄", "menu.principiantes", true, j -> new MiniJuegos()),
                    new Casilla("demo.boton", "demo.boton_ayuda", "▶", null, false, j -> new Demostraciones()),
                    new Casilla("menu.ejercicios", "menu.ejercicios_ayuda", "✓", null, false, j -> new Ejercicios()),
                    new Casilla("sesion.boton", "sesion.boton_ayuda", "⏱", null, false, j -> new SesionDeHoy()),
                    new Casilla("menu.temario", "menu.temario_ayuda", "▤", null, false, j -> new Temario())),
            new Bloque("menu.bloque_robot", COLOR_ROBOT_OSCURO, COLOR_ROBOT_CLARO, 3,
                    new Casilla("menu.taller", "menu.taller_ayuda", "⚙", null, false, j -> new Taller()),
                    new Casilla("menu.tienda", "menu.tienda_ayuda", "◆", null, false, j -> new Tienda()),
                    new Casilla("menu.depuracion", "menu.depuracion_ayuda", "!", null, false, j -> new DepuracionYTesting())),
            new Bloque("menu.bloque_arena", COLOR_ARENA_OSCURO, COLOR_ARENA_CLARO, 4,
                    new Casilla("menu.pelear_ya", "menu.pelear_ya_ayuda", "⚔", null, true, PrepararPelea::adaptada),
                    new Casilla("menu.pelear", "menu.pelear_ayuda", "►", null, false, j -> new PrepararPelea()),
                    new Casilla("menu.especial", "menu.especial_ayuda", "◆", null, false, j -> new ModoEspecial()),
                    new Casilla("menu.historial", "menu.historial_ayuda", "▲", null, false, j -> new Historial())),
    };

    /** Claves de las casillas y accesos visibles en el menú principal. */
    public static List<String> clavesDeCasillas() {
        List<String> claves = new ArrayList<>();
        for (Bloque b : BLOQUES) {
            for (Casilla c : b.casillas) claves.add(c.clave);
        }
        claves.add("menu.panel_ia");
        claves.add("menu.ajustes");
        return claves;
    }

    @Override
    public void alEntrar(Juego j) {
        if (vista == null) vista = new VistaDeArena(j.pincel);
        j.alarmaBajada.alEntrarAlMenu();
        j.guardar();
    }

    @Override
    public void fotograma(Juego j, float dt) {
        t += dt;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Comun.fondo(j, t);
        float arriba = Comun.cabecera(j, "MetajavBots", false);
        Jugador jug = j.jugador;
        dibujarMonedasCabecera(j, jug);
        float iz = 36, anchoIz = 340, y0 = arriba + 20, altoTotal = p.alto - y0 - 28;
        dibujarPanelRobot(j, jug, iz, y0, anchoIz, altoTotal);
        dibujarBloques(j, iz + anchoIz + 24, y0, p.ancho - (iz + anchoIz + 24) - 42, altoTotal);
        if (ui.atras()) {
            j.guardar();
            j.irSolo(new Inicio());
        }
        j.alarmaBajada.dibujarBannerEnMenu(j, arriba, dt);
    }

    /** Panel izquierdo con el robot elegido, sus datos y los accesos de configuración. */
    private void dibujarPanelRobot(Juego j, Jugador jug, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        ui.panel(x, y, w, h, null);
        float margen = 22, bw = w - margen * 2, bh = 46;
        float yAjustes = y + h - margen - bh;
        if (ui.boton("■ " + T.t("menu.ajustes"), x + margen, yAjustes, bw, bh, Ui.Estilo.NORMAL, true, T.t("menu.ajustes_ayuda"))) {
            j.ir(new Ajustes());
        }
        float yIa = yAjustes - bh - 10;
        if (ui.boton("● " + T.t("menu.panel_ia"), x + margen, yIa, bw, bh, Ui.Estilo.NORMAL, true, T.t("menu.panel_ia_ayuda"))) {
            j.ir(new PanelIA());
        }
        p.rect(x + margen, yIa - 14, bw, 1, ui.tema.borde, 0.7f);
        float alto = p.altoDeLinea(Math.min(ui.letra, 22f));
        float yExp = yIa - 24 - alto;
        float yMarcador = yExp - alto * 1.15f;
        String exp = T.f("menu.experiencia", jug.experiencia);
        String marcador = T.f("menu.marcador", jug.victorias(), jug.derrotas());
        p.textoCentrado(exp, x + w / 2, yExp, ajustarAncho(p, exp, w - 28, Math.min(ui.letra * 0.9f, 20f), 11f, false), false, ui.tema.textoSuave, 1);
        p.textoCentrado(marcador, x + w / 2, yMarcador, ajustarAncho(p, marcador, w - 28, Math.min(ui.letra, 22f), 12f, false), false, ui.tema.texto, 1);
        Jugador.Robot r = jug.robot();
        if (r != null) {
            String sub = r.montaje().modelo.nombre(T.idioma) + " · " + r.montaje().modelo.naturaleza.nombre(T.idioma);
            float tSub = ajustarAncho(p, sub, w - 28, Math.min(ui.letra * 0.9f, 20f), 11f, false);
            float tNom = ajustarAncho(p, r.nombre, w - 28, Math.min(ui.letra * 1.35f, 30f), 14f, true);
            float yModelo = yMarcador - alto * 1.45f;
            float yNombre = yModelo - p.altoDeLinea(tNom) - 4;
            p.textoCentrado(sub, x + w / 2, yModelo, tSub, false, ui.tema.textoSuave, 1);
            p.textoCentrado(r.nombre, x + w / 2, yNombre, tNom, true, ui.tema.texto, 1);
            float pies = yNombre - 28;
            float talla = Math.max(1.2f, Math.min(3.4f, (pies - y - 40) / 85f));
            vista.robotSuelto(p, r.montaje(), x + w / 2, pies, talla, t, 0, true);
        }
    }

    /** Dibuja los tres bloques temáticos con su código de colores sobre la zona derecha. */
    private void dibujarBloques(Juego j, float x, float y, float w, float hTotal) {
        float sepBloques = 14;
        float cabH = 34;
        float padInferior = 12;
        float sepFilas = 10;
        int totalFilas = 0;
        float fijos = sepBloques * (BLOQUES.length - 1);
        for (Bloque b : BLOQUES) {
            int f = b.filas();
            totalFilas += f;
            fijos += cabH + padInferior + (f - 1) * sepFilas;
        }
        float altoFila = Math.max(96, (hTotal - fijos) / totalFilas);
        float yy = y;
        for (Bloque b : BLOQUES) {
            int f = b.filas();
            float bh = cabH + padInferior + f * altoFila + (f - 1) * sepFilas;
            dibujarBloque(j, b, x, yy, w, bh, cabH, altoFila, sepFilas);
            yy += bh + sepBloques;
        }
    }

    private void dibujarBloque(Juego j, Bloque b, float x, float y, float w, float h, float cabH, float altoFila, float sepFilas) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        int color = b.color(ui.tema);
        boolean claro = ui.tema == Tema.CLARO;
        p.caja(x, y, w, h, 14, Pincel.mezcla(ui.tema.panel, color, claro ? 0.05f : 0.07f), 1);
        p.bordeRedondeado(x, y, w, h, 14, Pincel.mezcla(ui.tema.borde, color, 0.52f), 1, 1.5f);
        p.caja(x + 12, y + 8, 4, cabH - 14, 2, color, 1);
        String titulo = T.t(b.titulo).toUpperCase(java.util.Locale.ROOT);
        float tCab = ajustarAncho(p, titulo, w - 36, Math.min(ui.letra * 0.88f, 20f), 12f, true);
        p.texto(titulo, x + 24, y + (cabH - p.altoDeLinea(tCab)) / 2f + 2f, tCab, true, color, 1);
        p.rect(x + 12, y + cabH - 1, w - 24, 1, Pincel.mezcla(ui.tema.borde, color, 0.35f), 0.7f);
        float padX = 12, sepX = 10;
        float cw = (w - padX * 2 - (b.columnas - 1) * sepX) / b.columnas;
        for (int i = 0; i < b.casillas.length; i++) {
            int col = i % b.columnas, fila = i / b.columnas;
            float cx = x + padX + col * (cw + sepX);
            float cy = y + cabH + 6 + fila * (altoFila + sepFilas);
            dibujarCasilla(j, b.casillas[i], color, cx, cy, cw, altoFila - 4);
        }
    }

    private void dibujarCasilla(Juego j, Casilla c, int color, float x, float y, float w, float h) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        boolean encima = ui.e.dentro(x, y, w, h);
        boolean claro = ui.tema == Tema.CLARO;
        float mezclaFondo = c.destacada ? (encima ? 0.24f : 0.14f) : (encima ? 0.17f : 0.04f);
        if (claro) mezclaFondo *= 0.65f;
        p.caja(x, y + 2, w, h, 11, 0x000000, claro ? 0.10f : 0.24f);
        p.caja(x, y, w, h, 11, Pincel.mezcla(ui.tema.panel, color, mezclaFondo), 1);
        int borde = (encima || c.destacada) ? color : Pincel.mezcla(ui.tema.borde, color, 0.38f);
        p.bordeRedondeado(x, y, w, h, 11, borde, 1, encima ? 2.4f : c.destacada ? 1.8f : 1.1f);
        p.caja(x + 4, y + 9, 4, h - 18, 2, color, encima || c.destacada ? 1f : 0.78f);
        float radio = Math.min(17f, h * 0.17f);
        float icx = x + 16 + radio, icy = y + 10 + radio;
        p.circulo(icx, icy, radio, Pincel.mezcla(ui.tema.panel, color, claro ? 0.18f : 0.28f), 1);
        p.textoCentrado(c.icono, icx, icy - radio * 0.58f, radio * 1.05f, true, color, 1);
        float pillW = 0;
        if (c.etiqueta != null) {
            String et = T.t(c.etiqueta);
            float tEt = ajustarAncho(p, et, w * 0.42f, Math.min(ui.letra * 0.62f, 13.5f), 10f, true);
            pillW = p.ancho(et, tEt, true) + 14;
            float pillH = p.altoDeLinea(tEt) + 6;
            float px = x + w - 10 - pillW, py = y + 9;
            p.caja(px, py, pillW, pillH, pillH / 2f, Pincel.mezcla(ui.tema.panel, color, claro ? 0.20f : 0.34f), 1);
            p.bordeRedondeado(px, py, pillW, pillH, pillH / 2f, color, 0.9f, 1f);
            p.textoCentrado(et, px + pillW / 2f, py + 3, tEt, true, ui.tema.texto, 1);
        }
        String titulo = T.t(c.clave);
        float tx = x + 26 + radio * 2;
        float maxTitW = Math.max(40, x + w - 10 - (pillW > 0 ? pillW + 6 : 0) - tx);
        float tTit = ajustarAncho(p, titulo, maxTitW, Math.min(ui.letra * 1.05f, 22f), 12f, true);
        p.texto(titulo, tx, icy - p.altoDeLinea(tTit) / 2f, tTit, true, ui.tema.texto, 1);
        String ayuda = T.t(c.ayuda);
        float dy = Math.max(y + 14 + radio * 2, icy + p.altoDeLinea(tTit) / 2f + 4);
        float dw = w - 28, altoDisp = Math.max(16, y + h - 8 - dy);
        float tAy = Math.min(ui.letra * 0.76f, 17f);
        while (tAy > 10.5f && p.altoDeParrafo(ayuda, dw, tAy) > altoDisp) tAy -= 0.5f;
        p.parrafo(ayuda, x + 16, dy, dw, tAy, ui.tema.textoSuave, 1);
        if (encima) ui.avisar(ayuda);
        if (ui.e.clic(x, y, w, h)) {
            j.sonidos.tocar("clic", 0.5f);
            j.ir(c.destino.apply(j));
        }
    }

    /** Ajusta el tamaño de letra para que un texto quepa sin recortarse ni desbordar. */
    private static float ajustarAncho(Pincel p, String texto, float anchoMax, float tamMax, float tamMin, boolean negrita) {
        if (texto == null || texto.isEmpty()) return tamMax;
        float ancho = p.ancho(texto, tamMax, negrita);
        if (ancho <= anchoMax || ancho <= 0) return tamMax;
        return Math.max(tamMin, tamMax * anchoMax / ancho);
    }

    /** Muestra el saldo actual de monedas en la cabecera del menú, respetando letra escalable e idioma. */
    private void dibujarMonedasCabecera(Juego j, Jugador jug) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        String texto = "◆ " + T.f("menu.monedas", Jugador.formatearMonedas(jug.monedas));
        float anchoMax = Math.max(120, p.ancho - 460 - 360);
        float tam = ui.cabe(texto, anchoMax - 32, Math.min(ui.letra, 22f), true);
        float mw = Math.min(anchoMax, Math.max(130, p.ancho(texto, tam, true) + 32));
        float mx = p.ancho - 460 - mw - 16, my = 14, mh = 50;
        int dorado = 0xFFC857;
        boolean encima = ui.e.dentro(mx, my, mw, mh);
        p.caja(mx, my, mw, mh, 10, Pincel.oscurecer(dorado, encima ? 0.55f : 0.65f), 1);
        p.bordeRedondeado(mx, my, mw, mh, 10, dorado, 1, encima ? 2.5f : 2);
        p.textoCentrado(texto, mx + mw / 2, my + (mh - p.altoDeLinea(tam)) / 2, tam, true, 0xFFFFFF, 1);
        if (encima) ui.avisar(T.t("menu.monedas_ayuda"));
        if (ui.e.clic(mx, my, mw, mh)) {
            j.sonidos.tocar("clic", 0.5f);
            j.ir(new Tienda());
        }
    }
}
