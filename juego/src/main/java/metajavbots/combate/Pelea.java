package metajavbots.combate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import metajavbots.api.Puente;
import metajavbots.api.TopeDeCalculo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;

/**
 * PELEA — Dos robots, cuatro rondas. Cada ronda dura unos 40 segundos (1200
 * turnos) o hasta que a uno le rompen la cabeza. Entre ronda y ronda se para
 * (estado ENTRE_RONDAS) para que cada uno cambie su código y sus piezas. Gana
 * quien haya recibido menos daño en total; al final, el que pierde explota.
 *
 * Es solo lógica, sin dibujo: la misma semilla y los mismos robots dan la
 * misma pelea (las pruebas pelean cientos sin ventana). En cada turno:
 * 1) el código de cada robot (con su tope de pasos), 2) los ataques pedidos,
 * 3) el movimiento, los choques y los proyectiles.
 */
public final class Pelea {

    public enum Estado { ANTES, LUCHANDO, ENTRE_RONDAS, TERMINADA }

    public static final int RONDAS = 4;
    public static final int TURNOS_POR_SEGUNDO = 30;

    public final double ancho, alto;
    public final int turnosPorRonda;
    public final Combatiente[] combatientes;
    /** El equipo de cada combatiente: 0 y 1 en una pelea normal; uno distinto cada uno en un battle royale. */
    public final int[] equipo;
    private final long semilla;
    private final Random azar;
    private Estado estado = Estado.ANTES;
    private int ronda, turno;
    private Cuerpo[] cuerpos = new Cuerpo[0];
    public final List<Proyectil> proyectiles = new ArrayList<>();
    public final List<Rayo> rayos = new ArrayList<>();
    private final List<Suceso> sucesos = new ArrayList<>();
    private final List<Suceso> sucesosDelTurno = new ArrayList<>();
    /** El tiempo que puede pensar un robot en un turno (aparte de los pasos): sin esto, un cálculo enorme congelaría la pantalla. */
    public long nanosPorTurno = 30_000_000L;
    /** Si no es 0, el tope de pasos de todos (el modo especial «tope de cálculo»); si es 0, el de cada núcleo. */
    public long topeDePasos;
    /** Cuánto daño le llega a cada uno (1 = todo). Los jefes del modo especial ponen 0.1 si el código no usa su tema. */
    public final double[] resistencia;
    /** El ganador (0 o 1), -1 si empate, -2 si no ha acabado. */
    private int ganador = -2;

    /** Un rayo láser, para el dibujo (dura unos turnos). */
    public static final class Rayo {
        public final int dueno;
        public final double x1, y1, x2, y2;
        public final boolean acerto;
        public int vida = 8;

        Rayo(int dueno, double x1, double y1, double x2, double y2, boolean acerto) {
            this.dueno = dueno;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.acerto = acerto;
        }
    }

    public Pelea(Combatiente a, Combatiente b, long semilla) {
        this(a, b, semilla, 1000, 700, 1200);
    }

    /** Las rondas de esta pelea (4; 1 en las prácticas y los tutoriales). */
    public int rondas = RONDAS;

    public Pelea(Combatiente a, Combatiente b, long semilla, double ancho, double alto, int turnosPorRonda) {
        this(new Combatiente[]{a, b}, new int[]{0, 1}, semilla, ancho, alto, turnosPorRonda);
    }

    /** Una pelea de varios (battle royale: cada uno su equipo; por equipos: los del mismo número van juntos). */
    public Pelea(Combatiente[] cs, int[] equipos, long semilla) {
        this(cs, equipos, semilla, 1000, 700, 1200);
    }

    public Pelea(Combatiente[] cs, int[] equipos, long semilla, double ancho, double alto, int turnosPorRonda) {
        if (cs.length < 2 || equipos.length != cs.length) throw new IllegalArgumentException("hacen falta al menos dos, cada uno con su equipo");
        combatientes = cs.clone();
        equipo = equipos.clone();
        resistencia = new double[cs.length];
        java.util.Arrays.fill(resistencia, 1);
        this.semilla = semilla;
        this.azar = new Random(semilla);
        this.ancho = ancho;
        this.alto = alto;
        this.turnosPorRonda = turnosPorRonda;
    }

    // ================================================================ lo que se puede saber

    public Estado estado() {
        return estado;
    }

    public int ronda() {
        return ronda;
    }

    public int turno() {
        return turno;
    }

    public Cuerpo cuerpo(int i) {
        return i >= 0 && i < cuerpos.length ? cuerpos[i] : null;
    }

    /** Los sucesos del último turno (se vacían al dar el siguiente). */
    public List<Suceso> sucesos() {
        return Collections.unmodifiableList(sucesosDelTurno);
    }

    /** Todos los sucesos de la pelea (para el resumen y el locutor). */
    public List<Suceso> historia() {
        return Collections.unmodifiableList(sucesos);
    }

    public int ganador() {
        return ganador;
    }

    /** Lo que queda de ronda, de 0 a 1. */
    public double restoDeRonda() {
        return 1 - (double) turno / turnosPorRonda;
    }

    // ================================================================ las rondas

    /** Empieza la ronda siguiente (al principio y tras cada pausa entre rondas). */
    public void empezarRonda() {
        if (estado == Estado.LUCHANDO || estado == Estado.TERMINADA) return;
        ronda++;
        turno = 0;
        proyectiles.clear();
        rayos.clear();
        // Cada ronda, en un sitio distinto: uno a cada lado.
        double margen = 120;
        if (combatientes.length == 2) {
            double ya = margen + azar.nextDouble() * (alto - 2 * margen);
            double yb = margen + azar.nextDouble() * (alto - 2 * margen);
            boolean cambia = ronda % 2 == 0;
            double xa = cambia ? ancho - margen : margen, xb = cambia ? margen : ancho - margen;
            cuerpos = new Cuerpo[]{
                    new Cuerpo(this, 0, combatientes[0], xa, ya, cambia ? 180 : 0, semilla * 31 + ronda * 7),
                    new Cuerpo(this, 1, combatientes[1], xb, yb, cambia ? 0 : 180, semilla * 31 + ronda * 7 + 3)};
        } else {
            // Varios: en corro alrededor del centro, los del mismo equipo juntos, mirando al centro.
            int n = combatientes.length;
            Integer[] orden = new Integer[n];
            for (int i = 0; i < n; i++) orden[i] = i;
            java.util.Arrays.sort(orden, (p, q) -> Integer.compare(equipo[p], equipo[q]));
            double giro = azar.nextDouble() * 360;
            cuerpos = new Cuerpo[n];
            for (int k = 0; k < n; k++) {
                int i = orden[k];
                double a = Math.toRadians(giro + 360.0 * k / n);
                double x = ancho / 2 + Math.cos(a) * (ancho / 2 - margen), y = alto / 2 + Math.sin(a) * (alto / 2 - margen);
                cuerpos[i] = new Cuerpo(this, i, combatientes[i], x, y, normal(Math.toDegrees(a) + 180), semilla * 31 + ronda * 7 + i * 3);
            }
        }
        estado = Estado.LUCHANDO;
        sucesosDelTurno.clear();
        suceso(Suceso.Tipo.RONDA_EMPIEZA, -1, ancho / 2, alto / 2, 0, ronda, "");
        for (int i = 0; i < combatientes.length; i++) {
            Combatiente c = combatientes[i];
            c.informes.add(new Combatiente.Informe());
            prepararRobot(cuerpos[i]);
        }
    }

    /** Crea el robot si hace falta (código nuevo) y le avisa de que empieza la ronda. */
    private void prepararRobot(Cuerpo cu) {
        Combatiente c = cu.combatiente;
        if (c.fabrica == null) {
            c.robot = null;
            return;
        }
        if (c.robot == null || c.versionDelRobot != c.fabrica.version()) {
            Puente.contar(2_000_000, 500_000_000L);
            try {
                c.robot = c.fabrica.crear();
                c.versionDelRobot = c.fabrica.version();
            } catch (Throwable e) {
                c.robot = null;
                error(cu, e, "al crear el robot");
            } finally {
                Puente.dejarDeContar();
            }
        }
        if (c.robot == null) return;
        Puente.conectar(c.robot, cu);
        ejecutar(cu, () -> Puente.empezar(c.robot), 2_000_000);
    }

    /** Un turno de pelea. No hace nada si no se está luchando. */
    public void avanzar() {
        if (estado != Estado.LUCHANDO) return;
        sucesosDelTurno.clear();
        turno++;
        int n = cuerpos.length;
        // 1) Lo que ve cada uno (una foto del principio del turno): sus enemigos a la vista y sus compañeros.
        for (Cuerpo c : cuerpos) {
            List<Avistamiento> vistos = new ArrayList<>(1), companeros = new ArrayList<>(0);
            for (Cuerpo otro : cuerpos) {
                if (otro == c) continue;
                if (otro.equipo == c.equipo) {
                    if (!otro.fuera) companeros.add(new Avistamiento(c, otro));
                } else {
                    vistos.addAll(vistosPor(c, otro));
                }
            }
            if (vistos.size() > 1) vistos.sort((p, q) -> Double.compare(p.distancia(), q.distancia()));
            c.vistos = vistos;
            c.companeros = companeros;
        }
        // 2) El código de cada uno (empieza uno distinto cada turno, para que nadie vaya siempre primero).
        for (int k = 0; k < n; k++) pensar(cuerpos[(turno + k) % n]);
        // 3) Los ataques, el movimiento, los choques, los proyectiles.
        for (Cuerpo c : cuerpos) atacar(c, objetivo(c));
        for (Cuerpo c : cuerpos) mover(c);
        for (int i = 0; i < n; i++) for (int k = i + 1; k < n; k++) chocar(cuerpos[i], cuerpos[k]);
        for (Cuerpo c : cuerpos) efectosContinuos(c, objetivo(c));
        moverProyectiles();
        for (Rayo r : rayos) r.vida--;
        rayos.removeIf(r -> r.vida <= 0);
        // 4) ¿Acaba la ronda? Al acabarse el tiempo, o cuando solo queda en pie un equipo.
        if (turno >= turnosPorRonda || equiposEnPie() <= 1) acabarRonda();
    }

    private int equiposEnPie() {
        java.util.Set<Integer> e = new java.util.HashSet<>();
        for (Cuerpo c : cuerpos) if (!c.fuera) e.add(c.equipo);
        return e.size();
    }

    /** El enemigo al que apunta: el que queda más cerca de la mira (y, a igualdad, el más cercano). */
    Cuerpo objetivo(Cuerpo c) {
        Cuerpo mejor = null;
        double nota = Double.MAX_VALUE;
        for (Cuerpo e : cuerpos) {
            if (e == c || e.equipo == c.equipo || e.fuera) continue;
            double d = Math.hypot(e.x - c.x, e.y - c.y);
            double ang = Math.abs(normal(Math.toDegrees(Math.atan2(e.y - c.y, e.x - c.x)) - c.mira));
            double v = ang * 6 + d * 0.05;
            if (v < nota) {
                nota = v;
                mejor = e;
            }
        }
        if (mejor != null) return mejor;
        for (Cuerpo e : cuerpos) if (e != c && e.equipo != c.equipo) return e;
        return c;
    }

    /** Un aviso a los compañeros: les llega el turno siguiente (y se ve como un bocadillo). */
    void avisar(Cuerpo de, String mensaje) {
        for (Cuerpo c : cuerpos) if (c != de && c.equipo == de.equipo && c.avisos.size() < 20) c.avisos.add(new String[]{de.combatiente.nombre, mensaje});
        suceso(Suceso.Tipo.DECIR, de.indice, de.x, de.y, 0, 1, "» " + mensaje);
    }

    /** Los puntos de un equipo: el daño que han recibido entre todos (menos es mejor). */
    public double danoDelEquipo(int e) {
        double d = 0;
        for (int i = 0; i < combatientes.length; i++) if (equipo[i] == e) d += combatientes[i].danoTotalRecibido;
        return d;
    }

    /** El equipo ganador (-1 si empate, -2 si no ha acabado). */
    public int equipoGanador() {
        return ganador < 0 ? ganador : equipo[ganador];
    }

    private void acabarRonda() {
        for (Cuerpo c : cuerpos) {
            Combatiente.Informe inf = informe(c.indice);
            inf.turnos = turno;
            inf.fueraDeCombate = c.fuera;
            if (c.combatiente.robot != null) ejecutar(c, () -> Puente.finDeRonda(c.combatiente.robot), 500_000);
        }
        suceso(Suceso.Tipo.RONDA_ACABA, -1, ancho / 2, alto / 2, 0, ronda, "");
        if (ronda >= rondas) {
            estado = Estado.TERMINADA;
            // Gana el equipo que menos daño ha recibido; el ganador es su primer combatiente.
            java.util.Map<Integer, Double> puntos = new java.util.TreeMap<>();
            for (int e : equipo) puntos.put(e, danoDelEquipo(e));
            double mejor = Double.MAX_VALUE, segundo = Double.MAX_VALUE;
            int equipoMejor = -1;
            for (java.util.Map.Entry<Integer, Double> p : puntos.entrySet()) {
                if (p.getValue() < mejor) {
                    segundo = mejor;
                    mejor = p.getValue();
                    equipoMejor = p.getKey();
                } else if (p.getValue() < segundo) {
                    segundo = p.getValue();
                }
            }
            if (Math.abs(segundo - mejor) < 0.5) ganador = -1;
            else {
                ganador = 0;
                for (int i = 0; i < equipo.length; i++) if (equipo[i] == equipoMejor) {
                    ganador = i;
                    break;
                }
            }
            suceso(Suceso.Tipo.PELEA_ACABA, ganador, ancho / 2, alto / 2, 0, ganador, "");
        } else {
            estado = Estado.ENTRE_RONDAS;
        }
    }

    /** Termina la pelea ya (si el jugador se rinde): pierde quien se rinde. */
    public void rendirse(int quien) {
        if (estado == Estado.TERMINADA) return;
        estado = Estado.TERMINADA;
        ganador = 0;
        for (int i = 0; i < equipo.length; i++) if (equipo[i] != equipo[quien]) {
            ganador = i;
            break;
        }
        suceso(Suceso.Tipo.PELEA_ACABA, ganador, ancho / 2, alto / 2, 0, ganador, "rendicion");
    }

    public Combatiente.Informe informe(int i) {
        List<Combatiente.Informe> l = combatientes[i].informes;
        return l.isEmpty() ? new Combatiente.Informe() : l.get(l.size() - 1);
    }

    // ================================================================ el código del robot

    private void pensar(Cuerpo c) {
        if (c.fuera) return;
        if (c.tiempoDeBocadillo > 0) c.tiempoDeBocadillo--;
        Combatiente.Informe inf = informe(c.indice);
        if (c.aturdido > 0) {
            inf.turnosAturdido++;
            c.golpesRecibidos.clear();
            c.aciertos.clear();
            return;
        }
        if (c.deuda > 0) {
            c.deuda--;
            return;
        }
        Combatiente co = c.combatiente;
        if (co.robot == null) return;
        long tope = topeDePasos > 0 ? topeDePasos : (long) c.cifras.pasos;
        List<metajavbots.api.Impacto> golpes = new ArrayList<>(c.golpesRecibidos);
        List<metajavbots.api.Impacto> aciertos = new ArrayList<>(c.aciertos);
        c.golpesRecibidos.clear();
        c.aciertos.clear();
        boolean pared = c.chocoConPared;
        c.chocoConPared = false;
        metajavbots.api.Avistado choque = c.chocoCon;
        c.chocoCon = null;
        List<String[]> avisos = new ArrayList<>(c.avisos);
        c.avisos.clear();
        long pasos = ejecutar(c, () -> {
            for (metajavbots.api.Impacto g : golpes) Puente.golpeado(co.robot, g);
            for (metajavbots.api.Impacto g : aciertos) Puente.acierto(co.robot, g);
            if (pared) Puente.pared(co.robot);
            if (choque != null) Puente.choque(co.robot, choque);
            for (String[] a : avisos) Puente.equipo(co.robot, a[0], a[1]);
            for (Avistamiento v : c.vistos) Puente.ver(co.robot, v);
            Puente.turno(co.robot);
        }, tope);
        c.pasosDelTurno = pasos;
        inf.pasosTotales += pasos;
        inf.pasosMaximos = Math.max(inf.pasosMaximos, pasos);
        if (!c.vistos.isEmpty()) inf.turnosVistos++;
    }

    /** Ejecuta código del robot con su tope; devuelve los pasos dados. Nada de lo que haga el robot rompe la pelea. */
    private long ejecutar(Cuerpo c, Runnable r, long tope) {
        Puente.contar(tope, nanosPorTurno);
        metajavbots.codigo.Salida.capturar();
        try {
            r.run();
        } catch (TopeDeCalculo t) {
            Combatiente.Informe inf = informe(c.indice);
            inf.topes++;
            // Se pasa del tope: pierde los turnos siguientes, más cuanto más se pase (el código lento hace lento al robot).
            c.deuda = Math.min(20, c.deuda + 2 + inf.topes / 3);
            if (inf.topes <= 3 || inf.topes % 20 == 0) {
                c.combatiente.escribir(turno, "⏱ Tope de cálculo (" + tope + " pasos por turno): el robot se queda pensando " + c.deuda + " turnos.");
            }
            suceso(Suceso.Tipo.TOPE, c.indice, c.x, c.y, 0, c.deuda, "");
        } catch (StackOverflowError e) {
            error(c, e, "recursión sin final (StackOverflowError)");
        } catch (OutOfMemoryError e) {
            error(c, e, "sin memoria");
        } catch (Throwable e) {
            error(c, e, null);
        }
        // La traza (modo máquina contra máquina): qué líneas de su código se ejecutaron este turno.
        java.util.BitSet lineas = Puente.lineas();
        if (lineas != null && !lineas.isEmpty()) {
            c.lineasDelTurno = lineas;
            c.ultimaLinea = Puente.ultimaLinea();
        }
        // Lo que escribió con System.out va a su consola.
        String escrito = metajavbots.codigo.Salida.soltar();
        if (!escrito.isEmpty()) c.combatiente.escribir(turno, escrito.stripTrailing());
        return Puente.dejarDeContar();
    }

    private void error(Cuerpo c, Throwable e, String que) {
        Combatiente co = c.combatiente;
        Combatiente.Informe inf = informe(c.indice);
        inf.errores++;
        Throwable raiz = e;
        if (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null) raiz = e.getCause();
        int linea = -1;
        for (StackTraceElement s : raiz.getStackTrace()) {
            if (co.fabrica != null && co.fabrica.esSuya(s.getClassName())) {
                linea = s.getLineNumber();
                break;
            }
        }
        String texto = (que != null ? que + ": " : "") + raiz.getClass().getSimpleName()
                + (raiz.getMessage() != null ? ": " + raiz.getMessage() : "") + (linea > 0 ? " (línea " + linea + ")" : "");
        co.ultimoError = texto;
        co.lineaDelUltimoError = linea;
        if (inf.errores <= 5 || inf.errores % 50 == 0) co.escribir(turno, "⚠ " + texto);
        // Un error deja al robot un momento aturdido («se le cruzan los cables»).
        c.aturdido = Math.max(c.aturdido, 8);
        suceso(Suceso.Tipo.ERROR_CODIGO, c.indice, c.x, c.y, 0, linea, texto);
    }

    // ================================================================ ver

    private List<Avistamiento> vistosPor(Cuerpo yo, Cuerpo el) {
        List<Avistamiento> l = new ArrayList<>(1);
        if (el.fuera || yo.fuera) return l;
        double d = Math.hypot(el.x - yo.x, el.y - yo.y);
        Pieza cabeza = yo.piezaDe(Cuerpo.CABEZA);
        double radar = cabeza == null ? 400 : cabeza.valor("radar", 400);
        double cono = cabeza == null ? 50 : cabeza.valor("cono", 50);
        if (yo.parteRota(Montaje.CABEZA)) radar *= 0.4;
        double ang = Math.toDegrees(Math.atan2(el.y - yo.y, el.x - yo.x));
        boolean enCono = Math.abs(normal(ang - yo.mira)) <= cono / 2;
        boolean cerca = d <= 110 + el.radio;
        boolean ve = cerca || d <= radar && enCono;
        if (el.camuflaje > 0 && d > 150) ve = false;
        if (ve) l.add(new Avistamiento(yo, el));
        return l;
    }

    // ================================================================ atacar

    private void atacar(Cuerpo c, Cuerpo enemigo) {
        for (int brazo = 0; brazo < 3; brazo++) {
            if (!c.pedido[brazo]) continue;
            c.pedido[brazo] = false;
            if (c.fuera) continue;
            Pieza p = c.piezaDe(brazo);
            if (p == null) continue;
            c.ultimoAtaque = turno;
            c.brazoDelUltimoAtaque = brazo;
            if (brazo == Cuerpo.CABEZA) especial(c, enemigo, p);
            else arma(c, enemigo, p, brazo);
        }
    }

    double refuerzoDeEnfriamiento(Cuerpo c) {
        return c.combatiente.montaje.refuerzo("pasos") * 0.8;
    }

    private void arma(Cuerpo c, Cuerpo e, Pieza p, int brazo) {
        Combatiente.Informe inf = informe(c.indice);
        double sobre = c.sobrecarga > 0 ? c.factorSobrecarga : 1;
        double[] boca = boca(c, brazo);
        switch (p.arma()) {
            case LASER: {
                inf.ataque("laser");
                double alcance = p.valor("alcance", 500);
                double ang = c.mira;
                double dx = Math.cos(Math.toRadians(ang)), dy = Math.sin(Math.toRadians(ang));
                // El rayo se para en el primer robot que se cruza (justo en su chapa); a un compañero no le hace daño.
                double[] fin = {alcance};
                Cuerpo tocado = primeroEnLaLinea(c, boca[0], boca[1], dx, dy, alcance, fin);
                boolean da = tocado != null && tocado.equipo != c.equipo;
                double hx = boca[0] + dx * fin[0], hy = boca[1] + dy * fin[0];
                rayos.add(new Rayo(c.indice, boca[0], boca[1], hx, hy, da));
                suceso(Suceso.Tipo.LASER, c.indice, boca[0], boca[1], ang, p.valor("dano"), "laser");
                if (da) danar(tocado, c, p.valor("dano") * sobre, "laser", ang + 180, false, hx, hy);
                c.retroceso = 4;
                break;
            }
            case BALA: {
                int n = Math.max(1, (int) p.valor("perdigones", 1));
                double disp = p.valor("dispersion", 3) / c.cifras.precision;
                for (int i = 0; i < n; i++) {
                    inf.ataque("bala");
                    double ang = c.mira + (azar.nextDouble() * 2 - 1) * disp;
                    proyectiles.add(new Proyectil(c.indice, false, boca[0], boca[1], ang, p.valor("velocidad", 14),
                            p.valor("dano") * sobre, p.valor("alcance", 600), 0));
                }
                suceso(Suceso.Tipo.DISPARO, c.indice, boca[0], boca[1], c.mira, n, "bala");
                c.retroceso = 6;
                break;
            }
            case MISIL: {
                int n = Math.max(1, (int) p.valor("misiles", 1));
                for (int i = 0; i < n; i++) {
                    inf.ataque("misil");
                    double ang = c.mira + (n == 1 ? 0 : (i - (n - 1) / 2.0) * 14);
                    proyectiles.add(new Proyectil(c.indice, true, boca[0], boca[1], ang, p.valor("velocidad", 7),
                            p.valor("dano") * sobre, p.valor("alcance", 800), p.valor("giro", 4)));
                }
                suceso(Suceso.Tipo.MISIL, c.indice, boca[0], boca[1], c.mira, n, "misil");
                c.retroceso = 5;
                break;
            }
            case CUERPO:
                inf.ataque("cuerpo");
                golpear(c, p, sobre);
                break;
            case LLAMA:
                inf.ataque("llama");
                c.llama = (int) p.valor("duracion", 14);
                c.brazoDeLaLlama = brazo;
                suceso(Suceso.Tipo.LLAMA, c.indice, boca[0], boca[1], c.mira, c.llama, "llama");
                break;
            case ESCUDO:
                c.escudoBrazo = (int) p.valor("duracion", 45);
                c.bloqueoBrazo = p.valor("bloqueo", 0.6);
                c.conoEscudo = p.valor("cono", 120);
                c.brazoDelEscudo = brazo;
                suceso(Suceso.Tipo.ESPECIAL, c.indice, c.x, c.y, c.mira, c.escudoBrazo, "escudo_brazo");
                break;
            case REPARAR:
                reparar(c, p.valor("cura", 10));
                break;
            default:
                break;
        }
    }

    private void especial(Cuerpo c, Cuerpo e, Pieza p) {
        switch (p.especial()) {
            case ESCUDO:
                c.escudoCabeza = (int) p.valor("duracion", 90);
                c.bloqueoCabeza = p.valor("bloqueo", 0.6);
                break;
            case TURBO:
                c.turbo = (int) p.valor("duracion", 60);
                c.factorTurbo = p.valor("factor", 1.8);
                break;
            case REPARAR:
                reparar(c, p.valor("cura", 15));
                break;
            case CAMUFLAJE:
                c.camuflaje = (int) p.valor("duracion", 75);
                break;
            case PULSO: {
                // El pulso llega a todos los enemigos cercanos.
                for (Cuerpo otro : cuerpos) {
                    if (otro.equipo == c.equipo || otro.fuera) continue;
                    double d = Math.hypot(otro.x - c.x, otro.y - c.y);
                    if (d <= p.valor("alcance", 240)) {
                        aturdir(otro, (int) p.valor("aturde", 30));
                        danar(otro, c, 3, "pulso", Math.toDegrees(Math.atan2(c.y - otro.y, c.x - otro.x)), false);
                    }
                }
                // El pulso también borra los proyectiles enemigos cercanos.
                for (Proyectil pr : proyectiles) {
                    if (cuerpos[pr.dueno].equipo != c.equipo && Math.hypot(pr.x - c.x, pr.y - c.y) < p.valor("alcance", 240)) pr.vivo = false;
                }
                break;
            }
            case SOBRECARGA:
                c.sobrecarga = (int) p.valor("duracion", 90);
                c.factorSobrecarga = p.valor("factor", 1.5);
                break;
            default:
                return;
        }
        suceso(Suceso.Tipo.ESPECIAL, c.indice, c.x, c.y, c.mira, 0, p.especial().name().toLowerCase(java.util.Locale.ROOT));
    }

    private void reparar(Cuerpo c, double cura) {
        double total = 0;
        for (int i = 0; i < 4; i++) {
            if (c.parte[i] <= 0 && i == Montaje.CABEZA) continue;
            double falta = c.parteMax[i] - c.parte[i];
            double r = Math.min(falta, cura / 4 + (i == Montaje.PIERNAS ? 0 : 0));
            if (r > 0) {
                c.parte[i] += r;
                total += r;
            }
        }
        suceso(Suceso.Tipo.REPARACION, c.indice, c.x, c.y, 0, total, "");
    }

    private void aturdir(Cuerpo e, int turnos) {
        if (turnos <= 0 || e.fuera) return;
        e.aturdido = Math.max(e.aturdido, turnos);
        e.avance = 0;
        suceso(Suceso.Tipo.ATURDIDO, e.indice, e.x, e.y, 0, turnos, "");
    }

    /** Un golpe cuerpo a cuerpo: al enemigo que tenga delante y a su alcance (el más cercano), sea o no al que mira el código. */
    private void golpear(Cuerpo c, Pieza p, double sobre) {
        Cuerpo e = alAlcance(c, p.valor("alcance", 50), p.valor("cono", 70));
        // Un golpe cuerpo a cuerpo empuja un poco hacia delante.
        c.x += Math.cos(Math.toRadians(c.mira)) * 6;
        c.y += Math.sin(Math.toRadians(c.mira)) * 6;
        c.retroceso = -8;
        if (e == null) {
            suceso(Suceso.Tipo.FALLO_CUERPO, c.indice, c.x, c.y, c.mira, 0, p.id);
            return;
        }
        double ang = Math.toDegrees(Math.atan2(e.y - c.y, e.x - c.x));
        // Donde toca: en la chapa del otro, del lado del que pega.
        double hx = e.x - Math.cos(Math.toRadians(ang)) * e.radio * 0.85, hy = e.y - Math.sin(Math.toRadians(ang)) * e.radio * 0.85;
        suceso(Suceso.Tipo.GOLPE_CUERPO, c.indice, hx, hy, c.mira, p.valor("dano"), p.id);
        if (danar(e, c, p.valor("dano") * sobre * c.cifras.cuerpo, "cuerpo", ang + 180, true, hx, hy)) {
            int aturde = (int) p.valor("aturde");
            if (aturde > 0) aturdir(e, aturde);
            // Y lo echa para atrás.
            e.x += Math.cos(Math.toRadians(ang)) * 10;
            e.y += Math.sin(Math.toRadians(ang)) * 10;
        }
    }

    /**
     * El primer robot (que no sea c ni esté fuera) que cruza la línea que sale de (x, y) en la dirección (dx, dy), hasta
     * «alcance»; en fin[0] queda dónde toca su chapa (o el alcance si no toca a nadie).
     */
    public Cuerpo primeroEnLaLinea(Cuerpo c, double x, double y, double dx, double dy, double alcance, double[] fin) {
        Cuerpo primero = null;
        fin[0] = alcance;
        for (Cuerpo o : cuerpos) {
            if (o == c || o.fuera) continue;
            double ox = o.x - x, oy = o.y - y;
            double t = ox * dx + oy * dy;
            double perp = Math.abs(ox * dy - oy * dx);
            if (t <= 0 || perp > o.radio) continue;
            double entrada = Math.max(0, t - Math.sqrt(o.radio * o.radio - perp * perp));
            if (entrada <= alcance && entrada < fin[0]) {
                fin[0] = entrada;
                primero = o;
            }
        }
        return primero;
    }

    /** El enemigo más cercano que c tiene delante (en su cono) y al alcance de un golpe; null si no hay. */
    public Cuerpo alAlcance(Cuerpo c, double alcance, double cono) {
        Cuerpo mejor = null;
        double mejorD = Double.MAX_VALUE;
        for (Cuerpo o : cuerpos) {
            if (o == c || o.fuera || o.equipo == c.equipo || o.vuela() && !c.vuela()) continue;
            double d = Math.hypot(o.x - c.x, o.y - c.y);
            double ang = Math.toDegrees(Math.atan2(o.y - c.y, o.x - c.x));
            if (d <= c.radio + o.radio + alcance && Math.abs(normal(ang - c.mira)) <= cono / 2 && d < mejorD) {
                mejor = o;
                mejorD = d;
            }
        }
        return mejor;
    }

    /** Por dónde sale el ataque: la punta del brazo. */
    static double[] boca(Cuerpo c, int brazo) {
        double lado = brazo == Cuerpo.IZQUIERDA ? 1 : brazo == Cuerpo.DERECHA ? -1 : 0;
        double a = Math.toRadians(c.mira);
        double px = Math.cos(a) * c.radio * 0.9 - Math.sin(a) * lado * c.radio * 0.55;
        double py = Math.sin(a) * c.radio * 0.9 + Math.cos(a) * lado * c.radio * 0.55;
        return new double[]{c.x + px, c.y + py};
    }

    // ================================================================ hacer daño

    /** Le hace daño a e (que viene de «de», desde el ángulo «desde»). Devuelve si le llegó algo. */
    boolean danar(Cuerpo e, Cuerpo de, double dano, String arma, double desde, boolean cuerpoACuerpo) {
        return danar(e, de, dano, arma, desde, cuerpoACuerpo, e.x, e.y);
    }

    /** Lo mismo, con el punto donde pega (para que las chispas salgan ahí y no en el centro del robot). */
    boolean danar(Cuerpo e, Cuerpo de, double dano, String arma, double desde, boolean cuerpoACuerpo, double ix, double iy) {
        if (e.fuera || dano <= 0) return false;
        // El éter esquiva las balas y los misiles.
        if ((arma.equals("bala") || arma.equals("misil")) && azar.nextDouble() < e.cifras.esquiva) {
            suceso(Suceso.Tipo.ESQUIVA, e.indice, e.x, e.y, desde, dano, arma);
            return false;
        }
        dano *= resistencia[e.indice];
        double bloqueo = 0;
        if (e.escudoBrazo > 0 && Math.abs(normal(desde - e.mira)) <= e.conoEscudo / 2) bloqueo = e.bloqueoBrazo;
        if (e.escudoCabeza > 0) bloqueo = Math.max(bloqueo, e.bloqueoCabeza);
        if (bloqueo > 0) {
            suceso(Suceso.Tipo.BLOQUEO, e.indice, ix, iy, desde, dano * bloqueo, arma);
            dano *= 1 - bloqueo;
        }
        // Qué parte recibe el golpe.
        double[] pesos = cuerpoACuerpo ? new double[]{0.25, 0.3, 0.3, 0.15} : new double[]{0.18, 0.24, 0.24, 0.34};
        if (e.vuela()) pesos[Montaje.PIERNAS] *= 0.5;
        int parte = elegir(pesos);
        if (e.parte[parte] <= 0) {
            // Si ya está rota, el golpe va a otra que no lo esté.
            for (int k = 0; k < 4; k++) {
                int p = (parte + 1 + k) % 4;
                if (e.parte[p] > 0) {
                    parte = p;
                    break;
                }
            }
        }
        double antes = e.parte[parte];
        e.parte[parte] -= dano;
        double hecho = Math.min(dano, Math.max(0, antes));
        Combatiente ce = e.combatiente;
        ce.danoTotalRecibido += hecho;
        informe(e.indice).danoRecibido += hecho;
        informe(e.indice).danoPorArmaRecibido.merge(arma, hecho, Double::sum);
        if (de != null) {
            de.combatiente.danoTotalHecho += hecho;
            informe(de.indice).danoHecho += hecho;
            informe(de.indice).acierto(arma);
            de.aciertos.add(new ImpactoHecho(arma, hecho, desde + 180, parte));
        }
        ce.abollar(parte, azar, hecho, arma.equals("laser") || arma.equals("llama"));
        e.golpesRecibidos.add(new ImpactoHecho(arma, hecho, normal(desde), parte));
        e.ultimoGolpe = turno;
        e.sacudida = Math.min(10, 2 + hecho / 2);
        suceso(Suceso.Tipo.IMPACTO, e.indice, ix, iy, desde, hecho, arma);
        if (antes > 0 && e.parte[parte] <= 0) {
            e.parte[parte] = 0;
            suceso(Suceso.Tipo.PARTE_ROTA, e.indice, e.x, e.y, 0, parte, Montaje.PARTES[parte]);
            if (parte == Montaje.CABEZA) {
                e.fuera = true;
                e.velocidad = 0;
                suceso(Suceso.Tipo.FUERA_DE_COMBATE, e.indice, e.x, e.y, 0, 0, "");
            }
        }
        // Sin ninguna parte entera, también queda fuera.
        if (!e.fuera && e.blindajeQueQueda() <= 0) {
            e.fuera = true;
            suceso(Suceso.Tipo.FUERA_DE_COMBATE, e.indice, e.x, e.y, 0, 0, "");
        }
        return true;
    }

    private int elegir(double[] pesos) {
        double t = 0;
        for (double p : pesos) t += p;
        double r = azar.nextDouble() * t;
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r <= 0) return i;
        }
        return pesos.length - 1;
    }

    // ================================================================ moverse

    private void mover(Cuerpo c) {
        if (c.fuera) {
            c.velocidad *= 0.8;
            return;
        }
        c.energia = Math.min(c.cifras.energiaMax, c.energia + c.cifras.recarga);
        for (int i = 0; i < 3; i++) if (c.enfriamiento[i] > 0) c.enfriamiento[i]--;
        if (c.escudoBrazo > 0) c.escudoBrazo--;
        if (c.escudoCabeza > 0) c.escudoCabeza--;
        if (c.turbo > 0) c.turbo--;
        if (c.camuflaje > 0) c.camuflaje--;
        if (c.sobrecarga > 0) c.sobrecarga--;
        if (c.aturdido > 0) c.aturdido--;
        if (c.sacudida > 0) c.sacudida *= 0.8;
        c.retroceso *= 0.75;
        boolean piernasRotas = c.parteRota(Montaje.PIERNAS);
        double vmax = c.cifras.velocidad * (c.turbo > 0 ? c.factorTurbo : 1) * (piernasRotas ? 0.4 : 1);
        double gmax = c.cifras.giro * (piernasRotas ? 0.5 : 1);
        if (c.aturdido > 0) {
            vmax = 0;
            gmax = 0;
        }
        // Girar el cuerpo y la mira.
        double g = Math.max(-gmax, Math.min(gmax, c.giro));
        c.rumbo = normal(c.rumbo + g);
        c.giro -= g;
        double gm = Math.max(-14, Math.min(14, c.giroMira));
        if (c.aturdido > 0) gm = 0;
        c.mira = normal(c.mira + gm);
        c.giroMira -= gm;
        // Avanzar: acelera hacia lo que falta, y frena para llegar justo.
        double deseada = Math.max(-vmax, Math.min(vmax, c.avance));
        double acel = c.cifras.aceleracion * (Math.signum(deseada) != Math.signum(c.velocidad) || Math.abs(deseada) < Math.abs(c.velocidad) ? 2 : 1);
        if (c.velocidad < deseada) c.velocidad = Math.min(deseada, c.velocidad + acel);
        else if (c.velocidad > deseada) c.velocidad = Math.max(deseada, c.velocidad - acel);
        double rad = Math.toRadians(c.rumbo);
        c.x += Math.cos(rad) * c.velocidad;
        c.y += Math.sin(rad) * c.velocidad;
        c.avance -= c.velocidad;
        if (Math.abs(c.avance) < 0.01) c.avance = 0;
        c.paso += Math.abs(c.velocidad);
        // Las paredes.
        boolean choca = false;
        if (c.x < c.radio) { c.x = c.radio; choca = true; }
        if (c.x > ancho - c.radio) { c.x = ancho - c.radio; choca = true; }
        if (c.y < c.radio) { c.y = c.radio; choca = true; }
        if (c.y > alto - c.radio) { c.y = alto - c.radio; choca = true; }
        if (choca && Math.abs(c.velocidad) > 0.5) {
            double dano = 0.3 + Math.abs(c.velocidad) * 0.25;
            c.chocoConPared = true;
            suceso(Suceso.Tipo.PARED, c.indice, c.x, c.y, c.rumbo, dano, "");
            danar(c, null, dano, "pared", c.rumbo, false);
            c.velocidad = 0;
            c.avance = 0;
        }
    }

    private void chocar(Cuerpo a, Cuerpo b) {
        if (a.fuera || b.fuera) return;
        if (a.vuela() != b.vuela()) return;   // uno vuela por encima del otro
        double dx = b.x - a.x, dy = b.y - a.y;
        double d = Math.hypot(dx, dy), min = a.radio + b.radio;
        if (d >= min) return;
        if (d < 0.001) {
            dx = 1;
            dy = 0;
            d = 1;
        }
        double empuje = (min - d) / 2 + 0.5;
        double ux = dx / d, uy = dy / d;
        a.x -= ux * empuje;
        a.y -= uy * empuje;
        b.x += ux * empuje;
        b.y += uy * empuje;
        // El que va más deprisa y pesa más hace más daño.
        double va = Math.abs(a.velocidad), vb = Math.abs(b.velocidad);
        double pa = a.combatiente.montaje.peso(), pb = b.combatiente.montaje.peso();
        double angA = Math.toDegrees(Math.atan2(dy, dx));
        if (va + vb > 1 && a.equipo != b.equipo) {
            suceso(Suceso.Tipo.CHOQUE, a.indice, (a.x + b.x) / 2, (a.y + b.y) / 2, angA, va + vb, "");
            danar(b, a, 0.3 + va * 0.5 * (pa / Math.max(10, pb)), "choque", angA + 180, true);
            danar(a, b, 0.3 + vb * 0.5 * (pb / Math.max(10, pa)), "choque", angA, true);
        }
        if (a.equipo != b.equipo) {
            a.chocoCon = new Avistamiento(a, b);
            b.chocoCon = new Avistamiento(b, a);
        }
        a.velocidad *= 0.3;
        b.velocidad *= 0.3;
    }

    private void efectosContinuos(Cuerpo c, Cuerpo e) {
        if (c.llama > 0 && !c.fuera) {
            c.llama--;
            Pieza p = c.piezaDe(c.brazoDeLaLlama);
            if (p != null) {
                double d = Math.hypot(e.x - c.x, e.y - c.y);
                double ang = Math.toDegrees(Math.atan2(e.y - c.y, e.x - c.x));
                if (!e.fuera && d <= p.valor("alcance", 160) + e.radio && Math.abs(normal(ang - c.mira)) <= p.valor("cono", 30) / 2 + 8) {
                    double sobre = c.sobrecarga > 0 ? c.factorSobrecarga : 1;
                    danar(e, c, p.valor("dano", 1) * sobre * c.cifras.cuerpo, "llama", ang + 180, false);
                }
            }
        }
    }

    /** El blanco al que persigue un misil: el enemigo más cercano a él. */
    private Cuerpo objetivoDelMisil(Cuerpo dueno, Proyectil p) {
        Cuerpo mejor = null;
        double d = Double.MAX_VALUE;
        for (Cuerpo e : cuerpos) {
            if (e.equipo == dueno.equipo || e.fuera) continue;
            double x = Math.hypot(e.x - p.x, e.y - p.y);
            if (x < d) {
                d = x;
                mejor = e;
            }
        }
        return mejor == null ? dueno : mejor;
    }

    private void moverProyectiles() {
        for (Proyectil p : proyectiles) {
            if (!p.vivo) continue;
            Cuerpo dueno = cuerpos[p.dueno];
            Cuerpo blanco = objetivoDelMisil(dueno, p);
            if (p.misil && blanco != dueno && !blanco.fuera && blanco.camuflaje <= 0) {
                // El misil gira hacia el blanco, poco a poco.
                double quiero = Math.toDegrees(Math.atan2(blanco.y - p.y, blanco.x - p.x));
                double ahora = p.angulo();
                double g = Math.max(-p.giro, Math.min(p.giro, normal(quiero - ahora)));
                double nuevo = Math.toRadians(ahora + g);
                p.vx = Math.cos(nuevo) * p.velocidad;
                p.vy = Math.sin(nuevo) * p.velocidad;
            }
            p.x += p.vx;
            p.y += p.vy;
            p.recorrido += p.velocidad;
            if (p.recorrido > p.alcance || p.x < -20 || p.y < -20 || p.x > ancho + 20 || p.y > alto + 20) {
                p.vivo = false;
                if (p.misil) suceso(Suceso.Tipo.EXPLOSION, p.dueno, p.x, p.y, 0, 0, "misil");
                continue;
            }
            for (Cuerpo e : cuerpos) {
                if (e.equipo == dueno.equipo || e.fuera || (e.vuela() && !p.misil && false)) continue;
                if (Math.hypot(e.x - p.x, e.y - p.y) <= e.radio) {
                    p.vivo = false;
                    if (p.misil) suceso(Suceso.Tipo.EXPLOSION, p.dueno, p.x, p.y, 0, p.dano, "misil");
                    danar(e, dueno, p.dano, p.misil ? "misil" : "bala", p.angulo() + 180, false, p.x, p.y);
                    break;
                }
            }
        }
        proyectiles.removeIf(p -> !p.vivo);
    }

    // ================================================================ ayudas

    void suceso(Suceso.Tipo tipo, int quien, double x, double y, double angulo, double valor, String texto) {
        Suceso s = new Suceso(tipo, quien, x, y, angulo, valor, texto, turno);
        sucesosDelTurno.add(s);
        if (tipo != Suceso.Tipo.DECIR || sucesos.size() < 20000) sucesos.add(s);
        if (sucesos.size() > 40000) sucesos.subList(0, 10000).clear();
    }

    /** Lleva un ángulo a (-180, 180]. */
    public static double normal(double g) {
        if (Double.isNaN(g) || Double.isInfinite(g)) return 0;
        g %= 360;
        if (g > 180) g -= 360;
        if (g <= -180) g += 360;
        return g;
    }

    /** Juega la pelea entera de una vez (para las pruebas y para las peleas de práctica rápidas). */
    public void jugarEntera() {
        while (estado != Estado.TERMINADA) {
            if (estado != Estado.LUCHANDO) empezarRonda();
            avanzar();
        }
    }
}
