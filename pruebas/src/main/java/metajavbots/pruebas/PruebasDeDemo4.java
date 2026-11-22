package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.HashSet;
import java.util.Set;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.combate.Cuerpo;
import metajavbots.combate.ModificadoresPoo;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.MotorDemostracion;
import metajavbots.pantalla.TextoEnriquecido;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * Pruebas deterministas de Demo 4 (POO en combate, aparatos, energía y tope de
 * cálculo): carga trilingüe, validez de líneas resaltadas (▶), compilación real
 * sin fugas de API entre idiomas y ejecución en vivo coherente con
 * {@code ModificadoresPoo}, {@code Aparatos}, energía y {@code TopeDeCalculo}.
 */
final class PruebasDeDemo4 {

    private PruebasDeDemo4() {
    }

    static void apuntar() {
        apuntarCargaTrilingueYCompilacion();
        apuntarEjecucionEnVivoYCoherencia();
    }

    private static void apuntarCargaTrilingueYCompilacion() {
        caso("demo 4: carga trilingüe de 8 pasos (ES, DE, EN), líneas ▶, API oficial y compilación sin fugas", () -> {
            cierto(Demostracion.todas().size() >= 4, "deben existir Demo 1 a Demo 4");
            Demostracion d4 = Demostracion.de(Demostracion.ID_DEMO_4);
            cierto(d4 != null, "Demo 4 accesible por id");
            igual(4, d4.nivel, "Demo 4 es nivel 4");
            igual(VistaDeArena.Escenario.NEON, d4.escenario, "Demo 4 usa escenario NEON");
            igual(8, d4.pasos.size(), "Demo 4 tiene 8 pasos");

            String[] idsEsperados = {
                "poo_expuesto", "poo_encapsulado", "aparato_hueco_error", "aparato_hueco_valido",
                "gestion_energia", "tope_deuda", "tope_optimizado", "resumen_arquitectura"
            };
            for (int i = 0; i < idsEsperados.length; i++) {
                igual(idsEsperados[i], d4.pasos.get(i).id, "demo 4 paso " + (i + 1));
            }
            cierto(!d4.titulo(Idioma.ES).equals(d4.titulo(Idioma.DE)), "Demo 4 título distinto ES/DE");
            cierto(!d4.titulo(Idioma.ES).equals(d4.titulo(Idioma.EN)), "Demo 4 título distinto ES/EN");

            for (Idioma id : Idioma.values()) {
                String nivelTxt = T.en("demo.nivel.4", id);
                cierto(nivelTxt != null && !nivelTxt.isBlank() && !nivelTxt.equals("demo.nivel.4"),
                        "clave demo.nivel.4 válida en " + id);
                verificarDemo4EnIdioma(d4, id);
            }
        });
    }

    private static void verificarDemo4EnIdioma(Demostracion d4, Idioma id) {
        Set<String> fragmentos = new HashSet<>();
        StringBuilder todoCodigo = new StringBuilder();
        for (Demostracion.Paso p : d4.pasos) {
            String cod = p.codigo(d4, id);
            String[] lineas = cod.split("\n");
            cierto(!p.titulo(id).isBlank(), p.id + " título no vacío en " + id);
            cierto(!p.texto(id).isBlank(), p.id + " texto no vacío en " + id);
            cierto(p.lineaPrincipal >= 1 && p.lineaPrincipal <= lineas.length,
                    p.id + " línea principal fuera de rango en " + id + ": " + p.lineaPrincipal);
            for (int ls : p.lineasSecundarias) {
                cierto(ls >= 1 && ls <= lineas.length,
                        p.id + " línea secundaria fuera de rango en " + id + ": " + ls);
            }
            verificarParidadTexto(id, p.texto(id));
            if (fragmentos.add(cod)) {
                todoCodigo.append(cod).append('\n');
                if (Compilador.hayCompilador()) {
                    Compilador.Resultado res = Compilador.compilar(cod, true, Vigilante.Modo.ROBOT, id.locale());
                    cierto(res.bien, "fragmento de Demo 4 (" + p.id + ") debe compilar en " + id + ": " + res.errores);
                }
            }
        }
        igual(7, fragmentos.size(), "Demo 4 tiene 7 fragmentos de código distintos en " + id);
        verificarApiSinFugasCodigo(id, todoCodigo.toString());
    }

    private static void verificarApiSinFugasCodigo(Idioma id, String todo) {
        if (id == Idioma.ES) {
            cierto(todo.contains("extends Metajavbot"), "ES hereda de Metajavbot");
            cierto(todo.contains("miAparato()") && todo.contains("conectar(") && todo.contains("usarAparato()"),
                    "ES usa API oficial de aparatos");
            cierto(todo.contains("enemigosVistos()") && todo.contains("energia()"), "ES usa API oficial");
            falso(todo.contains("meinGeraet(") || todo.contains("anschliessen(") || todo.contains("gesehen(")
                    || todo.contains("myGadget(") || todo.contains("plugIn(") || todo.contains("seenEnemies("),
                    "ES sin fugas de API DE/EN");
        } else if (id == Idioma.DE) {
            cierto(todo.contains("extends MetajavbotDe"), "DE hereda de MetajavbotDe");
            cierto(todo.contains("meinGeraet()") && todo.contains("anschliessen(") && todo.contains("geraetBenutzen()"),
                    "DE usa API oficial de aparatos (meinGeraet/anschliessen)");
            cierto(todo.contains("gesehen()") && todo.contains("energie()"), "DE usa gesehen() y energie()");
            falso(todo.contains("geseheneGegner(") || todo.contains("miAparato(") || todo.contains("conectar(")
                    || todo.contains("myGadget(") || todo.contains("plugIn(") || todo.contains("seenEnemies("),
                    "DE sin fugas ES/EN ni alias inexistentes");
        } else {
            cierto(todo.contains("extends MetajavbotEn"), "EN hereda de MetajavbotEn");
            cierto(todo.contains("myGadget()") && todo.contains("plugIn(") && todo.contains("useGadget()"),
                    "EN usa API oficial de aparatos (myGadget/plugIn/useGadget)");
            cierto(todo.contains("seenEnemies()") && todo.contains("energy()"), "EN usa seenEnemies() y energy()");
            falso(todo.contains("miAparato(") || todo.contains("conectar(") || todo.contains("enemigosVistos(")
                    || todo.contains("meinGeraet(") || todo.contains("anschliessen(") || todo.contains("gesehen("),
                    "EN sin fugas ES/DE");
        }
    }

    private static void verificarParidadTexto(Idioma id, String texto) {
        if (id == Idioma.EN) {
            falso(texto.contains("armour"), "EN usa grafía estadounidense armor");
            falso(texto.contains("towards"), "EN usa grafía estadounidense toward");
            falso(texto.contains("backwards"), "EN usa grafía estadounidense backward");
            falso(texto.contains(" %"), "EN usa {0}% sin espacio");
        } else if (id == Idioma.DE) {
            falso(texto.contains("geseheneGegner"), "DE no cita alias inexistente");
        }
    }

    private static void apuntarEjecucionEnVivoYCoherencia() {
        caso("demo 4: ejecución en vivo de los 8 pasos, pausas, repetir y coherencia con POO, aparatos, energía y tope", () -> {
            Demostracion d4 = Demostracion.de(Demostracion.ID_DEMO_4);
            for (Idioma id : Idioma.values()) {
                verificarRecorridoDemo4(d4, id);
            }
        });
    }

    private static void verificarRecorridoDemo4(Demostracion d4, Idioma id) {
        MotorDemostracion m = new MotorDemostracion(d4, id);

        // Paso 1: POO expuesto (2 campos public mutables -> penalización real).
        double danoExpuesto = verificarPaso1PooExpuesto(m, id);

        // Paso 2: POO encapsulado (private + final + static -> menos daño y mayor presupuesto de pasos).
        m.continuar();
        verificarPaso2PooEncapsulado(m, id, danoExpuesto);

        // Paso 3: Aparato en hueco erróneo (aparece, recoge, falla conectar("cabeza"), sigue desconectado).
        m.continuar();
        verificarPaso3AparatoError(m, id);

        // Paso 4: Aparato en hueco válido (recoge, conecta en espalda, monta y activa).
        m.continuar();
        verificarPaso4AparatoBien(m, id);

        // Paso 5: Gestión de energía (gasta energía al atacar y recarga por turno).
        m.continuar();
        verificarPaso5Energia(m, id);

        // Paso 6: Tope de cálculo (TopeDeCalculo + deuda de turnos real).
        m.continuar();
        verificarPaso6TopeDeuda(m, id);

        // Paso 7: Versión optimizada (0 topes, 0 deuda, impacta al rival).
        m.continuar();
        verificarPaso7TopeOptimo(m, id);

        // Paso 8: Cierre pedagógico.
        m.continuar();
        igual(7, m.indicePaso(), id + " demo 4 paso 8");
        cierto(m.esUltimoPaso() && m.pausado(), id + " demo 4 paso 8 es último y pausado");
        cierto(TextoEnriquecido.tieneMarcas(m.pasoActual().texto(id)), id + " demo 4 paso 8 usa marcas [[...]]");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 8");
        m.continuar();
        cierto(m.terminada(), id + " demo 4 finalizada al continuar tras el último paso");
    }

    private static double verificarPaso1PooExpuesto(MotorDemostracion m, Idioma id) {
        igual(0, m.indicePaso(), id + " demo 4 paso 1");
        falso(m.pausado(), id + " demo 4 paso 1 arranca en vivo");
        ModificadoresPoo ef = m.pelea().cuerpo(0).efectosPoo();
        igual(2, ef.camposPublicosMutables, id + " paso 1 tiene 2 campos public mutables");
        cierto(ef.factorDanoRecibido > 1.05, id + " paso 1 penaliza daño recibido: " + ef.factorDanoRecibido);
        cierto(ef.factorRadarRival > 1.10, id + " paso 1 expone al radar rival: " + ef.factorRadarRival);
        correrHastaPausa(m, 80);
        cierto(m.pausado(), id + " demo 4 paso 1 pausa automática");
        cierto(m.danoRecibidoDemo() > 0.1, id + " demo 4 paso 1 recibe daño real: " + m.danoRecibidoDemo());
        igual(Demostracion.Resaltado.ERROR, m.resaltadoActual(), id + " paso 1 resalta en rojo");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 1");
        return m.danoRecibidoDemo();
    }

    private static void verificarPaso2PooEncapsulado(MotorDemostracion m, Idioma id, double danoExpuesto) {
        igual(1, m.indicePaso(), id + " demo 4 paso 2");
        falso(m.pausado(), id + " demo 4 paso 2 arranca en vivo");
        ModificadoresPoo ef = m.pelea().cuerpo(0).efectosPoo();
        cierto(ef.camposPrivados >= 2 && ef.metodosPrivados >= 1, id + " paso 2 encapsula con private");
        cierto(ef.camposFinal >= 2, id + " paso 2 declara constantes final");
        cierto(ef.camposStatic >= 1, id + " paso 2 declara campo static");
        igual(0, ef.camposPublicosMutables, id + " paso 2 sin campos public mutables");
        cierto(ef.factorDanoRecibido < 1.0, id + " paso 2 reduce daño recibido: " + ef.factorDanoRecibido);
        cierto(ef.factorPasos > 1.05, id + " paso 2 amplía tope de pasos: " + ef.factorPasos);
        correrHastaPausa(m, 80);
        cierto(m.pausado(), id + " demo 4 paso 2 pausa automática");
        cierto(m.danoRecibidoDemo() > 0.1 && m.danoRecibidoDemo() < danoExpuesto,
                id + " paso 2 recibe menos daño (" + m.danoRecibidoDemo() + " < " + danoExpuesto + ")");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 2");
    }

    private static void verificarPaso3AparatoError(MotorDemostracion m, Idioma id) {
        igual(2, m.indicePaso(), id + " demo 4 paso 3");
        falso(m.pausado(), id + " demo 4 paso 3 arranca en vivo");
        correrHastaPausa(m, 180);
        cierto(m.pausado(), id + " demo 4 paso 3 pausa automática");
        cierto(m.aparatoRecogido(), id + " paso 3 recoge el aparato de la arena");
        cierto(m.conexionFallida(), id + " paso 3 registra rechazo de hueco erróneo");
        falso(m.aparatoConectado(), id + " paso 3 no llega a conectar el aparato");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 3");

        m.repetirPaso();
        falso(m.pausado() || m.aparatoRecogido() || m.conexionFallida(), id + " repetir paso 3 reinicia estado");
        correrHastaPausa(m, 180);
        cierto(m.pausado() && m.aparatoRecogido() && m.conexionFallida(), id + " repetir paso 3 reproduce el fallo");
    }

    private static void verificarPaso4AparatoBien(MotorDemostracion m, Idioma id) {
        igual(3, m.indicePaso(), id + " demo 4 paso 4");
        falso(m.pausado(), id + " demo 4 paso 4 arranca en vivo");
        correrHastaPausa(m, 230);
        cierto(m.pausado(), id + " demo 4 paso 4 pausa automática");
        cierto(m.aparatoRecogido(), id + " paso 4 recoge el aparato");
        cierto(m.aparatoConectado(), id + " paso 4 conecta en el hueco válido");
        cierto(m.aparatoUsado(), id + " paso 4 activa el aparato tras montarlo");
        falso(m.conexionFallida(), id + " paso 4 sin errores de hueco");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 4");
    }

    private static void verificarPaso5Energia(MotorDemostracion m, Idioma id) {
        igual(4, m.indicePaso(), id + " demo 4 paso 5");
        falso(m.pausado(), id + " demo 4 paso 5 arranca en vivo");
        cierto(m.resaltarHudBarras(), id + " paso 5 resalta barras HUD de energía");
        double inicial = m.pelea().cuerpo(0).energia;
        correrHastaPausa(m, 140);
        cierto(m.pausado(), id + " demo 4 paso 5 pausa automática");
        cierto(m.energiaMinima() < inicial - 1.0, id + " paso 5 consume energía al atacar");
        cierto(m.energiaRecargada(), id + " paso 5 recupera energía con la recarga por turno");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 5");
    }

    private static void verificarPaso6TopeDeuda(MotorDemostracion m, Idioma id) {
        igual(5, m.indicePaso(), id + " demo 4 paso 6");
        falso(m.pausado(), id + " demo 4 paso 6 arranca en vivo");
        boolean vioEstadoDeuda = false;
        for (int t = 0; t < 60 && !m.pausado(); t++) {
            m.avanzarTurno();
            if (m.pelea().cuerpo(0).estadoPensamiento == Cuerpo.EstadoPensamiento.DEUDA_TOPE) {
                vioEstadoDeuda = true;
            }
        }
        cierto(m.pausado(), id + " demo 4 paso 6 pausa automática");
        cierto(m.topeAlcanzado() && m.pelea().informe(0).topes > 0, id + " paso 6 dispara TopeDeCalculo real");
        cierto(m.deudaObservada() && vioEstadoDeuda, id + " paso 6 entra en estado real DEUDA_TOPE");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 6");

        m.repetirPaso();
        falso(m.pausado() || m.topeAlcanzado() || m.deudaObservada(), id + " repetir paso 6 reinicia tope y deuda");
        correrHastaPausa(m, 60);
        cierto(m.pausado() && m.topeAlcanzado() && m.deudaObservada(), id + " repetir paso 6 reproduce el corte");
    }

    private static void verificarPaso7TopeOptimo(MotorDemostracion m, Idioma id) {
        igual(6, m.indicePaso(), id + " demo 4 paso 7");
        falso(m.pausado(), id + " demo 4 paso 7 arranca en vivo");
        correrHastaPausa(m, 130);
        cierto(m.pausado(), id + " demo 4 paso 7 pausa automática");
        igual(0, m.pelea().informe(0).topes, id + " paso 7 optimizado tiene 0 topes de cálculo");
        igual(0, m.pelea().cuerpo(0).deuda, id + " paso 7 optimizado tiene 0 deuda de turnos");
        falso(m.topeAlcanzado() || m.deudaObservada(), id + " paso 7 sin cortes ni deuda");
        cierto(m.impactoRealizado(), id + " paso 7 acierta al muñeco");
        cierto(m.ultimoError().isEmpty(), id + " demo 4 sin error en paso 7");
    }

    private static void correrHastaPausa(MotorDemostracion m, int maxTurnos) {
        for (int i = 0; i < maxTurnos && !m.pausado(); i++) {
            m.avanzarTurno();
        }
    }
}
