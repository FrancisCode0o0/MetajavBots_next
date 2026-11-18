package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.List;

import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.SesionDeUnaHora;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;

/** La hora de hoy: repaso de lo flojo, tema nuevo y pelea. */
final class PruebasDeLaSesion {

    private PruebasDeLaSesion() {
    }

    static void apuntar() {
        caso("sesión de una hora: repasa el tema a medias que peor lleva y abre el siguiente que su etiqueta permite", () -> {
            Banco b = Banco.delJuego();
            Jugador jug = new Jugador("Prueba");
            jug.etiqueta = Etiqueta.NOOB_2;
            // Nada hecho: no hay flojo; el nuevo es el primero.
            SesionDeUnaHora.Propuesta p = SesionDeUnaHora.proponer(jug, b);
            cierto(p.temaFlojo() == null && p.repaso().isEmpty(), "sin nada empezado, nada que repasar");
            igual("01", p.temaNuevo(), "el primer tema");
            cierto(p.leccion() != null && p.leccion().tema.equals("01") && p.ejercicioNuevo() != null, "con su lección y su ejercicio");
            // Todo el 01 hecho, el 02 casi entero y el 03 a medias: lo flojo es el 03; lo nuevo, el 04.
            for (Ejercicio e : b.deTema("01")) jug.ejerciciosResueltos.add(e.id);
            List<Ejercicio> dos = b.deTema("02");
            for (int i = 0; i < dos.size() - 1; i++) jug.ejerciciosResueltos.add(dos.get(i).id);
            jug.ejerciciosResueltos.add(b.deTema("03").get(0).id);
            p = SesionDeUnaHora.proponer(jug, b);
            igual("03", p.temaFlojo(), "lo que peor lleva");
            igual(2, p.repaso().size(), "dos de repaso");
            for (Ejercicio e : p.repaso()) cierto(!jug.ejerciciosResueltos.contains(e.id) && e.tema.equals("03"), "sin resolver y del tema: " + e.id);
            igual("04", p.temaNuevo(), "el siguiente sin tocar");
            // Un Noob no pasa a los temas de Senior.
            jug.etiqueta = Etiqueta.NOOB_1;
            for (String t : new String[]{"02", "03", "04", "05", "06", "07"}) for (Ejercicio e : b.deTema(t)) jug.ejerciciosResueltos.add(e.id);
            p = SesionDeUnaHora.proponer(jug, b);
            cierto(p.temaNuevo() == null || Integer.parseInt(p.temaNuevo()) <= 3, "nada por encima de su etiqueta: " + p.temaNuevo());
        });
    }
}
