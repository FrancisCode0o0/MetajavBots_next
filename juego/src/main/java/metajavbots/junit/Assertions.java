package metajavbots.junit;

import java.util.Arrays;
import java.util.Objects;

/**
 * ASSERTIONS — Las comprobaciones de las pruebas, con los mismos nombres
 * que en JUnit 5 (import static metajavbots.junit.Assertions.*;).
 */
public final class Assertions {

    private Assertions() {
    }

    private static String pre(String mensaje) {
        return mensaje == null || mensaje.isEmpty() ? "" : mensaje + " ==> ";
    }

    public static void assertEquals(Object esperado, Object real) {
        assertEquals(esperado, real, null);
    }

    public static void assertEquals(Object esperado, Object real, String mensaje) {
        if (esperado instanceof Number && real instanceof Number && !(esperado instanceof Double) && !(real instanceof Double)
                && !(esperado instanceof Float) && !(real instanceof Float)) {
            if (((Number) esperado).longValue() == ((Number) real).longValue()) return;
        } else if (Objects.equals(esperado, real)) {
            return;
        }
        throw new AssertionFailedError(pre(mensaje) + "expected: <" + esperado + "> but was: <" + real + ">");
    }

    public static void assertEquals(double esperado, double real, double delta) {
        assertEquals(esperado, real, delta, null);
    }

    public static void assertEquals(double esperado, double real, double delta, String mensaje) {
        boolean distintos = Double.isNaN(esperado) != Double.isNaN(real) || Math.abs(esperado - real) > delta;
        if (distintos) {
            throw new AssertionFailedError(pre(mensaje) + "expected: <" + esperado + "> but was: <" + real + "> (delta " + delta + ")");
        }
    }

    public static void assertNotEquals(Object noEsperado, Object real) {
        if (Objects.equals(noEsperado, real)) throw new AssertionFailedError("expected: not equal but was: <" + real + ">");
    }

    public static void assertTrue(boolean condicion) {
        assertTrue(condicion, null);
    }

    public static void assertTrue(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionFailedError(pre(mensaje) + "expected: <true> but was: <false>");
    }

    public static void assertFalse(boolean condicion) {
        assertFalse(condicion, null);
    }

    public static void assertFalse(boolean condicion, String mensaje) {
        if (condicion) throw new AssertionFailedError(pre(mensaje) + "expected: <false> but was: <true>");
    }

    public static void assertNull(Object o) {
        if (o != null) throw new AssertionFailedError("expected: <null> but was: <" + o + ">");
    }

    public static void assertNotNull(Object o) {
        if (o == null) throw new AssertionFailedError("expected: not <null>");
    }

    public static void assertArrayEquals(int[] esperado, int[] real) {
        if (!Arrays.equals(esperado, real)) {
            throw new AssertionFailedError("array contents differ: expected " + Arrays.toString(esperado) + " but was " + Arrays.toString(real));
        }
    }

    public static void assertArrayEquals(double[] esperado, double[] real, double delta) {
        if (esperado == null || real == null || esperado.length != real.length) {
            throw new AssertionFailedError("array lengths differ: " + Arrays.toString(esperado) + " vs " + Arrays.toString(real));
        }
        for (int i = 0; i < esperado.length; i++) assertEquals(esperado[i], real[i], delta, "index " + i);
    }

    public static void assertArrayEquals(Object[] esperado, Object[] real) {
        if (!Arrays.deepEquals(esperado, real)) {
            throw new AssertionFailedError("array contents differ: expected " + Arrays.deepToString(esperado) + " but was " + Arrays.deepToString(real));
        }
    }

    public static <T extends Throwable> T assertThrows(Class<T> tipo, Executable codigo) {
        try {
            codigo.execute();
        } catch (Throwable t) {
            if (tipo.isInstance(t)) return tipo.cast(t);
            throw new AssertionFailedError("expected " + tipo.getSimpleName() + " but " + t.getClass().getSimpleName() + " was thrown");
        }
        throw new AssertionFailedError("expected " + tipo.getSimpleName() + " to be thrown, but nothing was thrown");
    }

    public static void assertDoesNotThrow(Executable codigo) {
        try {
            codigo.execute();
        } catch (Throwable t) {
            throw new AssertionFailedError("unexpected exception: " + t);
        }
    }

    public static void fail(String mensaje) {
        throw new AssertionFailedError(mensaje);
    }
}
