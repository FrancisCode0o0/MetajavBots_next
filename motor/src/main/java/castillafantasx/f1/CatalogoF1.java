package castillafantasx.f1;

import java.util.ArrayList;
import java.util.List;

import castillafantasx.Contenido;

/**
 * CATALOGO DE F1 — Todas las herramientas de F1, tema a tema y en su orden
 * fijo (docs/F1.md). Es la lista que recorren ↑↓, la que pinta el mosaico y
 * la que puede leer la rueda del EDITOR. Las filas son las de siempre de
 * Partida.opcionesDeDepuracion(): aquí solo se agrupan.
 */
public final class CatalogoF1 {

    private CatalogoF1() { }

    // Las herramientas fijas, en el orden en que se ven: las da el juego (castillafantasx.Contenido.herramientasDeF1).
    private static final Herramienta[] FIJAS = Contenido.delJuego().herramientasDeF1();

    // El principio de las filas que se hacen solas, al final de F1.
    private static final String BAJAR = "Bajar a: ", IR = "Ir a: ";

    /**
     * Las herramientas de F1, en su orden, para las filas de ahora
     * (Partida.opcionesDeDepuracion()). Una fila que no esté aquí ni sea de
     * «Bajar a» o «Ir a» va al tema «Otras»: así nunca se pierde ninguna.
     */
    public static List<Herramienta> herramientas(List<String> filas) {
        List<Herramienta> lista = new ArrayList<>();
        boolean[] puesta = new boolean[filas.size()];
        for (Herramienta h : FIJAS) {
            if (h.fila >= filas.size()) continue;
            lista.add(h);
            puesta[h.fila] = true;
        }
        List<Herramienta> otras = new ArrayList<>();
        for (int i = 0; i < filas.size(); i++) {
            if (puesta[i]) continue;
            String texto = filas.get(i);
            if (texto.startsWith(BAJAR)) {
                String sitio = texto.substring(BAJAR.length());
                int punto = sitio.indexOf(" · ");
                String id = punto >= 0 ? sitio.substring(0, punto) : sitio;
                lista.add(new Herramienta("bajar_" + id, "Bajar a " + id.replace('_', ' '), Tema.SALTOS, "bajar", i, "mazmorra cueva zona"));
            } else if (texto.startsWith(IR)) {
                String lugar = texto.substring(IR.length());
                lista.add(new Herramienta("ir_" + PanelF1.normalizar(lugar).replace(' ', '_'), lugar, Tema.SALTOS, "ir", i, "ir lugar viajar"));
            } else {
                otras.add(new Herramienta("fila_" + i, texto, Tema.OTRAS, "otras", i, ""));
            }
        }
        lista.addAll(otras);
        return lista;
    }

    /** Las herramientas de un tema, en su orden. */
    public static List<Herramienta> deTema(List<Herramienta> todas, Tema tema) {
        List<Herramienta> deEse = new ArrayList<>();
        for (Herramienta h : todas) {
            if (h.tema == tema) deEse.add(h);
        }
        return deEse;
    }

    /** La herramienta que acciona esa fila, o null. */
    public static Herramienta deFila(List<Herramienta> todas, int fila) {
        for (Herramienta h : todas) {
            if (h.fila == fila) return h;
        }
        return null;
    }

    /** La herramienta con ese id, o null. */
    public static Herramienta deId(List<Herramienta> todas, String id) {
        for (Herramienta h : todas) {
            if (h.id.equals(id)) return h;
        }
        return null;
    }

    /** Todos los iconos que usa F1 (los de los temas, las herramientas y el buscador): el Taller de iconos los fabrica. */
    public static List<String> iconos() {
        List<String> nombres = new ArrayList<>();
        for (Tema t : Tema.values()) nombres.add(t.icono);
        for (Herramienta h : FIJAS) nombres.add(h.icono);
        nombres.add("bajar");
        nombres.add("ir");
        nombres.add("otras");
        nombres.add("lupa");
        nombres.add("favorita");
        nombres.add("recientes");
        return nombres;
    }
}
