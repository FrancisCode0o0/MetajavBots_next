package metajavbots.combate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import metajavbots.api.Enemigo;
import metajavbots.api.Metajavbot;
import metajavbots.api.Obstaculo;
import metajavbots.api.RobotBase;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;

/**
 * PISTA DE CARRERA — Generación de trazados 6× por arena, rotación sin repetir
 * la arena inmediata anterior, obstáculos repentinos seguros, robots defectuosos
 * en pista y el corredor rival controlado por la máquina.
 */
public final class PistaDeCarrera {

    /** Arenas disponibles para la rotación de Carrera de obstáculos. */
    public static final List<String> ARENAS = List.of(
            "CHATARRERIA", "COCINA", "HIELO", "TEJADO",
            "DESIERTO", "NEON", "HANGAR", "LABORATORIO", "LUNA");

    /** Margen mínimo delante del corredor al aparecer un obstáculo repentino. */
    public static final double MARGEN_REPENTINO = 190.0;

    /** Definición de un obstáculo repentino que aparece dinámicamente durante la carrera. */
    public static final class ObstaculoRepentino {
        public final String tipo;
        public final double xBase, yBase, radio;
        public final boolean solido;
        public final int ciclo, activo;
        public final double resistencia;
        public final double activacionX;
        public final int activacionTurno;
        private boolean aparecido;
        private Arena.Trampa trampa;

        ObstaculoRepentino(String tipo, double xBase, double yBase, double radio, boolean solido,
                int ciclo, int activo, double resistencia, double activacionX, int activacionTurno) {
            this.tipo = tipo;
            this.xBase = xBase;
            this.yBase = yBase;
            this.radio = radio;
            this.solido = solido;
            this.ciclo = ciclo;
            this.activo = activo;
            this.resistencia = resistencia;
            this.activacionX = activacionX;
            this.activacionTurno = activacionTurno;
        }

        /** Indica si este obstáculo repentino ya apareció en la pista. */
        public boolean aparecido() {
            return aparecido;
        }

        /** La trampa real creada en la arena cuando apareció (o null si aún no ha salido). */
        public Arena.Trampa trampa() {
            return trampa;
        }
    }

    /** Robot defectuoso posicionado en un tramo de la pista. */
    public record DefectuosoEnPista(Combatiente combatiente, double x, double y, boolean inmovil) {
    }

    private PistaDeCarrera() {
    }

    /** Elige una arena de carrera distinta de la inmediatamente anterior de forma determinista. */
    public static String siguienteArena(String anterior, long semilla) {
        int n = ARENAS.size();
        int idx = Math.floorMod(Long.hashCode(semilla * 31L + 17L), n);
        String elegida = ARENAS.get(idx);
        if (anterior != null && anterior.equalsIgnoreCase(elegida)) {
            elegida = ARENAS.get((idx + 1 + Math.floorMod(Long.hashCode(semilla), n - 1)) % n);
        }
        return elegida;
    }

    /** Agarre del suelo según el escenario elegido para la carrera. */
    public static double agarreDe(String escenario) {
        if ("HIELO".equals(escenario)) return 0.48;
        if ("TEJADO".equals(escenario)) return 0.74;
        if ("DESIERTO".equals(escenario)) return 0.86;
        return 1.0;
    }

    /** Genera la arena 6× con obstáculos visibles, ocultos, destructibles, indestructibles e impulsos. */
    public static Arena generarArena(double ancho, double alto, long semilla, String escenario) {
        String tema = escenario == null || escenario.isBlank() ? siguienteArena(null, semilla) : escenario;
        Arena a = Arena.crearParaCarrera(agarreDe(tema));
        Random r = new Random(semilla ^ tema.hashCode());
        int sectores = 14;
        double inicio = Math.max(420, ancho * 0.08);
        double fin = Math.min(ancho - 360, ancho * 0.94);
        double paso = (fin - inicio) / Math.max(1, sectores - 1);

        // Sector 0 visible: barril destructible y bloque indestructible garantizados.
        a.agregarDestructible("barril", inicio + jitter(r, 26), alto * 0.28 + jitter(r, 22), 28, Arena.RESISTENCIA_BARRIL, false);
        a.agregarTrampa("bloque", inicio + jitter(r, 26), alto * 0.72 + jitter(r, 22), 34, true, 0, 0, false);

        for (int s = 1; s < sectores; s++) {
            double sx = inicio + s * paso;
            poblarSector(a, r, s, sx, alto, tema);
        }
        colocarImpulsos(a, r, ancho, alto);
        return a;
    }

    private static void poblarSector(Arena a, Random r, int s, double sx, double alto, String tema) {
        boolean oculto = s % 3 == 1;
        double y1 = alto * (0.20 + (s % 2) * 0.10) + jitter(r, 28);
        double y2 = alto * 0.50 + jitter(r, 34);
        double y3 = alto * (0.80 - (s % 2) * 0.10) + jitter(r, 28);
        if (s % 2 == 1) {
            a.agregarDestructible(s % 4 == 1 ? "barril" : "chimenea", sx + jitter(r, 40), y1,
                    s % 4 == 1 ? 28 : 34, s % 4 == 1 ? Arena.RESISTENCIA_BARRIL : Arena.RESISTENCIA_CHIMENEA, oculto);
            agregarIndestructible(a, r, sx + jitter(r, 40), y2, tema, !oculto);
            a.agregarDestructible("barril", sx + jitter(r, 40), y3, 28, Arena.RESISTENCIA_BARRIL, s % 3 == 2);
        } else {
            agregarIndestructible(a, r, sx + jitter(r, 40), y1, tema, oculto);
            a.agregarDestructible("chimenea", sx + jitter(r, 40), y2, 34, Arena.RESISTENCIA_CHIMENEA, false);
            agregarIndestructible(a, r, sx + jitter(r, 40), y3, tema, !oculto);
        }
        agregarToqueDeArena(a, r, s, sx, alto, tema);
    }

    private static void agregarIndestructible(Arena a, Random r, double x, double y, String tema, boolean oculto) {
        int tipo = r.nextInt(3);
        if (tipo == 0) {
            a.agregarTrampa("bloque", x, y, 34, true, 0, 0, oculto);
        } else if (tipo == 1) {
            a.agregarTrampa("foso", x, y, 50, false, 0, 0, oculto);
        } else {
            Arena.Trampa p = a.agregarTrampa("pinchos", x, y, 46, false, 125, 55, oculto);
            p.fase = r.nextInt(125);
        }
    }

    private static void agregarToqueDeArena(Arena a, Random r, int s, double sx, double alto, String tema) {
        if ("CHATARRERIA".equals(tema) && s % 5 == 0) {
            a.agregarTrampa("iman", sx + 120, alto * 0.5, 145, false, 0, 0, false);
        } else if ("COCINA".equals(tema) && s % 4 == 0) {
            a.agregarTrampa("sierra", sx + 80, s % 8 == 0 ? 78 : alto - 78, 56, false, 0, 0, false);
        } else if ("TEJADO".equals(tema) && s % 5 == 0) {
            Arena.Trampa rayo = a.agregarTrampa("rayo", sx + 100, alto * 0.5, 58, false, 180, 32, false);
            rayo.vy = r.nextBoolean() ? 1.5 : -1.5;
        }
    }

    private static void colocarImpulsos(Arena a, Random r, double ancho, double alto) {
        int cantidad = 8;
        double inicio = Math.max(550, ancho * 0.09);
        double fin = ancho * 0.90;
        for (int i = 0; i < cantidad; i++) {
            double x = inicio + (fin - inicio) * i / (cantidad - 1) + jitter(r, 65);
            double y = alto * (0.22 + ((i * 37 + Math.abs(r.nextInt(30))) % 56) / 100.0);
            a.agregarImpulso(x, y, 30);
        }
    }

    /** Planifica los obstáculos repentinos (destructibles e indestructibles) que salen en marcha. */
    public static List<ObstaculoRepentino> planearRepentinos(double ancho, double alto, long semilla) {
        Random r = new Random(semilla * 73L + 19L);
        List<ObstaculoRepentino> lista = new ArrayList<>();
        int total = 8;
        for (int i = 0; i < total; i++) {
            double xBase = ancho * (0.11 + i * 0.105) + jitter(r, 40);
            double yBase = alto * (0.22 + (i % 3) * 0.28) + jitter(r, 28);
            double actX = Math.max(CarreraDeObstaculos.SALIDA_X + 120, xBase - 260);
            int actTurno = i == 0 ? 35 : i == 1 ? 110 : 240 + i * 280;
            if (i % 2 == 0) {
                boolean barril = i % 4 == 0;
                lista.add(new ObstaculoRepentino(barril ? "barril" : "chimenea", xBase, yBase,
                        barril ? 28 : 34, true, 0, 0,
                        barril ? Arena.RESISTENCIA_BARRIL : Arena.RESISTENCIA_CHIMENEA, actX, actTurno));
            } else {
                boolean bloque = i % 4 == 1;
                lista.add(new ObstaculoRepentino(bloque ? "bloque" : "pinchos", xBase, yBase,
                        bloque ? 34 : 46, bloque, bloque ? 0 : 120, bloque ? 0 : 55, 0, actX, actTurno));
            }
        }
        return lista;
    }

    /** Activa los obstáculos repentinos cuyo tramo o turno se haya alcanzado, siempre con margen seguro. */
    public static void actualizarRepentinos(List<ObstaculoRepentino> lista, Pelea pelea) {
        Cuerpo c0 = pelea.cuerpo(0);
        Cuerpo c1 = pelea.cuerpo(1);
        for (ObstaculoRepentino rep : lista) {
            if (rep.aparecido) continue;
            boolean cerca0 = c0 != null && !c0.fuera && c0.x >= rep.activacionX;
            boolean cerca1 = c1 != null && !c1.fuera && c1.x >= rep.activacionX;
            boolean porTurno = pelea.turno() >= rep.activacionTurno;
            if (!cerca0 && !cerca1 && !porTurno) continue;

            double ox = rep.xBase;
            double oy = rep.yBase;
            Cuerpo ref = cerca0 ? c0 : cerca1 ? c1 : c0;
            if (ref != null && ox < ref.x + MARGEN_REPENTINO) {
                ox = ref.x + MARGEN_REPENTINO;
            }
            double[] pos = asegurarPosicionLibre(ox, oy, rep.radio, pelea, c0, c1);
            rep.trampa = pelea.arena.agregarRepentina(rep.tipo, pos[0], pos[1], rep.radio,
                    rep.solido, rep.ciclo, rep.activo, rep.resistencia);
            rep.aparecido = true;
            pelea.suceso(Suceso.Tipo.TRAMPA, -1, pos[0], pos[1], 0, rep.radio, "repentino_" + rep.tipo);
        }
    }

    private static double[] asegurarPosicionLibre(double ox, double oy, double radio, Pelea pelea, Cuerpo c0, Cuerpo c1) {
        double x = Math.max(CarreraDeObstaculos.SALIDA_X + 160, Math.min(CarreraDeObstaculos.META_X - 100, ox));
        double y = Math.max(95, Math.min(pelea.alto - 95, oy));
        for (int intento = 0; intento < 8; intento++) {
            boolean libre0 = c0 == null || c0.fuera || Math.hypot(x - c0.x, y - c0.y) >= c0.radio + radio + 120;
            boolean libre1 = c1 == null || c1.fuera || Math.hypot(x - c1.x, y - c1.y) >= c1.radio + radio + 120;
            if (libre0 && libre1) break;
            x = Math.min(CarreraDeObstaculos.META_X - 95, x + 90);
            y = y > pelea.alto * 0.5 ? Math.max(95, y - 85) : Math.min(pelea.alto - 95, y + 85);
        }
        return new double[]{x, y};
    }

    /** Crea los robots defectuosos/averiados que entorpecen distintos tramos de la pista 6×. */
    public static List<DefectuosoEnPista> crearRobotsDefectuosos(double ancho, double alto, long semilla) {
        Random r = new Random(semilla * 97L + 41L);
        List<DefectuosoEnPista> lista = new ArrayList<>();
        int cantidad = 7;
        for (int i = 0; i < cantidad; i++) {
            boolean inmovil = i % 2 == 0;
            double x = ancho * (0.14 + i * 0.115) + jitter(r, 45);
            double y = alto * (0.24 + (i % 3) * 0.26) + jitter(r, 25);
            Montaje m = montajeDefectuoso();
            Fabrica fab = fabricaDefectuoso(inmovil);
            Combatiente c = new Combatiente("Averiado-" + (i + 1), m, fab);
            c.computadora = true;
            c.defectuoso = true;
            c.escalaBlindaje = 0.24;
            c.escalaDano = 0.35;
            for (int k = 0; k < 6; k++) {
                c.abollar(k % 4, r, 14 + r.nextDouble() * 12, k % 2 == 0);
            }
            lista.add(new DefectuosoEnPista(c, x, y, inmovil));
        }
        return lista;
    }

    private static Montaje montajeDefectuoso() {
        Montaje m = new Montaje(Modelo.de("cobalto"));
        for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
            if (p.arma() == Pieza.Arma.BALA && p.valor("dano") > 0) {
                m.poner(Montaje.Hueco.IZQUIERDA, p);
                break;
            }
        }
        return m;
    }

    private static Fabrica fabricaDefectuoso(boolean inmovil) {
        return new Fabrica() {
            @Override
            public RobotBase crear() {
                return new RobotDefectuoso(inmovil);
            }

            @Override
            public long version() {
                return 1;
            }

            @Override
            public String clase() {
                return RobotDefectuoso.class.getName();
            }

            @Override
            public boolean esSuya(String nombreDeClase) {
                return RobotDefectuoso.class.getName().equals(nombreDeClase);
            }
        };
    }

    private static double jitter(Random r, double amplitud) {
        return (r.nextDouble() * 2 - 1) * amplitud;
    }

    /** Robot averiado en pista: gira a trompicones o deambula sin rumbo y dispara al azar. */
    public static final class RobotDefectuoso extends Metajavbot {

        private final boolean inmovil;
        private double rumboErratico;
        private int paso;

        /** Crea un robot defectuoso inmóvil a trompicones o errático sin rumbo fijo. */
        public RobotDefectuoso(boolean inmovil) {
            this.inmovil = inmovil;
        }

        @Override
        public void cadaTurno() {
            int t = paso++;
            if (inmovil) {
                if (t % 11 == 0) {
                    girar(azar().nextDouble() * 90 - 45);
                }
                if (t % 29 == 0) {
                    avanzar(azar().nextBoolean() ? 14 : -14);
                }
            } else {
                if (t % 18 == 0) {
                    rumboErratico = azar().nextDouble() * 360 - 180;
                }
                if (y() < 110) rumboErratico = 75;
                else if (y() > altoArena() - 110) rumboErratico = -75;
                girarHacia(rumboErratico);
                avanzar(42);
            }
            if (t % 13 == 0) {
                apuntarHacia(azar().nextDouble() * 360 - 180);
            }
            if (t % 17 == 0) {
                atacar();
            }
        }
    }

    /** Robot rival de la máquina para pista 6×: esquiva, rompe destructibles, recoge impulsos y combate. */
    public static final class CorredorMaquina extends Metajavbot {

        @Override
        public void cadaTurno() {
            double rumboObjetivo = buscarImpulso();
            Obstaculo amenaza = buscarAmenaza();
            boolean disparo = false;
            if (amenaza != null) {
                boolean destructible = amenaza.tipo().equals("barril") || amenaza.tipo().equals("chimenea");
                if (destructible) {
                    apuntarHacia(amenaza.angulo());
                    atacar();
                    disparo = true;
                    if (amenaza.distancia() < 105) rumboObjetivo = elegirDesvio(amenaza.y());
                } else {
                    rumboObjetivo = elegirDesvio(amenaza.y());
                }
            }
            Enemigo blanco = buscarEnemigo();
            if (!disparo && blanco != null) {
                apuntarHacia(blanco.angulo());
                atacar();
                if (blanco.distancia() < 115 && Math.abs(normalizar(blanco.angulo())) < 55) {
                    rumboObjetivo = elegirDesvio(blanco.y());
                }
            } else if (!disparo) {
                apuntarHacia(0);
            }
            if (y() < 95 && rumboObjetivo < 0) rumboObjetivo = 28;
            if (y() > altoArena() - 95 && rumboObjetivo > 0) rumboObjetivo = -28;
            girarHacia(rumboObjetivo);
            avanzar(85);
        }

        private double buscarImpulso() {
            Obstaculo mejor = null;
            double dist = 340;
            for (Obstaculo o : obstaculos()) {
                if (!o.tipo().equals("impulso") || o.x() < x() - 20) continue;
                if (o.distancia() < dist && Math.abs(normalizar(o.angulo())) < 52) {
                    dist = o.distancia();
                    mejor = o;
                }
            }
            return mejor == null ? 0 : normalizar(mejor.angulo());
        }

        private Obstaculo buscarAmenaza() {
            Obstaculo amenaza = null;
            double mejorDist = 245;
            for (Obstaculo o : obstaculos()) {
                if (o.tipo().equals("impulso")) continue;
                double diff = Math.abs(normalizar(o.angulo()));
                if (o.distancia() < mejorDist && diff < 48) {
                    mejorDist = o.distancia();
                    amenaza = o;
                }
            }
            return amenaza;
        }

        private Enemigo buscarEnemigo() {
            Enemigo mejor = null;
            double dist = 420;
            for (Enemigo e : enemigosVistos()) {
                if (e.distancia() < dist) {
                    dist = e.distancia();
                    mejor = e;
                }
            }
            return mejor;
        }

        private double elegirDesvio(double yObstaculo) {
            if (y() < 140) return 54;
            if (y() > altoArena() - 140) return -54;
            return yObstaculo >= y() ? -54 : 54;
        }
    }
}
