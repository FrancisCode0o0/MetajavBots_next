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
import metajavbots.pantallas.PanelDepurador;
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
        apuntarLimpiezaDeEstados();
        apuntarIndicadorSinEjecucion();
        apuntarLimpiezaDeEventosBloqueados();
        apuntarTurnoMostrado();
        apuntarFormatoYDisposicion();
    }

    private static void apuntarLimpiezaDeEstados() {
        caso("depurador: aturdido limpia la telemetría del turno", () -> {
            Pelea p = peleaCon("import metajavbots.api.*;\n"
                    + "public class BotAturdido extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { avanzar(40); }\n"
                    + "}\n", 19);
            Cuerpo c = p.cuerpo(0);
            p.avanzar();
            cierto(c.pasosDelTurno > 0, "el primer turno ejecuta código");
            c.aturdido = 1;
            p.avanzar();
            igual(0L, c.pasosDelTurno, "pasos limpiados");
            cierto(c.lineasDelTurno.isEmpty(), "líneas limpias");
            cierto(c.ordenesDelTurno.isEmpty(), "órdenes limpias");
            falso(c.tieneConflictoDeOrdenes, "conflicto limpio");
            igual(Cuerpo.EstadoPensamiento.ATURDIDO, c.estadoPensamiento, "estado aturdido");
        });

        caso("depurador: deuda de tope limpia la telemetría del turno", () -> {
            Pelea p = peleaCon("import metajavbots.api.*;\n"
                    + "public class BotDeuda extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { avanzar(20); }\n"
                    + "}\n", 23);
            Cuerpo c = p.cuerpo(0);
            p.avanzar();
            c.deuda = 1;
            p.avanzar();
            igual(0L, c.pasosDelTurno, "pasos limpiados por deuda");
            cierto(c.lineasDelTurno.isEmpty(), "líneas limpias por deuda");
            cierto(c.ordenesDelTurno.isEmpty(), "órdenes limpias por deuda");
            falso(c.tieneConflictoDeOrdenes, "conflicto limpio por deuda");
            igual(Cuerpo.EstadoPensamiento.DEUDA_TOPE, c.estadoPensamiento, "estado de deuda");
        });

        caso("depurador: fuera de combate limpia la telemetría del turno", () -> {
            Pelea p = peleaCon("import metajavbots.api.*;\n"
                    + "public class BotFuera extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { avanzar(20); }\n"
                    + "}\n", 29);
            Cuerpo c = p.cuerpo(0);
            p.avanzar();
            c.fuera = true;
            p.avanzar();
            igual(0L, c.pasosDelTurno, "pasos limpiados al salir");
            cierto(c.lineasDelTurno.isEmpty(), "líneas limpias al salir");
            cierto(c.ordenesDelTurno.isEmpty(), "órdenes limpias al salir");
            falso(c.tieneConflictoDeOrdenes, "conflicto limpio al salir");
            igual(Cuerpo.EstadoPensamiento.FUERA_DE_COMBATE, c.estadoPensamiento, "estado fuera de combate");
        });
    }

    private static void apuntarIndicadorSinEjecucion() {
        caso("depurador: indicador sin ejecución para aturdimiento, deuda, fuera de combate y sin código", () -> {
            falso(PanelDepurador.sinEjecucion(null), "cuerpo nulo no marca sin ejecución");
            igual("", PanelDepurador.tituloSinEjecucion(null), "título vacío con cuerpo nulo");
            igual("", PanelDepurador.motivoSinEjecucion(null), "motivo vacío con cuerpo nulo");

            Pelea p = peleaCon("import metajavbots.api.*;\n"
                    + "public class BotEstados extends Metajavbot {\n"
                    + "    @Override public void cadaTurno() { avanzar(20); }\n"
                    + "}\n", 37);
            Cuerpo c = p.cuerpo(0);
            p.avanzar();
            falso(PanelDepurador.sinEjecucion(c), "turno normal ejecutado");
            igual("", PanelDepurador.tituloSinEjecucion(c), "sin título en turno normal");
            igual("", PanelDepurador.motivoSinEjecucion(c), "sin motivo en turno normal");

            c.aturdido = 3;
            p.avanzar();
            cierto(PanelDepurador.sinEjecucion(c), "aturdido marca sin ejecución");
            igual(2, c.turnosSinEjecucionRestantes(), "quedan 2 turnos de aturdimiento");
            igual(T.f("pelea.depurador_sin_ejecucion_turnos", 2), PanelDepurador.tituloSinEjecucion(c), "título aturdido con turnos");
            igual(T.t("pelea.estado_aturdido"), PanelDepurador.motivoSinEjecucion(c), "motivo aturdido");
            p.avanzar();
            p.avanzar();
            igual(0, c.turnosSinEjecucionRestantes(), "último turno de aturdimiento sin turnos restantes");
            igual(T.t("pelea.depurador_sin_ejecucion"), PanelDepurador.tituloSinEjecucion(c), "título aturdido último turno");

            c.deuda = 2;
            p.avanzar();
            cierto(PanelDepurador.sinEjecucion(c), "deuda marca sin ejecución");
            igual(1, c.turnosSinEjecucionRestantes(), "queda 1 turno de deuda");
            igual(T.f("pelea.depurador_sin_ejecucion_turnos", 1), PanelDepurador.tituloSinEjecucion(c), "título deuda con turnos");
            igual(T.t("pelea.estado_deuda_tope"), PanelDepurador.motivoSinEjecucion(c), "motivo deuda de tope");
            p.avanzar();
            p.avanzar();
            falso(PanelDepurador.sinEjecucion(c), "recupera ejecución normal tras agotar la deuda");

            c.fuera = true;
            p.avanzar();
            cierto(PanelDepurador.sinEjecucion(c), "fuera de combate marca sin ejecución");
            igual(0, c.turnosSinEjecucionRestantes(), "fuera de combate no tiene contador de turnos");
            igual(T.t("pelea.depurador_sin_ejecucion"), PanelDepurador.tituloSinEjecucion(c), "título fuera de combate");
            igual(T.t("pelea.estado_fuera"), PanelDepurador.motivoSinEjecucion(c), "motivo fuera de combate");

            List<Modelo> ms = Modelo.todos();
            Pelea sinCodigo = new Pelea(new Combatiente("SinCodigo", new Montaje(ms.get(0)), null),
                    new Combatiente("Muneco", new Montaje(ms.get(1)), Muneco::new), 41);
            sinCodigo.rondas = 1;
            sinCodigo.empezarRonda();
            Cuerpo cSin = sinCodigo.cuerpo(0);
            igual(Cuerpo.EstadoPensamiento.SIN_CODIGO, cSin.estadoPensamiento, "sin código ya al iniciar la ronda");
            cierto(PanelDepurador.sinEjecucion(cSin), "sin código marca sin ejecución");
            igual(T.t("pelea.estado_sin_codigo"), PanelDepurador.motivoSinEjecucion(cSin), "motivo sin código");
            sinCodigo.avanzar();
            igual(Cuerpo.EstadoPensamiento.SIN_CODIGO, cSin.estadoPensamiento, "sin código tras avanzar");
        });
    }

    private static void apuntarTurnoMostrado() {
        caso("depurador: el turno mostrado coincide con el visto por el robot tras el primer tick", () -> {
            Pelea p = peleaCon("import metajavbots.api.*;\n"
                    + "public class BotTurno extends Metajavbot {\n"
                    + "    public int turnoVisto;\n"
                    + "    @Override public void cadaTurno() { turnoVisto = turnoActual(); }\n"
                    + "}\n", 31);
            Cuerpo c = p.cuerpo(0);
            igual(0, c.turno(), "turno inicial");
            p.avanzar();
            igual(0, c.turno(), "turno mostrado tras el primer tick");
            igual(0, c.turnoPensado, "turno ejecutado registrado");
        });
    }

    private static void apuntarFormatoYDisposicion() {
        caso("depurador: la disposición de variables evita solapamientos y recortes en nombres y valores largos", () -> {
            float anchoInterior = PanelDepurador.anchoTextoVariables(444f);
            cierto(anchoInterior > 300f && anchoInterior <= 444f - 28f, "ancho útil deja margen para la barra");
            PanelDepurador.DisposicionVariable corta = PanelDepurador.disposicionVariable(anchoInterior, 90f);
            falso(corta.apilada(), "nombre corto usa dos columnas");
            cierto(corta.xValor() > corta.anchoNombre(), "sin solapamiento entre nombre y valor");
            cierto(corta.xValor() + corta.anchoValor() <= anchoInterior + 0.01f, "valor dentro del ancho útil");

            PanelDepurador.DisposicionVariable larga = PanelDepurador.disposicionVariable(anchoInterior, 280f);
            cierto(larga.apilada(), "nombre largo se apila en lugar de recortarse");
            igual(anchoInterior, larga.anchoNombre(), "nombre apilado aprovecha todo el ancho");
            cierto(larga.xValor() + larga.anchoValor() <= anchoInterior + 0.01f, "valor apilado dentro del ancho útil");

            PanelDepurador.DisposicionVariable estrecha = PanelDepurador.disposicionVariable(180f, 60f);
            cierto(estrecha.apilada(), "panel estrecho apila para mantener legibilidad");
            cierto(PanelDepurador.anchoTextoConsola(444f) <= 444f - 28f, "consola respeta márgenes y barra");
        });

        caso("depurador: formatea valores largos con indicador explícito y normaliza saltos y tabuladores", () -> {
            String largo = "x".repeat(PanelDepurador.MAX_LARGO_VALOR + 45);
            String fmt = PanelDepurador.formatearValor(largo);
            igual(PanelDepurador.MAX_LARGO_VALOR + 1, fmt.length(), "longitud truncada exacta");
            cierto(fmt.endsWith("…"), "indicador explícito de truncado");
            igual("a\nb    c", PanelDepurador.formatearValor("a\r\nb\tc"), "normaliza CRLF y tabulador");
            igual("null", PanelDepurador.formatearValor(null), "null legible");
            igual("3.14", PanelDepurador.formatearValor(3.14159), "decimales formateados");
        });
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
                    "pelea.depurador_pisado_linea", "pelea.depurador_pisado",
                    "pelea.depurador_sin_ejecucion", "pelea.depurador_sin_ejecucion_turnos",
                    "pelea.estado_aturdido", "pelea.estado_deuda_tope",
                    "pelea.estado_fuera", "pelea.estado_sin_codigo"};
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

    private static void apuntarLimpiezaDeEventosBloqueados() {
        caso("depurador: aturdido descarta pared y aviso pendientes", () -> {
            Modelo modelo = Modelo.de("vigia");
            Pelea p = new Pelea(new Combatiente[]{
                    PruebasDeLaPelea.con("registro", modelo, PruebasDeLaPelea.de(RegistradorDeEventos::new)),
                    PruebasDeLaPelea.con("enemigo", modelo, PruebasDeLaPelea.de(PruebasDeLaPelea.Sencillo::new))
            }, new int[]{0, 1}, 73);
            p.empezarRonda();
            Cuerpo receptor = p.cuerpo(0);
            campoReasignable(receptor, "chocoConPared", true);
            java.util.List<String[]> avisos = listaCampo(receptor, "avisos");
            avisos.add(new String[]{"equipo", "mensaje obsoleto"});
            receptor.aturdido = 1;
            p.avanzar();
            p.avanzar();
            cierto(!receptor.combatiente.memoria.containsKey("pared"), "la pared bloqueada no se entregó");
            cierto(!receptor.combatiente.memoria.containsKey("avisos"), "el aviso bloqueado no se entregó");
        });

        caso("depurador: deuda descarta todos los eventos pendientes", () -> {
            Modelo modelo = Modelo.de("vigia");
            Pelea p = new Pelea(new Combatiente[]{
                    PruebasDeLaPelea.con("registro", modelo, PruebasDeLaPelea.de(RegistradorDeEventos::new)),
                    PruebasDeLaPelea.con("enemigo", modelo, PruebasDeLaPelea.de(PruebasDeLaPelea.Sencillo::new))
            }, new int[]{0, 1}, 74);
            p.empezarRonda();
            Cuerpo receptor = p.cuerpo(0);
            java.util.List<metajavbots.api.Impacto> golpes =
                    listaCampo(receptor, "golpesRecibidos");
            golpes.add(null);
            java.util.List<metajavbots.api.Impacto> aciertos = listaCampo(receptor, "aciertos");
            aciertos.add(null);
            campoReasignable(receptor, "chocoConPared", true);
            metajavbots.api.Avistado choque = (metajavbots.api.Avistado) java.lang.reflect.Proxy.newProxyInstance(
                    metajavbots.api.Avistado.class.getClassLoader(),
                    new Class<?>[]{metajavbots.api.Avistado.class},
                    (proxy, metodo, argumentos) -> valorPredeterminado(metodo.getReturnType()));
            campoReasignable(receptor, "chocoCon", choque);
            java.util.List<String[]> avisos = listaCampo(receptor, "avisos");
            avisos.add(new String[]{"equipo", "mensaje obsoleto"});
            receptor.deuda = 1;
            p.avanzar();
            p.avanzar();
            cierto(!receptor.combatiente.memoria.containsKey("golpes"), "los golpes bloqueados no se entregaron");
            cierto(!receptor.combatiente.memoria.containsKey("aciertos"), "los aciertos bloqueados no se entregaron");
            cierto(!receptor.combatiente.memoria.containsKey("pared"), "la pared bloqueada no se entregó");
            cierto(!receptor.combatiente.memoria.containsKey("choques"), "el choque bloqueado no se entregó");
            cierto(!receptor.combatiente.memoria.containsKey("avisos"), "el aviso bloqueado no se entregó");
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> java.util.List<T> listaCampo(Object objeto, String nombre) throws Exception {
        java.lang.reflect.Field campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        return (java.util.List<T>) campo.get(objeto);
    }

    private static void campoReasignable(Object objeto, String nombre, Object valor) throws Exception {
        java.lang.reflect.Field campo = objeto.getClass().getDeclaredField(nombre);
        campo.setAccessible(true);
        campo.set(objeto, valor);
    }

    private static Object valorPredeterminado(Class<?> tipo) {
        if (tipo == boolean.class) return false;
        if (tipo == byte.class) return (byte) 0;
        if (tipo == short.class) return (short) 0;
        if (tipo == int.class) return 0;
        if (tipo == long.class) return 0L;
        if (tipo == float.class) return 0.0f;
        if (tipo == double.class) return 0.0d;
        if (tipo == char.class) return '\0';
        return null;
    }

    public static class RegistradorDeEventos extends metajavbots.api.Metajavbot {
        @Override
        public void alRecibirGolpe(metajavbots.api.Golpe g) { contar("golpes"); }

        @Override
        public void alAcertar(metajavbots.api.Golpe g) { contar("aciertos"); }

        @Override
        public void alChocarConPared() { contar("pared"); }

        @Override
        public void alChocarConEnemigo(metajavbots.api.Enemigo e) { contar("choques"); }

        @Override
        public void alOirAlEquipo(String de, String mensaje) { contar("avisos"); }

        private void contar(String clave) {
            int valor = memoria().get(clave) instanceof Integer ? (int) memoria().get(clave) : 0;
            memoria().put(clave, valor + 1);
        }
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
