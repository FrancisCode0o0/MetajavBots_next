package castillafantasx.maniqui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * CUERPO — Un cuerpo humano con su esqueleto, leído de res/maniquies/<nombre>.glb
 * (lo fabrica herramientas/fabricar_maniqui.py con MakeHuman): los huesos, con su
 * padre y dónde empieza cada uno, y la malla de la piel, con qué huesos mueven
 * cada punto. Las medidas van en metros: x hacia su izquierda, y arriba, z al frente.
 * Se lee una vez y se reutiliza (Cuerpo.de).
 */
public final class Cuerpo {

    public final String nombre;
    /** Los huesos: nombre, padre (-1 si no tiene), dónde empieza en reposo y sus tres ejes en reposo. */
    public final String[] huesos;
    public final int[] padre;
    public final Vector3[] cabeza;
    public final Vector3[] ejeX, ejeY;
    /** La malla: los puntos (x, y, z seguidos), los cuatro huesos que mueven cada punto y cuánto, y los triángulos. */
    public final float[] puntos;
    public final int[] huesosDelPunto;
    public final float[] pesos;
    public final int[] triangulos;
    private final Map<String, Integer> porNombre = new HashMap<>();

    private static final Map<String, Cuerpo> LEIDOS = new HashMap<>();

    /** El cuerpo con ese nombre (hombre, mujer…): se lee la primera vez y luego se reutiliza. */
    public static synchronized Cuerpo de(String nombre) {
        Cuerpo c = LEIDOS.get(nombre);
        if (c == null) {
            c = new Cuerpo(nombre);
            LEIDOS.put(nombre, c);
        }
        return c;
    }

    /** ¿Está fabricado ese cuerpo? (res/maniquies/<nombre>.glb) */
    public static boolean existe(String nombre) {
        return Cuerpo.class.getResource("/maniquies/" + nombre + ".glb") != null;
    }

    private Cuerpo(String nombre) {
        this.nombre = nombre;
        byte[] archivo = leer("/maniquies/" + nombre + ".glb");
        // Un .glb son dos trozos: un texto JSON que dice qué hay y los números en crudo.
        ByteBuffer b = ByteBuffer.wrap(archivo).order(ByteOrder.LITTLE_ENDIAN);
        int largoDelJson = b.getInt(12);
        JsonValue json = new JsonReader().parse(new String(archivo, 20, largoDelJson, StandardCharsets.UTF_8));
        int datos = 20 + largoDelJson + 8;
        JsonValue nodos = json.get("nodes"), piel = json.get("skins").get(0);
        int[] nodoDelHueso = piel.get("joints").asIntArray();
        int n = nodoDelHueso.length;
        huesos = new String[n];
        padre = new int[n];
        cabeza = new Vector3[n];
        ejeX = new Vector3[n];
        ejeY = new Vector3[n];
        Map<Integer, Integer> huesoDelNodo = new HashMap<>();
        for (int i = 0; i < n; i++) {
            huesos[i] = nodos.get(nodoDelHueso[i]).getString("name");
            porNombre.put(huesos[i], i);
            huesoDelNodo.put(nodoDelHueso[i], i);
            padre[i] = -1;
        }
        for (int i = 0; i < n; i++) {
            JsonValue hijos = nodos.get(nodoDelHueso[i]).get("children");
            if (hijos == null) continue;
            for (int hijo : hijos.asIntArray()) {
                if (huesoDelNodo.containsKey(hijo)) padre[huesoDelNodo.get(hijo)] = i;
            }
        }
        // De cada hueso viene la matriz que lleva del cuerpo al hueso: su inversa dice dónde está y hacia dónde apunta.
        float[] matrices = flotantes(json, b, datos, piel.getInt("inverseBindMatrices"), 16);
        for (int i = 0; i < n; i++) {
            float[] m = new float[16];
            System.arraycopy(matrices, i * 16, m, 0, 16);
            Matrix4 alCuerpo = new Matrix4(m).inv();
            cabeza[i] = alCuerpo.getTranslation(new Vector3());
            ejeX[i] = new Vector3(1, 0, 0).rot(alCuerpo).nor();
            ejeY[i] = new Vector3(0, 1, 0).rot(alCuerpo).nor();
        }
        JsonValue pieza = json.get("meshes").get(0).get("primitives").get(0), de = pieza.get("attributes");
        puntos = flotantes(json, b, datos, de.getInt("POSITION"), 3);
        pesos = flotantes(json, b, datos, de.getInt("WEIGHTS_0"), 4);
        huesosDelPunto = enteros(json, b, datos, de.getInt("JOINTS_0"), 4);
        triangulos = enteros(json, b, datos, pieza.getInt("indices"), 1);
    }

    /** El número de ese hueso, o -1 si este cuerpo no lo tiene. */
    public int hueso(String nombre) {
        Integer i = porNombre.get(nombre);
        return i == null ? -1 : i;
    }

    /** Lo que mide de alto, de los pies a la coronilla. */
    public float altura() {
        float alto = 0;
        for (int i = 1; i < puntos.length; i += 3) {
            alto = Math.max(alto, puntos[i]);
        }
        return alto;
    }

    private static byte[] leer(String recurso) {
        try (InputStream entrada = Cuerpo.class.getResourceAsStream(recurso)) {
            if (entrada == null) throw new IllegalStateException("No está el maniquí " + recurso);
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el maniquí " + recurso, e);
        }
    }

    // Códigos de glTF para el tipo de cada número.
    private static final int OCTETO = 5121, CORTO = 5123, ENTERO = 5125;

    /** Dónde empieza una lista de números (un «accessor» de glTF) dentro del archivo. */
    private static int inicio(JsonValue json, int datos, JsonValue lista) {
        JsonValue vista = json.get("bufferViews").get(lista.getInt("bufferView"));
        return datos + vista.getInt("byteOffset", 0) + lista.getInt("byteOffset", 0);
    }

    private static float[] flotantes(JsonValue json, ByteBuffer b, int datos, int cual, int porElemento) {
        JsonValue lista = json.get("accessors").get(cual);
        float[] v = new float[lista.getInt("count") * porElemento];
        int desde = inicio(json, datos, lista);
        for (int i = 0; i < v.length; i++) {
            v[i] = b.getFloat(desde + i * 4);
        }
        return v;
    }

    private static int[] enteros(JsonValue json, ByteBuffer b, int datos, int cual, int porElemento) {
        JsonValue lista = json.get("accessors").get(cual);
        int[] v = new int[lista.getInt("count") * porElemento];
        int desde = inicio(json, datos, lista), tipo = lista.getInt("componentType");
        for (int i = 0; i < v.length; i++) {
            if (tipo == OCTETO) v[i] = b.get(desde + i) & 0xFF;
            else if (tipo == CORTO) v[i] = b.getShort(desde + i * 2) & 0xFFFF;
            else if (tipo == ENTERO) v[i] = b.getInt(desde + i * 4);
            else throw new IllegalStateException("Tipo de número de glTF que no se esperaba: " + tipo);
        }
        return v;
    }
}
