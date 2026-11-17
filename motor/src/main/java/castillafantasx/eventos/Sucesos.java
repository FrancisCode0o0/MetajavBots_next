package castillafantasx.eventos;

import java.util.ArrayList;
import java.util.List;

/**
 * SUCESOS — Lo que el mundo le cuenta al modo evento: «ha hablado con el
 * pastor», «ha matado una fiera», «ha cogido una moneda»… Los lleva el héroe
 * (el mismo en la superficie y en la mazmorra): el mundo los apunta aquí y
 * el Motor los recoge en cada frame y mira qué eventos saltan.
 */
public final class Sucesos {

    /** Los tipos de suceso (los mismos nombres que el «cuando» de un evento). */
    public static final String HABLAR = "hablar", USAR = "usar", MATAR = "matar", COGER = "coger", COMPRAR = "comprar",
            GANCHO = "gancho", TIEMPO = "tiempo";

    /** Algo que ha pasado: de qué tipo y con qué (todos sus nombres: su id, su nudo, su aspecto…). */
    public static final class Suceso {
        public final String tipo;
        public final String[] nombres;

        Suceso(String tipo, String[] nombres) {
            this.tipo = tipo;
            this.nombres = nombres;
        }
    }

    /** Quien quiere enterarse antes de que pase (el Motor): puede hacerlo él en su lugar. */
    public interface Oyente {
        /** Va a pasar eso. Devuelve true si un evento se ha hecho cargo y lo de siempre ya no hace falta. */
        boolean antesDe(String tipo, String[] nombres);
    }

    public Oyente oyente;
    private final List<Suceso> pendientes = new ArrayList<>();
    // Para que un mundo sin modo evento (las pruebas antiguas) no acumule sucesos sin fin.
    private static final int TOPE = 64;

    /** Apunta que ha pasado algo. Los nombres vacíos o null no cuentan. */
    public void apuntar(String tipo, String... nombres) {
        if (pendientes.size() >= TOPE) pendientes.remove(0);
        pendientes.add(new Suceso(tipo, limpios(nombres)));
    }

    /** Va a pasar algo que un evento puede hacer en su lugar (hablar con alguien, usar algo). ¿Se ha hecho cargo un evento? */
    public boolean antesDe(String tipo, String... nombres) {
        return oyente != null && oyente.antesDe(tipo, limpios(nombres));
    }

    /** Lo apuntado desde la última vez; se vacía. */
    public List<Suceso> recoger() {
        List<Suceso> todos = new ArrayList<>(pendientes);
        pendientes.clear();
        return todos;
    }

    private static String[] limpios(String[] nombres) {
        List<String> buenos = new ArrayList<>();
        for (String n : nombres) {
            if (n != null && !n.trim().isEmpty()) buenos.add(n.trim());
        }
        return buenos.toArray(new String[0]);
    }
}
