package metajavbots.combate;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Map;

import metajavbots.api.Puente;
import metajavbots.api.RobotBase;
import metajavbots.api.TopeDeCalculo;

/**
 * PRESUPUESTO SENIOR — Medición segura, portable y 100 % determinista del
 * presupuesto de tiempo (nanosegundos por turno) y memoria/asignaciones (bytes
 * asignados + memoria retenida en campos y {@code memoria()}) en las peleas de
 * nivel Senior, sin depender de APIs prohibidas ni del recolector de basura.
 */
public final class PresupuestoSenior {

    /** Margen base gratuito (en bytes) para campos normales de un robot bien diseñado. */
    public static final long BASE_LIBRE_BYTES = 512L;
    private static final int MAX_NODOS = 512;
    private static final int MAX_PROFUNDIDAD = 6;

    private PresupuestoSenior() {
    }

    /**
     * Estima de forma determinista la memoria retenida (en bytes) por la instancia
     * del robot y su mapa {@code memoria()}. Usa {@link IdentityHashMap} para evitar
     * ciclos y topes de profundidad y nodos para mantener un coste acotado.
     */
    public static long estimarMemoriaRetenida(RobotBase robot, Map<String, Object> memoria) {
        IdentityHashMap<Object, Boolean> vistos = new IdentityHashMap<>();
        int[] nodos = {0};
        long total = 0;
        if (robot != null) total += medirObjeto(robot, vistos, nodos, 0, true);
        if (memoria != null && !memoria.isEmpty()) total += medirObjeto(memoria, vistos, nodos, 0, false);
        return total;
    }

    private static long medirObjeto(Object obj, IdentityHashMap<Object, Boolean> vistos, int[] nodos, int prof, boolean esRobot) {
        if (obj == null || prof > MAX_PROFUNDIDAD || nodos[0] >= MAX_NODOS) return 0;
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character) return 16;
        if (obj instanceof String) return 40L + ((String) obj).length() * 2L;
        if (vistos.put(obj, Boolean.TRUE) != null) return 0;
        nodos[0]++;
        Class<?> c = obj.getClass();
        if (c.isArray()) return medirArray(obj, c.getComponentType(), vistos, nodos, prof);
        if (obj instanceof Collection<?>) return medirColeccion((Collection<?>) obj, vistos, nodos, prof);
        if (obj instanceof Map<?, ?>) return medirMapa((Map<?, ?>) obj, vistos, nodos, prof);
        long bytes = 24;
        Class<?> actual = c;
        while (actual != null && actual != Object.class && (!esRobot || !actual.getName().startsWith("metajavbots.api."))) {
            for (Field f : actual.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                Class<?> t = f.getType();
                if (t.isPrimitive()) {
                    bytes += tamanoPrimitivo(t);
                    continue;
                }
                bytes += 8;
                try {
                    f.setAccessible(true);
                    Object v = f.get(obj);
                    if (v != null) bytes += medirObjeto(v, vistos, nodos, prof + 1, false);
                } catch (RuntimeException | IllegalAccessException e) {
                    // campo inaccesible: se contabiliza solo la referencia
                }
            }
            actual = actual.getSuperclass();
        }
        return bytes;
    }

    private static long medirArray(Object arr, Class<?> comp, IdentityHashMap<Object, Boolean> vistos, int[] nodos, int prof) {
        int len = Array.getLength(arr);
        if (comp.isPrimitive()) return 24L + (long) len * tamanoPrimitivo(comp);
        long bytes = 24L + (long) len * 8L;
        int limite = Math.min(len, 128);
        for (int i = 0; i < limite && nodos[0] < MAX_NODOS; i++) {
            bytes += medirObjeto(Array.get(arr, i), vistos, nodos, prof + 1, false);
        }
        return bytes;
    }

    private static long medirColeccion(Collection<?> col, IdentityHashMap<Object, Boolean> vistos, int[] nodos, int prof) {
        long bytes = 48L + (long) col.size() * 16L;
        int vistosAqui = 0;
        try {
            for (Object elem : col) {
                if (++vistosAqui > 128 || nodos[0] >= MAX_NODOS) break;
                bytes += medirObjeto(elem, vistos, nodos, prof + 1, false);
            }
        } catch (RuntimeException e) {
            // si una colección mutó concurrentemente, conservamos la estimación base
        }
        return bytes;
    }

    private static long medirMapa(Map<?, ?> map, IdentityHashMap<Object, Boolean> vistos, int[] nodos, int prof) {
        long bytes = 64L + (long) map.size() * 32L;
        int vistosAqui = 0;
        try {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (++vistosAqui > 128 || nodos[0] >= MAX_NODOS) break;
                bytes += medirObjeto(e.getKey(), vistos, nodos, prof + 1, false);
                bytes += medirObjeto(e.getValue(), vistos, nodos, prof + 1, false);
            }
        } catch (RuntimeException e) {
            // mapa mutado concurrentemente: conservamos la estimación base
        }
        return bytes;
    }

    private static int tamanoPrimitivo(Class<?> t) {
        if (t == double.class || t == long.class) return 8;
        if (t == int.class || t == float.class) return 4;
        if (t == short.class || t == char.class) return 2;
        return 1;
    }

    /**
     * Actualiza la telemetría de tiempo/memoria del turno y aplica penalizaciones
     * claras en combate cuando se supera el presupuesto Senior configurado.
     */
    static void evaluarTurno(Pelea pelea, Cuerpo c, long nanosUsados, long bytesAsignados, String motivoTope) {
        Combatiente.Informe inf = pelea.informe(c.indice);
        long retenida = pelea.presupuestoBytesPorTurno > 0
                ? estimarMemoriaRetenida(c.combatiente.robot, c.combatiente.memoria) : 0L;
        long excesoRetenida = Math.max(0L, retenida - BASE_LIBRE_BYTES);
        long bytesTurno = bytesAsignados + excesoRetenida;
        c.nanosDelTurno = nanosUsados;
        c.bytesDelTurno = bytesTurno;
        c.memoriaRetenida = retenida;
        inf.nanosTotales += nanosUsados;
        inf.nanosMaximos = Math.max(inf.nanosMaximos, nanosUsados);
        inf.bytesMaximos = Math.max(inf.bytesMaximos, bytesTurno);
        boolean pasoTiempo = pelea.presupuestoNanosPorTurno > 0
                && (nanosUsados > pelea.presupuestoNanosPorTurno || "tiempo".equals(motivoTope));
        if (pasoTiempo) {
            inf.excesosTiempo++;
            c.deuda = Math.min(20, c.deuda + 2);
            c.energia = Math.max(0, c.energia - 6.0);
            if (inf.excesosTiempo <= 3 || inf.excesosTiempo % 20 == 0) {
                c.combatiente.escribir(pelea.turno(), String.format(java.util.Locale.ROOT,
                        "⏱ Presupuesto de tiempo superado (%.2f ms / %.2f ms): -6 energía y +%d turnos de latencia.",
                        nanosUsados / 1_000_000.0, pelea.presupuestoNanosPorTurno / 1_000_000.0, c.deuda));
            }
            pelea.suceso(Suceso.Tipo.TOPE, c.indice, c.x, c.y, 0, c.deuda, "tiempo");
        }
        boolean pasoMemoria = pelea.presupuestoBytesPorTurno > 0
                && (bytesTurno > pelea.presupuestoBytesPorTurno
                || (motivoTope != null && motivoTope.startsWith("memoria")));
        if (pasoMemoria) {
            inf.excesosMemoria++;
            c.deuda = Math.min(20, c.deuda + 2);
            c.energia = Math.max(0, c.energia - 6.0);
            if (inf.excesosMemoria <= 3 || inf.excesosMemoria % 20 == 0) {
                c.combatiente.escribir(pelea.turno(), String.format(java.util.Locale.ROOT,
                        "⚠ Presupuesto de memoria superado (%d B / %d B): pausa de GC (-6 energía, %d turnos).",
                        bytesTurno, pelea.presupuestoBytesPorTurno, c.deuda));
            }
            pelea.suceso(Suceso.Tipo.TOPE, c.indice, c.x, c.y, 0, c.deuda, "memoria");
        }
    }

    /** Ejecuta código del robot con sus límites de pasos, tiempo y memoria; devuelve los pasos dados. */
    static long ejecutar(Pelea pelea, Cuerpo c, Runnable r, long tope, boolean esTurno) {
        long limiteNanos = pelea.limiteTiempoEfectivo();
        long maxBytes = esTurno && pelea.presupuestoBytesPorTurno > 0 ? pelea.presupuestoBytesPorTurno * 4L : Long.MAX_VALUE;
        Puente.contar(tope, limiteNanos, maxBytes);
        c.cuentaActiva = Puente.cuentaActual();
        metajavbots.codigo.Salida.capturar();
        long t0 = System.nanoTime();
        long pasos = 0;
        long bytesAsignados = 0;
        String motivoTope = null;
        try {
            try {
                r.run();
            } catch (TopeDeCalculo t) {
                motivoTope = t.getMessage();
                if (motivoTope == null || (!motivoTope.equals("tiempo") && !motivoTope.startsWith("memoria"))) {
                    Combatiente.Informe inf = pelea.informe(c.indice);
                    inf.topes++;
                    c.deuda = Math.min(20, c.deuda + 2 + inf.topes / 3);
                    if (inf.topes <= 3 || inf.topes % 20 == 0) {
                        c.combatiente.escribir(pelea.turno(), "⏱ Tope de cálculo (" + tope + " pasos de cálculo por turno): el robot se queda pensando "
                                + c.deuda + " turnos.");
                    }
                    pelea.suceso(Suceso.Tipo.TOPE, c.indice, c.x, c.y, 0, c.deuda, "");
                }
            } catch (StackOverflowError e) {
                error(pelea, c, e, "recursión sin final (StackOverflowError)");
            } catch (OutOfMemoryError e) {
                error(pelea, c, e, "sin memoria");
            } catch (Throwable e) {
                error(pelea, c, e, null);
            }
            java.util.BitSet lineas = Puente.lineas();
            if (lineas != null && !lineas.isEmpty()) {
                c.lineasDelTurno = lineas;
                c.ultimaLinea = Puente.ultimaLinea();
            }
            bytesAsignados = Puente.bytesAsignados();
        } finally {
            c.cuentaActiva = null;
            try {
                String escrito = metajavbots.codigo.Salida.soltar();
                if (!escrito.isEmpty()) c.combatiente.escribir(pelea.turno(), escrito.stripTrailing());
            } finally {
                pasos = Puente.dejarDeContar();
            }
        }
        long duracionNanos = System.nanoTime() - t0;
        if (esTurno) evaluarTurno(pelea, c, duracionNanos, bytesAsignados, motivoTope);
        return pasos;
    }

    /** Registra un error de ejecución del robot, contabiliza nulos/concurrencia y aturde brevemente al robot. */
    static void error(Pelea pelea, Cuerpo c, Throwable e, String que) {
        Combatiente co = c.combatiente;
        Combatiente.Informe inf = pelea.informe(c.indice);
        inf.errores++;
        Throwable raiz = e;
        if (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null) raiz = e.getCause();
        if (raiz instanceof NullPointerException) inf.fallosNulos++;
        if (raiz instanceof ConcurrentModificationException) inf.erroresConcurrencia++;
        int linea = -1;
        for (StackTraceElement s : raiz.getStackTrace()) {
            if (co.fabrica != null && co.fabrica.esSuya(s.getClassName())) {
                linea = s.getLineNumber();
                break;
            }
        }
        String texto = (que != null ? que + ": " : "") + raiz.getClass().getSimpleName()
                + (raiz.getMessage() != null ? ": " + raiz.getMessage() : "") + (linea > 0 ? " (línea " + linea + ")" : "");
        co.ultimoError = texto;
        co.lineaDelUltimoError = linea;
        if (inf.errores <= 5 || inf.errores % 50 == 0) co.escribir(pelea.turno(), "⚠ " + texto);
        c.aturdido = Math.max(c.aturdido, 8);
        pelea.suceso(Suceso.Tipo.ERROR_CODIGO, c.indice, c.x, c.y, 0, linea, texto);
    }
}
