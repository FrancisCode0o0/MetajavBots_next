package metajavbots.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * METAJAVBOT (DEUTSCH) — Dein Roboter auf Deutsch. Schreibe eine Klasse, die
 * von dieser erbt (Thema 11, Vererbung), und überschreibe (@Override) die
 * Methoden «bei…» und «jedenZug»:
 *
 * <pre>
 * public class MeinRoboter extends MetajavbotDe {
 *     &#64;Override
 *     public void jedenZug() {
 *         vorwaerts(50);
 *         drehen(10);
 *     }
 *
 *     &#64;Override
 *     public void beiSicht(Gegner g) {
 *         zielenNach(g.winkel());
 *         angreifen();
 *     }
 * }
 * </pre>
 *
 * In jedem Zug ruft der Kampf zuerst die Meldungen auf (beiTreffer, beiWand…),
 * dann beiSicht für jeden sichtbaren Gegner und zuletzt jedenZug. Befehle
 * (vorwaerts, drehen, zielen) warten nicht: Sie sagen, was du willst, und der
 * Roboter erledigt es Zug um Zug in seinem Tempo.
 *
 * Winkel in Grad: 0 ist rechts (Osten), 90 oben; positive Drehungen gehen nach
 * links. Die Arena reicht von (0, 0) unten links bis (arenaBreite(), arenaHoehe()).
 */
public abstract class MetajavbotDe extends RobotBase {

    // ================================================================ was du überschreibst

    /** Zu Beginn jeder Runde (vor dem ersten Zug). Thema 06: Methoden. */
    public void beimStart() {
    }

    /** In jedem Zug (etwa 30 pro Sekunde). Hier steht das Wichtigste. */
    public void jedenZug() {
    }

    /** In jedem Zug, in dem dein Radar einen Gegner sieht. */
    public void beiSicht(Gegner g) {
    }

    /** Wenn du getroffen wirst. */
    public void beiTreffer(Treffer t) {
    }

    /** Wenn dein Angriff trifft. */
    public void beiEigenemTreffer(Treffer t) {
    }

    /** Wenn du gegen eine Wand fährst (kostet etwas Panzerung). */
    public void beiWand() {
    }

    /** Wenn du mit einem Gegner zusammenstößt. */
    public void beiZusammenstoss(Gegner g) {
    }

    /** Am Ende der Runde. */
    public void beiRundenende() {
    }

    /** Wenn dir ein Teamkamerad etwas meldet (in Teamkämpfen). */
    public void beiTeamNachricht(String von, String nachricht) {
    }

    @Override
    final void empezar() {
        beimStart();
    }

    @Override
    final void turno() {
        jedenZug();
    }

    @Override
    final void ver(Avistado e) {
        beiSicht(e);
    }

    @Override
    final void golpeado(Impacto g) {
        beiTreffer(g);
    }

    @Override
    final void acierto(Impacto g) {
        beiEigenemTreffer(g);
    }

    @Override
    final void pared() {
        beiWand();
    }

    @Override
    final void choque(Avistado e) {
        beiZusammenstoss(e);
    }

    @Override
    final void finDeRonda() {
        beiRundenende();
    }

    @Override
    final void equipo(String de, String mensaje) {
        beiTeamNachricht(de, mensaje);
    }

    // ================================================================ fahren

    /** Fährt diese Strecke vorwärts (negativ: rückwärts). Ersetzt den vorigen Fahrbefehl. */
    public final void vorwaerts(double strecke) {
        m().avanzar(strecke);
    }

    /** Fährt diese Strecke rückwärts. */
    public final void rueckwaerts(double strecke) {
        m().avanzar(-strecke);
    }

    /** Dreht den Körper um diese Grad (positiv: nach links). */
    public final void drehen(double grad) {
        m().girar(grad);
    }

    public final void linksDrehen(double grad) {
        m().girar(grad);
    }

    public final void rechtsDrehen(double grad) {
        m().girar(-grad);
    }

    /** Dreht den Körper auf diesen absoluten Winkel, auf dem kürzesten Weg. */
    public final void drehenNach(double winkel) {
        m().girar(normalisieren(winkel - m().rumbo()));
    }

    /** Hält an: weder fahren noch drehen. */
    public final void anhalten() {
        m().detener();
    }

    // ================================================================ zielen und angreifen

    /** Dreht das Visier (Arme und Radar) um diese Grad, ohne den Körper zu drehen. */
    public final void zielen(double grad) {
        m().apuntar(grad);
    }

    /** Dreht das Visier auf diesen absoluten Winkel. */
    public final void zielenNach(double winkel) {
        m().apuntar(normalisieren(winkel - m().mira()));
    }

    /** Dreht das Visier auf diesen Punkt der Arena. */
    public final void zielenAuf(double x, double y) {
        zielenNach(anguloHasta(x, y));
    }

    /** Greift mit dem linken Arm in Blickrichtung an. Gibt false zurück, wenn er noch nicht bereit war. */
    public final boolean angriffLinks() {
        return m().atacar(Mando.IZQUIERDA);
    }

    public final boolean angriffRechts() {
        return m().atacar(Mando.DERECHA);
    }

    /** Benutzt das Spezielle des Kopfes (Schild, Turbo, Reparatur…, je nach Teil). */
    public final boolean spezial() {
        return m().atacar(Mando.CABEZA);
    }

    /** Greift mit dem Arm an, der bereit ist (zuerst der rechte). */
    public final boolean angreifen() {
        return m().atacar(Mando.DERECHA) || m().atacar(Mando.IZQUIERDA);
    }

    public final boolean bereitLinks() {
        return m().listo(Mando.IZQUIERDA);
    }

    public final boolean bereitRechts() {
        return m().listo(Mando.DERECHA);
    }

    public final boolean bereitSpezial() {
        return m().listo(Mando.CABEZA);
    }

    /** Die Waffe des linken Arms: "LASER", "BALA" (Kugel), "MISIL" (Rakete), "CUERPO" (Nahkampf), "ESCUDO" (Schild), "REPARAR"… ("" wenn keine). */
    public final String waffeLinks() {
        return m().arma(Mando.IZQUIERDA);
    }

    public final String waffeRechts() {
        return m().arma(Mando.DERECHA);
    }

    /** Das Spezielle des Kopfes: "ESCUDO" (Schild), "TURBO", "REPARAR" (Reparatur), "CAMUFLAJE" (Tarnung), "PULSO", "SOBRECARGA" ("" wenn keins). */
    public final String spezialKopf() {
        return m().arma(Mando.CABEZA);
    }

    /** Wie weit die Waffe des linken Arms reicht. */
    public final double reichweiteLinks() {
        return m().alcance(Mando.IZQUIERDA);
    }

    public final double reichweiteRechts() {
        return m().alcance(Mando.DERECHA);
    }

    // ================================================================ wissen

    public final double x() {
        return m().x();
    }

    public final double y() {
        return m().y();
    }

    /** Wohin der Körper schaut (Grad). */
    public final double richtung() {
        return m().rumbo();
    }

    /** Wohin das Visier schaut (Grad). */
    public final double blick() {
        return m().mira();
    }

    public final double geschwindigkeit() {
        return m().velocidad();
    }

    /** Die Energie (Laser und Spezielles verbrauchen sie; sie lädt sich von selbst). */
    public final double energie() {
        return m().energia();
    }

    /** Deine restliche Panzerung, von 0 bis 100. */
    public final double panzerung() {
        return m().blindaje();
    }

    /** Die Nummer des Zuges in dieser Runde (ab 0). */
    public final int zug() {
        return m().turno();
    }

    /** Die Runde (1 bis 4). */
    public final int runde() {
        return m().ronda();
    }

    public final double arenaBreite() {
        return m().anchoArena();
    }

    public final double arenaHoehe() {
        return m().altoArena();
    }

    /** Was vom letzten Fahrbefehl noch fehlt. */
    public final double restStrecke() {
        return m().restanteAvance();
    }

    /** Was vom letzten Drehbefehl noch fehlt. */
    public final double restDrehung() {
        return m().restanteGiro();
    }

    public final double restZielen() {
        return m().restanteMira();
    }

    /** Die Gegner, die du gerade siehst (Thema 15: Collections). */
    public final List<Gegner> gesehen() {
        return new ArrayList<>(m().vistos());
    }

    /** Die Fallen und Gegenstände der Arena: Stacheln, Sägen, Grube, Fässer … (leer, wenn es keine gibt; Thema 15). */
    public final List<Hindernis> hindernisse() {
        return new ArrayList<>(m().obstaculos());
    }

    /** Deine Teamkameraden (in Teamkämpfen; leer, wenn du allein kämpfst). */
    public final List<Gegner> kameraden() {
        return new ArrayList<>(m().companeros());
    }

    /** Meldet deinen Kameraden etwas (es kommt im nächsten Zug in beiTeamNachricht an). */
    public final void teamMelden(Object nachricht) {
        m().avisarAlEquipo(aTexto(nachricht));
    }

    // ================================================================ Hilfen

    /** Der absolute Winkel von dir zu diesem Punkt. */
    public final double winkelZu(double x, double y) {
        return anguloHasta(x, y);
    }

    /** Der Abstand von dir zu diesem Punkt. */
    public final double abstandZu(double x, double y) {
        return distanciaHasta(x, y);
    }

    /** Bringt einen Winkel nach (-180, 180]: normalisieren(350) ergibt -10. */
    public final double normalisieren(double grad) {
        return normalizado(grad);
    }

    /** Zufallszahlen (die deines Roboters: derselbe Kampf ergibt dasselbe). */
    public final Random zufall() {
        return m().azar();
    }

    /** Sagt etwas in einer Sprechblase über dem Roboter. */
    public final void sagen(Object text) {
        m().decir(aTexto(text));
    }

    /** Schreibt in die Konsole deines Roboters (unter dem Code). */
    public final void ausgeben(Object text) {
        m().escribir(aTexto(text));
    }

    /** Lo que quieras recordar de una ronda a otra (tema 15: Map). Sobrevive aunque cambies el código. */
    public final Map<String, Object> gedaechtnis() {
        return m().memoria();
    }
}
