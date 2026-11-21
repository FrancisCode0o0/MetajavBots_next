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
    private static final class Quieto extends Metajavbot {
        @Override
        public void cadaTurno() {
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
    /** Registra el valor de turnoActual() en alEmpezar() y en cadaTurno(). */
    public static class Cronometrador extends Metajavbot {
        @Override
        public void alEmpezar() {
            memoria().put("inicio", turnoActual());
            memoria().put("turnos", new java.util.ArrayList<Integer>());
            memoria().put("periodicos", new java.util.ArrayList<Integer>());
        }

        @Override
        @SuppressWarnings("unchecked")
        public void cadaTurno() {
            int t = turnoActual();
            ((java.util.List<Integer>) memoria().get("turnos")).add(t);
            if (t % 45 == 0) ((java.util.List<Integer>) memoria().get("periodicos")).add(t);
        }
    }
    /** Recorre enemigosVistos() con un bucle for-each para apuntar al enemigo más cercano. */
    public static class CazadorConBucle extends Metajavbot {
        @Override
        public void cadaTurno() {
            Enemigo mejor = null;
            for (Enemigo e : enemigosVistos()) {
                if (mejor == null || e.distancia() < mejor.distancia()) mejor = e;
            }
            int vistos = enemigosVistos().size();
            int max = (int) memoria().getOrDefault("maxVistos", 0);
            if (vistos > max) memoria().put("maxVistos", vistos);
            if (mejor != null) {
                apuntarHacia(mejor.angulo());
                if (mejor.distancia() > 180) {
                    girarHacia(mejor.angulo());
                    avanzar(40);
                }
                atacar();
            } else {
                apuntar(12);
                if (restanteAvance() == 0) avanzar(60);
            }
        }
    }
    /** Verifica geseheneGegner() en alemán. */
    public static class JaegerMitSchleife extends MetajavbotDe {
        @Override
        public void jedenZug() {
            int max = (int) gedaechtnis().getOrDefault("maxVistos", 0);
            int vistos = geseheneGegner().size();
            if (vistos > max) gedaechtnis().put("maxVistos", vistos);
            Gegner ziel = null;
            for (Gegner g : geseheneGegner()) {
                if (ziel == null || g.entfernung() < ziel.entfernung()) ziel = g;
            }
            if (ziel != null) {
                zielenNach(ziel.winkel());
                angreifen();
            } else {
                zielen(12);
            }
        }
    }
    /** Verifica seenEnemies() en inglés. */
    public static class LoopHunterEn extends MetajavbotEn {
        @Override
        public void onTick() {
            int max = (int) memory().getOrDefault("maxVistos", 0);
            int vistos = seenEnemies().size();
            if (vistos > max) memory().put("maxVistos", vistos);
            Enemy target = null;
            for (Enemy e : seenEnemies()) {
                if (target == null || e.distance() < target.distance()) target = e;
            }
            if (target != null) {
                aimTo(target.angle());
                attack();
            } else {
                aim(12);
            }
        }
    }
    static void apuntar() {
        apuntarParte1();
        apuntarParte2();
        apuntarParte3();
        apuntarParte4();
        apuntarArenas();
        apuntarAparatos();
        apuntarRondas();
        apuntarFrenoAvance();
        apuntarTurnoBaseCero();
        apuntarFrecuenciaMultiples();
        apuntarSimulacionMultiples();
        apuntarAvisoMultiplesObjetivos();
    }
    private static void apuntarFrenoAvance() {
        caso("avance positivo en hielo rebasa el objetivo sin quedar negativo ni invertir marcha", () -> {
            Modelo modelo = Modelo.de("cobalto");
            Pelea p = new Pelea(con("quieto", modelo, de(Quieto::new)),
                    con("rivales", modelo, de(Quieto::new)), 19, 1000, 700, 120);
            p.arena = metajavbots.combate.Arena.de("HIELO", p.ancho, p.alto);
            p.empezarRonda();
            metajavbots.combate.Cuerpo c = p.cuerpo(0);
            metajavbots.combate.Cuerpo rival = p.cuerpo(1);
            c.x = 500;
            c.y = 350;
            rival.x = 100;
            rival.y = 100;
            c.velocidad = 12;
            c.avanzar(3);
            p.avanzar();
            igual(0.0, c.restanteAvance(), "al rebasar el objetivo no queda avance negativo");
            cierto(c.velocidad() >= 0, "la marcha adelante no se invierte al rebasar");
            for (int i = 0; i < 80 && c.velocidad() != 0; i++) p.avanzar();
            igual(0.0, c.velocidad(), "el robot termina detenido");
        });

        caso("avance negativo en hielo rebasa el objetivo sin quedar positivo ni invertir marcha", () -> {
            Modelo modelo = Modelo.de("cobalto");
            Pelea p = new Pelea(con("quieto", modelo, de(Quieto::new)),
                    con("rivales", modelo, de(Quieto::new)), 23, 1000, 700, 120);
            p.arena = metajavbots.combate.Arena.de("HIELO", p.ancho, p.alto);
            p.empezarRonda();
            metajavbots.combate.Cuerpo c = p.cuerpo(0);
            metajavbots.combate.Cuerpo rival = p.cuerpo(1);
            c.x = 500;
            c.y = 350;
            rival.x = 100;
            rival.y = 100;
            c.velocidad = -12;
            c.avanzar(-3);
            p.avanzar();
            igual(0.0, c.restanteAvance(), "al rebasar atrás el objetivo no queda avance positivo");
            cierto(c.velocidad() <= 0, "la marcha atrás no se invierte al rebasar");
            for (int i = 0; i < 80 && c.velocidad() != 0; i++) p.avanzar();
            igual(0.0, c.velocidad(), "el robot termina detenido");
        });
    }
    private static void apuntarRondas() {
        caso("rondas como en el boxeo: con 4 o menos cada ronda empieza entera; con 12, el daño se arrastra (nunca por debajo de la mitad)", () -> {
            java.util.List<Modelo> ms = Modelo.todos();
            for (int rondas : new int[]{4, 12}) {
                Pelea p = new Pelea(Rivales.crear(ms.get(3), 0.8, 0.9, new Random(4)), Rivales.crear(ms.get(6), 0.8, 0.9, new Random(5)), 21);
                p.rondas = rondas;
                p.empezarRonda();
                while (p.estado() == Pelea.Estado.LUCHANDO) p.avanzar();
                if (p.estado() != Pelea.Estado.ENTRE_RONDAS) continue;
                p.empezarRonda();
                for (int i = 0; i < 2; i++) {
                    metajavbots.combate.Cuerpo c = p.cuerpo(i);
                    double total = c.blindaje(), dano = p.combatientes[i].danoTotalRecibido;
                    if (rondas == 4 || dano < 1) cierto(total > 99.9, "entera con " + rondas + " rondas: " + total);
                    else cierto(total < 99.9 && total >= 49.9, "con 12 rondas arrastra el daño (" + total + ")");
                }
            }
        });
    }
    @SuppressWarnings("unchecked")
    private static void apuntarTurnoBaseCero() {
        caso("turnoActual(): base 0 real en alEmpezar(), cadaTurno(), módulo 45, RONDA_EMPIEZA y duración de ronda", () -> {
            Modelo m = Modelo.de("cobalto");
            Pelea p = new Pelea(con("a", m, de(Cronometrador::new)), con("b", m, de(Cronometrador::new)), 19);
            p.empezarRonda();
            igual(0, p.combatientes[0].memoria.get("inicio"), "alEmpezar() observa turnoActual() == 0");
            igual(0, p.turno(), "tras empezarRonda() el contador es 0");
            cierto(p.sucesos().stream().anyMatch(s -> s.tipo == metajavbots.combate.Suceso.Tipo.RONDA_EMPIEZA),
                    "RONDA_EMPIEZA presente tras empezarRonda()");
            p.avanzar();
            java.util.List<Integer> turnos = (java.util.List<Integer>) p.combatientes[0].memoria.get("turnos");
            java.util.List<Integer> periodicos = (java.util.List<Integer>) p.combatientes[0].memoria.get("periodicos");
            igual(1, turnos.size(), "tras 1 avanzar() hubo 1 cadaTurno()");
            igual(0, turnos.get(0), "el primer cadaTurno() observa 0");
            igual(1, periodicos.size(), "turnoActual() % 45 == 0 se activa en el primer turno");
            igual(0, periodicos.get(0), "primer múltiplo periódico observado es 0");
            cierto(p.sucesos().stream().anyMatch(s -> s.tipo == metajavbots.combate.Suceso.Tipo.RONDA_EMPIEZA),
                    "RONDA_EMPIEZA sigue presente en el primer tick");
            p.avanzar();
            igual(2, turnos.size(), "tras 2 avanzar() hubo 2 cadaTurno()");
            igual(1, turnos.get(1), "el segundo cadaTurno() observa 1");
            igual(1, periodicos.size(), "en el turno 1 no se activa % 45");
            cierto(p.sucesos().stream().noneMatch(s -> s.tipo == metajavbots.combate.Suceso.Tipo.RONDA_EMPIEZA),
                    "RONDA_EMPIEZA se limpia en el segundo tick");
            while (p.estado() == Pelea.Estado.LUCHANDO) p.avanzar();
            igual(p.turnosPorRonda, turnos.size(), "una ronda completa ejecuta exactamente turnosPorRonda ticks");
            igual(p.turnosPorRonda - 1, turnos.get(turnos.size() - 1), "el último tick observado es turnosPorRonda - 1");
            igual(p.turnosPorRonda, p.turno(), "al acabar la ronda p.turno() == turnosPorRonda");
            igual(p.turnosPorRonda, p.informe(0).turnos, "el informe registra exactamente turnosPorRonda turnos");
        });
    }
    private static void apuntarAparatos() {
        caso("aparatos: sale a los 3 s, se coge al tocarlo, solo engancha en su hueco (en cualquier idioma), tarda en montarse, se usa y se recarga", () -> {
            java.util.List<Modelo> ms = Modelo.todos();
            Pelea p = new Pelea(Rivales.crear(ms.get(0), 0.5, 0.5, new Random(1)), Rivales.crear(ms.get(1), 0.5, 0.5, new Random(2)), 11);
            p.aparatos.activos = true;
            cierto(p.aparatos.siguiente() != null, "se sabe el de la primera ronda");
            p.empezarRonda();
            while (p.turno() < metajavbots.combate.Aparatos.SALE) p.avanzar();
            igual(1, p.aparatos.todos().size(), "ha salido");
            metajavbots.combate.Aparatos.Uno a = p.aparatos.todos().get(0);
            if (a.dueno < 0) {
                metajavbots.combate.Cuerpo c = p.cuerpo(0);
                c.x = a.x;
                c.y = a.y;
                p.avanzar();
            }
            cierto(a.dueno >= 0, "lo ha cogido alguien");
            metajavbots.combate.Cuerpo c = p.cuerpo(a.dueno);
            String otro = a.tipo.hueco.equals("cabeza") ? "piernas" : "cabeza";
            cierto(!c.conectar(otro), "en otro hueco no engancha");
            String aleman = java.util.Map.of("espalda", "Ruecken", "piernas", "beine", "cabeza", "kopf", "brazo", "arm").get(a.tipo.hueco);
            cierto(c.conectar(aleman), "en el suyo, dicho en alemán, sí");
            cierto(!c.miAparato().conectado(), "aún se está montando");
            for (int k = 0; k < metajavbots.combate.Aparatos.MONTAR; k++) p.avanzar();
            cierto(c.miAparato().conectado() && c.miAparato().listo(), "montado y listo");
            cierto(c.usarAparato(true, 0), "se usa");
            cierto(!c.usarAparato(true, 0), "y hay que esperar a que se recargue");
            // Una pelea entera con aparatos: la computadora los coge, los conecta y los usa sin que nada falle.
            for (int k = 0; k < 4; k++) {
                Pelea q = new Pelea(Rivales.crear(ms.get(k), 0.6, 0.7, new Random(k)), Rivales.crear(ms.get(k + 5), 0.6, 0.7, new Random(k + 3)), 50 + k);
                q.aparatos.activos = true;
                q.arena = metajavbots.combate.Arena.de(k % 2 == 0 ? "HIELO" : "HANGAR", q.ancho, q.alto);
                q.jugarEntera();
                cierto(q.estado() == Pelea.Estado.TERMINADA, "acaba");
            }
        });
    }
    private static void apuntarArenas() {
        caso("arenas con trampas: se pelea entero en cada una, las trampas hacen daño y la API las ve; las de siempre no tienen nada", () -> {
            igual(0, metajavbots.combate.Arena.de("HANGAR", 1000, 700).trampas().size(), "las de siempre, sin trampas");
            int trampas = 0;
            java.util.List<Modelo> ms = Modelo.todos();
            for (String a : new String[]{"CHATARRERIA", "COCINA", "HIELO", "TEJADO"}) {
                for (int k = 0; k < 3; k++) {
                    Pelea p = new Pelea(Rivales.crear(ms.get(k * 5 % ms.size()), 0.5, 0.8, new Random(k)), Rivales.crear(ms.get((k * 7 + 3) % ms.size()), 0.5,
                            0.8, new Random(k + 9)), 100 + k);
                    p.arena = metajavbots.combate.Arena.de(a, p.ancho, p.alto);
                    cierto(!p.arena.trampas().isEmpty(), a + " tiene trampas");
                    p.jugarEntera();
                    cierto(p.estado() == Pelea.Estado.TERMINADA, a + " acaba");
                    for (metajavbots.combate.Suceso su : p.historia()) if (su.tipo == metajavbots.combate.Suceso.Tipo.TRAMPA) trampas++;
                }
            }
            cierto(trampas > 0, "alguna trampa hizo daño");
            Pelea p = new Pelea(Rivales.crear(ms.get(0), 0.5, 0.5, new Random(1)), Rivales.crear(ms.get(1), 0.5, 0.5, new Random(2)), 7);
            p.arena = metajavbots.combate.Arena.de("CHATARRERIA", p.ancho, p.alto);
            p.empezarRonda();
            igual(p.arena.trampas().size(), p.cuerpo(0).obstaculos().size(), "obstaculos() las ve todas");
        });
        caso("volteado: vuela sobre pegamento y foso y vuelve a pisar al terminar", () -> {
            Modelo m = Modelo.de("cobalto");
            Pelea p = new Pelea(con("quieto", m, de(Quieto::new)), con("rivales", m, de(Quieto::new)), 31);
            p.arena = metajavbots.combate.Arena.de("HANGAR", p.ancho, p.alto);
            p.empezarRonda();
            metajavbots.combate.Cuerpo c = p.cuerpo(0);
            try {
                java.lang.reflect.Method ch = metajavbots.combate.Arena.class.getDeclaredMethod("charco",
                        String.class, double.class, double.class, double.class, int.class);
                ch.setAccessible(true);
                ch.invoke(p.arena, "pegamento", 500.0, 350.0, 80.0, 240);
            } catch (ReflectiveOperationException e) { throw new AssertionError("sin pegamento", e); }
            c.x = 500; c.y = 350; c.volteado = 24; c.velocidad = 10;
            cierto(c.vuela(), "volteado reporta vuela()");
            p.avanzar();
            cierto(c.velocidad > 8, "pegamento no reduce mientras vuela");
            c.volteado = 0;
            cierto(!c.vuela(), "sin volteado vuelve a pisar");
            c.velocidad = 10;
            p.avanzar();
            cierto(c.velocidad < 5, "pegamento reduce al terrestre");
            Pelea f = new Pelea(con("quieto", m, de(Quieto::new)), con("rivales", m, de(Quieto::new)), 37);
            f.arena = metajavbots.combate.Arena.de("HIELO", f.ancho, f.alto);
            f.empezarRonda();
            c = f.cuerpo(0); c.x = 500; c.y = 350; c.volteado = 24;
            cierto(c.vuela(), "volteado reporta vuela() ante el foso");
            f.avanzar();
            igual(500.0, c.x, "no sale del foso mientras vuela");
            igual(350.0, c.y, "no cae en el foso mientras vuela");
            c.volteado = 0;
            cierto(!c.vuela(), "agotado el volteado vuelve a ser terrestre");
            f.avanzar();
            cierto(c.aturdido > 0 || c.x != 500 || c.y != 350, "vuelve a caer en el foso");
        });
    }
    private static void apuntarParte1() {
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
            Pelea p1 = new Pelea(con("es", m, de(Sencillo::new)), con("de", m, de(EinfacherRoboter::new)), 1);
            p1.jugarEntera();
            Pelea p2 = new Pelea(con("es", m, de(Sencillo::new)), con("en", m, de(SimpleRobot::new)), 1);
            p2.jugarEntera();
            igual(p1.combatientes[1].danoTotalRecibido, p2.combatientes[1].danoTotalRecibido, "alemán e inglés, lo mismo");
            cierto(p1.combatientes[0].danoTotalHecho > 0, "atacan de verdad");
        });
    }
    private static void apuntarParte2() {
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
    }
    private static void apuntarParte3() {
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

        caso("piezas: las armas de las luchas de verdad (sierra, pinza, volteador, cuña, bola con cadena y mezclas) existen y empujan", () -> {
            metajavbots.piezas.CatalogoDePiezas cat = metajavbots.piezas.CatalogoDePiezas.delJuego();
            for (String id : new String[]{"sierra", "sierra_3", "pinza", "volteador", "cuna", "bola_cadena", "sierra_volteadora", "pinza_taladro"}) {
                metajavbots.piezas.Pieza p = cat.pieza(id);
                cierto(p != null, "existe " + id);
                igual(metajavbots.piezas.Pieza.Arma.CUERPO, p.arma());
                cierto(!p.nombre(metajavbots.textos.Idioma.DE).isEmpty() && !p.nombre(metajavbots.textos.Idioma.EN).isEmpty(), "tres idiomas");
            }
            cierto(cat.pieza("volteador").valor("volteo") > 0 && cat.pieza("cuna").valor("empuje", 10) > 10, "el volteador voltea y la cuña empuja");
            cierto(cat.pieza("bola_cadena").valor("alcance") > 150, "la bola llega lejos");
        });

        caso("pelea: el láser se para en la chapa del primer robot que cruza y el puñetazo va al que está delante", () -> {
            Random r = new Random(3);
            Combatiente[] cs = {Rivales.crear(Modelo.de("cobalto"), 0.5, 0.5, r), Rivales.crear(Modelo.de("mazo"), 0.5, 0.5, r),
                    Rivales.crear(Modelo.de("chispa"), 0.5, 0.5, r)};
            Pelea p = new Pelea(cs, new int[]{0, 1, 2}, 9);
            p.empezarRonda();
            metajavbots.combate.Cuerpo a = p.cuerpo(0), b = p.cuerpo(1), c = p.cuerpo(2);
            a.x = 100;
            a.y = 300;
            a.mira = 0;
            b.x = 300;
            b.y = 310;
            c.x = 500;
            c.y = 300;
            double[] fin = new double[1];
            cierto(p.primeroEnLaLinea(a, a.x, a.y, 1, 0, 900, fin) == b, "el del medio tapa al de detrás");
            double esperado = 200 - Math.sqrt(b.radio * b.radio - 100);
            cierto(Math.abs(fin[0] - esperado) < 1e-6, "en su chapa: " + fin[0] + " / " + esperado);
            b.y = 300 + b.radio + 5;
            cierto(p.primeroEnLaLinea(a, a.x, a.y, 1, 0, 900, fin) == c, "si el del medio no está en la línea, el rayo sigue");
            cierto(p.primeroEnLaLinea(a, a.x, a.y, 1, 0, 300, fin) == null && fin[0] == 300, "el alcance manda");
            b.x = a.x + a.radio + b.radio + 20;
            b.y = 300;
            c.x = a.x - a.radio - c.radio - 10;
            cierto(p.alAlcance(a, 50, 70) == b, "el de delante, no el de detrás");
            a.mira = 180;
            cierto(p.alAlcance(a, 50, 70) == c, "al girarse, el otro");
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
    }
    private static void apuntarParte4() {
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

        caso("visor táctico: lee radar, alcances, ángulo relativo, obstáculos y encaja etiquetas con letra 26", () -> {
            Pelea p = new Pelea(con("yo", Modelo.de("cobalto"), de(Sencillo::new)), con("el", Modelo.de("mazo"), de(Sencillo::new)), 11);
            p.arena = metajavbots.combate.Arena.de("CHATARRERIA", p.ancho, p.alto);
            p.empezarRonda();
            metajavbots.combate.Cuerpo yo = p.cuerpo(0), el = p.cuerpo(1);
            yo.x = 260;
            yo.y = 320;
            yo.mira = 15;
            el.x = 460;
            el.y = 390;
            p.avanzar();
            metajavbots.pantalla.VisorTactico.Datos d = metajavbots.pantalla.VisorTactico.leer(p, 1);
            cierto(d != null, "datos del visor");
            igual(yo.alcance(metajavbots.combate.Cuerpo.IZQUIERDA), d.alcanceIzq, "alcance izquierda");
            igual(yo.alcance(metajavbots.combate.Cuerpo.DERECHA), d.alcanceDer, "alcance derecha");
            cierto(d.radioRadar > 100 && d.conoRadar > 0, "cono y radio del radar");
            cierto(d.objetivo != null && d.objetivo.visible, "objetivo visible en el radar");
            metajavbots.api.Avistado visto = yo.vistos().get(0);
            cierto(Math.abs(d.objetivo.distancia - visto.distancia()) < 1e-6, "distancia coherente con e.distancia()");
            cierto(Math.abs(d.objetivo.anguloAbs - visto.angulo()) < 1e-6, "ángulo coherente con e.angulo()");
            cierto(Math.abs(d.objetivo.anguloRel - Pelea.normal(visto.angulo() - yo.mira())) < 1e-6, "ángulo relativo coherente");
            cierto(!d.obstaculos.isEmpty(), "lee los obstáculos de la arena");
            boolean hayPeligro = false;
            for (metajavbots.api.Cosa o : d.obstaculos) if (o.peligroso()) hayPeligro = true;
            cierto(hayPeligro, "detecta obstáculos peligrosos");
            // Con cabeza rota, el radio del radar se reduce al 40 % (igual que en Pelea.vistosPor).
            double radioSano = d.radioRadar;
            yo.parte[metajavbots.piezas.Montaje.CABEZA] = 0;
            cierto(Math.abs(metajavbots.pantalla.VisorTactico.leer(p, 1).radioRadar - radioSano * 0.4) < 1e-6, "cabeza rota reduce radar");
            // Las etiquetas del visor respetan techo y barra inferior incluso con -Pletra=26.
            float tl26 = metajavbots.pantalla.VisorTactico.tamanoLetra(26f);
            cierto(tl26 >= metajavbots.pantalla.Pincel.LETRA_MINIMA && tl26 <= 19f, "letra acotada en -Pletra=26: " + tl26);
            float[] pos = metajavbots.pantalla.VisorTactico.encajarEtiqueta(2000, 900, 120, 26, 0, 88, 1600, 812, 172);
            cierto(pos[0] + 120 <= 1590 && pos[1] >= 176 && pos[1] + 26 <= 816, "etiqueta dentro del área segura");
            for (metajavbots.textos.Idioma id : metajavbots.textos.Idioma.values()) {
                metajavbots.textos.T.idioma = id;
                for (String k : new String[]{"pelea.visor", "pelea.visor_ayuda", "visor.izq", "visor.der"}) {
                    cierto(!metajavbots.textos.T.t(k).equals(k) && !metajavbots.textos.T.t(k).isEmpty(), "clave " + k + " en " + id);
                }
            }
            metajavbots.textos.T.idioma = metajavbots.textos.Idioma.ES;
        });
    }
    private static void apuntarFrecuenciaMultiples() {
        caso("múltiples objetivos: NOOB_1..NOOB_3 nunca los generan y la frecuencia escala en NOOB_4, JUNIOR y SENIOR/MASTER", () -> {
            metajavbots.progreso.Etiqueta[] sinMulti = {
                metajavbots.progreso.Etiqueta.NOOB_1, metajavbots.progreso.Etiqueta.NOOB_2, metajavbots.progreso.Etiqueta.NOOB_3
            };
            for (metajavbots.progreso.Etiqueta e : sinMulti) {
                igual(0.0, metajavbots.progreso.Progreso.probabilidadMultiplesObjetivos(e), "probabilidad 0 en " + e);
                metajavbots.progreso.Jugador j = new metajavbots.progreso.Jugador("n");
                j.etiqueta = e;
                Random r = new Random(123);
                for (int k = 0; k < 200; k++) {
                    metajavbots.pantallas.Encuentro enc = metajavbots.pantallas.PrepararPelea.encuentro(j, "ADAPTADA", r);
                    cierto(enc.demas.isEmpty(), e + " debe mantener 1v1 puro");
                }
            }
            int muestras = 800;
            double fNoob4 = frecuenciaMultiples(metajavbots.progreso.Etiqueta.NOOB_4, muestras, 11L);
            double fJunior = frecuenciaMultiples(metajavbots.progreso.Etiqueta.JUNIOR_2, muestras, 22L);
            double fSenior = frecuenciaMultiples(metajavbots.progreso.Etiqueta.SENIOR_2, muestras, 33L);
            double fMaster = frecuenciaMultiples(metajavbots.progreso.Etiqueta.MASTER, muestras, 44L);
            cierto(fNoob4 > 0.15 && fNoob4 < 0.35, "NOOB_4 ~25 %: " + fNoob4);
            cierto(fJunior > 0.30 && fJunior < 0.50, "JUNIOR ~40 %: " + fJunior);
            cierto(fSenior > 0.45 && fSenior < 0.68, "SENIOR ~55 %: " + fSenior);
            cierto(fMaster > 0.45 && fMaster < 0.68, "MASTER ~55 %: " + fMaster);
            cierto(fNoob4 < fJunior && fJunior < fSenior, "escala NOOB_4 < JUNIOR < SENIOR: " + fNoob4 + " / " + fJunior + " / " + fSenior);
        });
    }
    private static void apuntarSimulacionMultiples() {
        caso("múltiples objetivos: pelea contra rival + drones y contra escuadrón ligero se simula limpia y llena enemigosVistos()", () -> {
            Random azar = new Random(7);
            java.util.List<Combatiente> enemigos = java.util.List.of(
                    Rivales.crear(Modelo.de("cobalto"), 0.4, 0.5, azar),
                    Rivales.dron(0.4, 0.5, azar, 1),
                    Rivales.dron(0.4, 0.5, azar, 2));
            Combatiente jugadorEs = con("yo", Modelo.de("vigia"), de(CazadorConBucle::new));
            Pelea p = Pelea.contraMultiples(jugadorEs, enemigos, 91L);
            cierto(p.tieneMultiplesObjetivos(), "detecta múltiples objetivos");
            igual(3, p.enemigosDe(0), "tres objetivos rivales");
            p.empezarRonda();
            // Al acercarlos dentro del radar, los tres aparecen a la vez en enemigosVistos() / geseheneGegner() / seenEnemies().
            metajavbots.combate.Cuerpo yo = p.cuerpo(0);
            yo.x = 200;
            yo.y = 350;
            yo.mira = 0;
            for (int i = 1; i <= 3; i++) {
                p.cuerpo(i).x = 420 + (i - 1) * 20;
                p.cuerpo(i).y = 320 + (i - 1) * 30;
            }
            p.avanzar();
            cierto(yo.vistos().size() >= 2, "enemigosVistos() devuelve varios objetivos a la vez: " + yo.vistos().size());
            p.jugarEntera();
            igual(Pelea.Estado.TERMINADA, p.estado(), "termina limpiamente");
            igual(Pelea.RONDAS, p.ronda(), "completa las rondas");
            cierto(p.ganador() >= -1, "ganador válido");
            cierto((int) jugadorEs.memoria.getOrDefault("maxVistos", 0) >= 2, "el bucle sobre enemigosVistos() vio varios objetivos");
            cierto(jugadorEs.danoTotalHecho > 0, "el jugador hace daño en pelea múltiple");

            // También en alemán (geseheneGegner) e inglés (seenEnemies) con encuentro generado en SENIOR.
            Combatiente jugDe = con("de", Modelo.de("vigia"), de(JaegerMitSchleife::new));
            Pelea pDe = Pelea.contraMultiples(jugDe, java.util.List.of(
                    Rivales.ligero(Modelo.de("chispa"), 0.5, 0.5, new Random(1), 2),
                    Rivales.ligero(Modelo.de("vigia"), 0.5, 0.5, new Random(2), 2)), 92L);
            pDe.empezarRonda();
            pDe.cuerpo(0).x = 200;
            pDe.cuerpo(0).y = 350;
            pDe.cuerpo(0).mira = 0;
            pDe.cuerpo(1).x = 400;
            pDe.cuerpo(1).y = 330;
            pDe.cuerpo(2).x = 420;
            pDe.cuerpo(2).y = 370;
            pDe.avanzar();
            cierto((int) jugDe.memoria.getOrDefault("maxVistos", 0) >= 2, "geseheneGegner() ve varios objetivos");
            pDe.jugarEntera();
            igual(Pelea.Estado.TERMINADA, pDe.estado(), "pelea DE termina limpiamente");

            Combatiente jugEn = con("en", Modelo.de("vigia"), de(LoopHunterEn::new));
            Pelea pEn = Pelea.contraMultiples(jugEn, java.util.List.of(
                    Rivales.ligero(Modelo.de("chispa"), 0.5, 0.5, new Random(3), 2),
                    Rivales.ligero(Modelo.de("cobalto"), 0.5, 0.5, new Random(4), 2)), 93L);
            pEn.empezarRonda();
            pEn.cuerpo(0).x = 200;
            pEn.cuerpo(0).y = 350;
            pEn.cuerpo(0).mira = 0;
            pEn.cuerpo(1).x = 400;
            pEn.cuerpo(1).y = 330;
            pEn.cuerpo(2).x = 420;
            pEn.cuerpo(2).y = 370;
            pEn.avanzar();
            cierto((int) jugEn.memoria.getOrDefault("maxVistos", 0) >= 2, "seenEnemies() ve varios objetivos");
            pEn.jugarEntera();
            igual(Pelea.Estado.TERMINADA, pEn.estado(), "pelea EN termina limpiamente");
        });
    }
    private static void apuntarAvisoMultiplesObjetivos() {
        caso("múltiples objetivos: aviso y pista en pseudocódigo (es, de, en) si no recorre enemigosVistos() con bucle", () -> {
            String sinBucle = "public class MiRobot extends Metajavbot { @Override public void alVerEnemigo(Enemigo e) { "
                    + "apuntarHacia(e.angulo()); atacar(); } }";
            String conBucleEs = "public class MiRobot extends Metajavbot { @Override public void cadaTurno() { "
                    + "for (Enemigo e : enemigosVistos()) { apuntarHacia(e.angulo()); } } }";
            String conBucleDe = "public class MeinRoboter extends MetajavbotDe { @Override public void jedenZug() { "
                    + "for (Gegner g : geseheneGegner()) { zielenNach(g.winkel()); } } }";
            String conBucleEn = "public class MyRobot extends MetajavbotEn { @Override public void onTick() { "
                    + "for (Enemy e : seenEnemies()) { aimTo(e.angle()); } } }";

            cierto(!metajavbots.tutor.ConsejosDelTaller.recorreEnemigosVistosConBucle(sinBucle), "sin bucle no pasa");
            cierto(metajavbots.tutor.ConsejosDelTaller.recorreEnemigosVistosConBucle(conBucleEs), "bucle en es");
            cierto(metajavbots.tutor.ConsejosDelTaller.recorreEnemigosVistosConBucle(conBucleDe), "bucle en de");
            cierto(metajavbots.tutor.ConsejosDelTaller.recorreEnemigosVistosConBucle(conBucleEn), "bucle en en");

            boolean tieneTipSinBucle = false;
            for (metajavbots.tutor.ConsejosDelTaller.Tip t : metajavbots.tutor.ConsejosDelTaller.de(sinBucle, true)) {
                if ("multiples_objetivos".equals(t.id())) tieneTipSinBucle = true;
            }
            cierto(tieneTipSinBucle, "ConsejosDelTaller avisa cuando hay múltiples objetivos sin bucle");
            boolean tieneTipConBucle = false;
            for (metajavbots.tutor.ConsejosDelTaller.Tip t : metajavbots.tutor.ConsejosDelTaller.de(conBucleEs, true)) {
                if ("multiples_objetivos".equals(t.id())) tieneTipConBucle = true;
            }
            cierto(!tieneTipConBucle, "ConsejosDelTaller no avisa si ya usa bucle sobre enemigosVistos()");

            Pelea p = Pelea.contraMultiples(con("yo", Modelo.de("cobalto"), de(Sencillo::new)), java.util.List.of(
                    Rivales.dron(0.3, 0.3, new Random(1), 1), Rivales.dron(0.3, 0.3, new Random(2), 2)), 77L);
            p.empezarRonda();
            p.avanzar();
            for (metajavbots.textos.Idioma id : metajavbots.textos.Idioma.values()) {
                metajavbots.textos.T.idioma = id;
                String esperado = metajavbots.textos.T.t("revisor.multiples_objetivos");
                cierto(!esperado.equals("revisor.multiples_objetivos") && !esperado.isBlank(), "texto revisor en " + id);
                String tip = metajavbots.textos.T.t("entrenador.tip.multiples_objetivos");
                cierto(!tip.equals("entrenador.tip.multiples_objetivos") && !tip.isBlank(), "texto tip en " + id);
                metajavbots.pantallas.Revisor.Informe infSin = metajavbots.pantallas.Revisor.revisar(sinBucle, p, id);
                cierto(infSin.consejos.contains(esperado), "Revisor avisa sin bucle en " + id);
                String conBucle = id == metajavbots.textos.Idioma.ES ? conBucleEs
                        : id == metajavbots.textos.Idioma.DE ? conBucleDe : conBucleEn;
                metajavbots.pantallas.Revisor.Informe infCon = metajavbots.pantallas.Revisor.revisar(conBucle, p, id);
                cierto(!infCon.consejos.contains(esperado), "Revisor no avisa con bucle en " + id);
            }
            metajavbots.textos.T.idioma = metajavbots.textos.Idioma.ES;
        });
    }

    private static double frecuenciaMultiples(metajavbots.progreso.Etiqueta etiqueta, int muestras, long semilla) {
        metajavbots.progreso.Jugador j = new metajavbots.progreso.Jugador("test");
        j.etiqueta = etiqueta;
        Random r = new Random(semilla);
        int multiples = 0;
        for (int i = 0; i < muestras; i++) {
            metajavbots.pantallas.Encuentro enc = metajavbots.pantallas.PrepararPelea.encuentro(j, "ADAPTADA", r);
            if (!enc.demas.isEmpty()) {
                cierto(enc.demas.size() >= 1 && enc.demas.size() <= 2, "entre 2 y 3 enemigos totales");
                for (int eq : enc.equiposDeLosDemas) igual(1, eq, "todos los enemigos en equipo 1");
                multiples++;
            }
        }
        return (double) multiples / muestras;
    }
}
