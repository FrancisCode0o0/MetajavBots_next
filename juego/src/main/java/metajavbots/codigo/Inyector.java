package metajavbots.codigo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * INYECTOR — Antes de compilar el código del jugador, mete (sin cambiar sus
 * líneas) una llamada a metajavbots.api.Tope.paso() en la condición de cada
 * bucle (while, do-while y for clásico) y al empezar cada método. Así cada
 * vuelta de un bucle y cada llamada cuentan como un paso: un bucle infinito
 * se corta solo, y el código que hace mucho de más cuesta turnos al robot.
 *
 * No se añaden saltos de línea: los números de línea de los errores siguen
 * siendo los del jugador; las columnas se devuelven a las suyas con columnaOriginal.
 */
public final class Inyector {

    public static final String PASO = "metajavbots.api.Tope.paso()";
    public static final String ASIGNAR = "metajavbots.api.Tope.asignar";

    /** El código ya inyectado y dónde se metió cada cosa (para devolver las columnas a su sitio). */
    public static final class Inyectado {
        public final String fuente;
        private final List<int[]> metidos;   // {posición en el original, largo}

        Inyectado(String fuente, List<int[]> metidos) {
            this.fuente = fuente;
            this.metidos = metidos;
        }

        /** La posición en el código del jugador de una posición del inyectado. */
        public long original(long pos) {
            long p = pos;
            long corrido = 0;
            for (int[] m : metidos) {
                if (m[0] + corrido < pos) {
                    p -= m[1];
                    corrido += m[1];
                } else {
                    break;
                }
            }
            return Math.max(0, p);
        }

        public int cuantos() {
            return metidos.size();
        }
    }

    private enum Llave { CLASE, ENUM, METODO, OTRA }

    private Inyector() {
    }

    public static Inyectado inyectar(String fuente) {
        return inyectar(fuente, false);
    }

    /** Dónde cierra cada paréntesis y cada llave (y dónde abre cada cierre); -1 si no tiene pareja. */
    static int[] parejas(List<Lexico.Trozo> t) {
        int[] pareja = new int[t.size()];
        java.util.Arrays.fill(pareja, -1);
        Deque<Integer> abiertos = new ArrayDeque<>();
        Deque<Integer> llavesAbiertas = new ArrayDeque<>();
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).es("(")) abiertos.push(i);
            else if (t.get(i).es(")") && !abiertos.isEmpty()) {
                int a = abiertos.pop();
                pareja[a] = i;
                pareja[i] = a;
            } else if (t.get(i).es("{")) {
                llavesAbiertas.push(i);
            } else if (t.get(i).es("}") && !llavesAbiertas.isEmpty()) {
                int a = llavesAbiertas.pop();
                pareja[a] = i;
                pareja[i] = a;
            }
        }
        return pareja;
    }

    /** El paso de un while o un for (en i): lo primero de su cuerpo o, sin llaves, dentro de la condición. */
    private static void pasoEnBucle(List<Lexico.Trozo> t, int i, int[] pareja, List<int[]> metidos, List<String> textos) {
        Lexico.Trozo x = t.get(i);
        int fin = pareja[i + 1];
        if (fin + 1 < t.size() && t.get(fin + 1).es("{")) {
            // Con llaves (lo normal): el paso, lo primero del cuerpo. Así «while (true)» sigue siendo un bucle sin fin
            // para javac (si no, pediría un return detrás).
            meter(metidos, textos, t.get(fin + 1).fin, marcaDeBloque(t, fin + 1, pareja));
        } else if (x.es("while")) {
            meter(metidos, textos, t.get(i + 1).fin, PASO + " && (");
            meter(metidos, textos, t.get(fin).inicio, ")");
        } else {
            // for sin llaves: el paso, en la condición (si es un for clásico con condición).
            int primero = -1, segundo = -1, nivel = 0;
            for (int k = i + 2; k < fin; k++) {
                Lexico.Trozo y = t.get(k);
                if (y.es("(") || y.es("[") || y.es("{")) nivel++;
                else if (y.es(")") || y.es("]") || y.es("}")) nivel--;
                else if (nivel == 0 && y.es(";")) {
                    if (primero < 0) primero = k;
                    else if (segundo < 0) segundo = k;
                }
            }
            if (primero >= 0 && segundo > primero + 1) {
                meter(metidos, textos, t.get(primero).fin, " " + PASO + " && (");
                meter(metidos, textos, t.get(segundo).inicio, ")");
            }
        }
    }

    /** Con traza, además, una llamada a Tope.linea(n) antes de cada sentencia (para ver qué línea se ejecuta). */
    public static Inyectado inyectar(String fuente, boolean traza) {
        List<Lexico.Trozo> t = Lexico.codigo(fuente);
        List<int[]> metidos = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        if (traza) trazar(fuente, t, metidos, textos);
        int[] pareja = parejas(t);
        // Los «while» que cierran un do-while (su paso ya va dentro del cuerpo del do).
        java.util.Set<Integer> whileDeDo = new java.util.HashSet<>();
        Deque<Llave> llaves = new ArrayDeque<>();
        Deque<String> clases = new ArrayDeque<>();
        boolean esperaClase = false, esperaEnum = false;
        String nombreDeClase = null;
        // En un enum, hasta el primer «;» del cuerpo van las constantes.
        boolean enConstantesDeEnum = false;
        for (int i = 0; i < t.size(); i++) {
            Lexico.Trozo x = t.get(i);
            boolean trasPunto = i > 0 && t.get(i - 1).es(".");
            if (!trasPunto && (x.es("class") || x.es("interface") || x.es("enum") || x.es("record") || x.es("@interface"))) {
                // «record» solo es palabra clave si va seguido de un nombre y un paréntesis.
                if (!x.es("record") || i + 2 < t.size() && t.get(i + 1).clase == Lexico.Clase.NOMBRE) {
                    esperaClase = true;
                    esperaEnum = x.es("enum");
                    nombreDeClase = i + 1 < t.size() ? t.get(i + 1).texto : null;
                }
            } else if (x.es("{")) {
                Llave tipo;
                Llave arriba = llaves.peek();
                if (esperaClase) {
                    tipo = esperaEnum ? Llave.ENUM : Llave.CLASE;
                    clases.push(nombreDeClase == null ? "" : nombreDeClase);
                    esperaClase = false;
                    enConstantesDeEnum = esperaEnum;
                    esperaEnum = false;
                } else if (esClaseAnonima(t, i, pareja)) {
                    tipo = Llave.CLASE;
                    clases.push("");
                } else if ((arriba == Llave.CLASE || arriba == Llave.ENUM && !enConstantesDeEnum) && esCuerpoDeMetodo(t, i, pareja)) {
                    tipo = Llave.METODO;
                    String nombre = nombreDelMetodo(t, i, pareja);
                    boolean constructor = nombre != null && nombre.equals(clases.peek());
                    if (!constructor) {
                        metidos.add(new int[]{x.fin, 0});
                        textos.add(marcaDeBloque(t, i, pareja));
                    }
                } else if (arriba == Llave.ENUM && enConstantesDeEnum) {
                    // El cuerpo de una constante de enum es como una clase.
                    tipo = Llave.CLASE;
                    clases.push("");
                } else {
                    tipo = Llave.OTRA;
                }
                llaves.push(tipo);
            } else if (x.es("}")) {
                Llave cerrada = llaves.isEmpty() ? Llave.OTRA : llaves.pop();
                if ((cerrada == Llave.CLASE || cerrada == Llave.ENUM) && !clases.isEmpty()) clases.pop();
            } else if (x.es(";") && llaves.peek() == Llave.ENUM) {
                enConstantesDeEnum = false;
            } else if (x.es("do") && i + 1 < t.size() && t.get(i + 1).es("{") && pareja[i + 1] > 0) {
                // do { paso(); … } while (…);
                meter(metidos, textos, t.get(i + 1).fin, marcaDeBloque(t, i + 1, pareja));
                int cierre = pareja[i + 1];
                if (cierre + 1 < t.size() && t.get(cierre + 1).es("while")) whileDeDo.add(cierre + 1);
            } else if ((x.es("while") || x.es("for")) && !whileDeDo.contains(i) && i + 1 < t.size() && t.get(i + 1).es("(")
                    && pareja[i + 1] > 0) {
                pasoEnBucle(t, i, pareja, metidos, textos);
            }
        }
        // Se aplica de atrás adelante para no mover las posiciones.
        Integer[] orden = new Integer[metidos.size()];
        for (int i = 0; i < orden.length; i++) orden[i] = i;
        java.util.Arrays.sort(orden, (a, b) -> Integer.compare(metidos.get(a)[0], metidos.get(b)[0]));
        StringBuilder b = new StringBuilder(fuente);
        List<int[]> ordenados = new ArrayList<>();
        for (int k = orden.length - 1; k >= 0; k--) {
            int[] m = metidos.get(orden[k]);
            String s = textos.get(orden[k]);
            b.insert(m[0], s);
        }
        for (Integer o : orden) ordenados.add(new int[]{metidos.get(o)[0], textos.get(o).length()});
        return new Inyectado(b.toString(), ordenados);
    }

    public static final String LINEA = "metajavbots.api.Tope.linea";

    private enum Bloque { CLASE, INICIALIZADOR, SWITCH, SENTENCIAS }

    /**
     * La traza: delante de cada sentencia de un bloque, «Tope.linea(n);» con la línea donde empieza. No se mete
     * donde no se puede: en el cuerpo de una clase, en un inicializador de array, entre los «case» de un switch,
     * delante de un else/catch/finally/while de un do, al principio de un constructor, ni tras un return, throw,
     * break, continue o yield (javac diría que no se llega).
     */
    private static void trazar(String fuente, List<Lexico.Trozo> t, List<int[]> metidos, List<String> textos) {
        int[] inicioDeLinea = lineaDeCadaPosicion(fuente);
        Deque<Bloque> bloques = new ArrayDeque<>();
        Deque<Integer> parentesisDelBloque = new ArrayDeque<>();
        // Si cada bloque es el cuerpo de un do (tras su «}» viene el while, que no se puede trazar).
        Deque<Boolean> deDo = new ArrayDeque<>();
        Deque<String> clases = new ArrayDeque<>();
        int parentesis = 0;
        boolean esperaClase = false;
        String nombreDeClase = null;
        boolean enConstantesDeEnum = false, esperaEnum = false;
        int inicioDeSentencia = 0;
        for (int i = 0; i < t.size(); i++) {
            Lexico.Trozo x = t.get(i);
            boolean trasPunto = i > 0 && t.get(i - 1).es(".");
            Lexico.Trozo antes = i > 0 ? t.get(i - 1) : null;
            if (x.es("(")) parentesis++;
            else if (x.es(")")) parentesis = Math.max(0, parentesis - 1);
            if (!trasPunto && (x.es("class") || x.es("interface") || x.es("enum") || x.es("@interface")
                    || x.es("record") && i + 2 < t.size() && t.get(i + 1).clase == Lexico.Clase.NOMBRE && t.get(i + 2).es("("))) {
                esperaClase = true;
                esperaEnum = x.es("enum");
                nombreDeClase = i + 1 < t.size() ? t.get(i + 1).texto : null;
            } else if (x.es("{")) {
                Bloque arriba = bloques.peek();
                Bloque tipo;
                boolean constructor = false;
                if (esperaClase) {
                    tipo = Bloque.CLASE;
                    clases.push(nombreDeClase == null ? "" : nombreDeClase);
                    esperaClase = false;
                    enConstantesDeEnum = esperaEnum;
                    esperaEnum = false;
                } else if (antes != null && antes.es("->")) {
                    tipo = Bloque.SENTENCIAS;
                } else if (antes != null && (antes.es("=") || antes.es("]") || antes.es("(") || antes.es(",") && arriba == Bloque.INICIALIZADOR
                        || antes.es("{") && arriba == Bloque.INICIALIZADOR || antes.es(","))) {
                    tipo = arriba == Bloque.CLASE && antes.es(",") ? Bloque.CLASE : Bloque.INICIALIZADOR;
                } else if (antes != null && antes.es(")") && Formateo.vieneDeSwitch(t, i)) {
                    tipo = Bloque.SWITCH;
                } else if (arriba == Bloque.CLASE && enConstantesDeEnum && antes != null && (antes.clase == Lexico.Clase.NOMBRE || antes.es(")"))
                        && !esMetodoAntes(t, i)) {
                    tipo = Bloque.CLASE;
                    clases.push("");
                } else if (antes != null && antes.es(")") && esClaseAnonimaTraza(t, i)) {
                    tipo = Bloque.CLASE;
                    clases.push("");
                } else {
                    tipo = Bloque.SENTENCIAS;
                    if (arriba == Bloque.CLASE && antes != null && antes.es(")")) {
                        String nombre = nombreAntesDeParentesis(t, i);
                        constructor = nombre != null && nombre.equals(clases.peek());
                    }
                }
                bloques.push(tipo);
                parentesisDelBloque.push(parentesis);
                deDo.push(antes != null && antes.es("do"));
                inicioDeSentencia = i + 1;
                if (tipo == Bloque.SENTENCIAS && !constructor && i + 1 < t.size() && !t.get(i + 1).es("}")) {
                    metidos.add(new int[]{x.fin, 0});
                    textos.add(" " + LINEA + "(" + inicioDeLinea[t.get(i + 1).inicio] + ");");
                }
            } else if (x.es("}")) {
                cerrar(t, i, bloques, parentesisDelBloque, deDo, clases, parentesis, inicioDeLinea, metidos, textos);
                inicioDeSentencia = i + 1;
            } else if (x.es(";")) {
                Bloque arriba = bloques.peek();
                if (arriba == Bloque.CLASE) enConstantesDeEnum = false;
                boolean nivelDelBloque = parentesisDelBloque.isEmpty() || parentesisDelBloque.peek() == parentesis;
                if (arriba == Bloque.SENTENCIAS && nivelDelBloque && i + 1 < t.size()) {
                    Lexico.Trozo sig = t.get(i + 1);
                    if (!trasEstaNo(t, inicioDeSentencia, i)) {
                        metidos.add(new int[]{x.fin, 0});
                        textos.add(" " + LINEA + "(" + inicioDeLinea[sig.inicio] + ");");
                    }
                }
                if (nivelDelBloque) inicioDeSentencia = i + 1;
            }
        }
    }

    /**
     * Cierra un bloque. Tras el «}» de un if, un bucle, un try o un switch, traza también la sentencia que sigue (así se
     * ve qué rama se saltó); no tras una lambda o una clase dentro de una expresión, ni tras el cuerpo de un do.
     */
    private static void cerrar(List<Lexico.Trozo> t, int i, Deque<Bloque> bloques, Deque<Integer> parentesisDelBloque, Deque<Boolean> deDo,
            Deque<String> clases, int parentesis, int[] inicioDeLinea, List<int[]> metidos, List<String> textos) {
        Bloque cerrado = bloques.isEmpty() ? Bloque.SENTENCIAS : bloques.pop();
        if (!parentesisDelBloque.isEmpty()) parentesisDelBloque.pop();
        boolean eraDeDo = !deDo.isEmpty() && deDo.pop();
        if (cerrado == Bloque.CLASE && !clases.isEmpty()) clases.pop();
        boolean nivelDelBloque = parentesisDelBloque.isEmpty() || parentesisDelBloque.peek() == parentesis;
        if ((cerrado == Bloque.SENTENCIAS || cerrado == Bloque.SWITCH) && bloques.peek() == Bloque.SENTENCIAS && nivelDelBloque && !eraDeDo
                && i + 1 < t.size() && empiezaSentencia(t.get(i + 1))) {
            metidos.add(new int[]{t.get(i).fin, 0});
            textos.add(" " + LINEA + "(" + inicioDeLinea[t.get(i + 1).inicio] + ");");
        }
    }

    /** ¿Empieza ahí una sentencia nueva (y no un else, catch, finally, case, default, while de un do, «;», «)»…)? */
    private static boolean empiezaSentencia(Lexico.Trozo t) {
        if (t.clase == Lexico.Clase.NOMBRE || t.clase == Lexico.Clase.TIPO_PRIMITIVO) return true;
        if (t.clase != Lexico.Clase.CLAVE) return false;
        for (String k : new String[] {"if", "for", "while", "do", "return", "switch", "try", "throw", "final", "this", "super", "new", "synchronized",
            "break", "continue", "assert"}) {
            if (t.es(k)) return true;
        }
        return false;
    }

    /** La línea (desde 1) de cada posición del texto. */
    static int[] lineaDeCadaPosicion(String fuente) {
        int[] linea = new int[fuente.length() + 2];
        int n = 1;
        for (int i = 0; i < fuente.length(); i++) {
            linea[i] = n;
            if (fuente.charAt(i) == '\n') n++;
        }
        linea[fuente.length()] = n;
        return linea;
    }

    /**
     * ¿No se puede trazar tras el «;» de la sentencia que va de «desde» a «fin»? No si lo que sigue es un cierre, un
     * else, catch, finally, while (de un do), case o default, ni si la sentencia era un return, throw, break, continue o yield
     * (lo de detrás no se alcanzaría: javac no lo compilaría).
     */
    static boolean trasEstaNo(List<Lexico.Trozo> t, int desde, int fin) {
        Lexico.Trozo sig = t.get(fin + 1);
        if (sig.es("}") || sig.es("else") || sig.es("catch") || sig.es("finally") || sig.es("while") || sig.es("case") || sig.es("default")) {
            return true;
        }
        for (int k = desde; k < fin; k++) {
            Lexico.Trozo y = t.get(k);
            if (y.es("return") || y.es("throw") || y.es("break") || y.es("continue") || y.es("yield")) return true;
        }
        return false;
    }

    /** ¿Lo de antes de la «{» es «nombre(…)» de un método (no una constante de enum con argumentos)? Se mira si hay tipo delante. */
    private static boolean esMetodoAntes(List<Lexico.Trozo> t, int llave) {
        int i = llave - 1;
        if (i < 0 || !t.get(i).es(")")) return false;
        int nivel = 0;
        for (; i >= 0; i--) {
            if (t.get(i).es(")")) nivel++;
            else if (t.get(i).es("(") && --nivel == 0) break;
        }
        return i >= 2 && (t.get(i - 2).clase == Lexico.Clase.NOMBRE || t.get(i - 2).clase == Lexico.Clase.TIPO_PRIMITIVO
                || t.get(i - 2).es(">") || t.get(i - 2).es("]"));
    }

    private static String nombreAntesDeParentesis(List<Lexico.Trozo> t, int llave) {
        int i = llave - 1, nivel = 0;
        for (; i >= 0; i--) {
            if (t.get(i).es(")")) nivel++;
            else if (t.get(i).es("(") && --nivel == 0) break;
        }
        return i >= 1 ? t.get(i - 1).texto : null;
    }

    private static boolean esClaseAnonimaTraza(List<Lexico.Trozo> t, int llave) {
        int i = llave - 1, nivel = 0;
        for (; i >= 0; i--) {
            if (t.get(i).es(")")) nivel++;
            else if (t.get(i).es("(") && --nivel == 0) break;
        }
        int k = i - 1, gen = 0;
        while (k >= 0) {
            Lexico.Trozo x = t.get(k);
            if (x.es(">")) gen++;
            else if (x.es("<")) gen--;
            else if (gen == 0 && !(x.clase == Lexico.Clase.NOMBRE || x.es("."))) break;
            k--;
        }
        return k >= 0 && t.get(k).es("new");
    }

    private static void meter(List<int[]> metidos, List<String> textos, int pos, String texto) {
        metidos.add(new int[]{pos, 0});
        textos.add(texto);
    }

    /** Marca de inicio de bloque: cuenta un paso y, si el bloque reserva objetos con {@code new}, registra sus bytes. */
    private static String marcaDeBloque(List<Lexico.Trozo> t, int llaveAbre, int[] pareja) {
        long bytes = bytesDeBloque(t, llaveAbre, pareja);
        return bytes > 0 ? " " + PASO + "; " + ASIGNAR + "(" + bytes + "L);" : " " + PASO + ";";
    }

    /** Estima de forma determinista los bytes reservados directamente en un bloque (sin contar bucles anidados). */
    private static long bytesDeBloque(List<Lexico.Trozo> t, int llaveAbre, int[] pareja) {
        int cierre = llaveAbre >= 0 && llaveAbre < pareja.length ? pareja[llaveAbre] : -1;
        if (cierre <= llaveAbre) return 0;
        long bytes = 0;
        for (int k = llaveAbre + 1; k < cierre; k++) {
            Lexico.Trozo x = t.get(k);
            if ((x.es("for") || x.es("while")) && k + 1 < cierre && t.get(k + 1).es("(") && pareja[k + 1] > 0) {
                int finCond = pareja[k + 1];
                if (finCond + 1 < cierre && t.get(finCond + 1).es("{") && pareja[finCond + 1] > 0) {
                    k = pareja[finCond + 1];
                    continue;
                }
            } else if (x.es("do") && k + 1 < cierre && t.get(k + 1).es("{") && pareja[k + 1] > 0) {
                k = pareja[k + 1];
                continue;
            }
            if (x.es("new") && !(k > 0 && t.get(k - 1).es("."))) {
                boolean array = false;
                for (int m = k + 1; m < cierre && m <= k + 6; m++) {
                    Lexico.Trozo sig = t.get(m);
                    if (sig.es("[")) { array = true; break; }
                    if (sig.es("(") || sig.es("{") || sig.es(";")) break;
                }
                bytes += array ? 256L : 64L;
            }
        }
        return bytes;
    }

    /** «) {» o «) throws A, B {» justo antes de la llave: el cuerpo de un método o de un constructor. */
    private static boolean esCuerpoDeMetodo(List<Lexico.Trozo> t, int llave, int[] pareja) {
        int i = llave - 1;
        // Saltar «throws A, B.C»
        int j = i;
        while (j >= 0 && (t.get(j).clase == Lexico.Clase.NOMBRE || t.get(j).es(",") || t.get(j).es("."))) j--;
        if (j >= 0 && t.get(j).es("throws")) i = j - 1;
        if (i < 0 || !t.get(i).es(")")) return false;
        int abre = pareja[i];
        if (abre <= 0) return false;
        Lexico.Trozo antes = t.get(abre - 1);
        return antes.clase == Lexico.Clase.NOMBRE;
    }

    private static String nombreDelMetodo(List<Lexico.Trozo> t, int llave, int[] pareja) {
        int i = llave - 1;
        int j = i;
        while (j >= 0 && (t.get(j).clase == Lexico.Clase.NOMBRE || t.get(j).es(",") || t.get(j).es("."))) j--;
        if (j >= 0 && t.get(j).es("throws")) i = j - 1;
        if (i < 0 || pareja[i] <= 0) return null;
        return t.get(pareja[i] - 1).texto;
    }

    /** «new Algo(…) {» o «new Algo<T>() {»: el cuerpo de una clase anónima. */
    private static boolean esClaseAnonima(List<Lexico.Trozo> t, int llave, int[] pareja) {
        int i = llave - 1;
        if (i < 0 || !t.get(i).es(")") || pareja[i] <= 0) return false;
        int k = pareja[i] - 1;
        // Saltar el nombre del tipo: A, a.b.C, C<T, U>
        int nivel = 0;
        while (k >= 0) {
            Lexico.Trozo x = t.get(k);
            if (x.es(">")) nivel++;
            else if (x.es("<")) nivel--;
            else if (nivel == 0 && !(x.clase == Lexico.Clase.NOMBRE || x.es(".") || x.clase == Lexico.Clase.ANOTACION)) break;
            else if (nivel > 0 && !(x.clase == Lexico.Clase.NOMBRE || x.es(".") || x.es(",") || x.es("?")
                    || x.es("extends") || x.es("super") || x.es("[") || x.es("]") || x.clase == Lexico.Clase.TIPO_PRIMITIVO)) return false;
            k--;
        }
        return k >= 0 && t.get(k).es("new");
    }
}
