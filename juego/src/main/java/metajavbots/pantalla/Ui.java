package metajavbots.pantalla;

import com.badlogic.gdx.Input;

/**
 * UI — Los elementos de la interfaz, «al momento» (se dibujan y se preguntan
 * en la misma llamada): botones, pestañas, deslizadores, casillas, barras,
 * avisos que salen al dejar el ratón encima… Todos grandes y con letra
 * clara. Lo que se desplaza guarda su estado en un Desplazable.
 */
public final class Ui {

    public final Pincel p;
    public final Entrada e;
    public Tema tema = Tema.OSCURO;
    /** El tamaño de la letra de la interfaz (el del código va aparte). */
    public float letra = 18;
    /** Lo que suena al pulsar (lo pone el juego; puede ser null). */
    public Runnable sonidoDeClic;
    /** El aviso que sale al dejar el ratón quieto encima de algo (se dibuja al final del fotograma). */
    private String aviso;
    private float avisoX, avisoY;
    /** Lo reservó alguien este fotograma o el anterior (la cabecera pregunta antes de que se dibuje el campo). */
    private boolean escapeReservado, escapeReservadoAntes;
    private Object focoActual;
    private Runnable desenfocarActual;
    private boolean focoVisto;

    public Ui(Pincel p, Entrada e) {
        this.p = p;
        this.e = e;
    }

    /** El error del editor que se mira en este fotograma y el del anterior (VentanaDeCodigo.Senalado; el entrenador flotante lo usa). */
    public VentanaDeCodigo.Senalado errorSenalado, errorSenaladoAntes;

    public void empezarFotograma() {
        errorSenaladoAntes = errorSenalado;
        errorSenalado = null;
        aviso = null;
        escapeReservadoAntes = escapeReservado;
        escapeReservado = false;
        focoVisto = false;
    }

    /** Registra qué componente tiene el foco exclusivo del teclado en este fotograma. */
    public void registrarFoco(Object quien, Runnable desenfocar) {
        if (quien == null) return;
        if (focoActual != null && focoActual != quien && desenfocarActual != null) {
            desenfocarActual.run();
        }
        focoActual = quien;
        desenfocarActual = desenfocar;
        focoVisto = true;
    }

    /** Suelta el foco si pertenece a «quien». */
    public void desenfocar(Object quien) {
        if (focoActual == quien) {
            if (desenfocarActual != null) desenfocarActual.run();
            focoActual = null;
            desenfocarActual = null;
            focoVisto = false;
        }
    }

    /** ¿Hay otro componente distinto con el foco activo? */
    public boolean tieneOtroFoco(Object quien) {
        return focoActual != null && focoActual != quien;
    }

    /** Quita el foco de cualquier componente activo (al cambiar de pantalla o pestaña). */
    public void quitarFoco() {
        if (desenfocarActual != null) desenfocarActual.run();
        focoActual = null;
        desenfocarActual = null;
        focoVisto = false;
    }

    /** Alguien usa Esc para lo suyo (un campo enfocado, una lista de propuestas): ese Esc no vuelve atrás. */
    public void reservarEscape() {
        escapeReservado = true;
    }

    /** Al final del fotograma: limpia focos que dejaron de dibujarse y muestra el aviso encima de todo. */
    public void acabarFotograma() {
        if (focoActual != null && !focoVisto) quitarFoco();
        if (aviso == null || e.quieto < 0.5f) return;
        float t = letra * 0.85f;
        float w = Math.min(520, p.ancho(aviso, t, false) + 24);
        float h = p.altoDeParrafo(aviso, w - 24, t) + 16;
        float x = Math.min(avisoX, p.ancho - w - 8), y = avisoY + 24;
        if (y + h > p.alto - 8) y = avisoY - h - 8;
        p.caja(x + 3, y + 4, w, h, 8, 0x000000, 0.35f);
        p.caja(x, y, w, h, 8, tema.panelAlto, 0.98f);
        p.bordeRedondeado(x, y, w, h, 8, tema.borde, 1, 1.5f);
        p.parrafo(aviso, x + 12, y + 8, w - 24, t, tema.texto, 1);
    }

    public void avisar(String texto) {
        aviso = texto;
        avisoX = e.ratonX;
        avisoY = e.ratonY;
    }

    /** Escribe un texto con contorno negro fino de 1 px detrás (helper reutilizable para menús). */
    public float textoConBorde(String s, float x, float y, float tamano, boolean negrita, int rgb, float alfa) {
        return p.textoConBordeNegro(s, x, y, tamano, negrita, rgb, alfa);
    }

    /** Escribe un texto centrado con contorno negro fino de 1 px detrás. */
    public float textoCentradoConBorde(String s, float cx, float y, float tamano, boolean negrita, int rgb, float alfa) {
        return p.textoCentradoConBordeNegro(s, cx, y, tamano, negrita, rgb, alfa);
    }

    /** Dibuja un pequeño icono vectorial de candado cerrado sin depender de glifos emoji. */
    public void candado(float cx, float cy, float tam, int rgb, float alfa) {
        float w = tam * 0.78f;
        float h = tam * 0.56f;
        float r = tam * 0.25f;
        p.aro(cx, cy - h * 0.42f, r, Math.max(1.5f, tam * 0.12f), rgb, alfa);
        p.caja(cx - w / 2f, cy - h * 0.15f, w, h, Math.max(2f, tam * 0.12f), rgb, alfa);
        p.circulo(cx, cy + h * 0.30f, Math.max(1.2f, tam * 0.09f), tema.panel, alfa);
    }

    // ================================================================ botones

    /** Colores de alto contraste (rojo con letras amarillas) para el botón de compilar. */
    public static final int COMPILAR_FONDO = 0xB8141A;
    public static final int COMPILAR_FONDO_ENCIMA = 0xD62229;
    public static final int COMPILAR_FONDO_PULSADO = 0x8F1015;
    public static final int COMPILAR_TEXTO = 0xFFE838;

    /** El aspecto de un botón: normal, principal, peligroso, plano, elegido, destacado (amarillo) o compilar (rojo con letras amarillas). */
    public enum Estilo { NORMAL, PRINCIPAL, PELIGRO, PLANO, ELEGIDO, DESTACADO, COMPILAR }

    public boolean boton(String texto, float x, float y, float w, float h) {
        return boton(texto, x, y, w, h, Estilo.NORMAL, true, null);
    }

    public boolean boton(String texto, float x, float y, float w, float h, Estilo estilo) {
        return boton(texto, x, y, w, h, estilo, true, null);
    }

    /** Un botón; devuelve true el fotograma en que se pulsa. Con «ayuda», un aviso al dejar el ratón encima. */
    public boolean boton(String texto, float x, float y, float w, float h, Estilo estilo, boolean activo, String ayuda) {
        boolean encima = activo && e.dentro(x, y, w, h);
        boolean apretado = encima && e.pulsado;
        int fondo, letraColor = tema.texto;
        switch (estilo) {
            case PRINCIPAL:
                fondo = tema.acento;
                letraColor = tema.acentoTexto;
                break;
            case PELIGRO:
                fondo = tema.mal;
                letraColor = 0xFFFFFF;
                break;
            case PLANO:
                fondo = tema.panel;
                break;
            case ELEGIDO:
                fondo = Pincel.mezcla(tema.panelAlto, tema.acento, 0.35f);
                break;
            case DESTACADO:
                fondo = 0xFFD23F;
                letraColor = 0x14161A;
                break;
            case COMPILAR:
                fondo = COMPILAR_FONDO;
                letraColor = COMPILAR_TEXTO;
                break;
            default:
                fondo = tema.panelAlto;
                break;
        }
        if (!activo) {
            fondo = tema.panel;
            letraColor = tema.textoApagado;
        } else if (apretado) {
            fondo = estilo == Estilo.COMPILAR ? COMPILAR_FONDO_PULSADO : Pincel.oscurecer(fondo, 0.15f);
        } else if (encima) {
            fondo = estilo == Estilo.COMPILAR ? COMPILAR_FONDO_ENCIMA : Pincel.aclarar(fondo, estilo == Estilo.PRINCIPAL ? 0.12f : 0.08f);
        }
        float desplaza = apretado ? 1.5f : 0;
        if (estilo != Estilo.PLANO) p.caja(x, y + 2, w, h, 8, 0x000000, 0.25f);
        p.caja(x, y + desplaza, w, h, 8, fondo, 1);
        if (estilo == Estilo.COMPILAR && activo) {
            p.bordeRedondeado(x, y + desplaza, w, h, 8, COMPILAR_TEXTO, encima ? 1f : 0.72f, encima ? 2f : 1.5f);
        } else if (encima || estilo == Estilo.ELEGIDO) {
            p.bordeRedondeado(x, y + desplaza, w, h, 8, estilo == Estilo.ELEGIDO ? tema.acento : tema.borde, 1, 1.5f);
        }
        // Si no cabe: en un botón alto, en dos líneas con la letra entera; si no, con la letra algo menor; y si ni así, cortado.
        float t = Math.min(letra, h * 0.5f);
        java.util.List<String> lineas = p.ancho(texto, t, true) > w - 16 && h >= t * 2.9f ? p.partir(texto, w - 16, t, true) : null;
        if (lineas == null || lineas.size() != 2) t = cabe(texto, w - 16, t, true);
        if (lineas != null && lineas.size() == 2) {
            // Un botón alto con un texto largo: en dos líneas, entero.
            float ly = y + desplaza + (h - 2 * p.altoDeLinea(t)) / 2;
            for (String l : lineas) {
                p.textoCentradoConBordeNegro(l, x + w / 2, ly, t, true, letraColor, 1);
                ly += p.altoDeLinea(t);
            }
        } else {
            String s = recortarTexto(texto, w - 16, t, true);
            p.textoCentradoConBordeNegro(s, x + w / 2, y + desplaza + (h - t * 1.15f) / 2, t, true, letraColor, 1);
        }
        if (encima && ayuda != null) avisar(ayuda);
        if (activo && e.clic(x, y, w, h)) {
            if (sonidoDeClic != null) sonidoDeClic.run();
            return true;
        }
        return false;
    }

    /** El alto que ocupó la última botonera (una fila o más). */
    public float altoDeBotonera;

    /**
     * Una fila de botones, cada uno del ancho de su texto (nada cortado); si no caben, siguen en otra fila debajo.
     * Devuelve cuál se pulsó (o -1); el alto que ocupan queda en altoDeBotonera.
     */
    public int botonera(String[] textos, Estilo[] estilos, boolean[] activos, String[] ayudas, float x, float y, float w, float h) {
        float t = Math.min(letra, h * 0.5f);
        float bx = x, by = y;
        int pulsado = -1;
        for (int i = 0; i < textos.length; i++) {
            float bw = Math.min(w, p.ancho(textos[i], t, true) + 34);
            if (bx > x && bx + bw > x + w) {
                bx = x;
                by += h + 8;
            }
            if (boton(textos[i], bx, by, bw, h, estilos[i], activos == null || activos[i], ayudas == null ? null : ayudas[i])) pulsado = i;
            bx += bw + 8;
        }
        altoDeBotonera = by + h - y;
        return pulsado;
    }

    /** El tamaño de letra con que cabe un texto en ese ancho: el de siempre o algo menor (hasta tres cuartos); si ni así, se cortará. */
    public float cabe(String s, float anchoMax, float t, boolean negrita) {
        if (s == null) return t;
        float w = p.ancho(s, t, negrita);
        return w <= anchoMax ? t : Math.max(t * 0.75f, t * anchoMax / w);
    }

    /** El tamaño de letra de código con que cabe un texto monoespaciado en ese ancho. */
    public float cabeDeCodigo(String s, float anchoMax, float t, boolean negrita) {
        if (s == null) return t;
        float w = p.anchoDeCodigo(s, t, negrita);
        return w <= anchoMax ? t : Math.max(t * 0.75f, t * anchoMax / w);
    }

    /** Un texto que no cabe se corta con «…». */
    public String recortarTexto(String s, float anchoMax, float t, boolean negrita) {
        return recortarConFuente(s, anchoMax, t, negrita, false);
    }

    /** Un texto de código que no cabe se corta con «…» usando métricas monoespaciadas. */
    public String recortarTextoDeCodigo(String s, float anchoMax, float t, boolean negrita) {
        return recortarConFuente(s, anchoMax, t, negrita, true);
    }

    private String recortarConFuente(String s, float anchoMax, float t, boolean negrita, boolean codigo) {
        if (s == null) return "";
        if ((codigo ? p.anchoDeCodigo(s, t, negrita) : p.ancho(s, t, negrita)) <= anchoMax) return s;
        int anchoQ = Math.round(anchoMax);
        int tamQ = Math.round(t * 10);
        int escQ = Math.round(p.escala * 100);
        sondaRecorte.fijar(s, anchoQ, tamQ, escQ, negrita, codigo);
        String hecho = recortados.get(sondaRecorte);
        if (hecho != null) return hecho;
        // Se busca el corte por mitades (medir letra a letra cada fotograma hacía lenta la lista de ejercicios).
        int bien = 1, mal = s.length();
        while (mal - bien > 1) {
            int n = (bien + mal) / 2;
            String prueba = s.substring(0, n) + "…";
            if ((codigo ? p.anchoDeCodigo(prueba, t, negrita) : p.ancho(prueba, t, negrita)) <= anchoMax) bien = n;
            else mal = n;
        }
        if (recortados.size() > 600) recortados.clear();
        hecho = s.substring(0, bien) + "…";
        recortados.put(new ClaveRecorte(s, anchoQ, tamQ, escQ, negrita, codigo), hecho);
        return hecho;
    }

    /** Clave sin asignaciones en consulta para el caché de textos recortados. */
    private static final class ClaveRecorte {
        String s;
        int anchoQ, tamQ, escQ;
        boolean negrita, codigo;
        int hash;

        ClaveRecorte() {
        }

        ClaveRecorte(String s, int anchoQ, int tamQ, int escQ, boolean negrita, boolean codigo) {
            fijar(s, anchoQ, tamQ, escQ, negrita, codigo);
        }

        void fijar(String s, int anchoQ, int tamQ, int escQ, boolean negrita, boolean codigo) {
            this.s = s;
            this.anchoQ = anchoQ;
            this.tamQ = tamQ;
            this.escQ = escQ;
            this.negrita = negrita;
            this.codigo = codigo;
            int h = s.hashCode();
            h = 31 * h + anchoQ;
            h = 31 * h + tamQ;
            h = 31 * h + escQ;
            h = 31 * h + (negrita ? 1 : 0) + (codigo ? 2 : 0);
            this.hash = h;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClaveRecorte c)) return false;
            return anchoQ == c.anchoQ && tamQ == c.tamQ && escQ == c.escQ
                    && negrita == c.negrita && codigo == c.codigo && s.equals(c.s);
        }
    }

    /** Los textos ya recortados (los mismos en cada fotograma). */
    private final java.util.Map<ClaveRecorte, String> recortados = new java.util.HashMap<>();
    private final ClaveRecorte sondaRecorte = new ClaveRecorte();

    /** Pestañas en fila; devuelve la elegida. */
    public int pestanas(String[] nombres, int elegida, float x, float y, float w, float h) {
        float ancho = w / nombres.length;
        int nueva = elegida;
        for (int i = 0; i < nombres.length; i++) {
            float bx = x + i * ancho;
            boolean es = i == elegida;
            boolean encima = e.dentro(bx, y, ancho, h);
            p.rect(bx, y, ancho - 2, h, es ? tema.panelAlto : encima ? Pincel.mezcla(tema.panel, tema.panelAlto, 0.5f) : tema.panel, 1);
            if (es) p.rect(bx, y + h - 3, ancho - 2, 3, tema.acento, 1);
            float t = cabe(nombres[i], ancho - 12, Math.min(letra, h * 0.48f), es);
            p.textoCentradoConBordeNegro(recortarTexto(nombres[i], ancho - 12, t, es), bx + ancho / 2, y + (h - t * 1.15f) / 2, t, es,
                    es ? tema.texto : tema.textoSuave, 1);
            if (e.clic(bx, y, ancho, h)) {
                nueva = i;
                if (sonidoDeClic != null) sonidoDeClic.run();
            }
        }
        return nueva;
    }

    /** Un deslizador de 0 a 1. */
    public float deslizador(String etiqueta, float valor, float x, float y, float w, float h, String ayuda) {
        float t = letra;
        p.textoConBordeNegro(etiqueta, x, y, t, false, tema.texto, 1);
        float by = y + t * 1.5f, bh = Math.max(8, h - t * 1.5f);
        float cy = by + bh / 2;
        p.caja(x, cy - 4, w, 8, 4, tema.panelAlto, 1);
        p.caja(x, cy - 4, w * valor, 8, 4, tema.acento, 1);
        boolean encima = e.dentro(x - 10, by - 6, w + 20, bh + 12);
        p.circulo(x + w * valor, cy, encima ? 11 : 9, tema.acento, 1);
        p.circulo(x + w * valor, cy, 4, 0xFFFFFF, 0.9f);
        p.textoDerechaConBordeNegro(Math.round(valor * 100) + " %", x + w, y, t * 0.85f, false, tema.textoSuave, 1);
        if (encima && ayuda != null) avisar(ayuda);
        if (encima && e.pulsado) {
            e.clicUsado = true;
            valor = Math.max(0, Math.min(1, (e.ratonX - x) / w));
        }
        if (encima && e.rueda != 0 && !e.ruedaUsada) {
            e.ruedaUsada = true;
            valor = Math.max(0, Math.min(1, valor - e.rueda * 0.05f));
        }
        return valor;
    }

    /** Una casilla de sí o no. */
    public boolean casilla(String texto, boolean valor, float x, float y, String ayuda) {
        float t = letra, lado = t * 1.2f;
        boolean encima = e.dentro(x, y, lado + 12 + p.ancho(texto, t, false), lado);
        p.caja(x, y, lado, lado, 5, valor ? tema.acento : tema.panelAlto, 1);
        p.bordeRedondeado(x, y, lado, lado, 5, encima ? tema.acento : tema.borde, 1, 1.5f);
        if (valor) {
            p.linea(x + lado * 0.22f, y + lado * 0.52f, x + lado * 0.42f, y + lado * 0.74f, 3, tema.acentoTexto, 1);
            p.linea(x + lado * 0.42f, y + lado * 0.74f, x + lado * 0.8f, y + lado * 0.28f, 3, tema.acentoTexto, 1);
        }
        p.textoConBordeNegro(texto, x + lado + 12, y + (lado - t * 1.15f) / 2, t, false, tema.texto, 1);
        if (encima && ayuda != null) avisar(ayuda);
        if (e.clic(x, y, lado + 12 + p.ancho(texto, t, false), lado)) {
            if (sonidoDeClic != null) sonidoDeClic.run();
            return !valor;
        }
        return valor;
    }

    /** Una barra de progreso con su texto encima. */
    public void barra(float x, float y, float w, float h, float parte, int color, String texto) {
        parte = Math.max(0, Math.min(1, parte));
        p.caja(x, y, w, h, h / 2, tema.panelAlto, 1);
        if (parte > 0) p.caja(x, y, Math.max(h, w * parte), h, h / 2, color, 1);
        if (texto != null) {
            float t = Math.min(letra * 0.8f, h * 0.75f);
            p.textoCentradoConBordeNegro(texto, x + w / 2, y + (h - t * 1.2f) / 2, t, true, tema.texto, 1);
        }
    }

    /** Un panel: un fondo con borde y, si hay, un título. Devuelve dónde empieza el contenido (y). */
    public float panel(float x, float y, float w, float h, String titulo) {
        p.caja(x, y, w, h, 10, tema.panel, 1);
        p.bordeRedondeado(x, y, w, h, 10, tema.borde, 1, 1);
        if (titulo == null) return y + 12;
        p.textoConBordeNegro(titulo, x + 16, y + 12, letra * 1.05f, true, tema.texto, 1);
        p.rect(x + 12, y + 16 + letra * 1.4f, w - 24, 1, tema.borde, 1);
        return y + 26 + letra * 1.4f;
    }

    public void titulo(String s, float x, float y) {
        p.textoConBordeNegro(s, x, y, letra * 1.9f, true, tema.texto, 1);
    }

    /** Una tecla Esc pulsada (para volver). */
    public boolean atras() {
        if (escapeReservadoAntes || escapeReservado) return false;
        return e.tecla(Input.Keys.ESCAPE);
    }

    // ================================================================ desplazar

    /** Lo que guarda una zona que se desplaza (una lista larga, un texto). */
    public static final class Desplazable {
        public float desplazamiento, contenido;
        boolean arrastrando;
        float desdeY, desdeDesplazamiento;

        public void alPrincipio() {
            desplazamiento = 0;
        }

        /** Para que algo (de y a y + h, dentro del contenido) quede a la vista. */
        public void mostrar(float y, float h, float alto) {
            if (y < desplazamiento) desplazamiento = y;
            if (y + h > desplazamiento + alto) desplazamiento = y + h - alto;
        }
    }

    /** Empieza una zona que se desplaza: recorta y devuelve el desplazamiento. Al acabar, finDesplazable. */
    public float desplazable(Desplazable d, float x, float y, float w, float h) {
        float maximo = Math.max(0, d.contenido - h);
        if (e.dentro(x, y, w, h) && e.rueda != 0 && !e.ruedaUsada) {
            e.ruedaUsada = true;
            d.desplazamiento += e.rueda * letra * 3;
        }
        // La barra de la derecha, que también se arrastra.
        if (maximo > 0) {
            float bh = Math.max(30, h * h / d.contenido);
            float by = y + (h - bh) * (d.desplazamiento / maximo);
            boolean encima = e.dentro(x + w - 12, y, 12, h);
            if (encima && e.recienPulsado && !e.clicUsado) {
                d.arrastrando = true;
                d.desdeY = e.ratonY;
                d.desdeDesplazamiento = d.desplazamiento;
                e.clicUsado = true;
            }
            if (!e.pulsado) d.arrastrando = false;
            if (d.arrastrando) d.desplazamiento = d.desdeDesplazamiento + (e.ratonY - d.desdeY) * d.contenido / h;
            p.caja(x + w - 9, y, 6, h, 3, tema.fondo, 0.35f);
            p.caja(x + w - 9, by, 6, bh, 3, encima || d.arrastrando ? tema.textoSuave : tema.borde, 0.9f);
        }
        d.desplazamiento = Math.max(0, Math.min(maximo, d.desplazamiento));
        p.recortar(x, y, w - 12, h);
        e.limitar(x, y, w - 12, h);
        return d.desplazamiento;
    }

    public void finDesplazable(Desplazable d, float alturaDelContenido) {
        d.contenido = alturaDelContenido;
        p.sinRecorte();
        e.sinLimite();
    }
}
