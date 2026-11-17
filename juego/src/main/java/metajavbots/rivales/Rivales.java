package metajavbots.rivales;

import java.util.List;
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

    private Rivales() {
    }

    /** Un rival con las piezas de fábrica de su modelo, mejoradas según el nivel (Mk I a Mk V). */
    public static Combatiente crear(Modelo modelo, double nivel, double agresividad, Random azar) {
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
        return combatiente(modelo.nombre.es, m, nivel, agresividad, estiloPara(m, azar), azar);
    }

    /** El muñeco de entrenamiento de los tutoriales (no cuenta para nada). */
    public static Combatiente muneco() {
        Montaje m = new Montaje(Modelo.de("chatarra"));
        Combatiente c = new Combatiente("Muñeco", m, new Fabrica() {
            @Override
            public RobotBase crear() {
                return new Muneco();
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
        return combatiente("¿" + modelo.nombre.es + "?", m, nivel, agresividad, estilos[azar.nextInt(estilos.length)], azar);
    }

    private static Combatiente combatiente(String nombre, Montaje m, double nivel, double agresividad, Estilo estilo, Random azar) {
        Ajuste ajuste = new Ajuste(nivel, agresividad, null);
        Fabrica f = new Fabrica() {
            @Override
            public RobotBase crear() {
                return new Rival(ajuste, estilo);
            }

            @Override
            public String clase() {
                return "Rival";
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
