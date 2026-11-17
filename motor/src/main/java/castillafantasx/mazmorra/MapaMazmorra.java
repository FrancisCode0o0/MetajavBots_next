package castillafantasx.mazmorra;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.combate.Arma;
import castillafantasx.datos.LectorSecciones;
import castillafantasx.seres.Ficha;

/**
 * MAPA DE UNA MAZMORRA — Lee res/mundo/mazmorras/<id>.txt.
 *
 * El mapa es una rejilla de casillas. En el archivo:
 *   .        suelo
 *   1–9      muros (cada número, una textura: sección [aspecto])
 *   E, X     puerta de entrada y puerta de salida (DECISIONES_CAVERNAS §1)
 *   O        botón de enemigos (solo en la galería de armas)
 *   A        punto de emboscada
 *   a        antorcha de pie (da luz)   c  columna   h  huesos   e  estatua   s  sarcófago
 *   v        grieta en el suelo: no se pisa; se salta (quien cae dentro, vuelve a donde saltó)
 *   d        derrumbe bajo: un montón de cascotes que cierra el paso; se salta
 *   w        agua honda: se nada (y cerca de la orilla se hace pie); las criaturas solo entran según res/datos/agua.txt
 *   letras mayúsculas del bestiario (la letra de cada ser la dice el juego: Ficha.porLetra): un enemigo
 * La sección [zonas] da nombre a las salas, para saltar a ellas desde el menú F1.
 * La sección [alturas] dice lo alto que está el techo de cada sala o tramo.
 */
public class MapaMazmorra {

    /** Las mazmorras del juego, en orden (res/datos/juego.txt, «mazmorras»). */
    public static final String[] IDS = castillafantasx.datos.ElJuego.mazmorras();

    /** Algo colocado en una casilla: un enemigo, un arma, un adorno. */
    public static final class Puesto {
        public final double x, y;         // centro de la casilla
        public final Ficha especie;     // si es un enemigo
        public final Arma arma;       // si es un arma en el suelo
        public final char adorno;         // si es un adorno ('a', 'c', 'h', 'e', 's')
        /** Cómo trata el agua este enemigo en particular (sección [agua]: nada, orilla o evita), o null: como su especie. */
        public String agua;
        /** Si lo puso el editor (sección [seres]): su tamaño y su línea tal como está en el archivo. */
        public float tam = 1;
        public String linea;
        /** «ser=»: el ser nuevo del editor (figura.SeresNuevos) como el que se dibuja, o null. */
        public String ser;

        public Puesto(double x, double y, Ficha especie, Arma arma, char adorno) {
            this.x = x;
            this.y = y;
            this.especie = especie;
            this.arma = arma;
            this.adorno = adorno;
        }
    }

    /** Una sala o zona con nombre, para saltar a ella desde el menú F1. */
    public static final class Zona {
        public final String nombre;
        public final double x, y;         // centro de su casilla

        Zona(String nombre, double x, double y) {
            this.nombre = nombre;
            this.x = x;
            this.y = y;
        }
    }

    // Luz de las antorchas: hasta dónde llega (casillas). PROVISIONAL.
    private static final double ALCANCE_ANTORCHA = 5.5;
    private static final String ADORNOS = "achesf";
    private static final String ADORNOS_QUE_ESTORBAN = "aces";
    public static final double RADIO_ADORNO = 0.28;

    public final String id, nombre, salida;
    public final boolean conEmboscada;
    public final int ancho, alto;
    private final char[][] celdas;
    /** Lo que hay en el suelo de cada casilla y pide saltar: 'v' una grieta, 'd' un derrumbe bajo, 0 nada. */
    private final char[][] salto;
    /** Agua honda ('w'): suelo que se pasa nadando (docs/encargos_viejos/ENCARGO_INTERFAZ.md). Cerca de la orilla se hace pie. */
    private final boolean[][] agua;
    /** Los derrumbes, para dibujarlos: el centro de cada casilla que tiene uno. */
    public final List<Puesto> derrumbes = new ArrayList<>();

    /** Algo en el fondo del agua, para recogerlo nadando: "energia" o el id de una cura (sección [fondo]). */
    public static final class Hallazgo {
        public final double x, y;
        public final String que;

        Hallazgo(double x, double y, String que) {
            this.x = x;
            this.y = y;
            this.que = que;
        }
    }

    public final List<Hallazgo> fondo = new ArrayList<>();

    // Aspecto: series de texturas ("muros/ladrillo" → ladrillo_0, _1…), luz y color del ambiente.
    public final String[] serieDeMuro = new String[10];
    public final String serieSuelo, serieTecho, puertaEntrada, puertaSalida;
    public final float luzAmbiente;
    public final int tinteAmbiente;       // RRGGBB

    public double entradaX, entradaY, entradaAngulo;
    public final List<Puesto> enemigos = new ArrayList<>();
    public final List<Puesto> armas = new ArrayList<>();
    public final List<Puesto> adornos = new ArrayList<>();
    /** Las antorchas que hay por el suelo, para llevárselas (sección [antorchas]: <columna> <fila>). */
    public final List<Puesto> antorchas = new ArrayList<>();
    public final List<double[]> puntosDeEmboscada = new ArrayList<>();
    /** Las zonas a las que salta el menú F1 (sección [zonas]); la primera es siempre la entrada. */
    public final List<Zona> zonas = new ArrayList<>();

    // La altura del techo, en casillas (un muro mide de ancho una casilla; El héroe, 0,72 de alto). PROVISIONALES.
    /** El techo de la mazmorra donde la sección [alturas] no dice otra cosa. */
    public static final double TECHO_SI_NO_SE_DICE = 1.6;
    /** El techo más bajo que se admite: El héroe, de pie, tiene que caber con algo de holgura. */
    public static final double TECHO_MINIMO = 0.9;
    private final double[][] techo;

    /** Luz de antorchas que llega a cada casilla, de 0 a 1. Se calcula una vez, al cargar. */
    public final float[][] luz;

    /** Lo puesto a mano con el editor (sección [colocado], docs/EDITOR.md): cosas, imágenes y manchas, en el suelo y en los muros. */
    public final castillafantasx.mundo.LoColocado colocado;

    public MapaMazmorra(String id) {
        LectorSecciones archivo = new LectorSecciones("/mundo/mazmorras/" + id + ".txt");
        this.id = id;
        colocado = new castillafantasx.mundo.LoColocado(archivo, castillafantasx.mundo.ArchivosDelMundo.archivoDeMazmorra(id));
        this.nombre = archivo.valor("mazmorra", "nombre", id);
        this.salida = archivo.valor("mazmorra", "salida", "aldea");
        this.conEmboscada = !archivo.valor("mazmorra", "emboscada", "si").equals("no");

        for (int n = 1; n <= 9; n++) {
            serieDeMuro[n] = archivo.valor("aspecto", "muro " + n, "muros/ladrillo");
        }
        serieSuelo = archivo.valor("aspecto", "suelo", "suelos/gris");
        serieTecho = archivo.valor("aspecto", "techo", "suelos/gris");
        puertaEntrada = archivo.valor("aspecto", "puerta_entrada", "decor/puerta_cerrada");
        puertaSalida = archivo.valor("aspecto", "puerta_salida", "decor/verja");
        luzAmbiente = Float.parseFloat(archivo.valor("aspecto", "luz", "0.10"));
        tinteAmbiente = Integer.parseInt(archivo.valor("aspecto", "tinte", "8090B0"), 16);

        List<String> lineas = archivo.lineas("mapa");
        alto = lineas.size();
        ancho = lineas.get(0).length();
        celdas = new char[alto][ancho];
        salto = new char[alto][ancho];
        agua = new boolean[alto][ancho];
        for (int f = 0; f < alto; f++) {
            String linea = lineas.get(f);
            if (linea.length() != ancho) {
                throw new IllegalStateException(id + ".txt: la fila " + f + " del mapa mide " + linea.length()
                        + " y debería medir " + ancho);
            }
            for (int c = 0; c < ancho; c++) {
                leerCasilla(linea.charAt(c), c, f);
            }
        }
        colocarEntrada();
        techo = leerAlturas(archivo.lineas("alturas"));

        for (String linea : archivo.lineas("armas")) {
            String[] p = linea.trim().split("\\s+");
            Arma tipo = Arma.porId(p[0]);
            if (tipo == null) throw new IllegalStateException(id + ".txt: no existe el arma «" + p[0] + "»");
            armas.add(new Puesto(Integer.parseInt(p[1]) + 0.5, Integer.parseInt(p[2]) + 0.5, null, tipo, (char) 0));
        }
        // [en_el_agua]: "<columna> <fila> <especie>", criaturas que viven en el agua; [fondo]: "<columna> <fila> energia|<cura>".
        for (String linea : archivo.lineas("en_el_agua")) {
            String[] p = linea.trim().split("\\s+");
            Ficha especie = Ficha.porId(p[2]);
            int c = Integer.parseInt(p[0]), f = Integer.parseInt(p[1]);
            if (especie == null || !especie.enMazmorra()) throw new IllegalStateException(id + ".txt: «" + p[2] + "» no es una criatura de las mazmorras");
            if (!esAgua(c, f)) throw new IllegalStateException(id + ".txt: la casilla de " + linea.trim() + " no es agua");
            enemigos.add(new Puesto(c + 0.5, f + 0.5, especie, null, (char) 0));
        }
        // [agua]: "<columna> <fila> nada|orilla|evita": cómo trata el agua el enemigo que nace en esa casilla (si no, como su especie).
        for (String linea : archivo.lineas("agua")) {
            String[] p = linea.trim().split("\\s+");
            double x = Integer.parseInt(p[0]) + 0.5, y = Integer.parseInt(p[1]) + 0.5;
            boolean hallado = false;
            for (Puesto enemigo : enemigos) {
                if (enemigo.x == x && enemigo.y == y) {
                    enemigo.agua = p[2];
                    hallado = true;
                }
            }
            if (!hallado) throw new IllegalStateException(id + ".txt [agua]: no hay ningún enemigo en " + linea.trim());
        }
        // [seres]: "<x> <y> <especie> [tam=…]": criaturas puestas con el editor, en cualquier punto (en casillas, con decimales).
        for (String linea : archivo.lineas("seres")) {
            String[] p = linea.trim().split("\\s+");
            Ficha especie = Ficha.porId(p[2]);
            if (especie == null || !especie.enMazmorra()) throw new IllegalStateException(id + ".txt [seres]: «" + p[2] + "» no es una criatura de las mazmorras");
            Puesto puesto = new Puesto(Double.parseDouble(p[0]), Double.parseDouble(p[1]), especie, null, (char) 0);
            String tam = castillafantasx.superficie.MapaSuperficie.dato(p, "tam");
            if (tam != null) puesto.tam = Float.parseFloat(tam);
            puesto.ser = castillafantasx.superficie.MapaSuperficie.dato(p, "ser");
            puesto.linea = linea.trim();
            enemigos.add(puesto);
        }
        for (String linea : archivo.lineas("fondo")) {
            String[] p = linea.trim().split("\\s+");
            int c = Integer.parseInt(p[0]), f = Integer.parseInt(p[1]);
            if (!esAgua(c, f)) throw new IllegalStateException(id + ".txt: lo del fondo en " + linea.trim() + " no está en el agua");
            if (!p[2].equals("energia") && !castillafantasx.Contenido.delJuego().existeCura(p[2])) throw new IllegalStateException(id + ".txt: no existe «" + p[2] + "» para el fondo");
            fondo.add(new Hallazgo(c + 0.5, f + 0.5, p[2]));
        }
        for (String linea : archivo.lineas("antorchas")) {
            String[] p = linea.trim().split("\\s+");
            Puesto antorcha = new Puesto(Integer.parseInt(p[0]) + 0.5, Integer.parseInt(p[1]) + 0.5, null, null, (char) 0);
            if (!cabe(antorcha.x, antorcha.y, 0.2)) throw new IllegalStateException(id + ".txt: la antorcha de " + linea.trim() + " no está en suelo libre");
            antorchas.add(antorcha);
        }
        zonas.add(new Zona("entrada", entradaX, entradaY));
        for (String linea : archivo.lineas("zonas")) {
            String[] p = linea.trim().split("\\s+", 3);
            Zona zona = new Zona(p[2], Integer.parseInt(p[0]) + 0.5, Integer.parseInt(p[1]) + 0.5);
            if (!cabe(zona.x, zona.y, Jugador.RADIO)) {
                throw new IllegalStateException(id + ".txt: la zona «" + zona.nombre + "» no está en suelo libre");
            }
            zonas.add(zona);
        }
        luz = calcularLuz();
    }

    private void leerCasilla(char letra, int c, int f) {
        celdas[f][c] = '.';
        if ((letra >= '1' && letra <= '9') || letra == 'E' || letra == 'X' || letra == 'O') {
            celdas[f][c] = letra;
        } else if (letra == 'w') {
            agua[f][c] = true;
        } else if (letra == 'v' || letra == 'd') {
            salto[f][c] = letra;
            if (letra == 'd') derrumbes.add(new Puesto(c + 0.5, f + 0.5, null, null, letra));
        } else if (letra == 'A') {
            puntosDeEmboscada.add(new double[] { c + 0.5, f + 0.5 });
        } else if (ADORNOS.indexOf(letra) >= 0) {
            adornos.add(new Puesto(c + 0.5, f + 0.5, null, null, letra));
        } else if (letra != '.') {
            Ficha especie = Ficha.porLetra(letra);
            if (especie == null) {
                throw new IllegalStateException(id + ".txt: letra desconocida «" + letra + "» en columna " + c + ", fila " + f);
            }
            enemigos.add(new Puesto(c + 0.5, f + 0.5, especie, null, (char) 0));
        }
    }

    /**
     * [alturas]: "<columna> <fila> <hasta columna> <hasta fila> <altura> <nombre>",
     * el techo de un rectángulo de casillas, en casillas. Lo de abajo manda sobre lo de arriba.
     */
    private double[][] leerAlturas(List<String> lineas) {
        double[][] alturas = new double[alto][ancho];
        for (double[] fila : alturas) java.util.Arrays.fill(fila, TECHO_SI_NO_SE_DICE);
        for (String linea : lineas) {
            String[] p = linea.trim().split("\\s+", 6);
            int c1 = Integer.parseInt(p[0]), f1 = Integer.parseInt(p[1]), c2 = Integer.parseInt(p[2]), f2 = Integer.parseInt(p[3]);
            double altura = Double.parseDouble(p[4]);
            if (altura < TECHO_MINIMO) throw new IllegalStateException(id + ".txt [alturas]: un techo de " + altura + " es más bajo que " + TECHO_MINIMO + ": " + linea.trim());
            if (c1 > c2 || f1 > f2 || c1 < 0 || f1 < 0 || c2 >= ancho || f2 >= alto) {
                throw new IllegalStateException(id + ".txt [alturas]: el rectángulo se sale del mapa: " + linea.trim());
            }
            for (int f = f1; f <= f2; f++) {
                for (int c = c1; c <= c2; c++) alturas[f][c] = altura;
            }
        }
        return alturas;
    }

    /** Un tramo de techo: casillas libres, juntas, con el techo a la misma altura. (x, y) es la casilla suya más cercana a su centro. */
    public static final class Tramo {
        public final double x, y, altura;
        public final int casillas;

        Tramo(double x, double y, double altura, int casillas) {
            this.x = x;
            this.y = y;
            this.altura = altura;
            this.casillas = casillas;
        }
    }

    private List<Tramo> tramos;

    /** Los tramos de techo de la mazmorra (para el plano y el modo pájaro: cada sala dice lo alto que tiene el techo). */
    public List<Tramo> tramosDeTecho() {
        if (tramos != null) return tramos;
        tramos = new ArrayList<>();
        boolean[][] visto = new boolean[alto][ancho];
        java.util.ArrayDeque<int[]> cola = new java.util.ArrayDeque<>();
        for (int f0 = 0; f0 < alto; f0++) {
            for (int c0 = 0; c0 < ancho; c0++) {
                if (visto[f0][c0] || esMuro(c0, f0)) continue;
                // Se extiende por las casillas libres vecinas con el mismo techo.
                double altura = techo[f0][c0];
                List<int[]> suyas = new ArrayList<>();
                visto[f0][c0] = true;
                cola.add(new int[] { c0, f0 });
                double sumaC = 0, sumaF = 0;
                while (!cola.isEmpty()) {
                    int[] casilla = cola.poll();
                    suyas.add(casilla);
                    sumaC += casilla[0];
                    sumaF += casilla[1];
                    int[][] vecinas = { { casilla[0] + 1, casilla[1] }, { casilla[0] - 1, casilla[1] }, { casilla[0], casilla[1] + 1 }, { casilla[0], casilla[1] - 1 } };
                    for (int[] v : vecinas) {
                        if (v[0] < 0 || v[1] < 0 || v[0] >= ancho || v[1] >= alto || visto[v[1]][v[0]] || esMuro(v[0], v[1]) || techo[v[1]][v[0]] != altura) continue;
                        visto[v[1]][v[0]] = true;
                        cola.add(v);
                    }
                }
                double centroC = sumaC / suyas.size(), centroF = sumaF / suyas.size();
                int[] mejor = suyas.get(0);
                for (int[] casilla : suyas) {
                    if (Math.hypot(casilla[0] - centroC, casilla[1] - centroF) < Math.hypot(mejor[0] - centroC, mejor[1] - centroF)) mejor = casilla;
                }
                tramos.add(new Tramo(mejor[0] + 0.5, mejor[1] + 0.5, altura, suyas.size()));
            }
        }
        return tramos;
    }

    /** Lo alto que está el techo en esa casilla, en casillas. Fuera del mapa, lo de siempre. */
    public double techo(int c, int f) {
        if (c < 0 || f < 0 || c >= ancho || f >= alto) return TECHO_SI_NO_SE_DICE;
        return techo[f][c];
    }

    public double techo(double x, double y) {
        return techo((int) Math.floor(x), (int) Math.floor(y));
    }

    /** El héroe aparece en la casilla libre pegada a la puerta E, mirando hacia dentro. */
    private void colocarEntrada() {
        int[][] lados = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int f = 0; f < alto; f++) {
            for (int c = 0; c < ancho; c++) {
                if (celdas[f][c] != 'E') continue;
                for (int[] l : lados) {
                    if (!esMuro(c + l[0], f + l[1])) {
                        entradaX = c + l[0] + 0.5;
                        entradaY = f + l[1] + 0.5;
                        entradaAngulo = Math.atan2(l[1], l[0]);
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException(id + ".txt: falta la puerta de entrada E");
    }

    /** Qué hay en una casilla: '.', un muro '1'–'9', 'E', 'X' u 'O'. Fuera del mapa, muro. */
    public char celda(int c, int f) {
        if (c < 0 || f < 0 || c >= ancho || f >= alto) return '1';
        return celdas[f][c];
    }

    public boolean esMuro(int c, int f) {
        return celda(c, f) != '.';
    }

    public boolean esMuro(double x, double y) {
        return esMuro((int) Math.floor(x), (int) Math.floor(y));
    }

    /** ¿Hay en esa casilla una grieta? */
    public boolean esGrieta(int c, int f) {
        return c >= 0 && f >= 0 && c < ancho && f < alto && salto[f][c] == 'v';
    }

    public boolean esGrieta(double x, double y) {
        return esGrieta((int) Math.floor(x), (int) Math.floor(y));
    }

    /** ¿Hay en esa casilla algo que solo se pasa saltando (una grieta o un derrumbe bajo)? */
    public boolean seSalta(int c, int f) {
        return c >= 0 && f >= 0 && c < ancho && f < alto && salto[f][c] != 0;
    }

    private boolean seSalta(double x, double y) {
        return seSalta((int) Math.floor(x), (int) Math.floor(y));
    }

    /** ¿Estorba el paso ese adorno? (Las antorchas de pie, las columnas, las estatuas y los sarcófagos.) */
    public boolean estorba(Puesto a) {
        return a.adorno != 0 && ADORNOS_QUE_ESTORBAN.indexOf(a.adorno) >= 0;
    }

    /** ¿Cabe un cuerpo redondo de ese radio con el centro en (x, y)? Mira los muros y los adornos que estorban. */
    public boolean cabe(double x, double y, double radio) {
        return cabe(x, y, radio, false);
    }

    /** Lo mismo; con 'saltando', las grietas y los derrumbes bajos no cuentan: se pasa por encima. El agua cierra el paso. */
    public boolean cabe(double x, double y, double radio, boolean saltando) {
        return cabe(x, y, radio, saltando, castillafantasx.agua.CifrasDeAgua.NO_ENTRA);
    }

    /**
     * Lo mismo, para quien entra al agua: 'hondoQueAdmite' es la hondura
     * (píxeles, como en la superficie) hasta la que se mete: NO_ENTRA, HACE_PIE
     * (solo la orilla) o NADA (cualquier agua: El héroe). Ver castillafantasx.agua.CifrasDeAgua.
     */
    public boolean cabe(double x, double y, double radio, boolean saltando, float hondoQueAdmite) {
        if (!saltando && (seSalta(x - radio, y - radio) || seSalta(x + radio, y - radio)
                || seSalta(x - radio, y + radio) || seSalta(x + radio, y + radio))) {
            return false;
        }
        if (hondoQueAdmite < castillafantasx.agua.CifrasDeAgua.NADA && (aguaQueNoAdmite(x - radio, y - radio, hondoQueAdmite)
                || aguaQueNoAdmite(x + radio, y - radio, hondoQueAdmite) || aguaQueNoAdmite(x - radio, y + radio, hondoQueAdmite)
                || aguaQueNoAdmite(x + radio, y + radio, hondoQueAdmite))) {
            return false;
        }
        if (esMuro(x - radio, y - radio) || esMuro(x + radio, y - radio)
                || esMuro(x - radio, y + radio) || esMuro(x + radio, y + radio)) {
            return false;
        }
        for (Puesto a : adornos) {
            if (ADORNOS_QUE_ESTORBAN.indexOf(a.adorno) < 0) continue;
            double dx = a.x - x, dy = a.y - y, junto = radio + RADIO_ADORNO;
            if (dx * dx + dy * dy < junto * junto) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // El agua honda (docs/encargos_viejos/ENCARGO_INTERFAZ.md): se mide como en la superficie, en píxeles de dibujo
    // ------------------------------------------------------------------

    /** Hasta cuántas casillas de distancia se busca la orilla (más lejos, el agua ya es todo lo honda que puede ser). */
    private static final int CASILLAS_HASTA_LA_ORILLA = 2;

    /** ¿Es agua esa casilla? */
    public boolean esAgua(int c, int f) {
        return c >= 0 && f >= 0 && c < ancho && f < alto && agua[f][c];
    }

    public boolean esAgua(double x, double y) {
        return esAgua((int) Math.floor(x), (int) Math.floor(y));
    }

    /** ¿Hay agua más honda de la que admite quien pasa por ahí? */
    private boolean aguaQueNoAdmite(double x, double y, float hondoQueAdmite) {
        return esAgua(x, y) && hondoEn(x, y) > hondoQueAdmite;
    }

    /**
     * La hondura del agua en un punto (x, y en casillas), en píxeles como los
     * de la superficie: 0 fuera del agua y, dentro, más cuanto más lejos
     * queda la orilla. Con ella se sabe si ahí se vadea o se nada.
     */
    public float hondoEn(double x, double y) {
        int col = (int) Math.floor(x), fila = (int) Math.floor(y);
        if (!esAgua(col, fila)) return 0;
        double cerca = CASILLAS_HASTA_LA_ORILLA;
        for (int f = fila - CASILLAS_HASTA_LA_ORILLA; f <= fila + CASILLAS_HASTA_LA_ORILLA; f++) {
            for (int c = col - CASILLAS_HASTA_LA_ORILLA; c <= col + CASILLAS_HASTA_LA_ORILLA; c++) {
                if (esAgua(c, f)) continue;
                // La distancia del punto al cuadro de esa casilla de orilla (un muro también es orilla).
                double dx = Math.max(0, Math.max(c - x, x - (c + 1))), dy = Math.max(0, Math.max(f - y, y - (f + 1)));
                cerca = Math.min(cerca, Math.hypot(dx, dy));
            }
        }
        return castillafantasx.agua.CifrasDeAgua.hondura((float) (cerca * castillafantasx.agua.CifrasDeAgua.CASILLA_DE_MAZMORRA));
    }

    /** La hondura del agua en el centro de una casilla (0 si no es agua). */
    public float hondoDeCasilla(int c, int f) {
        return hondoEn(c + 0.5, f + 0.5);
    }

    /** ¿Se ven dos puntos, sin muro en medio? Avanza por la línea a pasos cortos. */
    public boolean seVen(double x0, double y0, double x1, double y1) {
        double d = Math.hypot(x1 - x0, y1 - y0);
        int pasos = (int) (d / 0.1) + 1;
        for (int i = 1; i < pasos; i++) {
            double t = i / (double) pasos;
            if (esMuro(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t)) return false;
        }
        return true;
    }

    // --- La luz de color de las cosas colocadas que la dan (res/datos/luces.txt): calderos, fuegos, charcas, reliquias ---
    private float[][][] luzDeColor;         // por casilla: {rojo, verde, azul}, de 0 a 1 (o algo más)
    private int cambiosDeLaLuz = -1;
    private boolean lumbresDeLaLuz;

    /**
     * Calcula (si ha cambiado lo colocado) la luz de color que llega a cada
     * casilla desde las cosas que la dan. 'conLumbres': las que alguien
     * enciende (velas, braseros) están encendidas; en una mazmorra abandonada, no.
     */
    public void prepararLuzDeColor(boolean conLumbres) {
        if (luzDeColor != null && cambiosDeLaLuz == colocado.cambios && lumbresDeLaLuz == conLumbres) return;
        cambiosDeLaLuz = colocado.cambios;
        lumbresDeLaLuz = conLumbres;
        luzDeColor = new float[alto][ancho][3];
        for (castillafantasx.mundo.Colocado pieza : colocado.piezas) {
            if (pieza.clase != castillafantasx.mundo.Colocado.Clase.COSA) continue;
            castillafantasx.interiores.LucesDeCosas.Luz luzSuya = castillafantasx.interiores.LucesDeCosas.de(pieza.que);
            if (luzSuya == null || luzSuya.cuando == castillafantasx.interiores.LucesDeCosas.Cuando.DIA) continue;
            if (luzSuya.cuando == castillafantasx.interiores.LucesDeCosas.Cuando.LUMBRE && !conLumbres) continue;
            // Lo que alumbra en la superficie, en píxeles, pasa a casillas (una casilla mide 32).
            double alcance = luzSuya.radio * pieza.tam / castillafantasx.superficie.MapaSuperficie.TAM;
            float r = ((luzSuya.color >> 16) & 255) / 255f, g = ((luzSuya.color >> 8) & 255) / 255f, b = (luzSuya.color & 255) / 255f;
            // Una cosa colgada de un muro alumbra desde la casilla de suelo que tiene delante.
            double x = pieza.x, y = pieza.y;
            if (pieza.cara != null) {
                x = Math.floor(x) + 0.5 + (pieza.cara.equals("e") ? 1 : pieza.cara.equals("o") ? -1 : 0);
                y = Math.floor(y) + 0.5 + (pieza.cara.equals("s") ? 1 : pieza.cara.equals("n") ? -1 : 0);
            }
            for (int f = Math.max(0, (int) (y - alcance)); f <= Math.min(alto - 1, (int) (y + alcance)); f++) {
                for (int c = Math.max(0, (int) (x - alcance)); c <= Math.min(ancho - 1, (int) (x + alcance)); c++) {
                    double d = Math.hypot(x - (c + 0.5), y - (f + 0.5));
                    if (d >= alcance || (!esMuro(c, f) && !seVen(x, y, c + 0.5, f + 0.5))) continue;
                    if (esMuro(c, f) && d > 1.5) continue;         // a un muro solo le llega de lo que tiene al lado
                    float cerca = (float) (1 - d / alcance);
                    cerca *= cerca;
                    luzDeColor[f][c][0] += r * cerca;
                    luzDeColor[f][c][1] += g * cerca;
                    luzDeColor[f][c][2] += b * cerca;
                }
            }
        }
    }

    /** Deja en 'rgb' la luz de color que llega a un punto (mezclando las cuatro casillas vecinas). */
    public void luzDeColorEn(double x, double y, boolean conLumbres, float[] rgb) {
        prepararLuzDeColor(conLumbres);
        double gx = x - 0.5, gy = y - 0.5;
        int c0 = (int) Math.floor(gx), f0 = (int) Math.floor(gy);
        float tx = (float) (gx - c0), ty = (float) (gy - f0);
        for (int k = 0; k < 3; k++) {
            float a = colorDeCelda(c0, f0, k), b = colorDeCelda(c0 + 1, f0, k), c = colorDeCelda(c0, f0 + 1, k), d = colorDeCelda(c0 + 1, f0 + 1, k);
            float arriba = a + (b - a) * tx, abajo = c + (d - c) * tx;
            rgb[k] = arriba + (abajo - arriba) * ty;
        }
    }

    private float colorDeCelda(int c, int f, int k) {
        if (c < 0 || f < 0 || c >= ancho || f >= alto) return 0;
        return luzDeColor[f][c][k];
    }

    /** La luz de cada casilla: suma de las antorchas que la ven, más fuerte cuanto más cerca. */
    private float[][] calcularLuz() {
        float[][] l = new float[alto][ancho];
        for (int f = 0; f < alto; f++) {
            for (int c = 0; c < ancho; c++) {
                if (esMuro(c, f)) continue;
                double suma = 0;
                for (Puesto a : adornos) {
                    if (a.adorno != 'a') continue;
                    double d = Math.hypot(a.x - (c + 0.5), a.y - (f + 0.5));
                    if (d < ALCANCE_ANTORCHA && seVen(a.x, a.y, c + 0.5, f + 0.5)) {
                        double cerca = 1 - d / ALCANCE_ANTORCHA;
                        suma += cerca * cerca;
                    }
                }
                l[f][c] = (float) Math.min(1, suma);
            }
        }
        // A cada muro le llega la luz de la casilla de suelo más iluminada que tenga al lado.
        for (int f = 0; f < alto; f++) {
            for (int c = 0; c < ancho; c++) {
                if (!esMuro(c, f)) continue;
                float mejor = 0;
                if (!esMuro(c + 1, f)) mejor = Math.max(mejor, l[f][c + 1]);
                if (!esMuro(c - 1, f)) mejor = Math.max(mejor, l[f][c - 1]);
                if (!esMuro(c, f + 1)) mejor = Math.max(mejor, l[f + 1][c]);
                if (!esMuro(c, f - 1)) mejor = Math.max(mejor, l[f - 1][c]);
                l[f][c] = mejor;
            }
        }
        return l;
    }

    /** La luz de antorchas en un punto cualquiera, mezclando suavemente las cuatro casillas vecinas. */
    public float luzEn(double x, double y) {
        double gx = x - 0.5, gy = y - 0.5;
        int c0 = (int) Math.floor(gx), f0 = (int) Math.floor(gy);
        float tx = (float) (gx - c0), ty = (float) (gy - f0);
        float a = luzDeCelda(c0, f0), b = luzDeCelda(c0 + 1, f0);
        float c = luzDeCelda(c0, f0 + 1), d = luzDeCelda(c0 + 1, f0 + 1);
        float arriba = a + (b - a) * tx, abajo = c + (d - c) * tx;
        return arriba + (abajo - arriba) * ty;
    }

    public float luzDeCelda(int c, int f) {
        if (c < 0 || f < 0 || c >= ancho || f >= alto) return 0;
        return luz[f][c];
    }
}
