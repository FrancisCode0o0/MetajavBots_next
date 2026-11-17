package metajavbots.codigo;

import java.lang.reflect.Modifier;
import java.util.Locale;

import metajavbots.api.RobotBase;
import metajavbots.combate.Fabrica;

/**
 * ROBOT DEL JUGADOR — La Fábrica del robot que programa el jugador: guarda
 * su código compilado y crea su robot cuando la pelea lo pide. Cada vez que
 * se compila bien, sube la versión: la ronda siguiente estrena el robot nuevo
 * (la memoria() se conserva).
 */
public final class RobotDelJugador implements Fabrica {

    private String fuente = "";
    private Cargador cargador;
    private String clase = "";
    private long version;
    private Compilador.Resultado ultimo;

    /** Compila el código; si sale bien, la próxima ronda usa este robot. Devuelve el resultado (con sus errores). */
    public Compilador.Resultado compilar(String codigo, Locale idioma) {
        Compilador.Resultado r = Compilador.compilar(codigo, true, Vigilante.Modo.ROBOT, idioma);
        ultimo = r;
        if (!r.bien) return r;
        Cargador nuevo = new Cargador(r.clases, Vigilante.Modo.ROBOT);
        String elegida = null;
        for (String n : r.clases.keySet()) {
            try {
                Class<?> c = nuevo.loadClass(n);
                if (RobotBase.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()) && Modifier.isPublic(c.getModifiers())) {
                    if (elegida == null || n.equals(Compilador.nombreDeClase(codigo))) elegida = n;
                }
            } catch (ClassNotFoundException | LinkageError e) {
                // la revisará el robot al crearse
            }
        }
        if (elegida == null) {
            java.util.List<Compilador.Fallo> l = new java.util.ArrayList<>(r.errores);
            l.add(new Compilador.Fallo(Compilador.nombreDeClase(codigo), 1, 1, "metajavbots.sin.robot",
                    "No hay ninguna clase pública que herede de Metajavbot (o MetajavbotDe, MetajavbotEn)", true));
            ultimo = new Compilador.Resultado(false, l, java.util.Map.of(), r.milisegundos);
            return ultimo;
        }
        fuente = codigo;
        cargador = nuevo;
        clase = elegida;
        version++;
        return r;
    }

    public Compilador.Resultado ultimo() {
        return ultimo;
    }

    public String fuente() {
        return fuente;
    }

    public boolean listo() {
        return cargador != null;
    }

    @Override
    public RobotBase crear() throws Exception {
        if (cargador == null) throw new IllegalStateException("El robot aún no se ha compilado");
        Class<?> c = cargador.loadClass(clase);
        return (RobotBase) c.getDeclaredConstructor().newInstance();
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public String clase() {
        return clase;
    }

    @Override
    public boolean esSuya(String nombreDeClase) {
        return cargador != null && cargador.esSuya(nombreDeClase.contains("$") ? nombreDeClase.substring(0, nombreDeClase.indexOf('$')) : nombreDeClase)
                || cargador != null && cargador.esSuya(nombreDeClase);
    }
}
