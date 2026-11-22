package metajavbots.combate;

import java.util.ArrayList;
import java.util.List;

import metajavbots.api.Metajavbot;
import metajavbots.api.Obstaculo;
import metajavbots.api.RobotBase;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;

/**
 * CARRERA DE OBSTÁCULOS — Lógica headless de una carrera en pista desde la
 * salida (izquierda) hasta la meta (derecha) combinando obstáculos visibles en
 * el mapa general, obstáculos ocultos detectables mediante radar/sensores y
 * obstáculos destructibles (barriles/chimeneas) frente a indestructibles
 * (bloques, fosos y pinchos).
 */
public final class CarreraDeObstaculos {

    /** Ancho de la pista de carrera en unidades de arena. */
    public static final double ANCHO = 1400;
    /** Alto de la pista de carrera en unidades de arena. */
    public static final double ALTO = 700;
    /** Coordenada X de la línea de salida. */
    public static final double SALIDA_X = 95;
    /** Coordenada X de la línea de meta. */
    public static final double META_X = 1280;
    /** Duración máxima de una carrera en turnos (30 turnos/s = 40 s). */
    public static final int MAX_TURNOS = 1200;
    /** Recompensa exacta en monedas por cada carrera ganada por el jugador. */
    public static final double RECOMPENSA_MONEDAS = 1.0;

    /** La simulación física subyacente (reutiliza toda la física de combate y obstáculos). */
    public final Pelea pelea;
    /** Semilla determinista de la pista. */
    public final long semilla;
    /** Si el rival es la computadora (true) o un robot ajeno (false). */
    public final boolean contraMaquina;

    private boolean terminada;
    private int ganador = -2;
    private String motivoFin = "";
    private boolean recompensaAplicada;

    /** Crea una nueva carrera de obstáculos lista para ejecutarse turno a turno. */
    public CarreraDeObstaculos(Combatiente jugador, Combatiente rival, long semilla, boolean contraMaquina) {
        this.semilla = semilla;
        this.contraMaquina = contraMaquina;
        this.pelea = new Pelea(jugador, rival, semilla, ANCHO, ALTO, MAX_TURNOS);
        this.pelea.rondas = 1;
        this.pelea.arena = Arena.carreraDeObstaculos(ANCHO, ALTO, semilla);
        this.pelea.empezarRonda();
        colocarEnSalida();
    }

    private void colocarEnSalida() {
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        if (c0 != null) {
            c0.x = SALIDA_X;
            c0.y = ALTO * 0.34;
            c0.rumbo = 0;
            c0.mira = 0;
            c0.velocidad = 0;
            c0.detener();
        }
        if (c1 != null) {
            c1.x = SALIDA_X;
            c1.y = ALTO * 0.66;
            c1.rumbo = 0;
            c1.mira = 0;
            c1.velocidad = 0;
            c1.detener();
        }
    }

    /** Avanza un turno de la carrera y evalúa las condiciones de victoria. */
    public void avanzar() {
        if (terminada) return;
        pelea.avanzar();
        evaluarFin();
    }

    private void evaluarFin() {
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        boolean fuera0 = c0 == null || c0.fuera;
        boolean fuera1 = c1 == null || c1.fuera;
        boolean meta0 = !fuera0 && c0.x >= META_X;
        boolean meta1 = !fuera1 && c1.x >= META_X;
        if (meta0 || meta1) {
            terminada = true;
            motivoFin = "meta";
            if (meta0 && meta1) {
                double diff = c0.x - c1.x;
                ganador = Math.abs(diff) < 1e-3 ? -1 : diff > 0 ? 0 : 1;
            } else {
                ganador = meta0 ? 0 : 1;
            }
            return;
        }
        if (fuera0 || fuera1) {
            terminada = true;
            motivoFin = "ko";
            ganador = fuera0 && fuera1 ? -1 : fuera0 ? 1 : 0;
            return;
        }
        if (pelea.estado() == Pelea.Estado.TERMINADA) {
            terminada = true;
            motivoFin = "tiempo";
            double diff = c0.x - c1.x;
            ganador = Math.abs(diff) < 5.0 ? -1 : diff > 0 ? 0 : 1;
        }
    }

    /** Indica si la carrera ya ha concluido. */
    public boolean terminada() {
        return terminada;
    }

    /** Ganador de la carrera: 0 jugador, 1 rival, -1 empate, -2 en curso. */
    public int ganador() {
        return ganador;
    }

    /** Motivo de finalización: "meta", "ko", "tiempo" o "" si sigue en curso. */
    public String motivoFin() {
        return motivoFin;
    }

    /** Progreso normalizado [0, 1] del corredor indicado hacia la línea de meta. */
    public double progreso(int indice) {
        Cuerpo c = pelea.cuerpo(indice);
        if (c == null) return 0;
        return Math.max(0, Math.min(1, (c.x - SALIDA_X) / (META_X - SALIDA_X)));
    }

    /** Obstáculos visibles en el mapa general previo (excluye los ocultos). */
    public List<Arena.Trampa> obstaculosVisiblesEnMapa() {
        List<Arena.Trampa> visibles = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (!t.ocultoEnMapa) visibles.add(t);
        }
        return visibles;
    }

    /** Obstáculos que permanecen ocultos en el mapa general inicial. */
    public List<Arena.Trampa> obstaculosOcultosEnMapa() {
        List<Arena.Trampa> ocultos = new ArrayList<>();
        for (Arena.Trampa t : pelea.arena.trampas()) {
            if (t.ocultoEnMapa) ocultos.add(t);
        }
        return ocultos;
    }

    /**
     * Aplica la recompensa de 1 moneda si el jugador ganó la carrera (idempotente).
     * Si perdió o empató, no suma ninguna moneda. Devuelve si se otorgó la moneda.
     */
    public boolean aplicarRecompensa(Jugador jugador) {
        if (!terminada || ganador != 0 || recompensaAplicada || jugador == null) return false;
        jugador.sumarMonedas(RECOMPENSA_MONEDAS);
        recompensaAplicada = true;
        return true;
    }

    /** Indica si ya se otorgó la recompensa de moneda de esta carrera. */
    public boolean recompensaAplicada() {
        return recompensaAplicada;
    }

    /** Crea el combatiente rival controlado por la computadora para la carrera de obstáculos. */
    public static Combatiente rivalMaquina(String nombre) {
        Montaje montaje = new Montaje(Modelo.de("cobalto"));
        Fabrica fabrica = new Fabrica() {
            @Override
            public RobotBase crear() {
                return new CorredorMaquina();
            }

            @Override
            public long version() {
                return 1;
            }

            @Override
            public String clase() {
                return CorredorMaquina.class.getName();
            }

            @Override
            public boolean esSuya(String nombreDeClase) {
                return CorredorMaquina.class.getName().equals(nombreDeClase);
            }
        };
        Combatiente c = new Combatiente(nombre, montaje, fabrica);
        c.computadora = true;
        return c;
    }

    /** Devuelve la plantilla inicial compilable de Carrera de obstáculos en el idioma indicado (sin fugas de API). */
    public static String plantillaInicial(Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        switch (id) {
            case DE:
                return "import metajavbots.api.*;\n\n"
                        + "public class HindernisLaeufer extends MetajavbotDe {\n"
                        + "    @Override\n"
                        + "    public void jedenZug() {\n"
                        + "        double zielWinkel = 0;\n"
                        + "        for (Hindernis h : hindernisse()) {\n"
                        + "            double diff = Math.abs(normalisieren(h.winkel()));\n"
                        + "            if (h.entfernung() < 230 && diff < 42) {\n"
                        + "                if (h.typ().equals(\"barril\") || h.typ().equals(\"chimenea\")) {\n"
                        + "                    zielenNach(h.winkel());\n"
                        + "                    angreifen();\n"
                        + "                } else {\n"
                        + "                    zielWinkel = h.y() >= y() ? -52 : 52;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        drehenNach(zielWinkel);\n"
                        + "        vorwaerts(80);\n"
                        + "    }\n"
                        + "}\n";
            case EN:
                return "import metajavbots.api.*;\n\n"
                        + "public class ObstacleRacer extends MetajavbotEn {\n"
                        + "    @Override\n"
                        + "    public void onTick() {\n"
                        + "        double targetAngle = 0;\n"
                        + "        for (Obstacle o : obstacles()) {\n"
                        + "            double diff = Math.abs(normalize(o.angle()));\n"
                        + "            if (o.distance() < 230 && diff < 42) {\n"
                        + "                if (o.type().equals(\"barril\") || o.type().equals(\"chimenea\")) {\n"
                        + "                    aimTo(o.angle());\n"
                        + "                    attack();\n"
                        + "                } else {\n"
                        + "                    targetAngle = o.y() >= y() ? -52 : 52;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        turnTo(targetAngle);\n"
                        + "        ahead(80);\n"
                        + "    }\n"
                        + "}\n";
            default:
                return "import metajavbots.api.*;\n\n"
                        + "public class CorredorObstaculos extends Metajavbot {\n"
                        + "    @Override\n"
                        + "    public void cadaTurno() {\n"
                        + "        double rumboObjetivo = 0;\n"
                        + "        for (Obstaculo o : obstaculos()) {\n"
                        + "            double diff = Math.abs(normalizar(o.angulo()));\n"
                        + "            if (o.distancia() < 230 && diff < 42) {\n"
                        + "                if (o.tipo().equals(\"barril\") || o.tipo().equals(\"chimenea\")) {\n"
                        + "                    apuntarHacia(o.angulo());\n"
                        + "                    atacar();\n"
                        + "                } else {\n"
                        + "                    rumboObjetivo = o.y() >= y() ? -52 : 52;\n"
                        + "                }\n"
                        + "            }\n"
                        + "        }\n"
                        + "        girarHacia(rumboObjetivo);\n"
                        + "        avanzar(80);\n"
                        + "    }\n"
                        + "}\n";
        }
    }

    /** Robot rival de la máquina: navega a la meta, esquiva obstáculos indestructibles y dispara a destructibles. */
    public static final class CorredorMaquina extends Metajavbot {

        @Override
        public void cadaTurno() {
            double rumboObjetivo = 0;
            Obstaculo amenaza = null;
            double mejorDist = Double.MAX_VALUE;
            for (Obstaculo o : obstaculos()) {
                double diff = Math.abs(normalizar(o.angulo()));
                if (o.distancia() < 235 && diff < 46 && o.distancia() < mejorDist) {
                    mejorDist = o.distancia();
                    amenaza = o;
                }
            }
            if (amenaza != null) {
                boolean destructible = amenaza.tipo().equals("barril") || amenaza.tipo().equals("chimenea");
                if (destructible) {
                    apuntarHacia(amenaza.angulo());
                    atacar();
                    if (amenaza.distancia() < 95) {
                        rumboObjetivo = elegirDesvio(amenaza.y());
                    }
                } else {
                    rumboObjetivo = elegirDesvio(amenaza.y());
                    apuntarHacia(0);
                }
            } else {
                apuntarHacia(0);
            }
            if (y() < 75 && rumboObjetivo < 0) rumboObjetivo = 25;
            if (y() > altoArena() - 75 && rumboObjetivo > 0) rumboObjetivo = -25;
            girarHacia(rumboObjetivo);
            avanzar(75);
        }

        private double elegirDesvio(double yObstaculo) {
            if (y() < 120) return 52;
            if (y() > altoArena() - 120) return -52;
            return yObstaculo >= y() ? -52 : 52;
        }
    }
}
