package castillafantasx.eventos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HISTORIA — Lo que la partida recuerda del modo evento (docs/EVENTOS.md):
 * el estado de cada misión (sus variables y si está sin empezar, en curso o
 * acabada), la crónica del héroe (lo que ha hecho y decidido, común a
 * todas las misiones), los eventos que ya han saltado, el diario y lo que
 * los eventos han puesto y quitado del mundo. La lleva el héroe y se guarda
 * con la partida (Guardado), en líneas «historia.… = …».
 */
public final class Historia {

    public enum Estado { SIN_EMPEZAR, EN_CURSO, ACABADA }

    /** Un apunte del diario del héroe: cuándo lo escribió y qué. */
    public static final class Apunte {
        public final int dia;
        public final double hora;
        public final String mision, texto;

        Apunte(int dia, double hora, String mision, String texto) {
            this.dia = dia;
            this.hora = hora;
            this.mision = mision;
            this.texto = texto;
        }
    }

    /** La crónica del héroe: lo que ha hecho y decidido en cada misión («entrego_a_la_nina = si»). */
    public final Variables cronica = new Variables();
    private final Map<String, Variables> deMision = new LinkedHashMap<>();
    private final Map<String, Estado> estados = new LinkedHashMap<>();
    /** Las misiones en curso, de la primera que empezó a la última (la última manda en los límites). */
    public final List<String> enCurso = new ArrayList<>();
    /** Los eventos que ya han saltado: «mision/evento». */
    public final Set<String> eventosHechos = new LinkedHashSet<>();
    /** El diario del héroe: las pistas y lo que se sabe, del apunte más antiguo al más nuevo. */
    public final List<Apunte> diario = new ArrayList<>();
    /** Lo que los eventos han puesto en el mundo, por su id: «ser pastor superficie 100 200 nudo» u «objeto COFRE superficie 10 20 dato». */
    public final Map<String, String> puestos = new LinkedHashMap<>();
    /** La gente y los objetos del mapa que los eventos han quitado (su id, su nudo…). */
    public final Set<String> quitados = new LinkedHashSet<>();

    /** Las variables de esa misión (si aún no tiene, vacías). */
    public Variables de(String mision) {
        return deMision.computeIfAbsent(mision, m -> new Variables());
    }

    public Estado estado(String mision) {
        return estados.getOrDefault(mision, Estado.SIN_EMPEZAR);
    }

    public void poner(String mision, Estado estado) {
        estados.put(mision, estado);
        enCurso.remove(mision);
        if (estado == Estado.EN_CURSO) enCurso.add(mision);
    }

    /** La última misión que empezó y sigue en curso, o null. */
    public String ultimaEnCurso() {
        return enCurso.isEmpty() ? null : enCurso.get(enCurso.size() - 1);
    }

    /** Apunta algo en el diario (lo mismo dos veces seguidas, no). */
    public boolean apuntar(int dia, double hora, String mision, String texto) {
        if (texto == null || texto.trim().isEmpty()) return false;
        for (Apunte a : diario) {
            if (a.texto.equals(texto.trim())) return false;
        }
        diario.add(new Apunte(dia, hora, mision == null ? "" : mision, texto.trim()));
        return true;
    }

    /** Como al empezar una partida nueva (la crónica también se vacía: se importa aparte). */
    public void vaciar() {
        cronica.vaciar();
        deMision.clear();
        estados.clear();
        enCurso.clear();
        eventosHechos.clear();
        diario.clear();
        puestos.clear();
        quitados.clear();
    }

    // ------------------------------------------------------------------
    // Guardar y cargar con la partida
    // ------------------------------------------------------------------

    private static final String CRONICA = "historia.cronica", MISIONES = "historia.misiones", VARIABLES = "historia.variables.",
            EVENTOS = "historia.eventos", DIARIO = "historia.diario", PUESTOS = "historia.puestos", QUITADOS = "historia.quitados";

    /** Las líneas «clave = valor» que van a la partida guardada. */
    public List<String> aLineas() {
        List<String> lineas = new ArrayList<>();
        lineas.add(CRONICA + " = " + cronica.aTexto());
        // Las misiones, con su estado; las que están en curso, en el orden en que empezaron (van las últimas).
        List<String> misiones = new ArrayList<>();
        for (Map.Entry<String, Estado> e : estados.entrySet()) {
            if (e.getValue() != Estado.EN_CURSO) misiones.add(e.getKey() + ":" + e.getValue().name().toLowerCase());
        }
        for (String m : enCurso) {
            misiones.add(m + ":" + Estado.EN_CURSO.name().toLowerCase());
        }
        lineas.add(MISIONES + " = " + String.join(",", misiones));
        for (Map.Entry<String, Variables> v : deMision.entrySet()) {
            lineas.add(VARIABLES + v.getKey() + " = " + v.getValue().aTexto());
        }
        lineas.add(EVENTOS + " = " + String.join(",", eventosHechos));
        List<String> apuntes = new ArrayList<>();
        for (Apunte a : diario) {
            apuntes.add(a.dia + ":" + Math.round(a.hora * 100) / 100.0 + ":" + Variables.escapar(a.mision) + ":" + Variables.escapar(a.texto));
        }
        lineas.add(DIARIO + " = " + String.join("|", apuntes));
        List<String> cosas = new ArrayList<>();
        for (Map.Entry<String, String> p : puestos.entrySet()) {
            cosas.add(Variables.escapar(p.getKey()) + ":" + Variables.escapar(p.getValue()));
        }
        lineas.add(PUESTOS + " = " + String.join("|", cosas));
        lineas.add(QUITADOS + " = " + String.join(",", quitados));
        return lineas;
    }

    /** Lo contrario de aLineas(): 'datos' son las líneas de la partida, ya partidas en clave y valor. Una partida de antes no trae nada: todo vacío. */
    public void deLineas(Map<String, String> datos) {
        vaciar();
        cronica.deTexto(datos.getOrDefault(CRONICA, ""));
        for (String par : datos.getOrDefault(MISIONES, "").split(",")) {
            int dos = par.lastIndexOf(':');
            if (dos > 0) poner(par.substring(0, dos).trim(), Estado.valueOf(par.substring(dos + 1).trim().toUpperCase(java.util.Locale.ROOT)));
        }
        for (Map.Entry<String, String> d : datos.entrySet()) {
            if (d.getKey().startsWith(VARIABLES)) de(d.getKey().substring(VARIABLES.length())).deTexto(d.getValue());
        }
        for (String hecho : datos.getOrDefault(EVENTOS, "").split(",")) {
            if (!hecho.trim().isEmpty()) eventosHechos.add(hecho.trim());
        }
        for (String apunte : datos.getOrDefault(DIARIO, "").split("\\|")) {
            String[] p = apunte.split(":");
            if (p.length == 4) diario.add(new Apunte(Integer.parseInt(p[0].trim()), Double.parseDouble(p[1]), Variables.desescapar(p[2]), Variables.desescapar(p[3])));
        }
        for (String cosa : datos.getOrDefault(PUESTOS, "").split("\\|")) {
            int dos = cosa.indexOf(':');
            if (dos > 0) puestos.put(Variables.desescapar(cosa.substring(0, dos).trim()), Variables.desescapar(cosa.substring(dos + 1)));
        }
        for (String quitado : datos.getOrDefault(QUITADOS, "").split(",")) {
            if (!quitado.trim().isEmpty()) quitados.add(quitado.trim());
        }
    }

    // ------------------------------------------------------------------
    // La crónica, de una partida a otra
    // ------------------------------------------------------------------

    /**
     * Importa la crónica de una partida terminada (su partida.txt, o un
     * cronica.txt escrito al acabar una misión): lo que traiga se suma a la
     * de ahora (lo de la importada manda). Devuelve cuántas cosas ha traído, o -1 si no se pudo leer.
     */
    public int importarCronica(Path archivo) {
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                int igual = linea.indexOf('=');
                if (igual < 0 || !linea.substring(0, igual).trim().equals(CRONICA)) continue;
                Variables otra = new Variables();
                otra.deTexto(linea.substring(igual + 1).trim());
                for (String nombre : otra.nombres()) {
                    cronica.poner(nombre, otra.texto(nombre));
                }
                return otra.nombres().size();
            }
            return 0;
        } catch (IOException | RuntimeException e) {
            System.err.println("No se pudo importar la crónica de " + archivo + ": " + e.getMessage());
            return -1;
        }
    }

    /** Escribe la crónica sola en un archivo (al acabar una misión), para importarla en otra partida. ¿Se pudo? */
    public boolean escribirCronica(Path archivo) {
        try {
            List<String> lineas = new ArrayList<>();
            lineas.add("# La crónica de " + castillafantasx.datos.ElJuego.heroe() + ": lo que ha hecho y decidido (docs/EVENTOS.md).");
            lineas.add(CRONICA + " = " + cronica.aTexto());
            castillafantasx.datos.Candado.vigilar(archivo);
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("No se pudo escribir la crónica: " + e.getMessage());
            return false;
        }
    }
}
