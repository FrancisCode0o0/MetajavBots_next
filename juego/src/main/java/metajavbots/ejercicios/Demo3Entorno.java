package metajavbots.ejercicios;

import java.util.List;

import metajavbots.ejercicios.Demostracion.Accion;
import metajavbots.ejercicios.Demostracion.Paso;
import metajavbots.ejercicios.Demostracion.Resaltado;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Trilingue;

/**
 * DEMO 3 — Radar, puntería, proyectiles y obstáculos de la arena:
 * enseña cómo leer el entorno en combate (evento frente a lista de enemigos
 * vistos, ángulos absoluto y relativo, distancia frente al alcance del arma,
 * trampas fijas frente a obstáculos destructibles) y compara en vivo un robot
 * ingenuo con una versión ajustada sobre una arena real con obstáculos.
 */
final class Demo3Entorno {

    private Demo3Entorno() {
    }

    static Demostracion crear() {
        Trilingue titulo = new Trilingue(
                "Demo 3: Radar, proyectiles y obstáculos de la arena",
                "Demo 3: Radar, Projektile und Hindernisse der Arena",
                "Demo 3: Radar, projectiles, and arena obstacles");
        Trilingue resumen = new Trilingue(
                "Aprende a combinar el radar, el alcance y la puntería con la lectura de "
                        + "trampas fijas y obstáculos destructibles de la arena.",
                "Lerne, Radar, Reichweite und Zielen mit dem Erkennen fester Fallen "
                        + "und zerstörbarer Hindernisse in der Arena zu verbinden.",
                "Learn to combine radar, weapon range, and aiming with reading fixed "
                        + "traps and destructible obstacles in the arena.");
        Trilingue codigoIngenuo = new Trilingue(codigoIngenuoEs(), codigoIngenuoDe(), codigoIngenuoEn());
        Trilingue codigoAjustado = new Trilingue(codigoAjustadoEs(), codigoAjustadoDe(), codigoAjustadoEn());
        List<Paso> pasos = List.of(
                paso1RadarEventoYLista(),
                paso2PunteriaYAlcance(),
                paso3ObstaculosYTrampas(),
                paso4IngenuoEnVivo(),
                paso5AjustarCodigo(codigoAjustado),
                paso6AjustadoEnVivo(codigoAjustado),
                paso7CierreEntorno(codigoAjustado));
        return new Demostracion(Demostracion.ID_DEMO_3, 3, titulo, resumen,
                VistaDeArena.Escenario.CHATARRERIA, codigoIngenuo, pasos);
    }

    private static Paso paso1RadarEventoYLista() {
        return new Paso(
                "radar_evento_lista",
                new Trilingue(
                        "Radar: evento frente a lista completa",
                        "Radar: Ereignis oder ganze Liste",
                        "Radar: event vs. full list"),
                new Trilingue(
                        "Tu robot tiene dos formas complementarias de leer el radar en cada turno.\n"
                                + "alVerEnemigo(Enemigo e) es un aviso automático que salta una vez por cada rival visible "
                                + "en ese instante: sirve para reaccionar al vuelo.\n"
                                + "enemigosVistos() devuelve la lista completa dentro de cadaTurno(), ordenada del rival "
                                + "más cercano al más lejano: así sabes si está vacía para girar el radar o elegir al primero.",
                        "Dein Roboter hat zwei sich ergänzende Wege, das Radar in jedem Zug auszulesen.\n"
                                + "beiSicht(Gegner e) ist ein automatisches Ereignis, das einmal pro sichtbarem Gegner "
                                + "ausgelöst wird: ideal für schnelle Reaktionen.\n"
                                + "gesehen() liefert in jedenZug() die vollständige Liste, sortiert vom nächsten zum "
                                + "fernsten Gegner: So prüfst du mit isEmpty(), ob du suchen musst, oder wählst den nächsten.",
                        "Your robot has two complementary ways to read the radar on every tick.\n"
                                + "onSeeEnemy(Enemy e) is an automatic event triggered once per visible rival "
                                + "right away: it is great for instant reactions.\n"
                                + "seenEnemies() returns the full list inside onTick(), sorted from closest to farthest: "
                                + "that lets you check isEmpty() to sweep the radar or pick the nearest target."),
                5, new int[]{11, 13, 14}, true, 0, 0, Accion.QUIETO, Resaltado.BIEN, null);
    }

    private static Paso paso2PunteriaYAlcance() {
        return new Paso(
                "punteria_y_alcance",
                new Trilingue(
                        "Ángulos, distancia y alcance del arma",
                        "Winkel, Entfernung und Waffenreichweite",
                        "Angles, distance, and weapon range"),
                new Trilingue(
                        "e.angulo() es un ángulo absoluto en la arena, mientras que el error relativo frente a tu mira "
                                + "se calcula con normalizar(e.angulo() - mira()).\n"
                                + "Como la mira gira poco a poco cada turno, disparar antes de alinear desperdicia energía y recarga.\n"
                                + "Además, los proyectiles viajan a velocidad finita y desaparecen al superar el alcance del arma "
                                + "(alcanceIzquierda() y alcanceDerecha()): compara siempre e.distancia() con tu alcance.",
                        "e.winkel() ist ein absoluter Winkel in der Arena, während du den relativen Fehler zu deinem Visier "
                                + "mit normalisieren(e.winkel() - blick()) berechnest.\n"
                                + "Da sich das Visier pro Zug nur schrittweise dreht, verschwendet ein Schuss vor dem Ausrichten Energie und Ladezeit.\n"
                                + "Zudem fliegen Projektile mit endlicher Geschwindigkeit und enden hinter der Waffenreichweite "
                                + "(reichweiteLinks() und reichweiteRechts()): Vergleiche stets e.entfernung() mit deiner Reichweite.",
                        "e.angle() is an absolute angle in the arena, while the relative error from your sight "
                                + "is computed with normalize(e.angle() - aimDirection()).\n"
                                + "Because your sight turns gradually each tick, firing before aligning wastes energy and reload time.\n"
                                + "Projectiles also travel at finite speed and vanish past weapon range "
                                + "(rangeLeft() and rangeRight()): always compare e.distance() against your range."),
                6, new int[]{5, 7}, true, 0, 0, Accion.QUIETO, Resaltado.AVISO, null);
    }

    private static Paso paso3ObstaculosYTrampas() {
        return new Paso(
                "obstaculos_arena",
                new Trilingue(
                        "Trampas fijas y objetos destructibles",
                        "Feste Fallen und zerstörbare Objekte",
                        "Fixed traps and destructible objects"),
                new Trilingue(
                        "En arenas con elementos, obstaculos() te informa de todo lo que hay alrededor.\n"
                                + "Las trampas fijas (pinchos, sierras, foso o pegamento) no se rompen: "
                                + "comprueba o.peligroso() y esquívalas antes de pisarlas.\n"
                                + "En cambio, los objetos sólidos destructibles como barriles y chimeneas (o.solido()) "
                                + "detienen balas, láseres y tu avance, pero puedes romperlos a tiros o impactos para abrir paso.",
                        "In Arenen mit Elementen meldet dir hindernisse() alles in deiner Umgebung.\n"
                                + "Feste Fallen (Stacheln, Sägen, Grube oder Kleber) lassen sich nicht zerstören: "
                                + "Prüfe o.gefaehrlich() und weiche ihnen rechtzeitig aus.\n"
                                + "Feste zerstörbare Objekte wie Fässer und Schornsteine (o.fest()) blockieren dagegen "
                                + "Kugeln, Laser und deine Fahrt, lassen sich aber durch Schüsse oder Treffer aufbrechen, um den Weg freizumachen.",
                        "In arenas with hazards, obstacles() reports everything around you.\n"
                                + "Fixed traps (spikes, saws, pit, or glue) cannot be destroyed: "
                                + "check o.dangerous() and dodge them before stepping in.\n"
                                + "By contrast, solid destructible objects such as barrels and chimneys (o.solid()) "
                                + "block bullets, lasers, and movement, yet you can break them with shots or impacts to clear a path."),
                12, new int[]{7, 11}, true, 0, 0, Accion.QUIETO, Resaltado.AVISO, null);
    }

    private static Paso paso4IngenuoEnVivo() {
        return new Paso(
                "entorno_ingenuo",
                new Trilingue(
                        "En vivo: robot ingenuo que ignora el entorno",
                        "Live: Naiver Roboter ignoriert die Umgebung",
                        "Live: Naive robot ignoring the environment"),
                new Trilingue(
                        "¡Mira lo que ocurre con el código ingenuo en la chatarrería!\n"
                                + "La línea 7 llama a atacar(); en cuanto ve al muñeco, sin comprobar si la mira ya está alineada "
                                + "ni si hay un barril en medio, mientras avanzar(60); lo empuja de frente contra el obstáculo.\n"
                                + "El primer tiro sale desviado y los siguientes chocan contra el barril: "
                                + "el blindaje del muñeco sigue al 100 %.",
                        "Schau, was mit dem naiven Code auf dem Schrottplatz passiert!\n"
                                + "Zeile 7 ruft sofort angreifen(); auf, ohne zu prüfen, ob das Visier schon ausgerichtet ist "
                                + "oder ein Fass im Weg steht, während vorwaerts(60); ihn direkt gegen das Hindernis fährt.\n"
                                + "Der erste Schuss geht daneben, die nächsten prallen am Fass ab: "
                                + "Die Panzerung der Puppe bleibt bei 100 %.",
                        "Watch what happens with the naive code in the junkyard!\n"
                                + "Line 7 calls attack(); the instant it spots the dummy, without checking whether the sight is aligned "
                                + "or a barrel blocks the shot, while ahead(60); drives straight into the obstacle.\n"
                                + "The first shot flies wide and the next ones slam into the barrel: "
                                + "the dummy's armor stays at 100%."),
                7, new int[]{6, 12}, true, 16, 28, Accion.ENTORNO_INGENUO, Resaltado.ERROR, null);
    }

    private static Paso paso5AjustarCodigo(Trilingue codigoAjustado) {
        return new Paso(
                "ajustar_entorno",
                new Trilingue(
                        "Ajustar el código con radar y obstáculos",
                        "Den Code mit Radar und Hindernissen anpassen",
                        "Adjusting the code with radar and obstacles"),
                new Trilingue(
                        "Ahora el robot evalúa el entorno antes de disparar o moverse.\n"
                                + "Primero recorre obstaculos(): si detecta una trampa peligrosa cerca (o.peligroso()), la esquiva; "
                                + "si encuentra un obstáculo sólido delante (o.solido()), apunta con precisión y lo rompe.\n"
                                + "Después consulta enemigosVistos(), calcula su alcance real y el error relativo de mira, "
                                + "y solo dispara cuando el objetivo está a su alcance y alineado.",
                        "Jetzt wertet der Roboter die Umgebung aus, bevor er schießt oder fährt.\n"
                                + "Zuerst geht er hindernisse() durch: Bei einer nahen gefährlichen Falle (o.gefaehrlich()) weicht er aus; "
                                + "bei einem festen Hindernis davor (o.fest()) zielt er genau und zerstört es.\n"
                                + "Danach liest er gesehen(), prüft seine echte Reichweite und den relativen Zielfehler "
                                + "und schießt erst, wenn das Ziel in Reichweite und sauber ausgerichtet ist.",
                        "Now the robot evaluates its surroundings before shooting or moving.\n"
                                + "First it loops over obstacles(): if a dangerous trap is close (o.dangerous()), it steers away; "
                                + "if a solid obstacle stands in front (o.solid()), it aims carefully and breaks it.\n"
                                + "Then it checks seenEnemies(), compares its real weapon range and relative aim error, "
                                + "and fires only when the target is in range and lined up."),
                7, new int[]{8, 13, 21, 27, 29, 30}, true, 0, 0, Accion.AJUSTAR_ENTORNO, Resaltado.BIEN, codigoAjustado);
    }

    private static Paso paso6AjustadoEnVivo(Trilingue codigoAjustado) {
        return new Paso(
                "entorno_ajustado",
                new Trilingue(
                        "En vivo: abrir paso y acertar al rival",
                        "Live: Weg freiräumen und den Gegner treffen",
                        "Live: Clearing the path and hitting the rival"),
                new Trilingue(
                        "¡Gran diferencia! El robot espera a alinear la mira (error < 8°), destruye el barril que bloqueaba "
                                + "la línea de tiro y este desaparece de obstaculos().\n"
                                + "Con el camino libre, lee enemigosVistos(), comprueba que el muñeco está dentro del alcance "
                                + "y acierta de lleno: fíjate arriba cómo baja la barra de blindaje del muñeco.",
                        "Was für ein Unterschied! Der Roboter wartet, bis das Visier ausgerichtet ist (Fehler < 8°), "
                                + "zerstört das blockierende Fass, und es verschwindet aus hindernisse().\n"
                                + "Sobald die Bahn frei ist, liest er gesehen(), prüft die Reichweite "
                                + "und trifft sauber: Sieh oben, wie der Panzerungsbalken der Puppe sinkt.",
                        "What a difference! The robot waits until its sight is aligned (error < 8°), destroys the barrel "
                                + "blocking the line of fire, and the broken barrel disappears from obstacles().\n"
                                + "With the lane clear, it reads seenEnemies(), verifies the dummy is within range, "
                                + "and lands a clean hit: watch the dummy's armor bar drop at the top."),
                30, new int[]{13, 16, 21, 31}, true, 20, 220, Accion.ENTORNO_AJUSTADO, Resaltado.BIEN, codigoAjustado);
    }

    private static Paso paso7CierreEntorno(Trilingue codigoAjustado) {
        return new Paso(
                "resumen_entorno",
                new Trilingue(
                        "Leer la arena antes de actuar",
                        "Die Arena lesen, bevor du handelst",
                        "Reading the arena before acting"),
                new Trilingue(
                        "Ya dominas las tres claves para pelear con cabeza en cualquier escenario:\n"
                                + "1. Combina alVerEnemigo() y enemigosVistos() para no perder nunca de vista al rival.\n"
                                + "2. Verifica error relativo de mira y alcance antes de disparar tus proyectiles.\n"
                                + "3. Usa obstaculos() para esquivar trampas fijas y romper barriles o chimeneas; "
                                + "pruébalo ahora en el [[Taller]] y en los [[Ejercicios]].",
                        "Jetzt beherrschst du die drei Schlüssel für kluge Kämpfe in jeder Arena:\n"
                                + "1. Kombiniere beiSicht() und gesehen(), damit du Gegner nie aus den Augen verlierst.\n"
                                + "2. Prüfe relativen Zielfehler und Reichweite, bevor du deine Projektile abfeuerst.\n"
                                + "3. Nutze hindernisse(), um festen Fallen auszuweichen und Fässer oder Schornsteine zu zerstören; "
                                + "teste es jetzt in der [[Werkstatt]] und in den [[Übungen]].",
                        "You now know the three keys to fighting smart in any arena:\n"
                                + "1. Combine onSeeEnemy() and seenEnemies() so you never lose track of a rival.\n"
                                + "2. Check relative aim error and weapon range before firing your projectiles.\n"
                                + "3. Use obstacles() to dodge fixed traps and break barrels or chimneys; "
                                + "try it now in the [[Workshop]] and in [[Exercises]]."),
                6, new int[]{7, 8, 13, 21, 30}, true, 0, 0, Accion.CIERRE, Resaltado.BIEN, codigoAjustado);
    }

    private static String codigoIngenuoEs() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void alVerEnemigo(Enemigo e) {\n"
                + "        apuntarHacia(e.angulo());\n"
                + "        atacar();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        avanzar(60);\n"
                + "        if (enemigosVistos().isEmpty()) {\n"
                + "            apuntar(20);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoIngenuoDe() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    @Override\n"
                + "    public void beiSicht(Gegner e) {\n"
                + "        zielenNach(e.winkel());\n"
                + "        angreifen();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        vorwaerts(60);\n"
                + "        if (gesehen().isEmpty()) {\n"
                + "            zielen(20);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoIngenuoEn() {
        return "import metajavbots.api.*;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    @Override\n"
                + "    public void onSeeEnemy(Enemy e) {\n"
                + "        aimTo(e.angle());\n"
                + "        attack();\n"
                + "    }\n"
                + "\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        ahead(60);\n"
                + "        if (seenEnemies().isEmpty()) {\n"
                + "            aim(20);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAjustadoEs() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MiRobot extends Metajavbot {\n"
                + "    @Override\n"
                + "    public void cadaTurno() {\n"
                + "        for (Obstaculo o : obstaculos()) {\n"
                + "            if (o.peligroso() && o.distancia() < 150) {\n"
                + "                girarHacia(o.angulo() + 90);\n"
                + "                avanzar(60);\n"
                + "                return;\n"
                + "            }\n"
                + "            if (o.solido() && o.distancia() < 180) {\n"
                + "                apuntarHacia(o.angulo());\n"
                + "                if (Math.abs(normalizar(o.angulo() - mira())) < 8) {\n"
                + "                    atacar();\n"
                + "                }\n"
                + "                return;\n"
                + "            }\n"
                + "        }\n"
                + "        List<Enemigo> vistos = enemigosVistos();\n"
                + "        if (vistos.isEmpty()) {\n"
                + "            apuntar(20);\n"
                + "            return;\n"
                + "        }\n"
                + "        Enemigo e = vistos.get(0);\n"
                + "        double alcance = Math.max(alcanceIzquierda(), alcanceDerecha());\n"
                + "        apuntarHacia(e.angulo());\n"
                + "        double error = Math.abs(normalizar(e.angulo() - mira()));\n"
                + "        if (e.distancia() <= alcance && error < 8) {\n"
                + "            atacar();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAjustadoDe() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MeinRoboter extends MetajavbotDe {\n"
                + "    @Override\n"
                + "    public void jedenZug() {\n"
                + "        for (Hindernis o : hindernisse()) {\n"
                + "            if (o.gefaehrlich() && o.entfernung() < 150) {\n"
                + "                drehenNach(o.winkel() + 90);\n"
                + "                vorwaerts(60);\n"
                + "                return;\n"
                + "            }\n"
                + "            if (o.fest() && o.entfernung() < 180) {\n"
                + "                zielenNach(o.winkel());\n"
                + "                if (Math.abs(normalisieren(o.winkel() - blick())) < 8) {\n"
                + "                    angreifen();\n"
                + "                }\n"
                + "                return;\n"
                + "            }\n"
                + "        }\n"
                + "        List<Gegner> liste = gesehen();\n"
                + "        if (liste.isEmpty()) {\n"
                + "            zielen(20);\n"
                + "            return;\n"
                + "        }\n"
                + "        Gegner e = liste.get(0);\n"
                + "        double reichweite = Math.max(reichweiteLinks(), reichweiteRechts());\n"
                + "        zielenNach(e.winkel());\n"
                + "        double fehler = Math.abs(normalisieren(e.winkel() - blick()));\n"
                + "        if (e.entfernung() <= reichweite && fehler < 8) {\n"
                + "            angreifen();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoAjustadoEn() {
        return "import metajavbots.api.*;\n"
                + "import java.util.List;\n"
                + "\n"
                + "public class MyRobot extends MetajavbotEn {\n"
                + "    @Override\n"
                + "    public void onTick() {\n"
                + "        for (Obstacle o : obstacles()) {\n"
                + "            if (o.dangerous() && o.distance() < 150) {\n"
                + "                turnTo(o.angle() + 90);\n"
                + "                ahead(60);\n"
                + "                return;\n"
                + "            }\n"
                + "            if (o.solid() && o.distance() < 180) {\n"
                + "                aimTo(o.angle());\n"
                + "                if (Math.abs(normalize(o.angle() - aimDirection())) < 8) {\n"
                + "                    attack();\n"
                + "                }\n"
                + "                return;\n"
                + "            }\n"
                + "        }\n"
                + "        List<Enemy> seen = seenEnemies();\n"
                + "        if (seen.isEmpty()) {\n"
                + "            aim(20);\n"
                + "            return;\n"
                + "        }\n"
                + "        Enemy e = seen.get(0);\n"
                + "        double range = Math.max(rangeLeft(), rangeRight());\n"
                + "        aimTo(e.angle());\n"
                + "        double error = Math.abs(normalize(e.angle() - aimDirection()));\n"
                + "        if (e.distance() <= range && error < 8) {\n"
                + "            attack();\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }
}
