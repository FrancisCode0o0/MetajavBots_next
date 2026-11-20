package metajavbots.rivales;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import metajavbots.api.RobotBase;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Fabrica;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;

/**
 * RIVALES — Monta los robots de la computadora: con el modelo y el nivel
 * que toque (el modo adaptado, el desafío, el panel de la IA) o del todo al
 * azar (el modo azar: un metajavbot con piezas y habilidades al azar).
 */
public final class Rivales {

    /** Los rivales del juego cuyo código fuente se puede leer en el taller. */
    public static final List<String> CLASES = List.of("Rival", "Muneco");
    private static final Map<String, String> FUENTES = new HashMap<>();

    private Rivales() {
    }

    /** El código fuente de una clase de rival del juego (Rival o Muneco). */
    public static synchronized String codigoDe(String clase) {
        String clave = CLASES.contains(clase) ? clase : "Rival";
        String ya = FUENTES.get(clave);
        if (ya != null) return ya;
        String codigo = "// " + clave + ".java";
        try (InputStream in = Rivales.class.getResourceAsStream("/codigo_rival/" + clave + ".java")) {
            if (in != null) codigo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            codigo = "// " + e;
        }
        FUENTES.put(clave, codigo);
        return codigo;
    }

    /** El código real de un combatiente rival (su código propio si lo trae, o el de su clase). */
    public static String codigoDe(Combatiente rival) {
        if (rival != null && rival.fabrica != null) {
            String f = rival.fabrica.fuente();
            if (f != null && !f.isBlank()) return f;
            String c = rival.fabrica.clase();
            if (c != null && !c.isBlank()) return codigoDe(c);
        }
        return codigoDe("Rival");
    }

    /** El código de rival que corresponde a un modo del historial (para partidas guardadas de antes). */
    public static String codigoParaModo(String modo) {
        return "TUTORIAL".equals(modo) ? codigoDe("Muneco") : codigoDe("Rival");
    }

    /** El código de uno de los rivales válidos del juego, elegido al azar. */
    public static String codigoAlAzar(Random azar) {
        Random r = azar == null ? new Random() : azar;
        return codigoDe(CLASES.get(r.nextInt(CLASES.size())));
    }

    /** Un rival con las piezas de fábrica de su modelo, mejoradas según el nivel (Mk I a Mk V). */
    public static Combatiente crear(Modelo modelo, double nivel, double agresividad, Random azar) {
        return crear(modelo, nivel, agresividad, azar, false);
    }

    /** Con traza: compilado desde su código, para ver qué línea se ejecuta (el modo máquina contra máquina). */
    public static Combatiente crear(Modelo modelo, double nivel, double agresividad, Random azar, boolean traza) {
        Montaje m = new Montaje(modelo);
        int marca = 1 + (int) Math.round(Math.max(0, Math.min(1, nivel)) * 4);
        CatalogoDePiezas c = CatalogoDePiezas.delJuego();
        for (Montaje.Hueco h : Montaje.Hueco.values()) {
            Pieza p = m.pieza(h);
            if (p == null) continue;
            Pieza mejor = c.pieza(baseDe(p.id) + (marca == 1 ? "" : "_" + marca));
            if (mejor != null) m.poner(h, mejor);
        }
        // A partir de cierto nivel, lleva un núcleo de su naturaleza.
        if (nivel >= 0.4) {
            Pieza n = c.pieza("naturaleza_" + modelo.naturaleza.name().toLowerCase(java.util.Locale.ROOT) + (marca == 1 ? "" : "_" + marca));
            if (n != null) m.poner(Montaje.Hueco.NATURALEZA, n);
        }
        return combatiente(modelo.nombre.es, m, nivel, agresividad, estiloPara(m, azar), azar, traza);
    }

    /** El muñeco de entrenamiento de los tutoriales (no cuenta para nada). */
    public static Combatiente muneco() {
        Montaje m = new Montaje(Modelo.de("chatarra"));
        Combatiente c = new Combatiente("Muñeco", m, new Fabrica() {
            @Override
            public RobotBase crear() {
                return new Muneco();
            }

            @Override
            public String clase() {
                return "Muneco";
            }

            @Override
            public String fuente() {
                return codigoDe("Muneco");
            }
        });
        c.computadora = true;
        return c;
    }

    /** El modo azar: modelo, piezas y estilo al azar (de todas las piezas del juego, también las que el jugador no tiene). */
    public static Combatiente alAzar(double nivel, double agresividad, Random azar) {
        List<Modelo> modelos = Modelo.todos();
        Modelo modelo = modelos.get(azar.nextInt(modelos.size()));
        Montaje m = new Montaje(modelo);
        CatalogoDePiezas c = CatalogoDePiezas.delJuego();
        int marcaMax = 1 + (int) Math.round(Math.max(0, Math.min(1, nivel)) * 4);
        for (Montaje.Hueco h : Montaje.Hueco.values()) {
            List<Pieza> opciones = c.de(h.ranura);
            opciones.removeIf(p -> p.marca > marcaMax || p.rareza == Pieza.Rareza.LEGENDARIA && nivel < 0.9);
            if (opciones.isEmpty()) continue;
            if (h == Montaje.Hueco.NATURALEZA && azar.nextBoolean()) continue;
            m.poner(h, opciones.get(azar.nextInt(opciones.size())));
        }
        Estilo[] estilos = Estilo.values();
        return combatiente("¿" + modelo.nombre.es + "?", m, nivel, agresividad, estilos[azar.nextInt(estilos.length)], azar, false);
    }

    /** Un dron ligero de escolta para encuentros con múltiples objetivos (menos blindaje y daño). */
    public static Combatiente dron(double nivel, double agresividad, Random azar, int numero) {
        Modelo m = Modelo.de("chispa");
        if (m == null) m = Modelo.todos().get(0);
        double nivelDron = Math.max(0.05, nivel * 0.7);
        Combatiente base = crear(m, nivelDron, Math.min(0.6, agresividad), azar, false);
        Combatiente c = new Combatiente("Dron " + numero, base.montaje, base.fabrica);
        c.computadora = true;
        c.escalaBlindaje = 0.42;
        c.escalaDano = 0.40;
        return c;
    }

    /** Un robot ligero para escuadrones de 2 o 3 enemigos simultáneos (equilibrado para 1 contra varios). */
    public static Combatiente ligero(Modelo modelo, double nivel, double agresividad, Random azar, int totalEnemigos) {
        boolean trio = totalEnemigos >= 3;
        double nivelLigero = Math.max(0.05, nivel * (trio ? 0.75 : 0.85));
        Combatiente c = crear(modelo, nivelLigero, Math.min(0.7, agresividad), azar, false);
        c.escalaBlindaje = trio ? 0.48 : 0.64;
        c.escalaDano = trio ? 0.44 : 0.58;
        return c;
    }

    private static Combatiente combatiente(String nombre, Montaje m, double nivel, double agresividad, Estilo estilo, Random azar, boolean traza) {
        Ajuste ajuste = new Ajuste(nivel, agresividad, null);
        Fabrica conTraza = traza ? RivalConTraza.fabrica(ajuste, estilo) : null;
        Fabrica f = conTraza != null ? conTraza : new Fabrica() {
            @Override
            public RobotBase crear() {
                return new Rival(ajuste, estilo);
            }

            @Override
            public String clase() {
                return "Rival";
            }

            @Override
            public String fuente() {
                return codigoDe("Rival");
            }
        };
        Combatiente c = new Combatiente(nombre, m, f);
        c.computadora = true;
        return c;
    }

    /** El estilo que pega con sus armas: cuerpo a cuerpo → de frente; largo alcance → de lejos… */
    static Estilo estiloPara(Montaje m, Random azar) {
        int cuerpo = 0, lejos = 0;
        for (Montaje.Hueco h : new Montaje.Hueco[]{Montaje.Hueco.IZQUIERDA, Montaje.Hueco.DERECHA}) {
            Pieza p = m.pieza(h);
            if (p == null) continue;
            if (p.arma() == Pieza.Arma.CUERPO || p.arma() == Pieza.Arma.LLAMA) cuerpo++;
            else if (p.valor("alcance") >= 600) lejos++;
        }
        if (cuerpo == 2) return azar.nextBoolean() ? Estilo.EMBESTIDOR : Estilo.TANQUE;
        if (cuerpo == 1) return azar.nextBoolean() ? Estilo.TANQUE : Estilo.ACOSADOR;
        if (lejos >= 1) return azar.nextBoolean() ? Estilo.FRANCOTIRADOR : Estilo.DANZARIN;
        return azar.nextBoolean() ? Estilo.ACOSADOR : Estilo.DANZARIN;
    }

    /** «laser_basico_3» → «laser_basico». */
    static String baseDe(String id) {
        int i = id.lastIndexOf('_');
        if (i > 0 && id.substring(i + 1).matches("[2-9]|10")) return id.substring(0, i);
        return id;
    }
}
