package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import metajavbots.api.Metajavbot;
import metajavbots.combate.Arena;
import metajavbots.combate.Armas;
import metajavbots.combate.CarreraDeObstaculos;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Naturaleza;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.ConsejosDelJuego;

/**
 * Pruebas deterministas de equilibrio por compensación entre modelos, sinergia controlada
 * de armas características con libertad de montaje, fórmulas unificadas de daño y
 * destrucción de obstáculos en combate y Carrera de obstáculos, y base del entrenador.
 */
final class PruebasDeUnificacionDeRobots {

    private PruebasDeUnificacionDeRobots() {
    }

    /** Robot inmóvil para pruebas controladas de daño y paridad. */
    public static class Quieto extends Metajavbot {
        @Override
        public void cadaTurno() {
            // Quieto.
        }
    }

    /** Robot que dispara únicamente con el brazo izquierdo en cada turno. */
    public static class TiradorIzquierdo extends Metajavbot {
        @Override
        public void cadaTurno() {
            atacarIzquierda();
        }
    }

    static void apuntar() {
        apuntarEquilibrioDeModelos();
        apuntarArmasCaracteristicasYLibertad();
        apuntarDiferenciacionYObstaculos();
        apuntarParidadEntreModos();
        apuntarConocimientoDelEntrenador();
    }

    private static void apuntarEquilibrioDeModelos() {
        caso("unificación: equilibrio por compensación entre todos los modelos sin dominancia estricta", () -> {
            List<Modelo> modelos = Modelo.todos();
            cierto(modelos.size() >= 16, "catálogo completo de modelos: " + modelos.size());
            List<Montaje.Cifras> cifras = new ArrayList<>();
            for (Modelo m : modelos) {
                Montaje.Cifras c = new Montaje(m).cifras();
                cierto(c.velocidad > 0 && c.blindajeTotal() > 0 && c.cuerpo > 0 && c.distancia > 0, m.id + " cifras positivas");
                cifras.add(c);
            }
            // Ningún modelo supera estrictamente a otro simultáneamente en las 4 métricas principales.
            for (int i = 0; i < modelos.size(); i++) {
                Montaje.Cifras a = cifras.get(i);
                for (int j = 0; j < modelos.size(); j++) {
                    if (i == j) continue;
                    Montaje.Cifras b = cifras.get(j);
                    boolean domina = a.velocidad > b.velocidad + 1e-6
                            && a.blindajeTotal() > b.blindajeTotal() + 1e-6
                            && a.cuerpo > b.cuerpo + 1e-6
                            && a.distancia > b.distancia + 1e-6;
                    falso(domina, modelos.get(i).id + " no debe dominar en todo a " + modelos.get(j).id);
                }
            }
            // Los modelos más rápidos tienen claramente menos blindaje total que los más blindados.
            List<Modelo> porVel = new ArrayList<>(modelos);
            porVel.sort(Comparator.comparingDouble((Modelo m) -> new Montaje(m).cifras().velocidad).reversed());
            List<Modelo> porBlind = new ArrayList<>(modelos);
            porBlind.sort(Comparator.comparingDouble((Modelo m) -> new Montaje(m).cifras().blindajeTotal()).reversed());
            double blindajeRapidos = 0, blindajeTanques = 0;
            for (int k = 0; k < 3; k++) {
                blindajeRapidos += new Montaje(porVel.get(k)).cifras().blindajeTotal();
                blindajeTanques += new Montaje(porBlind.get(k)).cifras().blindajeTotal();
            }
            cierto(blindajeRapidos < blindajeTanques * 0.72,
                    "los 3 más rápidos tienen mucho menos blindaje que los 3 más blindados: " + blindajeRapidos + " vs " + blindajeTanques);

            // FURIA destaca en cuerpo a cuerpo y cede en distancia; FOCO destaca en distancia y cede en cuerpo a cuerpo.
            double maxFocoCuerpo = 0, minFuriaCuerpo = Double.MAX_VALUE;
            double maxFuriaDist = 0, minFocoDist = Double.MAX_VALUE;
            for (int i = 0; i < modelos.size(); i++) {
                Naturaleza nat = modelos.get(i).naturaleza;
                Montaje.Cifras c = cifras.get(i);
                if (nat == Naturaleza.FURIA) {
                    minFuriaCuerpo = Math.min(minFuriaCuerpo, c.cuerpo);
                    maxFuriaDist = Math.max(maxFuriaDist, c.distancia);
                } else if (nat == Naturaleza.FOCO) {
                    maxFocoCuerpo = Math.max(maxFocoCuerpo, c.cuerpo);
                    minFocoDist = Math.min(minFocoDist, c.distancia);
                }
            }
            cierto(minFuriaCuerpo > maxFocoCuerpo, "FURIA supera a FOCO en cuerpo a cuerpo");
            cierto(minFocoDist > maxFuriaDist, "FOCO supera a FURIA en daño a distancia");
        });
    }

    private static void apuntarArmasCaracteristicasYLibertad() {
        caso("unificación: armas características con sinergia acotada y libertad total de montaje cruzado", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            cierto(Montaje.BONO_ARMA_CARACTERISTICA > 0 && Montaje.BONO_ARMA_CARACTERISTICA <= 0.15,
                    "bonificación de sinergia real pero acotada: " + Montaje.BONO_ARMA_CARACTERISTICA);
            for (Modelo m : Modelo.todos()) {
                List<String> propias = m.armasCaracteristicas();
                cierto(!propias.isEmpty(), m.id + " tiene armas características definidas");
                Montaje montaje = new Montaje(m);
                cierto(montaje.armasCaracteristicasMontadas() >= 1, m.id + " trae sus armas características de fábrica");
                for (String baseId : propias) {
                    Pieza p = cat.pieza(baseId);
                    cierto(p != null && p.ranura == Pieza.Ranura.BRAZO, m.id + ": arma característica válida " + baseId);
                    cierto(montaje.esArmaCaracteristica(p), m.id + " reconoce " + p.id);
                    igual(Montaje.BONO_ARMA_CARACTERISTICA, montaje.sinergiaArma(p), m.id + " sinergia exacta con " + p.id);
                    Pieza mk2 = cat.pieza(baseId + "_2");
                    if (mk2 != null) {
                        cierto(montaje.esArmaCaracteristica(mk2), m.id + " reconoce también versión mejorada " + mk2.id);
                    }
                }
            }

            // Sinergia real pero acotada: mismo arma (martillo) en su modelo (mazo) frente a otro modelo (cobalto).
            Pieza martillo = cat.pieza("martillo");
            Montaje mazo = new Montaje(Modelo.de("mazo"));
            Montaje cobaltoConMartillo = new Montaje(Modelo.de("cobalto"));
            cierto(cobaltoConMartillo.poner(Montaje.Hueco.IZQUIERDA, martillo), "cobalto puede equipar martillo");
            igual(0.0, cobaltoConMartillo.sinergiaArma(martillo), "martillo no da sinergia de arma propia a cobalto");
            double danoMazo = Armas.danoDeArma(mazo, mazo.cifras(), martillo, 1.0);
            double danoCobalto = Armas.danoDeArma(cobaltoConMartillo, cobaltoConMartillo.cifras(), martillo, 1.0);
            cierto(danoMazo > danoCobalto, "mazo con su martillo pega más que cobalto con martillo: " + danoMazo + " vs " + danoCobalto);
            cierto(danoMazo < danoCobalto * 1.35, "la sinergia + naturaleza no duplica ni rompe el equilibrio");

            // Libertad de montaje: chispa con francotirador conserva velocidad alta y blindaje ligero de chispa.
            Pieza franco = cat.pieza("laser_francotirador");
            Montaje chispaFranco = new Montaje(Modelo.de("chispa"));
            cierto(chispaFranco.poner(Montaje.Hueco.DERECHA, franco), "chispa puede montar láser de precisión");
            igual(franco.id, chispaFranco.pieza(Montaje.Hueco.DERECHA).id, "arma cruzada queda equipada");
            igual(0.0, chispaFranco.sinergiaArma(franco), "sin sinergia indebida al cruzar armas");
            cierto(chispaFranco.cifras().velocidad > new Montaje(Modelo.de("cobalto")).cifras().velocidad * 2.0,
                    "chispa conserva su alta velocidad al cambiar de arma");
            cierto(chispaFranco.cifras().blindajeTotal() < new Montaje(Modelo.de("cobalto")).cifras().blindajeTotal() * 0.7,
                    "chispa conserva su menor blindaje al cambiar de arma");
        });
    }

    private static void apuntarDiferenciacionYObstaculos() {
        caso("unificación: diferenciación real cuerpo/distancia y destrucción de obstáculos en Pelea y Carrera", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            Pieza espada = cat.pieza("espada");
            Pieza laserPesado = cat.pieza("laser_pesado");
            Montaje mazo = new Montaje(Modelo.de("mazo"));
            Montaje vigia = new Montaje(Modelo.de("vigia"));
            mazo.poner(Montaje.Hueco.IZQUIERDA, espada);
            vigia.poner(Montaje.Hueco.IZQUIERDA, espada);
            double meleeMazo = Armas.danoDeArma(mazo, mazo.cifras(), espada, 1.0);
            double meleeVigia = Armas.danoDeArma(vigia, vigia.cifras(), espada, 1.0);
            cierto(meleeMazo > meleeVigia, "modelo de corto alcance hace más daño cuerpo a cuerpo: " + meleeMazo + " vs " + meleeVigia);

            mazo.poner(Montaje.Hueco.IZQUIERDA, laserPesado);
            vigia.poner(Montaje.Hueco.IZQUIERDA, laserPesado);
            double distVigia = Armas.danoDeArma(vigia, vigia.cifras(), laserPesado, 1.0);
            double distMazo = Armas.danoDeArma(mazo, mazo.cifras(), laserPesado, 1.0);
            cierto(distVigia > distMazo, "modelo a distancia hace más daño a distancia: " + distVigia + " vs " + distMazo);

            // Armas pesadas destruyen barril y chimenea en menos impactos que armas ligeras, igual en Pelea y Carrera.
            Pieza pesada = cat.pieza("canon_riel");
            Pieza ligera = cat.pieza("canon_ligero");
            int impactosPesadaBarrilPelea = impactosParaDestruirEnPelea("CHATARRERIA", "barril", pesada);
            int impactosLigeraBarrilPelea = impactosParaDestruirEnPelea("CHATARRERIA", "barril", ligera);
            cierto(impactosPesadaBarrilPelea > 0 && impactosPesadaBarrilPelea < impactosLigeraBarrilPelea,
                    "en Pelea el arma pesada rompe barril en menos impactos: "
                            + impactosPesadaBarrilPelea + " vs " + impactosLigeraBarrilPelea);

            int impactosPesadaChimeneaPelea = impactosParaDestruirEnPelea("TEJADO", "chimenea", pesada);
            int impactosLigeraChimeneaPelea = impactosParaDestruirEnPelea("TEJADO", "chimenea", ligera);
            cierto(impactosPesadaChimeneaPelea > 0 && impactosPesadaChimeneaPelea < impactosLigeraChimeneaPelea,
                    "en Pelea el arma pesada rompe chimenea en menos impactos: "
                            + impactosPesadaChimeneaPelea + " vs " + impactosLigeraChimeneaPelea);

            int impactosPesadaBarrilCarrera = impactosParaDestruirEnCarrera("barril", pesada);
            int impactosLigeraBarrilCarrera = impactosParaDestruirEnCarrera("barril", ligera);
            int impactosPesadaChimeneaCarrera = impactosParaDestruirEnCarrera("chimenea", pesada);
            int impactosLigeraChimeneaCarrera = impactosParaDestruirEnCarrera("chimenea", ligera);
            igual(impactosPesadaBarrilPelea, impactosPesadaBarrilCarrera, "mismos impactos en barril (arma pesada) Pelea vs Carrera");
            igual(impactosLigeraBarrilPelea, impactosLigeraBarrilCarrera, "mismos impactos en barril (arma ligera) Pelea vs Carrera");
            igual(impactosPesadaChimeneaPelea, impactosPesadaChimeneaCarrera, "mismos impactos en chimenea (arma pesada) Pelea vs Carrera");
            igual(impactosLigeraChimeneaPelea, impactosLigeraChimeneaCarrera, "mismos impactos en chimenea (arma ligera) Pelea vs Carrera");
        });
    }

    private static int impactosParaDestruirEnPelea(String escenario, String tipo, Pieza arma) {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        m.poner(Montaje.Hueco.IZQUIERDA, arma);
        Pelea p = new Pelea(
                new Combatiente("tirador", m, TiradorIzquierdo::new),
                new Combatiente("quieto", new Montaje(Modelo.de("cobalto")), Quieto::new),
                501L);
        p.arena = Arena.de(escenario, p.ancho, p.alto);
        p.empezarRonda();
        Arena.Trampa objetivo = null;
        for (Arena.Trampa t : p.arena.trampas()) {
            if (t.tipo.equals(tipo)) {
                objetivo = t;
                break;
            }
        }
        cierto(objetivo != null, "existe " + tipo + " en " + escenario);
        Cuerpo c0 = p.cuerpo(0), c1 = p.cuerpo(1);
        c1.x = 920;
        c1.y = 620;
        int impactos = 0;
        double resAnterior = objetivo.resistencia();
        for (int t = 0; t < 400 && !objetivo.destruida(); t++) {
            c0.x = objetivo.x - 120;
            c0.y = objetivo.y;
            c0.mira = 0;
            c0.energia = c0.cifras.energiaMax;
            p.avanzar();
            if (objetivo.resistencia() < resAnterior - 1e-6) {
                impactos++;
                resAnterior = objetivo.resistencia();
            }
        }
        cierto(objetivo.destruida(), tipo + " destruido en Pelea con " + arma.id);
        return impactos;
    }

    private static int impactosParaDestruirEnCarrera(String tipo, Pieza arma) {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        m.poner(Montaje.Hueco.IZQUIERDA, arma);
        CarreraDeObstaculos carrera = new CarreraDeObstaculos(
                new Combatiente("tirador", m, TiradorIzquierdo::new),
                new Combatiente("quieto", new Montaje(Modelo.de("cobalto")), Quieto::new),
                502L, false);
        Arena.Trampa objetivo = null;
        for (Arena.Trampa t : carrera.pelea.arena.trampas()) {
            if (t.tipo.equals(tipo) && !t.ocultoEnMapa) {
                objetivo = t;
                break;
            }
        }
        cierto(objetivo != null, "existe " + tipo + " visible en Carrera");
        Cuerpo c0 = carrera.pelea.cuerpo(0), c1 = carrera.pelea.cuerpo(1);
        c1.x = 120;
        c1.y = 680;
        for (Cuerpo def : carrera.robotsDefectuosos()) {
            def.fuera = true;
        }
        int impactos = 0;
        double resAnterior = objetivo.resistencia();
        for (int t = 0; t < 400 && !objetivo.destruida(); t++) {
            c0.x = objetivo.x - 120;
            c0.y = objetivo.y;
            c0.mira = 0;
            c0.energia = c0.cifras.energiaMax;
            carrera.avanzar();
            if (objetivo.resistencia() < resAnterior - 1e-6) {
                impactos++;
                resAnterior = objetivo.resistencia();
            }
        }
        cierto(objetivo.destruida(), tipo + " destruido en Carrera con " + arma.id);
        return impactos;
    }

    private static void apuntarParidadEntreModos() {
        caso("unificación: el mismo Montaje produce exactamente las mismas cifras físicas y de daño en Pelea y Carrera", () -> {
            CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
            Montaje m = new Montaje(Modelo.de("vigia"));
            m.poner(Montaje.Hueco.IZQUIERDA, cat.pieza("laser_francotirador"));
            m.poner(Montaje.Hueco.DERECHA, cat.pieza("martillo"));

            Pelea pelea = new Pelea(
                    new Combatiente("A", m.copia(), Quieto::new),
                    new Combatiente("B", new Montaje(Modelo.de("cobalto")), Quieto::new),
                    701L);
            pelea.empezarRonda();
            CarreraDeObstaculos carrera = new CarreraDeObstaculos(
                    new Combatiente("A", m.copia(), Quieto::new),
                    new Combatiente("B", new Montaje(Modelo.de("cobalto")), Quieto::new),
                    701L, false);

            Montaje.Cifras cp = pelea.cuerpo(0).cifras;
            Montaje.Cifras cc = carrera.pelea.cuerpo(0).cifras;
            igual(cp.velocidad, cc.velocidad, "velocidad idéntica");
            igual(cp.giro, cc.giro, "giro idéntico");
            igual(cp.aceleracion, cc.aceleracion, "aceleración idéntica");
            igual(cp.blindajeTotal(), cc.blindajeTotal(), "blindaje total idéntico");
            igual(cp.energiaMax, cc.energiaMax, "energía máxima idéntica");
            igual(cp.recarga, cc.recarga, "recarga idéntica");
            igual(cp.esquiva, cc.esquiva, "esquiva idéntica");
            igual(cp.pasos, cc.pasos, "pasos idénticos");
            igual(cp.cuerpo, cc.cuerpo, "multiplicador cuerpo idéntico");
            igual(cp.distancia, cc.distancia, "multiplicador distancia idéntico");

            Pieza izq = m.pieza(Montaje.Hueco.IZQUIERDA);
            Pieza der = m.pieza(Montaje.Hueco.DERECHA);
            igual(Armas.danoDeArma(pelea.cuerpo(0).combatiente.montaje, cp, izq, 1.0),
                    Armas.danoDeArma(carrera.pelea.cuerpo(0).combatiente.montaje, cc, izq, 1.0), "daño arma izquierda idéntico");
            igual(Armas.danoDeArma(pelea.cuerpo(0).combatiente.montaje, cp, der, 1.0),
                    Armas.danoDeArma(carrera.pelea.cuerpo(0).combatiente.montaje, cc, der, 1.0), "daño arma derecha idéntico");
            igual(Armas.danoChoqueAObstaculo(pelea.cuerpo(0).combatiente.montaje, cp, 5.0),
                    Armas.danoChoqueAObstaculo(carrera.pelea.cuerpo(0).combatiente.montaje, cc, 5.0), "daño choque obstáculo idéntico");
        });
    }

    private static void apuntarConocimientoDelEntrenador() {
        caso("unificación: conocimiento del entrenador actualizado en ES, DE y EN (offline e IA) sin vacíos ni fugas", () -> {
            ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("modelos_y_armas");
            cierto(entrada != null, "existe entrada modelos_y_armas");
            cierto(!entrada.consejos().isEmpty(), "existe consejo espontáneo para modelos_y_armas");

            Pattern apiEs = Pattern.compile("\\b(cadaTurno|avanzar|girar|apuntarHacia|enemigosVistos|obstaculos)\\(\\)");
            String[][] pruebas = {
                    {"armas características", "ES", "blindaje", "sinergia", "carrera de obstáculos"},
                    {"charakteristische Waffen", "DE", "panzerung", "synergie", "hindernislauf"},
                    {"signature weapons", "EN", "armor", "synergy", "obstacle race"}
            };
            for (String[] q : pruebas) {
                Idioma id = Idioma.valueOf(q[1]);
                List<Concepto> res = Concepto.buscar(q[0], id, 3);
                cierto(!res.isEmpty() && res.get(0).id.equals("modelos_y_armas"),
                        "buscador offline encuentra modelos_y_armas para «" + q[0] + "» (" + id + ")");
                String texto = String.join(" ", res.get(0).parrafos(id));
                String bajo = texto.toLowerCase(Locale.ROOT);
                cierto(bajo.contains(q[2]) && bajo.contains(q[3]) && bajo.contains(q[4]),
                        "contenido actualizado en " + id);
                if (id != Idioma.ES) {
                    falso(apiEs.matcher(texto).find(), "sin fugas de API en español en " + id);
                }
                String tip = ConsejosDelJuego.textoDelTip(entrada.consejos().get(0).aTip(), id);
                cierto(tip != null && !tip.isBlank(), "consejo espontáneo no vacío en " + id);
                String sistemaIa = ConsultaIaEntrenador.construirSistema(new Jugador("Test"), id, null).toLowerCase(Locale.ROOT);
                cierto(sistemaIa.contains("signature weapons") && sistemaIa.contains("trade-offs")
                        && sistemaIa.contains("obstacle race"), "contexto IA actualizado en " + id);
            }
        });
    }
}
