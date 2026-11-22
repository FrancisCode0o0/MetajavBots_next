package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import metajavbots.api.Metajavbot;
import metajavbots.codigo.AnalizadorDeBytecode;
import metajavbots.codigo.Compilador;
import metajavbots.codigo.DetectorDeTemas;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.codigo.Vigilante;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.combate.PresupuestoSenior;
import metajavbots.pantallas.Jefe;
import metajavbots.pantallas.Marcadores;
import metajavbots.pantallas.ModoEspecial;
import metajavbots.pantallas.Revisor;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.rivales.RivalSenior;
import metajavbots.rivales.Rivales;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;

/**
 * Pruebas de nivel Senior (bloque 4):
 * - Jefes Senior (records, selladas, inmutabilidad, estrategia y bytecode).
 * - Presupuesto determinista de tiempo y memoria por turno (cumplimiento y penalización).
 * - Robots en paralelo bajo Vigilante/Cargador sin deadlocks ni fugas de hilos.
 * - Rivales Senior que castigan fallos típicos y consejos del Revisor.
 * - Analizador de bytecode real (.class) y conocimiento del entrenador.
 */
final class PruebasDeSenior {

    private PruebasDeSenior() {
    }

    private static String robot(String dentro) {
        return "import metajavbots.api.*;\n\npublic class MiRobot extends Metajavbot {\n" + dentro
                + "\n    @Override\n    public void cadaTurno() {\n        avanzar(5);\n    }\n}\n";
    }

    private static Combatiente compilarCombatiente(String nombre, String codigo) {
        RobotDelJugador rj = new RobotDelJugador();
        Compilador.Resultado res = rj.compilar(codigo, Locale.ROOT, false);
        cierto(res.bien, "compila " + nombre + ": " + res.errores);
        return new Combatiente(nombre, new Montaje(Modelo.todos().get(0)), rj);
    }

    private static int contarHilosParalelos() {
        int n = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("metajavbots-paralelo-")) n++;
        }
        return n;
    }

    static void apuntar() {
        temasYJefesSenior();
        presupuestoTiempoYMemoria();
        robotsEnParaleloSinFugas();
        rivalesSeniorYRevisor();
        analizadorDeBytecodeYEntrenador();
    }

    private static void temasYJefesSenior() {
        caso("senior: cada tema de arriba (incl. bytecode) se reconoce en un robot que compila en la arena", () -> {
            String[][] casos = {
                {DetectorDeTemas.RECORDS, "    record Avistamiento(double x, double y) {}\n    private Avistamiento ultimo;\n"
                    + "    @Override\n    public void alVerEnemigo(Enemigo e) {\n        ultimo = new Avistamiento(e.x(), e.y());\n    }\n"},
                {DetectorDeTemas.SELLADAS, "    sealed interface Distancia permits Cerca, Lejos {}\n    record Cerca(double d) implements Distancia {}\n"
                    + "    record Lejos(double d) implements Distancia {}\n    @Override\n    public void alVerEnemigo(Enemigo e) {\n"
                    + "        Distancia d = e.distancia() < 100 ? new Cerca(e.distancia()) : new Lejos(e.distancia());\n"
                    + "        switch (d) {\n            case Cerca c -> atacar();\n            case Lejos l -> avanzar(l.d() / 2);\n        }\n    }\n"},
                {DetectorDeTemas.INMUTABILIDAD, "    static final class Plan {\n        private final double x;\n        private final double y;\n"
                    + "        Plan(double x, double y) {\n            this.x = x;\n            this.y = y;\n        }\n"
                    + "        Plan movido(double dx) {\n            return new Plan(x + dx, y);\n        }\n    }\n"},
                {DetectorDeTemas.ESTRATEGIA, "    interface Tactica {\n        void jugar(MiRobot r);\n    }\n"
                    + "    static class Huir implements Tactica {\n        public void jugar(MiRobot r) {\n            r.retroceder(50);\n        }\n    }\n"
                    + "    static class Atacar implements Tactica {\n        public void jugar(MiRobot r) {\n            r.atacar();\n        }\n    }\n"
                    + "    private Tactica tactica = new Atacar();\n"},
                {DetectorDeTemas.BYTECODE, "    private static final int MASCARA = 0x0F;\n    private int fase;\n"
                    + "    private int siguiente() {\n        return (++fase) & MASCARA;\n    }\n"},
            };
            for (String[] k : casos) {
                String codigo = robot(k[1]);
                Set<String> temas = DetectorDeTemas.temas(codigo);
                cierto(temas.contains(k[0]), k[0] + ": " + temas);
                if (Compilador.hayCompilador()) {
                    Compilador.Resultado r = Compilador.compilar(codigo, true, Vigilante.Modo.ROBOT, Locale.ROOT);
                    cierto(r.bien, k[0] + " compila en la arena: " + r.errores);
                }
            }
            Set<String> normal = DetectorDeTemas.temas(robot("    private double x;\n    public void setX(double v) {\n        x = v;\n    }\n"));
            for (String s : new String[]{DetectorDeTemas.RECORDS, DetectorDeTemas.SELLADAS, DetectorDeTemas.INMUTABILIDAD,
                DetectorDeTemas.ESTRATEGIA, DetectorDeTemas.BYTECODE}) {
                cierto(!normal.contains(s), "un robot normal no es " + s);
            }
            for (String id : new String[]{"records", "selladas", "inmutabilidad", "estrategia", "bytecode"}) {
                cierto(Jefe.de(id) != null, "el jefe " + id);
            }
            cierto(ModoEspecial.CLAVES_PESTANAS.contains("especial.senior"), "ModoEspecial incluye pestaña especial.senior");
        });
    }

    private static void presupuestoTiempoYMemoria() {
        caso("senior: presupuesto de tiempo y memoria determinista (cumplimiento y penalización)", () -> {
            if (!Compilador.hayCompilador()) return;
            String eficiente = "import metajavbots.api.*;\npublic class RobotLimpio extends Metajavbot {\n"
                    + "    private double dir = 1;\n"
                    + "    @Override public void cadaTurno() { avanzar(10); girar(2 * dir); }\n}\n";
            Combatiente cLimpio = compilarCombatiente("Limpio", eficiente);
            Pelea pOk = new Pelea(cLimpio, Rivales.muneco(), 42L, 1000, 700, 30);
            pOk.rondas = 1;
            pOk.presupuestoNanosPorTurno = 20_000_000L;
            pOk.presupuestoBytesPorTurno = 4_096L;
            pOk.empezarRonda();
            for (int i = 0; i < 15; i++) pOk.avanzar();
            igual(0, pOk.informe(0).excesosMemoria, "robot limpio no supera presupuesto de memoria");
            igual(0, pOk.informe(0).excesosTiempo, "robot limpio no supera presupuesto de tiempo");
            String hud = Marcadores.resumenSeniorHud(pOk, pOk.cuerpo(0));
            cierto(hud.contains("ms") && hud.contains("KB"), "HUD Senior muestra tiempo y memoria: " + hud);

            // Robot derrochador: asigna arrays dentro de un bucle y retiene lista creciente.
            String derrochador = "import metajavbots.api.*;\nimport java.util.*;\npublic class RobotDerrochador extends Metajavbot {\n"
                    + "    private final List<int[]> basura = new ArrayList<>();\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        for (int i = 0; i < 40; i++) { int[] tmp = new int[200]; if (i == 0) basura.add(tmp); }\n"
                    + "        avanzar(5);\n    }\n}\n";
            Combatiente cMal = compilarCombatiente("Derrochador", derrochador);
            Pelea pMem = new Pelea(cMal, Rivales.muneco(), 42L, 1000, 700, 20);
            pMem.rondas = 1;
            pMem.presupuestoBytesPorTurno = 2_048L;
            pMem.presupuestoNanosPorTurno = 1L; // 1 ns: fuerza también exceso de tiempo de forma determinista
            pMem.empezarRonda();
            double energiaAntes = pMem.cuerpo(0).energia;
            pMem.avanzar();
            Cuerpo cu = pMem.cuerpo(0);
            cierto(pMem.informe(0).excesosMemoria > 0, "supera presupuesto de memoria");
            cierto(pMem.informe(0).excesosTiempo > 0, "supera presupuesto de tiempo");
            cierto(cu.deuda > 0, "recibe penalización de deuda de turnos");
            cierto(cu.energia < energiaAntes, "recibe drenaje de energía por superar presupuesto");

            // Determinismo y seguridad frente a ciclos en el estimador de memoria retenida.
            List<Object> ciclo = new ArrayList<>();
            ciclo.add(ciclo);
            long m1 = PresupuestoSenior.estimarMemoriaRetenida(new Metajavbot() { }, Map.of("ciclo", ciclo, "arr", new int[100]));
            long m2 = PresupuestoSenior.estimarMemoriaRetenida(new Metajavbot() { }, Map.of("ciclo", ciclo, "arr", new int[100]));
            igual(m1, m2, "estimación de memoria retenida 100 % determinista");
            cierto(m1 >= 400L, "contabiliza tamaño del array retenido: " + m1);
        });
    }

    private static void robotsEnParaleloSinFugas() {
        caso("senior: robots en paralelo bajo Vigilante/Cargador sin deadlocks ni fugas de hilos", () -> {
            if (!Compilador.hayCompilador()) return;
            // Vigilante sigue bloqueando hilos crudos en robots.
            String prohibido = "import metajavbots.api.*;\npublic class RobotHilo extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { new Thread(() -> {}).start(); }\n}\n";
            Compilador.Resultado rProh = Compilador.compilar(prohibido, true, Vigilante.Modo.ROBOT, Locale.ROOT);
            falso(rProh.bien, "Vigilante sigue bloqueando Thread en Modo.ROBOT");

            String paraleloSeguro = "import metajavbots.api.*;\nimport java.util.concurrent.atomic.*;\n"
                    + "public class RobotParalelo extends Metajavbot {\n"
                    + "    private final AtomicInteger cuenta = new AtomicInteger();\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        if (cuenta.incrementAndGet() % 5 == 0) avisarAlEquipo(\"ok\");\n"
                    + "        avanzar(10); girar(4);\n    }\n}\n";
            Combatiente j1 = compilarCombatiente("Jugador1", paraleloSeguro);
            Combatiente j2 = compilarCombatiente("Jugador2", paraleloSeguro);
            Combatiente r1 = RivalSenior.crear(RivalSenior.Tipo.CONCURRENCIA, Idioma.ES);
            Combatiente r2 = RivalSenior.crear(RivalSenior.Tipo.CONCURRENCIA, Idioma.ES);
            Pelea pelea = new Pelea(new Combatiente[]{j1, r1, j2, r2}, new int[]{0, 1, 0, 1}, 99L, 1000, 700, 40);
            pelea.rondas = 1;
            pelea.pensarEnParalelo = true;
            pelea.jugarEntera();
            igual(Pelea.Estado.TERMINADA, pelea.estado(), "pelea paralela termina sin deadlocks");
            cierto(j1.informes.get(0).turnos > 0 && j2.informes.get(0).turnos > 0, "ambos robots aliados pensaron");
            igual(0, contarHilosParalelos(), "sin hilos metajavbots-paralelo vivos tras terminar la pelea");

            // Un bucle infinito en paralelo se corta y no deja hilos vivos tras rendirse.
            String infinito = "import metajavbots.api.*;\npublic class RobotInfinito extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { while (true) { avanzar(1); } }\n}\n";
            Combatiente inf = compilarCombatiente("Infinito", infinito);
            Pelea pInf = new Pelea(inf, Rivales.muneco(), 7L, 1000, 700, 30);
            pInf.rondas = 1;
            pInf.topeDePasos = 100;
            pInf.pensarEnParalelo = true;
            pInf.empezarRonda();
            pInf.avanzar();
            cierto(pInf.informe(0).topes > 0, "bucle infinito en paralelo cortado por tope");
            pInf.rendirse(0);
            igual(0, contarHilosParalelos(), "sin hilos vivos tras rendirse en pelea paralela");
        });
    }

    private static void rivalesSeniorYRevisor() {
        caso("senior: rivales que castigan fallos típicos en 3 idiomas y retroalimentación en Revisor", () -> {
            igual(5, RivalSenior.todos().size(), "cinco rivales Senior");
            for (RivalSenior.Tipo t : RivalSenior.todos()) {
                for (Idioma id : Idioma.values()) {
                    cierto(!t.nombre.en(id).isBlank(), t.id + " nombre " + id);
                    cierto(!t.biografiaAbsurda.en(id).isBlank(), t.id + " bio absurda " + id);
                    cierto(!t.descripcion.en(id).isBlank(), t.id + " desc " + id);
                    cierto(!t.consejoRevision.en(id).isBlank(), t.id + " consejo " + id);
                }
                Combatiente c = RivalSenior.crear(t, Idioma.ES);
                Pelea p = new Pelea(Rivales.muneco(), c, 123L, 1000, 700, 30);
                p.rondas = 1;
                p.rivalSeniorId = t.id;
                p.jugarEntera();
                igual(Pelea.Estado.TERMINADA, p.estado(), "rival Senior " + t.id + " pelea sin errores");
            }
            if (!Compilador.hayCompilador()) return;
            // Robot que dispara NullPointerException contra Señor NullPointer y no normaliza contra Barón 361°.
            String conNulo = "import metajavbots.api.*;\npublic class RobotNulo extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { String s = miAparato().tipo(); avanzar(s.length()); }\n}\n";
            Combatiente cNulo = compilarCombatiente("Nulo", conNulo);
            Pelea pNulo = new Pelea(cNulo, RivalSenior.crear(RivalSenior.Tipo.NULOS, Idioma.ES), 5L, 1000, 700, 20);
            pNulo.rondas = 1;
            pNulo.rivalSeniorId = RivalSenior.Tipo.NULOS.id;
            pNulo.empezarRonda();
            pNulo.avanzar();
            cierto(pNulo.informe(0).fallosNulos > 0, "registra NullPointerException");
            T.idioma = Idioma.ES;
            Revisor.Informe revNulo = Revisor.revisar(conNulo, pNulo, Idioma.ES);
            cierto(revNulo.consejos.stream().anyMatch(s -> s.contains("NullPointerException")), "Revisor avisa de NPE: " + revNulo.consejos);

            Pelea pAng = new Pelea(Rivales.muneco(), RivalSenior.crear(RivalSenior.Tipo.ANGULOS, Idioma.ES), 5L, 1000, 700, 10);
            pAng.rondas = 1;
            pAng.rivalSeniorId = RivalSenior.Tipo.ANGULOS.id;
            pAng.empezarRonda();
            pAng.avanzar();
            Revisor.Informe revAng = Revisor.revisar("public class R extends Metajavbot { public void cadaTurno() { girar(10); } }", pAng, Idioma.ES);
            cierto(revAng.consejos.stream().anyMatch(s -> s.contains("normalizar")), "Revisor avisa de ángulos: " + revAng.consejos);
        });
    }

    private static void analizadorDeBytecodeYEntrenador() {
        caso("senior: AnalizadorDeBytecode lee .class reales y el entrenador conoce peleas_senior", () -> {
            if (Compilador.hayCompilador()) {
                String conBoxYStatic = "import metajavbots.api.*;\nimport java.util.*;\npublic class RobotPesado extends Metajavbot {\n"
                        + "    public static int compartido;\n"
                        + "    @Override public synchronized void cadaTurno() {\n"
                        + "        compartido++;\n"
                        + "        Integer box = compartido;\n"
                        + "        List<Integer> l = new ArrayList<>();\n"
                        + "        l.add(box);\n"
                        + "        avanzar(l.size());\n    }\n}\n";
                Compilador.Resultado rPes = Compilador.compilar(conBoxYStatic, true, Vigilante.Modo.ROBOT, Locale.ROOT);
                cierto(rPes.bien, "compila RobotPesado: " + rPes.errores);
                AnalizadorDeBytecode.Informe infPes = AnalizadorDeBytecode.analizarTodas(rPes.clases);
                cierto(infPes.clases >= 1 && infPes.instrucciones > 10, "lee instrucciones reales: " + infPes.resumen());
                cierto(infPes.autoboxing > 0, "detecta autoboxing Integer.valueOf");
                cierto(infPes.asignaciones > 0, "detecta opcode NEW");
                cierto(infPes.tieneEstadoEstaticoMutable(), "detecta estado estático mutable / PUTSTATIC");
                falso(infPes.esEficiente(), "RobotPesado no es bytecode eficiente");

                String limpio = "import metajavbots.api.*;\npublic class RobotZeroBox extends Metajavbot {\n"
                        + "    private static final int PASO = 8;\n"
                        + "    @Override public void cadaTurno() { avanzar(PASO); }\n}\n";
                Compilador.Resultado rLim = Compilador.compilar(limpio, true, Vigilante.Modo.ROBOT, Locale.ROOT);
                cierto(rLim.bien, "compila RobotZeroBox");
                AnalizadorDeBytecode.Informe infLim = AnalizadorDeBytecode.analizarTodas(rLim.clases);
                cierto(infLim.esEficiente(), "RobotZeroBox es bytecode eficiente: " + infLim.resumen());
                falso(infLim.tieneEstadoEstaticoMutable(), "RobotZeroBox sin estado estático mutable");
            }
            Concepto c = Concepto.de("peleas_senior");
            cierto(c != null, "existe ficha peleas_senior en 07_juego.txt");
            for (Idioma id : Idioma.values()) {
                cierto(!c.titulo.en(id).isBlank() && c.parrafos(id).size() >= 2, "peleas_senior completo en " + id);
            }
            cierto(metajavbots.tutor.ConocimientoDelEntrenador.resumenParaIa().contains("Senior fights"),
                    "peleas_senior incluido en resumenParaIa()");
        });
    }
}
