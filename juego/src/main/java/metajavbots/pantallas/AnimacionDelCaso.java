package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.List;

import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * ANIMACIÓN DEL CASO — Un robotito hace lo que ha hecho el código en un caso
 * de un ejercicio (del banco, de plantilla o de la IA), según lo que
 * devuelve: un número, apila cajas; sí o no, elige una puerta; un texto, lo
 * escribe en una pizarra; una lista o un array, la coloca en una estantería;
 * lo que imprime, lo saca en una tira de papel; una excepción, salta la
 * alarma. Lo que se esperaba se ve en gris detrás: si el robot se equivoca,
 * se ve dónde (la caja que sobra o falta, la puerta mala, la letra o el
 * elemento distinto, en rojo).
 */
final class AnimacionDelCaso {

    /** Lo que se anima, según la forma de la respuesta. */
    enum Forma { CAJAS, PUERTAS, PIZARRA, ESTANTERIA, PAPEL, ALARMA }

    /** Los segundos desde que empezó el caso (para que el robotito baile o se caiga al acabar); solo se dibuja de uno en uno. */
    private static float reloj;
    private static EscenasDelRobotito.Escena escenaActual = EscenasDelRobotito.Escena.CLASIFICAR;

    /** Cuánto dura la animación de un caso (s). */
    static final float DURACION = 3.2f;
    static final int ROJO = 0xFF5E5B, VERDE = 0x5BD685;
    private static final int CAJA = 0xC98B4A;
    /** Cuándo acabó el caso (en el reloj; -1 si aún no) y qué final le toca (FinalesDelRobotito). */
    private static float acabo = -1;
    private static int variante;

    private AnimacionDelCaso() {
    }

    /** La forma que toca a un caso, mirando lo que se esperaba. */
    static Forma forma(Ejercicio.Tipo tipo, String esperado) {
        String s = esperado.strip();
        if (tipo == Ejercicio.Tipo.SALIDA) return Forma.PAPEL;
        if (s.startsWith("!")) return Forma.ALARMA;
        if (s.equals("true") || s.equals("false")) return Forma.PUERTAS;
        if (s.matches("-?\\d{1,6}L?")) return Forma.CAJAS;
        if (s.startsWith("List.of") || s.startsWith("new ") && s.contains("{") || s.startsWith("Arrays.asList") || s.startsWith("Set.of")) {
            return Forma.ESTANTERIA;
        }
        return Forma.PIZARRA;
    }

    /** Dibuja el caso con el robot; f: lo que lleva hecho (de 0 a 1, al ritmo de las líneas que ejecuta el código). */
    static void dibujar(Ui ui, float x, float y, float w, float h, Ejercicio e, Comprobador.Resultado r, float f, float segundos) {
        Pincel p = ui.p;
        Ejercicio.Tipo tipo = e.tipo;
        reloj = segundos;
        escenaActual = EscenasDelRobotito.de(e.tema, e.enunciado.es + " " + e.titulo.es);
        p.caja(x, y, w, h, 10, ui.tema.fondo, 1);
        p.recortar(x, y, w, h);
        EscenasDelRobotito.fondo(p, escenaActual, x, y, w, h, reloj);
        String esperado = r.caso.esperado;
        String obtenido = r.obtenido == null ? "" : r.obtenido;
        float suelo = y + h - 26;
        switch (forma(tipo, esperado)) {
            case CAJAS: cajas(ui, x, suelo, w, numero(esperado), numero(obtenido), f, r.bien); break;
            case PUERTAS: puertas(ui, x, suelo, w, h, esperado.strip().equals("true"), obtenido.strip().equals("true"), f, r.bien); break;
            case ESTANTERIA: estanteria(ui, x, suelo, w, elementos(dentro(esperado)), elementos(dentro(obtenido)), f, r.bien); break;
            case PAPEL: papel(ui, x, y, w, h, texto(esperado), texto(obtenido), f, r.bien); break;
            case ALARMA: alarma(ui, x, suelo, w, h, esperado.substring(1), obtenido, f, r.bien); break;
            default: pizarra(ui, x, y, w, h, texto(esperado), texto(obtenido), f, r.bien); break;
        }
        p.sinRecorte();
        String veredicto = r.bien ? "✔ " + T.t("animacion.bien") : "✘ " + T.t("animacion.mal");
        p.textoDerecha(veredicto, x + w - 250, y + 8, ui.letra * 0.9f, true, r.bien ? VERDE : ROJO, 1);
    }

    // ================================================================ el robotito

    /** El robotito, con sus ruedas, su cabeza y un brazo (alzado de 0 a 1). Contento si acierta. */
    static void robotito(Pincel p, float x, float suelo, float brazo, boolean contento, int color) {
        robotito(p, x, suelo, brazo, contento ? VERDE : ROJO, color);
    }

    /** El robotito con los ojos de ese color (rojo brillante, la máquina malvada). */
    static void robotito(Pincel p, float x, float suelo, float brazo, int ojo, int color) {
        float k = 1.6f;   // su tamaño
        p.elipse(x, suelo + 2, 16 * k, 4 * k, 0, 0x000000, 0.3f);
        p.circulo(x - 9 * k, suelo - 5 * k, 5 * k, 0x2A2F38, 1);
        p.circulo(x + 9 * k, suelo - 5 * k, 5 * k, 0x2A2F38, 1);
        p.caja(x - 13 * k, suelo - 34 * k, 26 * k, 26 * k, 5 * k, color, 1);
        p.caja(x - 9 * k, suelo - 52 * k, 18 * k, 16 * k, 4 * k, Pincel.aclarar(color, 0.15f), 1);
        p.caja(x - 6 * k, suelo - 47 * k, 12 * k, 6 * k, 2 * k, 0x111318, 1);
        p.circulo(x - 2.5f * k, suelo - 44 * k, 1.6f * k, ojo, 1);
        p.circulo(x + 2.5f * k, suelo - 44 * k, 1.6f * k, ojo, 1);
        p.linea(x, suelo - 52 * k, x + 3 * k, suelo - 59 * k, 1.5f * k, 0xAAB0BA, 1);
        p.circulo(x + 3 * k, suelo - 60 * k, 2 * k, ojo, 1);
        float bx = x + 13 * k, by = suelo - 26 * k;
        p.linea(bx, by, bx + 12 * k, by - (12 * brazo - 6 * (1 - brazo)) * k, 3 * k, Pincel.oscurecer(color, 0.2f), 1);
    }

    /** El robotito trabajando; al acabar, uno de sus finales (FinalesDelRobotito), alegre si acertó y desastroso si no. */
    static void robot(Pincel p, float x, float suelo, float brazo, float f, boolean bien) {
        if (f < 1) acabo = -1;
        else if (acabo < 0) {
            acabo = reloj;
            variante = (int) (Math.random() * FinalesDelRobotito.VARIANTES);
        }
        if (f < 1) {
            // En las excepciones, a media tarea tropieza (lanza), se tambalea y se recupera (catch): sigue con lo suyo.
            float tropiezo = EscenasDelRobotito.tropiezo(escenaActual, f);
            robotito(p, x + tropiezo * 10, suelo + tropiezo * 8, tropiezo > 0 ? 1 : brazo, tropiezo < 0.5f, 0x3FA9F5);
            if (tropiezo > 0.3f) p.texto("!", x - 6, suelo - 120, 24, true, 0xFFD23F, 1);
            return;
        }
        if (bien) FinalesDelRobotito.bien(p, x, suelo, reloj - acabo, variante);
        else FinalesDelRobotito.mal(p, x, suelo, reloj - acabo, variante);
    }

    /** Un objeto del escenario (lo que el robotito apila o coloca), según la escena. */
    static void objeto(Pincel p, float cx, float cy, float lado, int color, float alfa) {
        EscenasDelRobotito.objeto(p, escenaActual, cx, cy, lado, color, CAJA, alfa);
    }

    // ================================================================ las formas

    private static void cajas(Ui ui, float x, float suelo, float w, long esperado, long obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        long tope = Math.max(Math.abs(esperado), Math.abs(obtenido));
        // Muchas cajas: cada una vale más (y se dice cuánto).
        long valor = Math.max(1, (tope + 39) / 40);
        int nEsp = (int) Math.min(40, Math.abs(esperado) / valor), nObt = (int) Math.min(40, Math.abs(obtenido) / valor);
        float lado = Math.max(14, Math.min(28, (w - 180) / 20)), x0 = x + 130;
        int porFila = Math.max(1, (int) ((w - 160) / (lado + 2)));
        for (int i = 0; i < nEsp; i++) caja(p, x0, suelo, i, porFila, lado, 0x8A919C, 0.3f);
        // Cada caja es un viaje: al montón (izquierda), la coge, la lleva a la pila y la deja.
        float viajes = nObt * f;
        int puestas = Math.min(nObt, (int) viajes);
        float fase = puestas >= nObt ? 1 : viajes - puestas;
        for (int i = 0; i < puestas; i++) caja(p, x0, suelo, i, porFila, lado, i >= nEsp ? ROJO : CAJA, 1);
        float monton = x + 40;
        if (puestas < nObt) objeto(p, monton - lado / 2, suelo - lado - 2, lado, CAJA, 1);
        float destino = x0 + (puestas % porFila) * (lado + 2) - 30;
        float rx;
        boolean llevando = puestas < nObt && fase >= 0.3f && fase < 0.85f;
        if (puestas >= nObt) rx = x + 60;
        else if (fase < 0.3f) rx = monton + 26;
        else if (fase < 0.85f) rx = monton + 26 + (destino - monton - 26) * ((fase - 0.3f) / 0.55f);
        else rx = destino - (destino - monton - 26) * ((fase - 0.85f) / 0.15f);
        robot(p, rx, suelo, llevando ? 1 : 0.2f, f, bien);
        if (llevando) objeto(p, rx + 14, suelo - 70, lado, CAJA, 1);
        String cuenta = obtenido + (valor > 1 ? "  (" + T.f("animacion.cada_caja", valor) + ")" : "");
        p.texto(T.f("animacion.cajas", cuenta, esperado), x + 14, suelo - 150, ui.letra * 0.85f, false, ui.tema.texto, 1);
        if (obtenido < 0 || esperado < 0) p.texto(T.t("animacion.negativo"), x + 14, suelo - 126, ui.letra * 0.8f, false, ui.tema.textoSuave, 1);
    }

    private static void caja(Pincel p, float x0, float suelo, int i, int porFila, float lado, int color, float alfa) {
        float cx = x0 + (i % porFila) * (lado + 2), cy = suelo - (i / porFila + 1) * (lado + 2);
        objeto(p, cx, cy, lado, color, alfa);
    }

    private static void puertas(Ui ui, float x, float suelo, float w, float h, boolean esperado, boolean obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float pw = 90, ph = Math.min(120, h - 60);
        float[] px = {x + w * 0.35f, x + w * 0.7f};
        String[] nombre = {"true", "false"};
        for (int i = 0; i < 2; i++) {
            boolean laBuena = (i == 0) == esperado;
            p.caja(px[i] - pw / 2, suelo - ph, pw, ph, 6, Pincel.oscurecer(0x8C6A43, laBuena ? 0 : 0.3f), 1);
            if (laBuena) p.bordeRedondeado(px[i] - pw / 2, suelo - ph, pw, ph, 6, VERDE, 0.7f, 2);
            p.textoCentrado(nombre[i], px[i], suelo - ph + 10, ui.letra, true, 0xFFF1C1, 1);
        }
        float destino = obtenido ? px[0] : px[1];
        float rx = x + 50 + (destino - x - 50) * Math.min(1, f * 1.3f);
        robot(p, rx, suelo, f >= 0.77f ? 1 : 0, f, bien);
        if (f >= 0.77f && !bien) p.textoCentrado("✘", destino, suelo - ph - 30, ui.letra * 1.4f, true, ROJO, 1);
    }

    private static void estanteria(Ui ui, float x, float suelo, float w, List<String> esperado, List<String> obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float ey = suelo - 70;
        p.rect(x + 20, ey, w - 40, 6, 0x6B4F33, 1);
        p.rect(x + 20, suelo - 6, w - 40, 6, 0x6B4F33, 1);
        int n = Math.max(esperado.size(), obtenido.size());
        float hueco = n == 0 ? 0 : Math.min(110, (w - 80) / n);
        int puestos = Math.round(obtenido.size() * Math.min(1, f * 1.15f));
        float t = ui.letra * 0.8f;
        for (int i = 0; i < n; i++) {
            float cx = x + 40 + i * hueco;
            if (i < esperado.size()) {
                p.caja(cx, ey - 44, hueco - 8, 40, 5, 0x8A919C, 0.25f);
                p.textoCentrado(ui.recortarTexto(esperado.get(i), hueco - 12, t, false), cx + (hueco - 8) / 2, ey - 36, t, false, ui.tema.textoApagado, 1);
            }
            if (i < puestos) {
                boolean igual = i < esperado.size() && esperado.get(i).equals(obtenido.get(i));
                p.caja(cx, ey + 8, hueco - 8, 40, 5, igual ? 0x3FA9F5 : ROJO, 1);
                p.textoCentrado(ui.recortarTexto(obtenido.get(i), hueco - 12, t, true), cx + (hueco - 8) / 2, ey + 16, t, true, 0xFFFFFF, 1);
            }
        }
        if (n == 0) p.textoCentrado(T.t("animacion.vacia"), x + w / 2, ey - 30, ui.letra * 0.85f, false, ui.tema.textoSuave, 1);
        float rx = x + 40 + Math.max(0, puestos - 0.5f) * hueco;
        robot(p, Math.min(x + w - 30, rx), suelo, 0.6f, f, bien);
    }

    private static void pizarra(Ui ui, float x, float y, float w, float h, String esperado, String obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        float bx = x + 120, by = y + 40, bw = w - 150, bh = h - 40 - ui.letra * 2.2f;
        p.caja(bx, by, bw, bh, 6, 0x1F3A2E, 1);
        p.bordeRedondeado(bx, by, bw, bh, 6, 0x8C6A43, 1, 4);
        float t = Math.min(ui.letra * 1.1f, 26);
        p.texto(ui.recortarTexto(T.t("animacion.esperaba") + " " + esperado, bw - 24, t * 0.8f, false), bx + 12, by + 10, t * 0.8f, false, 0x9FB8AA, 1);
        int letras = Math.round(obtenido.length() * Math.min(1, f * 1.15f));
        String escrito = obtenido.substring(0, letras);
        int igual = 0;
        while (igual < escrito.length() && igual < esperado.length() && escrito.charAt(igual) == esperado.charAt(igual)) igual++;
        float ty = by + bh / 2;
        p.texto(escrito.substring(0, igual), bx + 12, ty, t, true, 0xF5F5F0, 1);
        float cx = bx + 12 + p.ancho(escrito.substring(0, igual), t, true);
        p.texto(ui.recortarTexto(escrito.substring(igual), bx + bw - 20 - cx, t, true), cx, ty, t, true, ROJO, 1);
        if (igual < escrito.length() || letras == obtenido.length() && !bien) p.rect(cx, ty + t * 1.25f, Math.max(12, bw - 24 - (cx - bx)) * 0.3f, 3, ROJO, 1);
        robot(p, x + 60, y + h - 26, letras < obtenido.length() ? (float) Math.abs(Math.sin(f * 30)) : 0.3f, f, bien);
    }

    private static void papel(Ui ui, float x, float y, float w, float h, String esperado, String obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        String[] esp = esperado.split("\n", -1), obt = obtenido.split("\n", -1);
        float t = ui.letra * 0.8f, alto = p.altoDeLinea(t);
        float bx = x + 110, bw = (w - 150) / 2;
        int lineas = Math.round(obt.length * Math.min(1, f * 1.15f));
        float py = y + 40, ph = h - 40 - ui.letra * 2;
        p.texto(T.t("animacion.salio"), bx, py, t, true, ui.tema.textoSuave, 1);
        p.texto(T.t("animacion.esperaba"), bx + bw + 20, py, t, true, ui.tema.textoSuave, 1);
        py += alto + 4;
        ph -= alto + 4;
        p.caja(bx, py, bw, ph, 4, 0xF2EEE3, 1);
        p.caja(bx + bw + 20, py, bw, ph, 4, 0xF2EEE3, 0.5f);
        for (int i = 0; i < esp.length && py + 6 + (i + 1) * alto < py + ph; i++) {
            p.texto(ui.recortarTexto(esp[i], bw - 16, t, false), bx + bw + 28, py + 6 + i * alto, t, false, 0x55524A, 1);
        }
        for (int i = 0; i < lineas && py + 6 + (i + 1) * alto < py + ph; i++) {
            boolean igual = i < esp.length && esp[i].equals(obt[i]);
            p.texto(ui.recortarTexto(obt[i], bw - 16, t, false), bx + 8, py + 6 + i * alto, t, false, igual ? 0x1E1E1E : ROJO, 1);
        }
        robot(p, x + 60, y + h - 26, lineas < obt.length ? (float) Math.abs(Math.sin(f * 30)) : 0.3f, f, bien);
    }

    private static void alarma(Ui ui, float x, float suelo, float w, float h, String esperado, String obtenido, float f, boolean bien) {
        Pincel p = ui.p;
        boolean luz = (int) (f * 10) % 2 == 0;
        p.circulo(x + w / 2, suelo - h + 70, 18, luz ? ROJO : 0x7A2A28, 1);
        p.texto(T.f("animacion.alarma", esperado), x + 110, suelo - 70, ui.letra * 0.85f, false, ui.tema.texto, 1);
        p.texto(ui.recortarTexto(T.t("animacion.salio") + " " + obtenido, w - 130, ui.letra * 0.85f, false), x + 110, suelo - 44, ui.letra * 0.85f,
                false, bien ? VERDE : ROJO, 1);
        robot(p, x + 60, suelo, luz ? 1 : 0, f, bien);
    }

    // ================================================================ leer las respuestas

    static long numero(String s) {
        try {
            return Long.parseLong(s.strip().replace("L", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Un texto entre comillas, sin ellas y con los \n de verdad. */
    static String texto(String s) {
        String t = s.strip();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) t = t.substring(1, t.length() - 1);
        return t.replace("\\n", "\n").replace("\\\"", "\"").replace("\\t", "\t");
    }

    /** Lo de dentro de «List.of(…)», «new int[]{…}», «[…]» o «{…}». */
    static String dentro(String s) {
        String t = s.strip();
        int a = t.indexOf('{') >= 0 && (t.indexOf('(') < 0 || t.indexOf('{') < t.indexOf('(')) ? t.indexOf('{') : t.indexOf('(');
        if (t.startsWith("[")) a = 0;
        int b = Math.max(t.lastIndexOf('}'), Math.max(t.lastIndexOf(')'), t.lastIndexOf(']')));
        return a >= 0 && b > a ? t.substring(a + 1, b) : t;
    }

    /** Los elementos de una lista escrita «1, 2, "a"» (las comas de dentro de textos o paréntesis no cuentan). */
    static List<String> elementos(String s) {
        List<String> l = new ArrayList<>();
        if (s.isBlank()) return l;
        int nivel = 0;
        boolean comillas = false;
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"') comillas = !comillas;
            if (!comillas && (c == '(' || c == '[' || c == '{')) nivel++;
            if (!comillas && (c == ')' || c == ']' || c == '}')) nivel--;
            if (c == ',' && nivel == 0 && !comillas) {
                l.add(texto(b.toString()));
                b.setLength(0);
            } else {
                b.append(c);
            }
        }
        l.add(texto(b.toString()));
        return l;
    }
}
