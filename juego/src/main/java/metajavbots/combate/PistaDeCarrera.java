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
        return generarArena(ancho, alto, semilla, escenario, DificultadCarrera.porDefecto());
    }

    /** Genera la arena 6× parametrizada por el nivel de dificultad elegido (subnivel I). */
    public static Arena generarArena(double ancho, double alto, long semilla, String escenario, DificultadCarrera dificultad) {
        return generarArena(ancho, alto, semilla, escenario, dificultad, DificultadCarrera.SUBNIVEL_MIN);
    }

    /** Genera la arena 6× parametrizada por el nivel de dificultad y el subnivel (1..3) elegidos. */
    public static Arena generarArena(double ancho, double alto, long semilla, String escenario,
            DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int sub = DificultadCarrera.normalizarSubnivel(subnivel);
        String tema = escenario == null || escenario.isBlank() ? siguienteArena(null, semilla) : escenario;
        Arena a = Arena.crearParaCarrera(agarreDe(tema));
        Random r = new Random(semilla ^ tema.hashCode() ^ (dif.ordinal() * 131L) ^ (sub * 29L));
        int sectores = dif.sectores;
        double inicio = Math.max(420, ancho * 0.08);
        double fin = Math.min(ancho - 360, ancho * 0.94);
        double paso = (fin - inicio) / Math.max(1, sectores - 1);

        // Sector 0 visible: barril destructible y bloque indestructible garantizados.
        a.agregarDestructible("barril", inicio + jitter(r, 26), alto * 0.28 + jitter(r, 22), 28, Arena.RESISTENCIA_BARRIL, false);
        a.agregarTrampa("bloque", inicio + jitter(r, 26), alto * 0.72 + jitter(r, 22), 34, true, 0, 0, false);

        for (int s = 1; s < sectores; s++) {
            double sx = inicio + s * paso;
            poblarSector(a, r, s, sx, alto, tema, dif, sub);
        }
        colocarTrampasMortales(a, r, ancho, alto, dif);
        colocarMonedasExtra(a, r, ancho, alto);
        colocarRampas(a, r, ancho, alto, dif);
        colocarZonasAgua(a, r, ancho, alto, dif);
        colocarReparaciones(a, r, ancho, alto, dif);
        colocarImpulsos(a, r, ancho, alto, dif);
        return a;
    }

    private static void poblarSector(Arena a, Random r, int s, double sx, double alto, String tema,
            DificultadCarrera dif, int subnivel) {
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        int extraSub = extra == 2 ? 1 : (extra == 1 && s % 2 == 0 ? 1 : 0);
        int cantidad = dif.obstaculosPorSector + (dif == DificultadCarrera.SENIOR && s % 2 == 0 ? 1 : 0) + extraSub;
        int carrilLibre = Math.floorMod(s * 3 + r.nextInt(Math.max(2, cantidad + 1)), cantidad + 1);
        double margenY = 105.0;
        double tramoY = (alto - margenY * 2) / Math.max(1, cantidad);
        double desfaseSector = ((s % 3) - 1) * 46.0;
        int indestructiblesEnSector = 0;
        int maxIndestructibles = Math.max(1, cantidad - 1);
        for (int k = 0; k < cantidad; k++) {
            int carril = k >= carrilLibre ? k + 1 : k;
            double oy = margenY + tramoY * (carril + 0.5) / (cantidad + 1) * cantidad + desfaseSector;
            oy = Math.max(92, Math.min(alto - 92, oy + jitter(r, 20)));
            double ox = sx + ((k % 2 == 0 ? -1 : 1) * (24 + k * 14)) + jitter(r, 24);
            boolean oculto = r.nextDouble() < dif.proporcionOcultos;
            boolean indestructible = indestructiblesEnSector < maxIndestructibles && r.nextDouble() < dif.proporcionIndestructibles;
            if (indestructible) {
                indestructiblesEnSector++;
                agregarIndestructible(a, r, ox, oy, tema, oculto);
            } else {
                boolean barril = (s + k) % 2 == 0;
                a.agregarDestructible(barril ? "barril" : "chimenea", ox, oy,
                        barril ? 28 : 34, barril ? Arena.RESISTENCIA_BARRIL : Arena.RESISTENCIA_CHIMENEA, oculto);
            }
        }
        agregarToqueDeArena(a, r, s, sx, alto, tema, dif);
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

    private static void agregarToqueDeArena(Arena a, Random r, int s, double sx, double alto, String tema, DificultadCarrera dif) {
        int pasoEspecial = dif == DificultadCarrera.NOOB ? 6 : dif == DificultadCarrera.JUNIOR ? 5 : dif == DificultadCarrera.SENIOR ? 4 : 3;
        if ("CHATARRERIA".equals(tema) && s % pasoEspecial == 0) {
            a.agregarTrampa("iman", sx + 120, alto * 0.5, 145, false, 0, 0, false);
        } else if ("COCINA".equals(tema) && s % pasoEspecial == 0) {
            a.agregarTrampa("sierra", sx + 80, s % (pasoEspecial * 2) == 0 ? 78 : alto - 78, 56, false, 0, 0, false);
        } else if ("TEJADO".equals(tema) && s % pasoEspecial == 0) {
            Arena.Trampa rayo = a.agregarTrampa("rayo", sx + 100, alto * 0.5, 58, false, 180, 32, false);
            rayo.vy = r.nextBoolean() ? 1.5 : -1.5;
        }
    }

    private static void colocarTrampasMortales(Arena a, Random r, double ancho, double alto, DificultadCarrera dif) {
        int cantidad = dif.cantidadTrampasMortales;
        double inicio = Math.max(950, ancho * 0.16);
        double fin = ancho * 0.87;
        for (int i = 0; i < cantidad; i++) {
            double frac = cantidad <= 1 ? 0.5 : (double) i / (cantidad - 1);
            double x = inicio + (fin - inicio) * frac + jitter(r, 55);
            double y = i == 0 ? alto * 0.34 + jitter(r, 14)
                    : alto * (0.24 + ((i * 29 + Math.abs(r.nextInt(24))) % 52) / 100.0);
            y = Math.max(130, Math.min(alto - 130, y));
            a.agregarTrampaMortal(x, y, 34);
        }
    }

    private static void colocarMonedasExtra(Arena a, Random r, double ancho, double alto) {
        int cantidad = 1 + r.nextInt(2);
        List<Arena.Trampa> peligros = new ArrayList<>();
        for (Arena.Trampa t : a.trampas()) {
            if (t.tipo.equals(Arena.TIPO_TRAMPA_MORTAL)) peligros.add(t);
        }
        for (int i = 0; i < cantidad; i++) {
            Arena.Trampa ref = !peligros.isEmpty() ? peligros.get(i % peligros.size()) : null;
            double mx, my;
            if (ref != null) {
                double dirY = ref.y > alto * 0.5 ? -1.0 : 1.0;
                mx = Math.max(580, Math.min(ancho - 380, ref.x + (i % 2 == 0 ? 18 : -18)));
                my = Math.max(115, Math.min(alto - 115, ref.y + dirY * 86.0));
            } else {
                mx = ancho * (0.35 + i * 0.32);
                my = alto * (0.30 + i * 0.36);
            }
            a.agregarMonedaExtra(mx, my, 24);
        }
    }

    private static void colocarRampas(Arena a, Random r, double ancho, double alto, DificultadCarrera dif) {
        int cantidad = dif.cantidadRampas;
        List<Arena.Trampa> trampasSuelo = new ArrayList<>();
        for (Arena.Trampa t : a.trampas()) {
            if (t.tipo.equals(Arena.TIPO_TRAMPA_MORTAL) || t.tipo.equals("foso") || t.tipo.equals("pinchos")) {
                trampasSuelo.add(t);
            }
        }
        int colocadas = 0;
        for (int i = 0; i < trampasSuelo.size() && colocadas < cantidad; i += Math.max(1, trampasSuelo.size() / Math.max(1, cantidad))) {
            Arena.Trampa obj = trampasSuelo.get(i);
            double rx = Math.max(480, obj.x - 94.0);
            double ry = Math.max(115, Math.min(alto - 115, obj.y));
            a.agregarRampa(rx, ry, 32);
            colocadas++;
        }
        double inicio = Math.max(700, ancho * 0.14);
        double fin = ancho * 0.86;
        while (colocadas < cantidad) {
            double frac = cantidad <= 1 ? 0.5 : (double) colocadas / (cantidad - 1);
            double rx = inicio + (fin - inicio) * frac + jitter(r, 40);
            double ry = Math.max(115, Math.min(alto - 115, alto * (0.28 + (colocadas % 3) * 0.22)));
            a.agregarRampa(rx, ry, 32);
            colocadas++;
        }
    }

    private static void colocarZonasAgua(Arena a, Random r, double ancho, double alto, DificultadCarrera dif) {
        int cantidad = dif.cantidadAguas;
        double inicio = Math.max(820, ancho * 0.13);
        double fin = ancho * 0.88;
        for (int i = 0; i < cantidad; i++) {
            double frac = cantidad <= 1 ? 0.5 : (double) i / (cantidad - 1);
            double ax = inicio + (fin - inicio) * frac + jitter(r, 55);
            double ay = alto * (0.22 + ((i * 33 + Math.abs(r.nextInt(28))) % 56) / 100.0);
            ay = Math.max(118, Math.min(alto - 118, ay));
            a.agregarAgua(ax, ay, 44);
        }
    }

    private static void colocarReparaciones(Arena a, Random r, double ancho, double alto, DificultadCarrera dif) {
        int cantidad = dif.cantidadReparaciones;
        double inicio = Math.max(1150, ancho * 0.17);
        double fin = ancho * 0.85;
        for (int i = 0; i < cantidad; i++) {
            double frac = cantidad <= 1 ? 0.5 : (double) i / (cantidad - 1);
            double x = inicio + (fin - inicio) * frac + jitter(r, 50);
            double y = alto * (0.20 + ((i * 41 + Math.abs(r.nextInt(26))) % 60) / 100.0);
            y = Math.max(115, Math.min(alto - 115, y));
            a.agregarReparacion(x, y, 28);
        }
    }

    private static void colocarImpulsos(Arena a, Random r, double ancho, double alto, DificultadCarrera dif) {
        int cantidad = dif.cantidadImpulsos;
        double inicio = Math.max(550, ancho * 0.09);
        double fin = ancho * 0.90;
        for (int i = 0; i < cantidad; i++) {
            double frac = cantidad <= 1 ? 0.5 : (double) i / (cantidad - 1);
            double x = inicio + (fin - inicio) * frac + jitter(r, 65);
            double y = alto * (0.22 + ((i * 37 + Math.abs(r.nextInt(30))) % 56) / 100.0);
            a.agregarImpulso(x, y, 30);
        }
    }

    /** Planifica los obstáculos repentinos (destructibles e indestructibles) que salen en marcha. */
    public static List<ObstaculoRepentino> planearRepentinos(double ancho, double alto, long semilla) {
        return planearRepentinos(ancho, alto, semilla, DificultadCarrera.porDefecto());
    }

    /** Planifica los obstáculos repentinos según la dificultad elegida (subnivel I). */
    public static List<ObstaculoRepentino> planearRepentinos(double ancho, double alto, long semilla, DificultadCarrera dificultad) {
        return planearRepentinos(ancho, alto, semilla, dificultad, DificultadCarrera.SUBNIVEL_MIN);
    }

    /** Planifica los obstáculos repentinos según la dificultad y el subnivel (1..3) elegidos. */
    public static List<ObstaculoRepentino> planearRepentinos(double ancho, double alto, long semilla,
            DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        Random r = new Random(semilla * 73L + 19L + dif.ordinal() * 997L + extra * 53L);
        List<ObstaculoRepentino> lista = new ArrayList<>();
        int total = dif.cantidadRepentinos + extra * 2;
        double inicioX = ancho * 0.10;
        double finX = ancho * 0.91;
        for (int i = 0; i < total; i++) {
            double frac = total <= 1 ? 0.5 : (double) i / (total - 1);
            double xBase = inicioX + (finX - inicioX) * frac + jitter(r, 36);
            double yBase = alto * (0.20 + (i % 3) * 0.30) + jitter(r, 26);
            double actX = Math.max(CarreraDeObstaculos.SALIDA_X + 120, xBase - (dif.margenRepentino + 70));
            int actTurno = i == 0 ? 35 : i == 1 ? 105 : 220 + i * Math.max(120, 280 - dif.ordinal() * 45);
            boolean indestructible = r.nextDouble() < dif.proporcionIndestructibles;
            if (!indestructible) {
                boolean barril = i % 2 == 0;
                lista.add(new ObstaculoRepentino(barril ? "barril" : "chimenea", xBase, yBase,
                        barril ? 28 : 34, true, 0, 0,
                        barril ? Arena.RESISTENCIA_BARRIL : Arena.RESISTENCIA_CHIMENEA, actX, actTurno));
            } else {
                boolean bloque = i % 2 == 1;
                lista.add(new ObstaculoRepentino(bloque ? "bloque" : "pinchos", xBase, yBase,
                        bloque ? 34 : 46, bloque, bloque ? 0 : 120, bloque ? 0 : 55, 0, actX, actTurno));
            }
        }
        return lista;
    }

    /** Activa los obstáculos repentinos cuyo tramo o turno se haya alcanzado, siempre con margen seguro. */
    public static void actualizarRepentinos(List<ObstaculoRepentino> lista, Pelea pelea) {
        actualizarRepentinos(lista, pelea, DificultadCarrera.porDefecto());
    }

    /** Activa los obstáculos repentinos respetando el margen de seguridad de la dificultad elegida. */
    public static void actualizarRepentinos(List<ObstaculoRepentino> lista, Pelea pelea, DificultadCarrera dificultad) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        double margen = Math.max(145.0, dif.margenRepentino);
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
            if (ref != null && ox < ref.x + margen) {
                ox = ref.x + margen;
            }
            double[] pos = asegurarPosicionLibre(ox, oy, rep.radio, pelea, c0, c1);
            rep.trampa = pelea.arena.agregarRepentina(rep.tipo, pos[0], pos[1], rep.radio,
                    rep.solido, rep.ciclo, rep.activo, rep.resistencia);
            rep.aparecido = true;
            pelea.suceso(Suceso.Tipo.TRAMPA, -1, pos[0], pos[1], 0, rep.radio, "repentino_" + rep.tipo);
        }
    }

    private static double[] asegurarPosicionLibre(double ox, double oy, double radio, Pelea pelea, Cuerpo c0, Cuerpo c1) {
        double maxX = Math.max(CarreraDeObstaculos.SALIDA_X + 300, pelea.ancho - 260);
        double x = Math.max(CarreraDeObstaculos.SALIDA_X + 160, Math.min(maxX, ox));
        double y = Math.max(95, Math.min(pelea.alto - 95, oy));
        for (int intento = 0; intento < 8; intento++) {
            boolean libre0 = c0 == null || c0.fuera || Math.hypot(x - c0.x, y - c0.y) >= c0.radio + radio + 120;
            boolean libre1 = c1 == null || c1.fuera || Math.hypot(x - c1.x, y - c1.y) >= c1.radio + radio + 120;
            if (libre0 && libre1) break;
            x = Math.min(maxX + 5, x + 90);
            y = y > pelea.alto * 0.5 ? Math.max(95, y - 85) : Math.min(pelea.alto - 95, y + 85);
        }
        return new double[]{x, y};
    }

    /** Crea los robots defectuosos/averiados que entorpecen distintos tramos de la pista 6×. */
    public static List<DefectuosoEnPista> crearRobotsDefectuosos(double ancho, double alto, long semilla) {
        return crearRobotsDefectuosos(ancho, alto, semilla, DificultadCarrera.porDefecto());
    }

    /** Crea los robots defectuosos/averiados escalados según la dificultad de la carrera (subnivel I). */
    public static List<DefectuosoEnPista> crearRobotsDefectuosos(double ancho, double alto, long semilla, DificultadCarrera dificultad) {
        return crearRobotsDefectuosos(ancho, alto, semilla, dificultad, DificultadCarrera.SUBNIVEL_MIN);
    }

    /** Crea los robots defectuosos/averiados escalados según la dificultad y el subnivel (1..3) de la carrera. */
    public static List<DefectuosoEnPista> crearRobotsDefectuosos(double ancho, double alto, long semilla,
            DificultadCarrera dificultad, int subnivel) {
        DificultadCarrera dif = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
        int extra = DificultadCarrera.normalizarSubnivel(subnivel) - 1;
        Random r = new Random(semilla * 97L + 41L + dif.ordinal() * 313L + extra * 71L);
        List<DefectuosoEnPista> lista = new ArrayList<>();
        int cantidad = dif.cantidadDefectuosos + extra;
        int cadencia = Math.max(5, dif.cadenciaDisparoDefectuosos - extra * 2);
        int movilesObjetivo = Math.max(1, (int) Math.round(cantidad * dif.proporcionDefectuososMoviles));
        double inicioX = ancho * 0.13;
        double finX = ancho * 0.89;
        for (int i = 0; i < cantidad; i++) {
            boolean inmovil = i >= movilesObjetivo;
            double frac = cantidad <= 1 ? 0.5 : (double) i / (cantidad - 1);
            double x = inicioX + (finX - inicioX) * frac + jitter(r, 42);
            double y = alto * (0.22 + (i % 3) * 0.28) + jitter(r, 25);
            Montaje m = montajeDefectuoso(dif, i);
            Fabrica fab = fabricaDefectuoso(inmovil, cadencia);
            Combatiente c = new Combatiente("Averiado-" + (i + 1), m, fab);
            c.computadora = true;
            c.defectuoso = true;
            c.escalaBlindaje = dif.escalaBlindajeDefectuosos;
            c.escalaDano = dif.escalaDanoDefectuosos;
            for (int k = 0; k < 6; k++) {
                c.abollar(k % 4, r, 14 + r.nextDouble() * 12, k % 2 == 0);
            }
            lista.add(new DefectuosoEnPista(c, x, y, inmovil));
        }
        return lista;
    }

    private static Montaje montajeDefectuoso(DificultadCarrera dif, int indice) {
        List<Modelo> todos = Modelo.todos();
        Modelo modelo = Modelo.de("cobalto");
        if (dif.ordinal() >= DificultadCarrera.SENIOR.ordinal() && !todos.isEmpty()) {
            modelo = todos.get(Math.floorMod(indice * 3 + dif.ordinal(), todos.size()));
        } else if (dif == DificultadCarrera.JUNIOR && todos.size() > 1 && indice % 2 == 1) {
            modelo = todos.get(1);
        }
        Montaje m = new Montaje(modelo);
        for (Pieza p : CatalogoDePiezas.delJuego().de(Pieza.Ranura.BRAZO)) {
            if (p.arma() == Pieza.Arma.BALA && p.valor("dano") > 0) {
                m.poner(Montaje.Hueco.IZQUIERDA, p);
                break;
            }
        }
        return m;
    }

    private static Fabrica fabricaDefectuoso(boolean inmovil, int cadenciaDisparo) {
        return new Fabrica() {
            @Override
            public RobotBase crear() {
                return new RobotDefectuoso(inmovil, cadenciaDisparo);
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

    /** Robot averiado en pista: gira a trompicones o deambula sin rumbo y dispara según la dificultad. */
    public static final class RobotDefectuoso extends Metajavbot {

        private final boolean inmovil;
        private final int cadenciaDisparo;
        private double rumboErratico;
        private int paso;

        /** Crea un robot defectuoso con cadencia intermedia por defecto. */
        public RobotDefectuoso(boolean inmovil) {
            this(inmovil, DificultadCarrera.porDefecto().cadenciaDisparoDefectuosos);
        }

        /** Crea un robot defectuoso inmóvil a trompicones o errático con la cadencia de disparo indicada. */
        public RobotDefectuoso(boolean inmovil, int cadenciaDisparo) {
            this.inmovil = inmovil;
            this.cadenciaDisparo = Math.max(5, cadenciaDisparo);
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
                if (t % 16 == 0) {
                    rumboErratico = azar().nextDouble() * 360 - 180;
                }
                if (y() < 110) rumboErratico = 75;
                else if (y() > altoArena() - 110) rumboErratico = -75;
                girarHacia(rumboErratico);
                avanzar(cadenciaDisparo <= 11 ? 56 : 42);
            }
            if (t % Math.max(6, cadenciaDisparo - 4) == 0) {
                apuntarHacia(azar().nextDouble() * 360 - 180);
            }
            if (t % cadenciaDisparo == 0) {
                atacar();
            }
        }
    }

    /** Robot rival de la máquina para pista 6×: escala velocidad, evasión, puntería e impulsos por dificultad y subnivel. */
    public static final class CorredorMaquina extends Metajavbot {

        private final DificultadCarrera dificultad;
        private final int subnivel;
        private int paso;

        /** Crea un corredor de la máquina con dificultad intermedia por defecto. */
        public CorredorMaquina() {
            this(DificultadCarrera.porDefecto(), DificultadCarrera.SUBNIVEL_MIN);
        }

        /** Crea un corredor de la máquina adaptado al nivel de dificultad elegido (subnivel I). */
        public CorredorMaquina(DificultadCarrera dificultad) {
            this(dificultad, DificultadCarrera.SUBNIVEL_MIN);
        }

        /** Crea un corredor de la máquina adaptado al nivel de dificultad y subnivel (1..3) elegidos. */
        public CorredorMaquina(DificultadCarrera dificultad, int subnivel) {
            this.dificultad = dificultad == null ? DificultadCarrera.porDefecto() : dificultad;
            this.subnivel = DificultadCarrera.normalizarSubnivel(subnivel);
        }

        @Override
        public void cadaTurno() {
            int t = paso++;
            double rumboObjetivo = buscarImpulso();
            Obstaculo amenaza = buscarAmenaza();
            boolean disparo = false;
            int cadencia = dificultad == DificultadCarrera.NOOB ? (subnivel >= 3 ? 3 : 4)
                    : dificultad == DificultadCarrera.JUNIOR ? 2 : 1;
            if (amenaza != null) {
                boolean destructible = amenaza.tipo().equals("barril") || amenaza.tipo().equals("chimenea");
                if (destructible && (t % cadencia == 0)) {
                    apuntarConError(amenaza.angulo());
                    atacar();
                    disparo = true;
                    if (amenaza.distancia() < 105) rumboObjetivo = elegirDesvio(amenaza.y());
                } else {
                    rumboObjetivo = elegirDesvio(amenaza.y());
                }
            }
            Enemigo blanco = buscarEnemigo();
            if (!disparo && blanco != null && (t % cadencia == 0)) {
                apuntarConError(blanco.angulo());
                atacar();
                if (blanco.distancia() < 120 && Math.abs(normalizar(blanco.angulo())) < 55) {
                    rumboObjetivo = elegirDesvio(blanco.y());
                }
            } else if (!disparo) {
                apuntarHacia(0);
            }
            if (y() < 95 && rumboObjetivo < 0) rumboObjetivo = 28;
            if (y() > altoArena() - 95 && rumboObjetivo > 0) rumboObjetivo = -28;
            girarHacia(rumboObjetivo);
            avanzar(dificultad.avanceRival * (1.0 + (subnivel - 1) * 0.05));
        }

        private void apuntarConError(double angulo) {
            double err = dificultad.errorPunteriaRival * Math.max(0.5, 1.0 - (subnivel - 1) * 0.15);
            double desvio = err <= 0 ? 0 : (azar().nextDouble() * 2 - 1) * err;
            apuntarHacia(angulo + desvio);
        }

        private double buscarImpulso() {
            Obstaculo mejor = null;
            double dist = 240 + dificultad.nivelRival * 220;
            boolean necesitaCurar = blindaje() < 75;
            for (Obstaculo o : obstaculos()) {
                boolean esBono = o.tipo().equals("impulso") || (necesitaCurar && o.tipo().equals(Arena.TIPO_REPARACION));
                if (!esBono || o.x() < x() - 20) continue;
                if (o.distancia() < dist && Math.abs(normalizar(o.angulo())) < 58) {
                    dist = o.distancia();
                    mejor = o;
                }
            }
            return mejor == null ? 0 : normalizar(mejor.angulo());
        }

        private Obstaculo buscarAmenaza() {
            Obstaculo amenaza = null;
            double mejorDist = dificultad.alcanceEvasionRival + (subnivel - 1) * 18.0;
            for (Obstaculo o : obstaculos()) {
                if (o.tipo().equals("impulso") || o.tipo().equals(Arena.TIPO_REPARACION)
                        || o.tipo().equals(Arena.TIPO_MONEDA_EXTRA) || o.tipo().equals(Arena.TIPO_RAMPA)) continue;
                boolean mortal = o.tipo().equals(Arena.TIPO_TRAMPA_MORTAL);
                double alcance = mortal ? mejorDist + 70 : mejorDist;
                double diff = Math.abs(normalizar(o.angulo()));
                if (o.distancia() < alcance && diff < (mortal ? 62 : 50)) {
                    mejorDist = o.distancia();
                    amenaza = o;
                }
            }
            return amenaza;
        }

        private Enemigo buscarEnemigo() {
            Enemigo mejor = null;
            double dist = 300 + dificultad.nivelRival * 260;
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
