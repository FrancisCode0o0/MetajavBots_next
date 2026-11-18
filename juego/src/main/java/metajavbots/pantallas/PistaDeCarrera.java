package metajavbots.pantallas;

import java.util.HashSet;
import java.util.Set;

import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.textos.T;

/**
 * PISTA DE CARRERA — Encima de la acción del robotito, una carrera: su
 * código corre por una calle y el de la solución (el fantasma) por la otra.
 * La pista sale de la traza, línea a línea: cada vuelta de un bucle (o cada
 * llamada de una recursión) es una valla que saltar, con su número; cada
 * condición es un cartel (verde si entra, rojo si se la salta) y, si se
 * salta código, el robot toma el desvío de un salto. Cada línea que se
 * ejecuta es un paso: quien ejecuta menos líneas llega antes. Si su código
 * acierta y llega antes o a la vez, gana la carrera; si acierta pero el
 * fantasma llega antes, se le dice por cuántos pasos; si falla, al cruzar la
 * meta lo descalifican (con banderita y todo).
 */
public final class PistaDeCarrera {

    private PistaDeCarrera() {
    }

    /** Lo que pasa en cada paso de la traza. */
    public enum Suceso { PASO, VALLA, DESVIO }

    /** Una carrera ya leída de la traza: los sucesos de cada paso, las vallas y los carteles (si es una condición). */
    public static final class Recorrido {
        /** Lo que pasa en cada paso. */
        public final Suceso[] sucesos;
        /** En cada paso, si su línea es una condición (if, while, for, switch, ?) y si entró (true) o se saltó lo de dentro. */
        public final Boolean[] carteles;
        /** El número de vuelta de cada valla (desde 1, por línea). */
        final int[] vuelta;
        /** Cuántas vallas (vueltas de bucle o llamadas recursivas) salta. */
        public final int vallas;

        Recorrido(Suceso[] sucesos, Boolean[] carteles, int[] vuelta, int vallas) {
            this.sucesos = sucesos;
            this.carteles = carteles;
            this.vuelta = vuelta;
            this.vallas = vallas;
        }

        /** Los pasos (líneas ejecutadas). */
        public int pasos() {
            return sucesos.length;
        }
    }

    /** Una línea con código de verdad (no vacía, ni solo llaves, ni un else, ni un comentario). */
    static boolean conCodigo(String l) {
        String s = l.strip();
        if (s.isEmpty() || s.startsWith("//") || s.startsWith("*") || s.startsWith("/*")) return false;
        String sin = s.replace("}", "").replace("{", "").replace("else", "").strip();
        return !sin.isEmpty();
    }

    /** ¿Es una condición (algo que decide por dónde sigue)? */
    static boolean esCondicion(String l) {
        String s = " " + l.strip() + " ";
        return s.matches(".*[^A-Za-z_](if|while|for|switch|case)[^A-Za-z_0-9].*") || s.contains(" ? ");
    }

    /**
     * Lee la traza (las líneas que se ejecutan, en orden) con el código: una valla en cada vuelta (cada vez que se entra
     * en una línea a la que el código vuelve hacia atrás: el cuerpo de un bucle o la primera de un método recursivo) y un
     * desvío donde se salta código hacia delante (una rama que no se toma).
     */
    public static Recorrido de(int[] traza, String codigo) {
        String[] lineas = codigo.split("\n", -1);
        int n = traza.length;
        Set<Integer> vueltas = new HashSet<>();
        for (int k = 1; k < n; k++) if (traza[k] <= traza[k - 1]) vueltas.add(traza[k]);
        Suceso[] s = new Suceso[n];
        Boolean[] carteles = new Boolean[n];
        int[] numero = new int[n];
        java.util.Map<Integer, Integer> cuenta = new java.util.HashMap<>();
        int vallas = 0;
        for (int k = 0; k < n; k++) {
            int l = traza[k];
            s[k] = Suceso.PASO;
            if (vueltas.contains(l)) {
                s[k] = Suceso.VALLA;
                numero[k] = cuenta.merge(l, 1, Integer::sum);
                vallas++;
            } else if (k > 0 && l > traza[k - 1] + 1 && saltaCodigo(lineas, traza[k - 1], l)) {
                s[k] = Suceso.DESVIO;
            }
            String texto = l >= 1 && l <= lineas.length ? lineas[l - 1] : "";
            if (esCondicion(texto)) {
                // Entra si la siguiente línea ejecutada es la de justo debajo (con código); si salta más allá, no.
                boolean entra = k + 1 < n && traza[k + 1] > l && !saltaCodigo(lineas, l, traza[k + 1]);
                if (k + 1 < n && traza[k + 1] <= l) entra = true;
                carteles[k] = entra;
            }
        }
        return new Recorrido(s, carteles, numero, vallas);
    }

    /** ¿Hay código entre la línea «desde» y la línea «hasta» (sin contarlas)? */
    static boolean saltaCodigo(String[] lineas, int desde, int hasta) {
        for (int l = desde + 1; l < hasta && l <= lineas.length; l++) if (l >= 1 && conCodigo(lineas[l - 1])) return true;
        return false;
    }

    // ================================================================ el dibujo

    private static final int CESPED = 0x2E5E3A, TIERRA = 0xB5653A, BLANCO = 0xF2F2F2, FANTASMA = 0xB7C4D6;

    /**
     * La carrera en (x, y, w, h): arriba la calle del fantasma (si lo hay), abajo la suya. «f» de 0 a 1: los dos avanzan un
     * paso a la vez; la pista mide lo que el más largo de los dos.
     */
    static void dibujar(Ui ui, float x, float y, float w, float h, Recorrido suyo, Recorrido fantasma, float f, boolean bien) {
        Pincel p = ui.p;
        p.rect(x, y, w, h, CESPED, 1);
        int largo = Math.max(1, Math.max(suyo.pasos(), fantasma == null ? 0 : fantasma.pasos()));
        float x0 = x + 70, x1 = x + w - 90;
        float calle = fantasma == null ? h - 16 : (h - 16) / 2;
        float yFantasma = y + 8, ySuyo = fantasma == null ? y + 8 : y + 8 + calle;
        float pasoActual = f * largo;
        if (fantasma != null) calle(ui, x, x0, x1, yFantasma, calle, fantasma, largo, pasoActual, true, true);
        calle(ui, x, x0, x1, ySuyo, calle, suyo, largo, pasoActual, false, bien);
        // La meta, a cuadros, al final de la pista más larga.
        for (int i = 0; i < (int) ((h - 16) / 8); i++) {
            p.rect(x1 + 26, y + 8 + i * 8, 6, 8, i % 2 == 0 ? BLANCO : 0x111111, 1);
            p.rect(x1 + 32, y + 8 + i * 8, 6, 8, i % 2 == 0 ? 0x111111 : BLANCO, 1);
        }
        resultado(ui, x, y, w, h, suyo, fantasma, f, bien, x1);
    }

    /** Una calle: tierra, rayas, las vallas, los carteles y el robot que corre (el fantasma, translúcido). */
    private static void calle(Ui ui, float x, float x0, float x1, float cy, float alto, Recorrido r, int largo, float pasoActual, boolean esFantasma,
            boolean bien) {
        Pincel p = ui.p;
        p.rect(x + 4, cy, x1 - x + 40, alto - 2, TIERRA, esFantasma ? 0.55f : 1);
        p.rect(x + 4, cy + alto - 4, x1 - x + 40, 2, BLANCO, 0.6f);
        String nombre = (esFantasma ? T.t("carrera.fantasma") : T.t("carrera.tu")) + " · " + T.f("carrera.cuenta", r.pasos(), r.vallas);
        p.texto(nombre, x + 8, cy + 2, ui.letra * 0.62f, true, esFantasma ? FANTASMA : BLANCO, 1);
        float suelo = cy + alto - 6;
        float paso = (x1 - x0) / largo;
        int hecho = Math.min(r.pasos(), (int) pasoActual);
        for (int k = 0; k < r.pasos(); k++) {
            float px = x0 + (k + 0.5f) * paso;
            if (r.sucesos[k] == Suceso.VALLA) valla(p, px, suelo, Math.min(alto * 0.4f, 22), k < hecho, r.vuelta[k], ui);
            if (r.carteles[k] != null && paso > 3) cartel(p, px - paso * 0.35f, suelo, Math.min(alto * 0.55f, 26), k < hecho ? r.carteles[k] : null);
            if (r.sucesos[k] == Suceso.DESVIO) p.triangulo(px - 6, suelo, px + 6, suelo, px + 6, suelo - 7, 0x7A4A2A, 1);
        }
        // Dónde va: entre dos pasos, avanza; en una valla o un desvío, salta.
        float pos = Math.min(r.pasos(), pasoActual);
        int k = Math.min(r.pasos() - 1, (int) pos);
        float u = pos - (int) pos;
        float rx = x0 + pos * paso;
        float salto = 0;
        if (k >= 0 && pos < r.pasos() && (r.sucesos[k] == Suceso.VALLA || r.sucesos[k] == Suceso.DESVIO)) salto = (float) Math.sin(u * Math.PI) * alto * 0.5f;
        boolean llego = pasoActual >= r.pasos();
        if (llego) rx = x0 + r.pasos() * paso + Math.min(1, (pasoActual - r.pasos()) / 3) * (x1 + 50 - x0 - r.pasos() * paso);
        corredor(p, rx, suelo - salto, Math.min(1.2f, alto / 44), pasoActual, esFantasma, llego && !esFantasma && !bien);
    }

    private static void valla(Pincel p, float px, float suelo, float alto, boolean saltada, int numero, Ui ui) {
        p.rect(px - 7, suelo - alto, 2, alto, BLANCO, 1);
        p.rect(px + 5, suelo - alto, 2, alto, BLANCO, 1);
        p.rect(px - 8, suelo - alto, 16, 4, saltada ? 0xFFD23F : 0xE04848, 1);
        if (alto >= 14) p.textoCentrado(Integer.toString(numero), px, suelo - alto - ui.letra * 0.62f - 1, ui.letra * 0.55f, true, BLANCO, 0.9f);
    }

    /** Un cartel de condición: gris si aún no ha llegado; verde si entra; rojo si se salta lo de dentro. */
    private static void cartel(Pincel p, float px, float suelo, float alto, Boolean entra) {
        int color = entra == null ? 0x8A919C : entra ? AnimacionDelCaso.VERDE : AnimacionDelCaso.ROJO;
        p.rect(px - 1, suelo - alto, 2, alto, 0x6B4F33, 1);
        p.poligono(new float[] {px, suelo - alto - 7, px + 7, suelo - alto, px, suelo - alto + 7, px - 7, suelo - alto}, color, 1);
    }

    /** El corredor: un robotito de perfil que mueve las piernas (el fantasma, translúcido y con sábana). */
    private static void corredor(Pincel p, float x, float pie, float k, float t, boolean fantasma, boolean descalificado) {
        float a = fantasma ? 0.55f : 1;
        int cuerpo = fantasma ? FANTASMA : descalificado ? 0x6E8FAF : 0x3FA9F5;
        float zancada = (float) Math.sin(t * 9) * 6 * k;
        p.linea(x, pie - 12 * k, x + zancada, pie, 3 * k, 0x2A2F38, a);
        p.linea(x, pie - 12 * k, x - zancada, pie, 3 * k, 0x2A2F38, a);
        p.caja(x - 8 * k, pie - 30 * k, 16 * k, 18 * k, 4 * k, cuerpo, a);
        p.caja(x - 6 * k, pie - 42 * k, 12 * k, 11 * k, 3 * k, Pincel.aclarar(cuerpo, 0.15f), a);
        p.circulo(x + 3 * k, pie - 37 * k, 1.6f * k, descalificado ? AnimacionDelCaso.ROJO : 0x111318, a);
        if (fantasma) {
            // La sábana con ojos.
            p.triangulo(x - 9 * k, pie - 12 * k, x - 3 * k, pie - 4 * k, x + 2 * k, pie - 12 * k, FANTASMA, a);
        }
    }

    /** El cartel del final: gana, gana el fantasma (por cuántos pasos) o descalificado; y las vueltas de cada uno. */
    private static void resultado(Ui ui, float x, float y, float w, float h, Recorrido suyo, Recorrido fantasma, float f, boolean bien, float x1) {
        Pincel p = ui.p;
        if (f < 1) return;
        String s;
        int color;
        if (!bien) {
            s = T.t("carrera.descalificado");
            color = AnimacionDelCaso.ROJO;
        } else if (fantasma == null || suyo.pasos() <= fantasma.pasos()) {
            s = T.t("carrera.ganas");
            color = 0xFFD23F;
        } else {
            s = T.f("carrera.pierdes", suyo.pasos() - fantasma.pasos());
            color = 0xFFB86B;
        }
        float tw = p.ancho(s, ui.letra * 0.85f, true) + 20;
        p.caja(x + (w - tw) / 2, y + h / 2 - ui.letra * 0.7f, tw, ui.letra * 1.4f, 8, 0x111318, 0.85f);
        p.textoCentrado(s, x + w / 2, y + h / 2 - ui.letra * 0.55f, ui.letra * 0.85f, true, color, 1);
    }
}
