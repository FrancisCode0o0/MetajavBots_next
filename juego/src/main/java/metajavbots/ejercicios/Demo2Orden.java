package metajavbots.ejercicios;

import java.util.List;

import metajavbots.ejercicios.Demostracion.Accion;
import metajavbots.ejercicios.Demostracion.Paso;
import metajavbots.ejercicios.Demostracion.Resaltado;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Trilingue;

/**
 * DEMO 2 — Por qué el orden de las líneas lo cambia todo y cómo subir de nivel:
 * muestra en vivo un fallo real causado por una línea fuera de sitio, explica
 * el orden exacto de los bloques en cada turno, mueve la línea al lugar correcto,
 * comprueba el acierto en la arena y enseña el camino de Noob a Junior.
 */
final class Demo2Orden {

    private Demo2Orden() {
    }

    static Demostracion crear() {
        Trilingue titulo = new Trilingue(
                "Demo 2: Por qué el orden de las líneas lo cambia todo",
                "Demo 2: Warum die Reihenfolge der Zeilen alles verändert",
                "Demo 2: Why the order of lines changes everything");
        Trilingue resumen = new Trilingue(
                "Descubre por qué mover una sola línea arruina o arregla un disparo, "
                        + "cómo se ejecutan los bloques en cada turno y cómo subir de Noob a Junior.",
                "Entdecke, warum eine einzige verschobene Zeile einen Schuss verdirbt oder rettet, "
                        + "wie Blöcke pro Zug laufen und wie du von Noob zu Junior aufsteigst.",
                "Discover why moving a single line ruins or fixes a shot, "
                        + "how blocks run each tick, and how to climb from Noob to Junior.");
        Trilingue codigoRoto = new Trilingue(codigoRotoEs(), codigoRotoDe(), codigoRotoEn());
        Trilingue codigoFijo = new Trilingue(codigoFijoEs(), codigoFijoDe(), codigoFijoEn());
        List<Paso> pasos = List.of(
                paso1ReglaReceta(),
                paso2FalloEnVivo(),
                paso3OrdenDelTurno(),
                paso4MoverLinea(codigoFijo),
                paso5PruebaEnArena(codigoFijo),
                paso6NoobAJunior(codigoFijo));
        return new Demostracion(Demostracion.ID_DEMO_2, 2, titulo, resumen, VistaDeArena.Escenario.LABORATORIO, codigoRoto, pasos);
    }

    private static Paso paso1ReglaReceta() {
        return new Paso(
                "regla_receta",
                new Trilingue(
                        "La regla de la receta",
                        "Die Rezept-Regel",
                        "The recipe rule"),
                new Trilingue(
                        "Tu robot lee el código de arriba abajo, paso a paso, igual que te vistes por la mañana: "
                                + "primero te pones los calcetines y después los zapatos.\n"
                                + "Si cambias el orden y te pones los zapatos antes que los calcetines, sale un desastre.\n"
                                + "En el código pasa exactamente lo mismo: "
                                + "mover una sola línea fuera de su sitio puede hacer que el robot falle todos sus tiros.",
                        "Dein Roboter liest den Code von oben nach unten, Schritt für Schritt, genau wie beim Anziehen am Morgen: "
                                + "Zuerst ziehst du die Socken an und erst danach die Schuhe.\n"
                                + "Wenn du die Reihenfolge umdrehst und die Schuhe vor den Socken anziehst, gibt es Chaos.\n"
                                + "Im Code ist es genauso: Eine einzige Zeile am falschen Platz kann dafür sorgen, dass dein Roboter jeden Schuss verfehlt.",
                        "Your robot reads your code from top to bottom, step by step, just like getting dressed in the morning: "
                                + "first you put on your socks, and only then your shoes.\n"
                                + "If you flip the order and put your shoes on before your socks, you get a mess.\n"
                                + "Code works the exact same way: moving a single line out of place can make your robot miss every shot."),
                3, new int[]{5, 11}, true, 0, 0, Accion.QUIETO, Resaltado.BIEN, null);
    }

    private static Paso paso2FalloEnVivo() {
        return new Paso(
                "fallo_en_vivo",
                new Trilingue(
                        "El fallo en vivo",
                        "Der Fehler live in Aktion",
                        "The mistake live in action"),
                new Trilingue(
                        "¡Mira qué acaba de pasar en la arena! El radar vio al muñeco, el robot disparó y el tiro pasó de largo.\n"
                                + "Fíjate en la línea 15 señalada en rojo: apuntar(25); está fuera de las llaves ({}) del if.\n"
                                + "Ese 25 gira el arma 25 grados hacia la izquierda (en sentido contrario a las agujas del reloj) en todos los turnos, "
                                + "aunque el muñeco ya esté delante, y pisa el apuntado de alVerEnemigo().",
                        "Schau, was gerade in der Arena passiert ist! Das Radar sah die Puppe, der Roboter schoss, und der Schuss ging vorbei.\n"
                                + "Sieh dir die rot markierte Zeile 15 an: zielen(25); steht außerhalb der geschweiften Klammern ({}) des if.\n"
                                + "Diese 25 dreht die Waffe in jedem Zug um 25 Grad nach links (gegen den Uhrzeigersinn), "
                                + "selbst wenn die Puppe schon vor dir steht, und überschreibt das Zielen aus beiSicht().",
                        "Look at what just happened in the arena! The radar spotted the dummy, the robot fired, and the shot flew right past it.\n"
                                + "Look at line 15 highlighted in red: aim(25); sits outside the curly braces ({}) of the if.\n"
                                + "That 25 turns the weapon 25 degrees to the left (counterclockwise) on every tick, "
                                + "even when the dummy is right in front of you, overwriting the aim from onSeeEnemy()."),
                15, new int[]{6, 7}, true, 12, 90, Accion.FALLO_ORDEN, Resaltado.ERROR, null);
    }

    private static Paso paso3OrdenDelTurno() {
        return new Paso(
                "orden_del_turno",
                new Trilingue(
                        "Qué pasa turno a turno",
                        "Was Zug für Zug passiert",
                        "What happens tick by tick"),
                new Trilingue(
                        "En cada turno, el motor ejecuta los avisos en este orden real: alRecibirGolpe, alAcertar, alChocarConPared, "
                                + "alChocarConEnemigo y alOirAlEquipo; después alVerEnemigo(Enemigo e) por cada rival visible y, al final, cadaTurno().\n"
                                + "Por eso, en el mismo turno, la línea 6 apunta con apuntarHacia(e.angulo());, "
                                + "pero la línea 15 (apuntar(25);) corre después dentro de cadaTurno() y pisa esa orden antes del disparo y el movimiento.\n"
                                + "Es como apuntar con una linterna a un amigo y apartar la mano justo antes de hacer la foto.",
                        "In jedem Zug ruft die Engine die Ereignisse genau in dieser Reihenfolge auf: beiTreffer, beiEigenemTreffer, beiWand, "
                                + "beiZusammenstoss und beiTeamNachricht; danach beiSicht(Gegner e) für jeden sichtbaren Gegner und zuletzt jedenZug().\n"
                                + "Deshalb zielt Zeile 6 mit zielenNach(e.winkel()); auf die Puppe, "
                                + "aber Zeile 15 (zielen(25);) läuft später in jedenZug() und überschreibt diesen Befehl vor Angriff und Bewegung.\n"
                                + "Das ist, als würdest du mit einer Taschenlampe auf einen Freund leuchten und genau vor dem Foto die Hand wegdrehen.",
                        "On every tick, the engine runs callbacks in this exact order: onHit, onHitEnemy, onHitWall, "
                                + "onBump, and onTeamMessage; then onSeeEnemy(Enemy e) for each visible rival, and finally onTick().\n"
                                + "So in the very same tick, line 6 aims with aimTo(e.angle());, "
                                + "but line 15 (aim(25);) runs afterward inside onTick() and overwrites that order before attacks and movement.\n"
                                + "It is like shining a flashlight on a friend and jerking your hand away right before taking the picture."),
                15, new int[]{5, 6, 11}, true, 0, 0, Accion.ORDEN_TURNO, Resaltado.AVISO, null);
    }

    private static Paso paso4MoverLinea(Trilingue codigoFijo) {
        return new Paso(
                "mover_linea",
                new Trilingue(
                        "Mover la línea de sitio",
                        "Die Zeile an den richtigen Platz setzen",
                        "Moving the line into place"),
                new Trilingue(
                        "¡Mira la línea 14 en verde! Hemos subido apuntar(25); una sola línea para meterla dentro de las llaves ({}) "
                                + "de if (enemigosVistos().isEmpty()).\n"
                                + "Ahora el robot solo pide avanzar 40 unidades y gira el radar 25 grados a la izquierda cuando no ve a nadie.\n"
                                + "En cuanto aparece el muñeco, cadaTurno() deja de girar la mira y alVerEnemigo() trabaja en orden lógico: "
                                + "primero apunta con apuntarHacia(e.angulo()); y luego dispara con atacar();.",
                        "Schau auf die grün markierte Zeile 14! Wir haben zielen(25); nur eine Zeile nach oben geschoben, "
                                + "mitten in die geschweiften Klammern ({}) von if (gesehen().isEmpty()).\n"
                                + "Jetzt fordert der Roboter nur dann 40 Einheiten Vorwärtsfahrt an und dreht das Radar um 25 Grad nach links, "
                                + "wenn er niemanden sieht.\n"
                                + "Sobald die Puppe auftaucht, hört jedenZug() auf zu drehen, und beiSicht() arbeitet in logischer Reihenfolge: "
                                + "zuerst zielen mit zielenNach(e.winkel());, dann schießen mit angreifen();.",
                        "Look at line 14 highlighted in green! We moved aim(25); up by just one line so it sits inside the curly braces ({}) "
                                + "of if (seenEnemies().isEmpty()).\n"
                                + "Now the robot only requests 40 units forward and sweeps the radar 25 degrees left when it sees nobody.\n"
                                + "The instant the dummy appears, onTick() stops spinning the sight, and onSeeEnemy() works in logical order: "
                                + "first aim with aimTo(e.angle());, then shoot with attack();."),
                14, new int[]{6, 7, 12}, true, 0, 0, Accion.CORREGIR_LINEA, Resaltado.BIEN, codigoFijo);
    }

    private static Paso paso5PruebaEnArena(Trilingue codigoFijo) {
        return new Paso(
                "prueba_en_arena",
                new Trilingue(
                        "La prueba en la arena",
                        "Der Test in der Arena",
                        "The test in the arena"),
                new Trilingue(
                        "¡Blanco perfecto! Al mover apuntar(25); dentro del if, el robot ya no estropea su propia puntería.\n"
                                + "El cono azul del radar encuentra al muñeco, la mira se queda clavada sobre él y el disparo acierta de lleno.\n"
                                + "Mira la barra de blindaje del muñeco arriba: acaba de bajar con ese impacto. ¡Una sola línea cambiada lo arregló todo!",
                        "Volltreffer! Weil zielen(25); jetzt im if steht, verdirbt der Roboter sich nicht mehr sein eigenes Zielen.\n"
                                + "Der blaue Radarkegel findet die Puppe, das Visier bleibt genau auf ihr liegen, und der Schuss trifft mitten ins Ziel.\n"
                                + "Schau oben auf den Panzerungsbalken der Puppe: Durch diesen Treffer ist er sofort gesunken. "
                                + "Eine einzige verschobene Zeile hat alles repariert!",
                        "Bullseye! With aim(25); moved inside the if, the robot no longer ruins its own aim.\n"
                                + "The blue radar cone finds the dummy, the sight locks straight onto it, and the shot lands cleanly.\n"
                                + "Look at the dummy's armor bar at the top: it just dropped from that hit. Moving one single line fixed everything!"),
                7, new int[]{6, 14}, true, 12, 110, Accion.ACIERTO_ORDEN, Resaltado.BIEN, codigoFijo);
    }

    private static Paso paso6NoobAJunior(Trilingue codigoFijo) {
        return new Paso(
                "noob_a_junior",
                new Trilingue(
                        "Cómo subir de Noob a Junior",
                        "Wie du von Noob zu Junior aufsteigst",
                        "How to climb from Noob to Junior"),
                new Trilingue(
                        "Ya entiendes cómo piensa tu robot. Para subir de Noob a Junior sigue este mapa paso a paso:\n"
                                + "1. Entra en [[Minijuegos]] para practicar una sola habilidad cada vez (moverse, radar, apuntar, disparar y esquivar).\n"
                                + "2. Haz [[Ejercicios]] cortitos, como rompecabezas rápidos, para ganar monedas y dominar cada tema.\n"
                                + "3. Gasta tus monedas en la [[Tienda]] para comprar piezas nuevas, actívalas superando su pequeño reto de código, "
                                + "móntalas en el [[Taller]] y gana peleas para llegar a Junior.",
                        "Jetzt verstehst du, wie dein Roboter denkt. Um von Noob zu Junior aufzusteigen, folge diesem Plan Schritt für Schritt:\n"
                                + "1. Öffne [[Minispiele]], um immer nur eine Fähigkeit auf einmal zu üben (Fahren, Radar, Zielen, Schießen und Ausweichen).\n"
                                + "2. Löse kurze [[Übungen]], wie kleine Rätsel, um Münzen zu verdienen und jedes Thema zu meistern.\n"
                                + "3. Gib deine Münzen im [[Laden]] für neue Teile aus, schalte sie mit ihrer kurzen Code-Aufgabe frei, "
                                + "bau sie in der [[Werkstatt]] ein und gewinne Kämpfe, um Junior zu erreichen.",
                        "Now you understand how your robot thinks. To climb from Noob to Junior, follow this step-by-step map:\n"
                                + "1. Open [[Minigames]] to practice one skill at a time (moving, radar, aiming, shooting, and dodging).\n"
                                + "2. Solve short [[Exercises]], like quick puzzles, to earn coins and master each topic.\n"
                                + "3. Spend your coins in the [[Shop]] to buy new parts, unlock them with their short code task, "
                                + "equip them in the [[Workshop]], and win fights to reach Junior."),
                3, new int[]{5, 6, 7, 11, 14}, true, 0, 0, Accion.CIERRE, Resaltado.BIEN, codigoFijo);
    }

    private static String codigoRotoEs() {
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
                + "        if (enemigosVistos().isEmpty()) {\n"
                + "            avanzar(40);\n"
                + "        }\n"
                + "        apuntar(25);\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoFijoEs() {
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
                + "        if (enemigosVistos().isEmpty()) {\n"
                + "            avanzar(40);\n"
                + "            apuntar(25);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoRotoDe() {
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
                + "        if (gesehen().isEmpty()) {\n"
                + "            vorwaerts(40);\n"
                + "        }\n"
                + "        zielen(25);\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoFijoDe() {
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
                + "        if (gesehen().isEmpty()) {\n"
                + "            vorwaerts(40);\n"
                + "            zielen(25);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoRotoEn() {
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
                + "        if (seenEnemies().isEmpty()) {\n"
                + "            ahead(40);\n"
                + "        }\n"
                + "        aim(25);\n"
                + "    }\n"
                + "}\n";
    }

    private static String codigoFijoEn() {
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
                + "        if (seenEnemies().isEmpty()) {\n"
                + "            ahead(40);\n"
                + "            aim(25);\n"
                + "        }\n"
                + "    }\n"
                + "}\n";
    }
}
