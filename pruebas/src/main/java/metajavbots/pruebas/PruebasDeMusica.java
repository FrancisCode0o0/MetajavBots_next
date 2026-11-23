package metajavbots.pruebas;

import static metajavbots.pruebas.Pruebas.caso;
import static metajavbots.pruebas.Pruebas.cierto;
import static metajavbots.pruebas.Pruebas.falso;
import static metajavbots.pruebas.Pruebas.igual;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import metajavbots.campanas.Campana;
import metajavbots.campanas.EscenaDeCampana;
import metajavbots.pantallas.Campanas;
import metajavbots.pantallas.Ejercicios;
import metajavbots.pantallas.Encuentro;
import metajavbots.pantallas.Menu;
import metajavbots.pantallas.MiniJuegos;
import metajavbots.pantallas.PantallaCarreraDeObstaculos;
import metajavbots.pantallas.PantallaDisenarPieza;
import metajavbots.pantallas.PantallaEscenaCampana;
import metajavbots.pantallas.PantallaMinijuegoDestruccion;
import metajavbots.pantallas.Taller;
import metajavbots.pantallas.Tienda;
import metajavbots.sonido.Musica;
import metajavbots.textos.Trilingue;

/**
 * PRUEBAS DE MÚSICA — Comprueba de forma determinista el catálogo de temas y
 * piezas MIDI (existencia, validez, tempo de tabla, duración y cierre de bucle
 * sin notas colgadas), la asignación de temas por pantalla/modo, las
 * transiciones con fundido sin solapamiento y la atenuación cuando hay voz.
 */
public final class PruebasDeMusica {

    private PruebasDeMusica() {
    }

    /** Registra los casos de prueba de música épica y transiciones. */
    public static void apuntar() {
        caso("música épica: catálogo completo en temas.txt y al menos 2 piezas por modo nuevo",
                PruebasDeMusica::probarCatalogo);
        caso("música épica: cada pieza nueva cumple tempo, duración prevista y 0 notas colgadas",
                PruebasDeMusica::probarTempoDuracionYNotas);
        caso("música épica: cada pantalla, modo y escena ilustrada tiene su tema válido",
                PruebasDeMusica::probarPantallasYModos);
        caso("música épica: fundido entre temas sin solapamiento, sin bucle en fanfarrias y bajada con voz",
                PruebasDeMusica::probarTransicionesYVoz);
    }

    private static void probarCatalogo() throws Exception {
        Map<String, String> temas = new Musica().deSerie();
        List<String> nuevos = List.of(
                "menu", "carrera", "carrera_recta", "destruccion", "destruccion_final",
                "minijuegos", "jefe", "senior", "campana_creed", "campana_isla",
                "escena_hospital", "escena_milagro", "escena_victoria", "escena_derrota",
                "tienda", "taller", "victoria", "derrota");
        for (String t : nuevos) {
            cierto(temas.containsKey(t), "falta tema en temas.txt: " + t);
            String[] piezas = temas.get(t).split(",");
            cierto(piezas.length >= 2, t + " debe tener al menos 2 piezas (tiene " + piezas.length + ")");
            for (String p : piezas) {
                try (InputStream in = Musica.class.getResourceAsStream("/musica/" + p.strip())) {
                    cierto(in != null, "falta archivo MIDI: " + p.strip());
                    Sequence seq = MidiSystem.getSequence(new BufferedInputStream(in));
                    cierto(seq.getTracks().length >= 2, p.strip() + " sin pistas");
                }
            }
        }
    }

    private static Map<String, Integer> tablaTemposNuevos() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("menu_2.mid", 100);
        m.put("carrera_turbo.mid", 148);
        m.put("carrera_neon.mid", 154);
        m.put("carrera_recta_1.mid", 162);
        m.put("carrera_recta_2.mid", 166);
        m.put("destruccion_pulso.mid", 134);
        m.put("destruccion_asalto.mid", 138);
        m.put("destruccion_cuenta_1.mid", 152);
        m.put("destruccion_cuenta_2.mid", 158);
        m.put("minijuegos_agil.mid", 126);
        m.put("minijuegos_pista.mid", 130);
        m.put("jefe_coloso.mid", 118);
        m.put("jefe_amenaza.mid", 124);
        m.put("senior_maquina.mid", 136);
        m.put("senior_precision.mid", 140);
        m.put("campana_creed_1.mid", 112);
        m.put("campana_creed_2.mid", 120);
        m.put("campana_isla_1.mid", 128);
        m.put("campana_isla_2.mid", 134);
        m.put("escena_hospital_1.mid", 72);
        m.put("escena_hospital_2.mid", 76);
        m.put("escena_milagro_1.mid", 120);
        m.put("escena_milagro_2.mid", 126);
        m.put("escena_victoria_1.mid", 116);
        m.put("escena_victoria_2.mid", 122);
        m.put("escena_derrota_1.mid", 74);
        m.put("escena_derrota_2.mid", 78);
        m.put("tienda_bazar.mid", 102);
        m.put("tienda_escaparate.mid", 106);
        m.put("taller_codigo.mid", 98);
        m.put("taller_chispa.mid", 104);
        m.put("victoria_gloria.mid", 124);
        m.put("victoria_campeon.mid", 128);
        m.put("derrota_lona.mid", 72);
        m.put("derrota_ceniza.mid", 76);
        return m;
    }

    private static void probarTempoDuracionYNotas() throws Exception {
        Map<String, Integer> esperados = tablaTemposNuevos();
        for (Map.Entry<String, Integer> e : esperados.entrySet()) {
            String archivo = e.getKey();
            int bpmEsperado = e.getValue();
            try (InputStream in = Musica.class.getResourceAsStream("/musica/" + archivo)) {
                cierto(in != null, "falta " + archivo);
                Sequence seq = MidiSystem.getSequence(new BufferedInputStream(in));
                int bpm = leerBpm(seq);
                cierto(Math.abs(bpm - bpmEsperado) <= 1,
                        archivo + ": tempo esperado " + bpmEsperado + " BPM, leído " + bpm);
                double seg = seq.getMicrosecondLength() / 1_000_000.0;
                boolean corta = archivo.startsWith("victoria_") || archivo.startsWith("derrota_")
                        || archivo.startsWith("escena_");
                if (corta) {
                    cierto(seg >= 5.5 && seg <= 16.0, archivo + ": duración corta fuera de rango (" + seg + " s)");
                } else {
                    cierto(seg >= 20.0 && seg <= 65.0, archivo + ": duración de bucle fuera de rango (" + seg + " s)");
                }
                igual(0, notasColgadas(seq), archivo + ": notas colgadas al cerrar el bucle");
            }
        }
    }

    private static int leerBpm(Sequence seq) {
        for (Track track : seq.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiMessage msg = track.get(i).getMessage();
                if (msg instanceof MetaMessage meta && meta.getType() == 0x51) {
                    byte[] d = meta.getData();
                    if (d.length == 3) {
                        int us = ((d[0] & 0xFF) << 16) | ((d[1] & 0xFF) << 8) | (d[2] & 0xFF);
                        return (int) Math.round(60_000_000.0 / us);
                    }
                }
            }
        }
        return -1;
    }

    private static int notasColgadas(Sequence seq) {
        Map<Integer, Integer> abiertas = new HashMap<>();
        for (Track track : seq.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent ev = track.get(i);
                MidiMessage msg = ev.getMessage();
                if (msg instanceof ShortMessage sm) {
                    int cmd = sm.getCommand();
                    int clave = (sm.getChannel() << 8) | sm.getData1();
                    if (cmd == ShortMessage.NOTE_ON && sm.getData2() > 0) {
                        abiertas.put(clave, abiertas.getOrDefault(clave, 0) + 1);
                    } else if (cmd == ShortMessage.NOTE_OFF || (cmd == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                        int n = abiertas.getOrDefault(clave, 0) - 1;
                        if (n <= 0) abiertas.remove(clave);
                        else abiertas.put(clave, n);
                    }
                }
            }
        }
        return abiertas.size();
    }

    private static void probarPantallasYModos() {
        Map<String, String> temas = new Musica().deSerie();
        igual("menu", new Menu().musica(), "Menu");
        igual("tienda", new Tienda().musica(), "Tienda");
        igual("taller", new Taller().musica(), "Taller");
        igual("taller", new PantallaDisenarPieza().musica(), "PantallaDisenarPieza");
        igual("carrera", new PantallaCarreraDeObstaculos().musica(), "Carrera");
        igual("destruccion", new PantallaMinijuegoDestruccion().musica(), "Destruccion");
        igual("minijuegos", new MiniJuegos().musica(), "MiniJuegos");
        igual("minijuegos", new Ejercicios().musica(), "Ejercicios");
        cierto(temas.containsKey(new Campanas().musica()), "Campanas");

        Encuentro reto = MiniJuegos.prepararReto("moverse", "recorre>=2500");
        igual("minijuegos", reto.musica, "encuentro minijuego");

        Campana creed = Campana.de("creed1");
        Campana isla = Campana.de("isla_torneo");
        cierto(creed != null && "campana_creed".equals(creed.musica), "campana creed1 usa campana_creed");
        cierto(isla != null && "campana_isla".equals(isla.musica), "campana isla_torneo usa campana_isla");

        EscenaDeCampana escHosp = new EscenaDeCampana("h", EscenaDeCampana.Momento.GIRO,
                new Trilingue("H", "H", "H"),
                List.of(new EscenaDeCampana.Vineta(EscenaDeCampana.Decorado.HOSPITAL,
                        EscenaDeCampana.Animacion.SACUDIDA, "cobalto", "mazo",
                        true, true, false, null, null, null)));
        igual("escena_hospital", new PantallaEscenaCampana(creed, creed.capitulos.get(0), escHosp, false, null).musica(),
                "escena hospital");

        EscenaDeCampana escMil = new EscenaDeCampana("m", EscenaDeCampana.Momento.FINAL,
                new Trilingue("M", "M", "M"),
                List.of(new EscenaDeCampana.Vineta(EscenaDeCampana.Decorado.MILAGRO,
                        EscenaDeCampana.Animacion.DESTELLO, "cobalto", "mazo",
                        true, false, true, null, null, null)));
        igual("escena_milagro", new PantallaEscenaCampana(creed, creed.capitulos.get(0), escMil, false, null).musica(),
                "escena milagro");

        EscenaDeCampana escVic = new EscenaDeCampana("v", EscenaDeCampana.Momento.FINAL,
                new Trilingue("V", "V", "V"),
                List.of(new EscenaDeCampana.Vineta(EscenaDeCampana.Decorado.ESTADIO,
                        EscenaDeCampana.Animacion.CONFETI, "cobalto", "mazo",
                        true, false, true, null, null, null)));
        igual("escena_victoria", new PantallaEscenaCampana(creed, creed.capitulos.get(0), escVic, false, null).musica(),
                "escena victoria");

        EscenaDeCampana escDer = new EscenaDeCampana("d", EscenaDeCampana.Momento.GIRO,
                new Trilingue("D", "D", "D"),
                List.of(new EscenaDeCampana.Vineta(EscenaDeCampana.Decorado.TORMENTA_AZOTEA,
                        EscenaDeCampana.Animacion.LLUVIA, "cobalto", "mazo",
                        true, false, false, null, null, null)));
        igual("escena_derrota", new PantallaEscenaCampana(creed, creed.capitulos.get(0), escDer, false, null).musica(),
                "escena derrota");
    }

    private static void probarTransicionesYVoz() throws Exception {
        falso(Musica.esBucle("victoria"), "victoria no debe repetirse en bucle");
        falso(Musica.esBucle("derrota"), "derrota no debe repetirse en bucle");
        falso(Musica.esBucle("escena_milagro"), "escena_milagro no debe repetirse en bucle");
        cierto(Musica.esBucle("carrera"), "carrera sí va en bucle");
        cierto(Musica.esBucle("destruccion"), "destruccion sí va en bucle");

        Musica m = new Musica();
        m.volumen = 0.6f;
        m.tocar("menu");
        for (int i = 0; i < 20; i++) m.fotograma(0.05f, false);
        igual("menu", m.temaActual(), "tema inicial");
        cierto(Math.abs(m.volumenEfectivo() - 0.6f) < 0.02f, "volumen completo tras fundido de entrada");
        cierto(m.pistasActivas() <= 1, "nunca más de una pista activa");

        // Cambio a carrera: debe fundir salida primero, cambiar y volver a subir, sin solapar.
        m.tocar("carrera");
        m.fotograma(0.05f, false);
        cierto(m.cambiandoTema(), "en mitad del fundido de salida");
        cierto(m.volumenEfectivo() < 0.6f, "el volumen baja durante el fundido");
        for (int i = 0; i < 20; i++) {
            m.fotograma(0.05f, false);
            cierto(m.pistasActivas() <= 1, "sin solapamiento durante el cambio");
        }
        igual("carrera", m.temaActual(), "tema cambiado tras el fundido");
        cierto(Math.abs(m.volumenEfectivo() - 0.6f) < 0.02f, "volumen recuperado en el nuevo tema");

        // Voz activa: el volumen debe bajar claramente y recuperarse al callar.
        float sinVoz = m.volumenEfectivo();
        for (int i = 0; i < 15; i++) m.fotograma(0.05f, true);
        float conVoz = m.volumenEfectivo();
        cierto(conVoz < sinVoz * 0.5f, "la música baja cuando hay voz (" + conVoz + " vs " + sinVoz + ")");
        for (int i = 0; i < 25; i++) m.fotograma(0.05f, false);
        cierto(m.volumenEfectivo() > sinVoz * 0.95f, "la música recupera el volumen al terminar la voz");

        // Prioridad de musica_personal/.
        Path anterior = Musica.carpetaPersonal;
        Path tmp = Files.createTempDirectory("metajavbots_musica_test");
        try {
            Path propio = tmp.resolve("carrera.wav");
            Files.write(propio, new byte[]{1, 2, 3});
            Musica.carpetaPersonal = tmp;
            igual(propio, Musica.personal("carrera"), "musica_personal tiene prioridad por nombre");
            Path listado = tmp.resolve("mi_tema.ogg");
            Files.write(listado, new byte[]{4, 5, 6});
            Files.writeString(tmp.resolve("temas.txt"), "destruccion: mi_tema.ogg\n");
            igual(listado, Musica.personal("destruccion"), "musica_personal/temas.txt tiene prioridad");
        } finally {
            Musica.carpetaPersonal = anterior;
            Files.deleteIfExists(tmp.resolve("temas.txt"));
            Files.deleteIfExists(tmp.resolve("mi_tema.ogg"));
            Files.deleteIfExists(tmp.resolve("carrera.wav"));
            Files.deleteIfExists(tmp);
        }
    }
}
