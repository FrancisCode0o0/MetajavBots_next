package castillafantasx;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CIERRE — Si un error tumba el juego, lo deja escrito en registro/AAAA-MM-DD_HH-MM.txt (junto al juego):
 * qué pasó, la traza entera, dónde y cómo estaba la partida y la versión (el commit).
 */
public final class Cierre {

    private Cierre() {
    }

    /** La carpeta de los registros (relativa a donde se lanza el juego). Las pruebas la cambian por una temporal. */
    public static Path carpeta = Paths.get("registro");
    /** Hasta dónde mira para contar las criaturas y personas «cerca»: casillas en la mazmorra, píxeles arriba. */
    /** Si también se escribe en la consola (las pruebas lo apagan para no llenarla). */
    public static boolean enConsola = true;
    /** Cómo acaba el nombre de la copia de la partida que se guarda junto al registro (Juego.caer). */
    public static final String COPIA_DE_LA_PARTIDA = "_partida.txt";

    /** Escribe el registro del error y devuelve su ruta entera, o null si no se ha podido escribir. */
    public static Path apuntar(Throwable error, LaPartida partida) {
        String texto = texto(error, partida);
        if (enConsola) System.err.println(texto);       // también a la consola, por si no se puede escribir el archivo
        try {
            Files.createDirectories(carpeta);
            String hora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            Path archivo = carpeta.resolve(hora + ".txt");
            // Dos cierres en el mismo minuto: el segundo no pisa al primero.
            for (int n = 2; Files.exists(archivo); n++) archivo = carpeta.resolve(hora + "_" + n + ".txt");
            castillafantasx.datos.Candado.vigilar(archivo);
            Files.write(archivo, texto.getBytes(StandardCharsets.UTF_8));
            return archivo.toAbsolutePath();
        } catch (IOException | RuntimeException e) {
            System.err.println("No se ha podido escribir el registro: " + e);
            return null;
        }
    }

    /** Todo lo que va en el registro. */
    public static String texto(Throwable error, LaPartida partida) {
        StringBuilder t = new StringBuilder();
        t.append(castillafantasx.datos.ElJuego.nombre()).append(": el juego se ha cerrado por un error.\n");
        t.append("Cuándo: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))).append('\n');
        t.append("Versión: ").append(version()).append('\n');
        t.append("Java: ").append(System.getProperty("java.version")).append(", ").append(System.getProperty("os.name")).append("\n\n");
        t.append("QUÉ PASÓ\n").append(error).append("\n\n");
        t.append("LA PARTIDA\n");
        // Cada parte por separado: si la partida está rota, lo que se pueda leer se apunta igual.
        try {
            t.append(partida == null ? "Aún no había partida (el error llegó al cargar).\n" : partida.estadoParaElRegistro());
        } catch (RuntimeException e) {
            t.append("(no se ha podido leer entera: ").append(e).append(")\n");
        }
        StringWriter traza = new StringWriter();
        error.printStackTrace(new PrintWriter(traza));
        t.append("\nDÓNDE (la traza entera)\n").append(traza);
        return t.toString();
    }

    /** La versión: el commit con el que se compiló (version.txt, lo escribe Gradle al compilar). */
    public static String version() {
        try (InputStream entrada = Cierre.class.getResourceAsStream("/version.txt")) {
            if (entrada != null) return new String(entrada.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            // sin versión: se dice abajo
        }
        return "desconocida";
    }
}
