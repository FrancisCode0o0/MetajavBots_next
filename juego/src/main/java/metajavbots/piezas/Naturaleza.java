package metajavbots.piezas;

import metajavbots.textos.Idioma;

/**
 * NATURALEZA — El carácter de un modelo y de sus piezas. Cada modelo tiene
 * una; las piezas de su misma naturaleza le sientan mejor, y los núcleos de
 * naturaleza (que se ganan venciendo) refuerzan lo suyo.
 */
public enum Naturaleza {
    ACERO("Acero", "Stahl", "Steel", "blindaje"),
    RAYO("Rayo", "Blitz", "Bolt", "velocidad"),
    FOCO("Foco", "Fokus", "Focus", "precision"),
    FURIA("Furia", "Wut", "Fury", "cuerpo"),
    INGENIO("Ingenio", "Geist", "Wit", "pasos"),
    ETER("Éter", "Äther", "Aether", "esquiva"),
    NINGUNA("—", "—", "—", "");

    private final String es, de, en;
    /** Lo que refuerza: blindaje, velocidad, precision, cuerpo, pasos o esquiva. */
    public final String refuerza;

    Naturaleza(String es, String de, String en, String refuerza) {
        this.es = es;
        this.de = de;
        this.en = en;
        this.refuerza = refuerza;
    }

    public String nombre(Idioma i) {
        return i.elegir(es, de, en);
    }

    /** El color que la representa (RGB). */
    public int color() {
        switch (this) {
            case ACERO: return 0x8FA3B8;
            case RAYO: return 0xFFD23F;
            case FOCO: return 0x3FE0D0;
            case FURIA: return 0xFF4F3F;
            case INGENIO: return 0xB07CFF;
            case ETER: return 0xE8F4FF;
            default: return 0x777777;
        }
    }

    public static Naturaleza de(String s) {
        return Pieza.enumDe(Naturaleza.class, s, NINGUNA);
    }
}
