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
            case CAJAS: AccionesDelRobotito.cajas(ui, x, suelo, w, numero(esperado), numero(obtenido), f, r.bien); break;
            case PUERTAS:
                AccionesDelRobotito.puertas(ui, x, suelo, w, h, esperado.strip().equals("true"), obtenido.strip().equals("true"), f, r.bien);
                break;
            case ESTANTERIA: AccionesDelRobotito.estanteria(ui, x, suelo, w, elementos(dentro(esperado)), elementos(dentro(obtenido)), f, r.bien); break;
            case PAPEL: AccionesDelRobotito.escribir(ui, x, y, w, h, texto(esperado), texto(obtenido), f, r.bien, true, paseo(e, r)); break;
            case ALARMA: AccionesDelRobotito.alarma(ui, x, suelo, w, h, esperado.substring(1), obtenido, f, r.bien); break;
            default: AccionesDelRobotito.escribir(ui, x, y, w, h, texto(esperado), texto(obtenido), f, r.bien, false, paseo(e, r)); break;
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
        if (brazo < 0) return;
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

    /**
     * El robotito con la mano en (hx, hy): el brazo sale del hombro y se estira lo que haga falta (es telescópico: se le
     * ven los tramos). Al acabar (f = 1), uno de sus finales.
     */
    static void robotMano(Pincel p, float x, float suelo, float hx, float hy, float f, boolean bien) {
        if (f >= 1) {
            robot(p, x, suelo, 0.3f, f, bien);
            return;
        }
        acabo = -1;
        float tropiezo = EscenasDelRobotito.tropiezo(escenaActual, f);
        x += tropiezo * 10;
        suelo += tropiezo * 8;
        robotito(p, x, suelo, -1, tropiezo < 0.5f ? VERDE : ROJO, 0x3FA9F5);
        float k = 1.6f, sx = x + 13 * k, sy = suelo - 26 * k;
        float largo = (float) Math.hypot(hx - sx, hy - sy);
        int tramos = Math.max(1, (int) (largo / 22));
        for (int i = 0; i < tramos; i++) {
            float a = i / (float) tramos, b = (i + 1) / (float) tramos;
            p.linea(sx + (hx - sx) * a, sy + (hy - sy) * a, sx + (hx - sx) * b, sy + (hy - sy) * b, (3.4f - i * 0.3f / tramos) * k,
                    Pincel.oscurecer(0x3FA9F5, 0.2f + 0.1f * (i % 2)), 1);
        }
        p.circulo(hx, hy, 3 * k, 0xAAB0BA, 1);
        if (tropiezo > 0.3f) p.texto("!", x - 6, suelo - 120, 24, true, 0xFFD23F, 1);
    }

    /** Un objeto del escenario (lo que el robotito apila o coloca), según la escena. */
    static void objeto(Pincel p, float cx, float cy, float lado, int color, float alfa) {
        EscenasDelRobotito.objeto(p, escenaActual, cx, cy, lado, color, CAJA, alfa);
    }

    // ================================================================ cuánto dura

    /**
     * Lo que tarda el robotito en hacer un caso (s), a buen ritmo pero viendo cada movimiento: un viaje por caja o por
     * cosa de la estantería; en la pizarra y el papel, el paseo y unas letras por segundo.
     */
    static float duracion(Ejercicio e, Comprobador.Resultado r) {
        String esp = r.caso.esperado, obt = r.obtenido == null ? "" : r.obtenido;
        switch (forma(e.tipo, esp)) {
            case CAJAS: return AccionesDelRobotito.viajesDeCajas(numero(esp), numero(obt)) * AccionesDelRobotito.VIAJE + 0.3f;
            case ESTANTERIA:
                int n = elementos(dentro(obt)).size(), m = elementos(dentro(esp)).size();
                return Math.max(1, n + (n < m ? 1 : 0)) * AccionesDelRobotito.VIAJE * 1.15f + 0.3f;
            case PIZARRA: case PAPEL: return AccionesDelRobotito.PASEO + letrasPorEscribir(e, r) / velocidadDeEscritura(e, r);
            default: return 2.6f;
        }
    }

    private static int letrasPorEscribir(Ejercicio e, Comprobador.Resultado r) {
        String obt = texto(r.obtenido == null ? "" : r.obtenido);
        return AccionesDelRobotito.letrasDe(forma(e.tipo, r.caso.esperado) == Forma.PAPEL ? obt.split("\n", -1) : new String[] {obt});
    }

    /** Letras por segundo: 14 de normal; si es muy largo, más deprisa (que no pase de unos 6 s). */
    private static float velocidadDeEscritura(Ejercicio e, Comprobador.Resultado r) {
        return Math.max(14, letrasPorEscribir(e, r) / 5.5f);
    }

    /** La parte del caso que se va en el paseo hasta la pizarra. */
    private static float paseo(Ejercicio e, Comprobador.Resultado r) {
        return AccionesDelRobotito.PASEO / duracion(e, r);
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
