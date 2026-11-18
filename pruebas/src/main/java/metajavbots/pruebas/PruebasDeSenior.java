package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;

import java.util.Locale;
import java.util.Set;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.DetectorDeTemas;
import metajavbots.codigo.Vigilante;

/** Los jefes de senior (bloque 4): records, jerarquías selladas con patrones, inmutabilidad y el patrón estrategia. */
final class PruebasDeSenior {

    private PruebasDeSenior() {
    }

    private static String robot(String dentro) {
        return "import metajavbots.api.*;\n\npublic class MiRobot extends Metajavbot {\n" + dentro
                + "\n    @Override\n    public void cadaTurno() {\n        avanzar(5);\n    }\n}\n";
    }

    static void apuntar() {
        caso("senior: cada tema de arriba se reconoce en un robot que compila en la arena (y uno normal no da ninguno)", () -> {
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
            for (String s : new String[]{DetectorDeTemas.RECORDS, DetectorDeTemas.SELLADAS, DetectorDeTemas.INMUTABILIDAD, DetectorDeTemas.ESTRATEGIA}) {
                cierto(!normal.contains(s), "un robot normal no es " + s);
            }
            for (String id : new String[]{"records", "selladas", "inmutabilidad", "estrategia"}) {
                cierto(metajavbots.pantallas.Jefe.de(id) != null, "el jefe " + id);
            }
        });
    }
}
