package castillafantasx.eventos;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MISION — Una misión tal como la escribe Fran en res/misiones/<id>.txt
 * (docs/EVENTOS.md): sus variables, sus zonas, sus límites, sus eventos
 * («cuando pasa algo, si se cumple algo, ocurre algo») y sus secuencias de
 * cine. Aquí solo se lee el archivo; quien lo hace andar es el Motor.
 *
 * El archivo es texto con secciones entre corchetes y líneas «clave = valor»,
 * como los de res/mundo y res/comercio. El editor lo leerá y lo escribirá igual.
 */
public final class Mision {

    /** La carpeta de las misiones en el disco, desde la raíz del juego: lo que edita Fran (y el editor). */
    public static final File CARPETA = new File("res/misiones");
    // Si no está la carpeta (el juego empaquetado), van dentro del juego.
    private static final String RECURSO = "/misiones/";
    /** El archivo que dice qué misiones hay, una por línea en su sección [misiones]. */
    public static final String INDICE = "misiones.txt";

    /** Un rectángulo de casillas de un mapa (la comarca, el bosque o una mazmorra), o un lugar del mapa por su nombre. */
    public static class Zona {
        public final String nombre;
        /** El id del mapa («superficie», «bosque» o el de una mazmorra); null si es un lugar del mapa por su nombre. */
        public final String mapa;
        public final int col0, fila0, col1, fila1;
        /** El nombre del lugar del mapa (los de [lugares] de res/mundo), si la zona es un lugar entero; si no, null. */
        public final String lugar;

        public Zona(String nombre, String mapa, int col0, int fila0, int col1, int fila1, String lugar) {
            this.nombre = nombre;
            this.mapa = mapa;
            this.col0 = Math.min(col0, col1);
            this.fila0 = Math.min(fila0, fila1);
            this.col1 = Math.max(col0, col1);
            this.fila1 = Math.max(fila0, fila1);
            this.lugar = lugar;
        }
    }

    /** Un trozo del mundo que entra en la misión. Puede empezar cerrado y abrirse con un evento. */
    public static final class Recinto extends Zona {
        public final boolean abiertoAlEmpezar;

        Recinto(Zona z, boolean abierto) {
            super(z.nombre, z.mapa, z.col0, z.fila0, z.col1, z.fila1, z.lugar);
            this.abiertoAlEmpezar = abierto;
        }
    }

    /** Cómo se detiene el héroe en un tramo del borde. */
    public enum Freno {
        /** Alguien le sale al paso y le habla (un guarda, un pastor, un fraile). */
        PERSONA,
        /** El héroe se niega a seguir, con una frase (en los datos, «heroe» o el id del héroe de res/datos/juego.txt). */
        HEROE,
        /** El terreno corta el paso (barranco, río crecido, monte cerrado). */
        TERRENO;

        /** Cómo se escribe en el archivo de la misión: «persona», «terreno» o el id del héroe (res/datos/juego.txt). */
        public String palabra() {
            return this == HEROE ? castillafantasx.datos.ElJuego.idDelHeroe() : name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Un tramo del borde de la misión y cómo se detiene en él el héroe. */
    public static final class Tramo {
        /** «norte», «sur», «este», «oeste», o null: todo el borde que no tenga tramo propio. */
        public final String lado;
        /** De qué columna a cuál (norte y sur) o de qué fila a cuál (este y oeste); -1 y -1: el lado entero. */
        public final int desde, hasta;
        /** El recinto de cuyo borde es el tramo, o null: el de cualquiera. */
        public final String recinto;
        public final Freno freno;
        /** Quién le sale al paso (su aspecto del Vestuario), si es una persona. */
        public final String aspecto;
        /** El nudo de Ink con lo que se dice. */
        public final String nudo;

        public Tramo(String lado, int desde, int hasta, String recinto, Freno freno, String aspecto, String nudo) {
            this.lado = lado;
            this.desde = desde;
            this.hasta = hasta;
            this.recinto = recinto;
            this.freno = freno;
            this.aspecto = aspecto;
            this.nudo = nudo;
        }

        /** ¿Es de ese lado, cubre esa columna (o fila) y es de ese recinto? */
        public boolean cubre(String elLado, int donde, String elRecinto) {
            if (recinto != null && !recinto.equals(elRecinto)) return false;
            if (lado == null) return true;
            return lado.equals(elLado) && (desde < 0 || (donde >= desde && donde <= hasta));
        }

        /** Lo concreto que es: el que dice su lado manda sobre el general, y el que dice su recinto, sobre el que no. */
        public int concreto() {
            return (lado != null ? 2 : 0) + (recinto != null ? 1 : 0);
        }

        /** Como se escribe en el archivo, sin el «borde»: «norte 200-230 en aldea = persona pastor limite_pastor». */
        public String texto() {
            String donde = (lado == null ? "" : lado) + (desde < 0 ? "" : " " + desde + "-" + hasta) + (recinto == null ? "" : " en " + recinto);
            return donde.trim() + " = " + freno.palabra() + (aspecto != null ? " " + aspecto : "") + " " + nudo;
        }
    }

    /** Un evento: cuándo salta, qué tiene que cumplirse, si salta una vez o cada vez, y lo que hace. */
    public static final class Evento {
        public final String id;
        /** El disparador: «zona», «hablar», «hora»… y con qué («era», «pastor_del_vado», «21»). */
        public String cuando = "nunca", con = "";
        /** Las condiciones: tienen que cumplirse todas (docs/EVENTOS.md). */
        public final List<String> si = new ArrayList<>();
        public boolean unaVez = true;
        /** Lo que hace, paso a paso (los mismos pasos que una secuencia). */
        public final List<String> hace = new ArrayList<>();

        Evento(String id) {
            this.id = id;
        }
    }

    public final String id;
    public String titulo;
    /** ¿Empieza sola con la partida nueva? Si no, la empieza un evento, un diálogo o F1. */
    public boolean empiezaSola;
    /** Las variables de la misión, con su valor de partida. */
    public final Map<String, String> variables = new LinkedHashMap<>();
    public final Map<String, Zona> zonas = new LinkedHashMap<>();
    /** Los trozos del mundo que entran en la misión. Sin ninguno, la misión no tiene límites en la superficie. */
    public final List<Recinto> recintos = new ArrayList<>();
    /** Las mazmorras que entran (true: abierta al empezar). Si 'limitaMazmorras' es false, entran todas. */
    public final Map<String, Boolean> mazmorras = new LinkedHashMap<>();
    public boolean limitaMazmorras;
    public final List<Tramo> tramos = new ArrayList<>();
    public final Map<String, Evento> eventos = new LinkedHashMap<>();
    public final Map<String, List<String>> secuencias = new LinkedHashMap<>();
    /** Lo que no se ha entendido al leer el archivo (se ve en la consola y en F1). */
    public final List<String> avisos = new ArrayList<>();

    private Mision(String id) {
        this.id = id;
        this.titulo = id;
    }

    // ------------------------------------------------------------------
    // Leer las misiones
    // ------------------------------------------------------------------

    private static File carpeta = CARPETA;

    /** La carpeta de la que se leen ahora las misiones (el editor escribe en ella los archivos de cada una). */
    public static File carpeta() {
        return carpeta;
    }

    /** Para las pruebas: lee las misiones de otra carpeta (null: la de siempre). */
    public static void usarCarpeta(File otra) {
        carpeta = otra == null ? CARPETA : otra;
    }

    /** Todas las misiones de res/misiones/misiones.txt, en su orden. Una que no se pueda leer se dice y se salta. */
    public static List<Mision> todas() {
        List<Mision> lista = new ArrayList<>();
        String indice;
        try {
            indice = leerArchivo(INDICE);
        } catch (IOException e) {
            return lista;       // sin índice no hay misiones (el juego sigue como siempre)
        }
        for (String linea : indice.split("\n")) {
            String idDeUna = sinComentario(linea).trim();
            if (idDeUna.isEmpty() || idDeUna.startsWith("[")) continue;
            try {
                lista.add(deTexto(idDeUna, leerArchivo(idDeUna + ".txt")));
            } catch (IOException | RuntimeException e) {
                System.err.println("La misión «" + idDeUna + "» no se puede leer: " + e.getMessage());
            }
        }
        return lista;
    }

    // Un archivo de la carpeta del disco o, si no está, de dentro del juego.
    private static String leerArchivo(String nombre) throws IOException {
        File f = new File(carpeta, nombre);
        if (f.isFile()) return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        if (carpeta != CARPETA) throw new IOException("Falta " + f);
        try (InputStream in = Mision.class.getResourceAsStream(RECURSO + nombre)) {
            if (in == null) throw new IOException("Falta el archivo de misión " + nombre);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Un '#' al principio de la línea, o precedido de dos espacios, empieza un comentario (como en res/mundo).
    private static String sinComentario(String linea) {
        if (linea.trim().startsWith("#")) return "";
        int i = linea.indexOf("  #");
        return i >= 0 ? linea.substring(0, i) : linea;
    }

    /** Lee una misión de su texto (el de su archivo). */
    public static Mision deTexto(String id, String texto) {
        Mision m = new Mision(id);
        String seccion = "", nombre = "";
        try (BufferedReader lector = new BufferedReader(new StringReader(texto))) {
            String cruda;
            while ((cruda = lector.readLine()) != null) {
                String linea = sinComentario(cruda).trim();
                if (linea.isEmpty()) continue;
                if (linea.startsWith("[") && linea.endsWith("]")) {
                    // «[evento llegar_a_la_era]»: la clase de sección y su nombre.
                    String[] p = linea.substring(1, linea.length() - 1).trim().split("\\s+", 2);
                    seccion = p[0];
                    nombre = p.length > 1 ? p[1].trim() : "";
                    if (seccion.equals("evento")) m.eventos.put(nombre, new Evento(nombre));
                    else if (seccion.equals("secuencia")) m.secuencias.put(nombre, new ArrayList<>());
                    continue;
                }
                m.leerLinea(seccion, nombre, linea);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);     // no puede pasar: se lee de un texto
        }
        for (String aviso : m.avisos) {
            System.err.println("Misión «" + id + "»: " + aviso);
        }
        return m;
    }

    private void leerLinea(String seccion, String nombre, String linea) {
        if (seccion.equals("secuencia")) {
            secuencias.get(nombre).add(linea);      // cada línea es un paso
            return;
        }
        int igual = linea.indexOf('=');
        if (igual < 0) {
            avisos.add("[" + seccion + "] falta el «=» en «" + linea + "»");
            return;
        }
        String clave = linea.substring(0, igual).trim(), valor = linea.substring(igual + 1).trim();
        try {
            switch (seccion) {
                case "mision":
                    if (clave.equals("titulo")) titulo = valor;
                    else if (clave.equals("empieza")) empiezaSola = valor.equals("sola");
                    else avisos.add("[mision] no se entiende «" + clave + "»");
                    break;
                case "variables":
                    variables.put(clave, valor);
                    break;
                case "zonas":
                    zonas.put(clave, zona(clave, valor));
                    break;
                case "limites":
                    leerLimite(clave, valor);
                    break;
                case "evento":
                    leerEvento(eventos.get(nombre), clave, valor);
                    break;
                default:
                    avisos.add("no se conoce la sección [" + seccion + "]");
                    break;
            }
        } catch (RuntimeException e) {
            avisos.add("[" + seccion + (nombre.isEmpty() ? "" : " " + nombre) + "] no se entiende «" + linea + "»: " + e.getMessage());
        }
    }

    /** Una zona: «superficie 190 224 196 228» (mapa, columna y fila de una esquina y de la otra) o «lugar <nombre del lugar>». */
    static Zona zona(String nombre, String valor) {
        String[] p = valor.trim().split("\\s+");
        if (p[0].equals("lugar")) return new Zona(nombre, null, 0, 0, 0, 0, valor.trim().substring("lugar".length()).trim());
        if (p.length < 5) throw new IllegalArgumentException("una zona es «mapa columna fila columna fila» o «lugar Nombre»");
        return new Zona(nombre, p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), null);
    }

    private void leerLimite(String clave, String valor) {
        String[] c = clave.split("\\s+");
        if (c[0].equals("recinto") && c.length == 2) {
            // «recinto valle = superficie 120 180 260 280 abierto» (o «cerrado»; si no se dice, abierto).
            boolean cerrado = valor.endsWith(" cerrado");
            String sinEstado = valor.replaceAll("\\s+(abierto|cerrado)$", "");
            recintos.add(new Recinto(zona(c[1], sinEstado), !cerrado));
        } else if (c[0].equals("mazmorras")) {
            // «mazmorras = cueva_natural, grieta cerrada»: las que entran; «ninguna», ninguna.
            limitaMazmorras = true;
            for (String una : valor.split(",")) {
                String[] p = una.trim().split("\\s+");
                if (!p[0].isEmpty() && !p[0].equals("ninguna")) mazmorras.put(p[0], !(p.length > 1 && p[1].startsWith("cerrad")));
            }
        } else if (c[0].equals("borde")) {
            tramos.add(tramo(clave.substring("borde".length()).trim(), valor));
        } else {
            avisos.add("[limites] no se entiende «" + clave + "»");
        }
    }

    /**
     * Un tramo del borde: donde («», «norte», «este 200-230», «sur en aldea»,
     * «en arena») y cómo se detiene («persona pastor nudo», «<id del héroe> nudo», «terreno nudo»).
     */
    public static Tramo tramo(String donde, String como) {
        String[] d = donde.trim().isEmpty() ? new String[0] : donde.trim().split("\\s+");
        String lado = null, recinto = null;
        int desde = -1, hasta = -1;
        for (int i = 0; i < d.length; i++) {
            if (d[i].equals("en") && i + 1 < d.length) {
                recinto = d[++i];
            } else if (d[i].matches("norte|sur|este|oeste") && lado == null) {
                lado = d[i];
            } else if (d[i].matches("\\d+-\\d+") && lado != null) {
                String[] r = d[i].split("-");
                desde = Integer.parseInt(r[0]);
                hasta = Integer.parseInt(r[1]);
            } else {
                throw new IllegalArgumentException("un tramo es «[norte, sur, este u oeste] [de-a] [en <recinto>]»");
            }
        }
        String[] p = como.trim().split("\\s+");
        String palabra = p[0].equals(castillafantasx.datos.ElJuego.idDelHeroe()) ? "heroe" : p[0];
        Freno freno = Freno.valueOf(palabra.toUpperCase(java.util.Locale.ROOT));
        if (freno == Freno.PERSONA) {
            if (p.length < 3) throw new IllegalArgumentException("«persona» pide su aspecto y su nudo de Ink");
            return new Tramo(lado, desde, hasta, recinto, freno, p[1], p[2]);
        }
        if (p.length < 2) throw new IllegalArgumentException("falta el nudo de Ink");
        return new Tramo(lado, desde, hasta, recinto, freno, null, p[1]);
    }

    private void leerEvento(Evento e, String clave, String valor) {
        switch (clave) {
            case "cuando": {
                String[] p = valor.split("\\s+", 2);
                e.cuando = p[0];
                e.con = p.length > 1 ? p[1].trim() : "";
                break;
            }
            case "si":
                e.si.add(valor);
                break;
            case "veces":
                e.unaVez = !valor.startsWith("cada") && !valor.equals("siempre");
                break;
            case "hace":
                e.hace.add(valor);
                break;
            default:
                avisos.add("[evento " + e.id + "] no se entiende «" + clave + "»");
                break;
        }
    }
}
