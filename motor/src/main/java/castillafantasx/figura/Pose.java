package castillafantasx.figura;



/**
 * POSE — La postura de un cuerpo humano en un instante: dónde tiene la
 * cadera, cómo dobla el torso, dónde están las manos y los pies y hacia dónde
 * apunta el arma. Los codos y las rodillas no se guardan: salen solos de
 * dónde están la mano y el pie (ver Humano.doblar).
 *
 * Mezclando dos poses se obtiene cualquier postura intermedia: así se anima.
 */
public final class Pose {

    /** Altura de la cadera de un adulto de pie, con las rodillas un poco flojas. */
    public static final double ALTO_CADERA = 22.8;

    public P3 cadera = new P3(0, 0, ALTO_CADERA);
    /** Torso: inclinado hacia delante, ladeado a su derecha y girado a su izquierda (radianes). */
    public double inclina, ladea, gira;
    public double cabezaBaja, cabezaGira;
    /** Las manos, medidas desde su hombro y dentro del torso (si el torso gira, van con él). */
    public P3 manoD = new P3(1.2, 1.5, -15.4), manoI = new P3(-1.2, 1.5, -15.4);
    /** Los pies, medidos desde el suelo bajo la figura (z = cuánto se levantan). */
    public P3 pieD = new P3(2.7, 0, 0), pieI = new P3(-2.7, 0, 0);
    /** Hacia dónde apunta el arma y hacia dónde mira su filo, dentro del torso. */
    public P3 arma = new P3(0.2, 0.8, -0.55), filo = new P3(0, 0, -1);
    /** Vuelta de todo el cuerpo, pies incluidos, sobre sí mismo (radianes, hacia su izquierda): los giros de un golpe. */
    public double vuelta;
    /** 0 = de pie; 1 = tumbado boca arriba en el suelo. */
    public double caida;
    /** Cuánto vuela la capa hacia atrás, de 0 a 1. */
    public double vuelo = 0.08;
    /** De 0 a 1: cuánto acude la mano izquierda a la empuñadura de un arma de una mano (un remate a dos manos). */
    public double agarraI;

    /** La pose que queda entre 'a' (parte 0) y 'b' (parte 1). */
    public static Pose mezcla(Pose a, Pose b, double parte) {
        Pose p = new Pose();
        p.cadera = a.cadera.hacia(b.cadera, parte);
        p.inclina = entre(a.inclina, b.inclina, parte);
        p.ladea = entre(a.ladea, b.ladea, parte);
        p.gira = entre(a.gira, b.gira, parte);
        p.cabezaBaja = entre(a.cabezaBaja, b.cabezaBaja, parte);
        p.cabezaGira = entre(a.cabezaGira, b.cabezaGira, parte);
        p.manoD = a.manoD.hacia(b.manoD, parte);
        p.manoI = a.manoI.hacia(b.manoI, parte);
        p.pieD = a.pieD.hacia(b.pieD, parte);
        p.pieI = a.pieI.hacia(b.pieI, parte);
        p.arma = a.arma.hacia(b.arma, parte);
        p.filo = a.filo.hacia(b.filo, parte);
        p.vuelta = entre(a.vuelta, b.vuelta, parte);
        p.caida = entre(a.caida, b.caida, parte);
        p.vuelo = entre(a.vuelo, b.vuelo, parte);
        p.agarraI = entre(a.agarraI, b.agarraI, parte);
        return p;
    }

    /**
     * Le suma lo retocado en el editor de poses: «manoD», «manoI», «pieD», «pieI», «cadera», «arma» y «filo» son
     * tres cifras (píxeles); «inclina», «ladea», «gira», «cabezaBaja», «cabezaGira» y «vuelta», una (grados); «vuelo», una.
     */
    public void sumar(java.util.Map<String, double[]> cambios) {
        for (java.util.Map.Entry<String, double[]> c : cambios.entrySet()) {
            double[] v = c.getValue();
            P3 mas = v.length >= 3 ? new P3(v[0], v[1], v[2]) : P3.CERO;
            double uno = v.length >= 1 ? v[0] : 0, rad = Math.toRadians(uno);
            switch (c.getKey()) {
                case "cadera":     cadera = cadera.mas(mas); break;
                case "manoD":      manoD = manoD.mas(mas); break;
                case "manoI":      manoI = manoI.mas(mas); break;
                case "pieD":       pieD = pieD.mas(mas); break;
                case "pieI":       pieI = pieI.mas(mas); break;
                case "arma":       arma = arma.mas(mas); break;
                case "filo":       filo = filo.mas(mas); break;
                case "inclina":    inclina += rad; break;
                case "ladea":      ladea += rad; break;
                case "gira":       gira += rad; break;
                case "cabezaBaja": cabezaBaja += rad; break;
                case "cabezaGira": cabezaGira += rad; break;
                case "vuelta":     vuelta += rad; break;
                case "vuelo":      vuelo = Math.max(0, Math.min(1, vuelo + uno)); break;
                default: break;
            }
        }
    }

    /** Mezcla que pasa cerca de una pose intermedia: el recorrido hace una curva en vez de ir recto. */
    public static Pose curva(Pose a, Pose medio, Pose b, double parte) {
        return mezcla(mezcla(a, medio, parte), mezcla(medio, b, parte), parte);
    }

    public static double entre(double a, double b, double parte) {
        return a + (b - a) * parte;
    }

    /** De 0 a 1 arrancando y frenando con suavidad. */
    public static double suave(double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    /** De 0 a 1 saliendo de golpe y frenando al final: un tajo, un puñetazo. */
    public static double latigazo(double t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }
}
