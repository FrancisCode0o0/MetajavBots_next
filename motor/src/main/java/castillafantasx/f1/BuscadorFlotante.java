package castillafantasx.f1;

import java.util.List;

/**
 * BUSCADOR FLOTANTE — El buscador pequeño de cualquier menú (encargo
 * ORDEN_ERGONOMIA, docs/ORDEN_ERGONOMIA.md): se escribe sin pulsar nada antes
 * y queda elegida la fila que mejor casa, como en F1 y en el editor. Busca
 * igual que F1 (sin tildes, varias palabras, el principio de una palabra).
 * Se pinta pequeño en una esquina del menú, con su lupa, que también se toca.
 */
public final class BuscadorFlotante {

    /** Lo escrito ("" si no se busca). */
    public String texto = "";
    /** Se ha tocado la lupa: se espera lo que se escriba (en la tablet, sale el teclado). */
    public boolean esperando;
    /** Las filas que se vieron en el último dibujo del menú: el dibujo las apunta y las teclas buscan en ellas. */
    public List<String> filas = java.util.Collections.emptyList();
    /**
     * Los valores entre los que elige una fila larga (las figuras de la
     * galería, por ejemplo: «buscar seres»), por su número de fila; null si
     * ninguna fila los da. Lo pone el menú.
     */
    public java.util.function.IntFunction<List<String>> valoresDe;

    /** Lo que casa: una fila (valor -1) o un valor de una fila. */
    public static final class Encontrado {
        public final int fila, valor;
        public final String texto;

        Encontrado(int fila, int valor, String texto) {
            this.fila = fila;
            this.valor = valor;
            this.texto = texto;
        }
    }

    // TODO-FRAN: cuántos valores encontrados enseña la lista de debajo del buscador.
    public static final int MAXIMO_DE_VALORES = 6;
    /** Lo encontrado con lo escrito: primero las filas que casan, luego los valores (las mejores notas primero). */
    public final List<Encontrado> encontrados = new java.util.ArrayList<>();
    /** El encontrado elegido (↑↓ lo cambian). */
    public int elegido;

    /** Vuelve a buscar con lo escrito (tras escribir o borrar). */
    public void buscar() {
        encontrados.clear();
        elegido = 0;
        if (!buscando()) return;
        for (int nota = 0; nota <= 3; nota++) {
            for (int i = 0; i < filas.size(); i++) {
                if (nota(filas.get(i)) == nota) encontrados.add(new Encontrado(i, -1, filas.get(i)));
            }
        }
        if (valoresDe == null) return;
        int valores = 0;
        for (int nota = 0; nota <= 3 && valores < MAXIMO_DE_VALORES; nota++) {
            for (int i = 0; i < filas.size() && valores < MAXIMO_DE_VALORES; i++) {
                List<String> deEsa = valoresDe.apply(i);
                if (deEsa == null) continue;
                String nombreDeLaFila = filas.get(i).split(":")[0].trim();
                for (int v = 0; v < deEsa.size() && valores < MAXIMO_DE_VALORES; v++) {
                    if (nota(deEsa.get(v)) != nota) continue;
                    encontrados.add(new Encontrado(i, v, nombreDeLaFila + ": " + deEsa.get(v)));
                    valores++;
                }
            }
        }
    }

    /** Lo encontrado elegido, o null. */
    public Encontrado elegido() {
        return encontrados.isEmpty() ? null : encontrados.get(Math.max(0, Math.min(elegido, encontrados.size() - 1)));
    }

    /** ↑↓ mientras se busca: el encontrado anterior o el siguiente, dando la vuelta. */
    public void moverElegido(int paso) {
        if (encontrados.isEmpty()) return;
        elegido = ((elegido + paso) % encontrados.size() + encontrados.size()) % encontrados.size();
    }

    public boolean buscando() {
        return !texto.isEmpty();
    }

    /** Una letra más. */
    public void escribir(char letra) {
        if (texto.length() >= PanelF1.LARGO_MAXIMO_DE_BUSQUEDA) return;
        if (letra == ' ' && (texto.isEmpty() || texto.endsWith(" "))) return;      // sin espacios al principio ni dobles
        texto += letra;
        esperando = false;
        buscar();
    }

    /** Borra la última letra. */
    public void borrar() {
        if (buscando()) texto = texto.substring(0, texto.length() - 1);
        buscar();
    }

    /** Lo deja vacío (Esc, al cambiar de menú o tras usar lo encontrado). */
    public void vaciar() {
        texto = "";
        esperando = false;
        encontrados.clear();
        elegido = 0;
    }

    /** La nota de esa fila con lo escrito (0 la mejor), o -1 si no casa. */
    public int nota(String fila) {
        String q = PanelF1.normalizar(texto).trim();
        if (q.isEmpty()) return 0;
        String todo = PanelF1.normalizar(fila);
        return PanelF1.nota(todo, todo, q);
    }

    /** ¿Casa esa fila con lo escrito? (Sin escribir nada, todas casan.) */
    public boolean casa(String fila) {
        return nota(fila) >= 0;
    }

    /** La fila que mejor casa (a igualdad, la primera), o -1 si no casa ninguna. */
    public int mejor() {
        int mejor = -1, notaMejor = Integer.MAX_VALUE;
        for (int i = 0; i < filas.size(); i++) {
            int n = nota(filas.get(i));
            if (n >= 0 && n < notaMejor) {
                mejor = i;
                notaMejor = n;
            }
        }
        return mejor;
    }

    /** La siguiente fila que casa desde 'desde' (paso 1) o la anterior (-1), dando la vuelta; 'desde' si no hay otra. */
    public int siguiente(int desde, int paso) {
        int n = filas.size();
        for (int k = 1; k <= n; k++) {
            int i = ((desde + paso * k) % n + n) % n;
            if (casa(filas.get(i))) return i;
        }
        return desde;
    }

    /** Lo que falta por escribir de lo encontrado elegido, si empieza por lo escrito (el autocompletado, en gris), o "". */
    public String autocompletado() {
        Encontrado e = elegido();
        if (e == null) return "";
        return autocompletado(e.valor < 0 ? e.texto : valoresDe.apply(e.fila).get(e.valor));
    }

    /** Completa lo escrito con lo encontrado elegido (Tab), hasta el final de la palabra. Devuelve false si no había qué. */
    public boolean completar() {
        String resto = autocompletado();
        if (resto.isEmpty()) return false;
        int corte = resto.indexOf(' ', 1);
        String trozo = corte > 0 ? resto.substring(0, corte) : resto;
        texto = (texto + trozo).substring(0, Math.min(PanelF1.LARGO_MAXIMO_DE_BUSQUEDA, texto.length() + trozo.length()));
        Encontrado antes = elegido();
        buscar();
        // Sigue elegido lo mismo que antes de completar.
        for (int i = 0; antes != null && i < encontrados.size(); i++) {
            if (encontrados.get(i).fila == antes.fila && encontrados.get(i).valor == antes.valor) elegido = i;
        }
        return true;
    }

    /** Lo que falta por escribir de ese texto, si empieza por lo escrito, o "". */
    private String autocompletado(String entera) {
        if (!buscando()) return "";
        boolean empieza = PanelF1.normalizar(entera).startsWith(PanelF1.normalizar(texto));
        return empieza && entera.length() > texto.length() ? entera.substring(texto.length()) : "";
    }
}
