package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import metajavbots.api.Cosa;
import metajavbots.combate.Arena;
import metajavbots.combate.CarreraDeObstaculos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.DificultadCarrera;
import metajavbots.combate.Fabrica;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.ConsejosDelJuego;

/** Pruebas automatizadas de monedas peligrosas, rampas, zonas de agua, botín por destrucción y escala de pista. */
final class PruebasDeCarreraDeObstaculos2 {

    private PruebasDeCarreraDeObstaculos2() {
    }

    static void apuntar() {
        apuntarMonedasExtraEnZonasPeligrosas();
        apuntarRampasDeSaltoYTrampasDeSuelo();
        apuntarZonasDeAguaYCortocircuito();
        apuntarBotinDestruccionYCentimosExactos();
        apuntarEscalaPistaPorDificultad();
        apuntarSensoresTrilinguesYEntrenador();
    }

    private static Combatiente combatienteConBala(String nombre, Fabrica fabrica) {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
            if (p.arma() == Pieza.Arma.BALA && p.valor("dano") > 0) {
                m.poner(Montaje.Hueco.IZQUIERDA, p);
                break;
            }
        }
        return new Combatiente(nombre, m, fabrica);
    }

    private static void apuntarMonedasExtraEnZonasPeligrosas() {
        caso("carrera de obstáculos: 1 o 2 monedas extra cerca de peligros suman +1 moneda cada una", () -> {
            for (long s = 10L; s < 20L; s++) {
                Combatiente jug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")),
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
                Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")),
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
                CarreraDeObstaculos c = new CarreraDeObstaculos(jug, riv, s, false, null, DificultadCarrera.NOOB);
                List<Arena.Trampa> monedas = c.monedasExtraActivas();
                cierto(monedas.size() >= 1 && monedas.size() <= 2,
                        "genera entre 1 y 2 monedas extra (semilla " + s + "): " + monedas.size());
                List<Arena.Trampa> mortales = c.trampasMortalesActivas();
                for (Arena.Trampa m : monedas) {
                    double minDist = Double.MAX_VALUE;
                    for (Arena.Trampa pel : mortales) {
                        minDist = Math.min(minDist, Math.hypot(m.x - pel.x, m.y - pel.y));
                    }
                    cierto(minDist >= 60.0 && minDist <= 140.0,
                            "moneda extra colocada cerca de trampa mortal y alcanzable: dist=" + minDist);
                }
            }

            Combatiente jug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(jug, riv, 42L, false, null, DificultadCarrera.JUNIOR);
            List<Arena.Trampa> monedas = carrera.monedasExtraActivas();
            Cuerpo c0 = carrera.pelea.cuerpo(0);
            for (Arena.Trampa m : monedas) {
                c0.x = m.x;
                c0.y = m.y;
                carrera.avanzar();
                cierto(m.destruida(), "la moneda extra desaparece al recogerla");
            }
            igual(monedas.size(), carrera.monedasExtraRecogidas(), "contabiliza todas las monedas extra recogidas");
            c0.x = carrera.metaX() + 5;
            carrera.avanzar();
            Jugador j = new Jugador("CazaMonedas");
            j.monedas = 4.0;
            cierto(carrera.aplicarRecompensa(j), "aplica recompensa con monedas extra");
            igual(4.0 + 1.0 + monedas.size(), j.monedas, "suma victoria (+1) + cada moneda extra (+1 c/u)");
        });
    }

    private static void apuntarRampasDeSaltoYTrampasDeSuelo() {
        caso("carrera de obstáculos: rampas activan salto breve y evitan trampas de suelo en el aire", () -> {
            Combatiente jug = new Combatiente("Saltador", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Recto::new));
            Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(jug, riv, 314L, false, null, DificultadCarrera.JUNIOR);
            List<Arena.Trampa> rampas = carrera.rampasActivas();
            List<Arena.Trampa> mortales = carrera.trampasMortalesActivas();
            cierto(rampas.size() >= 3, "hay rampas de salto repartidas por la pista");

            Arena.Trampa rampa = rampas.get(0);
            Arena.Trampa mortal = mortales.get(0);
            Cuerpo c0 = carrera.pelea.cuerpo(0);
            c0.x = rampa.x;
            c0.y = rampa.y;
            c0.rumbo = 0;
            double xAntes = c0.x;
            carrera.avanzar();
            cierto(c0.saltando > 0 && c0.vuela(), "al pasar por la rampa entra en estado de salto/vuelo breve");

            // Mientras está en el aire, pasar sobre una trampa mortal de suelo NO lo destruye.
            c0.x = mortal.x;
            c0.y = mortal.y;
            carrera.avanzar();
            falso(c0.fuera, "mientras salta en el aire evita la trampa mortal de suelo");
            cierto(c0.x > mortal.x, "conserva el avance hacia delante durante el salto");

            for (int t = 0; t < Arena.TURNOS_SALTO + 2; t++) {
                c0.x = xAntes + 120 + t * 4;
                c0.y = 85;
                carrera.avanzar();
            }
            igual(0, c0.saltando, "aterriza suavemente al terminar los turnos de salto");
            falso(c0.vuela(), "vuelve al suelo tras aterrizar");
        });
    }

    private static void apuntarZonasDeAguaYCortocircuito() {
        caso("carrera de obstáculos: zonas de agua causan cortocircuito temporal, desvío de rumbo y recuperación", () -> {
            Combatiente jug = new Combatiente("PilotoAgua", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(jug, riv, 512L, false, null, DificultadCarrera.JUNIOR);
            List<Arena.Trampa> aguas = carrera.zonasAguaActivas();
            cierto(aguas.size() >= 3, "hay zonas de caída de agua en la pista");

            Arena.Trampa agua = aguas.get(0);
            Cuerpo c0 = carrera.pelea.cuerpo(0);
            c0.x = agua.x;
            c0.y = agua.y;
            c0.rumbo = 0;
            carrera.avanzar();
            cierto(c0.cortocircuito > 0, "al tocar una zona de agua activa cortocircuito eléctrico");
            falso(c0.fuera, "el cortocircuito no destruye al robot");

            // Lo alejamos de la zona de agua y comprobamos alteración de rumbo durante el efecto.
            c0.x = 180;
            c0.y = 120;
            double maxDesvio = 0;
            for (int t = 0; t < Arena.TURNOS_CORTOCIRCUITO; t++) {
                carrera.avanzar();
                maxDesvio = Math.max(maxDesvio, Math.abs(c0.rumbo));
            }
            cierto(maxDesvio > 1.0, "el cortocircuito provoca alteración perceptible del rumbo: " + maxDesvio);
            igual(0, c0.cortocircuito, "el cortocircuito termina tras su duración breve");

            // Tras recuperarse, el rumbo ya no oscila solo.
            c0.rumbo = 15.0;
            carrera.avanzar();
            igual(15.0, c0.rumbo, "recupera el control normal al terminar el cortocircuito");
        });
    }

    private static void apuntarBotinDestruccionYCentimosExactos() {
        caso("carrera de obstáculos: botín por destrucción (+0,2 / +0,4 / +1,0), 1/4 y migración en Guardado", () -> {
            Path carpetaAnterior = Guardado.carpeta;
            Path temp = Files.createTempDirectory("mjb-carrera-botin");
            try {
                Guardado.carpeta = temp;
                Combatiente tirador = combatienteConBala("Tirador",
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Tirador::new));
                Combatiente riv = new Combatiente("Rival", new Montaje(Modelo.de("cobalto")),
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
                CarreraDeObstaculos c = new CarreraDeObstaculos(tirador, riv, 777L, false, null, DificultadCarrera.SENIOR);
                Cuerpo c0 = c.pelea.cuerpo(0);
                Cuerpo c1 = c.pelea.cuerpo(1);
                c1.x = 240;
                c1.y = 660;

                // 1) Destruir 5 objetos destructibles (+0,2 x 5 = +1,0 moneda exacta).
                int rotos = 0;
                for (Arena.Trampa t : new java.util.ArrayList<>(c.pelea.arena.trampas())) {
                    if (rotos >= 5) break;
                    if (!t.destructible || t.destruida()) continue;
                    for (int k = 0; k < 200 && !t.destruida() && !c.terminada(); k++) {
                        c0.x = t.x - 110;
                        c0.y = t.y;
                        c0.mira = 0;
                        c.avanzar();
                    }
                    if (t.destruida()) rotos++;
                }
                igual(5, c.objetosDestruidos(), "registra 5 obstáculos destructibles destruidos por el jugador");

                // 2) Destruir 1 robot defectuoso (+0,4 = 40 centésimas).
                Cuerpo roto = c.robotsDefectuosos().get(0);
                for (int k = 0; k < 400 && !roto.fuera && !c.terminada(); k++) {
                    c0.x = roto.x - 85;
                    c0.y = roto.y;
                    c0.mira = 0;
                    c.avanzar();
                }
                cierto(roto.fuera, "destruye al robot defectuoso");
                cierto(c.defectuososDestruidos() >= 1, "registra el robot defectuoso destruido");
                int defDest = c.defectuososDestruidos();

                // 3) Destruir al rival principal antes de terminar (+1,0 moneda entera).
                for (int k = 0; k < 400 && !c1.fuera && !c.terminada(); k++) {
                    c0.x = c1.x - 95;
                    c0.y = c1.y;
                    c0.mira = 0;
                    c.avanzar();
                }
                cierto(c1.fuera && c.terminada() && c.rivalDestruido(), "destruye al rival principal y lo registra");

                Jugador jug = new Jugador("Cazador");
                jug.monedas = 1.0;
                jug.centimosDeMoneda = 0;
                cierto(c.aplicarRecompensa(jug), "aplica recompensas de destrucción y victoria");
                int perf = c.medallaObtenida().esPerfecta ? CarreraDeObstaculos.CENTIMOS_PERFECCION : 0;
                int esperadoCentimos = 100 + perf + 5 * 20 + defDest * 40 + 100;
                igual(esperadoCentimos, c.centimosGanadosTotal(), "cálculo exacto en centésimas sin coma flotante");
                igual(1.0 + (esperadoCentimos / 100), jug.monedas, "monedas enteras acumuladas con exactitud");
                igual(esperadoCentimos % 100, jug.centimosDeMoneda, "resto exacto en centimosDeMoneda");

                Guardado.guardar(jug);
                Jugador rec = Guardado.leer(Guardado.carpetaDe(jug.nombre));
                igual(jug.monedas, rec.monedas, "monedas persisten en Guardado");
                igual(jug.centimosDeMoneda, rec.centimosDeMoneda, "centimosDeMoneda persiste exactamente en Guardado");

                // 4) Compatibilidad de migración desde partida antigua con solo cuartosDeMoneda=3 -> 75 centésimas.
                Path dirAntiguo = temp.resolve("JugadorAntiguo");
                Files.createDirectories(dirAntiguo);
                Files.writeString(dirAntiguo.resolve("partida.txt"),
                        "nombre = JugadorAntiguo\nmonedas = 2.00\ncuartosDeMoneda = 3\n", StandardCharsets.UTF_8);
                Jugador migrado = Guardado.leer("JugadorAntiguo");
                igual(75, migrado.centimosDeMoneda, "migra cuartosDeMoneda=3 a 75 centésimas");
                igual(3, migrado.cuartosDeMoneda, "mantiene cuartosDeMoneda sincronizado");
                igual(1, migrado.sumarCentimosDeMoneda(25), "75 + 25 centésimas completa +1 moneda entera");
                igual(3.0, migrado.monedas, "2 + 1 = 3 monedas tras completar 100 centésimas");
                igual(0, migrado.centimosDeMoneda, "centimosDeMoneda reinicia a 0");
            } finally {
                Guardado.carpeta = carpetaAnterior;
            }
        });
    }

    private static void apuntarEscalaPistaPorDificultad() {
        caso("carrera de obstáculos: pista crece ×4/3 por nivel (Noob -> Junior -> Senior -> Especial) y turnos proporcionales", () -> {
            double lNoob = CarreraDeObstaculos.anchoPara(DificultadCarrera.NOOB);
            double lJunior = CarreraDeObstaculos.anchoPara(DificultadCarrera.JUNIOR);
            double lSenior = CarreraDeObstaculos.anchoPara(DificultadCarrera.SENIOR);
            double lEspecial = CarreraDeObstaculos.anchoPara(DificultadCarrera.ESPECIAL);

            igual(CarreraDeObstaculos.ANCHO, lNoob, "Noob conserva el tamaño base 8400");
            cierto(Math.abs(lJunior - lNoob * 4.0 / 3.0) <= 1.0, "Junior mide 4/3 de Noob: " + lJunior);
            cierto(Math.abs(lSenior - lJunior * 4.0 / 3.0) <= 1.0, "Senior mide 4/3 de Junior: " + lSenior);
            cierto(Math.abs(lEspecial - lSenior * 4.0 / 3.0) <= 1.0, "Especial mide 4/3 de Senior: " + lEspecial);

            int tNoob = CarreraDeObstaculos.maxTurnosPara(DificultadCarrera.NOOB);
            int tJunior = CarreraDeObstaculos.maxTurnosPara(DificultadCarrera.JUNIOR);
            int tSenior = CarreraDeObstaculos.maxTurnosPara(DificultadCarrera.SENIOR);
            int tEspecial = CarreraDeObstaculos.maxTurnosPara(DificultadCarrera.ESPECIAL);
            igual(CarreraDeObstaculos.MAX_TURNOS, tNoob, "Noob conserva límite base de turnos");
            cierto(Math.abs(tJunior - tNoob * 4.0 / 3.0) <= 1.0, "turnos Junior escalan ×4/3");
            cierto(Math.abs(tSenior - tJunior * 4.0 / 3.0) <= 1.0, "turnos Senior escalan ×4/3");
            cierto(Math.abs(tEspecial - tSenior * 4.0 / 3.0) <= 1.0, "turnos Especial escalan ×4/3");

            Combatiente j = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            for (DificultadCarrera d : DificultadCarrera.values()) {
                CarreraDeObstaculos c = new CarreraDeObstaculos(j, CarreraDeObstaculos.rivalMaquina("IA", d), 600L, true, null, d);
                igual(CarreraDeObstaculos.anchoPara(d), c.pelea.ancho, "Pelea usa ancho escalado en " + d);
                igual(CarreraDeObstaculos.maxTurnosPara(d), c.pelea.turnosPorRonda, "Pelea usa turnos escalados en " + d);
            }
        });
    }

    private static void apuntarSensoresTrilinguesYEntrenador() {
        caso("carrera de obstáculos: moneda, rampa y agua detectables ES/DE/EN, ui.txt y entrenador trilingüe", () -> {
            Combatiente jug = new Combatiente("Jug", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos c = new CarreraDeObstaculos(jug, riv, 808L, false, null, DificultadCarrera.JUNIOR);
            boolean vioMoneda = false, vioRampa = false, vioAgua = false;
            for (Cosa cosa : c.pelea.cuerpo(0).obstaculos()) {
                if (cosa.tipo().equals(Arena.TIPO_MONEDA_EXTRA)
                        && cosa.typ().equals(Arena.TIPO_MONEDA_EXTRA)
                        && cosa.type().equals(Arena.TIPO_MONEDA_EXTRA)
                        && !cosa.peligroso() && !cosa.gefaehrlich() && !cosa.dangerous()) {
                    vioMoneda = true;
                }
                if (cosa.tipo().equals(Arena.TIPO_RAMPA)
                        && cosa.typ().equals(Arena.TIPO_RAMPA)
                        && cosa.type().equals(Arena.TIPO_RAMPA)
                        && !cosa.peligroso() && !cosa.gefaehrlich() && !cosa.dangerous()) {
                    vioRampa = true;
                }
                if (cosa.tipo().equals(Arena.TIPO_AGUA)
                        && cosa.typ().equals(Arena.TIPO_AGUA)
                        && cosa.type().equals(Arena.TIPO_AGUA)
                        && cosa.peligroso() && cosa.gefaehrlich() && cosa.dangerous()) {
                    vioAgua = true;
                }
            }
            cierto(vioMoneda, "moneda extra detectable en ES/DE/EN");
            cierto(vioRampa, "rampa de salto detectable en ES/DE/EN");
            cierto(vioAgua, "zona de agua detectable como peligrosa en ES/DE/EN");

            String[] clavesNuevas = {
                    "carrera_obs.leyenda_moneda", "carrera_obs.leyenda_rampa", "carrera_obs.leyenda_agua",
                    "carrera_obs.botin_total", "carrera_obs.desglose_bajas", "carrera_obs.desglose_extras"
            };
            for (Idioma id : Idioma.values()) {
                for (String k : clavesNuevas) {
                    String val = T.en(k, id);
                    cierto(val != null && !val.isBlank() && !val.equals(k), "clave nueva ui.txt en " + id + ": " + k);
                }
                ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("carrera_de_obstaculos");
                Concepto concepto = entrada.aConcepto();
                String texto = String.join(" ", concepto.parrafos(id)).toLowerCase(Locale.ROOT);
                cierto(texto.contains("moneda") && texto.contains("rampa") && texto.contains("agua")
                        && texto.contains("4/3"),
                        "ficha entrenador incluye moneda, rampa, agua y escala 4/3 en " + id);
                String tip = ConsejosDelJuego.textoDelTip(entrada.consejos().get(0).aTip(), id);
                cierto(tip.contains("moneda") && tip.contains("rampa") && tip.contains("agua"),
                        "tip espontáneo incluye moneda, rampa y agua en " + id);
                String ia = ConsultaIaEntrenador.construirSistema(new Jugador("T"), id, null).toLowerCase(Locale.ROOT);
                cierto(ia.contains("moneda") && ia.contains("rampa") && ia.contains("agua")
                        && ia.contains("+0.2") && ia.contains("+0.4") && ia.contains("4/3"),
                        "sistema IA actualizado con nuevas mecánicas en " + id);
            }
        });
    }
}
