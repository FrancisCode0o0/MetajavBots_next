package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import metajavbots.combate.Combatiente;
import metajavbots.combate.Pelea;
import metajavbots.combate.Suceso;
import metajavbots.combate.Terrenos;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.rivales.Ajuste;
import metajavbots.rivales.Estilo;
import metajavbots.rivales.Rival;

/**
 * PRUEBAS DEL TORNEO DE EQUILIBRIO — Ocho arquetipos (tanque con orugas,
 * francotirador ligero, embestidor con ruedas, volador, araña de cuerpo a
 * cuerpo, bípedo equilibrado, escudero y especialista en aparatos) pelean todos
 * contra todos en las nueve arenas con semillas fijas, con el mismo código de
 * la computadora: así lo que decide es el montaje. Ninguno puede ganar más del
 * 65 % en total; cada uno gana a algún estilo y pierde con otro; no hay
 * victorias en un turno ni peleas infinitas; y en ninguna arena gana siempre la
 * misma locomoción. La matriz sale por consola (docs/EQUILIBRIO.md).
 */
final class PruebasDeEquilibrioTorneo {

    private PruebasDeEquilibrioTorneo() {
    }

    /** Un arquetipo del torneo: su nombre, su modelo, sus piezas (cabeza, izquierda, derecha, piernas, núcleo) y su estilo. */
    record Arquetipo(String nombre, String modelo, String[] piezas, Estilo estilo, boolean cazaAparatos) {
    }

    static final int MARCA = 3;
    static final long[] SEMILLAS = {11L, 29L};

    static final List<Arquetipo> ARQUETIPOS = List.of(
            new Arquetipo("tanque", "bastion", new String[]{"cabeza_escudo", "canon_riel", "martillo", "orugas", "nucleo_blindado"},
                    Estilo.TANQUE, false),
            new Arquetipo("francotirador", "vigia", new String[]{"cabeza_ojo", "laser_francotirador", "canon_ligero", "piernas_bipedo",
                    "nucleo_bateria"}, Estilo.FRANCOTIRADOR, false),
            new Arquetipo("embestidor", "volteo", new String[]{"cabeza_turbo", "volteador", "canon_ligero", "ruedas", "nucleo_bateria"},
                    Estilo.EMBESTIDOR, false),
            new Arquetipo("volador", "grulla", new String[]{"cabeza_sombra", "lanzamisiles", "laser_basico", "alas", "nucleo_basico"},
                    Estilo.ACOSADOR, false),
            new Arquetipo("arana", "barrena", new String[]{"cabeza_furia", "pinza_taladro", "taladro", "patas_arana", "nucleo_blindado"},
                    Estilo.EMBESTIDOR, false),
            new Arquetipo("bipedo", "mazo", new String[]{"cabeza_radar", "canon_ligero", "espada", "piernas_bipedo", "nucleo_basico"},
                    Estilo.ACOSADOR, false),
            new Arquetipo("escudero", "cobalto", new String[]{"cabeza_escudo", "escudo_brazo", "canon_ligero", "piernas_bipedo",
                    "nucleo_blindado"}, Estilo.TANQUE, false),
            new Arquetipo("aparatos", "avispa", new String[]{"cabeza_pulso", "latigo", "ametralladora", "flotador", "nucleo_bateria"},
                    Estilo.DANZARIN, true));

    /** El montaje de un arquetipo (en la marca del torneo). */
    static Montaje montaje(Arquetipo a) {
        Montaje m = new Montaje(Modelo.de(a.modelo()));
        CatalogoDePiezas c = CatalogoDePiezas.delJuego();
        Montaje.Hueco[] huecos = {Montaje.Hueco.CABEZA, Montaje.Hueco.IZQUIERDA, Montaje.Hueco.DERECHA, Montaje.Hueco.PIERNAS, Montaje.Hueco.NUCLEO};
        for (int i = 0; i < huecos.length; i++) {
            Pieza p = c.pieza(a.piezas()[i] + (MARCA > 1 ? "_" + MARCA : ""));
            if (p == null) p = c.pieza(a.piezas()[i]);
            m.poner(huecos[i], p);
        }
        return m;
    }

    static Combatiente combatiente(Arquetipo a) {
        Ajuste ajuste = new Ajuste(0.6, 0.5, null);
        Combatiente c = new Combatiente(a.nombre(), montaje(a), () -> new Rival(ajuste, a.estilo(), a.cazaAparatos()));
        c.computadora = true;
        return c;
    }

    /** El resultado del torneo: victorias[i][j] de i contra j, peleas[i][j], y las locomociones que ganan en cada arena. */
    static final class Resultado {
        final int n = ARQUETIPOS.size();
        final int[][] victorias = new int[n][n], peleas = new int[n][n];
        final Map<String, Set<Pieza.Locomocion>> ganadoresPorArena = new LinkedHashMap<>();
        int primerasRondasCortas, peleasSinAcabar, total;

        double porcentaje(int i) {
            int v = 0, p = 0;
            for (int j = 0; j < n; j++) {
                v += victorias[i][j];
                p += peleas[i][j];
            }
            return p == 0 ? 0 : 100.0 * v / p;
        }

        double porcentaje(int i, int j) {
            return peleas[i][j] == 0 ? 0 : 100.0 * victorias[i][j] / peleas[i][j];
        }

        String matriz() {
            StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "%-14s", ""));
            for (Arquetipo a : ARQUETIPOS) b.append(String.format(Locale.ROOT, "%9.9s", a.nombre()));
            b.append("   total\n");
            for (int i = 0; i < n; i++) {
                b.append(String.format(Locale.ROOT, "%-14s", ARQUETIPOS.get(i).nombre()));
                for (int j = 0; j < n; j++) b.append(i == j ? "        —" : String.format(Locale.ROOT, "%8.0f%%", porcentaje(i, j)));
                b.append(String.format(Locale.ROOT, "  %5.1f%%%n", porcentaje(i)));
            }
            return b.toString();
        }
    }

    private static Resultado ultimo;

    /** Juega el torneo entero (una vez; las pruebas lo comparten). */
    static synchronized Resultado torneo() {
        if (ultimo != null) return ultimo;
        Resultado r = new Resultado();
        for (String arena : Terrenos.escenarios()) {
            Set<Pieza.Locomocion> ganan = EnumSet.noneOf(Pieza.Locomocion.class);
            for (int i = 0; i < r.n; i++) {
                for (int j = i + 1; j < r.n; j++) {
                    for (int s = 0; s < SEMILLAS.length; s++) {
                        boolean cambia = s % 2 == 1;
                        int a = cambia ? j : i, b = cambia ? i : j;
                        Pelea p = new Pelea(combatiente(ARQUETIPOS.get(a)), combatiente(ARQUETIPOS.get(b)),
                                SEMILLAS[s] * 1000 + i * 31 + j * 7 + arena.hashCode());
                        p.arena = Terrenos.arenaDePelea(arena, p.ancho, p.alto);
                        p.aparatos.activos = true;
                        jugar(p, r);
                        r.peleas[a][b]++;
                        r.peleas[b][a]++;
                        r.total++;
                        int g = p.ganador();
                        if (g == 0 || g == 1) {
                            int ganador = g == 0 ? a : b, perdedor = g == 0 ? b : a;
                            r.victorias[ganador][perdedor]++;
                            ganan.add(montaje(ARQUETIPOS.get(ganador)).cifras().locomocion);
                        }
                    }
                }
            }
            r.ganadoresPorArena.put(arena, ganan);
        }
        ultimo = r;
        return r;
    }

    private static void jugar(Pelea p, Resultado r) {
        int limite = p.rondas * p.turnosPorRonda + p.rondas + 5, pasos = 0;
        boolean primera = true;
        while (p.estado() != Pelea.Estado.TERMINADA && pasos++ < limite) {
            if (p.estado() != Pelea.Estado.LUCHANDO) p.empezarRonda();
            p.avanzar();
            if (primera && p.estado() != Pelea.Estado.LUCHANDO) {
                primera = false;
                for (Suceso su : p.historia()) {
                    if (su.tipo == Suceso.Tipo.FUERA_DE_COMBATE && su.turno < 30) r.primerasRondasCortas++;
                }
            }
        }
        if (p.estado() != Pelea.Estado.TERMINADA) r.peleasSinAcabar++;
    }

    static void apuntar() {
        caso("equilibrio: torneo de ocho arquetipos en todas las arenas (nadie pasa del 65 %, cada uno gana y pierde con alguien)", () -> {
            Resultado r = torneo();
            System.out.println("            torneo (" + r.total + " peleas, % de victorias de la fila contra la columna):\n" + r.matriz());
            for (int i = 0; i < r.n; i++) {
                String nom = ARQUETIPOS.get(i).nombre();
                cierto(r.porcentaje(i) <= 65.0, nom + " gana demasiado: " + String.format(Locale.ROOT, "%.1f%%", r.porcentaje(i)));
                boolean gana = false, pierde = false;
                for (int j = 0; j < r.n; j++) {
                    if (i == j) continue;
                    if (r.porcentaje(i, j) > 50) gana = true;
                    if (r.porcentaje(i, j) < 50 && r.porcentaje(j, i) > 50) pierde = true;
                }
                cierto(gana, nom + " no gana a ningún estilo");
                cierto(pierde, nom + " no pierde con ningún estilo");
            }
            cierto(r.primerasRondasCortas == 0, "victorias triviales en menos de un segundo: " + r.primerasRondasCortas);
            cierto(r.peleasSinAcabar == 0, "peleas que no acaban: " + r.peleasSinAcabar);
        });

        caso("equilibrio: en ninguna arena gana siempre la misma locomoción", () -> {
            Resultado r = torneo();
            for (Map.Entry<String, Set<Pieza.Locomocion>> e : r.ganadoresPorArena.entrySet()) {
                System.out.println("            " + e.getKey() + ": ganan " + e.getValue());
                cierto(e.getValue().size() >= 2, "en " + e.getKey() + " solo gana " + e.getValue());
            }
        });
    }
}
