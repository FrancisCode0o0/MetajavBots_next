package metajavbots.progreso;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.textos.Idioma;

/**
 * JUGADOR — Una partida guardada: quién juega, su etiqueta, sus robots (cada
 * uno con su modelo, sus piezas y su código), lo que ha ganado y resuelto, y
 * su historial. Lo guarda y lo lee Guardado (partidas/<nombre>/).
 */
public final class Jugador {

    /** Un robot del jugador. */
    public static final class Robot {
        public String nombre;
        public String modelo;
        public final EnumMap<Montaje.Hueco, String> piezas = new EnumMap<>(Montaje.Hueco.class);
        public String codigo = "";

        public Robot(String nombre, String modelo) {
            this.nombre = nombre;
            this.modelo = modelo;
        }

        /** El montaje con las piezas que tiene puestas (las que falten, las de fábrica del modelo). */
        public Montaje montaje() {
            Montaje m = new Montaje(Modelo.de(modelo));
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            for (Map.Entry<Montaje.Hueco, String> e : piezas.entrySet()) {
                Pieza p = c.pieza(e.getValue());
                if (p != null) m.poner(e.getKey(), p);
            }
            return m;
        }

        public void ponerPiezas(Montaje m) {
            piezas.clear();
            for (Map.Entry<Montaje.Hueco, Pieza> e : m.piezas().entrySet()) piezas.put(e.getKey(), e.getValue().id);
        }
    }

    public String nombre;
    public Idioma idioma = Idioma.ES;
    public Etiqueta etiqueta = Etiqueta.NOOB_1;
    /** ¿Ya hizo la prueba de nivel del principio? */
    public boolean pruebaHecha;
    public long experiencia;
    public int victoriasEnLaEtiqueta, derrotasEnLaEtiqueta, derrotasSeguidas, victoriasSeguidas;
    /** Las peleas desde el último desafío (hay que esperar para el siguiente). */
    public int peleasDesdeElDesafio = 99;
    public final List<Robot> robots = new ArrayList<>();
    public int robotElegido;
    /** Piezas ganadas sueltas (botín, desafíos), aparte de las que da cumplir su regla. */
    public final Set<String> piezasGanadas = new LinkedHashSet<>();
    public final Set<String> modelosGanados = new LinkedHashSet<>();
    public final Set<String> ejerciciosResueltos = new LinkedHashSet<>();
    /** El último código de cada ejercicio (para seguir donde se dejó). */
    public final Map<String, String> codigoDeEjercicios = new LinkedHashMap<>();
    public final Set<String> jefesVencidos = new LinkedHashSet<>();
    public final Set<String> retosHechos = new LinkedHashSet<>();
    public final Set<String> laboratoriosHechos = new LinkedHashSet<>();
    public final Set<String> tutorialesHechos = new LinkedHashSet<>();
    public final Set<String> legendariasGanadas = new LinkedHashSet<>();
    public final List<Registro> historial = new ArrayList<>();
    /** El panel de la IA: modo (ADAPTADA o una Dificultad), agresividad, humor y cortesía (0..1). */
    public String modoIA = "ADAPTADA";
    public double agresividad = 0.4, humor = 0.6, cortesia = 0.5;
    /** El cerebro de IA: NINGUNO, LOCAL, EN_LINEA o AMBOS (la clave no se guarda aquí: la guarda Claves aparte). */
    public String cerebro = "NINGUNO";
    public String proveedor = "anthropic", modeloEnLinea = "", direccionLocal = "http://localhost:5002", direccionEnLinea = "";
    /** El modelo local elegido (un .gguf de la carpeta) y la carpeta (vacía: la de Jarvis). */
    public String modeloLocal = "", carpetaModelos = "";
    /** Ajustes de la pantalla y el sonido. */
    public float tamanoDeLetra = 18, volumen = 0.8f, volumenVoz = 0.9f;
    public boolean vozDelLocutor = true, temaClaro;
    public int victoriasExtrema;

    public Jugador(String nombre) {
        this.nombre = nombre;
    }

    public Robot robot() {
        if (robots.isEmpty()) return null;
        return robots.get(Math.max(0, Math.min(robotElegido, robots.size() - 1)));
    }

    public int victorias() {
        int n = 0;
        for (Registro r : historial) if (r.resultado == Registro.Resultado.VICTORIA) n++;
        return n;
    }

    public int derrotas() {
        int n = 0;
        for (Registro r : historial) if (r.resultado == Registro.Resultado.DERROTA) n++;
        return n;
    }
}
