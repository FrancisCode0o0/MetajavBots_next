package castillafantasx.editor;

import java.util.List;

import castillafantasx.Pantalla;
import castillafantasx.f1.PanelF1;
import castillafantasx.interfaz.ListaQuieta;

/**
 * MENÚ DEL CLIC DERECHO (encargo ORDEN_ERGONOMIA_2) — Un menú pequeño junto
 * al puntero con lo que se puede hacer con lo que hay debajo: girarlo,
 * cambiarle el tamaño, quitarlo, abrir su figura o su código… Sale con el
 * clic derecho, con una pulsación larga (el dedo o el lápiz), con Mayús+F10
 * o con la fila «Qué hacer con lo elegido» del panel. Sus filas son Opcion,
 * como las del panel: ↑ ↓ eligen, ← → cambian el valor, Intro usa, Esc cierra
 * y basta con escribir para buscar entre ellas (docs/ORDEN_ERGONOMIA.md).
 */
public final class MenuContextual {

    /** De dónde salen sus filas: se piden cada vez, para que digan su valor de ahora. */
    public interface Fuente {
        List<Opcion> opciones(Editor editor);
    }

    /** Medidas: filas y cabecera de lo que pide un dedo (Estilo.TOQUE_MINIMO); el ancho, el de un nombre largo. */
    public static final int ALTO_DE_FILA = Estilo.TOQUE_MINIMO, ALTO_DEL_TITULO = Estilo.TOQUE_MINIMO, ANCHO = 250, FILAS_MAXIMAS = 12;
    /** Lo que se separa del puntero, para no taparlo. */
    private static final int SEPARACION = 4;

    /** Lo que dice arriba: de qué es el menú («Barril», «3 cosas elegidas»). */
    public final String titulo;
    private final Fuente fuente;
    /** Su esquina de arriba a la izquierda, en la imagen del juego. */
    public float x, y;
    /** La fila elegida (en la lista entera). */
    public int elegida;
    /** Lo escrito para buscar entre sus filas ("" si nada). */
    public String buscado = "";
    /** Si no caben todas, la lista no se mueve al señalar (como todas: ListaQuieta). */
    public final ListaQuieta lista = new ListaQuieta();
    /** ¿La última fila elegida lo fue con el puntero? (Entonces la lista no se mueve para enseñarla.) */
    public boolean conElPuntero;

    public MenuContextual(String titulo, Fuente fuente) {
        this.titulo = titulo;
        this.fuente = fuente;
    }

    public List<Opcion> opciones(Editor editor) {
        return fuente.opciones(editor);
    }

    /** ¿Casa esa fila con lo buscado? (Sin tildes, cualquiera de las palabras al principio de una palabra o dentro.) */
    public boolean casa(Opcion o) {
        if (o.esSeccion) return buscado.isEmpty();
        if (buscado.isEmpty()) return true;
        String texto = PanelF1.normalizar(o.texto);
        for (String palabra : PanelF1.normalizar(buscado).split(" ")) {
            if (!palabra.isEmpty() && !texto.contains(palabra)) return false;
        }
        return true;
    }

    /** ¿Se puede elegir esa fila? (Ni los títulos ni lo que no casa con lo buscado.) */
    public boolean seElige(Opcion o) {
        return !o.esSeccion && casa(o);
    }

    /** Cuántas filas caben a la vez. */
    public int caben(int total) {
        int porAlto = (int) ((Pantalla.alto - ALTO_DEL_TITULO - 2 * Estilo.MARGEN - Estilo.RENGLON) / ALTO_DE_FILA);
        return Math.max(1, Math.min(total, Math.min(FILAS_MAXIMAS, porAlto)));
    }

    /** El alto entero, con su cabecera. */
    public float alto(int total) {
        return ALTO_DEL_TITULO + caben(total) * ALTO_DE_FILA + 2;
    }

    /** Lo pone junto a ese punto (abajo a la derecha del puntero; si no cabe, al otro lado), sin salirse de la pantalla. */
    public void colocar(float px, float py, int total) {
        float alto = alto(total);
        x = px + SEPARACION + ANCHO <= Pantalla.ancho - Estilo.MARGEN ? px + SEPARACION : px - SEPARACION - ANCHO;
        y = py + SEPARACION + alto <= Pantalla.alto - Estilo.RENGLON - Estilo.MARGEN ? py + SEPARACION : py - SEPARACION - alto;
        x = Math.max(Estilo.MARGEN, Math.min(Pantalla.ancho - Estilo.MARGEN - ANCHO, x));
        y = Math.max(Editor.ALTO_DE_LA_BARRA + 2, Math.min(Pantalla.alto - Estilo.RENGLON - Estilo.MARGEN - alto, y));
    }

    public boolean dentro(float px, float py, int total) {
        return px >= x && px < x + ANCHO && py >= y && py < y + alto(total);
    }

    /** La × de cerrar, arriba a la derecha (una zona de toque entera). */
    public boolean enLaEquis(float px, float py) {
        return px >= x + ANCHO - Estilo.TOQUE_MINIMO && px < x + ANCHO && py >= y && py < y + ALTO_DEL_TITULO;
    }

    /** La primera fila que se ve. */
    public int primera(int total) {
        return lista.primera(elegida, total, caben(total), conElPuntero);
    }

    /** La fila (en la lista entera) que hay en ese punto, o -1. */
    public int filaEn(float px, float py, int total) {
        if (px < x || px >= x + ANCHO - ListaQuieta.ANCHO_DE_LA_BARRA * (total > caben(total) ? 1 : 0)) return -1;
        int fila = (int) Math.floor((py - y - ALTO_DEL_TITULO) / ALTO_DE_FILA);
        if (fila < 0 || fila >= caben(total)) return -1;
        int cual = primera(total) + fila;
        return cual < total ? cual : -1;
    }

    /** Elige la siguiente (o la anterior) fila que se puede elegir, dando la vuelta. */
    public void mover(List<Opcion> filas, int paso) {
        int n = filas.size();
        for (int i = 1; i <= n; i++) {
            int cual = Math.floorMod(elegida + paso * i, n);
            if (seElige(filas.get(cual))) {
                elegida = cual;
                conElPuntero = false;
                return;
            }
        }
    }

    /** Si la elegida no se puede elegir (un título, o no casa con lo buscado), la primera que sí. */
    public void ajustar(List<Opcion> filas) {
        if (elegida >= 0 && elegida < filas.size() && seElige(filas.get(elegida))) return;
        for (int i = 0; i < filas.size(); i++) {
            if (seElige(filas.get(i))) {
                elegida = i;
                return;
            }
        }
        elegida = Math.max(0, Math.min(elegida, filas.size() - 1));
    }
}
