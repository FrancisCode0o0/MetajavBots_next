package metajavbots.codigo;

import java.lang.reflect.Modifier;
import java.util.Locale;

import metajavbots.api.RobotBase;
import metajavbots.combate.Fabrica;
import metajavbots.progreso.Jugador;

/**
 * ROBOT DEL JUGADOR — La Fábrica del robot que programa el jugador: guarda
 * su código compilado y crea su robot cuando la pelea lo pide. Cada vez que
 * se compila bien, sube la versión: la ronda siguiente estrena el robot nuevo
 * (la memoria() se conserva).
 */
public final class RobotDelJugador implements Fabrica {

    /** Estado resultante al cargar el robot del jugador antes de un combate o reto. */
    public enum EstadoDeCarga {
        /** El código actual del taller coincide con la última compilación válida. */
        COMPILADO_ACTUAL,
        /** El código del taller tiene cambios sin compilar o con errores; se usa la última versión válida. */
        ULTIMA_VERSION_VALIDA,
        /** El robot nunca se ha compilado con éxito: entra en la arena sin comportamiento programado. */
        SIN_COMPILAR,
        /** No hay JDK disponible para compilar el código verificado. */
        SIN_JDK
    }

    private String fuente = "";
    private Cargador cargador;
    private String clase = "";
    private long version;
    private boolean conTraza;
    private Compilador.Resultado ultimo;
    private AnalizadorDeBytecode.Informe bytecode;

    /** Limpia la versión compilada en memoria (deja el robot sin código compilado). */
    public void limpiar() {
        fuente = "";
        cargador = null;
        clase = "";
        conTraza = false;
        ultimo = null;
        bytecode = null;
    }

    /**
     * Carga en esta fábrica únicamente la última versión válida compilada del robot del jugador.
     * Si el robot nunca ha sido compilado con éxito, queda sin código (no se compila implícitamente ni usa piloto automático).
     */
    public EstadoDeCarga cargarDesdeJugador(Jugador.Robot r, Locale idioma, boolean traza) {
        limpiar();
        if (r == null || !r.tieneCodigoCompilado()) return EstadoDeCarga.SIN_COMPILAR;
        if (!Compilador.hayCompilador()) return EstadoDeCarga.SIN_JDK;
        Compilador.Resultado res = compilar(r.codigoCompilado, idioma == null ? Locale.ROOT : idioma, traza);
        if (!res.bien) return EstadoDeCarga.SIN_COMPILAR;
        return r.tieneCambiosPendientes() ? EstadoDeCarga.ULTIMA_VERSION_VALIDA : EstadoDeCarga.COMPILADO_ACTUAL;
    }

    /** Devuelve esta fábrica si tiene código compilado listo, o {@code null} si el robot no tiene programa compilado. */
    public Fabrica fabricaSiListo() {
        return listo() ? this : null;
    }

    /** Compila el código; si sale bien, la próxima ronda usa este robot. Devuelve el resultado (con sus errores). */
    public Compilador.Resultado compilar(String codigo, Locale idioma) {
        return compilar(codigo, idioma, false);
    }

    /** Compila el código; con traza, cada sentencia apunta su línea al ejecutarse (para resaltarlas en el editor). */
    public Compilador.Resultado compilar(String codigo, Locale idioma, boolean traza) {
        Compilador.Resultado r = Compilador.compilar(java.util.Map.of(Compilador.nombreDeClase(codigo), codigo),
                true, traza, Vigilante.Modo.ROBOT, idioma);
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
        conTraza = traza;
        bytecode = AnalizadorDeBytecode.analizarTodas(r.clases);
        version++;
        return r;
    }

    /** Métricas reales del bytecode compilado del robot, o {@code null} si aún no compiló. */
    public AnalizadorDeBytecode.Informe bytecode() {
        return bytecode;
    }

    public boolean conTraza() {
        return conTraza;
    }

    public Compilador.Resultado ultimo() {
        return ultimo;
    }

    @Override
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
