package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

import metajavbots.pantalla.Fuentes;
import metajavbots.pantallas.AlarmaBajada;
import metajavbots.progreso.Etiqueta;
import metajavbots.progreso.Jugador;
import metajavbots.progreso.Progreso;
import metajavbots.progreso.Registro;
import metajavbots.sonido.Sintetizador;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;

/** Pruebas de la alarma visual/sonora de bajada de nivel y de la carga y cobertura de las nuevas fuentes. */
final class PruebasDeAlarmaYFuentes {

    private PruebasDeAlarmaYFuentes() {
    }

    static void apuntar() {
        apuntarBajada();
        apuntarFuentes();
    }

    private static void apuntarBajada() {
        caso("bajada de nivel: dispara evento, transición, alarma al menú, tono de alerta y textos en ES, DE y EN", () -> {
            Jugador j = new Jugador("Descenso");
            j.etiqueta = Etiqueta.JUNIOR_2;
            Random azar = new Random(11);
            Progreso.Cambio ultimo = null;
            for (int i = 0; i < Progreso.DERROTAS_PARA_BAJAR; i++) {
                Registro r = new Registro(System.currentTimeMillis(), "ADAPTADA", "Rival", "chispa", "cobalto", 0.5,
                        Registro.Resultado.DERROTA, 100, 20, 2, j.etiqueta.name());
                ultimo = Progreso.despuesDeLaPelea(j, r, "ACERO", azar);
            }
            cierto(ultimo != null && ultimo.bajo, "debe dispararse la bajada tras las derrotas seguidas");
            igual(Etiqueta.JUNIOR_2, ultimo.antes, "etiqueta anterior");
            igual(Etiqueta.JUNIOR_1, ultimo.despues, "etiqueta nueva");
            igual("Junior II ↓ Junior I", ultimo.transicionBajada(), "formato de transición en bajada");

            // Textos explícitos en los tres idiomas (es, de, en).
            for (Idioma id : Idioma.values()) {
                String titulo = T.en("final.bajas_titulo", id);
                String aviso = ultimo.mensajeBajada(id);
                cierto(titulo != null && !titulo.isBlank() && !titulo.equals("final.bajas_titulo"), "título en " + id);
                cierto(aviso != null && !aviso.isBlank() && !aviso.equals("final.bajas_aviso"), "aviso en " + id);
                igual(aviso, AlarmaBajada.mensaje(id), "AlarmaBajada expone el mismo aviso en " + id);
            }
            String es = ultimo.mensajeBajada(Idioma.ES);
            String de = ultimo.mensajeBajada(Idioma.DE);
            String en = ultimo.mensajeBajada(Idioma.EN);
            cierto(!es.equals(de) && !es.equals(en) && !de.equals(en), "aviso traducido en los tres idiomas");

            // Estado de la alarma visual y su transición al menú.
            AlarmaBajada alarma = new AlarmaBajada();
            falso(alarma.activa(), "empieza inactiva");
            alarma.activar(ultimo.antes, ultimo.despues);
            cierto(alarma.activa(), "se activa al bajar");
            cierto(alarma.pendienteEnMenu(), "queda pendiente para el menú");
            igual("Junior II ↓ Junior I", alarma.transicion(), "transición en alarma");
            alarma.alEntrarAlMenu();
            cierto(alarma.activa(), "sigue activa al entrar al menú");
            falso(alarma.pendienteEnMenu(), "ya se consumió la marca de menú");
            alarma.cancelar();
            falso(alarma.activa(), "se puede cerrar manualmente");

            // Tono breve de alerta descendente en Sintetizador.
            float[] muestras = Sintetizador.hacer("alerta_bajada", 7L);
            cierto(muestras.length > Sintetizador.FRECUENCIA / 5, "tono alerta_bajada con muestras suficientes");
            cierto(muestras.length < Sintetizador.FRECUENCIA * 2, "tono alerta_bajada breve");
        });
    }

    private static void apuntarFuentes() {
        caso("fuentes: cargan todas las tipografías SIL OFL y cubren ES, DE, EN y los símbolos del juego", () -> {
            // Licencias SIL OFL presentes junto a las fuentes.
            for (String lic : Fuentes.LICENCIAS) {
                try (InputStream in = Fuentes.abrir(lic)) {
                    cierto(in != null, "falta archivo de licencia: " + lic);
                    String txt = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    cierto(txt.contains("SIL OPEN FONT LICENSE"), "licencia OFL válida en " + lic);
                }
            }
            // Cada fuente se abre y se parsea como TrueType válida.
            Map<String, TrueTypeFont> cargadas = new LinkedHashMap<>();
            try {
                TTFParser parser = new TTFParser();
                for (String ruta : Fuentes.TODAS) {
                    try (InputStream in = Fuentes.abrir(ruta)) {
                        cierto(in != null, "falta recurso de fuente: " + ruta);
                        byte[] bytes = in.readAllBytes();
                        cierto(bytes.length > 10_000, "fuente demasiado pequeña: " + ruta);
                        TrueTypeFont ttf = parser.parse(new RandomAccessReadBuffer(bytes));
                        cierto(ttf.getNumberOfGlyphs() > 100, "sin glifos en " + ruta);
                        cargadas.put(ruta, ttf);
                    }
                }
                // Idiomas ES, DE y EN en las fuentes principales (interfaz, títulos y código).
                String idiomas = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789áéíóúüñÁÉÍÓÚÜÑäöüßÄÖÜ¿¡";
                String[] principales = {Fuentes.INTERFAZ, Fuentes.TITULO, Fuentes.CODIGO, Fuentes.CODIGO_NEGRITA};
                for (String ruta : principales) {
                    CmapLookup cmap = cargadas.get(ruta).getUnicodeCmapLookup();
                    for (int i = 0; i < idiomas.length(); i++) {
                        char c = idiomas.charAt(i);
                        cierto(cmap.getGlyphId(c) > 0, ruta + " no cubre el carácter '" + c + "'");
                    }
                }
                // Símbolos del juego cubiertos entre las fuentes principales y el respaldo de símbolos.
                String simbolos = "▶✎✕⚔⚙“”…⚠↓←→↑★◆✓■▣●▲▤❚";
                CmapLookup cmapUi = cargadas.get(Fuentes.INTERFAZ).getUnicodeCmapLookup();
                CmapLookup cmapCod = cargadas.get(Fuentes.CODIGO).getUnicodeCmapLookup();
                CmapLookup cmapSim = cargadas.get(Fuentes.SIMBOLOS).getUnicodeCmapLookup();
                CmapLookup cmapSim2 = cargadas.get(Fuentes.SIMBOLOS_2).getUnicodeCmapLookup();
                for (int i = 0; i < simbolos.length(); i++) {
                    char c = simbolos.charAt(i);
                    boolean cubierto = cmapUi.getGlyphId(c) > 0 || cmapCod.getGlyphId(c) > 0
                            || cmapSim.getGlyphId(c) > 0 || cmapSim2.getGlyphId(c) > 0;
                    cierto(cubierto, "símbolo sin cobertura ni respaldo: '" + c + "' (U+" + Integer.toHexString(c) + ")");
                }
            } finally {
                for (TrueTypeFont ttf : cargadas.values()) ttf.close();
            }
        });
    }
}
