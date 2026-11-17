package metajavbots.cerebro;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CEREBRO LOCAL PEREZOSO — El modelo local no gasta memoria mientras no se
 * usa: la primera pregunta arranca KoboldCpp (si la dirección es la del
 * juego y no hay nadie escuchando) con el modelo elegido, y un vigilante lo
 * apaga tras unos minutos sin preguntas, y al cerrar el juego. Si escucha
 * otro (el de Jarvis), se usa sin arrancar ni apagar nada.
 */
public final class CerebroLocalPerezoso implements Cerebro {

    /** Minutos sin preguntas tras los que se apaga el KoboldCpp que arrancó el juego. */
    public static final long MINUTOS_PARA_APAGAR = 8;
    private static volatile long ultimoUso;
    private static Thread vigilante;

    private final String direccion;
    private final Path carpeta;
    private final String modeloPreferido;
    private final CerebroHttp http;

    public CerebroLocalPerezoso(String direccion, Path carpeta, String modeloPreferido) {
        this.direccion = direccion;
        this.carpeta = carpeta;
        this.modeloPreferido = modeloPreferido;
        this.http = new CerebroHttp(CerebroHttp.Forma.OPENAI, direccion, "", "", "KoboldCpp");
    }

    @Override
    public String nombre() {
        return "KoboldCpp";
    }

    private boolean esDelJuego() {
        return direccion.contains(":" + CerebroLocal.PUERTO_DEL_JUEGO);
    }

    @Override
    public String preguntar(String sistema, List<Mensaje> conversacion, int maximoDePalabras) throws Exception {
        ultimoUso = System.currentTimeMillis();
        if (!CerebroLocal.escucha(direccion)) {
            if (!esDelJuego()) throw new IllegalStateException("no hay nadie escuchando en " + direccion);
            String r = CerebroLocal.arrancarElQueFuncione(carpeta, modelos(), null);
            if (!r.equals("ya")) throw new IllegalStateException("el cerebro local no arranca: " + r);
            vigilar();
        }
        try {
            return http.preguntar(sistema, conversacion, maximoDePalabras);
        } finally {
            ultimoUso = System.currentTimeMillis();
        }
    }

    /** Los modelos de la carpeta, el elegido primero. */
    private List<Path> modelos() {
        List<Path> l = new ArrayList<>(CerebroLocal.modelos(carpeta));
        if (modeloPreferido != null && !modeloPreferido.isBlank()) {
            Path p = carpeta.resolve(modeloPreferido);
            l.remove(p);
            if (java.nio.file.Files.isRegularFile(p)) l.add(0, p);
        }
        return l;
    }

    private static synchronized void vigilar() {
        if (vigilante != null && vigilante.isAlive()) return;
        vigilante = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    return;
                }
                if (System.currentTimeMillis() - ultimoUso > MINUTOS_PARA_APAGAR * 60_000) {
                    CerebroLocal.parar();
                    return;
                }
            }
        }, "apagar-cerebro-local");
        vigilante.setDaemon(true);
        vigilante.start();
    }
}
