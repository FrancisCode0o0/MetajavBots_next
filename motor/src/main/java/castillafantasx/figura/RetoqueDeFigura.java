package castillafantasx.figura;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * RETOQUE DE FIGURA — Lo que se le ha cambiado a mano a una figura en el
 * editor: a toda ella, a una parte del cuerpo (la cabeza, un brazo…) o a una
 * sola pieza. Cada cambio engorda o afina, alarga o acorta, mueve, gira o
 * pinta de otro material. Se guarda en res/figuras/retoques.txt (Retoques).
 *
 * Una parte se alarga y se gira desde su eje: el primer punto suyo que se
 * esculpe (el hombro de un brazo, el arranque de una cola).
 */
public final class RetoqueDeFigura implements Retoque {

    /** Lo que se le cambia a toda la figura, a una parte o a una pieza. */
    public static final class Cambio {
        /** Por cuánto se multiplica su grueso y su largo (1 = igual). */
        public double grosor = 1, largo = 1;
        /** Cuánto se mueve, en píxeles de figura (x a su derecha, y adelante, z arriba). */
        public P3 mover = P3.CERO;
        /** Cuánto gira desde su eje, en grados: 'gira' a su izquierda; 'alza' hacia arriba; 'ladea' hacia su derecha. */
        public double gira, alza, ladea;
        /** El nombre del material del que se pinta (null = el suyo). */
        public String material;
        /** No se pinta. */
        public boolean oculta;
        private Material pintura;

        public boolean vacio() {
            return grosor == 1 && largo == 1 && mover.largo() == 0 && gira == 0 && alza == 0 && ladea == 0 && material == null && !oculta;
        }

        public Cambio copia() {
            Cambio c = new Cambio();
            c.tomar(this);
            return c;
        }

        public void tomar(Cambio o) {
            grosor = o.grosor;
            largo = o.largo;
            mover = o.mover;
            gira = o.gira;
            alza = o.alza;
            ladea = o.ladea;
            material = o.material;
            oculta = o.oculta;
            pintura = null;
        }

        /** Dónde queda un punto, visto desde el eje de lo que se cambia. */
        P3 aplicar(P3 p, P3 eje) {
            P3 v = p.menos(eje);
            if (largo != 1) v = v.por(largo);
            if (alza != 0) v = v.inclinado(Math.toRadians(-alza));
            if (ladea != 0) v = v.ladeado(Math.toRadians(ladea));
            if (gira != 0) v = v.girado(Math.toRadians(gira));
            return eje.mas(v).mas(mover);
        }

        Material pintura() {
            if (material == null) return null;
            if (pintura == null) pintura = Materiales.de(material);
            return pintura;
        }

        /** Como se escribe en el archivo: «grosor 1.2 largo 1.1 mover 0 1 0 gira 10 material CARNE_VIVA». */
        public String aTexto() {
            StringBuilder s = new StringBuilder();
            if (grosor != 1) s.append(" grosor ").append(num(grosor));
            if (largo != 1) s.append(" largo ").append(num(largo));
            if (mover.largo() != 0) s.append(" mover ").append(num(mover.x)).append(' ').append(num(mover.y)).append(' ').append(num(mover.z));
            if (gira != 0) s.append(" gira ").append(num(gira));
            if (alza != 0) s.append(" alza ").append(num(alza));
            if (ladea != 0) s.append(" ladea ").append(num(ladea));
            if (material != null) s.append(" material ").append(material);
            if (oculta) s.append(" oculta");
            return s.toString().trim();
        }

        /** Lee lo que escribe aTexto(). Lo que no entiende, lo deja como estaba. */
        public static Cambio deTexto(String texto) {
            Cambio c = new Cambio();
            String[] t = texto.trim().split("\\s+");
            try {
                for (int i = 0; i < t.length; i++) {
                    switch (t[i]) {
                        case "grosor":   c.grosor = Double.parseDouble(t[++i]); break;
                        case "largo":    c.largo = Double.parseDouble(t[++i]); break;
                        case "mover":    c.mover = new P3(Double.parseDouble(t[i + 1]), Double.parseDouble(t[i + 2]), Double.parseDouble(t[i + 3])); i += 3; break;
                        case "gira":     c.gira = Double.parseDouble(t[++i]); break;
                        case "alza":     c.alza = Double.parseDouble(t[++i]); break;
                        case "ladea":    c.ladea = Double.parseDouble(t[++i]); break;
                        case "material": c.material = t[++i]; break;
                        case "oculta":   c.oculta = true; break;
                        default: break;
                    }
                }
            } catch (RuntimeException e) {
                // Un renglón a medias no tumba el juego: vale lo leído hasta ahí.
            }
            return c;
        }
    }

    static String num(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    /** Lo de toda la figura (su grueso, su talla, su material). */
    public final Cambio todo = new Cambio();
    /** Lo de cada parte del cuerpo (el número que la figura le da al Escultor) y lo de cada pieza (parte * 1000 + orden). */
    public final Map<Integer, Cambio> partes = new TreeMap<>(), piezas = new TreeMap<>();
    /** Los tintes: todo lo que era de un color (RRGGBB) pasa a ser de otro material. */
    public final Map<Integer, String> tintes = new LinkedHashMap<>();
    /** Solo las figuras con forma de persona: lo que se suma a su pose (Pose.sumar): «manoD» -> {x, y, z}, «inclina» -> {grados}. */
    public final Map<String, double[]> pose = new LinkedHashMap<>();

    private final Map<Integer, Material> tintesHechos = new TreeMap<>();
    // El eje de cada parte y el de la pieza que se está esculpiendo: su primer punto.
    private final P3[] ejeDeParte = new P3[64];
    private P3 ejeDePieza;
    private int piezaDelEje = -1;

    public boolean vacio() {
        return todo.vacio() && partes.isEmpty() && piezas.isEmpty() && tintes.isEmpty() && pose.isEmpty();
    }

    /** El cambio de esa parte; si no tenía, uno nuevo ya apuntado. */
    public Cambio deParte(int parte) {
        return partes.computeIfAbsent(parte, k -> new Cambio());
    }

    /** El cambio de esa pieza; si no tenía, uno nuevo ya apuntado. */
    public Cambio dePieza(int pieza) {
        return piezas.computeIfAbsent(pieza, k -> new Cambio());
    }

    /** Todo lo de ese color pasa a ser de ese material (null: vuelve a ser lo que era). */
    public void tenir(int rgb, String material) {
        if (material == null) tintes.remove(rgb & 0xFFFFFF);
        else tintes.put(rgb & 0xFFFFFF, material);
        tintesHechos.clear();
    }

    /** Quita los cambios que no cambian nada (para no guardarlos). */
    public void limpiar() {
        partes.values().removeIf(Cambio::vacio);
        piezas.values().removeIf(Cambio::vacio);
    }

    public RetoqueDeFigura copia() {
        RetoqueDeFigura r = new RetoqueDeFigura();
        r.tomar(this);
        return r;
    }

    public void tomar(RetoqueDeFigura o) {
        todo.tomar(o.todo);
        partes.clear();
        piezas.clear();
        tintes.clear();
        pose.clear();
        tintesHechos.clear();
        for (Map.Entry<Integer, Cambio> e : o.partes.entrySet()) partes.put(e.getKey(), e.getValue().copia());
        for (Map.Entry<Integer, Cambio> e : o.piezas.entrySet()) piezas.put(e.getKey(), e.getValue().copia());
        tintes.putAll(o.tintes);
        for (Map.Entry<String, double[]> e : o.pose.entrySet()) pose.put(e.getKey(), e.getValue().clone());
    }

    // ------------------------------------------------------------------
    // Lo que le pregunta el Escultor
    // ------------------------------------------------------------------

    @Override
    public void empezar() {
        java.util.Arrays.fill(ejeDeParte, null);
        piezaDelEje = -1;
    }

    @Override
    public P3 punto(P3 p, int parte, int pieza) {
        int cual = parte & 63;
        if (ejeDeParte[cual] == null) ejeDeParte[cual] = p;
        if (pieza != piezaDelEje) {
            piezaDelEje = pieza;
            ejeDePieza = p;
        }
        if (!piezas.isEmpty()) {
            Cambio c = piezas.get(pieza);
            if (c != null) p = c.aplicar(p, ejeDePieza);
        }
        if (!partes.isEmpty()) {
            Cambio c = partes.get(parte);
            if (c != null) p = c.aplicar(p, ejeDeParte[cual]);
        }
        // Toda la figura crece desde sus pies.
        if (todo.largo != 1 || todo.gira != 0 || todo.alza != 0 || todo.ladea != 0 || todo.mover != P3.CERO) p = todo.aplicar(p, P3.CERO);
        return p;
    }

    @Override
    public double grosor(P3 p, int parte, int pieza) {
        double g = todo.grosor * todo.largo;
        Cambio c = partes.get(parte);
        if (c != null) g *= c.grosor;
        c = piezas.get(pieza);
        if (c != null) g *= c.grosor;
        return g;
    }

    @Override
    public Material material(Material m, int parte, int pieza) {
        Cambio c = piezas.get(pieza);
        if (c != null) {
            if (c.oculta) return null;
            if (c.pintura() != null) return c.pintura();
        }
        c = partes.get(parte);
        if (c != null) {
            if (c.oculta) return null;
            if (c.pintura() != null) return c.pintura();
        }
        // Lo que da luz o se transparenta (una llama, una estela) no se tiñe.
        if (m.brilla) return m;
        if (!tintes.isEmpty()) {
            String nombre = tintes.get(m.rgb);
            if (nombre != null) {
                Material t = tintesHechos.get(m.rgb);
                if (t == null) {
                    t = Materiales.de(nombre);
                    if (t != null) tintesHechos.put(m.rgb, t);
                }
                if (t != null) return t;
            }
        }
        return todo.pintura() != null ? todo.pintura() : m;
    }

    // ------------------------------------------------------------------
    // El archivo
    // ------------------------------------------------------------------

    /** Los renglones de su sección del archivo. */
    public List<String> aLineas() {
        limpiar();
        List<String> lineas = new ArrayList<>();
        if (!todo.vacio()) lineas.add("todo = " + todo.aTexto());
        for (Map.Entry<Integer, Cambio> e : partes.entrySet()) lineas.add("parte " + e.getKey() + " = " + e.getValue().aTexto());
        for (Map.Entry<Integer, Cambio> e : piezas.entrySet()) lineas.add("pieza " + e.getKey() + " = " + e.getValue().aTexto());
        for (Map.Entry<Integer, String> e : tintes.entrySet()) lineas.add("tinte " + String.format(Locale.ROOT, "%06X", e.getKey()) + " = " + e.getValue());
        for (Map.Entry<String, double[]> e : pose.entrySet()) {
            StringBuilder s = new StringBuilder("pose " + e.getKey() + " =");
            for (double v : e.getValue()) s.append(' ').append(num(v));
            lineas.add(s.toString());
        }
        return lineas;
    }

    /** Lee un renglón de su sección. */
    public void leerLinea(String linea) {
        int igual = linea.indexOf('=');
        if (igual < 0) return;
        String clave = linea.substring(0, igual).trim(), valor = linea.substring(igual + 1).trim();
        try {
            if (clave.equals("todo")) todo.tomar(Cambio.deTexto(valor));
            else if (clave.startsWith("parte ")) partes.put(Integer.parseInt(clave.substring(6).trim()), Cambio.deTexto(valor));
            else if (clave.startsWith("pieza ")) piezas.put(Integer.parseInt(clave.substring(6).trim()), Cambio.deTexto(valor));
            else if (clave.startsWith("tinte ")) tenir(Integer.parseInt(clave.substring(6).trim(), 16), valor);
            else if (clave.startsWith("pose ")) {
                String[] t = valor.split("\\s+");
                double[] v = new double[t.length];
                for (int i = 0; i < t.length; i++) v[i] = Double.parseDouble(t[i]);
                pose.put(clave.substring(5).trim(), v);
            }
        } catch (RuntimeException e) {
            System.err.println("Retoque de figura: no se entiende «" + linea + "»");
        }
    }
}
