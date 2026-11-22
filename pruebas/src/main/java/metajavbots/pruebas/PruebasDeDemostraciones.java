package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.BitSet;
import java.util.List;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.MotorDemostracion;
import metajavbots.pantalla.TextoEnriquecido;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas del motor de demostraciones en vivo, coherencia numérica/visual y versalita en los tres idiomas (ES, DE, EN). */
final class PruebasDeDemostraciones {

    private PruebasDeDemostraciones() {
    }

    static void apuntar() {
        apuntarCargaTrilingue();
        apuntarEjecucionCompleta();
        apuntarCoherenciaYVersalita();
        apuntarDemo2TrilingueYEjecucion();
        apuntarPrecisionPedagogicaYAlcance();
    }

    private static void apuntarCargaTrilingue() {
        caso("demo 1: carga trilingüe de 8 pasos (ES, DE, EN), API de cada idioma, líneas válidas y compilación", () -> {
            List<Demostracion> todas = Demostracion.todas();
            cierto(!todas.isEmpty(), "debe existir al menos la Demo 1");
            Demostracion d1 = Demostracion.de(Demostracion.ID_DEMO_1);
            cierto(d1 != null, "Demo 1 accesible por id");
            igual(8, d1.pasos.size(), "Demo 1 tiene 8 pasos");
            String[] idsEsperados = {
                "intro_juego", "cada_turno", "avanzar", "girar",
                "radar", "ver_enemigo", "apuntar_disparar", "pelear_rondas"
            };
            for (int i = 0; i < idsEsperados.length; i++) {
                igual(idsEsperados[i], d1.pasos.get(i).id, "paso " + (i + 1));
            }

            verificarClavesUi();
            cierto(!d1.titulo(Idioma.ES).equals(d1.titulo(Idioma.DE)), "título distinto ES/DE");
            cierto(!d1.titulo(Idioma.ES).equals(d1.titulo(Idioma.EN)), "título distinto ES/EN");

            for (Idioma id : Idioma.values()) {
                String codigo = d1.codigo(id);
                cierto(codigo != null && !codigo.isBlank(), "código en " + id);
                String[] lineas = codigo.split("\n");
                for (Demostracion.Paso p : d1.pasos) {
                    cierto(!p.titulo(id).isBlank(), p.id + " título en " + id);
                    cierto(!p.texto(id).isBlank(), p.id + " texto en " + id);
                    cierto(p.lineaPrincipal >= 1 && p.lineaPrincipal <= lineas.length,
                            p.id + " línea principal fuera de rango en " + id + ": " + p.lineaPrincipal);
                    for (int ls : p.lineasSecundarias) {
                        cierto(ls >= 1 && ls <= lineas.length, p.id + " línea secundaria fuera de rango: " + ls);
                    }
                }
                verificarApiEnIdioma(id, codigo, lineas);
                if (Compilador.hayCompilador()) {
                    Compilador.Resultado res = Compilador.compilar(codigo, true, Vigilante.Modo.ROBOT, id.locale());
                    cierto(res.bien, "código de Demo 1 debe compilar en " + id + ": " + res.errores);
                }
            }
        });
    }

    private static void verificarClavesUi() {
        String[] claves = {
            "demo.boton", "demo.boton_ayuda", "demo.banner_texto", "demo.titulo", "demo.intro",
            "demo.ver", "demo.pasos", "demo.paso_de", "demo.nivel.1", "demo.nivel.2", "demo.nivel.3", "demo.nivel.4",
            "demo.continuar", "demo.repetir", "demo.salir", "demo.ejecutando", "demo.pausa_paso", "demo.fin"
        };
        for (String k : claves) {
            for (Idioma id : Idioma.values()) {
                String txt = T.en(k, id);
                cierto(txt != null && !txt.isBlank() && !txt.equals(k), "clave faltante " + k + " en " + id);
            }
        }
    }

    private static void verificarApiEnIdioma(Idioma id, String codigo, String[] lineas) {
        if (id == Idioma.ES) {
            cierto(codigo.contains("extends Metajavbot"), "ES hereda de Metajavbot");
            cierto(lineas[4].contains("cadaTurno()"), "ES línea 5 cadaTurno");
            cierto(lineas[5].contains("avanzar(80);"), "ES línea 6 avanzar(80)");
            cierto(lineas[6].contains("girar(45);"), "ES línea 7 girar(45)");
            cierto(lineas[7].contains("enemigosVistos()"), "ES línea 8 enemigosVistos");
            cierto(lineas[8].contains("apuntar(15);"), "ES línea 9 apuntar(15)");
            cierto(lineas[13].contains("alVerEnemigo("), "ES línea 14 alVerEnemigo");
            cierto(lineas[14].contains("apuntarHacia("), "ES línea 15 apuntarHacia");
            cierto(lineas[15].contains("atacar()"), "ES línea 16 atacar");
        } else if (id == Idioma.DE) {
            cierto(codigo.contains("extends MetajavbotDe"), "DE hereda de MetajavbotDe");
            cierto(lineas[4].contains("jedenZug()"), "DE línea 5 jedenZug");
            cierto(lineas[5].contains("vorwaerts(80);"), "DE línea 6 vorwaerts(80)");
            cierto(lineas[6].contains("drehen(45);"), "DE línea 7 drehen(45)");
            cierto(lineas[7].contains("gesehen()"), "DE línea 8 gesehen");
            cierto(lineas[8].contains("zielen(15);"), "DE línea 9 zielen(15)");
            cierto(lineas[13].contains("beiSicht("), "DE línea 14 beiSicht");
            cierto(lineas[14].contains("zielenNach("), "DE línea 15 zielenNach");
            cierto(lineas[15].contains("angreifen()"), "DE línea 16 angreifen");
        } else {
            cierto(codigo.contains("extends MetajavbotEn"), "EN hereda de MetajavbotEn");
            cierto(lineas[4].contains("onTick()"), "EN línea 5 onTick");
            cierto(lineas[5].contains("ahead(80);"), "EN línea 6 ahead(80)");
            cierto(lineas[6].contains("turn(45);"), "EN línea 7 turn(45)");
            cierto(lineas[7].contains("seenEnemies()"), "EN línea 8 seenEnemies");
            cierto(lineas[8].contains("aim(15);"), "EN línea 9 aim(15)");
            cierto(lineas[13].contains("onSeeEnemy("), "EN línea 14 onSeeEnemy");
            cierto(lineas[14].contains("aimTo("), "EN línea 15 aimTo");
            cierto(lineas[15].contains("attack()"), "EN línea 16 attack");
        }
    }

    private static void apuntarEjecucionCompleta() {
        caso("demo 1: ejecución en vivo de los 8 pasos sin errores, pausas automáticas y repetir paso", () -> {
            Demostracion d1 = Demostracion.de(Demostracion.ID_DEMO_1);
            for (Idioma id : Idioma.values()) {
                verificarRecorridoCompleto(d1, id);
            }
        });
    }

    private static void verificarRecorridoCompleto(Demostracion d1, Idioma id) {
        MotorDemostracion m = new MotorDemostracion(d1, id);

        // Paso 1: ¿De qué va este juego? (pausa inicial directa).
        igual(0, m.indicePaso(), id + " empieza en paso 1");
        cierto(m.pausado(), id + " paso 1 pausado");
        igual(3, m.lineaActiva(), id + " paso 1 resalta línea 3");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 1");

        // Paso 2: cadaTurno() (pausa directa resaltando el bloque).
        m.continuar();
        igual(1, m.indicePaso(), id + " paso 2");
        cierto(m.pausado(), id + " paso 2 pausado");
        igual(5, m.lineaActiva(), id + " paso 2 resalta cadaTurno");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 2");

        // Paso 3: avanzar(80); (avanza y pausa).
        m.continuar();
        igual(2, m.indicePaso(), id + " paso 3");
        falso(m.pausado(), id + " paso 3 arranca en vivo");
        correrHastaPausa(m, 100);
        cierto(m.pausado(), id + " paso 3 pausa automática");
        cierto(m.distanciaRecorrida() > 50, id + " robot avanzó en paso 3: " + m.distanciaRecorrida());
        igual(6, m.lineaActiva(), id + " paso 3 resalta avanzar");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 3");

        // Repetir paso 3 funciona limpio.
        m.repetirPaso();
        falso(m.pausado(), id + " repetir paso 3 vuelve a ejecutar");
        correrHastaPausa(m, 100);
        cierto(m.pausado(), id + " repetir paso 3 pausa de nuevo");
        cierto(m.distanciaRecorrida() > 50, id + " repetir paso 3 vuelve a moverse");

        // Paso 4: girar(45); (gira y pausa).
        m.continuar();
        igual(3, m.indicePaso(), id + " paso 4");
        falso(m.pausado(), id + " paso 4 arranca en vivo");
        correrHastaPausa(m, 100);
        cierto(m.pausado(), id + " paso 4 pausa automática");
        cierto(m.giroAcumulado() > 35, id + " robot giró en paso 4: " + m.giroAcumulado());
        igual(7, m.lineaActiva(), id + " paso 4 resalta girar");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 4");

        // Paso 5: Radar (VisorTactico activo, gira la mira sin girar el cuerpo).
        m.continuar();
        igual(4, m.indicePaso(), id + " paso 5");
        cierto(m.visorTactico(), id + " paso 5 activa visor táctico");
        falso(m.pausado(), id + " paso 5 arranca en vivo");
        correrHastaPausa(m, 100);
        cierto(m.pausado(), id + " paso 5 pausa automática");
        cierto(m.giroMiraAcumulado() > 30, id + " radar giró en paso 5: " + m.giroMiraAcumulado());
        igual(9, m.lineaActiva(), id + " paso 5 resalta apuntar");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 5");

        // Paso 6: ¡Enemigo a la vista! (pausa justo al detectar al muñeco, sin disparar aún).
        m.continuar();
        igual(5, m.indicePaso(), id + " paso 6");
        cierto(m.visorTactico(), id + " paso 6 activa visor táctico");
        correrHastaPausa(m, 150);
        cierto(m.pausado(), id + " paso 6 pausa al detectar");
        cierto(m.enemigoDetectado(), id + " paso 6 detecta al muñeco");
        falso(m.disparoRealizado(), id + " paso 6 no dispara todavía");
        igual(14, m.lineaActiva(), id + " paso 6 resalta alVerEnemigo");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 6");

        // Paso 7: Apuntar y disparar (detecta, apunta, dispara e impacta).
        m.continuar();
        igual(6, m.indicePaso(), id + " paso 7");
        cierto(m.visorTactico(), id + " paso 7 activa visor táctico");
        correrHastaPausa(m, 200);
        cierto(m.pausado(), id + " paso 7 pausa tras disparar");
        cierto(m.enemigoDetectado(), id + " paso 7 detecta al muñeco");
        cierto(m.disparoRealizado(), id + " paso 7 dispara");
        BitSet activas7 = m.lineasActivas();
        cierto(activas7.get(15) && activas7.get(16), id + " paso 7 resalta apuntarHacia y atacar");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 7");
        m.alternarVisor();
        falso(m.visorTactico(), id + " visor táctico desactivable");
        m.alternarVisor();
        cierto(m.visorTactico(), id + " visor táctico reactivable");

        // Paso 8: Cómo se juega una pelea real (rondas y mejoras).
        m.continuar();
        igual(7, m.indicePaso(), id + " paso 8");
        cierto(m.esUltimoPaso(), id + " paso 8 es el último");
        cierto(m.pausado(), id + " paso 8 pausado");
        cierto(m.ultimoError().isEmpty(), id + " sin error en paso 8");
        m.continuar();
        cierto(m.terminada(), id + " demo finalizada al continuar tras el último paso");
    }

    private static void correrHastaPausa(MotorDemostracion m, int maxTurnos) {
        for (int i = 0; i < maxTurnos && !m.pausado(); i++) {
            m.avanzarTurno();
        }
    }

    private static void apuntarCoherenciaYVersalita() {
        caso("demo 1: coherencia exacta de cifras, sentido de giro, HUD de barras y líneas resaltadas en ES, DE y EN", () -> {
            Demostracion d1 = Demostracion.de(Demostracion.ID_DEMO_1);
            for (Idioma id : Idioma.values()) {
                MotorDemostracion m = new MotorDemostracion(d1, id);
                cierto(m.hudBarrasVisible(), id + " barras HUD visibles desde el inicio");
                falso(m.resaltarHudBarras(), id + " paso 1 no resalta barras HUD");

                // Paso 3: avanzar(80) recorre exactamente ~80, nunca +90, y el texto cita 80.
                m.continuar();
                m.continuar();
                correrHastaPausa(m, 100);
                cierto(Math.abs(m.distanciaRecorrida() - 80.0) <= 1.0,
                        id + " avanzar(80) debe recorrer ~80, obtuvo " + m.distanciaRecorrida());
                cierto(m.pasoActual().texto(id).contains("80"), id + " paso 3 cita 80 en " + id);

                // Paso 4: girar(45) gira ~45° y explica sentido horario/antihorario y 0°/90°.
                m.continuar();
                correrHastaPausa(m, 100);
                cierto(Math.abs(m.giroAcumulado() - 45.0) <= 1.0,
                        id + " girar(45) debe girar ~45°, obtuvo " + m.giroAcumulado());
                String txtGiro = m.pasoActual().texto(id);
                cierto(txtGiro.contains("0°") && txtGiro.contains("90°") && txtGiro.contains("45"),
                        id + " paso 4 explica 0°, 90° y 45");
                cierto(txtGiro.contains("-45") && txtGiro.contains("-90"),
                        id + " paso 4 explica giros negativos");
                cierto(txtGiro.contains("agujas del reloj") || txtGiro.contains("Uhrzeigersinn") || txtGiro.contains("clockwise"),
                        id + " paso 4 explica sentido horario/antihorario");

                // Paso 5 y Paso 6 -> Paso 7: HUD de barras visible y resaltado al hablar de las barras.
                m.continuar();
                correrHastaPausa(m, 100);
                m.continuar();
                correrHastaPausa(m, 150);
                m.continuar();
                cierto(m.hudBarrasVisible(), id + " paso 7 muestra barras HUD");
                cierto(m.resaltarHudBarras(), id + " paso 7 resalta barras HUD");
                double energiaInicial = m.pelea().cuerpo(0).energia;
                boolean gastoEnergia = false;
                boolean entroEnRecarga = false;
                for (int t = 0; t < 200 && !m.pausado(); t++) {
                    m.avanzarTurno();
                    if (m.pelea().cuerpo(0).energia < energiaInicial - 0.01) {
                        gastoEnergia = true;
                    }
                    if (m.pelea().cuerpo(0).enfriamiento(0) > 0 || m.pelea().cuerpo(0).enfriamiento(1) > 0) {
                        entroEnRecarga = true;
                    }
                }
                cierto(gastoEnergia || entroEnRecarga, id + " paso 7 refleja gasto de energía o recarga en el HUD");
                cierto(m.pelea().cuerpo(1).blindaje() < 100.0, id + " paso 7 reduce la barra de blindaje del muñeco");

                // Paso 8: usa marca de versalita [[...]] para secciones/botones sin choque «a ¡».
                m.continuar();
                String txt8 = m.pasoActual().texto(id);
                cierto(TextoEnriquecido.tieneMarcas(txt8), id + " paso 8 usa marca [[...]]");
                falso(txt8.contains("a ¡"), id + " paso 8 sin choque sintáctico «a ¡»");
            }
        });

        caso("tipografía: parseo y trozos de versalita y negrita [[...]] (escala 78 %, texto plano y bordes)", () -> {
            cierto(Math.abs(TextoEnriquecido.ESCALA_VERSALITA - 0.78f) < 0.001f, "escala versalita 78 %");
            cierto(TextoEnriquecido.tieneMarcas("Ve al botón [[¡A pelear!]] ahora."), "detecta marcas");
            falso(TextoEnriquecido.tieneMarcas("Texto normal sin marcas."), "sin falsos positivos");
            falso(TextoEnriquecido.tieneMarcas("Sin cerrar [[hola"), "sin cerrar no cuenta");
            igual("Ve al botón ¡A pelear! ahora.",
                    TextoEnriquecido.textoPlano("Ve al botón [[¡A pelear!]] ahora."), "texto plano");
            cierto(TextoEnriquecido.quedaAbierta("Ve al botón [[¡A"), "detecta marca abierta");
            falso(TextoEnriquecido.quedaAbierta("Ve al botón [[¡A pelear!]]"), "marca cerrada");

            List<TextoEnriquecido.Trozo> trozos = TextoEnriquecido.aTrozos("En el [[Taller]]", false);
            igual(3, trozos.size(), "tres trozos: plano + inicial + versalita");
            igual("En el ", trozos.get(0).texto);
            falso(trozos.get(0).negrita, "tramo normal sin negrita");
            falso(trozos.get(0).pequeno, "tramo normal tamaño base");
            igual("T", trozos.get(1).texto);
            cierto(trozos.get(1).negrita && !trozos.get(1).pequeno, "inicial mayúscula negrita al 100 %");
            igual("ALLER", trozos.get(2).texto);
            cierto(trozos.get(2).negrita && trozos.get(2).pequeno, "resto en mayúsculas negrita al 78 %");

            List<TextoEnriquecido.Trozo> aleman = TextoEnriquecido.aTrozos("[[Beißen]]", false);
            igual(2, aleman.size(), "alemán con ß en versalita");
            igual("B", aleman.get(0).texto);
            igual("EISSEN", aleman.get(1).texto);
        });
    }

    private static void apuntarDemo2TrilingueYEjecucion() {
        caso("demo 2: carga trilingüe de 6 pasos (ES, DE, EN), líneas ▶, API y compilación de código roto y corregido", () -> {
            cierto(Demostracion.todas().size() >= 2, "deben existir Demo 1 y Demo 2");
            Demostracion d2 = Demostracion.de(Demostracion.ID_DEMO_2);
            cierto(d2 != null, "Demo 2 accesible por id");
            igual(6, d2.pasos.size(), "Demo 2 tiene 6 pasos");
            String[] idsEsperados = {
                "regla_receta", "fallo_en_vivo", "orden_del_turno",
                "mover_linea", "prueba_en_arena", "noob_a_junior"
            };
            for (int i = 0; i < idsEsperados.length; i++) {
                igual(idsEsperados[i], d2.pasos.get(i).id, "demo 2 paso " + (i + 1));
            }
            cierto(!d2.titulo(Idioma.ES).equals(d2.titulo(Idioma.DE)), "Demo 2 título distinto ES/DE");
            cierto(!d2.titulo(Idioma.ES).equals(d2.titulo(Idioma.EN)), "Demo 2 título distinto ES/EN");

            for (Idioma id : Idioma.values()) {
                String codigoRoto = d2.pasos.get(0).codigo(d2, id);
                String codigoFijo = d2.pasos.get(3).codigo(d2, id);
                cierto(!codigoRoto.equals(codigoFijo), "Demo 2 cambia el código al mover la línea en " + id);
                for (Demostracion.Paso p : d2.pasos) {
                    String cod = p.codigo(d2, id);
                    String[] lineas = cod.split("\n");
                    cierto(!p.titulo(id).isBlank(), p.id + " título en " + id);
                    cierto(!p.texto(id).isBlank(), p.id + " texto en " + id);
                    cierto(p.lineaPrincipal >= 1 && p.lineaPrincipal <= lineas.length,
                            p.id + " línea principal fuera de rango en " + id + ": " + p.lineaPrincipal);
                    for (int ls : p.lineasSecundarias) {
                        cierto(ls >= 1 && ls <= lineas.length, p.id + " línea secundaria fuera de rango: " + ls);
                    }
                }
                verificarApiDemo2(id, codigoRoto.split("\n"), codigoFijo.split("\n"));
                if (Compilador.hayCompilador()) {
                    Compilador.Resultado rRoto = Compilador.compilar(codigoRoto, true, Vigilante.Modo.ROBOT, id.locale());
                    cierto(rRoto.bien, "código roto de Demo 2 debe compilar en " + id + ": " + rRoto.errores);
                    Compilador.Resultado rFijo = Compilador.compilar(codigoFijo, true, Vigilante.Modo.ROBOT, id.locale());
                    cierto(rFijo.bien, "código corregido de Demo 2 debe compilar en " + id + ": " + rFijo.errores);
                }
            }
        });

        caso("demo 2: ejecución en vivo de los 6 pasos (fallo, orden, línea movida, acierto y cierre) en ES, DE y EN", () -> {
            Demostracion d2 = Demostracion.de(Demostracion.ID_DEMO_2);
            for (Idioma id : Idioma.values()) {
                verificarRecorridoDemo2(d2, id);
            }
        });
    }

    private static void verificarApiDemo2(Idioma id, String[] roto, String[] fijo) {
        if (id == Idioma.ES) {
            cierto(roto[14].contains("apuntar(25);"), "ES roto línea 15 apuntar(25)");
            cierto(fijo[13].contains("apuntar(25);"), "ES corregido línea 14 apuntar(25)");
            cierto(roto[5].contains("apuntarHacia("), "ES línea 6 apuntarHacia");
            cierto(roto[6].contains("atacar()"), "ES línea 7 atacar");
        } else if (id == Idioma.DE) {
            cierto(roto[14].contains("zielen(25);"), "DE roto línea 15 zielen(25)");
            cierto(fijo[13].contains("zielen(25);"), "DE corregido línea 14 zielen(25)");
            cierto(roto[5].contains("zielenNach("), "DE línea 6 zielenNach");
            cierto(roto[6].contains("angreifen()"), "DE línea 7 angreifen");
        } else {
            cierto(roto[14].contains("aim(25);"), "EN roto línea 15 aim(25)");
            cierto(fijo[13].contains("aim(25);"), "EN corregido línea 14 aim(25)");
            cierto(roto[5].contains("aimTo("), "EN línea 6 aimTo");
            cierto(roto[6].contains("attack()"), "EN línea 7 attack");
        }
    }

    private static void verificarRecorridoDemo2(Demostracion d2, Idioma id) {
        MotorDemostracion m = new MotorDemostracion(d2, id);

        // Paso 1: La regla de la receta (pausa inicial).
        igual(0, m.indicePaso(), id + " demo 2 paso 1");
        cierto(m.pausado(), id + " demo 2 paso 1 pausado");
        igual(3, m.lineaActiva(), id + " demo 2 paso 1 resalta línea 3");
        igual(Demostracion.Resaltado.BIEN, m.resaltadoActual(), id + " demo 2 paso 1 resaltado verde");
        cierto(m.ultimoError().isEmpty(), id + " demo 2 sin error en paso 1");

        // Paso 2: El fallo en vivo (dispara fuera de objetivo por apuntar(25) en línea 15, resaltado en rojo).
        m.continuar();
        igual(1, m.indicePaso(), id + " demo 2 paso 2");
        falso(m.pausado(), id + " demo 2 paso 2 arranca en vivo");
        correrHastaPausa(m, 150);
        cierto(m.pausado(), id + " demo 2 paso 2 pausa automática");
        cierto(m.enemigoDetectado(), id + " demo 2 paso 2 detecta al muñeco");
        cierto(m.disparoRealizado(), id + " demo 2 paso 2 dispara");
        falso(m.impactoRealizado(), id + " demo 2 paso 2 falla el tiro por el orden erróneo");
        igual(15, m.lineaActiva(), id + " demo 2 paso 2 señala línea culpable 15");
        igual(Demostracion.Resaltado.ERROR, m.resaltadoActual(), id + " demo 2 paso 2 resalta en rojo");
        cierto(m.ultimoError().isEmpty(), id + " demo 2 sin error en paso 2");

        // Paso 3: Qué pasa turno a turno (resaltado en ámbar).
        m.continuar();
        igual(2, m.indicePaso(), id + " demo 2 paso 3");
        cierto(m.pausado(), id + " demo 2 paso 3 pausado");
        igual(15, m.lineaActiva(), id + " demo 2 paso 3 señala línea 15");
        igual(Demostracion.Resaltado.AVISO, m.resaltadoActual(), id + " demo 2 paso 3 resalta en ámbar");
        BitSet activas3 = m.lineasActivas();
        cierto(activas3.get(5) && activas3.get(6) && activas3.get(11) && activas3.get(15),
                id + " demo 2 paso 3 resalta ambos bloques y la línea conflictiva");

        // Paso 4: Mover la línea de sitio (código actualizado y línea 14 en verde).
        m.continuar();
        igual(3, m.indicePaso(), id + " demo 2 paso 4");
        cierto(m.pausado(), id + " demo 2 paso 4 pausado");
        igual(14, m.lineaActiva(), id + " demo 2 paso 4 señala línea movida 14");
        igual(Demostracion.Resaltado.BIEN, m.resaltadoActual(), id + " demo 2 paso 4 resalta en verde");
        cierto(m.codigoActual().split("\n")[13].contains("(25);"), id + " demo 2 paso 4 tiene la orden en línea 14");

        // Paso 5: La prueba en la arena (acierta al objetivo y baja el blindaje del muñeco).
        m.continuar();
        igual(4, m.indicePaso(), id + " demo 2 paso 5");
        falso(m.pausado(), id + " demo 2 paso 5 arranca en vivo");
        cierto(m.resaltarHudBarras(), id + " demo 2 paso 5 resalta barras HUD");
        correrHastaPausa(m, 200);
        cierto(m.pausado(), id + " demo 2 paso 5 pausa tras acertar");
        cierto(m.enemigoDetectado(), id + " demo 2 paso 5 detecta al muñeco");
        cierto(m.disparoRealizado(), id + " demo 2 paso 5 dispara");
        cierto(m.impactoRealizado(), id + " demo 2 paso 5 acierta al muñeco");
        cierto(m.pelea().cuerpo(1).blindaje() < 100.0, id + " demo 2 paso 5 baja el blindaje del muñeco");
        igual(7, m.lineaActiva(), id + " demo 2 paso 5 resalta atacar");
        cierto(m.ultimoError().isEmpty(), id + " demo 2 sin error en paso 5");

        // Paso 6: Cómo subir de Noob a Junior (cierre con marcas [[...]]).
        m.continuar();
        igual(5, m.indicePaso(), id + " demo 2 paso 6");
        cierto(m.esUltimoPaso(), id + " demo 2 paso 6 es el último");
        cierto(m.pausado(), id + " demo 2 paso 6 pausado");
        String txt6 = m.pasoActual().texto(id);
        cierto(TextoEnriquecido.tieneMarcas(txt6), id + " demo 2 paso 6 usa marcas [[...]]");
        cierto(txt6.contains("Noob") && txt6.contains("Junior"), id + " demo 2 paso 6 explica Noob a Junior");
        cierto(m.ultimoError().isEmpty(), id + " demo 2 sin error en paso 6");
        m.continuar();
        cierto(m.terminada(), id + " demo 2 finalizada al continuar tras el último paso");
    }

    private static void apuntarPrecisionPedagogicaYAlcance() {
        caso("pedagogía: avanzar(80) orden vs distancia efectiva, orden real de callbacks, pseudocódigo de alcance y catálogo sin demos huérfanas", () -> {
            igual(4, Demostracion.todas().size(), "Demo 1, Demo 2, Demo 3 y Demo 4 están implementadas");
            cierto(T.existe("demo.nivel.3"), "debe existir la clave demo.nivel.3 para Demo 3");
            cierto(T.existe("demo.nivel.4"), "debe existir la clave demo.nivel.4 para Demo 4");
            falso(T.existe("demo.nivel.5"), "no debe existir clave huérfana demo.nivel.5");

            Demostracion d1 = Demostracion.de(Demostracion.ID_DEMO_1);
            Demostracion d2 = Demostracion.de(Demostracion.ID_DEMO_2);
            for (Idioma id : Idioma.values()) {
                MotorDemostracion m1 = new MotorDemostracion(d1, id);
                m1.irAPaso(2);
                correrHastaPausa(m1, 100);
                cierto(m1.turnosEnPaso() > 1, id + " avanzar(80) se cumple en varios turnos: " + m1.turnosEnPaso());
                verificarTextoAvanzar(id, d1.pasos.get(2).texto(id));
                verificarTextoAtaqueAntesMovimiento(id, d1.pasos.get(6).texto(id));
                verificarOrdenRealCallbacks(id, d2.pasos.get(2).texto(id));
                verificarPseudocodigoAlcance(id);
            }
        });
    }

    private static void verificarTextoAvanzar(Idioma id, String txt) {
        if (id == Idioma.ES) {
            cierto(txt.contains("orden solicitada") && txt.contains("distancia efectiva"), "ES distingue orden solicitada y distancia efectiva");
            falso(txt.contains("acaba de recorrer 80 pasos"), "ES no afirma recorrido instantáneo");
        } else if (id == Idioma.DE) {
            cierto(txt.contains("angeforderten Befehl") && txt.contains("tatsächlich zurückgelegte Strecke"),
                    "DE unterscheidet angeforderten Befehl und tatsächliche Strecke");
            falso(txt.contains("genau 80 Schritte"), "DE behauptet keine Sofortbewegung");
        } else {
            cierto(txt.contains("requested order") && txt.contains("actual distance traveled"),
                    "EN distinguishes requested order and actual distance traveled");
            falso(txt.contains("just traveled 80 steps"), "EN does not claim instant travel");
        }
    }

    private static void verificarTextoAtaqueAntesMovimiento(Idioma id, String txt) {
        if (id == Idioma.ES) {
            cierto(txt.contains("ataques antes del movimiento"), "ES indica que los ataques van antes del movimiento");
        } else if (id == Idioma.DE) {
            cierto(txt.contains("Angriffe vor der Bewegung"), "DE erklärt Angriffe vor der Bewegung");
        } else {
            cierto(txt.contains("attacks before movement"), "EN explains attacks before movement");
        }
    }

    private static void verificarOrdenRealCallbacks(Idioma id, String txt) {
        String[] nombres = id == Idioma.ES
                ? new String[]{"alRecibirGolpe", "alAcertar", "alChocarConPared", "alChocarConEnemigo", "alOirAlEquipo", "alVerEnemigo", "cadaTurno"}
                : id == Idioma.DE
                ? new String[]{"beiTreffer", "beiEigenemTreffer", "beiWand", "beiZusammenstoss", "beiTeamNachricht", "beiSicht", "jedenZug"}
                : new String[]{"onHit", "onHitEnemy", "onHitWall", "onBump", "onTeamMessage", "onSeeEnemy", "onTick"};
        int previo = -1;
        for (String nombre : nombres) {
            int pos = txt.indexOf(nombre);
            cierto(pos > previo, id + " orden real de callbacks debe incluir " + nombre + " en orden");
            previo = pos;
        }
    }

    private static void verificarPseudocodigoAlcance(Idioma id) {
        String disparar = T.en("mini.disparar.pseudo", id);
        String acerca = T.en("entrenador.rival.se_acerca.trozo", id);
        cierto(disparar.contains("<="), id + " mini.disparar.pseudo usa límite inclusivo <=");
        cierto(acerca.contains("<="), id + " entrenador.rival.se_acerca.trozo usa límite inclusivo <=");
        if (id == Idioma.ES) {
            cierto(disparar.contains("normalizar(") && disparar.contains("regla práctica"), "ES normaliza y marca regla práctica");
        } else if (id == Idioma.DE) {
            cierto(disparar.contains("normalisieren(") && disparar.contains("Faustregel"), "DE normalisiert und markiert Faustregel");
        } else {
            cierto(disparar.contains("normalize(") && disparar.contains("rule of thumb"), "EN normalizes and marks rule of thumb");
        }
    }
}
