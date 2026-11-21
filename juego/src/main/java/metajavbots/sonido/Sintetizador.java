package metajavbots.sonido;

import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * SINTETIZADOR — Los sonidos de la lucha de robots, hechos aquí con números
 * (osciladores, ruido, barridos de frecuencia y envolventes): láseres,
 * disparos, misiles, golpes de metal, explosiones, servos, chispas, pitidos
 * de la interfaz y fuegos artificiales. Nada viene de fuera, así que son
 * del autor (CC BY 4.0, como el resto del arte).
 */
public final class Sintetizador {

    public static final int FRECUENCIA = 22050;

    private Sintetizador() {
    }

    /** Los nombres de todos los sonidos que sabe hacer. */
    public static final String[] NOMBRES = {
            "clic", "encima", "bien", "mal", "compilar", "laser", "laser_pesado", "disparo", "ametralladora", "misil",
            "explosion", "explosion_grande", "golpe_metal", "golpe_fuerte", "fallo", "llama", "escudo", "turbo", "pulso",
            "reparar", "chispa", "pared", "choque", "servo", "parte_rota", "campana", "fuegos", "estallido", "subida",
            "bajada", "alerta_bajada", "tope", "error", "camuflaje", "obstaculo_roto"};

    public static float[] hacer(String nombre, long semilla) {
        Random r = new Random(semilla ^ nombre.hashCode());
        switch (nombre) {
            case "clic": return tono(0.05f, 1400, 900, Onda.CUADRADA, 0.35f, r);
            case "encima": return tono(0.03f, 2200, 2200, Onda.SENO, 0.15f, r);
            case "bien": return mezclar(tono(0.12f, 880, 880, Onda.CUADRADA, 0.3f, r), retrasar(tono(0.2f, 1320, 1320, Onda.CUADRADA, 0.3f, r), 0.1f));
            case "mal": return mezclar(tono(0.15f, 300, 280, Onda.SIERRA, 0.35f, r), retrasar(tono(0.25f, 220, 200, Onda.SIERRA, 0.35f, r), 0.13f));
            case "compilar": return arpegio(new float[]{523, 659, 784, 1046}, 0.06f, Onda.CUADRADA, 0.25f, r);
            case "laser": return eco(tono(0.32f, 2600, 240, Onda.SIERRA, 0.55f, r), 0.05f, 0.35f);
            case "laser_pesado": return eco(mezclar(tono(0.55f, 1400, 90, Onda.SIERRA, 0.6f, r), tono(0.55f, 1410, 95, Onda.CUADRADA, 0.3f, r)), 0.07f, 0.4f);
            case "disparo": return mezclar(ruido(0.18f, 0.9f, 8, r), tono(0.12f, 180, 60, Onda.SENO, 0.8f, r));
            case "ametralladora": return mezclar(ruido(0.07f, 0.8f, 6, r), tono(0.06f, 220, 90, Onda.CUADRADA, 0.4f, r));
            case "misil": return silbido(0.7f, r);
            case "explosion": return explosion(0.9f, 0.9f, r);
            case "explosion_grande": return eco(explosion(2.2f, 1f, r), 0.12f, 0.35f);
            case "golpe_metal": return metal(0.5f, 0.8f, r);
            case "golpe_fuerte": return mezclar(metal(0.8f, 0.9f, r), tono(0.3f, 120, 40, Onda.SENO, 0.9f, r));
            case "fallo": return barrido(0.25f, 0.5f, r);
            case "llama": return ruido(0.5f, 0.6f, 2, r);
            case "escudo": return eco(tono(0.5f, 300, 900, Onda.SENO, 0.45f, r), 0.04f, 0.5f);
            case "turbo": return tono(0.6f, 120, 1600, Onda.SIERRA, 0.4f, r);
            case "pulso": return eco(mezclar(tono(0.6f, 80, 30, Onda.SENO, 0.9f, r), ruido(0.4f, 0.4f, 1, r)), 0.09f, 0.4f);
            case "reparar": return arpegio(new float[]{660, 880, 990, 1320}, 0.08f, Onda.SENO, 0.3f, r);
            case "chispa": return ruido(0.06f, 0.5f, 1, r);
            case "pared": return mezclar(metal(0.25f, 0.5f, r), tono(0.15f, 90, 50, Onda.SENO, 0.6f, r));
            case "choque": return mezclar(metal(0.45f, 0.8f, r), ruido(0.2f, 0.6f, 4, r));
            case "servo": return servo(0.35f, r);
            case "parte_rota": return mezclar(explosion(0.5f, 0.7f, r), metal(0.6f, 0.6f, r));
            case "campana": return eco(campana(1.2f, 0.7f, r), 0.15f, 0.3f);
            case "fuegos": return silbido(0.6f, r);
            case "estallido": return mezclar(explosion(0.7f, 0.6f, r), chisporroteo(0.9f, r));
            case "subida": return arpegio(new float[]{523, 659, 784, 1046, 1318, 1568}, 0.09f, Onda.CUADRADA, 0.3f, r);
            case "bajada": return arpegio(new float[]{784, 659, 523, 392}, 0.14f, Onda.SIERRA, 0.3f, r);
            case "alerta_bajada": return mezclar(tono(0.18f, 660, 440, Onda.SIERRA, 0.34f, r),
                    retrasar(tono(0.26f, 440, 220, Onda.SIERRA, 0.36f, r), 0.16f));
            case "tope": return tono(0.4f, 400, 400, Onda.CUADRADA, 0.2f, r);
            case "ovacion": return mezclar(ruido(1.6f, 0.32f, 3, r), ruido(1.6f, 0.18f, 1, r));
            case "abucheo": return mezclar(tono(1.2f, 150, 115, Onda.SIERRA, 0.12f, r), ruido(1.2f, 0.12f, 6, r));
            case "bip_triste": return eco(tono(1.1f, 880, 160, Onda.CUADRADA, 0.18f, r), 0.25f, 0.3f);
            case "error": return mezclar(tono(0.2f, 160, 160, Onda.CUADRADA, 0.35f, r), ruido(0.2f, 0.25f, 3, r));
            case "camuflaje": return tono(0.5f, 1800, 200, Onda.SENO, 0.3f, r);
            case "obstaculo_roto": return mezclar(mezclar(ruido(0.35f, 0.85f, 3, r), metal(0.4f, 0.65f, r)), tono(0.18f, 140, 45, Onda.SENO, 0.7f, r));
            default: return tono(0.1f, 440, 440, Onda.SENO, 0.3f, r);
        }
    }

    enum Onda { SENO, CUADRADA, SIERRA, TRIANGULO }

    private static float onda(Onda o, double fase) {
        double f = fase - Math.floor(fase);
        switch (o) {
            case CUADRADA: return f < 0.5 ? 1 : -1;
            case SIERRA: return (float) (2 * f - 1);
            case TRIANGULO: return (float) (f < 0.5 ? 4 * f - 1 : 3 - 4 * f);
            default: return (float) Math.sin(2 * Math.PI * f);
        }
    }

    /** Un tono que va de una frecuencia a otra, con una envolvente de golpe y caída. */
    static float[] tono(float segundos, float desde, float hasta, Onda o, float fuerza, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double f = desde * Math.pow(hasta / desde, t);
            fase += f / FRECUENCIA;
            s[i] = (float) (onda(o, fase) * fuerza * envolvente(t, 0.02, 1.4));
        }
        return s;
    }

    static double envolvente(double t, double ataque, double caida) {
        double a = t < ataque ? t / ataque : 1;
        return a * Math.pow(1 - t, caida);
    }

    /** Ruido filtrado (más «grave» cuanto mayor el suavizado). */
    static float[] ruido(float segundos, float fuerza, int suavizado, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        float y = 0;
        for (int i = 0; i < n; i++) {
            float x = r.nextFloat() * 2 - 1;
            y += (x - y) / Math.max(1, suavizado);
            s[i] = y * fuerza * (float) envolvente((double) i / n, 0.01, 2) * (suavizado > 1 ? 1.6f : 1);
        }
        return s;
    }

    static float[] explosion(float segundos, float fuerza, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        float y = 0, z = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            float x = r.nextFloat() * 2 - 1;
            // Cada vez más grave (el filtro se cierra), con un golpe sordo al principio.
            float k = (float) (0.5 - 0.47 * t);
            y += (x - y) * k;
            z += (y - z) * k;
            double golpe = Math.sin(2 * Math.PI * 55 * i / FRECUENCIA * (1 - t * 0.5)) * Math.exp(-t * 9);
            s[i] = (float) ((z * 2.2 + golpe * 0.8) * fuerza * envolvente(t, 0.005, 1.8));
        }
        return s;
    }

    /** Metal golpeado: parciales inarmónicas que se apagan a distinta velocidad. */
    static float[] metal(float segundos, float fuerza, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        double[] f = {310, 437, 691, 1027, 1513, 2210};
        double base = 0.8 + r.nextDouble() * 0.4;
        for (int i = 0; i < n; i++) {
            double t = (double) i / FRECUENCIA;
            double v = 0;
            for (int k = 0; k < f.length; k++) v += Math.sin(2 * Math.PI * f[k] * base * t) * Math.exp(-t * (6 + k * 4)) / (k + 1);
            if (i < 400) v += (r.nextFloat() * 2 - 1) * (1 - i / 400.0);
            s[i] = (float) (v * fuerza * 0.7);
        }
        return s;
    }

    static float[] campana(float segundos, float fuerza, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        double[] f = {880, 1760 * 1.01, 2640 * 0.98, 3520 * 1.02};
        for (int i = 0; i < n; i++) {
            double t = (double) i / FRECUENCIA, v = 0;
            for (int k = 0; k < f.length; k++) v += Math.sin(2 * Math.PI * f[k] * t) * Math.exp(-t * (2 + k * 2)) / (k + 1);
            s[i] = (float) (v * fuerza * 0.6);
        }
        return s;
    }

    static float[] silbido(float segundos, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        double fase = 0;
        float y = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            fase += (900 + 1400 * t) / FRECUENCIA;
            float x = r.nextFloat() * 2 - 1;
            y += (x - y) * 0.3f;
            s[i] = (float) ((Math.sin(2 * Math.PI * fase) * 0.25 + y * 0.6) * envolvente(t, 0.1, 0.8) * 0.6);
        }
        return s;
    }

    static float[] chisporroteo(float segundos, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            if (r.nextFloat() < 0.02 * (1 - t)) {
                int largo = 80 + r.nextInt(200);
                float fuerza = 0.3f + r.nextFloat() * 0.5f;
                for (int k = 0; k < largo && i + k < n; k++) s[i + k] += (r.nextFloat() * 2 - 1) * fuerza * (1 - (float) k / largo);
            }
        }
        return s;
    }

    static float[] servo(float segundos, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double f = 140 + 60 * Math.sin(t * Math.PI);
            fase += f / FRECUENCIA;
            double zumbido = onda(Onda.SIERRA, fase) * 0.5 + onda(Onda.CUADRADA, fase * 2.01) * 0.2;
            s[i] = (float) (zumbido * 0.3 * Math.sin(Math.PI * t));
        }
        return s;
    }

    static float[] barrido(float segundos, float fuerza, Random r) {
        int n = (int) (segundos * FRECUENCIA);
        float[] s = new float[n];
        float y = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            float x = r.nextFloat() * 2 - 1;
            float k = (float) (0.05 + 0.5 * Math.sin(Math.PI * t));
            y += (x - y) * k;
            s[i] = (float) (y * fuerza * Math.sin(Math.PI * t));
        }
        return s;
    }

    static float[] arpegio(float[] notas, float cada, Onda o, float fuerza, Random r) {
        float[] s = new float[(int) ((notas.length * cada + 0.25f) * FRECUENCIA)];
        for (int k = 0; k < notas.length; k++) {
            float[] t = tono(cada * 2.5f, notas[k], notas[k], o, fuerza, r);
            int ini = (int) (k * cada * FRECUENCIA);
            for (int i = 0; i < t.length && ini + i < s.length; i++) s[ini + i] += t[i];
        }
        return s;
    }

    static float[] mezclar(float[] a, float[] b) {
        float[] s = new float[Math.max(a.length, b.length)];
        for (int i = 0; i < a.length; i++) s[i] += a[i];
        for (int i = 0; i < b.length; i++) s[i] += b[i];
        return s;
    }

    static float[] retrasar(float[] a, float segundos) {
        int d = (int) (segundos * FRECUENCIA);
        float[] s = new float[a.length + d];
        System.arraycopy(a, 0, s, d, a.length);
        return s;
    }

    static float[] eco(float[] a, float retraso, float fuerza) {
        int d = (int) (retraso * FRECUENCIA);
        float[] s = new float[a.length + d * 3];
        for (int i = 0; i < a.length; i++) {
            s[i] += a[i];
            s[i + d] += a[i] * fuerza;
            s[i + 2 * d] += a[i] * fuerza * fuerza;
        }
        return s;
    }

    /** Lo pasa a un WAV de 16 bits (mono), recortando lo que se pase de fuerte. */
    public static byte[] wav(float[] s) {
        ByteArrayOutputStream b = new ByteArrayOutputStream(44 + s.length * 2);
        int datos = s.length * 2;
        escribirTexto(b, "RIFF");
        escribirEntero(b, 36 + datos);
        escribirTexto(b, "WAVE");
        escribirTexto(b, "fmt ");
        escribirEntero(b, 16);
        escribirCorto(b, 1);
        escribirCorto(b, 1);
        escribirEntero(b, FRECUENCIA);
        escribirEntero(b, FRECUENCIA * 2);
        escribirCorto(b, 2);
        escribirCorto(b, 16);
        escribirTexto(b, "data");
        escribirEntero(b, datos);
        for (float x : s) {
            float v = (float) Math.tanh(x * 1.2);   // satura suave en vez de chasquear
            escribirCorto(b, Math.round(v * 32000));
        }
        return b.toByteArray();
    }

    private static void escribirTexto(ByteArrayOutputStream b, String s) {
        for (char c : s.toCharArray()) b.write(c);
    }

    private static void escribirEntero(ByteArrayOutputStream b, int v) {
        b.write(v);
        b.write(v >> 8);
        b.write(v >> 16);
        b.write(v >> 24);
    }

    private static void escribirCorto(ByteArrayOutputStream b, int v) {
        b.write(v);
        b.write(v >> 8);
    }
}
