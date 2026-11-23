package metajavbots.ejercicios;

import java.util.List;

import metajavbots.ejercicios.Demostracion.Accion;
import metajavbots.ejercicios.Demostracion.Paso;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Trilingue;

/**
 * DEMO 5 — Tu primera pelea, en palabras: el robot lo hace en la arena mientras
 * el entrenador lo explica, paso a paso (avanzar, girar, buscar con la cabeza,
 * verlo, apuntar, esperar y atacar, y entre rondas leer el informe y cambiar una
 * sola cosa). A la derecha, en vez de Java, la hoja de órdenes en pseudocódigo
 * (CLARIDAD, 4b: la idea, nunca código para copiar). Las líneas coinciden con
 * las que marca el robot de la demostración (MotorDemostracion): 5-10 el turno,
 * 14-16 el aviso de ver al enemigo.
 */
final class Demo5PrimeraPelea {

    private Demo5PrimeraPelea() {
    }

    static Demostracion crear() {
        Trilingue titulo = new Trilingue(
                "Tu primera pelea, en palabras",
                "Dein erster Kampf, in Worten",
                "Your first fight, in words");
        Trilingue resumen = new Trilingue(
                "Sin Java: la idea de un robot que pelea, en palabras, mientras lo ves hacerlo en la arena. "
                        + "Para quien empieza de cero.",
                "Ohne Java: die Idee eines kämpfenden Roboters, in Worten, während du ihm in der Arena "
                        + "zusiehst. Für alle, die bei null anfangen.",
                "No Java: the idea of a fighting robot, in words, while you watch it in the arena. For those "
                        + "starting from zero.");
        Trilingue hoja = new Trilingue("MI ROBOT (su hoja de órdenes, en palabras)"
                + "\n" + ""
                + "\n" + "gana quien recibe MENOS daño"
                + "\n" + ""
                + "\n" + "EN CADA TURNO:"
                + "\n" + "    avanzar un poco"
                + "\n" + "    girar el cuerpo un poco"
                + "\n" + "    SI no veo a nadie:"
                + "\n" + "        girar la cabeza para buscar"
                + "\n" + "        (la cabeza es como una linterna)"
                + "\n" + ""
                + "\n" + "(los avisos van ANTES que cada turno)"
                + "\n" + ""
                + "\n" + "AL VER al enemigo:"
                + "\n" + "    apuntar la cabeza hacia él"
                + "\n" + "    atacar cuando la mira llegue"
                + "\n" + ""
                + "\n" + "ENTRE RONDAS:"
                + "\n" + "    leer el informe"
                + "\n" + "    cambiar UNA sola cosa"
                + "\n" + "    volver a pelear",
                "MEIN ROBOTER (sein Befehlszettel, in Worten)"
                + "\n" + ""
                + "\n" + "es gewinnt, wer WENIGER Schaden nimmt"
                + "\n" + ""
                + "\n" + "IN JEDEM ZUG:"
                + "\n" + "    ein Stück vorwärts fahren"
                + "\n" + "    den Körper ein wenig drehen"
                + "\n" + "    WENN ich niemanden sehe:"
                + "\n" + "        den Kopf drehen und suchen"
                + "\n" + "        (der Kopf ist wie eine Taschenlampe)"
                + "\n" + ""
                + "\n" + "(die Meldungen kommen VOR jedem Zug)"
                + "\n" + ""
                + "\n" + "BEIM SEHEN des Gegners:"
                + "\n" + "    den Kopf auf ihn richten"
                + "\n" + "    angreifen, wenn das Visier angekommen ist"
                + "\n" + ""
                + "\n" + "ZWISCHEN DEN RUNDEN:"
                + "\n" + "    den Bericht lesen"
                + "\n" + "    nur EINE Sache ändern"
                + "\n" + "    noch einmal kämpfen",
                "MY ROBOT (its order sheet, in words)"
                + "\n" + ""
                + "\n" + "whoever takes LESS damage wins"
                + "\n" + ""
                + "\n" + "EVERY TICK:"
                + "\n" + "    move ahead a little"
                + "\n" + "    turn the body a little"
                + "\n" + "    IF I see nobody:"
                + "\n" + "        turn the head to search"
                + "\n" + "        (the head is like a flashlight)"
                + "\n" + ""
                + "\n" + "(the events come BEFORE every tick)"
                + "\n" + ""
                + "\n" + "WHEN I SEE the enemy:"
                + "\n" + "    point the head at it"
                + "\n" + "    attack when the sight gets there"
                + "\n" + ""
                + "\n" + "BETWEEN ROUNDS:"
                + "\n" + "    read the report"
                + "\n" + "    change just ONE thing"
                + "\n" + "    fight again");
        return new Demostracion(Demostracion.ID_DEMO_5, 1, titulo, resumen, VistaDeArena.Escenario.HANGAR, hoja,
                List.of(intro(), avanzar(), girar(), buscar(), verlo(), apuntar(), mejorar()));
    }

    private static Paso intro() {
        return new Paso(
                "intro",
                new Trilingue(
                "Tu primera pelea, en palabras",
                "Dein erster Kampf, in Worten",
                "Your first fight, in words"),
                new Trilingue(
                "A la derecha está la hoja de órdenes de un robot, pero en palabras, no en Java: así ves la "
                        + "idea sin copiar nada.\nTu robot pelea solo: hace lo que pone ahí. Y gana quien recibe MENOS "
                        + "daño, no quien pega más.",
                "Rechts steht der Befehlszettel eines Roboters, aber in Worten, nicht in Java: So siehst du die "
                        + "Idee, ohne etwas abzuschreiben.\nDein Roboter kämpft allein: Er tut, was dort steht. Und es "
                        + "gewinnt, wer WENIGER Schaden nimmt, nicht wer mehr austeilt.",
                "On the right is a robot’s order sheet, but in words, not in Java: that way you see the idea "
                        + "without copying anything.\nYour robot fights on its own: it does what is written there. And "
                        + "whoever takes LESS damage wins, not whoever hits harder."),
                1, new int[]{3}, false, 0, 0, Accion.QUIETO);
    }

    private static Paso avanzar() {
        return new Paso(
                "avanzar",
                new Trilingue(
                "Avanzar",
                "Vorwärts fahren",
                "Moving ahead"),
                new Trilingue(
                "Mira la arena: el robot avanza. La orden no lo teletransporta: le dice «ve hacia allí» y él va "
                        + "poco a poco, turno a turno.\nAntes de pulsar Continuar, adivina: ¿qué hará ahora con el cuerpo?",
                "Schau in die Arena: Der Roboter fährt. Der Befehl beamt ihn nicht: Er sagt „fahr dorthin“, und "
                        + "er fährt nach und nach, Zug für Zug.\nBevor du Weiter drückst, rate: Was macht er jetzt mit dem "
                        + "Körper?",
                "Look at the arena: the robot moves ahead. The order does not teleport it: it says \"go over "
                        + "there\" and it goes little by little, tick by tick.\nBefore pressing Continue, guess: what will "
                        + "it do with its body now?"),
                6, new int[]{5}, false, 18, 45, Accion.AVANZAR);
    }

    private static Paso girar() {
        return new Paso(
                "girar",
                new Trilingue(
                "Girar el cuerpo",
                "Den Körper drehen",
                "Turning the body"),
                new Trilingue(
                "Ahora gira el cuerpo, también poco a poco. El cuerpo decide hacia dónde avanza.\nLas ruedas, el "
                        + "cuerpo y la cabeza son tres mandos distintos: cada uno se queda con su última orden del turno.",
                "Jetzt dreht er den Körper, auch nach und nach. Der Körper bestimmt, wohin er fährt.\nRäder, "
                        + "Körper und Kopf sind drei verschiedene Regler: Jeder behält seinen letzten Befehl des Zugs.",
                "Now it turns its body, also little by little. The body decides where it moves.\nThe wheels, the "
                        + "body and the head are three different controls: each one keeps its last order of the tick."),
                7, new int[]{5}, false, 18, 45, Accion.GIRAR);
    }

    private static Paso buscar() {
        return new Paso(
                "buscar",
                new Trilingue(
                "Buscar con la cabeza",
                "Mit dem Kopf suchen",
                "Searching with the head"),
                new Trilingue(
                "Si no ve a nadie, gira la cabeza para buscar. La cabeza es como una linterna: solo ve lo que "
                        + "tiene delante y no muy lejos.",
                "Sieht er niemanden, dreht er den Kopf und sucht. Der Kopf ist wie eine Taschenlampe: Er sieht "
                        + "nur, was vor ihm ist, und nicht sehr weit.",
                "If it sees nobody, it turns its head to search. The head is like a flashlight: it only sees "
                        + "what is in front of it and not very far."),
                9, new int[]{8,10}, false, 18, 45, Accion.RADAR);
    }

    private static Paso verlo() {
        return new Paso(
                "verlo",
                new Trilingue(
                "¡Lo ve!",
                "Er sieht ihn!",
                "It sees it!"),
                new Trilingue(
                "En cuanto el enemigo entra en la luz de la linterna, salta el aviso «al ver al enemigo».\nLos "
                        + "avisos se leen ANTES que «en cada turno». Si «en cada turno» le da otra orden a la cabeza, "
                        + "borra la del aviso.",
                "Sobald der Gegner in den Lichtkegel kommt, kommt die Meldung „beim Sehen des "
                        + "Gegners“.\nMeldungen werden VOR „in jedem Zug“ gelesen. Gibt „in jedem Zug“ dem Kopf einen "
                        + "anderen Befehl, löscht er den aus der Meldung.",
                "As soon as the enemy enters the flashlight’s beam, the event \"when I see the enemy\" "
                        + "fires.\nEvents are read BEFORE \"every tick\". If \"every tick\" gives the head another order, it "
                        + "erases the one from the event."),
                14, new int[]{8}, true, 10, 120, Accion.DETECTAR);
    }

    private static Paso apuntar() {
        return new Paso(
                "apuntar",
                new Trilingue(
                "Apuntar, esperar y atacar",
                "Zielen, warten und angreifen",
                "Aim, wait and attack"),
                new Trilingue(
                "La cabeza gira hacia el enemigo despacio, como un búho. El ataque sale hacia donde mira la "
                        + "cabeza EN ESE MOMENTO.\nPor eso espera a que la mira llegue y entonces ataca. Mira cómo acierta.",
                "Der Kopf dreht sich langsam zum Gegner, wie eine Eule. Der Angriff geht dorthin, wohin der "
                        + "Kopf IN DIESEM MOMENT schaut.\nDarum wartet er, bis das Visier angekommen ist, und greift dann "
                        + "an. Schau, wie er trifft.",
                "The head turns toward the enemy slowly, like an owl. The attack goes where the head points AT "
                        + "THAT MOMENT.\nSo it waits for the sight to get there and then attacks. Watch it hit."),
                15, new int[]{14,16}, true, 20, 140, Accion.APUNTAR_Y_DISPARAR);
    }

    private static Paso mejorar() {
        return new Paso(
                "mejorar",
                new Trilingue(
                "Leer, cambiar una cosa y volver",
                "Lesen, eine Sache ändern, wieder kämpfen",
                "Read, change one thing and go again"),
                new Trilingue(
                "Entre ronda y ronda, el informe te dice qué ha ido bien y qué no, con una pista para cada "
                        + "cifra que va mal.\nCambia UNA sola cosa, compila y vuelve a pelear. Ahora te toca a ti "
                        + "escribirlo en Java, con tus palabras: en Tutoriales, «Tu primera pelea completa».",
                "Zwischen den Runden sagt dir der Bericht, was gut lief und was nicht, mit einem Tipp für jede "
                        + "Zahl, die schlecht ist.\nÄndere nur EINE Sache, kompiliere und kämpfe noch einmal. Jetzt bist "
                        + "du dran, es in Java mit deinen Worten zu schreiben: unter Tutorials, „Dein erster ganzer "
                        + "Kampf“.",
                "Between rounds, the report tells you what went well and what did not, with a hint for each "
                        + "number that is going badly.\nChange just ONE thing, compile and fight again. Now it is your "
                        + "turn to write it in Java, in your own words: in Tutorials, \"Your first complete fight\"."),
                19, new int[]{18,20,21}, false, 0, 0, Accion.CIERRE);
    }
}
