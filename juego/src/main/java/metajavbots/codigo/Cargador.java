package metajavbots.codigo;

import java.util.Map;
import java.util.Set;

/**
 * CARGADOR — Carga las clases compiladas del jugador. Para todo lo demás,
 * solo deja pasar lo que el Vigilante permite (la segunda barandilla: aunque
 * algo se colase en la compilación, aquí no se carga).
 */
public final class Cargador extends ClassLoader {

    private final Map<String, byte[]> clases;
    private final Vigilante.Modo modo;

    public Cargador(Map<String, byte[]> clases, Vigilante.Modo modo) {
        super(Cargador.class.getClassLoader());
        this.clases = clases;
        this.modo = modo;
    }

    public Set<String> nombres() {
        return clases.keySet();
    }

    public boolean esSuya(String nombre) {
        return clases.containsKey(nombre);
    }

    @Override
    protected Class<?> loadClass(String nombre, boolean resolver) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(nombre)) {
            Class<?> c = findLoadedClass(nombre);
            if (c == null) {
                if (clases.containsKey(nombre)) {
                    byte[] b = clases.get(nombre);
                    c = defineClass(nombre, b, 0, b.length);
                } else if (Vigilante.clasePermitida(nombre.replace('.', '/'), modo) || internaPermitida(nombre)) {
                    c = getParent().loadClass(nombre);
                } else {
                    throw new ClassNotFoundException(nombre + " (no se puede usar aquí)");
                }
            }
            if (resolver) resolveClass(c);
            return c;
        }
    }

    /** Lo que las fachadas necesitan por dentro aunque el jugador no lo nombre. */
    private static boolean internaPermitida(String nombre) {
        return nombre.equals("metajavbots.api.Avistado") || nombre.equals("metajavbots.api.Impacto");
    }
}
