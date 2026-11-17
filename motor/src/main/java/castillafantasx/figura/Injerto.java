package castillafantasx.figura;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * INJERTO — Una pieza de un ser nuevo (FiguraCompuesta): de qué figura sale
 * (una persona, una especie o una pieza de horror), qué partes suyas se toman,
 * dónde se pone, con qué giro y tamaño, cómo se deforma y cuántas veces se
 * repite. Se escribe en una línea del archivo del ser (res/seres/).
 */
public final class Injerto {

    /** De dónde sale: «persona:fraile», «especie:<id>» o «pieza:gancho». */
    public String de = "pieza:ojo";
    /** Las partes de esa figura que se toman (los números que ella le da al Escultor); vacía = todas. */
    public final List<Integer> partes = new ArrayList<>();
    /** Dónde se pone: desde el suelo bajo el ser o, si cuelga de otra pieza, desde ella (x a su derecha, y adelante, z arriba). */
    public P3 en = P3.CERO;
    /** Su giro, en grados: a su izquierda, hacia arriba y ladeado a su derecha. */
    public double gira, alza, ladea;
    /** Su tamaño (todo él), lo que se estira en cada eje y lo que se hincha (solo el grueso). */
    public double tam = 1, grosor = 1;
    public P3 estira = new P3(1, 1, 1);
    /** Lo que se retuerce (grados por cada 10 píxeles de alto) y lo que se abre en dos, a un lado y a otro (píxeles). */
    public double retuerce, abre;
    /** El material del que se pinta entero (null = los suyos). */
    public String material;
    /** De qué pieza cuelga (su número en la lista; -1 = de ninguna: del suelo) y de qué parte suya. */
    public int sujetoA = -1, parteDeSujecion;
    /** Cuántas veces se repite alrededor del eje vertical y en qué arco (grados). */
    public int veces = 1;
    public double arco = 360;
    /** Se funde con la pieza de la que cuelga: un puente de carne las une. */
    public boolean funde;
    /** Lo que va por delante o por detrás del resto al moverse (segundos). */
    public double desfase;

    public Injerto copia() {
        return deTexto(aTexto());
    }

    private static String num(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String tres(P3 p) {
        return num(p.x) + "," + num(p.y) + "," + num(p.z);
    }

    /** Como se escribe en el archivo: «de=persona:fraile partes=6 en=0,2,30 tam=1.2 material=CARNE_VIVA». Lo que está de fábrica no se escribe. */
    public String aTexto() {
        StringBuilder s = new StringBuilder("de=" + de);
        if (!partes.isEmpty()) {
            s.append(" partes=");
            for (int i = 0; i < partes.size(); i++) s.append(i > 0 ? "," : "").append(partes.get(i));
        }
        if (en.largo() != 0) s.append(" en=").append(tres(en));
        if (gira != 0 || alza != 0 || ladea != 0) s.append(" giro=").append(num(gira)).append(',').append(num(alza)).append(',').append(num(ladea));
        if (tam != 1) s.append(" tam=").append(num(tam));
        if (estira.x != 1 || estira.y != 1 || estira.z != 1) s.append(" estira=").append(tres(estira));
        if (grosor != 1) s.append(" grosor=").append(num(grosor));
        if (retuerce != 0) s.append(" retuerce=").append(num(retuerce));
        if (abre != 0) s.append(" abre=").append(num(abre));
        if (material != null) s.append(" material=").append(material);
        if (sujetoA >= 0) s.append(" sujeto=").append(sujetoA).append(':').append(parteDeSujecion);
        if (veces != 1) s.append(" veces=").append(veces).append(" arco=").append(num(arco));
        if (funde) s.append(" funde");
        if (desfase != 0) s.append(" desfase=").append(num(desfase));
        return s.toString();
    }

    private static P3 leerTres(String v) {
        String[] t = v.split(",");
        return new P3(Double.parseDouble(t[0]), Double.parseDouble(t[1]), Double.parseDouble(t[2]));
    }

    /** Lee lo que escribe aTexto(). Lo que no entiende, lo deja de fábrica. */
    public static Injerto deTexto(String texto) {
        Injerto in = new Injerto();
        for (String trozo : texto.trim().split("\\s+")) {
            int igual = trozo.indexOf('=');
            String clave = igual < 0 ? trozo : trozo.substring(0, igual), v = igual < 0 ? "" : trozo.substring(igual + 1);
            try {
                switch (clave) {
                    case "de":       in.de = v; break;
                    case "partes":
                        for (String p : v.split(",")) {
                            if (!p.isEmpty() && !p.equals("todas")) in.partes.add(Integer.parseInt(p));
                        }
                        break;
                    case "en":       in.en = leerTres(v); break;
                    case "giro": {
                        P3 g = leerTres(v);
                        in.gira = g.x;
                        in.alza = g.y;
                        in.ladea = g.z;
                        break;
                    }
                    case "tam":      in.tam = Double.parseDouble(v); break;
                    case "estira":   in.estira = leerTres(v); break;
                    case "grosor":   in.grosor = Double.parseDouble(v); break;
                    case "retuerce": in.retuerce = Double.parseDouble(v); break;
                    case "abre":     in.abre = Double.parseDouble(v); break;
                    case "material": in.material = v; break;
                    case "sujeto": {
                        String[] t = v.split(":");
                        in.sujetoA = Integer.parseInt(t[0]);
                        in.parteDeSujecion = t.length > 1 ? Integer.parseInt(t[1]) : 0;
                        break;
                    }
                    case "veces":    in.veces = Math.max(1, Math.min(24, Integer.parseInt(v))); break;
                    case "arco":     in.arco = Double.parseDouble(v); break;
                    case "funde":    in.funde = true; break;
                    case "desfase":  in.desfase = Double.parseDouble(v); break;
                    default: break;
                }
            } catch (RuntimeException e) {
                // Un trozo estropeado no tumba el juego: esa cifra se queda de fábrica.
                System.err.println("Injerto: no se entiende «" + trozo + "»");
            }
        }
        return in;
    }
}
