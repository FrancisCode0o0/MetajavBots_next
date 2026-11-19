package metajavbots.progreso;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import metajavbots.cerebro.Ranura;
import metajavbots.cerebro.Tarea;
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
    /** Los capítulos de campaña ganados («rocko:1», «karatebit:3»…). */
    public final Set<String> capitulosGanados = new LinkedHashSet<>();
    /** Los robots rotos que ha arreglado. */
    public final Set<String> robotsArreglados = new LinkedHashSet<>();
    /** El reto diario: el último día hecho (días desde 1970), su ejercicio, la racha y la mejor racha. */
    public long retoDiarioUltimo = Long.MIN_VALUE / 2;
    public String retoDiarioId = "";
    public int retoDiarioRacha, retoDiarioMejor;
    public final List<Registro> historial = new ArrayList<>();
    /** El último rival con el que se peleó (su nombre y su código real); vacíos si aún no ha peleado. */
    public String ultimoRivalNombre = "";
    public String ultimoRivalCodigo = "";
    /** El panel de la IA: modo (ADAPTADA o una Dificultad), agresividad, humor y cortesía (0..1). */
    public String modoIA = "ADAPTADA";
    public double agresividad = 0.4, humor = 0.6, cortesia = 0.5;
    /** Los tres cerebros de IA que puede integrar el jugador (las claves no se guardan aquí: las guarda Claves aparte). */
    public final Ranura[] cerebros = {new Ranura(), new Ranura(), new Ranura()};
    /** Qué cerebro (0, 1 o 2) hace cada tarea, por el orden de Tarea. */
    public final int[] reparto = repartoPorDefecto();
    /** La carpeta de KoboldCpp y los modelos (vacía: la de Jarvis). */
    public String carpetaModelos = "";
    /**
     * El cerebro de las partidas de antes de los tres cerebros: NINGUNO, LOCAL, EN_LINEA o AMBOS, con su proveedor, su
     * modelo y sus direcciones. Solo se lee para pasarlo a las ranuras (migrarCerebroAntiguo).
     */
    public String cerebro = "NINGUNO";
    public String proveedor = "anthropic", modeloEnLinea = "", direccionLocal = "http://localhost:5002", direccionEnLinea = "";
    public String modeloLocal = "";
    /** Ajustes de la pantalla y el sonido. */
    public float tamanoDeLetra = 18, volumen = 0.8f, volumenVoz = 0.9f;
    public boolean vozDelLocutor = true, temaClaro;
    /** La voz clara del locutor (Piper, si está); si no, la propia, más robótica. */
    public boolean vozClara = true;
    /** El entrenador del modo tutor (tutor/Entrenador). */
    public String entrenador = "tuercas";
    /** Cuánto ayuda el entrenador, de 0 (lo mínimo: preguntas) a 1 (lo máximo: pseudocódigo y trozos de código enseguida). */
    public float ayudaDelTutor = 0.5f;
    /** El entrenador flotante (pantallas/EntrenadorFlotante): si asoma, da tips y aconseja en la pelea; F2 lo abre igual. */
    public boolean entrenadorActivo = true;
    /** La música de fondo: si suena y a qué volumen. */
    public boolean musicaActiva = true;
    public float volumenMusica = 0.45f;
    /** Si las peleas al azar pueden salir en arenas con trampas (pinchos, sierras, foso…). */
    public boolean arenasConTrampas = true;
    /** Si salen aparatos en la arena de ronda en ronda (motor a chorro, antigravedad, descarga…). */
    public boolean aparatosEnLaArena = true;
    /** Las rondas de las peleas (3, 4, 6, 10 o 12, como en el boxeo; 6 de fábrica). */
    public int rondas = 6;
    /** Si sale el arma especial con cuenta atrás (un reto de código a mitad de pelea). */
    public boolean armasEspeciales = true;
    public int victoriasExtrema;

    public Jugador(String nombre) {
        this.nombre = nombre;
    }

    private static int[] repartoPorDefecto() {
        int[] r = new int[Tarea.values().length];
        for (Tarea t : Tarea.values()) r[t.ordinal()] = t.ranuraPorDefecto();
        return r;
    }

    /** El cerebro que hace esa tarea. */
    public int ranuraDe(Tarea t) {
        return Math.max(0, Math.min(cerebros.length - 1, reparto[t.ordinal()]));
    }

    /** ¿Hay algún cerebro puesto? */
    public boolean conCerebro() {
        migrarCerebroAntiguo();
        for (Ranura r : cerebros) if (!r.vacia()) return true;
        return false;
    }

    /**
     * Pasa el cerebro de antes (un solo campo: local, en línea o los dos) a las ranuras, si están vacías: el local a la 1
     * (lo rápido) y el de en línea a la 1 o, con los dos, a la 2 (lo pesado y los tutores). Después lo deja en NINGUNO.
     */
    public void migrarCerebroAntiguo() {
        if (cerebro == null || cerebro.equals("NINGUNO")) return;
        for (Ranura r : cerebros) if (!r.vacia()) return;
        boolean local = cerebro.equals("LOCAL") || cerebro.equals("AMBOS");
        boolean enLinea = cerebro.equals("EN_LINEA") || cerebro.equals("AMBOS");
        int siguiente = 0;
        if (local) {
            Ranura r = cerebros[siguiente++];
            r.tipo = Ranura.Tipo.LOCAL;
            r.modeloLocal = modeloLocal;
            r.direccionLocal = direccionLocal.contains(":5002") ? "" : direccionLocal;
        }
        if (enLinea) {
            Ranura r = cerebros[siguiente];
            r.tipo = Ranura.Tipo.EN_LINEA;
            r.proveedor = proveedor;
            r.modeloEnLinea = modeloEnLinea;
            r.direccionEnLinea = direccionEnLinea;
            // Antes, con los dos, los tutores eran del de en línea.
            if (local) reparto[Tarea.TUTOR.ordinal()] = 1;
        }
        cerebro = "NINGUNO";
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

    /** Apunta el último rival con el que se peleó (su nombre y su código real). */
    public void registrarUltimoRival(String nombreRival, String codigoRival) {
        ultimoRivalNombre = nombreRival == null ? "" : nombreRival;
        ultimoRivalCodigo = codigoRival == null ? "" : codigoRival;
    }

    /** ¿Ha disputado ya al menos una pelea (por último rival guardado o historial)? */
    public boolean haPeleado() {
        return ultimoRivalCodigo != null && !ultimoRivalCodigo.isBlank() || !historial.isEmpty();
    }
}
