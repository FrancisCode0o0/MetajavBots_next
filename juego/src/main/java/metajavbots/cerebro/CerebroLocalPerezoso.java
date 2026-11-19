package metajavbots.cerebro;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CEREBRO LOCAL PEREZOSO — Un modelo local no gasta memoria mientras no se
 * usa: la primera pregunta arranca su KoboldCpp (si la dirección es una de
 * las del juego y no hay nadie escuchando) con el modelo elegido, y un
 * vigilante apaga cada uno tras unos minutos sin preguntas, y todos al cerrar
 * el juego. Si escucha otro (el de Jarvis), se usa sin arrancar ni apagar nada.
 */
public final class CerebroLocalPerezoso implements Cerebro {

    /** Minutos sin preguntas tras los que se apaga un KoboldCpp que arrancó el juego. */
    public static final long MINUTOS_PARA_APAGAR = 8;
    private static Thread vigilante;

    private final String direccion;
    private final Path carpeta;
    private final String modeloPreferido;
    private final int puerto;
    private final CerebroHttp http;

    public CerebroLocalPerezoso(String direccion, Path carpeta, String modeloPreferido) {
        this.direccion = direccion;
        this.carpeta = carpeta;
        this.modeloPreferido = modeloPreferido;
        this.puerto = puertoDe(direccion);
        this.http = new CerebroHttp(CerebroHttp.Forma.OPENAI, direccion, "", "", "KoboldCpp");
    }

    /** El puerto de una dirección como «http://localhost:5002» (o -1). */
    static int puertoDe(String direccion) {
        int i = direccion.lastIndexOf(':');
        try {
            return Integer.parseInt(direccion.substring(i + 1).replaceAll("[^0-9].*", ""));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    @Override
    public String nombre() {
        String m = modeloPreferido == null || modeloPreferido.isBlank() ? "" : " · " + modeloPreferido.replace(".gguf", "");
        return "KoboldCpp" + m;
    }

    /** ¿Lo arranca el juego? (los puertos del juego empiezan en el 5002; el de Jarvis, no). */
    private boolean esDelJuego() {
        return direccion.contains("localhost") && puerto >= CerebroLocal.PUERTO_DEL_JUEGO && puerto < CerebroLocal.PUERTO_DEL_JUEGO + 10;
    }

    @Override
    public String preguntar(String sistema, List<Mensaje> conversacion, int maximoDePalabras) throws Exception {
        CerebroLocal.usado(puerto);
        if (!CerebroLocal.escucha(direccion)) {
            if (!esDelJuego()) throw new IllegalStateException("no hay nadie escuchando en " + direccion);
            String r = CerebroLocal.arrancarElQueFuncione(carpeta, modelos(), puerto, null);
            if (!r.equals("ya")) throw new IllegalStateException("el cerebro local no arranca: " + r);
            vigilar();
        }
        try {
            return http.preguntar(sistema, conversacion, maximoDePalabras);
        } finally {
            CerebroLocal.usado(puerto);
        }
    }

    /** Los modelos de la carpeta, el elegido primero. */
    private List<Path> modelos() {
        List<Path> l = new ArrayList<>(CerebroLocal.modelos(carpeta));
        if (modeloPreferido != null && !modeloPreferido.isBlank()) {
            Path p = carpeta.resolve(modeloPreferido);
            l.remove(p);
            if (Files.isRegularFile(p)) l.add(0, p);
        }
        return l;
    }

    private static synchronized void vigilar() {
        if (vigilante != null && vigilante.isAlive()) return;
        vigilante = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    return;
                }
                if (!CerebroLocal.apagarLosQuietos(MINUTOS_PARA_APAGAR)) return;
            }
        }, "apagar-cerebros-locales");
        vigilante.setDaemon(true);
        vigilante.start();
    }

    /** Detiene el hilo vigilante al cerrar el juego. */
    public static synchronized void cerrar() {
        if (vigilante != null) {
            vigilante.interrupt();
            vigilante = null;
        }
    }
}
