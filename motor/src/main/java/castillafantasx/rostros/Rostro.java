package castillafantasx.rostros;

import java.util.Map;

/**
 * ROSTRO — Los rasgos de una cara: edad, piel, pelo, barba y la forma de cada
 * parte. Se escriben a mano en res/datos/rostros.txt; lo que no se escriba
 * queda con el valor de aquí. Las medidas son proporciones: 1 = lo corriente.
 */
public final class Rostro {

    public enum Barba { NINGUNA, RASTROJO, BIGOTE, PERILLA, CORTA, LARGA }
    public enum Nariz { RECTA, AGUILENA, CHATA, ANCHA }
    public enum Cicatriz { NINGUNA, CEJA, MEJILLA, LABIO }

    public final String nombre;
    public boolean mujer;
    public int edad = 30;
    /** Color de la piel (0xRRGGBB); -1 = el de su atuendo. Y cuánto la ha curtido el sol y el frío (0 a 1). */
    public int piel = -1;
    public double curtida = 0.3;
    /** Color del pelo (-1 = el de su atuendo), cuántas canas (0 a 1) y cuánto le clarea la frente (0 a 1). */
    public int pelo = -1;
    public double canas, entradas;
    public Barba barba = Barba.NINGUNA;
    public Nariz nariz = Nariz.RECTA;
    public Cicatriz cicatriz = Cicatriz.NINGUNA;
    public int ojos = 0x2A1C14;
    // La forma: ancho de la cara, mandíbula, mentón, pómulos, tamaño de la nariz, de los ojos y de la boca, grosor de labios y cejas.
    public double ancho = 1, mandibula = 1, menton = 1, pomulos = 1, narizGrande = 1, ojosGrandes = 1, boca = 1, labios = 1, cejas = 1;
    /** De -1 (chupado, en los huesos) a 1 (gordo). */
    public double carnes;
    /** Hacia dónde tiene vuelta la cabeza en el retrato (radianes; negativo, hacia la luz). */
    public double giro = -0.28;
    /** Grosor del cuello: 1 = corriente; un guerrero, más. */
    public double cuello = 1;

    /** Solo para los rostros de la cantera: con la ropa de qué aspecto del Vestuario se enseñan en la galería. */
    public String viste = "aldeano";

    public Rostro(String nombre) {
        this.nombre = nombre;
    }

    /** Otro rostro igual (el editor de retratos retoca la copia: el de rostros.txt no se toca). */
    public Rostro copia() {
        Rostro c = new Rostro(nombre);
        c.mujer = mujer;
        c.edad = edad;
        c.piel = piel;
        c.curtida = curtida;
        c.pelo = pelo;
        c.canas = canas;
        c.entradas = entradas;
        c.barba = barba;
        c.nariz = nariz;
        c.cicatriz = cicatriz;
        c.ojos = ojos;
        c.ancho = ancho;
        c.mandibula = mandibula;
        c.menton = menton;
        c.pomulos = pomulos;
        c.narizGrande = narizGrande;
        c.ojosGrandes = ojosGrandes;
        c.boca = boca;
        c.labios = labios;
        c.cejas = cejas;
        c.carnes = carnes;
        c.giro = giro;
        c.cuello = cuello;
        c.viste = viste;
        return c;
    }

    /** De 0 (adulto) a 1 (niño pequeño): cambia las proporciones de la cabeza. */
    public double ninez() {
        return Math.max(0, Math.min(1, (16 - edad) / 10.0));
    }

    /** De 0 (joven) a 1 (anciano): arrugas, ojeras y carnes caídas. */
    public double vejez() {
        return Math.max(0, Math.min(1, (edad - 28) / 40.0));
    }

    /** Rellena el rostro con las líneas "clave = valor" de su sección. Una clave desconocida para el juego: mejor saberlo. */
    void leer(Map<String, String> datos) {
        for (Map.Entry<String, String> d : datos.entrySet()) {
            String v = d.getValue();
            switch (d.getKey()) {
                case "sexo":      mujer = v.equals("mujer"); break;
                case "edad":      edad = Integer.parseInt(v); break;
                case "piel":      piel = color(v); break;
                case "curtida":   curtida = Double.parseDouble(v); break;
                case "pelo":      pelo = color(v); break;
                case "canas":     canas = Double.parseDouble(v); break;
                case "entradas":  entradas = Double.parseDouble(v); break;
                case "barba":     barba = Barba.valueOf(v.toUpperCase()); break;
                case "nariz":     nariz = Nariz.valueOf(v.toUpperCase()); break;
                case "cicatriz":  cicatriz = Cicatriz.valueOf(v.toUpperCase()); break;
                case "ojos":      ojos = color(v); break;
                case "ancho":     ancho = Double.parseDouble(v); break;
                case "mandibula": mandibula = Double.parseDouble(v); break;
                case "menton":    menton = Double.parseDouble(v); break;
                case "pomulos":   pomulos = Double.parseDouble(v); break;
                case "nariz_grande": narizGrande = Double.parseDouble(v); break;
                case "ojos_grandes": ojosGrandes = Double.parseDouble(v); break;
                case "boca":      boca = Double.parseDouble(v); break;
                case "labios":    labios = Double.parseDouble(v); break;
                case "cejas":     cejas = Double.parseDouble(v); break;
                case "carnes":    carnes = Double.parseDouble(v); break;
                case "giro":      giro = Double.parseDouble(v); break;
                case "cuello":    cuello = Double.parseDouble(v); break;
                case "viste":     viste = v; break;
                default: throw new IllegalArgumentException("rostros.txt, rostro «" + nombre + "»: no entiendo «" + d.getKey() + "»");
            }
        }
    }

    /** Un color por su nombre (los de la lista) o escrito como RRGGBB. */
    static int color(String v) {
        switch (v) {
            case "palida":   return 0xC8A486;
            case "clara":    return 0xB48866;
            case "triguena": return 0x9E7250;
            case "morena":   return 0x835638;
            case "oscura":   return 0x623C28;
            case "negro":    return 0x1C1618;
            case "castano":  return 0x3E2A1E;
            case "rubio":    return 0x8A6A32;
            case "rojizo":   return 0x6A3420;
            case "cano":     return 0x9A958C;
            case "pardos":   return 0x2A1C14;
            case "miel":     return 0x6A4A20;
            case "verdes":   return 0x4A5A38;
            case "azules":   return 0x4A6278;
            case "grises":   return 0x5A6068;
            default:         return Integer.parseInt(v, 16);
        }
    }
}
