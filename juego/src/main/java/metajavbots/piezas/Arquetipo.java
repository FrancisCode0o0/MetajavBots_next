package metajavbots.piezas;

import metajavbots.textos.Idioma;

/**
 * ARQUETIPO — La forma de programarse de cada modelo de robot. Además de su
 * estética y sus estadísticas, cada arquetipo habilita un método propio en la
 * API trilingüe ({@code embestir}, {@code pulsoRadar}, {@code fortificar}).
 */
public enum Arquetipo {
    ASALTO("Asalto", "Angriff", "Assault",
            "embestir()", "ansturm()", "charge()",
            "impulso frontal de velocidad", "Vorwärts-Temposchub", "forward speed burst"),
    TACTICO("Táctico", "Taktik", "Tactical",
            "pulsoRadar()", "radarImpuls()", "radarPulse()",
            "barrido de radar en 360°", "360°-Radarimpuls", "360° radar sweep"),
    DEFENSOR("Defensor", "Verteidigung", "Defender",
            "fortificar()", "befestigen()", "fortify()",
            "postura blindada que reduce el daño", "Abwehrhaltung gegen Schaden", "fortified stance that reduces damage");

    private final String es, de, en;
    private final String habEs, habDe, habEn;
    private final String descEs, descDe, descEn;

    Arquetipo(String es, String de, String en,
            String habEs, String habDe, String habEn,
            String descEs, String descDe, String descEn) {
        this.es = es;
        this.de = de;
        this.en = en;
        this.habEs = habEs;
        this.habDe = habDe;
        this.habEn = habEn;
        this.descEs = descEs;
        this.descDe = descDe;
        this.descEn = descEn;
    }

    /** Nombre visible del arquetipo en el idioma pedido. */
    public String nombre(Idioma i) {
        return i.elegir(es, de, en);
    }

    /** Firma del método propio del arquetipo en el idioma pedido. */
    public String habilidad(Idioma i) {
        return i.elegir(habEs, habDe, habEn);
    }

    /** Breve descripción de la habilidad del arquetipo en el idioma pedido. */
    public String resumen(Idioma i) {
        return i.elegir(descEs, descDe, descEn);
    }

    /** Convierte un texto del archivo de modelos en su arquetipo (ASALTO por defecto). */
    public static Arquetipo de(String s) {
        return Pieza.enumDe(Arquetipo.class, s, ASALTO);
    }
}
