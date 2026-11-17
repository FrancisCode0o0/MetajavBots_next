package castillafantasx.paisaje;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import castillafantasx.superficie.MapaSuperficie;

/**
 * EDIFICIOS — Qué edificio es cada mancha de «h», «d» y «n» del mapa
 * (docs/encargos_viejos/ENCARGO_MAPA.md). En el mapa todos se escriben igual; aquí se junta
 * cada uno (las casillas que se tocan), se mira dónde tiene la puerta y las
 * ventanas, y se le da su tipo: el que diga la sección [edificios] del mapa
 * o, si no dice nada, una casa según su tamaño (las pequeñas, de adobe; las
 * medianas, de tapial o de piedra; las grandes, de entramado y dos plantas).
 *
 * Con esto, el juego dibuja cada edificio entero, una
 * vez, al cargar. Aquí solo se decide; lo que estorba es lo de siempre.
 */
public final class Edificios {

    private static final String CASAS = "hdn";          // las letras de edificio del mapa
    /** Los tipos que sabe dibujar el TallerDeEdificios. */
    public static final String[] TIPOS = { "iglesia", "ermita", "torre_del_homenaje", "cubo", "torre_fuerte", "casa_palacio", "molino",
        "fragua", "horno", "meson", "teneria", "pajar", "monasterio", "iglesia_del_monasterio", "casa_adobe", "casa_tapial", "casa_piedra", "casa_entramado" };

    /** Un edificio: un rectángulo de casillas, con su tipo y con las columnas de su puerta y de sus ventanas. */
    public static final class Edificio {
        public int col0, fila0, col1, fila1;        // de la esquina de arriba a la izquierda a la de abajo a la derecha
        public String tipo;
        public final List<Integer> puertas = new ArrayList<>();     // columnas, contadas desde col0
        public final List<Integer> ventanas = new ArrayList<>();
        public int semilla;                         // un número fijo de cada edificio: de él sale lo que varía
        /** El nombre de su dibujo en el Almacén. */
        public String dibujo() {
            return "edificio/" + col0 + "_" + fila0;
        }
        public int ancho() {
            return col1 - col0 + 1;
        }
        public int fondo() {
            return fila1 - fila0 + 1;
        }
    }

    public final List<Edificio> todos = new ArrayList<>();
    private final Edificio[][] deCasilla;
    private final boolean[][] almenado;     // los muros «M» que llevan almenas

    public Edificios(MapaSuperficie mapa) {
        deCasilla = new Edificio[mapa.filas][mapa.columnas];
        almenado = new boolean[mapa.filas][mapa.columnas];
        // Cada mancha de casillas de edificio que se tocan…
        int[][] mancha = new int[mapa.filas][mapa.columnas];
        int siguiente = 1;
        for (int f = 0; f < mapa.filas; f++) {
            for (int c = 0; c < mapa.columnas; c++) {
                if (!esCasa(mapa, c, f) || mancha[f][c] != 0) continue;
                llenar(mapa, mancha, c, f, siguiente, CASAS);
                siguiente++;
            }
        }
        // …se parte en rectángulos (casi siempre es uno solo): columnas seguidas que empiezan y acaban en las mismas filas.
        for (int c = 0; c < mapa.columnas; c++) {
            for (int f = 0; f < mapa.filas; f++) {
                if (mancha[f][c] == 0 || deCasilla[f][c] != null) continue;
                int abajo = f;
                while (mancha[abajo + 1][c] == mancha[f][c]) abajo++;
                Edificio e = new Edificio();
                e.col0 = e.col1 = c;
                e.fila0 = f;
                e.fila1 = abajo;
                while (mismaTira(mancha, e.col1 + 1, f, abajo, mancha[f][c])) e.col1++;
                e.semilla = Ruido.entero(e.col0, e.fila0, 901) & 0x7FFFFFFF;
                for (int cc = e.col0; cc <= e.col1; cc++) {
                    for (int ff = e.fila0; ff <= e.fila1; ff++) deCasilla[ff][cc] = e;
                    if (mapa.suelo(cc, e.fila1) == 'd') e.puertas.add(cc - e.col0);
                    if (mapa.suelo(cc, e.fila1) == 'n') e.ventanas.add(cc - e.col0);
                }
                todos.add(e);
            }
        }
        // Lo que dice el mapa de cada uno.
        for (MapaSuperficie.Senalado s : mapa.senalados) {
            if (s.tipo.equals("muralla")) {
                if (mapa.suelo(s.columna, s.fila) != 'M') throw new IllegalStateException("superficie.txt [edificios]: en " + s.columna + ", " + s.fila + " no hay un muro");
                int[][] muro = new int[mapa.filas][mapa.columnas];
                llenar(mapa, muro, s.columna, s.fila, 1, "M");
                for (int f = 0; f < mapa.filas; f++) {
                    for (int c = 0; c < mapa.columnas; c++) almenado[f][c] |= muro[f][c] == 1;
                }
                continue;
            }
            Edificio e = en(s.columna, s.fila);
            if (e == null) throw new IllegalStateException("superficie.txt [edificios]: en " + s.columna + ", " + s.fila + " no hay un edificio");
            if (!java.util.Arrays.asList(TIPOS).contains(s.tipo)) {
                throw new IllegalStateException("superficie.txt [edificios]: no hay edificios de tipo «" + s.tipo + "»");
            }
            e.tipo = s.tipo;
        }
        for (Edificio e : todos) {
            if (e.tipo == null) e.tipo = casaSegunSuTamano(e);
        }
    }

    private static boolean esCasa(MapaSuperficie mapa, int c, int f) {
        return CASAS.indexOf(mapa.suelo(c, f)) >= 0;
    }

    /** Marca con 'numero' todas las casillas de esas letras que se tocan con (c, f). */
    private static void llenar(MapaSuperficie mapa, int[][] mancha, int c, int f, int numero, String letras) {
        ArrayDeque<int[]> cola = new ArrayDeque<>();
        cola.add(new int[] { c, f });
        mancha[f][c] = numero;
        int[][] lados = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
        while (!cola.isEmpty()) {
            int[] aqui = cola.poll();
            for (int[] lado : lados) {
                int cc = aqui[0] + lado[0], ff = aqui[1] + lado[1];
                if (cc < 0 || ff < 0 || cc >= mapa.columnas || ff >= mapa.filas) continue;
                if (mancha[ff][cc] != 0 || letras.indexOf(mapa.suelo(cc, ff)) < 0) continue;
                mancha[ff][cc] = numero;
                cola.add(new int[] { cc, ff });
            }
        }
    }

    /** ¿Es la columna c una tira de esa mancha que va justo de la fila 'arriba' a la fila 'abajo'? */
    private boolean mismaTira(int[][] mancha, int c, int arriba, int abajo, int numero) {
        if (c >= mancha[0].length || deCasilla[arriba][c] != null) return false;
        for (int f = arriba; f <= abajo; f++) {
            if (mancha[f][c] != numero) return false;
        }
        return mancha[arriba - 1][c] != numero && mancha[abajo + 1][c] != numero;
    }

    /** La casa que toca a un edificio del que el mapa no dice nada: según lo que mide. */
    // TODO-FRAN: quién vive en cada casa (ahora, cuanto más grande, más rica).
    private static String casaSegunSuTamano(Edificio e) {
        int casillas = e.ancho() * e.fondo();
        boolean par = e.semilla % 2 == 0;
        if (e.fondo() < 3 || casillas <= 9) return "casa_adobe";
        if (casillas <= 15) return par ? "casa_tapial" : "casa_adobe";
        if (casillas <= 20) return par ? "casa_piedra" : "casa_tapial";
        return "casa_entramado";
    }

    /** El edificio de esa casilla, o null. */
    public Edificio en(int c, int f) {
        if (c < 0 || f < 0 || f >= deCasilla.length || c >= deCasilla[0].length) return null;
        return deCasilla[f][c];
    }

    /** ¿Lleva almenas el muro de esa casilla? */
    public boolean esMuralla(int c, int f) {
        return c >= 0 && f >= 0 && f < almenado.length && c < almenado[0].length && almenado[f][c];
    }
}
