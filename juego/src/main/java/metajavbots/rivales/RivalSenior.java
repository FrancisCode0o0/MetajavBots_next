package metajavbots.rivales;

import java.util.List;

import metajavbots.api.Enemigo;
import metajavbots.api.Golpe;
import metajavbots.api.Metajavbot;
import metajavbots.api.RobotBase;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Fabrica;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * RIVAL SENIOR — Cinco rivales de nivel Senior con estilo «absurdo al límite»
 * que castigan fallos típicos de programación en combate real:
 * 1) Bucles caros O(n²) y exceso de tiempo por turno.
 * 2) Falta de normalización angular alrededor de ±180°.
 * 3) Estado mutable compartido mal sincronizado (en modo robots en paralelo).
 * 4) Desreferencias nulas sin comprobar ({@code NullPointerException}).
 * 5) Mala gestión de memoria (asignaciones excesivas / GC) y energía.
 */
public final class RivalSenior extends Metajavbot {

    /** Perfil de cada rival Senior y sus reglas de presupuesto/concurrencia. */
    public enum Tipo {
        BUCLE_CARO("senior_bucle", "voltio", 0.78, 260L, 2_500_000L, 0L, false,
                new Trilingue("Don Relojero O(n²)", "Meister Endlosschleife", "Lord Big-O"),
                new Trilingue(
                        "Intentó ordenar sus tornillos con bogosort y juró venganza contra cualquier robot que tarde más de 2,5 ms por turno.",
                        "Versuchte seine Schrauben mit Bogosort zu sortieren und schwor jedem Roboter Rache, der länger als 2,5 ms pro Zug braucht.",
                        "Tried to sort its screws with bogosort and swore vengeance on any robot taking more than 2.5 ms per tick."),
                new Trilingue(
                        "Presupuesto estricto de tiempo (2,5 ms y 260 pasos/turno): castiga bucles caros y algoritmos O(n²).",
                        "Strenges Zeitbudget (2,5 ms und 260 Schritte/Zug): bestraft teure Schleifen und O(n²)-Algorithmen.",
                        "Strict time budget (2.5 ms and 260 steps/tick): punishes expensive loops and O(n²) algorithms."),
                new Trilingue(
                        "Reduce la complejidad de cadaTurno(): evita bucles anidados y precalcula valores constantes fuera del turno.",
                        "Reduziere die Komplexität von jedenZug(): Vermeide verschachtelte Schleifen und berechne Konstanten vorab.",
                        "Lower onTick() complexity: avoid nested loops and precompute constants outside the tick.")),
        ANGULOS("senior_angulos", "avispa", 0.80, 0L, 0L, 0L, false,
                new Trilingue("Barón 361°", "Baron 361°", "Baron 361°"),
                new Trilingue(
                        "Giró tanto en una rotonda sin normalizar ángulos que todavía debe 720 grados al ayuntamiento.",
                        "Drehte sich ohne Winkelnormalisierung so oft im Kreisverkehr, dass er der Stadt noch 720 Grad schuldet.",
                        "Spun so hard in a roundabout without normalizing angles that it still owes 720 degrees to city hall."),
                new Trilingue(
                        "Orbita rápido cruzando la frontera de ±180° para desorientar a quien resta ángulos sin normalizar().",
                        "Umkreist dich rasch über die ±180°-Grenze und verwirrt jeden, der Winkel ohne normalisieren() subtrahiert.",
                        "Orbits rapidly across the ±180° wrap-around to break robots that subtract angles without normalize()."),
                new Trilingue(
                        "Normaliza siempre las diferencias angulares con normalizar(objetivo - mira()) para girar por el camino corto en (-180, 180].",
                        "Normalisiere Winkeldifferenzen immer mit normalisieren(ziel - blick()), um den kürzesten Weg in (-180, 180] zu drehen.",
                        "Always normalize angle deltas with normalize(target - aimDirection()) so you turn the shortest way in (-180, 180].")),
        CONCURRENCIA("senior_concurrencia", "tejedora", 0.82, 0L, 0L, 0L, true,
                new Trilingue("Condesa Carrera de Datos", "Gräfin Datenrennen", "Countess Data-Race"),
                new Trilingue(
                        "Sus dos brazos se pelearon por la misma taza de aceite sin synchronized y terminaron sirviendo café al árbitro.",
                        "Ihre zwei Arme stritten ohne synchronized um dieselbe Öltasse und servierten am Ende dem Schiedsrichter Kaffee.",
                        "Its two arms fought over the same oil cup without synchronized and ended up pouring espresso on the referee."),
                new Trilingue(
                        "Robots en paralelo (2 contra 2 simultáneos): castiga estado estático mutable y colecciones sin sincronizar.",
                        "Parallele Roboter (2 gegen 2 gleichzeitig): bestraft veränderlichen static-Zustand und unsichere Collections.",
                        "Parallel robots (simultaneous 2v2): punishes mutable static state and unsynchronized collections."),
                new Trilingue(
                        "En paralelo, evita campos static mutables y usa AtomicInteger, ConcurrentHashMap, synchronized o avisarAlEquipo().",
                        "Vermeide im Parallelmodus veränderliche static-Felder und nutze AtomicInteger, ConcurrentHashMap, synchronized oder teamMelden().",
                        "In parallel mode, avoid mutable static fields and use AtomicInteger, ConcurrentHashMap, synchronized, or tellTeam().")),
        NULOS("senior_nulos", "eclipse", 0.80, 0L, 0L, 0L, false,
                new Trilingue("Señor NullPointer", "Herr Nullzeiger", "Sir NullPointer"),
                new Trilingue(
                        "Intentó beber de una taza null en la cantina y provocó una excepción que apagó las luces del estadio.",
                        "Versuchte in der Kantine aus einer null-Tasse zu trinken und löste eine Exception aus, die das Stadion abschaltete.",
                        "Tried to drink from a null mug in the cafeteria and threw an exception that blacked out the stadium."),
                new Trilingue(
                        "Rompe el contacto de radar con camuflaje y flanqueos: castiga leer miAparato(), memoria() o listas sin comprobar null.",
                        "Bricht den Radarkontakt mit Tarnung ab: bestraft Zugriffe auf meinGeraet(), gedaechtnis() oder Listen ohne null-Prüfung.",
                        "Breaks radar lock with stealth dashes: punishes using myGadget(), memory(), or lists without null/empty checks."),
                new Trilingue(
                        "Comprueba siempre != null y !isEmpty() antes de acceder a miAparato(), memoria().get(...) o enemigosVistos().get(0).",
                        "Prüfe immer != null und !isEmpty(), bevor du auf meinGeraet(), gedaechtnis().get(...) oder gesehen().get(0) zugreifst.",
                        "Always check != null and !isEmpty() before reading myGadget(), memory().get(...), or seenEnemies().get(0).")),
        MEMORIA_ENERGIA("senior_memoria", "galapago", 0.82, 0L, 0L, 8_192L, false,
                new Trilingue("Baronesa Basura GC", "Baronin Müllsammler", "Baroness GC-Storm"),
                new Trilingue(
                        "Compraba un chasis nuevo cada 33 milisegundos hasta que el recolector de basura le embargó el garaje.",
                        "Kaufte alle 33 Millisekunden ein neues Chassis, bis der Garbage Collector ihre Garage pfändete.",
                        "Bought a brand-new chassis every 33 milliseconds until the garbage collector repossessed its garage."),
                new Trilingue(
                        "Presupuesto estricto de memoria (8 KB/turno) y desgaste de energía: castiga crear objetos en bucles y malgastar disparos.",
                        "Strenges Speicherbudget (8 KB/Zug) und Energiedruck: bestraft Objekterzeugung in Schleifen und Fehlschüsse.",
                        "Strict memory budget (8 KB/tick) and energy pressure: punishes per-tick allocations and wasteful firing."),
                new Trilingue(
                        "Reutiliza campos primitivos, evita new dentro de bucles/cadaTurno() y dispara solo cuando tengas energía y puntería.",
                        "Verwende primitive Felder wieder, vermeide new in Schleifen/jedenZug() und feuere nur mit Energie und Zielgenauigkeit.",
                        "Reuse primitive fields, avoid new inside loops/onTick(), and fire only when energy and aim line up."));

        public final String id, modelo;
        public final double nivel;
        public final long topeDePasos, presupuestoNanos, presupuestoBytes;
        public final boolean enParalelo;
        public final Trilingue nombre, biografiaAbsurda, descripcion, consejoRevision;

        Tipo(String id, String modelo, double nivel, long topeDePasos, long presupuestoNanos, long presupuestoBytes, boolean enParalelo,
                Trilingue nombre, Trilingue biografiaAbsurda, Trilingue descripcion, Trilingue consejoRevision) {
            this.id = id;
            this.modelo = modelo;
            this.nivel = nivel;
            this.topeDePasos = topeDePasos;
            this.presupuestoNanos = presupuestoNanos;
            this.presupuestoBytes = presupuestoBytes;
            this.enParalelo = enParalelo;
            this.nombre = nombre;
            this.biografiaAbsurda = biografiaAbsurda;
            this.descripcion = descripcion;
            this.consejoRevision = consejoRevision;
        }

        /** Busca un rival Senior por su identificador, o {@code null} si no existe. */
        public static Tipo de(String id) {
            if (id == null || id.isBlank()) return null;
            for (Tipo t : values()) if (t.id.equals(id)) return t;
            return null;
        }
    }

    private final Tipo tipo;
    private double enemigoX, enemigoY, enemigoDist = 9999;
    private int ultimoVisto = -1000;
    private int sentido = 1;
    private int cambio;

    public RivalSenior(Tipo tipo) {
        this.tipo = tipo == null ? Tipo.BUCLE_CARO : tipo;
    }

    /** Todos los tipos de rival Senior disponibles. */
    public static List<Tipo> todos() {
        return List.of(Tipo.values());
    }

    /** Crea un combatiente Senior equipado a su nivel con el comportamiento que castiga su fallo típico. */
    public static Combatiente crear(Tipo tipo, Idioma idioma) {
        Tipo t = tipo == null ? Tipo.BUCLE_CARO : tipo;
        Idioma id = idioma == null ? Idioma.ES : idioma;
        Modelo mod = Modelo.de(t.modelo);
        if (mod == null) mod = Modelo.todos().get(0);
        Montaje m = new Montaje(mod);
        int marca = 1 + (int) Math.round(Math.max(0, Math.min(1, t.nivel)) * 4);
        CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
        for (Montaje.Hueco h : Montaje.Hueco.values()) {
            Pieza p = m.pieza(h);
            if (p == null) continue;
            Pieza mejor = cat.pieza(Rivales.baseDe(p.id) + (marca == 1 ? "" : "_" + marca));
            if (mejor != null) m.poner(h, mejor);
        }
        Combatiente c = new Combatiente(t.nombre.en(id), m, new Fabrica() {
            @Override
            public RobotBase crear() {
                return new RivalSenior(t);
            }

            @Override
            public String clase() {
                return "RivalSenior";
            }

            @Override
            public String fuente() {
                return Rivales.codigoDe("Rival");
            }
        });
        c.computadora = true;
        return c;
    }

    @Override
    public void alEmpezar() {
        ultimoVisto = -1000;
        sentido = azar().nextBoolean() ? 1 : -1;
        cambio = 35;
    }

    @Override
    public void alVerEnemigo(Enemigo e) {
        enemigoX = e.x();
        enemigoY = e.y();
        enemigoDist = e.distancia();
        ultimoVisto = turnoActual();
        if (tipo == Tipo.CONCURRENCIA && !companeros().isEmpty() && turnoActual() % 30 == 0) {
            avisarAlEquipo("objetivo " + (int) e.x() + " " + (int) e.y());
        }
    }

    @Override
    public void alRecibirGolpe(Golpe g) {
        if (tipo == Tipo.ANGULOS || tipo == Tipo.MEMORIA_ENERGIA) sentido = -sentido;
        girarHacia(g.angulo() + 90 * sentido);
        avanzar(90);
    }

    @Override
    public void cadaTurno() {
        if (turnoActual() - ultimoVisto >= 25) {
            apuntar(28 * sentido);
            if (restanteAvance() == 0) {
                girar(35 * sentido);
                avanzar(90);
            }
            evitarParedes();
            return;
        }
        double hacia = anguloHacia(enemigoX, enemigoY);
        apuntarHacia(hacia);
        cambio--;
        if (cambio <= 0) {
            cambio = tipo == Tipo.ANGULOS ? 22 : 40 + azar().nextInt(30);
            sentido = -sentido;
        }
        switch (tipo) {
            case ANGULOS:
                // Cruza rápido el eje izquierdo (±180°) alrededor del jugador para delatar ángulos sin normalizar.
                girarHacia(hacia + 78 * sentido);
                avanzar(150);
                break;
            case NULOS:
                // Usa camuflaje/especial y flanquea para vaciar el radar del rival.
                if (listoEspecial() && enemigoDist < 420) especial();
                girarHacia(hacia + (enemigoDist < 220 ? 115 : 65) * sentido);
                avanzar(130);
                break;
            case MEMORIA_ENERGIA:
                // Mantiene distancia larga para castigar a quien malgasta energía o se frena por GC.
                girarHacia(hacia + (enemigoDist < 360 ? 150 : 88) * sentido);
                avanzar(110);
                break;
            case CONCURRENCIA:
                // Presiona en pinza coordinada con su compañero paralelo.
                girarHacia(hacia + 55 * sentido);
                avanzar(120);
                break;
            case BUCLE_CARO:
            default:
                // Acoso constante que aprovecha cualquier turno perdido por deuda de tiempo/pasos.
                girarHacia(hacia + (enemigoDist > 260 ? 45 : 95) * sentido);
                avanzar(130);
                break;
        }
        evitarParedes();
        if (Math.abs(restanteMira()) < 14) {
            if (listoDerecha() && enemigoDist <= alcanceDerecha() * 0.95) atacarDerecha();
            if (listoIzquierda() && enemigoDist <= alcanceIzquierda() * 0.95) atacarIzquierda();
        }
        if (listoEspecial() && tipo != Tipo.NULOS && enemigoDist < 420 && azar().nextDouble() < 0.08) especial();
    }

    private void evitarParedes() {
        double m = 55;
        if (x() < m || y() < m || x() > anchoArena() - m || y() > altoArena() - m) {
            girarHacia(anguloHacia(anchoArena() / 2, altoArena() / 2));
            avanzar(110);
        }
    }
}
