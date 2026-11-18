package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.progreso.PruebaDeNivel;
import metajavbots.progreso.Registro;

/** El progreso: etiquetas, subir y bajar, desbloqueos, robots por partida, guardado e historial. */
final class PruebasDelProgreso {

    private PruebasDelProgreso() {
    }

    static Registro pelea(Registro.Resultado r, double nivel) {
        return new Registro(System.currentTimeMillis(), "ADAPTADA", "Rival", "chispa", "cobalto", nivel, r, 50, 80, 4, "NOOB_1");
    }

    static void apuntar() {
        apuntarParte1();
        apuntarParte2();
        apuntarParte3();
        apuntarParte4();
    }

    private static void apuntarParte1() {
        caso("progreso: doce etiquetas y Master, con los robots que permite cada categoría", () -> {
            igual(13, Etiqueta.values().length);
            igual(1, Etiqueta.NOOB_4.robotsPermitidos());
            igual(2, Etiqueta.JUNIOR_1.robotsPermitidos());
            igual(3, Etiqueta.SENIOR_2.robotsPermitidos());
            igual(4, Etiqueta.MASTER.robotsPermitidos());
            for (Etiqueta e : Etiqueta.values()) {
                cierto(e.nivelDeLaIA() >= 0 && e.nivelDeLaIA() <= 1, "nivel de la IA entre 0 y 1");
                if (e != Etiqueta.MASTER) cierto(e.siguiente().nivelDeLaIA() > e.nivelDeLaIA(), "la IA sube con la etiqueta");
            }
        });

        caso("progreso: se sube con victorias y ejercicios de su tema, y se baja tras muchas derrotas", () -> {
            Jugador j = new Jugador("prueba");
            Random azar = new Random(1);
            int hacen = Progreso.victoriasParaSubir(Etiqueta.NOOB_1);
            Progreso.Requisitos r = Progreso.requisitos(j);
            for (int i = 0; i < hacen; i++) Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 0.1), "ACERO", azar);
            if (r.ejercicios > 0) {
                igual(Etiqueta.NOOB_1, j.etiqueta, "sin ejercicios no se sube");
                for (metajavbots.ejercicios.Ejercicio e : Progreso.ejerciciosDe(Etiqueta.NOOB_1)) Progreso.despuesDelEjercicio(j, e);
            }
            igual(Etiqueta.NOOB_2, j.etiqueta, "sube a Noob II");
            Progreso.Cambio c = null;
            for (int i = 0; i < 12; i++) c = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.DERROTA, 0.2), "RAYO", azar);
            igual(Etiqueta.NOOB_1, j.etiqueta, "baja tras muchas derrotas");
        });

        caso("progreso: la IA se adapta (más fácil tras perder) y no cambia dentro de la pelea", () -> {
            Jugador j = new Jugador("x");
            j.etiqueta = Etiqueta.JUNIOR_2;
            double suma = 0, sumaTrasPerder = 0;
            Random azar = new Random(5);
            for (int i = 0; i < 200; i++) suma += Progreso.nivelParaLaPelea(j, azar);
            j.derrotasSeguidas = 4;
            for (int i = 0; i < 200; i++) sumaTrasPerder += Progreso.nivelParaLaPelea(j, azar);
            cierto(Math.abs(suma / 200 - Etiqueta.JUNIOR_2.nivelDeLaIA()) < 0.03, "alrededor del de su etiqueta: " + suma / 200);
            cierto(sumaTrasPerder < suma, "más fácil tras perder");
            cierto(Progreso.nivelDelDesafio(j) > Etiqueta.JUNIOR_2.nivelDeLaIA() + 0.2, "el desafío, unos escalones por encima");
        });

        caso("progreso: los robots nuevos se ganan con muchas victorias y su categoría", () -> {
            Jugador j = new Jugador("x");
            j.etiqueta = Etiqueta.JUNIOR_1;
            igual(1, Progreso.robotsQuePuedeTener(j), "Junior sin victorias: uno");
            for (int i = 0; i < Progreso.VICTORIAS_POR_ROBOT[1]; i++) j.historial.add(pelea(Registro.Resultado.VICTORIA, 0.3));
            igual(2, Progreso.robotsQuePuedeTener(j), "Junior con victorias: dos");
            j.etiqueta = Etiqueta.NOOB_3;
            igual(1, Progreso.robotsQuePuedeTener(j), "Noob: uno siempre");
        });

        caso("progreso: las piezas se desbloquean con victorias, niveles, retos y desafíos", () -> {
            Jugador j = new Jugador("x");
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            cierto(Progreso.piezaDisponible(j, c.pieza("laser_basico")), "la de inicio");
            falso(Progreso.piezaDisponible(j, c.pieza("laser_pesado")), "la de 6 victorias, no");
            for (int i = 0; i < 6; i++) j.historial.add(pelea(Registro.Resultado.VICTORIA, 0.3));
            cierto(Progreso.piezaDisponible(j, c.pieza("laser_pesado")), "con 6 victorias, sí");
            falso(Progreso.piezaDisponible(j, c.pieza("reto_prediccion")), "la del reto, no");
            j.retosHechos.add("prediccion");
            cierto(Progreso.piezaDisponible(j, c.pieza("reto_prediccion")), "tras el reto, sí");
            falso(Progreso.piezaDisponible(j, c.pieza("laser_aurora")), "la legendaria, no");
            Progreso.premioDelDesafio(j, false, c.pieza("laser_aurora"));
            cierto(Progreso.piezaDisponible(j, c.pieza("laser_aurora")), "la legendaria del desafío, sí");
            cierto(Progreso.piezaUsable(j, c.pieza("canon_riel"), Modelo.de("eclipse")), "las de fábrica de su modelo, siempre");
        });
    }

    private static void apuntarParte2() {
        caso("progreso: el botín de una victoria es un núcleo de la naturaleza del rival", () -> {
            Jugador j = new Jugador("x");
            Progreso.Cambio c = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 0.2), "FURIA", new Random(2));
            igual(1, c.botin.size(), "una pieza");
            igual("naturaleza_furia", c.botin.get(0).id);
        });

        caso("progreso: la prueba de nivel cambia cada vez (banco y preguntas generadas)", () -> {
            List<PruebaDeNivel.Pregunta> a = PruebaDeNivel.nueva(new Random(1)), b = PruebaDeNivel.nueva(new Random(2));
            igual(PruebaDeNivel.preguntas().size(), a.size(), "tres por escalón");
            int iguales = 0;
            for (int i = 0; i < a.size(); i++) {
                igual(PruebaDeNivel.preguntas().get(i).nivel, a.get(i).nivel, "los escalones, en orden");
                if (a.get(i).codigo.equals(b.get(i).codigo)) iguales++;
                igual(4, a.get(i).opciones.size());
                java.util.Set<String> distintas = new java.util.HashSet<>();
                for (metajavbots.textos.Trilingue t : a.get(i).opciones) distintas.add(t.es);
                igual(4, distintas.size(), "cuatro opciones distintas: " + a.get(i).codigo);
            }
            cierto(iguales < a.size() / 2, "dos pruebas se parecen poco (iguales: " + iguales + ")");
            List<Boolean> todo = new ArrayList<>();
            for (int i = 0; i < a.size(); i++) todo.add(true);
            igual(Etiqueta.SENIOR_2, PruebaDeNivel.resultado(a, todo));
        });

        caso("progreso: cada pregunta generada escribe de verdad la respuesta buena (se compila y se ejecuta)", () -> {
            if (!metajavbots.codigo.Compilador.hayCompilador()) {
                System.out.println("            (sin JDK: se salta)");
                return;
            }
            int n = 0;
            for (Etiqueta nivel : Etiqueta.values()) {
                for (java.util.function.Function<Random, metajavbots.progreso.GeneradorDePreguntas.Generada> plantilla
                        : metajavbots.progreso.GeneradorDePreguntas.plantillas(nivel)) {
                    StringBuilder texto = new StringBuilder();
                    StringBuilder codigo = new StringBuilder();
                    for (int semilla = 0; semilla < 12; semilla++) {
                        metajavbots.progreso.GeneradorDePreguntas.Generada g = plantilla.apply(new Random(semilla * 31L + n));
                        igual(3, new java.util.HashSet<>(g.malas).size(), "tres malas distintas");
                        falso(g.malas.contains(g.respuesta), "la buena no está entre las malas");
                        codigo.append("static class C").append(semilla).append(" {\n").append(g.arriba).append("\nstatic void q() {\n").append(g.cuerpo)
                                .append("\n}\n}\n");
                        texto.append("    C").append(semilla).append(".q(); => \"").append(g.respuesta.replace("\"", "\\\"")).append("\\n\"\n");
                    }
                    String ej = "=== generada\ntema: 01\ndificultad: 1\njava: 17\ntipo: salida\ntitulo.es: x\ntitulo.de: x\ntitulo.en: x\n"
                            + "enunciado.es: x\nenunciado.de: x\nenunciado.en: x\nfirma: static void q0()\n"
                            + "plantilla:\n    return;\nsolucion:\n    return;\ncasos:\n" + texto;
                    metajavbots.ejercicios.Ejercicio e = metajavbots.ejercicios.Ejercicio.leer(
                            new java.io.ByteArrayInputStream(ej.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "generada").get(0);
                    metajavbots.ejercicios.Comprobador.Informe inf = metajavbots.ejercicios.Comprobador.comprobar(e, codigo.toString(), java.util.Locale.ROOT);
                    cierto(inf.compila(), nivel + ": compila " + inf.errores + "\n" + codigo);
                    for (metajavbots.ejercicios.Comprobador.Resultado r : inf.casos) {
                        cierto(r.bien, nivel + ": " + r.caso.llamada + " esperaba " + r.caso.esperado + " y salió " + r.obtenido);
                    }
                    n++;
                }
            }
            System.out.println("            " + n + " plantillas, 12 azares cada una");
        });
    }

    private static void apuntarParte3() {
        caso("progreso: las partidas se cambian de nombre y se borran (a partidas/_borradas)", () -> {
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots");
            Guardado.carpeta = tmp;
            try {
                Jugador j = new Jugador("Ana");
                j.robots.add(new Jugador.Robot("Bip", "cobalto"));
                Guardado.guardar(j);
                Guardado.guardar(new Jugador("Luis"));
                String nueva = Guardado.renombrar("Ana", "Ana Robótica");
                igual("Ana_Robótica", nueva);
                igual(List.of("Ana_Robótica", "Luis"), Guardado.partidas());
                igual("Ana Robótica", Guardado.leer(nueva).nombre);
                igual(1, Guardado.leer(nueva).robots.size(), "con sus robots");
                try {
                    Guardado.renombrar("Luis", "ana robótica");
                    cierto(false, "no deja pisar otra partida");
                } catch (java.io.IOException e) {
                    igual("ya existe", e.getMessage());
                }
                Guardado.renombrar("Luis", "LUIS");
                igual(List.of("Ana_Robótica", "LUIS"), Guardado.partidas(), "solo mayúsculas");
                Guardado.borrar("LUIS");
                igual(List.of("Ana_Robótica"), Guardado.partidas());
                cierto(Files.isDirectory(tmp.resolve(Guardado.BORRADAS)), "se puede recuperar a mano");
            } finally {
                Guardado.carpeta = antes;
            }
        });

        caso("progreso: la prueba de nivel pone la etiqueta del principio", () -> {
            List<PruebaDeNivel.Pregunta> p = PruebaDeNivel.preguntas();
            cierto(p.size() >= 15, "preguntas: " + p.size());
            for (PruebaDeNivel.Pregunta q : p) {
                igual(4, q.opciones.size(), "cuatro opciones");
                cierto(q.bien >= 0 && q.bien < 4, "la buena existe");
                cierto(!q.pregunta.de.isEmpty() && !q.pregunta.en.isEmpty(), "tres idiomas");
            }
            List<Boolean> nada = new ArrayList<>(), todo = new ArrayList<>(), mitad = new ArrayList<>();
            for (int i = 0; i < p.size(); i++) {
                nada.add(false);
                todo.add(true);
                mitad.add(i < 6);
            }
            igual(Etiqueta.NOOB_1, PruebaDeNivel.resultado(nada));
            igual(Etiqueta.SENIOR_2, PruebaDeNivel.resultado(todo));
            igual(Etiqueta.NOOB_4, PruebaDeNivel.resultado(mitad));
        });
    }

    private static void apuntarParte4() {
        caso("progreso: la partida se guarda y se lee igual (robots, código, historial)", () -> {
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots");
            Guardado.carpeta = tmp;
            try {
                Jugador j = new Jugador("Fran Prueba");
                j.etiqueta = Etiqueta.JUNIOR_3;
                Jugador.Robot r = new Jugador.Robot("Cobaltito", "cobalto");
                Montaje m = new Montaje(Modelo.de("cobalto"));
                m.poner(Montaje.Hueco.IZQUIERDA, CatalogoDePiezas.delJuego().pieza("laser_basico_3"));
                r.ponerPiezas(m);
                r.codigo = "public class A extends Metajavbot {\n    // ñandú «comillas» | barra\n}\n";
                j.robots.add(r);
                j.ejerciciosResueltos.add("muestra_sumar");
                j.codigoDeEjercicios.put("muestra_sumar", "static int danoTotal(int a, int b) {\n    return a + b;\n}\n");
                j.historial.add(pelea(Registro.Resultado.VICTORIA, 0.42));
                j.agresividad = 0.9;
                Guardado.guardar(j);
                igual(List.of(Guardado.carpetaDe("Fran Prueba")), Guardado.partidas());
                Jugador k = Guardado.leer(Guardado.carpetaDe("Fran Prueba"));
                igual("Fran Prueba", k.nombre);
                igual(Etiqueta.JUNIOR_3, k.etiqueta);
                igual(r.codigo, k.robots.get(0).codigo, "el código, letra a letra");
                igual("laser_basico_3", k.robots.get(0).piezas.get(Montaje.Hueco.IZQUIERDA));
                igual("laser_basico_3", k.robots.get(0).montaje().pieza(Montaje.Hueco.IZQUIERDA).id);
                igual(1, k.historial.size());
                igual(0.42, k.historial.get(0).nivelRival);
                igual(j.codigoDeEjercicios, k.codigoDeEjercicios);
                igual(0.9, k.agresividad);
            } finally {
                Guardado.carpeta = antes;
            }
        });
    }
}
