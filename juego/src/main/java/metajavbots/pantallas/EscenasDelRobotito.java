package metajavbots.pantallas;

import metajavbots.pantalla.Pincel;
import metajavbots.textos.Idioma;
import metajavbots.textos.T;
import metajavbots.textos.Trilingue;

/**
 * ESCENAS DEL ROBOTITO — Un escenario para cada concepto de Java (uno por
 * tema, del 01 al 22): lo que el robotito apila, coloca o lleva cambia con el
 * tema, y el fondo cuenta la idea. La recursión son muñecas rusas junto a una
 * torre de Hanói; la herencia, una familia de robots que heredan la antena;
 * las excepciones, un robot que tropieza con una cáscara y se levanta; los
 * hilos, varios robots trabajando a la vez; los streams, un río de piezas con
 * un filtro… Lo dibuja AnimacionDelCaso, que pone las formas (cajas, puertas,
 * pizarra, estantería, papel).
 */
public final class EscenasDelRobotito {

    /** Un escenario por concepto, con su nombre en tres idiomas (sale abajo, en el suelo). */
    enum Escena {
        DESPERTAR("01", "Primeros pasos: el robot se enciende", "Erste Schritte: der Roboter startet", "First steps: the robot boots up"),
        TARROS("02", "Variables: tarros con etiqueta", "Variablen: Gläser mit Etikett", "Variables: labelled jars"),
        BALANZA("03", "Operadores: la balanza", "Operatoren: die Waage", "Operators: the scales"),
        APARCAMIENTO("04", "Condicionales: ¿qué plaza?", "Bedingungen: welcher Parkplatz?", "Conditionals: which parking spot?"),
        OBRA("05", "Bucles: vuelta a vuelta, la torre", "Schleifen: Runde um Runde, der Turm", "Loops: lap by lap, the tower"),
        COCINA("06", "Métodos: una receta que se repite", "Methoden: ein Rezept zum Wiederholen", "Methods: a reusable recipe"),
        TAQUILLAS("07", "Arrays: taquillas numeradas desde 0", "Arrays: nummerierte Schließfächer ab 0", "Arrays: lockers numbered from 0"),
        PINTURA("08", "Textos: letra a letra", "Texte: Buchstabe für Buchstabe", "Text: letter by letter"),
        PLANOS("09", "Clases: el plano y sus robots", "Klassen: der Bauplan und seine Roboter", "Classes: the blueprint and its robots"),
        CAJA_FUERTE("10", "Encapsulación: la caja fuerte", "Kapselung: der Tresor", "Encapsulation: the safe"),
        FAMILIA("11", "Herencia: la familia que hereda la antena", "Vererbung: die Familie erbt die Antenne", "Inheritance: the family inherits the antenna"),
        CORO("12", "Polimorfismo: la misma orden, cada uno a su manera", "Polymorphie: derselbe Befehl, jeder auf seine Art",
                "Polymorphism: same order, each in its own way"),
        ENCHUFES("13", "Interfaces: el mismo enchufe para todos", "Interfaces: dieselbe Steckdose für alle", "Interfaces: the same plug for all"),
        TROPIEZO("14", "Excepciones: tropieza, lo atrapa y sigue", "Ausnahmen: stolpert, fängt sich und macht weiter",
                "Exceptions: trips, catches it and carries on"),
        CLASIFICAR("15", "Colecciones: cada cosa en su saco", "Collections: alles in seinen Sack", "Collections: everything in its sack"),
        CAJAS_T("16", "Genéricos: la caja <T> vale para todo", "Generics: die Kiste <T> passt für alles", "Generics: the <T> box fits anything"),
        MAQUINA("17", "Lambdas: la maquinita que transforma", "Lambdas: die kleine Verwandlungsmaschine", "Lambdas: the little transforming machine"),
        RIO("18", "Streams: el río de piezas y su filtro", "Streams: der Fluss der Teile und sein Filter", "Streams: the river of parts and its filter"),
        FICHAS("19", "Java moderno: fichas (record) y tipos cerrados", "Modernes Java: Karten (record) und versiegelte Typen",
                "Modern Java: records and sealed types"),
        EQUIPO("20", "Hilos: varios robots a la vez", "Threads: mehrere Roboter gleichzeitig", "Threads: several robots at once"),
        MUNECAS("21", "Recursión: muñecas rusas y la torre de Hanói", "Rekursion: Matroschkas und der Turm von Hanoi",
                "Recursion: nesting dolls and the Tower of Hanoi"),
        ARQUITECTO("22", "Pruebas y diseño: el inspector de calidad", "Tests und Entwurf: der Qualitätsprüfer", "Testing and design: the quality inspector"),
        COMPOSICION("", "Composición: un robot hecho de piezas", "Komposition: ein Roboter aus Teilen", "Composition: a robot made of parts"),
        ABSTRACTA("", "Clases abstractas: el molde con huecos que rellena cada hijo", "Abstrakte Klassen: die Form mit Lücken, die jedes Kind füllt",
                "Abstract classes: the mould with gaps each child fills in");

        final String tema;
        final Trilingue nombre;

        Escena(String tema, String es, String de, String en) {
            this.tema = tema;
            this.nombre = new Trilingue(es, de, en);
        }
    }

    private static final int MADERA = 0x6B4F33, ACERO = 0x9AA0A6, OSCURO = 0x2C3646;

    private EscenasDelRobotito() {
    }

    /** La escena de un ejercicio: la de su tema, salvo si trata de composición o de clases abstractas (que tienen la suya). */
    static Escena de(String tema, String texto) {
        String t = texto == null ? "" : texto.toLowerCase(java.util.Locale.ROOT);
        if (t.contains("abstract")) return Escena.ABSTRACTA;
        if (t.contains("composici") || t.contains("komposition") || t.contains("composition")) return Escena.COMPOSICION;
        return de(tema);
    }

    /** La escena de un tema (la de la fábrica de piezas si el tema no tiene la suya). */
    static Escena de(String tema) {
        for (Escena e : Escena.values()) if (e.tema.equals(tema)) return e;
        return Escena.CLASIFICAR;
    }

    // ================================================================ el fondo

    /** El fondo: lo justo para que se reconozca la idea, sin quitar sitio a lo que hace el robotito. */
    static void fondo(Pincel p, Escena e, float x, float y, float w, float h, float reloj) {
        float suelo = y + h - 26;
        int colorSuelo = e == Escena.APARCAMIENTO ? 0x3A3F46 : e == Escena.MUNECAS || e == Escena.RIO ? OSCURO : MADERA;
        p.rect(x, suelo, w, 26, colorSuelo, 1);
        float dx = x + w - 200, arriba = y + 56;
        if (e.ordinal() < Escena.CORO.ordinal()) fondoDeLasBases(p, e, x, y, w, reloj, suelo, dx, arriba);
        else fondoDeLoAvanzado(p, e, x, y, w, reloj, suelo, dx, arriba);
        p.texto("◆ " + e.nombre.en(T.idioma == null ? Idioma.ES : T.idioma), x + 14, suelo + 4, 14, true, 0xF2EEE3, 0.85f);
    }

    /** Los fondos de los fundamentos, del control, de los datos y de las primeras ideas de objetos. */
    private static void fondoDeLasBases(Pincel p, Escena e, float x, float y, float w, float reloj, float suelo, float dx, float arriba) {
        switch (e) {
            case DESPERTAR:
                p.caja(dx + 60, arriba, 90, 60, 6, 0x111318, 1);
                p.texto(((int) (reloj * 2) % 2 == 0 ? "> _" : ">"), dx + 70, arriba + 18, 18, true, 0x5BD685, 1);
                break;
            case TARROS:
                for (int i = 0; i < 3; i++) {
                    p.caja(dx + i * 50, arriba + 20, 36, 50, 8, 0xBFE3F2, 0.5f);
                    p.texto(new String[]{"int", "char", "bool"}[i], dx + i * 50 + 2, arriba + 36, 14, true, 0xFFFFFF, 1);
                }
                break;
            case BALANZA: {
                float incl = (float) Math.sin(reloj * 1.5f) * 8;
                p.linea(dx + 90, arriba + 70, dx + 90, arriba + 20, 4, ACERO, 1);
                p.linea(dx + 30, arriba + 20 + incl, dx + 150, arriba + 20 - incl, 4, ACERO, 1);
                p.elipse(dx + 30, arriba + 30 + incl, 20, 5, 0, 0xFFD23F, 1);
                p.elipse(dx + 150, arriba + 30 - incl, 20, 5, 0, 0xFFD23F, 1);
                break;
            }
            case APARCAMIENTO:
                for (int i = 0; i < 10; i++) p.rect(x + 20 + i * w / 10, suelo + 10, w / 20, 4, 0xFFFFFF, 0.7f);
                p.caja(dx + 120, arriba, 34, 34, 6, 0x3A6EB3, 1);
                p.textoCentrado("P", dx + 137, arriba + 6, 20, true, 0xFFFFFF, 1);
                break;
            case OBRA:
                p.linea(x + w - 60, arriba, x + w - 60, suelo, 6, 0xFFB000, 1);
                p.linea(x + w - 180, arriba + 6, x + w - 30, arriba + 6, 5, 0xFFB000, 1);
                p.linea(x + w - 140, arriba + 6, x + w - 140, arriba + 30 + (float) Math.sin(reloj) * 6, 1.5f, 0xDDDDDD, 1);
                break;
            case COCINA:
                p.caja(dx + 50, suelo - 70, 120, 70, 6, ACERO, 1);
                for (int i = 0; i < 2; i++) p.aro(dx + 80 + i * 60, suelo - 70, 14, 3, 0xFF7A45, 0.9f);
                break;
            case TAQUILLAS:
                for (int i = 0; i < 4; i++) {
                    p.caja(dx + i * 46, arriba, 40, 70, 3, 0x5A7A9A, 1);
                    p.texto(String.valueOf(i), dx + i * 46 + 14, arriba + 8, 16, true, 0xFFFFFF, 1);
                }
                break;
            case PINTURA:
                for (int i = 0; i < 5; i++) {
                    p.circulo(dx + 40 + i * 26, arriba + 10, 9, new int[]{0xFF5E5B, 0xFFD23F, 0x5BD685, 0x3FA9F5, 0xB07CFF}[i], 1);
                }
                break;
            case PLANOS:
                p.caja(dx + 20, arriba, 150, 80, 4, 0x1E4D8C, 1);
                for (int i = 0; i < 5; i++) p.rect(dx + 30, arriba + 12 + i * 14, 80 + (i % 2) * 40, 2, 0xBFD8FF, 0.8f);
                break;
            case CAJA_FUERTE:
                // La caja cerrada (private) y sus botones de fuera (los métodos públicos).
                p.caja(dx + 40, arriba, 110, 80, 8, 0x5B6067, 1);
                p.aro(dx + 80, arriba + 40, 18, 3, 0xDDDDDD, 1);
                p.linea(dx + 80, arriba + 40, dx + 80 + (float) Math.cos(reloj) * 14, arriba + 40 + (float) Math.sin(reloj) * 14, 2, 0xFFD23F, 1);
                for (int i = 0; i < 2; i++) {
                    boolean pulsado = (int) (reloj * 2) % 2 == i;
                    p.caja(dx + 112, arriba + 12 + i * 32, 32, 24, 5, pulsado ? 0x5BD685 : 0x3FA9F5, 1);
                    p.texto(i == 0 ? "get" : "set", dx + 115, arriba + 15 + i * 32, 14, true, 0xFFFFFF, 1);
                }
                break;
            case FAMILIA:
                // La abuela robot (con su antena de pinchos); los hijos la heredan.
                AnimacionDelCaso.robotito(p, dx + 140, suelo, 0.2f, true, 0x7B61FF);
                p.linea(dx + 140, suelo - 96, dx + 132, suelo - 112, 2, 0xFFD23F, 1);
                p.linea(dx + 140, suelo - 96, dx + 148, suelo - 112, 2, 0xFFD23F, 1);
                break;
            default:
                break;
        }
    }

    /** Los fondos de los objetos (desde el polimorfismo) y de lo avanzado. */
    private static void fondoDeLoAvanzado(Pincel p, Escena e, float x, float y, float w, float reloj, float suelo, float dx, float arriba) {
        switch (e) {
            case CORO:
                for (int i = 0; i < 3; i++) {
                    AnimacionDelCaso.robotito(p, dx + 40 + i * 60, suelo - Math.abs((float) Math.sin(reloj * (3 + i))) * 10,
                            (float) Math.abs(Math.sin(reloj * (2 + i))),
                            true, new int[]{0xFF7A45, 0x5BD685, 0xB07CFF}[i]);
                }
                break;
            case ENCHUFES:
                enchufes(p, dx, arriba, reloj);
                break;
            case COMPOSICION:
                // El robot y sus piezas, que se acercan y se encajan (tiene un..., no es un...).
                for (int i = 0; i < 3; i++) {
                    float lejos = (float) Math.abs(Math.sin(reloj * 1.2f)) * 18;
                    p.caja(dx + 60 + (i - 1) * (40 + lejos), arriba + 20 + (i == 1 ? 0 : 20), 34, 34, 6, new int[]{0x5BD685, 0x3FA9F5, 0xFF7A45}[i], 1);
                }
                p.texto(T.idioma.elegir("tiene-un", "hat-ein", "has-a"), dx + 40, arriba + 74, 14, true, 0xF2EEE3, 1);
                break;
            case ABSTRACTA:
                // El molde con un hueco (el método abstracto) y dos hijos que lo rellenan cada uno a su manera.
                p.caja(dx + 20, arriba, 70, 70, 8, 0x5B6067, 1);
                p.caja(dx + 40, arriba + 20, 30, 30, 4, 0x111318, 1);
                p.textoCentrado("?", dx + 55, arriba + 24, 18, true, 0xFFD23F, 1);
                for (int i = 0; i < 2; i++) {
                    p.caja(dx + 110 + i * 45, arriba + 30, 38, 38, 6, i == 0 ? 0xFF7A45 : 0x5BD685, 1);
                    p.textoCentrado(i == 0 ? "★" : "◆", dx + 129 + i * 45, arriba + 38, 18, true, 0xFFFFFF, 1);
                }
                break;
            case TROPIEZO:
                // La cáscara con la que tropieza, y la red que lo recoge (el catch).
                p.elipse(x + w * 0.45f, suelo - 4, 14, 5, 0, 0xFFD23F, 1);
                for (int i = 0; i < 6; i++) p.linea(dx + i * 25, arriba + 70, dx + i * 25 + 12, arriba + 85, 1.5f, 0xDDDDDD, 0.6f);
                p.linea(dx, arriba + 70, dx + 150, arriba + 70, 2, 0xDDDDDD, 0.8f);
                break;
            case CLASIFICAR:
                for (int i = 0; i < 3; i++) p.caja(dx + i * 60, suelo - 60, 48, 60, 14, new int[]{0x8C6A43, 0x6E8C43, 0x436E8C}[i], 1);
                break;
            case CAJAS_T:
                p.caja(dx + 50, arriba + 10, 100, 70, 6, 0x8C6A43, 1);
                p.textoCentrado("<T>", dx + 100, arriba + 32, 22, true, 0xFFF1C1, 1);
                break;
            case MAQUINA:
                p.caja(dx + 40, arriba + 20, 110, 60, 10, 0x5B6067, 1);
                p.textoCentrado("x -> x * 2", dx + 95, arriba + 40, 16, true, 0x9BE7FF, 1);
                break;
            case RIO:
                for (int i = 0; i < 12; i++) {
                    float px = x + ((i * 70 + reloj * 50) % w);
                    p.circulo(px, suelo + 13, 4, 0x3FA9F5, 0.8f);
                }
                p.caja(dx + 80, suelo - 60, 16, 60, 3, ACERO, 1);
                p.texto("filter", dx + 102, suelo - 52, 16, true, 0x9BE7FF, 1);
                break;
            case FICHAS:
                for (int i = 0; i < 3; i++) p.caja(dx + i * 40, arriba + i * 8, 70, 46, 6, 0xF2EEE3, 1);
                p.texto("record", dx + 86, arriba + 30, 14, true, 0x333333, 1);
                break;
            case EQUIPO:
                // Los otros hilos: dos robotitos que trabajan a la vez que el nuestro.
                for (int i = 0; i < 2; i++) {
                    float px = dx + 60 + i * 70 + (float) Math.sin(reloj * (2 + i)) * 20;
                    AnimacionDelCaso.robotito(p, px, suelo, (float) Math.abs(Math.sin(reloj * (5 + i))), true, i == 0 ? 0xFF7A45 : 0x5BD685);
                }
                break;
            case MUNECAS:
                hanoi(p, dx, arriba, reloj);
                break;
            case ARQUITECTO:
                p.caja(dx + 60, arriba, 110, 80, 6, 0xF2EEE3, 1);
                for (int i = 0; i < 4; i++) p.texto(i < 3 ? "✔" : "✘", dx + 72, arriba + 6 + i * 18, 14, true, i < 3 ? 0x5BD685 : 0xFF5E5B, 1);
                break;
            default:
                break;
        }
    }

    /** El mismo enchufe (el contrato de la interfaz) y en cada uno una pieza distinta que lo cumple. */
    private static void enchufes(Pincel p, float dx, float arriba, float reloj) {
        // El mismo enchufe (el contrato), y en cada uno una pieza distinta que lo cumple: láser, rueda y puño.
        for (int i = 0; i < 3; i++) {
            float ex = dx + i * 60;
            p.caja(ex, arriba + 40, 40, 30, 6, 0xDDE3EA, 1);
            p.rect(ex + 12, arriba + 48, 4, 12, 0x333333, 1);
            p.rect(ex + 24, arriba + 48, 4, 12, 0x333333, 1);
            float sube = (float) Math.abs(Math.sin(reloj * 2 + i)) * 6;
            if (i == 0) p.rect(ex + 6, arriba + 14 - sube, 28, 10, 0xFF4FA3, 1);
            if (i == 1) p.aro(ex + 20, arriba + 22 - sube, 12, 4, 0x2A2F38, 1);
            if (i == 2) p.circulo(ex + 20, arriba + 20 - sube, 12, 0x3FA9F5, 1);
        }
    }

    /** La torre de Hanói: tres palos y tres discos que pasan solos del primero al último. */
    private static void hanoi(Pincel p, float dx, float arriba, float reloj) {
        // La torre de Hanói: tres palos y los discos, que se mueven solos.
        for (int i = 0; i < 3; i++) p.rect(dx + 20 + i * 70, arriba + 10, 5, 70, ACERO, 1);
        int movidos = (int) (reloj * 1.5f) % 4;
        for (int d = 0; d < 3; d++) {
            int palo = d < movidos ? 2 : 0;
            float anchoDisco = 50 - d * 12;
            p.caja(dx + 22 + palo * 70 - anchoDisco / 2, arriba + 70 - d * 12, anchoDisco, 10, 3, new int[]{0xFF5E5B, 0xFFD23F, 0x5BD685}[d], 1);
        }
    }

    // ================================================================ los objetos

    /** Lo que el robotito apila o coloca en esa escena (color CAJA: el suyo; si no, el que se pide: gris si se esperaba, rojo si sobra). */
    static void objeto(Pincel p, Escena e, float cx, float cy, float lado, int color, int caja, float alfa) {
        boolean suyo = color == caja;
        switch (e) {
            case COCINA:
                p.elipse(cx + lado / 2, cy + lado * 0.6f, lado * 0.55f, lado * 0.3f, 0, suyo ? 0xE8B04A : color, alfa);
                p.elipse(cx + lado / 2, cy + lado * 0.5f, lado * 0.45f, lado * 0.18f, 0, 0xF6D27A, alfa * 0.8f);
                break;
            case OBRA:
                p.caja(cx, cy + lado * 0.3f, lado, lado * 0.6f, 1, suyo ? 0xC0533A : color, alfa);
                p.rect(cx, cy + lado * 0.58f, lado, 1.5f, 0xE6DED0, alfa);
                break;
            case TAQUILLAS: case PLANOS: case FICHAS:
                p.caja(cx + lado * 0.1f, cy, lado * 0.8f, lado, 2, suyo ? (e == Escena.FICHAS ? 0xF2EEE3 : 0x5A7A9A) : color, alfa);
                p.rect(cx + lado * 0.2f, cy + lado * 0.3f, lado * 0.6f, 2, 0x333333, alfa * 0.6f);
                break;
            case PINTURA:
                p.caja(cx + lado * 0.15f, cy + lado * 0.2f, lado * 0.7f, lado * 0.8f, 3, 0xDDE3EA, alfa);
                p.caja(cx + lado * 0.15f, cy + lado * 0.2f, lado * 0.7f, lado * 0.3f, 3, suyo ? 0xB07CFF : color, alfa);
                break;
            case APARCAMIENTO:
                p.caja(cx, cy + lado * 0.35f, lado, lado * 0.45f, 4, suyo ? 0xFF7A45 : color, alfa);
                p.circulo(cx + lado * 0.25f, cy + lado * 0.85f, lado * 0.13f, 0x111111, alfa);
                p.circulo(cx + lado * 0.75f, cy + lado * 0.85f, lado * 0.13f, 0x111111, alfa);
                break;
            case TARROS: case CLASIFICAR: case COMPOSICION:
                p.caja(cx + lado * 0.1f, cy + lado * 0.1f, lado * 0.8f, lado * 0.9f, lado * 0.3f, suyo ? 0x8C6A43 : color, alfa);
                p.rect(cx + lado * 0.25f, cy + lado * 0.1f, lado * 0.5f, 2, 0x333333, alfa);
                break;
            case MUNECAS:
                // Una muñeca rusa (las de más arriba, más pequeñas).
                p.elipse(cx + lado / 2, cy + lado * 0.62f, lado * 0.4f, lado * 0.38f, 0, suyo ? 0xD94A5A : color, alfa);
                p.circulo(cx + lado / 2, cy + lado * 0.28f, lado * 0.24f, suyo ? 0xF2C9A0 : color, alfa);
                break;
            case FAMILIA: case CORO: case EQUIPO:
                // Un robotito diminuto (los de la familia llevan la antena heredada).
                p.caja(cx + lado * 0.2f, cy + lado * 0.35f, lado * 0.6f, lado * 0.6f, 2, suyo ? 0x7B61FF : color, alfa);
                p.caja(cx + lado * 0.3f, cy + lado * 0.1f, lado * 0.4f, lado * 0.3f, 2, suyo ? 0x9B85FF : color, alfa);
                if (e == Escena.FAMILIA) p.linea(cx + lado / 2, cy + lado * 0.1f, cx + lado / 2, cy - lado * 0.15f, 1.5f, 0xFFD23F, alfa);
                break;
            case RIO: case MAQUINA: case BALANZA:
                p.circulo(cx + lado / 2, cy + lado / 2, lado * 0.42f, suyo ? 0x3FA9F5 : color, alfa);
                p.circulo(cx + lado * 0.4f, cy + lado * 0.4f, lado * 0.12f, 0xFFFFFF, alfa * 0.7f);
                break;
            case CAJAS_T: case CAJA_FUERTE: case ABSTRACTA:
                p.caja(cx, cy, lado, lado, 2, suyo ? 0x8C6A43 : color, alfa);
                p.textoCentrado("T", cx + lado / 2, cy + lado * 0.15f, Math.max(10, lado * 0.6f), true, 0xFFF1C1, alfa);
                break;
            default:
                p.caja(cx, cy, lado, lado, 2, color, alfa);
                if (alfa >= 1) p.rect(cx + 2, cy + lado / 2 - 1, lado - 4, 2, Pincel.oscurecer(color, 0.25f), 1);
                break;
        }
    }

    /** Cuántas escenas hay. */
    public static int cuantas() {
        return Escena.values().length;
    }

    /** Una escena de muestra (para las capturas): su fondo, unos objetos apilados y el robotito trabajando. */
    public static void muestra(Pincel p, int i, float x, float y, float w, float h, float reloj) {
        Escena e = Escena.values()[i];
        p.caja(x, y, w, h, 8, 0x14171C, 1);
        p.recortar(x, y, w, h);
        fondo(p, e, x, y, w, h, reloj);
        float suelo = y + h - 26;
        for (int k = 0; k < 5; k++) objeto(p, e, x + 90 + k * 26, suelo - 26 - (k % 2) * 26, 24, 0xC98B4A, 0xC98B4A, 1);
        AnimacionDelCaso.robotito(p, x + 40, suelo, (float) Math.abs(Math.sin(reloj * 3)), true, 0x3FA9F5);
        p.sinRecorte();
    }

    /** En la escena de las excepciones, el robotito tropieza a media tarea (cuánto se tuerce, de 0 a 1) y se levanta. */
    static float tropiezo(Escena e, float f) {
        if (e != Escena.TROPIEZO || f < 0.42f || f > 0.6f) return 0;
        return (float) Math.sin((f - 0.42f) / 0.18f * Math.PI);
    }
}
