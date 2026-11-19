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
        caso("minijuegos: los objetivos de moverse, girar el radar, apuntar y disparar con cabeza", () -> {
            metajavbots.combate.Combatiente c = metajavbots.rivales.Rivales.muneco();
            metajavbots.combate.Combatiente.Informe inf = new metajavbots.combate.Combatiente.Informe();
            c.informes.add(inf);
            inf.recorrido = 2600;
            inf.turnosVistos = 299;
            inf.ataques = 10;
            inf.aciertos = 6;
            cierto(metajavbots.pantallas.Objetivo.cumple("recorre>=2500", c, ""), "recorrido");
            cierto(!metajavbots.pantallas.Objetivo.cumple("vistos>=300", c, ""), "un turno corto a la vista");
            cierto(metajavbots.pantallas.Objetivo.cumple("punteria>=60", c, ""), "60 %");
            inf.ataques = 4;
            inf.aciertos = 4;
            cierto(!metajavbots.pantallas.Objetivo.cumple("punteria>=60", c, ""), "con menos de 5 ataques no vale");
            for (String id : new String[]{"moverse", "girar", "apuntar", "disparar", "esquivar"}) {
                for (String k : new String[]{"", ".texto", ".pseudo"}) cierto(metajavbots.textos.T.existe("mini." + id + k), "mini." + id + k);
            }
            cierto(metajavbots.pantallas.Objetivo.texto("vistos>=300").contains("10"), "10 s: " + metajavbots.pantallas.Objetivo.texto("vistos>=300"));
        });

        caso("minijuegos: esquivar obstáculos valida sinDanoTrampa>= y carga una arena con trampas activas", () -> {
            metajavbots.combate.Combatiente c = metajavbots.rivales.Rivales.muneco();
            metajavbots.combate.Combatiente.Informe inf = new metajavbots.combate.Combatiente.Informe();
            c.informes.add(inf);
            inf.turnos = 599;
            cierto(!metajavbots.pantallas.Objetivo.cumple("sinDanoTrampa>=600", c, ""), "menos turnos de los pedidos no cumple");
            inf.turnos = 600;
            inf.danoPorArmaRecibido.put("pared", 3.0);
            cierto(metajavbots.pantallas.Objetivo.cumple("sinDanoTrampa>=600", c, ""), "sin daño de trampas durante 20 s cumple");
            inf.fueraDeCombate = true;
            cierto(!metajavbots.pantallas.Objetivo.cumple("sinDanoTrampa>=600", c, ""), "fuera de combate no cumple");
            inf.fueraDeCombate = false;
            inf.danoPorArmaRecibido.put("pinchos", 5.0);
            cierto(!metajavbots.pantallas.Objetivo.cumple("sinDanoTrampa>=600", c, ""), "con daño de pinchos no cumple");

            String[][] retos = metajavbots.pantallas.MiniJuegos.retos();
            igual(5, retos.length, "cinco minijuegos");
            igual("esquivar", retos[4][0], "id del 5.º minijuego");
            igual("sinDanoTrampa>=600", retos[4][1], "objetivo del 5.º minijuego");

            metajavbots.pantallas.Encuentro en = metajavbots.pantallas.MiniJuegos.prepararReto(retos[4][0], retos[4][1]);
            cierto(en.rival != null && en.escenario != null, "encuentro y escenario cargados");
            igual("sinDanoTrampa>=600", en.objetivo, "objetivo en el encuentro");
            metajavbots.combate.Arena arena = metajavbots.combate.Arena.de(en.escenario.name(), 1000, 700);
            cierto(!arena.trampas().isEmpty(), "la arena del 5.º minijuego tiene trampas activas: " + en.escenario);
            cierto(arena.trampas().stream().anyMatch(metajavbots.combate.Arena.Trampa::peligroso), "hay al menos una trampa peligrosa");

            for (metajavbots.textos.Idioma id : metajavbots.textos.Idioma.values()) {
                cierto(!metajavbots.textos.T.en("mini.esquivar", id).isBlank(), "mini.esquivar en " + id);
                cierto(!metajavbots.textos.T.en("mini.esquivar.texto", id).isBlank(), "mini.esquivar.texto en " + id);
                cierto(!metajavbots.textos.T.en("objetivo.sin_dano_trampa", id).isBlank(), "objetivo.sin_dano_trampa en " + id);
            }
            cierto(metajavbots.textos.T.en("mini.esquivar.pseudo", metajavbots.textos.Idioma.ES).contains("obstaculos()"), "pista ES con obstaculos()");
            cierto(metajavbots.textos.T.en("mini.esquivar.pseudo", metajavbots.textos.Idioma.DE).contains("hindernisse()"), "pista DE con hindernisse()");
            cierto(metajavbots.textos.T.en("mini.esquivar.pseudo", metajavbots.textos.Idioma.EN).contains("obstacles()"), "pista EN con obstacles()");
            cierto(metajavbots.pantallas.Objetivo.texto("sinDanoTrampa>=600").contains("20"),
                    "20 s: " + metajavbots.pantallas.Objetivo.texto("sinDanoTrampa>=600"));
        });

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
