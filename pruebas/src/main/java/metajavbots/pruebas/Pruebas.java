package metajavbots.pruebas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PRUEBAS — El banco de pruebas sin ventana: «gradlew pruebas:prueba» (o
 * «jugar.bat prueba»). Cada grupo apunta sus casos con caso(nombre, cuerpo);
 * se pasan todos y al final se dice cuáles fallaron. Con argumentos, solo los
 * casos cuyo nombre lleva alguno de esos textos.
 */
public final class Pruebas {

    /** Un caso: su nombre y lo que hace (lanza un error si falla). */
    public interface Cuerpo {
        void probar() throws Exception;
    }

    private static final List<String> NOMBRES = new ArrayList<>();
    private static final List<Cuerpo> CUERPOS = new ArrayList<>();

    public static void caso(String nombre, Cuerpo cuerpo) {
        NOMBRES.add(nombre);
        CUERPOS.add(cuerpo);
    }

    public static void main(String[] args) {
        PruebasDelEditor.apuntar();
        PruebasDelCompilador.apuntar();
        PruebasDeLaPelea.apuntar();
        PruebasDelContenido.apuntar();
        PruebasDelProgreso.apuntar();
        PruebasDelCerebro.apuntar();
        PruebasDelEntrenador.apuntar();
        PruebasDeTresCerebros.apuntar();
        PruebasDelIde.apuntar();
        PruebasDelTemario.apuntar();
        PruebasDeLaSesion.apuntar();
        PruebasDeLaPruebaConIa.apuntar();
        PruebasDeSenior.apuntar();
        PruebasDeLasTresCasillas.apuntar();
        PruebasDelBanco.apuntar();
        PruebasDelCodigoDelRival.apuntar();
        PruebasDelDepurador.apuntar();
        PruebasDeModificadoresPoo.apuntar();
        PruebasDeArquetipos.apuntar();
        PruebasDeAlarmaYFuentes.apuntar();
        PruebasDeAparatos.apuntar();
        PruebasDeDemostraciones.apuntar();
        PruebasDeDemo3.apuntar();
        PruebasDeDemo4.apuntar();
        PruebasDeNormalizacion.apuntar();
        PruebasDelHudPelea.apuntar();
        PruebasDeObstaculosDestructibles.apuntar();
        PruebasDeVozPiper.apuntar();
        PruebasDeDocumentacionTaller.apuntar();

        List<String> fallos = new ArrayList<>();
        int pasados = 0;
        long t0 = System.nanoTime();
        for (int i = 0; i < NOMBRES.size(); i++) {
            String nombre = NOMBRES.get(i);
            if (args.length > 0) {
                boolean vale = false;
                for (String a : args) if (nombre.contains(a)) vale = true;
                if (!vale) continue;
            }
            long t = System.nanoTime();
            try {
                CUERPOS.get(i).probar();
                pasados++;
                System.out.printf("  bien  %-60s %5d ms%n", nombre, (System.nanoTime() - t) / 1_000_000);
            } catch (Throwable e) {
                fallos.add(nombre + ": " + e);
                System.out.printf("  MAL   %-60s %s%n", nombre, e);
                StackTraceElement[] pila = e.getStackTrace();
                for (int k = 0; k < Math.min(6, pila.length); k++) System.out.println("            en " + pila[k]);
            }
        }
        System.out.printf("%n%d casos bien, %d mal (%d s)%n", pasados, fallos.size(), (System.nanoTime() - t0) / 1_000_000_000);
        for (String f : fallos) System.out.println("  MAL: " + f);
        System.exit(fallos.isEmpty() ? 0 : 1);
    }

    // ------------------------------------------------------------------ comprobaciones

    public static void igual(Object esperado, Object obtenido) {
        igual(esperado, obtenido, "");
    }

    public static void igual(Object esperado, Object obtenido, String que) {
        if (!Objects.equals(esperado, obtenido)) {
            throw new AssertionError((que.isEmpty() ? "" : que + ": ") + "se esperaba «" + esperado + "» y salió «" + obtenido + "»");
        }
    }

    public static void cierto(boolean condicion, String que) {
        if (!condicion) throw new AssertionError(que);
    }

    public static void falso(boolean condicion, String que) {
        if (condicion) throw new AssertionError(que);
    }

    private Pruebas() {
    }
}
