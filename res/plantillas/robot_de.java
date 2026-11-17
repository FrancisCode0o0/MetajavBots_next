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
        zielen(12);
    }

    @Override
    public void beiSicht(Gegner g) {
        zielenNach(g.winkel());
        angreifen();
    }

    @Override
    public void beiTreffer(Treffer t) {
        // Zur Seite ausweichen.
        drehenNach(t.winkel() + 90);
        vorwaerts(80);
    }
}
