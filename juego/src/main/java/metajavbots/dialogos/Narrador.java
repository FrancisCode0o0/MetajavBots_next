package metajavbots.dialogos;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
import metajavbots.textos.Idioma;

/**
 * NARRADOR — Quién dice qué en la pelea: el locutor robot loco comenta lo
 * que pasa (sin atropellarse: una frase cada pocos segundos, salvo lo
 * gordo) y cada robot suelta frases de su personalidad cuando le dan, cuando
 * acierta, cuando se queda sin blindaje… La computadora se burla del
 * jugador según la agresividad del panel de la IA (pullas de ring, nunca de
 * odio: así están escritas en res/dialogos).
 */
public final class Narrador {

    private final Dialogos locutor = Dialogos.de("locutor");
    private final Dialogos[] robots = new Dialogos[2];
    private final String[] nombres = new String[2];
    private final Random azar = new Random();
    public Idioma idioma = Idioma.ES;
    public String jugador = "";
    /** De 0 a 1 (el panel de la IA). */
    public double agresividad = 0.4, humor = 0.6;
    /** Lo que dice el locutor (el texto lo pinta la pantalla; la voz, quien escuche). */
    public Consumer<String> alHablarLocutor;
    /** Lo que dice un robot (quién, texto). */
    public BiConsumer<Integer, String> alHablarRobot;
    private float esperaLocutor, tiempoSinAtacar;
    private final float[] esperaRobot = new float[2];
    private final boolean[] avisoPocoBlindaje = new boolean[2];
    private boolean primerGolpe;
    private int ultimoLider = -1;
    /** Pullas nuevas que ha escrito el cerebro de IA para la computadora (ya moderadas); se gastan al decirlas. */
    public final java.util.Deque<String> pullasDeLaIA = new java.util.concurrent.ConcurrentLinkedDeque<>();

    public Narrador(Combatiente a, Combatiente b) {
        robots[0] = Dialogos.de(a.montaje.modelo.personalidad);
        robots[1] = Dialogos.de(b.montaje.modelo.personalidad);
        nombres[0] = a.nombre;
        nombres[1] = b.nombre;
    }

    private Map<String, String> huecos(int quien, String pieza) {
        Map<String, String> h = new HashMap<>();
        h.put("yo", quien >= 0 ? nombres[quien] : "");
        h.put("rival", quien >= 0 ? nombres[1 - quien] : nombres[1]);
        h.put("jugador", jugador);
        h.put("pieza", pieza == null ? "" : pieza);
        return h;
    }

    /** Lo dice el locutor si toca (o si es importante, aunque acabe de hablar). */
    public void locutor(String evento, boolean importante, Map<String, String> huecos) {
        if (!importante && esperaLocutor > 0) return;
        String f = locutor.frase(evento, idioma, huecos == null ? huecos(-1, null) : huecos);
        if (f == null) return;
        esperaLocutor = importante ? 2.5f : 4.5f + azar.nextFloat() * 2;
        if (alHablarLocutor != null) alHablarLocutor.accept(f);
    }

    /** Lo dice un robot si toca. */
    public void robot(int quien, String evento, boolean importante, String pieza) {
        if (quien < 0 || quien > 1) return;
        if (!importante && esperaRobot[quien] > 0) return;
        String f = robots[quien].frase(evento, idioma, huecos(quien, pieza));
        if (f == null) return;
        esperaRobot[quien] = 3.5f + azar.nextFloat() * 3;
        if (alHablarRobot != null) alHablarRobot.accept(quien, f);
    }

    public void ronda(int ronda) {
        Map<String, String> h = huecos(-1, null);
        h.put("ronda", Integer.toString(ronda));
        locutor(ronda == 1 ? "inicio_pelea" : "inicio_ronda", true, h);
        robot(0, ronda == 1 ? "saludo" : "inicio_ronda", true, null);
        robot(1, ronda == 1 ? "saludo" : "inicio_ronda", true, null);
    }

    /** Mira los sucesos del turno y decide qué se dice. dt: el tiempo que ha pasado. */
    public void turno(Pelea pelea, float dt) {
        esperaLocutor -= dt;
        esperaRobot[0] -= dt;
        esperaRobot[1] -= dt;
        tiempoSinAtacar += dt;
        for (Suceso s : pelea.sucesos()) {
            int q = s.quien;
            switch (s.tipo) {
                case RONDA_EMPIEZA: ronda((int) s.valor); break;
                case LASER: case DISPARO: case MISIL: case GOLPE_CUERPO: case LLAMA:
                    tiempoSinAtacar = 0;
                    if (s.tipo == Suceso.Tipo.LASER && azar.nextFloat() < 0.15f) locutor("laser", false, null);
                    if (s.tipo == Suceso.Tipo.MISIL && azar.nextFloat() < 0.3f) locutor("misil", false, null);
                    if (s.tipo == Suceso.Tipo.GOLPE_CUERPO && azar.nextFloat() < 0.4f) locutor("cuerpo", false, null);
                    if (azar.nextFloat() < 0.05f) robot(q, "ataque", false, null);
                    break;
                case IMPACTO:
                    if (s.texto.equals("pared")) break;
                    if (!primerGolpe) {
                        primerGolpe = true;
                        locutor("primer_golpe", true, null);
                    } else if (s.valor >= 12) {
                        locutor("golpe_fuerte", false, null);
                    }
                    robot(q, s.valor >= 12 ? "golpe_fuerte" : "golpe", false, null);
                    if (azar.nextFloat() < 0.25f) robot(1 - q, "acierto", false, null);
                    pullas(pelea, 1 - q);
                    break;
                case FALLO_CUERPO:
                    if (azar.nextFloat() < 0.3f) robot(q, "fallo", false, null);
                    break;
                case ESQUIVA:
                    if (azar.nextFloat() < 0.4f) locutor("esquiva", false, null);
                    robot(q, "esquiva", false, null);
                    break;
                case BLOQUEO: if (azar.nextFloat() < 0.4f) locutor("bloqueo", false, null); break;
                case PARTE_ROTA: {
                    String pieza = pieza(s.texto);
                    Map<String, String> h = huecos(q, pieza);
                    locutor("parte_rota", true, h);
                    robot(q, "parte_rota", true, pieza);
                    break;
                }
                case FUERA_DE_COMBATE: locutor("fuera_de_combate", true, null); break;
                case ERROR_CODIGO:
                    locutor("error_codigo", false, null);
                    robot(q, "error_codigo", false, null);
                    break;
                case TOPE: if (azar.nextFloat() < 0.5f) locutor("tope", false, null); break;
                case PARED: if (azar.nextFloat() < 0.2f) locutor("pared", false, null); break;
                case CHOQUE: if (azar.nextFloat() < 0.4f) locutor("choque", false, null); break;
                case ESPECIAL: robot(q, "especial", false, null); break;
                case REPARACION: robot(q, "reparar", false, null); break;
                case RONDA_ACABA: {
                    locutor("fin_ronda", true, null);
                    Pelea.Estado e = pelea.estado();
                    if (e != Pelea.Estado.TERMINADA) {
                        double a = pelea.informe(0).danoRecibido, b = pelea.informe(1).danoRecibido;
                        robot(a <= b ? 0 : 1, "victoria_ronda", true, null);
                        robot(a <= b ? 1 : 0, "derrota_ronda", true, null);
                    }
                    break;
                }
                default: break;
            }
        }
        for (int i = 0; i < 2; i++) {
            Cuerpo c = pelea.cuerpo(i);
            if (c != null && !avisoPocoBlindaje[i] && c.blindaje() < 30) {
                avisoPocoBlindaje[i] = true;
                robot(i, "poco_blindaje", true, null);
            }
        }
        if (tiempoSinAtacar > 7) {
            tiempoSinAtacar = 0;
            locutor("aburrido", false, null);
        }
        // ¿Remontada? Quien va perdiendo pasa a ir ganando.
        double da = pelea.combatientes[0].danoTotalRecibido, db = pelea.combatientes[1].danoTotalRecibido;
        int lider = Math.abs(da - db) < 5 ? ultimoLider : da < db ? 0 : 1;
        if (ultimoLider >= 0 && lider != ultimoLider && pelea.ronda() >= 2) locutor("remontada", true, null);
        ultimoLider = lider;
    }

    /** Las burlas: la computadora (índice 1) según la agresividad; el robot del jugador, alguna suave. */
    private void pullas(Pelea pelea, int quienPega) {
        boolean computadora = pelea.combatientes[quienPega].computadora;
        double ganas = computadora ? agresividad : 0.15;
        if (azar.nextDouble() > ganas * 0.25) return;
        boolean dura = computadora && agresividad >= 0.6 && azar.nextDouble() < agresividad;
        if (computadora && !pullasDeLaIA.isEmpty() && esperaRobot[quienPega] <= 0 && azar.nextBoolean()) {
            String f = pullasDeLaIA.poll();
            esperaRobot[quienPega] = 4;
            if (f != null && alHablarRobot != null) alHablarRobot.accept(quienPega, f);
            return;
        }
        robot(quienPega, dura ? "pulla" : "burla", false, null);
    }

    public String pieza(String parte) {
        switch (parte) {
            case "cabeza": return idioma.elegir("cabeza", "Kopf", "head");
            case "izquierda": return idioma.elegir("brazo izquierdo", "linker Arm", "left arm");
            case "derecha": return idioma.elegir("brazo derecho", "rechter Arm", "right arm");
            default: return idioma.elegir("piernas", "Beine", "legs");
        }
    }
}
