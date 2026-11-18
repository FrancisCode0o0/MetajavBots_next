package metajavbots.api;

/**
 * GADGET — A gizmo that turns up in the arena from round to round: jet
 * engine, anti-gravity thruster, electric shock, flamethrower, oil, glue…
 * The ones on the floor come in gadgets(); yours, in myGadget(). To fit it,
 * read its code (the gadget button in the fight): it says which slot it plugs
 * into (plugIn("back")…) and how to use it.
 *
 * Topic: interfaces (13) and composition (a robot made of parts).
 */
public interface Gadget {

    /** What it is: "chorro" (jet), "antigravedad" (anti-gravity), "descarga" (shock), "lanzallamas" (flamethrower), "aceite" (oil), "pegamento" (glue). */
    String type();

    /** Its brand name (Jetzazo, Flotón, Chispazo…), to say it. */
    String name();

    /** Where it is (if you carry it, where you are). */
    double x();

    double y();

    /** How far it is from you and at which angle you see it (degrees, absolute). */
    double distance();

    double angle();

    /** Is it already plugged into your robot? (Only yours.) */
    boolean pluggedIn();

    /** Can it be used yet? (Plugged in and recharged.) */
    boolean ready();
}
