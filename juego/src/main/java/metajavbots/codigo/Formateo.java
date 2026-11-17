package metajavbots.codigo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * FORMATEO — Ordena la sangría de un código Java con el estilo de los
 * profesionales (guía de Google para Java): 4 espacios por nivel, «case»
 * dentro del «switch», +8 en las líneas que siguen una sentencia partida y
 * +4 bajo un «if» sin llaves. Solo toca los espacios del principio y del
 * final de cada línea: el código queda igual letra por letra (lo comprueban
 * las pruebas). Los bloques de texto (""" … """) no se tocan.
 */
public final class Formateo {

    private Formateo() {
    }

    private static final class Abierta {
        final char c;
        final boolean esSwitch;

        Abierta(char c, boolean esSwitch) {
            this.c = c;
            this.esSwitch = esSwitch;
        }
    }

    public static String formatear(String fuente) {
        String texto = Documento.normalizar(fuente);
        List<Lexico.Trozo> trozos = Lexico.trozos(texto);
        String[] lineas = texto.split("\n", -1);
        int[] inicios = new int[lineas.length];
        for (int i = 1; i < lineas.length; i++) inicios[i] = inicios[i - 1] + lineas[i - 1].length() + 1;

        Deque<Abierta> pila = new ArrayDeque<>();
        StringBuilder salida = new StringBuilder(texto.length() + 64);
        int k = 0;                       // el siguiente trozo por mirar
        Lexico.Trozo ultimoCodigo = null; // el último trozo de código de las líneas anteriores
        String primeraDeLaAnterior = "";  // la primera palabra de la línea de código anterior
        boolean anteriorEraControlSinLlave = false;
        int sangriaDelComentario = 0;

        for (int l = 0; l < lineas.length; l++) {
            String linea = lineas[l];
            int ini = inicios[l], fin = ini + linea.length();
            if (l > 0) salida.append('\n');

            // ¿La línea empieza dentro de un trozo de varias líneas (comentario o bloque de texto)?
            Lexico.Trozo dentro = null;
            while (k < trozos.size() && trozos.get(k).fin <= ini) k++;
            if (k < trozos.size() && trozos.get(k).inicio < ini) dentro = trozos.get(k);
            if (dentro != null) {
                if (dentro.clase == Lexico.Clase.BLOQUE_DE_TEXTO) {
                    salida.append(linea);
                } else {
                    String t = linea.strip();
                    if (t.startsWith("*")) salida.append(" ".repeat(sangriaDelComentario + 1)).append(t);
                    else salida.append(linea.stripTrailing());
                }
                // Lo que quede de la línea tras el cierre del trozo cuenta para las llaves.
                for (int j = k + 1; j < trozos.size() && trozos.get(j).inicio < fin; j++) {
                    ultimoCodigo = contar(trozos, j, pila, ultimoCodigo);
                }
                continue;
            }
            String t = linea.strip();
            if (t.isEmpty()) {
                continue;
            }
            // Los trozos de esta línea.
            int desde = k, hasta = k;
            while (hasta < trozos.size() && trozos.get(hasta).inicio < fin) hasta++;
            Lexico.Trozo primero = desde < hasta ? trozos.get(desde) : null;

            int nivel = 0;
            for (Abierta a : pila) if (a.c == '{') nivel++;
            Abierta interior = pila.peek();
            boolean empiezaCierre = primero != null && (primero.es("}") || primero.es(")") || primero.es("]"));
            if (primero != null && primero.es("}") && nivel > 0) nivel--;
            int sangria = nivel * Documento.SANGRIA;
            boolean enParentesis = interior != null && interior.c != '{';
            boolean esCaso = primero != null && (primero.es("case") || primero.es("default"));
            if (interior != null && interior.esSwitch && !esCaso && !empiezaCierre) sangria += Documento.SANGRIA;
            if (enParentesis && !empiezaCierre) {
                sangria += 2 * Documento.SANGRIA;
            } else if (!enParentesis && primero != null && primero.esCodigo() && !empiezaCierre && !primero.es("{")
                    && ultimoCodigo != null) {
                if (anteriorEraControlSinLlave) {
                    sangria += Documento.SANGRIA;
                } else if (!terminaSentencia(ultimoCodigo, primeraDeLaAnterior)) {
                    sangria += 2 * Documento.SANGRIA;
                }
            }
            if (primero != null && (primero.clase == Lexico.Clase.COMENTARIO_BLOQUE || primero.clase == Lexico.Clase.JAVADOC)) {
                sangriaDelComentario = sangria;
            }
            salida.append(" ".repeat(sangria)).append(t);

            boolean habiaCodigo = false;
            String primeraPalabra = null;
            for (int j = desde; j < hasta; j++) {
                Lexico.Trozo tr = trozos.get(j);
                if (tr.esCodigo()) {
                    if (primeraPalabra == null) primeraPalabra = tr.texto;
                    habiaCodigo = true;
                }
                ultimoCodigo = contar(trozos, j, pila, ultimoCodigo);
            }
            if (habiaCodigo) {
                anteriorEraControlSinLlave = esControlSinLlave(trozos, desde, hasta);
                primeraDeLaAnterior = primeraPalabra;
            }
            // El último trozo puede seguir en la línea siguiente (un comentario /* … o un bloque de texto).
            k = Math.max(desde, hasta - 1);
        }
        return salida.toString();
    }

    /** Apunta en la pila lo que abre o cierra el trozo j y devuelve el último trozo de código. */
    private static Lexico.Trozo contar(List<Lexico.Trozo> trozos, int j, Deque<Abierta> pila, Lexico.Trozo ultimo) {
        Lexico.Trozo t = trozos.get(j);
        if (!t.esCodigo()) return ultimo;
        if (t.es("{")) {
            pila.push(new Abierta('{', vieneDeSwitch(trozos, j)));
        } else if (t.es("(") || t.es("[")) {
            pila.push(new Abierta(t.texto.charAt(0), false));
        } else if (t.es("}") || t.es(")") || t.es("]")) {
            char abre = t.es("}") ? '{' : t.es(")") ? '(' : '[';
            // Se cierra lo que toca (y lo que quedó abierto dentro, si el código está a medias).
            while (!pila.isEmpty() && pila.peek().c != abre) pila.pop();
            if (!pila.isEmpty()) pila.pop();
        }
        return t;
    }

    /** ¿La «{» del trozo j abre el cuerpo de un switch? (… switch ( … ) {) */
    private static boolean vieneDeSwitch(List<Lexico.Trozo> trozos, int j) {
        int i = j - 1;
        while (i >= 0 && !trozos.get(i).esCodigo()) i--;
        if (i < 0 || !trozos.get(i).es(")")) return false;
        int nivel = 0;
        for (; i >= 0; i--) {
            Lexico.Trozo t = trozos.get(i);
            if (!t.esCodigo()) continue;
            if (t.es(")")) nivel++;
            else if (t.es("(")) {
                nivel--;
                if (nivel == 0) break;
            }
        }
        i--;
        while (i >= 0 && !trozos.get(i).esCodigo()) i--;
        return i >= 0 && trozos.get(i).es("switch");
    }

    private static boolean terminaSentencia(Lexico.Trozo ultimo, String primeraDeLaAnterior) {
        if (ultimo.es(";") || ultimo.es("{") || ultimo.es("}") || ultimo.es(",") || ultimo.es(":") || ultimo.es("->")) {
            return true;
        }
        // Una línea que solo era una anotación (@Override) no parte la sentencia.
        if (ultimo.clase == Lexico.Clase.ANOTACION || ultimo.es(")") && primeraDeLaAnterior != null
                && primeraDeLaAnterior.startsWith("@")) {
            return true;
        }
        return false;
    }

    /** ¿La línea es un «if (…)», «for (…)», «while (…)» o «else» sin llave, cuyo cuerpo va en la línea siguiente? */
    private static boolean esControlSinLlave(List<Lexico.Trozo> trozos, int desde, int hasta) {
        Lexico.Trozo primero = null, ultimo = null;
        for (int j = desde; j < hasta; j++) {
            Lexico.Trozo t = trozos.get(j);
            if (!t.esCodigo()) continue;
            if (primero == null) primero = t;
            ultimo = t;
        }
        if (primero == null) return false;
        if (primero.es("}")) {
            // «} else» sin llave
            Lexico.Trozo segundo = null;
            for (int j = desde; j < hasta; j++) {
                if (trozos.get(j).esCodigo() && trozos.get(j) != primero) {
                    segundo = trozos.get(j);
                    break;
                }
            }
            if (segundo == null || !segundo.es("else")) return false;
            primero = segundo;
        }
        if (primero.es("else")) return ultimo.es("else") || ultimo.es(")");
        boolean control = primero.es("if") || primero.es("for") || primero.es("while");
        return control && ultimo.es(")");
    }
}
