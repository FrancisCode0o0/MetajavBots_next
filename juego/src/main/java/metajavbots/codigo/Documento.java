package metajavbots.codigo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * DOCUMENTO — El texto del editor de código, con su cursor, su selección y
 * su deshacer. Es solo lógica (no dibuja): la ventana de código (VentanaDeCodigo)
 * lo pinta y le pasa las teclas. Todo se guarda en un único texto y en
 * posiciones (desde 0); las líneas se recalculan tras cada cambio. Así no
 * hay forma de que el cursor quede fuera del texto: cada posición que llega
 * de fuera se recorta antes de usarla. Las pruebas (PruebasDelEditor) lo
 * machacan con miles de pulsaciones al azar.
 *
 * El estilo es el de los profesionales (las convenciones de Oracle y la
 * guía de Google para Java): sangría de 4 espacios y nunca tabuladores, la
 * llave que abre al final de la línea y la que cierra en su propia línea.
 */
public final class Documento {

    public static final int SANGRIA = 4;
    private static final int MAXIMO_DESHACER = 1000;
    /** Escribir seguido dentro de este tiempo (ms) se deshace de una vez. */
    private static final long JUNTAR_MS = 1200;

    private final StringBuilder texto = new StringBuilder();
    /** Dónde empieza cada línea. */
    private int[] inicios = {0};
    private int lineas = 1;

    private int cursor;
    /** El otro extremo de la selección; -1 si no hay selección. */
    private int ancla = -1;
    /** La columna a la que se quiere volver al subir y bajar. */
    private int columnaDeseada = -1;
    /** Cada cambio aumenta la versión (el coloreado y los errores se recalculan al cambiar). */
    private long version;
    /** Los cierres que puso el editor solo (un «)» tras «(»): se pueden pisar al escribirlos. */
    private int cierreAutomatico = -1;

    private final Deque<Cambio> deshacer = new ArrayDeque<>();
    private final Deque<Cambio> rehacer = new ArrayDeque<>();
    /** El reloj (se cambia en las pruebas). */
    public java.util.function.LongSupplier reloj = System::currentTimeMillis;

    /** Un cambio: en pos se quitó «quitado» y se puso «puesto». */
    private static final class Cambio {
        final int pos;
        String quitado, puesto;
        final int cursorAntes, anclaAntes;
        int cursorDespues, anclaDespues;
        final String tipo;
        long hora;

        Cambio(int pos, String quitado, String puesto, int cursorAntes, int anclaAntes, String tipo, long hora) {
            this.pos = pos;
            this.quitado = quitado;
            this.puesto = puesto;
            this.cursorAntes = cursorAntes;
            this.anclaAntes = anclaAntes;
            this.tipo = tipo;
            this.hora = hora;
        }
    }

    public Documento() {
    }

    public Documento(String inicial) {
        ponerTexto(inicial);
    }

    // ------------------------------------------------------------------ lectura

    public String texto() {
        return texto.toString();
    }

    public int largo() {
        return texto.length();
    }

    public long version() {
        return version;
    }

    public int lineas() {
        return lineas;
    }

    public int inicioDeLinea(int linea) {
        return inicios[recortar(linea, 0, lineas - 1)];
    }

    /** Dónde acaba la línea (sin el salto). */
    public int finDeLinea(int linea) {
        linea = recortar(linea, 0, lineas - 1);
        return linea + 1 < lineas ? inicios[linea + 1] - 1 : texto.length();
    }

    public String linea(int linea) {
        return texto.substring(inicioDeLinea(linea), finDeLinea(linea));
    }

    /** La línea (desde 0) donde está la posición. */
    public int lineaDe(int pos) {
        pos = recortar(pos, 0, texto.length());
        int lo = 0, hi = lineas - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (inicios[mid] <= pos) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    public int columnaDe(int pos) {
        pos = recortar(pos, 0, texto.length());
        return pos - inicios[lineaDe(pos)];
    }

    /** La posición de (línea, columna), recortada a lo que existe. */
    public int posicion(int linea, int columna) {
        linea = recortar(linea, 0, lineas - 1);
        int ini = inicios[linea];
        return ini + recortar(columna, 0, finDeLinea(linea) - ini);
    }

    public char letra(int pos) {
        return pos >= 0 && pos < texto.length() ? texto.charAt(pos) : '\0';
    }

    public int cursor() {
        return cursor;
    }

    public int lineaDelCursor() {
        return lineaDe(cursor);
    }

    public int columnaDelCursor() {
        return columnaDe(cursor);
    }

    public boolean haySeleccion() {
        return ancla >= 0 && ancla != cursor;
    }

    public int inicioDeSeleccion() {
        return haySeleccion() ? Math.min(ancla, cursor) : cursor;
    }

    public int finDeSeleccion() {
        return haySeleccion() ? Math.max(ancla, cursor) : cursor;
    }

    public String seleccion() {
        return haySeleccion() ? texto.substring(inicioDeSeleccion(), finDeSeleccion()) : "";
    }

    public boolean puedeDeshacer() {
        return !deshacer.isEmpty();
    }

    public boolean puedeRehacer() {
        return !rehacer.isEmpty();
    }

    // ------------------------------------------------------------------ todo el texto

    /** Pone un texto nuevo y olvida el deshacer (al abrir un archivo). */
    public void ponerTexto(String nuevo) {
        texto.setLength(0);
        texto.append(normalizar(nuevo == null ? "" : nuevo));
        recalcularLineas();
        cursor = 0;
        ancla = -1;
        columnaDeseada = -1;
        cierreAutomatico = -1;
        deshacer.clear();
        rehacer.clear();
        version++;
    }

    /** Cambia todo el texto como un cambio más (se puede deshacer): lo usa el formateo. */
    public void cambiarTodo(String nuevo) {
        nuevo = normalizar(nuevo);
        if (nuevo.contentEquals(texto)) return;
        int linea = lineaDelCursor(), col = columnaDelCursor();
        reemplazar(0, texto.length(), nuevo, "todo");
        ancla = -1;
        moverA(posicion(linea, col), false);
        if (!deshacer.isEmpty()) {
            deshacer.peek().cursorDespues = cursor;
            deshacer.peek().anclaDespues = -1;
        }
    }

    /** Saltos de Windows y Mac a «\n», tabuladores a espacios y nada de letras de control. */
    public static String normalizar(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') continue;
                b.append('\n');
            } else if (c == '\t') {
                b.append("    ");
            } else if (c == '\n' || c >= ' ' && c != 0x7f || Character.isSurrogate(c)) {
                b.append(c);
            }
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ el corazón: un solo sitio cambia el texto

    private void reemplazar(int desde, int hasta, String puesto, String tipo) {
        desde = recortar(desde, 0, texto.length());
        hasta = recortar(hasta, desde, texto.length());
        String quitado = texto.substring(desde, hasta);
        if (quitado.isEmpty() && puesto.isEmpty()) return;
        int cursorAntes = cursor, anclaAntes = ancla;
        long ahora = reloj.getAsLong();
        Cambio ultimo = deshacer.peek();
        boolean juntar = ultimo != null && tipo.equals(ultimo.tipo) && ahora - ultimo.hora < JUNTAR_MS
                && (tipo.equals("escribir") && quitado.isEmpty() && desde == ultimo.pos + ultimo.puesto.length()
                        && !puesto.contains("\n") && !(puesto.equals(" ") && !ultimo.puesto.endsWith(" "))
                    || tipo.equals("borrar") && puesto.isEmpty() && hasta == ultimo.pos && ultimo.puesto.isEmpty()
                    || tipo.equals("suprimir") && puesto.isEmpty() && desde == ultimo.pos && ultimo.puesto.isEmpty());
        texto.replace(desde, hasta, puesto);
        recalcularLineas();
        version++;
        rehacer.clear();
        if (juntar && tipo.equals("escribir")) {
            ultimo.puesto += puesto;
            ultimo.hora = ahora;
        } else if (juntar && tipo.equals("borrar")) {
            // Retroceso seguido: el cambio empieza ahora más atrás.
            deshacer.pop();
            Cambio c = new Cambio(desde, quitado + ultimo.quitado, "", ultimo.cursorAntes, ultimo.anclaAntes, tipo, ahora);
            deshacer.push(c);
        } else if (juntar) {
            ultimo.quitado += quitado;
            ultimo.hora = ahora;
        } else {
            deshacer.push(new Cambio(desde, quitado, puesto, cursorAntes, anclaAntes, tipo, ahora));
            while (deshacer.size() > MAXIMO_DESHACER) deshacer.removeLast();
        }
        cursor = desde + puesto.length();
        ancla = -1;
        columnaDeseada = -1;
        Cambio c = deshacer.peek();
        c.cursorDespues = cursor;
        c.anclaDespues = -1;
    }

    /** El próximo cambio no se junta con el anterior (al mover el cursor, por ejemplo). */
    private void cortarJunta() {
        Cambio c = deshacer.peek();
        if (c != null) c.hora = Long.MIN_VALUE / 2;
    }

    private void recalcularLineas() {
        int n = 1;
        for (int i = 0; i < texto.length(); i++) if (texto.charAt(i) == '\n') n++;
        if (inicios.length < n) inicios = new int[Math.max(n, inicios.length * 2)];
        inicios[0] = 0;
        int k = 1;
        for (int i = 0; i < texto.length(); i++) if (texto.charAt(i) == '\n') inicios[k++] = i + 1;
        lineas = n;
        cursor = recortar(cursor, 0, texto.length());
        if (ancla > texto.length()) ancla = texto.length();
        if (cierreAutomatico > texto.length()) cierreAutomatico = -1;
    }

    // ------------------------------------------------------------------ deshacer

    public boolean deshacer() {
        Cambio c = deshacer.poll();
        if (c == null) return false;
        texto.replace(c.pos, c.pos + c.puesto.length(), c.quitado);
        recalcularLineas();
        version++;
        rehacer.push(c);
        cursor = recortar(c.cursorAntes, 0, texto.length());
        ancla = c.anclaAntes >= 0 ? recortar(c.anclaAntes, 0, texto.length()) : -1;
        columnaDeseada = -1;
        cierreAutomatico = -1;
        return true;
    }

    public boolean rehacer() {
        Cambio c = rehacer.poll();
        if (c == null) return false;
        texto.replace(c.pos, c.pos + c.quitado.length(), c.puesto);
        recalcularLineas();
        version++;
        c.hora = Long.MIN_VALUE / 2;
        deshacer.push(c);
        cursor = recortar(c.cursorDespues, 0, texto.length());
        ancla = c.anclaDespues >= 0 ? recortar(c.anclaDespues, 0, texto.length()) : -1;
        columnaDeseada = -1;
        cierreAutomatico = -1;
        return true;
    }

    // ------------------------------------------------------------------ escribir

    /** Pone un texto donde está el cursor (o en lugar de lo seleccionado), tal cual. */
    public void insertar(String s) {
        s = normalizar(s);
        reemplazar(inicioDeSeleccion(), finDeSeleccion(), s, s.length() == 1 ? "escribir" : "pegar");
        cierreAutomatico = -1;
    }

    /** Pega: como insertar, pero la sangría de lo pegado se ajusta a la línea donde cae. */
    public void pegar(String s) {
        s = normalizar(s);
        if (!s.contains("\n")) {
            reemplazar(inicioDeSeleccion(), finDeSeleccion(), s, "pegar");
            return;
        }
        int ini = inicioDeSeleccion();
        String antes = texto.substring(inicioDeLinea(lineaDe(ini)), ini);
        if (!antes.isBlank()) {
            reemplazar(ini, finDeSeleccion(), s, "pegar");
            return;
        }
        // Se pega al principio de una línea (o tras su sangría): todas las líneas se mueven igual.
        String[] filas = s.split("\n", -1);
        int menor = Integer.MAX_VALUE;
        for (String f : filas) if (!f.isBlank()) menor = Math.min(menor, sangriaDe(f));
        if (menor == Integer.MAX_VALUE) menor = 0;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < filas.length; i++) {
            String f = filas[i];
            String sin = f.isBlank() ? "" : f.substring(Math.min(menor, sangriaDe(f)));
            if (i == 0) b.append(sin);
            else b.append('\n').append(sin.isEmpty() ? "" : antes + sin);
        }
        reemplazar(ini, finDeSeleccion(), b.toString(), "pegar");
    }

    /** Una letra tecleada, con las ayudas del editor: cerrar paréntesis y comillas, pisar el cierre, bajar la «}». */
    public void teclear(char c) {
        if (c == '\n') {
            nuevaLinea();
            return;
        }
        if (c == '\t') {
            tabulador(false);
            return;
        }
        if (c < ' ' || c == 0x7f) return;
        // Se escribe un cierre que el editor ya puso: se pasa por encima.
        if (!haySeleccion() && cierreAutomatico == cursor && letra(cursor) == c && ")]}\"'".indexOf(c) >= 0) {
            moverA(cursor + 1, false);
            cierreAutomatico = -1;
            return;
        }
        String cierre = cierreDe(c);
        if (haySeleccion() && cierre != null && c != '\'' ) {
            // Con algo seleccionado, «(» lo rodea: (seleccionado).
            int ini = inicioDeSeleccion(), fin = finDeSeleccion();
            String dentro = texto.substring(ini, fin);
            reemplazar(ini, fin, c + dentro + cierre, "rodear");
            ancla = ini + 1;
            cursor = ini + 1 + dentro.length();
            return;
        }
        if (cierre != null && debeCerrar(c)) {
            reemplazar(inicioDeSeleccion(), finDeSeleccion(), c + cierre, "escribir");
            cursor--;
            cierreAutomatico = cursor;
            Cambio u = deshacer.peek();
            if (u != null) u.cursorDespues = cursor;
            cortarJunta();
            return;
        }
        // El cierre puesto solo sigue esperando detrás de lo que se escribe dentro: «f(x|)».
        int pendiente = !haySeleccion() && cierreAutomatico >= cursor ? cierreAutomatico + 1 : -1;
        reemplazar(inicioDeSeleccion(), finDeSeleccion(), String.valueOf(c), "escribir");
        cierreAutomatico = pendiente;
        if (c == '}') {
            // La «}» sola en su línea vuelve a la sangría de la «{» que cierra.
            int linea = lineaDelCursor();
            String l = linea(linea);
            if (l.trim().equals("}")) {
                int abre = parejaDe(cursor - 1);
                int sangria = abre >= 0 ? sangriaDe(linea(lineaDe(abre))) : Math.max(0, sangriaDe(l) - SANGRIA);
                int ini = inicioDeLinea(linea);
                reemplazar(ini, ini + sangriaDe(l), " ".repeat(sangria), "escribir");
                moverA(ini + sangria + 1, false);
            }
        }
    }

    private static String cierreDe(char c) {
        switch (c) {
            case '(':
                return ")";
            case '[':
                return "]";
            case '{':
                return "}";
            case '"':
                return "\"";
            case '\'':
                return "'";
            default:
                return null;
        }
    }

    /** ¿Se cierra solo? Solo si detrás no hay una palabra pegada (para no estorbar al reescribir). */
    private boolean debeCerrar(char c) {
        char despues = letra(cursor);
        if (Character.isLetterOrDigit(despues) || despues == '_') return false;
        if (c == '"' || c == '\'') {
            char antes = letra(cursor - 1);
            if (Character.isLetterOrDigit(antes) || antes == c) return false;
            // Dentro de un texto no se abren comillas nuevas.
            if (dentroDeTexto(cursor)) return false;
        }
        if (c == '{') return false;   // la «{» se cierra al pulsar Intro (más cómodo)
        return true;
    }

    private boolean dentroDeTexto(int pos) {
        int ini = inicioDeLinea(lineaDe(pos));
        for (Lexico.Trozo t : Lexico.trozos(texto.substring(ini, finDeLinea(lineaDe(pos))))) {
            if (t.inicio + ini < pos && t.fin + ini > pos
                    && (t.clase == Lexico.Clase.TEXTO || t.clase == Lexico.Clase.LETRA)) return true;
        }
        return false;
    }

    /** Intro: la línea nueva con la sangría que toca; entre «{» y «}», la «}» baja a su propia línea. */
    public void nuevaLinea() {
        int ini = inicioDeSeleccion();
        int linea = lineaDe(ini);
        String antes = texto.substring(inicioDeLinea(linea), ini);
        int sangria = sangriaDe(linea(linea));
        if (antes.isBlank()) sangria = Math.min(sangria, antes.length());
        String recortado = sinComentario(antes).stripTrailing();
        boolean abre = recortado.endsWith("{") || recortado.endsWith("(") || recortado.endsWith("[")
                || recortado.endsWith("->") || esEtiquetaDeCaso(recortado);
        int fin = finDeSeleccion();
        // Lo que queda tras el cursor pierde los espacios del principio.
        int saltar = 0;
        while (fin + saltar < texto.length() && texto.charAt(fin + saltar) == ' ') saltar++;
        char despues = letra(fin + saltar);
        String nueva = " ".repeat(sangria + (abre ? SANGRIA : 0));
        if (recortado.endsWith("{") && despues == '}') {
            String fuera = "\n" + nueva + "\n" + " ".repeat(sangria);
            reemplazar(ini, fin + saltar, fuera, "intro");
            moverA(ini + 1 + nueva.length(), false);
            Cambio u = deshacer.peek();
            if (u != null) u.cursorDespues = cursor;
        } else if (recortado.endsWith("{") && llaveSinCerrar(ini)) {
            // Intro tras «{» sin su «}»: se pone la «}» debajo.
            String resto = texto.substring(fin + saltar, finDeLinea(lineaDe(fin)));
            String fuera = "\n" + nueva + resto + "\n" + " ".repeat(sangria) + "}";
            reemplazar(ini, finDeLinea(lineaDe(fin)), fuera, "intro");
            moverA(ini + 1 + nueva.length(), false);
            Cambio u = deshacer.peek();
            if (u != null) u.cursorDespues = cursor;
        } else {
            reemplazar(ini, fin + saltar, "\n" + nueva, "intro");
        }
        cierreAutomatico = -1;
    }

    private static boolean esEtiquetaDeCaso(String s) {
        String t = s.trim();
        return (t.startsWith("case ") || t.startsWith("default")) && t.endsWith(":");
    }

    /** ¿La «{» que está justo antes de pos se ha quedado sin cerrar? (cuenta las llaves de todo el texto). */
    private boolean llaveSinCerrar(int pos) {
        int balance = 0;
        for (Lexico.Trozo t : Lexico.codigo(texto)) {
            if (t.es("{")) balance++;
            else if (t.es("}")) balance--;
        }
        return balance > 0;
    }

    /** La línea sin su comentario de final («x = 1; // uno» → «x = 1; »). */
    private static String sinComentario(String linea) {
        for (Lexico.Trozo t : Lexico.trozos(linea)) {
            if (t.clase == Lexico.Clase.COMENTARIO) return linea.substring(0, t.inicio);
        }
        return linea;
    }

    public static int sangriaDe(String linea) {
        int i = 0;
        while (i < linea.length() && linea.charAt(i) == ' ') i++;
        return i;
    }

    /** Tabulador: con varias líneas elegidas, las mete todas; si no, espacios hasta el siguiente múltiplo de 4. */
    public void tabulador(boolean atras) {
        if (atras || haySeleccion() && seleccion().contains("\n")) {
            sangrarLineas(atras ? -1 : 1);
            return;
        }
        int col = columnaDe(inicioDeSeleccion());
        int espacios = SANGRIA - col % SANGRIA;
        reemplazar(inicioDeSeleccion(), finDeSeleccion(), " ".repeat(espacios), "tab");
    }

    /** Mete (+1) o saca (-1) un nivel de sangría a las líneas elegidas. */
    public void sangrarLineas(int sentido) {
        int l0 = lineaDe(inicioDeSeleccion());
        int l1 = lineaDe(finDeSeleccion());
        if (haySeleccion() && l1 > l0 && columnaDe(finDeSeleccion()) == 0) l1--;
        int ini = inicioDeLinea(l0), fin = finDeLinea(l1);
        StringBuilder b = new StringBuilder();
        int cambioPrimera = 0, cambioTotal = 0;
        for (int l = l0; l <= l1; l++) {
            String f = linea(l);
            String nueva;
            if (sentido > 0) {
                nueva = f.isBlank() ? f : " ".repeat(SANGRIA) + f;
            } else {
                int quitar = Math.min(SANGRIA, sangriaDe(f));
                if (quitar == 0 || (sangriaDe(f) % SANGRIA != 0)) quitar = Math.min(quitar, Math.max(1, sangriaDe(f) % SANGRIA));
                nueva = f.substring(Math.min(quitar, f.length()));
            }
            if (l == l0) cambioPrimera = nueva.length() - f.length();
            cambioTotal += nueva.length() - f.length();
            if (l > l0) b.append('\n');
            b.append(nueva);
        }
        boolean habia = haySeleccion();
        int a = ancla, c = cursor;
        if (b.toString().equals(texto.substring(ini, fin))) return;
        reemplazar(ini, fin, b.toString(), "sangria");
        if (habia) {
            int nuevoInicio = Math.max(ini, Math.min(a, c) + cambioPrimera);
            int nuevoFin = Math.max(nuevoInicio, Math.max(a, c) + cambioTotal);
            ancla = recortar(a <= c ? nuevoInicio : nuevoFin, 0, texto.length());
            cursor = recortar(a <= c ? nuevoFin : nuevoInicio, 0, texto.length());
        } else {
            cursor = recortar(c + cambioPrimera, ini, texto.length());
        }
        Cambio u = deshacer.peek();
        if (u != null) {
            u.cursorDespues = cursor;
            u.anclaDespues = ancla;
        }
    }

    // ------------------------------------------------------------------ borrar

    /** Retroceso: borra lo elegido; en la sangría, un nivel entero; entre «()» puestos juntos, los dos. */
    public void borrarAtras() {
        if (haySeleccion()) {
            reemplazar(inicioDeSeleccion(), finDeSeleccion(), "", "cortar");
            return;
        }
        if (cursor == 0) return;
        char a = letra(cursor - 1), d = letra(cursor);
        String par = cierreDe(a);
        if (par != null && par.charAt(0) == d && cierreAutomatico == cursor) {
            reemplazar(cursor - 1, cursor + 1, "", "borrar");
            cierreAutomatico = -1;
            return;
        }
        String antes = texto.substring(inicioDeLinea(lineaDelCursor()), cursor);
        if (antes.length() > 0 && antes.isBlank()) {
            int quitar = antes.length() % SANGRIA == 0 ? SANGRIA : antes.length() % SANGRIA;
            reemplazar(cursor - quitar, cursor, "", "borrar");
            return;
        }
        int paso = Character.isLowSurrogate(a) && cursor >= 2 ? 2 : 1;
        int pendiente = cierreAutomatico >= cursor ? cierreAutomatico - paso : -1;
        reemplazar(cursor - paso, cursor, "", "borrar");
        cierreAutomatico = pendiente;
    }

    public void borrarDelante() {
        if (haySeleccion()) {
            reemplazar(inicioDeSeleccion(), finDeSeleccion(), "", "cortar");
            return;
        }
        if (cursor >= texto.length()) return;
        int paso = Character.isHighSurrogate(letra(cursor)) && cursor + 1 < texto.length() ? 2 : 1;
        reemplazar(cursor, cursor + paso, "", "suprimir");
    }

    /** Ctrl+Retroceso: borra la palabra de antes. */
    public void borrarPalabraAtras() {
        if (haySeleccion()) {
            borrarAtras();
            return;
        }
        int desde = saltoDePalabra(cursor, -1);
        reemplazar(desde, cursor, "", "cortar");
    }

    public void borrarPalabraDelante() {
        if (haySeleccion()) {
            borrarDelante();
            return;
        }
        int hasta = saltoDePalabra(cursor, 1);
        reemplazar(cursor, hasta, "", "cortar");
    }

    /** Corta lo elegido (o la línea entera si no hay nada elegido, como los editores profesionales). */
    public String cortar() {
        if (!haySeleccion()) {
            int l = lineaDelCursor();
            int ini = inicioDeLinea(l), fin = Math.min(texto.length(), finDeLinea(l) + 1);
            String s = texto.substring(ini, fin);
            if (!s.endsWith("\n")) s += "\n";
            reemplazar(l > 0 && fin == texto.length() && finDeLinea(l) == texto.length() ? ini - 1 : ini, fin, "", "cortar");
            return s;
        }
        String s = seleccion();
        reemplazar(inicioDeSeleccion(), finDeSeleccion(), "", "cortar");
        return s;
    }

    /** Copia lo elegido (o la línea entera). */
    public String copiar() {
        if (haySeleccion()) return seleccion();
        return linea(lineaDelCursor()) + "\n";
    }

    // ------------------------------------------------------------------ líneas enteras

    public void duplicarLinea() {
        if (haySeleccion()) {
            String s = seleccion();
            int fin = finDeSeleccion();
            int a = ancla, c = cursor;
            reemplazar(fin, fin, s, "duplicar");
            ancla = a + s.length();
            cursor = c + s.length();
            return;
        }
        int l = lineaDelCursor(), col = columnaDelCursor();
        reemplazar(finDeLinea(l), finDeLinea(l), "\n" + linea(l), "duplicar");
        moverA(posicion(l + 1, col), false);
    }

    public void borrarLinea() {
        int l0 = lineaDe(inicioDeSeleccion()), l1 = lineaDe(finDeSeleccion());
        if (haySeleccion() && l1 > l0 && columnaDe(finDeSeleccion()) == 0) l1--;
        int col = columnaDelCursor();
        int ini = inicioDeLinea(l0), fin = finDeLinea(l1);
        if (fin < texto.length()) fin++;
        else if (ini > 0) ini--;
        reemplazar(ini, fin, "", "borrarLinea");
        moverA(posicion(Math.min(l0, lineas - 1), col), false);
    }

    /** Alt+Arriba / Alt+Abajo: sube o baja las líneas elegidas. */
    public void moverLineas(int sentido) {
        int l0 = lineaDe(inicioDeSeleccion()), l1 = lineaDe(finDeSeleccion());
        if (haySeleccion() && l1 > l0 && columnaDe(finDeSeleccion()) == 0) l1--;
        if (sentido < 0 && l0 == 0 || sentido > 0 && l1 >= lineas - 1) return;
        int a = ancla, c = cursor;
        int ini, fin;
        String nuevo;
        if (sentido < 0) {
            ini = inicioDeLinea(l0 - 1);
            fin = finDeLinea(l1);
            nuevo = texto.substring(inicioDeLinea(l0), fin) + "\n" + linea(l0 - 1);
        } else {
            ini = inicioDeLinea(l0);
            fin = finDeLinea(l1 + 1);
            nuevo = linea(l1 + 1) + "\n" + texto.substring(ini, finDeLinea(l1));
        }
        int desplazamiento = sentido < 0 ? -(linea(l0 - 1).length() + 1) : linea(l1 + 1).length() + 1;
        reemplazar(ini, fin, nuevo, "moverLineas");
        cursor = recortar(c + desplazamiento, 0, texto.length());
        ancla = a >= 0 ? recortar(a + desplazamiento, 0, texto.length()) : -1;
        Cambio u = deshacer.peek();
        if (u != null) {
            u.cursorDespues = cursor;
            u.anclaDespues = ancla;
        }
    }

    /** Ctrl+/: pone o quita «// » a las líneas elegidas (todas a la misma columna). */
    public void comentar() {
        int l0 = lineaDe(inicioDeSeleccion()), l1 = lineaDe(finDeSeleccion());
        if (haySeleccion() && l1 > l0 && columnaDe(finDeSeleccion()) == 0) l1--;
        boolean todas = true;
        int menor = Integer.MAX_VALUE;
        for (int l = l0; l <= l1; l++) {
            String f = linea(l);
            if (f.isBlank()) continue;
            menor = Math.min(menor, sangriaDe(f));
            if (!f.trim().startsWith("//")) todas = false;
        }
        if (menor == Integer.MAX_VALUE) return;
        StringBuilder b = new StringBuilder();
        for (int l = l0; l <= l1; l++) {
            String f = linea(l);
            if (l > l0) b.append('\n');
            if (f.isBlank()) {
                b.append(f);
            } else if (todas) {
                int i = f.indexOf("//");
                int j = i + 2;
                if (j < f.length() && f.charAt(j) == ' ') j++;
                b.append(f, 0, i).append(f.substring(j));
            } else {
                b.append(f, 0, menor).append("// ").append(f.substring(menor));
            }
        }
        boolean habia = haySeleccion();
        int ini = inicioDeLinea(l0), fin = finDeLinea(l1);
        int col = columnaDelCursor(), lc = lineaDelCursor();
        reemplazar(ini, fin, b.toString(), "comentar");
        if (habia) {
            ancla = ini;
            cursor = recortar(ini + b.length(), 0, texto.length());
        } else {
            moverA(posicion(lc, col + (todas ? -3 : 3)), false);
        }
        Cambio u = deshacer.peek();
        if (u != null) {
            u.cursorDespues = cursor;
            u.anclaDespues = ancla;
        }
    }

    // ------------------------------------------------------------------ mover el cursor

    /** Pone el cursor en pos; con extender, la selección va de donde estaba a pos. */
    public void moverA(int pos, boolean extender) {
        pos = recortar(pos, 0, texto.length());
        if (extender) {
            if (ancla < 0) ancla = cursor;
        } else {
            ancla = -1;
        }
        cursor = pos;
        if (ancla == cursor) ancla = -1;
        columnaDeseada = -1;
        cierreAutomatico = -1;
        cortarJunta();
    }

    public void izquierda(boolean extender, boolean palabra) {
        if (haySeleccion() && !extender) {
            moverA(inicioDeSeleccion(), false);
            return;
        }
        moverA(palabra ? saltoDePalabra(cursor, -1) : cursor - pasoDeLetra(-1), extender);
    }

    public void derecha(boolean extender, boolean palabra) {
        if (haySeleccion() && !extender) {
            moverA(finDeSeleccion(), false);
            return;
        }
        moverA(palabra ? saltoDePalabra(cursor, 1) : cursor + pasoDeLetra(1), extender);
    }

    private int pasoDeLetra(int sentido) {
        if (sentido < 0) return Character.isLowSurrogate(letra(cursor - 1)) && cursor >= 2 ? 2 : 1;
        return Character.isHighSurrogate(letra(cursor)) && cursor + 1 < texto.length() ? 2 : 1;
    }

    public void arriba(boolean extender) {
        moverVertical(-1, extender);
    }

    public void abajo(boolean extender) {
        moverVertical(1, extender);
    }

    /** Sube o baja n líneas (las páginas). */
    public void moverVertical(int n, boolean extender) {
        int col = columnaDeseada >= 0 ? columnaDeseada : columnaDelCursor();
        int l = lineaDelCursor() + n;
        int destino;
        if (l < 0) destino = 0;
        else if (l >= lineas) destino = texto.length();
        else destino = posicion(l, col);
        moverA(destino, extender);
        columnaDeseada = col;
    }

    /** Inicio: primero tras la sangría; si ya estaba ahí, al principio de la línea. */
    public void inicio(boolean extender) {
        int l = lineaDelCursor();
        int ini = inicioDeLinea(l);
        int trasSangria = ini + sangriaDe(linea(l));
        moverA(cursor == trasSangria ? ini : trasSangria, extender);
    }

    public void fin(boolean extender) {
        moverA(finDeLinea(lineaDelCursor()), extender);
    }

    public void principio(boolean extender) {
        moverA(0, extender);
    }

    public void finalDelTexto(boolean extender) {
        moverA(texto.length(), extender);
    }

    public void seleccionarTodo() {
        ancla = 0;
        cursor = texto.length();
        if (ancla == cursor) ancla = -1;
        cortarJunta();
    }

    public void seleccionar(int desde, int hasta) {
        desde = recortar(desde, 0, texto.length());
        hasta = recortar(hasta, 0, texto.length());
        ancla = desde == hasta ? -1 : desde;
        cursor = hasta;
        columnaDeseada = -1;
        cortarJunta();
    }

    /** Doble clic: la palabra que hay en pos. */
    public void seleccionarPalabra(int pos) {
        pos = recortar(pos, 0, texto.length());
        int a = pos, b = pos;
        while (a > 0 && esDePalabra(texto.charAt(a - 1))) a--;
        while (b < texto.length() && esDePalabra(texto.charAt(b))) b++;
        if (a == b && b < texto.length() && texto.charAt(b) != '\n') b++;
        seleccionar(a, b);
    }

    /** Triple clic: la línea entera. */
    public void seleccionarLinea(int pos) {
        int l = lineaDe(pos);
        int fin = l + 1 < lineas ? inicioDeLinea(l + 1) : texto.length();
        seleccionar(inicioDeLinea(l), fin);
    }

    /** La palabra (nombre) en la que está pos, o "". */
    public String palabraEn(int pos) {
        pos = recortar(pos, 0, texto.length());
        int a = pos, b = pos;
        while (a > 0 && esDePalabra(texto.charAt(a - 1))) a--;
        while (b < texto.length() && esDePalabra(texto.charAt(b))) b++;
        return texto.substring(a, b);
    }

    /** El trozo de palabra que hay justo antes del cursor (para el autocompletado). */
    public String prefijoDelCursor() {
        int a = cursor;
        while (a > 0 && esDePalabra(texto.charAt(a - 1))) a--;
        return texto.substring(a, cursor);
    }

    /** Cambia el prefijo de antes del cursor por la palabra entera (al elegir del autocompletado). */
    public void completar(String palabra) {
        String p = prefijoDelCursor();
        reemplazar(cursor - p.length(), cursor, palabra, "completar");
    }

    private static boolean esDePalabra(char c) {
        return Character.isJavaIdentifierPart(c) && c != '\0';
    }

    private int saltoDePalabra(int pos, int sentido) {
        int n = texto.length();
        if (sentido < 0) {
            int i = pos;
            while (i > 0 && texto.charAt(i - 1) == ' ') i--;
            if (i > 0 && texto.charAt(i - 1) == '\n') return i == pos ? i - 1 : i;
            if (i > 0 && esDePalabra(texto.charAt(i - 1))) {
                while (i > 0 && esDePalabra(texto.charAt(i - 1))) i--;
            } else if (i > 0) {
                i--;
            }
            return i;
        }
        int i = pos;
        if (i < n && texto.charAt(i) == '\n') return i + 1;
        if (i < n && esDePalabra(texto.charAt(i))) {
            while (i < n && esDePalabra(texto.charAt(i))) i++;
        } else if (i < n && texto.charAt(i) != ' ') {
            i++;
        }
        while (i < n && texto.charAt(i) == ' ') i++;
        return i;
    }

    // ------------------------------------------------------------------ llaves y búsqueda

    /** La pareja de la llave, paréntesis o corchete que hay en pos (sin contar los de textos y comentarios); -1 si no hay. */
    public int parejaDe(int pos) {
        char c = letra(pos);
        String abre = "([{", cierra = ")]}";
        int k = abre.indexOf(c), m = cierra.indexOf(c);
        if (k < 0 && m < 0) return -1;
        List<Lexico.Trozo> codigo = Lexico.codigo(texto);
        int indice = -1;
        for (int i = 0; i < codigo.size(); i++) {
            if (codigo.get(i).inicio == pos && codigo.get(i).fin == pos + 1) {
                indice = i;
                break;
            }
        }
        if (indice < 0) return -1;
        char a = k >= 0 ? c : abre.charAt(m), b = k >= 0 ? cierra.charAt(k) : c;
        int sentido = k >= 0 ? 1 : -1;
        int nivel = 0;
        for (int i = indice; i >= 0 && i < codigo.size(); i += sentido) {
            Lexico.Trozo t = codigo.get(i);
            if (t.texto.length() != 1) continue;
            char x = t.texto.charAt(0);
            if (x == a) nivel += sentido;
            else if (x == b) nivel -= sentido;
            if (nivel == 0) return t.inicio;
        }
        return -1;
    }

    /** Busca desde pos (hacia delante o atrás, dando la vuelta); -1 si no está. */
    public int buscar(String que, int desde, boolean adelante, boolean mayusculas) {
        if (que == null || que.isEmpty()) return -1;
        String t = mayusculas ? texto.toString() : texto.toString().toLowerCase(java.util.Locale.ROOT);
        String q = mayusculas ? que : que.toLowerCase(java.util.Locale.ROOT);
        desde = recortar(desde, 0, t.length());
        if (adelante) {
            int i = t.indexOf(q, desde);
            return i >= 0 ? i : t.indexOf(q);
        }
        int i = desde - 1 < 0 ? -1 : t.lastIndexOf(q, desde - 1);
        return i >= 0 ? i : t.lastIndexOf(q);
    }

    /** Todas las apariciones (para marcarlas). */
    public List<Integer> apariciones(String que, boolean mayusculas) {
        List<Integer> lista = new ArrayList<>();
        if (que == null || que.isEmpty()) return lista;
        String t = mayusculas ? texto.toString() : texto.toString().toLowerCase(java.util.Locale.ROOT);
        String q = mayusculas ? que : que.toLowerCase(java.util.Locale.ROOT);
        for (int i = t.indexOf(q); i >= 0 && lista.size() < 10000; i = t.indexOf(q, i + Math.max(1, q.length()))) lista.add(i);
        return lista;
    }

    /** Cambia lo elegido si es lo buscado, y busca el siguiente. */
    public boolean reemplazarSiguiente(String que, String por, boolean mayusculas) {
        if (que == null || que.isEmpty()) return false;
        String sel = seleccion();
        if (mayusculas ? sel.equals(que) : sel.equalsIgnoreCase(que)) {
            reemplazar(inicioDeSeleccion(), finDeSeleccion(), normalizar(por), "reemplazar");
        }
        int i = buscar(que, cursor, true, mayusculas);
        if (i < 0) return false;
        seleccionar(i, i + que.length());
        return true;
    }

    public int reemplazarTodo(String que, String por, boolean mayusculas) {
        List<Integer> donde = apariciones(que, mayusculas);
        if (donde.isEmpty()) return 0;
        StringBuilder b = new StringBuilder(texto);
        String p = normalizar(por);
        for (int k = donde.size() - 1; k >= 0; k--) b.replace(donde.get(k), donde.get(k) + que.length(), p);
        int lc = lineaDelCursor(), col = columnaDelCursor();
        reemplazar(0, texto.length(), b.toString(), "reemplazar");
        moverA(posicion(lc, col), false);
        return donde.size();
    }

    /** Ctrl+Mayús+F: ordena la sangría de todo el texto (Formateo). */
    public void formatear() {
        cambiarTodo(Formateo.formatear(texto.toString()));
    }

    private static int recortar(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }
}
