package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import metajavbots.api.Enemigo;
import metajavbots.api.Enemy;
import metajavbots.api.Gegner;
import metajavbots.api.Metajavbot;
import metajavbots.api.MetajavbotDe;
import metajavbots.api.MetajavbotEn;
import metajavbots.combate.Arena;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.Proyectil;
import metajavbots.combate.Terrenos;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Equilibrio;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;

/**
 * PRUEBAS DE EQUILIBRIO — Que el peso, la carga y la locomoción importan de
 * verdad (docs/EQUILIBRIO.md), con semillas fijas: los tramos de peso frente a
 * carga, el giro que baja con la velocidad y la frenada doble; la ventaja de
 * cada locomoción en su terreno o frente a su contrario; que un arma pesada
 * gira y se recupera más despacio (salvo el retroceso en orugas y patas); la
 * potencia regulable de Robocode; el reparto de terrenos de las arenas; que los
 * robots de antes y los rivales cargan y pelean; y la API en los tres idiomas.
 * El torneo de arquetipos está en PruebasDeEquilibrioTorneo.
 */
final class PruebasDeEquilibrio {

    private PruebasDeEquilibrio() {
    }

    /** Un robot que no hace nada (las pruebas mueven y disparan a mano). */
    public static class Quieto extends Metajavbot {
    }

    private static final Montaje.Hueco[] ORDEN = {Montaje.Hueco.CABEZA, Montaje.Hueco.IZQUIERDA, Montaje.Hueco.DERECHA,
        Montaje.Hueco.PIERNAS, Montaje.Hueco.NUCLEO};

    /** Un montaje de ese modelo con esas piezas (cabeza, izquierda, derecha, piernas, núcleo; null deja la de fábrica). */
    static Montaje montaje(String modelo, String... piezas) {
        Montaje m = new Montaje(Modelo.de(modelo));
        for (int i = 0; i < piezas.length && i < ORDEN.length; i++) {
            if (piezas[i] == null) continue;
            Pieza p = CatalogoDePiezas.delJuego().pieza(piezas[i]);
            cierto(p != null, "existe la pieza " + piezas[i]);
            m.poner(ORDEN[i], p);
        }
        return m;
    }

    static Montaje conPiernas(String modelo, String piernas) {
        return montaje(modelo, null, null, null, piernas, null);
    }

    /** Una pelea de dos robots quietos, ya empezada, en esa arena (null: la de siempre, sin nada). */
    static Pelea pelea(Montaje a, Montaje b, String arena) {
        Pelea p = new Pelea(new Combatiente("a", a, Quieto::new), new Combatiente("b", b, Quieto::new), 7L, 1000, 700, 2000);
        if (arena != null) p.arena = Terrenos.arenaDePelea(arena, p.ancho, p.alto);
        p.empezarRonda();
        colocar(p.cuerpo(0), 150, 120, 0);
        colocar(p.cuerpo(1), 850, 600, 180);
        return p;
    }

    static void colocar(Cuerpo c, double x, double y, double rumbo) {
        c.x = x;
        c.y = y;
        c.rumbo = rumbo;
        c.mira = rumbo;
        c.velocidad = 0;
        c.detener();
    }

    static void cerca(double esperado, double obtenido, String que) {
        cierto(Math.abs(esperado - obtenido) < 1e-6, que + ": se esperaba " + esperado + " y salió " + obtenido);
    }

    static void apuntar() {
        apuntarPesoYGiro();
        apuntarLocomociones();
        apuntarFlotadorYVolador();
        apuntarArmas();
        apuntarTerrenosYCompatibilidad();
        apuntarApi();
    }

    // ---------------------------------------------------------------- peso frente a carga y giro

    private static void apuntarPesoYGiro() {
        caso("equilibrio: tramos de peso frente a carga (ligero, normal, sobrecarga, imposible)", () -> {
            Equilibrio eq = Equilibrio.delJuego();
            igual(Equilibrio.Tramo.LIGERO, eq.tramo(0.5), "0,50 es ligero");
            igual(Equilibrio.Tramo.NORMAL, eq.tramo(0.85), "0,85 es normal");
            igual(Equilibrio.Tramo.SOBRECARGA, eq.tramo(1.1), "1,10 es sobrecarga");
            igual(Equilibrio.Tramo.IMPOSIBLE, eq.tramo(1.3), "1,30 no puede pelear");
            cerca(1.0, eq.factorVelocidad(0.7), "sin castigo hasta 0,70");
            cerca(0.85, eq.factorVelocidad(1.0), "−15 % en r = 1");
            cerca(0.925, eq.factorGiro(0.85), "baja en línea recta en el tramo normal");
            cerca(0.6, eq.factorVelocidad(1.1), "sobrecarga: −40 % de velocidad");
            cerca(0.6, eq.factorAceleracion(1.1), "sobrecarga: −40 % de aceleración");
            cerca(0.7, eq.factorGiro(1.1), "sobrecarga: −30 % de giro");
            cerca(1.4, eq.factorEnergia(1.1), "sobrecarga: +40 % de energía al moverse");
            cerca(1.0, eq.factorEnergia(0.9), "normal: lo de siempre");
            Montaje ligero = new Montaje(Modelo.de("vigia"));
            Montaje normal = new Montaje(Modelo.de("grulla"));
            Montaje sobre = montaje("grulla", null, "martillo", "canon_riel", null, null);
            Montaje imposible = montaje("grulla", null, "ariete_cinetico", "canon_electromagnetico", null, null);
            igual(Equilibrio.Tramo.LIGERO, ligero.tramo(), "Vigía de fábrica va ligero (" + ligero.proporcionDeCarga() + ")");
            igual(Equilibrio.Tramo.NORMAL, normal.tramo(), "Grulla de fábrica va normal (" + normal.proporcionDeCarga() + ")");
            igual(Equilibrio.Tramo.SOBRECARGA, sobre.tramo(), "Grulla con martillo y cañón de riel va sobrecargada (" + sobre.proporcionDeCarga() + ")");
            igual(Equilibrio.Tramo.IMPOSIBLE, imposible.tramo(), "Grulla con ariete y riel cuántico no puede (" + imposible.proporcionDeCarga() + ")");
            double suma = Modelo.de("grulla").chasis;
            for (Map.Entry<Montaje.Hueco, Pieza> e : sobre.piezas().entrySet()) if (e.getKey() != Montaje.Hueco.PIERNAS) suma += e.getValue().valor("peso");
            cerca(suma, sobre.pesoTotal(), "el peso total suma el chasis y todo menos las piernas");
            cierto(sobre.puedePelear() && !imposible.puedePelear(), "por encima del 120 % no sale a pelear");
            Montaje.Cifras cn = normal.cifras(), cs = sobre.cifras();
            cierto(cs.velocidad < cn.velocidad * 0.75 && cs.aceleracion < cn.aceleracion * 0.75 && cs.giro < cn.giro * 0.85,
                    "sobrecargado corre, acelera y gira mucho peor");
            cierto(cs.sobrecargado() && cs.esquiva == 0, "sobrecargado no esquiva rápido");
            cierto(ligero.cifras().esquiva > new Montaje(Modelo.de("mazo")).cifras().esquiva, "el ligero esquiva algo más");
        });

        caso("equilibrio: el giro baja con la velocidad (Robocode) y frenar es el doble de rápido que acelerar", () -> {
            Pelea p = pelea(conPiernas("vigia", "piernas_bipedo"), conPiernas("vigia", "piernas_bipedo"), null);
            Cuerpo c = p.cuerpo(0);
            double parado = c.giroMaximo();
            c.velocidad = Cuerpo.velocidadSegura(c.cifras.velocidad);
            double deprisa = c.giroMaximo();
            cerca(1 - Equilibrio.delJuego().giroPorVelocidad, deprisa / parado, "a toda velocidad gira 1 − 0,4");
            colocar(c, 300, 350, 0);
            c.girar(90);
            p.avanzar();
            double giroParado = Math.abs(Pelea.normal(c.rumbo()));
            colocar(c, 300, 350, 0);
            c.velocidad = Cuerpo.velocidadSegura(c.cifras.velocidad);
            c.avanzar(5000);
            c.girar(90);
            p.avanzar();
            double giroDeprisa = Math.abs(Pelea.normal(c.rumbo()));
            cierto(giroDeprisa < giroParado * 0.7, "deprisa gira menos en un turno: " + giroDeprisa + " frente a " + giroParado);
            colocar(c, 300, 350, 0);
            c.velocidad = 1;
            c.avanzar(5000);
            p.avanzar();
            double acelera = c.velocidad() - 1;
            colocar(c, 300, 350, 0);
            c.velocidad = 3;
            c.avanzar(0);
            p.avanzar();
            double frena = 3 - c.velocidad();
            cerca(2.0, frena / acelera, "frena el doble de lo que acelera");
        });
    }

    // ---------------------------------------------------------------- locomociones

    private static void apuntarLocomociones() {
        caso("equilibrio: las orugas aguantan empujones y volteos mucho más que las ruedas", () -> {
            double[] empuje = new double[2];
            int[] volteo = new int[2];
            String[] piernas = {"orugas", "ruedas"};
            for (int k = 0; k < 2; k++) {
                Pelea p = pelea(new Montaje(Modelo.de("volteo")), conPiernas("vigia", piernas[k]), null);
                Cuerpo a = p.cuerpo(0), b = p.cuerpo(1);
                colocar(a, 300, 350, 0);
                colocar(b, 300 + a.radio + b.radio + 20, 350, 180);
                double x0 = b.x;
                cierto(a.atacar(Cuerpo.IZQUIERDA), "el volteador está listo");
                p.avanzar();
                empuje[k] = b.x - x0;
                volteo[k] = b.volteado;
            }
            cierto(empuje[0] < empuje[1] * 0.5, "las orugas se mueven menos de la mitad: " + empuje[0] + " frente a " + empuje[1]);
            cierto(volteo[0] < volteo[1], "las orugas casi no se voltean: " + volteo[0] + " frente a " + volteo[1]);
        });

        caso("equilibrio: las ruedas recorren más en llano y las patas rinden más en escombros", () -> {
            Pelea p = pelea(conPiernas("vigia", "ruedas"), conPiernas("vigia", "piernas_bipedo"), "HANGAR");
            Cuerpo r = p.cuerpo(0), b = p.cuerpo(1);
            colocar(r, 80, 200, 0);
            colocar(b, 80, 500, 0);
            r.avanzar(5000);
            b.avanzar(5000);
            for (int t = 0; t < 90; t++) p.avanzar();
            cierto(r.paso > b.paso * 1.2, "en llano las ruedas recorren más: " + r.paso + " frente a " + b.paso);
            Pelea d = pelea(conPiernas("vigia", "ruedas"), conPiernas("vigia", "patas_arana"), "DESIERTO");
            colocar(d.cuerpo(0), 300, 210, 0);
            colocar(d.cuerpo(1), 700, 490, 0);
            igual("ESCOMBROS", d.cuerpo(0).terreno(), "pisa escombros");
            igual("ESCOMBROS", d.cuerpo(1).terreno(), "pisa escombros");
            double vr = d.cuerpo(0).velocidadMaxima() / Cuerpo.velocidadSegura(d.cuerpo(0).cifras.velocidad);
            double vp = d.cuerpo(1).velocidadMaxima() / Cuerpo.velocidadSegura(d.cuerpo(1).cifras.velocidad);
            cierto(vp > 1 && vr < 0.6, "en escombros las patas rinden más (" + vp + ") y las ruedas mucho menos (" + vr + ")");
        });

    }

    private static void apuntarFlotadorYVolador() {
        caso("equilibrio: el flotador ignora el aceite y las trampas de suelo", () -> {
            double[] hielo = new double[2], llano = new double[2];
            String[] piernas = {"flotador", "piernas_bipedo"};
            for (int k = 0; k < 2; k++) {
                for (String arena : new String[]{"HIELO", "HANGAR"}) {
                    Pelea p = pelea(conPiernas("vigia", piernas[k]), conPiernas("vigia", "orugas"), arena);
                    Cuerpo c = p.cuerpo(0);
                    colocar(c, 150, 120, 0);
                    c.avanzar(5000);
                    for (int t = 0; t < 6; t++) p.avanzar();
                    if (arena.equals("HIELO")) hielo[k] = c.velocidad(); else llano[k] = c.velocidad();
                }
            }
            cerca(llano[0], hielo[0], "en el hielo (aceite) el flotador acelera igual que en llano");
            cierto(hielo[1] < llano[1] * 0.6, "las piernas resbalan: " + hielo[1] + " frente a " + llano[1]);
            for (int k = 0; k < 2; k++) {
                Pelea p = pelea(conPiernas("vigia", piernas[k]), conPiernas("vigia", "orugas"), "HIELO");
                colocar(p.cuerpo(0), 500, 350, 0);
                p.avanzar();
                double dano = p.combatientes[0].danoTotalRecibido;
                if (k == 0) cierto(dano == 0, "el flotador pasa sobre el foso sin caer");
                else cierto(dano > 10, "las piernas caen al foso: " + dano);
            }
        });

        caso("equilibrio: el volador cae más ante el imán y ante los misiles que persiguen", () -> {
            Pelea p = pelea(conPiernas("vigia", "alas"), conPiernas("vigia", "orugas"), "CHATARRERIA");
            colocar(p.cuerpo(0), 500, 350 + 150, 0);
            colocar(p.cuerpo(1), 500, 350 - 150, 0);
            p.avanzar();
            double tiraVolador = 150 - Math.abs(p.cuerpo(0).y - 350), tiraOrugas = 150 - Math.abs(p.cuerpo(1).y - 350);
            cierto(tiraVolador > tiraOrugas * 2, "el imán tira mucho más del volador: " + tiraVolador + " frente a " + tiraOrugas);
            double[] dano = new double[2];
            String[] piernas = {"alas", "piernas_bipedo"};
            for (int k = 0; k < 2; k++) {
                Pelea q = pelea(montaje("vigia", null, "lanzamisiles", null, null, null), conPiernas("vigia", piernas[k]), null);
                Cuerpo a = q.cuerpo(0), b = q.cuerpo(1);
                colocar(a, 200, 350, 0);
                colocar(b, 600, 350, 180);
                for (int t = 0; t < 400; t++) {
                    if (a.listo(Cuerpo.IZQUIERDA)) a.atacar(Cuerpo.IZQUIERDA);
                    q.avanzar();
                }
                dano[k] = q.combatientes[1].danoTotalRecibido;
            }
            cierto(dano[0] > dano[1] * 1.3, "los misiles hacen más daño al volador: " + dano[0] + " frente a " + dano[1]);
        });
    }

    // ---------------------------------------------------------------- armas pesadas y potencia

    private static void apuntarArmas() {
        caso("equilibrio: un arma más pesada gira y se recupera más despacio; el retroceso no afecta a orugas ni a patas", () -> {
            Montaje ligera = montaje("vigia", null, "laser_basico", "laser_basico", null, null);
            Montaje pesada = montaje("vigia", null, "canon_electromagnetico", "laser_basico", null, null);
            cierto(pesada.cifras().giroMira < ligera.cifras().giroMira * 0.8, "la mira gira más despacio con el arma pesada");
            Pelea p = pelea(ligera, pesada, null);
            p.cuerpo(0).apuntar(90);
            p.cuerpo(1).apuntar(90);
            p.avanzar();
            cierto(Math.abs(p.cuerpo(1).restanteMira()) > Math.abs(p.cuerpo(0).restanteMira()), "en un turno gira menos la mira pesada");
            String[] piernas = {"piernas_bipedo", "orugas", "patas_arana"};
            int[] espera = new int[3];
            double[] atras = new double[3];
            for (int k = 0; k < 3; k++) {
                Pelea q = pelea(montaje("vigia", null, "canon_electromagnetico", null, piernas[k], null), conPiernas("vigia", "orugas"), null);
                Cuerpo c = q.cuerpo(0);
                colocar(c, 400, 350, 90);
                colocar(q.cuerpo(1), 900, 650, 180);
                cierto(c.atacar(Cuerpo.IZQUIERDA), "dispara");
                q.avanzar();
                espera[k] = c.enfriamiento(Cuerpo.IZQUIERDA);
                atras[k] = 350 - c.y;
            }
            cierto(espera[0] > espera[1] && atras[0] > 3, "en piernas el retroceso lo echa atrás (" + atras[0] + ") y retrasa el disparo");
            igual(espera[1], espera[2], "orugas y patas esperan lo mismo");
            cierto(Math.abs(atras[1]) < 1e-9 && Math.abs(atras[2]) < 1e-9, "orugas y patas absorben el retroceso");
            Pieza riel = CatalogoDePiezas.delJuego().pieza("canon_electromagnetico");
            Montaje aligerada = new Montaje(Modelo.de("vigia"));
            aligerada.poner(Montaje.Hueco.IZQUIERDA, riel.conValores(Map.of("peso", 6.0)));
            Pelea q = pelea(aligerada, conPiernas("vigia", "orugas"), null);
            colocar(q.cuerpo(0), 400, 350, 90);
            q.cuerpo(0).atacar(Cuerpo.IZQUIERDA);
            q.avanzar();
            cierto(q.cuerpo(0).enfriamiento(Cuerpo.IZQUIERDA) < espera[0], "la misma arma, más ligera, se recupera antes");
        });

        caso("equilibrio: potencia regulable (más potencia: más daño, bala más lenta, más calor y más energía)", () -> {
            double[] vel = new double[2], dano = new double[2], energia = new double[2];
            int[] enfr = new int[2];
            double[] potencias = {1, 3};
            for (int k = 0; k < 2; k++) {
                Pelea p = pelea(montaje("vigia", null, "canon_ligero", null, null, null), conPiernas("vigia", "orugas"), null);
                Cuerpo c = p.cuerpo(0);
                colocar(c, 400, 350, 90);
                c.regularPotencia(potencias[k]);
                double antes = c.energia();
                cierto(c.atacar(Cuerpo.IZQUIERDA), "dispara a potencia " + potencias[k]);
                energia[k] = antes - c.energia();
                p.avanzar();
                Proyectil pr = p.proyectiles.get(0);
                vel[k] = pr.velocidad;
                dano[k] = pr.dano;
                enfr[k] = c.enfriamiento(Cuerpo.IZQUIERDA);
            }
            cerca(4.0, dano[1] / dano[0], "daño 4p + 2·max(p − 1, 0): a potencia 3, 16 frente a 4");
            cerca(11.0 / 17.0, vel[1] / vel[0], "velocidad 20 − 3p");
            cierto(enfr[1] > enfr[0], "más calor: " + enfr[1] + " frente a " + enfr[0]);
            cierto(energia[0] == 0 && energia[1] > 20, "a potencia 1 no gasta energía; a 3, mucha: " + energia[1]);
        });
    }

    // ---------------------------------------------------------------- terrenos de las arenas y compatibilidad

    private static void apuntarTerrenosYCompatibilidad() {
        caso("equilibrio: el reparto de terrenos de las arenas no favorece a ninguna locomoción", () -> {
            List<String> arenas = Terrenos.escenarios();
            igual(metajavbots.pantalla.VistaDeArena.Escenario.values().length, arenas.size(), "cada arena tiene su mezcla de terrenos");
            java.util.Set<Pieza.Locomocion> mejores = java.util.EnumSet.noneOf(Pieza.Locomocion.class);
            for (Pieza.Locomocion l : Pieza.Locomocion.values()) {
                int buenas = 0, malas = 0;
                for (String a : arenas) {
                    double r = Terrenos.rendimiento(a, l), media = Terrenos.rendimientoMedio(a);
                    if (r > media + 1e-9) buenas++;
                    if (r < media - 1e-9) malas++;
                }
                cierto(buenas >= 1 && malas >= 1, l + " tiene arenas donde brilla (" + buenas + ") y donde sufre (" + malas + ")");
            }
            StringBuilder b = new StringBuilder();
            for (String a : arenas) {
                mejores.add(Terrenos.mejorEn(a));
                b.append(a).append(": mejor ").append(Terrenos.mejorEn(a)).append(", peor ").append(Terrenos.peorEn(a)).append("; ");
            }
            System.out.println("            " + b);
            cierto(mejores.size() >= 3, "la mejor locomoción cambia de una arena a otra: " + mejores);
            double suma = 0;
            for (double v : Terrenos.reparto("DESIERTO").values()) suma += v;
            cerca(1.0, suma, "el reparto de una arena suma 1");
            igual(0, Arena.de("HANGAR", 1000, 700).trampas().size(), "Arena.de (pruebas y demostraciones) no cambia");
        });

        caso("equilibrio: robots guardados sobrecargados cargan sin romperse y los rivales siempre pueden pelear", () -> {
            Jugador.Robot viejo = new Jugador.Robot("Viejo", "grulla");
            viejo.piezas.put(Montaje.Hueco.IZQUIERDA, "ariete_cinetico");
            viejo.piezas.put(Montaje.Hueco.DERECHA, "canon_electromagnetico");
            Montaje m = viejo.montaje();
            falso(m.puedePelear(), "con ese peso no puede salir a pelear");
            cierto(m.cifras().velocidad > 0 && !m.huecosQueMasPesan().isEmpty(), "se carga y el Taller puede proponer qué quitar");
            Pelea p = new Pelea(new Combatiente("v", m, Quieto::new), new Combatiente("q", new Montaje(Modelo.de("vigia")), Quieto::new), 3L);
            p.jugarEntera();
            igual(Pelea.Estado.TERMINADA, p.estado(), "aun así la simulación no se rompe");
            for (Modelo mo : Modelo.todos()) {
                for (double nivel : new double[]{0, 0.5, 1}) {
                    Combatiente c = Rivales.crear(mo, nivel, 0.5, new Random(1));
                    cierto(c.montaje.puedePelear(), mo.id + " de fábrica (nivel " + nivel + ") puede pelear: " + c.montaje.proporcionDeCarga());
                    cierto(c.montaje.tramo() != Equilibrio.Tramo.SOBRECARGA, mo.id + " de fábrica no va sobrecargado");
                }
            }
            for (int s = 0; s < 120; s++) {
                Combatiente c = Rivales.alAzar(s % 2 == 0 ? 0.3 : 1, 0.5, new Random(s));
                cierto(c.montaje.puedePelear(), "rival al azar " + s + " puede pelear: " + c.montaje.proporcionDeCarga());
            }
        });
    }

    // ---------------------------------------------------------------- la API en ES, DE y EN

    /** Lee su peso, su movilidad y las piernas del rival (español). */
    public static class LeeEs extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (turnoActual() == 0) {
                memoria().put("datos", String.format(Locale.ROOT, "%.3f %.3f %.3f %b %.3f %.3f %.3f %s %s", pesoTotal(), cargaMaxima(),
                        proporcionDeCarga(), sobrecargado(), velocidadMaxima(), giroMaximo(), aceleracionMaxima(), locomocion(), terreno()));
            }
        }

        @Override
        public void alVerEnemigo(Enemigo e) {
            memoria().put("rival", e.locomocion());
        }
    }

    /** Lo mismo con la fachada alemana. */
    public static class LeeDe extends MetajavbotDe {
        @Override
        public void jedenZug() {
            if (zug() == 0) {
                gedaechtnis().put("datos", String.format(Locale.ROOT, "%.3f %.3f %.3f %b %.3f %.3f %.3f %s %s", gesamtgewicht(), traglast(),
                        lastanteil(), ueberladen(), hoechstgeschwindigkeit(), maxDrehung(), maxBeschleunigung(), fortbewegung(), gelaende()));
            }
        }

        @Override
        public void beiSicht(Gegner g) {
            gedaechtnis().put("rival", g.fortbewegung());
        }
    }

    /** Lo mismo con la fachada inglesa. */
    public static class LeeEn extends MetajavbotEn {
        @Override
        public void onTick() {
            if (tick() == 0) {
                memory().put("datos", String.format(Locale.ROOT, "%.3f %.3f %.3f %b %.3f %.3f %.3f %s %s", totalWeight(), loadLimit(),
                        loadRatio(), overloaded(), topSpeed(), maxTurnRate(), maxAcceleration(), locomotion(), terrain()));
            }
        }

        @Override
        public void onSeeEnemy(Enemy e) {
            memory().put("rival", e.locomotion());
        }
    }

    private static void apuntarApi() {
        caso("equilibrio: la API dice peso, carga, movilidad, terreno y piernas del rival igual en ES, DE y EN", () -> {
            String[] datos = new String[3];
            String[] rival = new String[3];
            java.util.function.Supplier<?>[] robots = {LeeEs::new, LeeDe::new, LeeEn::new};
            for (int k = 0; k < 3; k++) {
                java.util.function.Supplier<?> s = robots[k];
                Montaje m = conPiernas("vigia", "piernas_bipedo");
                Combatiente c = new Combatiente("api", m, () -> (metajavbots.api.RobotBase) s.get());
                Pelea p = new Pelea(c, new Combatiente("r", conPiernas("vigia", "orugas"), Quieto::new), 5L, 1000, 700, 60);
                p.arena = Terrenos.arenaDePelea("HANGAR", p.ancho, p.alto);
                p.empezarRonda();
                colocar(p.cuerpo(0), 400, 350, 0);
                colocar(p.cuerpo(1), 480, 350, 180);
                p.avanzar();
                p.avanzar();
                datos[k] = String.valueOf(c.memoria.get("datos"));
                rival[k] = String.valueOf(c.memoria.get("rival"));
                Montaje.Cifras cf = m.cifras();
                cierto(datos[k].startsWith(String.format(Locale.ROOT, "%.3f %.3f %.3f false", cf.pesoTotal, cf.carga, cf.proporcion)),
                        "datos de peso correctos: " + datos[k]);
                cierto(datos[k].endsWith("BIPEDO LLANO"), "locomoción y terreno: " + datos[k]);
            }
            igual(datos[0], datos[1], "DE dice lo mismo que ES");
            igual(datos[0], datos[2], "EN dice lo mismo que ES");
            for (String r : rival) igual("ORUGAS", r, "se ven las piernas del rival");
        });
    }
}
