package metajavbots.escritorio;

import java.util.List;
import java.util.Map;

import static metajavbots.escritorio.MontajeDelTrailer.fija;
import static metajavbots.escritorio.MontajeDelTrailer.mejor;
import static metajavbots.escritorio.MontajeDelTrailer.rapida;

/**
 * VERSIONES DEL TRÁILER — Las tres versiones, para elegir. Las tomas se miden en pulsos de «Último asalto» (120 por minuto:
 * un pulso es medio segundo, un compás son cuatro). La pieza va por vueltas de 32 pulsos: timbales (0-32), metales (32-64),
 * coro, batería y golpes de orquesta (64-96), lo más fuerte (96-128) y el final en mayor (desde 128), donde entra el título.
 */
final class VersionesDelTrailer {

    /**
     * Hasta dónde se puede usar cada clip cuando no basta con su campana final: en la Carrera el robot cae a los ~17 s, y en
     * Destrucción el cartel del final sale antes que la campana.
     */
    static final Map<String, Double> HASTA = Map.of("carrera", 16.5, "taller", 11.6, "destruccion", 10.8);

    private static final String DUELO = "Duelo · Duel · Duell";
    private static final String ROYALE = "Battle royale";
    private static final String EQUIPOS = "Por equipos · Teams · Teamkampf";
    private static final String CARRERA = "Carrera de obstáculos · Obstacle race · Hindernisrennen";
    private static final String DESTRUCCION = "Destrucción y supervivencia · Destruction & survival";

    private VersionesDelTrailer() {
    }

    static List<MontajeDelTrailer.Version> todas() {
        return List.of(programar(), modos(), epico());
    }

    /** 1: programar y ver el resultado. El código se escribe entero, se compila y ese robot pelea. */
    private static MontajeDelTrailer.Version programar() {
        return new MontajeDelTrailer.Version("trailer_1_programar",
                "programar y ver el resultado: el Cangrejo se escribe y se compila en el Taller (11 s) y luego pelea; duelo, royale, "
                        + "equipos, carrera y destrucción; cortes cada 2 compases.", 12, List.of(
                fija("taller", 22, 0.4).rotulo("Programa tu robot en Java · Code your robot"),
                fija("duelo", 10, 0.4).rotulo("…y míralo pelear · …and watch it fight"),
                mejor("duelo", 8).destello(), mejor("duelo", 8), mejor("duelo", 8), mejor("duelo", 8),
                mejor("royale", 8).destello().rotulo(ROYALE), mejor("royale", 8).destello(),
                mejor("equipos", 8).destello().rotulo(EQUIPOS), mejor("equipos", 8).destello(),
                fija("carrera", 8, 0.6).destello().rotulo(CARRERA), mejor("destruccion", 8).destello().rotulo(DESTRUCCION),
                mejor("royale", 4).destello(), mejor("duelo", 4).destello(), mejor("equipos", 8).destello(),
                fija("titulo", 20, 0)));
    }

    /** 2: los modos de juego, uno tras otro, cada uno con su rótulo y su presentación. */
    private static MontajeDelTrailer.Version modos() {
        return new MontajeDelTrailer.Version("trailer_2_modos",
                "los modos de juego: cada uno con su rótulo y su presentación (duelo, battle royale, por equipos, carrera y destrucción); "
                        + "el código, al principio y más deprisa.", 30, List.of(
                rapida("taller", 16, 0.3, 1.4).rotulo("Programa tu robot en Java · Code your robot"),
                fija("duelo", 8, 0.4).destello().rotulo(DUELO), mejor("duelo", 8),
                fija("royale", 8, 0.6).destello().rotulo(ROYALE), mejor("royale", 8),
                fija("equipos", 8, 0.6).destello().rotulo(EQUIPOS), mejor("equipos", 8),
                fija("carrera", 16, 0.6).destello().rotulo(CARRERA),
                mejor("destruccion", 8).destello().rotulo(DESTRUCCION), mejor("royale", 8).destello(),
                mejor("royale", 8).destello(), mejor("duelo", 8).destello(), mejor("equipos", 8).destello(), mejor("royale", 8).destello(),
                fija("titulo", 20, 0)));
    }

    /** 3: rápido y épico: cortes cada compás y, en lo más fuerte, cada dos pulsos. */
    private static MontajeDelTrailer.Version epico() {
        return new MontajeDelTrailer.Version("trailer_3_epico",
                "rápido y épico: el código a toda velocidad y cortes cada compás (cada dos pulsos en lo más fuerte), con destellos en "
                        + "los golpes de la música.", 40, List.of(
                rapida("taller", 8, 0.3, 2.6).rotulo("Programa · Code · Programmiere"), fija("duelo", 8, 0.4).destello(),
                mejor("duelo", 4), mejor("royale", 4), mejor("equipos", 4), fija("carrera", 4, 0.6),
                mejor("royale", 4).destello(), mejor("duelo", 4), mejor("destruccion", 4).destello(), mejor("equipos", 4),
                mejor("royale", 4).destello(), fija("carrera", 4, 2.8), mejor("duelo", 4).destello(), mejor("equipos", 4),
                mejor("royale", 4).destello(), mejor("duelo", 4), mejor("royale", 4).destello(), mejor("equipos", 4),
                mejor("destruccion", 4).destello(), mejor("royale", 4), mejor("duelo", 4).destello(), mejor("equipos", 4),
                mejor("royale", 2).destello(), mejor("duelo", 2), mejor("equipos", 2).destello(), mejor("royale", 2),
                mejor("destruccion", 2).destello(), mejor("duelo", 2), mejor("royale", 2).destello(), mejor("equipos", 2),
                mejor("royale", 2).destello(), mejor("duelo", 2), mejor("equipos", 2).destello(), mejor("royale", 2),
                mejor("destruccion", 2).destello(), mejor("duelo", 2), mejor("royale", 2).destello(), mejor("equipos", 2),
                fija("titulo", 20, 0)));
    }
}
