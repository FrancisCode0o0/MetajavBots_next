package metajavbots.pantallas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import metajavbots.progreso.Jugador;
import metajavbots.textos.Idioma;
import metajavbots.textos.Trilingue;

/**
 * RIVALES ABSURDOS — Banco dedicado de nombres propios absurdos y
 * minibiografías de comedia puramente absurda en pasado (ES, DE y EN).
 * Provee los nombres propios de los rivales de la computadora en la arena
 * y su minibiografía en {@link CodigoDelRival} en el Taller cuando el
 * jugador ya ha combatido contra un rival.
 */
public final class RivalesAbsurdos {

    /** Un perfil rival absurdo: nombre propio y minibiografía en pasado en los tres idiomas. */
    public static final class Perfil {
        public final String nombre;
        private final Trilingue biografia;

        Perfil(String nombre, String es, String de, String en) {
            this.nombre = nombre;
            this.biografia = new Trilingue(es, de, en);
        }

        /** Minibiografía en pasado (1–2 líneas) en el idioma pedido. */
        public String biografia(Idioma idioma) {
            return biografia.en(idioma == null ? Idioma.ES : idioma);
        }
    }

    private static final List<Perfil> PERFILES = List.of(
            new Perfil("Robotón",
                    "Antes de entrar en la arena, intentó doctorarse en filosofía de tostadoras y perdió un juicio contra un microondas.",
                    "Vor dem Einzug in die Arena versuchte er, in Toaster-Philosophie zu promovieren, und verlor einen Prozess gegen eine Mikrowelle.",
                    "Before entering the arena, it tried to earn a PhD in toaster philosophy and lost a lawsuit against a microwave."),
            new Perfil("Chispamán",
                    "Solía cargar sus baterías en enchufes ajenos y una vez iluminó un pueblo entero al estornudar.",
                    "Er lud seine Akkus früher an fremden Steckdosen auf und beleuchtete einmal beim Niesen ein ganzes Dorf.",
                    "It used to charge its batteries in strangers' outlets and once lit up an entire village by sneezing."),
            new Perfil("Tuercaloco",
                    "Se pasó tres años coleccionando tornillos izquierdos hasta que descubrió que había sujetado el destornillador al revés.",
                    "Er sammelte drei Jahre lang Linksschrauben, bis er merkte, dass er den Schraubenzieher verkehrt herum gehalten hatte.",
                    "It spent three years collecting left-handed screws until it realized it had held the screwdriver upside down."),
            new Perfil("Latasaurio",
                    "Creyó durante meses que era un dinosaurio blindado y exigió que le sirvieran hojas de aluminio en el comedor.",
                    "Er hielt sich monatelang für einen gepanzerten Dinosaurier und verlangte in der Kantine frische Aluminiumblätter.",
                    "It believed for months that it was an armored dinosaur and demanded fresh aluminum leaves in the cafeteria."),
            new Perfil("Tornillón",
                    "Fundó una orquesta de taladros sinfónicos que fue desalojada en el segundo compás por provocar un pequeño terremoto.",
                    "Er gründete ein Sinfonieorchester aus Bohrmaschinen, das im zweiten Takt wegen eines kleinen Erdbebens geräumt wurde.",
                    "It founded a symphonic power-drill orchestra that was evicted on the second bar for causing a minor earthquake."),
            new Perfil("Voltiofresco",
                    "Intentó enfriar su procesador dentro de una nevera portátil y terminó haciéndose amigo íntimo de un yogur caducado.",
                    "Er versuchte, seinen Prozessor in einer Kühlbox abzukühlen, und freundete sich dabei mit einem abgelaufenen Joghurt an.",
                    "It tried to cool its processor inside a cooler box and ended up becoming best friends with an expired yogurt."),
            new Perfil("Muellecín",
                    "Rebotó tanto en su graduación de robótica que atravesó el techo y recogió el diploma desde un helicóptero.",
                    "Er hüpfte bei seiner Robotik-Abschlussfeier so hoch, dass er durchs Dach flog und sein Diplom aus einem Hubschrauber entgegennahm.",
                    "It bounced so high at its robotics graduation that it went through the roof and collected its diploma from a helicopter."),
            new Perfil("Cablegordo",
                    "Se enredó con un alargador de cincuenta metros en un concurso de baile y ganó el primer premio por arte contemporáneo.",
                    "Er verhedderte sich bei einem Tanzwettbewerb in einem fünfzig Meter langen Verlängerungskabel und gewann den ersten Preis für zeitgenössische Kunst.",
                    "It got tangled in a fifty-meter extension cord at a dance contest and won first prize for contemporary art."),
            new Perfil("Chatarrix",
                    "Construyó su blindaje con tapas de cacerola y convenció al jurado de que el sonido a sartén era una táctica psicológica.",
                    "Er baute seine Panzerung aus Kochtopfdeckeln und überzeugte die Jury, dass das Pfannenscheppern psychologische Taktik war.",
                    "It built its armor out of saucepan lids and convinced the judges that the clanging sound was psychological warfare."),
            new Perfil("Pistónidas",
                    "Declaró la guerra a una impresora atascada y firmó la paz cuando la máquina le escupió doscientas páginas en blanco.",
                    "Er erklärte einem verklemmten Drucker den Krieg und schloss Frieden, nachdem das Gerät zweihundert leere Seiten ausgespuckt hatte.",
                    "It declared war on a jammed printer and signed a peace treaty after the machine spat out two hundred blank pages."),
            new Perfil("Relémpago",
                    "Juró que corría más rápido que la luz, pero siempre llegaba tarde porque se paraba a saludar a todos los semáforos.",
                    "Er schwor, schneller als das Licht zu laufen, kam aber immer zu spät, weil er jede Ampel persönlich begrüßte.",
                    "It swore it could outrun light, yet always arrived late because it stopped to greet every single traffic light."),
            new Perfil("Baterión",
                    "Se presentó a unas elecciones vecinales prometiendo wifi gratis y acabó elegido presidente de un ascensor.",
                    "Er trat bei einer Nachbarschaftswahl mit dem Versprechen von Gratis-WLAN an und wurde am Ende zum Präsidenten eines Aufzugs gewählt.",
                    "It ran in a neighborhood election promising free Wi-Fi and ended up elected president of an elevator."),
            new Perfil("Engranancio",
                    "Aprendió ajedrez observando a una lavadora centrifugar y durante años solo movió las piezas en círculos.",
                    "Er lernte Schach, indem er einer schleudernden Waschmaschine zusah, und zog seine Figuren jahrelang nur im Kreis.",
                    "It learned chess by watching a washing machine spin and spent years moving its pieces exclusively in circles."),
            new Perfil("Zumbamatic",
                    "Intentó montar un coro de aspiradoras, pero todas se tragaron las partituras antes del estreno.",
                    "Er wollte einen Staubsauger-Chor gründen, doch alle Mitglieder verschluckten vor der Premiere die Notenblätter.",
                    "It tried to start a vacuum-cleaner choir, but every member swallowed the sheet music before opening night."),
            new Perfil("Oxidot",
                    "Se bañó en el mar para sentirse más romántico y tuvieron que despegarlo de la playa con tres litros de aceite.",
                    "Er ging aus purer Romantik im Meer baden und musste anschließend mit drei Litern Öl vom Strand gelöst werden.",
                    "It went swimming in the sea to feel romantic and had to be pried off the beach with three liters of oil."),
            new Perfil("Platinillo",
                    "Abrió una academia de cocina molecular donde el único plato del menú era sopa tibia de arandelas.",
                    "Er eröffnete eine Akademie für Molekularküche, auf deren Speisekarte nur lauwarme Unterlegscheibensuppe stand.",
                    "It opened a molecular gastronomy academy whose entire menu consisted of lukewarm washer soup."),
            new Perfil("Cromosauro",
                    "Pasó tantas horas puliéndose el chasis que un día se enamoró de su propio reflejo en una cuchara.",
                    "Er polierte sein Chassis so viele Stunden lang, dass er sich eines Tages in sein eigenes Spiegelbild auf einem Löffel verliebte.",
                    "It spent so many hours polishing its chassis that one day it fell in love with its own reflection in a spoon."),
            new Perfil("Antenón",
                    "Sintonizó por accidente la radio de un taxi espacial y pasó un mes obedeciendo órdenes para girar en la próxima rotonda.",
                    "Er empfing versehentlich den Funk eines Weltraumtaxis und gehorchte einen Monat lang Anweisungen, im nächsten Kreisverkehr abzubiegen.",
                    "It accidentally tuned into a space taxi radio and spent a month obeying orders to turn at the next roundabout."),
            new Perfil("Bujía Pérez",
                    "Escribió una novela de mil páginas en código binario que solo contenía ceros porque se le atascó la tecla del uno.",
                    "Er schrieb einen tausendseitigen Roman im Binärcode, der nur aus Nullen bestand, weil die Eins-Taste klemmte.",
                    "It wrote a thousand-page novel in binary that contained only zeros because its 1 key got stuck."),
            new Perfil("Frikibot",
                    "Retó a un reloj de cuco a un duelo de miradas y se quedó sin batería justo cuando el pájaro salió a saludar.",
                    "Er forderte eine Kuckucksuhr zum Blickduell heraus und ihm ging genau in dem Moment der Akku aus, als der Vogel heraussprang.",
                    "It challenged a cuckoo clock to a staring contest and ran out of battery right when the bird popped out.")
    );

    private RivalesAbsurdos() {
    }

    /** Todos los perfiles del banco de rivales absurdos. */
    public static List<Perfil> todos() {
        return PERFILES;
    }

    /** Elige un perfil absurdo al azar (con el generador dado, o uno nuevo si es nulo). */
    public static Perfil alAzar(Random azar) {
        return PERFILES.get((azar == null ? new Random() : azar).nextInt(PERFILES.size()));
    }

    /**
     * Elige un perfil absurdo al azar cuyo nombre no esté en {@code usados}
     * (si ya se usaron todos los del banco, elige del banco completo).
     */
    public static Perfil alAzarSinRepetir(Random azar, Collection<String> usados) {
        Random r = azar == null ? new Random() : azar;
        if (usados == null || usados.isEmpty()) return alAzar(r);
        List<Perfil> disponibles = new ArrayList<>(PERFILES.size());
        for (Perfil p : PERFILES) {
            if (!usados.contains(p.nombre)) disponibles.add(p);
        }
        if (disponibles.isEmpty()) return alAzar(r);
        return disponibles.get(r.nextInt(disponibles.size()));
    }

    /** El perfil absurdo con ese nombre propio, o {@code null} si no existe o el nombre está en blanco. */
    public static Perfil porNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return null;
        String buscado = nombre.trim();
        for (Perfil p : PERFILES) {
            if (p.nombre.equals(buscado)) return p;
        }
        return null;
    }

    /** Elige de forma determinista un perfil absurdo para el último rival combatido del jugador. */
    public static Perfil para(Jugador jugador) {
        if (jugador == null) return PERFILES.get(0);
        String nombre = jugador.ultimoRivalNombre == null ? "" : jugador.ultimoRivalNombre;
        if (nombre.isBlank() && !jugador.historial.isEmpty()) {
            String r = jugador.historial.get(jugador.historial.size() - 1).rival;
            if (r != null) nombre = r;
        }
        Perfil exacto = porNombre(nombre);
        if (exacto != null) return exacto;
        String codigo = jugador.ultimoRivalCodigo == null ? "" : jugador.ultimoRivalCodigo;
        int semilla = 31 * nombre.hashCode() + 17 * codigo.hashCode() + jugador.historial.size();
        return PERFILES.get(Math.floorMod(semilla, PERFILES.size()));
    }
}
