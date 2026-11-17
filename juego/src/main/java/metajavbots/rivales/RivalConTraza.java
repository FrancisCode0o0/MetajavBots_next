package metajavbots.rivales;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import metajavbots.api.RobotBase;
import metajavbots.codigo.Cargador;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.combate.Fabrica;

/**
 * RIVAL CON TRAZA — El rival de la computadora compilado desde su código
 * (res: codigo_rival/Rival.java) con la traza puesta: cada sentencia apunta
 * su línea al ejecutarse. Así el modo máquina contra máquina enseña qué
 * línea corre en cada momento. Es el mismo Rival que pelea siempre.
 */
public final class RivalConTraza {

    private static Map<String, byte[]> compilado;
    private static String fuente;

    private RivalConTraza() {
    }

    public static synchronized String fuente() {
        if (fuente == null) {
            try (InputStream in = RivalConTraza.class.getResourceAsStream("/codigo_rival/Rival.java")) {
                fuente = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                fuente = "";
            }
        }
        return fuente;
    }

    private static synchronized Map<String, byte[]> compilado() {
        if (compilado == null) {
            Map<String, String> f = new LinkedHashMap<>();
            f.put("Rival", fuente());
            Compilador.Resultado r = Compilador.compilar(f, true, true, Vigilante.Modo.CONFIANZA, Locale.ROOT);
            if (!r.bien) throw new IllegalStateException("el rival no compila con traza: " + r.errores);
            compilado = r.clases;
        }
        return compilado;
    }

    /** Una fábrica de rivales con traza (null si no hay JDK: entonces, sin traza). */
    public static Fabrica fabrica(Ajuste ajuste, Estilo estilo) {
        if (!Compilador.hayCompilador()) return null;
        Map<String, byte[]> clases = compilado();
        return new Fabrica() {
            private Cargador cargador;

            @Override
            public RobotBase crear() throws Exception {
                if (cargador == null) cargador = new Cargador(clases, Vigilante.Modo.CONFIANZA);
                Class<?> c = cargador.loadClass("metajavbots.rivales.Rival");
                return (RobotBase) c.getConstructor(Ajuste.class, Estilo.class).newInstance(ajuste, estilo);
            }

            @Override
            public String clase() {
                return "Rival";
            }

            @Override
            public boolean esSuya(String nombre) {
                return nombre.startsWith("metajavbots.rivales.Rival");
            }
        };
    }
}
