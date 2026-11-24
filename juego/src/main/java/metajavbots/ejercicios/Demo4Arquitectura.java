package metajavbots.ejercicios;

import java.util.List;

import metajavbots.ejercicios.Demostracion.Accion;
import metajavbots.ejercicios.Demostracion.Paso;
import metajavbots.ejercicios.Demostracion.Resaltado;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Trilingue;

/**
 * DEMO 4 — POO en combate, aparatos, energía y tope de cálculo:
 * muestra en vivo cómo la arquitectura de clases en Java ({@code private},
 * {@code final}, {@code static} frente a {@code public} mutable), el uso
 * correcto de aparatos en su hueco, la gestión de energía por turno y la
 * eficiencia algorítmica frente a {@code TopeDeCalculo} y la deuda de turnos
 * tienen consecuencias físicas reales dentro de la arena.
 */
final class Demo4Arquitectura {

    private Demo4Arquitectura() {
    }

    static Demostracion crear() {
        Trilingue titulo = new Trilingue(
                "Demo 4: POO en combate, aparatos, energía y tope de cálculo",
                "Demo 4: OOP im Kampf, Geräte, Energie und Rechenlimit",
                "Demo 4: OOP in combat, gadgets, energy, and step limit");
        Trilingue resumen = new Trilingue(
                "Comprueba en vivo cómo los modificadores POO, los aparatos, el gasto de energía "
                        + "y el tope de cálculo con deuda de turnos cambian la pelea real.",
                "Erlebe live, wie OOP-Modifikatoren, Geräte, Energieverbrauch "
                        + "und das Rechenlimit mit Zugschuld den echten Kampf verändern.",
                "See live how OOP modifiers, gadgets, energy use, "
                        + "and the step limit with tick debt change real fights.");
        Trilingue codPooExp = new Trilingue(codigoPooExpuestoEs(), codigoPooExpuestoDe(), codigoPooExpuestoEn());
        Trilingue codPooEnc = new Trilingue(codigoPooEncapsuladoEs(), codigoPooEncapsuladoDe(), codigoPooEncapsuladoEn());
        Trilingue codApErr = new Trilingue(codigoAparatoErrorEs(), codigoAparatoErrorDe(), codigoAparatoErrorEn());
        Trilingue codApBien = new Trilingue(codigoAparatoBienEs(), codigoAparatoBienDe(), codigoAparatoBienEn());
        Trilingue codEnergia = new Trilingue(codigoEnergiaEs(), codigoEnergiaDe(), codigoEnergiaEn());
        Trilingue codTopeLento = new Trilingue(codigoTopeLentoEs(), codigoTopeLentoDe(), codigoTopeLentoEn());
        Trilingue codTopeOptimo = new Trilingue(codigoTopeOptimoEs(), codigoTopeOptimoDe(), codigoTopeOptimoEn());
        List<Paso> pasos = List.of(
                paso1PooExpuesto(),
                paso2PooEncapsulado(codPooEnc),
                paso3AparatoError(codApErr),
                paso4AparatoBien(codApBien),
                paso5GestionEnergia(codEnergia),
                paso6TopeDeuda(codTopeLento),
                paso7TopeOptimo(codTopeOptimo),
                paso8CierreArquitectura(codTopeOptimo));
        return new Demostracion(Demostracion.ID_DEMO_4, 4, titulo, resumen,
                VistaDeArena.Escenario.NEON, codPooExp, pasos);
    }

    private static Paso paso1PooExpuesto() {
        return new Paso(
                "poo_expuesto",
                new Trilingue(
                        "POO en combate: el coste de campos public mutables",
                        "OOP im Kampf: der Preis veränderbarer public-Felder",
                        "OOP in combat: the cost of mutable public fields"),
                new Trilingue(
                        "El motor inspecciona los modificadores Java de tu clase al empezar la ronda.\n"
                                + "Aquí las líneas 4 y 5 exponen estado mutable como public sin encapsular.\n"
                                + "Eso penaliza en la arena: el rival te detecta desde más lejos (+16 % radar rival con dos campos) "
                                + "y recibes más daño por impacto (+8 % daño recibido). ¡Fíjate en el golpe que acaba de encajar!",
                        "Die Engine prüft beim Rundenstart die Java-Modifikatoren deiner Klasse.\n"
                                + "Hier legen die Zeilen 4 und 5 veränderbaren Zustand ungeschützt als public offen.\n"
                                + "Das kostet in der Arena: Der Gegner erkennt dich früher (+16 % Gegner-Radar bei zwei Feldern) "
                                + "und du nimmst mehr Schaden pro Treffer (+8 % erlittener Schaden). Schau dir den Treffer von eben an!",
                        "The engine inspects your class's Java modifiers when each round starts.\n"
                                + "Here lines 4 and 5 expose mutable state as unencapsulated public fields.\n"
                                + "That carries a real arena penalty: the rival detects you from farther away (+16% rival radar with two fields) "
                                + "and you take more damage per hit (+8% damage taken). Watch the hit it just took!"),
                4, new int[]{5, 8}, true, 12, 55, Accion.POO_EXPUESTO, Resaltado.ERROR, null);
    }

    private static Paso paso2PooEncapsulado(Trilingue codigo) {
        return new Paso(
                "poo_encapsulado",
                new Trilingue(
                        "Encapsular con private, final y static",
                        "Kapseln mit private, final und static",
                        "Encapsulating with private, final, and static"),
                new Trilingue(
                        "Ahora el estado es private (líneas 4, 5 y método auxiliar en línea 10): reduces el daño recibido (-6 %) "
                                + "y acortas el radar del rival (-8 %).\n"
                                + "Las constantes private final (líneas 6 y 7) amplían tu presupuesto de pasos de cálculo por turno (+10 %).\n"
                                + "El campo static (línea 8) conserva memoria de clase y en peleas por equipos (2v2+) aporta +10 % radar y +8 % recarga.",
                        "Jetzt ist der Zustand private (Zeilen 4, 5 und Hilfsmethode in Zeile 10): Du verringerst erlittenen Schaden (-6 %) "
                                + "und verkürzt das Gegner-Radar (-8 %).\n"
                                + "Die Konstanten private final (Zeilen 6 und 7) erweitern dein Rechenschritt-Budget pro Zug (+10 %).\n"
                                + "Das Feld static (Zeile 8) hält Klassenspeicher und bringt im Teamkampf (2v2+) +10 % Radar und +8 % Aufladung.",
                        "Now the state is private (lines 4, 5, and helper method on line 10): you cut damage taken (-6%) "
                                + "and shrink the rival's radar range (-8%).\n"
                                + "The private final constants (lines 6 and 7) expand your per-tick step budget (+10%).\n"
                                + "The static field (line 8) keeps class memory and in team fights (2v2+) grants +10% own radar and +8% energy recharge."),
                4, new int[]{6, 8, 10}, true, 12, 55, Accion.POO_ENCAPSULADO, Resaltado.BIEN, codigo);
    }

    private static Paso paso3AparatoError(Trilingue codigo) {
        return new Paso(
                "aparato_hueco_error",
                new Trilingue(
                        "Aparatos: recoger y fallar el hueco al conectar",
                        "Geräte: aufheben und am falschen Platz anschließen",
                        "Gadgets: picking up and plugging into the wrong slot"),
                new Trilingue(
                        "En la arena acaba de aparecer un motor a chorro (\"chorro\"): el robot consulta aparatos(), "
                                + "conduce hasta él y lo recoge al tocarlo.\n"
                                + "Al tenerlo en miAparato(), la línea 10 intenta conectar(\"cabeza\");, pero el hueco del chorro es \"espalda\".\n"
                                + "El motor rechaza la conexión (devuelve false): conectado() sigue en false y el aparato no se puede usar.",
                        "In der Arena erschien gerade ein Düsentriebwerk (\"chorro\"): Der Roboter liest geraete(), "
                                + "fährt hin und hebt es bei Berührung auf.\n"
                                + "Sobald meinGeraet() es liefert, versucht Zeile 10 anschliessen(\"kopf\");, aber der Platz ist \"ruecken\".\n"
                                + "Die Engine lehnt den Anschluss ab (gibt false zurück): angeschlossen() bleibt false und das Gerät startet nicht.",
                        "A jet engine (\"chorro\") just spawned in the arena: the robot reads gadgets(), "
                                + "drives toward it, and picks it up on contact.\n"
                                + "Once myGadget() returns it, line 10 tries plugIn(\"head\");, but the jet engine belongs in \"back\".\n"
                                + "The engine rejects the connection (returns false): pluggedIn() stays false and the gadget cannot be activated."),
                10, new int[]{7, 9, 14}, true, 16, 150, Accion.APARATO_ERROR, Resaltado.ERROR, codigo);
    }

    private static Paso paso4AparatoBien(Trilingue codigo) {
        return new Paso(
                "aparato_hueco_valido",
                new Trilingue(
                        "Aparatos: hueco correcto, montaje y activación",
                        "Geräte: richtiger Platz, Montage und Aktivierung",
                        "Gadgets: valid slot, mounting, and activation"),
                new Trilingue(
                        "Ahora comprobamos el tipo con mio.tipo() y conectamos en su hueco real: conectar(\"espalda\"); (línea 10).\n"
                                + "El motor acepta la conexión y tarda 30 turnos (1 segundo) en montarlo.\n"
                                + "En cuanto mio.listo() es true, la línea 12 ejecuta usarAparato(); y el chorro multiplica la velocidad por 2,6.",
                        "Jetzt prüfen wir den Typ mit meins.typ() und schließen am richtigen Platz an: anschliessen(\"ruecken\"); (Zeile 10).\n"
                                + "Die Engine akzeptiert den Anschluss und montiert das Gerät in 30 Zügen (1 Sekunde).\n"
                                + "Sobald meins.bereit() true ist, ruft Zeile 12 geraetBenutzen(); auf und der Schub erhöht das Tempo auf das 2,6-Fache.",
                        "Now we inspect mine.type() and connect it to its valid slot: plugIn(\"back\"); (line 10).\n"
                                + "The engine accepts the connection and takes 30 ticks (1 second) to mount it.\n"
                                + "As soon as mine.ready() is true, line 12 calls useGadget(); and the jet burst boosts speed by 2.6x."),
                10, new int[]{7, 9, 11, 12}, true, 40, 210, Accion.APARATO_BIEN, Resaltado.BIEN, codigo);
    }

    private static Paso paso5GestionEnergia(Trilingue codigo) {
        return new Paso(
                "gestion_energia",
                new Trilingue(
                        "Gestión de energía: consumo frente a recarga por turno",
                        "Energiemanagement: Verbrauch gegen Aufladung pro Zug",
                        "Energy management: spending vs. per-tick recharge"),
                new Trilingue(
                        "Mira la barra amarilla de energía en el HUD superior.\n"
                                + "Disparar con atacar() y activar especial() consumen energía al instante; si te quedas sin ella, las armas no salen.\n"
                                + "Al guardar una reserva mínima con energia() >= RESERVA_MINIMA (línea 16), "
                                + "la recarga natural por turno recupera energía entre ráfagas.",
                        "Schau oben im HUD auf den gelben Energiebalken.\n"
                                + "Schießen mit angreifen() und das Aktivieren von spezial() verbrauchen sofort Energie; ohne sie feuern die Waffen nicht.\n"
                                + "Wenn du mit energie() >= MINDESTRESERVE (Zeile 16) eine Reserve hältst, "
                                + "füllt die natürliche Aufladung pro Zug deine Energie zwischen Salven wieder auf.",
                        "Watch the yellow energy bar in the top HUD.\n"
                                + "Firing with attack() and triggering special() spend energy immediately; if it runs dry, weapons will not fire.\n"
                                + "Keeping a reserve with energy() >= MIN_RESERVE (line 16) lets natural per-tick recharge "
                                + "recover your energy between bursts."),
                16, new int[]{5, 17, 19}, true, 25, 120, Accion.GESTION_ENERGIA, Resaltado.BIEN, codigo);
    }

    private static Paso paso6TopeDeuda(Trilingue codigo) {
        return new Paso(
                "tope_deuda",
                new Trilingue(
                        "Tope de cálculo (TopeDeCalculo) y deuda de turnos",
                        "Rechenlimit (TopeDeCalculo) und Zugschuld",
                        "Step limit (TopeDeCalculo) and tick debt"),
                new Trilingue(
                        "Cada turno tu código tiene un presupuesto limitado de pasos de cálculo.\n"
                                + "El bucle anidado 200 × 200 de las líneas 7 y 8 supera ese tope: el motor corta la ejecución con TopeDeCalculo.\n"
                                + "Como penalización, el robot contrae deuda de turnos y queda bloqueado sin ejecutar tu código hasta saldarla.",
                        "In jedem Zug hat dein Code ein begrenztes Budget an Rechenschritten.\n"
                                + "Die verschachtelte 200-×-200-Schleife in den Zeilen 7 und 8 überschreitet dieses Limit: "
                                + "Die Engine bricht den Zug mit TopeDeCalculo ab.\n"
                                + "Als Strafe entsteht Zugschuld, und der Roboter bleibt ohne Code-Ausführung blockiert, bis sie abgebaut ist.",
                        "On every tick your code has a finite budget of computation steps.\n"
                                + "The nested 200 × 200 loop on lines 7 and 8 blows past that cap: the engine aborts the tick with TopeDeCalculo.\n"
                                + "As a penalty, the robot incurs tick debt and stays locked out of running your code until the debt is paid off."),
                8, new int[]{7, 9}, true, 8, 45, Accion.TOPE_DEUDA, Resaltado.ERROR, codigo);
    }

    private static Paso paso7TopeOptimo(Trilingue codigo) {
        return new Paso(
                "tope_optimizado",
                new Trilingue(
                        "Versión optimizada dentro del presupuesto de pasos de cálculo",
                        "Optimierte Version innerhalb des Rechenschritt-Budgets",
                        "Optimized version within the step budget"),
                new Trilingue(
                        "Al eliminar el bucle innecesario, cadaTurno() hace trabajo directo O(1) con enemigosVistos() "
                                + "y entra de sobra en el tope de cálculo.\n"
                                + "Además, private final ERROR_MAX (línea 5) añade margen extra al presupuesto.\n"
                                + "Sin TopeDeCalculo ni deuda de turnos, el robot responde en todos los turnos, alinea la mira y acierta al muñeco.",
                        "Ohne die unnötige Schleife erledigt jedenZug() direkte O(1)-Arbeit mit gesehen() "
                                + "und bleibt locker im Rechenschritt-Budget.\n"
                                + "Zudem gibt private final MAX_FEHLER (Zeile 5) zusätzlichen Spielraum beim Limit.\n"
                                + "Ohne TopeDeCalculo und Zugschuld reagiert der Roboter in jedem Zug, richtet das Visier aus und trifft die Puppe.",
                        "By removing the wasteful loop, onTick() does direct O(1) work with seenEnemies() "
                                + "and stays well inside the step budget.\n"
                                + "On top of that, private final MAX_ERROR (line 5) adds extra step headroom.\n"
                                + "With zero TopeDeCalculo cuts and zero tick debt, the robot acts every tick, lines up its sight, and hits the dummy."),
                16, new int[]{5, 9, 15, 17}, true, 12, 110, Accion.TOPE_OPTIMO, Resaltado.BIEN, codigo);
    }

    private static Paso paso8CierreArquitectura(Trilingue codigo) {
        return new Paso(
                "resumen_arquitectura",
                new Trilingue(
                        "Arquitectura Java = rendimiento real en la arena",
                        "Java-Architektur = echte Leistung in der Arena",
                        "Java architecture = real arena performance"),
                new Trilingue(
                        "En MetajavBots el buen diseño Java se nota físicamente en la pelea:\n"
                                + "1. Encapsula con private, fija constantes con final, coordina equipos con static y evita campos public mutables.\n"
                                + "2. Recoge aparatos, revisa miAparato().tipo() y conéctalos en su hueco real.\n"
                                + "3. Cuida tu energia() y evita bucles pesados para no sufrir TopeDeCalculo ni deuda de turnos; "
                                + "aplícalo ahora en el [[Taller]] y los [[Ejercicios]].",
                        "In MetajavBots wirkt sich sauberes Java-Design physisch im Kampf aus:\n"
                                + "1. Kapsle mit private, setze Konstanten mit final, koordiniere Teams mit static und meide veränderbare public-Felder.\n"
                                + "2. Hebe Geräte auf, prüfe meinGeraet().typ() und schließe sie am richtigen Platz an.\n"
                                + "3. Achte auf deine energie() und vermeide schwere Schleifen, um TopeDeCalculo und Zugschuld zu entgehen; "
                                + "nutze es jetzt in der [[Werkstatt]] und den [[Übungen]].",
                        "In MetajavBots, clean Java design translates directly into physical performance in fights:\n"
                                + "1. Encapsulate with private, lock constants with final, coordinate teams with static, and avoid mutable public fields.\n"
                                + "2. Pick up gadgets, inspect myGadget().type(), and plug them into their valid slot.\n"
                                + "3. Watch your energy() and avoid heavy loops to prevent TopeDeCalculo and tick debt; "
                                + "apply it now in the [[Workshop]] and [[Exercises]]."),
                5, new int[]{9, 15, 16, 17}, true, 0, 0, Accion.CIERRE, Resaltado.BIEN, codigo);
    }

    // ================================================================ Código POO expuesto

    private static String codigoPooExpuestoEs() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    public int fase = 0;\n"
                + "    public double distanciaObjetivo = 120;\n"
                + "\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        fase++;\n"
                + "        if (enemigosVistos().isEmpty()) {\n"
                + "            apuntar(15);\n"
                + "        } else {\n"
                + "            atacar();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoPooExpuestoDe() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    public int phase = 0;\n"
                + "    public double zielAbstand = 120;\n"
                + "\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        phase++;\n"
                + "        if (gesehen().isEmpty()) {\n"
                + "            zielen(15);\n"
                + "        } else {\n"
                + "            angreifen();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoPooExpuestoEn() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    public int phase = 0;\n"
                + "    public double targetDistance = 120;\n"
                + "\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        phase++;\n"
                + "        if (seenEnemies().isEmpty()) {\n"
                + "            aim(15);\n"
                + "        } else {\n"
                + "            attack();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    // ================================================================ Código POO encapsulado

    private static String codigoPooEncapsuladoEs() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    private int fase = 0;\n"
                + "    private double ultimoAngulo = 0;\n"
                + "    private final int UMBRAL_GIRO = 15;\n"
                + "    private final double DISTANCIA_SEGURA = 120;\n"
                + "    private static int senalEquipo = 0;\n"
                + "\n"
                + "    private void actualizarEstado() {\n"
                + "        fase++;\n"
                + "        senalEquipo = fase;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        actualizarEstado();\n"
                + "        if (enemigosVistos().isEmpty()) {\n"
                + "            apuntar(UMBRAL_GIRO);\n"
                + "        } else if (DISTANCIA_SEGURA > 0) {\n"
                + "            ultimoAngulo = mira();\n"
                + "            atacar();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoPooEncapsuladoDe() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    private int phase = 0;\n"
                + "    private double letzterWinkel = 0;\n"
                + "    private final int DREH_SCHRITT = 15;\n"
                + "    private final double SICHERER_ABSTAND = 120;\n"
                + "    private static int teamSignal = 0;\n"
                + "\n"
                + "    private void zustandAktualisieren() {\n"
                + "        phase++;\n"
                + "        teamSignal = phase;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        zustandAktualisieren();\n"
                + "        if (gesehen().isEmpty()) {\n"
                + "            zielen(DREH_SCHRITT);\n"
                + "        } else if (SICHERER_ABSTAND > 0) {\n"
                + "            letzterWinkel = blick();\n"
                + "            angreifen();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoPooEncapsuladoEn() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    private int phase = 0;\n"
                + "    private double lastAngle = 0;\n"
                + "    private final int SWEEP_STEP = 15;\n"
                + "    private final double SAFE_DISTANCE = 120;\n"
                + "    private static int teamSignal = 0;\n"
                + "\n"
                + "    private void updateState() {\n"
                + "        phase++;\n"
                + "        teamSignal = phase;\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        updateState();\n"
                + "        if (seenEnemies().isEmpty()) {\n"
                + "            aim(SWEEP_STEP);\n"
                + "        } else if (SAFE_DISTANCE > 0) {\n"
                + "            lastAngle = aimDirection();\n"
                + "            attack();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    // ================================================================ Código aparato (hueco erróneo)

    private static String codigoAparatoErrorEs() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        Aparato mio = miAparato();\n"
                + "        if (mio != null) {\n"
                + "            if (!mio.conectado()) {\n"
                + "                conectar(\"cabeza\");\n"
                + "            }\n"
                + "            return;\n"
                + "        }\n"
                + "        List<Aparato> suelo = aparatos();\n"
                + "        if (!suelo.isEmpty()) {\n"
                + "            Aparato a = suelo.get(0);\n"
                + "            girarHacia(a.angulo());\n"
                + "            avanzar(a.distancia());\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAparatoErrorDe() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        Geraet meins = meinGeraet();\n"
                + "        if (meins != null) {\n"
                + "            if (!meins.angeschlossen()) {\n"
                + "                anschliessen(\"kopf\");\n"
                + "            }\n"
                + "            return;\n"
                + "        }\n"
                + "        List<Geraet> boden = geraete();\n"
                + "        if (!boden.isEmpty()) {\n"
                + "            Geraet g = boden.get(0);\n"
                + "            drehenNach(g.winkel());\n"
                + "            vorwaerts(g.entfernung());\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAparatoErrorEn() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        Gadget mine = myGadget();\n"
                + "        if (mine != null) {\n"
                + "            if (!mine.pluggedIn()) {\n"
                + "                plugIn(\"head\");\n"
                + "            }\n"
                + "            return;\n"
                + "        }\n"
                + "        List<Gadget> floor = gadgets();\n"
                + "        if (!floor.isEmpty()) {\n"
                + "            Gadget g = floor.get(0);\n"
                + "            turnTo(g.angle());\n"
                + "            ahead(g.distance());\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    // ================================================================ Código aparato (hueco correcto)

    private static String codigoAparatoBienEs() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        Aparato mio = miAparato();\n"
                + "        if (mio != null) {\n"
                + "            if (!mio.conectado() && \"chorro\".equals(mio.tipo())) {\n"
                + "                conectar(\"espalda\");\n"
                + "            } else if (mio.listo()) {\n"
                + "                usarAparato();\n"
                + "            }\n"
                + "            return;\n"
                + "        }\n"
                + "        List<Aparato> suelo = aparatos();\n"
                + "        if (!suelo.isEmpty()) {\n"
                + "            Aparato a = suelo.get(0);\n"
                + "            girarHacia(a.angulo());\n"
                + "            avanzar(a.distancia());\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAparatoBienDe() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        Geraet meins = meinGeraet();\n"
                + "        if (meins != null) {\n"
                + "            if (!meins.angeschlossen() && \"chorro\".equals(meins.typ())) {\n"
                + "                anschliessen(\"ruecken\");\n"
                + "            } else if (meins.bereit()) {\n"
                + "                geraetBenutzen();\n"
                + "            }\n"
                + "            return;\n"
                + "        }\n"
                + "        List<Geraet> boden = geraete();\n"
                + "        if (!boden.isEmpty()) {\n"
                + "            Geraet g = boden.get(0);\n"
                + "            drehenNach(g.winkel());\n"
                + "            vorwaerts(g.entfernung());\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAparatoBienEn() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        Gadget mine = myGadget();\n"
                + "        if (mine != null) {\n"
                + "            if (!mine.pluggedIn() && \"chorro\".equals(mine.type())) {\n"
                + "                plugIn(\"back\");\n"
                + "            } else if (mine.ready()) {\n"
                + "                useGadget();\n"
                + "            }\n"
                + "            return;\n"
                + "        }\n"
                + "        List<Gadget> floor = gadgets();\n"
                + "        if (!floor.isEmpty()) {\n"
                + "            Gadget g = floor.get(0);\n"
                + "            turnTo(g.angle());\n"
                + "            ahead(g.distance());\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    // ================================================================ Código gestión de energía

    private static String codigoEnergiaEs() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    private final double RESERVA_MINIMA = 30.0;\n"
                + "\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        List<Enemigo> vistos = enemigosVistos();\n"
                + "        if (vistos.isEmpty()) {\n"
                + "            apuntar(15);\n"
                + "            return;\n"
                + "        }\n"
                + "        Enemigo e = vistos.get(0);\n"
                + "        apuntarHacia(e.angulo());\n"
                + "        if (energia() >= RESERVA_MINIMA) {\n"
                + "            atacar();\n"
                + "            if (listoEspecial()) {\n"
                + "                especial();\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoEnergiaDe() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    private final double MINDESTRESERVE = 30.0;\n"
                + "\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        List<Gegner> liste = gesehen();\n"
                + "        if (liste.isEmpty()) {\n"
                + "            zielen(15);\n"
                + "            return;\n"
                + "        }\n"
                + "        Gegner e = liste.get(0);\n"
                + "        zielenNach(e.winkel());\n"
                + "        if (energie() >= MINDESTRESERVE) {\n"
                + "            angreifen();\n"
                + "            if (bereitSpezial()) {\n"
                + "                spezial();\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoEnergiaEn() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    private final double MIN_RESERVE = 30.0;\n"
                + "\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        List<Enemy> seen = seenEnemies();\n"
                + "        if (seen.isEmpty()) {\n"
                + "            aim(15);\n"
                + "            return;\n"
                + "        }\n"
                + "        Enemy e = seen.get(0);\n"
                + "        aimTo(e.angle());\n"
                + "        if (energy() >= MIN_RESERVE) {\n"
                + "            attack();\n"
                + "            if (readySpecial()) {\n"
                + "                special();\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    // ================================================================ Código tope de cálculo (lento)

    private static String codigoTopeLentoEs() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        double suma = 0;\n"
                + "        for (int i = 0; i < 200; i++) {\n"
                + "            for (int j = 0; j < 200; j++) {\n"
                + "                suma += (i + j) * 0.01;\n"
                + "            }\n"
                + "        }\n"
                + "        if (suma > 0 && !enemigosVistos().isEmpty()) {\n"
                + "            Enemigo e = enemigosVistos().get(0);\n"
                + "            apuntarHacia(e.angulo());\n"
                + "            atacar();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoTopeLentoDe() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        double summe = 0;\n"
                + "        for (int i = 0; i < 200; i++) {\n"
                + "            for (int j = 0; j < 200; j++) {\n"
                + "                summe += (i + j) * 0.01;\n"
                + "            }\n"
                + "        }\n"
                + "        if (summe > 0 && !gesehen().isEmpty()) {\n"
                + "            Gegner e = gesehen().get(0);\n"
                + "            zielenNach(e.winkel());\n"
                + "            angreifen();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoTopeLentoEn() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        double sum = 0;\n"
                + "        for (int i = 0; i < 200; i++) {\n"
                + "            for (int j = 0; j < 200; j++) {\n"
                + "                sum += (i + j) * 0.01;\n"
                + "            }\n"
                + "        }\n"
                + "        if (sum > 0 && !seenEnemies().isEmpty()) {\n"
                + "            Enemy e = seenEnemies().get(0);\n"
                + "            aimTo(e.angle());\n"
                + "            attack();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    // ================================================================ Código tope optimizado

    private static String codigoTopeOptimoEs() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    private final double ERROR_MAX = 8.0;\n"
                + "\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        List<Enemigo> vistos = enemigosVistos();\n"
                + "        if (vistos.isEmpty()) {\n"
                + "            apuntar(15);\n"
                + "            return;\n"
                + "        }\n"
                + "        Enemigo e = vistos.get(0);\n"
                + "        apuntarHacia(e.angulo());\n"
                + "        if (Math.abs(normalizar(e.angulo() - mira())) < ERROR_MAX) {\n"
                + "            atacar();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoTopeOptimoDe() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    private final double MAX_FEHLER = 8.0;\n"
                + "\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        List<Gegner> liste = gesehen();\n"
                + "        if (liste.isEmpty()) {\n"
                + "            zielen(15);\n"
                + "            return;\n"
                + "        }\n"
                + "        Gegner e = liste.get(0);\n"
                + "        zielenNach(e.winkel());\n"
                + "        if (Math.abs(normalisieren(e.winkel() - blick())) < MAX_FEHLER) {\n"
                + "            angreifen();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoTopeOptimoEn() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    private final double MAX_ERROR = 8.0;\n"
                + "\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        List<Enemy> seen = seenEnemies();\n"
                + "        if (seen.isEmpty()) {\n"
                + "            aim(15);\n"
                + "            return;\n"
                + "        }\n"
                + "        Enemy e = seen.get(0);\n"
                + "        aimTo(e.angle());\n"
                + "        if (Math.abs(normalize(e.angle() - aimDirection())) < MAX_ERROR) {\n"
                + "            attack();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }
}
