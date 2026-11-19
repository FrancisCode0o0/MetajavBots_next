package metajavbots.pantallas;

import java.util.List;

import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * ACCIONES DEL ROBOTITO — El robotito de los ejercicios hace de verdad lo
 * que dice el código, y se ve cada movimiento: con un número, va al montón,
 * agarra una caja, la lleva, la coloca y vuelve a por la siguiente; con un
 * texto, va hasta la pizarra y escribe letra a letra con el brazo (que se
 * estira lo que haga falta); con lo que imprime, escribe línea a línea en
 * un papel; con una lista, lleva cada cosa a su hueco de la estantería; con
 * sí o no, va a una puerta y la abre; con una excepción, tira de la palanca
 * de la alarma. Cuando el código falla, falla haciendo la acción: tropieza
 * y se le cae la caja que sobra, agarra aire donde faltaba una, escribe
 * torcido y en rojo, la cosa equivocada resbala de la estantería, tras la
 * puerta mala hay una pared, la palanca se le queda en la mano.
 * «f» va de 0 a 1 (lo que lleva hecho); AnimacionDelCaso dice cuánto dura.
 */
final class AccionesDelRobotito {

    private AccionesDelRobotito() {
    }

    /** Cuántas cajas se dibujan como mucho (si hay más, cada una vale varias). */
    static final int MAX_CAJAS = 15;
    /** Lo que tarda cada viaje con una caja o una cosa de la estantería (s), y el paseo hasta la pizarra. */
    static final float VIAJE = 0.62f, PASEO = 0.6f;
    private static final int CAJA = 0xC98B4A, TIZA = 0xF5F5F0, ROJO = AnimacionDelCaso.ROJO;

    // ================================================================ ayudas

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    /** Suave al arrancar y al frenar. */
    private static float suave(float t) {
        float u = Math.max(0, Math.min(1, t));
        return u * u * (3 - 2 * u);
    }

    /** Una caja caída (girada), con una grieta: la que se le ha caído al robotito. */
    static void cajaCaida(Pincel p, float cx, float cy, float lado, float giro, int color) {
        float c = (float) Math.cos(giro), s = (float) Math.sin(giro), m = lado / 2;
        float[] v = new float[8];
        float[][] esq = {{-m, -m}, {m, -m}, {m, m}, {-m, m}};
        for (int i = 0; i < 4; i++) {
            v[i * 2] = cx + esq[i][0] * c - esq[i][1] * s;
            v[i * 2 + 1] = cy + esq[i][0] * s + esq[i][1] * c;
        }
        p.poligono(v, color, 1);
        p.contorno(v, 1.5f, Pincel.oscurecer(color, 0.35f), 1);
        p.linea(cx - m * 0.4f * c, cy - m * 0.4f * s, cx + m * 0.1f, cy + m * 0.3f, 1.5f, 0x2A1A10, 0.9f);
    }

    private static void bocadillo(Ui ui, String s, float x, float y, int color) {
        Pincel p = ui.p;
        float t = ui.letra * 0.8f, w = p.ancho(s, t, true) + 14;
        p.caja(x - w / 2, y - t * 1.5f, w, t * 1.5f, 6, 0xFFFFFF, 0.92f);
        p.textoCentrado(s, x, y - t * 1.3f, t, true, color, 1);
    }

    // ================================================================ cajas: un viaje por caja

    /** Cuánto vale cada caja (para que no haya más de MAX_CAJAS). */
    static long valorDeCaja(long esperado, long obtenido) {
        long tope = Math.max(Math.abs(esperado), Math.abs(obtenido));
        return Math.max(1, (tope + MAX_CAJAS - 1) / MAX_CAJAS);
    }

    /** Los viajes que hace con un número: uno por caja, y uno más (en vano) si le faltan. */
    static int viajesDeCajas(long esperado, long obtenido) {
        long v = valorDeCaja(esperado, obtenido);
        int nEsp = (int) (Math.abs(esperado) / v), nObt = (int) (Math.abs(obtenido) / v);
        return Math.max(1, nObt + (nObt < nEsp ? 1 : 0));
    }

    /** El sitio de la caja i en la pila (esquina de arriba a la izquierda). */
    private static float[] hueco(float x0, float suelo, int i, int porFila, float lado) {
        return new float[] {x0 + (i % porFila) * (lado + 2), suelo - (i / porFila + 1) * (lado + 2)};
    }

    /** Las cajas: el montón a la izquierda, la pila a la derecha; un viaje por caja (agarra, lleva, coloca, vuelve). */
    static void cajas(Ui ui, float x, float suelo, float w, long esperado, long obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        long valor = valorDeCaja(esperado, obtenido);
        int nEsp = (int) (Math.abs(esperado) / valor), nObt = (int) (Math.abs(obtenido) / valor);
        float lado = Math.max(16, Math.min(28, (w - 220) / 12)), x0 = x + 170, monton = x + 44;
        int porFila = Math.max(1, Math.min(8, (int) ((w - 200) / (lado + 2))));
        // Las que se le caen: las que sobran y, si el número es casi el bueno (misma cuenta de cajas), la última.
        boolean ultimaMala = !bien && nObt == nEsp && nObt > 0;
        int viajes = Math.max(1, nObt + (nObt < nEsp ? 1 : 0));
        float pos = Math.min(viajes, f * viajes);
        int i = Math.min(viajes - 1, (int) pos);
        float u = f >= 1 ? 1 : pos - i;
        for (int k = 0; k < nEsp; k++) {
            float[] h = hueco(x0, suelo, k, porFila, lado);
            boolean parpadea = k >= nObt && i >= nObt && (int) (u * 6) % 2 == 0;
            AnimacionDelCaso.objeto(p, h[0], h[1], lado, 0x8A919C, parpadea ? 0.55f : 0.25f);
        }
        // El montón del que coge (le quedan las que aún no ha llevado).
        int quedan = Math.max(0, Math.min(4, nObt - i - (u > 0.35f ? 1 : 0)));
        for (int k = 0; k < quedan; k++) AnimacionDelCaso.objeto(p, monton - lado / 2 + (k % 2) * 3, suelo - (k + 1) * (lado + 1), lado, CAJA, 1);
        // Las ya puestas (o caídas).
        for (int k = 0; k < Math.min(nObt, i + (u >= 0.95f ? 1 : 0)); k++) dejada(p, k, x0, suelo, porFila, lado, nEsp, ultimaMala && k == nObt - 1, monton);
        float inicio = i == 0 ? x + 70 : finDeViaje(i - 1, x0, suelo, porFila, lado, nEsp, ultimaMala && i - 1 == nObt - 1, monton);
        if (i >= nObt) {
            viajeEnVano(ui, inicio, monton, suelo, lado, u, f, bien);
        } else {
            boolean cae = i >= nEsp || ultimaMala && i == nObt - 1;
            viaje(ui, i, hueco(x0, suelo, i, porFila, lado), suelo, lado, monton, inicio, u, f, bien, cae);
        }
        String cuenta = obtenido + (valor > 1 ? "  (" + T.f("animacion.cada_caja", valor) + ")" : "");
        p.texto(T.f("animacion.cajas", cuenta, esperado), x + 14, suelo - 170, ui.letra * 0.85f, false, ui.tema.texto, 1);
        if (obtenido < 0 || esperado < 0) p.texto(T.t("animacion.negativo"), x + 14, suelo - 146, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
    }

    /** Dónde acaba el robotito tras el viaje k (junto a la pila, o donde se le cayó la caja). */
    private static float finDeViaje(int k, float x0, float suelo, int porFila, float lado, int nEsp, boolean ultimaMala, float monton) {
        float[] h = hueco(x0, suelo, k, porFila, lado);
        if (k >= nEsp || ultimaMala) return lerp(monton + 30, h[0] - 30, 0.5f);
        return h[0] - 30;
    }

    /** Una caja ya dejada: en la pila, o en el suelo, girada y en rojo, si se le cayó. */
    private static void dejada(Pincel p, int k, float x0, float suelo, int porFila, float lado, int nEsp, boolean ultimaMala, float monton) {
        if (k >= nEsp || ultimaMala) {
            float fin = finDeViaje(k, x0, suelo, porFila, lado, nEsp, ultimaMala, monton);
            cajaCaida(p, fin + 46 + (k % 3) * 9, suelo - lado * 0.45f, lado, 0.5f + k * 0.7f, ROJO);
            return;
        }
        float[] h = hueco(x0, suelo, k, porFila, lado);
        AnimacionDelCaso.objeto(p, h[0], h[1], lado, CAJA, 1);
    }

    /** Un viaje: va al montón, se agacha y agarra la caja, la lleva en alto, la coloca en su hueco (o tropieza y se le cae). */
    private static void viaje(Ui ui, int i, float[] h, float suelo, float lado, float monton, float inicio, float u, float f, boolean bien,
            boolean cae) {
        Pincel p = ui.p;
        float junto = monton + 30, destino = h[0] - 30;
        float rx, hx, hy;
        boolean enLaMano;
        float alto = suelo - 96;
        if (u < 0.3f) {
            rx = lerp(inicio, junto, suave(u / 0.3f));
            hx = rx + 22;
            hy = suelo - 50;
            enLaMano = false;
        } else if (u < 0.4f) {
            // Se agacha a por ella y la sube.
            float a = (u - 0.3f) / 0.1f;
            rx = junto;
            hx = lerp(rx + 22, monton, a < 0.5f ? a * 2 : 1);
            hy = a < 0.5f ? lerp(suelo - 50, suelo - lado - 4, a * 2) : lerp(suelo - lado - 4, alto, (a - 0.5f) * 2);
            enLaMano = a >= 0.5f;
        } else if (cae && u >= 0.6f) {
            caida(ui, junto, destino, suelo, lado, u, i, f, bien);
            return;
        } else if (u < 0.8f) {
            rx = lerp(junto, destino, suave((u - 0.4f) / 0.4f));
            hx = rx + 18;
            hy = alto + (float) Math.sin(u * 60) * 2;
            enLaMano = true;
        } else {
            // La baja hasta su hueco y la suelta.
            float a = Math.min(1, (u - 0.8f) / 0.15f);
            rx = destino;
            hx = lerp(rx + 18, h[0] + lado / 2, a);
            hy = lerp(alto, h[1] + lado / 2, a);
            enLaMano = u < 0.95f;
        }
        AnimacionDelCaso.robotMano(p, rx, suelo, hx, hy, f, bien);
        if (enLaMano) AnimacionDelCaso.objeto(p, hx - lado / 2, hy - lado / 2, lado, CAJA, 1);
    }

    /** La caja que sobra: a medio camino tropieza, la caja sale volando y cae al suelo, girando. */
    private static void caida(Ui ui, float junto, float destino, float suelo, float lado, float u, int i, float f, boolean bien) {
        Pincel p = ui.p;
        float rx = lerp(junto, destino, 0.5f);
        float a = Math.min(1, (u - 0.6f) / 0.2f);
        float bx = lerp(rx + 18, rx + 46 + (i % 3) * 9, a);
        float by = suelo - 96 - 30 * (float) Math.sin(Math.PI * a) + (96 - lado * 0.45f) * a * a;
        AnimacionDelCaso.robotMano(p, rx + (float) Math.sin(u * 50) * 3 * (1 - a), suelo + 6 * (1 - Math.abs(1 - 2 * a)), rx + 26, suelo - 70, f, bien);
        cajaCaida(p, bx, by, lado, a * (2.2f + i * 0.7f) + 0.5f, a >= 1 ? ROJO : CAJA);
        if (a > 0.2f && f < 1) bocadillo(ui, T.t("animacion.ups"), rx, suelo - 112, ROJO);
    }

    /** Le faltan cajas: va al montón, agarra aire, mira la mano y se queda con una interrogación. */
    private static void viajeEnVano(Ui ui, float inicio, float monton, float suelo, float lado, float u, float f, boolean bien) {
        float junto = monton + 30;
        float rx = lerp(inicio, junto, suave(u / 0.4f));
        float a = Math.max(0, (u - 0.4f) / 0.3f);
        float hx = lerp(rx + 22, monton, Math.min(1, a * 2)), hy = lerp(suelo - 50, suelo - lado - 4, Math.min(1, a * 2));
        if (a > 0.5f) hy = lerp(suelo - lado - 4, suelo - 90, (a - 0.5f) * 2);
        AnimacionDelCaso.robotMano(ui.p, rx, suelo, hx, hy, f, bien);
        if (u > 0.55f && f < 1) bocadillo(ui, "?", rx, suelo - 112, ROJO);
    }

    // ================================================================ escribir (pizarra y papel)

    /** Las letras «que cuestan» escribir unas líneas (cada salto de línea, como tres letras: el brazo va al principio). */
    static int letrasDe(String[] lineas) {
        int n = 0;
        for (String l : lineas) n += l.length() + 3;
        return n;
    }

    /**
     * Va hasta la pizarra (o el papel) y escribe letra a letra: el brazo se estira hasta la letra que escribe. Lo que no
     * coincide con lo esperado lo escribe torcido, cuesta abajo y en rojo; al acabar mal, la tiza se parte.
     */
    static void escribir(Ui ui, float x, float y, float w, float h, String esperado, String obtenido, float f, boolean bien, boolean papel,
            float paseo) {
        Pincel p = ui.p;
        String[] esp = esperado.split("\n", -1), obt = obtenido.split("\n", -1);
        if (!papel) {
            esp = new String[] {esperado.replace("\n", "⏎")};
            obt = new String[] {obtenido.replace("\n", "⏎")};
        }
        float bx = x + 150, by = y + 40, bw = papel ? (w - 190) * 0.62f : w - 170, bh = h - 40 - ui.letra * 1.8f;
        // La pizarra verde con su marco, o una hoja de papel; en el papel, lo esperado va en otra hoja al lado, en gris.
        if (papel) {
            p.caja(bx, by, bw, bh, 4, 0xF2EEE3, 1);
            for (float ly = by + 18; ly < by + bh; ly += 18) p.rect(bx + 4, ly, bw - 8, 1, 0xB9C6D6, 0.5f);
            p.caja(bx + bw + 20, by, w - 190 - bw, bh, 4, 0xC9C5BA, 1);
            p.texto(T.t("animacion.esperaba"), bx + bw + 26, by + 4, ui.letra * 0.7f, true, 0x55524A, 1);
        } else {
            p.caja(bx, by, bw, bh, 6, 0x1F3A2E, 1);
            p.bordeRedondeado(bx, by, bw, bh, 6, 0x8C6A43, 1, 4);
        }
        float t = Math.min(ui.letra * (papel ? 1.0f : 1.45f), papel ? 24 : 34);
        float tope = papel ? bw - 20 : bw - 24;
        for (String l : obt) while (t > 12 && p.ancho(l, t, true) > tope) t *= 0.92f;
        float alto = p.altoDeLinea(t) * (papel ? 1.05f : 1);
        int caben = Math.max(1, (int) ((bh - (papel ? 10 : t * 2.2f)) / alto));
        if (!papel) {
            p.texto(ui.recortarTexto(T.t("animacion.esperaba") + " " + esp[0], bw - 24, t * 0.75f, false), bx + 12, by + 8, t * 0.75f, false, 0x9FB8AA, 1);
        } else {
            for (int i = 0; i < esp.length && i < caben; i++) {
                p.texto(ui.recortarTexto(esp[i], w - 190 - bw - 14, t * 0.85f, false), bx + bw + 28, by + 8 + ui.letra + i * alto, t * 0.85f, false,
                        0x55524A, 1);
            }
        }
        // Lo que lleva escrito: primero el paseo hasta la pizarra; luego, letra a letra.
        float g = paseo >= 1 ? 1 : Math.max(0, (f - paseo) / (1 - paseo));
        int total = letrasDe(obt), hechas = Math.round(total * g);
        float y0 = papel ? by + 6 : by + 18 + t * 0.9f;
        int primera = 0;
        // Si no cabe todo, el papel sube (se ven las últimas líneas).
        int lineaActual = 0, cuenta = 0;
        for (int i = 0; i < obt.length; i++) {
            if (hechas >= cuenta) lineaActual = i;
            cuenta += obt[i].length() + 3;
        }
        if (papel && lineaActual >= caben) primera = lineaActual - caben + 1;
        float manoX = bx + 12, manoY = y0 + t * 0.7f;
        boolean escribiendo = false;
        int resto = hechas;
        for (int i = 0; i < obt.length && resto > 0; i++) {
            int letras = Math.min(obt[i].length(), resto);
            resto -= obt[i].length() + 3;
            if (i < primera) continue;
            float ly = y0 + (i - primera) * alto;
            String bueno = i < esp.length ? esp[i] : "";
            float[] fin = linea(p, obt[i].substring(0, letras), bueno, bx + (papel ? 8 : 12), ly, t, papel);
            manoX = fin[0];
            manoY = fin[1] + t * 0.7f;
            escribiendo = letras < obt[i].length() || resto < 0;
        }
        float rx = lerp(x + 40, bx + 20, suave(f / Math.max(0.01f, paseo)));
        if (f >= paseo) rx = Math.max(bx - 30, Math.min(bx + bw - 30, manoX - 46));
        // Al acabar se aparta de la pizarra (para que se lea lo escrito).
        if (f >= 1) rx = x + 70;
        float temblor = escribiendo ? (float) Math.sin(f * 400) * 2 : 0;
        if (f < paseo) AnimacionDelCaso.robotMano(p, rx, y + h - 26, rx + 24, y + h - 80, f, bien);
        else AnimacionDelCaso.robotMano(p, rx, y + h - 26, manoX + temblor, manoY - (escribiendo ? Math.abs(temblor) : 6), f, bien);
        if (f >= paseo && f < 1) p.circulo(manoX + temblor, manoY - 2, 3, papel ? 0x1E2A4A : TIZA, 1);
        if (obtenido.isEmpty() && f >= paseo && f < 1) bocadillo(ui, "…", rx, y + h - 140, ui.tema.textoSuave);
        if (f >= 1 && !bien) {
            // La tiza (o el lápiz), partida en el suelo.
            p.rect(manoX - 10, y + h - 30, 8, 3, papel ? 0xE8C547 : TIZA, 1);
            p.rect(manoX + 4, y + h - 28, 6, 3, papel ? 0xE8C547 : TIZA, 1);
        }
    }

    /**
     * Una línea escrita: lo que coincide, recto; desde la primera letra distinta, cada letra un poco más abajo (torcida)
     * y en rojo. Devuelve dónde acaba (para la mano).
     */
    private static float[] linea(Pincel p, String escrito, String bueno, float x, float y, float t, boolean papel) {
        int igual = 0;
        while (igual < escrito.length() && igual < bueno.length() && escrito.charAt(igual) == bueno.charAt(igual)) igual++;
        int recto = papel ? 0x1E1E1E : TIZA;
        p.texto(escrito.substring(0, igual), x, y, t, true, recto, 1);
        float cx = x + p.ancho(escrito.substring(0, igual), t, true), cy = y;
        for (int k = igual; k < escrito.length(); k++) {
            String c = escrito.substring(k, k + 1);
            float bajada = (k - igual + 1) * t * 0.11f + (float) Math.sin(k * 2.3f) * t * 0.08f;
            p.texto(c, cx, y + bajada, t, true, ROJO, 1);
            cx += p.ancho(c, t, true) * (1 + (k % 3) * 0.08f);
            cy = y + bajada;
        }
        // Le falta el final de la línea: un subrayado rojo donde debía seguir.
        if (igual == escrito.length() && escrito.length() < bueno.length() && escrito.length() > 0) p.rect(cx + 2, y + t * 1.2f, t, 2, ROJO, 0.6f);
        return new float[] {cx, cy};
    }

    // ================================================================ estantería: un viaje por elemento

    /** La caja de la izquierda con todas las cosas; un viaje por cada una hasta su hueco; la que no es, resbala y cae. */
    static void estanteria(Ui ui, float x, float suelo, float w, List<String> esperado, List<String> obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float ey = suelo - 92, cajon = x + 46;
        p.rect(x + 130, ey, w - 150, 6, 0x6B4F33, 1);
        p.rect(x + 134, ey + 6, 6, suelo - ey - 6, 0x5A4129, 1);
        p.rect(x + w - 30, ey + 6, 6, suelo - ey - 6, 0x5A4129, 1);
        // El cajón de las cosas.
        p.caja(cajon - 30, suelo - 34, 60, 34, 4, 0x8C6A43, 1);
        int n = Math.max(esperado.size(), obtenido.size());
        float hueco = n == 0 ? 0 : Math.min(110, (w - 180) / n), t = ui.letra * 0.75f;
        int viajes = Math.max(1, obtenido.size() + (obtenido.size() < esperado.size() ? 1 : 0));
        float pos = Math.min(viajes, f * viajes);
        int i = Math.min(viajes - 1, (int) pos);
        float u = f >= 1 ? 1 : pos - i;
        for (int k = 0; k < esperado.size(); k++) {
            float cx = x + 146 + k * hueco;
            p.caja(cx, ey - 40, hueco - 8, 38, 5, 0x8A919C, 0.22f);
            p.textoCentrado(ui.recortarTexto(esperado.get(k), hueco - 12, t, false), cx + (hueco - 8) / 2, ey - 32, t, false, ui.tema.textoApagado, 1);
        }
        for (int k = 0; k < Math.min(obtenido.size(), i + (u >= 0.95f ? 1 : 0)); k++) {
            cosa(ui, k, esperado, obtenido, x + 146 + k * hueco, ey, suelo, hueco, t, 1);
        }
        if (n == 0) p.textoCentrado(T.t("animacion.vacia"), x + w / 2, ey - 30, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
        float inicio = i == 0 ? x + 80 : x + 146 + (i - 1) * hueco - 26;
        if (i >= obtenido.size()) {
            viajeEnVano(ui, inicio, cajon, suelo, 20, u, f, bien);
            return;
        }
        float destino = Math.min(x + w - 40, x + 146 + i * hueco - 26), junto = cajon + 36;
        float rx, hx, hy;
        if (u < 0.3f) {
            rx = lerp(inicio, junto, suave(u / 0.3f));
            hx = rx + 22;
            hy = suelo - 50;
        } else if (u < 0.4f) {
            rx = junto;
            hx = cajon;
            hy = lerp(suelo - 30, suelo - 96, (u - 0.3f) / 0.1f);
        } else if (u < 0.8f) {
            rx = lerp(junto, destino, suave((u - 0.4f) / 0.4f));
            hx = rx + 20;
            hy = suelo - 100;
        } else {
            rx = destino;
            hx = lerp(rx + 20, rx + 26 + hueco / 2, (u - 0.8f) / 0.15f);
            hy = lerp(suelo - 100, ey - 20, (u - 0.8f) / 0.15f);
        }
        AnimacionDelCaso.robotMano(p, rx, suelo, hx, hy, f, bien);
        if (u >= 0.35f && u < 0.95f) etiqueta(ui, obtenido.get(i), hx - (hueco - 8) / 2, hy - 19, hueco - 8, t, 0x3FA9F5, 0);
    }

    /** Una cosa ya dejada: en su hueco si es la buena; si no, resbala del estante y queda en el suelo, ladeada y en rojo. */
    private static void cosa(Ui ui, int k, List<String> esperado, List<String> obtenido, float cx, float ey, float suelo, float hueco, float t, float a) {
        boolean igual = k < esperado.size() && esperado.get(k).equals(obtenido.get(k));
        if (igual) etiqueta(ui, obtenido.get(k), cx, ey - 40, hueco - 8, t, 0x3FA9F5, 0);
        else etiqueta(ui, obtenido.get(k), cx + 6, suelo - 38, hueco - 8, t, ROJO, 0.25f + (k % 2) * 0.15f);
    }

    /** Una caja con su texto (girada un poco si está caída: se pinta como una caja caída y el texto encima). */
    private static void etiqueta(Ui ui, String s, float x, float y, float w, float t, int color, float giro) {
        Pincel p = ui.p;
        if (giro == 0) p.caja(x, y, w, 38, 5, color, 1);
        else {
            float c = (float) Math.cos(giro), sn = (float) Math.sin(giro), cx = x + w / 2, cy = y + 19;
            float[] v = new float[8];
            float[][] esq = {{-w / 2, -19}, {w / 2, -19}, {w / 2, 19}, {-w / 2, 19}};
            for (int i = 0; i < 4; i++) {
                v[i * 2] = cx + esq[i][0] * c - esq[i][1] * sn;
                v[i * 2 + 1] = cy + esq[i][0] * sn + esq[i][1] * c;
            }
            p.poligono(v, color, 1);
        }
        p.textoCentrado(ui.recortarTexto(s, w - 6, t, true), x + w / 2, y + 8 + giro * 14, t, true, 0xFFFFFF, 1);
    }

    // ================================================================ puertas

    /** Va a la puerta que dice el código, la abre y entra; tras la buena hay luz; tras la mala, una pared con la que se da. */
    static void puertas(Ui ui, float x, float suelo, float w, float h, boolean esperado, boolean obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float pw = 90, ph = Math.min(120, h - 60);
        float[] px = {x + w * 0.35f, x + w * 0.7f};
        String[] nombre = {"true", "false"};
        int elegida = obtenido ? 0 : 1;
        float abre = Math.max(0, Math.min(1, (f - 0.45f) / 0.15f));
        for (int i = 0; i < 2; i++) {
            boolean laBuena = (i == 0) == esperado;
            float dx = px[i] - pw / 2, dy = suelo - ph;
            // Detrás: luz si es la buena; ladrillos si no.
            p.rect(dx, dy, pw, ph, laBuena ? 0xFFF1A8 : 0x8E4A3A, 1);
            if (!laBuena) for (float ly = dy + 10; ly < suelo; ly += 14) p.rect(dx, ly, pw, 2, 0x5A2A20, 1);
            float hoja = i == elegida ? pw * (1 - abre * 0.85f) : pw;
            p.caja(dx, dy, hoja, ph, 6, Pincel.oscurecer(0x8C6A43, laBuena ? 0 : 0.3f), 1);
            if (hoja > pw * 0.5f) p.circulo(dx + hoja - 12, dy + ph / 2, 4, 0xE8C547, 1);
            p.textoCentrado(nombre[i], px[i], dy - ui.letra * 1.3f, ui.letra, true, laBuena ? AnimacionDelCaso.VERDE : 0xFFF1C1, 1);
        }
        float destino = px[elegida];
        float rx;
        float caida = 0;
        boolean pared = obtenido != esperado;
        if (f < 0.45f) rx = lerp(x + 50, destino - pw / 2 - 26, suave(f / 0.45f));
        else if (f < 0.6f) rx = destino - pw / 2 - 26;
        else if (!pared) rx = lerp(destino - pw / 2 - 26, destino, suave((f - 0.6f) / 0.3f));
        else {
            // Se da contra la pared y sale rebotado.
            float a = (f - 0.6f) / 0.4f;
            rx = a < 0.4f ? lerp(destino - pw / 2 - 26, destino - 10, a / 0.4f) : lerp(destino - 10, destino - pw / 2 - 50, (a - 0.4f) / 0.6f);
            caida = a > 0.4f ? Math.min(1, (a - 0.4f) * 3) : 0;
        }
        float manoX = f >= 0.45f && f < 0.6f ? destino - pw / 2 + pw * (1 - abre * 0.85f) - 12 : rx + 22;
        AnimacionDelCaso.robotMano(p, rx, suelo + caida * 4, manoX, suelo - ph / 2, f, bien);
        if (pared && caida > 0 && f < 1) {
            for (int k = 0; k < 3; k++) {
                double a = f * 30 + k * 2.1;
                p.texto("★", rx + (float) Math.cos(a) * 22 - 6, suelo - 110 + (float) Math.sin(a) * 6, 16, true, 0xFFD23F, 1);
            }
            bocadillo(ui, T.idioma.elegir("¡PLONK!", "PLONK!", "PLONK!"), destino, suelo - ph - ui.letra * 1.6f, ROJO);
        }
    }

    // ================================================================ la alarma

    /** Va a la palanca y tira: con la excepción buena salta la alarma; si no, la palanca se le queda en la mano. */
    static void alarma(Ui ui, float x, float suelo, float w, float h, String esperado, String obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float lx = x + w * 0.42f, ly = suelo - 70;
        boolean tirada = f >= 0.55f;
        boolean salta = tirada && bien;
        boolean luz = salta && (int) (f * 14) % 2 == 0;
        float sirena = x + w * 0.75f;
        p.caja(sirena - 22, suelo - h + 50, 44, 30, 8, luz ? ROJO : 0x7A2A28, 1);
        if (luz) p.brillo(sirena, suelo - h + 65, 60, ROJO, 0.5f);
        // La palanca en su caja.
        p.caja(lx - 16, ly - 10, 32, 44, 4, 0x4A5260, 1);
        boolean rota = tirada && !bien;
        float giro = !tirada ? -0.6f : 0.6f;
        if (!rota) p.linea(lx, ly + 12, lx + (float) Math.sin(giro) * 36, ly + 12 - (float) Math.cos(giro) * 36, 5, 0xB8C0C8, 1);
        if (!rota) p.circulo(lx + (float) Math.sin(giro) * 36, ly + 12 - (float) Math.cos(giro) * 36, 7, ROJO, 1);
        float rx = lerp(x + 50, lx - 40, suave(f / 0.45f));
        float mx = f < 0.45f ? rx + 22 : lx + (float) Math.sin(f < 0.55f ? -0.6f : 0.6f) * 36;
        float my = f < 0.45f ? suelo - 50 : ly + 12 - (float) Math.cos(f < 0.55f ? -0.6f : 0.6f) * 36;
        if (rota) {
            mx = rx + 30;
            my = suelo - 80;
        }
        AnimacionDelCaso.robotMano(p, rx, suelo, mx, my, f, bien);
        if (rota && f < 1) {
            // Se ha quedado con la palanca en la mano.
            p.linea(mx, my, mx + 10, my - 34, 5, 0xB8C0C8, 1);
            p.circulo(mx + 10, my - 34, 7, ROJO, 1);
            bocadillo(ui, T.idioma.elegir("¿¡…!?", "?!…", "?!…"), rx, suelo - 120, ROJO);
        }
        p.texto(T.f("animacion.alarma", esperado), x + w * 0.52f, suelo - 70, ui.letra * 0.8f, false, ui.tema.texto, 1);
        p.texto(ui.recortarTexto(T.t("animacion.salio") + " " + obtenido, w * 0.46f, ui.letra * 0.8f, false), x + w * 0.52f, suelo - 44, ui.letra * 0.8f,
                false, bien ? AnimacionDelCaso.VERDE : ROJO, 1);
    }
}
