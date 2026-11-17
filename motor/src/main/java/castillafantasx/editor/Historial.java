package castillafantasx.editor;

import java.util.ArrayList;
import java.util.List;

/**
 * HISTORIAL — Deshacer y rehacer, para todo el editor. Cada herramienta, al
 * cambiar algo, apunta aquí cómo se deshace y cómo se rehace
 * (editor.historial.apuntar(...)); Ctrl+Z y Ctrl+Y hacen lo demás.
 */
public final class Historial {

    /** Un cambio que se puede deshacer: su nombre (se lee en la lista) y cómo se deshace y se rehace. */
    public static final class Cambio {
        public final String nombre;
        final Runnable deshacer, rehacer;

        Cambio(String nombre, Runnable deshacer, Runnable rehacer) {
            this.nombre = nombre;
            this.deshacer = deshacer;
            this.rehacer = rehacer;
        }
    }

    // TODO-FRAN: cuántos cambios recuerda el editor.
    public static final int MAXIMO = 200;

    private final List<Cambio> hechos = new ArrayList<>(), deshechos = new ArrayList<>();

    /** Mientras se hace algo a varios a la vez (encargo ORDEN_ERGONOMIA_2): sus cambios, que se apuntarán como uno solo; o null. */
    private List<Cambio> enGrupo;
    private String nombreDelGrupo;

    /** Lo que se apunte desde aquí hasta cerrarGrupo() se deshace y se rehace de una vez, con ese nombre. */
    public void abrirGrupo(String nombre) {
        enGrupo = new ArrayList<>();
        nombreDelGrupo = nombre;
    }

    /** Cierra el grupo abierto: lo apuntado en él queda como un solo cambio (si no se apuntó nada, nada). */
    public void cerrarGrupo() {
        List<Cambio> grupo = enGrupo;
        enGrupo = null;
        if (grupo == null || grupo.isEmpty()) return;
        if (grupo.size() == 1) {
            apuntar(nombreDelGrupo, grupo.get(0).deshacer, grupo.get(0).rehacer);
            return;
        }
        // Se deshace del último al primero y se rehace del primero al último.
        apuntar(nombreDelGrupo, () -> {
            for (int i = grupo.size() - 1; i >= 0; i--) grupo.get(i).deshacer.run();
        }, () -> {
            for (Cambio c : grupo) c.rehacer.run();
        });
    }

    /** Apunta un cambio que ya se ha hecho. Lo que estaba deshecho ya no se puede rehacer. */
    public void apuntar(String nombre, Runnable deshacer, Runnable rehacer) {
        if (enGrupo != null) {
            enGrupo.add(new Cambio(nombre, deshacer, rehacer));
            return;
        }
        hechos.add(new Cambio(nombre, deshacer, rehacer));
        if (hechos.size() > MAXIMO) hechos.remove(0);
        deshechos.clear();
    }

    /** Deshace el último cambio. Devuelve su nombre, o null si no hay nada que deshacer. */
    public String deshacer() {
        if (hechos.isEmpty()) return null;
        Cambio c = hechos.remove(hechos.size() - 1);
        c.deshacer.run();
        deshechos.add(c);
        return c.nombre;
    }

    /** Rehace el último cambio deshecho. Devuelve su nombre, o null si no hay nada que rehacer. */
    public String rehacer() {
        if (deshechos.isEmpty()) return null;
        Cambio c = deshechos.remove(deshechos.size() - 1);
        c.rehacer.run();
        hechos.add(c);
        return c.nombre;
    }

    /** Quita el último cambio apuntado sin deshacerlo: para juntarlo con el siguiente (letras escritas seguidas). */
    public void deshacerSinHacer() {
        if (!hechos.isEmpty()) hechos.remove(hechos.size() - 1);
    }

    public boolean hayQueDeshacer() {
        return !hechos.isEmpty();
    }

    public boolean hayQueRehacer() {
        return !deshechos.isEmpty();
    }

    /** Los nombres de los cambios hechos, el último primero (para la lista del Taller). */
    public List<String> nombres() {
        List<String> nombres = new ArrayList<>();
        for (int i = hechos.size() - 1; i >= 0; i--) nombres.add(hechos.get(i).nombre);
        return nombres;
    }

    public void olvidar() {
        hechos.clear();
        deshechos.clear();
    }
}
