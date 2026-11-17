package metajavbots.textos;

/** Los tres idiomas del juego. Todo texto del juego viene en los tres. */
public enum Idioma {
    ES("Español"), DE("Deutsch"), EN("English");

    public final String nombre;

    Idioma(String nombre) {
        this.nombre = nombre;
    }

    public String codigo() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Idioma de(String codigo) {
        for (Idioma i : values()) if (i.codigo().equalsIgnoreCase(codigo == null ? "" : codigo.trim())) return i;
        return ES;
    }

    /** Uno de tres textos, según el idioma. */
    public String elegir(String es, String de, String en) {
        switch (this) {
            case DE:
                return de != null && !de.isEmpty() ? de : es;
            case EN:
                return en != null && !en.isEmpty() ? en : es;
            default:
                return es;
        }
    }
}
