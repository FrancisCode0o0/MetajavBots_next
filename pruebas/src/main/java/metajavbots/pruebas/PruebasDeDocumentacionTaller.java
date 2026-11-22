package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import metajavbots.codigo.Paquetes;
import metajavbots.pantallas.PanelDePaquetes;
import metajavbots.pantallas.Taller;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;

/**
 * Pruebas automatizadas de la documentación estilo Javadoc del Taller:
 * etiqueta trilingüe, navegación por niveles con migas de pan, retroceso paso a paso
 * sin expulsar del Taller, completitud de fichas y ausencia de fugas de API entre ES/DE/EN.
 */
final class PruebasDeDocumentacionTaller {

    private static final String[] API_SOLO_ES = {"cadaTurno(", "alVerEnemigo(", "alRecibirGolpe(", "alEmpezar("};
    private static final String[] API_SOLO_DE = {"jedenZug(", "beiSicht(", "beiTreffer(", "beimStart("};
    private static final String[] API_SOLO_EN = {"onTick(", "onSeeEnemy(", "onHit(", "onStart("};

    private PruebasDeDocumentacionTaller() {
    }

    static void apuntar() {
        apuntarEtiquetas();
        apuntarNavegacionPorNiveles();
        apuntarDescripcionesYParidad();
    }

    private static void apuntarEtiquetas() {
        caso("documentación del taller: etiqueta clara y claves de navegación en ES, DE y EN", () -> {
            Trilingue pestana = T.todos().get("taller.paquetes");
            cierto(pestana != null, "existe clave taller.paquetes");
            igual("Documentación", pestana.en(Idioma.ES), "etiqueta ES");
            igual("Dokumentation", pestana.en(Idioma.DE), "etiqueta DE");
            igual("Documentation", pestana.en(Idioma.EN), "etiqueta EN");

            Trilingue atras = T.todos().get("paquetes.atras");
            cierto(atras != null, "existe clave paquetes.atras");
            igual("Atrás", atras.en(Idioma.ES), "atrás ES");
            igual("Zurück", atras.en(Idioma.DE), "atrás DE");
            igual("Back", atras.en(Idioma.EN), "atrás EN");

            Trilingue ejJava = T.todos().get("paquetes.miembro.ejemplo_java");
            cierto(ejJava != null, "existe clave paquetes.miembro.ejemplo_java");
            for (Idioma id : Idioma.values()) {
                cierto(!ejJava.en(id).isBlank(), "rótulo ejemplo Java no vacío en " + id);
            }
        });
    }

    private static void apuntarNavegacionPorNiveles() {
        caso("documentación del taller: navegación por 4 niveles, miga de pan y retroceso paso a paso sin salir del Taller", () -> {
            for (int previa : new int[]{0, 1, 2, 3}) {
                Taller taller = new Taller();
                taller.irAPestana(previa);
                igual(previa, taller.pestanaActual(), "pestaña inicial");
                taller.abrirDocumentacion();
                igual(4, taller.pestanaActual(), "entra en documentación");
                igual(previa, taller.pestanaAnterior(), "recuerda pestaña previa " + previa);

                PanelDePaquetes panel = taller.panelDePaquetes();
                igual(1, panel.nivelActual(), "nivel 1: índice");
                igual("Documentación", panel.rutaActual(Idioma.ES), "ruta nivel 1 ES");
                igual("Dokumentation", panel.rutaActual(Idioma.DE), "ruta nivel 1 DE");
                igual("Documentation", panel.rutaActual(Idioma.EN), "ruta nivel 1 EN");

                panel.abrirPaquete("metajavbots.api");
                igual(2, panel.nivelActual(), "nivel 2: paquete");
                igual("Documentación > metajavbots.api", panel.rutaActual(Idioma.ES), "ruta nivel 2 ES");

                panel.abrirComponente("metajavbots.api", "Metajavbot");
                igual(3, panel.nivelActual(), "nivel 3: clase");
                igual("Documentación > metajavbots.api > Metajavbot", panel.rutaActual(Idioma.ES), "ruta nivel 3 ES");
                igual("Dokumentation > metajavbots.api > MetajavbotDe", panel.rutaActual(Idioma.DE), "ruta nivel 3 DE");
                igual("Documentation > metajavbots.api > MetajavbotEn", panel.rutaActual(Idioma.EN), "ruta nivel 3 EN");

                panel.abrirMiembro("metajavbots.api", "Metajavbot", 0);
                igual(4, panel.nivelActual(), "nivel 4: ficha de método");
                cierto(panel.rutaActual(Idioma.ES).startsWith("Documentación > metajavbots.api > Metajavbot > "), "ruta nivel 4 ES");
                cierto(panel.rutaActual(Idioma.DE).contains("jedenZug()"), "ruta nivel 4 DE con método alemán");
                cierto(panel.rutaActual(Idioma.EN).contains("onTick()"), "ruta nivel 4 EN con método inglés");

                // Retroceso paso a paso: 4 -> 3 -> 2 -> 1 -> pestaña previa del Taller
                cierto(taller.retrocederEnDocumentacion(), "atrás 4->3 atendido");
                igual(4, taller.pestanaActual(), "sigue en documentación (nivel 3)");
                igual(3, panel.nivelActual(), "volvió a clase");

                cierto(taller.retrocederEnDocumentacion(), "atrás 3->2 atendido");
                igual(4, taller.pestanaActual(), "sigue en documentación (nivel 2)");
                igual(2, panel.nivelActual(), "volvió a paquete");

                cierto(taller.retrocederEnDocumentacion(), "atrás 2->1 atendido");
                igual(4, taller.pestanaActual(), "sigue en documentación (nivel 1)");
                igual(1, panel.nivelActual(), "volvió al índice");

                cierto(taller.retrocederEnDocumentacion(), "atrás desde índice vuelve a pestaña previa");
                igual(previa, taller.pestanaActual(), "vuelve a la pestaña previa " + previa + " sin salir del Taller");
                falso(taller.retrocederEnDocumentacion(), "fuera de documentación no retrocede");
            }
        });
    }

    private static void apuntarDescripcionesYParidad() {
        caso("documentación del taller: descripción en cada nivel, fichas completas con ejemplo Java y cero fugas de API en ES/DE/EN", () -> {
            cierto(!Paquetes.todos().isEmpty(), "catálogo de paquetes no vacío");
            for (Paquetes.Paquete pq : Paquetes.todos()) {
                for (Idioma id : Idioma.values()) {
                    cierto(!pq.resumen(id).isBlank(), pq.nombre + ": resumen vacío en " + id);
                    cierto(!pq.parrafos(id).isEmpty(), pq.nombre + ": sin párrafos en " + id);
                    cierto(!pq.descripcion(id).isBlank(), pq.nombre + ": descripción vacía en " + id);
                }
                for (Paquetes.Componente c : pq.componentes) {
                    for (Idioma id : Idioma.values()) {
                        cierto(!c.descripcion(id).isBlank(), c.id + ": descripción de clase vacía en " + id);
                    }
                    int totalMiembros = c.miembros(Idioma.ES).size();
                    for (int i = 0; i < totalMiembros; i++) {
                        for (Idioma id : Idioma.values()) {
                            PanelDePaquetes.FichaMiembro f = PanelDePaquetes.fichaDeMiembro(c, i, id);
                            String ctx = pq.nombre + "." + c.id + "[" + i + "] (" + id + ")";
                            cierto(!f.firma.isBlank(), ctx + ": firma vacía");
                            cierto(!f.descripcion.isBlank(), ctx + ": descripción vacía");
                            cierto(!f.donde.isBlank(), ctx + ": dónde vacío");
                            cierto(!f.pseudocodigo.isBlank(), ctx + ": pseudocódigo vacío");
                            cierto(!f.ejemploJava.isBlank(), ctx + ": ejemplo Java vacío");
                            comprobarSinFugasApi(ctx, f.donde, id);
                            comprobarSinFugasApi(ctx, f.pseudocodigo, id);
                            comprobarSinFugasApi(ctx, f.ejemploJava, id);
                        }
                    }
                }
            }
        });
    }

    private static void comprobarSinFugasApi(String ctx, String texto, Idioma id) {
        if (id != Idioma.ES) {
            for (String token : API_SOLO_ES) {
                falso(texto.contains(token), ctx + ": fuga de API ES (" + token + ") en " + texto);
            }
        }
        if (id != Idioma.DE) {
            for (String token : API_SOLO_DE) {
                falso(texto.contains(token), ctx + ": fuga de API DE (" + token + ") en " + texto);
            }
        }
        if (id != Idioma.EN) {
            for (String token : API_SOLO_EN) {
                falso(texto.contains(token), ctx + ": fuga de API EN (" + token + ") en " + texto);
            }
        }
    }
}
