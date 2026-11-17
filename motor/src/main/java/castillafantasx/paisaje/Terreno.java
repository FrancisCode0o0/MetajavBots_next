package castillafantasx.paisaje;

import java.util.ArrayDeque;

import castillafantasx.combate.CifrasDeSalto;
import castillafantasx.superficie.MapaSuperficie;

/**
 * TERRENO — Lo que el paisaje necesita saber de cada casilla del mapa, ya
 * calculado (una vez, al cargar): de qué clase es su suelo, a qué altura
 * está, si es peña y cuánto se mete en ella, lo cerca que tiene el agua, la
 * roca y los árboles, y hacia dónde corre el agua de un río.
 *
 * El mapa sigue siendo de casillas; con estos datos el Horno pinta formas
 * naturales (docs/encargos_viejos/ENCARGO_PAISAJE.md).
 */
public final class Terreno {

    // Las clases de suelo que se pintan.
    public static final int HIERBA = 0, TIERRA = 1, EMPEDRADO = 2, LOSA = 3, BARRO = 4, PAJA = 5, NERVIOS = 6,
            LANCHA = 7, AGUA = 8, GRIETA = 9, ROCA = 10, SEMBRADO = 11, BARBECHO = 12,
            // Los suelos de dentro de las casas (encargo de los interiores): un mapa los pide en su sección [aspecto].
            TARIMA = 13, BALDOSA = 14, ESTERA = 15, DAMERO = 16, OLAMBRILLA = 17, TIERRA_PISADA = 18, ENLOSADO = 19, CLASES = 20;
    /** Los suelos de interior por su nombre en [aspecto] («suelo . = tarima»). */
    public static final String[] SUELOS_DE_INTERIOR = { "tarima", "baldosa", "estera", "damero", "olambrilla", "tierra_pisada", "enlosado" };
    private static final int SIN_CLASE = -1;        // muros, casas y cobertizos: debajo sigue el suelo de al lado

    private static final int TAM = MapaSuperficie.TAM;
    // Cifras de dibujo, PROVISIONALES.
    static final int HONDO_DE_LA_ROCA = 4;          // casillas hacia dentro de una peña a partir de las que ya no sube más
    private static final int ALCANCE_DEL_AGUA = 5;  // casillas a las que llega el frescor de una orilla
    private static final int ALCANCE_DE_LA_ROCA = 3;        // casillas a las que llegan las piedras caídas de una peña
    private static final int RIO_MINIMO = 12;       // con menos casillas, un agua es una charca
    private static final float AGUA_DE_DENTRO = 0.25f;      // si más de esta parte de un agua está rodeada de agua, es un lago

    public final MapaSuperficie mapa;
    public final int columnas, filas;
    private final int[][] clase;
    private final float[][] altura;         // píxeles sobre el llano (la peña, la del suelo que tiene al lado)
    private final int[][] dentroDeRoca;     // 0 = no es peña; 1 = peña del borde; 2, 3… = más adentro
    private final float[][] frescor;        // de 0 (secano) a 1 (agua)
    private final float[][] pedregal;       // de 0 a 1: lo cerca que hay una peña
    private final float[][] arbolado;       // de 0 a 1: cuántos árboles hay alrededor
    private final float[][] pisado;         // de 0 a 1: lo cerca que hay casas y cobertizos
    private final boolean[][] rio;          // agua que corre
    private final float[][] corrienteX, corrienteY;     // hacia dónde corre (un paso de casilla)
    private final float[][] hondura;        // de 0 (orilla) a 1 (lo más hondo): cuánta agua tiene alrededor
    private final float[][] bulto;          // lo que levanta para dar sombra: su altura y, si es peña, la de la peña
    private final float[][] bultoCerca;     // el mayor bulto que hay a pocas casillas, a levante o a poniente

    public Terreno(MapaSuperficie mapa) {
        this.mapa = mapa;
        columnas = mapa.columnas;
        filas = mapa.filas;
        clase = new int[filas][columnas];
        altura = new float[filas][columnas];
        dentroDeRoca = new int[filas][columnas];
        frescor = new float[filas][columnas];
        pedregal = new float[filas][columnas];
        arbolado = new float[filas][columnas];
        pisado = new float[filas][columnas];
        rio = new boolean[filas][columnas];
        corrienteX = new float[filas][columnas];
        corrienteY = new float[filas][columnas];
        hondura = new float[filas][columnas];
        bulto = new float[filas][columnas];
        bultoCerca = new float[filas][columnas];
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                clase[f][c] = claseDe(mapa, mapa.suelo(c, f));
                altura[f][c] = mapa.alturaDe(c, f);
            }
        }
        sueloBajoLosEdificios();
        medirLasRocas();
        float[][] aguas = cercania(AGUA, ALCANCE_DEL_AGUA);
        float[][] rocas = cercania(ROCA, ALCANCE_DE_LA_ROCA);
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                frescor[f][c] = aguas[f][c];
                pedregal[f][c] = rocas[f][c];
                arbolado[f][c] = alrededor(c, f, 2, true) / 10f;
                pisado[f][c] = Math.min(1, alrededor(c, f, 2, false) / 6f);
                hondura[f][c] = honduraDe(c, f);
            }
        }
        rios();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                bulto[f][c] = altura[f][c] + (clase[f][c] == ROCA ? Formas.alzadoMaximoDeRoca(dentroDeRoca[f][c] - 1) * 0.75f : 0);
            }
        }
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                for (int ff = f - 1; ff <= f + 1; ff++) {
                    for (int cc = c - ALCANCE_DE_LA_SOMBRA; cc <= c + ALCANCE_DE_LA_SOMBRA; cc++) {
                        bultoCerca[f][c] = Math.max(bultoCerca[f][c], bulto(cc, ff));
                    }
                }
            }
        }
    }

    private static final int ALCANCE_DE_LA_SOMBRA = 4;      // casillas a las que puede llegar la sombra de algo alto

    /** Lo que levanta la casilla para dar sombra: su altura y, si es peña, su bulto. */
    public float bulto(int c, int f) {
        return bulto[fila(f)][col(c)];
    }

    /** El mayor bulto que hay a pocas casillas de esta, a levante o a poniente: si no pasa de su altura, nada le da sombra. */
    public float bultoCerca(int c, int f) {
        return bultoCerca[fila(f)][col(c)];
    }

    /** La clase con ese nombre de [aspecto] (los de interior y los de siempre), o -1 si no existe. */
    public static int claseDeNombre(String nombre) {
        for (int i = 0; i < SUELOS_DE_INTERIOR.length; i++) {
            if (SUELOS_DE_INTERIOR[i].equals(nombre)) return TARIMA + i;
        }
        switch (nombre) {
            case "hierba": return HIERBA;
            case "tierra": return TIERRA;
            case "empedrado": return EMPEDRADO;
            case "losa": return LOSA;
            case "barro": return BARRO;
            case "paja": return PAJA;
            default: return -1;
        }
    }

    /** La clase de una letra de suelo en ese mapa: la que diga su [aspecto] o, si no dice nada, la de siempre. */
    private static int claseDe(MapaSuperficie mapa, char suelo) {
        String aspecto = mapa.aspectoDeSuelo.get(suelo);
        if (aspecto != null) {
            int clase = claseDeNombre(aspecto);
            if (clase < 0) throw new IllegalStateException(mapa.id + ".txt [aspecto]: no hay suelos de «" + aspecto + "»");
            return clase;
        }
        return claseDe(suelo);
    }

    private static int claseDe(char suelo) {
        switch (suelo) {
            case '.': return HIERBA;
            case ',': return TIERRA;
            case ':': return EMPEDRADO;
            case '_': return LOSA;
            case '=': return BARRO;
            case ';': return PAJA;
            case 'x': return NERVIOS;
            case '>': return LANCHA;
            case '~': case '\'': return AGUA;
            case '/': return GRIETA;
            case '^': return ROCA;
            case '{': return SEMBRADO;
            case '}': return BARBECHO;
            default:  return SIN_CLASE;
        }
    }

    /** ¿Está la casilla dentro del mapa? */
    private boolean dentro(int c, int f) {
        return c >= 0 && f >= 0 && c < columnas && f < filas;
    }

    private static final int[][] LADOS = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

    /** Debajo de un muro o de una casa sigue el suelo de la casilla con suelo más cercana: así el muro no hace borde. */
    private void sueloBajoLosEdificios() {
        ArrayDeque<int[]> cola = new ArrayDeque<>();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (clase[f][c] != SIN_CLASE) cola.add(new int[] { c, f });
            }
        }
        while (!cola.isEmpty()) {
            int[] aqui = cola.poll();
            for (int[] lado : LADOS) {
                int c = aqui[0] + lado[0], f = aqui[1] + lado[1];
                if (!dentro(c, f) || clase[f][c] != SIN_CLASE) continue;
                // El agua, la grieta y la peña no se meten debajo de una casa: ahí, tierra.
                int vecina = clase[aqui[1]][aqui[0]];
                clase[f][c] = vecina == AGUA || vecina == GRIETA || vecina == ROCA ? TIERRA : vecina;
                cola.add(new int[] { c, f });
            }
        }
    }

    /**
     * Las peñas: cuánto se mete cada casilla de roca en su peña (1 = el
     * borde) y a qué altura está su pie, que es la del suelo que tiene al lado
     * (en [alturas] la roca no lleva altura: se la da el terreno que la rodea).
     */
    private void medirLasRocas() {
        ArrayDeque<int[]> cola = new ArrayDeque<>();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (clase[f][c] != ROCA) continue;
                float pie = -1;
                for (int[] lado : LADOS) {
                    int cc = c + lado[0], ff = f + lado[1];
                    if (dentro(cc, ff) && clase[ff][cc] != ROCA) pie = Math.max(pie, altura[ff][cc]);
                }
                if (pie < 0) continue;
                dentroDeRoca[f][c] = 1;
                altura[f][c] = pie;
                cola.add(new int[] { c, f });
            }
        }
        while (!cola.isEmpty()) {
            int[] aqui = cola.poll();
            for (int[] lado : LADOS) {
                int c = aqui[0] + lado[0], f = aqui[1] + lado[1];
                if (!dentro(c, f) || clase[f][c] != ROCA || dentroDeRoca[f][c] != 0) continue;
                dentroDeRoca[f][c] = Math.min(HONDO_DE_LA_ROCA, dentroDeRoca[aqui[1]][aqui[0]] + 1);
                altura[f][c] = altura[aqui[1]][aqui[0]];
                cola.add(new int[] { c, f });
            }
        }
    }

    /** De 0 a 1, lo cerca que tiene cada casilla una de esa clase: 1 en ella, 0 a 'alcance' casillas o más. */
    private float[][] cercania(int deClase, int alcance) {
        int[][] pasos = new int[filas][columnas];
        ArrayDeque<int[]> cola = new ArrayDeque<>();
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                pasos[f][c] = clase[f][c] == deClase ? 0 : alcance;
                if (clase[f][c] == deClase) cola.add(new int[] { c, f });
            }
        }
        while (!cola.isEmpty()) {
            int[] aqui = cola.poll();
            for (int[] lado : LADOS) {
                int c = aqui[0] + lado[0], f = aqui[1] + lado[1];
                if (!dentro(c, f) || pasos[f][c] <= pasos[aqui[1]][aqui[0]] + 1) continue;
                pasos[f][c] = pasos[aqui[1]][aqui[0]] + 1;
                cola.add(new int[] { c, f });
            }
        }
        float[][] cerca = new float[filas][columnas];
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                cerca[f][c] = 1 - pasos[f][c] / (float) alcance;
            }
        }
        return cerca;
    }

    /** Cuántos árboles (o, si no, cuántas casillas de casa o cobertizo) hay a 'radio' casillas o menos. */
    private int alrededor(int c, int f, int radio, boolean arboles) {
        int n = 0;
        for (int ff = f - radio; ff <= f + radio; ff++) {
            for (int cc = c - radio; cc <= c + radio; cc++) {
                if (!dentro(cc, ff)) continue;
                char cosa = mapa.cosa(cc, ff), suelo = mapa.suelo(cc, ff);
                if (arboles ? cosa == 't' : "hdnkq".indexOf(suelo) >= 0) n++;
            }
        }
        return n;
    }

    /** Lo hondo que es el agua de una casilla: cuanta más agua la rodea, más. */
    private float honduraDe(int c, int f) {
        if (clase[f][c] != AGUA) return 0;
        int n = 0;
        for (int ff = f - 1; ff <= f + 1; ff++) {
            for (int cc = c - 1; cc <= c + 1; cc++) {
                if (dentro(cc, ff) && clase[ff][cc] == AGUA) n++;
            }
        }
        return (n - 1) / 8f;
    }

    /**
     * Los ríos: cada agua larga y estrecha (casi toda ella es orilla) corre.
     * Va hacia donde sale del mapa (si toca un borde) o, si no, hacia su
     * casilla más al sur: desde ahí se cuentan los pasos río arriba, y cada
     * casilla corre hacia la vecina que tiene menos. Un puente (losas con
     * agua al lado) no parte el río: el agua sigue por debajo.
     */
    private void rios() {
        int[][] pasos = new int[filas][columnas];
        boolean[][] cauce = new boolean[filas][columnas];      // el agua y lo que pasa bajo los puentes
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                boolean puente = clase[f][c] == LOSA && (clase(c - 1, f) == AGUA || clase(c + 1, f) == AGUA || clase(c, f - 1) == AGUA || clase(c, f + 1) == AGUA);
                cauce[f][c] = clase[f][c] == AGUA || puente;
            }
        }
        boolean[][] vista = new boolean[filas][columnas];
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (clase[f][c] != AGUA || vista[f][c]) continue;
                // Todas las casillas de esta agua.
                java.util.List<int[]> suyas = new java.util.ArrayList<>();
                ArrayDeque<int[]> cola = new ArrayDeque<>();
                cola.add(new int[] { c, f });
                vista[f][c] = true;
                int[] desagua = { c, f };
                boolean sale = false;        // ¿toca el borde del mapa?
                int deDentro = 0;
                while (!cola.isEmpty()) {
                    int[] aqui = cola.poll();
                    suyas.add(aqui);
                    boolean enElBorde = aqui[0] == 0 || aqui[1] == 0 || aqui[0] == columnas - 1 || aqui[1] == filas - 1;
                    if (enElBorde && !sale) {
                        desagua = aqui;
                        sale = true;
                    } else if (!sale && aqui[1] > desagua[1]) desagua = aqui;
                    if (honduraDe(aqui[0], aqui[1]) == 1) deDentro++;
                    for (int[] lado : LADOS) {
                        int cc = aqui[0] + lado[0], ff = aqui[1] + lado[1];
                        if (!dentro(cc, ff) || !cauce[ff][cc] || vista[ff][cc]) continue;
                        vista[ff][cc] = true;
                        cola.add(new int[] { cc, ff });
                    }
                }
                if (suyas.size() < RIO_MINIMO || deDentro > suyas.size() * AGUA_DE_DENTRO) continue;
                // Los pasos hasta el desagüe, río arriba.
                for (int[] suya : suyas) pasos[suya[1]][suya[0]] = Integer.MAX_VALUE;
                pasos[desagua[1]][desagua[0]] = 0;
                cola.add(desagua);
                while (!cola.isEmpty()) {
                    int[] aqui = cola.poll();
                    for (int[] lado : LADOS) {
                        int cc = aqui[0] + lado[0], ff = aqui[1] + lado[1];
                        if (!dentro(cc, ff) || !cauce[ff][cc] || pasos[ff][cc] <= pasos[aqui[1]][aqui[0]] + 1) continue;
                        pasos[ff][cc] = pasos[aqui[1]][aqui[0]] + 1;
                        cola.add(new int[] { cc, ff });
                    }
                }
                for (int[] suya : suyas) {
                    if (clase[suya[1]][suya[0]] != AGUA) continue;      // bajo un puente no se pinta agua
                    rio[suya[1]][suya[0]] = true;
                    // Corre hacia donde hay menos pasos: se miran dos casillas, para que las curvas salgan suaves.
                    float dx = 0, dy = 0;
                    for (int ff = suya[1] - 2; ff <= suya[1] + 2; ff++) {
                        for (int cc = suya[0] - 2; cc <= suya[0] + 2; cc++) {
                            if (!dentro(cc, ff) || clase[ff][cc] != AGUA || pasos[ff][cc] == Integer.MAX_VALUE) continue;
                            int baja = pasos[suya[1]][suya[0]] - pasos[ff][cc];
                            dx += (cc - suya[0]) * baja;
                            dy += (ff - suya[1]) * baja;
                        }
                    }
                    float largoDelPaso = (float) Math.hypot(dx, dy);
                    corrienteX[suya[1]][suya[0]] = largoDelPaso == 0 ? 0 : dx / largoDelPaso;
                    corrienteY[suya[1]][suya[0]] = largoDelPaso == 0 ? 1 : dy / largoDelPaso;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Lo que se pregunta de cada casilla (fuera del mapa, lo del borde: allí todo es peña)
    // ------------------------------------------------------------------

    private int col(int c) {
        return Math.max(0, Math.min(columnas - 1, c));
    }

    private int fila(int f) {
        return Math.max(0, Math.min(filas - 1, f));
    }

    public int clase(int c, int f) {
        // Fuera del mapa, roca; fuera del de un interior no hay peña: sigue el suelo del borde (lo tapa el negro de sus muros).
        return dentro(c, f) ? clase[f][c] : mapa.esInterior() ? clase[fila(f)][col(c)] : ROCA;
    }

    /** La altura del suelo de la casilla, en píxeles (la de una peña, la de su pie). */
    public float altura(int c, int f) {
        return altura[fila(f)][col(c)];
    }

    public int dentroDeRoca(int c, int f) {
        return dentro(c, f) ? dentroDeRoca[f][c] : HONDO_DE_LA_ROCA;
    }

    public float frescor(int c, int f) {
        return frescor[fila(f)][col(c)];
    }

    public float pedregal(int c, int f) {
        return dentro(c, f) ? pedregal[f][c] : 1;
    }

    public float arbolado(int c, int f) {
        return arbolado[fila(f)][col(c)];
    }

    public float pisado(int c, int f) {
        return pisado[fila(f)][col(c)];
    }

    public float hondura(int c, int f) {
        return dentro(c, f) ? hondura[f][c] : 0;
    }

    public boolean esRio(int c, int f) {
        return dentro(c, f) && rio[f][c];
    }

    public float corrienteX(int c, int f) {
        return dentro(c, f) ? corrienteX[f][c] : 0;
    }

    public float corrienteY(int c, int f) {
        return dentro(c, f) ? corrienteY[f][c] : 0;
    }

    public boolean esPasadera(int c, int f) {
        return mapa.suelo(c, f) == '\'';
    }

    /** ¿Cruza un camino el agua por aquí? (Tiene camino a un lado y al otro: es donde se salta.) */
    public boolean esPaso(int c, int f) {
        if (clase(c, f) != AGUA) return false;
        return (clase(c - 1, f) == TIERRA && clase(c + 1, f) == TIERRA) || (clase(c, f - 1) == TIERRA && clase(c, f + 1) == TIERRA);
    }

    // ------------------------------------------------------------------
    // Las cuestas
    // ------------------------------------------------------------------

    /**
     * La altura del suelo en un punto de una cuesta, en píxeles: sube poco a
     * poco desde el lado que tiene más bajo hasta el que tiene más alto.
     * Fuera de una cuesta, la altura de la casilla.
     */
    public float alturaDeCuesta(int c, int f, float x, float y) {
        if (!mapa.esCuesta(c, f)) return altura(c, f);
        float abajo = (float) Math.floor(mapa.nivel(c, f)) * CifrasDeSalto.ESCALON;
        // Hacia qué lado sube: hacia la vecina más alta que no sea otra cuesta.
        float parte = 0.5f;
        if (sube(c, f - 1, c, f)) parte = 1 - (y - f * TAM) / TAM;
        else if (sube(c, f + 1, c, f)) parte = (y - f * TAM) / TAM;
        else if (sube(c - 1, f, c, f)) parte = 1 - (x - c * TAM) / TAM;
        else if (sube(c + 1, f, c, f)) parte = (x - c * TAM) / TAM;
        return abajo + Math.max(0, Math.min(1, parte)) * CifrasDeSalto.ESCALON;
    }

    private boolean sube(int c, int f, int desdeC, int desdeF) {
        return dentro(c, f) && clase[f][c] != ROCA && !mapa.esCuesta(c, f) && mapa.nivel(c, f) > mapa.nivel(desdeC, desdeF);
    }

    /** Lo que hay que subir o bajar el dibujo de quien pisa el punto (x, y) para que vaya por la pendiente de una cuesta. */
    public float ajusteDeCuesta(float x, float y) {
        int c = (int) Math.floor(x / TAM), f = (int) Math.floor(y / TAM);
        if (!mapa.esCuesta(c, f)) return 0;
        return alturaDeCuesta(c, f, x, y) - mapa.alturaDe(c, f);
    }
}
