package metajavbots.combate;

import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * DIFICULTAD CARRERA — Los cuatro niveles de dificultad seleccionables en la
 * Carrera de obstáculos: Noob, Junior, Senior y Especial. Cada nivel escala de
 * forma claramente perceptible los obstáculos (visibles, ocultos y repentinos),
 * los robots defectuosos, los impulsos de velocidad y el nivel del rival IA.
 */
public enum DificultadCarrera {

    /** Nivel inicial: pista base (1,0×), pocos repentinos y defectuosos, más impulsos y rival accesible. */
    NOOB("Noob", "Noob", "Noob", "carrera_obs.dif_noob_resumen", false, 2.2, 1.0,
            new Pista(10, 2, 0.15, 0.30, 4, 250.0, 12, 2, 5, 3, 3),
            new Defectuosos(3, 30, 0.33, 0.18, 0.25),
            new Rival(0.28, 18.0, 56.0, 180.0, 0.75)),

    /** Nivel intermedio: pista ×4/3, equilibrio entre ocultos, repentinos, defectuosos, rampas, agua e impulsos. */
    JUNIOR("Junior", "Junior", "Junior", "carrera_obs.dif_junior_resumen", false, 2.8, 4.0 / 3.0,
            new Pista(14, 3, 0.35, 0.48, 8, 200.0, 8, 3, 5, 4, 5),
            new Defectuosos(6, 18, 0.50, 0.24, 0.35),
            new Rival(0.55, 9.0, 78.0, 240.0, 1.00)),

    /** Nivel exigente: pista ×(4/3)², alta densidad de trampas, varios modelos defectuosos activos y rival agresivo. */
    SENIOR("Senior", "Senior", "Senior", "carrera_obs.dif_senior_resumen", false, 3.5, 16.0 / 9.0,
            new Pista(18, 3, 0.55, 0.62, 13, 165.0, 5, 5, 4, 5, 7),
            new Defectuosos(10, 11, 0.70, 0.32, 0.50),
            new Rival(0.80, 3.5, 94.0, 290.0, 1.10)),

    /** Dificultad máxima: pista ×(4/3)³, requiere Modo especial; máxima densidad y rival de élite. */
    ESPECIAL("Especial", "Spezial", "Special", "carrera_obs.dif_especial_resumen", true, 4.3, 64.0 / 27.0,
            new Pista(22, 4, 0.72, 0.72, 18, 145.0, 3, 7, 3, 6, 9),
            new Defectuosos(14, 7, 0.86, 0.40, 0.65),
            new Rival(1.00, 0.5, 112.0, 340.0, 1.20));

    private record Pista(int sectores, int obstaculosPorSector, double proporcionOcultos,
            double proporcionIndestructibles, int cantidadRepentinos, double margenRepentino,
            int cantidadImpulsos, int cantidadTrampasMortales, int cantidadReparaciones,
            int cantidadRampas, int cantidadAguas) {
    }

    private record Defectuosos(int cantidad, int cadenciaDisparo, double proporcionMoviles,
            double escalaBlindaje, double escalaDano) {
    }

    private record Rival(double nivel, double errorPunteria, double avance, double alcanceEvasion, double escalaDano) {
    }

    private final String es;
    private final String de;
    private final String en;
    private final String claveResumen;

    /** Número de sectores con obstáculos a lo largo de la pista 6×. */
    public final int sectores;
    /** Cantidad base de obstáculos colocados en cada sector (a partir del sector 1). */
    public final int obstaculosPorSector;
    /** Proporción [0, 1] de obstáculos que permanecen ocultos en el mapa general inicial. */
    public final double proporcionOcultos;
    /** Proporción [0, 1] de trampas fijas indestructibles frente a objetos destructibles. */
    public final double proporcionIndestructibles;
    /** Cantidad total de obstáculos repentinos planificados durante la carrera. */
    public final int cantidadRepentinos;
    /** Margen mínimo de seguridad delante del corredor al aparecer un obstáculo repentino. */
    public final double margenRepentino;
    /** Cantidad de robots defectuosos desplegados en la pista. */
    public final int cantidadDefectuosos;
    /** Turnos entre disparos de los robots defectuosos (menor = cadencia más alta). */
    public final int cadenciaDisparoDefectuosos;
    /** Proporción [0, 1] de robots defectuosos móviles/erráticos frente a inmóviles. */
    public final double proporcionDefectuososMoviles;
    /** Escala de blindaje aplicada a los robots defectuosos. */
    public final double escalaBlindajeDefectuosos;
    /** Escala de daño aplicada a los disparos de los robots defectuosos. */
    public final double escalaDanoDefectuosos;
    /** Cantidad de objetos recogibles de impulso de velocidad en la pista. */
    public final int cantidadImpulsos;
    /** Cantidad de trampas mortales de destrucción instantánea en la pista. */
    public final int cantidadTrampasMortales;
    /** Cantidad de bonos de reparación espaciados a lo largo de la pista. */
    public final int cantidadReparaciones;
    /** Cantidad de rampas de salto colocadas delante de zonas peligrosas. */
    public final int cantidadRampas;
    /** Cantidad de zonas de caída de agua (cortocircuito breve) en la pista. */
    public final int cantidadAguas;
    /** Multiplicador de longitud de la pista respecto al tamaño base Noob (1, 4/3, (4/3)², (4/3)³). */
    public final double escalaPista;
    /** Multiplicador de daño recibido por los robots específico de Carrera de obstáculos (> 1.0). */
    public final double multiplicadorDano;
    /** Nivel táctico del rival de la máquina en [0, 1]. */
    public final double nivelRival;
    /** Error de puntería en grados del rival de la máquina (menor = mayor precisión). */
    public final double errorPunteriaRival;
    /** Impulso de avance por turno del rival de la máquina. */
    public final double avanceRival;
    /** Distancia de detección anticipada de obstáculos del rival de la máquina. */
    public final double alcanceEvasionRival;
    /** Multiplicador de daño del rival de la máquina en carrera. */
    public final double escalaDanoRival;
    /** Indica si este nivel exige tener desbloqueado el Modo especial del juego. */
    public final boolean requiereModoEspecial;

    DificultadCarrera(String es, String de, String en, String claveResumen, boolean requiereModoEspecial,
            double multiplicadorDano, double escalaPista, Pista pista, Defectuosos defectuosos, Rival rival) {
        this.es = es;
        this.de = de;
        this.en = en;
        this.claveResumen = claveResumen;
        this.requiereModoEspecial = requiereModoEspecial;
        this.multiplicadorDano = multiplicadorDano;
        this.escalaPista = escalaPista;
        this.sectores = pista.sectores;
        this.obstaculosPorSector = pista.obstaculosPorSector;
        this.proporcionOcultos = pista.proporcionOcultos;
        this.proporcionIndestructibles = pista.proporcionIndestructibles;
        this.cantidadRepentinos = pista.cantidadRepentinos;
        this.margenRepentino = pista.margenRepentino;
        this.cantidadImpulsos = pista.cantidadImpulsos;
        this.cantidadTrampasMortales = pista.cantidadTrampasMortales;
        this.cantidadReparaciones = pista.cantidadReparaciones;
        this.cantidadRampas = pista.cantidadRampas;
        this.cantidadAguas = pista.cantidadAguas;
        this.cantidadDefectuosos = defectuosos.cantidad;
        this.cadenciaDisparoDefectuosos = defectuosos.cadenciaDisparo;
        this.proporcionDefectuososMoviles = defectuosos.proporcionMoviles;
        this.escalaBlindajeDefectuosos = defectuosos.escalaBlindaje;
        this.escalaDanoDefectuosos = defectuosos.escalaDano;
        this.nivelRival = rival.nivel;
        this.errorPunteriaRival = rival.errorPunteria;
        this.avanceRival = rival.avance;
        this.alcanceEvasionRival = rival.alcanceEvasion;
        this.escalaDanoRival = rival.escalaDano;
    }

    /** Nombre localizado del nivel de dificultad en ES, DE o EN. */
    public String nombre(Idioma idioma) {
        Idioma id = idioma == null ? Idioma.ES : idioma;
        return id.elegir(es, de, en);
    }

    /** Clave en ui.txt con la descripción resumida de este nivel. */
    public String claveResumen() {
        return claveResumen;
    }

    /** Resumen breve localizado de lo que cambia en este nivel de dificultad. */
    public String resumen(Idioma idioma) {
        return T.en(claveResumen, idioma == null ? Idioma.ES : idioma);
    }

    /** Comprueba si el jugador puede seleccionar este nivel respetando el desbloqueo real del Modo especial. */
    public boolean disponiblePara(Jugador jugador) {
        if (!requiereModoEspecial) return true;
        return Progreso.modoEspecialDesbloqueado(jugador);
    }

    /** Nivel por defecto al entrar a la Carrera de obstáculos. */
    public static DificultadCarrera porDefecto() {
        return JUNIOR;
    }

    /** Devuelve {@code deseada} si está disponible para el jugador o {@link #porDefecto()} en caso contrario. */
    public static DificultadCarrera seleccionarPara(Jugador jugador, DificultadCarrera deseada) {
        if (deseada != null && deseada.disponiblePara(jugador)) return deseada;
        return porDefecto();
    }
}
