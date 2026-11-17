package castillafantasx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PANTALLA — El tamaño de la imagen del juego y lo que el jugador ha elegido
 * sobre ella (pantalla completa, tamaño de la ventana y aumento).
 *
 * El juego se pinta en una imagen pequeña y esa se amplía un número ENTERO de
 * veces (el «aumento») hasta llenar la ventana: así cada píxel del dibujo es
 * un cuadrado exacto en cualquier monitor. La imagen no mide siempre lo mismo:
 * mide lo que mide la ventana dividido entre el aumento. Una ventana más ancha
 * o más alta enseña más mundo, y nunca quedan bandas negras.
 *
 * Todo el dibujo mira 'ancho' y 'alto' para colocarse: los marcadores van
 * pegados a los bordes y los menús, centrados.
 */
public final class Pantalla {

    private Pantalla() { }

    /** Lo menos que mide la imagen: todos los menús y marcadores caben en este tamaño. */
    public static final int ANCHO_MINIMO = 640, ALTO_MINIMO = 330;
    /** El alto de imagen que se busca: se elige el aumento que más se le acerca. PROVISIONAL. */
    public static final int ALTO_IDEAL = 450;
    /** La ventana con la que se arranca si no hay nada guardado. */
    public static final int VENTANA_ANCHO = 1280, VENTANA_ALTO = 720;

    /** Lo que mide ahora la imagen del juego, en píxeles del dibujo. */
    public static int ancho = ANCHO_MINIMO, alto = 360;
    /** Cuántas veces se amplía la imagen para llevarla a la ventana. */
    public static int aumento = 2;

    // --- Lo que elige el jugador (se guarda en pantalla.txt) ---
    public static boolean completa = true;
    /**
     * La pantalla completa es una ventana sin bordes del tamaño del monitor (de fábrica): cambiar de programa
     * con Alt+Tab no hace parpadear la pantalla. Con «no», la pantalla completa de antes (el juego se queda el monitor).
     */
    public static boolean sinBordes = true;
    public static int ventanaAncho = VENTANA_ANCHO, ventanaAlto = VENTANA_ALTO;
    /** El aumento elegido en el menú de pausa; 0 = automático. */
    public static int aumentoElegido = 0;

    // El tamaño de la ventana en el último ajuste (para saber qué aumentos caben).
    private static int ultimoAncho = VENTANA_ANCHO, ultimoAlto = VENTANA_ALTO;

    /** ¿Cabe la imagen mínima en esa ventana con ese aumento? */
    private static boolean cabe(int anchoVentana, int altoVentana, int cuanto) {
        return anchoVentana / cuanto >= ANCHO_MINIMO && altoVentana / cuanto >= ALTO_MINIMO;
    }

    /** El aumento automático: el entero que deja el alto de la imagen más cerca del ideal, sin bajar del tamaño mínimo. */
    public static int aumentoAutomatico(int anchoVentana, int altoVentana) {
        int cuanto = Math.max(1, Math.round(altoVentana / (float) ALTO_IDEAL));
        while (cuanto > 1 && !cabe(anchoVentana, altoVentana, cuanto)) cuanto--;
        return cuanto;
    }

    /** El mayor aumento con el que la imagen mínima aún cabe en la ventana de ahora. */
    public static int aumentoMaximo() {
        int cuanto = 1;
        while (cabe(ultimoAncho, ultimoAlto, cuanto + 1)) cuanto++;
        return cuanto;
    }

    /**
     * Calcula el aumento y el tamaño de la imagen para una ventana de ese
     * tamaño (en píxeles de verdad). Devuelve true si la imagen ha cambiado
     * de tamaño: entonces hay que rehacer los lienzos.
     */
    public static boolean ajustar(int anchoVentana, int altoVentana) {
        ultimoAncho = anchoVentana;
        ultimoAlto = altoVentana;
        int cuanto = aumentoAutomatico(anchoVentana, altoVentana);
        if (aumentoElegido > 0) cuanto = Math.min(aumentoElegido, aumentoMaximo());
        // Si la ventana no es múltiplo exacto del aumento, la imagen mide un píxel más y sobresale un poco.
        int nuevoAncho = Math.max(ANCHO_MINIMO, (anchoVentana + cuanto - 1) / cuanto);
        int nuevoAlto = Math.max(ALTO_MINIMO, (altoVentana + cuanto - 1) / cuanto);
        boolean cambia = nuevoAncho != ancho || nuevoAlto != alto;
        aumento = cuanto;
        ancho = nuevoAncho;
        alto = nuevoAlto;
        return cambia;
    }

    /** El menú de pausa pasa al aumento siguiente: automático, ×1, ×2… hasta el mayor que cabe, y vuelta. */
    public static void siguienteAumento() {
        aumentoElegido = aumentoElegido >= aumentoMaximo() ? 0 : aumentoElegido + 1;
        guardar();
    }

    /** Cómo se lee el aumento en el menú: "automático (×2)" o "×3". */
    public static String nombreDelAumento() {
        return aumentoElegido == 0 ? "automático (×" + aumento + ")" : "×" + Math.min(aumentoElegido, aumentoMaximo());
    }

    /** Enseñar el retrato de quien habla en los diálogos (opción de la pausa). De fábrica, sí. */
    public static boolean retratos = true;

    public static void cambiarRetratos() {
        retratos = !retratos;
        guardar();
    }

    /** Cuánto del estado del héroe (vida y aguante) se ve en pantalla. Es una opción de la pausa, no una dificultad. */
    public enum Interfaz {
        NINGUNA("ninguna"), MINIMA("mínima"), COMPLETA("completa");

        /** Cómo se lee en el menú de pausa. */
        public final String nombre;

        Interfaz(String nombre) {
            this.nombre = nombre;
        }
    }

    /** La interfaz elegida. De fábrica, ninguna: el estado se lee en el cuerpo del héroe. */
    public static Interfaz interfaz = Interfaz.NINGUNA;

    /** El menú de pausa pasa a la opción siguiente: ninguna, mínima, completa, y vuelta. */
    public static void siguienteInterfaz() {
        interfaz = Interfaz.values()[(interfaz.ordinal() + 1) % Interfaz.values().length];
        guardar();
    }

    /** En la superficie, el héroe mira hacia el cursor del ratón (opción de la pausa). De fábrica, con las teclas, como siempre. */
    public static boolean mirarConRaton = false;

    public static void cambiarMirada() {
        mirarConRaton = !mirarConRaton;
        guardar();
    }

    /**
     * En los menús, las letras escriben en su buscador (como en F1 y el
     * editor; encargo ORDEN_ERGONOMIA). Apagado, W, A, S y D vuelven a mover
     * por los menús, como antes (opción de la pausa).
     */
    public static boolean letrasBuscan = true;

    public static void cambiarLetras() {
        letrasBuscan = !letrasBuscan;
        guardar();
    }

    /**
     * El campo de visión de la mazmorra (opción de la pausa): la mitad del alto de la vista
     * a una casilla de distancia. Más amplio, se ve más mazmorra y todo más pequeño.
     */
    // TODO-FRAN: los campos de visión que se ofrecen y su apertura son PROVISIONALES.
    public enum Campo {
        ESTRECHO("estrecho", 0.56), NORMAL("normal", 0.66), AMPLIO("amplio", 0.78), MUY_AMPLIO("muy amplio", 0.92);

        public final String nombre;
        public final double apertura;

        Campo(String nombre, double apertura) {
            this.nombre = nombre;
            this.apertura = apertura;
        }
    }

    public static Campo campo = Campo.NORMAL;

    public static void siguienteCampo() {
        campo = Campo.values()[(campo.ordinal() + 1) % Campo.values().length];
        guardar();
    }

    /**
     * La vista de la mazmorra, pintada a la imagen entera del juego (nítida) o a la mitad (cada punto, 2 × 2 píxeles,
     * como antes del encargo de la optimización). Opción de la pausa.
     */
    // TODO-FRAN: si la vista entera queda de fábrica (lo es ahora) o solo como opción.
    public static boolean vistaEntera = true;

    public static void cambiarVista() {
        vistaEntera = !vistaEntera;
        guardar();
    }

    public static void cambiarCompleta() {
        completa = !completa;
        guardar();
    }

    // ------------------------------------------------------------------
    // pantalla.txt: "completa = si", "ventana = 1280x720", "aumento = 0", "retratos = si", "interfaz = ninguna"
    // ------------------------------------------------------------------

    /** Dónde se guarda. Las pruebas lo cambian para no tocar el de Fran. */
    public static Path archivo = Path.of("pantalla.txt");

    /** Escribe lo elegido en pantalla.txt. Si no se puede, el juego sigue. */
    public static void guardar() {
        List<String> lineas = new ArrayList<>();
        lineas.add("# Pantalla de " + castillafantasx.datos.ElJuego.nombre() + ". Se cambia en el menú de pausa y con la tecla de pantalla completa.");
        lineas.add("# Si borras este archivo, vuelve lo de fábrica. aumento = 0 es automático.");
        lineas.add("completa = " + (completa ? "si" : "no"));
        lineas.add("# sin_bordes = si: la pantalla completa es una ventana sin bordes (Alt+Tab no parpadea); no: la de antes.");
        lineas.add("sin_bordes = " + (sinBordes ? "si" : "no"));
        lineas.add("ventana = " + ventanaAncho + "x" + ventanaAlto);
        lineas.add("aumento = " + aumentoElegido);
        lineas.add("retratos = " + (retratos ? "si" : "no"));
        lineas.add("# interfaz = ninguna, minima o completa: cuánto se ve de la vida y el aguante.");
        lineas.add("interfaz = " + interfaz.name().toLowerCase());
        lineas.add("# mirar = teclado o raton: en la superficie, hacia dónde mira " + castillafantasx.datos.ElJuego.heroe() + ".");
        lineas.add("mirar = " + (mirarConRaton ? "raton" : "teclado"));
        lineas.add("# letras = buscan o mueven: en los menús, las letras escriben en el buscador o W, A, S y D mueven.");
        lineas.add("letras = " + (letrasBuscan ? "buscan" : "mueven"));
        lineas.add("# campo = estrecho, normal, amplio o muy_amplio: el campo de visión en la mazmorra.");
        lineas.add("campo = " + campo.name().toLowerCase());
        lineas.add("# vista = entera o mitad: la vista de la mazmorra, nítida o a la mitad de la imagen (como antes).");
        lineas.add("vista = " + (vistaEntera ? "entera" : "mitad"));
        try {
            castillafantasx.datos.Candado.vigilar(archivo);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar pantalla.txt: " + e.getMessage());
        }
    }

    /** ¿Está todo lo de pantalla.txt como de fábrica? */
    public static boolean deFabrica() {
        return completa && sinBordes && ventanaAncho == VENTANA_ANCHO && ventanaAlto == VENTANA_ALTO && aumentoElegido == 0 && retratos
                && interfaz == Interfaz.NINGUNA && !mirarConRaton && letrasBuscan && campo == Campo.NORMAL && vistaEntera;
    }

    /** Todo lo de pantalla.txt, como de fábrica (y guardado). */
    public static void aFabrica() {
        try {
            java.nio.file.Files.deleteIfExists(archivo);
        } catch (IOException e) {
            System.err.println("No se pudo quitar pantalla.txt: " + e.getMessage());
        }
        cargar();
        guardar();
    }

    /** Lee pantalla.txt. Lo que falte o esté mal escrito se queda como de fábrica. */
    public static void cargar() {
        completa = true;
        sinBordes = true;
        ventanaAncho = VENTANA_ANCHO;
        ventanaAlto = VENTANA_ALTO;
        aumentoElegido = 0;
        retratos = true;
        interfaz = Interfaz.NINGUNA;
        mirarConRaton = false;
        letrasBuscan = true;
        campo = Campo.NORMAL;
        vistaEntera = true;
        if (!Files.exists(archivo)) return;
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                int igual = linea.indexOf('=');
                if (linea.trim().startsWith("#") || igual < 0) continue;
                String clave = linea.substring(0, igual).trim(), valor = linea.substring(igual + 1).trim();
                if (clave.equals("completa")) {
                    completa = valor.equals("si");
                } else if (clave.equals("sin_bordes")) {
                    sinBordes = !valor.equals("no");
                } else if (clave.equals("ventana")) {
                    String[] lados = valor.split("x");
                    ventanaAncho = Math.max(ANCHO_MINIMO, Integer.parseInt(lados[0]));
                    ventanaAlto = Math.max(ALTO_MINIMO, Integer.parseInt(lados[1]));
                } else if (clave.equals("retratos")) {
                    retratos = !valor.equals("no");
                } else if (clave.equals("interfaz")) {
                    interfaz = Interfaz.valueOf(valor.toUpperCase());
                } else if (clave.equals("mirar")) {
                    mirarConRaton = valor.equals("raton");
                } else if (clave.equals("letras")) {
                    letrasBuscan = !valor.equals("mueven");
                } else if (clave.equals("campo")) {
                    campo = Campo.valueOf(valor.toUpperCase());
                } else if (clave.equals("vista")) {
                    vistaEntera = !valor.equals("mitad");
                } else if (clave.equals("aumento")) {
                    aumentoElegido = Math.max(0, Integer.parseInt(valor));
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("pantalla.txt no se pudo leer; se usa lo de fábrica.");
        }
    }
}
