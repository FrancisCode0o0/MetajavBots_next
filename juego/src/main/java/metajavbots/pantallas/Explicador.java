package metajavbots.pantallas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import metajavbots.codigo.Compilador;
import metajavbots.progreso.Etiqueta;
import metajavbots.textos.Idioma;

/**
 * EXPLICADOR — Explica un error de compilación según el nivel del jugador
 * (res/textos/errores.txt): a un Noob, con calma y con un ejemplo; a un
 * Junior, el concepto; a un Senior, una línea. Debajo va siempre el mensaje
 * de javac, para que aprenda a leerlo.
 */
public final class Explicador {

    private static Map<String, String> textos;

    private Explicador() {
    }

    private static synchronized Map<String, String> textos() {
        if (textos == null) {
            textos = new HashMap<>();
            try (InputStream in = Explicador.class.getResourceAsStream("/textos/errores.txt")) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    String linea, codigo = null;
                    while ((linea = r.readLine()) != null) {
                        linea = linea.strip();
                        if (linea.isEmpty() || linea.startsWith("#")) continue;
                        if (linea.startsWith("[") && linea.endsWith("]")) codigo = linea.substring(1, linea.length() - 1).strip();
                        else if (codigo != null && linea.contains("=")) {
                            int i = linea.indexOf('=');
                            textos.put(codigo + "|" + linea.substring(0, i).strip(), linea.substring(i + 1).strip());
                        }
                    }
                }
            } catch (IOException e) {
                // sin explicaciones: el mensaje de javac
            }
        }
        return textos;
    }

    public static String nivel(Etiqueta e) {
        switch (e.categoria()) {
            case NOOB: return "noob";
            case JUNIOR: return "junior";
            default: return "senior";
        }
    }

    /**
     * La explicación de ese error en un nivel concreto («noob», «junior» o «senior»), sin el mensaje de javac
     * debajo; null si no la hay. El entrenador flotante la usa para dar más o menos pista.
     */
    public static String enNivel(String codigo, String mensaje, int linea, String nivel, Idioma idioma) {
        if (codigo == null) return null;
        String s = textos().get(codigo + "|" + nivel + "." + idioma.codigo());
        if (s == null) s = textos().get(codigo + "|" + nivel + ".es");
        return s == null ? null : s.replace("{msg}", mensaje == null ? "" : mensaje).replace("{linea}", Integer.toString(linea));
    }

    /** La explicación para el jugador (con el mensaje de javac debajo). */
    public static String explicar(Compilador.Fallo f, Etiqueta etiqueta, Idioma idioma) {
        String n = nivel(etiqueta), clave = f.codigo + "|" + n + "." + idioma.codigo();
        String s = textos().get(clave);
        if (s == null) s = textos().get(f.codigo + "|" + n + ".es");
        String javac = f.mensaje;
        if (s == null) return (f.linea > 0 ? idioma.elegir("Línea ", "Zeile ", "Line ") + f.linea + ": " : "") + javac;
        s = s.replace("{msg}", javac).replace("{linea}", Long.toString(f.linea));
        if (f.codigo.startsWith("metajavbots.")) return s;
        return s + "\n\njavac: " + javac;
    }
}
