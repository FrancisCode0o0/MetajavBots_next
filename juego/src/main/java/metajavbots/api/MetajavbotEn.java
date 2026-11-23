package metajavbots.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * METAJAVBOT (ENGLISH) — Your robot, in English. Write a class that extends
 * this one (topic 11, inheritance) and override (@Override) the «on…»
 * methods to give it behavior:
 *
 * <pre>
 * public class MyRobot extends MetajavbotEn {
 *     &#64;Override
 *     public void onTick() {
 *         ahead(50);
 *         turn(10);
 *     }
 *
 *     &#64;Override
 *     public void onSeeEnemy(Enemy e) {
 *         aimTo(e.angle());
 *         attack();
 *     }
 * }
 * </pre>
 *
 * Every tick the fight first calls the events (onHit, onHitWall…), then
 * onSeeEnemy for each enemy in sight and finally onTick. Orders (ahead, turn,
 * aim) don't wait: they say what you want and the robot does it tick by tick,
 * at its own speed.
 *
 * The robot has three controls: the wheels (ahead, back), the body (turn,
 * turnTo) and the sight (aim, aimTo, aimAt). Each control remembers only the
 * last order of the tick: the later one erases the earlier one. The sight
 * turns slowly, and attack() fires where it points NOW.
 *
 * Angles in degrees: 0 is right (east), 90 is up; positive turns go left.
 * The arena goes from (0, 0), bottom left, to (arenaWidth(), arenaHeight()).
 */
public abstract class MetajavbotEn extends RobotBase {

    // ================================================================ what you override

    /** At the start of every round (before the first tick). Topic 06: methods. */
    public void onStart() {
    }

    /**
     * Every tick (about 30 per second). The main logic goes here. It is called last: the events (onHit, onHitWall…)
     * and onSeeEnemy come first. Each control (move, turn and sight) remembers only the last order of the tick, so
     * an order here erases the one for the same control you gave in an event.
     */
    public void onTick() {
    }

    /**
     * Every tick in which your radar sees an enemy, before onTick. Careful: if onTick then gives another sight order
     * (for example aim), it erases the one you give here, and the sight never reaches the enemy.
     */
    public void onSeeEnemy(Enemy e) {
    }

    /** When you get hit. */
    public void onHit(Hit h) {
    }

    /** When your attack lands. */
    public void onHitEnemy(Hit h) {
    }

    /** When you crash into a wall (it costs a little armor). */
    public void onHitWall() {
    }

    /** When you bump into an enemy. */
    public void onBump(Enemy e) {
    }

    /** At the end of the round. */
    public void onRoundEnd() {
    }

    /** When a teammate tells you something (in team fights). */
    public void onTeamMessage(String from, String message) {
    }

    @Override
    final void empezar() {
        ejecutarEn("onStart", this::onStart);
    }

    @Override
    final void turno() {
        ejecutarEn("onTick", this::onTick);
    }

    @Override
    final void ver(Avistado e) {
        ejecutarEn("onSeeEnemy", () -> onSeeEnemy(e));
    }

    @Override
    final void golpeado(Impacto g) {
        ejecutarEn("onHit", () -> onHit(g));
    }

    @Override
    final void acierto(Impacto g) {
        ejecutarEn("onHitEnemy", () -> onHitEnemy(g));
    }

    @Override
    final void pared() {
        ejecutarEn("onHitWall", this::onHitWall);
    }

    @Override
    final void choque(Avistado e) {
        ejecutarEn("onBump", () -> onBump(e));
    }

    @Override
    final void finDeRonda() {
        ejecutarEn("onRoundEnd", this::onRoundEnd);
    }

    @Override
    final void equipo(String de, String mensaje) {
        ejecutarEn("onTeamMessage", () -> onTeamMessage(de, mensaje));
    }

    // ================================================================ moving

    /**
     * Moves forward this distance (negative: backward). Replaces the previous move order: only the last one of the
     * tick counts. It does not wait: the robot carries it out little by little, over several ticks.
     */
    public final void ahead(double distance) {
        m().avanzar(distance);
        anotarOrden("ahead(" + redondear(distance) + ")", Mando.CANAL_AVANCE);
    }

    /** Moves backward this distance. It is a move order: only the last one of the tick counts, and it is carried out little by little. */
    public final void back(double distance) {
        m().avanzar(-distance);
        anotarOrden("back(" + redondear(distance) + ")", Mando.CANAL_AVANCE);
    }

    /**
     * Turns the body this many degrees (positive: left), little by little over several ticks. Only the last turn
     * order of the tick counts (turn, turnTo, stop…): the one called later erases the earlier one.
     */
    public final void turn(double degrees) {
        m().girar(degrees);
        anotarOrden("turn(" + redondear(degrees) + "°)", Mando.CANAL_GIRO);
    }

    public final void turnLeft(double degrees) {
        m().girar(degrees);
        anotarOrden("turnLeft(" + redondear(degrees) + "°)", Mando.CANAL_GIRO);
    }

    public final void turnRight(double degrees) {
        m().girar(-degrees);
        anotarOrden("turnRight(" + redondear(degrees) + "°)", Mando.CANAL_GIRO);
    }

    /**
     * Turns the body to face this absolute angle, the short way. Not all at once: it gets there over several ticks.
     * Only the last turn order of the tick counts.
     */
    public final void turnTo(double angle) {
        m().girar(normalize(angle - m().rumbo()));
        anotarOrden("turnTo(" + redondear(angle) + "°)", Mando.CANAL_GIRO);
    }

    /** Stops: no moving, no turning. */
    public final void stop() {
        m().detener();
        anotarOrden("stop()", Mando.CANAL_AVANCE | Mando.CANAL_GIRO);
    }

    // ================================================================ aiming and attacking

    /**
     * Turns the sight (arms and radar) this many degrees without turning the body. The sight turns slowly, about 14°
     * per tick depending on the weapon. Only the last sight order of the tick counts: an aim in onTick erases the
     * aimTo you gave in onSeeEnemy.
     */
    public final void aim(double degrees) {
        m().apuntar(degrees);
        anotarOrden("aim(" + redondear(degrees) + "°)", Mando.CANAL_MIRA);
    }

    /**
     * Asks the sight to turn to this absolute angle. Not instant: the sight turns slowly, about 14° per tick
     * depending on the weapon, and remainingAim() tells you how much is left. Only the last sight order of the tick
     * counts: if onTick calls aim afterwards, this one is erased.
     */
    public final void aimTo(double angle) {
        m().apuntar(normalize(angle - m().mira()));
        anotarOrden("aimTo(" + redondear(angle) + "°)", Mando.CANAL_MIRA);
    }

    /** Asks the sight to turn toward this point of the arena. Like aimTo: it takes several ticks and only the last sight order counts. */
    public final void aimAt(double x, double y) {
        m().apuntar(normalize(anguloHasta(x, y) - m().mira()));
        anotarOrden("aimAt(" + redondear(x) + ", " + redondear(y) + ")", Mando.CANAL_MIRA);
    }

    /** Attacks with the left arm where the sight points. Returns false if it was not ready yet. */
    public final boolean attackLeft() {
        boolean ok = m().atacar(Mando.IZQUIERDA);
        if (ok) anotarOrden("attackLeft()", 0);
        return ok;
    }

    public final boolean attackRight() {
        boolean ok = m().atacar(Mando.DERECHA);
        if (ok) anotarOrden("attackRight()", 0);
        return ok;
    }

    /** Uses the head's special (a shield, a turbo, repair…, depending on the part). */
    public final boolean special() {
        boolean ok = m().atacar(Mando.CABEZA);
        if (ok) anotarOrden("special()", 0);
        return ok;
    }

    /** Signature ability of the Assault archetype: forward speed burst (warns in the console on other archetypes). */
    public final boolean charge() {
        boolean ok = m().embestir();
        if (ok) anotarOrden("charge()", Mando.CANAL_AVANCE);
        return ok;
    }

    /** Signature ability of the Tactical archetype: 360° extended-range radar sweep (warns on other archetypes). */
    public final boolean radarPulse() {
        boolean ok = m().pulsoRadar();
        if (ok) anotarOrden("radarPulse()", 0);
        return ok;
    }

    /** Signature ability of the Defender archetype: fortified stance that reduces incoming damage (warns on other archetypes). */
    public final boolean fortify() {
        boolean ok = m().fortificar();
        if (ok) anotarOrden("fortify()", Mando.CANAL_AVANCE);
        return ok;
    }

    /**
     * Attacks with whichever arm is ready (right first). Fires at once, where the sight points NOW, not where you
     * just asked it to turn. Twice in the same tick uses both arms; more calls do nothing, because the arms need to
     * cool down.
     */
    public final boolean attack() {
        boolean ok = m().atacar(Mando.DERECHA) || m().atacar(Mando.IZQUIERDA);
        if (ok) anotarOrden("attack()", 0);
        return ok;
    }

    public final boolean readyLeft() {
        return m().listo(Mando.IZQUIERDA);
    }

    public final boolean readyRight() {
        return m().listo(Mando.DERECHA);
    }

    public final boolean readySpecial() {
        return m().listo(Mando.CABEZA);
    }

    /** The left arm weapon: "LASER", "BALA" (bullet), "MISIL" (missile), "CUERPO" (melee), "ESCUDO" (shield), "REPARAR" (repair)… ("" if none). */
    public final String weaponLeft() {
        return m().arma(Mando.IZQUIERDA);
    }

    public final String weaponRight() {
        return m().arma(Mando.DERECHA);
    }

    /** The head special: "ESCUDO" (shield), "TURBO", "REPARAR" (repair), "CAMUFLAJE" (cloak), "PULSO" (pulse), "SOBRECARGA" (overload) ("" if none). */
    public final String headSpecial() {
        return m().arma(Mando.CABEZA);
    }

    /** The archetype of your robot model: "ASALTO", "TACTICO" or "DEFENSOR". */
    public final String archetype() {
        return m().arquetipo();
    }

    /** How far the left arm's weapon reaches. */
    public final double rangeLeft() {
        return m().alcance(Mando.IZQUIERDA);
    }

    public final double rangeRight() {
        return m().alcance(Mando.DERECHA);
    }

    // ================================================================ weight, load, and mobility

    /** What your legs carry: the chassis of your model and all your parts except the legs. */
    public final double totalWeight() {
        return m().pesoTotal();
    }

    /** What your legs can carry (their load limit). */
    public final double loadLimit() {
        return m().cargaMaxima();
    }

    /** totalWeight() / loadLimit(): up to 0.70 you are light, up to 1 normal, up to 1.20 overloaded. */
    public final double loadRatio() {
        return m().proporcionDeCarga();
    }

    /** Are you overloaded? Then you drive and turn worse, spend more energy moving, and cannot jump or dodge quickly. */
    public final boolean overloaded() {
        return m().sobrecargado();
    }

    /** Your top speed right now, with your weight and the terrain under you. */
    public final double topSpeed() {
        return m().velocidadMaxima();
    }

    /** How far you can turn this tick: the faster you go, the worse you turn. */
    public final double maxTurnRate() {
        return m().giroMaximo();
    }

    /** Your acceleration right now (braking is twice as fast). */
    public final double maxAcceleration() {
        return m().aceleracionMaxima();
    }

    /** Your legs: "BIPEDO" (biped), "ORUGAS" (treads), "RUEDAS" (wheels), "FLOTANTE" (hover), "VUELO" (flight), or "PATAS" (spider legs). */
    public final String locomotion() {
        return m().locomocion();
    }

    /** The terrain under you: "LLANO" (flat), "ACEITE" (oil), "AGUA" (water), "RAMPA" (ramp), "ESCOMBROS" (rubble), or "TRAMPA" (floor trap). */
    public final String terrain() {
        return m().terreno();
    }

    /** The power of adjustable-power weapons (0.1 to 3): more power, more damage, but a slower bullet, more heat, and more energy. */
    public final void setPower(double power) {
        m().regularPotencia(power);
        anotarOrden("setPower(" + redondear(power) + ")", 0);
    }

    // ================================================================ knowing

    public final double x() {
        return m().x();
    }

    public final double y() {
        return m().y();
    }

    /** Where the body faces (degrees). */
    public final double heading() {
        return m().rumbo();
    }

    /** Where the sight points (degrees). */
    public final double aimDirection() {
        return m().mira();
    }

    public final double speed() {
        return m().velocidad();
    }

    /** Energy (lasers and specials use it; it recharges by itself). */
    public final double energy() {
        return m().energia();
    }

    /** Your remaining armor, from 0 to 100. */
    public final double armor() {
        return m().blindaje();
    }

    /** The tick number in this round (from 0). */
    public final int tick() {
        return m().turno();
    }

    /** The round (1 to 4). */
    public final int round() {
        return m().ronda();
    }

    public final double arenaWidth() {
        return m().anchoArena();
    }

    public final double arenaHeight() {
        return m().altoArena();
    }

    /** What is left of the last move order. */
    public final double remainingDistance() {
        return m().restanteAvance();
    }

    /** What is left of the last turn order. */
    public final double remainingTurn() {
        return m().restanteGiro();
    }

    public final double remainingAim() {
        return m().restanteMira();
    }

    /** The enemies you can see right now (topic 15: collections). */
    public final List<Enemy> seenEnemies() {
        return new ArrayList<>(m().vistos());
    }

    /** The traps and objects in the arena: spikes, saws, pit, barrels… (empty if there are none; topic 15). */
    public final List<Obstacle> obstacles() {
        return new ArrayList<>(m().obstaculos());
    }

    /** The gadgets on the arena floor: drive to one to pick it up (empty if there are none; topic 15). */
    public final List<Gadget> gadgets() {
        return new ArrayList<>(m().aparatos());
    }

    /** The gadget you carry (null if none). */
    public final Gadget myGadget() {
        return m().miAparato();
    }

    /** Plugs the gadget you carry into that slot: "back", "legs", "head" or "arm" (read its code to know which). */
    public final boolean plugIn(String slot) {
        return m().conectar(slot);
    }

    /** Uses the plugged-in gadget (where you aim, or at that angle). Returns whether it worked. */
    public final boolean useGadget() {
        return m().usarAparato(false, 0);
    }

    /** Uses the plugged-in gadget (where you aim, or at that angle). Returns whether it worked. */
    public final boolean useGadget(double angle) {
        return m().usarAparato(true, angle);
    }

    /** Your teammates (in team fights; empty when you fight alone). */
    public final List<Enemy> teammates() {
        return new ArrayList<>(m().companeros());
    }

    /** Tells your teammates something (it arrives next tick in onTeamMessage). */
    public final void tellTeam(Object message) {
        m().avisarAlEquipo(aTexto(message));
    }

    // ================================================================ helpers

    /** The absolute angle from you to that point. */
    public final double angleTo(double x, double y) {
        return anguloHasta(x, y);
    }

    /** The distance from you to that point. */
    public final double distanceTo(double x, double y) {
        return distanciaHasta(x, y);
    }

    /** Brings an angle into (-180, 180]: normalize(350) gives -10. */
    public final double normalize(double degrees) {
        return normalizado(degrees);
    }

    /** Random numbers (your robot's own: the same fight replayed gives the same). */
    public final Random random() {
        return m().azar();
    }

    /** Says something in a speech bubble above the robot. */
    public final void say(Object text) {
        m().decir(aTexto(text));
    }

    /** Writes to your robot's console (below the code). */
    public final void print(Object text) {
        m().escribir(aTexto(text));
    }

    /** What you want to remember from round to round (topic 15: Map). Survives even if you change the code. */
    public final Map<String, Object> memory() {
        return m().memoria();
    }
}
