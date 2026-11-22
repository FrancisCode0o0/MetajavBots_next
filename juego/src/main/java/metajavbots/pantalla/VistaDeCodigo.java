package metajavbots.pantalla;

import java.util.List;

import metajavbots.codigo.Documento;
import metajavbots.codigo.Lexico;

/**
 * VISTA DE CÓDIGO — Geometría exacta del margen de números (gutter), recorte del área útil de texto
 * y cálculo determinista del desplazamiento horizontal y vertical de {@link VentanaDeCodigo}.
 */
public final class VistaDeCodigo {

    /** Rectángulos de reparto del editor: margen de números (gutter) y área útil de texto (clip). */
    public record Geometria(float anchoGutter, float zx, float zy, float zw, float zh,
            float clipX, float clipY, float clipW, float clipH) {
    }

    /** Resultado de ajustar el desplazamiento horizontal y vertical en un fotograma. */
    public record Desplazamiento(float x, float y, float maxX, float maxY) {
    }

    private VistaDeCodigo() {
    }

    /** Calcula el margen de números y el rectángulo estricto de recorte del área de código. */
    public static Geometria geometria(float x, float y, float w, float h, float alto, float letra,
            int lineas, boolean buscando, boolean reemplazando) {
        float margen = letra * (String.valueOf(Math.max(1, lineas)).length() + 3.5f);
        float anchoGutter = Math.max(0, margen - letra * 0.8f);
        float barra = buscando ? alto * (reemplazando ? 2.4f : 1.4f) : 0;
        float zx = x + margen;
        float zy = y + barra;
        float zw = Math.max(0, w - margen - 12);
        float zh = Math.max(0, h - barra - 12);
        return new Geometria(anchoGutter, zx, zy, zw, zh, zx, zy, zw, zh);
    }

    /** Desplazamiento horizontal máximo posible para el documento actual (0 si todo cabe). */
    public static float maxX(int columnasMaximas, float zw, float letra) {
        if (zw <= 0 || letra <= 0) return 0;
        return Math.max(0, columnasMaximas * letra + letra * 4 - zw);
    }

    /** Desplazamiento vertical máximo posible para el documento actual (0 si todo cabe). */
    public static float maxY(int lineas, float zh, float alto) {
        if (zh <= 0 || alto <= 0) return 0;
        return Math.max(0, lineas * alto - zh + alto * 2);
    }

    /** El tirador de una barra: dónde empieza y cuánto mide en un carril que enseña «visible» de «visible + max». */
    public static float[] tirador(float carril, float visible, float max, float desplazamiento) {
        float largo = Math.max(30, carril * visible / Math.max(1, visible + max));
        return new float[]{(carril - largo) * (max <= 0 ? 0 : desplazamiento / max), largo};
    }

    /**
     * Recalcula y acota el desplazamiento horizontal y vertical. Si la línea actual cabe entera en el ancho visible,
     * devuelve el desplazamiento horizontal a 0; siempre acota X en [0, maxX] e Y en [0, maxY].
     */
    public static Desplazamiento ajustar(Documento doc, float despX, float despY, float zw, float zh, float alto, float letra,
            int columnasMaximas, boolean seguirCursor, boolean seguirActiva, int lineaActiva) {
        float x = despX;
        float y = despY;
        float maxX = maxX(columnasMaximas, zw, letra);
        float maxY = maxY(doc.lineas(), zh, alto);
        if (seguirCursor && zw > 0 && zh > 0) {
            int linea = doc.lineaDelCursor();
            float cy = linea * alto;
            float cx = doc.columnaDelCursor() * letra;
            float anchoLinea = doc.linea(linea).length() * letra;
            if (cy < y) y = cy;
            if (cy + alto * 2 > y + zh) y = cy + alto * 2 - zh;
            if (anchoLinea + letra <= zw) {
                x = 0;
            } else {
                float margenIzq = Math.min(letra * 2, zw * 0.2f);
                float margenDer = Math.min(letra * 4, zw * 0.25f);
                float colchonIzq = Math.min(letra * 6, zw * 0.3f);
                float colchonDer = Math.min(letra * 8, zw * 0.4f);
                if (cx < x + margenIzq) x = Math.max(0, cx - colchonIzq);
                if (cx > x + zw - margenDer) x = Math.max(0, cx - zw + colchonDer);
            }
        }
        if (seguirActiva && lineaActiva > 0) {
            float ay = (lineaActiva - 1) * alto;
            if (ay < y + alto * 2 || ay > y + zh - alto * 3) y = ay - zh / 2;
        }
        y = Math.max(0, Math.min(maxY, y));
        x = Math.max(0, Math.min(maxX, x));
        return new Desplazamiento(x, y, maxX, maxY);
    }

    /** Índice del primer trozo léxico que acaba después de «pos». */
    static int buscarTrozo(List<Lexico.Trozo> trozos, int pos) {
        int lo = 0, hi = trozos.size();
        while (lo < hi) {
            int m = (lo + hi) >>> 1;
            if (trozos.get(m).fin <= pos) lo = m + 1;
            else hi = m;
        }
        return lo;
    }

    /** Color RGB de resaltado de sintaxis para un trozo léxico. */
    static int colorDe(Tema t, List<Lexico.Trozo> trozos, Lexico.Trozo tr, int k) {
        switch (tr.clase) {
            case CLAVE: return t.clave;
            case TIPO_PRIMITIVO: return t.tipo;
            case NUMERO: return t.numero;
            case TEXTO: case LETRA: case BLOQUE_DE_TEXTO: return t.cadena;
            case COMENTARIO: case COMENTARIO_BLOQUE: case JAVADOC: return t.comentario;
            case ANOTACION: return t.anotacion;
            case OPERADOR: return t.operador;
            case NOMBRE:
                if (k + 1 < trozos.size() && trozos.get(k + 1).es("(")) return t.metodo;
                if (!tr.texto.isEmpty() && Character.isUpperCase(tr.texto.charAt(0))) return t.tipo;
                return t.nombre;
            default: return t.texto;
        }
    }

    /** Dibuja el subrayado ondulado de errores/avisos en el código. */
    static void ondas(Pincel p, float x, float y, float w, int color) {
        float paso = 3;
        for (float i = 0; i < w; i += paso) {
            float a = (i / paso) % 2 == 0 ? 0 : 2.5f, b = (i / paso) % 2 == 0 ? 2.5f : 0;
            p.linea(x + i, y + a, x + Math.min(w, i + paso), y + b, 1.5f, color, 1);
        }
    }

    private static final String[] NUMEROS_LINEA = crearNumerosLinea(256);

    private static String[] crearNumerosLinea(int n) {
        String[] res = new String[n];
        for (int i = 0; i < n; i++) res[i] = Integer.toString(i + 1);
        return res;
    }

    /** Texto del número de línea (1-based), reutilizando cadenas frecuentes sin asignar por fotograma. */
    static String numeroDeLinea(int lineaCero) {
        return lineaCero >= 0 && lineaCero < NUMEROS_LINEA.length ? NUMEROS_LINEA[lineaCero] : Integer.toString(lineaCero + 1);
    }

    /** Construye la cadena de color con marcas de BitmapFont para una línea de código (se cachea por versión y tema). */
    static String lineaMarcada(Documento doc, Tema t, List<Lexico.Trozo> trozos, int l) {
        int ini = doc.inicioDeLinea(l), fin = doc.finDeLinea(l);
        String texto = doc.linea(l);
        if (texto.isEmpty()) return "";
        StringBuilder b = new StringBuilder(texto.length() * 2 + 16);
        int col = 0;
        int k = buscarTrozo(trozos, ini);
        while (col < texto.length()) {
            int pos = ini + col;
            Lexico.Trozo tr = k < trozos.size() ? trozos.get(k) : null;
            int hasta;
            int color;
            if (tr != null && tr.inicio <= pos && tr.fin > pos) {
                hasta = Math.min(fin, tr.fin) - ini;
                color = colorDe(t, trozos, tr, k);
                if (tr.fin <= fin) k++;
            } else {
                int siguiente = tr != null && tr.inicio > pos ? Math.min(fin, tr.inicio) : fin;
                hasta = siguiente - ini;
                color = t.texto;
            }
            if (hasta <= col) hasta = col + 1;
            int lim = Math.min(hasta, texto.length());
            Pincel.anadirTrozoMarcado(b, texto.substring(col, lim), color & 0xFFFFFF);
            col = hasta;
        }
        return b.toString();
    }

    /** Carácter bajo la coordenada horizontal «rx» en un campo de búsqueda de una línea. */
    static int indiceEnLinea(Pincel p, float rx, float tam, String s) {
        if (rx <= 0 || s.isEmpty()) return 0;
        for (int i = 0; i < s.length(); i++) if (p.anchoDeCodigo(s.substring(0, i + 1), tam, false) >= rx) return i;
        return s.length();
    }
}
