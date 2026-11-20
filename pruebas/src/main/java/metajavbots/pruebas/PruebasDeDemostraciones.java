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
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas del motor de demostraciones en vivo y de la Demo 1 (8 pasos) en los tres idiomas (ES, DE, EN). */
final class PruebasDeDemostraciones {

    private PruebasDeDemostraciones() {
    }

    static void apuntar() {
        apuntarCargaTrilingue();
        apuntarEjecucionCompleta();
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
            "demo.ver", "demo.pasos", "demo.paso_de", "demo.nivel.1", "demo.continuar",
            "demo.repetir", "demo.salir", "demo.ejecutando", "demo.pausa_paso", "demo.fin"
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
}
