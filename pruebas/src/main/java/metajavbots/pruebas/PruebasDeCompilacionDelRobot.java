package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.RobotDelJugador;
import metajavbots.combate.Combatiente;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.Pelea;
import metajavbots.pantalla.Ui;
import metajavbots.pantallas.AvisoDeCompilacion;
import metajavbots.pantallas.Comun;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.pantallas.PanelDepurador;
import metajavbots.progreso.Guardado;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.Entrenador;

/**
 * PRUEBAS DE COMPILACIÓN DEL ROBOT — Verifica en ES, DE y EN la regla unificada:
 * robot nunca compilado permanece quieto sin actuar, robot editado sin compilar o
 * con error ejecuta la última versión válida, al recompilar ejecuta la nueva,
 * persistencia en guardado/carga y paridad trilingüe en UI, avisos y entrenador.
 */
public final class PruebasDeCompilacionDelRobot {

    private PruebasDeCompilacionDelRobot() {
    }

    /** Registra los casos de verificación de compilación del robot del jugador. */
    public static void apuntar() {
        caso("compilación robot: nunca compilado no se mueve ni actúa en ES, DE y EN",
                PruebasDeCompilacionDelRobot::probarNuncaCompilado);
        caso("compilación robot: editado sin compilar o con error ejecuta la última versión válida en ES, DE y EN",
                PruebasDeCompilacionDelRobot::probarEditadoSinCompilarOConError);
        caso("compilación robot: al compilar versión nueva pasa a ejecutarla en ES, DE y EN",
                PruebasDeCompilacionDelRobot::probarNuevaVersionValida);
        caso("compilación robot: persistencia de estado y último código válido en guardado/carga",
                PruebasDeCompilacionDelRobot::probarPersistencia);
        caso("compilación robot: paridad trilingüe en ui.txt, avisos del entrenador y conocimiento offline/IA",
                PruebasDeCompilacionDelRobot::probarParidadTrilingueYEntrenador);
    }

    private static void probarNuncaCompilado() {
        Idioma previo = T.idioma;
        try {
            for (Idioma idioma : Idioma.values()) {
                T.idioma = idioma;
                Jugador jug = jugadorNuevo("SinCompilar_" + idioma.name(), idioma);
                Jugador.Robot r = jug.robot();
                falso(r.tieneCodigoCompilado(), idioma + ": robot nuevo no debe estar compilado");
                RobotDelJugador fabrica = new RobotDelJugador();
                RobotDelJugador.EstadoDeCarga estado = fabrica.cargarDesdeJugador(r, idioma.locale(), true);
                igual(RobotDelJugador.EstadoDeCarga.SIN_COMPILAR, estado, idioma + ": estado sin compilar");
                falso(fabrica.listo(), idioma + ": fábrica no debe estar lista");
                cierto(fabrica.fabricaSiListo() == null, idioma + ": fabricaSiListo debe ser null");

                Combatiente yo = new Combatiente(r.nombre, jug.montajeDe(r), fabrica.fabricaSiListo());
                Pelea pelea = new Pelea(yo, Rivales.muneco(), 42L);
                pelea.rondas = 1;
                pelea.empezarRonda();
                Cuerpo c0 = pelea.cuerpo(0);
                double x0 = c0.x, y0 = c0.y, rumbo0 = c0.rumbo(), mira0 = c0.mira();
                for (int i = 0; i < 30; i++) pelea.avanzar();

                cierto(Math.hypot(c0.x - x0, c0.y - y0) < 1e-6, idioma + ": no debe moverse sin compilar");
                cierto(Math.abs(c0.rumbo() - rumbo0) < 1e-6, idioma + ": no debe girar sin compilar");
                cierto(Math.abs(c0.mira() - mira0) < 1e-6, idioma + ": no debe mover mira sin compilar");
                igual(Cuerpo.EstadoPensamiento.SIN_CODIGO, c0.estadoPensamiento, idioma + ": estado SIN_CODIGO");
                cierto(PanelDepurador.sinEjecucion(c0), idioma + ": depurador debe indicar sin ejecución");
                igual(T.en("pelea.estado_sin_codigo", idioma), PanelDepurador.motivoSinEjecucion(c0),
                        idioma + ": motivo del depurador");
                cierto(c0.ordenesDelTurno.isEmpty(), idioma + ": sin órdenes ejecutadas");
                igual(0, pelea.informe(0).ataques, idioma + ": sin ataques");
                cierto(yo.consola.isEmpty(), idioma + ": sin salida en consola");
            }
        } finally {
            T.idioma = previo;
        }
    }

    private static void probarEditadoSinCompilarOConError() {
        if (!Compilador.hayCompilador()) return;
        for (Idioma idioma : Idioma.values()) {
            Jugador jug = jugadorNuevo("Editado_" + idioma.name(), idioma);
            Jugador.Robot r = jug.robot();
            String v1 = codigoRobot(idioma, "V1_MARCA", 120);
            String v2 = codigoRobot(idioma, "V2_NO_COMPILADA", 20);
            String roto = codigoRoto(idioma);

            RobotDelJugador compiladorInicial = new RobotDelJugador();
            Compilador.Resultado res1 = compiladorInicial.compilar(v1, idioma.locale(), true);
            cierto(res1.bien, idioma + ": v1 debe compilar");
            r.codigo = v1;
            r.registrarCompilacionValida(v1);

            // Editar sin volver a compilar.
            r.codigo = v2;
            cierto(r.tieneCodigoCompilado(), idioma + ": mantiene compilación previa");
            cierto(r.tieneCambiosPendientes(), idioma + ": detecta cambios pendientes");

            RobotDelJugador fabrica = new RobotDelJugador();
            RobotDelJugador.EstadoDeCarga estado = fabrica.cargarDesdeJugador(r, idioma.locale(), true);
            igual(RobotDelJugador.EstadoDeCarga.ULTIMA_VERSION_VALIDA, estado, idioma + ": usa última válida");
            Combatiente yo = ejecutarPeleaCorta(jug, r, fabrica);
            cierto(contieneConsola(yo, "V1_MARCA"), idioma + ": debe ejecutar v1");
            falso(contieneConsola(yo, "V2_NO_COMPILADA"), idioma + ": no debe ejecutar v2 sin compilar");

            // Editar con error de sintaxis e intentar compilar: sigue manteniendo v1.
            r.codigo = roto;
            Compilador.Resultado resRoto = fabrica.compilar(roto, idioma.locale(), true);
            falso(resRoto.bien, idioma + ": el código roto no debe compilar");
            cierto(fabrica.listo(), idioma + ": la fábrica mantiene v1 en memoria");

            RobotDelJugador fabricaRecargada = new RobotDelJugador();
            igual(RobotDelJugador.EstadoDeCarga.ULTIMA_VERSION_VALIDA,
                    fabricaRecargada.cargarDesdeJugador(r, idioma.locale(), true),
                    idioma + ": tras error sigue cargando última versión válida");
            Combatiente yo2 = ejecutarPeleaCorta(jug, r, fabricaRecargada);
            cierto(contieneConsola(yo2, "V1_MARCA"), idioma + ": tras error sigue ejecutando v1");
        }
    }

    private static void probarNuevaVersionValida() {
        if (!Compilador.hayCompilador()) return;
        for (Idioma idioma : Idioma.values()) {
            Jugador jug = jugadorNuevo("VersionNueva_" + idioma.name(), idioma);
            Jugador.Robot r = jug.robot();
            String v1 = codigoRobot(idioma, "VERSION_UNO", 80);
            String v2 = codigoRobot(idioma, "VERSION_DOS", 150);

            RobotDelJugador fabrica = new RobotDelJugador();
            cierto(fabrica.compilar(v1, idioma.locale(), true).bien, idioma + ": compila v1");
            r.codigo = v1;
            r.registrarCompilacionValida(v1);

            Combatiente yo1 = ejecutarPeleaCorta(jug, r, fabrica);
            cierto(contieneConsola(yo1, "VERSION_UNO"), idioma + ": ejecuta v1");

            // Compilar nueva versión válida.
            r.codigo = v2;
            cierto(fabrica.compilar(v2, idioma.locale(), true).bien, idioma + ": compila v2");
            r.registrarCompilacionValida(v2);
            falso(r.tieneCambiosPendientes(), idioma + ": sin cambios pendientes tras compilar");

            RobotDelJugador fabrica2 = new RobotDelJugador();
            igual(RobotDelJugador.EstadoDeCarga.COMPILADO_ACTUAL,
                    fabrica2.cargarDesdeJugador(r, idioma.locale(), true), idioma + ": carga versión actual");
            Combatiente yo2 = ejecutarPeleaCorta(jug, r, fabrica2);
            cierto(contieneConsola(yo2, "VERSION_DOS"), idioma + ": ejecuta v2");
            falso(contieneConsola(yo2, "VERSION_UNO"), idioma + ": ya no ejecuta v1");
        }
    }

    private static void probarPersistencia() throws IOException {
        Path previa = Guardado.carpeta;
        Path temp = Files.createTempDirectory("metajavbots_comp_test_");
        Guardado.carpeta = temp;
        try {
            // 1) Nunca compilado.
            Jugador nuevo = jugadorNuevo("PilotoNuevo", Idioma.ES);
            Guardado.guardar(nuevo);
            Jugador leidoNuevo = Guardado.leer(Guardado.carpetaDe(nuevo.nombre));
            falso(leidoNuevo.robot().compiladoAlgunaVez, "persistencia: robot nuevo sigue sin compilar");
            falso(leidoNuevo.robot().tieneCodigoCompilado(), "persistencia: sin código compilado");
            igual("", leidoNuevo.robot().codigoCompilado, "persistencia: codigoCompilado vacío");
            falso(leidoNuevo.avisoCompilarVisto, "persistencia: avisoCompilarVisto en false");

            // 2) Compilado v1 y luego editado a v2 sin compilar.
            String v1 = codigoRobot(Idioma.DE, "PERSIST_V1", 90);
            String v2 = codigoRobot(Idioma.DE, "PERSIST_V2", 30);
            Jugador editado = jugadorNuevo("PilotoEditado", Idioma.DE);
            editado.robot().registrarCompilacionValida(v1);
            editado.robot().codigo = v2;
            editado.avisoCompilarVisto = true;
            Guardado.guardar(editado);
            Jugador leidoEditado = Guardado.leer(Guardado.carpetaDe(editado.nombre));
            cierto(leidoEditado.robot().tieneCodigoCompilado(), "persistencia: conserva compilación previa");
            cierto(leidoEditado.robot().tieneCambiosPendientes(), "persistencia: conserva marca de cambios pendientes");
            igual(v1, leidoEditado.robot().codigoCompilado, "persistencia: recupera último código compilado");
            igual(v2, leidoEditado.robot().codigo, "persistencia: recupera borrador del editor");
            cierto(leidoEditado.avisoCompilarVisto, "persistencia: conserva avisoCompilarVisto");

            if (Compilador.hayCompilador()) {
                RobotDelJugador fabrica = new RobotDelJugador();
                igual(RobotDelJugador.EstadoDeCarga.ULTIMA_VERSION_VALIDA,
                        fabrica.cargarDesdeJugador(leidoEditado.robot(), Idioma.DE.locale(), false),
                        "persistencia: recarga última versión válida");
                Combatiente yo = ejecutarPeleaCorta(leidoEditado, leidoEditado.robot(), fabrica);
                cierto(contieneConsola(yo, "PERSIST_V1"), "persistencia: ejecuta v1 tras recargar");
                falso(contieneConsola(yo, "PERSIST_V2"), "persistencia: no ejecuta borrador sin compilar");
            }

            // 3) Compatibilidad con partida antigua (sin clave robot.0.compilado).
            Path dirAntigua = temp.resolve("Antigua");
            Files.createDirectories(dirAntigua);
            Files.writeString(dirAntigua.resolve("partida.txt"),
                    "nombre = Antigua\nidioma = en\nexperiencia = 120\nrobots = 1\nrobot.0.nombre = OldBot\nrobot.0.modelo = cobalto\n",
                    StandardCharsets.UTF_8);
            String codigoAntiguo = codigoRobot(Idioma.EN, "OLD_OK", 70);
            Files.writeString(dirAntigua.resolve("robot_1.java"), codigoAntiguo, StandardCharsets.UTF_8);
            Jugador antigua = Guardado.leer("Antigua");
            cierto(antigua.robot().tieneCodigoCompilado(), "compatibilidad: partida antigua con progreso conserva compilación");
            igual(codigoAntiguo, antigua.robot().codigoCompilado, "compatibilidad: código compilado migrado");
        } finally {
            Guardado.carpeta = previa;
            borrarRecursivo(temp);
        }
    }

    private static void probarParidadTrilingueYEntrenador() {
        cierto(Ui.Estilo.COMPILAR != null, "existe estilo COMPILAR");
        cierto(Ui.COMPILAR_FONDO != Ui.COMPILAR_TEXTO, "contraste en botón compilar");

        String[] claves = {
            "pelea.depurador_sin_traza", "pelea.codigo_no_compila", "pelea.sin_compilar",
            "pelea.usando_ultima_compilada", "pelea.errores_ultima_valida",
            "taller.aviso_nunca_compilado", "taller.aviso_cambios_sin_compilar", "taller.sin_jdk",
            "entrenador.aviso_sin_compilar_titulo", "entrenador.aviso_sin_compilar_menu",
            "entrenador.aviso_sin_compilar_pelea", "entrenador.ir_al_taller",
            "entrenador.pelear_sin_compilar", "comun.entendido", "carrera_obs.iniciar_ayuda"
        };
        for (String clave : claves) {
            String es = T.en(clave, Idioma.ES);
            String de = T.en(clave, Idioma.DE);
            String en = T.en(clave, Idioma.EN);
            cierto(!es.isBlank() && !es.equals(clave), clave + " faltante en ES");
            cierto(!de.isBlank() && !de.equals(clave), clave + " faltante en DE");
            cierto(!en.isBlank() && !en.equals(clave), clave + " faltante en EN");
            cierto(!es.equals(de) && !es.equals(en) && !de.equals(en), clave + " debe tener traducción distinta en ES/DE/EN");
            for (String txt : new String[]{es, de, en}) {
                String low = txt.toLowerCase(Locale.ROOT);
                falso(low.contains("autopilot") || low.contains("piloto automático"),
                        clave + " no debe mencionar piloto automático: " + txt);
            }
        }
        verificarAvisosYConocimiento();
    }

    private static void verificarAvisosYConocimiento() {
        for (Idioma idioma : Idioma.values()) {
            Jugador jug = jugadorNuevo("Entrenador_" + idioma.name(), idioma);
            jug.entrenador = "sensei";
            Entrenador en = Entrenador.de(jug.entrenador);
            cierto(AvisoDeCompilacion.debeAvisarEnMenu(jug), idioma + ": debe avisar en menú si nunca compiló");
            cierto(AvisoDeCompilacion.debeAvisarAlPelear(jug), idioma + ": debe avisar al pelear si nunca compiló");
            cierto(AvisoDeCompilacion.mensajeBienvenida(jug, idioma).contains(en.nombre.en(idioma)),
                    idioma + ": aviso menú respeta el entrenador activo");
            cierto(AvisoDeCompilacion.mensajeAntesDePelear(jug, idioma).contains(en.nombre.en(idioma)),
                    idioma + ": aviso pelea respeta el entrenador activo");
            cierto(!AvisoDeCompilacion.recordatorioEnTaller(jug.robot(), jug.robot().codigo, idioma).isBlank(),
                    idioma + ": recordatorio en taller cuando nunca se compiló");

            jug.robot().registrarCompilacionValida(jug.robot().codigo);
            falso(AvisoDeCompilacion.debeAvisarAlPelear(jug), idioma + ": no debe avisar al pelear tras compilar");
            igual("", AvisoDeCompilacion.recordatorioEnTaller(jug.robot(), jug.robot().codigo, idioma),
                    idioma + ": sin recordatorio si coincide con compilado");
            cierto(!AvisoDeCompilacion.recordatorioEnTaller(jug.robot(), jug.robot().codigo + "\n// cambio", idioma).isBlank(),
                    idioma + ": recordatorio si hay cambios pendientes");

            Concepto compilar = Concepto.de("compilar");
            Concepto taller = Concepto.de("taller_y_compilar");
            cierto(compilar != null && taller != null, "conceptos compilar y taller_y_compilar existen");
            String txtCompilar = String.join(" ", compilar.parrafos(idioma)).toLowerCase(Locale.ROOT);
            String txtTaller = String.join(" ", taller.parrafos(idioma)).toLowerCase(Locale.ROOT);
            cierto(txtCompilar.contains("rojo") || txtCompilar.contains("rote") || txtCompilar.contains("red"),
                    idioma + ": ficha compilar menciona botón rojo");
            cierto(txtTaller.contains("rojo") || txtTaller.contains("rote") || txtTaller.contains("red"),
                    idioma + ": ficha taller menciona botón rojo");
            String tipPrimera = ConocimientoDelEntrenador.textoDeConsejo("juego_compilar_primera_vez", idioma);
            String tipTaller = ConocimientoDelEntrenador.textoDeConsejo("juego_taller", idioma);
            cierto(tipPrimera != null && !tipPrimera.isBlank(), idioma + ": tip juego_compilar_primera_vez");
            cierto(tipTaller != null && !tipTaller.isBlank(), idioma + ": tip juego_taller");

            String sistemaIa = ConsultaIaEntrenador.construirSistema(jug, idioma, en);
            String contextoIa = ConsultaIaEntrenador.construirContexto(jug, null, taller, null, "", idioma);
            cierto(sistemaIa.contains("red Compile button") && sistemaIa.contains("standing still"),
                    idioma + ": sistema IA incluye la regla de compilación");
            cierto(contextoIa.contains("robotCompiled=") && contextoIa.contains("uncompiledEdits="),
                    idioma + ": contexto IA incluye estado de compilación");
        }
    }

    private static Jugador jugadorNuevo(String nombre, Idioma idioma) {
        Jugador jug = new Jugador(nombre);
        jug.idioma = idioma;
        Jugador.Robot r = new Jugador.Robot("Cobalto", "cobalto");
        r.codigo = Comun.plantilla(idioma);
        jug.robots.add(r);
        return jug;
    }

    private static Combatiente ejecutarPeleaCorta(Jugador jug, Jugador.Robot r, RobotDelJugador fabrica) {
        Combatiente yo = new Combatiente(r.nombre, jug.montajeDe(r), fabrica.fabricaSiListo());
        Pelea pelea = new Pelea(yo, Rivales.muneco(), 77L);
        pelea.rondas = 1;
        pelea.empezarRonda();
        for (int i = 0; i < 15; i++) pelea.avanzar();
        return yo;
    }

    private static boolean contieneConsola(Combatiente c, String texto) {
        for (String linea : c.consola) {
            if (linea != null && linea.contains(texto)) return true;
        }
        return false;
    }

    private static String codigoRobot(Idioma idioma, String marca, int pasos) {
        switch (idioma) {
            case DE:
                return "import metajavbots.api.*;\n"
                        + "public class MeinTestBot extends MetajavbotDe {\n"
                        + "    @Override public void beimStart() { ausgeben(\"" + marca + "\"); }\n"
                        + "    @Override public void jedenZug() { vorwaerts(" + pasos + "); }\n"
                        + "}\n";
            case EN:
                return "import metajavbots.api.*;\n"
                        + "public class MyTestBot extends MetajavbotEn {\n"
                        + "    @Override public void onStart() { print(\"" + marca + "\"); }\n"
                        + "    @Override public void onTick() { ahead(" + pasos + "); }\n"
                        + "}\n";
            default:
                return "import metajavbots.api.*;\n"
                        + "public class MiTestBot extends Metajavbot {\n"
                        + "    @Override public void alEmpezar() { escribir(\"" + marca + "\"); }\n"
                        + "    @Override public void cadaTurno() { avanzar(" + pasos + "); }\n"
                        + "}\n";
        }
    }

    private static String codigoRoto(Idioma idioma) {
        switch (idioma) {
            case DE:
                return "import metajavbots.api.*;\npublic class Kaputt extends MetajavbotDe { public void jedenZug() { vorwaerts(10) } }\n";
            case EN:
                return "import metajavbots.api.*;\npublic class Broken extends MetajavbotEn { public void onTick() { ahead(10) } }\n";
            default:
                return "import metajavbots.api.*;\npublic class Roto extends Metajavbot { public void cadaTurno() { avanzar(10) } }\n";
        }
    }

    private static void borrarRecursivo(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // limpieza de prueba
                }
            });
        }
    }
}
