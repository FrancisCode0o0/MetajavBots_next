package metajavbots.pantallas;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import metajavbots.Juego;
import metajavbots.cerebro.Cerebro;
import metajavbots.cerebro.CerebroDelJuego;
import metajavbots.codigo.DetectorDeTemas;
import metajavbots.ejercicios.Banco;
import metajavbots.ejercicios.Comprobador;
import metajavbots.ejercicios.Ejercicio;
import metajavbots.ejercicios.Tema;
import metajavbots.pantalla.Pantalla;
import metajavbots.pantalla.Pincel;
import metajavbots.pantalla.Ui;
import metajavbots.pantalla.VentanaDeCodigo;
import metajavbots.piezas.Modelo;
import metajavbots.progreso.Jugador;
import metajavbots.rivales.Rivales;
import metajavbots.textos.T;

/**
 * MODO TEMARIO — El jugador pega su temario (apuntes, una lista de temas, el
 * índice de unas diapositivas) y el juego le prepara la práctica: los temas
 * que reconoce, ejercicios de cada uno y combates temáticos (el rival solo
 * recibe daño si el código usa ese tema, como los jefes). Con cerebro de IA,
 * además, ejercicios nuevos hechos a medida, que el comprobador valida
 * (su solución tiene que pasar sus casos) antes de dárselos.
 * El temario se puede pegar o cargar de un archivo (PDF, Word, OpenDocument,
 * PowerPoint, HTML, RTF o texto: ejercicios/LectorDeTemario), escribiendo su
 * ruta o soltándolo en la ventana. La IA propone ejercicios que cubren todo el
 * temario, a su nivel; los que se comprueban bien se guardan como campaña
 * propia (ejercicios/TemarioGuardado) y se juegan después sin IA.
 */
public final class Temario implements Pantalla {

    private final VentanaDeCodigo texto = new VentanaDeCodigo();
    private final Set<String> temas = new LinkedHashSet<>();
    private final List<Ejercicio> nuevos = new java.util.concurrent.CopyOnWriteArrayList<>();
    private CerebroDelJuego.Peticion peticion;
    private volatile String estado = "";
    private final Ui.Desplazable despl = new Ui.Desplazable();
    private final metajavbots.pantalla.CampoDeTexto ruta = new metajavbots.pantalla.CampoDeTexto();
    /** El nombre con el que se guarda la propuesta (el del archivo, o la primera línea del texto). */
    private String nombre = "";
    private List<metajavbots.ejercicios.TemarioGuardado> guardados;

    /** Palabras que delatan cada tema, en los tres idiomas (sin tildes, en minúscula). */
    static final Map<String, String[]> PALABRAS = new LinkedHashMap<>();

    static {
        PALABRAS.put("01", new String[]{"println", "hola mundo", "hello world", "comentario", "kommentar", "comment", "primer programa", "main"});
        PALABRAS.put("02", new String[]{"variable", "tipo", "int ", "double", "boolean", "char", "datentyp", "primitiv", "literal", "casting"});
        PALABRAS.put("03", new String[]{"operador", "operator", "expresion", "ausdruck", "expression", "modulo", "aritmet"});
        PALABRAS.put("04", new String[]{"if", "else", "switch", "condicion", "bedingung", "conditional", "verzweigung", "ternari"});
        PALABRAS.put("05", new String[]{"bucle", "for", "while", "schleife", "loop", "iteracion", "iteration", "do-while"});
        PALABRAS.put("06", new String[]{"metodo", "methode", "method", "funcion", "funktion", "parametro", "parameter", "return", "rueckgabe"});
        PALABRAS.put("07", new String[]{"array", "arreglo", "vector", "feld", "matriz", "matrix"});
        PALABRAS.put("08", new String[]{"string", "cadena", "texto", "zeichenkette", "stringbuilder", "caracter"});
        PALABRAS.put("09", new String[]{"clase", "objeto", "klasse", "objekt", "class", "object", "instancia", "instanz", "new "});
        PALABRAS.put("10", new String[]{"encapsul", "kapselung", "private", "getter", "setter", "constructor", "konstruktor"});
        PALABRAS.put("11", new String[]{"herencia", "vererbung", "inheritance", "extends", "super", "override", "subclase", "unterklasse"});
        PALABRAS.put("12", new String[]{"polimorf", "polymorph", "abstract", "abstrakt"});
        PALABRAS.put("13", new String[]{"interfaz", "interface", "schnittstelle", "implements", "comparable", "default"});
        PALABRAS.put("14", new String[]{"excepcion", "exception", "ausnahme", "try", "catch", "throw", "finally", "fehlerbehandlung"});
        PALABRAS.put("15", new String[]{"coleccion", "collection", "list", "arraylist", "map", "hashmap", "set", "liste", "sammlung"});
        PALABRAS.put("16", new String[]{"generico", "generic", "generisch", "<t>", "wildcard", "comodin"});
        PALABRAS.put("17", new String[]{"lambda", "funcional", "funktional", "functional", "predicate", "function", "->"});
        PALABRAS.put("18", new String[]{"stream", "filter", "map(", "collect", "reduce", "flujo"});
        PALABRAS.put("19", new String[]{"record", "enum", "sealed", "pattern matching", "java 17", "java 21", "switch expression"});
        PALABRAS.put("20", new String[]{"hilo", "thread", "concurren", "nebenlaeufig", "parallel", "synchronized", "executor", "nebenlaufig"});
        PALABRAS.put("21", new String[]{"recursi", "rekursi", "algoritm", "algorithm", "ordenacion", "sortier", "sorting", "busqueda", "suche"});
        PALABRAS.put("22", new String[]{"junit", "test", "prueba", "diseno", "design", "entwurf", "patron", "pattern", "refactor", "clean code"});
    }

    /** Los temas que aparecen en un texto (por palabras, en los tres idiomas). */
    public static Set<String> temasDe(String texto) {
        String s = " " + Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replace('ä', 'a').replace('ö', 'o').replace('ü', 'u') + " ";
        s = s.replace("ae", "ae");
        Set<String> t = new LinkedHashSet<>();
        for (Map.Entry<String, String[]> e : PALABRAS.entrySet()) {
            for (String p : e.getValue()) {
                String clave = p.strip();
                boolean corta = clave.length() <= 3;
                if (corta ? s.matches("(?s).*[^a-z]" + java.util.regex.Pattern.quote(clave) + "[^a-z].*") : s.contains(clave)) {
                    t.add(e.getKey());
                    break;
                }
            }
        }
        return t;
    }

    /** El tema del detector de código que encaja con un tema del temario (para el combate temático), o null. */
    static String temaDelDetector(String tema) {
        switch (tema) {
            case "05": return DetectorDeTemas.BUCLES;
            case "04": return DetectorDeTemas.CONDICIONALES;
            case "06": return DetectorDeTemas.METODOS;
            case "07": return DetectorDeTemas.ARRAYS;
            case "10": return DetectorDeTemas.ENCAPSULACION;
            case "11": return DetectorDeTemas.HERENCIA;
            case "12": return DetectorDeTemas.POLIMORFISMO;
            case "13": return DetectorDeTemas.INTERFACES;
            case "14": return DetectorDeTemas.EXCEPCIONES;
            case "15": return DetectorDeTemas.COLECCIONES;
            case "16": return DetectorDeTemas.GENERICOS;
            case "17": return DetectorDeTemas.LAMBDAS;
            case "18": return DetectorDeTemas.STREAMS;
            case "19": return DetectorDeTemas.MODERNO;
            case "20": return DetectorDeTemas.HILOS;
            case "21": return DetectorDeTemas.RECURSION;
            default: return null;
        }
    }

    @Override
    public void alEntrar(Juego j) {
        if (texto.doc.largo() == 0) texto.ponerTexto(T.t("temario.ejemplo"));
        texto.tamano = j.jugador.tamanoDeLetra * 0.9f;
    }

    @Override
    public void fotograma(Juego j, float dt) {
        Pincel p = j.pincel;
        Ui ui = j.ui;
        Jugador jug = j.jugador;
        if (peticion != null && peticion.lista) {
            CerebroDelJuego.Peticion pe = peticion;
            peticion = null;
            if (pe.respuesta != null) {
                estado = T.t("temario.validando");
                String r = pe.respuesta;
                Thread h = new Thread(() -> validar(j, r), "validar-temario");
                h.setDaemon(true);
                h.start();
            }
            else estado = T.f("cerebro.error", pe.error);
        }
        Comun.fondo(j, 0);
        float arriba = Comun.cabecera(j, T.t("menu.temario"), true);
        float x = 20, y = arriba + 12, w = p.ancho * 0.42f;
        float ayuda = p.parrafo(T.t("temario.ayuda"), x, y, w, ui.letra * 0.85f, ui.tema.textoSuave, 1) + 12;
        ayuda += archivo(j, x, y + ayuda, w);
        texto.dibujar(ui, x, y + ayuda, w, p.alto - y - ayuda - 100);
        if (ui.boton(T.t("temario.preparar"), x, p.alto - 86, Math.max(300, p.ancho(T.t("temario.preparar"), ui.letra, true) + 40), 56, Ui.Estilo.PRINCIPAL)) {
            temas.clear();
            temas.addAll(temasDe(texto.doc.texto()));
            nuevos.clear();
            Cerebro c = CerebroDelJuego.de(jug);
            if (nombre.isBlank()) nombre = primeraLinea(texto.doc.texto());
            if (c != null && peticion == null) {
                estado = T.t("temario.pidiendo");
                String t = texto.doc.texto();
                // Lo que cabe en la petición (un temario muy largo se recorta: los temas suelen estar al principio, en el índice).
                if (t.length() > 12_000) t = t.substring(0, 12_000);
                peticion = CerebroDelJuego.Peticion.lanzar(c, CerebroDelJuego.sistemaTemario(jug.idioma), CerebroDelJuego.uno("Syllabus:\n" + t), 3000);
            } else {
                estado = c == null ? T.t("temario.sin_ia") : "";
            }
        }
        // A la derecha: lo preparado.
        float rx = x + w + 20, rw = p.ancho - rx - 20;
        float cy = ui.panel(rx, arriba + 12, rw, p.alto - arriba - 32, T.t("temario.plan"));
        float d = ui.desplazable(despl, rx + 8, cy, rw - 16, p.alto - cy - 30);
        float yy = cy - d;
        if (!estado.isEmpty()) yy += p.parrafo(estado, rx + 20, yy, rw - 40, ui.letra * 0.85f, ui.tema.aviso, 1) + 12;
        if (temas.isEmpty()) yy += p.parrafo(T.t("temario.vacio"), rx + 20, yy, rw - 40, ui.letra * 0.9f, ui.tema.textoApagado, 1);
        for (String id : temas) {
            Tema tm = Tema.de(id);
            if (tm == null) continue;
            p.texto(tm.id + " · " + tm.nombre(T.idioma), rx + 20, yy, ui.letra, true, ui.tema.texto, 1);
            yy += ui.letra * 1.6f;
            int n = 0;
            for (Ejercicio e : Banco.delJuego().deTema(id)) {
                if (n++ >= 3) break;
                boolean hecho = jug.ejerciciosResueltos.contains(e.id);
                if (ui.boton((hecho ? "✔ " : "") + e.titulo(T.idioma), rx + 30, yy, rw - 360, 42, Ui.Estilo.PLANO)) j.ir(new PantallaEjercicio(e));
                yy += 48;
            }
            String det = temaDelDetector(id);
            if (det != null && ui.boton(T.t("temario.combate"), rx + rw - 310, yy - 48 * Math.min(3, n), 270, 46, Ui.Estilo.NORMAL, true,
                    T.t("temario.combate_ayuda"))) {
                j.ir(new PantallaPelea(combateTematico(j, tm, det)));
            }
            yy += 14;
        }
        yy = guardados(j, rx, yy, rw);
        if (!nuevos.isEmpty()) {
            p.texto(T.t("temario.nuevos"), rx + 20, yy, ui.letra, true, ui.tema.acento, 1);
            yy += ui.letra * 1.6f;
            for (Ejercicio e : nuevos) {
                if (ui.boton(e.titulo(T.idioma), rx + 30, yy, rw - 70, 42, Ui.Estilo.NORMAL)) j.ir(new PantallaEjercicio(e));
                yy += 48;
            }
        }
        ui.finDesplazable(despl, yy + d - cy);
    }

    /** Cargarlo de un archivo: la ruta (o soltarlo en la ventana) y «Cargar». Devuelve lo que ocupa. */
    private float archivo(Juego j, float x, float y, float w) {
        Ui ui = j.ui;
        String[] soltados = j.archivosSoltados;
        if (soltados != null && soltados.length > 0) {
            j.archivosSoltados = null;
            ruta.ponerTexto(soltados[0]);
            cargar(ruta.texto());
        }
        float bw = Math.max(150, j.pincel.ancho(T.t("temario.cargar"), ui.letra, true) + 30);
        boolean intro = ruta.dibujar(ui, x, y, w - bw - 8, 46, T.f("temario.ruta", metajavbots.ejercicios.LectorDeTemario.formatos()));
        if (ui.boton(T.t("temario.cargar"), x + w - bw, y, bw, 46, Ui.Estilo.NORMAL, true, T.t("temario.cargar_ayuda")) || intro) cargar(ruta.texto());
        return 56;
    }

    /** Carga el temario de ese archivo en el editor de la izquierda (o dice por qué no puede). */
    private void cargar(String r) {
        String s = r.strip().replaceAll("^\"|\"$", "");
        if (s.isEmpty()) return;
        try {
            java.nio.file.Path f = java.nio.file.Paths.get(s);
            texto.ponerTexto(metajavbots.ejercicios.LectorDeTemario.leer(f));
            String n = f.getFileName().toString();
            nombre = n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
            estado = T.f("temario.cargado", n, texto.doc.lineas());
        } catch (java.io.IOException | RuntimeException e) {
            estado = T.f("temario.no_se_lee", s, e.getMessage());
        }
    }

    private static String primeraLinea(String t) {
        for (String l : t.split("\n")) if (!l.isBlank()) return l.strip().length() > 40 ? l.strip().substring(0, 40) : l.strip();
        return "temario";
    }

    private java.nio.file.Path carpeta(Juego j) {
        return metajavbots.progreso.Guardado.carpeta.resolve(metajavbots.progreso.Guardado.carpetaDe(j.jugador.nombre));
    }

    /** Los temarios guardados (campañas propias, sin IA): al pulsar uno, sus temas y sus ejercicios. Devuelve dónde acaba. */
    private float guardados(Juego j, float rx, float yy, float rw) {
        if (guardados == null) guardados = metajavbots.ejercicios.TemarioGuardado.todos(carpeta(j));
        if (guardados.isEmpty()) return yy;
        Pincel p = j.pincel;
        Ui ui = j.ui;
        yy += 8;
        p.texto(T.t("temario.guardados"), rx + 20, yy, ui.letra, true, ui.tema.acento, 1);
        yy += ui.letra * 1.6f;
        for (metajavbots.ejercicios.TemarioGuardado g : guardados) {
            String n = T.f("temario.guardado", g.nombre, g.ejercicios.size());
            if (ui.boton(n, rx + 30, yy, rw - 70, 42, Ui.Estilo.PLANO, true, T.t("temario.guardado_ayuda"))) {
                temas.clear();
                temas.addAll(g.temas);
                nuevos.clear();
                nuevos.addAll(g.ejercicios);
                nombre = g.nombre;
                estado = T.f("temario.abierto", g.nombre);
            }
            yy += 48;
        }
        return yy + 8;
    }

    private Encuentro combateTematico(Juego j, Tema tm, String det) {
        Encuentro e = PrepararPelea.encuentro(j, "ADAPTADA");
        e.modo = "TEMARIO";
        e.jefe = new Jefe("temario_" + tm.id, det, e.rival.montaje.modelo.id, e.nivelRival, tm.nombre, tm.nombre);
        e.rival = Rivales.crear(Modelo.de(e.jefe.modelo), e.nivelRival, j.jugador.agresividad, new Random());
        return e;
    }

    /** Los ejercicios que propone la IA: solo se dan los que se comprueban bien (su solución pasa y su plantilla no). */
    private void validar(Juego j, String respuesta) {
        try {
            List<Ejercicio> l = Ejercicio.leer(new ByteArrayInputStream(respuesta.getBytes(StandardCharsets.UTF_8)), "temario");
            int buenos = 0;
            List<String> ids = new ArrayList<>();
            for (Ejercicio e : l) {
                if (e.casos.size() < 3) continue;
                Comprobador.Informe bien = Comprobador.comprobar(e, e.codigoSolucion(), Locale.ROOT);
                Comprobador.Informe plantilla = Comprobador.comprobar(e, e.codigoInicial(), Locale.ROOT);
                if (bien.todoBien() && !plantilla.todoBien()) {
                    nuevos.add(e);
                    ids.add(e.id);
                    buenos++;
                }
            }
            estado = T.f("temario.validados", buenos, l.size());
            // Lo que ha salido bien se guarda como campaña propia: se juega después sin IA.
            if (buenos > 0) {
                metajavbots.ejercicios.TemarioGuardado.guardar(carpeta(j), nombre, temas, respuesta, ids);
                guardados = null;
                estado += " " + T.f("temario.guardado_en", nombre);
            }
        } catch (Exception ex) {
            estado = T.f("cerebro.error", ex.getMessage());
        }
    }
}
