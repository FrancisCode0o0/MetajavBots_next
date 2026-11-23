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
        /** Si este robot se ha compilado con éxito al menos una vez. */
        public boolean compiladoAlgunaVez;
        /** Último código fuente compilado con éxito para este robot (vacío si nunca se ha compilado). */
        public String codigoCompilado = "";

        public Robot(String nombre, String modelo) {
            this.nombre = nombre;
            this.modelo = modelo;
        }

        /** Indica si el robot tiene una versión válida compilada lista para pelear. */
        public boolean tieneCodigoCompilado() {
            return compiladoAlgunaVez && codigoCompilado != null && !codigoCompilado.isBlank();
        }

        /** Indica si el código del taller difiere de la última versión válida compilada. */
        public boolean tieneCambiosPendientes() {
            if (!tieneCodigoCompilado()) return false;
            String actual = codigo == null ? "" : codigo;
            return !actual.equals(codigoCompilado);
        }

        /** Registra una compilación válida verificada y guarda esa versión como la ejecutable. */
        public void registrarCompilacionValida(String fuenteCompilada) {
            compiladoAlgunaVez = true;
            codigoCompilado = fuenteCompilada == null ? "" : fuenteCompilada;
        }

        /** El montaje con las piezas que tiene puestas (las que falten, las de fábrica del modelo). */
        public Montaje montaje() {
            return montaje(null);
        }

        /** El montaje aplicando, si existen, los ajustes personalizados (ya acotados) de las piezas activadas. */
        public Montaje montaje(Map<String, Map<String, Double>> ajustes) {
            return montaje(ajustes, null);
        }

        /** El montaje aplicando ajustes acotados y resolviendo también piezas propias diseñadas por el jugador. */
        public Montaje montaje(Map<String, Map<String, Double>> ajustes, Map<String, Pieza> piezasPropias) {
            Montaje m = new Montaje(Modelo.de(modelo));
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            for (Map.Entry<Montaje.Hueco, String> e : piezas.entrySet()) {
                Pieza p = c.pieza(e.getValue());
                if (p == null && piezasPropias != null) p = piezasPropias.get(e.getValue());
                if (p != null) {
                    Map<String, Double> aj = ajustes == null ? null : ajustes.get(p.id);
                    m.poner(e.getKey(), aj == null || aj.isEmpty() ? p : metajavbots.ejercicios.RetoDePieza.aplicarAjustes(p, aj));
                }
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
    /** ¿Ya vio el aviso inicial del entrenador sobre compilar antes de pelear? */
    public boolean avisoCompilarVisto;
    public long experiencia;
    /** Monedas gastables (separadas de la experiencia), con precisión de centésimas (p. ej. 0.25, 3.5). */
    public double monedas;
    /** Centésimas de moneda acumuladas (0..99; cada 100 centésimas suman +1 moneda entera). */
    public int centimosDeMoneda;
    /** Cuartos de moneda equivalentes (centimosDeMoneda / 25; mantenido sincronizado por compatibilidad). */
    public int cuartosDeMoneda;
    public int victoriasEnLaEtiqueta, derrotasEnLaEtiqueta, derrotasSeguidas, victoriasSeguidas;
    /** Las peleas desde el último desafío (hay que esperar para el siguiente). */
    public int peleasDesdeElDesafio = 99;
    public final List<Robot> robots = new ArrayList<>();
    public int robotElegido;
    /** Piezas ganadas sueltas (botín, desafíos) o activadas tras comprar en la tienda, aparte de las que da cumplir su regla. */
    public final Set<String> piezasGanadas = new LinkedHashSet<>();
    /** Piezas compradas en la tienda pendientes de superar su reto de código para quedar activas/equipables. */
    public final Set<String> piezasPendientes = new LinkedHashSet<>();
    /** Ajustes básicos programados por el jugador para piezas activadas (clave de pieza → clave=valor acotados). */
    public final Map<String, Map<String, Double>> ajustesDePiezas = new LinkedHashMap<>();
    /** Código fuente Java de las piezas diseñadas por el jugador (id de pieza propia → código Java). */
    public final Map<String, String> piezasPropiasCodigo = new LinkedHashMap<>();
    private final Map<String, Pieza> cachePiezasPropias = new LinkedHashMap<>();
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
    /** Si el entrenador avisa en la pelea de las órdenes borradas y de los ataques con la mira lejos (Ajustes). */
    public boolean avisosDeOrdenes = true;
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

    /** Indica si alguno de los robots de la partida tiene una compilación válida verificada. */
    public boolean haCompiladoAlgunRobot() {
        for (Robot r : robots) if (r.tieneCodigoCompilado()) return true;
        return false;
    }

    /** Indica si el robot actualmente seleccionado tiene una compilación válida verificada. */
    public boolean robotActualCompilado() {
        Robot r = robot();
        return r != null && r.tieneCodigoCompilado();
    }

    /** El montaje de un robot del jugador aplicando sus ajustes de piezas acotados y sus piezas propias. */
    public Montaje montajeDe(Robot r) {
        return r == null ? null : r.montaje(ajustesDePiezas, mapaPiezasPropias());
    }

    /** Guarda (o actualiza) una pieza diseñada en Java por el jugador y su código fuente. */
    public void guardarPiezaPropia(Pieza pieza, String codigo) {
        if (pieza == null || codigo == null) return;
        piezasPropiasCodigo.put(pieza.id, codigo);
        cachePiezasPropias.put(pieza.id, pieza);
    }

    /** Elimina una pieza propia del jugador y la desmonta si estaba puesta en algún robot. */
    public void eliminarPiezaPropia(String id) {
        if (id == null) return;
        piezasPropiasCodigo.remove(id);
        cachePiezasPropias.remove(id);
        for (Robot r : robots) r.piezas.values().removeIf(id::equals);
    }

    /** Devuelve todas las piezas propias válidas compiladas del jugador (compilando bajo demanda si hace falta). */
    public List<Pieza> piezasPropias() {
        return new ArrayList<>(mapaPiezasPropias().values());
    }

    /** Mapa id → Pieza de las piezas propias válidas del jugador. */
    public Map<String, Pieza> mapaPiezasPropias() {
        for (Map.Entry<String, String> e : piezasPropiasCodigo.entrySet()) {
            if (!cachePiezasPropias.containsKey(e.getKey())) {
                metajavbots.piezas.DisenadorDePieza.Resultado res =
                        metajavbots.piezas.DisenadorDePieza.compilarYValidar(e.getValue(), idioma);
                if (res.valida && res.pieza != null) cachePiezasPropias.put(e.getKey(), res.pieza);
            }
        }
        return cachePiezasPropias;
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

    /** Redondea una cantidad de monedas a centésimas exactas. */
    public static double redondearMonedas(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** Formatea el saldo de monedas sin ceros sobrantes (0, 2, 3.5, 0.25). */
    public static String formatearMonedas(double valor) {
        long centesimas = Math.round(valor * 100.0);
        if (centesimas % 100 == 0) return Long.toString(centesimas / 100);
        if (centesimas % 10 == 0) return String.format(java.util.Locale.ROOT, "%.1f", centesimas / 100.0);
        return String.format(java.util.Locale.ROOT, "%.2f", centesimas / 100.0);
    }

    /** Suma monedas al saldo del jugador (solo cantidades positivas). */
    public void sumarMonedas(double cantidad) {
        if (cantidad > 0) monedas = redondearMonedas(monedas + cantidad);
    }

    /**
     * Suma centésimas exactas de moneda (p. ej. +20 por objeto destruido, +25 por perfección, +40 por robot defectuoso)
     * y convierte cada 100 centésimas en +1 moneda entera sin errores de coma flotante.
     * Devuelve cuántas monedas enteras extra se han completado con esta suma.
     */
    public int sumarCentimosDeMoneda(int centimos) {
        if (centimos <= 0) return 0;
        int total = Math.max(0, centimosDeMoneda) + centimos;
        int extra = total / 100;
        centimosDeMoneda = total % 100;
        cuartosDeMoneda = centimosDeMoneda / 25;
        if (extra > 0) sumarMonedas(extra);
        return extra;
    }

    /**
     * Suma cuartos de moneda (p. ej. +1 = +25 centésimas por carrera perfecta) y convierte cada 4 cuartos en +1 moneda entera.
     * Devuelve cuántas monedas enteras extra se han completado con esta suma.
     */
    public int sumarCuartosDeMoneda(int cuartos) {
        if (cuartos <= 0) return 0;
        return sumarCentimosDeMoneda(cuartos * 25);
    }

    /** Gasta monedas si hay saldo suficiente; devuelve si se pudo realizar el gasto. */
    public boolean gastarMonedas(double cantidad) {
        if (cantidad < 0 || monedas + 1e-9 < cantidad) return false;
        monedas = Math.max(0, redondearMonedas(monedas - cantidad));
        return true;
    }
}
