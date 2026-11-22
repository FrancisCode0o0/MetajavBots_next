package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import metajavbots.codigo.Compilador;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.pantallas.PrevisualizacionDeEjercicio;
import metajavbots.textos.Idioma;
import metajavbots.tutor.ConocimientoDelEntrenador;

/**
 * PRUEBAS DE DIVERSIDAD DE EJERCICIOS — Verifica la ampliación del banco al
 * nivel de CodingBat y CodeStepByStep: crecimiento mínimo, cobertura por
 * categorías y dificultades, compilación y casos de solución/plantilla,
 * previsualización válida, unicidad de identificadores y títulos y paridad
 * trilingüe normativa.
 */
public final class PruebasDeDiversidadEjercicios {

    private static final int TOTAL_PREVIO = 272;
    private static final int CRECIMIENTO_MINIMO = 32;

    private static final List<String> IDS_NUEVOS = List.of(
            "t04_siesta_en_taller", "t04_cerca_de_cien", "t04_suma_sin_clones", "t04_cerca_y_lejos", "t04_placas_de_blindaje",
            "t05_sumar_pares_rango", "t05_potencia_de_dos", "t05_mesetas_de_energia", "t05_pasos_calibracion", "t05_piramide_circuitos",
            "t07_rotar_izquierda", "t07_picos_del_radar", "t07_serie_creciente", "t07_mayor_alcance", "t07_equilibrar_chasis",
            "t07_transponer_matriz",
            "t08_etiqueta_doble", "t08_codigo_sandwich", "t08_empate_bip_zap", "t08_quitar_interferencias", "t08_extremos_espejo",
            "t08_sumar_numeros_texto",
            "t10_bateria_recargable", "t11_sensor_calibrado", "t11_cuenta_vip", "t14_lote_de_baterias",
            "t15_unicos_ordenados", "t15_mapa_longitudes", "t15_agrupar_por_inicial", "t15_intercambiar_parejas",
            "t15_diferencia_simetrica",
            "t21_contar_sietes", "t21_limpiar_dobles", "t21_caminos_cuadricula", "t21_suma_sin_vecinos", "t21_dividir_en_dos");

    private static final Pattern API_ES = Pattern.compile(
            "\\b(avanzar|retroceder|girar|apuntar|apuntarHacia|atacar|memoria|obstaculos|alVerEnemigo|cadaTurno|enemigosVistos)\\(\\)");

    private PruebasDeDiversidadEjercicios() {
    }

    /** Registra las pruebas de diversidad y calidad del banco ampliado. */
    public static void apuntar() {
        caso("banco ampliado: crecimiento >= 32, categorías completas y progresión de dificultad 1-2, 3 y 4-5",
                PruebasDeDiversidadEjercicios::probarCrecimientoYCobertura);
        caso("banco ampliado: todos los ejercicios nuevos compilan solución y plantilla y pasan 100 % de casos",
                PruebasDeDiversidadEjercicios::probarCompilacionYCasos);
        caso("banco ampliado: todos los ejercicios nuevos tienen previsualización válida y sin títulos ni ids repetidos",
                PruebasDeDiversidadEjercicios::probarPrevisualizacionYUnicidad);
        caso("banco ampliado: paridad trilingüe normativa en ejercicios nuevos y conocimiento del entrenador",
                PruebasDeDiversidadEjercicios::probarParidadYEntrenador);
    }

    private static void probarCrecimientoYCobertura() {
        Banco banco = Banco.delJuego();
        List<Ejercicio> todos = banco.todos();
        cierto(todos.size() >= TOTAL_PREVIO + CRECIMIENTO_MINIMO,
                "el banco debe crecer al menos en " + CRECIMIENTO_MINIMO + " (actual: " + todos.size() + ")");
        cierto(IDS_NUEVOS.size() >= CRECIMIENTO_MINIMO, "lista de nuevos >= " + CRECIMIENTO_MINIMO);

        int faciles = 0;
        int medios = 0;
        int dificiles = 0;
        Map<String, Integer> porTema = new HashMap<>();
        for (String id : IDS_NUEVOS) {
            Ejercicio e = banco.ejercicio(id);
            cierto(e != null, "falta el ejercicio nuevo: " + id);
            porTema.merge(e.tema, 1, Integer::sum);
            if (e.dificultad <= 2) faciles++;
            else if (e.dificultad == 3) medios++;
            else dificiles++;
        }
        cierto(faciles >= 10, "al menos 10 ejercicios nuevos fáciles (1-2): " + faciles);
        cierto(medios >= 10, "al menos 10 ejercicios nuevos intermedios (3): " + medios);
        cierto(dificiles >= 10, "al menos 10 ejercicios nuevos difíciles (4-5): " + dificiles);
        for (String tema : List.of("04", "05", "07", "08", "10", "11", "14", "15", "21")) {
            cierto(porTema.getOrDefault(tema, 0) >= 1, "debe haber ejercicios nuevos en el tema " + tema);
        }
    }

    private static void probarCompilacionYCasos() {
        if (!Compilador.hayCompilador()) return;
        Banco banco = Banco.delJuego();
        for (String id : IDS_NUEVOS) {
            Ejercicio e = banco.ejercicio(id);
            cierto(e != null, "existe " + id);
            if (e.java > Compilador.versionDeJava()) continue;
            Comprobador.Informe sol = Comprobador.comprobar(e, e.codigoSolucion(), Locale.ROOT);
            cierto(sol.compila(), id + ": la solución debe compilar: " + sol.errores);
            cierto(sol.todoBien(), id + ": la solución debe pasar todos los casos");
            Comprobador.Informe traza = Comprobador.comprobar(e, e.codigoSolucion(), Locale.ROOT, true);
            cierto(traza.todoBien(), id + ": la solución con traza debe pasar todos los casos");
            Comprobador.Informe pl = Comprobador.comprobar(e, e.codigoInicial(), Locale.ROOT);
            cierto(pl.compila(), id + ": la plantilla inicial debe compilar: " + pl.errores);
            falso(pl.todoBien(), id + ": la plantilla inicial no debe pasar ya todos los casos");
        }
    }

    private static void probarPrevisualizacionYUnicidad() {
        Banco banco = Banco.delJuego();
        Set<String> ids = new HashSet<>();
        Set<String> titEs = new HashSet<>();
        Set<String> titDe = new HashSet<>();
        Set<String> titEn = new HashSet<>();
        Set<String> nuevos = new HashSet<>(IDS_NUEVOS);
        for (Ejercicio e : banco.todos()) {
            cierto(ids.add(e.id), "id repetido: " + e.id);
            if (!nuevos.contains(e.id)) {
                titEs.add(e.titulo.es.strip().toLowerCase(Locale.ROOT));
                titDe.add(e.titulo.de.strip().toLowerCase(Locale.ROOT));
                titEn.add(e.titulo.en.strip().toLowerCase(Locale.ROOT));
            }
        }
        for (String id : IDS_NUEVOS) {
            Ejercicio e = banco.ejercicio(id);
            cierto(e != null, "existe " + id);
            cierto(titEs.add(e.titulo.es.strip().toLowerCase(Locale.ROOT)), "título ES repetido: " + e.titulo.es);
            cierto(titDe.add(e.titulo.de.strip().toLowerCase(Locale.ROOT)), "título DE repetido: " + e.titulo.de);
            cierto(titEn.add(e.titulo.en.strip().toLowerCase(Locale.ROOT)), "título EN repetido: " + e.titulo.en);
            for (Idioma idioma : Idioma.values()) {
                PrevisualizacionDeEjercicio.Escenario esc = PrevisualizacionDeEjercicio.de(e, idioma);
                cierto(esc != null && esc.escenaId() != null && !esc.escenaId().isBlank(),
                        id + " (" + idioma + "): escena de previsualización válida");
                cierto(esc.formaId() != null && !esc.formaId().isBlank(),
                        id + " (" + idioma + "): forma de previsualización válida");
            }
        }
    }

    private static void probarParidadYEntrenador() {
        Banco banco = Banco.delJuego();
        for (String id : IDS_NUEVOS) {
            Ejercicio e = banco.ejercicio(id);
            cierto(e != null, "existe " + id);
            for (String s : new String[]{e.titulo.es, e.titulo.de, e.titulo.en, e.enunciado.es, e.enunciado.de, e.enunciado.en,
                e.pista.es, e.pista.de, e.pista.en}) {
                cierto(s != null && !s.isBlank(), id + ": sin textos vacíos en ES/DE/EN");
            }
            cierto(!e.titulo.es.equals(e.titulo.de) && !e.titulo.es.equals(e.titulo.en), id + ": título traducido en DE y EN");
            cierto(!e.enunciado.es.equals(e.enunciado.de) && !e.enunciado.es.equals(e.enunciado.en), id + ": enunciado traducido");
            cierto(!e.pista.es.equals(e.pista.de) && !e.pista.es.equals(e.pista.en), id + ": pista traducida");
            for (String s : new String[]{e.enunciado.de, e.enunciado.en, e.pista.de, e.pista.en}) {
                falso(API_ES.matcher(s).find(), id + ": fuga de API en español en DE/EN");
            }
        }
        ConocimientoDelEntrenador.Entrada entrada = ConocimientoDelEntrenador.de("minijuegos_y_ejercicios");
        cierto(entrada != null, "existe ficha minijuegos_y_ejercicios");
        String es = String.join(" ", entrada.parrafos().get(Idioma.ES)).toLowerCase(Locale.ROOT);
        String de = String.join(" ", entrada.parrafos().get(Idioma.DE)).toLowerCase(Locale.ROOT);
        String en = String.join(" ", entrada.parrafos().get(Idioma.EN)).toLowerCase(Locale.ROOT);
        cierto(es.contains("300") && es.contains("codestepbystep"), "entrenador ES refleja la ampliación y progresión");
        cierto(de.contains("300") && de.contains("codestepbystep"), "entrenador DE refleja la ampliación y progresión");
        cierto(en.contains("300") && en.contains("codestepbystep"), "entrenador EN refleja la ampliación y progresión");
    }
}
