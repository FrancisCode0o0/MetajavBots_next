package metajavbots.ejercicios;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PLANTILLAS DE EJERCICIOS — Ejercicios que no se repiten, sin IA: cada
 * plantilla pone números, letras y límites al azar y el juego calcula las
 * respuestas de los casos. Las pruebas compilan la solución de cada una con
 * muchos azares y comprueban que pasa todos sus casos (y la plantilla, no).
 * Con un cerebro de IA, además, el Preparador pide ejercicios nuevos.
 */
public final class PlantillasDeEjercicios {

    private PlantillasDeEjercicios() {
    }

    /** Los temas que tienen plantillas. */
    public static final List<String> TEMAS = List.of("02", "03", "04", "05", "06", "07", "08", "21");

    /** Un ejercicio nuevo del tema (o null si no tiene plantillas). */
    public static Ejercicio generar(String tema, Random r) {
        String texto;
        switch (tema) {
            case "02": texto = segundos(r); break;
            case "03": texto = energia(r); break;
            case "04": texto = decidir(r); break;
            case "05": texto = multiplos(r); break;
            case "06": texto = potencia(r); break;
            case "07": texto = mayores(r); break;
            case "08": texto = letras(r); break;
            case "21": texto = cuentaAtras(r); break;
            default: return null;
        }
        try {
            List<Ejercicio> l = Ejercicio.leer(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), "azar");
            return l.isEmpty() ? null : l.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private static int entre(Random r, int desde, int hasta) {
        return desde + r.nextInt(hasta - desde + 1);
    }

    /** El texto de un ejercicio en el formato de res/ejercicios (docs/FORMATOS.md). */
    private static String ejercicio(String tema, int dificultad, String[] titulo, String[] enunciado, String firma, String plantilla, String solucion,
            List<String> casos) {
        StringBuilder b = new StringBuilder("=== azar_").append(tema).append('_').append(Integer.toHexString(String.join("", enunciado).hashCode() & 0xFFFFF))
                .append("\ntema: ").append(tema).append("\ndificultad: ").append(dificultad).append("\njava: 8\ntipo: metodo\n");
        String[] idiomas = {"es", "de", "en"};
        for (int i = 0; i < 3; i++) b.append("titulo.").append(idiomas[i]).append(": ").append(titulo[i]).append('\n');
        for (int i = 0; i < 3; i++) b.append("enunciado.").append(idiomas[i]).append(": ").append(enunciado[i]).append('\n');
        b.append("firma: ").append(firma).append("\nplantilla:\n    ").append(plantilla).append("\nsolucion:\n");
        for (String l : solucion.split("\n")) b.append("    ").append(l).append('\n');
        b.append("casos:\n");
        for (String c : casos) b.append("    ").append(c).append('\n');
        return b.toString();
    }

    static String segundos(Random r) {
        int extra = entre(r, 2, 15);
        List<String> casos = new ArrayList<>();
        for (int m : new int[]{0, 1, entre(r, 2, 9), entre(r, 10, 60)}) casos.add("segundos(" + m + ") => " + (m * 60 + extra));
        return ejercicio("02", 1, new String[]{"Segundos de batería", "Sekunden Akku", "Battery seconds"},
                new String[]{"La batería dura «minutos» minutos y " + extra + " segundos de reserva. Devuelve cuántos segundos son en total.",
                    "Der Akku hält «minutos» Minuten und " + extra + " Sekunden Reserve. Gib zurück, wie viele Sekunden das insgesamt sind.",
                    "The battery lasts «minutos» minutes plus " + extra + " reserve seconds. Return how many seconds that is in total."},
                "static int segundos(int minutos)", "return 0;", "return minutos * 60 + " + extra + ";", casos);
    }

    static String energia(Random r) {
        int base = entre(r, 80, 150), coste = entre(r, 3, 12);
        List<String> casos = new ArrayList<>();
        for (int g : new int[]{0, 1, entre(r, 2, 5), entre(r, 6, 9)}) casos.add("energiaTras(" + g + ") => " + (base - g * coste));
        return ejercicio("03", 1, new String[]{"Energía tras los golpes", "Energie nach den Treffern", "Energy after the hits"},
                new String[]{"El robot empieza con " + base + " de energía y cada golpe le quita " + coste + ". Devuelve la energía tras «golpes» golpes.",
                    "Der Roboter startet mit " + base + " Energie, jeder Treffer kostet " + coste + ". Gib die Energie nach «golpes» Treffern zurück.",
                    "The robot starts with " + base + " energy and each hit costs " + coste + ". Return the energy after «golpes» hits."},
                "static int energiaTras(int golpes)", "return 0;", "return " + base + " - golpes * " + coste + ";", casos);
    }

    static String decidir(Random r) {
        int limite = entre(r, 20, 70);
        List<String> casos = new ArrayList<>();
        for (int e : new int[]{limite + 1 + r.nextInt(30), limite, Math.max(0, limite - 1 - r.nextInt(15)), 100}) {
            casos.add("accion(" + e + ") => \"" + (e > limite ? "ATACAR" : "HUIR") + "\"");
        }
        return ejercicio("04", 2, new String[]{"¿Atacar o huir?", "Angreifen oder fliehen?", "Attack or flee?"},
                new String[]{"Con más de " + limite + " de energía, devuelve \"ATACAR\"; si no, \"HUIR\".",
                    "Mit mehr als " + limite + " Energie gib \"ANGREIFEN\" zurück, sonst \"FLIEHEN\".",
                    "With more than " + limite + " energy return \"ATTACK\"; otherwise \"FLEE\"."},
                "static String accion(int energia)", "return \"\";", "return energia > " + limite + " ? \"ATACAR\" : \"HUIR\";", casos)
                + "terminos.de:\n    \"ATACAR\" ==> \"ANGREIFEN\"\n    \"HUIR\" ==> \"FLIEHEN\"\n"
                + "terminos.en:\n    \"ATACAR\" ==> \"ATTACK\"\n    \"HUIR\" ==> \"FLEE\"\n";
    }

    static String multiplos(Random r) {
        int k = entre(r, 2, 7);
        List<String> casos = new ArrayList<>();
        for (int n : new int[]{0, k - 1, k, entre(r, 10, 30), entre(r, 31, 60)}) {
            int s = 0;
            for (int i = k; i <= n; i += k) s += i;
            casos.add("sumaDeMultiplos(" + n + ") => " + s);
        }
        return ejercicio("05", 2, new String[]{"Suma de múltiplos de " + k, "Summe der Vielfachen von " + k, "Sum of multiples of " + k},
                new String[]{"Devuelve la suma de los múltiplos de " + k + " entre 1 y n (n incluido). Por ejemplo, con n = " + (2 * k) + ": " + k + " + "
                    + (2 * k) + " = " + (3 * k) + ".",
                    "Gib die Summe der Vielfachen von " + k + " zwischen 1 und n (n eingeschlossen) zurück. Beispiel n = " + (2 * k) + ": " + k + " + "
                    + (2 * k) + " = " + (3 * k) + ".",
                    "Return the sum of the multiples of " + k + " from 1 to n (n included). For n = " + (2 * k) + ": " + k + " + " + (2 * k) + " = "
                    + (3 * k) + "."},
                "static int sumaDeMultiplos(int n)", "return 0;",
                "int s = 0;\nfor (int i = " + k + "; i <= n; i += " + k + ") {\n    s += i;\n}\nreturn s;", casos);
    }

    static String potencia(Random r) {
        int k = entre(r, 2, 6), c = entre(r, 1, 20);
        List<String> casos = new ArrayList<>();
        for (int n : new int[]{0, 1, entre(r, 2, 9), entre(r, 10, 50)}) casos.add("potencia(" + n + ") => " + (n * k + c));
        return ejercicio("06", 1, new String[]{"La potencia del láser", "Die Laserstärke", "Laser power"},
                new String[]{"La potencia es el nivel por " + k + ", más " + c + ". Devuelve la potencia de un nivel.",
                    "Die Stärke ist die Stufe mal " + k + ", plus " + c + ". Gib die Stärke einer Stufe zurück.",
                    "The power is the level times " + k + ", plus " + c + ". Return the power of a level."},
                "static int potencia(int nivel)", "return 0;", "return nivel * " + k + " + " + c + ";", casos);
    }

    static String mayores(Random r) {
        int limite = entre(r, 10, 40);
        List<String> casos = new ArrayList<>();
        for (int caso = 0; caso < 4; caso++) {
            int n = caso == 0 ? 0 : entre(r, 1, 7);
            StringBuilder a = new StringBuilder();
            int cuenta = 0;
            for (int i = 0; i < n; i++) {
                int v = entre(r, 0, 60);
                if (v > limite) cuenta++;
                a.append(i == 0 ? "" : ", ").append(v);
            }
            casos.add("contarMayores(new int[]{" + a + "}) => " + cuenta);
        }
        return ejercicio("07", 2, new String[]{"Golpes fuertes", "Starke Treffer", "Strong hits"},
                new String[]{"Cuenta cuántos daños del array pasan de " + limite + ".", "Zähle, wie viele Schäden im Array größer als " + limite + " sind.",
                    "Count how many damages in the array are greater than " + limite + "."},
                "static int contarMayores(int[] danos)", "return 0;",
                "int n = 0;\nfor (int d : danos) {\n    if (d > " + limite + ") n++;\n}\nreturn n;", casos);
    }

    static String letras(Random r) {
        char letra = "aeiorst".charAt(r.nextInt(7));
        String[] palabras = {"metajavbot", "laser", "arena", "robot", "radar", "turbo", "motor", "sensor"};
        List<String> casos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            // Uno de los casos lleva la letra seguro (si no, «return 0» podría acertarlos todos).
            String p = i == 0 ? "" : i == 1 ? "b" + letra + "p" + letra + "z" : palabras[r.nextInt(palabras.length)];
            int n = 0;
            for (char c : p.toCharArray()) if (c == letra) n++;
            casos.add("cuantas(\"" + p + "\") => " + n);
        }
        return ejercicio("08", 2, new String[]{"¿Cuántas «" + letra + "»?", "Wie viele «" + letra + "»?", "How many «" + letra + "»?"},
                new String[]{"Devuelve cuántas veces sale la letra '" + letra + "' en el texto.", "Gib zurück, wie oft der Buchstabe '" + letra
                    + "' im Text vorkommt.", "Return how many times the letter '" + letra + "' appears in the text."},
                "static int cuantas(String texto)", "return 0;",
                "int n = 0;\nfor (char c : texto.toCharArray()) {\n    if (c == '" + letra + "') n++;\n}\nreturn n;", casos);
    }

    static String cuentaAtras(Random r) {
        int paso = entre(r, 1, 3);
        List<String> casos = new ArrayList<>();
        for (int n : new int[]{0, paso, entre(r, 4, 9), entre(r, 10, 20)}) {
            int s = 0;
            for (int i = n; i > 0; i -= paso) s += i;
            casos.add("sumaAtras(" + n + ") => " + s);
        }
        return ejercicio("21", 3, new String[]{"Cuenta atrás que suma", "Countdown, der addiert", "Countdown that adds"},
                new String[]{"Con recursión: suma n, n - " + paso + ", n - " + (2 * paso) + "… mientras sea mayor que 0. Con n ≤ 0, devuelve 0.",
                    "Mit Rekursion: addiere n, n - " + paso + ", n - " + (2 * paso) + "… solange es größer als 0 ist. Bei n ≤ 0 gib 0 zurück.",
                    "With recursion: add n, n - " + paso + ", n - " + (2 * paso) + "… while it is greater than 0. For n ≤ 0 return 0."},
                "static int sumaAtras(int n)", "return 0;", "if (n <= 0) return 0;\nreturn n + sumaAtras(n - " + paso + ");", casos);
    }
}
