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

/** Pruebas del motor de demostraciones en vivo y de la Demo 1 en los tres idiomas (ES, DE, EN). */
final class PruebasDeDemostraciones {

    private PruebasDeDemostraciones() {
    }

    static void apuntar() {
        apuntarCargaTrilingue();
        apuntarEjecucionCompleta();
    }

    private static void apuntarCargaTrilingue() {
        caso("demo 1: carga trilingüe (ES, DE, EN), API de cada idioma, líneas válidas y compilación", () -> {
            List<Demostracion> todas = Demostracion.todas();
            cierto(!todas.isEmpty(), "debe existir al menos la Demo 1");
            Demostracion d1 = Demostracion.de(Demostracion.ID_DEMO_1);
            cierto(d1 != null, "Demo 1 accesible por id");
            igual(4, d1.pasos.size(), "Demo 1 tiene 4 pasos");
            igual("taller", d1.pasos.get(0).id, "paso 1");
            igual("mover_girar", d1.pasos.get(1).id, "paso 2");
            igual("radar_disparo", d1.pasos.get(2).id, "paso 3");
            igual("pelear_rondas", d1.pasos.get(3).id, "paso 4");

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

    private static void verificarApiEnIdioma(Idioma id, String codigo, String[] lineas) {
        if (id == Idioma.ES) {
            cierto(codigo.contains("extends Metajavbot"), "ES hereda de Metajavbot");
            cierto(lineas[5].contains("avanzar("), "ES línea 6 avanzar");
            cierto(lineas[6].contains("girar("), "ES línea 7 girar");
            cierto(lineas[7].contains("enemigosVistos()"), "ES línea 8 enemigosVistos");
            cierto(lineas[13].contains("alVerEnemigo("), "ES línea 14 alVerEnemigo");
            cierto(lineas[14].contains("apuntarHacia("), "ES línea 15 apuntarHacia");
            cierto(lineas[15].contains("atacar()"), "ES línea 16 atacar");
        } else if (id == Idioma.DE) {
            cierto(codigo.contains("extends MetajavbotDe"), "DE hereda de MetajavbotDe");
            cierto(lineas[5].contains("vorwaerts("), "DE línea 6 vorwaerts");
            cierto(lineas[6].contains("drehen("), "DE línea 7 drehen");
            cierto(lineas[7].contains("gesehen()"), "DE línea 8 gesehen");
            cierto(lineas[13].contains("beiSicht("), "DE línea 14 beiSicht");
            cierto(lineas[14].contains("zielenNach("), "DE línea 15 zielenNach");
            cierto(lineas[15].contains("angreifen()"), "DE línea 16 angreifen");
        } else {
            cierto(codigo.contains("extends MetajavbotEn"), "EN hereda de MetajavbotEn");
            cierto(lineas[5].contains("ahead("), "EN línea 6 ahead");
            cierto(lineas[6].contains("turn("), "EN línea 7 turn");
            cierto(lineas[7].contains("seenEnemies()"), "EN línea 8 seenEnemies");
            cierto(lineas[13].contains("onSeeEnemy("), "EN línea 14 onSeeEnemy");
            cierto(lineas[14].contains("aimTo("), "EN línea 15 aimTo");
            cierto(lineas[15].contains("attack()"), "EN línea 16 attack");
        }
    }

    private static void apuntarEjecucionCompleta() {
        caso("demo 1: ejecución en vivo de todos los pasos sin errores, pausas automáticas y repetir paso", () -> {
            Demostracion d1 = Demostracion.de(Demostracion.ID_DEMO_1);
            for (Idioma id : Idioma.values()) {
                MotorDemostracion m = new MotorDemostracion(d1, id);

                // Paso 1: Taller (pausa inicial directa).
                igual(0, m.indicePaso(), id + " empieza en paso 1");
                cierto(m.pausado(), id + " paso 1 pausado");
                igual(3, m.lineaActiva(), id + " paso 1 resalta línea 3");
                cierto(m.ultimoError().isEmpty(), id + " sin error en paso 1");

                // Paso 2: Mover y girar.
                m.continuar();
                igual(1, m.indicePaso(), id + " paso 2");
                falso(m.pausado(), id + " paso 2 arranca en vivo");
                correrHastaPausa(m, 120);
                cierto(m.pausado(), id + " paso 2 hace pausa automática");
                cierto(m.distanciaRecorrida() > 20, id + " robot avanzó en paso 2: " + m.distanciaRecorrida());
                cierto(m.giroAcumulado() > 5, id + " robot giró en paso 2: " + m.giroAcumulado());
                BitSet activas2 = m.lineasActivas();
                cierto(activas2.get(6) && activas2.get(7), id + " paso 2 resalta mover y girar");
                cierto(m.ultimoError().isEmpty(), id + " sin error en paso 2");

                // Repetir paso 2 funciona limpio.
                m.repetirPaso();
                falso(m.pausado(), id + " repetir paso 2 vuelve a ejecutar");
                correrHastaPausa(m, 120);
                cierto(m.pausado(), id + " repetir paso 2 pausa de nuevo");
                cierto(m.distanciaRecorrida() > 20, id + " repetir paso 2 vuelve a moverse");

                // Paso 3: Radar y disparo (VisorTactico activo, detecta al muñeco y dispara).
                m.continuar();
                igual(2, m.indicePaso(), id + " paso 3");
                cierto(m.visorTactico(), id + " paso 3 activa el visor táctico");
                falso(m.pausado(), id + " paso 3 arranca en vivo");
                correrHastaPausa(m, 200);
                cierto(m.pausado(), id + " paso 3 hace pausa automática");
                cierto(m.enemigoDetectado(), id + " paso 3 detecta al muñeco con el radar");
                cierto(m.disparoRealizado(), id + " paso 3 apunta y dispara");
                cierto(m.ultimoError().isEmpty(), id + " sin error en paso 3");
                m.alternarVisor();
                falso(m.visorTactico(), id + " visor táctico desactivable");
                m.alternarVisor();
                cierto(m.visorTactico(), id + " visor táctico reactivable");

                // Paso 4: Ir a pelear y pausa entre rondas.
                m.continuar();
                igual(3, m.indicePaso(), id + " paso 4");
                cierto(m.esUltimoPaso(), id + " paso 4 es el último");
                cierto(m.pausado(), id + " paso 4 pausado");
                cierto(m.ultimoError().isEmpty(), id + " sin error en paso 4");
                m.continuar();
                cierto(m.terminada(), id + " demo finalizada al continuar tras el último paso");
            }
        });
    }

    private static void correrHastaPausa(MotorDemostracion m, int maxTurnos) {
        for (int i = 0; i < maxTurnos && !m.pausado(); i++) {
            m.avanzarTurno();
        }
    }
}
