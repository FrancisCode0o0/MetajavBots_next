package metajavbots.ejercicios;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.progreso.Jugador;

/**
 * PREPARADOR — Ejercicios nuevos de cada tema, listos de antemano: con un
 * cerebro de IA, en segundo plano se le piden ejercicios del tema y solo se
 * guardan los que se comprueban solos (la solución pasa todos sus casos y la
 * plantilla no). Al pedir uno, sale el primero listo; si no hay, uno de las
 * plantillas con valores al azar; y si el tema no tiene, uno del banco que aún
 * no esté resuelto.
 */
public final class Preparador {

    /** Cuántos se tienen listos por tema. */
    private static final int LISTOS_POR_TEMA = 2;
    private static final Map<String, Deque<Ejercicio>> LISTOS = new HashMap<>();
    private static final Set<String> PIDIENDO = new HashSet<>();

    private Preparador() {
    }

    /** Si hay cerebro y faltan, pide en segundo plano ejercicios nuevos de ese tema. */
    public static synchronized void preparar(Jugador j, Tema tema) {
        if (tema == null || PIDIENDO.contains(tema.id) || cola(tema.id).size() >= LISTOS_POR_TEMA || !metajavbots.codigo.Compilador.hayCompilador()) return;
        Cerebro c = CerebroDelJuego.pesado(j);
        if (c == null) return;
        PIDIENDO.add(tema.id);
        Thread h = new Thread(() -> {
            try {
                String r = c.preguntar(CerebroDelJuego.sistemaTemario(j.idioma),
                        CerebroDelJuego.uno("Syllabus: topic " + tema.id + " (" + tema.nombre(metajavbots.textos.Idioma.EN)
                                + "). Create exercises ONLY for this topic, all with tema: " + tema.id + "."), 900);
                for (Ejercicio e : validos(r)) {
                    synchronized (Preparador.class) {
                        cola(tema.id).add(e);
                    }
                }
            } catch (Exception e) {
                // sin respuesta: quedan las plantillas
            } finally {
                synchronized (Preparador.class) {
                    PIDIENDO.remove(tema.id);
                }
            }
        }, "preparar-ejercicios");
        h.setDaemon(true);
        h.start();
    }

    /** ¿Está el cerebro preparando ejercicios de ese tema? */
    public static synchronized boolean preparando(String tema) {
        return PIDIENDO.contains(tema);
    }

    /** Cuántos hay listos de la IA para ese tema. */
    public static synchronized int listos(String tema) {
        return cola(tema).size();
    }

    /** Uno nuevo del tema: de la IA si hay listo; si no, de las plantillas; si no, uno del banco sin resolver (o null). */
    public static synchronized Ejercicio nuevo(Jugador j, String tema, Random r) {
        Ejercicio e = cola(tema).poll();
        if (e != null) return e;
        e = PlantillasDeEjercicios.generar(tema, r);
        if (e != null) return e;
        List<Ejercicio> del = Banco.delJuego().deTema(tema);
        for (Ejercicio b : del) if (!j.ejerciciosResueltos.contains(b.id)) return b;
        return del.isEmpty() ? null : del.get(r.nextInt(del.size()));
    }

    /** Los ejercicios de la respuesta que se comprueban solos. */
    static List<Ejercicio> validos(String respuesta) throws java.io.IOException {
        List<Ejercicio> l = Ejercicio.leer(new ByteArrayInputStream(respuesta.getBytes(StandardCharsets.UTF_8)), "ia");
        l.removeIf(e -> e.casos.size() < 3 || !Comprobador.comprobar(e, e.codigoSolucion(), Locale.ROOT).todoBien()
                || Comprobador.comprobar(e, e.codigoInicial(), Locale.ROOT).todoBien());
        return l;
    }

    private static Deque<Ejercicio> cola(String tema) {
        return LISTOS.computeIfAbsent(tema, k -> new ArrayDeque<>());
    }
}
