package castillafantasx.maniqui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import castillafantasx.datos.LectorSecciones;

/**
 * MANIQUÍ — Un cuerpo (Cuerpo) que se pone en cualquier postura moviendo sus
 * articulaciones, cada una dentro de lo que da de sí una de verdad
 * (res/datos/articulaciones.txt). Todos los ángulos a cero es la postura
 * anatómica: de pie, con los brazos caídos. Tras cambiar ángulos se llama a
 * posar() y quedan al día los puntos de la piel, los ojos y las manos.
 * Herramienta de desarrollo (F1): encargos/ENCARGO_BRAZOS.md.
 */
public final class Maniqui {

    /** Un movimiento de una articulación (la flexión del codo derecho, el giro del cuello…), con sus topes. */
    public static final class Giro {
        public final String nombre;
        public final double minimo, maximo;
        private final int[] huesos;
        private final Vector3 eje;
        private double grados;

        Giro(String nombre, int[] huesos, Vector3 eje, double minimo, double maximo) {
            this.nombre = nombre;
            this.huesos = huesos;
            this.eje = eje;
            this.minimo = minimo;
            this.maximo = maximo;
        }

        public double grados() {
            return grados;
        }

        /** Pone el ángulo, sin pasar de los topes de la articulación. */
        public void poner(double cuanto) {
            grados = Math.max(minimo, Math.min(maximo, cuanto));
        }
    }

    /** Cómo queda una mano para sujetar algo: el centro del puño y hacia dónde van el mango, los nudillos y la palma. */
    public static final class Agarre {
        public final Vector3 centro = new Vector3(), mango = new Vector3(), nudillos = new Vector3(), palma = new Vector3();
    }

    public static final int DERECHA = 0, IZQUIERDA = 1;
    private static final String[] LADO = { "_r", "_l" }, LADO_EN_EL_NOMBRE = { "_d", "_i" };
    private static final String[] DEDOS = { "index", "middle", "ring", "pinky" };
    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/articulaciones.txt");
    // Dónde cae el centro del puño: de la muñeca a la base del dedo corazón, y separado de los huesos hacia la palma (metros).
    private static final float PUNIO_A_LO_LARGO = 0.75f, PUNIO_HACIA_LA_PALMA = 0.035f;
    private static final float MANGO_EN_DIAGONAL = 0.45f;      // el mango cruza la palma en diagonal, del talón a la base del índice
    private static final float OJOS_BAJO_LA_CORONILLA = 0.11f; // de la coronilla a los ojos, en metros

    public final Cuerpo cuerpo;
    public final List<Giro> giros = new ArrayList<>();
    /** Cuánto se cierra cada mano (DERECHA e IZQUIERDA): 0 abierta, 1 puño. */
    public final double[] cierre = new double[2];
    /** Los puntos de la piel en la postura puesta (x, y, z seguidos, como Cuerpo.puntos). */
    public final float[] puntos;

    private final int[] orden;                      // los huesos, cada padre antes que sus hijos
    private final Quaternion[] neutro, puesto, mundo;
    private final Vector3[] cabezaNeutra, cabezaPuesta;
    private final Matrix4[] matriz;
    private final Agarre[] agarreNeutro = { new Agarre(), new Agarre() }, agarre = { new Agarre(), new Agarre() };
    private final Vector3[] dedosNeutros = { new Vector3(), new Vector3() }, anchoNeutro = { new Vector3(), new Vector3() };
    private final Vector3 ojosEnReposo = new Vector3(), ojos = new Vector3();
    private final double[] cierreDeDedo = new double[3], cierreDePulgar = new double[3];
    private final int cabeza;

    public Maniqui(Cuerpo cuerpo) {
        this.cuerpo = cuerpo;
        int n = cuerpo.huesos.length;
        puntos = new float[cuerpo.puntos.length];
        neutro = new Quaternion[n];
        puesto = new Quaternion[n];
        mundo = new Quaternion[n];
        cabezaNeutra = new Vector3[n];
        cabezaPuesta = new Vector3[n];
        matriz = new Matrix4[n];
        orden = ordenar();
        for (int i = 0; i < n; i++) {
            puesto[i] = new Quaternion();
            mundo[i] = new Quaternion();
            cabezaPuesta[i] = new Vector3();
            matriz[i] = new Matrix4();
        }
        cabeza = cuerpo.hueso("head");
        ponerNeutro();
        medirLasManos();
        leerGiros();
        leerDedos();
        buscarLosOjos();
        posar();
    }

    // ------------------------------------------------------------------
    // 1. La postura anatómica y las medidas que salen de ella
    // ------------------------------------------------------------------

    private int[] ordenar() {
        int n = cuerpo.huesos.length, puestos = 0;
        int[] o = new int[n];
        boolean[] ya = new boolean[n];
        while (puestos < n) {
            for (int i = 0; i < n; i++) {
                if (!ya[i] && (cuerpo.padre[i] < 0 || ya[cuerpo.padre[i]])) {
                    ya[i] = true;
                    o[puestos++] = i;
                }
            }
        }
        return o;
    }

    /** El giro que lleva cada hueso de la postura del archivo (brazos en aspa) a la anatómica, según [neutro]. */
    private void ponerNeutro() {
        Map<String, String> reglas = new HashMap<>();
        for (String linea : ARCHIVO.lineas("neutro")) {
            String[] par = linea.split("=");
            if (par.length == 2) reglas.put(par[0].trim(), par[1].trim());
        }
        for (int h : orden) {
            int p = cuerpo.padre[h];
            Quaternion delPadre = p < 0 ? new Quaternion() : neutro[p];
            String regla = reglas.get(cuerpo.huesos[h].replaceAll("_[rl]$", ""));
            Quaternion propio = new Quaternion();
            if (regla != null) {
                Vector3 ahora = delPadre.transform(new Vector3(cuerpo.ejeY[h]));
                Vector3 debe = regla.equals("abajo") ? new Vector3(0, -1, 0) : neutro[p].transform(new Vector3(cuerpo.ejeY[p]));
                propio.setFromCross(ahora, debe);
            }
            neutro[h] = propio.mul(delPadre);
            cabezaNeutra[h] = p < 0 ? new Vector3(cuerpo.cabeza[h])
                    : neutro[p].transform(new Vector3(cuerpo.cabeza[h]).sub(cuerpo.cabeza[p])).add(cabezaNeutra[p]);
        }
    }

    /** De cada mano, en la postura anatómica: hacia dónde van los dedos, el ancho de la palma y la palma, y dónde cae el puño. */
    private void medirLasManos() {
        for (int lado = 0; lado < 2; lado++) {
            int mano = cuerpo.hueso("hand" + LADO[lado]), corazon = cuerpo.hueso("middle_01" + LADO[lado]);
            if (mano < 0 || corazon < 0) continue;
            Vector3 dedos = new Vector3(cabezaNeutra[corazon]).sub(cabezaNeutra[mano]);
            float largo = dedos.len();
            dedos.nor();
            Vector3 ancho = new Vector3(cabezaNeutra[cuerpo.hueso("index_01" + LADO[lado])]).sub(cabezaNeutra[cuerpo.hueso("pinky_01" + LADO[lado])]);
            ancho.mulAdd(dedos, -ancho.dot(dedos)).nor();
            // La palma queda del lado hacia el que se cierran los dedos.
            Vector3 normal = new Vector3(dedos).crs(ancho).nor();
            Vector3 alCerrar = new Quaternion(ejeDeCierre(corazon), 10).transform(new Vector3(dedos)).sub(dedos);
            if (alCerrar.dot(normal) < 0) normal.scl(-1);
            dedosNeutros[lado].set(dedos);
            anchoNeutro[lado].set(ancho);
            Agarre a = agarreNeutro[lado];
            a.palma.set(normal);
            a.centro.set(cabezaNeutra[mano]).mulAdd(dedos, largo * PUNIO_A_LO_LARGO).mulAdd(normal, PUNIO_HACIA_LA_PALMA);
            a.mango.set(ancho).mulAdd(dedos, MANGO_EN_DIAGONAL).nor();
            a.nudillos.set(dedos).mulAdd(a.mango, -dedos.dot(a.mango)).nor();
        }
    }

    /** El eje sobre el que se dobla una falange, en la postura anatómica. */
    private Vector3 ejeDeCierre(int hueso) {
        return neutro[hueso].transform(new Vector3(cuerpo.ejeX[hueso]));
    }

    private void buscarLosOjos() {
        float alto = cuerpo.altura() - OJOS_BAJO_LA_CORONILLA, cara = 0;
        float[] p = cuerpo.puntos;
        for (int i = 0; i < p.length; i += 3) {
            if (Math.abs(p[i]) > 0.02f && Math.abs(p[i]) < 0.045f && Math.abs(p[i + 1] - alto) < 0.01f) cara = Math.max(cara, p[i + 2]);
        }
        ojosEnReposo.set(0, alto, cara);
    }

    // ------------------------------------------------------------------
    // 2. Los movimientos de cada articulación (res/datos/articulaciones.txt)
    // ------------------------------------------------------------------

    private void leerGiros() {
        for (String linea : ARCHIVO.lineas("movimientos")) {
            int igual = linea.indexOf('=');
            if (igual < 0) continue;
            String nombre = linea.substring(0, igual).trim();
            String[] dato = linea.substring(igual + 1).split(",");
            if (dato.length != 5) throw new IllegalStateException("articulaciones.txt: «" + nombre + "» no tiene sus cinco datos");
            String[] bases = dato[0].trim().split("\\+");
            if (cuerpo.hueso(bases[0]) >= 0) {
                anadirGiro(nombre, bases, "", -1, dato);
            } else {
                int punto = nombre.indexOf('.');
                for (int lado = 0; lado < 2; lado++) {
                    anadirGiro(nombre.substring(0, punto) + LADO_EN_EL_NOMBRE[lado] + nombre.substring(punto), bases, LADO[lado], lado, dato);
                }
            }
        }
    }

    private void anadirGiro(String nombre, String[] bases, String sufijo, int lado, String[] dato) {
        int[] huesos = new int[bases.length];
        for (int i = 0; i < bases.length; i++) {
            huesos[i] = cuerpo.hueso(bases[i] + sufijo);
            if (huesos[i] < 0) return;          // este cuerpo no tiene ese hueso: se queda sin ese movimiento
        }
        String[] sentido = dato[4].trim().split(">");
        Vector3 eje = direccion(dato[1].trim(), huesos[0], lado);
        Vector3 prueba = direccion(sentido[0], huesos[0], lado), hacia = direccion(sentido[1], huesos[0], lado);
        // El sentido: con un ángulo positivo, lo que apunta a «prueba» tiene que irse hacia «hacia».
        Vector3 movido = new Quaternion(eje, 10).transform(new Vector3(prueba)).sub(prueba);
        if (movido.dot(hacia) < 0) eje.scl(-1);
        giros.add(new Giro(nombre, huesos, eje, Double.parseDouble(dato[2].trim()), Double.parseDouble(dato[3].trim())));
    }

    /** Una dirección con nombre, en la postura anatómica. 'lado' es DERECHA, IZQUIERDA o -1 si el hueso es del centro. */
    private Vector3 direccion(String nombre, int hueso, int lado) {
        float fuera = lado == IZQUIERDA ? 1 : -1;       // su derecha queda hacia -x
        switch (nombre) {
            case "lateral":  return new Vector3(1, 0, 0);
            case "sagital":  return new Vector3(0, 0, 1);
            case "vertical": return new Vector3(0, 1, 0);
            case "largo":    return neutro[hueso].transform(new Vector3(cuerpo.ejeY[hueso]));
            case "arriba":   return new Vector3(0, 1, 0);
            case "abajo":    return new Vector3(0, -1, 0);
            case "adelante": return new Vector3(0, 0, 1);
            case "atras":    return new Vector3(0, 0, -1);
            case "derecha":  return new Vector3(-1, 0, 0);
            case "afuera":   return new Vector3(fuera, 0, 0);
            case "adentro":  return new Vector3(-fuera, 0, 0);
            case "dedos":    return new Vector3(dedosNeutros[lado]);
            case "palma": case "normal_de_la_palma": return new Vector3(agarreNeutro[lado].palma);
            case "ancho_de_la_palma": return new Vector3(anchoNeutro[lado]);
            case "menique":  return new Vector3(anchoNeutro[lado]).scl(-1);
            default: throw new IllegalStateException("articulaciones.txt: no conozco la dirección «" + nombre + "»");
        }
    }

    private void leerDedos() {
        String[] dedo = ARCHIVO.valor("dedos", "dedo", "90, 100, 80").split(","), pulgar = ARCHIVO.valor("dedos", "pulgar", "30, 40, 60").split(",");
        for (int i = 0; i < 3; i++) {
            cierreDeDedo[i] = Double.parseDouble(dedo[i].trim());
            cierreDePulgar[i] = Double.parseDouble(pulgar[i].trim());
        }
    }

    /** El movimiento con ese nombre (por ejemplo «codo_d.flexion»), o null. */
    public Giro giro(String nombre) {
        for (Giro g : giros) {
            if (g.nombre.equals(nombre)) return g;
        }
        return null;
    }

    /** Vuelve a la postura anatómica, con las manos abiertas. */
    public void soltar() {
        for (Giro g : giros) {
            g.poner(0);
        }
        cierre[DERECHA] = 0;
        cierre[IZQUIERDA] = 0;
        posar();
    }

    // ------------------------------------------------------------------
    // 3. Poner la postura: de los ángulos a los huesos y a la piel
    // ------------------------------------------------------------------

    /** Pone al día huesos, piel, ojos y manos con los ángulos de ahora. */
    public void posar() {
        posarHuesos();
        vestirLaPiel();
    }

    /** Lo mismo sin la piel (mucho más rápido): huesos, ojos y manos. Para tantear posturas (Alcance). */
    public void posarHuesos() {
        int n = cuerpo.huesos.length;
        Quaternion[] propio = new Quaternion[n];
        for (int i = 0; i < n; i++) {
            propio[i] = new Quaternion();
        }
        // Lo que gira cada hueso por sus articulaciones: el primer movimiento del archivo manda sobre los siguientes.
        for (Giro g : giros) {
            for (int h : g.huesos) {
                propio[h].mul(new Quaternion(g.eje, (float) (g.grados / g.huesos.length)));
            }
        }
        // Los dedos: cada falange se dobla lo que le toca del cierre de su mano.
        for (int lado = 0; lado < 2; lado++) {
            for (int falange = 0; falange < 3; falange++) {
                for (String dedo : DEDOS) {
                    doblar(propio, dedo + "_0" + (falange + 1) + LADO[lado], cierre[lado] * cierreDeDedo[falange]);
                }
                doblar(propio, "thumb_0" + (falange + 1) + LADO[lado], cierre[lado] * cierreDePulgar[falange]);
            }
        }
        // Cada hueso lleva el giro de su padre y el suyo; y empieza donde lo deja su padre.
        for (int h : orden) {
            int p = cuerpo.padre[h];
            if (p < 0) {
                puesto[h].set(propio[h]);
                cabezaPuesta[h].set(cuerpo.cabeza[h]);
            } else {
                puesto[h].set(puesto[p]).mul(propio[h]);
                mundo[p].transform(cabezaPuesta[h].set(cuerpo.cabeza[h]).sub(cuerpo.cabeza[p])).add(cabezaPuesta[p]);
            }
            mundo[h].set(puesto[h]).mul(neutro[h]);
            Vector3 c = cuerpo.cabeza[h];
            matriz[h].idt().translate(cabezaPuesta[h]).rotate(mundo[h]).translate(-c.x, -c.y, -c.z);
        }
        if (cabeza >= 0) ojos.set(ojosEnReposo).mul(matriz[cabeza]);
        for (int lado = 0; lado < 2; lado++) {
            int mano = cuerpo.hueso("hand" + LADO[lado]);
            if (mano < 0) continue;
            Agarre de = agarreNeutro[lado], a = agarre[lado];
            puesto[mano].transform(a.centro.set(de.centro).sub(cabezaNeutra[mano])).add(cabezaPuesta[mano]);
            puesto[mano].transform(a.mango.set(de.mango));
            puesto[mano].transform(a.nudillos.set(de.nudillos));
            puesto[mano].transform(a.palma.set(de.palma));
        }
    }

    private void doblar(Quaternion[] propio, String nombre, double grados) {
        int h = cuerpo.hueso(nombre);
        if (h >= 0 && grados != 0) propio[h].mul(new Quaternion(ejeDeCierre(h), (float) grados));
    }

    /** Cada punto de la piel va con sus huesos: la media, según sus pesos, de adónde lo lleva cada uno. */
    private void vestirLaPiel() {
        float[] de = cuerpo.puntos, pesos = cuerpo.pesos;
        int[] huesos = cuerpo.huesosDelPunto;
        Vector3 v = new Vector3();
        for (int i = 0; i < de.length / 3; i++) {
            float x = 0, y = 0, z = 0;
            for (int k = 0; k < 4; k++) {
                float peso = pesos[i * 4 + k];
                if (peso == 0) continue;
                v.set(de[i * 3], de[i * 3 + 1], de[i * 3 + 2]).mul(matriz[huesos[i * 4 + k]]);
                x += v.x * peso;
                y += v.y * peso;
                z += v.z * peso;
            }
            puntos[i * 3] = x;
            puntos[i * 3 + 1] = y;
            puntos[i * 3 + 2] = z;
        }
    }

    // ------------------------------------------------------------------
    // 4. Lo que se pregunta al maniquí ya puesto
    // ------------------------------------------------------------------

    /** Dónde tiene los ojos (entre los dos, en la cara). */
    public Vector3 ojos() {
        return ojos;
    }

    /** Hacia dónde apunta, en la postura puesta, una dirección de su cabeza en reposo (el frente es 0, 0, 1). */
    public Vector3 desdeLaCabeza(float x, float y, float z) {
        return mundo[cabeza].transform(new Vector3(x, y, z));
    }

    /** Dónde empieza ahora ese hueso (la muñeca es «hand_r»; el codo, «lowerarm_r»…), o null si no lo tiene. */
    public Vector3 punto(String hueso) {
        int h = cuerpo.hueso(hueso);
        return h < 0 ? null : cabezaPuesta[h];
    }

    /** Cómo tiene la mano de ese lado para sujetar algo. */
    public Agarre agarre(int lado) {
        return agarre[lado];
    }

    /** ¿Mueven ese punto de la piel, sobre todo, la cabeza o el cuello? (Desde sus ojos no se pintan.) */
    public boolean esDeLaCabeza(int punto) {
        int mayor = 0;
        for (int k = 1; k < 4; k++) {
            if (cuerpo.pesos[punto * 4 + k] > cuerpo.pesos[punto * 4 + mayor]) mayor = k;
        }
        String hueso = cuerpo.huesos[cuerpo.huesosDelPunto[punto * 4 + mayor]];
        return hueso.equals("head") || hueso.equals("neck_01");
    }
}
