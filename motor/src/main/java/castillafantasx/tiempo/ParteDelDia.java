package castillafantasx.tiempo;

/** PARTE DEL DÍA — Los cuatro momentos del día, en el orden en que pasan. */
public enum ParteDelDia {

    ALBA("alba"), DIA("día"), CREPUSCULO("crepúsculo"), NOCHE("noche");

    public final String nombre;     // como se lee en pantalla

    ParteDelDia(String nombre) {
        this.nombre = nombre;
    }

    /** La clave con que se escribe en res/datos/dia_noche.txt: "alba", "dia", "crepusculo" o "noche". */
    public String clave() {
        return name().toLowerCase();
    }

    /** "noche" → NOCHE; null si no existe. */
    public static ParteDelDia porClave(String clave) {
        for (ParteDelDia p : values()) {
            if (p.clave().equals(clave)) return p;
        }
        return null;
    }
}
