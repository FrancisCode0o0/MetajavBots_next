package metajavbots.textos;

/** Un texto en los tres idiomas. */
public final class Trilingue {

    public final String es, de, en;

    public Trilingue(String es, String de, String en) {
        this.es = es == null ? "" : es;
        this.de = de == null ? "" : de;
        this.en = en == null ? "" : en;
    }

    public static Trilingue igual(String s) {
        return new Trilingue(s, s, s);
    }

    public String en(Idioma i) {
        return i.elegir(es, de, en);
    }

    @Override
    public String toString() {
        return es;
    }
}
