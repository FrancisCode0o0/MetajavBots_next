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
        apuntarTienda();
        apuntarActivacion();
        apuntarLimitesDeCombate();
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
            List<metajavbots.piezas.Pieza> iniciales = Progreso.piezasDisponibles(j);
            igual(4, iniciales.size(), "en NOOB_1 empieza con exactamente 4 complementos básicos: " + iniciales);
            cierto(Progreso.piezaDisponible(j, c.pieza("laser_basico")), "arma básica de inicio");
            cierto(Progreso.piezaDisponible(j, c.pieza("cabeza_radar")), "cabeza básica de inicio");
            cierto(Progreso.piezaDisponible(j, c.pieza("piernas_bipedo")), "piernas básicas de inicio");
            cierto(Progreso.piezaDisponible(j, c.pieza("nucleo_basico")), "núcleo básico de inicio");
            falso(Progreso.piezaDisponible(j, c.pieza("canon_ligero")), "cañón ligero ya requiere desbloqueo");
            falso(Progreso.piezaDisponible(j, c.pieza("punos")), "puños ya requieren desbloqueo");
            cierto(Progreso.piezaUsable(j, c.pieza("canon_ligero"), Modelo.de("cobalto")), "de fábrica en cobalto sigue siendo usable");
            falso(Progreso.piezaDisponible(j, c.pieza("laser_pesado")), "la de 6 victorias, no");
            j.historial.add(pelea(Registro.Resultado.VICTORIA, 0.2));
            cierto(Progreso.piezaDisponible(j, c.pieza("canon_ligero")), "con 1 victoria se desbloquea cañón ligero");
            falso(Progreso.piezaDisponible(j, c.pieza("punos")), "con 1 victoria aún no tiene puños");
            j.historial.add(pelea(Registro.Resultado.VICTORIA, 0.2));
            cierto(Progreso.piezaDisponible(j, c.pieza("punos")), "con 2 victorias se desbloquean puños");
            for (int i = 0; i < 4; i++) j.historial.add(pelea(Registro.Resultado.VICTORIA, 0.3));
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

    private static void apuntarTienda() {
        caso("tienda: catálogo por rangos, compra válida, sin saldo y bloqueo de rango superior", () -> {
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            cierto(!c.comprables(Etiqueta.Categoria.NOOB).isEmpty(), "hay piezas comprables en NOOB");
            cierto(!c.comprables(Etiqueta.Categoria.JUNIOR).isEmpty(), "hay piezas comprables en JUNIOR");
            cierto(!c.comprables(Etiqueta.Categoria.SENIOR).isEmpty(), "hay piezas comprables en SENIOR");
            falso(c.pieza("laser_basico").comprable(), "pieza inicial no se vende");
            cierto(c.pieza("laser_basico_2").comprable(), "Mk II de pieza inicial sí se vende");
            falso(c.pieza("laser_aurora").comprable(), "legendaria de desafío no se vende");
            falso(c.pieza("herencia_espada").comprable(), "épica de jefe no se vende");
            cierto(!metajavbots.pantallas.Tienda.piezasFiltradas(Etiqueta.Categoria.NOOB, metajavbots.piezas.Pieza.Ranura.BRAZO).isEmpty(),
                    "filtro por rango y ranura funciona");

            metajavbots.piezas.Pieza noob = c.pieza("canon_ligero");
            metajavbots.piezas.Pieza cara = c.pieza("escopeta");
            metajavbots.piezas.Pieza junior = c.pieza("laser_pesado");
            metajavbots.piezas.Pieza senior = c.pieza("bola_cadena");
            igual(Etiqueta.Categoria.NOOB, noob.rangoTienda, "canon_ligero es NOOB");
            igual(Etiqueta.Categoria.JUNIOR, junior.rangoTienda, "laser_pesado es JUNIOR");
            igual(Etiqueta.Categoria.SENIOR, senior.rangoTienda, "bola_cadena es SENIOR");

            Jugador j = new Jugador("Comprador");
            j.etiqueta = Etiqueta.NOOB_1;
            j.sumarMonedas(6.0);

            // Compra válida: descuenta monedas y queda pendiente de activar (no equipable aún).
            igual(Progreso.CompraResultado.COMPRADA, Progreso.comprarPieza(j, noob), "compra válida en NOOB");
            igual(Jugador.redondearMonedas(6.0 - noob.precio), j.monedas, "descuenta precio exacto");
            cierto(j.piezasPendientes.contains(noob.id), "guarda id en piezasPendientes");
            cierto(Progreso.piezaPendiente(j, noob), "pieza pendiente tras comprar");
            falso(Progreso.piezaDisponible(j, noob), "pieza bloqueada hasta superar reto de código");
            falso(Progreso.piezaUsable(j, noob, Modelo.de("mazo")), "no equipable en Taller hasta activar");

            // Comprar una ya adquirida (aunque siga pendiente) no descuenta de nuevo.
            double saldoTrasCompra = j.monedas;
            igual(Progreso.CompraResultado.YA_LA_TIENE, Progreso.comprarPieza(j, noob), "no recompra pieza pendiente");
            igual(saldoTrasCompra, j.monedas, "saldo intacto si ya la compró");

            // Intento sin saldo suficiente.
            igual(Progreso.CompraResultado.SALDO_INSUFICIENTE, Progreso.comprarPieza(j, cara), "rechaza si falta saldo");
            igual(saldoTrasCompra, j.monedas, "saldo intacto sin saldo suficiente");
            falso(Progreso.piezaDisponible(j, cara), "pieza no añadida sin saldo");

            // Intento de pieza de rango superior bloqueada (aun teniendo muchas monedas).
            j.sumarMonedas(100.0);
            double saldoAlto = j.monedas;
            igual(Progreso.CompraResultado.RANGO_INSUFICIENTE, Progreso.comprarPieza(j, junior), "NOOB no puede comprar JUNIOR");
            igual(Progreso.CompraResultado.RANGO_INSUFICIENTE, Progreso.comprarPieza(j, senior), "NOOB no puede comprar SENIOR");
            igual(saldoAlto, j.monedas, "saldo intacto tras bloqueo de rango");
            falso(Progreso.piezaDisponible(j, junior), "pieza JUNIOR sigue bloqueada");

            // Al subir a JUNIOR, desbloquea JUNIOR pero SENIOR sigue bloqueada.
            j.etiqueta = Etiqueta.JUNIOR_1;
            igual(Progreso.CompraResultado.COMPRADA, Progreso.comprarPieza(j, junior), "JUNIOR puede comprar pieza JUNIOR");
            cierto(Progreso.piezaPendiente(j, junior), "pieza JUNIOR pendiente tras compra");
            falso(Progreso.piezaDisponible(j, junior), "pieza JUNIOR no disponible hasta reto");
            igual(Progreso.CompraResultado.RANGO_INSUFICIENTE, Progreso.comprarPieza(j, senior), "JUNIOR no puede comprar SENIOR");

            // Al subir a SENIOR, desbloquea SENIOR.
            j.etiqueta = Etiqueta.SENIOR_1;
            igual(Progreso.CompraResultado.COMPRADA, Progreso.comprarPieza(j, senior), "SENIOR puede comprar pieza SENIOR");
            cierto(Progreso.piezaPendiente(j, senior), "pieza SENIOR pendiente tras compra");
        });

        caso("tienda: persistencia de pieza pendiente y monedas al guardar y cargar", () -> {
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots_tienda");
            Guardado.carpeta = tmp;
            try {
                CatalogoDePiezas c = CatalogoDePiezas.delJuego();
                metajavbots.piezas.Pieza pz = c.pieza("ametralladora");
                Jugador j = new Jugador("Tienda Save");
                j.etiqueta = Etiqueta.NOOB_2;
                j.sumarMonedas(12.5);
                igual(Progreso.CompraResultado.COMPRADA, Progreso.comprarPieza(j, pz), "compra antes de guardar");
                double saldoEsperado = Jugador.redondearMonedas(12.5 - pz.precio);
                igual(saldoEsperado, j.monedas, "saldo tras compra");
                Guardado.guardar(j);

                Jugador cargado = Guardado.leer(Guardado.carpetaDe("Tienda Save"));
                igual(saldoEsperado, cargado.monedas, "saldo descontado persiste en partida.txt");
                cierto(cargado.piezasPendientes.contains(pz.id), "pieza comprada persiste en piezasPendientes");
                cierto(Progreso.piezaPendiente(cargado, pz), "pieza sigue pendiente tras cargar");
                falso(Progreso.piezaDisponible(cargado, pz), "sigue bloqueada hasta superar el reto");
            } finally {
                Guardado.carpeta = antes;
            }
        });
    }

    private static void apuntarActivacion() {
        caso("tienda: reto de código trilingüe por mecánica, desbloqueo al pasarlo y persistencia de ajustes", () -> {
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            String[] muestras = {"canon_ligero", "punos", "escudo_brazo", "brazo_reparador", "lanzallamas",
                "cabeza_escudo", "ruedas", "nucleo_bateria", "naturaleza_furia"};
            for (String id : muestras) {
                metajavbots.piezas.Pieza pz = c.pieza(id);
                metajavbots.ejercicios.Ejercicio reto = metajavbots.ejercicios.RetoDePieza.para(pz);
                cierto(!reto.titulo.es.isEmpty() && !reto.titulo.de.isEmpty() && !reto.titulo.en.isEmpty(), id + ": título trilingüe");
                cierto(!reto.enunciado.es.isEmpty() && !reto.enunciado.de.isEmpty() && !reto.enunciado.en.isEmpty(), id + ": enunciado trilingüe");
                cierto(!reto.pista.es.isEmpty() && !reto.pista.de.isEmpty() && !reto.pista.en.isEmpty(), id + ": pista trilingüe");
                if (metajavbots.codigo.Compilador.hayCompilador()) {
                    metajavbots.ejercicios.Comprobador.Informe pl = metajavbots.ejercicios.Comprobador.comprobar(
                            reto, reto.codigoInicial(), java.util.Locale.ROOT);
                    cierto(pl.compila(), id + ": plantilla compila " + pl.errores);
                    falso(pl.todoBien(), id + ": plantilla falla casos");
                    metajavbots.ejercicios.Comprobador.Informe sol = metajavbots.ejercicios.Comprobador.comprobar(
                            reto, reto.codigoSolucion(), java.util.Locale.ROOT);
                    cierto(sol.todoBien(), id + ": solución pasa todos los casos " + sol.errores);
                }
            }

            // Flujo completo: compra -> bloqueada -> pasar reto -> desbloqueada y equipada con ajustes acotados.
            Path antes = Guardado.carpeta;
            Path tmp = Files.createTempDirectory("metajavbots_activar");
            Guardado.carpeta = tmp;
            try {
                metajavbots.piezas.Pieza pz = c.pieza("ametralladora");
                Jugador j = new Jugador("Activador");
                j.robots.add(new Jugador.Robot("Spark", "cobalto"));
                j.sumarMonedas(20.0);
                igual(Progreso.CompraResultado.COMPRADA, Progreso.comprarPieza(j, pz), "compra");
                falso(Progreso.piezaDisponible(j, pz), "bloqueada tras compra");
                falso(Progreso.piezaUsable(j, pz, Modelo.de("cobalto")), "no equipable antes del reto");

                metajavbots.ejercicios.Ejercicio reto = metajavbots.ejercicios.RetoDePieza.para(pz);
                if (metajavbots.codigo.Compilador.hayCompilador()) {
                    cierto(metajavbots.ejercicios.Comprobador.comprobar(reto, reto.codigoSolucion(), java.util.Locale.ROOT).todoBien(),
                            "pasa el reto de activación");
                }
                java.util.Map<String, Double> ajustes = new java.util.LinkedHashMap<>();
                ajustes.put("dano", 2.2);
                ajustes.put("enfriamiento", 0.0);
                cierto(Progreso.activarPieza(j, pz, ajustes), "activa la pieza");
                falso(Progreso.piezaPendiente(j, pz), "ya no está pendiente");
                cierto(Progreso.piezaDisponible(j, pz), "queda desbloqueada");
                cierto(Progreso.piezaUsable(j, pz, Modelo.de("cobalto")), "ya es equipable en Taller");
                igual(pz.id, j.robot().piezas.get(Montaje.Hueco.IZQUIERDA), "equipada en el robot activo");
                double enfAcotado = j.ajustesDePiezas.get(pz.id).get("enfriamiento");
                cierto(enfAcotado >= metajavbots.combate.Cuerpo.MIN_ENFRIAMIENTO, "enfriamiento <= 0 acotado: " + enfAcotado);

                Guardado.guardar(j);
                Jugador cargado = Guardado.leer(Guardado.carpetaDe("Activador"));
                cierto(Progreso.piezaDisponible(cargado, pz), "desbloqueada tras recargar");
                falso(Progreso.piezaPendiente(cargado, pz), "no vuelve a pendiente");
                igual(enfAcotado, cargado.ajustesDePiezas.get(pz.id).get("enfriamiento"), "ajustes persisten");
            } finally {
                Guardado.carpeta = antes;
            }
        });
    }

    private static void apuntarLimitesDeCombate() {
        caso("combate: rechazo y acotado de valores abusivos en Armas, Cuerpo y ajustes", () -> {
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            metajavbots.piezas.Pieza arma = c.pieza("canon_ligero");
            metajavbots.piezas.Pieza escudo = c.pieza("escudo_brazo");
            metajavbots.piezas.Pieza nucleo = c.pieza("nucleo_basico");

            // Acotado de ajustes propuestos por el jugador (valores abusivos, NaN, infinitos y claves inválidas).
            java.util.Map<String, Double> abusivos = new java.util.LinkedHashMap<>();
            abusivos.put("dano", 99999.0);
            abusivos.put("enfriamiento", -10.0);
            abusivos.put("trampa", 1234.0);
            java.util.Map<String, Double> acotArma = metajavbots.ejercicios.RetoDePieza.acotarAjustes(arma, abusivos);
            falso(acotArma.containsKey("trampa"), "clave desconocida rechazada");
            cierto(acotArma.get("dano") <= metajavbots.combate.Armas.MAX_DANO && acotArma.get("dano") <= arma.valor("dano") * 1.2,
                    "daño acotado: " + acotArma.get("dano"));
            cierto(acotArma.get("enfriamiento") >= metajavbots.combate.Cuerpo.MIN_ENFRIAMIENTO,
                    "enfriamiento acotado >= mínimo: " + acotArma.get("enfriamiento"));

            java.util.Map<String, Double> abusEsc = java.util.Map.of("bloqueo", 1.0, "duracion", Double.POSITIVE_INFINITY);
            java.util.Map<String, Double> acotEsc = metajavbots.ejercicios.RetoDePieza.acotarAjustes(escudo, abusEsc);
            cierto(acotEsc.get("bloqueo") <= metajavbots.combate.Armas.MAX_BLOQUEO, "bloqueo nunca 100 %");
            cierto(Double.isFinite(acotEsc.get("duracion")), "infinito rechazado");

            java.util.Map<String, Double> abusNuc = java.util.Map.of("recarga", -5.0, "energiaMax", Double.NaN);
            java.util.Map<String, Double> acotNuc = metajavbots.ejercicios.RetoDePieza.acotarAjustes(nucleo, abusNuc);
            cierto(acotNuc.get("recarga") >= metajavbots.combate.Cuerpo.MIN_RECARGA, "recarga <= 0 rechazada");
            cierto(Double.isFinite(acotNuc.get("energiaMax")), "NaN rechazado");

            // Validación directa en el motor de combate (Armas y Cuerpo).
            cierto(metajavbots.combate.Cuerpo.enfriamientoSeguro(0, 0) >= metajavbots.combate.Cuerpo.MIN_ENFRIAMIENTO, "enfriamiento 0 acotado");
            cierto(metajavbots.combate.Cuerpo.enfriamientoSeguro(-20, 5) >= metajavbots.combate.Cuerpo.MIN_ENFRIAMIENTO, "enfriamiento negativo acotado");
            cierto(metajavbots.combate.Cuerpo.enfriamientoSeguro(Double.NaN, 0) >= metajavbots.combate.Cuerpo.MIN_ENFRIAMIENTO, "enfriamiento NaN acotado");
            cierto(metajavbots.combate.Cuerpo.recargaSegura(0) >= metajavbots.combate.Cuerpo.MIN_RECARGA, "recarga 0 acotada");
            cierto(metajavbots.combate.Cuerpo.recargaSegura(-3) >= metajavbots.combate.Cuerpo.MIN_RECARGA, "recarga negativa acotada");
            cierto(metajavbots.combate.Cuerpo.recargaSegura(500) <= metajavbots.combate.Cuerpo.MAX_RECARGA, "recarga excesiva acotada");
            igual(0.0, metajavbots.combate.Armas.danoSeguro(-15), "daño negativo rechazado");
            igual(0.0, metajavbots.combate.Armas.danoSeguro(Double.NaN), "daño NaN rechazado");
            igual(metajavbots.combate.Armas.MAX_DANO, metajavbots.combate.Armas.danoSeguro(9999), "daño fuera de rango acotado");
            igual(metajavbots.combate.Armas.MAX_DANO_EFECTIVO, metajavbots.combate.Armas.danoEfectivoSeguro(9999), "daño efectivo acotado");
            igual(metajavbots.combate.Armas.MAX_BLOQUEO, metajavbots.combate.Armas.bloqueoSeguro(1.0), "bloqueo 100 % acotado");
            igual(metajavbots.combate.Armas.MAX_PROYECTILES, metajavbots.combate.Armas.proyectilesSeguros(200), "proyectiles excesivos acotados");

            // Pelea real con pieza manipulada (cadencia 0, daño 9999, recarga -10): el motor evita abusos.
            metajavbots.piezas.Pieza rota = c.pieza("laser_basico").conValores(java.util.Map.of("dano", 9999.0, "enfriamiento", 0.0));
            metajavbots.piezas.Pieza nucRoto = c.pieza("nucleo_basico").conValores(java.util.Map.of("recarga", -10.0));
            Montaje mA = new Montaje(Modelo.de("cobalto"));
            mA.poner(Montaje.Hueco.IZQUIERDA, rota);
            mA.poner(Montaje.Hueco.NUCLEO, nucRoto);
            metajavbots.combate.Combatiente ca = new metajavbots.combate.Combatiente("A", mA, null);
            metajavbots.combate.Combatiente cb = new metajavbots.combate.Combatiente("B", new Montaje(Modelo.de("cobalto")), null);
            metajavbots.combate.Pelea pelea = new metajavbots.combate.Pelea(ca, cb, 7L);
            pelea.empezarRonda();
            metajavbots.combate.Cuerpo a = pelea.cuerpo(0);
            metajavbots.combate.Cuerpo b = pelea.cuerpo(1);
            b.x = a.x + 150;
            b.y = a.y;
            a.mira = 0;
            cierto(a.atacar(metajavbots.combate.Cuerpo.IZQUIERDA), "primer ataque permitido");
            falso(a.listo(metajavbots.combate.Cuerpo.IZQUIERDA), "enfriamiento evita cadencia infinita");
            double energiaTrasDisparo = a.energia;
            pelea.avanzar();
            falso(a.listo(metajavbots.combate.Cuerpo.IZQUIERDA), "sigue en enfriamiento en el turno siguiente");
            cierto(a.energia > energiaTrasDisparo, "recarga acotada > 0 aunque el núcleo tuviera recarga <= 0");
            cierto(cb.danoTotalRecibido <= metajavbots.combate.Armas.MAX_DANO_EFECTIVO,
                    "daño en pelea acotado: " + cb.danoTotalRecibido);
        });
    }
}
