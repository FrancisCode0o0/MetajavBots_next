package castillafantasx.eventos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * VARIABLES — Lo que ya ha pasado y lo que se sabe, con nombre: un sí o un no
 * («vio_la_manada»), un número («pistas») o un texto («final»). Cada misión
 * tiene las suyas, y la crónica del héroe, las de todas (docs/EVENTOS.md).
 * Por dentro todo es texto: «si» y «no», «3», «2.5» o el texto tal cual.
 */
public final class Variables {

    public static final String SI = "si", NO = "no";

    private final Map<String, String> valores = new LinkedHashMap<>();
    /** Las que han cambiado desde la última vez que se miró (para los eventos que saltan «al cambiar una variable»). */
    public final Set<String> cambiadas = new LinkedHashSet<>();

    public boolean existe(String nombre) {
        return valores.containsKey(nombre);
    }

    /** Los nombres, en el orden en que se pusieron. */
    public List<String> nombres() {
        return new ArrayList<>(valores.keySet());
    }

    /** El valor como texto ("" si no existe). */
    public String texto(String nombre) {
        return valores.getOrDefault(nombre, "");
    }

    /** ¿Es que sí? Un «no», un 0 o nada son que no; lo demás, que sí. */
    public boolean si(String nombre) {
        return esSi(texto(nombre));
    }

    /** El valor como número (0 si no lo es; «si» vale 1). */
    public double numero(String nombre) {
        return aNumero(texto(nombre));
    }

    public void poner(String nombre, String valor) {
        String limpio = normal(valor);
        if (limpio.equals(valores.get(nombre))) return;
        valores.put(nombre, limpio);
        cambiadas.add(nombre);
    }

    public void poner(String nombre, boolean valor) {
        poner(nombre, valor ? SI : NO);
    }

    public void poner(String nombre, double valor) {
        poner(nombre, deNumero(valor));
    }

    public void sumar(String nombre, double cuanto) {
        poner(nombre, numero(nombre) + cuanto);
    }

    /** Pone el valor solo si la variable aún no existe (los valores de partida de una misión). */
    public void siFalta(String nombre, String valor) {
        if (!valores.containsKey(nombre)) valores.put(nombre, normal(valor));
    }

    public void quitar(String nombre) {
        valores.remove(nombre);
    }

    public void vaciar() {
        valores.clear();
        cambiadas.clear();
    }

    // ------------------------------------------------------------------
    // Qué es cada valor
    // ------------------------------------------------------------------

    public static boolean esSi(String valor) {
        String v = valor == null ? "" : valor.trim().toLowerCase(java.util.Locale.ROOT);
        return !(v.isEmpty() || v.equals(NO) || v.equals("false") || v.equals("0"));
    }

    /** ¿Es un sí o un no? (Para F1: Intro lo cambia.) */
    public static boolean esSiONo(String valor) {
        return valor.equals(SI) || valor.equals(NO);
    }

    public static boolean esNumero(String valor) {
        try {
            Double.parseDouble(valor.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static double aNumero(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.equals(SI) || v.equals("true")) return 1;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** El número como se guarda: sin decimales si no los tiene («3», no «3.0»). */
    public static String deNumero(double n) {
        return n == Math.rint(n) && Math.abs(n) < 1e15 ? String.valueOf((long) n) : String.valueOf(n);
    }

    // «sí», «true» y «verdad» se guardan como «si»; «false», como «no»; las comillas de un texto se quitan.
    private static String normal(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) return v.substring(1, v.length() - 1);
        String m = v.toLowerCase(java.util.Locale.ROOT);
        if (m.equals("sí") || m.equals("si") || m.equals("true")) return SI;
        if (m.equals("no") || m.equals("false")) return NO;
        return v;
    }

    // ------------------------------------------------------------------
    // Guardar y cargar: "nombre=valor;nombre=valor"
    // ------------------------------------------------------------------

    public String aTexto() {
        List<String> pares = new ArrayList<>();
        for (Map.Entry<String, String> v : valores.entrySet()) {
            pares.add(escapar(v.getKey()) + "=" + escapar(v.getValue()));
        }
        return String.join(";", pares);
    }

    public void deTexto(String texto) {
        vaciar();
        if (texto == null) return;
        for (String par : texto.split(";")) {
            int igual = par.indexOf('=');
            if (igual > 0) valores.put(desescapar(par.substring(0, igual).trim()), desescapar(par.substring(igual + 1)));
        }
    }

    /** En un texto guardado, los signos que separan y los saltos de línea van como %XX. */
    public static String escapar(String texto) {
        StringBuilder b = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (";=%|,:~\n\r".indexOf(c) >= 0) b.append('%').append(String.format("%02X", (int) c));
            else b.append(c);
        }
        return b.toString();
    }

    public static String desescapar(String texto) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '%' && i + 2 < texto.length()) {
                b.append((char) Integer.parseInt(texto.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
