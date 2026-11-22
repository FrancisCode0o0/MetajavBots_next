package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.util.BitSet;
import java.util.List;

import metajavbots.codigo.Compilador;
import metajavbots.codigo.Vigilante;
import metajavbots.combate.Arena;
import metajavbots.ejercicios.Demostracion;
import metajavbots.ejercicios.MotorDemostracion;
import metajavbots.pantalla.TextoEnriquecido;
import metajavbots.pantalla.VistaDeArena;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/**
 * Pruebas deterministas de Demo 3 (radar, puntería, proyectiles y obstáculos de la arena):
 * carga trilingüe, límites de líneas resaltadas (▶), compilación sin fugas de API entre
 * idiomas y ejecución en vivo coherente con Pelea, Arena y Proyectil.
 */
final class PruebasDeDemo3 {

    private PruebasDeDemo3() {
    }

    static void apuntar() {
        apuntarCargaTrilingueYCompilacion();
        apuntarEjecucionEnVivoYCoherencia();
    }

    private static void apuntarCargaTrilingueYCompilacion() {
        caso("demo 3: carga trilingüe de 7 pasos (ES, DE, EN), líneas ▶, API oficial y compilación sin fugas", () -> {
            cierto(Demostracion.todas().size() >= 3, "deben existir Demo 1, Demo 2 y Demo 3");
            Demostracion d3 = Demostracion.de(Demostracion.ID_DEMO_3);
            cierto(d3 != null, "Demo 3 accesible por id");
            igual(3, d3.nivel, "Demo 3 es nivel 3");
            igual(VistaDeArena.Escenario.CHATARRERIA, d3.escenario, "Demo 3 usa escenario CHATARRERIA");
            igual(7, d3.pasos.size(), "Demo 3 tiene 7 pasos");

            String[] idsEsperados = {
                "radar_evento_lista", "punteria_y_alcance", "obstaculos_arena",
                "entorno_ingenuo", "ajustar_entorno", "entorno_ajustado", "resumen_entorno"
            };
            for (int i = 0; i < idsEsperados.length; i++) {
                igual(idsEsperados[i], d3.pasos.get(i).id, "demo 3 paso " + (i + 1));
            }
            cierto(!d3.titulo(Idioma.ES).equals(d3.titulo(Idioma.DE)), "Demo 3 título distinto ES/DE");
            cierto(!d3.titulo(Idioma.ES).equals(d3.titulo(Idioma.EN)), "Demo 3 título distinto ES/EN");

            for (Idioma id : Idioma.values()) {
                String nivelTxt = T.en("demo.nivel.3", id);
                cierto(nivelTxt != null && !nivelTxt.isBlank() && !nivelTxt.equals("demo.nivel.3"),
                        "clave demo.nivel.3 válida en " + id);
                verificarDemo3EnIdioma(d3, id);
            }
        });
    }

    private static void verificarDemo3EnIdioma(Demostracion d3, Idioma id) {
        String ingenuo = d3.pasos.get(0).codigo(d3, id);
        String ajustado = d3.pasos.get(4).codigo(d3, id);
        cierto(!ingenuo.equals(ajustado), "Demo 3 cambia de código ingenuo a ajustado en " + id);

        for (Demostracion.Paso p : d3.pasos) {
            String cod = p.codigo(d3, id);
            String[] lineas = cod.split("\n");
            cierto(!p.titulo(id).isBlank(), p.id + " título no vacío en " + id);
            cierto(!p.texto(id).isBlank(), p.id + " texto no vacío en " + id);
            cierto(p.lineaPrincipal >= 1 && p.lineaPrincipal <= lineas.length,
                    p.id + " línea principal fuera de rango en " + id + ": " + p.lineaPrincipal);
            for (int ls : p.lineasSecundarias) {
                cierto(ls >= 1 && ls <= lineas.length,
                        p.id + " línea secundaria fuera de rango en " + id + ": " + ls);
            }
            verificarParidadSinFugasTexto(id, p.texto(id));
        }

        verificarApiSinFugasCodigo(id, ingenuo, ajustado);
        if (Compilador.hayCompilador()) {
            Compilador.Resultado rIng = Compilador.compilar(ingenuo, true, Vigilante.Modo.ROBOT, id.locale());
            cierto(rIng.bien, "código ingenuo de Demo 3 debe compilar en " + id + ": " + rIng.errores);
            Compilador.Resultado rAju = Compilador.compilar(ajustado, true, Vigilante.Modo.ROBOT, id.locale());
            cierto(rAju.bien, "código ajustado de Demo 3 debe compilar en " + id + ": " + rAju.errores);
        }
    }

    private static void verificarApiSinFugasCodigo(Idioma id, String ingenuo, String ajustado) {
        String todo = ingenuo + "\n" + ajustado;
        if (id == Idioma.ES) {
            cierto(ingenuo.contains("extends Metajavbot"), "ES hereda de Metajavbot");
            cierto(todo.contains("alVerEnemigo(") && todo.contains("enemigosVistos()")
                    && todo.contains("obstaculos()"), "ES usa API oficial");
            falso(todo.contains("gesehen(") || todo.contains("hindernisse(")
                    || todo.contains("seenEnemies(") || todo.contains("obstacles("),
                    "ES sin fugas de API DE/EN");
        } else if (id == Idioma.DE) {
            cierto(ingenuo.contains("extends MetajavbotDe"), "DE hereda de MetajavbotDe");
            cierto(todo.contains("beiSicht(") && todo.contains("gesehen()")
                    && todo.contains("hindernisse()"), "DE usa método oficial gesehen() y hindernisse()");
            falso(todo.contains("geseheneGegner(") || todo.contains("enemigosVistos(")
                    || todo.contains("obstaculos(") || todo.contains("seenEnemies(")
                    || todo.contains("obstacles("), "DE sin alias inexistentes ni fugas ES/EN");
        } else {
            cierto(ingenuo.contains("extends MetajavbotEn"), "EN hereda de MetajavbotEn");
            cierto(todo.contains("onSeeEnemy(") && todo.contains("seenEnemies()")
                    && todo.contains("obstacles()"), "EN usa API oficial");
            falso(todo.contains("enemigosVistos(") || todo.contains("obstaculos(")
                    || todo.contains("gesehen(") || todo.contains("hindernisse("),
                    "EN sin fugas de API ES/DE");
        }
    }

    private static void verificarParidadSinFugasTexto(Idioma id, String texto) {
        if (id == Idioma.EN) {
            falso(texto.contains("armour"), "EN usa grafía estadounidense armor");
            falso(texto.contains("towards"), "EN usa grafía estadounidense toward");
            falso(texto.contains("backwards"), "EN usa grafía estadounidense backward");
            falso(texto.contains("100 %"), "EN usa 100% sin espacio");
        } else if (id == Idioma.DE) {
            falso(texto.contains("geseheneGegner"), "DE no cita alias inexistente");
        }
    }

    private static void apuntarEjecucionEnVivoYCoherencia() {
        caso("demo 3: ejecución en vivo de los 7 pasos, pausas, repetir paso y coherencia con Arena/Proyectil", () -> {
            Demostracion d3 = Demostracion.de(Demostracion.ID_DEMO_3);
            for (Idioma id : Idioma.values()) {
                verificarRecorridoDemo3(d3, id);
            }
        });
    }

    private static void verificarRecorridoDemo3(Demostracion d3, Idioma id) {
        MotorDemostracion m = new MotorDemostracion(d3, id);

        // Pasos 1, 2 y 3: explicaciones teóricas pausadas con arena CHATARRERIA cargada.
        for (int paso = 0; paso < 3; paso++) {
            igual(paso, m.indicePaso(), id + " demo 3 paso " + (paso + 1));
            cierto(m.pausado(), id + " demo 3 paso " + (paso + 1) + " pausado");
            cierto(m.visorTactico(), id + " demo 3 paso " + (paso + 1) + " visor activo");
            cierto(!m.pelea().arena.trampas().isEmpty(), id + " demo 3 carga arena real con trampas y barriles");
            cierto(m.ultimoError().isEmpty(), id + " demo 3 sin error en paso " + (paso + 1));
            m.continuar();
        }

        // Paso 4: comportamiento ingenuo en vivo (dispara sin evaluar entorno; barril bloquea, rival intacto).
        igual(3, m.indicePaso(), id + " demo 3 paso 4");
        falso(m.pausado(), id + " demo 3 paso 4 arranca en vivo");
        correrHastaPausa(m, 100);
        cierto(m.pausado(), id + " demo 3 paso 4 pausa automática");
        cierto(m.enemigoDetectado(), id + " demo 3 paso 4 detecta al muñeco");
        cierto(m.disparoRealizado(), id + " demo 3 paso 4 dispara");
        falso(m.impactoRealizado(), id + " demo 3 paso 4 no alcanza al muñeco por el barril y el tiro desviado");
        falso(m.obstaculoDestruido(), id + " demo 3 paso 4 no llega a despejar el barril");
        igual(100.0, m.pelea().cuerpo(1).blindaje(), id + " demo 3 paso 4 muñeco intacto al 100 %");
        igual(7, m.lineaActiva(), id + " demo 3 paso 4 señala línea culpable 7");
        igual(Demostracion.Resaltado.ERROR, m.resaltadoActual(), id + " demo 3 paso 4 resalta en rojo");
        cierto(m.ultimoError().isEmpty(), id + " demo 3 sin error en paso 4");

        // Repetir paso 4 reinicia métricas y vuelve a pausar correctamente.
        m.repetirPaso();
        falso(m.pausado(), id + " repetir paso 4 vuelve a ejecutar");
        falso(m.disparoRealizado(), id + " repetir paso 4 limpia disparoRealizado");
        correrHastaPausa(m, 100);
        cierto(m.pausado() && m.disparoRealizado() && !m.impactoRealizado(),
                id + " repetir paso 4 reproduce el comportamiento ingenuo");

        // Paso 5: mostrar código ajustado.
        m.continuar();
        igual(4, m.indicePaso(), id + " demo 3 paso 5");
        cierto(m.pausado(), id + " demo 3 paso 5 pausado");
        igual(7, m.lineaActiva(), id + " demo 3 paso 5 señala bucle de obstáculos en línea 7");
        igual(Demostracion.Resaltado.BIEN, m.resaltadoActual(), id + " demo 3 paso 5 resalta en verde");

        // Paso 6: versión ajustada en vivo (rompe barril destructible, pinchos fijos intactos y acierta al muñeco).
        m.continuar();
        igual(5, m.indicePaso(), id + " demo 3 paso 6");
        falso(m.pausado(), id + " demo 3 paso 6 arranca en vivo");
        cierto(m.resaltarHudBarras(), id + " demo 3 paso 6 resalta barras HUD");
        int obstaculosAntes = m.pelea().cuerpo(0).obstaculos().size();
        boolean huboProyectilEnVuelo = false;
        for (int t = 0; t < 260 && !m.pausado(); t++) {
            m.avanzarTurno();
            if (!m.pelea().proyectiles.isEmpty()) huboProyectilEnVuelo = true;
        }
        cierto(m.pausado(), id + " demo 3 paso 6 pausa automática tras abrir paso y acertar");
        cierto(huboProyectilEnVuelo, id + " demo 3 paso 6 lanza proyectiles reales");
        cierto(m.obstaculoDestruido(), id + " demo 3 paso 6 destruye el barril bloqueante");
        cierto(m.pelea().cuerpo(0).obstaculos().size() < obstaculosAntes,
                id + " demo 3 paso 6 el barril destruido desaparece de obstaculos()");
        verificarTrampasFijasIntactas(m.pelea().arena.trampas(), id);
        cierto(m.enemigoDetectado() && m.disparoRealizado() && m.impactoRealizado(),
                id + " demo 3 paso 6 detecta, dispara y acierta tras romper el barril");
        cierto(m.pelea().cuerpo(1).blindaje() < 100.0, id + " demo 3 paso 6 reduce blindaje del muñeco");
        BitSet activas6 = m.lineasActivas();
        cierto(activas6.get(30) && activas6.get(31), id + " demo 3 paso 6 resalta comprobación y disparo");
        cierto(m.ultimoError().isEmpty(), id + " demo 3 sin error en paso 6");

        // Repetir paso 6 reinicia arena y vuelve a destruir barril e impactar al rival.
        m.repetirPaso();
        falso(m.pausado(), id + " repetir paso 6 vuelve a ejecutar");
        falso(m.obstaculoDestruido(), id + " repetir paso 6 reinicia obstaculoDestruido");
        correrHastaPausa(m, 260);
        cierto(m.pausado() && m.obstaculoDestruido() && m.impactoRealizado(),
                id + " repetir paso 6 reproduce la versión ajustada");

        // Paso 7: cierre pedagógico.
        m.continuar();
        igual(6, m.indicePaso(), id + " demo 3 paso 7");
        cierto(m.esUltimoPaso(), id + " demo 3 paso 7 es el último");
        cierto(m.pausado(), id + " demo 3 paso 7 pausado");
        cierto(TextoEnriquecido.tieneMarcas(m.pasoActual().texto(id)), id + " demo 3 paso 7 usa marcas [[...]]");
        cierto(m.ultimoError().isEmpty(), id + " demo 3 sin error en paso 7");
        m.continuar();
        cierto(m.terminada(), id + " demo 3 finalizada al continuar tras el último paso");
    }

    private static void verificarTrampasFijasIntactas(List<Arena.Trampa> trampas, Idioma id) {
        boolean hayPinchosActivos = false;
        for (Arena.Trampa t : trampas) {
            if (!t.destructible) {
                cierto(t.activa() && !t.destruida(), id + " trampa fija " + t.tipo + " sigue intacta");
                if ("pinchos".equals(t.tipo)) hayPinchosActivos = true;
            }
        }
        cierto(hayPinchosActivos, id + " existen pinchos fijos activos en CHATARRERIA");
    }

    private static void correrHastaPausa(MotorDemostracion m, int maxTurnos) {
        for (int i = 0; i < maxTurnos && !m.pausado(); i++) {
            m.avanzarTurno();
        }
    }
}
