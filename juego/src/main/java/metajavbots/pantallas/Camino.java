package metajavbots.pantallas;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import metajavbots.Juego;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Leccion;
import metajavbots.pantalla.Pantalla;
import metajavbots.progreso.Jugador;
import metajavbots.textos.T;

/**
 * CAMINO «EMPEZAR» — Para quien empieza, el menú no enseña veinte cosas de
 * golpe: enseña un solo camino, una cuesta que se empina muy poco a poco, y va
 * abriendo las casillas del menú según se avanza. Primero, cómo obedece el
 * robot (sin código); luego los ejercicios que enseñan el juego (res/ejercicios/
 * 24_juego.txt), cada uno con una sola mecánica nueva; el primer código; la
 * primera pelea completa; una pelea de verdad; y el resto de los ejercicios del
 * juego y la carrera. Quien ya sabe lo abre todo con un botón
 * (Jugador.menuCompleto); en Ajustes se puede volver al camino.
 */
public final class Camino {

    /** Un paso: qué es (lección, ejercicio o pelea), cuál y qué casillas del menú abre al hacerlo. */
    public record Paso(String tipo, String id, List<String> abre) {
    }

    /** Las casillas que se ven desde el principio (además de los botones de Ajustes, la IA y las mecánicas). */
    static final List<String> SIEMPRE = List.of();

    /** El camino, en orden. */
    public static final List<Paso> PASOS = List.of(
            new Paso("leccion", "l00_como_obedece", List.of("menu.tutoriales", "menu.ejercicios")),
            new Paso("ejercicio", "j_hasta_la_bandera", List.of()),
            new Paso("ejercicio", "j_puede_atacar", List.of()),
            new Paso("ejercicio", "j_usar_aparato", List.of("demo.boton")),
            new Paso("leccion", "l01_hola_arena", List.of("menu.taller", "mini.boton")),
            new Paso("leccion", "l01_primera_pelea", List.of("menu.pelear_ya")),
            new Paso("pelea", "", List.of("menu.pelear", "menu.tienda", "menu.historial", "menu.historia", "sesion.boton")),
            new Paso("ejercicio", "j_dos_ordenes_cabeza", List.of()),
            new Paso("ejercicio", "j_angulo_hacia", List.of()),
            new Paso("ejercicio", "j_normalizar", List.of()),
            new Paso("ejercicio", "j_cuanto_girar", List.of("menu.depuracion")),
            new Paso("ejercicio", "j_donde_estara", List.of()),
            new Paso("ejercicio", "j_elegir_brazo", List.of()),
            new Paso("ejercicio", "j_mas_cercano", List.of()),
            new Paso("leccion", "l05_carrera", List.of("menu.carrera")),
            new Paso("ejercicio", "j_memoria_huidas", List.of("menu.especial", "menu.temario")));

    private Camino() {
    }

    /** Si ese paso ya está hecho. */
    public static boolean hecho(Jugador j, Paso p) {
        switch (p.tipo()) {
            case "leccion": return j.tutorialesHechos.contains(p.id());
            case "ejercicio": return j.ejerciciosResueltos.contains(p.id());
            default: return !j.historial.isEmpty();
        }
    }

    /** El primer paso sin hacer (PASOS.size() si ya está todo). */
    public static int siguiente(Jugador j) {
        for (int i = 0; i < PASOS.size(); i++) if (!hecho(j, PASOS.get(i))) return i;
        return PASOS.size();
    }

    /** Cuántos pasos lleva hechos. */
    public static int hechos(Jugador j) {
        int n = 0;
        for (Paso p : PASOS) if (hecho(j, p)) n++;
        return n;
    }

    /** Las casillas del menú que ya se ven (todas si ha abierto el menú entero o ha acabado el camino). */
    public static Set<String> abiertas(Jugador j) {
        Set<String> s = new LinkedHashSet<>(SIEMPRE);
        for (Paso p : PASOS) if (hecho(j, p)) s.addAll(p.abre());
        return s;
    }

    /** Si esa casilla se ve en el menú de este jugador. */
    public static boolean abierta(Jugador j, String clave) {
        return j.menuCompleto || siguiente(j) >= PASOS.size() || abiertas(j).contains(clave);
    }

    /** El título del paso, en el idioma del jugador. */
    public static String titulo(Paso p) {
        switch (p.tipo()) {
            case "leccion": {
                Leccion l = leccion(p.id());
                return l == null ? p.id() : l.titulo(T.idioma);
            }
            case "ejercicio": {
                Ejercicio e = Banco.delJuego().ejercicio(p.id());
                return e == null ? p.id() : e.titulo.en(T.idioma);
            }
            default: return T.t("camino.pelea");
        }
    }

    /** Lo que se hace en ese paso, en una frase. */
    public static String que(Paso p) {
        return T.t("camino.que." + p.tipo());
    }

    /** Adónde lleva el botón del paso. */
    public static Pantalla destino(Juego j, Paso p) {
        switch (p.tipo()) {
            case "leccion": {
                Leccion l = leccion(p.id());
                return l == null ? new Tutoriales() : new PantallaLeccion(l);
            }
            case "ejercicio": {
                Ejercicio e = Banco.delJuego().ejercicio(p.id());
                return e == null ? new Ejercicios() : new PantallaEjercicio(e);
            }
            default: return PrepararPelea.adaptada(j);
        }
    }

    static Leccion leccion(String id) {
        for (Leccion l : Leccion.todas()) if (l.id.equals(id)) return l;
        return null;
    }
}
