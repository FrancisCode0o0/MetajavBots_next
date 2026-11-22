package metajavbots.pantalla;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import metajavbots.codigo.Documento;
import metajavbots.codigo.Lexico;

/**
 * VENTANA DE CÓDIGO — El editor que ve el jugador: dibuja un Documento con
 * números de línea, coloreado de Java, la línea actual, la selección, la
 * pareja de cada llave y los errores subrayados (con su explicación al dejar
 * el ratón encima). Las teclas son las de los editores profesionales
 * (docs/EDITOR.md). Toda la lógica del texto está en Documento, que las
 * pruebas machacan; aquí solo se dibuja y se traducen teclas y ratón.
 */
public final class VentanaDeCodigo {

    /** Un error o aviso que señalar: línea y columna desde 1. */
    public static final class Marca {
        public final int linea, columna;
        public final String texto;
        public final boolean grave;
        /** El código del error de javac («compiler.err.…») y su mensaje, para que el entrenador lo explique (o null). */
        public final String codigo, mensaje;

        public Marca(int linea, int columna, String texto, boolean grave) {
            this(linea, columna, texto, grave, null, null);
        }

        public Marca(int linea, int columna, String texto, boolean grave, String codigo, String mensaje) {
            this.linea = linea;
            this.columna = columna;
            this.texto = texto;
            this.grave = grave;
            this.codigo = codigo;
            this.mensaje = mensaje;
        }
    }

    /**
     * El error que se está mirando (con el ratón encima, o en la línea del cursor), con el texto de su línea y
     * todo el código; «pedido» si se ha pulsado su punto rojo del margen. El entrenador flotante lo explica (F2).
     */
    public record Senalado(Marca marca, String linea, String codigo, boolean pedido) {
    }

    public final Documento doc = new Documento();
    public float tamano = 18;
    public boolean soloLectura;
    public boolean enfocada = true;
    public final List<Marca> marcas = new ArrayList<>();
    /** Una línea que resaltar (la del último error en la pelea, por ejemplo); 0 si ninguna. */
    public int lineaResaltada;
    /** Las líneas que se acaban de ejecutar (la traza del modo máquina contra máquina), desde 1; y la última, más marcada (0 = verde). */
    public java.util.BitSet lineasActivas = new java.util.BitSet();
    public int lineaActiva, colorLineaActiva;
    /** Si se sigue a la línea activa (se desplaza solo para verla). */
    public boolean seguirActiva;
    /** Lo que hace Ctrl+S / Ctrl+Intro (compilar), F1 (ayuda de la palabra) y los cambios. */
    public Runnable alCompilar;
    public java.util.function.Consumer<String> alPedirAyuda;
    /**
     * Errores al escribir, como en los editores de verdad: tras un segundo sin teclear, la pantalla revisa el código
     * en segundo plano (lo compila) y devuelve sus marcas, que sustituyen a las que hay. Null: solo al compilar.
     */
    public java.util.function.Function<String, List<Marca>> revisarAlEscribir;
    /** La documentación de una palabra que no es del jugador (la API del robot y la de Java), o null si no la hay. */
    public java.util.function.Function<String, String> documentacion;
    private final RevisorAlEscribir revisor = new RevisorAlEscribir();
    private float encimaDe;
    private String palabraEncima = "", palabraDocCache = "", textoDocCache;
    private long versionDocCache = -1;
    /** Las palabras que propone el autocompletado (la API del idioma, las palabras clave…). */
    public List<String> palabras = new ArrayList<>();

    private float desplazamientoY, desplazamientoX;
    private boolean arrastrando, seguirCursor = true;
    /** Las barras de desplazamiento: cuál se arrastra (0 ninguna, 1 la vertical, 2 la horizontal) y por dónde se agarró. */
    private int barraArrastrada;
    private float agarre;
    /** Lo que mide la línea más larga (en letras), para la barra horizontal; se recalcula cuando cambia el texto. */
    private int columnasMaximas;
    private long versionDeColumnas = -1, versionDeDesplazamiento = -1;
    private float parpadeo;
    private long versionColoreada = -1;
    private List<Lexico.Trozo> trozos = new ArrayList<>();
    // Buscar y reemplazar.
    private boolean buscando, reemplazando, campoReemplazo;
    private final Documento busqueda = new Documento(), reemplazo = new Documento();
    // Autocompletar.
    private List<String> propuestas = new ArrayList<>();
    private int propuestaElegida;
    private boolean proponiendo;

    public void ponerTexto(String s) {
        doc.ponerTexto(s);
        desplazamientoX = desplazamientoY = 0;
        seguirCursor = true;
        marcas.clear();
    }

    /** Geometría del editor (margen de números y área útil de texto con recorte estricto). */
    public VistaDeCodigo.Geometria geometria(float x, float y, float w, float h, float alto, float letra) {
        return VistaDeCodigo.geometria(x, y, w, h, alto, letra, doc.lineas(), buscando, reemplazando);
    }

    /** Desplazamiento horizontal actual del contenido de código (siempre en [0, maxX]). */
    public float desplazamientoX() {
        return desplazamientoX;
    }

    /** Desplazamiento vertical actual del contenido de código (siempre en [0, maxY]). */
    public float desplazamientoY() {
        return desplazamientoY;
    }

    /** Desplazamiento horizontal máximo del documento actual para el ancho útil indicado. */
    public float maximoDesplazamientoX(float zw, float letra) {
        return maxX(zw, letra);
    }

    /** Recalcula y acota el desplazamiento horizontal y vertical según el estado actual del documento. */
    public void ajustarDesplazamiento(float zw, float zh, float alto, float letra, boolean seguir) {
        if (seguir) seguirCursor = true;
        desplazar(zw, zh, alto, letra);
    }

    // ================================================================ como un IDE: errores al escribir y documentación

    /** Con el ratón quieto sobre un nombre (medio segundo): su documentación; la de las clases del jugador, la primera. */
    private void documentacionAlPasar(Ui ui, float zx, float zy, float zw, float zh, float alto, float letra) {
        Entrada e = ui.e;
        if (!e.dentro(zx, zy, zw, zh) || proponiendo) {
            palabraEncima = "";
            return;
        }
        String w = doc.palabraEn(posicionEn(e.ratonX, e.ratonY, zx, zy, alto, letra));
        if (!w.equals(palabraEncima)) {
            palabraEncima = w;
            encimaDe = 0;
            return;
        }
        encimaDe += Gdx.graphics == null ? 0.016f : Gdx.graphics.getDeltaTime();
        if (encimaDe < 0.5f || w.length() < 2 || !Character.isJavaIdentifierStart(w.charAt(0))) return;
        if (!w.equals(palabraDocCache) || doc.version() != versionDocCache) {
            palabraDocCache = w;
            versionDocCache = doc.version();
            String propia = metajavbots.codigo.DocumentacionPropia.de(doc.texto(), w);
            textoDocCache = propia != null ? propia : documentacion == null ? null : documentacion.apply(w);
        }
        if (textoDocCache != null && !textoDocCache.isBlank()) ui.avisar(textoDocCache);
    }

    // ================================================================ dibujar

    /** Las olas de los errores de la línea l, su explicación al pasar el ratón y cuál se está mirando. */
    private void marcasDeLaLinea(Ui ui, int l, float x, VistaDeCodigo.Geometria g, float ly, float alto, float letra, int lineaCursor) {
        Entrada e = ui.e;
        String texto = doc.linea(l);
        boolean enZonaCodigo = e.dentro(g.clipX(), g.clipY(), g.clipW(), g.clipH());
        boolean enGutter = e.dentro(x, g.zy(), g.anchoGutter(), g.zh());
        for (Marca m : marcas) {
            if (m.linea != l + 1) continue;
            if (enGutter && e.clic(x, ly, letra * 1.4f, alto)) ui.errorSenalado = new Senalado(m, texto, doc.texto(), true);
            int c0 = Math.max(0, Math.min(texto.length(), m.columna - 1));
            int c1 = c0;
            while (c1 < texto.length() && Character.isJavaIdentifierPart(texto.charAt(c1))) c1++;
            if (c1 == c0) c1 = Math.min(texto.length(), c0 + 1);
            if (c1 == c0) c1 = c0 + 1;
            float ox = g.zx() + c0 * letra - desplazamientoX;
            VistaDeCodigo.ondas(ui.p, ox, ly + alto - 3, (c1 - c0) * letra, m.grave ? ui.tema.mal : ui.tema.aviso);
            boolean sobreOnda = enZonaCodigo && e.dentro(ox, ly, Math.max(letra, (c1 - c0) * letra), alto);
            if (sobreOnda || (enGutter && e.dentro(x, ly, letra * 1.4f, alto))) {
                ui.avisar(m.texto + "\n\n" + metajavbots.textos.T.t("entrenador.error_f2"));
                if (ui.errorSenalado == null) ui.errorSenalado = new Senalado(m, texto, doc.texto(), false);
            }
        }
        if (ui.errorSenalado == null && enfocada && l == lineaCursor) for (Marca m : marcas) if (m.linea == l + 1) {
            ui.errorSenalado = new Senalado(m, texto, doc.texto(), false);
            break;
        }
    }

    public void dibujar(Ui ui, float x, float y, float w, float h) {
        Pincel p = ui.p;
        Tema t = ui.tema;
        Entrada e = ui.e;
        parpadeo += Gdx.graphics == null ? 0.016f : Gdx.graphics.getDeltaTime();
        float alto = p.altoDeLinea(tamano), letra = p.anchoDeLetra(tamano);
        VistaDeCodigo.Geometria g = geometria(x, y, w, h, alto, letra);

        if (!e.bloqueada && e.recienPulsado) {
            if (e.dentro(x, y, w, h)) enfocada = true;
            else if (enfocada) { enfocada = false; ui.desenfocar(this); }
        } else if (ui.tieneOtroFoco(this)) enfocada = false;
        if (enfocada) ui.registrarFoco(this, () -> enfocada = false);
        if (revisarAlEscribir != null && !soloLectura) revisor.fotograma(doc, marcas, revisarAlEscribir);
        if (enfocada) teclas(ui, g.zh(), alto);
        raton(ui, g.zx(), g.zy(), g.zw(), g.zh(), alto, letra);
        float maxY = desplazar(g.zw(), g.zh(), alto, letra);
        arrastrarBarras(e, x, g.zx(), g.zy(), w, g.zw(), g.zh(), letra, maxY);

        p.rect(x, y, w, h, t.fondo, 1);
        p.rect(x, g.zy(), g.anchoGutter(), h - (g.zy() - y), t.panel, 1);
        int primera = Math.max(0, (int) (desplazamientoY / alto));
        int ultima = Math.min(doc.lineas() - 1, (int) ((desplazamientoY + g.zh()) / alto) + 1);
        int lineaCursor = doc.lineaDelCursor();
        int col = colorLineaActiva != 0 ? colorLineaActiva : t.bien;
        coloreado();
        dibujarMargen(p, t, x, g, primera, ultima, lineaCursor, col, alto, letra);
        dibujarZonaDeCodigo(ui, x, g, primera, ultima, lineaCursor, col, alto, letra);
        documentacionAlPasar(ui, g.zx(), g.zy(), g.zw(), g.zh(), alto, letra);
        barras(ui, x, g.zx(), g.zy(), w, g.zw(), g.zh(), letra, maxY);
        if (buscando) barraDeBuscar(ui, x, y, w, alto);
        if (proponiendo) dibujarPropuestas(ui, g.zx(), g.zy(), alto, letra);
        if (buscando || proponiendo) ui.reservarEscape();
        if (soloLectura) {
            String s = "■ " + metajavbots.textos.T.t("editor.solo_lectura");
            float tl = tamano * 0.8f, ew = p.ancho(s, tl, false) + 16;
            p.caja(x + w - 22 - ew, y + 4, ew, tl * 1.5f, 6, t.panelAlto, 0.95f);
            p.texto(s, x + w - 14 - ew, y + 4 + tl * 0.2f, tl, false, t.textoSuave, 1);
        }
    }

    private void dibujarMargen(Pincel p, Tema t, float x, VistaDeCodigo.Geometria g, int primera, int ultima,
            int lineaCursor, int col, float alto, float letra) {
        p.recortar(x, g.zy(), g.anchoGutter(), g.zh());
        for (int l = primera; l <= ultima; l++) {
            float ly = g.zy() + l * alto - desplazamientoY;
            if (lineaActiva == l + 1) p.texto("▶", x + 2, ly + alto * 0.08f, tamano * 0.95f, true, col, 1);
            boolean conError = false, conAviso = false;
            for (Marca m : marcas) if (m.linea == l + 1) { if (m.grave) conError = true; else conAviso = true; }
            p.textoDeCodigoDerecha(Integer.toString(l + 1), g.zx() - letra * 1.6f, ly + alto * 0.12f, tamano * 0.9f,
                    l == lineaCursor, l == lineaCursor ? t.texto : t.numeros, 1);
            if (conError || conAviso) p.circulo(x + letra * 0.7f, ly + alto / 2, alto * 0.22f, conError ? t.mal : t.aviso, 1);
        }
        p.sinRecorte();
    }

    private void dibujarZonaDeCodigo(Ui ui, float x, VistaDeCodigo.Geometria g, int primera, int ultima,
            int lineaCursor, int col, float alto, float letra) {
        Pincel p = ui.p;
        Tema t = ui.tema;
        int pareja = parejaDelCursor();
        String palabra = palabraDelCursor();
        p.recortar(g.clipX(), g.clipY(), g.clipW(), g.clipH());
        for (int l = primera; l <= ultima; l++) {
            float ly = g.zy() + l * alto - desplazamientoY;
            if (l == lineaCursor && enfocada) p.rect(g.zx(), ly, g.zw(), alto, t.lineaActual, 1);
            if (lineaResaltada == l + 1) p.rect(g.zx(), ly, g.zw(), alto, t.mal, 0.18f);
            if (lineasActivas.get(l + 1) || lineaActiva == l + 1) {
                p.rect(g.zx(), ly, g.zw(), alto, col, lineaActiva == l + 1 ? 0.34f : 0.18f);
                p.rect(g.zx(), ly, 5, alto, col, lineaActiva == l + 1 ? 1f : 0.75f);
            }
            if (lineaActiva == l + 1) p.marco(g.zx(), ly, g.zw(), alto, col, 0.98f, 2f);
            if (doc.haySeleccion()) {
                int ini = doc.inicioDeLinea(l), fin = doc.finDeLinea(l);
                int s0 = Math.max(ini, doc.inicioDeSeleccion()), s1 = Math.min(fin, doc.finDeSeleccion());
                boolean conSalto = doc.finDeSeleccion() > fin;
                if (s0 < s1 || conSalto && s0 <= fin && doc.inicioDeSeleccion() <= fin) {
                    float sx = g.zx() + (s0 - ini) * letra - desplazamientoX;
                    float ex = g.zx() + (s1 - ini) * letra - desplazamientoX + (conSalto ? letra * 0.6f : 0);
                    p.rect(sx, ly, Math.max(2, ex - sx), alto, t.seleccion, 1);
                }
            }
            dibujarLinea(p, t, l, g.zx() - desplazamientoX, ly, alto, letra, pareja, palabra);
            marcasDeLaLinea(ui, l, x, g, ly, alto, letra, lineaCursor);
        }
        if (enfocada && !soloLectura && (parpadeo % 1.0f) < 0.6f) {
            float cx = g.zx() + doc.columnaDelCursor() * letra - desplazamientoX;
            float cy = g.zy() + lineaCursor * alto - desplazamientoY;
            float rx = cx < g.clipX() && cx >= g.clipX() - 1 ? g.clipX() : cx - 1;
            p.rect(rx, cy + 2, 2.5f, alto - 4, t.acento, 1);
        }
        p.sinRecorte();
    }

    private void dibujarLinea(Pincel p, Tema t, int l, float x0, float y, float alto, float letra, int pareja, String palabra) {
        int ini = doc.inicioDeLinea(l), fin = doc.finDeLinea(l);
        String texto = doc.linea(l);
        float ty = y + alto * 0.12f;
        // Las guías de sangría, finas.
        int sangria = Documento.sangriaDe(texto);
        for (int s = Documento.SANGRIA; s < sangria; s += Documento.SANGRIA) p.rect(x0 + s * letra, y, 1, alto, t.borde, 0.6f);
        int col = 0;
        int k = VistaDeCodigo.buscarTrozo(trozos, ini);
        List<Object[]> colores = new ArrayList<>();
        while (col < texto.length()) {
            int pos = ini + col;
            Lexico.Trozo tr = k < trozos.size() ? trozos.get(k) : null;
            int hasta;
            int color;
            if (tr != null && tr.inicio <= pos && tr.fin > pos) {
                hasta = Math.min(fin, tr.fin) - ini;
                color = VistaDeCodigo.colorDe(t, trozos, tr, k);
                if (tr.fin <= fin) k++;
            } else {
                int siguiente = tr != null && tr.inicio > pos ? Math.min(fin, tr.inicio) : fin;
                hasta = siguiente - ini;
                color = t.texto;
            }
            if (hasta <= col) hasta = col + 1;
            String trozo = texto.substring(col, Math.min(hasta, texto.length()));
            if (palabra != null && trozo.equals(palabra)) p.rect(x0 + col * letra, y + 2, trozo.length() * letra, alto - 4, t.seleccion, 0.45f);
            colores.add(new Object[]{trozo, color});
            col = hasta;
        }
        p.textoDeColores(colores, x0, ty, tamano);
        if (pareja >= 0) {
            int cursorPareja = doc.cursor() > 0 && "(){}[]".indexOf(doc.letra(doc.cursor() - 1)) >= 0 ? doc.cursor() - 1 : doc.cursor();
            for (int pos : new int[]{pareja, cursorPareja}) {
                if (pos >= ini && pos <= fin && pos < doc.largo()) p.marco(x0 + (pos - ini) * letra, y + 1, letra, alto - 2, t.acento, 0.9f, 1.5f);
            }
        }
    }

    private void coloreado() {
        if (versionColoreada == doc.version()) return;
        versionColoreada = doc.version();
        trozos = Lexico.trozos(doc.texto());
    }

    private int parejaDelCursor() {
        int c = doc.cursor();
        if ("(){}[]".indexOf(doc.letra(c)) >= 0) {
            int p = doc.parejaDe(c);
            if (p >= 0) return p;
        }
        if (c > 0 && "(){}[]".indexOf(doc.letra(c - 1)) >= 0) return doc.parejaDe(c - 1);
        return -1;
    }

    private String palabraDelCursor() {
        if (doc.haySeleccion()) return null;
        String w = doc.palabraEn(doc.cursor());
        return w.length() >= 2 && !Lexico.CLAVES.contains(w) && !Lexico.PRIMITIVOS.contains(w) ? w : null;
    }

    /** Lo que se puede desplazar a lo ancho (0 si todo cabe). */
    private float maxX(float zw, float letra) {
        if (versionDeColumnas != doc.version()) {
            versionDeColumnas = doc.version();
            int m = 0;
            for (int l = 0; l < doc.lineas(); l++) m = Math.max(m, doc.linea(l).length());
            columnasMaximas = m;
        }
        return VistaDeCodigo.maxX(columnasMaximas, zw, letra);
    }

    /** Las barras se manejan con el ratón: se arrastra el tirador y un clic en el carril salta ahí. */
    private void arrastrarBarras(Entrada e, float x, float zx, float zy, float w, float zw, float zh, float letra, float maxY) {
        float maxX = maxX(zw, letra);
        if (!e.pulsado) barraArrastrada = 0;
        if (e.recienPulsado && !e.clicUsado) {
            if (maxY > 0 && e.dentro(x + w - 12, zy, 12, zh)) {
                e.clicUsado = true;
                float[] t = VistaDeCodigo.tirador(zh, zh, maxY, desplazamientoY);
                float enCarril = e.ratonY - zy;
                agarre = enCarril >= t[0] && enCarril <= t[0] + t[1] ? enCarril - t[0] : t[1] / 2;
                barraArrastrada = 1;
            } else if (maxX > 0 && e.dentro(zx, zy + zh, zw, 12)) {
                e.clicUsado = true;
                float[] t = VistaDeCodigo.tirador(zw, zw, maxX, desplazamientoX);
                float enCarril = e.ratonX - zx;
                agarre = enCarril >= t[0] && enCarril <= t[0] + t[1] ? enCarril - t[0] : t[1] / 2;
                barraArrastrada = 2;
            }
        }
        if (barraArrastrada == 1) {
            float[] t = VistaDeCodigo.tirador(zh, zh, maxY, desplazamientoY);
            float u = (e.ratonY - zy - agarre) / Math.max(1, zh - t[1]);
            desplazamientoY = Math.max(0, Math.min(maxY, u * maxY));
        } else if (barraArrastrada == 2) {
            float[] t = VistaDeCodigo.tirador(zw, zw, maxX, desplazamientoX);
            float u = (e.ratonX - zx - agarre) / Math.max(1, zw - t[1]);
            desplazamientoX = Math.max(0, Math.min(maxX, u * maxX));
        }
    }

    private void barras(Ui ui, float x, float zx, float zy, float w, float zw, float zh, float letra, float maxY) {
        Pincel p = ui.p;
        Entrada e = ui.e;
        float maxX = maxX(zw, letra);
        if (maxX > 0) {
            float[] t = VistaDeCodigo.tirador(zw, zw, maxX, desplazamientoX);
            boolean viva = barraArrastrada == 2 || e.dentro(zx, zy + zh, zw, 12);
            p.caja(zx, zy + zh + 2, zw, 9, 4, ui.tema.panel, viva ? 0.9f : 0.5f);
            p.caja(zx + t[0], zy + zh + 2, t[1], 9, 4, viva ? ui.tema.acento : ui.tema.borde, 0.9f);
        }
        if (maxY <= 0) return;
        float[] t = VistaDeCodigo.tirador(zh, zh, maxY, desplazamientoY);
        boolean viva = barraArrastrada == 1 || e.dentro(x + w - 12, zy, 12, zh);
        p.caja(x + w - 12, zy, 10, zh, 4, ui.tema.panel, viva ? 0.9f : 0.5f);
        p.caja(x + w - 12, zy + t[0], 10, t[1], 4, viva ? ui.tema.acento : ui.tema.borde, 0.9f);
        for (Marca m : marcas) {
            float my = zy + zh * (m.linea - 1) / Math.max(1f, doc.lineas());
            p.rect(x + w - 12, my, 10, 3, m.grave ? ui.tema.mal : ui.tema.aviso, 1);
        }
    }

    // ================================================================ el ratón

    private void raton(Ui ui, float zx, float zy, float zw, float zh, float alto, float letra) {
        Entrada e = ui.e;
        if (e.dentro(zx - 60, zy, zw + 70, zh) && e.rueda != 0 && !e.ruedaUsada) {
            e.ruedaUsada = true;
            if (e.ctrl()) tamano = Math.max(10, Math.min(40, tamano - e.rueda));
            else if (e.mayus()) desplazamientoX += e.rueda * letra * 4;
            else desplazamientoY += e.rueda * alto * 3;
        }
        int pos = posicionEn(e.ratonX, e.ratonY, zx, zy, alto, letra);
        if (e.recienPulsado && !e.clicUsado && e.dentro(zx - 40, zy, zw + 40, zh)) {
            e.clicUsado = true;
            enfocada = true;
            proponiendo = false;
            campoReemplazo = false;
            parpadeo = 0;
            if (e.clics == 2) doc.seleccionarPalabra(pos);
            else if (e.clics >= 3) doc.seleccionarLinea(pos);
            else if (e.ctrl() && alPedirAyuda != null) alPedirAyuda.accept(doc.palabraEn(pos));
            else doc.moverA(pos, e.mayus());
            arrastrando = e.clics == 1;
        }
        if (!e.pulsado) arrastrando = false;
        if (arrastrando) {
            doc.moverA(pos, true);
            if (e.ratonY < zy) desplazamientoY -= alto * 0.5f;
            if (e.ratonY > zy + zh) desplazamientoY += alto * 0.5f;
            if (e.ratonX < zx) desplazamientoX -= letra * 2;
            if (e.ratonX > zx + zw) desplazamientoX += letra * 2;
        }
    }

    private int posicionEn(float mx, float my, float zx, float zy, float alto, float letra) {
        int linea = (int) Math.floor((my - zy + desplazamientoY) / alto);
        int col = Math.round((mx - zx + desplazamientoX) / letra);
        if (linea < 0) return 0;
        if (linea >= doc.lineas()) return doc.largo();
        return doc.posicion(linea, Math.max(0, col));
    }

    // ================================================================ las teclas

    private void teclas(Ui ui, float zh, float alto) {
        if (ui.e.bloqueada) return;
        Entrada e = ui.e;
        int pagina = Math.max(1, (int) (zh / alto) - 2);
        if (buscando && teclasDeBuscar(e)) return;
        if (proponiendo && teclasDePropuestas(e)) return;
        for (Entrada.Tecla k : e.teclas) {
            if (k.usada || k.alt && k.ctrl) continue;   // Ctrl+Alt es AltGr: lo escribe keyTyped
            boolean c = k.ctrl, m = k.mayus;
            boolean usada = true;
            boolean cambia = true;
            switch (k.codigo) {
                case Keys.LEFT: doc.izquierda(m, c); cambia = false; break;
                case Keys.RIGHT: doc.derecha(m, c); cambia = false; break;
                case Keys.UP:
                    if (k.alt && !soloLectura) doc.moverLineas(-1);
                    else doc.arriba(m);
                    cambia = k.alt;
                    break;
                case Keys.DOWN:
                    if (k.alt && !soloLectura) doc.moverLineas(1);
                    else doc.abajo(m);
                    cambia = k.alt;
                    break;
                case Keys.HOME: if (c) doc.principio(m); else doc.inicio(m); cambia = false; break;
                case Keys.END: if (c) doc.finalDelTexto(m); else doc.fin(m); cambia = false; break;
                case Keys.PAGE_UP: doc.moverVertical(-pagina, m); desplazamientoY -= pagina * alto; cambia = false; break;
                case Keys.PAGE_DOWN: doc.moverVertical(pagina, m); desplazamientoY += pagina * alto; cambia = false; break;
                case Keys.BACKSPACE: if (soloLectura) break; if (c) doc.borrarPalabraAtras(); else doc.borrarAtras(); break;
                case Keys.FORWARD_DEL: if (soloLectura) break; if (c) doc.borrarPalabraDelante(); else doc.borrarDelante(); break;
                case Keys.ENTER:
                case Keys.NUMPAD_ENTER:
                    if (c) {
                        if (alCompilar != null) alCompilar.run();
                        cambia = false;
                    } else if (!soloLectura) {
                        doc.nuevaLinea();
                    }
                    break;
                case Keys.TAB: if (!soloLectura) doc.tabulador(m); break;
                case Keys.ESCAPE: usada = proponiendo; proponiendo = false; cambia = false; break;
                case Keys.F1: if (alPedirAyuda != null) alPedirAyuda.accept(doc.palabraEn(doc.cursor())); cambia = false; break;
                case Keys.F3: buscarSiguiente(!m); cambia = false; break;
                default:
                    usada = c && atajo(k.codigo, m);
                    cambia = usada;
                    break;
            }
            if (usada) {
                k.usada = true;
                seguirCursor = true;
                parpadeo = 0;
                if (cambia && proponiendo) actualizarPropuestas();
            }
        }
        if (soloLectura) return;
        for (char ch : e.letras) {
            doc.teclear(ch);
            seguirCursor = true;
            parpadeo = 0;
            if (Character.isJavaIdentifierPart(ch) || ch == '.') {
                if (doc.prefijoDelCursor().length() >= 2 || ch == '.') abrirPropuestas(false);
            } else {
                proponiendo = false;
            }
        }
        if (proponiendo) actualizarPropuestas();
        e.letras.clear();
    }

    /** Ctrl+letra. Devuelve si la usó. */
    private boolean atajo(int codigo, boolean mayus) {
        switch (codigo) {
            case Keys.Z: if (soloLectura) return false; if (mayus) doc.rehacer(); else doc.deshacer(); return true;
            case Keys.Y: if (soloLectura) return false; doc.rehacer(); return true;
            case Keys.A: doc.seleccionarTodo(); return true;
            case Keys.C: portapapeles(doc.copiar()); return true;
            case Keys.X: if (soloLectura) return false; portapapeles(doc.cortar()); return true;
            case Keys.V:
                if (soloLectura) return false;
                String s = leerPortapapeles();
                if (s != null && !s.isEmpty()) doc.pegar(s);
                return true;
            case Keys.D: if (soloLectura) return false; doc.duplicarLinea(); return true;
            case Keys.K: if (soloLectura || !mayus) return false; doc.borrarLinea(); return true;
            case Keys.SLASH: case Keys.NUMPAD_DIVIDE: case Keys.NUM_7:
                if (soloLectura) return false;
                doc.comentar();
                return true;
            case Keys.F:
                if (mayus) {
                    if (soloLectura) return false;
                    doc.formatear();
                    return true;
                }
                abrirBusqueda(false);
                return true;
            case Keys.H: abrirBusqueda(!soloLectura); return true;
            case Keys.S: if (alCompilar != null) alCompilar.run(); return true;
            case Keys.SPACE: if (!soloLectura) abrirPropuestas(true); return true;
            case Keys.PLUS: case Keys.EQUALS: case Keys.NUMPAD_ADD: tamano = Math.min(40, tamano + 1); return true;
            case Keys.MINUS: case Keys.NUMPAD_SUBTRACT: tamano = Math.max(10, tamano - 1); return true;
            case Keys.NUM_0: case Keys.NUMPAD_0: tamano = 18; return true;
            default: return false;
        }
    }

    private static void portapapeles(String s) {
        try {
            if (Gdx.app != null && s != null) Gdx.app.getClipboard().setContents(s);
        } catch (RuntimeException e) {
            // sin portapapeles
        }
    }

    private static String leerPortapapeles() {
        try {
            return Gdx.app == null ? null : Gdx.app.getClipboard().getContents();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ================================================================ buscar y reemplazar

    public void abrirBusqueda(boolean conReemplazo) {
        buscando = true;
        reemplazando = conReemplazo;
        campoReemplazo = false;
        String sel = doc.seleccion();
        if (!sel.isEmpty() && !sel.contains("\n")) busqueda.ponerTexto(sel);
        busqueda.seleccionarTodo();
    }

    private void buscarSiguiente(boolean adelante) {
        String q = busqueda.texto();
        if (q.isEmpty()) return;
        int desde = adelante ? doc.finDeSeleccion() : doc.inicioDeSeleccion();
        int i = doc.buscar(q, desde, adelante, false);
        if (i >= 0) doc.seleccionar(i, i + q.length());
        seguirCursor = true;
    }

    private boolean teclasDeBuscar(Entrada e) {
        Documento campo = campoReemplazo ? reemplazo : busqueda;
        boolean algo = false;
        for (Entrada.Tecla k : e.teclas) {
            if (k.usada || k.alt && k.ctrl) continue;
            boolean usada = true;
            switch (k.codigo) {
                case Keys.ESCAPE: buscando = false; break;
                case Keys.ENTER: case Keys.NUMPAD_ENTER:
                    if (campoReemplazo) doc.reemplazarSiguiente(busqueda.texto(), reemplazo.texto(), false);
                    else buscarSiguiente(!k.mayus);
                    seguirCursor = true;
                    break;
                case Keys.TAB: if (reemplazando) campoReemplazo = !campoReemplazo; break;
                case Keys.BACKSPACE: if (k.ctrl) campo.borrarPalabraAtras(); else campo.borrarAtras(); break;
                case Keys.FORWARD_DEL: if (k.ctrl) campo.borrarPalabraDelante(); else campo.borrarDelante(); break;
                case Keys.LEFT: campo.izquierda(k.mayus, k.ctrl); break;
                case Keys.RIGHT: campo.derecha(k.mayus, k.ctrl); break;
                case Keys.HOME: campo.principio(k.mayus); break;
                case Keys.END: campo.finalDelTexto(k.mayus); break;
                case Keys.F3: buscarSiguiente(!k.mayus); break;
                default:
                    if (k.ctrl && k.codigo == Keys.A) campo.seleccionarTodo();
                    else if (k.ctrl && k.codigo == Keys.V) {
                        String s = leerPortapapeles();
                        if (s != null) campo.insertar(s.replace("\n", " "));
                    } else if (k.ctrl && k.codigo == Keys.Z) { if (k.mayus) campo.rehacer(); else campo.deshacer(); }
                    else if (k.ctrl && k.codigo == Keys.Y) campo.rehacer();
                    else if (k.ctrl && k.codigo == Keys.C) portapapeles(campo.haySeleccion() ? campo.seleccion() : campo.texto());
                    else if (k.ctrl && k.codigo == Keys.X) {
                        portapapeles(campo.haySeleccion() ? campo.seleccion() : campo.texto());
                        if (campo.haySeleccion()) campo.borrarAtras(); else campo.cambiarTodo("");
                    } else if (k.ctrl && k.codigo == Keys.F) campoReemplazo = false;
                    else usada = false;
                    break;
            }
            if (usada) {
                k.usada = true;
                algo = true;
            }
        }
        for (char ch : e.letras) {
            campo.insertar(String.valueOf(ch));
            algo = true;
            if (!campoReemplazo) {
                int i = doc.buscar(busqueda.texto(), doc.inicioDeSeleccion(), true, false);
                if (i >= 0) { doc.seleccionar(i, i + busqueda.largo()); seguirCursor = true; }
            }
        }
        e.letras.clear();
        return algo || buscando;
    }

    private void barraDeBuscar(Ui ui, float x, float y, float w, float alto) {
        Pincel p = ui.p;
        Tema t = ui.tema;
        float h = alto * (reemplazando ? 2.4f : 1.4f);
        p.rect(x, y, w, h, t.panelAlto, 1);
        float t1 = tamano * 0.85f;
        String[] nombres = {metajavbots.textos.T.t("editor.buscar"), metajavbots.textos.T.t("editor.reemplazar")};
        Documento[] campos = {busqueda, reemplazo};
        for (int i = 0; i < (reemplazando ? 2 : 1); i++) {
            float fy = y + 6 + i * alto * 1.1f;
            p.texto(nombres[i], x + 10, fy + 2, t1, false, t.textoSuave, 1);
            float cx = x + 150, cw = Math.min(420, w - 330);
            boolean activo = campoReemplazo == (i == 1);
            String txt = campos[i].texto();
            p.caja(cx, fy - 2, cw, alto, 5, t.fondo, 1);
            p.bordeRedondeado(cx, fy - 2, cw, alto, 5, activo ? t.acento : t.borde, 1, 1.5f);
            p.recortar(cx + 4, fy - 2, cw - 8, alto);
            if (campos[i].haySeleccion()) {
                float sx = cx + 8 + p.anchoDeCodigo(txt.substring(0, campos[i].inicioDeSeleccion()), t1, false);
                p.rect(sx, fy, p.anchoDeCodigo(campos[i].seleccion(), t1, false), alto - 6, t.seleccion, 1);
            }
            p.textoDeCodigo(txt, cx + 8, fy + 2, t1, false, t.texto, 1);
            if (activo) p.rect(cx + 8 + p.anchoDeCodigo(txt.substring(0, campos[i].cursor()), t1, false), fy, 2, alto - 6, t.acento, 1);
            p.sinRecorte();
            if (ui.e.clic(cx, fy - 2, cw, alto)) {
                campoReemplazo = i == 1;
                campos[i].moverA(VistaDeCodigo.indiceEnLinea(p, ui.e.ratonX - (cx + 8), t1, txt), ui.e.mayus());
            }
        }
        int n = doc.apariciones(busqueda.texto(), false).size();
        float bx = x + Math.min(600, w - 170);
        p.texto(metajavbots.textos.T.f("editor.encontrados", n), bx, y + 8, t1, false, n == 0 && busqueda.largo() > 0 ? t.mal : t.textoSuave, 1);
        if (reemplazando && ui.boton(metajavbots.textos.T.t("editor.reemplazar_todo"), bx, y + alto * 1.15f, 160, alto * 0.95f)) {
            doc.reemplazarTodo(busqueda.texto(), reemplazo.texto(), false);
        }
        if (ui.boton("✕", x + w - alto * 1.1f, y + 4, alto, alto * 0.95f, Ui.Estilo.PLANO)) buscando = false;
    }

    // ================================================================ autocompletar

    private void abrirPropuestas(boolean siempre) {
        actualizarPropuestas();
        proponiendo = !propuestas.isEmpty() && (siempre || !(propuestas.size() == 1 && propuestas.get(0).equals(doc.prefijoDelCursor())));
    }

    private void actualizarPropuestas() {
        String pre = doc.prefijoDelCursor();
        List<String> l = new ArrayList<>();
        java.util.Set<String> vistas = new java.util.HashSet<>();
        String bajo = pre.toLowerCase(java.util.Locale.ROOT);
        // Tras «algo.», si «algo» es una clase del jugador (o una variable de una de ellas), lo suyo primero.
        int ini = doc.cursor() - pre.length();
        if (ini > 1 && doc.letra(ini - 1) == '.') {
            int a = ini - 1;
            while (a > 0 && Character.isJavaIdentifierPart(doc.letra(a - 1))) a--;
            String antes = doc.texto().substring(a, ini - 1);
            for (String m : metajavbots.codigo.DocumentacionPropia.miembros(doc.texto(), antes)) {
                if (m.toLowerCase(java.util.Locale.ROOT).startsWith(bajo) && vistas.add(m)) l.add(m);
            }
        }
        for (String w : palabras) if (w.toLowerCase(java.util.Locale.ROOT).startsWith(bajo) && vistas.add(w)) l.add(w);
        for (Lexico.Trozo tr : trozos) {
            if (tr.clase == Lexico.Clase.NOMBRE && tr.texto.length() > 2 && !tr.texto.equals(pre)
                    && tr.texto.toLowerCase(java.util.Locale.ROOT).startsWith(bajo) && vistas.add(tr.texto)) l.add(tr.texto);
        }
        for (String w : Lexico.CLAVES) if (w.startsWith(pre) && pre.length() >= 2 && vistas.add(w)) l.add(w);
        if (l.size() > 40) l = l.subList(0, 40);
        propuestas = l;
        propuestaElegida = Math.max(0, Math.min(propuestaElegida, l.size() - 1));
        if (l.isEmpty()) proponiendo = false;
    }

    private boolean teclasDePropuestas(Entrada e) {
        for (Entrada.Tecla k : e.teclas) {
            if (k.usada) continue;
            switch (k.codigo) {
                case Keys.UP: propuestaElegida = Math.max(0, propuestaElegida - 1); k.usada = true; break;
                case Keys.DOWN: propuestaElegida = Math.min(propuestas.size() - 1, propuestaElegida + 1); k.usada = true; break;
                case Keys.ENTER: case Keys.TAB:
                    if (!propuestas.isEmpty()) {
                        String w = propuestas.get(propuestaElegida);
                        doc.completar(w.contains("(") ? w.substring(0, w.indexOf('(')) : w);
                        seguirCursor = true;
                    }
                    proponiendo = false;
                    k.usada = true;
                    break;
                case Keys.ESCAPE: proponiendo = false; k.usada = true; break;
                default: break;
            }
        }
        return false;
    }

    private void dibujarPropuestas(Ui ui, float zx, float zy, float alto, float letra) {
        Pincel p = ui.p;
        Tema t = ui.tema;
        int n = Math.min(10, propuestas.size());
        if (n == 0) return;
        float px = zx + (doc.columnaDelCursor() - doc.prefijoDelCursor().length()) * letra - desplazamientoX;
        float py = zy + (doc.lineaDelCursor() + 1) * alto - desplazamientoY;
        float w = 0;
        for (String s : propuestas) w = Math.max(w, p.anchoDeCodigo(s, tamano * 0.9f, false));
        w += 30;
        int primera = Math.max(0, Math.min(propuestaElegida - n + 1, propuestas.size() - n));
        if (py + n * alto > p.alto) py -= (n + 1) * alto;
        p.caja(px + 3, py + 4, w, n * alto + 8, 6, 0x000000, 0.35f);
        p.caja(px, py, w, n * alto + 8, 6, t.panelAlto, 1);
        p.bordeRedondeado(px, py, w, n * alto + 8, 6, t.borde, 1, 1);
        for (int i = 0; i < n; i++) {
            int k = primera + i;
            float y = py + 4 + i * alto;
            if (k == propuestaElegida) p.rect(px + 2, y, w - 4, alto, t.seleccion, 1);
            p.textoDeCodigo(propuestas.get(k), px + 12, y + alto * 0.12f, tamano * 0.9f, false, t.texto, 1);
            if (ui.e.clic(px, y, w, alto)) {
                propuestaElegida = k;
                String s = propuestas.get(k);
                doc.completar(s.contains("(") ? s.substring(0, s.indexOf('(')) : s);
                proponiendo = false;
            }
        }
    }

    /** Lleva la vista al cursor (o a la línea que se ejecuta) y la mantiene acotada dentro del texto. */
    private float desplazar(float zw, float zh, float alto, float letra) {
        maxX(zw, letra);
        boolean cambio = versionDeDesplazamiento != doc.version();
        versionDeDesplazamiento = doc.version();
        VistaDeCodigo.Desplazamiento d = VistaDeCodigo.ajustar(doc, desplazamientoX, desplazamientoY, zw, zh, alto, letra,
                columnasMaximas, seguirCursor || cambio, seguirActiva, lineaActiva);
        seguirCursor = false;
        desplazamientoX = d.x();
        desplazamientoY = d.y();
        return d.maxY();
    }
}
