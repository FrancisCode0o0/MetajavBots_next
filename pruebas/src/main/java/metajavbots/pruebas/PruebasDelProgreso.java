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
        apuntarPista();
        apuntarMonedas();
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
        caso("ejercicios: los de plantilla cambian cada vez y su solución pasa sus casos (y la plantilla no)", () -> {
            if (!metajavbots.codigo.Compilador.hayCompilador()) return;
            int n = 0;
            for (String tema : metajavbots.ejercicios.PlantillasDeEjercicios.TEMAS) {
                java.util.Set<String> distintos = new java.util.HashSet<>();
                for (int semilla = 0; semilla < 6; semilla++) {
                    metajavbots.ejercicios.Ejercicio e = metajavbots.ejercicios.PlantillasDeEjercicios.generar(tema,
                            new Random(semilla * 7L + tema.hashCode()));
                    cierto(e != null, "hay plantilla del tema " + tema);
                    igual(tema, e.tema);
                    distintos.add(e.enunciado.es + e.casos);
                    cierto(metajavbots.ejercicios.Comprobador.comprobar(e, e.codigoSolucion(), java.util.Locale.ROOT).todoBien(), tema + ": la solución pasa");
                    falso(metajavbots.ejercicios.Comprobador.comprobar(e, e.codigoInicial(), java.util.Locale.ROOT).todoBien(), tema + ": la plantilla no");
                    n++;
                }
                cierto(distintos.size() >= 4, tema + ": salen distintos (" + distintos.size() + ")");
            }
            System.out.println("            " + n + " ejercicios de plantilla comprobados");
        });

        caso("ejercicios: con traza, cada caso sabe qué líneas del jugador ejecuta y en qué orden (para el robotito)", () -> {
            if (!metajavbots.codigo.Compilador.hayCompilador()) return;
            metajavbots.ejercicios.Ejercicio e = metajavbots.ejercicios.PlantillasDeEjercicios.generar("05", new Random(1));
            metajavbots.ejercicios.Comprobador.Informe inf = metajavbots.ejercicios.Comprobador.comprobar(e, e.codigoSolucion(),
                    java.util.Locale.ROOT, true);
            cierto(inf.todoBien(), "con traza también pasa");
            int[] t = inf.casos.get(inf.casos.size() - 1).traza;
            cierto(t.length > 6, "un bucle repite líneas: " + java.util.Arrays.toString(t));
            int lineas = e.codigoSolucion().split("\n").length;
            for (int l : t) cierto(l >= 1 && l <= lineas, "solo líneas del jugador");
            igual(0, metajavbots.ejercicios.Comprobador.comprobar(e, e.codigoSolucion(), java.util.Locale.ROOT).casos.get(0).traza.length, "sin traza, nada");
        });

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

    private static metajavbots.ejercicios.Ejercicio delBanco(String id) {
        for (metajavbots.ejercicios.Ejercicio e : metajavbots.ejercicios.Banco.delJuego().todos()) if (e.id.equals(id)) return e;
        throw new IllegalArgumentException(id);
    }

    /** La pista de carrera de los ejercicios (PistaDeCarrera). */
    private static void apuntarPista() {
        caso("ejercicios: la pista de carrera sale de la traza (una valla por vuelta, un desvío y un cartel por cada if)", () -> {
            if (!metajavbots.codigo.Compilador.hayCompilador()) return;
            metajavbots.ejercicios.Ejercicio suma = delBanco("t05_sumar_hasta");
            metajavbots.ejercicios.Comprobador.Informe inf = metajavbots.ejercicios.Comprobador.comprobar(suma, suma.codigoSolucion(),
                    java.util.Locale.ROOT, true);
            // sumarHasta(4): cuatro vueltas, y el return de después del bucle también se ve.
            metajavbots.pantallas.PistaDeCarrera.Recorrido r = metajavbots.pantallas.PistaDeCarrera.de(inf.casos.get(0).traza, suma.codigoSolucion());
            igual(4, r.vallas, "una valla por vuelta: " + java.util.Arrays.toString(inf.casos.get(0).traza));
            int[] t = inf.casos.get(0).traza;
            igual(suma.codigoSolucion().split("\n").length - 1, t[t.length - 1], "el return tras el «}» del for, en la traza");
            metajavbots.ejercicios.Ejercicio disparar = delBanco("t04_puede_disparar");
            String codigo = disparar.firma + " {\n    if (sobrecalentado) {\n        return false;\n    }\n    return energia >= coste;\n}\n";
            inf = metajavbots.ejercicios.Comprobador.comprobar(disparar, codigo, java.util.Locale.ROOT, true);
            cierto(inf.todoBien(), "con traza también pasa");
            r = metajavbots.pantallas.PistaDeCarrera.de(inf.casos.get(0).traza, codigo);
            igual(metajavbots.pantallas.PistaDeCarrera.Suceso.DESVIO, r.sucesos[1], "se salta el if: desvío");
            igual(Boolean.FALSE, r.carteles[0], "el cartel del if, en rojo");
            r = metajavbots.pantallas.PistaDeCarrera.de(inf.casos.get(3).traza, codigo);
            igual(Boolean.TRUE, r.carteles[0], "sobrecalentado: entra en el if");
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
                j.ayudaDelTutor = 0.8f;
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
                igual(0.8, Math.round(k.ayudaDelTutor * 10) / 10.0, "la ayuda del entrenador");
                Path rf = tmp.resolve(Guardado.carpetaDe("Fran Prueba")).resolve("robot_1.java");
                java.nio.file.attribute.FileTime marca = java.nio.file.attribute.FileTime.fromMillis(1_000_000L);
                Files.setLastModifiedTime(rf, marca);
                Guardado.guardar(j);
                igual(marca, Files.getLastModifiedTime(rf), "sin cambios no reescribe el archivo");
            } finally {
                Guardado.carpeta = antes;
            }
        });

        caso("progreso: reto diario no duplica la experiencia del bono", () -> {
            Jugador j = new Jugador("Reto");
            long hoy = metajavbots.progreso.RetoDiario.hoy();
            metajavbots.ejercicios.Ejercicio delDia = metajavbots.progreso.RetoDiario.de(j, hoy);
            cierto(delDia != null, "existe reto del día");
            Progreso.Cambio c = Progreso.despuesDelEjercicio(j, delDia);
            int esperado = delDia.experiencia() + 10 + 5 * Math.min(10, j.retoDiarioRacha);
            igual((long) esperado, j.experiencia, "suma base y bono una sola vez");
            igual((long) esperado, c.experiencia, "cambio coincide con la experiencia sumada");
        });
    }

    private static void apuntarMonedas() {
        caso("progreso: monedas por ejercicio (primera vez) y por victoria en Noob, Junior, Senior y Master", () -> {
            Jugador j = new Jugador("Monedas");
            Random azar = new Random(7);
            igual(0.0, j.monedas, "empieza sin monedas");

            List<metajavbots.ejercicios.Ejercicio> todos = metajavbots.ejercicios.Banco.delJuego().todos();
            metajavbots.ejercicios.Ejercicio e1 = todos.get(0);
            metajavbots.ejercicios.Ejercicio e2 = todos.get(1);

            Progreso.Cambio ce1 = Progreso.despuesDelEjercicio(j, e1);
            igual(0.25, ce1.monedas, "primer ejercicio: +0.25 monedas");
            igual(0.25, j.monedas, "saldo tras primer ejercicio");

            Progreso.Cambio ce1Rep = Progreso.despuesDelEjercicio(j, e1);
            igual(0.0, ce1Rep.monedas, "ejercicio repetido no vuelve a dar monedas");
            igual(0.25, j.monedas, "saldo sin cambios al repetir ejercicio");

            Progreso.Cambio ce2 = Progreso.despuesDelEjercicio(j, e2);
            igual(0.25, ce2.monedas, "segundo ejercicio nuevo: +0.25 monedas");
            igual(0.5, j.monedas, "saldo tras dos ejercicios");

            j.etiqueta = Etiqueta.NOOB_2;
            Progreso.Cambio cNoob = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 0.2), "ACERO", azar);
            igual(2.0, cNoob.monedas, "victoria en Noob: +2.0 monedas");
            igual(2.5, j.monedas, "saldo tras victoria Noob");

            Progreso.Cambio cDerrota = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.DERROTA, 0.2), "ACERO", azar);
            igual(0.0, cDerrota.monedas, "derrota no da monedas");
            Progreso.Cambio cEmpate = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.EMPATE, 0.2), "ACERO", azar);
            igual(0.0, cEmpate.monedas, "empate no da monedas");
            igual(2.5, j.monedas, "saldo sin cambios tras derrota y empate");

            j.etiqueta = Etiqueta.JUNIOR_3;
            Progreso.Cambio cJunior = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 0.5), "RAYO", azar);
            igual(3.0, cJunior.monedas, "victoria en Junior: +3.0 monedas");
            igual(5.5, j.monedas, "saldo tras victoria Junior");

            j.etiqueta = Etiqueta.SENIOR_1;
            Progreso.Cambio cSenior = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 0.8), "FUEGO", azar);
            igual(3.5, cSenior.monedas, "victoria en Senior: +3.5 monedas");
            igual(9.0, j.monedas, "saldo tras victoria Senior");

            j.etiqueta = Etiqueta.MASTER;
            Progreso.Cambio cMaster = Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 1.0), "SOMBRA", azar);
            igual(3.5, cMaster.monedas, "victoria en Master: +3.5 monedas");
            igual(12.5, j.monedas, "saldo tras victoria Master");

            long expAntes = j.experiencia;
            cierto(j.gastarMonedas(2.25), "permite gastar monedas disponibles");
            igual(10.25, j.monedas, "saldo tras gasto");
            igual(expAntes, j.experiencia, "gastar monedas no afecta a la experiencia");
            falso(j.gastarMonedas(50.0), "no permite gastar más del saldo");
            igual(10.25, j.monedas, "saldo intacto tras gasto rechazado");
            igual("0", Jugador.formatearMonedas(0));
            igual("0.25", Jugador.formatearMonedas(0.25));
            igual("3.5", Jugador.formatearMonedas(3.5));
            igual("10.25", Jugador.formatearMonedas(10.25));
        });

        caso("progreso: persistencia de monedas al guardar y cargar, y compatibilidad hacia atrás", () -> {
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots_monedas");
            Guardado.carpeta = tmp;
            try {
                Jugador j = new Jugador("Saldo Test");
                j.etiqueta = Etiqueta.JUNIOR_1;
                Progreso.despuesDelEjercicio(j, metajavbots.ejercicios.Banco.delJuego().todos().get(0));
                Progreso.despuesDeLaPelea(j, pelea(Registro.Resultado.VICTORIA, 0.4), "ACERO", new Random(3));
                igual(3.25, j.monedas, "saldo antes de guardar");
                Guardado.guardar(j);

                Jugador cargado = Guardado.leer(Guardado.carpetaDe("Saldo Test"));
                igual(3.25, cargado.monedas, "monedas conservadas al guardar y cargar");

                // Partida antigua sin el campo monedas: debe cargar en 0.
                Path archivo = tmp.resolve(Guardado.carpetaDe("Saldo Test")).resolve("partida.txt");
                List<String> sinMonedas = new ArrayList<>();
                for (String linea : Files.readAllLines(archivo, java.nio.charset.StandardCharsets.UTF_8)) {
                    if (!linea.startsWith("monedas =")) sinMonedas.add(linea);
                }
                Files.write(archivo, sinMonedas, java.nio.charset.StandardCharsets.UTF_8);
                Jugador antiguo = Guardado.leer(Guardado.carpetaDe("Saldo Test"));
                igual(0.0, antiguo.monedas, "partida sin campo monedas empieza en 0");
            } finally {
                Guardado.carpeta = antes;
            }
        });
    }
}
