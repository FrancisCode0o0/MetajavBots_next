package metajavbots.progreso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Naturaleza;
import metajavbots.piezas.Pieza;

/**
 * PROGRESO — Las reglas del avance (docs/NIVELES.md): qué hace falta para
 * subir de etiqueta, cuándo se baja, qué piezas y modelos tiene a mano el
 * jugador, cuántos robots puede tener, lo fuerte que sale la computadora en
 * cada pelea (adaptada a su etiqueta, fija durante toda la pelea) y el botín.
 */
public final class Progreso {

    // TODO-FRAN: los números finos del avance (victorias por escalón, porcentaje de ejercicios, botín) son tuyos.
    /** La parte de los ejercicios de los temas de la etiqueta que hay que resolver para subir. */
    public static final double PARTE_DE_EJERCICIOS = 0.7;
    /** Derrotas seguidas a partir de las cuales se puede bajar de etiqueta. */
    public static final int DERROTAS_PARA_BAJAR = 5;
    /** Victorias totales para ganarse el 2.º, el 3.º y el 4.º robot. */
    public static final int[] VICTORIAS_POR_ROBOT = {0, 30, 80, 150};
    public static final int VICTORIAS_EXTREMA_PARA_MASTER = 25;
    public static final int PELEAS_ENTRE_DESAFIOS = 5;

    private Progreso() {
    }

    // ================================================================ subir y bajar

    /** Victorias en la etiqueta que hacen falta para subir de ella. */
    public static int victoriasParaSubir(Etiqueta e) {
        return 3 + e.ordinal() / 2 + e.escalon();
    }

    /** Los ejercicios de los temas que se aprenden en esta etiqueta. */
    public static List<Ejercicio> ejerciciosDe(Etiqueta e) {
        List<Ejercicio> l = new ArrayList<>();
        for (Tema t : Tema.todos()) {
            if (t.etiqueta.equals(e.name())) l.addAll(Banco.delJuego().deTema(t.id));
        }
        return l;
    }

    /** Lo que le falta al jugador para subir (vacío si ya puede). */
    public static final class Requisitos {
        public int victorias, victoriasQueTiene, ejercicios, ejerciciosQueTiene;
        public int victoriasExtrema, victoriasExtremaQueTiene;

        public boolean cumple() {
            return victoriasQueTiene >= victorias && ejerciciosQueTiene >= ejercicios && victoriasExtremaQueTiene >= victoriasExtrema;
        }
    }

    public static Requisitos requisitos(Jugador j) {
        Requisitos r = new Requisitos();
        Etiqueta e = j.etiqueta;
        if (e == Etiqueta.MASTER) {
            r.victorias = Integer.MAX_VALUE;
            return r;
        }
        r.victorias = victoriasParaSubir(e);
        r.victoriasQueTiene = j.victoriasEnLaEtiqueta;
        List<Ejercicio> ej = e == Etiqueta.SENIOR_4 ? Banco.delJuego().todos() : ejerciciosDe(e);
        r.ejercicios = (int) Math.ceil(ej.size() * (e == Etiqueta.SENIOR_4 ? 0.8 : PARTE_DE_EJERCICIOS));
        for (Ejercicio x : ej) if (j.ejerciciosResueltos.contains(x.id)) r.ejerciciosQueTiene++;
        if (e == Etiqueta.SENIOR_4) {
            // Master: el límite de verdad. Ganarle muchas veces a la extrema.
            r.victoriasExtrema = VICTORIAS_EXTREMA_PARA_MASTER;
            r.victoriasExtremaQueTiene = j.victoriasExtrema;
        }
        return r;
    }

    /** Lo que cambió tras una pelea o un ejercicio (para la pantalla: fuegos artificiales, botín…). */
    public static final class Cambio {
        public boolean subio, bajo;
        public Etiqueta antes, despues;
        public final List<Pieza> botin = new ArrayList<>();
        public final List<Modelo> modelosNuevos = new ArrayList<>();
        public int robotsNuevos;
        public long experiencia;
    }

    /** Apunta una pelea en el historial y aplica sus consecuencias. */
    public static Cambio despuesDeLaPelea(Jugador j, Registro r, String naturalezaDelRival, Random azar) {
        Cambio c = new Cambio();
        c.antes = j.etiqueta;
        int robotsAntes = robotsQuePuedeTener(j);
        List<Modelo> modelosAntes = modelosDisponibles(j);
        j.historial.add(r);
        j.registrarUltimoRival(r.rival, metajavbots.rivales.Rivales.codigoParaModo(r.modo));
        j.peleasDesdeElDesafio++;
        boolean cuenta = !r.modo.equals("PRACTICA") && !r.modo.equals("AJENO");
        if (r.resultado == Registro.Resultado.VICTORIA) {
            j.victoriasSeguidas++;
            j.derrotasSeguidas = 0;
            if (cuenta) j.victoriasEnLaEtiqueta++;
            if (r.nivelRival >= 0.99) j.victoriasExtrema++;
            c.experiencia = 30 + Math.round(70 * r.nivelRival);
            Pieza p = botin(j, naturalezaDelRival, azar);
            if (p != null) {
                j.piezasGanadas.add(p.id);
                c.botin.add(p);
            }
        } else if (r.resultado == Registro.Resultado.DERROTA) {
            j.derrotasSeguidas++;
            j.victoriasSeguidas = 0;
            if (cuenta) j.derrotasEnLaEtiqueta++;
            c.experiencia = 10;
        } else {
            c.experiencia = 20;
        }
        j.experiencia += c.experiencia;
        comprobarSubida(j, c);
        if (!c.subio && cuenta && debeBajar(j)) {
            j.etiqueta = j.etiqueta.anterior();
            j.victoriasEnLaEtiqueta = 0;
            j.derrotasEnLaEtiqueta = 0;
            j.derrotasSeguidas = 0;
            c.bajo = true;
        }
        c.despues = j.etiqueta;
        c.robotsNuevos = Math.max(0, robotsQuePuedeTener(j) - robotsAntes);
        for (Modelo m : modelosDisponibles(j)) if (!modelosAntes.contains(m)) c.modelosNuevos.add(m);
        return c;
    }

    /** Se baja si pierde muchas seguidas y en la etiqueta lleva muchas más derrotas que victorias. */
    static boolean debeBajar(Jugador j) {
        return j.etiqueta != Etiqueta.NOOB_1 && j.derrotasSeguidas >= DERROTAS_PARA_BAJAR
                && j.derrotasEnLaEtiqueta > 2 * j.victoriasEnLaEtiqueta + 4;
    }

    /** Si cumple los requisitos, sube (una etiqueta cada vez). */
    public static void comprobarSubida(Jugador j, Cambio c) {
        if (j.etiqueta == Etiqueta.MASTER) return;
        if (requisitos(j).cumple()) subir(j, c);
    }

    static void subir(Jugador j, Cambio c) {
        j.etiqueta = j.etiqueta.siguiente();
        j.victoriasEnLaEtiqueta = 0;
        j.derrotasEnLaEtiqueta = 0;
        c.subio = true;
        c.despues = j.etiqueta;
    }

    /** Un ejercicio resuelto: experiencia, retos que desbloquea y, quizá, subir. */
    public static Cambio despuesDelEjercicio(Jugador j, Ejercicio e) {
        Cambio c = new Cambio();
        c.antes = j.etiqueta;
        // El reto del día (se mira antes de apuntarlo: después ya sería otro).
        long hoy = RetoDiario.hoy();
        Ejercicio delDia = RetoDiario.de(j, hoy);
        if (delDia != null && delDia.id.equals(e.id)) c.experiencia += RetoDiario.hecho(j, hoy, e.id);
        if (j.ejerciciosResueltos.add(e.id)) {
            int exp = e.experiencia();
            c.experiencia += exp;
            j.experiencia += exp;
            if (e.desbloquea.startsWith("reto:")) {
                String reto = e.desbloquea.substring(5);
                j.retosHechos.add(reto);
                for (Pieza p : CatalogoDePiezas.delJuego().todas()) if (p.desbloqueo.equals("reto:" + reto)) c.botin.add(p);
            }
        }
        comprobarSubida(j, c);
        c.despues = j.etiqueta;
        return c;
    }

    // ================================================================ lo que tiene a mano

    /** ¿Cumple el jugador esta regla de desbloqueo («victorias:8+nivel:JUNIOR_1»…)? */
    public static boolean cumple(Jugador j, String regla) {
        for (String parte : regla.split("\\+")) {
            parte = parte.strip();
            if (parte.isEmpty() || parte.equals("inicio")) continue;
            int i = parte.indexOf(':');
            String que = i < 0 ? parte : parte.substring(0, i), valor = i < 0 ? "" : parte.substring(i + 1);
            boolean vale;
            switch (que) {
                case "victorias":
                    vale = j.victorias() >= entero(valor);
                    break;
                case "nivel":
                    vale = j.etiqueta.alMenos(Etiqueta.de(valor));
                    break;
                case "jefe":
                    vale = j.jefesVencidos.contains(valor);
                    break;
                case "reto":
                    vale = j.retosHechos.contains(valor);
                    break;
                case "pruebas":
                    vale = j.laboratoriosHechos.contains(valor);
                    break;
                case "desafio":
                default:
                    vale = false;
                    break;
            }
            if (!vale) return false;
        }
        return true;
    }

    public static boolean piezaDisponible(Jugador j, Pieza p) {
        if (j.piezasGanadas.contains(p.id) || j.legendariasGanadas.contains(p.id)) return true;
        // Con muchas partes desbloqueadas en Master, como dice el encargo: todas las comunes y raras.
        if (j.etiqueta == Etiqueta.MASTER && p.rareza.ordinal() <= Pieza.Rareza.RARA.ordinal()) return true;
        return cumple(j, p.desbloqueo);
    }

    /** ¿Puede ponerle esta pieza a este robot? (las de fábrica de su modelo, siempre). */
    public static boolean piezaUsable(Jugador j, Pieza p, Modelo m) {
        return m.piezas.contains(p.id) || piezaDisponible(j, p);
    }

    public static boolean modeloDisponible(Jugador j, Modelo m) {
        return j.modelosGanados.contains(m.id) || cumple(j, m.desbloqueo);
    }

    public static List<Modelo> modelosDisponibles(Jugador j) {
        List<Modelo> l = new ArrayList<>();
        for (Modelo m : Modelo.todos()) if (modeloDisponible(j, m)) l.add(m);
        return l;
    }

    /** Los robots que puede tener: lo que permite su categoría, si además se los ha ganado con victorias. */
    public static int robotsQuePuedeTener(Jugador j) {
        int porVictorias = 0;
        for (int v : VICTORIAS_POR_ROBOT) if (j.victorias() >= v) porVictorias++;
        return Math.max(1, Math.min(j.etiqueta.robotsPermitidos(), porVictorias));
    }

    // ================================================================ la computadora

    /**
     * Lo fuerte que sale la computadora en la próxima pelea: el de su etiqueta, con un poco de azar (unas peleas
     * más fáciles que otras) y algo más fácil tras varias derrotas seguidas, para que no frustre. No cambia durante la pelea.
     */
    public static double nivelParaLaPelea(Jugador j, Random azar) {
        double n = j.etiqueta.nivelDeLaIA();
        n += azar.nextGaussian() * 0.07;
        if (j.derrotasSeguidas >= 2) n -= 0.06 * Math.min(4, j.derrotasSeguidas - 1);
        if (j.victoriasSeguidas >= 3) n += 0.04;
        return Math.max(0, Math.min(1, n));
    }

    public static boolean puedeDesafiar(Jugador j) {
        return j.peleasDesdeElDesafio >= PELEAS_ENTRE_DESAFIOS && j.etiqueta != Etiqueta.MASTER;
    }

    /** El desafío: una IA tres escalones por encima. */
    public static double nivelDelDesafio(Jugador j) {
        Etiqueta e = j.etiqueta;
        for (int i = 0; i < 3; i++) e = e.siguiente();
        return Math.min(1, e.nivelDeLaIA() + 0.05);
    }

    /** Las legendarias que aún no tiene (para elegir premio tras ganar un desafío). */
    public static List<Pieza> legendariasPorGanar(Jugador j) {
        List<Pieza> l = new ArrayList<>();
        for (Pieza p : CatalogoDePiezas.delJuego().todas()) {
            if (p.desbloqueo.equals("desafio") && !j.legendariasGanadas.contains(p.id)) l.add(p);
        }
        return l;
    }

    /** Gana un desafío: sube de etiqueta (si elige eso) o se lleva una legendaria. */
    public static Cambio premioDelDesafio(Jugador j, boolean subirDeNivel, Pieza legendaria) {
        Cambio c = new Cambio();
        c.antes = j.etiqueta;
        j.peleasDesdeElDesafio = 0;
        if (subirDeNivel && j.etiqueta != Etiqueta.SENIOR_4) {
            subir(j, c);
        } else if (legendaria != null) {
            j.legendariasGanadas.add(legendaria.id);
            c.botin.add(legendaria);
        }
        c.despues = j.etiqueta;
        return c;
    }

    /** El botín de una victoria: un núcleo de la naturaleza del rival (de la marca que toca), o una pieza casi ganada. */
    static Pieza botin(Jugador j, String naturalezaDelRival, Random azar) {
        CatalogoDePiezas c = CatalogoDePiezas.delJuego();
        Naturaleza n = Naturaleza.de(naturalezaDelRival);
        int marca = Math.min(5, 1 + j.etiqueta.ordinal() / 3);
        if (n != Naturaleza.NINGUNA) {
            for (int m = 1; m <= marca; m++) {
                Pieza p = c.pieza("naturaleza_" + n.name().toLowerCase(Locale.ROOT) + (m == 1 ? "" : "_" + m));
                if (p != null && !piezaDisponible(j, p)) return p;
            }
        }
        if (azar.nextDouble() < 0.5) {
            List<Pieza> casi = new ArrayList<>();
            for (Pieza p : c.todas()) {
                if (piezaDisponible(j, p) || !p.desbloqueo.startsWith("victorias:") || p.desbloqueo.contains("+")) continue;
                if (entero(p.desbloqueo.substring(10)) <= j.victorias() + 6) casi.add(p);
            }
            if (!casi.isEmpty()) return casi.get(azar.nextInt(casi.size()));
        }
        return null;
    }

    private static int entero(String s) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
