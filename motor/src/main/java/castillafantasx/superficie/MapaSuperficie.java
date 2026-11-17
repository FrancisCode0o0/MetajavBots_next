package castillafantasx.superficie;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.datos.LectorSecciones;

/**
 * MAPA DE LA SUPERFICIE — Lee res/mundo/superficie.txt (ahí está la leyenda)
 * y lo guarda en dos capas: el SUELO de cada casilla (hierba, camino, muro…)
 * y la COSA fija que tiene encima (árbol, farol, cruz…), si la tiene.
 */
public class MapaSuperficie {

    /** Lado de una casilla, en píxeles. */
    public static final int TAM = 32;

    private static final String SUELOS = ".,:_=x;'>{}";     // se pisan
    private static final String MACIZOS = "~Mm^&hdnkq/";    // no se pisan: agua, muros, roca, casas, cobertizos y grietas
    private static final String COSAS = "ty|\"+licberpof*%@auz-"; // van encima de un suelo
    private static final String SE_SALTAN = "~/";           // macizos sin bulto: saltando se pasa por encima (y quien cae en ellos, vuelve atrás)
    private static final String ARBOLES = "ty|";            // juntos (o pegados a un muro) cierran el paso entre ellos
    /** El pie de un árbol gigante del bosque (docs/encargos_viejos/ENCARGO_BOSQUE.md): dos o tres seguidos en una fila son un solo árbol. */
    public static final char GIGANTE = '|';
    private static final String CERCAS = "%@a";             // cerca de palos, pared de piedra seca y cerca de tablas: las vecinas se unen
    /** Lo alto, que estorba también a quien vuela y no se salta nunca: [muros_altos] de res/datos/choques.txt. */
    private static final String ALTOS = ChoquesDeCosas.murosAltos();
    private static final String SUELTOS = "sgwj$v";         // seres y cosas que se recogen

    /** Una marca del mapa: una letra con su definición de [marcas]. */
    public static final class Marca {
        public final int columna, fila;
        public final String tipo;         // inicio, llegada, persona, objeto
        public final String[] palabras;   // lo que va antes del primer '|', separado por espacios
        public final String[] textos;     // lo que va después de cada '|'
        /** Su letra en el mapa (el editor la quita de él al pasar a coordenadas lo que marca). Si es 0, viene de una línea de [objetos]. */
        public char letra;
        /** Su línea de [objetos], si viene de una. */
        public String linea;

        public Marca(int columna, int fila, String[] palabras, String[] textos) {
            this.columna = columna;
            this.fila = fila;
            this.tipo = palabras[0];
            this.palabras = palabras;
            this.textos = textos;
        }
    }

    /** Un ser o una cosa suelta dibujados en el mapa con su letra. */
    public static final class Suelto {
        public final char letra;
        public final int columna, fila;

        Suelto(char letra, int columna, int fila) {
            this.letra = letra;
            this.columna = columna;
            this.fila = fila;
        }
    }

    /** De las palabras de una línea de [seres] o [gente], la que dice «agua=…» (lo que va detrás), o null si no hay. */
    private static String tratoDelAgua(String[] palabras) {
        return dato(palabras, "agua");
    }

    /** De las palabras de una línea, lo que va detrás de «clave=», o null si no está. */
    public static String dato(String[] palabras, String clave) {
        for (int i = 3; i < palabras.length; i++) {
            if (palabras[i].startsWith(clave + "=")) return palabras[i].substring(clave.length() + 1);
        }
        return null;
    }

    /** Apunta en un puesto lo demás que dice su línea (id, dueño, tamaño y hacia dónde mira) y de dónde viene. */
    private static void completar(Puesto puesto, String[] palabras, String seccion, String linea) {
        puesto.agua = tratoDelAgua(palabras);
        puesto.id = dato(palabras, "id");
        puesto.dueno = dato(palabras, "dueno");
        puesto.ser = dato(palabras, "ser");
        if (dato(palabras, "tam") != null) puesto.tam = Float.parseFloat(dato(palabras, "tam"));
        if (dato(palabras, "mira") != null) puesto.mira = Integer.parseInt(dato(palabras, "mira"));
        puesto.seccion = seccion;
        puesto.linea = linea.trim();
    }

    /** Un ser colocado por coordenadas en [seres] o [gente] (para no gastar letras del mapa). */
    public static final class Puesto {
        public final int columna, fila;
        public final castillafantasx.seres.Ficha especie;   // si es una criatura
        public final String aspecto;                        // si es una persona
        public final String[] textos;
        /** Cómo trata el agua este en particular («nada», «orilla», «evita»), o null: como los de su especie o su aspecto. */
        public String agua;
        /** Lo demás que puede decir su línea (lo pone el editor): «id=» su nombre propio, «dueno=» su dueño, «tam=» su tamaño y «mira=» hacia dónde mira (grados). */
        public String id, dueno;
        /** «ser=»: el ser nuevo del editor (figura.SeresNuevos) como el que se dibuja esta criatura, o null. */
        public String ser;
        public float tam = 1;
        public int mira = -1;
        /** De qué sección es («seres» o «gente») y su línea tal como está en el archivo: con ellas el editor la encuentra para cambiarla. */
        public String seccion, linea;

        public Puesto(int columna, int fila, castillafantasx.seres.Ficha especie, String aspecto, String[] textos) {
            this.columna = columna;
            this.fila = fila;
            this.especie = especie;
            this.aspecto = aspecto;
            this.textos = textos;
        }
    }

    /** Un edificio del que el mapa dice qué es (sección [edificios]): una casilla suya y su tipo. */
    public static final class Senalado {
        public final int columna, fila;
        public final String tipo;

        Senalado(int columna, int fila, String tipo) {
            this.columna = columna;
            this.fila = fila;
            this.tipo = tipo;
        }
    }

    /** Una zona con nombre y con su luz ambiente. */
    public static final class Lugar {
        public final int col0, fila0, col1, fila1;
        public final int luz;             // RRGGBB
        public final String nombre;

        Lugar(int col0, int fila0, int col1, int fila1, int luz, String nombre) {
            this.col0 = col0;
            this.fila0 = fila0;
            this.col1 = col1;
            this.fila1 = fila1;
            this.luz = luz;
            this.nombre = nombre;
        }
    }

    /** Un punto de luz fijo (farol, velas, ventana, monolito), en píxeles del mapa. */
    public static final class Luz {
        public final float x, y, radio;
        public final int color;           // RRGGBB
        public final boolean parpadea;
        /** Si la da una cosa del mapa (un farol, unas velas), su casilla: al quitar la cosa se quita su luz. Si no, -1. */
        public int columna = -1, fila = -1;

        Luz(float x, float y, float radio, int color, boolean parpadea) {
            this.x = x;
            this.y = y;
            this.radio = radio;
            this.color = color;
            this.parpadea = parpadea;
        }
    }

    // Luces fijas. PROVISIONAL: radios y colores.
    private static final int LUZ_CALIDA = 0xFFB060, LUZ_EXTRANA = 0x50E0C8;
    private static final float RADIO_FAROL = 130, RADIO_VELAS = 70, RADIO_VENTANA = 80, RADIO_MONOLITO = 110;
    private static final float RADIO_CANDIL = 120;          // el candil de aceite alumbra menos que un farol

    public final int columnas, filas;
    private final char[][] suelo;
    private final char[][] cosa;
    /** La altura del suelo de cada casilla, en niveles (sección [alturas]): 0 = el llano; las cuestas valen medio nivel. */
    private final float[][] nivel;
    /** Cuánto cambia la altura al pasar andando de una casilla a la de al lado, como mucho (más pide saltar). */
    public static final float PASO_DE_NIVEL = 0.5f;
    /** La base con la que estorba la cosa de cada casilla: {x0, y0, x1, y1} en píxeles del mapa, o null. */
    private final float[][][] base;
    public final List<Marca> marcas = new ArrayList<>();
    public final List<Suelto> sueltos = new ArrayList<>();
    /** Qué cura es la «v» de cada casilla ("columna,fila" → id de la cura), según la sección [curas] del mapa. */
    private final java.util.Map<String, String> curasSueltas = new java.util.HashMap<>();
    /** Lo que hay en el fondo del agua (sección [agua]): el aspecto dice qué es ("moneda" o el id de una cura). */
    public final List<Puesto> enElAgua = new ArrayList<>();

    /** El id de la cura suelta («v») de esa casilla: la que diga la sección [curas] o, si no dice nada, pan. */
    public String curaEn(int columna, int fila) {
        return curasSueltas.getOrDefault(columna + "," + fila, castillafantasx.Contenido.delJuego().curaDeFabrica());
    }
    public final List<Lugar> lugares = new ArrayList<>();
    public final List<Luz> luces = new ArrayList<>();
    public final List<Puesto> puestos = new ArrayList<>();
    public final List<Senalado> senalados = new ArrayList<>();
    /** Un número que resume el terreno: dos mapas con el mismo tienen el mismo suelo, las mismas cosas y las mismas alturas. */
    public int huella;

    /** El nombre de la llegada por la que se entra a un interior: su marca «llegada entrada». */
    public static final String ENTRADA = "entrada";
    /** El mapa de la comarca (res/mundo/superficie.txt). */
    public static final String COMARCA = "superficie";
    /** El mapa del bosque (res/mundo/bosque.txt): linda con el borde sur de la comarca. */
    public static final String BOSQUE = "bosque";

    /** Qué mapa es: el nombre de su archivo de res/mundo, sin el «.txt». */
    public final String id;
    /** Las lindes (sección [lindes]): por cada borde ("norte", "sur", "este", "oeste"), el mapa al que se pasa al llegar a él. */
    public final java.util.Map<String, String> lindes = new java.util.HashMap<>();

    /** Los árboles gigantes: de cada uno, la columna de su primera casilla, su fila y cuántas casillas mide su pie. */
    public final List<int[]> gigantes = new ArrayList<>();
    /** Lo que crece en cada lugar (sección [flora]): nombre del lugar y su regla, tal como está escrita. */
    public final java.util.Map<String, String> flora = new java.util.HashMap<>();

    /** Un escondite (sección [escondites]): un sitio preparado para una planta, una hierba rara o un animal. */
    public static final class Escondite {
        public final int columna, fila;
        public final String clase, nota;

        Escondite(int columna, int fila, String clase, String nota) {
            this.columna = columna;
            this.fila = fila;
            this.clase = clase;
            this.nota = nota;
        }
    }

    // TODO-FRAN: qué planta, hierba rara o animal hay en cada escondite del bosque (hoy solo se guarda el sitio).
    public final List<Escondite> escondites = new ArrayList<>();

    public MapaSuperficie() {
        this(COMARCA);
    }

    /** Lo puesto a mano con el editor (sección [colocado], docs/EDITOR.md): cosas, imágenes, manchas, trazos y letreros. */
    public final castillafantasx.mundo.LoColocado colocado;

    /**
     * Lo propio de un interior (sección [interior], docs/INTERIORES.md): el mapa de dentro de un edificio.
     * Los mapas al aire libre no la tienen.
     */
    public static final class Interior {
        /** Cómo se llama (sale de rótulo al entrar). */
        public String nombre = "";
        /** El mapa de fuera y la casilla de su puerta: al salir, el héroe queda delante de ella. Sin puerta (-1): una pieza de la cantera. */
        public String fuera = COMARCA;
        public int columnaDeLaPuerta = -1, filaDeLaPuerta = -1;
        /** Cuánta luz del día entra por puertas y ventanas (0 = nada, 1 = como en la calle) y el color de su oscuridad (RRGGBB). */
        public float luzDeDia = 0.5f;
        public int oscuridad = 0x0C0A12;
        /** Los sitios donde se encuentra de noche a quien se ha recogido en esta casa (sección [camas]): {columna, fila}. */
        public final List<int[]> camas = new ArrayList<>();
        /** Cómo es su umbral en la escena de entrada (casa, iglesia, palacio…): la pinta el juego. */
        public String umbral = "casa";
    }

    /** Lo propio de este mapa si es un interior; si no, null. */
    public final Interior interior;
    /** Sección [aspecto]: cómo se ve en este mapa cada letra de suelo («suelo . = tarima») y de muro («muro M = encalado»). */
    public final java.util.Map<Character, String> aspectoDeSuelo = new java.util.HashMap<>(), aspectoDeMuro = new java.util.HashMap<>();

    /** ¿Es el mapa de dentro de un edificio? */
    public boolean esInterior() {
        return interior != null;
    }

    private Interior leerInterior(LectorSecciones archivo) {
        for (String linea : archivo.lineas("aspecto")) {
            int igual = linea.indexOf('=');
            String[] p = linea.substring(0, Math.max(0, igual)).trim().split("\\s+");
            if (igual < 0 || p.length != 2 || p[1].length() != 1) throw new IllegalStateException(id + ".txt [aspecto]: no se entiende «" + linea.trim() + "»");
            (p[0].equals("muro") ? aspectoDeMuro : aspectoDeSuelo).put(p[1].charAt(0), linea.substring(igual + 1).trim());
        }
        if (!archivo.secciones().contains("interior")) return null;
        Interior i = new Interior();
        i.nombre = archivo.valor("interior", "nombre", id);
        i.umbral = archivo.valor("interior", "umbral", i.umbral);
        i.luzDeDia = Float.parseFloat(archivo.valor("interior", "luz_de_dia", String.valueOf(i.luzDeDia)));
        i.oscuridad = Integer.parseInt(archivo.valor("interior", "oscuridad", "0C0A12"), 16);
        // Su puerta de la calle la dice el registro de los interiores (res/datos/interiores.txt, [puertas]).
        castillafantasx.interiores.Interiores.Puerta puerta = castillafantasx.interiores.Interiores.puertaDe(id);
        if (puerta != null) {
            i.fuera = puerta.mapa;
            i.columnaDeLaPuerta = puerta.columna;
            i.filaDeLaPuerta = puerta.fila;
        }
        // [camas]: «<columna> <fila>»: dónde se encuentra de noche a quien se ha recogido aquí.
        for (String linea : archivo.lineas("camas")) {
            String[] p = linea.trim().split("\\s+");
            i.camas.add(new int[] { Integer.parseInt(p[0]), Integer.parseInt(p[1]) });
        }
        return i;
    }

    public MapaSuperficie(String id) {
        this.id = id;
        LectorSecciones archivo = new LectorSecciones("/mundo/" + id + ".txt");
        interior = leerInterior(archivo);
        colocado = new castillafantasx.mundo.LoColocado(archivo, castillafantasx.mundo.ArchivosDelMundo.archivoDe(id));
        List<String> lineas = archivo.lineas("mapa");
        filas = lineas.size();
        columnas = lineas.get(0).length();
        suelo = new char[filas][columnas];
        cosa = new char[filas][columnas];
        base = new float[filas][columnas][];
        nivel = new float[filas][columnas];
        leerAlturas(archivo.lineas("alturas"));
        leerRejilla(archivo, lineas);
        huella = calcularHuella();
        leerSeresYGente(archivo);
        leerObjetos(archivo);
        leerEdificios(archivo);
        leerCurasYAgua(archivo);
        leerLindesFloraYEscondites(archivo);
        apuntarGigantes();
        leerLugares(archivo);
    }

    /** [mapa]: el suelo y la cosa de cada casilla, y luego la base de lo que estorba. */
    private void leerRejilla(LectorSecciones archivo, List<String> lineas) {
        for (int f = 0; f < filas; f++) {
            String linea = lineas.get(f);
            if (linea.length() != columnas) {
                throw new IllegalStateException(id + ".txt: la fila " + f + " del mapa mide "
                        + linea.length() + " y debería medir " + columnas);
            }
            for (int c = 0; c < columnas; c++) {
                leerCasilla(archivo, linea.charAt(c), c, f);
            }
        }
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                base[f][c] = calcularBase(c, f);
            }
        }
    }

    /** [seres] y [gente]: dónde está cada criatura y cada persona al poblar el mapa. */
    private void leerSeresYGente(LectorSecciones archivo) {
        // [seres]: "<columna> <fila> <especie>".   [gente]: "<columna> <fila> <aspecto> | texto | texto…".
        for (String linea : archivo.lineas("seres")) {
            String[] p = linea.trim().split("\\s+");
            castillafantasx.seres.Ficha especie = castillafantasx.seres.Ficha.porId(p[2]);
            if (especie == null || !especie.enSuperficie()) {
                throw new IllegalStateException(id + ".txt [seres]: «" + p[2] + "» no es una especie de la superficie");
            }
            puestos.add(new Puesto(Integer.parseInt(p[0]), Integer.parseInt(p[1]), especie, null, null));
            completar(puestos.get(puestos.size() - 1), p, "seres", linea);
        }
        for (String linea : archivo.lineas("gente")) {
            String[] trozos = linea.split("\\|");
            String[] p = trozos[0].trim().split("\\s+");
            String[] textos = new String[trozos.length - 1];
            for (int i = 1; i < trozos.length; i++) {
                textos[i - 1] = trozos[i].trim();
            }
            puestos.add(new Puesto(Integer.parseInt(p[0]), Integer.parseInt(p[1]), null, p[2], textos));
            completar(puestos.get(puestos.size() - 1), p, "gente", linea);
        }
    }

    private void leerObjetos(LectorSecciones archivo) {
        // [objetos]: "<columna> <fila> <TIPO> <id> | dato": objetos puestos por coordenadas (los pone el editor); son marcas sin letra.
        for (String linea : archivo.lineas("objetos")) {
            String[] trozos = linea.split("\\|");
            String[] p = trozos[0].trim().split("\\s+");
            String[] textos = new String[trozos.length - 1];
            for (int i = 1; i < trozos.length; i++) {
                textos[i - 1] = trozos[i].trim();
            }
            // Sus palabras quedan como las de una marca («objeto TIPO id»), con lo que el editor le haya añadido detrás (giro=, tam=).
            String[] palabras = new String[p.length - 1];
            palabras[0] = "objeto";
            System.arraycopy(p, 2, palabras, 1, p.length - 2);
            Marca marca = new Marca(Integer.parseInt(p[0]), Integer.parseInt(p[1]), palabras, textos);
            marca.linea = linea.trim();
            marcas.add(marca);
        }
    }

    private void leerEdificios(LectorSecciones archivo) {
        // [edificios]: "<columna> <fila> <tipo>" (ver castillafantasx.paisaje.Edificios).
        for (String linea : archivo.lineas("edificios")) {
            String[] p = linea.trim().split("\s+");
            senalados.add(new Senalado(Integer.parseInt(p[0]), Integer.parseInt(p[1]), p[2]));
        }
    }

    private void leerCurasYAgua(LectorSecciones archivo) {
        // [curas]: "<columna> <fila> <id de la cura>": qué cura es cada «v» del mapa (la que no salga aquí es pan).
        for (String linea : archivo.lineas("curas")) {
            String[] p = linea.trim().split("\\s+");
            if (!castillafantasx.Contenido.delJuego().existeCura(p[2])) throw new IllegalStateException(id + ".txt [curas]: no existe la cura «" + p[2] + "»");
            curasSueltas.put(p[0] + "," + p[1], p[2]);
        }
        // [agua]: "<columna> <fila> moneda|<id de la cura>": lo que hay en el fondo del agua (docs/encargos_viejos/ENCARGO_INTERFAZ.md).
        for (String linea : archivo.lineas("agua")) {
            String[] p = linea.trim().split("\\s+");
            int c = Integer.parseInt(p[0]), f = Integer.parseInt(p[1]);
            if (!esAgua(c, f)) throw new IllegalStateException(id + ".txt [agua]: la casilla " + c + "," + f + " no es agua");
            if (!p[2].equals("moneda") && !castillafantasx.Contenido.delJuego().existeCura(p[2])) throw new IllegalStateException(id + ".txt [agua]: no existe «" + p[2] + "»");
            enElAgua.add(new Puesto(c, f, null, p[2], null));
        }
    }

    private void leerLindesFloraYEscondites(LectorSecciones archivo) {
        // [lindes]: "<borde> <mapa>": al llegar andando a ese borde se pasa al otro mapa, por el borde de enfrente.
        for (String linea : archivo.lineas("lindes")) {
            String[] p = linea.trim().split("\\s+");
            lindes.put(p[0], p[1]);
        }
        // [flora]: "<lugar> = <árbol> <peso>, … | <árbol gigante> <peso>, …": lo lee castillafantasx.paisaje.Flora.
        for (String linea : archivo.lineas("flora")) {
            int igual = linea.indexOf('=');
            if (igual > 0) flora.put(linea.substring(0, igual).trim(), linea.substring(igual + 1).trim());
        }
        // [escondites]: "<columna> <fila> <clase> | nota".
        for (String linea : archivo.lineas("escondites")) {
            String[] partes = linea.split("\\|");
            String[] n = partes[0].trim().split("\\s+");
            escondites.add(new Escondite(Integer.parseInt(n[0]), Integer.parseInt(n[1]), n[2], partes.length > 1 ? partes[1].trim() : ""));
        }
    }

    private void apuntarGigantes() {
        // Los árboles gigantes: cada tira de «|» seguidos en una fila es uno.
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                if (cosa[f][c] != GIGANTE || (c > 0 && cosa[f][c - 1] == GIGANTE)) continue;
                int ancho = 1;
                while (c + ancho < columnas && cosa[f][c + ancho] == GIGANTE) ancho++;
                gigantes.add(new int[] { c, f, ancho });
            }
        }
    }

    /** [lugares]: los rectángulos con nombre (y su oscuridad); un interior sin ellos es todo él un lugar. */
    private void leerLugares(LectorSecciones archivo) {
        for (String linea : archivo.lineas("lugares")) {
            String[] partes = linea.split("\\|");
            String[] n = partes[0].trim().split("\\s+");
            lugares.add(new Lugar(Integer.parseInt(n[0]), Integer.parseInt(n[1]), Integer.parseInt(n[2]),
                    Integer.parseInt(n[3]), Integer.parseInt(n[4], 16), partes[1].trim()));
        }
        // Un interior sin [lugares] es todo él un lugar, con su nombre y su oscuridad.
        if (interior != null && lugares.isEmpty()) lugares.add(new Lugar(0, 0, columnas - 1, filas - 1, interior.oscuridad, interior.nombre));
    }

    /**
     * La sección [alturas]: una rejilla como la del mapa, con la altura de
     * cada casilla. '.' = el llano; '1' a '9' = ese nivel; 'a' a 'i' = cuesta
     * (medio nivel por debajo: 'a' une el llano con el 1, 'b' el 1 con el 2…).
     * Las filas que falten, o lo que falte al final de una fila, son llano.
     */
    private void leerAlturas(List<String> lineas) {
        if (lineas.size() > filas) throw new IllegalStateException(id + ".txt: [alturas] tiene más filas que el mapa");
        for (int f = 0; f < lineas.size(); f++) {
            String linea = lineas.get(f);
            if (linea.length() > columnas) throw new IllegalStateException(id + ".txt: la fila " + f + " de [alturas] es más larga que el mapa");
            for (int c = 0; c < linea.length(); c++) {
                char letra = linea.charAt(c);
                if (letra >= '1' && letra <= '9') nivel[f][c] = letra - '0';
                else if (letra >= 'a' && letra <= 'i') nivel[f][c] = letra - 'a' + PASO_DE_NIVEL;
                else if (letra != '.' && letra != ' ') {
                    throw new IllegalStateException(id + ".txt: en [alturas], columna " + c + ", fila " + f + " hay un «" + letra + "»");
                }
            }
        }
    }

    /** La altura del suelo de una casilla, en niveles; fuera del mapa, el llano. */
    public float nivel(int col, int fila) {
        if (col < 0 || fila < 0 || col >= columnas || fila >= filas) return 0;
        return nivel[fila][col];
    }

    /** Lo mismo, en píxeles. */
    public float alturaDe(int col, int fila) {
        return nivel(col, fila) * castillafantasx.combate.CifrasDeSalto.ESCALON;
    }

    /** La altura del suelo, en píxeles, bajo un punto del mapa. */
    public float alturaEn(float x, float y) {
        return alturaDe((int) Math.floor(x / TAM), (int) Math.floor(y / TAM));
    }

    /** ¿Es una cuesta? (Por ella se sube y se baja andando.) */
    public boolean esCuesta(int col, int fila) {
        float n = nivel(col, fila);
        return n != Math.floor(n);
    }

    /** ¿Es agua o una grieta? No se pisa, pero saltando se pasa por encima. */
    public boolean seSaltaPorEncima(int col, int fila) {
        return SE_SALTAN.indexOf(suelo(col, fila)) >= 0;
    }

    public boolean esAgua(int col, int fila) {
        return suelo(col, fila) == '~';
    }

    // ------------------------------------------------------------------
    // Lo hondo que es el agua (docs/encargos_viejos/ENCARGO_INTERFAZ.md; cifras en res/datos/agua.txt)
    // ------------------------------------------------------------------

    /** Hasta cuántas casillas de distancia se busca la orilla (más lejos, el agua ya es todo lo honda que puede ser). */
    private static final int CASILLAS_HASTA_LA_ORILLA = 2;

    /**
     * La hondura del agua en un punto del mapa, en píxeles: 0 fuera del agua
     * y, dentro, más cuanto más lejos queda la orilla (lo que no es agua).
     * Con ella se sabe si ahí se vadea o se nada (ver castillafantasx.agua.CifrasDeAgua).
     */
    public float hondoEn(float x, float y) {
        int col = (int) Math.floor(x / TAM), fila = (int) Math.floor(y / TAM);
        if (!esAgua(col, fila)) return 0;
        float cerca = CASILLAS_HASTA_LA_ORILLA * TAM;
        for (int f = fila - CASILLAS_HASTA_LA_ORILLA; f <= fila + CASILLAS_HASTA_LA_ORILLA; f++) {
            for (int c = col - CASILLAS_HASTA_LA_ORILLA; c <= col + CASILLAS_HASTA_LA_ORILLA; c++) {
                if (esAgua(c, f)) continue;
                // La distancia del punto al cuadro de esa casilla de orilla.
                float dx = Math.max(0, Math.max(c * TAM - x, x - (c + 1) * TAM));
                float dy = Math.max(0, Math.max(f * TAM - y, y - (f + 1) * TAM));
                cerca = Math.min(cerca, (float) Math.hypot(dx, dy));
            }
        }
        return castillafantasx.agua.CifrasDeAgua.hondura(cerca);
    }

    /** La hondura del agua de una casilla, en píxeles: la de su centro (0 si no es agua). */
    public float hondoDeCasilla(int col, int fila) {
        return hondoEn(col * TAM + TAM / 2f, fila * TAM + TAM / 2f);
    }

    /** ¿Es agua de orilla? (Agua pegada a tierra: por el lado de la tierra se hace pie, y ahí se vadea o se baña uno.) */
    public boolean esOrilla(int col, int fila) {
        return esAgua(col, fila) && (!esAgua(col - 1, fila) || !esAgua(col + 1, fila) || !esAgua(col, fila - 1) || !esAgua(col, fila + 1));
    }

    /** ¿Es agua honda? (Agua donde, en el centro de la casilla, no se hace pie: hay que nadar.) */
    public boolean esAguaHonda(int col, int fila) {
        return esAgua(col, fila) && hondoDeCasilla(col, fila) >= castillafantasx.agua.CifrasDeAgua.PARA_NADAR;
    }

    /** Lo que levanta del suelo la cosa de una casilla si se puede saltar; si no (o no hay), Choques.NO_SE_SALTA. */
    public float altoDeCosa(int col, int fila) {
        char encima = cosa(col, fila);
        return encima == 0 ? castillafantasx.superficie.ChoquesDeCosas.NO_SE_SALTA : castillafantasx.superficie.ChoquesDeCosas.altoDeCosa(encima);
    }

    private void leerCasilla(LectorSecciones archivo, char letra, int c, int f) {
        char sueloDeLaIzquierda = (c > 0 && SUELOS.indexOf(suelo[f][c - 1]) >= 0) ? suelo[f][c - 1] : '.';
        if (SUELOS.indexOf(letra) >= 0 || MACIZOS.indexOf(letra) >= 0) {
            suelo[f][c] = letra;
            if (letra == 'n') {
                luces.add(new Luz(c * TAM + 16, f * TAM + 24, RADIO_VENTANA, LUZ_CALIDA, false));
            }
            if (letra == 'q') luces.add(new Luz(c * TAM + 16, f * TAM + 20, RADIO_CANDIL, LUZ_CALIDA, true));
        } else if (COSAS.indexOf(letra) >= 0) {
            suelo[f][c] = (letra == 't' || letra == 'y' || letra == '"' || letra == GIGANTE) ? '.' : sueloDeLaIzquierda;
            cosa[f][c] = letra;
            encenderLaLuzDe(letra, c, f);
        } else if (SUELTOS.indexOf(letra) >= 0) {
            suelo[f][c] = sueloDeLaIzquierda;
            sueltos.add(new Suelto(letra, c, f));
        } else {
            // Una marca: su definición dice qué suelo tiene debajo y qué es.
            String definicion = archivo.valor("marcas", String.valueOf(letra), null);
            if (definicion == null) {
                throw new IllegalStateException(id + ".txt: en la columna " + c + ", fila " + f
                        + " hay un «" + letra + "» que no es terreno ni está en [marcas]");
            }
            suelo[f][c] = definicion.charAt(0);
            String[] trozos = definicion.substring(1).trim().split("\\|");
            String[] textos = new String[trozos.length - 1];
            for (int i = 1; i < trozos.length; i++) {
                textos[i - 1] = trozos[i].trim();
            }
            marcas.add(new Marca(c, f, trozos[0].trim().split("\\s+"), textos));
            marcas.get(marcas.size() - 1).letra = letra;
        }
    }

    /** La luz que da una cosa del mapa (el farol, las velas, el monolito y el candil), apuntada con su casilla. */
    private void encenderLaLuzDe(char letra, int c, int f) {
        Luz luz = null;
        if (letra == 'i') luz = new Luz(c * TAM + 16, f * TAM - 12, RADIO_FAROL, LUZ_CALIDA, true);
        if (letra == 'c') luz = new Luz(c * TAM + 16, f * TAM + 18, RADIO_VELAS, LUZ_CALIDA, true);
        if (letra == '*') luz = new Luz(c * TAM + 16, f * TAM, RADIO_MONOLITO, LUZ_EXTRANA, false);
        if (letra == 'z') luz = new Luz(c * TAM + 21, f * TAM - 6, RADIO_CANDIL, LUZ_CALIDA, true);
        if (luz == null) return;
        luz.columna = c;
        luz.fila = f;
        luces.add(luz);
    }

    /**
     * El editor quita la cosa fija de una casilla (un árbol, una cruz, un
     * farol): deja de estorbar y de dar luz, y queda el suelo que tenía
     * debajo. Devuelve la letra que había (0 si no había nada).
     */
    public char quitarCosa(int c, int f) {
        return ponerCosa(c, f, (char) 0);
    }

    /** Pone en una casilla esa cosa fija (o, con 0, la quita). Devuelve la que había. El paisaje se entera por la huella. */
    public char ponerCosa(int c, int f, char letra) {
        char antes = cosa(c, f);
        if (c < 0 || f < 0 || c >= columnas || f >= filas || antes == letra) return antes;
        cosa[f][c] = letra;
        luces.removeIf(luz -> luz.columna == c && luz.fila == f);
        encenderLaLuzDe(letra, c, f);
        // La base de las vecinas también cambia: los árboles y las cercas se estiran hacia sus vecinos.
        for (int ff = Math.max(0, f - 1); ff <= Math.min(filas - 1, f + 1); ff++) {
            for (int cc = Math.max(0, c - 1); cc <= Math.min(columnas - 1, c + 1); cc++) {
                base[ff][cc] = calcularBase(cc, ff);
            }
        }
        huella = calcularHuella();
        return antes;
    }

    private int calcularHuella() {
        return java.util.Arrays.deepHashCode(suelo) * 31 * 31 + java.util.Arrays.deepHashCode(cosa) * 31 + java.util.Arrays.deepHashCode(nivel);
    }

    /** El suelo de una casilla; fuera del mapa, roca (o muro, si es un interior). */
    public char suelo(int col, int fila) {
        // Fuera del mapa: roca; fuera del de un interior, el grueso de sus muros.
        if (col < 0 || fila < 0 || col >= columnas || fila >= filas) return interior != null ? 'M' : '^';
        return suelo[fila][col];
    }

    /** La cosa fija de una casilla, o 0 si no hay nada. */
    public char cosa(int col, int fila) {
        if (col < 0 || fila < 0 || col >= columnas || fila >= filas) return 0;
        return cosa[fila][col];
    }

    /** ¿Es un muro, una roca, una casa o agua? */
    public boolean macizo(int col, int fila) {
        return MACIZOS.indexOf(suelo(col, fila)) >= 0;
    }

    /** ¿Tiene esa casilla algo que estorba? (Es maciza, o tiene encima una cosa con base.) Para elegir casillas libres. */
    public boolean solida(int col, int fila) {
        return macizo(col, fila) || baseDeCosa(col, fila) != null;
    }

    /** La base con la que estorba la cosa de una casilla ({x0, y0, x1, y1}), o null si no hay o no estorba. */
    public float[] baseDeCosa(int col, int fila) {
        if (col < 0 || fila < 0 || col >= columnas || fila >= filas) return null;
        return base[fila][col];
    }

    private boolean esArbol(int col, int fila) {
        char encima = cosa(col, fila);
        return encima != 0 && ARBOLES.indexOf(encima) >= 0;
    }

    /** ¿Hay en esa casilla un tramo de cerca (de palos o de piedra seca)? */
    public boolean esCerca(int col, int fila) {
        char encima = cosa(col, fila);
        return encima != 0 && CERCAS.indexOf(encima) >= 0;
    }

    /**
     * ¿Sube la cerca de esa casilla hacia el norte? Sí, si tiene cerca justo
     * encima; y también si es el cabo de un tramo que baja hacia el sur y
     * encima tiene un árbol o un muro (así el corral se cierra contra ellos).
     */
    public boolean cercaAlNorte(int col, int fila) {
        if (esCerca(col, fila - 1)) return true;
        return esCerca(col, fila + 1) && (esArbol(col, fila - 1) || macizo(col, fila - 1));
    }

    /** ¿Tiene la cerca de esa casilla un tramo a lo ancho? Sí, si sigue a un lado o al otro, o si está sola. */
    public boolean cercaALoAncho(int col, int fila) {
        if (esCerca(col - 1, fila) || esCerca(col + 1, fila)) return true;
        return !esCerca(col, fila - 1) && !esCerca(col, fila + 1);
    }

    /**
     * La base de la cosa de una casilla: centrada abajo, con el ancho y el
     * fondo de res/datos/choques.txt. Un árbol se estira hasta el borde de la
     * casilla por donde tenga otro árbol o algo macizo: así no queda hueco entre ellos.
     */
    private float[] calcularBase(int c, int f) {
        float[] medida = cosa[f][c] == 0 ? null : castillafantasx.superficie.ChoquesDeCosas.baseDeCosa(cosa[f][c]);
        if (medida == null) return null;
        float centro = c * TAM + TAM / 2f, abajo = (f + 1) * TAM - PIE_DE_LAS_COSAS;
        float[] b = { centro - medida[0] / 2, abajo - medida[1], centro + medida[0] / 2, abajo };
        if (esArbol(c, f)) {
            if (esArbol(c - 1, f) || macizo(c - 1, f)) b[0] = c * TAM;
            if (esArbol(c + 1, f) || macizo(c + 1, f)) b[2] = (c + 1) * TAM;
            if (esArbol(c, f - 1) || macizo(c, f - 1)) b[1] = f * TAM;
            if (esArbol(c, f + 1) || macizo(c, f + 1)) b[3] = (f + 1) * TAM;
        }
        if (cosa[f][c] == '-') {
            // Un tronco tumbado: hacia el tronco vecino llega hasta el borde.
            if (cosa(c - 1, f) == '-') b[0] = c * TAM;
            if (cosa(c + 1, f) == '-') b[2] = (c + 1) * TAM;
        }
        if (esCerca(c, f)) {
            // La medida es la de un poste; hacia cada cerca vecina se estira hasta el borde: no queda hueco.
            if (esCerca(c - 1, f)) b[0] = c * TAM;
            if (esCerca(c + 1, f)) b[2] = (c + 1) * TAM;
            if (cercaAlNorte(c, f)) b[1] = f * TAM;
            if (esCerca(c, f + 1)) b[3] = (f + 1) * TAM;
        }
        return b;
    }

    /** A cuántos píxeles del borde de abajo de su casilla se apoyan las cosas y los objetos (como en el dibujo). */
    public static final int PIE_DE_LAS_COSAS = 2;

    /** ¿Estorba a quien vuela? Solo los muros altos, la roca, las casas y los cobertizos. */
    public boolean solidaParaQuienVuela(int col, int fila) {
        return ALTOS.indexOf(suelo(col, fila)) >= 0;
    }

    /** El borde del mapa que pisa esa casilla ("norte", "sur", "este", "oeste"), si por él se pasa a otro mapa; si no, null. */
    public String lindeEn(int col, int fila) {
        String borde = fila <= 0 ? "norte" : fila >= filas - 1 ? "sur" : col <= 0 ? "oeste" : col >= columnas - 1 ? "este" : null;
        return borde != null && lindes.containsKey(borde) ? borde : null;
    }

    /** El lugar al que pertenece una casilla, o null. */
    public Lugar lugarEn(int col, int fila) {
        for (Lugar l : lugares) {
            if (col >= l.col0 && col <= l.col1 && fila >= l.fila0 && fila <= l.fila1) return l;
        }
        return null;
    }

    /** El lugar con ese nombre, o null. */
    public Lugar lugar(String nombre) {
        for (Lugar l : lugares) {
            if (l.nombre.equals(nombre)) return l;
        }
        return null;
    }

    /** La marca de ese tipo que hay en una casilla ("salida", "paso"…), o null. */
    public Marca marcaEn(String tipo, int col, int fila) {
        for (Marca m : marcas) {
            if (m.columna == col && m.fila == fila && m.tipo.equals(tipo)) return m;
        }
        return null;
    }

    /** La marca "llegada <nombre>", o null. */
    public Marca llegada(String nombre) {
        for (Marca m : marcas) {
            if (m.tipo.equals("llegada") && m.palabras[1].equals(nombre)) return m;
        }
        return null;
    }
}
