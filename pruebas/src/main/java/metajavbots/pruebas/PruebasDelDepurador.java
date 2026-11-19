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
import metajavbots.combate.OrdenDelTurno;
import metajavbots.combate.Pelea;
import metajavbots.piezas.Modelo;
import metajavbots.piezas.Montaje;
import metajavbots.rivales.Muneco;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas del depurador en vivo (F9): traza de líneas del jugador, órdenes del turno y detección de órdenes pisadas. */
final class PruebasDelDepurador {

    private PruebasDelDepurador() {
    }

    static void apuntar() {
        apuntarTraza();
        apuntarPisada();
        apuntarSinFalsosPositivos();
        apuntarDetenerYTextos();
    }

    private static void apuntarTraza() {
        caso("depurador: captura la traza de líneas y las órdenes del turno del jugador", () -> {
            String codigo = "import metajavbots.api.*;\n"
                    + "public class BotTraza extends Metajavbot {\n"
                    + "    @Override\n"
                    + "    public void cadaTurno() {\n"
                    + "        avanzar(40);\n"
                    + "        girar(15);\n"
                    + "    }\n"
                    + "}\n";
            Pelea p = peleaCon(codigo, 7);
            for (int i = 0; i < 5; i++) p.avanzar();
            Cuerpo c = p.cuerpo(0);
            cierto(c.lineasDelTurno.get(5), "línea 5 (avanzar) en la traza: " + c.lineasDelTurno);
            cierto(c.lineasDelTurno.get(6), "línea 6 (girar) en la traza: " + c.lineasDelTurno);
            igual(6, c.ultimaLinea, "última línea ejecutada");
            igual(2, c.ordenesDelTurno.size(), "dos órdenes en el turno");
            igual("cadaTurno", c.ordenesDelTurno.get(0).metodo, "método de la primera orden");
            igual("avanzar(40)", c.ordenesDelTurno.get(0).accion, "primera acción");
            igual(5, c.ordenesDelTurno.get(0).linea, "línea de avanzar");
            igual("girar(15°)", c.ordenesDelTurno.get(1).accion, "segunda acción");
            igual(6, c.ordenesDelTurno.get(1).linea, "línea de girar");
            falso(c.tieneConflictoDeOrdenes, "sin conflictos dentro de cadaTurno");
        });
    }

    private static void apuntarPisada() {
        caso("depurador: detecta alVerEnemigo pisado por cadaTurno con la línea exacta", () -> {
            String codigo = "import metajavbots.api.*;\n"
                    + "public class BotPisado extends Metajavbot {\n"
                    + "    @Override\n"
                    + "    public void alVerEnemigo(Enemigo e) {\n"
                    + "        apuntarHacia(e.angulo());\n"
                    + "        atacar();\n"
                    + "    }\n"
                    + "    @Override\n"
                    + "    public void cadaTurno() {\n"
                    + "        avanzar(30);\n"
                    + "        apuntar(10);\n"
                    + "    }\n"
                    + "}\n";
            Pelea p = peleaCon(codigo, 11);
            Cuerpo c = p.cuerpo(0);
            boolean visto = false;
            for (int i = 0; i < 200 && p.estado() == Pelea.Estado.LUCHANDO; i++) {
                p.avanzar();
                if (c.tieneConflictoDeOrdenes) {
                    visto = true;
                    break;
                }
            }
            cierto(visto, "en algún turno alVerEnemigo es pisado por cadaTurno");
            OrdenDelTurno pisada = null;
            for (OrdenDelTurno o : c.ordenesDelTurno) {
                if (o.pisada()) {
                    pisada = o;
                    break;
                }
            }
            cierto(pisada != null, "hay una orden marcada como pisada");
            igual("alVerEnemigo", pisada.metodo, "método pisado");
            cierto(pisada.accion.startsWith("apuntarHacia("), "acción pisada: " + pisada.accion);
            igual(5, pisada.linea, "línea de apuntarHacia");
            igual("cadaTurno", pisada.pisadaPor, "pisado por cadaTurno");
            igual(11, pisada.lineaPisada, "línea exacta de apuntar(10) en cadaTurno");
        });
    }

    private static void apuntarSinFalsosPositivos() {
        caso("depurador: sin falsos positivos cuando alVerEnemigo y cadaTurno no comparten canal", () -> {
            String codigo = "import metajavbots.api.*;\n"
                    + "public class BotLimpio extends Metajavbot {\n"
                    + "    @Override\n"
                    + "    public void alVerEnemigo(Enemigo e) {\n"
                    + "        apuntarHacia(e.angulo());\n"
                    + "        atacar();\n"
                    + "    }\n"
                    + "    @Override\n"
                    + "    public void cadaTurno() {\n"
                    + "        avanzar(30);\n"
                    + "        girar(10);\n"
                    + "        if (enemigosVistos().isEmpty()) {\n"
                    + "            apuntar(10);\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n";
            Pelea p = peleaCon(codigo, 13);
            Cuerpo c = p.cuerpo(0);
            boolean turnoConAmbos = false;
            for (int i = 0; i < 200 && p.estado() == Pelea.Estado.LUCHANDO; i++) {
                p.avanzar();
                falso(c.tieneConflictoDeOrdenes, "no debe haber falsos positivos en turno " + p.turno());
                for (OrdenDelTurno o : c.ordenesDelTurno) falso(o.pisada(), "ninguna orden pisada: " + o.accion);
                boolean vioEnemigo = false, hizoTurno = false;
                for (OrdenDelTurno o : c.ordenesDelTurno) {
                    if ("alVerEnemigo".equals(o.metodo)) vioEnemigo = true;
                    if ("cadaTurno".equals(o.metodo)) hizoTurno = true;
                }
                if (vioEnemigo && hizoTurno) turnoConAmbos = true;
            }
            cierto(turnoConAmbos, "hubo turnos con órdenes de alVerEnemigo y cadaTurno a la vez sin falsos positivos");
        });
    }

    private static void apuntarDetenerYTextos() {
        caso("depurador: detener() en cadaTurno pisa avanzar y girar de un evento anterior", () -> {
            String codigo = "import metajavbots.api.*;\n"
                    + "public class BotDetener extends Metajavbot {\n"
                    + "    @Override\n"
                    + "    public void alVerEnemigo(Enemigo e) {\n"
                    + "        avanzar(80);\n"
                    + "        girar(25);\n"
                    + "    }\n"
                    + "    @Override\n"
                    + "    public void cadaTurno() {\n"
                    + "        if (!enemigosVistos().isEmpty()) {\n"
                    + "            detener();\n"
                    + "        } else {\n"
                    + "            avanzar(40);\n"
                    + "            apuntar(12);\n"
                    + "        }\n"
                    + "    }\n"
                    + "}\n";
            Pelea p = peleaCon(codigo, 17);
            Cuerpo c = p.cuerpo(0);
            boolean detectado = false;
            for (int i = 0; i < 400 && p.estado() == Pelea.Estado.LUCHANDO; i++) {
                p.avanzar();
                if (c.tieneConflictoDeOrdenes) {
                    int pisadas = 0;
                    for (OrdenDelTurno o : c.ordenesDelTurno) {
                        if (o.pisada() && "cadaTurno".equals(o.pisadaPor) && o.lineaPisada == 11) pisadas++;
                    }
                    igual(2, pisadas, "detener() pisa tanto avanzar como girar");
                    detectado = true;
                    break;
                }
            }
            cierto(detectado, "detener() en cadaTurno detectado como conflicto");
        });

        caso("depurador: textos nuevos en es, de y en sin claves faltantes", () -> {
            String[] claves = {
                    "pelea.depurador_vars", "pelea.depurador_codigo", "pelea.depurador_traza",
                    "pelea.depurador_ordenes", "pelea.depurador_sin_ordenes", "pelea.depurador_sin_traza",
                    "pelea.depurador_pisado_linea", "pelea.depurador_pisado"};
            Idioma antes = T.idioma;
            try {
                for (Idioma id : Idioma.values()) {
                    T.idioma = id;
                    for (String k : claves) {
                        String v = T.t(k);
                        cierto(!v.equals(k) && !v.isBlank(), "falta " + k + " en " + id);
                    }
                }
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static Pelea peleaCon(String codigo, long semilla) {
        RobotDelJugador rj = new RobotDelJugador();
        Compilador.Resultado res = rj.compilar(codigo, Locale.ROOT, true);
        cierto(res.bien, "compila con traza: " + res.errores);
        List<Modelo> ms = Modelo.todos();
        Combatiente a = new Combatiente("Jugador", new Montaje(ms.get(0)), rj);
        Combatiente b = new Combatiente("Muneco", new Montaje(ms.get(1)), Muneco::new);
        Pelea p = new Pelea(a, b, semilla);
        p.rondas = 1;
        p.empezarRonda();
        return p;
    }
}
