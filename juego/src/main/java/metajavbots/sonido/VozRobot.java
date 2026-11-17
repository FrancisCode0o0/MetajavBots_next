package metajavbots.sonido;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import metajavbots.textos.Idioma;

/**
 * VOZ ROBOT — Un sintetizador de voz por formantes, hecho aquí (sin internet
 * ni bibliotecas de fuera): pasa el texto a sonidos (reglas de cada idioma:
 * el español casi se lee como se escribe; el alemán y el inglés, con sus
 * reglas más comunes), y cada sonido se fabrica con una fuente de zumbido o
 * de ruido y tres resonadores (los formantes de la voz). Encima, el efecto
 * robótico: modulación en anillo, un eco metálico corto y saltos de tono de
 * locutor chiflado. Se entiende, y suena a máquina a propósito.
 */
public final class VozRobot {

    static final int F = Sintetizador.FRECUENCIA;

    private VozRobot() {
    }

    /** Un sonido de la voz: vocal, consonante o pausa. */
    private static final class Fono {
        final char c;
        final boolean tonico;

        Fono(char c, boolean tonico) {
            this.c = c;
            this.tonico = tonico;
        }
    }

    // ================================================================ del texto a los sonidos

    /**
     * Los sonidos, con un alfabeto propio de una letra cada uno:
     * a e i o u y(ü) ø(ö) @ (schwa) · p t k b d g · f s S(sh) x(j/ch) h z(s sonora) T(th) · m n N(ñ) · l r R(rr) · w j(y) · C(ch) · espacio y comas (pausas).
     */
    static List<Fono> fonos(String texto, Idioma idioma) {
        String s = normalizar(texto, idioma);
        StringBuilder f = new StringBuilder();
        for (String palabra : s.split("(?<=[ ,.!?])|(?=[ ,.!?])")) {
            if (palabra.isEmpty()) continue;
            char c0 = palabra.charAt(0);
            if (" ,.!?".indexOf(c0) >= 0) {
                f.append(c0 == ' ' ? ' ' : ',');
                continue;
            }
            switch (idioma) {
                case DE: f.append(aleman(palabra)); break;
                case EN: f.append(ingles(palabra)); break;
                default: f.append(espanol(palabra)); break;
            }
        }
        List<Fono> l = new ArrayList<>();
        // Acento: la primera vocal de cada palabra en alemán e inglés; la penúltima en español (aproximado).
        String fs = f.toString();
        int ini = 0;
        for (int i = 0; i <= fs.length(); i++) {
            if (i == fs.length() || fs.charAt(i) == ' ' || fs.charAt(i) == ',') {
                String pal = fs.substring(ini, i);
                int tonica = tonica(pal, idioma);
                int v = 0;
                for (int k = 0; k < pal.length(); k++) {
                    char c = pal.charAt(k);
                    boolean esVocal = vocal(c);
                    l.add(new Fono(c, esVocal && v == tonica));
                    if (esVocal) v++;
                }
                if (i < fs.length()) l.add(new Fono(fs.charAt(i), false));
                ini = i + 1;
            }
        }
        return l;
    }

    private static int tonica(String pal, Idioma idioma) {
        int n = 0;
        for (char c : pal.toCharArray()) if (vocal(c)) n++;
        if (idioma != Idioma.ES) return 0;
        return n <= 1 ? 0 : n - 2;
    }

    static boolean vocal(char c) {
        return "aeiouyø@".indexOf(c) >= 0;
    }

    static String normalizar(String t, Idioma idioma) {
        String s = t.toLowerCase(Locale.ROOT);
        StringBuilder b = new StringBuilder();
        StringBuilder numero = new StringBuilder();
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : ' ';
            if (Character.isDigit(c)) {
                numero.append(c);
                continue;
            }
            if (numero.length() > 0) {
                b.append(' ').append(numeroEnLetras(Integer.parseInt(numero.length() > 6 ? "999999" : numero.toString()), idioma)).append(' ');
                numero.setLength(0);
            }
            if (i == s.length()) break;
            if (Character.isLetter(c) || c == '\'') b.append(c);
            else if (c == ',' || c == ';' || c == ':' || c == '…' || c == '—' || c == '-') b.append(',');
            else if (c == '.' || c == '!' || c == '?' || c == '¡' || c == '¿') b.append(c == '¡' || c == '¿' ? ' ' : c);
            else b.append(' ');
        }
        return b.toString().replaceAll(" +", " ").strip();
    }

    static String numeroEnLetras(int n, Idioma i) {
        String[][] u = {
                {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce",
                        "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve"},
                {"null", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn", "elf", "zwölf", "dreizehn", "vierzehn",
                        "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn"},
                {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen",
                        "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"}};
        String[][] d = {
                {"", "", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"},
                {"", "", "zwanzig", "dreißig", "vierzig", "fünfzig", "sechzig", "siebzig", "achtzig", "neunzig"},
                {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"}};
        int k = i.ordinal();
        if (n < 20) return u[k][n];
        if (n < 100) {
            String dec = d[k][n / 10];
            if (n % 10 == 0) return dec;
            if (i == Idioma.ES) return dec + " y " + u[k][n % 10];
            if (i == Idioma.DE) return u[k][n % 10] + "und" + dec;
            return dec + " " + u[k][n % 10];
        }
        if (n < 1000) {
            String cien = i.elegir(n / 100 == 1 ? "cien" : u[0][n / 100] + "cientos", u[1][n / 100] + "hundert", u[2][n / 100] + " hundred");
            return cien + (n % 100 == 0 ? "" : " " + numeroEnLetras(n % 100, i));
        }
        String mil = i.elegir(n / 1000 == 1 ? "mil" : numeroEnLetras(n / 1000, i) + " mil", numeroEnLetras(n / 1000, i) + "tausend",
                numeroEnLetras(n / 1000, i) + " thousand");
        return mil + (n % 1000 == 0 ? "" : " " + numeroEnLetras(n % 1000, i));
    }

    static String espanol(String p) {
        p = p.replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ü", "u");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i), n = i + 1 < p.length() ? p.charAt(i + 1) : ' ';
            switch (c) {
                case 'c':
                    if (n == 'h') { b.append('C'); i++; }
                    else if (n == 'e' || n == 'i') b.append('s');
                    else b.append('k');
                    break;
                case 'q': b.append('k'); if (n == 'u') i++; break;
                case 'g':
                    if (n == 'u' && i + 2 < p.length() && (p.charAt(i + 2) == 'e' || p.charAt(i + 2) == 'i')) { b.append('g'); i++; }
                    else if (n == 'e' || n == 'i') b.append('x');
                    else b.append('g');
                    break;
                case 'j': b.append('x'); break;
                case 'h': break;
                case 'l': if (n == 'l') { b.append('j'); i++; } else b.append('l'); break;
                case 'r': if (n == 'r' || i == 0) { b.append('R'); if (n == 'r') i++; } else b.append('r'); break;
                case 'ñ': b.append('N'); break;
                case 'v': b.append('b'); break;
                case 'z': b.append('s'); break;
                case 'x': b.append("ks"); break;
                case 'y': b.append(i == p.length() - 1 ? 'i' : 'j'); break;
                case 'w': b.append('u'); break;
                default: if ("aeioubdfkmnpst".indexOf(c) >= 0) b.append(c); break;
            }
        }
        return b.toString();
    }

    static String aleman(String p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            String r = p.substring(i);
            char c = p.charAt(i), n = i + 1 < p.length() ? p.charAt(i + 1) : ' ';
            boolean fin = i == p.length() - 1;
            if (r.startsWith("sch")) { b.append('S'); i += 2; continue; }
            if (i == 0 && (r.startsWith("sp") || r.startsWith("st"))) { b.append('S'); continue; }
            if (r.startsWith("ch")) { b.append(i > 0 && "aou".indexOf(p.charAt(i - 1)) >= 0 ? 'x' : 'S'); i++; continue; }
            if (r.startsWith("ei") || r.startsWith("ai")) { b.append("ai"); i++; continue; }
            if (r.startsWith("ie")) { b.append('i'); i++; continue; }
            if (r.startsWith("eu") || r.startsWith("äu")) { b.append("oi"); i++; continue; }
            if (r.startsWith("au")) { b.append("au"); i++; continue; }
            if (r.startsWith("qu")) { b.append("kv"); i++; continue; }
            if (r.startsWith("ck")) { b.append('k'); i++; continue; }
            if (r.startsWith("ng")) { b.append("ng"); i++; continue; }
            if (r.startsWith("ss") || c == 'ß') { b.append('s'); if (c != 'ß') i++; continue; }
            switch (c) {
                case 'ä': b.append('e'); break;
                case 'ö': b.append('ø'); break;
                case 'ü': b.append('y'); break;
                case 'w': b.append('v'); break;
                case 'v': b.append('f'); break;
                case 'z': b.append("ts"); break;
                case 'j': b.append('j'); break;
                case 'y': b.append('y'); break;
                case 's': b.append(vocal(n) && (i == 0 || vocal(p.charAt(i - 1))) ? 'z' : 's'); break;
                case 'b': b.append(fin ? 'p' : 'b'); break;
                case 'd': b.append(fin ? 't' : 'd'); break;
                case 'g': b.append(fin ? 'k' : 'g'); break;
                case 'h': if (i == 0 || !vocal(p.charAt(i - 1))) b.append('h'); break;
                case 'c': b.append('k'); break;
                case 'e': b.append(fin || r.startsWith("en") && i + 2 >= p.length() || r.startsWith("er") && i + 2 >= p.length() ? '@' : 'e'); break;
                case 'r': b.append(fin ? '@' : 'r'); break;
                case 'x': b.append("ks"); break;
                default: if ("aioufklmnpt".indexOf(c) >= 0) b.append(c); break;
            }
        }
        return b.toString();
    }

    static String ingles(String p) {
        StringBuilder b = new StringBuilder();
        // La «e» muda del final alarga la vocal de antes: «make», «time», «home».
        boolean eMuda = p.length() > 3 && p.endsWith("e") && !vocal(p.charAt(p.length() - 2)) && p.charAt(p.length() - 2) != 'l';
        for (int i = 0; i < p.length(); i++) {
            String r = p.substring(i);
            char c = p.charAt(i), n = i + 1 < p.length() ? p.charAt(i + 1) : ' ';
            if (eMuda && i == p.length() - 1) break;
            boolean largo = eMuda && i == p.length() - 3;
            if (r.startsWith("tion")) { b.append("S@n"); i += 3; continue; }
            if (r.startsWith("igh")) { b.append("ai"); i += 2; continue; }
            if (r.startsWith("th")) { b.append('T'); i++; continue; }
            if (r.startsWith("sh")) { b.append('S'); i++; continue; }
            if (r.startsWith("ch")) { b.append('C'); i++; continue; }
            if (r.startsWith("ph")) { b.append('f'); i++; continue; }
            if (r.startsWith("wh")) { b.append('w'); i++; continue; }
            if (r.startsWith("ck")) { b.append('k'); i++; continue; }
            if (r.startsWith("ng")) { b.append("ng"); i++; continue; }
            if (r.startsWith("ee") || r.startsWith("ea")) { b.append('i'); i++; continue; }
            if (r.startsWith("oo")) { b.append('u'); i++; continue; }
            if (r.startsWith("ou") || r.startsWith("ow")) { b.append("au"); i++; continue; }
            if (r.startsWith("ai") || r.startsWith("ay")) { b.append("ei"); i++; continue; }
            if (r.startsWith("oi") || r.startsWith("oy")) { b.append("oi"); i++; continue; }
            if (r.startsWith("er") && i + 2 >= p.length()) { b.append('@'); i++; continue; }
            switch (c) {
                case 'a': b.append(largo ? "ei" : "a"); break;
                case 'e': b.append(i == p.length() - 1 && p.length() > 2 ? "" : largo ? "i" : "e"); break;
                case 'i': b.append(largo ? "ai" : "i"); break;
                case 'o': b.append(largo ? "ou" : "o"); break;
                case 'u': b.append(largo ? "ju" : "a"); break;
                case 'y': b.append(i == 0 ? 'j' : i == p.length() - 1 ? 'i' : 'i'); break;
                case 'c': b.append(n == 'e' || n == 'i' || n == 'y' ? 's' : 'k'); break;
                case 'g': b.append(n == 'e' || n == 'i' ? "dZ" : "g"); break;
                case 'j': b.append("dZ"); break;
                case 'q': b.append("kw"); if (n == 'u') i++; break;
                case 'x': b.append("ks"); break;
                case 'w': b.append('w'); break;
                case 'h': if (i == 0 || !vocal(p.charAt(i - 1))) b.append('h'); break;
                case 'r': b.append('r'); break;
                case 'z': b.append('z'); break;
                case '\'': break;
                default: if ("bdfklmnpstv".indexOf(c) >= 0) b.append(c); break;
            }
        }
        return b.toString().replace("Z", "S");
    }

    // ================================================================ de los sonidos al audio

    /** Los formantes de cada vocal (F1, F2, F3 en Hz). */
    private static float[] formantes(char c) {
        switch (c) {
            case 'a': return new float[]{750, 1250, 2550};
            case 'e': return new float[]{480, 1900, 2600};
            case 'i': return new float[]{290, 2250, 3000};
            case 'o': return new float[]{480, 900, 2500};
            case 'u': return new float[]{320, 800, 2400};
            case 'y': return new float[]{290, 1700, 2200};
            case 'ø': return new float[]{430, 1500, 2350};
            case '@': return new float[]{520, 1450, 2500};
            case 'l': return new float[]{380, 1100, 2700};
            case 'r': case 'R': return new float[]{450, 1200, 1700};
            case 'm': return new float[]{280, 1000, 2200};
            case 'n': case 'N': return new float[]{280, 1600, 2600};
            case 'w': return new float[]{300, 700, 2300};
            case 'j': return new float[]{280, 2100, 2900};
            case 'b': case 'd': case 'g': case 'v': case 'z': return new float[]{300, 1300, 2500};
            default: return new float[]{500, 1500, 2500};
        }
    }

    /** Un resonador de dos polos (un formante). */
    private static final class Resonador {
        double a, b, c, y1, y2;

        void poner(double f, double ancho) {
            double r = Math.exp(-Math.PI * ancho / F);
            c = -r * r;
            b = 2 * r * Math.cos(2 * Math.PI * f / F);
            a = 1 - b - c;
        }

        double paso(double x) {
            double y = a * x + b * y1 + c * y2;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    /** Dice un texto: devuelve el audio (mono, 22 050 Hz). */
    public static float[] decir(String texto, Idioma idioma, long semilla) {
        List<Fono> fs = fonos(texto, idioma);
        Random azar = new Random(semilla ^ texto.hashCode());
        List<float[]> trozos = new ArrayList<>();
        Resonador r1 = new Resonador(), r2 = new Resonador(), r3 = new Resonador();
        double fase = 0;
        float[] actual = formantes('@');
        boolean exclama = texto.contains("!"), pregunta = texto.contains("?");
        double tonoBase = 118;
        double tonoPalabra = tonoBase;
        int total = 0;
        for (int k = 0; k < fs.size(); k++) {
            Fono fo = fs.get(k);
            char c = fo.c;
            if (c == ' ') {
                // Locutor loco: cada palabra, un tono distinto (y a veces un salto enorme).
                tonoPalabra = tonoBase * (0.85 + azar.nextDouble() * 0.45) * (azar.nextInt(9) == 0 ? 1.6 : 1);
                trozos.add(new float[(int) (F * 0.03)]);
                continue;
            }
            if (c == ',') {
                trozos.add(new float[(int) (F * 0.16)]);
                continue;
            }
            boolean esVocal = vocal(c);
            double dur = esVocal ? (fo.tonico ? 0.15 : 0.095) : "ptkbdgC".indexOf(c) >= 0 ? 0.07 : "sSfxhTz".indexOf(c) >= 0 ? 0.1 : 0.07;
            int n = (int) (dur * F);
            float[] s = new float[n];
            float[] objetivo = formantes(c);
            boolean sordo = "ptkfsSxhTC".indexOf(c) >= 0;
            boolean oclusiva = "ptkbdgC".indexOf(c) >= 0;
            boolean nasal = "mnN".indexOf(c) >= 0;
            double fin = (exclama || pregunta) && k > fs.size() - 4 ? 1.35 : 1;
            for (int i = 0; i < n; i++) {
                double t = (double) i / n;
                // Los formantes van de lo anterior a lo nuevo (las transiciones hacen inteligible la voz).
                double mezcla = Math.min(1, t * 3);
                double f1 = actual[0] + (objetivo[0] - actual[0]) * mezcla;
                double f2 = actual[1] + (objetivo[1] - actual[1]) * mezcla;
                double f3 = actual[2] + (objetivo[2] - actual[2]) * mezcla;
                r1.poner(f1, 90);
                r2.poner(f2, 110);
                r3.poner(f3, 160);
                double tono = tonoPalabra * (fo.tonico ? 1.15 : 1) * (1 + (fin - 1) * t);
                fase += tono / F;
                double pulso = fase % 1.0 < 0.1 ? 1 : -0.11;   // un tren de pulsos: zumbido de máquina
                double ruido = azar.nextDouble() * 2 - 1;
                double fuente;
                if (oclusiva) fuente = t < 0.55 ? 0 : (sordo ? ruido * 0.8 : pulso * 0.6 + ruido * 0.3);
                else if (sordo) fuente = ruido * 0.55;
                else if (c == 'z' || c == 'v') fuente = pulso * 0.5 + ruido * 0.3;
                else fuente = pulso;
                double y;
                if (sordo && !oclusiva) {
                    // Fricativas: ruido filtrado alto (s y S más agudas).
                    double fc = c == 's' ? 5500 : c == 'S' ? 3200 : c == 'f' || c == 'T' ? 6000 : 1800;
                    r3.poner(Math.min(F / 2.3, fc), 1200);
                    y = r3.paso(fuente) * 0.6;
                } else {
                    y = r1.paso(fuente) + r2.paso(fuente) * 0.6 + r3.paso(fuente) * 0.3;
                    if (nasal) y *= 0.45;
                }
                double env = Math.min(1, Math.min(t * 12, (1 - t) * 10));
                s[i] = (float) (y * env * (esVocal ? 0.9 : 0.6));
            }
            if (!sordo || esVocal) actual = objetivo;
            trozos.add(s);
            total += n;
            if (total > F * 12) break;   // nada de discursos de más de 12 segundos
        }
        int largo = 0;
        for (float[] t : trozos) largo += t.length;
        float[] audio = new float[largo + (int) (F * 0.25)];
        int pos = 0;
        for (float[] t : trozos) {
            System.arraycopy(t, 0, audio, pos, t.length);
            pos += t.length;
        }
        return robotico(audio);
    }

    /** El efecto de máquina: modulación en anillo, eco metálico y un poco de saturación. */
    static float[] robotico(float[] a) {
        float maximo = 1e-6f;
        for (float x : a) maximo = Math.max(maximo, Math.abs(x));
        float[] s = new float[a.length];
        int d1 = (int) (F * 0.009), d2 = (int) (F * 0.021);
        for (int i = 0; i < a.length; i++) {
            double x = a[i] / maximo;
            double anillo = Math.sin(2 * Math.PI * 70 * i / F);
            double y = x * (0.62 + 0.38 * anillo);
            if (i >= d1) y += s[i - d1] * 0.35;
            if (i >= d2) y += s[i - d2] * 0.2;
            s[i] = (float) Math.tanh(y * 1.4) * 0.8f;
        }
        return s;
    }
}
