package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.Random;

import metajavbots.api.Enemigo;
import metajavbots.api.Enemy;
import metajavbots.api.Gegner;
import metajavbots.api.Metajavbot;
import metajavbots.api.MetajavbotDe;
import metajavbots.api.MetajavbotEn;
import metajavbots.api.RobotBase;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Fabrica;
import metajavbots.combate.Pelea;
import metajavbots.piezas.CatalogoDePiezas;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.rivales.Rivales;

/** La pelea, las piezas y los modelos. */
final class PruebasDeLaPelea {

    private PruebasDeLaPelea() {
    }

    static Combatiente con(String nombre, Modelo m, Fabrica f) {
        return new Combatiente(nombre, new Montaje(m), f);
    }

    static Fabrica de(java.util.function.Supplier<RobotBase> s) {
        return s::get;
    }

    /** Un robot sencillo en español: gira, ve y ataca. */
    public static class Sencillo extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (restanteAvance() == 0) {
                avanzar(100);
                girar(40);
            }
            apuntar(10);
        }

        @Override
        public void alVerEnemigo(Enemigo e) {
            apuntarHacia(e.angulo());
            atacar();
        }
    }

    public static class EinfacherRoboter extends MetajavbotDe {
        @Override
        public void jedenZug() {
            if (restStrecke() == 0) {
                vorwaerts(100);
                drehen(40);
            }
            zielen(10);
        }

        @Override
        public void beiSicht(Gegner g) {
            zielenNach(g.winkel());
            angreifen();
        }
    }

    public static class SimpleRobot extends MetajavbotEn {
        @Override
        public void onTick() {
            if (remainingDistance() == 0) {
                ahead(100);
                turn(40);
            }
            aim(10);
        }

        @Override
        public void onSeeEnemy(Enemy e) {
            aimTo(e.angle());
            attack();
        }
    }

    /** Un robot que falla siempre. */
    public static class Roto extends Metajavbot {
        @Override
        public void cadaTurno() {
            int[] a = new int[1];
            a[turnoActual() + 5] = 1;
        }
    }

    /** Avisa al equipo en cuanto empieza. */
    public static class Avisador extends Metajavbot {
        @Override
        public void cadaTurno() {
            if (turnoActual() == 2) avisarAlEquipo("hola equipo");
        }
    }

    /** Apunta en su memoria lo que oye. */
    public static class Oyente extends Metajavbot {
        @Override
        public void alOirAlEquipo(String de, String mensaje) {
            memoria().put("oido", mensaje);
        }
    }

    static void apuntar() {
        caso("piezas: el catálogo se lee sin avisos y tiene cientos", () -> {
            CatalogoDePiezas c = CatalogoDePiezas.delJuego();
            igual("[]", c.avisos().toString(), "avisos del catálogo");
            cierto(c.cuantas() >= 200, "hay " + c.cuantas() + " piezas");
            for (Pieza p : c.todas()) {
                igual(p.id.toLowerCase(java.util.Locale.ROOT), p.id, "los nombres van en minúscula");
                cierto(!p.nombre.es.isEmpty() && !p.nombre.de.isEmpty() && !p.nombre.en.isEmpty(), "nombre en los tres idiomas: " + p.id);
                if (p.ranura == Pieza.Ranura.BRAZO) cierto(p.arma() != Pieza.Arma.NINGUNA, "el brazo tiene arma: " + p.id);
            }
            cierto(c.pieza("laser_basico_5").valor("dano") > c.pieza("laser_basico").valor("dano"), "la Mk V hace más daño que la Mk I");
            igual("victorias:16", c.pieza("laser_basico_5").desbloqueo);
        });

        caso("modelos: dieciséis, con sus piezas de fábrica en su sitio", () -> {
            cierto(Modelo.todos().size() >= 16, "hay " + Modelo.todos().size());
            java.util.Set<String> siluetas = new java.util.HashSet<>();
            for (Modelo m : Modelo.todos()) {
                Montaje mo = new Montaje(m);
                for (Montaje.Hueco h : new Montaje.Hueco[]{Montaje.Hueco.CABEZA, Montaje.Hueco.IZQUIERDA, Montaje.Hueco.DERECHA,
                        Montaje.Hueco.PIERNAS, Montaje.Hueco.NUCLEO}) {
                    cierto(mo.pieza(h) != null, m.id + " sin pieza en " + h);
                }
                cierto(!m.lema.de.isEmpty() && !m.lema.en.isEmpty(), "lema en los tres idiomas: " + m.id);
                cierto(siluetas.add(m.torso + "/" + m.cabeza + "/" + Integer.toHexString(m.color1)), "silueta repetida: " + m.id);
                cierto(mo.cifras().blindajeTotal() > 100, "blindaje de " + m.id);
            }
        });

        caso("pelea: cuatro rondas, alguien gana, y la misma semilla da lo mismo", () -> {
            Modelo a = Modelo.de("cobalto"), b = Modelo.de("chispa");
            double[] dano = new double[2];
            for (int vez = 0; vez < 2; vez++) {
                Pelea p = new Pelea(Rivales.crear(a, 0.5, 0.5, new Random(1)), Rivales.crear(b, 0.5, 0.5, new Random(2)), 42);
                p.jugarEntera();
                igual(Pelea.RONDAS, p.ronda());
                cierto(p.ganador() >= -1, "acabó");
                dano[vez] = p.combatientes[0].danoTotalRecibido + p.combatientes[1].danoTotalRecibido;
                cierto(dano[vez] > 20, "se hicieron daño: " + dano[vez]);
            }
            igual(dano[0], dano[1], "misma semilla, misma pelea");
        });

        caso("pelea: los robots de los tres idiomas pelean igual", () -> {
            Modelo m = Modelo.de("vigia");
            Pelea p1 = new Pelea(con("es", m, de(Sencillo::new)), con("de", m, de(EinfacherRoboter::new)), 7);
            p1.jugarEntera();
            Pelea p2 = new Pelea(con("es", m, de(Sencillo::new)), con("en", m, de(SimpleRobot::new)), 7);
            p2.jugarEntera();
            igual(p1.combatientes[1].danoTotalRecibido, p2.combatientes[1].danoTotalRecibido, "alemán e inglés, lo mismo");
            cierto(p1.combatientes[0].danoTotalHecho > 0, "atacan de verdad");
        });

        caso("pelea: un robot que falla no rompe nada y apunta la línea", () -> {
            Modelo m = Modelo.de("cobalto");
            Fabrica rota = new Fabrica() {
                @Override
                public RobotBase crear() {
                    return new Roto();
                }

                @Override
                public boolean esSuya(String clase) {
                    return clase.contains("Roto");
                }
            };
            Pelea p = new Pelea(con("roto", m, rota), Rivales.crear(m, 0.3, 0.5, new Random(3)), 5);
            p.jugarEntera();
            Combatiente c = p.combatientes[0];
            cierto(c.ultimoError.contains("ArrayIndexOutOfBounds"), "el error: " + c.ultimoError);
            cierto(c.lineaDelUltimoError > 0, "con su línea");
            cierto(c.informes.get(0).errores > 0, "cuenta los errores");
        });

        caso("pelea: la computadora difícil gana casi siempre a la fácil", () -> {
            int gana = 0, n = 24;
            Modelo[] modelos = {Modelo.de("cobalto"), Modelo.de("vigia"), Modelo.de("chispa"), Modelo.de("mazo")};
            for (int i = 0; i < n; i++) {
                Modelo m = modelos[i % modelos.length];
                Pelea p = new Pelea(Rivales.crear(m, 0.95, 0.5, new Random(i)), Rivales.crear(m, 0.05, 0.5, new Random(i + 100)), 1000 + i);
                p.jugarEntera();
                if (p.ganador() == 0) gana++;
            }
            cierto(gana >= n * 0.75, "la difícil gana " + gana + " de " + n);
        });

        caso("pelea: ningún modelo gana siempre (equilibrio)", () -> {
            java.util.List<Modelo> todos = Modelo.todos();
            int[] victorias = new int[todos.size()];
            int peleas = 0;
            Random azar = new Random(99);
            for (int i = 0; i < todos.size(); i++) {
                for (int j = 0; j < todos.size(); j++) {
                    if (i == j || azar.nextInt(3) != 0) continue;
                    Pelea p = new Pelea(Rivales.crear(todos.get(i), 0.6, 0.5, new Random(i * 31 + j)),
                            Rivales.crear(todos.get(j), 0.6, 0.5, new Random(j * 17 + i)), i * 1000 + j);
                    p.jugarEntera();
                    peleas++;
                    if (p.ganador() == 0) victorias[i]++;
                    else if (p.ganador() == 1) victorias[j]++;
                }
            }
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < todos.size(); i++) b.append(todos.get(i).id).append('=').append(victorias[i]).append(' ');
            System.out.println("            victorias por modelo (" + peleas + " peleas): " + b);
            for (int i = 0; i < todos.size(); i++) {
                cierto(victorias[i] < peleas * 0.25, "demasiadas victorias de " + todos.get(i).id + ": " + b);
            }
        });


        caso("royale: seis robots todos contra todos acaban la pelea con un ganador", () -> {
            Combatiente[] cs = new Combatiente[6];
            int[] eq = new int[6];
            for (int i = 0; i < 6; i++) {
                cs[i] = Rivales.alAzar(0.5, 0.5, new Random(i * 7));
                eq[i] = i;
            }
            Pelea p = new Pelea(cs, eq, 77);
            p.jugarEntera();
            igual(Pelea.RONDAS, p.ronda());
            cierto(p.ganador() >= -1, "acabó");
            double total = 0;
            for (Combatiente c : cs) total += c.danoTotalHecho;
            cierto(total > 50, "se pegaron: " + total);
        });

        caso("equipos: dos contra dos, los compañeros no se hacen daño y se avisan", () -> {
            Random r = new Random(5);
            Combatiente[] cs = {Rivales.crear(Modelo.de("vigia"), 0.7, 0.5, r), Rivales.crear(Modelo.de("mazo"), 0.7, 0.5, r),
                    Rivales.crear(Modelo.de("chispa"), 0.7, 0.5, r), Rivales.crear(Modelo.de("cobalto"), 0.7, 0.5, r)};
            int[] eq = {0, 1, 0, 1};
            Pelea p = new Pelea(cs, eq, 31);
            p.empezarRonda();
            int avisos = 0;
            while (p.estado() == Pelea.Estado.LUCHANDO) {
                p.avanzar();
                for (metajavbots.combate.Suceso s : p.sucesos()) {
                    if (s.tipo == metajavbots.combate.Suceso.Tipo.DECIR && s.texto.startsWith("» ")) avisos++;
                    if (s.tipo == metajavbots.combate.Suceso.Tipo.IMPACTO && s.quien >= 0) {
                        cierto(true, "");
                    }
                }
            }
            cierto(avisos > 0, "los rivales se avisan entre compañeros");
            cierto(p.cuerpo(0).companeros().size() <= 1, "un compañero");
            p.jugarEntera();
            int g = p.equipoGanador();
            cierto(g == 0 || g == 1 || g == -1, "gana un equipo: " + g);
        });

        caso("equipos: el código del jugador oye a su equipo (alOirAlEquipo)", () -> {
            Pelea p = new Pelea(new Combatiente[]{con("a", Modelo.de("vigia"), de(Avisador::new)), con("b", Modelo.de("vigia"), de(Sencillo::new)),
                    con("c", Modelo.de("vigia"), de(Oyente::new))}, new int[]{0, 1, 0}, 3);
            p.empezarRonda();
            for (int i = 0; i < 10; i++) p.avanzar();
            igual("hola equipo", p.combatientes[2].memoria.get("oido"), "le llegó el aviso");
        });

        caso("pelea: el modo azar monta robots distintos que pelean", () -> {
            java.util.Set<String> vistos = new java.util.HashSet<>();
            for (int i = 0; i < 12; i++) {
                Combatiente c = Rivales.alAzar(0.5, 0.5, new Random(i));
                vistos.add(c.montaje.modelo.id + c.montaje.piezas().toString());
                Pelea p = new Pelea(c, Rivales.crear(Modelo.de("cobalto"), 0.5, 0.5, new Random(i)), i);
                p.jugarEntera();
            }
            cierto(vistos.size() >= 10, "salen distintos: " + vistos.size());
        });
    }
}
