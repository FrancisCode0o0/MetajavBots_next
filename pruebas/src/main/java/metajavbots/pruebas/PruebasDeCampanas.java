package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import metajavbots.Juego;
import metajavbots.api.Metajavbot;
import metajavbots.campanas.Campana;
import metajavbots.campanas.EscenaDeCampana;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.pantallas.Campanas;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.PantallaEscenaCampana;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * Pruebas deterministas de campañas ampliadas con escenas ilustradas:
 * 1) Dos campañas principales (Creed ampliada >= 8 capítulos e Isla Clandestina >= 8 capítulos).
 * 2) Escenas ilustradas válidas, no nulas, trilingües (ES/DE/EN), con marcadores balanceados
 *    y declinación alemana correcta en cada capítulo.
 * 3) Progresión completa de principio a fin, guardado/carga independiente y compatibilidad
 *    con los 7 entrenadores de Entrenador.java.
 * 4) Salto de escenas y ausencia de hilos o recursos colgados al salir.
 */
final class PruebasDeCampanas {

    private PruebasDeCampanas() {
    }

    static void apuntar() {
        apuntarDosCampanasYProgresionRangos();
        apuntarEscenasTrilinguesYMarcadores();
        apuntarProgresionCompletaGuardadoYEntrenadores();
        apuntarSaltoYSinHilosColgados();
    }

    private static void apuntarDosCampanasYProgresionRangos() {
        caso("campañas ampliadas: dos campañas principales (>= 8 caps.), rangos Noob-Especial y jefes con hospital", () -> {
            List<Campana> principales = Campana.principales();
            cierto(principales.size() >= 2, "hay al menos 2 campañas principales: " + principales.size());

            Campana creed = Campana.de("creed1");
            Campana isla = Campana.de("isla_torneo");
            cierto(creed != null && isla != null, "cargan creed1 e isla_torneo sin errores");
            cierto(creed.capitulos.size() >= 8, "creed1 ampliada tiene >= 8 capítulos (antes 3): " + creed.capitulos.size());
            cierto(isla.capitulos.size() >= 8, "isla_torneo tiene >= 8 capítulos: " + isla.capitulos.size());

            for (Campana c : List.of(creed, isla)) {
                Set<String> rangos = new HashSet<>();
                int jefes = 0;
                double nivelAnterior = 0.0;
                for (Campana.Capitulo k : c.capitulos) {
                    rangos.add(k.rango);
                    if (k.esJefe) jefes++;
                    cierto(k.nivel >= nivelAnterior, c.id + " nivel progresivo en cap " + k.numero);
                    nivelAnterior = k.nivel;
                }
                cierto(rangos.containsAll(Set.of("Noob", "Junior", "Senior", "Especial")),
                        c.id + " cubre los 4 rangos: " + rangos);
                cierto(jefes >= 3, c.id + " tiene al menos 3 jefes con hospital: " + jefes);
            }

            ConocimientoDelEntrenador.Entrada info = ConocimientoDelEntrenador.de("campanas_creed_y_biografias");
            Concepto concepto = Concepto.de("campanas_creed_y_biografias");
            cierto(info != null && concepto != null, "ficha de campañas en conocimiento del entrenador");
            cierto(info.resumenIa().contains("Secret Island") && info.resumenIa().contains("Creed"),
                    "resumen.ia incluye ambas campañas");
        });
    }

    private static void apuntarEscenasTrilinguesYMarcadores() {
        caso("campañas ampliadas: cada capítulo tiene escena ilustrada válida, textos ES/DE/EN y marcadores emparejados", () -> {
            String[] piezasDe = {"Kopf", "linker Arm", "rechter Arm", "Beine"};
            for (Campana c : Campana.deSerie()) {
                for (Campana.Capitulo k : c.capitulos) {
                    EscenaDeCampana esc = c.escenaDe(k);
                    cierto(esc != null, c.id + ":" + k.numero + " escena no nula");
                    cierto(!esc.vinetas.isEmpty(), c.id + ":" + k.numero + " al menos una viñeta");
                    for (Idioma idi : Idioma.values()) {
                        cierto(!esc.titulo.en(idi).isBlank(), esc.id + " título en " + idi);
                        for (EscenaDeCampana.Vineta v : esc.vinetas) {
                            cierto(v != null && v.decorado != null && v.animacion != null, esc.id + " viñeta válida");
                            String narr = v.narracion.en(idi);
                            cierto(!narr.isBlank(), esc.id + " narración no vacía en " + idi);
                            comprobarLlavesEmparejadas(narr, esc.id + ".narr." + idi);
                            comprobarLlavesEmparejadas(v.bocadilloIzq.en(idi), esc.id + ".izq." + idi);
                            comprobarLlavesEmparejadas(v.bocadilloDer.en(idi), esc.id + ".der." + idi);

                            String resNarr = v.textoNarracion(idi, "Don Tuercas", k.rival.en(idi), null);
                            falso(resNarr.contains("{") || resNarr.contains("}"), esc.id + " sin marcadores sin resolver: " + resNarr);
                            if (idi == Idioma.DE && (v.bocadilloIzq.de.contains("{pieza}") || v.bocadilloDer.de.contains("{pieza}"))) {
                                for (String pz : piezasDe) {
                                    String b1 = v.textoBocadilloIzq(idi, "Meister Mutter", k.rival.de, pz);
                                    String b2 = v.textoBocadilloDer(idi, "Meister Mutter", k.rival.de, pz);
                                    falso(b1.contains("{pieza}") || b2.contains("{pieza}"), esc.id + " {pieza} resuelto en DE");
                                    falso(b1.contains("meinem Beine") || b2.contains("dem Beine"), esc.id + " declinación DE válida");
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    private static void comprobarLlavesEmparejadas(String texto, String contexto) {
        if (texto == null) return;
        int abiertas = 0;
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            if (ch == '{') abiertas++;
            else if (ch == '}') {
                abiertas--;
                cierto(abiertas >= 0, "llave de cierre sin apertura en " + contexto + ": " + texto);
            }
        }
        igual(0, abiertas, "llaves balanceadas en " + contexto + ": " + texto);
    }

    private static void apuntarProgresionCompletaGuardadoYEntrenadores() {
        caso("campañas ampliadas: progresión completa con combates simulados, guardado separado y 7 entrenadores", () -> {
            Campana creed = Campana.de("creed1");
            Campana isla = Campana.de("isla_torneo");
            Path carpetaAnterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("metajavbots_campanas_");
            try {
                Guardado.carpeta = temp;
                Juego juego = new Juego();
                Jugador jug = new Jugador("CampeonTest");
                jug.robots.add(new Jugador.Robot("BotTest", "cobalto"));
                juego.jugador = jug;

                // Avanzar solo los 3 primeros capítulos de creed1 y guardar: isla_torneo debe seguir en 0.
                for (int i = 0; i < 3; i++) {
                    simularCapitulo(juego, creed, creed.capitulos.get(i));
                }
                igual(3, creed.ganados(jug.capitulosGanados), "creed1 lleva 3 ganados");
                igual(0, isla.ganados(jug.capitulosGanados), "isla_torneo sigue en 0");

                Guardado.guardar(jug);
                Jugador cargado = Guardado.leer(Guardado.carpetaDe(jug.nombre));
                igual(3, creed.ganados(cargado.capitulosGanados), "creed1 persiste tras cargar");
                igual(0, isla.ganados(cargado.capitulosGanados), "isla_torneo independiente tras cargar");

                // Completar ambas campañas de principio a fin.
                juego.jugador = cargado;
                for (int i = 3; i < creed.capitulos.size(); i++) {
                    simularCapitulo(juego, creed, creed.capitulos.get(i));
                }
                for (Campana.Capitulo k : isla.capitulos) {
                    simularCapitulo(juego, isla, k);
                }
                cierto(creed.terminada(cargado.capitulosGanados), "creed1 terminada al completo");
                cierto(isla.terminada(cargado.capitulosGanados), "isla_torneo terminada al completo");

                Guardado.guardar(cargado);
                Jugador finalCargado = Guardado.leer(Guardado.carpetaDe(cargado.nombre));
                cierto(creed.terminada(finalCargado.capitulosGanados), "creed1 terminada tras recargar");
                cierto(isla.terminada(finalCargado.capitulosGanados), "isla_torneo terminada tras recargar");

                // Compatibilidad con todos los entrenadores de Entrenador.java en ES, DE y EN.
                List<Entrenador> entrenadores = Entrenador.todos();
                igual(7, entrenadores.size(), "7 entrenadores");
                for (Idioma idi : Idioma.values()) {
                    T.idioma = idi;
                    Set<String> vocesCreed = new HashSet<>();
                    Set<String> vocesIsla = new HashSet<>();
                    for (Entrenador en : entrenadores) {
                        finalCargado.entrenador = en.id;
                        igual(en.id, creed.entrenadorPara(finalCargado).id, "creed usa " + en.id);
                        igual(en.id, isla.entrenadorPara(finalCargado).id, "isla usa " + en.id);
                        String vc = Campana.vozDeEsquina(creed, en, idi, false, false);
                        String vi = Campana.vozDeEsquina(isla, en, idi, false, false);
                        cierto(!vc.isBlank() && !vi.isBlank(), "voces presentes para " + en.id);
                        cierto(vocesCreed.add(vc), "voz creed única para " + en.id + " en " + idi);
                        cierto(vocesIsla.add(vi), "voz isla única para " + en.id + " en " + idi);
                    }
                }
                T.idioma = Idioma.ES;
            } finally {
                Guardado.carpeta = carpetaAnterior;
                borrarRecursivo(temp);
            }
        });
    }

    private static void simularCapitulo(Juego juego, Campana c, Campana.Capitulo k) {
        Encuentro enc = Campanas.encuentro(juego, c, k);
        igual(k.esJefe, enc.entrenadorHospitalizado, c.id + ":" + k.numero + " hospitalización jefe");
        Combatiente yo = new Combatiente("Yo", new Montaje(Modelo.de("cobalto")), RobotSimple::new);
        Pelea p = new Pelea(yo, enc.rival, 100L + k.numero);
        p.rondas = 1;
        p.empezarRonda();
        for (int t = 0; t < 10 && p.estado() == Pelea.Estado.LUCHANDO; t++) p.avanzar();
        cierto(enc.alAcabar != null, "callback alAcabar presente");
        enc.alAcabar.accept(true);
    }

    private static void apuntarSaltoYSinHilosColgados() {
        caso("campañas ampliadas: las escenas se avanzan y se saltan sin dejar hilos ni recursos colgados", () -> {
            int hilosAntes = Thread.activeCount();
            Campana isla = Campana.de("isla_torneo");
            Campana.Capitulo capFinal = isla.capitulos.get(isla.capitulos.size() - 1);
            EscenaDeCampana esc = isla.escenaDe(capFinal);
            cierto(esc.vinetas.size() >= 2, "escena final tiene varias viñetas");

            // Avance paso a paso.
            EscenaDeCampana.Controlador ctrl = esc.controlador();
            igual(0, ctrl.indice(), "empieza en viñeta 0");
            falso(ctrl.terminada(), "no terminada al iniciar");
            ctrl.avanzarTiempo(0.5f);
            ctrl.siguiente();
            igual(1, ctrl.indice(), "avanza a viñeta 1");
            while (!ctrl.terminada()) ctrl.siguiente();
            cierto(ctrl.terminada(), "termina al pasar la última viñeta");

            // Salto inmediato y salida limpia desde PantallaEscenaCampana.
            boolean[] terminado = {false};
            PantallaEscenaCampana pantalla = new PantallaEscenaCampana(isla, capFinal, esc, true, () -> terminado[0] = true);
            pantalla.alEntrar(null);
            pantalla.controlador().saltar();
            cierto(pantalla.controlador().terminada(), "saltar termina la escena al instante");
            pantalla.alSalir(null);
            falso(terminado[0], "alSalir no dispara efectos secundarios");

            int hilosDespues = Thread.activeCount();
            cierto(hilosDespues <= hilosAntes + 1, "sin hilos colgados tras escenas (" + hilosAntes + " -> " + hilosDespues + ")");
        });
    }

    private static void borrarRecursivo(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try (var s = Files.walk(dir)) {
            s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                    // limpieza temporal
                }
            });
        } catch (Exception ignored) {
            // limpieza temporal
        }
    }

    private static final class RobotSimple extends Metajavbot {
        @Override
        public void cadaTurno() {
            avanzar(10);
            girar(4);
        }
    }
}
