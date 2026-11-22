package metajavbots.campanas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;
import metajavbots.tutor.Entrenador;

/**
 * CAMPAÑA — Una historia de varias peleas seguidas, parodia de una película
 * de combate (res/datos/campanas.txt; las que inventa la IA, en la carpeta de
 * la partida, con el mismo formato). Cada capítulo cuenta un trozo de la
 * historia, presenta al rival y, al ganarle, abre el siguiente.
 */
public final class Campana {

    /** Un capítulo: el rival (su nombre y su modelo), lo fuerte que sale, dónde se pelea y lo que se cuenta antes y después. */
    public static final class Capitulo {
        public final int numero;
        public final String modelo, escenario;
        /** El nombre del rival (con uno solo, el mismo en los tres idiomas). */
        public final Trilingue rival;
        public final double nivel;
        public final int rondas;
        /** Si este capítulo es un jefe de campaña (el entrenador acaba en el hospital y reaparece al ganar). */
        public final boolean esJefe;
        public final Trilingue texto, victoria;

        Capitulo(int numero, Map<String, String> v) {
            this.numero = numero;
            this.rival = trilingue(v.getOrDefault("rival", "?"));
            this.modelo = v.getOrDefault("modelo", "cobalto");
            this.escenario = v.getOrDefault("escenario", "HANGAR").toUpperCase();
            this.nivel = decimal(v.get("nivel"), 0.3);
            this.rondas = (int) Math.max(1, Math.min(6, decimal(v.get("rondas"), 3)));
            this.esJefe = "true".equalsIgnoreCase(v.getOrDefault("jefe", "false"));
            this.texto = trilingue(v.getOrDefault("texto", ""));
            this.victoria = trilingue(v.getOrDefault("victoria", ""));
        }
    }

    public final String id;
    public final Trilingue titulo, lema;
    /** La película a la que rinde homenaje (solo para leerlo; el juego lo enseña en pequeño). */
    public final String homenaje;
    /** El entrenador que la cuenta (Entrenador) y el tema de música (Musica). */
    public final String entrenador, musica;
    /** La campaña que hay que acabar antes (las secuelas, en orden), o vacío. */
    public final String requiere;
    /** Si es una campaña estilo Creed (integra cualquier entrenador seleccionable y hospitalización en jefes). */
    public final boolean estiloCreed;
    public final List<Capitulo> capitulos;
    /** Si la inventó la IA (se guarda en la partida, no en el juego). */
    public final boolean inventada;

    private Campana(String id, Map<String, String> v, boolean inventada) {
        this.id = id;
        this.inventada = inventada;
        this.titulo = trilingue(v.getOrDefault("titulo", id));
        this.lema = trilingue(v.getOrDefault("lema", ""));
        this.homenaje = v.getOrDefault("homenaje", "");
        this.entrenador = v.getOrDefault("entrenador", "tuercas");
        this.musica = v.getOrDefault("musica", "combate");
        this.requiere = v.getOrDefault("requiere", "");
        this.estiloCreed = "creed".equalsIgnoreCase(v.getOrDefault("estilo", ""));
        // Los capítulos: «1.rival», «1.texto»… agrupados por su número.
        Map<Integer, Map<String, String>> porNumero = new TreeMap<>();
        for (Map.Entry<String, String> e : v.entrySet()) {
            int punto = e.getKey().indexOf('.');
            if (punto <= 0) continue;
            try {
                int n = Integer.parseInt(e.getKey().substring(0, punto));
                porNumero.computeIfAbsent(n, k -> new LinkedHashMap<>()).put(e.getKey().substring(punto + 1), e.getValue());
            } catch (NumberFormatException ex) {
                // una clave con punto que no es de un capítulo: se ignora
            }
        }
        List<Capitulo> cs = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, String>> e : porNumero.entrySet()) cs.add(new Capitulo(e.getKey(), e.getValue()));
        this.capitulos = Collections.unmodifiableList(cs);
    }

    /** La clave con que se apunta en la partida que se ganó ese capítulo. */
    public String clave(int capitulo) {
        return id + ":" + capitulo;
    }

    /** Cuántos capítulos seguidos, desde el primero, se han ganado (con los ganados de la partida). */
    public int ganados(Set<String> capitulosGanados) {
        int n = 0;
        while (n < capitulos.size() && capitulosGanados.contains(clave(capitulos.get(n).numero))) n++;
        return n;
    }

    public boolean terminada(Set<String> capitulosGanados) {
        return !capitulos.isEmpty() && ganados(capitulosGanados) == capitulos.size();
    }

    /** Si se puede jugar: no pide ninguna o la que pide ya está terminada. */
    public boolean abierta(Set<String> capitulosGanados) {
        if (requiere.isEmpty()) return true;
        Campana antes = de(requiere);
        return antes == null || antes.terminada(capitulosGanados);
    }

    /**
     * El entrenador que acompaña esta campaña: en las campañas estilo Creed se integra el entrenador
     * elegido por el jugador (respetando todos los modelos y estilos de Entrenador.java); en las demás,
     * el entrenador propio de la historia.
     */
    public Entrenador entrenadorPara(Jugador jug) {
        if (estiloCreed && jug != null && jug.entrenador != null && !jug.entrenador.isBlank()) {
            return Entrenador.de(jug.entrenador);
        }
        return Entrenador.de(entrenador);
    }

    /**
     * Comentario de esquina estilo Creed con la voz, estilo y personalidad propios de cada uno de los
     * entrenadores seleccionables (nunca reducido a un único arquetipo).
     */
    public static String vozDeEsquinaCreed(Entrenador en, Idioma idioma, boolean esJefe, boolean ganado) {
        String id = en == null ? "tuercas" : en.id;
        if (esJefe && !ganado) return vozHospitalCreed(id, idioma);
        if (esJefe) return vozMilagroCreed(id, idioma);
        return vozEntrenoCreed(id, idioma);
    }

    private static String vozEntrenoCreed(String id, Idioma idi) {
        switch (id) {
            case "aceite":
                return idi.elegir("«Primero respira y pule cada línea. El apellido no golpea; tu equilibrio, sí.»",
                        "«Atme zuerst und poliere jede Zeile. Der Nachname schlägt nicht; dein Gleichgewicht schon.»",
                        "\"Breathe first and polish every line. A last name doesn't throw punches; your balance does.\"");
            case "voltaje":
                return idi.elegir("«¡Baila en el centro del ring! Ese apellido pesa, pero tu código brilla todavía más.»",
                        "«Tanz mitten im Ring! Der Name wiegt schwer, aber dein Code strahlt noch heller.»",
                        "\"Dance in the center of the ring! That name is heavy, yet your code shines even brighter.\"");
            case "remache":
                return idi.elegir("«No quería entrenarte, pero ya estás aquí. Sube la guardia y demuestra tu propio código.»",
                        "«Ich wollte dich nicht trainieren, aber jetzt bist du da. Deckung hoch und zeig deinen eigenen Code.»",
                        "\"I didn't want to train you, yet here we are. Keep your guard up and prove your own code.\"");
            case "cobra":
                return idi.elegir("«¡Sin piedad con los bugs ni con las dudas! ¡Ataca primero y escribe tu propia leyenda!»",
                        "«Keine Gnade für Bugs oder Zweifel! Schlag zuerst zu und schreib deine eigene Legende!»",
                        "\"No mercy for bugs or hesitation! Strike first and write your own legend!\"");
            case "yanobit":
                return idi.elegir("«El legado es como el agua del estanque: si fuerzas la corriente, caes; si la usas, vences.»",
                        "«Das Erbe ist wie Wasser im Teich: Zwingst du den Strom, fällst du; nutzt du ihn, siegst du.»",
                        "\"Legacy is like water in a pond: fight the current and you fall; redirect it and you win.\"");
            case "pelobit":
                return idi.elegir("«¡Ja! ¿Crees que basta un apellido famoso? ¡Inclínate ante el punto y coma y entrena el doble!»",
                        "«Ha! Glaubst du, ein großer Name reicht? Verneige dich vor dem Semikolon und trainiere doppelt!»",
                        "\"Ha! Think a famous name is enough? Bow to the semicolon and train twice as hard!\"");
            default:
                return idi.elegir("«¡Arriba, chatarra! No peleas por una sombra: sales ahí a ganarte tu propio nombre.»",
                        "«Hoch mit dir, Schrotthaufen! Du kämpfst nicht für einen Schatten, sondern für deinen eigenen Namen.»",
                        "\"Up, scrap heap! You're not fighting for a shadow: get out there and earn your own name.\"");
        }
    }

    private static String vozHospitalCreed(String id, Idioma idi) {
        switch (id) {
            case "aceite":
                return idi.elegir("«Desde esta camilla solo puedo pulir el gota a gota… Hoy el equilibrio depende solo de ti.»",
                        "«Von dieser Trage aus kann ich nur den Tropf polieren … Heute liegt das Gleichgewicht allein bei dir.»",
                        "\"From this stretcher I can only wax the IV drip… Today balance rests solely on you.\"");
            case "voltaje":
                return idi.elegir("«¡Me oxidé en la playa antes del título! Haz vibrar el estadio por los dos, campeón.»",
                        "«Vor dem Titelkampf am Strand verrostet! Bring das Stadion für uns beide zum Beben, Champ.»",
                        "\"Rusted at the beach right before title night! Rock the stadium for both of us, champ.\"");
            case "remache":
                return idi.elegir("«Los médicos me han puesto cuatro candados en la camilla. No me falles ahí fuera.»",
                        "«Die Ärzte haben vier Schlösser an meine Trage gehängt. Enttäusch mich da draußen nicht.»",
                        "\"The doctors put four padlocks on my stretcher. Don't let me down out there.\"");
            case "cobra":
                return idi.elegir("«¡Un rayo me tumbó el firmware! ¡Aplasta a ese jefe sin piedad por tu sensei!»",
                        "«Ein Blitz hat meine Firmware gegrillt! Besiege diesen Boss ohne Gnade für deinen Sensei!»",
                        "\"Lightning fried my firmware! Crush that boss with no mercy for your sensei!\"");
            case "yanobit":
                return idi.elegir("«Caí en el hospital por una tuerca rebelde. Recuerda: cae siete veces, compila ocho.»",
                        "«Eine widerspenstige Mutter brachte mich ins Krankenhaus. Denk daran: siebenmal fallen, achtmal kompilieren.»",
                        "\"A rogue nut landed me in the hospital. Remember: fall seven times, compile eight.\"");
            case "pelobit":
                return idi.elegir("«¡Me enredé la barba de cables en el monitor! ¡Si no ganas este título, no me cures!»",
                        "«Mein Kabelbart steckt im Monitor fest! Wenn du diesen Titel nicht holst, bleibe ich hier liegen!»",
                        "\"My cable beard got tangled in the monitor! If you don't win this belt, don't bother curing me!\"");
            default:
                return idi.elegir("«Tomé aceite caducado y no puedo subir al ring… ¡Sal ahí y gana este combate, chaval!»",
                        "«Ich habe abgelaufenes Öl getrunken und schaffe es nicht in den Ring … Hol dir den Sieg, Kleiner!»",
                        "\"I drank expired oil and can't make it to ringside… Get out there and win this fight, kid!\"");
        }
    }

    private static String vozMilagroCreed(String id, Idioma idi) {
        switch (id) {
            case "aceite":
                return idi.elegir("«¡Milagro! Al ver tu victoria se me alinearon todos los tornillos. Al final sí vine.»",
                        "«Ein Wunder! Bei deinem Sieg richteten sich alle meine Schrauben aus. Am Ende kam ich doch.»",
                        "\"Miracle! Seeing you win aligned every single bolt. I made it to the fight after all.\"");
            case "voltaje":
                return idi.elegir("«¡Al final sí vine, disfrazado de foco! ¡Eso sí que fue una victoria con estilo!»",
                        "«Ich war doch da, als Scheinwerfer verkleidet! Das war mal ein Sieg mit Stil!»",
                        "\"I made it after all, disguised as a spotlight! Now that was a victory with style!\"");
            case "remache":
                return idi.elegir("«Me escapé del hospital cuesta abajo en la camilla. Bien hecho. No se lo digas a nadie.»",
                        "«Ich bin auf der Trage bergab aus der Klinik geflohen. Gut gemacht. Sag es keinem.»",
                        "\"Escaped the hospital downhill on the stretcher. Well done. Don't tell anyone.\"");
            case "cobra":
                return idi.elegir("«¡Tu victoria reinició mi sistema! ¡Sin piedad y con cinturón nuevo!»",
                        "«Dein Sieg hat mein System neu gestartet! Keine Gnade und ein neuer Gürtel!»",
                        "\"Your victory rebooted my whole system! No mercy and a brand-new belt!\"");
            case "yanobit":
                return idi.elegir("«Me curé de golpe al oír la campana final. El alumno ha superado al maestro.»",
                        "«Beim Schlussgong war ich sofort geheilt. Der Schüler hat den Meister übertroffen.»",
                        "\"Cured on the spot at the final bell. The student has surpassed the master.\"");
            case "pelobit":
                return idi.elegir("«¡Milagro de ingeniería! ¡Mi barba se soltó sola al ver semejante obra maestra!»",
                        "«Ein Wunder der Technik! Mein Bart löste sich von selbst bei diesem Meisterwerk!»",
                        "\"An engineering miracle! My beard untangled itself the moment you pulled off that masterpiece!\"");
            default:
                return idi.elegir("«¡Me curé de golpe al verte ganar, chaval! Sabía que al final no me perdería la pelea.»",
                        "«Als du gewonnen hast, war ich sofort gesund, Kleiner! Ich wusste, ich verpasse den Kampf nicht.»",
                        "\"Seeing you win cured me on the spot, kid! I knew I wouldn't miss the fight after all.\"");
        }
    }

    // ================================================================ leer

    private static List<Campana> deSerie;

    /** Las campañas escritas de serie, en el orden del archivo. */
    public static synchronized List<Campana> deSerie() {
        if (deSerie == null) {
            try (InputStream in = Campana.class.getResourceAsStream("/datos/campanas.txt")) {
                deSerie = Collections.unmodifiableList(leer(new InputStreamReader(in, StandardCharsets.UTF_8), false));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo leer res/datos/campanas.txt", e);
            }
        }
        return deSerie;
    }

    /** La campaña de serie con ese id, o null. */
    public static Campana de(String id) {
        for (Campana c : deSerie()) if (c.id.equals(id)) return c;
        return null;
    }

    /** Lee campañas en el formato del archivo («[id]», «clave: valor», «#» comentario). */
    public static List<Campana> leer(Reader fuente, boolean inventadas) throws IOException {
        List<Campana> l = new ArrayList<>();
        BufferedReader r = new BufferedReader(fuente);
        String id = null, linea;
        Map<String, String> v = new LinkedHashMap<>();
        while ((linea = r.readLine()) != null) {
            linea = linea.strip();
            if (linea.startsWith("#") || linea.isEmpty()) continue;
            if (linea.startsWith("[") && linea.contains("]")) {
                if (id != null) l.add(new Campana(id, v, inventadas));
                id = linea.substring(1, linea.indexOf(']')).strip();
                v = new LinkedHashMap<>();
            } else if (linea.contains(":")) {
                int i = linea.indexOf(':');
                v.put(linea.substring(0, i).strip(), linea.substring(i + 1).strip());
            }
        }
        if (id != null) l.add(new Campana(id, v, inventadas));
        return l;
    }

    /** Lee campañas de un texto (lo que contesta la IA); las que no traen ningún capítulo se descartan. */
    public static List<Campana> leer(String texto, boolean inventadas) {
        try {
            List<Campana> l = new ArrayList<>();
            for (Campana c : leer(new StringReader(texto), inventadas)) if (!c.capitulos.isEmpty()) l.add(c);
            return l;
        } catch (IOException e) {
            return List.of();
        }
    }

    static Trilingue trilingue(String s) {
        String[] p = s.split("\\|");
        String es = p[0].strip();
        return new Trilingue(es, p.length > 1 ? p[1].strip() : es, p.length > 2 ? p[2].strip() : es);
    }

    private static double decimal(String s, double siNo) {
        try {
            return s == null ? siNo : Double.parseDouble(s.strip());
        } catch (NumberFormatException e) {
            return siNo;
        }
    }
}
