package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.pantalla.Entrada;
import metajavbots.pantallas.Ajustes;
import metajavbots.pantallas.ConsultaIaEntrenador;
import metajavbots.pantallas.EntrenadorFlotante;
import metajavbots.pantallas.Inicio;
import metajavbots.pantallas.Menu;
import metajavbots.pantallas.Taller;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Registro;
import metajavbots.sonido.Timbre;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.tutor.CabezaDeEntrenador;
import metajavbots.tutor.Concepto;
import metajavbots.tutor.ConocimientoDelEntrenador;
import metajavbots.tutor.ConsejosDelJuego;
import metajavbots.tutor.ConsejosDelTaller;
import metajavbots.tutor.Entrenador;
import metajavbots.tutor.LecturaDelRival;

/**
 * El entrenador flotante: sus explicaciones de serie (res/conceptos) y cómo encuentra lo que se le pregunta,
 * los errores de javac que explica, los tips del taller, lo que lee del rival en la pelea, el hospital y las voces.
 */
final class PruebasDelEntrenador {

    private PruebasDelEntrenador() {
    }

    /** Lo que se importa al compilar un ejemplo (los ejemplos son trocitos: dan por hechos los import de siempre). */
    private static final String IMPORTS = "import java.util.*;\nimport java.util.function.*;\nimport java.util.stream.*;\nimport java.util.concurrent.*;\n"
            + "import java.util.concurrent.atomic.*;\nimport java.util.concurrent.locks.*;\nimport metajavbots.api.*;\n"
            + "import static metajavbots.junit.Assertions.*;\nimport metajavbots.junit.*;\n";

    /** Los que solo se enseñan, sin compilarse solos (necesitan otros archivos o módulos). */
    private static final Set<String> ILUSTRATIVOS = Set.of("modulos", "paquete_import");

    static void apuntar() {
        conceptos();
        busqueda();
        ayudas();
        bocadilloDeTips();
        limitesVerticalesDelBocadillo();
        esquinaInferiorDerechaYGeometria();
        cambioDeEntrenador();
        cierreConXYEnfriamiento();
        baseDeConocimientoYConsejos();
        consultaIaDelEntrenador();
    }

    private static void conceptos() {
        caso("entrenador: los conceptos, de cero a senior y el juego, en tres idiomas, con pseudocódigo, ejemplo y relacionados", () -> {
            List<Concepto> todos = Concepto.todos();
            cierto(todos.size() >= 145, "al menos 145 conceptos: " + todos.size());
            Set<String> ids = new HashSet<>();
            int[] porNivel = new int[Concepto.NIVEL_MAX + 1];
            for (Concepto c : todos) {
                cierto(ids.add(c.id), "id repetido: " + c.id);
                cierto(c.nivel >= 1 && c.nivel <= Concepto.NIVEL_MAX, c.id + ": nivel 1-" + Concepto.NIVEL_MAX);
                porNivel[c.nivel]++;
                for (Idioma i : Idioma.values()) {
                    cierto(!c.titulo.en(i).isBlank(), c.id + ": título en " + i);
                    cierto(c.parrafos(i).size() >= 2, c.id + ": explicación en " + i);
                    cierto(!c.claves(i).isEmpty(), c.id + ": claves en " + i);
                    cierto(!c.pseudo(i).isBlank(), c.id + ": pseudocódigo en " + i);
                    for (String l : c.ejemplo(i).split("\n")) {
                        int com = l.indexOf("//");
                        cierto(com < 0 || !l.substring(com).contains(" || "), c.id + ": comentario sin elegir en " + i + ": " + l);
                    }
                }
                cierto(c.guia > 0 || !c.ejemplo.isBlank(), c.id + ": ejemplo");
                cierto(c.guia <= metajavbots.pantallas.GuiaDeCerebros.PAGINAS, c.id + ": página de la guía");
            }
            for (int n = 1; n <= Concepto.NIVEL_MAX; n++) cierto(porNivel[n] >= 10, "nivel " + n + " con conceptos: " + porNivel[n]);
            for (Concepto c : todos) for (String v : c.ver) cierto(Concepto.de(v) != null, c.id + " → «ver» que no existe: " + v);
            for (Idioma i : Idioma.values()) {
                T.idioma = i;
                igual(Concepto.NIVEL_MAX, T.t("entrenador.niveles").split(";").length, "6 pestañas en " + i);
            }
            T.idioma = Idioma.ES;
            System.out.println("            " + todos.size() + " conceptos (por nivel: " + porNivel[1] + ", " + porNivel[2] + ", " + porNivel[3] + ", "
                    + porNivel[4] + ", " + porNivel[5] + ", juego=" + porNivel[6] + ")");
        });

        caso("entrenador: el ejemplo de cada concepto se ve con la API y los comentarios del idioma del jugador", () -> {
            Concepto v = Concepto.de("variable");
            cierto(v.ejemplo(Idioma.ES).contains("alRecibirGolpe") && v.ejemplo(Idioma.ES).contains("// fuera: dura toda la ronda"), "español");
            cierto(v.ejemplo(Idioma.DE).contains("beiTreffer") && v.ejemplo(Idioma.DE).contains("// außen: hält die ganze Runde"), "alemán");
            cierto(v.ejemplo(Idioma.EN).contains("onHit") && v.ejemplo(Idioma.EN).contains("// outside: lasts the whole round"), "inglés");
        });

        caso("entrenador: los ejemplos de los conceptos compilan (con los import de siempre)", () -> {
            if (!Compilador.hayCompilador()) return;
            List<String> malos = new ArrayList<>();
            for (Concepto c : Concepto.todos()) {
                if (c.ejemplo.isBlank() || ILUSTRATIVOS.contains(c.id) || soloComentarios(c.ejemplo)) continue;
                String fallo = compilarEjemplo(c.ejemplo);
                if (fallo != null) malos.add(c.id + ": " + fallo);
            }
            for (String m : malos) System.out.println("            ✗ " + m);
            igual(0, malos.size(), "ejemplos que no compilan");
        });

    }

    private static void busqueda() {
        caso("entrenador: encuentra el concepto por lo que pregunta quien no sabe nada, en cualquier idioma", () -> {
            String[][] preguntas = {
                {"¿qué es una variable?", "variable", "ES"},
                {"cómo se ejecuta un programa", "programa", "ES"},
                {"what is a class", "clase", "EN"},
                {"was ist ein Objekt", "objeto", "DE"},
                {"Vererbung", "herencia", "DE"},
                {"para qué sirve un bucle for", "for", "ES"},
                {"hilos virtuales", "hilos_virtuales", "ES"},
                {"virtual threads", "hilos_virtuales", "EN"},
                {"condición de carrera", "condicion_de_carrera", "ES"},
                {"recolector de basura", "recolector_de_basura", "ES"},
                {"? extends", "comodines", "ES"},
                {"try catch", "try_catch", "ES"},
                {"bytecode", "bytecode", "EN"},
                {"¿por qué pierdo?", "por_que_pierdo", "ES"},
                {"cómo pongo la clave de gemini", "cerebro_en_linea", "ES"},
                {"koboldcpp", "cerebro_local", "DE"},
                {"where can I see my grades", "ver_notas", "EN"},
                {"qué es una clase abstracta", "clase_abstracta", "ES"},
                {"modelo de memoria", "modelo_de_memoria", "ES"},
                {"cómo empiezo", "tutoriales_y_demos", "ES"},
                {"demostraciones", "tutoriales_y_demos", "ES"},
                {"minijuegos", "minijuegos_y_ejercicios", "ES"},
                {"tienda y monedas", "tienda_y_monedas", "ES"},
                {"depurador en vivo", "depuracion_y_testing", "ES"},
                {"mi temario", "hora_de_hoy_y_temario", "ES"},
                {"panel de la ia", "panel_ia_y_ajustes", "ES"},
                {"modo especial", "arena_y_modos", "ES"},
                {"compilar en el taller", "taller_y_compilar", "ES"},
                {"Minispiele", "minijuegos_y_ejercicios", "DE"},
                {"shop coins", "tienda_y_monedas", "EN"},
                {"testing lab", "depuracion_y_testing", "EN"},
            };
            for (String[] q : preguntas) {
                List<Concepto> l = Concepto.buscar(q[0], Idioma.valueOf(q[2]), 3);
                cierto(!l.isEmpty(), "algo para «" + q[0] + "»");
                igual(q[1], l.get(0).id, "«" + q[0] + "»");
            }
            cierto(Concepto.buscar("zzzz qqqq", Idioma.ES, 3).isEmpty(), "nada para lo que no se parece a nada");
        });

        caso("entrenador: cada error de javac explicado tiene un concepto detrás", () -> {
            igual("sentencia", Concepto.delError("compiler.err.expected").id, "falta «;»");
            igual("bloque", Concepto.delError("compiler.err.premature.eof").id, "falta «}»");
            igual("variable", Concepto.delError("compiler.err.cant.resolve.location").id, "símbolo no encontrado");
            igual("static", Concepto.delError("compiler.err.non-static.cant.be.ref").id, "static");
            igual("char", Concepto.delError("compiler.err.unclosed.char.lit").id, "char");
            igual("llamar_metodo", Concepto.delError("compiler.err.dot.class.expected").id, "no «expected» a secas");
            igual("error_de_compilacion", Concepto.delError("compiler.err.no.se.que").id, "lo desconocido");
            // Todos los de res/textos/errores.txt dan con un concepto que existe.
            try (var in = PruebasDelEntrenador.class.getResourceAsStream("/textos/errores.txt")) {
                for (String l : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                    l = l.strip();
                    if (l.startsWith("[") && l.endsWith("]")) cierto(Concepto.delError(l.substring(1, l.length() - 1)) != null, l);
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });

    }

    private static void ayudas() {
        caso("entrenador: los tips del taller ven lo que le pasa al código (y siempre tiene alguno)", () -> {
            String malo = "public class MiRobot extends Metajavbot {\n    public void cadaTurno() {\n        int vueltas = 0;\n        vueltas++;\n"
                    + "        while (true) {\n            avanzar(10);\n        }\n    }\n}\n";
            List<String> ids = ids(ConsejosDelTaller.de(malo));
            for (String id : new String[]{"contador_dentro", "while_true", "sin_radar", "sin_apuntar", "override", "sin_golpes", "paredes"})
                cierto(ids.contains(id), "ve «" + id + "»: " + ids);
            String bueno = "public class MiRobot extends Metajavbot {\n    private int vueltas;\n\n    @Override\n    public void cadaTurno() {\n"
                    + "        vueltas++;\n        avanzar(5);\n    }\n\n    @Override\n    public void alVerEnemigo(Enemigo e) {\n"
                    + "        apuntarHacia(e.angulo());\n        atacar();\n    }\n\n"
                    + "    @Override\n    public void alRecibirGolpe(Golpe g) {\n        girar(90);\n    }\n\n"
                    + "    @Override\n    public void alChocarConPared() {\n        girar(180);\n    }\n}\n";
            for (String id : ids(ConsejosDelTaller.de(bueno))) cierto(id.startsWith("general_"), "nada que señalar al bueno: " + id);
            cierto(ids(ConsejosDelTaller.de("")).contains("vacio"), "el vacío");
            cierto(ids(ConsejosDelTaller.de("class A { void f() { if (e.nombre() == \"Eclipse\") {} } }")).contains("string_igual"), "== con textos");
            // En alemán y en inglés, con su API.
            String de = "public class MeinRoboter extends MetajavbotDe {\n    @Override\n    public void jedenZug() {\n        vorwaerts(5);\n    }\n"
                    + "    @Override\n    public void beiSicht(Gegner g) {\n        zielenNach(g.winkel());\n    }\n}\n";
            List<String> idsDe = ids(ConsejosDelTaller.de(de));
            cierto(!idsDe.contains("sin_radar") && !idsDe.contains("sin_apuntar") && !idsDe.contains("override"), "API alemana: " + idsDe);
            for (ConsejosDelTaller.Tip t : ConsejosDelTaller.de(malo)) {
                cierto(T.existe("entrenador.tip." + t.id()), "texto del tip " + t.id());
                cierto(Concepto.de(t.concepto()) != null, "concepto del tip " + t.id() + ": " + t.concepto());
            }
        });

        caso("entrenador: en la pelea lee al rival marcado (rodea, se acerca, quieto, vuela) y tiene consejo para cada cosa", () -> {
            // Un rival que da vueltas a mi alrededor en contra de las agujas del reloj: me rodea por la izquierda.
            LecturaDelRival l = new LecturaDelRival();
            for (int k = 0; k < 90; k++) {
                double a = Math.toRadians(k * 2.0);
                l.observar(500, 350, 0, 500 + 200 * Math.cos(a), 350 + 200 * Math.sin(a), 4, false, 0, 0, 1000, 700);
            }
            cierto(l.lecturas().contains("rodea_izquierda"), "izquierda: " + l.lecturas());
            l.olvidar();
            for (int k = 0; k < 90; k++) {
                double a = Math.toRadians(-k * 2.0);
                l.observar(500, 350, 0, 500 + 200 * Math.cos(a), 350 + 200 * Math.sin(a), 4, false, 0, 0, 1000, 700);
            }
            cierto(l.lecturas().contains("rodea_derecha"), "derecha: " + l.lecturas());
            // Uno que se me echa encima y me pega más que yo a él.
            l.olvidar();
            for (int k = 0; k < 90; k++) l.observar(200, 350, 0, 800 - k * 5, 350, 5, false, k * 0.6, k * 0.1, 1000, 700);
            cierto(l.lecturas().contains("se_acerca") && l.lecturas().contains("te_gana"), "se acerca y gana: " + l.lecturas());
            // Uno quieto que vuela, y al que no apunto (miro al lado contrario).
            l.olvidar();
            for (int k = 0; k < 90; k++) l.observar(200, 350, 180, 500, 350, 0, true, 0, 0, 1000, 700);
            cierto(l.lecturas().containsAll(List.of("quieto", "vuela", "no_apuntas")), "quieto, vuela y no apunto: " + l.lecturas());
            cierto(new LecturaDelRival().lecturas().isEmpty(), "sin ver bastante, nada");
            for (String id : new String[]{"nada", "rodea_izquierda", "rodea_derecha", "se_acerca", "se_aleja", "lejos", "quieto", "rapido", "vuela",
                "pared", "no_apuntas", "te_gana", "le_ganas"}) {
                cierto(T.existe("entrenador.rival." + id) && T.existe("entrenador.rival." + id + ".trozo"), "consejo y trozo de " + id);
            }
        });

        caso("entrenador: cada entrenador con su voz; contra los jefes, en el hospital por causas que cambian", () -> {
            Set<Timbre> timbres = new HashSet<>();
            for (Entrenador e : Entrenador.todos()) timbres.add(e.timbre);
            igual(Entrenador.todos().size(), timbres.size(), "una voz distinta por entrenador");
            cierto(!timbres.contains(Timbre.LOCUTOR), "ninguno con la del locutor");
            igual(new Timbre(0.85, 0.9, 0.7), Timbre.de("0.85 0.9 0.7"), "se lee");
            igual(Timbre.LOCUTOR, Timbre.de("tonterías"), "lo mal escrito, el del locutor");
            float[] voz = new float[4410];
            for (int i = 0; i < voz.length; i++) voz[i] = (float) Math.sin(i * 0.1);
            cierto(metajavbots.sonido.VozPiper.robotico(voz, 0).length == voz.length, "el efecto no cambia la duración");
            Set<String> causas = new HashSet<>();
            for (long s = 0; s < 40; s++) causas.add(metajavbots.pantallas.Hospital.causa(s));
            cierto(causas.size() >= 5, "causas que cambian: " + causas.size());
            for (Idioma i : Idioma.values()) {
                T.idioma = i;
                igual(T.t("entrenador.hospital.causas").split(";").length, 10, "diez causas en " + i);
                cierto(T.t("entrenador.milagro.frases").split(";").length >= 4, "milagros en " + i);
            }
            T.idioma = Idioma.ES;
        });

        caso("entrenador: la guía con dibujos tiene todas sus páginas en los tres idiomas", () -> {
            for (Idioma i : Idioma.values()) {
                T.idioma = i;
                for (int n = 1; n <= metajavbots.pantallas.GuiaDeCerebros.PAGINAS; n++) {
                    for (String k : new String[]{"corto", "titulo", "pasos"}) cierto(T.existe("guia." + n + "." + k), "guia." + n + "." + k);
                    cierto(T.t("guia." + n + ".pasos").split("\n").length >= 3, "pasos de la página " + n + " en " + i);
                }
            }
            T.idioma = Idioma.ES;
        });
    }

    private static void bocadilloDeTips() {
        caso("entrenador: el bocadillo de tips tarda más en salir y respeta el enfriamiento al cerrarlo", () -> {
            EntrenadorFlotante ef = new EntrenadorFlotante();
            String codigo = "public class MiRobot extends Metajavbot { public void cadaTurno() { avanzar(10); } }";
            cierto(ef.tipActual() == null, "empieza sin tip");
            igual(EntrenadorFlotante.ESPERA_INICIAL_TIP, ef.esperaDelTip(), "espera inicial");
            cierto(EntrenadorFlotante.ESPERA_INICIAL_TIP == 60f, "pausa inicial prudencial de 60 s");
            cierto(EntrenadorFlotante.ESPERA_ENTRE_TIPS == 240f, "intervalo normal de 240 s (4 min)");
            cierto(EntrenadorFlotante.ESPERA_TRAS_CERRAR_MANUAL == 420f, "enfriamiento tras × de 420 s (7 min)");
            cierto(EntrenadorFlotante.ESPERA_TRAS_CERRAR_MANUAL > EntrenadorFlotante.ESPERA_ENTRE_TIPS, "cierre manual enfría más");
            cierto(EntrenadorFlotante.DURACION_VISIBLE_TIP >= 14f && EntrenadorFlotante.DURACION_VISIBLE_TIP <= 18f, "duración acotada");
            cierto(!ef.avanzarTemporizadorDeTip(codigo, 40f) && ef.tipActual() == null, "no molesta pronto");
            cierto(ef.avanzarTemporizadorDeTip(codigo, EntrenadorFlotante.ESPERA_INICIAL_TIP), "aparece al vencer la espera inicial");
            cierto(ef.tipActual() != null, "tip visible");
            // Al caducar solo, se oculta y aplica el enfriamiento entre tips.
            cierto(!ef.avanzarTemporizadorDeTip(codigo, EntrenadorFlotante.DURACION_VISIBLE_TIP + 1f), "caduca solo");
            cierto(ef.tipActual() == null, "oculto tras caducar");
            igual(EntrenadorFlotante.ESPERA_ENTRE_TIPS, ef.esperaDelTip(), "enfriamiento tras caducar");
            // tipAhora sigue disparando un tip al instante (capturas y pruebas).
            ef.tipAhora();
            cierto(ef.avanzarTemporizadorDeTip(codigo, 0.1f) && ef.tipActual() != null, "tipAhora dispara al instante");
            // Cerrar manualmente («×» o «Gracias») lo oculta al instante y respeta el enfriamiento largo.
            ef.cerrarTipManualmente();
            cierto(ef.tipActual() == null && !ef.abierto(), "cierre manual oculta sin abrir consulta");
            igual(EntrenadorFlotante.ESPERA_TRAS_CERRAR_MANUAL, ef.esperaDelTip(), "enfriamiento manual");
            cierto(!ef.avanzarTemporizadorDeTip(codigo, EntrenadorFlotante.ESPERA_ENTRE_TIPS), "no vuelve antes del enfriamiento manual");
            cierto(ef.tipActual() == null, "sigue oculto durante el enfriamiento manual");
            cierto(ef.avanzarTemporizadorDeTip(codigo, EntrenadorFlotante.ESPERA_TRAS_CERRAR_MANUAL), "vuelve al acabar el enfriamiento manual");
            cierto(ef.tipActual() != null, "nuevo tip tras enfriamiento manual");
            // Salir de una pantalla con código lo oculta y no deja la espera a cero.
            ef.avanzarTemporizadorDeTip(null, 0.1f);
            cierto(ef.tipActual() == null && ef.esperaDelTip() >= EntrenadorFlotante.ESPERA_ENTRE_TIPS, "salir del taller limpia y enfría");
        });

        caso("entrenador: el botón «×» del bocadillo no pisa el texto, consume el clic y tiene texto en tres idiomas", () -> {
            for (float[] tam : new float[][]{{1600, 900}, {1280, 720}, {1024, 768}}) {
                EntrenadorFlotante.CajaTip c = EntrenadorFlotante.cajaTip(tam[0], tam[1], 60f);
                cierto(c.contiene(c.cerrarX(), c.cerrarY()), "esquina sup-izq de × dentro del bocadillo");
                cierto(c.contiene(c.cerrarX() + c.cerrarTam() - 1f, c.cerrarY() + c.cerrarTam() - 1f), "esquina inf-der de × dentro");
                float finTexto = c.x() + EntrenadorFlotante.MARGEN_TEXTO_IZQ + c.anchoTexto();
                cierto(finTexto + 8f <= c.cerrarX(), "margen suficiente para que el texto no pise la ×");
                Entrada e = new Entrada(null);
                e.ratonX = c.cerrarX() + c.cerrarTam() / 2f;
                e.ratonY = c.cerrarY() + c.cerrarTam() / 2f;
                e.recienPulsado = true;
                cierto(c.enCerrar(e.ratonX, e.ratonY), "ratón sobre ×");
                cierto(e.clic(c.cerrarX(), c.cerrarY(), c.cerrarTam(), c.cerrarTam()), "clic en × reconocido");
                cierto(e.clicUsado, "clic en × consumido");
                cierto(!e.clic(0, 0, tam[0], tam[1]), "el clic no atraviesa hacia el panel de debajo");
            }
            for (Idioma i : Idioma.values()) {
                T.idioma = i;
                cierto(T.existe("entrenador.tip.cerrar_x") && !T.t("entrenador.tip.cerrar_x").isBlank(), "tooltip × en " + i);
            }
            T.idioma = Idioma.ES;
        });
    }

    private static void limitesVerticalesDelBocadillo() {
        caso("entrenador: el bocadillo de tips respeta límites verticales seguros con textos largos en ES, DE y EN", () -> {
            String codigoConMuchosTips = "public class MiRobot extends Metajavbot {\n"
                    + "    public int a;\n"
                    + "    public void cadaTurno() {\n"
                    + "        int n = 0; n++;\n"
                    + "        while (true) { if (\"a\" == \"b\") avanzar(10); }\n"
                    + "    }\n}\n";
            List<ConsejosDelTaller.Tip> tips = ConsejosDelTaller.de(codigoConMuchosTips);
            cierto(!tips.isEmpty(), "hay tips reales para probar");
            float[][] pantallas = {{1600, 900}, {1280, 720}, {1024, 768}, {800, 600}, {640, 480}};
            Idioma antes = T.idioma;
            try {
                for (float[] tam : pantallas) {
                    for (float altoTexto : new float[]{20f, 160f, 420f, 900f, 2000f}) {
                        EntrenadorFlotante.CajaTip c = EntrenadorFlotante.cajaTip(tam[0], tam[1], altoTexto);
                        cierto(c.y() >= EntrenadorFlotante.MARGEN_VERTICAL_TIP - 0.01f,
                                "borde superior seguro en " + tam[0] + "x" + tam[1] + " alto=" + altoTexto);
                        cierto(c.y() + c.h() <= tam[1] - EntrenadorFlotante.MARGEN_VERTICAL_TIP + 0.01f,
                                "borde inferior seguro en " + tam[0] + "x" + tam[1] + " alto=" + altoTexto);
                        cierto(c.x() >= 0f && c.x() + c.w() <= tam[0], "horizontal dentro de pantalla");
                        cierto(c.contiene(c.cerrarX(), c.cerrarY()), "× dentro del bocadillo");
                    }
                    for (Idioma id : Idioma.values()) {
                        T.idioma = id;
                        for (ConsejosDelTaller.Tip tip : tips) {
                            String txt = T.t("entrenador.tip." + tip.id());
                            EntrenadorFlotante.CajaTip base = EntrenadorFlotante.cajaTip(tam[0], tam[1], 60f);
                            int charsPorLinea = Math.max(12, (int) (base.anchoTexto() / 11f));
                            int lineas = (txt.length() + charsPorLinea - 1) / charsPorLinea;
                            EntrenadorFlotante.CajaTip c = EntrenadorFlotante.cajaTip(tam[0], tam[1], lineas * 28f);
                            cierto(c.y() >= EntrenadorFlotante.MARGEN_VERTICAL_TIP - 0.01f
                                    && c.y() + c.h() <= tam[1] - EntrenadorFlotante.MARGEN_VERTICAL_TIP + 0.01f,
                                    "tip " + tip.id() + " en " + id + " dentro de " + tam[0] + "x" + tam[1]);
                        }
                    }
                }
            } finally {
                T.idioma = antes;
            }
        });
    }

    private static void esquinaInferiorDerechaYGeometria() {
        caso("entrenador: en esquina inferior derecha recogido con flecha, desplegado y cartelito sin solapar en letra 18 y 26", () -> {
            float[][] pantallas = {{1600, 900}, {1280, 720}, {1024, 768}, {800, 600}, {640, 480}};
            for (float[] tam : pantallas) {
                for (float letra : new float[]{18f, 26f}) {
                    EntrenadorFlotante.CajaRecogida rec = EntrenadorFlotante.cajaRecogida(tam[0], tam[1], letra);
                    EntrenadorFlotante.CajaPestana reposo = EntrenadorFlotante.cajaPestana(tam[0], tam[1], 0f, letra);
                    EntrenadorFlotante.CajaPestana desplegada = EntrenadorFlotante.cajaPestana(tam[0], tam[1], 1f, letra);
                    EntrenadorFlotante.CajaTip tip = EntrenadorFlotante.cajaTip(tam[0], tam[1], 110f, letra);
                    cierto(rec.x() >= tam[0] * 0.70f && rec.x() + rec.w() <= tam[0], "recogida en borde derecho " + tam[0] + "x" + tam[1]);
                    cierto(rec.y() >= tam[1] * 0.65f && rec.y() + rec.h() <= tam[1], "recogida en esquina inferior " + tam[0] + "x" + tam[1]);
                    cierto(rec.w() < desplegada.w() && rec.h() < desplegada.h(), "recogida más compacta que desplegada");
                    cierto(rec.contiene(rec.flechaX(), rec.flechaY()), "flecha dentro de pestaña recogida");
                    float vis = rec.altoVisibleCabeza(tam[1]);
                    cierto(vis > 0f && vis < rec.altoTotalCabeza(), "cabecita recogida medio asomada (ni oculta ni entera): " + vis);
                    cierto(rec.cabezaY() - rec.radio() * 0.28f < tam[1] && rec.cabezaY() - rec.radio() * 0.28f >= rec.y(),
                            "ojos del entrenador visibles al asomar en " + tam[0] + "x" + tam[1]);
                    cierto(rec.contiene(rec.cabezaX(), rec.y() + rec.h() * 0.5f), "zona medio asomada de la cabeza clicable");
                    for (EntrenadorFlotante.CajaPestana c : new EntrenadorFlotante.CajaPestana[]{reposo, desplegada}) {
                        cierto(c.w() > 0f && c.h() > 0f && c.radio() >= 20f, "dimensiones positivas");
                        cierto(c.x() >= 0f && c.x() + c.w() <= tam[0], "horizontal dentro de " + tam[0] + "x" + tam[1]);
                        cierto(c.y() >= tam[1] * 0.60f && c.y() + c.h() <= tam[1], "anclada abajo en " + tam[0] + "x" + tam[1]);
                        cierto(c.cabezaX() - c.radio() * 1.15f >= 0f && c.cabezaX() + c.radio() * 1.15f <= tam[0],
                                "cabeza completa visible en X en " + tam[0] + "x" + tam[1]);
                        cierto(c.cabezaY() - c.radio() * 1.25f >= 0f && c.cabezaY() + c.radio() * 1.45f <= tam[1],
                                "cabeza completa visible en Y en " + tam[0] + "x" + tam[1]);
                        cierto(c.contiene(c.insigniaX(), c.insigniaY()), "insignia dentro de la pestaña");
                        cierto(c.enCabeza(c.cabezaX(), c.cabezaY()), "centro de cabeza abre consulta");
                        cierto(c.enRecoger(c.recogerX() + c.recogerW() / 2f, c.recogerY() + c.recogerH() / 2f), "franja recoger clicable");
                        cierto(!c.enCabeza(c.recogerX() + c.recogerW() / 2f, c.recogerY() + c.recogerH() / 2f), "recoger separado de cabeza");
                        Entrada e = new Entrada(null);
                        e.ratonX = c.cabezaX();
                        e.ratonY = c.cabezaY();
                        e.recienPulsado = true;
                        cierto(e.clic(c.x(), c.y(), c.w() - c.recogerW(), c.h()), "clic en cabeza reconocido");
                    }
                    cierto(desplegada.x() < reposo.x(), "al asomar se adelanta ligeramente hacia dentro");
                    cierto(tip.x() >= 0f && tip.x() + tip.w() + 10f <= desplegada.x(),
                            "cartelito pegado a la izquierda sin solapar cabecita en " + tam[0] + "x" + tam[1] + " letra=" + letra);
                    cierto(tip.y() >= 0f && tip.y() + tip.h() <= tam[1], "cartelito dentro de pantalla en Y");
                }
            }
            Jugador jug = new Jugador("Prueba");
            cierto(!EntrenadorFlotante.debeMostrarse(null, new Menu()), "sin partida no se muestra");
            cierto(!EntrenadorFlotante.debeMostrarse(jug, new Inicio()), "en la portada Inicio no se muestra");
            cierto(EntrenadorFlotante.debeMostrarse(jug, new Menu()), "en Menú sí se muestra");
            cierto(EntrenadorFlotante.debeMostrarse(jug, new Ajustes()), "en Ajustes sí se muestra");
        });
    }

    private static void cambioDeEntrenador() {
        caso("entrenador: cambiar de entrenador actualiza la cabecita al instante, limpia caché y diferencia cada retrato", () -> {
            List<Entrenador> todos = Entrenador.todos();
            cierto(todos.size() >= 7, "al menos 7 entrenadores definidos");
            Jugador jug = new Jugador("Prueba");
            EntrenadorFlotante ef = new EntrenadorFlotante();
            String codigo = "public class MiRobot extends Metajavbot { public void cadaTurno() { avanzar(10); } }";
            Set<String> firmas = new HashSet<>();
            String anteriorId = null;
            for (Entrenador e : todos) {
                if (anteriorId != null) {
                    ef.tipAhora();
                    cierto(ef.avanzarTemporizadorDeTip(codigo, 0.1f) && ef.tipActual() != null, "tip previo activo antes del cambio");
                }
                jug.entrenador = e.id;
                Entrenador activo = ef.sincronizarEntrenador(jug);
                igual(e.id, activo.id, "entrenador activo inmediato");
                igual(e.id, ef.entrenadorSincronizadoId(), "id sincronizado actualizado");
                if (anteriorId != null) {
                    cierto(ef.tipActual() == null, "al cambiar de entrenador se limpia el tip en caché de " + anteriorId);
                }
                for (String rasgo : e.rasgos) {
                    cierto(CabezaDeEntrenador.soportaRasgo(rasgo), "rasgo soportado en CabezaDeEntrenador: " + rasgo);
                }
                cierto(firmas.add(CabezaDeEntrenador.firmaVisual(e)), "aspecto visual único para " + e.id);
                CabezaDeEntrenador.dibujar(null, e, 100f, 100f, 20f, false, 0f);
                for (Idioma id : Idioma.values()) {
                    cierto(!e.nombre.en(id).isBlank() && !e.papel.en(id).isBlank() && !e.frase(id, 0).isBlank(),
                            "identidad completa de " + e.id + " en " + id);
                }
                anteriorId = e.id;
            }
            CabezaDeEntrenador.dibujar(null, null, 100f, 100f, 20f, true, 1f);
            igual(todos.size(), firmas.size(), "todos los entrenadores tienen retrato diferenciado");
            for (Idioma id : Idioma.values()) {
                T.idioma = id;
                cierto(T.existe("ajustes.entrenador_tipo") && !T.t("ajustes.entrenador_tipo").isBlank(),
                        "etiqueta de selector en Ajustes en " + id);
            }
            T.idioma = Idioma.ES;
        });
    }

    private static void cierreConXYEnfriamiento() {
        caso("entrenador: cierre con × recoge si no estaba fijado y conserva enfriamiento al cambiar de pantalla", () -> {
            Jugador principiante = new Jugador("Novato");
            EntrenadorFlotante ef = new EntrenadorFlotante();
            cierto(!ef.desplegado() && !ef.desplegadoFijo(), "empieza recogido en esquina");
            ef.tipAhora();
            cierto(ef.avanzarTemporizadorDeTip(principiante, null, 0.1f), "sale consejo en menú");
            cierto(ef.desplegado() && !ef.desplegadoFijo(), "asoma con el consejo sin quedar fijado");
            ef.cerrarTipManualmente();
            cierto(ef.tipActual() == null && !ef.desplegado(), "cerrar con × oculta el cartelito y vuelve a recoger");
            cierto(ef.esperaDelTip() >= EntrenadorFlotante.ESPERA_TRAS_CERRAR_MANUAL, "enfriamiento largo aplicado");
            cierto(!ef.avanzarTemporizadorDeTip(principiante, "class MiRobot {}", 1f), "no sale al cambiar a taller");
            cierto(ef.esperaDelTip() > EntrenadorFlotante.ESPERA_ENTRE_TIPS, "conserva el enfriamiento largo al cambiar de pantalla");
            cierto(!ef.avanzarTemporizadorDeTip(principiante, null, 1f), "tampoco sale al volver al menú");
            ef.desplegar();
            cierto(ef.desplegado() && ef.desplegadoFijo(), "desplegado manual");
            ef.tipAhora();
            cierto(ef.avanzarTemporizadorDeTip(principiante, null, 0.1f), "nuevo consejo estando fijado");
            ef.cerrarTipManualmente();
            cierto(ef.tipActual() == null && ef.desplegado(), "al cerrar × sigue desplegado si estaba fijado");
            ef.recoger();
            cierto(!ef.desplegado() && !ef.desplegadoFijo(), "recoger lo devuelve a la esquina");
            // Si el consejo expira por tiempo sin haber sido fijado, también vuelve al estado recogido medio asomado.
            ef.tipAhora();
            cierto(ef.avanzarTemporizadorDeTip(principiante, null, 0.1f) && ef.desplegado(), "sale entero al dar consejo");
            cierto(!ef.avanzarTemporizadorDeTip(principiante, null, EntrenadorFlotante.DURACION_VISIBLE_TIP + 1f), "caduca solo");
            cierto(ef.tipActual() == null && !ef.desplegado(), "al caducar vuelve al estado recogido medio asomado");
        });
    }

    private static void baseDeConocimientoYConsejos() {
        caso("entrenador: base de conocimiento centralizada alimenta buscador, IA y consejos en ES/DE/EN", () -> {
            cierto(ConocimientoDelEntrenador.entradas().size() >= 14, "entradas registradas en ConocimientoDelEntrenador");
            String guiaIa = ConocimientoDelEntrenador.resumenParaIa();
            cierto(guiaIa.contains("Tutorials") && guiaIa.contains("tactical view") && guiaIa.contains("natures"), "resumen IA automático");
            cierto(!Concepto.buscar("visor táctico", Idioma.ES, 3).isEmpty(), "buscador encuentra visor táctico");
            cierto(!Concepto.buscar("piezas y naturalezas", Idioma.ES, 3).isEmpty(), "buscador encuentra piezas y naturalezas");
            Set<String> idsJuego = new HashSet<>();
            for (ConsejosDelTaller.Tip t : ConsejosDelJuego.todosLosConsejosDeJuego()) {
                idsJuego.add(t.id());
                cierto(Concepto.de(t.concepto()) != null, "concepto existente para " + t.id() + ": " + t.concepto());
                for (Idioma id : Idioma.values()) {
                    String txt = ConsejosDelJuego.textoDelTip(t, id);
                    cierto(txt != null && !txt.isBlank() && !txt.startsWith("entrenador.tip."),
                            "texto de " + t.id() + " en " + id);
                }
            }
            for (String esperado : new String[]{"juego_empezar_tutoriales", "juego_demostraciones", "juego_oxidado_minijuegos",
                "juego_hora_de_hoy", "juego_taller", "juego_visor_tactico", "juego_tienda", "juego_piezas_naturalezas",
                "juego_depuracion", "juego_depurador_paso", "juego_laboratorio_pruebas", "juego_robots_rotos",
                "juego_temario", "juego_arena", "juego_panel_ia", "juego_ajustes", "juego_cambio_entrenador", "juego_cerebros_ia"}) {
                cierto(idsJuego.contains(esperado), "cubre consejo " + esperado);
            }
            Idioma antes = T.idioma;
            try {
                for (Idioma id : Idioma.values()) {
                    T.idioma = id;
                    for (String k : new String[]{"entrenador.desplegar", "entrenador.recoger", "entrenador.pestana"}) {
                        cierto(T.existe(k) && !T.t(k).isBlank(), k + " en " + id);
                    }
                }
            } finally {
                T.idioma = antes;
            }
        });

        caso("entrenador: mezcla consejos condicionales y aleatorios de jugabilidad sin repetir seguidos", () -> {
            Jugador principiante = new Jugador("Nuevo");
            cierto(ConsejosDelJuego.esPrincipiante(principiante), "detecta principiante");
            List<ConsejosDelTaller.Tip> dePrincipiante = ConsejosDelJuego.consejosDeJuego(principiante);
            igual("juego_empezar_tutoriales", dePrincipiante.get(0).id(), "principiante prioriza tutoriales y demos");
            igual("juego_demostraciones", dePrincipiante.get(1).id(), "principiante recomienda demostraciones");

            Jugador oxidado = new Jugador("Oxidado");
            oxidado.tutorialesHechos.addAll(List.of("t1", "t2"));
            oxidado.derrotasSeguidas = 3;
            oxidado.historial.add(new Registro(1L, "MiRobot", "equilibrado", "Rival", "tanque", 0.5, Registro.Resultado.DERROTA, 10, 50, 0, ""));
            cierto(!ConsejosDelJuego.esPrincipiante(oxidado) && ConsejosDelJuego.estaOxidado(oxidado), "detecta jugador oxidado");
            List<ConsejosDelTaller.Tip> deOxidado = ConsejosDelJuego.consejosDeJuego(oxidado);
            igual("juego_oxidado_minijuegos", deOxidado.get(0).id(), "oxidado prioriza minijuegos y ejercicios");
            igual("juego_hora_de_hoy", deOxidado.get(1).id(), "oxidado sugiere La hora de hoy");

            Set<String> idsAleatorios = new HashSet<>(ids(ConsejosDelJuego.consejosAleatoriosDeJuego()));
            cierto(idsAleatorios.size() >= 10, "amplio repertorio aleatorio de jugabilidad: " + idsAleatorios.size());
            EntrenadorFlotante ef = new EntrenadorFlotante(new Random(42L));
            Set<String> vistosCond = new HashSet<>();
            Set<String> vistosRand = new HashSet<>();
            String anterior = null;
            for (int k = 0; k < 20; k++) {
                ef.tipAhora();
                cierto(ef.avanzarTemporizadorDeTip(principiante, null, 0.1f), "consejo #" + k);
                String actual = ef.tipActual().id();
                cierto(!actual.equals(anterior), "no repite seguidos: " + actual);
                if (actual.equals("juego_empezar_tutoriales") || actual.equals("juego_demostraciones")) vistosCond.add(actual);
                if (idsAleatorios.contains(actual)) vistosRand.add(actual);
                anterior = actual;
                ef.cerrarTipManualmente();
            }
            cierto(vistosCond.size() == 2, "salen los condicionales del principiante: " + vistosCond);
            cierto(vistosRand.size() >= 6, "salen consejos aleatorios variados de jugabilidad: " + vistosRand);

            EntrenadorFlotante efOx = new EntrenadorFlotante(new Random(99L));
            efOx.tipAhora();
            cierto(efOx.avanzarTemporizadorDeTip(oxidado, null, 0.1f), "primer consejo del jugador oxidado");
            igual("juego_oxidado_minijuegos", efOx.tipActual().id(), "jugador oxidado recibe primero su condicional");
        });
    }

    private static void consultaIaDelEntrenador() {
        caso("entrenador: ConsultaIaEntrenador incluye programación, funcionalidades del juego, progreso y personalidad", () -> {
            Jugador novato = new Jugador("Ana");
            Entrenador en = Entrenador.de("miyagi");
            String sistema = ConsultaIaEntrenador.construirSistema(novato, Idioma.ES, en);
            cierto(sistema.contains(en.nombre.en) && sistema.contains("Metajavbot"), "personalidad y Java");
            cierto(sistema.contains("Tutorials") && sistema.contains("Demonstrations") && sistema.contains("Minigames"), "guía del juego");
            cierto(sistema.contains("Shop") && sistema.contains("Debugging & Testing") && sistema.contains("Settings"), "todas las pantallas");
            cierto(sistema.contains("NEVER the complete solution"), "nunca da la solución completa");
            String ctx = ConsultaIaEntrenador.construirContexto(novato, new Taller(), Concepto.de("tienda_y_monedas"), null,
                    "¿Cómo consigo piezas en la tienda?", Idioma.ES);
            cierto(ctx.contains("Player progress:") && ctx.contains("[BEGINNER]") && ctx.contains("Taller"), "progreso y pantalla");
            cierto(ctx.contains("¿Cómo consigo piezas en la tienda?"), "pregunta del jugador");
        });
    }

    private static List<String> ids(List<ConsejosDelTaller.Tip> l) {
        List<String> r = new ArrayList<>();
        for (ConsejosDelTaller.Tip t : l) r.add(t.id());
        return r;
    }

    private static boolean soloComentarios(String ejemplo) {
        for (String l : ejemplo.split("\n")) if (!l.isBlank() && !l.strip().startsWith("//")) return false;
        return true;
    }

    /**
     * Compila un ejemplo: si declara sus propias clases, tal cual (sin «public» delante, para que quepan en un
     * archivo); si es un trozo de robot, dentro de «class MiRobot extends Metajavbot»; y si son sentencias sueltas,
     * dentro de un método. Devuelve el primer error, o null si compila de alguna de esas maneras.
     */
    private static String compilarEjemplo(String ejemplo) {
        String sinPublic = ejemplo.replaceAll(
                "(?m)^public\\s+(?=(final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*(class|interface|record|enum|@interface)\\b)", "");
        boolean conTipos = java.util.regex.Pattern.compile("(?m)^(@\\w+(\\([^)]*\\))?\\s+)*(final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+)*"
                + "(class|interface|record|enum|@interface)\\s").matcher(sinPublic).find();
        String[] intentos = conTipos
                ? new String[]{IMPORTS + sinPublic + "\nclass MiRobotDePrueba extends Metajavbot {\n}\n"}
                : new String[]{IMPORTS + "class MiRobot extends Metajavbot {\n" + ejemplo + "\n}\n",
                    IMPORTS + "class MiRobot extends Metajavbot {\n    void ejemplo() throws Exception {\n" + ejemplo + "\n    }\n}\n"};
        String primero = null;
        for (String fuente : intentos) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("Ejemplo", fuente);
            Compilador.Resultado r = Compilador.compilar(m, false, Vigilante.Modo.CONFIANZA, Locale.ROOT);
            if (r.bien) return null;
            if (primero == null && !r.graves().isEmpty()) {
                Compilador.Fallo f = r.graves().get(0);
                int linea = (int) f.linea - IMPORTS.split("\n").length;
                primero = "línea " + linea + ": " + f.mensaje;
            }
        }
        return primero == null ? "no compila" : primero;
    }
}
