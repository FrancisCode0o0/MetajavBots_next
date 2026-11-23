import metajavbots.api.*;

/**
 * Mein erster Metajavbot. Ändere, was du willst, und drück Kompilieren (Strg+S).
 * Die Methoden «bei…» werden von selbst aufgerufen, wenn etwas passiert; jedenZug etwa 30-mal pro Sekunde.
 */
public class MeinRoboter extends MetajavbotDe {

    @Override
    public void beimStart() {
        sagen("Auf in den Kampf!");
    }

    @Override
    public void jedenZug() {
        // Wenn er angekommen ist, fährt er ein Stück weiter und dreht sich.
        if (restStrecke() == 0) {
            vorwaerts(120);
            drehen(35);
        }
        // Das Visier (Arme und Radar) bewegen, um den Gegner zu suchen.
        // Achtung: jedenZug kommt nach beiSicht, und das Visier merkt sich nur den letzten Befehl: Dieser löscht den dortigen.
        zielen(12);
    }

    @Override
    public void beiSicht(Gegner g) {
        // Das Visier dreht langsam (etwa 14° pro Zug): Es kommt nicht im selben Zug beim Gegner an.
        zielenNach(g.winkel());
        // angreifen() schießt sofort, dorthin, wohin das Visier JETZT zeigt, nicht dorthin, wohin es soll.
        angreifen();
    }

    @Override
    public void beiTreffer(Treffer t) {
        // Zur Seite ausweichen.
        drehenNach(t.winkel() + 90);
        vorwaerts(80);
    }
}
