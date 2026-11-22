package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.pantallas.PrevisualizacionDeEjercicio;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * PRUEBAS DE PREVISUALIZACIÓN DE EJERCICIOS — Verifica que todos los ejercicios
 * del banco tienen un escenario de espera válido y diferenciado antes de pulsar
 * «Comprobar», con textos trilingües y actualización del conocimiento del
 * entrenador en ES, DE y EN.
 */
public final class PruebasDePrevisualizacionEjercicios {

    private PruebasDePrevisualizacionEjercicios() {
    }

    /** Registra los casos de prueba de previsualización de ejercicios. */
    public static void apuntar() {
        caso("ejercicios previsualización: todos los ejercicios del banco tienen escenario inicial válido",
                PruebasDePrevisualizacionEjercicios::probarTodosLosEjercicios);
        caso("ejercicios previsualización: tipos distintos producen escenas y formas diferenciadas",
                PruebasDePrevisualizacionEjercicios::probarVariedadDeEscenarios);
        caso("ejercicios previsualización: textos en espera y entrenador actualizados en ES, DE y EN",
                PruebasDePrevisualizacionEjercicios::probarTextosYEntrenador);
    }

    private static void probarTodosLosEjercicios() {
        List<Ejercicio> todos = Banco.delJuego().todos();
        cierto(!todos.isEmpty(), "el banco debe tener ejercicios");
        for (Ejercicio e : todos) {
            for (Idioma id : Idioma.values()) {
                PrevisualizacionDeEjercicio.Escenario esc = PrevisualizacionDeEjercicio.de(e, id);
                cierto(esc != null, e.id + " (" + id + "): escenario no nulo");
                cierto(esc.escenaId() != null && !esc.escenaId().isBlank(), e.id + " (" + id + "): escena válida");
                cierto(esc.formaId() != null && !esc.formaId().isBlank(), e.id + " (" + id + "): forma válida");
                cierto(esc.llamadaMuestra() != null, e.id + " (" + id + "): llamada muestra no nula");
                cierto(esc.esperadoMuestra() != null, e.id + " (" + id + "): esperado muestra no nulo");
            }
        }
        PrevisualizacionDeEjercicio.Escenario respaldo = PrevisualizacionDeEjercicio.de(null, Idioma.ES);
        cierto(respaldo != null && !respaldo.escenaId().isBlank() && !respaldo.formaId().isBlank(), "respaldo seguro si ejercicio es null");
    }

    private static void probarVariedadDeEscenarios() {
        List<Ejercicio> todos = Banco.delJuego().todos();
        Set<String> formas = new HashSet<>();
        Set<String> escenas = new HashSet<>();
        boolean vioSalidaPapel = false;
        boolean vioVariablesCajas = false;
        boolean vioCondicionalPuertas = false;
        boolean vioBuclesObra = false;
        boolean vioArrayEstanteria = false;
        boolean vioPoo = false;
        for (Ejercicio e : todos) {
            PrevisualizacionDeEjercicio.Escenario esc = PrevisualizacionDeEjercicio.de(e, Idioma.ES);
            formas.add(esc.formaId());
            escenas.add(esc.escenaId());
            if (e.tipo == Ejercicio.Tipo.SALIDA && "PAPEL".equals(esc.formaId())) vioSalidaPapel = true;
            if ("02".equals(e.tema) && "TARROS".equals(esc.escenaId()) && "CAJAS".equals(esc.formaId())) vioVariablesCajas = true;
            if ("04".equals(e.tema) && "APARCAMIENTO".equals(esc.escenaId()) && "PUERTAS".equals(esc.formaId())) vioCondicionalPuertas = true;
            if ("05".equals(e.tema) && "OBRA".equals(esc.escenaId())) vioBuclesObra = true;
            if (("07".equals(e.tema) || "15".equals(e.tema)) && "ESTANTERIA".equals(esc.formaId())) vioArrayEstanteria = true;
            if ("PLANOS".equals(esc.escenaId()) || "CAJA_FUERTE".equals(esc.escenaId()) || "FAMILIA".equals(esc.escenaId())
                    || "CORO".equals(esc.escenaId()) || "ENCHUFES".equals(esc.escenaId())) {
                vioPoo = true;
            }
        }
        cierto(formas.size() >= 5, "al menos 5 formas visuales distintas: " + formas);
        cierto(escenas.size() >= 15, "al menos 15 escenas temáticas distintas: " + escenas.size());
        cierto(vioSalidaPapel, "ejercicios de impresión usan forma PAPEL");
        cierto(vioVariablesCajas, "ejercicios de variables usan escena TARROS y forma CAJAS");
        cierto(vioCondicionalPuertas, "ejercicios de condicionales booleanos usan APARCAMIENTO y PUERTAS");
        cierto(vioBuclesObra, "ejercicios de bucles usan escena OBRA");
        cierto(vioArrayEstanteria, "ejercicios de arrays/colecciones usan forma ESTANTERIA");
        cierto(vioPoo, "ejercicios de POO usan escenas propias de objetos");
    }

    private static void probarTextosYEntrenador() {
        String[] claves = {"animacion.vista_previa", "animacion.espera", "animacion.error_compilacion"};
        Idioma antes = T.idioma;
        try {
            for (String clave : claves) {
                T.idioma = Idioma.ES;
                String es = T.t(clave);
                T.idioma = Idioma.DE;
                String de = T.t(clave);
                T.idioma = Idioma.EN;
                String en = T.t(clave);
                cierto(!es.isBlank() && !es.equals(clave), "falta " + clave + " en ES");
                cierto(!de.isBlank() && !de.equals(clave), "falta " + clave + " en DE");
                cierto(!en.isBlank() && !en.equals(clave), "falta " + clave + " en EN");
                cierto(!es.equals(de) && !es.equals(en) && !de.equals(en), clave + " debe traducirse en ES, DE y EN");
            }
        } finally {
            T.idioma = antes;
        }
        ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("minijuegos_y_ejercicios");
        cierto(entrada != null, "existe la entrada minijuegos_y_ejercicios en el entrenador");
        String es = String.join(" ", entrada.parrafos().get(Idioma.ES)).toLowerCase(java.util.Locale.ROOT);
        String de = String.join(" ", entrada.parrafos().get(Idioma.DE)).toLowerCase(java.util.Locale.ROOT);
        String en = String.join(" ", entrada.parrafos().get(Idioma.EN)).toLowerCase(java.util.Locale.ROOT);
        cierto(es.contains("vista previa") && es.contains("robotito"), "ficha offline ES menciona la vista previa del robotito");
        cierto(de.contains("vorschau") && de.contains("roboter"), "ficha offline DE menciona la vista previa del robotito");
        cierto(en.contains("preview") && en.contains("robot"), "ficha offline EN menciona la vista previa del robotito");
        String sistema = ConsultaIaEntrenador.construirSistema(new Jugador("Prueba"), Idioma.ES, Entrenador.de("miyagi"));
        cierto(sistema.toLowerCase(java.util.Locale.ROOT).contains("preview"), "contexto de IA refleja la vista previa de ejercicios");
    }
}
