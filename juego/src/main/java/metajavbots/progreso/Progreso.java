package metajavbots.progreso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.RetoDePieza;
import metajavbots.ejercicios.Tema;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
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
    /** Monedas ganadas al resolver un ejercicio por primera vez. */
    public static final double MONEDAS_POR_EJERCICIO = 0.25;
    /** Monedas ganadas al vencer en una pelea según la categoría del rango. */
    public static final double MONEDAS_VICTORIA_NOOB = 2.0;
    public static final double MONEDAS_VICTORIA_JUNIOR = 3.0;
    public static final double MONEDAS_VICTORIA_SENIOR = 3.5;

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
        public double monedas;

        /** Texto de transición en caso de bajada de etiqueta (p. ej. «Junior II ↓ Junior I»), o vacío si no bajó. */
        public String transicionBajada() {
            if (!bajo || antes == null || despues == null) return "";
            return antes.nombre + " ↓ " + despues.nombre;
        }

        /** Mensaje explícito de bajada de nivel en el idioma indicado, o vacío si no bajó. */
        public String mensajeBajada(metajavbots.textos.Idioma idioma) {
            if (!bajo) return "";
            return metajavbots.textos.T.en("final.bajas_aviso", idioma);
        }
    }

    /** Monedas otorgadas por ganar una pelea según el rango (Noob: 2, Junior: 3, Senior/Master: 3.5). */
    public static double monedasPorVictoria(Etiqueta e) {
        if (e == null) return MONEDAS_VICTORIA_NOOB;
        switch (e.categoria()) {
            case JUNIOR: return MONEDAS_VICTORIA_JUNIOR;
            case SENIOR:
            case MASTER: return MONEDAS_VICTORIA_SENIOR;
            default: return MONEDAS_VICTORIA_NOOB;
        }
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
            c.monedas = monedasPorVictoria(j.etiqueta);
            j.sumarMonedas(c.monedas);
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
            c.monedas = MONEDAS_POR_EJERCICIO;
            j.sumarMonedas(MONEDAS_POR_EJERCICIO);
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

    /** Resultado de intentar comprar una pieza en la tienda. */
    public enum CompraResultado {
        COMPRADA, NO_COMPRABLE, YA_LA_TIENE, RANGO_INSUFICIENTE, SALDO_INSUFICIENTE
    }

    /** ¿Tiene el jugador rango suficiente para comprar piezas de esta categoría en la tienda? */
    public static boolean rangoDesbloqueadoEnTienda(Jugador j, Etiqueta.Categoria rango) {
        return j != null && rango != null && j.etiqueta.categoria().ordinal() >= rango.ordinal();
    }

    /**
     * Comprueba si el jugador ha desbloqueado el Modo especial avanzado en el juego:
     * haber alcanzado el rango secreto {@link Etiqueta#MASTER} o haber vencido al menos
     * un jefe/gimnasio en Modo especial.
     */
    public static boolean modoEspecialDesbloqueado(Jugador j) {
        return j != null && (j.etiqueta == Etiqueta.MASTER || !j.jefesVencidos.isEmpty());
    }

    /**
     * Intenta comprar una pieza del catálogo con las monedas del jugador.
     * Si la compra es válida, descuenta el precio y la marca en {@code j.piezasPendientes}
     * (no equipable aún hasta superar su reto de código).
     */
    public static CompraResultado comprarPieza(Jugador j, Pieza p) {
        if (j == null || p == null || !p.comprable()) return CompraResultado.NO_COMPRABLE;
        if (piezaDisponible(j, p) || piezaPendiente(j, p)) return CompraResultado.YA_LA_TIENE;
        if (!rangoDesbloqueadoEnTienda(j, p.rangoTienda)) return CompraResultado.RANGO_INSUFICIENTE;
        if (!j.gastarMonedas(p.precio)) return CompraResultado.SALDO_INSUFICIENTE;
        j.piezasPendientes.add(p.id);
        return CompraResultado.COMPRADA;
    }

    /** ¿Fue comprada en la tienda y está aún pendiente de superar su reto de código? */
    public static boolean piezaPendiente(Jugador j, Pieza p) {
        return j != null && p != null && j.piezasPendientes.contains(p.id) && !piezaDisponible(j, p);
    }

    /** Piezas compradas en la tienda que aún esperan su reto de código para activarse. */
    public static List<Pieza> piezasPendientes(Jugador j) {
        List<Pieza> l = new ArrayList<>();
        if (j == null) return l;
        CatalogoDePiezas cat = CatalogoDePiezas.delJuego();
        for (String id : j.piezasPendientes) {
            Pieza p = cat.pieza(id);
            if (p != null && !piezaDisponible(j, p)) l.add(p);
        }
        return l;
    }

    /**
     * Activa una pieza tras superar su reto de código: sale de {@code piezasPendientes}, pasa a
     * {@code piezasGanadas}, guarda sus parámetros básicos acotados dentro de los límites de
     * equilibrio y, si el jugador tiene un robot activo, la equipa en el hueco correspondiente.
     */
    public static boolean activarPieza(Jugador j, Pieza p, Map<String, Double> ajustes) {
        if (j == null || p == null) return false;
        j.piezasPendientes.remove(p.id);
        j.piezasGanadas.add(p.id);
        Map<String, Double> acotados = RetoDePieza.acotarAjustes(p, ajustes);
        if (acotados.isEmpty()) j.ajustesDePiezas.remove(p.id);
        else j.ajustesDePiezas.put(p.id, acotados);
        Jugador.Robot r = j.robot();
        if (r != null) {
            Montaje.Hueco hueco = huecoPorDefecto(p.ranura);
            if (hueco != null) r.piezas.put(hueco, p.id);
        }
        return true;
    }

    private static Montaje.Hueco huecoPorDefecto(Pieza.Ranura ranura) {
        if (ranura == null) return null;
        switch (ranura) {
            case CABEZA: return Montaje.Hueco.CABEZA;
            case BRAZO: return Montaje.Hueco.IZQUIERDA;
            case PIERNAS: return Montaje.Hueco.PIERNAS;
            case NUCLEO: return Montaje.Hueco.NUCLEO;
            case NATURALEZA: return Montaje.Hueco.NATURALEZA;
            default: return null;
        }
    }

    public static boolean piezaDisponible(Jugador j, Pieza p) {
        if (p == null) return false;
        if (j.piezasGanadas.contains(p.id) || j.legendariasGanadas.contains(p.id)) return true;
        // Con muchas partes desbloqueadas en Master, como dice el encargo: todas las comunes y raras.
        if (j.etiqueta == Etiqueta.MASTER && p.rareza.ordinal() <= Pieza.Rareza.RARA.ordinal()) return true;
        return cumple(j, p.desbloqueo);
    }

    /** Piezas desbloqueadas para el jugador (en NOOB_1 nuevo: los 4 complementos básicos de inicio). */
    public static List<Pieza> piezasDisponibles(Jugador j) {
        List<Pieza> l = new ArrayList<>();
        for (Pieza p : CatalogoDePiezas.delJuego().todas()) if (piezaDisponible(j, p)) l.add(p);
        return l;
    }

    /** ¿Puede ponerle esta pieza a este robot? (las de fábrica de su modelo, siempre). */
    public static boolean piezaUsable(Jugador j, Pieza p, Modelo m) {
        return p != null && ((m != null && m.piezas.contains(p.id)) || piezaDisponible(j, p));
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

    /**
     * Probabilidad (0..1) de encuentro con múltiples objetivos en peleas normales según la etiqueta:
     * NOOB_1..NOOB_3 = 0 % (1v1 puro), NOOB_4 = 25 %, JUNIOR = 40 %, SENIOR y MASTER = 55 %.
     */
    public static double probabilidadMultiplesObjetivos(Etiqueta e) {
        if (e == null) return 0.0;
        if (e == Etiqueta.NOOB_1 || e == Etiqueta.NOOB_2 || e == Etiqueta.NOOB_3) return 0.0;
        if (e == Etiqueta.NOOB_4) return 0.25;
        if (e.categoria() == Etiqueta.Categoria.JUNIOR) return 0.40;
        return 0.55;
    }

    /** ¿Debe esta pelea normal enfrentar al jugador a múltiples objetivos (drones o robots ligeros)? */
    public static boolean tocaMultiplesObjetivos(Jugador j, Random azar) {
        if (j == null || azar == null) return false;
        double prob = probabilidadMultiplesObjetivos(j.etiqueta);
        return prob > 0 && azar.nextDouble() < prob;
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
                if (p != null && !piezaDisponible(j, p) && !piezaPendiente(j, p)) return p;
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
