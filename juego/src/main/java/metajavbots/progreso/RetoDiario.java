package metajavbots.progreso;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;

/**
 * RETO DIARIO — Un ejercicio del banco para cada día (el mismo todo el día,
 * otro al siguiente; mejor uno sin resolver) y la racha de días seguidos en
 * que se ha hecho: si un día se salta, vuelve a empezar.
 */
public final class RetoDiario {

    private RetoDiario() {
    }

    public static long hoy() {
        return LocalDate.now().toEpochDay();
    }

    /** El reto de ese día para ese jugador (null si el banco está vacío). */
    public static Ejercicio de(Jugador j, long dia) {
        List<Ejercicio> es = new ArrayList<>();
        int version = Compilador.versionDeJava();
        for (Ejercicio e : Banco.delJuego().todos()) if (e.java <= version) es.add(e);
        if (es.isEmpty()) return null;
        es.sort(Comparator.comparing(e -> e.id));
        // Un salto grande y primo: días seguidos dan ejercicios de temas distintos.
        int inicio = (int) Math.floorMod(dia * 7919L, (long) es.size());
        for (int k = 0; k < es.size(); k++) {
            Ejercicio e = es.get((inicio + k) % es.size());
            if (!j.ejerciciosResueltos.contains(e.id) || j.retoDiarioUltimo == dia && e.id.equals(j.retoDiarioId)) return e;
        }
        return es.get(inicio);
    }

    /** La racha que sigue viva (si el último reto fue hoy o ayer); si no, 0. */
    public static int racha(Jugador j, long hoy) {
        return j.retoDiarioUltimo >= hoy - 1 ? j.retoDiarioRacha : 0;
    }

    /** Se ha hecho el reto de ese día: la racha sube (o vuelve a 1) y da experiencia extra; devuelve cuánta. */
    public static int hecho(Jugador j, long dia, String id) {
        if (j.retoDiarioUltimo == dia) return 0;
        j.retoDiarioRacha = j.retoDiarioUltimo == dia - 1 ? j.retoDiarioRacha + 1 : 1;
        j.retoDiarioUltimo = dia;
        j.retoDiarioId = id;
        j.retoDiarioMejor = Math.max(j.retoDiarioMejor, j.retoDiarioRacha);
        int extra = 10 + 5 * Math.min(10, j.retoDiarioRacha);
        j.experiencia += extra;
        return extra;
    }
}
