package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.List;
import java.util.Locale;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.ModificadoresPoo;
import metajavbots.combate.Pelea;
import metajavbots.pantallas.Revisor;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.piezas.Pieza;
import metajavbots.rivales.Muneco;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas de los efectos jugables en combate de los modificadores POO (private, final, static, protected, paquete y public). */
final class PruebasDeModificadoresPoo {

    private PruebasDeModificadoresPoo() {
    }

    static void apuntar() {
        apuntarPrivate();
        apuntarFinal();
        apuntarStaticEquipos();
        apuntarPublicMutable();
        apuntarProtectedYPaquete();
        apuntarTextos();
    }

    private static void apuntarPrivate() {
        caso("poo: private en campos de estado protege del daño y reduce el radar rival", () -> {
            String sinCampos = "import metajavbots.api.*;\n"
                    + "public class BotNeutro extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() {}\n"
                    + "}\n";
            String privado = "import metajavbots.api.*;\n"
                    + "public class BotPrivado extends Metajavbot {\n"
                    + "    private int fase = 1;\n"
                    + "    private double ultimoAngulo = 0;\n"
                    + "    private boolean alerta = false;\n"
                    + "    private void actualizar() { fase++; alerta = !alerta; ultimoAngulo += 1; }\n"
                    + "    @Override public void cadaTurno() { actualizar(); }\n"
                    + "}\n";
            Pelea pNeutro = peleaCon(sinCampos, 101);
            Pelea pPriv = peleaCon(privado, 101);
            ModificadoresPoo ef = pPriv.combatientes[0].efectosPoo;
            igual(3, ef.camposPrivados, "tres campos private");
            igual(1, ef.metodosPrivados, "un método private");
            cierto(ef.factorDanoRecibido < 1.0, "private reduce el daño recibido: " + ef.factorDanoRecibido);
            cierto(ef.factorRadarRival < 1.0, "private reduce el alcance del radar rival: " + ef.factorRadarRival);

            // Daño real en combate: golpeEspecial con el mismo daño base.
            pNeutro.golpeEspecial(1, 0, 20, "prueba");
            pPriv.golpeEspecial(1, 0, 20, "prueba");
            double danoNeutro = pNeutro.combatientes[0].danoTotalRecibido;
            double danoPriv = pPriv.combatientes[0].danoTotalRecibido;
            cierto(danoPriv < danoNeutro - 0.5, "private recibe menos daño (" + danoPriv + " vs " + danoNeutro + ")");

            // Radar real del rival: a 94 % del alcance base ve al neutro pero no al encapsulado.
            double radarBase = radarDe(pNeutro.cuerpo(1));
            colocarParaRadar(pNeutro, radarBase * 0.94);
            colocarParaRadar(pPriv, radarBase * 0.94);
            pNeutro.avanzar();
            pPriv.avanzar();
            cierto(!pNeutro.cuerpo(1).vistos().isEmpty(), "el rival ve al robot neutro dentro del radar base");
            cierto(pPriv.cuerpo(1).vistos().isEmpty(), "el rival NO detecta al robot private a esa distancia");
        });
    }

    private static void apuntarFinal() {
        caso("poo: final en campos de configuración amplía el tope de pasos y evita TopeDeCalculo", () -> {
            String sinFinal = "import metajavbots.api.*;\n"
                    + "public class BotSinFinal extends Metajavbot {\n"
                    + "    private int vueltas = 108;\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        int s = 0;\n"
                    + "        for (int i = 0; i < vueltas; i++) s += i;\n"
                    + "        if (s < 0) avanzar(1);\n"
                    + "    }\n"
                    + "}\n";
            String conFinal = "import metajavbots.api.*;\n"
                    + "public class BotConFinal extends Metajavbot {\n"
                    + "    private final int VUELTAS = 108;\n"
                    + "    private final double DISTANCIA = 120;\n"
                    + "    private final double GIRO = 15;\n"
                    + "    private final int CADENCIA = 5;\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        int s = 0;\n"
                    + "        for (int i = 0; i < VUELTAS; i++) s += i;\n"
                    + "        if (s < 0 && DISTANCIA > GIRO + CADENCIA) avanzar(1);\n"
                    + "    }\n"
                    + "}\n";
            Pelea pSin = peleaCon(sinFinal, 202);
            Pelea pCon = peleaCon(conFinal, 202);
            pSin.topeDePasos = 100;
            pCon.topeDePasos = 100;

            ModificadoresPoo ef = pCon.combatientes[0].efectosPoo;
            igual(4, ef.camposFinal, "cuatro campos final");
            cierto(ef.factorPasos > 1.15, "bono de pasos con final: " + ef.factorPasos);
            igual(100L, pSin.cuerpo(0).topeDePasos(), "sin final el tope es el base");
            cierto(pCon.cuerpo(0).topeDePasos() >= 120L, "con final el tope sube: " + pCon.cuerpo(0).topeDePasos());

            pSin.avanzar();
            pCon.avanzar();
            cierto(pSin.informe(0).topes > 0, "sin final se pasa del tope de 100 pasos");
            igual(0, pCon.informe(0).topes, "con final completa el bucle sin tocar el tope");
        });
    }

    private static void apuntarStaticEquipos() {
        caso("poo: static compartido en 2v2 comparte datos entre aliados y da sinergia de equipo", () -> {
            String codigo = "import metajavbots.api.*;\n"
                    + "public class BotStaticEquipo extends Metajavbot {\n"
                    + "    private static int senal = 0;\n"
                    + "    @Override public void cadaTurno() {\n"
                    + "        if (turnoActual() == 0 && senal == 0) senal = 77;\n"
                    + "        if (turnoActual() == 1) memoria().put(\"senal\", senal);\n"
                    + "    }\n"
                    + "}\n";
            RobotDelJugador rj = compilar(codigo);
            List<Modelo> ms = Modelo.todos();
            Combatiente a1 = new Combatiente("Aliado1", new Montaje(ms.get(0)), rj);
            Combatiente r1 = new Combatiente("Rival1", new Montaje(ms.get(1)), Muneco::new);
            Combatiente a2 = new Combatiente("Aliado2", new Montaje(ms.get(0)), rj);
            Combatiente r2 = new Combatiente("Rival2", new Montaje(ms.get(1)), Muneco::new);
            Pelea p2v2 = new Pelea(new Combatiente[]{a1, r1, a2, r2}, new int[]{0, 1, 0, 1}, 303);
            p2v2.rondas = 1;
            p2v2.empezarRonda();

            ModificadoresPoo ef2v2 = p2v2.combatientes[0].efectosPoo;
            igual(1, ef2v2.camposStatic, "un campo static compartido");
            cierto(ef2v2.enEquipo, "detecta modo equipos");
            cierto(ef2v2.factorRadarPropio > 1.05, "bono de radar propio en 2v2: " + ef2v2.factorRadarPropio);
            cierto(ef2v2.factorRecarga > 1.05, "bono de recarga de energía en 2v2: " + ef2v2.factorRecarga);

            // Compara recarga real frente a un 1v1 con el mismo robot.
            Pelea p1v1 = peleaCon(codigo, 303);
            falso(p1v1.combatientes[0].efectosPoo.enEquipo, "en 1v1 no hay sinergia de equipo");
            igual(1.0, p1v1.combatientes[0].efectosPoo.factorRecarga, "en 1v1 recarga normal");
            p2v2.cuerpo(0).energia = 20;
            p1v1.cuerpo(0).energia = 20;
            p2v2.avanzar();
            p1v1.avanzar();
            cierto(p2v2.cuerpo(0).energia > p1v1.cuerpo(0).energia, "2v2 con static recarga más energía por turno");

            // Turno 2: el segundo aliado lee el dato escrito en el campo static por el primer aliado.
            p2v2.avanzar();
            igual(77, p2v2.combatientes[2].memoria.get("senal"), "el aliado lee el campo static compartido");
        });
    }

    private static void apuntarPublicMutable() {
        caso("poo: public mutable penaliza daño, expone al radar y genera aviso táctico", () -> {
            String sinCampos = "import metajavbots.api.*;\n"
                    + "public class BotLimpioPub extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() {}\n"
                    + "}\n";
            String publico = "import metajavbots.api.*;\n"
                    + "public class BotPublico extends Metajavbot {\n"
                    + "    public int objetivoX = 0;\n"
                    + "    public double energiaLibre = 100;\n"
                    + "    @Override public void cadaTurno() { objetivoX++; }\n"
                    + "}\n";
            Pelea pNeutro = peleaCon(sinCampos, 404);
            Pelea pPub = peleaCon(publico, 404);
            ModificadoresPoo ef = pPub.combatientes[0].efectosPoo;
            igual(2, ef.camposPublicosMutables, "dos campos public mutables");
            cierto(ef.factorDanoRecibido > 1.05, "public mutable aumenta daño recibido: " + ef.factorDanoRecibido);
            cierto(ef.factorRadarRival > 1.10, "public mutable aumenta radar rival: " + ef.factorRadarRival);

            pNeutro.golpeEspecial(1, 0, 20, "prueba");
            pPub.golpeEspecial(1, 0, 20, "prueba");
            cierto(pPub.combatientes[0].danoTotalRecibido > pNeutro.combatientes[0].danoTotalRecibido + 0.5,
                    "public mutable recibe más daño real");

            // Radar real del rival: a 108 % del alcance base NO ve al neutro, pero SÍ detecta al public mutable.
            double radarBase = radarDe(pNeutro.cuerpo(1));
            colocarParaRadar(pNeutro, radarBase * 1.08);
            colocarParaRadar(pPub, radarBase * 1.08);
            pNeutro.avanzar();
            pPub.avanzar();
            cierto(pNeutro.cuerpo(1).vistos().isEmpty(), "fuera del radar base no ve al robot neutro");
            cierto(!pPub.cuerpo(1).vistos().isEmpty(), "pero SÍ detecta al robot con campos public mutables");

            // Resumen POO y aviso táctico del Revisor.
            boolean hayPena = false;
            for (ModificadoresPoo.Entrada e : ef.resumen()) {
                if (e.nivel == ModificadoresPoo.Nivel.PENALIZACION) hayPena = true;
            }
            cierto(hayPena, "resumen incluye entrada de penalización");
            Revisor.Informe rev = Revisor.revisar(publico, pPub, Idioma.ES);
            boolean hayConsejo = false;
            for (String c : rev.consejos) {
                if (c.contains("public")) hayConsejo = true;
            }
            cierto(hayConsejo, "el Revisor añade aviso táctico por campos public mutables: " + rev.consejos);
        });
    }

    private static void apuntarProtectedYPaquete() {
        caso("poo: protected y acceso por defecto aplican efectos diferenciados", () -> {
            String prot = "import metajavbots.api.*;\n"
                    + "public class BotProtected extends Metajavbot {\n"
                    + "    protected int nivel = 1;\n"
                    + "    protected double rumboBase = 0;\n"
                    + "    protected void pasoInterno() { nivel++; }\n"
                    + "    @Override public void cadaTurno() { pasoInterno(); }\n"
                    + "}\n";
            String paq = "import metajavbots.api.*;\n"
                    + "public class BotPaquete extends Metajavbot {\n"
                    + "    int contador = 0;\n"
                    + "    double marca = 0;\n"
                    + "    @Override public void cadaTurno() { contador++; marca += 1; }\n"
                    + "}\n";
            Pelea pProt = peleaCon(prot, 505);
            Pelea pPaq = peleaCon(paq, 505);

            ModificadoresPoo efProt = pProt.combatientes[0].efectosPoo;
            igual(2, efProt.camposProtected, "dos campos protected");
            igual(1, efProt.metodosProtected, "un método protected");
            cierto(efProt.factorDanoRecibido < 1.0 && efProt.factorDanoRecibido > 0.95, "protected da bono defensivo moderado");
            cierto(efProt.factorRadarRival < 1.0, "protected reduce ligeramente el radar rival");
            cierto(efProt.factorPasos > 1.0, "método protected aporta ligero bono de pasos");

            ModificadoresPoo efPaq = pPaq.combatientes[0].efectosPoo;
            igual(2, efPaq.camposPaquete, "dos campos de acceso por defecto");
            igual(1.0, efPaq.factorDanoRecibido, "acceso por defecto no penaliza daño");
            cierto(efPaq.factorRadarRival > 1.0 && efPaq.factorRadarRival < 1.10, "acceso por defecto expone levemente al radar");
        });
    }

    private static void apuntarTextos() {
        caso("poo: textos y resumen localizado en es, de y en sin claves faltantes", () -> {
            String mixto = "import metajavbots.api.*;\n"
                    + "public class BotMixto extends Metajavbot {\n"
                    + "    private int estado = 0;\n"
                    + "    private final int LIMITE = 5;\n"
                    + "    private static int comun = 0;\n"
                    + "    protected int heredable = 0;\n"
                    + "    int paquete = 0;\n"
                    + "    public int expuesto = 0;\n"
                    + "    @Override public void cadaTurno() { estado++; comun++; }\n"
                    + "}\n";
            Pelea p = peleaCon(mixto, 606);
            String[] claves = {
                    "poo.titulo", "poo.ninguno", "poo.private", "poo.final", "poo.static_equipo",
                    "poo.static_solo", "poo.protected", "poo.paquete", "poo.public", "poo.consejo_public"};
            Idioma antes = T.idioma;
            try {
                for (Idioma id : Idioma.values()) {
                    T.idioma = id;
                    for (String k : claves) {
                        String v = T.t(k);
                        cierto(!v.equals(k) && !v.isBlank(), "falta clave " + k + " en " + id);
                    }
                    List<ModificadoresPoo.Entrada> res = p.combatientes[0].efectosPoo.resumen();
                    cierto(res.size() >= 6, "resumen completo en " + id + ": " + res.size());
                    for (ModificadoresPoo.Entrada e : res) {
                        cierto(!e.texto.isBlank() && !e.texto.contains("poo."), "texto válido en " + id + ": " + e.texto);
                    }
                }
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static RobotDelJugador compilar(String codigo) {
        RobotDelJugador rj = new RobotDelJugador();
        Compilador.Resultado res = rj.compilar(codigo, Locale.ROOT, false);
        cierto(res.bien, "compila: " + res.errores);
        return rj;
    }

    private static Pelea peleaCon(String codigo, long semilla) {
        RobotDelJugador rj = compilar(codigo);
        List<Modelo> ms = Modelo.todos();
        Combatiente a = new Combatiente("Jugador", new Montaje(ms.get(0)), rj);
        Combatiente b = new Combatiente("Muneco", new Montaje(ms.get(1)), Muneco::new);
        Pelea p = new Pelea(a, b, semilla);
        p.rondas = 1;
        p.empezarRonda();
        return p;
    }

    private static double radarDe(Cuerpo c) {
        Pieza cabeza = c.combatiente.montaje.pieza(Montaje.Hueco.CABEZA);
        return cabeza == null ? 400 : cabeza.valor("radar", 400);
    }

    private static void colocarParaRadar(Pelea p, double distancia) {
        Cuerpo jugador = p.cuerpo(0);
        Cuerpo rival = p.cuerpo(1);
        rival.x = 150;
        rival.y = 350;
        rival.mira = 0;
        rival.rumbo = 0;
        jugador.x = rival.x + distancia;
        jugador.y = rival.y;
        jugador.rumbo = 180;
        jugador.mira = 180;
    }
}
