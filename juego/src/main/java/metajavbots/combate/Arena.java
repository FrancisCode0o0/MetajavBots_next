package metajavbots.combate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import metajavbots.api.Cosa;

/**
 * ARENA — Las trampas y los objetos de una arena, como en las luchas de
 * robots de la tele: pinchos que salen del suelo, sierras en las esquinas, un
 * imán, un foso, barriles, un carrito que va y viene… y lo que agarra el
 * suelo (en el hielo se resbala). Las arenas de antes no tienen nada
 * (NINGUNA): pelean igual que siempre. Los robots las ven con obstaculos().
 */
public final class Arena {

    /** Una trampa o un objeto: dónde está, lo que ocupa y lo que hace. */
    public static final class Trampa {
        final int id;
        public final String tipo;
        public double x, y;
        public final double radio;
        public final boolean solido;
        /** Si puede romperse con proyectiles, láseres, golpes o choques (barriles y chimeneas). */
        public final boolean destructible;
        /** Resistencia máxima del obstáculo destructible (0 si no se puede romper). */
        public final double resistenciaMax;
        /** Si permanece oculto en el mapa general inicial hasta ser detectado por sensores en carrera. */
        public final boolean ocultoEnMapa;
        /** Si apareció repentinamente durante la carrera (no estaba en turno 0). */
        public final boolean repentino;
        private boolean reveladoEnCarrera;
        /** Turnos restantes de señal visual de aparición repentina (0 cuando ya está asentado). */
        private int aparicion;
        private double resistencia;
        private boolean destruida;
        /** Turnos breves de restos visibles tras romperse (ya no colisiona ni aparece en obstaculos()). */
        private int restos;
        /** Para lo que se mueve: su velocidad (vx, vy); para lo que va a ratos, su ciclo y su fase (en turnos). */
        double vx, vy;
        final int ciclo, activo;
        int fase;
        /** Los charcos (aceite, pegamento) se secan: los turnos que les quedan (-1, para siempre). */
        int vida = -1;

        Trampa(int id, String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo, double resistenciaMax) {
            this(id, tipo, x, y, radio, solido, ciclo, activo, resistenciaMax, false, false);
        }

        Trampa(int id, String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo, double resistenciaMax,
                boolean ocultoEnMapa) {
            this(id, tipo, x, y, radio, solido, ciclo, activo, resistenciaMax, ocultoEnMapa, false);
        }

        Trampa(int id, String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo, double resistenciaMax,
                boolean ocultoEnMapa, boolean repentino) {
            this.id = id;
            this.tipo = tipo;
            this.x = x;
            this.y = y;
            this.radio = radio;
            this.solido = solido;
            this.ciclo = ciclo;
            this.activo = activo;
            this.destructible = resistenciaMax > 0;
            this.resistenciaMax = Math.max(0, resistenciaMax);
            this.resistencia = this.resistenciaMax;
            this.ocultoEnMapa = ocultoEnMapa;
            this.repentino = repentino;
            this.reveladoEnCarrera = !ocultoEnMapa;
            this.aparicion = repentino ? 36 : 0;
        }

        /** ¿Hace daño ahora? Los pinchos y el rayo, solo en su rato; las sierras y el carrito, siempre. */
        public boolean peligroso() {
            if (destruida) return false;
            switch (tipo) {
                case "pinchos": case "rayo": return fase % ciclo < activo;
                case "pegamento": return true;
                case "sierra": case "carrito": case "foso": return true;
                default: return false;
            }
        }

        /** Resistencia actual del obstáculo (0 si ya se rompió o no es destructible). */
        public double resistencia() {
            return resistencia;
        }

        /** Proporción de resistencia restante en [0, 1] (1 si no es destructible). */
        public double proporcionResistencia() {
            return resistenciaMax > 0 ? Math.max(0, resistencia / resistenciaMax) : 1;
        }

        /** Si este obstáculo ya ha sido destruido. */
        public boolean destruida() {
            return destruida;
        }

        /** Si este obstáculo sigue activo en la arena. */
        public boolean activa() {
            return !destruida;
        }

        /** Si este obstáculo ya es visible en la pista en directo (los no ocultos siempre lo son). */
        public boolean revelado() {
            return reveladoEnCarrera;
        }

        /** Marca este obstáculo oculto como detectado durante la carrera. */
        void revelar() {
            reveladoEnCarrera = true;
        }

        /** Turnos restantes de restos breves en el suelo tras destruirse. */
        public int restos() {
            return restos;
        }

        /** Turnos restantes de señal visual de aparición repentina (0 si ya está asentado). */
        public int aparicion() {
            return aparicion;
        }

        /** De 0 a 1, lo que han salido los pinchos (para dibujarlos subiendo y bajando). */
        public float salida() {
            if (ciclo <= 0) return 1;
            int f = fase % ciclo;
            if (f < activo) return Math.min(1, Math.min(f, activo - f) / 6f);
            return 0;
        }

        public int fase() {
            return fase;
        }

        /** Los turnos que le quedan a un charco (-1 si no se seca). */
        public int vida() {
            return vida;
        }

        public double rumbo() {
            return vx == 0 && vy == 0 ? 0 : Math.toDegrees(Math.atan2(vy, vx));
        }

        public double velocidad() {
            return Math.hypot(vx, vy);
        }
    }

    /** Lo que ve un robot de una trampa: sus datos y dónde queda desde él. */
    private static final class Vista implements Cosa {
        private final Trampa t;
        private final double distancia, angulo;

        Vista(Cuerpo yo, Trampa t) {
            this.t = t;
            distancia = Math.hypot(t.x - yo.x, t.y - yo.y);
            angulo = Math.toDegrees(Math.atan2(t.y - yo.y, t.x - yo.x));
        }

        @Override
        public String tipo() {
            return t.tipo;
        }

        @Override
        public double x() {
            return t.x;
        }

        @Override
        public double y() {
            return t.y;
        }

        @Override
        public double radio() {
            return t.radio;
        }

        @Override
        public double distancia() {
            return distancia;
        }

        @Override
        public double angulo() {
            return angulo;
        }

        @Override
        public boolean peligroso() {
            return t.peligroso();
        }

        @Override
        public boolean solido() {
            return t.solido;
        }

        @Override
        public double rumbo() {
            return t.rumbo();
        }

        @Override
        public double velocidad() {
            return t.velocidad();
        }
    }

    /** La arena de siempre: sin trampas y con el suelo que agarra. Es compartida: no se le añade nada (Pelea.arenaPropia). */
    public static final Arena NINGUNA = new Arena(1);

    /** Una arena sin nada, propia de una pelea (para echarle charcos encima). */
    static Arena vacia() {
        return new Arena(1);
    }

    /** Un charco que dura un rato: aceite (resbala) o pegamento (casi no deja moverse). */
    void charco(String tipo, double x, double y, double radio, int turnos) {
        Trampa t = poner(tipo, x, y, radio, false, 0, 0);
        t.vida = turnos;
    }

    /** Lo que agarra el suelo en ese sitio: el de la arena, y en el aceite casi nada. */
    double agarreEn(double x, double y) {
        double a = agarre;
        for (Trampa t : trampas) if (!t.destruida && t.tipo.equals("aceite") && Math.hypot(t.x - x, t.y - y) < t.radio) a = Math.min(a, 0.18);
        return a;
    }

    static final double RESISTENCIA_BARRIL = 30;
    static final double RESISTENCIA_CHIMENEA = 50;
    private static final int TURNOS_RESTOS = 45;
    /** Turnos que dura el impulso de velocidad recogido en pista. */
    public static final int TURNOS_IMPULSO = 90;
    /** Multiplicador de velocidad al recoger un impulso en pista. */
    public static final double FACTOR_IMPULSO = 1.75;
    /** Alcance mínimo de detección por radar/sensores para obstáculos ocultos de la carrera. */
    public static final double ALCANCE_SENSOR_OCULTOS = 260;

    private final List<Trampa> trampas = new ArrayList<>();
    private int siguienteId;
    /** Lo que agarra el suelo: 1 normal; menos, se resbala (cuesta frenar y cambiar de dirección). */
    public final double agarre;
    /** Cuándo puede volver a hacer daño cada trampa a cada robot (para que no pegue en todos los turnos). */
    private final java.util.Map<Long, Integer> espera = new java.util.HashMap<>();

    private Arena(double agarre) {
        this.agarre = agarre;
    }

    public List<Trampa> trampas() {
        return Collections.unmodifiableList(trampas);
    }

    /** Lo que ve ese robot: una vista de cada trampa activa (las ocultas solo al entrar en alcance de sensores/radar). */
    List<Cosa> vistas(Cuerpo c) {
        if (trampas.isEmpty()) return List.of();
        metajavbots.piezas.Pieza cabeza = c.combatiente.montaje.pieza(metajavbots.piezas.Montaje.Hueco.CABEZA);
        double radar = cabeza == null ? 0 : cabeza.valor("radar");
        double alcanceSensor = Math.max(ALCANCE_SENSOR_OCULTOS, radar * 0.45);
        if (c.pulsoTactico > 0) alcanceSensor *= 1.5;
        List<Cosa> l = new ArrayList<>(trampas.size());
        for (Trampa t : trampas) {
            if (t.destruida) continue;
            if (t.ocultoEnMapa) {
                double d = Math.hypot(t.x - c.x, t.y - c.y);
                if (d > alcanceSensor) continue;
                t.revelar();
            }
            l.add(new Vista(c, t));
        }
        return l;
    }

    /** La arena de ese escenario (CHATARRERIA, COCINA, HIELO, TEJADO); los demás, NINGUNA. */
    public static Arena de(String escenario, double ancho, double alto) {
        double cx = ancho / 2, cy = alto / 2;
        switch (escenario) {
            case "CHATARRERIA": {
                Arena a = new Arena(1);
                a.ponerDestructible("barril", ancho * 0.22, alto * 0.7, 30, RESISTENCIA_BARRIL);
                a.ponerDestructible("barril", ancho * 0.78, alto * 0.3, 30, RESISTENCIA_BARRIL);
                a.ponerDestructible("barril", ancho * 0.25, alto * 0.25, 26, RESISTENCIA_BARRIL);
                a.poner("pinchos", cx - 170, cy, 55, false, 150, 60);
                a.poner("pinchos", cx + 170, cy, 55, false, 150, 60).fase = 75;
                a.poner("iman", cx, cy, 200, false, 0, 0);
                return a;
            }
            case "COCINA": {
                Arena a = new Arena(1);
                for (double[] e : new double[][]{{0, 0}, {ancho, 0}, {0, alto}, {ancho, alto}}) a.poner("sierra", e[0], e[1], 80, false, 0, 0);
                Trampa carrito = a.poner("carrito", 120, cy, 36, true, 0, 0);
                carrito.vx = 2.6;
                a.poner("pinchos", cx, alto * 0.22, 60, false, 120, 45);
                a.poner("pinchos", cx, alto * 0.78, 60, false, 120, 45).fase = 60;
                return a;
            }
            case "HIELO": {
                Arena a = new Arena(0.3);
                a.poner("foso", cx, cy, 70, false, 0, 0);
                a.ponerDestructible("barril", ancho * 0.2, alto * 0.5, 28, RESISTENCIA_BARRIL);
                a.ponerDestructible("barril", ancho * 0.8, alto * 0.5, 28, RESISTENCIA_BARRIL);
                return a;
            }
            case "TEJADO": {
                Arena a = new Arena(0.6);
                a.ponerDestructible("chimenea", ancho * 0.3, alto * 0.65, 40, RESISTENCIA_CHIMENEA);
                a.ponerDestructible("chimenea", ancho * 0.7, alto * 0.35, 40, RESISTENCIA_CHIMENEA);
                Trampa rayo = a.poner("rayo", cx, cy, 70, false, 200, 30);
                rayo.vx = 1.4;
                rayo.vy = 1.0;
                return a;
            }
            default:
                return NINGUNA;
        }
    }

    private Trampa poner(String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo) {
        return poner(tipo, x, y, radio, solido, ciclo, activo, false);
    }

    private Trampa poner(String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo, boolean ocultoEnMapa) {
        Trampa t = new Trampa(siguienteId++, tipo, x, y, radio, solido, ciclo, activo, 0, ocultoEnMapa);
        trampas.add(t);
        return t;
    }

    private Trampa ponerDestructible(String tipo, double x, double y, double radio, double resistencia) {
        return ponerDestructible(tipo, x, y, radio, resistencia, false);
    }

    private Trampa ponerDestructible(String tipo, double x, double y, double radio, double resistencia, boolean ocultoEnMapa) {
        Trampa t = new Trampa(siguienteId++, tipo, x, y, radio, true, 0, 0, resistencia, ocultoEnMapa);
        trampas.add(t);
        return t;
    }

    static Arena crearParaCarrera(double agarre) {
        return new Arena(agarre);
    }

    Trampa agregarTrampa(String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo, boolean ocultoEnMapa) {
        return poner(tipo, x, y, radio, solido, ciclo, activo, ocultoEnMapa);
    }

    Trampa agregarDestructible(String tipo, double x, double y, double radio, double resistencia, boolean ocultoEnMapa) {
        return ponerDestructible(tipo, x, y, radio, resistencia, ocultoEnMapa);
    }

    Trampa agregarImpulso(double x, double y, double radio) {
        return poner("impulso", x, y, radio, false, 0, 0, false);
    }

    Trampa agregarRepentina(String tipo, double x, double y, double radio, boolean solido, int ciclo, int activo, double resistencia) {
        Trampa t = new Trampa(siguienteId++, tipo, x, y, radio, solido, ciclo, activo, resistencia, false, true);
        trampas.add(t);
        return t;
    }

    /** Crea una pista propia para la Carrera de obstáculos con obstáculos visibles, ocultos, destructibles, indestructibles e impulsos. */
    public static Arena carreraDeObstaculos(double ancho, double alto, long semilla) {
        return PistaDeCarrera.generarArena(ancho, alto, semilla, PistaDeCarrera.siguienteArena(null, semilla));
    }

    /** Crea la pista de Carrera de obstáculos con la estética y agarre del escenario indicado. */
    public static Arena carreraDeObstaculos(double ancho, double alto, long semilla, String escenario) {
        return PistaDeCarrera.generarArena(ancho, alto, semilla, escenario);
    }

    /** Aplica daño a un obstáculo destructible; al llegar a 0 lo destruye y avisa con OBSTACULO_ROTO. */
    boolean danar(Trampa t, Pelea p, int quien, double dano, double ix, double iy, String arma) {
        if (t == null || !t.destructible || t.destruida || !Double.isFinite(dano) || dano <= 0) return false;
        Cuerpo atacante = p.cuerpo(quien);
        if (atacante != null) dano *= atacante.combatiente.escalaDano;
        dano = Armas.danoEfectivoSeguro(dano);
        if (dano <= 0) return false;
        t.resistencia = Math.max(0, t.resistencia - dano);
        if (t.resistencia <= 0) {
            t.destruida = true;
            t.restos = TURNOS_RESTOS;
            p.suceso(Suceso.Tipo.OBSTACULO_ROTO, quien, t.x, t.y, 0, t.radio, t.tipo);
            return true;
        }
        p.suceso(Suceso.Tipo.TRAMPA, quien, ix, iy, 0, dano, t.tipo);
        return false;
    }

    /** Comprueba si un proyectil impacta contra un obstáculo destructible o bloque sólido; devuelve si el proyectil se detuvo. */
    boolean impactoProyectil(Pelea p, Proyectil pr) {
        for (Trampa t : trampas) {
            if (t.destruida) continue;
            if (!t.destructible && !t.tipo.equals("bloque")) continue;
            if (Math.hypot(t.x - pr.x, t.y - pr.y) <= t.radio) {
                pr.vivo = false;
                if (t.destructible) {
                    if (pr.misil) p.suceso(Suceso.Tipo.EXPLOSION, pr.dueno, pr.x, pr.y, 0, pr.dano, "misil");
                    danar(t, p, pr.dueno, pr.dano, pr.x, pr.y, pr.misil ? "misil" : "bala");
                } else {
                    p.suceso(Suceso.Tipo.TRAMPA, pr.dueno, pr.x, pr.y, 0, 0, t.tipo);
                }
                return true;
            }
        }
        return false;
    }

    /** El primer obstáculo destructible activo en la línea (x, y) + t*(dx, dy) hasta «alcance». */
    Trampa primeroEnLaLinea(double x, double y, double dx, double dy, double alcance, double[] fin) {
        Trampa primero = null;
        fin[0] = alcance;
        for (Trampa t : trampas) {
            if (!t.destructible || t.destruida) continue;
            double ox = t.x - x, oy = t.y - y;
            double proy = ox * dx + oy * dy;
            double perp = Math.abs(ox * dy - oy * dx);
            if (proy <= 0 || perp > t.radio) continue;
            double entrada = Math.max(0, proy - Math.sqrt(t.radio * t.radio - perp * perp));
            if (entrada <= alcance && entrada < fin[0]) {
                fin[0] = entrada;
                primero = t;
            }
        }
        return primero;
    }

    /** El obstáculo destructible más cercano al alcance de un golpe cuerpo a cuerpo de c; null si no hay. */
    Trampa alAlcance(Cuerpo c, double alcance, double cono) {
        Trampa mejor = null;
        double mejorD = Double.MAX_VALUE;
        for (Trampa t : trampas) {
            if (!t.destructible || t.destruida) continue;
            double d = Math.hypot(t.x - c.x, t.y - c.y);
            double ang = Math.toDegrees(Math.atan2(t.y - c.y, t.x - c.x));
            if (d <= c.radio + t.radio + alcance && Math.abs(Pelea.normal(ang - c.mira)) <= cono / 2 && d < mejorD) {
                mejor = t;
                mejorD = d;
            }
        }
        return mejor;
    }

    /** Daña los obstáculos destructibles alcanzados por un cono de llamas. */
    void quemar(Pelea p, Cuerpo c, double alcance, double cono, double dano) {
        for (Trampa t : trampas) {
            if (!t.destructible || t.destruida) continue;
            double d = Math.hypot(t.x - c.x, t.y - c.y);
            double ang = Math.toDegrees(Math.atan2(t.y - c.y, t.x - c.x));
            if (d <= alcance + t.radio && Math.abs(Pelea.normal(ang - c.mira)) <= cono / 2 + 8) {
                danar(t, p, c.indice, dano, t.x, t.y, "llama");
            }
        }
    }

    /** Un turno: se mueve lo que se mueve y cada trampa hace lo suyo a los robots que la tocan. */
    void turno(Pelea p) {
        if (trampas.isEmpty()) return;
        for (Trampa t : trampas) {
            if (t.destruida) {
                if (t.restos > 0) t.restos--;
                continue;
            }
            t.fase++;
            if (t.aparicion > 0) t.aparicion--;
            if (t.vida > 0) t.vida--;
            mover(t, p);
        }
        trampas.removeIf(t -> t.vida == 0 || t.destruida && t.restos <= 0);
        for (int i = 0; i < p.combatientes.length; i++) {
            Cuerpo c = p.cuerpo(i);
            if (c == null || c.fuera) continue;
            for (int k = 0; k < trampas.size(); k++) {
                Trampa t = trampas.get(k);
                if (!t.destruida) efecto(p, c, t);
            }
        }
    }

    /** Lo que se mueve rebota en las paredes (y el rayo va de un lado a otro por el tejado). */
    private static void mover(Trampa t, Pelea p) {
        if (t.vx == 0 && t.vy == 0) return;
        t.x += t.vx;
        t.y += t.vy;
        if (t.x < t.radio || t.x > p.ancho - t.radio) t.vx = -t.vx;
        if (t.y < t.radio || t.y > p.alto - t.radio) t.vy = -t.vy;
        t.x = Math.max(t.radio, Math.min(p.ancho - t.radio, t.x));
        t.y = Math.max(t.radio, Math.min(p.alto - t.radio, t.y));
    }

    private void efecto(Pelea p, Cuerpo c, Trampa t) {
        double dx = c.x - t.x, dy = c.y - t.y, d = Math.hypot(dx, dy);
        boolean suelo = !c.vuela();
        if (t.tipo.equals("pegamento")) {
            // El pegamento casi no deja moverse a lo que pisa el suelo.
            if (suelo && d < t.radio) c.velocidad *= 0.35;
            return;
        }
        if (t.tipo.equals("aceite")) return;
        if (t.tipo.equals("impulso")) {
            if (!c.combatiente.defectuoso && d < t.radio + c.radio) {
                t.destruida = true;
                t.restos = 0;
                c.turbo = Math.max(c.turbo, TURNOS_IMPULSO);
                c.factorTurbo = Math.max(c.factorTurbo, FACTOR_IMPULSO);
                p.suceso(Suceso.Tipo.ESPECIAL, c.indice, t.x, t.y, c.rumbo, TURNOS_IMPULSO, "impulso");
            }
            return;
        }
        if (t.tipo.equals("iman")) {
            // El imán tira de todos los que pisan el suelo, más cuanto más cerca.
            if (suelo && d < t.radio && d > 1) {
                double f = 0.9 * (1 - d / t.radio);
                c.x -= dx / d * f;
                c.y -= dy / d * f;
            }
            return;
        }
        if (t.solido && d < t.radio + c.radio && d > 0.01) {
            // Lo sólido no se atraviesa: se aparta al robot (y, si iba rápido, se da un golpe y daña destructibles).
            double meter = t.radio + c.radio - d;
            c.x += dx / d * meter;
            c.y += dy / d * meter;
            double vel = Math.abs(c.velocidad);
            if (vel > 1.5 && listo(t.id, c, p, 20)) {
                p.danar(c, null, 0.5 + vel * 0.3, t.tipo, Math.toDegrees(Math.atan2(-dy, -dx)), false);
                p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 1, t.tipo);
                if (t.destructible) {
                    double danoChoque = Armas.danoChoqueAObstaculo(c.combatiente.montaje, c.cifras, vel);
                    danar(t, p, c.indice, danoChoque, t.x + dx / d * t.radio, t.y + dy / d * t.radio, "choque");
                }
            }
            c.velocidad *= 0.5;
        }
        if (!t.peligroso() || t.solido && !t.tipo.equals("carrito")) return;
        boolean toca = t.tipo.equals("foso") ? d < t.radio - c.radio * 0.3 : d < t.radio + c.radio * 0.6;
        if (!toca || !suelo && !t.tipo.equals("rayo")) return;
        switch (t.tipo) {
            case "foso":
                // Se cae: mucho daño, aturdido un rato y vuelve a salir por el borde más cercano.
                p.danar(c, null, 18, "foso", 0, false);
                p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 3, "foso");
                c.aturdido = Math.max(c.aturdido, 45);
                double ang = Math.atan2(dy, dx);
                c.x = t.x + Math.cos(ang) * (t.radio + c.radio + 40);
                c.y = t.y + Math.sin(ang) * (t.radio + c.radio + 40);
                c.velocidad = 0;
                c.avance = 0;
                break;
            case "carrito":
                if (listo(t.id, c, p, 25)) {
                    p.danar(c, null, 4, "carrito", Math.toDegrees(Math.atan2(-dy, -dx)), false);
                    p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 2, "carrito");
                }
                break;
            default:
                // Pinchos, sierras y rayo: daño a golpes, no en cada turno.
                if (listo(t.id, c, p, t.tipo.equals("sierra") ? 12 : 30)) {
                    double dano = t.tipo.equals("sierra") ? 2.5 : t.tipo.equals("rayo") ? 7 : 5;
                    p.danar(c, null, dano, t.tipo, Math.toDegrees(Math.atan2(-dy, -dx)), false);
                    p.suceso(Suceso.Tipo.TRAMPA, c.indice, c.x, c.y, 0, 2, t.tipo);
                }
                break;
        }
    }

    /** ¿Puede esa trampa volver a dañar a ese robot? (Y, si sí, apunta cuándo podrá la próxima vez.) */
    private boolean listo(int trampa, Cuerpo c, Pelea p, int turnos) {
        long clave = (long) trampa * 64 + c.indice;
        int ahora = p.turno() + p.ronda() * 100_000;
        Integer cuando = espera.get(clave);
        if (cuando != null && ahora < cuando) return false;
        espera.put(clave, ahora + turnos);
        return true;
    }
}
