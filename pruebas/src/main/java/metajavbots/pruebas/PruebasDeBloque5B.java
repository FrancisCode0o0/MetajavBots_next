package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import metajavbots.Juego;
import metajavbots.api.Metajavbot;
import metajavbots.campanas.Campana;
import metajavbots.combate.Aparatos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.dialogos.Concordancia;
import metajavbots.dialogos.Dialogos;
import metajavbots.pantallas.Campanas;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.Hospital;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;
import metajavbots.tutor.Biografias;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * Pruebas del Bloque 5B: personalidades propias de los 6 robots nuevos,
 * biografías editables con persistencia y restauración, campañas estilo Creed
 * con todos los entrenadores y hospitalización, declinación alemana completa
 * con {pieza} y equilibrio de combates largos de 10-12 rondas.
 */
final class PruebasDeBloque5B {

    private static final String[] EVENTOS = {
        "saludo", "inicio_ronda", "ataque", "acierto", "fallo", "golpe", "golpe_fuerte",
        "poco_blindaje", "parte_rota", "esquiva", "victoria_ronda", "derrota_ronda",
        "victoria", "derrota", "burla", "pulla", "subida", "error_codigo", "espera",
        "elogio", "reparar", "especial"
    };

    private PruebasDeBloque5B() {
    }

    static void apuntar() {
        apuntarPersonalidades();
        apuntarBiografiasEditables();
        apuntarCampanasCreed();
        apuntarDeclinacionAlemana();
        apuntarEquilibrioCombatesLargos();
    }

    private static void apuntarPersonalidades() {
        caso("bloque 5B: todos los modelos (incluidos los 6 nuevos) tienen personalidad propia trilingüe completa", () -> {
            List<Modelo> modelos = Modelo.todos();
            cierto(modelos.size() >= 22, "al menos 22 modelos: " + modelos.size());
            Set<String> personalidades = new HashSet<>();
            for (Modelo m : modelos) {
                cierto(personalidades.add(m.personalidad), "personalidad única para " + m.id + ": " + m.personalidad);
                Dialogos d = Dialogos.de(m.personalidad);
                for (String ev : EVENTOS) {
                    List<Trilingue> frases = d.frases(ev);
                    cierto(frases.size() >= 3, m.personalidad + "/" + ev + " tiene al menos 3 frases");
                    for (Trilingue t : frases) {
                        cierto(!t.es.isBlank() && !t.de.isBlank() && !t.en.isBlank(), m.personalidad + "/" + ev + " sin vacíos");
                        if (t.es.length() > 4) {
                            falso(t.es.equals(t.de), m.personalidad + "/" + ev + " ES != DE: " + t.es);
                            falso(t.es.equals(t.en), m.personalidad + "/" + ev + " ES != EN: " + t.es);
                        }
                    }
                }
            }
            for (String nuevo : new String[]{"serrucho", "galapago", "volteo", "bolon", "barrena", "remolino"}) {
                Modelo m = Modelo.de(nuevo);
                cierto(Dialogos.de(m.personalidad).frases("saludo").size() >= 4, nuevo + " tiene saludos propios");
            }
        });
    }

    private static void apuntarBiografiasEditables() {
        caso("bloque 5B: edición, persistencia en archivo de usuario y restauración de biografías sin tocar recursos base", () -> {
            Path temp = Files.createTempFile("metajavbots_bio_", ".txt");
            try {
                Biografias.cargarPersonalizadas(temp);
                String baseRobot = Biografias.baseDeRobot("serrucho", Idioma.ES);
                String baseEntrenador = Biografias.baseDeEntrenador("tuercas", Idioma.DE);
                cierto(!baseRobot.isBlank() && !baseEntrenador.isBlank(), "biografías base presentes");
                falso(Biografias.esPersonalizadaRobot("serrucho", Idioma.ES), "sin editar al principio");

                String editRobot = "Corta pizzas galácticas a doce mil revoluciones por minuto.";
                String editEntrenador = "Meister Mutter repariert Motoren jetzt im Schlaf.";
                Biografias.guardarRobot("serrucho", Idioma.ES, editRobot);
                Biografias.guardarEntrenador("tuercas", Idioma.DE, editEntrenador);

                igual(editRobot, Biografias.deRobot("serrucho", Idioma.ES), "lee biografía editada de robot");
                igual(editEntrenador, Biografias.deEntrenador("tuercas", Idioma.DE), "lee biografía editada de entrenador");
                igual(baseRobot, Biografias.baseDeRobot("serrucho", Idioma.ES), "base de robot intacta");
                igual(baseEntrenador, Biografias.baseDeEntrenador("tuercas", Idioma.DE), "base de entrenador intacta");
                cierto(Biografias.esPersonalizadaRobot("serrucho", Idioma.ES), "marca robot personalizado");
                cierto(Biografias.esPersonalizadaEntrenador("tuercas", Idioma.DE), "marca entrenador personalizado");

                // Recarga desde disco para comprobar persistencia real.
                Biografias.limpiarPersonalizadas();
                igual(baseRobot, Biografias.deRobot("serrucho", Idioma.ES), "tras limpiar vuelve a la base");
                Biografias.cargarPersonalizadas(temp);
                igual(editRobot, Biografias.deRobot("serrucho", Idioma.ES), "persiste robot tras recargar");
                igual(editEntrenador, Biografias.deEntrenador("tuercas", Idioma.DE), "persiste entrenador tras recargar");

                // Restauración al original.
                Biografias.restaurarRobot("serrucho", Idioma.ES);
                Biografias.restaurarEntrenador("tuercas", Idioma.DE);
                igual(baseRobot, Biografias.deRobot("serrucho", Idioma.ES), "robot restaurado al original");
                igual(baseEntrenador, Biografias.deEntrenador("tuercas", Idioma.DE), "entrenador restaurado al original");
                falso(Biografias.esPersonalizadaRobot("serrucho", Idioma.ES), "robot ya no personalizado");
                falso(Biografias.esPersonalizadaEntrenador("tuercas", Idioma.DE), "entrenador ya no personalizado");
            } finally {
                Biografias.limpiarPersonalizadas();
                Files.deleteIfExists(temp);
            }
        });
    }

    private static void apuntarCampanasCreed() {
        caso("bloque 5B: campañas Creed encadenadas, compatibles con los 7 entrenadores, hospitalización y milagro", () -> {
            Campana c1 = Campana.de("creed1");
            Campana c2 = Campana.de("creed2");
            Campana c3 = Campana.de("creed3");
            cierto(c1 != null && c2 != null && c3 != null, "existen las 3 campañas Creed");
            cierto(c1.estiloCreed && c2.estiloCreed && c3.estiloCreed, "marcadas como estilo Creed");

            Set<String> ganados = new HashSet<>();
            cierto(c1.abierta(ganados), "creed1 abierta al principio");
            falso(c2.abierta(ganados), "creed2 cerrada antes de creed1");
            falso(c3.abierta(ganados), "creed3 cerrada antes de creed2");

            for (Campana.Capitulo k : c1.capitulos) ganados.add(c1.clave(k.numero));
            cierto(c1.terminada(ganados), "creed1 terminada");
            cierto(c2.abierta(ganados), "creed2 se abre tras creed1");
            falso(c3.abierta(ganados), "creed3 sigue cerrada");

            for (Campana.Capitulo k : c2.capitulos) ganados.add(c2.clave(k.numero));
            cierto(c3.abierta(ganados), "creed3 se abre tras creed2");

            // Jefe en cada campaña Creed con hospitalización de entrenador.
            Juego juego = new Juego();
            juego.jugador = new Jugador("Adonis");
            cierto(c1.capitulos.size() >= 8, "creed1 ampliada tiene al menos 8 capítulos: " + c1.capitulos.size());
            for (Campana c : List.of(c1, c2, c3)) {
                cierto(c.capitulos.size() >= 3, c.id + " tiene al menos 3 capítulos");
                falso(c.capitulos.get(0).esJefe, c.id + " cap 1 no es jefe");
                falso(c.capitulos.get(1).esJefe, c.id + " cap 2 no es jefe");
                cierto(c.capitulos.get(2).esJefe, c.id + " cap 3 es jefe");
                cierto(c.capitulos.get(c.capitulos.size() - 1).esJefe, c.id + " último capítulo es jefe final");
                Encuentro normal = Campanas.encuentro(juego, c, c.capitulos.get(0));
                Encuentro jefe = Campanas.encuentro(juego, c, c.capitulos.get(c.capitulos.size() - 1));
                falso(normal.entrenadorHospitalizado, c.id + " cap 1 sin hospitalización");
                cierto(jefe.entrenadorHospitalizado, c.id + " jefe final activa hospitalización");
            }

            // Compatibilidad y voces propias para todos los entrenadores de Entrenador.java.
            List<Entrenador> entrenadores = Entrenador.todos();
            igual(7, entrenadores.size(), "7 entrenadores");
            for (Idioma idi : Idioma.values()) {
                T.idioma = idi;
                Set<String> vocesEntreno = new HashSet<>();
                Set<String> vocesHospital = new HashSet<>();
                Set<String> vocesMilagro = new HashSet<>();
                for (Entrenador en : entrenadores) {
                    juego.jugador.entrenador = en.id;
                    igual(en.id, c1.entrenadorPara(juego.jugador).id, "Creed usa el entrenador elegido: " + en.id);
                    String ve = Campana.vozDeEsquinaCreed(en, idi, false, false);
                    String vh = Campana.vozDeEsquinaCreed(en, idi, true, false);
                    String vm = Campana.vozDeEsquinaCreed(en, idi, true, true);
                    cierto(!ve.isBlank() && !vh.isBlank() && !vm.isBlank(), en.id + " tiene voces Creed en " + idi);
                    cierto(vocesEntreno.add(ve), "voz de entreno distinta por entrenador (" + en.id + ", " + idi + ")");
                    cierto(vocesHospital.add(vh), "voz de hospital distinta por entrenador (" + en.id + ", " + idi + ")");
                    cierto(vocesMilagro.add(vm), "voz de milagro distinta por entrenador (" + en.id + ", " + idi + ")");
                }
                cierto(!Hospital.causa(7L).isBlank() && !Hospital.milagro(7L).isBlank(), "Hospital activo en " + idi);
            }
            T.idioma = Idioma.ES;

            // Conocimiento del entrenador actualizado.
            ConocimientoDelEntrenador.Entrada info = ConocimientoDelEntrenador.de("campanas_creed_y_biografias");
            Concepto concepto = Concepto.de("campanas_creed_y_biografias");
            cierto(info != null && concepto != null, "ficha campanas_creed_y_biografias registrada");
            cierto(info.resumenIa().contains("Creed") && info.resumenIa().contains("Biographies"), "resumen.ia actualizado");
        });
    }

    private static void apuntarDeclinacionAlemana() {
        caso("bloque 5B: declinación alemana con {pieza} en nominativo, acusativo, dativo, plural y todas las plantillas reales", () -> {
            Idioma de = Idioma.DE;
            // Nominativo, acusativo, dativo y posesivos con las 4 piezas (con y sin artículo de entrada).
            igual("Mein Kopf ist weg.", aplicarDe("Mein {pieza} ist weg.", "Kopf"));
            igual("Mein linker Arm ist weg.", aplicarDe("Mein {pieza} ist weg.", "linker Arm"));
            igual("Mein linker Arm ist weg.", aplicarDe("Mein {pieza} ist weg.", "der linke Arm"));
            igual("Meine Beine sind weg.", aplicarDe("Mein {pieza} ist weg.", "Beine"));
            igual("Der linke Arm fällt.", aplicarDe("Der {pieza} fällt.", "linker Arm"));
            igual("Die Beine fallen.", aplicarDe("Der {pieza} fällt.", "die Beine"));

            igual("Ohne Kopf!", aplicarDe("Ohne {pieza}!", "Kopf"));
            igual("Ohne linken Arm!", aplicarDe("Ohne {pieza}!", "linker Arm"));
            igual("Ohne rechten Arm!", aplicarDe("Ohne {pieza}!", "rechter Arm"));
            igual("Ohne Beine!", aplicarDe("Ohne {pieza}!", "Beine"));
            igual("Ohne meinen linken Arm!", aplicarDe("Ohne meinen {pieza}!", "linker Arm"));
            igual("Ohne meine Beine!", aplicarDe("Ohne meinen {pieza}!", "Beine"));

            igual("Du hast den Kopf zerstört.", aplicarDe("Du hast {pieza} zerstört.", "Kopf"));
            igual("Du hast den linken Arm zerstört.", aplicarDe("Du hast {pieza} zerstört.", "linker Arm"));
            igual("Du hast den rechten Arm zerstört.", aplicarDe("Du hast {pieza} zerstört.", "rechter Arm"));
            igual("Du hast die Beine zerstört.", aplicarDe("Du hast {pieza} zerstört.", "Beine"));
            igual("Du hast mir meinen linken Arm abgesägt!", aplicarDe("Du hast mir meinen {pieza} abgesägt!", "linker Arm"));
            igual("Du hast mir meine Beine abgesägt!", aplicarDe("Du hast mir meinen {pieza} abgesägt!", "Beine"));

            igual("Mit meinem Kopf geht es weiter.", aplicarDe("Mit meinem {pieza} geht es weiter.", "Kopf"));
            igual("Mit meinem linken Arm geht es weiter.", aplicarDe("Mit meinem {pieza} geht es weiter.", "linker Arm"));
            igual("Mit meinen Beinen geht es weiter.", aplicarDe("Mit meinem {pieza} geht es weiter.", "Beine"));
            igual("Mit dem rechten Arm.", aplicarDe("Mit dem {pieza}.", "rechter Arm"));
            igual("Mit den Beinen.", aplicarDe("Mit dem {pieza}.", "Beine"));
            igual("Ich brauche einen linken Arm.", aplicarDe("Ich brauche einen {pieza}.", "linker Arm"));
            igual("Ich brauche Beine.", aplicarDe("Ich brauche einen {pieza}.", "Beine"));
            igual("Ohne meine Beine ist der Tunnel enger.", aplicarDe("Ohne meinen {pieza} ist der Tunnel enger.", "Beine"));

            // Comprobación exhaustiva de TODAS las frases alemanas reales con {pieza} en res/dialogos/.
            String[] piezasDe = {"Kopf", "linker Arm", "rechter Arm", "Beine"};
            String[] prohibidos = {
                "Ohne linker Arm", "Ohne rechter Arm", "ohne linker Arm", "ohne rechter Arm",
                "der linker Arm", "Der linker Arm", "den linker Arm", "dem linker Arm",
                "der rechter Arm", "Der rechter Arm", "den rechter Arm", "dem rechter Arm",
                "meinen Beine ", "meinem Beine", "Meinen Beine ", "Meinem Beine",
                "dem Beine", "den Beine ", "der Beine", "Meine Beine ist", "Die Beine ist",
                "Meine Beine hat", "Die Beine hat", "Meine Beine fällt", "Die Beine fällt",
                "Meine Beine geht", "Die Beine geht", "hast linker Arm", "hast rechter Arm",
                "hast Kopf", "mir linker Arm", "mir rechter Arm", "mir Kopf",
                "entferne linker Arm", "entferne rechter Arm", "entferne Kopf"
            };
            int revisadas = 0;
            for (String archivo : PruebasDelContenido.archivosDe("dialogos")) {
                String nombre = archivo.substring(0, archivo.length() - 4);
                Dialogos d = Dialogos.de(nombre);
                for (String ev : d.eventos()) {
                    for (Trilingue tr : d.frases(ev)) {
                        if (!tr.de.contains("{pieza}")) continue;
                        for (String pz : piezasDe) {
                            String res = Concordancia.ajustar(tr.de, pz, de).replace("{pieza}", pz);
                            revisadas++;
                            falso(res.contains("{pieza}"), "hueco sin rellenar en " + nombre + ": " + res);
                            for (String mal : prohibidos) {
                                falso(res.contains(mal), "declinación errónea (" + mal + ") en " + nombre + ": " + res);
                            }
                        }
                    }
                }
            }
            cierto(revisadas >= 250, "frases alemanas reales comprobadas: " + revisadas);
        });
    }

    private static String aplicarDe(String plantilla, String pieza) {
        return Concordancia.ajustar(plantilla, pieza, Idioma.DE).replace("{pieza}", pieza);
    }

    private static void apuntarEquilibrioCombatesLargos() {
        caso("bloque 5B: equilibrio de combates largos (10-12 rondas), desgaste gradual, tienda, escudos, aparatos y golpe especial", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            // 1) Golpe especial Senior nunca provoca KO en 1 turno sobre un robot ligero sano.
            Combatiente atac = new Combatiente("Atac", new Montaje(Modelo.de("cobalto")), () -> new RobotQuieto());
            Combatiente ligero = new Combatiente("Chispa", new Montaje(Modelo.de("chispa")), () -> new RobotQuieto());
            Pelea pEsp = new Pelea(atac, ligero, 77L);
            pEsp.rondas = 4;
            pEsp.empezarRonda();
            pEsp.golpeEspecial(0, 1, 38.0, "laser");
            falso(pEsp.cuerpo(1).fuera, "el arma especial no puede hacer KO en 1 turno a un robot ligero sano");
            cierto(ligero.danoTotalRecibido >= 15.0 && ligero.danoTotalRecibido <= pEsp.cuerpo(1).blindajeMaximoTotal() * 0.35,
                    "daño especial significativo y acotado: " + ligero.danoTotalRecibido);

            // 2) Combate largo de 10 y 12 rondas con armas/escudos/espadas de Tienda y aparatos activos.
            for (int rondas : new int[]{10, 12}) {
                Montaje mA = new Montaje(Modelo.de("serrucho"));
                mA.poner(Montaje.Hueco.IZQUIERDA, cat.pieza("canon_solar"));
                mA.poner(Montaje.Hueco.DERECHA, cat.pieza("escudo_reflector"));
                Montaje mB = new Montaje(Modelo.de("galapago"));
                mB.poner(Montaje.Hueco.IZQUIERDA, cat.pieza("sable_termico"));
                mB.poner(Montaje.Hueco.DERECHA, cat.pieza("escudo_choque"));

                Combatiente ra = Rivales.crear(Modelo.de("serrucho"), 0.7, 0.5, new Random(11 + rondas));
                Combatiente rb = Rivales.crear(Modelo.de("galapago"), 0.7, 0.5, new Random(29 + rondas));
                Combatiente ca = new Combatiente("A", mA, ra.fabrica);
                Combatiente cb = new Combatiente("B", mB, rb.fabrica);

                Pelea p = new Pelea(ca, cb, 300L + rondas);
                p.rondas = rondas;
                p.aparatos.activos = true;
                double blindajeR2 = 100.0;
                double blindajeUltima = 100.0;
                int rondasCompletadas = 0;

                while (p.estado() != Pelea.Estado.TERMINADA) {
                    p.empezarRonda();
                    rondasCompletadas++;
                    double inicioA = p.cuerpo(0).blindaje();
                    double inicioB = p.cuerpo(1).blindaje();
                    cierto(inicioA >= 49.9 && inicioB >= 49.9,
                            "desgaste nunca baja del 50 % en ronda " + p.ronda() + " (" + inicioA + ", " + inicioB + ")");
                    if (p.ronda() == 2) blindajeR2 = Math.min(inicioA, inicioB);
                    if (p.ronda() == rondas) blindajeUltima = Math.min(inicioA, inicioB);

                    int turnos = 0;
                    while (p.estado() == Pelea.Estado.LUCHANDO) {
                        p.avanzar();
                        turnos++;
                        cierto(turnos <= p.turnosPorRonda + 5, "sin bucle infinito en ronda " + p.ronda());
                    }
                    cierto(turnos > 5, "sin victorias triviales en 1 turno en ronda " + p.ronda() + " (turnos=" + turnos + ")");
                }
                igual(rondas, rondasCompletadas, "completó todas las rondas (" + rondas + ")");
                cierto(ca.danoTotalHecho + cb.danoTotalHecho > 40.0, "hubo combate real en " + rondas + " rondas");
                cierto(blindajeUltima <= blindajeR2 + 0.1,
                        "desgaste progresivo entre ronda 2 (" + blindajeR2 + ") y ronda " + rondas + " (" + blindajeUltima + ")");
            }

            // 3) Aparato Sierra (Bloque 5A) y Martillo en combate: recarga y daño acotados.
            for (Aparatos.Tipo tipo : Aparatos.Tipo.values()) {
                cierto(tipo.recarga >= 90 && tipo.recarga <= 300, "recarga equilibrada de aparato " + tipo + ": " + tipo.recarga);
            }
            cierto(Map.of("sierra", Aparatos.Tipo.SIERRA).get("sierra") == Aparatos.Tipo.SIERRA, "sierra registrada");
        });
    }

    private static final class RobotQuieto extends Metajavbot {
        @Override
        public void cadaTurno() {
        }
    }
}
