package metajavbots.piezas;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import metajavbots.progreso.Etiqueta;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * PIEZA — Una pieza que se le pone a un robot: una cabeza, un brazo (con su
 * arma), unas piernas (su forma de moverse), un núcleo (su cerebro: los pasos
 * de cálculo y la energía) o un núcleo de naturaleza (que refuerza lo fuerte
 * de cada modelo). Salen de res/datos/piezas.txt (CatalogoDePiezas); cada
 * línea puede dar una serie (Mk I…V), así que el sistema aguanta cientos.
 */
public final class Pieza {

    /** Dónde se monta una pieza. */
    public enum Ranura { CABEZA, BRAZO, PIERNAS, NUCLEO, NATURALEZA }

    /** Las armas de los brazos. */
    public enum Arma { LASER, BALA, MISIL, CUERPO, LLAMA, ESCUDO, REPARAR, NINGUNA }

    /** Lo especial de las cabezas. */
    public enum Especial { ESCUDO, TURBO, REPARAR, CAMUFLAJE, PULSO, SOBRECARGA, NINGUNO }

    /** Cómo se mueven. */
    public enum Locomocion { BIPEDO, ORUGAS, RUEDAS, FLOTANTE, VUELO, PATAS }

    /** Lo difícil que es de conseguir. */
    public enum Rareza { COMUN, RARA, EPICA, LEGENDARIA }

    public final String id;
    public final Ranura ranura;
    public final Trilingue nombre;
    public final Naturaleza naturaleza;
    public final Rareza rareza;
    /** Cómo se gana: "inicio", "victorias:N", "nivel:<etiqueta>", "desafio", "jefe:<id>", "reto:<id>", "pruebas:<id>". */
    public final String desbloqueo;
    /** Mk I = 1 … Mk V = 5 (1 si no es de una serie). */
    public final int marca;
    /** Precio en monedas en la tienda (0 si no se vende en la tienda). */
    public final double precio;
    /** Rango mínimo del jugador para comprar esta pieza en la tienda (null si no se vende). */
    public final Etiqueta.Categoria rangoTienda;
    private final Map<String, Double> valores;
    private final Map<String, String> textos;

    Pieza(String id, Ranura ranura, Trilingue nombre, Naturaleza naturaleza, Rareza rareza, String desbloqueo, int marca,
          double precio, Etiqueta.Categoria rangoTienda, Map<String, Double> valores, Map<String, String> textos) {
        this.id = id;
        this.ranura = ranura;
        this.nombre = nombre;
        this.naturaleza = naturaleza;
        this.rareza = rareza;
        this.desbloqueo = desbloqueo;
        this.marca = marca;
        this.precio = precio;
        this.rangoTienda = rangoTienda;
        this.valores = Collections.unmodifiableMap(new LinkedHashMap<>(valores));
        this.textos = Collections.unmodifiableMap(new LinkedHashMap<>(textos));
    }

    /** Un número de la pieza (dano, alcance, enfriamiento, energia, peso, blindaje, velocidad, giro, radar, cono, pasos…); 0 si no lo tiene. */
    public double valor(String clave) {
        Double v = valores.get(clave);
        return v == null ? 0 : v;
    }

    public double valor(String clave, double siNo) {
        Double v = valores.get(clave);
        return v == null ? siNo : v;
    }

    public Map<String, Double> valores() {
        return valores;
    }

    public String texto(String clave) {
        String s = textos.get(clave);
        return s == null ? "" : s;
    }

    public Arma arma() {
        return enumDe(Arma.class, texto("arma"), Arma.NINGUNA);
    }

    public Especial especial() {
        return enumDe(Especial.class, texto("especial"), Especial.NINGUNO);
    }

    public Locomocion locomocion() {
        return enumDe(Locomocion.class, texto("locomocion"), Locomocion.BIPEDO);
    }

    public String nombre(Idioma i) {
        return nombre.en(i);
    }

    /** ¿Es de las que se tienen desde el principio? */
    public boolean deInicio() {
        return desbloqueo.equals("inicio");
    }

    /** ¿Se puede comprar en la tienda con monedas? */
    public boolean comprable() {
        return precio > 0 && rangoTienda != null;
    }

    static <E extends Enum<E>> E enumDe(Class<E> clase, String nombre, E siNo) {
        if (nombre == null || nombre.isEmpty()) return siNo;
        try {
            return Enum.valueOf(clase, nombre.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return siNo;
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
