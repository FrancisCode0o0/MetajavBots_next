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
import metajavbots.pantallas.Menu;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Etiqueta;
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
        apuntarReglaDerrotaUnCuarto();
        apuntarEscaladoRecompensasYSubniveles();
        apuntarSelectorPiezasEnMenuYAccesoPrincipal();
        apuntarModelosDesbloqueadosYRendirse();
        apuntarDanoIncrementadoYParidadTrilingue();
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
            int esperado = CarreraDeObstaculos.escalarCentimos(100 + monedas.size() * 100, carrera.multiplicadorRecompensa())
                    + CarreraDeObstaculos.CENTIMOS_PERFECCION;
            igual(esperado, carrera.centimosGanadosTotal(), "suma victoria + monedas extra escaladas por dificultad + perfección");
            igual(4.0 + (esperado / 100), j.monedas, "actualiza monedas enteras con exactitud");
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

                // 2) Destruir 1 robot defectuoso activo (+0,4 = 40 centésimas).
                Cuerpo roto = c.robotsDefectuosos().stream().filter(d -> !d.fuera).findFirst().orElse(null);
                cierto(roto != null, "queda al menos un robot defectuoso activo en pista");
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
                double mult = c.multiplicadorRecompensa();
                int bonosBase = 5 * 20 + defDest * 40 + 100 + c.monedasExtraRecogidas() * 100;
                int esperadoCentimos = CarreraDeObstaculos.escalarCentimos(100, mult) + perf
                        + CarreraDeObstaculos.escalarCentimos(bonosBase, mult);
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

    private static void apuntarReglaDerrotaUnCuarto() {
        caso("carrera de obstáculos: derrota sin bonos = 0 y derrota con bonos = exactamente 1/4", () -> {
            // 1) Derrota sin destruir ni recoger nada -> exactamente 0 centésimas.
            Combatiente jug0 = new Combatiente("Jug0", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos derrotaLimpia = new CarreraDeObstaculos(jug0, CarreraDeObstaculos.rivalMaquina("IA", DificultadCarrera.NOOB),
                    901L, true, null, DificultadCarrera.NOOB, 1);
            derrotaLimpia.pelea.cuerpo(1).x = derrotaLimpia.metaX() + 5;
            derrotaLimpia.avanzar();
            Jugador j0 = new Jugador("SinBonos");
            j0.monedas = 3.0;
            falso(derrotaLimpia.aplicarRecompensa(j0), "derrota sin bonos no suma nada");
            igual(0, derrotaLimpia.centimosGanadosTotal(), "derrota sin bonos suma exactamente 0 centésimas");
            igual(3.0, j0.monedas, "saldo intacto en derrota sin bonos");
            igual(0, j0.centimosDeMoneda, "centimosDeMoneda intacto en derrota sin bonos");

            // 2) Derrota tras destruir 1 objeto destructible (+20 base en NOOB I) -> 1/4 = exactamente 5 centésimas.
            Combatiente tirObj = combatienteConBala("TirObj", PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Tirador::new));
            Combatiente rivObj = new Combatiente("RivObj", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos derrotaObj = new CarreraDeObstaculos(tirObj, rivObj, 902L, false, null, DificultadCarrera.NOOB, 1);
            Arena.Trampa barril = null;
            for (Arena.Trampa t : derrotaObj.pelea.arena.trampas()) {
                if (t.destructible && !t.destruida()) {
                    barril = t;
                    break;
                }
            }
            cierto(barril != null, "existe un obstáculo destructible");
            Cuerpo cObj = derrotaObj.pelea.cuerpo(0);
            for (int k = 0; k < 200 && !barril.destruida() && !derrotaObj.terminada(); k++) {
                cObj.x = barril.x - 110;
                cObj.y = barril.y;
                cObj.mira = 0;
                derrotaObj.avanzar();
            }
            igual(1, derrotaObj.objetosDestruidos(), "destruyó 1 objeto");
            derrotaObj.pelea.cuerpo(1).x = derrotaObj.metaX() + 5;
            derrotaObj.avanzar();
            igual(1, derrotaObj.ganador(), "pierde la carrera");
            Jugador jObj = new Jugador("DerrotaObj");
            cierto(derrotaObj.aplicarRecompensa(jObj), "aplica 1/4 de bono en derrota con objeto destruido");
            cierto(derrotaObj.bonosReducidosPorDerrota(), "marca bonos reducidos a 1/4 por derrota");
            igual(5, derrotaObj.centimosGanadosTotal(), "20 / 4 = exactamente 5 centésimas");
            igual(5, jObj.centimosDeMoneda, "jugador recibe exactamente 5 centésimas");

            // 3) Derrota tras destruir 1 robot defectuoso (+40 base en NOOB I) -> 1/4 = exactamente 10 centésimas.
            Combatiente tirDef = combatienteConBala("TirDef", PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Tirador::new));
            Combatiente rivDef = new Combatiente("RivDef", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos derrotaDef = new CarreraDeObstaculos(tirDef, rivDef, 903L, false, null, DificultadCarrera.NOOB, 1);
            Cuerpo roto = derrotaDef.robotsDefectuosos().get(0);
            Cuerpo cDef = derrotaDef.pelea.cuerpo(0);
            for (int k = 0; k < 400 && !roto.fuera && !derrotaDef.terminada(); k++) {
                cDef.x = roto.x - 85;
                cDef.y = roto.y;
                cDef.mira = 0;
                derrotaDef.avanzar();
            }
            igual(1, derrotaDef.defectuososDestruidos(), "destruyó 1 robot defectuoso");
            derrotaDef.pelea.cuerpo(1).x = derrotaDef.metaX() + 5;
            derrotaDef.avanzar();
            Jugador jDef = new Jugador("DerrotaDef");
            cierto(derrotaDef.aplicarRecompensa(jDef), "aplica 1/4 de bono en derrota con defectuoso destruido");
            igual(10, derrotaDef.centimosGanadosTotal(), "40 / 4 = exactamente 10 centésimas");
            igual(10, jDef.centimosDeMoneda, "jugador recibe exactamente 10 centésimas");
        });
    }

    private static void apuntarEscaladoRecompensasYSubniveles() {
        caso("carrera de obstáculos: recompensas crecen Noob < Junior < Senior < Especial y subniveles I < II < III", () -> {
            DificultadCarrera[] niveles = DificultadCarrera.values();
            int prevWin = 0;
            for (DificultadCarrera d : niveles) {
                Combatiente cj = new Combatiente("Gan", new Montaje(Modelo.de("cobalto")),
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
                CarreraDeObstaculos c1 = new CarreraDeObstaculos(cj, CarreraDeObstaculos.rivalMaquina("IA", d, 1), 910L, true, null, d, 1);
                c1.pelea.cuerpo(0).x = c1.metaX() + 5;
                c1.avanzar();
                Jugador jt = new Jugador("Esc-" + d);
                c1.aplicarRecompensa(jt);
                cierto(c1.centimosGanadosTotal() > prevWin, "recompensa crece en " + d + ": " + c1.centimosGanadosTotal() + " > " + prevWin);
                prevWin = c1.centimosGanadosTotal();
            }
            int prevSubCent = 0, prevRep = 0, prevDef = 0;
            for (int sub = 1; sub <= 3; sub++) {
                Combatiente cj = new Combatiente("Sub", new Montaje(Modelo.de("cobalto")),
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
                CarreraDeObstaculos cs = new CarreraDeObstaculos(cj, CarreraDeObstaculos.rivalMaquina("IA", DificultadCarrera.NOOB, sub),
                        920L, true, null, DificultadCarrera.NOOB, sub);
                cierto(cs.obstaculosRepentinos().size() > prevRep, "repentinos crecen en subnivel " + sub);
                cierto(cs.robotsDefectuosos().size() > prevDef, "defectuosos crecen en subnivel " + sub);
                prevRep = cs.obstaculosRepentinos().size();
                prevDef = cs.robotsDefectuosos().size();
                cs.pelea.cuerpo(0).x = cs.metaX() + 5;
                cs.avanzar();
                Jugador js = new Jugador("Sub-" + sub);
                cs.aplicarRecompensa(js);
                cierto(cs.centimosGanadosTotal() > prevSubCent, "recompensa crece en subnivel " + sub + ": " + cs.centimosGanadosTotal());
                prevSubCent = cs.centimosGanadosTotal();
            }
        });
    }

    private static void apuntarSelectorPiezasEnMenuYAccesoPrincipal() {
        caso("carrera de obstáculos: cambio de robot y piezas en menú, acceso desde Menu.java y textos/entrenador trilingües", () -> {
            // 1) Acceso directo desde el bloque Arena de Menu.java.
            cierto(Menu.clavesDeCasillas().contains("menu.carrera"), "Menu principal incluye tarjeta directa menu.carrera");

            // 2) Cambio de robot y de piezas disponibles directamente en PantallaCarreraDeObstaculos sin pasar por Taller.
            Jugador jug = new Jugador("Mecanico");
            jug.etiqueta = Etiqueta.SENIOR_1;
            Jugador.Robot r0 = new Jugador.Robot("Primero", "cobalto");
            Jugador.Robot r1 = new Jugador.Robot("Segundo", Modelo.todos().size() > 1 ? Modelo.todos().get(1).id : "cobalto");
            jug.robots.add(r0);
            jug.robots.add(r1);
            List<Pieza> piernas = CatalogoDePiezas.delJuego().de(Pieza.Ranura.PIERNAS);
            Pieza alternativa = null;
            Pieza inicial = jug.montajeDe(r1).pieza(Montaje.Hueco.PIERNAS);
            for (Pieza p : piernas) {
                if (inicial == null || !p.id.equals(inicial.id)) {
                    alternativa = p;
                    jug.piezasGanadas.add(p.id);
                    break;
                }
            }
            cierto(alternativa != null, "existe una pieza de piernas alternativa en catálogo");

            PantallaCarreraDeObstaculos pantalla = new PantallaCarreraDeObstaculos();
            cierto(pantalla.seleccionarRobot(jug, 1), "permite seleccionar el segundo robot");
            igual(1, pantalla.robotSeleccionado(), "robotSeleccionado actualizado a 1");
            igual(1, jug.robotElegido, "jugador.robotElegido sincronizado");
            cierto(pantalla.equiparPieza(jug, Montaje.Hueco.PIERNAS, alternativa.id), "permite equipar pieza disponible desde el menú de carrera");
            igual(alternativa.id, r1.piezas.get(Montaje.Hueco.PIERNAS), "la pieza queda montada en el robot sin ir al Taller");

            Montaje.Cifras cifrasMenu = pantalla.cifrasActuales(jug);
            Combatiente corredor = CarreraDeObstaculos.combatienteDeJugador(jug, pantalla.robotSeleccionado(),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            igual(alternativa.id, corredor.montaje.pieza(Montaje.Hueco.PIERNAS).id, "el corredor sale con la pieza cambiada en el menú");
            igual(cifrasMenu.velocidad, corredor.montaje.cifras().velocidad, "las cifras del menú coinciden con las del corredor");

            // 3) Claves nuevas en ui.txt y conocimiento actualizado del entrenador (ES, DE, EN).
            String[] claves = {
                    "menu.carrera", "menu.carrera_ayuda", "carrera_obs.subnivel", "carrera_obs.multiplicador",
                    "carrera_obs.dif_bloqueada_rango", "carrera_obs.solo_un_robot", "carrera_obs.derrota_cuarto"
            };
            for (Idioma id : Idioma.values()) {
                for (String k : claves) {
                    String val = T.en(k, id);
                    cierto(val != null && !val.isBlank() && !val.equals(k), "clave ui.txt presente en " + id + ": " + k);
                }
                ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("carrera_de_obstaculos");
                String texto = String.join(" ", entrada.aConcepto().parrafos(id)).toLowerCase(Locale.ROOT);
                cierto(texto.contains("arena") && texto.contains("1/4") && texto.contains("iii"),
                        "ficha entrenador refleja acceso Arena, subniveles I/II/III y regla 1/4 en " + id);
                String ia = ConsultaIaEntrenador.construirSistema(jug, id, null).toLowerCase(Locale.ROOT);
                cierto(ia.contains("arena") && ia.contains("sublevel") && ia.contains("1/4"),
                        "sistema IA refleja acceso Arena, subniveles y regla 1/4 en " + id);
            }
        });
    }

    private static void apuntarModelosDesbloqueadosYRendirse() {
        caso("carrera de obstáculos: selector de modelos desbloqueados en Noob I y Rendirse con 0 premios", () -> {
            // 1) Jugador en Noob I con 3 modelos desbloqueados puede cambiar entre los 3 desde el menú y la carrera usa el elegido.
            Jugador jug = new Jugador("NovatoModelos");
            jug.etiqueta = Etiqueta.NOOB_1;
            List<Modelo> todos = Modelo.todos();
            cierto(todos.size() >= 3, "el catálogo tiene al menos 3 modelos");
            String m0 = todos.get(0).id, m1 = todos.get(1).id, m2 = todos.get(2).id;
            jug.modelosGanados.add(m0);
            jug.modelosGanados.add(m1);
            jug.modelosGanados.add(m2);
            jug.robots.add(new Jugador.Robot("MiBot", m0));

            List<Modelo> desbloqueados = PantallaCarreraDeObstaculos.modelosDesbloqueados(jug);
            cierto(desbloqueados.size() >= 3, "Noob I con 3 modelos desbloqueados disponibles: " + desbloqueados.size());
            PantallaCarreraDeObstaculos pantalla = new PantallaCarreraDeObstaculos();
            pantalla.seleccionarRobot(jug, 0);
            for (String objetivo : new String[]{m1, m2, m0}) {
                cierto(pantalla.seleccionarModelo(jug, objetivo), "permite elegir modelo desbloqueado " + objetivo);
                igual(objetivo, pantalla.modeloSeleccionado(jug), "modelo seleccionado actualizado a " + objetivo);
                Combatiente corr = CarreraDeObstaculos.combatienteDeJugador(jug, pantalla.robotSeleccionado(),
                        PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
                igual(objetivo, corr.montaje.modelo.id, "la carrera usa el modelo elegido: " + objetivo);
                igual(pantalla.cifrasActuales(jug).velocidad, corr.montaje.cifras().velocidad,
                        "cifras del menú coinciden con el modelo " + objetivo);
            }
            cierto(pantalla.cambiarModelo(jug, 1), "rotación +1 entre modelos desbloqueados funciona");

            // 2) Al pulsar Rendirse tras destruir un objeto, termina al instante con 0 monedas y 0 centésimas.
            Combatiente tir = combatienteConBala("Rendidor", PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Tirador::new));
            Combatiente riv = new Combatiente("Riv", new Montaje(Modelo.de("cobalto")),
                    PruebasDeLaPelea.de(PruebasDeCarreraDeObstaculos.Quieto::new));
            CarreraDeObstaculos cRend = new CarreraDeObstaculos(tir, riv, 950L, false, null, DificultadCarrera.NOOB, 1);
            Arena.Trampa barril = null;
            for (Arena.Trampa t : cRend.pelea.arena.trampas()) {
                if (t.destructible && !t.destruida()) {
                    barril = t;
                    break;
                }
            }
            cierto(barril != null, "hay un objeto destructible en pista");
            Cuerpo c0 = cRend.pelea.cuerpo(0);
            for (int k = 0; k < 200 && !barril.destruida() && !cRend.terminada(); k++) {
                c0.x = barril.x - 110;
                c0.y = barril.y;
                c0.mira = 0;
                cRend.avanzar();
            }
            cierto(cRend.objetosDestruidos() >= 1, "destruyó al menos un objeto antes de rendirse");
            cRend.rendirse();
            cierto(cRend.terminada() && cRend.rendido(), "rendirse termina la carrera inmediatamente");
            igual("rendicion", cRend.motivoFin(), "motivoFin registra rendición");
            igual(1, cRend.ganador(), "al rendirse el jugador no gana");
            Jugador jRend = new Jugador("SinPremio");
            jRend.monedas = 7.0;
            jRend.centimosDeMoneda = 15;
            falso(cRend.aplicarRecompensa(jRend), "rendirse no otorga ninguna recompensa");
            igual(0, cRend.centimosGanadosTotal(), "rendirse suma exactamente 0 centésimas");
            igual(0, cRend.monedasEnterasGanadas(), "rendirse suma exactamente 0 monedas");
            igual(7.0, jRend.monedas, "saldo de monedas intacto tras rendirse");
            igual(15, jRend.centimosDeMoneda, "centésimas intactas tras rendirse");
        });
    }

    private static void apuntarDanoIncrementadoYParidadTrilingue() {
        caso("carrera de obstáculos: multiplicador de daño +20-25% y paridad trilingüe de ui.txt y entrenador", () -> {
            cierto(DificultadCarrera.NOOB.multiplicadorDano >= 2.6, "NOOB multDano incrementado: " + DificultadCarrera.NOOB.multiplicadorDano);
            cierto(DificultadCarrera.JUNIOR.multiplicadorDano >= 3.3, "JUNIOR multDano incrementado: " + DificultadCarrera.JUNIOR.multiplicadorDano);
            cierto(DificultadCarrera.SENIOR.multiplicadorDano >= 4.2, "SENIOR multDano incrementado: " + DificultadCarrera.SENIOR.multiplicadorDano);
            cierto(DificultadCarrera.ESPECIAL.multiplicadorDano >= 5.2, "ESPECIAL multDano incrementado: " + DificultadCarrera.ESPECIAL.multiplicadorDano);
            DificultadCarrera[] niveles = DificultadCarrera.values();
            for (int i = 0; i < niveles.length - 1; i++) {
                cierto(niveles[i].multiplicadorDano < niveles[i + 1].multiplicadorDano,
                        "multiplicador estrictamente creciente: " + niveles[i] + " < " + niveles[i + 1]);
            }

            String[] claves = {
                    "carrera_obs.modelo", "carrera_obs.rendirse", "carrera_obs.rendido_titulo", "carrera_obs.rendido_sin_premio"
            };
            igual("Rendirse", T.en("carrera_obs.rendirse", Idioma.ES), "ES Rendirse");
            igual("Aufgeben", T.en("carrera_obs.rendirse", Idioma.DE), "DE Aufgeben");
            igual("Surrender", T.en("carrera_obs.rendirse", Idioma.EN), "EN Surrender");
            Jugador jug = new Jugador("Trilingue");
            for (Idioma id : Idioma.values()) {
                for (String k : claves) {
                    String val = T.en(k, id);
                    cierto(val != null && !val.isBlank() && !val.equals(k), "clave ui.txt presente en " + id + ": " + k);
                }
                ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("carrera_de_obstaculos");
                String texto = String.join(" ", entrada.aConcepto().parrafos(id)).toLowerCase(Locale.ROOT);
                String palRend = id == Idioma.DE ? "aufgeben" : id == Idioma.EN ? "surrender" : "rendirse";
                String palMod = id == Idioma.DE ? "vorschau" : id == Idioma.EN ? "preview" : "previsualizar";
                cierto(texto.contains(palRend) && texto.contains(palMod),
                        "ficha entrenador menciona rendición y previsualización de modelos en " + id);
                String ia = ConsultaIaEntrenador.construirSistema(jug, id, null).toLowerCase(Locale.ROOT);
                cierto(ia.contains("surrender") && ia.contains("preview") && ia.contains("unlocked robot models"),
                        "sistema IA actualizado con modelos desbloqueados, previsualización y Surrender en " + id);
            }
        });
    }
}
