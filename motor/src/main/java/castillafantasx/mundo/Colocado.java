package castillafantasx.mundo;

import java.util.ArrayList;
import java.util.List;

/**
 * COLOCADO — Una pieza puesta a mano en el mundo con el editor: una cosa (un
 * mueble, una planta, cualquier dibujo del juego), una imagen propia, una
 * mancha de pincel, un trazo dibujado o un letrero escrito. Está donde se
 * quiera (no en una casilla), girada y a su tamaño. Es una línea de la
 * sección [colocado] del archivo de su mapa.
 */
public final class Colocado {

    /** Qué es. */
    public enum Clase {
        COSA("cosa"),           // un dibujo del juego: 'que' es su nombre en el Almacen ("decor/cruz")
        IMAGEN("imagen"),       // una imagen propia pasada a pixel art: 'que' es su nombre en res/mundo/imagenes/
        MANCHA("mancha"),       // una pincelada: 'que' es de qué (sangre, barro, hollin, musgo, humedad)
        TRAZO("trazo"),         // una línea dibujada a mano: 'que' es su color (RRGGBB) y 'texto' sus puntos
        LETRERO("letrero");     // algo escrito: 'que' es su color y 'texto' lo que dice

        public final String id;

        Clase(String id) {
            this.id = id;
        }

        static Clase porId(String id) {
            for (Clase c : values()) {
                if (c.id.equals(id)) return c;
            }
            return null;
        }
    }

    /** En qué capa se pinta. */
    public enum Capa {
        SUELO("suelo"),         // pegada al suelo, debajo de todo lo que está de pie
        PIE("pie"),             // de pie: delante o detrás de los demás según dónde tenga el pie
        ENCIMA("encima");       // sobre todo lo demás: lo que va en una pared, una puerta o un objeto

        public final String id;

        Capa(String id) {
            this.id = id;
        }

        static Capa porId(String id) {
            for (Capa c : values()) {
                if (c.id.equals(id)) return c;
            }
            return null;
        }
    }

    public Clase clase;
    /** Dónde está: en la superficie, en píxeles del mapa (el centro de su pie); en la mazmorra, en casillas. */
    public float x, y;
    public String que;
    /** Cuánto está girada, en grados (0 = como es). */
    public int giro;
    /** Lo grande que es: 1 = su tamaño. */
    public float tam = 1;
    /** Si está vuelta del revés, como en un espejo. */
    public boolean espejo;
    public Capa capa = Capa.PIE;
    /** Si estorba el paso (solo las cosas de pie). */
    public boolean solido;
    /** Cuál de las variantes del dibujo de una mancha. */
    public int variante;
    /** En la mazmorra, en qué cara del muro va lo que está «encima» (n, s, e, o) y a qué altura (de 0, el suelo, a 1). */
    public String cara;
    public float altura = 0.5f;
    /** Lo escrito de un letrero, o los puntos de un trazo ("0,0 4,3 8,9", desde x e y). */
    public String texto;

    // Los límites de lo que deja hacer el editor (PROVISIONALES).
    // TODO-FRAN: lo más pequeño y lo más grande que puede ser una pieza colocada.
    public static final float TAM_MINIMO = 0.25f, TAM_MAXIMO = 4f;

    public Colocado(Clase clase, float x, float y, String que) {
        this.clase = clase;
        this.x = x;
        this.y = y;
        this.que = que;
    }

    /** Una copia suelta (para deshacer un cambio). */
    public Colocado copia() {
        Colocado c = new Colocado(clase, x, y, que);
        c.tomar(this);
        return c;
    }

    /** Se queda con todo lo de otra (para deshacer un cambio sin cambiar de objeto). */
    public void tomar(Colocado otra) {
        clase = otra.clase;
        x = otra.x;
        y = otra.y;
        que = otra.que;
        giro = otra.giro;
        tam = otra.tam;
        espejo = otra.espejo;
        capa = otra.capa;
        solido = otra.solido;
        variante = otra.variante;
        cara = otra.cara;
        altura = otra.altura;
        texto = otra.texto;
    }

    private static String cifra(float numero) {
        return numero == Math.rint(numero) ? String.valueOf((int) numero) : String.valueOf(Math.round(numero * 100) / 100f);
    }

    /** Su línea de [colocado]: «cosa 6712 7203 decor/cruz giro=90 tam=1.5 espejo capa=suelo solido | texto». */
    public String aLinea() {
        StringBuilder linea = new StringBuilder();
        linea.append(clase.id).append(' ').append(cifra(x)).append(' ').append(cifra(y)).append(' ').append(que);
        if (giro != 0) linea.append(" giro=").append(giro);
        if (tam != 1) linea.append(" tam=").append(cifra(tam));
        if (espejo) linea.append(" espejo");
        if (capa != Capa.PIE) linea.append(" capa=").append(capa.id);
        if (solido) linea.append(" solido");
        if (variante != 0) linea.append(" variante=").append(variante);
        if (cara != null) linea.append(" cara=").append(cara).append(" altura=").append(cifra(altura));
        if (texto != null) linea.append(" | ").append(texto);
        return linea.toString();
    }

    /** La pieza que dice esa línea de [colocado]; si no se entiende, null. */
    public static Colocado deLinea(String linea) {
        int barra = linea.indexOf('|');
        String[] p = (barra >= 0 ? linea.substring(0, barra) : linea).trim().split("\\s+");
        if (p.length < 4 || Clase.porId(p[0]) == null) return null;
        Colocado c;
        try {
            c = new Colocado(Clase.porId(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), p[3]);
            for (int i = 4; i < p.length; i++) {
                String palabra = p[i];
                int igual = palabra.indexOf('=');
                String clave = igual < 0 ? palabra : palabra.substring(0, igual), valor = igual < 0 ? "" : palabra.substring(igual + 1);
                switch (clave) {
                    case "giro": c.giro = Integer.parseInt(valor); break;
                    case "tam": c.tam = Float.parseFloat(valor); break;
                    case "espejo": c.espejo = true; break;
                    case "capa": c.capa = Capa.porId(valor) != null ? Capa.porId(valor) : Capa.PIE; break;
                    case "solido": c.solido = true; break;
                    case "variante": c.variante = Integer.parseInt(valor); break;
                    case "cara": c.cara = valor; break;
                    case "altura": c.altura = Float.parseFloat(valor); break;
                    default: break;
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (barra >= 0) c.texto = linea.substring(barra + 1).trim();
        return c;
    }

    /** Los puntos de un trazo, en píxeles desde (x, y): {dx0, dy0, dx1, dy1…}. */
    public int[] puntos() {
        if (texto == null || texto.isEmpty()) return new int[0];
        String[] pares = texto.trim().split("\\s+");
        List<Integer> lista = new ArrayList<>();
        for (String par : pares) {
            String[] xy = par.split(",");
            if (xy.length != 2) continue;
            try {
                int px = Integer.parseInt(xy[0]), py = Integer.parseInt(xy[1]);
                lista.add(px);
                lista.add(py);
            } catch (NumberFormatException e) {
                // Un punto mal escrito se salta.
            }
        }
        int[] puntos = new int[lista.size()];
        for (int i = 0; i < puntos.length; i++) puntos[i] = lista.get(i);
        return puntos;
    }

    /** El color de un trazo o de un letrero (su 'que', RRGGBB). */
    public int color() {
        try {
            return Integer.parseInt(que, 16);
        } catch (NumberFormatException e) {
            return 0xC8B48C;
        }
    }

    @Override
    public String toString() {
        return aLinea();
    }
}
