package metajavbots.api;

/**
 * EIGENES TEIL — Java-Vertrag auf Deutsch für ein selbst entworfenes Roboterteil
 * aus der Werkstatt. Implementiere dieses Interface, wähle den Platz ({@code KOPF},
 * {@code ARM}, {@code BEINE} oder {@code KERN}) und bleibe im Balance-Budget
 * (maximal 100 Punkte).
 */
public interface EigenesTeil extends PiezaPropia {

    /** Sichtbarer Name deines Teils (1 bis 28 Zeichen). */
    String name();

    /** Einbauplatz: {@code "KOPF"}, {@code "ARM"}, {@code "BEINE"} oder {@code "KERN"}. */
    String platz();

    /** Natur des Teils: {@code "ACERO"}, {@code "RAYO"}, {@code "FOCO"}, {@code "FURIA"}, {@code "INGENIO"} oder {@code "ETER"}. */
    default String natur() {
        return "ACERO";
    }

    /** Stil je nach Platz (Waffe, Spezialfähigkeit oder Fortbewegung). */
    default String stil() {
        return "LASER";
    }

    /** Zusätzliche Panzerung des Teils. */
    default double panzerung() {
        return 10;
    }

    /** Gewicht des Teils. */
    default double gewicht() {
        return 6;
    }

    /** Schaden pro Angriff (nur für {@code ARM}). */
    default double schaden() {
        return 9;
    }

    /** Reichweite der Waffe (nur für {@code ARM}). */
    default double reichweite() {
        return 260;
    }

    /** Abklingzeit in Zügen (nur für {@code ARM}). */
    default double abklingzeit() {
        return 18;
    }

    /** Energieverbrauch pro Einsatz. */
    default double energie() {
        return 10;
    }

    /** Öffnungswinkel des Radarkegels in Grad (nur für {@code KOPF}). */
    default double kegel() {
        return 50;
    }

    /** Grundtempo (nur für {@code BEINE}). */
    default double tempo() {
        return 4.0;
    }

    /** Drehgeschwindigkeit in Grad pro Zug (nur für {@code BEINE}). */
    default double drehung() {
        return 6.0;
    }

    /** Beschleunigung pro Zug (nur für {@code BEINE}). */
    default double beschleunigung() {
        return 0.5;
    }

    /** Maximale Traglast (nur für {@code BEINE}). */
    default double traglast() {
        return 32;
    }

    /** Rechenschritte pro Zug (nur für {@code KERN}). */
    default double schritte() {
        return 3200;
    }

    /** Maximale Energie (nur für {@code KERN}). */
    default double maxEnergie() {
        return 105;
    }

    /** Energieaufladung pro Zug (nur für {@code KERN}). */
    default double aufladung() {
        return 0.42;
    }

    @Override
    default String nombre() {
        return name();
    }

    @Override
    default String ranura() {
        return platz();
    }

    @Override
    default String naturaleza() {
        return natur();
    }

    @Override
    default String estilo() {
        return stil();
    }

    @Override
    default double blindaje() {
        return panzerung();
    }

    @Override
    default double peso() {
        return gewicht();
    }

    @Override
    default double dano() {
        return schaden();
    }

    @Override
    default double alcance() {
        return reichweite();
    }

    @Override
    default double enfriamiento() {
        return abklingzeit();
    }

    @Override
    default double energia() {
        return energie();
    }

    @Override
    default double cono() {
        return kegel();
    }

    @Override
    default double velocidad() {
        return tempo();
    }

    @Override
    default double giro() {
        return drehung();
    }

    @Override
    default double aceleracion() {
        return beschleunigung();
    }

    @Override
    default double carga() {
        return traglast();
    }

    @Override
    default double pasos() {
        return schritte();
    }

    @Override
    default double energiaMax() {
        return maxEnergie();
    }

    @Override
    default double recarga() {
        return aufladung();
    }
}
