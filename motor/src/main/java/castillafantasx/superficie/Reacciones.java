package castillafantasx.superficie;

import java.util.Random;

import castillafantasx.datos.LectorSecciones;

/**
 * REACCIONES — Lee res/datos/reacciones.txt una sola vez: cómo reacciona
 * cada grupo de gente cuando la agreden o cuando ataca un monstruo, a qué
 * grupo y cultura pertenece cada persona y por quién sale en defensa cada
 * cultura. Todo lo que hay aquí es PROVISIONAL.
 */
// TODO-FRAN: las proporciones y la tabla de culturas se ajustan en res/datos/reacciones.txt, sin tocar el código.
public final class Reacciones {

    private Reacciones() { }

    /** Lo que hace una persona agredida por el héroe. */
    public enum Agredido { PELEA, HUYE, VOCEA, PIEDRA, PALO }

    /** Lo que hace una persona cuando un monstruo ataca en su zona. */
    public enum EnPeligro { SE_ALEJA, ATACA, PIEDRA }

    private static final LectorSecciones ARCHIVO = new LectorSecciones("/datos/reacciones.txt");

    public static final float VELOCIDAD_DE_LA_PIEDRA = numero("velocidad_de_la_piedra");
    public static final float ALCANCE_DE_LA_PIEDRA = numero("alcance_de_la_piedra");
    public static final float RADIO_DE_LA_PIEDRA = numero("radio_de_la_piedra");
    public static final float PUNTERIA = numero("punteria");
    public static final float DISTANCIA_PARA_APEDREAR = numero("distancia_para_apedrear");
    public static final int FRAMES_ENTRE_PIEDRAS = frames("espera_entre_piedras");
    public static final float ALCANCE_DEL_PALO = numero("alcance_del_palo");
    public static final float DISTANCIA_A_SALVO = numero("distancia_a_salvo");
    public static final float RADIO_DEL_PROTECTOR = numero("radio_del_protector");
    public static final int FRAMES_DE_ENFADO = frames("segundos_de_enfado");
    public static final int FRAMES_DE_PELIGRO = frames("segundos_de_peligro");
    public static final float DESARMADOS_QUE_APEDREAN = numero("desarmados_que_apedrean");

    private static float numero(String clave) {
        String texto = ARCHIVO.valor("cifras", clave, null);
        if (texto == null) throw new IllegalStateException("reacciones.txt: falta «" + clave + "» en [cifras]");
        return Float.parseFloat(texto.trim());
    }

    /** Una cifra en segundos, pasada a frames (60 por segundo). */
    private static int frames(String clave) {
        return Math.round(numero(clave) * 60);
    }

    /** Las palabras de la ficha de una persona: {grupo, cultura} o {grupo, cultura, "nino"}. */
    private static String[] ficha(String aspecto) {
        return ARCHIVO.valor("gente", aspecto, "hombre castellana").trim().split("\\s+");
    }

    public static String grupoDe(String aspecto) {
        return ficha(aspecto)[0];
    }

    public static String culturaDe(String aspecto) {
        return ficha(aspecto)[1];
    }

    public static boolean esNino(String aspecto) {
        String[] f = ficha(aspecto);
        return f.length > 2 && f[2].equals("nino");
    }

    /** Las proporciones de un grupo en una sección ([agredidos] o [peligro]). */
    private static double[] proporciones(String seccion, String grupo) {
        String texto = ARCHIVO.valor(seccion, grupo, null);
        if (texto == null) throw new IllegalStateException("reacciones.txt: falta el grupo «" + grupo + "» en [" + seccion + "]");
        String[] p = texto.trim().split("\\s+");
        double[] partes = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            partes[i] = Double.parseDouble(p[i]);
        }
        return partes;
    }

    /** Elige una casilla de la tabla según sus proporciones: la tirada va de 0 a 1. */
    private static int elegir(double[] partes, double tirada) {
        double total = 0;
        for (double parte : partes) {
            total += parte;
        }
        double suma = 0;
        for (int i = 0; i < partes.length; i++) {
            suma += partes[i] / total;
            if (tirada < suma) return i;
        }
        return partes.length - 1;
    }

    /** La reacción que le toca a un individuo de ese grupo cuando le agreden. */
    public static Agredido agredido(String grupo, Random azar) {
        return Agredido.values()[elegir(proporciones("agredidos", grupo), azar.nextDouble())];
    }

    /** La reacción que le toca a un individuo de ese grupo cuando ataca un monstruo. */
    public static EnPeligro enPeligro(String grupo, Random azar) {
        return EnPeligro.values()[elegir(proporciones("peligro", grupo), azar.nextDouble())];
    }

    /**
     * ¿Sale en defensa un protector de esa cultura? 'protegido' es el id de
     * la especie del animal (su id) o "nino".
     */
    public static boolean defiende(String cultura, String protegido) {
        String lista = ARCHIVO.valor("culturas", cultura, null);
        if (lista == null) throw new IllegalStateException("reacciones.txt: falta la cultura «" + cultura + "»");
        for (String uno : lista.trim().split("\\s+")) {
            if (uno.equals(protegido)) return true;
        }
        return false;
    }
}
