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
        List<Lexico.Trozo> t = Lexico.codigo(fuente);
        List<int[]> metidos = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        // Los paréntesis: dónde cierra cada uno.
        int[] pareja = new int[t.size()];
        java.util.Arrays.fill(pareja, -1);
        Deque<Integer> abiertos = new ArrayDeque<>();
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).es("(")) abiertos.push(i);
            else if (t.get(i).es(")") && !abiertos.isEmpty()) {
                int a = abiertos.pop();
                pareja[a] = i;
                pareja[i] = a;
            }
        }
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
                        textos.add(" " + PASO + ";");
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
            } else if (x.es("while") && i + 1 < t.size() && t.get(i + 1).es("(") && pareja[i + 1] > 0) {
                Lexico.Trozo abre = t.get(i + 1), cierra = t.get(pareja[i + 1]);
                metidos.add(new int[]{abre.fin, 0});
                textos.add(PASO + " && (");
                metidos.add(new int[]{cierra.inicio, 0});
                textos.add(")");
            } else if (x.es("for") && i + 1 < t.size() && t.get(i + 1).es("(") && pareja[i + 1] > 0) {
                int fin = pareja[i + 1];
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
                if (primero >= 0 && segundo >= 0) {
                    if (segundo == primero + 1) {
                        metidos.add(new int[]{t.get(primero).fin, 0});
                        textos.add(" " + PASO);
                    } else {
                        metidos.add(new int[]{t.get(primero).fin, 0});
                        textos.add(" " + PASO + " && (");
                        metidos.add(new int[]{t.get(segundo).inicio, 0});
                        textos.add(")");
                    }
                }
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
