package metajavbots.ejercicios;

import java.util.ArrayList;
import java.util.List;

import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;

/**
 * SESIÓN DE UNA HORA — Lo que el juego propone para hoy (bloque 4), como haría
 * un entrenador: unos 20 minutos de repaso de lo flojo (dos ejercicios sin
 * resolver del tema empezado que peor lleva), unos 25 de tema nuevo (el
 * siguiente que su etiqueta ya permite y no ha tocado: su primera lección y
 * su primer ejercicio) y unos 15 de pelea para usarlo.
 */
public final class SesionDeUnaHora {

    /** El tema flojo y sus ejercicios de repaso; el tema nuevo, su lección y su ejercicio (cualquiera puede faltar: null o vacío). */
    public record Propuesta(String temaFlojo, List<Ejercicio> repaso, String temaNuevo, Leccion leccion, Ejercicio ejercicioNuevo) {
    }

    private SesionDeUnaHora() {
    }

    /** ¿Ya puede con ese tema por su etiqueta? */
    static boolean alcanzado(Tema t, Jugador jug) {
        try {
            return Etiqueta.valueOf(t.etiqueta).ordinal() <= jug.etiqueta.ordinal();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /** Lo que lleva resuelto de un tema, de 0 a 1 (y -1 si el tema no tiene ejercicios). */
    static double resuelto(String tema, Jugador jug, Banco banco) {
        List<Ejercicio> l = banco.deTema(tema);
        if (l.isEmpty()) return -1;
        int n = 0;
        for (Ejercicio e : l) if (jug.ejerciciosResueltos.contains(e.id)) n++;
        return n / (double) l.size();
    }

    public static Propuesta proponer(Jugador jug, Banco banco) {
        String flojo = null, nuevo = null;
        double peor = 2;
        for (Tema t : Tema.todos()) {
            if (!alcanzado(t, jug)) continue;
            double r = resuelto(t.id, jug, banco);
            if (r < 0) continue;
            if (r > 0 && r < 1 && r < peor) {
                peor = r;
                flojo = t.id;
            }
            if (r == 0 && nuevo == null) nuevo = t.id;
        }
        // Si no hay ninguno empezado a medias, el repaso es del último tema que tocó entero (o del primero).
        List<Ejercicio> repaso = new ArrayList<>();
        if (flojo != null) {
            for (Ejercicio e : banco.deTema(flojo)) {
                if (!jug.ejerciciosResueltos.contains(e.id) && repaso.size() < 2) repaso.add(e);
            }
        }
        Leccion leccion = null;
        Ejercicio primero = null;
        if (nuevo != null) {
            for (Leccion l : Leccion.todas()) {
                if (l.tema.equals(nuevo) && (leccion == null || l.orden < leccion.orden)) leccion = l;
            }
            List<Ejercicio> l = banco.deTema(nuevo);
            primero = l.isEmpty() ? null : l.get(0);
        }
        return new Propuesta(flojo, repaso, nuevo, leccion, primero);
    }
}
