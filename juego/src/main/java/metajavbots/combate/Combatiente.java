package metajavbots.combate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import metajavbots.api.RobotBase;
import metajavbots.piezas.Montaje;

/**
 * COMBATIENTE — Uno de los dos de una pelea, lo que dura las cuatro rondas:
 * su montaje, su código (Fabrica), su memoria, su consola, las abolladuras
 * que va acumulando y lo que lleva de daño. Lo de cada ronda está en Cuerpo.
 */
public final class Combatiente {

    /** Una abolladura: en qué parte, dónde (0..1) y cómo de grande. Se quedan toda la pelea. */
    public static final class Abolladura {
        public final int parte;
        public final float u, v, tamano;
        public final boolean quemadura;

        Abolladura(int parte, float u, float v, float tamano, boolean quemadura) {
            this.parte = parte;
            this.u = u;
            this.v = v;
            this.tamano = tamano;
            this.quemadura = quemadura;
        }
    }

    /** Lo que pasó en una ronda (el informe entre rondas). */
    public static final class Informe {
        public double danoRecibido, danoHecho;
        public int ataques, aciertos, errores, topes, turnosVistos, turnosCerca, turnosAturdido;
        public long pasosMaximos, pasosTotales;
        public int turnos;
        public final Map<String, int[]> porArma = new HashMap<>();   // arma → {ataques, aciertos}
        public final Map<String, Double> danoPorArmaRecibido = new HashMap<>();
        public boolean fueraDeCombate;

        void ataque(String arma) {
            ataques++;
            porArma.computeIfAbsent(arma, k -> new int[2])[0]++;
        }

        void acierto(String arma) {
            aciertos++;
            porArma.computeIfAbsent(arma, k -> new int[2])[1]++;
        }

        public double punteria() {
            return ataques == 0 ? 0 : (double) aciertos / ataques;
        }

        public double pasosMedios() {
            return turnos == 0 ? 0 : (double) pasosTotales / turnos;
        }
    }

    public final String nombre;
    public Montaje montaje;
    public Fabrica fabrica;
    /** El robot (su código) vivo; se crea al empezar la ronda si no existe o si cambió el código. */
    RobotBase robot;
    long versionDelRobot = Long.MIN_VALUE;
    /** Lo que el robot recuerda de una ronda a otra (memoria()). */
    public final Map<String, Object> memoria = new HashMap<>();
    /** La consola del robot (escribir y los errores). */
    public final List<String> consola = new ArrayList<>();
    public final List<Abolladura> abolladuras = new ArrayList<>();
    public final List<Informe> informes = new ArrayList<>();
    public double danoTotalRecibido, danoTotalHecho;
    /** Lo que pierde de vista la pelea: el último error (con su línea), para enseñarlo en el editor. */
    public String ultimoError = "";
    public int lineaDelUltimoError = -1;
    /** Es la computadora (para el locutor y los diálogos). */
    public boolean computadora;

    public Combatiente(String nombre, Montaje montaje, Fabrica fabrica) {
        this.nombre = nombre;
        this.montaje = montaje;
        this.fabrica = fabrica;
    }

    /** El robot vivo (para el depurador). */
    public RobotBase robot() {
        return robot;
    }

    void escribir(int turno, String linea) {
        if (linea == null) return;
        for (String l : linea.split("\n")) {
            consola.add(String.format("%4d │ %s", turno, l.length() > 300 ? l.substring(0, 300) + "…" : l));
        }
        while (consola.size() > 300) consola.remove(0);
    }

    void abollar(int parte, java.util.Random azar, double dano, boolean quemadura) {
        if (abolladuras.size() > 160) abolladuras.remove(0);
        float tam = (float) Math.min(1, 0.25 + dano / 25);
        abolladuras.add(new Abolladura(parte, azar.nextFloat(), azar.nextFloat(), tam, quemadura));
    }

    /** Cuánto daño lleva respecto a lo que aguanta en una ronda (0..1+): de esto salen el humo y las chispas. */
    public double estropeado() {
        double max = montaje.cifras().blindajeTotal();
        return max <= 0 ? 0 : danoTotalRecibido / (max * 1.6);
    }
}
